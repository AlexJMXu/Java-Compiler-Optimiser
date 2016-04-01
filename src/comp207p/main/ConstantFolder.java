package comp207p.main;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;

import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;
import org.apache.bcel.util.InstructionFinder;


public class ConstantFolder
{
	ClassParser parser = null;
	ClassGen gen = null;

	JavaClass original = null;
	JavaClass optimized = null;

	public ConstantFolder(String classFilePath)
	{
		try{
			this.parser = new ClassParser(classFilePath);
			this.original = this.parser.parse();
			this.gen = new ClassGen(this.original);
		} catch(IOException e){
			e.printStackTrace();
		}
	}
	
	public void optimize()
	{
		ClassGen cgen = new ClassGen(original);
		ConstantPoolGen cpgen = cgen.getConstantPool();

        ConstantPool cp = cpgen.getConstantPool();
        Method[] methods = cgen.getMethods();

        printConstants(cp);

        for(Method m : methods) {
            System.out.println(m); //Print method name

            optimiseMethod(cgen, cpgen, m);
        }
        
		this.optimized = cgen.getJavaClass();
	}

    /**
     * Optimise method instruction list
     */
    private void optimiseMethod(ClassGen cgen, ConstantPoolGen cpgen, Method method) {
        Code methodCode = method.getCode();

        if(methodCode != null) { // Non-abstract method
            System.out.println(methodCode);
        }

        InstructionList instructionList = new InstructionList(methodCode.getCode());

        // Initialise a method generator with the original method as the baseline
        MethodGen methodGen = new MethodGen(
                method.getAccessFlags(),
                method.getReturnType(),
                method.getArgumentTypes(),
                null, method.getName(),
                cgen.getClassName(),
                instructionList,
                cpgen
        );

        // Simple Folding / Constant Variable Folding
        int optimiseCounter = 1;
        
        // Run in while loop until no more optimisations can be made
        while (optimiseCounter > 0) {
            optimiseCounter = 0;

            optimiseCounter += optimiseArithmeticOperation(instructionList, cpgen);
        }

        // COMMENTED OUT UNTIL ISSUES ARE FIXED
        //optimiseComparisons(instructionList, cpgen);

        // setPositions(true) checks whether jump handles
        // are all within the current method
        instructionList.setPositions(true);

        // set max stack/local
        methodGen.setMaxStack();
        methodGen.setMaxLocals();

        // generate the new method with optimised instructions
        Method newMethod = methodGen.getMethod();

        System.out.println("Fully optimised instruction set:");
        printConstants(cpgen.getConstantPool());
        System.out.println(newMethod.getCode());

        // replace the method in the original class
        cgen.replaceMethod(method, newMethod);
    }

    /**
     * Optimise arithmetic operations
     * @param instructionList Instruction list
     * @return Number of changes made to instructions
     */
    private int optimiseArithmeticOperation(InstructionList instructionList, ConstantPoolGen cpgen) {
        int changeCounter = 0;

        String regExp = "(ConstantPushInstruction|CPInstruction|LoadInstruction) (ConversionInstruction)* " +
                "(ConstantPushInstruction|CPInstruction|LoadInstruction) (ConversionInstruction)* " +
                "ArithmeticInstruction";

        // Search for instruction list where two constants are loaded from the pool, followed by an arithmetic
        InstructionFinder finder = new InstructionFinder(instructionList);

        for(Iterator it = finder.search(regExp); it.hasNext();) {
            InstructionHandle[] match = (InstructionHandle[]) it.next();

            //Debug output
            System.out.println("==================================");
            System.out.println("Found optimisable instruction set");
            changeCounter++; // Optimisation found, iterate through instructions again to look for more optimisation
            for(InstructionHandle h : match) {
                if(h.getInstruction() instanceof LoadInstruction) {
                    System.out.format("%s | Val: %s\n", h, getLoadInstructionValue(h, cpgen));
                } else {
                    System.out.println(h);
                }
            }

            Number leftValue, rightValue;
            InstructionHandle leftInstruction, rightInstruction, operationInstruction;

            leftInstruction = match[0];
            if (match[1].getInstruction() instanceof ConversionInstruction) {
                rightInstruction = match[2];
            } else {
                rightInstruction = match[1];
            }

            if (rightInstruction == match[2] && match[3].getInstruction() instanceof ConversionInstruction) {
                operationInstruction = match[4];
            } else if (rightInstruction == match[2] || (rightInstruction == match[1] && match[2].getInstruction() instanceof ConversionInstruction)) {
                operationInstruction = match[3];
            } else {
                operationInstruction = match[2];
            }

            if (leftInstruction.getInstruction() instanceof LoadInstruction && rightInstruction.getInstruction() instanceof LoadInstruction) {
                leftValue = getLoadInstructionValue(leftInstruction, cpgen);
                rightValue = getLoadInstructionValue(rightInstruction, cpgen);
            } else if (leftInstruction.getInstruction() instanceof LoadInstruction) {
                leftValue = getLoadInstructionValue(leftInstruction, cpgen);
                rightValue = getConstantValue(rightInstruction, cpgen);
            } else if (rightInstruction.getInstruction() instanceof LoadInstruction) {
                leftValue = getConstantValue(leftInstruction, cpgen);
                rightValue = getLoadInstructionValue(rightInstruction, cpgen);
            } else {
                leftValue = getConstantValue(leftInstruction, cpgen);
                rightValue =  getConstantValue(rightInstruction, cpgen);
            }

            ArithmeticInstruction operation = (ArithmeticInstruction) operationInstruction.getInstruction();

            Double result = foldOperation(operation, leftValue, rightValue);
            System.out.format("Folding to value %f\n", result);

            //Insert as a new constant into constant pool
            int newPoolIndex;
            String type;
            if(checkSignature(leftInstruction, rightInstruction, cpgen, "D")) {
                newPoolIndex = cpgen.addDouble(result);
                type = "D";
            } else if(checkSignature(leftInstruction, rightInstruction, cpgen, "F")) {
                newPoolIndex = cpgen.addFloat(result.floatValue());
                type = "F";
            } else if(checkSignature(leftInstruction, rightInstruction, cpgen, "J")) { //J is the signature for long, wtf
                newPoolIndex = cpgen.addLong(result.longValue());
                type = "J";
            } else if(checkSignature(leftInstruction, rightInstruction, cpgen, "I")) { //int
                newPoolIndex = cpgen.addInteger(result.intValue());
                type = "I";
            } else if(checkSignature(leftInstruction, rightInstruction, cpgen, "B")) {
                newPoolIndex = cpgen.addInteger(result.intValue());
                type = "I"; //Promote byte to integer
            } else {
                throw new RuntimeException("Type not defined");
            }

            //TODO Check if these constants are ACTUALLY unused first, have to also check other methods
            //TODO Delete unused constants
                /*
                Setting it to writes wrongly formatted bytecode
                if (leftInstruction.getInstruction() instanceof LDC || leftInstruction.getInstruction() instanceof LDC2_W) {
                    cpgen.setConstant(((IndexedInstruction) leftInstruction.getInstruction()).getIndex(), null);
                }
                if (rightInstruction.getInstruction() instanceof LDC || rightInstruction.getInstruction() instanceof LDC2_W) {
                    cpgen.setConstant(((IndexedInstruction) rightInstruction.getInstruction()).getIndex(), null);
                }
                */

            //Set left constant handle to point to new index
            if (type.equals("F") || type.equals("I")) {
                LDC newInstruction = new LDC(newPoolIndex);
                leftInstruction.setInstruction(newInstruction);
            } else {
                LDC2_W newInstruction = new LDC2_W(newPoolIndex);
                leftInstruction.setInstruction(newInstruction);
            }

            //Delete other handles
            try {
                instructionList.delete(match[1], operationInstruction);
            } catch (TargetLostException e) {
                e.printStackTrace();
            }

            System.out.println("==================================");

            break;
        }

        return changeCounter;
    }

    private void optimiseComparisons(InstructionList instructionList, ConstantPoolGen cpgen) {
        String regExp = "((ConstantPushInstruction|CPInstruction|LoadInstruction)" +
                        " (ConstantPushInstruction|CPInstruction|LoadInstruction)" +
                        " (LCMP|DCMPG|DCMPL|FCMPG|FCMPL)* IfInstruction ICONST GOTO ICONST";

        InstructionFinder finder = new InstructionFinder(instructionList);

        for(Iterator it = finder.search(regExp); it.hasNext();) {
            InstructionHandle[] match = (InstructionHandle[]) it.next();

            //Debug output
            System.out.println("==================================");
            System.out.println("Found optimisable instruction set");
            for(InstructionHandle h : match) {
                if(h.getInstruction() instanceof LoadInstruction) {
                    System.out.format("%s | Val: %s\n", h, getLoadInstructionValue(h, cpgen));
                } else {
                    System.out.println(h);
                }
            }

            Number leftValue, rightValue;
            InstructionHandle leftInstruction, rightInstruction, compare = null, comparisonInstruction;

            leftInstruction = match[0];
            rightInstruction = match[1];

            if (match[2].getInstruction() instanceof IfInstruction) {
                comparisonInstruction = match[2];
            } else {
                compare = match[2];
                comparisonInstruction = match[3];
            }

            if (leftInstruction.getInstruction() instanceof LoadInstruction && rightInstruction.getInstruction() instanceof LoadInstruction) {
                leftValue = getLoadInstructionValue(leftInstruction, cpgen);
                rightValue = getLoadInstructionValue(rightInstruction, cpgen);
            } else if (leftInstruction.getInstruction() instanceof LoadInstruction) {
                leftValue = getLoadInstructionValue(leftInstruction, cpgen);
                rightValue = getConstantValue(rightInstruction, cpgen);
            } else if (rightInstruction.getInstruction() instanceof LoadInstruction) {
                leftValue = getConstantValue(leftInstruction, cpgen);
                rightValue = getLoadInstructionValue(rightInstruction, cpgen);
            } else {
                leftValue = getConstantValue(leftInstruction, cpgen);
                rightValue =  getConstantValue(rightInstruction, cpgen);
            }

            IfInstruction comparison = (IfInstruction) comparisonInstruction.getInstruction();

            int result = 0;

            if (comparisonInstruction == match[2]) {
                result = checkIntComparison(comparison, leftValue, rightValue);
            } else {
                result = checkFirstComparison(compare, leftValue, rightValue);
                result = checkSecondComparison(comparison, result);
            }

            System.out.format("Folding to value %d\n", result);

            //Set left constant handle to point to new index
            if (result == 1) {
                ICONST newInstruction = new ICONST(1);
                leftInstruction.setInstruction(newInstruction);
            } else if (result == 0) {
                ICONST newInstruction = new ICONST(0);
                leftInstruction.setInstruction(newInstruction);
            } else {
                ICONST newInstruction = new ICONST(-1);
                leftInstruction.setInstruction(newInstruction);
            }

            //Delete other handles
            try {
                instructionList.delete(match[1], match[match.length-1]);
            } catch (TargetLostException e) {
                e.printStackTrace();
            }

            System.out.println("==================================");
        }
    }

    private int checkIntComparison(IfInstruction comparison, Number leftValue, Number rightValue) {
        if (comparison instanceof IF_ICMPEQ) { // if value 1 equals value 2
            if (leftValue.intValue() == rightValue.intValue()) return 1;
            else return 0;
        } else if (comparison instanceof IF_ICMPGE) { // if value 1 greater than or equal to to value 2
            if (leftValue.intValue() >= rightValue.intValue()) return 1;
            else return 0;
        } else if (comparison instanceof IF_ICMPGT) { // if value 1 greater than value 2
            if (leftValue.intValue() > rightValue.intValue()) return 1;
            else return 0;
        } else if (comparison instanceof IF_ICMPLE) { // if value 1 less than or equal to value 2
            if (leftValue.intValue() <= rightValue.intValue()) return 1;
            else return 0;
        } else if (comparison instanceof IF_ICMPLT) { // if value 1 less than value 2
            if (leftValue.intValue() < rightValue.intValue()) return 1;
            else return 0;
        } else if (comparison instanceof IF_ICMPNE) { // if value 1 not equal to value 2
            if (leftValue.intValue() != rightValue.intValue()) return 1;
            else return 0;
        } else {
            throw new RuntimeException("Comparison not defined");
        }
    }

    private int checkFirstComparison(InstructionHandle comparison, Number leftValue, Number rightValue) {
        if (comparison.getInstruction() instanceof DCMPG) {
            if (leftValue.doubleValue() > rightValue.doubleValue()) return 1;
            else return 0;
        } else if (comparison.getInstruction()  instanceof DCMPL) {
            if (leftValue.doubleValue() < rightValue.doubleValue()) return 1;
            else return 0;
        } else if (comparison.getInstruction()  instanceof FCMPG) {
            if (leftValue.floatValue() > rightValue.floatValue()) return 1;
            else return 0;
        } else if (comparison.getInstruction()  instanceof FCMPL) {
            if (leftValue.floatValue() < rightValue.floatValue()) return 1;
            else return 0;
        } else if (comparison.getInstruction()  instanceof LCMP) {
            if (leftValue.longValue() == rightValue.longValue()) return 0;
            else if (leftValue.longValue() > rightValue.longValue()) return 1;
            else return -1;
        } else {
            throw new RuntimeException("Comparison not defined");
        }
    }

    private int checkSecondComparison(IfInstruction comparison, int value) {
        if (comparison instanceof IFEQ) {
            if (value == 0) return 1;
            else return 0;
        } else if (comparison instanceof IFGE) {
            if (value >= 0) return 1;
            else return 0;
        } else if (comparison instanceof IFGT) {
            if (value > 0) return 1;
            else return 0;
        } else if (comparison instanceof IFLE) {
            if (value <= 0) return 1;
            else return 0;
        } else if (comparison instanceof IFLT) {
            if (value < 0) return 1;
            else return 0;
        } else if (comparison instanceof IFNE) {
            if (value != 0) return 1;
            else return 0;
        } else {
            throw new RuntimeException("Comparison not defined");
        }
    }

    /**
     * Fold an arithmetic operation and get the result
     * @param operation Arithmetic operation e.g. IADD, DMUL, etc.
     * @param left Left value of binary operator
     * @param right Right side of binary operator
     * @return
     */
    private double foldOperation(ArithmeticInstruction operation, Number left, Number right) {
        if(operation instanceof IADD || operation instanceof  FADD || operation instanceof LADD || operation instanceof DADD) {
            return left.doubleValue() + right.doubleValue();
        } else if(operation instanceof ISUB || operation instanceof  FSUB || operation instanceof LSUB || operation instanceof DSUB){
            return left.doubleValue() - right.doubleValue();
        } else if(operation instanceof IMUL || operation instanceof  FMUL || operation instanceof LMUL || operation instanceof DMUL){
            return left.doubleValue() * right.doubleValue();
        } else if(operation instanceof IDIV || operation instanceof  FDIV || operation instanceof LDIV || operation instanceof DDIV){
            return left.doubleValue() / right.doubleValue();    
        } else {
            throw new RuntimeException("Not supported operation");
        }
    }

    /**
     * Get the value of a load instruction, e.g. iload_2
     * @param h The load instruction fetch the value from
     * @param cpgen Constant pool of the class
     * @return Load instruction value
     */
    private Number getLoadInstructionValue(InstructionHandle h, ConstantPoolGen cpgen) {
        Instruction instruction = h.getInstruction();
        if(!(instruction instanceof LoadInstruction)) {
            throw new RuntimeException("InstructionHandle has to be of type LoadInstruction");
        }

        int localVariableIndex = ((LocalVariableInstruction) instruction).getIndex();

        InstructionHandle handleIterator = h;
        while(!(instruction instanceof StoreInstruction) || ((StoreInstruction) instruction).getIndex() != localVariableIndex) {
            handleIterator = handleIterator.getPrev();
            instruction = handleIterator.getInstruction();
        }

        //Go back previous one more additional time to fetch constant push instruction
        handleIterator = handleIterator.getPrev();
        instruction = handleIterator.getInstruction();

        if(instruction instanceof ConstantPushInstruction) {
            return ((ConstantPushInstruction) instruction).getValue();
        } else if (instruction instanceof LDC) {
            return (Number) ((LDC) instruction).getValue(cpgen);
        } else if (instruction instanceof LDC2_W) {
            return ((LDC2_W) instruction).getValue(cpgen);
        } else {
            throw new RuntimeException("Cannot fetch value for this type of object");
        }
    }

    /**
     * Get the signature of a load instruction, e.g. iload_1 would return String "I"
     * @param h The load instruction fetch the value from
     * @param cpgen Constant pool of the class
     * @return Load instruction value signature
     */
    private String getLoadInstructionSignature(InstructionHandle h, ConstantPoolGen cpgen) {
        Instruction instruction = h.getInstruction();
        if(!(instruction instanceof LoadInstruction)) {
            throw new RuntimeException("InstructionHandle has to be of type LoadInstruction");
        }

        int localVariableIndex = ((LocalVariableInstruction) instruction).getIndex();

        InstructionHandle handleIterator = h;
        while(!(instruction instanceof StoreInstruction) || ((StoreInstruction) instruction).getIndex() != localVariableIndex) {
            handleIterator = handleIterator.getPrev();
            instruction = handleIterator.getInstruction();
        }

        //Go back previous one more additional time to fetch constant push instruction
        handleIterator = handleIterator.getPrev();
        instruction = handleIterator.getInstruction();

        return ((TypedInstruction)instruction).getType(cpgen).getSignature();
    }

    /**
     * Compare signatures of types of values from left and right instructions to specified signature, e.g. "D"
     * @param left Left InstructionHandle e.g iload_1, LDC
     * @param right Refer to left
     * @param cpgen Constant pool of the class
     * @param signature The specified String (Signature of Type) to compare
     * @return true or false
     */
    private boolean checkSignature(InstructionHandle left, InstructionHandle right, ConstantPoolGen cpgen, String signature) {
        if (left.getInstruction() instanceof LoadInstruction && right.getInstruction() instanceof LoadInstruction) {
            if (getLoadInstructionSignature(left, cpgen).equals(signature) || getLoadInstructionSignature(right, cpgen).equals(signature)) {
                return true;
            }
        } else if (left.getInstruction() instanceof LoadInstruction) {
            if (getLoadInstructionSignature(left, cpgen).equals(signature) || ((TypedInstruction)right.getInstruction()).getType(cpgen).getSignature().equals(signature)) {
                return true;
            }
        } else if (right.getInstruction() instanceof LoadInstruction) {
            if (((TypedInstruction)left.getInstruction()).getType(cpgen).getSignature().equals(signature) || getLoadInstructionSignature(right, cpgen).equals(signature) ) {
                return true;
            }
        } else {
            if(((TypedInstruction)left.getInstruction()).getType(cpgen).getSignature().equals(signature) || ((TypedInstruction)right.getInstruction()).getType(cpgen).getSignature().equals(signature)) {
                return true;
            }
        }

        return false;
    }

    /**
     * get Number value from InstructionHandle
     * @param h The load instruction fetch the value from
     * @param cpgen Constant pool of the class
     * @return InstructionHandle value
     */
    private Number getConstantValue(InstructionHandle h, ConstantPoolGen cpgen) {
        Number value;

        if (h.getInstruction() instanceof ConstantPushInstruction) {
            value = ((ConstantPushInstruction) h.getInstruction()).getValue();
        } else if (h.getInstruction() instanceof LDC) {
            value = (Number) ((LDC) h.getInstruction()).getValue(cpgen);
        } else if (h.getInstruction() instanceof LDC2_W) {
            value = ((LDC2_W) h.getInstruction()).getValue(cpgen);
        } else {
            throw new RuntimeException();
        }

        return value;
    }

    /**
     * Print out constants for debugging
     * @param cp Constant Pool
     */
    private void printConstants(ConstantPool cp) {
        Constant[] constants = cp.getConstantPool();
        int constantCount = 0;
        for(Constant c : constants) {
            if(c == null) continue;
            if(c instanceof ConstantString) continue; // We don't care about strings
            if(c instanceof ConstantUtf8) continue;

            System.out.println(c);

            constantCount++;
        }

        System.out.format("Total constants: %d\n", constantCount);
    }
	
	public void write(String optimisedFilePath)
	{
		this.optimize();

		try {
			FileOutputStream out = new FileOutputStream(new File(optimisedFilePath));
			this.optimized.dump(out);
		} catch (FileNotFoundException e) {
			// Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// Auto-generated catch block
			e.printStackTrace();
		}
	}
}