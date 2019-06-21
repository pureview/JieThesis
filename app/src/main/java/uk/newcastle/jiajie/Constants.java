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
}
