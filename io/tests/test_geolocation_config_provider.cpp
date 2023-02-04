#include <yandex_io/modules/geolocation/providers/geolocation_config_provider.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace YandexIO;

Y_UNIT_TEST_SUITE(testGeolocationConfigProvider) {
    Y_UNIT_TEST(testOverridingByDeviceConfig) {
        GeolocationConfigProvider provider;

        const auto config = "{\n"
                            "    \"latitude\": 38.716182,\n"
                            "    \"longitude\": -9.141589\n"
                            "}";

        Location location = {.latitude = 38.716182,
                             .longitude = -9.141589};

        UNIT_ASSERT(!provider.getLocation().has_value());
        UNIT_ASSERT(!provider.getTimezone().has_value());

        UNIT_ASSERT(provider.onDeviceConfig("location", config) == GeolocationConfigProvider::State::HAS_UPDATE);
        UNIT_ASSERT(provider.onDeviceConfig("location", config) == GeolocationConfigProvider::State::NO_UPDATE);
        UNIT_ASSERT(provider.getLocation().has_value());
        UNIT_ASSERT(!provider.getTimezone().has_value());
        UNIT_ASSERT(provider.getLocation() == location);

        UNIT_ASSERT(provider.onDeviceConfig("location", "{}") == GeolocationConfigProvider::State::HAS_UPDATE);
        UNIT_ASSERT(!provider.getLocation().has_value());
        UNIT_ASSERT(!provider.getTimezone().has_value());
    }

    Y_UNIT_TEST(testOverridingBySystemConfig) {
        GeolocationConfigProvider provider;
        Location location = {.latitude = -33.86,
                             .longitude = 151.21};
        Timezone timezone = {.timezoneName = "Australia/Sydney",
                             .timezoneOffsetSec = 36000};
        const auto configWithTimezone = "{\n"
                                        "   \"timezone\": {\n"
                                        "      \"offset_sec\": 36000,\n"
                                        "      \"timezone_name\": \"Australia/Sydney\"\n"
                                        "   }\n"
                                        "}";

        const auto configWithLocation = "{\n"
                                        "   \"location\": {\n"
                                        "      \"latitude\": -33.86,\n"
                                        "      \"longitude\": 151.21\n"
                                        "   }\n"
                                        "}";
        const auto config = "{\n"
                            "   \"location\": {\n"
                            "      \"latitude\": -33.86,\n"
                            "      \"longitude\": 151.21\n"
                            "   },\n"
                            "   \"timezone\": {\n"
                            "      \"offset_sec\": 36000,\n"
                            "      \"timezone_name\": \"Australia/Sydney\"\n"
                            "   }\n"
                            "}";

        UNIT_ASSERT(!provider.getLocation().has_value());
        UNIT_ASSERT(!provider.getTimezone().has_value());

        UNIT_ASSERT(provider.onSystemConfig("locationdSettings", configWithLocation) == GeolocationConfigProvider::State::HAS_UPDATE);
        UNIT_ASSERT(provider.getLocation().has_value());
        UNIT_ASSERT(!provider.getTimezone().has_value());
        UNIT_ASSERT(provider.getLocation() == location);

        UNIT_ASSERT(provider.onSystemConfig("locationdSettings", configWithTimezone) == GeolocationConfigProvider::State::HAS_UPDATE);
        UNIT_ASSERT(!provider.getLocation().has_value());
        UNIT_ASSERT(provider.getTimezone().has_value());
        UNIT_ASSERT(provider.getTimezone() == timezone);

        UNIT_ASSERT(provider.onSystemConfig("locationdSettings", config) == GeolocationConfigProvider::State::HAS_UPDATE);
        UNIT_ASSERT(provider.onSystemConfig("locationdSettings", config) == GeolocationConfigProvider::State::NO_UPDATE);
        UNIT_ASSERT(provider.getLocation().has_value());
        UNIT_ASSERT(provider.getTimezone().has_value());
        UNIT_ASSERT(provider.getLocation() == location);
        UNIT_ASSERT(provider.getTimezone() == timezone);

        UNIT_ASSERT(provider.onSystemConfig("locationdSettings", "{}") == GeolocationConfigProvider::State::HAS_UPDATE);
        UNIT_ASSERT(!provider.getLocation().has_value());
        UNIT_ASSERT(!provider.getTimezone().has_value());
    }

    Y_UNIT_TEST(testOverridingDeviceConfigBySystemConfig) {
        GeolocationConfigProvider provider;

        Location locationFromDeviceConfig = {.latitude = 38.716182,
                                             .longitude = -9.141589};
        Location locationFromSystemConfig = {.latitude = -33.86,
                                             .longitude = 151.21};
        Timezone timezoneFromSystemConfig = {.timezoneName = "Australia/Sydney",
                                             .timezoneOffsetSec = 36000};

        const auto deviceConfig = "{\n"
                                  "    \"latitude\": 38.716182,\n"
                                  "    \"longitude\": -9.141589\n"
                                  "}";
        const auto systemConfig = "{\n"
                                  "   \"location\": {\n"
                                  "      \"latitude\": -33.86,\n"
                                  "      \"longitude\": 151.21\n"
                                  "   },\n"
                                  "   \"timezone\": {\n"
                                  "      \"offset_sec\": 36000,\n"
                                  "      \"timezone_name\": \"Australia/Sydney\"\n"
                                  "   }\n"
                                  "}";

        UNIT_ASSERT(!provider.getLocation().has_value());
        UNIT_ASSERT(!provider.getTimezone().has_value());

        UNIT_ASSERT(provider.onDeviceConfig("location", deviceConfig) == GeolocationConfigProvider::State::HAS_UPDATE);
        UNIT_ASSERT(provider.getLocation().has_value());
        UNIT_ASSERT(!provider.getTimezone().has_value());
        UNIT_ASSERT(provider.getLocation() == locationFromDeviceConfig);

        UNIT_ASSERT(provider.onSystemConfig("locationdSettings", systemConfig) == GeolocationConfigProvider::State::HAS_UPDATE);
        UNIT_ASSERT(provider.getLocation().has_value());
        UNIT_ASSERT(provider.getTimezone().has_value());
        UNIT_ASSERT(provider.getLocation() == locationFromSystemConfig);
        UNIT_ASSERT(provider.getTimezone() == timezoneFromSystemConfig);

        UNIT_ASSERT(provider.onSystemConfig("locationdSettings", "{}") == GeolocationConfigProvider::State::HAS_UPDATE);
        UNIT_ASSERT(provider.getLocation().has_value());
        UNIT_ASSERT(!provider.getTimezone().has_value());
        UNIT_ASSERT(provider.getLocation() == locationFromDeviceConfig);

        UNIT_ASSERT(provider.onDeviceConfig("location", "{}") == GeolocationConfigProvider::State::HAS_UPDATE);
        UNIT_ASSERT(!provider.getLocation().has_value());
        UNIT_ASSERT(!provider.getTimezone().has_value());
    }
}
