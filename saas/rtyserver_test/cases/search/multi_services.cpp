#include <saas/rtyserver_test/testerlib/rtyserver_test.h>

START_TEST_DEFINE(TestSearchNServices)
bool Run() override{
    if (Controller->GetActiveBackends().size() != 3)
        ythrow yexception() << "incorrect backends number for this test, must be 3, found " << Controller->GetActiveBackends().size();

    const int CountMessages1 = 18, CountMessages2 = 9;
    TVector<NRTYServer::TMessage> messages1, messages2;
    GenerateInput(messages1, CountMessages1, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    GenerateInput(messages2, CountMessages2, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());

    int keyPrefix = 0;
    if (GetIsPrefixed()) {
        keyPrefix = 10;
        for (ui32 i = 0; i < messages1.size(); ++i) {
            messages1[i].MutableDocument()->SetKeyPrefix(keyPrefix);
        }
        for (ui32 i = 0; i < messages2.size(); ++i) {
            messages2[i].MutableDocument()->SetKeyPrefix(keyPrefix);
        }
    }
    IndexMessages(messages1, REALTIME, 1, 0, true, true, TDuration(), TDuration(), 1, "tests");
    IndexMessages(messages2, REALTIME, 1, 0, true, true, TDuration(), TDuration(), 1, "tests1");

    TVector<TDocSearchInfo> results;
    QuerySearch("body&service=tests,tests1&kps=" + ToString(keyPrefix), results);
    CHECK_TEST_FAILED(results.size() != CountMessages1 + CountMessages2, "incorrect common count");

    QuerySearch("body&service=tests,tests1&p=2&numdoc=10&kps=" + ToString(keyPrefix), results);
    CHECK_TEST_FAILED(results.size() != CountMessages1 + CountMessages2 - 20, "incorrect count for page2");

    return true;
}
};
