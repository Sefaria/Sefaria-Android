package org.sefaria.sefaria.activities;


import android.app.Activity;
import android.content.Intent;
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

import org.sefaria.sefaria.database.Recents;
import org.sefaria.sefaria.layouts.HomeActionbar;
import org.sefaria.sefaria.MenuElements.MenuDirectRef;
import org.sefaria.sefaria.MenuElements.MenuGrid;
import org.sefaria.sefaria.MenuElements.MenuState;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Settings;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.database.Book;
import org.sefaria.sefaria.database.DailyLearning;
import org.sefaria.sefaria.layouts.SefariaTextView;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends android.support.v4.app.Fragment {

    private final int NUM_COLUMNS = 1;
    private final boolean LIMIT_GRID_SIZE = true;

    private Activity activity;
    private MenuGrid menuGrid;
    private MenuState menuState;
    private List<MenuDirectRef> dailyLearnings;
    private List<MenuDirectRef> recentTexts;
    private LinearLayout recentRoot;
    private HomeActionbar homeActionbar;
    private View ThisView;

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
        //Log.d("HomeFrag", "onCreate");
        //setTheme(Settings.getTheme());
        //setContentView(R.layout.fragment_home);

        menuState = new MenuState(MenuState.IndexType.MAIN);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        ThisView = view;
        LinearLayout homeRoot = (LinearLayout) view.findViewById(R.id.homeRoot);
        addMenuGrid(view);
        //addRecentTexts(view); //done in on HomeFrag open
        addCalendar(view);


        //just extra spacing for the bottom
        homeRoot.addView(createTypeTitle("",false));


        LinearLayout abRoot = (LinearLayout) view.findViewById(R.id.actionbarRoot);
        //CustomActionbar cab = new CustomActionbar(activity,new MenuNode("Sefaria","ספאריה",null),
        //        Settings.getSystemLang(),null,null,closeClick,searchClick,null,menuClick,null,-1);
        //abRoot.addView(cab);
        homeActionbar = new HomeActionbar(getContext(),Settings.getMenuLang(),searchClick,langClick);
        abRoot.addView(homeActionbar);

        //set letter spacing
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            SefariaTextView recentTV = (SefariaTextView) view.findViewById(R.id.recentTextsTV);
            SefariaTextView browseTV = (SefariaTextView) view.findViewById(R.id.browseTV);
            SefariaTextView calendarTV = (SefariaTextView) view.findViewById(R.id.calendarTV);
            SefariaTextView supportTV = (SefariaTextView) view.findViewById(R.id.supportTV);
            SefariaTextView donateTV = (SefariaTextView) view.findViewById(R.id.donateTV);

            recentTV.setLetterSpacing(0.1f);
            browseTV.setLetterSpacing(0.1f);
            calendarTV.setLetterSpacing(0.1f);
            supportTV.setLetterSpacing(0.1f);
            donateTV.setLetterSpacing(0.1f);
        }




        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        menuGrid.setLang(menuGrid.getLang());
        Log.d("HomeFrag", "onActivityResult");
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);

        //out.putParcelable("menuState", menuState);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.d("HomeFrag", "onAttach");

        this.activity = activity;

    }

    public void onHomeFragClose(){
        menuGrid.closeMoreClick();
    }

    public void onHomeFragOpen(){
        addRecentTexts(ThisView);
    }

    @Override
    public void onResume() {
        super.onResume();
        //Log.d("HomeFrag","onresume");
        addRecentTexts(ThisView);
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


    private void addRecentTexts(View view){
        Log.d("homeFrag","adding recent Texts");
        //Recent Texts

        if(recentRoot == null) {
            recentRoot = (LinearLayout) view.findViewById(R.id.recentRoot);
        }
        else{
            recentRoot.removeAllViews();
        }
        final int columnNum = 1;
        List<MenuDirectRef> recents = Recents.getRecentDirectMenu(getContext(),true, false);
        recentTexts = new ArrayList<>();
        if(recents.size()>0) {
            LinearLayout recentRow = null;
            for (int i=0;i<recents.size();i++){
                if(i%columnNum  == 0){
                    recentRow = new LinearLayout(getContext());
                    recentRow.setOrientation(LinearLayout.HORIZONTAL);
                    recentRow.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
                    recentRoot.addView(recentRow);
                }
                MenuDirectRef menuDirectRef = recents.get(i);
                recentTexts.add(menuDirectRef);
                recentRow.addView(menuDirectRef);
            }
        } else {
            view.findViewById(R.id.recentTextsTV).setVisibility(View.GONE);
        }

    }




    private void addMenuGrid(View view){
        //Menu grid
        Util.Lang menuLang = Settings.getMenuLang();
        menuGrid = new MenuGrid(getContext(),NUM_COLUMNS, menuState,LIMIT_GRID_SIZE,menuLang);
        LinearLayout gridRoot = (LinearLayout) view.findViewById(R.id.gridRoot);
        gridRoot.addView(menuGrid);
    }

    public void setLang(Util.Lang lang){
        if(lang == Util.Lang.BI) {
            lang = Util.Lang.EN;
        }
        menuState.setLang(lang);
        menuGrid.setLang(lang);
        homeActionbar.setLang(lang);
        //not setting cab, b/c it should stay as the SystemLang
        for(MenuDirectRef menuDirectRef:dailyLearnings)
            menuDirectRef.setLang(lang);

        for(MenuDirectRef menuDirectRef:recentTexts){
            menuDirectRef.setLang(lang);
        }

    }

    private void addCalendar(View view){
        //Calendar
        LinearLayout calendarRoot = (LinearLayout) view.findViewById(R.id.calendarRoot);
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

    //onclick listener (see xml)
    public void donateNode(View view) {


    }

}
