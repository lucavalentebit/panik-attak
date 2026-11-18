package it.panikattak.app;
import java.time.*;
import java.util.*;

public class PlanTest {
    static int checks=0;
    static void check(boolean v,String label){checks++;if(!v)throw new AssertionError(label);}
    public static void main(String[] args){
        Plan p=new Plan();ZoneId zone=ZoneId.of("Europe/Rome");LocalDate day=LocalDate.of(2026,10,22);var events=p.events(day,zone);
        check(events.size()==26,"default count");check(events.get(0).phase==0,"first phase");
        check(events.get(1).at-events.get(0).at==600000,"10 minute start");check(events.get(3).phase==1,"5 minute phase");check(events.get(6).phase==2,"1 minute phase");check(events.get(11).phase==3,"panic phase");
        for(int i=1;i<events.size();i++)check(events.get(i).at>events.get(i-1).at,"ordered unique alarms");
        check(p.next(events.get(0).at,-1,zone).at==events.get(1).at,"skip exact past event");
        check(p.next(events.get(0).at,events.get(0).start,zone).start==p.events(day.plusDays(1),zone).get(0).start,"awake skips whole morning");
        p.days=31;LocalDate friday=LocalDate.of(2026,10,23);long end=p.events(friday,zone).get(25).at;
        check(Instant.ofEpochMilli(p.next(end,-1,zone).start).atZone(zone).getDayOfWeek()==DayOfWeek.MONDAY,"weekday repeat");
        p.hour=23;p.minute=50;p.days=127;var night=p.events(day,zone);long midnight=day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli();check(p.next(midnight,-1,zone).start==night.get(0).start,"sequence crossing midnight");
        p.hour=7;p.minute=30;var dst=p.events(LocalDate.of(2026,10,25),zone);check(Instant.ofEpochMilli(dst.get(0).at).atZone(zone).getHour()==7,"DST local start");
        p.durations[0]=7;p.intervals[0]=10;var shortPhase=p.events(day,zone);check(shortPhase.get(1).at-shortPhase.get(0).at==7*60000L,"duration shorter than interval");
        p.days=0;check(p.next(System.currentTimeMillis(),-1,zone)==null,"empty schedule");
        Plan editable=new Plan();
        String calm=editable.stateId(0), panic=editable.stateId(3);
        String custom="quiet-office";
        editable.customStates.put(custom,"Scrivania");
        editable.statePuzzles.put(custom,2);
        editable.append(custom,17);
        editable.append(calm,3);
        check(editable.valid()&&editable.phaseCount()==6,"custom and repeated states appended");
        check(editable.phaseName(4).equals("Scrivania")&&editable.phaseLevel(4)==0,"custom state retains its name and gentle sound");
        check(editable.puzzleFor(4)==2&&editable.puzzleFor(5)==editable.puzzleType,"state puzzle and routine fallback");
        long start=editable.events(day,zone).get(0).start;
        long customStart=start+editable.offset(4)*60_000L;
        check(editable.phaseAt(start,customStart)==4,"elapsed time selects added interval at its boundary");
        check(editable.next(customStart-1,-1,zone).phase==4,"added interval participates in alarm scheduling");
        String surviving=editable.intervalIds[4];
        check(editable.remove(2)&&editable.intervalIds[3].equals(surviving),"removal preserves surviving interval identity");
        check(editable.puzzleFor(3)==2&&editable.puzzleFor(4)==editable.puzzleType,"removal preserves puzzle associations");
        editable.customStates.put(custom,"Scrivania nuova");
        check(editable.phaseName(3).equals("Scrivania nuova"),"renaming a state affects saved intervals");
        editable.statePuzzles.put(calm,1);
        check(editable.puzzleFor(0)==1&&editable.puzzleFor(4)==1,"repeated state shares the same puzzle");
        editable.setState(0,panic);
        check(editable.phaseLevel(0)==Plan.PANIC,"panic intensity follows the state rather than the position");
        editable.fixed[0]=true;
        check(!editable.valid(),"panic rejects fixed recalls at any position");
        Plan longRun=new Plan();
        longRun.hour=0;
        longRun.minute=0;
        while(longRun.phaseCount()>1)longRun.remove(1);
        longRun.durations[0]=120;
        for(int phase=1;phase<12;phase++)longRun.append(longRun.stateId(0),120);
        Arrays.fill(longRun.intervals,29);
        LocalDate spring=LocalDate.of(2026,3,29);
        var nextDay=longRun.events(spring.plusDays(1),zone).get(0);
        check(longRun.valid()&&longRun.next(nextDay.at-1,-1,zone).at==nextDay.at,"spring-forward overlap chooses the earliest timestamp");
        System.out.println("PASS: "+checks+" scheduling checks");
    }
}
