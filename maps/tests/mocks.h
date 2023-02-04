#pragma once

#include <library/cpp/testing/gtest/gtest.h>
#include <maps/automotive/libs/s3/tests/mock.h>
#include <maps/automotive/store_internal/lib/context.h>
#include <maps/libs/http/include/test_utils.h>

namespace maps::automotive::store_internal {

extern std::unique_ptr<AppContext> g_ctx;

struct MockedS3Client: public S3Client
{
    MockedS3Client(const Config::S3& cfg);

    s3::S3ClientMock& mock() {
        return dynamic_cast<s3::S3ClientMock&>(*s3_);
    }
};

class AppContextMock: public AppContext {
public:
    AppContextMock();

    S3Client& s3() override { return s3_; }

    s3::S3ClientMock& s3Mock() { return s3_.mock(); }

public:
    MockedS3Client s3_;
    http::MockHandle tankerMock_;
    http::MockHandle teamcityMock_;
    http::MockHandle teamcityBrowserMock_;
};

class AppContextFixture: public ::testing::Test {
public:
    AppContextFixture();
    ~AppContextFixture();

    AppContextMock& ctxMock()
    {
        return dynamic_cast<AppContextMock&>(*g_ctx);
    }
    s3::S3ClientMock& s3Mock()
    {
        return ctxMock().s3Mock();
    }

protected:
    struct BaseCtorTag {};
    AppContextFixture(BaseCtorTag) {}
};

s3::model::HeadObjectOutcome notFoundOutcome();

template <typename Outcome>
Outcome s3Failed()
{
    Aws::Client::AWSError<Aws::S3::S3Errors> err;
    err.SetMessage("We tried hard, but operation failed");
    return Outcome(err);
}

} // namespace maps::automotive::store_internal
