
package com.dc.piglet.rtmp.core.io.f4v;


import io.netty.buffer.ByteBuf;

public class UnknownPayload implements Payload {

    private BoxType type;
    private ByteBuf data;

    public UnknownPayload(ByteBuf in, BoxType type) {
        this.data = in;
        this.type = type;
    }

    @Override
    public void read(ByteBuf in) {
        data = in;
    }

    @Override
    public ByteBuf write() {
        return data;
    }

    @Override
    public String toString() {
        return "[" + type + " (unknown) " + data + "]";
    }

}
