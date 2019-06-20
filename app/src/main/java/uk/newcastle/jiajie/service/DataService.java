package uk.newcastle.jiajie.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.widget.Toast;

import uk.newcastle.jiajie.MainActivity;
import uk.newcastle.jiajie.R;

public class DataService extends IntentService {
    public DataService() {
        super("DataService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "Data service has started",
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
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "Data service destroyed",
                Toast.LENGTH_SHORT).show();
    }
}
