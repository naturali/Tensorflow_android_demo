package org.tensorflow.demo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;


/**
 * Created by liuziqi on 2017/7/29.
 */

public class Classifier {
    private final float thres = 0.5f;
    private final float loose_thres = 0.2f;
    private final int lockout = 3;
    private final int h;
    private final int window_size;

    private class tuple {
        private int logit;
        private int frame;

        private tuple(int logit, int frame) {
            this.logit = logit;
            this.frame = frame;
        }
    }

    private int lockout_res = 0;
    private boolean loose = false;

    private LinkedList<String> strings = new LinkedList<>();

    public Classifier(int window_size,int num_classes) {
        this.window_size = window_size;
        this.h = num_classes;
        strings.add("");
    }

    public String ctc_decode(float[] softmax, int t) {
        LinkedList<tuple> result = new LinkedList<>();
        StringBuffer sb = new StringBuffer();
        int i = 0;
        i += lockout_res;
        while (i < t) {
//            System.out.println(i);
            if (loose) {
//                System.out.println("loose");
                if (util.max(softmax, i * h + 1, i * h + 5) < loose_thres) {
                    if (result.getLast().logit != 3) {
                        i += lockout;
                        loose = false;
                        continue;
                    }
                } else {
                    if (softmax[i * h + 3] > loose_thres) {
                        result.add(new tuple(3, i));
//                        System.out.println("flag");
                        i += lockout;
                        loose = false;
                        continue;
                    } else {
                        int pos = util.argmax(softmax, i * h + 1, i * h + 5, h) + 1;
                        if (softmax[i * h + pos - 1] > 0.6) {
                            if (result.getLast().frame + lockout < i) {
                                result.add(new tuple(pos, i));
//                                System.out.println("flag");
                            }
                        }
                    }
                }

            } else {
                if (util.max(softmax, i * h + 1, i * h + 5) > thres) {
                    result.add(new tuple(util.argmax(softmax, i * h + 1, i * h + 5, h), i));
//                    System.out.println("flag");
                    i += lockout;
                    if (result.size() >= 3) {
                        ArrayList<Integer> temp = new ArrayList<>();
                        for (tuple tup : result) {
                            temp.add(tup.logit);
                        }
                        int[] tempresult = util.toPrimitive(temp.toArray(new Integer[1]));
                        if (Arrays.equals(tempresult, new int[]{1, 2, 3}))
                            loose = true;
                    }
                    continue;
                }
            }
            i++;
        }
        lockout_res = i > t ? i - t : 0;
        for (tuple tup : result) {
            sb.append(tup.logit);
        }
        String s = strings.getLast() + sb.toString();
        if (strings.size() == window_size)
            strings.poll();

        strings.add(s);
        return s;
    }

    public void clear(){
        this.lockout_res=0;
        this.strings.clear();
        this.strings.add("");
        this.loose=false;
    }

}
