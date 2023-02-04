#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/env.h>

#include <util/system/env.h>
#include <mapreduce/yt/interface/client.h>

#include <maps/infopoint/takeout/lib/application.h>
#include <maps/libs/json/include/value.h>
#include <maps/libs/json/include/schema.h>

#include <iostream>

JSC_DOC(ResponseSchema) {
    JSC_ARRAY(road_events) {
        JSC_STR(description);
        JSC_DICT(origin) {
            JSC_DBL(lat);
            JSC_DBL(lon);
        };
        JSC_DICT(point) {
            JSC_DBL(lat);
            JSC_DBL(lon);
        };
        JSC_STR(type);
        JSC_STR(time);
    };

    JSC_ARRAY(comments) {
        JSC_STR(comment);
        JSC_DICT(point) {
            JSC_DBL(lat);
            JSC_DBL(lon);
        };
        JSC_STR(time);
    };

    JSC_ARRAY(votes) {
        JSC_STR(vote);
        JSC_DICT(point) {
            JSC_DBL(lat);
            JSC_DBL(lon);
        };
        JSC_STR(time);
    };
};

using ResponseTrait = ResponseSchema<maps::json::FetchTraits>;

namespace {

maps::infopoint::takeout::TakeoutData takeout(
    const maps::infopoint::takeout::Uid& uid,
    const maps::infopoint::takeout::YtTables& ytTables)
{
    TString ytProxy = GetEnv("YT_PROXY");

    auto client = NYT::CreateClient(ytProxy);

    UNIT_ASSERT(client->Exists(ytTables.ugcByUidTable.c_str()));
    UNIT_ASSERT(client->Exists(ytTables.roadEventsBySubkeyTable.c_str()));

    auto res = maps::infopoint::takeout::Application::performTakeout(
            client, uid, ytTables);
    UNIT_ASSERT(res.has_value());

    return res.value();
}

} // namespace

Y_UNIT_TEST_SUITE(InfopointTakeout)
{

Y_UNIT_TEST(BasicTakeout)
{
    auto result = takeout(
        "user1", 
        {
            "//basic_test/ugc_by_uid",
            "//basic_test/road_events_by_subkey"
        });
    std::cout << result << std::endl;
    auto deserializedResult = maps::json::Value::fromString(result); 
    auto response = maps::json::FetchTraits::wrap<ResponseTrait>(deserializedResult);

    UNIT_ASSERT_EQUAL(response.road_events().size(), 1);
    UNIT_ASSERT_EQUAL(response.road_events()[0].type(), "reconstruction");
    UNIT_ASSERT_EQUAL(response.votes().size(), 1);
    UNIT_ASSERT_EQUAL(response.votes()[0].vote(), "positive");
    UNIT_ASSERT_EQUAL(response.comments().size(), 1);
}

Y_UNIT_TEST(MultipleCommentsTakeout)
{
    auto result = takeout(
        "user1",
        {
            "//multiple_comments_test/ugc_by_uid",
            "//multiple_comments_test/road_events_by_subkey"
        });
    auto deserializedResult = maps::json::Value::fromString(result); 
    auto response = maps::json::FetchTraits::wrap<ResponseTrait>(deserializedResult);

    UNIT_ASSERT_EQUAL(response.comments().size(), 2);
}

Y_UNIT_TEST(UserWithoutData)
{
    auto result = takeout(
        "invalid_user",
        {
            "//basic_test/ugc_by_uid",
            "//basic_test/road_events_by_subkey"
        });

    UNIT_ASSERT_EQUAL(result, "");
}

}
