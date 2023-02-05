#include "primitive_count_test.h"

namespace maps::renderer::check {

namespace {

TileError calcError(size_t numPoints,
                    size_t numPolylines,
                    size_t numPolygons,
                    size_t numStraightLabels,
                    size_t numCurvedLabels)
{
    std::ostringstream sOut;
    sOut << "pt: " << numPoints
         << ", pl: " << numPolylines
         << ", pg: " << numPolygons
         << ", sl: " << numStraightLabels
         << ", cl: " << numCurvedLabels;

    return {0.0006f * numPoints
          + 0.0022f * numPolylines
          + 0.0014f * numPolygons
          + 0.0021f * numStraightLabels
          + 0.0142f * numCurvedLabels,
            sOut.str()};
}

} // namespace

PrimitiveCountTest::PrimitiveCountTest(const TileLoaders& tileLoader)
    : IMapTest(tileLoader)
    , numPoints_(0)
    , numPolylines_(0)
    , numPolygons_(0)
    , numStraightLabels_(0)
    , numCurvedLabels_(0)
    , tilesProcessed_(0)
    , maxErrorTiles_(5)
{
}

void PrimitiveCountTest::processSubRectAsync(const TileRange& rect)
{
    const std::string locale = "ru_RU";
    std::vector<TileTestResult> processedTiles;

    for (const auto& t : rect) {
        auto zoomRange = base::ZoomRange(t.z(), t.z());
        TileTestResult tileResult = {t, {0, ""}};
        const auto& tileData = tileLoader_.testTile(t, zoomRange, locale);

        numPoints_ += tileData.numPointFeatures();
        numPolylines_ += tileData.numPolylineFeatures();
        numPolygons_ += tileData.numPolygonFeatures();
        numStraightLabels_ += tileData.straightLabels().size();
        numCurvedLabels_ += tileData.curvedLabels().size();

        tileResult.error = calcError(
            tileData.numPointFeatures(),
            tileData.numPolylineFeatures(),
            tileData.numPolygonFeatures(),
            tileData.straightLabels().size(),
            tileData.curvedLabels().size());

        if (tileResult.error.val > 0)
            processedTiles.push_back(tileResult);
    }
    tilesProcessed_ += rect.size();

    maxErrorTiles_.add(processedTiles);
}

TestReport PrimitiveCountTest::createReport() const
{
    TestReport report;
    report.maxErrorTiles = maxErrorTiles_.get();
    report.aggregateError = calcError(
        numPoints_,
        numPolylines_,
        numPolygons_,
        numStraightLabels_,
        numCurvedLabels_).val / tilesProcessed_;

    std::ostringstream sOut;
    sOut << "Error value: " << report.aggregateError;
    report.problemDescr = sOut.str();
    return report;
}

std::unique_ptr<IMapTest> PrimitiveCountTestCreator::getTest(json::ValueRef /*testConfig*/,
                                                             const TileLoaders& tileLoader) const
{
    return std::make_unique<PrimitiveCountTest>(tileLoader);
}

} // namespace maps::renderer::check
