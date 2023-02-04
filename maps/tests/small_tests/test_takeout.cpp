#include <maps/wikimap/ugc/account/src/lib/gdpr/takeout.h>
#include <maps/wikimap/ugc/libs/test_helpers/printers.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

#include <maps/infra/yacare/include/yacare.h>

#include <maps/libs/json/include/value.h>

namespace maps::wiki::ugc::gdpr::tests {

namespace {

const maps::chrono::TimePoint TIME_POINT = maps::chrono::parseIsoDateTime("2021-02-01T12:00:00Z");

} // namespace

Y_UNIT_TEST_SUITE(test_takeout)
{

Y_UNIT_TEST(check_category_id)
{
    const std::string emptyBody = "";
    UNIT_ASSERT_EXCEPTION(checkCategoryId(emptyBody), yacare::errors::BadRequest);

    const std::string emptyIds = R"({"id":[]})";
    UNIT_ASSERT_EXCEPTION(checkCategoryId(emptyIds), yacare::errors::BadRequest);

    const std::string wrongId = R"({"id":["some_id"]})";
    UNIT_ASSERT_EXCEPTION(checkCategoryId(wrongId), yacare::errors::BadRequest);

    const std::string correctId = R"({"id":["ugc_id"]})";
    UNIT_ASSERT_NO_EXCEPTION(checkCategoryId(correctId));

    const std::string hasCorrectId = R"({"id":["some_id", "ugc_id"]})";
    UNIT_ASSERT_NO_EXCEPTION(checkCategoryId(hasCorrectId));
}

Y_UNIT_TEST(make_takeout_error_json)
{
    const auto expected = maps::json::Value::fromString(R"(
        {
            "errors": [
                {
                    "code": "some_code",
                    "message": "something bad happened"
                }
            ],
            "status": "error"
        }
    )");

    const auto result = maps::json::Value::fromString(
        makeTakeoutErrorJson("something bad happened", "some_code"));

    UNIT_ASSERT_VALUES_EQUAL(expected, result);
}

Y_UNIT_TEST(make_takeout_status_response_json)
{
    const auto expected = maps::json::Value::fromString(R"(
        {
            "data": [
                {
                    "id": "ugc_id",
                    "slug": "ugc_data",
                    "state": "empty",
                    "update_date": "2021-02-01T12:00:00Z"
                }
            ],
            "status": "ok"
        }
    )");

    const auto result = maps::json::Value::fromString(
        makeTakeoutStatusResponseJson({TakeoutState::Empty, TIME_POINT}));

    UNIT_ASSERT_VALUES_EQUAL(expected, result);
}

}

} // namespace maps::wiki::ugc::gdpr::tests
