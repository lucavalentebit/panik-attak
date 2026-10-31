package it.panikattak.app;

import java.util.Random;

/** Cutlery-only challenge rules. Model scores are not calibrated probabilities. */
final class ObjectRecognition {
    enum Cutlery {
        FORK(47,"Forchetta","una forchetta"),
        SPOON(49,"Cucchiaio","un cucchiaio"),
        KNIFE(48,"Coltello","un coltello");
        final int modelId;
        final String label,object;
        Cutlery(int modelId,String label,String object){this.modelId=modelId;this.label=label;this.object=object;}
        String prompt(){return "Fotografa "+object;}
    }
    static final class Result {
        final boolean passed;
        final String message;
        Result(boolean passed,String message){this.passed=passed;this.message=message;}
    }
    static Cutlery nextTarget(Random random,Cutlery previous){
        Cutlery[] choices=Cutlery.values();
        int selected=random.nextInt(previous==null?choices.length:choices.length-1);
        if(previous!=null&&selected>=previous.ordinal())selected++;
        return choices[selected];
    }
    static Cutlery restoreTarget(String saved,Random random){
        if(saved!=null)try{return Cutlery.valueOf(saved);}catch(IllegalArgumentException ignored){}
        return nextTarget(random,null);
    }
    static Result evaluate(Cutlery target,float[] classes,float[] scores,int count){
        if(target==null)throw new IllegalArgumentException("Missing cutlery target");
        Cutlery[] choices=Cutlery.values();float[] best=new float[choices.length];
        for(int n=0;n<Math.min(count,Math.min(classes.length,scores.length));n++){
            if(!Float.isFinite(scores[n])||scores[n]<0||scores[n]>1||!Float.isFinite(classes[n]))continue;
            for(int k=0;k<choices.length;k++)if(classes[n]==choices[k].modelId)best[k]=Math.max(best[k],scores[n]);
        }
        int winner=-1;float highest=0;boolean tied=false;StringBuilder details=new StringBuilder();
        for(int k=0;k<choices.length;k++){
            if(best[k]>highest){highest=best[k];winner=k;tied=false;}
            else if(best[k]>0&&best[k]==highest)tied=true;
            if(best[k]>=0.25f){
                if(details.length()>0)details.append(" · ");
                details.append(choices[k].label).append(' ').append(Math.round(best[k]*100)).append('%');
            }
        }
        boolean passed=highest>=0.5f&&!tied&&choices[winner]==target;
        String message;
        if(passed)message="Prova superata! Il modello ha riconosciuto "+target.object+".";
        else if(highest>=0.5f&&tied)message="Riconoscimento incerto tra più posate. Inquadra solo "+target.object+" e riprova.";
        else if(highest>=0.5f)message="Il modello rileva "+choices[winner].object+". La prova richiede "+target.object+". Riprova.";
        else message="Posata non riconosciuta con sufficiente confidenza. Inquadra "+target.object+" e riprova.";
        return new Result(passed,message+"\n\nRilevazioni: "+(details.length()==0?"nessuna":details)+"\nLe percentuali sono punteggi del modello, non garanzie di correttezza.");
    }
}
