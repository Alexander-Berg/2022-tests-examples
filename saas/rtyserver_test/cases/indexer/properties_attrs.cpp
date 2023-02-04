#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <library/cpp/string_utils/quote/quote.h>
#include <util/charset/wide.h>

START_TEST_DEFINE(TestPROPERTIES_FILTER_ONLY)
bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    size_t messagesCount = 10;
    const bool isPrefixed = GetIsPrefixed();
    GenerateInput(messages, messagesCount, NRTYServer::TMessage::ADD_DOCUMENT, isPrefixed, TAttrMap(), TString("text for test ") + WideToUTF8(u"ААА БББ"));
    size_t attrCount = messagesCount;
    for (size_t i = 0; i < messagesCount; ++i) {
        NRTYServer::TMessage::TDocument::TProperty* prop = messages[i].MutableDocument()->AddDocumentProperties();
        prop->set_name(ToString(i).data());
        prop->set_value(ToString(i).data());
        if (isPrefixed)
            messages[i].MutableDocument()->SetKeyPrefix(1);
    }

    IndexMessages(messages, REALTIME, 1);

    TString textSearch = WideToUTF8(u"\"text for test ААА ББ*\"");
    Quote(textSearch);
    if (isPrefixed)
        textSearch += "&kps=1";

    TVector<TDocSearchInfo> results;
    for (size_t i = 0; i < attrCount; ++i) {
        QuerySearch(textSearch + TString("&only=") + ToString(i), results);
        if (results.size() != messagesCount)
            ythrow yexception() << "BEFORE: Incorrect count of docs with mid:" << i;
    }
    QuerySearch(textSearch + TString("&only=") + ToString(attrCount + 1), results);
    if (results.size() != messagesCount)
        ythrow yexception() << "BEFORE: Incorrect count of docs with mid:" << (attrCount + 1);

    ReopenIndexers();

    for (size_t i = 0; i < attrCount; ++i) {
        QuerySearch(textSearch + TString("&only=") + ToString(i), results);
        if (results.size() != messagesCount)
            ythrow yexception() << "BEFORE: Incorrect count of docs with mid:" << i;
    }
    QuerySearch(textSearch + TString("&only=") + ToString(attrCount + 1), results);
    if (results.size() != messagesCount)
        ythrow yexception() << "BEFORE: Incorrect count of docs with mid:" << (attrCount + 1);

    DeleteQueryResult(textSearch + TString("&only=") + ToString(attrCount + 1), REALTIME);

    QuerySearch(textSearch + TString("&only=") + ToString(attrCount + 1), results);
    if (results.size() != 0)
        ythrow yexception() << "BEFORE: Incorrect count of docs with mid:" << (attrCount + 1) << " after deleting";

    return true;
}
};

START_TEST_DEFINE(TestPROPERTIES_MULTIVALUE)
void Check(const TVector<NRTYServer::TMessage>& messages, const char* comment, size_t attrCount) {
    TString textSearch = "body&kps=" + ToString(messages[0].GetDocument().GetKeyPrefix());
    TVector<TDocSearchInfo> results;
    TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > > resultProps;
    QuerySearch(textSearch, results, &resultProps);
    if (results.size() != 1)
        ythrow yexception() << comment << ": hren'";
    const THashMultiMap<TString, TString>& props = *resultProps[0];
    std::pair<THashMultiMap<TString, TString>::const_iterator, THashMultiMap<TString, TString>::const_iterator> myPropRange = props.equal_range("my_property");
    TSet<i64> propValues;
    for (;myPropRange.first != myPropRange.second; ++myPropRange.first)
        propValues.insert(FromString<i64>(myPropRange.first->second));
    if (propValues.size() != attrCount)
        ythrow yexception() << comment << " ne katit";
}

bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    const bool isPrefixed = GetIsPrefixed();
    GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, isPrefixed);
    const size_t attrCount = 5;
    for (size_t i = 0; i < attrCount; ++i) {
        NRTYServer::TMessage::TDocument::TProperty* prop = messages[0].MutableDocument()->AddDocumentProperties();
        prop->set_name("my_property");
        prop->set_value(ToString(i));
    }
    IndexMessages(messages, REALTIME, 1);
    Check(messages, "Memory", attrCount);
    ReopenIndexers();
    Check(messages, "Disk", attrCount);
    return true;
}
};

START_TEST_DEFINE_PARENT(TestPROPERTIES_MULTIVALUE_NO_TEXT, TTestPROPERTIES_MULTIVALUECaseClass)
bool InitConfig() override {
    if (!TTestPROPERTIES_MULTIVALUECaseClass::InitConfig())
        return false;
    (*ConfigDiff)["Indexer.Common.StoreTextToArchive"] = 0;
    return true;
}
};

START_TEST_DEFINE(TestTabInProperty)
bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    bool prefixed = GetIsPrefixed();
    GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, prefixed);
    AddProperty("bestprop", "first part \t second part", messages[0]);
    IndexMessages(messages, REALTIME, 1);
    Check(messages[0], "bestprop", "Realtime");
    ReopenIndexers();
    Check(messages[0], "bestprop", "Disk");
    return true;
}

void AddProperty(const TString& name, const TString& value, NRTYServer::TMessage& doc) {
    NRTYServer::TMessage::TDocument::TProperty& prop = * doc.MutableDocument()->AddDocumentProperties();
    prop.SetName(name);
    prop.SetValue(value);
}

void Check(const NRTYServer::TMessage& doc, const TString& propertieName, const TString& stage) {
    TString textSearch = "body&kps=" + ToString(doc.GetDocument().GetKeyPrefix());
    TVector<TDocSearchInfo> results;
    TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > > resultProps;
    QuerySearch(textSearch, results, &resultProps, nullptr, true);
    if (results.size() != 1)
        ythrow yexception() << "cannot find document in " << stage;
    const THashMultiMap<TString, TString>& props = *resultProps[0];
    std::pair<THashMultiMap<TString, TString>::const_iterator, THashMultiMap<TString, TString>::const_iterator> myPropRange = props.equal_range(propertieName);
    TSet<TString> propValues;
    for (;myPropRange.first != myPropRange.second; ++myPropRange.first)
        propValues.insert(myPropRange.first->second);
    if (propValues.size() != 1)
        ythrow yexception() << propValues.size() << " property values in " << stage;
}
};

START_TEST_DEFINE(TestEmptyProperty)
bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    bool prefixed = GetIsPrefixed();
    GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, prefixed);
    AddProperty("bestprop", "", messages[0]);
    IndexMessages(messages, REALTIME, 1);
    CheckSearchResults(messages);
    ReopenIndexers();
    CheckSearchResults(messages);
    return true;
}

void AddProperty(const TString& name, const TString& value, NRTYServer::TMessage& doc) {
    NRTYServer::TMessage::TDocument::TProperty& prop = *doc.MutableDocument()->AddDocumentProperties();
    prop.SetName(name);
    prop.SetValue(value);
}

};

START_TEST_DEFINE(TestLongProperties)
bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    size_t messagesCount = 1;
    const bool isPrefixed = GetIsPrefixed();
    GenerateInput(messages, messagesCount, NRTYServer::TMessage::ADD_DOCUMENT, isPrefixed);
    size_t attrCount = messagesCount;
    for (ui8 i = 0; i < messagesCount; ++i) {
        NRTYServer::TMessage::TDocument::TProperty* prop = messages[i].MutableDocument()->AddDocumentProperties();
        prop->SetName("a");
        prop->SetValue(TString(1 << 12, 'a' + i));
        prop = messages[i].MutableDocument()->AddDocumentProperties();
        prop->SetName("b");
        prop->SetValue(TString(1 << 12, 'b' + i));
        prop = messages[i].MutableDocument()->AddDocumentProperties();
        prop->SetName("c");
        prop->SetValue(TString(1 << 12, 'c' + i));
        if (isPrefixed)
            messages[i].MutableDocument()->SetKeyPrefix(1);
    }

    IndexMessages(messages, REALTIME, 1);

    TString textSearch = "body";
    Quote(textSearch);
    if (isPrefixed)
        textSearch += "&kps=1";

    TVector<TDocSearchInfo> results;
    QuerySearch(textSearch + TString("&only=") + ToString(attrCount + 1), results);
    if (results.size() != messagesCount)
        ythrow yexception() << "BEFORE: Incorrect count of docs with mid:" << (attrCount + 1);
    return true;
}
};

START_TEST_DEFINE(TestJsonPropertiesXMLEncoding)
bool Run() override {
    const TString jsonValue = "{\"type\":\"musicplayer\",\"format\":\"json\",\"flat\":\"1\",\"subtype\":\"track\",\"remove\":\"lyrics\",\"slot\":\"full\"}";
    TVector<NRTYServer::TMessage> messages;
    size_t messagesCount = 1;
    const bool isPrefixed = GetIsPrefixed();
    GenerateInput(messages, messagesCount, NRTYServer::TMessage::ADD_DOCUMENT, isPrefixed);
    for (ui8 i = 0; i < messagesCount; ++i) {
        NRTYServer::TMessage::TDocument::TProperty* prop = messages[i].MutableDocument()->AddDocumentProperties();
        prop->SetName("a");
        prop->SetValue(jsonValue);
        if (isPrefixed)
            messages[i].MutableDocument()->SetKeyPrefix(1);
    }

    IndexMessages(messages, REALTIME, 1);
    TString searchResult;
    ProcessQuery((TString)"/?xml=da&text=body" + (isPrefixed ? "&kps=1" : ""), &searchResult);

    INFO_LOG << searchResult << Endl;

    const TString specStr = "<a>{&quot;type&quot;:&quot;musicplayer&quot;,&quot;format&quot;:&quot;json";

    if (Cluster->GetNodesNames(TNODE_SEARCHPROXY).size()) {
        CHECK_TEST_EQ(searchResult.find(specStr), TString::npos);
    } else {
        CHECK_TEST_NEQ(searchResult.find(specStr), TString::npos);
    }
    return true;
}

bool InitConfig() override {
    SetIndexerParams(DISK, 10, 1);
    (*ConfigDiff)["Indexer.Memory.Enabled"] = "true";
    (*ConfigDiff)["Searcher.SnippetsDeniedZones"] = "";
    (*SPConfigDiff)["Service.MetaSearch.UserParams.InplaceXmlProperties"] = "a";
    return true;
}

};
