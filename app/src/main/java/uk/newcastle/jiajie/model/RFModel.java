package uk.newcastle.jiajie.model;


import android.util.AttributeSet;
import android.util.Log;

import java.util.List;
import org.slf4j.Logger;

import smile.classification.RandomForest;
import uk.newcastle.jiajie.bean.SensorBean;
import uk.newcastle.jiajie.service.DataService;

/**
 * @author Jie Jia
 * @date 20190622
 * @description Random forest model
 */
public class RFModel {

    private RandomForest randomForest;
    private Dataset dataset;

    public RFModel(DataService service) {
        Log.d("RFModel", "Load dataset");
        dataset = new Dataset(service);
        double[][] x = dataset.getX();
        int[] y = dataset.getY();
        randomForest = new RandomForest(x, y, 12);
    }

    /**
     * Predict the coming data
     */
    public String predict(List<SensorBean> sensorBeans) {
        double[] sx = dataset.transformForPredict(sensorBeans);
        int sy = randomForest.predict(sx);
        return dataset.translate(sy);
    }
}
