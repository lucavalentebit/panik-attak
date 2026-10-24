package it.panikattak.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MainActivity extends androidx.appcompat.app.AppCompatActivity {
    TextView countdown;long countdownAt;boolean visible;
    final Handler countdownHandler=new Handler(Looper.getMainLooper());
    final Runnable countdownTick=()->updateCountdown();
    static String remaining(long at,long now){long seconds=Math.max(0,(at-now+999)/1000);return String.format(Locale.ITALIAN,"%02d:%02d:%02d",seconds/3600,(seconds/60)%60,seconds%60);}
    void updateCountdown(){
        countdownHandler.removeCallbacks(countdownTick);if(countdown==null)return;
        long now=System.currentTimeMillis();
        if(countdownAt>0&&now>=countdownAt){render();return;}
        countdown.setText(countdownAt>0?remaining(countdownAt,now):"--:--:--");
        countdown.setContentDescription(countdownAt>0?"Tempo alla prossima sveglia: "+countdown.getText():"Routine in pausa");
        if(visible&&countdownAt>0)countdownHandler.postDelayed(countdownTick,1000-now%1000);
    }
    @Override protected void onPause(){visible=false;countdownHandler.removeCallbacks(countdownTick);super.onPause();}
    Plan plan; int pendingEdit=-1;String pendingRoutine,pendingDelete;
    @Override public void onCreate(Bundle b){super.onCreate(b);SequenceStatus.openFromNotification(this,getIntent());
        getSupportFragmentManager().registerFragmentLifecycleCallbacks(new androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks(){
            @Override public void onFragmentStarted(androidx.fragment.app.FragmentManager manager,androidx.fragment.app.Fragment fragment){
                if(fragment instanceof com.google.android.material.timepicker.MaterialTimePicker){var dialog=((com.google.android.material.timepicker.MaterialTimePicker)fragment).getDialog();if(dialog!=null){Ui.dialogButton(dialog.findViewById(com.google.android.material.R.id.material_timepicker_ok_button),true);Ui.dialogButton(dialog.findViewById(com.google.android.material.R.id.material_timepicker_cancel_button),false);}}
            }
        },false);
        if(b!=null){pendingEdit=b.getInt("pendingEdit",-1);pendingRoutine=b.getString("pendingRoutine");pendingDelete=b.getString("pendingDelete");}}
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);SequenceStatus.openFromNotification(this,intent);if(!Store.prefs(this).getBoolean("ringing",false))render();}
    @Override protected void onSaveInstanceState(Bundle out){out.putInt("pendingEdit",pendingEdit);out.putString("pendingRoutine",pendingRoutine);out.putString("pendingDelete",pendingDelete);super.onSaveInstanceState(out);}
    void requestEdit(int edit){
        if(Store.enabled(this)){pendingEdit=edit;pendingRoutine=Store.selected(this);startActivityForResult(ChallengeActivity.intent(this,ChallengeActivity.EDIT),81);}
        else openEditor(edit);
    }
    void openEditor(int edit){plan=Store.plan(this);if(edit==0)showTimeEditor();else if(edit==1)showDaysEditor();else if(edit>=2&&edit<=5)showPhaseEditor(edit-2);else if(edit==6)showPuzzleEditor();else if(edit==7){plan.escalating=!plan.escalating;save();}}
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request==81){int edit=pendingEdit;String id=pendingRoutine;pendingEdit=-1;pendingRoutine=null;
            if(result==RESULT_OK&&id!=null&&Store.exists(this,id)&&!Store.prefs(this).getBoolean("ringing",false)){Store.select(this,id);openEditor(edit);}}
        if(request==82){String id=pendingDelete;pendingDelete=null;if(result==RESULT_OK&&id!=null){Store.delete(this,id);Scheduler.schedule(this);render();}}
    }
    @Override public void onResume(){super.onResume();visible=true;if(Store.prefs(this).getBoolean("ringing",false)){startActivity(new Intent(this,AlarmActivity.class));return;}Scheduler.schedule(this);render();}
    void activate(){activate(Store.selected(this));}
    void activate(String routine){
        if(!Store.exists(this,routine))return;
        if(!ready())return;
        CallRinger.Result result=CallRinger.enable(this);
        if(result==CallRinger.Result.NEED_ACCESS){
            Ui.dialog(this).setTitle("Consenti la gestione della suoneria")
                .setMessage("Android richiede l'accesso a Non disturbare per uscire dal muto. Panik Attak riattiva la suoneria senza cambiare il filtro delle chiamate o i tuoi preferiti. Consenti l'accesso, poi torna e premi Attiva.")
                .setPositiveButton("Apri autorizzazioni",(d,w)->startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)))
                .setNegativeButton("Annulla",null).show();return;
        }
        if(result!=CallRinger.Result.READY){
            Ui.dialog(this).setTitle("Controlla le chiamate in arrivo")
                .setMessage(result==CallRinger.Result.DND_BLOCKS_CALLS?"Non disturbare sta bloccando tutte le chiamate. Consenti quelle dei preferiti nelle impostazioni, poi attiva la sequenza.":"Non riesco a confermare la suoneria attiva con Non disturbare invariato. Controlla le impostazioni audio, poi riprova.")
                .setPositiveButton("Apri impostazioni",(d,w)->startActivity(new Intent(result==CallRinger.Result.DND_BLOCKS_CALLS?Settings.ACTION_ZEN_MODE_PRIORITY_SETTINGS:Settings.ACTION_SOUND_SETTINGS)))
                .setNegativeButton("Chiudi",null).show();return;
        }
        Store.prefs(this).edit().putBoolean(Store.key(routine,"enabled"),true).remove(Store.key(routine,"skip")).apply();Scheduler.schedule(this);render();
        Toast.makeText(this,"Sequenza attiva · suoneria accesa · Non disturbare invariato",Toast.LENGTH_LONG).show();
    }
    void render(){
        plan=Store.plan(this);LinearLayout page=Ui.shell(this,true);
        LinearLayout brand=Ui.row(this);TextView logo=Ui.bold(this,"ϟ",32,Ui.LIME);brand.addView(logo);LinearLayout brandText=Ui.column(this);brandText.setPadding(Ui.dp(this,12),0,0,0);brandText.addView(Ui.bold(this,"Panik Attak",23,Ui.TEXT));Ui.space(brandText,5);TextView tag=Ui.text(this,"IL LETTO HA PERSO.",10,Ui.MUTED);tag.setLetterSpacing(.14f);brandText.addView(tag);Ui.weighted(brand,brandText);page.addView(brand);Ui.space(page,28);
        RoutineUi.add(this,page);
        LinearLayout heading=Ui.row(this);Ui.weighted(heading,Ui.bold(this,"Il tuo prossimo risveglio",14,Ui.MUTED));TextView status=Ui.bold(this,Store.enabled(this)?"●  ATTIVA":"○  IN PAUSA",10,Store.enabled(this)?Ui.LIME:Ui.MUTED);heading.addView(status);page.addView(heading);Ui.space(page,12);
        LinearLayout hero=Ui.card(this,Ui.LIME);
        String selected=Store.selected(this);
        Plan.Event upcoming=Store.enabled(this)?plan.next(System.currentTimeMillis(),Store.skipped(this,selected),ZoneId.systemDefault()):null;long next=upcoming==null?0:upcoming.at;
        String dateLabel="LA TUA PRIMA SVEGLIA";
        if(Store.enabled(this)&&next>0){LocalDate date=Instant.ofEpochMilli(next).atZone(ZoneId.systemDefault()).toLocalDate();dateLabel=(date.equals(LocalDate.now())?"OGGI":date.equals(LocalDate.now().plusDays(1))?"DOMANI":date.format(DateTimeFormatter.ofPattern("EEEE d MMM",Locale.ITALIAN))).toUpperCase(Locale.ITALIAN);}
        hero.addView(Ui.bold(this,Store.name(this,selected),18,Ui.INK));Ui.space(hero,10);
        TextView label=Ui.bold(this,dateLabel,11,Ui.INK);label.setLetterSpacing(.12f);LinearLayout timing=Ui.row(this);Ui.weighted(timing,label);
        countdown=Ui.bold(this,"",14,Ui.INK);countdown.setGravity(Gravity.END);countdown.setPadding(Ui.dp(this,8),0,0,0);countdownAt=next;timing.addView(countdown);hero.addView(timing);updateCountdown();Ui.space(hero,10);
        LinearLayout clock=Ui.row(this);TextView time=Ui.text(this,Store.enabled(this)&&next>0?Store.time(next):String.format(Locale.ITALIAN,"%02d:%02d",plan.hour,plan.minute),66,Ui.INK);time.setLetterSpacing(-.05f);Ui.weighted(clock,time);TextView change=Ui.button(this,"Modifica ↗",0x25000000,Ui.INK,this::editTime);clock.addView(change);hero.addView(clock);Ui.space(hero,12);
        hero.addView(Ui.text(this,dayLabel()+"  ·  "+plan.count()+" richiami",13,Ui.INK));Ui.space(hero,21);
        hero.addView(Ui.button(this,Store.enabled(this)?"Metti in pausa · risolvi":"Attiva la sequenza  →",Ui.INK,Ui.LIME,()->{if(Store.enabled(this)){startActivity(ChallengeActivity.intent(this,ChallengeActivity.PAUSE));}else activate();}));page.addView(hero);Ui.space(page,26);
        LinearLayout sequenceHeader=Ui.row(this);Ui.weighted(sequenceHeader,Ui.bold(this,"Dal sonno al panico",21,Ui.TEXT));TextView preview=Ui.text(this,"Anteprima ↗",12,Ui.LIME);preview.setPadding(6,14,0,14);preview.setOnClickListener(v->preview());sequenceHeader.addView(preview);page.addView(sequenceHeader);Ui.space(page,7);page.addView(Ui.text(this,"Quattro fasi. Sempre meno scuse.",13,Ui.MUTED));Ui.space(page,16);
        int offset=0;
        for(int phase=0;phase<4;phase++){
            final int p=phase;LinearLayout card=Ui.row(this);card.setPadding(Ui.dp(this,16),Ui.dp(this,17),Ui.dp(this,16),Ui.dp(this,17));card.setBackground(Ui.shape(Ui.CARD,Ui.dp(this,21)));
            TextView number=Ui.bold(this,phase==3?"ϟ":"0"+(phase+1),phase==3?27:15,Ui.COLORS[phase]);number.setGravity(Gravity.CENTER);number.setBackground(Ui.shape(0x14FFFFFF,Ui.dp(this,14)));card.addView(number,new LinearLayout.LayoutParams(Ui.dp(this,45),Ui.dp(this,45)));
            LinearLayout words=Ui.column(this);words.setPadding(Ui.dp(this,14),0,0,0);words.addView(Ui.bold(this,Plan.NAMES[phase],16,Ui.TEXT));Ui.space(words,6);words.addView(Ui.text(this,timeAt(offset)+" – "+timeAt(offset+plan.durations[phase])+"  ·  "+plan.durations[phase]+" min",11,Ui.MUTED));Ui.weighted(card,words);
            LinearLayout cadence=Ui.column(this);TextView interval=Ui.bold(this,cadence(phase),16,Ui.COLORS[phase]);interval.setGravity(Gravity.END);cadence.addView(interval);Ui.space(cadence,6);TextView edit=Ui.text(this,"modifica",10,Ui.MUTED);edit.setGravity(Gravity.END);cadence.addView(edit);card.addView(cadence,new LinearLayout.LayoutParams(Ui.dp(this,80),-2));card.setOnClickListener(v->editPhase(p));card.setContentDescription(Plan.NAMES[phase]+", "+cadence(phase));page.addView(card);Ui.space(page,9);offset+=plan.durations[phase];
        }
        Ui.space(page,7);page.addView(Ui.text(this,"Panic Mode: suono pieno e vibrazione insistente.\nDopo "+timeAt(plan.total())+" non partono altri richiami; quello in corso continua finché lo risolvi.",12,Ui.MUTED));Ui.space(page,24);
        LinearLayout options=Ui.card(this,Ui.CARD);options.addView(Ui.bold(this,"A modo tuo",18,Ui.TEXT));Ui.space(options,18);
        options.addView(Ui.button(this,"Ripeti  ·  "+dayLabel(),0x12FFFFFF,Ui.TEXT,this::editDays));Ui.space(options,10);
        options.addView(Ui.text(this,"Ultima linea: le chiamate.\nAll'attivazione, suoneria accesa almeno al 60%. Non disturbare e i tuoi preferiti restano come li hai impostati.",13,Ui.LILAC));Ui.space(options,12);
        Switch vibration=new Switch(this);vibration.setText("Vibrazione");vibration.setTextColor(Ui.TEXT);vibration.setTextSize(15);vibration.setMinHeight(Ui.dp(this,52));vibration.setChecked(Store.vibration(this,selected));vibration.setOnCheckedChangeListener((b,on)->Store.prefs(this).edit().putBoolean(Store.key(selected,"vibration"),on).apply());options.addView(vibration);
        options.addView(Ui.button(this,getString(R.string.puzzle_title)+" · "+getResources().getStringArray(R.array.puzzle_types)[plan.puzzleType],0x12FFFFFF,Ui.TEXT,()->requestEdit(6)));Ui.space(options,10);
        options.addView(Ui.button(this,getString(R.string.preview_puzzle),0x12FFFFFF,Ui.LILAC,this::previewPuzzle));Ui.space(options,10);
        Switch escalating=new Switch(this);escalating.setText(R.string.escalating);escalating.setTextColor(Ui.TEXT);escalating.setMinHeight(Ui.dp(this,52));escalating.setChecked(plan.escalating);escalating.setOnClickListener(v->{escalating.setChecked(plan.escalating);requestEdit(7);});options.addView(escalating);
        options.addView(Ui.text(this,getString(R.string.escalating_hint),12,Ui.MUTED));Ui.space(options,12);
        options.addView(Ui.text(this,getString(R.string.puzzle_explanation),13,Ui.MUTED));page.addView(options);Ui.space(page,16);
        boolean permissions=notifications()&&Scheduler.allowed(this)&&fullscreen();
        LinearLayout reliability=Ui.card(this,Ui.CARD);reliability.addView(Ui.bold(this,permissions?"●  Permessi pronti":"○  Completa la configurazione",15,permissions?Ui.LIME:Ui.ORANGE));Ui.space(reliability,10);reliability.addView(Ui.text(this,"Suono: volume Sveglia del telefono.\nSu Samsung, scegli batteria “Senza restrizioni” per Panik Attak.",12,Ui.MUTED));Ui.space(reliability,12);reliability.addView(Ui.button(this,"Controlla impostazioni  ↗",0x12FFFFFF,Ui.TEXT,this::settings));page.addView(reliability);Ui.space(page,16);
        page.addView(Ui.button(this,"ϟ   Prova tra 12 secondi",Ui.LILAC,Ui.INK,()->{if(ready()){Scheduler.test(this);Toast.makeText(this,"Prova avviata: blocca lo schermo. Suona tra 12 secondi.",Toast.LENGTH_LONG).show();}}));Ui.space(page,18);
        TextView footer=Ui.text(this,"FATTA PER CHI DICE “ANCORA UN MINUTO”.",9,Ui.MUTED);footer.setGravity(Gravity.CENTER);footer.setLetterSpacing(.07f);page.addView(footer);
    }
    String timeAt(int offset){return LocalTime.of(plan.hour,plan.minute).plusMinutes(offset).format(DateTimeFormatter.ofPattern("HH:mm"));}
    String dayLabel(){if(plan.days==127)return "Ogni giorno";if(plan.days==31)return "Lun–Ven";String[] d={"Lun","Mar","Mer","Gio","Ven","Sab","Dom"};ArrayList<String> names=new ArrayList<>();for(int i=0;i<7;i++)if((plan.days&(1<<i))!=0)names.add(d[i]);return String.join(" · ",names);}
    void save(){Store.save(this,plan);Store.prefs(this).edit().remove(Store.key(Store.selected(this),"skip")).apply();Scheduler.schedule(this);render();}
    void editTime(){requestEdit(0);}
    void editDays(){requestEdit(1);}
    void editPhase(int phase){requestEdit(phase+2);}
    String cadence(int phase){return plan.fixed[phase]?getString(R.string.at_time,formatMinutes(plan.fixedMinutes[phase])):getString(R.string.every_minutes,plan.intervals[phase]);}
    static String formatMinutes(int minutes){return String.format(Locale.ITALIAN,"%02d:%02d",Math.floorMod(minutes,1440)/60,Math.floorMod(minutes,60));}
    interface TimeChosen {void accept(int minute);}
    void pickTime(int title,int minutes,TimeChosen chosen){
        if(getSupportFragmentManager().isStateSaved())return;
        com.google.android.material.timepicker.MaterialTimePicker picker=new com.google.android.material.timepicker.MaterialTimePicker.Builder()
            .setTimeFormat(com.google.android.material.timepicker.TimeFormat.CLOCK_24H)
            .setInputMode(com.google.android.material.timepicker.MaterialTimePicker.INPUT_MODE_CLOCK)
            .setHour(Math.floorMod(minutes,1440)/60).setMinute(Math.floorMod(minutes,60)).setTitleText(title)
            .setPositiveButtonText(R.string.done).setNegativeButtonText(R.string.cancel).build();
        picker.addOnPositiveButtonClickListener(v->chosen.accept(picker.getHour()*60+picker.getMinute()));
        picker.show(getSupportFragmentManager(),"timePicker");
    }
    void showTimeEditor(){pickTime(R.string.first_alarm,plan.hour*60+plan.minute,m->{
        Plan candidate=Store.plan(this);candidate.hour=m/60;candidate.minute=m%60;
        if(!candidate.valid()){Toast.makeText(this,R.string.invalid_fixed,Toast.LENGTH_LONG).show();return;}
        plan=candidate;save();
    });}
    void showPhaseEditor(int phase){PhaseEditor.show(this,phase);}
    void showPuzzleEditor(){Ui.dialog(this).setTitle(R.string.puzzle_title).setSingleChoiceItems(R.array.puzzle_types,plan.puzzleType,(d,n)->{plan.puzzleType=n;save();d.dismiss();}).setNegativeButton(R.string.cancel,null).show();}
    void previewPuzzle(){Ui.dialog(this).setTitle(R.string.preview_puzzle).setItems(new String[]{getString(R.string.simple),getString(R.string.hard)},(d,n)->startActivity(new Intent(this,ChallengeActivity.class).putExtra("purpose",ChallengeActivity.PREVIEW).putExtra("level",n==0?0:2))).setNegativeButton(R.string.cancel,null).show();}
    void showDaysEditor(){boolean[] chosen=new boolean[7];for(int i=0;i<7;i++)chosen[i]=(plan.days&(1<<i))!=0;AlertDialog d=Ui.dialog(this).setTitle("Quali mattine?").setMultiChoiceItems(new String[]{"Lunedì","Martedì","Mercoledì","Giovedì","Venerdì","Sabato","Domenica"},chosen,(v,n,on)->chosen[n]=on).setNegativeButton("Annulla",null).setPositiveButton("Salva",null).create();d.setOnShowListener(v->{Ui.styleDialog(d);d.getButton(-1).setOnClickListener(b->{int days=0;for(int j=0;j<7;j++)if(chosen[j])days|=1<<j;if(days==0){Toast.makeText(this,"Scegli almeno un giorno",Toast.LENGTH_SHORT).show();return;}plan.days=days;save();d.dismiss();});});d.show();}
    void preview(){StringBuilder s=new StringBuilder();for(int p=0;p<4;p++){s.append(Plan.NAMES[p]).append("\n");int count=0;for(Plan.Event e:plan.events(LocalDate.now(),ZoneId.systemDefault()))if(e.phase==p){if(count>0)s.append("  ·  ");s.append(Store.time(e.at));count++;}s.append("\n\n");}TextView txt=Ui.text(this,s.toString(),15,Ui.TEXT);txt.setPadding(30,20,30,10);ScrollView scroll=new ScrollView(this);scroll.addView(txt);Ui.dialog(this).setTitle("La tua sequenza").setView(scroll).setPositiveButton("Perfetto",null).show();}
    boolean notifications(){return getSystemService(NotificationManager.class).areNotificationsEnabled();}
    boolean fullscreen(){return Build.VERSION.SDK_INT<34||getSystemService(NotificationManager.class).canUseFullScreenIntent();}
    boolean ready(){if(!notifications()){if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},11);else startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName()));Toast.makeText(this,"Consenti le notifiche e poi riprova.",Toast.LENGTH_LONG).show();return false;}if(!Scheduler.allowed(this)){startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getPackageName())));return false;}if(!fullscreen()){startActivity(new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,Uri.parse("package:"+getPackageName())));Toast.makeText(this,"Consenti le sveglie a schermo intero e poi riprova.",Toast.LENGTH_LONG).show();return false;}AudioManager audio=getSystemService(AudioManager.class);if(audio.getStreamVolume(AudioManager.STREAM_ALARM)==0){Ui.dialog(this).setTitle("Il volume sveglia è a zero").setMessage("Alzalo nelle impostazioni audio prima di attivare la sequenza.").setPositiveButton("Apri audio",(d,w)->startActivity(new Intent(Settings.ACTION_SOUND_SETTINGS))).setNegativeButton("Chiudi",null).show();return false;}return true;}
    void settings(){String[] entries={"Notifiche  ·  "+(notifications()?"OK":"Da consentire"),"Sveglie precise  ·  "+(Scheduler.allowed(this)?"OK":"Da consentire"),"Schermo intero  ·  "+(fullscreen()?"OK":"Da consentire"),"Batteria e informazioni app","Volume sveglia"};Ui.dialog(this).setTitle("Pronta anche a schermo spento").setItems(entries,(d,n)->{switch(n){case 0:startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName()));break;case 1:if(Build.VERSION.SDK_INT>=31)startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getPackageName())));break;case 2:if(Build.VERSION.SDK_INT>=34)startActivity(new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,Uri.parse("package:"+getPackageName())));break;case 3:startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())));break;case 4:startActivity(new Intent(Settings.ACTION_SOUND_SETTINGS));}}).setPositiveButton("Chiudi",null).show();}
}

