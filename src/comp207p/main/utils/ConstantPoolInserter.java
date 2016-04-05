package comp207p.main.utils;

import org.apache.bcel.generic.*;

public class ConstantPoolInserter {

    /**
     * Inserts the folded value into constant pool
     * @param cpgen
     * @return Index of newly inserted constant
     */
    public static int insert(Double value, String type, ConstantPoolGen cpgen) {
        switch (type) {
            case "D":
                return cpgen.addDouble(value);
            case "F":
                return cpgen.addFloat(value.floatValue());
            case "J":
                return cpgen.addLong(value.longValue());
            case "I":
                return cpgen.addInteger(value.intValue());
            case "S":
                return cpgen.addInteger(value.intValue()); //Promote short to integer    
            case "B":
                return cpgen.addInteger(value.intValue()); //Promote byte to integer
            default:
                throw new RuntimeException("Type not defined");
        }
    }

    /**
     * Replace an instruction handle with a new load constant instruction
     * @param h
     * @param type
     * @param poolIndex
     */
    public static void replaceInstructionHandleWithLoadConstant(InstructionHandle h, String type, int poolIndex) {
        //Set left constant handle to point to new index
        if (type.equals("F") || type.equals("I")) { //Float or integer
            LDC newInstruction = new LDC(poolIndex);
            h.setInstruction(newInstruction);
        } else { //Types larger than integer use LDC2_W
            LDC2_W newInstruction = new LDC2_W(poolIndex);
            h.setInstruction(newInstruction);
        }
    }

    public static String getFoldedConstantSignature(InstructionHandle left, InstructionHandle right, ConstantPoolGen cpgen) {
        //Identify the type of the constant
        if(Signature.checkSignature(left, right, cpgen, "D")) { //double
            return "D";
        } else if(Signature.checkSignature(left, right, cpgen, "F")) { //float
            return "F";
        } else if(Signature.checkSignature(left, right, cpgen, "J")) { //J is the signature for long, wtf
            return "J";
        } else if(Signature.checkSignature(left, right, cpgen, "S")) { //short
            return "S";
        } else if(Signature.checkSignature(left, right, cpgen, "I")) { //int
            return "I";
        } else if(Signature.checkSignature(left, right, cpgen, "B")) {
            return "I"; //Promote byte to integer
        } else {
            throw new RuntimeException("Type not defined");
        }
    }

}
