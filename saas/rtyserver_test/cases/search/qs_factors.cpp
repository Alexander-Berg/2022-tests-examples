#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/rtyserver_test/util/factors_parsers.h>
#include <util/string/vector.h>
#include <util/system/fs.h>
#include <kernel/keyinv/indexfile/searchfile.h>
#include <library/cpp/string_utils/quote/quote.h>
#include <util/generic/ymath.h>

SERVICE_TEST_RTYSERVER_DEFINE(TestQSFactorsCommon)
    TVector<NRTYServer::TMessage> Messages;
    typedef THashMap<TString, float> TFactors;
    typedef THashMap<TString, TFactors> TVectorFactorsByQuery;
    typedef THashMap<TString, TVectorFactorsByQuery> TFactorsByUrl;
    TFactorsByUrl FactorsByUrl;

    bool CheckResults(const TString& query, const TString& qsQuery) {
        TVector<TDocSearchInfo> results;
        TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > > resultProps;
        TString queryQuote = query;
        TString qsQueryQuote = qsQuery;
        Quote(queryQuote);
        Quote(qsQueryQuote);
        ui32 resCount = 0;
        bool failFlag = false;
        for (ui32 p = 0;; ++p) {
            for (ui32 attempt = 0; attempt < 3; ++attempt) {
                bool retry = false;
                QuerySearch(queryQuote + "&numdoc=750&qs_req=" + qsQueryQuote + "&dbgrlv=da&fsgta=_JsonFactors&" + GetAllKps(Messages) + "&p=" + ToString(p), results, &resultProps, nullptr, true);
                TVector<THashMap<TString, double> > factors = TRTYFactorsParser::GetJsonFactorsValues(resultProps);
                if (attempt == 0)
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
                        if (Abs(itF->second - valueTest) > 0.001) {
                            DEBUG_LOG << "Incorrect " << itF->first << " factor value: " << itF->second << " != " << valueTest << ". att = " << attempt << Endl;
                            if (attempt == 3) {
                                failFlag = true;
                            } else {
                                retry = true;
                            }
                        }
                    }
                }
                if (!retry)
                    break;
            }
            if (results.size() < 750) {
                break;
            }
        }
        if (failFlag) {
            TEST_FAILED("Incorrect factor values");
        }
        if (resCount != FactorsByUrl.size())
            ythrow yexception() << "document count is incorrect: " << resCount << " != " << FactorsByUrl.size();
        return true;
    }

};


START_TEST_DEFINE_PARENT(TestQSFactors, TestQSFactorsCommon)
bool Run() override {
    FactorsByUrl.clear();
    GenerateInput(Messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), "a b c aa bb cc");
    FactorsByUrl["testUrl"]["a b c"]["aaa"] = 2;
    FactorsByUrl["testUrl"]["a b c"]["bbb"] = 1;
    FactorsByUrl["testUrl"]["aa bb cc"]["aaa"] = 4;
    FactorsByUrl["testUrl"]["aa bb cc"]["bbb"] = 2;
    Messages[0].MutableDocument()->SetUrl("testUrl");
    NRTYServer::TMessage::TQSInfo* info = Messages[0].MutableDocument()->AddQSInfo();
    info->SetName("QSText");
    info->AddFactorNames("aaa");
    info->AddFactorNames("bbb");
    NRTYServer::TMessage::TFactorsByKey* factorsInfo = info->AddFactors();
    factorsInfo->SetKey("a b c");
    factorsInfo->MutableValues()->AddValues()->SetValue(2);
    factorsInfo->MutableValues()->AddValues()->SetValue(1);
    factorsInfo = info->AddFactors();
    factorsInfo->SetKey("aa bb cc");
    factorsInfo->MutableValues()->AddValues()->SetValue(4);
    factorsInfo->MutableValues()->AddValues()->SetValue(2);

    IndexMessages(Messages, REALTIME, 1);
    ReopenIndexers();
    if (!CheckResults("a b c softness:100", "a b c")) {
        return false;
    }
    if (!CheckResults("aa bb cc softness:100", "aa bb cc")) {
        return false;
    }
    return true;
}
bool InitConfig() override {
    (*ConfigDiff)["Searcher.FiltrationModel"] = "SIMPLE";
    (*ConfigDiff)["Searcher.FactorsInfo"] = GetResourcesDirectory() + "/factors/qs_factors.cfg";
    return true;
}
};

START_TEST_DEFINE_PARENT(TestQSFactorsQSCgi, TestQSFactorsCommon)
bool Run() override {
    FactorsByUrl.clear();
    GenerateInput(Messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), "a b c aa bb cc");
    FactorsByUrl["testUrl"]["a b c"]["aaa"] = 2;
    FactorsByUrl["testUrl"]["a b c"]["bbb"] = 1;
    FactorsByUrl["testUrl"]["aa bb cc"]["aaa"] = 4;
    FactorsByUrl["testUrl"]["aa bb cc"]["bbb"] = 2;
    Messages[0].MutableDocument()->SetUrl("testUrl");
    NRTYServer::TMessage::TQSInfo* info = Messages[0].MutableDocument()->AddQSInfo();
    info->SetName("QSCgi");
    info->AddFactorNames("eee");
    NRTYServer::TMessage::TFactorsByKey* factorsInfo = info->AddFactors();
    factorsInfo->SetKey("a b c");
    factorsInfo->MutableValues()->AddValues()->SetValue(1);
    factorsInfo = info->AddFactors();
    factorsInfo->SetKey("aa bb cc");
    factorsInfo->MutableValues()->AddValues()->SetValue(2);

    IndexMessages(Messages, REALTIME, 1);
    ReopenIndexers();
    CHECK_TEST_TRUE(CheckResults("a b c softness:100", "a b c"));
    CHECK_TEST_TRUE(CheckResults("aa bb cc softness:100", "aa bb cc"));
    return true;
}
bool InitConfig() override {
    (*ConfigDiff)["Searcher.FiltrationModel"] = "SIMPLE";
    (*ConfigDiff)["Searcher.FactorsInfo"] = GetResourcesDirectory() + "/factors/qs_factors2.cfg";
    return true;
}
};

START_TEST_DEFINE_PARENT(TestQSFactorsManyDocs, TestQSFactorsCommon)

    bool Run() override {
        FactorsByUrl.clear();
        ui32 maxDoc = GetMaxDocuments();
        GenerateInput(Messages, 2 * maxDoc, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), "a b c aa bb cc");

        for (ui32 docModif = 0; docModif < Messages.size(); ++docModif) {
            Messages[docModif].MutableDocument()->SetUrl(ToString(docModif));
            if (GetIsPrefixed())
                Messages[docModif].MutableDocument()->SetKeyPrefix(1 + docModif * GetShardsNumber());
            NRTYServer::TMessage::TQSInfo* info = Messages[docModif].MutableDocument()->AddQSInfo();
            info->SetName("QSText");
            info->AddFactorNames("aaa");
            info->AddFactorNames("bbb");
            NRTYServer::TMessage::TFactorsByKey* factorsInfo = info->AddFactors();
            factorsInfo->SetKey("a b c");
            factorsInfo->MutableValues()->AddValues()->SetValue(docModif * 2);
            factorsInfo->MutableValues()->AddValues()->SetValue(1000000 - docModif * 2);
            factorsInfo = info->AddFactors();
            factorsInfo->SetKey("aa bb cc");
            factorsInfo->MutableValues()->AddValues()->SetValue(docModif * 2 + 1);
            factorsInfo->MutableValues()->AddValues()->SetValue(1000000 - docModif * 2 - 1);
            FactorsByUrl[ToString(docModif)]["a b c"]["aaa"] = docModif * 2;
            FactorsByUrl[ToString(docModif)]["a b c"]["bbb"] = 1000000 - docModif * 2;
            FactorsByUrl[ToString(docModif)]["aa bb cc"]["aaa"] = docModif * 2 + 1;
            FactorsByUrl[ToString(docModif)]["aa bb cc"]["bbb"] = 1000000 - docModif * 2 - 1;
        }
        IndexMessages(Messages, REALTIME, 1);
        if (!CheckResults("a b c softness:100", "a b c"))
            return false;
        if (!CheckResults("aa bb cc softness:100", "aa bb cc"))
            return false;
        ReopenIndexers();
        if (!CheckResults("a b c softness:100", "a b c"))
            return false;
        if (!CheckResults("aa bb cc softness:100", "aa bb cc"))
            return false;
        Controller->ProcessCommand("create_merger_tasks");
        Controller->ProcessCommand("do_all_merger_tasks&wait=true");
        if (!CheckResults("a b c softness:100", "a b c"))
            return false;
        if (!CheckResults("aa bb cc softness:100", "aa bb cc"))
            return false;
        return true;
}
bool InitConfig() override {
    (*ConfigDiff)["Searcher.FiltrationModel"] = "SIMPLE";
    (*ConfigDiff)["Searcher.FactorsInfo"] = GetResourcesDirectory() + "/factors/qs_factors.cfg";
    SetMergerParams(true, 1, -1, mcpTIME, 1000000000);
    SetIndexerParams(REALTIME, 2000, 2);
    SetIndexerParams(DISK, 2000, 4);
    return true;
}
};

SERVICE_TEST_RTYSERVER_DEFINE_PARENT(TestQSFactorsRestoreUpdate, TestQSFactorsCommon)
    virtual bool DoInitialDocs(){
        GenerateInput(Messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), "a b c aa bb cc");
        Messages[0].MutableDocument()->SetUrl("testUrl");
        NRTYServer::TMessage::TQSInfo* info = Messages[0].MutableDocument()->AddQSInfo();
        info->SetName("QSText");
        info->AddFactorNames("aaa");
        info->AddFactorNames("bbb");
        NRTYServer::TMessage::TFactorsByKey* factorsInfo = info->AddFactors();
        factorsInfo->SetKey("a b c");
        factorsInfo->MutableValues()->AddValues()->SetValue(2);
        factorsInfo->MutableValues()->AddValues()->SetValue(1);
        IndexMessages(Messages, REALTIME, 1);
        ReopenIndexers();
        Sleep(TDuration::Seconds(1));

        FactorsByUrl["testUrl"]["a b c"]["aaa"] = 2;
        FactorsByUrl["testUrl"]["a b c"]["bbb"] = 1;

        return CheckResults("a b c softness:100", "a b c");
    }

    void ChangeQsForUpdate(TVector<NRTYServer::TMessage>& updates) {
        updates[0].SetMessageType(NRTYServer::TMessage::DEPRECATED__UPDATE_DOCUMENT);
        updates[0].SetMessageId(IMessageGenerator::CreateMessageId());
        updates[0].MutableDocument()->ClearBody();
        updates[0].MutableDocument()->MutableQSInfo(0)->MutableFactors(0)->MutableValues()->MutableValues(0)->SetValue(8);
    }

bool InitConfig() override {
    (*ConfigDiff)["Searcher.FiltrationModel"] = "SIMPLE";
    (*ConfigDiff)["Searcher.FactorsInfo"] = GetResourcesDirectory() + "/factors/qs_factors.cfg";
    return true;
}
};

START_TEST_DEFINE_PARENT(TestQSFactorsUpdate, TestQSFactorsRestoreUpdate)
    bool Run() override {
        FactorsByUrl.clear();

        CHECK_TEST_FAILED(!DoInitialDocs(), "Error while initial qs indexing");

        TVector<NRTYServer::TMessage> updates = Messages;
        ChangeQsForUpdate(updates);
        NRTYServer::TMessage::TFactorsByKey* factorsInfo = updates[0].MutableDocument()->MutableQSInfo(0)->AddFactors();
        factorsInfo->SetKey("aa bb cc");
        factorsInfo->MutableValues()->AddValues()->SetValue(4);
        factorsInfo->MutableValues()->AddValues()->SetValue(2);

        IndexMessages(updates, REALTIME, 1);
        ReopenIndexers();
        Sleep(TDuration::Seconds(1));

        FactorsByUrl["testUrl"]["a b c"]["aaa"] = 8;
        FactorsByUrl["testUrl"]["a b c"]["bbb"] = 1;
        FactorsByUrl["testUrl"]["aa bb cc"]["aaa"] = 4;
        FactorsByUrl["testUrl"]["aa bb cc"]["bbb"] = 2;

        CHECK_TEST_FAILED(!CheckResults("a b c softness:100", "a b c"), "qs factors were not modified");

        CHECK_TEST_FAILED(!CheckResults("aa bb cc softness:100", "aa bb cc"), "qs factors were not added");

        return true;
    }
};

START_TEST_DEFINE_PARENT(TestQSFactorsRestore, TestQSFactorsRestoreUpdate)
    bool Run() override {
        FactorsByUrl.clear();

        CHECK_TEST_FAILED(!DoInitialDocs(), "Error while initial qs indexing");

        TVector<NRTYServer::TMessage> updates = Messages;
        updates[0].SetMessageType(NRTYServer::TMessage::DEPRECATED__UPDATE_DOCUMENT);
        updates[0].SetMessageId(IMessageGenerator::CreateMessageId());
        updates[0].MutableDocument()->SetBody("a b c defg");
        updates[0].MutableDocument()->ClearQSInfo();
        IndexMessages(updates, REALTIME, 1);
        ReopenIndexers();
        Sleep(TDuration::Seconds(1));

        CHECK_TEST_FAILED(!CheckResults("a b c softness:100", "a b c"), "qs lost while UPDATE");
        return true;
    }

};

START_TEST_DEFINE_PARENT(TestQSFactorsRepair, TestQSFactorsCommon)
bool Run() override {
    FactorsByUrl.clear();

    GenerateInput(Messages, 10, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), "a b c aa bb cc");
    for (size_t i = 0; i < 10; ++i){
        TString url = "testUrl" + ToString(i);
        int aaa = i;
        int bbb = i + 5;
        Messages[i].MutableDocument()->SetUrl(url);
        NRTYServer::TMessage::TQSInfo* info = Messages[i].MutableDocument()->AddQSInfo();
        info->SetName("QSText");
        info->AddFactorNames("aaa");
        info->AddFactorNames("bbb");
        NRTYServer::TMessage::TFactorsByKey* factorsInfo = info->AddFactors();
        factorsInfo->SetKey("a b c");
        factorsInfo->MutableValues()->AddValues()->SetValue(aaa);
        factorsInfo->MutableValues()->AddValues()->SetValue(bbb);

        FactorsByUrl[url]["a b c"]["aaa"] = aaa;
        FactorsByUrl[url]["a b c"]["bbb"] = bbb;
    }

    IndexMessages(Messages, DISK, 1);

    Controller->RestartServer(true);
    Controller->WaitIsRepairing();

    CHECK_TEST_FAILED(!CheckResults("a b c softness:100", "a b c"), "wrong qs after repair");

    return true;
}
bool InitConfig() override {
    (*ConfigDiff)["Searcher.FiltrationModel"] = "SIMPLE";
    (*ConfigDiff)["Searcher.FactorsInfo"] = GetResourcesDirectory() + "/factors/qs_factors.cfg";
    SetEnabledRepair();
    return true;
}
};

START_TEST_DEFINE_PARENT(TestQSFactorsNormalize, TestQSFactorsCommon)
bool Run() override {
    FactorsByUrl.clear();

    GenerateInput(Messages, 10, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed(), TAttrMap(), "a b c aa bb cc");
    TVector<NRTYServer::TMessage> messagesWihtoutQS = Messages;
    for (size_t i = 0; i < 10; ++i){
        TString url = "testUrl" + ToString(i);
        int aaa = i;
        int bbb = i + 5;
        Messages[i].MutableDocument()->SetUrl(url);
        messagesWihtoutQS[i].MutableDocument()->SetUrl(url);
        NRTYServer::TMessage::TQSInfo* info = Messages[i].MutableDocument()->AddQSInfo();
        info->SetName("QSText");
        info->AddFactorNames("aaa");
        info->AddFactorNames("bbb");
        NRTYServer::TMessage::TFactorsByKey* factorsInfo = info->AddFactors();
        factorsInfo->SetKey("a b c");
        factorsInfo->MutableValues()->AddValues()->SetValue(aaa);
        factorsInfo->MutableValues()->AddValues()->SetValue(bbb);

        FactorsByUrl[url]["a b c"]["aaa"] = aaa;
        FactorsByUrl[url]["a b c"]["bbb"] = bbb;
    }

    IndexMessages(messagesWihtoutQS, DISK, 1);
    Controller->ProcessCommand("stop");
    (*ConfigDiff)["Searcher.FactorsInfo"] = GetResourcesDirectory() + "/factors/qs_factors.cfg";
    Controller->ApplyConfigDiff(ConfigDiff);
    Controller->RestartServer(true);
    Controller->ProcessCommand("clear_index");
    (*ConfigDiff)["Components"] = INDEX_COMPONENT_NAME "," FULL_ARCHIVE_COMPONENT_NAME;
    Controller->ApplyConfigDiff(ConfigDiff);
    Controller->RestartServer(true);
    IndexMessages(Messages, DISK, 1);
    ReopenIndexers();
    CHECK_TEST_FAILED(!CheckResults("a b c softness:100", "a b c"), "wrong qs after normalize keyinv");
    Controller->ProcessCommand("stop");
    (*ConfigDiff)["Searcher.FactorsInfo"] = GetResourcesDirectory() + "/factors/qs_factors2.cfg";
    Controller->ApplyConfigDiff(ConfigDiff);
    Controller->RestartServer(true);
    CHECK_TEST_FAILED(!CheckResults("a b c softness:100", "a b c"), "wrong qs after normalize erf");

    for (size_t i = 0; i < 10; ++i){
        const TString& url = Messages[i].GetDocument().GetUrl();
        FactorsByUrl[url]["a b c"]["ccc"] = 000;
        FactorsByUrl[url]["a b c"].erase("bbb");
    }

    Controller->ProcessCommand("stop");
    (*ConfigDiff)["Searcher.FactorsInfo"] = GetResourcesDirectory() + "/factors/qs_factors1.cfg";
    Controller->ApplyConfigDiff(ConfigDiff);
    Controller->RestartServer(true);
    CHECK_TEST_FAILED(!CheckResults("a b c softness:100", "a b c"), "wrong qs after normalize by fullarc");

    return true;
}
bool InitConfig() override {
    (*ConfigDiff)["Searcher.FiltrationModel"] = "SIMPLE";
    (*ConfigDiff)["Components"] = INDEX_COMPONENT_NAME;
    (*ConfigDiff)["ComponentsConfig"] = "__remove__";
    (*ConfigDiff)["Indexer.Common.UseSlowUpdate"] = "0";
    return true;
}
};
