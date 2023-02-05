#include "building_test.h"

#include <maps/libs/log8/include/log8.h>


namespace maps::renderer::check {

namespace {

namespace basemap = yandex::maps::proto::renderer::layers::basemap;

bool hasBuildingTag(const std::vector<basemap::Tag>& tags)
{
    return std::find(tags.begin(), tags.end(), basemap::Tag::BUILDING) != tags.end();
}

bool isClickable(const std::string& id)
{
    return !id.empty();
}

size_t getNumBuildings(const TileData& tile, bool onlyClickable)
{
    size_t res = 0;
    for (const auto& p : tile.polygonFeatures()) {
        if (hasBuildingTag(p.tags)) {
            if (!onlyClickable || isClickable(p.layer.id))
                ++res;
        }
    }

    return res;
}

} // namespace

BuildingTest::BuildingTest(const TileLoaders& tileLoader,
                           bool onlyClickable)
    : IMapTest(tileLoader)
    , numBldGolden_(0)
    , numBldTest_(0)
    , onlyClickable_(onlyClickable)
    , maxErrorTiles_(5)
{}

void BuildingTest::processSubRectAsync(const TileRange& rect)
{
    std::vector<TileTestResult> processedTiles;

    size_t numBldGolden = 0;
    size_t numBldTest = 0;
    const std::string locale = "ru_RU";
    for (const auto& t : rect) {
        auto zoomRange = base::ZoomRange(t.z(), t.z());
        size_t tileNumBldGolden = getNumBuildings(tileLoader_.goldenTile(t, zoomRange, locale), onlyClickable_);
        size_t tileNumBldTest = getNumBuildings(tileLoader_.testTile(t, zoomRange, locale), onlyClickable_);
        if (tileNumBldGolden != tileNumBldTest)
            processedTiles.push_back({t, {fabs(float(tileNumBldGolden) - float(tileNumBldTest)), ""}});

        numBldGolden += tileNumBldGolden;
        numBldTest += tileNumBldTest;
    }

    numBldGolden_ += numBldGolden;
    numBldTest_ += numBldTest;
    maxErrorTiles_.add(processedTiles);
}

TestReport BuildingTest::createReport() const
{
    TestReport report;
    report.aggregateError = fabs(float(numBldGolden_) - float(numBldTest_)) / numBldGolden_;
    report.maxErrorTiles = maxErrorTiles_.get();
    std::ostringstream sOut;
    sOut << "Number of buildings. "
         << "Golden: " << numBldGolden_ << ". "
         << "Test: " << numBldTest_ << ". "
         << "Error value: " << report.aggregateError << ".";
    report.problemDescr = sOut.str();
    INFO() << sOut.str();
    return report;
}

std::unique_ptr<IMapTest> ClickableBuildingTestCreator::getTest(json::ValueRef /*testConfig*/,
                                                                const TileLoaders& tileLoader) const
{
    bool isClickable = true;
    return std::make_unique<BuildingTest>(tileLoader, isClickable);
}

} // namespace maps::renderer::check
