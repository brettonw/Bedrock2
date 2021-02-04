package com.brettonw.bedrock.bag;

/**
 * A helper class for composing paths used in BagObject indexing.
 */
public final class Key {
    Key () {}

    /**
     * Concatenate multiple string components to make a path
     * @param components the different levels of the hierarchy to index
     * @return a String with the components in path form for indexing into a bedrock object
     */
    public static String cat (Object... components) {
        var stringBuilder = new StringBuilder ();
        var separator = "";
        for (var component : components) {
            stringBuilder.append(separator).append (component.toString());
            separator = BagObject.PATH_SEPARATOR;
        }
        return stringBuilder.toString ();
    }

    static String[] split (String key) {
        return key.split (BagObject.PATH_SEPARATOR, 2);
    }
}
