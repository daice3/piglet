
package com.dc.piglet.rtmp.core.io.f4v.box;


import com.dc.piglet.rtmp.core.io.f4v.Payload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.ArrayList;
import java.util.List;
public class CTTS implements Payload {

    private static final Logger log = LoggerFactory.getLogger(CTTS.class);

    public static class CTTSRecord {

        private int sampleCount;
        private int sampleOffset;

        public int getSampleCount() {
            return sampleCount;
        }

        public int getSampleOffset() {
            return sampleOffset;
        }
        
    }

    private List<CTTSRecord> records;

    public List<CTTSRecord> getRecords() {
        return records;
    }

    public void setRecords(List<CTTSRecord> records) {
        this.records = records;
    }

    public CTTS(ByteBuf in) {
        read(in);
    }

    @Override
    public void read(ByteBuf in) {
        in.readInt(); // UI8 version + UI24 flags
        final int count = in.readInt();
        log.debug("no of composition time to sample records: {}", count);
        records = new ArrayList<CTTSRecord>(count);
        for (int i = 0; i < count; i++) {
            CTTSRecord record = new CTTSRecord();
            record.sampleCount = in.readInt();
            record.sampleOffset = in.readInt();
            records.add(record);
        }
    }

    @Override
    public ByteBuf write() {
        ByteBuf out = Unpooled.buffer(256);
        out.writeInt(0); // UI8 version + UI24 flags
        final int count = records.size();
        out.writeInt(count);
        for (int i = 0; i < count; i++) {
            final CTTSRecord record = records.get(i);
            out.writeInt(record.sampleCount);
            out.writeInt(record.sampleOffset);
        }
        return out;
    }

}
