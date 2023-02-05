#include "mesh_test.h"

#include <maps/libs/log8/include/log8.h>

namespace maps::renderer::check {

namespace {

int countMeshes(const TileData& tile)
{
    int numMeshes = 0;
    for (const auto& polygon : tile.polygonFeatures()) {
        if (polygon.mesh)
            ++numMeshes;
    }

    return numMeshes;
}

std::vector<std::string>
getFailedToLoadMeshes(const TileData& testTile,
                      const TileLoaders& tileLoader)
{
    std::vector<std::string> res;
    for (const auto& polygon : testTile.polygonFeatures()) {
        if (polygon.mesh) {
            if (tileLoader.testMesh(polygon.mesh->meshId).empty())
                res.push_back(polygon.mesh->meshId);
        }
    }

    return res;
}

} // namespace

MeshTest::MeshTest(const TileLoaders& tileLoader)
    : IMapTest(tileLoader)
    , numMeshesGolden_(0)
    , numMeshesTest_(0)
    , maxErrorTiles_(5)
{
}

void MeshTest::processSubRectAsync(const TileRange& rect)
{
    std::vector<TileTestResult> processedTiles;
    size_t numMeshesGolden = 0;
    size_t numMeshesTest = 0;

    std::string locale = "ru_RU";
    for (const auto& t : rect) {
        auto zoomRange = base::ZoomRange(t.z(), t.z());
        auto testTile = tileLoader_.testTile(t, zoomRange, locale);
        numMeshesTest += countMeshes(testTile);
        auto failedToLoadMeshes = getFailedToLoadMeshes(testTile, tileLoader_);
        if (!failedToLoadMeshes.empty()) {
            std::ostringstream sOut;
            sOut << "Following meshes have failed to load: "
                 << failedToLoadMeshes.front();
            for (size_t i = 1; i != failedToLoadMeshes.size(); ++i)
                sOut << ", " << failedToLoadMeshes[i];

            TileTestResult tileResult = {t, {0, ""}};
            tileResult.error.problemDescr = sOut.str();
            tileResult.error.val = failedToLoadMeshes.size();
            processedTiles.push_back(tileResult);
            numMeshesTest -= failedToLoadMeshes.size();
        }

        numMeshesGolden += countMeshes(tileLoader_.goldenTile(t, zoomRange, locale));
    }

    maxErrorTiles_.add(processedTiles);
    numMeshesGolden_ += numMeshesGolden;
    numMeshesTest_ += numMeshesTest;
}

TestReport MeshTest::createReport() const
{
    TestReport report;
    report.maxErrorTiles = maxErrorTiles_.get();
    report.aggregateError = std::max(fabs(
        float(numMeshesGolden_) - float(numMeshesTest_)) / numMeshesGolden_,
        report.maxErrorTiles.empty() ? 0.0f: 1.0f);

    std::ostringstream sOut;
    sOut << "Number of meshes. "
         << "Golden: " << numMeshesGolden_ << ". "
         << "Test: " << numMeshesTest_ << ". "
         << "Error value: " << report.aggregateError << ".";
    report.problemDescr = sOut.str();
    INFO() << sOut.str();

    return report;
}

std::unique_ptr<IMapTest> MeshTestCreator::getTest(json::ValueRef /*testConfig*/,
                                                   const TileLoaders& tileLoader) const
{
    return std::make_unique<MeshTest>(tileLoader);
}

} // namespace maps::renderer::check
