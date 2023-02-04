#include <maps/factory/services/backend/tests/test_utils.h>

#include <maps/factory/libs/db/delivery_gateway.h>
#include <maps/factory/libs/db/mosaic_gateway.h>
#include <maps/factory/libs/db/mosaic_source_gateway.h>
#include <maps/factory/libs/db/release_gateway.h>
#include <maps/factory/libs/sproto_helpers/etag_utils.h>
#include <maps/factory/libs/sproto_helpers/mosaic.h>
#include <maps/factory/libs/sproto_helpers/rendering_params.h>

#include <library/cpp/testing/gtest/gtest.h>
#include <maps/libs/sql_chemistry/include/sequence.h>
#include <maps/libs/geolib/include/algorithm.h>

namespace maps::factory::backend::tests {

namespace {

using namespace sproto_helpers;

const std::string URL_BULK_CREATE = "http://localhost/v1/mosaics/bulk_create";
const std::string URL_GET = "http://localhost/v1/mosaics/get";
const std::string URL_BULK_GET = "http://localhost/v1/mosaics/bulk_get";
const std::string URL_SEARCH = "http://localhost/v1/mosaics/search";
const std::string URL_UPDATE = "http://localhost/v1/mosaics/update";
const std::string URL_BULK_UPDATE = "http://localhost/v1/mosaics/bulk_update";
const std::string URL_DELETE = "http://localhost/v1/mosaics/delete";
const std::string URL_BULK_DELETE = "http://localhost/v1/mosaics/bulk_delete";

class MosaicsFixture : public BackendFixture {
public:
    MosaicsFixture()
        : testRelease_(createTestRelease())
    {
        auto txn = txnHandle();

        auto delivery = createTestDelivery();
        db::DeliveryGateway(*txn).insert(delivery);

        db::ReleaseGateway(*txn).insert(testRelease_);

        auto mosaicSource = createTestMosaicSource();
        db::MosaicSourceGateway(*txn).insert(mosaicSource);

        testMosaics_ = db::Mosaics{
            createTestMosaic(mosaicSource.id(), testRelease_.id()),
            createTestMosaic(mosaicSource.id(), testRelease_.id())
        };
        db::MosaicGateway(*txn).insert(testMosaics_);

        mosaicEtag_ = calculateEtag(testMosaics_.at(0));

        sql_chemistry::Sequence<db::table::MosaicZorderSeq>(*txn).setVal(ZORDER);

        txn->commit();
    }

    const db::Mosaics& testMosaics() { return testMosaics_; }

    const db::Mosaic& testMosaic() { return testMosaics_.at(0); }

    const db::Release& testRelease() { return testRelease_; }

    const std::string& mosaicEtag() { return mosaicEtag_; }

private:
    db::Mosaics testMosaics_;
    db::Release testRelease_;
    std::string mosaicEtag_;
};

} // namespace

TEST_F(MosaicsFixture, test_mosaics_api_bulk_create)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    http::MockRequest request(http::POST, http::URL(URL_BULK_CREATE));
    request.body = boost::lexical_cast<std::string>(
        convertToSproto<smosaics::MosaicsData>(testMosaics()));
    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(response.status, 200);

    const auto respMosaics = boost::lexical_cast<smosaics::Mosaics>(response.body);
    ASSERT_EQ(respMosaics.mosaics().size(), 2u);
    for (const auto& respMosaic: respMosaics.mosaics()) {
        ASSERT_NE(respMosaic.id(), "0");
    }

    db::Mosaics dbMosaics;
    ASSERT_NO_THROW(
        dbMosaics = db::MosaicGateway(*txnHandle())
            .loadByIds({
                db::parseId(respMosaics.mosaics().at(0).id()),
                db::parseId(respMosaics.mosaics().at(1).id())
            });
    );

    int64_t zorder = ZORDER;
    for (const auto& respMosaic: respMosaics.mosaics()) {
        EXPECT_EQ(respMosaic.mercatorShift()->x(), SHIFT.x());
        EXPECT_EQ(respMosaic.mercatorShift()->y(), SHIFT.y());
        EXPECT_EQ(respMosaic.zoomMin(), MIN_ZOOM);
        EXPECT_EQ(respMosaic.zoomMax(), MAX_ZOOM);
        EXPECT_TRUE(
            geolib3::convertGeodeticToMercator(
                geolib3::sproto::decode(*respMosaic.geometry())
            ) == GEOMETRY
        );
        EXPECT_EQ(respMosaic.zIndex(), ++zorder);
        EXPECT_EQ(convertFromSproto(
            *respMosaic.colorCorrectionParams()), COLOR_CORRECTION_PARAMS);
        EXPECT_EQ(convertFromSproto(
            *respMosaic.sharpingParams()), SHARPING_PARAMS);
        EXPECT_EQ(*respMosaic.modifiedBy(), USER_NAME);
        EXPECT_TRUE(respMosaic.modifiedAt());
    }

    zorder = ZORDER;
    for (const auto& dbMosaic: dbMosaics) {
        EXPECT_EQ(dbMosaic.zOrder(), ++zorder);
        EXPECT_EQ(dbMosaic.minZoom(), MIN_ZOOM);
        EXPECT_EQ(dbMosaic.maxZoom(), MAX_ZOOM);
        EXPECT_EQ(dbMosaic.shift().x(), SHIFT.x());
        EXPECT_EQ(dbMosaic.shift().y(), SHIFT.y());
        EXPECT_TRUE(dbMosaic.mercatorGeom() == GEOMETRY);
        EXPECT_EQ(*dbMosaic.renderingParams(), RENDERING_PARAMS);
        EXPECT_EQ(dbMosaic.modifiedBy(), USER_NAME);
        EXPECT_TRUE(dbMosaic.modifiedAt());
    }
}

TEST_F(MosaicsFixture, test_mosaics_api_bulk_create_wrong_status)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    auto txn = txnHandle();
    auto release = testRelease();
    release.setStatus(db::ReleaseStatus::Ready);
    db::ReleaseGateway(*txn).update(release);
    txn->commit();

    http::MockRequest request(http::POST, http::URL(URL_BULK_CREATE));
    request.body = boost::lexical_cast<std::string>(
        convertToSproto<smosaics::MosaicsData>(testMosaics()));
    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(response.status, 409);
}

TEST_F(MosaicsFixture, test_mosaics_api_bulk_create_wrong_mosaic_source_status)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    auto txn = txnHandle();
    auto mosaicSource = createTestMosaicSource();
    mosaicSource.setName(mosaicSource.name() + "_clone");
    mosaicSource.setStatus(db::MosaicSourceStatus::New);
    db::MosaicSourceGateway{*txn}.insert(mosaicSource);
    auto mosaic = createTestMosaic(mosaicSource.id(), testRelease().id());
    txn->commit();

    http::MockRequest request(http::POST, http::URL(URL_BULK_CREATE));
    request.body = boost::lexical_cast<std::string>(
        convertToSproto<smosaics::MosaicsData>(db::Mosaics{mosaic}));
    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(response.status, 409);
}

TEST_F(MosaicsFixture, test_mosaics_api_bulk_create_source_without_cog)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    auto txn = txnHandle();

    auto mosaicSource = createTestMosaicSource();
    mosaicSource.setName(mosaicSource.name() + "_NO_COG");
    mosaicSource.setCogPath(std::nullopt);
    db::MosaicSourceGateway(*txn).insert(mosaicSource);

    auto mosaic = createTestMosaic(mosaicSource.id(), testRelease().id());
    txn->commit();

    http::MockRequest request(http::POST, http::URL(URL_BULK_CREATE));
    request.body = boost::lexical_cast<std::string>(
        convertToSproto<smosaics::MosaicsData>(db::Mosaics{mosaic}));
    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(response.status, 400);
}

TEST_F(MosaicsFixture, test_mosaics_api_get)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    http::MockRequest request(
        http::GET,
        http::URL(URL_GET)
            .addParam("id", testMosaic().id())
    );
    auto response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 200);

    const auto respMosaic = boost::lexical_cast<smosaics::Mosaic>(response.body);

    EXPECT_EQ(respMosaic.mercatorShift()->x(), SHIFT.x());
    EXPECT_EQ(respMosaic.mercatorShift()->y(), SHIFT.y());
    EXPECT_EQ(respMosaic.zoomMin(), MIN_ZOOM);
    EXPECT_EQ(respMosaic.zoomMax(), MAX_ZOOM);
    EXPECT_TRUE(
        geolib3::convertGeodeticToMercator(
            geolib3::sproto::decode(*respMosaic.geometry())
        ) == GEOMETRY
    );
    EXPECT_EQ(respMosaic.zIndex(), ZORDER);
    EXPECT_EQ(convertFromSproto(
        *respMosaic.colorCorrectionParams()), COLOR_CORRECTION_PARAMS);
    EXPECT_EQ(convertFromSproto(
        *respMosaic.sharpingParams()), SHARPING_PARAMS);
}

TEST_F(MosaicsFixture, test_mosaics_api_old_bulk_get)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    http::MockRequest request(
        http::GET,
        http::URL(URL_BULK_GET)
            .addParam("ids", std::to_string(testMosaics().at(0).id()) + ',' +
                             std::to_string(testMosaics().at(1).id()))
    );
    auto response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 200);

    auto respMosaics = boost::lexical_cast<smosaics::Mosaics>(response.body);

    for (const auto& respMosaic: respMosaics.mosaics()) {
        EXPECT_EQ(respMosaic.mercatorShift()->x(), SHIFT.x());
        EXPECT_EQ(respMosaic.mercatorShift()->y(), SHIFT.y());
        EXPECT_EQ(respMosaic.zoomMin(), MIN_ZOOM);
        EXPECT_EQ(respMosaic.zoomMax(), MAX_ZOOM);
        EXPECT_TRUE(
            geolib3::convertGeodeticToMercator(
                geolib3::sproto::decode(*respMosaic.geometry())
            ) == GEOMETRY
        );
        EXPECT_EQ(respMosaic.zIndex(), ZORDER);
        EXPECT_EQ(convertFromSproto(
            *respMosaic.colorCorrectionParams()), COLOR_CORRECTION_PARAMS);
        EXPECT_EQ(convertFromSproto(
            *respMosaic.sharpingParams()), SHARPING_PARAMS);
    }
}

TEST_F(MosaicsFixture, test_mosaics_api_old_bulk_get_special_cases)
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
                .addParam("ids", std::to_string(testMosaic().id()) + ",1111")
        );
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(response.status, 200);
    }
}

TEST_F(MosaicsFixture, test_mosaics_api_bulk_get)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    http::MockRequest request(http::POST, http::URL(URL_BULK_GET));
    request.body = boost::lexical_cast<std::string>(
        sproto_helpers::convertToSproto<smosaics::GetMosaicsRequest>({
            testMosaics().at(0).id(), testMosaics().at(1).id()
        })
    );

    auto response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 200);

    auto respMosaics = boost::lexical_cast<smosaics::Mosaics>(response.body);

    for (const auto& respMosaic: respMosaics.mosaics()) {
        EXPECT_EQ(respMosaic.mercatorShift()->x(), SHIFT.x());
        EXPECT_EQ(respMosaic.mercatorShift()->y(), SHIFT.y());
        EXPECT_EQ(respMosaic.zoomMin(), MIN_ZOOM);
        EXPECT_EQ(respMosaic.zoomMax(), MAX_ZOOM);
        EXPECT_TRUE(
            geolib3::convertGeodeticToMercator(
                geolib3::sproto::decode(*respMosaic.geometry())
            ) == GEOMETRY
        );
        EXPECT_EQ(respMosaic.zIndex(), ZORDER);
        EXPECT_EQ(convertFromSproto(
            *respMosaic.colorCorrectionParams()), COLOR_CORRECTION_PARAMS);
        EXPECT_EQ(convertFromSproto(
            *respMosaic.sharpingParams()), SHARPING_PARAMS);
    }
}

TEST_F(MosaicsFixture, test_mosaics_api_bulk_get_special_cases)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));
    {
        http::MockRequest request(http::POST, http::URL(URL_BULK_GET));
        sproto_helpers::smosaics::GetMosaicsRequest ids;
        ids.mosaic_ids().push_back("a");
        request.body = boost::lexical_cast<std::string>(ids);
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(response.status, 400);
    }
    {
        http::MockRequest request(http::POST, http::URL(URL_BULK_GET));
        request.body = boost::lexical_cast<std::string>(
            sproto_helpers::convertToSproto<smosaics::GetMosaicsRequest>({
                testMosaic().id(), 1111
            })
        );
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(response.status, 200);
    }
}

TEST_F(MosaicsFixture, test_mosaics_api_search_by_ll)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    auto mosaicCentroid = geolib3::findCentroid(testMosaic().geodeticGeom());

    http::MockRequest request(
        http::GET,
        http::URL(URL_SEARCH + "?ll=" + std::to_string(mosaicCentroid.x())
                  + "," + std::to_string(mosaicCentroid.y()))
    );
    auto response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 200);

    auto respMosaics = boost::lexical_cast<smosaics::Mosaics>(response.body);
    EXPECT_EQ(respMosaics.mosaics().size(), 2u);
}

TEST_F(MosaicsFixture, test_mosaics_api_filter_search_by_ll)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    {
        auto txn = txnHandle();
        auto mosaic = createTestMosaic(
            testMosaic().mosaicSourceId(), testRelease().id());
        mosaic.setMercatorGeom(geolib3::MultiPolygon2({
            geolib3::Polygon2(
                geolib3::PointsVector{
                    {4165686.57521403580904, 7495706.201570989564061},
                    {4216731.124910146929324, 7497640.497828515246511},
                    {4187008.982049120590091, 7457941.783788669854403},
                    {4165686.57521403580904, 7495706.201570989564061}
                }
            )
        }));
        db::MosaicGateway(*txn).insert(mosaic);
        txn->commit();
    }

    http::MockRequest request(
        http::GET,
        http::URL(URL_SEARCH + "?ll=37.513786,55.769340")
    );
    auto response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 200);

    auto respMosaics = boost::lexical_cast<smosaics::Mosaics>(response.body);
    EXPECT_EQ(respMosaics.mosaics().size(), 0u);
}

TEST_F(MosaicsFixture, test_mosaics_api_search_by_release_id)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    http::MockRequest request(
        http::GET,
        http::URL(URL_SEARCH + "?release_id=" + std::to_string(testRelease().id()))
    );
    auto response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 200);

    auto respMosaics = boost::lexical_cast<smosaics::Mosaics>(response.body);
    EXPECT_EQ(respMosaics.mosaics().size(), 2u);
}

TEST_F(MosaicsFixture, test_mosaics_api_update)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    auto uMosaic = testMosaic();
    updateTestMosaic(uMosaic);
    auto sprotoUMosaic = convertToSproto(uMosaic);
    sprotoUMosaic.etag() = mosaicEtag();

    http::MockRequest request(http::POST, http::URL(URL_UPDATE));
    request.body = boost::lexical_cast<std::string>(sprotoUMosaic);
    auto response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 200);

    const auto respMosaic = boost::lexical_cast<smosaics::Mosaic>(response.body);

    db::Mosaic dbMosaic = createTestMosaic();
    ASSERT_NO_THROW(
        dbMosaic = db::MosaicGateway(*txnHandle())
            .loadById(db::parseId(respMosaic.id()));
    );

    EXPECT_EQ(respMosaic.mercatorShift()->x(), SHIFT_U.x());
    EXPECT_EQ(respMosaic.mercatorShift()->y(), SHIFT_U.y());
    EXPECT_EQ(respMosaic.zoomMin(), MIN_ZOOM_U);
    EXPECT_EQ(respMosaic.zoomMax(), MAX_ZOOM_U);
    EXPECT_TRUE(
        geolib3::convertGeodeticToMercator(
            geolib3::sproto::decode(*respMosaic.geometry())
        ) == GEOMETRY_U
    );
    EXPECT_EQ(respMosaic.zIndex(), ZORDER_U);
    EXPECT_EQ(convertFromSproto(
        *respMosaic.colorCorrectionParams()), COLOR_CORRECTION_PARAMS_U);
    EXPECT_EQ(convertFromSproto(
        *respMosaic.sharpingParams()), SHARPING_PARAMS_U);
    EXPECT_EQ(*respMosaic.modifiedBy(), USER_NAME);
    EXPECT_TRUE(respMosaic.modifiedAt());

    EXPECT_EQ(dbMosaic.zOrder(), ZORDER_U);
    EXPECT_EQ(dbMosaic.minZoom(), MIN_ZOOM_U);
    EXPECT_EQ(dbMosaic.maxZoom(), MAX_ZOOM_U);
    EXPECT_EQ(dbMosaic.shift().x(), SHIFT_U.x());
    EXPECT_EQ(dbMosaic.shift().y(), SHIFT_U.y());
    EXPECT_TRUE(dbMosaic.mercatorGeom() == GEOMETRY_U);
    EXPECT_EQ(*dbMosaic.renderingParams(), RENDERING_PARAMS_U);
    EXPECT_EQ(dbMosaic.modifiedBy(), USER_NAME);
    EXPECT_TRUE(dbMosaic.modifiedAt());
}

TEST_F(MosaicsFixture, test_mosaics_api_bulk_update)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    auto uMosaics = testMosaics();
    updateTestMosaic(uMosaics.at(0));
    updateTestMosaic(uMosaics.at(1));
    auto sprotoUMosaics = convertToSproto(uMosaics);
    sprotoUMosaics.mosaics().at(0).etag() = mosaicEtag();
    sprotoUMosaics.mosaics().at(1).etag() = mosaicEtag();

    http::MockRequest request(http::POST, http::URL(URL_BULK_UPDATE));
    request.body = boost::lexical_cast<std::string>(sprotoUMosaics);
    auto response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 200);

    const auto respMosaics = boost::lexical_cast<smosaics::Mosaics>(response.body);

    db::Mosaics dbMosaics;
    ASSERT_NO_THROW(
        dbMosaics = db::MosaicGateway(*txnHandle())
            .loadByIds({
                db::parseId(respMosaics.mosaics().at(0).id()),
                db::parseId(respMosaics.mosaics().at(1).id())
            });
    );

    int64_t zorder = ZORDER_U;
    for (const auto& respMosaic: respMosaics.mosaics()) {
        EXPECT_EQ(respMosaic.mercatorShift()->x(), SHIFT_U.x());
        EXPECT_EQ(respMosaic.mercatorShift()->y(), SHIFT_U.y());
        EXPECT_EQ(respMosaic.zoomMin(), MIN_ZOOM_U);
        EXPECT_EQ(respMosaic.zoomMax(), MAX_ZOOM_U);
        EXPECT_TRUE(
            geolib3::convertGeodeticToMercator(
                geolib3::sproto::decode(*respMosaic.geometry())
            ) == GEOMETRY_U
        );
        EXPECT_EQ(respMosaic.zIndex(), zorder++);
        EXPECT_EQ(convertFromSproto(
            *respMosaic.colorCorrectionParams()), COLOR_CORRECTION_PARAMS_U);
        EXPECT_EQ(convertFromSproto(
            *respMosaic.sharpingParams()), SHARPING_PARAMS_U);
        EXPECT_EQ(*respMosaic.modifiedBy(), USER_NAME);
        EXPECT_TRUE(respMosaic.modifiedAt());
    }

    zorder = ZORDER_U;
    for (const auto& dbMosaic: dbMosaics) {
        EXPECT_EQ(dbMosaic.zOrder(), zorder++);
        EXPECT_EQ(dbMosaic.minZoom(), MIN_ZOOM_U);
        EXPECT_EQ(dbMosaic.maxZoom(), MAX_ZOOM_U);
        EXPECT_EQ(dbMosaic.shift().x(), SHIFT_U.x());
        EXPECT_EQ(dbMosaic.shift().y(), SHIFT_U.y());
        EXPECT_TRUE(dbMosaic.mercatorGeom() == GEOMETRY_U);
        EXPECT_EQ(*dbMosaic.renderingParams(), RENDERING_PARAMS_U);
        EXPECT_EQ(dbMosaic.modifiedBy(), USER_NAME);
        EXPECT_TRUE(dbMosaic.modifiedAt());
    }
}

TEST_F(MosaicsFixture, test_mosaics_api_zindex_update)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    auto uMosaic = testMosaic();
    updateTestMosaic(uMosaic);
    auto sprotoUMosaic = convertToSproto(uMosaic);
    sprotoUMosaic.etag() = mosaicEtag();

    const int64_t newZorder = 100;
    sql_chemistry::Sequence<db::table::MosaicZorderSeq>(*txnHandle()).setVal(newZorder);

    http::MockRequest request(http::POST, http::URL(URL_UPDATE));
    request.body = boost::lexical_cast<std::string>(sprotoUMosaic);
    auto response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 200);

    const auto respMosaic = boost::lexical_cast<smosaics::Mosaic>(response.body);

    db::Mosaic dbMosaic = createTestMosaic();
    ASSERT_NO_THROW(
        dbMosaic = db::MosaicGateway(*txnHandle())
            .loadById(db::parseId(respMosaic.id()));
    );

    EXPECT_EQ(respMosaic.zIndex(), newZorder + 1);
    EXPECT_EQ(dbMosaic.zOrder(), newZorder + 1);
}

TEST_F(MosaicsFixture, test_mosaics_api_zindex_bulk_update)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    auto uMosaics = testMosaics();
    updateTestMosaic(uMosaics.at(0));
    updateTestMosaic(uMosaics.at(1));
    auto sprotoUMosaics = convertToSproto(uMosaics);
    sprotoUMosaics.mosaics().at(0).etag() = mosaicEtag();
    sprotoUMosaics.mosaics().at(1).etag() = mosaicEtag();

    int64_t newZorder = 100;
    sql_chemistry::Sequence<db::table::MosaicZorderSeq>(*txnHandle()).setVal(newZorder);

    http::MockRequest request(http::POST, http::URL(URL_BULK_UPDATE));
    request.body = boost::lexical_cast<std::string>(sprotoUMosaics);
    auto response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 200);

    const auto respMosaics = boost::lexical_cast<smosaics::Mosaics>(response.body);

    db::Mosaics dbMosaics;
    ASSERT_NO_THROW(
        dbMosaics = db::MosaicGateway(*txnHandle())
            .loadByIds({
                db::parseId(respMosaics.mosaics().at(0).id()),
                db::parseId(respMosaics.mosaics().at(1).id())
            });
    );

    for (size_t i = 0; i != dbMosaics.size(); ++i) {
        const auto curZorder = ++newZorder;
        EXPECT_EQ(respMosaics.mosaics().at(i).zIndex(), curZorder);
        EXPECT_EQ(dbMosaics.at(i).zOrder(), curZorder);
    }
}

TEST_F(MosaicsFixture, test_mosaics_api_update_fail_status_change)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    auto txn = txnHandle();

    auto release = testRelease();
    release.setStatus(db::ReleaseStatus::New);
    db::ReleaseGateway(*txn).update(release);

    release = createTestRelease();
    release.setStatus(db::ReleaseStatus::Ready);
    release.setName("new_test_release");
    db::ReleaseGateway(*txn).insert(release);
    txn->commit();

    {
        auto mosaic = testMosaic();
        mosaic.setReleaseId(release.id());

        http::MockRequest request(http::POST, http::URL(URL_UPDATE));
        request.body = boost::lexical_cast<std::string>(convertToSproto(mosaic));
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(response.status, 409);
    }

    {
        auto mosaic = testMosaic();
        const auto oldReleaseId = *mosaic.releaseId();

        txn = txnHandle();
        mosaic.setReleaseId(release.id());
        db::MosaicGateway(*txn).update(mosaic);
        mosaic.setReleaseId(oldReleaseId);
        txn->commit();

        http::MockRequest request(http::POST, http::URL(URL_UPDATE));
        request.body = boost::lexical_cast<std::string>(convertToSproto(mosaic));
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(response.status, 409);
    }
}

TEST_F(MosaicsFixture, test_mosaics_api_fail_with_empty_mosaic)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    {
        auto smosaicsData =
            convertToSproto<smosaics::MosaicsData>(testMosaics());
        smosaicsData.mosaicsData().at(0).geometry() =
            geolib3::sproto::encode(maps::geolib3::MultiPolygon2{});

        http::MockRequest request(http::POST, http::URL(URL_BULK_CREATE));
        request.body = boost::lexical_cast<std::string>(smosaicsData);
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(response.status, 400);
    }

    {
        auto smosaic = convertToSproto(testMosaic());
        smosaic.geometry() = geolib3::sproto::encode(maps::geolib3::MultiPolygon2{});

        http::MockRequest request(http::POST, http::URL(URL_UPDATE));
        request.body = boost::lexical_cast<std::string>(smosaic);
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(response.status, 400);
    }

    {
        auto smosaics = convertToSproto(testMosaics());
        smosaics.mosaics().at(0).geometry() =
            geolib3::sproto::encode(maps::geolib3::MultiPolygon2{});

        http::MockRequest request(http::POST, http::URL(URL_BULK_UPDATE));
        request.body = boost::lexical_cast<std::string>(smosaics);
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(response.status, 400);
    }
}

TEST_F(MosaicsFixture, test_mosaics_api_delete)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    http::MockRequest request(
        http::DELETE,
        http::URL(URL_DELETE)
            .addParam("id", testMosaic().id())
    );
    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(response.status, 200);

    EXPECT_THROW(
        db::MosaicGateway(*txnHandle()).loadById(
            testMosaic().id()),
        maps::sql_chemistry::ObjectNotFound
    );
}

TEST_F(MosaicsFixture, test_mosaics_api_bulk_delete)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    http::MockRequest request(
        http::DELETE,
        http::URL(URL_BULK_DELETE)
            .addParam("ids", std::to_string(testMosaics().at(0).id()) + ',' +
                             std::to_string(testMosaics().at(1).id()))
    );
    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(response.status, 200);

    EXPECT_TRUE(
        db::MosaicGateway(*txnHandle()).loadByIds(
            {testMosaics().at(0).id(), testMosaics().at(1).id()}).empty()
    );
}

TEST_F(MosaicsFixture, test_mosaics_api_delete_wrong_status)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    auto txn = txnHandle();
    auto release = testRelease();
    release.setStatus(db::ReleaseStatus::Ready);
    db::ReleaseGateway(*txn).update(release);
    txn->commit();

    {
        http::MockRequest request(
            http::DELETE,
            http::URL(URL_DELETE)
                .addParam("id", testMosaic().id())
        );
        auto response = yacare::performTestRequest(request);
        ASSERT_EQ(response.status, 409);
    }
    {
        http::MockRequest request(
            http::DELETE,
            http::URL(URL_BULK_DELETE)
                .addParam("ids", std::to_string(testMosaics().at(0).id()) + ',' +
                                 std::to_string(testMosaics().at(1).id()))
        );
        auto response = yacare::performTestRequest(request);
        ASSERT_EQ(response.status, 409);
    }
}

TEST_F(MosaicsFixture, test_mosaics_api_not_found)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));
    {
        http::MockRequest request(http::POST, http::URL(URL_UPDATE));
        auto uMosaic = convertToSproto(testMosaic());
        uMosaic.id() = "1111";
        request.body = boost::lexical_cast<std::string>(uMosaic);
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

TEST_F(MosaicsFixture, test_mosaics_api_etag_error)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    auto mosaic = testMosaic();
    const auto sprotoMosaic = convertToSproto(mosaic);
    updateTestMosaic(mosaic);
    auto txn = txnHandle();
    db::MosaicGateway(*txn).update(mosaic);
    txn->commit();

    http::MockRequest request(http::POST, http::URL(URL_UPDATE));
    request.body = boost::lexical_cast<std::string>(sprotoMosaic);
    auto response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 409);

    const auto sprotoMosaics = convertToSproto(testMosaics());
    http::MockRequest bulkRequest(http::POST, http::URL(URL_BULK_UPDATE));
    bulkRequest.body = boost::lexical_cast<std::string>(sprotoMosaics);
    response = yacare::performTestRequest(bulkRequest);
    EXPECT_EQ(response.status, 409);
}

/*
TEST_F(MosaicsFixture, test_mosaics_api_no_authorization)
{
    const auto login = "john";
    auth::UserInfo userInfo{};
    userInfo.setLogin(login);
    yacare::tests::UserInfoFixture userInfoFixture(userInfo);

    http::MockRequest request(http::POST, http::URL(URL_BULK_CREATE));
    auto response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 403);

    request = http::MockRequest(http::POST, http::URL(URL_UPDATE));
    response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 403);

    request = http::MockRequest(http::POST, http::URL(URL_BULK_UPDATE));
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
        http::URL(URL_SEARCH + "?ll=1,1")
    );
    response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 403);

    request = http::MockRequest(
        http::DELETE,
        http::URL(URL_DELETE).addParam("id", "1")
    );
    response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 403);

    request = http::MockRequest(
        http::DELETE,
        http::URL(URL_BULK_DELETE).addParam("ids", "1")
    );
    response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 403);
}

TEST_F(MosaicsFixture, test_mosaics_api_no_edit_access)
{
    std::string login = "john";
    auto txn = txnHandle();
    idm::IdmService(*txn, login)
        .addRole(idm::parseSlugPath("project/mapsfactory/role/viewer"));
    txn->commit();

    auth::UserInfo userInfo{};
    userInfo.setLogin(login);
    yacare::tests::UserInfoFixture userInfoFixture(userInfo);

    http::MockRequest request(http::POST, http::URL(URL_BULK_CREATE));
    auto response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 403);

    request = http::MockRequest(http::POST, http::URL(URL_UPDATE));
    response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 403);

    request = http::MockRequest(http::POST, http::URL(URL_BULK_UPDATE));
    response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 403);

    request = http::MockRequest(
        http::DELETE,
        http::URL(URL_DELETE).addParam("id", "1")
    );
    response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 403);

    request = http::MockRequest(
        http::DELETE,
        http::URL(URL_BULK_DELETE).addParam("ids", "1")
    );
    response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 403);
}
*/

} // maps::factory::backend::tests
