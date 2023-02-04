#pragma once

#include <maps/factory/libs/storage/s3_storage.h>
#include <maps/libs/common/include/exception.h>

#include <util/system/env.h>

namespace maps::factory::processing::tests {

inline storage::AuthPtr testS3Auth()
{
    return storage::S3CloudStorage::keyAuth({.accessKeyId = "1234567890", .secretKey = "abcdefabcdef"});
}

inline std::string testS3Endpoint()
{
    std::string port = GetEnv("S3MDS_PORT");
    REQUIRE(!port.empty(), "Cannot connect to local S3 server");
    return "http://127.0.0.1:" + port;
}

inline storage::S3Ptr testS3(const std::string& bucket)
{
    auto s3 = storage::s3Storage(testS3Endpoint(), bucket, "", testS3Auth());
    s3->createBucket();
    return s3;
}

} // namespace maps::factory::processing::tests
