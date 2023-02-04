#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <library/cpp/string_utils/quote/quote.h>

START_TEST_DEFINE(TestSearchProxyRedirect)
bool Run() override {
    const int CountMessages = 1000;
    const int NumDoc = 100;
    TVector<NRTYServer::TMessage> messagesToMem;
    GenerateInput(messagesToMem, CountMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    IndexMessages(messagesToMem, REALTIME, 1);

    const TString kps = GetIsPrefixed() ? GetAllKps(messagesToMem) : "";
    TVector<TDocSearchInfo> results;
    QuerySearch("body&p=4&numdoc=" + ToString(NumDoc) + kps, results);
    size_t p = 4 * NumDoc;
    for (size_t i = 0; i < NumDoc; ++i) {
        const TString& marker = "/p=" + ToString(p) + "/";
        if (results[i].GetUrl().find(marker) == TString::npos)
            ythrow yexception() << "Incorrect redirect url " << results[i].GetUrl();
        ++p;
    }

    return true;
}
bool InitConfig() override {
    if (!SendIndexReply) {
        SetIndexerParams(ALL, 2000, -1, 0);
    }
    (*SPConfigDiff)["SearchConfig.ReArrangeOptions"] = "RTYRedirect(url=ohffs.com,ReplaceUrl=1)";
    return true;
}
};

START_TEST_DEFINE(TestSearchProxyClickUrl)
virtual bool CountGroups() {
    return true;
}
bool Run() override {
    const int CountMessages = 100;

    TAttrMap attrMap;
    attrMap.resize(CountMessages);
    for (size_t i = 0; i < CountMessages; i++) {
        attrMap[i]["gr_attr"] = i % 5;
    }

    TVector<NRTYServer::TMessage> messagesToMem;
    GenerateInput(messagesToMem, CountMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), attrMap, "nobody", true);
    IndexMessages(messagesToMem, REALTIME, 1);

    const TString kps = GetIsPrefixed() ? GetAllKps(messagesToMem) : "";
    TVector<TDocSearchInfo> results;
    TVector<TSimpleSharedPtr<THashMultiMap<TString, TString>>> props;
    QuerySearch("url:\"*\"&g=1.gr_attr.10.3" + kps, results, &props);
    for (size_t i = 0; i < results.size(); ++i) {
        auto it = props[i]->find("clickUrl");
        CHECK_WITH_LOG(it != props[i]->end());
        DEBUG_LOG << "clickUrl=" << it->second << Endl;

        int p = CountGroups() ? i / 3 : i;
        const TString& marker = "/p=" + ToString(p) + "/";
        if (it->second.find(marker) == TString::npos)
            ythrow yexception() << "Incorrect click url " << it->second;

        if (CountGroups()) {
            TString saasUrl = results[i - i % 3].GetUrl();
            Quote(saasUrl, "");
            const TString& marker = "/saas_url=" + saasUrl + "/";
            if (it->second.find(marker) == TString::npos)
                ythrow yexception() << "Incorrect click url " << it->second;

        }
    }

    return true;
}
bool InitConfig() override {
    if (!SendIndexReply) {
        SetIndexerParams(ALL, 2000, -1, 0);
    }
    (*SPConfigDiff)["SearchConfig.ReArrangeOptions"] = "RTYRedirect(url=ohffs.com,CountGroups=" + ToString(CountGroups()) + ")";
    return true;
}
};

START_TEST_DEFINE_PARENT(TestSearchProxyClickUrlNoCountGrps, TTestSearchProxyClickUrlCaseClass)
bool CountGroups() override {
    return false;
}
};
