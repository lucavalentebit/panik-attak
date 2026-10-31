package it.panikattak.app;

/** Run with ObjectRecognition.java; kept separate from legacy alarm-only runners. */
public final class ObjectRecognitionTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        String spoon=ObjectRecognition.describe(new float[]{49},new float[]{0.44f},1);
        check(spoon.startsWith("Nessun oggetto"),"44% must stay uncertain");
        check(spoon.contains("Cucchiaio 44%"),"show the user's low-confidence spoon result");
        String all=ObjectRecognition.describe(new float[]{47,49,48},new float[]{0.91f,0.85f,0.78f},3);
        check(all.startsWith("Forchetta · 91%\nCucchiaio · 85%\nColtello · 78%"),"keep cutlery identities separate");
        check(!all.contains("Posate"),"do not merge distinct target objects");
        String duplicate=ObjectRecognition.describe(new float[]{49,49},new float[]{0.4f,0.8f},2);
        check(duplicate.startsWith("Cucchiaio · 80%\n\nRilevazioni: Cucchiaio 80%"),"use strongest detection per category");
        check(ObjectRecognition.describe(new float[]{49},new float[]{0.9f},0).contains("Rilevazioni: nessuna"),"respect detection count");
        check(ObjectRecognition.describe(new float[]{49},new float[]{0.5f},1).startsWith("Cucchiaio · 50%"),"preserve acceptance threshold");
        check(ObjectRecognition.describe(new float[]{49},new float[]{0.249f},1).contains("Rilevazioni: nessuna"),"filter weak detections");
        check(ObjectRecognition.describe(new float[]{16,45},new float[]{0.99f,0.99f},2).startsWith("Nessun oggetto"),"unrelated classes never become targets or coffee pods");
        check(ObjectRecognition.describe(new float[]{49,47,48},new float[]{Float.NaN,Float.POSITIVE_INFINITY,1.1f},3).contains("Rilevazioni: nessuna"),"reject invalid scores");
        check(ObjectRecognition.describe(new float[]{49},new float[]{0.9f},20).startsWith("Cucchiaio"),"bound count by output arrays");
        check(ObjectRecognition.describe(new float[]{61},new float[]{0.9f},1).contains("gambe non verificate"),"retain chair limitation");
        System.out.println("PASS: "+checks+" object recognition checks");
    }
}
