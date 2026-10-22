package it.panikattak.app;

public class WakeChallengeTest {
    static int checks;
    static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        WakeChallenge example=new WakeChallenge(17,4,26);
        check(example.accepts("94"),"Two-step solution");
        for(String wrong:new String[]{"", "0", "68", "95", "-94", "94.0", "94 ", "0940", "abc"})check(!example.accepts(wrong),"Reject invalid input");
        check(!example.accepts(null),"Reject missing input");
        for(int i=0;i<2000;i++){
            WakeChallenge problem=WakeChallenge.create();int result=problem.a*problem.b+problem.c;
            check(result>=53&&result<=183,"Manageable result range");
            check(problem.a>=12&&problem.a<=24&&problem.b>=3&&problem.b<=6&&problem.c>=17&&problem.c<=39,"Manageable operand ranges");
            check(problem.accepts(Integer.toString(result)),"Generated problem has a valid solution");
            check(!problem.accepts(Integer.toString(result+1)),"Wrong answer is rejected");
        }
        System.out.println("PASS: "+checks+" challenge checks");
    }
}
