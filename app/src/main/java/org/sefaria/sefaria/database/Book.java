package org.sefaria.sefaria.database;
import org.json.JSONArray;
import org.sefaria.sefaria.GoogleTracker;
import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.Settings;
import org.sefaria.sefaria.Util;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;


import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class Book implements Parcelable {

    private List<Book> allCommentaries = null;
    public int bid;
    private int commentsOn;
    private String  [] sectionNames;
    /**
     * Little sections (like verse) to Big (like chap) just like Segment.levels works
     */
    public String  [] sectionNamesL2B;
    /**
     * Little sections (like verse) to Big (like chap) just like Segment.levels works
     */
    public String  [] heSectionNamesL2B;
    public String [] categories;
    public int textDepth;
    public int wherePage;
    public String title;
    public String heTitle;
    public int languages;
    private String enCollectiveTitle;
    private String heCollectiveTitle;

    private static final int DEFAULT_WHERE_PAGE = 2;
    public Book(){
        //empty constructor
        wherePage = DEFAULT_WHERE_PAGE; //this is the default value for it.
    }
    public Book(Cursor cursor){
        wherePage = DEFAULT_WHERE_PAGE; //this is the default value for it.
        getFromCursor(cursor);
    }

    public Book(String title) throws BookNotFoundException {
        wherePage = DEFAULT_WHERE_PAGE;
        get(title, false);
    }

    public Book(String title,boolean searchHeAndEnTitles) throws BookNotFoundException {
        wherePage = DEFAULT_WHERE_PAGE;
        get(title,searchHeAndEnTitles);
    }

    private Book(boolean dummy){
        if(dummy){
            wherePage = DEFAULT_WHERE_PAGE;
            title = "Book Error";
            bid = 0;
            heTitle = "Book Error";
            sectionNames = new String[]{};
            sectionNamesL2B  = new String[]{};
            heSectionNamesL2B = new String[]{};
            categories = new String[]{};
        }
    }
    public static Book dummyBook = new Book();

    /**
     *  returns a list of roots to trees that represent the Table of Contents for a book.
     * @return roots
     */
    public List<Node> getTOCroots() throws API.APIException {
        List<Node> TOCroots;
        TOCroots = Node.getRoots(this);
        return TOCroots;
    }

    public Node getNodeFromPathStr(String path) throws Node.InvalidPathException, API.APIException {
        return Node.getNodeFromPathStr(this,path);
    }

    public String getTitle(Util.Lang lang){
        if(Util.Lang.EN == lang)
            return title;
        else if(Util.Lang.HE == lang)
            return heTitle;
        else if(Util.Lang.BI == lang) {
            return title + " - " + heTitle;
        }
        else{
            Log.e("Book", "wrong lang num");
            return "title";
        }
    }


    public String getCategories(){
        String string = "";
        for(int i=0;i<categories.length;i++){
            String cat;
            if(i<categories.length -1)
                cat = categories[i] + ", ";
            else
                cat = categories[i];
            string += cat;
        }
        return string;
    }


    @Override
    public String toString() {
        return "bid: " + bid  + " commentsOn: " + commentsOn + " sectionNames: " + sectionNames + " categories: "+ categories +
                "textDepth: " + textDepth + "wherePage: " + wherePage +" title: "  + title + "heTitle: " + heTitle + Klanguages + languages;
    }

    public void log(){
        Log.d("sql_dis",toString());
    }

    public static final String TABLE_BOOKS = "Books";
    private static final String KcommentsOn = "commentsOn";
    private static final String KsectionNames = "sectionNames";
    private static final String Kcategories = "categories";
    private static final String KtextDepth = "textDepth";
    private static final String KwherePage = "wherePage";
    private static final String Klengths = "lengths";
    private static final String Ktitle = "title";
    private static final String KheTitle = "heTitle";

    private static final String KdataVersion = "dataVersion";
    private static final String KversionTitle = "versionTitle";
    private static final String Kversions = "versions";
    private static final String Klanguages = "languages"; // en is 1, he is 2, both is 3.
    private static final String KheSectionNames = "heSectionNames"; // en is 1, he is 2, both is 3.


    public static void add(SQLiteDatabase db1, JSONObject json) throws JSONException {
        add(db1, json, false); // you want to add a completely new thing (without updating).
    }

    public static void add(SQLiteDatabase db, JSONObject json, boolean shouldUpdate) throws JSONException {
        //DatabaseHandler dbHandler = DatabaseHandler.getInstance(MyApp.context);
        //SQLiteDatabase db = dbHandler.getWritableDatabase();

        ContentValues values = new ContentValues();
        String title = json.getString(Ktitle);
        //values.put(KcommentsOn, json.getString(KcommentsOn)); // comments on is something that we will add on our own.
        values.put(KsectionNames, json.getString(KsectionNames).replace("\"\"", "\"Section\"")); //replaced empty SectionsNames with the default name "Section"
        //	values.put(Kcategories, json.getString(Kcategories));
        values.put(KtextDepth, json.getInt(KtextDepth));
        if(json.getInt(KtextDepth) == 1)
            values.put(KwherePage, 1);
        //values.put(KwherePage, json.getString(KwherePage)); // wherePage is something that we will add on our own.
        //	values.put(Klengths, json.getString(Klengths));
        //TODO MAKE THESE VALUES WORK>>>>>>>>>!!!!!
        values.put(Ktitle, title);
        try{
            values.put(KheTitle,json.getString(KheTitle));
        }catch(JSONException e){
            GoogleTracker.sendException(e);
            values.put(KheTitle,json.getString(Ktitle));
        }

        String langString = json.getString("language");
        int lang = getNum(title, Klanguages) | returnLangNums(langString); //the updated langs value
        values.put(Klanguages, String.valueOf(lang));

        if(shouldUpdate){
            if(db.update(TABLE_BOOKS, values, Ktitle + "=?", new String [] {title}) == 1)
                ;//Log.d("sql_add_book", "Updated book info: " + title + " " + langString);
            else
                Log.e("sql_add_book", "Couldn't update book info: " + title + " " + langString);
        }
        else{ //you are adding a new thing (not updating it).
            if(db.insert(TABLE_BOOKS, null, values) != -1)
                ;//Log.d("sql_add_book", "Added new book info: " + title  + " " + langString);
            else
                Log.e("sql_add_book", "Couldn't add book info: " + title + " " + langString);
        }
        //db.close(); // Closing database connection
    }

    public static int returnLangNums(String langString){
        int langs = 0;
        if(langString.equals("en"))
            langs = 1;
        else if(langString.equals("he"))
            langs = 2;
        return langs;

    }

	/*public static void remove(String title){
		DatabaseHandler dbHandler = DatabaseHandler.getInstance(MyApp.context);
		SQLiteDatabase db = dbHandler.getWritableDatabase();
		db.delete(TABLE_BOOKS, Ktitle + "=?", new String [] {title});
		db.close(); // Closing database connection
		Log.d("sql_remove_book", title + " removed.");
	}*/



    private void getFromCursor(Cursor cursor){
        try{
            bid = cursor.getInt(0);
            commentsOn = cursor.getInt(1);
            sectionNames = Util.str2strArray(cursor.getString(2));
            sectionNamesL2B = new String [sectionNames.length];
            for(int i = 0; i<sectionNames.length;i++)
                sectionNamesL2B[i] = sectionNames[sectionNames.length - i -1];
            categories = Util.str2strArray(cursor.getString(3));
            textDepth = cursor.getInt(4);
            wherePage = cursor.getInt(5);
            title = cursor.getString(7);
            heTitle = cursor.getString(8);
            languages = cursor.getInt(cursor.getColumnIndex(Klanguages));
            String [] heSectionNamesTemp = Util.str2strArray(cursor.getString(cursor.getColumnIndex(KheSectionNames)));
            heSectionNamesL2B = new String [heSectionNamesTemp.length];
            for(int i = 0; i<heSectionNamesTemp.length;i++)
                heSectionNamesL2B[i] = heSectionNamesTemp[heSectionNamesTemp.length - i -1];
            //try: //path // try is needed in case it's an old DB.
            try{
                //if it's blank(or null) it will use the regular title with " on .*" filtered out
                enCollectiveTitle = cursor.getString(18);
                heCollectiveTitle = cursor.getString(19);
                //not doing b/c its not needed for anything and is using a weird
                // format "[(1),(2),(6),]" so not worth the time trying to parse
                //commentsOnMultiple = Util.str2intArray(cursor.getString(17));
            }catch (Exception e){
            }

        }
        catch(Exception e){
            GoogleTracker.sendException(e);
            bid = 0;
            return;
        }

    }


    public static class BookNotFoundException extends Exception{
        public BookNotFoundException(){
            super("Book not found error");
        }
        public BookNotFoundException(String message){
            super(message);
        }
        private static final long serialVersionUID = 1L;
    }

    public void get(String title, boolean searchHeAndEnTitles) throws BookNotFoundException{
        SQLiteDatabase db = Database.getDB();
        Cursor cursor = db.query(TABLE_BOOKS, null, Ktitle + "=?",
                new String[] { title }, null, null, null, null);

        try{
            if (cursor.moveToFirst()){
                getFromCursor(cursor);
            }
            else if(searchHeAndEnTitles){
                cursor.close();
                cursor = db.query(TABLE_BOOKS, null, KheTitle + "=?",
                        new String[] { title }, null, null, null, null);
                if (cursor.moveToFirst()){
                    getFromCursor(cursor);
                }
            }
            else
                throw new BookNotFoundException();
        }finally {
            cursor.close();
        }
    }

    private static HashMap<Integer, Book> bid2Book = null;

    public static Book getByBid(int bid) throws BookNotFoundException {
        if(bid2Book == null) {
            bid2Book = new HashMap<>();
            SQLiteDatabase db = Database.getDB();
            Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_BOOKS, null);
            if (cursor.moveToFirst()) {
                do {
                    Book book = new Book(cursor);
                    bid2Book.put(book.bid, book);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        Book book = bid2Book.get(bid);
        if(book == null)
            throw new BookNotFoundException();
        else
            return book;
    }


    /**
     *
     * @param title
     * @return the bid or 0 if error
     */
    public static int getBid(String title) throws BookNotFoundException {
        SQLiteDatabase db = Database.getDB();
        Cursor cursor = db.query(TABLE_BOOKS, new String[]{"_id"}, Ktitle + "=?",
                new String[]{title}, null, null, null, null);
        try {
            if (cursor.moveToFirst()) {
                int bid = cursor.getInt(0);
                return bid;
            } else {
                throw new BookNotFoundException();
            }
        } catch (Exception e) {
            throw new BookNotFoundException();
        }finally {
            cursor.close();
        }
    }

    public static String getTitle(int bid){
        SQLiteDatabase db = Database.getDB();
        Cursor cursor = db.query(TABLE_BOOKS, new String[]{"title"}, "_id=?",
                new String[]{String.valueOf(bid)}, null, null, null, null);

        if (cursor.moveToFirst()){
            try{
                String title = cursor.getString(0);
                return title;
            }catch(Exception e){
                GoogleTracker.sendException(e, "" + bid);
                return ""; //I'm having a problem... I assume it means that this book isn't in the database.
            }finally {
                cursor.close();
            }

        }
        return "";
    }

    //I'm not sure if this function is ever used.
    private static int getNum(String title, String type){
        SQLiteDatabase db = Database.getDB();

        Cursor cursor = db.query(TABLE_BOOKS, new String[]{type}, Ktitle + "=?",
                new String[]{title}, null, null, null, null);

        if (cursor != null){
            cursor.moveToFirst();
            try{
                return cursor.getInt(0);//the type you wanted
            }catch(Exception e){
                GoogleTracker.sendException(e, title);
                return 0; //I'm having a problem... I assume it means that this book isn't in the database.
            }finally {
                cursor.close();
            }

        }
        return 0;
    }

    public static String getJSONObjectString(JSONObject json) {
        StringBuilder sb = new StringBuilder("{");
        Iterator<?> keys = json.keys();
        try {
            while (keys.hasNext()) {
                String key = (String) keys.next();
                sb.append("\"");
                sb.append(key);
                sb.append("\"");
                sb.append(": ");
                if (json.get(key) instanceof String) {
                    sb.append("\"");
                    sb.append(json.getString(key));
                    sb.append("\"");
                }  else if (json.get(key) instanceof Integer) {
                    sb.append(json.getInt(key));
                } else if (json.get(key) instanceof Long) {
                    sb.append(json.getLong(key));
                } else if (json.get(key) instanceof Boolean) {
                    sb.append(json.getBoolean(key));
                }else {
                    Object[] tempArray = (Object[])json.get(key);
                    String arrayString = "[]";
                    if (tempArray != null) {
                        arrayString = "[" + tempArray.toString() + "]";
                    }
                    sb.append(arrayString);
                }
                if (keys.hasNext())
                    sb.append(",");
            }
            sb.append("}");
            return sb.toString();
        } catch (JSONException e) {
            return "";
        }
    }

    public List<Book> getAllCommentaries(){
        if(allCommentaries != null)
            return allCommentaries;
        SQLiteDatabase db = Database.getDB();
        List<Book> bookList = new ArrayList<>();
        String selectQuery;
        if(Database.isNewCommentaryVersion()){
            selectQuery = "SELECT * FROM " + TABLE_BOOKS + " WHERE commentsOnMultiple like '%(" + this.bid + ")%'";
        }else{
            selectQuery = "SELECT * FROM " + TABLE_BOOKS + " WHERE commentsOn = " + this.bid;
        }

        Cursor cursor = db.rawQuery(selectQuery, null);
        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                // Adding  to list
                bookList.add(new Book(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();

        allCommentaries = bookList;
        //Log.d("Book", "getAllCommentary returning .size():" + allCommentaries.size());
        return bookList;
    }


    /**
     *
     * @param mainBook the book that it is commenting on (for example, Genesis)
     * @return the name of the commentary without the " on xyzBook" (for example, Rashi)
     */

    public String getCleanedCommentaryTitle(Util.Lang lang, Book mainBook){
        if(lang == Util.Lang.HE) {
            if(heCollectiveTitle == null)
                return getTitle(Util.Lang.HE).replace(" על " + mainBook.heTitle, "");
            else
                return heCollectiveTitle;
        }else{
            if(enCollectiveTitle == null)
                return getTitle(Util.Lang.EN).replace(" on " + mainBook.title, "");
            else
                return enCollectiveTitle;
        }
    }

    public static List<Book> getAll() {
        SQLiteDatabase db = Database.getDB();
        List<Book> bookList = new ArrayList<>();
        // Select All Query
        String selectQuery = "SELECT  * FROM " + TABLE_BOOKS;

        Cursor cursor = db.rawQuery(selectQuery, null);
        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                // Adding  to list
                bookList.add(new Book(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return bookList;
    }

    public static ArrayList<String> getAllBookNames(Util.Lang lang) {
        SQLiteDatabase db = Database.getDB();
        // Select All Query
        String selectQuery = "SELECT  * FROM " + TABLE_BOOKS;

        Cursor cursor = db.rawQuery(selectQuery, null);
        // looping through all rows and adding to list
        ArrayList<String> bookNameList = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                if(lang == Util.Lang.EN || lang == Util.Lang.BI){
                    bookNameList.add(cursor.getString(7)); //7 is index of enbook title
                }
                if (lang == Util.Lang.HE || lang == Util.Lang.BI) {
                    bookNameList.add(Util.getRemovedNikudString(cursor.getString(8))); //8 is index of hebook title
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        return bookNameList;
    }

    public int getCatColor() {
        if (categories == null) return -1;
        return MyApp.getCatColor(categories[0]);
    }

    public static void getAllSectionNames(){
        List<Book> bookList = getAll();
        HashMap<String, Integer> secList = new HashMap<String, Integer>() ;
        for(int i = 0; i< bookList.size(); i++){
            for(int j = 0; j<bookList.get(i).sectionNames.length; j++){
                secList.put(bookList.get(i).sectionNames[j], 1);
            }

        }
        Object []  keyList = secList.keySet().toArray();
        for(int i = 0;i< keyList.length; i++ ){
            Log.i("sql_sectionList", keyList[i].toString());

        }

    }

    public boolean isTalmudBavli(){
        try{
            return ("Talmud".equals(this.categories[0]) &&  "Bavli".equals(this.categories[1]));
        }catch (Exception e){
            return false;
        }
    }

    //PARCELABLE------------------------------------------------------------------------

    public static final Parcelable.Creator<Book> CREATOR
            = new Parcelable.Creator<Book>() {
        public Book createFromParcel(Parcel in) {
            return new Book(in);
        }

        public Book[] newArray(int size) {
            return new Book[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(bid);
        dest.writeInt(commentsOn);
        dest.writeStringArray(sectionNames);
        dest.writeStringArray(sectionNamesL2B);
        dest.writeStringArray(heSectionNamesL2B);
        dest.writeStringArray(categories);
        dest.writeInt(textDepth);
        dest.writeInt(wherePage);
        dest.writeString(title);
        dest.writeString(heTitle);
        dest.writeInt(languages);
    }

    private Book(Parcel in) {
        bid = in.readInt();
        commentsOn = in.readInt();
        sectionNames = in.createStringArray();
        sectionNamesL2B = in.createStringArray();
        heSectionNamesL2B = in.createStringArray();
        categories = in.createStringArray();

        textDepth = in.readInt();
        wherePage = in.readInt();
        title = in.readString();
        heTitle = in.readString();
        languages = in.readInt();


    }

}





/*	public void add() {
		DatabaseHandler dbHandler = DatabaseHandler.getInstance(MyApp.context);

	SQLiteDatabase db = dbHandler.getWritableDatabase();

	ContentValues values = new ContentValues();
	values.put(KcommentsOn, commentsOn); 
	values.put(KsectionNames, sectionNames);
	values.put(Kcategories, categories); 
	values.put(KtextDepth, textDepth); 
	values.put(KwherePage, wherePage);
	values.put(Klengths, lengths); 
	values.put(Ktitle, title); 
	values.put(KheTitle, heTitle); 

	// Inserting Row
	db.insert(TABLE_BOOKS, null, values);
	db.close(); // Closing database connection
}
 */

