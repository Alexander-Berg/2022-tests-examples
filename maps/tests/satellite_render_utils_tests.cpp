#include <library/cpp/testing/common/env.h>
#include <library/cpp/testing/gtest/gtest.h>

#include "fixture.h"
#include "test_data.h"

#include <maps/factory/libs/unittest/tests_common.h>
#include <maps/factory/libs/common/s3_keys.h>
#include <maps/factory/libs/db/dem_release_gateway.h>
#include <maps/factory/libs/db/dem_patch_gateway.h>
#include <maps/factory/libs/db/mosaic_gateway.h>
#include <maps/factory/libs/db/mosaic_source_gateway.h>
#include <maps/factory/libs/db/dem_release_gateway.h>
#include <maps/factory/libs/db/dem_patch_gateway.h>
#include <maps/factory/libs/geometry/geolib.h>
#include <maps/factory/libs/dataset/tiles.h>
#include <maps/factory/libs/rendering/mosaic_parameters.h>
#include <maps/factory/libs/rendering/impl/patched_dem_tile_dataset.h>
#include <maps/factory/libs/unittest/tests_common.h>
#include <maps/factory/services/renderer/lib/satellite_render_utils.h>
#include <maps/factory/services/sputnica_back/lib/filter.h>

#include <maps/libs/geolib/include/test_tools/comparison.h>
#include <maps/libs/geolib/include/test_tools/io_operations.h>
#include <maps/libs/log8/include/log8.h>
#include <maps/libs/tile/include/geometry.h>
#include <maps/libs/introspection/include/stream_output.h>

#include <sstream>

namespace maps::factory::db {

using introspection::operator==;
using introspection::operator!=;
using introspection::operator<<;

} // namespace maps::factory::db


namespace maps::factory::renderer::tests {
namespace {

constexpr double GEOM_EPS = 1e-6;

const json::Value RENDERING_PARAMS{
    {"hue", json::Value{90}},
    {"saturation", json::Value{50}},
    {"lightness", json::Value{50}},
    {"sigma", json::Value{0.5}},
    {"radius", json::Value{0.9}}
};

class PrepareMosaicsFixture : public RendererFixture {
public:
    static constexpr int64_t DEM_RELEASE_1_ISSUE_ID = 1;

    PrepareMosaicsFixture()
        : baseMosaicMercatorGeom_{geolib3::convertGeodeticToMercator(
        geolib3::MultiPolygon2({geolib3::Polygon2(geolib3::PointsVector{
            {37.3438, 55.9119},
            {37.4591, 55.9134},
            {37.4564, 55.8466},
            {37.3493, 55.8435}})}))}
        , testTile_{618, 320, 10}
        , emptyTile_{619, 320, 10}
        , nextGeomIdx_{}
    {
        insertData();
    }

    const tile::Tile& testTile() const { return testTile_; }
    const tile::Tile& emptyTile() const { return emptyTile_; }
    const std::vector<db::MosaicSource>& mosaicSources() const { return mosaicSources_; }
    const std::vector<db::Mosaic>& mosaics() const { return mosaics_; }

private:
    geolib3::Vector2 makeNextMercatorShift()
    {
        const double pxShiftBetweenGeoms{10};
        const int geomsInRow = 15;
        const double resolution = tile::zoomToResolution(testTile_.z());
        const double shiftX = (nextGeomIdx_ % geomsInRow) * pxShiftBetweenGeoms * resolution;
        const double shiftY = -(nextGeomIdx_ / geomsInRow) * pxShiftBetweenGeoms * resolution;
        ++nextGeomIdx_;
        return {shiftX, shiftY};
    }

    geolib3::MultiPolygon2 makeNextTestMercatorGeom(const geolib3::Vector2& shift)
    {
        geolib3::SimpleGeometryTransform2 transform(
            geolib3::AffineTransform2::translate(shift.x(), shift.y()));
        auto result = transform(baseMosaicMercatorGeom_, geolib3::TransformDirection::Forward);
        return result;
    }

    void insertData()
    {
        auto txn = txnHandle();
        auto release = db::Release("draft_release").setStatus(db::ReleaseStatus::New);
        db::ReleaseGateway(*txn).insert(release);

        auto delivery = db::Delivery(std::nullopt, "2020-01-01", "delivery_2020", "subfolder1");
        db::DeliveryGateway(*txn).insert(delivery);

        const auto addMs = [&](const std::string& n) {
            db::MosaicSource ms(n);
            ms.setStatus(db::MosaicSourceStatus::Ready);
            ms.setDeliveryId(delivery.id());
            auto shift = makeNextMercatorShift();
            ms.setMercatorGeom(makeNextTestMercatorGeom(shift));
            ms.setCogPath("TEST_COG_PATH");
            mosaicSources_.push_back(ms);
        };

        mosaicSources_.clear();
        addMs("first");
        addMs("second");
        db::MosaicSourceGateway(*txn).insert(mosaicSources_);

        int zorder = 1;
        const int minZoom = 10;
        const int maxZoom = 19;
        const auto addM = [&]() {
            auto shift = makeNextMercatorShift();
            auto m = db::Mosaic(mosaicSources_.at(0).id(), ++zorder, minZoom, maxZoom,
                makeNextTestMercatorGeom(shift));
            m.setReleaseId(release.id());
            m.setShift(shift);
            m.setRenderingParams(RENDERING_PARAMS);
            mosaics_.push_back(m);
        };
        mosaics_.clear();
        addM();
        addM();
        db::MosaicGateway(*txn).insert(mosaics_);

        auto demRelease = db::DemRelease{"dem_release_1"}
            .setStatus(db::DemReleaseStatus::Production)
            .setIssueId(DEM_RELEASE_1_ISSUE_ID);
        db::DemReleaseGateway(*txn).insert(demRelease);

        db::DemPatch demPatch{"patch_1", demRelease.id()};
        demPatch.setBbox(dataset::toBox2d(baseMosaicMercatorGeom_.boundingBox()));
        db::DemPatchGateway(*txn).insert(demPatch);

        txn->commit();
    }

    const geolib3::MultiPolygon2 baseMosaicMercatorGeom_;
    const tile::Tile testTile_;
    const tile::Tile emptyTile_;
    size_t nextGeomIdx_;
    std::vector<db::MosaicSource> mosaicSources_;
    std::vector<db::Mosaic> mosaics_;
};

bool correspondsTo(const rendering::MosaicInfo& bi, const db::MosaicSource& ms)
{
    return bi.cogPath == ms.cogPath() &&
           bi.shiftMeters.isApprox(Eigen::Vector2d{0, 0}, GEOM_EPS) &&
           bi.geometryShiftMeters.isApprox(Eigen::Vector2d{0, 0}, GEOM_EPS) &&
           geolib3::test_tools::approximateEqual(
               bi.mercatorGeom, ms.mercatorGeom(), GEOM_EPS);
}

bool correspondsTo(const rendering::MosaicInfo& bi, const db::Mosaic& m)
{
    return
        bi.shiftMeters.isApprox(
            Eigen::Vector2d{m.shift().x(), m.shift().y()}, GEOM_EPS) &&
        bi.geometryShiftMeters.isApprox(Eigen::Vector2d{0, 0}, GEOM_EPS) &&
        geolib3::test_tools::approximateEqual(
            bi.mercatorGeom, m.mercatorGeom(), GEOM_EPS) &&
        bi.parameters == rendering::parametersForMosaic(m);
}

MATCHER_P(MosaicInfoEq, val, std::string(negation ? "not " : ""))
{
    return correspondsTo(arg, val);
}

MATCHER(MosaicInfoEq, "")
{
    return correspondsTo(std::get<0>(arg), std::get<1>(arg));
}

} // namespace

TEST_F(PrepareMosaicsFixture, loadMosaicInfos_should_properly_fill_mosaic_info)
{
    auto txn = txnHandle();

    const auto& ms = mosaicSources().at(0);
    const auto& m = mosaics().at(0);
    const auto loadedMosaicInfos = loadMosaicInfos(
        RenderRequest{
            .tile = testTile(),
            .mosaicSourceIds = {ms.id()},
            .mosaicIds = {m.id()}},
        *txn);
    ASSERT_EQ(loadedMosaicInfos.size(), 2u);

    const auto& bi0 = loadedMosaicInfos[0];
    const auto& bi1 = loadedMosaicInfos[1];

    // In first place should be info created from mosaic
    EXPECT_EQ(bi1.cogPath, ms.cogPath());
    EXPECT_EQ(bi0.cogPath, ms.cogPath());

    EXPECT_THAT(bi1.shiftMeters,
        factory::tests::EigEq(Eigen::Vector2d(m.shift().x(), m.shift().y()), GEOM_EPS));
    EXPECT_THAT(bi1.geometryShiftMeters, factory::tests::EigEq(Eigen::Vector2d{0, 0}, GEOM_EPS));
    EXPECT_THAT(bi0.shiftMeters, factory::tests::EigEq(Eigen::Vector2d{0, 0}, GEOM_EPS));
    EXPECT_THAT(bi0.geometryShiftMeters, factory::tests::EigEq(Eigen::Vector2d{0, 0}, GEOM_EPS));

    EXPECT_EQ(bi1.minZoom, m.minZoom());
    EXPECT_EQ(bi1.maxZoom, m.maxZoom());
    EXPECT_EQ(bi0.minZoom, 0u);
    EXPECT_EQ(bi0.maxZoom, static_cast<uint16_t>(geometry::MAX_SAFE_ZOOM));

    EXPECT_EQ(rendering::PatchedDemName::fromString(*bi1.demPath).issueId(), DEM_RELEASE_1_ISSUE_ID);
    EXPECT_EQ(rendering::PatchedDemName::fromString(*bi0.demPath).issueId(), DEM_RELEASE_1_ISSUE_ID);

    EXPECT_THAT(bi1.mercatorGeom, factory::tests::GeoEq(m.mercatorGeom(), GEOM_EPS));
    EXPECT_THAT(bi0.mercatorGeom, factory::tests::GeoEq(ms.mercatorGeom(), GEOM_EPS));

    EXPECT_EQ(bi1.parameters, rendering::parametersForMosaic(m));
    EXPECT_EQ(bi0.parameters, rendering::RenderParameters{});
}

TEST_F(PrepareMosaicsFixture, loadMosaicInfos_should_filter_by_tile)
{
    auto txn = txnHandle();
    const auto& ms = mosaicSources().at(0);
    {
        const auto loadedMosaicInfos = loadMosaicInfos(
            RenderRequest{.tile = testTile(), .mosaicSourceIds = {ms.id()}}, *txn);
        ASSERT_EQ(loadedMosaicInfos.size(), 1u);
        EXPECT_THAT(loadedMosaicInfos[0], MosaicInfoEq(ms));
    }

    {
        const auto loadedMosaicInfos = loadMosaicInfos(
            RenderRequest{.tile = emptyTile(), .mosaicSourceIds = {ms.id()}}, *txn);
        ASSERT_EQ(loadedMosaicInfos.size(), 0u);
    }
}

TEST_F(PrepareMosaicsFixture, loadMosaicInfos_should_implement_basic_filtering)
{
    auto txn = txnHandle();

    EXPECT_THAT(
        loadMosaicInfos(
            RenderRequest{
                .tile = testTile(),
                .nodePath = navigation::NodePath{"releases"}},
            *txn),
        ::testing::Pointwise(MosaicInfoEq(), {mosaics().at(1), mosaics().at(0)}));

    EXPECT_THAT(
        loadMosaicInfos(
            RenderRequest{
                .tile = testTile(),
                .nodePath = navigation::NodePath{"deliveries"}},
            *txn),
        ::testing::Pointwise(MosaicInfoEq(), {mosaicSources().at(1), mosaicSources().at(0)}));

    EXPECT_THAT(
        loadMosaicInfos(
            RenderRequest{
                .tile = testTile(),
                .filter = sputnica::Filter::fromString("status:ready")},
            *txn),
        ::testing::Pointwise(MosaicInfoEq(), {mosaicSources().at(1), mosaicSources().at(0)}));

    EXPECT_THAT(
        loadMosaicInfos(
            RenderRequest{
                .tile = testTile(),
                .mosaicSourceIds = {mosaicSources().at(0).id()}},
            *txn),
        ::testing::Pointwise(MosaicInfoEq(), {mosaicSources().at(0)}));

    EXPECT_THAT(
        loadMosaicInfos(
            RenderRequest{
                .tile = testTile(),
                .mosaicIds = {mosaics().at(0).id()}},
            *txn),
        ::testing::Pointwise(MosaicInfoEq(), {mosaics().at(0)}));

    auto loadedMosaicInfos = loadMosaicInfos(
        RenderRequest{
            .tile = testTile(),
            .nodePath = navigation::NodePath{"releases"},
            .filter = sputnica::Filter::fromString("status:ready"),
            .mosaicSourceIds = {mosaicSources().at(0).id()},
            .mosaicIds = {mosaics().at(0).id()}},
        *txn);
    ASSERT_EQ(loadedMosaicInfos.size(), 4u);
    EXPECT_THAT(loadedMosaicInfos[0], MosaicInfoEq(mosaicSources().at(1)));
    EXPECT_THAT(loadedMosaicInfos[1], MosaicInfoEq(mosaicSources().at(0)));
    EXPECT_THAT(loadedMosaicInfos[2], MosaicInfoEq(mosaics().at(1)));
    EXPECT_THAT(loadedMosaicInfos[3], MosaicInfoEq(mosaics().at(0)));
}

TEST_F(PrepareMosaicsFixture, loadMosaicInfos_should_support_zindex_up)
{
    auto txn = txnHandle();
    EXPECT_THAT(
        loadMosaicInfos(
            RenderRequest{
                .tile = testTile(),
                .nodePath = navigation::NodePath{"releases"},
                .mosaicIds = {mosaics().at(0).id()},
                .renderMosaicsOnTop = true},
            *txn),
        ::testing::Pointwise(MosaicInfoEq(), {mosaics().at(0), mosaics().at(1)}));

    EXPECT_THAT(
        loadMosaicInfos(
            RenderRequest{
                .tile = testTile(),
                .nodePath = navigation::NodePath{"releases"},
                .mosaicIds = {mosaics().at(0).id()}},
            *txn),
        ::testing::Pointwise(MosaicInfoEq(), {mosaics().at(1), mosaics().at(0)}));
}

TEST_F(PrepareMosaicsFixture, loadMosaicInfos_should_support_shift)
{
    auto txn = txnHandle();
    const Eigen::Vector2d shiftMeters{10, 20};

    auto loadedMosaicInfos =
        loadMosaicInfos(
            RenderRequest{
                .tile = testTile(),
                .nodePath = navigation::NodePath{"releases"},
                .mosaicIds = {mosaics().at(0).id()},
                .shiftMeters = shiftMeters},
            *txn);
    ASSERT_EQ(loadedMosaicInfos.size(), 2u);
    EXPECT_THAT(loadedMosaicInfos.at(0), MosaicInfoEq(mosaics().at(1)));

    auto bi1 = loadedMosaicInfos.at(1);
    const auto& m = mosaics().at(0);
    EXPECT_THAT(bi1.shiftMeters,
        factory::tests::EigEq(
            Eigen::Vector2d(m.shift().x(), m.shift().y()) + shiftMeters,
            GEOM_EPS));
    EXPECT_THAT(bi1.geometryShiftMeters, factory::tests::EigEq(shiftMeters, GEOM_EPS));

    //apart from geometry shift everything else must be equal to original mosaic values
    bi1.geometryShiftMeters = Eigen::Vector2d{0, 0};
    bi1.shiftMeters = Eigen::Vector2d(m.shift().x(), m.shift().y());
    EXPECT_THAT(bi1, MosaicInfoEq(m));
}

TEST_F(PrepareMosaicsFixture, loadMosaicInfos_should_consider_shift_in_geometry_filter)
{
    auto txn = txnHandle();
    const Eigen::Vector2d shiftMeters{50000, 0};

    auto loadedMosaicInfos =
        loadMosaicInfos(
            RenderRequest{
                .tile = testTile(),
                .nodePath = navigation::NodePath{"releases"},
                .mosaicIds = {mosaics().at(0).id()},
                .shiftMeters = shiftMeters},
            *txn);
    ASSERT_EQ(loadedMosaicInfos.size(), 1u);
    EXPECT_THAT(loadedMosaicInfos.at(0), MosaicInfoEq(mosaics().at(1)));

    loadedMosaicInfos =
        loadMosaicInfos(
            RenderRequest{
                .tile = emptyTile(),
                .nodePath = navigation::NodePath{"releases"},
                .mosaicIds = {mosaics().at(0).id()},
                .shiftMeters = shiftMeters},
            *txn);
    ASSERT_EQ(loadedMosaicInfos.size(), 1u);

    auto bi = loadedMosaicInfos.at(0);
    const auto& m = mosaics().at(0);
    //apart from geometry shift everything else must be equal to original mosaic values
    bi.geometryShiftMeters = Eigen::Vector2d{0, 0};
    bi.shiftMeters = Eigen::Vector2d(m.shift().x(), m.shift().y());

    EXPECT_THAT(bi, MosaicInfoEq(m));
}

TEST_F(PrepareMosaicsFixture, loadMosaicInfos_should_support_rendering_params)
{
    auto txn = txnHandle();
    const Eigen::Vector2d shiftMeters{10, 20};

    auto renderingParamsOverride = rendering::RenderParameters();
    renderingParamsOverride.add("hue", -1);

    auto loadedMosaicInfos =
        loadMosaicInfos(
            RenderRequest{
                .tile = testTile(),
                .nodePath = navigation::NodePath{"releases"},
                .mosaicIds = {mosaics().at(0).id()},
                .renderingParameters = renderingParamsOverride},
            *txn);
    ASSERT_EQ(loadedMosaicInfos.size(), 2u);
    EXPECT_THAT(loadedMosaicInfos.at(0), MosaicInfoEq(mosaics().at(1)));

    auto bi1 = loadedMosaicInfos.at(1);
    const auto& m = mosaics().at(0);
    auto renderingParams = rendering::parametersForMosaic(m);
    renderingParams.override(renderingParamsOverride);
    EXPECT_TRUE(bi1.parameters == renderingParams);

    //apart from rendering params everything else must be equal to original mosaic values
    bi1.parameters = rendering::parametersForMosaic(m);
    EXPECT_THAT(bi1, MosaicInfoEq(m));
}

} // namespace maps::factory::renderer::tests
