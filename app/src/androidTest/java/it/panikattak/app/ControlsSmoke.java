package it.panikattak.app;
import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.graphics.Bitmap;
import java.util.*;
import java.io.*;
import com.google.android.material.timepicker.MaterialTimePicker;

/** Device touch regression for direct dragging and shared dialog controls. */
public class ControlsSmoke extends ConfigSmoke {
    void screenshot(String name)throws Exception{SystemClock.sleep(500);Bitmap bitmap=getUiAutomation().takeScreenshot();check(bitmap!=null,"screen captured");try(FileOutputStream out=new FileOutputStream(new File(context.getExternalFilesDir(null),name+".png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}finally{bitmap.recycle();}}
    @Override void capture(View view,String name){SystemClock.sleep(350);super.capture(view,name);}
    @Override public void onStart(){
        context=getTargetContext();var prefs=Store.prefs(context);Map<String,?> before=new HashMap<>(prefs.getAll());Bundle result=new Bundle();int code=Activity.RESULT_CANCELED;MainActivity main=null;ChallengeActivity challenge=null;final AlertDialog[] dialog={null};
        if(prefs.getBoolean("ringing",false)){result.putString("stream","SKIP: real alarm active\n");finish(code,result);return;}
        try{
            for(Store.Routine routine:Store.routines(context))Store.setEnabled(context,routine.id,false);Scheduler.cancel(context);
            main=(MainActivity)startActivitySync(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));final MainActivity a=main;waitForIdleSync();
            runOnMainSync(()->dialog[0]=Ui.dialog(a).setTitle("Quali mattine?").setMultiChoiceItems(new String[]{"Lunedì","Martedì","Mercoledì","Giovedì","Venerdì","Sabato","Domenica"},new boolean[]{true,true,true,true,true,false,false},(d,n,on)->{}).setPositiveButton("Salva",null).setNegativeButton("Annulla",null).show());waitForIdleSync();
            check(dialog[0].getButton(-1).getCurrentTextColor()==Ui.INK,"primary button has dark text");
            runOnMainSync(()->{var button=dialog[0].getButton(-1);Bitmap rendered=Bitmap.createBitmap(button.getWidth(),button.getHeight(),Bitmap.Config.ARGB_8888);button.draw(new android.graphics.Canvas(rendered));check(rendered.getPixel(button.getWidth()/2,Ui.dp(context,9))==Ui.LIME,"primary background visibly filled");rendered.recycle();});check(dialog[0].getButton(-1).getHeight()>=Ui.dp(context,48),"confirm touch target");
            CheckedTextView option=(CheckedTextView)dialog[0].getListView().getChildAt(0);check(option.getCheckMarkTintList().getColorForState(new int[]{android.R.attr.state_enabled,android.R.attr.state_checked},0)==Ui.LIME,"checkbox accent");
            capture(dialog[0].getWindow().getDecorView(),"controls-days");screenshot("controls-days-screen");runOnMainSync(()->dialog[0].dismiss());
            runOnMainSync(()->dialog[0]=Ui.dialog(a).setTitle(R.string.puzzle_title).setSingleChoiceItems(R.array.puzzle_types,3,(d,n)->{}).setNegativeButton(R.string.cancel,null).show());waitForIdleSync();
            CheckedTextView radio=(CheckedTextView)dialog[0].getListView().getChildAt(3);check(radio.isChecked()&&radio.getCheckMarkTintList().getColorForState(new int[]{android.R.attr.state_enabled,android.R.attr.state_checked},0)==Ui.LIME,"radio accent");capture(dialog[0].getWindow().getDecorView(),"controls-puzzles");runOnMainSync(()->dialog[0].dismiss());
            runOnMainSync(()->dialog[0]=Ui.dialog(a).setTitle("La mia routine").setItems(new String[]{"Rinomina","Duplica","Elimina routine"},(d,n)->{}).setNegativeButton(R.string.cancel,null).show());waitForIdleSync();
            check(dialog[0].getListView().getDividerHeight()==Ui.dp(context,1),"action separators");check(dialog[0].getListView().getChildAt(0).getHeight()>=Ui.dp(context,52),"action touch target");capture(dialog[0].getWindow().getDecorView(),"controls-actions");runOnMainSync(()->dialog[0].dismiss());
            runOnMainSync(()->dialog[0]=PhaseEditor.show(a,0));waitForIdleSync();check(dialog[0].getButton(-1).getCurrentTextColor()==Ui.INK,"phase editor uses shared controls");capture(dialog[0].getWindow().getDecorView(),"controls-phase");runOnMainSync(()->dialog[0].dismiss());
            runOnMainSync(()->a.pickTime(R.string.first_alarm,480,m->{}));waitForIdleSync();MaterialTimePicker picker=(MaterialTimePicker)a.getSupportFragmentManager().findFragmentByTag("timePicker");
            var confirm=(com.google.android.material.button.MaterialButton)picker.getDialog().findViewById(com.google.android.material.R.id.material_timepicker_ok_button);
            check(confirm.getCurrentTextColor()==Ui.INK&&confirm.getBackground() instanceof android.graphics.drawable.RippleDrawable,"time picker confirm style");capture(picker.getDialog().getWindow().getDecorView(),"controls-time");runOnMainSync(picker::dismissAllowingStateLoss);waitForIdleSync();
            runOnMainSync(()->RoutineUi.manage(a));waitForIdleSync();screenshot("controls-real-manage");sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);waitForIdleSync();
            Plan plan=Store.plan(context);plan.puzzleType=Puzzle.ORDER;Store.save(context,plan);
            challenge=(ChallengeActivity)startActivitySync(ChallengeActivity.intent(context,ChallengeActivity.PREVIEW).putExtra("level",2).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();SystemClock.sleep(300);PuzzleView view=challenge.puzzleView;
            int[] original=view.order.clone();TextView first=view.orderCells.get(0),last=view.orderCells.get(4);int[] xy=new int[2],target=new int[2];runOnMainSync(()->{first.getLocationOnScreen(xy);last.getLocationOnScreen(target);});
            float x=xy[0]+first.getWidth()/2f,y=xy[1]+first.getHeight()/2f,tx=target[0]+last.getWidth()/2f;long down=SystemClock.uptimeMillis();touch(down,MotionEvent.ACTION_DOWN,x,y);
            for(int i=1;i<=8;i++){touch(down,MotionEvent.ACTION_MOVE,x+(tx-x)*i/8,y);SystemClock.sleep(18);}
            check(first.getTranslationX()>first.getWidth(),"tile follows finger without long press");check(Arrays.equals(original,view.order),"order unchanged before release");screenshot("controls-drag-moving");
            touch(down,MotionEvent.ACTION_UP,tx,y);SystemClock.sleep(300);waitForIdleSync();check(view.order[4]==original[0]&&view.order[0]==original[4],"release swaps intended tiles");check(view.order[1]==original[1]&&view.order[2]==original[2]&&view.order[3]==original[3],"other tiles unchanged");check(!view.settling&&view.orderCells.get(0).getTranslationX()==0,"swap animation settles");
            int left=view.order[0],right=view.order[1];runOnMainSync(()->view.orderCells.get(0).performClick());check(view.selected==0,"tap selection retained");runOnMainSync(()->view.orderCells.get(1).performClick());check(view.settling,"tap swap animated");SystemClock.sleep(300);waitForIdleSync();check(view.order[0]==right&&view.order[1]==left,"tap alternative works");
            TextView cancelTile=view.orderCells.get(0);runOnMainSync(()->cancelTile.getLocationOnScreen(xy));x=xy[0]+cancelTile.getWidth()/2f;y=xy[1]+cancelTile.getHeight()/2f;int[] beforeCancel=view.order.clone();down=SystemClock.uptimeMillis();touch(down,MotionEvent.ACTION_DOWN,x,y);touch(down,MotionEvent.ACTION_MOVE,x+cancelTile.getWidth(),y);touch(down,MotionEvent.ACTION_CANCEL,x+cancelTile.getWidth(),y);SystemClock.sleep(300);waitForIdleSync();check(Arrays.equals(beforeCancel,view.order)&&view.dragFrom==-1,"cancel gesture preserves order");
            runOnMainSync(()->view.submit("wrong"));check(!view.complete&&Arrays.equals(view.order,view.puzzle.data),"wrong answer still resets puzzle");
            capture(challenge,"controls-order");result.putString("stream","PASS: "+checks+" controls and direct-drag checks.\n");code=Activity.RESULT_OK;
        }catch(Throwable e){result.putString("stream","FAIL after "+checks+": "+e+"\n");}
        finally{if(dialog[0]!=null)runOnMainSync(()->dialog[0].dismiss());if(challenge!=null){final Activity c=challenge;runOnMainSync(c::finish);}if(main!=null){final Activity a=main;runOnMainSync(a::finish);}waitForIdleSync();restore(before);}
        finish(code,result);
    }
}
