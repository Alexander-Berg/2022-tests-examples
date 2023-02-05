package ru.yandex.navi.tf;

import io.appium.java_client.MobileElement;
import org.openqa.selenium.Rectangle;

import java.util.List;

public final class Rect {
    public Integer left;
    public Integer right;
    public Integer top;
    public Integer bottom;

    private Rect() {}

    private Rect(Rectangle rect) {
        left = rect.x;
        right = rect.x + rect.width;
        top = rect.y;
        bottom = rect.y + rect.height;
    }

    public static Rect forItems(List<MobileElement> items) {
        Rect result = null;
        for (MobileElement item : items) {
            Rectangle rect = item.getRect();
            if (result == null)
                result = new Rect(rect);
            else
                result.add(rect);
        }

        if (result == null)
            result = new Rect();

        return result;
    }

    public static Rect forItem(MobileElement item) {
        return new Rect(item.getRect());
    }

    private void add(Rectangle rect) {
        if (left != null) {
            if (left != rect.x)
                left = null;
        }

        if (right != null) {
            if (right != rect.x + rect.width)
                right = null;
        }

        if (top != null) {
            if (rect.y < top)
                top = null;
        }

        if (bottom != null) {
            if (rect.y >= bottom)
                bottom = rect.y + rect.height;
            else
                bottom = null;
        }
    }
}
