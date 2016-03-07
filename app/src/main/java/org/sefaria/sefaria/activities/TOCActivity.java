package org.sefaria.sefaria.activities;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import org.sefaria.sefaria.GoogleTracker;
import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Settings;
import org.sefaria.sefaria.TOCElements.TOCGrid;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.database.Book;
import org.sefaria.sefaria.database.Huffman;
import org.sefaria.sefaria.database.Node;
import org.sefaria.sefaria.layouts.CustomActionbar;
import org.sefaria.sefaria.MenuElements.MenuNode;

import java.util.List;

public class TOCActivity extends AppCompatActivity {

    private Book book;
    private String pathDefiningNode;
    private Util.Lang lang;
    private Context context;
    private TOCGrid tocGrid;

    public static Intent getStartTOCActivityIntent(Context superTextActivityThis, Book book, Node currNode){
        Intent intent = new Intent(superTextActivityThis, TOCActivity.class);
        intent.putExtra("currBook", book);
        String pathDefiningNode = currNode.makePathDefiningNode();
        intent.putExtra("pathDefiningNode", pathDefiningNode);
        return intent;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Huffman.makeTree(true);
        GoogleTracker.sendScreen("TOCActivity");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(Settings.getTheme());
        setContentView(R.layout.activity_toc);

        if(savedInstanceState == null){//it's coming from a saved to ram state
            Intent intent = getIntent();
            book = intent.getParcelableExtra("currBook");
            pathDefiningNode = intent.getStringExtra("pathDefiningNode");
        }else{
            book = savedInstanceState.getParcelable("currBook");
            pathDefiningNode = savedInstanceState.getString("pathDefiningNode");
            String bookTitle = savedInstanceState.getString("bookTitle");
            //Log.d("TOCActivity", "Coming back with savedInstanceState. book:" + book + ". pathDefiningNode: " + pathDefiningNode + ". bookTitle:" + bookTitle);
        }
        lang = Settings.getMenuLang();
        context = this;

        init();
    }

    private void init() {
        MenuNode titleNode = new MenuNode("Table of Contents","תוכן העניינים",null);
        int catColor = book.getCatColor();
        //CustomActionbar cab = new CustomActionbar(this, titleNode, Settings.getSystemLang(),homeClick,null,null,null,langClick,backClick,catColor);
        CustomActionbar cab = new CustomActionbar(this, titleNode, Settings.getSystemLang(),null,null,closeClick,null,null,langClick,backClick,catColor);
        LinearLayout abRoot = (LinearLayout) findViewById(R.id.actionbarRoot);
        abRoot.addView(cab);

        List<Node> tocNodesRoots = book.getTOCroots();
        Log.d("toc", "ROOTs SIZE " + tocNodesRoots.size());
        List<Book> commentaries = book.getAllCommentaries();

        ScrollView tocRoot = (ScrollView) findViewById(R.id.toc_root);

        tocGrid = new TOCGrid(this,book, tocNodesRoots,false,lang,pathDefiningNode);

        tocRoot.addView(tocGrid);

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable("currBook", book);
        outState.putString("pathDefiningNode", pathDefiningNode);
        outState.putString("bookTitle", book.title);
    }


    View.OnClickListener closeClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            finish();
        }
    };

    View.OnClickListener homeClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            MyApp.homeClick(TOCActivity.this,false);
            finish();
        }
    };

    View.OnClickListener langClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            //TODO change icon (maybe)
            lang = Settings.switchMenuLang();
            tocGrid.setLang(lang);
            //cab not changed b/c it stays as the systemLang
        }

    };

    View.OnClickListener backClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            onBackPressed();
        }
    };

}
