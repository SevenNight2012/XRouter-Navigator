package com.mrcd.xrouter.nav.notification;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * 展示通知
 */
public class SimpleNotification {

    private static final String NOTIFICATION_DISPLAY_ID = "XRouter-Navigation";

    private final NotificationGroup mNotificationGroup = new NotificationGroup(NOTIFICATION_DISPLAY_ID, NotificationDisplayType.BALLOON, true);

    /**
     * 捕获一个异常，然后展示通知
     *
     * @param throwable throwable对象
     * @param project   Project对象
     */
    public void showError(Throwable throwable, Project project) {
        StringWriter writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        throwable.printStackTrace(printWriter);
        printWriter.flush();
        Notification notification = mNotificationGroup.createNotification(writer.toString(), NotificationType.ERROR);
        notification.notify(project);
    }

    /**
     * 展示一个错误信息
     *
     * @param message 消息内容
     * @param project Project对象
     */
    public void showError(String message, Project project) {
        Notification notification = mNotificationGroup.createNotification(message, NotificationType.ERROR);
        notification.notify(project);
    }

    /**
     * 展示一个普通消息的通知
     *
     * @param message 消息内容
     * @param project Project对象
     */
    public void showInfo(String message, Project project) {
        Notification notification = mNotificationGroup.createNotification(message, NotificationType.INFORMATION);
        notification.notify(project);
    }

}
