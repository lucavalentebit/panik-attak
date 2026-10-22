package it.panikattak.app;

import java.time.*;
import java.util.*;

public final class UpgradeTest {
    static int checks;
    static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    public static void main(String[] args){
        ZoneId zone=ZoneId.of("Europe/Rome");LocalDate date=LocalDate.of(2026,10,22);
        Plan p=new Plan();check(p.valid(),"default migration");check(!p.escalating&&p.puzzleType==0,"default puzzle unchanged");
        p.fixed[1]=true;p.fixedMinutes[1]=8*60+10;check(p.valid(),"fixed within phase");
        var events=p.events(date,zone);check(events.size()==24&&p.count()==24,"one fixed replaces interval");
        check(events.stream().filter(e->e.phase==1).count()==1,"exactly one fixed event");
        var fixed=events.stream().filter(e->e.phase==1).findFirst().get();
        check(Instant.ofEpochMilli(fixed.at).atZone(zone).toLocalTime().equals(LocalTime.of(8,10)),"exact wall clock");
        check(p.next(fixed.at,-1,zone).phase==2,"past fixed does not ring again");
        p.fixedMinutes[1]=480;check(p.valid(),"start inclusive");p.fixedMinutes[1]=495;check(!p.valid(),"end exclusive");
        p.fixedMinutes[1]=479;check(!p.valid(),"before start rejected");p.fixedMinutes[1]=490;p.fixed[3]=true;check(!p.valid(),"panic fixed prohibited");p.fixed[3]=false;
        p.hour=23;p.minute=50;p.fixed[1]=false;p.fixed[0]=true;p.fixedMinutes[0]=5;check(p.valid(),"overnight fixed valid");
        fixed=p.events(date,zone).get(0);check(Instant.ofEpochMilli(fixed.at).atZone(zone).toLocalDate().equals(date.plusDays(1)),"fixed moves to next date");
        long midnight=date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli();check(p.next(midnight,-1,zone).at==fixed.at,"restore after midnight");
        check(p.next(fixed.at,fixed.start,zone).start!=fixed.start,"morning skip applies to fixed");
        p=new Plan();p.fixed[0]=true;p.fixedMinutes[0]=7*60+45;
        for(ZoneId z:new ZoneId[]{zone,ZoneId.of("America/New_York"),ZoneId.of("Asia/Tokyo")})for(LocalDate d:new LocalDate[]{date,LocalDate.of(2026,10,25),LocalDate.of(2026,3,29)}){
            var e=p.events(d,z).get(0);check(Instant.ofEpochMilli(e.at).atZone(z).toLocalTime().equals(LocalTime.of(7,45)),"timezone and DST restore");
        }
        for(int type=0;type<5;type++)for(int level=0;level<3;level++){
            String previous="";
            for(int n=0;n<300;n++){
                Puzzle puzzle=Puzzle.create(type,level,previous);check(!puzzle.encode().equals(previous),"no consecutive repeat");previous=puzzle.encode();
                check(puzzle.accepts(puzzle.solution()),"solution accepted");check(!puzzle.accepts(null)&&!puzzle.accepts("wrong"),"invalid answer rejected");
                check(Puzzle.decode(puzzle.encode()).solution().equals(puzzle.solution()),"persistence preserves challenge");
                if(type==Puzzle.COLORS||type==Puzzle.TILES){check(puzzle.data.length==4+level,"memory difficulty length");for(int v:puzzle.data)check(v>=0&&v<(type==Puzzle.COLORS?6:9),"memory positions valid");}
                if(type==Puzzle.ORDER)check(!Puzzle.sorted(puzzle.data),"order starts unsolved");
                if(type==Puzzle.MATH&&level==2)check(puzzle.data.length==3,"two-step difficult math");
            }
        }
        for(int n=0;n<10;n++){check(Puzzle.level(false,n)==0,"escalation opt-in");check(Puzzle.level(true,n)==(n<2?0:n<4?1:2),"escalation thresholds");}
        System.out.println("PASS: "+checks+" v2 checks");
    }
}
