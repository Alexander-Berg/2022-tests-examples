#include <maps/factory/libs/db/acceptance_report_gateway.h>
#include <maps/factory/libs/db/acceptance_report_mosaic_source_gateway.h>
#include <maps/factory/libs/db/mosaic_source_gateway.h>
#include <maps/factory/libs/db/order_gateway.h>
#include <maps/factory/libs/db/tests/test_data.h>

#include <maps/factory/libs/unittest/fixture.h>

#include <maps/libs/introspection/include/comparison.h>
#include <maps/libs/introspection/include/stream_output.h>
#include <maps/libs/geolib/include/test_tools/comparison.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

namespace maps::factory::db {

using introspection::operator==;
using introspection::operator!=;
using introspection::operator<<;

} // namespace maps::factory::db

namespace maps::factory::db::tests {
using namespace testing;

Y_UNIT_TEST_SUITE(test_acceptance_report_gateway) {

namespace {

constexpr int64_t USER1 = 1;
constexpr int64_t USER2 = 2;

} // namespace

Y_UNIT_TEST(creating_acceptance_report)
{
    unittest::Fixture fixture;
    pqxx::connection conn(fixture.postgres().connectionString());

    Order order(2019, OrderType::Tasking);
    MosaicSource ms(TEST_MOSAIC_SOURCE_NAME);
    ms.setMercatorGeom(TEST_MOSAIC_SOURCE_GEOMETRY);
    ms.setMinZoom(10);
    ms.setMaxZoom(18);
    ms.setSatellite(TEST_MOSAIC_SOURCE_SATELLITE);

    {
        pqxx::work txn(conn);
        OrderGateway(txn).insert(order);
        ms.setOrderId(order.id());
        MosaicSourceGateway(txn).insert(ms);
        txn.commit();
    }

    AcceptanceReport acceptanceReport(order.id(), USER1);

    {
        pqxx::work txn(conn);
        AcceptanceReportGateway(txn).insert(acceptanceReport);
        txn.commit();
        EXPECT_EQ(acceptanceReport.id(), 1);
        EXPECT_EQ(acceptanceReport.status(), AcceptanceReportStatus::Generating);
        EXPECT_EQ(acceptanceReport.createdBy(), USER1);
    }

    AcceptanceReportMosaicSource reportMosaicSource(
        acceptanceReport.id(), ms.id(), 10., TEST_MOSAIC_SOURCE_GEOMETRY);
    {
        pqxx::work txn(conn);
        AcceptanceReportMosaicSourceGateway(txn).insert(reportMosaicSource);
        acceptanceReport.setStatusToGenerated();
        EXPECT_EQ(acceptanceReport.status(), AcceptanceReportStatus::Generated);
        AcceptanceReportGateway(txn).update(acceptanceReport);
        txn.commit();
    }

    {
        acceptanceReport.approve(USER2);
        EXPECT_EQ(acceptanceReport.status(), AcceptanceReportStatus::Approved);
        EXPECT_EQ(acceptanceReport.approvedBy(), USER2);
        EXPECT_TRUE(acceptanceReport.approvedAt().has_value());
        pqxx::work txn(conn);
        auto loadedReportMosaicSources = AcceptanceReportMosaicSourceGateway(txn)
            .load(table::AcceptanceReportMosaicSource::acceptanceReportId == acceptanceReport.id());
        EXPECT_EQ(loadedReportMosaicSources.size(), 1u);
        EXPECT_EQ(loadedReportMosaicSources[0].acceptanceReportId(), reportMosaicSource.acceptanceReportId());
        EXPECT_EQ(loadedReportMosaicSources[0].mosaicSourceId(), reportMosaicSource.mosaicSourceId());
        EXPECT_EQ(loadedReportMosaicSources[0].additionalArea(), reportMosaicSource.additionalArea());
        EXPECT_TRUE(geolib3::test_tools::approximateEqual(
            loadedReportMosaicSources[0].additionalMercatorGeom(),
            reportMosaicSource.additionalMercatorGeom(), 0.0
        ));
        AcceptanceReportGateway(txn).update(acceptanceReport);
        txn.commit();
    }

    {
        pqxx::work txn(conn);
        auto loadedAcceptanceReport =
            AcceptanceReportGateway(txn).loadById(acceptanceReport.id());

        EXPECT_EQ(loadedAcceptanceReport, acceptanceReport);
    }

    // test update reportMosaicSource
    reportMosaicSource.setAdditionalArea(20.);

    {
        pqxx::work txn(conn);
        AcceptanceReportMosaicSourceGateway gtw(txn);
        gtw.update(reportMosaicSource);
        auto loadedReportMosaicSources =
            gtw.load(table::AcceptanceReportMosaicSource::acceptanceReportId == acceptanceReport.id());
        EXPECT_EQ(loadedReportMosaicSources.size(), 1u);
        EXPECT_EQ(loadedReportMosaicSources[0].additionalArea(), 20.);
    }
}

Y_UNIT_TEST(forbidden_to_refer_one_mosaic_in_several_reports)
{
    unittest::Fixture fixture;
    pqxx::connection conn(fixture.postgres().connectionString());

    Order order(2019, OrderType::Tasking);
    MosaicSource ms(TEST_MOSAIC_SOURCE_NAME);
    ms.setMercatorGeom(TEST_MOSAIC_SOURCE_GEOMETRY);
    ms.setMinZoom(10);
    ms.setMaxZoom(18);
    ms.setSatellite(TEST_MOSAIC_SOURCE_SATELLITE);

    {
        pqxx::work txn(conn);
        OrderGateway(txn).insert(order);
        ms.setOrderId(order.id());
        MosaicSourceGateway(txn).insert(ms);
        txn.commit();
    }

    AcceptanceReport acceptanceReport1(order.id(), USER1);
    acceptanceReport1.setStatusToGenerated();
    AcceptanceReport acceptanceReport2(order.id(), USER1);
    acceptanceReport2.setStatusToGenerated();

    {
        pqxx::work txn(conn);
        AcceptanceReportGateway(txn).insert(acceptanceReport1);
        AcceptanceReportGateway(txn).insert(acceptanceReport2);
        txn.commit();
    }

    AcceptanceReportMosaicSource reportMosaicSource1(
        acceptanceReport1.id(), ms.id(), 10., TEST_MOSAIC_SOURCE_GEOMETRY);
    AcceptanceReportMosaicSource reportMosaicSource2(
        acceptanceReport2.id(), ms.id(), 10., TEST_MOSAIC_SOURCE_GEOMETRY);
    {
        pqxx::work txn(conn);
        AcceptanceReportMosaicSourceGateway(txn).insert(reportMosaicSource1);
        EXPECT_THROW(
            AcceptanceReportMosaicSourceGateway(txn).insert(reportMosaicSource2),
            maps::Exception);
    }
}

} // Y_UNIT_TEST_SUITE(test_acceptance_report_gateway)


} //namespace maps::factory::db::tests

