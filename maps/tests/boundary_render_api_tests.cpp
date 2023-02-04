#include <library/cpp/testing/common/env.h>
#include <library/cpp/testing/gtest/gtest.h>
#include "fixture.h"

#include <maps/factory/libs/db/aoi_gateway.h>
#include <maps/factory/libs/db/delivery_gateway.h>
#include <maps/factory/libs/db/dem_patch_gateway.h>
#include <maps/factory/libs/db/dem_release_gateway.h>
#include <maps/factory/libs/db/mosaic_gateway.h>
#include <maps/factory/libs/db/mosaic_source_gateway.h>
#include <maps/factory/libs/db/order_gateway.h>
#include <maps/factory/libs/db/project_gateway.h>
#include <maps/factory/libs/db/release_gateway.h>
#include <maps/factory/libs/db/source_gateway.h>
#include <maps/factory/libs/db/secret_object_gateway.h>
#include <maps/factory/libs/geometry/geolib.h>
#include <maps/factory/libs/services/yacare.h>

#include <yandex/maps/proto/factory/navigation.sproto.h>
#include <yandex/maps/proto/factory/rendering.sproto.h>
#include <yandex/maps/proto/renderer/vmap2/tile.pb.h>

#include <maps/libs/img/include/raster.h>
#include <maps/libs/tile/include/tile.h>
#include <maps/libs/tile/include/utils.h>
#include <maps/libs/common/include/file_utils.h>
#include <maps/libs/geolib/include/conversion.h>
#include <maps/libs/geolib/include/transform.h>

#include <boost/lexical_cast.hpp>

#include <vector>

namespace maps::factory::renderer::tests {
using namespace image;
using namespace geometry;
using namespace testing;
using namespace services;

namespace {

const std::string EMPTY_HOTSPOT_RESPONSE =
    R"({"error":{"code":404,"message":"Not found"}})";

using TileV2 = yandex::maps::proto::renderer::vmap2::Tile;
namespace snavigation = yandex::maps::sproto::factory::navigation;

struct LayerInfo {
    std::string id;
    std::string name;
};

class RenderingFixture : public RendererFixture {
public:
    RenderingFixture()
        : baseMosaicMercatorGeom_{geolib3::convertGeodeticToMercator(
        geolib3::MultiPolygon2({geolib3::Polygon2(
            geolib3::PointsVector{{37.3438, 55.9119},
                {37.4591, 55.9134},
                {37.4564, 55.8466},
                {37.3493, 55.8435}})}))}
        , testTile_{309, 160, 9}
        , nextGeomIdx_{}
        , pgpool_(
            postgres().connectionString(),
            pgpool3::PoolConstants(1, 5, 1, 5))
    {
        insertData();
    }

    pgpool3::TransactionHandle txnHandle()
    {
        return pgpool_.masterWriteableTransaction();
    }

    const tile::Tile testTile() const { return testTile_; }

    geolib3::MultiPolygon2 makeNextTestMercatorGeom()
    {
        const double pxShiftBetweenGeoms{10};
        const int geomsInRow = 15;
        const double resolution = tile::zoomToResolution(testTile_.z());
        const double shiftX = (nextGeomIdx_ % geomsInRow) * pxShiftBetweenGeoms * resolution;
        const double shiftY = (nextGeomIdx_ / geomsInRow) * pxShiftBetweenGeoms * resolution;
        geolib3::SimpleGeometryTransform2 transform(
            geolib3::AffineTransform2::translate(shiftX, -shiftY));
        auto result = transform(baseMosaicMercatorGeom_, geolib3::TransformDirection::Forward);
        ++nextGeomIdx_;
        return result;
    }

    void insertData()
    {
        auto txn = txnHandle();
        releases = std::vector<db::Release>{
            db::Release("draft_release").setStatus(db::ReleaseStatus::New),
            db::Release("ready_release").setStatus(db::ReleaseStatus::Ready),
            db::Release("testing_release").setStatus(db::ReleaseStatus::Testing),
            db::Release("production_release").setStatus(db::ReleaseStatus::Production)
        };
        db::ReleaseGateway(*txn).insert(releases);

        auto source = db::Source("source_name", db::SourceType::Local, "path");
        db::SourceGateway(*txn).insert(source);

        deliveries = std::vector<db::Delivery>{
            db::Delivery(source.id(), "2020-01-01", "delivery_2020", "subfolder1").setYear(2020),
            db::Delivery(source.id(), "2019-01-01", "delivery_2019", "subfolder2").setYear(2019)
        };
        db::DeliveryGateway(*txn).insert(deliveries);

        projects = std::vector<db::Project>{{"Moscow"}, {"Nino"}};
        db::ProjectGateway(*txn).insert(projects);

        const auto addMs = [&](const std::string& n, db::Id proj = -1) {
            db::MosaicSource ms(n);
            ms.setStatus(db::MosaicSourceStatus::Ready);
            ms.setDeliveryId(deliveries.at(0).id());
            ms.setMercatorGeom(makeNextTestMercatorGeom());
            if (proj >= 0) { ms.setProjectId(projects.at(proj).id()); }
            mosaicSources.push_back(ms);
        };
        mosaicSources.clear();
        addMs("unassigned", 1);
        addMs("draft", 0);
        addMs("ready");
        addMs("testing");
        addMs("production");

        orders.push_back(db::Order(2020, db::OrderType::Tasking));
        db::OrderGateway(*txn).insert(orders);

        aois.emplace_back(orders.at(0).id(), "1", makeNextTestMercatorGeom().polygonAt(0));
        aois.emplace_back(orders.at(0).id(), "2", makeNextTestMercatorGeom().polygonAt(0));
        db::AoiGateway(*txn).insert(aois);

        const auto addSputnicaMs =
            [&](db::MosaicSourceStatus status, int64_t aoiId) {
                db::MosaicSource ms(std::string(toString(status)));
                ms.setStatus(status);
                ms.setOrderId(orders.at(0).id());
                ms.setAoiId(aoiId);
                ms.setMercatorGeom(makeNextTestMercatorGeom());
                mosaicSources.push_back(ms);
            };

        addSputnicaMs(db::MosaicSourceStatus::New, aois.at(0).id());
        addSputnicaMs(db::MosaicSourceStatus::ExtendBoundary, aois.at(0).id());
        addSputnicaMs(db::MosaicSourceStatus::ExtendBoundaryDeclined, aois.at(0).id());
        addSputnicaMs(db::MosaicSourceStatus::ExtendBoundaryApproved, aois.at(0).id());
        addSputnicaMs(db::MosaicSourceStatus::Fixed, aois.at(0).id());
        addSputnicaMs(db::MosaicSourceStatus::Rejected, aois.at(1).id());
        addSputnicaMs(db::MosaicSourceStatus::RejectedDeclined, aois.at(1).id());
        addSputnicaMs(db::MosaicSourceStatus::RejectedApproved, aois.at(1).id());

        INFO() << "### ms.orderId " << mosaicSources.back().orderId().value();

        db::MosaicSourceGateway(*txn).insert(mosaicSources);

        int zorder = 1;
        const int minZoom = 10;
        const int maxZoom = 19;
        const auto addM = [&](db::Id ms, db::Id rel, unsigned maxZoom) {
            db::Mosaic m(mosaicSources.at(ms).id(), ++zorder, minZoom, maxZoom, makeNextTestMercatorGeom());
            m.setReleaseId(releases.at(rel).id());
            mosaics.push_back(m);
        };
        mosaics.clear();
        addM(1, 0, 17);
        mosaics.back().setShift({1., 1.});
        addM(1, 0, 18);
        addM(1, 0, 19);
        addM(1, 0, 20);
        addM(1, 0, 21);
        addM(1, 0, 22);
        addM(2, 1, maxZoom);
        addM(3, 2, maxZoom);
        addM(4, 3, maxZoom);
        db::MosaicGateway(*txn).insert(mosaics);

        auto object = db::SecretObject("secret", makeNextTestMercatorGeom());
        db::SecretObjectGateway(*txn).insert(object);

        db::MosaicSource newMs("unassigned_new");
        newMs.setStatus(db::MosaicSourceStatus::New);
        newMs.setDeliveryId(deliveries.at(0).id());
        newMs.setProjectId(projects[1].id());
        newMs.setMercatorGeom(makeNextTestMercatorGeom());
        db::MosaicSourceGateway(*txn).insert(newMs);

        auto demReleases = db::DemReleases{{"dem_release_1"}, {"dem_release_2"}};
        db::DemReleaseGateway(*txn).insert(demReleases);

        auto demPatches = db::DemPatches{
            db::DemPatch("patch_1", demReleases[0].id())
                .setBbox(toBox2d(makeNextTestMercatorGeom().boundingBox())),
            db::DemPatch("patch_2", demReleases[1].id())
                .setBbox(toBox2d(makeNextTestMercatorGeom().boundingBox())),
            db::DemPatch("patch_3", demReleases[1].id())
                .setBbox(toBox2d(makeNextTestMercatorGeom().boundingBox()))
        };
        db::DemPatchGateway(*txn).insert(demPatches);

        txn->commit();
    }

    const geolib3::MultiPolygon2 baseMosaicMercatorGeom_;
    const tile::Tile testTile_;
    size_t nextGeomIdx_;
    std::vector<db::Release> releases;
    std::vector<db::Delivery> deliveries;
    std::vector<db::Project> projects;
    std::vector<db::MosaicSource> mosaicSources;
    std::vector<db::Mosaic> mosaics;
    std::vector<db::Order> orders;
    std::vector<db::Aoi> aois;
    pgpool3::Pool pgpool_;
};
} // namespace

namespace srendering = yandex::maps::sproto::factory::rendering;

MATCHER(LayerInfoEqual, "")
{
    const srendering::LayerInfo& sinfo = std::get<0>(arg);
    const LayerInfo& info = std::get<1>(arg);
    return
        testing::ExplainMatchResult(
            testing::Eq(sinfo.id()), info.id, result_listener) &&
        testing::ExplainMatchResult(
            testing::Eq(sinfo.name()), info.name, result_listener);
}

TEST_F(RenderingFixture, test_list_boundary_layers)
{
    std::vector<LayerInfo> expectedData{
        {"production", "production"},
        {"new", "new"},
        {"ready", "ready"},
        {"unassigned", "unassigned"},
        {"zoom", "zoom"},
        {"tree_node_objects", "tree_node_objects"},
        {"masking_zones", "masking_zones"},
        {"dem_patches", "dem_patches"}
    };

    http::MockRequest rq(
        http::GET,
        http::URL("http://localhost/v1/rendering/list_boundary_layers")
    );
    auto resp = yacare::performTestRequest(rq);
    ASSERT_EQ(resp.status, 200);

    auto sprotoResponse = boost::lexical_cast<srendering::LayerInfos>(resp.body);
    EXPECT_THAT(sprotoResponse.layerInfos(),
        UnorderedPointwise(LayerInfoEqual(), expectedData))
                    << "response = " << textDump(sprotoResponse);

}

TEST_F(RenderingFixture, test_render_boundary_layers)
{
    auto loadEtalonImage =
        [](const std::string& name) {
            std::stringstream fileName;
            fileName << "data/render_boundary_layers/" << name << ".png";
            return UInt8Image::fromFile(SRC_(fileName.str()));
        };

    struct TestData {
        std::string nodeId;
        std::string layers;
        std::optional<std::string> etalonImageName = {};
        std::optional<std::string> releaseId = {};
        std::optional<std::string> patchIds = {};
        std::optional<std::string> sputnicaFilter = {};
    };

    const std::string nodeDependantLayers = "tree_node_objects,zoom";
    const std::string constLayers = "production,new,ready,unassigned";
    const std::string allLayers = constLayers + "," + nodeDependantLayers;

    std::vector<TestData> testData{
        {"", nodeDependantLayers},
        {"", constLayers, "const_layers"},
        {"", "production", "production"},
        {"", "new", "new"},
        {"", "ready", "ready"},
        {"", "unassigned", "unassigned"},
        {"releases", "tree_node_objects", "releases"},
        {"releases/new/" + std::to_string(releases[0].id()), nodeDependantLayers, "releases_new"},
        {"releases/new/" + std::to_string(releases[0].id()), "tree_node_objects",
            "tree_node_objects_releases_new"},
        {"releases/new/" + std::to_string(releases[0].id()), "zoom", "zoom_releases_new"},
        {"releases/ready/" + std::to_string(releases[1].id()), nodeDependantLayers, "releases_ready"},
        {"releases/testing/" + std::to_string(releases[2].id()), nodeDependantLayers, "releases_testing"},
        {"releases/production/" + std::to_string(releases[3].id()), nodeDependantLayers,
            "releases_production"},
        {"deliveries/2020/" + std::to_string(deliveries[0].id()), nodeDependantLayers, "deliveries"},
        {"", "masking_zones", "masking_zones"},
        {"", "dem_patches", "dem_patches"},
        {"", "dem_patches", "dem_tile_release_2", "2"},
        {"", "dem_patches", "dem_tile_patch_1_2", {}, "1,2"},
        {"", "aois", "aois", {}, {}, "order:" + std::to_string(orders.at(0).id())},
        {"", "mosaic_sources", "mosaic_sources", {}, {}, "order:" + std::to_string(orders.at(0).id())},
        {"", "mosaic_sources", "mosaic_sources_aoi", {}, {}, "order:1*aoi:" + std::to_string(aois.at(0).id())}
    };

    INFO() << "Working dir is " << GetWorkPath();

    for (const auto&[nodeId, layers, etalonName, releaseId, patchIds, sputnicaFilter]: testData) {
        SCOPED_TRACE(nodeId);
        auto url = http::URL("http://localhost/v1/rendering/render_boundary_layers")
            .addParam("node_id", nodeId)
            .addParam("l", layers)
            .addParam("x", testTile().x())
            .addParam("y", testTile().y())
            .addParam("z", testTile().z());

        if (releaseId) {
            url.addParam("dem_release_id", releaseId.value());
        }
        if (patchIds) {
            url.addParam("dem_patch_ids", patchIds.value());
        }
        if (sputnicaFilter.has_value()) {
            url.addParam("filter", sputnicaFilter.value());
        }
        http::MockRequest rq(http::GET, url);
        auto resp = yacare::performTestRequest(rq);

        if (!etalonName.has_value()) {
            EXPECT_EQ(resp.status, 204) << "for node_id=" << nodeId << ", layers=" << layers;
            continue;
        }

        EXPECT_EQ(resp.status, 200) << "for node_id=" << nodeId << ", layers=" << layers;
        EXPECT_EQ(resp.headers.at("Content-Type"), "image/png")
                        << "for node_id=" << nodeId << ", layers=" << layers;

        ::maps::common::writeFile(etalonName.value() + ".png", resp.body);

        auto receivedImage = UInt8Image::fromBuffer(resp.body);
        auto etalonImage = loadEtalonImage(etalonName.value());

        EXPECT_EQ(
            receivedImage.maxAbsDifference(etalonImage), 0u)
                        << "for node_id=" << nodeId << ", layers=" << layers;
    }
}

TEST_F(RenderingFixture, test_render_boundary_layers_protobuf)
{
    http::MockRequest rq(
        http::GET,
        http::URL("http://localhost/v1/rendering/render_boundary_layers")
            .addParam("node_id", "releases/new/" + std::to_string(releases[0].id()))
            .addParam("l", "tree_node_objects")
            .addParam("x", testTile().x())
            .addParam("y", testTile().y())
            .addParam("z", testTile().z())
    );
    rq.headers.emplace("Accept", "application/x-protobuf");
    auto resp = yacare::performTestRequest(rq);

    EXPECT_EQ(resp.status, 200);
    EXPECT_EQ(resp.headers.at("Content-Type"), "application/x-protobuf");

    TileV2 tileV2;
    Y_PROTOBUF_SUPPRESS_NODISCARD
    tileV2.ParseFromArray(resp.body.data(), resp.body.size());
    EXPECT_TRUE(tileV2.IsInitialized());

    ::maps::common::writeFile("tile_protobuf.txt", tileV2.DebugString());

    std::string expectedResponse =
        maps::common::readFileToString(
            SRC_("data/render_boundary_layers/tile_protobuf.txt")
        );
    EXPECT_EQ(tileV2.DebugString(), expectedResponse);

    INFO() << "Working dir is " << GetWorkPath();
}

void testHotspotBoundaryLayers(
    const tile::Tile& tile,
    const std::string& layer,
    const std::string& expectedName,
    const std::string& nodeId,
    const std::string& releaseId = "",
    const std::string& patchIds = "",
    const std::string& sputnicaFilter = "")
{
    auto url = http::URL("http://localhost/v1/rendering/hotspot_boundary_layers")
        .addParam("node_id", nodeId)
        .addParam("l", layer)
        .addParam("x", tile.x())
        .addParam("y", tile.y())
        .addParam("z", tile.z());
    if (!releaseId.empty()) {
        url.addParam("dem_release_id", releaseId);
    }
    if (!patchIds.empty()) {
        url.addParam("dem_patch_ids", patchIds);
    }
    if (!sputnicaFilter.empty()) {
        url.addParam("filter", sputnicaFilter);
    }
    http::MockRequest rq(http::GET, url);
    auto resp = yacare::performTestRequest(rq);

    ASSERT_EQ(resp.status, 200);
    EXPECT_EQ(resp.headers.at("Content-Type"), "application/json");

    ::maps::common::writeFile(expectedName, resp.body);

    std::string expectedResponse =
        maps::common::readFileToString(
            SRC_("data/render_boundary_layers/" + expectedName)
        );

    auto receivedJson = json::Value::fromString(resp.body);
    auto expectedJson = json::Value::fromString(expectedResponse);

    auto receivedFeatures = receivedJson["data"]["features"];
    auto expectedFeatures = expectedJson["data"]["features"];
    EXPECT_TRUE(std::is_permutation(receivedFeatures.begin(),
        receivedFeatures.end(),
        expectedFeatures.begin(),
        expectedFeatures.end()));
}

TEST_F(RenderingFixture, test_hotspot_boundary_layers)
{
    INFO() << "Working dir is " << GetWorkPath();
    std::string nodeId = "releases/new/" + std::to_string(releases[0].id());
    testHotspotBoundaryLayers(testTile(), "tree_node_objects", "tile.json", nodeId);
    testHotspotBoundaryLayers(testTile(), "dem_patches", "dem_tile.json", nodeId);

    testHotspotBoundaryLayers(
        testTile(), "mosaic_sources", "mosaic_sources_tile.json", "", "", "",
        "order:" + std::to_string(orders.at(0).id()));
}

TEST_F(RenderingFixture, test_hotspot_boundary_layers_dem_filtered)
{
    INFO() << "Working dir is " << GetWorkPath();
    std::string nodeId = "releases/new/" + std::to_string(releases[0].id());
    testHotspotBoundaryLayers(testTile(), "dem_patches", "dem_tile_release_2.json", nodeId, "2");
    testHotspotBoundaryLayers(testTile(), "dem_patches", "dem_tile_patch_1_2.json", nodeId, "", "1,2");
}

TEST_F(RenderingFixture, test_hotspot_boundary_layers_with_callback)
{
    INFO() << "Working dir is " << GetWorkPath();
    const std::string callbackFunc = "callback_func";
    http::MockRequest rq(
        http::GET,
        http::URL("http://localhost/v1/rendering/hotspot_boundary_layers")
            .addParam("node_id", "releases/new/" + std::to_string(releases[0].id()))
            .addParam("l", "tree_node_objects")
            .addParam("x", testTile().x())
            .addParam("y", testTile().y())
            .addParam("z", testTile().z())
            .addParam("callback", callbackFunc)
    );
    auto resp = yacare::performTestRequest(rq);

    ASSERT_EQ(resp.status, 200);
    EXPECT_EQ(resp.headers.at("Content-Type"), "application/javascript");

    ::maps::common::writeFile("tile.json", resp.body);

    auto received = resp.body;
    const std::string EXPECTED_PREFIX = "/**/" + callbackFunc + "(";
    const std::string EXPECTED_SUFFIX = ");";
    EXPECT_EQ(received.find(EXPECTED_PREFIX), 0u);
    EXPECT_EQ(received.rfind(EXPECTED_SUFFIX), received.size() - EXPECTED_SUFFIX.size());
    auto receivedJson = json::Value::fromString(
        received.substr(EXPECTED_PREFIX.size(),
            received.size() - EXPECTED_PREFIX.size() - EXPECTED_SUFFIX.size()));

    std::string expectedResponse =
        maps::common::readFileToString(
            SRC_("data/render_boundary_layers/tile.json")
        );

    auto expectedJson = json::Value::fromString(expectedResponse);

    auto receivedFeatures = receivedJson["data"]["features"];
    auto expectedFeatures = expectedJson["data"]["features"];
    EXPECT_TRUE(std::is_permutation(receivedFeatures.begin(),
        receivedFeatures.end(),
        expectedFeatures.begin(),
        expectedFeatures.end()));
}

TEST_F(RenderingFixture, test_hotspot_boundary_layers_empty_tile)
{
    INFO() << "Working dir is " << GetWorkPath();
    http::MockRequest rq(
        http::GET,
        http::URL("http://localhost/v1/rendering/hotspot_boundary_layers")
            .addParam("node_id", "releases")
            .addParam("l", "tree_node_objects")
            .addParam("x", 10)
            .addParam("y", 10)
            .addParam("z", 18)
    );
    auto resp = yacare::performTestRequest(rq);

    ASSERT_EQ(resp.status, 200);
    EXPECT_EQ(resp.headers.at("Content-Type"), "application/json");
    EXPECT_EQ(resp.body, EMPTY_HOTSPOT_RESPONSE);

}

TEST_F(RenderingFixture, test_render_boundary_layers_access_allow_origin)
{
    struct TestData {
        std::string origin;
        bool allowed;
    };

    std::vector<TestData> testData{
        {"https://factory-admin.tst.c.maps.yandex-team.ru", true},
        {"https://google.com", false},
    };

    for (const auto&[origin, allowed]: testData) {
        SCOPED_TRACE(origin);

        http::MockRequest rq(
            http::GET,
            http::URL("http://localhost/v1/rendering/render_boundary_layers")
                .addParam("node_id", "releases")
                .addParam("l", "tree_node_objects")
                .addParam("x", testTile().x())
                .addParam("y", testTile().y())
                .addParam("z", testTile().z())
        );
        rq.headers.emplace("Origin", origin);
        rq.headers.emplace("Accept", "application/x-protobuf");
        auto resp = yacare::performTestRequest(rq);

        EXPECT_EQ(resp.status, 200);
        EXPECT_EQ(resp.headers.at("Content-Type"), "application/x-protobuf");

        auto allowOriginItr = resp.headers.find("Access-Control-Allow-Origin");
        auto allowCredentialsItr = resp.headers.find("Access-Control-Allow-Credentials");
        if (allowed) {
            EXPECT_TRUE(allowOriginItr != resp.headers.end()
                        && allowOriginItr->second == origin
                        && allowCredentialsItr != resp.headers.end()
                        && allowCredentialsItr->second == "true");
        } else {
            EXPECT_TRUE(allowOriginItr == resp.headers.end()
                        && allowCredentialsItr == resp.headers.end());
        }
    }
}

TEST_F(RenderingFixture, cache_using_etag)
{
    http::MockRequest rq(
        http::GET,
        http::URL("http://localhost/v1/rendering/render_boundary_layers")
            .addParam("node_id", "releases/new/" + std::to_string(releases[0].id()))
            .addParam("l", "tree_node_objects")
            .addParam("x", testTile().x())
            .addParam("y", testTile().y())
            .addParam("z", testTile().z())
    );
    rq.headers.emplace("Accept", "application/x-protobuf");

    {
        auto resp = yacare::performTestRequest(rq);

        EXPECT_EQ(resp.status, 200);
        EXPECT_EQ(resp.headers.at("Content-Type"), "application/x-protobuf");
        EXPECT_EQ(resp.headers.at("Cache-Control"), "no-cache");
        ASSERT_EQ(resp.headers.count(ETAG_HEADER), 1u);
        const std::string etag = resp.headers[ETAG_HEADER];

        TileV2 tileV2;
        Y_PROTOBUF_SUPPRESS_NODISCARD
        tileV2.ParseFromArray(resp.body.data(), resp.body.size());
        EXPECT_TRUE(tileV2.IsInitialized());

        ::maps::common::writeFile("tile_protobuf.txt", tileV2.DebugString());

        std::string expectedResponse =
            maps::common::readFileToString(
                SRC_("data/render_boundary_layers/tile_protobuf.txt")
            );
        EXPECT_EQ(tileV2.DebugString(), expectedResponse);

        INFO() << "Working dir is " << GetWorkPath();
        rq.headers["If-None-Match"] = etag;
    }

    {
        auto resp = yacare::performTestRequest(rq);
        ASSERT_EQ(resp.status, 304);
    }
}

TEST_F(RenderingFixture, test_get_boundary_layers_objects)
{
    http::MockRequest rq(
        http::GET,
        http::URL("http://localhost/v1/rendering/get_boundary_layers_objects")
            .addParam("node_id", "releases/new/" + std::to_string(releases[0].id()))
            .addParam("l", "tree_node_objects,unassigned")
            .addParam("bbox", "37.2656,55.5587~37.9688,55.9552")
            .addParam("z", testTile().z())
    );
    rq.headers.emplace("Accept", "application/x-protobuf");
    auto resp = yacare::performTestRequest(rq);

    ASSERT_EQ(resp.status, 200);
    EXPECT_EQ(resp.headers.at("Content-Type"), "application/x-protobuf");

    auto sprotoResponse = boost::lexical_cast<snavigation::NodeObjects>(resp.body);

    std::vector<int64> expectedMosaicIds{
        mosaics.at(0).id(),
        mosaics.at(1).id(),
        mosaics.at(2).id(),
        mosaics.at(3).id(),
        mosaics.at(4).id(),
        mosaics.at(5).id(),
    };

    std::vector<int64> expectedMosaicSourcesIds{
        mosaicSources.at(0).id(),
    };

    std::vector<int64> mIds;
    for (auto& arg: sprotoResponse.mosaics()) {
        mIds.push_back(std::stol(arg.id()));
    }
    std::vector<int64> msIds;
    for (auto& arg: sprotoResponse.mosaicSources()) {
        msIds.push_back(std::stol(arg.id()));
    }
    EXPECT_THAT(mIds, UnorderedElementsAreArray(expectedMosaicIds));
    EXPECT_THAT(msIds, UnorderedElementsAreArray(expectedMosaicSourcesIds));
}

} // namespace maps::factory::renderer::tests
