package uk.newcastle.jiajie.model;


import android.util.AttributeSet;
import android.util.Log;

import java.util.List;

import org.slf4j.Logger;

import smile.classification.RandomForest;
import uk.newcastle.jiajie.bean.SensorBean;
import uk.newcastle.jiajie.interfaces.Model;
import uk.newcastle.jiajie.service.DataService;

/**
 * @author Jie Jia
 * @date 20190622
 * @description Random forest model
 */
public class RFModel implements Model {

    private RandomForest randomForest;
    private Dataset dataset;

    public RFModel(DataService service) {
        service.logToFront("RFModel | Load dataset");
        dataset = new Dataset(service);
        double[][] x = dataset.getX();
        int[] y = dataset.getY();
        service.logToFront("RFModel | Begin training. There are " + y.length + " samples for training");
        randomForest = new RandomForest(x, y, 12);
        service.logToFront("RFModel train error: " + randomForest.error());
    }

    /**
     * Predict the coming data
     */
    @Override
    public String predict(List<SensorBean> sensorBeans) {
        double[] sx = dataset.transformForPredict(sensorBeans);
        int sy = randomForest.predict(sx);
        return dataset.translate(sy);
    }
}
