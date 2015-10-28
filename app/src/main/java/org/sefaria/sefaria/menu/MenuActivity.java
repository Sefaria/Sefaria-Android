package org.sefaria.sefaria.menu;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import org.sefaria.sefaria.layouts.CustomActionbar;
import org.sefaria.sefaria.Util;

import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import org.sefaria.sefaria.R;

public class MenuActivity extends AppCompatActivity {

    private final int NUM_COLUMNS = 2;
    private final boolean LIMIT_GRID_SIZE = false;

    private MenuGrid menuGrid;
    private MenuState menuState;
    private boolean isPopup;
    private boolean hasSectionBack; //true when you clicked a subsection to get to this menu

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
        setTitle(menuState.getCurrNode().getTitle(menuState.getLang()));

        //this specifically comes before menugrid, b/c in tabs it menugrid does funny stuff to currnode
        CustomActionbar cab = new CustomActionbar(this, menuState.getCurrNode(),Util.EN,searchClick,null,null,null);
        LinearLayout abRoot = (LinearLayout) findViewById(R.id.actionbarRoot);
        abRoot.addView(cab);

        menuGrid = new MenuGrid(this,NUM_COLUMNS, menuState,LIMIT_GRID_SIZE);
        ScrollView root = (ScrollView) findViewById(R.id.gridRoot);
        root.addView(menuGrid);


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

        boolean searchClicked = data.getBooleanExtra("searchClicked",false);

        Log.d("menu","SERACH " + searchClicked);

        if (searchClicked) {
            Intent returnIntent = new Intent();
            returnIntent.putExtra("searchClicked",true);
            setResult(0,returnIntent);
            finish();
        }
        menuGrid.setLang(menuGrid.getLang());
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putParcelable("menuState",menuState);
        out.putBoolean("hasSectionBack", hasSectionBack);
    }

    View.OnClickListener searchClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent returnIntent = new Intent();
            returnIntent.putExtra("searchClicked",true);
            setResult(0, returnIntent);
            finish();
        }
    };

}
