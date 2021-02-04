package com.brettonw.bedrock.bag;

import java.util.Arrays;

public class SortKey {
    public static final String KEY = "key";
    public static final String TYPE = "type";
    public static final SortType DEFAULT_TYPE = SortType.ALPHABETIC;
    public static final String ORDER = "order";
    public static final SortOrder DEFAULT_ORDER = SortOrder.ASCENDING;

    public static final SortKey[] DEFAULT = { new SortKey () };

    private String key;
    private SortType type;
    private SortOrder order;

    public SortKey () {
        this ((String) null);
    }

    public SortKey (String key) {
        this (key, DEFAULT_TYPE, DEFAULT_ORDER);
    }

    public SortKey (String key, SortType type, SortOrder order) {
        this.key = key;
        this.type = type;
        this.order = order;
    }

    public SortKey (BagObject bagObject) {
        this (
                bagObject.getString (KEY),
                bagObject.getEnum (TYPE, SortType.class, () -> DEFAULT_TYPE),
                bagObject.getEnum (ORDER, SortOrder.class, () -> DEFAULT_ORDER)
        );
    }

    public String getKey () {
        return key;
    }

    public SortType getType () {
        return type;
    }

    public SortOrder getOrder () {
        return order;
    }

    public SortKey setKey (String key) {
        this.key = key;
        return this;
    }

    public SortKey setType (SortType type) {
        this.type = type;
        return this;
    }

    public SortKey setOrder (SortOrder order) {
        this.order = order;
        return this;
    }

    public int compare (String left, String right) {
        // XXX should consider how to handle nulls
        return switch (type) {
            case ALPHABETIC -> switch (order) {
                case ASCENDING -> left.compareTo(right);
                case DESCENDING -> right.compareTo(left);
            };
            case NUMERIC -> switch (order) {
                case ASCENDING -> Double.valueOf(left).compareTo(Double.valueOf(right));
                case DESCENDING -> Double.valueOf(right).compareTo(Double.valueOf(left));
            };
        };
    }

    public static SortKey[] keys (String... keys) {
        var sortKeys = new SortKey[keys.length];
        for (int i = 0, end = keys.length; i < end; ++i) {
            sortKeys[i] = new SortKey (keys[i]);
        }
        return sortKeys;
    }
    // array of sort keys like [ { "key":"key1" }, { "key":"key2", "type":"alphabetic"}, { "key":"key2", "type":"numeric", "order":"ascending"} ]
    public static SortKey[] keys (BagArray keys) {
        var sortKeys = new SortKey[keys.getCount ()];
        for (int i = 0, end = keys.getCount (); i < end; ++i) {
            sortKeys[i] = new SortKey (keys.getBagObject (i));
        }
        return sortKeys;
    }
}
