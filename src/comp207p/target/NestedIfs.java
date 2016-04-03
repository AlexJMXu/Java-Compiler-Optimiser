package comp207p.target;

public class NestedIfs {
    public int foo() {
        int a = 10;

        if(a > 4) {
            a+= 20;

            if(a > 25) {
                return a;
            }
        }

        return -1;
    }
}
