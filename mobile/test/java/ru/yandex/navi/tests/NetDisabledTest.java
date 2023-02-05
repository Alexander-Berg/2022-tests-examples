package ru.yandex.navi.tests;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import ru.yandex.navi.tf.RetryRunner;
import ru.yandex.navi.categories.*;

import java.time.Duration;

@RunWith(RetryRunner.class)
@Category(SkipIos.class)
public final class NetDisabledTest extends BaseTest {
    @Override
    void doBeforeStart() {
        user.setAirplaneMode(true);
    }

    @Override
    void doEnd() {
        user.setAirplaneMode(false);
    }

    @Test
    public void launch() {
    }

    @Test
    @Ignore("MOBNAVI-23917")
    public void search() {
        tabBar.clickSearch().clickSearch().typeText("Зеленоград")
                .expectError("Нет соединения", Duration.ofSeconds(30));
    }
}
