package com.example.hankwu.socketrecevier;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.media.MediaMuxer;
import android.media.MediaPlayer;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;

public class MainActivity extends Activity {

    SurfaceView sv = null;
    Button btn = null;
    VideoSurfaceView mVideoSurfaceView = null;
    boolean bShow = true;
    int numberShow = 0;
    boolean bStart = false;
    VideoThread vt = null;
    String ipAddress = "192.168.12.108";
    MediaMuxer mediaMuxer = null;

    Bitmap red = null;
    Bitmap white = null;
    Bitmap yellow = null;

    DatagramSocket dataSocket = null;
    int btnNumber = 6;
    Button[] btnArrary = new Button[btnNumber];


    public void setButtonVisiable(int visiable) {
        for(int i=0;i<btnNumber;i++) {
            btnArrary[i].setVisibility(visiable);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();


    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        //Remove notification bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);


        red = BitmapFactory.decodeResource(this.getResources(),
                R.drawable.red_grid);

        white = BitmapFactory.decodeResource(this.getResources(),
                R.drawable.white_grid);

        yellow = BitmapFactory.decodeResource(this.getResources(),
                R.drawable.yellow_grid);

        LinearLayout ll = (LinearLayout) findViewById(R.id.hank);
        mVideoSurfaceView = new VideoSurfaceView(this);
        ll.addView(mVideoSurfaceView);

        final Button ipBtn =  (Button)findViewById(R.id.btnIp);
        Button btnA = ((Button)findViewById(R.id.btnA));
        Button btn1 = ((Button)findViewById(R.id.btn1));
        Button btn2 = ((Button)findViewById(R.id.btn2));
        Button btn3 = ((Button)findViewById(R.id.btn3));
        Button btn4 = ((Button)findViewById(R.id.btn4));
        Button btn5 = ((Button)findViewById(R.id.btn5));
        int i =0;
        btnArrary[i++] = btnA;
        btnArrary[i++] = btn1;
        btnArrary[i++] = btn2;
        btnArrary[i++] = btn3;
        btnArrary[i++] = btn4;
        btnArrary[i++] = btn5;

        setButtonVisiable(View.INVISIBLE);

        ((Button) findViewById(R.id.btnIp)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder a = new AlertDialog.Builder(MainActivity.this);
                final EditText et = new EditText(MainActivity.this);
                et.setText(ipAddress);
                a.setTitle("Set 5880 Ip Address");
                a.setView(et)
                .setPositiveButton("Enter", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ipAddress = et.getText().toString();
                    }
                }).create().show();
            }
        });

        btn = (Button)findViewById(R.id.btn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(!bStart) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            socket = new Socket();
                            InetSocketAddress isa = new InetSocketAddress(ipAddress, 1234);
                            try {
                                socket.connect(isa, 10000);
                                InputStream s = socket.getInputStream();

                                while (true) {
                                    byte[] bb = new byte[128];
                                    int readed = s.read(bb);
                                    if (readed > 0) {
                                        String[] splites = (new String(bb)).substring(0, readed).split(":");
                                        int w = Integer.valueOf(splites[0]);
                                        int h = Integer.valueOf(splites[1]);
                                        byte[] sps = hexStringToByteArray("00000001" + splites[2]);
                                        byte[] pps = hexStringToByteArray("00000001" + splites[3]);
                                        rawDataSocket = new Socket();

                                        InetSocketAddress isaa = new InetSocketAddress(ipAddress, 1236);

                                        rawDataSocket.connect(isaa, 10000);
                                        is = rawDataSocket.getInputStream();

                                        vt = new VideoThread(MainActivity.this, 0, mVideoSurfaceView.getSurface(), "video/avc", w, h, "00000001" + splites[2], "00000001" + splites[3], is, "socket");
                                        vt.start();
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                btn.setText("Stop");
                                                ipBtn.setVisibility(View.INVISIBLE);
                                                setButtonVisiable(View.VISIBLE);

                                                bStart = true;
                                            }
                                        });

                                    }
                                }
                            } catch (java.io.IOException e) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(MainActivity.this, "5880("+ipAddress+") is not online ", Toast.LENGTH_LONG).show();
                                    }
                                });
                                Log.d("SSS", "Something wrong");
                            }
                        }
                    }).start();
                } else {
                    ipBtn.setVisibility(View.VISIBLE);
                    setButtonVisiable(View.INVISIBLE);
                    btn.setText("Start");
                    bStart = false;
                    vt.setStop();
                    vt.interrupt();
                    try {
                        vt.join();
                        vt = null;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    try {
                        is.close();
                        is = null;
                        rawDataSocket = null;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        View.OnClickListener btnClick = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (view.getId()) {
                    case R.id.btnA :
                        Global.component = -1;
                        break;
                    case R.id.btn1:
                        Global.component = 0;
                        break;
                    case R.id.btn2:
                        Global.component = 1;
                        break;
                    case R.id.btn3:
                        Global.component = 2;
                        break;
                    case R.id.btn4:
                        Global.component = 3;
                        break;
                    case R.id.btn5:
                        if(numberShow==0) {
                            ((ImageView) findViewById(R.id.image)).setAlpha(1.0f);
                            //((ImageView) findViewById(R.id.image)).setBackgroundResource(R.drawable.red_grid);
                            ((ImageView) findViewById(R.id.image)).setImageBitmap(red);
                            ((ImageView) findViewById(R.id.image)).setScaleType(ImageView.ScaleType.FIT_XY);


                            numberShow = 1;
                        } else if (numberShow==1) {
                            ((ImageView) findViewById(R.id.image)).setAlpha(1.0f);

                            //((ImageView) findViewById(R.id.image)).setBackgroundResource(R.drawable.yellow_grid);
                            ((ImageView) findViewById(R.id.image)).setImageBitmap(yellow);
                            ((ImageView) findViewById(R.id.image)).setScaleType(ImageView.ScaleType.FIT_XY);


                            numberShow = 2;
                        } else if (numberShow==2) {
                            ((ImageView) findViewById(R.id.image)).setAlpha(1.0f);

                            ((ImageView) findViewById(R.id.image)).setImageBitmap(white);
                            ((ImageView) findViewById(R.id.image)).setScaleType(ImageView.ScaleType.FIT_XY);
                            numberShow = 3;
                        } else if (numberShow==3) {
                            ((ImageView) findViewById(R.id.image)).setAlpha(.0f);
                            numberShow = 0;
                        }

                        break;
                }
            }
        };

        ((Button)findViewById(R.id.btnA)).setOnClickListener(btnClick);
        ((Button)findViewById(R.id.btn1)).setOnClickListener(btnClick);
        ((Button)findViewById(R.id.btn2)).setOnClickListener(btnClick);
        ((Button)findViewById(R.id.btn3)).setOnClickListener(btnClick);
        ((Button)findViewById(R.id.btn4)).setOnClickListener(btnClick);
        ((Button)findViewById(R.id.btn5)).setOnClickListener(btnClick);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    Socket socket = null;


    public byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    Socket rawDataSocket = null;
    InputStream is = null;
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_settings) {

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
