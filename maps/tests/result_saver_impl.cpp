/// @file result_saver_impl.cpp
/// Tests for ResultSaveImpl, interacting with real s3 storage.
/// To run this test, s3 storage should be accessable with put, get and delete object rights.
/// You should set VM_SCHEDULER_CLIENT_TESTS_S3_HOST env with s3 address (http://...).

#include "maps/b2bgeo/vm_scheduler/agent/libs/result_saver/impl/result_saver_impl.h"
#include "maps/b2bgeo/vm_scheduler/agent/libs/result_saver/impl/errors.h"

#include <maps/b2bgeo/vm_scheduler/libs/common/include/env.h>
#include <maps/libs/http/include/request.h>
#include <maps/libs/log8/include/log8.h>

#include <library/cpp/testing/gtest/gtest.h>

#include <fstream>

using namespace maps::b2bgeo;
using namespace maps::b2bgeo::vm_scheduler;
using namespace maps::b2bgeo::vm_scheduler::agent;

class ResultSaverCase : public ::testing::Test {
protected:
    void SetUp() override
    {
        // You can use this address http://yandex-vm-scheduler-results.s3.us-east-2.amazonaws.com
        if (!std::getenv("VMA_S3_ADDRESS_TESTS")) {
            GTEST_SKIP() << "To run s3 tests with maps/b2bgeo/vm_scheduler/agent/libs/result_saver,"
                         << " please, specify VMA_S3_ADDRESS_TESTS.";
        }
    }
};

namespace {

ResultSaverConfig createTestConfig()
{
    return {
        .s3Address = getFromEnvOrThrow("VMA_S3_ADDRESS_TESTS"),
        .s3Expiration = std::chrono::days(1),
    };
}

} // namespace

TEST_F(ResultSaverCase, simple)
{
    // create file
    const auto folder = std::filesystem::current_path() / "data";
    ASSERT_TRUE(std::filesystem::create_directory(folder));
    const auto path = folder / "test.txt";
    const auto data = "some data\nother line";
    {
        auto file = std::ofstream(path);
        ASSERT_TRUE(file);
        file << data;
    }

    // upload with result saver
    const auto config = createTestConfig();
    auto saver = ResultSaverImpl(config);
    auto result = saver.save(path);
    EXPECT_TRUE(result.IsSuccess());
    const auto url = std::move(result).ValueOrThrow();

    // download with http
    maps::http::Client client;
    auto response = maps::http::Request(client, maps::http::GET, url).perform();
    EXPECT_EQ(response.responseClass(), maps::http::ResponseClass::Success);
    EXPECT_EQ(response.readBody(), data);

    // cleanup
    const auto s3Client = S3Client(config.s3Address, {}, {}, {}, std::nullopt);
    const auto key = url.substr(config.s3Address.size() + 1);
    s3Client.remove(key, {});
    const auto empty =
        maps::http::Request(client, maps::http::GET, url).perform();
    EXPECT_EQ(empty.responseClass(), maps::http::ResponseClass::ClientError);
    ASSERT_TRUE(std::filesystem::remove_all(folder));
}

TEST_F(ResultSaverCase, extension)
{
    // create file
    const auto folder = std::filesystem::current_path() / "data";
    ASSERT_TRUE(std::filesystem::create_directory(folder));
    const auto path = folder / "test.txt";
    const auto data = "some data\nother line";
    {
        auto file = std::ofstream(path);
        ASSERT_TRUE(file);
        file << data;
    }

    // upload with result saver
    auto config = createTestConfig();
    config.s3Extension = ".json";
    auto saver = ResultSaverImpl(config);
    auto result = saver.save(path);
    EXPECT_TRUE(result.IsSuccess());
    const auto url = std::move(result).ValueOrThrow();

    // download with http
    maps::http::Client client;
    auto response = maps::http::Request(client, maps::http::GET, url).perform();
    EXPECT_EQ(response.responseClass(), maps::http::ResponseClass::Success);
    EXPECT_EQ(response.readBody(), data);

    // cleanup
    const auto s3Client = S3Client(config.s3Address, {}, {}, {}, std::nullopt);

    const auto key = url.substr(url.find_last_of('/') + 1);
    s3Client.remove(key, {});
    const auto empty =
        maps::http::Request(client, maps::http::GET, url).perform();
    EXPECT_EQ(empty.responseClass(), maps::http::ResponseClass::ClientError);
    ASSERT_TRUE(std::filesystem::remove_all(folder));
}

TEST_F(ResultSaverCase, prefix)
{
    // create file
    const auto folder = std::filesystem::current_path() / "data";
    ASSERT_TRUE(std::filesystem::create_directory(folder));
    const auto path = folder / "test.txt";
    const auto data = "some data\nother line";
    {
        auto file = std::ofstream(path);
        ASSERT_TRUE(file);
        file << data;
    }

    // upload with result saver
    auto config = createTestConfig();
    config.s3Prefix = "folder";
    auto saver = ResultSaverImpl(config);
    auto result = saver.save(path);
    EXPECT_TRUE(result.IsSuccess());
    const auto url = std::move(result).ValueOrThrow();

    // download with http
    maps::http::Client client;
    auto response = maps::http::Request(client, maps::http::GET, url).perform();
    EXPECT_EQ(response.responseClass(), maps::http::ResponseClass::Success);
    EXPECT_EQ(response.readBody(), data);

    // cleanup
    const auto s3Client =
        S3Client(config.s3Address, {}, {}, config.s3Prefix, std::nullopt);
    const auto key = url.substr(url.find_last_of('/') + 1);
    s3Client.remove(key, {});
    const auto empty =
        maps::http::Request(client, maps::http::GET, url).perform();
    EXPECT_EQ(empty.responseClass(), maps::http::ResponseClass::ClientError);
    ASSERT_TRUE(std::filesystem::remove_all(folder));
}
