#pragma once

#include <string>

namespace maps::garden::modules::masstransit_tester {

struct MasstransitDataValidationConfig {
    double stopsMaxRelativeDifference;
    double routesMaxRelativeDifference;
    double threadsMaxRelativeDifference;
};

std::pair<bool, std::string> validateData(
    const std::string& toValidate,
    const std::string& groundTruth,
    const MasstransitDataValidationConfig& validationConfig);

} // namespace maps::garden::modules::masstransit_tester
