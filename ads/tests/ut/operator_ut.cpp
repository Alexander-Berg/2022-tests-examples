#include <ads/bsyeti/big_rt/lib/serializable_profile/operator.h>

#include <yt/yt/client/table_client/row_buffer.h>
#include <yt/yt/client/table_client/schema.h>

#include <library/cpp/testing/gtest/gtest.h>

#include <vector>

namespace {

    using NYT::NYTree::ConvertTo;
    using NYT::NYson::TYsonString;
    using namespace NYT::NTableClient;
    using namespace NYT::NTransactionClient;

    TVersionedRow BuildVersionedRow(TRowBufferPtr buffer,
                                    const TString& keyYson,
                                    const TString& valueYson,
                                    const std::vector<TTimestamp>& deleteTimestamps = {})
    {
        return YsonToVersionedRow(
            buffer,
            keyYson,
            valueYson,
            deleteTimestamps);
    }

    TTableSchemaPtr BuildSimpleSchema(int totalItems, bool hasAggregate = false) {
        auto yson = TString("<unique_keys=%false;strict=%true>[");
        for (int i = 0; i < totalItems; ++i) {
            yson += "{name=k" + ToString(i) + ";type=int64";
            if (i + 1 == totalItems && hasAggregate) {
                yson += ";aggregate=sum";
            }
            yson += "};";
        }
        yson += "]";
        return NYT::New<TTableSchema>(ConvertTo<TTableSchema>(TYsonString(yson)));
    }

    TUnversionedRow BuildUnversionedRow(TRowBufferPtr buffer, const TString& valueYson) {
        auto row = YsonToSchemalessRow(valueYson);
        return buffer->CaptureRow(row);
    }

}


TEST(ToUnversionedRow, Empty) {
    const auto buffer = NYT::New<TRowBuffer>();
    const auto schema = BuildSimpleSchema(0);

    auto row = BuildVersionedRow(buffer, "", "");

    EXPECT_EQ(
        NBigRT::ToUnversionedRow(buffer, schema, row, NullTimestamp),
        TUnversionedRow(nullptr));
}

TEST(ToUnversionedRow, Simple) {
    const auto buffer = NYT::New<TRowBuffer>();
    const auto schema = BuildSimpleSchema(2);

    auto row = BuildVersionedRow(buffer, "<id=0> 0", "<id=1;ts=200> 1");
    EXPECT_EQ(
        NBigRT::ToUnversionedRow(buffer, schema, row, NullTimestamp),
        BuildUnversionedRow(buffer, "<id=0> 0; <id=1> 1"));
}

TEST(ToUnversionedRow, AbsentValue) {
    const auto buffer = NYT::New<TRowBuffer>();
    const auto schema = BuildSimpleSchema(3);

    auto row = BuildVersionedRow(buffer, "<id=0> 0", "<id=1;ts=200> 1");
    EXPECT_EQ(
        NBigRT::ToUnversionedRow(buffer, schema, row, NullTimestamp),
        BuildUnversionedRow(buffer, "<id=0> 0; <id=1> 1; <id=2> #"));
}

TEST(ToUnversionedRow, SkipByDeleteTimestamps) {
    const auto buffer = NYT::New<TRowBuffer>();
    const auto schema = BuildSimpleSchema(3);

    auto row = BuildVersionedRow(buffer, "<id=0> 0", "<id=1;ts=200> 1; <id=2;ts=400> 2", {300});
    EXPECT_EQ(
        NBigRT::ToUnversionedRow(buffer, schema, row, NullTimestamp),
        BuildUnversionedRow(buffer, "<id=0> 0; <id=1> #; <id=2> 2"));

    row = BuildVersionedRow(buffer, "<id=0> 0", "<id=1;ts=200> 1; <id=2;ts=400> 2", {400});
    EXPECT_EQ(
        NBigRT::ToUnversionedRow(buffer, schema, row, NullTimestamp),
        BuildUnversionedRow(buffer, "<id=0> 0; <id=1> #; <id=2> #"));

}

TEST(ToUnversionedRow, SkipByRetentionTimestamp) {
    const auto buffer = NYT::New<TRowBuffer>();
    const auto schema = BuildSimpleSchema(3);

    auto row = BuildVersionedRow(buffer, "<id=0> 0", "<id=1;ts=200> 1; <id=2;ts=400> 2");
    EXPECT_EQ(
        NBigRT::ToUnversionedRow(buffer, schema, row, 300),
        BuildUnversionedRow(buffer, "<id=0> 0; <id=1> #; <id=2> 2"));

    row = BuildVersionedRow(buffer, "<id=0> 0", "<id=1;ts=200> 1; <id=2;ts=400> 2");
    EXPECT_EQ(
        NBigRT::ToUnversionedRow(buffer, schema, row, 500),
        TUnversionedRow(nullptr));

}

TEST(ToUnversionedRow, AggregatesNotSupported) {
    const auto buffer = NYT::New<TRowBuffer>();
    const auto schema = BuildSimpleSchema(3, /*hasAggregates=*/true);

    auto row = BuildVersionedRow(
        buffer,
        "<id=0> 0", "<id=1;ts=200> 1; <id=2;aggregate=true;ts=400> 2");
    EXPECT_THROW_MESSAGE_HAS_SUBSTR(
        NBigRT::ToUnversionedRow(buffer, schema, row, NullTimestamp),
        std::exception,
        "Aggregate columns are not supported");
}
