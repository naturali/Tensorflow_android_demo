package org.tensorflow.demo;


import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.lang.Math;


public class Util {
    public static float[] byte2float(byte[] b) {
        byte bLength = 2;
        short[] s = new short[b.length / bLength];
        for (int iLoop = 0; iLoop < s.length; iLoop++) {
            byte[] temp = new byte[bLength];
            for (int jLoop = 0; jLoop < bLength; jLoop++) {
                temp[jLoop] = b[iLoop * bLength + jLoop];
            }
            s[iLoop] = byteArrayToShort(temp);
        }
        float[] f = short2float32(s);
        return f;
    }

    public static short byteArrayToShort(byte[] b) {
        return ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).getShort();
    }

    public static float[] short2float32(short[] ar) {
        int n_bytes = 2;
        float dst[] = new float[ar.length];
        float scale = 1.f / (float) (1 << ((8 * n_bytes) - 1));
        for (int i = 0; i < ar.length; i++) {
            dst[i] = scale * ar[i];
        }
        return dst;
    }

    public static void softmax(float[] ar, int st, int end) {
        float expSum = 0.f;
        for (int i = st; i < end; i ++) {

            expSum += Math.exp(ar[i]);
        }
        for (int i = st; i < end; i ++) {
            ar[i] = (float)Math.exp(ar[i]) / expSum;
        }
    }

    public static int argmax(float[] ar, int st, int end, int h) {
        float max = ar[st];
        int index = st;
        for (int i = st + 1; i < end; i++) {
            if (max < ar[i]) {
                max = ar[i];
                index = i;
            }
        }
        return index % h;
    }

    public static float vad(float[] sig){
        float sum = 0.f;
        for(float f:sig) {
            sum += Math.abs(f);
        }
        Log.i("sum", String.valueOf(sum));
        return sum;
    }
}
