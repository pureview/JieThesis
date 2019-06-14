package uk.newcastle.jiajie;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.RxBleDevice;
import com.polidea.rxandroidble2.RxBleScanResult;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import uk.newcastle.jiajie.ble.BluetoothClient;
import uk.newcastle.jiajie.ble.BluetoothClientBLEV2Adapter;
import uk.newcastle.jiajie.ble.bean.BLEDevice;
import uk.newcastle.jiajie.ble.callback.BaseResultCallback;
import uk.newcastle.jiajie.ble.originV2.BluetoothLeInitialization;
import uk.newcastle.jiajie.util.StringUtil;

import static uk.newcastle.jiajie.Constants.*;

public class MainActivity extends AppCompatActivity {
    private TextView tvLog, tvItemDevice, tvOut, tvCurDevice;
    private EditText etIn;
    private Button btnScan, btnSend, btnClear;
    List<BLEDevice> devices = new ArrayList<>();
    private ListView deviceList;
    private BluetoothClient btClient;
    private BLEDevice curDevice = null;

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermission();
        tvLog = findViewById(R.id.tv_log);
        btnScan = findViewById(R.id.btn_scan);
        btnSend = findViewById(R.id.btn_send);
        deviceList = findViewById(R.id.device_list);
        tvOut = findViewById(R.id.tv_out);
        tvCurDevice = findViewById(R.id.tv_cur_device);
        etIn = findViewById(R.id.et_in);
        btnClear = findViewById(R.id.btn_clear);
        // Init scan button
        btnScan.setOnClickListener(v -> {
            Toast.makeText(MainActivity.this, "Begin to scan ble devices!", Toast.LENGTH_LONG).show();
            // Initialize bluetooth manager
            initBle();
        });
        // Init message send button
        btnSend.setOnClickListener(v -> {
            if (curDevice == null) {
                Toast.makeText(MainActivity.this, "There is no device connected.",
                        Toast.LENGTH_LONG).show();
            } else {
                // Send message
                String toBeSent = StringUtil.escap(etIn.getText().toString());
                writeToBle(toBeSent);
            }
        });
        // Init clear button
        btnClear.setOnClickListener(v -> tvLog.setText(""));
    }

    /**
     * Request necessary permissions.
     */
    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 519);
        }
    }

    /**
     * Initialize bluetooth manager
     */
    private void initBle() {
        btClient = new BluetoothClientBLEV2Adapter(BluetoothLeInitialization.getInstance(this));
        btClient.openBluetooth();
        devices.clear();
        for (int i = 0; i < 30; i++) {
            logToFront("test");
        }
        // 第一参数指定扫描时间，第二个参数指定是否中断当前正在进行的扫描操作
        btClient.search(3000, false)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<BLEDevice>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        logToFront("start scanning\n");
                    }

                    @Override
                    public void onNext(BLEDevice value) {
                        devices.add(value);
                        logToFront(value.getDeviceName() + "\n");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e("main", e.toString());
                        logToFront("Scan error:" + e.getMessage() + '\n');
                    }

                    @Override
                    public void onComplete() {
                        logToFront("Scan complete\n");
                        Log.d("main", "device size " + devices.size());
                        deviceList.setAdapter(new BaseAdapter() {
                            @Override
                            public int getCount() {
                                return devices.size();
                            }

                            @Override
                            public Object getItem(int position) {
                                return devices.get(position);
                            }

                            @Override
                            public long getItemId(int position) {
                                return 0;
                            }

                            @Override
                            public View getView(int position, View convertView, ViewGroup parent) {
                                LayoutInflater inflater = MainActivity.this.getLayoutInflater();
                                View view;
                                if (convertView == null) {
                                    //因为getView()返回的对象，adapter会自动赋给ListView
                                    view = inflater.inflate(R.layout.item_device, null);
                                } else {
                                    view = convertView;
                                }
                                tvItemDevice = view.findViewById(R.id.tv_device);
                                String deviceName = devices.get(position).getDeviceName();
                                if (deviceName == null) {
                                    deviceName = "Device is empty";
                                }
                                tvItemDevice.setText(deviceName);
                                tvItemDevice.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
                                return view;
                            }
                        });
                        //  Set item click listener
                        deviceList.setOnItemClickListener((parent, view, position, id) -> {
                            // Ready to click
                            curDevice = devices.get(position);
                            Toast.makeText(MainActivity.this,
                                    "Ready to connect " + curDevice,
                                    Toast.LENGTH_LONG).show();
                            logToFront("Ready to connect " + curDevice.getDeviceName() + "\n");
                            tvCurDevice.setText(curDevice.getDeviceName());
                            connect();
                        });
                    }
                });

    }

    /**
     * Connect with ble device
     */
    private void connect() {
        btClient.connect(curDevice.getMac())
                .flatMap((Function<String, ObservableSource<String>>) s -> btClient.registerNotify(curDevice.getMac(), serviceUUID,
                        readUUID, new BaseResultCallback<byte[]>() {
                            @Override
                            public void onSuccess(byte[] data) {
                                logToFront("[read] " + new String(data) + "\n");
                            }

                            @Override
                            public void onFail(String msg) {
                                logToFront("[error]" + msg);
                            }
                        }));
    }

    private void logToFront(String msg) {
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        tvLog.setText(dateFormat.format(date) + " " + msg + "\n" + tvLog.getText());
    }

    /**
     * Write msg to current ble device.
     *
     * @param msg The message to be sent
     */
    private void writeToBle(String msg) {
        btClient.connect(curDevice.getMac())
                .flatMap((Function<String, ObservableSource<String>>) s -> {
                    logToFront("[write]" + msg);
                    return btClient.write(curDevice.getMac(), serviceUUID,
                            writeUUID, msg.getBytes());
                });
    }
}
