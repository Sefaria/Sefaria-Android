package org.sefaria.sefaria.activities;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import net.simonvt.menudrawer.MenuDrawer;

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
import org.sefaria.sefaria.database.Searching;
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
public abstract class SuperTextActivity extends FragmentActivity {
    public enum TextEnums {
        NEXT_SECTION, PREV_SECTION
    }

    public static int SEGMENT_SELECTOR_LINE_FROM_TOP = 150; //pixels from top of layout
    public static int MAX_LINK_FRAG_SNAP_DISTANCE = 230;
    protected static int LINK_FRAG_ANIM_TIME = 300; //ms
    protected static CharSequence CONTEXT_MENU_COPY_TITLE = "Copy";
    protected static CharSequence CONTEXT_MENU_SEND_CORRECTION = "Report Mistake";
    protected static CharSequence CONTEXT_MENU_SHARE = "Share";
    protected static CharSequence CONTEXT_MENU_VISIT = "View on Site";
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
    protected int colorTheme;
    protected boolean isSideBySide;

    protected float textSize;
    protected boolean isLoadingSection; //to make sure multiple sections don't get loaded at once
    protected boolean isLoadingInit; //true while init is loading. previous loads should not take place until after isLoadingInit is false

    //link vars
    protected LinkFragment linkFragment;
    //protected LinkDraggerView linkDraggerView;
    /**
     * hacky boolean so that if there's a problem with the on create, the subclasses know not to continue with init (after they call super.onCreate)
     */
    protected boolean badOnCreate = false;
    private CustomActionbar customActionbar;

    protected long openedNewBook = 0; //this is used for Analytics for when someone first goes to a book
    protected int openedNewBookType = 0;
    protected boolean reportedNewBookBack = false;
    protected boolean reportedNewBookTOC = false;
    protected boolean reportedNewBookScroll = false;
    private static final int NO_HASH_NODE = -1;

    private MenuDrawer menuDrawer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*
        menuDrawer = MenuDrawer.attach(this);
        menuDrawer.setContentView(R.layout.activity_section);
        menuDrawer.setMenuView(R.layout.activity_home);
        */

        Log.d("SuperTextActi", "onCreate");
        Intent intent = getIntent();
        Integer nodeHash = NO_HASH_NODE;

        if (savedInstanceState != null) {//it's coming back after it cleared the activity from ram
            linkFragment = (LinkFragment) getSupportFragmentManager().getFragment(savedInstanceState,LINK_FRAG_TAG);
            nodeHash = savedInstanceState.getInt("nodeHash", NO_HASH_NODE);
            book = savedInstanceState.getParcelable("currBook");
            openToText = savedInstanceState.getParcelable("incomingLinkText");
        }else{
            nodeHash = intent.getIntExtra("nodeHash", NO_HASH_NODE);
            book = intent.getParcelableExtra("currBook");
            openToText = intent.getParcelableExtra("incomingLinkText");
        }

        if(book == null && openToText == null && nodeHash == NO_HASH_NODE){
            Log.d("SuperTextActi","appIsFirstOpening");
            MyApp.homeClick(this, false, true);
            try {
                MyApp.dealWithDatabaseStuff(this);
                book = new Book(Settings.getLastBook());
            } catch (Book.BookNotFoundException e) {
                Toast.makeText(this,"Problem getting book",Toast.LENGTH_SHORT).show();
                try{
                    book = new Book("Genesis");
                }catch (Book.BookNotFoundException e1) {
                    e.printStackTrace();
                    badOnCreate = true;
                    return;
                }
            }
        }

        if(linkFragment == null){
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
                if (openToText != null) { //wants to go to specific text (for example, coming from Links or searching).
                    firstLoadedNode = Node.getNodeFromText(openToText, book);
                    if(firstLoadedNode == null){
                        Log.e("SuperTextAct", "firstLoadedNode is null");
                    }
                }else {
                    Settings.BookSettings bookSettings = Settings.BookSettings.getSavedBook(book);
                    textLang = bookSettings.lang;
                    if(bookSettings.node != null) { //opening previously opened book
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
                        Log.d("SuperTextAct", "getFirstDescent... api: and going to get Node.textList");
                        firstLoadedNode = root.getFirstDescendant(true);
                        GoogleTracker.sendEvent(GoogleTracker.CATEGORY_OPEN_NEW_BOOK_ACTION,"Opened New Book");
                        openedNewBook = System.currentTimeMillis();
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
            /*
            if(firstLoadedNode == null){//there's a problem with getting the Node from hash. This happens when there's mutli tabs and restores from ram.
                MyApp.homeClick(this, false,false);
                badOnCreate = true;
                finish();
                return;
            }*/
            book = new Book(firstLoadedNode.getBid());
        }
        //These vars are specifically initialized here and not in init() so that they don't get overidden when coming from TOC
        //defaults
        isCts = false;
        isSideBySide = Settings.getIsSideBySide();
        colorTheme = Settings.getTheme();
        setTheme(colorTheme);

        if(textLang == null)
            textLang = Settings.BookSettings.getSavedBook(book).lang;
        textSize = Settings.getDefaultFontSize();
        //end defaults
        isLoadingInit = false;
        menuLang = Settings.getMenuLang();
        if(customActionbar != null)//it's already been set
            customActionbar.setLang(menuLang);

        Settings.setLastBook(book.title);
    }

    protected void init() {
        isLoadingInit = true;
        perekTextViews = new ArrayList<>();
        textChapterHeaders = new ArrayList<>();

        isTextMenuVisible = false;
        textMenuRoot = (LinearLayout) findViewById(R.id.textMenuRoot);
        textMenuBar = new TextMenuBar(SuperTextActivity.this,textMenuBtnClick);
        textMenuBar.setState(textLang,isCts,isSideBySide,colorTheme);
        textMenuRoot.addView(textMenuBar);
        textMenuRoot.setVisibility(View.GONE);

        textScrollView = (ScrollViewExt) findViewById(R.id.textScrollView);

        //linkDraggerView = (LinkDraggerView) findViewById(R.id.link_dragger);

        //this specifically comes before menugrid, b/c in tabs it menugrid does funny stuff to currnode
        if (customActionbar == null) {
            MenuNode menuNode = new MenuNode("a","b",null); //TODO possibly replace this object with a more general bilinual node
            int catColor = book.getCatColor();
            customActionbar = new CustomActionbar(this, menuNode, menuLang,homeClick,homeLongClick, null,null,titleClick,menuClick,backClick,catColor); //TODO.. I'm not actually sure this should be lang.. instead it shuold be MENU_LANG from Util.S
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
        GoogleTracker.sendEvent(GoogleTracker.CATEGORY_NEW_TEXT, book.title, Settings.lang2Int(textLang));
        if(!veryFirstTime) {
            setMenuLang(Settings.getMenuLang());
        }else
            veryFirstTime = false;

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Settings.BookSettings.setSavedBook(book, currNode, currText, textLang);//data also saved with home click
        outState.putParcelable("currBook", book);
        getSupportFragmentManager().putFragment(outState, LINK_FRAG_TAG, linkFragment);
    }


    /**
     * used for pressing on link
     * @param context
     * @param text
     */
    public static void startNewTextActivityIntent(Context context, Text text, boolean openNewTask){
        Book book = new Book(text.bid);
        startNewTextActivityIntent(context, book, text, null,openNewTask);
    }


    /**
     * used for coming in from Menu
     * @param context
     * @param book
     */
    public static void startNewTextActivityIntent(Context context, Book book, boolean openNewTask){
        Settings.RecentTexts.addRecentText(book.title);
        startNewTextActivityIntent(context, book, null, null,openNewTask);
    }

    /**
     * used for coming in from DirectRefMenu
     * @param context
     * @param book
     * @param node
     */


    public static void startNewTextActivityIntent(Context context, Book book, Text text, Node node,boolean openNewTask) {
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
        if(openNewTask){
            intent = MyApp.startNewTab(intent);
        }
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
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MyApp.REQUEST_WRITE_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    MyApp.dealWithDatabaseStuff(this);
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }


    @Override
    public void onBackPressed() {
        if(openedNewBook >0 && !reportedNewBookBack){
            long time = (System.currentTimeMillis() - openedNewBook);
            if(time <10000) {
                String category;
                if(reportedNewBookScroll || reportedNewBookTOC)
                    category = GoogleTracker.CATEGORY_OPEN_NEW_BOOK_ACTION_2;
                else
                    category = GoogleTracker.CATEGORY_OPEN_NEW_BOOK_ACTION;
                GoogleTracker.sendEvent(category, "Back Pressed", time);
            }
            reportedNewBookBack = true;
        }

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
        textMenuRoot.bringToFront();
        customActionbar.bringToFront();
        if (isTextMenuVisible) {
            //textMenuRoot.setVisibility(View.GONE);
            AnimateLinkTMBClose(textMenuRoot);
        } else {
            //textMenuRoot.setVisibility(View.VISIBLE);
            AnimateLinkTMBOpen(textMenuRoot);
        }
        isTextMenuVisible = !isTextMenuVisible;
    }

    View.OnLongClickListener homeLongClick = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            Settings.BookSettings.setSavedBook(book, currNode, currText, textLang);
            MyApp.homeClick(SuperTextActivity.this, true,false);
            return true;
        }
    };

    private List<Text> searchList;
    private int spotInSearching = 0;

    View.OnClickListener searchClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.d("TextAct","here");
            if(searchList == null) {
                List<Text> list = Searching.findOnPage(currNode, "the");

                searchList = new ArrayList<>();
                searchList.addAll(list);
            }
            if(spotInSearching < searchList.size())
                jumpToText(searchList.get(spotInSearching++));
            else{
                Toast.makeText(SuperTextActivity.this,"No more results.",Toast.LENGTH_SHORT).show();
            }

        }
    };

    View.OnClickListener homeClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Settings.BookSettings.setSavedBook(book, currNode, currText, textLang);
            MyApp.homeClick(SuperTextActivity.this, false,false);
        }
    };

    View.OnClickListener titleClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(openedNewBook >0 && !reportedNewBookTOC){
                String category;
                if(reportedNewBookScroll || reportedNewBookBack)
                    category = GoogleTracker.CATEGORY_OPEN_NEW_BOOK_ACTION_2;
                else
                    category = GoogleTracker.CATEGORY_OPEN_NEW_BOOK_ACTION;
                GoogleTracker.sendEvent(category,"Opening TOC", (System.currentTimeMillis() - openedNewBook));
                reportedNewBookTOC = true;
            }
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
                    setIsCts(true);
                    break;
                case R.id.sep_btn:
                    setIsCts(false);
                    break;
                case R.id.sbs_btn:
                    setIsSideBySide(true);
                    break;
                case R.id.tb_btn:
                    setIsSideBySide(false);
                    break;
                case R.id.white_btn:
                    setColorTheme(R.style.SefariaTheme_White);
                    break;
                case R.id.grey_btn:
                    break;
                case R.id.black_btn:
                    setColorTheme(R.style.SefariaTheme_Black);
                    break;
                case R.id.small_btn:


                    if (textSize > getResources().getDimension(R.dimen.min_text_font_size)) {
                        incrementTextSize(false);
                        updatedTextSize = true;
                    }
                    break;
                case R.id.big_btn:
                    if (textSize < getResources().getDimension(R.dimen.max_text_font_size)) {
                        incrementTextSize(true);
                        updatedTextSize = true;
                    }
                    break;
            }
            textMenuBar.setState(textLang,isCts,isSideBySide,colorTheme);
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
    public boolean getIsCts(){ return isCts;}
    public boolean getIsSideBySide() { return isSideBySide; }
    public int getColorTheme() { return colorTheme; }
    private void setColorTheme(int colorTheme) {
        Settings.setTheme(colorTheme);
        finish();
        startNewTextActivityIntent(this,book,currText,currNode,false);
        /*Intent intent = new Intent(this, .class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);*/
    }

    protected abstract void setTextLang(Util.Lang textLang);
    protected abstract void setIsCts(boolean isCts);
    protected void setIsSideBySide(boolean isSideBySide){
        this.isSideBySide = isSideBySide;
        Settings.setIsSideBySide(isSideBySide);
    }

    protected void incrementTextSize(boolean isIncrement){
        float increment = getResources().getDimension(R.dimen.text_font_size_increment);
        if (isIncrement) textSize  += increment;
        else textSize -= increment;
        Settings.setDefaultFontSize(textSize);
    }


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
            Log.d("SuperTextAct", "trying to getTexts");
            if(newNode == null){//This error occurs when using API and the book no longer exists in Sefaria (it could also happen other times we don't know about)
                //TODO add error text into the list.
                finish();
                return new ArrayList<>();
            }
            textsList = newNode.getTexts();
            return textsList;
        } catch (API.APIException e) {
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
                //onFinishLinkFragOpen();
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

    public int getLinkFragMaxHeight() {
        return findViewById(R.id.root).getHeight()-findViewById(R.id.actionbarRoot).getHeight();
    }


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

                //linkDraggerView.setVisibility(View.VISIBLE);
            }

        });

    }

    public void AnimateLinkFragClose(final View v) {
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
                //linkDraggerView.setVisibility(View.GONE);

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

                if (v.getHeight() < MAX_LINK_FRAG_SNAP_DISTANCE) {
                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) v.getLayoutParams();
                    params.height = MAX_LINK_FRAG_SNAP_DISTANCE;
                    v.setLayoutParams(params);
                }

            }

        });

    }

}
