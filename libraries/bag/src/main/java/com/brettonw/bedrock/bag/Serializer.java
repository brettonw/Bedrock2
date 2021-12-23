package com.brettonw.bedrock.bag;

import com.brettonw.bedrock.logger.*;


import sun.misc.Unsafe;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

public class Serializer {
    private static final Logger log = LogManager.getLogger (Serializer.class);

    // the static interface
    private static final String BASE64_ENCODED = "__BASE64_ENCODED__";
    private static final String SPECIAL_CHAR = "@";
    public static final String TYPE_KEY = SPECIAL_CHAR + "type";
    public static final String VERSION_KEY = SPECIAL_CHAR + "v";
    public static final String KEY_KEY = SPECIAL_CHAR + "key";
    public static final String VALUE_KEY = SPECIAL_CHAR + "value";

    // future changes might require the serializer to know a different type of encoding is expected.
    // we use a two step version, where changes in the ".x" region don't require a new deserializer
    // but for which we want old version of serialization to fail. changes in the "1." region
    // indicate a completely new deserializer is needed. we will not ever support serializing to
    // older formats (link against the old version of this package if you want that). we will decide
    // whether or not to support multiple deserializer formats when the time comes.
    public static final String SERIALIZER_VERSION_1 = "1.0";
    public static final String SERIALIZER_VERSION_2 = "2";
    public static final String SERIALIZER_VERSION_3 = "3";
    public static final String SERIALIZER_VERSION_4 = "4";
    public static final String SERIALIZER_VERSION_5 = "5.0";
    public static final String SERIALIZER_VERSION = SERIALIZER_VERSION_5;

    // flags to clarify whether or not to include/expect version
    private static final boolean WITH_VERSION = true;
    private static final boolean WITHOUT_VERSION = false;

    // maps and sets to help with determining boxed type relationships
    private static final Map<String, Class> BOXED_TYPES_MAP;
    private static final Set<Class> BOXED_TYPES_SET;


    private static Unsafe getUnsafe () {
        // try to get the unsafe object using reflection to avoid a security exception
        // https://stackoverflow.com/questions/13003871/how-do-i-get-the-instance-of-sun-misc-unsafe
        try {
            var theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            return (Unsafe) theUnsafe.get(null);
        } catch (Exception exception) {
            log.error ("Deserialization of Java Objects will not be supported.", exception);
        }
        return null;
    }
    private static final Unsafe unsafe = getUnsafe ();

    static {
        BOXED_TYPES_MAP = new HashMap<> ();
        BOXED_TYPES_MAP.put ("int", Integer.class);
        BOXED_TYPES_MAP.put ("long", Long.class);
        BOXED_TYPES_MAP.put ("short", Short.class);
        BOXED_TYPES_MAP.put ("byte", Byte.class);
        BOXED_TYPES_MAP.put ("char", Character.class);
        BOXED_TYPES_MAP.put ("boolean", Boolean.class);
        BOXED_TYPES_MAP.put ("float", Float.class);
        BOXED_TYPES_MAP.put ("double", Double.class);

        BOXED_TYPES_SET = new HashSet<> ();
        for (var key : BOXED_TYPES_MAP.keySet ()) {
            BOXED_TYPES_SET.add (BOXED_TYPES_MAP.get (key));
        }
    }

    private static Class getClass (String typeString) throws ClassNotFoundException {
        return Serializer.class.getClassLoader ().loadClass (typeString);
    }

    // different types of objects are handled differently by the serializer, this is roughly how we
    // group those types
    enum SerializationType {
        PRIMITIVE,
        ENUM,
        BAG_OBJECT,
        BAG_ARRAY,
        JAVA_OBJECT,
        COLLECTION,
        MAP,
        ARRAY
    }

    private static boolean isBoxedPrimitive (Class type) {
        // boxed primitives and strings...
        return BOXED_TYPES_SET.contains (type) || (type.equals (String.class));
    }

    private static Class getBoxedType (String typeString) throws ClassNotFoundException {
        return BOXED_TYPES_MAP.containsKey (typeString)
                ? BOXED_TYPES_MAP.get (typeString)
                : getClass (typeString);
    }

    private static Class getBoxedType (Class type) {
        var boxedType = BOXED_TYPES_MAP.get (type.getName ());
        return (boxedType != null) ? boxedType : type;
    }

    private static SerializationType serializationType (Class type) {
        if (type.isPrimitive () || isBoxedPrimitive (type)) return SerializationType.PRIMITIVE;
        if (type.isEnum ()) return SerializationType.ENUM;
        if (type.isArray ()) return SerializationType.ARRAY;
        if (Collection.class.isAssignableFrom (type)) return SerializationType.COLLECTION;
        if (Map.class.isAssignableFrom (type)) return SerializationType.MAP;
        if (BagObject.class.isAssignableFrom (type)) return SerializationType.BAG_OBJECT;
        if (BagArray.class.isAssignableFrom (type)) return SerializationType.BAG_ARRAY;

        // if it's none of the above...
        return SerializationType.JAVA_OBJECT;
    }

    private static SerializationType serializationType (String typeString) throws ClassNotFoundException {
        return  (typeString.charAt (0) == '[')
                ?  SerializationType.ARRAY
                :  serializationType (getBoxedType (typeString));
    }

    private static Set<Field> getAllFields (Set<Field> fields, Class type) {
        // recursively walk up the class declaration to gather all of the fields, but stop when we
        // hit the top of the class hierarchy
        if (type != null) {
            fields.addAll (Arrays.asList (type.getFields ()));
            fields.addAll (Arrays.asList (type.getDeclaredFields ()));
            getAllFields (fields, type.getSuperclass ());
        }
        return fields;
    }

    private static BagObject serializeJavaObjectType (Object object, Class type) {
        // this bedrock object will hold the value(s) of the fields
        var bagObject = new BagObject ();
        try {
            @SuppressWarnings("unchecked")
            // gather all of the fields declared; public, private, static, etc., then loop over them
            var fieldSet = getAllFields(new HashSet<>(), type);
            for (var field : fieldSet) {
                // check if the field is static, we don't want to serialize any static values, as this
                // leads to recursion
                if (!Modifier.isStatic(field.getModifiers())) {
                    // force accessibility for serialization - this is an issue with the reflection API
                    // that we want to step around because serialization is assumed to be the primary
                    // goal, as opposed to viewing a way to workaround an API that needs to be over-
                    // ridden. This should prevent the IllegalAccessException from ever happening.

                    // XXX 02/02/2021 Java 14 conversion is simply not allowing this without a-priori
                    // knowledge of the class being serialized as a serializable type - i.e. java core
                    // objects must be "opened" to our package, which is just brilliant (not)

                    // https://blogs.oracle.com/javamagazine/the-unsafe-class-unsafe-at-any-speed
                    // http://marxsoftware.blogspot.com/2018/06/future-of-java-serialization.html
                    field.setAccessible(true);

                    // get the name and type, and get the value to encode
                    try {
                        // only serialize this field if it has a value
                        var fieldObject = field.get(object);
                        if (fieldObject != null) {
                            // if the type of the object is not a subclass of the field type, serialize it
                            // directly - otherwise, serialize with type
                            var fieldObjectType = getBoxedType(fieldObject.getClass());
                            var fieldType = getBoxedType(field.getType());
                            if (fieldObjectType.isAssignableFrom(fieldType)) {
                                bagObject.put(field.getName(), serialize(fieldObject));
                            } else {
                                bagObject.put(field.getName(), serializeWithType(fieldObject, WITHOUT_VERSION));
                            }
                        }
                    } catch (IllegalAccessException exception) {
                        // NOTE this shouldn't happen, per the comments above, and is untestable for
                        // purpose of measuring coverage
                        log.debug(exception);
                    }
                }
            }
        } catch (InaccessibleObjectException exception) {
            log.warn (exception);

            // fallback... if we got here we are dealing with an object that is not opened to us, so
            // we can't get and set its members directly. we're going to try to use the java
            // serialization interface to write a text encoded version of the binary stream result.
            if (object instanceof Serializable) {
                try {
                    var byteArrayOutputStream = new ByteArrayOutputStream();
                    var objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
                    objectOutputStream.writeObject(object);
                    var encodedObject = Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());
                    bagObject.put(BASE64_ENCODED, encodedObject);
                    log.info ("Object serialized via BASE-64 encoding");
                } catch (IOException ioException) {
                    // XXX well #^@#$&! - we'll return an empty serialization
                    log.error ("Object not serialized", exception);
                }
            } else {
                // XXX well #^@#$&! - we'll return an empty serialization
                log.error ("Object not serializable");
            }
        }
        return bagObject;
    }

    private static BagArray serializeArrayType (Object object) {
        var length = Array.getLength (object);
        var bagArray = new BagArray (length);
        for (int i = 0; i < length; ++i) {
            // serialized containers could use base classes as the container type specifier, so we
            // have to instantiate each object individually
            bagArray.add (serializeWithType (Array.get (object, i), WITHOUT_VERSION));
        }
        return bagArray;
    }

    private static BagArray serializeMapType (Map object) {
        var keys = object.keySet ().toArray ();
        var bagArray = new BagArray (keys.length);
        for (var key : keys) {
            var item = object.get (key);
            // serialized containers could use base classes as the container type specifier, so we
            // have to instantiate each object individually
            var pair = new BagObject (2)
                    .put (KEY_KEY, serializeWithType (key, WITHOUT_VERSION))
                    .put (VALUE_KEY, serializeWithType (item, WITHOUT_VERSION));
            bagArray.add (pair);
        }
        return bagArray;
    }

    static Object serialize (Object object) {
        // fill out the header of the encapsulating bedrock
        var type = object.getClass ();

        // the next step depends on the actual type of what's being serialized
        return switch (serializationType(type)) {
            case PRIMITIVE -> object;
            case ENUM -> object.toString();
            case BAG_OBJECT, BAG_ARRAY -> object;
            case JAVA_OBJECT -> serializeJavaObjectType(object, type);
            case COLLECTION -> serializeArrayType(((Collection) object).toArray());
            case MAP -> serializeMapType((Map) object);
            case ARRAY -> serializeArrayType(object);
        };
    }

    private static BagObject serializeWithType (Object object, boolean emitVersion) {
        if (object != null) {
            // build an encapsulation for the serializer
            return (emitVersion ? new BagObject (3).put (VERSION_KEY, SERIALIZER_VERSION) : new BagObject (2))
                    .put (TYPE_KEY, object.getClass ().getName ())
                    .put (VALUE_KEY, serialize (object));
        }
        return null;
    }

    /**
     * Return the @value component of a serialized object
     *
     * @param bagObject the BagObject representing the serialized object.
     * @return A BagObject from the @value component, or null if it's not a valid serialization.
     */
    public static BagObject Unwrap (BagObject bagObject) {
        return bagObject.getBagObject (VALUE_KEY);
    }

    /**
     * Convert the given object to a BagObject representation that can be used to reconstitute the
     * given object after serialization.
     *
     * @param object the target element to serialize. It must be one of the following: primitive,
     *               boxed-primitive, Plain Old Java Object (POJO) class, object class with getters
     *               and setters for all members, BagObject, BagArray, array, or list or map-based
     *               container of one of the previously mentioned types.
     * @return A BagObject encapsulation of the target object, or null if the conversion failed.
     */
    public static BagObject toBagObject (Object object) {
        return serializeWithType (object, WITH_VERSION);
    }

    private static Object deserializePrimitiveType (String typeString, Object object) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException {
        @SuppressWarnings("unchecked")
        var string = (String) object;
        var type = getBoxedType (typeString);

        // Character types don't have a constructor from a String, so we have to handle that as a
        // special case. Fingers crossed we don't find any others
        return (type.isAssignableFrom (Character.class))
                ? type.getConstructor (char.class).newInstance (string.charAt (0))
                : type.getConstructor (String.class).newInstance (string);
    }

    private static Object deserializeJavaEnumType (String typeString, Object object) throws ClassNotFoundException {
        @SuppressWarnings("unchecked")
        var type = getClass (typeString);
        return Enum.valueOf (type, (String) object);
    }

    private static Object deserializeJavaObjectType (String typeString, Object object) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        // get requested type from the local classloader, it has to know...
        // "In this dirty old part of the city, Where the sun refused to shine..."
        var type = getClass (typeString);

        // use unsafe to allocate an instance without calling a constructor
        var target = (Object) null;
        if (unsafe != null) {
            target = unsafe.allocateInstance(type);

            // Wendy, is the water warm enough? Yes, Lisa. (Prince, RIP)
            // gather all of the fields declared; public, private, static, etc., then loop over them
            var bagObject = (BagObject) object;
            if (bagObject.has(BASE64_ENCODED)) {
                try {
                    // an object that wasn't open to us was serialized using the java serialization
                    // interface, and we want to de-serialize the object using the java serialization
                    // interface in reverse. our source data is a text encoded version of the binary
                    // stream - decode it and feed it to Java
                    var bytes = Base64.getDecoder().decode(bagObject.getString(BASE64_ENCODED));
                    var byteArrayInputStream = new ByteArrayInputStream(bytes);
                    var inputStream = new ObjectInputStream(byteArrayInputStream);
                    target = inputStream.readObject();
                    log.info("Object deserialized via BASE-64 decoding");
                } catch (IOException exception) {
                    log.error("Failed to deserialize using Java serialization", exception);
                    // XXX and do what exactly...
                }
            } else {
                // get all the fields for the type and iterate over them to restore the ones we have. we
                // don't expect this is a panacea, it's mostly for open types we use, or for POJOs that
                // should be serialized with readable text.
                var fieldSet = getAllFields(new HashSet<>(), type);
                for (var field : fieldSet) {
                    // only populate this field if we serialized it - the assumption being: if we
                    // serialized the field, then it is deserializable.
                    if (bagObject.has(field.getName())) {
                        // force accessibility for serialization, as above... this should prevent the
                        // IllegalAccessException from ever happening.
                        field.setAccessible(true);

                        // get the name and type, and set the value from the encode value
                        //log.trace ("Add " + field.getName () + " as " + field.getType ().getName ());
                        var fieldObject = bagObject.getObject(field.getName());
                        var fieldType = field.getType().getName();
                        if ((fieldObject instanceof BagObject) && (((BagObject) fieldObject).getString(TYPE_KEY) != null)) {
                            field.set(target, deserializeWithType((BagObject) fieldObject, WITHOUT_VERSION));
                        } else {
                            field.set(target, deserialize(fieldType, fieldObject));
                        }
                    } else {
                        // warn about skipping a non-static field
                        if (!Modifier.isStatic(field.getModifiers())) {
                            log.warn("Skipping non-static field initializer (" + field.getName() + "), not in source bedrock object");
                        }
                    }
                }
            }
        }
        return target;
    }

    private static Object deserializeCollectionType (String typeString, Object object) throws ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        var type = getClass (typeString);
        var target = (Collection) type.getConstructor().newInstance ();
        var bagArray = (BagArray) object;
        for (int i = 0, end = bagArray.getCount (); i < end; ++i) {
            target.add (deserializeWithType (bagArray.getBagObject (i), WITHOUT_VERSION));
        }
        return target;
    }

    private static Object deserializeMapType (String typeString, Object object) throws ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        var type = getClass (typeString);
        var target = (Map) type.getConstructor().newInstance ();
        var bagArray = (BagArray) object;
        for (int i = 0, end = bagArray.getCount (); i < end; ++i) {
            var entry = bagArray.getBagObject (i);
            var key = deserializeWithType (entry.getBagObject (KEY_KEY), WITHOUT_VERSION);
            var value = deserializeWithType (entry.getBagObject (VALUE_KEY), WITHOUT_VERSION);
            target.put (key, value);
        }
        return target;
    }

    private static Class getArrayType (String typeName) throws ClassNotFoundException {
        var arrayDepth = 0;
        while (typeName.charAt (arrayDepth) == '[') { ++arrayDepth; }
        switch (typeName.substring (arrayDepth)) {
            case "B": return byte.class;
            case "C": return char.class;
            case "D": return double.class;
            case "F": return float.class;
            case "I": return int.class;
            case "J": return long.class;
            case "S": return short.class;
            case "Z": return boolean.class;

            case "Ljava.lang.Byte;": return Byte.class;
            case "Ljava.lang.Character;": return Character.class;
            case "Ljava.lang.Double;": return Double.class;
            case "Ljava.lang.Float;": return Float.class;
            case "Ljava.lang.Integer;": return Integer.class;
            case "Ljava.lang.Long;": return Long.class;
            case "Ljava.lang.Short;": return Short.class;
            case "Ljava.lang.Boolean;": return Boolean.class;
        }

        // if we get here, the type is either a class name, or ???
        if (typeName.charAt (arrayDepth) == 'L') {
            var semiColon = typeName.indexOf (';');
            typeName = typeName.substring (arrayDepth + 1, semiColon);
            // note that this could throw ClassNotFound if the typeName is not legitimate.
            return getClass (typeName);
        }

        // this will only happen if we are deserializing from modified source
        throw new ClassNotFoundException(typeName);
    }

    private static int[] getArraySizes (String typeString, BagArray bagArray) {
        // figure the array dimension
        var dimension = 0;
        while (typeString.charAt (dimension) == '[') { ++dimension; }

        // create and populate the sizes array
        var sizes = new int[dimension];
        for (int i = 0; i < dimension; ++i) {
            sizes[i] = bagArray.getCount ();
            bagArray = bagArray.getBagObject (0).getBagArray (VALUE_KEY);
        }

        // return the result
        return sizes;
    }

    private static void populateArray(int x, int[] arraySizes, Object target, BagArray bagArray) {
        if (x < (arraySizes.length - 1)) {
            // we should recur for each value to populate a sub-array
            for (int i = 0, end = arraySizes[x]; i < end; ++i) {
                var nextTarget = Array.get (target, i);
                var bagObject = bagArray.getBagObject (i);
                populateArray (x + 1, arraySizes, nextTarget, bagObject.getBagArray (VALUE_KEY));
            }
        } else {
            // we should set each value
            for (int i = 0, end = arraySizes[x]; i < end; ++i) {
                Array.set (target, i, deserializeWithType (bagArray.getBagObject (i), WITHOUT_VERSION));
            }
        }
    }

    private static Object deserializeArrayType (String typeString, Object object) throws ClassNotFoundException {
        var bagArray = (BagArray) object;
        var arraySizes = getArraySizes (typeString, bagArray);
        var type = getArrayType (typeString);
        var target = Array.newInstance (type, arraySizes);
        populateArray (0, arraySizes, target, bagArray);
        return target;
    }

    private static boolean checkVersion (boolean expectVersion, BagObject bagObject) throws BadVersionException {
        if (expectVersion) {
            var version = bagObject.getString (VERSION_KEY);
            if (!SERIALIZER_VERSION.equals (version)) {
                throw new BadVersionException (version, SERIALIZER_VERSION);
            }
        }
        return true;
    }

    private static Object deserialize (String typeString, Object object) {
        try {
            return switch (serializationType(typeString)) {
                case PRIMITIVE -> deserializePrimitiveType(typeString, object);
                case ENUM -> deserializeJavaEnumType(typeString, object);
                case BAG_OBJECT, BAG_ARRAY -> object;
                case JAVA_OBJECT -> deserializeJavaObjectType(typeString, object);
                case COLLECTION -> deserializeCollectionType(typeString, object);
                case MAP -> deserializeMapType(typeString, object);
                case ARRAY -> deserializeArrayType(typeString, object);
            };
        } catch (Exception exception) {
            log.error (exception);
        }
        return null;
    }

    private static Object deserializeWithType (BagObject bagObject, boolean expectVersion) {
        return ((bagObject != null) && checkVersion (expectVersion, bagObject))
                ? deserialize (bagObject.getString (TYPE_KEY), bagObject.getObject (VALUE_KEY))
                : null;
    }

    /**
     * Reconstitute the given BagObject representation back to the object it represents.
     *
     * @param <WorkingType> template parameter for the type to return
     * @param bagObject the target BagObject to deserialize. It must be a valid representation of
     *                  the encoded type(i.e. created by the toBagObject method).
     * @return the reconstituted object, or null if the reconstitution failed.
     */
    public static <WorkingType> WorkingType fromBagObject (BagObject bagObject) {
        // we expect a future change might use a different approach to deserialization, so we
        // check to be sure this is the version we are working to
        return (WorkingType) deserializeWithType (bagObject, WITH_VERSION);
    }

    /**
     * Reconstitute the given BagObject representation back to the object it represents, using a
     * "best-effort" approach to matching the fields of the BagObject to the class being initialized.
     * @param bag The input data to reconstruct from, either a BagObject or BagArray
     * @param type the Class representing the type to reconstruct
     * @return the reconstituted object, or null if the reconstitution failed.
     */
    public static <WorkingType> WorkingType fromBagAsType (Bag bag, Class type) {
        return (bag != null) ? (WorkingType) deserialize (type.getName (), bag) : null;
    }
}
