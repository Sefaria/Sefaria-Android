package org.sefaria.sefaria.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.layouts.CustomActionbar;
import org.sefaria.sefaria.Util;

import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import org.sefaria.sefaria.R;
import org.sefaria.sefaria.menu.MenuGrid;
import org.sefaria.sefaria.menu.MenuState;

public class MenuActivity extends Activity {

    private final int NUM_COLUMNS = 2;
    private final boolean LIMIT_GRID_SIZE = false;

    private MenuGrid menuGrid;
    private MenuState menuState;
    private boolean isPopup;
    private boolean hasSectionBack; //true when you clicked a subsection to get to this menu
    private CustomActionbar cab;

    @Override
    protected void onCreate(Bundle in) {
        super.onCreate(in);
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
        Util.Lang menuLang = menuState.getLang();
        setTitle(menuState.getCurrNode().getTitle(menuLang));
        //this specifically comes before menugrid, b/c in tabs it menugrid does funny stuff to currnode
        cab = new CustomActionbar(this, menuState.getCurrNode(),menuLang,homeClick,null,null,null,menuClick);
        LinearLayout abRoot = (LinearLayout) findViewById(R.id.actionbarRoot);
        abRoot.addView(cab);

        menuGrid = new MenuGrid(this,NUM_COLUMNS, menuState,LIMIT_GRID_SIZE,menuLang);
        ScrollView root = (ScrollView) findViewById(R.id.gridRoot);
        root.addView(menuGrid);
    }
    private void setLang(Util.Lang lang){
        if(lang == Util.Lang.BI) {
            lang = Util.Lang.EN;
        }
        menuState.setLang(lang);
        menuGrid.setLang(lang);
        cab.setLang(lang);
    }

    @Override
    protected void onResume() {
        super.onResume();
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
        menuGrid.setLang(menuGrid.getLang());//TODO noah this line seems useless
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putParcelable("menuState",menuState);
        out.putBoolean("hasSectionBack", hasSectionBack);
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


    View.OnClickListener menuClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
           setLang(MyApp.switchMenuLang());
        }
    };

}
