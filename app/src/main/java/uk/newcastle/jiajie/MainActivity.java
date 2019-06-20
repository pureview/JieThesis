package uk.newcastle.jiajie;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
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

import com.inuker.bluetooth.library.BluetoothClient;
import com.inuker.bluetooth.library.connect.listener.BleConnectStatusListener;
import com.inuker.bluetooth.library.connect.response.BleNotifyResponse;
import com.inuker.bluetooth.library.model.BleGattProfile;
import com.inuker.bluetooth.library.search.SearchRequest;
import com.inuker.bluetooth.library.search.SearchResult;
import com.inuker.bluetooth.library.search.response.SearchResponse;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import uk.newcastle.jiajie.service.DataService;
import uk.newcastle.jiajie.util.DecodeUtil;
import uk.newcastle.jiajie.util.StringUtil;

import static com.inuker.bluetooth.library.Constants.*;
import static uk.newcastle.jiajie.Constants.*;

public class MainActivity extends AppCompatActivity {
    private TextView tvLog, tvItemDevice, tvOut, tvCurDevice;
    private EditText etIn;
    private Button btnScan, btnSend, btnClear;
    List<SearchResult> devices = new ArrayList<>();
    private ListView deviceList;
    private SearchResult curDevice = null;
    private BaseAdapter deviceAdapter;
    private BluetoothClient btClient;
    private StringBuilder streamBuffer = new StringBuilder();
    private ServiceConnection dataConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermission();
        startDataServiec();
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
            if (curDevice != null) {
                logToFront("Disconnect from device " + curDevice.getName());
                btClient.disconnect(curDevice.getAddress());
            }
            curDevice = devices.get(position);
            Toast.makeText(MainActivity.this,
                    "Ready to connect " + curDevice,
                    Toast.LENGTH_LONG).show();
            logToFront("Ready to connect " + curDevice.getName());
            tvCurDevice.setText(curDevice.getName());
            connect();
        });
        btClient = new BluetoothClient(this);
        if (!btClient.isBluetoothOpened()) {
            toast("Please open bluetooth");
            btClient.openBluetooth();
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

    private void logToConsole(String msg) {
        Log.d("main", msg);
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


    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    /**
     * Use android official interface
     */
    private void initBle() {
        devices.clear();
        deviceAdapter.notifyDataSetChanged();
        btClient.stopSearch();
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
                devices.add(device);
                deviceAdapter.notifyDataSetChanged();
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
    private void connect() {
        btClient.connect(curDevice.getAddress(), (i, bleGattProfile) -> {
            if (i == REQUEST_SUCCESS) {
                logToFront("Connect success " + bleGattProfile.toString());
                tvCurDevice.setText(curDevice.getName() + ", mac=" + curDevice.getAddress());
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
                            logToConsole("Finish reading buffer");
                            String buff = streamBuffer.toString();
                            sendToService(buff);
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
     * Start data processing service
     */
    private void startDataServiec() {
        Intent intent = new Intent(this, DataService.class);
        startService(intent);
    }

    /**
     * Send message to data process service
     */
    private void sendToService(String msg){
        Intent intent=new Intent();
    }
}
