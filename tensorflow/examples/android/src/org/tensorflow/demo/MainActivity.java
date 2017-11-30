package org.tensorflow.demo;

import android.app.Activity;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;


public class MainActivity extends Activity {
    private Tester mTester;
    private Spinner mTestSpinner;
    private SoundPool sp;
    private byte[] bytes;
    private TensorFlowKeywordSpotting tensorFlowKeywordSpotting;
    public static final String[] TEST_PROGRAM_ARRAY = {
            "读取wav文件"
    };
    private final String MODEL_DIR = "file:///android_asset/graph.pb";
    private SaveData saveData;
    private ArrayList<Float> TensorFlowList;
    private float[] floats;
    int soundID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTestSpinner = (Spinner) findViewById(R.id.TestSpinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, TEST_PROGRAM_ARRAY);
        mTestSpinner.setAdapter(adapter);
        saveData = new SaveData();

        Log.e("SOUNDid", String.valueOf(soundID));
//        tensorFlowKeywordSpotting = new TensorFlowKeywordSpotting(getAssets(), MODEL_DIR);

    }

    public void onClickStartTest(View v) {
        mTester = new ReaderTester();
        Log.e("liuziqi", Environment.getExternalStorageDirectory().toString());
        if (mTester != null) {
            bytes = mTester.startTesting();
            saveData.save(bytes);
            floats = Util.byte2float(bytes);
            Toast.makeText(this, "Start Testing !\n", Toast.LENGTH_SHORT).show();
//            SoundPool.Builder spb = new SoundPool.Builder();
//            spb.setMaxStreams(10);
//            spb.setAudioAttributes(new AudioAttributes.Builder()
//                    .setUsage(AudioAttributes.USAGE_MEDIA)
//                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
//                    .build());   //转换音频格式
//            SoundPool sp = spb.build();      //创建SoundPool对象
//            Log.e("fuck", "123");
//            int resid = getResources().getIdentifier("goat.wav", "raw", this.getPackageName());
//            Log.e("RESID",String.valueOf(resid));
//
//            this.soundID = sp.load(this, R.raw.goat, 1);
//
//            this.sp.play(this.soundID, 1, 1, 0, 0, 1);
            MediaPlayer mp = MediaPlayer.create(this, R.raw.goat);
            mp.start();

        } else {
            Log.e("liuziqi", "mTester is null");
        }
    }

    public void onClickTensorFlow(View v) {
        Log.e("liuziqi", "12345a");
        tensorFlowKeywordSpotting = new TensorFlowKeywordSpotting(getAssets(), MODEL_DIR);
        Log.e("liuziqi", "sdlkfjlskd");
    }

    public void onClickStopTest(View v) {
        if (mTester != null) {
            mTester.stopTesting();
            Toast.makeText(this, "Stop Testing !", Toast.LENGTH_SHORT).show();
        }
    }
}
