#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/rtyserver/components/erf/erf_manager.h>
#include <saas/rtyserver/components/erf/erf_disk.h>
#include <saas/rtyserver/components/erf/erf_component.h>
#include <saas/rtyserver/components/erf/erf_parsed_entity.h>
#include <saas/api/factors_erf.h>
#include <kernel/web_factors_info/factor_names.h>

SERVICE_TEST_RTYSERVER_DEFINE(TErfTest)
protected:
    TAtomicSharedPtr<NRTYFactors::TConfig> FactorsConfig;
    TAtomicSharedPtr<TRTYStaticFactorsConfig> FactorsConfigDescr;

    void GenerateMessages(TVector<NRTYServer::TMessage>& result, size_t count, NRTYServer::TMessage::TMessageType type) {
        GenerateInput(result, count, type, GetIsPrefixed());
        for (size_t i = 0; i < count; ++i) {
            if (GetIsPrefixed())
                result[i].MutableDocument()->SetKeyPrefix(1);
            for (size_t fact = 0; fact < FactorsConfig->StaticFactors().size(); ++fact) {
                NSaas::AddSimpleFactor(FactorsConfig->StaticFactors()[fact].Name, ToString(i * FactorsConfig->StaticFactors().size() + fact),
                    *result[i].MutableDocument()->MutableFactors());
            }
            for (size_t fact = 0; fact < FactorsConfig->IgnoredFactors().size(); ++fact) {
                NSaas::AddSimpleFactor(FactorsConfig->IgnoredFactors()[fact].Name, ToString(i * FactorsConfig->IgnoredFactors().size() + fact),
                    *result[i].MutableDocument()->MutableFactors());
            }
        }
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

START_TEST_DEFINE_PARENT(TErfTestDisk, TErfTest)
public:
    bool Run() override {
        size_t countMessages = 10;
        TVector<NRTYServer::TMessage> messages;
        GenerateMessages(messages, countMessages, NRTYServer::TMessage::ADD_DOCUMENT);
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();
        TVector<TSimpleSharedPtr<IRTYErfManager> > erfManagers = CreateErfManagers();
        TVector<bool> docsOk(messages.size(), false);
        TBasicFactorStorage factors(N_FACTOR_COUNT);
        for (size_t i = 0; i < erfManagers.size(); ++i) {
            IRTYErfManager& manager = *erfManagers[i];
            for (size_t docid = 0; docid < manager.Size(); ++docid) {
                if (!manager.ReadRaw(factors, docid))
                    ythrow yexception() << "error on read";
                float messIndex = factors[0] / FactorsConfig->StaticFactors().size();
                if ((i64)messIndex != messIndex || messIndex < 0 || messIndex >= messages.size())
                    ythrow yexception() << "error on first factor";
                size_t mess = messIndex;

                // only a few first fields is set by ReadRaw ('factors' here is an erf block, not the actual Factors)
                for (size_t f = 1; f < FactorsConfig->StaticFactors().size(); ++f)
                    if (fabs(factors[f] - factors[0] - f) > 1e-4)
                        ythrow yexception() << "error on factor " << f << ", url = " << messages[mess].GetDocument().GetUrl();
                docsOk[mess] = true;
            }

        }
        for (TVector<bool>::const_iterator i = docsOk.begin(); i != docsOk.end(); ++i)
            if (!*i)
                ythrow yexception() << "erf not found for url " << messages[i - docsOk.begin()].GetDocument().GetUrl();
        return true;
    }
};

START_TEST_DEFINE_PARENT(TErfTestMemory, TErfTest)
public:
    bool Run() override {
        size_t countMessages = 10;
        TVector<NRTYServer::TMessage> messages;
        GenerateMessages(messages, countMessages, NRTYServer::TMessage::ADD_DOCUMENT);
        IndexMessages(messages, REALTIME, 1);
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
START_TEST_DEFINE_PARENT(TErfTestRepair, TErfTest)
public:
    bool Run() override {
        size_t countMessages = 10;
        TVector<NRTYServer::TMessage> messages;
        GenerateMessages(messages, countMessages, NRTYServer::TMessage::ADD_DOCUMENT);
        IndexMessages(messages, DISK, 1);
        Controller->RestartServer(true);
        Controller->WaitIsRepairing();
        TVector<TSimpleSharedPtr<IRTYErfManager> > erfManagers = CreateErfManagers();
        TVector<bool> docsOk(messages.size(), false);
        for (size_t i = 0; i < erfManagers.size(); ++i) {
            IRTYErfManager& manager = *erfManagers[i];
            for (size_t docid = 0; docid < manager.Size(); ++docid) {
                TBasicFactorStorage factors(N_FACTOR_COUNT);
                if(!manager.ReadRaw(factors, docid))
                    ythrow yexception() << "error on read";
                float messIndex = factors[0] / FactorsConfig->StaticFactors().size();
                if ((i64)messIndex != messIndex || messIndex < 0 || messIndex >= messages.size())
                    ythrow yexception() << "error on first factor";
                size_t mess = messIndex;

                // only a few first fields is set by ReadRaw ('factors' here is an erf block, not the actual Factors)
                for (size_t f = 1; f < FactorsConfig->StaticFactors().size(); ++f)
                    if (fabs(factors[f] - factors[0] - f) > 1e-4)
                        ythrow yexception() << "error on factor " << f << ", url = " << messages[mess].GetDocument().GetUrl();
                docsOk[mess] = true;
            }

        }
        for (TVector<bool>::const_iterator i = docsOk.begin(); i != docsOk.end(); ++i)
            if (!*i)
                ythrow yexception() << "erf not found for url " << messages[i - docsOk.begin()].GetDocument().GetUrl();
        return true;
    }

    bool InitConfig() override {
        if(!TErfTest::InitConfig())
            return false;
        SetEnabledRepair();
        return true;
    }
};

START_TEST_DEFINE_PARENT(TErfTestDefaultValues, TErfTest)
public:
    void SetDefaultFactorValues(TVector<NRTYServer::TMessage>& result) {
        for (size_t i = 0; i < result.size(); ++i) {
            if (GetIsPrefixed())
                result[i].MutableDocument()->SetKeyPrefix(1);
            for (size_t fact = 0; fact < FactorsConfig->StaticFactors().size(); ++fact)
                if (!FactorsConfig->StaticFactors()[fact].DefaultValue.IsSet()) {
                    NSaas::AddSimpleFactor(FactorsConfig->StaticFactors()[fact].Name, ToString(i * FactorsConfig->StaticFactors().size() + fact),
                        *result[i].MutableDocument()->MutableFactors());
                }
        }
    }
    bool Run() override {
        size_t countMessages = 10;
        TVector<NRTYServer::TMessage> messagesDisk;
        GenerateInput(messagesDisk, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
        SetDefaultFactorValues(messagesDisk);
        IndexMessages(messagesDisk, DISK, 1);
        ReopenIndexers();
        TVector<TSimpleSharedPtr<IRTYErfManager> > erfManagers = CreateErfManagers();
        for (size_t i = 0; i < erfManagers.size(); ++i) {
            IRTYErfManager& manager = *erfManagers[i];
            for (size_t docid = 0; docid < manager.Size(); ++docid) {
                TBasicFactorStorage factors(N_FACTOR_COUNT);
                if(!manager.ReadRaw(factors, docid))
                    ythrow yexception() << "error on read";
                TVector<float> factorsVector(factors.factors, factors.factors + 10);

                // only a few first fields is set by ReadRaw ('factors' here is an erf block, not the actual Factors)
                for (size_t i = 0; i != FactorsConfig->StaticFactors().size(); ++i) {
                    const NRTYFactors::TFactor& factorDescription = FactorsConfig->StaticFactors()[i];
                    if (factorDescription.DefaultValue.IsSet() && fabs(factors[i] - factorDescription.DefaultValue.Get()) > 1e-4)
                        ythrow yexception() << "factor " << factorDescription.Name << " doesn't have its default value: "
                        << factors[i] << " != " << factorDescription.DefaultValue.Get();
                }
            }
        }
        return true;
    }
};

