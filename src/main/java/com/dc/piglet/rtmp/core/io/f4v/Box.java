
package com.dc.piglet.rtmp.core.io.f4v;


import com.dc.piglet.rtmp.core.io.BufferReader;
import com.dc.piglet.rtmp.core.io.f4v.box.TKHD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
public class Box {
    private static final Logger log = LoggerFactory.getLogger(TKHD.class);
    private final BoxType type;
    private final long fileOffset;
    private List<Box> children;
    private Payload payload;

    public Box(final BufferReader in, final long endPos) {
        final long boxSize = in.readUnsignedInt();
        final byte[] typeBytes = in.readBytes(4);
        type = BoxType.parse(new String(typeBytes));        
        final long payloadSize;
        if (boxSize == 1) { // extended
            final byte[] extBytes = in.readBytes(8);
            final BigInteger bigLen = new BigInteger(1, extBytes);
            payloadSize = bigLen.longValue() - 16;
        } else if (boxSize == 0) { // decided by parent bound
            payloadSize = endPos - in.position();
        } else {
            payloadSize = boxSize - 8;
        }
        fileOffset = in.position();
        final long childEndPos = fileOffset + payloadSize;
        log.debug(">> type: {}, payloadSize: {}", type, payloadSize);
        final BoxType[] childBoxes = type.getChildren();
        if(childBoxes == null) {            
            if(type == BoxType.MDAT) {
                log.debug("skipping MDAT");
                in.position(childEndPos);
                return;
            }          
            payload = type.read(in.read((int) payloadSize));
            log.debug("<< {} payload: {}", type, payload);
            return;
        }        
        while(in.position() < childEndPos) {
            if(children == null) {
                children = new ArrayList<Box>();
            }
            children.add(new Box(in, childEndPos));
        }
        log.debug("<< {} children: {}", type, children);
    }

    public BoxType getType() {
        return type;
    }

    public long getFileOffset() {
        return fileOffset;
    }

    public List<Box> getChildren() {
        return children;
    }

    public Payload getPayload() {
        return payload;
    }

    public static void recurse(final Box box, final List<Box> collect, final int level) {
        if(collect != null && box.getPayload() != null) {
            collect.add(box);
        }
        if(box.getChildren() != null) {
            for(final Box child : box.getChildren()) {
                recurse(child, collect, level + 1);
            }
        }        
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append('[').append(type);
        sb.append(" fileOffset: ").append(fileOffset);
        if(children != null) {
            sb.append(" children: [");
            for(Box box : children) {
                sb.append(box.getType()).append(' ');
            }
            sb.append(']');
        }
        sb.append(" payload: ").append(payload);
        sb.append(']');
        return sb.toString();
    }

}
