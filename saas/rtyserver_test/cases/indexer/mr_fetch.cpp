#include <saas/api/yt_pull_client/saas_yt_writer.h>

#include <saas/rtyserver_test/common/mr_opts.h>
#include <saas/rtyserver_test/testerlib/rtyserver_test.h>


#include <mapreduce/yt/interface/client.h>


START_TEST_DEFINE(TestMRFetchSimple)
    const NSaas::NYTPull::TRtyServerTestMROpts MROpts = NSaas::NYTPull::CreateMROpts("mr_simple");


//FIXME there should be just one place with the default service name
    const TString ServiceName = "tests";


    const NSearchMapParser::TSearchMapService* GetSearchMapService() const {
        const NSaas::TCluster& cluster = Cluster->GetCluster();
        if (cluster.IsMetaServiceExist(ServiceName)) {
            return cluster.ServiceSearchMap(ServiceName + "-search");
        } else {
            return cluster.ServiceSearchMap(ServiceName);
        }
    }

    const NSaas::TShardsDispatcher::TPtr GetShardsDispatcherPtr() const {
        const NSearchMapParser::TSearchMapService* searchMapServicePtr = GetSearchMapService();
        if (!searchMapServicePtr) {
            return NSaas::TShardsDispatcher::TPtr();
        } else {
            return searchMapServicePtr->ShardsDispatcher;
        }
    }

    void AppendChunks(const TString& folder, const TString& masterTablePath, ui32 timestamp, const TVector<NRTYServer::TMessage>& messages, NYT::IClientBasePtr client) {
        const TString slaveTablePath = folder + "/" + ToString(timestamp);
        client->Create(slaveTablePath, NYT::NT_TABLE);
        NSaas::TShardsDispatcher::TPtr shardDispatcherPtr = GetShardsDispatcherPtr();
        if (!shardDispatcherPtr) {
            ythrow yexception() << "Shard dispatcher is null";
        }

        NSaas::NYTPull::TSaasSlaveTableHoldingWriter writer(client, slaveTablePath, shardDispatcherPtr, false);
        for (const NRTYServer::TMessage& message: messages) {
            writer.WriteMessage(message);
        }
        writer.Finish();

        NSaas::NYTPull::RegisterSlaveTable(masterTablePath, slaveTablePath, timestamp, client);
    }

    void CleanupMR(NYT::IClientBasePtr client) const {
        client->Remove(MROpts.TestFolder, NYT::TRemoveOptions().Recursive(true));
    }

    bool Run() override {
        const bool prefixed = GetIsPrefixed();
        TVector<NRTYServer::TMessage> messages;

        TIndexerClient::TContext ctx;
        const ui32 docCount = 100;
        GenerateInput(messages, docCount, NRTYServer::TMessage::MODIFY_DOCUMENT, prefixed);
        if (prefixed) {
            for (NRTYServer::TMessage& message: messages)
                message.MutableDocument()->SetKeyPrefix(1);
        }

        const TString kps = prefixed ? "&kps=1" : "";

        NYT::IClientPtr clientPtr = NYT::CreateClient(MROpts.MrServer,  NYT::TCreateClientOptions().Token(MROpts.MrToken));
        NYT::ITransactionPtr txClientPtr = clientPtr->StartTransaction();
        try {
            NSaas::NYTPull::CreateMasterTable(MROpts.MasterTablePath, txClientPtr);
            AppendChunks(MROpts.TestFolder, MROpts.MasterTablePath, Now().Seconds(), messages, txClientPtr);
            txClientPtr->Commit();
        } catch(...) {
            txClientPtr->Abort();
            ERROR_LOG << "YT Operations failed " << CurrentExceptionMessage() << Endl;
            return false;
        }
        txClientPtr.Drop();

        bool docsIndexed = false;
        for (ui32 i = 0; i < 50 && !docsIndexed; ++i) {
            Sleep(TDuration().Seconds(10));
            docsIndexed = (Controller->GetMetric("Indexer_DocumentsAdded", TBackendProxy::TBackendSet(0)) == docCount);
        }

        CleanupMR(clientPtr);
        clientPtr.Drop();

        CHECK_TEST_TRUE(docsIndexed);

        TQuerySearchContext context;
        context.ResultCountRequirement = docCount;
        context.AttemptionsCount = 10;
        context.PrintResult = true;

        TVector<TDocSearchInfo> results;
        QuerySearch("url:\"*\"&numdoc=500&nocache=da&rearr=AntiDup_off" + kps, results, context);
        CHECK_TEST_EQ(results.size(), docCount);

        ReopenIndexers();

        QuerySearch("url:\"*\"&numdoc=500&nocache=da&rearr=AntiDup_off" + kps, results, context);
        CHECK_TEST_EQ(results.size(), docCount);

        return true;
    }

    bool InitConfig() override {
        (*ConfigDiff)["ModulesConfig.DOCFETCHER.Enabled"] = "true";
        (*ConfigDiff)["ModulesConfig.DOCFETCHER.Stream.StreamType"] = "MapReduce";
        (*ConfigDiff)["ModulesConfig.DOCFETCHER.Stream.Server"] = MROpts.MrServer;
        (*ConfigDiff)["ModulesConfig.DOCFETCHER.Stream.Token"] = MROpts.MrToken;
        (*ConfigDiff)["ModulesConfig.DOCFETCHER.Stream.Table"] = MROpts.MasterTablePath;
        (*ConfigDiff)["ModulesConfig.DOCFETCHER.Stream.ShardMax"] = "65533";
        (*ConfigDiff)["ModulesConfig.DOCFETCHER.Stream.MasterTableCheckInterval"] = "0";
        return true;
    }
};

namespace {
    using NRTYServer::TMessage;

    const TString TestServiceName = "tests";

    struct TFakeShardDispatcher : NSaas::IShardDispatcher {
        NSearchMapParser::TShardIndex GetShard(const NRTYServer::TMessage& message) const override {
            auto& document = message.GetDocument();
            return GetShard(document.GetUrl(), document.GetKeyPrefix());
        }

        NSearchMapParser::TShardIndex GetShard(const TStringBuf& url, NSaas::TKeyPrefix /*kps*/) const override {
            return static_cast<NSearchMapParser::TShardIndex>(url.size()) % NSearchMapParser::SearchMapShards;
        }
    };

    TVector<TMessage> CreateMessages(int kps, const TVector<std::pair<TString, TString>>& input) {
        size_t count = input.size();
        TVector<TMessage> messages;
        messages.reserve(count);
        for (ui32 i = 0; i < count; ++i) {
            messages.push_back(CreateSimpleKVMessage(input[i].first, input[i].second, TString{"attr"}, kps));
        }
        return messages;
    }
}

SERVICE_TEST_RTYSERVER_DEFINE(YtPullTestBase)
    const ui64 TestWaitingTimeout = 180;

    virtual const TVector<TVector<std::pair<TString, TString>>>& GetTestMessages() const = 0;

    virtual ui64 GetExpectedDocCount() const {
        ui64 docCount = 0;
        for (auto& pack : GetTestMessages()) {
            docCount += pack.size();
        }
        return docCount;
    }

    virtual void TweakConfig() {
    }

    virtual bool DoFinalChecks() {
        return true;
    }

    bool Run() override {
        const bool prefixed = GetIsPrefixed();
        auto& messagePacks = GetTestMessages();
        NSaas::NYTPull::TScopedYtStorage ytStorage{YtConfig, MakeAtomicShared<TFakeShardDispatcher>()};
        {
            auto builder = ytStorage.MakeBuilder();
            for (auto& pack : messagePacks) {
                TVector<NRTYServer::TMessage> messages = CreateMessages(prefixed ? 1 : -1, pack);
                builder.CreateChunk(messages, Now().Seconds());
            }
            builder.Commit();
        }

        auto expectedDocCount = GetExpectedDocCount();
        return WaitAndSearch(expectedDocCount, expectedDocCount) && DoFinalChecks();
    }

    bool WaitAndSearch(ui64 expectedDocCount, ui64 added, bool enableSearchAfterWait = false) {
        auto docCount = WaitForDocuments(TestWaitingTimeout, added);
        CHECK_TEST_EQ(docCount, added);

        if (enableSearchAfterWait) {
            Controller->ProcessCommand("enable_search", Controller->GetActiveBackends());
        }

        const TString kps = GetIsPrefixed() ? "&kps=1" : "";

        TQuerySearchContext context;
        context.ResultCountRequirement = expectedDocCount;
        context.AttemptionsCount = 10;
        context.PrintResult = true;

        TVector<TDocSearchInfo> results;
        QuerySearch("url:\"*\"&numdoc=500&nocache=da&rearr=AntiDup_off" + kps, results, context);
        CHECK_TEST_EQ(results.size(), expectedDocCount);

        ReopenIndexers();

        QuerySearch("url:\"*\"&numdoc=500&nocache=da&rearr=AntiDup_off" + kps, results, context);
        CHECK_TEST_EQ(results.size(), expectedDocCount);
        return true;
    }

    ui64 WaitForDocuments(ui32 waitSeconds, ui64 expectedCount) const {
        auto waitDuration = TDuration::Seconds(waitSeconds);
        auto startTime = TInstant::Now();
        ui64 docCount = 0;
        do {
            docCount = Controller->GetMetric("Indexer_DocumentsAdded", TBackendProxy::TBackendSet(0));
            if (docCount >= expectedCount) {
                return docCount;
            }
            Sleep(TDuration::Seconds(1));
        } while (TInstant::Now() - startTime < waitDuration);
        return docCount;
    }

    i32 GetSourceCount() const {
        TJsonPtr reply = Controller->ProcessCommand("get_info_server", Controller->GetActiveBackends());
        NJson::TJsonValue value;
        if (!(*reply)[0].GetValueByPath("result.search_sources_count", value)) {
            return -1;
        }
        return static_cast<i32>(value.GetInteger());
    }

    bool InitConfig() final {
        (*ConfigDiff)["ModulesConfig.DOCFETCHER.Enabled"] = "true";
        (*ConfigDiff)["ModulesConfig.DOCFETCHER.Stream.StreamType"] = "MapReduce";
        (*ConfigDiff)["ModulesConfig.DOCFETCHER.Stream.Server"] = YtConfig.MrServer;
        (*ConfigDiff)["ModulesConfig.DOCFETCHER.Stream.Token"] = YtConfig.MrToken;
        (*ConfigDiff)["ModulesConfig.DOCFETCHER.Stream.Table"] = YtConfig.MasterTablePath;
        (*ConfigDiff)["ModulesConfig.DOCFETCHER.Stream.ShardMax"] = "65533";
        (*ConfigDiff)["ModulesConfig.DOCFETCHER.Stream.MasterTableCheckInterval"] = "0";
        TweakConfig();
        return true;
    }

    const NSaas::NYTPull::TRtyServerTestMROpts YtConfig = NSaas::NYTPull::CreateMROpts("yt_pull");
};

START_TEST_DEFINE_PARENT(TestYtPullSimple, YtPullTestBase)
    const TVector<TVector<std::pair<TString, TString>>>& GetTestMessages() const override {
        return TestMessages;
    }
    TVector<TVector<std::pair<TString, TString>>> TestMessages = {
        {
            {"1", "one"},
            {"12", "two"},
            {"123", "three"},
            {"1234", "four"}
        }
    };
};

START_TEST_DEFINE_PARENT(TestYtPullSimpleRestart, YtPullTestBase)
    const TVector<TVector<std::pair<TString, TString>>>& GetTestMessages() const override {
        return TestMessages;
    }

    TVector<TVector<std::pair<TString, TString>>> TestMessages = {
        {
            {"1", "one"},
            {"12", "two"},
            {"123", "three"},
            {"1234", "four"}
        }
    };

    bool DoFinalChecks() override {
        Controller->RestartBackend(*(Controller->GetActiveBackends().begin()));
        Sleep(TDuration::Seconds(10));

        ReopenIndexers();
        CHECK_TEST_EQ(GetSourceCount(), 2);
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestYtPullTwoChunks, YtPullTestBase)
    const TVector<TVector<std::pair<TString, TString>>>& GetTestMessages() const override {
        return TestMessages;
    }
    TVector<TVector<std::pair<TString, TString>>> TestMessages = {
        {
            {"1", "one"},
            {"12", "two"},
            {"123", "three"},
            {"1234", "four"}
        },
        {
            {"01", "one"},
            {"012", "two"},
            {"0123", "three"},
            {"01234", "four"}
        }
    };
};

START_TEST_DEFINE_PARENT(TestYtPullTwoChunksPause, YtPullTestBase)
    const TVector<TVector<std::pair<TString, TString>>>& GetTestMessages() const override {
        return TestMessages;
    }
    TVector<TVector<std::pair<TString, TString>>> TestMessages = {
        {
            {"1", "one"},
            {"12", "two"},
            {"123", "three"},
            {"1234", "four"}
        },
        {
            {"01", "one"},
            {"012", "two"},
            {"0123", "three"},
            {"01234", "four"}
        }
    };

    bool Run() override {
        const bool prefixed = GetIsPrefixed();
        auto& messagePacks = GetTestMessages();
        NSaas::NYTPull::TScopedYtStorage ytStorage{YtConfig, MakeAtomicShared<TFakeShardDispatcher>()};
        {
            auto builder = ytStorage.MakeBuilder();
            TVector<NRTYServer::TMessage> messages = CreateMessages(prefixed ? 1 : -1, messagePacks[0]);
            builder.CreateChunk(messages, Now().Seconds());
            builder.Commit();
        }

        auto expectedDocCount = messagePacks[0].size();
        if (!WaitAndSearch(expectedDocCount, expectedDocCount)) {
            return false;
        }

        {
            auto builder = ytStorage.MakeBuilder();
            TVector<NRTYServer::TMessage> messages = CreateMessages(prefixed ? 1 : -1, messagePacks[1]);
            builder.CreateChunk(messages, Now().Seconds());
            builder.Commit();
        }
        expectedDocCount = messagePacks[0].size() + messagePacks[1].size();
        return WaitAndSearch(expectedDocCount, expectedDocCount);
    }
};

START_TEST_DEFINE_PARENT(TestYtPullMaxDocAge, YtPullTestBase)
    const ui64 TestMaxDocAge = 1000;

    void TweakConfig() override {
        (*ConfigDiff)["ModulesConfig.DOCFETCHER.Stream.MaxDocAgeToKeepSec"] = TestMaxDocAge;
    }

    const TVector<TVector<std::pair<TString, TString>>>& GetTestMessages() const override {
        return TestMessages;
    }
    TVector<TVector<std::pair<TString, TString>>> TestMessages = {
        {
            {"1", "one"}
        },
        {
            {"12", "two"}
        }
    };

    ui64 GetExpectedDocCount() const override {
        return 3;
    }

    bool Run() override {
        const bool prefixed = GetIsPrefixed();
        auto& messagePacks = GetTestMessages();
        NSaas::NYTPull::TScopedYtStorage ytStorage{YtConfig, MakeAtomicShared<TFakeShardDispatcher>()};
        {
            auto builder = ytStorage.MakeBuilder();
            TVector<NRTYServer::TMessage> messages = CreateMessages(prefixed ? 1 : -1, messagePacks[0]);
            builder.CreateChunk(messages, Now().Seconds() - TestMaxDocAge - 10);
            builder.Commit();
        }
        {
            auto builder = ytStorage.MakeBuilder();
            TVector<NRTYServer::TMessage> messages = CreateMessages(prefixed ? 1 : -1, messagePacks[1]);
            builder.CreateChunk(messages, Now().Seconds());
            builder.Commit();
        }
        auto expectedDocCount = messagePacks[1].size();
        return WaitAndSearch(expectedDocCount, expectedDocCount);
    }
};

START_TEST_DEFINE_PARENT(TestYtPullShardBoundaries, YtPullTestBase)
    void TweakConfig() override {
        (*ConfigDiff)["ModulesConfig.DOCFETCHER.Stream.ShardMax"] = "2";
    }

    const TVector<TVector<std::pair<TString, TString>>>& GetTestMessages() const override {
        return TestMessages;
    }
    TVector<TVector<std::pair<TString, TString>>> TestMessages = {
        {
            {"1", "one"},
            {"12", "two"},
            {"13", "three"},
            {"1234", "four"}
        }
    };

    ui64 GetExpectedDocCount() const override {
        return 3;
    }
};

SERVICE_TEST_RTYSERVER_DEFINE_PARENT(YtPullBackupTestBase, YtPullTestBase)
    const NSaas::NYTPull::TRtyServerTestMROpts YtBackupConfig = NSaas::NYTPull::CreateMROpts("yt_pull");
    TVector<TVector<std::pair<TString, TString>>> Dummy;

    const TVector<TVector<std::pair<TString, TString>>>& GetTestMessages() const override {
        return Dummy;
    }

    void TweakConfig() override {
        (*ConfigDiff)["ModulesConfig.DOCFETCHER.Stream.BackupTable"] = YtBackupConfig.MasterTablePath;
    }
};

START_TEST_DEFINE_PARENT(TestYtPullBackupFromScratch, YtPullBackupTestBase)
    bool Run() override {
        const int kps =  GetIsPrefixed() ? 1 : -1;
        const ui64 now = Now().Seconds();

        Controller->ProcessCommand("pause_docfetcher", Controller->GetActiveBackends());

        NSaas::NYTPull::TScopedYtStorage ytStorage{YtConfig, MakeAtomicShared<TFakeShardDispatcher>()};
        {
            TVector<NRTYServer::TMessage> chunk1 = CreateMessages(kps, {
                {"1", "one"},
                {"2", "two"}
            });
            TVector<NRTYServer::TMessage> chunk2 = CreateMessages(kps, {
                {"3", "three"}
            });
            auto builder = ytStorage.MakeBuilder();
            builder.CreateChunk(chunk1, now - 100);
            builder.CreateChunk(chunk2, now);
            builder.Commit();
        }
        NSaas::NYTPull::TScopedYtStorage backupStorage{YtBackupConfig, MakeAtomicShared<TFakeShardDispatcher>()};
        {
            TVector<NRTYServer::TMessage> chunk1 = CreateMessages(kps, {
                {"0", "zero"},
                {"1", "one"},
                {"2", "two"},
                {"3", "three"},
                {"4", "four"}
            });
            TVector<NRTYServer::TMessage> chunk2 = CreateMessages(kps, {
                {"1", "one"},
                {"2", "two"},
                {"3", "three"},
                {"4", "four"}
            });
            auto builder = backupStorage.MakeBuilder();
            builder.CreateChunk(chunk1, now - 10);
            builder.CreateChunk(chunk2, now);
            builder.Commit();
        }

        Controller->RestartBackend(*(Controller->GetActiveBackends().begin()));

        return WaitAndSearch(4, 4, true);
    }
};

START_TEST_DEFINE_PARENT(TestYtPullBackupFromOneChunk, YtPullBackupTestBase)
    bool Run() override {
        const int kps =  GetIsPrefixed() ? 1 : -1;
        const ui64 now = Now().Seconds();

        NSaas::NYTPull::TScopedYtStorage ytStorage{YtConfig, MakeAtomicShared<TFakeShardDispatcher>()};
        {
            TVector<NRTYServer::TMessage> chunk = CreateMessages(kps, {
                {"1", "one"},
                {"2", "two"}
            });
            auto builder = ytStorage.MakeBuilder();
            builder.CreateChunk(chunk, now - 100);
            builder.Commit();
        }

        if (!WaitAndSearch(2, 2)) {
            return false;
        }
        Controller->ProcessCommand("pause_docfetcher", Controller->GetActiveBackends());

        {
            TVector<NRTYServer::TMessage> chunk = CreateMessages(kps, {
                {"3", "three"},
                {"4", "four"},
                {"5", "five"},
                {"6", "six"},
                {"7", "seven"},
                {"8", "eight"}
            });
            auto builder = ytStorage.MakeBuilder();
            builder.CreateChunk(chunk, now);
            builder.Commit();
        }

        NSaas::NYTPull::TScopedYtStorage backupStorage{YtBackupConfig, MakeAtomicShared<TFakeShardDispatcher>()};
        {
            TVector<NRTYServer::TMessage> chunk = CreateMessages(kps, {
                {"1", "one"},
                {"2", "two"},
                {"3", "three"},
                {"4", "four"}
            });
            auto builder = backupStorage.MakeBuilder();
            builder.CreateChunk(chunk, now);
            builder.Commit();
        }

        Controller->RestartBackend(*(Controller->GetActiveBackends().begin()));

        return WaitAndSearch(4, 4, true);
    }
};

START_TEST_DEFINE_PARENT(TestYtPullInvalidChunk, YtPullTestBase)
    const TVector<TVector<std::pair<TString, TString>>>& GetTestMessages() const override {
        return TestMessages;
    }
    TVector<TVector<std::pair<TString, TString>>> TestMessages = {
        {
            {"1", "one"},
            {"12", "two"},
            {"123", "three"},
            {"1234", "four"}
        }
    };

    bool Run() override {
        const bool prefixed = GetIsPrefixed();
        auto& messagePacks = GetTestMessages();
        auto baseTimestamp = Now().Seconds();
        TString validChunkPath;
        NSaas::NYTPull::TScopedYtStorage ytStorage{YtConfig, MakeAtomicShared<TFakeShardDispatcher>()};
        {
            auto builder = ytStorage.MakeBuilder();
            TVector<NRTYServer::TMessage> messages = CreateMessages(prefixed ? 1 : -1, messagePacks[0]);
            builder.AddExistingChunk(YtConfig.TestFolder + "/invalid_chunk", baseTimestamp);
            validChunkPath = builder.CreateChunk(messages, baseTimestamp + 1000);
            builder.Commit();
        }

        auto expectedDocCount = messagePacks[0].size();
        bool gotAllDocs = WaitForDocuments(TestWaitingTimeout, expectedDocCount);
        CHECK_TEST_TRUE(!gotAllDocs);

        {
            ytStorage.GetYtClient().Remove(YtConfig.MasterTablePath, NYT::TRemoveOptions().Force(true));
            auto builder = ytStorage.MakeBuilder();
            builder.AddExistingChunk(validChunkPath, baseTimestamp + 1000);
            builder.Commit();
        }

        gotAllDocs = WaitForDocuments(TestWaitingTimeout, expectedDocCount);
        CHECK_TEST_TRUE(gotAllDocs);
        return true;
    }
};
