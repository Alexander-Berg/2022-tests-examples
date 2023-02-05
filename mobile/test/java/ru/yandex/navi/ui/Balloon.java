package ru.yandex.navi.ui;

import io.appium.java_client.MobileElement;
import ru.yandex.navi.tf.MobileUser;

import java.util.ArrayList;
import java.util.List;

public final class Balloon extends MapObject {
    private Balloon(String imageName) {
        this(imageName, null);
    }

    private Balloon(String imageName, MobileElement image) {
        super(imageName, image);
    }

    public static Balloon getActiveVariantBalloon() {
        return new Balloon("var_balloon_blue");
    }

    public static List<Balloon> getVariantBalloons() {
        final MobileUser user = MobileUser.getUser();

        List<Balloon> balloons = new ArrayList<>();
        final String[] imageNames = {"var_balloon_blue", "var_balloon_blue_h", "var_balloon_red"};
        for (String imageName : imageNames) {
            for (MobileElement image : user.findElementsByImage(imageName)) {
                balloons.add(new Balloon(imageName, image));
            }
        }

        return balloons;
    }
}
