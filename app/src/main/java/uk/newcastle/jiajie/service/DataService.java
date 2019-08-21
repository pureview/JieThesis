package uk.newcastle.jiajie.service;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.inuker.bluetooth.library.BluetoothClient;
import com.inuker.bluetooth.library.connect.listener.BleConnectStatusListener;
import com.inuker.bluetooth.library.connect.response.BleNotifyResponse;
import com.inuker.bluetooth.library.search.SearchRequest;
import com.inuker.bluetooth.library.search.SearchResult;
import com.inuker.bluetooth.library.search.response.SearchResponse;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import uk.newcastle.jiajie.Constants;
import uk.newcastle.jiajie.MainActivity;
import uk.newcastle.jiajie.R;
import uk.newcastle.jiajie.bean.SensorBean;
import uk.newcastle.jiajie.model.RFModel;
import uk.newcastle.jiajie.type.ServiceStatusType;
import uk.newcastle.jiajie.util.DecodeUtil;

import static com.inuker.bluetooth.library.Constants.REQUEST_SUCCESS;
import static com.inuker.bluetooth.library.Constants.STATUS_CONNECTED;
import static com.inuker.bluetooth.library.Constants.STATUS_DISCONNECTED;
import static uk.newcastle.jiajie.Constants.*;

public class DataService extends Service {

    // Service mode
    private ServiceStatusType serviceStatus = ServiceStatusType.FREE;
    // Current label
    private String curLabel;
    // Enable visualization
    private boolean enableDraw = false;
    // Bluetooth data structure
    private List<SearchResult> devices = new ArrayList<>();
    private SearchResult curDevice = null;
    private BluetoothClient btClient;
    private StringBuilder streamBuffer = new StringBuilder();
    private List<SensorBean> cache = new LinkedList<>();
    private String curFileName;
    private RFModel rfModel;

    private static final int flushThresh = 500;
    private static final int trimHead = 0;
    private static final int trimTail = 0;
    private int predictDrawStride = 20;

    @Override
    public void onCreate() {
        Toast.makeText(this, "Data service has been created",
                Toast.LENGTH_LONG).show();
        /*
        Notification.Builder builder = new Notification.Builder(this.getApplicationContext());
        Intent nfIntent = new Intent(this, MainActivity.class);
        builder.setContentIntent(PendingIntent.
                getActivity(this, 0, nfIntent, 0))
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(),
                        R.mipmap.ic_launcher))
                .setContentTitle("Data service")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentText("Processing sensor data")
                .setWhen(System.currentTimeMillis());
        Notification notification = builder.build();
        notification.defaults = Notification.DEFAULT_SOUND;
        startForeground(519, notification);
        */
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction() == null) {
            return super.onStartCommand(intent, flags, startId);
        }
        if (btClient == null) {
            btClient = new BluetoothClient(this);
            if (!btClient.isBluetoothOpened()) {
                toast("Please open bluetooth");
                btClient.openBluetooth();
            }
        }
        switch (intent.getAction()) {
            case CONNECT_DEVICE:
                int pos = Integer.valueOf(intent.getStringExtra(MAIN_ACTION_DATA));
                connect(pos);
                break;
            case STRIDE:
                predictDrawStride = Integer.valueOf(intent.getStringExtra(MAIN_ACTION_DATA));
                break;
            case ENABLE_DRAW:
                enableDraw = Boolean.getBoolean(intent.getStringExtra(MAIN_ACTION_DATA));
            case SCAN:
                initBle();
                break;
            case MAIN_ACTION_CMD:
                String cmd = intent.getStringExtra(MAIN_ACTION_DATA);
                writeToBle(cmd);
                break;
            case Constants.ACTION_LABEL:
                serviceStatus = ServiceStatusType.LABEL;
                curLabel = intent.getStringExtra(MAIN_ACTION_DATA);
                logToConsole("Change to label mode, " + curLabel);
                initLabelFile();
                break;
            case Constants.ACTION_STOP:
                toast("Change to free mode");
                serviceStatus = ServiceStatusType.FREE;
                if (serviceStatus == ServiceStatusType.LABEL) {
                    toast("Ready to flush data");
                    flushCache();
                    trimFile();
                    toast("Flush data successfully");
                }
                break;
            case REVERT:
                logToConsole("Ready to revert last labelling section");
                revertLast();
                break;
            case Constants.ACTION_PREDICT:
                logToConsole("Change to predict mode");
                serviceStatus = ServiceStatusType.PREDICT;
                break;
            case TRAIN:
                logToConsole("Begin train");
                train();
                break;
            case EXPORT:
                logToFront("Begin export");
                export();
                break;
            case IMPORT:
                logToFront("Begin import");
                incorporate();
                break;
        }

        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * Train the model
     */
    private void train() {
        if (rfModel == null) {
            rfModel = new RFModel(this);
            logToFront("Training is finished");
        }
    }

    /**
     * Revert last labelling section
     */
    private void revertLast() {
        if (curFileName == null || "".equals(curFileName)) {
            return;
        }
        deleteFile(curFileName);
        logToFront("File " + curFileName + " is deleted");
    }

    /**
     * Trim current file
     */
    private void trimFile() {
        if (trimHead == 0 && trimTail == 0) {
            return;
        }
        try {
            List<String> holder = new LinkedList<>();
            FileInputStream in = openFileInput(curFileName);
            BufferedReader bi = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = bi.readLine()) != null) {
                holder.add(line);
            }
            bi.close();
            in.close();
            FileOutputStream out = openFileOutput(curFileName, MODE_PRIVATE);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out));
            for (int i = trimHead; i < holder.size() - trimTail; i++) {
                bw.write(holder.get(i) + '\n');
            }
            bw.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Prepare the labelling file for writing
     */
    private void initLabelFile() {
        String[] currentFiles = fileList();
        int label_ind = 0;
        for (String name : currentFiles) {
            if (name.startsWith(curLabel)) {
                label_ind = Math.max(label_ind, Integer.valueOf(name.split("_")[1]));
            }
        }
        label_ind++;
        curFileName = curLabel + "_" + label_ind;
    }

    /**
     * Checks if external storage is available for read and write
     */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /**
     * Import file from external storage
     */
    private void incorporate() {
        if (!isExternalStorageWritable()) {
            logToFront("External storage is not available");
            return;
        }
        File dir = new File(Environment.getExternalStorageDirectory(), "Jie");
        logToFront("Data dir:" + Environment.getDataDirectory());
        dir.mkdirs();
        for (File name : dir.listFiles()) {
            logToFront("Copying " + name.getName());
            try {
                FileInputStream in = new FileInputStream(name);
                BufferedReader bi = new BufferedReader(new InputStreamReader(in));
                // Get name
                FileOutputStream out = openFileOutput(name.getName(), MODE_PRIVATE);
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out));
                String line;
                while ((line = bi.readLine()) != null) {
                    bw.write(line + '\n');
                }
                bw.flush();
                bi.close();
                in.close();
                bw.close();
                out.close();
            } catch (IOException e) {
                logToFront(e.getMessage());
            }
        }
    }

    /**
     * Output dataset to external storage
     */
    private void export() {
        if (!isExternalStorageWritable()) {
            logToFront("External storage is not available");
            return;
        }
        String[] currentFiles = fileList();
        for (String name : currentFiles) {
            logToFront("Copying " + name);
            try {
                FileInputStream in = openFileInput(name);
                BufferedReader bi = new BufferedReader(new InputStreamReader(in));
                // Get name
                logToFront("Data dir:" + Environment.getDataDirectory());
                File dir = new File(Environment.getExternalStorageDirectory(), "Jie");
                dir.mkdirs();
                File file = new File(dir, name);
                logToFront("Write path:" + file.getAbsolutePath());
                // file.createNewFile();
                FileOutputStream out = new FileOutputStream(file);
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out));
                String line;
                while ((line = bi.readLine()) != null) {
                    bw.write(line + '\n');
                }
                bw.flush();
                bi.close();
                in.close();
                bw.close();
                out.close();
            } catch (IOException e) {
                logToFront(e.getMessage());
            }
        }
    }

    /**
     * Flush cache data into disk
     */
    private void flushCache() {
        try {
            FileOutputStream out = openFileOutput(curFileName, MODE_PRIVATE);
            long tik = System.currentTimeMillis();
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out));
            for (SensorBean sensorBean : cache) {
                bw.write(sensorBean.toString() + '\n');
            }
            long tok = System.currentTimeMillis();
            logToFront("Flush cache of size " + cache.size() + " into disk spend " + (tok - tik) / 1000. + " seconds");
            bw.close();
            out.close();
            // Send data to front end and draw chart
            drawChart(cache.subList(Math.max(0, cache.size() - 50), cache.size()),
                    LABEL_DRAW, "Current label: " + curLabel);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void drawChart(List<SensorBean> data,
                           String type,
                           String title) {
        StringBuilder sb = new StringBuilder();
        for (SensorBean sensorBean : data) {
            sb.append(sensorBean).append('\n');
        }
        logToConsole("Begin draw chart: " + type + "|" + title + "|" + sb);
        Intent intent = new Intent();
        intent.setAction(type);
        intent.putExtra(MAIN_ACTION_DATA, sb.toString());
        intent.putExtra(TITLE, title);
        sendBroadcast(intent);
    }

    /**
     * Process predict data
     */
    private void processPredictData(String data) {
        if (rfModel == null) {
            logToFront("Please train the model first");
            return;
        }
        cache.addAll(DecodeUtil.decodeBytes(data, curLabel));
        Map<String, Integer> labelMap = new HashMap<>();
        Integer maxCount = 0;
        String finalLabel = "";
        if (cache.size() > PREDICT_CACHE_SIZE) {
            while (cache.size() >= WINDOW_SIZE) {
                String label = rfModel.predict(cache.subList(0, WINDOW_SIZE));
                if (!labelMap.containsKey(label)) {
                    labelMap.put(label, 0);
                }
                labelMap.put(label, labelMap.get(label) + 1);
                if (labelMap.get(label) > maxCount) {
                    maxCount = labelMap.get(label);
                    finalLabel = label;
                }
                cache = cache.subList(predictDrawStride, cache.size());
            }
        }
        logToFront("Predict result: " + finalLabel);
        if (enableDraw) {
            drawChart(cache.subList(Math.max(0, cache.size() - 50), cache.size()),
                    PREDICT_DRAW, finalLabel);
        }
    }

    /**
     * Process labelling data
     */
    private void processLabelData(String data) {
        if (curLabel == null || curLabel.equals("")) {
            logToConsole("Current label is null");
            return;
        }
        cache.addAll(DecodeUtil.decodeBytes(data, curLabel));
        if (cache.size() >= flushThresh) {
            logToConsole("Ready to flush into disk " + cache.size());
            flushCache();
            cache.clear();
        }
    }

    @Override
    public void onDestroy() {
        toast("Data service destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Toast a msg to foreground
     */
    private void toast(String msg) {
        Toast.makeText(this, msg,
                Toast.LENGTH_LONG).show();
    }

    private void logToConsole(String msg) {
        Log.e("DataService", msg);
    }

    public void logToFront(String msg) {
        logToConsole(msg);
        Intent intent = new Intent();
        intent.setAction(MAIN_ACTION_LOG);
        intent.putExtra(MAIN_ACTION_DATA, msg);
        sendBroadcast(intent);
    }

    private void sendCommand(String cmd, String data) {
        logToConsole("sendCommand:" + cmd + "|" + data);
        Intent intent = new Intent();
        intent.setAction(MAIN_ACTION_CMD);
        intent.putExtra(MAIN_ACTION_CMD, cmd);
        intent.putExtra(MAIN_ACTION_DATA, data);
        sendBroadcast(intent);
    }

    /**
     * Use android official interface
     */
    private void initBle() {
        toast("Begin scanning bluetooth devices");
        devices.clear();
        btClient.stopSearch();
        sendCommand(CLEAR, "");
        SearchRequest request = new SearchRequest.Builder()
                .searchBluetoothLeDevice(6000, 2)
                .build();
        btClient.search(request, new SearchResponse() {
            @Override
            public void onSearchStarted() {
                logToFront("Start search for ble devices");
            }

            @Override
            public void onDeviceFounded(SearchResult device) {
                logToFront("Find device " + device.getName() +
                        ", address " + device.getAddress());
                sendCommand(DEVICE_FOUND, device.getName());
                devices.add(device);
            }

            @Override
            public void onSearchStopped() {
                logToFront("Search bluetooth devices stopped");
            }

            @Override
            public void onSearchCanceled() {
                logToFront("Search cancelled");
            }
        });
    }

    /**
     * Connect to curDevice
     */
    private void connect(int pos) {
        curDevice = devices.get(pos);
        btClient.connect(curDevice.getAddress(), (i, bleGattProfile) -> {
            if (i == REQUEST_SUCCESS) {
                logToFront("Connect success " + bleGattProfile.toString());
                sendCommand(CONNECT_DEVICE, "Connected: " + curDevice.getName());
            } else {
                curDevice = null;
                logToFront("Connect fail, code = " + i);
            }
        });

        BleConnectStatusListener mBleConnectStatusListener = new BleConnectStatusListener() {

            @Override
            public void onConnectStatusChanged(String mac, int status) {
                if (status == STATUS_CONNECTED) {
                    logToFront("Device connected " + mac);
                } else if (status == STATUS_DISCONNECTED) {
                    logToFront("Device disconnected " + mac);
                }
            }
        };
        btClient.registerConnectStatusListener(curDevice.getAddress(),
                mBleConnectStatusListener);
        logToFront("Register device notification");
        btClient.notify(curDevice.getAddress(),
                serviceUUID,
                readUUID,
                new BleNotifyResponse() {
                    @Override
                    public void onNotify(UUID service, UUID character, byte[] bytes) {
                        if (bytes.length < 2) {
                            return;
                        }
                        int size = bytes.length;
                        streamBuffer.append(new String(bytes));
                        if (bytes[size - 2] == '\r' && bytes[size - 1] == '\n') {
                            // logToConsole("Finish reading buffer, status is " + serviceStatus);
                            String buff = streamBuffer.toString();
                            switch (serviceStatus) {
                                case FREE:
                                    break;
                                case LABEL:
                                    processLabelData(buff);
                                    break;
                                case PREDICT:
                                    processPredictData(buff);
                                    break;
                            }
                            streamBuffer = new StringBuilder();
                            //logToConsole("[NotifyRead]" + buff +"; BuffSize=" + buff.length());
                            //logToFront("[Decode]" + DecodeUtil.decodeBytes(buff));
                        }
                    }

                    @Override
                    public void onResponse(int i) {
                        if (i == REQUEST_SUCCESS) {
                            logToFront("Register notify success");
                            // Ready to stream data
                            logToFront("Ready to set to stream mode");
                            writeToBle("I");
                        } else {
                            logToFront("Register notify fail. Code=" + i);
                        }
                    }
                });
    }

    /**
     * Write msg to current ble device.
     *
     * @param msg The message to be sent
     */
    private void writeToBle(String msg) {
        logToFront("[write]" + msg);
        btClient.write(curDevice.getAddress(),
                serviceUUID, writeUUID, msg.getBytes(),
                code -> {
                    if (code == REQUEST_SUCCESS) {
                        logToFront("Write success");
                    } else {
                        logToFront("Write fail, code= " + code);
                    }
                });
    }
}
