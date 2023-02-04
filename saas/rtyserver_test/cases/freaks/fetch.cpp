#include <saas/rtyserver_test/testerlib/rtyserver_test.h>

START_TEST_DEFINE(TestIncorrectBroadcastFetch)
bool InitConfig() override {
    SetIndexerParams(ALL, 600, 1);
    SetMergerParams(true, 1, -1, mcpNONE);

    (*SPConfigDiff)["Service.MetaSearch.TwoStepQuery"] = "true";
    (*SPConfigDiff)["Service.MetaSearch.InsensitiveClientNumFetch"] = "true";
    (*SPConfigDiff)["Service.MetaSearch.SwitchToNextSourceFetchStage"] = "true";
    return true;
}

size_t ValidDocsSize(const TVector<TDocSearchInfo>& results) {
    size_t ret = 0;
    for (auto&& doc : results) {
        if (doc.UrlInitialized())
            ++ret;
    }
    return ret;
}

bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 5, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    IndexMessages(messages, REALTIME, 1);
    TString kps = GetAllKps(messages);
    ReopenIndexers();
    TVector<TDocSearchInfo> results;
    QuerySearch("url:\"*\"&haha=da" + kps, results);
    if (results.size() != 5)
        ythrow yexception() << "document not found";
    TString dh1 = results[0].GetFullDocId();
    TString dh3 = results[1].GetFullDocId().substr(results[1].GetFullDocId().find("-") + 1);

    QuerySearch("url:\"*\"&noqtree=1&broadcast_fetch=1&DF=da&dh=" + dh1 + "&dh=" + dh3 + "&" + kps, results);
    if (ValidDocsSize(results) != 0) {
        ythrow yexception() << "incorrect broadcast_fetch worked";
    }
    return true;
}

};
