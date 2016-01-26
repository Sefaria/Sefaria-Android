package org.sefaria.sefaria;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.sefaria.sefaria.activities.SuperTextActivity;
import org.sefaria.sefaria.database.API;
import org.sefaria.sefaria.database.Book;
import org.sefaria.sefaria.database.Node;


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
        String langStr = generalSettings.getString("menuLang", "");
        if(langStr.equals("he"))
            return Util.Lang.HE;
        else if(langStr.equals("en"))
            return Util.Lang.EN;
        else if(langStr.equals("bi"))
            return Util.Lang.BI;
        else //if(langStr.equals("") || anything else)
            return MyApp.getSystemLang();
    }

    static public Node getSavedBook(Book book) throws API.APIException, Node.InvalidPathException {
        SharedPreferences bookSavedSettings = getBookSavedSettings();
        String stringThatRepsSavedSettings = bookSavedSettings.getString(book.title, "");
        Log.d("SuperTextAct", "bookSavedSettings:" + stringThatRepsSavedSettings);
        String nodePathStr = stringThatRepsSavedSettings; //TODO  make based on split()
        Node node = book.getNodeFromPathStr(nodePathStr);
        Log.d("SuperTextAct", "bookSavedSettings... node:" + node);
        node =  node.getFirstDescendant();//should be unneeded line, but in case there was a previous bug this should return a isTextSection() node to avoid bugs
        return node;
    }

    static public void setSavedBook(Book book, Node node){
        //update the place that the book will go to when returning to book
        SharedPreferences bookSavedSettings = getBookSavedSettings();
        SharedPreferences.Editor editor = bookSavedSettings.edit();
        String strTreeAndNode = node.makePathDefiningNode();
        //"<en|he|bi>.<cts|sep>.<white|grey|black>.10px:"+ <rootNum>.<Childnum>.<until>.<leaf>.<verseNum>"
        editor.putString(book.title, strTreeAndNode);
        editor.commit();
    }

    static private SharedPreferences getBookSavedSettings(){
        return MyApp.getContext().getSharedPreferences("org.sefaria.sefaria.book_save_settings", Context.MODE_PRIVATE);
    }

}
