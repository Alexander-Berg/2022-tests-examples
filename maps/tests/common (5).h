#pragma once
#include <boost/test/unit_test.hpp>

#include <maps/libs/json/include/value.h>

#include <functional>
#include <string>

namespace maps {
namespace wiki {
namespace routing {
namespace tests {

typedef std::function<void(const json::Value&)> TestRunner;

bool initTestSuite(const std::string& suiteName, TestRunner run);

} // namespace tests
} // namespace routing
} // namespace wiki
} // namespace maps
