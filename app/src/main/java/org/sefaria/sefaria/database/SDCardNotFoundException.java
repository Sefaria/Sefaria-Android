package org.sefaria.sefaria.database;

/**
 * Created by nss on 3/6/17.
 */

public class SDCardNotFoundException extends Exception{
    public SDCardNotFoundException() {
        super("SD card not found exception");
    }
    public SDCardNotFoundException(String message){
        super(message);
    }
    private static final long serialVersionUID = 613L;
}
