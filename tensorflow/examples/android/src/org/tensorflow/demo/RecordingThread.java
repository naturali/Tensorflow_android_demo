package org.tensorflow.demo;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.SystemClock;
import android.util.Log;

import java.util.LinkedList;


public class RecordingThread {
    private static final String TAG = "RecordingThread";
    private static final String modelDir = "file:///android_asset/xiaobudian.pb";
    private static final int sampleRate = 16000;
    private static final int signalLength = 3600;
    private static final boolean needDebug = true;
    private boolean mShouldContinue;
    private Thread mThread;
    public SpeechCallback mCallback;

    // control whether to trigger classifier
    private int nonSpeechCount = 0;
    private boolean triggerClassify = false;

    // keep last at most 20 packages' volume
    private int maxKeepNum = 10;
    private LinkedList<Float> historyVolume = new LinkedList<>();
    private float avgVolume = 0f;
    private float vadThreshold = 40f;// 55f; // different device has different number
    private float volumeIncreaseTime = 3;

    private TensorFlowKeywordSpotting tensorFlowKeywordSpotting;
    private detect_callback callback;
    class detect_callback implements Runnable{
        private Context mContext;

        public detect_callback(Context context) {
            mContext = context;
        }

        @Override
        public void run() {
            PackageManager pm = mContext.getPackageManager();
            mContext.startActivity(pm.getLaunchIntentForPackage("com.singulariti.niapp"));
        }
    }
    private Runnable detect_callback;

    public static interface SpeechCallback {
        public void onSuccess(byte[] result);

        public void onError(byte[] result);
    }

    public RecordingThread(AssetManager assetManager, Context current) {
        tensorFlowKeywordSpotting = new TensorFlowKeywordSpotting(assetManager, modelDir);
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
        // initialize
        nonSpeechCount = 0;
        triggerClassify = false;
        historyVolume.clear();

        if (needDebug) {
            Log.v(TAG, "Start");
        }
        // read 1 sec each time
        /*
        int bufferSize = (int) (SAMPLE_RATE * 2);
        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            bufferSize = SAMPLE_RATE * 2;
        }*/
        int bufferSize = (int) (signalLength * 2);
        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            bufferSize = signalLength * 2;
        }
        // 225ms per package
        byte[] audioBuffer = new byte[signalLength * 2];

        AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_CONFIGURATION_MONO,
                // 1,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize);
        if (record.getState() != AudioRecord.STATE_INITIALIZED) {
            if (needDebug) {
                Log.e(TAG, "Audio Record can't initialize!");
            }
            return;
        }
        record.startRecording();
        if (needDebug) {
            Log.v(TAG, "Start recording");
        }

        while (mShouldContinue) {
            record.read(audioBuffer, 0, audioBuffer.length);
            float floatValues[] = Util.byte2float(audioBuffer);
            float vadSum = Util.vad(floatValues);
            // --- increase volume strategy: ---
            //    1. not too high for loud device (vad_sum < 200f);
            //    2. history volume (last 2s) is not too loud, which
            //       is to avoid increase noise (avg_volume < 90f);
            if (vadSum < 200f && avgVolume < 90f) {
                for (int i = 0; i < floatValues.length; i ++) {
                    floatValues[i] *= volumeIncreaseTime;
                }
            }
            if (vadSum > Math.max(avgVolume, vadThreshold)) {
                nonSpeechCount = 0;
                triggerClassify = true;
            } else {
                nonSpeechCount += 1;
            }
            // --- update history volume queue ---
            if (historyVolume.size() == maxKeepNum) {
                historyVolume.poll();
            }
            avgVolume = (avgVolume * historyVolume.size() + vadSum) / (historyVolume.size() + 1);
            historyVolume.add(vadSum);

            // --- classify ---
            boolean triggerHotword = false;
            if (triggerClassify) {
                Log.e(TAG, "run classify!!!");
                long startTime = SystemClock.uptimeMillis();
                this.tensorFlowKeywordSpotting.feed(floatValues);
                triggerHotword = this.tensorFlowKeywordSpotting.classify();
                long endTime = SystemClock.uptimeMillis();
                Log.d(TAG, "Timecost to run model inference: " + Long.toString(endTime - startTime));
            }
            if (triggerHotword) {
                Log.e(TAG, "Trigger!!!!");
                new Thread(callback).start();
                break;
            }
            Log.e(TAG, "::nonSpeechCount: " + nonSpeechCount);
            // continuous 500ms no speech, clear all states.
            if (nonSpeechCount >= 2) {
                tensorFlowKeywordSpotting.clear();
                nonSpeechCount = 0;
                triggerClassify = false;
            }
        }
        record.stop();
        record.release();
        tensorFlowKeywordSpotting.clear();
        if (needDebug) {
            Log.v(TAG, "Recording stopped");
        }

    }
}
