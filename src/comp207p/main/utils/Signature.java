package comp207p.main.utils;

import org.apache.bcel.generic.*;

public class Signature {
    /**
     * Get the signature of a load instruction, e.g. iload_1 would return String "I"
     * @param h The load instruction fetch the value from
     * @param cpgen Constant pool of the class
     * @return Load instruction value signature
     */
    public static String getLoadInstructionSignature(InstructionHandle h, ConstantPoolGen cpgen) {
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
    public static boolean checkSignature(InstructionHandle left, InstructionHandle right, ConstantPoolGen cpgen, String signature) {
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
}
