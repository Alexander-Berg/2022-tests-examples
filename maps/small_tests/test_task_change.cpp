#include <maps/wikimap/feedback/api/src/libs/common/task_change.h>
#include <maps/wikimap/feedback/api/src/libs/dbqueries/constants.h>
#include <maps/libs/common/include/file_utils.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::feedback::api::tests {

Y_UNIT_TEST_SUITE(test_task_change)
{

// TODO: validate result with json schema from
// https://a.yandex-team.ru/review/1321206/files#file-0-38935406

Y_UNIT_TEST(task_change_to_json)
{
    TaskChange change{
        TaskChangeId{1},
        TaskId{"9a8d36c4-3f44-4b39-8e55-75d6233adb03"},
        TaskStatus::New,
        {Service::Nmaps},
        std::nullopt, //message,
        maps::chrono::parseIsoDateTime("2020-03-26 09:16:25+03")
    };
    const auto expectedJson = maps::common::readFileToString(SRC_("data/change1.json"));
    maps::json::Builder builder;
    builder << change;
    UNIT_ASSERT_VALUES_EQUAL(builder.str(), expectedJson);
}

Y_UNIT_TEST(task_change_to_json2)
{
    TaskChange change{
        TaskChangeId{1},
        TaskId{"9a8d36c4-3f44-4b39-8e55-75d6233adb03"},
        TaskStatus::Published,
        std::nullopt, //service
        {"message"},
        maps::chrono::parseIsoDateTime("2020-03-26 09:16:25+01")
    };
    const auto expectedJson = maps::common::readFileToString(SRC_("data/change2.json"));
    maps::json::Builder builder;
    builder << change;
    UNIT_ASSERT_VALUES_EQUAL(builder.str(), expectedJson);
}

} // test_task_change suite

} // namespace maps::wiki::feedback::api::tests
