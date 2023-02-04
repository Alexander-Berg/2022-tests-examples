// #include <ads/bigkv/preprocessor_primitives/base_preprocessor/common_preprocessors.h>
#include <ads/bigkv/preprocessor_primitives/yt/mapper_adapter.h>

#include <ads/bigkv/preprocessor_primitives/tests/test_helpers/user_proto_helpers.h>
#include <library/cpp/testing/unittest/registar.h>
#include <mapreduce/yt/tests/yt_unittest_lib/yt_unittest_lib.h>

#include <mapreduce/yt/interface/client.h>

#include <yabs/server/proto/keywords/keywords_data.pb.h>

#include "some_preprocessor.h"


namespace NProfilePreprocessing {

    REGISTER_NAMED_MAPPER("TMapperAdapter<TSomePreprocessor>", TMapperAdapter<TSomePreprocessor>);

    class TMapAdapterTests : public TTestBase {
    public:

        void CheckSchemaInference() {
            TString dstTable = "//tmp/CheckSchemaInference";
            RunMap(Client, Mapper, TablePath, dstTable);

            auto origSchema = Client->Get(TablePath + "/@schema");
            auto newSchema = Client->Get(dstTable + "/@schema");

            THashSet<TString> stringCols{"Profile", "OtherColumn", "FirstQuery", "Param"}, uint64Cols{"Timestamp", "Region", "TimestampParsed"}, doubleCols{"Counter4FirstVal"};
            UNIT_ASSERT_VALUES_EQUAL(newSchema.AsList().size(), stringCols.size() + uint64Cols.size() + doubleCols.size());

            for (const auto& column: newSchema.AsList()) {
                auto colName = column.AsMap().at("name").AsString();
                auto colType = column.AsMap().at("type_v3").AsString();
                UNIT_ASSERT(stringCols.contains(colName) || uint64Cols.contains(colName) || doubleCols.contains(colName));
                if (stringCols.contains(colName)) {
                    UNIT_ASSERT_VALUES_EQUAL(colType, "string");
                }
                if (uint64Cols.contains(colName)) {
                    UNIT_ASSERT_VALUES_EQUAL(colType, "uint64");
                }
                if (doubleCols.contains(colName)) {
                    UNIT_ASSERT_VALUES_EQUAL(colType, "double");
                }
            }
        }

        void CheckRunMap() {
            TString dstTable = "//tmp/CheckRunMap";
            RunMap(Client, Mapper, TablePath, dstTable);
            auto reader = Client->CreateTableReader<NYT::TNode>(dstTable);
            THashSet<ui64> is;
            for (auto& cursor: *reader) {
                const auto &row = cursor.GetRow();
                auto i = row["Timestamp"].AsUint64();
                is.insert(i);
                UNIT_ASSERT_UNEQUAL(row["Profile"].AsString().size(), 0);
                UNIT_ASSERT_VALUES_EQUAL(row["OtherColumn"].AsString(), "qewrer" + ToString(i));

                UNIT_ASSERT_VALUES_EQUAL(row["FirstQuery"].AsString(), "My " + ToString(i) + " query");
                UNIT_ASSERT_DOUBLES_EQUAL(row["Counter4FirstVal"].AsDouble(), i + 7, 0.00001);
                UNIT_ASSERT_VALUES_EQUAL(row["Region"].AsUint64(), i);
                UNIT_ASSERT_VALUES_EQUAL(row["TimestampParsed"].AsUint64(), i);
                UNIT_ASSERT_VALUES_EQUAL(row["Param"].AsString(), "asdfg");

            }

            for (ui64 i = 0; i < 10; ++i) {
                UNIT_ASSERT(is.contains(i));
            }
        }

        void SetUp() override {
            Client = NYT::NTesting::CreateTestClient();

            {
                auto schema = NYT::TNode::CreateList({
                    NYT::TNode()("name", "Profile")("type_v3", NYT::TNode()("type_name", "string")),
                    NYT::TNode()("name", "Timestamp")("type_v3", NYT::TNode()("type_name", "uint64")),
                    NYT::TNode()("name", "OtherColumn")("type_v3", NYT::TNode()("type_name", "string"))
                });
                TablePath = "//tmp/table";
                if (Client->Exists(TablePath)) {
                    Client->Remove(TablePath);
                }
                Client->Create(TablePath, NYT::NT_TABLE, NYT::TCreateOptions().Attributes(
                    NYT::TNode()("schema", schema)
                ));
            }

            {
                auto writer = Client->CreateTableWriter<NYT::TNode>(NYT::TRichYPath(TablePath).Append(true));
                for (ui32 i = 0; i < 10; ++i) {
                    TUserProtoBuilder profileBuilder;
                    profileBuilder.AddItem(NBSData::NKeywords::KW_USER_REGION, {i});
                    profileBuilder.AddCounter(4, {4, 5, 6}, {7. + i, 8, 9});
                    profileBuilder.AddQuery("My " + ToString(i) + " query");

                    writer->AddRow(
                        NYT::TNode()
                            ("Profile", profileBuilder.GetDump())
                            ("Timestamp", i)
                            ("OtherColumn", "qewrer" + ToString(i))
                    );
                }
            }

            {
                auto preprocessor = TSomePreprocessor("asdfg");
                auto premap = [](NYT::TNode &row, TMutableProfilesPack &profiles, TArgs &args) {
                    Y_PROTOBUF_SUPPRESS_NODISCARD (*profiles.UserProfile).ParseFromString(row["Profile"].AsString());
                    args.Timestamp = row["Timestamp"].AsUint64();
                };
                Mapper = TIntrusivePtr(
                    new TMapperAdapter(preprocessor, premap)
                );
            }
        }

    private:
        NYT::IClientPtr Client;
        TString TablePath;
        TIntrusivePtr<TMapperAdapter<TSomePreprocessor>> Mapper;

        UNIT_TEST_SUITE(TMapAdapterTests);
        UNIT_TEST(CheckSchemaInference);
        UNIT_TEST(CheckRunMap);
        UNIT_TEST_SUITE_END();
    };

    UNIT_TEST_SUITE_REGISTRATION(TMapAdapterTests);
}
