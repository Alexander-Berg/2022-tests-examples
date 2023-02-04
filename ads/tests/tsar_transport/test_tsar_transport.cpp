#include <ads/tensor_transport/proto/banner.pb.h>
#include <ads/tensor_transport/yt_lib/tsar_transport.h>

#include <library/cpp/testing/unittest/registar.h>

#include <mapreduce/yt/interface/client.h>
#include <mapreduce/yt/interface/serialize.h>
#include <mapreduce/yt/tests/yt_unittest_lib/yt_unittest_lib.h>

#include <util/generic/hash.h>
#include <util/generic/set.h>
#include <util/generic/yexception.h>
#include <util/system/env.h>

using namespace NTsarTransport;

class TCopyBannerIDMapper : public NYT::IMapper<NYT::TTableReader<TensorTransport::TBannerRecord>, NYT::TTableWriter<NYT::TNode>> {
public:
    TCopyBannerIDMapper() = default;

    TCopyBannerIDMapper(ui64 version): Version(version) {}

    void Do(TReader *reader, TWriter *writer) override {
        for (; reader->IsValid(); reader->Next()) {
            auto row = reader->GetRow();
            NYT::TNode node;
            node["BannerID"] = row.GetBannerID();
            node["Version"] = Version;
            writer->AddRow(node);
            NYT::WriteCustomStatistics("banners/banners_count", 0);
            NYT::WriteCustomStatistics("banners/processed_banners", 0);
        }
    }

    Y_SAVELOAD_JOB(Version);

private:
    ui64 Version;
};

class TFailMapper : public NYT::IMapper<NYT::TTableReader<TensorTransport::TBannerRecord>, NYT::TTableWriter<NYT::TNode>> {
public:
    TFailMapper() = default;

    void Do(TReader *, TWriter *) override {
        ythrow yexception();
    }
};

REGISTER_NAMED_MAPPER("CopyBannerID", TCopyBannerIDMapper);
REGISTER_NAMED_MAPPER("FailMapper", TFailMapper);

class TTsarTransportTest : public TTestBase {
public:
    UNIT_TEST_SUITE(TTsarTransportTest);
    UNIT_TEST(TestTwoBannerVersionsWork);
    UNIT_TEST(TestOlderVersionUpdated);
    UNIT_TEST(TestAddingBrokenVersionDoesNotBrakeOlderVersion);
    UNIT_TEST(TestSizeValidationFailUsesOlderVersion);
    UNIT_TEST_EXCEPTION(TestAllVersionFailWithoutBackup, TNoTablesReadyError);
    UNIT_TEST_SUITE_END();

    void TestTwoBannerVersionsWork() {
        TTsarTransport::TMappers mappers;
        mappers.emplace(1ULL, new TCopyBannerIDMapper(1ULL));
        mappers.emplace(2ULL, new TCopyBannerIDMapper(2ULL));
        TTsarTransport transport(
                Client,
                mappers,
                ScoringDataSizePerJobGB,
                ScoringMapperMemoryLimitGB,
                AcceptableDataWeightDeviation,
                VersionsPath);
        transport.Run(BannersTablePath, ReadyTablePath);
        UNIT_ASSERT(Client->Exists(VersionsPath + "/1"));
        UNIT_ASSERT(Client->Exists(VersionsPath + "/2"));
        UNIT_ASSERT(Client->Exists(ReadyTablePath));
        auto reader = Client->CreateTableReader<NYT::TNode>(ReadyTablePath);
        TSet<ui64> versions = {1ULL, 2ULL};
        int rowCount = 0;
        for (; reader->IsValid(); reader->Next()) {
            auto row = reader->GetRow();
            UNIT_ASSERT_EQUAL(row["BannerID"].ConvertTo<i64>(), 1LL);
            UNIT_ASSERT(versions.contains(row["Version"].ConvertTo<ui64>()));
            versions.erase(row["Version"].ConvertTo<ui64>());
            ++rowCount;
        }
        UNIT_ASSERT_EQUAL(rowCount, 2);
        UNIT_ASSERT(versions.empty());
    }

    void TestOlderVersionUpdated() {
        const ui64 numRows = 1;
        PrepareTableVersionOne(numRows);
        TTsarTransport::TMappers mappers;
        mappers.emplace(1ULL, new TCopyBannerIDMapper(1ULL));
        TTsarTransport transport(
                Client,
                mappers,
                ScoringDataSizePerJobGB,
                ScoringMapperMemoryLimitGB,
                AcceptableDataWeightDeviation,
                VersionsPath);
        transport.Run(BannersTablePath, ReadyTablePath);
        UNIT_ASSERT(Client->Exists(VersionsPath + "/1"));
        UNIT_ASSERT(Client->Exists(ReadyTablePath));
        auto reader = Client->CreateTableReader<NYT::TNode>(ReadyTablePath);
        int rowCount = 0;
        for (; reader->IsValid(); reader->Next()) {
            auto row = reader->GetRow();
            UNIT_ASSERT_EQUAL(row["BannerID"].ConvertTo<i64>(), 1LL);
            UNIT_ASSERT_EQUAL(row["Version"].ConvertTo<ui64>(), 1ULL);
            ++rowCount;
        }
        UNIT_ASSERT_EQUAL(rowCount, numRows);
    }

    void TestAddingBrokenVersionDoesNotBrakeOlderVersion() {
        TTsarTransport::TMappers mappers;
        mappers.emplace(1ULL, new TCopyBannerIDMapper(1ULL));
        mappers.emplace(2ULL, new TFailMapper());
        TTsarTransport transport(
                Client,
                mappers,
                ScoringDataSizePerJobGB,
                ScoringMapperMemoryLimitGB,
                AcceptableDataWeightDeviation,
                VersionsPath);
        transport.Run(BannersTablePath, ReadyTablePath);
        UNIT_ASSERT(Client->Exists(VersionsPath + "/1"));
        UNIT_ASSERT(!Client->Exists(VersionsPath + "/2"));
        auto reader = Client->CreateTableReader<NYT::TNode>(ReadyTablePath);
        int rowCount = 0;
        for (; reader->IsValid(); reader->Next()) {
            auto row = reader->GetRow();
            UNIT_ASSERT_EQUAL(row["BannerID"].ConvertTo<i64>(), 1LL);
            UNIT_ASSERT_EQUAL(row["Version"].ConvertTo<ui64>(), 1ULL);
            ++rowCount;
        }
        UNIT_ASSERT_EQUAL(rowCount, 1);
    }

    void TestSizeValidationFailUsesOlderVersion() {
        const ui64 numRows = 10;
        PrepareTableVersionOne(numRows);
        TTsarTransport::TMappers mappers;
        mappers.emplace(1ULL, new TCopyBannerIDMapper(1ULL));
        const float acceptableDeviation = 0.5f;
        TTsarTransport transport(
                Client,
                mappers,
                ScoringDataSizePerJobGB,
                ScoringMapperMemoryLimitGB,
                acceptableDeviation,
                VersionsPath);
        transport.Run(BannersTablePath, ReadyTablePath);
        UNIT_ASSERT(Client->Exists(VersionsPath + "/1"));
        UNIT_ASSERT(Client->Exists(ReadyTablePath));
        auto reader = Client->CreateTableReader<NYT::TNode>(ReadyTablePath);
        int rowCount = 0;
        for (; reader->IsValid(); reader->Next()) {
            auto row = reader->GetRow();
            UNIT_ASSERT_EQUAL(row["Version"].ConvertTo<int>(), 0);
            ++rowCount;
        }
        UNIT_ASSERT_EQUAL(rowCount, 10);
    }

    void TestAllVersionFailWithoutBackup() {
        TTsarTransport::TMappers mappers;
        mappers.emplace(1ULL, new TFailMapper);
        mappers.emplace(2ULL, new TFailMapper);
        TTsarTransport transport(
                Client,
                mappers,
                ScoringDataSizePerJobGB,
                ScoringMapperMemoryLimitGB,
                AcceptableDataWeightDeviation,
                VersionsPath);
        transport.Run(BannersTablePath, ReadyTablePath);
        // expect exception
    }

    TTsarTransportTest()
    : Client(NYT::NTesting::CreateTestClient(GetEnv("YT_PROXY")))
    , BannersTablePath("//home/tsar/banners")
    , VersionsPath("//home/tsar/versions")
    , ReadyTablePath("//home/tsar/ready")
    , ScoringDataSizePerJobGB(1)
    , ScoringMapperMemoryLimitGB(1)
    , AcceptableDataWeightDeviation(0)
    {
    }

    void SetUp() override {
        Client->Create(BannersTablePath, NYT::NT_TABLE, NYT::TCreateOptions().Recursive(true));
        auto writer = Client->CreateTableWriter<TensorTransport::TBannerRecord>(BannersTablePath);
        TensorTransport::TBannerRecord row;
        row.SetBannerID(1);
        writer->AddRow(row);
        writer->Finish();
    }

    void TearDown() override {
        TVector<TString> tables = {BannersTablePath, VersionsPath, ReadyTablePath};
        for (const auto& table : tables) {
            if (Client->Exists(table)) {
                Client->Remove(table, NYT::TRemoveOptions().Recursive(true));
            }
        }
    }

private:
    void PrepareTableVersionOne(ui64 numRows) {
        Client->Create(VersionsPath + "/1", NYT::NT_TABLE, NYT::TCreateOptions().Recursive(true));
        auto writer = Client->CreateTableWriter<NYT::TNode>(VersionsPath + "/1");
        for (ui64 bannerID = 1; bannerID <= numRows; ++bannerID) {
            NYT::TNode row;
            row["BannerID"] = bannerID;
            row["Version"] = 0ULL;
            writer->AddRow(row);
        }
        writer->Finish();
    }

    NYT::IClientPtr Client;
    const TString BannersTablePath;
    const TString VersionsPath;
    const TString ReadyTablePath;
    ui64 ScoringDataSizePerJobGB;
    ui64 ScoringMapperMemoryLimitGB;
    float AcceptableDataWeightDeviation;
};

UNIT_TEST_SUITE_REGISTRATION(TTsarTransportTest);
