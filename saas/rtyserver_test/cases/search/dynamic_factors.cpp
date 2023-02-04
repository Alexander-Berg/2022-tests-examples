#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/rtyserver_test/util/doc_info.h>
#include <saas/rtyserver_test/util/factors_parsers.h>
#include <saas/rtyserver/components/zones_makeup/read_write_makeup_manager.h>
#include <saas/rtyserver/config/common_indexers_config.h>
#include <saas/api/factors_erf.h>
#include <search/begemot/rules/query_factors/proto/query_factors.pb.h> // to generate bgfactors=
#include <search/begemot/rules/webfresh/query_rt_factors/proto/query_rt_factors.pb.h> // to generate bgrtfactors=
#include <kernel/keyinv/indexfile/searchfile.h>

#include <util/string/vector.h>
#include <library/cpp/string_utils/quote/quote.h>
#include <util/system/fs.h>
#include <util/generic/ymath.h>

SERVICE_TEST_RTYSERVER_DEFINE(TestDynamicFactorsParent)
    void CheckMakeup() {
        const TSet<TString> finalIndexes = Controller->GetFinalIndexes();
        TVector<THolder<TReadWriteRTYMakeupManager>> mms;
        TRTYServerConfig config(MakeAtomicShared<TDaemonConfig>(TDaemonConfig::DefaultEmptyConfig.data(), false));
        config.IsPrefixedIndex = GetIsPrefixed();
        config.GetCommonIndexers().UseSlowUpdate = Controller->GetConfigValue<bool>("Indexer.Common.UseSlowUpdate");
        for (TSet<TString>::const_iterator i = finalIndexes.begin(); i != finalIndexes.end(); ++i) {
            mms.push_back(MakeHolder<TReadWriteRTYMakeupManager>(TPathName{*i}, config, false, true, false));
            mms.back()->Open();
            const TString hdrFileName = *i + TRTYMakeupManager::HdrFileName;
            const TString docsFileName = *i + TRTYMakeupManager::DocsFileName;
            if (!NFs::Exists(hdrFileName) || !NFs::Exists(docsFileName) ) {
                ythrow yexception() << "missing makeup in " << *i;
            }
            NFs::Remove(hdrFileName);
            NFs::Remove(docsFileName);
        }
        Controller->RestartServer();
        int counter = 0;
        for (TSet<TString>::const_iterator i = finalIndexes.begin(); i != finalIndexes.end(); ++i) {
            TReadWriteRTYMakeupManager manager(TPathName{*i}, config, false, true, false);
            manager.Open();
            if (*mms[counter] != manager)
                ythrow yexception() << "invalid makeup in " << *i;
            ++counter;
            manager.Close();
        }
        for (auto&& i : mms) {
            i->Close();
        }
    }

    TDocSearchInfo GetDocSearchInfo(const TString& url, const ui64 kps) {
        TVector<TDocSearchInfo> results;
        const TString kpsString = kps ? "&kps=" + ToString(kps) : "";
        QuerySearch("url:\"" + url + "\"" + kpsString, results);
        if (results.size() != 1)
            ythrow yexception() << "url " << url << " is not unique";
        return results[0];
    }

    void PrintMakeup(TDocInfo& docInfo) {
        for (TDocInfo::TZonesLengths::const_iterator i = docInfo.GetMakeupDocInfo().ZonesLengths.begin();
            i != docInfo.GetMakeupDocInfo().ZonesLengths.end(); ++i)
            DEBUG_LOG << i->first << " length: " << i->second << Endl;
        for (TDocInfo::TZonesInfo::const_iterator i = docInfo.GetMakeupDocInfo().ZonesInfo.begin();
            i != docInfo.GetMakeupDocInfo().ZonesInfo.end(); ++i)
            DEBUG_LOG << i->first << " : " << i->second << Endl;
    }
};

START_TEST_DEFINE_PARENT(TestDynamicFactors, TestDynamicFactorsParent)
    THashMap<TString, double> Factors;

    double GetFactor(const TString& factor) {
        THashMap<TString, double>::const_iterator iFactor = Factors.find(factor);
        if (iFactor == Factors.end())
            ythrow yexception() << "there is no " << factor << " in result";
        return iFactor->second;
    }
    double CheckFactor(const TString& factor, float value) {
        const double f = GetFactor(factor);
        if (fabs(f - value) > 0.01)
            ythrow yexception() << "Invalid " << factor << " value: " << f << " != " << value;
        return f;
    }
    double CheckFactorNotNull(const TString& factor) {
        const double f = GetFactor(factor);
        if (fabs(f) < 0.001)
            ythrow yexception() << "null " << factor << " value: " << f;
        return f;
    }
    bool Run() override {
        return RunImpl("a", "a b c softness:100", true, GetIsPrefixed() ? 1 : 0);
    }
    void GetFactorsImpl(const TString& query, const TString& kps, const TString& opts = {}) {
        TVector<TDocSearchInfo> results;
        TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > > resultProps;
        TString textSearch = query;
        Quote(textSearch);
        QuerySearch(textSearch + "&dbgrlv=da&fsgta=_JsonFactors&" + kps + opts, results, &resultProps);
        if (results.size() != 1)
            ythrow yexception() << "softness does not work";
        Factors = TRTYFactorsParser::GetJsonFactorsValues(resultProps)[0];
        for (auto f : Factors) {
            DEBUG_LOG << f.first << "=" << f.second << Endl;
        }
    }
    bool RunImpl(const TString& text, const TString& query, bool checkQuorum, ui64 kps) {
        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), text);
        for (auto&& m : messages) {
            m.MutableDocument()->SetKeyPrefix(kps);
        }
        IndexMessages(messages, REALTIME, 1);
        GetFactorsImpl(query, GetAllKps(messages));
        if (checkQuorum) {
            CheckFactor("TRDocQuorum", 0.33);
        }
        CheckFactorNotNull("LongQuery");
        CheckFactorNotNull("TLen");
        CheckFactorNotNull("MatrixNet");
        return true;
    }

    bool InitConfig() override {
        (*ConfigDiff)["Searcher.FiltrationModel"] = "SIMPLE";
        (*ConfigDiff)["Searcher.FactorsInfo"] = GetResourcesDirectory() + "/text_relev/text_factors.cfg";
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestBegemotQueryFactors, TTestDynamicFactorsCaseClass)
    bool InitConfig() override {
        (*ConfigDiff)["Searcher.FiltrationModel"] = "SIMPLE";
        (*ConfigDiff)["Searcher.FactorsInfo"] = GetResourcesDirectory() + "/text_relev/begemot_slice.cfg";
        return true;
    }
    bool Run() override {
        return RunImpl("a", "a b c softness:100", GetIsPrefixed() ? 1 : 0);
    }
    bool RunImpl(const TString& text, const TString& query, ui64 kps) {
        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), text);
        for (auto&& m : messages) {
            m.MutableDocument()->SetKeyPrefix(kps);
        }
        IndexMessages(messages, REALTIME, 1);
        GetFactorsImpl(query, GetAllKps(messages), MakeMockBegemotData());
        // check InvWordCount: a web_production factor that is filled in TRTYRelevance::FillPerQueryParams. It is not provided by Begemot, despite being present in begemot_query_factors slice.
        CheckFactor("InvWordCount", 1.0f / 3);
        // check a factor from begemot_query_factors, that is obtained from relev=bgfactors and then copied to web_production
        CheckFactor("IsMobileRequest", 1.0f);
        // check a factor from begemot_query_rt_l2_factors slice, that is filled in (search/base)TDocumentAttributeCalcer::CalcFactorsFast, is obtained from relev=bgrtfactors, and is NOT available in web_production.
        CheckFactor("FreshPairsContrastDesktopL2", 0.1328f);
        // check that NRTYFactors::TConfig::NeedDocsLens() works with multiple slices
        CheckFactorNotNull("TLen");
        return true;
    }
    static TString MakeMockBegemotData() {
        TStringStream ss;
        {
            NBg::NProto::TQueryFactors bgfactors;
            bgfactors.SetIsMobileRequest(true);
            ss << "&relev=bgfactors="  << Base64Encode(bgfactors.SerializeAsString());
        }
        {
            NBg::NProto::TQueryRtFactors bgrtfactors;
            bgrtfactors.SetFreshPairsContrastDesktop(0.1328f);
            ss << "&relev=bgrtfactors="  << Base64Encode(bgrtfactors.SerializeAsString());
        }
        return ss.Str();
    }
};

START_TEST_DEFINE_PARENT(TestAdvancedDynamicFactors, TTestDynamicFactorsCaseClass)
bool Run() override {
    const TString& text1 =
        "The ad's headline and copy in Russian should be grammatically "
        "correct and free of spelling errors. If the advertised website is in "
        "any language other than Russian, the ad should explicitly inform about "
        "this. The key words, to which the ad will be displayed, or catalogue "
        "categories, where the ad will be placed, should match the ad's content. "
        "Destination URL in the ad should match its content and should function "
        "properly. You cannot use IP-address for domain name. The contact "
        "information should be valid and correct, and the telephone information "
        "should not differ from the information in the ad. If applicable, the "
        "headline, the copy, or the destination URL cannot not contain: "
        "spaces between letters in w o r d s;";
    const TString& query1 = "russian language";

    const TString& text2 = "Yandex ana sayfasında nasıl listesi oluşturulur";
    const TString& query2 = "listesi ana";

    RunImpl(text1, query1, false, GetIsPrefixed() ? 1 : 0);
    const double bclm1 = CheckFactorNotNull("Bclm");
    const double ymw1 = CheckFactorNotNull("YmwFull2");
    RunImpl(text2, query2, false, GetIsPrefixed() ? 1 : 0);
    const double bclm2 = CheckFactorNotNull("Bclm");
    const double ymw2 = CheckFactorNotNull("YmwFull2");

    Controller->ProcessCommand("create_merger_tasks");
    Controller->ProcessCommand("do_all_merger_tasks");

    GetFactorsImpl(query1, GetIsPrefixed() ? "kps=1" : "");
    CheckFactor("Bclm", bclm1);
    CheckFactor("YmwFull2", ymw1);

    GetFactorsImpl(query2, GetIsPrefixed() ? "kps=1" : "");
    CheckFactor("Bclm", bclm2);
    CheckFactor("YmwFull2", ymw2);

    return true;
}

bool InitConfig() override {
    (*ConfigDiff)["Searcher.FiltrationModel"] = "SIMPLE";
    (*ConfigDiff)["Searcher.FactorsInfo"] = GetResourcesDirectory() + "/text_relev/text_factors.cfg";
    SetMergerParams(true, 1, -1, mcpNONE);
    return true;
}
};

START_TEST_DEFINE_PARENT(TestFreshFactors, TestDynamicFactorsParent)
bool Run() override {
    TVector<NRTYServer::TMessage> messages1;
    GenerateInput(messages1, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), "body");
    DEBUG_LOG << "BaseValue1:" << Now().Hours() << Endl;
    NSaas::AddSimpleFactor("start", ToString(Now().Hours()), *messages1[0].MutableDocument()->MutableFactors());
    IndexMessages(messages1, REALTIME, 1);

    Sleep(TDuration::Seconds(10));
    TVector<NRTYServer::TMessage> messages2;
    GenerateInput(messages2, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), "body");
    messages2[0].MutableDocument()->SetKeyPrefix(messages1[0].GetDocument().GetKeyPrefix());
    NSaas::AddSimpleFactor("start", ToString(Now().Hours()), *messages2[0].MutableDocument()->MutableFactors());
    DEBUG_LOG << "BaseValue2:" << Now().Hours() << Endl;
    IndexMessages(messages2, REALTIME, 1);

    TVector<NRTYServer::TMessage> messages3;
    GenerateInput(messages3, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), "tuple");
    messages3[0].MutableDocument()->SetKeyPrefix(messages1[0].GetDocument().GetKeyPrefix());
    NSaas::AddSimpleFactor("start", ToString(Now().Hours()), *messages3[0].MutableDocument()->MutableFactors());
    auto ts = (Now() - TDuration::Days(7)).Seconds();
    messages3[0].MutableDocument()->SetModificationTimestamp(ts);
    IndexMessages(messages3, REALTIME, 1);

    TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > > resultProps;
    TVector<TDocSearchInfo> results;
    QuerySearch("body&dbgrlv=da&fsgta=_JsonFactors&" + GetAllKps(messages1), results, &resultProps);
    if (results.size() != 2)
        TEST_FAILED("Incorrect results count: " + ToString(results.size()));
    THashMap<TString, double> factors0 = TRTYFactorsParser::GetJsonFactorsValues(resultProps)[0];
    THashMap<TString, double>::const_iterator iFactor0 = factors0.find("LiveTime");
    THashMap<TString, double> factors1 = TRTYFactorsParser::GetJsonFactorsValues(resultProps)[1];
    THashMap<TString, double>::const_iterator iFactor1 = factors1.find("LiveTime");
    if (iFactor0 == factors0.end())
        TEST_FAILED("there is no LiveTime factor in result0");
    if (iFactor1 == factors1.end())
        TEST_FAILED("there is no LiveTime factor in result1");

    if (fabs(iFactor0->second - iFactor1->second) > 11)
        ythrow yexception() << "Invalid factor values " << fabs(iFactor0->second - iFactor1->second);

    QuerySearch("body&pron=sortbyInvLiveTime&dbgrlv=da&fsgta=_JsonFactors&relev=formula=default0&" + GetAllKps(messages1), results);
    if (results.size() != 2)
        TEST_FAILED("Incorrect results count: " + ToString(results.size()));
    if (results[0].GetUrl() != messages1[0].GetDocument().GetUrl() ||
        results[1].GetUrl() != messages2[0].GetDocument().GetUrl())
        TEST_FAILED("Incorrect urls sequence 1");
    QuerySearch("body&pron=sortbyInvLiveTime&dbgrlv=da&fsgta=_JsonFactors&relev=formula=default1&" + GetAllKps(messages1), results);
    if (results.size() != 2)
        TEST_FAILED("Incorrect results count: " + ToString(results.size()));
    if (results[0].GetUrl() != messages2[0].GetDocument().GetUrl() ||
        results[1].GetUrl() != messages1[0].GetDocument().GetUrl())
        TEST_FAILED("Incorrect urls sequence 2");
    sleep(1);
    QuerySearch("body&pron=sortbyInvLiveTime&dbgrlv=da&fsgta=_JsonFactors&relev=formula=default2&" + GetAllKps(messages1), results, &resultProps, nullptr, true);
    if (results.size() != 2)
        TEST_FAILED("Incorrect results count: " + ToString(results.size()));
    factors0 = TRTYFactorsParser::GetJsonFactorsValues(resultProps)[0];
    iFactor0 = factors0.find("_Time__start");
    factors1 = TRTYFactorsParser::GetJsonFactorsValues(resultProps)[1];
    iFactor1 = factors1.find("_Time__start");
    if (iFactor0 == factors0.end() || iFactor1 == factors1.end()) {
        TEST_FAILED("Can't find fresh factors");
    }
    if (fabs(iFactor0->second - iFactor1->second) > 1)
        TEST_FAILED("Invalid factor values: " + ToString(iFactor0->second) + " != " + ToString(iFactor1->second));
    if (iFactor0->second > 1)
        TEST_FAILED("Invalid factor values: " + ToString(iFactor0->second) + " !in [0, 1]");
    if (iFactor1->second > 1)
        TEST_FAILED("Invalid factor values: " + ToString(iFactor1->second) + " !in [0, 1]");

    QuerySearch("tuple&pron=sortbyInvLiveTime&dbgrlv=da&fsgta=_JsonFactors&relev=formula=default2&relev=all_factors&" + GetAllKps(messages2), results, &resultProps, nullptr, true);
    factors0 = TRTYFactorsParser::GetJsonFactorsValues(resultProps)[0];
    auto fDay = factors0.find("FreshnessDay");
    auto fWeek = factors0.find("FreshnessWeek");
    auto fMonth = factors0.find("FreshnessMonth");
    Y_ENSURE(fDay != factors0.end(), "No FreshnessDay");
    Y_ENSURE(fWeek != factors0.end(), "No FreshnessWeek");
    Y_ENSURE(fMonth != factors0.end(), "No FreshnessMonth");
    Y_ENSURE(fabs(fDay->second - 0.125f) < 0.1, "incorrect FreshnessDay");
    Y_ENSURE(fabs(fWeek->second - 0.5f) < 0.1, "incorrect FreshnessWeek");
    Y_ENSURE(fabs(fMonth->second - 0.81f) < 0.1, "incorrect FreshnessMonth");
    return true;
}
bool InitConfig() override {
    (*ConfigDiff)["Searcher.FiltrationModel"] = "SIMPLE";
    (*ConfigDiff)["Searcher.FactorsInfo"] = GetResourcesDirectory() + "/text_relev/text_factors_fresh.cfg";
    return true;
}
};

START_TEST_DEFINE_PARENT(TestZonesMakeupMerger, TestDynamicFactorsParent)
#define THROW_IF_NOT_EQUAL(A,B) if (A != B) ythrow yexception() << "check failed " << A << " != " << B;

inline void CheckRef(TJsonPtr json) {
    TDocInfo di(*json);
    PrintMakeup(di);
    TDocInfo::TMakeupDocInfo& mi = const_cast<TDocInfo::TMakeupDocInfo&>(di.GetMakeupDocInfo());
    TDocInfo::TZonesLengths zonesLength = mi.ZonesLengths;
    THROW_IF_NOT_EQUAL(zonesLength["aaa"], 1);
    THROW_IF_NOT_EQUAL(zonesLength["bbb"], 2);

    TDocInfo::TZonesInfo zonesInfo = mi.ZonesInfo;
    THROW_IF_NOT_EQUAL(zonesInfo["aaa"], "00010");
    THROW_IF_NOT_EQUAL(zonesInfo["bbb"], "00001");
}

inline void CheckRef1(TJsonPtr json) {
    TDocInfo di(*json);
    PrintMakeup(di);
    TDocInfo::TMakeupDocInfo& mi = const_cast<TDocInfo::TMakeupDocInfo&>(di.GetMakeupDocInfo());
    TDocInfo::TZonesLengths zonesLength = mi.ZonesLengths;
    THROW_IF_NOT_EQUAL(zonesLength["aaa"], 2);
    THROW_IF_NOT_EQUAL(zonesLength["bbb"], 3);

    TDocInfo::TZonesInfo zonesInfo = mi.ZonesInfo;
    THROW_IF_NOT_EQUAL(zonesInfo["aaa"], "01010");
    THROW_IF_NOT_EQUAL(zonesInfo["bbb"], "00001");
}

bool Run() override {
    TVector<TString> texts, texts1;
    TVector<NRTYServer::TMessage> messages, messages1;
    texts.push_back("<xml><sss>asdsa</sss><aaa1>sdsd</aaa1><aaa int_zone_attr=\"42\">world</aaa><bbb>hello whatever</bbb></xml>");
    texts1.push_back("<xml1><aaa>asdsa</aaa><aaa2>sdsd</aaa2><aaa int_zone_attr=\"42\">world</aaa><bbb>hello moto music</bbb></xml1>");
    GenerateInput(messages, texts.size(), NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    GenerateInput(messages1, texts1.size(), NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    for (size_t i = 0; i < texts.size(); ++i) {
        messages[i].MutableDocument()->SetMimeType("text/xml");
        messages[i].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());
        messages[i].MutableDocument()->SetBody(texts[i]);
    }
    for (size_t i = 0; i < texts1.size(); ++i) {
        messages1[i].MutableDocument()->SetMimeType("text/xml");
        messages1[i].MutableDocument()->SetKeyPrefix(messages1[0].GetDocument().GetKeyPrefix());
        messages1[i].MutableDocument()->SetBody(texts1[i]);
    }
    {
        ui64 kps = messages[0].GetDocument().GetKeyPrefix();
        TString url = messages[0].GetDocument().GetUrl();

        IndexMessages(messages, REALTIME, 1);
        DEBUG_LOG << "Check doc mem" << Endl;
        TDocSearchInfo dsi = GetDocSearchInfo(url, kps);
        CheckRef(Controller->GetDocInfo(dsi.GetSearcherId(), dsi.GetDocId()));

        ReopenIndexers();
        DEBUG_LOG << "Check doc disk" << Endl;
        dsi = GetDocSearchInfo(url, kps);
        CheckRef(Controller->GetDocInfo(dsi.GetSearcherId(), dsi.GetDocId()));
    }
    {
        ui64 kps = messages1[0].GetDocument().GetKeyPrefix();
        TString url = messages1[0].GetDocument().GetUrl();

        IndexMessages(messages1, REALTIME, 1);
        DEBUG_LOG << "Check doc1 mem" << Endl;
        TDocSearchInfo dsi = GetDocSearchInfo(url, kps);
        CheckRef1(Controller->GetDocInfo(dsi.GetSearcherId(), dsi.GetDocId()));

        ReopenIndexers();
        DEBUG_LOG << "Check doc1 disk" << Endl;
        dsi = GetDocSearchInfo(url, kps);
        CheckRef1(Controller->GetDocInfo(dsi.GetSearcherId(), dsi.GetDocId()));
    }
    Controller->ProcessCommand("create_merger_tasks");
    Controller->ProcessCommand("do_all_merger_tasks");
    {
        ui64 kps = messages[0].GetDocument().GetKeyPrefix();
        TString url = messages[0].GetDocument().GetUrl();

        DEBUG_LOG << "Check doc merged" << Endl;
        TDocSearchInfo dsi = GetDocSearchInfo(url, kps);
        CheckRef(Controller->GetDocInfo(dsi.GetSearcherId(), dsi.GetDocId()));
    }
    {
        ui64 kps = messages1[0].GetDocument().GetKeyPrefix();
        TString url = messages1[0].GetDocument().GetUrl();

        DEBUG_LOG << "Check doc1 merged" << Endl;
        TDocSearchInfo dsi = GetDocSearchInfo(url, kps);
        CheckRef1(Controller->GetDocInfo(dsi.GetSearcherId(), dsi.GetDocId()));
    }
    CheckMakeup();
    return true;
}

bool InitConfig() override {
    (*ConfigDiff)["Indexer.Common.XmlParserConfigFile"] = "";
    (*ConfigDiff)["Searcher.FactorsInfo"] = GetResourcesDirectory() + "/factors/zone_factors.cfg";
    SetMergerParams(true, 1, -1, mcpNONE);
    return true;
}
};


START_TEST_DEFINE_PARENT(TestZoneFactorsBM25F, TestDynamicFactorsParent)
void Check(const char* comment, const TVector<NRTYServer::TMessage>& messages) {
    TVector<TDocSearchInfo> results;
    TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > > resultProps;
    QuerySearch("hello&relev=formula=test_bm25&dbgrlv=da&fsgta=_JsonFactors" + GetAllKps(messages), results, &resultProps);
    TVector<THashMap<TString, double> > factors = TRTYFactorsParser::GetJsonFactorsValues(resultProps);
    if (results.size() != messages.size())
        ythrow yexception() << comment << ": some messages not found for 'hello'";
    if (results[0].GetUrl() != messages[0].GetDocument().GetUrl())
        ythrow yexception() << comment << ": invalid result order for 'hello'";
    if (factors[0]["_BM25F_St_aaa"] > 0.001)
        ythrow yexception() << comment << ": invalid factor aaa for first document";
    if (factors[0]["_BM25F_St_bbb"] < 0.001)
        ythrow yexception() << comment << ": invalid factor bbb for first document";
    if (factors[1]["_BM25F_St_bbb"] > 0.001)
        ythrow yexception() << comment << ": invalid factor bbb for second document";
    if (factors[1]["_BM25F_St_aaa"] < 0.001)
        ythrow yexception() << comment << ": invalid factor aaa for second document";


    QuerySearch("world&relev=formula=test_bm25&dbgrlv=da&fsgta=_JsonFactors" + GetAllKps(messages), results, &resultProps);
    factors = TRTYFactorsParser::GetJsonFactorsValues(resultProps);
    if (results.size() != messages.size())
        ythrow yexception() << comment << ": some messages not found for 'word'";
    if (results[0].GetUrl() != messages[1].GetDocument().GetUrl())
        ythrow yexception() << comment << ": invalid result order for 'word'";
    if (factors[0]["_BM25F_St_aaa"] > 0.001)
        ythrow yexception() << comment << ": invalid factor aaa for first document";
    if (factors[0]["_BM25F_St_bbb"] < 0.001)
        ythrow yexception() << comment << ": invalid factor bbb for first document";
    if (factors[1]["_BM25F_St_bbb"] > 0.001)
        ythrow yexception() << comment << ": invalid factor bbb for second document";
    if (factors[1]["_BM25F_St_aaa"] < 0.001)
        ythrow yexception() << comment << ": invalid factor aaa for second document";
}

bool Run() override {
    TVector<TString> texts;
    texts.push_back("<xml><aaa int_zone_attr=\"42\">world</aaa><bbb>hello</bbb></xml>");
    texts.push_back("<xml><aaa int_zone_attr=\"12\">hello</aaa><bbb>world</bbb></xml>");
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, texts.size(), NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    for (size_t i = 0; i < texts.size(); ++i) {
        messages[i].MutableDocument()->SetMimeType("text/xml");
        messages[i].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());
        messages[i].MutableDocument()->SetBody(texts[i]);
    }
    IndexMessages(messages, REALTIME, 1);
    Check("memory", messages);
    ReopenIndexers();
    Check("disk", messages);
    TestDynamicFactorsParent::CheckMakeup();
    return true;
}

bool InitConfig() override {
    (*ConfigDiff)["Indexer.Common.XmlParserConfigFile"] = "";
    (*ConfigDiff)["Searcher.FactorsInfo"] = GetResourcesDirectory() + "/factors/zone_factors.cfg";
    return true;
}
};

START_TEST_DEFINE_PARENT(TestManyZonesFactorsBM25F, TestDynamicFactorsParent)
void Check(const char* comment, const TVector<NRTYServer::TMessage>& messages, const TString& addArgs) {
    TVector<TDocSearchInfo> results;
    TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > > resultProps;
    QuerySearch("hello&relev=formula=test_bm25&dbgrlv=da&fsgta=_JsonFactors" + GetAllKps(messages) + addArgs, results, &resultProps);
    TVector<THashMap<TString, double> > factors = TRTYFactorsParser::GetJsonFactorsValues(resultProps);
    if (results.size() != messages.size())
        ythrow yexception() << comment << ": some messages not found for 'hello'";
//    if (results[0].GetUrl() != messages[0].GetDocument().GetUrl())
//        ythrow yexception() << comment << ": invalid result order for 'hello'";
    if (factors[0]["_BM25F_St_aaa"] > 0.001)
        ythrow yexception() << comment << ": invalid factor aaa for first document";
    if (factors[0]["_BM25F_St_bbb"] < 0.001)
        ythrow yexception() << comment << ": invalid factor bbb for first document";
    if (factors[1]["_BM25F_St_bbb"] > 0.001)
        ythrow yexception() << comment << ": invalid factor bbb for second document";
    if (factors[1]["_BM25F_St_aaa"] < 0.001)
        ythrow yexception() << comment << ": invalid factor aaa for second document";


    QuerySearch("world&relev=formula=test_bm25&dbgrlv=da&fsgta=_JsonFactors" + GetAllKps(messages) + addArgs, results, &resultProps);
    factors = TRTYFactorsParser::GetJsonFactorsValues(resultProps);
    if (results.size() != messages.size())
        ythrow yexception() << comment << ": some messages not found for 'word'";
//    if (results[0].GetUrl() != messages[1].GetDocument().GetUrl())
//        ythrow yexception() << comment << ": invalid result order for 'word'";
    if (factors[0]["_BM25F_St_aaa"] > 0.001)
        ythrow yexception() << comment << ": invalid factor aaa for first document";
    if (factors[0]["_BM25F_St_bbb"] < 0.001)
        ythrow yexception() << comment << ": invalid factor bbb for first document";
    if (factors[1]["_BM25F_St_bbb"] > 0.001)
        ythrow yexception() << comment << ": invalid factor bbb for second document";
    if (factors[1]["_BM25F_St_aaa"] < 0.001)
        ythrow yexception() << comment << ": invalid factor aaa for second document";
}

void TimeTest(bool withFake, i64& t1, i64& t2) {
    TVector<TString> texts;
    TString fakeText1;
    TString fakeText2;
    TString fakeText3;
    if (withFake) {
        for (ui32 i = 0; i < 10000; ++i) {
            TString zoneName1 = "zone" + ToString(i);
            TString zoneName2 = "zone" + ToString(i + 7000);
            TString zoneName3 = "zone" + ToString(i + 17000);
            fakeText1 += "<" + zoneName1 + ">" + zoneName1 + "</" + zoneName1 + ">";
            fakeText2 += "<" + zoneName2 + ">" + zoneName2 + "</" + zoneName2 + ">";
            fakeText3 += "<" + zoneName3 + ">" + zoneName3 + "</" + zoneName3 + ">";
        }
    }
    texts.push_back("<xml>" + fakeText1 + "<aaa int_zone_attr=\"42\">world</aaa>" + fakeText2 + "<bbb>hello</bbb>" + fakeText3 + "</xml>");
    texts.push_back("<xml>" + fakeText1 + "<aaa int_zone_attr=\"12\">hello</aaa>" + fakeText2 + "<bbb>world</bbb>" + fakeText3 + "</xml>");
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, texts.size(), NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    messages[0].MutableDocument()->SetUrl("0");
    messages[1].MutableDocument()->SetUrl("1");
    for (size_t i = 0; i < texts.size(); ++i) {
        messages[i].MutableDocument()->SetMimeType("text/xml");
        messages[i].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());
        messages[i].MutableDocument()->SetBody(texts[i]);
    }
    IndexMessages(messages, REALTIME, 1);
    t1 = Now().MilliSeconds();
    for (ui32 i = 0; i < 250; ++i) {
        Check("memory", messages, "&haha=da");
    }
    t1 = Now().MilliSeconds() - t1;

    ReopenIndexers();

    t2 = Now().MilliSeconds();
    for (ui32 i = 0; i < 250; ++i) {
        Check("disk", messages, "&haha=da");
    }
    t2 = Now().MilliSeconds() - t2;
}

bool Run() override {
    i64 t1F, t2F, t1NF, t2NF;
    TimeTest(false, t1NF, t2NF);
    TimeTest(true, t1F, t2F);
    DEBUG_LOG << t2F - t2NF << "/" << Max(t2F, t2NF) << Endl;
    DEBUG_LOG << t1F - t1NF << "/" << Max(t1F, t1NF) << Endl;
    if (t2F > t2NF) {
        CHECK_TEST_LESS((1.0 * (t2F - t2NF)) / Max(t2F, t2NF), 0.15);
    }
    if (t1F > t1NF) {
        CHECK_TEST_LESS((1.0 * (t1F - t1NF)) / Max(t1F, t1NF), 0.15);
    }
    return true;

}

bool InitConfig() override {
    (*ConfigDiff)["Indexer.Common.XmlParserConfigFile"] = "";
    (*ConfigDiff)["Searcher.FactorsInfo"] = GetResourcesDirectory() + "/factors/zone_factors.cfg";
    return true;
}
};

START_TEST_DEFINE_PARENT(TestSwitchZoneFactors, TestDynamicFactorsParent)
bool Run() override {
    TVector<TString> texts;
    texts.push_back("<xml><aaa int_zone_attr=\"42\">world</aaa><bbb>hello</bbb></xml>");
    texts.push_back("<xml><ccc int_zone_attr=\"12\">hello</ccc><ddd>world</ddd></xml>");
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, texts.size(), NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    for (size_t i = 0; i < texts.size(); ++i) {
        messages[i].MutableDocument()->SetMimeType("text/xml");
        messages[i].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());
        messages[i].MutableDocument()->SetBody(texts[i]);
    }
    IndexMessages(messages, DISK, 1);
    ReopenIndexers();

    InitConfig2();
    TConfigFieldsPtr configFields = Controller->ApplyConfigDiff(ConfigDiff);
    return true;
}

bool InitConfig() override {
    (*ConfigDiff)["Indexer.Common.XmlParserConfigFile"] = "";
    (*ConfigDiff)["Searcher.FactorsInfo"] = GetResourcesDirectory() + "/factors/zone_factors.cfg";
    return true;
}

virtual bool InitConfig2() {
    (*ConfigDiff)["Searcher.FactorsInfo"] = GetResourcesDirectory() + "/factors/zone_factors2.cfg";
    return true;
}
};


START_TEST_DEFINE_PARENT(TestZoneFactorsPeoples, TestDynamicFactorsParent)
inline void CheckRef(TJsonPtr json) {
    TDocInfo di(*json);
    PrintMakeup(di);
    TDocInfo::TZonesInfo& makeupDocInfo = const_cast<TDocInfo::TZonesInfo&>(di.GetMakeupDocInfo().ZonesInfo);
    THROW_IF_NOT_EQUAL(makeupDocInfo["psnames"], "0011100000000000000000000000000000000000000000");
    THROW_IF_NOT_EQUAL(makeupDocInfo["pscontacts"], "0000000000000000000000000000000000000000000000");
    THROW_IF_NOT_EQUAL(makeupDocInfo["pslocations"], "0000000000000000000000000000000000000000000000");
    THROW_IF_NOT_EQUAL(makeupDocInfo["pscareers"], "0000000001111111111111111111111111111111000000");
    THROW_IF_NOT_EQUAL(makeupDocInfo["psbirth"], "0000000000000000000000000000000000000000111100");
    THROW_IF_NOT_EQUAL(makeupDocInfo["psbio"], "0000000000000000000000000000000000000000000000");
    THROW_IF_NOT_EQUAL(makeupDocInfo["psnets"], "0000000000000000000000000000000000000000000011");
}
bool Run() override {
    TVector<TString> texts;
    texts.push_back("<body1><aaTitle1>Наталья Сорокина (Сатурова)</aaTitle1><psnames><psfull1> Наталья Сорокина (Сатурова)</psfull1><psfirst1> наталья</psfirst1><pssecond1> сорокина</pssecond1></psnames><psgeo1 psactual=\"1\"  psgeobaseid=\"47\" ><pscountry1>Россия</pscountry1><psregion1>Нижегородская область</psregion1><pscity1>Нижний Новгород</pscity1><pstitle1>Нижний Новгород</pstitle1></psgeo1><pscareers1 psstartyear=\"2011\"  psendyear=\"2011\"  pstype=\"1\"  psrecognizedschoolnumber=\"22\" ><psgeo1 psactual=\"0\"  psgeobaseid=\"47\" ><pscountry1>Россия</pscountry1><psregion1>Нижегородская область</psregion1><pscity1>Нижний Новгород</pscity1><pstitle1>Нижний Новгород, Россия</pstitle1></psgeo1><psstartyear1>2011</psstartyear1><psendyear1>2011</psendyear1><pscaption1>22 школа</pscaption1><psrecognizedschoolnumber1>22</psrecognizedschoolnumber1></pscareers1><pscareers1 psstartyear=\"2000\"  psendyear=\"2008\"  pstype=\"2\"  psrecognizedschoolnumber=\"0\" ><psgeo1 psactual=\"0\"  psgeobaseid=\"213\" ><pscountry1>Россия</pscountry1><psregion1>Москва и Московская область</psregion1><pscity1>Москва</pscity1><pstitle1>Москва, Россия</pstitle1></psgeo1><psstartyear1>2000</psstartyear1><psendyear1>2008</psendyear1><pscaption1>МПИ ФСБ РФ, Московский пограничный институт ФСБ РФ; бывш. МВИ ФПС РФ, МВПКУ КГБ СССР, ВПУ</pscaption1></pscareers1><pscareers1 psstartyear=\"1993\"  psendyear=\"2006\"  pstype=\"1\"  psrecognizedschoolnumber=\"7\" ><psgeo1 psactual=\"0\"  psgeobaseid=\"47\" ><pscountry1>Россия</pscountry1><psregion1>Нижегородская область</psregion1><pscity1>Нижний Новгород</pscity1><pstitle1>Нижний Новгород, Россия</pstitle1></psgeo1><psstartyear1>1993</psstartyear1><psendyear1>2006</psendyear1><pscaption1>7 школа</pscaption1><psrecognizedschoolnumber1>7</psrecognizedschoolnumber1></pscareers1><pscareers1 psstartyear=\"1974\"  psendyear=\"1982\"  pstype=\"2\"  psrecognizedschoolnumber=\"0\" ><psgeo1 psactual=\"0\"  psgeobaseid=\"153\" ><pscountry1>Беларусь</pscountry1><psregion1>Брестская область</psregion1><pscity1>Брест</pscity1><pstitle1>Брест, Беларусь</pstitle1></psgeo1><psstartyear1>1974</psstartyear1><psendyear1>1982</psendyear1><pscaption1>БрГТУ, Строительный факультет</pscaption1></pscareers1><psbirth1 psage=\"56\" ><psdate1 psyear=\"1957\"  psmonth=\"8\"  psday=\"16\" ><psyear1>1957</psyear1><psmonth1>8</psmonth1><psday1>16</psday1></psdate1><psage1>56</psage1></psbirth1><psattributes1 psauthorizedcountries=\"225\" ></psattributes1><psnets1><psprofileurl1>http://www.odnoklassniki.ru/profile/517594360411</psprofileurl1><psprofileid1>profile/517594360411</psprofileid1><psattributes1 psinfotype=\"1\"  pssourcetype=\"2\" ></psattributes1></psnets1></body1>");
    texts.push_back("<body><Title>Наталья Сорокина (Сатурова)</Title><psnames><psfull> Наталья Сорокина (Сатурова)</psfull><psfirst> наталья</psfirst><pssecond> сорокина</pssecond></psnames><psgeo psactual=\"1\"  psgeobaseid=\"47\" ><pscountry>Россия</pscountry><psregion>Нижегородская область</psregion><pscity>Нижний Новгород</pscity><pstitle>Нижний Новгород</pstitle></psgeo><pscareers psstartyear=\"2011\"  psendyear=\"2011\"  pstype=\"1\"  psrecognizedschoolnumber=\"22\" ><psgeo psactual=\"0\"  psgeobaseid=\"47\" ><pscountry>Россия</pscountry><psregion>Нижегородская область</psregion><pscity>Нижний Новгород</pscity><pstitle>Нижний Новгород, Россия</pstitle></psgeo><psstartyear>2011</psstartyear><psendyear>2011</psendyear><pscaption>22 школа</pscaption><psrecognizedschoolnumber>22</psrecognizedschoolnumber></pscareers><pscareers psstartyear=\"2000\"  psendyear=\"2008\"  pstype=\"2\"  psrecognizedschoolnumber=\"0\" ><psgeo psactual=\"0\"  psgeobaseid=\"213\" ><pscountry>Россия</pscountry><psregion>Москва и Московская область</psregion><pscity>Москва</pscity><pstitle>Москва, Россия</pstitle></psgeo><psstartyear>2000</psstartyear><psendyear>2008</psendyear><pscaption>МПИ ФСБ РФ, Московский пограничный институт ФСБ РФ; бывш. МВИ ФПС РФ, МВПКУ КГБ СССР, ВПУ</pscaption></pscareers><pscareers psstartyear=\"1993\"  psendyear=\"2006\"  pstype=\"1\"  psrecognizedschoolnumber=\"7\" ><psgeo psactual=\"0\"  psgeobaseid=\"47\" ><pscountry>Россия</pscountry><psregion>Нижегородская область</psregion><pscity>Нижний Новгород</pscity><pstitle>Нижний Новгород, Россия</pstitle></psgeo><psstartyear>1993</psstartyear><psendyear>2006</psendyear><pscaption>7 школа</pscaption><psrecognizedschoolnumber>7</psrecognizedschoolnumber></pscareers><pscareers psstartyear=\"1974\"  psendyear=\"1982\"  pstype=\"2\"  psrecognizedschoolnumber=\"0\" ><psgeo psactual=\"0\"  psgeobaseid=\"153\" ><pscountry>Беларусь</pscountry><psregion>Брестская область</psregion><pscity>Брест</pscity><pstitle>Брест, Беларусь</pstitle></psgeo><psstartyear>1974</psstartyear><psendyear>1982</psendyear><pscaption>БрГТУ, Строительный факультет</pscaption></pscareers><psbirth psage=\"56\" ><psdate psyear=\"1957\"  psmonth=\"8\"  psday=\"16\" ><psyear>1957</psyear><psmonth>8</psmonth><psday>16</psday></psdate><psage>56</psage></psbirth><psattributes psauthorizedcountries=\"225\" ></psattributes><psnets><psprofileurl>http://www.odnoklassniki.ru/profile/517594360411</psprofileurl><psprofileid>profile/517594360411</psprofileid><psattributes psinfotype=\"1\"  pssourcetype=\"2\" ></psattributes></psnets></body>");
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, texts.size(), NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    for (size_t i = 0; i < texts.size(); ++i) {
        messages[i].MutableDocument()->SetMimeType("text/xml");
        messages[i].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());
        messages[i].MutableDocument()->SetBody(texts[i]);
    }
    {
        ui64 kps = messages[1].GetDocument().GetKeyPrefix();
        TString url = messages[1].GetDocument().GetUrl();

        IndexMessages(messages, REALTIME, 1);
        DEBUG_LOG << "Check doc mem" << Endl;
        TDocSearchInfo dsi = GetDocSearchInfo(url, kps);
        CheckRef(Controller->GetDocInfo(dsi.GetSearcherId(), dsi.GetDocId()));

        ReopenIndexers();
        DEBUG_LOG << "Check doc disk" << Endl;
        dsi = GetDocSearchInfo(url, kps);
        CheckRef(Controller->GetDocInfo(dsi.GetSearcherId(), dsi.GetDocId()));
    }
    return true;
}

bool InitConfig() override {
    (*ConfigDiff)["Indexer.Common.XmlParserConfigFile"] = "";
    (*ConfigDiff)["Searcher.FactorsInfo"] = GetResourcesDirectory() + "/factors/people_factors.cfg";
    return true;
}
};

START_TEST_DEFINE_PARENT(TestZoneFactorsCoordinationMatch, TestDynamicFactorsParent)
void Check(const char* comment, const TVector<NRTYServer::TMessage>& messages) {
    TVector<TDocSearchInfo> results;
    TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > > resultProps;
    QuerySearch("url:\"*\"&relev=formula=test_cm&dbgrlv=da&fsgta=_JsonFactors" + GetAllKps(messages), results, &resultProps);
    if (results.size() != messages.size())
        ythrow yexception() << comment << ": check count1 failed. " << results.size() << " != " << 2;
    QuerySearch("vasya&relev=formula=test_cm&dbgrlv=da&fsgta=_JsonFactors" + GetAllKps(messages), results, &resultProps);
    if (results.size() != 1)
        ythrow yexception() << comment << ": check count_vasya failed. " << results.size() << " != " << 1;
    QuerySearch("hello&relev=formula=test_cm&dbgrlv=da&fsgta=_JsonFactors" + GetAllKps(messages), results, &resultProps);
    if (results.size() != 2)
        ythrow yexception() << comment << ": check count2 failed. " << results.size() << " != " << 2;
    QuerySearch("world&relev=formula=test_cm&dbgrlv=da&fsgta=_JsonFactors" + GetAllKps(messages), results, &resultProps);
    if (results.size() != 2)
        ythrow yexception() << comment << ": check count3 failed. " << results.size() << " != " << 2;
    QuerySearch("hello world&relev=formula=test_cm&dbgrlv=da&fsgta=_JsonFactors" + GetAllKps(messages), results, &resultProps);
    TVector<THashMap<TString, double> > factors = TRTYFactorsParser::GetJsonFactorsValues(resultProps);
    if (results.size() != 2)
        ythrow yexception() << comment << ": some messages not found for 'hello world'. " << results.size() << " != " << 2;
    if (results[0].GetUrl() != messages[1].GetDocument().GetUrl())
        ythrow yexception() << comment << ": invalid result order for 'hello world'";
    if (Abs(factors[0]["_CM_St_aaa"] - 1) > 0.001)
        ythrow yexception() << comment << ": invalid factor aaa for first document";
    if (Abs(factors[0]["_CM_St_bbb"] - 0.5) > 0.001)
        ythrow yexception() << comment << ": invalid factor bbb for first document";
    if (Abs(factors[1]["_CM_St_aaa"] - 0.5) > 0.001)
        ythrow yexception() << comment << ": invalid factor aaa for second document";
    if (Abs(factors[1]["_CM_St_bbb"] - 0.5) > 0.001)
        ythrow yexception() << comment << ": invalid factor bbb for second document";


    QuerySearch("cool less softness:100&relev=formula=test_cm&dbgrlv=da&fsgta=_JsonFactors" + GetAllKps(messages), results, &resultProps);
    factors = TRTYFactorsParser::GetJsonFactorsValues(resultProps);
    if (results.size() != 2)
        ythrow yexception() << comment << ": some messages not found for 'cool less softness:100'. " << results.size() << " != " << 2;
    if (results[0].GetUrl() != messages[0].GetDocument().GetUrl())
        ythrow yexception() << comment << ": invalid result order for 'word'";
    if (Abs(factors[0]["_CM_St_aaa"] - 0.5) > 0.001)
        ythrow yexception() << comment << ": invalid factor aaa for first document";
    if (Abs(factors[0]["_CM_St_bbb"] - 0.5) > 0.001)
        ythrow yexception() << comment << ": invalid factor bbb for first document";
    if (Abs(factors[1]["_CM_St_aaa"] - 0) > 0.001)
        ythrow yexception() << comment << ": invalid factor aaa for second document";
    if (Abs(factors[1]["_CM_St_bbb"] - 0.5) > 0.001)
        ythrow yexception() << comment << ": invalid factor bbb for second document";

    QuerySearch("cool&relev=formula=test_cm&dbgrlv=da&relev=all_factors&fsgta=_JsonFactors" + GetAllKps(messages), results, &resultProps);
    auto invFreq = TRTYFactorsParser::GetJsonFactorsValues(resultProps);
    if (Abs(invFreq[0]["_IF_St_aaa"] - 0.5) > 0.1)
        ythrow yexception() << comment << ": invalid factor _IF_St_aaa " << invFreq[0]["_IF_St_aaa"];
}

bool Run() override {
    TVector<TString> texts;
    texts.push_back("<xml><aaa>world big stupid mmm cool cool hren</aaa><bbb>hello me too. its cool cool</bbb></xml>");
    texts.push_back("<xml><aaa>hello world</aaa><bbb>world is cool</bbb></xml>");
    texts.push_back("vasya was here");
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, texts.size(), NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    for (size_t i = 0; i < texts.size(); ++i) {
        if (i < 2)
            messages[i].MutableDocument()->SetMimeType("text/xml");
        else
            messages[i].MutableDocument()->SetMimeType("text/plain");
        messages[i].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());
        messages[i].MutableDocument()->SetBody(texts[i]);
    }
    IndexMessages(messages, REALTIME, 1);
    Check("memory", messages);
    ReopenIndexers();
    Check("disk", messages);
    TestDynamicFactorsParent::CheckMakeup();
    return true;
}

bool InitConfig() override {
    (*ConfigDiff)["Indexer.Common.XmlParserConfigFile"] = "";
    (*ConfigDiff)["Searcher.FiltrationModel"] = "SIMPLE";
    (*ConfigDiff)["Searcher.FactorsInfo"] = GetResourcesDirectory() + "/factors/zone_factors.cfg";
    return true;
}
};

START_TEST_DEFINE_PARENT(TestZoneFactorsCZL, TestDynamicFactorsParent)
void Check(const char* comment, const TVector<NRTYServer::TMessage>& messages) {
    TVector<TDocSearchInfo> results;
    TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > > resultProps;
    QuerySearch("url:\"*\"&relev=formula=test_czl&dbgrlv=da&fsgta=_JsonFactors" + GetAllKps(messages), results, &resultProps);
    if (results.size() != messages.size())
        ythrow yexception() << comment << ": check count1 failed. " << results.size() << " != " << 2;
    QuerySearch("vasya&relev=formula=test_czl&dbgrlv=da&fsgta=_JsonFactors" + GetAllKps(messages), results, &resultProps);
    if (results.size() != 1)
        ythrow yexception() << comment << ": check count_vasya failed. " << results.size() << " != " << 1;
    QuerySearch("hello&relev=formula=test_czl&dbgrlv=da&fsgta=_JsonFactors" + GetAllKps(messages), results, &resultProps);
    if (results.size() != 2)
        ythrow yexception() << comment << ": check count2 failed. " << results.size() << " != " << 2;
    QuerySearch("world&relev=formula=test_czl&dbgrlv=da&fsgta=_JsonFactors" + GetAllKps(messages), results, &resultProps);
    if (results.size() != 2)
        ythrow yexception() << comment << ": check count3 failed. " << results.size() << " != " << 2;
    QuerySearch("hello world&relev=formula=test_czl&dbgrlv=da&fsgta=_JsonFactors" + GetAllKps(messages), results, &resultProps);
    TVector<THashMap<TString, double> > factors = TRTYFactorsParser::GetJsonFactorsValues(resultProps);
    if (results.size() != 2)
        ythrow yexception() << comment << ": some messages not found for 'hello world'. " << results.size() << " != " << 2;
    if (results[0].GetUrl() != messages[1].GetDocument().GetUrl())
        ythrow yexception() << comment << ": invalid result order for 'word'";
    if (Abs(factors[0]["_CZL_St_aaa"] - 2.0 / 2.0) > 0.001)
        ythrow yexception() << comment << ": invalid factor aaa CZL for first document";
    if (Abs(factors[0]["_CZL_St_bbb"] - 1.0 / 3.0) > 0.001)
        ythrow yexception() << comment << ": invalid factor bbb CZL for first document";
    if (Abs(factors[1]["_CZL_St_aaa"] - 1.0 / 7.0) > 0.001)
        ythrow yexception() << comment << ": invalid factor aaa CZL for second document";
    if (Abs(factors[1]["_CZL_St_bbb"] - 1.0 / 7.0) > 0.001)
        ythrow yexception() << comment << ": invalid factor bbb CZL for second document";

    if (Abs(factors[0]["_CZ_St_aaa"] - 2.0) > 0.001)
        ythrow yexception() << comment << ": invalid factor aaa CZ for first document";
    if (Abs(factors[0]["_CZ_St_bbb"] - 1.0) > 0.001)
        ythrow yexception() << comment << ": invalid factor bbb CZ for first document";
    if (Abs(factors[1]["_CZ_St_aaa"] - 1.0) > 0.001)
        ythrow yexception() << comment << ": invalid factor aaa CZ for second document";
    if (Abs(factors[1]["_CZ_St_bbb"] - 1.0) > 0.001)
        ythrow yexception() << comment << ": invalid factor bbb CZ for second document";


    QuerySearch("cool less softness:100&relev=formula=test_czl&dbgrlv=da&fsgta=_JsonFactors" + GetAllKps(messages), results, &resultProps);
    factors = TRTYFactorsParser::GetJsonFactorsValues(resultProps);
    if (results.size() != 2)
        ythrow yexception() << comment << ": some messages not found for 'cool less softness:100'. " << results.size() << " != " << 2;
    if (results[0].GetUrl() != messages[0].GetDocument().GetUrl())
        ythrow yexception() << comment << ": invalid result order for 'word'";
    if (Abs(factors[0]["_CZL_St_aaa"] - 1.0 / 7.0) > 0.001)
        ythrow yexception() << comment << ": invalid factor aaa CZL for first document";
    if (Abs(factors[0]["_CZL_St_bbb"] - 1.0 / 7.0) > 0.001)
        ythrow yexception() << comment << ": invalid factor bbb CZL for first document";
    if (Abs(factors[1]["_CZL_St_aaa"] - 0) > 0.001)
        ythrow yexception() << comment << ": invalid factor aaa CZL for second document";
    if (Abs(factors[1]["_CZL_St_bbb"] - 1.0 / 3.0) > 0.001)
        ythrow yexception() << comment << ": invalid factor bbb CZL for second document";

    if (Abs(factors[0]["_CZ_St_aaa"] - 1.0) > 0.001)
        ythrow yexception() << comment << ": invalid factor aaa CZ for first document";
    if (Abs(factors[0]["_CZ_St_bbb"] - 1.0) > 0.001)
        ythrow yexception() << comment << ": invalid factor bbb CZ for first document";
    if (Abs(factors[1]["_CZ_St_aaa"] - 0) > 0.001)
        ythrow yexception() << comment << ": invalid factor aaa CZ for second document";
    if (Abs(factors[1]["_CZ_St_bbb"] - 1.0) > 0.001)
        ythrow yexception() << comment << ": invalid factor bbb CZ for second document";

    QuerySearch("hhh:(world hello)+bbb:(vvvbbb)&relev=formula=test_czl&dbgrlv=da&fsgta=_JsonFactors" + GetAllKps(messages), results, &resultProps);
    factors = TRTYFactorsParser::GetJsonFactorsValues(resultProps);
    if (results.size() != 1)
        ythrow yexception() << comment << ": some messages not found for 'cool less softness:100'. " << results.size() << " != " << 1;
    if (results[0].GetUrl() != messages[0].GetDocument().GetUrl())
        ythrow yexception() << comment << ": invalid result order for 'word'";

    if (Abs(factors[0]["_CZ_St_aaa"] - 1.0) > 0.001)
        ythrow yexception() << comment << ": invalid factor aaa CZ for first document" << factors[0]["_CZ_St_aaa"];

    // 1 !!!! hhh:(world hello) makes one posfilter and one wide hit
    if (Abs(factors[0]["_CZ_St_bbb"] - 1.0) > 0.001)
        ythrow yexception() << comment << ": invalid factor bbb CZ for first document: " << factors[0]["_CZ_St_bbb"];

    QuerySearch("hhh:(world) hhh:(hello)+bbb:(vvvbbb)&relev=formula=test_czl&dbgrlv=da&fsgta=_JsonFactors" + GetAllKps(messages), results, &resultProps);
    factors = TRTYFactorsParser::GetJsonFactorsValues(resultProps);
    if (results.size() != 1)
        ythrow yexception() << comment << ": some messages not found for 'cool less softness:100'. " << results.size() << " != " << 1;
    if (results[0].GetUrl() != messages[0].GetDocument().GetUrl())
        ythrow yexception() << comment << ": invalid result order for 'word'";

    if (Abs(factors[0]["_CZ_St_aaa"] - 1.0) > 0.001)
        ythrow yexception() << comment << ": invalid factor aaa CZ for first document" << factors[0]["_CZ_St_aaa"];

    if (Abs(factors[0]["_CZ_St_bbb"] - 2.0) > 0.001)
        ythrow yexception() << comment << ": invalid factor bbb CZ for first document: " << factors[0]["_CZ_St_bbb"];
}

bool Run() override {
    TVector<TString> texts;
    texts.push_back("<xml><hhh><aaa>world big stupid mmm cool cool hren</aaa><bbb>hello me too. its vvvbbb cool cool</bbb></hhh></xml>");
    texts.push_back("<xml><aaa>hello world</aaa><bbb>world is cool</bbb></xml>");
    texts.push_back("vasya was here");
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, texts.size(), NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    for (size_t i = 0; i < texts.size(); ++i) {
        if (i < 2)
            messages[i].MutableDocument()->SetMimeType("text/xml");
        else
            messages[i].MutableDocument()->SetMimeType("text/plain");
        messages[i].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());
        messages[i].MutableDocument()->SetBody(texts[i]);
    }
    IndexMessages(messages, REALTIME, 1);
    Check("memory", messages);
    ReopenIndexers();
    Check("disk", messages);
    TestDynamicFactorsParent::CheckMakeup();
    return true;
}

bool InitConfig() override {
    (*ConfigDiff)["Indexer.Common.XmlParserConfigFile"] = "";
    (*ConfigDiff)["Searcher.FiltrationModel"] = "SIMPLE";
    (*ConfigDiff)["Searcher.FactorsInfo"] = GetResourcesDirectory() + "/factors/zone_factors.cfg";
    return true;
}
};

START_TEST_DEFINE_PARENT(TestZoneFactorsZL, TestDynamicFactorsParent)
void Check(const char* comment, const TVector<NRTYServer::TMessage>& messages) {
    TVector<TDocSearchInfo> results;
    TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > > resultProps;
    QuerySearch("url:\"*\"&relev=formula=test_zl&dbgrlv=da&fsgta=_JsonFactors" + GetAllKps(messages), results, &resultProps);
    if (results.size() != messages.size())
        ythrow yexception() << comment << ": check count1 failed. " << results.size() << " != " << 2;
    QuerySearch("vasya&relev=formula=test_zl&dbgrlv=da&fsgta=_JsonFactors" + GetAllKps(messages), results, &resultProps);
    if (results.size() != 1)
        ythrow yexception() << comment << ": check count_vasya failed. " << results.size() << " != " << 1;
    QuerySearch("hello&relev=formula=test_zl&dbgrlv=da&fsgta=_JsonFactors" + GetAllKps(messages), results, &resultProps);
    if (results.size() != 2)
        ythrow yexception() << comment << ": check count2 failed. " << results.size() << " != " << 2;
    QuerySearch("world&relev=formula=test_zl&dbgrlv=da&fsgta=_JsonFactors" + GetAllKps(messages), results, &resultProps);
    if (results.size() != 2)
        ythrow yexception() << comment << ": check count3 failed. " << results.size() << " != " << 2;
    QuerySearch("hello world&relev=formula=test_zl&dbgrlv=da&fsgta=_JsonFactors" + GetAllKps(messages), results, &resultProps);
    TVector<THashMap<TString, double> > factors = TRTYFactorsParser::GetJsonFactorsValues(resultProps);
    if (results.size() != 2)
        ythrow yexception() << comment << ": some messages not found for 'hello world'. " << results.size() << " != " << 2;
    if (results[0].GetUrl() != messages[0].GetDocument().GetUrl())
        ythrow yexception() << comment << ": invalid result order for 'word'";
    if (Abs(factors[0]["_ZL_aaa"] - 7) > 0.001)
        ythrow yexception() << comment << ": invalid factor aaa " << factors[0]["_ZL_aaa"] << " for first document";
    if (Abs(factors[0]["_ZL_bbb"] - 6) > 0.001)
        ythrow yexception() << comment << ": invalid factor bbb " << factors[0]["_ZL_bbb"] << " for first document";
    if (Abs(factors[1]["_ZL_aaa"] - 2) > 0.001)
        ythrow yexception() << comment << ": invalid factor aaa " << factors[1]["_ZL_aaa"] << " for second document";
    if (Abs(factors[1]["_ZL_bbb"] - 3) > 0.001)
        ythrow yexception() << comment << ": invalid factor bbb " << factors[1]["_ZL_bbb"] << " for second document";


    QuerySearch("cool less softness:100&relev=formula=test_zl&dbgrlv=da&fsgta=_JsonFactors" + GetAllKps(messages), results, &resultProps);
    factors = TRTYFactorsParser::GetJsonFactorsValues(resultProps);
    if (results.size() != 2)
        ythrow yexception() << comment << ": some messages not found for 'cool less softness:100'. " << results.size() << " != " << 2;
    if (results[0].GetUrl() != messages[0].GetDocument().GetUrl())
        ythrow yexception() << comment << ": invalid result order for 'word'";
    if (Abs(factors[0]["_ZL_aaa"] - 7) > 0.001)
        ythrow yexception() << comment << ": invalid factor aaa for first document";
    if (Abs(factors[0]["_ZL_bbb"] - 6) > 0.001)
        ythrow yexception() << comment << ": invalid factor bbb for first document";
    if (Abs(factors[1]["_ZL_aaa"] - 2) > 0.001)
        ythrow yexception() << comment << ": invalid factor aaa for second document";
    if (Abs(factors[1]["_ZL_bbb"] - 3) > 0.001)
        ythrow yexception() << comment << ": invalid factor bbb for second document";
}

bool Run() override {
    TVector<TString> texts;
    texts.push_back("<xml><aaa>world big stupid mmm cool cool hren</aaa><bbb>hello me too. its cool cool</bbb></xml>");
    texts.push_back("<xml><aaa>hello world</aaa><bbb>world is cool</bbb></xml>");
    texts.push_back("vasya was here");
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, texts.size(), NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    for (size_t i = 0; i < texts.size(); ++i) {
        if (i < 2)
            messages[i].MutableDocument()->SetMimeType("text/xml");
        else
            messages[i].MutableDocument()->SetMimeType("text/plain");
        messages[i].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());
        messages[i].MutableDocument()->SetBody(texts[i]);
    }
    IndexMessages(messages, REALTIME, 1);
    Check("memory", messages);
    ReopenIndexers();
    Check("disk", messages);
    TestDynamicFactorsParent::CheckMakeup();
    return true;
}

bool InitConfig() override {
    (*ConfigDiff)["Indexer.Common.XmlParserConfigFile"] = "";
    (*ConfigDiff)["Searcher.FiltrationModel"] = "SIMPLE";
    (*ConfigDiff)["Searcher.FactorsInfo"] = GetResourcesDirectory() + "/factors/zone_factors.cfg";
    return true;
}
};

START_TEST_DEFINE_PARENT(TestRelevFormulaWithMatrixNet, TestDynamicFactorsParent)
bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), "a");
    for (auto&& m : messages) {
        m.MutableDocument()->SetKeyPrefix(GetIsPrefixed() ? 1 : 0);
    }
    IndexMessages(messages, REALTIME, 1);

    TVector<TDocSearchInfo> results;
    TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > > resultProps;
    const TString kps = GetAllKps(messages);
    TString textSearch = "a b c softness:100";
    Quote(textSearch);

    QuerySearch(textSearch + kps, results, &resultProps, nullptr, true);
    CHECK_TEST_EQ(results.size(), 1);
    ui64 rlvSource = results[0].GetRelevance();
    DEBUG_LOG << "Source relevance: " << rlvSource << Endl;

    QuerySearch(textSearch + "&relev=formula=L00300E0040000000SF0000GV10000U7&" + kps, results, &resultProps, nullptr, true);
    CHECK_TEST_EQ(results.size(), 1);
    ui64 rlvFormula = results[0].GetRelevance();
    DEBUG_LOG << "Formula relevance: " << rlvFormula << Endl;

    QuerySearch(textSearch + "&relev=formula=L00300E00K5080000000OV00000V30000SF0000GV1&" + kps, results, &resultProps, nullptr, true);
    CHECK_TEST_EQ(results.size(), 1);
    ui64 rlvMxNet = results[0].GetRelevance();
    DEBUG_LOG << "Relevance with MatrixNet: " << rlvMxNet << Endl;

    QuerySearch(textSearch + "&relev=formula=L00300E00K5080000000OV00000V30000SF0000GV1;base_rank_model=default&" + kps, results, &resultProps, nullptr, true);
    CHECK_TEST_EQ(results.size(), 1);
    ui64 rlvMxNetRebase = results[0].GetRelevance();
    DEBUG_LOG << "Relevance with MatrixNet (base_formula=default): " << rlvMxNetRebase << Endl;

    CHECK_TEST_NEQ(rlvSource, rlvFormula);
    CHECK_TEST_NEQ(rlvFormula, rlvMxNet);
    CHECK_TEST_EQ(rlvMxNet, rlvMxNetRebase);

    CHECK_TEST_EQ(rlvSource, 103387912);
    CHECK_TEST_EQ(rlvFormula, 104396320);
    CHECK_TEST_EQ(rlvMxNet, 109854176);

    return true;
}

bool InitConfig() override {
    (*ConfigDiff)["Searcher.FiltrationModel"] = "SIMPLE";
    (*ConfigDiff)["Searcher.FactorsInfo"] = GetResourcesDirectory() + "/text_relev/text_factors.cfg";
    return true;
}
};

START_TEST_DEFINE_PARENT(TestRelevFormulaCheckMatrixNet, TestDynamicFactorsParent)
bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 3, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    messages[0].MutableDocument()->SetBody("a");
    messages[1].MutableDocument()->SetBody("a b");
    messages[2].MutableDocument()->SetBody("a something c");
    IndexMessages(messages, REALTIME, 1);

    TString textSearch = "a b c softness:100";
    Quote(textSearch);
    textSearch += GetAllKps(messages);

    TVector<TDocSearchInfo> resultsA;
    TVector<TDocSearchInfo> resultsB;
    QuerySearch(textSearch, resultsA, nullptr, nullptr, true);
    QuerySearch(textSearch + "&relev=formula=L00D1020000000U7AOL72U0", resultsB, nullptr, nullptr, true);

    CHECK_TEST_EQ(resultsA.size(), resultsB.size());
    for (size_t i = 0; i < resultsA.size(); i++) {
        CHECK_TEST_EQ(resultsA[i].GetUrl(), resultsB[i].GetUrl());
        CHECK_TEST_EQ(resultsA[i].GetRelevance(), resultsB[i].GetRelevance());
    }

    return true;
}
bool InitConfig() override {
    (*ConfigDiff)["Searcher.FiltrationModel"] = "SIMPLE";
    (*ConfigDiff)["Searcher.FactorsInfo"] = GetResourcesDirectory() + "/text_relev/text_factors.cfg";
    return true;
}
};

START_TEST_DEFINE_PARENT(TestRelevMatrixNetCache, TestDynamicFactorsParent)
bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 3, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    messages[0].MutableDocument()->SetBody("a");
    messages[1].MutableDocument()->SetBody("a b");
    messages[2].MutableDocument()->SetBody("a something c");
    IndexMessages(messages, REALTIME, 1);

    TString textSearch = "a b c softness:100 <- b";
    Quote(textSearch);
    textSearch += GetAllKps(messages);

    TVector<TDocSearchInfo> resultsA;
    TVector<TDocSearchInfo> resultsB;
    QuerySearch(textSearch + "&relev=filter_border=0.1&gta=MatrixNet&relev=u=" + messages[0].GetDocument().GetUrl(), resultsA, nullptr, nullptr, true);
    QuerySearch(textSearch + "&relev=filter=L00300E0040000000SF0000GV10000U7&relev=filter_border=0.1&gta=MatrixNet", resultsB, nullptr, nullptr, true);

    CHECK_TEST_EQ(resultsA.size(), 3);
    CHECK_TEST_EQ(resultsA.size(), resultsB.size());

 /*   for (size_t i = 0; i < resultsA.size(); i++) {
        CHECK_TEST_EQ(resultsA[i].GetUrl(), resultsB[i].GetUrl());
        CHECK_TEST_EQ(resultsA[i].GetRelevance(), resultsB[i].GetRelevance());
    }*/

    return true;
}
bool InitConfig() override {
    (*ConfigDiff)["Searcher.FiltrationModel"] = "SIMPLE";
    (*ConfigDiff)["Searcher.FactorsInfo"] = GetResourcesDirectory() + "/text_relev/text_factors.cfg";
    return true;
}
};

START_TEST_DEFINE_PARENT(TestTRTitle, TTestDynamicFactorsCaseClass)
bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    messages[0].MutableDocument()->ClearBody();
    auto zone = messages[0].MutableDocument()->MutableRootZone()->AddChildren();
    zone->SetName("text");
    zone->SetText("abc");
    zone = zone->AddChildren();
    zone->SetName("title");
    zone->SetText("text");

    IndexMessages(messages, REALTIME, 1);
    ReopenIndexers();
    GetFactorsImpl("title:(text)", GetAllKps(messages));
    CheckFactorNotNull("TRtitle");
    return true;
}
bool InitConfig() override {
    (*ConfigDiff)["Searcher.FiltrationModel"] = "SIMPLE";
    (*ConfigDiff)["Searcher.FactorsInfo"] = GetResourcesDirectory() + "/text_relev/trtitle_factors.cfg";
    return true;
}
};
