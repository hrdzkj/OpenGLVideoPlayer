package com.zork.openglvideoplayer;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;

public class MainActivity extends Activity implements GLSurfaceView.OnTouchListener,View.OnClickListener {

    private GLSurfaceView glSurfaceView;
    private GLRenderer glRenderer;
    private SeekBar seekBar;
    private Button controlButton;
    private Button switchButton;
    private Handler handler;
    private boolean alreadyTimeRemark = true;
    private DelayThread timeRemarkThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        controlButton = (Button) findViewById(R.id.btn_control);
        controlButton.setOnClickListener(this);
        switchButton = (Button) findViewById(R.id.btn_switch);
        switchButton.setOnClickListener(this);
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
                if (msg.what == 0) {
                    int progress = glRenderer.getDuration();
                    if (progress != 0) {
                        seekBar.setProgress(100 * glRenderer.getCurrentPosition() / glRenderer.getDuration());
                    }
                } else if(msg.what == 1){
                    controlButton.setVisibility(View.GONE);
                    switchButton.setVisibility(View.GONE);
                    seekBar.setVisibility(View.GONE);
                    alreadyTimeRemark = false;
                }
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
        if(timeRemarkThread != null)
            timeRemarkThread.interrupt();
        glSurfaceView.onPause();
        glRenderer.pause();
    }

    @Override
    protected void onResume(){
        super.onResume();
        glSurfaceView.onResume();
        glRenderer.seekTo(glRenderer.getCurrentPosition()+1);
        controlButton.setText(R.string.play);
        controlButton.setVisibility(View.VISIBLE);
        switchButton.setVisibility(View.VISIBLE);
        seekBar.setVisibility(View.VISIBLE);
        alreadyTimeRemark = true;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        controlButton.setVisibility(View.VISIBLE);
        switchButton.setVisibility(View.VISIBLE);
        seekBar.setVisibility(View.VISIBLE);
        if(!alreadyTimeRemark) {
            alreadyTimeRemark = true;
            hideWidgetDelay();
        }
        return false;
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_control:{
                Button btn = (Button) v;
                if(btn.getText().equals(getResources().getString(R.string.play))) {
                    glRenderer.play();
                    DelayThread delayThread = new DelayThread(100,0);
                    delayThread.start();
                    hideWidgetDelay();
                    btn.setText(R.string.pause);
                } else {
                    glRenderer.pause();
                    btn.setText(R.string.play);
                }
                break;
            }
            case R.id.btn_switch:{
                Button btn = (Button) v;
                if(btn.getText().equals(getResources().getString(R.string.mirror))) {
                    glRenderer.setFragmentShaderId(1);
                    btn.setText(R.string.normal);
                } else {
                    glRenderer.setFragmentShaderId(0);
                    btn.setText(R.string.mirror);
                }
                break;
            }
            default:
        }
    }

    class SeekBarChangeEvent implements SeekBar.OnSeekBarChangeListener{
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if(fromUser) {
                glRenderer.seekTo(glRenderer.getDuration() * progress / 100);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            alreadyTimeRemark = true;
            if(timeRemarkThread != null)
                timeRemarkThread.interrupt();
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            alreadyTimeRemark = false;
            hideWidgetDelay();
        }
    }

    class DelayThread extends Thread {
        int milliseconds;
        int what;

        public DelayThread(int milliseconds,int what) {
            this.milliseconds = milliseconds;
            this.what = what;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    sleep(milliseconds);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                    if(what == 1)
                        break;
                }
                handler.sendEmptyMessage(what);
                if(what == 1)
                    break;
            }
        }
    }

    private void hideWidgetDelay(){
        timeRemarkThread = new DelayThread(3000,1);
        timeRemarkThread.start();
    }
}
