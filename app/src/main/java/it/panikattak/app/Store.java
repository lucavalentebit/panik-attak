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
        else for(String k:new String[]{"hour","minute","days","puzzleType","escalating","enabled","vibration","skip","ignored","ignoredDay","ignoredStart","phasePlan"})e.remove(k);
        e.apply();return true;
    }
    static Plan plan(Context c){return plan(c,selected(c));}
    static Plan plan(Context c,String id){
        var s=prefs(c);Plan p=new Plan();p.hour=s.getInt(key(id,"hour"),7);p.minute=s.getInt(key(id,"minute"),30);p.days=s.getInt(key(id,"days"),127);p.puzzleType=s.getInt(key(id,"puzzleType"),0);p.escalating=s.getBoolean(key(id,"escalating"),false);
        String phases = s.getString(key(id,"phasePlan"), null);
        if (phases != null) {
            try {
                readPhases(p, new JSONObject(phases));
                if (p.valid()) return p;
            } catch (JSONException | IllegalArgumentException invalid) {
                android.util.Log.w("PanikAttak", "Cannot read routine intervals", invalid);
            }
            p.customStates.clear();
            p.statePuzzles.clear();
            Plan defaults = new Plan();
            p.stateIds = defaults.stateIds;
            p.intervalIds = defaults.intervalIds;
            p.intervals = defaults.intervals;
            p.durations = defaults.durations;
            p.fixed = defaults.fixed;
            p.fixedMinutes = defaults.fixedMinutes;
        }
        for(int i=0;i<p.phaseCount();i++){p.intervals[i]=s.getInt(key(id,"interval"+i),p.intervals[i]);p.durations[i]=s.getInt(key(id,"duration"+i),p.durations[i]);p.fixed[i]=i<3&&s.getBoolean(key(id,"fixed"+i),false);p.fixedMinutes[i]=s.getInt(key(id,"fixedMinutes"+i),p.fixedMinutes[i]);}return p;
    }
    static void writePlan(android.content.SharedPreferences.Editor e,String id,Plan p){
        if(!p.valid())throw new IllegalArgumentException("Invalid plan");
        e.putInt(key(id,"hour"),p.hour).putInt(key(id,"minute"),p.minute).putInt(key(id,"days"),p.days).putInt(key(id,"puzzleType"),p.puzzleType).putBoolean(key(id,"escalating"),p.escalating);
        e.putString(key(id,"phasePlan"), encodePhases(p));
    }
    private static String encodePhases(Plan plan) {
        JSONArray intervals = new JSONArray();
        try {
            for (int i = 0; i < plan.phaseCount(); i++) {
                intervals.put(new JSONObject().put("id", plan.intervalIds[i]).put("state", plan.stateId(i))
                        .put("duration", plan.durations[i]).put("cadence", plan.intervals[i])
                        .put("fixed", plan.fixed[i]).put("time", plan.fixedMinutes[i]));
            }
            return new JSONObject().put("intervals", intervals)
                    .put("states", new JSONObject(plan.customStates))
                    .put("puzzles", new JSONObject(plan.statePuzzles)).toString();
        } catch (JSONException impossible) {
            throw new IllegalStateException("Cannot write routine intervals", impossible);
        }
    }

    private static void readPhases(Plan plan, JSONObject value) throws JSONException {
        JSONArray rows = value.getJSONArray("intervals");
        if (rows.length() < 1 || rows.length() > Plan.MAX_PHASES) throw new JSONException("Invalid number of intervals");
        JSONObject states = value.getJSONObject("states");
        for (Iterator<String> it = states.keys(); it.hasNext();) {
            String id = it.next();
            String name = states.getString(id);
            if (id.isEmpty() || plan.builtIn(id) >= 0 || !plan.stateNameAvailable(name, "")) {
                throw new JSONException("Invalid custom state");
            }
            plan.customStates.put(id, name);
        }
        JSONObject puzzles = value.getJSONObject("puzzles");
        for (Iterator<String> it = puzzles.keys(); it.hasNext();) {
            String id = it.next();
            int puzzle=puzzles.optInt(id,Plan.INHERIT);
            if(puzzle>=0&&puzzle<=5&&plan.stateName(id)!=null)plan.statePuzzles.put(id,puzzle);
        }
        plan.resize(rows.length());
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i);
            plan.intervalIds[i] = row.getString("id");
            plan.stateIds[i] = row.getString("state");
            plan.durations[i] = row.getInt("duration");
            plan.intervals[i] = row.getInt("cadence");
            plan.fixed[i] = row.getBoolean("fixed");
            plan.fixedMinutes[i] = row.getInt("time");
        }
    }

    static void save(Context c,Plan p){save(c,selected(c),p);}
    static void save(Context c,String id,Plan p){if(!exists(c,id))throw new IllegalArgumentException("Missing routine");HabitHistory.runningEvent(c,id,"partial","recall");var e=prefs(c).edit();writePlan(e,id,p);e.apply();}
    static boolean enabled(Context c){return enabled(c,selected(c));}
    static boolean enabled(Context c,String id){return exists(c,id)&&prefs(c).getBoolean(key(id,"enabled"),false);}
    static void setEnabled(Context c,String id,boolean enabled){if(!exists(c,id))return;prefs(c).edit().putBoolean(key(id,"enabled"),enabled).apply();}
    static long skipped(Context c,String id){return prefs(c).getLong(key(id,"skip"),-1);}
    static boolean vibration(Context c,String id){return prefs(c).getBoolean(key(id,"vibration"),true);}
    static Puzzle newPuzzle(Context c,int type,int level){var s=prefs(c);String key="lastPuzzle"+type+"_"+level;Puzzle p=Puzzle.create(type,level,s.getString(key,""));s.edit().putString(key,p.encode()).apply();return p;}
    static Puzzle activePuzzle(Context c){var s=prefs(c);String value=s.getString("activePuzzle","");if(!value.isEmpty())return Puzzle.decode(value);var plan=plan(c,activeRoutine(c));var routinePrefs=prefs(c);int phase=plan.phaseAt(routinePrefs.getLong("activeStart",0),routinePrefs.getLong("activeAt",0));Puzzle p=newPuzzle(c,routinePrefs.getBoolean("activeTest",false)?plan.puzzleType:plan.puzzleFor(phase),0);s.edit().putString("activePuzzle",p.encode()).apply();return p;}
    static String time(long at){return java.time.Instant.ofEpochMilli(at).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));}
}
