#pragma once

#include "map_test.h"
#include "max_error_tiles.h"
#include "test_creator.h"

namespace maps::renderer::check {

class PrimitiveCountTest : public IMapTest
{
public:
    PrimitiveCountTest(const TileLoaders& tileLoader);

    void processSubRectAsync(const TileRange& rect) override;

    TestReport createReport() const override;

private:
    std::atomic<size_t> numPoints_;
    std::atomic<size_t> numPolylines_;
    std::atomic<size_t> numPolygons_;
    std::atomic<size_t> numStraightLabels_;
    std::atomic<size_t> numCurvedLabels_;
    std::atomic<size_t> tilesProcessed_;

    MaxErrorTiles maxErrorTiles_;
};

class PrimitiveCountTestCreator : public TestCreator
{
public:
    std::unique_ptr<IMapTest> getTest(json::ValueRef testConfig,
                                      const TileLoaders& tileLoader) const override;
};

} // namespace maps::renderer::check
