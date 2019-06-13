package uk.newcastle.jiajie.ble.exception;

public class BluetoothWriteExceptionWithMac extends BluetoothExceptionWithMac {

    public BluetoothWriteExceptionWithMac(String msg, String mac) {
        super(msg, mac);
    }
}
