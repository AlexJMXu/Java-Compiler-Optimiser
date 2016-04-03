package comp207p.target;

public class WhileLoop {
    public double foo() {
        double a = 2.2232;

        int i = 0;
        double you = 0;
        while(i < 10) {
            you = a * i + 4;
            System.out.println(you + 4);
            i+= 2;
        }

        return you;
    }
}
