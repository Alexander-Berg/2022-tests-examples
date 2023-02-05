#include <maps/sprav/callcenter/libs/proto_tools/ut/schema.pb.h>
#include <maps/sprav/callcenter/libs/proto_tools/util.h>

#include <library/cpp/testing/gtest/gtest.h>

namespace maps::sprav::callcenter::tests {

TEST(UtilTest, SelectAll) {
    std::vector<std::string> result = getSelectAll<proto::ExampleRow>();
    std::vector<std::string> expected {
        "oneof_field as \"oneof_field\"",
        "oneof_type_field as \"oneof_type_field\"",
        "int_field as \"int_field\"",
        "string_field as \"string_field\"",
        "enum_int_field as \"enum_int_field\"",
        "enum_string_field as \"enum_string_field\"",
        "extract(epoch from timestamp_seconds_field) as \"timestamp_seconds_field\"",
        "extract(epoch from timestamp_milliseconds_field) as \"timestamp_milliseconds_field\"",
        "repeated_string_field as \"repeated_string_field\"",
        "message_json_field as \"message_json_field\"",
        "message_proto_field as \"message_proto_field\"",
        "message_proto_snappy_field as \"message_proto_snappy_field\""
    };

    EXPECT_EQ(result, expected);
}

TEST(UtilTest, SelectAllWithPrefix) {
    std::vector<std::string> result = getSelectAll<proto::ExampleRow>("test_table");
    std::vector<std::string> expected {
        "test_table.oneof_field as \"test_table.oneof_field\"",
        "test_table.oneof_type_field as \"test_table.oneof_type_field\"",
        "test_table.int_field as \"test_table.int_field\"",
        "test_table.string_field as \"test_table.string_field\"",
        "test_table.enum_int_field as \"test_table.enum_int_field\"",
        "test_table.enum_string_field as \"test_table.enum_string_field\"",
        "extract(epoch from test_table.timestamp_seconds_field) as \"test_table.timestamp_seconds_field\"",
        "extract(epoch from test_table.timestamp_milliseconds_field) as \"test_table.timestamp_milliseconds_field\"",
        "test_table.repeated_string_field as \"test_table.repeated_string_field\"",
        "test_table.message_json_field as \"test_table.message_json_field\"",
        "test_table.message_proto_field as \"test_table.message_proto_field\"",
        "test_table.message_proto_snappy_field as \"test_table.message_proto_snappy_field\""
    };

    EXPECT_EQ(result, expected);
}

} // maps::sprav::callcenter::tests
