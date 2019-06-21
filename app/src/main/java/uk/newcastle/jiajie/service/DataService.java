package uk.newcastle.jiajie.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import uk.newcastle.jiajie.Constants;
import uk.newcastle.jiajie.MainActivity;
import uk.newcastle.jiajie.R;
import uk.newcastle.jiajie.type.ServiceStatusType;
import uk.newcastle.jiajie.util.DecodeUtil;

public class DataService extends Service {

    // Service mode
    private ServiceStatusType serviceStatus = ServiceStatusType.FREE;
    // Current label
    private String curLabel;

    @Override
    public void onCreate() {
        Toast.makeText(this, "Data service has been created",
                Toast.LENGTH_LONG).show();
        Notification.Builder builder = new Notification.Builder(this.getApplicationContext());
        Intent nfIntent = new Intent(this, MainActivity.class);
        builder.setContentIntent(PendingIntent.
                getActivity(this, 0, nfIntent, 0))
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(),
                        R.mipmap.ic_launcher))
                .setContentTitle("Data service")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentText("Processing sensor data")
                .setWhen(System.currentTimeMillis());
        Notification notification = builder.build();
        notification.defaults = Notification.DEFAULT_SOUND;
        startForeground(519, notification);
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        switch (intent.getAction()) {
            case Constants.ACTION_STREAM:
                switch (serviceStatus) {
                    case FREE:
                        logToConsole("Receive data, but I will do nothing");
                        break;
                    case LABEL:
                        processLabelData(intent.getStringExtra("data"));
                        break;
                    case PREDICT:
                        processPredictData(intent.getStringExtra("data"));
                        break;
                }
                break;
            case Constants.ACTION_LABEL:
                logToConsole("Change to label mode");
                serviceStatus = ServiceStatusType.LABEL;
                curLabel = intent.getStringExtra("label");
                break;
            case Constants.ACTION_STOP:
                logToConsole("Change to free mode");
                serviceStatus = ServiceStatusType.FREE;
            case Constants.ACTION_PREDICT:
                logToConsole("Change to predict mode");
        }

        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * Process predict data
     */
    private void processPredictData(String data) {
    }

    /**
     * Process labelling data
     */
    private void processLabelData(String data) {
        DecodeUtil.decodeBytes(data);
    }

    @Override
    public void onDestroy() {
        toast("Data service destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Toast a msg to foreground
     */
    private void toast(String msg) {
        Toast.makeText(this, msg,
                Toast.LENGTH_LONG).show();
    }

    private void logToConsole(String msg) {
        Log.e("DataService", msg);
    }
}
