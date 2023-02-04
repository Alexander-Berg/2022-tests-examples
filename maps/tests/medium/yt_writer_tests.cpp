#include <maps/indoor/long-tasks/src/pg-dumper/lib/yt_writer.h>

#include <library/cpp/testing/unittest/gtest.h>
#include <mapreduce/yt/tests/yt_unittest_lib/yt_unittest_lib.h>
#include <maps/libs/common/include/exception.h>

using namespace NYT::NTesting;

namespace maps::mirc::pg_dumper {

struct TestStruct{
    int32_t someNumber;
    std::string someText;
};

template<>
NYT::TAlterTableOptions getSchemaOptions<TestStruct>()
{
    return NYT::TAlterTableOptions()
        .Schema(
            NYT::TTableSchema()
                .Strict(true)
                .AddColumn("someNumber", NYT::EValueType::VT_INT64)
                .AddColumn("someText", NYT::EValueType::VT_STRING)
                .SortBy({"someNumber"})
    );
}

template<>
NYT::TNode toNode<TestStruct>(const TestStruct& testStruct) {
    NYT::TNode res;
    res["someNumber"] = testStruct.someNumber;
    res["someText"] = NYT::TNode(testStruct.someText);
    return res;
}

} // namespace maps::mirc::pg_dumper

namespace maps::mirc::pg_dumper::tests {

namespace {

NYT::IClientPtr getTestYTClientPtr()
{
    static const auto ytClient = CreateTestClient();
    return ytClient;
}

} // namespace

TEST(TestSetSchema, NoThrow)
{
    const auto table = CreateTestDirectory(getTestYTClientPtr()) + "/someTable";
    {
        const auto client = getTestYTClientPtr();
        client->Create(TString(table), NYT::NT_TABLE);
    }
    EXPECT_NO_THROW(
        setSchema<TestStruct>(table, getTestYTClientPtr())
    );
}

TEST(TestSetSchema, WriteCorrectDataNoThrow)
{
    const auto table = CreateTestDirectory(getTestYTClientPtr()) + "/someTable";
    {
        const auto client = getTestYTClientPtr();
        client->Create(TString(table), NYT::NT_TABLE);
        setSchema<TestStruct>(table, getTestYTClientPtr());
    }

    const auto client = getTestYTClientPtr();
    {
        auto writer = client->CreateTableWriter<NYT::TNode>(TString(table));
        NYT::TNode row;
        row["someNumber"] = 42;
        row["someText"] = "TEXT";
        EXPECT_NO_THROW(writer->AddRow(row));
        EXPECT_NO_THROW(writer->Finish());
    }
}

TEST(TestSetSchema, ThrowOnWrongType)
{
    const auto table = CreateTestDirectory(getTestYTClientPtr()) + "/someTable";
    {
        const auto client = getTestYTClientPtr();
        client->Create(TString(table), NYT::NT_TABLE);
        setSchema<TestStruct>(TString(table), getTestYTClientPtr());
    }

    const auto client = getTestYTClientPtr();
    {
        auto writer = client->CreateTableWriter<NYT::TNode>(TString(table));
        NYT::TNode row;
        row["someNumber"] = "42"; // wrong type
        row["someText"] = "TEXT";
        EXPECT_NO_THROW(writer->AddRow(row));
        EXPECT_THROW(writer->Finish(), NYT::TErrorResponse);
    }
}

TEST(TestSetSchema, ThrowOnWrongColumn)
{
    const auto table = CreateTestDirectory(getTestYTClientPtr()) + "/someTable";
    {
        const auto client = getTestYTClientPtr();
        client->Create(TString(table), NYT::NT_TABLE);
        setSchema<TestStruct>(TString(table), getTestYTClientPtr());
    }

    const auto client = getTestYTClientPtr();
    {
        auto writer = client->CreateTableWriter<NYT::TNode>(TString(table));
        NYT::TNode row;
        row["someNumber"] = 42;
        row["someText"] = "TEXT";
        row["nonExistentColumn"] = 17; // wrong column name
        EXPECT_NO_THROW(writer->AddRow(row));
        EXPECT_THROW(writer->Finish(), NYT::TErrorResponse);
    }
}

TEST(TestYTWriter, ThrowsOnClientNullptr)
{
    const auto table = CreateTestDirectory(getTestYTClientPtr()) + "/someTable";
    EXPECT_THROW(
        YTWriter(nullptr, table),
        maps::RuntimeError
    );
}

TEST(TestYTWriter, ThrowWhenPathDoesntExist)
{
    const auto table = CreateTestDirectory(getTestYTClientPtr())
        + "/nonExistentSubDir" + "/someTable";
    EXPECT_THROW(YTWriter(getTestYTClientPtr(), TString(table)), maps::RuntimeError);
}

TEST(TestYTWriter, WriteBatchNoThrow)
{

    const auto client = getTestYTClientPtr();
    const auto table = CreateTestDirectory(client) + "/someTable";
    {
        client->Create(TString(table), NYT::ENodeType::NT_TABLE);
        setSchema<TestStruct>(table, client);
    }

    const auto batch = Batch<TestStruct>{
        TestStruct{1, "one"},
        TestStruct{2, "two"},
        TestStruct{3, "three"},
        TestStruct{4, "four"},
    };
    {
        auto writer = YTWriter(client, TString(table));
        EXPECT_NO_THROW(writer.write(batch));
    }
    {
        auto reader = client->CreateTableReader<NYT::TNode>(TString(table));

        ASSERT_TRUE(reader->IsValid() && !reader->IsEndOfStream());

        size_t counter = 0;
        for (auto& cursor : *reader) {
            auto& row = cursor.GetRow();
            EXPECT_EQ(row["someNumber"].AsInt64(), batch[counter].someNumber);
            EXPECT_EQ(row["someText"].AsString(), batch[counter].someText);
            ++counter;
        }
        EXPECT_EQ(counter, batch.size());
    }
}

} // namespace maps::mirc::pg_dumper::tests
