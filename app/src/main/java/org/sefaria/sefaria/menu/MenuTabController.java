package org.sefaria.sefaria.menu;

import android.os.Bundle;

import java.util.ArrayList;

/**
 * Created by nss on 9/15/15.
 */
public class MenuTabController {

    private static ArrayList<MenuState> tabs;
    private static boolean wasStateRestored = false;

    //returns the new tab's number
    public static int addTab(MenuState menuState) {
        if (tabs == null) tabs = new ArrayList<>();

        tabs.add(menuState);
        return tabs.size()-1;
    }

    public static void removeTab(MenuState menuState) { tabs.remove(menuState); }

    public static MenuState getTab(int pos) { return tabs.get(pos); }

    public static void saveState(Bundle out) {
        wasStateRestored = false;
        out.putParcelableArrayList("MTCTabList", tabs);
    }

    public static void restoreState(Bundle in) {
        if (!wasStateRestored) {

            tabs = in.getParcelableArrayList("MTCTabList");
            wasStateRestored = true;
        }
    }
}
