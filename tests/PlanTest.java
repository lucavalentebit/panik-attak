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
        System.out.println("PASS: "+checks+" scheduling checks");
    }
}
