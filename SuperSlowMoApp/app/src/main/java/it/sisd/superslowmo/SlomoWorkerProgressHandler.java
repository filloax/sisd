package it.sisd.superslowmo;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.work.Data;
import androidx.work.ForegroundInfo;
import androidx.work.WorkManager;

public class SlomoWorkerProgressHandler implements IProgressHandler{

    private Context context;
    private SlomoWorker worker;

    private NotificationManager notificationManager;
    private String progress_tag;
    private String progress_message_tag;

    public SlomoWorkerProgressHandler(Context context, SlomoWorker worker, String progress_tag, String progress_message_tag){
        this.context = context;
        this.worker = worker;
        this.progress_tag = progress_tag;
        this.progress_message_tag = progress_message_tag;
    }

    @Override
    public void publishProgress(float progress) {
        worker.setProgressAsync(new Data.Builder().putFloat(progress_tag, progress).build());
        worker.setForegroundAsync(createForegroundInfo(progress));
    }

    @Override
    public void publishProgress(float progress, String message) {
        worker.setProgressAsync(new Data.Builder().putFloat(progress_tag, progress).putString(progress_message_tag, message).build());
        worker.setForegroundAsync(createForegroundInfo(progress));
    }

    @NonNull
    protected ForegroundInfo createForegroundInfo(@NonNull float progress) {
        // Build or Update a notification with progress bar

        String title = context.getString(R.string.notification_ongoing_title);
        String cancel = context.getString(R.string.cancel_elaboration);
        // This PendingIntent can be used to cancel the worker
        PendingIntent intent = WorkManager.getInstance(context)
                .createCancelPendingIntent(worker.getId());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }

        Notification notification = new NotificationCompat.Builder(context, context.getString(R.string.notification_channel_id))
                .setContentTitle(title)
                .setTicker(title)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setProgress(100, (int)(progress * 100), false)
                // Add the cancel action to the notification which can
                // be used to cancel the worker
                .addAction(android.R.drawable.ic_delete, cancel, intent)
                .build();

        return new ForegroundInfo(context.getResources().getInteger(R.integer.notification_id), notification);
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        CharSequence name = context.getString(R.string.notification_channel_name);
        String description = context.getString(R.string.notification_channel_description);
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel(context.getString(R.string.notification_channel_id), name, importance);
        channel.setDescription(description);
        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
        notificationManager = context.getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }
}
