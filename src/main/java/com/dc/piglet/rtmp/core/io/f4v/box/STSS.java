
package com.dc.piglet.rtmp.core.io.f4v.box;


import com.dc.piglet.rtmp.core.io.f4v.Payload;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
public class STSS implements Payload {

    private static final Logger log = LoggerFactory.getLogger(STSS.class);

    private List<Integer> sampleNumbers;
    
    public STSS(ByteBuf in) {
        read(in);
    }

    public List<Integer> getSampleNumbers() {
        return sampleNumbers;
    }

    public void setSampleNumbers(List<Integer> sampleNumbers) {
        this.sampleNumbers = sampleNumbers;
    }

    @Override
    public void read(ByteBuf in) {
        in.readInt(); // UI8 version + UI24 flags
        final int count = in.readInt();
        log.debug("no of sample sync records: {}", count);
        sampleNumbers = new ArrayList<Integer>(count);
        for (int i = 0; i < count; i++) {
            final Integer sampleNumber = in.readInt();
            sampleNumbers.add(sampleNumber);
        }
    }

    @Override
    public ByteBuf write() {
        ByteBuf out = Unpooled.buffer(256);
        out.writeInt(0); // UI8 version + UI24 flags
        out.writeInt(sampleNumbers.size());
        for (Integer sampleNumber : sampleNumbers) {
            out.writeInt(sampleNumber);
        }
        return out;
    }
    
}
