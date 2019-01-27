package org.sefaria.sefaria.database;


import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sefaria.sefaria.MenuElements.MenuDirectRef;
import org.sefaria.sefaria.Settings;
import org.sefaria.sefaria.Util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

public class DailyLearning {

    private String enTitle;
    private String heTitle;
    private String nodePath;

    public DailyLearning(){
    }

    private static MenuDirectRef getDafYomi(Context context){
        String today = getLongDate(0);
        try {
            JSONArray dafs = Util.openJSONArrayFromAssets("calendar/daf-yomi.json");
            for (int i = 0; i < dafs.length(); i++) {
                JSONObject daf = dafs.getJSONObject(i);
                String date = daf.getString("date");
                if(date.equals(today))
                {

                    String todayDaf = daf.getString("daf");
                    String bookTitle = todayDaf.replaceFirst("\\s[0-9]*$", "");
                    String dafNum = todayDaf.replaceFirst("^[^0-9]*\\s","");

                    Book book = new Book(bookTitle);
                    Node node = book.getTOCroots().get(0);
                    for(Node child: node.getChildren()){
                        if(child.getNiceGridNum(Util.Lang.EN).equals(dafNum + "a")){
                            node = child;
                            break;

                        }
                    }
                    node = node.getFirstDescendant();
                    MenuDirectRef menuDirectRef = new MenuDirectRef(
                            context,
                            todayDaf,
                            book.heTitle + " " + node.getNiceGridNum(Util.Lang.HE),
                            node.makePathDefiningNode(),
                            book,
                            "Daf Yomi",
                            null);
                    return menuDirectRef;
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static Node getParshaFromSefer(Book sefer, String parshaName) throws API.APIException{
        Node node = sefer.getTOCroots().get(1);
        for(Node child : node.getChildren()){
            if(child.getTitle(Util.Lang.EN).equals(parshaName)){
                //go to first aliyah
                return child.getFirstDescendant();
            }
        }
        // if you made it here you have a problem...
        // I guess just return first in sefer
        return node.getFirstDescendant();
    }

    static class ChapVersePath{
        public ChapVerse start = null;
        public ChapVerse end = null;

        public String toHeString() {
            if (end != null) {
                return start.toHeString() + "-" + end.toHeString();
            } else {
                return start.toHeString();
            }
        }

        public void add(ChapVerse chapVerse, int index){
            if(index == 0){
                start = chapVerse;
            }else{
                end = chapVerse;
            }
        }
    }

    static class ChapVerse {
        public Integer chap = null;
        public Integer verse = null;
        public String toHeString(){
            if(chap == null)
                return "";
            else if(verse == null)
                return Util.int2heb(chap);
            else
                return Util.int2heb(chap) + ":" + Util.int2heb(verse);
        }
    }

    private static ChapVersePath getChapVerses(String fullNumber){
        String [] numbers = fullNumber.split("-");
        ChapVersePath chapVersePath = new ChapVersePath();
        for(int i = 0; i < numbers.length; i++){
            String [] chapVerseStrings = numbers[i].split(":");
            ChapVerse chapVerse = new ChapVerse();
            chapVerse.chap = Integer.valueOf(chapVerseStrings[0]);
            if(chapVerseStrings.length == 2){
                chapVerse.verse = Integer.valueOf(chapVerseStrings[1]);
            }
            chapVersePath.add(chapVerse, i);
        }
        return chapVersePath;
    }

    private static boolean shouldOverrideParsha() {
        boolean isBeforeIsraelMeetsUS = false;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/mm/yyyy");
            Date strDate = sdf.parse("12/05/2018");
            Calendar dayIsraelMeetsUS = new GregorianCalendar();
            dayIsraelMeetsUS.setTime(strDate);
            isBeforeIsraelMeetsUS = Calendar.getInstance().after(dayIsraelMeetsUS);
            if (isBeforeIsraelMeetsUS) {
                String data = API.getDataFromURL("http://ip-api.com/json");
                JSONObject response = new JSONObject(data);
                return response.getString("countryCode").equals("IL");
            } else {
                return false;
            }
        } catch (API.APIException e) {
            return false;
        } catch (JSONException e) {
            return false;
        } catch (ParseException e) {
            return false;
        }

    }

    public static MenuDirectRef [] getParsha(Context context){
        String todaysDate = getLongDate(1);
        boolean shouldOverrideParsha = shouldOverrideParsha();

        JSONArray weeks = null;
        //todaysDate = "12-May-2018"; //fake date for testing
        try {
            //Log.d("DailyLearning", "getParsha..today: "+ todaysDate);
            weeks = Util.openJSONArrayFromAssets("calendar/parshiot.json");

            for (int i = 0; i < weeks.length(); i++) {
                JSONObject week = weeks.getJSONObject(i);
                if(week.getString("date").equals(todaysDate)){
                    String parsha = week.getString("parasha");
                    String [] multiParshas = parsha.split("-"); // If it's a double parsha only get the first
                    String aliyah = week.getJSONArray("aliyot").getString(0);
                    if (shouldOverrideParsha && parsha.equals("Emor")) {
                        parsha = "Behar";
                        multiParshas = parsha.split("-");
                        aliyah = "Leviticus 25:1-25:18";
                    } else if (shouldOverrideParsha && parsha.equals("Behar-Bechukotai")) {
                        parsha = "Bechukotai";
                        multiParshas = parsha.split("-");
                        aliyah = "Leviticus 26:3-26:5";
                    }

                    String bookName = aliyah.replaceFirst("\\s[0-9]+.*$", "");
                    Book book = new Book(bookName);
                    Node node = getParshaFromSefer(book, multiParshas[0]);
                    Settings.BookSettings bookSettings = Settings.BookSettings.getSavedBook(book);
                    Integer textNum = null;
                    if(bookSettings.node != null) {
                        String currSavedPlaceParshaName = bookSettings.node.getParent().getTitle(Util.Lang.EN);
                        if (currSavedPlaceParshaName.equals(node.getParent().getTitle(Util.Lang.EN))
                                || (multiParshas.length == 2 && currSavedPlaceParshaName.equals(multiParshas[1]))) {
                            //The saved stop is actually in the same parsha.
                            // So lets just go to the saved spot in the book

                            node = bookSettings.node;
                            textNum = bookSettings.textNum;
                        }
                    }
                    String heTitle;
                    if(multiParshas.length == 1){
                        heTitle = node.getParent().getTitle(Util.Lang.HE);
                    }else{
                        Node secondParshaNode = getParshaFromSefer(book, multiParshas[1]);
                        heTitle = node.getParent().getTitle(Util.Lang.HE)
                                + "-" + secondParshaNode.getParent().getTitle(Util.Lang.HE);
                    }
                    MenuDirectRef parshaMenu = new MenuDirectRef(
                            context,
                            parsha, // Just show whatever the parsha value says
                            heTitle,
                            node.makePathDefiningNode(),
                            book,
                            "Parsha",
                            textNum
                            );


                    JSONArray haftaras = week.getJSONArray("haftara");
                    //maybe also use: "shabbat_name": "Shabbat HaGadol"
                    //Log.d("DailyLearning", "this weeks parsha: " + parsha + "");

                    //TODO deal with multi part haftara (have a binder with multi part haf)
                    String fullHaftaraStr = "";
                    String heFullHaftaraStr = "";
                    Book firstHaftaraBook = null;
                    Node firstHaftaraNode = null;
                    Integer haftaraTextNum = null;
                    for(int j = 0; j < haftaras.length(); j++){
                        String haftara = haftaras.getString(j);
                        String haftaraBookName = haftara.replaceFirst("\\s[0-9]+.*$","");
                        String haftaraFullNumber = haftara.replaceFirst("^[^0-9]+\\s", "");
                        ChapVersePath chapVersePath = getChapVerses(haftaraFullNumber);
                        Book haftaraBook = new Book(haftaraBookName);
                        Node haftaraNode = haftaraBook.getTOCroots().get(0);
                        haftaraNode = haftaraNode.getChildren().get(chapVersePath.start.chap - 1);
                        if(j == 0) { // only going to make link go there
                            firstHaftaraBook = haftaraBook;
                            firstHaftaraNode = haftaraNode;
                            haftaraTextNum = chapVersePath.start.verse - 1;
                        }else{ // add commas from more than one
                            fullHaftaraStr += ", ";
                            heFullHaftaraStr += ", ";
                        }

                        if(fullHaftaraStr.contains(haftaraBookName)){
                            //I know that there is never A, B, A (so I don't have to worry about something else in the mid
                            fullHaftaraStr += haftaraFullNumber;
                            heFullHaftaraStr += chapVersePath.toHeString();
                        }else{
                            fullHaftaraStr += haftara;
                            heFullHaftaraStr += haftaraBook.getTitle(Util.Lang.HE) + " " + chapVersePath.toHeString();
                        }
                    }

                    //TODO maybe check  that it's correct in case we're missing a chap (but that's unlikely to happen in Tanach).
                    //+ ": " + haftaraBookName + " " + haftaraFullNumber
                    /*
                    "Haftara: " + fullHaftaraStr,
                        "הפטרה: " + heFullHaftaraStr,
                     */
                    MenuDirectRef haftaraMenu = new MenuDirectRef(
                            context,
                            fullHaftaraStr,
                            heFullHaftaraStr,
                            firstHaftaraNode.makePathDefiningNode(),
                            firstHaftaraBook,
                            "",
                            haftaraTextNum
                    );
                    return new MenuDirectRef [] {parshaMenu, haftaraMenu};
                }


            }
        } catch (Exception e) {
            Log.e("DailyLearning", "getParsha Problems");
            e.printStackTrace();
        }
        return new MenuDirectRef[]{};
    }

    public static List<MenuDirectRef> getDailyLearnings(Context context){
        //Log.d("DailyLearning", "starting get dailyLearning");
        List<MenuDirectRef> dailyLearnings = new ArrayList<>();

        for(MenuDirectRef menuDirectRef:getParsha(context)) {
            dailyLearnings.add(menuDirectRef);
        }

        MenuDirectRef dafMenu = getDafYomi(context);
        if(dafMenu !=null){
            dailyLearnings.add(dafMenu);
        }

        return dailyLearnings;
    }

    static final String [] months = new String[] {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
    private static String getLongDate(int type){
        Calendar c = Calendar.getInstance();
        String date;
        if(type == 0)
            date= (c.get(Calendar.MONTH)+1) + "/" + c.get(Calendar.DAY_OF_MONTH) + "/" + c.get(Calendar.YEAR);
        else { // if (type==1)
            c.add(Calendar.DAY_OF_MONTH,(7-c.get(Calendar.DAY_OF_WEEK)));
            date =  c.get(Calendar.DAY_OF_MONTH) + "-" + months[c.get(Calendar.MONTH)] + "" + "-" + c.get(Calendar.YEAR);
            if(c.get(Calendar.DAY_OF_MONTH) <10)
                date = "0" + date;
        }
        return date;
    }
}
