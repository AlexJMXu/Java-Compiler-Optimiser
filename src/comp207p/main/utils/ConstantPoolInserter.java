package comp207p.main.utils;

import org.apache.bcel.generic.*;

public class ConstantPoolInserter {

    /**
     * Inserts the folded value into constant pool
     * @param left
     * @param right
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
            return 'B';
        } else {
            throw new RuntimeException("Type not defined");
        }
    }

    /**
     * Fold an arithmetic operation and get the result
     * @param operation Arithmetic operation e.g. IADD, DMUL, etc.
     * @param left Left value of binary operator
     * @param right Right side of binary operator
     * @return Result of the calculation
     */
    public static double foldOperation(ArithmeticInstruction operation, Number left, Number right) {
        if(operation instanceof IADD || operation instanceof FADD || operation instanceof LADD || operation instanceof DADD) {
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
}
