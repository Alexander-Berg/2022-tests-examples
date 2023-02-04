#include <saas/deploy_manager/scripts/database/set_version/action.h>
#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/rtyserver/zoo_sync/zoo_sync.h>

class IVersionSwitcher {
public:
    virtual ~IVersionSwitcher() {}

    virtual void Increment(ui32 delta = 1) = 0;
    virtual void WaitSwitch() = 0;
};

class TZooSyncEmulator: public IMessageProcessor, public IVersionSwitcher {
private:
    ui32 CurrentVersion = 1;

public:
    TZooSyncEmulator() {
        RegisterGlobalMessageProcessor(this);
    }

    ~TZooSyncEmulator() {
        UnregisterGlobalMessageProcessor(this);
    }

    void Increment(ui32 delta = 1) override {
        SendGlobalMessage<TMessageBaseVersionChanged>(CurrentVersion, CurrentVersion + delta, nullptr);
        CurrentVersion += delta;
    }

    void WaitSwitch() override {
    }

    bool Process(IMessage* message) override {
        TRequestCurrentDatabaseVersion* messVersionRequest = dynamic_cast<TRequestCurrentDatabaseVersion*>(message);
        if (messVersionRequest) {
            messVersionRequest->Version = CurrentVersion;
            return true;
        }
        return false;
    }

    TString Name() const override {
        return "ZooSyncEmulator";
    }
};

class TDeployManagerSwitcher : public IVersionSwitcher {
private:
    TBackendProxy* Controller;
    const TString Service;
    const TString CType;

    ui32 Version;
    TMutex Mutex;

public:
    TDeployManagerSwitcher(TBackendProxy* controller, const TString& service, const TString& ctype)
        : Controller(controller)
        , Service(service)
        , CType(ctype)
        , Version(1)
    {
        SetVersion(Version);
    }

    void Increment(ui32 delta = 1) override {
        TGuard<TMutex> guard(Mutex);
        Version += delta;
        SetVersion(Version);
    }

    void WaitSwitch() override {
        ui32 version = 0;

        for (ui32 attempt = 0; attempt < 10; ++attempt) {
            auto infoServerPtr = Controller->GetServerInfo();
            Y_ENSURE(infoServerPtr, "Cannot get info server");

            NJson::TJsonValue infoServer = *infoServerPtr;
            version = infoServer[0]["database_version"].GetUIntegerRobust();
            if (version >= Version) {
                break;
            }

            Sleep(TDuration::Seconds(10));
        }

        if (version < Version) {
            throw yexception() << "WaitSwitch timeout: " << version << " != " << Version;
        }
    }

private:
    void SetVersion(ui32 version) {
        NRTYDeploy::TActionSetDatabaseVersion action(Service, CType, version);
        Controller->ExecuteActionOnDeployManager(action);
    }
};

SERVICE_TEST_RTYSERVER_DEFINE(TestVersionSwitch)
THolder<IVersionSwitcher> VersionSwitcher = MakeHolder<TZooSyncEmulator>();
bool InitConfig() override {
    (*ConfigDiff)["ModulesConfig.ZooSynchronizer.CheckIntervalSeconds"] = 5;
    return true;
}
};

SERVICE_TEST_RTYSERVER_DEFINE_PARENT(TestDeferredIndexationIncrementBase, TestVersionSwitch)

bool InitConfig() override {
    if (!TestVersionSwitch::InitConfig()) {
        return false;
    }

    SetIndexerParams(ALL, 600, 1);
    SetMergerParams(true, 1, -1, mcpNONE);

    (*ConfigDiff)["IndexGenerator"] = INDEX_COMPONENT_NAME;
    (*ConfigDiff)["Components"] = FULL_ARCHIVE_COMPONENT_NAME;
    (*ConfigDiff)["Searcher.SnippetsDeniedZones"] = "";
    (*ConfigDiff)["ExternalLogicConfig.DeferredIndexation.StoragePath"] = GetIndexDir() + "/deferred_storage";
    return true;
}

bool Run() override {
    if (GetIsPrefixed())
        return true;

    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 10, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());

    for (ui32 i = 0; i < messages.size(); ++i) {
        messages[i].MutableDocument()->SetDatabaseVersion(i + 1);
    }

    IndexMessages(messages, REALTIME, 1);
    for (ui32 i = 0; i < messages.size() / 2; ++i) {
        TVector<TDocSearchInfo> results;
        TQuerySearchContext context;
        context.AttemptionsCount = 5;
        context.ResultCountRequirement = 2 * i + 1;
        QuerySearch("url:\"*\"", results, context);
        CHECK_TEST_EQ(results.size(), 2 * i + 1);
        Controller->RestartServer();
        VersionSwitcher->Increment(2);
        VersionSwitcher->WaitSwitch();
    }

    return true;
}

};

START_TEST_DEFINE_PARENT(TestDeferredIndexationIncrementEmulator, TestDeferredIndexationIncrementBase)
};
START_TEST_DEFINE_PARENT(TestDeferredIndexationIncrementZK, TestDeferredIndexationIncrementBase)
bool InitConfig() override {
    if (!TestDeferredIndexationIncrementBase::InitConfig()) {
        return false;
    }

    CHECK_WITH_LOG(Cluster);

    const TString service = "tests";
    const TString ctype = "test-ctype";
    (*ConfigDiff)["AdditionalModules"] = "ZooSynchronizer";
    (*ConfigDiff)["ModulesConfig.ZooSynchronizer.Address"] = "zookeeper-prestable1.search.yandex.net:2281,zookeeper-prestable2.search.yandex.net:2281,zookeeper-prestable3.search.yandex.net:2281,zookeeper-prestable4.search.yandex.net:2281,zookeeper-prestable5.search.yandex.net:2281";
    (*ConfigDiff)["ModulesConfig.ZooSynchronizer.Root"] = Cluster->GetConfig().PreprocessorPatches.at("DeployManager.Storage.Zoo.Root");
    (*ConfigDiff)["ModulesConfig.ZooSynchronizer.ServiceName"] = service;
    (*ConfigDiff)["ModulesConfig.ZooSynchronizer.CType"] = ctype;
    Y_ASSERT(Controller);
    VersionSwitcher = MakeHolder<TDeployManagerSwitcher>(Controller, service, ctype);

    return true;
}
};

SERVICE_TEST_RTYSERVER_DEFINE_PARENT(TestDeferredIndexationBigStepBase, TestVersionSwitch)

bool InitConfig() override {
    if (!TestVersionSwitch::InitConfig()) {
        return false;
    }

    SetIndexerParams(ALL, 600, 1);
    SetMergerParams(true, 1, -1, mcpNONE);

    (*ConfigDiff)["IndexGenerator"] = INDEX_COMPONENT_NAME;
    (*ConfigDiff)["Components"] = FULL_ARCHIVE_COMPONENT_NAME;
    (*ConfigDiff)["Searcher.SnippetsDeniedZones"] = "";
    (*ConfigDiff)["ExternalLogicConfig.DeferredIndexation.StoragePath"] = GetIndexDir() + "/deferred_storage";
    return true;
}

bool Run() override {
    if (GetIsPrefixed())
        return true;

    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 10, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());

    for (ui32 i = 0; i < messages.size(); ++i) {
        messages[i].MutableDocument()->SetDatabaseVersion(i + 1);
    }

    IndexMessages(messages, REALTIME, 1);
    TVector<TDocSearchInfo> results;
    TQuerySearchContext context;
    context.AttemptionsCount = 5;
    context.ResultCountRequirement = 1;
    QuerySearch("url:\"*\"", results, context);
    CHECK_TEST_EQ(results.size(), 1);

    VersionSwitcher->Increment(10);
    VersionSwitcher->WaitSwitch();

    context.AttemptionsCount = 5;
    context.ResultCountRequirement = 10;
    QuerySearch("url:\"*\"", results, context);
    CHECK_TEST_EQ(results.size(), 10);

    return true;
}

};

START_TEST_DEFINE_PARENT(TestDeferredIndexationBigStepEmulator, TestDeferredIndexationBigStepBase)
};
START_TEST_DEFINE_PARENT(TestDeferredIndexationBigStepZK, TestDeferredIndexationBigStepBase)
bool InitConfig() override {
    if (!TestDeferredIndexationBigStepBase::InitConfig()) {
        return false;
    }

    CHECK_WITH_LOG(Cluster);

    const TString service = "tests";
    const TString ctype = "test-ctype";
    (*ConfigDiff)["AdditionalModules"] = "ZooSynchronizer";
    (*ConfigDiff)["ModulesConfig.ZooSynchronizer.Address"] = "zookeeper-prestable1.search.yandex.net:2281,zookeeper-prestable2.search.yandex.net:2281,zookeeper-prestable3.search.yandex.net:2281,zookeeper-prestable4.search.yandex.net:2281,zookeeper-prestable5.search.yandex.net:2281";
    (*ConfigDiff)["ModulesConfig.ZooSynchronizer.Root"] = Cluster->GetConfig().PreprocessorPatches.at("DeployManager.Storage.Zoo.Root");
    (*ConfigDiff)["ModulesConfig.ZooSynchronizer.ServiceName"] = service;
    (*ConfigDiff)["ModulesConfig.ZooSynchronizer.CType"] = ctype;
    Y_ASSERT(Controller);
    VersionSwitcher = MakeHolder<TDeployManagerSwitcher>(Controller, service, ctype);

    return true;
}
};
