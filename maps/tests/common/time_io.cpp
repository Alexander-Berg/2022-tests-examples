#include "time_io.h"

namespace std {

std::ostream& operator<< (
        std::ostream& stream,
        const infopoint::TimePoint& point) {
    stream << infopoint::to_iso_extended_string(point);
    return stream;
}

std::ostream& operator<< (
        std::ostream& stream,
        const std::chrono::seconds& seconds) {
    stream << seconds.count() << "s";
    return stream;
}

} // namespace std
