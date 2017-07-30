/* Copyright 2016 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package org.tensorflow.demo;

import android.content.res.AssetManager;
import android.os.Trace;
import android.util.Log;

import java.util.ArrayList;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.util.Arrays;
import java.util.LinkedList;


/**
 * A classifier specialized to label images using TensorFlow.
 */
public class TensorFlowKeywordSpotting {
    private static final String TAG = "tf.KeywordSpotting";

    private final String target_label = "1233";

    // Config values.
    private String inputName;
    private String stateName;
    private String outputName;
    private String[] outputNames;
    private final int fft_size = 400;
    private final int hop_size = 160;
    private final int seg_len = 3600; //    (assume sample rate = 16000)
    private final int window_size = 15; // at most 10 segments (3s)  will be kept for classify
    private final int num_classes = 6; // 0 space 1 ni 2 hao 3 le 4 otherWords 5 blank_ctc
    private final int lockout = 3;
    private final float thres = 0.5f;
    private final float loose_thres = 0.2f;
    private final int num_layers = 2;
    private final int hidden_size = 128;
    private final String label_seq = "123";
    private final int state_size;

    // data
    private float[] floatValues;
    private LinkedList<float[]> softmaxList = new LinkedList<>();
    private float[] res = new float[0];
    private boolean logStats;
    private float[] state;

    private Classifier classifier;
    private TensorFlowInferenceInterface inferenceInterface;

    public TensorFlowKeywordSpotting(AssetManager assetManager,
                                     String modelPath) {
        this.inferenceInterface = new TensorFlowInferenceInterface(assetManager, modelPath);
        this.classifier = new Classifier(this.window_size,this.num_classes);


        inputName = "model/inputX";
        outputName = "model/softmax";
        stateName = "model/rnn_states";
        outputNames = new String[]{outputName, stateName};
        state_size = num_layers * hidden_size;
        state = new float[state_size];
        Arrays.fill(state, 0.f);
        logStats = false;
    }


    private float[] concat(float[] first, float[] second) {
        float[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    private boolean evaluate(String s) {
        return s.contains(this.label_seq);
    }

    private float[] padding(float[] origin) {
        float[] padding = new float[seg_len - origin.length];
        return concat(origin, padding);
    }

    public void feed(float[] segment) {
        // generally we assume segment len equal to seg_len, the last segment can be shorter and we will do the padding
        assert segment.length <= this.seg_len;
        if (segment.length < this.seg_len) {
            segment = padding(segment);
        }

        this.floatValues = (concat(this.res, segment));
        int resLen = (floatValues.length - fft_size) % hop_size + this.fft_size - this.hop_size;
        this.res = Arrays.copyOfRange(this.floatValues, this.floatValues.length - resLen, this.floatValues.length);
        int floatLen = floatValues.length - (floatValues.length - fft_size) % hop_size;
        this.floatValues = Arrays.copyOfRange(this.floatValues, 0, floatLen);
    }


    public boolean classify() {
        // Log this method so that it can be analyzed with systrace.
        Trace.beginSection("classify keyword");

        // Copy the input data into TensorFlow.
        Trace.beginSection("feed");

//        Log.e(TAG, String.valueOf(floatValues.length));
        inferenceInterface.feed(inputName, floatValues, floatValues.length);
        inferenceInterface.feed("model/rnn_initial_states", state, num_layers, 1, hidden_size);
        Trace.endSection();

        // Run the inference call.
        Trace.beginSection("run");
        inferenceInterface.run(this.outputNames, logStats);
        Trace.endSection();

        // Copy the output Tensor back into the output array.
        Trace.beginSection("fetch");
        float[] outputs = new float[computeFrame(this.floatValues.length) * num_classes];
        inferenceInterface.fetch(outputName, outputs);
//        Log.e(TAG, Arrays.toString(outputs));
        inferenceInterface.fetch(stateName, state);
        Trace.endSection();

//        if (softmaxList.size() >= 10) {
//            softmaxList.pop();
//        }
//        softmaxList.add(outputs);
//        float[] concated = util.concatAll(softmaxList);
        String result = classifier.ctc_decode(outputs, outputs.length / num_classes);

        Log.i("STRING RESULT","::"+result);

        return evaluate(result);
    }

    public void clear(){
        this.classifier.clear();
        Arrays.fill(this.state,0.f);
        Log.e(TAG,"clean state");
    }

    public ArrayList<Float> demo(float[] src) {
        int seg_size = src.length / 300;
        ArrayList<Float> list = new ArrayList<>();
        for (int i = 0; i < seg_size; i++) {
            Log.e("inference", String.valueOf(i));
            float[] output = new float[300 * computeFFT_size(300)];
//            Log.e("tf_input",Arrays.toString(Arrays.copyOfRange(src, i * 300, (i + 1) * 300)));
            inferenceInterface.feed("model/inputX", Arrays.copyOfRange(src, i * 300, (i + 1) * 300), 300);
            inferenceInterface.run(new String[]{"model/output", "model/test", "model/fft"}, logStats);
            inferenceInterface.fetch("model/fft", output);
            Log.e("tf_output", Arrays.toString(output));
            for (float f : output) {
                list.add(f);
            }
        }
        return list;
    }

    public boolean test_whole_audio(float[] src) {

        int seg_size = src.length / this.seg_len;
//        Log.e(TAG,String.valueOf(seg_size));
        for (int i = 0; i <= seg_size; i++) {
            System.out.println(i);
            if (i == seg_size)
                feed(Arrays.copyOfRange(src, i * seg_len, src.length));
            else
                feed(Arrays.copyOfRange(src, i * seg_len, (i + 1) * seg_len));
            boolean result = classify();
//            Log.e(TAG, String.valueOf(result));
            if (result) {
                Log.e(TAG, "Trigger!!!!!!!");
                return true;
            }
        }

        return false;
    }

    // compute the frame numbers when slicing window
    private int computeFrame(int t) {
        return (t - this.window_size) / this.hop_size + 1;
    }

    private int computeFFT_size(int fft_length) {
        return fft_length / 2 + 1;

    }

    public String getStatString() {
        return inferenceInterface.getStatString();
    }

    public void enableStatLogging(boolean logStats) {
        this.logStats = logStats;
    }

    public void close() {
        inferenceInterface.close();
    }
}
