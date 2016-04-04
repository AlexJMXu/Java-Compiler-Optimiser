package comp207p.main.utils;

import org.apache.bcel.generic.*;

public class ComparisonChecker {
    /**
     * Integer comparison by checking the type of comparison (such as if integers equal, IF_ICMPEQ)
     * After identifying the type, compares the values, and returns 1 or 0 accordingly
     * @param comparison Comparison type for the integers
     * @param leftValue Left value of the comparison
     * @param rightValue Right value of the comparison
     * @return Comparison result
     */
    public static int checkIntComparison(IfInstruction comparison, Number leftValue, Number rightValue) {
        if (comparison instanceof IF_ICMPEQ) { // if value 1 equals value 2
            if (leftValue.intValue() == rightValue.intValue()) return 1;
            else return 0;
        } else if (comparison instanceof IF_ICMPGE) { // if value 1 greater than or equal to to value 2
            if (leftValue.intValue() >= rightValue.intValue()) return 1;
            else return 0;
        } else if (comparison instanceof IF_ICMPGT) { // if value 1 greater than value 2
            if (leftValue.intValue() > rightValue.intValue()) return 1;
            else return 0;
        } else if (comparison instanceof IF_ICMPLE) { // if value 1 less than or equal to value 2
            if (leftValue.intValue() <= rightValue.intValue()) return 1;
            else return 0;
        } else if (comparison instanceof IF_ICMPLT) { // if value 1 less than value 2
            if (leftValue.intValue() < rightValue.intValue()) return 1;
            else return 0;
        } else if (comparison instanceof IF_ICMPNE) { // if value 1 not equal to value 2
            if (leftValue.intValue() != rightValue.intValue()) return 1;
            else return 0;
        } else {
            throw new RuntimeException("Comparison not defined");
        }
    }

    /**
     * Comparison for non-integers by checking the type of comparison
     * After identifying the type, compares the values, and returns 1 or -1 accordingly (includes 0 for long compare)
     * @param comparison Comparison type such as DCMPG
     * @param leftValue Left value of the comparison
     * @param rightValue Right value of the comparison
     * @return Comparison result
     */
    public static int checkFirstComparison(InstructionHandle comparison, Number leftValue, Number rightValue) {
        if (comparison.getInstruction() instanceof DCMPG) { //if double 1 greater than double 2
            if (leftValue.doubleValue() > rightValue.doubleValue()) return 1;
            else return -1;
        } else if (comparison.getInstruction()  instanceof DCMPL) { //if double 1 less than double 2
            if (leftValue.doubleValue() < rightValue.doubleValue()) return -1;
            else return 1;
        } else if (comparison.getInstruction()  instanceof FCMPG) { //if float 1 greater than float 2
            if (leftValue.floatValue() > rightValue.floatValue()) return 1;
            else return -1;
        } else if (comparison.getInstruction()  instanceof FCMPL) { //if float 1 less than float 2
            if (leftValue.floatValue() < rightValue.floatValue()) return -1;
            else return 1;
        } else if (comparison.getInstruction()  instanceof LCMP) { //long comparison, 0 if equal, 1 if long 1 greater than long 2, -1 if long 1 less than long 2
            if (leftValue.longValue() == rightValue.longValue()) return 0;
            else if (leftValue.longValue() > rightValue.longValue()) return 1;
            else return -1;
        } else {
            throw new RuntimeException("Comparison not defined");
        }
    }

    /**
     * If comparison
     * After identifying the type of if comparison, compares the values, and returns 1 or 0 accordingly
     * @param comparison Comparison type for the integers
     * @param value Value passed from first comparison
     * @return Comparison result
     */
    public static int checkSecondComparison(IfInstruction comparison, int value) {
        if (comparison instanceof IFEQ) { //if equal
            if (value == 0) return 1;
            else return 0;
        } else if (comparison instanceof IFGE) { //if greater than or equal
            if (value >= 0) return 1;
            else return 0;
        } else if (comparison instanceof IFGT) { //if greater than
            if (value > 0) return 1;
            else return 0;
        } else if (comparison instanceof IFLE) { //if less than or equal
            if (value <= 0) return 1;
            else return 0;
        } else if (comparison instanceof IFLT) { //if less than
            if (value < 0) return 1;
            else return 0;
        } else if (comparison instanceof IFNE) { //if not equal
            if (value != 0) return 1;
            else return 0;
        } else {
            throw new RuntimeException("Comparison not defined");
        }
    }
}
