package dev.kairos.admin.shared.ui;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;

/**
 * Notification factory. Centralizes position, duration and theme so features
 * never call {@link Notification#show} with ad-hoc settings.
 */
public final class Notifications {

    private static final int DURATION_MS = 3_000;
    private static final Notification.Position POSITION = Notification.Position.BOTTOM_END;

    private Notifications() {
    }

    public static Notification success(String message) {
        return show(message, NotificationVariant.LUMO_SUCCESS);
    }

    public static Notification error(String message) {
        return show(message, NotificationVariant.LUMO_ERROR);
    }

    public static Notification info(String message) {
        return show(message, NotificationVariant.LUMO_CONTRAST);
    }

    private static Notification show(String message, NotificationVariant variant) {
        Notification notification = new Notification(message, DURATION_MS, POSITION);
        notification.addThemeVariants(variant);
        notification.open();
        return notification;
    }
}
