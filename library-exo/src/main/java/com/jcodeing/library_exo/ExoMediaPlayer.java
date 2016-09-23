package com.jcodeing.library_exo;

import android.content.Context;
import android.net.Uri;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.util.Util;
import com.jcodeing.library_exo.player.DemoPlayer;
import com.jcodeing.library_exo.player.ExtractorRendererBuilder;
import com.jcodeing.library_exo.player.HlsRendererBuilder;
import com.jcodeing.library_exo.player.SmoothStreamingRendererBuilder;

import java.io.FileDescriptor;
import java.util.Map;

public class ExoMediaPlayer extends AbstractMediaPlayer {
    private Context mAppContext;
    private DemoPlayer mInternalPlayer;
    private EventLogger mEventLogger;
    private String mDataSource;
    private int mVideoWidth;
    private int mVideoHeight;
    private Surface mSurface;

    private DemoPlayer.RendererBuilder mRendererBuilder;

    public ExoMediaPlayer(Context context) {
        mAppContext = context.getApplicationContext();

        mDemoListener = new DemoPlayerListener();

        mEventLogger = new EventLogger();
        mEventLogger.startSession();
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
        mSurface = surface;
        if (mInternalPlayer != null)
            mInternalPlayer.setSurface(surface);
    }

    @Override
    public void setDataSource(Context context, Uri uri) {
        mDataSource = uri.toString();
        mRendererBuilder = getRendererBuilder();
    }

    @Override
    public void setDataSource(Context context, Uri uri, Map<String, String> headers) {
        // TODO: handle headers
        setDataSource(context, uri);
    }

    @Override
    public void setDataSource(String path) {
        setDataSource(mAppContext, Uri.parse(path));
    }

    @Override
    public void setDataSource(FileDescriptor fd) {
        // TODO: no support
        throw new UnsupportedOperationException("no support");
    }

    @Override
    public String getDataSource() {
        return mDataSource;
    }

    @Override
    public void prepareAsync() throws IllegalStateException {
        if (mInternalPlayer != null)
            throw new IllegalStateException("can't prepare a prepared player");

        mInternalPlayer = new DemoPlayer(mRendererBuilder);
        mInternalPlayer.addListener(mDemoListener);
        mInternalPlayer.addListener(mEventLogger);
        mInternalPlayer.setInfoListener(mEventLogger);
        mInternalPlayer.setInternalErrorListener(mEventLogger);

        if (mSurface != null)
            mInternalPlayer.setSurface(mSurface);

        mInternalPlayer.prepare();
        mInternalPlayer.setPlayWhenReady(false);
    }

    @Override
    public void start() throws IllegalStateException {
        if (mInternalPlayer == null)
            return;
        mInternalPlayer.setPlayWhenReady(true);
    }

    @Override
    public void stop() throws IllegalStateException {
        if (mInternalPlayer == null)
            return;
        mInternalPlayer.release();
    }

    @Override
    public void pause() throws IllegalStateException {
        if (mInternalPlayer == null)
            return;
        mInternalPlayer.setPlayWhenReady(false);
    }

    @Override
    public void setWakeMode(Context context, int mode) {
        // FIXME: implement
    }

    @Override
    public void setScreenOnWhilePlaying(boolean screenOn) {
        // TODO: do nothing
    }

    @Override
    public int getVideoWidth() {
        return mVideoWidth;
    }

    @Override
    public int getVideoHeight() {
        return mVideoHeight;
    }

    @Override
    public boolean isPlaying() {
        if (mInternalPlayer == null)
            return false;
        int state = mInternalPlayer.getPlaybackState();
        switch (state) {
            case ExoPlayer.STATE_BUFFERING:
            case ExoPlayer.STATE_READY:
                return mInternalPlayer.getPlayWhenReady();
            case ExoPlayer.STATE_IDLE:
            case ExoPlayer.STATE_PREPARING:
            case ExoPlayer.STATE_ENDED:
            default:
                return false;
        }
    }

    @Override
    public void seekTo(long msec) throws IllegalStateException {
        if (mInternalPlayer == null)
            return;
        mInternalPlayer.seekTo(msec);
    }

    @Override
    public long getCurrentPosition() {
        if (mInternalPlayer == null)
            return 0;
        return mInternalPlayer.getCurrentPosition();
    }

    @Override
    public long getDuration() {
        if (mInternalPlayer == null)
            return 0;
        return mInternalPlayer.getDuration();
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
    public void reset() {
        if (mInternalPlayer != null) {
            mInternalPlayer.release();
            mInternalPlayer.removeListener(mDemoListener);
            mInternalPlayer.removeListener(mEventLogger);
            mInternalPlayer.setInfoListener(null);
            mInternalPlayer.setInternalErrorListener(null);
            mInternalPlayer = null;
        }

        mSurface = null;
        mDataSource = null;
        mVideoWidth = 0;
        mVideoHeight = 0;
    }

    @Override
    public void setLooping(boolean looping) {
        // TODO: no support
        throw new UnsupportedOperationException("no support");
    }

    @Override
    public boolean isLooping() {
        // TODO: no support
        return false;
    }

    @Override
    public void setVolume(float leftVolume, float rightVolume) {
        // TODO: no support
    }


    @Override
    public int getAudioSessionId() {
        // TODO: no support
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

    @Override
    public void release() {
        if (mInternalPlayer != null) {
            reset();

            mDemoListener = null;

            mEventLogger.endSession();
            mEventLogger = null;
        }
    }

    public int getBufferedPercentage() {
        if (mInternalPlayer == null)
            return 0;

        return mInternalPlayer.getBufferedPercentage();
    }

    private DemoPlayer.RendererBuilder getRendererBuilder() {
        Uri contentUri = Uri.parse(mDataSource);
        String userAgent = Util.getUserAgent(mAppContext, "ExoMediaPlayer");
        int contentType = inferContentType(contentUri);
        switch (contentType) {
            case Util.TYPE_SS:
                return new SmoothStreamingRendererBuilder(mAppContext, userAgent, contentUri.toString(),
                        new SmoothStreamingTestMediaDrmCallback());
         /*   case Util.TYPE_DASH:
                return new DashRendererBuilder(mAppContext , userAgent, contentUri.toString(),
                        new WidevineTestMediaDrmCallback(contentId, provider));*/
            case Util.TYPE_HLS:
                return new HlsRendererBuilder(mAppContext, userAgent, contentUri.toString());
            case Util.TYPE_OTHER:
            default:
                return new ExtractorRendererBuilder(mAppContext, userAgent, contentUri);
        }
    }

    /**
     * Makes a best guess to infer the type from a media {@link Uri}
     *
     * @param uri The {@link Uri} of the media.
     * @return The inferred type.
     */
    private static int inferContentType(Uri uri) {
        String lastPathSegment = uri.getLastPathSegment();
        return Util.inferContentType(lastPathSegment);
    }

    private class DemoPlayerListener implements DemoPlayer.Listener {
        private boolean mIsPrepareing = false;
        private boolean mDidPrepare = false;
        private boolean mIsBuffering = false;

        public void onStateChanged(boolean playWhenReady, int playbackState) {
            if (mIsBuffering) {
                switch (playbackState) {
                    case ExoPlayer.STATE_ENDED:
                    case ExoPlayer.STATE_READY:
                        notifyOnInfo(IMediaPlayer.MEDIA_INFO_BUFFERING_END, mInternalPlayer.getBufferedPercentage());
                        mIsBuffering = false;
                        break;
                }
            }

            if (mIsPrepareing) {
                switch (playbackState) {
                    case ExoPlayer.STATE_READY:
                        notifyOnPrepared();
                        mIsPrepareing = false;
                        mDidPrepare = false;
                        break;
                }
            }

            switch (playbackState) {
                case ExoPlayer.STATE_IDLE:
                    notifyOnCompletion();
                    break;
                case ExoPlayer.STATE_PREPARING:
                    mIsPrepareing = true;
                    break;
                case ExoPlayer.STATE_BUFFERING:
                    notifyOnInfo(IMediaPlayer.MEDIA_INFO_BUFFERING_START, mInternalPlayer.getBufferedPercentage());
                    mIsBuffering = true;
                    break;
                case ExoPlayer.STATE_READY:
                    break;
                case ExoPlayer.STATE_ENDED:
                    notifyOnCompletion();
                    break;
                default:
                    break;
            }
        }

        public void onError(Exception e) {
            notifyOnError(IMediaPlayer.MEDIA_ERROR_UNKNOWN, IMediaPlayer.MEDIA_ERROR_UNKNOWN);
        }

        public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees,
                                       float pixelWidthHeightRatio) {
            mVideoWidth = width;
            mVideoHeight = height;
            notifyOnVideoSizeChanged(width, height, 1, 1);
            if (unappliedRotationDegrees > 0)
                notifyOnInfo(IMediaPlayer.MEDIA_INFO_VIDEO_ROTATION_CHANGED, unappliedRotationDegrees);
        }
    }

    private DemoPlayerListener mDemoListener;


    // ------------------------------K------------------------------@Sonic
    @Override
    public void setSonicSpeed(float speed) {
        if (mInternalPlayer != null)
            mInternalPlayer.setSonicSpeed(speed);
    }

    @Override
    public float getSonicSpeed() {
        if (mInternalPlayer != null)
            return mInternalPlayer.getSonicSpeed();
        else
            return 1.0f;
    }

    @Override
    public void setSonicPitch(float pitch) {
        if (mInternalPlayer != null)
            mInternalPlayer.setSonicPitch(pitch);
    }

    @Override
    public float getSonicPitch() {
        if (mInternalPlayer != null)
            return mInternalPlayer.getSonicPitch();
        else
            return 1.0f;
    }

    @Override
    public void setSonicRate(float rate) {
        if (mInternalPlayer != null)
            mInternalPlayer.setSonicRate(rate);
    }

    @Override
    public float getSonicRate() {
        if (mInternalPlayer != null)
            return mInternalPlayer.getSonicRate();
        else
            return 1.0f;
    }
}
