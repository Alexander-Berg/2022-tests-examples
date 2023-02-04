#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/rtyserver/components/ddk/ddk_fields.h>
#include <saas/rtyserver/components/ddk/ddk_globals.h>
#include <saas/rtyserver/components/erf/erf_disk.h>
#include <saas/rtyserver_test/util/tass_parsers.h>
#include <util/system/file.h>

START_TEST_DEFINE(TestADD_DOCUMENT_MEMORY)
bool Run() override {
    const int CountMessages = 1;
    TVector<NRTYServer::TMessage> messagesForMemory;
    GenerateInput(messagesForMemory, CountMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());

    IndexMessages(messagesForMemory, REALTIME, 10);

    ReopenIndexers();

    // Variant - count may be 2. For Linux.
    CheckSearchResults(messagesForMemory, TSet<std::pair<ui64, TString> >(), 1, 2);

    return true;
}
public:
    bool InitConfig() override {
        SetIndexerParams(REALTIME, 2000, 1);
        return true;
    }
};

START_TEST_DEFINE(TestADD_DOCUMENT_DISK)
bool Run() override {
    const int CountMessages = 10;
    TVector<NRTYServer::TMessage> messagesForDisk;
    GenerateInput(messagesForDisk, CountMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());

    IndexMessages(messagesForDisk, DISK, 3);
    ReopenIndexers();

    // TFinalIndexNormalizer::RemoveDoubles works there
    CheckSearchResults(messagesForDisk, TSet<std::pair<ui64, TString> >(), 1);
    return true;
}
public:
    bool InitConfig() override {
        SetIndexerParams(REALTIME, 2000, 1);
        return true;
    }
};

START_TEST_DEFINE(TestADD_DOCUMENT_REMOVE_FROM_MEMORY)
bool Run() override {
    const int CountMessages = GetMaxDocuments() - 1;
    TVector<NRTYServer::TMessage> messagesForMemory, messagesForDisk;
    GenerateInput(messagesForMemory, CountMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());

    IndexMessages(messagesForMemory, REALTIME, 1);
    sleep(2);
    // Variant - count may be 2. For Linux.
    CheckSearchResults(messagesForMemory);
    TSet<std::pair<ui64, TString> > deleted;
    DeleteSomeMessages(messagesForMemory, deleted, REALTIME);
    CheckSearchResults(messagesForMemory, deleted, 1);
    return true;
}
public:
    bool InitConfig() override {
        SetIndexerParams(REALTIME, 100, 2);
        SetIndexerParams(DISK, 10);
        return true;
    }
};

START_TEST_DEFINE(TestADD_DOCUMENT_CLOSE_MEMORY)
bool Run() override {
    const size_t kfPrefix = GetShardsNumber();
    const size_t CountMessages = (GetMaxDocuments() - 1) * kfPrefix;
    TVector<NRTYServer::TMessage> messagesForMemory, messagesForMemory1;
    GenerateInput(messagesForMemory, CountMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    GenerateInput(messagesForMemory1, kfPrefix, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());

    IndexMessages(messagesForMemory, REALTIME, 1);
    sleep(2);
    CheckSearchResults(messagesForMemory);
    TJsonPtr serverInfo(Controller->GetServerInfo());
    NJson::TJsonValue* info = &(*serverInfo)[0];
    ui32 memSearchersCount = (*info)["memory_searchers_count"].GetUInteger();
    if (memSearchersCount != kfPrefix)
        ythrow yexception() << "Incorrect memory indexers count case A";
    IndexMessages(messagesForMemory1, REALTIME, 1);

    TInstant timeStart = Now();
    while (Now() - timeStart < TDuration::Minutes(10)) {
        serverInfo = Controller->GetServerInfo();
        info = &(*serverInfo)[0];
        if ((*info)["docs_in_disk_indexers"].GetInteger() == 0 &&
            (*info)["memory_searchers_count"].GetInteger() == (i64)kfPrefix &&
            (*info)["docs_in_memory_indexes"].GetInteger() == 0
            )
            break;
        sleep(10);
    }
    PrintInfoServer();
    CHECK_TEST_EQ((*info)["docs_in_disk_indexers"].GetInteger(), 0);
    CHECK_TEST_EQ((*info)["docs_in_memory_indexes"].GetInteger(), 0);
    CHECK_TEST_EQ((*info)["memory_searchers_count"].GetInteger(), (i64)kfPrefix);
    CheckSearchResults(messagesForMemory);
    CheckSearchResults(messagesForMemory1);
    return true;
}
public:
    bool InitConfig() override {
        SetIndexerParams(REALTIME, 1, 1);
        SetIndexerParams(DISK, 4, 1);
        return true;
    }
};

START_TEST_DEFINE(TestNormalizeDDK)
void Test(bool switchOffFullArc) {
    const int CountMessages = 10;
    TVector<NRTYServer::TMessage> messagesForMemory;
    GenerateInput(messagesForMemory, CountMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());

    IndexMessages(messagesForMemory, DISK, 10);
    TFsPath id(GetIndexDir());
    Controller->ProcessCommand("stop");
    TVector<TFsPath> indexes;
    id.List(indexes);
    for (TVector<TFsPath>::const_iterator i = indexes.begin(); i != indexes.end(); ++i) {
        if (!i->IsDirectory())
            continue;
        if (!i->GetName().StartsWith("index_"))
            continue;
        ui32 dc = TFile((*i / "indexfrq").GetPath(), RdOnly).GetLength() / sizeof(i16);
        DEBUG_LOG << *i << " " << dc;
        TRTYErfDiskManager::TCreationContext cc(TPathName{i->GetPath()}, "indexddk.rty", &GetDDKFields());
        cc.BlockCount = dc;
        TRTYErfDiskManager erf(cc, DDK_COMPONENT_NAME);
        erf.Open();
        for (ui32 docid = 0; docid < dc; ++docid) {
            TFactorStorage erfData(NRTYServer::NDDK::KeysCount);
            erf.ReadRaw(erfData, docid);
            memset(erfData.factors + NRTYServer::NDDK::HashIndex, 0, TDocSearchInfo::THash::Size);
            erf.Write(erfData, docid);
        }
        erf.Close();
    }
    if (switchOffFullArc) {
        SetEnabledRepair(false);
        (*ConfigDiff)["Indexer.Common.UseSlowUpdate"] = 0;
        (*ConfigDiff)["Components"] = "INDEX,MAKEUP,DDK,FASTARC";
        ApplyConfig();
    }
    Controller->RestartServer();
    // Variant - count may be 2. For Linux.
    CheckSearchResults(messagesForMemory, TSet<std::pair<ui64, TString> >(), 1, 2);
}

bool Run() override {
    Test(false);
    Test(true);
    return true;
}
public:
    bool InitConfig() override {
        SetIndexerParams(REALTIME, 2000, 1);
        return true;
    }
};


START_TEST_DEFINE(TestNormalizeFRQ)
bool Run() override {

    const ui32 CountMessages = 20;
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, CountMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    IndexMessages(messages, DISK, 1);
    Controller->RestartServer();
    TSet<std::pair<ui64, TString>> deleted;
    DeleteSomeMessages(messages, deleted, DISK, 2);
    CheckSearchResults(messages, deleted);

    const TFsPath id(GetIndexDir());
    Controller->ProcessCommand("stop");

    TVector<TFsPath> indexes;
    id.List(indexes);
    for (auto i = indexes.begin(); i != indexes.end(); ++i) {
        if (!i->IsDirectory())
            continue;
        if (!i->GetName().StartsWith("index_"))
            continue;
        const TFsPath frqPath(*i / "indexfrq");
        const ui32 dc = TFile(frqPath.GetPath(), RdOnly).GetLength() / sizeof(i16);
        DEBUG_LOG << "FRQ: " << *i << " " << dc << Endl;

        TFileHandle frqFile(frqPath.c_str(), OpenAlways);
        TVector<i16> data(dc, 0);
        frqFile.Write(&data[0], data.size() * sizeof(data[0]));
    }
    Controller->RestartServer();
    CheckSearchResults(messages, deleted);
    return true;
}

public:
    bool InitConfig() override {
        SetIndexerParams(REALTIME, 2000, 1);
        return true;
    }
};


START_TEST_DEFINE(TestIndexerDocsDbgLimit)
struct TIndexerLimitReached: public IMessageProcessor {
    TAutoEvent DbgMessage;

    TIndexerLimitReached() {
        RegisterGlobalMessageProcessor(this);
    }
    ~TIndexerLimitReached() {
        UnregisterGlobalMessageProcessor(this);
    }
    TString Name() const override {
        return "IndexerLimitReached";
    }
    bool Process(IMessage* message) override {
        if (auto m = message->As<TUniversalAsyncMessage>()) {
            if (m->GetType() == "DbgMaxDocumentsTotal") {
                DbgMessage.Signal();
            }
            return true;
        }
        return false;
    }
};

bool Run() override {
    if (GetIsPrefixed())
        return true;

    TIndexerClient::TContext ctx;
    ctx.DoWaitIndexing = true;
    ctx.IgnoreErrors = true;

    TIndexerLimitReached hook;
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 5, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    IndexMessages(messages, DISK, ctx);
    GenerateInput(messages, 5, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    IndexMessages(messages, DISK, ctx);
    CHECK_TEST_TRUE(hook.DbgMessage.WaitT(TDuration::Zero())); // should be instant if not under GDB
    CHECK_TEST_TRUE(CheckTass(3,10));
    ReopenIndexers();
    CHECK_TEST_TRUE(CheckTass(5,12)); // REOPEN_INDEXES should not be filtered, so +2 (two because async)

    //ReopenIndexers should not reset the limit
    GenerateInput(messages, 10, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    IndexMessages(messages, REALTIME, ctx);
    ReopenIndexers();
    CHECK_TEST_TRUE(CheckTass(7,24));

    TVector<TDocSearchInfo> results;
    QuerySearch("url:\"*\"", results);
    if (results.size() != 3)
        ythrow yexception() << "invalid search results count " << results.size() << " != 3";
    return true;
}

bool CheckTass(ui64 nAllowedMsg, ui64 nTotalMsg) {
    ui64 indexOk = 0;
    ui64 indexDeprecated = 0;
    ui64 indexErrors = 0;
    TString tassResult;
    Controller->ProcessQuery("/tass", &tassResult, "localhost", Controller->GetConfig().Controllers[0].Port, false);
    TRTYTassParser::GetTassValue(tassResult, "backend-index-CTYPE-200_dmmm", &indexOk);
    TRTYTassParser::GetTassValue(tassResult, "backend-index-CTYPE-3xx_dmmm", &indexDeprecated);
    TRTYTassParser::GetTassValue(tassResult, "backend-index-CTYPE-5xx_dmmm", &indexErrors);
    CHECK_TEST_EQ(indexOk, nAllowedMsg);
    CHECK_TEST_GREATEREQ(indexDeprecated, nTotalMsg - nAllowedMsg);
    CHECK_TEST_EQ(indexErrors, 0);
    return true;
}

bool InitConfig() override {
    (*ConfigDiff)["Indexer.Memory.Enabled"] = "false";
    (*ConfigDiff)["Indexer.Disk.DbgMaxDocumentsTotal"] = "3";
    (*ConfigDiff)["Indexer.Disk.Threads"] = "1";
    return true;
}
};
