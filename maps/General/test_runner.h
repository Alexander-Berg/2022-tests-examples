#pragma once

#include "map_test.h"
#include "test_creator.h"
#include "tile_utils.h"

#include <maps/renderer/libs/base/include/json_fwd.h>
#include <maps/renderer/libs/base/include/thread_pool.h>

namespace maps::renderer::check {

enum Status {
    Passed,
    Warning,
    Failed
};

std::string statusToStr(Status status);

struct CheckResult
{
    std::string testName;
    uint32_t zoom;
    Status status;
    TestReport report;
    std::vector<TileTestResult> failedToLoadTiles;
};

class TestRunner
{
public:
    TestRunner(unsigned int numThreads);

    std::vector<CheckResult> run(TestCreator& testCreator,
                                 json::ValueRef testConfig,
                                 const TileLoader& testTiles,
                                 const TileLoader& goldenTiles) const;

private:
    TestReport testZoom(IMapTest& test, unsigned int z) const;
    TestReport testTileRange(IMapTest& test, const Tile& lt, const Tile& rb) const;

    mutable base::ThreadPool threadPool_;
};

} // namespace maps::renderer::check
