#pragma once

#include <yandex/maps/mapkit/guidance/internal/tester.h>

#include <ostream>

namespace yandex {
namespace maps {
namespace mapkit {
namespace guidance {
namespace internal {
namespace tester {

/**
 * Print report to stream.
 * 
 * By default output includes only errors.
 * 
 * @param printSuccessExpectation include success expectation
 */
void printReport(
    const Result& result,
    std::ostream& os,
    bool printSuccessesExpectations = false);

} } } } } } // yandex::maps::mapkit::guidance::internal::tester
