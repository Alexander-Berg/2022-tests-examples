#include <saas/rtyserver_test/testerlib/rtyserver_test.h>

START_TEST_DEFINE(TestFindResultsOrder)
    bool Run() override {
        TVector<TString> texts;
        texts.push_back("Настройка");
        texts.push_back("Настройка нашего любимого пользователя");
        texts.push_back("Настройки нашего любимого пользователя");
        texts.push_back("Пользователь любит наcтройки");
        texts.push_back("Ни пользователя ни настройки");
        texts.push_back("Пользователь пользователь и ещё раз пользователь");
        texts.push_back("Где настройки пользователя не могу найти");
        texts.push_back("Настройки пользователя");
        const TString textForSearch = "настройки пользователя";

        TVector<NRTYServer::TMessage> messages;
        const bool isPrefixed = GetIsPrefixed();
        for(TVector<TString>::const_iterator i = texts.begin(), e = texts.end(); i != e; ++i) {
            GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, isPrefixed, TAttrMap(), *i);
            if (isPrefixed)
                messages.back().MutableDocument()->SetKeyPrefix(1);
        }

        TSet<TString> firstUrls;
        firstUrls.insert(messages.back().GetDocument().GetUrl());
        firstUrls.insert(messages[messages.size()-2].GetDocument().GetUrl());

        INFO_LOG << "Testing memory index" << Endl;
        IndexMessages(messages, REALTIME, 1);

        TVector<TDocSearchInfo> results;
        TString query = textForSearch;
        if (messages.back().GetDocument().GetKeyPrefix())
            query += "&kps=" + ToString(messages.back().GetDocument().GetKeyPrefix());
        QuerySearch(query+"&ps=da&how=rlv", results);
        if (!results.ysize()) {
            ythrow yexception() << "Query results not found";
        }
        for (size_t i = 0; i < firstUrls.size(); i++) {
            if (firstUrls.find(results[i].GetUrl()) == firstUrls.end()) {
                ythrow yexception() << "Query results are incorrect: " << results[i].GetUrl();
            }
        }
        return true;
    }

};

SERVICE_TEST_RTYSERVER_DEFINE(TestFindResultsOrderService)

    TVector<NRTYServer::TMessage> messages;
    TString textForSearch;

    class TAttrCmp {
    public:
        typedef NRTYServer::TMessage value_t;

        inline bool operator()(const value_t& value1, const value_t& value2) const {

            i64 single1 = GetGroupAttrValue(value1, "single");
            i64 single2 = GetGroupAttrValue(value2, "single");
            i64 aa1 = GetGroupAttrValue(value1, "attr_aa_grp");
            i64 aa2 = GetGroupAttrValue(value2, "attr_aa_grp");
            if (single1 > single2)
                return 0;
            else if (single1 == single2)
                return aa1 < aa2;
            else
                return 1;
        }
    };

    class TAttrCmpBackInternal {
    public:
        typedef NRTYServer::TMessage value_t;

        inline bool operator()(const value_t& value1, const value_t& value2) const {

            i64 single1 = GetGroupAttrValue(value1, "single");
            i64 single2 = GetGroupAttrValue(value2, "single");
            i64 aa1 = GetGroupAttrValue(value1, "attr_aa_grp");
            i64 aa2 = GetGroupAttrValue(value2, "attr_aa_grp");
            if (single1 > single2)
                return 0;
            else if (single1 == single2)
                return aa1 > aa2;
            else
                return 1;
        }
    };

    static i64 GetGroupAttrValue(const NRTYServer::TMessage& message, TString nameParam) {
        for (int index = 0; index < message.GetDocument().GetGroupAttributes().size(); index++)
            if (message.GetDocument().GetGroupAttributes().Get(index).name() == nameParam)
                return FromString<i64>(message.GetDocument().GetGroupAttributes().Get(index).value());
        VERIFY_WITH_LOG(false, "incorrect group attr name for message");
        return 0;
    }

    void CheckOrder(TString query, const TVector<NRTYServer::TMessage>& order, bool isParticularOrderCheck = false) {
        TVector<TDocSearchInfo> results;
        TString queryKps = query;
        if (GetIsPrefixed())
            queryKps += "&kps=1";

        QuerySearch(queryKps, results);
        if (isParticularOrderCheck && results.size() < order.size()) {
            ythrow yexception() << "Query results count is lesser then order check-list";
        } else if (!isParticularOrderCheck && results.size() != order.size()) {
            ythrow yexception() << "Query results count is incorrect";
        }
        for (size_t iCheck = 0; iCheck < order.size(); iCheck++) {
            const TString& Url1 = results[iCheck].GetUrl();
            const TString& Url2 = order[iCheck].GetDocument().GetUrl();
            if (Url1 != Url2) {
                ythrow yexception() << "Incorrect order of documents";
            }
        }
    }

    void BuildAndIndexDocs(TVector<TString> texts) {
        const bool isPrefixed = GetIsPrefixed();
        for(TVector<TString>::const_iterator i = texts.begin(), e = texts.end(); i != e; ++i) {
            int CountInternal = 5;
            GenerateInput(messages, CountInternal, NRTYServer::TMessage::ADD_DOCUMENT, isPrefixed, TAttrMap(), *i);
            for (int iMess = 1; iMess <= CountInternal; iMess++) {
                ::NRTYServer::TMessage::TDocument* md = messages[messages.size() - iMess].MutableDocument();
                ::NRTYServer::TAttribute* attr = md->AddGroupAttributes();
                attr->set_name("single");
                attr->set_value(ToString(i - texts.begin()));
                attr->set_type(::NRTYServer::TAttribute::INTEGER_ATTRIBUTE);
                ::NRTYServer::TMessage::TDocument::TProperty* prop = md->AddDocumentProperties();
                prop->set_name("single");
                prop->set_value(ToString(i - texts.begin()));

                attr = md->AddGroupAttributes();
                attr->set_name("attr_aa_grp");
                attr->set_value(ToString(6 - iMess));
                attr->set_type(::NRTYServer::TAttribute::INTEGER_ATTRIBUTE);
                prop = md->AddDocumentProperties();
                prop->set_name("attr_aa_grp");
                prop->set_value(ToString(6 - iMess));
                if (isPrefixed)
                    md->SetKeyPrefix(1);
            }
        }
        IndexMessages(messages, REALTIME, 1);
    }

};

SERVICE_TEST_RTYSERVER_DEFINE_PARENT(TestOrderGroupAttrService, TestFindResultsOrderService)

    void PrepareData() {
        TVector<TString> texts;
        texts.push_back("tuk puk");
        texts.push_back("tuk puk stuk");
        texts.push_back("tuk puk stuk gryuk");
        texts.push_back("tuk puk stuk gryuk bryams");
        texts.push_back("tuk puk stuk gryuk bryams tryams");
        texts.push_back("tuk puk stuk gryuk bryams tryams tararams");

        BuildAndIndexDocs(texts);
    }

    bool Test(bool isRealTime) {

        PrepareData();

        if (!isRealTime)
            ReopenIndexers();

        StableSort(messages.rbegin(), messages.rend(), TAttrCmpBackInternal());
        CheckOrder("tuk puk&how=single&g=1.single.10.10.-1.0.0.-1.attr_aa_grp.1", messages);

        StableSort(messages.rbegin(), messages.rend(), TAttrCmp());
        CheckOrder("tuk puk&how=single&g=1.single.10.10.-1.0.0.-1.attr_aa_grp.0", messages);

        return true;
    }
};

SERVICE_TEST_RTYSERVER_DEFINE_PARENT(TestOrderRelevanceService, TestFindResultsOrderService)
    void PrepareData() {

        TVector<TString> texts;
        texts.push_back("tuk puk aaa bbb ccc");
        texts.push_back("tuk puk stuk");
        texts.push_back("tuk puk stuk gryuk");
        texts.push_back("tuk puk stuk gryuk bryams");
        texts.push_back("tuk puk stuk gryuk bryams tryams");
        texts.push_back("tuk puk stuk gryuk bryams tryams tararams");

        BuildAndIndexDocs(texts);
    }

    bool Test(bool isRealTime) {

        PrepareData();

        messages.erase(messages.begin() + 5, messages.end());
        if (!isRealTime)
            ReopenIndexers();
        StableSort(messages.begin(), messages.end(), TAttrCmp());
        CheckOrder("(tuk | puk | stuk) <- (tuk puk aaa)&g=1.single.10.10.-1.0.0.-1.attr_aa_grp.1", messages, true);

        StableSort(messages.rbegin(), messages.rend(), TAttrCmp());
        CheckOrder("(tuk | puk | stuk) <- (tuk puk aaa)&g=1.single.10.10.-1.0.0.-1.attr_aa_grp.0", messages, true);

        return true;
    }
};

START_TEST_DEFINE_PARENT(TestFindResultsInternalOrderGroupingMemory, TestOrderGroupAttrService)
    bool Run() override {
        Test(true);
        return true;

    }
};

START_TEST_DEFINE_PARENT(TestFindResultsInternalOrderGroupingDisk, TestOrderGroupAttrService)
    bool Run() override {
        Test(false);
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestFindResultsRlvOrderGroupingDisk, TestOrderRelevanceService)
    bool Run() override {
        Test(false);
        return true;

    }
    bool InitConfig() override {
        SetIndexerParams(ALL, 1000, 10, 1000);
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestFindResultsRlvOrderGroupingMemory, TestOrderRelevanceService)
    bool Run() override {
        Test(true);
        return true;

    }
};

START_TEST_DEFINE(TestAdvqFormula)
void Check(const TString& relev, const TString& kps, const NRTYServer::TMessage& first, const NRTYServer::TMessage& second, const NRTYServer::TMessage& third) {
    TVector<TDocSearchInfo> results;
    QuerySearch("body&dbgrlv=da&fsgta=_JsonFactors&relev=" + relev + kps, results);
    if (results.size() != 3)
        ythrow yexception() << "Query results count is incorrect: " << results.size() << " != 3, " << relev;
    if (results[0].GetUrl() != first.GetDocument().GetUrl() || results[1].GetUrl() != second.GetDocument().GetUrl() || results[2].GetUrl() != third.GetDocument().GetUrl())
        ythrow yexception() << "Incorrect order for " << relev << ": " << results[0].GetUrl() << ", " << results[1].GetUrl() << ", " << results[2].GetUrl();
}

bool Run() override {
    TAttrMap map(3);
    map[0]["gr1"] = 1;
    map[0]["gr2"] = 2;
    map[0]["gr3"] = 3;
    map[1]["gr3"] = 4;
    map[1]["gr4"] = 5;
    map[1]["gr5"] = 6;
    map[2]["gr5"] = 7;
    map[2]["gr6"] = 8;
    map[2]["gr7"] = 9;
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, map.size(), NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), map);
    IndexMessages(messages, REALTIME, 1);
    const TString kps(GetAllKps(messages));
    Check("calc=user0:sum(#group_gr1,#group_gr2,#group_gr3);formula=100100SDIO0L3", kps, messages[0], messages[1], messages[2]);
    Check("calc=user0:sum(#group_gr3,#group_gr4,#group_gr5);formula=100100SDIO0L3", kps, messages[1], messages[2], messages[0]);
    Check("calc=user0:sum(#group_gr5,#group_gr6,#group_gr7);formula=100100SDIO0L3", kps, messages[2], messages[1], messages[0]);
    return true;
}

bool InitConfig() override {
    (*ConfigDiff)["Searcher.FiltrationModel"] = "SIMPLE";
    (*ConfigDiff)["Searcher.FactorsInfo"] = GetResourcesDirectory() + "/factors/advq_factors.cfg";
    return true;
}
};
