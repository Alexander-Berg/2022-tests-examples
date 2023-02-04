#pragma once

#include <maps/renderer/cartograph/lib/infra/stylerepo_client.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

namespace maps::renderer::cartograph::test_util {

class MockStylerepoClient : public StylerepoClient {
public:
    MOCK_METHOD(std::optional<Bundle>, getBundle, (const std::string&), (const, override));
    MOCK_METHOD(std::optional<std::string>, getStyleSheet, (const std::string&), (const, override));
    MOCK_METHOD(std::optional<std::string>, getIcon, (const std::string&), (const, override));
};

} // namespace maps::renderer::cartograph::test_util
