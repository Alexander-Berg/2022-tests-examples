#include <maps/factory/services/backend/tests/test_utils.h>

#include <maps/factory/libs/db/mosaic_gateway.h>
#include <maps/factory/libs/db/mosaic_source_gateway.h>
#include <maps/factory/libs/db/release_gateway.h>
#include <maps/factory/libs/db/release_validation_gateway.h>
#include <maps/factory/libs/sproto_helpers/release_validation.h>

#include <library/cpp/testing/gtest/gtest.h>

namespace maps::factory::backend::tests {

namespace {

using namespace sproto_helpers;

const std::string URL_START = "http://localhost/v1/releases/start_validation";
const std::string URL_CANCEL = "http://localhost/v1/releases/cancel_validation";
const std::string URL_MUTE = "http://localhost/v1/releases/mute_validation_error";
const std::string URL_UNMUTE = "http://localhost/v1/releases/unmute_validation_error";
const std::string URL_GET = "http://localhost/v1/releases/get_validation";
const std::string URL_GET_ALL = "http://localhost/v1/releases/get_all_validations";

class ValidationFixture : public BackendFixture {
public:
    ValidationFixture()
        : testRelease_(createTestRelease())
        , testValidation_(createTestReleaseValidation(1))
        , testError_(createTestReleaseValidationError())
    {
        auto txn = txnHandle();
        db::ReleaseGateway(*txn).insert(testRelease_);
        db::MosaicSourceGateway(*txn).insert(createTestMosaicSource());
        db::MosaicGateway(*txn).insert(createTestMosaic());
        db::MosaicGateway(*txn).insert(createTestMosaic());
        db::ValidationGateway(*txn).insert(testValidation_);
        db::ValidationErrorGateway(*txn).insert(testError_);
        txn->commit();
    }

    const db::Release& testRelease() { return testRelease_; }

    const db::Validation& testValidation() { return testValidation_; }

    const db::ValidationError& testError() { return testError_; }

private:
    db::Release testRelease_;
    db::Validation testValidation_;
    db::ValidationError testError_;
};

} // namespace

TEST_F(ValidationFixture, test_release_validation_api_start)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    http::MockRequest request(
        http::POST,
        http::URL(URL_START)
            .addParam("release_id", testRelease().id())
    );
    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(response.status, 200);

    auto respValidation = boost::lexical_cast<srelease::Validation>(response.body);
    ASSERT_NE(respValidation.id(), "0");

    auto dbValidation = createTestReleaseValidation(1);
    ASSERT_NO_THROW(
        dbValidation = db::ValidationGateway(*txnHandle())
            .loadById(db::parseId(respValidation.id()));
    );

    EXPECT_EQ(*respValidation.status(), convertToSproto(RELEASE_VALIDATION_STATUS));

    EXPECT_EQ(dbValidation.status(), RELEASE_VALIDATION_STATUS);
}

TEST_F(ValidationFixture, test_release_validation_api_cancel)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    http::MockRequest request(
        http::POST,
        http::URL(URL_CANCEL)
            .addParam("id", std::to_string(testValidation().id()))
    );
    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(response.status, 200);

    auto respValidation = boost::lexical_cast<srelease::Validation>(response.body);

    auto dbValidation = createTestReleaseValidation(1);
    ASSERT_NO_THROW(
        dbValidation = db::ValidationGateway(*txnHandle())
            .loadById(db::parseId(respValidation.id()));
    );

    EXPECT_EQ(*respValidation.status(), convertToSproto(RELEASE_VALIDATION_STATUS_U));
    EXPECT_EQ(respValidation.createdAt().value(),
        chrono::convertToUnixTime(CREATED_AT));
    EXPECT_NE(respValidation.finishedAt()->value(),
        chrono::convertToUnixTime(FINISHED_AT));

    EXPECT_EQ(dbValidation.status(), RELEASE_VALIDATION_STATUS_U);
}

TEST_F(ValidationFixture, test_release_validation_api_mute_unmute)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));
    {
        http::MockRequest request(
            http::POST,
            http::URL(URL_MUTE)
                .addParam("id", testError().id())
        );
        auto response = yacare::performTestRequest(request);
        ASSERT_EQ(response.status, 200);

        const auto respError =
            boost::lexical_cast<srelease::ValidationError>(response.body);

        EXPECT_EQ(db::parseId(*respError.id()), testError().id());
        EXPECT_TRUE(*respError.muted());

        const auto error = db::ValidationErrorGateway(*txnHandle())
            .loadById(db::parseId(*respError.id()));
        EXPECT_TRUE(error.muted());
    }
    {
        http::MockRequest request(
            http::POST,
            http::URL(URL_UNMUTE)
                .addParam("id", testError().id())
        );
        auto response = yacare::performTestRequest(request);
        ASSERT_EQ(response.status, 200);

        const auto respError =
            boost::lexical_cast<srelease::ValidationError>(response.body);

        EXPECT_EQ(db::parseId(*respError.id()), testError().id());
        EXPECT_FALSE(*respError.muted());

        const auto error = db::ValidationErrorGateway(*txnHandle())
            .loadById(db::parseId(*respError.id()));
        EXPECT_FALSE(error.muted());
    }
}

TEST_F(ValidationFixture, test_release_validation_api_get)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    http::MockRequest request(
        http::GET,
        http::URL(URL_GET)
            .addParam("release_id", testRelease().id())
    );
    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(response.status, 200);

    auto respValidation = boost::lexical_cast<srelease::Validation>(response.body);

    EXPECT_EQ(*respValidation.status(), convertToSproto(RELEASE_VALIDATION_STATUS));
    EXPECT_EQ(respValidation.createdAt().value(),
        chrono::convertToUnixTime(CREATED_AT));
    EXPECT_EQ(respValidation.finishedAt()->value(),
        chrono::convertToUnixTime(FINISHED_AT));
}

TEST_F(ValidationFixture, test_release_validation_api_get_all)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    http::MockRequest request(
        http::GET,
        http::URL(URL_GET_ALL)
            .addParam("release_id", testRelease().id())
    );
    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(response.status, 200);

    auto respValidation = boost::lexical_cast<srelease::Validations>(response.body)
        .validations().at(0);

    EXPECT_EQ(*respValidation.status(), convertToSproto(RELEASE_VALIDATION_STATUS));
    EXPECT_EQ(respValidation.createdAt().value(),
        chrono::convertToUnixTime(CREATED_AT));
    EXPECT_EQ(respValidation.finishedAt()->value(),
        chrono::convertToUnixTime(FINISHED_AT));
}

TEST_F(ValidationFixture, test_release_validation_sort_results)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    auto txn = txnHandle();
    db::MosaicGateway(*txn).insert(std::vector<db::Mosaic>({
        createTestMosaic(), createTestMosaic(), createTestMosaic()}));
    db::ValidationErrorGateway(*txn).insert(std::vector<db::ValidationError>({
        createTestReleaseValidationError(1, 2, 3, constructCubePolygon(8)),
        createTestReleaseValidationError(1, 3, 4, constructCubePolygon(6)),
        createTestReleaseValidationError(1, 4, 5, constructCubePolygon(7))
    }));
    txn->commit();

    {
        http::MockRequest request(
            http::GET,
            http::URL(URL_GET_ALL)
                .addParam("release_id", testRelease().id())
        );
        auto response = yacare::performTestRequest(request);
        ASSERT_EQ(response.status, 200);

        const auto results = unpackSprotoRepeated<srelease::ValidationError>(
            boost::lexical_cast<srelease::Validations>(response.body)
                .validations().at(0).results().at(0).errors()
        );

        ASSERT_EQ(results.size(), 4u);
        EXPECT_EQ(*results.at(0).id(), "2");
        EXPECT_EQ(*results.at(1).id(), "4");
        EXPECT_EQ(*results.at(2).id(), "3");
        EXPECT_EQ(*results.at(3).id(), "1");
    }
    {
        http::MockRequest request(
            http::GET,
            http::URL(URL_GET)
                .addParam("release_id", testRelease().id())
        );
        auto response = yacare::performTestRequest(request);
        ASSERT_EQ(response.status, 200);

        const auto results = unpackSprotoRepeated<srelease::ValidationError>(
            boost::lexical_cast<srelease::Validation>(response.body)
                .results().at(0).errors()
        );

        ASSERT_EQ(results.size(), 4u);
        EXPECT_EQ(*results.at(0).id(), "2");
        EXPECT_EQ(*results.at(1).id(), "4");
        EXPECT_EQ(*results.at(2).id(), "3");
        EXPECT_EQ(*results.at(3).id(), "1");
    }
    {
        http::MockRequest request(
            http::POST,
            http::URL(URL_CANCEL)
                .addParam("id", std::to_string(testValidation().id()))
        );
        auto response = yacare::performTestRequest(request);
        ASSERT_EQ(response.status, 200);

        const auto results = unpackSprotoRepeated<srelease::ValidationError>(
            boost::lexical_cast<srelease::Validation>(response.body)
                .results().at(0).errors()
        );

        ASSERT_EQ(results.size(), 4u);
        EXPECT_EQ(*results.at(0).id(), "2");
        EXPECT_EQ(*results.at(1).id(), "4");
        EXPECT_EQ(*results.at(2).id(), "3");
        EXPECT_EQ(*results.at(3).id(), "1");
    }
}

/*
TEST_F(ValidationFixture, test_release_validation_api_no_authorization)
{
    const auto login = "john";
    auth::UserInfo userInfo{};
    userInfo.setLogin(login);
    yacare::tests::UserInfoFixture userInfoFixture(userInfo);

    http::MockRequest request(http::POST, http::URL(URL_START));
    auto response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 403);

    request = http::MockRequest(http::POST, http::URL(URL_CANCEL));
    response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 403);

    request = http::MockRequest(
        http::GET,
        http::URL(URL_GET).addParam("id", "1")
    );
    response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 403);

    request = http::MockRequest(http::GET, http::URL(URL_GET_ALL));
    response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 403);
}

TEST_F(ValidationFixture, test_release_validation_api_no_edit_access)
{
    std::string login = "john";
    auto txn = txnHandle();
    idm::IdmService(*txn, login)
        .addRole(idm::parseSlugPath("project/mapsfactory/role/viewer"));
    txn->commit();

    auth::UserInfo userInfo{};
    userInfo.setLogin(login);
    yacare::tests::UserInfoFixture userInfoFixture(userInfo);

    http::MockRequest request(http::POST, http::URL(URL_START));
    auto response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 403);

    request = http::MockRequest(http::POST, http::URL(URL_CANCEL));
    response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 403);
}
*/

} // namespace maps::factory::backend::tests
