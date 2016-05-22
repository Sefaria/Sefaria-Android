package org.sefaria.sefaria.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import org.sefaria.sefaria.Dialog.DialogNoahSnackbar;
import org.sefaria.sefaria.GoogleTracker;
import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.Settings;
import org.sefaria.sefaria.database.Huffman;
import org.sefaria.sefaria.layouts.CustomActionbar;
import org.sefaria.sefaria.Util;

import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import org.sefaria.sefaria.R;
import org.sefaria.sefaria.MenuElements.MenuGrid;
import org.sefaria.sefaria.MenuElements.MenuState;

public class MenuActivity extends Activity {

    private final int NUM_COLUMNS = 2;
    private final boolean LIMIT_GRID_SIZE = false;

    private MenuGrid menuGrid;
    private MenuState menuState;
    private boolean isPopup;
    private boolean hasSectionBack; //true when you clicked a subsection to get to this menu
    private CustomActionbar customActionbar;

    @Override
    protected void onCreate(Bundle in) {
        super.onCreate(in);
        setTheme(Settings.getTheme());
        setContentView(R.layout.activity_menu);


        Intent intent = getIntent();
        menuState = intent.getParcelableExtra("menuState");
        hasSectionBack = intent.getBooleanExtra("hasSectionBack",false);

        boolean hasTabs = false;
        if (in != null) {
            menuState = in.getParcelable("menuState");
            hasSectionBack = in.getBoolean("hasSectionBack");
        }
        init();
    }

    private void init() {
        Util.Lang menuLang = Settings.getMenuLang();//menuState.getLang();
        setTitle(menuState.getCurrNode().getTitle(menuLang));
        //this specifically comes before menugrid, b/c in tabs it menugrid does funny stuff to currnode
        int catColor = menuState.getCurrNode().getTopLevelColor();
        homeClick = null;
        homeLongClick = null;
        customActionbar = new CustomActionbar(this, menuState.getCurrNode(),menuLang,homeClick,homeLongClick, null,null,null,null,backClick,menuClick,catColor,true);
        customActionbar.setMenuBtnLang(menuLang);
        LinearLayout abRoot = (LinearLayout) findViewById(R.id.actionbarRoot);
        abRoot.addView(customActionbar);

        menuGrid = new MenuGrid(this,NUM_COLUMNS, menuState,LIMIT_GRID_SIZE,menuLang);
        LinearLayout root = (LinearLayout) findViewById(R.id.gridRoot);
        root.addView(menuGrid);
        setLang(menuLang);
    }

    private void setLang(Util.Lang lang){
        if(lang == Util.Lang.BI) {
            lang = Util.Lang.EN;
        }
        menuState.setLang(lang);
        menuGrid.setLang(lang);
        customActionbar.setLang(lang);
    }

    private boolean veryFirstTime = true;
    @Override
    protected void onResume() {
        super.onResume();
        Huffman.makeTree(true);
        GoogleTracker.sendScreen("MenuActivity");
        GoogleTracker.sendEvent(GoogleTracker.CATEGORY_OPEN_MENU,menuState.getCurrNode().getTitle(Util.Lang.EN));
        if(!veryFirstTime) {
            setLang(Settings.getMenuLang());
        }else
            veryFirstTime = false;

        DialogNoahSnackbar.checkCurrentDialog(this, (ViewGroup) this.findViewById(R.id.dialogNoahSnackbarRoot));
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data != null) {
            boolean homeClicked = data.getBooleanExtra("homeClicked", false);

            if (homeClicked) {
                Intent returnIntent = new Intent();
                returnIntent.putExtra("homeClicked", true);
                setResult(0, returnIntent);
                finish();
            }
        }
        //menuGrid.setLang(menuGrid.getLang());//TODO noah this line seems useless
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putParcelable("menuState",menuState);
        out.putBoolean("hasSectionBack", hasSectionBack);
    }

    View.OnLongClickListener homeLongClick = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            //MyApp.homeClick(MenuActivity.this, true,false);
            return true;
        }
    };

    View.OnClickListener homeClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            /*
            Intent returnIntent = new Intent();
            returnIntent.putExtra("homeClicked",true);
            setResult(0, returnIntent);
            finish();
            */
            //MyApp.homeClick(MenuActivity.this,false,false);
            //finish();
        }
    };


    View.OnClickListener menuClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Util.Lang lang = Settings.switchMenuLang();
            setLang(lang);
            customActionbar.setMenuBtnLang(lang);
        }
    };

    View.OnClickListener backClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            onBackPressed();
        }
    };

}
