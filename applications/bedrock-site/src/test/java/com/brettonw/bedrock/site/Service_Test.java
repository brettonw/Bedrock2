package com.brettonw.bedrock.site;

import com.brettonw.bedrock.bag.BagArray;
import com.brettonw.bedrock.bag.BagObject;
import com.brettonw.bedrock.bag.BagObjectFrom;
import com.brettonw.bedrock.servlet.Tester;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class Service_Test extends Service {
    private static final Logger log = LogManager.getLogger (Service_Test.class);

    public static final String ECHO = "echo";
    public static final String HEADERS = "headers";
    public static final String FETCH = "fetch";

    Tester tester;

    public Service_Test () {
        tester = new Tester (this);
    }

    private BagObject bagObjectFromPost (BagObject query) throws IOException {
        return tester.bagObjectFromPost(new BagObject(), query);
    }

    @Test
    public void testPostIP () throws IOException {
        BagObject query = BagObject.open (EVENT, IP_ADDRESS);
        BagObject response = bagObjectFromPost (query);
        assertTrue (response.getString (STATUS).equals (OK));
        String ipAddress = response.getBagObject (RESPONSE).getString (IP_ADDRESS);
        assertTrue (ipAddress != null);
        log.info (IP_ADDRESS + ": " + ipAddress);
    }

    @Test
    public void testPostOk () throws IOException {
        BagObject query = BagObject.open (EVENT, OK);
        BagObject response = bagObjectFromPost (query);
        assertTrue (response.getString (STATUS).equals (OK));
    }

    @Test
    public void testPostHeaders () throws IOException {
        BagObject query = BagObject.open (EVENT, HEADERS);
        BagObject response = bagObjectFromPost (query);
        assertTrue (response.getString (STATUS).equals (OK));
    }

    @Test
    public void testEmptyPost () throws IOException {
        BagObject response = bagObjectFromPost (new BagObject ());
        assertTrue (response.getString (STATUS).equals (ERROR));
    }

    @Test
    public void testGet () throws IOException {
        BagObject response = tester.bagObjectFromGet ("");
        assertTrue (response.getString (STATUS).equals (ERROR));
    }
}
