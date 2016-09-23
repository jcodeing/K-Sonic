//Copyright 2012 James Falcon
//
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.

package com.prestissimo;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import com.aocate.presto.service.IDeathCallback_0_8;
import com.aocate.presto.service.IOnBufferingUpdateListenerCallback_0_8;
import com.aocate.presto.service.IOnCompletionListenerCallback_0_8;
import com.aocate.presto.service.IOnErrorListenerCallback_0_8;
import com.aocate.presto.service.IOnInfoListenerCallback_0_8;
import com.aocate.presto.service.IOnPitchAdjustmentAvailableChangedListenerCallback_0_8;
import com.aocate.presto.service.IOnPreparedListenerCallback_0_8;
import com.aocate.presto.service.IOnSeekCompleteListenerCallback_0_8;
import com.aocate.presto.service.IOnSpeedAdjustmentAvailableChangedListenerCallback_0_8;

import org.vinuxproject.sonic.Sonic;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.ReentrantLock;

public class Track {
    private boolean isJMono;
    private int jMsec;
    private AudioTrack mTrack;
    private Sonic mSonic;
    private MediaExtractor mExtractor;
    private MediaCodec mCodec;
    private Thread mDecoderThread;
    private String mPath;
    private Uri mUri;
    private final ReentrantLock mLock;
    private final Object mDecoderLock;
    private boolean mContinue;
    private boolean mIsDecoding;
    private long mDuration;
    private float mCurrentSpeed;
    private float mCurrentPitch;
    private int mCurrentState;
    private final Context mContext;

    private final static int TRACK_NUM = 0;
    private final static String TAG_TRACK = "PrestissimoTrack";

    private final static int STATE_IDLE = 0;
    private final static int STATE_INITIALIZED = 1;
    private final static int STATE_PREPARING = 2;
    private final static int STATE_PREPARED = 3;
    private final static int STATE_STARTED = 4;
    private final static int STATE_PAUSED = 5;
    private final static int STATE_STOPPED = 6;
    private final static int STATE_PLAYBACK_COMPLETED = 7;
    private final static int STATE_END = 8;
    private final static int STATE_ERROR = 9;

    // Not available in API 16 :(
    private final static int MEDIA_ERROR_MALFORMED = 0xfffffc11;
    private final static int MEDIA_ERROR_IO = 0xfffffc14;

    // The aidl interface should automatically implement stubs for these, so
    // don't initialize or require null checks.
    protected IOnErrorListenerCallback_0_8 errorCallback;
    protected IOnCompletionListenerCallback_0_8 completionCallback;
    protected IOnBufferingUpdateListenerCallback_0_8 bufferingUpdateCallback;
    protected IOnInfoListenerCallback_0_8 infoCallback;
    protected IOnPitchAdjustmentAvailableChangedListenerCallback_0_8 pitchAdjustmentAvailableChangedCallback;
    protected IOnPreparedListenerCallback_0_8 preparedCallback;
    protected IOnSeekCompleteListenerCallback_0_8 seekCompleteCallback;
    protected IOnSpeedAdjustmentAvailableChangedListenerCallback_0_8 speedAdjustmentAvailableChangedCallback;

    public Track(Context context, IDeathCallback_0_8 cb) {
        mCurrentState = STATE_IDLE;
        mCurrentSpeed = (float) 1.0;
        mCurrentPitch = (float) 1.0;
        mContinue = false;
        mIsDecoding = false;
        mContext = context;
        IDeathCallback_0_8 death = cb;
        mPath = null;
        mUri = null;
        mLock = new ReentrantLock();
        mDecoderLock = new Object();
    }

    // TODO: This probably isn't right...
    public float getCurrentPitchStepsAdjustment() {
        return mCurrentPitch;
    }

    public int getCurrentPosition() {
        switch (mCurrentState) {
            case STATE_ERROR:
                error();
                break;
            default:
                //Relative_onBufferingUpdate
                /*
      相对的缓冲百分比
     */
                int buf_relative;
                try {
                    buf_relative = ((int) (((double) mExtractor.getCachedDuration() / mDuration) * 150));
                } catch (Exception e) {
                    buf_relative = 0;
                    e.printStackTrace();
                }
                try {
                    bufferingUpdateCallback.onBufferingUpdate(buf_relative);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

                //Null Pointer
                try {
                    return (int) (mExtractor.getSampleTime() / 1000);
                } catch (Exception e) {//IOException e) {
//                    error(-1);
                    return 0;
                }
        }
        return 0;
    }

    public float getCurrentSpeed() {
        return mCurrentSpeed;
    }

    public int getDuration() {
        switch (mCurrentState) {
            case STATE_INITIALIZED:
            case STATE_IDLE:
            case STATE_ERROR:
                error();
                break;
            default:
                return (int) (mDuration / 1000);
        }
        return 0;
    }

    public boolean isPlaying() {
        switch (mCurrentState) {
            case STATE_ERROR:
                error();
                break;
            default:
                return mCurrentState == STATE_STARTED;
        }
        return false;
    }

    public void pause() {
        switch (mCurrentState) {
            case STATE_STARTED:
            case STATE_PAUSED:
                try {
                    if (mTrack != null)
                        mTrack.pause();
                } catch (Exception e) {
                    Log.d(TAG_TRACK, "mTrack.pause() Exception:" + e.toString());
                }
                mCurrentState = STATE_PAUSED;
                Log.d(TAG_TRACK, "State changed to STATE_PAUSED");
                break;
            default:
                error();
        }
    }

    public void prepare() throws Exception {
        switch (mCurrentState) {
            case STATE_INITIALIZED:
            case STATE_STOPPED:
                try {
                    initStream();
                } catch (IOException e_io) {
                    Log.e(TAG_TRACK, "Failed setting data source!", e_io);
                    e_io.printStackTrace();
                    error(-1004, -2);//MediaPlayer.MEDIA_ERROR_IO://API:17
                    throw e_io;
                } catch (IllegalStateException e_is) {
                    e_is.printStackTrace();
                    error(-1010, -2);//MediaPlayer.MEDIA_ERROR_UNSUPPORTED //API:17
                    throw e_is;
                } catch (Exception e) {
                    e.printStackTrace();
                    error(-1010, -2);//MediaPlayer.MEDIA_ERROR_UNSUPPORTED //API:17
                    throw e;
                }
                mCurrentState = STATE_PREPARED;
                Log.d(TAG_TRACK, "State changed to STATE_PREPARED");
                try {
                    preparedCallback.onPrepared();
                } catch (Exception e) {//RemoteException
                    // Binder should handle our death
                    Log.e(TAG_TRACK,
                            "RemoteException calling onPrepared after prepare", e);
                }
                break;
            default:
                error();
        }
    }

    public void prepareAsync() {
        switch (mCurrentState) {
            case STATE_INITIALIZED:
            case STATE_STOPPED:
                mCurrentState = STATE_PREPARING;
                Log.d(TAG_TRACK, "State changed to STATE_PREPARING");

                Thread t = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            initStream();
                        } catch (IOException e_io) {
                            Log.e(TAG_TRACK, "Failed setting data source!", e_io);
                            e_io.printStackTrace();
                            error(-1004, -2);//MediaPlayer.MEDIA_ERROR_IO://API:17
                            return;
                        } catch (IllegalStateException e_is) {
                            e_is.printStackTrace();
                            error(-1010, -2);//MediaPlayer.MEDIA_ERROR_UNSUPPORTED //API:17
                            return;
                        } catch (Exception e) {
                            e.printStackTrace();
                            error(-1010, -2);//MediaPlayer.MEDIA_ERROR_UNSUPPORTED //API:17
                            return;
                        }
                        if (mCurrentState != STATE_ERROR) {
                            mCurrentState = STATE_PREPARED;
                            Log.d(TAG_TRACK, "State changed to STATE_PREPARED");
                        }
                        try {
                            preparedCallback.onPrepared();

                        } catch (Exception e) {//RemoteException
                            // Binder should handle our death
                            Log.e(TAG_TRACK,
                                    "RemoteException trying to call onPrepared after prepareAsync",
                                    e);
                        }

                    }
                });
                t.setDaemon(true);
                t.start();

                break;
            default:
                error();
        }
    }

    public void stop() {
        switch (mCurrentState) {
            case STATE_PREPARED:
            case STATE_STARTED:
            case STATE_STOPPED:
            case STATE_PAUSED:
            case STATE_PLAYBACK_COMPLETED:
                mCurrentState = STATE_STOPPED;
                Log.d(TAG_TRACK, "State changed to STATE_STOPPED");
                mContinue = false;
                if (mTrack != null) {
                    mTrack.pause();
                    mTrack.flush();
                }
                break;
            default:
                error();
        }
    }

    public void start() {
        switch (mCurrentState) {
            case STATE_PLAYBACK_COMPLETED:
                //音频播放完成,再次start,初始配置
                try {
                    initConfig();
                } catch (IllegalStateException e_is) {
                    e_is.printStackTrace();
                    error(-1010, -2);//MediaPlayer.MEDIA_ERROR_UNSUPPORTED //API:17
                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                    error(-1010, -2);//MediaPlayer.MEDIA_ERROR_UNSUPPORTED //API:17
                    return;
                }
            case STATE_PREPARED:
                mCurrentState = STATE_STARTED;
                Log.d(SoundService.TAG_API, "State changed to STATE_STARTED");
                mContinue = true;
                if (mTrack != null)
                    mTrack.play();
                try {
                    decode();
                } catch (Exception e) {
                    //IllegalStateException -MediaCodec.start
                }
            case STATE_STARTED:
                break;
            case STATE_PAUSED:
                mCurrentState = STATE_STARTED;
                Log.d(SoundService.TAG_API, "State changed to STATE_STARTED");
                synchronized (mDecoderLock) {
                    mDecoderLock.notify();
                }
                if (mTrack != null)
                    mTrack.play();
                break;
            default:
                mCurrentState = STATE_ERROR;
                Log.d(SoundService.TAG_API, "State changed to STATE_ERROR in start");
                if (mTrack != null) {
                    error();
                } else {
                    Log.d("start",
                            "Attempting to start while in idle after construction.  Not allowed by no callbacks called");
                }
        }
    }

    public void release() {
        reset();
        errorCallback = null;
        completionCallback = null;
        bufferingUpdateCallback = null;
        infoCallback = null;
        pitchAdjustmentAvailableChangedCallback = null;
        preparedCallback = null;
        seekCompleteCallback = null;
        speedAdjustmentAvailableChangedCallback = null;
        mCurrentState = STATE_END;
    }

    public void reset() {
        boolean captured = mLock.tryLock();
        try {
            mContinue = false;
            try {
                if (mDecoderThread != null
                        && mCurrentState != STATE_PLAYBACK_COMPLETED) {
                    while (mIsDecoding) {
                        synchronized (mDecoderLock) {
                            mDecoderLock.notify();
                            mDecoderLock.wait();
                        }
                    }
                }
            } catch (InterruptedException e) {
                Log.e(TAG_TRACK,
                        "Interrupted in reset while waiting for decoder thread to stop.",
                        e);
            }
            if (mCodec != null) {
                mCodec.release();
                mCodec = null;
            }
            if (mExtractor != null) {
                mExtractor.release();
                mExtractor = null;
            }
            if (mTrack != null) {
                mTrack.release();
                mTrack = null;
            }
            mCurrentState = STATE_IDLE;
            Log.d(TAG_TRACK, "State changed to STATE_IDLE");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (captured)
                mLock.unlock();
        }
    }

    public void seekTo(final int msec) {
        switch (mCurrentState) {
            case STATE_PREPARED:
            case STATE_STARTED:
            case STATE_PAUSED:
            case STATE_PLAYBACK_COMPLETED:
                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mLock.lock();
                        try {
                            if (mTrack == null) {
                                try {
                                    //当音频播放完成,进行seekTo,需要重新配置,mcodec
                                    if (mCurrentState == STATE_PLAYBACK_COMPLETED) {
                                        //记录当前seek的位置
                                        jMsec = msec;
                                        //在initconfig中,进行seekto
                                        start();
                                        //RemoteException
                                        seekCompleteCallback.onSeekComplete();
                                    }
                                } catch (Exception e) {//RemoteException
                                    if (e instanceof RemoteException) {
                                        // Binder should handle our death
                                        Log.e(TAG_TRACK,
                                                "Received RemoteException trying to call onSeekComplete in seekTo",
                                                e);
                                    }
                                }
                                return;
                            }
                            mTrack.flush();
                            mExtractor.seekTo(((long) msec * 1000),
                                    MediaExtractor.SEEK_TO_CLOSEST_SYNC);
                            //RemoteException
                            seekCompleteCallback.onSeekComplete();
                        } catch (Exception e) {
                            if (e instanceof RemoteException) {
                                // Binder should handle our death
                                Log.e(TAG_TRACK,
                                        "Received RemoteException trying to call onSeekComplete in seekTo",
                                        e);
                            }
                        } finally {
                            mLock.unlock();
                        }
                    }
                });
                t.setDaemon(true);
                t.start();
                break;
            default:
                error();
        }
    }

    public void setDataSourceString(String path) {
        switch (mCurrentState) {
            case STATE_IDLE:
                mPath = path;
                mCurrentState = STATE_INITIALIZED;
                Log.d(TAG_TRACK, "Moving state to STATE_INITIALIZED");
                break;
            default:
                error();
        }
    }

    public void setDataSourceUri(Uri uri) {
        switch (mCurrentState) {
            case STATE_IDLE:
                mUri = uri;
                mCurrentState = STATE_INITIALIZED;
                Log.d(TAG_TRACK, "Moving state to STATE_INITIALIZED");
                break;
            default:
                error();
        }
    }

    public void setPlaybackPitch(float f) {
        mCurrentPitch = f;
    }

    public void setPlaybackSpeed(float f) {
        mCurrentSpeed = f;
    }

    public void setVolume(float left, float right) {
        // Pass call directly to AudioTrack if available.
        if (null != mTrack) {
            mTrack.setStereoVolume(left, right);
        }
    }

    public void error() {
//        error(0);
    }

    public void error(int what, int extra) {
        Log.e(TAG_TRACK, "Moved to error state!");
        mCurrentState = STATE_ERROR;
        try {
            errorCallback.onError(what * 2, extra);
            //出现什么状态就走什么回调,一定概率下去调用完成回调,有可能出异常
//            boolean handled = errorCallback.onError(
//                    MediaPlayer.MEDIA_ERROR_UNKNOWN, extra);
//            if (!handled) {
//                completionCallback.onCompletion();
//            }
        } catch (Exception e) {//RemoteException
            if (e instanceof RemoteException) {
                // Binder should handle our death
                Log.e(TAG_TRACK,
                        "Received RemoteException when trying to call onCompletion in error state",
                        e);
            }
        }
    }

    private int findFormatFromChannels(int numChannels) {
        switch (numChannels) {
            case 1:
                return AudioFormat.CHANNEL_OUT_MONO;
            case 2:
                return AudioFormat.CHANNEL_OUT_STEREO;
            default:
                return -1; // Error
        }
    }

    public void initStream() throws Exception {
        mLock.lock();
        try {
            mExtractor = new MediaExtractor();
            if (!TextUtils.isEmpty(mPath)) {
                mExtractor.setDataSource(mPath);
            } else if (mUri != null) {
                mExtractor.setDataSource(mContext, mUri, null);
            } else {
                throw new IOException();
            }
        } catch (Exception e) {//IOException
            throw e;
        } finally {
            mLock.unlock();
        }
        initConfig();
    }

    public void initConfig() throws Exception {
        if (mCurrentState == STATE_PLAYBACK_COMPLETED) {
            mExtractor.seekTo(((long) jMsec * 1000), MediaExtractor.SEEK_TO_CLOSEST_SYNC);
            //归零
            jMsec = 0;
        }
        mLock.lock();
        try {
            MediaFormat oFormat = mExtractor.getTrackFormat(TRACK_NUM);
            //oFormat NullPointerException
            int sampleRate = oFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            int channelCount = oFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
            //Deal with mono ,, Some models do not support
            if (isJMono = channelCount == 1)
                oFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, channelCount = 2);
            String mime = oFormat.getString(MediaFormat.KEY_MIME);
            //Deal with mime ,, Some models will mime identification into ffmpeg
            if ("audio/ffmpeg".equals(mime))
                oFormat.setString(MediaFormat.KEY_MIME, mime = "audio/mpeg");
            mDuration = oFormat.getLong(MediaFormat.KEY_DURATION);

            Log.v(TAG_TRACK, "Sample rate: " + sampleRate);
            Log.v(TAG_TRACK, "Mime type: " + mime);

            initDevice(sampleRate, channelCount);
            mExtractor.selectTrack(TRACK_NUM);
            mCodec = MediaCodec.createDecoderByType(mime);
            mCodec.configure(oFormat, null, null, 0);
        } catch (Exception e) {//IOException
            throw e;
        } finally {
            mLock.unlock();
        }
    }

    private void initDevice(int sampleRate, int numChannels) {
        if (isJMono)
            numChannels = 2;
        mLock.lock();
        try {
            final int format = findFormatFromChannels(numChannels);
            final int minSize = AudioTrack.getMinBufferSize(sampleRate, format,
                    AudioFormat.ENCODING_PCM_16BIT);
            mTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, format,
                    AudioFormat.ENCODING_PCM_16BIT, minSize * 4,
                    AudioTrack.MODE_STREAM);
            mSonic = new Sonic(sampleRate, numChannels);
        } catch (Exception e) {//IllegalArgumentException
            throw e;
        } finally {
            mLock.unlock();
        }
    }

    public void decode() {
        mDecoderThread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (mCodec == null)
                    return;

                try {
                    mIsDecoding = true;
                    mCodec.start();

                    ByteBuffer[] inputBuffers = mCodec.getInputBuffers();
                    ByteBuffer[] outputBuffers = mCodec.getOutputBuffers();

                    boolean sawInputEOS = false;
                    boolean sawOutputEOS = false;

                    while (!sawInputEOS && !sawOutputEOS && mContinue) {

                        if (mCurrentState == STATE_PAUSED) {
                            System.out.println("Decoder changed to PAUSED");
                            try {
                                synchronized (mDecoderLock) {
                                    mDecoderLock.wait();
                                    System.out.println("Done with wait");
                                }
                            } catch (InterruptedException e) {
                                // Purposely not doing anything here
                            }
                            continue;
                        }

                        if (null != mSonic) {
                            mSonic.setSpeed(mCurrentSpeed);
                            mSonic.setPitch(mCurrentPitch);
                        }

                        int inputBufIndex = mCodec.dequeueInputBuffer(200);
                        if (inputBufIndex >= 0) {
                            ByteBuffer dstBuf = inputBuffers[inputBufIndex];
                            int sampleSize = mExtractor.readSampleData(dstBuf, 0);
                            long presentationTimeUs = 0;
                            if (sampleSize < 0) {
                                sawInputEOS = true;
                                sampleSize = 0;
                            } else {
                                presentationTimeUs = mExtractor.getSampleTime();
                            }
                            mCodec.queueInputBuffer(
                                    inputBufIndex,
                                    0,
                                    sampleSize,
                                    presentationTimeUs,
                                    sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                            : 0);
                            if (!sawInputEOS) {
                                mExtractor.advance();
                            }
                        }

                        final MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                        byte[] modifiedSamples = new byte[info.size];

                        int res;
                        do {
                            res = mCodec.dequeueOutputBuffer(info, 200);
                            if (res >= 0) {
                                int outputBufIndex = res;
                                ByteBuffer buf = outputBuffers[outputBufIndex];
                                final byte[] chunk = new byte[info.size];
                                outputBuffers[res].get(chunk);
                                outputBuffers[res].clear();

                                if (chunk.length > 0) {
                                    mSonic.putBytes(chunk, chunk.length);
                                } else {
                                    mSonic.flush();
                                }
                                int available = mSonic.availableBytes();
                                if (available > 0) {
                                    if (modifiedSamples.length < available) {
                                        modifiedSamples = new byte[available];
                                    }
                                    mSonic.receiveBytes(modifiedSamples, available);
                                    //dispose Mono
                                    if (isJMono)
                                        available = (modifiedSamples = MonoToStereo(modifiedSamples)).length;
                                    mTrack.write(modifiedSamples, 0, available);
                                }

                                mCodec.releaseOutputBuffer(outputBufIndex, false);

                                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                    sawOutputEOS = true;
                                }
                            } else if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                                outputBuffers = mCodec.getOutputBuffers();
                                Log.d("PCM", "Output buffers changed");
                            } else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                                mTrack.stop();
                                mLock.lock();
                                try {
                                    mTrack.release();
                                    final MediaFormat oformat = mCodec
                                            .getOutputFormat();
                                    Log.d("PCM", "Output format has changed to"
                                            + oformat);
                                    initDevice(
                                            oformat.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                                            oformat.getInteger(MediaFormat.KEY_CHANNEL_COUNT));
                                    outputBuffers = mCodec.getOutputBuffers();
                                    mTrack.play();
                                } catch (Exception e) {//IllegalStateException
                                    e.printStackTrace();
                                } finally {
                                    mLock.unlock();
                                }
                            }
                        } while (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED
                                || res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED);
                    }
                    Log.d(TAG_TRACK,
                            "Decoding loop exited. Stopping codec and track");
                    Log.d(TAG_TRACK, "Duration: " + (int) (mDuration / 1000));
                    Log.d(TAG_TRACK,
                            "Current position: "
                                    + (int) (mExtractor.getSampleTime() / 1000));
                    //play end ------release
                    mCodec.release();
                    mCodec = null;
                    mTrack.release();
                    mTrack = null;

                    Log.d(TAG_TRACK, "Stopped codec and track");
                    Log.d(TAG_TRACK,
                            "Current position: "
                                    + (int) (mExtractor.getSampleTime() / 1000));
                    mIsDecoding = false;
                    if (mContinue && (sawInputEOS || sawOutputEOS)) {
                        mCurrentState = STATE_PLAYBACK_COMPLETED;
                        Thread t = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    completionCallback.onCompletion();
                                } catch (RemoteException e) {
                                    // Binder should handle our death
                                    Log.e(TAG_TRACK,
                                            "RemoteException trying to call onCompletion after decoding",
                                            e);
                                }
                            }
                        });
                        t.setDaemon(true);
                        t.start();
                    } else {
                        Log.d(TAG_TRACK,
                                "Loop ended before saw input eos or output eos");
                        Log.d(TAG_TRACK, "sawInputEOS: " + sawInputEOS);
                        Log.d(TAG_TRACK, "sawOutputEOS: " + sawOutputEOS);
                    }
                    synchronized (mDecoderLock) {
                        mDecoderLock.notifyAll();
                    }
                } catch (MediaCodec.CryptoException e) {
                    e.printStackTrace();
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        mDecoderThread.setDaemon(true);
        mDecoderThread.start();
    }

    /**
     * Part of the Chinese mobile phone does not support mono
     *
     * @param input
     * @return
     */
    private byte[] MonoToStereo(byte[] input) {
        byte[] output = new byte[input.length * 2];
        int outputIndex = 0;
        for (int n = 0; n < input.length; n += 2) {
            // copy in the first 16 bit sample
            output[outputIndex++] = input[n];
            output[outputIndex++] = input[n + 1];
            // now copy it in again
            output[outputIndex++] = input[n];
            output[outputIndex++] = input[n + 1];
        }
        return output;
    }


    public static final int MPEG1 = 3;
    public static final int MPEG2 = 2;
    public static final int MPEG25 = 0;
    public static final int MAX_FRAMESIZE = 1732;    //MPEG 1.0/2.0/2.5, Layer 1/2/3

    /*
     * bitrate[lsf][layer-1][bitrate_index]
     */
    private static final int[][][] bitrate = {
            {
                    //MPEG 1
                    //Layer I
                    {0, 32, 64, 96, 128, 160, 192, 224, 256, 288, 320, 352, 384, 416, 448},
                    //Layer II
                    {0, 32, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 384},
                    //Layer III
                    {0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320}
            },
            {
                    //MPEG 2.0/2.5
                    //Layer I
                    {0, 32, 48, 56, 64, 80, 96, 112, 128, 144, 160, 176, 192, 224, 256},
                    //Layer II
                    {0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160},
                    //Layer III
                    {0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160}
            }
    };

    /*
     * samplingRate[verID][sampling_frequency]
     */
    private static final int[][] samplingRate = {
            {11025, 12000, 8000, 0},    //MPEG Version 2.5
            {0, 0, 0, 0,},                    //reserved
            {22050, 24000, 16000, 0},    //MPEG Version 2 (ISO/IEC 13818-3)
            {44100, 48000, 32000, 0}        //MPEG Version 1 (ISO/IEC 11172-3)
    };

    /*
     * verID: 2 bits
     * "00"  MPEG Version 2.5 (unofficial extension of MPEG 2);
     * "01"  reserved;
     * "10"  MPEG Version 2 (ISO/IEC 13818-3);
     * "11"  MPEG Version 1 (ISO/IEC 11172-3).
     */
    private static int verID;

    /*
     * layer: 2 bits
     * "11"	 Layer I
     * "10"	 Layer II
     * "01"	 Layer III
     * "00"	 reserved
     * 已换算intLayer=4-layer: 1-Layer I; 2-Layer II; 3-Layer III; 4-reserved
     */
    private static int layer;

    /*
     * protection_bit: 1 bit
     * "1"  no CRC;
     * "0"  protected by 16 bit CRC following header.
     */
    private static int protection_bit;

    /*
     * bitrate_index: 4 bits
     */
    private static int bitrate_index;

    /*
     * sampling_frequency: 2 bits
     * '00'	 44.1kHz
     * '01'	 48kHz
     * '10'	 32kHz
     * '11'  reserved
     */
    private static int sampling_frequency;

    /*
     * mode: 2 bits
     * '00'  Stereo;
     * '01'  Joint Stereo (Stereo);
     * '10'  Dual channel (Two mono channels);
     * '11'  Single channel (Mono).
     */
    private static int mode;

    /*
     * mode_extension: 2 bits
     * 		 intensity_stereo	boolMS_Stereo
     * '00'	 off				off
     * '01'	 on					off
     * '10'	 off				on
     * '11'	 on					on
     */
    private static int mode_extension;

    private static int frameSize;
    private static int maindataSize;    //main_data length
    private static int sideinfoSize;    //side_information length
    private static int lsf;
    private static int headerMask;
    private static boolean boolMS, boolIntensity;


    private void parseHeader(int h) {
        verID = (h >> 19) & 3;
        layer = 4 - (h >> 17) & 3;
        protection_bit = (h >> 16) & 0x1;
        bitrate_index = (h >> 12) & 0xF;
        sampling_frequency = (h >> 10) & 3;
        int padding_bit = (h >> 9) & 0x1;
        mode = (h >> 6) & 3;
        mode_extension = (h >> 4) & 3;

        boolMS = mode == 1 && (mode_extension & 2) != 0;
        boolIntensity = mode == 1 && (mode_extension & 0x1) != 0;
        lsf = (verID == MPEG1) ? 0 : 1;

        switch (layer) {
            case 1:
                frameSize = bitrate[lsf][0][bitrate_index] * 12000;
                frameSize /= samplingRate[verID][sampling_frequency];
                frameSize = ((frameSize + padding_bit) << 2);
                break;
            case 2:
                frameSize = bitrate[lsf][1][bitrate_index] * 144000;
                frameSize /= samplingRate[verID][sampling_frequency];
                frameSize += padding_bit;
                break;
            case 3:
                frameSize = bitrate[lsf][2][bitrate_index] * 144000;
                frameSize /= samplingRate[verID][sampling_frequency] << (lsf);
                frameSize += padding_bit;
                //计算帧边信息长度
                if (verID == MPEG1)
                    sideinfoSize = (mode == 3) ? 17 : 32;
                else
                    sideinfoSize = (mode == 3) ? 9 : 17;
                break;
        }

        //计算主数据长度
        maindataSize = frameSize - 4 - sideinfoSize;
        if (protection_bit == 0)
            maindataSize -= 2;    //CRC
    }

    private int byte2int(byte[] b, int off) {
        int h = b[off] & 0xff;
        h <<= 8;
        h |= b[off + 1] & 0xff;
        h <<= 8;
        h |= b[off + 2] & 0xff;
        h <<= 8;
        h |= b[off + 3] & 0xff;
        return h;
    }

    private static int intFrameCounter;    //当前帧序号
    private static boolean boolSync;    //true:帧头的特征未改变

    /*
     * 帧同步: 查找到帧同步字后与下一帧的verID等比较,确定是否找到有效的同步字.
     * 返回b的下标值.返回负值表示b[off]之后未查找到同步字.
     */
    public int syncFrame(byte[] b, int off, int endPos) {
        int h, idx = off, i, curmask = 0;
        if (endPos - off < 4)
            return -off;
        h = (b[idx++] << 24) | ((b[idx++] & 0xff) << 16)
                | ((b[idx++] & 0xff) << 8) | (b[idx++] & 0xff);
        int resemask = h & 0x1EFC00;
        while (true) {
            // 1.查找帧同步字
            while ((h & headerMask) != headerMask
                    || ((h >> 19) & 3) == 1        // version ID:  01 - reserved
                    || ((h >> 17) & 3) == 0        // Layer index: 00 - reserved
                    || ((h >> 12) & 0xf) == 0xf    // Bitrate Index: 1111 - reserved
                    || ((h >> 12) & 0xf) == 0    // Bitrate Index: 0000 - free
                    || ((h >> 10) & 3) == 3)    // Sampling Rate Index: 11 - reserved
            {
                h = (h << 8) | (b[idx++] & 0xff);
                if (idx == endPos)
                    return 3 - idx;
            }
            if (idx - off > 4)
                boolSync = false;

            // 2. 解析帧头
            parseHeader(h);
            if (endPos - idx + 4 < frameSize)
                return 4 - idx;

            // 若verID等帧的特征未改变(boolSync=true),不用与下一帧的同步头比较
            if (boolSync)
                break;

            // 3.与下一帧的同步头比较
            curmask = 0xffe00000;    // syncword
            curmask |= h & 0x180000;// version ID
            curmask |= h & 0x60000;    // Layer index
            curmask |= h & 0x60000;    // sampling_frequency
            // mode,mode_extension 不是始终不变.

            if (idx + frameSize > endPos)
                return 4 - idx;
            i = byte2int(b, idx + frameSize - 4);
            if ((i & curmask) == curmask && ((i >> 19) & 3) != 1
                    && ((i >> 17) & 3) != 0 && ((i >> 12) & 15) != 15
                    && ((i >> 12) & 0xf) != 0 && ((i >> 10) & 3) != 3) {
                boolSync = true;
                if (headerMask == 0xffe00000) {    // 是第一帧
                    longFrames = longAllFrameSize / frameSize;
                    parseVBR(b, idx);
                    headerMask = curmask;
                    floatFrameDuration = 1152f / (getFrequency() << lsf);
                }
                break;
            }
            h = (h << 8) | (b[idx++] & 0xff);
        }

        if (protection_bit == 0)
            idx += 2;    //CRC
        intFrameCounter++;
        return idx;
    }

    public boolean isMS() {
        return boolMS;
    }

    public boolean isIntensity() {
        return boolIntensity;
    }

    public int getBitrate() {
        return bitrate[lsf][layer - 1][bitrate_index];
    }

    public int getBitrateIndex() {
        return bitrate_index;
    }

    public int getChannels() {
        return mode == 3 ? 1 : 2;
    }

    public int getMode() {
        return mode;
    }

    public int getModeExtension() {
        return mode_extension;
    }

    public int getVersion() {
        return verID;
    }

    public int getLayer() {
        return layer;
    }

    public int getSampleFrequency() {
        return sampling_frequency;
    }

    public int getFrequency() {
        return samplingRate[verID][sampling_frequency];
    }

    public int getMainDataSize() {
        return maindataSize;
    }

    public int getSideInfoSize() {
        return sideinfoSize;
    }

    public int getFrameSize() {
        return frameSize;
    }

    public int getFrameCounter() {
        return intFrameCounter;
    }

    public long getTrackFrames() {
        return longFrames;
    }

    public float getFrameDuration() {
        return floatFrameDuration;
    }


    public void setTrackBytes(long len) {
        longAllFrameSize = len;
    }

    // MP3 文件帧数等信息
    private static long longAllFrameSize;    //帧长度总和(文件长度减去tag长度)
    private static long longFrames;            //帧数
    private static float floatFrameDuration;//一帧时长(秒)

    private void parseVBR(byte[] b, int off) {
        //System.out.println("tagsize=" + (frameSize-4-sideinfoSize));
        int maxIdx = off + frameSize - 4, idx = off;
        if (maxIdx >= b.length)
            return;
        for (int i = 2; i < sideinfoSize; i++)    //前2字节可能是CRC_word
            if (b[idx + i] != 0)
                return;
        idx += sideinfoSize;

        //-------------------------------VBR tag------------------------------
        int intTocFactor;
        int intTocPer;
        int intTocNumber;
        byte[] byteVBRToc;
        if ((b[idx] == 'X' && b[idx + 1] == 'i' && b[idx + 2] == 'n' && b[idx + 3] == 'g') ||
                (b[idx] == 'I' && b[idx + 1] == 'n' && b[idx + 2] == 'f' && b[idx + 3] == 'o')) {
            //--------Xing/Info header--------
            if (maxIdx - idx < 120)
                return;
            longAllFrameSize -= frameSize;
            int xing_flags = byte2int(b, idx + 4);
            if ((xing_flags & 1) == 1) {    // track frames
                longFrames = byte2int(b, idx + 8);
                System.out.println("track frames: " + longFrames + "  [" + new String(b, idx, 4) + "]");
                idx += 4;
            }
            idx += 8;    // VBR header ID + flag
            if ((xing_flags & 0x2) != 0) { // track bytes
                longAllFrameSize = byte2int(b, idx);
                idx += 4;
                System.out.println(" track bytes: " + longAllFrameSize);
            }
            if ((xing_flags & 0x4) != 0) { // TOC: 100 bytes.
                byteVBRToc = new byte[100];
                System.arraycopy(b, idx, byteVBRToc, 0, 100);
                idx += 100;
                //System.out.println("         TOC: true");
            }
            if ((xing_flags & 0x8) != 0) { // VBR quality
                int xing_quality = byte2int(b, idx);
                idx += 4;
                System.out.println("     quality: " + xing_quality);
            }
            intTocNumber = 100;    //TOC共100个表项
            intTocPer = 1;        //每个表项1字节
            intTocFactor = 1;
        } else if (b[idx] == 'V' && b[idx + 1] == 'B' && b[idx + 2] == 'R' && b[idx + 3] == 'I') {
            //--------VBRI header--------
            if (maxIdx - idx < 26)
                return;
            //version ID: 2 bytes
            //Delay: 2 bytes
            int vbri_quality = (b[idx + 8] & 0xff) | (b[idx + 9] & 0xff);
            System.out.println("     quality: " + vbri_quality + "  [" + new String(b, 0, idx + 4) + "]");
            longAllFrameSize = byte2int(b, idx + 10);
            System.out.println(" track bytes: " + longAllFrameSize);
            longFrames = byte2int(b, idx + 14);
            System.out.println("track frames: " + longFrames);
            intTocNumber = (b[idx + 18] & 0xff) | (b[idx + 19] & 0xff);
            intTocFactor = (b[idx + 20] & 0xff) | (b[idx + 21] & 0xff);
            intTocPer = (b[idx + 22] & 0xff) | (b[idx + 23] & 0xff);
            //int toc_frames = (b[idx+24] & 0xff) | (b[idx+25] & 0xff);	//每个TOC表项的帧数
            idx += 26;
            int toc_size = intTocNumber * intTocPer;
            if (maxIdx - idx < toc_size)
                return;
            System.out.println("         TOC: " + intTocNumber + " * " +
                    intTocPer + " = " + toc_size + "factor=" + intTocFactor);
            byteVBRToc = new byte[toc_size];
            System.arraycopy(b, idx, byteVBRToc, 0, toc_size);
            idx += toc_size;
        } else
            return;

        //-------------------------------LAME tag------------------------------
        //9+1+1+8+1+1+3+1+1+2+4+2+2=36 bytes
        String strBitRate;
        if (maxIdx - idx < 36 || b[idx] == 0) {
            strBitRate = "VBR";
            return;
        }
        //Encoder Version: 9 bytes
        String strEncoder = new String(b, idx, 9);
        idx += 9;
        System.out.println("     encoder: " + strEncoder);

        //'Info Tag' revision + VBR method: 1 byte
        //boolean isCBR=false, isABR=false, isVBR=false;
        int revi = (b[idx] & 0xff) >> 4;    //0:rev0; 1:rev1; 15:reserved
        int lame_vbr = b[idx++] & 0xf;        //0:unknown

        //Lowpass filter value(低通滤波上限值): 1 byte
        int lowpass = b[idx++] & 0xff;
        System.out.println("     lowpass: " + (lowpass * 100) + "Hz" + "  [revi " + revi + "]");

        //Replay Gain(回放增益):8 bytes
        float peak = Float.intBitsToFloat(byte2int(b, idx));    //Peak signal amplitude
        idx += 4;
        int radio = ((b[idx] & 0xff) << 8) | (b[idx + 1] & 0xff);    //Radio Replay Gain
        /*
        * radio:
		* bits 0h-2h: NAME of Gain adjustment:
		*	000 = not set
		*	001 = radio
		*	010 = audiophile
		* bits 3h-5h: ORIGINATOR of Gain adjustment:
		*	000 = not set
		*	001 = set by artist
		*	010 = set by user
		*	011 = set by my model
		*	100 = set by simple RMS average
		* bit 6h: Sign bit
		* bits 7h-Fh: ABSOLUTE GAIN ADJUSTMENT.
		*  storing 10x the adjustment (to give the extra decimal place).
		*/
        idx += 2;
        int phile = ((b[idx] & 0xff) << 8) | (b[idx + 1] & 0xff);    //Audiophile Replay Gain
        /*
        * phile各位含义同上(radio)
		*/
        idx += 2;

        //Encoding flags + ATH Type: 1 byte
        /*int enc_flag = (b[iOff] & 0xff) >> 4;
        int ath_type = b[iOff] & 0xf;
		//000?0000: LAME uses "--nspsytune" ?
		boolean nsp = ((enc_flag & 0x1) == 0) ? false : true;
		//00?00000: LAME uses "--nssafejoint" ?
		boolean nsj = ((enc_flag & 0x2) == 0) ? false : true;
		//0?000000: This track is --nogap continued in a next track ?
		//is true for all but the last track in a --nogap album
		boolean nogap_next = ((enc_flag & 0x4) == 0) ? false : true;
		//?0000000: This track is the --nogap continuation of an earlier one ?
		//is true for all but the first track in a --nogap album
		boolean nogap_cont = ((enc_flag & 0x8) == 0) ? false : true;*/
        idx++;

        // ABR/CBR位率或VBR的最小位率(0xFF表示位率为255Kbps以上): 1 byte
        int lame_bitrate = b[idx++] & 0xff;
        switch (lame_vbr) {
            case 1:
            case 8: // CBR
                strBitRate = String.format("CBR %1$dK", getBitrate());
                break;
            case 2:
            case 9: // ABR
                if (lame_bitrate < 0xff)
                    strBitRate = String.format("ABR %1$dK", lame_bitrate);
                else
                    strBitRate = String.format("ABR %1$dK以上", lame_bitrate);
                break;
            default:
                if (lame_bitrate == 0)    // 0: unknown is VBR ?
                    strBitRate = "VBR";
                else
                    strBitRate = String.format("VBR %1$dK以上", lame_bitrate);
        }

        //Encoder delays: 3 bytes
        idx += 3;

        //Misc: 1 byte
        idx++;

        //MP3 Gain: 1 byte.
        //任何MP3能无损放大2^(mp3_gain/4),以1.5dB为步进值改变'Replay Gain'的3个域:
        //	"Peak signal amplitude", "Radio Replay Gain", "Audiophile Replay Gain"
        //mp3_gain = -127..+127, 对应的:
        //	分贝值-190.5dB..+190.5dB; mp3_gain增加1, 增加1.5dB
        //	放大倍数0.000000000276883..3611622602.83833951
        int mp3_gain = b[idx++];    //其缺省值为0
        if (mp3_gain != 0)
            System.out.println("    MP3 Gain: " + mp3_gain + " [psa=" + peak + ",rrg=" + radio + ",arg=" + phile + "]");

        //Preset and surround info: 2 bytes
        int preset_surround = ((b[idx] & 0xff) << 8) | (b[idx + 1] & 0xff);
        int surround_info = (preset_surround >> 11) & 0x7;
        switch (surround_info) {
            case 0:        //no surround info
                break;
            case 1:        //DPL encoding
                System.out.println("    surround: DPL");
                break;
            case 2:        //DPL2 encoding
                System.out.println("    surround: DPL2");
                break;
            case 3:        //Ambisonic encoding
                System.out.println("    surround: Ambisonic");
                break;
            case 7:        // reserved
                System.out.println("    surround: invalid data");
                break;
        }
        preset_surround &= 0x7ff;    //11 bits: 2047 presets
        if (preset_surround != 0)    //0: unknown / no preset used
            System.out.println("    surround: preset " + preset_surround);
        idx += 2;

        //MusicLength: 4 bytes
        //MP3文件原始的(即除去ID3 tag,APE tag等)'LAME Tag frame'和'音乐数据'的总字节数
        int music_len = byte2int(b, idx);
        idx += 4;
        if (music_len != 0)
            longAllFrameSize = music_len;

        //MusicCRC: 2 bytes
        idx += 2;

        //CRC-16 of Info Tag: 2 bytes
    }


    private static StringBuffer progress;
    private static int progress_index = 1;

    public void printState() {
        float t = intFrameCounter * floatFrameDuration;
        int m = (int) (t / 60);
        float s = t - 60 * m;
        int i = ((int) (100f * intFrameCounter / longFrames + 0.5) << 2) / 10;
        if (i == progress_index) {
            progress.replace(i - 1, i + 1, "=>");
            progress_index++;
        }
        System.out.printf("\r#%-5d [%-41s] %02d:%04.1f ", intFrameCounter, progress, m, s);
    }

}
