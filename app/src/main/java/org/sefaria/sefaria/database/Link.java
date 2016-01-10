package org.sefaria.sefaria.database;


import java.util.ArrayList;
import java.util.List;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.util.Pair;

public class Link implements Parcelable {

    public Link(Cursor cursor){
        lid = cursor.getInt(0);
        connType = cursor.getString(1);
        bid = cursor.getInt(2);
        final int LEVELS_START_NUM = 3;
        for(int i = 0;i<NUM_LEVELS;i++) {
            levels[i] = cursor.getInt(i + LEVELS_START_NUM);
        }
		/*
		bidb = cursor.getInt(9);
		level1b = cursor.getInt(10);
		level2b = cursor.getInt(11);
		level3b = cursor.getInt(12);
		level4b = cursor.getInt(13);
		level5b = cursor.getInt(14);
		level6b = cursor.getInt(15);
		 */
    }

    public static final String KconnType = "connType";
    public static final String Kbida = "bida";
    public static final String Klevel1a = "level1a";
    public static final String Klevel2a = "level2a";
    public static final String Klevel3a = "level3a";
    public static final String Klevel4a = "level4a";
    public static final String Klevel5a = "level5a";
    public static final String Klevel6a = "level6a";
    public static final String Kbidb = "bidb";
    public static final String Klevel1b = "level1b";
    public static final String Klevel2b = "level2b";
    public static final String Klevel3b = "level3b";
    public static final String Klevel4b = "level4b";
    public static final String Klevel5b = "level5b";
    public static final String Klevel6b = "level6b";

    public int lid;
    public String connType;
    public int bid;
    private final static int NUM_LEVELS = 6;
    public int [] levels = new int [NUM_LEVELS];

    /*
    public int level1;
    public int level2;
    public int level3;
    public int level4;
    public int level5;
    public int level6;

    public int bidb;
    public int level1b;
    public int level2b;
    public int level3b;
    public int level4b;
    public int level5b;
    public int level6b;

     */
    @Override
    public String toString(){
        String str =  String.valueOf(lid) + " " + connType + " bid: " + String.valueOf(bid);
        for(int i=0;i<NUM_LEVELS;i++){
            str += " "  + String.valueOf(levels[i]);
        }
        return str;
                //+ " bidb: "  + String.valueOf(bidb)
                //+ " "  + String.valueOf(level1b) + " "  + String.valueOf(level2b)
                //+ " "  + String.valueOf(level3b) + " "  + String.valueOf(level4b)
                //+ " "  + String.valueOf(level5b) + " "  + String.valueOf(level6b))
                //;
    }

    public static final String TABLE_LINKS = "Links" ;

    private static String addLevelWhere(String levelName, int levelNum, String linkTableName){
        return addLevelWhere(levelName, levelNum, linkTableName, -1);
    }
    private static String addLevelWhere(String levelName, int levelNum, String linkTableName, int wherePage){
        if(levelNum == 0)//it means this level doesn't exist (it's too high), so don't display anything.
            return " ";
        else if(levelNum == -1){//it means that we want to display everything from this level
            levelNum =  0;
        }
        return " AND (" + linkTableName + "." + levelName  + "=" + String.valueOf(levelNum)// + " OR " + linkTableName +"." + levelName + "=0"
                + ") ";
    }

    private static String makeWhereStatement(Text text){
        String str = " L." + Kbida + "=" + text.bid;
        for(int i=0;i<text.levels.length;i++)
            str += addLevelWhere(Klevel1a, text.levels[0],"L");
        str += " AND    L.bidb=T.bid "
                + " AND (T." + Text.Klevel1 + "=L." + Klevel1b + ")"// + " OR L." + Klevel1b + "=0)"
                + " AND (T." + Text.Klevel2 + "=L." + Klevel2b + ")"//+ " OR L." + Klevel2b + "=0)"
                + " AND (T." + Text.Klevel3 + "=L." + Klevel3b + ")"//+ " OR L." + Klevel3b + "=0)"
                + " AND (T." + Text.Klevel4 + "=L." + Klevel4b + ")"//+ " OR L." + Klevel4b + "=0)"
                + " AND (T." + Text.Klevel5 + "=L." + Klevel5b + ")"//+ " OR L." + Klevel5b + "=0)"
                + " AND (T." + Text.Klevel6 + "=L." + Klevel6b + ")"//+ " OR L." + Klevel6b + "=0)"
        ;
        return str;
    }

    private static String makeWhereStatement2(Text text){
        String str = " L2." + Kbidb + "=" + text.bid ;
        for(int i=0;i<text.levels.length;i++)
            str += addLevelWhere(Klevel1b, text.levels[i], "L2");
        str += " AND    L2.bida=T3.bid "
                + " AND (T3." + Text.Klevel1 + "=L2." + Klevel1a + ")"//+ " OR L2." + Klevel1a + "=0)"
                + " AND (T3." + Text.Klevel2 + "=L2." + Klevel2a + ")"//+ " OR L2." + Klevel2a + "=0)"
                + " AND (T3." + Text.Klevel3 + "=L2." + Klevel3a + ")"//+ " OR L2." + Klevel3a + "=0)"
                + " AND (T3." + Text.Klevel4 + "=L2." + Klevel4a + ")"//+ " OR L2." + Klevel4a + "=0)"
                + " AND (T3." + Text.Klevel5 + "=L2." + Klevel5a + ")"//+ " OR L2." + Klevel5a + "=0)"
                + " AND (T3." + Text.Klevel6 + "=L2." + Klevel6a + ")" ; //+ " OR L2." + Klevel6a + "=0)";
        return str;
    }

    static List<Pair<String, Integer>> getCountsTitles(Text text){
        Database2 dbHandler = Database2.getInstance();
        SQLiteDatabase db = dbHandler.getReadableDatabase();

        List<Pair<String, Integer>> countList = new ArrayList<Pair<String, Integer>>();

        String select = "SELECT T.bid"
                + " FROM " + TABLE_LINKS +" L, " + Text.TABLE_TEXTS + " T "
                + " WHERE " + makeWhereStatement(text)
                ;

        String select2 = "SELECT T3.bid"
                + " FROM " + TABLE_LINKS +" L2, " + Text.TABLE_TEXTS + " T3 "
                + " WHERE " + makeWhereStatement2(text)
                ;

        String sql = "SELECT B.title, booksCount FROM Books B, (SELECT bid as linkBid, Count(*) as booksCount FROM (" + select + " UNION ALL " + select2 + ") GROUP BY bid) WHERE B._id = linkBid";

        Cursor cursor = db.rawQuery(sql, null);

        if (cursor.moveToFirst()) {
            do {
                // Adding  to list
                countList.add(new Pair<String, Integer> (cursor.getString(0), cursor.getInt(1)));
            } while (cursor.moveToNext());
        }

        //for(int i = 0; i < countList.size(); i++)
        //	Log.d("SQL_linkcounts", " " + countList.get(i).first + " "+ countList.get(i).second);

        return countList;
    }

    public static List<Pair<String, Integer>> getCountsTitlesFromLinks_small(Text dummyText, int tidMin, int tidMax){
        Database2 dbHandler = Database2.getInstance();
        SQLiteDatabase db = dbHandler.getReadableDatabase();

        List<Pair<String, Integer>> countList = new ArrayList<Pair<String, Integer>>();


        String select1 = "SELECT T.bid"
                + " FROM " + TABLE_LINKS +" L, " + Text.TABLE_TEXTS + " T "
                + " WHERE " + makeWhereStatement(dummyText)
                ;

        String select2 = "SELECT T3.bid"
                + " FROM " + TABLE_LINKS +" L2, " + Text.TABLE_TEXTS + " T3 "
                + " WHERE " + makeWhereStatement2(dummyText)
                ;

        String select = "SELECT T1.bid FROM " + Text.TABLE_TEXTS + " T1 WHERE T1._id"
                + " IN ( SELECT L1.tid2 FROM Links_small L1 WHERE L1.tid1 >= " + tidMin + " AND " + " L1.tid1 < " + tidMax
                + " UNION ALL "
                + " SELECT L3.tid1 FROM Links_small L3 WHERE L3.tid2 >= " + tidMin + " AND " + " L3.tid2 < " + tidMax
                + ")";

        String sql = "SELECT B.title, booksCount FROM Books B, (SELECT bid as linkBid, Count(*) as booksCount FROM (" + select + " UNION ALL "  + select1 + " UNION ALL "  + select2 +  ") GROUP BY bid) WHERE B._id = linkBid";

        Cursor cursor = db.rawQuery(sql, null);

        if (cursor.moveToFirst()) {
            do {
                // Adding  to list
                countList.add(new Pair<String, Integer> (cursor.getString(0), cursor.getInt(1)));
            } while (cursor.moveToNext());
        }

        //for(int i = 0; i < countList.size(); i++)
        //	Log.d("SQL_linkcounts", " " + countList.get(i).first + " "+ countList.get(i).second);

        return countList;
    }

	/*
	public static List<Pair<String, Integer>> getCountsConnType(Text text){
		Database2 dbHandler = Database2.getInstance(MyApp.context);
		SQLiteDatabase db = dbHandler.getReadableDatabase();

		List<Pair<String, Integer>> countList = new ArrayList<Pair<String, Integer>>();

		String select = "SELECT L.connType"
				+ " FROM " + TABLE_LINKS +" L, " + Text.TABLE_TEXTS + " T "
				+ " WHERE " + makeWhereStatement(text)
				;

		String select2 = "SELECT L2.connType"
				+ " FROM " + TABLE_LINKS +" L2, " + Text.TABLE_TEXTS + " T3 "
				+ " WHERE " + makeWhereStatement2(text)
				;

		String sql = "SElECT connType, Count(*) from (" + select + " UNION ALL " + select2 + ") GROUP BY connType";

		Cursor cursor = db.rawQuery(sql, null);

		if (cursor.moveToFirst()) {
			do {
				// Adding  to list
				countList.add(new Pair<String, Integer> (cursor.getString(0), cursor.getInt(1)));
			} while (cursor.moveToNext());
		}

		//for(int i = 0; i < countList.size(); i++)
		//	Log.d("SQL_linkcounts", " " + countList.get(i).first + " "+ countList.get(i).second);

		return countList;
	}
	 */

    private static List<Text> getLinkedTextsFromDB(Text text, int limit, int offset) {
        Database2 dbHandler = Database2.getInstance();
        SQLiteDatabase db = dbHandler.getReadableDatabase();
        List<Text> linkList = new ArrayList<Text>();


        String sql = "SELECT T.* FROM " + Text.TABLE_TEXTS + " T, Books B WHERE T.bid = B._id AND T._id"
                + " IN ( SELECT L1.tid2 FROM Links_small L1 WHERE L1.tid1 = " + text.tid
                + " UNION "
                + " SELECT L2.tid1 FROM Links_small L2 WHERE L2.tid2 = " + text.tid
                + ")"
                + " ORDER BY (case when B.commentsOn=" + text.bid  + " then 0 else 1 end), T.bid"
                + " LIMIT " + String.valueOf(limit)
                + " OFFSET " + String.valueOf(offset)
                ;

        Cursor cursor = db.rawQuery(sql, null);
        if (cursor.moveToFirst()) {
            do {
                // Adding  to list
                linkList.add(new Text(cursor));
            } while (cursor.moveToNext());
        }


        return linkList;
    }

    /**
     * Get links for specific text (ex. verse).
     * @param text
     * @param limit
     * @param offset
     * @return linkList
     */
    static List<Text> getLinkedTexts(Text text, int limit, int offset) {
        List<Text> linkList = new ArrayList<Text>();
        try{
            linkList = getLinkedTextsFromDB(text, limit, offset);
        }catch(SQLiteException e){
            if(!e.toString().contains(API.NO_TEXT_MESSAGE)){
                throw e; //don't know what the problem is so throw it back out
            }
            linkList = API.getLinks(text,limit,offset);
        }catch(Exception e){
            e.printStackTrace();
        }

        return linkList;
    }


    private static List<Text> getLinkedChapTextsFromDB(Text text, int limit, int offset) {
        Database2 dbHandler = Database2.getInstance();
        SQLiteDatabase db = dbHandler.getReadableDatabase();

        List<Text> linkList = new ArrayList<Text>();
        String whereStatement = makeWhereStatement(text);
        String whereStatement2 = makeWhereStatement2(text);

        String select = "SELECT T2.* FROM " + Text.TABLE_TEXTS + " T2 WHERE T2._id"
                + " IN (  SELECT T._id"
                + " FROM " + TABLE_LINKS +" L, " + Text.TABLE_TEXTS + " T "
                + " WHERE " + whereStatement
                + ")";

        String select2 = "SELECT T4.* FROM " + Text.TABLE_TEXTS + " T4 WHERE T4._id"
                + " IN (  SELECT T3._id"
                + " FROM " + TABLE_LINKS +" L2, " + Text.TABLE_TEXTS + " T3 "
                + " WHERE " + whereStatement2
                + ")";

        String sql = select + " UNION " + select2 //"( " + select + " )";// + " UNION " + "( " + select2 + " )"
                + "ORDER BY bid"
                + " LIMIT " + String.valueOf(limit)
                + " OFFSET " + String.valueOf(offset)
                ;
        Cursor cursor = db.rawQuery(sql, null);
        if (cursor.moveToFirst()) {
            do {
                // Adding  to list
                linkList.add(new Text(cursor));
            } while (cursor.moveToNext());
        }
        return linkList;
    }


    static String createWhere3(Text text, String type){
        String whereStatement =  " WHERE L.bid" + type + " = " + text.bid;
        for(int i = 0; i < 6; i++){
            if (text.levels[i] == 0)
                return whereStatement;
            whereStatement+= " AND L.level" + (i +1) + type + " = " + text.levels[i];
        }
        return whereStatement;


    }

    private static List<Link> getLinks(Text text, int limit, int offset) {
        Database2 dbHandler = Database2.getInstance();
        SQLiteDatabase db = dbHandler.getReadableDatabase();

        List<Link> linkList = new ArrayList<Link>();

        String sql = "SELECT L._id, L.connType, L.bidb as bid, L.level1b, L.level2b, L.level3b, L.level4b, L.level5b, L.level6b FROM Links L"
                + createWhere3(text,"a")
                //+ " UNION"
                //+ " SELECT L._id, L.connType, L.bida as bid, L.level1a, L.level2a, L.level3a, L.level4a, L.level5a, L.level6a FROM Links L"
                //+  createWhere3(text,"b")
                //+ " WHERE L.bidb = ? AND L.level1b = ? AND L.level2b = ? AND L.level3b = ? AND L.level4b = ?  AND L.level5b = ? AND L.level6b = ?"
                + " ORDER BY bid";

        Log.d("sql", sql);
        Cursor cursor = db.rawQuery(sql, null);
        if (cursor.moveToFirst()) {
            do {
                // Adding  to list
                linkList.add(new Link(cursor));
            } while (cursor.moveToNext());
        }

        return linkList;
    }

    //gets links to a particular level other than the last level
    public static List<Text> getLinkedChapTexts(Text text, int limit, int offset) {
        List<Text> texts = new ArrayList<Text>();
        Text dummyChapText = Text.makeDummyChapText(text);
        try{
            texts = getLinkedChapTextsFromDB(dummyChapText, limit, offset);
        }catch(SQLiteException e){
            if(!e.toString().contains(API.NO_TEXT_MESSAGE)){
                throw e; //don't know what the problem is so throw it back out
            }
            texts = API.getChapLinks(dummyChapText,limit,offset);
        }catch(Exception e){
            e.printStackTrace();
        }

        return texts;

    }





    //PARCELABLE------------------------------------------------------------------------

    public static final Parcelable.Creator<Link> CREATOR
            = new Parcelable.Creator<Link>() {
        public Link createFromParcel(Parcel in) {
            return new Link(in);
        }

        public Link[] newArray(int size) {
            return new Link[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(lid);
        dest.writeString(connType);
        dest.writeInt(bid);
        for(int i =0;i<NUM_LEVELS;i++)
            dest.writeInt(levels[i]);


    }

    private Link(Parcel in) {
        lid = in.readInt();
        connType = in.readString();
        bid = in.readInt();
        for(int i =0;i<NUM_LEVELS;i++)
            levels[i] = in.readInt();
    }

	/*
	 private static List<Link> getAll() { //really only to be used for testing
		Database2 dbHandler = Database2.getInstance(MyApp.context);
		List<Link> linkList = new ArrayList<Link>();
		// Select All Query
		String selectQuery = "SELECT  * FROM " + TABLE_LINKS;

		SQLiteDatabase db = dbHandler.getReadableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);

		// looping through all rows and adding to list
		if (cursor.moveToFirst()) {
			do {
				// Adding  to list
				linkList.add(new Link(cursor));
			} while (cursor.moveToNext());
		}

		return linkList;
	}


	public static final String CREATE_TABLE_LINKS = "CREATE TABLE " + TABLE_LINKS + "(\r\n" +
			"	_id INTEGER PRIMARY KEY,\r\n" +
			KconnType + " CHAR(3),\r\n" +
			Kbida + "  INTEGER,\r\n" +
			"	level1a INTEGER,\r\n" +
			"	level2a INTEGER,\r\n" +
			"	level3a INTEGER,\r\n" +
			"	level4a INTEGER,\r\n" +
			"	level5a INTEGER,\r\n" +
			"	level6a INTEGER,\r\n" +
			//"	FOREIGN KEY(" + Kbida + ") \r\n" +
			//"		REFERENCES Books(bid)\r\n" +
			//"		ON DELETE CASCADE,\r\n" +
			Kbidb + "  INTEGER,\r\n" +
			"	level1b INTEGER,\r\n" +
			"	level2b INTEGER,\r\n" +
			"	level3b INTEGER,\r\n" +
			"	level4b INTEGER,\r\n" +
			"	level5b INTEGER,\r\n" +
			"	level6b INTEGER\r\n" + //"	level6b INTEGER,\r\n" +
			//"	FOREIGN KEY(" + Kbidb + ") \r\n" +
			//"		REFERENCES Books(bid)\r\n" +
			//"		ON DELETE CASCADE\r\n" +
			")";


	public static void addLinks(Context context){
		Database2 dbHandler = Database2.getInstance(MyApp.context);
		SQLiteDatabase db = dbHandler.getWritableDatabase();
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_LINKS);
		db.execSQL(CREATE_TABLE_LINKS);

		final int linkVer = 3;
		int linkFileNum = -1;
		while(true){
			linkFileNum++;
			String path = "links/" + linkVer+ "link"+ linkFileNum +  ".csv";
			try{
				CSVReader reader = new CSVReader(new InputStreamReader(context.getResources().getAssets().open(path)));
				addLinkFile(db, reader);
				Log.d("sql_link_adding","(I think) added links " + linkFileNum + ".csv to database");
			}catch(Exception e){
				sendException(e);
				break;
			}
		}
		db.close();

	}

	private static void addLinkFile(SQLiteDatabase db, CSVReader reader){

		String next[] = {};

		try {
			Book booka = new Book();
			Book bookb = new Book();
			db.beginTransaction();
			while(true) {
				next = reader.readNext();
				if(next != null) {
					if(!next[0].equals(booka.title))
						booka = new Book(next[0],db);
					if(!next[7].equals(bookb.title))
						bookb = new Book(next[7],db);
					try{
						if(db.insert(TABLE_LINKS, null, putValues(next, booka, bookb)) != -1)
							;//Log.d("sql_link_add", "Added new link: " + title  + " " + langString);
						else
							Log.e("sql_link_add", "Couldn't add link: ");
					} catch (Exception e){
						Log.e("sql_link_add", "Couldn't add link. " + e);

					}
				} else {
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			db.setTransactionSuccessful();
			db.endTransaction();
		}
		return;
	}

	private static ContentValues putValues(String [] row, Book booka, Book bookb){
		ContentValues values = new ContentValues();

		int startingNum = 6;
		int endingNum = 1;
		//int whileLoopC = 0;
		while(row[startingNum - booka.textDepth + 1].equals("0")){
			//return values; //return emtpy thing.
			for(int i = startingNum; i> endingNum ; i-- ){
				row[i - 1] = row[i];
			}
			row[startingNum] = "0";
			//Log.d("sql_link_values", "preforming fix row[x] A-" + booka.title + " " + whileLoopC++);
		}


		startingNum = 13;
		endingNum = 8;
		//whileLoopC = 0;
		while(row[startingNum - bookb.textDepth + 1].equals("0")){
			for(int i = startingNum; i> endingNum ; i-- ){
				row[i - 1] = row[i];
			}
			row[startingNum] = "0";
			//Log.d("sql_link_values", "preforming fix row[x] B-" + bookb.title+ " " + whileLoopC++ );
		}

		values.put(KconnType, row[14]);

		values.put(Kbida, booka.bid);


		values.put(Klevel1a, row[6]);
		values.put(Klevel2a, row[5]);
		values.put(Klevel3a, row[4]);
		values.put(Klevel4a, row[3]);
		values.put(Klevel5a, row[2]);
		values.put(Klevel6a, row[1]);


		values.put(Kbidb, bookb.bid);
		values.put(Klevel1b, row[13]);
		values.put(Klevel2b, row[12]);
		values.put(Klevel3b, row[11]);
		values.put(Klevel4b, row[10]);
		values.put(Klevel5b, row[9]);
		values.put(Klevel6b, row[8]);

		//Log.d("sql_links_putValues", row.toString());

		return values;
	}

	 */



}


