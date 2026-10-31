package it.panikattak.app;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.function.BooleanSupplier;

/** Run with CAMERA granted. Virtual camera checks flow, never physical LED or accuracy. */
@SuppressWarnings("deprecation")
public final class ObjectCameraSmoke extends Instrumentation {
    private ObjectTestActivity screen;
    private int checks;
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    private Object field(String name){
        Object[] value=new Object[1];
        runOnMainSync(()->{try{var field=ObjectTestActivity.class.getDeclaredField(name);field.setAccessible(true);value[0]=field.get(screen);}catch(Exception e){throw new RuntimeException(e);}});
        return value[0];
    }
    private String text(String name){TextView view=(TextView)field(name);String[] value=new String[1];runOnMainSync(()->value[0]=view.getText().toString());return value[0];}
    private boolean ready(){
        TextView button=(TextView)field("shoot");boolean[] value=new boolean[1];
        runOnMainSync(()->value[0]=button.isEnabled()&&button.getText().toString().equals("Scatta la foto"));return value[0];
    }
    private void await(String label,BooleanSupplier condition){
        long deadline=SystemClock.uptimeMillis()+60000;
        while(SystemClock.uptimeMillis()<deadline){if(condition.getAsBoolean()){check(true,label);return;}SystemClock.sleep(250);}
        throw new AssertionError(label+"; status="+text("status")+"; button="+text("shoot"));
    }
    private void check(boolean value,String label){
        checks++;if(!value)throw new AssertionError(label);
        Bundle progress=new Bundle();progress.putString("stream","PASS: "+label+"\n");sendStatus(0,progress);
    }
    private void click(String name){View button=(View)field(name);runOnMainSync(button::performClick);}
    private String configuredLight(){
        Camera camera=(Camera)field("camera");String[] value=new String[1];
        runOnMainSync(()->value[0]=camera.getParameters().getFlashMode());return value[0];
    }
    private void visiblePreview(){
        TextureView view=(TextureView)field("preview");Bitmap[] image=new Bitmap[1];runOnMainSync(()->image[0]=view.getBitmap(64,64));
        check(image[0]!=null,"preview produces a bitmap");
        int minimum=255,maximum=0;
        for(int y=8;y<56;y++)for(int x=8;x<56;x++){int pixel=image[0].getPixel(x,y);int luminance=(((pixel>>16)&255)+((pixel>>8)&255)+(pixel&255))/3;minimum=Math.min(minimum,luminance);maximum=Math.max(maximum,luminance);}
        image[0].recycle();check(maximum>35&&maximum-minimum>15,"live preview contains a visible scene, not a black rectangle");
    }
    private void screenshot(String name)throws Exception{
        Bitmap bitmap=getUiAutomation().takeScreenshot();
        if(bitmap!=null){try(FileOutputStream output=new FileOutputStream(new File(getTargetContext().getExternalFilesDir(null),name+".png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,output);}finally{bitmap.recycle();}}
    }
    @Override public void onStart(){
        Bundle out=new Bundle();int code=Activity.RESULT_CANCELED;
        var before=new HashMap<>(Store.prefs(getTargetContext()).getAll());
        try{
            screen=(ObjectTestActivity)startActivitySync(new Intent(getTargetContext(),ObjectTestActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            await("camera enables capture only after a frame",this::ready);visiblePreview();screenshot("object-camera-preview");
            ObjectRecognition.Cutlery initialTarget=(ObjectRecognition.Cutlery)field("target");
            check(text("targetPrompt").equals(initialTarget.prompt()),"requested cutlery is shown before capture");
            boolean supported=(Boolean)field("flashAvailable");
            if(supported){
                String mode=configuredLight();check(Camera.Parameters.FLASH_MODE_TORCH.equals(mode)||Camera.Parameters.FLASH_MODE_ON.equals(mode),"light enabled by default where supported");
                click("light");check(Camera.Parameters.FLASH_MODE_OFF.equals(configuredLight()),"light can be switched off");
                click("light");check(!Camera.Parameters.FLASH_MODE_OFF.equals(configuredLight()),"light can be switched back on");
            }else check(text("light").equals("Flash non disponibile"),"camera without flash remains usable");
            click("shoot");await("photo captured and analyzed locally",()->text("shoot").equals("Riprova la foto")||text("shoot").equals("Prossima posata"));
            ImageView photo=(ImageView)field("photo");boolean[] shown=new boolean[1];runOnMainSync(()->shown[0]=photo.getVisibility()==View.VISIBLE&&photo.getDrawable()!=null);
            check(shown[0],"captured photograph is visible");check(field("camera")==null,"camera and illumination released after capture");
            check(text("result").contains("Rilevazioni:"),"TensorFlow Lite returns a recognition result");screenshot("object-camera-result");
            boolean passed=(Boolean)field("challengePassed");
            click("shoot");await("new photo restarts preview",this::ready);check(text("result").isEmpty(),"new capture clears old recognition");visiblePreview();
            check(passed?field("target")!=initialTarget:field("target")==initialTarget,"success draws a new target; retry keeps the requested cutlery");
            runOnMainSync(()->callActivityOnPause(screen));check(field("camera")==null,"background releases camera");
            runOnMainSync(()->callActivityOnResume(screen));await("foreground restores preview",this::ready);
            // Interrupt autofocus before its callback; stale callbacks must not take a photo.
            View shoot=(View)field("shoot");runOnMainSync(()->{shoot.performClick();callActivityOnPause(screen);callActivityOnResume(screen);});
            await("interrupted capture can resume",this::ready);visiblePreview();
            if(supported)click("light");
            Object targetBeforeRotation=field("target");
            ActivityMonitor monitor=addMonitor(ObjectTestActivity.class.getName(),null,false);
            ObjectTestActivity previous=screen;runOnMainSync(()->previous.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));
            Activity replacement=waitForMonitorWithTimeout(monitor,15000);removeMonitor(monitor);
            check(replacement instanceof ObjectTestActivity&&replacement!=previous,"activity recreated");screen=(ObjectTestActivity)replacement;
            await("recreated activity restores camera",this::ready);visiblePreview();
            check(field("target")==targetBeforeRotation,"requested cutlery survives rotation");
            if(supported)check(Camera.Parameters.FLASH_MODE_OFF.equals(configuredLight()),"light preference survives rotation");
            check(before.equals(Store.prefs(getTargetContext()).getAll()),"camera experiment preserves alarm preferences");
            out.putString("stream","PASS: "+checks+" object camera checks. Physical flash and target accuracy require a real phone.\n");code=Activity.RESULT_OK;
        }catch(Throwable error){out.putString("stream","FAIL after "+checks+" checks: "+error+"\n");}
        finally{if(screen!=null)runOnMainSync(screen::finish);}
        finish(code,out);
    }
}
