#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <search/idl/meta.pb.h>

SERVICE_TEST_RTYSERVER_DEFINE(TestPruningSearchBase)
virtual void SetPruningValue(NRTYServer::TMessage::TDocument& doc, ui64 value) const = 0;
virtual TString GetHow() const = 0;
bool Run() override {
    if (GetIsPrefixed())
        return true;
    const ui64 countMessages = 50;
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    for (ui64 i = 0; i < countMessages; ++i) {
        SetPruningValue(*messages[i].MutableDocument(), i + 1);
    }
    messages.front().MutableDocument()->SetBody("body abv");
    IndexMessages(messages, DISK, 1, 0, true, true, TDuration(), TDuration(), 8);
    TVector<TDocSearchInfo> results;
    ReopenIndexers();
    TString how = "&how=" + GetHow();
    QuerySearch("body""&numdoc=10" + how, results);
    if (results.size() != 10)
        ythrow yexception() << "pruning does not work on disk";
    if (results[0].GetUrl() != messages.back().GetDocument().GetUrl())
        ythrow yexception() << "incorrect sort order with pruning";

    QuerySearch("body""&numdoc=10&asc=yes" + how, results);
    if (results.size() != 10)
        ythrow yexception() << "error in search with asc";
    if (results[0].GetUrl() != messages.front().GetDocument().GetUrl())
        ythrow yexception() << "incorrect sort order with asc";

    QuerySearch("body""&numdoc=10&p=3" + how, results);
    if (results.size() != 10)
        ythrow yexception() << "pruning does not work on disk";
    QuerySearch("body abv&numdoc=10000&how=rlv", results);
    if (results[0].GetUrl() != messages.front().GetDocument().GetUrl())
        ythrow yexception() << "incorrect sort order without pruning";
    return true;
}
bool InitConfig() override {
    SetIndexerParams(DISK, 100000, 1);
    SetIndexerParams(REALTIME, 100, 1);
    SetPruneAttrSort(GetHow());
    (*ConfigDiff)["Searcher.FactorsInfo"] = GetResourcesDirectory() + "/factors/zone_factors.cfg";
    return true;
}
};

START_TEST_DEFINE_PARENT(TestPruningSearch, TestPruningSearchBase)
void SetPruningValue(NRTYServer::TMessage::TDocument& doc, ui64 value) const override {
    auto attr = doc.AddGroupAttributes();
    attr->SetName(GetHow());
    attr->SetValue(ToString(value));
    attr->SetType(NRTYServer::TAttribute::INTEGER_ATTRIBUTE);
}

TString GetHow() const override {
    return "unique_attr";
}
};

START_TEST_DEFINE_PARENT(TestPruningSearchTm, TestPruningSearchBase)
void SetPruningValue(NRTYServer::TMessage::TDocument& doc, ui64 value) const override {
    doc.SetModificationTimestamp(1288180384 + value);
}

TString GetHow() const override {
    return "tm";
}
};

START_TEST_DEFINE(TestPruningSearchMergeResults)
bool Run() override {
    const ui64 countMessages = 50;
    TVector<NRTYServer::TMessage> messagesOdd, messagesEven;
    TAttrMap attrsEven(countMessages);
    TAttrMap attrsOdd(countMessages);
    for (ui64 i = 0; i < countMessages; ++i) {
        attrsEven[i]["unique_attr"] = 2 * i;
        attrsOdd[i]["unique_attr"] = 2 * i + 1;
    }
    GenerateInput(messagesEven, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), attrsEven);
    GenerateInput(messagesOdd, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), attrsOdd);

    if (GetIsPrefixed()) {
        for (ui64 i = 0; i < countMessages; ++i) {
            messagesEven[i].MutableDocument()->SetKeyPrefix(1);
            messagesOdd[i].MutableDocument()->SetKeyPrefix(1);
        }
    }

    IndexMessages(messagesEven, DISK, 1, 0, true, true, TDuration(), TDuration(), 8);
    ReopenIndexers();
    IndexMessages(messagesOdd, DISK, 1, 0, true, true, TDuration(), TDuration(), 8);
    ReopenIndexers();

    TVector<TDocSearchInfo> results;
    if (GetIsPrefixed())
        QuerySearch("body""&numdoc=10&how=unique_attr&kps=1", results);
    else
        QuerySearch("body""&numdoc=10&how=unique_attr", results);
    if (results.size() != 10)
        ythrow yexception() << "pruning does not work on disk";

    for (size_t i = 0; i < results.size(); i++) {
        TString resultUrl = results[i].GetUrl();
        TString checkOddUrl = messagesOdd[messagesOdd.size() - 1 - i / 2].MutableDocument()->GetUrl();
        TString checkEvenUrl = messagesEven[messagesEven.size() - 1 - i / 2].MutableDocument()->GetUrl();
        if ((!(i % 2) && (resultUrl != checkOddUrl)) ||
           (((i % 2)) && (resultUrl != checkEvenUrl)))
            ythrow yexception() << "incorrect merging with pruning";
    }
    return true;
}
bool InitConfig() override {
    SetIndexerParams(DISK, 100000, 1);
    SetIndexerParams(REALTIME, 100, 1);
    SetPruneAttrSort("unique_attr");
    return true;
}
};
