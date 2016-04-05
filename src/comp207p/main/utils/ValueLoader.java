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
    public static Number getValue(InstructionHandle h, ConstantPoolGen cpgen, InstructionList list) throws UnableToFetchValueException {
        Instruction instruction = h.getInstruction();
        if(instruction instanceof LoadInstruction) {
            return ValueLoader.getLoadInstructionValue(h, cpgen, list);
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
    public static Number getLoadInstructionValue(InstructionHandle h, ConstantPoolGen cpgen, InstructionList list) throws UnableToFetchValueException {
        Instruction instruction = h.getInstruction();
        if(!(instruction instanceof LoadInstruction)) {
            throw new RuntimeException("InstructionHandle has to be of type LoadInstruction");
        }

        int localVariableIndex = ((LocalVariableInstruction) instruction).getIndex();

        InstructionHandle handleIterator = h;
        int incrementAccumulator = 0;
        while(!(instruction instanceof StoreInstruction) || ((StoreInstruction) instruction).getIndex() != localVariableIndex) {

            //If there is an IINC while scanning, we need to accumulate that on top of the ISTORE value
            if(instruction instanceof IINC) {
                IINC increment = (IINC) instruction;

                if(increment.getIndex() == localVariableIndex) {
                    System.out.println("Found increment instruction");

                    //If it's in a for loop, we cannot get the value
                    if(ForLoopChecker.checkDynamicVariable(h, list)) {
                        throw new UnableToFetchValueException("IINC in for loop");
                    }

                    System.out.format("%s | Incrementing by %d | Index: %d\n\n", increment, increment.getIncrement(), increment.getIndex());
                    incrementAccumulator += increment.getIncrement();
                }
            }

            handleIterator = handleIterator.getPrev();
            instruction = handleIterator.getInstruction();
        }

        //Go back previous one more additional time to fetch constant push instruction
        handleIterator = handleIterator.getPrev();
        instruction = handleIterator.getInstruction();

        Number storeValue;
        if(instruction instanceof ConstantPushInstruction) {
            storeValue = ((ConstantPushInstruction) instruction).getValue();
        } else if (instruction instanceof LDC) {
            storeValue = (Number) ((LDC) instruction).getValue(cpgen);
        } else if (instruction instanceof LDC2_W) {
            storeValue = ((LDC2_W) instruction).getValue(cpgen);
        } else {
            throw new UnableToFetchValueException("Cannot fetch value for this type of object");
        }

        return Utilities.foldOperation(new DADD(), storeValue, incrementAccumulator);
    }
}
