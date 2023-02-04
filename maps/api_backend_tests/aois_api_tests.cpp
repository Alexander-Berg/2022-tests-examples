#include <maps/factory/services/sputnica_back/tests/common/common.h>
#include <maps/factory/services/sputnica_back/tests/api_backend_tests/fixture.h>
#include <maps/factory/services/sputnica_back/tests/common/test_data.h>
#include <maps/factory/services/sputnica_back/lib/const.h>
#include <maps/factory/services/sputnica_back/lib/yacare_helpers.h>

#include <maps/factory/libs/db/aoi.h>
#include <maps/factory/libs/db/aoi_gateway.h>
#include <maps/factory/libs/db/aoi_validation_error_gateway.h>
#include <maps/factory/libs/db/order.h>
#include <maps/factory/libs/db/order_gateway.h>
#include <maps/factory/libs/sproto_helpers/order.h>

#include <maps/libs/common/include/exception.h>
#include <maps/libs/common/include/file_utils.h>
#include <maps/libs/http/include/test_utils.h>
#include <maps/libs/geolib/include/polygon.h>
#include <yandex/maps/geolib3/sproto.h>
#include <maps/libs/geolib/include/test_tools/comparison.h>
#include <maps/infra/yacare/include/test_utils.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <boost/lexical_cast.hpp>

#include <string>

namespace maps::factory::sputnica::tests {

const std::string FIRST_CATALOG_ID = "123";
const std::string SECOND_CATALOG_ID = "456";

const geolib3::Polygon2 AOI_BAD_GEOMETRY({{0, 0}, {100, 0}, {100, 100}, {0, 100}});

Y_UNIT_TEST_SUITE_F(aoi_api_tests, Fixture) {

Y_UNIT_TEST(test_aoi_workflow)
{
    pqxx::connection conn(postgres().connectionString());

    std::string orderId;
    std::string aoiId;
    //protobuf api can handle time points with second-wise precision. Trimming fractional part of now()
    chrono::TimePoint collectionStartTime =
        chrono::convertFromUnixTime(chrono::convertToUnixTime(chrono::TimePoint::clock::now()));
    std::optional<db::Delivery> delivery;
    //creating new order
    {
        pqxx::work txn(conn);
        db::Order order(ORDER_YEAR, ORDER_TYPE);
        db::OrderGateway(txn).insert(order);
        delivery = makeTestDelivery(txn);
        txn.commit();
        orderId = std::to_string(order.id());
    }

    //creating new Aoi
    {
        sproto_helpers::sfactory::AoiData sprotoAoiData;
        sprotoAoiData.name() = FIRST_AOI_NAME;
        sprotoAoiData.geometry() = geolib3::sproto::encode(geolib3::convertMercatorToGeodetic(AOI_GEOMETRY));
        sprotoAoiData.catalogIds() = {FIRST_CATALOG_ID, SECOND_CATALOG_ID};

        http::MockRequest rq(
            http::POST,
            http::URL("http://localhost/aois/add")
                .addParam("orderId", orderId)
        );
        setAuthHeaderFor(db::Role::Customer, rq);
        rq.body = boost::lexical_cast<std::string>(sprotoAoiData);
        auto resp = yacare::performTestRequest(rq);
        ASSERT_EQ(resp.status, 200);

        auto sprotoAoi = boost::lexical_cast<sproto_helpers::sfactory::Aoi>(resp.body);
        EXPECT_EQ(sprotoAoi.orderId(), orderId);
        EXPECT_EQ(sprotoAoi.name(), FIRST_AOI_NAME);
        const auto expectedBoundingBox =
            geolib3::convertMercatorToGeodetic(AOI_GEOMETRY).boundingBox();
        EXPECT_TRUE(
            geolib3::test_tools::approximateEqual(
                geolib3::sproto::decode(sprotoAoi.boundingBox()),
                expectedBoundingBox,
                0.01)
        );
        EXPECT_FALSE(sprotoAoi.collectionStartTime());
        EXPECT_FALSE(sprotoAoi.collectionEndTime());
        EXPECT_FALSE(sprotoAoi.maxOffNadirAngle());
        EXPECT_EQ(sprotoAoi.catalogIds()[0], FIRST_CATALOG_ID);
        EXPECT_EQ(sprotoAoi.catalogIds()[1], SECOND_CATALOG_ID);
        aoiId = sprotoAoi.id();
    }

    /*
     * First, testing if Order <-> Aoi rebinding is not allowed.
     * Then updating Aoi with new name
     */
    {
        sproto_helpers::sfactory::AoiData sprotoAoi;
        sprotoAoi.geometry() = geolib3::sproto::encode(geolib3::convertMercatorToGeodetic(AOI_GEOMETRY));
        sprotoAoi.name() = SECOND_AOI_NAME;
        sprotoAoi.collectionStartTime() = chrono::convertToUnixTime(collectionStartTime);


        /*
         * First, sending bad request which request backend to rebind aoi to another (non-existent) order
         */
        http::MockRequest badRequest(
            http::POST,
            http::URL("http://localhost/aois/update")
                .addParam("orderId", "31337")
                .addParam("aoiId", aoiId)
        );
        setAuthHeaderFor(db::Role::Customer, badRequest);
        badRequest.body = boost::lexical_cast<std::string>(sprotoAoi);
        auto badResponse = yacare::performTestRequest(badRequest);
        EXPECT_EQ(badResponse.status, 400);

        /*
         * Updating aoi data without rebinding
         */
        http::MockRequest goodRequest(
            http::POST,
            http::URL("http://localhost/aois/update")
                .addParam("orderId", orderId)
                .addParam("aoiId", aoiId)
        );
        setAuthHeaderFor(db::Role::Customer, goodRequest);
        goodRequest.body = boost::lexical_cast<std::string>(sprotoAoi);
        auto goodResponse = yacare::performTestRequest(goodRequest);
        EXPECT_EQ(goodResponse.status, 200);
    }

    //listing aois
    {
        http::MockRequest rq(http::GET,
            http::URL("http://localhost/orders/list-aois")
                .addParam("orderId", orderId)
        );
        setAuthHeaderFor(db::Role::Viewer, rq);
        auto resp = yacare::performTestRequest(rq);
        ASSERT_EQ(resp.status, 200);
        auto sprotoAois = boost::lexical_cast<sproto_helpers::sfactory::Aois>(resp.body);
        EXPECT_EQ(sprotoAois.aois().size(), 1u);
        EXPECT_EQ(sprotoAois.aois()[0].id(), aoiId);
        EXPECT_EQ(sprotoAois.aois()[0].name(), SECOND_AOI_NAME);
        EXPECT_EQ(sprotoAois.aois()[0].catalogIds()[0], FIRST_CATALOG_ID);
        EXPECT_EQ(sprotoAois.aois()[0].catalogIds()[1], SECOND_CATALOG_ID);
    }

    /*
     * Searching for aois without mosaic source constraints,
     * expecting result to be equal to /orders/list-aois
     */
    {
        http::MockRequest rq(http::GET,
            http::URL("http://localhost/aois/search")
                .addParam("filter", "order:" + orderId)
        );
        setAuthHeaderFor(db::Role::Viewer, rq);
        auto resp = yacare::performTestRequest(rq);
        ASSERT_EQ(resp.status, 200);
        auto sprotoAois = boost::lexical_cast<sproto_helpers::sfactory::Aois>(resp.body);
        EXPECT_EQ(sprotoAois.aois().size(), 1u);
        EXPECT_EQ(sprotoAois.aois()[0].name(), SECOND_AOI_NAME);
    }

    /*
     * Searching for aois with mosaic sources constratins.
     * Since there is no MosaicSources in database, the result should be empty
     */
    {
        http::MockRequest rq(http::GET,
            http::URL("http://localhost/aois/search")
                .addParam(
                    "filter",
                    "order:" + orderId + "*status:new"
                )
        );
        setAuthHeaderFor(db::Role::Viewer, rq);
        auto resp = yacare::performTestRequest(rq);
        ASSERT_EQ(resp.status, 200);
        auto sprotoAois = boost::lexical_cast<sproto_helpers::sfactory::Aois>(resp.body);
        EXPECT_EQ(sprotoAois.aois().size(), 0u);
    }

    //Creating MosaicSource bound to current AOI
    {
        pqxx::work txn(conn);
        auto aoi = db::AoiGateway(txn).loadById(std::stoi(aoiId));
        makeNewMosaicSource(aoi.orderId(), aoi.id(), delivery->id(), MOSAIC_NAME, txn);
        txn.commit();
    }

    /*
     * Supplying status:parsed to the filter, expecting AOI to be returned
     * since it has MosaicSources matching the filter
     */
    {
        http::MockRequest rq(http::GET,
            http::URL("http://localhost/aois/search")
                .addParam(
                    "filter",
                    "order:" + orderId + "*status:new"
                )
        );
        setAuthHeaderFor(db::Role::Viewer, rq);
        auto resp = yacare::performTestRequest(rq);
        ASSERT_EQ(resp.status, 200);
        auto sprotoAois = boost::lexical_cast<sproto_helpers::sfactory::Aois>(resp.body);
        EXPECT_EQ(sprotoAois.aois().size(), 1u);
        EXPECT_EQ(sprotoAois.aois()[0].id(), aoiId);
        EXPECT_EQ(sprotoAois.aois()[0].name(), SECOND_AOI_NAME);
    }

    //exporting aois to JSON
    {
        http::MockRequest rq(
            http::GET,
            http::URL("http://localhost/orders/export-aois")
                .addParam("orderId", orderId)
        );
        setAuthHeaderFor(db::Role::Viewer, rq);
        auto resp = yacare::performTestRequest(rq);
        EXPECT_EQ(resp.headers[CONTENT_TYPE_HEADER], CONTENT_TYPE_JSON);
        json::Value jsonAois = json::Value::fromString(resp.body);
        EXPECT_EQ(jsonAois[backend::field::TYPE].as<std::string>(), backend::value::FEATURE_COLLECTION);
        EXPECT_EQ(jsonAois[backend::field::FEATURES].size(), 1u);
        const auto& firstFeature = jsonAois[backend::field::FEATURES][0];
        EXPECT_EQ(firstFeature[backend::field::GEOMETRY][backend::field::TYPE].as<std::string>(), "Polygon");
        EXPECT_EQ(firstFeature[backend::field::PROPERTIES][backend::field::NAME].as<std::string>(),
            SECOND_AOI_NAME);
        EXPECT_EQ(
            firstFeature[backend::field::PROPERTIES][backend::field::COLLECTION_START_TIME].as<std::string>(),
            chrono::formatIsoDateTime(collectionStartTime)
        );
        EXPECT_EQ(
            firstFeature[backend::field::PROPERTIES][backend::field::CATALOG_IDS][0].as<std::string>(),
            FIRST_CATALOG_ID
        );
        EXPECT_EQ(
            firstFeature[backend::field::PROPERTIES][backend::field::CATALOG_IDS][1].as<std::string>(),
            SECOND_CATALOG_ID
        );
    }
}

Y_UNIT_TEST(test_aoi_validation)
{
    pqxx::connection conn(postgres().connectionString());

    std::string orderId;
    std::string aoiId;
    //creating new order
    {
        pqxx::work txn(conn);
        db::Order order(ORDER_YEAR, ORDER_TYPE);
        db::OrderGateway(txn).insert(order);
        txn.commit();
        orderId = std::to_string(order.id());
    }

    //creating new Aoi
    {
        sproto_helpers::sfactory::AoiData sprotoAoiData;
        sprotoAoiData.name() = FIRST_AOI_NAME;
        sprotoAoiData.geometry() =
            geolib3::sproto::encode(geolib3::convertMercatorToGeodetic(AOI_BAD_GEOMETRY));
        sprotoAoiData.catalogIds() = {FIRST_CATALOG_ID, SECOND_CATALOG_ID};

        http::MockRequest rq(
            http::POST,
            http::URL("http://localhost/aois/add")
                .addParam("orderId", orderId)
        );
        setAuthHeaderFor(db::Role::Customer, rq);
        rq.body = boost::lexical_cast<std::string>(sprotoAoiData);
        auto resp = yacare::performTestRequest(rq);
        ASSERT_EQ(resp.status, 200);

        auto sprotoAoi = boost::lexical_cast<sproto_helpers::sfactory::Aoi>(resp.body);
        EXPECT_EQ(sprotoAoi.validationErrors().size(), 5u);
        aoiId = sprotoAoi.id();
        pqxx::work txn(conn);
        EXPECT_EQ(db::AoiValidationErrorGateway(txn).count(), 5u);
    }

    {
        http::MockRequest rq(
            http::GET,
            http::URL("http://localhost/orders/list-aois")
                .addParam("orderId", orderId)
        );
        setAuthHeaderFor(db::Role::Customer, rq);
        auto resp = yacare::performTestRequest(rq);
        ASSERT_EQ(resp.status, 200);

        auto sprotoAois = boost::lexical_cast<sproto_helpers::sfactory::Aois>(resp.body);
        ASSERT_EQ(sprotoAois.aois().size(), 1u);
        EXPECT_EQ(sprotoAois.aois().at(0).validationErrors().size(), 5u);
    }

    {
        sproto_helpers::sfactory::AoiData sprotoAoi;
        sprotoAoi.name() = SECOND_AOI_NAME;
        sprotoAoi.geometry() = geolib3::sproto::encode(geolib3::convertMercatorToGeodetic(AOI_BAD_GEOMETRY));

        /*
         * Updating aoi data without rebinding
         */
        http::MockRequest goodRequest(
            http::POST,
            http::URL("http://localhost/aois/update")
                .addParam("orderId", orderId)
                .addParam("aoiId", aoiId)
        );
        setAuthHeaderFor(db::Role::Customer, goodRequest);
        goodRequest.body = boost::lexical_cast<std::string>(sprotoAoi);
        auto resp = yacare::performTestRequest(goodRequest);
        EXPECT_EQ(resp.status, 200);
        auto respSprotoAoi = boost::lexical_cast<sproto_helpers::sfactory::Aoi>(resp.body);
        EXPECT_EQ(respSprotoAoi.validationErrors().size(), 5u);
        pqxx::work txn(conn);
        EXPECT_EQ(db::AoiValidationErrorGateway(txn).count(), 5u);
    }

    {
        sproto_helpers::sfactory::AoiData sprotoAoi;
        sprotoAoi.geometry() = geolib3::sproto::encode(geolib3::convertMercatorToGeodetic(AOI_GEOMETRY));
        sprotoAoi.name() = SECOND_AOI_NAME;

        /*
         * Updating aoi data without rebinding
         */
        http::MockRequest goodRequest(
            http::POST,
            http::URL("http://localhost/aois/update")
                .addParam("orderId", orderId)
                .addParam("aoiId", aoiId)
        );
        setAuthHeaderFor(db::Role::Customer, goodRequest);
        goodRequest.body = boost::lexical_cast<std::string>(sprotoAoi);
        auto resp = yacare::performTestRequest(goodRequest);
        EXPECT_EQ(resp.status, 200);
        auto respSprotoAoi = boost::lexical_cast<sproto_helpers::sfactory::Aoi>(resp.body);
        EXPECT_EQ(respSprotoAoi.validationErrors().size(), 0u);
        pqxx::work txn(conn);
        EXPECT_EQ(db::AoiValidationErrorGateway(txn).count(), 0u);
    }
}

Y_UNIT_TEST(test_aoi_validation_in_import)
{
    pqxx::connection conn(postgres().connectionString());

    std::string orderId;
    std::string aoiId;
    //creating new order
    {
        pqxx::work txn(conn);
        db::Order order(ORDER_YEAR, ORDER_TYPE);
        db::OrderGateway(txn).insert(order);
        txn.commit();
        orderId = std::to_string(order.id());
    }

    auto taskingImportUrl = http::URL("http://localhost/orders/import-aois")
        .addParam("orderId", orderId);
    http::MockRequest importRequest(http::POST, taskingImportUrl);
    importRequest.body = common::readFileToString(SRC_("../data/order_aois_validation_errors.json"));
    setAuthHeaderFor(db::Role::Customer, importRequest);
    auto importResponse = yacare::performTestRequest(importRequest);
    ASSERT_EQ(importResponse.status, 200);

    pqxx::work txn(conn);
    EXPECT_EQ(db::AoiValidationErrorGateway(txn).count(), 5u);  // 4 segments + 1 area
    auto sprotoAois = boost::lexical_cast<sproto_helpers::sfactory::Aois>(importResponse.body);
    ASSERT_EQ(sprotoAois.aois().size(), 1u);
    EXPECT_EQ(sprotoAois.aois().at(0).validationErrors().size(), 5u);
}

Y_UNIT_TEST(test_revalidate)
{
    pqxx::connection conn(postgres().connectionString());

    int64_t orderId, aoiId;

    {
        pqxx::work txn(conn);
        orderId = makeOrder(txn);
        aoiId = makeAoi(txn, orderId, "AOI", AOI_BAD_GEOMETRY);
        txn.commit();
    }

    {
        http::MockRequest rq(
            http::POST,
            http::URL("http://localhost/aois/revalidate")
                .addParam("orderId", std::to_string(orderId))
        );
        setAuthHeaderFor(db::Role::Customer, rq);
        auto resp = yacare::performTestRequest(rq);
        ASSERT_EQ(resp.status, 200);
    }

    {
        pqxx::work txn(conn);
        EXPECT_EQ(db::AoiValidationErrorGateway(txn).count(), 5u);
        db::Aoi aoi = db::AoiGateway(txn).loadById(aoiId);
        aoi.setMercatorGeometry(AOI_GEOMETRY);
        db::AoiGateway(txn).update(aoi);
        txn.commit();
    }

    {
        http::MockRequest rq(
            http::POST,
            http::URL("http://localhost/aois/revalidate")
                .addParam("orderId", std::to_string(orderId))
        );
        setAuthHeaderFor(db::Role::Customer, rq);
        auto resp = yacare::performTestRequest(rq);
        ASSERT_EQ(resp.status, 200);
    }

    pqxx::work txn(conn);
    EXPECT_EQ(db::AoiValidationErrorGateway(txn).count(), 0u);
}

Y_UNIT_TEST(test_importing_aois)
{
    std::string taskingOrderId;
    std::string archiveOrderId;

    //creating two orders - one tasking, one archive
    {
        pqxx::connection conn(postgres().connectionString());
        pqxx::work txn(conn);
        db::Order taskingOrder(ORDER_YEAR, db::OrderType::Tasking);
        db::OrderGateway(txn).insert(taskingOrder);

        db::Order archiveOrder(ORDER_YEAR, db::OrderType::Archive);
        db::OrderGateway(txn).insert(archiveOrder);

        txn.commit();
        taskingOrderId = std::to_string(taskingOrder.id());
        archiveOrderId = std::to_string(archiveOrder.id());
    }

    {
        auto taskingImportUrl = http::URL("http://localhost/orders/import-aois")
            .addParam("orderId", taskingOrderId);
        http::MockRequest importRequest(http::POST, taskingImportUrl);
        importRequest.body = common::readFileToString(SRC_("../data/aois_tasking.json"));
        setAuthHeaderFor(db::Role::Customer, importRequest);
        auto importResponse = yacare::performTestRequest(importRequest);
        ASSERT_EQ(importResponse.status, 200);

        auto taskingSearchAoisUrl = http::URL("http://localhost/orders/list-aois")
            .addParam("orderId", taskingOrderId);
        http::MockRequest searchRequest(http::GET, taskingSearchAoisUrl);
        setAuthHeaderFor(db::Role::Viewer, searchRequest);
        auto searchResponse = yacare::performTestRequest(searchRequest);
        ASSERT_EQ(importResponse.status, 200);
        auto sprotoAois = boost::lexical_cast<sproto_helpers::sfactory::Aois>(searchResponse.body);

        ASSERT_EQ(sprotoAois.aois().size(), 1u);
        const auto& sprotoAoi = sprotoAois.aois()[0];
        EXPECT_EQ(sprotoAoi.name(), "Novokuzneck");
        EXPECT_EQ(*sprotoAoi.maxOffNadirAngle(), 20);
        EXPECT_EQ(
            *sprotoAoi.collectionStartTime(),
            chrono::convertToUnixTime(chrono::parseIsoDateTime("2019-03-01T00:00:00Z"))
        );
        EXPECT_EQ(
            *sprotoAoi.collectionEndTime(),
            chrono::convertToUnixTime(chrono::parseIsoDateTime("2019-10-31T23:59:00Z"))
        );
        EXPECT_TRUE(sprotoAoi.catalogIds().empty());
    }

    {
        auto archiveImportUrl = http::URL("http://localhost/orders/import-aois")
            .addParam("orderId", archiveOrderId);
        http::MockRequest importRequest(http::POST, archiveImportUrl);
        importRequest.body = common::readFileToString(SRC_("../data/aois_archive.json"));
        setAuthHeaderFor(db::Role::Customer, importRequest);
        auto importResponse = yacare::performTestRequest(importRequest);
        ASSERT_EQ(importResponse.status, 200);

        auto archiveSearchAoisUrl = http::URL("http://localhost/orders/list-aois")
            .addParam("orderId", archiveOrderId);
        http::MockRequest searchRequest(http::GET, archiveSearchAoisUrl);
        setAuthHeaderFor(db::Role::Viewer, searchRequest);
        auto searchResponse = yacare::performTestRequest(searchRequest);
        ASSERT_EQ(importResponse.status, 200);
        auto sprotoAois = boost::lexical_cast<sproto_helpers::sfactory::Aois>(searchResponse.body);

        ASSERT_EQ(sprotoAois.aois().size(), 1u);
        const auto& sprotoAoi = sprotoAois.aois()[0];
        EXPECT_EQ(sprotoAoi.name(), "Accra");
        EXPECT_EQ(sprotoAoi.catalogIds().size(), 2u);
        EXPECT_EQ(sprotoAoi.catalogIds()[0], "1030010088105800");
        EXPECT_EQ(sprotoAoi.catalogIds()[1], "9b2920c0-b24c-4073-a1a4-25b372728bcd-inv");
    }
}

Y_UNIT_TEST(test_aois_import_add_update_errors_scenarios)
{
    const std::string AOI_INVALID_NAME = "Москва";
    pqxx::connection conn(postgres().connectionString());

    int64_t orderId, aoiId;

    {
        pqxx::work txn(conn);
        orderId = makeOrder(txn);
        aoiId = makeAoi(txn, orderId, "AOI", AOI_BAD_GEOMETRY);
        txn.commit();
    }

    {
        // try to add valid geojson twice
        auto taskingImportUrl = http::URL("http://localhost/orders/import-aois")
            .addParam("orderId", std::to_string(orderId));
        http::MockRequest importRequest(http::POST, taskingImportUrl);
        importRequest.body = common::readFileToString(SRC_("../data/aois_error_name_duplicate.json"));
        setAuthHeaderFor(db::Role::Customer, importRequest);
        auto importResponse = yacare::performTestRequest(importRequest);
        ASSERT_EQ(importResponse.status, 422);
        auto invalidAoiDatum =
            boost::lexical_cast<sproto_helpers::sfactory::InvalidAoiDatum>(importResponse.body);
        ASSERT_TRUE(invalidAoiDatum.invalidName().defined());
        EXPECT_EQ(invalidAoiDatum.invalidName().get().reason().get(),
            sproto_helpers::sfactory::InvalidString::DUPLICATE);
        EXPECT_EQ(invalidAoiDatum.invalidName().get().value().text(), "Novokuzneck");
    }

    {
        // try to add valid geojson twice
        auto taskingImportUrl = http::URL("http://localhost/orders/import-aois")
            .addParam("orderId", std::to_string(orderId));
        http::MockRequest importRequest(http::POST, taskingImportUrl);
        importRequest.body = common::readFileToString(SRC_("../data/aois_tasking.json"));
        setAuthHeaderFor(db::Role::Customer, importRequest);
        auto importResponse = yacare::performTestRequest(importRequest);
        ASSERT_EQ(importResponse.status, 200);

        importResponse = yacare::performTestRequest(importRequest);
        ASSERT_EQ(importResponse.status, 422);
        auto invalidAoiDatum =
            boost::lexical_cast<sproto_helpers::sfactory::InvalidAoiDatum>(importResponse.body);
        ASSERT_TRUE(invalidAoiDatum.invalidName().defined());
        EXPECT_EQ(invalidAoiDatum.invalidName().get().reason().get(),
            sproto_helpers::sfactory::InvalidString::DUPLICATE);
        EXPECT_EQ(invalidAoiDatum.invalidName().get().value().text(), "Novokuzneck");
    }

    {
        auto taskingImportUrl = http::URL("http://localhost/orders/import-aois")
            .addParam("orderId", std::to_string(orderId));
        http::MockRequest importRequest(http::POST, taskingImportUrl);
        importRequest.body = common::readFileToString(SRC_("../data/aois_error_name_forbidden_symbol.json"));
        setAuthHeaderFor(db::Role::Customer, importRequest);
        auto importResponse = yacare::performTestRequest(importRequest);
        ASSERT_EQ(importResponse.status, 422);
        auto invalidAoiDatum =
            boost::lexical_cast<sproto_helpers::sfactory::InvalidAoiDatum>(importResponse.body);
        ASSERT_TRUE(invalidAoiDatum.invalidName().defined());
        EXPECT_EQ(invalidAoiDatum.invalidName().get().reason().get(),
            sproto_helpers::sfactory::InvalidString::CONTAINS_FORBIDDEN_SYMBOLS);
        EXPECT_EQ(invalidAoiDatum.invalidName().get().value().text(), "Москва");
        ASSERT_EQ(invalidAoiDatum.invalidName().get().value().span().size(), 1u);
        EXPECT_EQ(invalidAoiDatum.invalidName().get().value().span()[0].begin(), 0);
        EXPECT_EQ(invalidAoiDatum.invalidName().get().value().span()[0].end(), 5);
    }

    {
        auto taskingImportUrl = http::URL("http://localhost/orders/import-aois")
            .addParam("orderId", std::to_string(orderId));
        http::MockRequest importRequest(http::POST, taskingImportUrl);
        importRequest.body = common::readFileToString(SRC_("../data/aois_error_name_empty.json"));
        setAuthHeaderFor(db::Role::Customer, importRequest);
        auto importResponse = yacare::performTestRequest(importRequest);
        ASSERT_EQ(importResponse.status, 422);
        auto invalidAoiDatum =
            boost::lexical_cast<sproto_helpers::sfactory::InvalidAoiDatum>(importResponse.body);
        ASSERT_TRUE(invalidAoiDatum.invalidName().defined());
        EXPECT_EQ(invalidAoiDatum.invalidName().get().reason().get(),
            sproto_helpers::sfactory::InvalidString::EMPTY);
        EXPECT_EQ(invalidAoiDatum.invalidName().get().value().text(), "");
    }

    {
        auto taskingImportUrl = http::URL("http://localhost/orders/import-aois")
            .addParam("orderId", std::to_string(orderId));
        http::MockRequest importRequest(http::POST, taskingImportUrl);
        importRequest.body = common::readFileToString(SRC_("../data/aois_error_geometry_interior_ring.json"));
        setAuthHeaderFor(db::Role::Customer, importRequest);
        auto importResponse = yacare::performTestRequest(importRequest);
        ASSERT_EQ(importResponse.status, 422);
        auto invalidAoiDatum =
            boost::lexical_cast<sproto_helpers::sfactory::InvalidAoiDatum>(importResponse.body);
        ASSERT_TRUE(invalidAoiDatum.invalidGeometry().defined());
        EXPECT_TRUE(invalidAoiDatum.invalidGeometry().get().interiorRingsAreNotAllowed().defined());
    }

    {
        auto taskingImportUrl = http::URL("http://localhost/orders/import-aois")
            .addParam("orderId", std::to_string(orderId));
        http::MockRequest importRequest(http::POST, taskingImportUrl);
        importRequest.body =
            common::readFileToString(SRC_("../data/aois_error_geometry_self_intersection.json"));
        setAuthHeaderFor(db::Role::Customer, importRequest);
        auto importResponse = yacare::performTestRequest(importRequest);
        ASSERT_EQ(importResponse.status, 422);
        auto invalidAoiDatum =
            boost::lexical_cast<sproto_helpers::sfactory::InvalidAoiDatum>(importResponse.body);
        ASSERT_TRUE(invalidAoiDatum.invalidGeometry().defined());
        EXPECT_TRUE(invalidAoiDatum.invalidGeometry().get().selfIntersection().defined());
        EXPECT_FLOAT_EQ(invalidAoiDatum.invalidGeometry().get().selfIntersection().get().point().lon(), 5.);
        EXPECT_FLOAT_EQ(invalidAoiDatum.invalidGeometry().get().selfIntersection().get().point().lat(), 5.);
    }

    {
        sproto_helpers::sfactory::AoiData sprotoAoiData;
        sprotoAoiData.name() = AOI_INVALID_NAME;
        sprotoAoiData.geometry() = geolib3::sproto::encode(geolib3::convertMercatorToGeodetic(AOI_GEOMETRY));

        http::MockRequest rq(
            http::POST,
            http::URL("http://localhost/aois/add")
                .addParam("orderId", std::to_string(orderId))
        );
        setAuthHeaderFor(db::Role::Customer, rq);
        rq.body = boost::lexical_cast<std::string>(sprotoAoiData);
        auto resp = yacare::performTestRequest(rq);
        ASSERT_EQ(resp.status, 422);
    }

    {
        sproto_helpers::sfactory::AoiData sprotoAoi;
        sprotoAoi.geometry() = geolib3::sproto::encode(geolib3::convertMercatorToGeodetic(AOI_GEOMETRY));
        sprotoAoi.name() = AOI_INVALID_NAME;

        /*
         * Updating aoi data without rebinding
         */
        http::MockRequest goodRequest(
            http::POST,
            http::URL("http://localhost/aois/update")
                .addParam("orderId", std::to_string(orderId))
                .addParam("aoiId", std::to_string(aoiId))
        );
        setAuthHeaderFor(db::Role::Customer, goodRequest);
        goodRequest.body = boost::lexical_cast<std::string>(sprotoAoi);
        auto resp = yacare::performTestRequest(goodRequest);
        EXPECT_EQ(resp.status, 422);
    }
}

Y_UNIT_TEST(test_delete)
{
    pqxx::connection conn(postgres().connectionString());

    int64_t orderId, aoiId1, aoiId2;

    {
        pqxx::work txn(conn);
        orderId = makeOrder(txn);
        aoiId1 = makeAoi(txn, orderId, "AOI1", AOI_BAD_GEOMETRY);
        aoiId2 = makeAoi(txn, orderId, "AOI2", AOI_GEOMETRY);
        txn.commit();
    }

    {
        http::MockRequest rq(http::DELETE, "http://localhost/aois/delete?aoiId=" + std::to_string(aoiId1));
        setAuthHeaderFor(db::Role::Customer, rq);
        auto resp = yacare::performTestRequest(rq);
        ASSERT_EQ(resp.status, 200);
        pqxx::work txn(conn);
        EXPECT_EQ(db::AoiGateway(txn).count(), 1u);
    }

    {
        http::MockRequest rq(http::DELETE, "http://localhost/aois/delete?orderId=" + std::to_string(orderId));
        setAuthHeaderFor(db::Role::Customer, rq);
        auto resp = yacare::performTestRequest(rq);
        ASSERT_EQ(resp.status, 200);
        pqxx::work txn(conn);
        EXPECT_EQ(db::AoiGateway(txn).count(), 0u);
    }

}

} //Y_UNIT_TEST_SUITE
} //namespace maps::factory::sputnica::tests
