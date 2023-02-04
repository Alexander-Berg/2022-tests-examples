#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/rtyserver_test/testerlib/standart_generator.h>
#include <saas/api/factors_erf.h>
#include <search/idl/meta.pb.h>

#include <google/protobuf/text_format.h>

START_TEST_DEFINE(TestGROUP_ATTRS)
    bool Run() override {
        if (GetIsPrefixed())
            return true;
        const int CountMessages = 2;
        TAttrMap::value_type map1, map2;
        map1["mid"] = 1;
        map2["mid"] = 2;
        TVector<NRTYServer::TMessage> messagesForMemory1, messagesForMemory2;
        GenerateInput(messagesForMemory1, CountMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(CountMessages, map1));
        GenerateInput(messagesForMemory2, CountMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(CountMessages, map2));
        IndexMessages(messagesForMemory1, REALTIME, 1);
        IndexMessages(messagesForMemory2, REALTIME, 1);
        CheckSearchResults(messagesForMemory1);
        CheckSearchResults(messagesForMemory2);

        TVector<TDocSearchInfo> results;
        QuerySearch("\"body\"&fa=mid:1", results);
        if (results.size() != 2)
            ythrow yexception() << "BEFORE: Incorrect count of docs with mid:1";
        QuerySearch("\"body\"", results);
        if (results.size() != 4)
            ythrow yexception() << "BEFORE: Incorrect count of docs";
        QuerySearch("\"body\"&fa=mid:2", results);
        if (results.size() != 2)
            ythrow yexception() << "BEFORE: Incorrect count of docs with mid:2";
        ReopenIndexers();
        QuerySearch("\"body\"&fa=mid:1,2", results);
        if (results.size() != 4)
            ythrow yexception() << "AFTER: Incorrect count of docs with mid:1,2";
        QuerySearch("\"body\"&fa=mid:1", results);
        if (results.size() != 2)
            ythrow yexception() << "AFTER: Incorrect count of docs with mid:1";
        QuerySearch("\"body\"&fa=mid:2", results);
        if (results.size() != 2)
            ythrow yexception() << "AFTER: Incorrect count of docs with mid:2";
        QuerySearch("\"body\"", results);
        if (results.size() != 4)
            ythrow yexception() << "AFTER: Incorrect count of docs";
        QuerySearch("\"body\"&fa=middd:2", results);
        QuerySearch("\"body\"&fa=mid:2", results);
        if (results.size() != 2)
            ythrow yexception() << "AFTER: Incorrect count of docs with mid:2";
        return true;
    }
};

START_TEST_DEFINE(TestGROUP_ATTRS_INTERVALS)

TString kps;

void Test() {
    TVector<TDocSearchInfo> results;
    for (ui32 i = 0; i < 3; ++i) {
        QuerySearch("\"body\"&fa=mid:1-20,30,40-42&how=mid&numdoc=10000&timeout=9999999" + kps, results, nullptr, nullptr, true);
        if (results.size() == 24)
            break;
    }
    if (results.size() != 24)
        ythrow yexception() << "Incorrect count of docs " << results.size();
    results.clear();
    for (ui32 i = 0; i < 3; ++i) {
        QuerySearch("\"body\"&how=mid&numdoc=10000&fa=mid:801-999,1900-;mid1:-1030,1900-&timeout=9999999" + kps, results, nullptr, nullptr, true);
        if (results.size() == 130)
            break;
    }
    if (results.size() != 130)
        ythrow yexception() << "Incorrect count of docs " << results.size();
}


bool Run() override {
    const int CountMessages = 2000;
    TAttrMap::value_type map1, map2;
    TVector<TMap<TString, TAttr> > groupAttrs;
    groupAttrs.resize(CountMessages);
    for (int i = 0; i < CountMessages; i++) {
        groupAttrs[i]["mid"] = i;
        groupAttrs[i]["mid1"] = CountMessages - i;
    }
    TVector<NRTYServer::TMessage> messagesForMemory1, messagesForMemory2;
    GenerateInput(messagesForMemory1, CountMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), groupAttrs);

    if (GetIsPrefixed()) {
        for (int i = 0; i < messagesForMemory1.ysize(); i++) {
            messagesForMemory1[i].MutableDocument()->SetKeyPrefix(1);
        }
        kps = "&kps=1";
    } else
        kps = "&kps=0";

    IndexMessages(messagesForMemory1, REALTIME, 1);

    Test();
    ReopenIndexers();
    Test();

    return true;
}
};

START_TEST_DEFINE(TestGROUP_ATTRS_MERGER)
bool Run() override {
    if (GetIsPrefixed())
        return true;
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 2, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed(), TAttrMap(), "body", false);
    IndexMessages(messages, REALTIME, 1);
    ReopenIndexers();
    messages.erase(messages.begin());
    NRTYServer::TAttribute& attr = *messages.back().MutableDocument()->AddGroupAttributes();
    attr.SetName("dafaf");
    attr.SetValue("1");
    attr.SetType(NRTYServer::TAttribute::INTEGER_ATTRIBUTE);
    IndexMessages(messages, REALTIME, 1);
    TVector<TDocSearchInfo> results;
    QuerySearch("body&fa=dafaf:1", results);
    if (results.size() != 1)
        ythrow yexception() << "error in memory";
    ReopenIndexers();
    Controller->ProcessCommand("create_merger_tasks");
    Controller->ProcessCommand("do_all_merger_tasks");
    results.clear();
    QuerySearch("body&fa=dafaf:1", results);
    if (results.size() != 1)
        ythrow yexception() << "error in memory";
    return true;
}

bool InitConfig() override {
    SetMergerParams(true, 1);
    SetIndexerParams(DISK, 100, 1);
    SetIndexerParams(REALTIME, 100);
    return true;
}

};

SERVICE_TEST_RTYSERVER_DEFINE(TestGROUP_ATTRS_INCORRECT)
void TestAttr(TIndexerType indexer) {

    if (!SendIndexReply)
        ythrow yexception() << "Incorrect test configuration: this test not works withno reply";
    if (GetIsPrefixed())
        return;
    const int CountMessages = 2;
    TAttrMap::value_type map1, map2;
    map1["single"] = 0xFFFFFFFFF;
    map2["unknown"] = 999999999999;
    TVector<NRTYServer::TMessage> messages1, messages2;
    GenerateInput(messages1, CountMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(CountMessages, map1));
    GenerateInput(messages2, CountMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(CountMessages, map2));
    bool wereErrors = true;
    TRY
        IndexMessages(messages1, indexer, 1);
        wereErrors = false;
    CATCH ("All ok.")
    if (!wereErrors)
        ythrow yexception() << "We index 8-byte integer to 4-byte group attribute.";
    IndexMessages(messages2, indexer, 1);
    if (indexer == DISK)
        ReopenIndexers();

    CheckSearchResults(messages2);

    TVector<TDocSearchInfo> results;
    QuerySearch("\"body\"&fa=unknown:999999999999", results);
    if (results.size() != 2)
        ythrow yexception() << "BEFORE: Incorrect count of docs with unknown:0xAAAAAAAAA";
}
};

START_TEST_DEFINE_PARENT(TestGROUP_ATTRS_INCORRECT_DISK, TestGROUP_ATTRS_INCORRECT)
bool Run() override {
    TestAttr(DISK);
    return true;
}
};

START_TEST_DEFINE_PARENT(TestGROUP_ATTRS_INCORRECT_MEMORY, TestGROUP_ATTRS_INCORRECT)
bool Run() override {
    TestAttr(REALTIME);
    return true;
}
};

START_TEST_DEFINE(TestGROUP_ATTRS_DUPLICATE)
bool Run() override {
    if (GetIsPrefixed())
        return true;
    TAttrMap::value_type map;
    map["mid"] = 0;
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(1, map));
    const int attrCount = 1000;
    for (int i = 1; i < attrCount; ++i) {
        NRTYServer::TAttribute* attr = messages.back().MutableDocument()->AddGroupAttributes();
        attr->set_name("mid");
        attr->set_value(ToString(i).data());
        attr->set_type(NRTYServer::TAttribute::INTEGER_ATTRIBUTE);
    }

    IndexMessages(messages, REALTIME, 1);

    TVector<TDocSearchInfo> results;
    for (int i = 0; i < attrCount; ++i) {
        QuerySearch("\"body\"&fa=mid:" + ToString(i), results);
        if (results.size() != 1)
            ythrow yexception() << "BEFORE: Incorrect count of docs with mid:" << i;
    }
    QuerySearch("\"body\"&fa=mid:" + ToString(attrCount + 1), results);
    if (results.size() != 0)
        ythrow yexception() << "BEFORE: Incorrect count of docs with mid:" << (attrCount + 1);

    ReopenIndexers();

    for (int i = 0; i < attrCount; ++i) {
        QuerySearch("\"body\"&fa=mid:" + ToString(i), results);
        if (results.size() != 1)
            ythrow yexception() << "BEFORE: Incorrect count of docs with mid:" << i;
    }
    QuerySearch("\"body\"&fa=mid:" + ToString(attrCount + 1), results);
    if (results.size() != 0)
        ythrow yexception() << "BEFORE: Incorrect count of docs with mid:" << (attrCount + 1);

    return true;
}
};

SERVICE_TEST_RTYSERVER_DEFINE(TestGROUPING_ATTRS)
public:
    TString Kps;
public:
    bool PrepareData() {
        TAttrMap::value_type groupMap1;
        groupMap1["mid"] = 1;
        groupMap1["attr_cc_grp"] = TAttr("1_1");
        TAttrMap::value_type groupMap2;
        groupMap2["mid"] = 2;
        groupMap2["attr_cc_grp"] = TAttr("2_2");
        TAttrMap::value_type groupMap3;
        groupMap3["mid"] = 3;
        groupMap3["attr_cc_grp"] = TAttr("3_3");
        TAttrMap::value_type searchMap;
        searchMap["mid"] = TString("123");
        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, 10, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(10, groupMap1), "body", true, TAttrMap(10, searchMap));
        GenerateInput(messages, 10, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(10, groupMap2), "body", true, TAttrMap(10, searchMap));
        GenerateInput(messages, 10, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(10, groupMap3), "body", true, TAttrMap(10, searchMap));
        IndexMessages(messages, REALTIME, 1);
        Kps = GetAllKps(messages);
        return true;
    }

    bool CheckGroupping(unsigned kf = 1) {

        TVector<TDocSearchInfo> results;
        QuerySearch("body", results);
        if (results.size() != 30 * kf)
            ythrow yexception() << "documents aren't exists: " << results.size() << " != " << 30;

        TString searchResult = "";
        DEBUG_LOG << "Test A" << Endl;
        if (!CheckGroupsTry("/?ms=proto&text=body&g=1.mid.20.5&how=mid&numdoc=1000&dump=eventlog&timeout=9999999", 3, 5))
            return false;

        DEBUG_LOG << "Test B" << Endl;
        if (!CheckGroupsTry("/?ms=proto&text=body&g=1.mid.20.35&how=mid&numdoc=1000&dump=eventlog&timeout=9999999", 3, 10 * kf))
            return false;

        DEBUG_LOG << "Test C" << Endl;
        if (!CheckGroupsTry("/?ms=proto&text=body&g=1.mid.2.35&how=mid&numdoc=1000&dump=eventlog&timeout=9999999", 2, 10 * kf))
            return false;

        DEBUG_LOG << "Test D" << Endl;
        QuerySearch("body&g=1.mid.20.30&numdoc=1000&dump=eventlog&timeout=9999999", results);
        if (results.size() != 30 * kf)
            ythrow yexception() << "BEFORE: Incorrect count of docs with group attr mid:0";
        DEBUG_LOG << "Test E" << Endl;
        QuerySearch("body mid:\"123\"&numdoc=1000&dump=eventlog&timeout=9999999", results);
        if (results.size() != 30 * kf)
            ythrow yexception() << "BEFORE: Incorrect count of docs with search mid:123";
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestGROUP_ATTRS_WITHNO_DECLARATION, TestGROUPING_ATTRS)
bool Run() override {
    if (GetIsPrefixed())
        return true;
    TAttrMap::value_type map;
    TVector<NRTYServer::TMessage> messages;
    map["mid123"] = 1;
    GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(1, map));
    map["mid123"] = 2;
    GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(1, map));
    map["mid123"] = 3;
    GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(1, map));

    IndexMessages(messages, REALTIME, 1);

    TString searchResult = "";
    if (200 != ProcessQuery("/?ms=proto&text=body&g=1.mid123.3.15&numdoc=1000", &searchResult))
        return false;
    CheckGroups(searchResult, 3, 1);

    if (200 != ProcessQuery("/?ms=proto&text=body&g=1.mid123.2.15&numdoc=1000", &searchResult))
        return false;
    CheckGroups(searchResult, 2, 1);

    if (200 != ProcessQuery("/?ms=proto&text=body&g=1.mid222.2.15&numdoc=1000", &searchResult))
        return false;
    CheckGroups(searchResult, 0, 0, 0);

    ReopenIndexers();

    if (200 != ProcessQuery("/?ms=proto&text=body&g=1.mid123.3.15&numdoc=1000", &searchResult))
        return false;
    CheckGroups(searchResult, 3, 1);

    if (200 != ProcessQuery("/?ms=proto&text=body&g=1.mid123.2.15&numdoc=1000", &searchResult))
        return false;
    CheckGroups(searchResult, 2, 1);

    if (200 != ProcessQuery("/?ms=proto&text=body&g=1.mid222.2.15&numdoc=1000", &searchResult))
        return false;
    CheckGroups(searchResult, 0, 0, 0);

    return true;
}
};


START_TEST_DEFINE_PARENT(TestGROUPING_DISK, TestGROUPING_ATTRS)
bool Run() override {
    if (GetIsPrefixed())
        return true;
    PrepareData();
    ReopenIndexers();
    PrepareData();
    ReopenIndexers();
    return CheckGroupping(2);
}
};

START_TEST_DEFINE_PARENT(TestGROUPING_MEMORY, TestGROUPING_ATTRS)
bool Run() override {
    if (GetIsPrefixed())
        return true;
    PrepareData();
    return CheckGroupping();

}
};

START_TEST_DEFINE_PARENT(TestGROUPING_ZERO_NUMDOCS, TestGROUPING_ATTRS)
bool Run() override {
    PrepareData();
    TVector<TDocSearchInfo> results;
    TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > > resultProps;
    THashMultiMap<TString, TString> searchProperties;
    QuerySearch("body&g=1.mid.10.0.-1.0.0.-1.mid.0..0.0&numdoc=1000&dump=eventlog&timeout=9999999&pron=nofastrank", results, &resultProps, &searchProperties, true);
    if (results.size())
        ythrow yexception() << "query must fail miserably";

    return true;
}
bool InitConfig() override {
    (*ConfigDiff)["Searcher.KeepAllDocuments"] = false;
    return true;
}
};

SERVICE_TEST_RTYSERVER_DEFINE(TestGROUP_NO_ATTRS)
void TestAttr(TIndexerType indexer) {
    if (!SendIndexReply)
        ythrow yexception() << "Incorrect test configuration: this test not works withno reply";
    const int CountMessages = 100;
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, CountMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), "body", false);
    IndexMessages(messages, indexer, 1);
    ReopenIndexers();
    CheckSearchResults(messages);
}

bool InitConfig() override {
    (*ConfigDiff)["Indexer.Common.Groups"] = "";
    return true;
}
};

START_TEST_DEFINE_PARENT(TestGROUP_NO_MEMORY, TestGROUP_NO_ATTRS)
bool Run() override {
    TestAttr(REALTIME);
    return true;

}
};

START_TEST_DEFINE_PARENT(TestGROUP_NO_DISK, TestGROUP_NO_ATTRS)
bool Run() override {
    TestAttr(DISK);
    return true;

}
};

START_TEST_DEFINE(TestGROUPPING_WITH_PRUNING)
bool Run() override {
    const int CountMessages = 200;
    TStandartDocumentGenerator* sdg = new TStandartDocumentGenerator(GetIsPrefixed());

    TString kps = "";
    if (GetIsPrefixed())
        kps = "&kps=1";

    TStandartAttributesFiller* safPrune = new TStandartAttributesFiller();
    safPrune->SetDocumentsCount(CountMessages);
    TStandartAttributesFiller* safGroup = new TStandartAttributesFiller();
    safGroup->SetDocumentsCount(CountMessages);
    for (int i = 0; i < CountMessages; i++) {
        safPrune->AddDocAttribute(i, "prune_attr", ToString(CountMessages - i), TStandartAttributesFiller::atGroup);
        safGroup->AddDocAttribute(i, "group_attr", ToString(i < CountMessages - 2 ? 1 : 2), TStandartAttributesFiller::atGroup);
    }
    sdg->RegisterFiller("prune", safPrune);
    sdg->RegisterFiller("group", safGroup);
    TStandartMessagesGenerator smg(sdg, SendIndexReply);

    TVector<NRTYServer::TMessage> messagesForMemory;
    GenerateInput(messagesForMemory, CountMessages, smg);
    IndexMessages(messagesForMemory, REALTIME, 1);

    ReopenIndexers();

    TVector<TDocSearchInfo> results;

    TString searchResult = "";
    if (!ProcessQuery("/?ms=proto&text=body&numdoc=20&g=1.group_attr.2.2&how=prune_attr" + kps, &searchResult)) {
        ERROR_LOG << "Incorrect ProcessQuery result" << Endl;
        return false;
    }
    CheckGroups(searchResult, 2, 2);

    return true;
}
bool InitConfig() override {
    SetPruneAttrSort("prune_attr");
    SetIndexerParams(ALL, 5000);
    SetMergerParams(true, 1);
    return true;
}
};

START_TEST_DEFINE(TestGroupAttrsWithCache)
    bool Run() override {
        const int CountMessages = 1;
        TStandartDocumentGenerator* sdg = new TStandartDocumentGenerator(GetIsPrefixed());

        TString kps = "";
        if (GetIsPrefixed())
            kps = "&kps=1";

        TStandartAttributesFiller* safGroup = new TStandartAttributesFiller();
        safGroup->SetDocumentsCount(CountMessages);
        for (int i = 0; i < CountMessages; i++) {
            safGroup->AddDocAttribute(i, "group_attr", "1", TStandartAttributesFiller::atGroup);
        }
        sdg->RegisterFiller("group", safGroup);
        TStandartMessagesGenerator smg(sdg, SendIndexReply);
        smg.SetMessageType(NRTYServer::TMessage::MODIFY_DOCUMENT);

        TVector<NRTYServer::TMessage> messagesForMemory;
        GenerateInput(messagesForMemory, CountMessages, smg);
        IndexMessages(messagesForMemory, REALTIME, 1);

        TVector<TDocSearchInfo> results;
        QuerySearch("body" + kps + "&g=1.group_attr.10.10", results);
        if (results.size() != 1)
            ythrow yexception() << "Incorrect count documents A";
        ReopenIndexers();
        QuerySearch("body" + kps + "&g=1.group_attr.10.10", results);
        if (results.size() != 1)
            ythrow yexception() << "Incorrect count documents A";
        IndexMessages(messagesForMemory, REALTIME, 1);
        QuerySearch("body" + kps + "&g=1.group_attr.10.10", results);
        if (results.size() != 1)
            ythrow yexception() << "Incorrect count documents A";

        return true;
}

bool InitConfig() override {
    SetSearcherParams(abTRUE, "500s", "CacheSupporter");
    SetIndexerParams(ALL, 5000);
    SetMergerParams(false, 1);
    return true;
}
};

START_TEST_DEFINE(TestGROUPPING_DIFFKEYS_WITH_PRUNING)
bool Run() override {
    if (!GetIsPrefixed())
        return true;
    const int CountMessages = 200;
    TStandartDocumentGenerator* sdg = new TStandartDocumentGenerator(GetIsPrefixed());

    TString kps = "&kps=1,2";

    TStandartAttributesFiller* safPrune = new TStandartAttributesFiller();
    safPrune->SetDocumentsCount(CountMessages);
    TStandartAttributesFiller* safGroup = new TStandartAttributesFiller();
    safGroup->SetDocumentsCount(CountMessages);
    for (int i = 0; i < CountMessages; i++) {
        safPrune->AddDocAttribute(i, "prune_attr", ToString(CountMessages - i), TStandartAttributesFiller::atGroup);
        safGroup->AddDocAttribute(i, "group_attr", ToString(i < CountMessages - 2 ? 1 : 2), TStandartAttributesFiller::atGroup);
    }
    sdg->RegisterFiller("prune", safPrune);
    sdg->RegisterFiller("group", safGroup);
    TStandartMessagesGenerator smg(sdg, SendIndexReply);

    TVector<NRTYServer::TMessage> messagesForMemory;
    GenerateInput(messagesForMemory, CountMessages, smg);

    messagesForMemory.back().MutableDocument()->SetKeyPrefix(2);
    messagesForMemory[messagesForMemory.size() - 2].MutableDocument()->SetKeyPrefix(2);

    IndexMessages(messagesForMemory, REALTIME, 1);

    ReopenIndexers();

    TVector<TDocSearchInfo> results;

    TString searchResult = "";
    if (!ProcessQuery("/?ms=proto&text=body&numdoc=20&g=1.group_attr.2.2&how=prune_attr" + kps, &searchResult)) {
        ERROR_LOG << "Incorrect ProcessQuery result" << Endl;
        return false;
    }
    CheckGroups(searchResult, 2, 2);

    return true;
}
bool InitConfig() override {
    SetPruneAttrSort("prune_attr");
    SetIndexerParams(ALL, 5000);
    SetMergerParams(true, 1);
    return true;
}
};

START_TEST_DEFINE(TestGROUP_SORT_BY_COUNT)

void Test(TIndexerType indexer) {
    ui32 docsCount = 303;
    TStandartDocumentGenerator* sdg = new TStandartDocumentGenerator(GetIsPrefixed());
    TString textForSearch = "disk";
    if (indexer == REALTIME)
        textForSearch = "realtime";
    sdg->SetTextConstant(textForSearch);
    TStandartAttributesFiller* saf = new TStandartAttributesFiller();
    saf->SetDocumentsCount(docsCount);
    ui32 grSize = 100;
    ui32 grCount = 100;
    for (ui32 i = 0; i < docsCount; i++) {
        saf->AddDocAttribute(i, "gr_attr", ToString(grSize), TStandartAttributesFiller::atGroup);
        grCount--;
        if (!grCount) {
            grSize++;
            grCount = grSize;
        }
    }
    sdg->RegisterFiller("gr", saf);
    TStandartMessagesGenerator smg(sdg, SendIndexReply);

    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, docsCount, smg);
    IndexMessages(TVector<NRTYServer::TMessage>(messages.begin(), messages.begin() + 150), indexer, 1);
    ReopenIndexers();
    IndexMessages(TVector<NRTYServer::TMessage>(messages.begin() + 150, messages.end()), indexer, 1);
    ReopenIndexers();
    TVector<TDocSearchInfo> results;
    QuerySearch(textForSearch + "&g=1.gr_attr.10.10.-1.0.0.-1.gr_attr.1.count", results);
    ui32 currentCateg = 0;
    for (int i = 0; i < results.ysize(); ++i) {
        ui32 categ = FromString<ui32>(results[i].GetCategory());
        if (currentCateg < categ)
            currentCateg = FromString<ui32>(results[i].GetCategory());
        else if (currentCateg > categ)
            ythrow yexception() << "Incorrect categ sequence";
    }
    QuerySearch(textForSearch + "&g=1.gr_attr.10.10.-1.0.0.-1.gr_attr.0.count", results);
    currentCateg = Max<ui32>();
    for (int i = 0; i < results.ysize(); ++i) {
        ui32 categ = FromString<ui32>(results[i].GetCategory());
        if (currentCateg > categ)
            currentCateg = FromString<ui32>(results[i].GetCategory());
        else if (currentCateg < categ)
            ythrow yexception() << "Incorrect categ sequence";
    }
}

bool Run() override {
    Test(REALTIME);
    Test(DISK);
    return true;
}

bool InitConfig() override {
    (*ConfigDiff)["Searcher.ReArrangeOptions"] = "CountSort";
    SetIndexerParams(ALL, 5000);
    SetMergerParams(false, 1);
    return true;
}
};

START_TEST_DEFINE_PARENT(TestGroupAttrsFacets, TestGROUPING_ATTRS)
bool Run() override {
    PrepareData();

    TVector<TDocSearchInfo> results;
    TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > > docProperties;
    THashMultiMap<TString, TString> searchProperties;
    QuerySearch("url:\"*\"&gafacets=mid,not_existing&qi=rty_allfacets" + Kps, results, &docProperties, &searchProperties, true);

    THashMultiMap<TString, TString>::const_iterator i = searchProperties.find("facet_mid");
    if (i == searchProperties.end())
        ythrow yexception() << "no facet";

    if (i->second != "1:10;2:10;3:10")
        ythrow yexception() << "wrong facet";

    THashMultiMap<TString, TString>::const_iterator mdi = searchProperties.find("rty_max_gafacet_max_docid");
    THashMultiMap<TString, TString>::const_iterator sdi = searchProperties.find("rty_max_gafacet_segment_docs");
    if (mdi == searchProperties.end() || sdi == searchProperties.end())
        ythrow yexception() << "no max stat for facet";

    ui32 md = FromString<ui32>(mdi->second);
    ui32 sd = FromString<ui32>(sdi->second);
    if (sd - md != 1)
        ythrow yexception() << "smth wrong with max_docid(" << md << ") or segment_docs(" << sd << ")";

    return true;
}
};

START_TEST_DEFINE_PARENT(TestGroupAttrsLiteralFacets, TestGROUPING_ATTRS)
bool Run() override {
    PrepareData();

    TVector<TDocSearchInfo> results;
    TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > > docProperties;
    THashMultiMap<TString, TString> searchProperties;
    TVector<TString> errors;

    QuerySearch("url:\"*\"&gafacets=attr_cc_grp,not_existing&qi=rty_allfacets" + Kps, results, &docProperties, &searchProperties, true);
    THashMultiMap<TString, TString>::const_iterator i = searchProperties.find("facet_attr_cc_grp");
    if (i == searchProperties.end())
        errors.emplace_back("no facet in memory");
    if (i->second != "1_1:10;2_2:10;3_3:10")
        errors.emplace_back("wrong facet in memory: " + i->second + "!= 1_1:10;2_2:10;3_3:10");

    ReopenIndexers();
    QuerySearch("url:\"*\"&gafacets=attr_cc_grp,not_existing&qi=rty_allfacets" + Kps, results, &docProperties, &searchProperties, true);
    i = searchProperties.find("facet_attr_cc_grp");
    if (i == searchProperties.end())
        errors.emplace_back("no facet in disk");
    if (i->second != "1_1:10;2_2:10;3_3:10")
        errors.emplace_back("wrong facet in disk: " + i->second + " != 1_1:10;2_2:10;3_3:10");
    if (!errors.empty()) {
        ythrow yexception() << JoinSeq("\n", errors);
    }
    return true;
}
};

START_TEST_DEFINE_PARENT(TestGroupAttrsLiteralFacetsWithTop, TestGROUPING_ATTRS)
bool Run() override {
    PrepareData();

    TVector<TDocSearchInfo> results;
    TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > > docProperties;
    THashMultiMap<TString, TString> searchProperties;
    TVector<TString> errors;

    QuerySearch("url:\"*\"&gafacets=attr_cc_grp:2,not_existing&qi=rty_allfacets" + Kps, results, &docProperties, &searchProperties, true);
    THashMultiMap<TString, TString>::const_iterator i = searchProperties.find("facet_attr_cc_grp");
    if (i == searchProperties.end())
        errors.emplace_back("no facet in memory");
    if (i->second != "1_1:10;2_2:10")
        errors.emplace_back("wrong facet in memory: " + i->second + "!= 1_1:10;2_2:10");

    ReopenIndexers();
    QuerySearch("url:\"*\"&gafacets=attr_cc_grp:2,not_existing&qi=rty_allfacets" + Kps, results, &docProperties, &searchProperties, true);
    i = searchProperties.find("facet_attr_cc_grp");
    if (i == searchProperties.end())
        errors.emplace_back("no facet in disk");
    if (i->second != "1_1:10;2_2:10")
        errors.emplace_back("wrong facet in disk: " + i->second + " != 1_1:10;2_2:10");
    if (!errors.empty()) {
        ythrow yexception() << JoinSeq("\n", errors);
    }
    return true;
}
};

START_TEST_DEFINE_PARENT(TestGROUP_NO_KEEP_ALL_DOCS, TestGROUPING_ATTRS)
bool InitConfig() override {
    SetIndexerParams(ALL, 15000, 1);
    (*ConfigDiff)["Searcher.KeepAllDocuments"] = false;
    (*ConfigDiff)["Searcher.FiltrationModel"] = "SIMPLE";
    (*ConfigDiff)["Searcher.FactorsInfo"] = GetResourcesDirectory() + "/factors/change_factors1.cfg";
    SetPruneAttrSort("unique_attr");
    return true;
}
bool Run() override {
    ui32 bigGroups = 20;
    ui32 smallGroups = 1;
    ui32 docsInGroup = 600;
    ui32 docCount = docsInGroup * bigGroups + smallGroups * 2;
    TAttrMap map(docCount);
    for (ui32 i = 0; i < smallGroups; ++i) {
        map[i]["attr_aa_grp"] = bigGroups + 10 + i;
        map[docCount - i - 1]["attr_aa_grp"] = bigGroups + 10 + i;
        map[i]["unique_attr"] = 1 + docCount - i;
        map[docCount - i - 1]["unique_attr"] = 2 + i;
    }
    for (ui32 i = smallGroups; i < docCount - smallGroups; ++i) {
        map[i]["attr_aa_grp"] = 1 + (i - smallGroups) / docsInGroup;
        map[i]["unique_attr"] = 1 + docCount - i;
    }
    TVector<NRTYServer::TMessage> messages;
    bool isPreffixed = GetIsPrefixed();
    GenerateInput(messages, docCount, NRTYServer::TMessage::ADD_DOCUMENT, isPreffixed, map);
    for (ui32 i = 0; i < docCount; ++i) {
        NSaas::AddSimpleFactor("stat1", ToString(.0001f * i), *messages[i].MutableDocument()->MutableFactors());
        NSaas::AddSimpleFactor("stat2", ToString(.0001f * i), *messages[i].MutableDocument()->MutableFactors());
        NSaas::AddSimpleFactor("stat3", ToString(.0001f * i), *messages[i].MutableDocument()->MutableFactors());
        messages[i].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());
    }
    TString kps = GetAllKps(messages);
    IndexMessages(messages, DISK, 1);
    ReopenIndexers();
    TString searchResult = "";
    if (200 != ProcessQuery("/?ms=proto&text=body&g=1.attr_aa_grp.2.2&relev=formula=80010000000V3&pron=noprune&pron=egr-1" + kps, &searchResult))
        ythrow yexception() << "error on search request";
    CheckGroups(searchResult, 2, 2);
    return true;
}
};

SERVICE_TEST_RTYSERVER_DEFINE(C2NTest)
void AddC2NAttr(NRTYServer::TMessage& message, const TString& name, const TString& value) {
    NRTYServer::TAttribute& a = *message.MutableDocument()->AddGroupAttributes();
    a.SetName(name);
    a.SetValue(value);
    a.SetType(NRTYServer::TAttribute::LITERAL_ATTRIBUTE);
}

void AddGroupAttr(NRTYServer::TMessage& message, const TString& name, i64 value) {
    NRTYServer::TAttribute& a = *message.MutableDocument()->AddGroupAttributes();
    a.SetName(name);
    a.SetValue(ToString(value));
    a.SetType(NRTYServer::TAttribute::INTEGER_ATTRIBUTE);
}


void VerifyDoc(const NRTYServer::TMessage& doc, const TString& attr) {
    TString rawReport;
    const TString& kps = GetIsPrefixed() ? ("&kps=" + ToString(doc.GetDocument().GetKeyPrefix())) : "";
    ProcessQuery("/?text=url:\"" + doc.GetDocument().GetUrl() + "\"&numdoc=1000&ms=proto&" + kps + "&g=1." + attr + ".100.100", &rawReport);

    NMetaProtocol::TReport report;
    if (!report.ParseFromString(rawReport))
        ythrow yexception() << "cannot parse report";

    const NMetaProtocol::TGrouping& grouping = report.GetGrouping(0);
    if (grouping.GetAttr() != attr)
        ythrow yexception() << "grouping is incorrect";

    const TString& searchedCategory = grouping.GetGroup(0).GetCategoryName();

    TString referenceCategory;
    for (auto&& a : doc.GetDocument().GetGroupAttributes()) {
        if (a.GetName() == attr) {
            referenceCategory = a.GetValue();
            break;
        }
    }

    if (searchedCategory != referenceCategory) {
        ythrow yexception() << "Category mismatch " << searchedCategory << " != " << referenceCategory << " for " << doc.GetDocument().GetUrl() << Endl;
    }
}

void VerifyGrouping(const TString& attr, const TString& category, const ui32 expectedDocCount) {
    TString rawReport;
    const TString& kps = GetIsPrefixed() ? "&kps=1" : "";
    ProcessQuery("/?text=body&numdoc=1000&ms=proto&" + kps + "&g=1." + attr + ".100.100", &rawReport);

    NMetaProtocol::TReport report;
    if (!report.ParseFromString(rawReport))
        ythrow yexception() << "cannot parse report";

    TString textReport;
    ::google::protobuf::TextFormat::PrintToString(report, &textReport);
    DEBUG_LOG << textReport << Endl;

    const NMetaProtocol::TGrouping& grouping = report.GetGrouping(0);
    if (grouping.GetAttr() != attr)
        ythrow yexception() << "grouping is incorrect";

    ui32 docCount = 0;
    for (auto&& g : grouping.GetGroup()) {
        if (g.GetCategoryName() == category)
            docCount = g.DocumentSize();
    }

    if (docCount != expectedDocCount)
        ythrow yexception() << "incorrect number of docs in group " << category << " " << docCount << " != " << expectedDocCount;
}
};

START_TEST_DEFINE_PARENT(TestGROUP_ATTRS_C2N, C2NTest)
bool CheckIvalidC2N(const TString& name, const TString& value) {
    TVector<NRTYServer::TMessage> pack;
    GenerateInput(pack, 1, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed(), TAttrMap(), "body", false);
    AddC2NAttr(pack[0], name, value);
    try {
        IndexMessages(pack, REALTIME, 1);
    }
    catch (...) {
        return true;
    }
    ERROR_LOG << "Was not error with C2N name='" << name << "', value='" << value << "'" << Endl;
    return false;
}

bool Run() override {
    const ui32 baseCount = 5;

    TVector<NRTYServer::TMessage> pack1;
    {
        GenerateInput(pack1, baseCount, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed(), TAttrMap(), "body", false);
        for (ui32 i = 0; i < pack1.size(); ++i) {
            AddC2NAttr(pack1[i], "union", "music");
            pack1[i].MutableDocument()->SetKeyPrefix(GetIsPrefixed() ? 1 : 0);
        }
    }

    TVector<NRTYServer::TMessage> pack2;
    {
        GenerateInput(pack2, 2 * baseCount, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed(), TAttrMap(), "body", false);
        for (ui32 i = 0; i < pack2.size(); ++i) {
            AddC2NAttr(pack2[i], "union", (i < baseCount) ? "art" : "music");
            pack2[i].MutableDocument()->SetKeyPrefix(GetIsPrefixed() ? 1 : 0);
        }
    }

    IndexMessages(pack1, REALTIME, 1);
    VerifyGrouping("union", "music", baseCount);

    ReopenIndexers();
    VerifyGrouping("union", "music", baseCount);

    IndexMessages(pack2, REALTIME, 1);
    VerifyGrouping("union", "music", 2 * baseCount);
    VerifyGrouping("union", "art", baseCount);

    ReopenIndexers();
    VerifyGrouping("union", "music", 2 * baseCount);
    VerifyGrouping("union", "art", baseCount);

    Controller->ProcessCommand("create_merger_tasks");
    Controller->ProcessCommand("do_all_merger_tasks");

    VerifyGrouping("union", "music", 2 * baseCount);
    VerifyGrouping("union", "art", baseCount);

    TVector<NRTYServer::TMessage> pack3;
    {
        GenerateInput(pack3, 4 * baseCount, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed(), TAttrMap(), "body", false);
        for (ui32 i = 0; i < pack3.size(); ++i) {
            AddC2NAttr(pack3[i], "union", "ctrl_" + ToString(i));
            pack3[i].MutableDocument()->SetKeyPrefix(GetIsPrefixed() ? 1 : 0);
        }
    }

    IndexMessages(pack3, REALTIME, 1);
    ReopenIndexers();
    Controller->ProcessCommand("create_merger_tasks");
    Controller->ProcessCommand("do_all_merger_tasks");
    for (auto&& m : pack3) {
        VerifyDoc(m, "union");
    }
    if (!CheckIvalidC2N("", "value"))
        return false;
    if (!CheckIvalidC2N("union", ""))
        return false;
    if (!CheckIvalidC2N("union", "qw\ner"))
        return false;

    return true;
}

bool InitConfig() override {
    SetMergerParams(true, 1);
    SetIndexerParams(DISK, 100, 1);
    SetIndexerParams(REALTIME, 100);
    return true;
}

};

START_TEST_DEFINE_PARENT(TestGROUP_ATTRS_ASC_SORT, C2NTest)
bool Run() override {
    const ui32 baseCount = 5;
    const TString kps = GetIsPrefixed() ? "&kps=1" : "";

    TVector<NRTYServer::TMessage> pack1;
    {
        GenerateInput(pack1, baseCount, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed(), TAttrMap(), "body", false);
        for (ui32 i = 0; i < pack1.size(); ++i) {
            AddC2NAttr(pack1[i], "union", "music" + ToString(i));
            AddGroupAttr(pack1[i], "sorter", i);
            pack1[i].MutableDocument()->SetKeyPrefix(GetIsPrefixed() ? 1 : 0);
        }
    }

    IndexMessages(pack1, REALTIME, 1);

    TVector<TDocSearchInfo> results;
    QuerySearch("body&g=1.union.10.10&how=sorter&asc=1&pron=nofastrank" + kps, results, nullptr, nullptr, true);
    if (results[0].GetUrl() != pack1[0].GetDocument().GetUrl()) {
        ythrow yexception() << "incorrect first document";
    }

    return true;
}

bool InitConfig() override {
    (*ConfigDiff)["Searcher.KeepAllDocuments"] = false;
    (*ConfigDiff)["Searcher.ExternalSearch"] = "rty_relevance";
    return true;
}

};

START_TEST_DEFINE(TestGROUP_ATTRS_PARTIAL)
bool Run() override {
    if (GetIsPrefixed())
        return true;
    TAttrMap::value_type map1;
    map1["fudge"] = 1;
    TVector<NRTYServer::TMessage> messages1, messages2;
    GenerateInput(messages1, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(1, map1));
    GenerateInput(messages2, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    IndexMessages(messages1, REALTIME, 1);
    IndexMessages(messages2, REALTIME, 1);
    ReopenIndexers();
    CheckSearchResults(messages1);
    CheckSearchResults(messages2);

    TVector<TDocSearchInfo> results;
    QuerySearch("\"body\"&how=fudge&haha=da&pron=earlyurls&pron=nofastrank", results, nullptr, nullptr, true);
    if (results.size() != 2)
        ythrow yexception() << "incorrect over";
    if (results[0].GetUrl() != messages1[0].GetDocument().GetUrl())
        ythrow yexception() << "incorrect url";
    return true;
}
bool InitConfig() override {
    (*ConfigDiff)["Searcher.KeepAllDocuments"] = false;
    return true;
}
};
