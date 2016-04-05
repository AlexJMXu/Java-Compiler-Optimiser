package comp207p.main.utils;

import org.apache.bcel.generic.*;

public class ForLoopChecker {
    public static boolean checkDynamicVariable(InstructionHandle h, InstructionList list) {
        InstructionHandle handleIterator = list.getStart();
        while(handleIterator != null) {
            try {
                handleIterator = handleIterator.getNext();
                if (handleIterator.getInstruction() instanceof GotoInstruction
                        && (handleIterator.getPrev().getInstruction() instanceof IINC
                        || handleIterator.getPrev().getInstruction() instanceof StoreInstruction)) {
                    if (((BranchInstruction) handleIterator.getInstruction()).getTarget().getInstruction().equals(h.getInstruction())) {
                        return true;
                    } else {
                        InstructionHandle subIterator = handleIterator;
                        while (subIterator != null) {
                            subIterator = subIterator.getPrev();
                            if (subIterator.getInstruction() instanceof StoreInstruction) {
                                if (((StoreInstruction)subIterator.getInstruction()).getIndex() == ((LoadInstruction)h.getInstruction()).getIndex()) {
                                    return true;
                                }
                            } else {
                                if (subIterator.equals(h)) {
                                    break;
                                }
                            }
                        }
                    }
                }
            } catch (NullPointerException e) {
                break;
            }
        }

        return false;
    }
}
