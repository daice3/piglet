
package com.dc.piglet.rtmp.core.protocol;


import com.dc.piglet.rtmp.entity.MessageType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * 设置chunk的大小
 */
public class ChunkSize extends AbstractMessage {

    private int chunkSize;

    public ChunkSize(RtmpHeader header, ByteBuf in) {
        super(header, in);
    }

    public ChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.CHUNK_SIZE;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    @Override
    public ByteBuf encode() {
        ByteBuf out = Unpooled.buffer(4);
        out.writeInt(chunkSize);
        return out;
    }

    @Override
    public void decode(ByteBuf in) {
        chunkSize = in.readInt();
    }

    @Override
    public String toString() {
        return super.toString() + chunkSize;
    }

}
