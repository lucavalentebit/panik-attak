package it.panikattak.app;

import java.time.*;
import java.util.*;

/** Pure scheduling logic; all phases use offsets from the first alarm. */
public final class Plan {
    public int hour=7, minute=30;
    public int[] intervals={10,5,1,1};
    public int[] durations={30,15,5,15};
    public int days=127;
    public boolean[] fixed={false,false,false,false};
    public int[] fixedMinutes={450,480,495,500};
    public int puzzleType=0;
    public boolean escalating=false;
    public int offset(int phase){int n=0;for(int i=0;i<phase;i++)n+=durations[i];return n;}
    public int fixedOffset(int phase){return Math.floorMod(fixedMinutes[phase]-(hour*60+minute),1440);}
    public boolean valid(){
        if(hour<0||hour>23||minute<0||minute>59||days<1||days>127||puzzleType<0||puzzleType>4||fixed[3])return false;
        for(int i=0;i<4;i++)if(durations[i]<1||durations[i]>120||intervals[i]<1||intervals[i]>30||
            (fixed[i]&&(fixedMinutes[i]<0||fixedMinutes[i]>=1440||fixedOffset(i)<offset(i)||fixedOffset(i)>=offset(i)+durations[i])))return false;
        return true;
    }
    public static final String[] NAMES={"Con calma", "Ci siamo", "Alzati", "Panic Mode"};
    public static final class Event {
        public final long at, start; public final int phase;
        Event(long at,long start,int phase) { this.at=at; this.start=start; this.phase=phase; }
    }
    public List<Event> events(LocalDate date, ZoneId zone) {
        long start=date.atTime(hour,minute).atZone(zone).toInstant().toEpochMilli();
        ArrayList<Event> result=new ArrayList<>(); int offset=0;
        for(int p=0;p<4;p++) {
            if(fixed[p]&&p!=3){
                // Resolve the selected wall-clock time in the date's zone, including midnight/DST.
                int absolute=hour*60+minute+fixedOffset(p);
                long at=date.plusDays(absolute/1440).atTime(fixedMinutes[p]/60,fixedMinutes[p]%60).atZone(zone).toInstant().toEpochMilli();
                if(at>=start+offset*60000L&&at<start+(offset+durations[p])*60000L)result.add(new Event(at,start,p));
            }else for(int t=0;t<durations[p];t+=intervals[p]) result.add(new Event(start+(offset+t)*60000L,start,p));
            offset+=durations[p];
        }
        return result;
    }
    public Event next(long after,long skippedStart,ZoneId zone) {
        LocalDate today=Instant.ofEpochMilli(after).atZone(zone).toLocalDate();
        for(int d=-1;d<=8;d++) {
            LocalDate date=today.plusDays(d);
            if((days & (1<<(date.getDayOfWeek().getValue()-1)))==0) continue;
            for(Event e:events(date,zone)) if(e.at>after && e.start!=skippedStart) return e;
        }
        return null;
    }
    public int count() { int n=0;for(int i=0;i<4;i++)n+=fixed[i]&&i!=3?1:(durations[i]+intervals[i]-1)/intervals[i];return n; }
    public int total() {return Arrays.stream(durations).sum();}
}
