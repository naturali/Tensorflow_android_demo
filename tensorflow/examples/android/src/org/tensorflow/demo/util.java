package org.tensorflow.demo;


import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.lang.Math;


/**
 * Created by liuziqi on 2017/7/26.
 */

public class util {
    static public float[] byte2float(byte[] b) {
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

    public static float max(float[] ar, int st, int end) {
        float max = ar[st];
        for (int i = st + 1; i < end; i++) {
            max = max > ar[i] ? max : ar[i];
        }
        return max;
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

    public static int[] toPrimitive(Integer[] ar) {
        int[] a = new int[ar.length];
        for (int i = 0; i < ar.length; i++) {
            a[i] = ar[i];
        }
        return a;
    }

    public static float[] concatAll(List<float[]> list) {
        int len = 0;
        for (float[] fl : list) {
            len += fl.length;
        }
        float[] concat = new float[len];
        int pt = 0;
        for (int i = 0; i < list.size(); i++) {
            float[] temp = list.get(i);
            System.arraycopy(temp, 0, concat, pt, temp.length);
            pt += temp.length;
        }
        return concat;
    }

    public static float[] merge2Arrays(float[] a1, float[] a2) {
        float[] b = new float[a1.length + a2.length];
        System.arraycopy(a1, 0, b, 0, a1.length);
        System.arraycopy(a2, 0, b, a1.length, a2.length);
        return b;
    }

    public static boolean vad(float[] sig,float thres){
        float sum=0.f;
        for(float f:sig){
            sum+=Math.abs(f);
        }
        Log.i("sum",String.valueOf(sum));
        return sum>thres;
    }


}
