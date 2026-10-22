package it.panikattak.app;
import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
public class AlarmActivity extends Activity {
    Puzzle puzzle;PuzzleView puzzleView;TextView waitingLabel;long activeAt=-1;String solvedAnswer,routine;
    final BroadcastReceiver updates=new BroadcastReceiver(){public void onReceive(Context c,Intent i){refresh();}};
    @android.annotation.SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        if(Build.VERSION.SDK_INT>=27){setShowWhenLocked(true);setTurnScreenOn(true);}
        if(Build.VERSION.SDK_INT>=33)registerReceiver(updates,new IntentFilter("it.panikattak.REFRESH"),Context.RECEIVER_NOT_EXPORTED);else registerReceiver(updates,new IntentFilter("it.panikattak.REFRESH"));
        refresh();
    }
    void refresh(){
        var s=Store.prefs(this);if(!s.getBoolean("ringing",false)){finish();return;}
        updateWaiting();long at=s.getLong("activeAt",0);Puzzle current=Store.activePuzzle(this);
        if(Store.activeRoutine(this).equals(routine)&&at==activeAt&&puzzle!=null&&puzzle.encode().equals(current.encode()))return;
        routine=Store.activeRoutine(this);activeAt=at;puzzle=current;solvedAnswer=null;render();
    }
    @Override public void onResume(){super.onResume();refresh();if(puzzleView!=null)puzzleView.resume();}
    @Override protected void onPause(){if(puzzleView!=null)puzzleView.suspend();super.onPause();}
    @Override protected void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);refresh();}
    @Override protected void onDestroy(){unregisterReceiver(updates);super.onDestroy();}
    @Override public void onBackPressed(){Toast.makeText(this,R.string.alarm_hint,Toast.LENGTH_SHORT).show();}
    void updateWaiting(){if(waitingLabel!=null){int waiting=AlarmQueue.read(this).size()-1;waitingLabel.setText(getString(R.string.routines_waiting,Math.max(0,waiting)));waitingLabel.setVisibility(waiting>0?View.VISIBLE:View.GONE);}}
    void render(){
        int phase=Store.prefs(this).getInt("activePhase",0);int accent=Ui.COLORS[phase];
        LinearLayout page=Ui.shell(this,true);page.addView(Ui.bold(this,"ϟ  PANIK ATTAK",13,Ui.TEXT));Ui.space(page,20);
        page.addView(Ui.bold(this,Store.name(this,routine),22,Ui.LIME));waitingLabel=Ui.text(this,"",13,Ui.MUTED);page.addView(waitingLabel);updateWaiting();Ui.space(page,12);
        TextView badge=Ui.bold(this,Plan.NAMES[phase],15,accent);badge.setGravity(Gravity.CENTER);page.addView(badge);Ui.space(page,12);
        TextView clock=Ui.text(this,Store.time(activeAt),58,Ui.TEXT);clock.setGravity(Gravity.CENTER);page.addView(clock);Ui.space(page,12);
        page.addView(Ui.text(this,getString(R.string.alarm_hint),14,Ui.MUTED));Ui.space(page,18);
        TextView dismiss=Ui.button(this,getString(R.string.dismiss_current),Ui.LIME,Ui.INK,()->{Scheduler.dismissCurrent(this,routine,activeAt,puzzle,solvedAnswer);refresh();});dismiss.setVisibility(View.GONE);
        puzzleView=new PuzzleView(this,puzzle,answer->{solvedAnswer=answer;dismiss.setVisibility(View.VISIBLE);});page.addView(puzzleView);Ui.space(page,16);page.addView(dismiss);Ui.space(page,10);
        page.addView(Ui.button(this,getString(R.string.stop_morning),Ui.CARD,Ui.LILAC,()->startActivity(ChallengeActivity.intent(this,ChallengeActivity.MORNING))));
    }
}
