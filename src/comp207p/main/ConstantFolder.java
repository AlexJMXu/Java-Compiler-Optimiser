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

        InstructionFinder finder = new InstructionFinder(instructionList);
        for(Iterator it = finder.search("LDC LDC ArithmeticInstruction"); it.hasNext();) {
            InstructionHandle[] match = (InstructionHandle[]) it.next();
            System.out.println("==================================");
            System.out.println("Found optimisable instruction set");
            for(InstructionHandle h : match) {

                System.out.println(h);
            }

            LDC left = (LDC) match[0].getInstruction();
            LDC right = (LDC) match[1].getInstruction();
            ArithmeticInstruction operation = (ArithmeticInstruction) match[2].getInstruction();

            System.out.println(left.getType(cpgen));
            System.out.println(operation);
            System.out.println(right.getType(cpgen));

            Double result = foldOperation(operation,(Number) left.getValue(cpgen), (Number) right.getValue(cpgen));
            System.out.format("Folding to value %f\n", result);

            int poolIndex = -1;
            if(left.getType(cpgen).getSignature().equals("D") || (right.getType(cpgen).getSignature().equals("D"))) {

            } else if(left.getType(cpgen).getSignature().equals("F") || (right.getType(cpgen).getSignature().equals("F"))) {

            } else if(left.getType(cpgen).getSignature().equals("L") || (right.getType(cpgen).getSignature().equals("L"))) {

            } else { //int
                poolIndex = cpgen.addInteger(result.intValue());
            }

            //Set left constant handle to point to new index
            left.setIndex(poolIndex);

            //Delete other handles
            try {
                instructionList.delete(match[1], match[2]);
            } catch (TargetLostException e) {
                e.printStackTrace();
            }

            //TODO Delete unused constants
            System.out.println("==================================");

        }

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

    private double foldOperation(ArithmeticInstruction operation, Number left, Number right) {
        if(operation instanceof IADD || operation instanceof  FADD || operation instanceof LADD || operation instanceof DADD) {
            return left.doubleValue() + right.doubleValue();
        } else {
            throw new RuntimeException("Not supported operation");
        }

        //TODO Implement div, sub, mul
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