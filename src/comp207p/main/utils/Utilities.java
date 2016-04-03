package comp207p.main.utils;

import comp207p.main.exceptions.UnableToFetchValueException;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.ConstantString;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.LoadInstruction;

public class Utilities {
    /**
     * Print out constants for debugging
     * @param cp Constant Pool
     */
    public static void printConstants(ConstantPool cp) {
        Constant[] constants = cp.getConstantPool();
        int constantCount = 0;
        for(Constant c : constants) {
            if((c == null) || (c instanceof ConstantString) || (c instanceof ConstantUtf8)) continue; //ignore these constant types

            System.out.println(c);

            constantCount++;
        }

        System.out.format("Total constants: %d\n", constantCount);
    }

    public static void printInstructionHandles(InstructionHandle[] handles, ConstantPoolGen cpgen) {
        for(InstructionHandle h : handles) {
            if(h.getInstruction() instanceof LoadInstruction) {
                try {
                    System.out.format("%s | Val: %s\n", h, ValueLoader.getLoadInstructionValue(h, cpgen));
                } catch (UnableToFetchValueException e) {
                    System.out.format("%s | Val: Could not get\n", h);
                }
            } else {
                System.out.println(h);
            }
        }
    }
}
