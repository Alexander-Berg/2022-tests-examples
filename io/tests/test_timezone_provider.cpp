#include <yandex_io/libs/http_client/mock/http_client.h>
#include <yandex_io/modules/geolocation/providers/timezone_provider.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace quasar;
using namespace testing;
using namespace YandexIO;

Y_UNIT_TEST_SUITE(testTimezoneProvider) {
    Y_UNIT_TEST(testRequest) {
        const auto mockHttpClient = std::make_shared<mock::MockHttpClient>();
        const auto provider = TimezoneProvider(mockHttpClient, "https://quasar.yandex.net");

        EXPECT_CALL(*mockHttpClient, get)
            .WillOnce([](std::string_view tag, const std::string& url, const IHttpClient::Headers& headers) {
                UNIT_ASSERT_VALUES_EQUAL(tag, "get-timezone");
                UNIT_ASSERT_VALUES_EQUAL(url, "https://quasar.yandex.net/get_timezone?lat=37.5707&lon=126.977");
                UNIT_ASSERT(headers.empty());

                return IHttpClient::HttpResponse(
                    200,
                    "application/json",
                    "{\n"
                    "   \"offset_sec\":32400,\n"
                    "   \"status\":\"ok\",\n"
                    "   \"timezone\":\"Asia/Seoul\"\n"
                    "}");
            });

        const auto timezone = provider.request(
            {.latitude = 37.57070580712891,
             .longitude = 126.97694609887695});

        UNIT_ASSERT(timezone.has_value());
        UNIT_ASSERT_VALUES_EQUAL(timezone->timezoneName, "Asia/Seoul");
        UNIT_ASSERT_VALUES_EQUAL(timezone->timezoneOffsetSec, 32400);
    }
}
