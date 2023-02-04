#include <saas/rtyserver_test/testerlib/rtyserver_test.h>

SERVICE_TEST_RTYSERVER_DEFINE(SuspendIdleParent)
bool Run() override {
    //special 'test' for external usage, run cluster and do nothing
    ui16 iPort = 0;
    if (Callback->GetNodesNames(TNODE_INDEXERPROXY).size() > 0) {
        iPort = Controller->GetConfig().Indexer.Port;
    } else {
        iPort = Controller->GetConfig().Controllers[0].Port - 1;
    }
    ui16 sPort = 0;
    if (Callback->GetNodesNames(TNODE_SEARCHPROXY).size() > 0) {
        sPort = Controller->GetConfig().Searcher.Port;
    } else {
        sPort = Controller->GetConfig().Controllers[0].Port - 3;
    }
    Cout << ";search_port=" << sPort << ";indexer_port=" << iPort << ";";
    Cout.Flush();
    while (true) {
        Sleep(TDuration::Seconds(5));
    }
    return true;
}
bool ConfigureRuns(TTestVariants& /*variants*/, bool) override {
    return true;
}
};

START_TEST_DEFINE_PARENT(SuspendIdle, SuspendIdleParent)
bool InitConfig() override {
    TFsPath configsPath(GetResourcesDirectory());
    if (configsPath.Exists()) {
        TVector<TFsPath> files;
        configsPath.List(files);
        for (const auto& file : files) {
            TConfigFieldsPtr diff;
            if (file.Basename().StartsWith("rtyserver")) {
                diff = ConfigDiff;
            } else if (file.Basename().StartsWith("indexerproxy")) {
                diff = IPConfigDiff;
            } else if (file.Basename().StartsWith("searchproxy")) {
                diff = SPConfigDiff;
            }
            if (!diff)
                continue;
            TFileInput fi(file);
            diff->Deserialize(fi.ReadAll());
        }
    }
    return true;
}
};

START_TEST_DEFINE_PARENT(SuspendIdleKv, SuspendIdleParent)
bool InitConfig() override {
    (*ConfigDiff)["IndexGenerator"] = FULL_ARCHIVE_COMPONENT_NAME;
    (*ConfigDiff)["Components"] = FULL_ARCHIVE_COMPONENT_NAME;
    (*ConfigDiff)["Indexer.Memory.Enabled"] = "true";
    (*ConfigDiff)["Searcher.SnippetsDeniedZones"] = "";
    (*ConfigDiff)["Searcher.TwoStepQuery"] = "false";
    (*SPConfigDiff)["Service.MetaSearch.TwoStepQuery"] = "false";
    return true;
}
};
