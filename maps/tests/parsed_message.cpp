#include "parsed_message.h"

#include <maps/libs/json/include/builder.h>

#include <library/cpp/testing/unittest/registar.h>

#include <algorithm>

template <>
void Out<maps::wiki::diffalert::tests::ParsedMessage>(
    IOutputStream& os, const maps::wiki::diffalert::tests::ParsedMessage& msg)
{
    maps::json::Builder b;
    b << [&msg](maps::json::ObjectBuilder b) {
        b["oid"] = msg.objectId;
        b["priority"] = msg.priorityStr;
        b["description"] = msg.description;
    };
    os << b.str();
}

namespace maps {
namespace wiki {
namespace diffalert {
namespace tests {

std::vector<ParsedMessage> messagesFromJson(const json::Value& json)
{
    std::vector<ParsedMessage> result;
    for (const auto& msgValue : json) {
        ParsedMessage msg{
            msgValue["oid"].as<TId>(),
            msgValue["priority"].toString(),
            msgValue["description"].toString()
        };
        result.push_back(std::move(msg));
    }
    return result;
}

void compare(
        std::vector<ParsedMessage> actual,
        std::vector<ParsedMessage> expected,
        const std::string& scope)
{
    std::sort(expected.begin(), expected.end());
    std::sort(actual.begin(), actual.end());

    std::vector<ParsedMessage> didNotGet;
    std::set_difference(
            expected.begin(), expected.end(),
            actual.begin(), actual.end(),
            std::back_inserter(didNotGet));

    std::vector<ParsedMessage> unexpected;
    std::set_difference(
            actual.begin(), actual.end(),
            expected.begin(), expected.end(),
            std::back_inserter(unexpected));

    for (const auto& msg : didNotGet) {
        UNIT_FAIL_NONFATAL("   did not get message: " << msg);
    }
    for (const auto& msg : unexpected) {
        UNIT_FAIL_NONFATAL("    unexpected message: " << msg);
    }

    UNIT_ASSERT_C(
        didNotGet.empty() && unexpected.empty(),
        "expected and actual messages do not match (" << scope << ')');
}

} // namespace tests
} // namespace diffalert
} // namespace wiki
} // namespace maps
