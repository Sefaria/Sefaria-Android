package org.sefaria.sefaria.database;


import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sefaria.sefaria.MenuElements.MenuDirectRef;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
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
                    MenuDirectRef menuDirectRef = new MenuDirectRef(context,todayDaf,book.heTitle + " " + node.getNiceGridNum(Util.Lang.HE),node.makePathDefiningNode(),book, "Daf Yomi");
                    return menuDirectRef;
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static MenuDirectRef [] getParsha(Context context){
        String todaysDate = getLongDate(1);
        JSONArray weeks = null;
        try {
            weeks = Util.openJSONArrayFromAssets("calendar/parshiot.json");

            for (int i = 0; i < weeks.length(); i++) {
                JSONObject week = weeks.getJSONObject(i);

                if(week.getString("date").equals(todaysDate)){
                    String parsha = week.getString("parasha");
                    JSONArray haftaras = week.getJSONArray("haftara");
                    //TODO deal with multi part hafotra
                    String haftara = haftaras.getString(0);
                    String aliyah = week.getJSONArray("aliyot").getString(0);
                    String bookName = aliyah.replaceFirst("\\s[0-9]+.*$", "");
                    Book book = new Book(bookName);
                    Node node = book.getTOCroots().get(1);
                    for(Node child:node.getChildren()){
                        if(child.getTitle(Util.Lang.EN).equals(parsha)){
                            node = child;
                            break;
                        }
                    }
                    node = node.getFirstDescendant();//go to first aliyah
                    MenuDirectRef parshaMenu = new MenuDirectRef(context,node.getParent().getTitle(Util.Lang.EN),node.getParent().getTitle(Util.Lang.HE),node.makePathDefiningNode(),book, "Parsha");

                    String haftaraBookName = haftara.replaceFirst("\\s[0-9]+.*$","");
                    String haftaraFullNumber = haftara.replaceFirst("^[^0-9]+\\s","");
                    String haftaraNumber = haftaraFullNumber.replaceFirst("-[0-9]+.*$", "");
                    Book haftaraBook = new Book(haftaraBookName);
                    Node haftaraNode = haftaraBook.getTOCroots().get(0);
                    int haftraChap = Integer.valueOf(haftaraNumber.split(":")[0]);
                    haftaraNode = haftaraNode.getChildren().get(haftraChap-1);
                    //TODO maybe check  that it's correct incase we're missing a chap (but that's unlikely to happen in Tanach).

                    //+ ": " + haftaraBookName + " " + haftaraFullNumber
                    MenuDirectRef haftaraMenu = new MenuDirectRef(context,"Haftara","הפטרה",haftaraNode.makePathDefiningNode(),haftaraBook,"");
                    return new MenuDirectRef [] {parshaMenu,haftaraMenu};
                }


            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new MenuDirectRef[]{};
    }

    public static List<MenuDirectRef> getDailyLearnings(Context context){
        List<MenuDirectRef> dailyLearnings = new ArrayList<>();
        MenuDirectRef dafMenu = getDafYomi(context);
        if(dafMenu !=null){
            dailyLearnings.add(dafMenu);
        }

        for(MenuDirectRef menuDirectRef:getParsha(context)) {
            dailyLearnings.add(menuDirectRef);
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
