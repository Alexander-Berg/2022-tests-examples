package ru.yandex.navi;

import com.google.common.collect.ImmutableMap;
import ru.yandex.navi.tf.MobileUser;

public final class Settings {
    private final MobileUser user;

    public Settings(MobileUser user) {
        this.user = user;
    }

    public void setOfflineCacheWifiOnly(boolean value) {
        doSet("offlineCacheWifiOnly", value);
    }

    public void setSoundNotifications(String value) {
        doSet("soundNotifications", value);
    }

    public void disableRoadEvents() {
        user.openNaviUrl("set_setting?name=roadEventModes_v2&value=&client=141&signature="
            + "fNnVCG43dtCtFuYelQBdbgb4511mpZlR%2Bxc8uIFc88l%2FvFZEta%2Bn42XcJBINW%2"
            + "FKAeIoMb%2BH7QGyfnChKNt%2BaBQ%3D%3D", null);
    }

    private void doSet(String name, Object value) {
        user.openNaviUrl("set_setting", ImmutableMap.of("name", name, "value", value.toString()));
    }
}
