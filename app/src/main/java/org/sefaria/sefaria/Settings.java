package org.sefaria.sefaria;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Pair;

import org.sefaria.sefaria.activities.SuperTextActivity;
import org.sefaria.sefaria.database.API;
import org.sefaria.sefaria.database.Book;
import org.sefaria.sefaria.database.Node;
import org.sefaria.sefaria.database.Text;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


public class Settings {

    static public SharedPreferences getGeneralSettings(){
        return MyApp.getContext().getSharedPreferences("org.sefaria.sefaria.general_settings", Context.MODE_PRIVATE);
    }

    /*
    Lang stuff
     */

    private static Util.Lang menuLang = null;

    static public void setMenuLang(Util.Lang lang){
        menuLang = lang;
        SharedPreferences generalSettings = getGeneralSettings();
        SharedPreferences.Editor editor = generalSettings.edit();
        String langStr = lang2Str(lang);
        editor.putString("menuLang",langStr);
        editor.commit();
    }


    static public Util.Lang getMenuLang(){
        if(menuLang != null) return menuLang;
        SharedPreferences generalSettings = getGeneralSettings();
        String langStr = generalSettings.getString("menuLang", "");
        menuLang = str2Lang(langStr);
        return menuLang;
    }

    private static String lang2Str(Util.Lang lang){
        if(lang == Util.Lang.HE)
            return "he";
        else if(lang == Util.Lang.EN)
            return "en";
        else //bi
            return  "bi";
    }

    private static Util.Lang str2Lang(String langStr){
        if(langStr.equals("he"))
            return Util.Lang.HE;
        else if(langStr.equals("en"))
            return Util.Lang.EN;
        else if(langStr.equals("bi"))
            return Util.Lang.BI;
        else //if(langStr.equals("") || anything else)
            return getSystemLang();
    }

    /**
     * change the menuLang from hebrew to english or visa versa
     *  it returns the new menuLang;
     */
    public static Util.Lang switchMenuLang(){
        if(menuLang == Util.Lang.EN)
            menuLang = Util.Lang.HE;
        else menuLang = Util.Lang.EN;
        Settings.setMenuLang(menuLang);
        return menuLang;
    }

    public static Util.Lang getSystemLang(){
        if(Util.isSystemLangHe())
            return Util.Lang.HE;
        else
            return Util.Lang.EN;
    }

    public static Util.Lang getDefaultTextLang(){
        return Util.Lang.BI;
    }


    public static class BookSettings {
        public Node node;
        public int tid;
        public Util.Lang lang;


        BookSettings(Node node, int tid, Util.Lang lang){
            this.node = node;
            this.tid = tid;
            this.lang = lang;
        }

        static private SharedPreferences getBookSavedSettings() {
            return MyApp.getContext().getSharedPreferences("org.sefaria.sefaria.book_save_settings", Context.MODE_PRIVATE);
        }

        static private SharedPreferences getBookSavedTitleSettings() {
            return MyApp.getContext().getSharedPreferences("org.sefaria.sefaria.book_save_title_settings", Context.MODE_PRIVATE);
        }

        static public BookSettings getSavedBook(Book book) throws API.APIException, Node.InvalidPathException {
            SharedPreferences bookSavedSettings = getBookSavedSettings();
            String stringThatRepsSavedSettings = bookSavedSettings.getString(book.title, "");
            //Log.d("SuperTextAct", "bookSavedSettings:" + stringThatRepsSavedSettings);
            String[] settings = stringThatRepsSavedSettings.split(SETTINGS_SPLITTER);
            Log.d("Settings", "stringThatRepsSavedSettings:" + stringThatRepsSavedSettings);
            String nodePathStr = settings[0];
            int tid = 0;
            try {
                Log.d("Settings0", settings[0]);
                Log.d("Settings1", settings[1]);
                tid = Integer.valueOf(settings[1]);
            }catch (Exception e){
                e.printStackTrace();
            }
            Node node = book.getNodeFromPathStr(nodePathStr);
            node = node.getFirstDescendant();//should be unneeded line, but in case there was a previous bug this should return a isTextSection() node to avoid bugs
            Log.d("Settings", "node:" + node);
            BookSettings bookSettings = new BookSettings(node,tid, Util.Lang.BI);
            return bookSettings;
        }

        final static private String SETTINGS_SPLITTER = "@";

        static public void setSavedBook(Book book,Node node, Text text, Util.Lang lang){
            SharedPreferences bookSavedSettings = getBookSavedSettings();
            SharedPreferences.Editor editor = bookSavedSettings.edit();
            //"<en|he|bi>.<cts|sep>.<white|grey|black>.10px:"+ <rootNum>.<Childnum>.<until>.<leaf>.<verseNum>"
            String tid = "0";
            if (text != null)
                tid = "" + text.tid;
            String settingStr = node.makePathDefiningNode() + SETTINGS_SPLITTER + tid + SETTINGS_SPLITTER + lang2Str(lang);
            editor.putString(book.title, settingStr);
            editor.commit();

            //now for titles
            editor = getBookSavedTitleSettings().edit();
            editor.putString(EN_TITLE + book.title, node.getMenuBarTitle(book, Util.Lang.EN));
            editor.putString(HE_TITLE + book.title, node.getMenuBarTitle(book, Util.Lang.HE));
            editor.commit();

        }



        final static private String EN_TITLE = "EN___";
        final static private String HE_TITLE = "HE___";

        static public Pair<String, String> getSavedBookTitle(String title) {
            SharedPreferences bookSavedTitleSettings = getBookSavedTitleSettings();
            Pair<String, String> pair = new Pair<>(
                    bookSavedTitleSettings.getString(EN_TITLE + title, ""),
                    bookSavedTitleSettings.getString(HE_TITLE + title, "")
            );
            return pair;
        }
    }



    public static class RecentTexts {

        static private SharedPreferences getRecentSettings() {
            return MyApp.getContext().getSharedPreferences("org.sefaria.sefaria.recent_texts_settings", Context.MODE_PRIVATE);
        }

        public static List<String> getRecentTexts() {
            List<String> books = new ArrayList<>(MAX_RECENT_TEXTS);
            SharedPreferences recentSettings = getRecentSettings();
            for (int i = 0; i < MAX_RECENT_TEXTS; i++) {
                String bookTitle = recentSettings.getString("" + i, "");
                if (bookTitle == "")
                    return books;
                books.add(bookTitle);
            }
            return books;
        }

        private final static int MAX_RECENT_TEXTS = 3;

        public static void addRecentText(String bookTitle) {
            List<String> books = getRecentTexts();
            for (int i = 0; i <books.size() && i<MAX_RECENT_TEXTS ; i++) {
                if(books.get(i).equals(bookTitle))
                    books.remove(i);
            }
            books.add(0, bookTitle);
            SharedPreferences.Editor editor = getRecentSettings().edit();
            for (int i = 0; i < books.size() && i < MAX_RECENT_TEXTS; i++) {
                editor.putString("" + i, books.get(i));
            }
            editor.commit();
        }
    }

}
