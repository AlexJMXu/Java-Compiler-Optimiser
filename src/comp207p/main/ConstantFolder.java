package comp207p.main;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;

import comp207p.main.exceptions.UnableToFetchValueException;
import comp207p.main.utils.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;
import org.apache.bcel.util.InstructionFinder;

import static comp207p.main.utils.ForLoopChecker.checkDynamicVariable;


public class ConstantFolder
{
    ClassParser parser = null;
    ClassGen gen = null;

    JavaClass original = null;
    JavaClass optimized = null;

    //Regex for matching an instruction that pushes a value onto the stack
    private static final String LOAD_INSTRUCTION_REGEXP = "(ConstantPushInstruction|LDC|LDC2_W|LoadInstruction)";

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
            optimiseCounter += optimiseComparisons(instructionList, cpgen); //Add number of comparison optimisations made
            optimiseCounter += optimiseNegations(instructionList, cpgen);
            optimiseCounter += optimiseArithmeticOperation(instructionList, cpgen); //Add number of arithmetic optimisations made
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
     * Fold negations
     * @param instructionList
     * @param cpgen
     * @return
     */
    private int optimiseNegations(InstructionList instructionList, ConstantPoolGen cpgen) {
        int changeCounter = 0;

        String regExp = LOAD_INSTRUCTION_REGEXP + " (INEG|FNEG|LNEG|DNEG)";

        // Search for instruction list where two constants are loaded from the pool, followed by an arithmetic
        InstructionFinder finder = new InstructionFinder(instructionList);

        for(Iterator it = finder.search(regExp); it.hasNext();) { // Iterate through instructions to look for arithmetic optimisation
            InstructionHandle[] match = (InstructionHandle[]) it.next();

            //Debug output
            System.out.println("==================================");
            System.out.println("Found optimisable negation");

            InstructionHandle loadInstruction = match[0];
            InstructionHandle negationInstruction = match[1];

            String type = comp207p.main.utils.Signature.getInstructionSignature(negationInstruction, cpgen);

            Utilities.printInstructionHandles(match, cpgen, instructionList, type);

            Number value = ValueLoader.getValue(loadInstruction, cpgen, instructionList, type);

            //Multiply by -1 to negate it, inefficient but oh well
            Number negatedValue = Utilities.foldOperation(new DMUL(), value, -1);

            System.out.format("Folding to value %s | Type: %s\n", negatedValue, type);

            int newPoolIndex = ConstantPoolInserter.insert(negatedValue, type, cpgen);

            //Set left constant handle to point to new index
            ConstantPoolInserter.replaceInstructionHandleWithLoadConstant(loadInstruction, type, newPoolIndex);

            //Delete other handles
            try {
                instructionList.delete(match[1]);
            } catch (TargetLostException e) {
                e.printStackTrace();
            }

            System.out.println("==================================");
            changeCounter++;

        }

        return changeCounter;
    }

    /**
     * Optimise arithmetic operations
     * @param instructionList Instruction list
     * @return Number of changes made to instructions
     */
    private int optimiseArithmeticOperation(InstructionList instructionList, ConstantPoolGen cpgen) {
        int changeCounter = 0;

        String regExp = LOAD_INSTRUCTION_REGEXP + " (ConversionInstruction)? " +
                LOAD_INSTRUCTION_REGEXP + " (ConversionInstruction)? " +
                "ArithmeticInstruction";

        // Search for instruction list where two constants are loaded from the pool, followed by an arithmetic
        InstructionFinder finder = new InstructionFinder(instructionList);

        for(Iterator it = finder.search(regExp); it.hasNext();) { // Iterate through instructions to look for arithmetic optimisation
            InstructionHandle[] match = (InstructionHandle[]) it.next(); 

            //Debug output
            System.out.println("==================================");
            System.out.println("Found optimisable arithmetic set");

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

            if (leftInstruction.getInstruction() instanceof LoadInstruction) { //Recognise for loops
                if (checkDynamicVariable(leftInstruction, instructionList)) {
                    Utilities.printDynamicVariableDetected();
                    continue;
                }
            }
            if (rightInstruction.getInstruction() instanceof LoadInstruction) {
                if (checkDynamicVariable(rightInstruction, instructionList)) {
                    Utilities.printDynamicVariableDetected();
                    continue;
                }
            }

            //Get the signature of the folded value
            String type = ConstantPoolInserter.getFoldedConstantSignature(leftInstruction, rightInstruction, cpgen);

            Utilities.printInstructionHandles(match, cpgen, instructionList, type);

            //Fetch values for push instructions
            try {
                leftValue = ValueLoader.getValue(leftInstruction, cpgen, instructionList, type);
                rightValue = ValueLoader.getValue(rightInstruction, cpgen, instructionList, type);
            } catch (UnableToFetchValueException e) {
                Utilities.printDynamicVariableDetected();
                continue;
            }

            ArithmeticInstruction operation = (ArithmeticInstruction) operationInstruction.getInstruction();

            Number foldedValue = Utilities.foldOperation(operation, leftValue, rightValue); //Perform the operation on the two values

            System.out.format("Folding to value %s | Type: %s\n", foldedValue, type);

            //Insert new constant into pool
            int newPoolIndex = ConstantPoolInserter.insert(foldedValue, type, cpgen);

            //Set left constant handle to point to new index
            ConstantPoolInserter.replaceInstructionHandleWithLoadConstant(leftInstruction, type, newPoolIndex);

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

    /**
     * Optimise comparison instructions
     * @param instructionList Instruction list
     * @return Number of changes made to instructions
     */
    private int optimiseComparisons(InstructionList instructionList, ConstantPoolGen cpgen) { //Iterate through instructions to look for comparison optimisations
        int changeCounter = 0;
        String regExp = LOAD_INSTRUCTION_REGEXP + " (ConversionInstruction)?" +
                        LOAD_INSTRUCTION_REGEXP + "?" + " (ConversionInstruction)?" +
                        "(LCMP|DCMPG|DCMPL|FCMPG|FCMPL)? IfInstruction (ICONST GOTO ICONST)?";

        InstructionFinder finder = new InstructionFinder(instructionList);

        for(Iterator it = finder.search(regExp); it.hasNext();) { // I
            InstructionHandle[] match = (InstructionHandle[]) it.next();

            //Debug output
            System.out.println("==================================");
            System.out.println("Found optimisable comparison set");

            Number leftValue = 0, rightValue = 0;
            InstructionHandle leftInstruction = null, rightInstruction = null, compare = null, comparisonInstruction = null;

            //Get instructions
            leftInstruction = match[0]; //Left instruction is always first match
            if (match[1].getInstruction() instanceof ConversionInstruction
                && !(match[2].getInstruction() instanceof IfInstruction)) { 
                rightInstruction = match[2]; //If conversion exists for left, then right instruction occurs after it
            } else if (!(match[1].getInstruction() instanceof IfInstruction)) {
                rightInstruction = match[1]; //No conversion instruction for left
            } else {
                rightInstruction = null;
            }

            int matchCounter = 0;
            if (rightInstruction != null) {
                if (rightInstruction == match[2] 
                    && match[3].getInstruction() instanceof ConversionInstruction) { //If left has conversion and conversion exists for right
                    matchCounter = 2; 
                } else if (rightInstruction == match[2] 
                    || (rightInstruction == match[1]
                    && match[2].getInstruction() instanceof ConversionInstruction)) { //Left has conversion or right has conversion
                        matchCounter = 1;
                } else {
                    matchCounter = 0; //No conversion for either instruction
                }
            } else {
                if (!(match[1].getInstruction() instanceof ConversionInstruction)) {
                    matchCounter = -1;
                }
            }

            if (leftInstruction.getInstruction() instanceof LoadInstruction) { //Recognise for loops
                if (checkDynamicVariable(leftInstruction, instructionList)) {
                    Utilities.printDynamicVariableDetected();
                    continue;
                }
            }
            if (rightInstruction != null && rightInstruction.getInstruction() instanceof LoadInstruction) {
                if (checkDynamicVariable(rightInstruction, instructionList)) {
                    Utilities.printDynamicVariableDetected();
                    continue;
                }
            }

            if (match[2+matchCounter].getInstruction() instanceof IfInstruction) { //If the following instruction after left and right is an IfInstruction (meaning integer comparison), such as IF_ICMPGE
                comparisonInstruction = match[2+matchCounter];
            } else {
                compare = match[2+matchCounter]; //Comparison for non-integers, such as LCMP
                comparisonInstruction = match[3+matchCounter]; //IfInstruction
            }

            String type;
            if(rightInstruction != null) {
                type = ConstantPoolInserter.getFoldedConstantSignature(leftInstruction, rightInstruction, cpgen);
            } else {
                type = comp207p.main.utils.Signature.getInstructionSignature(leftInstruction, cpgen);
            }

            Utilities.printInstructionHandles(match, cpgen, instructionList, type);

            System.out.println("Comparison instruction type: " + type);

            //Fetch values for push instructions
            try {
                leftValue = ValueLoader.getValue(leftInstruction, cpgen, instructionList, type);
                if (rightInstruction != null) { 
                    rightValue = ValueLoader.getValue(rightInstruction, cpgen, instructionList, type);
                }
            } catch (UnableToFetchValueException e) {
                Utilities.printDynamicVariableDetected();
                continue;
            }

            IfInstruction comparison = (IfInstruction) comparisonInstruction.getInstruction();

            int result;

            if (rightInstruction != null) {
                if (comparisonInstruction == match[2]) { //Integer comparison
                    result = ComparisonChecker.checkIntComparison(comparison, leftValue, rightValue);
                } else { //Non-integer type comparison
                    result = ComparisonChecker.checkFirstComparison(compare, leftValue, rightValue);
                    result = ComparisonChecker.checkSecondComparison(comparison, result);
                }
            } else {
                result = ComparisonChecker.checkSecondComparison(comparison, leftValue.intValue());
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
                if (match[match.length-1].getInstruction() instanceof IfInstruction) {
                    InstructionHandle tempHandle = (InstructionHandle) ((BranchInstruction)comparisonInstruction.getInstruction()).getTarget().getPrev();
                    if (result == 1) {
                        instructionList.delete(match[0], comparisonInstruction);
                        if (tempHandle.getInstruction() instanceof GotoInstruction) {
                            InstructionHandle gotoTarget = (InstructionHandle) ((BranchInstruction)tempHandle.getInstruction()).getTarget().getPrev();
                            instructionList.delete(tempHandle, gotoTarget);
                        }
                    } else {
                        instructionList.delete(match[0], tempHandle);
                    }
                } else {
                    instructionList.delete(match[1], match[match.length-1]);
                }
            } catch (TargetLostException e) {
                e.printStackTrace();
            }

            System.out.println("==================================");
            changeCounter++; //Optimisation found
            break;
        }

        return changeCounter;
    }

}