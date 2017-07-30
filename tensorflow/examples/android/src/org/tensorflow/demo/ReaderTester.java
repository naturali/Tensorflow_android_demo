package org.tensorflow.demo;

import android.os.Environment;
import android.util.Log;

import java.io.IOException;

/**
 * Created by apple on 7/20/17.
 */

public class ReaderTester extends Tester{
    final String DEFAULT_TEST_FILE = Environment.getExternalStorageDirectory() + "/temp1.wav";
    private WavFileReader mWavFileReader;
    private volatile boolean mIsTestingExit = false;
    @Override
    public byte[] startTesting() {

        mWavFileReader = new WavFileReader();

        try {
            mWavFileReader.openFile(DEFAULT_TEST_FILE);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
            byte[] buffer = new byte[mWavFileReader.getmWavFileHeader().mSubChunk2Size];
            if(!mIsTestingExit){
                mWavFileReader.readData(buffer,0,buffer.length);
            }
            try {
                mWavFileReader.closeFile();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
            return buffer;
    }

    @Override
    public boolean stopTesting() {
        mIsTestingExit = true;
        return true;
    }
    /*private Runnable AudioPlayRunnable = new Runnable() {
        @Override
        public void run() {
            byte[] buffer = new byte[mAudioPlayer.getMinBufferSize()];
            while (!mIsTestingExit && mWavFileReader.readData(buffer, 0, buffer.length) > 0) {
                mAudioPlayer.play(buffer, 0, buffer.length);
            }
            try {
                mWavFileReader.closeFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }*/
}
