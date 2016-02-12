package org.sefaria.sefaria.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MotionEventCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import org.sefaria.sefaria.GoogleTracker;
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
import org.sefaria.sefaria.layouts.LinkDraggerView;
import org.sefaria.sefaria.layouts.PerekTextView;
import org.sefaria.sefaria.layouts.ScrollViewExt;
import org.sefaria.sefaria.MenuElements.MenuNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Created by nss on 1/5/16.
 */
public abstract class SuperTextActivity extends FragmentActivity {
    public enum TextEnums {
        NEXT_SECTION, PREV_SECTION
    }
    protected static int LINK_FRAG_ANIM_TIME = 300; //ms
    public static final int PREV_CHAP_DRAWN = 234234;
    public static final int TOC_CHAPTER_CLICKED_CODE = 3456;
    protected static final int LOAD_PIXEL_THRESHOLD = 500; //num pixels before the bottom (or top) of a segment after (before) which the next (previous) segment will be loaded

    protected static final String LINK_FRAG_TAG = "LinkFragTag";

    protected boolean isTextMenuVisible;
    protected LinearLayout textMenuRoot;

    protected ScrollViewExt textScrollView;
    protected TextMenuBar textMenuBar;
    protected Book book;
    protected List<PerekTextView> perekTextViews;
    protected List<TextChapterHeader> textChapterHeaders;

    protected Node firstLoadedNode;
    protected Node currNode; // Node which you're currently up to in scrollView
    protected Text currText;
    protected Node lastLoadedNode;
    protected Text openToText;

    protected Util.Lang menuLang;
    protected Util.Lang textLang = null;
    protected boolean isCts;
    protected Util.TextBG textBG;
    protected float textSize;
    protected boolean isLoadingSection; //to make sure multiple sections don't get loaded at once
    protected boolean isLoadingInit; //true while init is loading. previous loads should not take place until after isLoadingInit is false

    //link vars
    protected LinkFragment linkFragment;
    protected LinkDraggerView linkDraggerView;
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
            linkFragment = (LinkFragment) getSupportFragmentManager().getFragment(savedInstanceState,LINK_FRAG_TAG);
            nodeHash = savedInstanceState.getInt("nodeHash", -1);
            book = savedInstanceState.getParcelable("currBook");
            openToText = savedInstanceState.getParcelable("incomingLinkText");

            Log.d("save","frag == null = " + (linkFragment == null));
        }else{
            nodeHash = intent.getIntExtra("nodeHash", -1);
            book = intent.getParcelableExtra("currBook");
            openToText = intent.getParcelableExtra("incomingLinkText");

            //LINK FRAGMENT
            linkFragment = new LinkFragment();
            //Bundle args = new Bundle();
            //args.putParcelable(LinkFragment.ARG_CURR_SECTION, sectionAdapter.getItem(position));
            //linkFragment.setArguments(args);
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.add(R.id.linkRoot, linkFragment,LINK_FRAG_TAG);
            fragmentTransaction.commit();
        }
        if(book != null){ //||nodeHash == -1){// that means it came in from the menu or the TOC commentary tab
            try {
                if (openToText != null) {
                    firstLoadedNode = Node.getNodeFromText(openToText, book);
                    if(firstLoadedNode == null){
                        Log.e("SuperTextAct", "firstLoadedNode is null");
                    }
                }else {
                    Settings.BookSettings bookSettings = Settings.BookSettings.getSavedBook(book);
                    textLang = bookSettings.lang;
                    if(bookSettings.node != null) {
                        openToText = new Text(bookSettings.tid);
                        firstLoadedNode = bookSettings.node;
                    }else {//couldn't get saved Node data (most likely you were never at the book, or possibly an error happened).
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
            book = new Book(firstLoadedNode.getBid());
        }
        //These vars are specifically initialized here and not in init() so that they don't get overidden when coming from TOC
        //defaults
        textBG = Util.TextBG.WHITE;
        isCts = false;
        if(textLang == null)
            textLang = Settings.BookSettings.getSavedBook(book).lang;
        textSize = getResources().getDimension(R.dimen.default_text_font_size);
        //end defaults
        isLoadingInit = false;
        menuLang = Settings.getMenuLang();
        if(customActionbar != null)//it's already been set
            customActionbar.setLang(menuLang);
    }

    protected void init() {
        isLoadingInit = true;
        perekTextViews = new ArrayList<>();
        textChapterHeaders = new ArrayList<>();

        isTextMenuVisible = false;
        textMenuRoot = (LinearLayout) findViewById(R.id.textMenuRoot);
        textMenuBar = new TextMenuBar(SuperTextActivity.this,textMenuBtnClick);
        textMenuBar.setState(textLang,isCts,textBG);
        textMenuRoot.addView(textMenuBar);
        textMenuRoot.setVisibility(View.GONE);

        textScrollView = (ScrollViewExt) findViewById(R.id.textScrollView);

        linkDraggerView = (LinkDraggerView) findViewById(R.id.link_dragger);

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

    protected boolean veryFirstTime = true;
    @Override
    protected void onResume() {
        super.onResume();
        GoogleTracker.sendScreen("SuperTextActivity");
        GoogleTracker.sendEvent(GoogleTracker.CATEGORY_NEW_TEXT,book.title);
        if(!veryFirstTime) {
            setMenuLang(Settings.getMenuLang());
        }else
            veryFirstTime = false;

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Settings.BookSettings.setSavedBook(book, currNode, currText, textLang);
        outState.putParcelable("currBook", book);
        getSupportFragmentManager().putFragment(outState, LINK_FRAG_TAG, linkFragment);
    }

    /**
     * used for pressing on link
     * @param context
     * @param text
     */
    public static void startNewTextActivityIntent(Context context, Text text){
        Book book = new Book(text.bid);
        startNewTextActivityIntent(context, book, text, null);
    }

    /**
     * used for coming in from Menu
     * @param context
     * @param book
     */
    public static void startNewTextActivityIntent(Context context, Book book){
        Settings.RecentTexts.addRecentText(book.title);
        startNewTextActivityIntent(context, book, null, null);
    }

    /**
     * used for coming in from DirectRefMenu
     * @param context
     * @param book
     * @param node
     */

    public static void startNewTextActivityIntent(Context context, Book book, Text text, Node node){
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

        if(node == null)
            intent.putExtra("currBook", book);
        if(text != null)
            intent.putExtra("incomingLinkText",text);
        if(node != null){
            node.log();
            Node.saveNode(node);
            intent.putExtra("nodeHash",node.hashCode());
        }
        //lang replaced by general MyApp.getMenuLang() function


        //trick to destroy all activities beforehand
        //ComponentName cn = intent.getComponent();
        //Intent mainIntent = IntentCompat.makeRestartActivityTask(cn);
        //intent.putExtra("menuState", newMenuState);
        context.startActivity(intent);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        comingFromTOC(intent);
    }

    private void comingFromTOC(Intent intent){
        //lang = (Util.Lang) data.getSerializableExtra("lang"); TODO you might need to set lang here if user can change lang in TOC
        int nodeHash = intent.getIntExtra("nodeHash", -1);
        //TODO it might want to try to keep the old loaded sections and just go scroll to it if the new section is already loaded
        firstLoadedNode = Node.getSavedNode(nodeHash);
        lastLoadedNode = null;
        init();
    }

    /*
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("SuperTextAct", "onActivityResult");
        if (data != null) {
            //you're returning to text page b/c chapter was clicked in toc
            if (requestCode == TOC_CHAPTER_CLICKED_CODE) {
                comingFromTOC(data);
            }
        }
    }
    */

    @Override
    public void onBackPressed() {
        Settings.BookSettings.setSavedBook(book, currNode, currText, textLang);
        if(isTextMenuVisible)
            toggleTextMenu();
        else if (linkFragment != null && linkFragment.getIsOpen()) {
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
            node = lastLoadedNode;
        } else if (dir == TextEnums.PREV_SECTION || true){
            node = firstLoadedNode;
        }
        return new Text(node);
    }

    protected void toggleTextMenu() {
        if (isTextMenuVisible) {
            AnimateLinkTMBClose(textMenuRoot);
        } else {
            AnimateLinkTMBOpen(textMenuRoot);
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
            textMenuBar.setState(textLang,isCts,textBG);
            if (!updatedTextSize) toggleTextMenu();
        }
    };


    public Util.Lang getTextLang() { return textLang; }

    public Util.Lang getMenuLang() { return menuLang; }

    public float getTextSize() {
        return textSize;
    }

    public void setTextSize(float textSize) {
        this.textSize = textSize;
    }

    //return the currently selected text, as determined by the link fragment
    public Text getCurrLinkSegment() { return linkFragment.getSegment(); }
    public boolean getFragmentIsOpen() { return linkFragment.getIsOpen(); }

    protected void setMenuLang(Util.Lang menuLang){
        this.menuLang = menuLang;
        customActionbar.setTitleText(currNode.getMenuBarTitle(book, menuLang), menuLang, true, true);
        linkFragment.notifyDataSetChanged();
    }

    public Book getBook() { return book; }

    protected abstract void setTextLang(Util.Lang textLang);
    protected abstract void setIsCts(boolean isCts);
    protected abstract void incrementTextSize(boolean isIncrement);
    protected abstract void jumpToText(Text text);
    protected abstract void updateFocusedSegment();

    protected void setCurrNode(Node node){
        setCurrNode(node,null);
    }
    protected  void setCurrNode(Text text) {
        Node node;
        if (text == null) node = null;
        else node = text.parentNode;
        setCurrNode(node, text);
    }

    private void setCurrNode(Node node, Text text){
        currText = text;
        if(node == null) return;
        if(currNode != node){
            currNode = node;
            customActionbar.setTitleText(currNode.getMenuBarTitle(book, menuLang), menuLang, true, true);

        }
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
            } else if (dir == TextEnums.PREV_SECTION) {
                if (firstLoadedNode == null) {
                    newNode = lastLoadedNode;
                } else {
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
    //TEXT MENU BAR
    //-----
    protected void AnimateLinkTMBOpen(final View v) {
        Animation slide = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
                -1.0f, Animation.RELATIVE_TO_SELF, 0.0f);

        slide.setDuration(LINK_FRAG_ANIM_TIME);
        slide.setFillAfter(true);
        slide.setFillEnabled(true);
        v.startAnimation(slide);

        slide.setAnimationListener(new Animation.AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {v.setVisibility(View.VISIBLE);}

            @Override
            public void onAnimationRepeat(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                v.clearAnimation();
                onFinishLinkFragOpen();
            }

        });

    }

    protected void AnimateLinkTMBClose(final View v) {
        Animation slide = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
                0.0f, Animation.RELATIVE_TO_SELF, -1.0f);

        slide.setDuration(LINK_FRAG_ANIM_TIME);
        slide.setFillAfter(true);
        slide.setFillEnabled(true);
        v.startAnimation(slide);

        slide.setAnimationListener(new Animation.AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationRepeat(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                v.clearAnimation();
                v.setVisibility(View.GONE);
            }

        });

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

                linkDraggerView.setVisibility(View.VISIBLE);
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
                linkDraggerView.setVisibility(View.GONE);

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
