#include <maps/sprav/callcenter/libs/yang_client/yang_client.h>
#include <maps/libs/http/include/test_utils.h>

#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/testing/gtest_extensions/assertions.h>

namespace maps::sprav::callcenter::yang::tests {

TEST(YangClientTest, deleteTask) {
    yang::Client yangClient("http://mockYangApi", "yangToken");

    auto mockHandle1 = maps::http::addMock(
        "http://mockYangApi/api/v1/task-suites/taskId1/set-overlap-or-min",
        [](const maps::http::MockRequest& request) {
            EXPECT_THAT(request.body, "{\"overlap\":0}");
            return maps::http::MockResponse::withStatus(409);
        }
    );
    EXPECT_TRUE(yangClient.deleteTask("taskId1"));

    auto mockHandle2 = maps::http::addMock(
        "http://mockYangApi/api/v1/task-suites/taskId2/set-overlap-or-min",
        [](const maps::http::MockRequest& request) {
            EXPECT_THAT(request.body, "{\"overlap\":0}");
            return maps::http::MockResponse{"{\"response\":\"body\"}"};
        }
    );
    EXPECT_FALSE(yangClient.deleteTask("taskId2"));

    auto mockHandle3 = maps::http::addMock(
        "http://mockYangApi/api/v1/task-suites/taskId3/set-overlap-or-min",
        [](const maps::http::MockRequest& request) {
            EXPECT_THAT(request.body, "{\"overlap\":0}");
            return maps::http::MockResponse{"{\"overlap\":0}"};
        }
    );
    EXPECT_TRUE(yangClient.deleteTask("taskId3"));
}

} // maps::sprav::callcenter::yang::tests
