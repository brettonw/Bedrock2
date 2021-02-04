package com.brettonw.bedrock.bag.formats;

import com.brettonw.bedrock.bag.BagArray;
import com.brettonw.bedrock.bag.BagObject;
import com.brettonw.bedrock.bag.entry.Handler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FormatReaderTable extends FormatReader implements ArrayFormatReader {
    private static final Logger log = LogManager.getLogger (FormatReaderTable.class);

    private Handler arrayHandler;
    private BagArray titlesArray;

    public FormatReaderTable () {}

    public FormatReaderTable (String input, Handler arrayHandler) {
        this (input, arrayHandler, null);
    }

    /**
     * @param input
     * @param arrayHandler a handler to return an array of arrays
     * @param titlesArray
     */
    public FormatReaderTable (String input, Handler arrayHandler, BagArray titlesArray) {
        super (input);
        this.arrayHandler = arrayHandler;
        this.titlesArray = titlesArray;
    }

    @Override
    public BagArray readBagArray () {
        // get the processed array
        var bagArray = (BagArray) arrayHandler.getEntry (input);
        if (bagArray != null) {
            // filter it for actual entries, check to see if anything is left
            bagArray = bagArray.filter (object -> ((BagArray) object).getCount () > 0);
            if (bagArray.getCount () > 0) {
                // if we have a titles array, use it, otherwise use the first row of the array
                var titlesArray = (this.titlesArray != null) ? this.titlesArray : (BagArray) bagArray.dequeue ();
                var count = titlesArray.getCount ();

                // map each entry to a new bedrock object using the titles array
                var mappedBagArray = new BagArray (bagArray.getCount ());
                bagArray.forEach (object -> {
                    var entryArray = (BagArray) object;
                    if (count == entryArray.getCount ()) {
                        var bagObject = new BagObject (count);
                        for (int i = 0; i < count; ++i) {
                            bagObject.put (titlesArray.getString (i), entryArray.getObject (i));
                        }
                        mappedBagArray.add (bagObject);
                    } else {
                        log.warn ("Mismatched size of entry and titles (skipping row)");
                    }
                });
                bagArray = mappedBagArray;
            }
        }
        return bagArray;
    }
}
