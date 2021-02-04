package com.brettonw.bedrock.bag.entry;

import com.brettonw.bedrock.bag.BagArray;
import com.brettonw.bedrock.bag.BagObject;

public class HandlerObjectFromTitlesArray implements Handler {
    private final BagArray titlesArray;
    private final Handler arrayHandler;

    public HandlerObjectFromTitlesArray (BagArray titlesArray, Handler arrayHandler) {
        this.titlesArray = titlesArray;
        this.arrayHandler = arrayHandler;
    }

    @Override
    public Object getEntry (String input) {
        // read the bedrock array of the input, and check for success
        var bagArray = (BagArray) arrayHandler.getEntry (input);
        if (bagArray != null) {
            // create a bedrock object from the array of entries using the titles array
            var count = titlesArray.getCount ();
            if (count == bagArray.getCount ()) {
                var bagObject = new BagObject (count);
                for (int i = 0; i <count; ++i) {
                    bagObject.put (titlesArray.getString (i), bagArray.getObject (i));
                }

                // return the result
                return bagObject;
            }
        }
        return null;
    }
}
