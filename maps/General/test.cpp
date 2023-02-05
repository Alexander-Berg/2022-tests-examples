#include "db_fixture.h"

#include <maps/sprav/callcenter/libs/proto_tools/ut/schema.pb.h>

#include <maps/sprav/callcenter/libs/proto_tools/parsing.h>
#include <maps/sprav/callcenter/libs/proto_tools/serialization.h>
#include <maps/sprav/callcenter/libs/proto_tools/util.h>

#include <maps/wikimap/mapspro/libs/query_builder/include/insert_query.h>
#include <maps/wikimap/mapspro/libs/query_builder/include/select_query.h>
#include <maps/wikimap/mapspro/libs/query_builder/include/update_query.h>

#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/testing/gtest_protobuf/matcher.h>


namespace maps::sprav::callcenter::tests {

namespace {
    auto createTransaction() {
        static DbFixture db;
        return db.pool().masterWriteableTransaction();
    }

    proto::ExampleRow createRow() {
        proto::ExampleRow row;
        row.set_int_field(1);
        row.set_string_field("str");
        row.set_enum_int_field(proto::ExampleRow::FIRST);
        row.set_enum_string_field(proto::ExampleRow::SECOND);
        row.set_timestamp_seconds_field(1640995200.123456);
        row.set_timestamp_milliseconds_field(1640995200123);
        row.add_repeated_string_field("str1");
        row.add_repeated_string_field("str2");
        row.mutable_message_json_field()->set_inner_string("inner_str");
        row.mutable_message_json_field()->set_inner_int(2);
        row.mutable_message_proto_field()->set_inner_string("inner_str2");
        row.mutable_message_proto_field()->set_inner_int(22);
        row.mutable_message_proto_snappy_field()->set_inner_string("inner_str_snappy");
        row.mutable_message_proto_snappy_field()->set_inner_int(333);
        row.mutable_oneof_first()->set_inner_string("inner_str_3");
        row.mutable_oneof_first()->set_inner_int(3);
        return row;
    }

    pqxx::result select(pqxx::transaction_base& tx, const std::set<std::string>& ids) {
        maps::wiki::query_builder::SelectQuery selectQuery(
            "proto_test.test_table",
            getSelectAll<proto::ExampleRow>(),
            maps::wiki::query_builder::WhereConditions()
                .keyInValues("int_field", ids)
        );
        return selectQuery.exec(tx);
    }

    void insert(pqxx::transaction_base& tx, const proto::ExampleRow& row) {
        maps::wiki::query_builder::InsertQuery insertQuery("proto_test.test_table");
        formatModifyQuery(row, insertQuery);
        insertQuery.exec(tx);
    }

    void update(pqxx::transaction_base& tx, const proto::ExampleRow& row) {
        maps::wiki::query_builder::UpdateQuery updateQuery(
            "proto_test.test_table",
            maps::wiki::query_builder::WhereConditions()
                .appendQuoted("int_field", ToString(row.int_field()))
        );
        formatModifyQuery(row, updateQuery);
        updateQuery.exec(tx);
    }
} // namespace

TEST(ProtoToolsTest, CreateAndGet) {
    auto tx = createTransaction();
    auto row = createRow();

    insert(*tx, row);
    auto parsedRow = parseSingleQueryResult<proto::ExampleRow>(select(*tx, {"1"}));

    EXPECT_THAT(row, NGTest::EqualsProto(parsedRow.value()));
}

TEST(ProtoToolsTest, CreateAndGetMultiple) {
    auto tx = createTransaction();
    auto row = createRow();
    auto row2 = createRow();
    row2.set_int_field(2);
    row2.mutable_oneof_second()->set_inner_double(345.1);

    insert(*tx, row);
    insert(*tx, row2);

    auto parsedRows = parseQueryResult<proto::ExampleRow>(select(*tx, {"1", "2"}));
    EXPECT_EQ(parsedRows.size(), 2ul);

    EXPECT_THAT(row, NGTest::EqualsProto(parsedRows[0]));
    EXPECT_THAT(row2, NGTest::EqualsProto(parsedRows[1]));
}

TEST(ProtoToolsTest, CreateAndGetWithPrefix) {
    auto tx = createTransaction();
    auto row = createRow();

    insert(*tx, row);

    auto result = maps::wiki::query_builder::SelectQuery(
        "proto_test.test_table",
        getSelectAll<proto::ExampleRow>("test_table"),
        maps::wiki::query_builder::WhereConditions()
            .append("test_table.int_field", "1")
    ).exec(*tx);

    auto parsedRow = parseSingleQueryResult<proto::ExampleRow>(result, { .fieldsPrefix = "test_table." });

    EXPECT_THAT(row, NGTest::EqualsProto(parsedRow.value()));
}


TEST(ProtoToolsTest, Update) {
    auto tx = createTransaction();
    auto row = createRow();

    insert(*tx, row);
    row.mutable_oneof_second()->set_inner_double(345.1);
    update(*tx, row);

    auto parsedRow = parseSingleQueryResult<proto::ExampleRow>(select(*tx, {"1"}));

    EXPECT_THAT(row, NGTest::EqualsProto(parsedRow.value()));
}

TEST(ProtoToolsTest, ParseRequestResult) {
    auto tx = createTransaction();
    tx->exec("INSERT INTO proto_test.lat VALUES ('id', 5)");
    tx->exec("INSERT INTO proto_test.lon VALUES ('id', 1)");

    auto row = tx->exec1(
        "SELECT lat.value as lat, lon.value as lon "
        "FROM proto_test.lat JOIN proto_test.lon USING(id)"
    );
    auto point = parseRow<proto::Point>(row);
    EXPECT_THAT(point.lat(), 5);
    EXPECT_THAT(point.lon(), 1);
}

} // maps::sprav::callcenter::tests
