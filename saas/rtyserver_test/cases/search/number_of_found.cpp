#include <saas/rtyserver_test/testerlib/rtyserver_test.h>

START_TEST_DEFINE(TestNumberOfFound)
bool Run() override {
    if (GetIsPrefixed())
        return true;
    const size_t countMessages = 100;

    TVector<NRTYServer::TMessage> messages1;
    for (size_t gen = 0; gen < countMessages; gen++) {
        TAttrMap::value_type map;
        map["mid"] = gen % 10;
        GenerateInput(messages1, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(1, map));
    }
    IndexMessages(messages1, DISK, 1);

    ReopenIndexers();

    TVector<NRTYServer::TMessage> messages2;
    for (size_t gen = 0; gen < countMessages; gen++) {
        TAttrMap::value_type map;
        map["mid"] = gen % 10;
        GenerateInput(messages2, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(1, map));
    }
    IndexMessages(messages2, REALTIME, 1);

    ReopenIndexers();

    if (!CheckGroups(Query("/?ms=proto&text=body&g=1.mid.30.150&numdoc=100"), 10, 20, 1)) {
        ythrow yexception() << "Incorrect documents count";
    }
    return true;
}
public:
    bool InitConfig() override {
        SetIndexerParams(ALL, 10, 1);
        return true;
    }
};
