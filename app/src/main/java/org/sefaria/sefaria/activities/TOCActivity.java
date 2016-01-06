package org.sefaria.sefaria.activities;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
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
import org.sefaria.sefaria.layouts.CustomActionbar;
import org.sefaria.sefaria.menu.MenuNode;

import java.net.URI;
import java.util.List;

public class TOCActivity extends AppCompatActivity {

    private Book book;
    private Util.Lang lang;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_toc);

        Intent intent = getIntent();
        book = intent.getParcelableExtra("currBook");
        lang = MyApp.getDefaultLang(Util.SETTING_LANG_TYPE.MENU);
        init();
    }

    private void init() {
        MenuNode titleNode = new MenuNode("Table of Contents","תוכן העניינים",null);
        CustomActionbar cab = new CustomActionbar(this, titleNode, lang,null,closeClick,null,null);
        LinearLayout abRoot = (LinearLayout) findViewById(R.id.actionbarRoot);
        abRoot.addView(cab);





        TextView bookTitleTV = (TextView) findViewById(R.id.book_name);
        bookTitleTV.setText(book.getTitle(lang));

        //Log.d("toc", "TOC " + tocRoots.get(0).getChildren());

        List<Node> tocRoots = book.getTOCroots();
        Log.d("toc", "ROOTs SIZE " + tocRoots.size());

        ScrollView tocRoot = (ScrollView) findViewById(R.id.toc_root);
        TOCGrid tocGrid = new TOCGrid(this,tocRoots,false,lang);
        tocRoot.addView(tocGrid);

    }

    View.OnClickListener closeClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            finish();
        }
    };

}
