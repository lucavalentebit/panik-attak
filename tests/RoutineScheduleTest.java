package it.panikattak.app;
import java.time.*;
import java.util.*;

public class RoutineScheduleTest {
    static int checks;
    static void check(boolean value,String description){checks++;if(!value)throw new AssertionError(description);}
    static RoutineSchedule.Source source(String id,Plan p,boolean on,long skip){return new RoutineSchedule.Source(id,p,on,skip);}
    public static void main(String[] args){
        ZoneId zone=ZoneId.of("Europe/Rome");LocalDate friday=LocalDate.of(2026,10,23);long midnight=friday.atStartOfDay(zone).toInstant().toEpochMilli();
        Plan work=new Plan(),weekend=new Plan();work.days=31;weekend.days=96;weekend.hour=9;
        var workSource=source("work",work,true,-1);var weekendSource=source("weekend",weekend,true,-1);
        var next=RoutineSchedule.next(Arrays.asList(workSource,weekendSource),midnight,zone);check(next.size()==1&&next.get(0).id.equals("work"),"weekday routine only");
        next=RoutineSchedule.next(Arrays.asList(workSource,weekendSource),midnight+86400000L,zone);check(next.size()==1&&next.get(0).id.equals("weekend"),"weekend independent calendar");
        Plan gym=new Plan();gym.days=31;gym.puzzleType=2;
        next=RoutineSchedule.next(Arrays.asList(workSource,source("gym",gym,true,-1)),midnight,zone);check(next.size()==2,"coincident alarms both retained");check(next.get(0).event.at==next.get(1).event.at,"same timestamp");
        long first=next.get(0).event.at,start=next.get(0).event.start;
        next=RoutineSchedule.next(Arrays.asList(source("work",work,true,start),source("gym",gym,true,-1)),midnight,zone);check(next.size()==1&&next.get(0).id.equals("gym"),"skip morning isolated");
        next=RoutineSchedule.next(Arrays.asList(source("work",work,false,-1),source("gym",gym,true,-1)),midnight,zone);check(next.size()==1&&next.get(0).id.equals("gym"),"pause isolated");
        next=RoutineSchedule.next(Arrays.asList(workSource,source("gym",gym,true,-1)),first,zone);check(next.size()==2&&next.get(0).event.at==first+600000,"both advance beyond delivered timestamp");
        gym.fixed[0]=true;gym.fixedMinutes[0]=455;
        next=RoutineSchedule.next(Arrays.asList(workSource,source("gym",gym,true,-1)),first,zone);check(next.size()==1&&next.get(0).id.equals("gym")&&next.get(0).event.at==first+300000,"fixed event merges with intervals");
        Plan night=new Plan();night.hour=23;night.minute=50;
        long saturday=friday.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli();
        next=RoutineSchedule.next(Arrays.asList(weekendSource,source("night",night,true,-1)),saturday,zone);check(next.get(0).id.equals("night")&&next.get(0).event.start<saturday,"night sequence crosses day boundary");
        check(RoutineSchedule.next(Arrays.asList(source("work",work,false,-1),source("gym",gym,false,-1)),midnight,zone).isEmpty(),"all paused schedules nothing");
        for(ZoneId z:new ZoneId[]{zone,ZoneId.of("America/New_York"),ZoneId.of("Asia/Tokyo")}){
            LocalDate day=LocalDate.of(2026,10,25);next=RoutineSchedule.next(Arrays.asList(weekendSource),day.atStartOfDay(z).toInstant().toEpochMilli(),z);
            check(Instant.ofEpochMilli(next.get(0).event.at).atZone(z).getHour()==9,"local time restored in timezone");
        }
        System.out.println("PASS: "+checks+" multi-routine scheduling checks");
    }
}
