#pragma once
#include "mock_file_storage.h"
#include "mock_sandbox_client.h"
#include "mock_stylerepo_client.h"
#include "postgres_fixture.h"

#include <maps/renderer/cartograph/lib/core/application.h>

#include <library/cpp/testing/common/env.h>

namespace maps::renderer::cartograph::test_util {

struct AppFixture : test_util::PostgresFixture {
    AppFixture();

    std::unique_ptr<Application> createApp();

    config::Config config;
    std::shared_ptr<test_util::MockSandboxClient> sandboxClient;
    std::shared_ptr<test_util::MockStylerepoClient> stylerepoClient;
    std::shared_ptr<test_util::MockFileStorage> userDataSetFileStorage;
    std::shared_ptr<test_util::MockFileStorage> exportFileStorage;
};

} // namespace maps::renderer::cartograph::test_util
