#pragma once

#include <string>
#include <fstream>
#include <iostream>

namespace maps::wiki::autocart {

struct TestResult {
    size_t truePositive = 0;
    size_t falseNegative = 0;
    size_t trueNegative = 0;
    size_t falsePositive = 0;
};

std::ostream& operator<<(std::ostream& os, const TestResult& result);

TestResult evaluateClassifier(const std::string& datasetPath);

} // namespace maps::wiki::autocart
