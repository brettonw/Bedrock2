package com.brettonw.bedrock.service;

import com.brettonw.bedrock.bag.*;
import com.brettonw.bedrock.servlet.Tester;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Bootstrap_Test extends Base {
    Tester tester;

    public Bootstrap_Test () {
        super("xxx.json");
        tester = new Tester (this);
    }

    public void handleEventHello (Event event) {
        event.ok (BagObject.open  ("testing", "123"));
    }

    public void handleEventGoodbye (Event event) {
        event.ok (BagObject.open ("testing", "456"));
    }

    public void handleEventDashName (Event event) {
        event.ok ();
    }

    private BagObject bagObjectFromPost (BagObject query) throws IOException {
        return tester.bagObjectFromPost(new BagObject (), query);
    }

    private void assertQuery(BagObject bagObject, BagObject query, String status) {
        assertTrue (bagObject.getString (STATUS).equals (status));
        assertTrue (bagObject.getBagObject (QUERY).equals (query));
    }

    @Test
    public void testAttribute () {
        assertTrue (getContext () != null);
        assertTrue (getAttribute (SERVLET) == this);
    }

    @Test
    public void testBadInstall () {
        String event = "JUNK";
        assertFalse (install ("JUNK"));
    }

    @Test
    public void testUnknownEvent () throws IOException {
        BagObject query = BagObject.open (EVENT, "nohandler");
        assertTrue (bagObjectFromPost (query).getString (STATUS).equals (ERROR));
    }

    @Test
    public void testMissingHandler () throws IOException {
        BagObject query = BagObject.open (EVENT, "no-handler");
        assertTrue (bagObjectFromPost (query).getString (STATUS).equals (ERROR));
    }

    @Test
    public void testGet () throws IOException {
        BagObject query = BagObject
                .open (EVENT, "hello")
                .put ("param1", 1)
                .put ("param2", 2);
        assertTrue (tester.bagObjectFromGet (query).getString (STATUS).equals (ERROR));
    }

    @Test
    public void testPostOk () throws IOException {
        BagObject query = BagObject.open (EVENT, OK);
        assertQuery(bagObjectFromPost (query), query, OK);

        query.put ("param4", 4);
        assertQuery(bagObjectFromPost (query), query, OK);
    }

    @Test
    public void testPost () throws IOException {
        BagObject testPost = BagObjectFrom.resource (getClass (), "/testPost.json");
        BagObject query = BagObject
                .open (EVENT, "goodbye")
                .put ("param1", 1)
                .put ("param2", 2)
                .put ("testPost", testPost);
        BagObject response = bagObjectFromPost (query);
        assertQuery(response, query, OK);
        assertTrue (response.getBagObject (QUERY).has ("testPost"));
        assertTrue (response.getBagObject (QUERY).getBagObject ("testPost").equals (testPost));

        query.put ("param3", 3);
        response = bagObjectFromPost (query);
        assertQuery(response, query, OK);
        assertTrue (response.getBagObject (QUERY).has ("testPost"));
        assertTrue (response.getBagObject (QUERY).getBagObject ("testPost").equals (testPost));

        query.put ("param4", 4);
        assertTrue (bagObjectFromPost (query).getString (STATUS).equals (OK));
        query.remove ("param4");

        query.put ("param3", 2);
        assertTrue (bagObjectFromPost (query).getString (STATUS).equals (OK));
    }

    @Test
    public void testEmptyRequest () throws IOException {
        BagObject response = bagObjectFromPost (new BagObject ());
        assertTrue (response.getString (STATUS).equals (ERROR));
        assertTrue (response.getString (Key.cat (ERROR, 0)).equals ("Missing '" + EVENT + "'"));
    }

    @Test
    public void testHelp () throws IOException {
        BagObject query = BagObject.open (EVENT, HELP);
        BagObject response = bagObjectFromPost (query);
        assertTrue (response.getString (STATUS).equals (OK));

        // make sure the response matches the schema
        assertTrue (response.getBagObject (RESPONSE).equals (getSchema ()));
    }

    @Test
    public void testBadPost () throws IOException {
        BagObject query = BagObject
                .open (EVENT, "halp")
                .put ("param1", 1)
                .put ("param2", 2);
        assertTrue (bagObjectFromPost (query).getString (STATUS).equals (ERROR));
    }

    @Test
    public void testBadParameters () throws IOException {
        BagObject query = BagObject
                .open (EVENT, "hello")
                .put ("param1", 1)
                .put ("param3", 3);
        assertTrue (bagObjectFromPost (query).getString (STATUS).equals (OK));
    }

    @Test
    public void testVersion () throws IOException {
        BagObject query = BagObject.open (EVENT, VERSION);
        BagObject response = bagObjectFromPost (query);
        assertTrue (response.getString (STATUS).equals (OK));
    }

    @Test
    public void testMultiple () throws IOException {
        BagObject query = BagObject
                .open (EVENT, MULTIPLE)
                .put (EVENTS, BagArray
                        .open (BagObject.open (EVENT, VERSION))
                        .add (BagObject.open (EVENT, HELP))
                        .add (BagObject.open (EVENT, OK))
                );
        var response = bagObjectFromPost (query);
        assertQuery (response, query, OK);
    }

    @Test
    public void testDashName () throws IOException {
        BagObject query = BagObject.open (EVENT, "dash-name");
        assertQuery(bagObjectFromPost (query), query, OK);
    }
}
