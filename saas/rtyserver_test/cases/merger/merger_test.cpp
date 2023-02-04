#include "merger_test.h"

#include <saas/rtyserver/components/fullarchive/globals.h>
#include <saas/rtyserver/indexer_core/index_metadata_processor.h>
#include <saas/rtyserver/common/common_messages.h>
#include <saas/rtyserver/common/sharding.h>
#include <saas/util/json/json.h>
#include <saas/rtyserver_test/util/doc_info.h>
#include <kernel/multipart_archive/multipart.h>
#include <kernel/multipart_archive/owner.h>

#include <kernel/sent_lens/sent_lens.h>
#include <util/folder/filelist.h>
#include <util/system/event.h>
#include <util/system/fs.h>
#include <saas/rtyserver_test/util/tass_parsers.h>

bool TMergerTest::TestMerger(NRTYServer::TMessage::TMessageType messageType) {
    TRY
        ui32 shardsNumber = GetShardsNumber();
        ui32 messagesCount = 8u * GetMaxDocuments() * shardsNumber;
        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, messagesCount, messageType, GetIsPrefixed());
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();
        CheckMergerResult();
        CheckSearchResults(messages);
        return true;
    CATCH("while TestMerger executing");
    return false;
}

bool TMergerTest::InitConfig() {
    SetMergerParams(true, 1, -1, mcpNONE);
    SetIndexerParams(DISK, 100, 4);
    SetIndexerParams(REALTIME, 100);
    return true;
}

TMaybeFail<ui64> TMergerTest::GetMaxDeadlineRemovedStats() {
    ui64 nRemoved = 0;
    TString tassResults;
    Controller->ProcessQuery("/tass", &tassResults, "localhost", Controller->GetConfig().Controllers[0].Port, false);
    if (!TRTYTassParser::GetTassValue(tassResults, "surplus-doc-count_dvvm", &nRemoved))
        return TMaybeFail<ui64>();

    return nRemoved;
}

SERVICE_TEST_RTYSERVER_DEFINE_PARENT(TestMergerRemove, TMergerTest)
void TestRemove(bool all) {
    TVector<NRTYServer::TMessage> messages;
    ui32 shardsNumber = GetShardsNumber();
    ui32 messagesCount = 10u * GetMaxDocuments() * shardsNumber;
    GenerateInput(messages, messagesCount, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    IndexMessages(messages, DISK, 1);
    if (!all) {
        TVector<NRTYServer::TMessage> messagesAdd;
        GenerateInput(messagesAdd, shardsNumber, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
        IndexMessages(messagesAdd, DISK, 1);
    }
    ReopenIndexers();
    TSet<std::pair<ui64, TString> > deleted;
    DeleteSomeMessages(messages, deleted, DISK, 1);
    ReopenIndexers();
    CheckMergerResult();
}

bool InitConfig() override {
    SetIndexerParams(DISK, 100, 1);
    SetIndexerParams(REALTIME, 100);
    SetMergerParams(true, 2, -1, mcpNONE, -1, GetMaxDocuments());
    return true;
}
};

START_TEST_DEFINE_PARENT(TestMergerREMOVE, TestMergerRemove, TTestMarksPool::OneBackendOnly)
bool Run() override {
    TestRemove(false);
    return true;
}
};

START_TEST_DEFINE_PARENT(TestMergerSameUrls, TestMergerRemove, TTestMarksPool::OneBackendOnly)
bool Run() override {
    if (!GetIsPrefixed())
        return true;
    TVector<NRTYServer::TMessage> messages;
    TVector<NRTYServer::TMessage> messages1;
    GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    GenerateInput(messages1, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());

    messages[0].MutableDocument()->SetKeyPrefix(1);
    messages1[0].MutableDocument()->SetKeyPrefix(2);
    messages[0].MutableDocument()->SetUrl("aaa");
    messages1[0].MutableDocument()->SetUrl("aaa");

    IndexMessages(messages, DISK, 1);
    ReopenIndexers();

    IndexMessages(messages1, DISK, 1);
    ReopenIndexers();

    CheckMergerResult();

    CheckSearchResults(messages);
    CheckSearchResults(messages1);

    messages[0].SetMessageType(NRTYServer::TMessage::MODIFY_DOCUMENT);
    messages[0].MutableDocument()->SetBody("aaa");
    messages1[0].SetMessageType(NRTYServer::TMessage::MODIFY_DOCUMENT);
    messages1[0].MutableDocument()->SetBody("bbb");

    IndexMessages(messages, REALTIME, 1);
    CheckSearchResults(messages);
    CheckSearchResults(messages1);

    IndexMessages(messages1, REALTIME, 1);
    CheckSearchResults(messages);
    CheckSearchResults(messages1);

    return true;
}
bool InitConfig() override {
    SetIndexerParams(DISK, 100, 1);
    SetIndexerParams(REALTIME, 100);
    (*ConfigDiff)["ShardsNumber"] = 1;
    SetMergerParams(true, 1, -1, mcpNEWINDEX, -1, GetMaxDocuments());
    return true;
}
};

START_TEST_DEFINE_PARENT(TestMergerREMOVE_ALL, TestMergerRemove, TTestMarksPool::OneBackendOnly)
bool Run() override {
    TestRemove(true);
    return true;
}
};

START_TEST_DEFINE_PARENT(TestMergerADD, TMergerTest, TTestMarksPool::OneBackendOnly)
    bool Run() override {
        return TestMerger(NRTYServer::TMessage::ADD_DOCUMENT);
    }
};

START_TEST_DEFINE_PARENT(TestMergerMODIFY, TMergerTest, TTestMarksPool::OneBackendOnly)
    bool Run() override {
        return TestMerger(NRTYServer::TMessage::MODIFY_DOCUMENT);
    }
};

START_TEST_DEFINE(TestMergerPolicyContinuous)
class TAddTaskIndexer: public IMessageProcessor {
public:
    TAddTaskIndexer(TTestMergerPolicyContinuousCaseClass& parent)
        : Parent(parent)
        , AddTaskEvent(TSystemEvent::rAuto)
        , IndexRemainder(true)
        , Successful(false)
    {
        RegisterGlobalMessageProcessor(this);
    }

    ~TAddTaskIndexer() {
        UnregisterGlobalMessageProcessor(this);
    }
    bool Process(IMessage* incomingMessage) override {
        if (TUniversalAsyncMessage* message = dynamic_cast<TUniversalAsyncMessage*>(incomingMessage)) {
            if (message->GetType() == "mergerAnalyzerAddTask") {
                if (IndexRemainder) {
                    DEBUG_LOG << "IndexRemainder" << Endl;
                    Parent.IndexRemainder();
                    IndexRemainder = false;
                    DEBUG_LOG << "IndexRemainder OK" << Endl;
                } else {
                    DEBUG_LOG << "Second..." << Endl;
                    Successful = true;
                    AddTaskEvent.Signal();
                    DEBUG_LOG << "Second ... OK" << Endl;
                }
            }
            return true;
        }
        return false;
    }

    TString Name() const override {
        return "add_task_indexer";
    }

    void Wait(TDuration timeout) {
        AddTaskEvent.WaitT(timeout);
    }
private:
    TTestMergerPolicyContinuousCaseClass& Parent;
    TSystemEvent AddTaskEvent;
    bool IndexRemainder;
public:
    bool Successful;
};

bool InitConfig() override {
    (*ConfigDiff)["ShardsNumber"] = 1;
    SetMergerParams(true, 1, 1, mcpCONTINUOUS);
    SetIndexerParams(DISK, 100, 1);
    SetIndexerParams(REALTIME, 100);
    return true;
}

bool Run() override {
    if (GetIsPrefixed())
        return true;
    TRY
        // Send 2x docs into disk indexer to trigger merge
        ui32 shardsNumber = GetShardsNumber();
        ui32 messagesCount = 2u * GetMaxDocuments() * shardsNumber;
        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, messagesCount, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
        IndexMessages(messages, DISK, 1, 0, false);
        CheckMergerResult();
        WaitIndexersClose();
        Controller->ProcessCommand("do_all_merger_tasks");
        return true;
    CATCH("while TestMergerPolicyContinuous executing");
        return false;
}

void CheckMergerResult() {
    // AddTaskIndexer sends in remainder 0.5x docs upon first merge AddTask event,
    // unblocks when it sees the second merge AddTask event,
    // proving that the temp index is forced closed by mcpCONTINUOUS policy.
    TAddTaskIndexer addTaskIndexer(*this);
    addTaskIndexer.Wait(TDuration::Seconds(60)); // just in case something breaks
    if (!addTaskIndexer.Successful) {
        ythrow yexception() << "mcpCONTINUOUS sanity check timed out or failed";
    }
}
public:
    void IndexRemainder() {
        // Send 0.5x docs into disk indexer
        ui32 shardsNumber = GetShardsNumber();
        ui32 messagesCount = GetMaxDocuments() * shardsNumber / 2;
        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, messagesCount, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
        IndexMessages(messages, DISK, 1);
    }
};


START_TEST_DEFINE(TestMergerPolicyNewIndex)
    class TAddTaskSubscriber: public IMessageProcessor {
    public:
        TAddTaskSubscriber()
        {
            RegisterGlobalMessageProcessor(this);
        }
        ~TAddTaskSubscriber() {
            UnregisterGlobalMessageProcessor(this);
        }

        bool Process(IMessage* incomingMessage) override {
            if (incomingMessage->As<TMessageRegisterIndex>()) {
                StartTime = Now();
                return false;
            }
            if (TUniversalAsyncMessage* message = dynamic_cast<TUniversalAsyncMessage*>(incomingMessage)) {
                if (message->GetType() == "mergerAnalyzerAddTask" && StartTime) {
                    AddTaskEvent.Signal();
                    EventTime = Now();
                    return true;
                }
            }
            return false;
        }

        TString Name() const override {
            return "add_task_subscriber";
        }

        TDuration Wait(TDuration timeout) {
            bool waitOk = AddTaskEvent.WaitT(timeout);
            if (waitOk && StartTime)
                return EventTime - StartTime;
            return TDuration::Max();
        }
    private:
        TManualEvent AddTaskEvent;
        TInstant StartTime;
        TInstant EventTime;
    };

    bool Run() override {
        if (GetIsPrefixed())
            return true;
        TRY
            ui32 messagesCount = GetMaxDocuments() * GetShardsNumber() * 3 / 2;
            TVector<NRTYServer::TMessage> messages;
            GenerateInput(messages, messagesCount, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
            IndexMessages(messages, DISK, 1, 0, false);

            TAddTaskSubscriber waiter;
            ReopenIndexers();
            TDuration dur = waiter.Wait(TDuration::Seconds(13));
            CHECK_TEST_LESS(dur, TDuration::Seconds(13));
            CHECK_TEST_LESSEQ(TDuration::Seconds(8), dur);

            return true;
        CATCH("while TestMergerPolicyNewIndex executing");
        return false;
    }

    bool InitConfig() override {
        (*ConfigDiff)["ShardsNumber"] = 1;
        SetMergerParams(true, 1, 1, mcpNEWINDEX);
        (*ConfigDiff)["Merger.NewIndexDefermentSec"] = 10;
        SetIndexerParams(DISK, 100, 1);
        return true;
    }
};

START_TEST_DEFINE(TestMergerRemoveIndex)
class TIndexRemover: public IMessageProcessor {
public:
    TIndexRemover(TTestMergerRemoveIndexCaseClass& parent)
        : DeleteSuccessful(false)
        , Parent(parent)
        , AllDeletedEvent(TSystemEvent::rAuto)
    {
        RegisterGlobalMessageProcessor(this);
    }

    ~TIndexRemover() {
        UnregisterGlobalMessageProcessor(this);
    }
    bool Process(IMessage* incomingMessage) override {
        if (TUniversalAsyncMessage* message = dynamic_cast<TUniversalAsyncMessage*>(incomingMessage)) {
            if (message->GetType() == "mergerDeletePhase") {
                DeleteSuccessful = Parent.DeleteAll();
                AllDeletedEvent.Signal();
            }
            return true;
        }
        return false;
    }

    TString Name() const override {
        return "index_remover";
    }

    void Wait() {
        AllDeletedEvent.WaitI();
    }
public:
    bool DeleteSuccessful;
private:
    TTestMergerRemoveIndexCaseClass& Parent;
    TSystemEvent AllDeletedEvent;
};

bool InitConfig() override {
    SetMergerParams(true, 1, -1, mcpNONE, 50);
    SetIndexerParams(DISK, 100, 1);
    SetIndexerParams(REALTIME, 100);
    return true;
}

bool Run() override {
    TRY
        ui32 shardsNumber = GetShardsNumber();
        ui32 messagesCount = 8u * GetMaxDocuments() * shardsNumber;
        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, messagesCount, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();
        CheckMergerResult();
        return true;
    CATCH("while TestMerger executing");
        return false;
}

void CheckMergerResult() {
    TIndexRemover indexRemover(*this);
    Controller->ProcessCommand("create_merger_tasks");
    Controller->ProcessCommand("do_all_merger_tasks");
    indexRemover.Wait();
    if (!indexRemover.DeleteSuccessful)
        ythrow yexception() << "delete all has been unsuccessful";
}
public:
    bool DeleteAll() {
        NRTYServer::TReply reply = DeleteSpecial();
        return reply.GetStatus() == 0;
    }
};

START_TEST_DEFINE(TestFastArchiveMerger)
TDocSearchInfo GetDocSearchInfo(const TString& url, const ui64 kps) {
    TVector<TDocSearchInfo> results;
    const TString kpsString = kps ? "&kps=" + ToString(kps) : "";
    QuerySearch("url:\"" + url + "\"" + kpsString, results);
    if (results.size() != 1)
        ythrow yexception() << "url " << url << " is not unique";
    return results[0];
}
bool Run() override {
    int count = 10;
    const TString& propName = "muse";
    TVector<NRTYServer::TMessage> messages1;
    {
        GenerateInput(messages1, count, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
        for (int i = 0; i < count; ++i) {
            auto prop = messages1[i].MutableDocument()->AddDocumentProperties();
            prop->SetName(propName);
            prop->SetValue("first");
        }
        IndexMessages(messages1, REALTIME, 1);
    }
    (*ConfigDiff)["ComponentsConfig.FASTARC.FastProperties"] = propName;
    ApplyConfig();
    Controller->RestartServer();

    TVector<NRTYServer::TMessage> messages2;
    {
        GenerateInput(messages2, count, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
        for (int i = 0; i < count; ++i) {
            auto prop = messages2[i].MutableDocument()->AddDocumentProperties();
            prop->SetName(propName);
            prop->SetValue("second");
        }
        IndexMessages(messages2, REALTIME, 1);
    }

    ReopenIndexers();
    Controller->ProcessCommand("create_merger_tasks");
    Controller->ProcessCommand("do_all_merger_tasks");

    Copy(messages2.begin(), messages2.end(), inserter(messages1, messages1.end()));
    for (const auto& message: messages1) {
        const auto& doc = message.GetDocument();
        const TDocSearchInfo& dsi = GetDocSearchInfo(doc.GetUrl(), doc.GetKeyPrefix());
        TJsonPtr jsonDocInfoPtr = Controller->GetDocInfo(dsi.GetSearcherId(), dsi.GetDocId());
        DEBUG_LOG << NUtil::JsonToString(*jsonDocInfoPtr) << Endl;
        TDocInfo di(*jsonDocInfoPtr);
        TString propValue;
        for (ui32 i = 0; i < doc.DocumentPropertiesSize(); ++i) {
            if (doc.GetDocumentProperties(i).GetName() == propName) {
                CHECK_TEST_TRUE(!propValue);
                propValue = doc.GetDocumentProperties(i).GetValue();
            }
        }
        CHECK_TEST_EQ(di.GetFastArchiveInfo()[propName][0], propValue);
    }

    return true;
}

bool InitConfig() override {
    SetMergerParams(true, 1, -1, mcpNONE, 50);
    SetIndexerParams(DISK, 100, 1);
    SetIndexerParams(REALTIME, 100);
    return true;
}
};

START_TEST_DEFINE_PARENT(TestFullArchiveMerger, TMergerTest, TTestMarksPool::OneBackendOnly)
bool Run() override {
    ui32 shardsNumber = GetShardsNumber();
    ui32 messagesCount = 2u * GetMaxDocuments() * shardsNumber;
    TVector<NRTYServer::TMessage> messagesAll;
    TSet<std::pair<ui64, TString> > deleted;
    for (int i = 0; i < 10; ++i) {
        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, messagesCount, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
        messagesAll.insert(messagesAll.end(), messages.begin(), messages.end());
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();
        DeleteSomeMessages(messages, deleted, DISK, 2);
        CheckMergerResult();
    }
    TSet<TString> indexes = Controller->GetFinalIndexes();
    Controller->ProcessCommand("stop");
    for (const auto& path : indexes) {
        TFsPath final(path);
        TFsPath temp(final.Parent() / ("temp_" + final.GetName().substr(strlen("index"))));
        temp.MkDirs();
        TString archName = FULL_ARC_FILE_NAME_PREFIX + NRTYServer::NFullArchive::FullLayer;
        NRTYArchive::HardLinkOrCopy(final / archName, temp / archName);
        TFile normalIndex(temp / "normal_index", WrOnly | OpenAlways);
        TIndexMetadataProcessor meta(temp);
        *meta = *TIndexMetadataProcessor(final);
        meta->MutableFullArcHeader()->ClearWritedLayers();
        meta->MutableFullArcHeader()->AddWritedLayers(NRTYServer::NFullArchive::FullLayer);
        final.ForceDelete();
    }
    Controller->RestartServer();
    Controller->WaitIsRepairing();
    CheckSearchResults(messagesAll, deleted);
    return true;
}

bool InitConfig() override {
    SetIndexerParams(DISK, 10, 1);
    SetIndexerParams(REALTIME, 10);
    SetMergerParams(true, 2, -1, mcpNONE, -1, GetMaxDocuments());
    SetEnabledRepair();
    return true;
}
};

START_TEST_DEFINE_PARENT(TestMergeSearchArchive, TMergerTest)
bool Check(const TString& kps) {
    TVector<TDocSearchInfo> results;
    QuerySearch("url:\"*\"" + kps, results);
    CHECK_TEST_TRUE(results.size());
    for (const auto& res: results) {
        TVector<TDocSearchInfo> res1;
        QuerySearch("url:\"" + res.GetUrl() + "\"&kps=" + ToString(res.GetKeyPrefix()), res1);
        CHECK_TEST_EQ(res1.size(), 1);
        CHECK_TEST_EQ(res1[0].GetDocId(), res.GetDocId());
    }
    return true;
}

bool Run() override {
    ui32 shardsNumber = GetShardsNumber();
    ui32 messagesCount = 2u * GetMaxDocuments() * shardsNumber;
    TVector<NRTYServer::TMessage> messagesAll;
    TSet<std::pair<ui64, TString> > deleted;
    for (int i = 0; i < 10; ++i) {
        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, messagesCount, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
        messagesAll.insert(messagesAll.end(), messages.begin(), messages.end());
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();
    }
    TString kps = GetAllKps(messagesAll);
    CHECK_TEST_TRUE(Check(kps));
    CheckMergerResult();
    CHECK_TEST_TRUE(Check(kps));
    return true;
}

bool InitConfig() override {
    SetIndexerParams(DISK, 10, 1);
    SetIndexerParams(REALTIME, 10);
    SetMergerParams(true, 2, -1, mcpNONE, -1, GetMaxDocuments());
    SetEnabledRepair();
    return true;
}
};

SERVICE_TEST_RTYSERVER_DEFINE(TTestSentIndexMergeBase)
protected:
    struct TSentStats {
        void operator +=(const TSentStats& s) {
            DocsCount += s.DocsCount;
            SentCount += s.SentCount;
            WordCount += s.WordCount;
        }

        static TSentStats ReadFromFile(const TString& fileName) {
            TSentenceLengthsReader reader(fileName);
            TSentStats stats;
            stats.DocsCount = reader.GetSize();
            for (size_t i = 0; i < reader.GetSize(); i++) {
                TSentenceLengths lens;
                reader.Get(i, &lens);
                stats.SentCount += lens.size();
                for (size_t j = 0; j < lens.size(); j++)
                    stats.WordCount += lens[j];
            }
            return stats;
        }

        size_t DocsCount = 0;
        size_t SentCount = 0;
        size_t WordCount = 0;
    };

    bool DoRun(const TVector<NRTYServer::TMessage>& messages1, const TVector<NRTYServer::TMessage>& messages2, const TVector<TString>& queries, bool checkStats) {
        IndexMessages(messages1, DISK, 1, 0, false);
        ReopenIndexers();

        IndexMessages(messages2, DISK, 1, 0, false);
        ReopenIndexers();

        TSentStats statsBeforeMerge;
        TSet<TString> indices = Controller->GetFinalIndexes();
        for (const auto& path : indices) {
            statsBeforeMerge += TSentStats::ReadFromFile(TFsPath(path) / "indexsent");
        }

        Controller->ProcessCommand("restart");
        Controller->ProcessCommand("create_merger_tasks");
        Controller->ProcessCommand("do_all_merger_tasks");
        ReopenIndexers();

        CheckSearchResults(messages1);
        CheckSearchResults(messages2);

        // search test is important, it may crash on corrupted indexsent: SAAS-4771
        TVector<TDocSearchInfo> results;
        for (const auto& query : queries) {
            QuerySearch(query, results);
            CHECK_TEST_GREATER(results.size(), 0);
        }

        indices = Controller->GetFinalIndexes();
        CHECK_TEST_EQ(indices.size(), 1);
        TSentStats statsAfterMerge = TSentStats::ReadFromFile(TFsPath(*indices.cbegin()) / "indexsent");

        if (checkStats) {
            CHECK_TEST_EQ(statsBeforeMerge.DocsCount, statsAfterMerge.DocsCount);
            CHECK_TEST_EQ(statsBeforeMerge.SentCount, statsAfterMerge.SentCount);
            CHECK_TEST_LESS(statsBeforeMerge.WordCount, statsAfterMerge.WordCount); // we don't know the length of the 0-sentence (URL) before the merge
        }

        return true;
    }
};

START_TEST_DEFINE_PARENT(TestSentIndexMerge, TTestSentIndexMergeBase)
bool InitConfig() final {
    SetIndexerParams(ALL, 600, 1);
    SetMergerParams(true, 1, -1, mcpNONE);
    return true;
}

bool Run() override {
    if (GetIsPrefixed())
        return true;

    TVector<NRTYServer::TMessage> messages1;
    GenerateInput(messages1, 2, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    messages1[0].MutableDocument()->SetBody("this is a sample sentence<hr>this is another one<hr>and one more");
    messages1[1].MutableDocument()->SetBody("qq ww ee rr tt yy<hr>zz xx cc vv bb nn mm aaaaaa");

    TVector<NRTYServer::TMessage> messages2;
    GenerateInput(messages2, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    messages2[0].MutableDocument()->SetUrl("qq qq qq");
    messages2[0].MutableDocument()->SetBody("aa bb");

    return DoRun(messages1, messages2, {"qq", "ww", "aa"}, true);
}
};

START_TEST_DEFINE_PARENT(TestSentIndexMergeEmpty, TTestSentIndexMergeBase)
bool InitConfig() final {
    SetIndexerParams(ALL, 600, 1);
    SetMergerParams(true, 1, -1, mcpNONE);
    (*ConfigDiff)["Indexer.Common.TokenizeUrl"] = false;
    return true;
}

bool Run() override {
    if (GetIsPrefixed())
        return true;

    TVector<NRTYServer::TMessage> messages1(1);
    messages1[0].SetMessageType(NRTYServer::TMessage::ADD_DOCUMENT);
    messages1[0].MutableDocument()->SetUrl("url1");

    TVector<NRTYServer::TMessage> messages2(1);
    messages2[0].SetMessageType(NRTYServer::TMessage::ADD_DOCUMENT);
    messages2[0].MutableDocument()->SetUrl("url2");

    // TODO(salmin@): need to enable checkStats, see SAAS-4789.
    return DoRun(messages1, messages2, {}, false);
}
};

START_TEST_DEFINE_PARENT(TestSentIndexMergeOnlyAttrs, TTestSentIndexMergeBase)
// no text hits, only search attributes, see SAAS-4771

bool InitConfig() final {
    SetIndexerParams(ALL, 600, 1);
    SetMergerParams(true, 1, -1, mcpNONE);
    (*ConfigDiff)["Indexer.Common.TokenizeUrl"] = false;
    return true;
}

bool Run() override {
    if (GetIsPrefixed())
        return true;

    TVector<NRTYServer::TMessage> messages1(1);
    messages1[0].SetMessageType(NRTYServer::TMessage::ADD_DOCUMENT);
    messages1[0].MutableDocument()->SetUrl("url1");
    AddSearchProperty(messages1[0], "s_attr1", "value1");

    TVector<NRTYServer::TMessage> messages2(1);
    messages2[0].SetMessageType(NRTYServer::TMessage::ADD_DOCUMENT);
    messages2[0].MutableDocument()->SetUrl("url2");
    AddSearchProperty(messages1[0], "s_attr1", "value1");
    AddSearchProperty(messages1[0], "s_attr2", "value2");

    // TODO(salmin@): need to enable checkStats, see SAAS-4789.
    return DoRun(messages1, messages2, {"(s_attr1:value1)", "(s_attr2:value2)"}, false);
}
};

START_TEST_DEFINE(TestMaxDocsPerKps)
bool Run() override {
    bool isPrefixed = GetIsPrefixed();
    const int CountMessages = 200;
    TAttrMap attrs;
    for (int i = 0; i < CountMessages; i++) {
        TAttrMap::value_type map1;
        map1["unique_attr"] = (i % 10) * 200 + i;
        attrs.push_back(map1);
    }
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, CountMessages, NRTYServer::TMessage::ADD_DOCUMENT, isPrefixed, attrs);
    if (isPrefixed) {
        for (int i = 0; i < CountMessages; i++)
            messages[i].MutableDocument()->SetKeyPrefix(1 + i % 3);
    }

    IndexMessages(messages, REALTIME, 1);
    ReopenIndexers();
    TSet<std::pair<ui64, TString> > deleted;
    DeleteSomeMessages(messages, deleted, DISK);
    TMap <ui32, TMap<ui32, TString> > kpsPrunUrl;
    for (int i = 0; i < CountMessages; i++) {
        TString url = messages[i].GetDocument().GetUrl();
        ui64 kps = messages[i].GetDocument().GetKeyPrefix();
        if (deleted.contains(std::make_pair(kps, url)))
            continue;
        ui64 prun = FromString<ui64>(attrs[i]["unique_attr"].Value);
        kpsPrunUrl[kps][prun] = url;
    }
    for (const auto& kps : kpsPrunUrl)
        for (const auto& prun : kps.second)
            DEBUG_LOG << kps.first << " " << prun.second << " " << prun.first << Endl;
    Controller->RestartServer(false, nullptr);
    Controller->ProcessCommand("create_merger_tasks");
    Controller->ProcessCommand("do_all_merger_tasks");
    Controller->RestartServer(false, nullptr);
    for (const auto& kps: kpsPrunUrl) {
        ui64 expextedResults = (kps.first == 1) ? 30 : 20;
        TVector<TDocSearchInfo> results;
        QuerySearch("body&numdoc=100&how=docid&kps=" + ToString(kps.first), results);
        if (results.size() != expextedResults)
            ythrow yexception() << "kps=" << kps.first << ":" << results.size() << "!= " << expextedResults;
        auto expectedUrl = kps.second.rbegin();
        for (ui32 i = 0; i < expextedResults; ++i) {
            if (results[i].GetUrl() != expectedUrl->second) {
                ythrow yexception() << "kps=" << kps.first << " (" << i << "):" << results[i].GetUrl() << "!= " << expectedUrl->second;
            }
            ++expectedUrl;
        }
    }

    return true;
}
bool InitConfig() override {
    SetPruneAttrSort("unique_attr");
    SetIndexerParams(ALL, 50);
    SetMergerParams(true, 1);
    (*ConfigDiff)["Merger.MaxDocsPerKps.Default"] = 20;
    (*ConfigDiff)["Merger.MaxDocsPerKps.Exceptions.1"] = 30;
    return true;
}
};
