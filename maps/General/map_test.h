#pragma once

#include "tile_loader.h"
#include "tile_utils.h"

#include <maps/renderer/libs/base/include/json_fwd.h>
#include <maps/renderer/libs/base/include/thread_pool.h>

namespace maps::renderer::check {

struct TestReport
{
    float aggregateError = 0.0f;
    std::string problemDescr;
    std::vector<TileTestResult> maxErrorTiles;
};

class IMapTest
{
public:
    IMapTest(const TileLoaders& tileLoader);

    virtual ~IMapTest() {}

    // Test rectangle is divided into several. For each smaller rectangle
    // this method will be called concurrently. Results merge should be done
    // manually at the end of this function.
    virtual void processSubRectAsync(const TileRange& rect) = 0;

    // This method will be called at the end of the test to obtain final results.
    virtual TestReport createReport() const = 0;

protected:
    const TileLoaders& tileLoader_;
};

class INonAggregateMapTest
{
public:
    virtual TileError testTile(const TileData& testTile,
                               const TileData& goldenTile) = 0;
    virtual ~INonAggregateMapTest() {}
};

class INonAggregateNoRefMapTest
{
public:
    virtual TileError testTile(const TileData& testTile) = 0;
    virtual ~INonAggregateNoRefMapTest() {}
};

std::unique_ptr<IMapTest>
convert(std::unique_ptr<INonAggregateMapTest> test,
        const TileLoaders& tileLoader,
        const std::string& locale,
        size_t maxTilesToStore);

std::unique_ptr<IMapTest>
convert(std::unique_ptr<INonAggregateNoRefMapTest> noRefTest,
        const TileLoaders& tileLoader,
        const std::string& locale,
        size_t maxTilesToStore);

} // namespace maps::renderer::check
