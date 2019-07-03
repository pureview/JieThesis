package uk.newcastle.jiajie.interfaces;

import java.util.List;

import uk.newcastle.jiajie.bean.SensorBean;
import uk.newcastle.jiajie.model.Dataset;

/**
 * @author Jie Jia
 * @date 20190701
 */

public interface Model {
    /**
     * Predict a sample and output label in string format
     */
    String predict(List<SensorBean> sensorBeans);
}
