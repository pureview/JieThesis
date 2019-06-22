package uk.newcastle.jiajie.bean;

import android.support.annotation.NonNull;
import android.util.Log;

/**
 * @author Jie Jia
 * @date 20190621
 * @description The sensor output structure
 */
public class SensorBean {
    private Integer x;
    private Integer y;
    private Integer z;
    private String label;
    public static final int FEAT_NUM=3;

    public SensorBean(Integer x,
                      Integer y,
                      Integer z,
                      String label) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.label = label;
    }

    public SensorBean(String s) {
        String[] sp = s.split(",");
        if (sp.length != 4) {
            Log.e("SensorBean", "Decode fail " + s);
        }
        x = Integer.valueOf(sp[0]);
        y = Integer.valueOf(sp[1]);
        z = Integer.valueOf(sp[2]);
        label = sp[3];
    }

    public Integer getX() {
        return x;
    }

    public Integer getY() {
        return y;
    }

    public Integer getZ() {
        return z;
    }

    public String getLabel() {
        return label;
    }

    public double[] translate(){
        double[] ret=new double[3];
        ret[0]=(double)x;
        ret[1]=(double)y;
        ret[2]=(double)z;
        return ret;
    }

    @NonNull
    @Override
    public String toString() {
        return "" + x + "," + y + "," + z + "," + label;
    }
}
