#include <maps/factory/services/backend/tests/test_utils.h>

#include <maps/factory/libs/db/delivery_gateway.h>
#include <maps/factory/libs/db/mosaic_source_gateway.h>
#include <maps/factory/libs/sproto_helpers/etag_utils.h>
#include <maps/factory/libs/sproto_helpers/mosaic_source.h>

#include <library/cpp/testing/gtest/gtest.h>

namespace maps::factory::backend::tests {

namespace {

using namespace sproto_helpers;

const std::string URL_GET = "http://localhost/v1/mosaic-sources/get";
const std::string URL_BULK_GET = "http://localhost/v1/mosaic-sources/bulk_get";
const std::string URL_SEARCH = "http://localhost/v1/mosaic-sources/search";
const std::string URL_UPDATE = "http://localhost/v1/mosaic-sources/update";

class MosaicSourceFixture : public BackendFixture {
public:
    MosaicSourceFixture()
        : testMosaicSource_(createTestMosaicSource())
    {
        auto txn = txnHandle();

        auto delivery = createTestDelivery();
        db::DeliveryGateway(*txn).insert(delivery);

        auto mosaicSource = createTestMosaicSource();
        db::MosaicSourceGateway(*txn).insert(testMosaicSource_);

        txn->commit();
    }

    const db::MosaicSource& testMosaicSource() { return testMosaicSource_; }

private:
    db::MosaicSource testMosaicSource_;
};

} // namespace

TEST_F(MosaicSourceFixture, test_mosaic_source_api_get)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    http::MockRequest request(
        http::GET,
        http::URL(URL_GET)
            .addParam("id", testMosaicSource().id())
    );
    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(response.status, 200);

    auto respMosaicSource = boost::lexical_cast<smosaics::MosaicSource>(response.body);

    EXPECT_EQ(respMosaicSource.name(), MOSAIC_SOURCE_NAME);
    EXPECT_EQ(*respMosaicSource.status(), convertToSproto(MOSAIC_SOURCE_STATUS));
    EXPECT_EQ(respMosaicSource.satellite(), SATELLITE);
    EXPECT_TRUE(
        convertFromSprotoGeoGeometry(
            unpackSprotoRepeated<scommon::geometry::Polygon>(
                respMosaicSource.geometry()
            )
        ) == GEOMETRY
    );
    EXPECT_EQ(
        convertFromSproto(
            unpackSprotoRepeated<scommon::KeyValuePair>(
                respMosaicSource.metadata()
            )
        ),
        METADATA
    );
    EXPECT_EQ(*respMosaicSource.resolutionMeterPerPx(), RESOLUTION);
    EXPECT_EQ(*respMosaicSource.offnadir(), OFFNADIR);
    EXPECT_EQ(*respMosaicSource.heading(), HEADING);
    EXPECT_EQ(
        chrono::convertFromUnixTime(
            respMosaicSource.collectionDate()->value()
        ),
        COLLECTION_TIME
    );
    EXPECT_EQ(respMosaicSource.tags().at(0), TAGS.at(0));
}

TEST_F(MosaicSourceFixture, test_mosaic_source_api_old_bulk_get)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    http::MockRequest request(
        http::GET,
        http::URL(URL_BULK_GET)
            .addParam("ids", testMosaicSource().id())
    );
    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(response.status, 200);

    auto respMosaicSource = boost::lexical_cast<smosaics::MosaicSources>(response.body)
        .mosaicSources().at(0);

    EXPECT_EQ(respMosaicSource.name(), MOSAIC_SOURCE_NAME);
    EXPECT_EQ(*respMosaicSource.status(), convertToSproto(MOSAIC_SOURCE_STATUS));
    EXPECT_EQ(respMosaicSource.satellite(), SATELLITE);
    EXPECT_TRUE(
        convertFromSprotoGeoGeometry(
            unpackSprotoRepeated<scommon::geometry::Polygon>(
                respMosaicSource.geometry()
            )
        ) == GEOMETRY
    );
    EXPECT_EQ(
        convertFromSproto(
            unpackSprotoRepeated<scommon::KeyValuePair>(
                respMosaicSource.metadata()
            )
        ),
        METADATA
    );
    EXPECT_EQ(*respMosaicSource.resolutionMeterPerPx(), RESOLUTION);
    EXPECT_EQ(*respMosaicSource.offnadir(), OFFNADIR);
    EXPECT_EQ(*respMosaicSource.heading(), HEADING);
    EXPECT_EQ(
        chrono::convertFromUnixTime(
            respMosaicSource.collectionDate()->value()
        ),
        COLLECTION_TIME
    );
    EXPECT_EQ(respMosaicSource.tags().at(0), TAGS.at(0));
}

TEST_F(MosaicSourceFixture, test_mosaic_source_api_bulk_get)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    http::MockRequest request(http::POST, http::URL(URL_BULK_GET));
    request.body = boost::lexical_cast<std::string>(
        sproto_helpers::convertToSproto<smosaics::GetMosaicSourcesRequest>({
            testMosaicSource().id()
        })
    );
    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(response.status, 200);

    auto respMosaicSource = boost::lexical_cast<smosaics::MosaicSources>(response.body)
        .mosaicSources().at(0);

    EXPECT_EQ(respMosaicSource.name(), MOSAIC_SOURCE_NAME);
    EXPECT_EQ(*respMosaicSource.status(), convertToSproto(MOSAIC_SOURCE_STATUS));
    EXPECT_EQ(respMosaicSource.satellite(), SATELLITE);
    EXPECT_TRUE(
        convertFromSprotoGeoGeometry(
            unpackSprotoRepeated<scommon::geometry::Polygon>(
                respMosaicSource.geometry()
            )
        ) == GEOMETRY
    );
    EXPECT_EQ(
        convertFromSproto(
            unpackSprotoRepeated<scommon::KeyValuePair>(
                respMosaicSource.metadata()
            )
        ),
        METADATA
    );
    EXPECT_EQ(*respMosaicSource.resolutionMeterPerPx(), RESOLUTION);
    EXPECT_EQ(*respMosaicSource.offnadir(), OFFNADIR);
    EXPECT_EQ(*respMosaicSource.heading(), HEADING);
    EXPECT_EQ(
        chrono::convertFromUnixTime(
            respMosaicSource.collectionDate()->value()
        ),
        COLLECTION_TIME
    );
    EXPECT_EQ(respMosaicSource.tags().at(0), TAGS.at(0));
}

TEST_F(MosaicSourceFixture, test_mosaic_source_api_search_basic)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    http::MockRequest request(
        http::GET,
        http::URL(URL_SEARCH)
            .addParam("text", testMosaicSource().name())
    );
    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(response.status, 200);

    auto respMosaicSources = boost::lexical_cast<smosaics::MosaicSources>(response.body);
    ASSERT_EQ(respMosaicSources.mosaicSources().size(), 1u);
    const auto respMosaicSource = respMosaicSources.mosaicSources().at(0);
    EXPECT_EQ(respMosaicSource.name(), testMosaicSource().name());
}

TEST_F(BackendFixture, test_mosaic_source_api_search_relevance)
{
    auto makeMosaic =
        [](const auto& name) {
            auto ms = createTestMosaicSource();
            ms.setName(name);
            return ms;
        };

    auto collectNames =
        [](const auto& mosaicSources) -> std::vector<std::string> {
            std::vector<std::string> names;
            names.reserve(mosaicSources.size());
            for (const auto& mosaicSource: mosaicSources) {
                names.push_back(mosaicSource.name());
            }
            return names;
        };

    const std::vector<std::string> testMosaicsNames{
        "abcd",
        "abc",
        "abd",
        "ad",
        "ef"
    };

    {
        auto txn = txnHandle();
        for (const auto& name: testMosaicsNames) {
            auto ms = makeMosaic(name);
            db::MosaicSourceGateway(*txn).insert(ms);
        }

        txn->commit();
    }

    struct TestDatum {
        std::string text;
        size_t skip;
        size_t results;
        std::vector<std::string> expectedNames;
    };

    std::vector<TestDatum> testData{
        {"abcd", 0, 10, {"abcd", "abc"}},
        {"abcd", 0, 1, {"abcd"}},
        {"abcd", 1, 1, {"abc"}},
        {"ad", 0, 10, {"ad"}},
    };

    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    for (const auto& td: testData) {
        SCOPED_TRACE(td.text);

        http::MockRequest request(
            http::GET,
            http::URL(URL_SEARCH)
                .addParam("text", td.text)
                .addParam("results", td.results)
                .addParam("skip", td.skip)
        );
        auto response = yacare::performTestRequest(request);
        ASSERT_EQ(response.status, 200);

        const auto mosaicSources = boost::lexical_cast<smosaics::MosaicSources>(response.body);

        EXPECT_THAT(
            collectNames(mosaicSources.mosaicSources()),
            ::testing::ContainerEq(td.expectedNames)
        );
    }
}

TEST_F(MosaicSourceFixture, test_mosaic_source_api_old_bulk_get_special_cases)
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
                .addParam("ids", std::to_string(testMosaicSource().id()) + ",1111")
        );
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(response.status, 200);
    }
}

TEST_F(MosaicSourceFixture, test_mosaic_source_api_bulk_get_special_cases)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));
    {
        http::MockRequest request(http::POST, http::URL(URL_BULK_GET));
        sproto_helpers::smosaics::GetMosaicSourcesRequest ids;
        ids.mosaic_source_ids().push_back("a");
        request.body = boost::lexical_cast<std::string>(ids);
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(response.status, 400);
    }
    {
        http::MockRequest request(http::POST, http::URL(URL_BULK_GET));
        request.body = boost::lexical_cast<std::string>(
            sproto_helpers::convertToSproto<smosaics::GetMosaicSourcesRequest>({
                testMosaicSource().id(), 1111
            })
        );
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(response.status, 200);
    }
}

TEST_F(MosaicSourceFixture, test_mosaic_source_api_update)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    auto uMosaicSource = testMosaicSource();
    updateTestMosaicSource(uMosaicSource);
    auto sprotoUMosaicSource = convertToSproto(uMosaicSource);
    sprotoUMosaicSource.etag() = sproto_helpers::calculateEtag(testMosaicSource());

    http::MockRequest request(http::POST, http::URL(URL_UPDATE));
    request.body = boost::lexical_cast<std::string>(sprotoUMosaicSource);
    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(response.status, 200);

    auto respMosaicSource = boost::lexical_cast<smosaics::MosaicSource>(response.body);

    auto dbMosaicSource = createTestMosaicSource();
    ASSERT_NO_THROW(
        dbMosaicSource = db::MosaicSourceGateway(*txnHandle())
            .loadById(db::parseId(respMosaicSource.id()));
    );

    EXPECT_EQ(respMosaicSource.name(), MOSAIC_SOURCE_NAME);
    EXPECT_EQ(*respMosaicSource.status(), convertToSproto(MOSAIC_SOURCE_STATUS));
    EXPECT_EQ(respMosaicSource.satellite(), SATELLITE);
    EXPECT_TRUE(
        convertFromSprotoGeoGeometry(
            unpackSprotoRepeated<scommon::geometry::Polygon>(
                respMosaicSource.geometry()
            )
        ) == GEOMETRY
    );
    EXPECT_EQ(
        convertFromSproto(
            unpackSprotoRepeated<scommon::KeyValuePair>(
                respMosaicSource.metadata()
            )
        ),
        METADATA
    );
    EXPECT_EQ(*respMosaicSource.resolutionMeterPerPx(), RESOLUTION);
    EXPECT_EQ(*respMosaicSource.offnadir(), OFFNADIR);
    EXPECT_EQ(*respMosaicSource.heading(), HEADING);
    EXPECT_EQ(
        chrono::convertFromUnixTime(
            respMosaicSource.collectionDate()->value()
        ),
        COLLECTION_TIME
    );
    EXPECT_EQ(respMosaicSource.tags().at(0), TAGS_U.at(0));

    EXPECT_EQ(dbMosaicSource.name(), MOSAIC_SOURCE_NAME);
    EXPECT_EQ(dbMosaicSource.status(), MOSAIC_SOURCE_STATUS);
    EXPECT_EQ(dbMosaicSource.satellite(), SATELLITE);
    EXPECT_TRUE(dbMosaicSource.mercatorGeom() == GEOMETRY);
    EXPECT_EQ(dbMosaicSource.metadata(), METADATA);
    EXPECT_EQ(*dbMosaicSource.resolutionMeterPerPx(), RESOLUTION);
    EXPECT_EQ(*dbMosaicSource.offnadir(), OFFNADIR);
    EXPECT_EQ(*dbMosaicSource.heading(), HEADING);
    EXPECT_EQ(*dbMosaicSource.collectionDate(), COLLECTION_TIME);
    EXPECT_EQ(dbMosaicSource.tags().at(0), TAGS_U.at(0));
}

TEST_F(MosaicSourceFixture, test_mosaic_source_api_not_found)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));
    {
        http::MockRequest request(http::POST, http::URL(URL_UPDATE));
        auto uMosaicSource = convertToSproto(testMosaicSource());
        uMosaicSource.id() = "1111";
        request.body = boost::lexical_cast<std::string>(uMosaicSource);
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

TEST_F(MosaicSourceFixture, test_mosaic_source_api_etag_error)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    auto source = testMosaicSource();
    const auto sprotoSource = convertToSproto(source);
    updateTestMosaicSource(source);
    auto txn = txnHandle();
    db::MosaicSourceGateway(*txn).update(source);
    txn->commit();

    http::MockRequest request(http::POST, http::URL(URL_UPDATE));
    request.body = boost::lexical_cast<std::string>(sprotoSource);
    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(response.status, 409);
}

/*
TEST_F(MosaicSourceFixture, test_mosaic_source_api_no_authorization)
{
    const auto login = "john";
    auth::UserInfo userInfo{};
    userInfo.setLogin(login);
    yacare::tests::UserInfoFixture userInfoFixture(userInfo);

    http::MockRequest request(http::POST, http::URL(URL_UPDATE));
    auto response = yacare::performTestRequest(request);
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
}

TEST_F(MosaicSourceFixture, test_mosaic_source_api_no_edit_access)
{
    std::string login = "john";
    auto txn = txnHandle();
    idm::IdmService(*txn, login)
        .addRole(idm::parseSlugPath("project/mapsfactory/role/viewer"));
    txn->commit();

    auth::UserInfo userInfo{};
    userInfo.setLogin(login);
    yacare::tests::UserInfoFixture userInfoFixture(userInfo);

    http::MockRequest request(http::POST, http::URL(URL_UPDATE));
    auto response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 403);
}
*/

} // namespace maps::factory::backend::tests
