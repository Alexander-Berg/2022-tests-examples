#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/rtyserver_test/cases/search/mix_factors.h>
#include <saas/library/sharding/sharding.h>
#include <saas/library/sharding/rules/kps.h>
#include <saas/library/daemon_base/actions_engine/controller_script.h>
#include <saas/library/daemon_base/controller_actions/restart.h>
#include <saas/rtyserver/common/common_messages.h>
#include <util/generic/ymath.h>
#include <util/generic/map.h>

using TShardingContext = NSaas::TShardsDispatcher::TContext;

SERVICE_TEST_RTYSERVER_DEFINE(TestShardingBase)
protected:
    ui32 Shift = 0;
    typedef TMap<ui32, TVector<NRTYServer::TMessage>> TMessagesByShard;

    TMessagesByShard GenerateMessages(ui32 shardsCnt, ui32 docsCount, TVector<NRTYServer::TMessage>& messages) {
        GenerateInput(messages, shardsCnt * docsCount, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());

        TMessagesByShard result;
        for (ui32 shard = 0; shard < shardsCnt; ++shard) {
            auto curInterval = NSaas::TSharding::GetInterval(shard, shardsCnt, 0, NSearchMapParser::SearchMapShards);
            NSaas::TKeyPrefix keyPrefix = curInterval.GetMin();
            if (keyPrefix == 0)
                keyPrefix++;
            TShardingContext ctx(NSaas::KeyPrefix, Shift);
            for (ui32 doc = 0; doc < docsCount; ++ doc) {
                auto& mes = messages[shard * docsCount + doc];
                mes.MutableDocument()->SetKeyPrefix(keyPrefix);
                NSearchMapParser::TShardIndex mesShard = NSaas::TShardsDispatcher(ctx).GetShard(mes);
                DEBUG_LOG << "Shard for " << mes.GetDocument().GetUrl() << " - " << mesShard << Endl;
                NSearchMapParser::TShardIndex kpsShard = NSaas::TKpsShardingRule::GetPrefixShard(keyPrefix, ctx);
                if (!(kpsShard <= mesShard && mesShard < kpsShard + (1 << Shift))) {
                    ythrow yexception() << "Incorrect sharding " << mesShard << "/" << kpsShard << Endl;
                }
                result[kpsShard].push_back(mes);
            }
        }

        return result;
    }

    bool Run() override {
        if (!GetIsPrefixed()) {
            ythrow yexception() << "Only prefixed case makes sence";
        }
        return DoRun();
    }

    void InitCluster() override {
        Shift = 5;
        SetMergerParams(true, 1, -1, mcpNONE);
    }

    virtual bool DoRun() = 0;
};


START_TEST_DEFINE_PARENT(TestDetachAndSynch, TestShardingBase)
bool DoRun() override {
    TVector<NRTYServer::TMessage> messages;
    auto mesByKps = GenerateMessages(5, 5, messages);
    IndexMessages(messages, DISK, 1);
    ReopenIndexers();

    CheckSearchResults(messages);

    TString reply;
    if (!Controller->Detach(0, NSearchMapParser::SearchMapShards, TShardingContext(NSaas::KeyPrefix, Shift), reply)) {
        ERROR_LOG << "Detach failed: " << reply << Endl;
        return false;
    }

    Controller->ProcessCommand("restart");

    DeleteSpecial();
    Controller->ProcessCommand("create_merger_tasks");
    Controller->ProcessCommand("do_all_merger_tasks");

    TString syncReply;
    if (!Controller->Synchronize(reply, syncReply)) {
        ERROR_LOG << "Synchronization failed: " << syncReply << Endl;
        return false;
    }

    Controller->RestartServer();

    Controller->ProcessCommand("do_all_merger_tasks");
    CheckSearchResults(messages);

    TVector<ui32> kpsVec;
    for (auto& kps : mesByKps) {
        kpsVec.push_back(kps.first);
    }

    if (!Controller->Detach(kpsVec[1], kpsVec[3] - 1, TShardingContext(NSaas::KeyPrefix, Shift), reply)) {
        ERROR_LOG << "Detach failed: " << reply << Endl;
        return false;
    }

    Controller->ProcessCommand("restart");

    DeleteSpecial();
    Controller->ProcessCommand("create_merger_tasks");
    Controller->ProcessCommand("do_all_merger_tasks");

    if (!Controller->Synchronize(reply, syncReply)) {
        ERROR_LOG << "Synchronization failed: " << syncReply << Endl;
        return false;
    }
    CHECK_TEST_EQ(Controller->GetServerBrief(), "OK");

    Controller->RestartServer();

    Controller->ProcessCommand("do_all_merger_tasks");

    TVector<TDocSearchInfo> results;
    QuerySearch("url:\"*\"&" + GetAllKps(messages) + "&numdoc=10000", results, nullptr, nullptr, true);
    CHECK_TEST_EQ(results.size(), 10);

    CheckSearchResults(mesByKps[1]);
    CheckSearchResults(mesByKps[2]);

    return true;
}
};

START_TEST_DEFINE_PARENT(TestRemoveAndCheck, TestShardingBase)
bool DoRun() override {
    TVector<NRTYServer::TMessage> messages;
    auto mesByKps = GenerateMessages(5, 5, messages);
    IndexMessages(messages, DISK, 1);
    ReopenIndexers();

    if (!SendIndexReply) {
        Controller->WaitEmptyIndexingQueues();
        sleep(10);
    }

    ReopenIndexers();
    CheckSearchResults(messages);

    TVector<ui32> kpsVec;
    for (auto& kps : mesByKps) {
        INFO_LOG << "GENERATED_KPS " << kps.first << Endl;
        kpsVec.push_back(kps.first);
    }

    TString reply;
    if (!Controller->ShardsAction(0, kpsVec[2] - 1, TShardingContext(NSaas::KeyPrefix, Shift), reply, "remove")) {
        ERROR_LOG << "Remove failed: " << reply << Endl;
        return false;
    }

    if (!Controller->ShardsAction(kpsVec[4], NSearchMapParser::SearchMapShards, TShardingContext(NSaas::KeyPrefix, Shift), reply, "remove")) {
        ERROR_LOG << "Remove failed: " << reply << Endl;
        return false;
    }

    CheckSearchResults(mesByKps[2]);
    CheckSearchResults(mesByKps[3]);

    TVector<TDocSearchInfo> results;
    QuerySearch("url:\"*\"&" + GetAllKps(messages) + "&numdoc=10000", results, nullptr, nullptr, true);
    CHECK_TEST_EQ(results.size(), 10);

    TString replyCheck;
    if (!Controller->ShardsAction(kpsVec[2], kpsVec[4] - 1, TShardingContext(NSaas::KeyPrefix, Shift), replyCheck, "check")) {
        ERROR_LOG << "Check failed: " << reply << Endl;
        return false;
    }

    if (replyCheck.find("\"shards_result\":\"OK\"") == TString::npos) {
        ERROR_LOG << "Check result failed: " << replyCheck << Endl;
        return false;
    }

    return true;
}
};

START_TEST_DEFINE(TestKpsSharding)
bool Run() override {
    if (!GetIsPrefixed()) {
        return true;
    }

    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 2, NRTYServer::TMessage::ADD_DOCUMENT, true);
    messages[0].MutableDocument()->SetKeyPrefix(1);
    messages[1].MutableDocument()->SetKeyPrefix(60000);
    IndexMessages(messages, REALTIME, 1);
    ReopenIndexers();

    TVector<TDocSearchInfo> results;
    QuerySearch("body&kps=1", results, nullptr, nullptr, true);
    CHECK_TEST_EQ(results.size(), 1);

    auto firstFirst = Controller->GetMetric("MetaSearch_ResponseTime_Total", 0);
    auto secondFirst = Controller->GetMetric("MetaSearch_ResponseTime_Total", 1);
    CHECK_TEST_EQ(firstFirst, 1);
    CHECK_TEST_EQ(secondFirst, 0);

    QuerySearch("body&kps=60000", results, nullptr, nullptr, true);
    CHECK_TEST_EQ(results.size(), 1);

    auto firstSecond = Controller->GetMetric("MetaSearch_ResponseTime_Total", 0);
    auto secondSecond = Controller->GetMetric("MetaSearch_ResponseTime_Total", 1);
    CHECK_TEST_EQ(firstSecond, 1);
    CHECK_TEST_EQ(secondSecond, 1);

    return true;
}
};

START_TEST_DEFINE(TestKeySharding)
bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 10, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    for (size_t i = 0; i < 10; ++i) {
        messages[i].MutableDocument()->SetUrl("yandex" + ToString(i));
    }
    IndexMessages(messages, REALTIME, 1);
    ReopenIndexers();

    for (auto&& m : messages) {
        TVector<TDocSearchInfo> results;
        QuerySearch(m.GetDocument().GetUrl() + "&sgkps=" + ToString(m.GetDocument().GetKeyPrefix()), results, nullptr, nullptr, true);
        CHECK_TEST_EQ(results.size(), 1);
    }

    TVector<TDocSearchInfo> results;
    QuerySearch(messages[0].GetDocument().GetUrl() + "," + messages[1].GetDocument().GetUrl() +
        "&sgkps=" + ToString(messages[0].GetDocument().GetKeyPrefix()) + "," + ToString(messages[1].GetDocument().GetKeyPrefix()), results, nullptr, nullptr, true);
    CHECK_TEST_EQ(results.size(), 2);

    auto firstCount = Controller->GetMetric("MetaSearch_ResponseTime_Total", 0);
    auto secondCount = Controller->GetMetric("MetaSearch_ResponseTime_Total", 1);
    CHECK_TEST_EQ(firstCount + secondCount, messages.size() + 2);

    return true;
}
bool InitConfig() override {
    (*ConfigDiff)["IndexGenerator"] = FULL_ARCHIVE_COMPONENT_NAME;
    (*ConfigDiff)["Components"] = FULL_ARCHIVE_COMPONENT_NAME;
    (*ConfigDiff)["Indexer.Memory.Enabled"] = "false";
    (*ConfigDiff)["Searcher.SnippetsDeniedZones"] = "";
    (*ConfigDiff)["Searcher.TwoStepQuery"] = "false";
    (*SPConfigDiff)["Service.MetaSearch.TwoStepQuery"] = "false";
    (*SPConfigDiff)["Service.MetaSearch.ReArrangeOptions"] = "UrlSharding";

    return true;
}
};

START_TEST_DEFINE(TestUrlSharding)
bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 10, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    for (size_t i = 0; i < 10; ++i) {
        messages[i].MutableDocument()->SetUrl("yandex" + ToString(i));
    }
    IndexMessages(messages, REALTIME, 1);
    ReopenIndexers();

    for (auto&& m : messages) {
        TVector<TDocSearchInfo> results;
        QuerySearch("url:\"" + m.GetDocument().GetUrl() + "\"" + "&kps=" + ToString(m.GetDocument().GetKeyPrefix()), results, nullptr, nullptr, true);
        CHECK_TEST_EQ(results.size(), 1);
    }

    auto firstCount = Controller->GetMetric("MetaSearch_ResponseTime_Total", 0);
    auto secondCount = Controller->GetMetric("MetaSearch_ResponseTime_Total", 1);
    CHECK_TEST_EQ(firstCount + secondCount, messages.size());

    return true;
}
bool InitConfig() override {
    (*SPConfigDiff)["Service.MetaSearch.ReArrangeOptions"] = "UrlSharding(Mode=Search)";

    return true;
}
};
