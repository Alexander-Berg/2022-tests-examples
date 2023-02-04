#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/rtyserver_test/cases/oxy/oxy.h>
#include <saas/rtyserver/components/erf/erf_manager.h>
#include <saas/rtyserver/components/erf/erf_disk.h>
#include <saas/rtyserver/components/erf/erf_component.h>
#include <saas/rtyserver/components/erf/erf_parsed_entity.h>
#include <saas/api/factors_erf.h>
#include <saas/rtyserver_test/testerlib/messages_generator.h>
#include <saas/rtyserver/common/debug_messages.h>
#include <saas/rtyserver_test/util/factors_parsers.h>
#include <kernel/web_factors_info/factor_names.h>

SERVICE_TEST_RTYSERVER_DEFINE(TestUpdateErf)
protected:
    class TIndexerBySignal: public IMessageProcessor {
    public:

        TAtomic CallsCounter = 0;

        TIndexerBySignal(const TVector<NRTYServer::TMessage>& msgs, TRTYServerTestCase& owner)
            : Messages(msgs)
            , Owner(owner)
        {
            RegisterGlobalMessageProcessor(this);
        }

        ~TIndexerBySignal() {
            UnregisterGlobalMessageProcessor(this);
        }

        bool Process(IMessage* message) override {
            TMessageOnCloseIndex* msg = dynamic_cast<TMessageOnCloseIndex*>(message);
            if (msg) {
                AtomicIncrement(CallsCounter);
                Owner.IndexMessages(Messages, REALTIME, 1, 0, false);
                return true;
            }
            return false;
        }

        TString Name() const override {
            return "TIndexerBySignal";
        }

        TVector<NRTYServer::TMessage> Messages;
        TRTYServerTestCase& Owner;
        TMessageOnCloseIndex::TStage Stage;
    };

    TAtomicSharedPtr<NRTYFactors::TConfig> FactorsConfig;
    TAtomicSharedPtr<TRTYStaticFactorsConfig> FactorsConfigDescr;

    void GenerateMessages(TVector<NRTYServer::TMessage>& result, size_t count, NRTYServer::TMessage::TMessageType type) {
        bool isPrefixed = GetIsPrefixed();
        GenerateInput(result, count, type, isPrefixed);
        for (size_t i = 0; i < count; ++i) {
            if (isPrefixed)
                result[i].MutableDocument()->SetKeyPrefix(1);
            for (size_t fact = 0; fact < FactorsConfig->StaticFactors().size(); ++fact) {
                NSaas::AddSimpleFactor(FactorsConfig->StaticFactors()[fact].Name, "0.0",  *result[i].MutableDocument()->MutableFactors());
            }
        }
    }

    TVector<NRTYServer::TMessage> GenerateUpdateMessages(const TVector<NRTYServer::TMessage>& messages) {
        TVector<NRTYServer::TMessage> result = messages;
        for (TVector<NRTYServer::TMessage>::iterator i = result.begin(); i != result.end(); ++i) {
            i->SetMessageType(NRTYServer::TMessage::DEPRECATED__UPDATE_DOCUMENT);
            i->SetMessageId(IMessageGenerator::CreateMessageId());
            i->MutableDocument()->clear_body();
            i->MutableDocument()->clear_searchattributes();
            i->MutableDocument()->clear_groupattributes();
            i->MutableDocument()->clear_documentproperties();
            i->MutableDocument()->clear_factors();
            for (size_t fact = 0; fact < FactorsConfig->StaticFactors().size(); ++fact) {
                NSaas::AddSimpleFactor(FactorsConfig->StaticFactors()[fact].Name, ToString((i - result.begin()) * FactorsConfig->StaticFactors().size() + fact), *i->MutableDocument()->MutableFactors());
            }
        }
        return result;
    }

    TVector<TSimpleSharedPtr<IRTYErfManager> > CreateErfManagers() {
        const TSet<TString> finalIndexes = Controller->GetFinalIndexes();
        Controller->ProcessCommand("stop");
        TVector<TSimpleSharedPtr<IRTYErfManager> > result;
        for (TSet<TString>::const_iterator i = finalIndexes.begin(); i != finalIndexes.end(); ++i) {
            TRTYErfDiskManager::TCreationContext cc(TPathName{*i}, "indexerf.rty", FactorsConfigDescr.Get());
            cc.ReadOnly = true;
            result.push_back(new TRTYErfDiskManager(cc, ERF_COMPONENT_NAME));
            result.back()->Open();
        }
        return result;
    };

public:
    bool InitConfig() override {
        if (!NFs::Exists(FactorsFileName))
            ythrow yexception() << "this test must be started with correct factors info";
        FactorsConfig.Reset(new NRTYFactors::TConfig(FactorsFileName.data()));
        FactorsConfigDescr.Reset(new TRTYStaticFactorsConfig(FactorsConfig.Get()));
        (*ConfigDiff)["Searcher.FactorsInfo"] = FactorsFileName;
        return true;
    }
};

SERVICE_TEST_RTYSERVER_DEFINE_PARENT(TestUpdateErfDisk, TestUpdateErf)
public:
    void Test(bool temp) {
        size_t countMessages = 10;
        TVector<NRTYServer::TMessage> messages;
        GenerateMessages(messages, countMessages, NRTYServer::TMessage::ADD_DOCUMENT);
        IndexMessages(messages, DISK, 1);
        if (!temp)
            ReopenIndexers();
        IndexMessages(GenerateUpdateMessages(messages), DISK, 1);
        if (temp)
            ReopenIndexers();
        TVector<TSimpleSharedPtr<IRTYErfManager> > erfManagers = CreateErfManagers();
        TVector<bool> docsOk(messages.size(), false);
        for (size_t i = 0; i < erfManagers.size(); ++i) {
            IRTYErfManager& manager = *erfManagers[i];
            for (size_t docid = 0; docid < manager.Size(); ++docid) {
                TBasicFactorStorage factors(N_FACTOR_COUNT);
                if (!manager.ReadRaw(factors, docid))
                    ythrow yexception() << "error on read";
                float messIndex = factors[0] / FactorsConfig->StaticFactors().size();
                if ((i64)messIndex != messIndex || messIndex < 0 || messIndex >= messages.size())
                    ythrow yexception() << "error on first factor";
                size_t mess = messIndex;
                for (size_t f = 1; f < FactorsConfig->StaticFactors().size(); ++f)
                    if (fabs(factors[f] - factors[0] - f) > 1e-4)
                        ythrow yexception() << "error on factor " << f << ", url = " << messages[mess].GetDocument().GetUrl();
                docsOk[mess] = true;
            }

        }
        for (TVector<bool>::const_iterator i = docsOk.begin(); i != docsOk.end(); ++i)
            if (!*i)
                ythrow yexception() << "erf not found for url " << messages[i - docsOk.begin()].GetDocument().GetUrl();
    }
};

START_TEST_DEFINE_PARENT(TestUpdateErfTemp, TestUpdateErfDisk)
bool Run() override {
    Test(true);
    return true;
}
};

START_TEST_DEFINE_PARENT(TestUpdateErfTempFastNoFANOSlowUpd, TestUpdateErf)

bool Run() override {
    TOxyProcessorSleepAction someActionOnSleep(3);
    size_t countMessages = 100;
    TVector<NRTYServer::TMessage> messages;
    GenerateMessages(messages, countMessages, NRTYServer::TMessage::ADD_DOCUMENT);
    auto messUpd = GenerateUpdateMessages(messages);
    TIndexerBySignal signalProcessor(messUpd, *this);
    try {
        IndexMessages(messages, DISK, 3);
    } catch (...) {
    }
    CHECK_TEST_LESS(100, AtomicGet(signalProcessor.CallsCounter));
    return true;
}
bool InitConfig() override {
    (*ConfigDiff)["Components"] = "ERF";
    SetIndexerParams(ALL, 10, 8);
    (*ConfigDiff)["Indexer.Common.UseSlowUpdate"] = "false";
    return TestUpdateErf::InitConfig();
}
};

START_TEST_DEFINE_PARENT(TestUpdateErfTempFastNoFAUseSlowUpd, TestUpdateErf)
bool Run() override {
    TOxyProcessorSleepAction someActionOnSleep(3);
    size_t countMessages = 100;
    TVector<NRTYServer::TMessage> messages;
    GenerateMessages(messages, countMessages, NRTYServer::TMessage::ADD_DOCUMENT);
    auto messUpd = GenerateUpdateMessages(messages);
    TIndexerBySignal signalProcessor(messUpd, *this);
    try {
        IndexMessages(messages, DISK, 3);
    } catch (...) {
    }
    CHECK_TEST_LESS(100, AtomicGet(signalProcessor.CallsCounter));
    return true;
}
bool InitConfig() override {
    (*ConfigDiff)["Components"] = "ERF";
    SetIndexerParams(ALL, 10, 8);
    (*ConfigDiff)["Indexer.Common.UseSlowUpdate"] = "true";
    return TestUpdateErf::InitConfig();
}
};

START_TEST_DEFINE_PARENT(TestUpdateErfFinal, TestUpdateErfDisk)
    bool Run() override {
        Test(false);
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestUpdateErfMemory, TestUpdateErf)
public:
    bool Run() override {
        size_t countMessages = 10;
        TVector<NRTYServer::TMessage> messages;
        GenerateMessages(messages, countMessages, NRTYServer::TMessage::ADD_DOCUMENT);
        IndexMessages(messages, REALTIME, 1);
        IndexMessages(GenerateUpdateMessages(messages), REALTIME, 1);
        TVector<TDocSearchInfo> results;
        if (GetIsPrefixed())
            QuerySearch("body&kps=1", results);
        else
            QuerySearch("body", results);
        for (size_t i = 0; i < countMessages; ++i) {
            if (messages[i].MutableDocument()->GetUrl() != results[countMessages - i - 1].GetUrl()) {
                ERROR_LOG << "Incorrect sequence of documents for memory search with custom relevance" << Endl;
                return false;
            }
        }
        ReopenIndexers();
        if (GetIsPrefixed())
            QuerySearch("body&kps=1", results);
        else
            QuerySearch("body", results);
        for (size_t i = 0; i < countMessages; ++i) {
            if (messages[i].MutableDocument()->GetUrl() != results[countMessages - i - 1].GetUrl()) {
                ERROR_LOG << "Incorrect sequence of documents for base search with custom relevance" << Endl;
                return false;
            }
        }
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestUpdateWhileCloseIndex, TestUpdateErf)

class TIndexerBySignal : public IMessageProcessor {
public:
    TIndexerBySignal(const TVector<NRTYServer::TMessage>& msgs, TRTYServerTestCase& owner, TMessageOnCloseIndex::TStage stage)
        : Messages(msgs)
        , Owner(owner)
        , Stage(stage)
    {
        RegisterGlobalMessageProcessor(this);
    }

    ~TIndexerBySignal() {
        UnregisterGlobalMessageProcessor(this);
    }

    bool Process(IMessage* message) override {
        TMessageOnCloseIndex* msg = dynamic_cast<TMessageOnCloseIndex*>(message);
        if (msg) {
            if (Stage == msg->GetStage()) {
                Owner.IndexMessages(Messages, REALTIME, 1, 0, false);
            }
            return true;
        }
        return false;
    }

    TString Name() const override {
        return "hsdkdsafkkdsf";
    }

    TVector<NRTYServer::TMessage> Messages;
    TRTYServerTestCase& Owner;
    TMessageOnCloseIndex::TStage Stage;
};

void Check(TMessageOnCloseIndex::TStage stage, float value, ui32 countMessages) {
    TString request = "body" + ToString((int)stage) + "&dbgrlv=da&fsgta=_JsonFactors&relev=all_factors&kps=" + ToString((ui64)GetIsPrefixed());
    TVector<TDocSearchInfo> results;
    TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > > docProperties;
    QuerySearch(request, results, &docProperties);
    if (results.size() != countMessages)
        ythrow yexception() << "stage " << ((int)stage) << ": invalid  results count " << results.size() << " != " << countMessages;
    TVector<THashMap<TString, double> > factors = TRTYFactorsParser::GetJsonFactorsValues(docProperties);

    for (const auto& docPrp : factors) {
        for (const auto& fact : FactorsConfig->StaticFactors()) {
            if (!docPrp.contains(fact.Name))
                ythrow yexception() << "stage " << ((int)stage) << ":invalid factor not found " << fact.Name;
            float val = docPrp.find(fact.Name)->second;
            if (fabs(value - val) > 1e-4)
                ythrow yexception() << "stage " << ((int)stage) << ":invalid factor " << fact.Name << " value " << val << " != " << value;
        }
    }
}

void Test(TMessageOnCloseIndex::TStage stage) {
    DEBUG_LOG << "Test stage " << ((int)stage) << Endl;
    ui32 countMessages = 1;
    TVector<NRTYServer::TMessage> messages, updates(2 * countMessages);
    ui64 kps = GetIsPrefixed();
    GenerateInput(messages, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, kps);
    for (ui32 i = 0; i < countMessages; ++i) {
        NRTYServer::TMessage& m = messages[i];
        NRTYServer::TMessage& u = updates[/*2  * */i];
        u.SetMessageId(2 * m.GetMessageId());
        u.SetMessageType(NRTYServer::TMessage::DEPRECATED__UPDATE_DOCUMENT);
        u.MutableDocument()->SetUrl(m.GetDocument().GetUrl());
        m.MutableDocument()->SetKeyPrefix(kps);
        m.MutableDocument()->SetBody("body" + ToString((int)stage));
        u.MutableDocument()->SetKeyPrefix(kps);
        NRTYServer::TMessage& us = updates[2 * i + 1];
        us = u;
        us.SetMessageId(1 + 2 * m.GetMessageId());
        us.MutableDocument()->SetBody("body" + ToString((int)stage));
        us.MutableDocument()->SetMimeType("text/plain");
        for (const auto& fact : FactorsConfig->StaticFactors()) {
            NSaas::AddSimpleFactor(fact.Name, "10", *u.MutableDocument()->MutableFactors());
            NSaas::AddSimpleFactor(fact.Name, "0", *m.MutableDocument()->MutableFactors());
        }
    }
    TIndexerBySignal indexer(updates, *this, stage);
    IndexMessages(messages, REALTIME, 1);
    ReopenIndexers();
    Check(stage, 10, countMessages);
}

bool Run() override {
    for (int i = 0; i < TMessageOnCloseIndex::StageMax; ++i) {
        Test((TMessageOnCloseIndex::TStage)i);
    }
    return true;
}
/*
virtual bool InitConfig() {
    (*ConfigDiff)["Components"] = "ERF";
    (*ConfigDiff)["Indexer.Common.UseSlowUpdate"] = "false";
    SetEnabledRepair();
    return TestUpdateErf::InitConfig();
}
*/
};
