package comp207p.main.utils;

import org.apache.bcel.generic.*;

public class ConstantPoolInserter {

    /**
     * Inserts the folded value into constant pool
     * @param cpgen
     * @return Index of newly inserted constant
     */
    public static int insert(Double value, char type, ConstantPoolGen cpgen) {
        switch (type) {
            case 'D':
                return cpgen.addDouble(value);
            case 'F':
                return cpgen.addFloat(value.floatValue());
            case 'J':
                return cpgen.addLong(value.longValue());
            case 'I':
                return cpgen.addInteger(value.intValue());
            case 'B':
                return cpgen.addInteger(value.intValue()); //Promote byte to integer
            default:
                throw new RuntimeException("Type not defined");
        }
    }

    public static char getFoldedConstantSignature(InstructionHandle left, InstructionHandle right, ConstantPoolGen cpgen) {
        //Identify the type of the constant
        if(Signature.checkSignature(left, right, cpgen, "D")) { //double
            return 'D';
        } else if(Signature.checkSignature(left, right, cpgen, "F")) { //float
            return 'F';
        } else if(Signature.checkSignature(left, right, cpgen, "J")) { //J is the signature for long, wtf
            return 'J';
        } else if(Signature.checkSignature(left, right, cpgen, "I")) { //int
            return 'I';
        } else if(Signature.checkSignature(left, right, cpgen, "B")) {
            return 'I'; //Promote byte to integer
        } else {
            throw new RuntimeException("Type not defined");
        }
    }

}
