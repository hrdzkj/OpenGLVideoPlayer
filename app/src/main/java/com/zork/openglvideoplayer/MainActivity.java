package com.zork.openglvideoplayer;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;

public class MainActivity extends Activity implements GLSurfaceView.OnTouchListener,View.OnClickListener {

    private GLSurfaceView glSurfaceView;
    private GLRenderer glRenderer;
    private SeekBar seekBar;
    private Button controlButton;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        controlButton = (Button) findViewById(R.id.btn_control);
        controlButton.setOnClickListener(this);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBarChangeEvent());
        glSurfaceView = (GLSurfaceView) findViewById(R.id.surface_view);
        glSurfaceView.setEGLContextClientVersion(2);
        glRenderer = new GLRenderer(this);
        glSurfaceView.setRenderer(glRenderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        glSurfaceView.setOnTouchListener(this);
        handler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                seekBar.setProgress(100 * glRenderer.getCurrentPosition() / glRenderer.getDuration());
            }
        };
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        glRenderer.release();
    }

    @Override
    protected void onPause(){
        super.onPause();
        glSurfaceView.onPause();
        glRenderer.pause();
    }

    @Override
    protected void onResume(){
        super.onResume();
        glSurfaceView.onResume();
        controlButton.setText(R.string.play);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return false;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_control:{
                Button btn = (Button) v;
                if(btn.getText().equals(getResources().getString(R.string.play))) {
                    glRenderer.play();
                    DelayThread delayThread = new DelayThread(100);
                    delayThread.start();
                    btn.setText(R.string.pause);
                } else {
                    glRenderer.pause();
                    btn.setText(R.string.play);
                }
            } break;
            default:
        }
    }

    class SeekBarChangeEvent implements SeekBar.OnSeekBarChangeListener{
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if(fromUser) {
                glRenderer.getMediaPlayer().seekTo(glRenderer.getDuration() * progress / 100);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    }

    class DelayThread extends Thread{
        int milliseconds;

        public DelayThread(int milliseconds) {
            this.milliseconds = milliseconds;
        }

        @Override
        public void run() {
            while (true){
                try {
                    sleep(milliseconds);
                } catch (InterruptedException ex){
                    ex.printStackTrace();
                }
                handler.sendEmptyMessage(0);
            }
        }
    }
}
