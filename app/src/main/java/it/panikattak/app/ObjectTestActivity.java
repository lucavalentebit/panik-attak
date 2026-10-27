package it.panikattak.app;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.ByteArrayInputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Isolated experiment: camera photos never enter the alarm or puzzle state. */
@SuppressWarnings("deprecation")
public final class ObjectTestActivity extends Activity implements SurfaceHolder.Callback {
    private static final int CAMERA_REQUEST = 31;
    private SurfaceView preview;
    private ImageView photo;
    private TextView status, result, shoot;
    private Camera camera;
    private boolean surfaceReady, busy;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private ObjectDetector detector;

    @Override public void onCreate(Bundle state){super.onCreate(state);
        getWindow().setStatusBarColor(Ui.BG);getWindow().setNavigationBarColor(Ui.BG);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(Ui.BG);
        LinearLayout page=Ui.column(this);page.setPadding(Ui.dp(this,22),Ui.dp(this,24),Ui.dp(this,22),Ui.dp(this,30));
        scroll.addView(page);setContentView(scroll);
        TextView back=Ui.button(this,"←  Indietro",Ui.CARD,Ui.TEXT,this::finish);page.addView(back);Ui.space(page,20);
        page.addView(Ui.bold(this,"Riconosci un oggetto",25,Ui.TEXT));Ui.space(page,10);
        page.addView(Ui.text(this,"Inquadra posate, una tazza o una sedia. Scatta una foto e guarda cosa riconosce il modello sul telefono.",14,Ui.MUTED));Ui.space(page,18);
        preview=new SurfaceView(this);preview.getHolder().addCallback(this);
        preview.setBackgroundColor(Color.BLACK);
        page.addView(preview,new LinearLayout.LayoutParams(-1,Ui.dp(this,260)));
        photo=new ImageView(this);photo.setAdjustViewBounds(true);photo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);photo.setVisibility(View.GONE);
        page.addView(photo,new LinearLayout.LayoutParams(-1,Ui.dp(this,260)));
        Ui.space(page,12);shoot=Ui.button(this,"Scatta la foto",Ui.LIME,Ui.INK,this::capture);page.addView(shoot);
        Ui.space(page,12);status=Ui.text(this,"",13,Ui.MUTED);page.addView(status);
        Ui.space(page,12);result=Ui.bold(this,"",16,Ui.TEXT);page.addView(result);
        Ui.space(page,20);page.addView(Ui.text(this,"Prova sperimentale: il modello può sbagliare. Per una sedia riconosce la categoria, ma non verifica il numero delle gambe. Le foto non vengono inviate online né conservate nello storico.",12,Ui.MUTED));
    }
    @Override protected void onResume(){super.onResume();startCamera();}
    @Override protected void onPause(){releaseCamera();super.onPause();}
    @Override protected void onDestroy(){worker.execute(()->{if(detector!=null)detector.close();});worker.shutdown();super.onDestroy();}
    @Override public void surfaceCreated(SurfaceHolder holder){surfaceReady=true;startCamera();}
    @Override public void surfaceChanged(SurfaceHolder holder,int format,int width,int height){}
    @Override public void surfaceDestroyed(SurfaceHolder holder){surfaceReady=false;releaseCamera();}
    private void startCamera(){
        if(!surfaceReady||camera!=null||preview==null)return;
        if(checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.CAMERA},CAMERA_REQUEST);return;}
        try{
            camera=Camera.open();Camera.Parameters parameters=camera.getParameters();
            parameters.setFocusMode(parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)?Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE:parameters.getFocusMode());
            camera.setParameters(parameters);
            Camera.CameraInfo info=new Camera.CameraInfo();Camera.getCameraInfo(0,info);
            int display=getWindowManager().getDefaultDisplay().getRotation()*90;
            camera.setDisplayOrientation((info.orientation-display+360)%360);
            Camera.Parameters jpeg=camera.getParameters();jpeg.setRotation((info.orientation+display)%360);camera.setParameters(jpeg);
            camera.setPreviewDisplay(preview.getHolder());camera.startPreview();status.setText("Fotocamera pronta.");
        }catch(Exception error){releaseCamera();status.setText("Fotocamera non disponibile su questo dispositivo.");shoot.setEnabled(false);}
    }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);
        if(request!=CAMERA_REQUEST)return;
        if(results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED)startCamera();
        else status.setText("Consenti l’accesso alla fotocamera per fare la prova.");
    }
    private void releaseCamera(){if(camera!=null){camera.setPreviewCallback(null);camera.release();camera=null;}}
    private void capture(){
        if(photo.getVisibility()==View.VISIBLE){photo.setImageDrawable(null);photo.setVisibility(View.GONE);preview.setVisibility(View.VISIBLE);shoot.setText("Scatta la foto");status.setText("Prepara il prossimo scatto.");startCamera();return;}
        if(camera==null||busy){status.setText("Attendi che la fotocamera sia pronta.");return;}
        busy=true;shoot.setEnabled(false);status.setText("Analisi della foto in corso…");result.setText("");
        try{camera.takePicture(null,null,(bytes,c)->analyze(bytes));}
        catch(RuntimeException error){busy=false;shoot.setEnabled(true);status.setText("Scatto non riuscito. Riprova.");}
    }
    private void analyze(byte[] bytes){
        BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=4;
        Bitmap bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);
        if(bitmap==null){finishAnalysis("Impossibile leggere la foto.");return;}
        try{
            int orientation=new ExifInterface(new ByteArrayInputStream(bytes)).getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL);
            int degrees=orientation==ExifInterface.ORIENTATION_ROTATE_90?90:orientation==ExifInterface.ORIENTATION_ROTATE_180?180:orientation==ExifInterface.ORIENTATION_ROTATE_270?270:0;
            if(degrees!=0){Matrix matrix=new Matrix();matrix.postRotate(degrees);Bitmap rotated=Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);bitmap.recycle();bitmap=rotated;}
        }catch(Exception ignored){}
        photo.setImageBitmap(bitmap);photo.setVisibility(View.VISIBLE);preview.setVisibility(View.GONE);
        Bitmap photograph=bitmap;
        worker.execute(()->{
            String outcome;
            try{if(detector==null)detector=new ObjectDetector(this);outcome=detector.detect(photograph);}
            catch(Exception error){outcome="Riconoscimento non riuscito. Riprova.";}
            String message=outcome;
            runOnUiThread(()->{if(!isDestroyed())finishAnalysis(message);});
        });
    }
    private void finishAnalysis(String message){busy=false;shoot.setEnabled(true);status.setText("Foto analizzata sul dispositivo.");result.setText(message);releaseCamera();
        if(photo.getVisibility()==View.VISIBLE)shoot.setText("Nuova foto");else startCamera();
    }
}
