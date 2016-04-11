package org.sefaria.sefaria.activities;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.sefaria.sefaria.BuildConfig;
import org.sefaria.sefaria.GoogleTracker;
import org.sefaria.sefaria.HomeActionbar;
import org.sefaria.sefaria.MenuElements.MenuDirectRef;
import org.sefaria.sefaria.MenuElements.MenuGrid;
import org.sefaria.sefaria.MenuElements.MenuNode;
import org.sefaria.sefaria.MenuElements.MenuState;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Settings;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.database.Book;
import org.sefaria.sefaria.database.DailyLearning;
import org.sefaria.sefaria.database.Database;
import org.sefaria.sefaria.database.Huffman;
import org.sefaria.sefaria.database.Searching;
import org.sefaria.sefaria.layouts.CustomActionbar;
import org.sefaria.sefaria.layouts.SefariaTextView;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends android.support.v4.app.Fragment {

    private final int NUM_COLUMNS = 2;
    private final boolean LIMIT_GRID_SIZE = true;

    private Activity activity;
    private MenuGrid menuGrid;
    private MenuState menuState;
    private List<MenuDirectRef> dailyLearnings;
    private List<MenuDirectRef> recentTexts;
    private LinearLayout recentRoot;
    private HomeActionbar homeActionbar;

    public static HomeFragment newInstance() {

        Bundle args = new Bundle();

        HomeFragment fragment = new HomeFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public HomeFragment() {

    }

    @Override
    public void onCreate(Bundle in) {
        super.onCreate(in);
        //setTheme(Settings.getTheme());
        //setContentView(R.layout.fragment_home);

        menuState = new MenuState();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        LinearLayout gridRoot = (LinearLayout) view.findViewById(R.id.gridRoot);
        LinearLayout homeRoot = new LinearLayout(activity);
        homeRoot.setOrientation(LinearLayout.VERTICAL);
        homeRoot.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        homeRoot.setGravity(Gravity.CENTER);
        gridRoot.addView(homeRoot);

        //Don't mention the living library thing unless you're actually at the home screen
        //addHeader(homeRoot);
        addMenuGrid(homeRoot);
        addRecentTexts(homeRoot);
        addCalendar(homeRoot);


        //just extra spacing for the bottom
        homeRoot.addView(createTypeTitle("",false));


        LinearLayout abRoot = (LinearLayout) view.findViewById(R.id.actionbarRoot);
        //CustomActionbar cab = new CustomActionbar(activity,new MenuNode("Sefaria","ספאריה",null),
        //        Settings.getSystemLang(),null,null,closeClick,searchClick,null,menuClick,null,-1);
        //abRoot.addView(cab);
        homeActionbar = new HomeActionbar(getContext(),Settings.getMenuLang(),searchClick,langClick);
        abRoot.addView(homeActionbar);
        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        this.activity = activity;

    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    /*private void addHeader(LinearLayout homeRoot){
        //Living Library
        TextView livingLibraryView = createTypeTitle("A Living Library of Jewish Texts",true);
        livingLibraryView.setTextSize(20);
        int livingPadding = 60;
        livingLibraryView.setPadding(3, livingPadding, 3, livingPadding);
        livingLibraryView.setTextColor(Util.getColor(getContext(), R.attr.text_color_main));
        homeRoot.addView(livingLibraryView);
    }*/

    private void addRecentTexts(LinearLayout homeRoot){
        //Recent Texts
        if(recentRoot == null) {
            recentRoot = new LinearLayout(getContext());
            recentRoot.setOrientation(LinearLayout.VERTICAL);
            recentRoot.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
            homeRoot.addView(recentRoot);
        }
        else{
            recentRoot.removeAllViews();
        }
        final int columNum = 2;
        List<String> recentBooks = Settings.RecentTexts.getRecentTexts();
        recentTexts = new ArrayList<>();
        if(recentBooks.size()>0) {
            recentRoot.addView(createTypeTitle("Recent Texts",false));
            LinearLayout recentRow = null;
            for (int i=0;i<recentBooks.size();i++){
                if(i%columNum  == 0){
                    recentRow = new LinearLayout(getContext());
                    recentRow.setOrientation(LinearLayout.HORIZONTAL);
                    recentRow.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
                    recentRoot.addView(recentRow);
                }

                String bookTitle = recentBooks.get(i);
                Book book = null;
                try {
                    book = new Book(bookTitle);
                    Pair<String,String> pair = Settings.BookSettings.getSavedBookTitle(bookTitle);
                    MenuDirectRef menuDirectRef = new MenuDirectRef(getContext(), pair.first, pair.second, null, book, null);
                    menuDirectRef.setLongClickPinning();
                    recentTexts.add(menuDirectRef);
                    recentRow.addView(menuDirectRef);
                } catch (Exception e) {
                    Log.e("HomeActivity", "Problem getting Recent Texts:" + e.getMessage());
                }

            }
        }

    }




    private void addMenuGrid(LinearLayout homeRoot){
        //Menu grid
        Util.Lang menuLang = Settings.getMenuLang();
        menuGrid = new MenuGrid(getContext(),NUM_COLUMNS, menuState,LIMIT_GRID_SIZE,menuLang);
        homeRoot.addView(createTypeTitle("Browse Texts",false));
        homeRoot.addView(menuGrid);
    }

    private void setLang(Util.Lang lang){
        if(lang == Util.Lang.BI) {
            lang = Util.Lang.EN;
        }
        menuState.setLang(lang);
        menuGrid.setLang(lang);
        //not setting cab, b/c it should stay as the SystemLang
        for(MenuDirectRef menuDirectRef:dailyLearnings)
            menuDirectRef.setLang(lang);

        for(MenuDirectRef menuDirectRef:recentTexts){
            menuDirectRef.setLang(lang);
        }

    }

    private void addCalendar(LinearLayout homeRoot){
        //Calendar
        LinearLayout calendarRoot = new LinearLayout(getContext());
        calendarRoot.setOrientation(LinearLayout.HORIZONTAL);
        calendarRoot.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        homeRoot.addView(createTypeTitle("Calendar", false));
        homeRoot.addView(calendarRoot);
        dailyLearnings = DailyLearning.getDailyLearnings(getContext());
        for(MenuDirectRef menuDirectRef: dailyLearnings) {
            calendarRoot.addView(menuDirectRef);
        }
    }

    private TextView createTypeTitle(String title, boolean isSerif){
        SefariaTextView textView = new SefariaTextView(getContext());
        textView.setText(title);
        final int paddingSide= 3;
        final int paddingTop = 20;
        textView.setPadding(paddingSide, paddingTop * 2, paddingSide, paddingTop);
        textView.setTextSize(20);
        textView.setFont(Util.Lang.EN, isSerif); //TODO change with system lang
        textView.setTextColor(Util.getColor(getContext(), R.attr.text_color_english));
        textView.setGravity(Gravity.CENTER);
        if (! isSerif && Build.VERSION.SDK_INT > 14) {
            textView.setAllCaps(true);
        }

        return textView;
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        menuGrid.setLang(menuGrid.getLang());
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);

        //out.putParcelable("menuState", menuState);
    }

    View.OnClickListener searchClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            Intent intent = new Intent(getContext(),SearchActivity.class);
            startActivity(intent);
        }
    };

    View.OnClickListener langClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Util.Lang newLang = Settings.switchMenuLang();
            setLang(newLang);
            homeActionbar.setLang(newLang);
        }
    };

}
