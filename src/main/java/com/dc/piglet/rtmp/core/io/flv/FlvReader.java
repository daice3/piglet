
package com.dc.piglet.rtmp.core.io.flv;

import com.dc.piglet.rtmp.core.io.BufferReader;
import com.dc.piglet.rtmp.core.io.FileChannelReader;
import com.dc.piglet.rtmp.core.io.RtmpReader;
import com.dc.piglet.rtmp.core.protocol.*;
import com.dc.piglet.rtmp.entity.MessageType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlvReader implements RtmpReader {

    private static final Logger logger = LoggerFactory.getLogger(FlvReader.class);
    
    private final BufferReader in;
    private final long mediaStartPosition;
    private final Metadata metadata;
    private int aggregateDuration;

	private int width;
	private int height;    

    public FlvReader(final String path) {
        in = new FileChannelReader(path);
        in.position(13); // skip flv header
        
        final RtmpMessage metadataAtom = next();

        /* TODO: block added to ignore an exception caused probably due to a new message in flv/rtmp
                 that is not treated by flazr */
        RtmpMessage metadataTemp = null;
        try {
            metadataTemp = MessageType.decode(metadataAtom.getHeader(), metadataAtom.encode());
        } catch (Exception e) {
            if (e.getMessage().equals("bad value / byte: 101 (hex: 65), java.lang.ArrayIndexOutOfBoundsException: 101")) {
                logger.debug("Ignoring malformed metadata (bad value / byte: 101 (hex: 65))");
            }
        }

        if(metadataTemp != null && metadataTemp.getHeader().isMetadata()) {
            metadata = (Metadata) metadataTemp;
            mediaStartPosition = in.position();
        } else {
            logger.warn("flv file does not start with 'onMetaData', using empty one");
            metadata = new MetadataAmf0("onMetaData");
            in.position(13);
            mediaStartPosition = 13;
        }
        logger.debug("flv file metadata: {}", metadata);
        
        RtmpMessage firstFrame;
        do {
        	firstFrame = next();
        } while (!firstFrame.getHeader().isVideo() && hasNext());
        
        if (firstFrame != null) {
	        Video video = new Video(firstFrame.getHeader(), firstFrame.encode());
	        width = video.getWidth();
	        height = video.getHeight();
	        metadata.setValue("width", width);
	        metadata.setValue("height", height);
	        // rewind
	        seek(0);
        }
    }

    @Override
    public Metadata getMetadata() {
        return metadata;
    }

    @Override
    public RtmpMessage[] getStartMessages() {
        return new RtmpMessage[] { metadata };
    }

    @Override
    public void setAggregateDuration(int targetDuration) {
        this.aggregateDuration = targetDuration;
    }

    @Override
    public long getTimePosition() {
        final int time;
        if(hasNext()) {
            time = next().getHeader().getTimestamp();
            prev();
        } else if(hasPrev()) {
            time = prev().getHeader().getTimestamp();
            next();
        } else {
            throw new RuntimeException("not seekable");
        }
        return time;
    }

    private static boolean isSyncFrame(final RtmpMessage message) {
        final byte firstByte = message.encode().getByte(0);
        if((firstByte & 0xF0) == 0x10) {
            return true;
        }
        return false;
    }

    @Override
    public long seek(final long time) {
        logger.debug("trying to seek to: {}", time);
        if(time == 0) { // special case
            try {
                in.position(mediaStartPosition);
                return 0;
            } catch(Exception e) {
                throw new RuntimeException(e);
            }
        }
        final long start = getTimePosition();        
        if(time > start) {
            while(hasNext()) {
                final RtmpMessage cursor = next();
                if(cursor.getHeader().getTimestamp() >= time) {
                    break;
                }
            }
        } else {
            while(hasPrev()) {
                final RtmpMessage cursor = prev();
                if(cursor.getHeader().getTimestamp() <= time) {
                    next();
                    break;
                }
            }
        }
        // find the closest sync frame prior
        try {
            final long checkPoint = in.position();
            while(hasPrev()) {
                final RtmpMessage cursor = prev();
                if(cursor.getHeader().isVideo() && isSyncFrame(cursor)) {
                    logger.debug("returned seek frame / position: {}", cursor);
                    return cursor.getHeader().getTimestamp();
                }
            }
            // could not find a sync frame !
            // TODO better handling, what if file is audio only
            in.position(checkPoint);
            return getTimePosition();
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean hasNext() {        
        return in.position() < in.size();
    }


    protected boolean hasPrev() {        
        return in.position() > mediaStartPosition;
    }

    protected RtmpMessage prev() {        
        final long oldPos = in.position();
        in.position(oldPos - 4);
        final long newPos = oldPos - 4 - in.readInt();
        in.position(newPos);
        final FlvAtom flvAtom = new FlvAtom(in);
        in.position(newPos);
        return flvAtom;
    }

    private static final int AGGREGATE_SIZE_LIMIT = 65536;

    @Override
    public RtmpMessage next() {
        if(aggregateDuration <= 0) {
            return new FlvAtom(in);
        }
        final ByteBuf out = Unpooled.buffer(256);
        int firstAtomTime = -1;
        while(hasNext()) {
            final FlvAtom flvAtom = new FlvAtom(in);
            final int currentAtomTime = flvAtom.getHeader().getTimestamp();
            if(firstAtomTime == -1) {
                firstAtomTime = currentAtomTime;
            }
            final ByteBuf temp = flvAtom.write();
            if(out.readableBytes() + temp.readableBytes() > AGGREGATE_SIZE_LIMIT) {
                prev();
                break;
            }
            out.writeBytes(temp);
            if(currentAtomTime - firstAtomTime > aggregateDuration) {
                break;
            }
        }
        return new Aggregate(firstAtomTime, out);
    }

    @Override
    public void close() {
        in.close();
    }

    @Override
    public int getWidth() {
		return width;
	}

    @Override
	public int getHeight() {
		return height;
	}


}
