#include <library/cpp/testing/gtest/gtest.h>
#include <maps/automotive/libs/s3/error.h>
#include <maps/automotive/store_internal/tests/matchers.h>
#include <maps/automotive/store_internal/tests/mocks.h>

namespace maps::automotive::store_internal {

namespace {

constexpr auto EXTERNAL_BUCKET = "external-bucket";

MATCHER_P(TargetBucketPath, key, "")
{
    return arg.GetBucket() == EXTERNAL_BUCKET
        && arg.GetKey() == key
        && arg.GetMetadataDirective() == s3::model::MetadataDirective::REPLACE 
        && arg.GetContentType() == "application/octet-stream";
}

Config::S3 makeConfig()
{
    s3::initAwsApi();
    Config::S3 cfg;
    cfg.mutable_buckets()->set_external(EXTERNAL_BUCKET);
    auto* retry = cfg.mutable_retry_head_after_copy();
    retry->set_try_number(1);
    retry->set_initial_cooldown_millis(10);
    retry->set_cooldown_backoff(1.0);
    return cfg;
}

struct S3Fixture: public ::testing::Test
{
    S3Fixture()
        : s3(makeConfig())
    {}

    MockedS3Client s3;

    void testBadTeamcityUrl(const std::string& url)
    {
        auto mockRedirect = http::addMock(
            url,
            [&](const http::MockRequest&) {
                auto rsp = http::MockResponse().withStatus(302);
                rsp.headers["Location"] = "https://s3.mds.yandex.net/bucket/key";
                return rsp;
            });
        EXPECT_THROW(s3.headIncomingObject(url), yacare::Error);
    }

};

} // namespace

using namespace ::testing;

TEST_F(S3Fixture, headIncomingObjectNotFound)
{
    Aws::Client::AWSError<Aws::S3::S3Errors> err;
    err.SetResponseCode(Aws::Http::HttpResponseCode::NOT_FOUND);
    EXPECT_CALL(s3.mock(), HeadObject(_))
        .WillOnce(Return(s3::model::HeadObjectOutcome(err)));

    EXPECT_THROW(s3.headIncomingObject("bucket/path"), yacare::ClientError);
}

TEST_F(S3Fixture, headIncomingObjectError)
{
    EXPECT_CALL(s3.mock(), HeadObject(_))
        .WillOnce(Return(s3::model::HeadObjectOutcome()));
    EXPECT_THROW(s3.headIncomingObject("bucket/path"), yacare::Error);
}

TEST_F(S3Fixture, headIncomingObjectS3Private)
{
    auto md5 = "some_md5";
    auto tcUrl = "https://teamcity.yandex-team.ru/repository/download/some/artifact";
    auto redirectUrl = "https://buket.s3-private.mds.yandex.net/apk?param=ignore";
    auto mockRedirect = http::addMock(
        tcUrl,
        [&](const http::MockRequest&) {
            auto rsp = http::MockResponse().withStatus(302);
            rsp.headers["Location"] = redirectUrl;
            return rsp;
        });
    EXPECT_CALL(s3.mock(), HeadObject(BucketPath("buket/apk")))
        .WillOnce(Return(s3::model::HeadObjectResult()
            .WithContentLength(100560)
            .WithETag(std::string("\"") + md5 + "\"")));

    auto rsp = s3.headIncomingObject(tcUrl);
    EXPECT_EQ(md5, rsp.GetETag());
    EXPECT_EQ(100560, rsp.GetContentLength());
}

TEST_F(S3Fixture, headIncomingObjectGoodUrl)
{
    auto md5 = "some_md5";
    EXPECT_CALL(s3.mock(), HeadObject(BucketPath("bucket/some path/with spaces")))
        .WillRepeatedly(Return(s3::model::HeadObjectResult()
            .WithContentLength(1202134)
            .WithETag(std::string("\"") + md5 + "\"")));

    EXPECT_EQ(md5, s3.headIncomingObject("bucket/some path/with spaces").GetETag());
    EXPECT_EQ(md5, s3.headIncomingObject("bucket/some%20path/with%20spaces").GetETag());

    auto tcUrl = "https://teamcity.yandex-team.ru/repository/download/some artifact";
    auto redirectUrl = "https://s3.mds.yandex.net/bucket/some%20path/with%20spaces";
    auto mockRedirect = http::addMock(
        tcUrl,
        [&](const http::MockRequest&) {
            auto rsp = http::MockResponse().withStatus(302);
            rsp.headers["Location"] = redirectUrl;
            return rsp;
        });

    EXPECT_EQ(md5, s3.headIncomingObject(tcUrl).GetETag());
}

TEST_F(S3Fixture, headIncomingObjectBadUrl)
{
    ON_CALL(s3.mock(), HeadObject(_))
        .WillByDefault(Return(s3::model::HeadObjectResult()
            .WithContentLength(1202134)
            .WithETag("\"md5\"")));

    EXPECT_THROW(s3.headIncomingObject(".not-url.:but:some:crap"), yacare::Error);
    testBadTeamcityUrl("http://teamcity.yandex-team.ru/repository/download/some/artifact");
    testBadTeamcityUrl("https://tc.yandex-team.ru/repository/download/some/artifact");
    testBadTeamcityUrl("https://teamcity.yandex-team.ru/give/me/some/artifact");
}

TEST_F(S3Fixture, headIncomingObjectUnexpectedTeamcityResponse)
{
    auto tcUrl = "https://teamcity.yandex-team.ru/repository/download/some/artifact";
    EXPECT_CALL(s3.mock(), HeadObject(_)).Times(0);

    { // no Location header
        auto mockRedirect = http::addMock(
            tcUrl,
            [&](const http::MockRequest&) {
                return http::MockResponse().withStatus(302);
            });
        EXPECT_THROW(s3.headIncomingObject(tcUrl), LogicError);
    }
    { // URL not found
        auto mockRedirect = http::addMock(
            tcUrl,
            [&](const http::MockRequest&) {
                return http::MockResponse("not found").withStatus(404);
            });
        EXPECT_THROW(s3.headIncomingObject(tcUrl), yacare::Error);
    }
    { // unexpected HTTP status
        auto mockRedirect = http::addMock(
            tcUrl,
            [&](const http::MockRequest&) {
                return http::MockResponse("some body").withStatus(200);
            });
        EXPECT_THROW(s3.headIncomingObject(tcUrl), RuntimeError);
    }
    { // redirected to http
        auto mockRedirect = http::addMock(
            tcUrl,
            [&](const http::MockRequest&) {
                auto rsp = http::MockResponse().withStatus(302);
                rsp.headers["Location"] = "http://s3.mds.yandex.net";
                return rsp;
            });
        EXPECT_THROW(s3.headIncomingObject(tcUrl), RuntimeError);
    }
    { // redirected to unexpected location
        auto mockRedirect = http::addMock(
            tcUrl,
            [&](const http::MockRequest&) {
                auto rsp = http::MockResponse().withStatus(302);
                rsp.headers["Location"] = "https://what.the.fck";
                return rsp;
            });
        EXPECT_THROW(s3.headIncomingObject(tcUrl), yacare::ClientError);
    }
    { // cannot extract bucket and object key
        auto mockRedirect = http::addMock(
            tcUrl,
            [&](const http::MockRequest&) {
                auto rsp = http::MockResponse().withStatus(302);
                rsp.headers["Location"] = "https://s3.mds.yandex.net/no_obj";
                return rsp;
            });
        EXPECT_THROW(s3.headIncomingObject(tcUrl), yacare::Error);
    }
}

TEST_F(S3Fixture, headIncomingObjectEscapedCharacters)
{
    auto md5 = "MD5";
    EXPECT_CALL(s3.mock(), HeadObject(BucketPath("bucket/key with spaces")))
        .WillOnce(Return(s3::model::HeadObjectResult()
            .WithContentLength(1020)
            .WithETag(std::string("\"") + md5 + "\"")));
    EXPECT_EQ(md5, s3.headIncomingObject("bucket/key%20with%20spaces").GetETag());
}

TEST_F(S3Fixture, copyObject)
{
    auto targetPath = "target/path";
    EXPECT_CALL(s3.mock(), CopyObject(TargetBucketPath(targetPath)))
        .WillOnce(Return(s3::model::CopyObjectResult()));

    s3.copyObject(s3::model::CopyObjectRequest()
        .WithCopySource("incoming/path")
        .WithKey(targetPath));

    EXPECT_CALL(s3.mock(), CopyObject(TargetBucketPath(targetPath)))
        .WillOnce(Return(s3::model::CopyObjectOutcome()));
    EXPECT_THROW(
        s3.copyObject(s3::model::CopyObjectRequest()
            .WithCopySource("incoming/path")
            .WithKey(targetPath)),
        yacare::Error);
}

TEST_F(S3Fixture, getIncomingObjectNotFound)
{
    Aws::Client::AWSError<Aws::S3::S3Errors> err;
    err.SetResponseCode(Aws::Http::HttpResponseCode::NOT_FOUND);
    EXPECT_CALL(s3.mock(), GetObject(_))
        .WillOnce(Return(ByMove(s3::model::GetObjectOutcome(err))));

    EXPECT_THROW(s3.getIncomingObject("bucket/manifest"), yacare::Error);
}

TEST_F(S3Fixture, getIncomingObjectSuccess)
{
    s3::model::GetObjectResult result;
    result.SetContentLength(12345);

    EXPECT_CALL(s3.mock(), GetObject(BucketPath("bucket/manifest")))
        .WillOnce(
            Return(ByMove(s3::model::GetObjectOutcome(std::move(result)))));

    auto rsp = s3.getIncomingObject("bucket/manifest");
    EXPECT_EQ(12345, rsp.GetContentLength());
}

TEST_F(S3Fixture, getIncomingObjectRedirect)
{
    auto tcUrl =
        "https://teamcity.yandex-team.ru/repository/download/some/artifact";
    auto redirectUrl =
        "https://bucket.s3-private.mds.yandex.net/manifest?param=ignore";

    auto mockRedirect = http::addMock(tcUrl, [&](const http::MockRequest&) {
        auto rsp = http::MockResponse().withStatus(302);
        rsp.headers["Location"] = redirectUrl;
        return rsp;
    });

    s3::model::GetObjectResult result;
    result.SetContentLength(12345);

    EXPECT_CALL(s3.mock(), GetObject(BucketPath("bucket/manifest")))
        .WillOnce(
            Return(ByMove(s3::model::GetObjectOutcome(std::move(result)))));

    auto rsp = s3.getIncomingObject(tcUrl);
    EXPECT_EQ(12345, rsp.GetContentLength());
}
}
