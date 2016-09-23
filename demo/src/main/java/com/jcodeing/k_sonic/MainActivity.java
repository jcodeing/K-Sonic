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

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.aocate.media.MediaPlayer;
import com.jcodeing.k_sonic.explorer.FileExplorerActivity;
import com.jcodeing.library_exo.ExoMediaPlayer;
import com.jcodeing.library_exo.IMediaPlayer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

public class MainActivity extends AppCompatActivity implements IMediaPlayer.OnPreparedListener, PlusMinusNum.OnNumChangeListener, View.OnClickListener, IMediaPlayer.OnErrorListener, IMediaPlayer.OnCompletionListener {

    private Settings mSettings;
    private IMediaPlayer mediaPlayer;
    private int mediaEngine;

    private TextView audioPathTv;
    private TextView mediaEngineTv;

    private FloatingActionButton fab;

    // ------------------------------K------------------------------@Override
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // =========@Init@=========
        if (mSettings == null)
            mSettings = new Settings(this);

        audioPathTv = (TextView) findViewById(R.id.tv_audio_path);
        audioPathTv.setOnClickListener(this);
        if (!TextUtils.isEmpty(mSettings.getLastFile()))
            audioPathTv.setText(mSettings.getLastFile());


        mediaEngineTv = (TextView) findViewById(R.id.tv_media_engine);

        PlusMinusNum speedPmn = (PlusMinusNum) findViewById(R.id.pmn_speed);
        speedPmn.setOnNumChangeListener(this);
        speedPmn.setNum(speed = mSettings.getSonicSpeed());
        PlusMinusNum pitchPmn = (PlusMinusNum) findViewById(R.id.pmn_pitch);
        pitchPmn.setOnNumChangeListener(this);
        pitchPmn.setNum(pitch = mSettings.getSonicPitch());
        PlusMinusNum ratePmn = (PlusMinusNum) findViewById(R.id.pmn_rate);
        ratePmn.setOnNumChangeListener(this);
        ratePmn.setNum(rate = mSettings.getSonicRate());

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(this);

        // =========@Init Media@=========
        switchMediaEngine(null);

        // =========@intent-filter android.intent.action.VIEW@=========
        Intent intent = getIntent();
        String intentAction = intent.getAction();
        if (!TextUtils.isEmpty(intentAction)) {
            if (intentAction.equals(Intent.ACTION_VIEW)) {
                goPlay(intent.getDataString());
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //update audioPath TextView
        if (audioPathTv != null && !TextUtils.isEmpty(mSettings.getLastFile()))
            audioPathTv.setText(mSettings.getLastFile());
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mediaPlayer != null && mediaPlayer.isPlaying())
            mediaPlayer.stop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_audio_path:
                startActivity(new Intent(this, FileExplorerActivity.class));
                break;
            case R.id.fab:
                goPlay(audioPathTv.getText().toString());
                break;
        }
    }

    @Override
    public void onPrepared(IMediaPlayer mp) {
        mediaPlayer.setSonicSpeed(speed);
        mediaPlayer.setSonicPitch(pitch);
        mediaPlayer.setSonicRate(rate);

        mp.start();
    }

    @Override
    public boolean onError(IMediaPlayer mp, int what, int extra) {
        showSnackbar("onError : " + what + "#" + extra);
        return false;
    }

    @Override
    public void onCompletion(IMediaPlayer mp) {
        showSnackbar("Play Completion");
    }


    private float speed;
    private float pitch;
    private float rate;

    @Override
    public void onNumChange(View view, float num) {
        if (view.getId() == R.id.pmn_speed) {
            mSettings.setSonicSpeed(speed = num);
            mediaPlayer.setSonicSpeed(num);
        } else if (view.getId() == R.id.pmn_pitch) {
            mSettings.setSonicPitch(pitch = num);
            mediaPlayer.setSonicPitch(num);
        } else if (view.getId() == R.id.pmn_rate) {
            mSettings.setSonicRate(rate = num);
            mediaPlayer.setSonicRate(num);
        }
    }

    // ------------------------------K------------------------------@Media

    //Button switch_engine android:onClick="switchMediaEngine"
    public void switchMediaEngine(View v) {
        // =========@Init mediaEngine value@=========
        if (v == null)
            mediaEngine = mSettings.getMediaEngine();
        else
            mediaEngine = mediaEngine == 1 ? 2 : 1;

        // =========@Init MediaPlayer@=========
        if (mediaPlayer != null)
            mediaPlayer.stop();

        if (mediaEngine == 1) {
            mediaPlayer = new ExoMediaPlayer(this);
            if (mediaEngineTv != null)
                mediaEngineTv.setText(getString(R.string.media_engine_exo));
        } else {//2
            mediaPlayer = new MediaPlayer(this);
            if (mediaEngineTv != null)
                mediaEngineTv.setText(getString(R.string.media_engine_presto));
        }

        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnCompletionListener(this);

        // =========@save mediaEngine value@=========
        mSettings.setMediaEngine(mediaEngine);
    }


    public void goPlay(String path) {
        try {
            if (mediaPlayer.isPlaying())
                mediaPlayer.stop();
            mediaPlayer.reset();

            if (!TextUtils.isEmpty(path)) {
                mediaPlayer.setDataSource(path);
                mediaPlayer.prepareAsync();
                showSnackbar("goPlayAudioPath...");
            } else {
                goPlayDemo();
                showSnackbar("goPlayDemo...");
            }
        } catch (Exception e) {//IO
            e.printStackTrace();
        }
    }


    String pathDemo;

    public void goPlayDemo() {

        // =========@verify Demo File@=========
        if (TextUtils.isEmpty(pathDemo))
            pathDemo = getDir("demo", MODE_PRIVATE).getPath() + File.separator + "demo";

        File fileDemo = new File(pathDemo);
        if (!fileDemo.exists()) {
            FileChannel inputChannel = null;
            FileChannel outputChannel = null;
            try {
                //Be careful: getFileDescriptor() file size *2 > original file size problem
                inputChannel = new FileInputStream(getAssets().openFd("demo").getFileDescriptor()).getChannel();
                outputChannel = new FileOutputStream(fileDemo).getChannel();
                outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
            } catch (Exception e) {//IO
                e.printStackTrace();
            } finally {
                try {
                    if (inputChannel != null)
                        inputChannel.close();
                    if (outputChannel != null)
                        outputChannel.close();
                } catch (Exception e) {//IO
                    e.printStackTrace();
                }
            }
        }

        // =========@Play Demo File@=========
        try {
            mediaPlayer.setDataSource(pathDemo);
            mediaPlayer.prepareAsync();
        } catch (Exception e) {//IO
            e.printStackTrace();
        }
    }


    // ------------------------------K------------------------------@Assist
    public void showSnackbar(@NonNull CharSequence text) {
        if (fab != null && !TextUtils.isEmpty(text))
            Snackbar.make(fab, text, Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
    }


}
