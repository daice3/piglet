
package com.dc.piglet.rtmp.core.io.f4v;

import com.dc.piglet.rtmp.core.io.BufferReader;
import com.dc.piglet.rtmp.core.io.f4v.box.FTYP;
import com.dc.piglet.rtmp.core.io.f4v.box.MVHD;
import com.dc.piglet.rtmp.core.io.f4v.box.STSD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
public class MovieInfo {
    private static final Logger log = LoggerFactory.getLogger(MovieInfo.class);
    private long moovPosition;
    private FTYP ftyp;
    private MVHD mvhd;
    private List<TrackInfo> tracks = new ArrayList<TrackInfo>();
    private List<Sample> samples;

    public List<Sample> getSamples() {
        return samples;
    }

    public long getMoovPosition() {
        return moovPosition;
    }

    public double getDuration() {
        return mvhd.getDuration() / (double) mvhd.getTimeScale();
    }

    private void initSamples() {
        samples = new ArrayList<Sample>();
        for(TrackInfo track : tracks) {
            for(Chunk chunk : track.getChunks()) {
                samples.addAll(chunk.getSamples());
            }
        }
        Collections.sort(samples); // sort by time, implements comparable
    }

    public MovieInfo(final BufferReader in) {
        while(in.position() < in.size()) {            
            Box box = new Box(in, in.size());
            if(box.getType() == BoxType.FTYP) {
                ftyp = (FTYP) box.getPayload();
                log.debug("unpacked: {}", ftyp);
            }
            if(box.getType() == BoxType.MOOV) {
                moovPosition = box.getFileOffset();
                log.debug("moov position: {}", moovPosition);
                for(Box moov : box.getChildren()) {
                    if(moov.getType() == BoxType.MVHD) {
                        mvhd = (MVHD) moov.getPayload();
                        log.debug("unpacked: {}", mvhd);
                    }
                    if(moov.getType() == BoxType.TRAK) {
                        TrackInfo track = new TrackInfo(moov);
                        track.setMovie(this);
                        tracks.add(track);
                        log.debug("unpacked: {}", track);
                    }
                }
            }
        }
        initSamples();
        log.debug("initialized movie info table");
    }

    public List<TrackInfo> getTracks() {
        return tracks;
    }

    public TrackInfo getVideoTrack() {
        for(TrackInfo track : tracks) {
            if(track.getStsd().getSampleType(1).isVideo()) {
                return track;
            }
        }
        return null;
    }

    public byte[] getVideoDecoderConfig() {
        return getVideoSampleDescription().getConfigBytes();
    }

    public STSD.VideoSD getVideoSampleDescription() {
        TrackInfo track = getVideoTrack();
        if(track == null) {
            return null;
        }
        return (STSD.VideoSD) track.getStsd().getSampleDescription(1);
    }

    public TrackInfo getAudioTrack() {
        for(TrackInfo track : tracks) {
            if(!track.getStsd().getSampleType(1).isVideo()) {
                return track;
            }
        }
        return null;
    }

    public byte[] getAudioDecoderConfig() {
        STSD.AudioSD audioSD = getAudioSampleDescription();
        if(audioSD != null){
            return audioSD.getConfigBytes();
        }
        return null;
    }

    public STSD.AudioSD getAudioSampleDescription() {
        TrackInfo track = getAudioTrack();
        if(track == null) {
            return null;
        }
        return (STSD.AudioSD) track.getStsd().getSampleDescription(1);
    }

}
