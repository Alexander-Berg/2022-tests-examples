#include "await_task.h"

#include <maps/infra/yacare/include/test_utils.h>
#include <library/cpp/testing/gtest/gtest.h>

namespace maps::renderer::cartograph {

maps::json::Value awaitTask(const http::MockRequest& request)
{
    auto response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 202);

    auto task = maps::json::Value::fromString(response.body);
    while (task["status"].as<std::string>() == "running") {
        response = yacare::performTestRequest({http::GET, task["url"].as<std::string>()});
        EXPECT_EQ(response.status, 200);
        task = maps::json::Value::fromString(response.body);
    }
    return task;
}

} // namespace maps::renderer::cartograph
