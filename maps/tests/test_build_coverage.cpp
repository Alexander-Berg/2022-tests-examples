#include <maps/factory/libs/processing/build_coverage.h>

#include <maps/factory/libs/db/delivery_gateway.h>
#include <maps/factory/libs/db/delivery.h>
#include <maps/factory/libs/db/mosaic_gateway.h>
#include <maps/factory/libs/db/mosaic.h>
#include <maps/factory/libs/db/mosaic_source_gateway.h>
#include <maps/factory/libs/db/mosaic_source.h>
#include <maps/factory/libs/db/release_gateway.h>
#include <maps/factory/libs/db/release.h>
#include <maps/factory/libs/db/source_gateway.h>
#include <maps/factory/libs/db/source.h>
#include <maps/factory/libs/db/tests/test_data.h>
#include <maps/factory/libs/storage/storage.h>
#include <maps/factory/libs/unittest/fixture.h>
#include <maps/factory/libs/processing/tests/test_context.h>

#include <yandex/maps/coverage5/coverage.h>
#include <maps/libs/geolib/include/test_tools/comparison.h>
#include <maps/libs/geolib/include/conversion.h>
#include <maps/libs/log8/include/log8.h>
#include <maps/libs/xml/include/xml.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <pqxx/pqxx>

#include <filesystem>

namespace maps::factory::processing::tests {

namespace {

using namespace testing;
namespace cvg = maps::coverage5;

const char* g_cvrNamespace = "http://maps.yandex.ru/coverage/2.x";

const std::string OUTPUT_FILEPATH = "./tmp/build_coverage/";
const std::string OUTPUT_FILENAME = "./tmp/build_coverage/sat.mms.1";

class Fixture : public unittest::Fixture {
public:
    Fixture()
        : source(makeSource())
        , vendor1("vendor1")
        , vendor2("vendor2")
        , delivery1(source.id(), "2018-12-25", "Delivery1", "/path1")
        , delivery2(source.id(), "2016-10-25", "Delivery2", "/path2")
    {
        std::filesystem::create_directory(OUTPUT_FILEPATH);

        pqxx::connection conn(postgres().connectionString());
        pqxx::work txn(conn);

        delivery1.setCopyrights({vendor1});
        delivery2.setCopyrights({vendor2});
        db::DeliveryGateway(txn).insert(delivery1);
        db::DeliveryGateway(txn).insert(delivery2);

        txn.commit();
    }

    db::Source makeSource()
    {
        pqxx::connection conn(postgres().connectionString());
        pqxx::work txn(conn);
        db::Source source("test data", db::SourceType::Local, "/dev/null");
        db::SourceGateway(txn).insert(source);
        txn.commit();
        return source;
    }

    db::Source source;
    std::string vendor1;
    std::string vendor2;
    db::Delivery delivery1;
    db::Delivery delivery2;
};

std::optional<std::string> loadCollectionDateFromMetadata(const char* metadata)
{
    auto xmlDoc = xml3::Doc::fromString(metadata);
    xml3::Node root = xmlDoc.root();
    root.addNamespace("cvr", g_cvrNamespace);
    auto dateNode = root.node("/cvr:MetaData/collection_date");
    if (!dateNode.isNull()) {
        return dateNode.value<std::string>();
    }
    return std::nullopt;
}

std::optional<std::string> loadDeliveryDateFromMetadata(const char* metadata)
{
    auto xmlDoc = xml3::Doc::fromString(metadata);
    xml3::Node root = xmlDoc.root();
    root.addNamespace("cvr", g_cvrNamespace);
    auto dateNode = root.node("/cvr:MetaData/delivery_date");
    if (!dateNode.isNull()) {
        return dateNode.value<std::string>();
    }
    return std::nullopt;
}

struct RegionData {
    std::vector<std::string> authorNames;
    std::optional<std::string> collectionDate;
    std::string deliveryDate;
    cvg::Zoom zoomMin;
    cvg::Zoom zoomMax;
};

std::ostream& operator<<(std::ostream& os, const RegionData& r)
{
    return os << std::to_string(r.zoomMin) + "-" + std::to_string(r.zoomMax) << " "
        << (r.collectionDate.has_value() ? r.collectionDate.value() : "null") << " "
        << r.deliveryDate;
}

MATCHER(AuthorNameEq, "")
{
    const auto& cvgAuthor = std::get<0>(arg);
    const auto& name = std::get<1>(arg);

    return ExplainMatchResult(Eq(name), cvgAuthor.names.at(0), result_listener);
}


/// Checks for equality cvg::Region and RegionData
MATCHER(RegionEq, "")
{
    const auto& cvgRegion = std::get<0>(arg);
    const auto& regionData = std::get<1>(arg);

    return
        ExplainMatchResult(UnorderedPointwise(AuthorNameEq(), regionData.authorNames), cvgRegion.authors(),
            result_listener) &&
        ExplainMatchResult(Ne(nullptr), cvgRegion.metaData(), result_listener) &&
        ExplainMatchResult(Eq(regionData.collectionDate),
            loadCollectionDateFromMetadata(cvgRegion.metaData()), result_listener) &&
        ExplainMatchResult(Eq(regionData.deliveryDate), loadDeliveryDateFromMetadata(cvgRegion.metaData()),
            result_listener) &&
        ExplainMatchResult(Eq(regionData.zoomMin), *cvgRegion.zoomMin(), result_listener) &&
        ExplainMatchResult(Eq(regionData.zoomMax), *cvgRegion.zoomMax(), result_listener);
}

} // namespace

Y_UNIT_TEST_SUITE(build_coverage_tasks_should) {

Y_UNIT_TEST(build_simple_coverage)
{

    Fixture fixture;
    TestContext ctx(fixture.postgres().connectionString());
    pqxx::connection conn(fixture.postgres().connectionString());
    pqxx::work txn(conn);

    const geolib3::MultiPolygon2 TEST_MOSAIC_GEOMETRY{{
        geolib3::Polygon2{geolib3::PointsVector{
            {37.5381, 55.8096},
            {37.7369, 55.8119},
            {37.7408, 55.7000},
            {37.5426, 55.6979}
        }}
    }};

    db::MosaicSource ms("name");
    ms.setDeliveryId(fixture.delivery1.id());
    ms.setMercatorGeom(TEST_MOSAIC_GEOMETRY);
    ms.setMinZoom(10);
    ms.setMaxZoom(18);
    ms.setMetadata(json::Value{json::repr::ObjectRepr{
        {"datetime", json::Value("2016-07-22 02:26:08.1Z")}
    }});

    db::MosaicSourceGateway(txn).insert(ms);

    const int ISSUE_1 = 1;
    db::Release release("test_release");
    release.setIssue(ISSUE_1);
    release.setStatus(db::ReleaseStatus::Testing);
    db::ReleaseGateway(txn).insert(release);

    db::Mosaic mosaic(
        ms.id(),
        /* zOrder = */ 31337,
        /* zoomMin = */ 10,
        /* zoomMax = */ 18,
        geolib3::convertGeodeticToMercator(TEST_MOSAIC_GEOMETRY)
    );

    mosaic.setReleaseId(release.id());
    db::MosaicGateway(txn).insert(mosaic);

    txn.commit();

    const std::string
    VERSION = "0.0.1";

    const BuildCoverage worker;
    worker(ReleaseIssue(ISSUE_1),
        ReleaseVersion(VERSION),
        LocalDirectoryPath(OUTPUT_FILEPATH),
        ctx);

    ASSERT_TRUE(std::filesystem::exists(OUTPUT_FILENAME));

    coverage5::Coverage cov(OUTPUT_FILEPATH);

    ASSERT_TRUE(cov.hasLayer("sat"));
    const auto& layer = cov["sat"];

    EXPECT_EQ(layer.regions(geolib3::Point2(37.5897, 55.7669), 9).size(), 0u);
    EXPECT_EQ(layer.regions(geolib3::Point2(-1.0, -1.0), 11).size(), 0u);

    EXPECT_THAT(layer.regions(geolib3::Point2(37.5897, 55.7669), 11),
        Pointwise(RegionEq(),
            {RegionData{{fixture.vendor1}, "2016-07-22", fixture.delivery1.date(), 10, 18}}));
}

Y_UNIT_TEST(overlaped_mosaic_should_not_export)
{

    Fixture fixture;
    TestContext ctx(fixture.postgres().connectionString());
    pqxx::connection conn(fixture.postgres().connectionString());
    pqxx::work txn(conn);

    // GEOMETRY1 is overlaped by union of GEOMETRY2 and GEOMETRY3
    const geolib3::MultiPolygon2 GEOMETRY1{{geolib3::Polygon2{
        geolib3::PointsVector{{0, 0}, {10, 0}, {10, 10}, {0, 10}}}}};
    const geolib3::MultiPolygon2 GEOMETRY2{{geolib3::Polygon2{
        geolib3::PointsVector{{-1, -1}, {6, -1}, {6, 11}, {-1, 11}}}}};
    const geolib3::MultiPolygon2 GEOMETRY3{{geolib3::Polygon2{
        geolib3::PointsVector{{5, -1}, {11, -1}, {11, 11}, {5, 11}}}}};

    db::MosaicSource ms1("name1");
    ms1.setDeliveryId(fixture.delivery1.id());
    ms1.setMercatorGeom(GEOMETRY1);
    ms1.setMetadata(json::Value{json::repr::ObjectRepr{
        {"datetime", json::Value("2016-07-22 02:26:08.1Z")}
    }});

    db::MosaicSource ms2("name2");
    ms2.setDeliveryId(fixture.delivery2.id());
    ms2.setMercatorGeom(GEOMETRY2);
    ms2.setMetadata(json::Value{json::repr::ObjectRepr{
        {"datetime", json::Value("2017-07-22 02:26:08.1Z")}
    }});

    db::MosaicSource ms3("name3");
    ms3.setDeliveryId(fixture.delivery2.id());
    ms3.setMercatorGeom(GEOMETRY3);
    ms3.setMetadata(json::Value{json::repr::ObjectRepr{
        {"datetime", json::Value("2018-07-22 02:26:08.1Z")}
    }});

    db::MosaicSourceGateway msg(txn);
    msg.insert(ms1);
    msg.insert(ms2);
    msg.insert(ms3);

    auto release1 = db::Release("test_release1")
        .setIssue(1)
        .setStatus(db::ReleaseStatus::Testing);
    auto release2 = db::Release("test_release2")
        .setIssue(2)
        .setStatus(db::ReleaseStatus::Testing);
    auto release3 = db::Release("test_release3")
        .setIssue(3)
        .setStatus(db::ReleaseStatus::Testing);
    db::ReleaseGateway rg(txn);
    rg.insert(release1);
    rg.insert(release2);
    rg.insert(release3);

    db::Mosaic mosaic1(
        ms1.id(),
        /* zOrder = */ 1,
        /* zoomMin = */ 0,
        /* zoomMax = */ 18,
        GEOMETRY1);
    mosaic1.setReleaseId(release1.id());

    db::Mosaic mosaic2(
        ms2.id(),
        /* zOrder = */ 1,
        /* zoomMin = */ 10,
        /* zoomMax = */ 18,
        GEOMETRY2);
    mosaic2.setReleaseId(release2.id());

    db::Mosaic mosaic3(
        ms3.id(),
        /* zOrder = */ 1,
        /* zoomMin = */ 10,
        /* zoomMax = */ 18,
        GEOMETRY3);
    mosaic3.setReleaseId(release3.id());

    db::MosaicGateway mg(txn);
    mg.insert(mosaic1);
    mg.insert(mosaic2);
    mg.insert(mosaic3);

    txn.commit();

    const std::string
    VERSION = "0.0.1";
    const BuildCoverage worker;
    worker(ReleaseIssue(release3.issue().value()),
        ReleaseVersion(VERSION),
        LocalDirectoryPath(OUTPUT_FILEPATH),
        ctx);

    ASSERT_TRUE(std::filesystem::exists(OUTPUT_FILENAME));

    coverage5::Coverage cov(OUTPUT_FILEPATH);

    ASSERT_TRUE(cov.hasLayer("sat"));
    const auto& layer = cov["sat"];

    EXPECT_THAT(layer.regions(geolib3::convertMercatorToGeodetic(geolib3::Point2(1, 1)), 11),
        Pointwise(RegionEq(),
            {RegionData{{fixture.vendor2}, "2017-07-22", fixture.delivery2.date(), 10, 18}}));

    EXPECT_THAT(layer.regions(geolib3::convertMercatorToGeodetic(geolib3::Point2(9, 1)), 11),
        Pointwise(RegionEq(),
            {RegionData{{fixture.vendor2}, "2018-07-22", fixture.delivery2.date(), 10, 18}}));

    EXPECT_THAT(layer.regions(geolib3::convertMercatorToGeodetic(geolib3::Point2(5.5, 1)), 11),
        UnorderedPointwise(RegionEq(),
            {
                RegionData{{fixture.vendor2}, "2018-07-22", fixture.delivery2.date(), 10, 18},
                RegionData{{fixture.vendor2}, "2017-07-22", fixture.delivery2.date(), 10, 18}
            }
        )
    );

    EXPECT_THAT(layer.regions(geolib3::convertMercatorToGeodetic(geolib3::Point2(1, 1)), 8),
        Pointwise(RegionEq(), {RegionData{{fixture.vendor1}, "2016-07-22", fixture.delivery1.date(), 0, 9}}));
}

Y_UNIT_TEST(overlaped_world_mosaics)
{

    Fixture fixture;
    TestContext ctx(fixture.postgres().connectionString());
    pqxx::connection conn(fixture.postgres().connectionString());
    pqxx::work txn(conn);

    const geolib3::MultiPolygon2 GEOMETRY1{{geolib3::Polygon2{
        geolib3::PointsVector{
            {-20037508.342789, -20037508.342789},
            {-20037508.342789, 20037508.342789},
            {20037508.342789, 20037508.342789},
            {20037508.342789, -20037508.342789},
            {-20037508.342789, -20037508.342789}}}}};

    const geolib3::MultiPolygon2 GEOMETRY2{{geolib3::Polygon2{
        geolib3::PointsVector{
            {2504695.86177097, 10018747.1548312},
            {3757030.40840329, 10018746.7292488},
            {3757028.27499136, 8766416.8248898},
            {2504700.68083005, 8766416.86005028},
            {2504695.86177097, 10018747.1548312}}}}};

    db::MosaicSource ms1("name1");
    ms1.setDeliveryId(fixture.delivery1.id());
    ms1.setMercatorGeom(GEOMETRY1);
    ms1.setMetadata(json::Value{json::repr::ObjectRepr{
        {"datetime", json::Value("2016-07-22 02:26:08.1Z")}
    }});

    db::MosaicSource ms2("name2");
    ms2.setDeliveryId(fixture.delivery2.id());
    ms2.setMercatorGeom(GEOMETRY2);
    ms2.setMetadata(json::Value{json::repr::ObjectRepr{
        {"datetime", json::Value("2017-07-22 02:26:08.1Z")}
    }});

    db::MosaicSource ms3("name3");
    ms3.setDeliveryId(fixture.delivery2.id());
    ms3.setMercatorGeom(GEOMETRY1);
    ms3.setMetadata(json::Value{json::repr::ObjectRepr{
        {"datetime", json::Value("2018-07-22 02:26:08.1Z")}
    }});

    db::MosaicSourceGateway msg(txn);
    msg.insert(ms1);
    msg.insert(ms2);
    msg.insert(ms3);

    auto release1 = db::Release("test_release1").setIssue(1)
                                                .setStatus(db::ReleaseStatus::Testing);
    auto release2 = db::Release("test_release2").setIssue(2)
                                                .setStatus(db::ReleaseStatus::Testing);
    auto release3 = db::Release("test_release3").setIssue(3)
                                                .setStatus(db::ReleaseStatus::Testing);
    db::ReleaseGateway rg(txn);
    rg.insert(release1);
    rg.insert(release2);
    rg.insert(release3);

    db::Mosaic mosaic1(
        ms1.id(),
        /* zOrder = */ 1,
        /* zoomMin = */ 0,
        /* zoomMax = */ 12,
        GEOMETRY1);
    mosaic1.setReleaseId(release1.id());

    db::Mosaic mosaic2(
        ms2.id(),
        /* zOrder = */ 1,
        /* zoomMin = */ 0,
        /* zoomMax = */ 7,
        GEOMETRY2);
    mosaic2.setReleaseId(release2.id());

    db::Mosaic mosaic3(
        ms3.id(),
        /* zOrder = */ 1,
        /* zoomMin = */ 10,
        /* zoomMax = */ 17,
        GEOMETRY1);
    mosaic3.setReleaseId(release3.id());

    db::MosaicGateway mg(txn);
    mg.insert(mosaic1);
    mg.insert(mosaic2);
    mg.insert(mosaic3);

    txn.commit();

    const std::string
    VERSION = "0.0.1";
    const BuildCoverage worker;
    worker(ReleaseIssue(release3.issue().value()),
        ReleaseVersion(VERSION),
        LocalDirectoryPath(OUTPUT_FILEPATH),
        ctx);

    ASSERT_TRUE(std::filesystem::exists(OUTPUT_FILENAME));

    coverage5::Coverage cov(OUTPUT_FILEPATH);

    ASSERT_TRUE(cov.hasLayer("sat"));
    const auto& layer = cov["sat"];

    EXPECT_THAT(layer.regions(geolib3::Point2(28.12, 64.32), 3),
        UnorderedPointwise(RegionEq(), {
            RegionData{{fixture.vendor2}, "2017-07-22", fixture.delivery2.date(), 0, 7},
            RegionData{{fixture.vendor1}, "2016-07-22", fixture.delivery1.date(), 0, 9}
        })
    );

    EXPECT_THAT(layer.regions(geolib3::Point2(37.5897, 55.7669), 3),
        Pointwise(RegionEq(), {RegionData{{fixture.vendor1}, "2016-07-22", fixture.delivery1.date(), 0, 9}}));

    EXPECT_THAT(layer.regions(geolib3::Point2(37.5897, 55.7669), 13),
        Pointwise(RegionEq(),
            {RegionData{{fixture.vendor2}, "2018-07-22", fixture.delivery2.date(), 10, 17}}));
}

} // suite
} // namespace maps::factory::processing::tests
