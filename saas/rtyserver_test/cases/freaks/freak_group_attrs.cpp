#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/rtyserver_test/testerlib/standart_generator.h>
#include <util/generic/set.h>

START_TEST_DEFINE(TestIndexEmptyText)
bool Run() override {
    const ui64 countMessages = 3;
    TVector<NRTYServer::TMessage> messages;
    TStandartDocumentGenerator* sdg = new TStandartDocumentGenerator(GetIsPrefixed());
    sdg->SetTextConstant("");
    TStandartMessagesGenerator smg(sdg, true);
    GenerateInput(messages, countMessages, smg);
    IndexMessages(messages, REALTIME, 1);

    return true;
}

bool InitConfig() override {
    SetIndexerParams(DISK, 100000, 1);
    SetIndexerParams(REALTIME, 100, 1);
    return true;
}
};

START_TEST_DEFINE(TestPruningWithFreakAttrs)
    bool Run() override {
        const ui64 countMessages = 3;
        TVector<NRTYServer::TMessage> messages, messagesFail;
        TStandartDocumentGenerator* sdg = new TStandartDocumentGenerator(GetIsPrefixed());
        if (GetIsPrefixed())
            sdg->SetPrefixConstant(1);

        TStandartAttributesFiller* saf1 = new TStandartAttributesFiller();
        saf1->AddCommonAttribute("unique_attr", "1", TStandartAttributesFiller::atGroup);

        TStandartAttributesFiller* saf2 = new TStandartAttributesFiller();
        saf2->AddCommonAttribute("unique_attr", "2", TStandartAttributesFiller::atGroup);

        TStandartMessagesGenerator smg(sdg, true);
        GenerateInput(messages, countMessages, smg);

        bool wasError = true;

        try {
            IndexMessages(messages, REALTIME, 1);
            ReopenIndexers();
            wasError = false;
        } catch (...) {
        }
        if (!wasError)
            ythrow yexception() << "Pruning incorrect situation don't cause error!! A";

        messages.clear();
        sdg->RegisterFiller("prune_attrs_1", saf1);
        GenerateInput(messages, countMessages, smg);

        IndexMessages(messages, REALTIME, 1);

        CheckSearchResults(messages);
        wasError = true;

        messagesFail.clear();
        sdg->RegisterFiller("prune_attrs_2", saf2);
        GenerateInput(messagesFail, countMessages, smg);

        try {
            IndexMessages(messagesFail, REALTIME, 1);
            wasError = false;
        } catch (...) {
        }
        if (!wasError)
            ythrow yexception() << "Pruning incorrect situation don't cause error!! C";
        CheckSearchResults(messages);
        CheckSearchResults(messagesFail, TSet<std::pair<ui64, TString> >(), 0);

        return true;
}

bool InitConfig() override {
    SetIndexerParams(DISK, 100000, 1);
    SetIndexerParams(REALTIME, 100, 1);
    SetPruneAttrSort("unique_attr");
    return true;
}
};

START_TEST_DEFINE(TestNegativeGroupAttrs)
    bool Run() override {
        const ui64 countMessages = 3;
        TVector<NRTYServer::TMessage> messages, messagesFail;
        TStandartDocumentGenerator* sdg = new TStandartDocumentGenerator(GetIsPrefixed());
        if (GetIsPrefixed())
            sdg->SetPrefixConstant(1);

        TStandartAttributesFiller* saf1 = new TStandartAttributesFiller();
        saf1->SetDocumentsCount(3);
        saf1->AddDocAttribute(0, "unique_attr", "-1", TStandartAttributesFiller::atGroup);
        saf1->AddDocAttribute(1, "unique_attr", "1", TStandartAttributesFiller::atGroup);
        saf1->AddDocAttribute(2, "unique_attr", "1", TStandartAttributesFiller::atGroup);

        TStandartAttributesFiller* saf2 = new TStandartAttributesFiller();
        saf2->AddCommonAttribute("unique_attr", "2", TStandartAttributesFiller::atGroup);

        TStandartMessagesGenerator smg(sdg, true);
        sdg->RegisterFiller("aaa", saf1);
        GenerateInput(messages, countMessages, smg);

        bool wasError = true;

        try {
            IndexMessages(messages, REALTIME, 1);
            ReopenIndexers();
            wasError = false;
        } catch (...) {
        }
        if (!wasError)
            ythrow yexception() << "Incorrect GA don't cause error!!";

        return true;
}

bool InitConfig() override {
    SetIndexerParams(DISK, 100000, 1);
    SetIndexerParams(REALTIME, 100, 1);
    return true;
}
};

START_TEST_DEFINE(TestPruningWithnoAttrsLater)
bool Run() override {
    const ui64 countMessages = 3;
    TVector<NRTYServer::TMessage> messages;
    TStandartDocumentGenerator* sdg = new TStandartDocumentGenerator(GetIsPrefixed());
    if (GetIsPrefixed())
        sdg->SetPrefixConstant(1);
    TStandartMessagesGenerator smg(sdg, true);
    GenerateInput(messages, countMessages, smg);
    IndexMessages(messages, REALTIME, 1);
    ReopenIndexers();

    SetPruneAttrSort("unique_attr");
    ApplyConfig();
    CheckSearchResults(messages);
    return true;
}

bool InitConfig() override {
    SetIndexerParams(DISK, 100000, 1);
    SetIndexerParams(REALTIME, 100, 1);
    return true;
}
};

