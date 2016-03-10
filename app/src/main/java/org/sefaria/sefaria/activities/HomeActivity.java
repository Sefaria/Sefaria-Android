package org.sefaria.sefaria.activities;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
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
import org.sefaria.sefaria.database.LinkFilter;
import org.sefaria.sefaria.database.Node;
import org.sefaria.sefaria.database.Searching;
import org.sefaria.sefaria.database.Text;
import org.sefaria.sefaria.database.UpdateService;
import org.sefaria.sefaria.layouts.CustomActionbar;
import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.MenuElements.MenuGrid;
import org.sefaria.sefaria.MenuElements.MenuNode;
import org.sefaria.sefaria.MenuElements.MenuState;
import org.sefaria.sefaria.layouts.SefariaTextView;

import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends Activity {

    private final int NUM_COLUMNS = 3;
    private final boolean LIMIT_GRID_SIZE = true;

    private MenuGrid menuGrid;
    private MenuState menuState;
    private boolean isPopup;
    private List<MenuDirectRef> dailyLearnings;
    private List<MenuDirectRef> recentTexts;
    private LinearLayout recentRoot;

    @Override
    protected void onCreate(Bundle in) {
        super.onCreate(in);
        setTheme(Settings.getTheme());
        setContentView(R.layout.activity_home);
        Huffman.makeTree(true);


        Intent intent = getIntent();

        handleIncomingURL(intent);

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

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIncomingURL(intent);
    }

    private void init() {

        if (menuState == null) {
            menuState = new MenuState();
        }

        LinearLayout gridRoot = (LinearLayout) findViewById(R.id.gridRoot);
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
        homeRoot.addView(createTypeTitle("",false));

        LayoutInflater inflater = (LayoutInflater) getSystemService(Activity.LAYOUT_INFLATER_SERVICE);


        //toggle closeClick, depending on if menu is popup or not
        View.OnClickListener tempCloseClick = null;
        //if (isPopup) tempCloseClick = closeClick; //Removing the close click for now to test without it


        LinearLayout abRoot = (LinearLayout) findViewById(R.id.actionbarRoot);
        CustomActionbar cab = new CustomActionbar(this,new MenuNode("Sefaria","ספאריה",null),
                Settings.getSystemLang(),null,null,tempCloseClick,null,null,menuClick,null,-1);
        abRoot.addView(cab);

        dealWithDatabaseStuff();
    }

    private void handleIncomingURL(Intent intent){
        try {
            if (intent != null && intent.getAction().equalsIgnoreCase(Intent.ACTION_VIEW)
                //&&  intent.getCategories().contains(Intent.CATEGORY_BROWSABLE)
                    ) {
                //url to go to  www.sefaria.org
                String url = intent.getDataString();
                Log.d("HomeActivity", "Sefaria URL:" + url);
                GoogleTracker.sendEvent(GoogleTracker.CATEGORY_OPENED_URL, url);
                try {
                    String place = url.replaceAll("(?i).*sefaria\\.org/?(s2/)?", "");
                    Log.d("HomeActivity", "place:" + place);
                    String[] spots = place.split("\\.");
                    Book book = new Book(spots[0]);
                    if (spots.length == 1) {
                        SuperTextActivity.startNewTextActivityIntent(this, book, true);
                        finish();
                        return;
                    } else {
                        Node node = book.getTOCroots().get(0);
                        for (int i = 1; i < spots.length; i++) {
                            Node tempNode = node.getChild(spots[i]);
                            if (tempNode == null) {
                                tempNode = node.getFirstDescendant(false);
                                if (tempNode == node) {//you were already at the final level... I guess this number means it's the level1 value
                                    try {
                                        int num = Integer.valueOf(spots[i]);
                                        List<Text> texts = node.getTexts();
                                        for (Text text : texts) {
                                            if (text.levels[0] == num) {
                                                SuperTextActivity.startNewTextActivityIntent(this, book, text, node, true);
                                                finish();
                                                return;
                                            }
                                        }
                                    } catch (Exception e1) {

                                    }
                                }
                                break;
                            } else {
                                node = tempNode;
                            }
                        }
                        SuperTextActivity.startNewTextActivityIntent(this, book, null, node,true);
                        finish();
                        return;
                    }
                } catch (Exception e) {
                    Toast.makeText(this, this.getString(R.string.cannot_parse_link), Toast.LENGTH_SHORT).show();
                    Log.e("HomeActivity", "Parsing URL. " + e.getMessage());
                    url = url.replaceFirst("http", "https");
                    Intent intent2 = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent2);
                }
            }
        }catch (Exception e){
            Log.e("HomeActivity","not able to open intent for URL parse " + e.getMessage());
        }
    }

    private void dealWithDatabaseStuff(){
        long time = Settings.getDownloadSuccess(true);
        if(time >0)
            GoogleTracker.sendEvent("Download", "Update Finished",time);

        Util.deleteNonRecursiveDir(Downloader.FULL_DOWNLOAD_PATH); //remove any old temp downloads

        if(API.useAPI() || !Database.isValidDB()) {
            Database.createAPIdb();
            Toast.makeText(this, "Starting Download", Toast.LENGTH_SHORT).show();
            Downloader.updateLibrary(this,false);

        }
    }

    private void addHeader(LinearLayout homeRoot){
        //Living Library
        TextView livingLibraryView = createTypeTitle("A Living Library of Jewish Texts",true);
        livingLibraryView.setTextSize(20);
        int livingPadding = 60;
        livingLibraryView.setPadding(3, livingPadding, 3, livingPadding);
        livingLibraryView.setTextColor(Util.getColor(this, R.attr.text_color_main));
        homeRoot.addView(livingLibraryView);
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
            recentRoot.addView(createTypeTitle("Recent Texts",false));
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
                    menuDirectRef.setLongClickPinning();
                    recentTexts.add(menuDirectRef);
                    recentRow.addView(menuDirectRef);
                } catch (Exception e) {
                    Log.e("HomeActivity", "Problem getting Recent Texts:" + e.getMessage());
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
        LinearLayout calendarRoot = new LinearLayout(this);
        calendarRoot.setOrientation(LinearLayout.HORIZONTAL);
        calendarRoot.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        homeRoot.addView(createTypeTitle("Calendar", false));
        homeRoot.addView(calendarRoot);
        dailyLearnings = DailyLearning.getDailyLearnings(this);
        for(MenuDirectRef menuDirectRef: dailyLearnings) {
            calendarRoot.addView(menuDirectRef);
        }
    }

    private TextView createTypeTitle(String title, boolean isSerif){
        SefariaTextView textView = new SefariaTextView(this);
        textView.setText(title);
        final int paddingSide= 3;
        final int paddingTop = 20;
        textView.setPadding(paddingSide, paddingTop * 2, paddingSide, paddingTop);
        textView.setTextSize(20);
        textView.setFont(Util.Lang.EN, isSerif); //TODO change with system lang
        textView.setTextColor(Util.getColor(this, R.attr.text_color_english));
        textView.setGravity(Gravity.CENTER);
        if (! isSerif && Build.VERSION.SDK_INT > 14) {
            textView.setAllCaps(true);
        }

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

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MyApp.REQUEST_WRITE_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    dealWithDatabaseStuff();

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
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
        Intent intent = new Intent(HomeActivity.this, SettingsActivity.class);
        startActivity(intent);
    }

    public void aboutClick(View v) {
        String url = "https://sefaria.org/about";
        Intent intent = new Intent(Intent.ACTION_VIEW,Uri.parse(url));
        startActivity(intent);
    }

    private static Searching searching;
    public void feedbackClick(View v) {

        /*
        try {
            if(searching == null) {
                boolean alsoSearchCommentary = true;
                searching = new Searching("ברא", new LinkFilter("Tosefta", 0, "", LinkFilter.DEPTH_TYPE.CAT),alsoSearchCommentary);
                //searching = new Searching("ברא", null,alsoSearchCommentary);
            }
            ArrayList<Text> results = searching.getResults();
            results = API.getSearchResults("love",null,0,10);

            Log.d("Searching", "results.size" + results.size());
            for(Text verse:results){
                verse.log();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        if(true) return;
        */

        String email = "android@sefaria.org";
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                "mailto", email, null));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Android App Feedback");
        emailIntent.putExtra(Intent.EXTRA_TEXT, getEmailHeader());
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String [] {email});
        startActivity(Intent.createChooser(emailIntent, "Send email"));
    }

    public static String getEmailHeader(){
        return  "App Version: " + BuildConfig.VERSION_NAME + " ("  + BuildConfig.VERSION_CODE + ")" + "\n"
                + "Library Version: " + Util.convertDBnum(Database.getVersionInDB()) + "\n"
                + GoogleTracker.randomID + "\n"
                + Build.VERSION.RELEASE + " (" + Build.VERSION.SDK_INT + ")" + "\n"
                +"\n\n\n";
    }

    public void siteClick(View v){
        String url = "https://sefaria.org";
        Intent intent = new Intent(Intent.ACTION_VIEW,Uri.parse(url));
        startActivity(intent);
    }

}
