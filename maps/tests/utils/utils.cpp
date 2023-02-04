#include "utils.h"

#include <util/generic/guid.h>

#include <random>


namespace maps::mirc::radiomap_evaluator::tests {

double getRand(double min, double max)
{
    static std::mt19937 rng;
    std::uniform_real_distribution<double> distribution(min, max);
    return distribution(rng);
}

std::string generateUUID()
{
    const auto tsGuid = CreateGuidAsString();
    return std::string(tsGuid.data(), tsGuid.length());
}

} // namespace maps::mirc::radiomap_evaluator::tests
