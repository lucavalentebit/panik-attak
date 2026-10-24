package it.panikattak.app;

import java.time.*;
import java.util.*;

public class HabitStatsTest {
    static int checks;
    static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    static HabitStats.Event event(String routine,long start,String kind,int minute){
        HabitStats.Event e=new HabitStats.Event();e.routine=routine;e.name=routine;e.session=routine+":"+start;e.id=e.session+":"+kind;e.kind=kind;e.start=start;e.first=start;e.last=start;e.scheduled=start;e.expected=1;e.wall=start+minute*60000L;e.elapsed=86400000L+minute*60000L;e.boot="7";e.zone="Europe/Rome";e.offset=Instant.ofEpochMilli(e.wall).atZone(ZoneId.of(e.zone)).getOffset().getTotalSeconds();return e;
    }
    static long at(int day,int hour,int minute){return LocalDate.of(2026,10,day).atTime(hour,minute).atZone(ZoneId.of("Europe/Rome")).toInstant().toEpochMilli();}
    static List<HabitStats.Event> fixture(){
        List<HabitStats.Event> events=new ArrayList<>();for(int n=0;n<5;n++){long start=at(18+n,8,0);HabitStats.Event delivery=event("Lavoro",start,"delivered",0);delivery.phase=n==4?3:0;events.add(delivery);events.add(event("Lavoro",start,"solved",n*2+1));events.add(event("Lavoro",start,"dismissed",n*2+2));events.add(event("Lavoro",start,"stop",12+n*2));}
        long start=at(23,8,0);events.add(event("Lavoro",start,"delivered",0));events.add(event("Lavoro",start,"dismissed",3));events.add(event("Lavoro",start,"finished",3));
        start=at(24,8,0);events.add(event("Weekend",start,"delivered",0));events.add(event("Weekend",start,"pause",10));
        start=at(24,9,0);events.add(event("Lavoro",start,"delivered",0));events.add(event("Lavoro",start,"solved",2));
        start=at(24,10,0);HabitStats.Event partial=event("Lavoro",start,"delivered",0);partial.covered=false;events.add(partial);events.add(event("Lavoro",start,"stop",5));
        return events;
    }
    public static void main(String[] args){
        List<HabitStats.Event> events=fixture();events.add(events.get(0));List<HabitStats.Session> sessions=HabitStats.sessions(events);HabitStats.Summary summary=new HabitStats.Summary(sessions);
        check(sessions.size()==9,"stable session IDs and duplicate event replay");check(summary.complete==7&&summary.panic==1,"Panic denominator includes terminal pause, excludes partial/running");
        check(summary.responses.size()==6&&HabitStats.median(summary.responses)==300000L,"first accepted response median = five minutes");check(summary.stops.size()==5&&HabitStats.median(summary.stops)==960000L,"explicit stop median = sixteen minutes");
        check(HabitStats.histogram(sessions,"stop")[16]==5,"08:00 histogram includes only five complete explicit stops");check(Arrays.stream(HabitStats.histogram(sessions,"finished")).sum()==1,"natural completion not mixed into stops");check(Arrays.stream(HabitStats.histogram(sessions,"pause")).sum()==1,"pause has separate histogram");
        check(summary.running==1&&summary.partial==1,"solved but not dismissed is still running");check(summary.phases[0]==5,"stops use accepted stop phase");
        check(HabitStats.filter(sessions,LocalDate.of(2026,10,24),7,"Weekend").size()==1,"routine filter isolates overlap");check(HabitStats.filter(sessions,LocalDate.of(2026,10,24),7,"").size()==9,"inclusive seven-day range");
        check(HabitStats.filter(sessions,LocalDate.of(2026,10,24),1,"").size()==3,"calendar filter uses session start date");check(summary.weekdays.get(4).size()==1,"weekday response sample count");
        for(String purpose:new String[]{"preview","test","edit"}){HabitStats.Event excluded=event(purpose,at(24,11,0),"delivered",0);excluded.purpose=purpose;check(HabitStats.sessions(Arrays.asList(excluded)).isEmpty(),purpose+" excluded");}
        long midnight=at(23,23,55);HabitStats.Session overnight=HabitStats.sessions(Arrays.asList(event("N",midnight,"delivered",0),event("N",midnight,"dismissed",10),event("N",midnight,"stop",15))).get(0);
        check(overnight.date().equals(LocalDate.of(2026,10,23))&&overnight.stopDelay()==900000L,"overnight duration and session date");check(HabitStats.histogram(Arrays.asList(overnight),"stop")[0]==1,"midnight ending goes to 00:00 bin");
        HabitStats.Event first=event("D",at(25,1,50),"delivered",0),last=event("D",at(25,1,50),"stop",80);check(HabitStats.sessions(Arrays.asList(first,last)).get(0).stopDelay()==4800000L,"DST uses elapsed time, not clock subtraction");
        last.zone="America/New_York";last.offset=-14400;check(HabitStats.histogram(HabitStats.sessions(Arrays.asList(first,last)),"stop")[42]==1,"histogram retains event offset after travel");
        last.wall+=3600000;check(!HabitStats.sessions(Arrays.asList(first,last)).get(0).complete(),"wall clock change invalidates duration coverage");last.wall-=3600000;last.boot="8";check(!HabitStats.sessions(Arrays.asList(first,last)).get(0).complete(),"reboot invalidates unsupported monotonic span");
        first=event("Gap",at(24,7,0),"delivered",0);first.scheduled+=60000;first.ordinal=1;check(!HabitStats.sessions(Arrays.asList(first,event("Gap",at(24,7,0),"stop",5))).get(0).complete(),"missing first recall is partial");
        HabitStats.Event unknown=event("U",at(24,7,0),"interrupted",5);check(!HabitStats.sessions(Arrays.asList(event("U",at(24,7,0),"delivered",0),unknown)).get(0).complete(),"interrupted isn't success");
        String csv=HabitStats.csv(sessions);check(csv.contains("complete_observation")&&csv.contains("\"false\""),"CSV includes sample eligibility");check(HabitStats.csvCell("=SUM(1,2)").equals("\"'=SUM(1,2)\""),"CSV formula injection neutralized");check(HabitStats.csvCell("a,\"b\"").equals("\"a,\"\"b\"\"\""),"CSV commas and quotes escaped");
        check(HabitStats.median(Arrays.asList(1L,9L))==5L&&HabitStats.median(Collections.emptyList())==null,"median even and empty");
        long terminal=at(24,6,0);HabitStats.Event after=event("Closed",terminal,"partial",6);after.covered=false;HabitStats.Session closed=HabitStats.sessions(Arrays.asList(event("Closed",terminal,"delivered",0),event("Closed",terminal,"stop",5),after)).get(0);check(closed.complete()&&closed.events.size()==2,"later changes cannot rewrite a terminal session's coverage or timeline");
        HabitStats.Event deliveredNext=event("Crash",terminal,"delivered",10);deliveredNext.id+="next";deliveredNext.scheduled+=600000;deliveredNext.ordinal=1;
        check(!HabitStats.sessions(Arrays.asList(event("Crash",terminal,"delivered",0),deliveredNext,event("Crash",terminal,"stop",12))).get(0).complete(),"missing disposition after process interruption cannot claim a complete session");
        HabitStats.Event unseenStop=event("Unseen",terminal,"stop",12);unseenStop.scheduled+=600000;check(!HabitStats.sessions(Arrays.asList(event("Unseen",terminal,"delivered",0),unseenStop)).get(0).complete(),"accepted action with missing delivery is partial");
        System.out.println("PASS: "+checks+" history aggregation checks.");
    }
}
