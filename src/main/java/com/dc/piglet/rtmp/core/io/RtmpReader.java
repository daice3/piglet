
package com.dc.piglet.rtmp.core.io;


import com.dc.piglet.rtmp.core.protocol.Metadata;
import com.dc.piglet.rtmp.core.protocol.RtmpMessage;

public interface RtmpReader {

    Metadata getMetadata();

    RtmpMessage[] getStartMessages();

    void setAggregateDuration(int targetDuration);

    long getTimePosition();

    long seek(long timePosition);

    void close();

    boolean hasNext();

    RtmpMessage next();
    
    int getWidth();
    
    int getHeight();

}
