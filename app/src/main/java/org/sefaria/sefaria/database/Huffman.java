package org.sefaria.sefaria.database;

import android.os.AsyncTask;
import android.util.Log;

import org.sefaria.sefaria.GoogleTracker;
import org.sefaria.sefaria.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class Huffman {

    private static String [] plainTexts;
    private static int [] leftChilds;
    private static int [] rightChilds;
    private static int [] parents;

    private static int index = 1;
    private static boolean createdTree = false;
    private static boolean startedTree = false;

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


    public static StringBuilder printedTree = new StringBuilder();
    private static void printTree(int node, String tabs,String path){
        if(node == 0) {
            return;
        }
        if(getPlainText(node) != null)
            printedTree.append(path + "\t:" + "_" + getPlainText(node) + "_\n");
        //Log.d("Huffman", );
        printTree(getLeft(node), tabs + "\t",path + "0");
        printTree(getRight(node), tabs + "\t",path + "1");
    }

    public String makeString(int index) {
        String plainText = getPlainText(index);
        if(plainText == null)
            return hashCode() + "_";
        return plainText + ":";
    }

    private static final int nodeRoot = 1;
    public static String decode(byte [] bytes, int size){
        makeTree(false);
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
                    node = getRight(node);
                }else{
                    node = getLeft(node);
                }
                String text = getPlainText(node);
                if(text != null){
                    //text = "{"  + text + "}"; //THIS IS MAJOR DEBUGGING!!//+byteString.toString() + ":"
                    decode.append(text);
                    node = nodeRoot;
                }
            }
        }catch(Exception e){
           Log.e("Huffman", "Problem decoding text.");
            e.printStackTrace();
        }
        return decode.toString();
    }

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

    private static boolean makeTree(){
        if(!Database.hasOfflineDB())
            return false;
        Log.d("Huffman", "getTree started");
        long startTime= System.currentTimeMillis();
        try {
            String path = Database.getDbPath() + "/SefariaHuffmanDeflated.txt";
            String deflated = Util.readFile(path);

            int size = Database.getDBSetting("huffmanSize",false);
            if(size == Database.BAD_SETTING_GET) {
                Log.e("Huffman", "BAD_SETTING_GET" + size);
                size = ((int) Math.ceil(deflated.length() / 2.5));
            }
            allocate(size + 10);//it really only needs to be +2, but just to be safe
            int node = getNew();
            for(int i=1;i<deflated.length();i++){
                Character character = deflated.charAt(i);
                if(character ==  ONE){
                    String tempText = null;
                    int startIndex = i+1;
                    while(i<deflated.length()-1){
                        character = deflated.charAt(++i);
                        if(character == ONE || character == ZERO){
                            break;
                        }
                    }
                    if(i == deflated.length() -1) i++; //make sure to get the very last character
                    tempText = deflated.substring(startIndex, i--);
                    if(tempText == null)
                        tempText = "";
                    int tempNode = getNew();
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
            createdTree = true;
        } catch (Exception e) {
            //e.printStackTrace();
            Log.e("Huffman", "problems getting huffman tree");
            return false;
        }
        Log.d("Huffman", "getTree finished");
        GoogleTracker.sendEvent(GoogleTracker.CATEGORY_STATUS_INFO,"Huffman tree finished time",(System.currentTimeMillis() - startTime) );
        return true;
    }

    public static boolean makeTree(boolean doAsync) {
        if(createdTree)
            return true;
        else if (startedTree && doAsync) {
            return false;
        }else if(startedTree && !doAsync){
            while(!createdTree && startedTree) {
                try {
                   Thread.sleep(20);
                } catch (InterruptedException e) {
                   e.printStackTrace();
                }
            }
            return createdTree;
        }else if(doAsync) { //&& !startedTree
            Huffman huffman = new Huffman();
            huffman.new makeTreeAsync().execute();
            return false;
        }else{ //(!doAsync && !startedTree)
            return makeTree();
        }

    }




    private class makeTreeAsync extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            if(startedTree)
                return null;
            Log.d("Huffman", "Async task started");
            startedTree = true;
            makeTree();

            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            startedTree = false;
        }
    }

}
