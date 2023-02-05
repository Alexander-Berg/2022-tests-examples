#include "poi_test.h"

#include <maps/libs/log8/include/log8.h>


namespace maps::renderer::check {

namespace {

namespace basemap = yandex::maps::proto::renderer::layers::basemap;

bool hasPoiTag(const std::vector<basemap::Tag>& tags)
{
    return std::find(tags.begin(), tags.end(), basemap::Tag::POI) != tags.end();
}

bool isClickable(const std::map<std::string, std::string>& attr)
{
    auto it = attr.find("uri");
    if (it == attr.end())
        return false;

    return !it->second.empty();
}

size_t getNumPOI(const TileData& tile, bool onlyClickable)
{
    size_t res = 0;
    for (const auto& l : tile.straightLabels()) {
        if (hasPoiTag(l.tags)) {
            if (!onlyClickable || isClickable(l.layer.attrs))
                ++res;
        }
    }

    return res;
}

base::ZoomRange calcFeatureZoom(const Tile& t)
{
    switch (t.z()) {
        case 15:
            return {15, 19};

        default:
            return {t.z(), t.z()};
    }
}

} // namespace

PoiTest::PoiTest(const TileLoaders& tileLoader,
                 bool requestMultizoomTiles,
                 bool onlyClickable)
    : IMapTest(tileLoader)
    , numPoiGolden_(0)
    , numPoiTest_(0)
    , requestMultizoomTiles_(requestMultizoomTiles)
    , onlyClickable_(onlyClickable)
    , maxErrorTiles_(5)
{}

void PoiTest::processSubRectAsync(const TileRange& rect)
{
    std::vector<TileTestResult> processedTiles;

    size_t numPoiGolden = 0;
    size_t numPoiTest = 0;
    const std::string locale = "ru_RU";
    for (const auto& t : rect) {
        auto zoomRange = requestMultizoomTiles_ ? calcFeatureZoom(t)
                                                : base::ZoomRange(t.z(), t.z());
        size_t tileNumPoiGolden = getNumPOI(tileLoader_.goldenTile(t, zoomRange, locale), onlyClickable_);
        size_t tileNumPoiTest = getNumPOI(tileLoader_.testTile(t, zoomRange, locale), onlyClickable_);
        if (tileNumPoiGolden != tileNumPoiTest)
            processedTiles.push_back({t, {fabs(float(tileNumPoiGolden) - float(tileNumPoiTest)), ""}});

        numPoiGolden += tileNumPoiGolden;
        numPoiTest += tileNumPoiTest;
    }

    numPoiGolden_ += numPoiGolden;
    numPoiTest_ += numPoiTest;
    maxErrorTiles_.add(processedTiles);
}

TestReport PoiTest::createReport() const
{
    TestReport report;
    report.aggregateError = fabs(float(numPoiGolden_) - float(numPoiTest_));
    if (numPoiGolden_ != 0)
        report.aggregateError /= numPoiGolden_;
    report.maxErrorTiles = maxErrorTiles_.get();
    std::ostringstream sOut;
    sOut << "Number of poi. "
         << "Golden: " << numPoiGolden_ << ". "
         << "Test: " << numPoiTest_ << ". "
         << "Error value: " << report.aggregateError << ".";
    report.problemDescr = sOut.str();
    INFO() << sOut.str();
    return report;
}

std::unique_ptr<IMapTest> PoiTestCreator::getTest(json::ValueRef testConfig,
                                                  const TileLoaders& tileLoader) const
{
    auto requestMultizoomTilesIt = testConfig->FindMember("multizoom");
    bool requestMultizoomTiles = requestMultizoomTilesIt != testConfig->MemberEnd() ?
        requestMultizoomTilesIt->value.GetBool() : false;
    bool isClickable = false;
    return std::make_unique<PoiTest>(tileLoader, requestMultizoomTiles, isClickable);
}

std::unique_ptr<IMapTest> ClickablePoiTestCreator::getTest(json::ValueRef /*testConfig*/,
                                                           const TileLoaders& tileLoader) const
{
    bool requestMultizoomTiles = false;
    bool isClickable = true;
    return std::make_unique<PoiTest>(tileLoader, requestMultizoomTiles, isClickable);
}

} // namespace maps::renderer::check
