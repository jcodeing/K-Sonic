//MIT License
//
//Copyright (c) 2016 Jcodeing <jcodeing@gmail.com>
//
//Permission is hereby granted, free of charge, to any person obtaining a copy
//of this software and associated documentation files (the "Software"), to deal
//in the Software without restriction, including without limitation the rights
//to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
//copies of the Software, and to permit persons to whom the Software is
//furnished to do so, subject to the following conditions:
//
//The above copyright notice and this permission notice shall be included in all
//copies or substantial portions of the Software.
//
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
//SOFTWARE.

package com.jcodeing.library_exo.sonic;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Handler;

import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecSelector;
import com.google.android.exoplayer.SampleSource;
import com.google.android.exoplayer.audio.AudioCapabilities;
import com.google.android.exoplayer.drm.DrmSessionManager;
import com.google.android.exoplayer.util.Util;

import java.nio.ByteBuffer;

public final class SonicMediaCodecAudioTrackRenderer extends MediaCodecAudioTrackRenderer {

    private Sonic sonic;
    private float speed;
    private float pitch;
    private float rate;
    private byte[] inBuffer;
    private byte[] outBuffer;
    private ByteBuffer bufferSonicOut;

    private int bufferIndex;


    public SonicMediaCodecAudioTrackRenderer(SampleSource source, MediaCodecSelector mediaCodecSelector, DrmSessionManager drmSessionManager, boolean playClearSamplesWithoutKeys, Handler eventHandler, EventListener eventListener, AudioCapabilities audioCapabilities, int streamType) {
        super(source, mediaCodecSelector, drmSessionManager, playClearSamplesWithoutKeys, eventHandler, eventListener, audioCapabilities, streamType);
        //init
        bufferIndex = -1;
        speed = 1.0f;
        pitch = 1.0f;
        rate = 1.0f;
    }

    // ------------------------------K------------------------------@Override

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected final void onOutputFormatChanged(MediaCodec codec, MediaFormat outputFormat) {
        super.onOutputFormatChanged(codec, outputFormat);
        int sampleRate = outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        int channelCount = outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);

        int bufferSize = channelCount * 4096;//1024*4 4M 22.05 kHz/4096
        inBuffer = new byte[bufferSize];
        outBuffer = new byte[bufferSize];

        sonic = new Sonic(sampleRate, channelCount);
        bufferSonicOut = ByteBuffer.wrap(outBuffer, 0, 0);

        setSonicSpeed(speed);
        setSonicPitch(pitch);
        setSonicRate(rate);
    }


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected boolean processOutputBuffer(long positionUs, long elapsedRealtimeUs, MediaCodec codec, ByteBuffer buffer, MediaCodec.BufferInfo bufferInfo, int bufferIndex, boolean shouldSkip) throws ExoPlaybackException {
        if (bufferIndex == this.bufferIndex) {//bufferIndex: 0 ~ 14 / 0 ~ 3
            return super.processOutputBuffer(positionUs, elapsedRealtimeUs, codec, bufferSonicOut, bufferInfo, bufferIndex, shouldSkip);
        } else {
            int sizeSonic;
            this.bufferIndex = bufferIndex;
            if (Util.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {//<21
                buffer.position(0);
                sizeSonic = bufferInfo.size;
            } else {
                sizeSonic = buffer.remaining();
            }
            buffer.get(inBuffer, 0, sizeSonic);
            sonic.writeBytesToStream(inBuffer, sizeSonic);
            sizeSonic = sonic.readBytesFromStream(outBuffer, outBuffer.length);
            bufferInfo.offset = 0;
            bufferSonicOut.position(0);
            bufferInfo.size = sizeSonic;
            bufferSonicOut.limit(sizeSonic);
            return super.processOutputBuffer(positionUs, elapsedRealtimeUs, codec, bufferSonicOut, bufferInfo, bufferIndex, shouldSkip);
        }
    }


    // ------------------------------K------------------------------@Speed

    public final void setSonicSpeed(float speed) {
        synchronized (this) {
            try {
                this.speed = speed;
                if (sonic != null) {
                    sonic.setSpeed(speed);
                }
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
    }

    public float getSonicSpeed() {
        return sonic.getSpeed();
    }

    public final void setSonicPitch(float pitch) {
        synchronized (this) {
            try {
                this.pitch = pitch;
                if (sonic != null) {
                    sonic.setPitch(pitch);
                }
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
    }

    public float getSonicPitch() {
        return sonic.getPitch();
    }


    public final void setSonicRate(float rate) {
        synchronized (this) {
            try {
                this.rate = rate;
                if (sonic != null) {
                    sonic.setRate(rate);
                }
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
    }

    public float getSonicRate() {
        return sonic.getRate();
    }
}

