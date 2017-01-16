package org.sefaria.sefaria.activities;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import org.sefaria.sefaria.Dialog.DialogNoahSnackbar;
import org.sefaria.sefaria.GoogleTracker;
import org.sefaria.sefaria.MenuElements.MenuDirectRef;
import org.sefaria.sefaria.MenuElements.MenuNode;
import org.sefaria.sefaria.MenuElements.MenuDirectRefsGrid;
import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Settings;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.database.Recents;
import org.sefaria.sefaria.layouts.CustomActionbar;

import java.util.List;
import java.util.Set;

public class RecentsActivity extends AppCompatActivity {

    private CustomActionbar customActionbar;
    private MenuDirectRefsGrid menuDirectRefsGrid;
    private int oldTheme = Settings.getTheme();
    private final int NUM_COLUMNS = 2;
    private final boolean LIMIT_GRID_SIZE = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(Settings.getTheme());
        setContentView(R.layout.activity_recents);

        Util.Lang menuLang = Settings.getMenuLang();
        setTitle("Recently Viewed");
        //this specifically comes before menugrid, b/c in tabs it menugrid does funny stuff to currnode
        customActionbar = new CustomActionbar(this,new MenuNode(MyApp.getRString(R.string.recently_viewed),MyApp.getRString(R.string.recently_viewed),null),Settings.getSystemLang(),null,null,null,null,null,null,backClick,menuClick,R.color.system,true,true);
        customActionbar.setMenuBtnLang(menuLang);
        LinearLayout abRoot = (LinearLayout) findViewById(R.id.actionbarRoot);
        abRoot.addView(customActionbar);

        List<MenuDirectRef> recents = Recents.getRecentDirectMenu(this, true, true);
        menuDirectRefsGrid = new MenuDirectRefsGrid(this, NUM_COLUMNS, recents);
        LinearLayout root = (LinearLayout) findViewById(R.id.gridRoot);
        root.addView(menuDirectRefsGrid);
    }

    private void setLang(Util.Lang lang){
        if(lang == Util.Lang.BI) {
            lang = Util.Lang.EN;
        }
        menuDirectRefsGrid.setLang(lang);
        // //customActionbar.setLang(lang);//not setting lang b/c it should be system lang
    }

    private boolean veryFirstTime = true;
    @Override
    protected void onResume() {
        super.onResume();

        if(oldTheme != Settings.getTheme()){
            restartActivity();
            return;
        }

        GoogleTracker.sendScreen("RecentsActivity");
        if(!veryFirstTime) {
            //in case the list has changed.
            // say, for example, you went to a segment so now that should be on top (and maybe it will have a different perek in it's name)
            menuDirectRefsGrid.setNewList(Recents.getRecentDirectMenu(this,true,true));
        }else
            veryFirstTime = false;

        DialogNoahSnackbar.checkCurrentDialog(this, (ViewGroup) this.findViewById(R.id.dialogNoahSnackbarRoot));
    }

    private void restartActivity(){
        Intent intent = new Intent(this,MenuActivity.class);
        //intent.putExtra("menuState",menuState);
        startActivity(intent);
        finish();
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        //out.putParcelable("menuState",menuState);
    }

    View.OnClickListener menuClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Util.Lang lang = Settings.switchMenuLang();
            setLang(lang);
            // //customActionbar.setLang(lang);//not setting lang b/c it should be system lang
        }
    };

    View.OnClickListener backClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            onBackPressed();
        }
    };
}
