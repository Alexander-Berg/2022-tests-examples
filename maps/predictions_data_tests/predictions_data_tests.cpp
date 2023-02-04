#include <maps/masstransit/info/mtinfo/lib/ydbfree/predictions_store.h>

#include <maps/libs/geolib/include/bounding_box.h>
#include <maps/libs/geolib/include/intersection.h>
#include <maps/libs/geolib/include/point.h>
#include <maps/libs/geolib/include/polyline.h>
#include <maps/libs/log8/include/log8.h>

#include <library/cpp/testing/common/env.h>
#include <library/cpp/testing/gtest/gtest.h>

#include <fstream>
#include <iterator>
#include <set>
#include <string>
#include <vector>

namespace geolib = maps::geolib3;
namespace mpd = maps::masstransit::ydbfree::predictions_dataset;
using namespace maps::masstransit::info::mtinfo;

namespace {

using PolylinesWithRef = std::vector<std::pair<geolib::Polyline2, const mpd::VehiclePrediction&>>;

std::vector<mpd::VehiclePrediction> readPredictionsFromFile(const std::string& filename) {
    std::ifstream istr{filename};
    REQUIRE(istr, "Can't read predictions from file " << filename);
    mpd::ForecastsReader reader{istr};
    std::vector<mpd::VehiclePrediction> predictions;
    try {
        reader.readAll(std::back_inserter(predictions));
    } catch (...) {
        WARN() << "Maybe someone changed forecasts protobuf structure?\n";
        throw;
    }
    return predictions;
}

PolylinesWithRef getPolylines(const std::vector<mpd::VehiclePrediction>& preds) {
    PolylinesWithRef res;
    res.reserve(preds.size());
    for (const auto& pred : preds) {
        auto& polyline = res.emplace_back(geolib::Polyline2{}, pred).first;
        for (const auto& seg : pred.prediction.segments) {
            const auto& lons = seg.polyline.lons.coords;
            const auto& lats = seg.polyline.lats.coords;
            for (size_t i = 0; i < lons.size(); ++i) {
                polyline.add(geolib::Point2(lons[i], lats[i]));
            }
        }
    }
    return res;
}

void testForBbox(
    const PredictionsData& pd, const PolylinesWithRef& polylines,
    const geolib::BoundingBox& bbox
) {
    std::set<VehicleId> pdVids;
    auto [begin, end] = pd.findInBBox(bbox);
    while (begin != end) {
        const auto& p = begin->value().get();
        pdVids.emplace(p.clid, p.uuid);
        ++begin;
    }

    std::set<VehicleId> expectedVids;
    for (const auto& [polyline, pred] : polylines) {
        const auto curPredBbox = geolib::expand(
            polyline.boundingBox(), geolib3::Point2(pred.lastGpsSignal.lon, pred.lastGpsSignal.lat).boundingBox()
        );
        if (geolib::intersects(curPredBbox, bbox)) {
            expectedVids.emplace(pred.clid, pred.uuid);
        }
    }

    // this is for what all of this code in that file was written
    EXPECT_EQ(pdVids, expectedVids);
}

void testForAllBboxes(
    const PredictionsData& pd, const PolylinesWithRef& polylines,
    const geolib::BoundingBox& bbox
) {
    for (double expandRatio = 0.1; expandRatio < 1.2; expandRatio += 0.1) {
        testForBbox(pd, polylines, resizeByRatio(bbox, expandRatio));
    }
}

void testForFile(const std::string& predictions, const std::string& sigstats, const geolib::BoundingBox& bbox) {
    PredictionsData pd(predictions, sigstats, "test-version");
    EXPECT_EQ(pd.version(), "test-version");

    EXPECT_EQ(pd.dataStatistics().clidStatistics.get("test").signals, 10);
    EXPECT_EQ(pd.dataStatistics().clidStatistics.get("fake").signals, std::nullopt);

    const auto preds = readPredictionsFromFile(predictions);
    const auto polylines = getPolylines(preds);
    EXPECT_EQ(preds.size(), polylines.size());

    testForAllBboxes(pd, polylines, bbox);
}

} // namespace

TEST(PredictionsDataTest, PredictionsDataInit) {
    testForFile(
        (TFsPath(GetWorkPath()) / "1616702362_280.pb").GetPath(),
        ArcadiaSourceRoot() + "/maps/masstransit/info/mtinfo/tests/predictions_data_tests/sigstats.json",
        geolib::BoundingBox{geolib::Point2{37.624860, 55.750781}, 0.099174, 0.031662}
    );
    testForFile(
        (TFsPath(GetWorkPath()) / "1616702406_45.pb").GetPath(),
        ArcadiaSourceRoot() + "/maps/masstransit/info/mtinfo/tests/predictions_data_tests/sigstats.json",
        geolib::BoundingBox{geolib::Point2{37.633283, 55.754475}, 0.007578, 0.002419}
    );
}
