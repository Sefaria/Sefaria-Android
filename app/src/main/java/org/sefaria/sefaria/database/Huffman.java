package org.sefaria.sefaria.database;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by LenJ on 2/8/2016.
 */
public class Huffman {

    /*
    private String plainText;
    private Huffman leftChild;
    private Huffman rightChild;
    private Huffman parent;
    */

    private static String [] plainTexts;
    private static int [] leftChilds;
    private static int [] rightChilds;
    private static int [] parents;

    private static int index = 1;
    private static boolean createdTree = false;

    private static int getNew(){
        return index++;
    }

    private static int getParent(int index){
        return parents[index];
    }

    private static int getLeft(int index){
        return leftChilds[index];
    }

    private static int getRight(int index){
        return rightChilds[index];
    }

    private static String getPlainText(int index){
        return plainTexts[index];
    }

    private static void setPlainText(int index, String string){
        plainTexts[index] = string;
    }

    private static void setleft(int index, int left){
        leftChilds[index] = left;
    }

    private static void setRight(int index, int right){
        rightChilds[index] = right;
    }

    private static void setParent(int index, int parent){
        parents[index] = parent;
    }

    private static Huffman huffmanRoot = null;
    /*
    public Huffman(String plainText){
        this.plainText = plainText;
    }
    */

    private static void allocate(int size){
        plainTexts = new String[size];
        leftChilds = new int[size];
        rightChilds = new int[size];
        parents = new int[size];
        index = 1;
        createdTree = false;
    }
    public Huffman(){
    }

    /*
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
            return hashCode() + "_";
        return plainText + ":";
    }
*/
    //final private static byte [] MASKS = new byte[]{0x01,0x02,0x04,0x08,0x10};

    private static final int nodeRoot = 1;
    public static String decode(byte [] bytes, int size){
        if(!createdTree)
            getTree();
        Log.d("Huffman", "started decode");
        StringBuilder decode = new StringBuilder();
        int node = nodeRoot;
        int byteNum = -1;
        try{
            for(int i=0;i<size;i++){
                int mod = i % 8;
                if(mod == 0)
                    byteNum++;
                int a = (bytes[byteNum] & 0x01<<(7-mod));
                if(a != 0){
                    node = getRight(node);//.rightChild;
                }else{
                    node = getLeft(node);//.leftChild;
                }
                String text = getPlainText(node);
                if(text != null){
                    decode.append(text);
                    node = nodeRoot;
                }
            }
        }catch(Exception e){
           Log.e("Huffman", "Problem decoding text.");
            e.printStackTrace();
        }
        Log.d("Huffman", "started decode");
        return decode.toString();
    }


    /*
     public static String decode(byte [] bytes, int size){
        if(huffmanRoot == null)
            getTree();
        Log.d("Huffman", "started decode");
        StringBuilder decode = new StringBuilder();
        Huffman node = huffmanRoot;
        int byteNum = -1;
        try{
            for(int i=0;i<size;i++){
                int mod = i % 8;
                if(mod == 0)
                    byteNum++;
                int a = (bytes[byteNum] & 0x01<<(7-mod));
                if(a != 0){
                    node = node.rightChild;
                }else{
                    node = node.leftChild;
                }
                if(node.plainText != null){
                    decode.append(node.plainText);
                    node = huffmanRoot;
                }
            }
        }catch(Exception e){
           Log.e("Huffman", "Problem decoding text.");
            e.printStackTrace();
        }
        Log.d("Huffman", "started decode");
        return decode.toString();
    }

    private static Huffman getPlacementNode(Huffman node, Huffman cameFrom){
        if(node.leftChild == null || node.rightChild == null)
            return node;
        else if(node.leftChild.plainText == null && cameFrom != node.leftChild){
            return getPlacementNode(node.leftChild, null);
        }else{
            return getPlacementNode(node.parent, node);
        }
    }

   public static Huffman enflateTree(String deflated){
        Log.d("Huffman", "starting enflate");
        Huffman root = new Huffman();
        Huffman node = root;
        HuffmanPool pool = new HuffmanPool(500000);
        Log.d("Huffman", "created pool");
        for(int i=1;i<deflated.length();i++){
            Character character = deflated.charAt(i);
            if(character ==  ONE){
                String tempText = null;
                int startIndex = i+1;
                while(i<deflated.length()-1){
                    character = deflated.charAt(++i);
                    if(character == ONE || character == ZERO){
                        tempText = deflated.substring(startIndex, i--);
                        break;
                    }
                }
                //TODO deal with end of deflation!!!!!
                if(tempText == null)
                    tempText = "";
                int tempNode = pool.getHuffman();//new Huffman(tempText);
                getPlainText(tempNode) = tempText;
                node = getPlacementNode(node, null);
                if(node.leftChild == null){
                    node.leftChild = tempNode;
                }else{
                    node.rightChild = tempNode;
                }
                tempNode.parent = node;
            }else{// if(character == ZERO){
                Huffman tempNode = pool.getHuffman();//new Huffman();
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
        Log.d("Huffman", "finishing enflate");
        return root;
    }

       public static class HuffmanPool{
        private Huffman [] buffer;
        private int index = 0;
        private int cap;

        HuffmanPool(int size){
            buffer = new Huffman[size];
            cap = size;
        }

        Huffman getHuffman(){
            if(index >= cap){
                buffer = new Huffman[cap];
                index = 0;
            }
            return buffer[index++];
        }

    }


     */

    private static final Character ZERO = '\u0000';
    private static final Character ONE = '\u0001';


    private static int getPlacementNode(int node, int cameFrom){
        if(getLeft(node) == 0 || getRight(node) == 0)
            return node;
        else{
            int left = getLeft(node);
            if(getPlainText(left) == null && cameFrom != left){
                return getPlacementNode(left, 0);
            }else{
                return getPlacementNode(getParent(node), node);
            }
        }
    }

    public static boolean getTree() {
        Log.d("Huffman", "getTree started");
        try {
            String path = Database.getDbPath() + "/SefariaHuffmanDeflated.txt";
            String compression = readFile(path);

            enflateTree(compression);
            createdTree = true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        Log.d("Huffman", "getTree finished");
        return true;
    }

    static String readFile(String path) throws IOException
    {
        File file = new File(path);
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        fis.close();
        String str = new String(data, "UTF-8");
        return str;

    }
    public static void enflateTree(String deflated){
        Log.d("Huffman", "starting enflate");
        allocate(deflated.length()/2 + 10);
        int node = getNew();
        Log.d("Huffman", "created pool");
        for(int i=1;i<deflated.length();i++){
            Character character = deflated.charAt(i);
            if(character ==  ONE){
                String tempText = null;
                int startIndex = i+1;
                while(i<deflated.length()-1){
                    character = deflated.charAt(++i);
                    if(character == ONE || character == ZERO){
                        tempText = deflated.substring(startIndex, i--);
                        break;
                    }
                }
                //TODO deal with end of deflation!!!!!
                if(tempText == null)
                    tempText = "";
                int tempNode = getNew();//new Huffman(tempText);
                setPlainText(tempNode,tempText);
                node = getPlacementNode(node, 0);
                int left = getLeft(node);
                if(left == 0){
                    setleft(node,tempNode);
                }else{
                    setRight(node,tempNode);
                }
                setParent(tempNode,node);
            }else{// if(character == ZERO){
                int tempNode = getNew();
                node = getPlacementNode(node,0);
                int left = getLeft(node);
                if(left == 0){
                    setleft(node,tempNode);
                }else{
                    setRight(node,tempNode);
                }
                setParent(tempNode,node);
                node = tempNode;
            }
        }
        Log.d("Huffman", "finishing enflate");
        return;
    }




}
