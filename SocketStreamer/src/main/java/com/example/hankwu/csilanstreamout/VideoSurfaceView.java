package com.example.hankwu.csilanstreamout;

import java.io.IOException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;



@SuppressLint("ViewConstructor")
class VideoSurfaceView extends GLSurfaceView {

    VideoRender mRenderer;
    static public int number_of_play = 1;

    static public WindowSurfaceFactory windowSurfaceFactory = new WindowSurfaceFactory();
    static public ContextFactory contextFactory = new ContextFactory();


    //static public HandlerThread ht = null;
    //static public Handler h = null;

    public VideoSurfaceView(Context context) {
        super(context);

        setEGLContextClientVersion(2);
        this.setPreserveEGLContextOnPause(true);

        mRenderer = new VideoRender(context);
        this.setEGLContextFactory(contextFactory);
        this.setEGLWindowSurfaceFactory(windowSurfaceFactory);

        setRenderer(mRenderer);
        this.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    @Override
    public void onResume() {

        super.onResume();
    }

    private static class VideoRender
            implements GLSurfaceView.Renderer {
        private static String TAG = "VideoRender";

        private static final int FLOAT_SIZE_BYTES = 4;
        private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES;
        private static final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;
        private static final int TRIANGLE_VERTICES_DATA_UV_OFFSET = 3;
        private final float[] mTriangleVerticesData = {
                // X, Y, Z, U, V
                -1.0f, -1.0f, 0, 0.f, 0.f,
                1.0f, -1.0f, 0, 1.f, 0.f,
                -1.0f,  1.0f, 0, 0.f, 1.f,
                1.0f,  1.0f, 0, 1.f, 1.f,
        };


        private FloatBuffer mTriangleVertices;
        private FloatBuffer[] mTriangleVerticesPart = new FloatBuffer[4];


        private FloatBuffer tmpTriangleVertices;


        private final String mVertexShader =
                "uniform mat4 uMVPMatrix;\n" +
                        "uniform mat4 uSTMatrix;\n" +
                        "attribute vec4 aPosition;\n" +
                        "attribute vec4 aTextureCoord;\n" +
                        "varying vec2 vTextureCoord;\n" +
                        "void main() {\n" +
                        "  gl_Position = uMVPMatrix * aPosition;\n" +
                        "  vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n" +
                        "}\n";

        private final String mFragmentShader =
                "#extension GL_OES_EGL_image_external : require\n" +
                        "precision mediump float;\n" +
                        "varying vec2 vTextureCoord;\n" +
                        "uniform samplerExternalOES sTexture;\n" +
                        "void main() {\n" +
                        "  gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
                        "}\n";

        private float[] mMVPMatrix = new float[16];
        private float[] mSTMatrix = new float[16];
        private int[] textures = new int[number_of_play];

        private int mProgram;
        private int mTextureID;
        private int muMVPMatrixHandle;
        private int muSTMatrixHandle;
        private int maPositionHandle;
        private int maTextureHandle;

        SurfaceTexture[] mSurfaceTextures = new SurfaceTexture[number_of_play];

        private static int GL_TEXTURE_EXTERNAL_OES = 0x8D65;

        //TODO: Think view port, maybe can use real resolution to set X,Y,Z position
        //TODO: Maybe it doesn't need to scale to min -1 -1 0 max 1 1 0.

        public VideoRender(Context context) {
            mTriangleVertices = ByteBuffer.allocateDirect(
                    mTriangleVerticesData.length * FLOAT_SIZE_BYTES)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();

            mTriangleVertices.put(mTriangleVerticesData).position(0);

            final float[] mTriangleVerticesData1 = {
                    // X, Y, Z, U, V
                    -1.0f, -1.0f, 0, 0.f, 0.5f,
                    1.0f, -1.0f, 0, 0.5f, 0.5f,
                    -1.0f,  1.0f, 0, 0.f, 1.f,
                    1.0f,  1.0f, 0, 0.5f, 1.f,
            };
            final float[] mTriangleVerticesData2 = {
                    // X, Y, Z, U, V
                    -1.0f, -1.0f, 0, 0.5f, 0.5f,
                    1.0f, -1.0f, 0, 1.f, 0.5f,
                    -1.0f,  1.0f, 0, 0.5f, 1.f,
                    1.0f,  1.0f, 0, 1.f, 1.f,
            };
            final float[] mTriangleVerticesData3 = {
                    // X, Y, Z, U, V
                    -1.0f, -1.0f, 0, 0.f, 0.f,
                    1.0f, -1.0f, 0, 0.5f, 0.f,
                    -1.0f,  1.0f, 0, 0.f, 0.5f,
                    1.0f,  1.0f, 0, 0.5f, 0.5f,
            };
            final float[] mTriangleVerticesData4 = {
                    // X, Y, Z, U, V
                    -1.0f, -1.0f, 0, 0.5f, 0.f,
                    1.0f, -1.0f, 0, 1.f, 0.f,
                    -1.0f,  1.0f, 0, 0.5f, 1.f,
                    1.0f,  1.0f, 0, 1.f, 0.5f,
            };

            for(int i=0;i<4;i++) {
                mTriangleVerticesPart[i] = ByteBuffer.allocateDirect(
                        mTriangleVerticesData.length * FLOAT_SIZE_BYTES)
                        .order(ByteOrder.nativeOrder()).asFloatBuffer();
                if(i==0)
                    mTriangleVerticesPart[i].put(mTriangleVerticesData1).position(0);
                if(i==1)
                    mTriangleVerticesPart[i].put(mTriangleVerticesData2).position(0);
                if(i==2)
                    mTriangleVerticesPart[i].put(mTriangleVerticesData3).position(0);
                if(i==3)
                    mTriangleVerticesPart[i].put(mTriangleVerticesData4).position(0);
            }
            Matrix.setIdentityM(mSTMatrix, 0);
        }
        int number = 0;
        boolean bFirst = false;
        boolean onRecreateRecorder = false;
        @Override
        public void onDrawFrame(GL10 glUnused) {

            if(Utils.bRecorder) {
                if (!bFirst) {
                    Recorder.getRecorder().initialize();
                    try {
                        Recorder.getRecorder().setRecordPath("/mnt/usbdisk/usbdisk4/hank/");
                        Recorder.getRecorder().createMainRecord();
                        windowSurfaceFactory.setRecordSurface(Recorder.getRecorder().getMainRecorderSurface());
                        Recorder.getRecorder().start();
                        bFirst = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            if (number%2==0) {
                number = 0;
                mSurfaceTextures[0].updateTexImage();
                mSurfaceTextures[0].getTransformMatrix(mSTMatrix);

                windowSurfaceFactory.makeCurrentToPreview(contextFactory.getContext());
                GLES20.glViewport(0, 0, W, H);
                Draw();

                if(!Utils.bRecorder) {
                    windowSurfaceFactory.makeCurrentToRecord(contextFactory.getContext());
                    GLES20.glViewport(0, 0, GlobalInfo.width, GlobalInfo.height);
                    if (GlobalInfo.encodeComponent == -1) {
                        Draw();
                    } else {
                        Draw(GlobalInfo.encodeComponent);
                    }
                    windowSurfaceFactory.swapRecordBuffers();

                    Encoder.getEncoder().streamOut();
                } else {
                    if(!onRecreateRecorder) {
                        windowSurfaceFactory.makeCurrentToRecord(contextFactory.getContext());
                        GLES20.glViewport(0, 0, GlobalInfo.width, GlobalInfo.height);
                        Draw();
                        windowSurfaceFactory.swapRecordBuffers();
                    }
                }
            } else if (Utils.bRecorder && !onRecreateRecorder) {
                if(Recorder.getRecorder().checkTime()) {
                    onRecreateRecorder = true;
                    new Thread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            try
                            {
                                Recorder.getRecorder().recreate();
                                windowSurfaceFactory.changeRecordEGLSurface(Recorder.getRecorder().getMainRecorderSurface());
                                Recorder.getRecorder().start();
                                onRecreateRecorder = false;
                            }
                            catch (IOException e)
                            {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                }
            }
            number++;
        }
        int W = 0;
        int H = 0;
        @Override
        public void onSurfaceChanged(GL10 glUnused, int width, int height) {
            W = width;
            H = height;
        }

        boolean needUpdate = false;
        HandlerThread ht = null;
        Handler h = null;
        @Override
        public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
            mProgram = createProgram(mVertexShader, mFragmentShader);
            if (mProgram == 0) {
                return;
            }
            maPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
            checkGlError("glGetAttribLocation aPosition");
            if (maPositionHandle == -1) {
                throw new RuntimeException("Could not get attrib location for aPosition");
            }
            maTextureHandle = GLES20.glGetAttribLocation(mProgram, "aTextureCoord");
            checkGlError("glGetAttribLocation aTextureCoord");
            if (maTextureHandle == -1) {
                throw new RuntimeException("Could not get attrib location for aTextureCoord");
            }

            muMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
            checkGlError("glGetUniformLocation uMVPMatrix");
            if (muMVPMatrixHandle == -1) {
                throw new RuntimeException("Could not get attrib location for uMVPMatrix");
            }

            muSTMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uSTMatrix");
            checkGlError("glGetUniformLocation uSTMatrix");
            if (muSTMatrixHandle == -1) {
                throw new RuntimeException("Could not get attrib location for uSTMatrix");
            }


            GLES20.glGenTextures(number_of_play, textures, 0);


            for(int i=0;i<number_of_play;i++) {

                mTextureID = textures[i];
                GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, mTextureID);
                checkGlError("glBindTexture mTextureID");

                GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                        GLES20.GL_NEAREST);
                GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                        GLES20.GL_LINEAR);

            /*
             * Create the SurfaceTexture and pass it to the MediaPlayer
             */
                mSurfaceTextures[i] = new SurfaceTexture(mTextureID);

            }

            // MediaPlayerController part
            MediaPlayerController.mediaPlayerControllerSingleton.setSurfaceTextures(mSurfaceTextures);
            try {
                String[] path = new String[number_of_play];

                for(int i=0;i<number_of_play;i++) {
                    //CSI CAMERA PATH
                    path[i] = "test:libv4l2player.so?url=/dev/video11";
                }

                MediaPlayerController.mediaPlayerControllerSingleton.setDataSources(path);
                MediaPlayerController.mediaPlayerControllerSingleton.prepare();
                MediaPlayerController.mediaPlayerControllerSingleton.start();

            } catch (IOException e) {
                Log.d("HANK",e.toString());
                e.printStackTrace();
            }
        }

        public void Draw() {
            GLES20.glUseProgram(mProgram);
            checkGlError("glUseProgram");

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, textures[0]);

            tmpTriangleVertices = mTriangleVertices;

            tmpTriangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET);
            GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false,
                    TRIANGLE_VERTICES_DATA_STRIDE_BYTES, tmpTriangleVertices);
            checkGlError("glVertexAttribPointer maPosition");
            GLES20.glEnableVertexAttribArray(maPositionHandle);
            checkGlError("glEnableVertexAttribArray maPositionHandle");

            tmpTriangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET);
            GLES20.glVertexAttribPointer(maTextureHandle, 3, GLES20.GL_FLOAT, false,
                    TRIANGLE_VERTICES_DATA_STRIDE_BYTES, tmpTriangleVertices);
            checkGlError("glVertexAttribPointer maTextureHandle");
            GLES20.glEnableVertexAttribArray(maTextureHandle);
            checkGlError("glEnableVertexAttribArray maTextureHandle");

            Matrix.setIdentityM(mMVPMatrix, 0);
            GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mMVPMatrix, 0);
            GLES20.glUniformMatrix4fv(muSTMatrixHandle, 1, false, mSTMatrix, 0);

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
            checkGlError("glDrawArrays");

            GLES20.glDisableVertexAttribArray(maTextureHandle);
            GLES20.glDisableVertexAttribArray(maPositionHandle);
        }

        public void Draw(int i) {
            GLES20.glUseProgram(mProgram);
            checkGlError("glUseProgram");

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, textures[0]);

            tmpTriangleVertices = mTriangleVerticesPart[i];

            tmpTriangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET);
            GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false,
                    TRIANGLE_VERTICES_DATA_STRIDE_BYTES, tmpTriangleVertices);
            checkGlError("glVertexAttribPointer maPosition");
            GLES20.glEnableVertexAttribArray(maPositionHandle);
            checkGlError("glEnableVertexAttribArray maPositionHandle");

            tmpTriangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET);
            GLES20.glVertexAttribPointer(maTextureHandle, 3, GLES20.GL_FLOAT, false,
                    TRIANGLE_VERTICES_DATA_STRIDE_BYTES, tmpTriangleVertices);
            checkGlError("glVertexAttribPointer maTextureHandle");
            GLES20.glEnableVertexAttribArray(maTextureHandle);
            checkGlError("glEnableVertexAttribArray maTextureHandle");

            Matrix.setIdentityM(mMVPMatrix, 0);
            GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mMVPMatrix, 0);
            GLES20.glUniformMatrix4fv(muSTMatrixHandle, 1, false, mSTMatrix, 0);

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
            checkGlError("glDrawArrays");

            GLES20.glDisableVertexAttribArray(maTextureHandle);
            GLES20.glDisableVertexAttribArray(maPositionHandle);
        }


        private int loadShader(int shaderType, String source) {
            int shader = GLES20.glCreateShader(shaderType);
            if (shader != 0) {
                GLES20.glShaderSource(shader, source);
                GLES20.glCompileShader(shader);
                int[] compiled = new int[1];
                GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
                if (compiled[0] == 0) {
                    Log.e(TAG, "Could not compile shader " + shaderType + ":");
                    Log.e(TAG, GLES20.glGetShaderInfoLog(shader));
                    GLES20.glDeleteShader(shader);
                    shader = 0;
                }
            }
            return shader;
        }

        private int createProgram(String vertexSource, String fragmentSource) {
            int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
            if (vertexShader == 0) {
                return 0;
            }
            int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
            if (pixelShader == 0) {
                return 0;
            }

            int program = GLES20.glCreateProgram();
            if (program != 0) {
                GLES20.glAttachShader(program, vertexShader);
                checkGlError("glAttachShader");
                GLES20.glAttachShader(program, pixelShader);
                checkGlError("glAttachShader");
                GLES20.glLinkProgram(program);
                int[] linkStatus = new int[1];
                GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
                if (linkStatus[0] != GLES20.GL_TRUE) {
                    Log.e(TAG, "Could not link program: ");
                    Log.e(TAG, GLES20.glGetProgramInfoLog(program));
                    GLES20.glDeleteProgram(program);
                    program = 0;
                }
            }
            return program;
        }

        private void checkGlError(String op) {
            int error;
            while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
                Log.e(TAG, op + ": glError " + error);
                throw new RuntimeException(op + ": glError " + error);
            }
        }

    }

}