package org.sefaria.sefaria;

import android.app.Activity;
import android.app.admin.DeviceAdminInfo;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.support.annotation.AttrRes;
import android.support.annotation.DrawableRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.text.BidiFormatter;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.StateSet;
import android.util.TypedValue;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.sefaria.sefaria.layouts.IndeterminateCheckable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.text.Bidi;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by nss on 9/8/15.
 */
public class Util {

    public enum Lang {
        HE,BI,EN
    }



    public static final String VERSE_BULLET = "\u25CF";
    public static final String LINK_CAT_VERTICAL_LINE = "\u007C";
    public static final float EN_HE_RATIO = 40f/35f; //universal constant


    static final private char[] heChars = {
            '\u05d0','\u05d1','\u05d2','\u05d3','\u05d4','\u05d5','\u05d6','\u05d7','\u05d8','\u05d9',
            //'\u05da',
            '\u05db','\u05dc',
            //'\u05dd',
            '\u05de',
            //'\u05df',
            '\u05e0','\u05e1','\u05e2',
            //'\u05e3',
            '\u05e4',
            //'\u05e5',
            '\u05e6','\u05e7','\u05e8','\u05e9',
            '\u05ea'};

    static final private String[] htmlTags = { "b","i","strong","em","small","big"}; //to be replaced with spans using the html2Span() fn


    public static boolean isSystemLangHe(){
        return Locale.getDefault().getLanguage().equals("iw");
    }


    public static void writeFile(String path, String data) throws IOException {
        PrintWriter writer = new PrintWriter(path, "UTF-8");
        writer.print(data);
        writer.close();
    }

    public static String readFile(String path) throws IOException {
        File file = new File(path);
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        fis.close();
        String str = new String(data, "UTF-8");
        return str;
    }

    static String readFileFromAssets(Context context, String path) throws IOException
    {
        Resources resources = context.getResources();
        InputStream iS = resources.getAssets().open(path);
        //create a buffer that has the same size as the InputStream
        byte[] buffer = new byte[iS.available()];
        //read the segment file as a stream, into the buffer
        iS.read(buffer);
        //create a output stream to write the buffer into
        ByteArrayOutputStream oS = new ByteArrayOutputStream();
        //write this buffer to the output stream
        oS.write(buffer);
        //Close the Input and Output streams
        oS.close();
        iS.close();

        //return the output stream as a String
        return oS.toString();
    }

    static JSONObject getJSON(Context context, String path) throws JSONException,IOException {
        String jsonText = readFileFromAssets(context,path);
        JSONObject object = (JSONObject) new JSONTokener(jsonText).nextValue();
        return object;
    }

    static String joinArrayList(ArrayList<?> r, String delimiter) {
        if(r == null || r.size() == 0 ){
            return "";
        }
        StringBuffer sb = new StringBuffer();
        int i, len = r.size() - 1;
        for (i = 0; i < len; i++){
            sb.append(r.get(i).toString() + delimiter);
        }
        return sb.toString() + r.get(i).toString();
    }

    //given two arrays, join them (need to be non-primitive)
    static <T> T[] concatenateArrays (T[] a, T[] b) {
        int aLen = a.length;
        int bLen = b.length;

        @SuppressWarnings("unchecked")
        T[] c = (T[]) Array.newInstance(a.getClass().getComponentType(), aLen + bLen);
        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);

        return c;
    }

    public static String[] str2strArray(String str) {
        if(str == null)
            return new String [] {};
        final Pattern r = Pattern.compile("(\\[|\\]|\")+"); //matches all [ , ] & "
        str = r.matcher(str).replaceAll("");
        String[] strArray = str.split(",");
        return strArray;
    }

    public static int[] str2intArray(String str) {
        if(str == null)
            return new int [] {};
        final Pattern r = Pattern.compile("(\\[|\\]|\")+"); //matches all [ , ] & "
        str = r.matcher(str).replaceAll("");
        String[] strArray = str.split(",");
        int [] intArray = new int[strArray.length];
        for(int i=0; i<strArray.length; i++){
            intArray[i] = Integer.parseInt(strArray[i]);
        }
        return intArray;
    }


    /**
     * @return Number of bytes available on internal storage
     */
    public static long getInternalAvailableSpace() {
        long availableSpace = -1L;
        try {StatFs stat = new StatFs(Environment.getDataDirectory()
                .getPath());
            stat.restat(Environment.getDataDirectory().getPath());
            if(Build.VERSION.SDK_INT >= 18)
                availableSpace = stat.getAvailableBlocksLong() * stat.getBlockSizeLong();
            else
                availableSpace = (long) stat.getAvailableBlocks() * (long) stat.getBlockSize();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return availableSpace;
    }



    public static long getFolderSize(File dir) {
        long size = 0;
        for (File file : dir.listFiles()) {
            if (file.isFile()) {
                System.out.println(file.getName() + " " + file.length());
                size += file.length();
            }
            else
                size += getFolderSize(file);
        }
        return size;
    }

    public static boolean deleteNonRecursiveDir(String dirname){
        File dir = new File(dirname);
        if (dir.exists() && dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (int i = 0; i < children.length; i++) {
                    new File(dir, children[i]).delete();
                }
            }
        }
        return dir.delete();
    }

    public static String array2str(String [] array){
        String str = "[";
        for(int i=0; i<array.length;i++)
            str += array[i] +",";
        str += "]";
        return str;
    }

    public static String getRemovedNikudString(String nikStr) {
        //final Pattern r = Pattern.compile("[\u0591-\u05C7]");
        //final Pattern r = Pattern.compile("[\u0591-\u05BD\u05BF\u05C7]");
        final Pattern r = Pattern.compile("[\u0591-\u05BD\u05BF\u05C1\u05C2\u05C4\u05C5\u05C7]");
        final Pattern r2 = Pattern.compile("\u05be");
        return r.matcher( r2.matcher(nikStr).replaceAll(" ")).replaceAll("");
        //return r.matcher(nikStr).replaceAll("");
    }

    static int[] concatIntArrays(int[] a, int[] b) {
        int aLen = a.length;
        int bLen = b.length;
        int[] c= new int[aLen+bLen];
        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);
        return c;
    }

    public static String int2heb(int num) {
        int origNum = num;
        String heb = "";
        int place = 0;
        while (num >= 1) {
            int digit = num%10;
            //Log.d("gem","DIGIT: " + digit);
            num /= 10;
            int baseHebChar = 0; //this is the position of a char in hebChars
            char hebChar;
            if (digit == 0) {
                //Log.d("gem","zero");
                hebChar = '\0'; //no char when exactly multiple of ten
            }
            else {
                if (place == 0) {
                    baseHebChar = 0; //alef
                    hebChar = heChars[(baseHebChar + digit-1)];
                    heb = hebChar + heb;
                } else if (place == 1) {
                    baseHebChar = 9; //yud
                    hebChar = heChars[(baseHebChar + digit-1)];
                    heb = hebChar + heb;
                } else if (place >= 2) {
                    baseHebChar = 18; //kuf
                    if (digit == 9) { //can't be greater than tuf
                        char hChar1 = heChars[(baseHebChar + digit-9)];
                        char hChar2 = heChars[(baseHebChar + 3)]; //tuf, need two of these
                        heb = "" + hChar2 + hChar2 + hChar1 + heb;
                    } else if (digit > 4) {
                        char hChar1 = heChars[(baseHebChar + digit-5)];
                        char hChar2 = heChars[(baseHebChar + 3)]; //tuf
                        heb = "" + hChar2 + hChar1 + heb;
                    } else {
                        char hChar1 = heChars[(baseHebChar + digit-1)];
                        heb = "" + hChar1 + heb;
                    }
                }
            }
            place++;
        }
        //now search for 15 & 16 to replace
        final String ka = "\u05D9\u05D4"; //careful...don't join these strings
        final String ku = "\u05D9\u05D5";
        final Pattern kaPatt = Pattern.compile("(" + ka + ")+");
        final Pattern kuPatt = Pattern.compile("(" + ku + ")+");
        heb = kaPatt.matcher(heb).replaceAll("\u05D8\u05D5");
        heb = kuPatt.matcher(heb).replaceAll("\u05D8\u05D6");

        //Log.d("gem",origNum + " = " + heb);
        return heb;
    }

    public static String html2Span(String html) {
        //first find all indices of interesting tags
        StringBuffer sb = new StringBuffer();
        HashMap<Integer,Integer> htmlIndsMap = new HashMap<>(); //maps index in string to index in htmlTags array
        for (String tag : htmlTags) {
            Pattern p = Pattern.compile("(<" + tag + ">)(.*)(</" + tag + ">)");
            Matcher m = p.matcher(html);

            if (m.find())
            {
                m.appendReplacement(sb,m.group(2));
            }
        }
        return sb.toString();
    }

    //returns true if segment has any hebrew character in it
    public static boolean hasHebrew(String text) {
        Pattern patt = Pattern.compile("[\u05d0-\u05ea]");
        return patt.matcher(text).find();
    }

    public static void moveFile(String inputPath, String inputFile, String outputPath, String outputFile) {

        InputStream in = null;
        OutputStream out = null;
        try {

            //create output directory if it doesn't exist
            File dir = new File (outputPath);
            if (!dir.exists())
            {
                dir.mkdirs();
            }


            in = new FileInputStream(inputPath + inputFile);
            out = new FileOutputStream(outputPath + outputFile);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            in = null;

            // write the output file
            out.flush();
            out.close();
            out = null;

            // delete the original file
            new File(inputPath + inputFile).delete();
        } catch (Exception e) {
            GoogleTracker.sendException(e, "Moving file");
        }


    }





    public static String convertDBnum(int DBnum){
        if(DBnum == -1)
            return MyApp.getRString(R.string.none);
        if(DBnum <= 0)
            return String.valueOf(DBnum);
        String passDot;
        if(DBnum%100 <10){
            passDot = "0" + DBnum%100;
        }
        else
            passDot = "" + DBnum%100;
        return String.valueOf(DBnum/100) + "." + passDot;
    }

    public static float pixelsToSp(float px) {
        float scaledDensity = MyApp.getContext().getResources().getDisplayMetrics().scaledDensity;
        return px/scaledDensity;
    }

    public static float dpToPixels(float px) {
        float scale = MyApp.getContext().getResources().getDisplayMetrics().density;
        return (px * scale + 0.5f);
    }

    private float inchesToPixels(Activity activity, float inches) {
        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        return inches*metrics.ydpi;
    }

    public static int getRelativeTop(View myView) {
        if (myView.getParent()== null) return 0;
        if (myView.getParent() == myView.getRootView())
            return myView.getTop();
        else
            return myView.getTop() + getRelativeTop((View) myView.getParent());
    }


    private static final Pattern DIR_SEPARATOR = Pattern.compile("/");

    /**
     * Returns all available SD-Cards in the system (include emulated)
     *
     * Warning: Hack! Based on Android source code of version 4.3 (API 18)
     * Because there is no standart way to get it.
     * TODO: Test on future Android versions 4.4+
     *
     * @return paths to all available SD-Cards in the system (include emulated)
     */
    public static String[] getStorageDirectories()
    {
        // Final set of paths
        final Set<String> rv = new HashSet<String>();
        // Primary physical SD-CARD (not emulated)
        final String rawExternalStorage = System.getenv("EXTERNAL_STORAGE");
        // All Secondary SD-CARDs (all exclude primary) separated by ":"
        final String rawSecondaryStoragesStr = System.getenv("SECONDARY_STORAGE");
        // Primary emulated SD-CARD
        final String rawEmulatedStorageTarget = System.getenv("EMULATED_STORAGE_TARGET");
        if(TextUtils.isEmpty(rawEmulatedStorageTarget))
        {
            // Device has physical external storage; use plain paths.
            if(TextUtils.isEmpty(rawExternalStorage))
            {
                // EXTERNAL_STORAGE undefined; falling back to default.
                rv.add("/storage/sdcard0");
            }
            else
            {
                rv.add(rawExternalStorage);
            }
        }
        else
        {
            // Device has emulated storage; external storage paths should have
            // userId burned into them.
            final String rawUserId;
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1)
            {
                rawUserId = "";
            }
            else
            {
                final String path = Environment.getExternalStorageDirectory().getAbsolutePath();
                final String[] folders = DIR_SEPARATOR.split(path);
                final String lastFolder = folders[folders.length - 1];
                boolean isDigit = false;
                try
                {
                    Integer.valueOf(lastFolder);
                    isDigit = true;
                }
                catch(NumberFormatException ignored)
                {
                }
                rawUserId = isDigit ? lastFolder : "";
            }
            // /storage/emulated/0[1,2,...]
            if(TextUtils.isEmpty(rawUserId))
            {
                rv.add(rawEmulatedStorageTarget);
            }
            else
            {
                rv.add(rawEmulatedStorageTarget + File.separator + rawUserId);
            }
        }
        // Add all secondary storages
        if(!TextUtils.isEmpty(rawSecondaryStoragesStr))
        {
            // All Secondary SD-CARDs splited into array
            final String[] rawSecondaryStorages = rawSecondaryStoragesStr.split(File.pathSeparator);
            Collections.addAll(rv, rawSecondaryStorages);
        }
        return rv.toArray(new String[rv.size()]);
    }

    public static JSONArray openJSONArrayFromAssets(String file) throws IOException, JSONException {
        JSONArray jsonObject = new JSONArray(getStringFromAssets(file));
        return jsonObject;
    }

    public static String getStringFromAssets(String file)throws IOException, JSONException {
        InputStream is = MyApp.getContext().getAssets().open(file);
        int size = is.available();
        byte[] buffer = new byte[size];
        is.read(buffer);
        is.close();
        String bufferString = new String(buffer);
        return bufferString;
    }

    public static JSONObject openJSONObjectFromAssets(String file) throws IOException, JSONException {
        JSONObject jsonObject = new JSONObject(getStringFromAssets(file));
        return jsonObject;
    }

    public static int getColor(Context context, int id) {
        TypedValue colorVal = new TypedValue();
        context.getTheme().resolveAttribute(id, colorVal, true);
        return colorVal.data;
    }

    public static int getDrawable(Context context, int id) {
        TypedValue drawableVal = new TypedValue();
        context.getTheme().resolveAttribute(id, drawableVal, false); //false means that this is equivalent of using the R.drawable.x value
        return drawableVal.data;
    }

    public static int convertDafOrIntegerToNum(String spot){
        int num = 0;
        if(spot.replaceFirst("[0-9]+[ab]","").length() == 0){
            if(spot.contains("b"))
                num +=1;
            num += Integer.valueOf(spot.replaceAll("[ab]",""))*2-1;
        }else{
            num = Integer.valueOf(spot);
        }
        return num;
    }

    /**
     * Unclear if this function works for HTML tags. So far in my tests it hasn't worked
     * @param input
     * @param mainLang - either Util.Lang.HE or Util.Lang.EN, depending on what you expect the language to be
     * @return
     */
    public static String getBidiString(String input, Util.Lang mainLang) {
        Pattern htmlPat = Pattern.compile("<.+?>");

        boolean rtlContext;
        int bidiDirection;
        int defaultBidiDirection;
        if (mainLang == Util.Lang.EN) {
            rtlContext = false;
            bidiDirection = Bidi.DIRECTION_LEFT_TO_RIGHT;
            defaultBidiDirection = Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT;
        } else /* if (lang == Util.Lang.HE) */{
            rtlContext = true;
            bidiDirection = Bidi.DIRECTION_RIGHT_TO_LEFT;
            defaultBidiDirection = Bidi.DIRECTION_DEFAULT_RIGHT_TO_LEFT;
        }

        Bidi bidi = new Bidi(input,defaultBidiDirection);
        if (!bidi.isMixed()) return input;

        BidiFormatter bidiFormatter = BidiFormatter.getInstance(rtlContext);
        StringBuilder bidiTestBuilder = new StringBuilder();
        for (int i = 0; i < bidi.getRunCount(); i++)
        {
            int start = bidi.getRunStart(i);
            int level = bidi.getRunLevel(i);
            int limit = bidi.getRunLimit(i);
            String run = input.substring(start, limit);


            //if there is HTML in this run, don't unicode wrap it because this will mess up the presentation
            boolean hasHtml = htmlPat.matcher(run).find();

            //apparently level is even when ltr and odd when rtl
            //bidiDirection == 0 when LTR and 1 when RTL
            if ((level % 2) != bidiDirection && !hasHtml) {
                run = bidiFormatter.unicodeWrap(run ,!rtlContext) + " ";
            }
            bidiTestBuilder.append(run);
        }

        return bidiTestBuilder.toString();

        //SOME TEST
        //String testHe = "הבחור Noah Santacruz הוא חמוד מאוד ונולד בשנת 1992.";
        //String testEn = "English is cool, but what I really love is להקליד בעברית all day long!";

        //String testEn = "English is cool, but what I really love is להקליד בעברית all day long!";
        //String testHe = "ויכל ביום השביעי, by the time the seventh day had started, all G’d’s work had been completed so that there was no creative activity left for G’d to perform on the seventh day. It is therefore technically correct to state that the meaning of the words is that G’d’s work had been completed, and completion of work cannot be termed “work.” The meaning of the words אשר עשה, therefore is that “all the creative activities which G’d had performed during the preceding six “days” had been terminated with the advent of the seventh day, so that there was nothing left to be done on that day.” We have similar constructions in the Torah, for instance in Exodus 12,16 ביום הראשון תשביתו שאור, which means that on that day leavened things should be in a state of having been destroyed, banished. (compare Pessachim 5) וישבות, He discontinued, what He had completed (שבת).";
/*
        String testHe = "איזו קבוצה כדאי לי לאהוד בפלייאוף ה-NBA?\n" +
                "בעד האנדרדוג, הולכים עם העדר, בעלי ניסיון או עקשנים בקטע טוב? עשר שאלות ותדעו מי השותפה האידאלית עבורכם ללילות הלבנים";
*/
        //BidiFormatter bdf = BidiFormatter.getInstance(false);


        //String bidiTest = bdf.unicodeWrap(testHe,false);
        //Log.d("SearchActivity", bidiTest);
    }

    //there's currently an error when you search for "עורכי" and select midrash. parentheses seem to be an issue
    public static String regexpEscape(String str) {
        //Pattern escaper = Pattern.compile("([^a-zA-z0-9\\ \\'])");
        //return escaper.matcher(str).replaceAll("\\\\$1");

        Pattern escaper = Pattern.compile("[{}()\\[\\].+*?^$\\\\|\\/]");
        return escaper.matcher(str).replaceAll("\\\\$0");
    }

    /**
     * for indeterminate checkbox
     */

    public static int applyAlpha(int color, float alpha) {
        return Color.argb(Math.round(Color.alpha(color) * alpha),
                Color.red(color),
                Color.green(color),
                Color.blue(color));
    }

    public static Drawable tintDrawable(View view, @DrawableRes int drawable) {
        if (!(view instanceof IndeterminateCheckable)) {
            throw new IllegalArgumentException("view must implement IndeterminateCheckable");
        }

        final ColorStateList colorStateList = createIndeterminateColorStateList(view.getContext());

        final Drawable d = DrawableCompat.wrap(ContextCompat.getDrawable(view.getContext(), drawable));
        DrawableCompat.setTintList(d, colorStateList);

        return d;
    }

    private static ColorStateList createIndeterminateColorStateList(Context context) {

        final int[][] states = new int[][]{
                new int[]{-android.R.attr.state_enabled},
                new int[]{R.attr.state_indeterminate},
                new int[]{android.R.attr.state_checked},
                StateSet.WILD_CARD
        };

        final int normal = resolveColor(context, R.attr.colorControlNormal, Color.DKGRAY);
        final int activated = resolveColor(context, R.attr.colorControlActivated, Color.CYAN);
        final float disabledAlpha = resolveFloat(context, android.R.attr.disabledAlpha, 0.25f);
        final int[] colors = new int[]{
                applyAlpha(normal, disabledAlpha),
                normal,
                activated,
                normal
        };

        return new ColorStateList(states, colors);
    }

    private static int resolveColor(Context context, @AttrRes int attr, int defaultValue) {
        TypedArray a = context.getTheme().obtainStyledAttributes(new int[]{attr});
        try {
            return a.getColor(0, defaultValue);
        } finally {
            a.recycle();
        }
    }

    private static float resolveFloat(Context context, @AttrRes int attr, float defaultValue) {
        TypedValue val = new TypedValue();
        if (context.getTheme().resolveAttribute(attr, val, true)) {
            return val.getFloat();
        } else {
            return defaultValue;
        }
    }

    /**
     *
     * @param color 24-bit decimal color
     * @param tint 0 - 1
     * @return
     */
    public static int tintColor(int color, float tint) {
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        r -= r*tint;
        g -= g*tint;
        b -= b*tint;
        return Color.rgb(r,g,b);
    }


    /**
     * Return a string with a maximum length of <code>length</code> characters.
     * If there are more than <code>length</code> characters, then string ends with an ellipsis ("...").
     *
     * @param text
     * @param length
     * @return
     */
    public static String ellipsis(final String text, int length, int dontTouchLength)
    {
        // The letters [iIl1] are slim enough to only count as half a character.
        length += Math.ceil(text.replaceAll("[^iIl]", "").length() / 2.0d);

        dontTouchLength = Math.max(length, dontTouchLength);
        if (text.length() > dontTouchLength)
        {
            return text.substring(0, length/2) + "\u2026" + text.substring(text.length() - length/2 -1, text.length());
        }

        return text;
    }

}
