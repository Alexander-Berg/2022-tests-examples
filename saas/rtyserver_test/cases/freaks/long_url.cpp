#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <library/cpp/string_utils/quote/quote.h>

START_TEST_DEFINE(TestLongUrl)
    bool Run() override {
        const int CountMessages = 1;
        TVector<NRTYServer::TMessage> messagesForMemory, messagesForDisk;
        GenerateInput(messagesForMemory, CountMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
        GenerateInput(messagesForDisk, CountMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
        TString freakUrlDisk = GetRandomWord(1025);
        TString freakUrlMemory = GetRandomWord(1025);
        messagesForMemory.front().MutableDocument()->SetUrl(freakUrlMemory);
        messagesForDisk.front().MutableDocument()->SetUrl(freakUrlDisk);

        bool wasError = true;
        try {
            IndexMessages(messagesForDisk, DISK, 1);
            wasError = false;
        } catch (...) {
        }
        if (!wasError)
            ythrow yexception() << "No throw exception on incorrect url. DiskIndexer.";

        try {
            IndexMessages(messagesForMemory, REALTIME, 1);
            wasError = false;
        } catch (...) {
        }
        if (!wasError)
            ythrow yexception() << "No throw exception on incorrect url. MemoryIndexer.";
        return true;
    }
public:
    bool InitConfig() override {
        SetIndexerParams(REALTIME, 10, 1);
        return true;
    }
};

START_TEST_DEFINE(TestBrokenUrl)
bool Run() override {
    const int CountMessages = 1;
    TVector<NRTYServer::TMessage> messagesForMemory, messagesForDisk;
    GenerateInput(messagesForMemory, CountMessages, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    GenerateInput(messagesForDisk, CountMessages, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    const TString urlDisk("http://test/test.xml-disk/?xml=yes++&xxx=da");
    const TString urlMemory("http://test/test.xml-memory/?xml=yes++&xxx=da");
    TString freakUrlDisk(urlDisk);
    TString freakUrlMemory(urlMemory);

    messagesForMemory.front().MutableDocument()->SetUrl(urlMemory);
    messagesForDisk.front().MutableDocument()->SetUrl(urlDisk);
    TString KeyPrefixInfo = "";
    if (GetIsPrefixed()) {
        messagesForMemory.front().MutableDocument()->SetKeyPrefix(1);
        messagesForDisk.front().MutableDocument()->SetKeyPrefix(1);
        KeyPrefixInfo = "&kps=1";
    }

    TVector<TDocSearchInfo> results;
    IndexMessages(messagesForMemory, REALTIME, 1);

    Quote(freakUrlMemory);
    QuerySearch("url:\"" + freakUrlMemory + "\"" + KeyPrefixInfo, results);
    if (results.size() != 1) {
        ythrow yexception() << "incorrect case A";
    }
    if (results[0].GetUrl() != urlMemory)
        ythrow yexception() << "incorrect case A*";

    IndexMessages(messagesForDisk, DISK, 1);
    ReopenIndexers();

    Quote(freakUrlDisk);
    QuerySearch("url:\"" + freakUrlDisk + "\"" + KeyPrefixInfo, results);
    if (results.size() != 1) {
        ythrow yexception() << "incorrect case B";
    }
    if (results[0].GetUrl() != urlDisk)
        ythrow yexception() << "incorrect case B*";

    QuerySearch("url:\"" + freakUrlMemory + "\"" + KeyPrefixInfo, results);
    if (results.size() != 1) {
        ythrow yexception() << "incorrect case C";
    }
    if (results[0].GetUrl() != urlMemory)
        ythrow yexception() << "incorrect case C*";

    DeleteQueryResult("url:\"" + freakUrlMemory  + "\""+ KeyPrefixInfo, REALTIME);
    QuerySearch("url:\"" + freakUrlMemory + "\"" + KeyPrefixInfo, results);
    if (results.size() != 0) {
        ythrow yexception() << "incorrect case A1";
    }

    DeleteQueryResult("url:\"" + freakUrlDisk  + "\""+ KeyPrefixInfo, REALTIME);
    QuerySearch("url:\"" + freakUrlDisk + "\"" + KeyPrefixInfo, results);
    if (results.size() != 0) {
        ythrow yexception() << "incorrect case B1";
    }

    return true;
}
public:
    bool InitConfig() override {
        SetIndexerParams(REALTIME, 10, 3);
        return true;
    }
};

START_TEST_DEFINE(TestMoneyUrl)
bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 2, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
//    messages[0].MutableDocument()->SetUrl("scid337");
    messages[0].MutableDocument()->SetUrl("scid1889");
    messages[1].MutableDocument()->SetUrl("scid1884");
    messages[1].MutableDocument()->SetBody("asdsda");
    messages[0].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());
    messages[1].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());
    IndexMessages(messages, REALTIME, 1);
    sleep(1);
    TVector<TDocSearchInfo> results;
    TSet<ui32> docIds;
    QuerySearch("url:\"*\"&kps=" + ToString(messages[0].GetDocument().GetKeyPrefix()), results);
    for (size_t i = 0; i < results.size(); i++) {
        docIds.insert(results[i].GetDocId());
    }
    if (docIds.size() != 2)
        ythrow yexception() << "hashes collision!";
    QuerySearch("asdsda&kps=" + ToString(messages[0].GetDocument().GetKeyPrefix()), results);
    if (results.ysize() != 1)
        ythrow yexception() << "incorrect query result: asdsda";
    if (results[0].GetUrl() != "scid1884")
        ythrow yexception() << "incorrect asdsda url: " << results[0].GetUrl();
    QuerySearch("body&kps=" + ToString(messages[0].GetDocument().GetKeyPrefix()), results);
    if (results.ysize() != 1)
        ythrow yexception() << "incorrect query result: body";
    if (results[0].GetUrl() != "scid1889")
        ythrow yexception() << "incorrect body url: " << results[0].GetUrl();
    return true;
}
public:
    bool InitConfig() override {
        SetIndexerParams(REALTIME, 10, 1);
        return true;
    }
};

START_TEST_DEFINE(TestPeopleUrl)
bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    messages[0].MutableDocument()->SetUrl("http%3A//twitter.com/u_u_u_u");
    IndexMessages(messages, REALTIME, 1);
    sleep(1);
    TVector<TDocSearchInfo> results;
    QuerySearch("\"u\"&kps=" + ToString(messages[0].GetDocument().GetKeyPrefix()), results);
    if (!results.empty())
        ythrow yexception() << "Fail";
    return true;
}

public:
    bool InitConfig() override {
        SetIndexerParams(REALTIME, 10, 1);
        return true;
    }
};

START_TEST_DEFINE(TestCaseSensInUrl)
bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    messages[0].MutableDocument()->SetUrl("http%3A//TWitter.com/u_u_u_u/?AAA=BBB");
    IndexMessages(messages, DISK, 1);
    ReopenIndexers();
    return true;
}

public:
    bool InitConfig() override {
        SetIndexerParams(REALTIME, 10, 1);
        return true;
    }
};

START_TEST_DEFINE(TestWORMHOLE)
bool Run() override {
    if (GetIsPrefixed())
        return true;
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    messages[0].MutableDocument()->SetUrl("http%3A//twitter.com/u_u_u_u");
    IndexMessages(messages, REALTIME, 1);
    ReopenIndexers();
    IndexMessages(messages, REALTIME, 1);
    TVector<TDocSearchInfo> results;
    QuerySearch("url:\"*\"&kps=" + ToString(messages[0].GetDocument().GetKeyPrefix()), results, nullptr, nullptr, false, TString(), 0, new TDefaultSourceSelector(0));
    if (results.size() != 1)
        ythrow yexception() << "Fail";
    QuerySearch("url:\"*\"&kps=" + ToString(messages[0].GetDocument().GetKeyPrefix()), results, nullptr, nullptr, false, TString(), 0, new TDefaultSourceSelector(1));
    if (results.size() != 1)
        ythrow yexception() << "Fail";
    QuerySearch("url:\"*\"&kps=" + ToString(messages[0].GetDocument().GetKeyPrefix()), results);
    if (results.size() != 2)
        ythrow yexception() << "Fail";
    return true;
}

public:
    bool InitConfig() override {
        SetIndexerParams(REALTIME, 10, 1);
        return true;
    }
};

START_TEST_DEFINE(TestIncorrectTimeout)
bool Run() override {
    TVector<TDocSearchInfo> results;
    CHECK_TEST_EQ(400, QuerySearch("url:\"*\"&timeout=abc&kps=" + (TString)(GetIsPrefixed() ? "1" : "0"), results));
    CHECK_TEST_EQ(400, QuerySearch("url:\"*\"&timeout=0&kps=" + (TString)(GetIsPrefixed() ? "1" : "0"), results));
    return true;
}

public:
    bool InitConfig() override {
        SetIndexerParams(REALTIME, 10, 1);
        return true;
    }
};

START_TEST_DEFINE(TestAutoDelegation)
bool Run() override {
    if (GetIsPrefixed())
        return true;
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    messages[0].MutableDocument()->SetUrl("http%3A//twitter.com/u_u_u_u");
    IndexMessages(messages, DISK, 1);
    ReopenIndexers();
    TVector<TDocSearchInfo> results;
    TQuerySearchContext ctx;
    ctx.SourceSelector = new TIndifferentSourceSelector(-1);
    QuerySearch("url:\"*\"&service=tests&kps=" + ToString(messages[0].GetDocument().GetKeyPrefix()), results, ctx);
    if (results.size() != 1)
        ythrow yexception() << "Fail";
    return true;
}

public:
    bool InitConfig() override {
        if (GetIsPrefixed())
            return true;
        (*ConfigDiff)["Indexer.Memory.Enabled"] = "false";
        (*ConfigDiff)["SearchersCountLimit"] = 1;
        (*ConfigDiff)["Searcher.DelegateRequestOptimization"] = true;
        SetMergerParams(true, 1, -1, mcpTIME, 500000);
        SetIndexerParams(REALTIME, 10, 1);
        return true;
    }
};

START_TEST_DEFINE(TestDelegationOnMerge)
bool Run() override {
    if (GetIsPrefixed())
        return true;
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 100000, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    IndexMessages(messages, DISK, 1, 1000, false);
    TSearchMessagesContext context = TSearchMessagesContext::BuildDefault(1);
    context.CountThreads = 2;
    context.CountResults.insert(0);
    CheckSearchResults(messages, context);
    Controller->WaitEmptyIndexingQueues();
    return true;
}

public:
    bool InitConfig() override {
        if (GetIsPrefixed())
            return true;
        (*ConfigDiff)["Indexer.Memory.Enabled"] = "false";
        (*ConfigDiff)["Indexer.Disk.MaxDocuments"] = "1000";
        (*ConfigDiff)["SearchersCountLimit"] = 1;
        (*ConfigDiff)["Searcher.DelegateRequestOptimization"] = true;
        SetMergerParams(true, 1, -1, mcpCONTINUOUS, 500000);
        return true;
    }
};

START_TEST_DEFINE(TestBigRuid)
bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    IndexMessages(messages, REALTIME, 1);
    sleep(1);
    TVector<TDocSearchInfo> results;
    QuerySearch("url:\"*\"&ruid=2&kps=" + ToString(messages[0].GetDocument().GetKeyPrefix()), results);
    if (!results.size())
        ythrow yexception() << "incorrect query result: all documents";
    QuerySearch("url:\"*\"&ruid=11328668292793306319717571892&kps=" + ToString(messages[0].GetDocument().GetKeyPrefix()), results);
    if (!results.size())
        ythrow yexception() << "incorrect query result: all documents";
    return true;
}
public:
    bool InitConfig() override {
        SetIndexerParams(REALTIME, 10, 1);
        return true;
    }
};
