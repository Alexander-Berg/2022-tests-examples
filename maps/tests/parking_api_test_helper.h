#pragma once

#include <maps/libs/json/include/builder.h>
#include <maps/libs/json/include/value.h>

#include <string>

namespace maps::automotive::parking::tests {

inline std::function<void (json::ObjectBuilder)> errorJson(int code, const std::string& message)
{
    return [code, &message](maps::json::ObjectBuilder builder) {
        builder["error"] << [code, &message](maps::json::ObjectBuilder builder) {
            builder["code"] = maps::json::Value(code);
            builder["message"] = maps::json::Value(message);
        };
    };
}

} //maps::automotive::parking::tests
