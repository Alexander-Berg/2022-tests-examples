#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/rtyserver_test/util/doc_info.h>
#include <saas/rtyserver_test/util/tass_parsers.h>
#include <saas/rtyserver/components/fullarchive/disk_manager.h>
#include <saas/rtyserver/components/fullarchive/globals.h>
#include <saas/rtyserver/components/erf/erf_disk.h>
#include <saas/rtyserver/components/ddk/ddk_fields.h>
#include <saas/rtyserver/indexer_core/index_metadata_processor.h>
#include <saas/util/json/json.h>

#include <kernel/index_mapping/index_mapping.h>

#include <util/random/random.h>
#include <util/system/shellcommand.h>


SERVICE_TEST_RTYSERVER_DEFINE(TestFullArchive)
    void PrepareDocs(TVector<TBuffer>& docs) {
        TFsPath path(GetResourcesDirectory() + "/full_arc/performance");
        TVector<TString> files;
        path.ListNames(files);
        for (ui32 i = 0; i < 1000; ++i) {
            TFileInput fi(path / (ToString(i) + ".dump"));
            TString data = fi.ReadAll();
            docs.push_back(TBuffer(data.data(), data.size()));
        }
    }
};

START_TEST_DEFINE_PARENT(TestFullArchivePerfomance, TestFullArchive)
    bool Run() override {
        TVector<TBuffer> docs;
        PrepareDocs(docs);
        TRTYServerConfig config(MakeAtomicShared<TDaemonConfig>(TDaemonConfig::DefaultEmptyConfig.data(), false));
        const ui32 repeat = 1000000 / docs.size();
        TString dir = GetIndexDir();
        ui64 docsCount = repeat * docs.size();
        TDiskFAManager manager(dir, docsCount, config, 0, {NRTYServer::NFullArchive::FullLayer}, false, false, false);
//        TDiskFAManager manager(dir, docsCount, config, 30 * (1 << 20), true);
        manager.Open();
        INFO_LOG << "Start Wirte " << docsCount << " docs" << Endl;
        TInstant start = Now();
        docsCount = 0;
        ui64 fileSize = 0;
        for (ui32 r = 0; r < repeat; ++r)
            for (const auto& doc : docs) {
                manager.IndexUnsafe(TBlob::NoCopy(doc.data(),doc.size()), docsCount++);
                fileSize += doc.size();
            }
        manager.Close();
        TDuration work = Now() - start;
        INFO_LOG << "Write " << docsCount << ": " << (1000000 * docsCount / work.MicroSeconds()) << " docs/s; " << (1000000 * fileSize / work.MicroSeconds()) << " B/s; FileSize: " << fileSize << Endl;
        manager.Open();
        INFO_LOG << "Start Iterate with unzip " << docsCount << " docs" << Endl;
        start = Now();
        for (auto i = manager.CreateIterator(); i->IsValid(); i->Next())
            i->GetDocBlob();
        work = Now() - start;
        INFO_LOG << "Iterate with unzip: " << (1000000 * docsCount / work.MicroSeconds()) << " docs/s; " << (1000000 * fileSize / work.MicroSeconds()) << " B/s" << Endl;
        INFO_LOG << "Start Random read " << docsCount << " docs" << Endl;
        start = Now();
        ui64 docsSize = 0;
        docsCount = 10000;
        for (ui32 i = 0; i < docsCount; ++i) {
            NRTYServer::TParsedDoc pd;
            ui32 docid = RandomNumber(docsCount - 1);
            ui32 size;
            manager.ReadParsedDoc(NRTYServer::NFullArchive::FullLayer, pd, docid, &size);
            docsSize += size;
        }
        work = Now() - start;
        INFO_LOG << "Random read " << docsCount << " docs: " << (1000000 * docsCount / work.MicroSeconds()) << " docs/s; " << (1000000 * docsSize / work.MicroSeconds()) << " B/s" << Endl;
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestFullArchiveSimple, TestFullArchive)
    THolder<TDiskFAManager> Manager;
    TVector<TBuffer> Docs;
    ui32 DocsCount;
    void CheckDocs(const char* comment) {
        ui32 readedDocs = 0;
        for (auto iter = Manager->CreateIterator(); iter->IsValid(); iter->Next()) {
            ui32 docid = readedDocs;
            if (++readedDocs > DocsCount)
                ythrow yexception() << comment << ": readed " << readedDocs << " docs, but wrote only " << DocsCount;
            if (iter->GetDocId() != docid)
                ythrow yexception() << comment << ": wrong docid: " << iter->GetDocId() << " != " << docid;
            TBlob blob = iter->GetDocBlob();
            if (blob.Size() != Docs[docid].Size() || memcmp(blob.AsCharPtr(), Docs[docid].data(), blob.Size()))
                ythrow yexception() << comment << ": wrong doc readed docid=" << docid;
        }
        if (readedDocs < DocsCount)
            ythrow yexception() << comment << ": readed only " << readedDocs << " docs, but wrote" << DocsCount;
    }

    bool Run() override {
        ThreadDisableBalloc();
        PrepareDocs(Docs);
        TRTYServerConfig config(MakeAtomicShared<TDaemonConfig>(TDaemonConfig::DefaultEmptyConfig.data(), false));
        DocsCount = 0;
        size_t size = 0;
        size_t ZIP_BUFFER_SIZE = 1 << 15;
        for (; size < ZIP_BUFFER_SIZE; size += Docs[DocsCount++].Size()) {
        }
        while (Docs[DocsCount++].Size() > ZIP_BUFFER_SIZE) {
        }
        DEBUG_LOG << "DocsCount: " << DocsCount << Endl;

        TString dir = GetIndexDir();
        Manager.Reset(new TDiskFAManager(dir, DocsCount, config, 0, {NRTYServer::NFullArchive::FullLayer}, false, false, false));
        Manager->Open();
        ui32 version = TIndexMetadataProcessor(dir)->GetFullArcHeader().GetVersion();
        if (version != FULL_ARC_VERSION)
            ythrow yexception() << "invalid version in " << dir << ", " << version << " != " << FULL_ARC_VERSION;
        for (ui32 i = 0; i < DocsCount; ++i)
            Manager->IndexUnsafe(TBlob::NoCopy(Docs[i].data(), Docs[i].size()), i);
        CheckDocs("Before reopen");
        Manager->Close();
        version = TIndexMetadataProcessor(dir)->GetFullArcHeader().GetVersion();
        if (version != FULL_ARC_VERSION)
            ythrow yexception() << "invalid version in " << dir << ", " << version << " != " << FULL_ARC_VERSION;
        Manager->Open();
        version = TIndexMetadataProcessor(dir)->GetFullArcHeader().GetVersion();
        if (version != FULL_ARC_VERSION)
            ythrow yexception() << "invalid version in " << dir << ", " << version << " != " << FULL_ARC_VERSION;
        CheckDocs("After reopen");
        return true;
    }
};


START_TEST_DEFINE(TestDDKCollisionNormalize, TTestMarksPool::OneBackendOnly)
bool Test(ui32 docsCount, const TString& comment) {
    ThreadDisableBalloc();
    INFO_LOG << "Test " << comment << "..." << Endl;
    TVector<NRTYServer::TMessage> messages;
    ui32 kps = GetIsPrefixed();
    GenerateInput(messages, docsCount, NRTYServer::TMessage::ADD_DOCUMENT, kps);
    if (kps)
        for (auto& msg : messages)
            msg.MutableDocument()->SetKeyPrefix(kps);
    IndexMessages(messages, DISK, 1);
    ReopenIndexers();
    TSet<TString> indexes = Controller->GetFinalIndexes();
    Controller->ProcessCommand("stop");
    ui32 maxVersion = 0;
    ui32 maxDocid = 0;
    TString cfgPath = Controller->GetConfigValue("Controller.ConfigsRoot", "DaemonConfig", TBackendProxy::TBackendSet(0)) + "/rtyserver.conf";
    TString cfgText = TUnbufferedFileInput(cfgPath).ReadAll();

    TRTYServerConfig config(TServerConfigConstructorParams(cfgText.data()));
    for (const auto& path : indexes) {
        TFsPath final(path);
        TDiskFAManager fullArc(path, 0, config, 0, {
            NRTYServer::NFullArchive::BaseLayer,
            NRTYServer::NFullArchive::FullLayer
        }, false, false, false);
        fullArc.Open();
        TRTYErfDiskManager::TCreationContext cc(TPathName{path}, "indexddk.rty", &GetDDKFields());
        cc.BlockCount = docsCount;
        TRTYErfDiskManager erf(cc, DDK_COMPONENT_NAME);
        erf.Open();
        TDocSearchInfo dsi("url1", kps);
        for (ui32 docid = 0; docid < docsCount; ++docid) {
            TFactorStorage storage(NRTYServer::NDDK::KeysCount);
            TGeneralResizeableFactorStorage::FloatClear(storage.factors, storage.factors + NRTYServer::NDDK::KeysCount);
            NRTYServer::TParsedDoc pd;
            if (!fullArc.ReadParsedDoc(NRTYServer::NFullArchive::FullLayer, pd, docid))
                ythrow yexception() << "cannot read doc " << docid;
            pd.MutableDocument()->SetUrl(dsi.GetUrl());
            pd.MutableDocument()->SetKeyPrefix(dsi.GetKeyPrefix());
            ui32 version = abs((int)(docsCount - 2 * docid));
            if (version > maxVersion) {
                maxVersion = version;
                maxDocid = docid;
            }
            pd.MutableDocument()->SetVersion(version);
            pd.MutableDocument()->SetBody(ToString(version));
            (ui32&)storage[NRTYServer::NDDK::VersionIndex] = version;
            SetHash(storage, dsi);
            fullArc.IndexUnsafe(pd, docid);
            erf.Write(storage, docid);
        }
        NFs::Remove(TFsPath(path) / "files_info");
    }
    Controller->RestartServer();
    TVector<TDocSearchInfo> results;
    for (ui32 i = 0; i < 5; ++i) {
        QuerySearch("body&kps=" + ToString(kps), results);
        if (results.size() == 1)
            break;
        sleep(1);
    }
    PrintInfoServer();
    CHECK_TEST_EQ(results.size(), 1);
    CHECK_TEST_EQ(results[0].GetDocId(), maxDocid);
    return true;
}

bool Run() override {
    CHECK_TEST_EQ(Test(10, "repair by data"), true);
    return true;
}

bool InitConfig() override {
    SetIndexerParams(DISK, 20, 1);
    SetIndexerParams(REALTIME, 20);
    SetEnabledRepair();
    return true;
}
};

START_TEST_DEFINE(TestFullArchiveGenerate, TTestMarksPool::OneBackendOnly)
bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 10, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    for (ui32 i = 0; i < messages.size(); ++i) {
        messages[i].MutableDocument()->SetKeyPrefix(GetIsPrefixed() ? 2 : 0);
        messages[i].MutableDocument()->SetBody(ToString(i));
    }
    IndexMessages(messages, DISK, 1);
    ReopenIndexers();

    TString keys;
    for (ui32 i = 0; i < messages.size(); ++i) {
        if (i)
            keys += ",";
        keys += messages[i].GetDocument().GetUrl();
        TVector<TDocSearchInfo> results;
        TQuerySearchContext::TDocProperties docProperties;
        TQuerySearchContext ctx;
        {
            ctx.DocProperties = &docProperties;
            QuerySearch(messages[i].GetDocument().GetUrl() + "&sgkps=" + ToString(messages[i].GetDocument().GetKeyPrefix()), results, ctx);
            CHECK_TEST_EQ(results.size(), 1);
            CHECK_TEST_NEQ(docProperties.size(), 0);
            TString data(Base64Decode(docProperties[0]->find("data")->second));
            NRTYServer::TMessage::TDocument doc;
            Y_PROTOBUF_SUPPRESS_NODISCARD doc.ParseFromString(data);
            CHECK_TEST_EQ(doc.GetBody(), messages[i].GetDocument().GetBody());
        }

        if (i) {
            QuerySearch(messages[i].GetDocument().GetUrl() + "," + messages[i - 1].GetDocument().GetUrl() + "&sgkps=" + ToString(messages[i].GetDocument().GetKeyPrefix()), results, ctx);
            CHECK_TEST_EQ(results.size(), 2);
            CHECK_TEST_NEQ(docProperties.size(), 0);
            TString data(Base64Decode(docProperties[0]->find("data")->second));
            NRTYServer::TMessage::TDocument doc;
            Y_PROTOBUF_SUPPRESS_NODISCARD doc.ParseFromString(data);
            TString body = StripString(doc.GetBody());
            TString body0 = StripString(messages[i].GetDocument().GetBody());
            TString body1 = StripString(messages[i - 1].GetDocument().GetBody());
            INFO_LOG << body << " / " << body0 << " / " << body1 << Endl;
            CHECK_TEST_TRUE((body == body0) || (body == body1));
        }
    }
    TVector<TDocSearchInfo> results;
    TQuerySearchContext ctx;
    ctx.PrintResult = true;
    NOT_EXCEPT(QuerySearch(keys + "&sgkps=" + ToString(messages[0].GetDocument().GetKeyPrefix()), results, ctx));
    CHECK_TEST_EQ(results.size(), messages.size());
    if (Controller->GetActiveBackends().size() == 1) {
        CHECK_TEST_EQ(GetSearchableDocsCount(Controller), 10);
    }

    if (Cluster->GetNodesNames(TNODE_SEARCHPROXY).size()) {
        ctx.SourceSelector = new TLevelSourceSelector(2);
    } else {
        ctx.SourceSelector = new TDirectSourceSelector();
    }

    NOT_EXCEPT(QuerySearch(keys + "&sp_meta_search=multi_proxy&meta_search=first_found&sgkps=" + ToString(messages[0].GetDocument().GetKeyPrefix()), results, ctx));
    INFO_LOG << results.size() << Endl;
    CHECK_TEST_EQ(results.size(), messages.size());

    NOT_EXCEPT(QuerySearch(keys + "&sp_meta_search=meta&meta_search=first_found&sgkps=" + ToString(messages[0].GetDocument().GetKeyPrefix()), results, ctx));
    CHECK_TEST_EQ(results.size(), messages.size());
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

struct TSourceSelectorStub : ISearchSourceSelector {
    bool DoCheck(const TVector<TString>& /*splitKey*/, const TString& /*query*/, ui32 /*requiredMetaSearchLevel*/) override {
        return true;
    }
};

START_TEST_DEFINE(TestFullArchiveNoTextSplit, TTestMarksPool::OneBackendOnly)
bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 10, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    for (ui32 i = 0; i < messages.size(); ++i) {
        ui64 prefix = !GetIsPrefixed() ? 0 : 2 - i % 2;
        messages[i].MutableDocument()->SetUrl("key," + ToString(i));
        messages[i].MutableDocument()->SetKeyPrefix(prefix);
        messages[i].MutableDocument()->SetBody(ToString(i));
    }
    IndexMessages(messages, DISK, 1);
    ReopenIndexers();

    Y_ENSURE(!messages.empty());
    TString query;
    {
        const TStringBuf sgkps = GetIsPrefixed() ? "1,2" : "0";
        TStringOutput queryBuilder(query);
        for (ui32 i = 0; i < messages.size(); ++i) {
            queryBuilder << "&text=" << messages[i].GetDocument().GetUrl();
        }
        queryBuilder << "&sgkps=" << sgkps << "&saas_no_text_split=&sp_meta_search=multi_proxy&meta_search=first_found";
    }
    TVector<TDocSearchInfo> results;
    TQuerySearchContext ctx;
    ctx.SourceSelector = new TSourceSelectorStub;
    NOT_EXCEPT(Controller->QuerySearch(query, results, GetIsPrefixed(), ctx));
    CHECK_TEST_EQ(results.size(), messages.size());
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

START_TEST_DEFINE(TestFullArchiveEmptyText, TTestMarksPool::OneBackendOnly)
bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 10, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    for (ui32 i = 0; i < messages.size(); ++i) {
        ui64 prefix = !GetIsPrefixed() ? 0 : 2 - i % 2;
        messages[i].MutableDocument()->SetUrl("key," + ToString(i));
        messages[i].MutableDocument()->SetKeyPrefix(prefix);
        messages[i].MutableDocument()->SetBody(ToString(i));
    }
    IndexMessages(messages, DISK, 1);
    ReopenIndexers();

    Y_ENSURE(!messages.empty());
    TVector<TString> queries = {
        "&sp_meta_search=multi_proxy&meta_search=first_found&text=",
        "&sp_meta_search=multi_proxy&meta_search=first_found&text=,",
        "&sp_meta_search=multi_proxy&meta_search=first_found&text=&key_name=key_name",
        "&saas_no_text_split=1&sp_meta_search=multi_proxy&meta_search=first_found&text=",
        "&saas_no_text_split=1&sp_meta_search=multi_proxy&meta_search=first_found&text=&text=",
        "&saas_no_text_split=1&sp_meta_search=multi_proxy&meta_search=first_found&text=&key_name=key_name"
    };
    TVector<TDocSearchInfo> results;
    TQuerySearchContext ctx;
    ctx.SourceSelector = new TSourceSelectorStub;

    for (auto& query : queries) {
        NOT_EXCEPT(Controller->QuerySearch(query, results, GetIsPrefixed(), ctx));
        CHECK_TEST_EQ(results.size(), 0);
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

START_TEST_DEFINE(TestFullArchiveMultiKpsSearch, TTestMarksPool::OneBackendOnly)
bool Run() override {
    if (!GetIsPrefixed()) {
        INFO_LOG << "Only prefix case have sense" << Endl;
        return false;
    }
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 10, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    TString keys;
    for (ui32 i = 0; i < messages.size(); ++i) {
        if (i)
            keys += ",";
        keys += messages[i].GetDocument().GetUrl();
        messages[i].MutableDocument()->SetBody(ToString(i));
    }
    IndexMessages(messages, DISK, 1);
    ReopenIndexers();
    const TString sgkps = GetAllKps(messages, "&sgkps=");
    {
        TVector<TDocSearchInfo> results;
        TQuerySearchContext ctx;
        ctx.PrintResult = true;
        QuerySearch(keys + sgkps, results, ctx);
        INFO_LOG << results.size() << Endl;
        CHECK_TEST_EQ(results.size(), messages.size());
    }
    {
        TVector<TDocSearchInfo> results;
        TQuerySearchContext ctx;
        ctx.PrintResult = true;
        if (Cluster->GetNodesNames(TNODE_SEARCHPROXY).size()) {
            ctx.SourceSelector = new TLevelSourceSelector(2);
        } else {
            ctx.SourceSelector = new TDirectSourceSelector();
        }
        NOT_EXCEPT(QuerySearch(keys + "&sp_meta_search=meta&meta_search=first_found" + sgkps, results, ctx));
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

START_TEST_DEFINE(TestFullArchiveDifferentKeyTypes, TTestMarksPool::OneBackendOnly)
bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    TVector<NRTYServer::TMessage> messages1;
    TVector<NRTYServer::TMessage> messages2;
    GenerateInput(messages, 10, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    TString keys;
    ui32 kps = GetIsPrefixed() ? 1 : 0;
    for (ui32 i = 0; i < messages.size(); ++i) {
        messages[i].MutableDocument()->SetBody(ToString(i));
        messages[i].MutableDocument()->SetKeyPrefix(kps);
    }
    IndexMessages(messages, REALTIME, 1);

    TQuerySearchContext ctx;
    TVector<TDocSearchInfo> results;
    ctx.PrintResult = true;
    ctx.ResultCountRequirement = 10;
    ctx.AttemptionsCount = 5;

    QuerySearch("value1&key_name=key1&sgkps=" + ToString(kps), results, ctx);
    CHECK_TEST_EQ(results.size(), 10);

    QuerySearch("value2&key_name=key2&sgkps=" + ToString(kps), results, ctx);
    CHECK_TEST_EQ(results.size(), 10);

    QuerySearch("value3&key_name=key1&sgkps=" + ToString(kps), results, ctx);
    CHECK_TEST_EQ(results.size(), 10);

    QuerySearch("value3&key_name=key2&sgkps=" + ToString(kps), results, ctx);
    CHECK_TEST_EQ(results.size(), 0);

    auto a = BuildDeleteMessage(messages[0]);
    messages1.push_back(a);
    IndexMessages(messages1, REALTIME, 1);

    ctx.ResultCountRequirement = 9;
    QuerySearch("value1&key_name=key1&sgkps=" + ToString(kps), results, ctx);
    CHECK_TEST_EQ(results.size(), 9);

    QuerySearch("value2&key_name=key2&sgkps=" + ToString(kps), results, ctx);
    CHECK_TEST_EQ(results.size(), 9);

    QuerySearch("value3&key_name=key1&sgkps=" + ToString(kps), results, ctx);
    CHECK_TEST_EQ(results.size(), 9);

    QuerySearch("value3&key_name=key2&sgkps=" + ToString(kps), results, ctx);
    CHECK_TEST_EQ(results.size(), 0);

    ReopenIndexers();
    QuerySearch("value1&key_name=key1&sgkps=" + ToString(kps), results, ctx);
    CHECK_TEST_EQ(results.size(), 9);

    QuerySearch("value2&key_name=key2&sgkps=" + ToString(kps), results, ctx);
    CHECK_TEST_EQ(results.size(), 9);

    QuerySearch("value3&key_name=key1&sgkps=" + ToString(kps), results, ctx);
    CHECK_TEST_EQ(results.size(), 9);

    QuerySearch("value3&key_name=key2&sgkps=" + ToString(kps), results, ctx);
    CHECK_TEST_EQ(results.size(), 0);

    auto b = BuildDeleteMessage(messages[1]);
    messages2.push_back(b);

    IndexMessages(messages2, REALTIME, 1);

    ctx.ResultCountRequirement = 8;
    QuerySearch("value1&key_name=key1&sgkps=" + ToString(kps), results, ctx);
    CHECK_TEST_EQ(results.size(), 8);

    QuerySearch("value2&key_name=key2&sgkps=" + ToString(kps), results, ctx);
    CHECK_TEST_EQ(results.size(), 8);

    QuerySearch("value3&key_name=key1&sgkps=" + ToString(kps), results, ctx);
    CHECK_TEST_EQ(results.size(), 8);

    QuerySearch("value3&key_name=key2&sgkps=" + ToString(kps), results, ctx);
    CHECK_TEST_EQ(results.size(), 0);

    return true;
}

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

START_TEST_DEFINE(TestFullArchiveNoKeys, TTestMarksPool::OneBackendOnly)
bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 10, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    TString keys;
    ui32 kps = GetIsPrefixed() ? 1 : 0;
    for (ui32 i = 0; i < messages.size(); ++i) {
        if (i)
            keys += ",";
        keys += messages[i].GetDocument().GetUrl();
        messages[i].MutableDocument()->ClearAdditionalKeys();
        messages[i].MutableDocument()->SetBody(ToString(i));
        messages[i].MutableDocument()->SetKeyPrefix(kps);
    }
    IndexMessages(messages, REALTIME, 1);

    TQuerySearchContext ctx;
    TVector<TDocSearchInfo> results;
    ctx.PrintResult = true;
    ctx.ResultCountRequirement = 10;

    QuerySearch(keys + "&sgkps=" + ToString(kps), results, ctx);
    CHECK_TEST_EQ(results.size(), 10);

    ReopenIndexers();
    QuerySearch(keys + "&sgkps=" + ToString(kps), results, ctx);
    CHECK_TEST_EQ(results.size(), 10);

    auto a = BuildDeleteMessage(messages[0]);
    messages.clear();
    messages.push_back(a);

    IndexMessages(messages, REALTIME, 1);

    QuerySearch(keys + "&sgkps=" + ToString(kps), results, ctx);
    CHECK_TEST_EQ(results.size(), 9);
    return true;
}

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

START_TEST_DEFINE(TestFullArchiveEmptyKeys, TTestMarksPool::OneBackendOnly)
bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 10, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    TString keys;
    ui32 kps = GetIsPrefixed() ? 1 : 0;
    for (ui32 i = 0; i < messages.size(); ++i) {
        if (i)
            keys += ",";
        keys += messages[i].GetDocument().GetUrl();
        messages[i].MutableDocument()->ClearAdditionalKeys();
        {
            auto* prop = messages[i].MutableDocument()->AddAdditionalKeys();
            prop->SetName("1");
            prop->SetValue("");
        }
        messages[i].MutableDocument()->SetBody(ToString(i));
        messages[i].MutableDocument()->SetKeyPrefix(kps);
    }
    MUST_BE_BROKEN(IndexMessages(messages, REALTIME, 1));

    for (ui32 i = 0; i < messages.size(); ++i) {
        messages[i].MutableDocument()->ClearAdditionalKeys();
        {
            auto* prop = messages[i].MutableDocument()->AddAdditionalKeys();
            prop->SetName("");
            prop->SetValue("1");
        }
    }
    MUST_BE_BROKEN(IndexMessages(messages, REALTIME, 1));

    return true;
}

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

START_TEST_DEFINE(TestFullArchiveGenerateDiffReplicas, TTestMarksPool::OneBackendOnly)
bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 10, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    for (ui32 i = 0; i < messages.size(); ++i) {
        messages[i].MutableDocument()->SetKeyPrefix(GetIsPrefixed() ? 2 : 0);
        messages[i].MutableDocument()->SetBody(ToString(i));
    }

    {
        TBackendProxy::TBackendSet stopBe;
        stopBe.insert(*Controller->GetActiveBackends().rbegin());
        Controller->ProcessCommand("stop", stopBe);
    }
    MUST_BE_BROKEN(IndexMessages(messages, DISK, 1));
    {
        Controller->ProcessCommand("restart");
    }

    {
        TBackendProxy::TBackendSet stopBe;
        stopBe.insert(*Controller->GetActiveBackends().begin());
        Controller->ProcessCommand("stop", stopBe);
    }
    MUST_BE_BROKEN(IndexMessages(TVector<NRTYServer::TMessage>(messages.begin(), messages.begin() + messages.size() / 2), DISK, 1));
    {
        Controller->ProcessCommand("restart");
    }

    ReopenIndexers();

    TString keys;
    for (ui32 i = 0; i < messages.size(); ++i) {
        if (i)
            keys += ",";
        keys += messages[i].GetDocument().GetUrl();
    }
    TVector<TDocSearchInfo> results;
    TQuerySearchContext ctx;
    if (Cluster->GetNodesNames(TNODE_SEARCHPROXY).size() && Controller->GetActiveBackends().size() == 2) {
        ctx.SourceSelector = new TLevelSourceSelector(2);

        ui32 resultSize1 = 0;
        ui32 resultSize2 = 0;
        for (auto i = 0; i < 100; ++i) {
            NOT_EXCEPT(QuerySearch(keys + "&relev=1&sp_meta_search=meta&meta_search=first_found&sgkps=" + ToString(messages[0].GetDocument().GetKeyPrefix()), results, ctx));
            resultSize1 = results.size();

            NOT_EXCEPT(QuerySearch(keys + "&relev=" + ToString(i) + "&sp_meta_search=meta&meta_search=first_found&sgkps=" + ToString(messages[0].GetDocument().GetKeyPrefix()), results, ctx));
            resultSize2 = results.size();

            CHECK_TEST_TRUE((resultSize1 == messages.size()) || (resultSize1 == messages.size() / 2));
            CHECK_TEST_TRUE((resultSize2 == messages.size()) || (resultSize2 == messages.size() / 2));

            if (resultSize1 != resultSize2) {
                break;
            }
        }
        CHECK_TEST_NEQ(resultSize1, resultSize2);

        ctx.SourceSelector = new TDirectSourceSelector();

        for (auto i = 0; i < 100; ++i) {
            NOT_EXCEPT(QuerySearch(keys + "&relev=1&sp_meta_search=proxy&meta_search=first_found&sgkps=" + ToString(messages[0].GetDocument().GetKeyPrefix()), results, ctx));
            resultSize1 = results.size();

            NOT_EXCEPT(QuerySearch(keys + "&relev=" + ToString(i) + "&sp_meta_search=proxy&meta_search=first_found&sgkps=" + ToString(messages[0].GetDocument().GetKeyPrefix()), results, ctx));
            resultSize2 = results.size();

            CHECK_TEST_TRUE((resultSize1 == messages.size()) || (resultSize1 == messages.size() / 2));
            CHECK_TEST_TRUE((resultSize2 == messages.size()) || (resultSize2 == messages.size() / 2));

            if (resultSize1 != resultSize2) {
                break;
            }
        }
        CHECK_TEST_NEQ(resultSize1, resultSize2);
    } else {
        ERROR_LOG << "Incorrect test usage case" << Endl;
        return false;
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
    (*SPConfigDiff)["Service.ProxyMeta.ParallelRequestCount"] = 1;
    (*SPConfigDiff)["Service.ProxyMeta.TasksCheckIntervalms"] = 100000;
    return true;
}
};

START_TEST_DEFINE(TestFullArchiveDelegate, TTestMarksPool::OneBackendOnly)
bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 10, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    for (ui32 i = 0; i < messages.size(); ++i) {
        messages[i].MutableDocument()->SetKeyPrefix(messages[0].MutableDocument()->GetKeyPrefix());
    }
    IndexMessages(messages, DISK, 1);
    ReopenIndexers();
    Controller->ProcessCommand("create_merger_tasks");
    Controller->ProcessCommand("do_all_merger_tasks");

    for (ui32 i = 0; i < messages.size(); ++i) {
        TVector<TDocSearchInfo> results;
        TQuerySearchContext ctx;
        ctx.SourceSelector = new TDirectSourceSelector();
        QuerySearch(messages[i].GetDocument().GetUrl() + "&sp_meta_search=proxy", results, ctx);
        if (results.size() != 1)
            TEST_FAILED("Test failed: " + ToString(results.size()));
    }
    CHECK_TEST_EQ(GetSearchableDocsCount(Controller), 10);
    return true;
}

bool InitConfig() override {
    SetMergerParams(true, 1, 1, mcpTIME, Max<i32>());
    (*ConfigDiff)["IndexGenerator"] = FULL_ARCHIVE_COMPONENT_NAME;
    (*ConfigDiff)["Components"] = FULL_ARCHIVE_COMPONENT_NAME;
    (*ConfigDiff)["Indexer.Memory.Enabled"] = "false";
    (*ConfigDiff)["Searcher.SnippetsDeniedZones"] = "";
    (*ConfigDiff)["Searcher.DelegateRequestOptimization"] = "true";
    (*ConfigDiff)["SearchersCountLimit"] = 1;
    (*ConfigDiff)["Searcher.TwoStepQuery"] = "false";
    (*SPConfigDiff)["Service.MetaSearch.TwoStepQuery"] = "false";
    return true;
}
};

START_TEST_DEFINE(TestFullArchiveCustomMetasearch, TTestMarksPool::OneBackendOnly)
bool Run() override {
    TVector<NRTYServer::TMessage> toCheck;
    {
        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, 10, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
        for (ui32 i = 0; i < messages.size(); ++i) {
            messages[i].MutableDocument()->SetKeyPrefix(messages[0].MutableDocument()->GetKeyPrefix());
            toCheck.push_back(messages[i]);
        }
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();
    }
    {
        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, 10, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
        for (ui32 i = 0; i < messages.size(); ++i) {
            messages[i].MutableDocument()->SetKeyPrefix(messages[0].MutableDocument()->GetKeyPrefix());
            toCheck.push_back(messages[i]);
        }
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();
    }
    {
        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, 10, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
        for (ui32 i = 0; i < messages.size(); ++i) {
            messages[i].MutableDocument()->SetKeyPrefix(messages[0].MutableDocument()->GetKeyPrefix());
            toCheck.push_back(messages[i]);
        }
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();
    }

    for (ui32 i = 0; i < toCheck.size(); ++i) {
        TVector<TDocSearchInfo> results;
        TQuerySearchContext ctx;
        ctx.SourceSelector = new TDirectSourceSelector();
        QuerySearch(toCheck[i].GetDocument().GetUrl() + "&sp_meta_search=proxy&meta_search=first_found&sgkps=" + ToString(toCheck[i].GetDocument().GetKeyPrefix()), results, ctx);
        if (results.size() != 1)
            TEST_FAILED("Test failed: " + ToString(results.size()));
    }

    CHECK_TEST_EQ(GetSearchableDocsCount(Controller), 30);
    return CheckShardsCount(3);
}

bool CheckShardsCount(long long required) {
    TJsonPtr info = Controller->GetServerInfo();
    NJson::TJsonValue::TArray jsonArr;
    CHECK_TEST_EQ(info->GetArray(&jsonArr), true);
    NJson::TJsonValue countShards = jsonArr[0]["search_sources_count"];
    if (!countShards.IsInteger()) {
        ythrow yexception() << "there is no countShards: " << info->GetStringRobust() << Endl;
    }
    CHECK_TEST_EQ(countShards.GetInteger(), required);
    return true;
}

bool InitConfig() override {
    (*ConfigDiff)["IndexGenerator"] = FULL_ARCHIVE_COMPONENT_NAME;
    (*ConfigDiff)["Components"] = FULL_ARCHIVE_COMPONENT_NAME;
    (*ConfigDiff)["Indexer.Memory.Enabled"] = "false";
    (*ConfigDiff)["Searcher.SnippetsDeniedZones"] = "";
    (*ConfigDiff)["Searcher.TwoStepQuery"] = "false";
    return true;
}
};

START_TEST_DEFINE(TestFullArchiveDetach, TTestMarksPool::OneBackendOnly)
bool GenAndIndexMessages(ui32 count, ui32 checkCount, TVector<NRTYServer::TMessage>& messages) {
    GenerateInput(messages, count, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    for (ui32 i = 0; i < messages.size(); ++i) {
        messages[i].MutableDocument()->SetKeyPrefix(messages[0].MutableDocument()->GetKeyPrefix());
    }
    IndexMessages(messages, REALTIME, 1);

    for (ui32 i = 0; i < messages.size(); ++i) {
        TVector<TDocSearchInfo> results;
        TQuerySearchContext ctx;
        ctx.AttemptionsCount = 5;
        ctx.ResultCountRequirement = 1;
        QuerySearch(messages[i].GetDocument().GetUrl() + "&sgkps=" + ToString(messages[i].GetDocument().GetKeyPrefix()), results, ctx);
        if (results.size() != 1) {
            PrintInfoServer();
            TEST_FAILED("Test failed: " + ToString(results.size()));
        }
    }

    CHECK_TEST_EQ(GetSearchableDocsCount(Controller), checkCount);
    return true;
}


bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    INFO_LOG << "Run test stage IndexMessage15" << Endl;
    CHECK_TEST_TRUE(GenAndIndexMessages(15, 15, messages));
    messages.clear();
    INFO_LOG << "Run test stage IndexMessage100" << Endl;
    CHECK_TEST_TRUE(GenAndIndexMessages(85, 100, messages));

    ReopenIndexers();

    for (ui32 i = 0; i < messages.size(); ++i) {
        TVector<TDocSearchInfo> results;
        QuerySearch(messages[i].GetDocument().GetUrl() + "&sgkps=" + ToString(messages[i].GetDocument().GetKeyPrefix()), results);
        if (results.size() != 1)
            TEST_FAILED("Test failed: " + ToString(results.size()));
    }

    TString reply;
    if (!Controller->Detach(0, NSearchMapParser::SearchMapShards, NSaas::TShardsDispatcher::TContext(NSaas::UrlHash), reply)) {
        ERROR_LOG << "Detach failed: " << reply << Endl;
        return false;
    }
    INFO_LOG << reply << Endl;
    CHECK_TEST_TRUE(reply.StartsWith("rbtorrent"));

    TString syncReply;
    if (!Controller->Synchronize(reply, syncReply)) {
        ERROR_LOG << "Sync failed: " << syncReply << Endl;
        return false;
    }
    return true;
}

bool InitConfig() override {
    SetIndexerParams(DISK, 10, 1);
    (*ConfigDiff)["IndexGenerator"] = FULL_ARCHIVE_COMPONENT_NAME;
    (*ConfigDiff)["Components"] = FULL_ARCHIVE_COMPONENT_NAME;
    (*ConfigDiff)["Indexer.Memory.Enabled"] = "true";
    (*ConfigDiff)["Indexer.Memory.MaxDocumentsReserveCapacityCoeff"] = "5";
    (*ConfigDiff)["Searcher.SnippetsDeniedZones"] = "";
    (*ConfigDiff)["Searcher.TwoStepQuery"] = "false";
    (*SPConfigDiff)["Service.MetaSearch.TwoStepQuery"] = "false";
    return true;
}
};

START_TEST_DEFINE(TestFullArchiveRealtime, TTestMarksPool::OneBackendOnly)
bool GenAndIndexMessages(ui32 count, ui32 checkCount, TVector<NRTYServer::TMessage>& messages) {
    GenerateInput(messages, count, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    for (ui32 i = 0; i < messages.size(); ++i) {
        messages[i].MutableDocument()->SetKeyPrefix(messages[0].MutableDocument()->GetKeyPrefix());
    }
    IndexMessages(messages, REALTIME, 1);

    for (ui32 i = 0; i < messages.size(); ++i) {
        TVector<TDocSearchInfo> results;
        TQuerySearchContext ctx;
        ctx.AttemptionsCount = 5;
        ctx.ResultCountRequirement = 1;
        QuerySearch(messages[i].GetDocument().GetUrl() + "&sgkps=" + ToString(messages[i].GetDocument().GetKeyPrefix()), results, ctx);
        if (results.size() != 1) {
            PrintInfoServer();
            TEST_FAILED("Test failed: " + ToString(results.size()));
        }
    }
    for (ui32 i = 0; i < 5; ++i) {
        if (GetSearchableDocsCount(Controller) == checkCount) {
            return true;
        } else {
            ERROR_LOG << GetSearchableDocsCount(Controller) << " != " << checkCount << Endl;
        }
        Sleep(TDuration::Seconds(1));
    }
    return false;
}


bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    INFO_LOG << "Run test stage IndexMessage15" << Endl;
    CHECK_TEST_TRUE(GenAndIndexMessages(15, 15, messages));
    messages.clear();
    INFO_LOG << "Run test stage IndexMessage100" << Endl;
    CHECK_TEST_TRUE(GenAndIndexMessages(85, 100, messages));

    ReopenIndexers();

    for (ui32 i = 0; i < messages.size(); ++i) {
        TVector<TDocSearchInfo> results;
        QuerySearch(messages[i].GetDocument().GetUrl() + "&sgkps=" + ToString(messages[i].GetDocument().GetKeyPrefix()), results);
        if (results.size() != 1)
            TEST_FAILED("Test failed: " + ToString(results.size()));
    }

    if (const auto kps = messages[0].GetDocument().GetKeyPrefix()) {
        DeleteSpecial(kps);

        TVector<TDocSearchInfo> results;
        QuerySearch(messages[0].GetDocument().GetUrl() + "&sgkps=" + ToString(messages[0].GetDocument().GetKeyPrefix()), results);
        if (results.size() != 0) {
            TEST_FAILED("Document is not deleted: " + ToString(messages[0].GetDocument().GetUrl()));
        }
    }

    DeleteSpecial();
    for (ui32 i = 0; i < messages.size(); ++i) {
        TVector<TDocSearchInfo> results;
        QuerySearch(messages[i].GetDocument().GetUrl() + "&sgkps=" + ToString(messages[i].GetDocument().GetKeyPrefix()), results);
        if (results.size() != 0)
            TEST_FAILED("Document is not deleted: " + ToString(messages[1].GetDocument().GetUrl()));
    }

    return true;
}

bool InitConfig() override {
    SetIndexerParams(DISK, 10, 1);
    (*ConfigDiff)["IndexGenerator"] = FULL_ARCHIVE_COMPONENT_NAME;
    (*ConfigDiff)["Components"] = FULL_ARCHIVE_COMPONENT_NAME;
    (*ConfigDiff)["Indexer.Memory.Enabled"] = "true";
    (*ConfigDiff)["Searcher.SnippetsDeniedZones"] = "";
    (*ConfigDiff)["Searcher.TwoStepQuery"] = "false";
    (*SPConfigDiff)["Service.MetaSearch.TwoStepQuery"] = "false";
    return true;
}
};

START_TEST_DEFINE(TestFullArchiveRtModify, TTestMarksPool::OneBackendOnly)
bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 10, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    for (ui32 i = 0; i < messages.size(); ++i) {
        messages[i].MutableDocument()->SetKeyPrefix(messages[0].MutableDocument()->GetKeyPrefix());
        messages[i].MutableDocument()->SetBody(ToString(i));
    }
    IndexMessages(messages, REALTIME, 1);

    for (ui32 i = 0; i < messages.size(); ++i) {
        TVector<TDocSearchInfo> results;
        TQuerySearchContext::TDocProperties docProperties;
        TQuerySearchContext ctx;
        ctx.DocProperties = &docProperties;
        QuerySearch(messages[i].GetDocument().GetUrl() + "&sgkps=" + ToString(messages[i].GetDocument().GetKeyPrefix()), results, ctx);
        if (results.size() != 1) {
            TEST_FAILED("Test failed: " + ToString(results.size()));
        } else {
            CHECK_TEST_NEQ(docProperties.size(), 0);
            TString data(Base64Decode(docProperties[0]->find("data")->second));
            NRTYServer::TMessage::TDocument doc;
            Y_PROTOBUF_SUPPRESS_NODISCARD doc.ParseFromString(data);
            CHECK_TEST_EQ(doc.GetBody(), messages[i].GetDocument().GetBody());
        }
    }

    TVector<NRTYServer::TMessage> modified;
    messages[0].MutableDocument()->SetBody("modified");
    modified.push_back(messages[0]);
    IndexMessages(modified, REALTIME, 1);

    TVector<TDocSearchInfo> results;
    TQuerySearchContext::TDocProperties docProperties;
    TQuerySearchContext ctx;
    ctx.DocProperties = &docProperties;
    QuerySearch(messages[0].GetDocument().GetUrl() + "&sgkps=" + ToString(messages[0].GetDocument().GetKeyPrefix()), results, ctx);
    if (results.size() != 1) {
        TEST_FAILED("Test failed: " + ToString(results.size()));
    } else {
        CHECK_TEST_NEQ(docProperties.size(), 0);
        TString data(docProperties[0]->find("data")->second);
        NRTYServer::TMessage::TDocument doc;
        Y_PROTOBUF_SUPPRESS_NODISCARD doc.ParseFromString(Base64Decode(data));
        CHECK_TEST_EQ(doc.GetBody(), "modified");
    }
    return true;
}

bool InitConfig() override {
    SetIndexerParams(DISK, 100, 1);
    SetIndexerParams(REALTIME, 100, 1);
    (*ConfigDiff)["IndexGenerator"] = FULL_ARCHIVE_COMPONENT_NAME;
    (*ConfigDiff)["Components"] = FULL_ARCHIVE_COMPONENT_NAME;
    (*ConfigDiff)["Indexer.Memory.Enabled"] = "true";
    (*ConfigDiff)["Searcher.SnippetsDeniedZones"] = "";
    (*ConfigDiff)["Searcher.TwoStepQuery"] = "false";
    (*SPConfigDiff)["Service.MetaSearch.TwoStepQuery"] = "false";
    return true;
}
};

SERVICE_TEST_RTYSERVER_DEFINE(TestFullArchiveReindexingCheckMetricsBase, TTestMarksPool::OneBackendOnly)
bool DoRun(TIndexerType indexer) {
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 200, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    for (ui32 i = 0; i < messages.size(); ++i) {
        messages[i].MutableDocument()->SetKeyPrefix(messages[0].MutableDocument()->GetKeyPrefix());
        messages[i].MutableDocument()->SetBody(ToString(i));
    }

    TIndexerClient::TContext indexingCtx;
    indexingCtx.CountThreads = 32;
    indexingCtx.DoWaitReply = true;
    IndexMessages(messages, indexer, indexingCtx);
    INFO_LOG << "Memory index RPS: " << GetMemoryIndexRps(Controller) << Endl;
    IndexMessages(messages, indexer, indexingCtx);
    if (indexer == DISK) {
        ReopenIndexers();
    }
    for (ui32 i = 0; i < messages.size(); ++i) {
        TVector<TDocSearchInfo> results;
        TQuerySearchContext::TDocProperties docProperties;
        TQuerySearchContext ctx;
        ctx.AttemptionsCount = 5;
        ctx.ResultCountRequirement = 1;
        ctx.DocProperties = &docProperties;
        QuerySearch(messages[i].GetDocument().GetUrl() + "&sgkps=" + ToString(messages[i].GetDocument().GetKeyPrefix()), results, ctx);
        if (results.size() != 1) {
            PrintInfoServer();
            TEST_FAILED("Test failed: " + ToString(results.size()));
        }
    }
    return true;
}

ui64 GetDocsCountFromMetrics() {
    const ui64 added = Controller->GetMetric("Indexer_DocumentsAdded", TBackendProxy::TBackendSet(0));
    const ui64 deleted = Controller->GetMetric("Indexer_DocumentsDeleted", TBackendProxy::TBackendSet(0));
    return added - deleted;
}

virtual bool DoRun() {
    CHECK_TEST_TRUE(DoRun(DISK));
    CHECK_TEST_EQ(GetSearchableDocsCount(Controller), 200);
    CHECK_TEST_EQ(GetDocsCountFromMetrics(), 200);

    CHECK_TEST_TRUE(DoRun(REALTIME));
    CHECK_TEST_EQ(GetSearchableDocsCount(Controller), 400);
    CHECK_TEST_EQ(GetDocsCountFromMetrics(), 400);
    return true;
}

bool InitConfig() override {
    SetIndexerParams(DISK, 10, 8);
    SetIndexerParams(REALTIME, 10, 8);
    SetMergerParams(true, 1, 4, mcpCONTINUOUS, -1, 1 << DOC_LEVEL_Bits);
    (*ConfigDiff)["IndexGenerator"] = FULL_ARCHIVE_COMPONENT_NAME;
    (*ConfigDiff)["Components"] = FULL_ARCHIVE_COMPONENT_NAME;
    (*ConfigDiff)["Indexer.Memory.Enabled"] = "true";
    (*ConfigDiff)["Indexer.Disk.CloseThreads"] = "4";
    (*ConfigDiff)["Searcher.SnippetsDeniedZones"] = "";
    (*ConfigDiff)["Searcher.TwoStepQuery"] = "false";
    (*SPConfigDiff)["Service.MetaSearch.TwoStepQuery"] = "false";
    return true;
}
};

START_TEST_DEFINE_PARENT(TestFullArchiveReindexingCheckMetrics, TestFullArchiveReindexingCheckMetricsBase)
bool Run() override {
    return DoRun();
}
};

START_TEST_DEFINE_PARENT(TestFullArchiveReindexingCheckMetricsPersistent, TestFullArchiveReindexingCheckMetricsBase)
bool Run() override {
    if (!DoRun()) {
        return false;
    }
    Controller->RestartServer();
    CHECK_TEST_EQ(GetDocsCountFromMetrics(), 400);
    return true;
}
};


START_TEST_DEFINE(TestFullArchiveReindexing, TTestMarksPool::OneBackendOnly)
bool DoRun(TIndexerType indexer) {
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 10000, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    for (ui32 i = 0; i < messages.size(); ++i) {
        messages[i].MutableDocument()->SetKeyPrefix(messages[0].MutableDocument()->GetKeyPrefix());
        messages[i].MutableDocument()->SetBody(ToString(i));
    }

    TIndexerClient::TContext indexingCtx;
    indexingCtx.CountThreads = 32;
    indexingCtx.DoWaitReply = true;
    IndexMessages(messages, indexer, indexingCtx);
    INFO_LOG << "Memory index RPS: " << GetMemoryIndexRps(Controller) << Endl;
    IndexMessages(messages, indexer, indexingCtx);
    if (indexer == DISK) {
        ReopenIndexers();
    }
    for (ui32 i = 0; i < messages.size(); ++i) {
        TVector<TDocSearchInfo> results;
        TQuerySearchContext::TDocProperties docProperties;
        TQuerySearchContext ctx;
        ctx.DocProperties = &docProperties;
        QuerySearch(messages[i].GetDocument().GetUrl() + "&sgkps=" + ToString(messages[i].GetDocument().GetKeyPrefix()), results, ctx);
        if (results.size() != 1) {
            PrintInfoServer();
            TEST_FAILED("Test failed: " + ToString(results.size()));
        }
    }
    return true;
}

bool Run() override {
    CHECK_TEST_TRUE(DoRun(DISK));
    CHECK_TEST_EQ(GetSearchableDocsCount(Controller), 10000);
    CHECK_TEST_TRUE(DoRun(REALTIME));
    CHECK_TEST_EQ(GetSearchableDocsCount(Controller), 20000);
    return true;
}

bool InitConfig() override {
    SetIndexerParams(DISK, 50, 8);
    SetIndexerParams(REALTIME, 50, 8);
    SetMergerParams(true, 1, 4, mcpCONTINUOUS, -1, 1 << DOC_LEVEL_Bits);
    (*ConfigDiff)["IndexGenerator"] = FULL_ARCHIVE_COMPONENT_NAME;
    (*ConfigDiff)["Components"] = FULL_ARCHIVE_COMPONENT_NAME;
    (*ConfigDiff)["Indexer.Memory.Enabled"] = "true";
    (*ConfigDiff)["Indexer.Disk.CloseThreads"] = "4";
    (*ConfigDiff)["Searcher.SnippetsDeniedZones"] = "";
    (*ConfigDiff)["Searcher.TwoStepQuery"] = "false";
    (*SPConfigDiff)["Service.MetaSearch.TwoStepQuery"] = "false";
    return true;
}
};

SERVICE_TEST_RTYSERVER_DEFINE(TestMergerBase)
virtual bool DoRun(bool optimize) {
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 1000, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    for (ui32 i = 0; i < messages.size(); ++i) {
        messages[i].MutableDocument()->SetKeyPrefix(messages[0].MutableDocument()->GetKeyPrefix());
        messages[i].MutableDocument()->SetBody(ToString(i));
    }

    IndexMessages(messages, DISK, 1, 0, true);

    ReopenIndexers();
    ui32 initialIndexesCount = 0;
    {
        TSet<TString> indexes = Controller->GetFinalIndexes();

        NRTYArchive::TMultipartConfig config;
        config.ReadContextDataAccessType = NRTYArchive::IDataAccessor::DIRECT_FILE;
        config.Compression = NRTYArchive::IArchivePart::COMPRESSED;

        for (const auto& path : indexes) {
            if (path.StartsWith("temp_"))
                continue;
            TFsPath fullArc(path + "/" + FULL_ARC_FILE_NAME_PREFIX + NRTYServer::NFullArchive::FullLayer);
            TArchiveOwner::TPtr archive = TArchiveOwner::Create(fullArc, config);
            size_t partsCount = archive->GetPartsCount();
            CHECK_TEST_EQ(partsCount, 2);
            initialIndexesCount++;
        }
        Controller->RestartServer();
    }

    SetMergerParams(true, 1, 1, mcpTIME, Max<i32>());

    if (optimize) {
        // Set optimize config.
        (*ConfigDiff)["ComponentsConfig." FULL_ARCHIVE_COMPONENT_NAME ".Layers.full.PartSizeLimit"] = ToString(1 << 30);
        PrintInfoServer();
    }

    ApplyConfig();
    Controller->RestartServer();

    Controller->ProcessCommand("create_merger_tasks");
    Controller->ProcessCommand("do_all_merger_tasks");

    {
        TSet<TString> indexes = Controller->GetFinalIndexes();
        NRTYArchive::TMultipartConfig config;
        config.ReadContextDataAccessType = NRTYArchive::IDataAccessor::DIRECT_FILE;
        config.Compression = NRTYArchive::IArchivePart::COMPRESSED;

        ui32 indexesCount = 0;
        for (const auto& path : indexes) {
            if (path.StartsWith("temp_"))
                continue;
            TFsPath fullArc(path + "/" + FULL_ARC_FILE_NAME_PREFIX + NRTYServer::NFullArchive::FullLayer);
            TArchiveOwner::TPtr archive = TArchiveOwner::Create(fullArc, config);
            size_t partsCount = archive->GetPartsCount();

            if (optimize) {
                CHECK_TEST_EQ(partsCount, 2);
            } else {
                CHECK_TEST_EQ(partsCount, initialIndexesCount + 1);
            }

            indexesCount++;
        }
        CHECK_TEST_EQ(indexesCount, 1);
        Controller->RestartServer();
        CHECK_TEST_EQ(GetSearchableDocsCount(Controller), 1000);
    }
    PrintInfoServer();

    for (ui32 i = 0; i < messages.size(); ++i) {
        TVector<TDocSearchInfo> results;
        TQuerySearchContext ctx;
        if (Cluster->GetNodesNames(TNODE_SEARCHPROXY).size()) {
            CHECK_TEST_EQ(Controller->GetActiveBackends().size(), 1);
            ctx.SourceSelector = new TLevelSourceSelector(1);
        } else {
            ctx.SourceSelector = new TDirectSourceSelector();
        }
        QuerySearch(messages[i].GetDocument().GetUrl() + "&sp_meta_search=proxy&meta_search=first_found&sgkps=" + ToString(messages[i].GetDocument().GetKeyPrefix()), results, ctx);
        if (results.size() != 1) {
            PrintInfoServer();
            TEST_FAILED("Test failed for " + messages[i].GetDocument().GetUrl() + ": documents count - " + ToString(results.size()));
        }
    }

    return true;
}

bool InitConfig() override {
    SetIndexerParams(DISK, 10, 8);
    (*ConfigDiff)["IndexGenerator"] = FULL_ARCHIVE_COMPONENT_NAME;
    (*ConfigDiff)["Components"] = FULL_ARCHIVE_COMPONENT_NAME;
    (*ConfigDiff)["Searcher.SnippetsDeniedZones"] = "";
    (*ConfigDiff)["Searcher.TwoStepQuery"] = "false";
    (*SPConfigDiff)["Service.MetaSearch.TwoStepQuery"] = "false";
    (*SPConfigDiff)["Service.ProxyMeta.TimeoutSendingms"] = "100";
    (*SPConfigDiff)["Service.ProxyMeta.TimeoutConnectms"] = "100";
    return true;
}
};

START_TEST_DEFINE_PARENT(TestFullArchiveMergeWithoutOptimize, TestMergerBase)
bool Run() override {
    return DoRun(false);
}
};

START_TEST_DEFINE_PARENT(TestFullArchiveOptimizeParts, TestMergerBase)
bool Run() override {
    return DoRun(true);
}
};

START_TEST_DEFINE(TestFullArchiveDocInfo, TTestMarksPool::OneBackendOnly)
bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 10, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    for (ui32 i = 0; i < messages.size(); ++i) {
        messages[i].MutableDocument()->SetKeyPrefix(messages[0].MutableDocument()->GetKeyPrefix());
        messages[i].MutableDocument()->SetModificationTimestamp(10000 * i);
    }
    IndexMessages(messages, REALTIME, 1);

    for (ui32 i = 0; i < messages.size(); ++i) {
        TVector<TDocSearchInfo> results;
        TQuerySearchContext ctx;
        ctx.AttemptionsCount = 5;
        QuerySearch(messages[i].GetDocument().GetUrl() + "&sgkps=" + ToString(messages[i].GetDocument().GetKeyPrefix()), results, ctx);
        if (results.size() != 1) {
            PrintInfoServer();
            TEST_FAILED("Test failed: " + ToString(results.size()));
        }

        const TDocSearchInfo& dsi = results[0];
        TJsonPtr jsonDocInfoPtr = Controller->GetDocInfo(dsi.GetSearcherId(), dsi.GetDocId());
        DEBUG_LOG << NUtil::JsonToString(*jsonDocInfoPtr) << Endl;
        TDocInfo di(*jsonDocInfoPtr);
        CHECK_TEST_EQ(di.GetDDKDocInfo()["Timestamp"], 10000 * i);
    }
    return true;
}

bool InitConfig() override {
    SetIndexerParams(DISK, 100, 1);
    (*ConfigDiff)["IndexGenerator"] = FULL_ARCHIVE_COMPONENT_NAME;
    (*ConfigDiff)["Components"] = FULL_ARCHIVE_COMPONENT_NAME;
    (*ConfigDiff)["Indexer.Memory.Enabled"] = "true";
    (*ConfigDiff)["Searcher.SnippetsDeniedZones"] = "";
    (*ConfigDiff)["Searcher.TwoStepQuery"] = "false";
    (*SPConfigDiff)["Service.MetaSearch.TwoStepQuery"] = "false";
    return true;
}
};

START_TEST_DEFINE(TestFullArchiveNormalReport)
bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 10, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    for (ui32 i = 0; i < messages.size(); ++i) {
        messages[i].MutableDocument()->SetKeyPrefix(messages[0].MutableDocument()->GetKeyPrefix());
        messages[i].MutableDocument()->SetUrl("key_" + ToString(i));
        messages[i].MutableDocument()->SetBody("");
        auto prop = messages[i].MutableDocument()->AddDocumentProperties();
        prop->SetName("Subkey_1");
        prop->SetValue("Value_1");
        prop = messages[i].MutableDocument()->AddDocumentProperties();
        prop->SetName("Subkey_2");
        prop->SetValue("Value_2");
    }
    messages[0].MutableDocument()->SetBody("Body");

    IndexMessages(messages, DISK, 1);
    ReopenIndexers();

    for (ui32 i = 0; i < messages.size(); ++i) {
        TVector<TDocSearchInfo> results;
        TQuerySearchContext ctx;

        if (Cluster->GetNodesNames(TNODE_SEARCHPROXY).size()) {
            ctx.SourceSelector = new TLevelSourceSelector(2);
        } else {
            ctx.SourceSelector = new TDirectSourceSelector();
        }

        ctx.AttemptionsCount = 5;
        ctx.PrintResult = true;
        TQuerySearchContext::TDocProperties resultProps;
        ctx.DocProperties = &resultProps;
        QuerySearch(messages[i].GetDocument().GetUrl() + "&sp_meta_search=multi_proxy&normal_kv_report=da&gta=_AllDocInfos&meta_search=first_found&sgkps=" + ToString(messages[i].GetDocument().GetKeyPrefix()), results, ctx);
        if (results.size() != 1) {
            PrintInfoServer();
            TEST_FAILED("Test failed: " + ToString(results.size()));
        }

        auto docProperties = resultProps[0];
        TString val1 = docProperties->find("Subkey_1")->second;
        CHECK_TEST_EQ(val1, "Value_1");
        TString val2 = docProperties->find("Subkey_2")->second;
        CHECK_TEST_EQ(val2, "Value_2");
        if (i == 0) {
            CHECK_TEST_TRUE(docProperties->find("_Body") != docProperties->end());
            TString val1 = docProperties->find("_Body")->second;
            CHECK_TEST_EQ(val1, "Body");
        } else {
            CHECK_TEST_TRUE(docProperties->find("_Body") == docProperties->end());
        }
    }
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
    return true;
}
};

START_TEST_DEFINE(TestFullArchiveCompressedReport)
bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    IndexMessages(messages, DISK, 1);
    ReopenIndexers();

    NRTYServer::TMessage& mes = messages[0];
    TString key = mes.GetDocument().GetUrl();
    TString prefix = ToString(mes.GetDocument().GetKeyPrefix());
    if (Cluster->GetNodesNames(TNODE_SEARCHPROXY).size()) {
        TQuerySearchContext ctx;
        ctx.PrintResult = true;
        ctx.CompressedReport = true;
        TVector<TDocSearchInfo> results;
        QuerySearch(key + "&meta_search=first_found&pron=pcgzip&kv_compression=gzip&sgkps=" + prefix, results, ctx);
        if (results.size() != 1) {
            PrintInfoServer();
            TEST_FAILED("Test failed: " + ToString(results.size()));
        }

        results.clear();
        ctx.SourceSelector = new TDirectSourceSelector();
        QuerySearch(key + "&meta_search=first_found&pron=pcgzip&kv_compression=gzip&sp_meta_search=proxy&sgkps=" + prefix, results, ctx);
        if (results.size() != 1) {
            PrintInfoServer();
            TEST_FAILED("Test failed: " + ToString(results.size()));
        }

        ctx.SourceSelector = new TDefaultSourceSelector(-1);
        ctx.CompressedReport = false;
        results.clear();
        QuerySearch(key + "&meta_search=first_found&sgkps=" + prefix, results, ctx);
        if (results.size() != 1) {
            PrintInfoServer();
            TEST_FAILED("Test failed: " + ToString(results.size()));
        }
    } else {
        TQuerySearchContext ctx;
        ctx.PrintResult = true;
        ctx.SourceSelector = new TDirectSourceSelector();
        ctx.CompressedReport = true;

        TVector<TDocSearchInfo> results;
        QuerySearch(key + "&meta_search=first_found&pron=pcgzip&sgkps=" + prefix, results, ctx);
        if (results.size() != 1) {
            PrintInfoServer();
            TEST_FAILED("Test failed: " + ToString(results.size()));
        }
    }

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
    return true;
}
};

START_TEST_DEFINE(TestFullArchiveSearchReport)
bool Run() override {

    CHECK_TEST_TRUE(Cluster->GetNodesNames(TNODE_SEARCHPROXY).size());

    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 2, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    TString prefixes;
    if (GetIsPrefixed()) {
        messages[0].MutableDocument()->SetKeyPrefix(1);
        messages[1].MutableDocument()->SetKeyPrefix(65000);
        prefixes = "1,65000";
    } else {
        prefixes = "0";
    }
    messages[0].MutableDocument()->SetUrl("AAA");
    messages[1].MutableDocument()->SetUrl("KeyXXXXXYYYY");

    TString key = messages[0].GetDocument().GetUrl();
    TString prefix = ToString(messages[0].GetDocument().GetKeyPrefix());

    TString key2 = messages[1].GetDocument().GetUrl();
    TString prefix2 = ToString(messages[1].GetDocument().GetKeyPrefix());

    IndexMessages(messages, DISK, 1);
    ReopenIndexers();

    TQuerySearchContext ctx;
    ctx.HumanReadable = true;
    ctx.PrintResult = true;
    TVector<TDocSearchInfo> results;
    ctx.SourceSelector = new TLevelSourceSelector(2);
    {
        QuerySearch(key + "," + key2 + "&meta_search=first_found&sp_meta_search=meta&sgkps=" + prefixes, results, ctx);
        CHECK_TEST_EQ(results.size(), 2);
        TString docId1 = results[0].GetFullDocId();
        TString docId2 = results[1].GetFullDocId();
        TString src1 = docId1.substr(0, docId1.find('-'));
        TString src2 = docId2.substr(0, docId2.find('-'));
        DEBUG_LOG << docId1 << "/" << docId2 << Endl;
        CHECK_TEST_NEQ(src1, src2);
    }

    {

        // MetaSearch
        // Simple queries
        QuerySearch(key + "&meta_search=first_found&sp_meta_search=meta&sgkps=" + prefix, results, ctx);
        CHECK_TEST_EQ(results.size(), 1);

        results.clear();
        QuerySearch(key + "&meta_search=first_found&sp_meta_search=multi_proxy&sgkps=" + prefix, results, ctx);
        CHECK_TEST_EQ(results.size(), 1);

        // Search unknown keys
        QuerySearch("unknown_key&meta_search=first_found&sp_meta_search=meta&sgkps=" + prefix, results, ctx);
        CHECK_TEST_EQ(results.size(), 0);

        QuerySearch("unknown_key&meta_search=first_found&sp_meta_search=multi_proxy&sgkps=" + prefix, results, ctx);
        CHECK_TEST_EQ(results.size(), 0);

        // Search 2 keys
        QuerySearch(key + "," + key2 + "&meta_search=first_found&sp_meta_search=meta&sgkps=" + prefixes, results, ctx);
        CHECK_TEST_EQ(results.size(), 2);

        QuerySearch(key + "," + key2 + "&meta_search=first_found&sp_meta_search=multi_proxy&sgkps=" + prefixes, results, ctx);
        CHECK_TEST_EQ(results.size(), 2);

        // Proxi
        ctx.SourceSelector = new TDirectSourceSelector();

        // Simple queries
        QuerySearch(key + "&meta_search=first_found&sp_meta_search=proxy&sgkps=" + prefix, results, ctx);
        CHECK_TEST_EQ(results.size(), 1);

        // Search unknown keys
        QuerySearch("unknown_key&meta_search=first_found&sp_meta_search=proxy&sgkps=" + prefix, results, ctx);
        CHECK_TEST_EQ(results.size(), 0);

        // Search 2 keys
        QuerySearch(key + "," + key2 + "&meta_search=first_found&sp_meta_search=proxy&sgkps=" + prefixes, results, ctx);
        CHECK_TEST_EQ(results.size(), 0);
        CHECK_TEST_EQ(ctx.Errors.size(), 1);

        ctx.HumanReadable = false;
        QuerySearch("unknown_key&meta_search=first_found&sp_meta_search=multi_proxy&sgkps=" + prefix, results, ctx);
        CHECK_TEST_EQ(results.size(), 0);

    }

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
    return true;
}
};

START_TEST_DEFINE(TestFullArchiveEmptyGta)
bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 1, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    messages[0].MutableDocument()->SetKeyPrefix(messages[0].MutableDocument()->GetKeyPrefix());
    messages[0].MutableDocument()->SetUrl("some_key");
    messages[0].MutableDocument()->SetBody("Body");

    auto prop = messages[0].MutableDocument()->AddDocumentProperties();
    prop->SetName("some_property");
    prop->SetValue("test");

    IndexMessages(messages, DISK, 1);
    ReopenIndexers();

    TQuerySearchContext ctx;
    ctx.PrintResult = true;
    ctx.SourceSelector = new TDirectSourceSelector();
    TQuerySearchContext::TDocProperties resultProps;
    ctx.DocProperties = &resultProps;

    TVector<TDocSearchInfo> results;
    QuerySearch("some_key&sp_meta_search=proxy&meta_search=first_found&normal_kv_report=1&sgkps=" + ToString(messages[0].MutableDocument()->GetKeyPrefix()), results, ctx);

    if (results.size() != 1) {
        PrintInfoServer();
        TEST_FAILED("Test failed: " + ToString(results.size()));
    }

    CHECK_TEST_EQ(resultProps[0]->find("_Body")->second, "Body");
    CHECK_TEST_EQ(resultProps[0]->find("some_property")->second, "test");

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
    return true;
}
};

START_TEST_DEFINE(TestFullArchiveGtaAllDocInfos)
bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 1, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    messages[0].MutableDocument()->SetKeyPrefix(messages[0].MutableDocument()->GetKeyPrefix());
    messages[0].MutableDocument()->SetUrl("some_key");

    auto prop = messages[0].MutableDocument()->AddDocumentProperties();
    prop->SetName("some_property");
    prop->SetValue("test");

    ui64 ts = TInstant::Now().Seconds();
    messages[0].MutableDocument()->SetModificationTimestamp(ts);

    ui32 version = messages[0].MutableDocument()->GetVersion() + 1;
    messages[0].MutableDocument()->SetVersion(version);

    ui32 deadline = Seconds() / 60 + 10;
    messages[0].MutableDocument()->SetDeadlineMinutesUTC(deadline);

    IndexMessages(messages, DISK, 1);
    ReopenIndexers();

    TQuerySearchContext ctx;
    ctx.PrintResult = true;
    ctx.SourceSelector = new TDirectSourceSelector();
    TQuerySearchContext::TDocProperties resultProps;
    ctx.DocProperties = &resultProps;

    TVector<TDocSearchInfo> results;
    QuerySearch("some_key&sp_meta_search=proxy&meta_search=first_found&normal_kv_report=1&gta=_AllDocInfos&sgkps=" + ToString(messages[0].GetDocument().GetKeyPrefix()), results, ctx);

    if (results.size() != 1) {
        PrintInfoServer();
        TEST_FAILED("Test failed: " + ToString(results.size()));
    }

    CHECK_TEST_EQ(resultProps[0]->find("_Timestamp")->second, ToString(ts));
    CHECK_TEST_EQ(resultProps[0]->find("_Version")->second, ToString(version));
    CHECK_TEST_EQ(resultProps[0]->find("_Deadline")->second, ToString(deadline));
    CHECK_TEST_EQ(resultProps[0]->find("some_property")->second, "test");

    QuerySearch("some_key&component=DDK&sp_meta_search=proxy&meta_search=first_found&gta=_AllDocInfos&sgkps=" + ToString(messages[0].GetDocument().GetKeyPrefix()), results, ctx);

    if (results.size() != 1) {
        PrintInfoServer();
        TEST_FAILED("DDK Test failed: " + ToString(results.size()));
    }

    CHECK_TEST_EQ(resultProps[0]->find("_Timestamp")->second, ToString(ts));
    CHECK_TEST_EQ(resultProps[0]->find("_Version")->second, ToString(version));
    CHECK_TEST_EQ(resultProps[0]->find("_Deadline")->second, ToString(deadline));

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
    return true;
}
};


START_TEST_DEFINE(TestFullArchiveLockIndexFiles)
bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 100, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    IndexMessages(messages, DISK, 1);
    ReopenIndexers();

    TQuerySearchContext ctx;
    ctx.PrintResult = true;
    ctx.SourceSelector = new TDirectSourceSelector();

    for (auto& mes: messages) {
        TVector<TDocSearchInfo> results;
        QuerySearch(mes.GetDocument().GetUrl() + "&sp_meta_search=proxy&meta_search=first_found&normal_kv_report=1&sgkps=" + ToString(mes.GetDocument().GetKeyPrefix()), results, ctx);
        if (results.size() != 1) {
            PrintInfoServer();
            TEST_FAILED("Test failed: " + ToString(results.size()));
        }
    }

    Controller->RestartServer();
    TSet<TString> indexes = Controller->GetFinalIndexes();
    Controller->RestartServer();

    for (const auto& path : indexes) {
        CHECK_TEST_TRUE(IsIndexFileLocked(TFsPath(path) / "indexddk.rty"));
        CHECK_TEST_TRUE(IsIndexFileLocked(TFsPath(path) / "indexfullarc.full.fat"));
    }

    return true;
}

bool InitConfig() override {
    SetIndexerParams(DISK, 1000, 1);
    (*ConfigDiff)["IndexGenerator"] = FULL_ARCHIVE_COMPONENT_NAME;
    (*ConfigDiff)["Components"] = FULL_ARCHIVE_COMPONENT_NAME;
    (*ConfigDiff)["Indexer.Memory.Enabled"] = "false";
    (*ConfigDiff)["Searcher.SnippetsDeniedZones"] = "";
    (*ConfigDiff)["Searcher.LockIndexFiles"] = "true";
    (*ConfigDiff)["Searcher.TwoStepQuery"] = "false";
    (*SPConfigDiff)["Service.MetaSearch.TwoStepQuery"] = "false";
    return true;
}
};

START_TEST_DEFINE(TestProxyMetaTimeout)
bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 10, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    IndexMessages(messages, DISK, 1);
    ReopenIndexers();

    TQuerySearchContext ctx;
    ctx.PrintResult = true;
    ctx.SourceSelector = new TDirectSourceSelector();

    const TString key = messages[0].GetDocument().GetUrl();
    const TString kps = ToString(messages[0].GetDocument().GetKeyPrefix());
    TVector<TDocSearchInfo> results;
    ui16 code = QuerySearch(key + "&sp_meta_search=proxy&meta_search=first_found&normal_kv_report=1&timeout=1000&sleep=1000000&sgkps=" + kps, results, ctx);
    if (code != 502) {
        PrintInfoServer();
        TEST_FAILED("Test failed: expected code 502 but " + ToString(code) + " received");
    }

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
    return true;
}
};

START_TEST_DEFINE(TestProxyMetaUnavailableBackend)
bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 10, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    IndexMessages(messages, DISK, 1);
    ReopenIndexers();

    TQuerySearchContext ctx;
    ctx.PrintResult = true;
    ctx.SourceSelector = new TDirectSourceSelector();

    Controller->ProcessCommand("stop", TBackendProxy::TBackendSet(0));

    for (size_t i = 0; i < messages.size(); ++i) {
        const TString key = messages[0].GetDocument().GetUrl();
        const TString kps = ToString(messages[0].GetDocument().GetKeyPrefix());
        TVector<TDocSearchInfo> results;
        ui16 code = QuerySearch(key + "&sp_meta_search=proxy&meta_search=first_found&normal_kv_report=1&timeout=10000000&sgkps=" + kps, results, ctx);
        if (code != 200) {
            TEST_FAILED("Test failed: expected code 200 but " + ToString(code) + " received");
        }
    }

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
    (*SPConfigDiff)["Service.ProxyMeta.TasksCheckIntervalms"] = 10000000;
    return true;
}
};

START_TEST_DEFINE(TestProxyMetaMaxKeysPerRequest)

bool CheckMaxDocs(const TString query, const ui32 messagesCount, const TMaybe<ui32> maxDocsPerRequest) {

    if (maxDocsPerRequest.Defined()) {
        (*SPConfigDiff)["Service.ProxyMeta.MaxKeysPerRequest"] = ToString(maxDocsPerRequest.GetRef());
    } else {
        (*SPConfigDiff)["Service.ProxyMeta.MaxKeysPerRequest"] = "__remove__";
    }
    Controller->ApplyProxyConfigDiff(SPConfigDiff, "search", "SearchProxy");

    TString tassResult;
    ui64 prevTassValue = 0;
    Controller->ProcessQuery("/tass", &tassResult, "localhost", Controller->GetConfig().Controllers[0].Port, false);
    NOTICE_LOG << tassResult << Endl;
    TRTYTassParser::GetTassValue(tassResult, "backend-search-CTYPE-result-ok_dmmm", &prevTassValue);

    TVector<TDocSearchInfo> results;

    TQuerySearchContext ctx;
    ctx.PrintResult = true;
    ctx.SourceSelector = new TLevelSourceSelector(2);

    QuerySearch(query, results, ctx);
    CHECK_TEST_EQ(results.size(), messagesCount);

    Controller->ProcessQuery("/tass", &tassResult, "localhost", Controller->GetConfig().Controllers[0].Port, false);

    ui64 tassValue = 0;
    NOTICE_LOG << tassResult << Endl;
    if (!TRTYTassParser::GetTassValue(tassResult, "backend-search-CTYPE-result-ok_dmmm", &tassValue)) {
        ythrow (yexception() << "Failed to get old documents count from TUnistat data");
    }

    if (maxDocsPerRequest.Empty()) {
        CHECK_TEST_EQ((tassValue - prevTassValue), 1);
    } else {
        CHECK_TEST_EQ((tassValue - prevTassValue), ui64(std::ceil(double(messagesCount) / maxDocsPerRequest.GetRef())));
    }
    return true;
}

bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 6, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    IndexMessages(messages, DISK, 1);
    ReopenIndexers();

    TSet<TString> urls;
    for (const NRTYServer::TMessage& message: messages) {
        urls.insert(message.GetDocument().GetUrl());
    }

    const TString keys = JoinStrings(urls.cbegin(), urls.cend(), ",");
    const TString kps = GetAllKps(messages, "&sgkps=");
    const TString query = keys + "&sp_meta_search=multi_proxy&meta_search=first_found&normal_kv_report=1" + kps;

    CHECK_TEST_TRUE(CheckMaxDocs(query, messages.size(), Nothing()));
    for (ui32 i = 1; i < messages.size() + 1; ++i) {
        CHECK_TEST_TRUE(CheckMaxDocs(query, messages.size(), i));
    }

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
    return true;
}
};

START_TEST_DEFINE(TestFullArchiveUpsert)
bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 1, NRTYServer::TMessage::DEPRECATED__UPDATE_DOCUMENT, GetIsPrefixed());
    messages[0].MutableDocument()->SetUrl("some_key");

    TVector<NRTYServer::TMessage> updates = messages;

    messages[0].MutableDocument()->SetModificationTimestamp(1);
    auto prop = messages[0].MutableDocument()->AddDocumentProperties();
    prop->SetName("some_property");
    prop->SetValue("test");

    updates[0].MutableDocument()->SetModificationTimestamp(2);
    prop = updates[0].MutableDocument()->AddDocumentProperties();
    prop->SetName("some_property2");
    prop->SetValue("test2");
    prop = updates[0].MutableDocument()->AddDocumentProperties();
    prop->SetName("some_property");
    prop->SetValue("__delete__");

    IndexMessages(messages, REALTIME, 1);

    TQuerySearchContext ctx;
    ctx.PrintResult = true;
    ctx.SourceSelector = new TDirectSourceSelector();
    TQuerySearchContext::TDocProperties resultProps;
    ctx.DocProperties = &resultProps;

    TVector<TDocSearchInfo> results;
    QuerySearch("some_key&sp_meta_search=proxy&meta_search=first_found&normal_kv_report=1&gta=_AllDocInfos&sgkps=" + ToString(messages[0].GetDocument().GetKeyPrefix()), results, ctx);

    if (results.size() != 1) {
        PrintInfoServer();
        TEST_FAILED("Test failed: " + ToString(results.size()));
    }

    auto docProperties = resultProps[0];
    TString val1 = docProperties->find("_Timestamp")->second;
    CHECK_TEST_EQ(val1, ToString(1));
    TString val2 = docProperties->find("some_property")->second;
    CHECK_TEST_EQ(val2, "test");

    IndexMessages(updates, REALTIME, 1);
    QuerySearch("some_key&sp_meta_search=proxy&meta_search=first_found&normal_kv_report=1&gta=_AllDocInfos&sgkps=" + ToString(messages[0].GetDocument().GetKeyPrefix()), results, ctx);
    if (results.size() != 1) {
        PrintInfoServer();
        TEST_FAILED("Test failed: " + ToString(results.size()));
    }

    docProperties = resultProps[0];
    CHECK_TEST_EQ(docProperties->find("_Timestamp")->second, ToString(2));
    Y_ENSURE(docProperties->find("some_property") == docProperties->end(), "removed attr present");
    CHECK_TEST_EQ(docProperties->find("some_property2")->second, "test2");

    return true;
}

bool InitConfig() override {
    SetIndexerParams(DISK, 100, 1);
    (*ConfigDiff)["IndexGenerator"] = FULL_ARCHIVE_COMPONENT_NAME;
    (*ConfigDiff)["Components"] = FULL_ARCHIVE_COMPONENT_NAME;
    (*ConfigDiff)["Indexer.Common.StoreUpdateData"] = 1;
    (*ConfigDiff)["Indexer.Common.UseSlowUpdate"] = 1;
    (*ConfigDiff)["Indexer.Memory.Enabled"] = "true";
    (*ConfigDiff)["Searcher.SnippetsDeniedZones"] = "";
    (*ConfigDiff)["Searcher.TwoStepQuery"] = "false";
    (*SPConfigDiff)["Service.MetaSearch.TwoStepQuery"] = "false";
    return true;
}
};

START_TEST_DEFINE(TestFullArchiveOptimizeOnStart, TTestMarksPool::OneBackendOnly)

std::pair<ui32, ui32> GetIndexesInfo() {
    TSet<TString> indexes = Controller->GetFinalIndexes();
    Controller->ProcessCommand("stop");

    ui32 indexesC = 0;
    ui32 partsC = 0;
    for (const auto& path : indexes) {
        if (path.StartsWith("temp_"))
            continue;

        TFsPath fullArc(path + "/" + FULL_ARC_FILE_NAME_PREFIX + NRTYServer::NFullArchive::FullLayer);
        if (TArchiveOwner::Exists(fullArc)) {
            TArchiveOwner::TPtr archive = TArchiveOwner::Create(fullArc, NRTYArchive::TMultipartConfig());
            partsC += archive->GetPartsCount() - 1;
            indexesC++;
        }
    }

    Controller->ProcessCommand("restart");
    return std::make_pair(indexesC, partsC);
}

bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    TVector<NRTYServer::TMessage> messages1;

    {
        GenerateInput(messages, 1000, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
        for (ui32 i = 0; i < messages.size(); ++i) {
            messages[i].MutableDocument()->SetKeyPrefix(messages[0].MutableDocument()->GetKeyPrefix());
            if (i % 4 != 0) {
                messages1.push_back(messages[i]);
            }
        }
    }

    IndexMessages(messages, DISK, 1);
    ReopenIndexers();
    IndexMessages(messages1, DISK, 1);
    ReopenIndexers();

    auto stat1 = GetIndexesInfo();

    Controller->ProcessCommand("create_merger_tasks");
    Controller->ProcessCommand("do_all_merger_tasks");

    auto stat2 = GetIndexesInfo();

    INFO_LOG << stat1.first << "/" << stat1.second << Endl;
    INFO_LOG << stat2.first << "/" << stat2.second << Endl;

    CHECK_TEST_EQ(stat2.first, 1);
    CHECK_TEST_LESS(stat2.second, stat1.second);

    (*ConfigDiff)["ComponentsConfig." FULL_ARCHIVE_COMPONENT_NAME ".Layers.full.PartSizeDeviation"] = "0.1";
    (*ConfigDiff)["ComponentsConfig." FULL_ARCHIVE_COMPONENT_NAME ".Layers.full.PartSizeLimit"] = "1000000000";
    Controller->ApplyConfigDiff(ConfigDiff);

    auto stat3 = GetIndexesInfo();
    INFO_LOG << stat3.first << "/" << stat3.second << Endl;
    CHECK_TEST_EQ(stat3.first, 1);
    CHECK_TEST_EQ(stat3.second, 1);

    return true;
}

bool InitConfig() override {
    SetIndexerParams(DISK, 100, 1);
    SetMergerParams(true, 1, 1, mcpTIME, Max<i32>());
    (*ConfigDiff)["IndexGenerator"] = FULL_ARCHIVE_COMPONENT_NAME;
    (*ConfigDiff)["Components"] = FULL_ARCHIVE_COMPONENT_NAME;
    (*ConfigDiff)["Searcher.TwoStepQuery"] = "false";
    (*ConfigDiff)["Searcher.SnippetsDeniedZones"] = "";
    return true;
}
};
