#include <saas/rtyserver_test/cases/search/mix_factors.h>

#include <saas/rtyserver/indexer_core/index_dir.h>
#include <saas/library/sharding/sharding.h>
#include <saas/library/sharding/rules/urlhash.h>

static NSaas::TShardsDispatcher::TContext UrlShardingContext(NSaas::UrlHash);

START_TEST_DEFINE_PARENT(TestSimpleSynchronization, TestMixFactorsBase)
bool InitConfig() override {
    if (!TestMixFactorsBase::InitConfig())
        return false;
    SetMergerParams(true, 1, -1, mcpNONE);
    return true;
}
virtual bool DelSome() const {
    return true;
}
virtual bool NeedsMergeToSearch() const {
    return false;
}
virtual bool OnAfterSimpleSync() {
    return true;
}
virtual bool OnAfterReshardedSync() {
    return true;
}
void MergeIndexes() {
    Controller->ProcessCommand("create_merger_tasks");
    Controller->ProcessCommand("do_all_merger_tasks");
}
void RemoveAllDocs() {
    DeleteSpecial();
    MergeIndexes();
}

bool Run() override {
    GenerateMessages(100);

    IndexMessages(Messages, DISK, 1);
    ReopenIndexers();
    IndexMessages(Messages, DISK, 1);
    ReopenIndexers();
    IndexMessages(Messages, DISK, 1);

    if (!SendIndexReply) {
        Controller->WaitEmptyIndexingQueues();
        sleep(10);
    }
    ReopenIndexers();
    TSet<std::pair<ui64, TString> > deleted;
    if (DelSome())
        DeleteSomeMessages(Messages, deleted, REALTIME);

    if (Y_LIKELY(!NeedsMergeToSearch())) {
        CheckSearchResults(Messages, deleted, 3);
    }

    TString reply;
    if (!Controller->Detach(0, NSearchMapParser::SearchMapShards, UrlShardingContext, reply)) {
        ERROR_LOG << "Detach failed: " << reply << Endl;
        return false;
    }

    RemoveAllDocs();

    Controller->ProcessCommand("restart");

    TString syncReply;
    if (!Controller->Synchronize(reply, syncReply)) {
        ERROR_LOG << "Synchronization failed: " << syncReply << Endl;
        return false;
    }

    Controller->RestartServer();
    CHECK_TEST_TRUE(OnAfterSimpleSync());

    Controller->ProcessCommand("do_all_merger_tasks");
    CheckSearchResults(Messages, deleted, 1);

    TString replyHalf;
    if (!Controller->Detach(0, NSearchMapParser::SearchMapShards / 2, UrlShardingContext, replyHalf)) {
        ERROR_LOG << "Detach failed: " << reply << Endl;
        return false;
    }

    RemoveAllDocs();

    TString syncReplyHalf;
    if (!Controller->Synchronize(replyHalf, syncReplyHalf)) {
        ERROR_LOG << "Synchronization failed: " << syncReplyHalf << Endl;
        return false;
    }
    Controller->ProcessCommand("stop");
    TConfigFieldsPtr diff(new TConfigFields);
    (*diff)["Merger.Enabled"] = false;
    Controller->ApplyConfigDiff(diff);
    Controller->RestartServer();
    CHECK_TEST_TRUE(OnAfterReshardedSync());

    TVector<TDocSearchInfo> results;
    QuerySearch("url:\"*\"" + GetAllKps(Messages) + "&numdoc=10000", results, nullptr, nullptr, true);
    ui32 count = 0;
    for (TVector<TDocSearchInfo>::const_iterator i = results.begin(); i != results.end(); ++i) {
        NSearchMapParser::TShardIndex shard = NSaas::TUrlShardingRule::GetUrlShard(i->GetUrl(), NSearchMapParser::SearchMapShards);
        INFO_LOG << "Checking shard for " << i->GetUrl() << " : " << shard << " < " << NSearchMapParser::SearchMapShards / 2 << Endl;
        CHECK_TEST_LESSEQ(shard, NSearchMapParser::SearchMapShards / 2);
        ++count;
    }
    DEBUG_LOG << "result docs count: " << count << Endl;
    CheckResults(results.size());

    return true;
}
};

START_TEST_DEFINE_PARENT(TestPrepsSynchronization, TTestSimpleSynchronizationCaseClass)
bool InitConfig() override {
    if (!TTestSimpleSynchronizationCaseClass::InitConfig())
        return false;
    (*ConfigDiff)["Indexer.Disk.PreparatesMode"] = true;
    (*ConfigDiff)["Indexer.Memory.Enabled"] = false;
    (*ConfigDiff)["SearchersCountLimit"] = 1;;
    return true;
}

bool NeedsMergeToSearch() const override {
    return true;
}

ui32 GetPrepSegmentsCount() {
    ui32 count = 0;
    auto indexDirs = Controller->GetFinalIndexes(/*stopServer=*/false);
    for (const TString& dir : indexDirs) {
        if (NRTYServer::HasIndexDirPrefix(dir, DIRPREFIX_PREP)) {
            ++count;
        }
    }
    return count;
}

bool OnAfterSimpleSync() override {
    return GetPrepSegmentsCount() == 0;
}

bool OnAfterReshardedSync() override {
    const bool checkOk = GetPrepSegmentsCount() == 0;
    return checkOk;
}
};

START_TEST_DEFINE_PARENT(TestMultiIntervalSynchronization, TestMixFactorsBase)
bool InitConfig() override {
    if (!TestMixFactorsBase::InitConfig())
        return false;
    SetMergerParams(true, 1, -1, mcpNONE);
    return true;
}

bool Run() override {
    GenerateMessages(100);

    IndexMessages(Messages, DISK, 1);
    ReopenIndexers();
    IndexMessages(Messages, DISK, 1);
    ReopenIndexers();
    IndexMessages(Messages, DISK, 1);

    if (!SendIndexReply) {
        Controller->WaitEmptyIndexingQueues();
        sleep(10);
    }
    ReopenIndexers();

    TVector<TDocSearchInfo> results;
    TSet<std::pair<ui64, TString> > deleted;

    CheckSearchResults(Messages, deleted, 3);
    TVector<TString> reply;
    {
        TVector<NSearchMapParser::TShardIndex> smin;
        TVector<NSearchMapParser::TShardIndex> smax;
        smin.push_back(0);
        smax.push_back(NSearchMapParser::SearchMapShards / 3);
        smin.push_back(NSearchMapParser::SearchMapShards / 3 + 1);
        smax.push_back(NSearchMapParser::SearchMapShards);
        if (!Controller->Detach(smin, smax, UrlShardingContext, reply)) {
            ERROR_LOG << "Detach failed" << Endl;
            return false;
        }
    }

    DeleteSpecial();
    Controller->ProcessCommand("create_merger_tasks");
    Controller->ProcessCommand("do_all_merger_tasks");

    TString syncReply;
    if (!Controller->Synchronize(reply[0], syncReply)) {
        ERROR_LOG << "Synchronization failed: " << syncReply << Endl;
        return false;
    }
    if (!Controller->Synchronize(reply[1], syncReply)) {
        ERROR_LOG << "Synchronization failed: " << syncReply << Endl;
        return false;
    }

    Controller->RestartServer();

    CheckSearchResults(Messages, deleted, 1);

    TVector<TString> replyHalf;
    {
        TVector<NSearchMapParser::TShardIndex> smin;
        TVector<NSearchMapParser::TShardIndex> smax;
        smin.push_back(0);
        smax.push_back(NSearchMapParser::SearchMapShards / 4);
        smin.push_back((NSearchMapParser::SearchMapShards * 3) / 4);
        smax.push_back(NSearchMapParser::SearchMapShards);
        if (!Controller->Detach(smin, smax, UrlShardingContext, replyHalf)) {
            ERROR_LOG << "Detach failed" << Endl;
            return false;
        }
    }

    DeleteSpecial();

    Controller->ProcessCommand("create_merger_tasks");
    Controller->ProcessCommand("do_all_merger_tasks");

    TString syncReplyHalf;
    if (!Controller->Synchronize(replyHalf[0], syncReplyHalf)) {
        ERROR_LOG << "Synchronization failed: " << syncReplyHalf << Endl;
        return false;
    }
    if (!Controller->Synchronize(replyHalf[1], syncReplyHalf)) {
        ERROR_LOG << "Synchronization failed: " << syncReplyHalf << Endl;
        return false;
    }
    Controller->ProcessCommand("stop");
    TConfigFieldsPtr diff(new TConfigFields);
    (*diff)["Merger.Enabled"] = false;
    Controller->ApplyConfigDiff(diff);
    Controller->RestartServer();

    QuerySearch("url:\"*\"" + GetAllKps(Messages) + "&numdoc=10000", results, nullptr, nullptr, true);
    ui32 count = 0;
    for (TVector<TDocSearchInfo>::const_iterator i = results.begin(); i != results.end(); ++i) {
        NSearchMapParser::TShardIndex shard = NSaas::TUrlShardingRule::GetUrlShard(i->GetUrl(), NSearchMapParser::SearchMapShards);
        if ((shard > NSearchMapParser::SearchMapShards / 4) && (shard < (NSearchMapParser::SearchMapShards * 3) / 4))
            ythrow yexception() << "wrong document download: " << i->GetUrl();
        ++count;
    }
    DEBUG_LOG << count << Endl;
    CheckResults(results.size());

    return true;
}
};

START_TEST_DEFINE_PARENT(TestSimpleSynchronizationFewSegments, TTestSimpleSynchronizationCaseClass)
    bool DelSome() const override {
        return false;
    }
    bool Run() override {
        if (!TTestSimpleSynchronizationCaseClass::Run())
            return false;
        TSet<TString> fi = Controller->GetFinalIndexes();
        if (fi.size() != 2)
            ythrow yexception() << "invalid segments count: " << fi.size() << " != 2";
        return true;
    }

    bool InitConfig() override {
        if (!TTestSimpleSynchronizationCaseClass::InitConfig())
            return false;
        (*ConfigDiff)["ModulesConfig.Synchronizer.DetachedSegmentSize"] = 50;
        (*ConfigDiff)["ModulesConfig.Synchronizer.DetachedSegmentSizeDeviation"] = 0;
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestRemoveSynchronization, TestMixFactorsBase)
bool Run() override {
    GenerateMessages(100);

    IndexMessages(Messages, DISK, 1);
    if (!SendIndexReply) {
        Controller->WaitEmptyIndexingQueues();
        sleep(10);
    }
    ReopenIndexers();

    TVector<TDocSearchInfo> results;
    TSet<std::pair<ui64, TString> > deleted;

    CheckSearchResults(Messages, deleted, 1);

    TString reply;
    if (!Controller->ShardsAction(0, NSearchMapParser::SearchMapShards / 2, UrlShardingContext, reply, "remove")) {
        ERROR_LOG << "Remove failed: " << reply << Endl;
        return false;
    }

    QuerySearch("url:\"*\"&" + GetAllKps(Messages) + "&numdoc=10000", results, nullptr, nullptr, true);

    if (Abs((long)(results.size() - Messages.size() / 2)) > 6) {
        ERROR_LOG << "Hash very strange: " << Abs((long)(results.size() - Messages.size() / 2)) << Endl;
        return false;
    }

    TString replyCheck;
    if (!Controller->ShardsAction(NSearchMapParser::SearchMapShards / 2 + 1, NSearchMapParser::SearchMapShards, UrlShardingContext, replyCheck, "check")) {
        ERROR_LOG << "Check failed: " << reply << Endl;
        return false;
    }

    if (replyCheck.find("\"shards_result\":\"OK\"") == TString::npos) {
        ERROR_LOG << "Check result failed: " << replyCheck << Endl;
        return false;
    }

    return true;
}

bool InitConfig() override {
    if (!TestMixFactorsBase::InitConfig())
        return false;
    if (!SendIndexReply) {
        SetIndexerParams(ALL, 2000, -1, 0);
        SetMergerParams(false);
    }
    return true;
}
};

START_TEST_DEFINE_PARENT(TestDoubleSynchronization, TestMixFactorsBase)
bool Run() override {
    GenerateMessages(100);

    IndexMessages(Messages, DISK, 1);
    if (!SendIndexReply) {
        Controller->WaitEmptyIndexingQueues();
        sleep(10);
    }
    ReopenIndexers();

    TVector<TDocSearchInfo> results;
    TSet<std::pair<ui64, TString> > deleted;

    CheckSearchResults(Messages, deleted, 1);

    TString reply1;
    if (!Controller->Detach(0, NSearchMapParser::SearchMapShards / 2, UrlShardingContext, reply1)) {
        ERROR_LOG << "Detach failed: " << reply1 << Endl;
        return false;
    }

    if (!Controller->Detach(0, NSearchMapParser::SearchMapShards / 2, UrlShardingContext, reply1)) {
        ERROR_LOG << "Detach failed: " << reply1 << Endl;
        return false;
    }

    TString reply2;
    if (!Controller->Detach(NSearchMapParser::SearchMapShards / 2 + 1, NSearchMapParser::SearchMapShards, UrlShardingContext, reply2)) {
        ERROR_LOG << "Detach failed: " << reply2 << Endl;
        return false;
    }

    DeleteSpecial();

    TString syncReply;
    if (!Controller->Synchronize(reply1, syncReply)) {
        ERROR_LOG << "Synchronization failed: " << syncReply << Endl;
        return false;
    }

    if (!Controller->Synchronize(reply2, syncReply)) {
        ERROR_LOG << "Synchronization failed: " << syncReply << Endl;
        return false;
    }

    Controller->RestartServer();

    CheckSearchResults(Messages, deleted);

    return true;
}

bool InitConfig() override {
    if (!TestMixFactorsBase::InitConfig())
        return false;
    if (!SendIndexReply) {
        SetIndexerParams(ALL, 2000, -1, 0);
        SetMergerParams(false);
    }
    return true;
}
};


START_TEST_DEFINE_PARENT(TestSimpleSynchronizationDefaultSpeedLimit, TestMixFactorsBase)
bool InitConfig() override {
    if (!TestMixFactorsBase::InitConfig())
        return false;
    SetMergerParams(true, 1, -1, mcpNONE);
    return true;
}

virtual bool CheckSkyArgs(const TString& syncResult) {
    return DoCheckSkyArgs(syncResult, 0, 0, 0);
}

static bool CheckHasOption(const TString& cmdLine, const TString& option, const ui64 value) {
    bool found = false;
    bool valueNext = false;
    const TVector<TString> split = SplitString(cmdLine, " ");
    for (const TString& item: split) {
        if (item.find(option + "=") == 0) {
            CHECK_TEST_EQ(item, option + "=" + ToString(value));
            found = true;
        } else if (valueNext) {
            valueNext = false;
            CHECK_TEST_EQ(item, ToString(value));
            found = true;
        } else if (item == option) {
            valueNext = true;
        }
    }
    return found;
}

static bool DoCheckSkyArgs(const TString& syncResult, const ui64 ulSpeed, const ui64 dlSpeed, const ui32 timeoutSec) {
    NJson::TJsonValue result;
    TStringInput si(syncResult);
    if (!NJson::ReadJsonTree(&si, &result))
        ythrow yexception() << "incorrect json from sky: " << syncResult;

    CHECK_TEST_TRUE(result.Has("sky_progress"));
    const NJson::TJsonValue& progress = result["sky_progress"];
    CHECK_TEST_TRUE(progress.IsArray());
    for (const NJson::TJsonValue& entry: progress.GetArray()) {
        CHECK_TEST_TRUE(entry.IsMap());
        CHECK_TEST_TRUE(entry.Has("command-line"));
        const TString skyLine = entry["command-line"].GetString();
        if (!ulSpeed) {
            CHECK_TEST_TRUE(skyLine.find("--max-ul-speed") == TString::npos);
        } else {
            CHECK_TEST_TRUE(CheckHasOption(skyLine, "--max-ul-speed", ulSpeed));
        }
        if (!dlSpeed) {
            CHECK_TEST_TRUE(skyLine.find("--max-dl-speed") == TString::npos);
        } else {
            CHECK_TEST_TRUE(CheckHasOption(skyLine, "--max-dl-speed", dlSpeed));
        }
        if (timeoutSec) {
            CHECK_TEST_TRUE(CheckHasOption(skyLine, "-t", timeoutSec));
        }
    }
    return true;
}

bool Run() override {
    GenerateMessages(100);

    IndexMessages(Messages, DISK, 1);
    ReopenIndexers();

    if (!SendIndexReply) {
        Controller->WaitEmptyIndexingQueues();
        sleep(10);
    }

    ReopenIndexers();

    CheckSearchResults(Messages);

    TString reply;
    if (!Controller->Detach(0, NSearchMapParser::SearchMapShards, UrlShardingContext, reply)) {
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
    Controller->ProcessCommand("create_merger_tasks");
    Controller->ProcessCommand("do_all_merger_tasks");
    CheckSearchResults(Messages);

    CHECK_TEST_TRUE(CheckSkyArgs(syncReply));
    return true;
}
};

START_TEST_DEFINE_PARENT(TestSimpleSynchronizationGlobalSpeedLimit, TTestSimpleSynchronizationDefaultSpeedLimitCaseClass)

bool InitConfig() override {
    if (!TTestSimpleSynchronizationDefaultSpeedLimitCaseClass::InitConfig())
        return false;
    (*ConfigDiff)["ResourceFetchConfig.SkyGet.DownloadSpeedBps"] = "104857600";
    (*ConfigDiff)["ResourceFetchConfig.SkyGet.UploadSpeedBps"] = "104857600";
    (*ConfigDiff)["ResourceFetchConfig.SkyGet.TimeoutSeconds"] = "66";
    return true;
}

bool CheckSkyArgs(const TString& syncResult) override {
    return DoCheckSkyArgs(syncResult, 104857600, 104857600, 66);
}

};


START_TEST_DEFINE_PARENT(TestSimpleSynchronizationSyncSpeedLimit, TTestSimpleSynchronizationDefaultSpeedLimitCaseClass)

bool InitConfig() override {
    if (!TTestSimpleSynchronizationDefaultSpeedLimitCaseClass::InitConfig())
        return false;
    (*ConfigDiff)["ModulesConfig.Synchronizer.ResourceFetchConfig.SkyGet.DownloadSpeedBps"] = "10485760";
    (*ConfigDiff)["ModulesConfig.Synchronizer.ResourceFetchConfig.SkyGet.UploadSpeedBps"] = "104857600";
    (*ConfigDiff)["ModulesConfig.Synchronizer.ResourceFetchConfig.SkyGet.TimeoutSeconds"] = "66";
    return true;
}

bool CheckSkyArgs(const TString& syncResult) override {
    return DoCheckSkyArgs(syncResult, 104857600, 10485760, 66);
}

};

START_TEST_DEFINE_PARENT(TestSimpleSynchronizationSyncSpeedLimitOverride, TTestSimpleSynchronizationDefaultSpeedLimitCaseClass)

bool InitConfig() override {
    if (!TTestSimpleSynchronizationDefaultSpeedLimitCaseClass::InitConfig())
        return false;

    (*ConfigDiff)["ResourceFetchConfig.SkyGet.DownloadSpeedBps"] = "104857600";
    (*ConfigDiff)["ResourceFetchConfig.SkyGet.UploadSpeedBps"] = "104857600";
    (*ConfigDiff)["ResourceFetchConfig.SkyGet.TimeoutSeconds"] = "10";
    (*ConfigDiff)["ModulesConfig.Synchronizer.ResourceFetchConfig.SkyGet.DownloadSpeedBps"] = "10485760";
    (*ConfigDiff)["ModulesConfig.Synchronizer.ResourceFetchConfig.SkyGet.UploadSpeedBps"] = "10485760";
    (*ConfigDiff)["ModulesConfig.Synchronizer.ResourceFetchConfig.SkyGet.TimeoutSeconds"] = "66";
    return true;
}

bool CheckSkyArgs(const TString& syncResult) override {
    return DoCheckSkyArgs(syncResult, 10485760, 10485760, 66);
}

};

START_TEST_DEFINE_PARENT(TestSimpleSynchronizationSyncSpeedLimitOverrideZero, TTestSimpleSynchronizationDefaultSpeedLimitCaseClass)

bool InitConfig() override {
    if (!TTestSimpleSynchronizationDefaultSpeedLimitCaseClass::InitConfig())
        return false;

    (*ConfigDiff)["ResourceFetchConfig.SkyGet.DownloadSpeedBps"] = "104857600";
    (*ConfigDiff)["ResourceFetchConfig.SkyGet.UploadSpeedBps"] = "104857600";
    (*ConfigDiff)["ModulesConfig.Synchronizer.ResourceFetchConfig.SkyGet.DownloadSpeedBps"] = "0";
    (*ConfigDiff)["ModulesConfig.Synchronizer.ResourceFetchConfig.SkyGet.UploadSpeedBps"] = "0";
    return true;
}

bool CheckSkyArgs(const TString& syncResult) override {
    return DoCheckSkyArgs(syncResult, 0, 0, 0);
}

};

