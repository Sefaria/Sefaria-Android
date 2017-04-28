package org.sefaria.sefaria.database;

import android.content.Context;
import android.util.Pair;

import org.sefaria.sefaria.MenuElements.MenuDirectRef;
import org.sefaria.sefaria.Settings;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by LenJ on 9/27/2016.
 */
public class Recents {

    public Recents(){
        ;
    }
    static public List<MenuDirectRef> getRecentDirectMenu(Context context, boolean setLongClickPinning, boolean getAllRecents){
        int recentsCount = 3;
        if(getAllRecents)
            recentsCount = 0;
        List<String> recentBooks = Settings.RecentTexts.getRecentTexts(recentsCount);
        List<MenuDirectRef> recents = new ArrayList<>();
        for(String title: recentBooks){
            try {
                Book book = new Book(title);
                Pair<String, String> pair = Settings.BookSettings.getSavedBookTitle(title);
                MenuDirectRef menuDirectRef = new MenuDirectRef(context, pair.first, pair.second, null, book, null, null);
                if(setLongClickPinning)
                    menuDirectRef.setLongClickPinning();
                recents.add(menuDirectRef);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return recents;
    }
}
