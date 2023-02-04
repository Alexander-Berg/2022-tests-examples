#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/rtyserver_test/util/factors_parsers.h>
#include <util/string/vector.h>
#include <util/system/fs.h>
#include <kernel/keyinv/indexfile/searchfile.h>
#include <library/cpp/string_utils/quote/quote.h>
#include <util/generic/ymath.h>

SERVICE_TEST_RTYSERVER_DEFINE(TestCSFactorsCommon)
    TVector<NRTYServer::TMessage> Messages;
    typedef THashMap<TString, float> TFactors;
    typedef THashMap<TString, TFactors> TFactorsByUrl;
    TFactorsByUrl FactorsByUrl;

    bool CheckResults(const TString& query) {
        TVector<TDocSearchInfo> results;
        TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > > resultProps;
        TString queryQuote = query;
        DEBUG_LOG << "Result checking for " << query << Endl;
        Quote(queryQuote);
        ui32 resCount = 0;
        for (ui32 p = 0; ; ++p) {
            QuerySearch(queryQuote + "&numdoc=750&dbgrlv=da&fsgta=_JsonFactors&" + GetAllKps(Messages) + "&p=" + ToString(p), results, &resultProps);
            TVector<THashMap<TString, double>> factors = TRTYFactorsParser::GetJsonFactorsValues(resultProps);
            resCount += results.size();
            for (ui32 i = 0; i < results.size(); ++i) {
                TFactorsByUrl::const_iterator itUrl = FactorsByUrl.find(results[i].GetUrl());
                const TFactors* factorValues = nullptr;
                if (itUrl != FactorsByUrl.end()) {
                    factorValues = &itUrl->second;
                }
                for (THashMap<TString, double>::const_iterator itF = factors[i].begin(), itE = factors[i].end(); itF != itE; ++itF) {
                    float valueTest = 0;
                    if (factorValues) {
                        TFactors::const_iterator itCompFactor = factorValues->find(itF->first);
                        if (itCompFactor != factorValues->end())
                            valueTest = itCompFactor->second;
                    }
                    if (Abs(itF->second - valueTest) > 0.001)
                        TEST_FAILED("Incorrect " + itF->first + " factor value for " + results[i].GetUrl() + " : " + ToString(itF->second) + "!= " + ToString(valueTest));
                }
            }
            if (results.size() < 750)
                break;
        }
        if (resCount != FactorsByUrl.size())
            ythrow yexception() << "document count is incorrect: " << resCount << " != " << FactorsByUrl.size();
        return true;
    }

};


START_TEST_DEFINE_PARENT(TestCSFactorsManyDocs, TestCSFactorsCommon)

    bool Run() override {
        FactorsByUrl.clear();
        ui32 maxDoc = 2 * GetMaxDocuments();
        GenerateInput(Messages, maxDoc, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), "a b c aa bb cc");

        for (ui32 docModif = 0; docModif < Messages.size(); ++docModif) {
            Messages[docModif].MutableDocument()->SetUrl(ToString(docModif));
            if (GetIsPrefixed())
                Messages[docModif].MutableDocument()->SetKeyPrefix((1 + docModif * GetShardsNumber()) % 5);
            NRTYServer::TMessage::TCSInfo* info = Messages[docModif].MutableDocument()->AddCSInfo();
            info->SetName("CSDomain");
            info->MutableFactors()->AddNames("aaa");
            info->MutableFactors()->AddNames("bbb");
            info->MutableFactors()->MutableValues()->AddValues()->SetValue(docModif * 2);
            info->MutableFactors()->MutableValues()->AddValues()->SetValue(1000000 - docModif * 2);
            FactorsByUrl[ToString(docModif)]["aaa"] = docModif * 2;
            FactorsByUrl[ToString(docModif)]["bbb"] = 1000000 - docModif * 2;
        }
        IndexMessages(Messages, REALTIME, 1);
        if (!CheckResults("a b c softness:100"))
            return false;
        ReopenIndexers();
        if (!CheckResults("a b c softness:100"))
            return false;
        Controller->ProcessCommand("create_merger_tasks");
        Controller->ProcessCommand("do_all_merger_tasks&wait=true");
        if (!CheckResults("a b c softness:100"))
            return false;
        return true;
}
bool InitConfig() override {
    (*ConfigDiff)["Searcher.FiltrationModel"] = "SIMPLE";
    (*ConfigDiff)["Searcher.FactorsInfo"] = GetResourcesDirectory() + "/factors/cs_factors.cfg";
    SetMergerParams(true, 1, -1, mcpTIME, 1000000000);
    SetIndexerParams(REALTIME, 2000, 2);
    SetIndexerParams(DISK, 2000, 4);
    return true;
}
};

START_TEST_DEFINE_PARENT(TestCSFactors, TestCSFactorsCommon)
    bool Run() override {
        FactorsByUrl.clear();
        GenerateInput(Messages, 2, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), "a b c aa bb cc");
        FactorsByUrl["http://yandex.ru:19000/?text=fff"]["aaa"] = 4;
        FactorsByUrl["http://yandex.ru:19000/?text=fff"]["bbb"] = 2;
        FactorsByUrl["http://yandex.ru:19000/?text=aaa"]["aaa"] = 4;
        FactorsByUrl["http://yandex.ru:19000/?text=aaa"]["bbb"] = 2;
        if (GetIsPrefixed()) {
            Messages[0].MutableDocument()->SetKeyPrefix(1);
            Messages[1].MutableDocument()->SetKeyPrefix(1 + GetShardsNumber());
        }

        Messages[0].MutableDocument()->SetUrl("http://yandex.ru:19000/?text=aaa");
        NRTYServer::TMessage::TCSInfo* info = Messages[0].MutableDocument()->AddCSInfo();
        info->SetName("CSDomain");
        info->MutableFactors()->AddNames("aaa");
        info->MutableFactors()->MutableValues()->AddValues()->SetValue(2);
        info->MutableFactors()->AddNames("bbb");
        info->MutableFactors()->MutableValues()->AddValues()->SetValue(1);

        Messages[1].MutableDocument()->SetUrl("http://yandex.ru:19000/?text=fff");
        info = Messages[1].MutableDocument()->AddCSInfo();
        info->SetName("CSDomain");
        info->MutableFactors()->AddNames("aaa");
        info->MutableFactors()->MutableValues()->AddValues()->SetValue(4);
        info->MutableFactors()->AddNames("bbb");
        info->MutableFactors()->MutableValues()->AddValues()->SetValue(2);

        IndexMessages(TVector<NRTYServer::TMessage>(Messages.begin() + 1, Messages.begin() + 2), REALTIME, 1);
        IndexMessages(TVector<NRTYServer::TMessage>(Messages.begin(), Messages.begin() + 1), REALTIME, 1);
        if (!CheckResults("a b c softness:100")) {
            return false;
        }
        ReopenIndexers();
        if (!CheckResults("a b c softness:100")) {
            return false;
        }
        return true;
}
bool InitConfig() override {
    (*ConfigDiff)["Searcher.FiltrationModel"] = "SIMPLE";
    (*ConfigDiff)["Searcher.FactorsInfo"] = GetResourcesDirectory() + "/factors/cs_factors.cfg";
    return true;
}
};

START_TEST_DEFINE_PARENT(TestCSBadFactors, TestCSFactorsCommon)
    bool Run() override {
        FactorsByUrl.clear();
        GenerateInput(Messages, 2, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), "a b c aa bb cc");
        FactorsByUrl["http://yandex.ru:19000/?text=fff"]["aaa"] = 4;
        FactorsByUrl["http://yandex.ru:19000/?text=fff"]["bbb"] = 2;
        FactorsByUrl["http://yandex.ru:19000/?text=aaa"]["aaa"] = 4;
        FactorsByUrl["http://yandex.ru:19000/?text=aaa"]["bbb"] = 2;
        if (GetIsPrefixed()) {
            Messages[0].MutableDocument()->SetKeyPrefix(1);
            Messages[1].MutableDocument()->SetKeyPrefix(1 + GetShardsNumber());
        }

        Messages[0].MutableDocument()->SetUrl("http://yandex.ru:19000/?text=aaa");
        Messages[1].MutableDocument()->SetUrl("http://yandex.ru:19000/?text=fff");
        MUST_BE_BROKEN(IndexMessages(Messages, REALTIME, 1));
        ReopenIndexers();
        return true;
}
bool InitConfig() override {
    (*ConfigDiff)["Searcher.FiltrationModel"] = "SIMPLE";
    (*ConfigDiff)["Searcher.FactorsInfo"] = GetResourcesDirectory() + "/factors/cs_factors.cfg";
    return true;
}
};

START_TEST_DEFINE_PARENT(TestCSFactorsSameMerge, TestCSFactorsCommon)
    bool Run() override {
        FactorsByUrl.clear();
        GenerateInput(Messages, 2, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), "a b c aa bb cc");

        if (GetIsPrefixed()) {
            Messages[0].MutableDocument()->SetKeyPrefix(1);
            Messages[1].MutableDocument()->SetKeyPrefix(1 + GetShardsNumber());
        }

        Messages[0].MutableDocument()->SetUrl("http://yandex.ru:19000/?text=aaa");
        NRTYServer::TMessage::TCSInfo* info = Messages[0].MutableDocument()->AddCSInfo();
        info->SetName("CSDomain");
        info->MutableFactors()->AddNames("aaa");
        info->MutableFactors()->MutableValues()->AddValues()->SetValue(2);
        info->MutableFactors()->AddNames("bbb");
        info->MutableFactors()->MutableValues()->AddValues()->SetValue(1);

        Messages[1].MutableDocument()->SetUrl("http://yandex.ru:19000/?text=fff");
        info = Messages[1].MutableDocument()->AddCSInfo();
        info->SetName("CSDomain");
        info->MutableFactors()->AddNames("aaa");
        info->MutableFactors()->MutableValues()->AddValues()->SetValue(4);
        info->MutableFactors()->AddNames("bbb");
        info->MutableFactors()->MutableValues()->AddValues()->SetValue(2);

        IndexMessages(TVector<NRTYServer::TMessage>(Messages.begin() + 1, Messages.begin() + 2), REALTIME, 1);
        ReopenIndexers();
        IndexMessages(TVector<NRTYServer::TMessage>(Messages.begin(), Messages.begin() + 1), REALTIME, 1);
        FactorsByUrl["http://yandex.ru:19000/?text=fff"]["aaa"] = 4;
        FactorsByUrl["http://yandex.ru:19000/?text=fff"]["bbb"] = 2;
        FactorsByUrl["http://yandex.ru:19000/?text=aaa"]["aaa"] = 4;
        FactorsByUrl["http://yandex.ru:19000/?text=aaa"]["bbb"] = 2;
        if (!CheckResults("a b c softness:100")) {
            return false;
        }
        ReopenIndexers();
        FactorsByUrl["http://yandex.ru:19000/?text=fff"]["aaa"] = 4;
        FactorsByUrl["http://yandex.ru:19000/?text=fff"]["bbb"] = 2;
        FactorsByUrl["http://yandex.ru:19000/?text=aaa"]["aaa"] = 2;
        FactorsByUrl["http://yandex.ru:19000/?text=aaa"]["bbb"] = 1;
        if (!CheckResults("a b c softness:100")) {
            return false;
        }
        Controller->ProcessCommand("create_merger_tasks");
        Controller->ProcessCommand("do_all_merger_tasks&wait=true");

        FactorsByUrl["http://yandex.ru:19000/?text=fff"]["aaa"] = 2;
        FactorsByUrl["http://yandex.ru:19000/?text=fff"]["bbb"] = 1;
        FactorsByUrl["http://yandex.ru:19000/?text=aaa"]["aaa"] = 2;
        FactorsByUrl["http://yandex.ru:19000/?text=aaa"]["bbb"] = 1;
        if (!CheckResults("a b c softness:100")) {
            FactorsByUrl["http://yandex.ru:19000/?text=fff"]["aaa"] = 4;
            FactorsByUrl["http://yandex.ru:19000/?text=fff"]["bbb"] = 2;
            FactorsByUrl["http://yandex.ru:19000/?text=aaa"]["aaa"] = 4;
            FactorsByUrl["http://yandex.ru:19000/?text=aaa"]["bbb"] = 2;
            if (!CheckResults("a b c softness:100")) {
                return false;
            }
        }
        return true;
}
bool InitConfig() override {
    (*ConfigDiff)["Searcher.FiltrationModel"] = "SIMPLE";
    (*ConfigDiff)["Searcher.FactorsInfo"] = GetResourcesDirectory() + "/factors/cs_factors.cfg";
    SetMergerParams(true, 1, -1, mcpTIME, 1000000000);
    return true;
}
};

SERVICE_TEST_RTYSERVER_DEFINE_PARENT(TestCSFactorsRestoreUpdate, TestCSFactorsCommon)

    virtual bool DoInitialDocs(){
        GenerateInput(Messages, 2, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), "a b c aa bb cc");

        if (GetIsPrefixed()) {
            Messages[0].MutableDocument()->SetKeyPrefix(1);
            Messages[1].MutableDocument()->SetKeyPrefix(1);
        }
        Messages[0].MutableDocument()->SetUrl("http://yandex.ru:19000/?text=aaa");
        Messages[1].MutableDocument()->SetUrl("http://yandex.ru:19000/?text=fff");

        NRTYServer::TMessage::TCSInfo* info;
        for (int i = 0; i < 2; ++i) {
            info = Messages[i].MutableDocument()->AddCSInfo();
            info->SetName("CSDomain");
            info->MutableFactors()->AddNames("aaa");
            info->MutableFactors()->MutableValues()->AddValues()->SetValue(2);
            info->MutableFactors()->AddNames("bbb");
            info->MutableFactors()->MutableValues()->AddValues()->SetValue(1);
        }

        IndexMessages(Messages, REALTIME, 1);

        FactorsByUrl["http://yandex.ru:19000/?text=fff"]["aaa"] = 2;
        FactorsByUrl["http://yandex.ru:19000/?text=fff"]["bbb"] = 1;
        FactorsByUrl["http://yandex.ru:19000/?text=aaa"]["aaa"] = 2;
        FactorsByUrl["http://yandex.ru:19000/?text=aaa"]["bbb"] = 1;

        return CheckResults("a b c softness:100");
    }

    virtual bool UpdateCsChecked(ui32 updateCount = 1){
        TVector<NRTYServer::TMessage> updates = TVector<NRTYServer::TMessage>(Messages.begin(), Messages.begin() + updateCount);
        for (ui32 i = 0; i < updateCount; ++i) {
            updates[i].SetMessageType(NRTYServer::TMessage::DEPRECATED__UPDATE_DOCUMENT);
            updates[i].SetMessageId(IMessageGenerator::CreateMessageId());
            updates[i].MutableDocument()->ClearBody();
            updates[i].MutableDocument()->MutableCSInfo(0)->MutableFactors()->MutableValues()->MutableValues(0)->SetValue(6);
            updates[i].MutableDocument()->MutableCSInfo(0)->MutableFactors()->MutableValues()->MutableValues(1)->SetValue(5);
        }

        IndexMessages(updates, REALTIME, 1);

        FactorsByUrl["http://yandex.ru:19000/?text=fff"]["aaa"] = 6;
        FactorsByUrl["http://yandex.ru:19000/?text=fff"]["bbb"] = 5;
        FactorsByUrl["http://yandex.ru:19000/?text=aaa"]["aaa"] = 6;
        FactorsByUrl["http://yandex.ru:19000/?text=aaa"]["bbb"] = 5;

        return CheckResults("a b c softness:100");
    }

bool InitConfig() override {
    (*ConfigDiff)["Searcher.FiltrationModel"] = "SIMPLE";
    (*ConfigDiff)["Searcher.FactorsInfo"] = GetResourcesDirectory() + "/factors/cs_factors.cfg";
    return true;
}
};

SERVICE_TEST_RTYSERVER_DEFINE_PARENT(TestCSFactorsUpdate, TestCSFactorsRestoreUpdate)
    bool Test(bool reopen) {
        FactorsByUrl.clear();
        CHECK_TEST_FAILED(!DoInitialDocs(), "Error while initial cs indexing: ");
        if (reopen)
            ReopenIndexers();
        TVector<NRTYServer::TMessage> updates(1);
        updates[0].SetMessageType(NRTYServer::TMessage::DEPRECATED__UPDATE_DOCUMENT);
        updates[0].SetMessageId(IMessageGenerator::CreateMessageId());
        updates[0].MutableDocument()->SetUrl(Messages[0].GetDocument().GetUrl());
        updates[0].MutableDocument()->SetKeyPrefix(Messages[0].GetDocument().GetKeyPrefix());
        NRTYServer::TMessage::TCSInfo* info = updates[0].MutableDocument()->AddCSInfo();
        info->SetName("CSDomain");
        info->MutableFactors()->AddNames("aaa");
        info->MutableFactors()->MutableValues()->AddValues()->SetValue(4);
        info->MutableFactors()->AddNames("bbb");
        info->MutableFactors()->MutableValues()->AddValues()->SetValue(3);

        IndexMessages(updates, REALTIME, 1);

        FactorsByUrl["http://yandex.ru:19000/?text=fff"]["aaa"] = 4;
        FactorsByUrl["http://yandex.ru:19000/?text=fff"]["bbb"] = 3;
        FactorsByUrl["http://yandex.ru:19000/?text=aaa"]["aaa"] = 4;
        FactorsByUrl["http://yandex.ru:19000/?text=aaa"]["bbb"] = 3;
        CHECK_TEST_FAILED(!CheckResults("a b c softness:100"), "error in cs Fast UPDATE");
        CHECK_TEST_FAILED(!UpdateCsChecked(reopen ? 2 : 1), "error in cs Slow UPDATE");
        return true;
    }
};
START_TEST_DEFINE_PARENT(TestCSFactorsUpdateMem, TestCSFactorsUpdate)
    bool Run() override {
        return Test(false);
    }
};

START_TEST_DEFINE_PARENT(TestCSFactorsUpdateDisk, TestCSFactorsUpdate)
    bool Run() override {
        return Test(true);
    }
};

START_TEST_DEFINE_PARENT(TestCSFactorsRestore, TestCSFactorsRestoreUpdate)
    bool Run() override {
        FactorsByUrl.clear();

        CHECK_TEST_FAILED(!DoInitialDocs(), "Error while initial cs indexing");

        TVector<NRTYServer::TMessage> updates = TVector<NRTYServer::TMessage>(Messages.begin(), Messages.begin() + 1);
        updates[0].SetMessageType(NRTYServer::TMessage::DEPRECATED__UPDATE_DOCUMENT);
        updates[0].SetMessageId(IMessageGenerator::CreateMessageId());
        updates[0].MutableDocument()->SetBody("a b c defg");
        updates[0].MutableDocument()->ClearCSInfo();
        IndexMessages(updates, REALTIME, 1);
        Sleep(TDuration::Seconds(1));

        CHECK_TEST_FAILED(!CheckResults("a b c softness:100"), "cs lost while UPDATE");
        Sleep(TDuration::Seconds(1));

        TVector<TDocSearchInfo> results;
        QuerySearch("defg&" + GetAllKps(Messages), results);
        CHECK_TEST_FAILED(results.size() == 0, "body hasn't changed after UPDATE");

        return true;
    }

};

START_TEST_DEFINE_PARENT(TestCSFactorsRepair, TestCSFactorsCommon)
bool Run() override {
    FactorsByUrl.clear();
    GenerateInput(Messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), "a b c aa bb cc");
    Messages[0].MutableDocument()->SetUrl("http://yandex.ru:19000/?text=aaa");

    NRTYServer::TMessage::TCSInfo* info = Messages[0].MutableDocument()->AddCSInfo();
    info->SetName("CSDomain");
    info->MutableFactors()->AddNames("aaa");
    info->MutableFactors()->MutableValues()->AddValues()->SetValue(6);
    info->MutableFactors()->AddNames("bbb");
    info->MutableFactors()->MutableValues()->AddValues()->SetValue(5);

    FactorsByUrl["http://yandex.ru:19000/?text=aaa"]["aaa"] = 6;
    FactorsByUrl["http://yandex.ru:19000/?text=aaa"]["bbb"] = 5;

    IndexMessages(Messages, DISK, 1);

    Controller->RestartServer(true);
    Controller->WaitIsRepairing();

    return CheckResults("a b c softness:100");
}

bool InitConfig() override {
    (*ConfigDiff)["Searcher.FiltrationModel"] = "SIMPLE";
    (*ConfigDiff)["Searcher.FactorsInfo"] = GetResourcesDirectory() + "/factors/cs_factors.cfg";
    SetEnabledRepair();
    return true;
}
};
