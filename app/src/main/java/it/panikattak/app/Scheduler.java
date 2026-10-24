package it.panikattak.app;
import android.app.*;
import android.content.*;
import android.os.Build;
import java.time.ZoneId;
import java.util.*;
import org.json.*;

final class Scheduler {
    static PendingIntent pending(Context c,long at,long start,int phase,boolean test){
        Intent i=new Intent(c,AlarmReceiver.class).setAction(test?"TEST":"RING").putExtra("at",at).putExtra("start",start).putExtra("phase",phase).putExtra("test",test).putExtra("routine",Store.selected(c));
        return PendingIntent.getBroadcast(c,test?2:1,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    static boolean allowed(Context c){return Build.VERSION.SDK_INT<31||c.getSystemService(AlarmManager.class).canScheduleExactAlarms();}
    static void cancel(Context c){c.getSystemService(AlarmManager.class).cancel(pending(c,0,0,0,false));Store.prefs(c).edit().remove("next").remove("nextBatch").apply();}
    static List<RoutineSchedule.Due> next(Context c,long after){
        List<RoutineSchedule.Source> sources=new ArrayList<>();for(Store.Routine r:Store.routines(c))sources.add(new RoutineSchedule.Source(r.id,Store.plan(c,r.id),Store.enabled(c,r.id),Store.skipped(c,r.id)));
        return RoutineSchedule.next(sources,after,ZoneId.systemDefault());
    }
    static Plan.Event schedule(Context c){return schedule(c,System.currentTimeMillis());}
    static Plan.Event schedule(Context c,long after){
        try{
        cancel(c);if(!allowed(c))return null;List<RoutineSchedule.Due> due=next(c,Math.max(after,System.currentTimeMillis()));if(due.isEmpty())return null;
        JSONArray batch=new JSONArray();try{for(var d:due)batch.put(new JSONObject().put("id",d.id).put("at",d.event.at).put("start",d.event.start).put("phase",d.event.phase));}catch(JSONException e){throw new IllegalStateException(e);}
        Plan.Event event=due.get(0).event;
        Store.prefs(c).edit().putLong("next",event.at).putString("nextBatch",batch.toString()).apply();set(c,event.at,event.start,event.phase,false);return event;
        }finally{SequenceStatus.refresh(c);}
    }
    static void set(Context c,long at,long start,int phase,boolean test){
        PendingIntent show=PendingIntent.getActivity(c,10,new Intent(c,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        c.getSystemService(AlarmManager.class).setAlarmClock(new AlarmManager.AlarmClockInfo(at,show),pending(c,at,start,phase,test));
    }
    static void test(Context c){long at=System.currentTimeMillis()+12000;Store.prefs(c).edit().putString("testRoutine",Store.selected(c)).apply();set(c,at,at,3,true);}
    static void syncSound(Context c){syncSound(c,false);}
    static void syncSound(Context c,boolean fromAlarm){
        if(Store.prefs(c).getBoolean("ringing",false)){Intent service=new Intent(c,AlarmService.class);if(fromAlarm)c.startForegroundService(service);else c.startService(service);}else c.stopService(new Intent(c,AlarmService.class));
        c.sendBroadcast(new Intent("it.panikattak.REFRESH").setPackage(c.getPackageName()));
    }
    static void dismissCurrent(Context c){List<AlarmQueue.Entry> entries=AlarmQueue.read(c);if(!entries.isEmpty())entries.remove(0);AlarmQueue.write(c,entries);syncSound(c);schedule(c);}
    static boolean dismissCurrent(Context c,long at,Puzzle puzzle,String answer){return dismissCurrent(c,Store.activeRoutine(c),at,puzzle,answer);}
    static boolean dismissCurrent(Context c,String id,long at,Puzzle puzzle,String answer){
        var s=Store.prefs(c);if(puzzle==null||!puzzle.accepts(answer)||!s.getBoolean("ringing",false)||!id.equals(Store.activeRoutine(c))||s.getLong("activeAt",-1)!=at||!Store.activePuzzle(c).encode().equals(puzzle.encode()))return false;
        dismissCurrent(c);return true;
    }
    static boolean dismissMorning(Context c,long start,boolean test,Puzzle puzzle,String answer){return dismissMorning(c,Store.activeRoutine(c),start,test,puzzle,answer);}
    static boolean dismissMorning(Context c,String id,long start,boolean test,Puzzle puzzle,String answer){if(puzzle==null||puzzle.level!=2||!puzzle.accepts(answer))return false;return stopMorning(c,id,start,test);}
    static boolean dismissMorning(Context c,long start,boolean test,WakeChallenge challenge,String answer){if(challenge==null||!challenge.accepts(answer))return false;return stopMorning(c,Store.activeRoutine(c),start,test);}
    private static boolean stopMorning(Context c,String id,long start,boolean test){
        var s=Store.prefs(c);if(!s.getBoolean("ringing",false)||!id.equals(Store.activeRoutine(c))||s.getLong("activeStart",-1)!=start||s.getBoolean("activeTest",false)!=test)return false;
        if(!test)s.edit().putLong(Store.key(id,"skip"),start).putInt(Store.key(id,"ignored"),0).apply();
        List<AlarmQueue.Entry> entries=AlarmQueue.read(c);entries.removeIf(e->e.routine.equals(id)&&e.start==start&&e.test==test);AlarmQueue.write(c,entries);syncSound(c);schedule(c);return true;
    }
    static boolean pause(Context c,Puzzle puzzle,String answer){return pause(c,Store.selected(c),puzzle,answer);}
    static boolean pause(Context c,String id,Puzzle puzzle,String answer){if(!Store.exists(c,id)||puzzle==null||puzzle.level!=2||!puzzle.accepts(answer))return false;pauseRoutine(c,id);return true;}
    static boolean pause(Context c,WakeChallenge challenge,String answer){if(challenge==null||!challenge.accepts(answer))return false;pauseRoutine(c,Store.selected(c));return true;}
    private static void pauseRoutine(Context c,String id){
        Store.prefs(c).edit().putBoolean(Store.key(id,"enabled"),false).putInt(Store.key(id,"ignored"),0).apply();AlarmQueue.removeRoutine(c,id);
        if(id.equals(Store.prefs(c).getString("testRoutine",Store.DEFAULT)))c.getSystemService(AlarmManager.class).cancel(pending(c,0,0,0,true));
        syncSound(c);schedule(c);
    }
}
