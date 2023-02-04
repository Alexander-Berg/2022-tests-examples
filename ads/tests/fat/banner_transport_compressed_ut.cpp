#include <ads/tensor_transport/lib_2/difacto_model.h>
#include <ads/tensor_transport/lib_2/lemmer_fixlist.h>
#include <ads/tensor_transport/lib_2/feature_computer.h>
#include <ads/tensor_transport/yt_lib/banner_transport_compressed.h>
#include <ads/quality/lib/ytutils/ytutils.h>
#include <mapreduce/yt/interface/operation.h>
#include <library/cpp/testing/unittest/registar.h>
#include <mapreduce/yt/interface/client.h>
#include <util/system/fs.h>
#include <util/system/env.h>
#include "yabs/proto/tsar.pb.h"


using namespace NTsarTransport;

using NYT::TTableReader;
using NYT::TTableWriter;
using NYT::TNodeWriter;
using NYT::TNode;
using NYT::IClientPtr;
using NYT::NT_TABLE;
using NYT::IMapper;
using NYT::TMessageReader;
using NYT::TMessageWriter;
using NYT::IReducer;
using NYT::IClientBasePtr;
using NYT::TRichYPath;
using NYT::TMapOperationSpec;


REGISTER_MAPPER(TBannerTransportCompressedMapper<TFactorizationBannerAdapter<TModel>>);


class TBannerTransportCompressedTest : public TTestBase {
public:
    void SetUp() override {
        TString ytProxy = GetEnv("YT_PROXY");

        Client = NYT::CreateClient(ytProxy);
        TString source = "//tmp/table";
        ResultTable = "//tmp/resultTable";
        auto writer = Client->CreateTableWriter<NYT::TNode>(source);
        i64 groupExportID = 123;
        i64 offset = 12;
        ui64 version = 1;
        ui64 vectorSize = BannerVector.size() + 1;

        TVector<NYT::TNode> records = {
                NYT::TNode()("BannerID", 14)("GroupExportID", groupExportID)("IsDisabledInRsya", false)("IsActive", true)("IsArchive", false),
                NYT::TNode()("BannerID", 14 + offset)("GroupExportID", groupExportID + offset)("IsDisabledInRsya", false)("IsActive", true)("IsArchive", false),
                NYT::TNode()("BannerID", 14 + offset)("GroupExportID", groupExportID)("IsDisabledInRsya", true)("IsActive", true)("IsArchive", false),
                NYT::TNode()("BannerID", 14 + offset)("GroupExportID", groupExportID)("IsDisabledInRsya", false)("IsActive", false)("IsArchive", true)
        };

        for (const auto& record: records) {
            writer->AddRow(record);
        }
        writer->Finish();
        THashMap<ui64, TVector<float>> table;
        table[9395869041812228098ULL] = BannerVector;
        THashSet<ui64> goodExportID = {(ui64)groupExportID};
        TComputer<TModel> computer(MakeHolder<TModel>(std::move(table)));
        THashMap<TString, THashSet<TString>> lemmerFixlist;

        for (const auto& [word, valueList] : kLemmerFixlist) {
            THashSet<TString> index = StringSplitter(valueList).Split(',').SkipEmpty();
            lemmerFixlist.emplace(word, std::move(index));
        }
        TFactorizationBannerAdapter<TModel> adapter(std::move(computer), lemmerFixlist);
        auto spec = TMapOperationSpec()
                .AddInput<TensorTransport::TBannerRecord>(source)
                .AddOutput<TNode>(ResultTable);
        float minValue = 3.0;
        float maxValue = 5.0;
        Client->Map(
                spec,
                new TBannerTransportCompressedMapper<TFactorizationBannerAdapter<TModel>>(
                        vectorSize,
                        version,
                        std::move(goodExportID),
                        std::move(adapter),
                        minValue,
                        maxValue,
                        true
                )
        );

    }

    void TearDown() override {
        Client->Remove(ResultTable, NYT::TRemoveOptions().Recursive(true));
    }

    void ApplyVectorTest() {
        auto tableReader = Client->CreateTableReader<NYT::TNode>(ResultTable);
        ui16 readedRecords = 0;
        for (; tableReader->IsValid(); tableReader->Next())
        {
            auto record = tableReader->GetRow();
            yabs::proto::NTsar::TTsarCompressedVector vector;
            const TString& strVector = record.ChildAsString("Vector");
            Y_PROTOBUF_SUPPRESS_NODISCARD vector.ParseFromString(strVector);
            TString factors = vector.GetFactors();
            const ui16 * parsedArray = reinterpret_cast<const ui16*>(factors.data());

            ui16 expectedValue = std::numeric_limits<ui16>::max();

            ui16 actualValue = *parsedArray;
            UNIT_ASSERT_VALUES_EQUAL(0, actualValue);

            actualValue = *(++parsedArray);
            UNIT_ASSERT_VALUES_EQUAL(expectedValue, actualValue);
            actualValue = *(++parsedArray);
            UNIT_ASSERT_VALUES_EQUAL(0, actualValue);
            actualValue = *(++parsedArray);
            UNIT_ASSERT(actualValue != 0 && actualValue != std::numeric_limits<ui16>::max());
            actualValue = *(++parsedArray);
            UNIT_ASSERT_VALUES_EQUAL(0, actualValue);
            actualValue = *(++parsedArray);
            UNIT_ASSERT_VALUES_EQUAL(std::numeric_limits<ui16>::max(), actualValue);
            ++readedRecords;
        }
        UNIT_ASSERT(readedRecords > 0);
    }

private:
    UNIT_TEST_SUITE(TBannerTransportCompressedTest);
        UNIT_TEST(ApplyVectorTest);
    UNIT_TEST_SUITE_END();
    TString ResultTable;
    NYT::IClientPtr Client;
    TVector<float> BannerVector = {5.0, 3.0, 4.5, -5.0, 10.0};

};
UNIT_TEST_SUITE_REGISTRATION(TBannerTransportCompressedTest);


class TBannerTransportCompressedWithoutGenocideTest : public TTestBase {
public:
    void SetUp() override {
        TString ytProxy = GetEnv("YT_PROXY");

        Client = NYT::CreateClient(ytProxy);
        TString source = "//tmp/table";
        ResultTable = "//tmp/resultTable";
        auto writer = Client->CreateTableWriter<NYT::TNode>(source);
        i64 groupExportID = 123;
        i64 offset = 12;
        ui64 version = 1;
        ui64 vectorSize = BannerVector.size() + 1;

        TVector<NYT::TNode> records = {
                NYT::TNode()("BannerID", 14)("GroupExportID", groupExportID)("IsDisabledInRsya", false)("IsActive", true)("IsArchive", false),
                NYT::TNode()("BannerID", 14 + offset)("GroupExportID", groupExportID + offset)("IsDisabledInRsya", false)("IsActive", true)("IsArchive", false),
        };

        for (const auto& record: records) {
            writer->AddRow(record);
        }
        Banners = records.size();
        writer->Finish();
        THashMap<ui64, TVector<float>> table;
        table[9395869041812228098ULL] = BannerVector;
        THashSet<ui64> goodExportID = {};
        TComputer<TModel> computer(MakeHolder<TModel>(std::move(table)));
        THashMap<TString, THashSet<TString>> lemmerFixlist;

        for (const auto& [word, valueList] : kLemmerFixlist) {
            THashSet<TString> index = StringSplitter(valueList).Split(',').SkipEmpty();
            lemmerFixlist.emplace(word, std::move(index));
        }
        TFactorizationBannerAdapter<TModel> adapter(std::move(computer), lemmerFixlist);
        auto spec = TMapOperationSpec()
                .AddInput<TensorTransport::TBannerRecord>(source)
                .AddOutput<TNode>(ResultTable);
        float minValue = 3.0;
        float maxValue = 5.0;
        Client->Map(
                spec,
                new TBannerTransportCompressedMapper<TFactorizationBannerAdapter<TModel>>(
                        vectorSize,
                        version,
                        std::move(goodExportID),
                        std::move(adapter),
                        minValue,
                        maxValue,
                        true
                )
        );

    }

    void TearDown() override {
        Client->Remove(ResultTable, NYT::TRemoveOptions().Recursive(true));
    }

    void ApplyVectorTest() {
        auto tableReader = Client->CreateTableReader<NYT::TNode>(ResultTable);
        auto readedRecords = GetTableSize(Client, ResultTable);
        UNIT_ASSERT(readedRecords == Banners);
    }

private:
    UNIT_TEST_SUITE(TBannerTransportCompressedWithoutGenocideTest);
        UNIT_TEST(ApplyVectorTest);
    UNIT_TEST_SUITE_END();
    size_t Banners;
    TString ResultTable;
    NYT::IClientPtr Client;
    TVector<float> BannerVector = {5.0, 3.0, 4.5, -5.0, 10.0};

};
UNIT_TEST_SUITE_REGISTRATION(TBannerTransportCompressedWithoutGenocideTest);


class TBannerTransportCompressedWithoutFilterActiveBanners : public TTestBase {
public:
    void SetUp() override {
        TString ytProxy = GetEnv("YT_PROXY");

        Client = NYT::CreateClient(ytProxy);
        TString source = "//tmp/table";
        ResultTable = "//tmp/resultTable";
        auto writer = Client->CreateTableWriter<NYT::TNode>(source);
        i64 groupExportID = 123;
        i64 offset = 12;
        ui64 version = 1;
        ui64 vectorSize = BannerVector.size() + 1;

        TVector<NYT::TNode> records = {
                NYT::TNode()("BannerID", 14)("GroupExportID", groupExportID)("IsDisabledInRsya", false)("IsActive",
                                                                                                        false)("IsArchive", false),
                NYT::TNode()("BannerID", 14 + offset)("GroupExportID", groupExportID + offset)("IsDisabledInRsya", false)("IsActive", true)("IsArchive", false),
        };

        for (const auto& record: records) {
            writer->AddRow(record);
        }
        Banners = records.size();
        writer->Finish();
        THashMap<ui64, TVector<float>> table;
        table[9395869041812228098ULL] = BannerVector;
        THashSet<ui64> goodExportID = {};
        TComputer<TModel> computer(MakeHolder<TModel>(std::move(table)));
        THashMap<TString, THashSet<TString>> lemmerFixlist;

        for (const auto& [word, valueList] : kLemmerFixlist) {
            THashSet<TString> index = StringSplitter(valueList).Split(',').SkipEmpty();
            lemmerFixlist.emplace(word, std::move(index));
        }
        TFactorizationBannerAdapter<TModel> adapter(std::move(computer), lemmerFixlist);
        auto spec = TMapOperationSpec()
                .AddInput<TensorTransport::TBannerRecord>(source)
                .AddOutput<TNode>(ResultTable);
        float minValue = 3.0;
        float maxValue = 5.0;
        Client->Map(
                spec,
                new TBannerTransportCompressedMapper<TFactorizationBannerAdapter<TModel>>(
                        vectorSize,
                        version,
                        std::move(goodExportID),
                        std::move(adapter),
                        minValue,
                        maxValue,
                        false
                )
        );

    }

    void TearDown() override {
        Client->Remove(ResultTable, NYT::TRemoveOptions().Recursive(true));
    }

    void ApplyVectorTest() {
        auto tableReader = Client->CreateTableReader<NYT::TNode>(ResultTable);
        auto readedRecords = GetTableSize(Client, ResultTable);
        UNIT_ASSERT(readedRecords == Banners);
    }

private:
    UNIT_TEST_SUITE(TBannerTransportCompressedWithoutFilterActiveBanners);
        UNIT_TEST(ApplyVectorTest);
    UNIT_TEST_SUITE_END();
    size_t Banners;
    TString ResultTable;
    NYT::IClientPtr Client;
    TVector<float> BannerVector = {5.0, 3.0, 4.5, -5.0, 10.0};

};
UNIT_TEST_SUITE_REGISTRATION(TBannerTransportCompressedWithoutFilterActiveBanners);



class TBannerTransportCompressedFilterSearch : public TTestBase {
public:
    void SetUp() override {
        TString ytProxy = GetEnv("YT_PROXY");

        Client = NYT::CreateClient(ytProxy);
        TString source = "//tmp/searchInputTable";
        ResultTable = "//tmp/resultSearchTable";
        auto writer = Client->CreateTableWriter<NYT::TNode>(source);
        i64 groupExportID = 123;
        i64 offset = 12;
        ui64 version = 1;
        ui64 vectorSize = BannerVector.size() + 1;

        TVector<NYT::TNode> records = {
                NYT::TNode()
                    ("BannerID", 14)
                    ("GroupExportID", groupExportID)
                    ("IsDisabledInRsya", true)
                    ("IsActive",false)
                    ("IsArchive", false)
                    ("IsDisabledInSearch", false),
                NYT::TNode()
                    ("BannerID", 14 + offset)
                    ("GroupExportID", groupExportID + offset)
                    ("IsDisabledInRsya", true)
                    ("IsActive", true)
                    ("IsArchive", false)
                    ("IsDisabledInSearch", false)
        };

        for (const auto& record: records) {
            writer->AddRow(record);
        }
        Banners = records.size();
        writer->Finish();
        THashMap<ui64, TVector<float>> table;
        table[9395869041812228098ULL] = BannerVector;
        THashSet<ui64> goodExportID = {};
        TComputer<TModel> computer(MakeHolder<TModel>(std::move(table)));
        THashMap<TString, THashSet<TString>> lemmerFixlist;

        for (const auto& [word, valueList] : kLemmerFixlist) {
            THashSet<TString> index = StringSplitter(valueList).Split(',').SkipEmpty();
            lemmerFixlist.emplace(word, std::move(index));
        }
        TFactorizationBannerAdapter<TModel> adapter(std::move(computer), lemmerFixlist);
        auto spec = TMapOperationSpec()
                .AddInput<TensorTransport::TBannerRecord>(source)
                .AddOutput<TNode>(ResultTable);
        float minValue = 3.0;
        float maxValue = 5.0;
        TensorTransport::TModel model;
        model.SetModelType(TensorTransport::EModelType::DSSM);
        model.SetMinValue(minValue);
        model.SetMaxValue(maxValue);
        model.SetVersion(version);
        model.SetVectorSize(vectorSize);
        model.SetModelDestination(TensorTransport::EModelDestination::Search);
        Client->Map(
                spec,
                new TBannerTransportCompressedMapper<TFactorizationBannerAdapter<TModel>>(
                        std::move(goodExportID),
                        std::move(adapter),
                        model,
                        false
                )
        );

    }

    void TearDown() override {
        Client->Remove(ResultTable, NYT::TRemoveOptions().Recursive(true));
    }

    void ApplyVectorTest() {
        auto tableReader = Client->CreateTableReader<NYT::TNode>(ResultTable);
        auto readedRecords = GetTableSize(Client, ResultTable);
        UNIT_ASSERT(readedRecords == Banners);
    }

private:
    UNIT_TEST_SUITE(TBannerTransportCompressedFilterSearch);
        UNIT_TEST(ApplyVectorTest);
    UNIT_TEST_SUITE_END();
    size_t Banners;
    TString ResultTable;
    NYT::IClientPtr Client;
    TVector<float> BannerVector = {5.0, 3.0, 4.5, -5.0, 10.0};

};
UNIT_TEST_SUITE_REGISTRATION(TBannerTransportCompressedFilterSearch);
