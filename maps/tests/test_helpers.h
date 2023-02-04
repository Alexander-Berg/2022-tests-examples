#pragma once

#include <maps/infra/quotateka/agent/include/inventory.h>
#include <maps/infra/quotateka/agent/include/metrics.h>

#include <maps/infra/ratelimiter2/common/include/test_helpers.h>
#include <maps/libs/introspection/include/stream_output.h>

namespace maps::quotateka {

using introspection::operator<<;

inline auto introspect(const Inventory::ResourceQuotas& r) {
    return std::tie(
        r.resourceId, r.unit,
        r.defaultLimit, r.anonymLimit,
        r.accountLimits, r.endpointCosts
    );
}

namespace tests {

inline std::string toString(const yasm_metrics::YasmMetrics& metrics)
{
    std::stringstream ss;
    ss << metrics;
    return ss.str();
}

} // namespace tests

} // namespace maps::quotateka
