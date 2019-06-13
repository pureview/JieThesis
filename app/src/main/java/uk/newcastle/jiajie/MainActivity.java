package uk.newcastle.jiajie;

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


import java.util.ArrayList;
import java.util.List;

import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import uk.newcastle.jiajie.util.StringUtil;

import static uk.newcastle.jiajie.Constants.*;

public class MainActivity extends AppCompatActivity {
    private TextView tvLog, tvItemDevice, tvOut, tvCurDevice;
    private EditText etIn;
    private Button btnScan, btnSend, btnClear;
    private ListView deviceList;
    private RxBleClient btClient;
    private RxBleDevice curDevice = null;
    private RxBleConnection connection = null;

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvLog = findViewById(R.id.tv_log);
        btnScan = findViewById(R.id.btn_scan);
        btnSend = findViewById(R.id.btn_send);
        deviceList = findViewById(R.id.device_list);
        tvOut = findViewById(R.id.tv_out);
        tvCurDevice = findViewById(R.id.tv_cur_device);
        etIn = findViewById(R.id.et_in);
        btnClear = findViewById(R.id.btn_clear);
        // Init scan button
        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Begin to scan ble devices!", Toast.LENGTH_LONG).show();
                // Initialize bluetooth manager
                initBle();
            }
        });
        // Init message send button
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (curDevice == null) {
                    Toast.makeText(MainActivity.this, "There is no device connected.",
                            Toast.LENGTH_LONG).show();
                } else {
                    // Send message
                    String toBeSent = StringUtil.escap(etIn.getText().toString());
                    curDevice.establishConnection(false)
                            .flatMapSingle(o -> {
                                return o.writeCharacteristic(writeUUID, toBeSent.getBytes());
                            })
                            .subscribe(o -> {
                                Toast.makeText(MainActivity.this, "Send success",
                                        Toast.LENGTH_LONG).show();
                                curDevice.establishConnection(false)
                                        .flatMapSingle(r -> r.readCharacteristic(readUUID))
                                        .subscribe(r -> {
                                            tvOut.setText(r.toString());
                                        }).dispose();
                            }).dispose();
                }
            }
        });
        // Init clear button
        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tvLog.setText("");
            }
        });
    }

    /**
     * Initialize bluetooth manager
     */
    private void initBle() {
        final List<RxBleDevice> devices = new ArrayList<>();
        btClient = RxBleClient.create(MainActivity.this);
        Disposable scanSubscription = btClient.scanBleDevices()
                .subscribe(new Consumer<RxBleScanResult>() {
                    @Override
                    public void accept(RxBleScanResult o) throws Exception {
                        devices.add(o.getBleDevice());
                        Log.d(TAG, "onComplete: search");
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
                                tvItemDevice.setText(devices.get(position).getName());
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
                            tvLog.setText(tvLog.getText() + "\nReady to connect " + curDevice.getName());
                            tvCurDevice.setText(curDevice.getName());
                            curDevice.establishConnection(false)
                                    .flatMap(rxBleConnection ->
                                            rxBleConnection.setupNotification(readUUID))
                                    .doOnNext(notifyObservable -> {
                                    })
                                    .flatMap(o1 -> o1)
                                    .subscribe(o1 -> {
                                        tvOut.setText(tvOut.getText() + "\n" + o1.toString());
                                    }).dispose();

                        });
                        tvLog.setText(tvLog.getText() + "\n\n" + "complete");
                    }
                });
        scanSubscription.dispose();

    }
}
