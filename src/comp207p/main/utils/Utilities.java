package comp207p.main.utils;

import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.ConstantString;
import org.apache.bcel.classfile.ConstantUtf8;

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
}
