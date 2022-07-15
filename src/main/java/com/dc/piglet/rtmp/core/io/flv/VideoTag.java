
package com.dc.piglet.rtmp.core.io.flv;

public class VideoTag {

    private final FrameType frameType;
    private final CodecType codecType;

    public VideoTag(final byte byteValue) {
        frameType = FrameType.valueToEnum(byteValue >> 4);
        codecType = CodecType.valueToEnum(0x0F & byteValue);
    }

    public boolean isKeyFrame() {
        return frameType == FrameType.KEY;
    }

    public FrameType getFrameType() {
        return frameType;
    }

    public CodecType getCodecType() {
        return codecType;
    }

    public static enum FrameType{
        
        KEY(1),
        INTER(2),
        DISPOSABLE_INTER(3),
        GENERATED_KEY(4),
        COMMAND(5);

        private final int value;

        FrameType(final int value) {
            this.value = value;
        }

        public int intValue() {
            return value;
        }

        public static FrameType valueToEnum(final int value) {
            FrameType[] values = FrameType.values();
            for (FrameType frameType : values) {
                if(frameType.value == value){
                    return frameType;
                }
            }
            return null;
        }

    }

    public static enum CodecType {
        
        JPEG(1),
        H263(2),
        SCREEN(3),
        ON2VP6(4),
        ON2VP6_ALPHA(5),
        SCREEN_V2(6),
        AVC(7);

        private final int value;

        CodecType(final int value) {
            this.value = value;
        }

        public int intValue() {
            return value;
        }

        public static CodecType valueToEnum(final int value) {
            CodecType[] values = CodecType.values();
            for (CodecType codecType : values) {
                if(codecType.value == value){
                    return codecType;
                }
            }
            return null;
        }

    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("[frameType: ").append(frameType);
        sb.append(", codecType: ").append(codecType);
        sb.append(']');
        return sb.toString();
    }

}
