package com.yandex.bitbucket.plugin.utils;

import com.yandex.bitbucket.plugin.buildmanager.entity.RepoPrCompositeKey;
import org.junit.Test;

import static ru.yandex.bitbucket.plugin.testutil.TestUtils.assertSerializable;

public class RepoPrCompositeKeyTest {
    @Test
    public void isSerializable() {
        assertSerializable(new RepoPrCompositeKey(1, 1L));
    }
}
