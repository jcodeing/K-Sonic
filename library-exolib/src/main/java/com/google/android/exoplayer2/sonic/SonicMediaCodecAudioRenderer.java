/*
 * MIT License
 *
 * Copyright (c) 2017 K Sun <jcodeing@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.google.android.exoplayer2.sonic;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Handler;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.audio.AudioCapabilities;
import com.google.android.exoplayer2.audio.AudioProcessor;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;

import java.nio.ByteBuffer;

public final class SonicMediaCodecAudioRenderer extends MediaCodecAudioRenderer {

  // =========@Sonic
  private Sonic sonic;
  private float speed;
  private float pitch;
  private float rate;

  // =========@Buffer
  private byte[] sonicBuffer;
  private int bufferIndex;

  public SonicMediaCodecAudioRenderer(MediaCodecSelector mediaCodecSelector,
      DrmSessionManager<FrameworkMediaCrypto> drmSessionManager,
      boolean playClearSamplesWithoutKeys, Handler eventHandler,
      AudioRendererEventListener eventListener, AudioCapabilities audioCapabilities,
      AudioProcessor... audioProcessors) {
    super(mediaCodecSelector, drmSessionManager, playClearSamplesWithoutKeys, eventHandler, eventListener, audioCapabilities, audioProcessors);
    //Init
    bufferIndex = -1;
    speed = 1.0f;
    pitch = 1.0f;
    rate = 1.0f;
  }

  // ------------------------------K------------------------------@Override
  private MediaFormat outputFormat;

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
  @Override
  protected final void onOutputFormatChanged(MediaCodec codec, MediaFormat outputFormat) throws ExoPlaybackException {
    super.onOutputFormatChanged(codec, outputFormat);
    this.outputFormat = outputFormat;
    if (sonic != null)
      sonic.allocateStreamBuffers(outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE), outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT));
  }

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
  @Override
  protected boolean processOutputBuffer(long positionUs, long elapsedRealtimeUs, MediaCodec codec, ByteBuffer buffer, int bufferIndex, int bufferFlags, long bufferPresentationTimeUs, boolean shouldSkip) throws ExoPlaybackException {
    if (bufferIndex != this.bufferIndex) {
      this.bufferIndex = bufferIndex;
      if ((speed != 1.0f || pitch != 1.0f || rate != 1.0f) && initSonic(buffer.remaining())) {
        // =========@Sonic@=========
        int sonicProcessingSize;
        int position = buffer.position();

        // =========@Get the data and processing
        sonicProcessingSize = buffer.remaining();
        buffer.get(sonicBuffer, 0, sonicProcessingSize);
        sonic.writeBytesToStream(sonicBuffer, sonicProcessingSize);
        sonicProcessingSize = sonic.readBytesFromStream(sonicBuffer, sonicBuffer.length);

        // =========@Put the sonic processing data
        buffer.position(position);
        buffer.limit(position + sonicProcessingSize);
        buffer.put(sonicBuffer, 0, sonicProcessingSize);
        buffer.position(position);
      }
    }
    return super.processOutputBuffer(positionUs, elapsedRealtimeUs, codec, buffer, bufferIndex, bufferFlags, bufferPresentationTimeUs, shouldSkip);
  }

  // ------------------------------K------------------------------@Sonic

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
  private boolean initSonic(int bufferSize) {
    //Sonic
    if (sonic == null && outputFormat != null) {
      sonic = new Sonic(outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE), outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT));
      sonic.setSpeed(speed);
      sonic.setPitch(pitch);
      sonic.setRate(rate);
    }
    //Buffer
    if (sonic != null) {
      if (sonicBuffer == null) {
        if (4096 >= bufferSize)
          sonicBuffer = new byte[4096];
        else
          sonicBuffer = new byte[bufferSize];
      } else if (sonicBuffer.length < bufferSize) {
        sonicBuffer = new byte[bufferSize];
      }
    }
    return sonic != null && sonic.isDo && sonicBuffer != null && sonicBuffer.length >= bufferSize;
  }

  public void setSonicSpeed(float speed) {
    this.speed = speed;
    if (sonic != null)
      sonic.setSpeed(speed);
  }

  public float getSonicSpeed() {
    return sonic.getSpeed();
  }

  public void setSonicPitch(float pitch) {
    this.pitch = pitch;
    if (sonic != null)
      sonic.setPitch(pitch);
  }

  public float getSonicPitch() {
    return sonic.getPitch();
  }

  public void setSonicRate(float rate) {
    this.rate = rate;
    if (sonic != null)
      sonic.setRate(rate);
  }

  public float getSonicRate() {
    return sonic.getRate();
  }
}

