package com.brettonw.bedrock.bag.formats;

// The FormatReaderJson is loosely modeled after a JSON parser grammar from the site (http://www.json.org).
// The main difference is that we ignore differences between value types (all of them will be
// strings internally), and assume the input is a well formed string representation of a BagObject
// or BagArray in JSON-ish format

import com.brettonw.bedrock.bag.BagArray;
import com.brettonw.bedrock.bag.BagObject;

import java.util.Arrays;

public class FormatReaderJson extends FormatReaderParsed implements ArrayFormatReader, ObjectFormatReader {
    public FormatReaderJson () {}

    public FormatReaderJson (String input) {
        super (input);
    }

    @Override
    public BagArray readBagArray () {
        // <Array> :: [ ] | [ <Elements> ]
        var bagArray = new BagArray ();
        return (expect('[') && readElements (bagArray) && require(']')) ? bagArray : null;
    }

    private boolean storeValue (BagArray bagArray) {
        // the goal here is to try to read a "value" from the input stream, and store it into the
        // BagArray. BagArrays can store null values, so we have a special handling case to make
        // sure we properly convert "null" string to null value - as distinguished from a failed
        // read, which returns null value to start.the method returns true if a valid value was
        // fetched from the stream (in which case it was added to the BagArray)
        var value = readValue ();
        if (value != null) {
            // special case for "null"
            if ((value instanceof String) && (((String) value).equalsIgnoreCase ("null"))) {
                value = null;
            }
            bagArray.add (value);
            return true;
        }
        return false;
    }

    private boolean readElements (BagArray bagArray) {
        // <Elements> ::= <Value> | <Value> , <Elements>
        var result = true;
        if (storeValue (bagArray)) {
            while (expect (',')) {
                result = require (storeValue (bagArray), "Valid value");
            }
        }
        return result;
    }

    @Override
    public BagObject readBagObject () {
        // <Object> ::= { } | { <Members> }
        var bagObject = new BagObject ();
        return (expect('{') && readMembers (bagObject) && require(expect ('}'), "Valid pair (<String>:<Value>) or '}'")) ? bagObject : null;
    }

    private boolean readMembers (BagObject bagObject) {
        // <Members> ::= <Pair> | <Pair> , <Members>
        var result = true;
        if (readPair (bagObject)) {
            while (expect (',')) {
                result = require (readPair (bagObject), "Valid pair (<String>:<Value>)");
            }
        }
        return result;
    }

    private boolean storeValue (BagObject bagObject, String key) {
        // the goal here is to try to read a "value" from the input stream, and store it into the
        // BagObject. BagObject can NOT store null values, so we have a special handling case to
        // make sure we properly convert "null" string to null value - as distinguished from a failed
        // read, which returns null value to start. the method returns true if a valid value was
        // fetched from the stream, regardless of whether a null value was stored in the BagObject.
        var value = readValue ();
        if (value != null) {
            // special case for "null"
            if (!((value instanceof String) && (((String) value).equalsIgnoreCase ("null")))) {
                bagObject.put (key, value);
            }
            return true;
        }
        return false;
    }

    private boolean readPair (BagObject bagObject) {
        // <Pair> ::= <String> : <Value>
        var key = readString ();
        return (key != null) && (key.length () > 0) && require (':') && require (storeValue (bagObject, key), "Valid value");
    }

    private static final char[] BARE_VALUE_STOP_CHARS = sortString (" \u00a0\t\n:{}[]\",");
    private static final char[] QUOTED_STRING_STOP_CHARS = sortString ("\n\"");

    private static char[] sortString (String string) {
        var chars = string.toCharArray ();
        Arrays.sort (chars);
        return chars;
    }

    private boolean notIn (char[] stopChars, char c) {
        int i = 0;
        int end = stopChars.length;
        char stopChar = 0;
        while ((i < end) && (c > (stopChar = stopChars[i]))) {
            ++i;
        }
        return stopChar != c;
    }

    private int consumeUntilStop (char[] stopChars) {
        var start = index;
        char c;
        //while (check () && (Arrays.binarySearch (stopChars, c = input.charAt (index)) < 0)) {
        while (check () && notIn (stopChars, (c = input.charAt (index)))) {
            // using the escape mechanism is like a free pass for the next character, but we
            // don't do any transformation on the substring, just return it as written
            index += (c == '\\') ? 2 : 1;
        }
        return start;
    }

    private String readString () {
        // " chars " | <chars>
        var result = (String) null;
        if (expect('"')) {
            // digest the string, and be sure to eat the end quote
            var start = consumeUntilStop (QUOTED_STRING_STOP_CHARS);
            result = input.substring (start, index++);
        }
        return result;
    }

    private String readBareValue () {
        // " chars " | <chars>
        var result = (String) null;

        // technically, we're being sloppy allowing bare values in some cases where quoted strings
        // are the standard, but it's part of the simplified structure we support. This allows us to
        // read valid JSON files without handling every single pedantic case.
        var start = consumeUntilStop (BARE_VALUE_STOP_CHARS);

        // capture the result if we actually consumed some characters
        if (index > start) {
            result = input.substring (start, index);
        }

        return result;
    }

    private Object readValue () {
        // <Value> ::= <String> | <Object> | <Array>
        consumeWhiteSpace ();
        return check () ? switch (input.charAt(index)) {
            case '{' -> readBagObject();
            case '[' -> readBagArray();
            case '"' -> readString();
            default -> readBareValue();
        } : null;
    }

    static {
        MimeType.addExtensionMapping (MimeType.JSON, "json");
        MimeType.addMimeTypeMapping (MimeType.JSON, "text/json");
        FormatReader.registerFormatReader (MimeType.JSON, false, FormatReaderJson::new);
    }
}
