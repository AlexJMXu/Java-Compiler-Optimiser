package comp207p.target;

public class IfFor {
    public int foo() {
        int a = 10;
        for(int i = 0; i < 10; i++){
            if(i > 5){
                a+=i;
            }
        }
        return a;
    }
}
