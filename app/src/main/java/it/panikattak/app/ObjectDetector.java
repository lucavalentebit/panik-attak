package it.panikattak.app;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import org.tensorflow.lite.Interpreter;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

/** CPU-only COCO detector. Call from one background thread. */
final class ObjectDetector implements AutoCloseable {
    private static final int SIZE=300;
    private final Interpreter interpreter;
    ObjectDetector(Context context) throws Exception {
        try(AssetFileDescriptor asset=context.getAssets().openFd("object_detector.tflite");
            FileInputStream stream=new FileInputStream(asset.getFileDescriptor())) {
            ByteBuffer model=stream.getChannel().map(FileChannel.MapMode.READ_ONLY,asset.getStartOffset(),asset.getDeclaredLength());
            interpreter=new Interpreter(model,new Interpreter.Options().setNumThreads(2));
        }
    }
    String detect(Bitmap original){
        Bitmap image=Bitmap.createScaledBitmap(original,SIZE,SIZE,true);
        ByteBuffer input=ByteBuffer.allocateDirect(SIZE*SIZE*3).order(ByteOrder.nativeOrder());
        int[] pixels=new int[SIZE*SIZE];image.getPixels(pixels,0,SIZE,0,0,SIZE,SIZE);
        for(int pixel:pixels){input.put((byte)(pixel>>16));input.put((byte)(pixel>>8));input.put((byte)pixel);}
        input.rewind();if(image!=original)image.recycle();
        int maxDetections=interpreter.getOutputTensor(0).shape()[1];
        float[][][] boxes=new float[1][maxDetections][4];
        float[][] classes=new float[1][maxDetections],scores=new float[1][maxDetections];
        float[] count=new float[1];
        java.util.Map<Integer,Object> outputs=new java.util.HashMap<>();
        outputs.put(0,boxes);outputs.put(1,classes);outputs.put(2,scores);outputs.put(3,count);
        interpreter.runForMultipleInputsOutputs(new Object[]{input},outputs);
        return ObjectRecognition.describe(classes[0],scores[0],(int)count[0]);
    }
    @Override public void close(){interpreter.close();}
}
