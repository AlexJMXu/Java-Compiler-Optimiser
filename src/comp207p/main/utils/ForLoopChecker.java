package comp207p.main.utils;

import org.apache.bcel.generic.*;

public class ForLoopChecker {
    public static boolean checkIfForLoop(InstructionHandle h) {
        InstructionHandle handleInterator = h;
        while(handleInterator != null) {
            try {
                handleInterator = handleInterator.getNext();
                if (handleInterator.getInstruction() instanceof GotoInstruction
                        && (handleInterator.getPrev().getInstruction() instanceof IINC
                        || handleInterator.getPrev().getInstruction() instanceof StoreInstruction)) {
                    if (((BranchInstruction) handleInterator.getInstruction()).getTarget().getInstruction().equals(h.getInstruction())) {
                        return true;
                    }
                }
            } catch (NullPointerException e) {
                break;
            }
        }

        return false;
    }
}
