package org.sefaria.sefaria.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import org.sefaria.sefaria.BuildConfig;
import org.sefaria.sefaria.Dialog.DialogCallable;
import org.sefaria.sefaria.Dialog.DialogManager2;
import org.sefaria.sefaria.Dialog.DialogNoahSnackbar;
import org.sefaria.sefaria.MenuElements.MenuNode;
import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Settings;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.database.Database;
import org.sefaria.sefaria.database.Downloader;
import org.sefaria.sefaria.layouts.CustomActionbar;
import org.sefaria.sefaria.layouts.SefariaTextView;

public class SettingsActivity extends Activity {

    private final int TOT_NUM_DEBUG_DB_CLICKS = 6;
    private int numDebugDBUnlockClicks;
    private int numDebugAPIClicks;
    //private EditText fontSize;

    //buttons
    private SefariaTextView menuEnBtn;
    private SefariaTextView menuHeBtn;
    private SefariaTextView bookEnBtn;
    private SefariaTextView bookBiBtn;
    private SefariaTextView bookHeBtn;

    private SefariaTextView saveBtn;
    private SefariaTextView updateBtn;
    private SefariaTextView deleteBtn;
    private SefariaTextView downloadBtn;

    private Util.Lang currMenuLang;
    private Util.Lang currBookLang;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(Settings.getTheme());
        setContentView(R.layout.activity_settings);

        numDebugDBUnlockClicks = 0;

        backClick = null;
        CustomActionbar customActionbar = new CustomActionbar(this, new MenuNode("Settings","הגדרות", null), Settings.getSystemLang(),null,null,closeClick,null,null,null,null,null,R.color.system,false,false);
        LinearLayout abRoot = (LinearLayout) findViewById(R.id.actionbarRoot);
        abRoot.addView(customActionbar);
        //fontSize   = (EditText)findViewById(R.id.fontSize);

        menuEnBtn = (SefariaTextView) findViewById(R.id.menu_en_btn);
        menuHeBtn = (SefariaTextView) findViewById(R.id.menu_he_btn);
        bookEnBtn = (SefariaTextView) findViewById(R.id.book_en_btn);
        bookHeBtn = (SefariaTextView) findViewById(R.id.book_he_btn);
        bookBiBtn = (SefariaTextView) findViewById(R.id.book_bi_btn);

        saveBtn = (SefariaTextView) findViewById(R.id.save_btn);
        updateBtn = (SefariaTextView) findViewById(R.id.update_library);
        deleteBtn = (SefariaTextView) findViewById(R.id.delete_library);
        downloadBtn = (SefariaTextView) findViewById(R.id.download_library);

        menuEnBtn.setOnClickListener(btnClick);
        menuHeBtn.setOnClickListener(btnClick);
        bookEnBtn.setOnClickListener(btnClick);
        bookBiBtn.setOnClickListener(btnClick);
        bookHeBtn.setOnClickListener(btnClick);

        saveBtn.setOnClickListener(saveClick);


        deleteBtn.setOnClickListener(deleteClick);
        downloadBtn.setOnClickListener(downloadClick);
        updateBtn.setOnClickListener(updateClick);
        updateBtn.setOnLongClickListener(longUpdateLibrary);


    }
    View.OnClickListener backClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            onBackPressed();
        }
    };

    View.OnClickListener closeClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            done();
        }
    };


    View.OnClickListener saveClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            done();
        }
    };

    View.OnClickListener deleteClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
        DialogManager2.showDialog(SettingsActivity.this, new DialogCallable(MyApp.getRString(R.string.are_you_sure_delete_title),
                MyApp.getRString(R.string.are_you_sure_delete_message), MyApp.getRString(R.string.delete_library),
                MyApp.getRString(R.string.CANCEL), null, DialogCallable.DialogType.ALERT) {
            @Override
            public void positiveClick() {
                Database.deleteDatabase();
                setState(null,null,Settings.getUseAPI());
                setDatabaseInfo();
            }
        });
        }
    };


    View.OnClickListener downloadClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            updateLibrary(v);
        }
    };


    View.OnClickListener updateClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            updateLibrary(v);
        }
    };

    View.OnLongClickListener longUpdateLibrary = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            Toast.makeText(SettingsActivity.this, MyApp.getRString(R.string.downloading_library_even_not_newest), Toast.LENGTH_SHORT).show();
            Downloader.updateLibrary(SettingsActivity.this, true);
            DialogNoahSnackbar.showDialog(SettingsActivity.this, (ViewGroup) findViewById(R.id.dialogNoahSnackbarRoot));
            return true;
        }
    };

    public void updateLibrary(View v){
        Downloader.updateLibrary(this, false);
    }

    public void done(){
        //saveFontSize();
        Database.checkAndSwitchToNeededDB(this);
        finish();
    }
    /*private void saveFontSize(){
        Float value = 0f;
        try{
            value = Float.valueOf(fontSize.getText().toString());
        }catch (Exception e){
            ;
        }
        if(value != 0f){
            Settings.setDefaultFontSize(value);
        }
    }*/

    View.OnClickListener btnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.menu_en_btn:
                    currMenuLang = Util.Lang.EN;
                    break;
                case R.id.menu_he_btn:
                    currMenuLang = Util.Lang.HE;
                    break;
                case R.id.book_en_btn:
                    currBookLang = Util.Lang.EN;
                    break;
                case R.id.book_bi_btn:
                    currBookLang = Util.Lang.BI;
                    break;
                case R.id.book_he_btn:
                    currBookLang = Util.Lang.HE;
                    break;
            }

            setState(currMenuLang,currBookLang,Settings.getUseAPI());
        }
    };

    public void setState(Util.Lang menuLang, Util.Lang bookLang, boolean useAPI) {

        if(menuLang != null)
            currMenuLang = menuLang;
        else
            menuLang = currMenuLang;
        if(bookLang != null)
            currBookLang = bookLang;
        else
            bookLang = currBookLang;


        //BOOK LANG
        int currBookLangViewId;
        if (bookLang == Util.Lang.EN) currBookLangViewId = R.id.book_en_btn;
        else if (bookLang == Util.Lang.BI) currBookLangViewId = R.id.book_bi_btn;
        else /*if (bookLang == Util.Lang.HE)*/ currBookLangViewId = R.id.book_he_btn;

        if (currBookLangViewId == R.id.book_en_btn) {
            Settings.setDefaultTextLang(Util.Lang.EN);
            bookEnBtn.setBackgroundResource(Util.getDrawable(this, R.attr.text_menu_button_background_left_clicked_drawable));
        } else
            bookEnBtn.setBackgroundResource(Util.getDrawable(this,R.attr.text_menu_button_background_left_ripple_drawable));
        if (currBookLangViewId == R.id.book_bi_btn) {
            Settings.setDefaultTextLang(Util.Lang.BI);
            bookBiBtn.setBackgroundResource(Util.getDrawable(this, R.attr.text_menu_button_background_center_clicked_drawable));
        } else
            bookBiBtn.setBackgroundResource(Util.getDrawable(this,R.attr.text_menu_button_background_center_ripple_drawable));
        if (currBookLangViewId == R.id.book_he_btn) {
            Settings.setDefaultTextLang(Util.Lang.HE);
            bookHeBtn.setBackgroundResource(Util.getDrawable(this, R.attr.text_menu_button_background_right_clicked_drawable));
        } else
            bookHeBtn.setBackgroundResource(Util.getDrawable(this,R.attr.text_menu_button_background_right_ripple_drawable));


        //MENU LANG
        int currMenuLangViewId;
        if (menuLang == Util.Lang.EN) currMenuLangViewId = R.id.menu_en_btn;
        else /*if (menuLang == Util.Lang.HE) */ currMenuLangViewId = R.id.menu_he_btn;

        if (currMenuLangViewId == R.id.menu_en_btn) {
            Settings.setMenuLang(Util.Lang.EN);
            menuEnBtn.setBackgroundResource(Util.getDrawable(this, R.attr.text_menu_button_background_left_clicked_drawable));
        } else
            menuEnBtn.setBackgroundResource(Util.getDrawable(this,R.attr.text_menu_button_background_left_ripple_drawable));
        if (currMenuLangViewId == R.id.menu_he_btn) {
            Settings.setMenuLang(Util.Lang.HE);
            menuHeBtn.setBackgroundResource(Util.getDrawable(this, R.attr.text_menu_button_background_right_clicked_drawable));
        } else
            menuHeBtn.setBackgroundResource(Util.getDrawable(this,R.attr.text_menu_button_background_right_ripple_drawable));

        //IS OFFLINE???

        if(useAPI){
            downloadBtn.setVisibility(View.VISIBLE);
            deleteBtn.setVisibility(View.GONE);
            updateBtn.setVisibility(View.GONE);
        }else{
            downloadBtn.setVisibility(View.GONE);
            deleteBtn.setVisibility(View.VISIBLE);
            updateBtn.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Util.Lang defaultTextLang = Settings.getDefaultTextLang();
        Util.Lang menuLang = Settings.getMenuLang();
        boolean useAPI = Settings.getUseAPI();


        setState(menuLang,defaultTextLang,useAPI);

        //fontSize.setText(""+Settings.getDefaultFontSize());
        //fontSize.clearFocus();



        SefariaTextView appInfo = (SefariaTextView) findViewById(R.id.appInfo);
        appInfo.setText("App Version: " + BuildConfig.VERSION_NAME);
        setDatabaseInfo();
        View updateLibraryButton = findViewById(R.id.update_library);
        updateLibraryButton.setOnLongClickListener(longUpdateLibrary);

        DialogNoahSnackbar.checkCurrentDialog(this, (ViewGroup) this.findViewById(R.id.dialogNoahSnackbarRoot));

    }

    private void setDatabaseInfo(){
        String debugVer = "";
        if (Settings.getIsDebug()) debugVer = "D ";
        SefariaTextView onlineInfo = (SefariaTextView) findViewById(R.id.onlineInfo);
        SefariaTextView offlineInfo = (SefariaTextView) findViewById(R.id.offlineInfo);
        onlineInfo.setText(MyApp.getRString(R.string.online_library_version) + Util.convertDBnum(Database.getVersionInDB(true)));
        offlineInfo.setText(MyApp.getRString(R.string.offline_library_version) + debugVer + Util.convertDBnum(Database.getVersionInDB(false)));
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        Database.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    @Override
    public void onBackPressed() {
        //saveFontSize();
        Database.checkAndSwitchToNeededDB(this);
        super.onBackPressed();
    }





    public void clearAllBookSettings(View v){
        Settings.BookSettings.clearAllBookSettings();
    }

    //onclick listener (see xml)
    public void debugDBUnlockClick(View view) {
        if (numDebugDBUnlockClicks >= TOT_NUM_DEBUG_DB_CLICKS) {
            numDebugDBUnlockClicks = 0;
            Settings.setIsDebug(!Settings.getIsDebug()); //toggle
            setDatabaseInfo();
            Toast.makeText(this,"DB isDebug == " + Settings.getIsDebug(),Toast.LENGTH_SHORT).show();

        } else {
            numDebugDBUnlockClicks++;
        }

    }

    public void switchAPIClick(View view) {
        if (numDebugAPIClicks >= TOT_NUM_DEBUG_DB_CLICKS) {
            numDebugAPIClicks = 0;
            Settings.setUseAPI(!Settings.getUseAPI()); //toggle
            Toast.makeText(this,"usingAPI == " + Settings.getUseAPI(),Toast.LENGTH_SHORT).show();
            Database.checkAndSwitchToNeededDB(this);
            setState(currMenuLang,currBookLang,Settings.getUseAPI());
        } else {
            numDebugAPIClicks++;
        }

    }



}
