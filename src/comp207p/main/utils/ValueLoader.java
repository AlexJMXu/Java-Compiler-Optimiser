package comp207p.main.utils;

import comp207p.main.exceptions.UnableToFetchValueException;
import org.apache.bcel.generic.*;

public class ValueLoader {

    /**
     * Get value for any instruction handle that pushes a value onto the stack
     * @param h The load instruction fetch the value from
     * @param cpgen Constant pool of the class
     * @return Instruction handle value
     */
    public static Number getValue(InstructionHandle h, ConstantPoolGen cpgen) throws UnableToFetchValueException {
        Instruction instruction = h.getInstruction();
        if(instruction instanceof LoadInstruction) {
            return ValueLoader.getLoadInstructionValue(h, cpgen);
        } else {
            return ValueLoader.getConstantValue(h, cpgen);
        }
    }

    /**
     * Get Number value from InstructionHandle loading constant from constant pool
     * @param h The load instruction fetch the value from
     * @param cpgen Constant pool of the class
     * @return InstructionHandle value
     */
    public static Number getConstantValue(InstructionHandle h, ConstantPoolGen cpgen) {
        Number value;

        if (h.getInstruction() instanceof ConstantPushInstruction) {
            value = ((ConstantPushInstruction) h.getInstruction()).getValue();
        } else if (h.getInstruction() instanceof LDC) {
            value = (Number) ((LDC) h.getInstruction()).getValue(cpgen);
        } else if (h.getInstruction() instanceof LDC2_W) {
            value = ((LDC2_W) h.getInstruction()).getValue(cpgen);
        } else {
            throw new RuntimeException();
        }

        return value;
    }

    /**
     * Get the value of a load instruction, e.g. iload_2
     * @param h The load instruction fetch the value from
     * @param cpgen Constant pool of the class
     * @return Load instruction value
     */
    public static Number getLoadInstructionValue(InstructionHandle h, ConstantPoolGen cpgen) throws UnableToFetchValueException {
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
            throw new UnableToFetchValueException("Cannot fetch value for this type of object");
        }
    }
}
