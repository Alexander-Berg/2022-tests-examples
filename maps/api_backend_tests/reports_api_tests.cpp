#include <maps/factory/services/sputnica_back/tests/common/common.h>
#include <maps/factory/services/sputnica_back/tests/api_backend_tests/fixture.h>
#include <maps/factory/services/sputnica_back/tests/common/test_data.h>

#include <maps/factory/services/sputnica_back/lib/yacare_helpers.h>

#include <maps/factory/libs/db/acceptance_report_gateway.h>
#include <maps/factory/libs/db/acceptance_report_mosaic_source_gateway.h>
#include <maps/factory/libs/db/mosaic_source_pipeline.h>
#include <maps/factory/libs/unittest/fixture.h>
#include <maps/factory/libs/sproto_helpers/order.h>

#include <maps/libs/common/include/exception.h>
#include <maps/libs/csv/include/csv.h>
#include <maps/libs/http/include/test_utils.h>
#include <yandex/maps/geolib3/sproto.h>
#include <maps/infra/yacare/include/test_utils.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

namespace maps::factory::sputnica::tests {

namespace {

template <typename T>
void checkNextVal(csv::InputStream& csvStream, T expectedVal)
{
    T val;
    csvStream >> val;
    EXPECT_EQ(val, expectedVal);
}

} // namespace

Y_UNIT_TEST_SUITE_F(reports_api_tests, Fixture) {

Y_UNIT_TEST(search)
{
    const int64_t USER = 2;
    int64_t orderId;

    pqxx::connection conn(postgres().connectionString());

    //creating new order, and aoi bound to it
    {
        pqxx::work txn(conn);
        auto mosaicSource = makeNewMosaicSource(txn);
        orderId = mosaicSource.orderId().value();
        txn.commit();
    }

    {
        http::MockRequest rq(
            http::GET,
            http::URL("http://localhost/reports/search")
                .addParam("orderId", std::to_string(orderId))
        );
        setAuthHeaderFor(db::Role::Viewer, rq);
        auto resp = yacare::performTestRequest(rq);
        ASSERT_EQ(resp.status, 200);

        auto sprotoReport = boost::lexical_cast<sproto_helpers::sfactory::Reports>(resp.body);
        EXPECT_EQ(sprotoReport.reports().size(), 0u);

    }

    db::AcceptanceReport report(orderId, USER);
    report.setStatusToGenerated();
    {
        pqxx::work txn(conn);
        db::AcceptanceReportGateway(txn).insert(report);
        txn.commit();
    }

    {
        http::MockRequest rq(
            http::GET,
            http::URL("http://localhost/reports/search")
                .addParam("orderId", std::to_string(orderId))
        );
        setAuthHeaderFor(db::Role::Viewer, rq);
        auto resp = yacare::performTestRequest(rq);
        ASSERT_EQ(resp.status, 200);

        auto sprotoReports = boost::lexical_cast<sproto_helpers::sfactory::Reports>(resp.body);
        EXPECT_EQ(sprotoReports.reports().size(), 1u);
        auto& sprotoReport = sprotoReports.reports()[0];
        EXPECT_EQ(sprotoReport.id(), std::to_string(report.id()));
        EXPECT_EQ(sprotoReport.orderId(), std::to_string(orderId));
        EXPECT_EQ(sprotoReport.status().get(), sproto_helpers::sfactory::Report::Status::GENERATED);
        EXPECT_EQ(sprotoReport.createdBy(), std::to_string(USER));
        EXPECT_FALSE(sprotoReport.approvedBy().defined());
        EXPECT_FALSE(sprotoReport.approvedAt().defined());
        EXPECT_EQ(sprotoReport.aoiData().size(), 0u);
    }

    report.approve(USER);
    {
        pqxx::work txn(conn);
        db::AcceptanceReportGateway(txn).update(report);
        txn.commit();
    }

    {
        http::MockRequest rq(
            http::GET,
            http::URL("http://localhost/reports/search")
                .addParam("orderId", std::to_string(orderId))
        );
        setAuthHeaderFor(db::Role::Viewer, rq);
        auto resp = yacare::performTestRequest(rq);
        ASSERT_EQ(resp.status, 200);

        auto sprotoReports = boost::lexical_cast<sproto_helpers::sfactory::Reports>(resp.body);
        EXPECT_EQ(sprotoReports.reports().size(), 1u);
        auto& sprotoReport = sprotoReports.reports()[0];
        EXPECT_EQ(sprotoReport.id(), "1");
        EXPECT_EQ(sprotoReport.orderId(), std::to_string(orderId));
        EXPECT_EQ(sprotoReport.status().get(), sproto_helpers::sfactory::Report::Status::APPROVED);
        EXPECT_TRUE(sprotoReport.approvedBy().defined());
        EXPECT_EQ(sprotoReport.approvedBy().get(), std::to_string(USER));
        EXPECT_TRUE(sprotoReport.approvedAt().defined());
        EXPECT_EQ(sprotoReport.aoiData().size(), 0u);
    }
}

Y_UNIT_TEST(add)
{
    const int64_t USER = 2;
    int64_t orderId;

    pqxx::connection conn(postgres().connectionString());

    //creating new order, and aoi bound to it
    {
        pqxx::work txn(conn);
        auto mosaicSource = makeNewMosaicSource(txn);
        orderId = mosaicSource.orderId().value();
        db::MosaicSourcePipeline(txn)
            .transition(mosaicSource, db::MosaicSourceStatus::Ready,
                db::UserRole(USER, db::Role::Customer));
        txn.commit();
    }

    {
        http::MockRequest rq(
            http::POST,
            http::URL("http://localhost/reports/add")
                .addParam("orderId", std::to_string(orderId))
        );
        setAuthHeaderFor(db::Role::Customer, rq);
        auto resp = yacare::performTestRequest(rq);
        ASSERT_EQ(resp.status, 200);

        auto sprotoReport = boost::lexical_cast<sproto_helpers::sfactory::Report>(resp.body);
        EXPECT_EQ(sprotoReport.id(), "1");
        EXPECT_EQ(sprotoReport.orderId(), std::to_string(orderId));
        EXPECT_EQ(sprotoReport.status().get(), sproto_helpers::sfactory::Report::Status::GENERATED);
        EXPECT_EQ(sprotoReport.createdBy(), std::to_string(unittest::TEST_CUSTOMER_USER_ID));
        EXPECT_FALSE(sprotoReport.approvedBy().defined());
        EXPECT_FALSE(sprotoReport.approvedAt().defined());
        EXPECT_EQ(sprotoReport.aoiData().size(), 0u);
    }

    {
        pqxx::work txn(conn);
        auto reports = db::AcceptanceReportGateway(txn)
            .load(db::table::AcceptanceReport::orderId == orderId,
                sql_chemistry::orderBy(db::table::AcceptanceReport::id));
        EXPECT_EQ(reports.size(), 1u);

        auto reportMosaicSources = db::AcceptanceReportMosaicSourceGateway(txn)
            .load(db::table::AcceptanceReportMosaicSource::acceptanceReportId == reports.at(0).id());
        EXPECT_EQ(reportMosaicSources.size(), 1u);
    }
}

Y_UNIT_TEST(add_with_limit)
{
    const int64_t USER = 2;
    int64_t orderId;

    pqxx::connection conn(postgres().connectionString());

    //creating new order, and aoi bound to it
    {
        pqxx::work txn(conn);
        auto mosaicSource = makeNewMosaicSource(txn);
        db::MosaicSourcePipeline(txn)
            .transition(mosaicSource, db::MosaicSourceStatus::Ready,
                db::UserRole(USER, db::Role::Customer));
        orderId = mosaicSource.orderId().value();
        txn.commit();
    }

    {
        http::MockRequest rq(
            http::POST,
            http::URL("http://localhost/reports/add")
                .addParam("orderId", std::to_string(orderId))
                .addParam("maxAreaSqrKm", "1")
        );
        setAuthHeaderFor(db::Role::Customer, rq);
        auto resp = yacare::performTestRequest(rq);
        ASSERT_EQ(resp.status, 200);

        auto sprotoReport = boost::lexical_cast<sproto_helpers::sfactory::Report>(resp.body);
        EXPECT_EQ(sprotoReport.id(), "1");
        EXPECT_EQ(sprotoReport.orderId(), std::to_string(orderId));
        EXPECT_EQ(sprotoReport.status().get(), sproto_helpers::sfactory::Report::Status::GENERATED);
        EXPECT_EQ(sprotoReport.createdBy(), std::to_string(unittest::TEST_CUSTOMER_USER_ID));
        EXPECT_FALSE(sprotoReport.approvedBy().defined());
        EXPECT_FALSE(sprotoReport.approvedAt().defined());
        EXPECT_EQ(sprotoReport.aoiData().size(), 0u);
    }

    {
        pqxx::work txn(conn);
        auto reports = db::AcceptanceReportGateway(txn)
            .load(db::table::AcceptanceReport::orderId == orderId,
                sql_chemistry::orderBy(db::table::AcceptanceReport::id));
        EXPECT_EQ(reports.size(), 1u);

        auto reportMosaicSources = db::AcceptanceReportMosaicSourceGateway(txn)
            .load(db::table::AcceptanceReportMosaicSource::acceptanceReportId == reports.at(0).id());
        EXPECT_EQ(reportMosaicSources.size(), 0u);
    }

    {
        http::MockRequest rq(
            http::POST,
            http::URL("http://localhost/reports/add")
                .addParam("orderId", std::to_string(orderId))
                .addParam("maxAreaSqrKm", "2e2")
        );
        setAuthHeaderFor(db::Role::Customer, rq);
        auto resp = yacare::performTestRequest(rq);
        ASSERT_EQ(resp.status, 200);

        auto sprotoReport = boost::lexical_cast<sproto_helpers::sfactory::Report>(resp.body);
        EXPECT_EQ(sprotoReport.id(), "2");
        EXPECT_EQ(sprotoReport.orderId(), std::to_string(orderId));
        EXPECT_EQ(sprotoReport.status().get(), sproto_helpers::sfactory::Report::Status::GENERATED);
        EXPECT_EQ(sprotoReport.createdBy(), std::to_string(unittest::TEST_CUSTOMER_USER_ID));
        EXPECT_FALSE(sprotoReport.approvedBy().defined());
        EXPECT_FALSE(sprotoReport.approvedAt().defined());
        EXPECT_EQ(sprotoReport.aoiData().size(), 0u);
    }

    {
        pqxx::work txn(conn);
        auto reports = db::AcceptanceReportGateway(txn)
            .load(db::table::AcceptanceReport::orderId == orderId,
                sql_chemistry::orderBy(db::table::AcceptanceReport::id));
        EXPECT_EQ(reports.size(), 2u);

        auto reportMosaicSources = db::AcceptanceReportMosaicSourceGateway(txn)
            .load(db::table::AcceptanceReportMosaicSource::acceptanceReportId == reports.at(1).id());
        EXPECT_EQ(reportMosaicSources.size(), 1u);
    }
}

Y_UNIT_TEST(delete)
{
    const int64_t USER = 2;
    int64_t orderId;

    pqxx::connection conn(postgres().connectionString());
    std::optional<db::MosaicSource> mosaicSource;

    {
        http::MockRequest rq(
            http::DELETE,
            http::URL("http://localhost/reports/delete")
                .addParam("reportId", "1")
        );
        setAuthHeaderFor(db::Role::Customer, rq);
        auto resp = yacare::performTestRequest(rq);
        ASSERT_EQ(resp.status, 404);
    }

    //creating new order, and aoi bound to it
    {
        pqxx::work txn(conn);
        mosaicSource = makeNewMosaicSource(txn);
        orderId = mosaicSource->orderId().value();
        txn.commit();
    }

    db::AcceptanceReport report(orderId, USER);
    report.setStatusToGenerated();

    {
        pqxx::work txn(conn);
        db::AcceptanceReportGateway(txn).insert(report);
        db::AcceptanceReportMosaicSource
            mosaicSourceReport(report.id(), mosaicSource->id(), 10., MOSAIC_GEOMETRY);
        db::AcceptanceReportMosaicSourceGateway(txn).insert(mosaicSourceReport);
        txn.commit();
    }

    {
        http::MockRequest rq(
            http::DELETE,
            http::URL("http://localhost/reports/delete")
                .addParam("reportId", std::to_string(report.id()))
        );
        setAuthHeaderFor(db::Role::Viewer, rq);
        auto resp = yacare::performTestRequest(rq);
        INFO() << rq.body;
        ASSERT_EQ(resp.status, 403);

        setAuthHeaderFor(db::Role::Supplier, rq);
        resp = yacare::performTestRequest(rq);
        ASSERT_EQ(resp.status, 403);

        setAuthHeaderFor(db::Role::Customer, rq);
        resp = yacare::performTestRequest(rq);
        ASSERT_EQ(resp.status, 200);

        pqxx::work txn(conn);
        auto reports = db::AcceptanceReportGateway(txn).load();
        EXPECT_EQ(reports.size(), 0u);

        auto reportMosaicSources = db::AcceptanceReportMosaicSourceGateway(txn).load();
        EXPECT_EQ(reports.size(), 0u);
    }

    report = db::AcceptanceReport(orderId, USER);
    report.setStatusToGenerated().approve(USER);
    {
        pqxx::work txn(conn);
        db::AcceptanceReportGateway(txn).insert(report);
        txn.commit();
    }

    {
        http::MockRequest rq(
            http::DELETE,
            http::URL("http://localhost/reports/delete")
                .addParam("reportId", std::to_string(report.id()))
        );
        setAuthHeaderFor(db::Role::Customer, rq);
        auto resp = yacare::performTestRequest(rq);
        ASSERT_EQ(resp.status, 412);

        pqxx::work txn(conn);
        auto reports = db::AcceptanceReportGateway(txn).load();
        EXPECT_EQ(reports.size(), 1u);
    }
}

Y_UNIT_TEST(set_status)
{
    const int64_t USER = 2;
    int64_t orderId;

    pqxx::connection conn(postgres().connectionString());
    std::optional<db::MosaicSource> mosaicSource;

    //creating new order, and aoi bound to it
    {
        pqxx::work txn(conn);
        mosaicSource = makeNewMosaicSource(txn);
        orderId = mosaicSource->orderId().value();
        txn.commit();
    }

    db::AcceptanceReport report(orderId, USER);

    {
        pqxx::work txn(conn);
        db::AcceptanceReportGateway(txn).insert(report);
        txn.commit();
    }

    {
        http::MockRequest rq(
            http::PATCH,
            http::URL("http://localhost/reports/set-status")
                .addParam("reportId", std::to_string(report.id()))
        );

        sproto_helpers::sfactory::ReportStatus statusMessage;
        statusMessage.status() = sproto_helpers::sfactory::Report::Status::APPROVED;
        rq.body = boost::lexical_cast<std::string>(statusMessage);

        setAuthHeaderFor(db::Role::Customer, rq);
        auto resp = yacare::performTestRequest(rq);
        ASSERT_EQ(resp.status, 403);
    }

    report.setStatusToGenerated();

    {
        pqxx::work txn(conn);
        db::AcceptanceReportGateway(txn).update(report);
        db::AcceptanceReportMosaicSource
            mosaicSourceReport(report.id(), mosaicSource->id(), 10., MOSAIC_GEOMETRY);
        db::AcceptanceReportMosaicSourceGateway(txn).insert(mosaicSourceReport);
        txn.commit();
    }

    {
        http::MockRequest rq(
            http::PATCH,
            http::URL("http://localhost/reports/set-status")
                .addParam("reportId", std::to_string(report.id()))
        );

        sproto_helpers::sfactory::ReportStatus statusMessage;
        statusMessage.status() = sproto_helpers::sfactory::Report::Status::APPROVED;
        rq.body = boost::lexical_cast<std::string>(statusMessage);

        setAuthHeaderFor(db::Role::Viewer, rq);
        auto resp = yacare::performTestRequest(rq);
        INFO() << rq.body;
        ASSERT_EQ(resp.status, 403);

        setAuthHeaderFor(db::Role::Supplier, rq);
        resp = yacare::performTestRequest(rq);
        ASSERT_EQ(resp.status, 403);

        setAuthHeaderFor(db::Role::Customer, rq);
        resp = yacare::performTestRequest(rq);
        ASSERT_EQ(resp.status, 200);

        auto sprotoReport = boost::lexical_cast<sproto_helpers::sfactory::Report>(resp.body);
        EXPECT_EQ(sprotoReport.id(), "1");
        EXPECT_EQ(sprotoReport.orderId(), std::to_string(orderId));
        EXPECT_EQ(sprotoReport.status().get(), sproto_helpers::sfactory::Report::Status::APPROVED);
        EXPECT_EQ(sprotoReport.createdBy(), std::to_string(USER));
        EXPECT_TRUE(sprotoReport.approvedBy().defined());
        EXPECT_EQ(sprotoReport.approvedBy().get(), std::to_string(unittest::TEST_CUSTOMER_USER_ID));
        EXPECT_TRUE(sprotoReport.approvedAt().defined());
        EXPECT_EQ(sprotoReport.aoiData().size(), 0u);

        pqxx::work txn(conn);
        auto report = db::AcceptanceReportGateway(txn).loadById(1);
        EXPECT_EQ(report.status(), db::AcceptanceReportStatus::Approved);
    }
}

Y_UNIT_TEST(get_content)
{
    int64_t orderId;
    int64_t aoiId;
    int64_t mosaicSourceId;
    int64_t USER = 1;
    std::optional<db::MosaicSource> mosaicSource;
    double EXPECTED_PAID_AREA = 754540624.2;
    double EXPECTED_ORDERED_AREA = 103772134.62523091;

    pqxx::connection conn(postgres().connectionString());

    {
        pqxx::work txn(conn);
        mosaicSource = makeNewMosaicSource(txn);
        orderId = mosaicSource->orderId().value();
        aoiId = mosaicSource->aoiId().value();
        mosaicSourceId = mosaicSource->id();
        db::MosaicSourcePipeline pipeline(txn);
        pipeline
            .transition(*mosaicSource, db::MosaicSourceStatus::Ready, db::UserRole(USER, db::Role::Customer));
        txn.commit();
    }

    db::AcceptanceReport report(orderId, USER);

    {
        pqxx::work txn(conn);
        db::AcceptanceReportGateway(txn).insert(report);
        db::AcceptanceReportMosaicSource
            mosaicSourceReport(report.id(), mosaicSource->id(), EXPECTED_PAID_AREA, MOSAIC_GEOMETRY);
        db::AcceptanceReportMosaicSourceGateway(txn).insert(mosaicSourceReport);
        report.setStatusToGenerated();
        db::AcceptanceReportGateway(txn).update(report);
        txn.commit();
    }

    {
        http::MockRequest rq(
            http::GET,
            http::URL("http://localhost/reports/get-content")
                .addParam("reportId", std::to_string(report.id()))
        );
        setAuthHeaderFor(db::Role::Viewer, rq);
        auto resp = yacare::performTestRequest(rq);
        ASSERT_EQ(resp.status, 200);

        auto sprotoReport = boost::lexical_cast<sproto_helpers::sfactory::Report>(resp.body);
        EXPECT_EQ(sprotoReport.id(), std::to_string(report.id()));
        EXPECT_EQ(sprotoReport.orderId(), std::to_string(orderId));
        EXPECT_EQ(sprotoReport.status().get(), sproto_helpers::sfactory::Report::Status::GENERATED);
        EXPECT_FALSE(sprotoReport.approvedBy().defined());
        EXPECT_FALSE(sprotoReport.approvedBy().defined());
        EXPECT_EQ(sprotoReport.aoiData().size(), 1u);
        auto& saoiData = sprotoReport.aoiData()[0];

        EXPECT_EQ(saoiData.aoiId(), std::to_string(aoiId));
        EXPECT_EQ(saoiData.aoiName(), FIRST_AOI_NAME);
        EXPECT_NEAR(saoiData.area().get(), EXPECTED_ORDERED_AREA, 0.1);
        EXPECT_EQ(saoiData.mosaicData().size(), 1u);

        auto& smosaicData = saoiData.mosaicData()[0];
        EXPECT_EQ(smosaicData.mosaicId(), std::to_string(mosaicSource->id()));
        EXPECT_EQ(smosaicData.mosaicName(), mosaicSource->name());
        EXPECT_EQ(smosaicData.dgOrderNo().get(), MOSAIC_DG_ORDER_NO);
        EXPECT_EQ(smosaicData.dgOrderItemNo().get(), MOSAIC_DG_ORDER_ITEM_NO);
        EXPECT_TRUE(smosaicData.deliveryDate().defined());
        EXPECT_TRUE(smosaicData.acceptanceDate().defined());
        EXPECT_NEAR(smosaicData.area(), EXPECTED_PAID_AREA, 0.1);
    }

    {
        http::MockRequest rq(
            http::GET,
            http::URL("http://localhost/reports/get-content")
                .addParam("reportId", std::to_string(report.id()))
                .addParam("format", "csv")
        );
        setAuthHeaderFor(db::Role::Viewer, rq);
        auto resp = yacare::performTestRequest(rq);
        ASSERT_EQ(resp.status, 200);

        EXPECT_TRUE(resp.headers.count("Last-Modified"));

        std::istringstream is(resp.body);
        csv::InputStream csvResp(is);

        const std::vector<std::string> EXPECTED_HEADERS{
            "orderId", "aoiName", "dgOrderNo", "dgOrderItemNo", "productName",
            "deliveredAt", "acceptedAt", "additionalArea km2"
        };

        for (const auto& header: EXPECTED_HEADERS) {
            std::string val;
            csvResp >> val;
            EXPECT_EQ(header, val);
        }
        EXPECT_EQ(csvResp.line(), 2u);

        checkNextVal(csvResp, report.id());
        checkNextVal(csvResp, FIRST_AOI_NAME);
        checkNextVal(csvResp, MOSAIC_DG_ORDER_NO);
        checkNextVal(csvResp, MOSAIC_DG_ORDER_ITEM_NO);
        checkNextVal(csvResp, MOSAIC_NAME);

        std::string dateStr;
        csvResp >> dateStr;
        EXPECT_FALSE(dateStr.empty());
        csvResp >> dateStr;
        EXPECT_FALSE(dateStr.empty());

        checkNextVal(csvResp, 754.54);

        EXPECT_TRUE(csvResp.eof());
    }
}

Y_UNIT_TEST(estimate)
{
    int64_t orderId;
    int64_t aoiId;
    int64_t mosaicSourceId;
    int64_t USER = 1;
    std::optional<db::MosaicSource> mosaicSource;
    double EXPECTED_ACCEPTED_AREA = 102515150.73123933;
    double EXPECTED_PAID_AREA = 1000;
    double EXPECTED_ORDERED_AREA = 103772134.62523091;

    pqxx::connection conn(postgres().connectionString());

    //creating new order, and aoi bound to it
    {
        pqxx::work txn(conn);

        mosaicSource = makeNewMosaicSource(txn);
        orderId = mosaicSource->orderId().value();
        aoiId = mosaicSource->aoiId().value();
        mosaicSourceId = mosaicSource->id();
        txn.commit();
    }

    {
        http::MockRequest rq(
            http::GET,
            http::URL("http://localhost/reports/estimate")
                .addParam("orderId", std::to_string(orderId))
        );
        setAuthHeaderFor(db::Role::Viewer, rq);
        auto resp = yacare::performTestRequest(rq);
        ASSERT_EQ(resp.status, 200);

        auto sprotoReport = boost::lexical_cast<sproto_helpers::sfactory::EstimatedReport>(resp.body);
        ASSERT_EQ(sprotoReport.aoiData().size(), 1u);
        const auto& firstAoi = sprotoReport.aoiData()[0];
        EXPECT_EQ(firstAoi.aoiId(), std::to_string(aoiId));
        EXPECT_EQ(firstAoi.aoiName(), FIRST_AOI_NAME);
        EXPECT_NEAR(firstAoi.acceptedArea(), 0, 0.1);
        EXPECT_NEAR(firstAoi.paidArea(), 0., 0.1);
    }

    db::AcceptanceReport report(orderId, USER);

    {
        pqxx::work txn(conn);
        mosaicSource->setStatus(db::MosaicSourceStatus::Ready);
        db::MosaicSourceGateway(txn).update(*mosaicSource);
        db::AcceptanceReportGateway(txn).insert(report);
        db::AcceptanceReportMosaicSource
            mosaicSourceReport(report.id(), mosaicSource->id(), EXPECTED_PAID_AREA, MOSAIC_GEOMETRY);
        db::AcceptanceReportMosaicSourceGateway(txn).insert(mosaicSourceReport);
        report.setStatusToGenerated();
        db::AcceptanceReportGateway(txn).update(report);
        txn.commit();
    }

    {
        http::MockRequest rq(
            http::GET,
            http::URL("http://localhost/reports/estimate")
                .addParam("orderId", std::to_string(orderId))
        );
        setAuthHeaderFor(db::Role::Viewer, rq);
        auto resp = yacare::performTestRequest(rq);
        ASSERT_EQ(resp.status, 200);

        auto sprotoReport = boost::lexical_cast<sproto_helpers::sfactory::EstimatedReport>(resp.body);
        ASSERT_EQ(sprotoReport.aoiData().size(), 1u);
        const auto& firstAoi = sprotoReport.aoiData()[0];
        EXPECT_NEAR(firstAoi.acceptedArea(), EXPECTED_ACCEPTED_AREA, 0.1);
        EXPECT_NEAR(firstAoi.paidArea(), 0., 0.1);
    }

    {
        pqxx::work txn(conn);
        report.approve(USER);
        db::AcceptanceReportGateway(txn).update(report);
        txn.commit();
    }

    {
        http::MockRequest rq(
            http::GET,
            http::URL("http://localhost/reports/estimate")
                .addParam("orderId", std::to_string(orderId))
        );
        setAuthHeaderFor(db::Role::Viewer, rq);
        auto resp = yacare::performTestRequest(rq);
        ASSERT_EQ(resp.status, 200);

        auto sprotoReport = boost::lexical_cast<sproto_helpers::sfactory::EstimatedReport>(resp.body);
        ASSERT_EQ(sprotoReport.aoiData().size(), 1u);
        const auto& firstAoi = sprotoReport.aoiData()[0];
        EXPECT_NEAR(firstAoi.acceptedArea(), EXPECTED_ACCEPTED_AREA, 0.1);
        EXPECT_NEAR(firstAoi.paidArea(), EXPECTED_PAID_AREA, 0.1);
        EXPECT_NEAR(firstAoi.area().get(), EXPECTED_ORDERED_AREA, 0.1);
        EXPECT_NEAR(sprotoReport.totalAcceptedArea(), EXPECTED_ACCEPTED_AREA, 0.1);
        EXPECT_NEAR(sprotoReport.totalPaidArea(), EXPECTED_PAID_AREA, 0.1);
        EXPECT_NEAR(sprotoReport.totalArea().get(), EXPECTED_ORDERED_AREA, 0.1);
    }
}

} //Y_UNIT_TEST_SUITE

} //namespace maps::factory::sputnica::tests
