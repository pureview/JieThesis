package uk.newcastle.jiajie.model;

import android.util.Pair;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private DataService dataService;

    public RFModel(DataService service) {
        this.dataService = service;
        service.logToFront("RFModel | Load dataset");
        long tik = System.currentTimeMillis();
        dataset = new Dataset(service);
        double[][] x = dataset.getX();
        int[] y = dataset.getY();
        long tok = System.currentTimeMillis();
        service.logToFront("Load dataset spends " + (tok - tik) / 1000. + " seconds");
        service.logToFront("RFModel | Begin training. There are " + y.length + " samples for training");
        randomForest = new RandomForest(x, y, 12);
        service.logToFront("Begin testing.");
        int[] pred = randomForest.predict(x);
        String acc = calculateAcc(pred, y);
        service.logToFront("Training accuracy is: " + acc);
        service.logToFront("Dataset size: " + x.length);
        service.logToFront("Training model spends " + (System.currentTimeMillis() - tok) / 1000. + " seconds");
        service.logToFront("RFModel train error: " + randomForest.error());
    }

    /**
     * Calculate the training accuracy of the model
     */
    private String calculateAcc(int[] pred,
                                int[] y) {
        double counter = 0.;
        Map<Integer, Integer> clsCounter = new HashMap<>();
        Map<Integer, Integer> truthCounter = new HashMap<>();
        for (int i = 0; i < pred.length; i++) {
            if (!clsCounter.containsKey(y[i])) {
                clsCounter.put(y[i], 0);
            }
            if (!truthCounter.containsKey(y[i])) {
                truthCounter.put(y[i], 0);
            }
            if (pred[i] == y[i]) {
                counter += 1;
                truthCounter.put(y[i], truthCounter.get(y[i]) + 1);
            }
            clsCounter.put(y[i], clsCounter.get(y[i]) + 1);
        }
        StringBuilder sb = new StringBuilder();
        sb.append(counter / pred.length)
                .append(" | ");
        for (Integer key : clsCounter.keySet()) {
            sb.append(dataset.translate(key))
                    .append(" : ")
                    .append(truthCounter.get(key) / clsCounter.get(key))
                    .append(", ");
        }
        if (sb.length() > 2) {
            sb.delete(sb.length() - 2, sb.length());
        }
        return sb.toString();
    }

    /**
     * Predict the coming data
     */
    @Override
    public String predict(List<SensorBean> sensorBeans) {
        double[] sx = dataset.transformForPredict(sensorBeans);
        Long tik = System.currentTimeMillis();
        int sy = randomForest.predict(sx);
        Long tok = System.currentTimeMillis();
        dataService.logToFront("Predict spend " + (tok - tik) / 1000. + " seconds");
        return dataset.translate(sy);
    }
}
