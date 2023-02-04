#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <util/charset/utf8.h>
#include <library/cpp/charset/wide.h>

namespace {
    const TString FreakNoTags = "<root><opening_tag><freak attr=\"\"/></root>";
    const TString FreakBrokenAttr = "<root><atrr_cont grr_attr=\"/></root>";
    const TString FreakNonUTF8 = WideToChar(u"<root><section>Не UTF8 текст</section></root>", CODES_YANDEX);
}

START_TEST_DEFINE(TestFreaksML)
bool IndexXml(const TString& xml, TString& error) {
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), xml);
    messages.back().MutableDocument()->SetMimeType("text/xml");
    try {
        const TVector<NRTYServer::TReply>& replies = IndexMessages(messages, REALTIME, 1);
        if (replies.ysize() != 1)
            ythrow yexception() << "incorrect number of replies";
    } catch (const yexception& e) {
        error = e.what();
        return false;
    }
    return true;
}
#define CHECK_XML(X) if (IndexXml(X, error)) ythrow yexception() << "case " << #X << " failed: expected indexation fail"
bool Run() override {
    TString error;

    VERIFY_WITH_LOG(UTF8Detect(FreakNonUTF8) == NotUTF8, "test is broken, no non-UTF8 sample");

    //CHECK_XML(FreakNonUTF8);
    CHECK_XML(FreakNoTags);
    CHECK_XML(FreakBrokenAttr);
    return true;
}
};

START_TEST_DEFINE(TestPartialUtf8)
bool Run() override {
    const TString FreakPartialyNonUTF8 = "<root><section>" + WideToChar(u"частично UTF8 ", CODES_YANDEX) + "пример" + "</section></root>";
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), FreakPartialyNonUTF8);
    messages.back().MutableDocument()->SetMimeType("text/xml");
    messages.back().MutableDocument()->SetKeyPrefix(GetIsPrefixed());
    IndexMessages(messages, REALTIME, 1);
    TVector<TDocSearchInfo> results;
    QuerySearch(WideToChar(u"частично", CODES_YANDEX) + "&kps=" + ToString(GetIsPrefixed()), results);
    if (results.size() != 0)
        ythrow yexception() << "not Utf8 chars not cuted";
    QuerySearch(WideToChar(u"пример", CODES_YANDEX) + "&kps=" + ToString(GetIsPrefixed()), results);
    if (results.size() != 1)
        ythrow yexception() << "Utf8 chars cuted";
    return true;
}
};
