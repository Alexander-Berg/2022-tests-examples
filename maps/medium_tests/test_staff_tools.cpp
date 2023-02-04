#include <maps/wikimap/feedback/api/src/synctool/lib/staff_tools.h>

#include <maps/libs/http/include/test_utils.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

#include <map>

namespace maps::wiki::feedback::api::sync::tests {

Y_UNIT_TEST_SUITE(test_staff_tools)
{

Y_UNIT_TEST(test_is_staff_user)
{
    int triggerCounter = 0;
    auto staffMock = maps::http::addMock(
        "https://staff-api.test.yandex-team.ru/v3/persons",
        [&triggerCounter](const maps::http::MockRequest& request) {
            ++triggerCounter;

            auto params = request.url.params();
            if (params.find("staffemail%40yandex.ru") != std::string::npos) {
                return maps::http::MockResponse::fromFile(
                    SRC_("data/staff_api_nonempty_response.json"));
            } else if (params.find("otheremail%40yandex.ru") != std::string::npos) {
                return maps::http::MockResponse::fromFile(
                    SRC_("data/staff_api_empty_response.json"));
            } else if (params.find("dismissedemail%40yandex.ru") != std::string::npos) {
                return maps::http::MockResponse::fromFile(
                    SRC_("data/staff_api_dismissed_response.json"));
            }
            UNIT_ASSERT(false && "Wrong params in request to staff-api");
            return maps::http::MockResponse{};
        });

    const auto staffClient = staff::Client(maps::json::Value::fromString(R"({
        "yandexHosts": {
            "staff": "https://staff-api.test.yandex-team.ru"
        }
    })"));

    UNIT_ASSERT(isStaffUser(staffClient, "StaffEmail@yandex.ru"));
    UNIT_ASSERT(!isStaffUser(staffClient, "OtherEmail@yandex.ru"));
    UNIT_ASSERT(!isStaffUser(staffClient, "DismissedEmail@yandex.ru"));
    UNIT_ASSERT_VALUES_EQUAL(triggerCounter, 3);
}

} // test_staff_tools suite

} // namespace maps::wiki::feedback::api::sync::tests
