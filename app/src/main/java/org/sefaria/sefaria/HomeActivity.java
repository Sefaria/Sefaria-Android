package org.sefaria.sefaria;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import org.sefaria.sefaria.menu.MenuGrid;
import org.sefaria.sefaria.menu.MenuNode;
import org.sefaria.sefaria.menu.MenuState;
import org.sefaria.sefaria.menu.MenuTabController;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;

public class HomeActivity extends AppCompatActivity {

    private final int NUM_COLUMNS = 3;
    private final boolean LIMIT_GRID_SIZE = true;

    private MenuGrid menuGrid;
    private MenuState menuState;
    private boolean isPopup;

    @Override
    protected void onCreate(Bundle in) {
        super.onCreate(in);
        setContentView(R.layout.activity_home);

        Intent intent = getIntent();
        menuState = intent.getParcelableExtra("menuState");
        isPopup = intent.getBooleanExtra("isPopup",false);

        if (in != null) {
            menuState = in.getParcelable("menuState");
            MenuTabController.restoreState(in);
        }

        init();


    }

    private void init() {

        if (menuState == null) {
            menuState = new MenuState();
        }

        menuGrid = new MenuGrid(this,NUM_COLUMNS, menuState,LIMIT_GRID_SIZE);
        ScrollView gridRoot = (ScrollView) findViewById(R.id.gridRoot);
        gridRoot.addView(menuGrid);

        //toggle closeClick, depending on if menu is popup or not
        View.OnClickListener tempCloseClick = null;
        if (isPopup) tempCloseClick = closeClick;

        CustomActionbar cab = new CustomActionbar(this,new MenuNode("Sefaria","ספאריה",null,null),
                Util.EN,null,tempCloseClick,null,null);
        LinearLayout abRoot = (LinearLayout) findViewById(R.id.actionbarRoot);
        abRoot.addView(cab);
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
        MenuTabController.saveState(out);
    }

    View.OnClickListener closeClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            finish();

        }
    };
}
