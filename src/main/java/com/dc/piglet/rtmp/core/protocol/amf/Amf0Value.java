
package com.dc.piglet.rtmp.core.protocol.amf;

import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

public class Amf0Value {

    private static final Logger logger = LoggerFactory.getLogger(Amf0Value.class);

    private Amf0Value() {}

    public static enum Type{

        NUMBER(0x00),
        BOOLEAN(0x01),
        STRING(0x02),
        OBJECT(0x03),
        NULL(0x05),
        UNDEFINED(0x06),
        REFERENCE(0x07),
        MAP(0x08), // ecma-array
        OBJECT_END(0x09),
        ARRAY(0x0A), // strict-array
        DATE(0x0B),
        LONG_STRING(0x0C),
        UNSUPPORTED(0x0D),
        RECORDSET(0x0E),
        XML_DOCUMENT(0x0F),
        TYPED_OBJECT(0x10);

        private final int value;

        private Type(int value) {
            this.value = value;
        }

        public int intValue() {
            return value;
        }

        public static Type valueToEnum(final int value) {
            Type[] values = Type.values();
            for (Type type : values) {
                if (type.value == value){
                    return type;
                }
            }
            return null;
        }

        private static Type getType(final Object value) {
            if (value == null) {
                return NULL;
            } else if (value instanceof String) {
                return STRING;
            } else if (value instanceof Number) {
                return NUMBER;
            } else if (value instanceof Boolean) {
                return BOOLEAN;
            } else if (value instanceof Amf0Object) {
                Amf0Object obj = (Amf0Object) value;
                if (obj.containsKey("classname")) {
                    return TYPED_OBJECT;
                }
                else {
                    return OBJECT;
                }
            } else if (value instanceof Map) {
                return MAP;
            } else if (value instanceof Object[]) {
                return ARRAY;
            } else if(value instanceof Date) {
                return DATE;
            } else {
                throw new RuntimeException("unexpected type: " + value.getClass());
            }
        }

    }
    
    private static final byte BOOLEAN_TRUE = 0x01;
    private static final byte BOOLEAN_FALSE = 0x00;
    private static final byte[] OBJECT_END_MARKER = new byte[]{0x00, 0x00, 0x09};    

    public static void encode(final ByteBuf out, final Object value) {
        final Type type = Type.getType(value);
        if(logger.isDebugEnabled()) {
            logger.debug(">> " + toString(type, value));
        }
        out.writeByte((byte) type.value);
        switch (type) {
            case NUMBER:
                if(value instanceof Double) {
                    out.writeLong(Double.doubleToLongBits((Double) value));
                } else {
                    out.writeLong(Double.doubleToLongBits(Double.valueOf(value.toString())));
                }
                return;
            case BOOLEAN:                
                out.writeByte((Boolean) value ? BOOLEAN_TRUE : BOOLEAN_FALSE);
                return;
            case STRING:
                encodeString(out, (String) value);
                return;
            case NULL:
                return;
            case MAP:
                out.writeInt(0);
                // no break; remaining processing same as OBJECT
            case OBJECT:
                encodeObject(out, value);
                return;
            case ARRAY:
                final Object[] array = (Object[]) value;
                out.writeInt(array.length);
                for(Object o : array) {
                    encode(out, o);
                }
                return;
            case DATE:
                final long time = ((Date) value).getTime();
                out.writeLong(Double.doubleToLongBits(time));
                out.writeShort((short) 0);
                return;
            case TYPED_OBJECT:
                final Map<String, Object> map = (Map) value;
                encodeString(out, (String) map.remove("classname"));
                encodeObject(out, value);
                return;
            default:
                // ignoring other types client doesn't require for now
                throw new RuntimeException("unexpected type: " + type);
        }
    }

    private static String decodeString(final ByteBuf in) {
        final int size = in.readUnsignedShort();
        final byte[] bytes = new byte[size];
        in.readBytes(bytes);
        return new String(bytes); // TODO UTF-8 ?
    }

    private static void encodeString(final ByteBuf out, final String value) {
        final byte[] bytes = value.getBytes(); // TODO UTF-8 ?
        out.writeShort((short) bytes.length);
        out.writeBytes(bytes);
    }

    private static void encodeObject(final ByteBuf out, final Object value) {
        final Map<String, Object> map = (Map) value;
        for (final Map.Entry<String, Object> entry : map.entrySet()) {
            encodeString(out, entry.getKey());
            encode(out, entry.getValue());
        }
        out.writeBytes(OBJECT_END_MARKER);
    }

    public static void encode(final ByteBuf out, final Object... values) {
        for (final Object value : values) {
            encode(out, value);
        }
    }

    public static Object decode(final ByteBuf in) {
        final Type type = Type.valueToEnum(in.readByte());
        final Object value = decode(in, type);
        if(logger.isDebugEnabled()) {
            logger.debug("<< " + toString(type, value));
        }
        return value;
    }

    private static Object decode(final ByteBuf in, final Type type) {
    	String decodedString = "";
        switch (type) {
            case NUMBER: return Double.longBitsToDouble(in.readLong());
            case BOOLEAN: return in.readByte() == BOOLEAN_TRUE;
            case STRING: 
            	try {
            		decodedString = decodeString(in);
            	} catch(Exception e) {
                	logger.error("Exception while decoding a message from type {}: {}", type, e.toString());
                	in.clear();
                	decodedString = new String();
            	}
            	return decodedString;
            case ARRAY:
                final int arraySize = in.readInt();
                final Object[] array = new Object[arraySize];
                for (int i = 0; i < arraySize; i++) {
                    array[i] = decode(in);
                }
                return array;
            case MAP:               
            case OBJECT:
                final int count;
                final Map<String, Object> map;
                if(type == Type.MAP) {
                    count = in.readInt(); // should always be 0
                    map = new LinkedHashMap<String, Object>();
                    if(count > 0 && logger.isDebugEnabled()) {
                        logger.debug("non-zero size for MAP type: {}", count);
                    }
                } else {
                    count = 0;
                    map = new Amf0Object();
                }
                int i = 0;
                final byte[] endMarker = new byte[3];
                while (in.isReadable()) {
                    in.getBytes(in.readerIndex(), endMarker);
                    if (Arrays.equals(endMarker, OBJECT_END_MARKER)) {
                        in.skipBytes(3);
                        if(logger.isDebugEnabled()) {
                            logger.debug("end MAP / OBJECT, found object end marker [000009]");
                        }
                        break;
                    }
                    if(count > 0 && i++ == count) {
                        if(logger.isDebugEnabled()) {
                            logger.debug("stopping map decode after reaching count: {}", count);
                        }
                        break;
                    }
                	try {
                		decodedString = decodeString(in);
                	} catch(Exception e) {
                    	logger.error("Exception while decoding a message from type {}: {}", type, e.toString());
                    	decodedString = new String();
                	}
                    map.put(decodedString, decode(in));
                }
                return map;
            case DATE:
                final long dateValue = in.readLong();
                in.readShort(); // consume the timezone
                return new Date((long) Double.longBitsToDouble(dateValue));
            case LONG_STRING:
                final int stringSize = in.readInt();
                final byte[] bytes = new byte[stringSize];
                in.readBytes(bytes);
                return new String(bytes); // TODO UTF-8 ?
            case NULL:
            case UNDEFINED:
            case UNSUPPORTED:
                return null;
            case TYPED_OBJECT:
            	try {
            		decodedString = decodeString(in);
            	} catch(Exception e) {
                	logger.error("Exception while decoding a message from type {}: {}", type, e.toString());
                	decodedString = new String();
            	}
                String classname = decodedString;
                Amf0Object object = (Amf0Object) decode(in, Type.OBJECT);
                object.put("classname", classname);
                return object;
            default:
                throw new RuntimeException("unexpected type: " + type);
        }
    }
    
    private static String toString(final Type type, final Object value) {
        StringBuilder sb = new StringBuilder();
        sb.append('[').append(type).append(" ");
        if(type == Type.ARRAY) {
            sb.append(Arrays.toString((Object[]) value));
        } else {
            sb.append(value);
        }
        sb.append(']');
        return sb.toString();
    }
    
}
