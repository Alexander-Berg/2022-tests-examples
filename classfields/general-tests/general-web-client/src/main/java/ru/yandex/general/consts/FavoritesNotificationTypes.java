package ru.yandex.general.consts;

import lombok.Getter;

public class FavoritesNotificationTypes {

    private FavoritesNotificationTypes() {
    }

    @Getter
    public enum FavoriteSearchNotifications {

        PUSH("Push"),
        EMAIL("Email");

        private String notificationType;

        FavoriteSearchNotifications(String notificationType) {
            this.notificationType = notificationType;
        }
    }

}
