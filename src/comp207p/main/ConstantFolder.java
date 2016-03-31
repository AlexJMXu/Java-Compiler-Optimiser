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
        Constant[] constants = cp.getConstantPool();
        Method[] methods = cgen.getMethods();

        for(Constant c : constants) {
            if(c == null) continue;
            if(c instanceof ConstantString) continue; // We don't care about strings
            if(c instanceof ConstantUtf8) continue;

            System.out.println(c); //Print out constants
        }

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
        MethodGen methodGen = new MethodGen(method.getAccessFlags(), method.getReturnType(), method.getArgumentTypes(), null, method.getName(), cgen.getClassName(), instructionList, cpgen);

        // Search for instruction list where two constants are loaded from the pool, followed by an arithmetic
        // instruction. E.g. IADD, DMUL, etc.
        InstructionFinder finder = new InstructionFinder(instructionList);

        //Regular expression matching string
        String regExp = "(ConstantPushInstruction|LDC|LDC2_W) (ConstantPushInstruction|LDC|LDC2_W) ArithmeticInstruction";

        for(Iterator it = finder.search(regExp); it.hasNext();) {
            InstructionHandle[] match = (InstructionHandle[]) it.next();

            //Debug output
            System.out.println("==================================");
            System.out.println("Found optimisable instruction set");
            for(InstructionHandle h : match) {
                System.out.println(h);
            }

            LDC left = (LDC) match[0].getInstruction();
            LDC right = (LDC) match[1].getInstruction();
            ArithmeticInstruction operation = (ArithmeticInstruction) match[2].getInstruction();

            System.out.format("Left: %s %s\n", left.getValue(cpgen), left.getType(cpgen));
            System.out.format("Operator: %s\n", operation);
            System.out.format("Right: %s %s\n", right.getValue(cpgen), right.getType(cpgen));

            Double result = foldOperation(operation,(Number) left.getValue(cpgen), (Number) right.getValue(cpgen));
            System.out.format("Folding to value %f\n", result);

            //Insert as a new constant into constant pool
            int newPoolIndex;
            if(left.getType(cpgen).getSignature().equals("D") || (right.getType(cpgen).getSignature().equals("D"))) {
                newPoolIndex = cpgen.addDouble(result);
            } else if(left.getType(cpgen).getSignature().equals("F") || (right.getType(cpgen).getSignature().equals("F"))) {
                newPoolIndex = cpgen.addFloat(result.floatValue());
            } else if(left.getType(cpgen).getSignature().equals("L") || (right.getType(cpgen).getSignature().equals("L"))) {
                newPoolIndex = cpgen.addLong(result.longValue());
            } else { //int
                newPoolIndex = cpgen.addInteger(result.intValue());
            }

            //Set unused constants to null
            cpgen.setConstant(left.getIndex(), null);
            cpgen.setConstant(right.getIndex(), null);

            //Set left constant handle to point to new index
            left.setIndex(newPoolIndex);

            //Delete other handles
            try {
                instructionList.delete(match[1], match[2]);
            } catch (TargetLostException e) {
                e.printStackTrace();
            }

            //TODO Delete unused constants?
            //unused constants currently just set to null
            System.out.println("==================================");
        }


        // START OF CONSTANT VARIABLE FOLDING TEST/PRACTICE
        regExp = "(ConstantPushInstruction|CPInstruction) StoreInstruction LoadInstruction (ConstantPushInstruction|CPInstruction) ArithmeticInstruction (ConstantPushInstruction|CPInstruction) ArithmeticInstruction";
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

            Number firstNumber = 0, secondNumber = 0, thirdNumber = 0;

            if (match[0].getInstruction() instanceof ConstantPushInstruction) {
                firstNumber = (Number) ((ConstantPushInstruction) match[0].getInstruction()).getValue();
            } else if (match[0].getInstruction() instanceof LDC) {
                firstNumber = (Number) ((LDC) match[0].getInstruction()).getValue(cpgen);
            } else if (match[0].getInstruction() instanceof LDC2_W) {
                firstNumber = (Number) ((LDC2_W) match[0].getInstruction()).getValue(cpgen);
            }

            if (match[3].getInstruction() instanceof ConstantPushInstruction) {
                secondNumber = (Number) ((ConstantPushInstruction) match[3].getInstruction()).getValue();
            } else if (match[3].getInstruction() instanceof LDC) {
                secondNumber = (Number) ((LDC) match[3].getInstruction()).getValue(cpgen);
            } else if (match[3].getInstruction() instanceof LDC2_W) {
                secondNumber = (Number) ((LDC2_W) match[3].getInstruction()).getValue(cpgen);
            }

            if (match[5].getInstruction() instanceof ConstantPushInstruction) {
                thirdNumber = (Number) ((ConstantPushInstruction) match[5].getInstruction()).getValue();
            } else if (match[5].getInstruction() instanceof LDC) {
                thirdNumber = (Number) ((LDC) match[5].getInstruction()).getValue(cpgen);
            } else if (match[5].getInstruction() instanceof LDC2_W) {
                thirdNumber = (Number) ((LDC2_W) match[5].getInstruction()).getValue(cpgen);
            }

            ArithmeticInstruction firstOperation = (ArithmeticInstruction) match[4].getInstruction();
            ArithmeticInstruction secondOperation = (ArithmeticInstruction) match[6].getInstruction();

            Double firstResult = foldOperation(firstOperation, firstNumber, secondNumber);
            Double secondResult = foldOperation(secondOperation, firstResult, thirdNumber);
            System.out.format("Folding to value %f\n", secondResult);

            //Insert as a new constant into constant pool
            int newPoolIndex;
            if(((TypedInstruction)match[0].getInstruction()).getType(cpgen).getSignature().equals("D") || ((TypedInstruction)match[3].getInstruction()).getType(cpgen).getSignature().equals("D") || ((TypedInstruction)match[5].getInstruction()).getType(cpgen).getSignature().equals("D")) {
                newPoolIndex = cpgen.addDouble(secondResult);
            } else if(((TypedInstruction)match[0].getInstruction()).getType(cpgen).getSignature().equals("F") || ((TypedInstruction)match[3].getInstruction()).getType(cpgen).getSignature().equals("F") || ((TypedInstruction)match[5].getInstruction()).getType(cpgen).getSignature().equals("F")) {
                newPoolIndex = cpgen.addFloat(secondResult.floatValue());
            } else if(((TypedInstruction)match[0].getInstruction()).getType(cpgen).getSignature().equals("L") || ((TypedInstruction)match[3].getInstruction()).getType(cpgen).getSignature().equals("L") || ((TypedInstruction)match[5].getInstruction()).getType(cpgen).getSignature().equals("L")) {
                newPoolIndex = cpgen.addLong(secondResult.longValue());
            } else { //int
                newPoolIndex = cpgen.addInteger(secondResult.intValue());
            }

            //Set unused constants to null
            if (match[3].getInstruction() instanceof LDC || match[3].getInstruction() instanceof LDC2_W) {
                cpgen.setConstant(((IndexedInstruction) match[3].getInstruction()).getIndex(), null);
            }
            if (match[5].getInstruction() instanceof LDC || match[5].getInstruction() instanceof LDC2_W) {
                cpgen.setConstant(((IndexedInstruction) match[5].getInstruction()).getIndex(), null);
            }

            //Set left constant handle to point to new index
            LDC newInstruction = new LDC(newPoolIndex);
            match[3].setInstruction(newInstruction);

            //Delete other handles
            try {
                instructionList.delete(match[2]);
                instructionList.delete(match[4], match[6]);
            } catch (TargetLostException e) {
                e.printStackTrace();
            }

            System.out.println("==================================");
        }
        // END OF CONSTANT VARIABLE FOLDING TEST/PRACTICE

        for(InstructionHandle handle : instructionList.getInstructionHandles()) {
            //TODO When x is an integer type, the value of 0 * x is zero even if the compiler does not know the value of x.

            if(handle.getInstruction() instanceof LDC) {
            }
        }

        // setPositions(true) checks whether jump handles
        // are all within the current method
        instructionList.setPositions(true);

        // set max stack/local
        methodGen.setMaxStack();
        methodGen.setMaxLocals();

        // generate the new method with optimised instructions
        Method newMethod = methodGen.getMethod();

        // replace the method in the original class
        cgen.replaceMethod(method, newMethod);
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