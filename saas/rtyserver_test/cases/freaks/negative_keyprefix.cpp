#include <saas/rtyserver_test/testerlib/rtyserver_test.h>

START_TEST_DEFINE(TestNegativeKeyPrefix)
bool Run() override {
    if (!GetIsPrefixed())
        return true;
    const int CountMessages = 1;
    TVector<NRTYServer::TMessage> messagesForMemory, messagesForDisk;
    GenerateInput(messagesForMemory, CountMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    GenerateInput(messagesForDisk, CountMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    messagesForMemory.front().MutableDocument()->SetKeyPrefix(-1);
    messagesForDisk.front().MutableDocument()->SetKeyPrefix(-1);

    MUST_BE_BROKEN(IndexMessages(messagesForDisk, DISK, 1));

    ReopenIndexers();

    MUST_BE_BROKEN(IndexMessages(messagesForMemory, REALTIME, 1));
    return true;
}
public:
    bool InitConfig() override {
        SetIndexerParams(REALTIME, 10, 1);
        return true;
    }
};

START_TEST_DEFINE(TestBigKeyPrefix)
bool Run() override {
    if (!GetIsPrefixed())
        return true;
    const int CountMessages = 1;
    TVector<NRTYServer::TMessage> messagesForMemory, messagesForDisk;
    GenerateInput(messagesForMemory, CountMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    GenerateInput(messagesForDisk, CountMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    messagesForMemory.front().MutableDocument()->SetKeyPrefix(Max<ui64>() - 10);
    messagesForDisk.front().MutableDocument()->SetKeyPrefix(Max<ui64>() - 10);

    IndexMessages(messagesForDisk, DISK, 1);

    ReopenIndexers();

    IndexMessages(messagesForMemory, REALTIME, 1);

    TSearchMessagesContext smc;
    smc.CountResults.insert(1);

    CheckSearchResults(messagesForMemory, smc);
    CheckSearchResults(messagesForDisk, smc);

    return true;
}
public:
    bool InitConfig() override {
        SetIndexerParams(REALTIME, 10, 1);
        return true;
    }
};

START_TEST_DEFINE(TestSearchAttrNegative)
protected:
    void Test(TIndexerType indexer) {
        const unsigned countMessages = 100;
        TVector<NRTYServer::TMessage> messages;
        const TString indexerType(indexer == DISK ? "disk" : "memory");
        GenerateInput(messages, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), indexerType, true);
        for (unsigned i = 0; i < countMessages; ++i) {
            NRTYServer::TMessage::TDocument &doc = *messages[i].MutableDocument();
            doc.SetKeyPrefix(messages.front().GetDocument().GetKeyPrefix());
            NRTYServer::TAttribute* attr = doc.AddSearchAttributes();
            attr->set_name("attachsize_b");
            attr->set_value(ToString(-1));
            attr->set_type(NRTYServer::TAttribute::INTEGER_ATTRIBUTE);
        }
        MUST_BE_BROKEN(IndexMessages(messages, indexer, 1));
    }
public:
    bool Run() override {
        Test(REALTIME);
        Test(DISK);
        return true;
    }
};

START_TEST_DEFINE(TestSearchAttrBig)
protected:
    void Test(TIndexerType indexer) {
        const unsigned countMessages = 100;
        TVector<NRTYServer::TMessage> messages;
        const TString indexerType(indexer == DISK ? "disk" : "memory");
        GenerateInput(messages, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), indexerType, true);
        for (unsigned i = 0; i < countMessages; ++i) {
            NRTYServer::TMessage::TDocument &doc = *messages[i].MutableDocument();
            doc.SetKeyPrefix(messages.front().GetDocument().GetKeyPrefix());
            NRTYServer::TAttribute* attr = doc.AddSearchAttributes();
            attr->set_name("attachsize_b");
            attr->set_value(ToString(Max<i64>() - 1));
            attr->set_type(NRTYServer::TAttribute::INTEGER_ATTRIBUTE);
        }
        MUST_BE_BROKEN(IndexMessages(messages, indexer, 1));
    }
public:
    bool Run() override {
        Test(REALTIME);
        Test(DISK);
        return true;
    }
};

START_TEST_DEFINE(TestDefaultKps)
public:
    bool Run() override {
        if (!GetIsPrefixed())
            return true;
        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, 2, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
        messages[0].MutableDocument()->SetKeyPrefix(1);
        messages[0].MutableDocument()->SetBody("one");
        messages[1].MutableDocument()->SetKeyPrefix(2);
        messages[1].MutableDocument()->SetBody("two");
        IndexMessages(messages, REALTIME, 1);
        TVector<TDocSearchInfo> results;
        QuerySearch("one", results);
        if (!results.empty())
            ythrow yexception() << "strange...";
        SwitchDefaultKps(1);
        QuerySearch("one", results);
        if (results.size() != 1)
            ythrow yexception() << "first switch had not work";
        SwitchDefaultKps(2);
        QuerySearch("two", results);
        if (results.size() != 1)
            ythrow yexception() << "second switch had not work";
        Controller->RestartServer(false);
        QuerySearch("two", results);
        if (results.size() != 1)
            ythrow yexception() << "restart server reset default kps";
        QuerySearch("one&kps=1", results);
        if (results.size() != 1)
            ythrow yexception() << "explicit kps had not work";
        NJson::TJsonValue info = GetInfoRequest();
        CHECK_WITH_LOG(info.IsArray());
        for (NJson::TJsonValue::TArray::const_iterator i = info.GetArray().begin(); i != info.GetArray().end(); ++i){
            CHECK_WITH_LOG(i->IsMap());
            auto j = i->GetMap().find("default_kps");
            if (j == i->GetMap().end())
                ythrow yexception() << "kps info not found";
            if (j->second.GetInteger() != 2)
                ythrow yexception() << "incorrect kps info message processing: " << j->second.GetInteger() << ", must be 2";
        }

        return true;
    }
};
