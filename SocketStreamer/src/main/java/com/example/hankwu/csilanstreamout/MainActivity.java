package com.example.hankwu.csilanstreamout;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;

public class MainActivity extends Activity {
    VideoSurfaceView mVideoSurfaceView = null;
    MainActivity act = this;
    TextView tv = null;
    LinearLayout ll = null;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        // Add GLSurfaceView to Activity
        ll = (LinearLayout) findViewById(R.id.hank);

//        mVideoSurfaceView = new VideoSurfaceView(MainActivity.this);
//        ll.addView(mVideoSurfaceView);
//
//        tv = (TextView) findViewById(R.id.textView);
//        tv.setText(Utils.getIPAddress(true));


        if(Utils.bRecorder) {
            showInputPathDialog();
        } else {
            addVideoSurfaceView(ll);
        }
    }

    private void showInputPathDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        final EditText et = new EditText(MainActivity.this);
        builder.setTitle("Input Record Path");
        builder.setView(et);
        builder.setPositiveButton("Enter", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String path = et.getText().toString();
                File f = new File(path);
                if(!f.exists()) {
                    AlertDialog.Builder builder2 = new AlertDialog.Builder(MainActivity.this);
                    builder2.setTitle("please input path is not correct").setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            showInputPathDialog();
                        }
                    }).create().show();
                } else {
                    Recorder.getRecorder().setRecordPath(path);
                    addVideoSurfaceView(ll);
                }
            }
        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showInputPathDialog();
            }
        }).create().show();
    }

    private void addVideoSurfaceView(final LinearLayout ll) {

                mVideoSurfaceView = new VideoSurfaceView(MainActivity.this);
                ll.addView(mVideoSurfaceView);

                tv = (TextView) findViewById(R.id.textView);
                tv.setText(Utils.getIPAddress(true));

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
    protected void onPause() {
        MediaPlayerController.mediaPlayerControllerSingleton.stop();
        MediaPlayerController.mediaPlayerControllerSingleton.release();
        super.onPause();

        if(Utils.bRecorder) {
            Recorder.getRecorder().stopAll();
        }
    }
}
