
package com.dc.piglet.rtmp.core.io.f4v;

import com.dc.piglet.rtmp.core.io.f4v.box.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TrackInfo {
    private static final Logger log = LoggerFactory.getLogger(TrackInfo.class);
    private MovieInfo movie;

    private TKHD tkhd;
    private MDHD mdhd;
    private STSD stsd;
    private STTS stts;
    private CTTS ctts;
    private STSC stsc;
    private STSZ stsz;
    private STCO stco;
    private STSS stss;

    private List<Chunk> chunks;

    public TrackInfo(Box trak) {
        ArrayList<Box> collect = new ArrayList<Box>();
        Box.recurse(trak, collect, 0);
        for(Box box : collect) {
            log.debug("unpacking: {}", box);
            Payload pay = box.getPayload();
            switch(box.getType()) {
                case TKHD: tkhd = (TKHD) pay; break;
                case MDHD: mdhd = (MDHD) pay; break;
                case STSD: stsd = (STSD) pay; break;
                case STTS: stts = (STTS) pay; break;
                case CTTS: ctts = (CTTS) pay; break;
                case STSC: stsc = (STSC) pay; break;
                case STSZ: stsz = (STSZ) pay; break;
                case STCO: stco = (STCO) pay; break;
                case STSS: stss = (STSS) pay; break;
            }
        }
        initChunks();
        log.debug("initialized track info table");
    }

    public MDHD getMdhd() {
        return mdhd;
    }

    public STSD getStsd() {
        return stsd;
    }

    public MovieInfo getMovie() {
        return movie;
    }

    public void setMovie(MovieInfo movie) {
        this.movie = movie;
    }

    public List<Chunk> getChunks() {
        return chunks;
    }

    private void initChunks() {
        int stcoIndex = 0;
        int stszIndex = 0;
        chunks = new ArrayList<Chunk>();
        int stscCount = stsc.getRecords().size();
        Set<Integer> syncSampleNumbers = null;
        if (stss != null) {
            syncSampleNumbers = new HashSet<Integer>(stss.getSampleNumbers());
        }
        for (int i = 0; i < stscCount; i++) {
            STSC.STSCRecord stscRecord = stsc.getRecords().get(i);
            int lastChunkWithSameSize;
            if (i + 1 == stscCount) {
                if (i == 0) {
                    lastChunkWithSameSize = stco.getOffsets().size();
                } else {
                    lastChunkWithSameSize = stscRecord.getFirstChunk();
                }
            } else {
                lastChunkWithSameSize = stsc.getRecords().get(i + 1).getFirstChunk() - 1;
            }
            for (int j = stcoIndex; j < lastChunkWithSameSize; j++) {
                Chunk chunk = new Chunk();
                int sampleFileOffset = 0;
                chunk.setSampleDescIndex(stscRecord.getSampleDescIndex());
                chunk.setFileOffset(stco.getOffsets().get(stcoIndex++));
                for (int k = 0; k < stscRecord.getSamplesPerChunk(); k++) {
                    Sample sample = new Sample();
                    sample.setSize(stsz.getSampleSizes().get(stszIndex++));
                    sample.setFileOffset(chunk.getFileOffset() + sampleFileOffset);
                    sampleFileOffset += sample.getSize();
                    if (syncSampleNumbers != null && syncSampleNumbers.contains(stszIndex)) {
                        sample.setSyncSample(true);
                    }
                    chunk.add(sample);
                }
                chunk.setTrack(this);
                chunks.add(chunk);
            }
        }        
        int chunkIndex = 0;
        int sampleIndex = 0;
        long rawTime = 0;
        Chunk chunk = chunks.get(chunkIndex);
        scan:
        for (STTS.STTSRecord sttsRecord : stts.getRecords()) {
            for (int i = 0; i < sttsRecord.getSampleCount(); i++) {
                if (sampleIndex == chunk.getSampleCount()) {
                    chunkIndex++;
                    if (chunkIndex == chunks.size()) {
                        break scan;
                    }
                    chunk = chunks.get(chunkIndex);
                    sampleIndex = 0;
                }
                Sample sample = chunk.getSamples().get(sampleIndex++);
                final int rawDuration = sttsRecord.getSampleDuration();
                final int duration = sample.convertFromTimeScale(rawDuration);
                sample.setDuration(duration);
                final int time = sample.convertFromTimeScale(rawTime);
                sample.setTime(time);
                rawTime += rawDuration;
            }
        }
        if(ctts == null) {
            return;
        }
        chunkIndex = 0;
        sampleIndex = 0;
        chunk = chunks.get(chunkIndex);
        scan:
        for (CTTS.CTTSRecord cttsRecord : ctts.getRecords()) {
            for (int i = 0; i < cttsRecord.getSampleCount(); i++) {
                if (sampleIndex == chunk.getSampleCount()) {
                    chunkIndex++;
                    if (chunkIndex == chunks.size()) {
                        break scan;
                    }
                    chunk = chunks.get(chunkIndex);
                    sampleIndex = 0;
                }
                Sample sample = chunk.getSamples().get(sampleIndex++);
                final int compositionTimeOffset = sample.convertFromTimeScale(cttsRecord.getSampleOffset());
                sample.setCompositionTimeOffset(compositionTimeOffset);
            }
        }
    }

}
