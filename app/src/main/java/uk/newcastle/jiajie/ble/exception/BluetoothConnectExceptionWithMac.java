package uk.newcastle.jiajie.ble.exception;

/**
 * 蓝牙连接异常
 */
public class BluetoothConnectExceptionWithMac extends BluetoothExceptionWithMac {

    public BluetoothConnectExceptionWithMac(String msg, String mac) {
        super(msg, mac);
    }
}
