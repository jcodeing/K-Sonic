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

package com.jcodeing.k_sonic;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Settings {
    private Context appContext;
    private SharedPreferences sp;

    public Settings(Context context) {
        appContext = context.getApplicationContext();
        sp = PreferenceManager.getDefaultSharedPreferences(appContext);
    }

    // ------------------------------K------------------------------@Media Engine

    public int getMediaEngine() {
        return sp.getInt(appContext.getString(R.string.pref_key_media_engine), 1);
    }

    public void setMediaEngine(int engine) {
        sp.edit().putInt(appContext.getString(R.string.pref_key_media_engine), engine).apply();
    }

    // ------------------------------K------------------------------@Path

    public String getLastDirectory() {
        return sp.getString(appContext.getString(R.string.pref_key_last_directory), "/");
    }

    public void setLastDirectory(String path) {
        sp.edit().putString(appContext.getString(R.string.pref_key_last_directory), path).apply();
    }

    public String getLastFile() {
        return sp.getString(appContext.getString(R.string.pref_key_last_file), "");
    }

    public void setLastFile(String path) {
        sp.edit().putString(appContext.getString(R.string.pref_key_last_file), path).apply();
    }

    // ------------------------------K------------------------------@Sonic

    public float getSonicSpeed() {
        return sp.getFloat(appContext.getString(R.string.pref_key_sonic_speed), 1.3f);
    }

    public void setSonicSpeed(float speed) {
        sp.edit().putFloat(appContext.getString(R.string.pref_key_sonic_speed), speed).apply();
    }

    public float getSonicPitch() {
        return sp.getFloat(appContext.getString(R.string.pref_key_sonic_pitch), 1.3f);
    }

    public void setSonicPitch(float pitch) {
        sp.edit().putFloat(appContext.getString(R.string.pref_key_sonic_pitch), pitch).apply();
    }

    public float getSonicRate() {
        return sp.getFloat(appContext.getString(R.string.pref_key_sonic_rate), 1.3f);
    }

    public void setSonicRate(float rate) {
        sp.edit().putFloat(appContext.getString(R.string.pref_key_sonic_rate), rate).apply();
    }
}
