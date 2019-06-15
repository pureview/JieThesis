package uk.newcastle.jiajie;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
import uk.newcastle.jiajie.service.BluetoothService;
import uk.newcastle.jiajie.util.StringUtil;

import static uk.newcastle.jiajie.Constants.*;

public class MainActivity extends AppCompatActivity {
    private TextView tvLog, tvItemDevice, tvOut, tvCurDevice;
    private EditText etIn;
    private Button btnScan, btnSend, btnClear;
    List<BluetoothDevice> devices = new ArrayList<>();
    private ListView deviceList;
    private BluetoothDevice curDevice = null;
    private BluetoothAdapter bluetoothAdapter;
    private Handler handler;
    private boolean isScanning = false;
    private BaseAdapter deviceAdapter;
    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device. This can be a
    // result of read or notification operations.
    private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothService.ACTION_GATT_CONNECTED.equals(action)) {
                connected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (BluetoothService.ACTION_GATT_DISCONNECTED.equals(action)) {
                connected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();
            } else if (BluetoothService.
                    ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the
                // user interface.
                displayGattServices(bluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };

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
        // Init device list adapter
        deviceAdapter = new BaseAdapter() {
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
                String deviceName = devices.get(position).getName();
                if (deviceName == null) {
                    deviceName = "Device is empty";
                }
                tvItemDevice.setText(deviceName);
                tvItemDevice.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
                return view;
            }
        };
        deviceList.setAdapter(deviceAdapter);
        //  Set item click listener
        deviceList.setOnItemClickListener((parent, view, position, id) -> {
            // Ready to click
            curDevice = devices.get(position);
            Toast.makeText(MainActivity.this,
                    "Ready to connect " + curDevice,
                    Toast.LENGTH_LONG).show();
            logToFront("Ready to connect " + curDevice.getName());
            tvCurDevice.setText(curDevice.getName());
            connect();
        });
        // Initializes Bluetooth adapter.
        BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Please open bluetooth",
                    Toast.LENGTH_LONG).show();
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
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

    private void logToFront(String msg) {
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        tvLog.setText(dateFormat.format(date) + " " + msg + "\n" + tvLog.getText());
        Log.d("main", dateFormat.format(date) + " " + msg + "\n" + tvLog.getText());
    }

    /**
     * Write msg to current ble device.
     *
     * @param msg The message to be sent
     */
    private void writeToBle(String msg) {
        logToFront("[write]" + msg);
        btClient.write(curDevice.getMac(), serviceUUID,
                writeUUID, msg.getBytes());
    }


    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    /**
     * Use android official interface
     */
    private void initBle() {
        if (isScanning) {
            toast("Is scanning! Please wait");
            return;
        }
        // Scan bluetooth devices
        long SCAN_PERIOD = 6000;
        // init scan callback
        BluetoothAdapter.LeScanCallback leScanCallback =
                (device, rssi, scanRecord) -> runOnUiThread(() -> {
                    devices.add(device);
                    deviceAdapter.notifyDataSetChanged();
                });
        handler.postDelayed(() -> {
            isScanning = false;
            bluetoothAdapter.stopLeScan(leScanCallback);
        }, SCAN_PERIOD);

        isScanning = true;
        bluetoothAdapter.startLeScan(leScanCallback);
    }

    /**
     * Connect to curDevice
     */
    private void connect() {
        bluetoothGatt = device.connectGatt(this, false, gattCallback);
    }
}
