package ru.yandex.solomon.alert.domain;

import java.util.concurrent.ThreadLocalRandom;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Vladimir Gordiychuk
 */
public class AbstractAlertTest {

    @Test
    public void equalContentSame() {
        var alert = AlertTestSupport.randomAlert();
        assertTrue(alert.equalContent(alert));
    }

    @Test
    public void equalContentIgnoreModifyField() {
        var alert = AlertTestSupport.randomAlert();
        assertTrue(alert.equalContent(alert.toBuilder().setUpdatedBy("my_test_user_1").build()));
        assertTrue(alert.equalContent(alert.toBuilder().setUpdatedAt(ThreadLocalRandom.current().nextLong()).build()));
        assertTrue(alert.equalContent(alert.toBuilder().setCreatedAt(ThreadLocalRandom.current().nextLong()).build()));
        assertTrue(alert.equalContent(alert.toBuilder().setCreatedBy("my_test_user_2").build()));
        assertTrue(alert.equalContent(alert.toBuilder().setVersion(424242).build()));
    }

    @Test
    public void notEqualContent() {
        for (int index = 0; index < 1000; index++) {
            var left = AlertTestSupport.randomAlert();
            var right = AlertTestSupport.randomAlert()
                .toBuilder()
                .setId(left.getId())
                .setProjectId(left.getId())
                .setFolderId(left.getFolderId())
                .setCreatedAt(left.getCreatedAt())
                .setCreatedBy(left.getCreatedBy())
                .setUpdatedBy(left.getUpdatedBy())
                .setUpdatedAt(left.getUpdatedAt())
                .setVersion(left.getVersion())
                .build();

            assertFalse(left + " != " + right, left.equalContent(right));
        }
    }
}
