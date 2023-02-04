#include <saas/rtyserver_test/testerlib/rtyserver_test.h>

#include <library/cpp/digest/md5/md5.h>
#include <util/system/env.h>

START_TEST_DEFINE(TestFullArchiveGenerateReplyCodes, TTestMarksPool::OneBackendOnly)
bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 10, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    for (ui32 i = 0; i < messages.size(); ++i) {
        messages[i].MutableDocument()->SetKeyPrefix(GetIsPrefixed() ? 2 : 0);
        messages[i].MutableDocument()->SetBody(ToString(i));
    }
    IndexMessages(messages, DISK, 1);
    ReopenIndexers();

    TVector<TDocSearchInfo> results;
    TQuerySearchContext ctx;
    ctx.SourceSelector = new TDirectSourceSelector();
    if (Cluster->GetNodesNames(TNODE_SEARCHPROXY).size()) {
        CHECK_TEST_EQ(404, QuerySearch("aaa&sp_meta_search=proxy&meta_search=first_found&sgkps=" + ToString(messages[0].GetDocument().GetKeyPrefix()), results, ctx));
    } else {
        CHECK_TEST_EQ(200, QuerySearch("aaa&sp_meta_search=proxy&meta_search=first_found&sgkps=" + ToString(messages[0].GetDocument().GetKeyPrefix()), results, ctx));
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

START_TEST_DEFINE(TestSprErrorDiffAnswers)
bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 10, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    for (ui32 i = 0; i < messages.size(); ++i) {
        messages[i].MutableDocument()->SetKeyPrefix(GetIsPrefixed() ? 2 : 0);
        messages[i].MutableDocument()->SetBody(ToString(i));
    }
    IndexMessages(messages, DISK, 1);
    ReopenIndexers();

    for (ui32 i = 0; i < messages.size(); ++i) {
        TVector<TDocSearchInfo> results;
        TQuerySearchContext ctx;
        ctx.PrintResult = true;
        ctx.SourceSelector = new TDirectSourceSelector();
        {
            if (Cluster->GetNodesNames(TNODE_SEARCHPROXY).size()) {
                CHECK_TEST_EQ(502, QuerySearch(messages[i].GetDocument().GetUrl() + "&sp_meta_search=proxy&meta_search=first_found&sgkps=" + ToString(messages[i].GetDocument().GetKeyPrefix()), results, ctx));
            } else {
                CHECK_TEST_EQ(500, QuerySearch(messages[i].GetDocument().GetUrl() + "&sp_meta_search=proxy&meta_search=first_found&sgkps=" + ToString(messages[i].GetDocument().GetKeyPrefix()), results, ctx));
            }
        }

    }

    {
        TConfigFieldsPtr diff(new TConfigFields);
        (*diff)["Searcher.ExceptionOnSearch"] = false;
        Controller->ApplyConfigDiff(diff, TBackendProxy::TBackendSet(0));

        for (ui32 i = 0; i < messages.size(); ++i) {
            TVector<TDocSearchInfo> results;
            TQuerySearchContext ctx;
            ctx.PrintResult = true;
            ctx.SourceSelector = new TDirectSourceSelector();
            {
                CHECK_TEST_EQ(200, QuerySearch(messages[i].GetDocument().GetUrl() + "&sp_meta_search=proxy&meta_search=first_found&sgkps=" + ToString(messages[i].GetDocument().GetKeyPrefix()), results, ctx));
                CHECK_TEST_EQ(results.size(), 1);
            }

        }
    }

    {
        TConfigFieldsPtr diff(new TConfigFields);
        (*diff)["Searcher.ExceptionOnSearch"] = true;
        Controller->ApplyConfigDiff(diff, TBackendProxy::TBackendSet(0));
        TConfigFieldsPtr diff1(new TConfigFields);
        (*diff1)["Searcher.ExceptionOnSearch"] = false;
        Controller->ApplyConfigDiff(diff1, TBackendProxy::TBackendSet(1));

        for (ui32 i = 0; i < messages.size(); ++i) {
            TVector<TDocSearchInfo> results;
            TQuerySearchContext ctx;
            ctx.PrintResult = true;
            ctx.SourceSelector = new TDirectSourceSelector();
            {
                CHECK_TEST_EQ(200, QuerySearch(messages[i].GetDocument().GetUrl() + "&sp_meta_search=proxy&meta_search=first_found&sgkps=" + ToString(messages[i].GetDocument().GetKeyPrefix()), results, ctx));
                CHECK_TEST_EQ(results.size(), 1);
            }

        }
    }
    return true;
}

bool InitConfig() override {
    (*ConfigDiff)["IndexGenerator"] = FULL_ARCHIVE_COMPONENT_NAME;
    (*ConfigDiff)["Components"] = FULL_ARCHIVE_COMPONENT_NAME;
    (*ConfigDiff)["Indexer.Memory.Enabled"] = "false";
    (*ConfigDiff)["Searcher.SnippetsDeniedZones"] = "";
    (*ConfigDiff)["Searcher.TwoStepQuery"] = "false";
    (*ConfigDiff)["Searcher.ExceptionOnSearch"] = true;
    (*SPConfigDiff)["Service.MetaSearch.TwoStepQuery"] = "false";
    return true;
}
};
