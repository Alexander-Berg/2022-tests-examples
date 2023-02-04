#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/api/factors_erf.h>
#include <saas/rtyserver/indexer_core/index_metadata_processor.h>
#include <saas/rtyserver/components/fullarchive/globals.h>
#include <kernel/multipart_archive/multipart.h>
#include <util/system/fs.h>

START_TEST_DEFINE(TestPRUNING_PREPARE_WITHNO_MERGE)
bool Run() override {
    if (GetIsPrefixed())
        return true;
    const unsigned CountMessages = 100;
    TAttrMap attrs;
    for (unsigned i = 0; i < CountMessages; i++) {
        TAttrMap::value_type map1;
        map1["unique_attr"] = (i * 2) % CountMessages;
        attrs.push_back(map1);
    }
    TVector<NRTYServer::TMessage> messagesForMemory1;
    GenerateInput(messagesForMemory1, CountMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), attrs);
    IndexMessages(messagesForMemory1, REALTIME, 1);

    Controller->RestartServer(false, nullptr);

    return true;
}

bool InitConfig() override {
    SetPruneAttrSort("unique_attr");
    SetIndexerParams(ALL, 200);
    return true;
}
};

START_TEST_DEFINE(TestPRUNING_PREPARE_WITH_MERGE)
bool Run() override {
    if (GetIsPrefixed())
        return true;
    const int CountMessages = 200;
    TAttrMap attrs;
    for (int i = 0; i < CountMessages; i++) {
        TAttrMap::value_type map1;
        map1["unique_attr"] = i + 10;
        attrs.push_back(map1);
    }
    TVector<NRTYServer::TMessage> messagesForMemory1;
    GenerateInput(messagesForMemory1, CountMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), attrs);
    IndexMessages(messagesForMemory1, REALTIME, 1);

    Controller->RestartServer(false, nullptr);
    Controller->ProcessCommand("create_merger_tasks");
    Controller->ProcessCommand("do_all_merger_tasks");
    Controller->RestartServer(false, nullptr);

    return true;
}
bool InitConfig() override {
    SetPruneAttrSort("unique_attr");
    SetIndexerParams(ALL, 50);
    SetMergerParams(true, 1);
    return true;
}
};

START_TEST_DEFINE(TestFinalIndexNormalizer)
bool Run() override {
    const ui64 countMessages = 50;
    TVector<NRTYServer::TMessage> messages;
    TAttrMap attrs(countMessages);
    for (ui64 i = 0; i < countMessages; ++i) {
        attrs[i]["unique_attr"] = i + 1;
    }
    GenerateInput(messages, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), attrs);
    for (ui64 i = 0; i < countMessages; ++i) {
        NSaas::AddSimpleFactor("stat1", "2.5", *messages[i].MutableDocument()->MutableFactors());
        NSaas::AddSimpleFactor("stat2", "4.3", *messages[i].MutableDocument()->MutableFactors());
        NSaas::AddSimpleFactor("stat3", "6.1", *messages[i].MutableDocument()->MutableFactors());
    }
    IndexMessages(messages, DISK, 1, 0, true, true, TDuration(), TDuration(), 8);
    ReopenIndexers();
    TSet<std::pair<ui64, TString> > deleted;
    DeleteSomeMessages(messages, deleted, DISK);
    SetPruneAttrSort("unique_attr");
    ApplyConfig();
    Controller->RestartServer();
    CheckSearchResults(messages, deleted);
    TSet<TString> indexes = Controller->GetFinalIndexes();
    Controller->ProcessCommand("stop");
    for (const auto& path : indexes) {
        TFsPath final(path);
        ui32 version = TIndexMetadataProcessor(final)->GetFullArcHeader().GetVersion();
        if (version != FULL_ARC_VERSION)
            ythrow yexception() << "invalid version in " << final.GetPath() << ", " << version << " != " << FULL_ARC_VERSION;
        TFsPath temp(final.Parent() / ("temp_" + final.GetName().substr(strlen("index"))));
        temp.MkDirs();
        NRTYArchive::HardLinkOrCopy(final / FULL_ARC_FILE_NAME_PREFIX, temp / FULL_ARC_FILE_NAME_PREFIX);
        TFile normalIndex(temp / "normal_index", WrOnly | OpenAlways);
        *TIndexMetadataProcessor(temp) = *TIndexMetadataProcessor(final);
        final.ForceDelete();
    }
    Controller->RestartServer();
    Controller->WaitIsRepairing();
    DEBUG_LOG << "Check after repair" << Endl;
    CheckSearchResults(messages, deleted);
    return true;
}
bool InitConfig() override {
    SetEnabledRepair();
    (*ConfigDiff)["Searcher.FactorsInfo"] = FactorsFileName;
    return true;
}
};

START_TEST_DEFINE(TestPruningMergePortions)
bool Run() override {
    const ui64 countMessages = 50;
    TVector<NRTYServer::TMessage> messages;
    TAttrMap attrs(countMessages);
    for (ui64 i = 0; i < countMessages; ++i) {
        attrs[i]["unique_attr"] = i + 1;
    }
    GenerateInput(messages, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), attrs);
    IndexMessages(messages, DISK, 1, 0, true, true, TDuration(), TDuration(), 8);
    TSet<std::pair<ui64, TString> > deleted;
    DeleteSomeMessages(messages, deleted, DISK);
    ReopenIndexers();
    Controller->RestartServer(false, nullptr);
    CheckSearchResults(messages, deleted);
    return true;
}
bool InitConfig() override {
    SetPruneAttrSort("unique_attr");
    return true;
}
};

START_TEST_DEFINE(TestPruningByStatFormula)
protected:
    void Test(TIndexerType indexer) {
        TVector<NRTYServer::TMessage> messages;
        const TString indexerType(indexer == DISK ? "disk" : "memory");
        GenerateInput(messages, 100, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), indexerType);
        TMap<float, ui32> sequence;
        ui32 f0, f1, f2;
        float value;
        for (int i = 0; i < messages.ysize(); i++) {
            messages[i].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());
            do {
                f0 = rand() % 100;
                f1 = rand() % 100;
                f2 = rand() % 100;
                value = 0.1 * f0 + 0.2 * f1 + 0.3 * f2;
            } while (sequence.find(value) != sequence.end());
            sequence[value] = i;
            NSaas::AddSimpleFactor("stat7", ToString(f2), *messages[i].MutableDocument()->MutableFactors());
            NSaas::AddSimpleFactor("stat2", ToString(rand() % 100), *messages[i].MutableDocument()->MutableFactors());
            NSaas::AddSimpleFactor("stat3", ToString(f0), *messages[i].MutableDocument()->MutableFactors());
            NSaas::AddSimpleFactor("stat4", ToString(f1), *messages[i].MutableDocument()->MutableFactors());
            NSaas::AddSimpleFactor("stat6", ToString(rand() % 100), *messages[i].MutableDocument()->MutableFactors());

            ::NRTYServer::TMessage::TDocument::TProperty* prop = messages[i].MutableDocument()->AddDocumentProperties();
            prop->set_name("value");
            prop->set_value(ToString(value));
        }
        TSet<TString> deleted;
        IndexMessages(messages, indexer, 1);
        TVector<TDocSearchInfo> results;
        if (indexer == DISK)
            ReopenIndexers();
        QuerySearch(indexerType + "&numdoc=1000&how=docid&" + GetAllKps(messages), results);
        if (results.size() != 100)
            ythrow yexception() << "very strange";

        int id = 0;
        for (TMap<float, ui32>::const_reverse_iterator i = sequence.rbegin(), e = sequence.rend(); i != e; i++, id++) {
            if (results[id].GetUrl() != messages[i->second].GetDocument().GetUrl()) {
                ythrow yexception() << "incorrect sort order with pruning";
            }
        }

        TVector<NRTYServer::TMessage> updates;
        for (auto&& m : messages) {
            NRTYServer::TMessage u;
            u.SetMessageType(NRTYServer::TMessage::DEPRECATED__UPDATE_DOCUMENT);
            u.SetMessageId(m.GetMessageId() + 1000);
            u.MutableDocument()->SetUrl(m.GetDocument().GetUrl());
            u.MutableDocument()->SetKeyPrefix(m.GetDocument().GetKeyPrefix());
            NSaas::AddSimpleFactor("factor_for_update", ToString(rand() % 100), *u.MutableDocument()->MutableFactors());
            updates.push_back(u);
        }

        IndexMessages(updates, indexer, 1);
    }
public:
    bool InitConfig() override {
        SetIndexerParams(ALL, 1000, 1);
        SetMergerParams(true, 1, -1, mcpNONE);
        (*ConfigDiff)["Searcher.FactorsInfo"] = GetResourcesDirectory() + "/factors/pruning_prepare_factors.cfg";
        (*ConfigDiff)["Indexer.Common.UseSlowUpdate"] = 0;
        SetPruneAttrSort("formula:default");
        return true;
    }

    bool Run() override {
        Test(DISK);
        return true;
    }
};

START_TEST_DEFINE(TestPruningByStatFormulaDeferredUpdate)
protected:
    void Test(TIndexerType indexer) {
        TVector<NRTYServer::TMessage> messages;
        const TString indexerType(indexer == DISK ? "disk" : "memory");
        GenerateInput(messages, 100, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), indexerType);
        TMap<float, ui32> sequence;
        ui32 f0, f1, f2;
        float value;
        for (int i = 0; i < messages.ysize(); i++) {
            messages[i].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());
            do {
                f0 = rand() % 100;
                f1 = rand() % 100;
                f2 = rand() % 100;
                value = 0.1 * f0 + 0.2 * f1 + 0.3 * f2;
            } while (sequence.find(value) != sequence.end());
            sequence[value] = i;
            NSaas::AddSimpleFactor("stat7", ToString(f2), *messages[i].MutableDocument()->MutableFactors());
            NSaas::AddSimpleFactor("stat2", ToString(rand() % 100), *messages[i].MutableDocument()->MutableFactors());
            NSaas::AddSimpleFactor("stat3", ToString(f0), *messages[i].MutableDocument()->MutableFactors());
            NSaas::AddSimpleFactor("stat4", ToString(f1), *messages[i].MutableDocument()->MutableFactors());
            NSaas::AddSimpleFactor("stat6", ToString(rand() % 100), *messages[i].MutableDocument()->MutableFactors());

            ::NRTYServer::TMessage::TDocument::TProperty* prop = messages[i].MutableDocument()->AddDocumentProperties();
            prop->set_name("value");
            prop->set_value(ToString(value));
        }
        IndexMessages(messages, indexer, 1);

        TVector<NRTYServer::TMessage> updates;
        for (auto&& m : messages) {
            NRTYServer::TMessage u;
            u.SetMessageType(NRTYServer::TMessage::DEPRECATED__UPDATE_DOCUMENT);
            u.SetMessageId(m.GetMessageId() + 1000);
            u.MutableDocument()->SetUrl(m.GetDocument().GetUrl());
            u.MutableDocument()->SetKeyPrefix(m.GetDocument().GetKeyPrefix());
            NSaas::AddSimpleFactor("factor_for_update", ToString(rand() % 100), *u.MutableDocument()->MutableFactors());
            updates.push_back(u);
        }

        IndexMessages(updates, indexer, 1);
        ReopenIndexers();
    }
public:
    bool InitConfig() override {
        SetIndexerParams(ALL, 1000, 1);
        SetMergerParams(true, 1, -1, mcpNONE);
        (*ConfigDiff)["Searcher.FactorsInfo"] = GetResourcesDirectory() + "/factors/pruning_prepare_factors.cfg";
        (*ConfigDiff)["Indexer.Common.UseSlowUpdate"] = 0;
        SetPruneAttrSort("formula:default");
        return true;
    }

    bool Run() override {
        Test(DISK);
        return true;
    }
};
