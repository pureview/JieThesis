package uk.newcastle.jiajie.model;

import java.util.List;

import uk.newcastle.jiajie.bean.SensorBean;
import uk.newcastle.jiajie.interfaces.Model;
import uk.newcastle.jiajie.service.DataService;

import smile.classification.NeuralNetwork;

public class MLPModel implements Model {

    Dataset dataset;
    NeuralNetwork mlp;

    private static final String tag = "MLPModel | ";

    public MLPModel(DataService service) {
        service.logToFront(tag + "Load dataset");
        dataset = new Dataset(service);
        double[][] x = dataset.getX();
        int[] y = dataset.getY();
        service.logToFront(tag + "Begin training. There are " + y.length + " samples for training");
        mlp = new NeuralNetwork(NeuralNetwork.ErrorFunction.CROSS_ENTROPY,
                NeuralNetwork.ActivationFunction.SOFTMAX,
                128, dataset.getNumLabel());
        mlp.learn(x, y);
        service.logToFront(tag + "Train finished.");
    }

    /**
     * Predict a sample and output label in string format
     */
    @Override
    public String predict(List<SensorBean> sensorBeans) {
        double[] sx = dataset.transformForPredict(sensorBeans);
        int sy = mlp.predict(sx);
        return dataset.translate(sy);
    }
}
