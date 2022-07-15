package com.dc.piglet.rtmp.core.protocol;


import com.dc.piglet.rtmp.entity.MessageType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * 丢弃chunk
 * @author daice
 */
public class Abort extends AbstractMessage {

    private int csId;

    public Abort(final int csId) {
        this.csId = csId;
    }

    public Abort(final RtmpHeader header, final ByteBuf in) {
        super(header, in);
    }

    public int getStreamId() {
        return csId;
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.ABORT;
    }

    @Override
    public ByteBuf encode() {
        final ByteBuf out = Unpooled.buffer(4);
        out.writeInt(csId);
        return out;
    }

    @Override
    public void decode(ByteBuf in) {
        csId = in.readInt();
    }

    @Override
    public String toString() {
        return super.toString() + "chunk stream id: " + csId;
    }

}
