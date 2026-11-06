package it.panikattak.app;

import android.app.*;
import android.content.*;
import android.content.res.Configuration;
import android.os.*;
import android.view.*;
import android.widget.*;

/** Run at phone, tablet portrait and tablet landscape window sizes. No stored settings changed. */
public class TabletLayoutSmoke extends Instrumentation {
    @Override public void onCreate(Bundle arguments){super.onCreate(arguments);start();}
    void main(Runnable action){
        Throwable[] failure={null};super.runOnMainSync(()->{try{action.run();}catch(Throwable e){failure[0]=e;}});
        if(failure[0]!=null)throw new AssertionError(failure[0]);
    }
    void capture(){
        View view=screen.getWindow().getDecorView();
        android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(view.getWidth(),view.getHeight(),android.graphics.Bitmap.Config.ARGB_8888);
        view.draw(new android.graphics.Canvas(bitmap));
        java.io.File file=new java.io.File(screen.getExternalFilesDir(null),"tablet-"+screen.destination+"-"+view.getWidth()+"x"+view.getHeight()+".png");
        try(java.io.FileOutputStream output=new java.io.FileOutputStream(file)){bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,output);}catch(java.io.IOException e){throw new RuntimeException(e);}finally{bitmap.recycle();}
    }
    int checks;
    MainActivity screen;
    void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    void settle(){waitForIdleSync();SystemClock.sleep(400);waitForIdleSync();}
    void inspect(View view){
        if(view.getVisibility()!=View.VISIBLE)return;
        if(view instanceof TextView&&view.isClickable()){
            TextView t=(TextView)view;
            if(t.getText().length()>0){
                float minimum=android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP,18,t.getResources().getDisplayMetrics());
                check(t.getTextSize()>=minimum-0.1f,"readable button: "+t.getText()+" ("+t.getTextSize()+"px; minimum "+minimum+"px)");
                if(t.getLayout()!=null){
                    check(t.getLayout().getHeight()<=t.getHeight()-t.getCompoundPaddingTop()-t.getCompoundPaddingBottom(),"button text fits vertically: "+t.getText());
                    for(int i=0;i<t.getLayout().getLineCount();i++)check(t.getLayout().getEllipsisCount(i)==0,"button text is complete: "+t.getText());
                }
            }
        }
        if(view instanceof ViewGroup){ViewGroup g=(ViewGroup)view;for(int i=0;i<g.getChildCount();i++)inspect(g.getChildAt(i));}
    }
    void page(int destination){main(()->screen.navigate(destination));settle();layout();}
    void layout(){main(()->{
        check(screen.shell.body.getWidth()<=Ui.dp(screen,760),"bounded content width");
        check(screen.shell.scroll.getHeight()>0&&screen.shell.navigation.getHeight()>0,"content and navigation visible");
        int[] body=new int[2];screen.shell.body.getLocationOnScreen(body);
        int[] scroll=new int[2];screen.shell.scroll.getLocationOnScreen(scroll);
        check(Math.abs((body[0]+screen.shell.body.getWidth()/2)-(scroll[0]+screen.shell.scroll.getWidth()/2))<=2,"content centered");
        capture();inspect(screen.getWindow().getDecorView());
    });}
    @Override public void onStart(){
        Bundle result=new Bundle();int code=Activity.RESULT_CANCELED;
        try{
            Context c=getTargetContext();
            for(int width:new int[]{360,600,924}){
                Configuration config=new Configuration(c.getResources().getConfiguration());config.smallestScreenWidthDp=width;
                Context window=c.createConfigurationContext(config);
                check(Ui.scale(window)==(width==360?1f:width==600?1.2f:1.35f),"window size selects proportions");
                check(width==360?Ui.sp(window,15)==15:Ui.sp(window,15)>=18,"phone unchanged and tablet text readable");
            }
            check(Ui.scale(c)>1,"run on a tablet-sized window");
            screen=(MainActivity)startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));settle();
            page(MainActivity.TODAY);page(MainActivity.ROUTINES);page(MainActivity.HABITS);
            main(screen::openSettings);settle();layout();
            main(()->screen.openRoutine(Store.DEFAULT));settle();layout();
            main(()->{
                for(int kind=0;kind<=Puzzle.CAMERA;kind++){
                    PuzzleView puzzle=new PuzzleView(screen,Puzzle.create(kind,0,null),answer->{});
                    int width=screen.shell.body.getWidth()-screen.shell.body.getPaddingLeft()-screen.shell.body.getPaddingRight();
                    puzzle.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
                    puzzle.layout(0,0,width,puzzle.getMeasuredHeight());inspect(puzzle);
                }
            });
            result.putString("stream","PASS: "+checks+" tablet layout checks.\n");code=Activity.RESULT_OK;
        }catch(Throwable e){result.putString("stream","FAIL after "+checks+" checks: "+e+"\n");}
        finally{if(screen!=null)main(screen::finish);}
        finish(code,result);
    }
}
