package com.dc.piglet.rtmp.core.io;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.RandomAccessFile;

public class RandomAccessFileReader implements BufferReader {

    private static final Logger logger = LoggerFactory.getLogger(RandomAccessFileReader.class);

    private final String absolutePath;
    private final RandomAccessFile in;
    private final long fileSize;

    public RandomAccessFileReader(final String path) {
        this(new File(path));
    }

    public RandomAccessFileReader(final File file) {
        absolutePath = file.getAbsolutePath();        
        try {
            in = new RandomAccessFile(file, "r");
            fileSize = in.length();
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long size() {
        return fileSize;
    }

    @Override
    public long position() {
        try {
            return in.getFilePointer();
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void position(final long position) {
        try {
            in.seek(position);
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ByteBuf read(final int size) {
        return Unpooled.wrappedBuffer(readBytes(size));
    }

    @Override
    public byte[] readBytes(int size) {
        final byte[] bytes = new byte[size];
        try {
            in.readFully(bytes);
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
        return bytes;
    }

    @Override
    public int readInt() {
        return read(4).readInt();
    }

    @Override
    public long readUnsignedInt() {
        return read(4).readUnsignedInt();
    }

    @Override
    public void close() {
        try {
            in.close();
        } catch(Exception e) {
            logger.warn("error closing file {}: {}", absolutePath, e.getMessage());
        }
        logger.info("closed file: {}", absolutePath);
    }

}
