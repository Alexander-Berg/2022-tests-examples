package com.yandex.launcher;

import com.yandex.launcher.common.util.DeviceUtils;
import com.yandex.launcher.app.TestApplication;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

@RunWith(ParameterizedRobolectricTestRunner.class)
@Config(sdk = 26, manifest = Config.NONE, packageName = "com.yandex.launcher", application = TestApplication.class)
public class VersionSuffixTest {

    @ParameterizedRobolectricTestRunner.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[] { "2.1.1-qa", "qa" },
                             new Object[] { "2.1.1-dev", "dev" },
                             new Object[] { "2.1.11-dev", "dev" },
                             new Object[] { "2.1.100500-dev", "dev" },
                             new Object[] { "2-suffix", "suffix" },
                             new Object[] { "suffix", null },
                             new Object[] { "2", null },
                             new Object[] { "2-", "" },
                             new Object[] { "", null });
    }

    private final String mVersionName;
    private final String mResultSuffix;

    public VersionSuffixTest(String versionName, String resultSuffix) {
        mVersionName = versionName;
        mResultSuffix = resultSuffix;
    }

    @Test
    public void versionNameHas3Letters_lettersExtracted() {
        String result = DeviceUtils.getVersionSuffix(mVersionName);

        assertThat(result, is(equalTo(mResultSuffix)));
    }
}
