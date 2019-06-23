package uk.newcastle.jiajie;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import uk.newcastle.jiajie.service.DataService;
import uk.newcastle.jiajie.util.StringUtil;

import com.ashokvarma.bottomnavigation.BottomNavigationBar;
import com.ashokvarma.bottomnavigation.BottomNavigationItem;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import static uk.newcastle.jiajie.Constants.*;

public class MainActivity extends AppCompatActivity {
    private TextView tvLog, tvItemDevice, tvOut, tvCurDevice;
    private EditText etIn;
    private Button btnScan, btnSend, btnClear;
    private ListView deviceList;
    private BaseAdapter deviceAdapter;
    private ServiceConnection dataConnection;
    private BroadcastReceiver broadcastReceiver;
    private List<String> devices = new ArrayList<>();
    private BottomNavigationBar navigationView;
    private ScrollView containerHome, containerLabel, containerPredict;
    private LineChart labelChart, predictChart;
    private Button labelStart, labelStop, labelRevert, labelData;
    private TextView tvLabelLog, tvPredictTitle, tvLabelTitle;
    private EditText etLabel;
    private Button btnTrain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initReceiver();
        requestPermission();
        startDataService();
        initContainers();
        initHomeWidgets();
        initLabelWidgets();
        initPredictWidgets();
    }

    private void initPredictWidgets() {
        predictChart = findViewById(R.id.chart_predict);
        tvPredictTitle = findViewById(R.id.tv_predict_title);
        btnTrain = findViewById(R.id.btn_train);
        btnTrain.setOnClickListener(v -> {
            toast("Begin training. Please switch to home tab for logs");
            sendCommand(TRAIN, "");
        });
    }

    private void initLabelWidgets() {
        labelChart = findViewById(R.id.chart_label);
        labelStart = findViewById(R.id.btn_label_begin);
        tvLabelTitle = findViewById(R.id.tv_label_title);
        etLabel = findViewById(R.id.et_label);
        labelStart.setOnClickListener(v -> {
            if (etLabel.getText().toString().length() == 0) {
                toast("Please input a label");
            } else {
                toast("Labelling now starts");
                sendCommand(ACTION_LABEL, etLabel.getText().toString());
            }
        });
        labelStop = findViewById(R.id.btn_label_stop);
        labelStop.setOnClickListener(o -> {
            sendCommand(ACTION_STOP, "");
        });
        labelRevert = findViewById(R.id.btn_label_revert);
        labelRevert.setOnClickListener(v -> {
            new AlertDialog.Builder(MainActivity.this)
                    .setPositiveButton("Yes", (dialog, which) -> sendCommand(REVERT, ""))
                    .setNegativeButton("No", (dialog, which) -> {

                    })
                    .setTitle("Are you sure revert labelling data?")
                    .create().show();
        });
        tvLabelLog = findViewById(R.id.tv_label_log);
        labelData = findViewById(R.id.btn_label_check_data);
        labelData.setOnClickListener(v -> {
            String[] currentFiles = fileList();
            StringBuilder sb = new StringBuilder();
            for (String s : currentFiles) {
                sb.append(s).append('\n');
            }
            tvLabelLog.setText(sb.toString());
        });
    }

    /**
     * Init container of different tabs
     */
    private void initContainers() {
        containerHome = findViewById(R.id.home_container);
        containerLabel = findViewById(R.id.label_container);
        containerPredict = findViewById(R.id.predict_container);
        navigationView = findViewById(R.id.nav_view);
        navigationView.addItem(new BottomNavigationItem(R.drawable.ic_home_black_24dp, "Home"));
        navigationView.addItem(new BottomNavigationItem(R.drawable.ic_label, "Label"));
        navigationView.addItem(new BottomNavigationItem(R.drawable.ic_predict, "Predict"));
        navigationView.initialise();
        navigationView.setTabSelectedListener(new BottomNavigationBar.OnTabSelectedListener() {
            @Override
            public void onTabSelected(int i) {
switch (i) {
                    case 0:
                        containerHome.setVisibility(View.VISIBLE);
                        containerLabel.setVisibility(View.GONE);
                        containerPredict.setVisibility(View.GONE);
                        break;
                    case 1:
                        containerHome.setVisibility(View.GONE);
                        containerLabel.setVisibility(View.VISIBLE);
                        containerPredict.setVisibility(View.GONE);
                        break;
                    case 2:
                        containerHome.setVisibility(View.GONE);
                        containerLabel.setVisibility(View.GONE);
                        containerPredict.setVisibility(View.VISIBLE);
                        break;
                }
            }

            @Override
            public void onTabUnselected(int i) {

            }

            @Override
            public void onTabReselected(int i) {
            }
        });
        navigationView.selectTab(0);
    }

    /**
     * Init main tab widgets
     */
    private void initHomeWidgets() {
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
                String deviceName = devices.get(position);
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
            logToFront("Ready to connect " + devices.get(position));
            sendCommand(CONNECT_DEVICE, String.valueOf(position));
        });

        // Init scan button
        btnScan.setOnClickListener(v -> {
            Toast.makeText(MainActivity.this, "Begin to scan ble devices!", Toast.LENGTH_LONG).show();
            // Initialize bluetooth manager
            sendCommand(SCAN, "");
        });
        // Init message send button
        btnSend.setOnClickListener(v -> {
            // Send message
            String toBeSent = StringUtil.escap(etIn.getText().toString());
            sendCommand(MAIN_ACTION_CMD, toBeSent);
        });
        // Init clear button
        btnClear.setOnClickListener(v -> tvLog.setText(""));
    }

    /**
     * Send command to data service
     */
    private void sendCommand(String cmd, String data) {
        Intent intent = new Intent(this, DataService.class);
        intent.setAction(cmd);
        intent.putExtra(MAIN_ACTION_DATA, data);
        startService(intent);
    }

    /**
     * Accept broadcast from service
     */
    private void initReceiver() {
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                logToConsole("Receive cmd:" + intent.getAction() + "|" + intent.getStringExtra(MAIN_ACTION_DATA));
                switch (intent.getAction()) {
                    case MAIN_ACTION_LOG:
                        logToFront(intent.getStringExtra(MAIN_ACTION_DATA));
                        break;
                    case MAIN_ACTION_CMD:
                        switch (intent.getStringExtra(MAIN_ACTION_CMD)) {
                            case CLEAR:
                                devices.clear();
                                deviceAdapter.notifyDataSetChanged();
                                break;
                            case DEVICE_FOUND:
                                devices.add(intent.getStringExtra(MAIN_ACTION_DATA));
                                deviceAdapter.notifyDataSetChanged();
                                break;
                            case CONNECT_DEVICE:
                                tvCurDevice.setText(intent.getStringExtra(MAIN_ACTION_DATA));
                                break;
                            default:
                                logToFront("[receive] Action not recognized " + intent.getStringExtra(MAIN_ACTION_CMD));
                        }
                        break;
                    case LABEL_DRAW:
                        draw(intent.getStringExtra(LABEL_DRAW), labelChart);
                        tvLabelTitle.setText(intent.getStringExtra(TITLE));
                        break;
                    case PREDICT_DRAW:
                        draw(intent.getStringExtra(LABEL_DRAW), predictChart);
                        tvPredictTitle.setText(intent.getStringExtra(TITLE));
                        break;
                    default:
                        logToFront("[receive] Action not recognized " + intent.getAction());
                }
            }
        };
        IntentFilter filter = new IntentFilter(MAIN_ACTION_CMD);
        this.registerReceiver(broadcastReceiver, filter);
    }

    private void draw(String stringExtra, LineChart chart) {
        List<Entry> entryX = new ArrayList<>();
        List<Entry> entryY = new ArrayList<>();
        List<Entry> entryZ = new ArrayList<>();
        int ind = 0;
        for (String line : stringExtra.split("\n")) {
            entryX.add(new Entry(ind, Integer.valueOf(line.split(",")[0])));
            entryY.add(new Entry(ind, Integer.valueOf(line.split(",")[1])));
            entryZ.add(new Entry(ind, Integer.valueOf(line.split(",")[2])));
        }
        LineDataSet dataSetX = new LineDataSet(entryX, "sensorX");
        dataSetX.setColor(Color.BLUE);
        LineDataSet dataSetY = new LineDataSet(entryY, "sensorY");
        dataSetY.setColor(Color.GREEN);
        LineDataSet dataSetZ = new LineDataSet(entryZ, "sensorZ");
        dataSetZ.setColor(Color.RED);
        LineData lineData = new LineData();
        lineData.addDataSet(dataSetX);
        lineData.addDataSet(dataSetY);
        lineData.addDataSet(dataSetZ);
        chart.setData(lineData);
        chart.invalidate();
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
        Log.e("main", msg);
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    /**
     * Start data processing service
     */
    private void startDataService() {
        Intent intent = new Intent(this, DataService.class);
        startService(intent);
    }

}
