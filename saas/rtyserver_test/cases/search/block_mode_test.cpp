#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/api/indexing_client/client.h>
#include <saas/library/hash_to_block_mode/hash_to_block_mode.h>

START_TEST_DEFINE(TestBlockMode)
    bool Run() override {
        NSaas::TAction actionDoc1;
        NSaas::TDocument& doc1 = actionDoc1.AddDocument();
        doc1.SetUrl("some_url_1");
        doc1.AddProperty("gta_1", "1");

        NSaas::TAction actionDoc2;
        NSaas::TDocument& doc2 = actionDoc2.AddDocument();
        doc2.SetUrl("some_url_2");
        doc2.AddProperty("gta_2", "2");

        NSaas::TAction action;
        action.SetActionType(NSaas::TAction::atModify);
        NSaas::TDocument& blockDoc = action.AddDocument();

        TBlockHash blockHash(1);
        TString blockKey1 = ToString(blockHash.GetBlockKey("0", "some_url_1"));
        TString blockKey2 = ToString(blockHash.GetBlockKey("0", "some_url_2"));
        CHECK_TEST_EQ(blockKey1, blockKey2);
        TString propKey1 = blockHash.GetGtaKey("0", "some_url_1");
        TString propKey2 = blockHash.GetGtaKey("0", "some_url_2");

        blockDoc.SetUrl(blockKey1);
        blockDoc.AddProperty(propKey1, actionDoc1.ToProtobuf().GetDocument().SerializeAsString());
        blockDoc.AddProperty(propKey2, actionDoc2.ToProtobuf().GetDocument().SerializeAsString());

        auto& backendConf = Controller->GetConfig();
        NSaas::TIndexingClient client(backendConf.Indexer.Host, backendConf.Indexer.Port, "/service/" + MD5::Calc("tests json_ref"));
        auto res = client.Send(action, "json_ref");
        CHECK_TEST_TRUE(res.GetCode() == NSaas::TSendResult::srOK);

        ReopenIndexers();

        TQuerySearchContext ctx; 
        ctx.AttemptionsCount = 5;
        ctx.PrintResult = true;
        TQuerySearchContext::TDocProperties resultProps;
        ctx.DocProperties = &resultProps;
        ctx.SourceSelector = new TDirectSourceSelector();

        TVector<TDocSearchInfo> results;
        QuerySearch("some_url_1&sp_meta_search=proxy&meta_search=first_found&normal_kv_report=1", results, ctx);

        if (results.size() != 1) {
            PrintInfoServer();
            TEST_FAILED("Test failed: " + ToString(results.size()));
        }

        CHECK_TEST_EQ(resultProps[0]->find("gta_1")->second, "1");

        return true;
    }

    bool InitConfig() override {
        SetIndexerParams(DISK, 100, 1);
        (*ConfigDiff)["IndexGenerator"] = FULL_ARCHIVE_COMPONENT_NAME;
        (*ConfigDiff)["Components"] = FULL_ARCHIVE_COMPONENT_NAME;
        (*ConfigDiff)["Indexer.Memory.Enabled"] = "false";
        (*ConfigDiff)["Searcher.SnippetsDeniedZones"] = "";
        (*ConfigDiff)["Searcher.TwoStepQuery"] = "false";
        (*SPConfigDiff)["Service.MetaSearch.TwoStepQuery"] = "false";

        (*SPConfigDiff)["Service.CustomRearranges.BlockMode"] = "TotalDocCount=2;MaxBlockDocCount=2";
        return true;
    }
};
