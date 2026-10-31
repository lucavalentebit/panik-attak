package it.panikattak.app;

/** Presentation of the bundled COCO model's scores, not calibrated probabilities. */
final class ObjectRecognition {
    private static final int[] IDS={47,49,48,46,61};
    private static final String[] NAMES={"Forchetta","Cucchiaio","Coltello","Tazza","Sedia"};
    static String describe(float[] classes,float[] scores,int count){
        float[] best=new float[IDS.length];
        for(int n=0;n<Math.min(count,Math.min(classes.length,scores.length));n++){
            if(!Float.isFinite(scores[n])||scores[n]<0||scores[n]>1||!Float.isFinite(classes[n]))continue;
            int id=Math.round(classes[n]);
            for(int k=0;k<IDS.length;k++)if(id==IDS[k])best[k]=Math.max(best[k],scores[n]);
        }
        StringBuilder found=new StringBuilder(),details=new StringBuilder();
        for(int k=0;k<IDS.length;k++){
            if(best[k]>=0.5f){
                if(found.length()>0)found.append('\n');
                found.append(NAMES[k]).append(" · ").append(Math.round(best[k]*100)).append('%');
                if(IDS[k]==61)found.append(" (gambe non verificate)");
            }
            if(best[k]>=0.25f){
                if(details.length()>0)details.append(" · ");
                details.append(NAMES[k]).append(' ').append(Math.round(best[k]*100)).append('%');
            }
        }
        if(found.length()==0)found.append("Nessun oggetto riconosciuto con sufficiente confidenza.");
        return found+"\n\nRilevazioni: "+(details.length()==0?"nessuna":details)+"\nLe percentuali sono punteggi del modello, non garanzie di correttezza.";
    }
}
