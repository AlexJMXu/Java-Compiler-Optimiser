package comp207p.main.utils;

import org.apache.bcel.generic.*;

public class ForLoopChecker {
    /*public static boolean checkDynamicVariable(InstructionHandle h, InstructionList list) {
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
    }*/

    public static boolean checkDynamicVariable(InstructionHandle h, InstructionList list) {
        Instruction checkingInstruction = h.getInstruction();
        Instruction currentInstruction, currentSubInstruction;
        InstructionHandle handleIterator = h;
        while(handleIterator != null) {
            try {
                handleIterator = handleIterator.getPrev();
                currentInstruction = handleIterator.getInstruction();
                if (currentInstruction instanceof StoreInstruction 
                    && ((StoreInstruction)currentInstruction).getIndex() == ((LoadInstruction)checkingInstruction).getIndex()) {
                    InstructionHandle subIterator = handleIterator;
                    while (subIterator != null) {
                        subIterator = subIterator.getPrev();
                        currentSubInstruction = subIterator.getInstruction();
                        if (currentSubInstruction instanceof BranchInstruction) {
                            if (((BranchInstruction) currentSubInstruction).getTarget().getPosition() > handleIterator.getPosition()) {
                                return true;
                            } else {
                                System.out.println(((BranchInstruction) currentSubInstruction).getTarget().getPosition());
                                System.out.println(handleIterator.getPosition());
                                return false;
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
