package it.panikattak.app;

import java.time.*;
import java.util.*;

/** Pure scheduling logic; all phases use offsets from the first alarm. */
public final class Plan {
    static final int MAX_PHASES = 24, MAX_DURATION = 120, PANIC = 3, INHERIT = -1;
    private static final int[] DEFAULT_CADENCES = {10, 5, 1, 1};
    private static final String[] DEFAULT_STATES = {"calm", "ready", "wake", "panic"};
    public static final String[] NAMES = {"Con calma", "Ci siamo", "Alzati", "Panic Mode"};
    String[] stateIds = DEFAULT_STATES.clone();
    String[] intervalIds = {"initial-0", "initial-1", "initial-2", "initial-3"};
    final Map<String, String> customStates = new LinkedHashMap<>();
    final Map<String, Integer> statePuzzles = new LinkedHashMap<>();
    public int hour=7, minute=30;
    public int[] intervals=DEFAULT_CADENCES.clone();
    public int[] durations={30,15,5,15};
    public int days=127;
    public boolean[] fixed={false,false,false,false};
    public int[] fixedMinutes={450,480,495,500};
    public int puzzleType=0;
    public boolean escalating=false;
    public int offset(int phase){int n=0;for(int i=0;i<phase;i++)n+=durations[i];return n;}
    public int fixedOffset(int phase){return Math.floorMod(fixedMinutes[phase]-(hour*60+minute),1440);}
    int phaseCount() { return durations.length; }
    String stateId(int phase) { return stateIds[phase]; }
    List<String> states() {
        List<String> ids = new ArrayList<>(Arrays.asList(DEFAULT_STATES));
        ids.addAll(customStates.keySet());
        return ids;
    }
    private boolean hasState(String id) {
        return builtIn(id) >= 0 || customStates.containsKey(id);
    }
    int builtIn(String id) { return Arrays.asList(DEFAULT_STATES).indexOf(id); }
    String stateName(String id) {
        int index = builtIn(id);
        return index >= 0 ? NAMES[index] : customStates.get(id);
    }
    String phaseName(int phase) {
        return phase >= 0 && phase < phaseCount() ? stateName(stateId(phase)) : NAMES[0];
    }
    int phaseLevel(int phase) {
        return phase >= 0 && phase < phaseCount() ? Math.max(0, builtIn(stateId(phase))) : 0;
    }
    int defaultCadence(int phase) { return DEFAULT_CADENCES[phaseLevel(phase)]; }
    int puzzleFor(int phase) {
        return phase >= 0 && phase < phaseCount() ? statePuzzles.getOrDefault(stateId(phase), puzzleType) : puzzleType;
    }
    int phaseAt(long start, long now) {
        long elapsed = Math.max(0, now - start);
        int minutes = 0;
        for (int phase = 0; phase < phaseCount(); phase++) {
            minutes += durations[phase];
            if (elapsed < minutes * 60_000L) return phase;
        }
        return phaseCount() - 1;
    }
    void append(String state, int duration) {
        int next = phaseCount();
        if (next >= MAX_PHASES || !hasState(state)) throw new IllegalArgumentException("Invalid interval");
        resize(next + 1);
        intervalIds[next] = UUID.randomUUID().toString();
        setState(next, state);
        durations[next] = duration;
        intervals[next] = defaultCadence(next);
        fixedMinutes[next] = Math.floorMod(hour * 60 + minute + offset(next), 1440);
    }
    void setState(int phase, String state) {
        if (!hasState(state)) throw new IllegalArgumentException("Unknown state");
        stateIds[phase] = state;
        if (phaseLevel(phase) == PANIC) fixed[phase] = false;
    }
    boolean remove(int phase) {
        int count = phaseCount();
        if (count == 1) return false;
        int remaining = count - phase - 1;
        System.arraycopy(intervalIds, phase + 1, intervalIds, phase, remaining);
        System.arraycopy(stateIds, phase + 1, stateIds, phase, remaining);
        System.arraycopy(durations, phase + 1, durations, phase, remaining);
        System.arraycopy(intervals, phase + 1, intervals, phase, remaining);
        System.arraycopy(fixed, phase + 1, fixed, phase, remaining);
        System.arraycopy(fixedMinutes, phase + 1, fixedMinutes, phase, remaining);
        resize(count - 1);
        return true;
    }
    void resize(int count) {
        intervalIds = Arrays.copyOf(intervalIds, count);
        stateIds = Arrays.copyOf(stateIds, count);
        durations = Arrays.copyOf(durations, count);
        intervals = Arrays.copyOf(intervals, count);
        fixed = Arrays.copyOf(fixed, count);
        fixedMinutes = Arrays.copyOf(fixedMinutes, count);
    }
    boolean stateNameAvailable(String name, String except) {
        if (name.isEmpty() || name.length() > 40) return false;
        for (String id : states()) {
            if (!id.equals(except) && name.equalsIgnoreCase(stateName(id))) return false;
        }
        return true;
    }
    public boolean valid(){
        if(hour<0||hour>23||minute<0||minute>59||days<1||days>127||puzzleType<0||puzzleType>5||phaseCount()<1||phaseCount()>MAX_PHASES||total()>1440)return false;
        int count=phaseCount();
        if(intervalIds.length!=count||stateIds.length!=count||intervals.length!=count
                ||fixed.length!=count||fixedMinutes.length!=count)return false;
        Set<String> unique = new HashSet<>();
        for (var puzzle : statePuzzles.entrySet()) {
            if (!hasState(puzzle.getKey()) || puzzle.getValue() < 0 || puzzle.getValue() > 5) return false;
        }
        for (int i = 0; i < phaseCount(); i++) {
            if (intervalIds[i] == null || intervalIds[i].isEmpty() || !unique.add(intervalIds[i])
                    || !hasState(stateIds[i]) || (fixed[i] && phaseLevel(i) == PANIC)) return false;
        }
        for(int i=0;i<phaseCount();i++)if(durations[i]<1||durations[i]>MAX_DURATION||intervals[i]<1||intervals[i]>30||
            (fixed[i]&&(fixedMinutes[i]<0||fixedMinutes[i]>=1440||fixedOffset(i)<offset(i)||fixedOffset(i)>=offset(i)+durations[i])))return false;
        return true;
    }
    public static final class Event {
        public final long at, start; public final int phase;
        Event(long at,long start,int phase) { this.at=at; this.start=start; this.phase=phase; }
    }
    public List<Event> events(LocalDate date, ZoneId zone) {
        long start=date.atTime(hour,minute).atZone(zone).toInstant().toEpochMilli();
        ArrayList<Event> result=new ArrayList<>(); int offset=0;
        for(int p=0;p<phaseCount();p++) {
            if(fixed[p] && phaseLevel(p) != PANIC){
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
        long span=total()*60_000L;
        Event earliest=null;
        for(int d=-2;d<=8;d++) {
            LocalDate date=today.plusDays(d);
            if((days & (1<<(date.getDayOfWeek().getValue()-1)))==0) continue;
            long dayStart=date.atTime(hour,minute).atZone(zone).toInstant().toEpochMilli();
            if(earliest!=null&&dayStart>=earliest.at)break;
            if(dayStart==skippedStart||dayStart+span<=after)continue;
            for(Event event:events(date,zone)) {
                if(event.at<=after)continue;
                if(earliest==null||event.at<earliest.at)earliest=event;
                break;
            }
        }
        return earliest;
    }
    public int count() { int n=0;for(int i=0;i<phaseCount();i++)n+=fixed[i]&&phaseLevel(i)!=PANIC?1:(durations[i]+intervals[i]-1)/intervals[i];return n; }
    public int total() {return Arrays.stream(durations).sum();}
}
