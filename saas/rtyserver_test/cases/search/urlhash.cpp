#include <saas/rtyserver_test/testerlib/rtyserver_test.h>

START_TEST_DEFINE(TestInternalBroadcastFetch)
public:
    bool InitConfig() override {
        SetIndexerParams(ALL, 600, 1);
        SetMergerParams(true, 1, -1, mcpNONE);
        (*ConfigDiff)["Searcher.EnableUrlHash"] = "true";
        (*ConfigDiff)["Searcher.BroadcastFetch"] = "true";
        (*ConfigDiff)["Indexer.Memory.MaxDocumentsReserveCapacityCoeff"] = 2;
        return true;
    }

    bool Run() override {
        TVector<NRTYServer::TMessage> mass;
        GenerateInput(mass, 10, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
        IndexMessages(mass, REALTIME, 1);

        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, 1, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
        messages[0].MutableDocument()->SetBody("abc");
        const TString kps = "&kps=" + ToString(messages[0].GetDocument().GetKeyPrefix());

        TVector<NRTYServer::TMessage> first = messages;
        TVector<NRTYServer::TMessage> second = messages;
        AddProperty(first[0], "fake_plastic", "trees");
        AddProperty(second[0], "fake_plastic", "knees");

        IndexMessages(first, REALTIME, 1);
        ReopenIndexers();

        TVector<TDocSearchInfo> results;
        TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > > docProperties;
        QuerySearch("abc" + kps, results);
        if (results.size() != 1) {
            ythrow yexception() << "incorrect results count: " << results.size();
        }

        const TString fullDocId = results[0].GetFullDocId();
        QuerySearch("abc&gta=fake_plastic&noqtree=1&DF=da&dh=" + fullDocId, results, &docProperties, nullptr, true);
        if (results.size() != 1) {
            ythrow yexception() << "incorrect results count: " << results.size();
        }

        auto p = docProperties[0]->find("fake_plastic");
        if (docProperties[0]->find("fake_plastic") == docProperties[0]->end()) {
            ythrow yexception() << "no fake plastic nothing";
        }
        if (p->second != "trees") {
            ythrow yexception() << "no fake plastic trees";
        }

        IndexMessages(second, REALTIME, 1);
        ReopenIndexers();
        Sleep(TDuration::Seconds(10));
        QuerySearch("abc&gta=fake_plastic&noqtree=1&DF=da&dh=" + fullDocId, results, &docProperties, nullptr, true);
        if (results.size() != 1) {
            ythrow yexception() << "incorrect results count: " << results.size();
        }

        p = docProperties[0]->find("fake_plastic");
        if (docProperties[0]->find("fake_plastic") == docProperties[0]->end()) {
            ythrow yexception() << "no fake plastic nothing";
        }
        if (p->second != "knees") {
            ythrow yexception() << "no fake plastic knees";
        }

        Sleep(TDuration::Seconds(10));
        QuerySearch("abcdefg&gta=fake_plastic&noqtree=1&DF=da&dh=" + fullDocId, results, &docProperties, nullptr, true);
        if (results.size() != 1) {
            ythrow yexception() << "incorrect results count: " << results.size();
        }

        p = docProperties[0]->find("fake_plastic");
        if (docProperties[0]->find("fake_plastic") == docProperties[0]->end()) {
            ythrow yexception() << "no fake plastic nothing";
        }
        if (p->second != "knees") {
            ythrow yexception() << "no fake plastic knees";
        }

        return true;
    }
};

START_TEST_DEFINE(TestDocFilteringStatus)
public:
    bool InitConfig() override {
        (*ConfigDiff)["Searcher.EnableUrlHash"] = "true";
        return true;
    }

    void CheckQLossStatus(const TString& request, const ui32 count,
        const TString& status, const TString& caseName) {

        TVector<TDocSearchInfo> results;
        TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > > docProperties;
        THashMultiMap<TString, TString> searchProperties;

        QuerySearch(request, results, &docProperties, &searchProperties, true);
        if (results.size() != count) {
            ythrow yexception() << "incorrect results count: " << results.size() << ", expected: " << count << ", case: " << caseName;
        }
        THashMultiMap<TString, TString>::const_iterator i = searchProperties.find("QLoss.debug");
        if (i == searchProperties.end()) {
            ythrow yexception() << "no QLoss.debug property for " << caseName << " case";
        }
        if (i->second.find(status) == TString::npos) {
            ythrow yexception() << "QLoss.debug must contain '" << status << "', got: " << i->second;
        }
    }

    bool Run() override {
        TVector<NRTYServer::TMessage> messages0;
        GenerateInput(messages0, 3, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
        messages0[0].MutableDocument()->SetBody("abc");
        const ui64 kpsUi = messages0[0].GetDocument().GetKeyPrefix();
        const TString kps = "&kps=" + ToString(kpsUi);
        messages0[1].MutableDocument()->SetBody("abc");
        messages0[1].MutableDocument()->SetKeyPrefix(kpsUi);
        messages0[2].MutableDocument()->SetBody("def");
        messages0[2].MutableDocument()->SetKeyPrefix(kpsUi);

        IndexMessages(messages0, REALTIME, 1);

        TVector<TDocSearchInfo> results;
        TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > > docProperties;
        THashMultiMap<TString, TString> searchProperties;

        QuerySearch("abc" + kps, results);
        if (results.size() != 2) {
            ythrow yexception() << "incorrect results count: " << results.size();
        }
        const TString fullDocId0 = results[0].GetFullDocId();
        const TString url = results[0].GetUrl();
        CheckQLossStatus("abc&pron=doc_filtering_status_" + fullDocId0 + kps, 2, "PASSED", "memory_index");

        ReopenIndexers();
        CheckQLossStatus("abc&pron=doc_filtering_status_" + fullDocId0 + kps, 2, "PASSED", "final_index");

        CheckQLossStatus("def&pron=doc_filtering_status_" + fullDocId0 + kps, 1, "NOT_TOUCHED", "not_suitable_doc");

        TString fullDocIdAbsent = fullDocId0.substr(0, fullDocId0.length() - 3) + "789";
        CheckQLossStatus("def&pron=doc_filtering_status_" + fullDocIdAbsent + kps, 1, "NOT_IN_BASE", "absent_docid");

        DeleteQueryResult("url:\"" + url + "\"" + kps, REALTIME);
        CheckQLossStatus("abc&pron=doc_filtering_status_" + fullDocId0 + kps, 1,
            "FILTERED_BY_DYNAMIC_ANTISPAM_PREPRUNING", "deleted_doc");

        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, 1, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
        messages[0].MutableDocument()->SetUrl(url);
        messages[0].MutableDocument()->SetBody("abc def");
        messages[0].MutableDocument()->SetKeyPrefix(kpsUi);

        IndexMessages(messages, REALTIME, 1);
        CheckQLossStatus("abc&pron=doc_filtering_status_" + fullDocId0 + kps, 2, "PASSED", "add_after_delete");

        ReopenIndexers();
        CheckQLossStatus("abc&pron=doc_filtering_status_" + fullDocId0 + kps, 2, "PASSED", "add_after_delete_final");

        return true;
    }
};
