#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <library/cpp/charset/wide.h>
#include <util/string/vector.h>

START_TEST_DEFINE(TestMultitoken)

TVector<NRTYServer::TMessage> messages;

void Check() {
    TVector<TDocSearchInfo> results;
    QuerySearch("aaa" + GetAllKps(messages), results);
    if (results.size() != 1)
        ythrow yexception() << "Incorrect search results count case A";

    QuerySearch("aaa444bbb" + GetAllKps(messages), results);
    if (results.size() != 1)
        ythrow yexception() << "Incorrect search results count case B";

    QuerySearch(WideToChar(u"дня", CODES_YANDEX) + GetAllKps(messages), results);
    if (results.size() != 1)
        ythrow yexception() << "Incorrect search results count case C";

    QuerySearch(WideToChar(u"пфт", CODES_YANDEX) + GetAllKps(messages), results);
    if (results.size() != 1)
        ythrow yexception() << "Incorrect search results count case D";

    QuerySearch(WideToChar(u"день", CODES_YANDEX) + GetAllKps(messages), results);
    if (results.size() != 1)
        ythrow yexception() << "Incorrect search results count case E";
}

bool Run() override {
    TVector<TString> texts;
    texts.push_back("aaa444bbb");
    texts.push_back(WideToUTF8(u"день33пфт"));
    GenerateInput(messages, texts.size(), NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    for (size_t i = 0; i < texts.size(); ++i) {
        messages[i].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());
        messages[i].MutableDocument()->SetBody(texts[i]);
    }
    IndexMessages(messages, REALTIME, 1);
    sleep(5);
    Check();
    ReopenIndexers();
    Check();
    return true;
}
};

START_TEST_DEFINE(TestSolidMultitoken)

TVector<NRTYServer::TMessage> messages;

bool Check() {
    TVector<TDocSearchInfo> results;
    QuerySearch("aaa" + GetAllKps(messages) + "&keepjoins=yes", results);
    CHECK_TEST_EQ(results.size(), 1);

    QuerySearch("aaa444bbb" + GetAllKps(messages) + "&keepjoins=yes", results);
    CHECK_TEST_EQ(results.size(), 1);

    QuerySearch("aaa44*" + GetAllKps(messages) + "&keepjoins=yes", results);
    CHECK_TEST_EQ(results.size(), 1);

    QuerySearch(WideToChar(u"день33пфт", CODES_YANDEX) + GetAllKps(messages) + "&keepjoins=yes", results);
    CHECK_TEST_EQ(results.size(), 1);

    QuerySearch(WideToChar(u"день3*", CODES_YANDEX) + GetAllKps(messages) + "&keepjoins=yes", results);
    CHECK_TEST_EQ(results.size(), 1);

    QuerySearch(WideToChar(u"дня", CODES_YANDEX) + GetAllKps(messages) + "&keepjoins=yes", results);
    CHECK_TEST_EQ(results.size(), 1);

    QuerySearch(WideToChar(u"пфт", CODES_YANDEX) + GetAllKps(messages) + "&keepjoins=yes", results);
    CHECK_TEST_EQ(results.size(), 1);

    QuerySearch(WideToChar(u"день", CODES_YANDEX) + GetAllKps(messages) + "&keepjoins=yes", results);
    CHECK_TEST_EQ(results.size(), 1);
    return true;
}

bool Run() override {
    TVector<TString> texts;
    texts.push_back("aaa444bbb");
    texts.push_back(WideToUTF8(u"день33пфт"));
    GenerateInput(messages, texts.size(), NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    for (size_t i = 0; i < texts.size(); ++i) {
        messages[i].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());
        messages[i].MutableDocument()->SetBody(texts[i]);
    }
    IndexMessages(messages, REALTIME, 1);
    sleep(5);
    CHECK_TEST_EQ(Check(), true);
    ReopenIndexers();
    CHECK_TEST_EQ(Check(), true);
    return true;
}
};

