package org.sefaria.sefaria.activities;

import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.sefaria.sefaria.layouts.CustomActionbar;
import org.sefaria.sefaria.layouts.JustifyTextView;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.layouts.PerekTextView;
import org.sefaria.sefaria.layouts.ScrollViewExt;
import org.sefaria.sefaria.layouts.ScrollViewListener;
import org.sefaria.sefaria.layouts.TextMenuBar;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.layouts.VerseSpannable;
import org.sefaria.sefaria.database.Book;
import org.sefaria.sefaria.database.Text;
import org.sefaria.sefaria.menu.MenuState;

import java.util.ArrayList;
import java.util.List;

public class TextActivity extends AppCompatActivity {

    private MenuState menuState;
    private boolean isTextMenuVisible;
    private LinearLayout textMenuRoot;
    private LinearLayout textRoot;
    private ScrollViewExt textScrollView;
    private Book book;
    private int currLoadedChapter;
    private List<PerekTextView> perekTextViews;

    @Override
    protected void onCreate(Bundle in) {
        super.onCreate(in);
        setContentView(R.layout.activity_text);

        Intent intent = getIntent();
        menuState = intent.getParcelableExtra("menuState");
        if (in != null) {
            menuState = in.getParcelable("menuState");
        }
        init();
    }

    private void init() {
        perekTextViews = new ArrayList<>();
        currLoadedChapter = 0;
        isTextMenuVisible = false;
        textMenuRoot = (LinearLayout) findViewById(R.id.textMenuRoot);
        textRoot = (LinearLayout) findViewById(R.id.textRoot);
        textScrollView = (ScrollViewExt) findViewById(R.id.textScrollView);
        textScrollView.setScrollViewListener(new ScrollViewListener() {
             @Override
             public void onScrollChanged(ScrollViewExt scrollView, int x, int y, int oldx, int oldy) {
                 // We take the last son in the scrollview
                 View view = (View) scrollView.getChildAt(scrollView.getChildCount() - 1);
                 int diff = (view.getBottom() - (scrollView.getHeight() + scrollView.getScrollY()));

                 // if diff is zero, then the bottom has been reached
                 if (diff <= 0) {
                     loadSection();
                 }
             }
         });

                setTitle(menuState.getCurrNode().getTitle(menuState.getLang()));

        //this specifically comes before menugrid, b/c in tabs it menugrid does funny stuff to currnode
        CustomActionbar cab = new CustomActionbar(this, menuState.getCurrNode(), Util.EN,searchClick,null,null,menuClick);
        LinearLayout abRoot = (LinearLayout) findViewById(R.id.actionbarRoot);
        abRoot.addView(cab);



        String title = menuState.getCurrNode().getTitle(Util.EN);
        book = new Book(title);


        loadSection();
    }

    private void loadSection() {
        currLoadedChapter++;
        int[] levels = {0,currLoadedChapter};
        List<Text> textsList = Text.get(book, levels);
        PerekTextView content = new PerekTextView(this,textsList);
        perekTextViews.add(content);
        content.setTextSize(20);


        //content.setIsCts(true);
        //content.invalidate();

        textRoot.addView(content);
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putParcelable("menuState", menuState);
    }

    View.OnClickListener searchClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(TextActivity.this, HomeActivity.class);
            intent.putExtra("searchClicked",true);
            intent.putExtra("isPopup",true);
            startActivity(intent);
        }
    };

    View.OnClickListener menuClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (isTextMenuVisible) {
                textMenuRoot.removeAllViews();
            } else {
                TextMenuBar tmb = new TextMenuBar(TextActivity.this,textMenuBtnClick);
                textMenuRoot.addView(tmb);
            }
            isTextMenuVisible = !isTextMenuVisible;
        }
    };

    View.OnClickListener textMenuBtnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.en_btn:
                    Log.d("text","EN");
                    for (PerekTextView ptv: perekTextViews) {
                        ptv.setLang(Util.EN);
                    }
                    return;
                case R.id.he_btn:
                    Log.d("text","HE");
                    for (PerekTextView ptv: perekTextViews) {
                        ptv.setLang(Util.HE);
                    }
                    return;
                case R.id.bi_btn:
                    Log.d("text","BI");
                    for (PerekTextView ptv: perekTextViews) {
                        ptv.setLang(Util.BI);
                    }
                    return;
                case R.id.cts_btn:
                    Log.d("text","CTS");
                    for (PerekTextView ptv : perekTextViews) {
                        ptv.setIsCts(true);
                    }
                    return;
                case R.id.sep_btn:
                    Log.d("text","SEP");
                    for (PerekTextView ptv : perekTextViews) {
                        ptv.setIsCts(false);
                    }
                    return;
                case R.id.white_btn:
                    Log.d("text","WHITE");
                    return;
                case R.id.grey_btn:
                    Log.d("text","GREY");
                    return;
                case R.id.black_btn:
                    Log.d("text","BLACK");
                    return;
                case R.id.small_btn:
                    Log.d("text","SMALL");
                    return;
                case R.id.big_btn:
                    Log.d("text","BIG");
                    return;
            }
        }
    };

}
