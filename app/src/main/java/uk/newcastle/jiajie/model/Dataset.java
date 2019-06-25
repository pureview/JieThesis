package uk.newcastle.jiajie.model;

import android.util.Log;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import uk.newcastle.jiajie.bean.SensorBean;
import uk.newcastle.jiajie.service.DataService;

import static uk.newcastle.jiajie.Constants.PADDING_SIZE;
import static uk.newcastle.jiajie.Constants.WINDOW_SIZE;

/**
 * @author Jie Jia
 * @date 20190622
 * @description Prepare dataset for training
 */

public class Dataset {

    private DataService service;
    private double[][] X;
    private ArrayList<double[]> tX = new ArrayList<>();
    private int[] Y;
    private ArrayList<Integer> tY = new ArrayList<>();
    private Map<String, Integer> labelMap = new HashMap<>();
    private List<String> labelList = new ArrayList<>();
    private List<SensorBean> cache = new LinkedList<>();

    public Dataset(DataService service) {
        this.service = service;
        loadDataset();
    }

    /**
     * Load dataset from disk
     */
    private void loadDataset() {
        String[] currentFiles = service.fileList();
        initLabels(currentFiles);
        for (int i = 0; i < currentFiles.length; i++) {
            String s = currentFiles[i];
            String curLabelString = s.split("_")[0];
            service.logToFront("Dataset | Processing " + i + "th file out of " + curLabelString.length() + " files");
            if (!labelMap.containsKey(curLabelString)) {
                continue;
            }
            int curLabel = labelMap.get(curLabelString);
            try {
                FileInputStream in = service.openFileInput(s);
                BufferedReader bi = new BufferedReader(new InputStreamReader(in));
                String line;
                int counter=0;
                while ((line = bi.readLine()) != null) {
                    if (line.length() > 0) {
                        SensorBean sensorBean = new SensorBean(line);
                        transformForTrain(sensorBean, curLabel);
                    }
                    counter+=1;
                }
                service.logToFront("Dataset | This file has " + counter + " lines. " +
                "Begin generate dataset");
                bi.close();
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        generateXY();
    }

    /**
     * Generate x y from cache
     */
    private void generateXY() {
        service.logToFront("Dataset | begin to generate dataset "+ tX.size());
        int size = tX.size();
        X = new double[size][SensorBean.FEAT_NUM * WINDOW_SIZE];
        Y = new int[size];
        for (int i = 0; i < size; i++) {
            X[i] = tX.get(i);
            Y[i] = tY.get(i);
        }
    }

    /**
     * Prepare the label string and label index map
     */
    private void initLabels(String[] currentFiles) {
        Set<String> labels = new HashSet<>();
        for (String s : currentFiles) {
            labels.add(s.split("_")[0]);
        }
        labelList = new ArrayList<>(labels);
        for (int i = 0; i < labelList.size(); i++) {
            labelMap.put(labelList.get(i), i);
        }
    }

    /**
     * Get training data
     */
    public double[][] getX() {
        return X;
    }

    /**
     * Get label of the training data
     */
    public int[] getY() {
        return Y;
    }

    /**
     * Transform for predict mode
     */
    public double[] transformForPredict(List<SensorBean> sensorBeans) {
        return generateVector(sensorBeans);
    }

    /**
     * Transform for training mode
     */
    private void transformForTrain(SensorBean sensorBean, int curLabel) {
        if (cache.size() == WINDOW_SIZE) {
            cache = cache.subList(PADDING_SIZE, WINDOW_SIZE);
        }
        cache.add(sensorBean);
        if (cache.size() == WINDOW_SIZE) {
            tX.add(generateVector(cache));
            tY.add(curLabel);
        }
    }


    /**
     * Generate vector
     */
    private double[] generateVector(List<SensorBean> cache) {
        double[] ret = new double[cache.size() * SensorBean.FEAT_NUM];
        for (int i = 0; i < cache.size(); i++) {
            double[] feat = cache.get(i).translate();
            for (int j = 0; j < SensorBean.FEAT_NUM; j++) {
                ret[i * SensorBean.FEAT_NUM + j] = feat[j];
            }
        }
        return ret;
    }

    /**
     * Translate label from integer
     */
    public String translate(int sy) {
        return labelList.get(sy);
    }
}
