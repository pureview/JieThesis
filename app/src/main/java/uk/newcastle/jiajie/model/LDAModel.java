package uk.newcastle.jiajie.model;

import java.util.List;

import smile.classification.LDA;
import uk.newcastle.jiajie.bean.SensorBean;
import uk.newcastle.jiajie.interfaces.Model;
import uk.newcastle.jiajie.service.DataService;

public class LDAModel implements Model {

    private LDA lda;
    private Dataset dataset;

    public LDAModel(DataService service) {
        service.logToFront("LDAModel | Load dataset");
        dataset = new Dataset(service);
        double[][] x = dataset.getX();
        int[] y = dataset.getY();
        service.logToFront("LDAModel | Begin training. There are " + y.length + " samples for training");
        lda = new LDA(x, y);
        service.logToFront("LDAModel | Train finished.");
    }


    /**
     * Predict a sample and output label in string format
     */
    @Override
    public String predict(List<SensorBean> sensorBeans) {
        double[] sx = dataset.transformForPredict(sensorBeans);
        int sy = lda.predict(sx);
        return dataset.translate(sy);
    }
}
