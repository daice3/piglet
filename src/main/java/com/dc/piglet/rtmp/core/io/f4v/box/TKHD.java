
package com.dc.piglet.rtmp.core.io.f4v.box;


import com.dc.piglet.rtmp.core.io.f4v.Payload;
import com.dc.piglet.rtmp.util.Util;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TKHD implements Payload {
    private static final Logger log = LoggerFactory.getLogger(TKHD.class);
    private byte version;
    private byte[] flags;
    private long creationTime;
    private long modificationTime;
    private int trackId;
    private int reserved1;
    private long duration;
    private int[] reserved2; // 2
    private short layer;
    private short alternateGroup;
    private short volume;
    private short reserved3;
    private int[] transformMatrix; // 9
    private int width;
    private int height;

    public TKHD(ByteBuf in) {
        read(in);
    }

    public int getTrackId() {
        return trackId;
    }

    @Override
    public void read(ByteBuf in) {
        version = in.readByte();
        log.debug("version: {}", Util.toHex(version));
        flags = new byte[3];
        in.readBytes(flags);
        if (version == 0x00) {
            creationTime = in.readInt();
            modificationTime = in.readInt();
        } else {
            creationTime = in.readLong();
            modificationTime = in.readLong();
        }
        trackId = in.readInt();
        reserved1 = in.readInt();
        if (version == 0x00) {
            duration = in.readInt();
        } else {
            duration = in.readLong();
        }
        reserved2 = new int[2];
        reserved2[0] = in.readInt();
        reserved2[1] = in.readInt();
        layer = in.readShort();
        alternateGroup = in.readShort();
        volume = in.readShort();
        reserved3 = in.readShort();
        transformMatrix = new int[9];
        for (int i = 0; i < transformMatrix.length; i++) {
            transformMatrix[i] = in.readInt();
            log.debug("transform matrix[{}]: {}", i, transformMatrix[i]);
        }
        width = in.readInt();
        height = in.readInt();
        log.debug("width {} height {}",width, height);
    }

    @Override
    public ByteBuf write() {
        ByteBuf out = Unpooled.buffer(256);
        out.writeByte(version);
        out.writeBytes(new byte[3]); // flags
        if (version == 0x00) {
            out.writeInt((int) creationTime);
            out.writeInt((int) modificationTime);
        } else {
            out.writeLong(creationTime);
            out.writeLong(modificationTime);
        }
        out.writeInt(trackId);
        out.writeInt(reserved1);
        if (version == 0x00) {
            out.writeInt((int) duration);
        } else {
            out.writeLong(duration);
        }
        out.writeInt(reserved2[0]);
        out.writeInt(reserved2[1]);
        out.writeShort(layer);
        out.writeShort(alternateGroup);
        out.writeShort(volume);
        out.writeShort(reserved3);
        for (int i = 0; i < transformMatrix.length; i++) {
            out.writeInt(transformMatrix[i]);
        }
        out.writeInt(width);
        out.writeInt(height);
        return out;
    }
    
}
