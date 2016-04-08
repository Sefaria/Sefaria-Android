package org.sefaria.sefaria;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.util.Pair;

import org.sefaria.sefaria.database.API;
import org.sefaria.sefaria.database.Book;
import org.sefaria.sefaria.database.Database;
import org.sefaria.sefaria.database.LinkFilter;
import org.sefaria.sefaria.database.Node;
import org.sefaria.sefaria.database.Text;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class Settings {

    static public SharedPreferences getGeneralSettings(){
        return MyApp.getContext().getSharedPreferences("org.sefaria.sefaria.general_settings", Context.MODE_PRIVATE);
    }

    /*
    Lang stuff
     */

    private static Util.Lang menuLang = null;
    private static Util.Lang defaultTextLang = null;

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

    public static String lang2Str(Util.Lang lang){
        if(lang == Util.Lang.HE)
            return "he";
        else if(lang == Util.Lang.EN)
            return "en";
        else //bi
            return  "bi";
    }

    public static int lang2Int(Util.Lang lang){
        if(lang == Util.Lang.HE)
            return 1;
        else if(lang == Util.Lang.EN)
            return 2;
        else //bi
            return 3;
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
        if(defaultTextLang != null) return defaultTextLang;
        SharedPreferences generalSettings = getGeneralSettings();
        String langStr = generalSettings.getString("defaultTextLang", "bi");
        defaultTextLang = str2Lang(langStr);
        return defaultTextLang;
    }

    static public void setDefaultTextLang(Util.Lang lang){
        defaultTextLang = lang;
        SharedPreferences generalSettings = getGeneralSettings();
        SharedPreferences.Editor editor = generalSettings.edit();
        String langStr = lang2Str(lang);
        editor.putString("defaultTextLang",langStr);
        editor.commit();

        BookSettings.setAllBookSettingsTextLang(lang);
    }



    public static boolean getIsSideBySide(){
        SharedPreferences generalSettings = getGeneralSettings();
        return generalSettings.getBoolean("sideBySide", false);
    }

    public static void setIsSideBySide(boolean isSideBySide){
        SharedPreferences generalSettings = getGeneralSettings();
        SharedPreferences.Editor editor = generalSettings.edit();
        editor.putBoolean("sideBySide", isSideBySide);
        editor.commit();
    }

    public static boolean getIsFirstTimeOpened(){
        SharedPreferences generalSettings = getGeneralSettings();
        return generalSettings.getBoolean("isFirstTimeOpened", true);
    }

    public static void setIsFirstTimeOpened(boolean isFirstTimeOpened){
        SharedPreferences generalSettings = getGeneralSettings();
        SharedPreferences.Editor editor = generalSettings.edit();
        editor.putBoolean("isFirstTimeOpened", isFirstTimeOpened);
        editor.commit();
    }

    private static final int DEFAULT_THEME = R.style.SefariaTheme_White;
    public static int getTheme(){
        SharedPreferences generalSettings = getGeneralSettings();
        return generalSettings.getInt("theme", DEFAULT_THEME);
    }

    public static void setTheme(int theme){
        SharedPreferences generalSettings = getGeneralSettings();
        SharedPreferences.Editor editor = generalSettings.edit();
        editor.putInt("theme", theme);
        editor.commit();
    }

    public static class BookSettings {
        public Node node;
        public int textNum;
        public Util.Lang lang;

        BookSettings(Node node, Util.Lang lang, int textNum){
            this.node = node;
            this.textNum = textNum;
            this.lang = lang;

            if(lang == null)
                this.lang = getDefaultTextLang();
        }

        static private SharedPreferences getBookSavedSettings() {
            return MyApp.getContext().getSharedPreferences("org.sefaria.sefaria.book_save_settings", Context.MODE_PRIVATE);
        }

        static private SharedPreferences getBookSavedTitleSettings() {
            return MyApp.getContext().getSharedPreferences("org.sefaria.sefaria.book_save_title_settings", Context.MODE_PRIVATE);
        }

        static private void setAllBookSettingsTextLang(Util.Lang lang){
            String langStr = lang2Str(lang);
            SharedPreferences bookSettings = getBookSavedSettings();
            SharedPreferences.Editor editor = bookSettings.edit();
            Map<String,?> map =  bookSettings.getAll();
            for (String title : map.keySet()) {
                String stringThatRepsSavedSettings = (String) map.get(title);
                String[] settings = stringThatRepsSavedSettings.split(SETTINGS_SPLITTER,4);
                String newSettings = "";
                try {
                    newSettings = settings[0] + SETTINGS_SPLITTER + settings[1] + SETTINGS_SPLITTER + langStr + SETTINGS_SPLITTER + settings[3];
                }catch (Exception e) {
                    try {
                        newSettings = settings[0] + SETTINGS_SPLITTER + settings[1] + SETTINGS_SPLITTER + langStr;
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                }
                editor.putString(title,newSettings);
            }
            editor.apply();
        }

        static public void clearAllBookSettings(){
            SharedPreferences.Editor editor = getBookSavedSettings().edit();
            editor.clear();
            editor.commit();
            editor = getBookSavedTitleSettings().edit();
            editor.clear();
            editor.apply();
        }


        static public BookSettings getSavedBook(Book book){
            SharedPreferences bookSavedSettings = getBookSavedSettings();
            String stringThatRepsSavedSettings = bookSavedSettings.getString(book.title, "");
            //Log.d("SuperTextAct", "bookSavedSettings:" + stringThatRepsSavedSettings);
            String[] settings = stringThatRepsSavedSettings.split(SETTINGS_SPLITTER);
            Log.d("Settings", "stringThatRepsSavedSettings:" + stringThatRepsSavedSettings);
            String nodePathStr = settings[0];
            int textNum = -1;
            Util.Lang lang = null;
            try {
                Log.d("Settings0", settings[0]);
                Log.d("Settings1", settings[1]);
                textNum = Integer.valueOf(settings[1]);
                lang = str2Lang(settings[2]);
            }catch (Exception e){
                e.printStackTrace();
            }
            Node node = null;
            try {
                node = book.getNodeFromPathStr(nodePathStr);
                node = node.getFirstDescendant(true);//should be unneeded line, but in case there was a previous bug this should return a isTextSection() node to avoid bugs
            } catch (Exception e) {
                ;
            }

            BookSettings bookSettings = new BookSettings(node,lang,textNum);
            return bookSettings;
        }

        final static private String SETTINGS_SPLITTER = "@";

        static public void setSavedBook(Book book,Node node, Text text, Util.Lang lang){
            SharedPreferences bookSavedSettings = getBookSavedSettings();
            SharedPreferences.Editor editor = bookSavedSettings.edit();
            //"<en|he|bi>.<cts|sep>.<white|grey|black>.10px:"+ <rootNum>.<Childnum>.<until>.<leaf>.<verseNum>"
            int textNum = -1;
            try {
                textNum = node.getTexts().indexOf(text);
            } catch (API.APIException e) {
                e.printStackTrace();
            }
            String settingStr = node.makePathDefiningNode() + SETTINGS_SPLITTER + textNum + SETTINGS_SPLITTER + lang2Str(lang);
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

    public static boolean getIsDebug(){
        SharedPreferences settings = getGeneralSettings();
        return settings.getBoolean("isDebug", false);
    }

    public static void setIsDebug(boolean isDebug){
        SharedPreferences.Editor editor = getGeneralSettings().edit();
        editor.putBoolean("isDebug", isDebug);
        editor.commit();
    }

    public static float getDefaultFontSize(){
        SharedPreferences settings = getGeneralSettings();
        return settings.getFloat("defaultFontSize", MyApp.getContext().getResources().getDimension(R.dimen.default_text_font_size));
    }

    public static void setDefaultFontSize(float size){
        if(size > MyApp.getContext().getResources().getDimension(R.dimen.max_text_font_size)){
            size = MyApp.getContext().getResources().getDimension(R.dimen.max_text_font_size);
        }else if(size < MyApp.getContext().getResources().getDimension(R.dimen.min_text_font_size)){
            size = MyApp.getContext().getResources().getDimension(R.dimen.min_text_font_size);
        }
        SharedPreferences.Editor editor = getGeneralSettings().edit();
        editor.putFloat("defaultFontSize", size);
        editor.commit();
    }


    public static long getDownloadSuccess(boolean clearValue){
        SharedPreferences settings = getGeneralSettings();
        long time =  settings.getLong("DownloadSuccess", 0);
        if(clearValue)
            setDownloadSuccess(0);
        return time;
    }

    public static void setDownloadSuccess(long time){
        SharedPreferences.Editor editor = getGeneralSettings().edit();
        editor.putLong("DownloadSuccess", time);
        editor.commit();
    }


    private static Boolean useAPI = null;
    public static boolean getUseAPI(){
        if(useAPI != null)
            return useAPI;
        SharedPreferences settings = getGeneralSettings();
        useAPI = settings.getBoolean("useAPI",false);
        return useAPI;
    }

    public static void setUseAPI(boolean useAPI){
        SharedPreferences.Editor editor = getGeneralSettings().edit();
        editor.putBoolean("useAPI",useAPI);
        editor.commit();
        Settings.useAPI = useAPI;
    }

    public static void setLastBook(String title){
        SharedPreferences.Editor editor = getGeneralSettings().edit();
        editor.putString("lastBook",title);
        editor.commit();
    }

    public static String getLastBook(){
        SharedPreferences settings = getGeneralSettings();
        return settings.getString("lastBook", "Genesis");
    }

    public static class Link {
        static private SharedPreferences getLinkSettings() {
            return MyApp.getContext().getSharedPreferences("org.sefaria.sefaria.link_settings", Context.MODE_PRIVATE);
        }

        public static void setLinks(String enBookTitle, List<LinkFilter> linkFilters){
            Set<String> set = new HashSet<>();
            for(LinkFilter linkFilter:linkFilters){
                set.add(linkFilter.getRealTitle(Util.Lang.EN));
            }
            SharedPreferences.Editor editor = getLinkSettings().edit();
            editor.putStringSet(enBookTitle,set);
            editor.commit();
        }


        private static void parseLinkFilterTree(LinkFilter linkFilter, LinkedList<LinkFilter> list, Set<String> set){
            if(set.contains(linkFilter.getRealTitle(Util.Lang.EN))){
                list.add(linkFilter);
            }
            for(LinkFilter child:linkFilter.getChildren()){
                parseLinkFilterTree(child,list,set);
            }
        }

        /**
         *
         * @param enBookTitle
         * @return Set of the strings that are the EN names for the linkFilters
         */
        public static LinkedList<LinkFilter> getLinks(String enBookTitle, LinkFilter linkFilterAll){
            SharedPreferences settings = getLinkSettings();
            Set<String> set = settings.getStringSet(enBookTitle, null);
            LinkedList<LinkFilter> list = new LinkedList<>();
            if(set == null)
                return list;
            parseLinkFilterTree(linkFilterAll,list,set);
            return list;
        }

    }


    public static class RecentTexts {

        static private SharedPreferences getRecentSettings() {
            return MyApp.getContext().getSharedPreferences("org.sefaria.sefaria.recent_texts_settings", Context.MODE_PRIVATE);
        }

        public static List<String> getRecentTexts() {
            List<String> books = new ArrayList<>();
            SharedPreferences recentSettings = getRecentSettings();
            Set<String> pinnedTexts = getPinned();
            int recentTextCount = 3;
            while(pinnedTexts.size()/(recentTextCount*1.0) > .6){
                recentTextCount += 3;
            }
            for(String bookTitle:pinnedTexts){
                books.add(bookTitle);
            }
            for (int i = 0; i < recentTextCount-pinnedTexts.size() && i<MAX_RECENT_TEXTS; i++) {
                String bookTitle = recentSettings.getString("" + i, "");
                if (bookTitle == "")
                    return books;
                if(pinnedTexts.contains(bookTitle)) {
                    recentTextCount++;
                    continue;
                }
                books.add(bookTitle);
            }
            return books;
        }

        private final static int MAX_RECENT_TEXTS = 9;

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

        public static boolean addPinned(String bookTitle){
            Set<String> pinnedStringSet = getPinned();

            boolean added;
            if(pinnedStringSet.contains(bookTitle)){
                pinnedStringSet.remove(bookTitle);
                added = false;
            } else {
                pinnedStringSet.add(bookTitle);
                added = true;
            }

            SharedPreferences.Editor editor = getRecentSettings().edit();
            editor.putStringSet(PINNED_RECENT_TEXTS, pinnedStringSet);
            editor.commit();
            return  added;

        }

        private static String PINNED_RECENT_TEXTS = "pinned_recent_texts";
        public static Set<String> getPinned(){
            SharedPreferences pinned = getRecentSettings();
            Set<String> pinnedStringSet = pinned.getStringSet(PINNED_RECENT_TEXTS, null);
            if(pinnedStringSet == null){
                pinnedStringSet = new HashSet<>();
            }
            return pinnedStringSet;
        }

    }

}
