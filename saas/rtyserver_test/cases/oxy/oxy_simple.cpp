#include "oxy.h"
#include <util/folder/filelist.h>
#include <saas/util/system/dir_digest.h>
#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/rtyserver/indexer_core/index_metadata_processor.h>
#include <saas/rtyserver_test/testerlib/search_checker.h>
#include <saas/rtyserver/config/const.h>
#include <saas/rtyserver/common/sharding.h>
#include <saas/rtyserver/components/fullarchive/globals.h>
#include <util/generic/algorithm.h>
#include <library/cpp/string_utils/url/url.h>
#include <util/system/fs.h>
#include <saas/rtyserver/indexer_core/index_dir.h>

START_TEST_DEFINE_PARENT(TestOxygenDocsSimple, TestOxygenDocs)
bool Run() override {
    if (GetIsPrefixed())
        return true;
    GenerateDocs("oxy/test_1000/docs", NRTYServer::TMessage::ADD_DOCUMENT, 1);
    IndexMessages(Messages, REALTIME, 1);
    TVector<TDocSearchInfo> results;
    QuerySearch("Fancy yourself a bit of an art connoisseur" + GetAllKps(Messages), results);
    if (results.size() != 1)
        ythrow yexception() << "invalid search results count " << results.size() << " != 1";
    ReopenIndexers();
    QuerySearch("Fancy yourself a bit of an art connoisseur" + GetAllKps(Messages), results);
    if (results.size() != 1)
        ythrow yexception() << "invalid search results count " << results.size() << " != 1";
    return true;
}
};

START_TEST_DEFINE_PARENT(TestOxygenDocsDetachAndConsumePrepIndex, TestOxygenDocs)
public:
    bool InitConfig() override {
        TestOxygenDocs::InitConfig();
        (*ConfigDiff)["SearchersCountLimit"] = "1";
        (*ConfigDiff)["Indexer.Disk.PreparatesMode"] = "true";
        (*ConfigDiff)["ComponentsConfig." FULL_ARCHIVE_COMPONENT_NAME ".ActiveLayers"] = "full,FOR_MERGE,COMPLEMENT";
        return true;
    }

    bool Run() override {
        if (GetIsPrefixed()) {
            return true;
        }
        GenerateDocs("docs", NRTYServer::TMessage::ADD_DOCUMENT, 1);
        IndexMessages(Messages, DISK, 1);
        ReopenIndexers();

        auto indexDirs = Controller->GetFinalIndexes(/*stopServer=*/false);
        CHECK_TEST_EQ(indexDirs.size(), 1);

        for (auto indexDir : indexDirs) {
            CHECK_TEST_TRUE(NRTYServer::HasIndexDirPrefix(indexDir, DIRPREFIX_PREP));
        }

        TString reply;
        NSaas::TShardsDispatcher::TContext urlShardingContext(NSaas::UrlHash);
        CHECK_TEST_TRUE(Controller->Detach(0, NSearchMapParser::SearchMapShards, urlShardingContext, reply));
        // Example: reply: rbtorrent:cb45f27e8b4ae3a9e6793e23037d50cbecc4ed4e

        Controller->StopBackends();
        NFs::RemoveRecursive(GetIndexDir());
        NFs::MakeDirectory(GetIndexDir());
        Controller->RestartServer(false, nullptr);

        TString syncResult;
        CHECK_TEST_TRUE(Controller->Synchronize(reply, NRTYServer::EConsumeMode::Replace, syncResult));

        TVector<TDocSearchInfo> results;
        QuerySearch("Fancy yourself a bit of an art connoisseur" + GetAllKps(Messages), results);
        CHECK_TEST_EQ(results.size(), 1);
        return true;
    }
};


START_TEST_DEFINE_PARENT(TestOxygenDocsCheckAndFix, TestOxygenDocs)
bool Run() override {
    if (GetIsPrefixed())
        return true;
    GenerateDocs("oxy/test_1000/docs", NRTYServer::TMessage::ADD_DOCUMENT, 1);
    IndexMessages(Messages, REALTIME, 1);
    ReopenIndexers();
    TVector<TDocSearchInfo> results;
    TSet<TString> indexes = Controller->GetFinalIndexes();
    Controller->ProcessCommand("stop");
    for (const auto& path : indexes) {
        TIndexMetadataProcessor(path)->MutableFullArcHeader()->SetStage(NRTYServer::TIndexMetadata::TFullArchiveHeader::STARTED);
        NFs::Remove(path + "/files_info");
    }
    Controller->RestartServer();
    Controller->WaitIsRepairing();
    QuerySearch("Fancy yourself a bit of an art connoisseur" + GetAllKps(Messages), results);
    if (results.size() != 1)
        ythrow yexception() << "invalid search results count " << results.size() << " != 1";

    return true;
}
};

START_TEST_DEFINE_PARENT(TestOxygenDocsDiskSearch, TestOxygenDocs)
bool Run() override {
    if (GetIsPrefixed())
        return true;
    const int CountMessages = 25000;
    DEBUG_LOG << "TestDiskIndexerSearch::Indexation::START" << Endl;
    THolder<TCallbackForCheckSearch> checker(new TCallbackForCheckSearch(this, 100));
    TIndexerClient::TContext context;
    context.Callback = checker.Get();
    checker->Start();
    DEBUG_LOG << "TestDiskIndexerSearch::Indexation::CHECKER_STARTED" << Endl;
    ui32 CountMessagesLoc = 1000;
    for (ui32 i = 0; i < CountMessages / CountMessagesLoc; ++i) {
        try {
            Messages.clear();
            GenerateDocs("oxy/25000", NRTYServer::TMessage::ADD_DOCUMENT, CountMessagesLoc, false, true, i * CountMessagesLoc);
            IndexMessages(Messages, REALTIME, context);
        } catch (...) {
            WARNING_LOG << CurrentExceptionMessage() << Endl;
        }
    }
    DEBUG_LOG << "TestDiskIndexerSearch::Indexation::FINISHED" << Endl;
    checker->Stop();
    DEBUG_LOG << "TestDiskIndexerSearch::Indexation::CHECKER_STOPPED" << Endl;

    bool result = checker->CheckAndPrintInfo(15, 0.99);
    CHECK_TEST_EQ(result, true);

    CHECK_TEST_NEQ(checker->GetDocsCount(), 0);

    return true;
}
bool InitConfig() override {
    if (!TestOxygenDocs::InitConfig())
        return false;
    SetIndexerParams(DISK, 20000, -1, 0);
    SetEnabledDiskSearch();
    (*ConfigDiff)["Indexer.Common.OxygenOptionsFile"] = GetResourcesDirectory() + "/oxy/configs/OxyOptionsWebby.cfg";
    (*ConfigDiff)["Indexer.Memory.Enabled"] = "false";
    (*ConfigDiff)["Searcher.EnableUrlHash"] = "true";
    (*ConfigDiff)["SearchersCountLimit"] = 1;

    SetMergerParams(true, 1, -1, mcpCONTINUOUS, 500000);
    return true;
}
};

START_TEST_DEFINE_PARENT(TestOxygenDocs1000, TestOxygenDocs)
bool Run() override {
    if (GetIsPrefixed())
        return true;
    GenerateDocs("oxy/test_1000/docs", NRTYServer::TMessage::ADD_DOCUMENT, 1000);
    try {
        IndexMessages(Messages, REALTIME, 1);
        TEST_FAILED("error wasn't detected");
    } catch (...) {
    }
    TVector<TDocSearchInfo> results;
    QuerySearch("Fancy yourself a bit of an art connoisseur&pron=st0" + GetAllKps(Messages), results);
    if (results.size() != 2) {
        TString report;
        for (ui32 i = 0; i < results.size(); ++i) {
            report += results[i].GetUrl() + ",";
        }
        ythrow yexception() << "invalid search results count " << results.size() << " != 2: " << report;
    }
    ReopenIndexers();
    QuerySearch("Fancy yourself a bit of an art connoisseur&pron=st0" + GetAllKps(Messages), results);
    if (results.size() != 2) {
        TString report;
        for (ui32 i = 0; i < results.size(); ++i) {
            report += results[i].GetUrl() + ",";
        }
        ythrow yexception() << "invalid search results count " << results.size() << " != 2: " << report;
    }
    return true;
}
};

START_TEST_DEFINE_PARENT(TestOxygenDocsCompare, TestOxygenDocs)
bool Run() override {
    if (GetIsPrefixed())
        return true;

    BuildOXYIndex();

    TFsPath(TmpDir + "/oxy_index/incremental.dat").DeleteIfExists();

    GenerateDocs("oxy/test_compare/docs", NRTYServer::TMessage::ADD_DOCUMENT, 10000, false, false);
    try {
        IndexMessages(Messages, REALTIME, 1, 0, true, true, TDuration(), TDuration(), 1, "tests", -1);
        TEST_FAILED("errors wasn't detected");
    } catch (...) {
    }
    TVector<TDocSearchInfo> results;
    ReopenIndexers();
    return CompareIndexes() && CompareIndexes(GetResourcesDirectory() + "/oxy/test_compare/index");
}
bool InitConfig() override {
    if (!TestOxygenDocs::InitConfig())
        return false;
    (*ConfigDiff)["Indexer.Common.OxygenOptionsFile"] = GetResourcesDirectory() + "/oxy/configs/OxygenOptionsGA2.cfg";
    SetIndexerParams(DISK, 5000, 1);
    return true;
}
};

START_TEST_DEFINE_PARENT(TestOxygenDocsCompareBackOrder, TestOxygenDocs)
bool Run() override {
    if (GetIsPrefixed())
        return true;

    BuildOXYIndex();
    CleanReorderSensitive();

    GenerateDocs("oxy/test_compare/docs", NRTYServer::TMessage::ADD_DOCUMENT, 10000, true, false);
    try {
        IndexMessages(Messages, REALTIME, 1, 0, true, true, TDuration(), TDuration(), 1, "tests", -1);
        TEST_FAILED("errors wasn't detected");
    } catch (...) {
    }
    TVector<TDocSearchInfo> results;
    ReopenIndexers();

    return CompareIndexes() && CompareIndexes(GetResourcesDirectory() + "/oxy/test_compare/index_reorder_invariant");
}
bool InitConfig() override {
    if (!TestOxygenDocs::InitConfig())
        return false;
    (*ConfigDiff)["Indexer.Common.OxygenOptionsFile"] = GetResourcesDirectory() + "/oxy/configs/OxygenOptionsGA2.cfg";
    SetIndexerParams(DISK, 5000, 1);
    return true;
}
};

START_TEST_DEFINE_PARENT(TestOxygenDocsCompareReorder, TestOxygenDocs)
bool Run() override {
    if (GetIsPrefixed())
        return true;
    BuildOXYIndex();
    CleanReorderSensitive();

    GenerateDocs("oxy/test_compare/docs", NRTYServer::TMessage::ADD_DOCUMENT, 10000, false, false);
    try {
        IndexMessages(Messages, REALTIME, 1);
        TEST_FAILED("errors wasn't detected");
    } catch (...) {
    }
    ReopenIndexers();

    return CompareIndexes() && CompareIndexes(GetResourcesDirectory() + "/oxy/test_compare/index_reorder_invariant");
}
bool InitConfig() override {
    if (!TestOxygenDocs::InitConfig())
        return false;
    (*ConfigDiff)["Indexer.Common.OxygenOptionsFile"] = GetResourcesDirectory() + "/oxy/configs/OxygenOptionsGA2.cfg";
    SetIndexerParams(DISK, 5000, 1);
    return true;
}
};

START_TEST_DEFINE_PARENT(TestOxygenBrokenFullarcRepair, TestOxygenDocs)
bool Prepare() override {
    if (GetIsPrefixed())
        return true;
    PrepareData("oxy/broken_fullarc/source");
    return true;
}
bool Run() override {
    if (GetIsPrefixed())
        return true;
    Controller->ProcessCommand("create_merger_tasks");
    Controller->ProcessCommand("do_all_merger_tasks");
    CHECK_TEST_TRUE(QueryCount() > 0);
    return true;
}
bool InitConfig() override {
    if (!TestOxygenDocs::InitConfig())
        return false;
    (*ConfigDiff)["ComponentsConfig." FULL_ARCHIVE_COMPONENT_NAME ".ActiveLayers"] = NRTYServer::NFullArchive::FullLayer;
    (*ConfigDiff)["ComponentsConfig." OXY_COMPONENT_NAME ".ArchiveLayersFilteredForMerge"] = "";
    (*ConfigDiff)["ComponentsConfig." OXY_COMPONENT_NAME ".ArchiveLayersFilteredForIndex"] = NRTYServer::NFullArchive::FullLayer;
    (*ConfigDiff)["ComponentsConfig." OXY_COMPONENT_NAME ".ArchiveLayersMergeComplement"] = "";
    (*ConfigDiff)["Indexer.Common.OxygenOptionsFile"] = GetResourcesDirectory() + "/oxy/broken_fullarc/OxygenOptions.cfg";
    SetIndexerParams(DISK, 1000, 1);
    SetMergerParams(true, 1, -1, mcpNONE, 50);
    (*ConfigDiff)["Merger.ClearRemoved"] = "0";
    return true;
}
};

START_TEST_DEFINE_PARENT(TestOxygenDocsMerge1, TestOxygenDocs)
bool Run() override {
    if (GetIsPrefixed())
        return true;
    GenerateDocs("oxy/test_compare/docs", NRTYServer::TMessage::ADD_DOCUMENT, 500);
    IndexMessages(Messages, REALTIME, 1);
    TVector<TDocSearchInfo> results;
    ReopenIndexers();
    Controller->ProcessCommand("create_merger_tasks");
    Controller->ProcessCommand("do_all_merger_tasks");
    CHECK_TEST_TRUE(CheckLayersAndCount(500));

    return true;
}
bool InitConfig() override {
    if (!TestOxygenDocs::InitConfig())
        return false;
    (*ConfigDiff)["Indexer.Common.OxygenOptionsFile"] = GetResourcesDirectory() + "/oxy/configs/OxygenOptionsGA2.cfg";
    SetIndexerParams(DISK, 100, 1);
    SetMergerParams(true, 1, -1, mcpNONE, 50);
    (*ConfigDiff)["Merger.ClearRemoved"] = "0";
    return true;
}
};

START_TEST_DEFINE_PARENT(TestOxygenDocsMerge1Deadline10, TestOxygenDocs)
bool Run() override {
    if (GetIsPrefixed())
        return true;
    GenerateDocs("oxy/test_compare/docs", NRTYServer::TMessage::ADD_DOCUMENT, 50);
    IndexMessages(Messages, REALTIME, 1);
    TVector<TDocSearchInfo> results;
    ReopenIndexers();
    Controller->ProcessCommand("create_merger_tasks");
    Controller->ProcessCommand("do_all_merger_tasks");
    try {
        TJsonPtr serverInfo(Controller->GetServerInfo());
        NJson::TJsonValue& info = (*serverInfo)[0];
        DEBUG_LOG << info.GetStringRobust() << Endl << info["indexes"]["index_0000000000_0000000007"]["count"].GetUInteger() << Endl;
        auto map = info["indexes"].GetMap();
        ui32 compRes = 0;
        for (auto& i : map) {
            compRes += i.second["count"].GetUInteger();
        }
        CHECK_TEST_EQ(compRes, 15);
    } catch (...) {
        ERROR_LOG << CurrentExceptionMessage() << Endl;
    }

    QuerySearch("url:\"*\"&numdoc=100", results, nullptr, nullptr, true);

    const TString requiredUrls[] = {
        "bdklaboratory.lv/index.php/ru/erotichesky-gelj.html",
        "yringlet.3dn.ru/news/zhenskaja_ehnergetika_zhizn_v_potoke_tvorenija/2014-07-01-54",
        "iprim.ru/news/12043/3269",
        "my.mail.ru/mail/gg.komar/",
        "my.mail.ru/mail/janik_8909",
        "my.mail.ru/mail/maykova_81/",
        "my.mail.ru/mail/natali.bezhanogly/",
        "my.mail.ru/mail/savina.m/",
        "myzuka.ru/Song/2813623/%D0%93%D1%80-%D0%A0%D0%BE%D0%B6%D0%B4%D0%B5%D1%81%D1%82%D0%B2%D0%BE-%D0%92%D0%BE%D1%82-%D0%A2%D0%B0%D0%BA",
        "www.hp-c.name.ru/services/office/",
        "www.pzemi.ru/sitemap.htm",
        "www.regnum.ru/news/medicine/1814759.html",
        "www.tobolsk-eparhia.ru/arh_list.php?start=-22799&width=100",
        "udetstvo.ru/onas1"
    };

    for (auto&& url: requiredUrls) {
        if (FindIf(results.begin(), results.end(), [&](const TDocSearchInfo& info) -> bool {
            return CutHttpPrefix(info.GetUrl()) == CutHttpPrefix(url);
        }) == results.end())
        {
            throw yexception() << url << " is missing";
        }
    }

    return true;
}
bool InitConfig() override {
    if (!TestOxygenDocs::InitConfig())
        return false;
    (*ConfigDiff)["Indexer.Common.OxygenOptionsFile"] = GetResourcesDirectory() + "/oxy/configs/OxygenOptionsGA2.cfg";
    SetIndexerParams(DISK, 10, 1);
    SetMergerParams(true, 1, -1, mcpNONE, 50);
    (*ConfigDiff)["Merger.ClearRemoved"] = "0";
    (*ConfigDiff)["Merger.MaxDeadlineDocs"] = "15";
    return true;
}
};

START_TEST_DEFINE_PARENT(TestOxygenDocsCompare1, TestOxygenDocs)
bool Run() override {
    if (GetIsPrefixed())
        return true;
    BuildOXYIndex(0, 1);
    GenerateDocs("oxy/test_compare/docs", NRTYServer::TMessage::ADD_DOCUMENT, 1);
    IndexMessages(Messages, REALTIME, 1);
    TVector<TDocSearchInfo> results;
    ReopenIndexers();
    return CompareIndexes() && CompareIndexes(GetResourcesDirectory() + "/oxy/test_1/index");
}
bool InitConfig() override {
    if (!TestOxygenDocs::InitConfig())
        return false;
    (*ConfigDiff)["Indexer.Common.OxygenOptionsFile"] = GetResourcesDirectory() + "/oxy/configs/OxygenOptionsGA2.cfg";
    SetIndexerParams(DISK, 5000, 1);
    return true;
}
};

START_TEST_DEFINE_PARENT(TestOxygenIncorrectDocIndexFAIL, TestOxygenDocs)
bool Run() override {
    if (GetIsPrefixed())
        return true;
    Messages.clear();
    GenerateDocs("oxy/test_compare/docs", NRTYServer::TMessage::MODIFY_DOCUMENT, 1, false, true, 3066);

    try {
        IndexMessages(Messages, REALTIME, 1);
        TEST_FAILED("Incorrect errors processing");
    } catch (...) {
    }

    TVector<TDocSearchInfo> results;
    QuerySearch("url:\"*\"", results);
    CHECK_TEST_EQ(results.size(), 0);
    return true;
}
bool InitConfig() override {
    if (!TestOxygenDocs::InitConfig())
        return false;
    (*ConfigDiff)["Indexer.Common.UseSlowUpdate"] = 1;
    (*ConfigDiff)["Indexer.Common.OxygenOptionsFile"] = GetResourcesDirectory() + "/oxy/configs/OxygenOptionsGA2.cfg";
    SetIndexerParams(DISK, 5000, 1);
    return true;
}
};

START_TEST_DEFINE_PARENT(TestOxygenDocsMany, TestOxygenDocs)
bool Run() override {
    if (GetIsPrefixed())
        return true;
    GenerateDocs("oxy/test_1000/docs", NRTYServer::TMessage::ADD_DOCUMENT, 10);
    IndexMessages(Messages, REALTIME, 1);
    TVector<TDocSearchInfo> results;
    for (ui32 i = 0; i < 10; ++i) {
        QuerySearch("Fancy yourself a bit of an art connoisseur" + GetAllKps(Messages), results);
        if (results.size() == 1)
            break;
        if (i == 9)
            ythrow yexception() << "invalid search results count " << results.size() << " != 1";
    }
    ReopenIndexers();
    QuerySearch("Fancy yourself a bit of an art connoisseur" + GetAllKps(Messages), results);
    if (results.size() != 1)
        ythrow yexception() << "invalid search results count " << results.size() << " != 1";
    return true;
}

bool InitConfig() override {
    if (!TestOxygenDocs::InitConfig())
        return false;
    SetIndexerParams(DISK, 5, 1);
    return true;
}
};

START_TEST_DEFINE_PARENT(TestOxygenDocsMerge, TestOxygenDocs)
bool Run() override {
    if (GetIsPrefixed())
        return true;
    GenerateDocs("oxy/test_1000/docs", NRTYServer::TMessage::ADD_DOCUMENT, 20);
    TVector<NRTYServer::TMessage> mess1(Messages.begin(), Messages.begin() + 10);
    TVector<NRTYServer::TMessage> mess2(Messages.begin() + 10, Messages.begin() + 20);
    IndexMessages(mess1, REALTIME, 1);
    ReopenIndexers();
    TVector<TDocSearchInfo> results;
    QuerySearch("url:\"*\"&numdoc=100" + GetAllKps(Messages), results);
    if (results.size() != 10)
        ythrow yexception() << "invalid search results count " << results.size() << " != 1";
    IndexMessages(mess2, REALTIME, 1);
    QuerySearch("url:\"*\"&numdoc=100" + GetAllKps(Messages), results);
    if (results.size() != 20)
        ythrow yexception() << "invalid search results count " << results.size() << " != 1";
    ReopenIndexers();
    Controller->ProcessCommand("create_merger_tasks");
    Controller->ProcessCommand("do_all_merger_tasks");
    QuerySearch("url:\"*\"&numdoc=100" + GetAllKps(Messages), results);
    CHECK_TEST_EQ(results.size(), 20);

    CheckMergerResult();
    return true;
}

bool InitConfig() override {
    if (!TestOxygenDocs::InitConfig())
        return false;
    SetMergerParams(true, 1, -1, mcpNONE, 50);
    SetIndexerParams(DISK, 50, 1);
    return true;
}
};

START_TEST_DEFINE_PARENT(TestOxygenDocsMergeWhileIndexingModify, TestOxygenDocs, TTestMarksPool::OneBackendOnly)
bool Run() override {
    if (GetIsPrefixed())
        return true;
    const int messagesPerIndex = 500;
    ui32 maxSegments = GetMergerMaxSegments();
    VERIFY_WITH_LOG(maxSegments > 1, "at least two segments required for test");
    VERIFY_WITH_LOG(!IsMergerTimeCheck(), "timed check must be off for test");
    TVector<NRTYServer::TMessage> additionalMessages;
    GenerateDocs("oxy/test_compare/docs", NRTYServer::TMessage::ADD_DOCUMENT, messagesPerIndex * (maxSegments + 1));
    additionalMessages = Messages;
    for (TVector<NRTYServer::TMessage>::iterator i = additionalMessages.begin(); i != additionalMessages.end(); ++i) {
        i->SetMessageType(NRTYServer::TMessage::MODIFY_DOCUMENT);
    }

    for (unsigned i = 0; i < maxSegments + 1; ++i) {
        IndexMessages(TVector<NRTYServer::TMessage>(Messages.begin() + i * messagesPerIndex, Messages.begin() + (i + 1) * messagesPerIndex), DISK, 1);
        ReopenIndexers();
    }

    TVector<TDocSearchInfo> results;
    QuerySearch("url:\"*\"&numdoc=10000&pron=fastcount10000&relev=attr_limit=10000" + GetAllKps(Messages), results);
    size_t c = results.size();

    Controller->ProcessCommand("create_merger_tasks");
    Controller->ProcessCommand("do_all_merger_tasks&wait=false");
    IndexMessages(additionalMessages, DISK, 1);
    Controller->ProcessCommand("do_all_merger_tasks");
    ReopenIndexers();
    QuerySearch("url:\"*\"&numdoc=10000&pron=fastcount10000&relev=attr_limit=10000" + GetAllKps(Messages), results);
    if (results.size() != c)
        ythrow yexception() << "invalid docs count " << results.size() << " != " << c;
    return true;
}

public:
    bool InitConfig() override {
        if (!TestOxygenDocs::InitConfig())
            return false;
        (*ConfigDiff)["Indexer.Common.OxygenOptionsFile"] = GetResourcesDirectory() + "/oxy/configs/OxygenOptionsGA2.cfg";
        SetIndexerParams(DISK, 2000, 1);
        SetIndexerParams(REALTIME, 2000);
        SetMergerParams(true, 2, -1, mcpNONE);
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestOxygenFakeDoc, TestOxygenDocs)
void IndexFromTextProtobufFile(const TString& path) {
    TIFStream inputStream(path);
    const TString& data = inputStream.ReadAll();

    NRTYServer::TMessage message;
    if (!google::protobuf::TextFormat::ParseFromString(data, &message)) {
        ythrow yexception() << "cannot parse protobuf file " << path;
    }

    message.SetMessageId(1);
    IndexMessages({ message }, REALTIME, 1);
}
bool Run() override {
    if (GetIsPrefixed())
        return true;
    IndexFromTextProtobufFile(GetResourcesDirectory() + "/kiwi_test/oxy_fake_message.0");
    TVector<TDocSearchInfo> results;
    QuerySearch("url:\"*\"", results);
    if (results.size() != 1) {
        ythrow yexception() << "invalid search results count " << results.size() << " != 1";
    }
    ReopenIndexers();
    QuerySearch("url:\"*\"", results);
    if (results.size() != 1) {
        ythrow yexception() << "invalid search results count " << results.size() << " != 1";
    }
    return true;
}
bool InitConfig() override {
    if (!TestOxygenDocs::InitConfig())
        return false;
    (*ConfigDiff)["Indexer.Common.OxygenOptionsFile"] = GetResourcesDirectory() + "/oxy/configs/OxygenOptions_fakes.cfg";
    return true;
}
};


START_TEST_DEFINE_PARENT(TestOxygenMultizoneSimple, TestOxygenDocs)
bool Run() override {
    if (GetIsPrefixed())
        return true;

    if (!TmpDir)
        TmpDir = TFsPath(GetIndexDir()).Parent().GetPath(); // instead of BuildOXYIndex()

    GenerateDocs("oxy/test_multizone/docs", NRTYServer::TMessage::ADD_DOCUMENT, 60);
    CHECK_TEST_TRUE(Messages.size());

    IndexMessages(Messages, DISK, 1);

    ReopenIndexers();
    CleanReorderSensitive();

    return CompareIndexes(GetResourcesDirectory() + "/oxy/test_multizone/index");
}



bool InitConfig() override {
    if (!TestOxygenDocs::InitConfig())
        return false;

    (*ConfigDiff)["Indexer.Common.OxygenOptionsFile"] = GetResourcesDirectory() + "/oxy/configs/OxygenOptionsMixer.cfg";
    SetIndexerParams(DISK, 100, 1);

    return true;
}
};

