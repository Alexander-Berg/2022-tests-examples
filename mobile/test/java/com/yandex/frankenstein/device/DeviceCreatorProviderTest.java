package com.yandex.frankenstein.device;

import com.yandex.frankenstein.device.android.AndroidDeviceCreator;
import com.yandex.frankenstein.device.dummy.DummyDeviceCreator;
import com.yandex.frankenstein.device.ios.physical.IosPhysicalDeviceCreator;
import com.yandex.frankenstein.device.ios.simulator.IosSimulatorDeviceCreator;
import com.yandex.frankenstein.settings.DeviceSettings.DeviceType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class DeviceCreatorProviderTest {

    private static final Map<String, String> CAPABILITIES = new HashMap<>();
    private static final String CASE_REQUEST_URL = "some_case_request_url";

    private final DeviceCreatorProvider<String> mCreatorProvider =
            new DeviceCreatorProvider<>(CAPABILITIES, CASE_REQUEST_URL);

    @Mock private DeviceCreator<String> mTestCreator;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test(expected = IllegalStateException.class)
    public void testGetIllegalDeviceCreator() {
        mCreatorProvider.get("wrong_platform", "wrong_device_type");
    }

    @Test
    public void testGetNewDeviceCreator() {
        final String platform = "new_platform";
        final String deviceType = "new_device_type";
        mCreatorProvider.add(platform, deviceType, mTestCreator);
        final DeviceCreator creator = mCreatorProvider.get(platform, deviceType);
        assertThat(creator).isSameAs(mTestCreator);
    }

    @Test
    public void testGetRedefinedDeviceCreator() {
        mCreatorProvider.add(Platform.ANDROID, null, mTestCreator);
        final DeviceCreator creator = mCreatorProvider.get(Platform.ANDROID, null);
        assertThat(creator).isSameAs(mTestCreator);
    }

    @RunWith(Parameterized.class)
    public static class GetDefault {

        private final DeviceCreatorProvider mCreatorProvider =
                new DeviceCreatorProvider(CAPABILITIES, CASE_REQUEST_URL);

        @Parameterized.Parameter
        public String mPlatform;

        @Parameterized.Parameter(value = 1)
        public String mDeviceType;

        @Parameterized.Parameter(value = 2)
        public Class mExpectedClass;

        @Parameterized.Parameters(name = "{0}, {1}, {2}")
        public static Collection<Object[]> data() {
            return Arrays.asList(
                    new Object[]{Platform.DUMMY, null, DummyDeviceCreator.class},
                    new Object[]{Platform.DUMMY, DeviceType.VIRTUAL, DummyDeviceCreator.class},
                    new Object[]{Platform.DUMMY, DeviceType.PHYSICAL, DummyDeviceCreator.class},

                    new Object[]{Platform.IOS, DeviceType.PHYSICAL, IosPhysicalDeviceCreator.class},
                    new Object[]{Platform.IOS, DeviceType.VIRTUAL, IosSimulatorDeviceCreator.class},
                    new Object[]{Platform.ANDROID, null, AndroidDeviceCreator.class},

                    new Object[]{Platform.UNITY_IOS, DeviceType.PHYSICAL, IosPhysicalDeviceCreator.class},
                    new Object[]{Platform.UNITY_IOS, DeviceType.VIRTUAL, IosSimulatorDeviceCreator.class},
                    new Object[]{Platform.UNITY_ANDROID, null, AndroidDeviceCreator.class},

                    new Object[]{Platform.XAMARIN_IOS, DeviceType.PHYSICAL, IosPhysicalDeviceCreator.class},
                    new Object[]{Platform.XAMARIN_IOS, DeviceType.VIRTUAL, IosSimulatorDeviceCreator.class},
                    new Object[]{Platform.XAMARIN_ANDROID, null, AndroidDeviceCreator.class},

                    new Object[]{Platform.CORDOVA_ANDROID, null, AndroidDeviceCreator.class},

                    new Object[]{Platform.REACT_NATIVE_IOS, DeviceType.PHYSICAL, IosPhysicalDeviceCreator.class},
                    new Object[]{Platform.REACT_NATIVE_IOS, DeviceType.VIRTUAL, IosSimulatorDeviceCreator.class},
                    new Object[]{Platform.REACT_NATIVE_ANDROID, null, AndroidDeviceCreator.class},

                    new Object[]{Platform.FLUTTER_IOS, DeviceType.PHYSICAL, IosPhysicalDeviceCreator.class},
                    new Object[]{Platform.FLUTTER_IOS, DeviceType.VIRTUAL, IosSimulatorDeviceCreator.class},
                    new Object[]{Platform.FLUTTER_ANDROID, null, AndroidDeviceCreator.class}
                    );
        }

        @Test
        public void testGetDefaultDeviceCreator() {
            final DeviceCreator creator = mCreatorProvider.get(mPlatform, mDeviceType);
            assertThat(creator).isInstanceOf(mExpectedClass);
        }
    }
}
