package org.sefaria.sefaria.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import org.sefaria.sefaria.MenuElements.MenuDirectRef;
import org.sefaria.sefaria.Settings;
import org.sefaria.sefaria.database.API;
import org.sefaria.sefaria.database.DailyLearning;
import org.sefaria.sefaria.database.Downloader;
import org.sefaria.sefaria.layouts.CustomActionbar;
import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.MenuElements.MenuGrid;
import org.sefaria.sefaria.MenuElements.MenuNode;
import org.sefaria.sefaria.MenuElements.MenuState;

import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import java.util.List;

public class HomeActivity extends Activity {

    private final int NUM_COLUMNS = 3;
    private final boolean LIMIT_GRID_SIZE = true;

    private MenuGrid menuGrid;
    private MenuState menuState;
    private boolean isPopup;
    private CustomActionbar cab;
    private List<MenuDirectRef> dailtylearnings;

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

        Util.Lang menuLang = Settings.getMenuLang();
        menuGrid = new MenuGrid(this,NUM_COLUMNS, menuState,LIMIT_GRID_SIZE,menuLang);
        homeRoot.addView(menuGrid);

        LinearLayout calendarRoot = new LinearLayout(this);
        calendarRoot.setOrientation(LinearLayout.HORIZONTAL);
        calendarRoot.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        homeRoot.addView(calendarRoot);

        dailtylearnings = DailyLearning.getDailyLearnings(this);
        for(MenuDirectRef menuDirectRef: dailtylearnings)
            calendarRoot.addView(menuDirectRef);



        //toggle closeClick, depending on if menu is popup or not
        View.OnClickListener tempCloseClick = null;
        //if (isPopup) tempCloseClick = closeClick; //Removing the close click for now to test without it

        String title; //This is forcing the word Sefaria to be based on System lang and not based on menuLang (it can easily be changed by inserting each value into the new MenuNode

        cab = new CustomActionbar(this,new MenuNode("Sefaria","ספאריה",null),
                Settings.getSystemLang(),null,tempCloseClick,null,null,menuClick,null,-1);
        LinearLayout abRoot = (LinearLayout) findViewById(R.id.actionbarRoot);
        abRoot.addView(cab);




        if(API.useAPI()) {
            Toast.makeText(this, "starting download", Toast.LENGTH_SHORT).show();
            Downloader.updateLibrary(this);
        }

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
