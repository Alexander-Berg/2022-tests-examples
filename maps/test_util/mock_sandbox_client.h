#pragma once

#include <maps/renderer/cartograph/lib/infra/sandbox_client.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

namespace maps::renderer::cartograph::test_util {

class MockSandboxClient : public SandboxClient {
public:
    MOCK_METHOD(
        std::optional<std::string>, downloadResource, (const std::string&), (const, override));
    MOCK_METHOD(
        void,
        updateResource,
        (const std::string&, const maps::json::repr::ObjectRepr&),
        (const, override));
    MOCK_METHOD(
        std::optional<maps::json::Value>,
        getResource,
        (const std::string&, std::optional<ReadPreference>),
        (const, override));
    MOCK_METHOD(
        maps::json::Value,
        getResources,
        (const std::string&, const maps::json::repr::ObjectRepr&, std::optional<ReadPreference>),
        (const, override));
    MOCK_METHOD(std::string, fileUrl, (const std::string&), (const, override));
};

} // namespace maps::renderer::cartograph::test_util
