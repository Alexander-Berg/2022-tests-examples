#include <saas/rtyserver_test/testerlib/rtyserver_test.h>

#include <library/cpp/charset/recyr.hh>

START_TEST_DEFINE(TestRusToEng)
    bool Run() override {
        int keyPrefix = 0;
        int countMessages = 2;
        const bool isPrefixed = GetIsPrefixed();
        if (isPrefixed)
            keyPrefix = 1;
        TVector<NRTYServer::TMessage> messagesForMemory, messagesForDisk;

        GenerateInput(messagesForMemory, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap());
        GenerateInput(messagesForDisk, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap());
        for (int i = 0; i < countMessages; i++) {
            if (isPrefixed) {
                messagesForMemory[i].MutableDocument()->SetKeyPrefix(keyPrefix);
                messagesForDisk[i].MutableDocument()->SetKeyPrefix(keyPrefix);
            }
        }
        messagesForDisk[0].MutableDocument()->SetUrl("3be4etc");
        messagesForDisk[1].MutableDocument()->SetUrl("4be5");
        messagesForDisk[0].MutableDocument()->SetBody("to be or not to be");
        messagesForMemory[0].MutableDocument()->SetBody("to be or not to be");
        messagesForDisk[1].MutableDocument()->SetBody("to3be3or3no33to3be");
        messagesForMemory[1].MutableDocument()->SetBody("to3be3or3not3to3be");

        IndexMessages(messagesForDisk, DISK, 1);
        IndexMessages(messagesForMemory, REALTIME, 1);

        TVector<TDocSearchInfo> results;

        sleep(1);
        QuerySearch(RecodeToYandex(CODES_UTF8, "был")+"&kps="+ToString(keyPrefix), results);
        if (results.size())
            ythrow yexception() << "TestRusToEng failed case A: " << results.size() << " != 0";

        QuerySearch("be&kps="+ToString(keyPrefix), results);
        if (results.size() != 2)
            ythrow yexception() << "TestRusToEng failed case B: " << results.size() << " != 1";

        ReopenIndexers();

        QuerySearch(RecodeToYandex(CODES_UTF8, "был")+"&kps="+ToString(keyPrefix), results);
        if (results.size())
            ythrow yexception() << "TestRusToEng failed case C: " << results.size() << " != 0";

        QuerySearch("be&kps=" + ToString(keyPrefix), results);
        if (results.size() != 4)
            ythrow yexception() << "TestRusToEng failed case D" << results.size() << " != 2";

        QuerySearch("etc&kps=" + ToString(keyPrefix), results);
        if (results.size() != 0)
            ythrow yexception() << "TestRusToEng failed case E" << results.size() << " != 0";

        QuerySearch("domain:\"*\"&kps=" + ToString(keyPrefix), results);
        if (results.size() != 0)
            ythrow yexception() << "TestRusToEng failed case F" << results.size() << " != 0";

        QuerySearch("rhost:\"*\"&kps=" + ToString(keyPrefix), results);
        if (results.size() != 0)
            ythrow yexception() << "TestRusToEng failed case G" << results.size() << " != 0";

        return true;
    }
public:
    bool InitConfig() override {
        (*ConfigDiff)["Indexer.Common.TokenizeUrl"] = false;
        (*ConfigDiff)["Indexer.Common.MadeUrlAttributes"] = false;
        return true;
    }
};

START_TEST_DEFINE(TestRhost)
bool Run() override {
    int keyPrefix = 0;
    int countMessages = 1;
    const bool isPrefixed = GetIsPrefixed();
    if (isPrefixed)
        keyPrefix = 1;
    TVector<NRTYServer::TMessage> message;

    GenerateInput(message, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap());
    for (int i = 0; i < countMessages; i++) {
        if (isPrefixed) {
            message[i].MutableDocument()->SetKeyPrefix(keyPrefix);
        }
    }
    message[0].MutableDocument()->SetUrl("http://yandex.ru/some/uri");

    IndexMessages(message, REALTIME, 1);

    TVector<TDocSearchInfo> results;

    sleep(1);
    QuerySearch("rhost:\"ru.yandex\"&kps=" + ToString(keyPrefix), results);
    if (results.size() != 1)
        ythrow yexception() << "TestRusToEng failed case A: " << results.size() << " != 1";

    ReopenIndexers();

    QuerySearch("rhost:\"ru.yandex\"&kps=" + ToString(keyPrefix), results);
    if (results.size() != 1)
        ythrow yexception() << "TestRusToEng failed case A*: " << results.size() << " != 1";
    return true;
}
public:
    bool InitConfig() override {
        (*ConfigDiff)["Indexer.Common.TokenizeUrl"] = false;
        return true;
    }
};
