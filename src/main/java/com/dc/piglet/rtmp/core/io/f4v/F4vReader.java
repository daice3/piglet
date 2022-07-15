
package com.dc.piglet.rtmp.core.io.f4v;


import com.dc.piglet.rtmp.core.io.BufferReader;
import com.dc.piglet.rtmp.core.io.FileChannelReader;
import com.dc.piglet.rtmp.core.io.RtmpReader;
import com.dc.piglet.rtmp.core.io.flv.FlvAtom;
import com.dc.piglet.rtmp.core.protocol.*;
import com.dc.piglet.rtmp.util.Util;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
public class F4vReader implements RtmpReader {

    private static final Logger log = LoggerFactory.getLogger(F4vReader.class);
    private static final byte[] MP4A_BEGIN_PREFIX = Util.fromHex("af00");
    private static final byte[] MP4A_PREFIX = Util.fromHex("af01");
    private static final byte[] AVC1_BEGIN_PREFIX = Util.fromHex("1700000000");
    private static final byte[] AVC1_PREFIX_KEYFRAME = Util.fromHex("1701");
    private static final byte[] AVC1_PREFIX = Util.fromHex("2701");

    private byte[] AVC1_BEGIN;
    private byte[] MP4A_BEGIN;

    private final BufferReader in;
    private final List<Sample> samples;
    private final Metadata metadata;

    private int cursor;
    private int aggregateDuration;

    public F4vReader(final String path) {
        in = new FileChannelReader(path);
        final MovieInfo movie = new MovieInfo(in);
        in.position(0);
        AVC1_BEGIN = movie.getVideoDecoderConfig();
        MP4A_BEGIN = movie.getAudioDecoderConfig();
        log.debug("video decoder config inited: {}", Util.toHex(AVC1_BEGIN));
        metadata = Metadata.onMetaData(movie);
        samples = movie.getSamples();
        cursor = 0;
    }

    @Override
    public Metadata getMetadata() {
        return metadata;
    }

    @Override
    public RtmpMessage[] getStartMessages() {
        return new RtmpMessage[] {
            getMetadata(),
            new Video(AVC1_BEGIN_PREFIX, AVC1_BEGIN),
            new Audio(MP4A_BEGIN_PREFIX, MP4A_BEGIN)
        };
    }

    @Override
    public void setAggregateDuration(int targetDuration) {
        this.aggregateDuration = targetDuration;
    }

    @Override
    public long getTimePosition() {
        final int index;
        if(cursor == samples.size()) {
            index = cursor - 1;
        } else {
            index = cursor;
        }
        return samples.get(index).getTime();
    }

    @Override
    public long seek(long timePosition) {
        cursor = 0;
        while(cursor < samples.size()) {
            final Sample sample = samples.get(cursor);
            if(sample.getTime() >= timePosition) {
                break;
            }
            cursor++;
        }
        while(!samples.get(cursor).isSyncSample() && cursor > 0) {
            cursor--;
        }
        return samples.get(cursor).getTime();
    }

    @Override
    public boolean hasNext() {
        return cursor < samples.size();
    }

    private static final int AGGREGATE_SIZE_LIMIT = 65536;

    @Override
    public RtmpMessage next() {
        if(aggregateDuration <= 0) {
            return getMessage(samples.get(cursor++));
        }
        final ByteBuf out = Unpooled.buffer(256);
        int startSampleTime = -1;
        while(cursor < samples.size()) {
            final Sample sample = samples.get(cursor++);
            if(startSampleTime == -1) {
                startSampleTime = sample.getTime();
            }
            final RtmpMessage message = getMessage(sample);
            final RtmpHeader header = message.getHeader();
            final FlvAtom flvAtom = new FlvAtom(header.getMsgType(), header.getTimestamp(), message.encode());
            final ByteBuf temp = flvAtom.write();
            if(out.readableBytes() + temp.readableBytes() > AGGREGATE_SIZE_LIMIT) {
                cursor--;
                break;
            }
            out.writeBytes(temp);
            if(sample.getTime() - startSampleTime > aggregateDuration) {
                break;
            }
        }
        return new Aggregate(startSampleTime, out);
    }

    private RtmpMessage getMessage(final Sample sample) {
        in.position(sample.getFileOffset());
        final byte[] sampleBytes = in.readBytes(sample.getSize());        
        final byte[] prefix;        
        if(sample.isVideo()) {
            if(sample.isSyncSample()) {
                prefix = AVC1_PREFIX_KEYFRAME;
            } else {
                prefix = AVC1_PREFIX;
            }
            // TODO move prefix logic to Audio / Video
            return new Video(sample.getTime(), prefix, sample.getCompositionTimeOffset(), sampleBytes);
        } else {
            prefix = MP4A_PREFIX;
            return new Audio(sample.getTime(), prefix, sampleBytes);
        }
    }

    @Override
    public void close() {
        in.close();
    }   

    public static void main(String[] args) {
        F4vReader reader = new F4vReader("test2.5.mp4");
        while(reader.hasNext()) {
            log.debug("read: {}", reader.next());
        }
    }

	@Override
	public int getWidth() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getHeight() {
		// TODO Auto-generated method stub
		return 0;
	}

}
