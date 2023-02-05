#pragma once

#include <maps/infra/ratelimiter2/common/include/limit_info.h>
#include <maps/infra/ratelimiter2/common/include/counters.h>
#include <maps/infra/ratelimiter2/common/include/sorted_counters.h>
#include <maps/infra/ratelimiter2/common/include/limit.h>

#include <maps/libs/introspection/include/stream_output.h>
#include <maps/libs/introspection/include/comparison.h>

#include <boost/lexical_cast.hpp>
#include <boost/uuid/uuid_io.hpp>

#include <chrono>

// Introspection helpers for tests

namespace maps::rate_limiter2 {
namespace impl {

template<typename K, typename T, typename H, typename E, typename A>
inline const std::unordered_map<K, T, H, E, A>& introspect(const AdditiveMap<K, T, H, E, A>& data)
{
    return data;
}

template<class K, class V>
inline const auto& introspect(const SortedMap<K, V>& data)
{
    return data.storage();
}

using introspection::operator<<;

} // namespace impl

inline auto introspect(const LimitInfo& t) {
    return std::tie(t.spec, t.burst);
}

inline auto introspect(const Limit& t) {
    return std::tie(t.rate, t.unit, t.gen);
}

using introspection::operator<<;
using introspection::operator==;

namespace tests {

class ManualClock
{
    std::chrono::system_clock::time_point t_;
public:
    ManualClock(std::chrono::system_clock::time_point t = std::chrono::system_clock::now()) : t_(t) {}

    std::chrono::system_clock::time_point operator()() const { return t_; }
    std::chrono::system_clock::time_point& operator()() { return t_; }
};

inline boost::uuids::uuid uuid(std::string_view str)
{
    return boost::lexical_cast<boost::uuids::uuid>(str);
}

} // namespace tests

} // namespace maps::rate_limiter2
