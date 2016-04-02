package comp207p.main;

/**
 * Optimise a single class only for debugging
 */
public class OptimiseSingleClass {

    /**
     * 1: ClassName e.g "SimpleFolding.class"
     * @param args
     */
    public static void main(String args[]) {
        String className = args[0];
        String inputPath = "build/classes/comp207p/target/" + className;
        String optimisedPath = "optimised/classes/comp207p/target/" + className;

        System.out.format("Optimising %s\n", className);

        ConstantFolder cf = new ConstantFolder(inputPath);
        cf.write(optimisedPath);
    }
}
