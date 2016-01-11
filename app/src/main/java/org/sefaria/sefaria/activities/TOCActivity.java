package org.sefaria.sefaria.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.IInterface;
import android.os.PersistableBundle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.TOCElements.TOCGrid;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.database.Book;
import org.sefaria.sefaria.database.Node;
import org.sefaria.sefaria.layouts.AutoResizeTextView;
import org.sefaria.sefaria.layouts.CustomActionbar;
import org.sefaria.sefaria.menu.MenuNode;

import java.net.URI;
import java.util.List;

public class TOCActivity extends AppCompatActivity {

    private Book book;
    private String pathDefiningNode;
    private Util.Lang lang;
    private Context context;
    private TOCGrid tocGrid;
    CustomActionbar cab;

    public static Intent getStartTOCActivityIntent(Context superTextActivityThis, Book book, Node currNode){
        Intent intent = new Intent(superTextActivityThis, TOCActivity.class);
        intent.putExtra("currBook", book);
        String pathDefiningNode = currNode.makePathDefiningNode();
        intent.putExtra("pathDefiningNode", pathDefiningNode);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_toc);
        Log.d("TOCActivity", "TOCActivity started");



        if(savedInstanceState == null){//it's coming from a saved to ram state
            Intent intent = getIntent();
            book = intent.getParcelableExtra("currBook");
            pathDefiningNode = intent.getStringExtra("pathDefiningNode");
            Log.d("TOCActivity", "Coming with getIntent");
        }else{
            book = savedInstanceState.getParcelable("currBook");
            pathDefiningNode = savedInstanceState.getString("pathDefiningNode");
            String bookTitle = savedInstanceState.getString("bookTitle");
            Log.d("TOCActivity", "Coming back with savedInstanceState. book:" + book + ". pathDefiningNode: " + pathDefiningNode + ". bookTitle:" + bookTitle);
        }
        lang = MyApp.getMenuLang();
        context = this;

        init();
    }

    private void init() {
        MenuNode titleNode = new MenuNode("Table of Contents","תוכן העניינים",null);
        cab = new CustomActionbar(this, titleNode, lang,homeClick,closeClick,null,null,langClick,null);
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
        Log.e("TOCActvivity", "onSaveInstanceState called (not error)");
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

            Intent intent = new Intent(context,HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); //Clear Activity stack
            startActivity(intent);//TODO make this work with the proper stack order

            finish();
        }
    };

    View.OnClickListener langClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            //TODO change icon
            if(lang == Util.Lang.HE){
                lang = Util.Lang.EN;
            }else if(lang == Util.Lang.EN){
                lang = Util.Lang.HE;
            }else{// if(lang == Util.Lang.BI){
                lang = Util.Lang.EN;
            }
            Log.d("TOCAct", "new lang is: " + lang);
            tocGrid.setLang(lang);
            cab.setLang(lang);
        }

    };

    View.OnClickListener backClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            onBackPressed();
        }
    };

}
