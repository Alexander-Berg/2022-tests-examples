#include <saas/rtyserver_test/testerlib/rtyserver_test.h>

START_TEST_DEFINE(TestTitleCase)
bool Run() override {
    int keyPrefix = 0;
    int countMessages = 2;
    if (GetIsPrefixed())
        keyPrefix = 1;
    TVector<NRTYServer::TMessage> messagesForMemory, messagesForDisk;

    GenerateInput(messagesForMemory, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, keyPrefix, TAttrMap());
    GenerateInput(messagesForDisk, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, keyPrefix, TAttrMap());
    TVector<TString> texts;
    for (int i = 0; i < countMessages; i++) {
        if (keyPrefix) {
            messagesForMemory[i].MutableDocument()->SetKeyPrefix(keyPrefix);
            messagesForDisk[i].MutableDocument()->SetKeyPrefix(keyPrefix);
        }
    }
    messagesForDisk[0].MutableDocument()->SetBody("tv1000");
    messagesForMemory[0].MutableDocument()->SetBody("tv1000");
    messagesForDisk[1].MutableDocument()->SetBody("Tv1000");
    messagesForMemory[1].MutableDocument()->SetBody("Tv1000");

    IndexMessages(messagesForDisk, DISK, 1);
    IndexMessages(messagesForMemory, REALTIME, 1);

    TVector<TDocSearchInfo> results;

    sleep(10);
    QuerySearch("tv1000&kps=" + ToString(keyPrefix), results);
    if (results.size() != 2)
        ythrow yexception() << "TestTitleCase failed case B: " << results.size() << " != 0";

    QuerySearch("Tv1000&kps=" + ToString(keyPrefix), results);
    if (results.size() != 2)
        ythrow yexception() << "TestTitleCase failed case A: " << results.size() << " != 0";

    ReopenIndexers();

    QuerySearch("Tv1000&kps=" + ToString(keyPrefix), results);
    if (results.size() != 4)
        ythrow yexception() << "TestTitleCase failed case C: " << results.size() << " != 0";

    QuerySearch("tv1000&kps=" + ToString(keyPrefix), results);
    if (results.size() != 4)
        ythrow yexception() << "TestTitleCase failed case D: " << results.size() << " != 0";

    return true;
}
public:
    bool InitConfig() override {
        (*ConfigDiff)["DoIndexUrls"] = false;
        return true;
    }
};
