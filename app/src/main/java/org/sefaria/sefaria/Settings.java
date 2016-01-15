package org.sefaria.sefaria;

import android.content.Context;
import android.content.SharedPreferences;

import org.sefaria.sefaria.activities.SuperTextActivity;


public class Settings {

    static public SharedPreferences getGeneralSettings(){
        return MyApp.getContext().getSharedPreferences("org.sefaria.sefaria.general_settings", Context.MODE_PRIVATE);
    }

    static public void setSavedMenuLang(Util.Lang lang){
        SharedPreferences generalSettings = getGeneralSettings();
        SharedPreferences.Editor editor = generalSettings.edit();
        String langStr;
        if(lang == Util.Lang.HE)
            langStr = "he";
        else if(lang == Util.Lang.EN)
            langStr = "en";
        else //bi
            langStr = "bi";
        editor.putString("menuLang",langStr);
        editor.commit();
    }

    static public Util.Lang getSavedMenuLang(){
        SharedPreferences generalSettings = getGeneralSettings();
        String langStr = generalSettings.getString("menuLang","");
        if(langStr.equals("he"))
            return Util.Lang.HE;
        else if(langStr.equals("en"))
            return Util.Lang.EN;
        else if(langStr.equals("bi"))
            return Util.Lang.BI;
        else //if(langStr.equals("") || anything else)
            return MyApp.getSystemLang();
    }

    //There's also SuperTextActivity.getBookSavedSettings()

}
