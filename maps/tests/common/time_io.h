#pragma once

#include <maps/infopoint/lib/misc/time.h>

#include <chrono>
#include <ostream>

// Boost test needs output operator for BOOST_CHECK_EQUAL macro.

namespace std {

std::ostream& operator<< (
        std::ostream& stream,
        const infopoint::TimePoint& point);

std::ostream& operator<< (
        std::ostream& stream,
        const std::chrono::seconds& seconds);

} // namespace std
