package org.lynxz.forwardsms.util;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import org.lynxz.forwardsms.R;

/**
 * 消息管理通知工具类: 8.0的app icon badge参考文档:
 * https://developer.android.com/training/notify-user/badges
 * <p>
 * 使用方法:
 * val notificationId = 100
 * val notification = NotificationUtils.getInstance(this).getNotification("title","message")
 * startForeground(NOTICE_ID, notificationId);
 * 或者
 * NotificationUtils.getInstance(this) // 若已经创建过, 则可以传null
 * .updateChannelInfo("channelId","channelName") // 更新channel信息,可选
 * .sendNotification("title","message",notificationId); // 发送消息
 */
public class NotificationUtils extends ContextWrapper {

    // 默认notificationId
    public static final int DEFAULT_NOTIFICATION_ID = 1;
    private static NotificationUtils mNotificationUtils = null;
    public String channelId = "channel_sms";
    public String channelName = "channel_name_sms";
    private NotificationManager manager;

    public NotificationUtils(Context context) {
        super(context);
    }

    public static NotificationUtils getInstance(Context context) {
        if (mNotificationUtils == null) {
            mNotificationUtils = new NotificationUtils(context.getApplicationContext());
        }
        return mNotificationUtils;
    }

    /**
     * 更新channel id和name
     * 在 {@link #sendNotification(String, String, int)} 前触发
     */
    public void updateChannelInfo(String channelId, String channelName) {
        this.channelId = channelId;
        this.channelName = channelName;
    }

    @TargetApi(Build.VERSION_CODES.O)
    public void createNotificationChannel(boolean showBadge) {
        NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);
        channel.setShowBadge(showBadge);
        NotificationManager manager = getManager();
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    private NotificationManager getManager() {
        if (manager == null) {
            manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        }
        return manager;
    }

    @TargetApi(Build.VERSION_CODES.O)
    public Notification.Builder getChannelNotification(String title, String content) {
        return new Notification.Builder(getApplicationContext(), channelId).setContentTitle(title).setContentText(content)
                .setSmallIcon(R.mipmap.ic_launcher).setAutoCancel(true);
    }

    public NotificationCompat.Builder getNotification_25(String title, String content) {
        return new NotificationCompat.Builder(getApplicationContext()).setContentTitle(title).setContentText(content)
                .setSmallIcon(R.mipmap.ic_launcher).setAutoCancel(true);
    }

    public void sendNotification(String title, String content, int notificationId) {
        this.sendNotification(title, content, null, false, 0, notificationId);
    }

    public void sendNotification(String title, String content, PendingIntent pi, int notificationId) {
        this.sendNotification(title, content, pi, false, 0, notificationId);
    }

    public int sendNotification(String title, String content, PendingIntent pi, boolean showBadge, int messageCount,
                                int notificationId) {
        NotificationManager manager = getManager();
        Notification notification = null;

        if (Build.VERSION.SDK_INT >= 26) {
            createNotificationChannel(showBadge && messageCount > 0);
            notification = getChannelNotification(title, content).setContentIntent(pi).setNumber(messageCount).build();
        } else {
            notification = getNotification_25(title, content).setContentIntent(pi).setNumber(messageCount).build();
        }

        if (notificationId <= 0) {
            notificationId = (int) (System.currentTimeMillis() % 100000);
        }

        if (manager != null && notification != null) {
            manager.notify(notificationId, notification);
        }

        return notificationId;
    }

    public Notification getNotification(String title, String content) {
        return getNotification(title, content, false, 0);
    }

    /**
     * @param title        通知栏信息标题
     * @param content      通知条内容详情
     * @param showBadge    是否要显示app icon badge
     * @param messageCount app icon badge 消息数量
     */
    public Notification getNotification(String title, String content, boolean showBadge, int messageCount) {
        NotificationManager manager = getManager();
        Notification notification = null;
        // 可以在这里设置一个默认的PendingIntent
        // Intent intent = new Intent(this, HomeActivitySdx.class);
        // PendingIntent pi = PendingIntent.getActivity(this, 0, intent,
        // PendingIntent.FLAG_UPDATE_CURRENT);

        if (Build.VERSION.SDK_INT >= 26) {
            createNotificationChannel(showBadge && messageCount > 0);
            notification = getChannelNotification(title, content)
                    // .setContentIntent(pi)
                    .setNumber(messageCount).build();
        } else {
            notification = getNotification_25(title, content)
                    // .setContentIntent(pi)
                    .setNumber(messageCount).build();
        }
        return notification;
    }

    /**
     * 取消某个通知栏消息
     *
     * @param id 大于等于0时有效
     */
    public void cancelNotification(int id) {
        NotificationManager notificationManager = getManager();
        if (notificationManager != null && id >= 0) {
            notificationManager.cancel(id);
        }
    }

    public void cancelAllNotification() {
        NotificationManager manager = getManager();
        if (manager != null) {
            manager.cancelAll();
        }
    }
}