package org.sefaria.sefaria.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import org.sefaria.sefaria.BuildConfig;
import org.sefaria.sefaria.MenuElements.MenuNode;
import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Settings;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.database.Database;
import org.sefaria.sefaria.database.Downloader;
import org.sefaria.sefaria.layouts.CustomActionbar;

import java.util.Set;

public class SettingsActivity extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        CustomActionbar customActionbar = new CustomActionbar(this, new MenuNode("Settings","Settings (he)", null), Settings.getSystemLang(),homeClick,null,null,null,null,backClick,-1);
        LinearLayout abRoot = (LinearLayout) findViewById(R.id.actionbarRoot);
        abRoot.addView(customActionbar);

        RadioButton defaultTextButton;
        Util.Lang defaultTextLang = Settings.getDefaultTextLang();
        switch (defaultTextLang){
            case BI:
                defaultTextButton = (RadioButton) findViewById(R.id.bilingual);
                break;
            case HE:
                defaultTextButton = (RadioButton) findViewById(R.id.hebrew);
                break;
            default://case EN:
                defaultTextButton = (RadioButton) findViewById(R.id.english);
                break;
        }
        defaultTextButton.setChecked(true);

        //LinearLayout gridRoot = (LinearLayout) findViewById(R.id.gridRoot);
        TextView appInfo = (TextView) findViewById(R.id.appInfo);
        appInfo.setText("App Version: " + BuildConfig.VERSION_NAME);
        TextView databaseInfo = (TextView) findViewById(R.id.databaseInfo);
        databaseInfo.setText("Library Version: " + Util.convertDBnum(Database.getDBDownloadVersion()));


    }

    View.OnClickListener homeClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent returnIntent = new Intent();
            returnIntent.putExtra("homeClicked",true);
            setResult(0, returnIntent);
            finish();
        }
    };

    View.OnClickListener backClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            onBackPressed();
        }
    };

    public void updateLibrary(View v){
        Toast.makeText(this, "Checking for updates", Toast.LENGTH_SHORT).show();
        Downloader.updateLibrary(this);
    }


    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.hebrew:
                if (checked)
                    Settings.setDefaultTextLang(Util.Lang.HE);
                    break;
            case R.id.english:
                if (checked)
                    Settings.setDefaultTextLang(Util.Lang.EN);
                    break;
            case R.id.bilingual:
                if (checked)
                    Settings.setDefaultTextLang(Util.Lang.BI);
                    break;
        }
    }


}
