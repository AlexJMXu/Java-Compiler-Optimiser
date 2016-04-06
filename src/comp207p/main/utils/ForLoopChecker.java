package comp207p.main.utils;

import org.apache.bcel.generic.*;

public class ForLoopChecker {
    public static boolean checkDynamicVariable(InstructionHandle h, InstructionList list) {
        Instruction checkingInstruction = h.getInstruction();
        Instruction currentInstruction, previousInstruction, currentSubInstruction;
        InstructionHandle handleIterator = list.getStart();
        while(handleIterator != null) {
            try {
                handleIterator = handleIterator.getNext();
                currentInstruction = handleIterator.getInstruction();
                previousInstruction = handleIterator.getPrev().getInstruction();
                if (currentInstruction instanceof GotoInstruction
                        && (previousInstruction instanceof IINC
                        || previousInstruction instanceof StoreInstruction)
                        && (handleIterator.getPosition() > ((BranchInstruction) currentInstruction).getTarget().getPosition())) {
                    if (((BranchInstruction) currentInstruction).getTarget().getInstruction().equals(checkingInstruction)) {
                        return true;
                    }
                    InstructionHandle subIterator = handleIterator;
                    while (subIterator != null) {
                        subIterator = subIterator.getPrev();
                        currentSubInstruction = subIterator.getInstruction();
                        if (currentSubInstruction instanceof StoreInstruction) {
                            if (((StoreInstruction)currentSubInstruction).getIndex() == ((LoadInstruction)checkingInstruction).getIndex()) {
                                return true;
                            }
                        } else {
                            if (subIterator.equals((InstructionHandle) ((BranchInstruction)handleIterator.getInstruction()).getTarget())) {
                                break;
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
