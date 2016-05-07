package com.example.hankwu.csilanstreamout;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;

/**
 * Created by HankWu on 16/3/29.
 */
public class Encoder {
    private static Encoder encoder = new Encoder();
    private MediaCodec mEncoder;
    public Surface mEncodeSurface;
    private MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

    ServerSocket serverSocket = null;
    ServerSocket ss2 = null;
    Socket[] rawDataClients = new Socket[4];
    OutputStream[] outputStreams = new OutputStream[4];

    DatagramSocket datagramSocket = null;

    private int framerate = 24;

    public void create() {
        try {
            mEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        } catch (Exception e) {
            e.printStackTrace();
        }

        MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC,GlobalInfo.width,GlobalInfo.height);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE,8000);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE,framerate);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10);

        mEncoder.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        mEncodeSurface = mEncoder.createInputSurface();
        mEncoder.start();

        new Thread(socket_server1).start();

        if(GlobalInfo.bUDP) {
            try {
                datagramSocket = new DatagramSocket(1236);
                datagramSocket.setBroadcast(true);
            } catch (SocketException e) {

            }
        } else {
            new Thread(socket_server2).start();
        }

    }

    private byte[] sps;
    private byte[] pps;

    static public Encoder getEncoder() { return encoder; }
    public Surface getsurface() { return mEncodeSurface; }

    public void streamOut() {

        int outputBufferIdx = mEncoder.dequeueOutputBuffer(bufferInfo,0);
        if(outputBufferIdx>=0) {

            ByteBuffer outBuffer = mEncoder.getOutputBuffers()[outputBufferIdx];

            if(bufferInfo.size>0) {
                byte[] outData = new byte[bufferInfo.size];
                outBuffer.get(outData);
                if (sps != null && pps != null) {

                    if(GlobalInfo.bUDP) {
                        DatagramPacket packet = new DatagramPacket(outData,outData.length);
                        try {
                            datagramSocket.send(packet);
                        } catch (IOException e) {
                            Log.d("Encode","Send Packet fail");
                        }
                    } else {
                        // Send Raw Data From Socket OutputStream
                        for (int i = 0; i < 4; i++) {
                            if (rawDataClients[i] != null && outputStreams[i] != null) {
                                try {
                                    outputStreams[i].write(outData);
                                } catch (IOException e) {
                                    outputStreams[i] = null;
                                    rawDataClients[i] = null;
                                    Log.d("Encode", "Write to OutputStream Something wrong");
                                    e.printStackTrace();
                                }
                            }
                        }
                    }

                } else {
                    ByteBuffer spsPpsBuffer = ByteBuffer.wrap(outData);
                    if (spsPpsBuffer.getInt() == 0x00000001) {
                        System.out.println("parsing sps/pps");
                    } else {
                        System.out.println("something is amiss?");
                    }

                    int ppsIndex = 0;
                    while(!(spsPpsBuffer.get() == 0x00 && spsPpsBuffer.get() == 0x00 && spsPpsBuffer.get() == 0x00 && spsPpsBuffer.get() == 0x01)) {

                    }
                    ppsIndex = spsPpsBuffer.position();
                    sps = new byte[ppsIndex - 8];
                    System.arraycopy(outData, 4, sps, 0, sps.length);
                    pps = new byte[outData.length - ppsIndex];
                    System.arraycopy(outData, ppsIndex, pps, 0, pps.length);
                    // get sps pps
                }
                mEncoder.releaseOutputBuffer(outputBufferIdx,false);
            }
        }
    }

    Socket client = null;

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    private Runnable socket_server1 = new Runnable(){
        public void run(){
            try{
                serverSocket = new ServerSocket(1234);
                while (true) {
                    client = serverSocket.accept();
                    boolean bWait = true;

                    if(findNullSlot()==-1) {
                        client.getOutputStream().write("full".getBytes());
                        client = null;
                        bWait = false;
                    }

                    while(bWait) {
                        try {
                            String sendString = GlobalInfo.width+":"+GlobalInfo.height+":"+bytesToHex(sps)+":"+bytesToHex(pps);
                            client.getOutputStream().write(sendString.getBytes());
                            client = null;
                            bWait = false;
                        } catch (Exception e) {
                            Log.d("ServerSocket", "Send fail");
                            client = null;
                            bWait = false;
                        }
                    }
                }
            }catch(IOException e){
                client = null;
                Log.d("ServerSocket","connect fail");
            }
        }
    };


    private int findNullSlot() {
        for(int i=0;i<4;i++) {
            if(rawDataClients[i] == null) {
                return i;
            }
        }
        return -1;
    }


    private Runnable socket_server2 = new Runnable(){
        public void run(){
            while(true) {
                try {
                    ss2 = new ServerSocket(1236);

                    while(true) {
                        int emptySlot = findNullSlot();
                        if(emptySlot!=-1) {
                            try {
                                rawDataClients[emptySlot] = ss2.accept();
                                outputStreams[emptySlot] = rawDataClients[emptySlot].getOutputStream();
                            } catch (IOException e) {
                                rawDataClients[emptySlot] = null;
                                outputStreams[emptySlot] = null;
                            }
                        } else {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } catch (IOException e) {
                    Log.d("ServerSocket", "connect fail");
                }
            }
        }
    };

}
