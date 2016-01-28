package org.sefaria.sefaria.database;


import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sefaria.sefaria.MenuElements.MenuDirectRef;
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

    private static Node getDafYomi(){
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
                            return node;

                        }
                    }
                    return node;
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Node();
    }

    public static MenuDirectRef [] getParsha(Context context){
        String todaysDate = getLongDate(1);
        Log.d("date",todaysDate);
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
                    String bookName = aliyah.replaceFirst("\\s[0-9]*.*$", "");
                    Book book = new Book(bookName);
                    Node node = book.getTOCroots().get(1);
                    for(Node child:node.getChildren()){
                        if(child.getTitle(Util.Lang.EN).equals(parsha)){
                            node = child;
                            break;
                        }
                    }
                    node = node.getChildren().get(Calendar.getInstance().DAY_OF_WEEK -1);
                    MenuDirectRef menuDirectRef = new MenuDirectRef(context,bookName,book.heTitle+"parshaHETODO",node.makePathDefiningNode(),book);
                    if(true)
                        return new MenuDirectRef [] {menuDirectRef};

                    String haftaraBookName = haftara.replaceFirst("\\s[0-9]*.*$","");
                    String haftaraNumber = haftara.replaceFirst("^[^0-9]*\\s","").replaceFirst("-[0-9]*.*$", "");
                    Book haftaraBook = new Book(haftaraBookName);
                    Node haftaraNode = haftaraBook.getTOCroots().get(0);
                    int haftraChap = Integer.valueOf(haftaraNumber.split(":")[0]);
                    haftaraNode = haftaraNode.getChildren().get(haftraChap-1);
                    //TODO maybe check  that it's correct incase we're missing a chap (but that's unlikely to happen in Tanach).

                    break;
                }


            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new MenuDirectRef[]{};
    }

    public static List<MenuDirectRef> getDailyLearnings(Context context){
        List<MenuDirectRef> dailyLearnings = new ArrayList<>();
        getDafYomi().log();

        dailyLearnings.add(getParsha(context)[0]);


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
        }
        return date;
    }
}
