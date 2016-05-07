package com.example.hankwu.csilanstreamout;

import android.app.Activity;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    VideoSurfaceView mVideoSurfaceView = null;
    MainActivity act = this;
    TextView tv = null;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        // Add GLSurfaceView to Activity
        LinearLayout ll = (LinearLayout) findViewById(R.id.hank);
        mVideoSurfaceView = new VideoSurfaceView(this);
        ll.addView(mVideoSurfaceView);

        tv = (TextView)findViewById(R.id.textView);
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
    }
}
