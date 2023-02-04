#include <maps/infopoint/tests/common/compare_json.h>

#include <maps/infopoint/lib/comments/comment_id.h>
#include <maps/infopoint/lib/moderation/clean_web.h>
#include <maps/infopoint/lib/moderation/task.h>
#include <maps/infopoint/lib/point/point_uuid.h>

#include <maps/libs/geolib/include/point.h>

#include <library/cpp/testing/gtest/gtest.h>

#include <boost/uuid/string_generator.hpp>
#include <boost/uuid/uuid.hpp>


using namespace infopoint;

namespace {

const auto POINT_UUID = PointUuid(
    boost::uuids::string_generator()("bc55b020-dcfe-490a-b8ba-001abb3f4705"));

void checkTarget(moderation::TargetId target, const std::string& expectedJsonString)
{
    auto task = moderation::Task{
        .targetId = std::move(target),
        .regionIds = {1, 2, 3},
        .text = "some_text",
        .authorUid = Uid("some_uid"),
        .pointPosition = maps::geolib3::Point2{11.1234567, 51.1234567},
        .userPublicName = "some_display_name",
        .extraInfo = moderation::ExtraInfo{
            .userAgent = "some_user_agent",
            .userUuid = UserUuid("some_uuid"),
            .deviceId = "some_device_id",
            .userPosition = maps::geolib3::Point2{10.1234567, 50.1234567},
            .ja3 = "some_ja3_fingerprint",
        },
        .userIpAddress = "some_ip"};

    auto expected = maps::json::Value::fromString(expectedJsonString);
    auto actual = maps::json::Value::fromString(
        moderation::toJSONString("request_id", task, "env"));
    ASSERT_NO_THROW(assertJsonValuesEqual(actual, expected));
}

} // namespace

TEST(moderation_request_body, test_point_moderation_request_body)
{
    checkTarget(POINT_UUID, R"(
    {
        "jsonrpc": "2.0",
        "id": "request_id",
        "method": "process",
        "params": {
            "service": "map_talks",
            "type": "text",
            "key": "bc55b020-dcfe-490a-b8ba-001abb3f4705",
            "body": {
                "environment": "env",
                "text": "some_text",
                "display_name": "some_display_name",
                "regions": [1, 2, 3],
                "object_category": "point",
                "ll": {
                    "lon": 11.123456,
                    "lat": 51.123456
                },
                "uuid": "some_uuid",
                "device_id": "some_device_id",
                "user_agent": "some_user_agent",
                "ja3": "some_ja3_fingerprint",
                "user_ip": "some_ip",
                "ull": {
                    "lon": 10.123456,
                    "lat": 50.123456
                }
            },
            "puid": "some_uid"
        }
    })");
}

TEST(moderation_request_body, test_comment_moderation_request_body)
{
    checkTarget(
        CommentUniqueId{
            .pointUuid = POINT_UUID,
            .commentId = 3
        },
        R"(
        {
            "jsonrpc": "2.0",
            "id": "request_id",
            "method": "process",
            "params": {
                "service": "map_talks",
                "type": "text",
                "key": "bc55b020-dcfe-490a-b8ba-001abb3f4705/3",
                "body": {
                    "environment": "env",
                    "text": "some_text",
                    "display_name": "some_display_name",
                    "regions": [1, 2, 3],
                    "object_category": "comment",
                    "ll": {
                        "lon": 11.123456,
                        "lat": 51.123456
                    },
                    "uuid": "some_uuid",
                    "device_id": "some_device_id",
                    "user_agent": "some_user_agent",
                    "ja3": "some_ja3_fingerprint",
                    "user_ip": "some_ip",
                    "ull": {
                        "lon": 10.123456,
                        "lat": 50.123456
                    }
                },
                "puid": "some_uid"
            }
        })");
}
