#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/rtyserver/components/fullarchive/disk_manager.h>
#include <saas/rtyserver/components/fullarchive/globals.h>
#include <saas/rtyserver/indexer_core/index_metadata_processor.h>



START_TEST_DEFINE(TestFullArchiveVersionChanges, TTestMarksPool::OneBackendOnly)
bool Run() override {
    TFsPath path("full_arc/without_meta");
    Controller->ProcessCommand("stop");
    if (GetIsPrefixed())
        PrepareData(path / "prefix/");
    else
        PrepareData(path / "non_prefix/");

    Controller->RestartServer();

    CHECK_TEST_TRUE(QueryCount() == 100);

    TSet<TString> indexes = Controller->GetFinalIndexes();
    Controller->ProcessCommand("stop");

    for (const auto& path : indexes) {
        TFsPath final(path);
        ui32 version = TIndexMetadataProcessor(final)->GetFullArcHeader().GetVersion();
        if (version != FULL_ARC_VERSION)
            ythrow yexception() << "invalid version in " << final.GetPath() << ", " << version << " != " << FULL_ARC_VERSION;
        TFsPath arcPath = final / "indexfullarc";
        CHECK_TEST_TRUE(TArchiveOwner::Check(arcPath.GetPath() + ".full"));
        CHECK_TEST_TRUE(TArchiveOwner::Check(arcPath.GetPath() + ".base"));
    }
    return true;
}

bool InitConfig() override {
    SetIndexerParams(DISK, 10, 1);
    SetIndexerParams(REALTIME, 10);
    return true;
}
};

START_TEST_DEFINE(TestFullArchiveRepairArchiveWithEmptyPart, TTestMarksPool::OneBackendOnly)
bool Run() override {
    TFsPath path("full_arc/corrupted/data");
    Controller->ProcessCommand("stop");
    if (GetIsPrefixed())
        PrepareData(path / "prefix/");
    else
        PrepareData(path / "non_prefix/");
    Controller->RestartServer();

    CHECK_TEST_EQ(QueryCount(), 10);

    TSet<TString> indexes = Controller->GetFinalIndexes();
    Controller->ProcessCommand("stop");

    for (const auto& path : indexes) {
        TFsPath final(path);
        ui32 version = TIndexMetadataProcessor(final)->GetFullArcHeader().GetVersion();
        if (version != FULL_ARC_VERSION)
            ythrow yexception() << "invalid version in " << final.GetPath() << ", " << version << " != " << FULL_ARC_VERSION;
        TFsPath arcPath = final / "indexfullarc";
        CHECK_TEST_TRUE(TArchiveOwner::Check(arcPath.GetPath() + ".base"));
    }
    return true;
}

bool InitConfig() override {
    SetIndexerParams(DISK, 10, 1);
    SetIndexerParams(REALTIME, 10);
    (*ConfigDiff)["ComponentsConfig.FULLARC.ActiveLayers"] = "base";
    (*ConfigDiff)["Indexer.Common.UseSlowUpdate"] = "0";
    (*ConfigDiff)["Components"] = "INDEX,MAKEUP,DDK,FASTARC,Keys";
    return true;
}
};

START_TEST_DEFINE(TestFullArchiveRepair, TTestMarksPool::OneBackendOnly)
bool Run() override {
    ui64 docsCount = 100;
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, docsCount, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    IndexMessages(messages, DISK, 1);

    TSet<TString> indexes = Controller->GetFinalIndexes();
    Controller->ProcessCommand("stop");
    for (const auto& path : indexes) {
        TFsPath final(path);
        TFsPath temp(final.Parent() / ("temp_" + final.GetName().substr(strlen("index"))));
        temp.MkDirs();
        NRTYArchive::HardLinkOrCopy(final / FULL_ARC_FILE_NAME_PREFIX, temp / FULL_ARC_FILE_NAME_PREFIX);
        TFile normalIndex(temp / "normal_index", WrOnly | OpenAlways);
        *TIndexMetadataProcessor(temp) = *TIndexMetadataProcessor(final);
        final.ForceDelete();
    }
    Controller->RestartServer();
    Controller->WaitIsRepairing();
    ui32 get = QueryCount();
    if (get != docsCount)
        ythrow yexception() << "Incorrect doc count after repair: " << get << " != " << docsCount;

    return true;
}

bool InitConfig() override {
    SetIndexerParams(DISK, 30, 1);
    SetIndexerParams(REALTIME, 30);
    SetEnabledRepair();
    return true;
}
};


START_TEST_DEFINE(TestFullArchiveRestartAfterFail, TTestMarksPool::OneBackendOnly)
bool Run() override {
    using namespace NRTYArchive;

    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 10000, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    IndexMessages(messages, DISK, 1);
    ReopenIndexers();
    TSet<TString> indexes = Controller->GetFinalIndexes();
    Controller->ProcessCommand("stop");
    for (const auto& path : indexes) {
        TVector<ui32> partIndexes;
        const TString arcPrefix = path + "/indexfullarc.full";
        NRTYArchive::TMultipartArchive::FillPartsIndexes(arcPrefix, partIndexes);
        for (auto& index : partIndexes) {
            CHECK_TEST_TRUE(TPartMetaSaver(GetPartMetaPath(arcPrefix, index))->GetStage() == TPartMetaInfo::CLOSED);
            TPartMetaSaver(GetPartMetaPath(arcPrefix, index))->SetStage(TPartMetaInfo::OPENED);
        }
    }
    Controller->RestartServer();
    Controller->ProcessCommand("create_merger_tasks");
    Controller->ProcessCommand("do_all_merger_tasks");

    indexes = Controller->GetFinalIndexes();
    Controller->ProcessCommand("stop");
    if (GetIsPrefixed()) {
        CHECK_TEST_EQ(indexes.size(), 5);
    } else {
        CHECK_TEST_EQ(indexes.size(), 1);
    }

    for (const auto& path : indexes) {
        TVector<ui32> partIndexes;
        const TString arcPrefix = path + "/indexfullarc.full";
        NRTYArchive::TMultipartArchive::FillPartsIndexes(arcPrefix, partIndexes);
        for (auto& index : partIndexes) {
            CHECK_TEST_TRUE(TPartMetaSaver(GetPartMetaPath(arcPrefix, index))->GetStage() == TPartMetaInfo::CLOSED);
        }
    }
    return true;
}

bool InitConfig() override {
    SetIndexerParams(DISK, 100, 1);
    SetMergerParams(true, 1, 1, mcpTIME, Max<i32>());
    (*ConfigDiff)["IndexGenerator"] = FULL_ARCHIVE_COMPONENT_NAME;
    (*ConfigDiff)["Components"] = FULL_ARCHIVE_COMPONENT_NAME;
    (*ConfigDiff)["Indexer.Memory.Enabled"] = "false";
    (*ConfigDiff)["Searcher.SnippetsDeniedZones"] = "";
    (*ConfigDiff)["Searcher.TwoStepQuery"] = "false";
    (*SPConfigDiff)["Service.MetaSearch.TwoStepQuery"] = "false";
    return true;
}
};
