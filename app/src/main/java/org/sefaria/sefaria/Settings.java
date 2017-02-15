package org.sefaria.sefaria;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.util.Pair;

import org.sefaria.sefaria.TOCElements.TOCVersion;
import org.sefaria.sefaria.activities.SuperTextActivity;
import org.sefaria.sefaria.database.API;
import org.sefaria.sefaria.database.Book;
import org.sefaria.sefaria.database.LinkFilter;
import org.sefaria.sefaria.database.Node;
import org.sefaria.sefaria.database.Segment;

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
        editor.apply();
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
        editor.apply();

        BookSettings.setAllBookSettingsTextLang(lang);
    }


    public static void addSearchTerm(String string){
        Set set = getSearchTerms();
        if(!set.contains(string))
            set.add(string);
        SharedPreferences.Editor editor = getGeneralSettings().edit();
        editor.putStringSet("searchTerms", set);
        editor.apply();
    }

    public static Set<String> getSearchTerms(){
        SharedPreferences generalSettings = getGeneralSettings();
        Set<String> set = generalSettings.getStringSet("searchTerms", null);
        if(set == null)
            set = new HashSet<>();
        return set;
    }


    private static final boolean defaultSideBySide = MyApp.getScreenSize().x > 400;

    public static boolean getIsSideBySide(){
        SharedPreferences generalSettings = getGeneralSettings();
        boolean isSideBySide =  generalSettings.getBoolean("sideBySide", defaultSideBySide);
        return isSideBySide;
    }

    public static void setIsSideBySide(boolean isSideBySide){
        SharedPreferences generalSettings = getGeneralSettings();
        SharedPreferences.Editor editor = generalSettings.edit();
        editor.putBoolean("sideBySide", isSideBySide);
        editor.apply();
    }

    public static boolean getIsCts() {
        return getIsCts(null);
    }

    /**
     * default value is dependent on whether or not the segment can be cts
     * @param book
     * @return
     */
    public static boolean getIsCts(Book book) {
        SharedPreferences generalSettings = getGeneralSettings();
        boolean isCts = generalSettings.getBoolean("cts", SuperTextActivity.canBeCts(book));
        return isCts;
    }

    public static void setIsCts(boolean isCts) {
        SharedPreferences generalSettings = getGeneralSettings();
        SharedPreferences.Editor editor = generalSettings.edit();
        editor.putBoolean("cts", isCts);
        editor.apply();
    }

    public static boolean getIsFirstTimeOpened(){
        SharedPreferences generalSettings = getGeneralSettings();
        return generalSettings.getBoolean("isFirstTimeOpened", true);
    }

    public static void setIsFirstTimeOpened(boolean isFirstTimeOpened){
        SharedPreferences generalSettings = getGeneralSettings();
        SharedPreferences.Editor editor = generalSettings.edit();
        editor.putBoolean("isFirstTimeOpened", isFirstTimeOpened);
        editor.apply();
    }

    public static boolean getUseSDCard() {
        SharedPreferences generalSettings = getGeneralSettings();
        return generalSettings.getBoolean("useSDCard",true);
    }

    public static void setUseSDCard(boolean useSDCard) {
        SharedPreferences generalSettings = getGeneralSettings();
        SharedPreferences.Editor editor = generalSettings.edit();
        editor.putBoolean("useSDCard", useSDCard);
        editor.apply();
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
        editor.apply();
    }

    public static class BookSettings {
        public Node node;
        public int textNum;
        public Util.Lang lang;
        public TOCVersion textVersion;

        BookSettings(Node node, Util.Lang lang, int textNum, TOCVersion textVersion){
            this.node = node;
            this.textNum = textNum;
            this.lang = lang;
            this.textVersion = textVersion;


            if(lang == null)
                this.lang = getDefaultTextLang();
        }

        static private SharedPreferences getBookSavedSettings() {
            return MyApp.getContext().getSharedPreferences("org.sefaria.sefaria.book_save_settings", Context.MODE_PRIVATE);
        }

        static private SharedPreferences getBookSavedTitleSettings() {
            return MyApp.getContext().getSharedPreferences("org.sefaria.sefaria.book_save_title_settings", Context.MODE_PRIVATE);
        }

        static private SharedPreferences getBookSavedTextVersion() {
            return MyApp.getContext().getSharedPreferences("org.sefaria.sefaria.book_save_text_version_settings", Context.MODE_PRIVATE);
        }

        static public void setTextVersion(Book book, TOCVersion textVersion){
            SharedPreferences.Editor editor = getBookSavedTextVersion().edit();
            String textVersionString = null;
            if(textVersion != null){
                textVersionString = textVersion.getAPIString();
                if(textVersionString.equals(""))
                    textVersionString = null;
            }
            editor.putString(book.getTitle(Util.Lang.EN),textVersionString);
            editor.apply();
        }

        static public TOCVersion getTextVersion(Book book){
            SharedPreferences sharedPreferences = getBookSavedTextVersion();
            String versionString = sharedPreferences.getString(book.getTitle(Util.Lang.EN),null);
            if(versionString == null)
                return null;
            else if(versionString.equals(TOCVersion.DEFAULT_TEXT_VERSION))
                return null;
            else
                return new TOCVersion(versionString);
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
            editor.apply();
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
            TOCVersion textVersion = getTextVersion(book);

            Node node = null;
            try {
                node = book.getNodeFromPathStr(nodePathStr);
                node = node.getFirstDescendant();//true);//should be unneeded line, but in case there was a previous bug this should return a isTextSection() node to avoid bugs
                node.setTextVersion(textVersion);
            } catch (Exception e) {
                ;
            }

            BookSettings bookSettings = new BookSettings(node,lang,textNum,textVersion);
            return bookSettings;
        }

        final static private String SETTINGS_SPLITTER = "@";


        static private int getTextNum(Node node, Segment segment) throws API.APIException {
            int textNum = -1;
            textNum = node.getTexts(true).indexOf(segment);
            return textNum;
        }

        static public boolean setSavedBook(Book book, Node node, Segment segment, Util.Lang lang){
            if(book == null) return false;
            SharedPreferences bookSavedSettings = getBookSavedSettings();
            SharedPreferences.Editor editor = bookSavedSettings.edit();
            //"<en|he|bi>.<cts|sep>.<white|grey|black>.10px:"+ <rootNum>.<Childnum>.<until>.<leaf>.<verseNum>"
            try {
                int textNum = getTextNum(node, segment);
                String settingStr = node.makePathDefiningNode() + SETTINGS_SPLITTER + textNum + SETTINGS_SPLITTER + lang2Str(lang);
                editor.putString(book.title, settingStr);
            }catch (Exception e){
                editor.remove(book.title);
                return false;
            }

            editor.apply();

            //now for titles
            editor = getBookSavedTitleSettings().edit();
            editor.putString(EN_TITLE + book.title, node.getMenuBarTitle(book, Util.Lang.EN));
            editor.putString(HE_TITLE + book.title, node.getMenuBarTitle(book, Util.Lang.HE));
            editor.apply();


            setTextVersion(book,node.getTextVersion());
            return true;
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


    final private static long TIME_TO_RECHECK = 604800000;//7*24*60*60*1000
    public static boolean getIfShouldDoUpdateCheck(){
        SharedPreferences settings = getGeneralSettings();
        long now = System.currentTimeMillis();
        Log.d("Settings","now:" + now);
        return (now - settings.getLong("lastUpdateCheck", 0) > TIME_TO_RECHECK);
    }

    public static void setLastUpdateCheckToNow(){
        SharedPreferences.Editor editor = getGeneralSettings().edit();
        long time = System.currentTimeMillis();
        editor.putLong("lastUpdateCheck", time);
        editor.apply();
    }

    public static boolean getIsDebug(){
        SharedPreferences settings = getGeneralSettings();
        return settings.getBoolean("isDebug", false);
    }

    public static void setIsDebug(boolean isDebug){
        SharedPreferences.Editor editor = getGeneralSettings().edit();
        editor.putBoolean("isDebug", isDebug);
        editor.apply();
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
        editor.apply();
    }


    public static long getDownloadSuccess(boolean clearValue){
        SharedPreferences settings = getGeneralSettings();
        long time = settings.getLong("DownloadSuccess", 0);
        if(clearValue)
            setDownloadSuccess(0);
        return time;
    }

    public static void setDownloadSuccess(long time){
        SharedPreferences.Editor editor = getGeneralSettings().edit();
        editor.putLong("DownloadSuccess", time);
        editor.apply();
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
        editor.apply();
        Settings.useAPI = useAPI;
    }

    public static void setLastBook(String title){
        SharedPreferences.Editor editor = getGeneralSettings().edit();
        editor.putString("lastBook",title);
        editor.apply();
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
            editor.apply();
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

        /**
         *
         * @param recentTextCount 0 means unlimited
         * @return
         */
        public static List<String> getRecentTexts(int recentTextCount) {
            List<String> books = new ArrayList<>();
            SharedPreferences recentSettings = getRecentSettings();
            Set<String> pinnedBooks = getPinnedBook();
            for(String bookTitle: pinnedBooks){
                books.add(bookTitle);
            }

            if(recentTextCount == 0) {//0 means unlimited
                recentTextCount = MAX_RECENT_TEXTS;
            }
            while (pinnedBooks.size() / (recentTextCount * 1.0) > .6) {
                recentTextCount += 1;
            }
            for (int i = 0; i < recentTextCount - pinnedBooks.size() && i<MAX_RECENT_TEXTS; i++) {
                String bookTitle = recentSettings.getString("" + i, "");
                if (bookTitle.length() == 0)
                    return books;
                if(pinnedBooks.contains(bookTitle)) {
                    recentTextCount++;
                }else {
                    books.add(bookTitle);
                }
            }
            return books;
        }

        private final static int MAX_RECENT_TEXTS = 900;

        public static void addRecentText(String bookTitle) {
            Log.d("Recents","starting addRecentText");
            List<String> books = getRecentTexts(0);
            for (int i = 0; i <books.size() && i<MAX_RECENT_TEXTS ; i++) {
                if(books.get(i).equals(bookTitle))
                    books.remove(i);
            }
            books.add(0, bookTitle);
            SharedPreferences.Editor editor = getRecentSettings().edit();
            for (int i = 0; i < books.size() && i < MAX_RECENT_TEXTS; i++) {
                editor.putString("" + i, books.get(i));
            }
            editor.apply();
            Log.d("Recents","finishing addRecentText");
        }

        private static String PINNED_RECENT_TEXTS = "pinned_recent_texts";
        private static String PINNED_RECENT_BOOKS = "pinned_recent_books";
        private static final String SPLITTER = "@@@";
        public static boolean addBookmark(Segment segment){
            Set<String> pinnedTextStringSet = getBookmarks();
            try {

                Book book = new Book(segment.bid);
                Node node = segment.getNodeFromText(book);
                int textNum = BookSettings.getTextNum(node, segment);
                String settingStr = node.getMenuBarTitle(book, Util.Lang.EN) + SPLITTER
                                    + node.getMenuBarTitle(book, Util.Lang.HE) + SPLITTER
                                    + node.getBid() + SPLITTER
                                    + node.makePathDefiningNode() + SPLITTER
                                    + textNum + SPLITTER;

                boolean added;

                if(pinnedTextStringSet.contains(settingStr)){
                    pinnedTextStringSet.remove(settingStr);
                    added = false;
                } else {
                    pinnedTextStringSet.add(settingStr);
                    added = true;
                }

                SharedPreferences.Editor editor = getRecentSettings().edit();
                editor.putStringSet(PINNED_RECENT_TEXTS, pinnedTextStringSet);
                editor.apply();

                return added;
            } catch (Exception e) {
                return false;
            }
        }
        public static Set<String> getBookmarks(){
            SharedPreferences pinned = getRecentSettings();
            Set<String> pinnedStringSet = pinned.getStringSet(PINNED_RECENT_TEXTS, null);
            if(pinnedStringSet == null){
                pinnedStringSet = new HashSet<>();
            }
            return pinnedStringSet;
        }

        public static boolean addPinnedBook(String bookTitle){
            Set<String> pinnedStringSet = getPinnedBook();

            boolean added;
            if(pinnedStringSet.contains(bookTitle)){
                pinnedStringSet.remove(bookTitle);
                added = false;
            } else {
                pinnedStringSet.add(bookTitle);
                added = true;
            }

            SharedPreferences.Editor editor = getRecentSettings().edit();
            editor.putStringSet(PINNED_RECENT_BOOKS, pinnedStringSet);
            editor.apply();
            return  added;

        }
        public static Set<String> getPinnedBook(){
            SharedPreferences pinned = getRecentSettings();
            Set<String> pinnedStringSet = pinned.getStringSet(PINNED_RECENT_BOOKS, null);
            if(pinnedStringSet == null){
                pinnedStringSet = new HashSet<>();
            }
            return pinnedStringSet;
        }

    }



}
