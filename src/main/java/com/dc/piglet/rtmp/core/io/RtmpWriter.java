
package com.dc.piglet.rtmp.core.io;


import com.dc.piglet.rtmp.core.protocol.RtmpMessage;

public interface RtmpWriter {

    void write(RtmpMessage message);

    void close();

}
