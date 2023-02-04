#include "validate.h"

#include <maps/garden/sdk/cpp/exceptions.h>
#include <maps/libs/log8/include/log8.h>
#include <maps/masstransit/libs/data/reader/include/data.h>

#include <sstream>
#include <unordered_set>


namespace maps::garden::modules::masstransit_tester {

template<class Type>
bool satisfiesMaxRelativeDifference(
        const xrange_view::XRangeView<Type, uint32_t>& currentValues,
        const xrange_view::XRangeView<Type, uint32_t>& oldValues,
        const double maxRelativeDifference,
        std::ostringstream& report)
{
    std::unordered_set<std::string_view> currentResult;
    std::for_each(currentValues.begin(), currentValues.end(),
        [&currentResult](const Type& obj) {
            currentResult.insert(obj.id());
        }
    );
    const int64_t currentValuesNumber = currentResult.size();
    report << "<p>Current values number: " << currentValuesNumber << "\n";

    std::unordered_set<std::string_view> oldResult;
    std::for_each(oldValues.begin(), oldValues.end(),
        [&oldResult](const Type& obj) {
            oldResult.insert(obj.id());
        }
    );
    const int64_t oldValuesNumber = oldResult.size();
    report << "<br>In previous dataset: " << oldValuesNumber << "\n";

    const double relativeDifference =
        std::abs(oldValuesNumber - currentValuesNumber) / double(oldValuesNumber);
    report << "<br>Relative difference: " << std::setprecision(4) <<
        std::fixed << relativeDifference << "\n" <<
        "<br>Max allowed relative difference: " << maxRelativeDifference << "\n";

    const bool result = relativeDifference < maxRelativeDifference;
    report << "<br>ok: " << (result ? "True" : "False") << "</p>\n";
    if (!result) {
        report << "<h3>Added values:</h3>\n";
        report << "<ol>\n";
        for (const auto& current : currentResult) {
            if (!oldResult.contains(current)) {
                report << "<li>" << current << "\n";
            }
        }

        report << "<h3>Deleted values:</h3>\n";
        report << "<ol>\n";
        for (const auto& old : oldResult) {
            if (!currentResult.contains(old)) {
                report << "<li>" << old << "\n";
            }
        }
    }
    return result;
}

std::pair<bool, std::string> validateData(const std::string& toValidate,
    const std::string& groundTruth,
    const MasstransitDataValidationConfig& validationConfig)
{
    using MtData = maps::masstransit::data::reader::Masstransit;
    auto mtToValidate = MtData::fromFile(toValidate + "/static.fb", EMappingMode::Locked);
    auto mtGroundTruth = MtData::fromFile(groundTruth + "/static.fb", EMappingMode::Locked);

    std::ostringstream report;
    bool validateResult = true;
    report << "<h1> Masstransit data validation report </h1>\n";

    report << "<h2>Stops</h2>\n";
    validateResult &= satisfiesMaxRelativeDifference(mtToValidate.stops(),
        mtGroundTruth.stops(),
        validationConfig.stopsMaxRelativeDifference,
        report
    );

    report << "<h2>Routes</h2>\n";
    validateResult &= satisfiesMaxRelativeDifference(mtToValidate.routes(),
        mtGroundTruth.routes(),
        validationConfig.routesMaxRelativeDifference,
        report
    );

    report << "<h2>Threads</h2>\n";
    validateResult &= satisfiesMaxRelativeDifference(mtToValidate.threads(),
        mtGroundTruth.threads(),
        validationConfig.threadsMaxRelativeDifference,
        report
    );

    return {validateResult, report.str()};
}

} // namespace maps::garden::modules::masstransit_tester
