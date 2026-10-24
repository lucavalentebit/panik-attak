package it.panikattak.app;

import android.app.*;
import android.content.*;
import android.view.*;
import android.widget.*;
import androidx.appcompat.widget.SwitchCompat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

final class HabitsPage {
    static final String PERIOD="habits.period",ROUTINE="habits.routine",CLOCK="habits.clock",ENDING="habits.ending";
    static int days(Context c){int days=Store.prefs(c).getInt(PERIOD,30);return days==7||days==90?days:30;}
    static String routine(Context c){return Store.prefs(c).getString(ROUTINE,"");}
    static List<HabitStats.Session> selected(Context c,List<HabitStats.Session> all){return HabitStats.filter(all,LocalDate.now(),days(c),routine(c));}
    static LinearLayout card(MainActivity a,LinearLayout page,String title){LinearLayout card=Ui.card(a,Ui.CARD);card.addView(Ui.bold(a,title,18,Ui.TEXT));Ui.space(card,14);page.addView(card);Ui.space(page,16);return card;}
    static void hint(MainActivity a,LinearLayout parent,String value){parent.addView(Ui.text(a,value,12,Ui.MUTED));}
    static void preference(MainActivity a,String key,String value){Store.prefs(a).edit().putString(key,value).apply();a.render();}
    static void add(MainActivity a,LinearLayout page){
        page.addView(Ui.bold(a,"Abitudini",28,Ui.TEXT));Ui.space(page,8);hint(a,page,"Le tue risposte alle sveglie, un giorno alla volta.");Ui.space(page,20);
        LinearLayout periods=Ui.row(a);for(int days:new int[]{7,30,90}){TextView chip=Ui.button(a,days+" giorni",days==days(a)?Ui.LIME:Ui.CARD,days==days(a)?Ui.INK:Ui.MUTED,()->{Store.prefs(a).edit().putInt(PERIOD,days).apply();a.render();});chip.setTag("period:"+days);chip.setPadding(Ui.dp(a,6),Ui.dp(a,12),Ui.dp(a,6),Ui.dp(a,12));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMargins(Ui.dp(a,2),0,Ui.dp(a,2),0);periods.addView(chip,lp);}page.addView(periods);Ui.space(page,10);
        TextView filter=Ui.button(a,"Tutte le routine ▾",Ui.CARD,Ui.LIME,()->{});filter.setTag("habitRoutineFilter");page.addView(filter);Ui.space(page,20);
        if(!Store.prefs(a).getBoolean("history.introSeen",false)){
            LinearLayout intro=card(a,page,"Uno storico solo tuo");hint(a,intro,"Da questo aggiornamento registriamo orari e risultati delle sveglie su questo telefono. Nessun dato precedente viene ricostruito. Puoi disattivare, esportare o cancellare lo storico in Impostazioni.");Ui.space(intro,12);intro.addView(Ui.button(a,"Ho capito",0x24DCFA84,Ui.LIME,()->{Store.prefs(a).edit().putBoolean("history.introSeen",true).apply();a.render();}));
        }
        if(!HabitHistory.enabled(a)){LinearLayout disabled=card(a,page,"Registrazione in pausa");hint(a,disabled,"Lo storico già salvato resta qui. Le sveglie continuano a funzionare.");Ui.space(disabled,10);disabled.addView(Ui.button(a,"Gestisci lo storico",0x24DCFA84,Ui.LIME,a::openSettings));}
        LinearLayout results=Ui.column(a);page.addView(results);hint(a,results,"Caricamento dello storico…");int generation=a.shell.generation;int scroll=a.shell.positions.getInt("page:"+MainActivity.HABITS,0);
        HabitHistory.IO.execute(()->{
            try{List<HabitStats.Session> all=HabitStats.sessions(HabitHistory.events(a));a.runOnUiThread(()->{
                if(a.isFinishing()||a.shell.generation!=generation)return;
                results.removeAllViews();setFilter(a,filter,all);List<HabitStats.Session> sessions=selected(a,all);render(a,results,sessions);results.setTag("habitsLoaded");
                if(Store.prefs(a).getBoolean(HabitHistory.ERROR,false)){hint(a,results,"Alcuni eventi potrebbero non essere stati salvati. Le sveglie restano attive.");}
                a.shell.scroll.post(()->{if(a.shell.generation==generation)a.shell.scroll.scrollTo(0,scroll);});
            });}catch(RuntimeException error){HabitHistory.failed(a,error);a.runOnUiThread(()->{if(a.shell.generation==generation){results.removeAllViews();hint(a,results,"Storico temporaneamente non disponibile. Le sveglie continuano a funzionare.");results.addView(Ui.button(a,"Riprova",Ui.CARD,Ui.LIME,a::render));}});}
        });
    }
    static void setFilter(MainActivity a,TextView filter,List<HabitStats.Session> all){
        LinkedHashMap<String,String> names=new LinkedHashMap<>();names.put("","Tutte le routine");for(Store.Routine r:Store.routines(a))names.put(r.id,r.name);for(HabitStats.Session s:all)if(!names.containsKey(s.routine))names.put(s.routine,s.name+" · archiviata");
        String selected=routine(a);if(!names.containsKey(selected)){selected="";Store.prefs(a).edit().putString(ROUTINE,"").apply();}filter.setText(names.get(selected)+" ▾");String[] ids=names.keySet().toArray(new String[0]),labels=names.values().toArray(new String[0]);final String current=selected;
        filter.setOnClickListener(v->Ui.dialog(a).setTitle("Quale routine?").setSingleChoiceItems(labels,Arrays.asList(ids).indexOf(current),(dialog,n)->{dialog.dismiss();preference(a,ROUTINE,ids[n]);}).setNegativeButton("Annulla",null).show());
    }
    static void metric(MainActivity a,LinearLayout card,String label,String value,int count,String explanation){
        card.addView(Ui.bold(a,label,14,Ui.TEXT));Ui.space(card,7);card.addView(Ui.bold(a,count>=5?value:"—",30,Ui.LIME));Ui.space(card,6);hint(a,card,count+" sequenze · "+(count<5?"Dati ancora pochi":explanation));
    }
    static void render(MainActivity a,LinearLayout page,List<HabitStats.Session> sessions){
        if(sessions.isEmpty()){LinearLayout empty=card(a,page,"Ogni mattina lascia una traccia");hint(a,empty,a.getString(R.string.habits_empty_hint));Ui.space(empty,16);empty.addView(Ui.button(a,"Vai alle routine ↗",0x24DCFA84,Ui.LIME,()->a.navigate(MainActivity.ROUTINES)));return;}
        HabitStats.Summary summary=new HabitStats.Summary(sessions);
        LinearLayout overview=card(a,page,"Il tuo ritmo");metric(a,overview,"Prima risposta",HabitStats.duration(HabitStats.median(summary.responses)),summary.responses.size(),"mediana dalla prima sveglia");Ui.space(overview,16);SettingsPage.separator(overview);Ui.space(overview,16);
        metric(a,overview,"Quando scegli di fermarti",HabitStats.duration(HabitStats.median(summary.stops)),summary.stops.size(),"mediana fino a Ferma la mattina");Ui.space(overview,16);SettingsPage.separator(overview);Ui.space(overview,16);
        metric(a,overview,"Arrivi a Panic Mode",summary.complete==0?"—":Math.round(summary.panic*100f/summary.complete)+"%",summary.complete,summary.panic+" su "+summary.complete+" sequenze osservate interamente");Ui.space(overview,14);hint(a,overview,"Le medie non misurano quando ti alzi dal letto. Qui usiamo la mediana, solo su sequenze concluse con dati completi.");

        boolean clock=Store.prefs(a).getBoolean(CLOCK,false);LinearLayout plot=card(a,page,"Quando termini la sequenza");
        TextView toggle=Ui.button(a,clock?"Ora locale ↔ Tempo trascorso":"Tempo trascorso ↔ Ora locale",0x24DCFA84,Ui.LIME,()->{Store.prefs(a).edit().putBoolean(CLOCK,!clock).apply();a.render();});toggle.setTextSize(13);plot.addView(toggle);Ui.space(plot,12);
        HabitChart chart=new HabitChart(a,sessions,LocalDate.now(),days(a),clock,s->detail(a,s));chart.setTag("habitScatter");
        if(chart.points.isEmpty())hint(a,plot,"I punti appariranno quando una sequenza sarà conclusa e osservata interamente.");else plot.addView(chart,new LinearLayout.LayoutParams(-1,Ui.dp(a,240)));
        Ui.space(plot,10);hint(a,plot,"○ Prima risposta    ◆ Fine: fermata o completata\nTocca un punto per il dettaglio. "+(clock?"Ora all’evento; +1 indica il giorno successivo.":"Minuti dalla prima sveglia programmata. Nessuna linea nei giorni senza dati."));Ui.space(plot,12);plot.addView(Ui.button(a,"Mostra i dati",0x12FFFFFF,Ui.TEXT,()->table(a,sessions)));

        String outcome=Store.prefs(a).getString(ENDING,"stop");int[] bins=HabitStats.histogram(sessions,outcome);int total=Arrays.stream(bins).sum(),maximum=Arrays.stream(bins).max().orElse(0);
        LinearLayout distribution=card(a,page,"A che ora, più spesso?");String[] kinds={"stop","finished","pause"},labels={"Fermate da te","Completate","Messe in pausa"};int selected=Math.max(0,Arrays.asList(kinds).indexOf(outcome));
        distribution.addView(Ui.button(a,labels[selected]+" ▾",0x24DCFA84,Ui.LIME,()->Ui.dialog(a).setTitle("Quali conclusioni?").setSingleChoiceItems(labels,selected,(dialog,n)->{dialog.dismiss();preference(a,ENDING,kinds[n]);}).setNegativeButton("Annulla",null).show()));Ui.space(distribution,12);
        hint(a,distribution,total+" conclusioni · fasce di 30 minuti · ora locale all’evento");
        if(total==0){Ui.space(distribution,12);hint(a,distribution,"Nessuna conclusione di questo tipo nel periodo.");}else{
            distribution.addView(new HabitChart(a,bins),new LinearLayout.LayoutParams(-1,Ui.dp(a,150)));List<String> modes=new ArrayList<>();
            for(int n=0;n<bins.length;n++)if(bins[n]>0){hint(a,distribution,HabitStats.binLabel(n)+" · "+bins[n]+" su "+total);if(bins[n]==maximum)modes.add(HabitStats.binLabel(n));}
            Ui.space(distribution,12);hint(a,distribution,total<5?"Dati ancora pochi per indicare un orario abituale.":"Fasce più frequenti: "+String.join(", ",modes)+(modes.size()>1?" · a pari merito":""));
        }

        LinearLayout phases=card(a,page,"In quale fase ti fermi");for(int n=0;n<4;n++)bar(a,phases,Plan.NAMES[n],summary.phases[n],summary.explicit);
        Ui.space(phases,10);hint(a,phases,"Solo Ferma la mattina: "+summary.explicit+" sequenze complete.\nAltre conclusioni: "+summary.finished+" completate · "+summary.paused+" in pausa · "+summary.interrupted+" interrotte.\n"+summary.running+" ancora in corso · "+summary.partial+" con dati parziali.");

        LinearLayout weekdays=card(a,page,"Il ritmo della settimana");String[] names={"Lunedì","Martedì","Mercoledì","Giovedì","Venerdì","Sabato","Domenica"};for(int n=0;n<7;n++){List<Long> values=summary.weekdays.get(n);LinearLayout row=Ui.row(a);row.setMinimumHeight(Ui.dp(a,44));Ui.weighted(row,Ui.text(a,names[n],14,Ui.TEXT));row.addView(Ui.bold(a,(values.size()<5?"—":HabitStats.duration(HabitStats.median(values)))+" · n="+values.size(),13,Ui.LIME));weekdays.addView(row);}hint(a,weekdays,"Mediana fino alla prima risposta. Almeno 5 sequenze complete per giorno; “—” indica dati ancora pochi.");

        LinearLayout history=card(a,page,"Le tue sequenze · "+sessions.size());int[] shown={0};LinearLayout rows=Ui.column(a);history.addView(rows);TextView more=Ui.button(a,"Mostra altre",0x12FFFFFF,Ui.TEXT,()->{});history.addView(more);
        Runnable append=()->{int end=Math.min(shown[0]+20,sessions.size());for(;shown[0]<end;shown[0]++){HabitStats.Session s=sessions.get(shown[0]);TextView row=Ui.button(a,s.date().format(DateTimeFormatter.ofPattern("dd MMM",Locale.ITALIAN))+" · "+s.name+"\n"+HabitStats.outcome(s.outcome)+(s.partial?" · dati parziali":""),0x12FFFFFF,Ui.TEXT,()->detail(a,s));row.setTextSize(14);row.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);row.setTag("session:"+s.id);rows.addView(row);Ui.space(rows,8);}more.setVisibility(shown[0]<sessions.size()?View.VISIBLE:View.GONE);};more.setOnClickListener(v->append.run());append.run();Ui.space(history,12);history.addView(Ui.button(a,"Esporta questo periodo · CSV",0x24DCFA84,Ui.LIME,()->a.exportHabits(true)));
        hint(a,page,"I giorni senza una sequenza registrata non contano come insuccessi. Le prove e le anteprime non entrano nello storico.");
    }
    static void bar(MainActivity a,LinearLayout card,String name,int count,int total){
        LinearLayout row=Ui.row(a);Ui.weighted(row,Ui.text(a,name,14,Ui.TEXT));row.addView(Ui.bold(a,count+" / "+total,13,Ui.LIME));card.addView(row);Ui.space(card,7);
        LinearLayout track=Ui.row(a);track.setBackground(Ui.shape(0x229FA695,Ui.dp(a,3)));View fill=new View(a);fill.setBackground(Ui.shape(Ui.LIME,Ui.dp(a,3)));float part=total==0?0:count/(float)total;
        if(part>0)track.addView(fill,new LinearLayout.LayoutParams(0,Ui.dp(a,5),part));if(part<1)track.addView(new View(a),new LinearLayout.LayoutParams(0,Ui.dp(a,5),1-part));card.addView(track);Ui.space(card,14);
    }
    static void dialog(MainActivity a,String title,LinearLayout body){ScrollView scroll=new ScrollView(a);scroll.addView(body);Ui.dialog(a).setTitle(title).setView(scroll).setPositiveButton("Chiudi",null).show();}
    static LinearLayout dialogBody(MainActivity a){LinearLayout body=Ui.column(a);int pad=Ui.dp(a,24);body.setPadding(pad,Ui.dp(a,12),pad,Ui.dp(a,12));return body;}
    static void table(MainActivity a,List<HabitStats.Session> sessions){LinearLayout body=dialogBody(a);for(HabitStats.Session s:sessions){body.addView(Ui.button(a,s.date()+" · "+s.name+"\nPrima risposta: "+HabitStats.duration(s.firstDelay())+"\n"+HabitStats.outcome(s.outcome)+" · "+(s.ending==null?"—":HabitStats.local(s.ending))+(s.partial?" · dati parziali":""),Ui.CARD,Ui.TEXT,()->detail(a,s)));Ui.space(body,10);}dialog(a,"Dati del periodo",body);}
    static String eventLabel(HabitStats.Event e){switch(e.kind){case "delivered":return "Richiamo consegnato";case "displayed":return "Puzzle mostrato";case "solved":return "Puzzle risolto";case "dismissed":return "Spegni solo questa · accettato";case "stop":return "Ferma la mattina · accettato";case "pause":return "Pausa · accettata";case "finished":return "Sequenza completata";case "superseded":return "Richiamo sostituito, ancora irrisolto";case "wrong":return "Tentativi errati: "+e.count;case "partial":return "Registrazione parziale";default:return "Continuità interrotta";}}
    static void detail(MainActivity a,HabitStats.Session s){
        LinearLayout body=dialogBody(a);body.addView(Ui.bold(a,s.date()+" · "+HabitStats.outcome(s.outcome),16,Ui.LIME));Ui.space(body,10);hint(a,body,(s.complete()?"Osservazione completa":s.partial?"Dati parziali · esclusa dai riepiloghi":"Sequenza ancora aperta · esclusa dai riepiloghi")+"\nInizio programmato: "+Instant.ofEpochMilli(s.first).atZone(ZoneId.of(s.zone)).format(DateTimeFormatter.ofPattern("dd/MM HH:mm"))+" · "+s.zone+"\nRichiami consegnati: "+s.delivered+" su "+s.expected+" previsti");
        if(s.response!=null&&s.delivery!=null){Long delay=s.delay(s.response);hint(a,body,"Prima risposta: "+HabitStats.duration(s.firstDelay())+"\nDalla consegna: "+HabitStats.duration(delay==null?null:delay-(s.delivery.wall-s.first)));}
        Map<Long,HabitStats.Event> delivered=new HashMap<>(),displayed=new HashMap<>(),solved=new HashMap<>();
        for(HabitStats.Event e:s.events){Ui.space(body,16);body.addView(Ui.bold(a,HabitStats.local(e)+" · "+eventLabel(e),14,Ui.TEXT));hint(a,body,Plan.NAMES[Math.max(0,Math.min(3,e.phase))]+(e.purpose.equals("recall")?"":" · "+(e.purpose.equals("pause")?"sfida pausa":"sfida Ferma la mattina"))+" · "+e.zone);
            if(e.kind.equals("delivered")){delivered.put(e.scheduled,e);hint(a,body,"Programmato: "+Instant.ofEpochMilli(e.scheduled).atOffset(ZoneOffset.ofTotalSeconds(e.offset)).format(DateTimeFormatter.ofPattern("HH:mm:ss"))+" · ritardo consegna: "+(e.wall>=e.scheduled?HabitStats.duration(e.wall-e.scheduled):"orologio non valido"));}
            if(e.purpose.equals("recall")){
                if(e.kind.equals("displayed")){displayed.put(e.scheduled,e);HabitStats.Event d=delivered.get(e.scheduled);if(d!=null)hint(a,body,"Attesa prima del puzzle: "+HabitStats.duration(span(d,e)));}
                if(e.kind.equals("solved")){solved.put(e.scheduled,e);HabitStats.Event d=displayed.get(e.scheduled);if(d!=null)hint(a,body,"Tempo dalla visualizzazione, incluse interruzioni: "+HabitStats.duration(span(d,e)));}
                if(e.kind.equals("dismissed")){HabitStats.Event d=solved.get(e.scheduled);if(d!=null)hint(a,body,"Tra soluzione e conferma: "+HabitStats.duration(span(d,e)));}
            }
            if(e.kind.equals("displayed"))hint(a,body,a.getResources().getStringArray(R.array.puzzle_types)[e.type]+" · livello "+(e.level+1));
        }
        dialog(a,s.name,body);
    }
    static Long span(HabitStats.Event from,HabitStats.Event to){long elapsed=to.elapsed-from.elapsed;return !from.boot.isEmpty()&&from.boot.equals(to.boot)&&elapsed>=0&&Math.abs(to.wall-from.wall-elapsed)<=2000?elapsed:null;}
    static void settings(MainActivity a,LinearLayout page){
        LinearLayout history=card(a,page,"Storico delle abitudini");LinearLayout row=Ui.row(a);Ui.weighted(row,SettingsPage.labels(a,"Registra le risposte","Orari e risultati restano solo su questo telefono. Le sveglie funzionano anche senza storico."));SwitchCompat toggle=new SwitchCompat(a);toggle.setTag(HabitHistory.ENABLED);toggle.setChecked(HabitHistory.enabled(a));toggle.setContentDescription("Registra le risposte");toggle.setMinHeight(Ui.dp(a,48));toggle.setThumbTintList(Ui.controlTint());toggle.setOnCheckedChangeListener((v,on)->{HabitHistory.setEnabled(a,on);});row.addView(toggle);row.setOnClickListener(v->toggle.performClick());history.addView(row);SettingsPage.separator(history);Ui.space(history,12);
        history.addView(Ui.button(a,"Conserva per "+HabitHistory.retention(a)+" giorni ▾",0x12FFFFFF,Ui.TEXT,()->{int[] values={30,90,365};String[] labels={"30 giorni","90 giorni","365 giorni"};Ui.dialog(a).setTitle("Conservazione dello storico").setItems(labels,(d,n)->{Runnable apply=()->{HabitHistory.retain(a,values[n]);a.render();};if(values[n]<HabitHistory.retention(a))Ui.dialog(a).setTitle("Accorciare lo storico?").setMessage("Le sequenze più vecchie di "+values[n]+" giorni verranno eliminate, anche dagli eventi in attesa. Questa operazione non cambia le routine.").setNegativeButton("Annulla",null).setPositiveButton("Conferma",(dialog,which)->apply.run()).show();else apply.run();}).setNegativeButton("Annulla",null).show();}));Ui.space(history,10);
        history.addView(Ui.button(a,"Esporta tutto · CSV",0x24DCFA84,Ui.LIME,()->a.exportHabits(false)));Ui.space(history,10);
        history.addView(Ui.button(a,"Cancella lo storico",0x12FFFFFF,Ui.MUTED,()->Ui.dialog(a).setTitle("Cancellare lo storico?").setMessage("Elimina tutte le statistiche e gli eventi in attesa. Le routine e le sveglie restano configurate. Non puoi annullare questa operazione.").setNegativeButton("Annulla",null).setPositiveButton("Cancella",(dialog,n)->{try{HabitHistory.erase(a);a.render();Toast.makeText(a,"Storico cancellato",Toast.LENGTH_SHORT).show();}catch(RuntimeException error){Toast.makeText(a,"Non riesco a cancellare lo storico. Riprova.",Toast.LENGTH_LONG).show();}}).show()));
    }
}
