package org.sefaria.sefaria.MenuElements;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.sefaria.sefaria.BilingualNode;
import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Settings;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.activities.MenuActivity;
import org.sefaria.sefaria.activities.SuperTextActivity;
import org.sefaria.sefaria.activities.TOCActivity;
import org.sefaria.sefaria.database.Book;
import org.sefaria.sefaria.database.Database;
import org.sefaria.sefaria.database.Recents;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by LenJ on 9/27/2016.
 */
public class MenuDirectRefsGrid extends LinearLayout{
    private Context context;
    private int numColumns;

    //the LinearLayout which contains the actual grid of buttons, as opposed to the tabs
    //which is useful for destroying the page and switching to a new tab
    private LinearLayout gridRoot;
    private List<MenuDirectRef> allMenuDirectRefs;

    private boolean flippedForHe; //are the views flipped for hebrew
    public MenuDirectRefsGrid(Context context, int numColumns, List<MenuDirectRef> menuDirectRefs) {
        super(context);
        this.allMenuDirectRefs = menuDirectRefs;
        this.context = context;
        this.numColumns = numColumns;

        init();
    }

    private void init() {
        this.setOrientation(LinearLayout.VERTICAL);
        this.setPadding(10, 10, 10, 10);
        this.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        this.gridRoot = new LinearLayout(context);
        gridRoot.setOrientation(LinearLayout.VERTICAL);
        gridRoot.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        gridRoot.setGravity(Gravity.CENTER);
        this.removeAllViews();
        this.addView(gridRoot);

        this.flippedForHe = false;


        int currNodeIndex = 0;

        for (int i = 0; i <= Math.ceil(allMenuDirectRefs.size()/numColumns) && currNodeIndex < allMenuDirectRefs.size(); i++) {
            LinearLayout ll = addRow(gridRoot);
            for (int j = 0; j < numColumns && currNodeIndex < allMenuDirectRefs.size();  j++) {
                ll.removeViewAt(j);
                MenuDirectRef menuDirectRef = allMenuDirectRefs.get(currNodeIndex);
                menuDirectRef.setPadding(10,0,10,0);
                menuDirectRef.setTVGravity(Gravity.CENTER);

                ll.addView(menuDirectRef, j);
                currNodeIndex++;
            }
        }

        if (Settings.getMenuLang() == Util.Lang.HE) {
            flippedForHe = true;
            flipViews(true);
        }
    }

    public void setNewList(List<MenuDirectRef> menuDirectRefs){
        allMenuDirectRefs = menuDirectRefs;
        init();
    }


    private LinearLayout addRow(LinearLayout parentView) {
        LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.HORIZONTAL);
        //ll.setPadding(10,10,10,10);
        ll.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        ll.setGravity(Gravity.CENTER);
        ll.setHorizontalGravity(Gravity.CENTER_HORIZONTAL);
        for (int i = 0; i < numColumns; i++) {
            MenuButton menuButton = new MenuButton(context);
            ll.addView(menuButton);
        }
        parentView.addView(ll);
        return ll;
    }




    public void setLang(Util.Lang lang) {
        if ((lang == Util.Lang.HE && !flippedForHe) ||
                (lang == Util.Lang.EN && flippedForHe)) {

            flippedForHe = (lang == Util.Lang.HE);
            flipViews(true);
        }


        for (MenuDirectRef menuDirectRef : allMenuDirectRefs) {
            menuDirectRef.setLang(lang);
        }

    }

    //used when switching languages or building a hebrew page
    //reverse the order of every row in the grid
    private void flipViews(boolean flipTabsAlso) {

        int numChildren;

        for (int i = 0; i < gridRoot.getChildCount(); i++) {
            View v = gridRoot.getChildAt(i);
            if (v instanceof LinearLayout) {
                LinearLayout ll = (LinearLayout) v;
                numChildren = ll.getChildCount();
                for (int j = 0; j < numChildren - 1; j++) {
                    View tempView = ll.getChildAt(numChildren - 1);
                    ll.removeViewAt(numChildren - 1);
                    ll.addView(tempView, j);
                }


            }
        }
    }
}
