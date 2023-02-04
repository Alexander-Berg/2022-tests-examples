#include <ads/tensor_transport/yt_lib/yt_validator_stats.h>
#include <library/cpp/testing/unittest/registar.h>
#include <mapreduce/yt/tests/yt_unittest_lib/yt_unittest_lib.h>
#include <mapreduce/yt/interface/client.h>
#include <util/random/random.h>
#include <util/system/env.h>

using namespace NTsarTransport;

class TValidatorYtStatsTest : public TTestBase {
public:

    void SetUp() override {
        TString ytProxy = GetEnv("YT_PROXY");
        Client = NYT::CreateClient(ytProxy);
        Client->Create(Source, NYT::NT_TABLE);
        auto writer = Client->CreateTableWriter<NYT::TNode>(Source);
        writer->AddRow(
                NYT::TNode()("NumberOfHashes", PrevNumberOfHashes)("OutputTableSize", OutputTableSize)
                );
        Stats = new TYtValidatorStats(Client, Source);
    }

    void PrevNumberOfHashesTest() {
        ui64 actual = Stats->GetNumberOfHashes();
        TString message = "Expected: " + ToString(PrevNumberOfHashes) + " Actual: " + ToString(actual);
        UNIT_ASSERT_EQUAL_C(actual, PrevNumberOfHashes, message);
    }

    void OutputTableSizeTest() {
        ui64 actual = Stats->GetOutputTableSize();
        TString message = "Expected: " + ToString(OutputTableSize) + " Actual: " + ToString(actual);
        UNIT_ASSERT_EQUAL_C(actual, OutputTableSize, message);
    }

    void TearDown() override {
        Client->Remove(Source, NYT::TRemoveOptions().Recursive(true));
    }

private:
    UNIT_TEST_SUITE(TValidatorYtStatsTest);
    UNIT_TEST(PrevNumberOfHashesTest);
    UNIT_TEST(OutputTableSizeTest);
    UNIT_TEST_SUITE_END();
    NYT::IClientPtr Client;
    IValidatorStats *Stats;
    TString Source = "//tmp/test_table";
    ui64 PrevNumberOfHashes = RandomNumber<ui64>();
    ui64 OutputTableSize = RandomNumber<ui64>();
};


class TEmptyTableValidatorYtStatsTest : public TTestBase {
public:

    void SetUp() override {
        TString ytProxy = GetEnv("YT_PROXY");
        Client = NYT::CreateClient(ytProxy);
        Stats = new TYtValidatorStats(Client, Source);
    }

    void PrevNumberOfHashesTest() {
        ui64 actual = Stats->GetNumberOfHashes();
        TString message = "Expected: " + ToString(0) + " Actual: " + ToString(actual);
        UNIT_ASSERT_EQUAL_C(actual, 0, message);
    }

    void OutputTableSizeTest() {
        ui64 actual = Stats->GetOutputTableSize();
        TString message = "Expected: " + ToString(0) + " Actual: " + ToString(actual);
        UNIT_ASSERT_EQUAL_C(actual, 0, message);
    }

private:
    UNIT_TEST_SUITE(TEmptyTableValidatorYtStatsTest);
    UNIT_TEST(PrevNumberOfHashesTest);
    UNIT_TEST(OutputTableSizeTest);
    UNIT_TEST_SUITE_END();
    NYT::IClientPtr Client;
    IValidatorStats *Stats;
    TString Source = "//tmp/no_table";
};

UNIT_TEST_SUITE_REGISTRATION(TEmptyTableValidatorYtStatsTest);


class TValidatorStatsWriterTest : public TTestBase {
public:

    void SetUp() override {
        TString ytProxy = GetEnv("YT_PROXY");
        Client = NYT::CreateClient(ytProxy);
        Client->Create(Source, NYT::NT_TABLE);
        Writer = new TYtValidatorStatsWriter(Client, Source);
    }

    void PrevNumberOfHashesTest() {
        auto reader = Client->CreateTableReader<NYT::TNode>(Source);
        ui64 numberOfHashes = RandomNumber<ui64>();
        ui64 outputTableSize = RandomNumber<ui64>();
        TValidatorStats validatorStats(numberOfHashes, outputTableSize);
        Writer->Write(&validatorStats);
        TYtValidatorStats ytValidatorStats(Client, Source);
        TString message = "Expected value: " +
                ToString(outputTableSize) +
                " Actual value: " +
                ToString(ytValidatorStats.GetOutputTableSize());
        UNIT_ASSERT_EQUAL_C(ytValidatorStats.GetOutputTableSize(), outputTableSize, message);
    }

    void TearDown() override  {
        Client->Remove(Source, NYT::TRemoveOptions().Recursive(true));
    }

private:
    UNIT_TEST_SUITE(TValidatorStatsWriterTest);
    UNIT_TEST(PrevNumberOfHashesTest);
    UNIT_TEST_SUITE_END();
    NYT::IClientPtr Client;
    TYtValidatorStatsWriter *Writer;
    TString Source = "//tmp/test_table";
    };

UNIT_TEST_SUITE_REGISTRATION(TValidatorStatsWriterTest);
