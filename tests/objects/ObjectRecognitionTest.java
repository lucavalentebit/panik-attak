package it.panikattak.app;

import java.util.Random;
import static it.panikattak.app.ObjectRecognition.Cutlery.*;

/** Synthetic detector outputs verify challenge rules, not photographic accuracy. */
public final class ObjectRecognitionTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static Random choose(int index){return new Random(){@Override public int nextInt(int bound){if(index<0||index>=bound)throw new AssertionError("Invalid random bound");return index;}};}
    private static ObjectRecognition.Result result(ObjectRecognition.Cutlery target,float[] classes,float[] scores,int count){return ObjectRecognition.evaluate(target,classes,scores,count);}
    public static void main(String[] args){
        ObjectRecognition.Cutlery[] choices=ObjectRecognition.Cutlery.values();
        check(choices.length==3,"challenge has exactly the three cutlery targets");
        for(int k=0;k<choices.length;k++){
            var target=choices[k];
            check(ObjectRecognition.nextTarget(choose(k),null)==target,"initial draw can select "+target);
            check(ObjectRecognition.restoreTarget(target.name(),choose(0))==target,"saved target is preserved: "+target);
            var correct=result(target,new float[]{target.modelId},new float[]{0.9f},1);
            check(correct.passed&&correct.message.startsWith("Prova superata!"),"requested cutlery passes: "+target);
            check(correct.message.contains(target.object),"success names the requested cutlery: "+target);
            check(result(target,new float[]{target.modelId},new float[]{0.5f},1).passed,"50% remains the acceptance boundary: "+target);
            check(!result(target,new float[]{target.modelId},new float[]{0.499f},1).passed,"below threshold cannot pass: "+target);
            for(var other:choices)if(other!=target){
                var wrong=result(target,new float[]{other.modelId},new float[]{0.99f},1);
                check(!wrong.passed,"wrong cutlery never passes: "+other+" for "+target);
                check(wrong.message.contains(target.object)&&wrong.message.contains(other.object),"retry explains requested and detected cutlery");
                check(!result(target,new float[]{target.modelId,other.modelId},new float[]{0.7f,0.9f},2).passed,"stronger competing cutlery blocks success");
            }
            for(int draw=0;draw<2;draw++){
                var next=ObjectRecognition.nextTarget(choose(draw),target);
                check(next!=target,"next draw excludes the completed target");
                check(next==choices[(draw>=k)?draw+1:draw],"both remaining cutlery choices are reachable");
            }
            var unrelated=result(target,new float[]{46,61,45,16},new float[]{0.99f,0.99f,0.99f,0.99f},4);
            check(!unrelated.passed&&unrelated.message.contains("Rilevazioni: nessuna"),"non-cutlery cannot pass or appear in detections");
            check(!unrelated.message.contains("Tazza")&&!unrelated.message.contains("Sedia"),"removed categories are absent from results");
            check(result(target,new float[]{46,61,target.modelId},new float[]{0.99f,0.99f,0.8f},3).passed,"unrelated detections do not compete with cutlery");
        }
        var spoon=result(SPOON,new float[]{49},new float[]{0.44f},1);
        check(!spoon.passed&&spoon.message.contains("Cucchiaio 44%"),"44% spoon stays uncertain");
        var duplicate=result(SPOON,new float[]{49,49},new float[]{0.4f,0.8f},2);
        check(duplicate.passed&&duplicate.message.contains("Rilevazioni: Cucchiaio 80%"),"strongest duplicate is used once");
        check(!result(FORK,new float[]{47,49},new float[]{0.8f,0.8f},2).passed,"tied cutlery classifications stay uncertain");
        check(result(KNIFE,new float[]{47,49,48},new float[]{0.6f,0.6f,0.9f},3).passed,"stronger winner resolves an earlier tie");
        check(!result(SPOON,new float[]{49},new float[]{0.9f},0).passed,"zero count ignores output padding");
        check(!result(SPOON,new float[]{49},new float[]{0.9f},-1).passed,"negative count does not read detections");
        check(result(SPOON,new float[]{49},new float[]{0.9f},20).passed,"oversized count is bounded by output arrays");
        check(!result(SPOON,new float[]{49},new float[]{},1).passed,"mismatched output lengths are bounded");
        check(!result(FORK,new float[]{},new float[]{},0).passed,"empty output cannot pass");
        check(result(SPOON,new float[]{49},new float[]{0.249f},1).message.contains("Rilevazioni: nessuna"),"weak detections are hidden");
        check(result(SPOON,new float[]{49},new float[]{0.25f},1).message.contains("Cucchiaio 25%"),"diagnostic boundary is retained");
        check(result(SPOON,new float[]{49,49,49,49},new float[]{Float.NaN,Float.POSITIVE_INFINITY,-0.1f,1.1f},4).message.contains("Rilevazioni: nessuna"),"invalid scores are rejected");
        check(result(SPOON,new float[]{Float.NaN,Float.POSITIVE_INFINITY,49.1f},new float[]{0.9f,0.9f,0.9f},3).message.contains("Rilevazioni: nessuna"),"invalid class identifiers are rejected");
        check(ObjectRecognition.restoreTarget("CHAIR",choose(1))==SPOON,"obsolete state falls back to supported cutlery");
        check(ObjectRecognition.restoreTarget(null,choose(2))==KNIFE,"missing saved state draws a target");
        System.out.println("PASS: "+checks+" cutlery challenge rule checks on synthetic outputs");
    }
}
