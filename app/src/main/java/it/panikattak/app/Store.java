package it.panikattak.app;
import android.content.*;
import org.json.*;
import java.util.*;

final class Store {
    static final String DEFAULT="default";
    static android.content.SharedPreferences prefs(Context c){return c.createDeviceProtectedStorageContext().getSharedPreferences("panik",Context.MODE_PRIVATE);}
    static String key(String id,String key){return DEFAULT.equals(id)?key:"routine."+id+"."+key;}
    static final class Routine {
        final String id,name;
        Routine(String id,String name){this.id=id;this.name=name;}
    }
    static List<Routine> routines(Context c){
        String raw=prefs(c).getString("routines",null);ArrayList<Routine> result=new ArrayList<>();
        if(raw!=null)try{JSONArray list=new JSONArray(raw);for(int i=0;i<list.length();i++){JSONObject o=list.getJSONObject(i);result.add(new Routine(o.getString("id"),o.getString("name")));}}catch(JSONException e){throw new IllegalStateException("Cannot read routines",e);}
        // Legacy values stay in place: upgrading never replaces the user's schedule.
        if(result.isEmpty())result.add(new Routine(DEFAULT,c.getString(R.string.default_routine)));
        return result;
    }
    static String encode(List<Routine> list){JSONArray array=new JSONArray();try{for(Routine r:list)array.put(new JSONObject().put("id",r.id).put("name",r.name));}catch(JSONException e){throw new IllegalStateException(e);}return array.toString();}
    static boolean exists(Context c,String id){for(Routine r:routines(c))if(r.id.equals(id))return true;return false;}
    static String selected(Context c){String id=prefs(c).getString("selectedRoutine",DEFAULT);return exists(c,id)?id:routines(c).get(0).id;}
    static String activeRoutine(Context c){return prefs(c).getString("activeRoutine",DEFAULT);}
    static String name(Context c,String id){for(Routine r:routines(c))if(r.id.equals(id))return r.name;return c.getString(R.string.default_routine);}
    static void select(Context c,String id){if(!exists(c,id))throw new IllegalArgumentException("Missing routine");prefs(c).edit().putString("selectedRoutine",id).apply();}
    static String checkedName(String value){String name=value.trim();if(name.isEmpty()||name.length()>40)throw new IllegalArgumentException("Invalid name");return name;}
    static boolean nameAvailable(Context c,String name,String except){for(Routine r:routines(c))if(!r.id.equals(except)&&r.name.equalsIgnoreCase(name.trim()))return false;return true;}
    static String create(Context c,String name,boolean duplicate){
        name=checkedName(name);if(!nameAvailable(c,name,""))throw new IllegalArgumentException("Duplicate name");
        String source=selected(c),id=UUID.randomUUID().toString();Plan p=duplicate?plan(c,source):new Plan();List<Routine> list=routines(c);list.add(new Routine(id,name));
        var e=prefs(c).edit().putString("routines",encode(list)).putString("selectedRoutine",id);writePlan(e,id,p);e.putBoolean(key(id,"enabled"),false).putBoolean(key(id,"vibration"),!duplicate||vibration(c,source)).apply();return id;
    }
    static void rename(Context c,String id,String name){
        name=checkedName(name);if(!nameAvailable(c,name,id))throw new IllegalArgumentException("Duplicate name");List<Routine> list=routines(c);for(int i=0;i<list.size();i++)if(list.get(i).id.equals(id))list.set(i,new Routine(id,name));prefs(c).edit().putString("routines",encode(list)).apply();
    }
    static boolean delete(Context c,String id){
        List<Routine> list=routines(c);if(list.size()==1||!exists(c,id)||enabled(c,id)||AlarmQueue.contains(c,id))return false;
        list.removeIf(r->r.id.equals(id));var e=prefs(c).edit().putString("routines",encode(list));
        if(selected(c).equals(id))e.putString("selectedRoutine",list.get(0).id);
        if(!DEFAULT.equals(id)){for(String k:prefs(c).getAll().keySet())if(k.startsWith("routine."+id+"."))e.remove(k);}
        else for(String k:new String[]{"hour","minute","days","puzzleType","escalating","enabled","vibration","skip","ignored","ignoredDay","ignoredStart"})e.remove(k);
        e.apply();return true;
    }
    static Plan plan(Context c){return plan(c,selected(c));}
    static Plan plan(Context c,String id){
        var s=prefs(c);Plan p=new Plan();p.hour=s.getInt(key(id,"hour"),7);p.minute=s.getInt(key(id,"minute"),30);p.days=s.getInt(key(id,"days"),127);p.puzzleType=s.getInt(key(id,"puzzleType"),0);p.escalating=s.getBoolean(key(id,"escalating"),false);
        for(int i=0;i<4;i++){p.intervals[i]=s.getInt(key(id,"interval"+i),p.intervals[i]);p.durations[i]=s.getInt(key(id,"duration"+i),p.durations[i]);p.fixed[i]=i<3&&s.getBoolean(key(id,"fixed"+i),false);p.fixedMinutes[i]=s.getInt(key(id,"fixedMinutes"+i),p.fixedMinutes[i]);}return p;
    }
    static void writePlan(android.content.SharedPreferences.Editor e,String id,Plan p){
        if(!p.valid())throw new IllegalArgumentException("Invalid plan");
        e.putInt(key(id,"hour"),p.hour).putInt(key(id,"minute"),p.minute).putInt(key(id,"days"),p.days).putInt(key(id,"puzzleType"),p.puzzleType).putBoolean(key(id,"escalating"),p.escalating);
        for(int i=0;i<4;i++)e.putBoolean(key(id,"fixed"+i),p.fixed[i]).putInt(key(id,"fixedMinutes"+i),p.fixedMinutes[i]).putInt(key(id,"interval"+i),p.intervals[i]).putInt(key(id,"duration"+i),p.durations[i]);
    }
    static void save(Context c,Plan p){save(c,selected(c),p);}
    static void save(Context c,String id,Plan p){if(!exists(c,id))throw new IllegalArgumentException("Missing routine");HabitHistory.runningEvent(c,id,"partial","recall");var e=prefs(c).edit();writePlan(e,id,p);e.apply();}
    static boolean enabled(Context c){return enabled(c,selected(c));}
    static boolean enabled(Context c,String id){return exists(c,id)&&prefs(c).getBoolean(key(id,"enabled"),false);}
    static void setEnabled(Context c,String id,boolean enabled){if(!exists(c,id))return;prefs(c).edit().putBoolean(key(id,"enabled"),enabled).apply();}
    static long skipped(Context c,String id){return prefs(c).getLong(key(id,"skip"),-1);}
    static boolean vibration(Context c,String id){return prefs(c).getBoolean(key(id,"vibration"),true);}
    static Puzzle newPuzzle(Context c,int type,int level){var s=prefs(c);String key="lastPuzzle"+type+"_"+level;Puzzle p=Puzzle.create(type,level,s.getString(key,""));s.edit().putString(key,p.encode()).apply();return p;}
    static Puzzle activePuzzle(Context c){var s=prefs(c);String value=s.getString("activePuzzle","");if(!value.isEmpty())return Puzzle.decode(value);Puzzle p=newPuzzle(c,plan(c,activeRoutine(c)).puzzleType,0);s.edit().putString("activePuzzle",p.encode()).apply();return p;}
    static String time(long at){return java.time.Instant.ofEpochMilli(at).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));}
}
