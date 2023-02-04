#include <maps/factory/services/backend/tests/test_utils.h>

#include <maps/factory/libs/db/secret_object_gateway.h>
#include <yandex/maps/proto/factory/masking_zones.sproto.h>

#include <library/cpp/testing/gtest/gtest.h>
#include <maps/libs/geolib/include/conversion.h>
#include <maps/libs/geolib/include/spatial_relation.h>
#include <yandex/maps/geolib3/sproto.h>
#include <maps/libs/geolib/include/test_tools/io_operations.h>

namespace maps::factory::backend::tests {

namespace {

namespace smz = yandex::maps::sproto::factory::masking_zones;

const std::string URL_CREATE = "http://localhost/v1/masking_zones/create";
const std::string URL_GET = "http://localhost/v1/masking_zones/get";
const std::string URL_UPDATE = "http://localhost/v1/masking_zones/update";
const std::string URL_DELETE = "http://localhost/v1/masking_zones/delete";
const std::string URL_IMPORT = "http://localhost/v1/masking_zones/import";
const std::string URL_EXPORT = "http://localhost/v1/masking_zones/export";

static const geolib3::MultiPolygon2 TEST_GEOM(
    {geolib3::Polygon2(geolib3::PointsVector{{37.3438, 55.9119},
        {37.4591, 55.9134},
        {37.4564, 55.8466},
        {37.3493, 55.8435}})});

} // namespace

TEST_F(BackendFixture, test_masking_zones_create)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    http::MockRequest request(http::POST, http::URL(URL_CREATE));
    smz::CreateMaskingZoneRequest createRequest;
    createRequest.geometry() = geolib3::sproto::encode(TEST_GEOM);
    const size_t ZOOM_MIN = 10;
    createRequest.zoomMin() = ZOOM_MIN;

    request.body = boost::lexical_cast<std::string>(createRequest);
    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(response.status, 200);

    const auto maskingZone = boost::lexical_cast<smz::MaskingZone>(response.body);
    EXPECT_TRUE(
        geolib3::sproto::decode(maskingZone.geometry()) == TEST_GEOM
    ) << geolib3::io::geometryToString(geolib3::sproto::decode(maskingZone.geometry()))
                    << " is not equal to "
                    << geolib3::io::geometryToString(TEST_GEOM);
    EXPECT_EQ(maskingZone.id(), "1");
    ASSERT_TRUE(maskingZone.zoomMin());
    EXPECT_EQ(*maskingZone.zoomMin(), ZOOM_MIN);

    auto txn = txnHandle();
    auto objects = db::SecretObjectGateway(*txn).load();
    ASSERT_EQ(objects.size(), 1u);

    EXPECT_TRUE(objects[0].geodeticGeom() == TEST_GEOM);
    EXPECT_EQ(objects[0].zMin(), (int) ZOOM_MIN);
    EXPECT_TRUE(objects[0].updatedAt().has_value());
}

TEST_F(BackendFixture, test_masking_zones_get_existing)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    db::SecretObject object("", geolib3::convertGeodeticToMercator(TEST_GEOM), 15);
    {
        auto txn = txnHandle();
        db::SecretObjectGateway(*txn).insert(object);
        txn->commit();
    }
    http::MockRequest request(
        http::GET,
        http::URL(URL_GET).addParam("id", std::to_string(object.id())));

    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(response.status, 200);
    const auto maskingZone = boost::lexical_cast<smz::MaskingZone>(response.body);
    EXPECT_TRUE(
        geolib3::sproto::decode(maskingZone.geometry()) == TEST_GEOM
    );
    EXPECT_EQ(maskingZone.id(), std::to_string(object.id()));
    ASSERT_TRUE(maskingZone.zoomMin());
    EXPECT_EQ(*maskingZone.zoomMin(), (unsigned int) object.zMin());
}

TEST_F(BackendFixture, test_masking_zones_get_not_existing)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    http::MockRequest request(
        http::GET,
        http::URL(URL_GET).addParam("id", "1"));

    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(response.status, 404);
}

TEST_F(BackendFixture, test_masking_zones_delete_existing)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    db::SecretObject object("", geolib3::convertGeodeticToMercator(TEST_GEOM), 15);
    {
        auto txn = txnHandle();
        db::SecretObjectGateway(*txn).insert(object);
        txn->commit();
    }
    http::MockRequest request(
        http::DELETE,
        http::URL(URL_DELETE).addParam("id", std::to_string(object.id())));

    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(response.status, 200);

    EXPECT_EQ(db::SecretObjectGateway(*txnHandle()).count(), 0u);
}

TEST_F(BackendFixture, test_masking_zones_delete_not_existing)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    http::MockRequest request(
        http::DELETE,
        http::URL(URL_DELETE).addParam("id", "1"));

    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(response.status, 200);
}

TEST_F(BackendFixture, test_masking_zones_update_existing)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    db::SecretObject object("", geolib3::convertGeodeticToMercator(TEST_GEOM), 15);
    {
        auto txn = txnHandle();
        db::SecretObjectGateway(*txn).insert(object);
        txn->commit();
    }
    http::MockRequest request(
        http::PUT,
        http::URL(URL_UPDATE).addParam("id", std::to_string(object.id())));

    smz::UpdateMaskingZoneRequest updateRequest;
    const geolib3::MultiPolygon2 NEW_TEST_GEOM(
        {geolib3::Polygon2(geolib3::PointsVector{{46.3438, 34.9119},
            {46.4591, 34.9134},
            {46.4564, 34.8466},
            {46.3493, 34.8435}})});
    updateRequest.geometry() = geolib3::sproto::encode(NEW_TEST_GEOM);
    const size_t ZOOM_MIN = 12;
    updateRequest.zoomMin() = ZOOM_MIN;

    request.body = boost::lexical_cast<std::string>(updateRequest);

    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(response.status, 200);

    const auto maskingZone = boost::lexical_cast<smz::MaskingZone>(response.body);
    EXPECT_TRUE(
        geolib3::sproto::decode(maskingZone.geometry()) == NEW_TEST_GEOM
    );
    EXPECT_GT(maskingZone.id().size(), 0u);
    ASSERT_TRUE(maskingZone.zoomMin());
    EXPECT_EQ(*maskingZone.zoomMin(), (unsigned int) ZOOM_MIN);

    auto txn = txnHandle();
    object = db::SecretObjectGateway(*txn).loadById(object.id());

    EXPECT_TRUE(object.geodeticGeom() == NEW_TEST_GEOM);
    EXPECT_EQ(object.zMin(), (int) ZOOM_MIN);
    EXPECT_TRUE(object.updatedAt().has_value());
}

TEST_F(BackendFixture, test_masking_zones_update_non_existing)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    db::SecretObject object("", geolib3::convertGeodeticToMercator(TEST_GEOM), 15);
    http::MockRequest request(
        http::PUT,
        http::URL(URL_UPDATE).addParam("id", "1"));

    smz::UpdateMaskingZoneRequest updateRequest;
    const geolib3::MultiPolygon2 NEW_TEST_GEOM(
        {geolib3::Polygon2(geolib3::PointsVector{{46.3438, 34.9119},
            {46.4591, 34.9134},
            {46.4564, 34.8466},
            {46.3493, 34.8435}})});
    updateRequest.geometry() = geolib3::sproto::encode(NEW_TEST_GEOM);
    const size_t ZOOM_MIN = 12;
    updateRequest.zoomMin() = ZOOM_MIN;

    request.body = boost::lexical_cast<std::string>(updateRequest);

    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(response.status, 404);
}

TEST_F(BackendFixture, test_masking_zones_export)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    auto object = db::SecretObject("", geolib3::convertGeodeticToMercator(TEST_GEOM), 15)
        .setUpdatedAt(chrono::TimePoint::clock::now());
    {
        auto txn = txnHandle();
        db::SecretObjectGateway(*txn).insert(object);
        txn->commit();
    }
    auto request = http::MockRequest(http::GET, http::URL(URL_EXPORT));
    auto response = yacare::performTestRequest(request);

    EXPECT_EQ(response.status, 200);
    EXPECT_EQ(response.headers.at("Content-Type"), "application/json");
    EXPECT_EQ(response.headers.at("Content-Disposition"), "attachment; filename=\"masking_zones.json\"");

    auto jsonValue = json::Value::fromString(response.body);
    EXPECT_EQ(jsonValue["type"].as<std::string>(), "FeatureCollection");
    auto jsonFeatures = jsonValue["features"];
    ASSERT_EQ(jsonFeatures.size(), 1u);
    const auto& firstFeature = jsonFeatures[0];
    EXPECT_EQ(firstFeature["geometry"]["type"].as<std::string>(), "MultiPolygon");
    EXPECT_TRUE(firstFeature["properties"]["updatedAt"].exists());
}

TEST_F(BackendFixture, test_masking_zones_import_valid)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));
    auto request = http::MockRequest(http::POST, http::URL(URL_IMPORT));
    request.body = R"({
	"type": "FeatureCollection",
	"features": [
		{
			"type": "Feature",
			"geometry": {
				"type": "Polygon",
				"coordinates": [
					[
						[87.514482, 53.966435],
						[87.339283, 53.984519],
						[86.932765, 53.966435],
						[86.853903, 54.110669],
						[87.514482, 53.966435]
					]
				]
			}
		}
	]
})";

    auto response = yacare::performTestRequest(request);

    EXPECT_EQ(response.status, 200);
    EXPECT_EQ(db::SecretObjectGateway(*txnHandle()).count(), 1u);
}

TEST_F(BackendFixture, test_masking_zones_import_invalid)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    auto request = http::MockRequest(http::POST, http::URL(URL_IMPORT));
    request.body = "text";

    auto response = yacare::performTestRequest(request);

    EXPECT_EQ(response.status, 400);
}

/*
TEST_F(BackendFixture, test_masking_zones_no_authorization)
{
    const auto login = "john";
    auth::UserInfo userInfo{};
    userInfo.setLogin(login);
    yacare::tests::UserInfoFixture userInfoFixture(userInfo);

    http::MockRequest request(http::POST, http::URL(URL_CREATE));
    auto response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 403);

    request = http::MockRequest(
        http::PUT,
        http::URL(URL_UPDATE).addParam("id", "1")
    );
    response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 403);

    request = http::MockRequest(
        http::GET,
        http::URL(URL_GET).addParam("id", "1")
    );
    response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 403);

    request = http::MockRequest(
        http::DELETE,
        http::URL(URL_DELETE).addParam("id", "1")
    );
    response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 403);

    request = http::MockRequest(http::POST, http::URL(URL_IMPORT));
    response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 403);

    request = http::MockRequest(http::GET, http::URL(URL_EXPORT));
    response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 403);
}

TEST_F(BackendFixture, test_masking_zones_no_edit_access)
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

    request = http::MockRequest(
        http::PUT,
        http::URL(URL_UPDATE).addParam("id", "1")
    );
    response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 403);

    request = http::MockRequest(
        http::DELETE,
        http::URL(URL_DELETE).addParam("id", "1")
    );
    response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 403);

    request = http::MockRequest(http::POST, http::URL(URL_IMPORT));
    response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 403);
}
*/

} // maps::factory::backend::tests
