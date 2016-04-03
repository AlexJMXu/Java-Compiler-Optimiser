package comp207p.main;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;

import comp207p.main.utils.ConstantPoolInserter;
import comp207p.main.utils.Utilities;
import comp207p.main.utils.ValueLoader;
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

    /**
     * Initial method
     */
	public void optimize()
	{
		ClassGen cgen = new ClassGen(original);
        cgen.setMajor(50); //Set major version number of class file to 50 (instead of default 45) to eliminate StackMapFrame errors
		ConstantPoolGen cpgen = cgen.getConstantPool();

        ConstantPool cp = cpgen.getConstantPool();
        Method[] methods = cgen.getMethods();

        Utilities.printConstants(cp);

        for(Method m : methods) {
            System.out.println(m); //Print method name

            optimiseMethod(cgen, cpgen, m); //Optimise each method
        }
        
		this.optimized = cgen.getJavaClass();
	}

    /**
     * Optimise method instruction list
     */
    private void optimiseMethod(ClassGen cgen, ConstantPoolGen cpgen, Method method) {
        Code methodCode = method.getCode();

        if(methodCode != null) { // Non-abstract method
            System.out.println("Method code: " +  methodCode);
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

        int optimiseCounter = 1;
        
        // Run in while loop until no more optimisations can be made
        while (optimiseCounter > 0) {
            optimiseCounter = 0;
            optimiseCounter += optimiseArithmeticOperation(instructionList, cpgen); //Add number of arithmetic optimisations made
            optimiseCounter += optimiseComparisons(instructionList, cpgen); //Add number of comparison optimisations made
        }

        // setPositions(true) checks whether jump handles
        // are all within the current method
        instructionList.setPositions(true);

        // set max stack/local
        methodGen.setMaxStack();
        methodGen.setMaxLocals();

        // generate the new method with optimised instructions
        Method newMethod = methodGen.getMethod();

        System.out.println("Fully optimised instruction set:");
        Utilities.printConstants(cpgen.getConstantPool());
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

        String regExp = "(ConstantPushInstruction|LDC|LDC2_W|LoadInstruction) (ConversionInstruction)? " +
                "(ConstantPushInstruction|LDC|LDC2_W|LoadInstruction) (ConversionInstruction)? " +
                "ArithmeticInstruction";

        // Search for instruction list where two constants are loaded from the pool, followed by an arithmetic
        InstructionFinder finder = new InstructionFinder(instructionList);

        for(Iterator it = finder.search(regExp); it.hasNext();) { // Iterate through instructions to look for arithmetic optimisation
            InstructionHandle[] match = (InstructionHandle[]) it.next(); 

            //Debug output
            System.out.println("==================================");
            System.out.println("Found optimisable arithmetic set");
            for(InstructionHandle h : match) { 
                if(h.getInstruction() instanceof LoadInstruction) {
                    try {
                        System.out.format("%s | Val: %s\n", h, ValueLoader.getLoadInstructionValue(h, cpgen));
                    } catch (RuntimeException e) {
                        System.out.format("%s\n", h);
                    }
                } else {
                    System.out.println(h);
                }
            }

            Number leftValue, rightValue;
            InstructionHandle leftInstruction, rightInstruction, operationInstruction;

            //Get instructions
            leftInstruction = match[0]; //Left instruction is always first match
            if (match[1].getInstruction() instanceof ConversionInstruction) { 
                rightInstruction = match[2]; //If conversion exists for left, then right instruction occurs after it
            } else {
                rightInstruction = match[1]; //No conversion instruction for left
            }
            if (rightInstruction == match[2] && match[3].getInstruction() instanceof ConversionInstruction) { //If left has conversion and conversion exists for right
                operationInstruction = match[4]; 
            } else if (rightInstruction == match[2] || (rightInstruction == match[1] && match[2].getInstruction() instanceof ConversionInstruction)) { //Left has conversion or right has conversion
                operationInstruction = match[3]; 
            } else {
                operationInstruction = match[2]; //No conversion for either instruction
            }

            if (leftInstruction.getInstruction() instanceof LoadInstruction) { //Recognise for loops, stops at ISTORE for second condition as no need to check further
                if (checkIfForLoop(leftInstruction)) {
                    printForLoopDetected();
                    continue;
                }
            }
            if (rightInstruction.getInstruction() instanceof LoadInstruction) {
                if (checkIfForLoop(rightInstruction)) {
                    printForLoopDetected();
                    continue;
                }
            }

            //Fetch values for push instructions
            try {
                leftValue = ValueLoader.getValue(leftInstruction, cpgen);
                rightValue = ValueLoader.getValue(rightInstruction, cpgen);
            } catch (RuntimeException e) {
                printForLoopDetected();
                continue;
            }

            ArithmeticInstruction operation = (ArithmeticInstruction) operationInstruction.getInstruction();

            Double foldedValue = ConstantPoolInserter.foldOperation(operation, leftValue, rightValue); //Perform the operation on the two values
            System.out.format("Folding to value %f\n", foldedValue);

            //Get the signature of the folded value
            char type = ConstantPoolInserter.getFoldedConstantSignature(leftInstruction, rightInstruction, cpgen);
            System.out.format("Type: %c\n", type);

            //Insert new constant into pool
            int newPoolIndex = ConstantPoolInserter.insert(foldedValue, type, cpgen);

            //Set left constant handle to point to new index
            if (type == 'F' || type == 'I') { //Float or integer
                LDC newInstruction = new LDC(newPoolIndex);
                leftInstruction.setInstruction(newInstruction);
            } else { //Types larger than integer use LDC2_W
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
            changeCounter++; //Optimisation found

            break;
        }

        return changeCounter;
    }

    public boolean checkIfForLoop(InstructionHandle h) {
        InstructionHandle handleInterator = h;
        while(handleInterator != null) {
            try {
                handleInterator = handleInterator.getNext();
                if (handleInterator.getInstruction() instanceof GotoInstruction) {
                    if (((BranchInstruction) handleInterator.getInstruction()).getTarget().getInstruction().equals(h.getInstruction())) {
                        return true;
                    }
                } 
            } catch (NullPointerException e) {
                break;
            } 
        }

        return false;
    }

    /**
     * Optimise comparison instructions
     * @param instructionList Instruction list
     * @return Number of changes made to instructions
     */
    private int optimiseComparisons(InstructionList instructionList, ConstantPoolGen cpgen) { //Iterate through instructions to look for comparison optimisations
        int changeCounter = 0;
        String regExp = "(ConstantPushInstruction|LDC|LDC2_W|LoadInstruction)" +
                        "(ConstantPushInstruction|LDC|LDC2_W|LoadInstruction)" +
                        "(LCMP|DCMPG|DCMPL|FCMPG|FCMPL)* IfInstruction ICONST GOTO ICONST";

        InstructionFinder finder = new InstructionFinder(instructionList);

        for(Iterator it = finder.search(regExp); it.hasNext();) { // I
            InstructionHandle[] match = (InstructionHandle[]) it.next();

            //Debug output
            System.out.println("==================================");
            System.out.println("Found optimisable comparison set");
            changeCounter++; //Optimisation found
            for(InstructionHandle h : match) {
                if(h.getInstruction() instanceof LoadInstruction) {
                    System.out.format("%s | Val: %s\n", h, ValueLoader.getLoadInstructionValue(h, cpgen));
                } else {
                    System.out.println(h);
                }
            }

            Number leftValue, rightValue;
            InstructionHandle leftInstruction, rightInstruction, compare = null, comparisonInstruction;

            leftInstruction = match[0];
            rightInstruction = match[1];

            if (match[2].getInstruction() instanceof IfInstruction) { //If the following instruction after left and right is an IfInstruction (meaning integer comparison), such as IF_ICMPGE
                comparisonInstruction = match[2];
            } else {
                compare = match[2]; //Comparison for non-integers, such as LCMP
                comparisonInstruction = match[3]; //IfInstruction
            }

            //Fetch values for push instructions
            leftValue = ValueLoader.getValue(leftInstruction, cpgen);
            rightValue = ValueLoader.getValue(rightInstruction, cpgen);

            IfInstruction comparison = (IfInstruction) comparisonInstruction.getInstruction();

            int result = 0;

            if (comparisonInstruction == match[2]) { //Integer comparison
                result = checkIntComparison(comparison, leftValue, rightValue); 
            } else { //Non-integer type comparison
                result = checkFirstComparison(compare, leftValue, rightValue);
                result = checkSecondComparison(comparison, result);
            }

            //Set left constant handle to point to new index
            //1 -> 0 and 0 -> 1 due to instruction interpretation
            if (result == 1) {
                ICONST newInstruction = new ICONST(0);
                leftInstruction.setInstruction(newInstruction);
                result = 0;
            } else if (result == 0) {
                ICONST newInstruction = new ICONST(1);
                leftInstruction.setInstruction(newInstruction);
                result = 1;
            } else {
                ICONST newInstruction = new ICONST(-1);
                leftInstruction.setInstruction(newInstruction);
            }

            System.out.format("Folding return value to %d\n", result);

            //Delete other handles
            try {
                instructionList.delete(match[1], match[match.length-1]);
            } catch (TargetLostException e) {
                e.printStackTrace();
            }

            System.out.println("==================================");
            break;
        }

        return changeCounter;
    }

    private void printForLoopDetected() {
        System.out.println("For loop variable detected, no folding will occur.");
        System.out.println("==================================");
    }

    /**
     * Integer comparison by checking the type of comparison (such as if integers equal, IF_ICMPEQ)
     * After identifying the type, compares the values, and returns 1 or 0 accordingly
     * @param comparison Comparison type for the integers
     * @param leftValue Left value of the comparison
     * @param rightValue Right value of the comparison
     * @return Comparison result
     */
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

    /**
     * Comparison for non-integers by checking the type of comparison 
     * After identifying the type, compares the values, and returns 1 or 0 accordingly (includes -1 for long compare)
     * @param comparison Comparison type such as DCMPG 
     * @param leftValue Left value of the comparison
     * @param rightValue Right value of the comparison
     * @return Comparison result
     */
    private int checkFirstComparison(InstructionHandle comparison, Number leftValue, Number rightValue) {
        if (comparison.getInstruction() instanceof DCMPG) { //if double 1 greater than double 2
            if (leftValue.doubleValue() > rightValue.doubleValue()) return 1;
            else return 0;
        } else if (comparison.getInstruction()  instanceof DCMPL) { //if double 1 less than double 2
            if (leftValue.doubleValue() < rightValue.doubleValue()) return 1;
            else return 0;
        } else if (comparison.getInstruction()  instanceof FCMPG) { //if float 1 greater than float 2
            if (leftValue.floatValue() > rightValue.floatValue()) return 1;
            else return 0;
        } else if (comparison.getInstruction()  instanceof FCMPL) { //if float 1 less than float 2
            if (leftValue.floatValue() < rightValue.floatValue()) return 1;
            else return 0;
        } else if (comparison.getInstruction()  instanceof LCMP) { //long comparison, 0 if equal, 1 if long 1 greater than long 2, -1 if long 1 less than long 2
            if (leftValue.longValue() == rightValue.longValue()) return 0; 
            else if (leftValue.longValue() > rightValue.longValue()) return 1;
            else return -1;
        } else {
            throw new RuntimeException("Comparison not defined");
        }
    }

    /**
     * If comparison
     * After identifying the type of if comparison, compares the values, and returns 1 or 0 accordingly
     * @param comparison Comparison type for the integers
     * @param value Value passed from first comparison
     * @return Comparison result
     */
    private int checkSecondComparison(IfInstruction comparison, int value) {
        if (comparison instanceof IFEQ) { //if equal
            if (value == 0) return 1;
            else return 0;
        } else if (comparison instanceof IFGE) { //if greater than or equal
            if (value >= 0) return 1;
            else return 0;
        } else if (comparison instanceof IFGT) { //if greater than
            if (value > 0) return 1;
            else return 0;
        } else if (comparison instanceof IFLE) { //if less than or equal
            if (value <= 0) return 1;
            else return 0;
        } else if (comparison instanceof IFLT) { //if less than
            if (value < 0) return 1;
            else return 0;
        } else if (comparison instanceof IFNE) { //if not equal
            if (value != 0) return 1;
            else return 0;
        } else {
            throw new RuntimeException("Comparison not defined");
        }
    }

}