package org.sefaria.sefaria.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import org.sefaria.sefaria.database.API;
import org.sefaria.sefaria.database.Book;
import org.sefaria.sefaria.database.Link;
import org.sefaria.sefaria.database.Text;
import org.sefaria.sefaria.layouts.CustomActionbar;
import org.sefaria.sefaria.DialogManager;
import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.database.UpdateReceiver;
import org.sefaria.sefaria.database.UpdateService;
import org.sefaria.sefaria.MenuElements.MenuGrid;
import org.sefaria.sefaria.MenuElements.MenuNode;
import org.sefaria.sefaria.MenuElements.MenuState;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

public class HomeActivity extends Activity {

    private final int NUM_COLUMNS = 3;
    private final boolean LIMIT_GRID_SIZE = true;

    private MenuGrid menuGrid;
    private MenuState menuState;
    private boolean isPopup;
    private CustomActionbar cab;

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
        Util.Lang menuLang = MyApp.getMenuLang();
        menuGrid = new MenuGrid(this,NUM_COLUMNS, menuState,LIMIT_GRID_SIZE,menuLang);
        ScrollView gridRoot = (ScrollView) findViewById(R.id.gridRoot);
        gridRoot.addView(menuGrid);

        //toggle closeClick, depending on if menu is popup or not
        View.OnClickListener tempCloseClick = null;
        if (isPopup) tempCloseClick = closeClick;

        cab = new CustomActionbar(this,new MenuNode("Sefaria","ספאריה",null),
                menuLang,null,tempCloseClick,searchClick,null,menuClick,null);
        LinearLayout abRoot = (LinearLayout) findViewById(R.id.actionbarRoot);
        abRoot.addView(cab);

        /*
        This code doesn't belong here:
         */
        try {
            Book book = (new Book("Genesis"));
            Text text = book.getTOCroots().get(0).getFirstDescendant().getTexts().get(0);
            Link.LinkCount linkCount = Link.LinkCount.getFromLinks_small(text);
            linkCount.getSlimmedTitle(book, menuLang);
            linkCount.getCount();
            for(Link.LinkCount linkFilter : linkCount.getChildren()){
                Link.getLinkedTexts(text,linkFilter);
                Link.getLinkedTexts(text,linkFilter.getChildren().get(0));
            }

            //see LinkCount.printTree() for recursive function using getChildren()
        }catch (Exception e){
            e.printStackTrace();
        }

        if(API.useAPI()) { //TODO move
            Toast.makeText(this, "starting download", Toast.LENGTH_SHORT).show();
            updateLibrary();
        }
    }

    private void setLang(Util.Lang lang){
        if(lang == Util.Lang.BI) {
            lang = Util.Lang.EN;
        }
        menuState.setLang(lang);
        menuGrid.setLang(lang);
        cab.setLang(lang);

    }

    //this is a click event listener
    public void updateLibrary() { //(View button)
        UpdateService.lockOrientation(this);
        Intent intent = new Intent(this,UpdateReceiver.class);
        intent.putExtra("isPre",true);
        intent.putExtra("userInit",true);
        sendBroadcast(intent);
        DialogManager.showDialog(DialogManager.CHECKING_FOR_UPDATE);
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
            setLang(MyApp.switchMenuLang());
        }
    };


}
