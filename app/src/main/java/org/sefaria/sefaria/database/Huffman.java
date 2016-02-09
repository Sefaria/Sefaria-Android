package org.sefaria.sefaria.database;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.Date;
import java.util.List;

/**
 * Created by LenJ on 2/8/2016.
 */
public class Huffman {

    private String plainText;
    private int count;
    private Huffman leftChild;
    private Huffman rightChild;
    private Huffman parent;
    private boolean isRight;

    private static Huffman huffmanRoot = null;

    public Huffman(String plainText, int count){
        this.plainText = plainText;
        this.count = count;
    }

    public Huffman(){

    }

    public Huffman(Huffman h1, Huffman h2){
        h1.parent = this;
        h2.parent = this;
        h1.isRight = false;
        h2.isRight = true;
        leftChild = h1;
        rightChild = h2;
        count = h1.count + h2.count;
    }

    private static void printTree(Huffman node, String tabs){
        if(node == null)
            return;
        Log.d("Huffman", tabs + node);
        printTree(node.leftChild, tabs + "\t");
        printTree(node.rightChild, tabs + "\t");
    }

    @Override
    public String toString() {
        if(plainText == null)
            return hashCode() + "_" + count;
        return plainText + ": " + count;
    }

    //final private static byte [] MASKS = new byte[]{0x01,0x02,0x04,0x08,0x10};

    public static String decode(byte [] bytes, int size){
        if(huffmanRoot == null)
            getTree();
        String decode = "";
        Huffman node = huffmanRoot;
        int byteNum = -1;
        for(int i=0;i<size;i++){
            int mod = i % 8;
            if(mod == 0)
                byteNum++;
            int a = (bytes[byteNum] & 0x01<<mod);
            if(a == 0){
                node = node.rightChild;
            }else{
                node = node.leftChild;
            }
            if(node.plainText != null){
                decode += node.plainText;
                node = huffmanRoot;
            }
        }

        return decode;
    }

    public static String decode(List<Boolean> encoded){
        if(huffmanRoot == null)
            getTree();
        String decode = "";
        Huffman node = huffmanRoot;
        for(Boolean bit: encoded){
            if(bit){
                node = node.rightChild;
            }else{
                node = node.leftChild;
            }
            if(node.plainText != null){
                decode += node.plainText;
                node = huffmanRoot;
            }
        }

        return decode;
    }
    private static final Character ZERO = '\u0000';
    private static final Character ONE = '\u0001';


    private static Huffman getPlacementNode(Huffman node, Huffman cameFrom){
        if(node.leftChild == null || node.rightChild == null)
            return node;
        else if(node.leftChild.plainText == null && cameFrom != node.leftChild){
            return getPlacementNode(node.leftChild, null);
        }else{
            return getPlacementNode(node.parent, node);
        }
    }

    public static boolean getTree() {
        Log.d("Huffman", "started");
        try {
            Database dbHandler = Database.getInstance();
            SQLiteDatabase db = dbHandler.getReadableDatabase();
            Cursor cursor = db.query("Settings", null, "_id" + "=?",
                    new String[]{"huffman"}, null, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                String compression = cursor.getString(1);
                huffmanRoot = enflateTree(compression);
                //printTree(huffmanRoot,"");

            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        Log.d("Huffman", "finished");
        printTree(huffmanRoot, "");
        return true;
    }
    public static Huffman enflateTree(String deflated){
        Date date = new Date();
        long startTime = date.getTime();
        Huffman root = new Huffman();
        Huffman node = root;
        for(int i=1;i<deflated.length();i++){
            Character character = deflated.charAt(i);
            if(character ==  ONE){
                String tempText = "";
                while(i<deflated.length()-1){
                    character = deflated.charAt(++i);
                    if(character != ONE && character != ZERO)
                        tempText += character;
                    else{
                        i--;
                        break;
                    }
                }
                Huffman tempNode = new Huffman(tempText,0);
                node = getPlacementNode(node, null);
                if(node.leftChild == null){
                    node.leftChild = tempNode;
                }else{
                    node.rightChild = tempNode;
                }
                tempNode.parent = node;
            }else{// if(character == ZERO){
                Huffman tempNode = new Huffman();
                node = getPlacementNode(node,null);
                if(node.leftChild == null){
                    node.leftChild = tempNode;
                }else{
                    node.rightChild = tempNode;
                }
                tempNode.parent = node;
                node = tempNode;
            }
        }

        return root;
    }



}
