#include "wrapper.h"

#include <maps/routing/matrix_router/data_preparation/lib/include/prepare_slices.h>

#include <library/cpp/testing/unittest/env.h>
#include <mapreduce/yt/interface/io.h>

#include <util/stream/file.h>

namespace maps::routing::matrix_router {

std::vector<std::vector<float>> prepareSlicesWrapper(
    const std::string& routesTable,
    const std::string& timePartsTable,
    float defaultSpeed,
    size_t maxTestRoutesSize,
    double minRouteLength,
    double minRouteTravelTime)
{
    TIFStream routesStream(SRC_(routesTable));
    auto routesReader = NYT::CreateTableReader<NYT::TNode>(&routesStream);

    TIFStream timePartsStream(SRC_(timePartsTable));
    auto timePartsReader = NYT::CreateTableReader<NYT::TNode>(&timePartsStream);

    return prepareSlices(
        routesReader,
        timePartsReader,
        defaultSpeed,
        maxTestRoutesSize,
        minRouteLength,
        minRouteTravelTime);
}

}  // namespace maps::routing::matrix_router
