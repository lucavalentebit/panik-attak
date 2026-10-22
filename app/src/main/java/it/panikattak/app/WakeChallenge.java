package it.panikattak.app;

import java.util.Random;

/** Two mental steps, always an integer between 53 and 183. */
final class WakeChallenge {
    final int a, b, c;
    WakeChallenge(int a, int b, int c) { this.a=a; this.b=b; this.c=c; }
    static WakeChallenge create() {
        Random random=new Random();
        return new WakeChallenge(12+random.nextInt(13),3+random.nextInt(4),17+random.nextInt(23));
    }
    String question() { return "("+a+" × "+b+") + "+c; }
    boolean accepts(String answer) {
        if(answer==null||!answer.matches("[0-9]{1,3}"))return false;
        return Integer.parseInt(answer)==a*b+c;
    }
}
