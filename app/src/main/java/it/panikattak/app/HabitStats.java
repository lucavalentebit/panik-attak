package it.panikattak.app;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Pure history projection. No alarm state or UI is changed when calculating statistics. */
final class HabitStats {
    static final class Event {
        String id="",session="",routine="",name="",kind="",purpose="recall",zone="UTC",boot="",config="";
        long start,scheduled,wall,elapsed,first,last;
        int phase,type,level,expected,ordinal,offset,count=1;
        boolean covered=true;
    }
    static final class Session {
        String id,routine,name,zone,config,outcome="running";
        long start,first,last;int expected,phase,delivered,wrong,superseded;
        boolean partial,panic;
        Event delivery,response,ending;
        final List<Event> events=new ArrayList<>();
        final Set<Long> recalls=new HashSet<>();
        LocalDate date(){return Instant.ofEpochMilli(start).atZone(ZoneId.of(zone)).toLocalDate();}
        boolean complete(){return !partial&&delivery!=null&&ending!=null&&Arrays.asList("stop","finished","pause").contains(outcome);}
        Long delay(Event e){
            if(delivery==null||e==null||!delivery.boot.equals(e.boot)||delivery.boot.isEmpty())return null;
            long wall=e.wall-delivery.wall,elapsed=e.elapsed-delivery.elapsed,initial=delivery.wall-first;
            if(elapsed<0||initial<0||Math.abs(wall-elapsed)>2000||delivery.elapsed<initial)return null;
            return initial+elapsed;
        }
        Long firstDelay(){return complete()?delay(response):null;}
        Long stopDelay(){return complete()&&outcome.equals("stop")?delay(ending):null;}
    }
    static List<Session> sessions(List<Event> input){
        Map<String,Session> map=new LinkedHashMap<>();Set<String> ids=new HashSet<>();
        for(Event e:input){
            if(e.session.isEmpty()||!ids.add(e.id)||e.purpose.equals("preview")||e.purpose.equals("test")||e.purpose.equals("edit"))continue;
            Session s=map.get(e.session);if(s==null){s=new Session();s.id=e.session;s.routine=e.routine;s.name=e.name;s.zone=e.zone;s.start=e.start;s.first=e.first;s.last=e.last;s.expected=e.expected;s.config=e.config;map.put(s.id,s);}s.events.add(e);
        }
        for(Session s:map.values()){
            s.events.sort(Comparator.comparingLong((Event e)->bootNumber(e.boot)).thenComparing(e->e.boot).thenComparingLong(e->e.elapsed));
            Set<Integer> ordinals=new HashSet<>();Set<Long> pending=new HashSet<>();
            int retained=0;
            for(Event e:s.events){
                if(s.ending!=null)break;retained++;
                s.partial|=!e.covered;
                if(e.kind.equals("delivered")){
                    if(!pending.isEmpty())s.partial=true;pending.add(e.scheduled);
                    if(s.delivery==null){s.delivery=e;s.name=e.name;s.zone=e.zone;s.first=e.first;s.last=e.last;s.expected=e.expected;s.config=e.config;}
                    s.recalls.add(e.scheduled);ordinals.add(e.ordinal);s.panic|=e.phase==3;s.phase=e.phase;
                }
                else if(!e.kind.equals("partial")&&!e.kind.equals("interrupted")&&!s.recalls.contains(e.scheduled))s.partial=true;
                if(e.kind.equals("dismissed")||e.kind.equals("superseded"))pending.remove(e.scheduled);
                if(e.kind.equals("finished")&&!pending.isEmpty())s.partial=true;
                if(e.kind.equals("dismissed")&&s.response==null)s.response=e;
                if(e.kind.equals("wrong"))s.wrong+=e.count;
                if(e.kind.equals("superseded"))s.superseded++;
                if(e.kind.equals("partial"))s.partial=true;
                if(s.ending==null&&Arrays.asList("stop","finished","pause","interrupted").contains(e.kind)){s.ending=e;s.outcome=e.kind;s.phase=e.phase;}
            }
            s.events.subList(retained,s.events.size()).clear();
            s.delivered=s.recalls.size();
            if(s.delivery==null||s.delivery.scheduled!=s.first)s.partial=true;
            if(!ordinals.isEmpty()){int maximum=Collections.max(ordinals);for(int i=0;i<=maximum;i++)if(!ordinals.contains(i))s.partial=true;}
            if(s.outcome.equals("finished")&&s.delivered<s.expected)s.partial=true;
            if(s.delivery!=null)for(Event e:s.events)if(s.delay(e)==null)s.partial=true;
            if(s.name.isEmpty())s.name="Routine · nome non disponibile";
        }
        List<Session> result=new ArrayList<>(map.values());result.sort(Comparator.comparingLong((Session s)->s.start).reversed());return result;
    }
    static long bootNumber(String boot){try{return Long.parseLong(boot);}catch(NumberFormatException ignored){return Long.MIN_VALUE;}}
    static List<Session> filter(List<Session> sessions,LocalDate today,int days,String routine){
        LocalDate from=today.minusDays(days-1);List<Session> result=new ArrayList<>();
        for(Session s:sessions)if((routine.isEmpty()||routine.equals(s.routine))&&!s.date().isBefore(from)&&!s.date().isAfter(today))result.add(s);return result;
    }
    static final class Summary {
        final List<Long> responses=new ArrayList<>(),stops=new ArrayList<>();
        final List<List<Long>> weekdays=new ArrayList<>();
        final int[] phases=new int[4];
        int complete,panic,partial,running,finished,paused,interrupted,explicit;
        Summary(List<Session> sessions){
            for(int n=0;n<7;n++)weekdays.add(new ArrayList<>());
            for(Session s:sessions){
                if(s.partial)partial++;if(s.outcome.equals("running"))running++;
                if(s.outcome.equals("finished"))finished++;if(s.outcome.equals("pause"))paused++;if(s.outcome.equals("interrupted"))interrupted++;
                if(!s.complete())continue;complete++;if(s.panic)panic++;
                Long first=s.firstDelay();if(first!=null){responses.add(first);weekdays.get(s.date().getDayOfWeek().getValue()-1).add(first);}
                Long stop=s.stopDelay();if(stop!=null){stops.add(stop);explicit++;phases[s.phase]++;}
            }
        }
    }
    static Long median(List<Long> values){if(values.isEmpty())return null;List<Long> sorted=new ArrayList<>(values);Collections.sort(sorted);int n=sorted.size();return n%2==1?sorted.get(n/2):sorted.get(n/2-1)+(sorted.get(n/2)-sorted.get(n/2-1))/2;}
    static int[] histogram(List<Session> sessions,String outcome){
        int[] bins=new int[48];for(Session s:sessions)if(s.complete()&&s.outcome.equals(outcome)){OffsetDateTime local=Instant.ofEpochMilli(s.ending.wall).atOffset(ZoneOffset.ofTotalSeconds(s.ending.offset));bins[(local.getHour()*60+local.getMinute())/30]++;}return bins;
    }
    static String binLabel(int index){int m=index*30;return String.format(Locale.ITALIAN,"%02d:%02d–%02d:%02d",m/60,m%60,(m+29)/60,(m+29)%60);}
    static String duration(Long ms){if(ms==null)return "—";long seconds=Math.max(0,ms/1000);return seconds<60?seconds+" s":seconds<3600?String.format(Locale.ITALIAN,"%d′ %02d″",seconds/60,seconds%60):String.format(Locale.ITALIAN,"%dh %02dm",seconds/3600,seconds/60%60);}
    static String outcome(String kind){switch(kind){case "stop":return "Fermata da te";case "finished":return "Completata";case "pause":return "Messa in pausa";case "interrupted":return "Continuità interrotta";default:return "In corso";}}
    static String local(Event e){return Instant.ofEpochMilli(e.wall).atOffset(ZoneOffset.ofTotalSeconds(e.offset)).format(DateTimeFormatter.ofPattern("HH:mm:ss"));}
    static String csvCell(String value){if(value==null)value="";if(!value.isEmpty()&&"=+-@\t\r".indexOf(value.charAt(0))>=0)value="'"+value;return "\""+value.replace("\"","\"\"")+"\"";}
    static String csv(List<Session> sessions){
        StringBuilder out=new StringBuilder("session_id,routine_id,routine_name,session_date,start_utc,start_zone,outcome,complete_observation,delivered_recalls,first_response_utc,first_response_ms,ending_utc,ending_zone,ending_offset_seconds,explicit_stop_ms,panic_reached,wrong_attempts,superseded_recalls\r\n");
        for(Session s:sessions){String[] cells={s.id,s.routine,s.name,s.date().toString(),Instant.ofEpochMilli(s.first).toString(),s.zone,s.outcome,Boolean.toString(s.complete()),Integer.toString(s.delivered),s.response==null?"":Instant.ofEpochMilli(s.response.wall).toString(),s.firstDelay()==null?"":s.firstDelay().toString(),s.ending==null?"":Instant.ofEpochMilli(s.ending.wall).toString(),s.ending==null?"":s.ending.zone,s.ending==null?"":Integer.toString(s.ending.offset),s.stopDelay()==null?"":s.stopDelay().toString(),Boolean.toString(s.panic),Integer.toString(s.wrong),Integer.toString(s.superseded)};for(int i=0;i<cells.length;i++){if(i>0)out.append(',');out.append(csvCell(cells[i]));}out.append("\r\n");}return out.toString();
    }
}
