#include "test_runner.h"

#include "rect_splitter.h"

#include <maps/libs/log8/include/log8.h>

#include <maps/libs/geolib/include/conversion.h>
#include <maps/renderer/libs/base/include/future.h>
#include <maps/renderer/libs/base/include/wallclock_timer.h>

namespace maps::renderer::check {

namespace {

Tile latLon2Tile(double lat, double lon, unsigned int zoom)
{
    auto mercatorP = geolib3::geoPoint2Mercator({lon, lat});
    return tile::mercatorToTile(mercatorP, zoom);
}

struct Threshold
{
    float warning;
    float critical;
};

Threshold getThresholds(json::ValueRef testSetConfig)
{
    auto errorField = testSetConfig->FindMember("error");
    REQUIRE(errorField != testSetConfig->MemberEnd(), "error field is missing");
    return {float(errorField->value["warning"].GetDouble()),
            float(errorField->value["critical"].GetDouble())};
}

Status calcStatus(float error, const Threshold& t)
{
    if (error < t.warning)
        return Status::Passed;
    else if (error < t.critical)
        return Status::Warning;

    return Status::Failed;
}

void filterTiles(std::vector<TileTestResult>& maxErrorTiles, const Threshold& t)
{
    auto it = maxErrorTiles.begin();
    for (; it != maxErrorTiles.end() && (*it).error.val >= t.warning; ++it);

    maxErrorTiles.erase(it, maxErrorTiles.end());
}

} // namespace

std::string statusToStr(Status status)
{
    switch (status) {
        case Status::Failed:
            return "Failed";
        case Status::Warning:
            return "Warning";
        case Status::Passed:
            return "Passed";
    }
    return "unknown";
}

TestRunner::TestRunner(unsigned int numThreads)
{
    threadPool_.startThreads(numThreads);
}

std::vector<CheckResult>
TestRunner::run(TestCreator& testCreator,
                json::ValueRef testSetConfig,
                const TileLoader& testTiles,
                const TileLoader& goldenTiles) const
{
    std::vector<CheckResult> reports;
    auto testsField = testSetConfig->FindMember("tests");
    REQUIRE(testsField != testSetConfig->MemberEnd(), "tests field is missing");

    auto thresholds = getThresholds(testSetConfig);

    const auto& testConfigs = testsField->value;
    for (unsigned i = 0; i != testConfigs.Size(); ++i) {
        const auto& testConfig = testConfigs[i];
        auto testName = testConfig["name"].GetString();
        if (testConfig.HasMember("error")) {
            INFO() << "Customized error has been found for test " << testName;
            thresholds = getThresholds(testConfig);
        }
        auto testRectField = testConfig.FindMember("rect");
        base::WallclockTimer timer;
        const auto& zooms = testConfig["zooms"];
        for (unsigned j = 0; j != zooms.Size(); ++j) {
            TileLoaders tileLoader(testTiles, goldenTiles);

            auto zoom = zooms[j].GetUint();
            auto test = testCreator.getTest(testConfig, tileLoader);
            INFO() << "Running test: " << testName << " on zoom " << zoom;

            CheckResult checkResult;
            checkResult.testName = testName;
            checkResult.zoom = zoom;

            if (testRectField == testConfig.MemberEnd()) {
                checkResult.report = testZoom(*test, zoom);
            } else {
                json::ValueRef rect = testRectField->value;
                Tile lt(latLon2Tile(rect["lt"]["lat"]->GetDouble(),
                                    rect["lt"]["lon"]->GetDouble(), zoom));
                Tile rb(latLon2Tile(rect["rb"]["lat"]->GetDouble(),
                                    rect["rb"]["lon"]->GetDouble(), zoom));
                checkResult.report = testTileRange(*test, lt, rb);
            }

            checkResult.status = calcStatus(checkResult.report.aggregateError, thresholds);
            if (checkResult.status == Status::Passed) {
                checkResult.report.maxErrorTiles.clear();
            } else {
                filterTiles(checkResult.report.maxErrorTiles, thresholds);
            }

            checkResult.failedToLoadTiles = tileLoader.failedToLoadTiles();
            if (!checkResult.failedToLoadTiles.empty()) {
                checkResult.status = Status::Failed;
            }

            size_t tilesProcessed = tileLoader.testTilesRequested() + tileLoader.goldenTilesRequested();
            INFO() << "Tiles processed: " << tilesProcessed << " (" <<  tilesProcessed * 1000.0 / timer.elapsedMs() << " rps)";

            reports.push_back(checkResult);
            INFO() << "Status: " << statusToStr(checkResult.status);
        }
    }

    return reports;
}

TestReport TestRunner::testTileRange(IMapTest& test, const Tile& lt, const Tile& rb) const
{
    REQUIRE(lt.x() <= rb.x() && lt.y() <= rb.y(),
        "Wrapped rectangles are not supported");

    RectSplitter splitter(lt, rb, 8);
    std::vector<base::Future<void>> jobsFutures;
    for (const auto& jobRect : RectSplitter(lt, rb, 8)) {
        jobsFutures.push_back(base::async(&threadPool_, [jobRect, &test] {
            return test.processSubRectAsync(jobRect);
        }));
    }

    for (auto& f : jobsFutures) {
        f.get();
    }

    return test.createReport();
}

TestReport TestRunner::testZoom(IMapTest& test, uint32_t z) const
{
    auto maxCoordVal = (1 << z) - 1;
    return testTileRange(test,
                         Tile(TileCoord(0, 0), z),
                         Tile(TileCoord(maxCoordVal, maxCoordVal), z));
}

} // namespace maps::renderer::check
