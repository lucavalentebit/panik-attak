package it.panikattak.app;
import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
/** The common gate uses the selected type at maximum difficulty. */
public class ChallengeActivity extends Activity {
    static final String PAUSE="pause", MORNING="morning", EDIT="edit", PREVIEW="preview";
    Puzzle challenge; PuzzleView puzzleView;
    String answer="", purpose, routine; long sequenceStart; boolean test;
    static Intent intent(Context c,String purpose){return intent(c,purpose,MORNING.equals(purpose)?Store.activeRoutine(c):Store.selected(c));}
    static Intent intent(Context c,String purpose,String routine){return new Intent(c,ChallengeActivity.class).putExtra("routine",routine).putExtra("purpose",purpose).putExtra("start",Store.prefs(c).getLong("activeStart",0)).putExtra("test",Store.prefs(c).getBoolean("activeTest",false));}
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);purpose=getIntent().getStringExtra("purpose");
        if(!PAUSE.equals(purpose)&&!MORNING.equals(purpose)&&!EDIT.equals(purpose)&&!PREVIEW.equals(purpose)){finish();return;}
        routine=getIntent().getStringExtra("routine");if(routine==null)routine=MORNING.equals(purpose)?Store.activeRoutine(this):Store.selected(this);if(!Store.exists(this,routine)){finish();return;}
        sequenceStart=getIntent().getLongExtra("start",0);test=getIntent().getBooleanExtra("test",false);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);if(Build.VERSION.SDK_INT>=27)setShowWhenLocked(true);
        int type=MORNING.equals(purpose)?Store.activePuzzle(this).type:Store.plan(this,routine).puzzleType;
        challenge=saved==null?Store.newPuzzle(this,type,PREVIEW.equals(purpose)?getIntent().getIntExtra("level",0):2):Puzzle.decode(saved.getString("puzzle"));
        LinearLayout page=Ui.shell(this,true);page.addView(Ui.bold(this,"ϟ  PANIK ATTAK",13,Ui.TEXT));Ui.space(page,24);
        page.addView(Ui.bold(this,getString(PREVIEW.equals(purpose)?R.string.preview:R.string.gate_heading),13,Ui.LILAC));Ui.space(page,12);
        page.addView(Ui.bold(this,Store.name(this,routine),20,Ui.LIME));Ui.space(page,12);
        if(!PREVIEW.equals(purpose)){
            page.addView(Ui.bold(this,getString(PAUSE.equals(purpose)?R.string.pause_title:EDIT.equals(purpose)?R.string.edit_title:R.string.morning_title),28,Ui.TEXT));Ui.space(page,12);
            page.addView(Ui.text(this,getString(PAUSE.equals(purpose)?R.string.pause_hint:EDIT.equals(purpose)?R.string.edit_hint:R.string.morning_hint),14,Ui.MUTED));Ui.space(page,22);
        }
        puzzleView=new PuzzleView(this,challenge,this::apply);puzzleView.onWrong=()->HabitHistory.gate(this,routine,sequenceStart,purpose,"wrong",test);page.addView(puzzleView);HabitHistory.gate(this,routine,sequenceStart,purpose,"displayed",test);Ui.space(page,14);
        page.addView(Ui.button(this,getString(PREVIEW.equals(purpose)?R.string.cancel:R.string.cancel_gate),Ui.CARD,Ui.MUTED,this::finish));
    }
    void apply(String value){
        if(!challenge.accepts(value)||!Store.exists(this,routine))return;
        HabitHistory.gate(this,routine,sequenceStart,purpose,"solved",test);
        boolean applied=true;
        if(PAUSE.equals(purpose))applied=Scheduler.pause(this,routine,challenge,value);
        else if(MORNING.equals(purpose))applied=Scheduler.dismissMorning(this,routine,sequenceStart,test,challenge,value);
        if(applied){if(PREVIEW.equals(purpose))Toast.makeText(this,R.string.correct,Toast.LENGTH_SHORT).show();setResult(RESULT_OK,new Intent().putExtra("routine",routine));finish();}
        else {Toast.makeText(this,R.string.stale_alarm,Toast.LENGTH_LONG).show();finish();}
    }
    void key(String key){puzzleView.answer=answer;puzzleView.key(key);answer=puzzleView.answer;}
    @Override protected void onSaveInstanceState(Bundle out){if(challenge!=null)out.putString("puzzle",challenge.encode());super.onSaveInstanceState(out);}
    @Override protected void onPause(){if(puzzleView!=null)puzzleView.suspend();super.onPause();}
    @Override protected void onResume(){super.onResume();if(puzzleView!=null)puzzleView.resume();}
}
