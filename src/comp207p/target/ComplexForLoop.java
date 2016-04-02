package comp207p.target;

public class ComplexForLoop {
    public void methodFour(){
        int a = 534245;
        int b = a - 1234;

        for(int i = 0; i < 20; i = i + 4){
            System.out.println((b-a) * i);
        }
    }
}
