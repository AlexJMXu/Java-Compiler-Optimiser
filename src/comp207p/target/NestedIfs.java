package comp207p.target;

public class NestedIfs {
    public int foo() {
        int a = 10;
        int b = 4;


        if(a + b > 4) {
            a+= 20;

            if(a > 25) {
                return a;
            }
        }

        return -1;
    }
}
