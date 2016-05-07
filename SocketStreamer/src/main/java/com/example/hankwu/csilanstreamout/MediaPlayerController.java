package com.example.hankwu.csilanstreamout;

import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;

/**
 * Created by HankWu on 16/1/9.
 */
public class MediaPlayerController {
    public static MediaPlayerController mediaPlayerControllerSingleton = new MediaPlayerController();
    private MediaPlayer[] mps = null;
    private int number_of_play = 0;
    private HandlerThread ht = null;
    private Handler h = null;
    public void setSurfaceTextures(SurfaceTexture[] sts) {
        number_of_play = sts.length;
        mps = new MediaPlayer[number_of_play];

        ht = new HandlerThread("onFrameAvailable");
        ht.start();
        h = new Handler(ht.getLooper());


        for(int i=0;i<number_of_play;i++) {
            mps[i] = new MediaPlayer();

            Surface surface = new Surface(sts[i]);

            //sts[i].setOnFrameAvailableListener(new FrameAvailable(i), h);
            mps[i].setSurface(surface);

            surface.release();
        }
    }

    public void setDataSources(String[] ss) throws IOException {
        for(int i=0;i<number_of_play;i++) {
            if(ss[i].equalsIgnoreCase("") || ss[i]==null) {
                mps[i] = null;
            } else {
                Log.d("HANK", ss[i]);
                mps[i].setDataSource(ss[i]);
            }
        }
    }

boolean[] bPrepared = new boolean[4];

    class PreparedListenerr implements MediaPlayer.OnPreparedListener {
        int index = -1;
        PreparedListenerr(int i) {
            index = i;
            bPrepared[i] = false;
        }
        @Override
        public void onPrepared(MediaPlayer mediaPlayer) {
            bPrepared[index] = true;
            if(bPrepared[0] && bPrepared[1]) {
                MediaPlayerController.mediaPlayerControllerSingleton.start();
            }
        }
    }

    public void prepare() throws IOException {
        for(int i=0;i<number_of_play;i++) {
            if(mps[i]!=null) {
                mps[i].setOnPreparedListener(new PreparedListenerr(i));
                mps[i].prepare();
            }
        }
    }

    public void start() {
        for(int i=0;i<number_of_play;i++) {
            if(mps[i]!=null)
                mps[i].start();
        }
    }

    public void stop() {
        for(int i=0;i<number_of_play;i++) {
            mps[i].stop();
        }
    }

    public void release() {
        for(int i=0;i<number_of_play;i++) {
            mps[i].release();
            mps[i] = null;
        }
    }

    public void pause() {
        for(int i=0;i<number_of_play;i++) {
            mps[i].pause();
        }
    }

    public void seekToZero() {
        for(int i=0;i<number_of_play;i++) {
            mps[i].seekTo(0);
        }
    }

    public boolean checkAllAvailable() {
        for(int i=0;i<number_of_play;i++) {
            if(!frameAvailable[i])
                return false;
        }

        for(int i=0;i<number_of_play;i++) {
            frameAvailable[i] = false;
        }

        return true;
    }


    boolean[] frameAvailable = new boolean[4];
    public boolean bCanPlay = false;

    class FrameAvailable implements SurfaceTexture.OnFrameAvailableListener {

        int numberSurface = -1;

        FrameAvailable(int num) {
            numberSurface = num;
        }
        @Override
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
            frameAvailable[numberSurface] = true;
        }
    }

}
