#include "report.h"

namespace yandex {
namespace maps {
namespace mapkit {
namespace guidance {
namespace internal {
namespace tester {

namespace {

void printSuccess(const Result& result, std::ostream& os)
{
    for (const auto& event: result.realizedEvents) {
        os << "Realized expected event '" << event.type << "' " <<
            "at " << to_string(event.timestamp) << "\n";
    }
}

} // namespace

void printReport(
    const Result& result,
    std::ostream& os,
    bool printSuccessExpectations)
{
    for (const auto& event: result.unexpectedEvents) {
        os << "Unexpected event '" << event.type <<
            "' at " << to_string(event.timestamp) << "\n";
    }
    for (const auto& expectation: result.failedExpectations) {
        os << "Failed expectation " << expectation << "\n";
    }

    if (printSuccessExpectations) {
        printSuccess(result, os);
    }
}

} } } } } } // yandex::maps::mapkit::guidance::internal::tester
