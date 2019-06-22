package uk.newcastle.jiajie;

import java.util.UUID;

public class Constants {
    public static final int REQUEST_ENABLE_BT = 123;
    public static final UUID serviceUUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
    public static final UUID readUUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");
    public static final UUID writeUUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E");
    public static final String ACTION_STREAM = "stream";
    public static final String ACTION_LABEL = "label";
    public static final String ACTION_PREDICT = "predict";
    public static final String ACTION_STOP = "stop";
    public static final String MAIN_ACTION_LOG = "log";
    public static final String MAIN_ACTION_DATA = "data";
    public static final String MAIN_ACTION_CMD = "cmd";
    public static final String CLEAR = "clear";
    public static final String DEVICE_FOUND = "device_found";
    public static final String CONNECT_DEVICE = "connect_device";
    public static final String SCAN = "scan";
    public static final String LABEL_STOP = "label_stop";
    public static final String REVERT = "revert";
    public static final String LABEL_DRAW = "label_draw";
    public static final int WINDOW_SIZE =50;
    public static final int PADDING_SIZE =25;
}
