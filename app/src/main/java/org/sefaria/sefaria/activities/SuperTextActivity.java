package org.sefaria.sefaria.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.sefaria.sefaria.R;
import org.sefaria.sefaria.TextElements.TextChapterHeader;
import org.sefaria.sefaria.TextElements.TextMenuBar;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.database.API;
import org.sefaria.sefaria.database.Book;
import org.sefaria.sefaria.database.Node;
import org.sefaria.sefaria.database.Text;
import org.sefaria.sefaria.layouts.CustomActionbar;
import org.sefaria.sefaria.layouts.PerekTextView;
import org.sefaria.sefaria.layouts.ScrollViewExt;
import org.sefaria.sefaria.menu.MenuNode;
import org.sefaria.sefaria.menu.MenuState;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nss on 1/5/16.
 */
public abstract class SuperTextActivity extends Activity {
    public enum TextEnums {
        NEXT_SECTION, PREV_SECTION
    }
    public static final int PREV_CHAP_DRAWN = 234234;
    public static final int TOC_CHAPTER_CLICKED_CODE = 3456;
    protected static final int WHERE_PAGE = 2;
    protected static final int LOAD_PIXEL_THRESHOLD = 500; //num pixels before the bottom (or top) of a segment after (before) which the next (previous) segment will be loaded

    protected boolean isTextMenuVisible;
    protected LinearLayout textMenuRoot;

    protected ScrollViewExt textScrollView;
    protected Book book;
    protected List<PerekTextView> perekTextViews;
    protected List<TextChapterHeader> textChapterHeaders;

    protected Node firstLoadedNode;
    protected Node lastLoadedNode;

    protected Util.Lang lang;
    protected boolean isCts;
    protected float textSize;
    protected boolean isLoadingSection; //to make sure multiple sections don't get loaded at once

    @Override
    protected void onCreate(Bundle in) {
        super.onCreate(in);

        Intent intent = getIntent();
        MenuState menuState;
        menuState = intent.getParcelableExtra("menuState");
        if (in != null) {
            menuState = in.getParcelable("menuState");
        }
        if(menuState != null){//menuState means it came in from the menu
            String title = menuState.getCurrNode().getTitle(Util.Lang.EN);
            book = new Book(title);
            lang = menuState.getLang();
            Node root = book.getTOCroots().get(0);
            firstLoadedNode = Node.getFirstDescendant(root);
        }
        else { // no menuState means it came in from TOC
            lang = (Util.Lang) intent.getSerializableExtra("lang");
            Integer nodeHash = intent.getIntExtra("nodeHash", -1);
            if(in != null){
                lang = (Util.Lang) in.getSerializable("lang");
                nodeHash = in.getInt("nodeHash",-1);
            }

            firstLoadedNode = Node.getSavedNode(nodeHash);
            lastLoadedNode = firstLoadedNode;
            Log.d("Section","firstLoadedChap init:" + firstLoadedNode.getGridNum());
            book = new Book(firstLoadedNode.getBid());
        }
        //These vars are specifically initialized here and not in init() so that they don't get overidden when coming from TOC
        //defaults
        isCts = false;
        lang = Util.Lang.BI;
        textSize = getResources().getDimension(R.dimen.default_text_font_size);
        //end defaults
        setTitle(book.getTitle(lang));
    }


    protected void init() {
        perekTextViews = new ArrayList<>();
        textChapterHeaders = new ArrayList<>();

        isTextMenuVisible = false;
        textMenuRoot = (LinearLayout) findViewById(R.id.textMenuRoot);


        textScrollView = (ScrollViewExt) findViewById(R.id.textScrollView);

        //this specifically comes before menugrid, b/c in tabs it menugrid does funny stuff to currnode
        MenuNode menuNode = new MenuNode(book.getTitle(Util.Lang.EN),book.getTitle(Util.Lang.HE),null); //TODO possibly replace this object with a more general bilinual node
        CustomActionbar cab = new CustomActionbar(this, menuNode, Util.Lang.EN,searchClick,null,titleClick,menuClick);
        LinearLayout abRoot = (LinearLayout) findViewById(R.id.actionbarRoot);
        abRoot.addView(cab);
    }

    protected void toggleTextMenu() {
        if (isTextMenuVisible) {
            textMenuRoot.removeAllViews();
        } else {
            TextMenuBar tmb = new TextMenuBar(SuperTextActivity.this,textMenuBtnClick);
            textMenuRoot.addView(tmb);
        }
        isTextMenuVisible = !isTextMenuVisible;
    }

    View.OnClickListener searchClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(SuperTextActivity.this, HomeActivity.class);
            intent.putExtra("searchClicked",true);
            intent.putExtra("isPopup",true);
            startActivity(intent);
        }
    };

    View.OnClickListener titleClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(SuperTextActivity.this, TOCActivity.class);
            intent.putExtra("currBook",book);
            startActivityForResult(intent, TOC_CHAPTER_CLICKED_CODE);
        }
    };

    View.OnClickListener menuClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            toggleTextMenu();
        }
    };

    View.OnClickListener textMenuBtnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            boolean updatedTextSize = false;
            switch (v.getId()) {
                case R.id.en_btn:
                    setLang(Util.Lang.EN);
                    break;
                case R.id.he_btn:
                    setLang(Util.Lang.HE);
                    break;
                case R.id.bi_btn:
                     setLang(Util.Lang.BI);
                    break;
                case R.id.cts_btn:
                    //can only be cts if not bilingual
                    if (lang != Util.Lang.BI) {
                        setIsCts(true);
                    }
                    break;
                case R.id.sep_btn:
                    setIsCts(false);
                    break;
                case R.id.white_btn:
                    Log.d("text", "WHITE");
                    break;
                case R.id.grey_btn:
                    Log.d("text","GREY");
                    break;
                case R.id.black_btn:
                    Log.d("text","BLACK");
                    break;
                case R.id.small_btn:
                    Log.d("text", "SMALL");


                    if (textSize >= getResources().getDimension(R.dimen.min_text_font_size)-getResources().getDimension(R.dimen.text_font_size_increment)) {
                        textSize -= getResources().getDimension(R.dimen.text_font_size_increment);
                        incrementTextSize(false);
                        updatedTextSize = true;
                    }
                    break;
                case R.id.big_btn:
                    Log.d("text", "BIG");
                    if (textSize <= getResources().getDimension(R.dimen.max_text_font_size)+getResources().getDimension(R.dimen.text_font_size_increment)) {
                        textSize += getResources().getDimension(R.dimen.text_font_size_increment);
                        incrementTextSize(true);
                        updatedTextSize = true;
                    }
                    break;
            }

            if (!updatedTextSize) toggleTextMenu();
        }
    };

    public Util.Lang getLang() { return lang; }

    public float getTextSize() {
        return textSize;
    }

    public void setTextSize(float textSize) {
        this.textSize = textSize;
    }

    protected abstract void setLang(Util.Lang lang);
    protected abstract void setIsCts(boolean isCts);
    protected abstract void incrementTextSize(boolean isIncrement);


    protected List<Text> loadSection(TextEnums dir) {
        Node newNode = null;
        if (dir == TextEnums.NEXT_SECTION) {
            //newNode = lastLoadedNode.getNextTextNode();
            //lastLoadedNode = newNode;
            newNode = firstLoadedNode;
        }

        else if (dir == TextEnums.PREV_SECTION) {
            newNode = lastLoadedNode; //TODO this needs to actually update
        }

        Log.d("Section","firstLoadedChap in loadSection " + firstLoadedNode.getGridNum());



        //int[] levels = {0,currLoadedChapter};
        //Text.getNextChap(book,levels,next);

        List<Text> textsList;
        try {
            textsList = newNode.getTexts();
            //textsList = Text.get(book, levels);
            return textsList;
        } catch (API.APIException e) {
            Toast.makeText(SuperTextActivity.this, "API Exception!!!", Toast.LENGTH_SHORT).show();
            return new ArrayList<>();
        }


    }
}
