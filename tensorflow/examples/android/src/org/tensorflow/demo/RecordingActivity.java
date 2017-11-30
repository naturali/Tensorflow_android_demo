package org.tensorflow.demo;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;



public class RecordingActivity  extends Activity {
    private byte[] recordbytes;
    RecordingThread recordingThread;
    private SaveData saveData = new SaveData();
    private Button startButton,stopButton;
    private RecordingThread.SpeechCallback callback = new RecordingThread.SpeechCallback() {
        @Override
        public void onSuccess(byte[] result) {
             recordbytes = result;
            saveData.save(recordbytes);
        }

        @Override
        public void onError(byte[] result) {

        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);
        startButton = (Button)findViewById(R.id.record_start);
        startButton.setEnabled(true);
        stopButton = (Button)findViewById(R.id.record_stop);
        stopButton.setEnabled(false);
    }
    public void onClickRecord(View v){
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        recordingThread = new RecordingThread(getAssets(),this);
        recordingThread.startRecording();
    }
    public void onClickStopRecord(View v){
        recordingThread.stopRecording(callback);
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
    }
}
