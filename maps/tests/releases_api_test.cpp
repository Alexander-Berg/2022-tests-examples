#include <maps/factory/services/backend/tests/test_utils.h>

#include <maps/factory/libs/db/delivery_gateway.h>
#include <maps/factory/libs/db/mosaic_gateway.h>
#include <maps/factory/libs/db/mosaic_source_gateway.h>
#include <maps/factory/libs/db/release_gateway.h>
#include <maps/factory/libs/processing/publication.h>
#include <maps/factory/libs/sproto_helpers/etag_utils.h>
#include <maps/factory/libs/sproto_helpers/release.h>
#include <maps/factory/libs/tasks/impl/tasks_gateway.h>

#include <library/cpp/testing/gtest/gtest.h>

namespace maps::factory::backend::tests {

namespace {

using namespace sproto_helpers;

const std::string URL_CREATE = "http://localhost/v1/releases/create";
const std::string URL_UPDATE = "http://localhost/v1/releases/update";
const std::string URL_GET = "http://localhost/v1/releases/get";
const std::string URL_BULK_GET = "http://localhost/v1/releases/bulk_get";
const std::string URL_SEARCH = "http://localhost/v1/releases/search";
const std::string URL_DELETE = "http://localhost/v1/releases/delete";

class ReleaseFixture : public BackendFixture {
public:
    ReleaseFixture()
        : testRelease_(createTestRelease())
    {
        releaseEtag_ = calculateEtag(testRelease_);
        auto txn = txnHandle();
        db::ReleaseGateway(*txn).insert(testRelease_);
        txn->commit();
    }

    const db::Release& testRelease() { return testRelease_; }

    const std::string& testReleaseEtag() { return releaseEtag_; }

    const db::Release& syncTestRelease()
    {
        testRelease_ = db::ReleaseGateway(*txnHandle()).loadById(testRelease_.id());
        releaseEtag_ = calculateEtag(testRelease_);
        return testRelease_;
    }

private:
    db::Release testRelease_;
    std::string releaseEtag_;
};

void makeUpdateRequest(db::Release& release, const db::ReleaseStatus status)
{
    const std::string etag = calculateEtag(release);
    release.setStatus(status);
    http::MockRequest request(http::POST, http::URL(URL_UPDATE));
    auto sprotoRelease = sproto_helpers::convertToSproto(release);
    sprotoRelease.etag() = etag;
    request.body = boost::lexical_cast<std::string>(sprotoRelease);
    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(response.status, 200);
}

} // namespace

TEST_F(BackendFixture, test_releases_api_create)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    srelease::ReleaseData sprotoNewReleaseData;
    sprotoNewReleaseData.name() = RELEASE_NAME;
    sprotoNewReleaseData.targetStatus() = convertToSproto(RELEASE_STATUS);

    http::MockRequest request(http::POST, http::URL(URL_CREATE));
    request.body = boost::lexical_cast<std::string>(sprotoNewReleaseData);
    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(response.status, 200);

    auto respRelease = boost::lexical_cast<srelease::Release>(response.body);
    ASSERT_NE(respRelease.id(), "0");

    auto dbRelease = createTestRelease();
    ASSERT_NO_THROW(
        dbRelease = db::ReleaseGateway(*txnHandle())
            .loadById(db::parseId(respRelease.id()));
    );

    EXPECT_EQ(respRelease.name(), RELEASE_NAME);
    EXPECT_EQ(*respRelease.currentStatus(), convertToSproto(RELEASE_STATUS));
    EXPECT_EQ(*respRelease.modifiedBy(), USER_NAME);
    EXPECT_TRUE(respRelease.modifiedAt());

    EXPECT_EQ(dbRelease.name(), RELEASE_NAME);
    EXPECT_EQ(dbRelease.status(), RELEASE_STATUS);
    EXPECT_EQ(dbRelease.modifiedBy(), USER_NAME);
    EXPECT_TRUE(dbRelease.modifiedAt());
}

TEST_F(ReleaseFixture, test_releases_api_update)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    auto uRelease = testRelease();
    updateTestRelease(uRelease);
    auto sprotoURelease = convertToSproto(uRelease);
    sprotoURelease.etag() = testReleaseEtag();

    http::MockRequest request(http::POST, http::URL(URL_UPDATE));
    request.body = boost::lexical_cast<std::string>(sprotoURelease);
    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(response.status, 200);

    auto respRelease = boost::lexical_cast<srelease::Release>(response.body);

    auto dbRelease = createTestRelease();
    ASSERT_NO_THROW(
        dbRelease = db::ReleaseGateway(*txnHandle())
            .loadById(db::parseId(respRelease.id()));
    );

    EXPECT_EQ(respRelease.name(), RELEASE_NAME_U);
    EXPECT_EQ(*respRelease.currentStatus(), convertToSproto(RELEASE_STATUS_U));
    EXPECT_EQ(*respRelease.modifiedBy(), USER_NAME);
    EXPECT_TRUE(respRelease.modifiedAt());

    EXPECT_EQ(dbRelease.name(), RELEASE_NAME_U);
    EXPECT_EQ(dbRelease.status(), RELEASE_STATUS_U);
    EXPECT_EQ(dbRelease.modifiedBy(), USER_NAME);
    EXPECT_TRUE(dbRelease.modifiedAt());
}

TEST_F(ReleaseFixture, test_change_release_state_forward)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    db::Release release = syncTestRelease();
    makeUpdateRequest(release, db::ReleaseStatus::Ready);
    auto dbRelease = db::ReleaseGateway(*txnHandle()).loadById(release.id());
    EXPECT_TRUE(dbRelease.issue().has_value());
    EXPECT_EQ(dbRelease.status(), db::ReleaseStatus::Ready);

    release = syncTestRelease();
    makeUpdateRequest(release, db::ReleaseStatus::Testing);
    EXPECT_EQ(db::ReleaseGateway(*txnHandle()).loadById(release.id()).status(),
        db::ReleaseStatus::MovingToTesting);
    EXPECT_EQ(tasks::TasksGateway(*txnHandle())
        .count(tasks::table::Task::name == processing::PublishReleaseToTesting::name), 1u);

    auto txn = txnHandle();
    release.setStatus(db::ReleaseStatus::Testing);
    db::ReleaseGateway(*txn).update(release);
    txn->commit();

    release = syncTestRelease();
    makeUpdateRequest(release, db::ReleaseStatus::Production);
    EXPECT_EQ(db::ReleaseGateway(*txnHandle()).loadById(release.id()).status(),
        db::ReleaseStatus::MovingToProduction);
    EXPECT_EQ(tasks::TasksGateway(*txnHandle())
        .count(tasks::table::Task::name == processing::PublishReleaseToProduction::name), 1u);
}

TEST_F(ReleaseFixture, test_change_release_state_backward)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    auto txn = txnHandle();
    db::Release release = testRelease();
    release.setStatus(db::ReleaseStatus::Production);
    release.setIssue(1);
    db::ReleaseGateway(*txn).update(release);
    txn->commit();

    makeUpdateRequest(release, db::ReleaseStatus::Testing);
    EXPECT_EQ(db::ReleaseGateway(*txnHandle()).loadById(release.id()).status(),
        db::ReleaseStatus::RollbackProductionToTesting);
    EXPECT_EQ(tasks::TasksGateway(*txnHandle())
        .count(tasks::table::Task::name == processing::RollbackReleaseFromProductionToTesting::name), 1u);

    txn = txnHandle();
    release.setStatus(db::ReleaseStatus::Testing);
    db::ReleaseGateway(*txn).update(release);
    txn->commit();

    release = syncTestRelease();
    makeUpdateRequest(release, db::ReleaseStatus::Ready);
    EXPECT_EQ(db::ReleaseGateway(*txnHandle()).loadById(release.id()).status(),
        db::ReleaseStatus::RollbackTestingToReady);
    EXPECT_EQ(tasks::TasksGateway(*txnHandle())
        .count(tasks::table::Task::name == processing::RollbackReleaseFromTestingToReady::name), 1u);

    txn = txnHandle();
    release.setStatus(db::ReleaseStatus::Ready);
    db::ReleaseGateway(*txn).update(release);
    txn->commit();

    release = syncTestRelease();
    makeUpdateRequest(release, db::ReleaseStatus::Frozen);
    auto dbRelease = db::ReleaseGateway(*txnHandle()).loadById(release.id());
    EXPECT_FALSE(dbRelease.issue().has_value());
    EXPECT_EQ(dbRelease.status(), db::ReleaseStatus::Frozen);
}

TEST_F(ReleaseFixture, test_change_release_state_while_busy)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    auto txn = txnHandle();
    db::Release originalRelease = testRelease();
    db::Release release("test_release2");
    originalRelease.setStatus(db::ReleaseStatus::MovingToTesting);
    release.setStatus(db::ReleaseStatus::Ready)
           .setIssue(2);
    db::ReleaseGateway(*txn).update(originalRelease);
    db::ReleaseGateway(*txn).insert(release);
    txn->commit();

    const std::string etag = calculateEtag(release);
    release.setStatus(db::ReleaseStatus::Testing);
    http::MockRequest request(http::POST, http::URL(URL_UPDATE));
    auto sprotoRelease = sproto_helpers::convertToSproto(release);
    sprotoRelease.etag() = etag;
    request.body = boost::lexical_cast<std::string>(sprotoRelease);
    auto response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 400);
}

TEST_F(ReleaseFixture, test_change_release_state_to_production_while_ready)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    auto txn = txnHandle();
    db::Release originalRelease = testRelease();
    db::Release release("test_release2");
    originalRelease.setStatus(db::ReleaseStatus::Ready)
                   .setIssue(1);
    release.setStatus(db::ReleaseStatus::Testing)
           .setIssue(2);
    db::ReleaseGateway(*txn).update(originalRelease);
    db::ReleaseGateway(*txn).insert(release);
    txn->commit();

    const std::string etag = calculateEtag(release);
    release.setStatus(db::ReleaseStatus::Production);
    http::MockRequest request(http::POST, http::URL(URL_UPDATE));
    auto sprotoRelease = sproto_helpers::convertToSproto(release);
    sprotoRelease.etag() = etag;
    request.body = boost::lexical_cast<std::string>(sprotoRelease);
    auto response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 400);
}

TEST_F(ReleaseFixture, test_change_release_state_to_rollback_while_published)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    auto txn = txnHandle();
    db::Release originalRelease = testRelease();
    db::Release release("test_release2");
    originalRelease.setStatus(db::ReleaseStatus::Production)
                   .setIssue(2);
    release.setStatus(db::ReleaseStatus::Testing)
           .setIssue(1);
    db::ReleaseGateway(*txn).update(originalRelease);
    db::ReleaseGateway(*txn).insert(release);
    txn->commit();

    {
        const std::string etag = calculateEtag(release);
        release.setStatus(db::ReleaseStatus::Ready);
        http::MockRequest request(http::POST, http::URL(URL_UPDATE));
        auto sprotoRelease = sproto_helpers::convertToSproto(release);
        sprotoRelease.etag() = etag;
        request.body = boost::lexical_cast<std::string>(sprotoRelease);
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(response.status, 400);
    }

    txn = txnHandle();
    release.setStatus(db::ReleaseStatus::Production);
    db::ReleaseGateway(*txn).update(release);
    txn->commit();

    {
        const std::string etag = calculateEtag(release);
        release.setStatus(db::ReleaseStatus::Testing);
        http::MockRequest request(http::POST, http::URL(URL_UPDATE));
        auto sprotoRelease = sproto_helpers::convertToSproto(release);
        sprotoRelease.etag() = etag;
        request.body = boost::lexical_cast<std::string>(sprotoRelease);
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(response.status, 400);
    }
}

TEST_F(ReleaseFixture, test_change_release_state_to_testing_while_moving_to_production)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    auto txn = txnHandle();
    db::Release originalRelease = testRelease();
    db::Release release("test_release2");
    originalRelease.setStatus(db::ReleaseStatus::MovingToProduction)
                   .setIssue(1);
    release.setStatus(db::ReleaseStatus::Ready)
           .setIssue(2);
    db::ReleaseGateway(*txn).update(originalRelease);
    db::ReleaseGateway(*txn).insert(release);
    txn->commit();

    const std::string etag = calculateEtag(release);
    release.setStatus(db::ReleaseStatus::Testing);
    http::MockRequest request(http::POST, http::URL(URL_UPDATE));
    auto sprotoRelease = sproto_helpers::convertToSproto(release);
    sprotoRelease.etag() = etag;
    request.body = boost::lexical_cast<std::string>(sprotoRelease);
    auto response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 200);
}

TEST_F(ReleaseFixture, test_releases_api_get)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    http::MockRequest request(
        http::GET,
        http::URL(URL_GET)
            .addParam("id", testRelease().id())
    );
    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(response.status, 200);

    auto respRelease = boost::lexical_cast<srelease::Release>(response.body);

    EXPECT_EQ(respRelease.name(), RELEASE_NAME);
    EXPECT_EQ(*respRelease.currentStatus(), convertToSproto(RELEASE_STATUS));
}

TEST_F(ReleaseFixture, test_releases_api_bulk_get)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    http::MockRequest request(
        http::GET,
        http::URL(URL_BULK_GET)
            .addParam("ids", testRelease().id())
    );
    auto response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 200);

    auto respRelease = boost::lexical_cast<srelease::Releases>(response.body)
        .releases().at(0);

    EXPECT_EQ(respRelease.name(), RELEASE_NAME);
    EXPECT_EQ(*respRelease.currentStatus(), convertToSproto(RELEASE_STATUS));
}

TEST_F(ReleaseFixture, test_releases_api_bulk_get_special_cases)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));
    {
        http::MockRequest request(
            http::GET,
            http::URL(URL_BULK_GET)
                .addParam("ids", "a,b,c")
        );
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(response.status, 400);
    }
    {
        http::MockRequest request(
            http::GET,
            http::URL(URL_BULK_GET)
                .addParam("ids", std::to_string(testRelease().id()) + ",1111")
        );
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(response.status, 200);
    }
}

TEST_F(ReleaseFixture, test_releases_api_search)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    auto txn = txnHandle();
    auto delivery = createTestDelivery();
    db::DeliveryGateway(*txn).insert(delivery);
    txn->commit();

    txn = txnHandle();
    auto mosaicSource = createTestMosaicSource();
    db::MosaicSourceGateway(*txn).insert(mosaicSource);
    txn->commit();

    txn = txnHandle();
    db::MosaicGateway(*txn).insert(
        createTestMosaic(mosaicSource.id(), testRelease().id()));
    txn->commit();

    http::MockRequest request(
        http::GET,
        http::URL(URL_SEARCH)
            .addParam("status", "draft")
            .addParam("mosaic_source_id", mosaicSource.id())
    );
    auto response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 200);
    auto releases = boost::lexical_cast<srelease::Releases>(response.body);
    EXPECT_EQ(releases.releases().size(), 1u);
}

TEST_F(ReleaseFixture, test_releases_api_delete)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    http::MockRequest request(
        http::DELETE,
        http::URL(URL_DELETE)
            .addParam("id", testRelease().id())
    );
    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(response.status, 200);

    EXPECT_THROW(
        db::ReleaseGateway(*txnHandle()).loadById(testRelease().id()),
        maps::sql_chemistry::ObjectNotFound
    );
}

TEST_F(ReleaseFixture, test_releases_api_not_found)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));
    {
        http::MockRequest request(http::POST, http::URL(URL_UPDATE));
        auto uRelease = convertToSproto(testRelease());
        uRelease.id() = "1111";
        request.body = boost::lexical_cast<std::string>(uRelease);
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(response.status, 404);
    }
    {
        http::MockRequest request(
            http::GET,
            http::URL(URL_GET)
                .addParam("id", "1111")
        );
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(response.status, 404);
    }
}

TEST_F(ReleaseFixture, test_releases_api_etag_error)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    auto release = testRelease();
    const auto sprotoRelease = convertToSproto(release);
    updateTestRelease(release);
    auto txn = txnHandle();
    db::ReleaseGateway(*txn).update(release);
    txn->commit();

    http::MockRequest request(http::POST, http::URL(URL_UPDATE));
    request.body = boost::lexical_cast<std::string>(sprotoRelease);
    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(response.status, 409);
}

/*
TEST_F(ReleaseFixture, test_releases_api_no_authorization)
{
    const auto login = "john";
    auth::UserInfo userInfo{};
    userInfo.setLogin(login);
    yacare::tests::UserInfoFixture userInfoFixture(userInfo);

    http::MockRequest request(http::POST, http::URL(URL_CREATE));
    auto response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 403);

    request = http::MockRequest(http::POST, http::URL(URL_UPDATE));
    response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 403);

    request = http::MockRequest(
        http::GET,
        http::URL(URL_GET).addParam("id", "1")
    );
    response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 403);

    request = http::MockRequest(
        http::GET,
        http::URL(URL_BULK_GET).addParam("ids", "1")
    );
    response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 403);

    request = http::MockRequest(
        http::GET,
        http::URL(URL_SEARCH)
            .addParam("status", "draft")
            .addParam("mosaic_source_id", "1")
    );
    response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 403);

    request = http::MockRequest(
        http::DELETE,
        http::URL(URL_DELETE).addParam("id", "1")
    );
    response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 403);
}

TEST_F(ReleaseFixture, test_releases_api_no_edit_access)
{
    std::string login = "john";
    auto txn = txnHandle();
    idm::IdmService(*txn, login)
        .addRole(idm::parseSlugPath("project/mapsfactory/role/viewer"));
    txn->commit();

    auth::UserInfo userInfo{};
    userInfo.setLogin(login);
    yacare::tests::UserInfoFixture userInfoFixture(userInfo);

    http::MockRequest request(http::POST, http::URL(URL_CREATE));
    auto response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 403);

    request = http::MockRequest(http::POST, http::URL(URL_UPDATE));
    response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 403);

    request = http::MockRequest(
        http::DELETE,
        http::URL(URL_DELETE).addParam("id", "1")
    );
    response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 403);
}
*/

} // namespace maps::factory::backend::tests
