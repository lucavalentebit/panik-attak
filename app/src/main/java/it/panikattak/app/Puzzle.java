package it.panikattak.app;

import java.util.*;

/** Immutable challenge content. Answers are validated again at the scheduling boundary. */
final class Puzzle {
    static final int MATH=0, COLORS=1, ODD=2, ORDER=3, TILES=4;
    final int type, level;
    final int[] data;
    Puzzle(int type,int level,int[] data){this.type=type;this.level=level;this.data=data.clone();}
    static int level(boolean escalating,int ignored){return !escalating||ignored<2?0:ignored<4?1:2;}
    static Puzzle create(int type,int level,String previous){
        Random r=new Random();Puzzle p;
        do {
            int[] d;
            if(type==MATH){if(level==2){WakeChallenge w=WakeChallenge.create();d=new int[]{w.a,w.b,w.c};}else d=new int[]{3+r.nextInt(level==0?15:40),2+r.nextInt(level==0?13:30)};}
            else if(type==ODD){int size=level==0?4:5;d=new int[]{r.nextInt(size*size),r.nextInt(4)};}
            else if(type==ORDER){int n=level==0?3:5;d=new int[n];for(int i=0;i<n;i++)d[i]=level==0?1+r.nextInt(30):i;
                if(level==0){for(int i=0;i<n;i++)for(int j=0;j<i;j++)if(d[i]==d[j]){d[i]=1+r.nextInt(30);j=-1;}}
                Arrays.sort(d);for(int i=n-1;i>0;i--){int j=r.nextInt(i+1),v=d[i];d[i]=d[j];d[j]=v;}
                if(sorted(d)){int v=d[0];d[0]=d[n-1];d[n-1]=v;}
            }else{d=new int[4+level];for(int i=0;i<d.length;i++)d[i]=r.nextInt(type==COLORS?6:9);}
            p=new Puzzle(type,level,d);
        }while(p.encode().equals(previous));
        return p;
    }
    static boolean sorted(int[] a){for(int i=1;i<a.length;i++)if(a[i]<=a[i-1])return false;return true;}
    String question(){return data.length==3?"("+data[0]+" × "+data[1]+") + "+data[2]:data[0]+" + "+data[1];}
    String solution(){
        if(type==MATH)return Integer.toString(data.length==3?data[0]*data[1]+data[2]:data[0]+data[1]);
        if(type==ODD)return Integer.toString(data[0]);
        int[] values=data.clone();if(type==ORDER)Arrays.sort(values);return join(values);
    }
    boolean accepts(String answer){return solution().equals(answer);}
    static String join(int[] values){StringJoiner s=new StringJoiner(",");for(int v:values)s.add(Integer.toString(v));return s.toString();}
    String encode(){return type+";"+level+";"+join(data);}
    static Puzzle decode(String value){String[] parts=value.split(";");String[] cells=parts[2].split(",");int[] d=new int[cells.length];for(int i=0;i<d.length;i++)d[i]=Integer.parseInt(cells[i]);return new Puzzle(Integer.parseInt(parts[0]),Integer.parseInt(parts[1]),d);}
}
