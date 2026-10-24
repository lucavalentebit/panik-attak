package it.panikattak.app;

import android.content.*;
import org.json.*;
import java.util.*;

/** Persisted outstanding alarms. At most one current recall per routine and test mode. */
final class AlarmQueue {
    static final class Entry {
        final String routine,puzzle;final long at,start;final int phase;final boolean test;
        Entry(String routine,long at,long start,int phase,boolean test,String puzzle){this.routine=routine;this.at=at;this.start=start;this.phase=phase;this.test=test;this.puzzle=puzzle;}
        JSONObject json() throws JSONException{return new JSONObject().put("routine",routine).put("at",at).put("start",start).put("phase",phase).put("test",test).put("puzzle",puzzle);}
    }
    static List<Entry> read(Context c){
        var prefs=Store.prefs(c);ArrayList<Entry> entries=new ArrayList<>();
        if(!prefs.getBoolean("ringing",false))return entries;
        try{JSONArray array=new JSONArray(prefs.getString("ringQueue","[]"));for(int i=0;i<array.length();i++){JSONObject o=array.getJSONObject(i);entries.add(new Entry(o.getString("routine"),o.getLong("at"),o.getLong("start"),o.getInt("phase"),o.getBoolean("test"),o.getString("puzzle")));}}catch(JSONException e){throw new IllegalStateException("Cannot read pending alarms",e);}
        if(entries.isEmpty())entries.add(new Entry(Store.activeRoutine(c),prefs.getLong("activeAt",0),prefs.getLong("activeStart",0),prefs.getInt("activePhase",0),prefs.getBoolean("activeTest",false),Store.activePuzzle(c).encode()));
        return entries;
    }
    static boolean contains(Context c,String id){for(Entry e:read(c))if(e.routine.equals(id))return true;return false;}
    static void write(Context c,List<Entry> entries){
        JSONArray array=new JSONArray();try{for(Entry e:entries)array.put(e.json());}catch(JSONException e){throw new IllegalStateException(e);}
        var edit=Store.prefs(c).edit().putString("ringQueue",array.toString()).putBoolean("ringing",!entries.isEmpty());
        if(!entries.isEmpty()){Entry e=entries.get(0);edit.putString("activeRoutine",e.routine).putLong("activeAt",e.at).putLong("activeStart",e.start).putInt("activePhase",e.phase).putBoolean("activeTest",e.test).putString("activePuzzle",e.puzzle);}
        edit.commit();
    }
    static void offer(Context c,String id,long at,long start,int phase,boolean test){
        List<Entry> entries=read(c);int index=-1;
        for(int i=0;i<entries.size();i++){Entry e=entries.get(i);if(e.routine.equals(id)&&e.test==test){if(e.at==at&&e.start==start)return;index=i;break;}}
        var prefs=Store.prefs(c);String day=java.time.Instant.ofEpochMilli(at).atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString();
        boolean same=!test&&prefs.getLong(Store.key(id,"ignoredStart"),-1)==start&&day.equals(prefs.getString(Store.key(id,"ignoredDay"),""));
        int ignored=same?prefs.getInt(Store.key(id,"ignored"),0):0;
        if(same&&index>=0)ignored++;
        if(!test)prefs.edit().putLong(Store.key(id,"ignoredStart"),start).putString(Store.key(id,"ignoredDay"),day).putInt(Store.key(id,"ignored"),ignored).apply();
        Plan plan=Store.plan(c,id);Puzzle puzzle=Store.newPuzzle(c,plan.puzzleType,Puzzle.level(!test&&plan.escalating,ignored));
        Entry entry=new Entry(id,at,start,phase,test,puzzle.encode());if(index<0)entries.add(entry);else entries.set(index,entry);write(c,entries);
        if(!test)SequenceStatus.delivered(c,id,start,at,phase);
    }
    static void removeRoutine(Context c,String id){List<Entry> entries=read(c);entries.removeIf(e->e.routine.equals(id));write(c,entries);}
    static void clear(Context c){write(c,new ArrayList<>());}
}
