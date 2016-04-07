package org.sefaria.sefaria;

import java.util.concurrent.Callable;

/**
 * Created by nss on 4/4/16.
 */
public abstract class DialogCallable  {

    public enum DialogType {
        ALERT,PROGRESS
    }

    private String title;
    private String message;
    private String positiveText;
    private String negativeText;
    private String neutralText;
    private DialogType type;

    //If DialogCallable is type=PROGRESS, only the negativeClick() function is used

    public DialogCallable(String title, String message, String positiveText, String negativeText, String neutralText,DialogType type ) {
        this.title = title;
        this.message = message;
        this.positiveText = positiveText;
        this.negativeText = negativeText;
        this.neutralText = neutralText;
        this.type = type;
    }
    public void positiveClick(){}
    public void negativeClick(){}
    public void neutralClick(){}

    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getPositiveText() { return positiveText; }
    public String getNegativeText() { return negativeText; }
    public String getNeutralText() { return neutralText; }
    public DialogType getType() { return type; }
}
