#include <saas/rtyserver_test/testerlib/rtyserver_test.h>

START_TEST_DEFINE(TestEmptySearchSuggest)
bool Run() override {
    TVector<TDocSearchInfo> results;
    QuerySearch("\xE0\xE2", results); // two latin-1 encoded chars: "small letter A with circumflex", "small letter A with grave"
    CHECK_TEST_EQ(results.size(), 0);
    return true;
}
bool InitConfig() override {
    (*ConfigDiff)["IndexGenerator"] = "Suggest";
    (*ConfigDiff)["Indexer.Common.UseSlowUpdate"] = "0";
    (*ConfigDiff)["Indexer.Memory.Enabled"] = "false";
    (*ConfigDiff)["Searcher.SnippetsDeniedZones"] = "";
    (*ConfigDiff)["SearchersCountLimit"] = 1;
    (*ConfigDiff)["ComponentsConfig.Suggest.WordsCountToSave"] = "200";
    (*ConfigDiff)["ComponentsConfig.Suggest.WordsCountToReject"] = "200";
    (*ConfigDiff)["Searcher.FactorsInfo"] = GetResourcesDirectory() + "/factors/suggest_factors.cfg";
    SetEnabledDiskSearch();
    SetMergerParams(true, 1, -1, mcpCONTINUOUS);
    SetEnabledRepair();
    return true;
}
};

START_TEST_DEFINE(TestEmptySearchIndex)
bool Run() override {
    TVector<TDocSearchInfo> results;
    QuerySearch("body", results);
    CHECK_TEST_EQ(results.size(), 0);
    return true;
}
bool InitConfig() override {
    SetIndexerParams(DISK, 15000, -1, 0);
    SetEnabledDiskSearch();
    SetMergerParams(true, 1, -1, mcpCONTINUOUS);
    (*ConfigDiff)["Indexer.Memory.Enabled"] = "false";
    return true;
}
};

START_TEST_DEFINE(TestFullArchiveEmptyKey, TTestMarksPool::OneBackendOnly)
bool Run() override {

    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 10, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    TVector<TString> keys;
    keys.reserve(messages.size());
    for (ui32 i = 0; i < messages.size(); ++i) {
        keys.push_back(messages[i].GetDocument().GetUrl());
    }
    IndexMessages(messages, DISK, 1);
    ReopenIndexers();
    const TString keysStr = keys[0] + ",," + JoinStrings(keys.cbegin() + 1, keys.cend(), ",");
    const TString sgkps = GetAllKps(messages, "&sgkps=");
    TQuerySearchContext ctx;
    ctx.PrintResult = true;
    {
        TVector<TDocSearchInfo> results;
        QuerySearch(keysStr + sgkps, results, ctx);
        INFO_LOG << results.size() << Endl;
        CHECK_TEST_EQ(results.size(), messages.size());
    }
    {
        TVector<TDocSearchInfo> results;
        if (Cluster->GetNodesNames(TNODE_SEARCHPROXY).size()) {
            ctx.SourceSelector = new TLevelSourceSelector(2);
        } else {
            ctx.SourceSelector = new TDirectSourceSelector();
        }
        NOT_EXCEPT(QuerySearch(keysStr + "&sp_meta_search=meta&meta_search=first_found" + sgkps, results, ctx));
        INFO_LOG << results.size() << Endl;
        CHECK_TEST_EQ(results.size(), messages.size());
    }
    return true;
}

bool InitConfig() override {
    (*ConfigDiff)["IndexGenerator"] = FULL_ARCHIVE_COMPONENT_NAME;
    (*ConfigDiff)["Components"] = FULL_ARCHIVE_COMPONENT_NAME;
    (*ConfigDiff)["Indexer.Memory.Enabled"] = "false";
    (*ConfigDiff)["Searcher.SnippetsDeniedZones"] = "";
    (*ConfigDiff)["Searcher.TwoStepQuery"] = "false";
    (*SPConfigDiff)["Service.MetaSearch.TwoStepQuery"] = "false";
    return true;
}
};
