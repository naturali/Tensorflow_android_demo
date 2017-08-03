package org.tensorflow.demo;

import android.content.Context;
import android.content.res.AssetManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.SoundPool;
import android.util.Log;
import android.widget.Toast;


/**
 * Created by apple on 7/26/17.
 */

public class RecordingThread {
    private static final String LOG_TAG = "RecordingThread";
    private final String MODEL_DIR = "file:///android_asset/graph.pb";
    private static final int SAMPLE_RATE = 16000;
    private static final boolean NEED_DEBUG = true;
    private final int soundID = 0;
    public byte[] mRecordResult;
    private boolean mShouldContinue;
    private Thread mThread;
    public SpeechCallback mCallback;
    private int non_speech_count = 0;

    private TensorFlowKeywordSpotting tensorFlowKeywordSpotting;
    private SoundPool sp;
    private Context context;
    private detect_callback callback;
    class detect_callback implements Runnable{
        private MediaPlayer mp;

        public detect_callback(Context context) {
            this.mp = MediaPlayer.create(context, R.raw.goat);
        }

        @Override
        public void run() {
            mp.start();
        }
    }
    private Runnable detect_callback;

    public static interface SpeechCallback {
        public void onSuccess(byte[] result);

        public void onError(byte[] result);
    }

    public RecordingThread(AssetManager assetManager, Context current) {
        tensorFlowKeywordSpotting = new TensorFlowKeywordSpotting(assetManager, MODEL_DIR);
//        SoundPool.Builder spb = new SoundPool.Builder();
//        spb.setMaxStreams(10);
//        spb.setAudioAttributes(null);    //转换音频格式
//        SoundPool sp = spb.build();      //创建SoundPool对象
//        this.context = current;
//        int resid = context.getResources().getIdentifier("goat.wav", "raw", this.context.getPackageName());
//        this.soundID = sp.load(this.context, resid, 1);
        callback = new detect_callback(current);

    }

    public boolean recording() {
        return mThread != null;
    }

    public void startRecording() {

        if (recording())
            return;

        mShouldContinue = true;
        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                record();
            }
        });
        mThread.start();
    }

    public void stopRecording(SpeechCallback callback) {
        if (!recording())
            return;
        mCallback = callback;
        mShouldContinue = false;
        mThread = null;
    }

    private void record() {
        if (NEED_DEBUG) {
            Log.v(LOG_TAG, "Start");
        }
        // read 1 sec each time
        int bufferSize = (int) (SAMPLE_RATE * 2);
        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            bufferSize = SAMPLE_RATE * 2;
        }
        byte[] audioBuffer = new byte[3600 * 2];

        AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize);
        if (record.getState() != AudioRecord.STATE_INITIALIZED) {
            if (NEED_DEBUG) {
                Log.e(LOG_TAG, "Audio Record can't initialize!");
            }
            return;
        }
        record.startRecording();
        if (NEED_DEBUG) {
            Log.v(LOG_TAG, "Start recording");
        }
        while (mShouldContinue) {
            int numberOfByte = record.read(audioBuffer, 0, audioBuffer.length);
//            Log.e("fuck",String.valueOf(numberOfByte));
            float floatValues[] = util.byte2float(audioBuffer);
            if (util.vad(floatValues, 55)) {
                this.non_speech_count = 0;
            } else {
                this.non_speech_count += 1;
                if (this.non_speech_count >= 2) {
                    this.tensorFlowKeywordSpotting.clear();
                    this.non_speech_count = 0;
                }
            }
            this.tensorFlowKeywordSpotting.feed(floatValues);
            boolean result = this.tensorFlowKeywordSpotting.classify();
            if (result) {
                Log.e(LOG_TAG, "Trigger!!!!");
                new Thread(callback).start();
//                Toast.makeText(this.context, "Trigger!!!!!\n", Toast.LENGTH_SHORT).show();
                this.tensorFlowKeywordSpotting.clear();
            }

        }
        record.stop();
        record.release();
        this.tensorFlowKeywordSpotting.clear();
//        mCallback.onSuccess(mRecordResult);
        if (NEED_DEBUG) {
            Log.v(LOG_TAG, "Recording stopped");
        }

    }
}
