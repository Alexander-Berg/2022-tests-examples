#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/rtyserver_test/testerlib/standart_generator.h>

SERVICE_TEST_RTYSERVER_DEFINE(TDeletingTestCase)
    void Test(size_t messagesCount, TIndexerType indexer, bool reopen)
    {
        TVector<NRTYServer::TMessage> messages;
        TVector<NRTYServer::TMessage> messagesDel;
        GenerateInput(messages, messagesCount, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
        messagesDel.push_back(BuildDeleteMessage(messages.front()));
        TSet<std::pair<ui64, TString> > deletedUrls;
        deletedUrls.insert(std::make_pair(messagesDel.back().GetDocument().GetKeyPrefix(), messagesDel.back().GetDocument().GetUrl()));
        IndexMessages(messages, indexer, 1);
        IndexMessages(messagesDel, indexer, 1);
        if (reopen)
            ReopenIndexers();
        CheckSearchResults(messages, deletedUrls);
    }

    void MultiDeleteTest(size_t messagesCount, TIndexerType indexer, bool reopen)
    {
        TVector<NRTYServer::TMessage> messages, messagesDel;
        GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
        for (size_t i = 1; i < messagesCount; ++i) {
            messages.push_back(messages.front());
            if (SendIndexReply)
                messages.back().SetMessageId(IMessageGenerator::CreateMessageId());
        }

        messagesDel.push_back(BuildDeleteMessage(messages.front()));

        IndexMessages(messages, indexer, 1);

        if (reopen)
            ReopenIndexers();
        sleep(1);
        IndexMessages(messagesDel, indexer, 1);
        CheckSearchResults(messages, TSet<std::pair<ui64, TString> >(), 0);
    }

public:
    bool InitConfig() override {
        SetIndexerParams(ALL, 10, 1);
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestDeletingMemory, TDeletingTestCase)
    bool Run() override {
        ui32 maxDocs = GetMaxDocuments();
        DEBUG_LOG << "Simple case" << Endl;
        Test(maxDocs / 2u, REALTIME, false);
        DEBUG_LOG << "On Close" << Endl;
        Test(maxDocs, REALTIME, false);
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestDeletingTemp, TDeletingTestCase)
    bool Run() override {
        size_t shardsNumber = GetShardsNumber();
        DEBUG_LOG << "Delete from temp index" << Endl;
        Test(shardsNumber * GetMaxDocuments() / 2u, DISK, true);
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestDeletingDisk, TDeletingTestCase)
    bool Run() override {
        size_t shardsNumber = GetShardsNumber();
        ui32 maxDocs = GetMaxDocuments();
        DEBUG_LOG << "Delete from complate index" << Endl;
        Test(shardsNumber * maxDocs / 2u, DISK, true);
        DEBUG_LOG << "Delete from autocomplate index" << Endl;
        Test(shardsNumber * maxDocs, DISK, false);
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestMultiDeletingMemory, TDeletingTestCase)
bool Run() override {
    ui32 maxDocs = GetMaxDocuments();
    DEBUG_LOG << "Simple case" << Endl;
    MultiDeleteTest(maxDocs / 2u, REALTIME, false);
    DEBUG_LOG << "On Close" << Endl;
    MultiDeleteTest(maxDocs, REALTIME, false);
    return true;
}
};

START_TEST_DEFINE_PARENT(TestMultiDeletingTemp, TDeletingTestCase)
bool Run() override {
    size_t shardsNumber = GetShardsNumber();
    DEBUG_LOG << "Delete from temp index" << Endl;
    MultiDeleteTest(shardsNumber * GetMaxDocuments() / 2u, DISK, true);
    return true;
}
};

START_TEST_DEFINE_PARENT(TestMultiDeletingDisk, TDeletingTestCase)
bool Run() override {
    size_t shardsNumber = GetShardsNumber();
    ui32 maxDocs = GetMaxDocuments();
    DEBUG_LOG << "Delete from complate index" << Endl;
    MultiDeleteTest(shardsNumber * maxDocs / 2u, DISK, true);
    DEBUG_LOG << "Delete from autocomplate index" << Endl;
    MultiDeleteTest(shardsNumber * maxDocs, DISK, false);
    return true;
}
};

START_TEST_DEFINE(TestDeletingQueryDelTemp)
void Test(TIndexerType indexer, TIndexerType deleter) {
    size_t messagesCount = 10;
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, messagesCount, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    IndexMessages(messages, indexer, 1);
    const TString kps = "&kps=" + GetAllKps(messages);
    DeleteQueryResult("body" + kps, deleter);
    ReopenIndexers();
    TVector<TDocSearchInfo> results;
    QuerySearch("body" + kps, results);
    if (results.size() != 0)
        ythrow yexception() << "some messages were not delete";
}

bool Run() override {
    Test(REALTIME, REALTIME);
    Test(REALTIME, DISK);
    return true;
}
};

START_TEST_DEFINE(TestDeletingQueryDelCount)
void Test(TIndexerType indexer) {
    TVector<NRTYServer::TMessage> messages;
    size_t messagesCount = 13;
    TStandartDocumentGenerator* dg = new TStandartDocumentGenerator(GetIsPrefixed());
    TString kps = "";
    if (GetIsPrefixed()) {
        dg->SetPrefixConstant(1);
        kps = "&kps=1";
    }
    TStandartMessagesGenerator smg(dg, true);
    GenerateInput(messages, messagesCount, smg);
    IndexMessages(messages, indexer, 1);
    if (indexer == DISK)
        ReopenIndexers();
    NRTYServer::TReply reply = DeleteQueryResult("url:\"*\"" + kps + "&ftests=yes&numdoc=2", indexer);
    if (reply.GetStatusMessage().find("del_count=2;") == TString::npos)
        ythrow yexception() << "incorrect delete reply 2: " << reply.GetStatusMessage();
    reply = DeleteQueryResult("url:\"*\"" + kps + "&ftests=yes", indexer);
    if (reply.GetStatusMessage().find("del_count=11;") == TString::npos)
        ythrow yexception() << "incorrect delete reply 11: " << reply.GetStatusMessage();
    TVector<TDocSearchInfo> results;
    QuerySearch("body" + kps, results);
    if (results.size() != 0)
        ythrow yexception() << "some messages were not delete";
    ReopenIndexers();
    if (results.size() != 0)
        ythrow yexception() << "some messages were not delete";
}

bool Run() override {
    Test(REALTIME);
    Test(DISK);
    return true;
}
};

START_TEST_DEFINE(TestDeletingKpsCount)
bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    TStandartDocumentGenerator* dg = new TStandartDocumentGenerator(GetIsPrefixed());
    TString kps = "";
    bool isPrefixed = GetIsPrefixed();
    if (isPrefixed) {
        dg->SetPrefixConstant(10000);
        kps += "&kps=10000,20000";
    }
    TStandartMessagesGenerator smg(dg, true);
    GenerateInput(messages, 16, smg);
    if (isPrefixed)
        dg->SetPrefixConstant(20000);
    GenerateInput(messages, 23, smg);
    IndexMessages(messages, REALTIME, 1);
    sleep(4);
    TSet<std::pair<ui64, TString> > deleted;
    CheckSearchResults(messages, deleted);
    TVector<TDocSearchInfo> results;
    QuerySearch("url:\"*\"" + kps, results, nullptr, nullptr, true);
    PRINT_INFO_AND_TEST(CHECK_TEST_EQ(results.size(), 16 + 23));
    if (isPrefixed) {
        NRTYServer::TReply reply = DeleteSpecial(10000);
        PRINT_INFO_AND_TEST(CHECK_TEST_SUBSTR(reply.GetStatusMessage(), "del_count=16;"));
        reply = DeleteSpecial(20000);
        PRINT_INFO_AND_TEST(CHECK_TEST_SUBSTR(reply.GetStatusMessage(), "del_count=23;"));
    } else {
        NRTYServer::TReply reply = DeleteSpecial(0);
        PRINT_INFO_AND_TEST(CHECK_TEST_SUBSTR(reply.GetStatusMessage(), "del_count=39;"));
    }

    QuerySearch("url:\"*\"" + kps, results);
    PRINT_INFO_AND_TEST(CHECK_TEST_EQ(results.size(), 0));

    IndexMessages(messages, REALTIME, 1);

    CheckSearchResults(messages, deleted);

    TQuerySearchContext context;
    context.ResultCountRequirement = 39;
    context.AttemptionsCount = 30;
    context.PrintResult = true;

    QuerySearch("url:\"*\"" + kps, results);
    PRINT_INFO_AND_TEST(CHECK_TEST_EQ(results.size(), 16 + 23));

    NRTYServer::TReply reply = DeleteSpecial();
    PRINT_INFO_AND_TEST(CHECK_TEST_SUBSTR(reply.GetStatusMessage(), "del_count=39;"));

    QuerySearch("url:\"*\"" + kps, results);
    PRINT_INFO_AND_TEST(CHECK_TEST_EQ(results.size(), 0));
    return true;
}
public:
    bool InitConfig() override {
        SetIndexerParams(ALL, 10, 8);
        return true;
    }
};

START_TEST_DEFINE(TestDeleteIndex)
bool Run() override {
    const int numberOfMessages = 10;
    TVector<NRTYServer::TMessage> messagesA, messagesB;

    GenerateInput(messagesA, numberOfMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    GenerateInput(messagesB, numberOfMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());

    TString kps;
    if (GetIsPrefixed()) {
        kps = "&kps=1,65532";
        for (int i = 0; i < numberOfMessages; ++i) {
            messagesA[i].MutableDocument()->SetKeyPrefix(1);
            messagesB[i].MutableDocument()->SetKeyPrefix(65532);
        }
    }

    IndexMessages(messagesA, REALTIME, 1);
    IndexMessages(messagesB, REALTIME, 1);

    TVector<TDocSearchInfo> results;
    TQuerySearchContext searchContext;
    searchContext.AttemptionsCount = 10;
    searchContext.ResultCountRequirement = 2 * numberOfMessages;
    QuerySearch("url:\"*\"" + kps, results, searchContext);
    CHECK_TEST_EQ(results.size(), 2 * numberOfMessages);

    DeleteSpecial();

    QuerySearch("url:\"*\"" + kps, results);
    CHECK_TEST_EQ(results.size(), 0);

    ReopenIndexers();

    QuerySearch("url:\"*\"" + kps, results);
    CHECK_TEST_EQ(results.size(), 0);

    return true;
}
public:
    bool InitConfig() override {
        SetIndexerParams(ALL, 10, 8);
        return true;
    }
};

START_TEST_DEFINE(TestRemoveDocsPerformance)
bool Test(TIndexerType indexer, bool spec) {
    TVector<NRTYServer::TMessage> messages;
    size_t messagesCount = 20000;
    TStandartDocumentGenerator* dg = new TStandartDocumentGenerator(GetIsPrefixed());
    TString kps = "";
    if (GetIsPrefixed()) {
        dg->SetPrefixConstant(1);
        kps = "&kps=1";
    }
    TStandartMessagesGenerator smg(dg, true);
    GenerateInput(messages, messagesCount, smg);
    DEBUG_LOG << "Index start..." << Endl;
    IndexMessages(messages, indexer, 1, 0, true, true, TDuration(), TDuration(), 4);
    DEBUG_LOG << "Index OK" << Endl;
    if (indexer == DISK)
        ReopenIndexers();
    DEBUG_LOG << "Search start..." << Endl;
    TVector<TDocSearchInfo> results;
    QuerySearch("body" + kps + "&numdoc=10", results);
    if (results.ysize() != 10)
        TEST_FAILED("Incorrect search results count");
    DEBUG_LOG << "Search OK" << Endl;
    DEBUG_LOG << "Delete start..." << Endl;
    TInstant timeStart = Now();
    NRTYServer::TReply reply;
    if (spec) {
        if (GetIsPrefixed())
            reply = DeleteSpecial(1);
        else
            reply = DeleteSpecial();
    }
    else
        reply = DeleteQueryResult("url:\"*\"" + kps + "&ftests=yes", indexer);
    DEBUG_LOG << "reply_status=" << reply.GetStatus() << Endl;
    DEBUG_LOG << "Remove time = " << Now() - timeStart << ", spec = " << spec << Endl;
    QuerySearch("body" + kps, results);
    if (results.size() != 0)
        ythrow yexception() << "some messages were not delete: " << results.size();
    ReopenIndexers();
    if (results.size() != 0)
        ythrow yexception() << "some messages were not delete: " << results.size();
    return true;
}

bool Run() override {
    Test(REALTIME, true);
    Test(DISK, true);
    Test(REALTIME, false);
    Test(DISK, false);
    return true;
}

public:
    bool InitConfig() override {
        SetIndexerParams(ALL, 10000, 8);
        return true;
    }
};

