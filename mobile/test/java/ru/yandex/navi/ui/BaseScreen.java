package ru.yandex.navi.ui;

import io.appium.java_client.MobileElement;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.support.PageFactory;
import ru.yandex.navi.tf.Displayable;
import ru.yandex.navi.tf.MobileUser;

import java.time.Duration;

public abstract class BaseScreen implements Displayable {
    final MobileUser user;
    private MobileElement view;

    BaseScreen() {
        this.user = MobileUser.getUser();
        PageFactory.initElements(new AppiumFieldDecorator(user.getDriver()), this);
    }

    final void setView(MobileElement view) {
        this.view = view;
    }

    @Override
    public boolean isDisplayed() {
        assert view != null;
        return MobileUser.isDisplayed(view);
    }

    public boolean isDisplayed(Duration time) {
        try {
            checkVisible(time);
            return true;
        }
        catch (NoSuchElementException ignored) {
            return false;
        }
    }

    public final void checkVisible() {
        user.shouldSee(this);
    }

    public final void checkVisible(Duration time) {
        user.waitFor(this, time);
    }

    public final BaseScreen checkVisible(final String... items) {
        user.shouldSeeAll(items);
        return this;
    }
}
