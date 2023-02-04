#include <saas/api/clientapi.h>
#include <saas/rtyserver/components/zones_makeup/read_write_makeup_manager.h>
#include <saas/rtyserver_test/testerlib/rtyserver_test.h>

using namespace NRTY;

SERVICE_TEST_RTYSERVER_DEFINE(TestCustomZoneCommon)
void Check(const char* comment, i64 prefix) {
    TVector<TDocSearchInfo> results;
    const TString kps = "&kps=" + ToString(prefix);
    QuerySearch("абв" + kps, results);
    if (results.ysize() != 1)
        ythrow yexception() << comment << ": Query 'абв" << kps << "'results count incorrect: " << results.ysize() << " != " << 1;
    QuerySearch("some text" + kps, results);
    if (results.ysize() != 1)
        ythrow yexception() << comment << ": Query 'some text" << kps << "'results count incorrect: " << results.ysize() << " != " << 1;
    QuerySearch("hhh:((some text))" + kps, results);
    if (results.ysize() != 1)
        ythrow yexception() << comment << ": Query 'hhh:((some text))" << kps << "'results count incorrect: " << results.ysize() << " != " << 1;
    QuerySearch("hhh:((абв))" + kps, results);
    if (results.ysize() != 0)
        ythrow yexception() << comment << ": Query 'hhh:((абв))" << kps << "'results count incorrect: " << results.ysize() << " != " << 0;
    QuerySearch("hhh:((grr_attr:\"attr_value\"))" + kps, results);
    if (results.ysize() != 1)
        ythrow yexception() << comment << ": Query 'hhh:((grr_attr:\"attr_value\"))" << kps << "'results count incorrect: " << results.ysize() << " != " << 1;
    QuerySearch("hhh:((int_zone_attr:42))" + kps, results);
    if (results.ysize() != 1)
        ythrow yexception() << comment << ": Query 'hhh:((int_zone_attr:42))" << kps << "'results count incorrect: " << results.ysize() << " != " << 1;
}

void Test(bool rigidStop) {
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), "");
    messages[0].MutableDocument()->ClearBody();
    NRTYServer::TZone& root = *messages[0].MutableDocument()->MutableRootZone();
    root.SetText("<абв");
    NRTYServer::TZone& child  = *root.AddChildren();
    child.SetName("hhh");
    child.SetText("some text");
    NRTYServer::TAttribute& int_attr = *child.AddSearchAttributes();
    int_attr.SetName("int_zone_attr");
    int_attr.SetValue("42");
    int_attr.SetType(NRTYServer::TAttribute::INTEGER_ATTRIBUTE);
    NRTYServer::TAttribute& lit_attr = *child.AddSearchAttributes();
    lit_attr.SetName("grr_attr");
    lit_attr.SetValue("attr_value");
    lit_attr.SetType(NRTYServer::TAttribute::LITERAL_ATTRIBUTE);
    IndexMessages(messages, REALTIME, 1);
    Check("memory", messages[0].GetDocument().GetKeyPrefix());
    if (rigidStop) {
        Controller->RestartServer(true);
        Controller->WaitIsRepairing();
    } else
        ReopenIndexers();
    Check("disk", messages[0].GetDocument().GetKeyPrefix());
}
bool InitConfig() override {
    SetIndexerParams(ALL, 10, 1);
    SetEnabledRepair();
    return true;
}
};

START_TEST_DEFINE_PARENT(TestCustomZone, TestCustomZoneCommon)
    bool Run() override {
        Test(false);
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestCustomZoneRepair, TestCustomZoneCommon)
    bool Run() override {
        Test(true);
        return true;
    }
};

START_TEST_DEFINE(TestMultipleZoneValues)
bool Run() override {
    TAction action;
    action.SetId(1);
    action.SetPrefix(GetIsPrefixed() ? 1 : 0);
    TDocument& document = action.AddDocument();
    document.SetUrl("url");
    document.SetMimeType("text/html");
    document.AddZone("hhh").SetText("dog");
    document.AddZone("hhh").SetText("cat");
    document.AddZone("hhh").SetText("ass");
    TVector<NRTYServer::TMessage> message;
    message.push_back(action.ToProtobuf());
    IndexMessages(message, REALTIME, 1);
    TVector<TDocSearchInfo> results;
    const TString kps = GetIsPrefixed() ? "&kps=1" : "";
    QuerySearch("hhh:((dog))" + kps, results);
    if (results.size() != 1)
        ythrow yexception() << "cannot find dog";
    QuerySearch("hhh:((cat))" + kps, results);
    if (results.size() != 1)
        ythrow yexception() << "cannot find cat";
    QuerySearch("hhh:((ass))" + kps, results);
    if (results.size() != 1)
        ythrow yexception() << "cannot find ass";
    return true;
}
};

SERVICE_TEST_RTYSERVER_DEFINE(TestUnicodeSymbolsZonesClashCommon)
// Uses unicode copyright and other symbols that translates into (<symbol>) and clashes with opening zone key
const TString DOCUMENT ="{\"docs\":[{\"psbio\":{\"type\":\"#z\",\"value\":\"ځآجے ݦݩ خدأڔؤ ݕےݜٺڔ ٺۅے🎶 آہڹڱأے 🎶🌵ݜآہےݧ🌵ݦےݕےݩݦ ٺآ ښخڹږاݧے هآی ټۊ ݕعڐإݫ ݩمآݫ ݒآی ݦنݕڔ\\n👑💙łїғ⒠ ℳᴇ $нÅнїИ ηα⒥αғї👑💙\\n#ڜږررڒږيم_بې_أمٵڹ...😍💝💪👌👊\"},\"Title\":{\"type\":\"#z\",\"value\":\"Alireza Sharrr Shahin Najafi\"},\"options\":{\"realtime\":false,\"charset\":\"utf-8\",\"mime_type\":\"text/xml\"},\"url\":\"3026169450@18\"}],\"action\":\"modify\",\"prefix\":0}";
bool RunBasic() {
    NJson::TJsonValue result;
    if (!NJson::ReadJsonFastTree(DOCUMENT, &result, true)) {
        ythrow yexception() << "Failed to parse json";
    }
    TAction action;
    action.ParseFromJson(result);
    action.SetId(1);
    action.SetPrefix(GetIsPrefixed() ? 1 : 0);
    TVector<NRTYServer::TMessage> messages;
    messages.push_back(action.ToProtobuf());
    IndexMessages(messages, DISK, 1);
    ReopenIndexers();
    TVector<TDocSearchInfo> results;
    const TString kps = GetIsPrefixed() ? "&kps=1" : "";
    QuerySearch("url:((3026169450@18))" + kps, results);
    CHECK_TEST_EQ(results.size(), 1);
    return true;
}

};

START_TEST_DEFINE_PARENT(TestUnicodeSymbolsZonesClash, TestUnicodeSymbolsZonesClashCommon)
bool Run() override {
    return RunBasic();
}
};

START_TEST_DEFINE_PARENT(TestUnicodeSymbolsZonesRecoveryClash, TestUnicodeSymbolsZonesClashCommon)
bool Run() override {
    CHECK_TEST_TRUE(RunBasic());
    const TSet<TString> finalIndexes = Controller->GetFinalIndexes();
    for (TSet<TString>::const_iterator i = finalIndexes.begin(); i != finalIndexes.end(); ++i) {
        const TString hdrFileName = *i + TRTYMakeupManager::HdrFileName;
        const TString docsFileName = *i + TRTYMakeupManager::DocsFileName;
        if (!NFs::Exists(hdrFileName) || !NFs::Exists(docsFileName) ) {
            ythrow yexception() << "missing makeup in " << *i;
        }
        NFs::Remove(hdrFileName);
        NFs::Remove(docsFileName);
    }
    Controller->RestartServer();
    const TString kps = GetIsPrefixed() ? "&kps=1" : "";
    TVector<TDocSearchInfo> results;
    QuerySearch("url:((3026169450@18))" + kps, results);
    CHECK_TEST_EQ(results.size(), 1);
    return true;
}
};
