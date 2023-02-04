#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/rtyserver/components/erf/erf_manager.h>
#include <saas/rtyserver/components/erf/erf_disk.h>
#include <saas/rtyserver/components/erf/erf_component.h>
#include <saas/rtyserver/components/erf/erf_parsed_entity.h>
#include <saas/api/factors_erf.h>
#include <kernel/web_factors_info/factor_names.h>
#include <library/cpp/packedtypes/packedfloat.h>

SERVICE_TEST_RTYSERVER_DEFINE(TestUPDATE_DOCUMENT)
    bool Test(TIndexerType indexer, bool reopenBeforeUpdate, bool reopenAfterUpdate, bool restart, bool changeConfig = false) {
        const size_t countMessages = 10;
        const size_t countUpdate = countMessages / 2;
        TAttrMap::value_type map1, map2;
        ui64 value2 = (1llu << 34) - 1;
        map1["unique_attr"] = 1;
        map2["unique_attr"] = value2;
        TAttrMap initAttrs(countMessages, map1);
        TAttrMap updateAttrs(countUpdate, map2);

        TVector<NRTYServer::TMessage> messages, update;
        GenerateInput(messages, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), initAttrs);
        GenerateInput(update, countUpdate, NRTYServer::TMessage::DEPRECATED__UPDATE_DOCUMENT, GetIsPrefixed(), updateAttrs, "");
        const TString kps = GetAllKps(messages);
        for (size_t i = 0; i < countUpdate; ++i) {
            update[i].MutableDocument()->SetUrl(messages[i].GetDocument().GetUrl());
            update[i].MutableDocument()->SetKeyPrefix(messages[i].GetDocument().GetKeyPrefix());
            update[i].MutableDocument()->clear_searchattributes();
            update[i].MutableDocument()->clear_factors();
            update[i].MutableDocument()->clear_documentproperties();
        }
        IndexMessages(messages, indexer, 1);
        if (reopenBeforeUpdate)
            ReopenIndexers();
        if (changeConfig) {
            Controller->ProcessCommand("stop");
            (*ConfigDiff)["Indexer.Common.Groups"] = "unique_attr:2:unique";
            Controller->ApplyConfigDiff(ConfigDiff);
            Controller->RestartServer(true);
        }
        IndexMessages(update, indexer, 1);
        if (restart) {
            Controller->RestartServer(true);
            Controller->WaitIsRepairing();
        }
        if (reopenAfterUpdate)
            ReopenIndexers();
        else
            sleep(5);

        TVector<TDocSearchInfo> results;
        QuerySearch("\"body\"" + kps, results);
        if (results.size() != countMessages)
            ythrow yexception() << "Incorrect count of docs: " << results.size() << " != " << countMessages;

        QuerySearch("\"body\"&fa=unique_attr:" + ToString(value2) + kps, results);
        if (results.size() != countUpdate)
            ythrow yexception() << "Incorrect count of updated docs with unique_attr:2, " << results.size() << " != " << countUpdate;

        QuerySearch("\"body\"&fa=unique_attr:1" + kps, results);
        if (results.size() != countMessages - countUpdate)
            ythrow yexception() << "Incorrect count of not updated docs with unique_attr:1, " << results.size() << " != " << countMessages - countUpdate;
        return true;
    }
    bool InitConfig() override {
        SetIndexerParams(DISK, 100, 1);
        SetIndexerParams(REALTIME, 100, 1);
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestUPDATE_DOCUMENT_DISK, TestUPDATE_DOCUMENT)
    bool Run() override {
        return Test(DISK, true, false, false);
    }
};

START_TEST_DEFINE_PARENT(TestUPDATE_DOCUMENT_MEMORY, TestUPDATE_DOCUMENT)
    bool Run() override {
        return Test(REALTIME, false, false, false);
    }
};

START_TEST_DEFINE_PARENT(TestUPDATE_DOCUMENT_TEMP, TestUPDATE_DOCUMENT)
    bool Run() override {
        return Test(DISK, false, true, false);
    }
};

START_TEST_DEFINE_PARENT(TestUPDATE_DOCUMENT_DISK_CHANGE_CONFIG, TestUPDATE_DOCUMENT)
bool Run() override {
    return Test(DISK, true, false, false, true);
}
bool InitConfig() override {
    if (!TestUPDATE_DOCUMENT::InitConfig())
        return false;
    (*ConfigDiff)["Indexer.Common.Groups"] = "unique_attr:1";
    return true;
}
};

START_TEST_DEFINE_PARENT(TestUPDATE_DOCUMENT_REPAIR, TestUPDATE_DOCUMENT)
    bool Run() override {
        return Test(DISK, false, true, true);
    }
    bool InitConfig() override {
        if(!TestUPDATE_DOCUMENT::InitConfig())
            return false;
        SetEnabledRepair();
        return true;
    }
};

SERVICE_TEST_RTYSERVER_DEFINE(TestUPDATE_DOCUMENT_ADV)
void Test(bool restart, bool reopen) {
    const size_t countMessages = 10;
    const size_t countUpdate = countMessages / 2;
    TAttrMap::value_type map1, map2;
    map1["unique_attr"] = 1;
    map2["unique_attr"] = 2;
    TAttrMap initAttrs(countMessages, map1);
    TAttrMap updateAttrs(countUpdate, map2);

    TVector<NRTYServer::TMessage> messages, update;
    GenerateInput(messages, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), initAttrs);
    GenerateInput(update, countUpdate, NRTYServer::TMessage::DEPRECATED__UPDATE_DOCUMENT, GetIsPrefixed(), updateAttrs, "");
    for (size_t i = 0; i < countUpdate; ++i) {
        update[i].MutableDocument()->SetUrl(messages[i].GetDocument().GetUrl());
        update[i].MutableDocument()->SetKeyPrefix(messages[i].GetDocument().GetKeyPrefix());
        update[i].MutableDocument()->clear_searchattributes();
        update[i].MutableDocument()->clear_factors();
        update[i].MutableDocument()->clear_documentproperties();
    }
    IndexMessages(messages, DISK, 1);
    IndexMessages(update, DISK, 1);
    IndexMessages(messages, DISK, 1);
    if (restart) {
        Controller->RestartServer(true);
        Controller->WaitIsRepairing();
    }
    if (reopen)
        ReopenIndexers();
    TVector<TDocSearchInfo> results;
    QuerySearch("\"body\"&fa=unique_attr:1" + GetAllKps(messages), results);
    if (results.size() != countMessages)
        ythrow yexception() << "Incorrect count of not updated docs with unique_attr:1, " << results.size() << " != " << countMessages;
}
bool InitConfig() override {
    SetIndexerParams(DISK, 100, 1);
    SetIndexerParams(REALTIME, 100, 1);
    return true;
}
};

START_TEST_DEFINE_PARENT(TestUPDATE_DOCUMENT_ADV_TEMP, TestUPDATE_DOCUMENT_ADV)
bool Run() override {
    Test(false, true);
    return true;
}
};

START_TEST_DEFINE_PARENT(TestUPDATE_DOCUMENT_ADV_REPAIR, TestUPDATE_DOCUMENT_ADV)
bool Run() override {
    Test(true, false);
    return true;
}
bool InitConfig() override {
    if(!TestUPDATE_DOCUMENT_ADV::InitConfig())
        return false;
    SetEnabledRepair();
    return true;
}
};

SERVICE_TEST_RTYSERVER_DEFINE(TestUpdateDeferred)
protected:
class TMessageProcessor: public IMessageProcessor {
private:
    TestUpdateDeferred& Owner;
public:

    TMessageProcessor(TestUpdateDeferred& owner)
        : Owner(owner)
    {
        RegisterGlobalMessageProcessor(this);
    }

    ~TMessageProcessor() {
        UnregisterGlobalMessageProcessor(this);
    }

    bool Process(IMessage* message) override {
        TMessageDeferredUpdatesNotification* messNotify = dynamic_cast<TMessageDeferredUpdatesNotification*>(message);
        if (messNotify) {
            Owner.OnUpdatesApplied(messNotify->CountUpdates);
            return true;
        }

        TMessageDeferredUpdaterActivated* mess = dynamic_cast<TMessageDeferredUpdaterActivated*>(message);
        if (mess) {
            if (mess->DeferredUpdateMoment == TMessageDeferredUpdaterActivated::EDeferredUpdateMoment::dumCloseIndex) {
                Owner.OnCloseIndex();
                return true;
            }

            if (mess->DeferredUpdateMoment == TMessageDeferredUpdaterActivated::EDeferredUpdateMoment::dumMerge) {
                Owner.OnMerge();
                return true;
            }
        }

        return false;
    }

    TString Name() const override {
        return "TestPruningByStatFormulaDeferredUpdate";
    };

};

void PrepareMessages(TVector<NRTYServer::TMessage>& messages, TVector<NRTYServer::TMessage>& updates, TVector<ui32>* ua1 = nullptr) {
    CHECK_WITH_LOG(messages.size() == updates.size());
    CHECK_WITH_LOG(!ua1 || (ua1->size() == updates.size()));
    for (size_t i = 0; i < messages.size(); ++i) {
        messages[i].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());
        {
            auto* attr = messages[i].MutableDocument()->AddGroupAttributes();
            attr->SetName("unique_attr_1");
            if (!ua1) {
                attr->SetValue(ToString(rand() % 256));
            } else {
                attr->SetValue(ToString((*ua1)[i]));
            }
            attr->SetType(NRTYServer::TAttribute::INTEGER_ATTRIBUTE);
        }

        {
            auto* attr = messages[i].MutableDocument()->AddGroupAttributes();
            attr->SetName("unique_attr");
            attr->SetValue("1");
            attr->SetType(NRTYServer::TAttribute::INTEGER_ATTRIBUTE);
        }
    }
    for (size_t i = 0; i < messages.size(); ++i) {
        updates[i].MutableDocument()->SetUrl(messages[i].GetDocument().GetUrl());
        updates[i].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());
        updates[i].MutableDocument()->clear_groupattributes();
        auto* attr = updates[i].MutableDocument()->AddGroupAttributes();
        attr->SetName("unique_attr");
        attr->SetValue("2");
        attr->SetType(NRTYServer::TAttribute::INTEGER_ATTRIBUTE);
        updates[i].MutableDocument()->clear_body();
        updates[i].MutableDocument()->clear_searchattributes();
        updates[i].MutableDocument()->clear_factors();
        updates[i].MutableDocument()->clear_documentproperties();
    }
}

bool OnMergeFlag = false;
bool OnCloseIndexFlag = false;

TSystemEvent EventClose;
ui32 CountUpdates = 0;

void OnUpdatesApplied(ui32 count) {
    CountUpdates = count;
}

virtual void DoOnCloseIndex() = 0;
virtual void DoOnMerge() = 0;

void OnCloseIndex() {

    DoOnCloseIndex();
    OnCloseIndexFlag = true;
    EventClose.Signal();
}

void OnMerge() {
    DoOnMerge();
    OnMergeFlag = true;
    EventClose.Signal();
}

};

START_TEST_DEFINE_PARENT(TestUpdateDeferredOnClose, TestUpdateDeferred)

TVector<NRTYServer::TMessage> Updates;

void DoOnCloseIndex() override {
    try {
        TIndexerClient::TContext context;
        context.DoWaitIndexing = false;
        context.DoWaitReply = false;
        IndexMessages(Updates, DISK, context);
    } catch (...) {
    }
}

void DoOnMerge() override {
}

bool Run() override {

    const ui32 countMessages = 100;
    TMessageProcessor processor(*this);
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    GenerateInput(Updates, countMessages, NRTYServer::TMessage::DEPRECATED__UPDATE_DOCUMENT, GetIsPrefixed());
    PrepareMessages(messages, Updates);

    OnCloseIndexFlag = OnMergeFlag = false;
    IndexMessages(messages, DISK, 1);

    TSet<std::pair<ui64, TString> > deleted;
    DeleteSomeMessages(messages, deleted, DISK);

    ReopenIndexers();
    CHECK_TEST_TRUE(CountUpdates);

    EventClose.WaitT(TDuration::Minutes(1));
    CHECK_TEST_EQ(OnCloseIndexFlag, true);
    CHECK_TEST_EQ(OnMergeFlag, false);

    TQuerySearchContext context;
    context.ResultCountRequirement = 90;
    context.AttemptionsCount = 10;
    TVector<TDocSearchInfo> results;
    QuerySearch("body&fa=unique_attr:2&kps=" + ToString(messages[0].GetDocument().GetKeyPrefix()), results, context);
    CHECK_TEST_EQ(results.size(), 90);

    return true;
}

bool InitConfig() override {
    SetIndexerParams(DISK, 200, 1);
    SetIndexerParams(REALTIME, 200, 1);
    SetPruneAttrSort("unique_attr_1");
    return true;
}

};

START_TEST_DEFINE_PARENT(TestUpdateDeferredOnMerge, TestUpdateDeferred)
TVector<NRTYServer::TMessage> Updates;

void DoOnCloseIndex() override {
}

void DoOnMerge() override {
    try {
        TIndexerClient::TContext context;
        context.DoWaitIndexing = false;
        context.DoWaitReply = false;
        IndexMessages(Updates, DISK, context);
    } catch (...) {
    }
}

bool Run() override {

    const ui32 countMessages = 1000;
    TMessageProcessor processor(*this);
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    GenerateInput(Updates, countMessages, NRTYServer::TMessage::DEPRECATED__UPDATE_DOCUMENT, GetIsPrefixed());
    PrepareMessages(messages, Updates);

    for (ui32 i = 0; i < 10; ++i) {
        IndexMessages(TVector<NRTYServer::TMessage>(messages.begin() + messages.size() / 10 * i, messages.begin() + messages.size() / 10 * (i + 1)), DISK, 1);
        ReopenIndexers();
    }
    CHECK_TEST_TRUE(!CountUpdates);

    TSet<std::pair<ui64, TString> > deleted;
    DeleteSomeMessages(messages, deleted, DISK);

    CHECK_TEST_EQ(OnCloseIndexFlag, true);
    CHECK_TEST_EQ(OnMergeFlag, false);

    TQuerySearchContext context;
    context.ResultCountRequirement = 1000;
    context.AttemptionsCount = 10;
    context.PrintResult = true;
    TVector<TDocSearchInfo> results;
    QuerySearch("body&fa=unique_attr:1&timeout=100000000&kps=" + ToString(messages[0].GetDocument().GetKeyPrefix()), results, context);
    CHECK_TEST_EQ(results.size(), 900);

    Controller->ProcessCommand("create_merger_tasks");
    Controller->ProcessCommand("do_all_merger_tasks");
    CheckMergerResult();
    CHECK_TEST_TRUE(CountUpdates);
    EventClose.WaitT(TDuration::Minutes(1));
    CHECK_TEST_EQ(OnMergeFlag, true);

    QuerySearch("body&fa=unique_attr:2&timeout=100000000&kps=" + ToString(messages[0].GetDocument().GetKeyPrefix()), results, context);
    CHECK_TEST_EQ(results.size(), 900);

    return true;
}

bool InitConfig() override {
    SetPruneAttrSort("unique_attr_1");
    SetMergerParams(true, 1, -1, mcpNONE);
    SetIndexerParams(DISK, 200, 1);
    SetIndexerParams(REALTIME, 200, 1);
    return true;
}

};

START_TEST_DEFINE_PARENT(TestUpdateDeferredOnMergeOneDoc, TestUpdateDeferred)
TVector<NRTYServer::TMessage> Updates;

void DoOnCloseIndex() override {
}

void DoOnMerge() override {
    try {
        TIndexerClient::TContext context;
        context.DoWaitIndexing = false;
        context.DoWaitReply = false;
        IndexMessages(Updates, DISK, context);
    } catch (...) {
    }
}

bool Run() override {

    const ui32 countMessages = 4;
    TMessageProcessor processor(*this);
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, countMessages, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    GenerateInput(Updates, countMessages, NRTYServer::TMessage::DEPRECATED__UPDATE_DOCUMENT, GetIsPrefixed());
    TVector<ui32> ua1 = {10, 2, 3, 4};
    PrepareMessages(messages, Updates, &ua1);

    for (ui32 i = 1; i < messages.size(); ++i) {
        messages[i].MutableDocument()->SetUrl("abcde");
        Updates[i].MutableDocument()->SetUrl("abcde");
    }

    IndexMessages(TVector<NRTYServer::TMessage>(messages.begin(), messages.begin() + 3), DISK, 1);
    ReopenIndexers();
    IndexMessages(TVector<NRTYServer::TMessage>(messages.begin() + 3, messages.begin() + 4), DISK, 1);
    ReopenIndexers();

    CHECK_TEST_TRUE(!CountUpdates);

    CHECK_TEST_EQ(OnCloseIndexFlag, true);
    CHECK_TEST_EQ(OnMergeFlag, false);

    TQuerySearchContext context;
    context.ResultCountRequirement = 1000;
    context.AttemptionsCount = 10;
    TVector<TDocSearchInfo> results;
    QuerySearch("body&fa=unique_attr:1&timeout=100000000&kps=" + ToString(messages[0].GetDocument().GetKeyPrefix()), results, context);
    CHECK_TEST_EQ(results.size(), 2);

    Controller->ProcessCommand("create_merger_tasks");
    Controller->ProcessCommand("do_all_merger_tasks");
    CheckMergerResult();
    CHECK_TEST_TRUE(CountUpdates);
    EventClose.WaitT(TDuration::Minutes(1));
    CHECK_TEST_EQ(OnMergeFlag, true);

    QuerySearch("body&fa=unique_attr:2&timeout=100000000&kps=" + ToString(messages[0].GetDocument().GetKeyPrefix()), results, context);
    CHECK_TEST_EQ(results.size(), 2);

    return true;
}

bool InitConfig() override {
    SetPruneAttrSort("unique_attr_1");
    SetMergerParams(true, 1, -1, mcpTIME, 50000000);
    SetIndexerParams(DISK, 200, 1);
    SetIndexerParams(REALTIME, 200, 1);
    (*ConfigDiff)["SearchersCountLimit"] = 1;
    return true;
}

};

START_TEST_DEFINE(TestUpdateArchive)
TAtomicSharedPtr<NRTYFactors::TConfig> FactorsConfig;

enum TWhatUpdate {
    BODY = 1,
    GROUP = 2,
    SEARCH = 4,
    PROPS = 8,
    FACTORS = 16
};

void Update(const TVector<NRTYServer::TMessage>& messages, TIndexerType indexer, ui32 what) {
    TVector<NRTYServer::TMessage> update = messages;
    for (TVector<NRTYServer::TMessage>::iterator i = update.begin(); i != update.end(); ++i) {
        if (!(what & BODY))
            i->MutableDocument()->clear_body();
        if (!(what & GROUP))
            i->MutableDocument()->clear_groupattributes();
        if (!(what & SEARCH))
            i->MutableDocument()->clear_searchattributes();
        if (!(what & PROPS))
            i->MutableDocument()->clear_documentproperties();
        if (!(what & FACTORS))
            i->MutableDocument()->clear_factors();
    }
    IndexMessages(update, indexer, 1);
}

bool Run() override {
    const size_t countMessages = 1;
    TAttrMap::value_type search1, search2, search3, group1, group2, group3;
    search1["attachsize"] = 1;
    search1["x_urls"] = TString("url_1");
    search2["attachsize"] = 2;
    search2["x_urls"] = TString("url_2");
    search3["attachsize"] = 3;
    search3["x_urls"] = TString("url_3");
    group1["unique_attr"] = 1;
    group2["unique_attr"] = 2;
    group3["unique_attr"] = 3;
    TVector<NRTYServer::TMessage> messages, update1, update2;
    GenerateInput(messages, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(countMessages, group1), "body1", true, TAttrMap(countMessages, search1));
    GenerateFactors(messages, 0.0);
    GenerateInput(update1, countMessages, NRTYServer::TMessage::DEPRECATED__UPDATE_DOCUMENT, GetIsPrefixed(), TAttrMap(countMessages, group2), "body2", true, TAttrMap(countMessages, search2));
    GenerateFactors(update1, 1.0);
    GenerateInput(update2, countMessages, NRTYServer::TMessage::DEPRECATED__UPDATE_DOCUMENT, GetIsPrefixed(), TAttrMap(countMessages, group3), "body3", true, TAttrMap(countMessages, search3));
    GenerateFactors(update2, 2.0);
    for (size_t i = 0; i < countMessages; ++i) {
        AddProp(messages[i], "some_prop", "value1");
        update1[i].MutableDocument()->SetUrl(messages[i].GetDocument().GetUrl());
        update1[i].MutableDocument()->SetKeyPrefix(messages[i].GetDocument().GetKeyPrefix());
        AddProp(update1[i], "some_prop", "value2");
        update2[i].MutableDocument()->SetUrl(messages[i].GetDocument().GetUrl());
        update2[i].MutableDocument()->SetKeyPrefix(messages[i].GetDocument().GetKeyPrefix());
        AddProp(update2[i], "some_prop", "value3");
    }
    const TString kps =  GetAllKps(messages);
    IndexMessages(messages, REALTIME, 1);
    Sleep(TDuration::Seconds(1));
    CheckDocPropety("body1" + kps, "value1", "Init: ", countMessages);

    Update(update1, REALTIME, BODY | GROUP);
    Sleep(TDuration::Seconds(1));
    CheckQuery("body1" + kps, "body2" + kps, "Memory: Incorrect count docs ", countMessages);
    CheckQuery("body2&fa=unique_attr:1" + kps, "body2&fa=unique_attr:2" + kps, "Memory: Incorrect count docs ", countMessages);
    CheckQuery("attachsize:2" + kps, "attachsize:1" + kps, "Memory: Incorrect count docs ", countMessages);
    CheckQuery("x_urls:url_2" + kps, "x_urls:url_1" + kps, "Memory: Incorrect count docs ", countMessages);
    CheckDocPropety("body2" + kps, "value1", "Init: ", countMessages);

    Update(update1, REALTIME, SEARCH);
    Sleep(TDuration::Seconds(1));
    CheckQuery("body1" + kps, "body2" + kps, "Memory: Incorrect count docs ", countMessages);
    CheckQuery("body2&fa=unique_attr:1" + kps, "body2&fa=unique_attr:2" + kps, "Memory: Incorrect count docs ", countMessages);
    CheckQuery("attachsize:1" + kps, "attachsize:2" + kps, "Memory: Incorrect count docs ", countMessages);
    CheckQuery("x_urls:url_1" + kps, "x_urls:url_2" + kps, "Memory: Incorrect count docs ", countMessages);
    CheckDocPropety("body2" + kps, "value1", "Init: ", countMessages);

    Update(update1, REALTIME, PROPS);
    CheckDocPropety("body2" + kps, "value2", "Memory: ", countMessages);

    ReopenIndexers();
    Update(update2, DISK, BODY | GROUP);
    ReopenIndexers();
    CheckQuery("body2" + kps, "body3" + kps, "Disk: Incorrect count docs ", countMessages);
    CheckQuery("body3&fa=unique_attr:2" + kps, "body3&fa=unique_attr:3" + kps, "Memory: Incorrect count docs ", countMessages);

    Update(update2, DISK, SEARCH);
    ReopenIndexers();
    CheckQuery("attachsize:2" + kps, "attachsize:3" + kps, "Disk: Incorrect count docs ", countMessages);
    CheckQuery("x_urls:url_2" + kps, "x_urls:url_3" + kps, "Disk: Incorrect count docs ", countMessages);

    Update(update2, DISK, PROPS | FACTORS);
    ReopenIndexers();
    CheckDocPropety("body3" + kps, "value3", "Disk: ", countMessages);
    CheckFactors(messages, 2.0);

    return true;
}

void CheckQuery(const TString& query1, const TString& query2, const TString& comment, size_t countMessages) {
    TVector<TDocSearchInfo> results;
    QuerySearch(query1, results);
    if (results.size() != 0)
        ythrow yexception() << comment << query1 << ": " << results.size() << " != " << 0;
    QuerySearch(query2, results);
    if (results.size() != countMessages)
        ythrow yexception() << comment << query2 << ": " << results.size() << " != " << countMessages;
}
void CheckDocPropety(const TString& query, const TString& right_value, const TString& comment, size_t countMessages) {
    TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > > docProperties;
    TVector<TDocSearchInfo> results;
    QuerySearch(query, results, &docProperties);
    if (results.size() != countMessages)
        ythrow yexception() << comment << right_value << ": " << results.size() << " != " << countMessages;
    for (size_t i = 0; i < countMessages; ++i) {
        THashMultiMap<TString, TString>::const_iterator iter = docProperties[i]->find("some_prop");
        if (iter == docProperties[i]->end())
            ythrow yexception() << comment << "there is no property some prop in doc " << results[i].GetUrl();
        if (iter->second != right_value)
            ythrow yexception() << comment << "incorrect property value in doc " << results[i].GetUrl() << ", " << iter->second << " != " << right_value;
    }
}

void AddProp(NRTYServer::TMessage& message, const TString& name, const TString& value) {
    NRTYServer::TMessage::TDocument::TProperty& prop = *message.MutableDocument()->AddDocumentProperties();
    prop.set_name(name);
    prop.set_value(value);
}

void GenerateFactors(TVector<NRTYServer::TMessage>& messages, float step) {
    for (TVector<NRTYServer::TMessage>::iterator i = messages.begin(); i != messages.end(); ++i) {
        for (size_t fact = 0; fact < FactorsConfig->StaticFactors().size(); ++fact) {
            NSaas::AddSimpleFactor(FactorsConfig->StaticFactors()[fact].Name, ToString(step * ((i - messages.begin()) * FactorsConfig->StaticFactors().size() + fact)), *i->MutableDocument()->MutableFactors());
        }
    }
}

void CheckFactors(const TVector<NRTYServer::TMessage>& messages, float step) {
    const TSet<TString> finalIndexes = Controller->GetFinalIndexes();
    Controller->ProcessCommand("stop");
    TVector<TSimpleSharedPtr<IRTYErfManager> > erfManagers;
//    for (TSet<TString>::const_iterator i = finalIndexes.begin(); i != finalIndexes.end(); ++i) {
    TRTYStaticFactorsConfig factorsConfigDescr(FactorsConfig.Get());
        TRTYErfDiskManager::TCreationContext cc(TPathName{*finalIndexes.rbegin()}, "indexerf.rty", &factorsConfigDescr);
        cc.ReadOnly = true;
        erfManagers.push_back(new TRTYErfDiskManager(cc, ERF_COMPONENT_NAME));
        erfManagers.back()->Open();
//    }
    TVector<bool> docsOk(messages.size(), false);
    for (size_t i = 0; i < erfManagers.size(); ++i) {
        IRTYErfManager& manager = *erfManagers[i];
        for (size_t docid = 0; docid < manager.Size(); ++docid) {
            TBasicFactorStorage factors(N_FACTOR_COUNT - 13); // "13" is arbitrary
            if (!manager.ReadRaw(factors, docid))
                ythrow yexception() << "error on read";
            float messIndex = factors[0] / FactorsConfig->StaticFactors().size();
            if ((i64)messIndex != messIndex || messIndex < 0 || messIndex >= messages.size())
                ythrow yexception() << "error on first factor";
            size_t mess = messIndex;
            for (size_t f = 1; f < FactorsConfig->StaticFactors().size(); ++f)
                if (fabs(factors[f] - factors[0] - f * step) > 1e-4)
                    ythrow yexception() << "error on factor " << f << ", url = " << messages[mess].GetDocument().GetUrl();
            docsOk[mess] = true;
        }
    }
    for (TVector<bool>::const_iterator i = docsOk.begin(); i != docsOk.end(); ++i)
        if (!*i)
            ythrow yexception() << "erf not found for url " << messages[i - docsOk.begin()].GetDocument().GetUrl();
}

bool InitConfig() override {
    if (!NFs::Exists(FactorsFileName))
        ythrow yexception() << "this test must be started with correct factors info";
    FactorsConfig.Reset(new NRTYFactors::TConfig(FactorsFileName.data()));
    (*ConfigDiff)["Searcher.FactorsInfo"] = FactorsFileName;
    SetIndexerParams(DISK, 100, 1);
    SetIndexerParams(REALTIME, 100, 1);
    return true;
}
};

START_TEST_DEFINE_PARENT(TestRestoreZones, TTestUpdateArchiveCaseClass)

TString Kps;

void CheckQuery(const TString& query, int count) {
    TVector<TDocSearchInfo> results;
    QuerySearch(query + Kps, results);
    if (results.ysize() != count) {
        ythrow yexception() << "Query '" << query + Kps << "'results count incorrect: " << results.ysize() << " != " << count;
    }
}

bool Run() override {
    const size_t countMessages = 1;
    TAttrMap::value_type searchAttrs;
    searchAttrs["attachsize"] = 1;
    searchAttrs["x_urls"] = TString("url_1");
    TVector<NRTYServer::TMessage> messages, update;
    const TString body("<xml><hhh><aaa zone_attr=\"asd\">кириллица</aaa></hhh><hhh zone_attr=\"bbb\">lololo lalala lululu</hhh><aaa>cccccc</aaa></xml>");
    GenerateInput(messages, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), body);
    GenerateInput(update, countMessages, NRTYServer::TMessage::DEPRECATED__UPDATE_DOCUMENT, GetIsPrefixed(), TAttrMap(), "глаголица", true, TAttrMap(countMessages, searchAttrs));
    for (size_t i = 0; i < countMessages; ++i) {
        messages[i].MutableDocument()->SetMimeType("text/xml");
        update[i].MutableDocument()->SetUrl(messages[i].GetDocument().GetUrl());
        update[i].MutableDocument()->SetKeyPrefix(messages[i].GetDocument().GetKeyPrefix());
        update[i].MutableDocument()->SetMimeType(messages[i].GetDocument().GetMimeType());
    }

    Kps =  GetAllKps(messages);
    IndexMessages(messages, REALTIME, 1);
    Sleep(TDuration::Seconds(1));
    CheckQuery("hhh:(zone_attr:\"bbb\")", countMessages);
    CheckQuery("zone_attr:\"bbb\"", countMessages);
    CheckQuery("zone_attr:\"asd\"", countMessages);
    CheckQuery("aaa:(zone_attr:\"bbb\")", 0);
    CheckQuery("aaa:(zone_attr:\"asd\")", countMessages);
    CheckQuery("aaa:(кириллица)", countMessages);
    Update(update, REALTIME, SEARCH);
    Sleep(TDuration::Seconds(1));
    CheckQuery("hhh:(zone_attr:\"bbb\")", countMessages);
    CheckQuery("zone_attr:\"bbb\"", countMessages);
    CheckQuery("zone_attr:\"asd\"", countMessages);
    CheckQuery("aaa:(zone_attr:\"bbb\")", 0);
    CheckQuery("aaa:(zone_attr:\"asd\")", countMessages);
    CheckQuery("aaa:(кириллица)", countMessages);
    CheckQuery("attachsize:1", countMessages);
    CheckQuery("x_urls:url_1", countMessages);
    for (size_t i = 0; i < countMessages; ++i)
        update[i].MutableDocument()->SetMimeType("text/html");
    Update(update, REALTIME, BODY);
    Sleep(TDuration::Seconds(1));
    CheckQuery("глаголица", countMessages);
    Update(update, REALTIME, SEARCH);
    Sleep(TDuration::Seconds(1));
    CheckQuery("глаголица", countMessages);
    return true;
}
bool InitConfig() override {
    SetIndexerParams(DISK, 100, 1);
    SetIndexerParams(REALTIME, 100, 1);
    (*ConfigDiff)["Indexer.Common.XmlParserConfigFile"] = "";
    (*ConfigDiff)["Indexer.Common.HtmlParserConfigFile"] = "";
    return true;
}
};
