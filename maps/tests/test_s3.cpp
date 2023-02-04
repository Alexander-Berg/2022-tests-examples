#include <library/cpp/testing/gtest/gtest.h>
#include <maps/automotive/libs/s3/client.h>
#include <maps/automotive/libs/s3/error.h>
#include <maps/automotive/libs/s3/tests/mock.h>
#include <maps/libs/common/include/exception.h>

namespace maps::automotive::s3 {

using namespace ::testing;

namespace {

struct MockedClient: public Client
{
    MockedClient() : Client(std::make_unique<S3ClientMock>()) {}

    S3ClientMock& mock() {
        return dynamic_cast<S3ClientMock&>(*s3_);
    }
};

} // namespace

TEST(s3, unquote)
{
    EXPECT_EQ("string", unquote(R"("string")"));
    EXPECT_EQ(R"("string")", unquote(R"(""string"")"));
    EXPECT_EQ(R"(just"a"string)", unquote(R"("just"a"string")"));
    EXPECT_EQ("", unquote(R"("")"));
}

TEST(s3, unquoteError)
{
    EXPECT_THROW(unquote(R"("string)"), maps::LogicError);
    EXPECT_THROW(unquote(R"(string")"), maps::LogicError);
    EXPECT_THROW(unquote("''"), maps::LogicError);
    EXPECT_THROW(unquote(R"(")"), maps::LogicError);
    EXPECT_THROW(unquote("a"), maps::LogicError);
    EXPECT_THROW(unquote(""), maps::LogicError);
}

TEST(s3, constructByConfig)
{
    Config config;
    Client client(config);
}

TEST(s3, constructByClient)
{
    initAwsApi();
    Client client(std::make_unique<Aws::S3::S3Client>());
}

TEST(s3, headObjectNotFound)
{
    MockedClient client;
    Aws::Client::AWSError<Aws::S3::S3Errors> err;
    err.SetResponseCode(Aws::Http::HttpResponseCode::NOT_FOUND);
    EXPECT_CALL(client.mock(), HeadObject(_))
        .WillOnce(Return(model::HeadObjectOutcome(err)));
    EXPECT_FALSE(client.headObject({}).has_value());
}

TEST(s3, headObjectBadEtag)
{
    MockedClient client;

    EXPECT_CALL(client.mock(), HeadObject(_))
        .WillOnce(Return(model::HeadObjectOutcome(model::HeadObjectResult())));
    EXPECT_THROW(client.headObject({}), maps::LogicError);

    EXPECT_CALL(client.mock(), HeadObject(_))
        .WillOnce(Return(model::HeadObjectOutcome(model::HeadObjectResult()
            .WithETag("etag"))));
    EXPECT_THROW(client.headObject({}), maps::LogicError);
}

TEST(s3, headObject)
{
    MockedClient client;
    EXPECT_CALL(client.mock(), HeadObject(_))
        .WillOnce(Return(model::HeadObjectOutcome(model::HeadObjectResult()
            .WithETag(R"("etag")"))));
    auto res = client.headObject({});
    ASSERT_TRUE(res.has_value());
    EXPECT_EQ("etag", res->GetETag());
}

TEST(s3, putObject)
{
    MockedClient client;

    EXPECT_CALL(client.mock(), PutObject(_))
        .WillOnce(Return(model::PutObjectOutcome()));
    EXPECT_THROW(client.putObject({}), Error);

    EXPECT_CALL(client.mock(), PutObject(_))
        .WillOnce(Return(model::PutObjectOutcome(model::PutObjectResult())));
    EXPECT_THROW(client.putObject({}), maps::LogicError);

    EXPECT_CALL(client.mock(), PutObject(_))
        .WillOnce(Return(model::PutObjectOutcome(model::PutObjectResult()
            .WithETag("etag"))));
    EXPECT_THROW(client.putObject({}), maps::LogicError);

    EXPECT_CALL(client.mock(), PutObject(_))
        .WillOnce(Return(model::PutObjectOutcome(model::PutObjectResult()
            .WithETag(R"("etag")"))));
    client.putObject({});
}

TEST(s3, deleteObject)
{
    MockedClient client;

    EXPECT_CALL(client.mock(), DeleteObject(_))
        .WillOnce(Return(model::DeleteObjectOutcome()));
    EXPECT_THROW(client.deleteObject({}), Error);

    EXPECT_CALL(client.mock(), DeleteObject(_))
        .WillOnce(Return(model::DeleteObjectOutcome(model::DeleteObjectResult())));
    client.deleteObject({});
}

TEST(s3, copyObject)
{
    MockedClient client;

    EXPECT_CALL(client.mock(), CopyObject(_))
        .WillOnce(Return(model::CopyObjectOutcome()));
    EXPECT_THROW(client.copyObject({}), Error);

    EXPECT_CALL(client.mock(), CopyObject(_))
        .WillOnce(Return(model::CopyObjectOutcome(model::CopyObjectResult())));
    client.copyObject({});
}

}
