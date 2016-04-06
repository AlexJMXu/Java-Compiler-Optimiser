package comp207p.target;

public class BConditional{
    public int foo(){
        int a = 5;

        if(a > 5){
            a = 4;
        }
        else{
            a = 3;
        }
        return a + 3;
    }

    public boolean returnFalse(){
        return false;
    }
}
