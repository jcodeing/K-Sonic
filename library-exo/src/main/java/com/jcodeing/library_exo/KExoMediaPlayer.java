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
package com.jcodeing.library_exo;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.text.TextUtils;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.io.FileDescriptor;
import java.util.Map;

public class KExoMediaPlayer extends AbstractMediaPlayer {

    private Context context;
    private SimpleExoPlayer player;

    private EventLogger eventLogger = new EventLogger();
    private PlayerListener playerListener = new PlayerListener();

    private String userAgent;
    private Handler mainHandler;
    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();

    public KExoMediaPlayer(Context context) {
        this.context = context.getApplicationContext();

        // =========@Init@=========
        TrackSelection.Factory trackSelectionFactory =
                new AdaptiveTrackSelection.Factory(BANDWIDTH_METER);
        DefaultTrackSelector trackSelector = new DefaultTrackSelector(trackSelectionFactory);

        player = ExoPlayerFactory.newSimpleInstance(this.context, trackSelector, new DefaultLoadControl(),
                null);
        player.addListener(eventLogger);
        player.addListener(playerListener);
        player.setVideoListener(playerListener);
        player.setPlayWhenReady(false);

        mainHandler = new Handler();
        userAgent = Util.getUserAgent(this.context, "KExoMediaPlayer");
        mediaDataSourceFactory = new DefaultDataSourceFactory(this.context, userAgent, BANDWIDTH_METER);
    }

    // ============================@Source@============================
    private String mDataSource;
    private MediaSource mediaSource;
    private DefaultDataSourceFactory mediaDataSourceFactory;

    @Override
    public void setDataSource(Context context, Uri uri) {
        mDataSource = uri.toString();
        mediaSource = buildMediaSource(uri, "");
    }

    @Override
    public void setDataSource(String path) {
        setDataSource(context, Uri.parse(path));
    }

    @Override
    public void setDataSource(Context context, Uri uri, Map<String, String> headers) {
        setDataSource(context, uri);
    }

    @Override
    public void setDataSource(FileDescriptor fd) {
        throw new UnsupportedOperationException("no support");
    }

    @Override
    public String getDataSource() {
        return mDataSource;
    }

    private MediaSource buildMediaSource(Uri uri, String overrideExtension) {
        int type = Util.inferContentType(!TextUtils.isEmpty(overrideExtension) ? "." + overrideExtension
                : uri.getLastPathSegment());
        switch (type) {
            case C.TYPE_SS:
                return new SsMediaSource(uri, new DefaultDataSourceFactory(context, userAgent),
                        new DefaultSsChunkSource.Factory(mediaDataSourceFactory), mainHandler, eventLogger);
            case C.TYPE_DASH:
                return new DashMediaSource(uri, new DefaultDataSourceFactory(context, userAgent),
                        new DefaultDashChunkSource.Factory(mediaDataSourceFactory), mainHandler, eventLogger);
            case C.TYPE_HLS:
                return new HlsMediaSource(uri, mediaDataSourceFactory, mainHandler, eventLogger);
            case C.TYPE_OTHER:
                return new ExtractorMediaSource(uri, mediaDataSourceFactory, new DefaultExtractorsFactory(),
                        mainHandler, eventLogger);
            default: {
                throw new IllegalStateException("Unsupported type: " + type);
            }
        }
    }

    @Override
    public void prepareAsync() throws IllegalStateException {
        if (player != null && mediaSource != null) {
            player.prepare(mediaSource);
            playerListener.isPreparing = true;
        }
    }


    // ============================@Control@============================
    @Override
    public void start() throws IllegalStateException {
        if (player != null) {
            if (player.getPlaybackState() == ExoPlayer.STATE_ENDED)
                player.seekTo(0);
            else
                player.setPlayWhenReady(true);
        }
    }

    @Override
    public void pause() throws IllegalStateException {
        if (player != null)
            player.setPlayWhenReady(false);
    }

    @Override
    public void stop() throws IllegalStateException {
        if (player != null)
            player.stop();
    }

    @Override
    public boolean isPlaying() {
        if (player == null)
            return false;
        int state = player.getPlaybackState();
        switch (state) {
            case ExoPlayer.STATE_BUFFERING:
            case ExoPlayer.STATE_READY:
                return player.getPlayWhenReady();
            case ExoPlayer.STATE_IDLE:
            case ExoPlayer.STATE_ENDED:
            default:
                return false;
        }
    }

    @Override
    public void seekTo(long msec) throws IllegalStateException {
        if (player != null) {
            player.seekTo(msec);
            playerListener.isSeekToing = true;
        }
    }

    @Override
    public long getCurrentPosition() {
        if (player != null)
            return player.getCurrentPosition();
        return 0;
    }

    @Override
    public long getDuration() {
        if (player != null)
            return player.getDuration();
        return 0;
    }

    @Override
    public void reset() {
        if (player != null)
            player.stop();
    }

    @Override
    public void release() {
        if (player != null) {
            reset();
            player.release();
            player.removeListener(playerListener);
            player.removeListener(eventLogger);
            player = null;
            eventLogger = null;
            playerListener = null;
        }
    }

    @Override
    public void setVolume(float leftVolume, float rightVolume) {
        player.setVolume(leftVolume);
    }

    @Override
    public void setWakeMode(Context context, int mode) {
        // FIXME: implement
    }

    @Override
    public void setScreenOnWhilePlaying(boolean screenOn) {
        // TODO: do nothing
    }

    private int mVideoWidth;
    private int mVideoHeight;

    @Override
    public int getVideoWidth() {
        return mVideoWidth;
    }

    @Override
    public int getVideoHeight() {
        return mVideoHeight;
    }

    @Override
    public int getVideoSarNum() {
        return 1;
    }

    @Override
    public int getVideoSarDen() {
        return 1;
    }


    @Override
    public void setDisplay(SurfaceHolder sh) {
        if (sh == null)
            setSurface(null);
        else
            setSurface(sh.getSurface());
    }

    @Override
    public void setSurface(Surface surface) {
        // do nothing
    }

    boolean looping;

    @Override
    public void setLooping(boolean looping) {
        this.looping = looping;
    }

    @Override
    public boolean isLooping() {
        return looping;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

    @Override
    public void setLogEnabled(boolean enable) {
        // do nothing
    }

    @Override
    public boolean isPlayable() {
        return true;
    }

    @Override
    public void setAudioStreamType(int streamtype) {
        // do nothing
    }

    @Override
    public void setKeepInBackground(boolean keepInBackground) {
        // do nothing
    }

    public int getBufferedPercentage() {
        if (player == null)
            return 0;
        return player.getBufferedPercentage();
    }


    // ============================@Listener@============================
    private class PlayerListener implements ExoPlayer.EventListener, SimpleExoPlayer.VideoListener {

        private boolean isPreparing = false;
        private boolean isSeekToing = false;
        private boolean isBuffering = false;

        @Override
        public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees,
                                       float pixelWidthHeightRatio) {
            mVideoWidth = width;
            mVideoHeight = height;
            notifyOnVideoSizeChanged(width, height, 1, 1);
            if (unappliedRotationDegrees > 0)
                notifyOnInfo(IMediaPlayer.MEDIA_INFO_VIDEO_ROTATION_CHANGED, unappliedRotationDegrees);
        }

        @Override
        public void onRenderedFirstFrame() {

        }

        @Override
        public void onTimelineChanged(Timeline timeline, Object manifest) {

        }

        @Override
        public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

        }

        @Override
        public void onLoadingChanged(boolean isLoading) {

        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {

            if (isBuffering && (playbackState == ExoPlayer.STATE_READY || playbackState == ExoPlayer.STATE_ENDED)) {
                notifyOnInfo(IMediaPlayer.MEDIA_INFO_BUFFERING_END, player.getBufferedPercentage());
                isBuffering = false;
            }

            if (isPreparing && playbackState == ExoPlayer.STATE_READY) {
                notifyOnPrepared();
                isPreparing = false;
            }

            if (isSeekToing && playbackState == ExoPlayer.STATE_READY) {
                notifyOnSeekComplete();
                isSeekToing = false;
            }

            switch (playbackState) {
                case ExoPlayer.STATE_IDLE:
                    break;
                case ExoPlayer.STATE_BUFFERING:
                    notifyOnInfo(IMediaPlayer.MEDIA_INFO_BUFFERING_START, player.getBufferedPercentage());
                    isBuffering = true;
                    break;
                case ExoPlayer.STATE_READY:
                    break;
                case ExoPlayer.STATE_ENDED:
                    if (isLooping())
                        start();
                    else
                        notifyOnCompletion();
                    break;
            }

        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {
            notifyOnError(IMediaPlayer.MEDIA_ERROR_UNKNOWN, IMediaPlayer.MEDIA_ERROR_UNKNOWN);
        }

        @Override
        public void onPositionDiscontinuity() {

        }
    }


    // ------------------------------K------------------------------@Sonic
    @Override
    public void setSonicSpeed(float speed) {
        if (player != null)
            player.setPlaybackSpeed(speed);
    }

    @Override
    public float getSonicSpeed() {
        if (player != null)
            return player.getPlaybackSpeed();
        else
            return 1.0f;
    }

    @Override
    public void setSonicPitch(float pitch) {
        if (player != null)
            player.setPlaybackPitch(pitch);
    }

    @Override
    public float getSonicPitch() {
        if (player != null)
            return player.getPlaybackPitch();
        else
            return 1.0f;
    }

    @Override
    public void setSonicRate(float rate) {
        if (player != null)
            player.setPlaybackRate(rate);
    }

    @Override
    public float getSonicRate() {
        if (player != null)
            return player.getPlaybackRate();
        else
            return 1.0f;
    }
}
