#pragma once

#include <contrib/libs/jwt-cpp/include/jwt-cpp/jwt.h>

namespace maps::b2bgeo::jwt {

class SimplePayloadType {
public:
    static constexpr auto TYPE = "simple_payload";

    static SimplePayloadType fromPayloadJson(const picojson::object& value)
    {
        return SimplePayloadType(value);
    }

    SimplePayloadType() = default;

    explicit SimplePayloadType(const picojson::object& value) : value_(value)
    { }

    picojson::object serialize() const
    {
        picojson::object subject;
        subject["type"] = picojson::value(SimplePayloadType::TYPE);
        subject["value"] = picojson::value(value_);

        picojson::object payloadObj;

        payloadObj["subject"] = picojson::value(std::move(subject));

        return payloadObj;
    }

private:
    picojson::object value_;
};

} // namespace maps::b2bgeo::jwt
