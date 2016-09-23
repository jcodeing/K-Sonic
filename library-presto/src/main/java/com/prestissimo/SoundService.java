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

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.util.SparseArray;

import com.aocate.presto.service.IDeathCallback_0_8;
import com.aocate.presto.service.IOnBufferingUpdateListenerCallback_0_8;
import com.aocate.presto.service.IOnCompletionListenerCallback_0_8;
import com.aocate.presto.service.IOnErrorListenerCallback_0_8;
import com.aocate.presto.service.IOnInfoListenerCallback_0_8;
import com.aocate.presto.service.IOnPitchAdjustmentAvailableChangedListenerCallback_0_8;
import com.aocate.presto.service.IOnPreparedListenerCallback_0_8;
import com.aocate.presto.service.IOnSeekCompleteListenerCallback_0_8;
import com.aocate.presto.service.IOnSpeedAdjustmentAvailableChangedListenerCallback_0_8;
import com.aocate.presto.service.IPlayMedia_0_8;

public class SoundService extends Service {
    private SparseArray<Track> mTracks;
    private static int trackId = 0;

    private final static String TAG_SERVICE = "PrestissimoService";
    protected final static String TAG_API = "PrestissimoAPI";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG_SERVICE, "Service created");
        mTracks = new SparseArray<Track>();
    }

    @Override
    public void onDestroy() {
        for (int id = 0; id < trackId; id++) {
            removeTrack(id);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG_SERVICE, "Returning binder");
        return mBinder;
    }

    // Indicates client has crashed. Stop the track and release any resources
    // associated with it
    private void handleRemoteException(long lSessionId) {
        Log.e(TAG_SERVICE, "Received RemoteException.  Service will die.");
        int sessionId = (int) lSessionId;
        removeTrack(sessionId);
    }

    private void removeTrack(int sessionId) {
        Track track = mTracks.get(sessionId);
        if (track != null) {
            track.release();
            mTracks.delete(sessionId);
        }
    }

    private final IPlayMedia_0_8.Stub mBinder = new IPlayMedia_0_8.Stub() {

        @Override
        public boolean canSetPitch(long sessionId) {
            return true;
        }

        @Override
        public boolean canSetSpeed(long sessionId) {
            return true;
        }

        @Override
        public float getCurrentPitchStepsAdjustment(long sessionId) {
            Track track;
            synchronized (this) {
                track = mTracks.get((int) sessionId);
            }
            return track.getCurrentPitchStepsAdjustment();
        }

        @Override
        public int getCurrentPosition(long sessionId) {
            Track track;
            synchronized (this) {
                track = mTracks.get((int) sessionId);
            }
            if (track != null) {
                return track.getCurrentPosition();
            } else {
                //TODO track为null暂返回0
                return 0;
            }
        }

        @Override
        public float getCurrentSpeedMultiplier(long sessionId) {
            Track track = mTracks.get((int) sessionId);
            return track.getCurrentSpeed();
        }

        @Override
        public int getDuration(long sessionId) {
            Track track;
            synchronized (this) {
                track = mTracks.get((int) sessionId);
            }
            if (track != null) {
                return track.getDuration();
            } else {
                //TODO track为null暂返回0
                return 0;
            }
        }

        @Override
        public float getMaxSpeedMultiplier(long sessionId) {
            return 2;
        }

        @Override
        public float getMinSpeedMultiplier(long sessionId) {
            return (float) 0.5;
        }

        @Override
        public int getVersionCode() {
            return -1;
        }

        @Override
        public String getVersionName() {
            return "";
        }

        @Override
        public boolean isLooping(long sessionId) {
            // No
            return false;
        }

        @Override
        public boolean isPlaying(long sessionId) {
            Track track;
            synchronized (this) {
                track = mTracks.get((int) sessionId);
            }
            return track != null && track.isPlaying();
        }

        @Override
        public void pause(long sessionId) {
            Log.d(TAG_API, "Session: " + sessionId + ". Pause called");
            Track track;
            synchronized (this) {
                track = mTracks.get((int) sessionId);
            }
            if (track != null) {
                track.pause();
            }
        }

        @Override
        public void prepare(long sessionId) throws RemoteException {
            Log.d(TAG_API, "Session: " + sessionId + ". Prepare called");
            Track track;
            synchronized (this) {
                track = mTracks.get((int) sessionId);
            }
            //AIDL,必须抛出异常,终止切换媒体框架
            try {
                track.prepare();
            } catch (Exception e) {
                e.printStackTrace();
                throw new RemoteException();
            }
        }

        @Override
        public void prepareAsync(long sessionId) {
            Track track;
            synchronized (this) {
                track = mTracks.get((int) sessionId);
            }
            Log.d(TAG_API, "Session: " + sessionId + ". PrepareAsync called");
            track.prepareAsync();
        }

        @Override
        public void registerOnBufferingUpdateCallback(long sessionId,
                                                      IOnBufferingUpdateListenerCallback_0_8 cb) {
            Track track;
            synchronized (this) {
                track = mTracks.get((int) sessionId);
            }
            track.bufferingUpdateCallback = cb;
        }

        @Override
        public void registerOnCompletionCallback(long sessionId,
                                                 IOnCompletionListenerCallback_0_8 cb) {
            Track track;
            synchronized (this) {
                track = mTracks.get((int) sessionId);
            }
            track.completionCallback = cb;
        }

        @Override
        public void registerOnErrorCallback(long sessionId,
                                            IOnErrorListenerCallback_0_8 cb) {
            Track track;
            synchronized (this) {
                track = mTracks.get((int) sessionId);
            }
            track.errorCallback = cb;
        }

        @Override
        public void registerOnInfoCallback(long sessionId,
                                           IOnInfoListenerCallback_0_8 cb) {
            Track track;
            synchronized (this) {
                track = mTracks.get((int) sessionId);
            }
            track.infoCallback = cb;
        }

        @Override
        public void registerOnPitchAdjustmentAvailableChangedCallback(
                long sessionId,
                IOnPitchAdjustmentAvailableChangedListenerCallback_0_8 cb) {
            Track track;
            synchronized (this) {
                track = mTracks.get((int) sessionId);
            }
            track.pitchAdjustmentAvailableChangedCallback = cb;
        }

        @Override
        public void registerOnPreparedCallback(long sessionId,
                                               IOnPreparedListenerCallback_0_8 cb) {
            Track track;
            synchronized (this) {
                track = mTracks.get((int) sessionId);
            }
            track.preparedCallback = cb;
        }

        @Override
        public void registerOnSeekCompleteCallback(long sessionId,
                                                   IOnSeekCompleteListenerCallback_0_8 cb) {
            Track track;
            synchronized (this) {
                track = mTracks.get((int) sessionId);
            }
            track.seekCompleteCallback = cb;
        }

        @Override
        public void registerOnSpeedAdjustmentAvailableChangedCallback(
                long sessionId,
                IOnSpeedAdjustmentAvailableChangedListenerCallback_0_8 cb) {
            Track track;
            synchronized (this) {
                track = mTracks.get((int) sessionId);
            }
            track.speedAdjustmentAvailableChangedCallback = cb;
        }

        @Override
        public void release(long sessionId) {
            Log.d(TAG_API, "Session: " + sessionId + ". Release called");
            synchronized (this) {
                removeTrack((int) sessionId);
            }
            Log.d(TAG_API, "Session: " + sessionId
                    + ". State changed to Track.STATE_END");
        }

        @Override
        public void reset(long sessionId) {
            Log.d(TAG_API, "Session: " + sessionId + ". Reset called");
            Track track;
            synchronized (this) {
                track = mTracks.get((int) sessionId);
            }
            if (track != null) {
                track.reset();
                Log.d(TAG_API, "Session: " + sessionId + ". End of reset");
            }
        }

        @Override
        public void seekTo(long sessionId, final int msec) {
            Log.d(TAG_API, "Session: " + sessionId + ". SeekTo called");
            Track track;
            synchronized (this) {
                track = mTracks.get((int) sessionId);
            }
            if (track != null) {
                track.seekTo(msec);
                Log.d(TAG_API, "Session: " + sessionId + ". SeekTo done");
            }
        }

        @Override
        public void setAudioStreamType(long sessionId, int streamtype) {

        }

        @Override
        public void setDataSourceString(long sessionId, String path) {
            Log.d(TAG_API, "Session: " + sessionId
                    + ". SetDataSourceString called");
            Track track;
            synchronized (this) {
                track = mTracks.get((int) sessionId);
            }
            track.setDataSourceString(path);

        }

        @Override
        public void setDataSourceUri(long sessionId, Uri uri) {
            Track track;
            synchronized (this) {
                track = mTracks.get((int) sessionId);
            }
            Log.d(TAG_API, "Session: " + sessionId
                    + ". setDataSourceUri called");
            track.setDataSourceUri(uri);
        }

        @Override
        public void setEnableSpeedAdjustment(long sessionId,
                                             boolean enableSpeedAdjustment) {

        }

        @Override
        public void setLooping(long sessionId, boolean looping) {

        }

        @Override
        public void setPitchStepsAdjustment(long sessionId, float pitchSteps) {

        }

        @Override
        public void setPlaybackPitch(long sessionId, float f) {
            Track track;
            synchronized (this) {
                track = mTracks.get((int) sessionId);
            }
            track.setPlaybackPitch(f);
        }

        @Override
        public void setPlaybackSpeed(long sessionId, float f) {
            Track track;
            synchronized (this) {
                track = mTracks.get((int) sessionId);
            }
            if (track != null) {
                track.setPlaybackSpeed(f);
            }
        }

        @Override
        public void setSpeedAdjustmentAlgorithm(long sessionId, int algorithm) {

        }

        @Override
        public void setVolume(long sessionId, float left, float right) {
            // The IPlayMedia_0_8.Stub defines this method but the presto client
            // never actually calls it. ortylp provided a reasonable
            // implementation just in case.
            Log.d(TAG_API, "Session: " + sessionId + ". Set volume to (" + left
                    + ", " + right + ")");
            Track track;
            synchronized (this) {
                track = mTracks.get((int) sessionId);
            }
            if (null != track) {
                track.setVolume(left, right);
            }
        }

        @Override
        public void start(long sessionId) {
            Log.d(TAG_API, "Session: " + sessionId + ". Start called");
            Track track;
            synchronized (this) {
                track = mTracks.get((int) sessionId);
            }
            //TODO track 有几率出现null的现象,后期优化考虑,是否在次服务中直接,发出error(注意,当track为null的时候,无法使用(track.errorCallback进行错误回调))
            if (track != null) {
                track.start();
                Log.d(TAG_API, "Session: " + sessionId + ". Start done");
            }
        }

        @Override
        public long startSession(IDeathCallback_0_8 cb) {
            Log.d(TAG_API, "Calling startSession");
            final int sessionId = trackId++;
            Log.d(TAG_API, "Registering new sessionId: " + sessionId);
            try {
                cb.asBinder().linkToDeath(new DeathRecipient() {
                    @Override
                    public void binderDied() {
                        Log.d(TAG_API, "Our caller is DEAD.  Releasing.");
                        handleRemoteException(sessionId);
                        return;
                    }
                }, 0);
            } catch (RemoteException e) {
                Log.wtf(TAG_API,
                        "Service died when trying to set what to do when it dies.  Good luck!",
                        e);
            }
            // It seems really strange to me to passing this callback to the
            // track since it never actually gets called or used by the track.
            // However, unless we 'do' something with it, cb will be a candidate
            // for garbage collection after this method pops
            synchronized (this) {
                mTracks.append(sessionId, new Track(SoundService.this, cb));
            }
            return sessionId;
        }

        @Override
        public void stop(long sessionId) {
            Log.d(TAG_API, "Session: " + sessionId + ". Stop called");
            Track track;
            synchronized (this) {
                track = mTracks.get((int) sessionId);
            }
            if (track != null) {
                track.stop();
                Log.d(TAG_API, "Session: " + sessionId + ". Stop done");
            }
        }

        @Override
        public void unregisterOnBufferingUpdateCallback(long sessionId,
                                                        IOnBufferingUpdateListenerCallback_0_8 cb) {
            Log.e("SoundService",
                    "In unregisterOnBufferingUpdateCallback. This should never happen!");
        }

        @Override
        public void unregisterOnCompletionCallback(long sessionId,
                                                   IOnCompletionListenerCallback_0_8 cb) {
            Log.e("SoundService",
                    "In unregisterOnCompletionCallback. This should never happen!");
        }

        @Override
        public void unregisterOnErrorCallback(long sessionId,
                                              IOnErrorListenerCallback_0_8 cb) {
            Log.e("SoundService",
                    "In unregisterOnErrorCallback. This should never happen!");
        }

        @Override
        public void unregisterOnInfoCallback(long sessionId,
                                             IOnInfoListenerCallback_0_8 cb) {
            Log.e("SoundService",
                    "In unregisterOnInfoCallback. This should never happen!");
        }

        @Override
        public void unregisterOnPitchAdjustmentAvailableChangedCallback(
                long sessionId,
                IOnPitchAdjustmentAvailableChangedListenerCallback_0_8 cb) {
            Log.e("SoundService",
                    "In unregisterOnPitchAdjustmentAvailableChangedCallback. This should never happen!");
        }

        @Override
        public void unregisterOnPreparedCallback(long sessionId,
                                                 IOnPreparedListenerCallback_0_8 cb) {
            Log.e("SoundService",
                    "In unregisterOnPreparedCallback. This should never happen!");
        }

        @Override
        public void unregisterOnSeekCompleteCallback(long sessionId,
                                                     IOnSeekCompleteListenerCallback_0_8 cb) {
            Log.e("SoundService",
                    "In unregisterOnSeekCompleteCallback. This should never happen!");
        }

        @Override
        public void unregisterOnSpeedAdjustmentAvailableChangedCallback(
                long sessionId,
                IOnSpeedAdjustmentAvailableChangedListenerCallback_0_8 cb) {
            Log.e("SoundService",
                    "In unregisterOnSpeedAdjustmentAvailableChangedCallback. This should never happen!");
        }

    };
}
