#include <maps/factory/services/sputnica_back/tests/api_backend_tests/fixture.h>
#include <maps/factory/services/sputnica_back/tests/common/test_data.h>

#include <maps/factory/services/sputnica_back/lib/report_gen.h>
#include <maps/factory/libs/unittest/tests_common.h>
#include <maps/factory/libs/unittest/fixture.h>

#include <maps/factory/libs/db/aoi_gateway.h>
#include <maps/factory/libs/db/acceptance_report_gateway.h>
#include <maps/factory/libs/db/acceptance_report_mosaic_source_gateway.h>
#include <maps/factory/libs/db/mosaic_source_gateway.h>
#include <maps/factory/libs/db/mosaic_source_pipeline.h>
#include <maps/factory/libs/db/order_gateway.h>
#include <maps/libs/geolib/include/test_tools/comparison.h>
#include <maps/libs/geolib/include/conversion.h>
#include <maps/libs/log8/include/log8.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <iomanip>
#include <numeric>

namespace maps::factory::sputnica::tests {
using namespace testing;

namespace {
i64 TEST_USER = 1;
double AREA_EPS = 1000.;

const geolib3::Polygon2 AOI_1_GEO_GEOM(
    geolib3::PointsVector{
        {37.297210693359375, 55.93612557333063},
        {37.320556640625, 55.55737918433739},
        {37.934417724609375, 55.58144971869657},
        {37.841033935546875, 55.919968935096776}
    });

constexpr double AOI1_AREA = 1453036968;

const geolib3::Polygon2 AOI_2_GEO_GEOM(
    geolib3::PointsVector{
        {37.40020751953125, 56.11799819390808},
        {37.430419921875, 55.88917574736247},
        {37.94952392578125, 55.87222907165985},
        {37.9412841796875, 56.15166933290848}
    });

const geolib3::MultiPolygon2 MOSAIC_1_1_GEO_GEOM({geolib3::Polygon2(
    geolib3::PointsVector{
        {37.58148193359375, 55.983018567950026},
        {37.55676269531249, 55.33851784425634},
        {38.07037353515625, 55.32445685891768},
        {37.8863525390625, 55.95535088453652},
        {37.58148193359375, 55.983018567950026}})});

const geolib3::MultiPolygon2 MOSAIC_1_2_GEO_GEOM({geolib3::Polygon2(
    geolib3::PointsVector{
        {37.166748046875, 55.989164255467934},
        {37.15850830078125, 55.77039358162004},
        {37.6446533203125, 55.7642131648377},
        {37.6446533203125, 56.00298848125946}})});

const geolib3::MultiPolygon2 MOSAIC_1_3_GEO_GEOM({geolib3::Polygon2(
    geolib3::PointsVector{
        {37.0843505859375, 55.81208577289997},
        {37.02392578125, 55.48663527739909},
        {37.69134521484375, 55.28067965709029},
        {37.70233154296875, 55.81671548420953}})});

i64 makeMosaic(
    pqxx::transaction_base& txn,
    std::string name,
    db::MosaicSourceStatus status,
    geolib3::MultiPolygon2 mercatorGeom,
    i64 orderId, i64 aoiId, i64 deliveryId
)
{
    db::MosaicSource ms(std::move(name));
    ms.setMercatorGeom(std::move(mercatorGeom));
    ms.setMinMaxZoom(10, 19);

    constexpr auto INITIAL_STATUS = db::MosaicSourceStatus::New;
    ms.setStatus(INITIAL_STATUS);
    ms.setOrderId(orderId);
    ms.setAoiId(aoiId);
    ms.setDeliveryId(deliveryId);
    db::MosaicSourceGateway(txn).insert(ms);

    if (status != INITIAL_STATUS) {
        db::MosaicSourcePipeline(txn)
            .transition(ms, status, db::UserRole(TEST_USER, db::Role::Customer));
    }
    return ms.id();
}

i64 generateReport(pqxx::transaction_base& txn, i64 orderId)
{
    db::AcceptanceReport report(orderId, TEST_USER);
    db::AcceptanceReportGateway(txn).insert(report);
    backend::generateAcceptanceReport(txn, report);
    return report.id();
}

struct MosaicReportData {
    i64 mosaicId;
    double additionalArea;
    std::optional<geolib3::MultiPolygon2> mercatorGeom;
};

MATCHER(ReportEq, "")
{
    return
        ExplainMatchResult(Eq(std::get<0>(arg).mosaicSourceId()), std::get<1>(arg).mosaicId, result_listener)
        &&
        ExplainMatchResult(DoubleNear(std::get<0>(arg).additionalArea(), AREA_EPS),
            std::get<1>(arg).additionalArea, result_listener) &&
        (std::get<1>(arg).mercatorGeom ? geolib3::test_tools::approximateEqual(
            std::get<0>(arg).additionalMercatorGeom(),
            std::get<1>(arg).mercatorGeom.value(),
            0.0)
                                       : true);
}

std::vector<db::AcceptanceReportMosaicSource>
loadReportData(pqxx::transaction_base& txn, i64 reportId)
{
    return db::AcceptanceReportMosaicSourceGateway(txn)
        .load(db::table::AcceptanceReportMosaicSource::acceptanceReportId == reportId,
            sql_chemistry::orderBy(db::table::AcceptanceReportMosaicSource::mosaicSourceId));
}

void setStatusToReady(pqxx::transaction_base& txn, i64 mosaicId)
{
    db::MosaicSourceGateway gtw(txn);
    auto mosaic = gtw.loadById(mosaicId);
    db::MosaicSourcePipeline(txn)
        .transition(mosaic, db::MosaicSourceStatus::Ready, db::UserRole(TEST_USER, db::Role::Customer));
    gtw.update(mosaic);
}

double calcTotalAcceptedArea(pqxx::transaction_base& txn, i64 orderId)
{
    auto mosaicReports = db::AcceptanceReportMosaicSourceGateway(txn)
        .load(db::table::AcceptanceReportMosaicSource::acceptanceReportId ==
              db::table::AcceptanceReport::id &&
              db::table::AcceptanceReport::orderId == orderId);
    double res{};
    for (const auto& report: mosaicReports) {
        res += report.additionalArea();
    }
    return res;
}

}

Y_UNIT_TEST_SUITE_F(report_gen_should, Fixture) {

Y_UNIT_TEST(basic_test)
{
    pqxx::connection conn(postgres().connectionString());
    pqxx::work txn(conn);
    //creating new order, and aoi bound to it
    auto orderId = makeOrder(txn);
    auto aoiId = makeAoi(txn, orderId, FIRST_AOI_NAME, geolib3::convertGeodeticToMercator(AOI_1_GEO_GEOM));
    auto delivery = makeTestDelivery(txn);

    auto mosaicSourceId1 = makeMosaic(
        txn, "MOSAIC1", db::MosaicSourceStatus::Ready,
        geolib3::convertGeodeticToMercator(MOSAIC_1_1_GEO_GEOM), orderId, aoiId, delivery.id());

    auto reportId = generateReport(txn, orderId);
    auto loadedReportData = loadReportData(txn, reportId);
    EXPECT_THAT(loadedReportData, Pointwise(ReportEq(),
        std::vector<MosaicReportData>{{mosaicSourceId1, 771913517.5, std::nullopt}}));

    // repetitive report should be empty
    reportId = generateReport(txn, orderId);
    EXPECT_THAT(loadReportData(txn, reportId), Pointwise(ReportEq(),
        std::vector<MosaicReportData>{}));

    // add second mosaic
    auto mosaicSourceId2 = makeMosaic(
        txn, "MOSAIC2", db::MosaicSourceStatus::New,
        geolib3::convertGeodeticToMercator(MOSAIC_1_2_GEO_GEOM), orderId, aoiId, delivery.id());
    reportId = generateReport(txn, orderId);
    EXPECT_THAT(loadReportData(txn, reportId), Pointwise(ReportEq(),
        std::vector<MosaicReportData>{}));

    setStatusToReady(txn, mosaicSourceId2);
    reportId = generateReport(txn, orderId);
    EXPECT_THAT(loadReportData(txn, reportId), Pointwise(ReportEq(),
        std::vector<MosaicReportData>{{mosaicSourceId2, 315170584.5, std::nullopt}}));

    // add mosaic
    auto mosaicSourceId3 = makeMosaic(
        txn, "MOSAIC3", db::MosaicSourceStatus::Ready,
        geolib3::convertGeodeticToMercator(MOSAIC_1_3_GEO_GEOM), orderId, aoiId, delivery.id());
    reportId = generateReport(txn, orderId);
    EXPECT_THAT(loadReportData(txn, reportId), Pointwise(ReportEq(),
        std::vector<MosaicReportData>{{mosaicSourceId3, 365952865.8, std::nullopt}}));

    // nothing should be uncovered
    // add mosaic
    auto mosaicSourceId4 = makeMosaic(
        txn, "MOSAIC4", db::MosaicSourceStatus::Ready,
        geolib3::convertGeodeticToMercator(geolib3::MultiPolygon2({AOI_1_GEO_GEOM})), orderId, aoiId,
        delivery.id());
    reportId = generateReport(txn, orderId);
    EXPECT_THAT(loadReportData(txn, reportId), Pointwise(ReportEq(),
        std::vector<MosaicReportData>{{mosaicSourceId4, 0., std::nullopt}}));

    EXPECT_THAT(calcTotalAcceptedArea(txn, orderId), DoubleNear(AOI1_AREA, AREA_EPS));
}

Y_UNIT_TEST(full_accurate_coverage)
{
    pqxx::connection conn(postgres().connectionString());
    pqxx::work txn(conn);
    //creating new order, and aoi bound to it
    auto orderId = makeOrder(txn);
    auto aoiId = makeAoi(txn, orderId, FIRST_AOI_NAME, geolib3::convertGeodeticToMercator(AOI_1_GEO_GEOM));
    auto delivery = makeTestDelivery(txn);

    auto mosaicSourceId1 = makeMosaic(
        txn, "MOSAIC1", db::MosaicSourceStatus::Ready,
        geolib3::convertGeodeticToMercator(geolib3::MultiPolygon2({AOI_1_GEO_GEOM})), orderId, aoiId,
        delivery.id());

    auto reportId = generateReport(txn, orderId);
    EXPECT_THAT(loadReportData(txn, reportId), Pointwise(ReportEq(),
        std::vector<MosaicReportData>{{mosaicSourceId1, AOI1_AREA, std::nullopt}}));

    EXPECT_THAT(calcTotalAcceptedArea(txn, orderId), DoubleNear(AOI1_AREA, AREA_EPS));

}

Y_UNIT_TEST(intersected_aois_test)
{
    pqxx::connection conn(postgres().connectionString());
    pqxx::work txn(conn);
    //creating new order, and aoi bound to it
    auto orderId = makeOrder(txn);
    auto aoiId1 = makeAoi(txn, orderId, FIRST_AOI_NAME, geolib3::convertGeodeticToMercator(AOI_1_GEO_GEOM));
    auto delivery = makeTestDelivery(txn);
    makeAoi(txn, orderId, SECOND_AOI_NAME, geolib3::convertGeodeticToMercator(AOI_2_GEO_GEOM));
    auto mosaicSourceId1 = makeMosaic(
        txn, "MOSAIC1", db::MosaicSourceStatus::Ready,
        geolib3::convertGeodeticToMercator(MOSAIC_1_1_GEO_GEOM), orderId, aoiId1, delivery.id());

    // mosaicSourceId1 should appear only in aoi1
    auto reportId = generateReport(txn, orderId);
    EXPECT_THAT(loadReportData(txn, reportId), Pointwise(ReportEq(),
        std::vector<MosaicReportData>{{mosaicSourceId1, 771913517.5, std::nullopt}}));

}

} // Y_UNIT_TEST_SUITE(report_gen_should)

} // namespace maps::factory::sputnica::tests
