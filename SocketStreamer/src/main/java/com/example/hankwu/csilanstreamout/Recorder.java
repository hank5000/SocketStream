package com.example.hankwu.csilanstreamout;

import android.annotation.TargetApi;
import android.media.MediaRecorder;
import android.util.Log;
import android.view.Surface;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;

/**
 * Created by HankWu_Office on 2016/1/15.
 */

public class Recorder {
    static public Recorder recorder = new Recorder();
    public MediaRecorder[] mMediaRecorders = null;

    String recordPath;
    boolean bInit;
    boolean bStart;
    long mTime;
    static int width = 2048;
    static int height = 1152;
    int mainRecordIndex = 0;

    int deletePercentage = 25;
    int periodicalTime = 30 ;// second

    public void setRecordPath(String recordPath) {
        this.recordPath = recordPath+"/";
    }

    public void initialize() {
        mMediaRecorders = new MediaRecorder[2];

        bInit = false;
        bStart = false;
        mTime = 0;

        //recordPath = VIAManager.getInstance().getFourInOneRecordPath()+"/"+VIAManager.getInstance().getFourInOnePrefix()+"-";
    }

    @TargetApi(21)
    public Surface getMainRecorderSurface() {
        return mMediaRecorders[mainRecordIndex].getSurface();
    }

    public static int getWidth() {
        return width;
    }

    public static int getHeight() {
        return height;
    }

    public static Recorder getRecorder() {
        return recorder;
    }

    public void createMainRecord() throws IOException {
        int i = mainRecordIndex;

        mMediaRecorders[i] = new MediaRecorder();
        mMediaRecorders[i].setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorders[i].setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

        Calendar c = Calendar.getInstance();
        String year = String.valueOf(c.get(Calendar.YEAR));
        String month = String.format("%02d", (c.get(Calendar.MONTH)+1));
        String day = String.format("%02d", c.get(Calendar.DAY_OF_MONTH));
        String hr = String.format("%02d", c.get(Calendar.HOUR_OF_DAY));
        String minute = String.format("%02d", c.get(Calendar.MINUTE));
        String second = String.format("%02d", c.get(Calendar.SECOND));
        String save_path = recordPath+year+month+day+"_"+hr+minute+second+".mp4";

        // Initialize the counter or time
        mTime = System.currentTimeMillis();

        mMediaRecorders[i].setOutputFile(save_path);
        mMediaRecorders[i].setVideoEncodingBitRate(80000);
        mMediaRecorders[i].setVideoFrameRate(30);
        mMediaRecorders[i].setVideoSize(width, height);
        mMediaRecorders[i].setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorders[i].setPreviewDisplay(null);

        mMediaRecorders[i].prepare();

        bStart = false;
        bInit = true;
    }

    public void destroy(final int i) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(mMediaRecorders!=null) {
                    if(mMediaRecorders[i]!=null) {
                        try {
                            mMediaRecorders[i].stop();
                            mMediaRecorders[i].release();
                            mMediaRecorders[i] = null;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();
    }


    public void recreate() throws IOException {

        destroy(mainRecordIndex);

        if(mainRecordIndex==0) {
            mainRecordIndex = 1;
        } else {
            mainRecordIndex = 0;
        }
        int i = mainRecordIndex;

        mMediaRecorders[i] = new MediaRecorder();

        mMediaRecorders[i].setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorders[i].setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

        Calendar c = Calendar.getInstance();
        String year = String.valueOf(c.get(Calendar.YEAR));
        String month = String.format("%02d", (c.get(Calendar.MONTH)+1));
        String day = String.format("%02d", c.get(Calendar.DAY_OF_MONTH));
        String hr = String.format("%02d", c.get(Calendar.HOUR_OF_DAY));
        String minute = String.format("%02d", c.get(Calendar.MINUTE));
        String second = String.format("%02d", c.get(Calendar.SECOND));
        String save_path = recordPath+year+month+day+"_"+hr+minute+second+".mp4";

        // Initialize the counter or time
        mTime = System.currentTimeMillis();

        mMediaRecorders[i].setOutputFile(save_path);
        mMediaRecorders[i].setVideoEncodingBitRate(80000);
        mMediaRecorders[i].setVideoFrameRate(30);
        mMediaRecorders[i].setVideoSize(width, height);
        mMediaRecorders[i].setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorders[i].setPreviewDisplay(null);

        mMediaRecorders[i].prepare();

        bStart = false;
        bInit = true;
    }

    public String getSizeString(int size) {
        switch (size) {
            case 0:
                return "Bytes";
            case 1:
                return "KBytes";
            case 2:
                return "MBytes";
            case 3:
                return "GBytes";
            case 4:
                return "TBytes";

        }
        return "?B";
    }

    public boolean checkTime() {
        if((System.currentTimeMillis()-mTime)>1000*periodicalTime) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    String rootPath = recordPath;
                    File f = new File(recordPath);
                    long usable = f.getUsableSpace();
                    long total  = f.getTotalSpace();
                    long occupy = total-usable;

                    long t = total;
                    long o = occupy;

                    float occ = occupy;
                    float tot = total;

                    int Ocounter = 0;
                    int Tcounter = 0;

                    while(occ>1024) {
                        occ /= 1024;
                        Ocounter++;
                    }
                    while (tot>1024) {
                        tot /= 1024;
                        Tcounter++;
                    }

                    float usedPercentage = ((float)(o*100)/(float)t);
                    Log.d("HANK","================Disk Information====================");
                    Log.d("HANK","= Disk   Path  : "+rootPath);
                    Log.d("HANK","= Total  Size  : "+tot+getSizeString(Tcounter));
                    Log.d("HANK","= Used   Size  : "+occ+getSizeString(Ocounter));
                    Log.d("HANK","= Used/Total % : "+usedPercentage+"%");
                    Log.d("HANK","================Disk Information End================");

                    if (usedPercentage>deletePercentage) {
                        Log.d("HANK","===== Disk Used > "+deletePercentage+"% =====");

                        /*
                            delete a file
                         */
                        File fourInOnePathFile = new File(recordPath);
                        File[] files = fourInOnePathFile.listFiles();
                        Arrays.sort(files, new Comparator() {
                            public int compare(Object o1, Object o2) {
                                if (((File) o1).lastModified() > ((File) o2).lastModified()) {
                                    return +1;
                                } else if (((File) o1).lastModified() < ((File) o2).lastModified()) {
                                    return -1;
                                } else {
                                    return 0;
                                }
                            }
                        });

                        int deleteCounter = 0;
                        while(usedPercentage>deletePercentage) {
                            if(files.length==0) {
                                Log.d("VIARecorder","The disk is almost full, but there has no file in fourinone folder!!!");
                                Log.d("VIARecorder","Force stop record!!");
                                break;
                            }
                            if(files[deleteCounter].delete()) {
                                deleteCounter++;
                                Log.d("HANK","Delete File Success =>"+files[deleteCounter].getAbsolutePath());
                            } else {
                                Log.d("HANK","something wrong when delete file =>"+files[deleteCounter].getAbsolutePath());
                            }

                            f = new File(rootPath);
                            usable = f.getUsableSpace();
                            t = f.getTotalSpace();
                            o = t - usable;
                            usedPercentage = ((float) (o * 100) / (float) t);
                        }
                    }
                }
            }).start();
            return true;
        }
        return false;
    }

    @TargetApi(21)
    public Surface getSurface() {
        return mMediaRecorders[0].getSurface();
    }

    public boolean isInitialized()
    {
        return bInit;
    }

    public boolean isStarted() {
        return bStart;
    }

    public void start() {
        if (!bStart && bInit) {
            mMediaRecorders[mainRecordIndex].start();
            bStart = true;
        }
    }

    public void stopAll() {
        for(int i=0;i<mMediaRecorders.length;i++) {
            stop(i);
        }
    }

    public void stop(int i) {
        if(bInit && bStart) {
            if(mMediaRecorders[i]!=null) {
                try {
                    mMediaRecorders[i].stop();
                    mMediaRecorders[i].release();
                    mMediaRecorders[i] = null;
                    bStart = false;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }




}
