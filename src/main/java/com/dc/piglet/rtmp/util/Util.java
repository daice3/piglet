package com.dc.piglet.rtmp.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;

public class Util {
    private static final Logger log = LoggerFactory.getLogger(Util.class);
    private static final char[] HEX_DIGITS = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'A', 'B', 'C', 'D', 'E', 'F'};

    private static final char BYTE_SEPARATOR = ' ';


    public static byte[] intToByte(int i){
        byte[] result = new byte[4];
        result[0] = (byte)((i >> 24) & 0xFF);
        result[1] = (byte)((i >> 16) & 0xFF);
        result[2] = (byte)((i >> 8) & 0xFF);
        result[3] = (byte)(i & 0xFF);
        return result;
    }

    public static byte[] slice(int start,int end,byte[] source){
        byte[] dest = new byte[end - start];
        for (int i=start;i<end;i++){
            dest[i-start] = source[i];
        }
        return dest;
    }

    public static byte[] combine(byte[] b1,byte[]b2){
        byte[] dest = new byte[b1.length + b2.length];
        for(int i=0;i<dest.length;i++){
            byte t;
            if(i<b1.length){
                t = b1[i];
            }else{
                t = b2[i-b1.length];
            }
            dest[i] =  t;
        }
        return dest;
    }

    public static byte[] HmacSHA256(byte[] data, byte[] key) {
        final Mac mac;
        try {
            mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return mac.doFinal(data);
    }

    public static void main(String[] args) {
        ByteBuf buffer = Unpooled.buffer(5);
        byte[] b = {1,2,3,4,5,6};
        buffer.writeBytes(b); //initialCapacity 而已
        System.out.println(buffer.writableBytes());

        ByteBuf buf = Unpooled.buffer(0, 5);
        byte[] a = {1,2,3};
        buf.writeBytes(a);
        System.out.println(buf.writableBytes());
        System.out.println(buf.maxWritableBytes());
    }

    public static boolean compare(byte[] digest, byte[] tempHash) {
        if(digest.length != tempHash.length){
            return false;
        }
        for(int i=0;i<digest.length;i++){
            if (digest[i] != tempHash[i]){
                return false;
            }
        }
        return true;
    }

    public static void copy(byte[] dest,int start,int end,byte[] source){
        int count = 0;
        for (int i=start;i<end;i++){
            count ++;
            if(count > source.length) {
                break;
            }
            dest[i] = source[i-start];
        }
    }

    public static String trimSlashes(String raw) {
        if(raw == null) {
            return null;
        }
        return raw.replace("/", "").replace("\\", "");
    }

    public static String toHex(final byte b) {
        final char[] chars = toHexChars(b);
        return String.valueOf(chars);
    }

    private static char[] toHexChars(final int b) {
        final char left = HEX_DIGITS[(b >>> 4) & 0x0F];
        final char right = HEX_DIGITS[b & 0x0F];
        return new char[]{left, right};
    }

    public static int readInt32Reverse(final ByteBuf in) {
        final byte a = in.readByte();
        final byte b = in.readByte();
        final byte c = in.readByte();
        final byte d = in.readByte();
        int val = 0;
        val += d << 24;
        val += c << 16;
        val += b << 8;
        val += a;
        return val;
    }


    public static String toHex(final byte[] ba) {
        return toHex(ba, false);
    }

    public static String toHex(final byte[] ba, final boolean withSeparator) {
        return toHex(ba, 0, ba.length, withSeparator);
    }

    public static String toHex(final byte[] ba, final int offset, final int length, final boolean withSeparator) {
        final char[] buf;
        if (withSeparator) {
            buf = new char[length * 3];
        } else {
            buf = new char[length * 2];
        }
        for (int i = offset, j = 0; i < offset + length;) {
            final char[] chars = toHexChars(ba[i++]);
            buf[j++] = chars[0];
            buf[j++] = chars[1];
            if (withSeparator) {
                buf[j++] = BYTE_SEPARATOR;
            }
        }
        return new String(buf);
    }

    public static byte[] fromHex(final char[] hex) {
        final int length = hex.length / 2;
        final byte[] raw = new byte[length];
        for (int i = 0; i < length; i++) {
            final int high = Character.digit(hex[i * 2], 16);
            final int low = Character.digit(hex[i * 2 + 1], 16);
            int value = (high << 4) | low;
            if (value > 127) {
                value -= 256;
            }
            raw[i] = (byte) value;
        }
        return raw;
    }

    public static byte[] fromHex(final String s) {
        return fromHex(s.replace(" ", "").toCharArray());
    }

    public static byte[] toInt24(final int value) {
        return new byte[] {(byte)(value >>> 16), (byte)(value >>> 8), (byte)value};
    }


    public static byte[] readAsByteArray(File file) {
        return readAsByteArray(file, file.length());
    }

    public static byte[] readAsByteArray(File file, long length) {
        try {
            byte[] bytes = new byte[(int) length];
            int offset = 0;
            int numRead = 0;
            FileInputStream is = new FileInputStream(file);
            while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
                offset += numRead;
            }
            is.close();
            return bytes;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeInt32Reverse(final ByteBuf out, final int value) {
        out.writeByte((byte) (0xFF & value));
        out.writeByte((byte) (0xFF & (value >> 8)));
        out.writeByte((byte) (0xFF & (value >> 16)));
        out.writeByte((byte) (0xFF & (value >> 24)));
    }
}
