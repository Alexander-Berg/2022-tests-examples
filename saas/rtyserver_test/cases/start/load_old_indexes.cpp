#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/rtyserver/factors/factors_config.h>
#include <saas/rtyserver/components/erf/erf_manager.h>
#include <saas/rtyserver/components/erf/erf_parsed_entity.h>
#include <saas/api/factors_erf.h>

START_TEST_DEFINE(TestLoadOldIndexes)
    bool Run() override {
        const int CountMessages = 100;
        TVector<NRTYServer::TMessage> messagesForMemory;
        TVector<NRTYServer::TMessage> messagesForDisk;
        GenerateInput(messagesForMemory, CountMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
        GenerateInput(messagesForDisk, CountMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());

        INFO_LOG << "Messages indexing to memory index..." << Endl;
        IndexMessages(messagesForMemory, REALTIME, 1);
        INFO_LOG << "Messages indexing to disk index..." << Endl;
        IndexMessages(messagesForDisk, DISK, 1);

        INFO_LOG << "Server restarting..." << Endl;
        Controller->RestartServer();
        INFO_LOG << "Server restart OK" << Endl;
        INFO_LOG << "Memory messages checking..." << Endl;
        CheckSearchResults(messagesForMemory);
        INFO_LOG << "Disk messages checking..." << Endl;
        CheckSearchResults(messagesForDisk);
        return true;
    }
};

START_TEST_DEFINE(TestRestoreIdentifiers)
protected:
    TAtomicSharedPtr<NRTYFactors::TConfig> FactorsConfig;
    TAtomicSharedPtr<TRTYStaticFactorsConfig> FactorsConfigDescr;

    void GenerateMessages(TVector<NRTYServer::TMessage>& result, size_t count, NRTYServer::TMessage::TMessageType type) {
        GenerateInput(result, count, type, GetIsPrefixed());
        for (size_t i = 0; i < count; ++i) {
            if (GetIsPrefixed())
                result[i].MutableDocument()->SetKeyPrefix(1);
            for (size_t fact = 0; fact < FactorsConfig->StaticFactors().size(); ++fact) {
                NSaas::AddSimpleFactor(FactorsConfig->StaticFactors()[fact].Name, ToString(i * FactorsConfig->StaticFactors().size() + fact), *result[i].MutableDocument()->MutableFactors());
            }
        }
    }

public:
    bool Run() override {
        const int countMessages = 100;
        TVector<NRTYServer::TMessage> messages;
        GenerateMessages(messages, countMessages, NRTYServer::TMessage::ADD_DOCUMENT);
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();
        INFO_LOG << "Server restarting..." << Endl;
        const TSet<TString> finalIndexes = Controller->GetFinalIndexes();
        for (TSet<TString>::const_iterator i = finalIndexes.begin(); i != finalIndexes.end(); ++i) {
            (TFsPath(*i) / "indexddk.rty").DeleteIfExists();
            (TFsPath(*i) / "indexerf.rty").DeleteIfExists();
            (TFsPath(*i) / "indexddk.rty.hdr").DeleteIfExists();
            (TFsPath(*i) / "indexerf.rty.hdr").DeleteIfExists();
        }
        Controller->RestartServer();
        INFO_LOG << "Server restart OK" << Endl;
        INFO_LOG << "messages checking..." << Endl;
        CheckSearchResults(messages);
        return true;
    }
    bool InitConfig() override {
        if (!NFs::Exists(FactorsFileName.data()))
            ythrow yexception() << "this test must be started with correct factors info, file '" << FactorsFileName << "' does not exist";
        FactorsConfig.Reset(new NRTYFactors::TConfig(FactorsFileName.data()));
        FactorsConfigDescr.Reset(new TRTYStaticFactorsConfig(FactorsConfig.Get()));
        (*ConfigDiff)["Searcher.FactorsInfo"] = FactorsFileName;
        return true;
    }

};
