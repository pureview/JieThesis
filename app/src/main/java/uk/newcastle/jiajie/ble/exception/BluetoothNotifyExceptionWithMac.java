package uk.newcastle.jiajie.ble.exception;

public class BluetoothNotifyExceptionWithMac extends BluetoothExceptionWithMac {

    public BluetoothNotifyExceptionWithMac(String msg, String mac) {
        super(msg, mac);
    }
}
