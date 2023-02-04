package ru.yandex.solomon.alert.notification.channel.telegram;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Ivan Tsybulin
 */
@ParametersAreNonnullByDefault
public class ButtonTypeTest {
    @Test
    public void buttonNumbersAreUnique() {
        for (var button : ButtonType.values()) {
            assertEquals(button, ButtonType.byNumber(button.getNumber()));
        }
    }
}
