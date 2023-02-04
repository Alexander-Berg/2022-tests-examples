#pragma once

namespace maps {

namespace json {

class Value;

} // namespace json

namespace wiki {
namespace groupedit {
namespace tests {

// Should be run from within boost unit test
void checkExpectedJson(
    const json::Value& objects,
    const json::Value& expected);

} // namespace tests
} // namespace groupedit
} // namespace wiki
} // namespace maps
