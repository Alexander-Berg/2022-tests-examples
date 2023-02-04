#include "mix_factors.h"
#include <saas/rtyserver_test/util/factors_parsers.h>
#include <saas/api/factors_erf.h>
#include <library/cpp/string_utils/quote/quote.h>
#include <util/generic/ymath.h>

bool TestMixFactorsBase::CheckResults(const TString& query, const TString& qsQuery, const TString& cgi, i32 mustBeCount) {
    TVector<TDocSearchInfo> results;
    TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > > resultProps;
    TString queryQuote = query;
    TString qsQueryQuote = qsQuery;
    Quote(queryQuote);
    Quote(qsQueryQuote);
    ui32 resCount = 0;
    for (ui32 p = 0; ; ++p) {
        QuerySearch(queryQuote + "&numdoc=750&" + cgi + "&qs_req=" + qsQueryQuote + "&dbgrlv=da&fsgta=_JsonFactors&" + GetAllKps(Messages) + "&p=" + ToString(p), results, &resultProps);
        TVector<THashMap<TString, double> > factors = TRTYFactorsParser::GetJsonFactorsValues(resultProps);
        resCount += results.size();
        for (ui32 i = 0; i < results.size(); ++i) {
            TFactorsByUrl::const_iterator itUrl = FactorsByUrl.find(results[i].GetUrl());
            const TFactors* factorValues = nullptr;
            if (itUrl != FactorsByUrl.end()) {
                TVectorFactorsByQuery::const_iterator itQ = itUrl->second.find(qsQuery);
                if (itQ != itUrl->second.end()) {
                    factorValues = &itQ->second;
                }
            }
            for (THashMap<TString, double>::const_iterator itF = factors[i].begin(), itE = factors[i].end(); itF != itE; ++itF) {
                float valueTest = 0;
                if (factorValues) {
                    TFactors::const_iterator itCompFactor = factorValues->find(itF->first);
                    if (itCompFactor != factorValues->end())
                        valueTest = itCompFactor->second;
                }
                if (valueTest == Max<float>()) {
                    if (Abs(itF->second) < 0.001)
                        TEST_FAILED("Incorrect " + itF->first + " factor value == 0");
                }
                else {
                    if (Abs(itF->second) < 0.0001) {
                        if (Abs(valueTest) > 0.0001)
                            TEST_FAILED("Incorrect " + itF->first + " factor value " + ToString(itF->second) + " != " + ToString(valueTest));
                    }
                    else {
                        if (Abs(itF->second - valueTest) / Abs(itF->second) > 0.005)
                            TEST_FAILED("Incorrect " + itF->first + " factor value " + ToString(itF->second) + " != " + ToString(valueTest));
                    }
                }
            }
        }
        if (results.size() < 750)
            break;
    }
    if (resCount != (mustBeCount < 0 ? FactorsByUrl.size() : mustBeCount))
        ythrow yexception() << "document count is incorrect: " << resCount << " != " << FactorsByUrl.size();
    return true;
}

void TestMixFactorsBase::GenerateMessages(size_t count) {
    FactorsByUrl.clear();
    GenerateInput(Messages, count, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), "<xml><aaa>a b c</aaa><bbb>aa bb cc</bbb></xml>");
    for (ui32 docModif = 0; docModif < Messages.size(); ++docModif) {
        NRTYServer::TMessage::TDocument* doc = Messages[docModif].MutableDocument();
        const TString& url = doc->GetUrl();
        doc->SetMimeType("text/xml");
        doc->SetKeyPrefix(Messages[0].GetDocument().GetKeyPrefix());

        // CS
        FactorsByUrl[url]["a b c"]["cs_aaa"] = docModif * 2;
        FactorsByUrl[url]["a b c"]["cs_bbb"] = 1000000 - docModif * 2;
        FactorsByUrl[url]["aa bb cc"]["cs_aaa"] = docModif * 2;
        FactorsByUrl[url]["aa bb cc"]["cs_bbb"] = 1000000 - docModif * 2;
        NRTYServer::TMessage::TCSInfo* csInfo = doc->AddCSInfo();
        csInfo->SetName("CSDomain");
        csInfo->MutableFactors()->AddNames("cs_aaa");
        csInfo->MutableFactors()->AddNames("cs_bbb");
        csInfo->MutableFactors()->MutableValues()->AddValues()->SetValue(docModif * 2);
        csInfo->MutableFactors()->MutableValues()->AddValues()->SetValue(1000000 - docModif * 2);

        // QS
        FactorsByUrl[url]["a b c"]["qs_aaa"] = 2 * (docModif + 1);
        FactorsByUrl[url]["a b c"]["qs_bbb"] = 1 * (docModif + 1);
        FactorsByUrl[url]["aa bb cc"]["qs_aaa"] = 4 * (docModif + 1);
        FactorsByUrl[url]["aa bb cc"]["qs_bbb"] = 2 * (docModif + 1);
        NRTYServer::TMessage::TQSInfo* qsInfo = doc->AddQSInfo();
        qsInfo->SetName("QSText");
        qsInfo->AddFactorNames("qs_aaa");
        qsInfo->AddFactorNames("qs_bbb");
        NRTYServer::TMessage::TFactorsByKey* qsFactorsInfo = qsInfo->AddFactors();
        qsFactorsInfo->SetKey("a b c");
        qsFactorsInfo->MutableValues()->AddValues()->SetValue(2 * (docModif + 1));
        qsFactorsInfo->MutableValues()->AddValues()->SetValue(1 * (docModif + 1));
        qsFactorsInfo = qsInfo->AddFactors();
        qsFactorsInfo->SetKey("aa bb cc");
        qsFactorsInfo->MutableValues()->AddValues()->SetValue(4 * (docModif + 1));
        qsFactorsInfo->MutableValues()->AddValues()->SetValue(2 * (docModif + 1));

        //static
        FactorsByUrl[url]["a b c"]["start"] = 5 * (docModif + 1);
        FactorsByUrl[url]["a b c"]["finish"] = 7 * (docModif + 1);
        FactorsByUrl[url]["aa bb cc"]["start"] = 5 * (docModif + 1);
        FactorsByUrl[url]["aa bb cc"]["finish"] = 7 * (docModif + 1);

        NSaas::AddSimpleFactor("start", ToString(5 * (docModif + 1)), *doc->MutableFactors());
        NSaas::AddSimpleFactor("finish", ToString(7 * (docModif + 1)), *doc->MutableFactors());

        //user
        FactorsByUrl[url]["a b c"]["user1"] = 5 * (docModif + 1);
        FactorsByUrl[url]["a b c"]["user2"] = 7 * (docModif + 1);
        FactorsByUrl[url]["aa bb cc"]["user1"] = 7 * (docModif + 1);
        FactorsByUrl[url]["aa bb cc"]["user2"] = 5 * (docModif + 1);

        //zone
        FactorsByUrl[url]["a b c"]["_BM25F_St_aaa"] = Max<float>();
        FactorsByUrl[url]["a b c"]["_BM25F_St_bbb"] = 0;
        FactorsByUrl[url]["aa bb cc"]["_BM25F_St_aaa"] = 0;
        FactorsByUrl[url]["aa bb cc"]["_BM25F_St_bbb"] = Max<float>();

        //dynamic
        FactorsByUrl[url]["a b c"]["TextBM25"] = Max<float>();
        FactorsByUrl[url]["a b c"]["TRDocQuorum"] = Max<float>();
        FactorsByUrl[url]["aa bb cc"]["TextBM25"] = Max<float>();
        FactorsByUrl[url]["aa bb cc"]["TRDocQuorum"] = Max<float>();
        //rty_dynamic_factors
        FactorsByUrl[url]["a b c"]["LiveTime"] = Max<float>();
        FactorsByUrl[url]["a b c"]["InvLiveTime"] = Max<float>();
        FactorsByUrl[url]["aa bb cc"]["LiveTime"] = Max<float>();
        FactorsByUrl[url]["aa bb cc"]["InvLiveTime"] = Max<float>();
    }
}

void TestMixFactorsBase::CheckResults(i32 mustBeCount) {
    if (!CheckResults("aaa:(a b c)", "a b c", "relev=set=user1:start,user2:finish", mustBeCount))
        return ythrow yexception() << "first check";
    if (!CheckResults("bbb:(aa bb cc)", "aa bb cc", "relev=set=user1:finish,user2:start", mustBeCount))
        return ythrow yexception() << "second check";
}

bool TestMixFactorsBase::InitConfig() {
    (*ConfigDiff)["Searcher.FiltrationModel"] = "SIMPLE";
    (*ConfigDiff)["Searcher.FactorsInfo"] = GetResourcesDirectory() + "/factors/mix_factors.cfg";
    (*ConfigDiff)["Indexer.Common.XmlParserConfigFile"] = "";
    return true;
}

START_TEST_DEFINE_PARENT(TestMixFactors, TestMixFactorsBase)
    bool Run() override {
        GenerateMessages(10);
        IndexMessages(Messages, REALTIME, 1);
        ReopenIndexers();
        sleep(3);
        CheckResults();
        Sleep(TDuration::Seconds(5));
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestMixFactorsRepair, TestMixFactorsBase)
    bool Run() override {
        GenerateMessages(10);
        IndexMessages(Messages, REALTIME, 1);
        Controller->RestartServer(true);
        Controller->WaitIsRepairing();
        ReopenIndexers();
        CheckResults();
        return true;
    }
    bool InitConfig() override {
        TestMixFactorsBase::InitConfig();
        SetEnabledRepair();
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestMixFactorsWidth, TestMixFactorsBase)
void GenMsgs() {
    FactorsByUrl.clear();
    GenerateInput(Messages, 10, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    for (ui32 docModif = 0; docModif < Messages.size(); ++docModif) {
        NRTYServer::TMessage::TDocument* doc = Messages[docModif].MutableDocument();
        const TString& url = doc->GetUrl();
        doc->SetKeyPrefix(Messages[0].GetDocument().GetKeyPrefix());
        //static
        TFactors& factors = FactorsByUrl[url]["body"];
        factors["f32"] = Max<float>() - 1;
        factors["f16"] = 0.2f / 3;
        factors["f8"] = 0.3294117749;
        factors["i8"] = Max<i8>() - 3;
        factors["i16"] = Max<i16>() - 5;
        factors["i32"] = Max<i32>() / 4 - 4;
        for (TFactors::const_iterator i = factors.begin(); i != factors.end(); ++i)
            NSaas::AddSimpleFactor(i->first, ToString(i->second), *doc->MutableFactors());
    }
}

bool Run() override {
    GenMsgs();
    IndexMessages(Messages, REALTIME, 1);
    sleep(3);
    if (!CheckResults("body", "body", ""))
        ythrow yexception() << "memory case";
    ReopenIndexers();
    if (!CheckResults("body", "body", ""))
        ythrow yexception() << "disk case";
    return true;
}

bool InitConfig() override {
    TestMixFactorsBase::InitConfig();
    (*ConfigDiff)["Searcher.FactorsInfo"] = GetResourcesDirectory() + "/factors/mix_factors_width.cfg";
    return true;
}
};
