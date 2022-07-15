
package com.dc.piglet.rtmp.core.io;

import io.netty.buffer.ByteBuf;

public interface BufferReader {

    long size();

    long position();

    void position(long position);

    ByteBuf read(int size);

    byte[] readBytes(int size);

    int readInt();

    long readUnsignedInt();

    void close();

}
