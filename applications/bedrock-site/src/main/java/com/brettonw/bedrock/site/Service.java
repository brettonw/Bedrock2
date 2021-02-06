package com.brettonw.bedrock.site;

import com.brettonw.bedrock.bag.*;
import com.brettonw.bedrock.bag.formats.MimeType;
import com.brettonw.bedrock.service.Base;
import com.brettonw.bedrock.service.Event;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.regex.Pattern;

public class Service extends Base {
    private static final Logger log = LogManager.getLogger (Service.class);

    public static final String FETCH_URL = "url";
    public static final String FETCH_CONTENT = "content";
    public static final String FETCH_MIME_TYPE = "mime-type";
    public static final String FETCH_ESCAPE_TYPE = "escape-type";
    public static final String IP_ADDRESS = "ip-address";

    public Service () { }

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
}
