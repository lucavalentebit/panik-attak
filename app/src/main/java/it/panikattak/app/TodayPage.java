package it.panikattak.app;

import android.content.Intent;
import android.view.Gravity;
import android.widget.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

final class TodayPage {
    static final class Upcoming {
        final Store.Routine routine;final Plan.Event event;
        Upcoming(Store.Routine routine,Plan.Event event){this.routine=routine;this.event=event;}
    }
    static List<Upcoming> upcoming(MainActivity a,long now){
        List<Upcoming> result=new ArrayList<>();
        for(var routine:Store.routines(a))if(Store.enabled(a,routine.id)){
            Plan.Event event=Store.plan(a,routine.id).next(now,Store.skipped(a,routine.id),ZoneId.systemDefault());
            if(event!=null)result.add(new Upcoming(routine,event));
        }
        result.sort(Comparator.comparingLong((Upcoming u)->u.event.at).thenComparing(u->u.routine.name));return result;
    }
    static String date(long at){
        LocalDate day=Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).toLocalDate();
        return day.equals(LocalDate.now())?"Oggi":day.equals(LocalDate.now().plusDays(1))?"Domani":day.format(DateTimeFormatter.ofPattern("EEE d MMM",Locale.ITALIAN));
    }
    static void add(MainActivity a,LinearLayout page){
        page.addView(Ui.bold(a,a.getString(R.string.page_today),28,Ui.TEXT));Ui.space(page,6);
        page.addView(Ui.text(a,LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d MMMM",Locale.ITALIAN)),14,Ui.MUTED));Ui.space(page,24);
        List<SequenceStatus.Run> runs=SequenceStatus.current(a,System.currentTimeMillis());
        if(!runs.isEmpty()){
            page.addView(Ui.bold(a,a.getString(R.string.running_count,runs.size()),18,Ui.TEXT));Ui.space(page,12);
            for(var run:runs){
                LinearLayout card=Ui.card(a,Ui.CARD);card.addView(Ui.bold(a,Store.name(a,run.routine),18,Ui.LIME));Ui.space(card,8);
                card.addView(Ui.text(a,Plan.NAMES[run.phase]+" · "+(run.ringing?a.getString(R.string.recall_pending):a.getString(R.string.recall_at,Store.time(run.nextAt))),14,Ui.MUTED));Ui.space(card,14);
                card.addView(Ui.button(a,a.getString(run.ringing?R.string.open_puzzle:R.string.open_routine),0x12FFFFFF,Ui.TEXT,()->{if(run.ringing)a.startActivity(new Intent(a,AlarmActivity.class));else a.openRoutine(run.routine);}));page.addView(card);Ui.space(page,12);
            }
            Ui.space(page,12);
        }
        List<Upcoming> next=upcoming(a,System.currentTimeMillis());
        if(next.isEmpty()){
            LinearLayout card=Ui.card(a,Ui.CARD);card.addView(Ui.bold(a,a.getString(R.string.no_active_routines),22,Ui.TEXT));Ui.space(card,10);
            card.addView(Ui.text(a,a.getString(R.string.choose_routine_hint),14,Ui.MUTED));Ui.space(card,20);
            card.addView(Ui.button(a,a.getString(R.string.choose_routine),Ui.LIME,Ui.INK,()->a.navigate(MainActivity.ROUTINES)));page.addView(card);
        }else{
            Upcoming first=next.get(0);List<Upcoming> tied=new ArrayList<>();for(var item:next)if(item.event.at==first.event.at)tied.add(item);
            LinearLayout card=Ui.card(a,Ui.LIME);card.addView(Ui.bold(a,a.getString(R.string.next_alarm),13,Ui.INK));Ui.space(card,12);
            card.addView(Ui.bold(a,first.routine.name,22,Ui.INK));if(tied.size()>1){Ui.space(card,6);card.addView(Ui.text(a,a.getString(R.string.more_routines,tied.size()-1),14,Ui.INK));}
            Ui.space(card,20);LinearLayout timing=Ui.row(a);Ui.weighted(timing,Ui.bold(a,date(first.event.at),13,Ui.INK));
            a.countdown=Ui.bold(a,"",15,Ui.INK);a.countdownAt=first.event.at;a.countdown.setGravity(Gravity.END);timing.addView(a.countdown);card.addView(timing);
            Ui.space(card,8);card.addView(Ui.text(a,Store.time(first.event.at),60,Ui.INK));Ui.space(card,16);
            card.addView(Ui.button(a,a.getString(tied.size()>1?R.string.choose_routine:R.string.open_routine),Ui.INK,Ui.LIME,()->{
                if(tied.size()==1)a.openRoutine(first.routine.id);
                else a.chooseRoutine(tied);
            }));page.addView(card);
            if(next.size()>tied.size()){
                Ui.space(page,26);page.addView(Ui.bold(a,a.getString(R.string.also_scheduled),18,Ui.TEXT));Ui.space(page,12);
                for(int i=tied.size();i<Math.min(next.size(),tied.size()+3);i++){
                    Upcoming item=next.get(i);TextView row=Ui.button(a,item.routine.name+"\n"+date(item.event.at)+" · "+Store.time(item.event.at),Ui.CARD,Ui.TEXT,()->a.openRoutine(item.routine.id));row.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);page.addView(row);Ui.space(page,8);
                }
            }
            Ui.space(page,16);page.addView(Ui.button(a,a.getString(R.string.see_routines),Ui.CARD,Ui.TEXT,()->a.navigate(MainActivity.ROUTINES)));
        }
        if(!a.notifications()||!Scheduler.allowed(a)||!a.fullscreen()){
            Ui.space(page,20);LinearLayout warning=Ui.card(a,Ui.CARD);warning.addView(Ui.bold(a,"Completa la configurazione",17,Ui.ORANGE));Ui.space(warning,8);
            warning.addView(Ui.text(a,"Consenti i permessi necessari per ricevere i richiami anche a schermo spento.",13,Ui.MUTED));Ui.space(warning,14);
            warning.addView(Ui.button(a,"Controlla permessi ↗",0x12FFFFFF,Ui.TEXT,a::settings));page.addView(warning);
        }
    }
}
