
package com.dc.piglet.rtmp.core.io.f4v;

public enum SampleType {

    AVC1(true),
    MP4A(false);
    
    private final boolean video;

    SampleType(boolean video) {
        this.video = video;
    }

    public boolean isVideo() {
        return video;
    }

    public static SampleType parse(String type) {
        return SampleType.valueOf(type.toUpperCase());
    }

}
