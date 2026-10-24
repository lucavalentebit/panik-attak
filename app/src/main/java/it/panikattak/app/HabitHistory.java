package it.panikattak.app;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import android.os.*;
import android.provider.Settings;
import android.util.Log;
import org.json.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

/** Append-only SQLite journal. History failure never blocks an alarm action. */
final class HabitHistory {
    static final String ENABLED="history.enabled",SINCE="history.since",RETENTION="history.retention",ERROR="history.error";
    static final String DATABASE="habits.db",OUTBOX="habit-outbox.db";
    static final ExecutorService IO=Executors.newSingleThreadExecutor();
    // Instrumentation uses separate databases, never the user's history.
    static String testNamespace="";
    static boolean enabled(Context c){return Store.prefs(c).getBoolean(ENABLED,true);}
    static int retention(Context c){return Store.prefs(c).getInt(RETENTION,365);}
    static boolean unlocked(Context c){return c.getSystemService(UserManager.class).isUserUnlocked();}
    static void initialize(Context c){if(!Store.prefs(c).contains(SINCE))Store.prefs(c).edit().putLong(SINCE,System.currentTimeMillis()).commit();}
    static void failed(Context c,RuntimeException e){Log.e("PanikAttak","History unavailable; alarm state is unchanged",e);try{Store.prefs(c).edit().putBoolean(ERROR,true).putLong("history.lastGap",System.currentTimeMillis()).apply();}catch(RuntimeException ignored){}}
    static final class Database extends SQLiteOpenHelper {
        Database(Context c,boolean outbox){super(outbox?c.createDeviceProtectedStorageContext():c.getApplicationContext(),testNamespace+(outbox?OUTBOX:DATABASE),null,1);setWriteAheadLoggingEnabled(true);}
        @Override public void onCreate(SQLiteDatabase db){db.execSQL("CREATE TABLE events (id TEXT PRIMARY KEY, session TEXT NOT NULL, start INTEGER NOT NULL, wall INTEGER NOT NULL, data TEXT NOT NULL)");db.execSQL("CREATE INDEX session_start ON events(start)");}
        @Override public void onUpgrade(SQLiteDatabase db,int from,int to){throw new IllegalStateException("Unsupported history schema");}
    }
    static JSONObject json(HabitStats.Event e){try{return new JSONObject().put("id",e.id).put("session",e.session).put("routine",e.routine).put("name",e.name).put("kind",e.kind).put("purpose",e.purpose).put("zone",e.zone).put("boot",e.boot).put("config",e.config).put("start",e.start).put("scheduled",e.scheduled).put("wall",e.wall).put("elapsed",e.elapsed).put("first",e.first).put("last",e.last).put("phase",e.phase).put("type",e.type).put("level",e.level).put("expected",e.expected).put("ordinal",e.ordinal).put("offset",e.offset).put("count",e.count).put("covered",e.covered);}catch(JSONException impossible){throw new IllegalStateException(impossible);}}
    static HabitStats.Event decode(String value){try{JSONObject o=new JSONObject(value);HabitStats.Event e=new HabitStats.Event();e.id=o.getString("id");e.session=o.getString("session");e.routine=o.getString("routine");e.name=o.optString("name");e.kind=o.getString("kind");e.purpose=o.getString("purpose");e.zone=o.getString("zone");e.boot=o.getString("boot");e.config=o.optString("config");e.start=o.getLong("start");e.scheduled=o.getLong("scheduled");e.wall=o.getLong("wall");e.elapsed=o.getLong("elapsed");e.first=o.getLong("first");e.last=o.getLong("last");e.phase=o.getInt("phase");e.type=o.getInt("type");e.level=o.getInt("level");e.expected=o.getInt("expected");e.ordinal=o.getInt("ordinal");e.offset=o.getInt("offset");e.count=o.getInt("count");e.covered=o.getBoolean("covered");return e;}catch(JSONException invalid){throw new IllegalStateException("Unreadable history event",invalid);}}
    static ContentValues values(HabitStats.Event e){ContentValues v=new ContentValues();v.put("id",e.id);v.put("session",e.session);v.put("start",e.start);v.put("wall",e.wall);v.put("data",json(e).toString());return v;}
    static void insert(SQLiteDatabase db,HabitStats.Event e,boolean accumulate){
        if(accumulate){try(Cursor previous=db.query("events",new String[]{"data"},"id=?",new String[]{e.id},null,null,null)){if(previous.moveToFirst())e.count+=decode(previous.getString(0)).count;}db.insertWithOnConflict("events",null,values(e),SQLiteDatabase.CONFLICT_REPLACE);}
        else db.insertWithOnConflict("events",null,values(e),SQLiteDatabase.CONFLICT_IGNORE);
    }
    static HabitStats.Event event(Context c,AlarmQueue.Entry entry,String kind,String purpose){
        initialize(c);HabitStats.Event e=new HabitStats.Event();ZoneId zone=ZoneId.systemDefault();e.routine=entry.routine;e.start=entry.start;e.scheduled=entry.at;e.session=entry.routine+":"+entry.start;e.kind=kind;e.purpose=purpose;e.id=e.session+":"+entry.at+":"+purpose+":"+kind;
        e.wall=System.currentTimeMillis();e.elapsed=SystemClock.elapsedRealtime();e.boot=Integer.toString(Settings.Global.getInt(c.getContentResolver(),Settings.Global.BOOT_COUNT,-1));if(e.boot.equals("-1"))e.boot="";
        e.zone=zone.getId();e.offset=Instant.ofEpochMilli(e.wall).atZone(zone).getOffset().getTotalSeconds();e.phase=entry.phase;
        Plan plan=Store.plan(c,entry.routine);Puzzle puzzle=Puzzle.decode(entry.puzzle);e.type=puzzle.type;e.level=purpose.equals("recall")?puzzle.level:2;
        List<Plan.Event> planned=plan.events(Instant.ofEpochMilli(entry.start).atZone(zone).toLocalDate(),zone);e.first=planned.isEmpty()?entry.start:planned.get(0).at;e.last=planned.isEmpty()?entry.at:planned.get(planned.size()-1).at;e.expected=planned.size();e.ordinal=-1;
        for(int n=0;n<planned.size();n++)if(planned.get(n).at==entry.at)e.ordinal=n;
        e.covered=e.ordinal>=0&&Store.prefs(c).getLong(SINCE,e.wall)<=e.first&&Store.prefs(c).getLong("history.lastGap",0)<e.first;
        if(unlocked(c)){e.name=Store.name(c,entry.routine);e.config=plan.hour+":"+plan.minute+";days="+plan.days+";durations="+Arrays.toString(plan.durations)+";intervals="+Arrays.toString(plan.intervals)+";fixed="+Arrays.toString(plan.fixed)+";times="+Arrays.toString(plan.fixedMinutes)+";puzzle="+plan.puzzleType+";escalating="+plan.escalating;}
        else e.covered=false;
        return e;
    }
    static synchronized void append(Context c,HabitStats.Event e,boolean accumulate){
        if(!enabled(c))return;
        boolean privateStore=unlocked(c);
        try(Database helper=new Database(c,!privateStore)){
            if(!privateStore){e.name="";e.config="";e.covered=false;}
            insert(helper.getWritableDatabase(),e,accumulate);
        }catch(RuntimeException failure){
            failed(c,failure);
            if(privateStore)try(Database outbox=new Database(c,true)){e.name="";e.config="";e.covered=false;insert(outbox.getWritableDatabase(),e,accumulate);}catch(RuntimeException ignored){}
        }
    }
    static void record(Context c,AlarmQueue.Entry e,String kind,String purpose){
        if(e==null||e.test||!enabled(c)||purpose.equals("preview")||purpose.equals("edit"))return;
        try{append(c,event(c,e,kind,purpose),kind.equals("wrong"));}catch(RuntimeException failure){failed(c,failure);}
    }
    static void current(Context c,String kind){try{List<AlarmQueue.Entry> queue=AlarmQueue.read(c);if(!queue.isEmpty())record(c,queue.get(0),kind,"recall");}catch(RuntimeException failure){failed(c,failure);}}
    static AlarmQueue.Entry running(Context c,String id){
        for(AlarmQueue.Entry e:AlarmQueue.read(c))if(!e.test&&e.routine.equals(id))return e;
        SequenceStatus.Run run=SequenceStatus.read(c).get(id);if(run==null)return null;
        // Only the type/level is needed by history, never an answer or actual challenge contents.
        return fromRun(c,run);
    }
    static AlarmQueue.Entry fromRun(Context c,SequenceStatus.Run run){return new AlarmQueue.Entry(run.routine,run.lastAt,run.start,run.phase,false,Puzzle.create(Store.plan(c,run.routine).puzzleType,0,"").encode());}
    static void runEvent(Context c,SequenceStatus.Run run,String kind){try{record(c,fromRun(c,run),kind,"recall");}catch(RuntimeException failure){failed(c,failure);}}
    static void runningEvent(Context c,String id,String kind,String purpose){try{record(c,running(c,id),kind,purpose);}catch(RuntimeException failure){failed(c,failure);}}
    static void gate(Context c,String id,long start,String purpose,String kind,boolean test){
        if(test||purpose.equals("preview")||purpose.equals("edit"))return;
        try{AlarmQueue.Entry e=running(c,id);if(e!=null&&(!purpose.equals("morning")||e.start==start))record(c,e,kind,purpose);}catch(RuntimeException failure){failed(c,failure);}
    }
    static synchronized void drain(Context c){
        if(!unlocked(c))return;
        try(Database source=new Database(c,true);Database destination=new Database(c,false)){
            SQLiteDatabase from=source.getWritableDatabase(),to=destination.getWritableDatabase();
            try(Cursor rows=from.query("events",new String[]{"data"},null,null,null,null,"rowid")){
                to.beginTransaction();try{while(rows.moveToNext())insert(to,decode(rows.getString(0)),false);to.setTransactionSuccessful();}finally{to.endTransaction();}
            }
            from.delete("events",null,null);prune(from,retention(c));prune(to,retention(c));
        }catch(RuntimeException failure){failed(c,failure);}
    }
    static void maintenance(Context c){Context app=c.getApplicationContext();IO.execute(()->drain(app));}
    static void prune(SQLiteDatabase db,int days){long cutoff=System.currentTimeMillis()-days*86400000L;db.delete("events","start<?",new String[]{Long.toString(cutoff)});}
    static List<HabitStats.Event> events(Context c){
        drain(c);List<HabitStats.Event> events=new ArrayList<>();if(!unlocked(c))return events;
        try(Database helper=new Database(c,false);Cursor rows=helper.getReadableDatabase().query("events",new String[]{"data"},null,null,null,null,"rowid")){while(rows.moveToNext())events.add(decode(rows.getString(0)));}
        return events;
    }
    static synchronized void setEnabled(Context c,boolean value){
        if(value==enabled(c))return;
        if(!value)for(SequenceStatus.Run run:SequenceStatus.read(c).values())runningEvent(c,run.routine,"partial","recall");
        Store.prefs(c).edit().putBoolean(ENABLED,value).putLong(SINCE,System.currentTimeMillis()).commit();
        drain(c);
    }
    static synchronized void retain(Context c,int days){if(days!=30&&days!=90&&days!=365)throw new IllegalArgumentException("Invalid retention");Store.prefs(c).edit().putInt(RETENTION,days).commit();drain(c);}
    static synchronized void erase(Context c){
        // Serializes with import and appends: pending pre-erasure events cannot reappear.
        try(Database outbox=new Database(c,true);Database history=new Database(c,false)){
            outbox.getWritableDatabase().delete("events",null,null);history.getWritableDatabase().delete("events",null,null);
            Store.prefs(c).edit().putLong(SINCE,System.currentTimeMillis()).putBoolean(ERROR,false).remove("history.lastGap").commit();
        }
    }
}
