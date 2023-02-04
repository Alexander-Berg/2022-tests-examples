#pragma once
#include "app_fixture.h"

#include <maps/renderer/cartograph/lib/api/api.h>

namespace maps::renderer::cartograph::test_util {

extern const std::string TESTS_DATA_PATH;

struct ApiFixture : test_util::AppFixture {
    ApiFixture() { initApi(createApp()); }
    ~ApiFixture() { tearDownApi(); }
};

} // namespace maps::renderer::cartograph::test_util
