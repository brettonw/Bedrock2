package com.brettonw.bedrock.site;

import com.brettonw.bedrock.bag.*;
import com.brettonw.bedrock.bag.formats.MimeType;
import com.brettonw.bedrock.service.Base;
import com.brettonw.bedrock.service.Event;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.regex.Pattern;

public class Service extends Base {
    private static final Logger log = LogManager.getLogger (Service.class);

    public static final String FETCH_URL = "url";
    public static final String FETCH_CONTENT = "content";
    public static final String FETCH_MIME_TYPE = "mime-type";
    public static final String FETCH_ESCAPE_TYPE = "escape-type";
    public static final String IP_ADDRESS = "ip-address";

    public Service () { }

    public void handleEventEcho (Event event) {
        event.respond (event.getQuery ());
    }

    public void handleEventIpAddress (Event event) {
        event.ok (BagObject.open (IP_ADDRESS, event.getIpAddress ()));
    }

    public void handleEventHeaders (Event event) {
        var request = event.getRequest ();
        var responseBagObject = new BagObject ();
        var headerNames = request.getHeaderNames ();
        while (headerNames.hasMoreElements ()) {
            var headerName = (String) headerNames.nextElement ();
            var headerValue = request.getHeader (headerName);
            responseBagObject.put (headerName, headerValue);
        }
        event.ok (responseBagObject);
    }

    private String unescapeUrl (String urlString) {
        var pattern = Pattern.compile ("[^\\\\]%[0-9a-fA-F]{2}");
        var matcher = pattern.matcher (urlString);
        var sb = new StringBuffer (urlString.length ());
        while (matcher.find ()) {
            var group = matcher.group ();
            var hexVal = matcher.group ().substring (2);
            var intVal = Integer.parseInt (hexVal, 16);
            var s = group.substring (0, 1) + (char) intVal;
            matcher.appendReplacement (sb, s);
        }
        matcher.appendTail (sb);
        return sb.toString ();
    }

    public void handleEventFetch (Event event) {
        try {
            // decode the URL
            var urlString = unescapeUrl (event.getQuery ().getString (FETCH_URL));

            // fetch the requested site and get its mime type (and subtypes)
            var sourceAdapterHttp = new SourceAdapterHttp (urlString);
            var mimeType = sourceAdapterHttp.getMimeType ();
            var mimeSubTypes = mimeType.split ("/");

            // decide how to encode the response, text and text-based protocols that are not JSON
            // need to be escaped so the result can be rebuilt on the receiver side.
            if (mimeSubTypes[0].equals ("text") || mimeType.equals (MimeType.TEXT) || mimeType.equals (MimeType.XML)) {
                var response = sourceAdapterHttp.getStringData ()
                    .replace ("\\", "\\\\")
                    .replace ("\n", "\\n")
                    .replace ("\r", "\\r")
                    .replace ("\f", "\\f")
                    .replace ("\t", "\\t")
                    .replace ("\b", "\\b")
                    .replace ("\"", "\\\"");
                event.ok (new BagObject ().put (FETCH_CONTENT, response).put (FETCH_MIME_TYPE, mimeType).put (FETCH_ESCAPE_TYPE, "text"));
            } else if (mimeType.equals (MimeType.JSON)) {
                // straight JSON content can be embedded
                event.ok (new BagObject ().put (FETCH_CONTENT, BagObjectFrom.string (sourceAdapterHttp.getStringData ())).put (FETCH_MIME_TYPE, mimeType).put (FETCH_ESCAPE_TYPE, "none"));
            } else {
                // XXX right now, we don't do anything with other types, but we might base64 encode
                // XXX them in the future
                event.error ("Unsupported response content type");
            }
        } catch (IOException exception) {
            event.error ("Fetch FAILURE (" + exception.toString () + ")");
        }
    }
}
