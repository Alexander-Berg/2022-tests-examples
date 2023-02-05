#pragma once
#include <yandex/maps/wiki/diffalert/common.h>
#include <yandex/maps/wiki/diffalert/message.h>

#include <maps/libs/json/include/value.h>
#include <maps/libs/introspection/include/comparison.h>

#include <iostream>
#include <string>
#include <vector>

namespace maps {
namespace wiki {
namespace diffalert {
namespace tests {

struct ParsedMessage
{
    TId objectId;
    std::string priorityStr;
    std::string description;
};

inline auto introspect(const ParsedMessage& message)
{
    return std::tie(message.objectId, message.priorityStr, message.description);
}
using maps::introspection::operator <;

std::vector<ParsedMessage> messagesFromJson(const json::Value& json);

template<typename Run, typename Diffs>
std::vector<ParsedMessage> messagesFromOutput(Run run, const Diffs& diffs)
{
    std::vector<ParsedMessage> result;
    for (const auto& diff: diffs) {
        for (const auto& outputMsg : run(diff)) {
            const auto& priority = outputMsg.priority();
            ParsedMessage msg{
                outputMsg.objectId(),
                std::to_string(priority.major) + "." + std::to_string(priority.minor),
                outputMsg.description()
            };
            result.push_back(std::move(msg));
        }
    }
    return result;
}

void compare(
        std::vector<ParsedMessage> actual,
        std::vector<ParsedMessage> expected,
        const std::string& scope);

} // namespace tests
} // namespace diffalert
} // namespace wiki
} // namespace maps
