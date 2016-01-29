package org.sefaria.sefaria.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import org.sefaria.sefaria.MenuElements.MenuDirectRef;
import org.sefaria.sefaria.Settings;
import org.sefaria.sefaria.database.API;
import org.sefaria.sefaria.database.Book;
import org.sefaria.sefaria.database.DailyLearning;
import org.sefaria.sefaria.database.Downloader;
import org.sefaria.sefaria.layouts.AutoResizeTextView;
import org.sefaria.sefaria.layouts.CustomActionbar;
import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.MenuElements.MenuGrid;
import org.sefaria.sefaria.MenuElements.MenuNode;
import org.sefaria.sefaria.MenuElements.MenuState;

import android.view.Gravity;
import android.view.View;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends Activity {

    private final int NUM_COLUMNS = 3;
    private final boolean LIMIT_GRID_SIZE = true;

    private MenuGrid menuGrid;
    private MenuState menuState;
    private boolean isPopup;
    private List<MenuDirectRef> dailtylearnings;
    private List<MenuDirectRef> recentTexts;

    @Override
    protected void onCreate(Bundle in) {
        super.onCreate(in);
        MyApp.currActivityContext = this;
        setContentView(R.layout.activity_home);

        Intent intent = getIntent();
        menuState = intent.getParcelableExtra("menuState");
        isPopup = intent.getBooleanExtra("isPopup",false);

        if (in != null) {
            menuState = in.getParcelable("menuState");
        }

        init();

    }

    private void init() {

        if (menuState == null) {
            menuState = new MenuState();
        }

        ScrollView gridRoot = (ScrollView) findViewById(R.id.gridRoot);
        LinearLayout homeRoot = new LinearLayout(this);
        homeRoot.setOrientation(LinearLayout.VERTICAL);
        homeRoot.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        homeRoot.setGravity(Gravity.CENTER);
        gridRoot.addView(homeRoot);

        addHeader(homeRoot);
        addMenuGrid(homeRoot);
        addRecentTexts(homeRoot);
        addCalendar(homeRoot);



        //toggle closeClick, depending on if menu is popup or not
        View.OnClickListener tempCloseClick = null;
        //if (isPopup) tempCloseClick = closeClick; //Removing the close click for now to test without it


        LinearLayout abRoot = (LinearLayout) findViewById(R.id.actionbarRoot);
        CustomActionbar cab = new CustomActionbar(this,new MenuNode("Sefaria","ספאריה",null),
                Settings.getSystemLang(),null,tempCloseClick,null,null,menuClick,null,-1);
        abRoot.addView(cab);




        if(API.useAPI()) {
            Toast.makeText(this, "Starting Download", Toast.LENGTH_SHORT).show();
            Downloader.updateLibrary(this);
        }

    }

    private void addHeader(LinearLayout homeRoot){
        //Living Library
        TextView livingLibaryView = createTypeTitle("A Living Library of Jewish Texts");
        livingLibaryView.setTextSize(20);
        int livingPadding = 60;
        livingLibaryView.setPadding(3, livingPadding, 3, livingPadding);
        livingLibaryView.setTextColor(Color.parseColor("#000000"));
        homeRoot.addView(livingLibaryView);
    }

    private void addRecentTexts(LinearLayout homeRoot){
        //Recent Texts
        /*
        GridLayout gridLayout = new GridLayout(this);
        gridLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        final int numberOfRecentText = 6;
        final int numColumns = 2;
        gridLayout.setRowCount((int) Math.ceil(numberOfRecentText / numColumns));
        gridLayout.setColumnCount(numColumns);
        */

        //homeRoot.addView(gridLayout);
        LinearLayout recentRoot = new LinearLayout(this);
        recentRoot.setOrientation(LinearLayout.HORIZONTAL);
        recentRoot.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        List<String> recentBooks = Settings.getRecentTexts();
        recentTexts = new ArrayList<>();
        if(recentBooks.size()>0) {
            homeRoot.addView(createTypeTitle("Recent Texts"));
            homeRoot.addView(recentRoot);
            for (String bookTitle : recentBooks) {
                Book book = null;
                try {
                    book = new Book(bookTitle);
                } catch (Book.BookNotFoundException e) {
                    e.printStackTrace();
                }
                MenuDirectRef menuDirectRef = new MenuDirectRef(this, bookTitle, book.heTitle, null, book);
                recentTexts.add(menuDirectRef);
                recentRoot.addView(menuDirectRef);
            }
        }
    }

    private void addMenuGrid(LinearLayout homeRoot){
        //Menu grid
        Util.Lang menuLang = Settings.getMenuLang();
        menuGrid = new MenuGrid(this,NUM_COLUMNS, menuState,LIMIT_GRID_SIZE,menuLang);
        homeRoot.addView(createTypeTitle("Browse Texts"));
        homeRoot.addView(menuGrid);
    }

    private void setLang(Util.Lang lang){
        if(lang == Util.Lang.BI) {
            lang = Util.Lang.EN;
        }
        menuState.setLang(lang);
        menuGrid.setLang(lang);
        //not setting cab, b/c it should stay as the SystemLang
        for(MenuDirectRef menuDirectRef:dailtylearnings)
            menuDirectRef.setLang(lang);

        for(MenuDirectRef menuDirectRef:recentTexts){
            menuDirectRef.setLang(lang);
        }

    }

    private void addCalendar(LinearLayout homeRoot){
        //Calendar
        LinearLayout calendarRoot = new LinearLayout(this);
        calendarRoot.setOrientation(LinearLayout.HORIZONTAL);
        calendarRoot.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        homeRoot.addView(createTypeTitle("Calendar"));
        homeRoot.addView(calendarRoot);
        dailtylearnings = DailyLearning.getDailyLearnings(this);
        for(MenuDirectRef menuDirectRef: dailtylearnings)
            calendarRoot.addView(menuDirectRef);
    }

    private TextView createTypeTitle(String title){
        TextView textView = new TextView(this);
        textView.setText(title);
        final int paddingSide= 3;
        final int paddingTop = 20;
        textView.setPadding(paddingSide,paddingTop*2,paddingSide,paddingTop);
        textView.setTextSize(20);
        textView.setGravity(Gravity.CENTER);

        return textView;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        menuGrid.setLang(menuGrid.getLang());
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);

        out.putParcelable("menuState", menuState);
    }

    View.OnClickListener searchClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
        }
    };

    View.OnClickListener closeClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            finish();
        }
    };

    View.OnClickListener menuClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            setLang(Settings.switchMenuLang());
        }
    };


}
