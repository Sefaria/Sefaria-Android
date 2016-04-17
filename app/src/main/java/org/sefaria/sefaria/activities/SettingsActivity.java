package org.sefaria.sefaria.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import org.sefaria.sefaria.BuildConfig;
import org.sefaria.sefaria.Dialog.DialogNoahSnackbar;
import org.sefaria.sefaria.MenuElements.MenuNode;
import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Settings;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.database.Database;
import org.sefaria.sefaria.database.Downloader;
import org.sefaria.sefaria.layouts.CustomActionbar;

public class SettingsActivity extends Activity {

    private final int TOT_NUM_DEBUG_DB_CLICKS = 7;
    private int numDebugDBUnlockClicks;
    private EditText fontSize;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        numDebugDBUnlockClicks = 0;

        backClick = null;
        CustomActionbar customActionbar = new CustomActionbar(this, new MenuNode("Settings","Settings (he)", null), Settings.getSystemLang(),null,null,closeClick,null,null,null,backClick,null,-1);
        LinearLayout abRoot = (LinearLayout) findViewById(R.id.actionbarRoot);
        abRoot.addView(customActionbar);
        fontSize   = (EditText)findViewById(R.id.fontSize);
    }

    @Override
    protected void onResume() {
        super.onResume();
        RadioButton defaultTextButton;
        Util.Lang defaultTextLang = Settings.getDefaultTextLang();
        switch (defaultTextLang){
            case BI:
                defaultTextButton = (RadioButton) findViewById(R.id.text_bilingual);
                break;
            case HE:
                defaultTextButton = (RadioButton) findViewById(R.id.text_hebrew);
                break;
            default://case EN:
                defaultTextButton = (RadioButton) findViewById(R.id.text_english);
                break;
        }
        defaultTextButton.setChecked(true);

        RadioButton menuLangButton;
        Util.Lang menuLang = Settings.getMenuLang();
        switch (menuLang){
            case HE:
                menuLangButton = (RadioButton) findViewById(R.id.menu_hebrew);
                break;
            default: //case EN:
                menuLangButton = (RadioButton) findViewById(R.id.menu_english);
                break;
        }
        menuLangButton.setChecked(true);

        RadioButton useDBButton;
        boolean useAPI = Settings.getUseAPI();
        if (useAPI){
            useDBButton = (RadioButton) findViewById(R.id.DB_use_API);
        }else{
            useDBButton = (RadioButton) findViewById(R.id.DB_use_full);
        }
        useDBButton.setChecked(true);


        fontSize.setText(""+Settings.getDefaultFontSize());
        fontSize.clearFocus();


        //LinearLayout gridRoot = (LinearLayout) findViewById(R.id.gridRoot);


        TextView appInfo = (TextView) findViewById(R.id.appInfo);
        appInfo.setText("App Version: " + BuildConfig.VERSION_NAME);
        setDatabaseInfo();
        View updateLibraryButton = findViewById(R.id.update_library);
        updateLibraryButton.setOnLongClickListener(longUpdateLibrary);

        DialogNoahSnackbar.checkCurrentDialog(this, (ViewGroup) this.findViewById(R.id.dialogNoahSnackbarRoot));

    }

    private void setDatabaseInfo(){
        String debugVer = "";
        if (Settings.getIsDebug()) debugVer = "D ";
        TextView databaseInfo = (TextView) findViewById(R.id.databaseInfo);
        databaseInfo.setText(
                MyApp.getRString(R.string.online_library_version) + Util.convertDBnum(Database.getVersionInDB(true)) + "\n"
                + MyApp.getRString(R.string.offline_library_version) + debugVer + Util.convertDBnum(Database.getVersionInDB(false))
        );
    }

    private void saveFontSize(){
        Float value = 0f;
        try{
            value = Float.valueOf(fontSize.getText().toString());
        }catch (Exception e){
            ;
        }
        if(value != 0f){
            Settings.setDefaultFontSize(value);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        Database.onRequestPermissionsResult(this,requestCode,permissions,grantResults);
    }

    View.OnLongClickListener homeLongClick = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            saveFontSize();
            //MyApp.homeClick(SettingsActivity.this, true,false);
            return true;
        }
    };

    View.OnClickListener homeClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            saveFontSize();
            Intent returnIntent = new Intent();
            returnIntent.putExtra("homeClicked",true);
            setResult(0, returnIntent);
            finish();
        }
    };



    @Override
    public void onBackPressed() {
        saveFontSize();
        Database.checkAndSwitchToNeededDB(this);
        super.onBackPressed();
    }

    View.OnClickListener closeClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            done(v);
        }
    };

    View.OnClickListener backClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            onBackPressed();
        }
    };

    public void updateLibrary(View v){
        Downloader.updateLibrary(this, false);
        DialogNoahSnackbar.showDialog(this, (ViewGroup) findViewById(R.id.dialogNoahSnackbarRoot));
    }

    View.OnLongClickListener longUpdateLibrary = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            Toast.makeText(SettingsActivity.this, "Downloading Library (even if it's not a new version).", Toast.LENGTH_SHORT).show();
            Downloader.updateLibrary(SettingsActivity.this, true);
            DialogNoahSnackbar.showDialog(SettingsActivity.this, (ViewGroup) findViewById(R.id.dialogNoahSnackbarRoot));
            return true;
        }
    };

    public void clearAllBookSettings(View v){
        Settings.BookSettings.clearAllBookSettings();
    }

    public void done(View v){
        saveFontSize();
        Database.checkAndSwitchToNeededDB(this);
        finish();
    }

    public void onUseDB(View view){
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.DB_use_API:
                if (checked)
                    Settings.setUseAPI(true);
                break;
            case R.id.DB_use_full:
                if (checked)
                    Settings.setUseAPI(false);
                    Database.getOfflineDB(this,true);
                break;
        }
    }

    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.text_hebrew:
                if (checked)
                    Settings.setDefaultTextLang(Util.Lang.HE);
                    break;
            case R.id.text_english:
                if (checked)
                    Settings.setDefaultTextLang(Util.Lang.EN);
                    break;
            case R.id.text_bilingual:
                if (checked)
                    Settings.setDefaultTextLang(Util.Lang.BI);
                    break;
        }
    }

    public void onMenuLangRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.menu_hebrew:
                if (checked)
                    Settings.setMenuLang(Util.Lang.HE);
                break;
            case R.id.menu_english:
                if (checked)
                    Settings.setMenuLang(Util.Lang.EN);
                break;
        }
    }

    public void debubDBUnlockClick(View view) {
        if (numDebugDBUnlockClicks >= TOT_NUM_DEBUG_DB_CLICKS) {
            numDebugDBUnlockClicks = 0;
            Settings.setIsDebug(!Settings.getIsDebug()); //toggle
            setDatabaseInfo();
            Toast.makeText(this,"DB isDebug == " + Settings.getIsDebug(),Toast.LENGTH_SHORT).show();

        } else {
            numDebugDBUnlockClicks++;
        }

    }



}
