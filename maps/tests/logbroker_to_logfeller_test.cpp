#include "gmock_json_matchers.h"

#include <maps/infra/yacare/include/logbroker/topic.h>
#include <maps/infra/yacare/include/logbroker/yson.h>
#include <maps/infra/yacare/include/logbroker/schema.h>
#include <maps/infra/yacare/inspect.h>
#include <maps/libs/json/include/value.h>
#include <maps/libs/json/include/builder.h>

#include <library/cpp/testing/unittest/registar.h>

#include <sstream>
#include <concepts>

namespace yacare::tests {

auto hasLinkToTopic(const maps::log8::Tag& tag, const maps::json::Value& config)
{
    return ::testing::AllOf(
        yacare::tests::hasField<std::string>("tag", tag.value()),
        yacare::tests::hasField<maps::json::Value>("config", config));
}

using Message = std::string;
using Temperature = int64_t;

YCR_LOGFELLER_FIELD(MessageField, message, Message)
YCR_LOGFELLER_FIELD(TemperatureField, temperature, Temperature)
YCR_LOGFELLER_FIELD(YsonField, yson, YsonType)

using TestLogFellerSchema = LogFellerSchema<
    MessageField,
    TemperatureField,
    YsonField>;

YCR_LOG_DECLARE(TAG, TestLogFellerSchema)

Y_UNIT_TEST_SUITE(test_logfeller_schema) {

Y_UNIT_TEST(config_linking_configuration)
{
    std::stringstream configStream;
    yacare::impl::inspect(configStream);

    maps::json::Value parsed{configStream};
    EXPECT_THAT(
        parsed["logbroker"]["topics"],
        ::testing::IsSupersetOf(
            {hasLinkToTopic(TAG, maps::json::Value::fromString(yacare::impl::logFellerConfig<TestLogFellerSchema>()))}));
}

Y_UNIT_TEST(yson_empty) {
    YsonField field;
    EXPECT_STREQ(
        (maps::json::Builder() << field.value()).str().c_str(),
        "{}"
    );
}

Y_UNIT_TEST(schema_fields_log)
{
    TestLogFellerSchema schema = {{
        {.message = "Teapot temperature is 10"},
        {.temperature = 10},
        {.yson = [](maps::json::ObjectBuilder b) { b["nested_field"] = 42; }},
    }};

    std::stringstream stream;
    stream << schema;
    auto actualValue = maps::json::Value::fromString(stream.str());
    EXPECT_TRUE(actualValue["timestamp"].exists());
    EXPECT_STREQ(actualValue["message"].as<std::string>().c_str(), "Teapot temperature is 10");
    EXPECT_EQ(actualValue["temperature"].as<int>(), 10);
    EXPECT_EQ(actualValue["yson"]["nested_field"].as<int>(), 42);
}

Y_UNIT_TEST(schema_fields_config)
{
    EXPECT_THAT(
        maps::json::Value::fromString(yacare::impl::logFellerConfig<TestLogFellerSchema>()),
        maps::json::Value::fromString(R"({
            "fields": [
                {
                    "name": "timestamp",
                    "type":"YT_TYPE_TIMESTAMP",
                    "demand": "optional"
                },
                {
                    "name": "message",
                    "type": "YT_TYPE_STRING",
                    "demand": "optional"
                },
                {
                    "name": "temperature",
                    "type": "YT_TYPE_INT64",
                    "demand": "optional"
                },
                {
                    "name": "yson",
                    "type": "YT_TYPE_YSON",
                    "demand": "optional"
                }
            ]
        })")
    );
}

} // Y_UNIT_TEST_SUITE(test_logfeller_schema)

} // namespace yacare::tests
