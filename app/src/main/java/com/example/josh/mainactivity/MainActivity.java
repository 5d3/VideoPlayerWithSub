package com.example.josh.mainactivity;

import android.Manifest;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.app.Activity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;

import com.example.utils.TimedTextObject;
import com.example.utils.FormatSRT;

import static android.support.v4.content.PermissionChecker.PERMISSION_GRANTED;


public class MainActivity extends Activity {

    private final String TAG = "main";
    private EditText et_path;
    private SurfaceView sv;
    private Button btn_play, btn_pause, btn_replay, btn_stop;
    private MediaPlayer mediaPlayer;
    private SeekBar seekBar;
    private int currentPosition = 0;
    private boolean isPlaying;
    private TimedTextObject mSRT;
    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        seekBar = (SeekBar) findViewById(R.id.seekBar);
        sv = (SurfaceView) findViewById(R.id.sv);
        et_path = (EditText) findViewById(R.id.et_path);

        btn_play = (Button) findViewById(R.id.btn_play);
        btn_pause = (Button) findViewById(R.id.btn_pause);
        btn_replay = (Button) findViewById(R.id.btn_replay);
        btn_stop = (Button) findViewById(R.id.btn_stop);

        btn_play.setOnClickListener(click);
        btn_pause.setOnClickListener(click);
        btn_replay.setOnClickListener(click);
        btn_stop.setOnClickListener(click);

        // 为SurfaceHolder添加回调
        sv.getHolder().addCallback(callback);

        // 4.0版本之下需要设置的属性
        // 设置Surface不维护自己的缓冲区，而是等待屏幕的渲染引擎将内容推送到界面
        // sv.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        // 为进度条添加进度更改事件
        seekBar.setOnSeekBarChangeListener(change);

        // Gesture detection
        gestureDetector = new GestureDetector(this, new MyGestureDetector());


        //Add gesture support only for the SurfaceView  @joshjliu
        sv.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                gestureDetector.onTouchEvent(motionEvent);
                return true;
            }
        });
    }
/*
    //Add gesture support for MainActivity  @joshjliu
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }
*/
    private Callback callback = new Callback() {
        // SurfaceHolder被修改的时候回调
        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.i(TAG, "SurfaceHolder 被销毁");
            // 销毁SurfaceHolder的时候记录当前的播放位置并停止播放
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                currentPosition = mediaPlayer.getCurrentPosition();
                mediaPlayer.stop();
            }
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.i(TAG, "SurfaceHolder 被创建");
            if (currentPosition > 0) {
                // 创建SurfaceHolder的时候，如果存在上次播放的位置，则按照上次播放位置进行播放
                play(currentPosition);
                currentPosition = 0;
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width,
                                   int height) {
            Log.i(TAG, "SurfaceHolder 大小被改变");
        }

    };

    private OnSeekBarChangeListener change = new OnSeekBarChangeListener() {

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            // 当进度条停止修改的时候触发
            // 取得当前进度条的刻度
            int progress = seekBar.getProgress();
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                // 设置当前播放的位置
                mediaPlayer.seekTo(progress);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress,
                                      boolean fromUser) {

        }
    };
    private View.OnClickListener click = new View.OnClickListener() {

        @Override
        public void onClick(View v) {

            switch (v.getId()) {
                case R.id.btn_play:
                    play(0);
                    break;
                case R.id.btn_pause:
                    pause();
                    break;
                case R.id.btn_replay:
                    replay();
                    break;
                case R.id.btn_stop:
                    stop();
                    break;
                default:
                    break;
            }
        }
    };


    /*
144      * 停止播放
145      */
    protected void stop() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
            btn_play.setEnabled(true);
            isPlaying = false;
        }
    }

    /**
     * 157      * 开始播放
     * 158      *
     * 159      * @param msec 播放初始位置
     * 160
     */
    protected void play(final int msec) {
        // 获取视频文件地址
        String path = et_path.getText().toString().trim();
        path = Environment.getExternalStorageDirectory() + "/Lesson1.mp4";
        path = Environment.getExternalStorageDirectory() + "/jellies.mp4";
        String sub = Environment.getExternalStorageDirectory() + "/jellies.srt";

        try {
            FileInputStream sub_stream = new FileInputStream(sub);
            FormatSRT formatSRT = new FormatSRT();
            mSRT = formatSRT.parseFile("sample.srt", sub_stream);
        } catch (FileNotFoundException e) {
            Toast.makeText(this, "subtitle file NOT found", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "error while formatting SRT", Toast.LENGTH_SHORT).show();
        }
        File file = new File(path);
        if (!file.exists()) {
            Toast.makeText(this, "视频文件路径错误", Toast.LENGTH_SHORT).show();
            return;
        }
//        int permissionCheck = ContextCompat.checkSelfPermission(mContext,
//                Manifest.permission.READ_EXTERNAL_STORAGE);
//        if(PERMISSION_GRANTED == permissionCheck) {
//
//        }
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            // 设置播放的视频源
            mediaPlayer.setDataSource(file.getAbsolutePath());
            // 设置显示视频的SurfaceHolder
            mediaPlayer.setDisplay(sv.getHolder());
            Log.i(TAG, "开始装载");
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(new OnPreparedListener() {

                @Override
                public void onPrepared(MediaPlayer mp) {
                    Log.i(TAG, "装载完成");
                    mediaPlayer.start();
                    // 按照初始位置播放
                    mediaPlayer.seekTo(msec);
                    // 设置进度条的最大进度为视频流的最大播放时长
                    seekBar.setMax(mediaPlayer.getDuration());
                    // 开始线程，更新进度条的刻度
                    new Thread() {

                        @Override
                        public void run() {
                            try {
                                isPlaying = true;
                                while (isPlaying) {
                                    int current = mediaPlayer
                                            .getCurrentPosition();
                                    seekBar.setProgress(current);

                                    sleep(500);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }.start();

                    btn_play.setEnabled(false);
                }
            });
            mediaPlayer.setOnCompletionListener(new OnCompletionListener() {

                @Override
                public void onCompletion(MediaPlayer mp) {
                    // 在播放完毕被回调
                    btn_play.setEnabled(true);
                    mediaPlayer.stop();
                    mediaPlayer.release();
                    mediaPlayer = null;
                    //btn_play.setEnabled(true);
                    isPlaying = false;
                }
            });

            mediaPlayer.setOnErrorListener(new OnErrorListener() {

                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    // 发生错误重新播放
                    play(0);
                    isPlaying = false;
                    return false;
                }
            });

            mediaPlayer.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {

                @Override
                public void onVideoSizeChanged(MediaPlayer mp, int videoWidth,
                                               int videoHeight) {

                    DisplayMetrics metrics = new DisplayMetrics();
                    getWindowManager().getDefaultDisplay().getMetrics(metrics);
                    // Get the width of the screen
                    int screenWidth = metrics.widthPixels;
                    int screenHeight = metrics.heightPixels;

                    // Get the SurfaceView layout parameters
                    android.view.ViewGroup.LayoutParams lp = sv.getLayoutParams();

                    int displayHeight = (int) (((float) videoHeight / (float) videoWidth) * (float) screenWidth);
                    int displayWidth;
                    if (displayHeight > screenHeight) {
                        displayHeight = screenHeight;
                        displayWidth = (int) (((float) videoWidth / (float) videoHeight) * (float) screenHeight);
                    } else {
                        displayWidth = screenWidth;
                    }

                    // Set the width of the SurfaceView to the width of the
                    // screen
                    lp.width = displayWidth;

				/*
				 * Set the height of the SurfaceView to match the aspect ratio
				 * of the video be sure to cast these as floats otherwise the
				 * calculation will likely be 0
				 */
                    lp.height = displayHeight;

                    // Commit the layout parameters
                    sv.setLayoutParams(lp);

                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     *  重新开始播放
     *
     */
    protected void replay() {
        //在暂停状态下按 重播  程序会挂住
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.seekTo(0);
            Toast.makeText(this, "重新播放", Toast.LENGTH_SHORT).show();
            btn_pause.setText("暂停");
            return;
        }

        if (mediaPlayer != null && ! (mediaPlayer.isPlaying())) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
            btn_play.setEnabled(true);
            btn_pause.setText("暂停");
        }
        isPlaying = false;
        play(0);
        //mediaPlayer.setVideoScalingMode();
        //mediaPlayer.setOnTimedTextListener();


    }

    /**
     * 暂停或继续
     *
     */
    protected void pause() {
        if (btn_pause.getText().toString().trim().equals("继续")) {
            btn_pause.setText("暂停");
            mediaPlayer.start();
            Toast.makeText(this, "继续播放", Toast.LENGTH_SHORT).show();
            return;
        }
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            btn_pause.setText("继续");
            Toast.makeText(this, "暂停播放", Toast.LENGTH_SHORT).show();
        }

    }


    class MyGestureDetector extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            /*
            try {
                if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
                    return false;
                // right to left swipe
                if(e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    Toast.makeText(SelectFilterActivity.this, "Left Swipe", Toast.LENGTH_SHORT).show();
                } else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    Toast.makeText(SelectFilterActivity.this, "Right Swipe", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                // nothing
            }
            */
            float x1 = e1.getX();
            float x2 = e2.getX();
            float deltaX = x1 - x2;
            int SWIPE_MIN_DISTANCE = 120;
            int SWIPE_THRESHOLD_VELOCITY = 500;
            if (Math.abs(deltaX) < SWIPE_MIN_DISTANCE)
                return false;

            String dir = "";

            if (Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                if (deltaX > 0) {
                    dir = "toLeft " + x1 + " " + x2 + " " + velocityX;
                } else {
                    dir = "toRight " + x1 + " " + x2 + " " + velocityX;
                }
                Toast.makeText(MainActivity.this, "fling " + dir, Toast.LENGTH_SHORT).show();
            }
            return true;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }
    }
}
