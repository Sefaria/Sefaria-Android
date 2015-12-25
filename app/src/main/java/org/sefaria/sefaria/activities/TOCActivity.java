package org.sefaria.sefaria.activities;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import org.sefaria.sefaria.R;
import org.sefaria.sefaria.TOCElements.TOCGrid;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.database.Book;
import org.sefaria.sefaria.database.Node;
import org.sefaria.sefaria.layouts.CustomActionbar;
import org.sefaria.sefaria.menu.MenuNode;

import java.util.List;

public class TOCActivity extends AppCompatActivity {

    private Book book;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_toc);

        Intent intent = getIntent();
        book = intent.getParcelableExtra("currBook");


        init();
    }

    private void init() {
        MenuNode titleNode = new MenuNode("Table of Contents","תוכן העניינים",null);
        CustomActionbar cab = new CustomActionbar(this, titleNode, Util.Lang.EN,null,closeClick,null,null);
        LinearLayout abRoot = (LinearLayout) findViewById(R.id.actionbarRoot);
        abRoot.addView(cab);


        //ChapNumBox chapNumBox = new ChapNumBox(this,i, Util.Lang.BI);
        List<Node> tocRoots = book.getTOC();
        Log.d("toc","ROOT SIZE " + tocRoots.size());
        Log.d("toc", "TOC " + tocRoots.get(1).getChildren());

        TOCGrid tocGrid = new TOCGrid(this,tocRoots, 4,false,Util.Lang.HE);
        ScrollView tocRoot = (ScrollView) findViewById(R.id.toc_root);
        tocRoot.addView(tocGrid);




    }

    View.OnClickListener closeClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            finish();
        }
    };

}
