#include <ads/tensor_transport/yt_lib/tsar_transport.h>
#include <ads/tensor_transport/proto/page.pb.h>

#include <library/cpp/testing/unittest/registar.h>
#include <mapreduce/yt/interface/client.h>
#include <mapreduce/yt/tests/yt_unittest_lib/yt_unittest_lib.h>
#include <util/system/env.h>

using namespace NTsarTransport;

class TSuccessPageMapper:  public NYT::IMapper<NYT::TTableReader<TensorTransport::TPageRecord>, NYT::TTableWriter<NYT::TNode>> {

    void Do(TReader* reader, TWriter* writer) override {
        for (; reader->IsValid(); reader->Next()) {
            const auto row = reader->MoveRow();
            NYT::TNode node;
            node["PageID"] = row.GetPageID();
            node["ImpID"] = row.GetImpID();
            writer->AddRow(node);
        }
    }
};

class TTsarPageTransportTest : public TTestBase {
    UNIT_TEST_SUITE(TTsarPageTransportTest);
    UNIT_TEST(TestSinglePageMapper);
    UNIT_TEST_SUITE_END();

    TTsarPageTransportTest() :
        Client(NYT::NTesting::CreateTestClient(GetEnv("YT_PROXY"))),
        BannersTablePath("//home/bs/test_banner_table"),
        TablesPrefix("//home/bs/tmp/test_page_prefix"),
        OutputTablePath("//home/bs/output_table"),
        PageID(1),
        ImpID(2)
    {

    }

    void TestSinglePageMapper() {
        TTsarTransport::TMappers mappers;
        mappers.emplace(1, new TSuccessPageMapper());
        TTsarTransport transport(
                Client,
                mappers,
                1,
                1,
                0.1,
                TablesPrefix
        );
        transport.Run(BannersTablePath, OutputTablePath);

        auto tableReader = Client->CreateTableReader<NYT::TNode>(OutputTablePath);
        for (; tableReader->IsValid(); tableReader->Next()) {
            const auto row = tableReader->MoveRow();
            UNIT_ASSERT_EQUAL(row.ChildAsUint64("PageID"), PageID);
        }

    }


    void SetUp() override {
        Client->Create(BannersTablePath, NYT::NT_TABLE, NYT::TCreateOptions().Recursive(true));
        auto writer = Client->CreateTableWriter<TensorTransport::TPageRecord>(BannersTablePath);
        TensorTransport::TPageRecord row;
        row.SetPageID(PageID);
        row.SetImpID(ImpID);
        writer->AddRow(row);
        writer->Finish();
    }

    void TearDown() override {
        TVector<TString> tables = {BannersTablePath, TablesPrefix, OutputTablePath};
        for (const auto& table : tables) {
            if (Client->Exists(table)) {
                Client->Remove(table, NYT::TRemoveOptions().Recursive(true));
            }
        }
    }

    NYT::IClientPtr Client;
    const TString BannersTablePath;
    const TString TablesPrefix;
    const TString OutputTablePath;
    const ui64 PageID;
    const ui64 ImpID;
};



