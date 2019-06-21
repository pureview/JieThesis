package uk.newcastle.jiajie.bean;

/**
 * @author Jie Jia
 * @date 20190621
 * @description The sensor output structure
 */
public class SensorBean {
    private Double x;
    private Double y;
    private Double z;

    public SensorBean(Double x,
                      Double y,
                      Double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Double getX() {
        return x;
    }

    public Double getY() {
        return y;
    }

    public Double getZ() {
        return z;
    }
}
