package org.sefaria.sefaria.activities;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import org.sefaria.sefaria.Dialog.DialogNoahSnackbar;
import org.sefaria.sefaria.GoogleTracker;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Settings;
import org.sefaria.sefaria.TOCElements.TOCGrid;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.database.API;
import org.sefaria.sefaria.database.Book;
import org.sefaria.sefaria.database.Huffman;
import org.sefaria.sefaria.database.Node;
import org.sefaria.sefaria.layouts.CustomActionbar;
import org.sefaria.sefaria.MenuElements.MenuNode;

import java.util.List;

public class TOCActivity extends AppCompatActivity {

    private Book book;
    public String pathDefiningNode;
    private Util.Lang lang;
    private Context context;
    private TOCGrid tocGrid;
    public boolean cameInFromBackPress;
    //public static final int COMING_BACK_TO_TOC_REQUEST_CODE = 1784;
    private CustomActionbar customActionbar;
    private int oldTheme = Settings.getTheme();


    public static Intent getStartTOCActivityIntent(Context context, Book book, Node currNode, String pathDefiningNode){
        Intent intent = new Intent(context, TOCActivity.class);
        intent.putExtra("currBook", book);
        if(pathDefiningNode != null){
            intent.putExtra("pathDefiningNode", pathDefiningNode);
        }
        else if(currNode != null) {
            pathDefiningNode = currNode.makePathDefiningNode();
            intent.putExtra("pathDefiningNode", pathDefiningNode);
        }
        return intent;
    }

    public static Intent getStartTOCActivityIntent(Context context, Book book, Node currNode){
        return getStartTOCActivityIntent(context,book,currNode,null);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(Settings.getTheme());
        setContentView(R.layout.activity_toc);
        cameInFromBackPress = false;

        if(savedInstanceState == null){//it's coming from a saved to ram state
            Intent intent = getIntent();
            book = intent.getParcelableExtra("currBook");
            pathDefiningNode = intent.getStringExtra("pathDefiningNode");
        }else{
            book = savedInstanceState.getParcelable("currBook");
            pathDefiningNode = savedInstanceState.getString("pathDefiningNode");
            //String bookTitle = savedInstanceState.getString("bookTitle");
            //Log.d("TOCActivity", "Coming back with savedInstanceState. book:" + book + ". pathDefiningNode: " + pathDefiningNode + ". bookTitle:" + bookTitle);
        }
        lang = Settings.getMenuLang();
        context = this;


        MenuNode titleNode = new MenuNode("Table of Contents","תוכן העניינים",null);
        int catColor = book.getCatColor();
        //this would make it that when coming straight to TOC from Menu, it's a back icon instead of close, but we removed the back functionality, so it's currently not going to work
        if(pathDefiningNode == null || cameInFromBackPress) {
            closeClick = null;
        }else{
            backClick = null;
        }
        homeClick = null;
        //pathDefiningNode = null;//this is so that when back is pressed (and not coming back from bundle.. TOTDO has to be dealt with) it will act differently

        customActionbar = new CustomActionbar(this, titleNode, Settings.getSystemLang(),homeClick,null,closeClick,null,null,null,backClick,langClick,catColor,false,false);
        customActionbar.setMenuBtnLang(lang);
        LinearLayout abRoot = (LinearLayout) findViewById(R.id.actionbarRoot);
        abRoot.addView(customActionbar);


        List<Node> tocNodesRoots = null;
        try {
            tocNodesRoots = book.getTOCroots();
        } catch (API.APIException e) {
            API.makeAPIErrorToast(context);
            finish();
            return;
        }

        //List<Book> commentaries = book.getAllCommentaries();
        ScrollView tocRoot = (ScrollView) findViewById(R.id.toc_root);
        tocGrid = new TOCGrid(TOCActivity.this,book, tocNodesRoots,false,lang,pathDefiningNode);
        tocRoot.addView(tocGrid);

    }

    @Override
    protected void onResume() {
        super.onResume();
        if(Settings.getTheme() != oldTheme){
            finish();
            startActivity(getStartTOCActivityIntent(this,book,null,pathDefiningNode));
            return;
        }
        Huffman.makeTree(true);
        GoogleTracker.sendScreen("TOCActivity");
        //Log.d("TOCAct","onResume");

        DialogNoahSnackbar.checkCurrentDialog(this, (ViewGroup) this.findViewById(R.id.dialogNoahSnackbarRoot));
    }



    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable("currBook", book);
        outState.putString("pathDefiningNode", pathDefiningNode);
        outState.putString("bookTitle", book.title);
    }

    /**
     * go from TOC to textActivity
     *
     * @param context
     * @param node
     * @param lang
     */
    public static void gotoTextActivity(Context context,Node node,Util.Lang lang){
        Node.saveNode(node);

        Book book;
        try{
            book = node.getBook();
        } catch (Book.BookNotFoundException e) {
            book = null;
        }
        Util.Lang bookLang;
        if (book != null)
            bookLang = Settings.BookSettings.getSavedBook(book).lang;
        else
            bookLang = Settings.getDefaultTextLang();

        Intent intent;
        if (Settings.getIsCts() && bookLang != Util.Lang.BI && SuperTextActivity.canBeCts(book)) {
            intent = new Intent(context, CtsTextActivity.class);
        } else {
            intent = new Intent(context, SepTextActivity.class);
        }
        intent.putExtra("nodeHash", node.hashCode());
        intent.putExtra("lang", lang);
        if(((TOCActivity) context).pathDefiningNode != null && ((TOCActivity) context).pathDefiningNode.length() >0 && !((TOCActivity) context).cameInFromBackPress)//it will only do this if a SectionAct was already opened
            ;//intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);//This will make it return back to TextAct
            // I turned this off to simplify the stack a reduce weird back bugs. Such as Segment ->TOC ->TextPage on button press -> Backpress brings you to TOC -> button press tries to renew a textAct that already was killed (so it would just bring up a random one form the past and mess things up.

        //This don't need request b/c I'm no longer doing the REORDERING
        //((TOCActivity) context).startActivityForResult(intent, TOCActivity.COMING_BACK_TO_TOC_REQUEST_CODE); //using result so that we can know if someone presses back to come back fromo TextAct (instead of reopening the TOC).

        //TODO determine if SepTextActivity was already open... Make sure to be careful of multi-tab stuff
        //TODO I think it should also actually have the back button work for going to the TOC from textActivity

        context.startActivity(intent);


        //Activity act = (Activity) context; //stupid casting
        //act.setResult(Activity.RESULT_OK,intent);
        //act.finish();//close the TOC
    }



    /*
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("TOCAct","requestCode, resultCode"  + requestCode + " ," + resultCode);
        if (requestCode == COMING_BACK_TO_TOC_REQUEST_CODE) {
            if (resultCode == RESULT_CANCELED) {
                Log.d("TOCAct","resultCode == RESULT_CANCELED");
                cameInFromBackPress = true;// so you know it was a back press from SuperTextActivity (so you know to create a new textActivity
            }
        }
    }*/

    View.OnClickListener closeClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            finish();
        }
    };

    View.OnClickListener homeClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            //MyApp.homeClick(TOCActivity.this,false,false);
        }
    };

    View.OnClickListener langClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            lang = Settings.switchMenuLang();
            tocGrid.setLang(lang);
            //cab regular lang does not changed b/c it stays as the systemLang
            //but the little icon should change
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
