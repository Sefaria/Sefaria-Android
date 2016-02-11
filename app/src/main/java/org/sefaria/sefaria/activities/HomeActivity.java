package org.sefaria.sefaria.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;

import org.sefaria.sefaria.BuildConfig;
import org.sefaria.sefaria.GoogleTracker;
import org.sefaria.sefaria.MenuElements.MenuDirectRef;
import org.sefaria.sefaria.Settings;
import org.sefaria.sefaria.database.API;
import org.sefaria.sefaria.database.Book;
import org.sefaria.sefaria.database.DailyLearning;
import org.sefaria.sefaria.database.Database;
import org.sefaria.sefaria.database.Downloader;
import org.sefaria.sefaria.database.Huffman;
import org.sefaria.sefaria.layouts.AutoResizeTextView;
import org.sefaria.sefaria.layouts.CustomActionbar;
import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.MenuElements.MenuGrid;
import org.sefaria.sefaria.MenuElements.MenuNode;
import org.sefaria.sefaria.MenuElements.MenuState;

import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class HomeActivity extends Activity {

    private final int NUM_COLUMNS = 3;
    private final boolean LIMIT_GRID_SIZE = true;

    private MenuGrid menuGrid;
    private MenuState menuState;
    private boolean isPopup;
    private List<MenuDirectRef> dailtylearnings;
    private List<MenuDirectRef> recentTexts;
    private LinearLayout recentRoot;

    @Override
    protected void onCreate(Bundle in) {
        super.onCreate(in);
        MyApp.currActivityContext = this;
        setContentView(R.layout.activity_home);
        Huffman.makeTree(true);

        Intent intent = getIntent();
        menuState = intent.getParcelableExtra("menuState");
        isPopup = intent.getBooleanExtra("isPopup",false);

        if (in != null) {
            menuState = in.getParcelable("menuState");
        }

        init();

    }

    private boolean veryFirstTime = true;

    @Override
    protected void onResume() {
        super.onResume();
        if(!veryFirstTime) {
            Huffman.makeTree(true);
            addRecentTexts(null);
            setLang(Settings.getMenuLang());
        }else
            veryFirstTime = false;
        GoogleTracker.sendScreen("HomeActivity");
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

        if (!isPopup) { //Don't mention the living library thing unless you're actually at the home screen
            addHeader(homeRoot);
        }
        addMenuGrid(homeRoot);
        addRecentTexts(homeRoot);
        addCalendar(homeRoot);


        //just extra spacing for the bottom
        homeRoot.addView(createTypeTitle(""));

        LayoutInflater inflater = (LayoutInflater) getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        View homeFooter = inflater.inflate(R.layout.home_footer, null);
        homeRoot.addView(homeFooter);


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
        if(recentRoot == null) {
            recentRoot = new LinearLayout(this);
            recentRoot.setOrientation(LinearLayout.VERTICAL);
            recentRoot.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
            homeRoot.addView(recentRoot);
        }
        else{
            recentRoot.removeAllViews();
        }
        final int columNum = 3;
        List<String> recentBooks = Settings.RecentTexts.getRecentTexts();
        recentTexts = new ArrayList<>();
        if(recentBooks.size()>0) {
            recentRoot.addView(createTypeTitle("Recent Texts"));
            LinearLayout recentRow = null;
            for (int i=0;i<recentBooks.size();i++){
                if(i%columNum  == 0){
                    recentRow = new LinearLayout(this);
                    recentRow.setOrientation(LinearLayout.HORIZONTAL);
                    recentRow.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
                    recentRoot.addView(recentRow);
                }

                String bookTitle = recentBooks.get(i);
                Book book = null;
                try {
                    book = new Book(bookTitle);
                    Pair<String,String> pair = Settings.BookSettings.getSavedBookTitle(bookTitle);
                    MenuDirectRef menuDirectRef = new MenuDirectRef(this, pair.first, pair.second, null, book, null);
                    recentTexts.add(menuDirectRef);
                    recentRow.addView(menuDirectRef);
                } catch (Book.BookNotFoundException e) {
                    e.printStackTrace();
                }

            }
        }
        /*
                    //add 'more' button in the row which was overflowed
            if (Math.floor(HOME_MENU_OVERFLOW_NUM/numColumns) == i+1 && limitGridSize)
                addMoreButton(ll);
         */

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
        for(MenuDirectRef menuDirectRef: dailtylearnings) {
            calendarRoot.addView(menuDirectRef);
        }
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

    public void settingsClick(View v) {
        //Toast.makeText(this,"You got me",Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(HomeActivity.this, AboutActivity.class);
        startActivity(intent);
    }

    public void aboutClick(View v) {
        String url = "http://sefaria.org/about";
        Intent intent = new Intent(Intent.ACTION_VIEW,Uri.parse(url));
        startActivity(intent);
    }

    public void feedbackClick(View v) {
        String email = "dev@sefaria.org";
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                "mailto", email, null));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Android App Feedback");
        emailIntent.putExtra(Intent.EXTRA_TEXT,
                "\n\n\n\n\nApp Version data:\n"
                + BuildConfig.VERSION_NAME + " ("  + BuildConfig.VERSION_CODE + ")" + "\n"
                + "Database Down Version:" + Database.getDBDownloadVersion() + "\n"
                + "Database Internal Version:" + Database.getVersionInDB() + "\n"
                + GoogleTracker.randomID

        );
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String [] {email});
        startActivity(Intent.createChooser(emailIntent, "Send email"));
    }

    public void siteClick(View v){
        String url = "http://sefaria.org";
        Intent intent = new Intent(Intent.ACTION_VIEW,Uri.parse(url));
        startActivity(intent);
    }

}
