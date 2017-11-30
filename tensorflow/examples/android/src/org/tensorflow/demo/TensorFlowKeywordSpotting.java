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

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.util.Arrays;
import java.util.List;


/**
 * A classifier specialized to detect keyword using TensorFlow.
 */
public class TensorFlowKeywordSpotting {
    private static final String TAG = "tf.KeywordSpotting";

    // Tensor names
    private final String inputName = "model/inputX";
    private final String stateInputName = "model/rnn_initial_states";
    private final String stateOutputName = "model/rnn_states";
    private final String outputName = "model/logit";
    private String[] outputFetchNames = {outputName, stateOutputName};

    // Config values.
    private final int fftSize = 320;//400;
    private final int hopSize = 160;
    private final int maxSegmentLength = 3600; // 3920 buffer size + 320 append
    private final int numFrames = (maxSegmentLength - fftSize) / hopSize + 1;
    private final int numLayers = 2;
    private final int hiddenSize = 128;

    // Decoder config
    private final float threshold = 0.2f;
    private final int maxKeepLength = 15; // at most 10 segments (3s)  will be kept for classify
    private final int numClasses = 6; // 0 space 1 ni 2 hao 3 le 4 otherWords 5 blank_ctc
    private final String labelSeq = "123";
    private final char otherWordIdx = '4';
    private final List<String> fuzzyLabels = Arrays.asList("143", "23", "423", "1423", "243", "1443");

    // data
    private float[] floatValues;
    private float[] floatValuesRemain;
    private boolean logStats;
    private float[] state;

    private Classifier classifier;
    private TensorFlowInferenceInterface inferenceInterface;

    public TensorFlowKeywordSpotting(AssetManager assetManager,
                                     String modelPath) {
        inferenceInterface = new TensorFlowInferenceInterface(assetManager, modelPath);
        classifier = new Classifier(threshold, maxKeepLength, numClasses, otherWordIdx);

        state = new float[numLayers * hiddenSize];
        Arrays.fill(state, 0.f);
        floatValuesRemain = new float[0];
        logStats = false;
    }

    private float[] concat(float[] first, float[] second) {
        float[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    private boolean evaluate(String s, boolean strict) {
        if (strict) {
            return s.contains(labelSeq);
        } else {
            for (String lb : fuzzyLabels) {
                if (s.endsWith(lb)) {
                    return true;
                }
            }
            return false;
        }
    }

    private float[] padding(float[] origin, int paddingLength) {
        float[] padding = new float[paddingLength - origin.length];
        return concat(origin, padding);
    }


    // compute the frame numbers when slicing window
    private int computeFrame(int t) {
        return (t - fftSize) / hopSize + 1;
    }


    public void feed(float[] segment) {
        // generally we assume segment len equal to seg_len, the last segment can be shorter and we will do the padding
        assert segment.length <= maxSegmentLength;
        if (segment.length < maxSegmentLength) {
            segment = padding(segment, maxSegmentLength);
        }
        floatValues = concat(floatValuesRemain, segment);
        int remainLen = (floatValues.length - fftSize) % hopSize + fftSize - hopSize;
        floatValuesRemain =
                Arrays.copyOfRange(floatValues, floatValues.length - remainLen, floatValues.length);
    }


    public boolean classify() {
        // Log this method so that it can be analyzed with systrace.
        Trace.beginSection("classify keyword");

        // Copy the input data into TensorFlow.
        Trace.beginSection("feed");

        inferenceInterface.feed(inputName, floatValues, floatValues.length);
        inferenceInterface.feed(stateInputName, state, numLayers, 1, hiddenSize);
        Trace.endSection();

        // Run the inference call.
        Trace.beginSection("run");
        inferenceInterface.run(outputFetchNames, logStats);
        Trace.endSection();

        // Copy the output Tensor back into the output array.
        Trace.beginSection("fetch");
        float[] outputValues = new float[computeFrame(floatValues.length) * numClasses];
        inferenceInterface.fetch(outputName, outputValues);
        inferenceInterface.fetch(stateOutputName, state);

        Trace.endSection();

        String result = classifier.ctcDecode(outputValues, numFrames);
        Log.i("STRING RESULT","::"+result);

        return evaluate(result, false);
    }

    public void clear(){
        classifier.clear();
        Arrays.fill(state, 0.f);
        if (floatValues != null) {
            floatValues = null;
        }
        floatValuesRemain = new float[0];
        Log.e(TAG,"clean state~~~~~~");
    }

    public void close() {
        inferenceInterface.close();
    }
}
