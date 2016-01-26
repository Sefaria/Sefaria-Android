package org.sefaria.sefaria.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Settings;
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
import org.sefaria.sefaria.MenuElements.MenuNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by nss on 1/5/16.
 */
public abstract class SuperTextActivity extends Activity {
    public enum TextEnums {
        NEXT_SECTION, PREV_SECTION
    }
    protected static int LINK_FRAG_ANIM_TIME = 300; //ms
    public static final int PREV_CHAP_DRAWN = 234234;
    public static final int TOC_CHAPTER_CLICKED_CODE = 3456;
    protected static final int LOAD_PIXEL_THRESHOLD = 500; //num pixels before the bottom (or top) of a segment after (before) which the next (previous) segment will be loaded

    protected boolean isTextMenuVisible;
    protected LinearLayout textMenuRoot;

    protected ScrollViewExt textScrollView;
    protected Book book;
    protected List<PerekTextView> perekTextViews;
    protected List<TextChapterHeader> textChapterHeaders;

    protected Node firstLoadedNode;
    protected Node currNode; // Node which you're currently up to in scrollView
    protected Node lastLoadedNode;
    protected Text openToText;

    protected Util.Lang menuLang;
    protected Util.Lang textLang;
    protected boolean isCts;
    protected float textSize;
    protected boolean isLoadingSection; //to make sure multiple sections don't get loaded at once
    protected boolean isLoadingInit; //true while init is loading. previous loads should not take place until after isLoadingInit is false

    //link vars
    protected LinkFragment linkFragment;
    /**
     * hacky boolean so that if there's a problem with the on create, the subclasses know not to continue with init (after they call super.onCreate)
     */
    protected boolean badOnCreate = false;
    private CustomActionbar customActionbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        Integer nodeHash;
        if (savedInstanceState != null) {//it's coming back after it cleared the activity from ram
            nodeHash = savedInstanceState.getInt("nodeHash", -1);
            book = savedInstanceState.getParcelable("currBook");
            openToText = savedInstanceState.getParcelable("incomingLinkText");
        }else{
            nodeHash = intent.getIntExtra("nodeHash", -1);
            book = intent.getParcelableExtra("currBook");
            openToText = intent.getParcelableExtra("incomingLinkText");
        }
        if(book != null){ //||nodeHash == -1){// that means it came in from the menu or the TOC commentary tab
            try {
                if (openToText != null) {
                    firstLoadedNode = Node.getNodeFromText(openToText, book);
                    if(firstLoadedNode == null){
                        Log.e("SuperTextAct", "firstLoadedNode is null");
                    }
                }else {
                    try {
                        firstLoadedNode = Settings.getSavedBook(book);
                    } catch (Node.InvalidPathException e) {//couldn't get saved Node data (most likely you were never at the book, or possibly an error happened).
                        Log.e("SuperTextAct", "Problem gettting saved book data");
                        List<Node> TOCroots = book.getTOCroots();
                        if (TOCroots.size() == 0) {
                            Toast.makeText(this, "Unable to load Table of Contents for this book", Toast.LENGTH_SHORT).show();
                            badOnCreate = true;
                            return;
                        }
                        Node root = TOCroots.get(0);
                        firstLoadedNode = root.getFirstDescendant();
                    }
                }
            }catch (API.APIException e) {
                Toast.makeText(this,"Problem getting data from internet", Toast.LENGTH_SHORT).show();
                badOnCreate = true;
                return;
            }
            //lastLoadedNode = firstLoadedNode; PURPOSEFULLY NOT INITIALLIZING TO INDICATE THAT NOTHING HAS BEEN LOADED YET
        }
        else { // no book means it came in from TOC
            firstLoadedNode = Node.getSavedNode(nodeHash);
            //lastLoadedNode = firstLoadedNode;
            Log.d("Section","firstLoadedChap init:" + firstLoadedNode.getNiceGridNum(menuLang));
            book = new Book(firstLoadedNode.getBid());
        }
        //These vars are specifically initialized here and not in init() so that they don't get overidden when coming from TOC
        //defaults
        isCts = false;
        textLang = MyApp.getDefaultTextLang();
        textSize = getResources().getDimension(R.dimen.default_text_font_size);
        //end defaults
        isLoadingInit = false;
        menuLang = MyApp.getMenuLang();
        if(customActionbar != null)//it's already been set
            customActionbar.setLang(menuLang);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable("currBook", book);
    }

    public static void startNewTextActivityIntent(Context context, Text text){
        Book book = new Book(text.bid);
        startNewTextActivityIntent(context,book,text);
    }

    public static void startNewTextActivityIntent(Context context, Book book){
        startNewTextActivityIntent(context,book,null);
    }

    private static void startNewTextActivityIntent(Context context, Book book, Text text){
        List<String> cats = Arrays.asList(book.categories);
        boolean isCtsText = false;
        final String[] CTS_TEXT_CATS = {};// {"Tanach","Talmud"};//
        for (String ctsText : CTS_TEXT_CATS) {
            isCtsText = cats.contains(ctsText);
            if (isCtsText) break;
        }
        Intent intent;
        if (isCtsText) {
            intent = new Intent(context, TextActivity.class);
        } else {
            intent = new Intent(context, SectionActivity.class);
        }

        intent.putExtra("currBook", book);
        if(text != null)
            intent.putExtra("incomingLinkText",text);
        //lang replaced by general MyApp.getMenuLang() function


        //trick to destroy all activities beforehand
        //ComponentName cn = intent.getComponent();
        //Intent mainIntent = IntentCompat.makeRestartActivityTask(cn);
        //intent.putExtra("menuState", newMenuState);
        context.startActivity(intent);
    }


    protected void init() {
        isLoadingInit = true;
        perekTextViews = new ArrayList<>();
        textChapterHeaders = new ArrayList<>();

        isTextMenuVisible = false;
        textMenuRoot = (LinearLayout) findViewById(R.id.textMenuRoot);

        textScrollView = (ScrollViewExt) findViewById(R.id.textScrollView);

        //this specifically comes before menugrid, b/c in tabs it menugrid does funny stuff to currnode
        if (customActionbar == null) {
            MenuNode menuNode = new MenuNode("a","b",null); //TODO possibly replace this object with a more general bilinual node
            int catColor = book.getCatColor();
            customActionbar = new CustomActionbar(this, menuNode, menuLang,homeClick,null,null,titleClick,menuClick,backClick,catColor); //TODO.. I'm not actually sure this should be lang.. instead it shuold be MENU_LANG from Util.S
            LinearLayout abRoot = (LinearLayout) findViewById(R.id.actionbarRoot);
            abRoot.addView(customActionbar);
            customActionbar.setLang(menuLang);
        }
        setCurrNode(firstLoadedNode);

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null) {

            //you're returning to text page b/c chapter was clicked in toc
            if (requestCode == TOC_CHAPTER_CLICKED_CODE) {
                //lang = (Util.Lang) data.getSerializableExtra("lang"); TODO you might need to set lang here if user can change lang in TOC
                int nodeHash = data.getIntExtra("nodeHash", -1);
                //TODO it might want to try to keep the old loaded sections and just go scroll to it if the new section is already loaded
                firstLoadedNode = Node.getSavedNode(nodeHash);
                lastLoadedNode = null;
                init();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (linkFragment != null && linkFragment.getIsOpen()) {
            if (linkFragment.getCurrState() == LinkFragment.State.MAIN) {
                View linkRoot = findViewById(R.id.linkRoot);
                AnimateLinkFragClose(linkRoot);
            } else { //In CAT or BOOK state
                linkFragment.gotoState(LinkFragment.State.MAIN,linkFragment.getView(),null);
            }
        } else {
            super.onBackPressed();
        }
    }

    protected Text getSectionHeaderText(TextEnums dir){
        Node node;
        if(dir == TextEnums.NEXT_SECTION) {
            Log.d("SuperTextAct","using lastLoadedNode for getSectionHeaderText()");
            node = lastLoadedNode;
        } else if (dir == TextEnums.PREV_SECTION || true){
            Log.d("SuperTextAct","using firstLoadedNode for getSectionHeaderText()");
            node = firstLoadedNode;
        }
        return new Text(node);
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

    View.OnClickListener homeClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(SuperTextActivity.this, HomeActivity.class);
            intent.putExtra("homeClicked",true);
            intent.putExtra("isPopup",true);
            startActivity(intent);
        }
    };

    View.OnClickListener titleClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = TOCActivity.getStartTOCActivityIntent(SuperTextActivity.this, book,firstLoadedNode);
            startActivityForResult(intent, TOC_CHAPTER_CLICKED_CODE);
        }
    };

    View.OnClickListener menuClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            toggleTextMenu();
        }
    };

    View.OnClickListener backClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            onBackPressed();
        }
    };

    View.OnClickListener textMenuBtnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            boolean updatedTextSize = false;
            switch (v.getId()) {
                case R.id.en_btn:
                    setTextLang(Util.Lang.EN);
                    break;
                case R.id.he_btn:
                    setTextLang(Util.Lang.HE);
                    break;
                case R.id.bi_btn:
                    setTextLang(Util.Lang.BI);
                    break;
                case R.id.cts_btn:
                    //can only be cts if not bilingual
                    if (textLang != Util.Lang.BI) {
                        setIsCts(true);
                    }
                    setIsCts(true);
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


    public Util.Lang getTextLang() { return textLang; }

    public float getTextSize() {
        return textSize;
    }

    public void setTextSize(float textSize) {
        this.textSize = textSize;
    }

    public Book getBook() { return book; }

    protected abstract void setTextLang(Util.Lang textLang);
    protected abstract void setMenuLang(Util.Lang menuLang);
    protected abstract void setIsCts(boolean isCts);
    protected abstract void incrementTextSize(boolean isIncrement);
    protected abstract void jumpToText(Text text);

    protected void setCurrNode(Node node){
        if(node == null) return;
        if(currNode == node) return;

        currNode = node;
        customActionbar.setTitleText(currNode.getMenuBarTitle(book,menuLang), menuLang, true, true);
        Settings.setSavedBook(book,currNode);


    }
    protected  void setCurrNode(Text text) {
        Node node = text.parentNode;
        setCurrNode(node);
    }


    protected List<Text> loadSection(TextEnums dir) {
        Node newNode = null;
        try {
            if (dir == TextEnums.NEXT_SECTION) {
                if (lastLoadedNode == null) { //this is the initial load
                    newNode = firstLoadedNode;
                } else {
                    newNode = lastLoadedNode.getNextTextNode();
                }
                lastLoadedNode = newNode;
            }

            else if (dir == TextEnums.PREV_SECTION) {
                if(firstLoadedNode == null){
                    newNode = lastLoadedNode;
                }else{
                    newNode = firstLoadedNode.getPrevTextNode();
                }
                firstLoadedNode = newNode;
            }
        } catch (Node.LastNodeException e) {
            return new ArrayList<>();
        }


        List<Text> textsList;
        try {
            textsList = newNode.getTexts();
            return textsList;
        } catch (API.APIException e) {
            Toast.makeText(SuperTextActivity.this, "API Exception!!!", Toast.LENGTH_SHORT).show();
            return new ArrayList<>();
        }


    }



    //-----
    //LINK FRAGMENT
    //-----

    protected abstract void onFinishLinkFragOpen();
    protected abstract void onStartLinkFragClose();

    //Thank you Farhan Shah! https://stackoverflow.com/questions/20323628/android-layout-animations-from-bottom-to-top-and-top-to-bottom-on-imageview-clic

    protected void AnimateLinkFragOpen(final View v) {
        Animation slide = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
                1.0f, Animation.RELATIVE_TO_SELF, 0.0f);

        slide.setDuration(LINK_FRAG_ANIM_TIME);
        slide.setFillAfter(true);
        slide.setFillEnabled(true);
        v.startAnimation(slide);

        slide.setAnimationListener(new Animation.AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {

                v.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {

                v.clearAnimation();

                onFinishLinkFragOpen();
                linkFragment.setDontUpdate(false);
                linkFragment.setIsOpen(true);
            }

        });

    }

    protected void AnimateLinkFragClose(final View v) {
        Animation slide = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
                0.0f, Animation.RELATIVE_TO_SELF, 1.0f);

        slide.setDuration(LINK_FRAG_ANIM_TIME);
        slide.setFillAfter(true);
        slide.setFillEnabled(true);
        v.startAnimation(slide);

        slide.setAnimationListener(new Animation.AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
                linkFragment.setIsOpen(false);
                linkFragment.setDontUpdate(true);

                onStartLinkFragClose();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {

                v.clearAnimation();

                v.setVisibility(View.GONE);

            }

        });

    }
}
