#include <saas/rtyserver_test/testerlib/rtyserver_test.h>

START_TEST_DEFINE(TestRecognizer)
    bool Run() override {
        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), "фильм");
        messages.back().MutableDocument()->SetLanguage("");
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();
        if (!CheckExistsByText("фильмов", false, messages)) {
            ythrow yexception() << "epic fail";
        }
        messages.clear();
        GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), "люди");
        messages.back().MutableDocument()->SetLanguage("");
        IndexMessages(messages, REALTIME, 1);
        Sleep(TDuration::Seconds(5));
        if (!CheckExistsByText("человек", false, messages)) {
            ythrow yexception() << "epic fail";
        }
        return true;
    }
};

START_TEST_DEFINE(TestDash)
bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), "sprav-title");
    messages.back().MutableDocument()->SetLanguage("sk");
    IndexMessages(messages, REALTIME, 1);
    if (!CheckExistsByText("sprav-title", false, messages))
        ythrow yexception() << "fail on memory";
    ReopenIndexers();
    if (!CheckExistsByText("sprav-title", false, messages))
        ythrow yexception() << "fail on disk";
    return true;
}
bool InitConfig() override {
    SetMorphologyParams("sk");
    return true;
}
};
