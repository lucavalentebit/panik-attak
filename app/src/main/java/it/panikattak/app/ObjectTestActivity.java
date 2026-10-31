package it.panikattak.app;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Isolated experiment: camera photos never enter the alarm or puzzle state. */
@SuppressWarnings("deprecation")
public final class ObjectTestActivity extends Activity implements TextureView.SurfaceTextureListener {
    private static final int CAMERA_REQUEST=31;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Random random=new Random();
    private TextureView preview;
    private ImageView photo;
    private TextView status,result,shoot,light,targetPrompt;
    private ObjectRecognition.Cutlery target;
    private boolean challengePassed;
    private Camera camera;
    private ObjectDetector detector;
    private boolean resumed,busy,analyzing,previewReady,permissionPending,lightEnabled=true;
    private boolean flashAvailable;
    private String lightMode;
    private int previewWidth,previewHeight,previewRotation,captureId;
    private Runnable previewTimeout,focusTimeout,captureTimeout;

    @Override public void onCreate(Bundle state){super.onCreate(state);
        if(state!=null)lightEnabled=state.getBoolean("lightEnabled",true);
        target=ObjectRecognition.restoreTarget(state==null?null:state.getString("cutleryTarget"),random);
        LinearLayout page=Ui.shell(this,true);
        page.addView(Ui.button(this,"←  Indietro",Ui.CARD,Ui.TEXT,this::finish));Ui.space(page,20);
        page.addView(Ui.bold(this,"Trova la posata",25,Ui.TEXT));Ui.space(page,10);
        page.addView(Ui.text(this,"Una posata a caso tra forchetta, cucchiaio e coltello. Fotografa quella richiesta per superare la prova.",14,Ui.MUTED));Ui.space(page,14);
        targetPrompt=Ui.bold(this,target.prompt(),21,Ui.LIME);page.addView(targetPrompt);Ui.space(page,8);
        page.addView(Ui.text(this,"Inquadra una sola posata, intera e ben visibile.",13,Ui.MUTED));Ui.space(page,18);
        FrameLayout frame=new FrameLayout(this);frame.setBackgroundColor(Ui.CARD);
        page.addView(frame,new LinearLayout.LayoutParams(-1,Ui.dp(this,260)));
        // A TextureView participates in normal view composition. An opaque background
        // on the old SurfaceView could cover the camera surface with a black rectangle.
        preview=new TextureView(this);preview.setContentDescription("Anteprima della fotocamera");
        preview.setSurfaceTextureListener(this);frame.addView(preview,new FrameLayout.LayoutParams(-1,-1));
        photo=new ImageView(this);photo.setScaleType(ImageView.ScaleType.FIT_CENTER);photo.setBackgroundColor(Ui.CARD);
        photo.setContentDescription("Foto scattata");photo.setVisibility(View.GONE);
        frame.addView(photo,new FrameLayout.LayoutParams(-1,-1));
        Ui.space(page,12);light=Ui.button(this,"Preparazione della luce…",Ui.CARD,Ui.TEXT,this::toggleLight);light.setEnabled(false);page.addView(light);
        Ui.space(page,12);shoot=Ui.button(this,"Avvio fotocamera…",Ui.LIME,Ui.INK,this::capture);shoot.setEnabled(false);page.addView(shoot);
        Ui.space(page,12);status=Ui.text(this,"Avvio dell’anteprima…",13,Ui.MUTED);page.addView(status);
        Ui.space(page,12);result=Ui.bold(this,"",16,Ui.TEXT);page.addView(result);
        Ui.space(page,20);page.addView(Ui.text(this,"Prova sperimentale: il modello può sbagliare. Questa prova non modifica le sveglie. Le foto vengono analizzate sul telefono e non vengono salvate nello storico.",12,Ui.MUTED));
    }
    @Override protected void onSaveInstanceState(Bundle state){state.putBoolean("lightEnabled",lightEnabled);state.putString("cutleryTarget",target.name());super.onSaveInstanceState(state);}
    @Override protected void onResume(){super.onResume();resumed=true;startCamera();}
    @Override protected void onPause(){resumed=false;releaseCamera();if(!analyzing)busy=false;super.onPause();}
    @Override protected void onDestroy(){main.removeCallbacksAndMessages(null);worker.execute(()->{if(detector!=null)detector.close();});worker.shutdown();super.onDestroy();}
    @Override public void onSurfaceTextureAvailable(SurfaceTexture texture,int width,int height){startCamera();}
    @Override public void onSurfaceTextureSizeChanged(SurfaceTexture texture,int width,int height){fitPreview();}
    @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture texture){releaseCamera();if(!analyzing)busy=false;return true;}
    @Override public void onSurfaceTextureUpdated(SurfaceTexture texture){
        if(camera!=null&&resumed&&!busy&&!previewReady){
            if(previewTimeout!=null)main.removeCallbacks(previewTimeout);
            previewReady=true;shoot.setText("Scatta la foto");shoot.setEnabled(true);
            status.setText("Fotocamera pronta. Inquadra la posata richiesta.");updateLight();
        }
    }

    private void startCamera(){
        if(!resumed||busy||camera!=null||!preview.isAvailable()||photo.getVisibility()==View.VISIBLE)return;
        if(checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){
            shoot.setText("Consenti fotocamera");shoot.setEnabled(true);
            if(!permissionPending){permissionPending=true;requestPermissions(new String[]{Manifest.permission.CAMERA},CAMERA_REQUEST);}return;
        }
        shoot.setText("Avvio fotocamera…");shoot.setEnabled(false);status.setText("Avvio dell’anteprima…");
        try{
            int cameraId=0;Camera.CameraInfo info=new Camera.CameraInfo();
            for(int id=0;id<Camera.getNumberOfCameras();id++){Camera.getCameraInfo(id,info);if(info.facing==Camera.CameraInfo.CAMERA_FACING_BACK){cameraId=id;break;}}
            Camera.getCameraInfo(cameraId,info);camera=Camera.open(cameraId);
            Camera.Parameters parameters=camera.getParameters();
            List<String> modes=parameters.getSupportedFocusModes();
            if(modes!=null&&modes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            else if(modes!=null&&modes.contains(Camera.Parameters.FOCUS_MODE_AUTO))parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            Camera.Size picture=chooseSize(parameters.getSupportedPictureSizes(),4d/3,1600*1200);
            if(picture!=null)parameters.setPictureSize(picture.width,picture.height);
            else picture=parameters.getPictureSize();
            Camera.Size size=chooseSize(parameters.getSupportedPreviewSizes(),(double)picture.width/picture.height,1280*960);
            if(size!=null)parameters.setPreviewSize(size.width,size.height);
            else size=parameters.getPreviewSize();
            int display=getWindowManager().getDefaultDisplay().getRotation()*90;
            boolean front=info.facing==Camera.CameraInfo.CAMERA_FACING_FRONT;
            previewRotation=front?(360-(info.orientation+display)%360)%360:(info.orientation-display+360)%360;
            parameters.setRotation(front?(info.orientation+display)%360:(info.orientation-display+360)%360);
            parameters.setJpegQuality(95);camera.setParameters(parameters);
            previewWidth=size.width;previewHeight=size.height;fitPreview();
            camera.setDisplayOrientation(previewRotation);configureLight();
            camera.setErrorCallback((error,source)->{if(source==camera)cameraFailure("Fotocamera interrotta. Tocca Riprova fotocamera.");});
            camera.setPreviewTexture(preview.getSurfaceTexture());camera.startPreview();
            Camera opened=camera;previewTimeout=()->{if(camera==opened&&!previewReady)cameraFailure("L’anteprima non risponde. Tocca Riprova fotocamera.");};
            main.postDelayed(previewTimeout,10000);
        }catch(Exception error){cameraFailure("Fotocamera non disponibile. Controlla i permessi e riprova.");}
    }
    private static Camera.Size chooseSize(List<Camera.Size> sizes,double aspect,int targetArea){
        if(sizes==null)return null;
        Camera.Size best=null;double bestCost=Double.MAX_VALUE;
        for(Camera.Size size:sizes){
            double cost=20*Math.abs((double)size.width/size.height-aspect)+Math.abs(Math.log((double)size.width*size.height/targetArea));
            if(cost<bestCost){best=size;bestCost=cost;}
        }return best;
    }
    private void fitPreview(){
        if(previewWidth==0||preview.getWidth()==0||preview.getHeight()==0)return;
        boolean swap=previewRotation%180!=0;
        float width=swap?previewHeight:previewWidth,height=swap?previewWidth:previewHeight;
        float scale=Math.min(preview.getWidth()/width,preview.getHeight()/height);
        Matrix matrix=new Matrix();matrix.setScale(width*scale/preview.getWidth(),height*scale/preview.getHeight(),preview.getWidth()/2f,preview.getHeight()/2f);
        preview.setTransform(matrix);
    }
    private void configureLight(){
        Camera.Parameters parameters=camera.getParameters();List<String> modes=parameters.getSupportedFlashModes();
        flashAvailable=modes!=null&&(modes.contains(Camera.Parameters.FLASH_MODE_TORCH)||modes.contains(Camera.Parameters.FLASH_MODE_ON));
        lightMode=null;
        // Prefer continuous light so the user and autofocus see the illuminated object.
        String[] candidates=lightEnabled?new String[]{Camera.Parameters.FLASH_MODE_TORCH,Camera.Parameters.FLASH_MODE_ON,Camera.Parameters.FLASH_MODE_OFF}:new String[]{Camera.Parameters.FLASH_MODE_OFF};
        if(modes!=null)for(String mode:candidates){
            if(!modes.contains(mode))continue;
            try{parameters=camera.getParameters();parameters.setFlashMode(mode);camera.setParameters(parameters);lightMode=mode;break;}
            catch(RuntimeException ignored){/* Some devices advertise modes they reject. */}
        }
        if(lightEnabled&&(lightMode==null||Camera.Parameters.FLASH_MODE_OFF.equals(lightMode)))flashAvailable=false;
        if(!lightEnabled&&flashAvailable&&!Camera.Parameters.FLASH_MODE_OFF.equals(lightMode))throw new IllegalStateException("Cannot switch light off");
        updateLight();
    }
    private void updateLight(){
        light.setEnabled(camera!=null&&previewReady&&flashAvailable&&!busy);
        if(camera==null){light.setText("Luce: spenta");return;}
        light.setText(!flashAvailable?"Flash non disponibile":Camera.Parameters.FLASH_MODE_TORCH.equals(lightMode)?"Luce continua: accesa":Camera.Parameters.FLASH_MODE_ON.equals(lightMode)?"Flash allo scatto: attivo":"Luce: spenta");
    }
    private void toggleLight(){
        if(camera==null||busy)return;lightEnabled=!lightEnabled;
        try{configureLight();}catch(RuntimeException error){cameraFailure("Impossibile regolare la luce. Riprova.");}
    }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);
        if(request!=CAMERA_REQUEST)return;permissionPending=false;
        if(results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED)startCamera();
        else{status.setText("Consenti l’accesso alla fotocamera per fare la prova.");shoot.setText("Consenti fotocamera");shoot.setEnabled(true);}
    }
    private void releaseCamera(){
        previewReady=false;
        if(previewTimeout!=null)main.removeCallbacks(previewTimeout);
        if(focusTimeout!=null)main.removeCallbacks(focusTimeout);
        if(captureTimeout!=null)main.removeCallbacks(captureTimeout);
        Camera previous=camera;camera=null;
        if(previous!=null){
            try{Camera.Parameters parameters=previous.getParameters();List<String> modes=parameters.getSupportedFlashModes();
                if(modes!=null&&modes.contains(Camera.Parameters.FLASH_MODE_OFF)){parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);previous.setParameters(parameters);}
            }catch(RuntimeException ignored){}
            previous.setErrorCallback(null);previous.release();
        }
        if(light!=null)updateLight();
        if(shoot!=null&&photo.getVisibility()!=View.VISIBLE&&!busy)shoot.setEnabled(false);
    }
    private void cameraFailure(String message){
        busy=false;releaseCamera();status.setText(message);shoot.setText("Riprova fotocamera");shoot.setEnabled(true);
    }
    private void capture(){
        if(busy)return;
        if(photo.getVisibility()==View.VISIBLE){
            if(challengePassed){target=ObjectRecognition.nextTarget(random,target);targetPrompt.setText(target.prompt());}
            challengePassed=false;
            photo.setImageDrawable(null);photo.setVisibility(View.GONE);result.setText("");startCamera();return;
        }
        if(camera==null){startCamera();return;}
        if(!previewReady)return;
        busy=true;shoot.setEnabled(false);updateLight();result.setText("");status.setText("Messa a fuoco…");
        Camera source=camera;int request=++captureId;
        focusTimeout=()->takePhoto(source,request);
        main.postDelayed(focusTimeout,1500);
        try{
            String focus=source.getParameters().getFocusMode();
            if(Camera.Parameters.FOCUS_MODE_AUTO.equals(focus)||Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE.equals(focus)||Camera.Parameters.FOCUS_MODE_MACRO.equals(focus))
                source.autoFocus((success,focused)->{if(focusTimeout!=null)takePhoto(focused,request);});
            else takePhoto(source,request);
        }catch(RuntimeException error){takePhoto(source,request);}
    }
    private void takePhoto(Camera source,int request){
        if(source!=camera||request!=captureId||!resumed||!busy||focusTimeout==null)return;
        main.removeCallbacks(focusTimeout);focusTimeout=null;status.setText("Scatto della foto…");
        captureTimeout=()->{if(source==camera&&busy)cameraFailure("Scatto non riuscito. Tocca Riprova fotocamera.");};
        main.postDelayed(captureTimeout,10000);
        try{source.takePicture(null,null,(bytes,c)->{
            if(c!=camera||request!=captureId||!resumed)return;
            analyzing=true;releaseCamera();status.setText("Analisi della foto in corso…");analyze(bytes);
        });}catch(RuntimeException error){cameraFailure("Scatto non riuscito. Tocca Riprova fotocamera.");}
    }
    private void analyze(byte[] bytes){
        ObjectRecognition.Cutlery requested=target;
        // Decode, rotate and infer off the UI thread; retain enough detail for thin cutlery.
        worker.execute(()->{
            Bitmap bitmap=null;ObjectRecognition.Result outcome;
            try{
                BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;
                BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);options.inJustDecodeBounds=false;options.inSampleSize=1;
                while(Math.max(options.outWidth,options.outHeight)/options.inSampleSize>1600)options.inSampleSize*=2;
                bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);
                if(bitmap==null)throw new IllegalArgumentException("Invalid JPEG");
                int orientation=new ExifInterface(new ByteArrayInputStream(bytes)).getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL);
                Matrix rotation=new Matrix();
                switch(orientation){
                    case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:rotation.setScale(-1,1);break;
                    case ExifInterface.ORIENTATION_ROTATE_180:rotation.setRotate(180);break;
                    case ExifInterface.ORIENTATION_FLIP_VERTICAL:rotation.setScale(1,-1);break;
                    case ExifInterface.ORIENTATION_TRANSPOSE:rotation.setRotate(90);rotation.postScale(-1,1);break;
                    case ExifInterface.ORIENTATION_ROTATE_90:rotation.setRotate(90);break;
                    case ExifInterface.ORIENTATION_TRANSVERSE:rotation.setRotate(-90);rotation.postScale(-1,1);break;
                    case ExifInterface.ORIENTATION_ROTATE_270:rotation.setRotate(-90);break;
                }
                if(!rotation.isIdentity()){Bitmap rotated=Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),rotation,true);if(rotated!=bitmap)bitmap.recycle();bitmap=rotated;}
                if(detector==null)detector=new ObjectDetector(this);outcome=detector.detect(bitmap,requested);
            }catch(Exception error){outcome=new ObjectRecognition.Result(false,"Riconoscimento non riuscito. Scatta un’altra foto della posata richiesta.");}
            Bitmap photograph=bitmap;ObjectRecognition.Result evaluation=outcome;
            runOnUiThread(()->{
                if(isDestroyed()||isFinishing()){if(photograph!=null)photograph.recycle();return;}
                analyzing=false;busy=false;
                challengePassed=evaluation.passed;
                if(photograph!=null){photo.setImageBitmap(photograph);photo.setVisibility(View.VISIBLE);}
                result.setText(evaluation.message);status.setText(photograph!=null?"Foto analizzata sul dispositivo.":"Impossibile leggere la foto.");
                shoot.setText(photograph!=null?(challengePassed?"Prossima posata":"Riprova la foto"):"Riprova fotocamera");shoot.setEnabled(true);
            });
        });
    }
}
