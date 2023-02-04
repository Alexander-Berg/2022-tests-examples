#pragma once

#include <maps/renderer/cartograph/lib/infra/file_storage.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

namespace maps::renderer::cartograph::test_util {

class MockFileStorage : public FileStorage {
public:
    MOCK_METHOD(void, putFile, (const std::string&, const std::string&), (override));

    MOCK_METHOD(void, removeFile, (const std::string&), (override));

    MOCK_METHOD(std::optional<std::string>, file, (const std::string&), (override));

    MOCK_METHOD(std::string, fileUrl, (const std::string&), (override));
};

} // namespace maps::renderer::cartograph::test_util
