#include <yandex_io/libs/appmetrica/app_metrica.h>

#include <yandex_io/libs/json_utils/json_utils.h>

#include <json/json.h>

#include <library/cpp/testing/unittest/registar.h>

#include <string>
#include <vector>

using namespace quasar;

Y_UNIT_TEST_SUITE(MetricaMetadataTest) {
    Y_UNIT_TEST(TestJsonConvertion) {
        const auto initialMetadata = MetricaMetadata{
            .UUID = "some_uuid",
            .deviceID = "some_device_id",
        };

        const auto metadataJson{initialMetadata.toJson()};

        UNIT_ASSERT_VALUES_EQUAL(metadataJson["UUID"].asString(), initialMetadata.UUID);
        UNIT_ASSERT_VALUES_EQUAL(metadataJson["deviceID"].asString(), initialMetadata.deviceID);

        const auto parsedMetadata = MetricaMetadata::fromJson(metadataJson);
        UNIT_ASSERT_EQUAL(initialMetadata, parsedMetadata);
    }

    Y_UNIT_TEST(TestFromReportConfig) {
        const auto validMetadata = MetricaMetadata{
            .UUID = "some_uuid",
            .deviceID = "some_device_id",
        };

        const auto reportConfig = ReportConfiguration(
            0,
            std::vector<std::string>{},
            StartupConfiguration{},
            validMetadata.UUID,
            validMetadata.deviceID);

        const auto metadata = MetricaMetadata::fromReportConfig(reportConfig);
        UNIT_ASSERT_EQUAL(validMetadata, metadata);
    }
}
