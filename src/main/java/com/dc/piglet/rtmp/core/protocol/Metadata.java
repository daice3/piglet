
package com.dc.piglet.rtmp.core.protocol;


import com.dc.piglet.rtmp.core.io.f4v.MovieInfo;
import com.dc.piglet.rtmp.core.io.f4v.TrackInfo;
import com.dc.piglet.rtmp.core.io.f4v.box.STSD;
import com.dc.piglet.rtmp.core.protocol.amf.Amf0Object;
import io.netty.buffer.ByteBuf;

import java.util.*;

public abstract class Metadata extends AbstractMessage {

    protected String name;
    protected Object[] data;

    public Metadata(String name, Object... data) {
        this.name = name;
        this.data = data;
        header.setMsgLength(encode().readableBytes());
    }

    public Metadata(RtmpHeader header, ByteBuf in) {
        super(header, in);
    }

    public Object getData(int index) {
        if(data == null || data.length < index + 1) {
            return null;
        }
        return data[index];
    }

    private Object getValue(String key) {
        final Map<String, Object> map = getMap(0);
        if(map == null) {
            return null;
        }
        return map.get(key);
    }

    public void setValue(String key, Object value) {
        if(data == null || data.length == 0) {
            data = new Object[]{new LinkedHashMap<String, Object>()};
        }
        if(data[0] == null) {
            data[0] = new LinkedHashMap<String, Object>();
        }
        final Map<String, Object> map = (Map) data[0];
        map.put(key, value);
    }

    public Map<String, Object> getMap(int index) {
        return (Map<String, Object>) getData(index);
    }

    public String getString(String key) {
        return (String) getValue(key);
    }

    public Boolean getBoolean(String key) {
        return (Boolean) getValue(key);
    }

    public Double getDouble(String key) {
        return (Double) getValue(key);
    }

    public double getDuration() {
        if(data == null || data.length == 0) {
            return -1;
        }
        final Map<String, Object> map = getMap(0);
        if(map == null) {
            return -1;
        }
        final Object o = map.get("duration");
        if(o == null) {
            return -1;
        }
        return ((Double) o).longValue();
    }

    public void setDuration(final double duration) {
        if(data == null || data.length == 0) {
            data = new Object[] {map(pair("duration", duration))};
        }
        final Object meta = data[0];
        final Map<String, Object> map = (Map) meta;
        if(map == null) {
            data[0] = map(pair("duration", duration));
            return;
        }
        map.put("duration", duration);
    }

    public static Metadata onPlayStatus(double duration, double bytes) {
        Map<String, Object> map = Command.onStatus(Command.OnStatus.STATUS,
                "NetStream.Play.Complete",
                pair("duration", duration),
                pair("bytes", bytes));
        return new MetadataAmf0("onPlayStatus", map);
    }

    public static Metadata rtmpSampleAccess() {
        return new MetadataAmf0("|RtmpSampleAccess", false, false);
    }

    public static Metadata dataStart() {
        return new MetadataAmf0("onStatus", object(pair("code", "NetStream.Data.Start")));
    }

    public static Metadata onMetaDataTest(MovieInfo movie) {
        Amf0Object track1 = object(
            pair("length", 3369366.0),
            pair("timescale", 30000.0),
            pair("language", "eng"),
            pair("sampledescription", new Amf0Object[]{object(pair("sampletype", "avc1"))})
        );
        Amf0Object track2 = object(
            pair("length", 2697216.0),
            pair("timescale", 24000.0),
            pair("language", "eng"),
            pair("sampledescription", new Amf0Object[]{object(pair("sampletype", "mp4a"))})
        );
        Map<String, Object> map = map(
            pair("duration", movie.getDuration()),
            pair("moovPosition", movie.getMoovPosition()),
            pair("width", 640.0),
            pair("height", 352.0),
            pair("videocodecid", "avc1"),
            pair("audiocodecid", "mp4a"),
            pair("avcprofile", 100.0),
            pair("avclevel", 30.0),
            pair("aacaot", 2.0),
            pair("videoframerate", 29.97002997002997),
            pair("audiosamplerate", 24000.0),
            pair("audiochannels", 2.0),
            pair("trackinfo", new Amf0Object[]{track1, track2})
        );
        return new MetadataAmf0("onMetaData", map);
    }

    public static Metadata onMetaData(MovieInfo movie) {
        Map<String, Object> map = map(
            pair("duration", movie.getDuration()),
            pair("moovPosition", movie.getMoovPosition())
        );
        TrackInfo track1 = movie.getVideoTrack();
        Amf0Object t1 = null;
        if(track1 != null) {
            String sampleType = track1.getStsd().getSampleTypeString(1);
            t1 = object(
                pair("length", track1.getMdhd().getDuration()),
                pair("timescale", track1.getMdhd().getTimeScale()),
                pair("sampledescription", new Amf0Object[]{object(pair("sampletype", sampleType))})
            );
            STSD.VideoSD video = movie.getVideoSampleDescription();
            map(map,
                pair("width", (double) video.getWidth()),
                pair("height", (double) video.getHeight()),
                pair("videocodecid", sampleType)
            );
        }
        TrackInfo track2 = movie.getAudioTrack();
        Amf0Object t2 = null;
        if(track2 != null) {
            String sampleType = track2.getStsd().getSampleTypeString(1);
            t2 = object(
                pair("length", track2.getMdhd().getDuration()),
                pair("timescale", track2.getMdhd().getTimeScale()),
                pair("sampledescription", new Amf0Object[]{object(pair("sampletype", sampleType))})
            );
            map(map,
                pair("audiocodecid", sampleType)
            );
        }
        List<Amf0Object> trackList = new ArrayList<Amf0Object>();
        if(t1 != null) {
            trackList.add(t1);
        }
        if(t2 != null) {
            trackList.add(t2);
        }
        map(map, pair("trackinfo", trackList.toArray()));
        return new MetadataAmf0("onMetaData", map);
    }

    //==========================================================================

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        sb.append("name: ").append(name);
        sb.append(" data: ").append(Arrays.toString(data));
        return sb.toString();
    }

}
