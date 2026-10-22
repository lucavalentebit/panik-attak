package it.panikattak.app;

import java.time.ZoneId;
import java.util.*;

/** Merge independent calendars, retaining every routine when timestamps coincide. */
final class RoutineSchedule {
    static final class Source {
        final String id; final Plan plan; final boolean enabled; final long skipped;
        Source(String id,Plan plan,boolean enabled,long skipped){this.id=id;this.plan=plan;this.enabled=enabled;this.skipped=skipped;}
    }
    static final class Due {
        final String id; final Plan.Event event;
        Due(String id,Plan.Event event){this.id=id;this.event=event;}
    }
    static List<Due> next(List<Source> sources,long after,ZoneId zone){
        ArrayList<Due> result=new ArrayList<>();long earliest=Long.MAX_VALUE;
        for(Source source:sources){
            if(!source.enabled)continue;Plan.Event event=source.plan.next(after,source.skipped,zone);if(event==null)continue;
            if(event.at<earliest){result.clear();earliest=event.at;}
            if(event.at==earliest)result.add(new Due(source.id,event));
        }
        return result;
    }
}
