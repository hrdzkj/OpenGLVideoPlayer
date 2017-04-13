package com.zork.openglvideoplayer;

import android.content.Context;
import android.graphics.ColorMatrix;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.effect.EffectFactory;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;
import android.view.Surface;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Administrator on 2017/2/2.
 */

public class GLRenderer implements GLSurfaceView.Renderer,SurfaceTexture.OnFrameAvailableListener,MediaPlayer.OnVideoSizeChangedListener {
    public enum Status {
        IDLE, PREPARED, PLAYING, PAUSED, STOPPED, COMPLETE, ERROR
    }

    private static final String TAG = "GLRenderer";

    private Context context;
    private int programId;
    private int aPositionHandle;
    private int uMatrixHandle;
    private int textureId;
    private int uTextureSamplerHandle;
    private int aTextureCoordHandle;
    private int uSTMMatrixHandle;
    private FloatBuffer vertexBuffer;
    private FloatBuffer textureVertexBuffer;

    private final float[] projectionMatrix = new float[16];
    private final float[] vertexData = {
            1f, -1f, 0f,
            -1f, -1f, 0f,
            1f, 1f, 0f,
            -1f, 1f, 0f,
    };

    private final float[] textureVertexData = {
            1f, 0f,
            0f, 0f,
            1f, 1f,
            0f, 1f
    };

    private MediaPlayer mediaPlayer;
    private SurfaceTexture surfaceTexture;

    private boolean updateSurface;
    private Status status;

    private int screenWidth, screenHeight;

    private final float[] mSTMatrix = new float[16];

    public GLRenderer(Context context) {
        this.context = context;

        status = Status.IDLE;
        synchronized (this) {
            updateSurface = false;
        }
        vertexBuffer = ByteBuffer.allocateDirect(vertexData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertexData);
        vertexBuffer.position(0);

        textureVertexBuffer = ByteBuffer.allocateDirect(textureVertexData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(textureVertexData);
        textureVertexBuffer.position(0);

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnErrorListener(new MediaPlayerErrorEvent());
        mediaPlayer.setOnCompletionListener(new MediaPlayerCompletionEvent());
        mediaPlayer = MediaPlayer.create(context, R.raw.viedo1);
        status = Status.PREPARED;
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnVideoSizeChangedListener(this);
        mediaPlayer.setLooping(true);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        String vertexShader = ShaderUtils.readRawTextFile(context, R.raw.vertex_shader);
        String fragmentShader = ShaderUtils.readRawTextFile(context, R.raw.fragment_shader);
        programId = ShaderUtils.createProgram(vertexShader, fragmentShader);
        uMatrixHandle = GLES20.glGetUniformLocation(programId, "uMatrix");
        uTextureSamplerHandle = GLES20.glGetUniformLocation(programId, "sTexture");
        aPositionHandle = GLES20.glGetAttribLocation(programId, "aPosition");
        aTextureCoordHandle = GLES20.glGetAttribLocation(programId, "aTexCoord");
        uSTMMatrixHandle = GLES20.glGetUniformLocation(programId, "uSTMatrix");

        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);

        textureId = textures[0];
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
        ShaderUtils.checkGlError("glBindTexture mTextureID");
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        surfaceTexture = new SurfaceTexture(textureId);
        surfaceTexture.setOnFrameAvailableListener(this);

        Surface surface = new Surface(surfaceTexture);
        mediaPlayer.setSurface(surface);
        surface.release();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.d(TAG, "onSurfaceChanged: " + width + " " + height);
        screenWidth = width;
        screenHeight = height;
        float ratio = width > height ?
                (float) width / height :
                (float) height / width;
        if (width > height) {
            Matrix.orthoM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, -1f, 1f);
        } else {
            Matrix.orthoM(projectionMatrix, 0, -1f, 1f, -ratio, ratio, -1f, 1f);
        }
        updateProjection(mediaPlayer.getVideoWidth(),mediaPlayer.getVideoHeight(),width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        synchronized (this) {
            if (updateSurface) {
                surfaceTexture.updateTexImage();
                surfaceTexture.getTransformMatrix(mSTMatrix);
                updateSurface = false;
            }
        }
        GLES20.glUseProgram(programId);
        GLES20.glUniformMatrix4fv(uMatrixHandle, 1, false, projectionMatrix, 0);
        GLES20.glUniformMatrix4fv(uSTMMatrixHandle, 1, false, mSTMatrix, 0);
        vertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aPositionHandle);
        GLES20.glVertexAttribPointer(aPositionHandle, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer);

        textureVertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aTextureCoordHandle);
        GLES20.glVertexAttribPointer(aTextureCoordHandle, 2, GLES20.GL_FLOAT, false, 8, textureVertexBuffer);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);

        GLES20.glUniform1i(uTextureSamplerHandle, 0);
        GLES20.glViewport(0, 0, screenWidth, screenHeight);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
    }

    @Override
    synchronized public void onFrameAvailable(SurfaceTexture surface) {
        updateSurface = true;
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        Log.d(TAG, "onVideoSizeChanged: " + width + " " + height);
        //updateProjection(width, height);
    }

    private void updateProjection(int videoWidth, int videoHeight) {
        float screenRatio = (float) screenWidth / screenHeight;
        float videoRatio = (float) videoWidth / videoHeight;
        if (videoRatio > screenRatio) {
            Matrix.orthoM(projectionMatrix, 0, -1f, 1f, -videoRatio / screenRatio, videoRatio / screenRatio, -1f, 1f);
        } else {
            Matrix.orthoM(projectionMatrix, 0, -screenRatio / videoRatio, screenRatio / videoRatio, -1f, 1f, -1f, 1f);
        }
    }

    private void updateProjection(int videoWidth, int videoHeight, int screenWidth, int screenHeight) {
        float screenRatio = (float) screenWidth / screenHeight;
        float videoRatio = (float) videoWidth / videoHeight;
        if (videoRatio > screenRatio) {
            Matrix.orthoM(projectionMatrix, 0, -1f, 1f, -videoRatio / screenRatio, videoRatio / screenRatio, -1f, 1f);
        } else {
            Matrix.orthoM(projectionMatrix, 0, -screenRatio / videoRatio, screenRatio / videoRatio, -1f, 1f, -1f, 1f);
        }
    }

    public int getCurrentPosition() {
        return mediaPlayer.getCurrentPosition();
    }

    public int getDuration() {
        if(status != null && status != Status.IDLE) {
            return mediaPlayer.getDuration();
        } else {
            return 0;
        }
    }

    public void play() {
        if (status == Status.PREPARED || status == Status.PAUSED ) {
            mediaPlayer.start();
            status = Status.PLAYING;
        }
    }

    public void seekTo(int msec){
        mediaPlayer.seekTo(msec);
    }

    public void pause() {
        if (status == Status.PLAYING) {
            mediaPlayer.pause();
            status = Status.PAUSED;
        }
    }

    public void release() {
            mediaPlayer.release();
            status = null;
    }

    public void stop() {
        if (status != Status.STOPPED) {
            mediaPlayer.stop();
            status = Status.STOPPED;
        }
    }

    public Status getStatus(){
        return status;
    }

    class MediaPlayerCompletionEvent implements MediaPlayer.OnCompletionListener{
        @Override
        public void onCompletion(MediaPlayer mp) {
            if(!mp.isLooping()) {
                status = Status.COMPLETE;
            }
        }
    }

    class MediaPlayerErrorEvent implements MediaPlayer.OnErrorListener {
        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            status = Status.ERROR;
            return false;
        }
    }
}