#pragma once
#include <maps/libs/http/include/test_utils.h>
#include <maps/libs/json/include/value.h>

namespace maps::renderer::cartograph {

maps::json::Value awaitTask(const http::MockRequest& request);

} // namespace maps::renderer::cartograph
