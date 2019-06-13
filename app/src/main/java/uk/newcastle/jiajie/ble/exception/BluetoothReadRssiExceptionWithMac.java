package uk.newcastle.jiajie.ble.exception;

/**
 * 蓝牙读取信号错误异常
 */
public class BluetoothReadRssiExceptionWithMac extends BluetoothExceptionWithMac {

    public BluetoothReadRssiExceptionWithMac(String msg, String mac) {
        super(msg, mac);
    }
}
