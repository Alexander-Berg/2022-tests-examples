#include <saas/api/search_client/client.h>
#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/api/factors_erf.h>
#include <saas/rtyserver_test/util/factors_parsers.h>
#include <search/meta/doc_bin_data/ranking_factors_accessor.h>

#include <library/cpp/string_utils/base64/base64.h>
#include <util/digest/city.h>
#include <util/generic/ymath.h>
#include <util/stream/fwd.h>

START_TEST_DEFINE(TestGta)
bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 1, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    NRTYServer::TAttribute& attr = *messages[0].MutableDocument()->AddGroupAttributes();
    attr.set_name("g_abc");
    attr.set_value("123");
    attr.set_type(NRTYServer::TAttribute::INTEGER_ATTRIBUTE);
/*
    //  case for group and prop with same name, broken now
    NRTYServer::TAttribute& attr1 = *messages[0].MutableDocument()->AddGroupAttributes();
    attr1.set_name("p_abc");
    attr1.set_value("lit123");
    attr1.set_type(NRTYServer::TAttribute::LITERAL_ATTRIBUTE);*/

    NRTYServer::TMessage::TDocument::TProperty& prop = *messages[0].MutableDocument()->AddDocumentProperties();
    prop.set_name("p_abc");
    prop.set_value("prop");
    IndexMessages(messages, REALTIME, 1);
    TVector<TDocSearchInfo> results;
    TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > > docProps;
    const TString kps = GetAllKps(messages);
    QuerySearch("body&gta=g_abc" + kps, results, &docProps);
    if (docProps.size() != 1)
        ythrow yexception() << "documents count too small";
    THashMultiMap<TString, TString>::const_iterator i = docProps[0]->find("g_abc");
    if (i == docProps[0]->end() || i->second != "123") {
        ythrow yexception() << "I do not see group attr value";
    }

    QuerySearch("body&gta=p_abc" + kps, results, &docProps);
    if (docProps.size() != 1)
        ythrow yexception() << "documents count too small";
    i = docProps[0]->find("p_abc");
    if (i == docProps[0]->end() || i->second != "prop")
        ythrow yexception() << "I do not see doc prop value";

    messages[0].MutableDocument()->clear_groupattributes();
    IndexMessages(messages, REALTIME, 1);

    Controller->QueryPrint("body&gta=p_abc" + kps);
    QuerySearch("body&gta=p_abc" + kps, results, &docProps);
    if (docProps.size() != 1)
        ythrow yexception() << "documents count too small";
    i = docProps[0]->find("p_abc");
    if (i == docProps[0]->end() || i->second != "prop")
        ythrow yexception() << "I do not see doc prop value";

    QuerySearch("body&gta=g_abc" + kps, results, &docProps);
    if (docProps.size() != 1)
        ythrow yexception() << "documents count too small";
    i = docProps[0]->find("g_abc");
    if (i != docProps[0]->end()) {
        ythrow yexception() << "Incorrect g_abc attribute";
    }
    return true;
}
};

START_TEST_DEFINE(TestGtaDynamicFactorNotInFormula)
bool InitConfig() override {
    (*ConfigDiff)["Searcher.FactorsInfo"] = WriteConfigFile<EConfigType::Factors>(CreateFactorsConfigTemplate());
    (*ConfigDiff)["Components"] = INDEX_COMPONENT_NAME "," FULL_ARCHIVE_COMPONENT_NAME;
    (*ConfigDiff)["ComponentsConfig." FULL_ARCHIVE_COMPONENT_NAME ".ActiveLayers"] = "base,full";
    (*ConfigDiff)["Indexer.Memory.Enabled"] = "false";
    return true;
}

 NJson::TJsonValue CreateFactorsConfigTemplate() {
    static const TString configBody = R"({
            "dynamic_factors": {
                "MatrixNet" : 2,
                "TextBM25" : 3,
                "FullMatrixNet" : 4,
                "FastMatrixNet" : 5,
                "FullPolynom" : 6,
                "FastPolynom" : 7,
                "FilterMatrixNet" : 8,
                "FilterPolynom" : {
                    "index": 9,
                    "default_value": 179
                }
            },
            "user_factors": {
                "user1": {
                    "index": 0,
                    "default_value": 42.0
                },
                "user2": {
                    "index": 1,
                    "default_value": 2.0
                }
            },
            "formulas": {
                "default": {
                    "polynom": "1005002000000G480000Q01"
                },
                "fast_rank": {
                    "polynom": "10010000000V3"
                },
                "all_factors": {
                    "polynom": "100500I00400000009G0000K120000U7"
                }
            }
        })";
    NJson::TJsonValue result;
    Cerr << configBody << Endl;
    NJson::ReadJsonTree(configBody, &result, true /*throwOnError*/);
    return result;
}

bool CheckBM25InFactorMapping(const TString& searchResult, const TString& comment) {
    Cerr << comment << Endl;
    NMetaProtocol::TReport report;
    ::google::protobuf::io::CodedInputStream decoder((ui8*)searchResult.data(), searchResult.size());
    decoder.SetTotalBytesLimit(1 << 29);
    Y_ENSURE(report.ParseFromCodedStream(&decoder) && decoder.ConsumedEntireMessage());

    CHECK_TEST_EQ(report.GetGrouping().size(), 1);
    const NMetaProtocol::TGrouping& grouping(report.GetGrouping(0));
    CHECK_TEST_EQ(grouping.GetGroup().size(), 1);
    NMetaProtocol::TDocument resultDocument = grouping.GetGroup(0).GetDocument(0);
    
    CHECK_TEST_EQ(report.GetHead().GetFactorMapping().size(), 1);
    const NMetaProtocol::TFactorMapping& factorMapping = report.GetHead().GetFactorMapping(0);
    CHECK_TEST_EQ(factorMapping.GetKey(), "TextBM25");
    CHECK_TEST_EQ(factorMapping.GetValue(), 0);

    const NMetaProtocol::TPairIntFloat& binFactor = resultDocument.GetBinFactor(0);
    CHECK_TEST_EQ(binFactor.GetKey(), 0);
    CHECK_TEST_EQ(binFactor.GetValue(), 0.5);
    Cerr << comment << " -- Ok" << Endl;
    return true;
}

bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    IndexMessages(messages, DISK, 1);
    ReopenIndexers();
    TString searchResult;
    const TString kps = GetAllKps(messages);
    CHECK_TEST_EQ(ProcessQuery("/?text=body&dump=eventlog&ms=proto&service=tests&gta=TextBM25&pron=fastcount10&relev=formula=default&relev=fast_polynom=100500I00400000009G0000K120000U7" + kps, &searchResult), 200);
    CHECK_TEST_TRUE(CheckBM25InFactorMapping(searchResult, "factor is used only in fastrank formula"));
    CHECK_TEST_EQ(ProcessQuery("/?text=body&dump=eventlog&ms=proto&service=tests&gta=TextBM25&pron=fastcount10" + kps, &searchResult), 200);
    CHECK_TEST_TRUE(CheckBM25InFactorMapping(searchResult, "factor is not used anywhere"));
    CHECK_TEST_EQ(ProcessQuery("/?text=body&dump=eventlog&ms=proto&service=tests&gta=TextBM25&pron=fastcount10&relev=formula=default&relev=polynom=100500I00400000009G0000K120000U7" + kps, &searchResult), 200);
    CHECK_TEST_TRUE(CheckBM25InFactorMapping(searchResult, "factor is used only in fullrank formula"));

    return true;
}
};

START_TEST_DEFINE(TestFacetAndBan)
protected:
void Check(const TString& comment, bool facetsfb, const TVector<NRTYServer::TMessage>& messages) {
    TVector<TDocSearchInfo> results;
    THashMultiMap<TString, TString> searchProperties;
    const TString kps(GetAllKps(messages));
    const TString opt = facetsfb ? "&facetsfb=1" : "";
    THashMultiMap<TString, TString>::const_iterator i;
    QuerySearch("body&borders=ValueProp1&qi=rty_max_ValueProp1&qi=rty_min_ValueProp1&sums=ValueProp1&qi=rty_sum_ValueProp1&facets=SomeProp1&qi=facet_SomeProp1" + kps + opt, results, nullptr, &searchProperties);
    i = searchProperties.find("rty_min_ValueProp1");
    if (i != searchProperties.end())
        ythrow yexception() << comment << ": rty_min_ValueProp1 is incorrect";
    results.clear();
    searchProperties.clear();
    QuerySearch("body&borders=ValueProp&rty_hits_count=da&rty_hits_detail=da&qi=rty_hits_count&qi=rty_hits_count_full&qi=rty_hits_count_sents&qi=rty_max_ValueProp&qi=rty_min_ValueProp&sums=ValueProp&qi=rty_sum_ValueProp&facets=SomeProp&qi=facet_SomeProp" + kps + opt, results, nullptr, &searchProperties);
    if (results.size() != messages.size())
        ythrow yexception() << comment << ": incorrect count: " << results.size() << " != " << messages.size();
    i = searchProperties.find("facet_SomeProp");
    if (i == searchProperties.end())
        ythrow yexception() << comment << ": facet_SomeProp not found";
    if (i->second != "0:3;1:2;2:1")
        ythrow yexception() << comment << ": facet_SomeProp has incorrect value: '" << i->second <<"' != '100:1;101:1;102:1'";

    i = searchProperties.find("sum--facet_SomeProp");
    if (i == searchProperties.end())
        ythrow yexception() << comment << ": facet_SomeProp--sum not found";
    if (i->second != "6")
        ythrow yexception() << comment << ": facet_SomeProp--sum has incorrect value: '" << i->second << "' != '6'";

    i = searchProperties.find("rty_hits_count");
    if (i == searchProperties.end())
        ythrow yexception() << comment << ": rty_hits_count not found";
    if (i->second != "3")
        ythrow yexception() << comment << ": rty_hits_count has incorrect value: '" << i->second << "' != '3'";

    i = searchProperties.find("rty_hits_count_full");
    if (i == searchProperties.end())
        ythrow yexception() << comment << ": rty_hits_count_full not found";
    if (i->second != "9")
        ythrow yexception() << comment << ": rty_hits_count_full has incorrect value: '" << i->second << "' != '9'";

    i = searchProperties.find("rty_hits_count_sents");
    if (i == searchProperties.end())
        ythrow yexception() << comment << ": rty_hits_count_sents not found";
    if (i->second != "3")
        ythrow yexception() << comment << ": rty_hits_count_sents has incorrect value: '" << i->second << "' != '3'";

    i = searchProperties.find("rty_sum_ValueProp");
    if (i == searchProperties.end())
        ythrow yexception() << comment << ": rty_sum_ValueProp not found";
    if (i->second != "303")
        ythrow yexception() << comment << ": rty_sum_ValueProp has incorrect value: '" << i->second <<"' != '303'";

    i = searchProperties.find("rty_min_ValueProp");
    if (i == searchProperties.end())
        ythrow yexception() << comment << ": rty_min_ValueProp not found";
    if (i->second != "100")
        ythrow yexception() << comment << ": rty_min_ValueProp has incorrect value: '" << i->second <<"' != '100'";

    i = searchProperties.find("rty_max_ValueProp");
    if (i == searchProperties.end())
        ythrow yexception() << comment << ": rty_max_ValueProp not found";
    if (i->second != "102")
        ythrow yexception() << comment << ": rty_max_ValueProp has incorrect value: '" << i->second <<"' != '102'";

    QuerySearch("body&facets=SomeProp&rty_hits_count=da&qi=rty_hits_count&qi=facet_SomeProp&bu=" + messages[0].GetDocument().GetUrl() + kps + opt, results, nullptr, &searchProperties);
    if (results.size() != messages.size() - 1)
        ythrow yexception() << comment << ": bu - incorrect count: " << results.size() << " != " << messages.size() - 1;
    for (size_t r = 0; r < results.size(); ++r)
        if (results[r].GetUrl() == messages[0].GetDocument().GetUrl())
            ythrow yexception() << comment << ": bu - invalid url: " << results[r].GetUrl();
    i = searchProperties.find("facet_SomeProp");
    if (i == searchProperties.end())
        ythrow yexception() << comment << ": facet_SomeProp not found";
    if (i->second != "0:2;1:2;2:1")
        ythrow yexception() << comment << ": facet_SomeProp has incorrect value: '" << i->second <<"' != '0:2;1:2;2:1'";

    i = searchProperties.find("rty_hits_count");
    if (i == searchProperties.end())
        ythrow yexception() << comment << ": rty_hits_count not found";
    if (i->second != "2")
        ythrow yexception() << comment << ": rty_hits_count has incorrect value: '" << i->second << "' != '3'";

    i = searchProperties.find("sum--facet_SomeProp");
    if (i == searchProperties.end())
        ythrow yexception() << comment << ": facet_SomeProp--sum not found";
    if (i->second != "5")
        ythrow yexception() << comment << ": facet_SomeProp--sum has incorrect value: '" << i->second << "' != '5'";

    QuerySearch("body&au=" + messages[0].GetDocument().GetUrl() + kps + opt, results);
    if (results.size() != 1)
        ythrow yexception() << comment << ": au - incorrect count: " << results.size() << " != 1";
    for (size_t r = 0; r < results.size(); ++r)
        if (results[r].GetUrl() != messages[0].GetDocument().GetUrl())
            ythrow yexception() << comment << ": au - invalid url: " << results[r].GetUrl();

    QuerySearch("body&au=" + messages[0].GetDocument().GetUrl() + "," + messages[1].GetDocument().GetUrl() + "&bu=" + messages[0].GetDocument().GetUrl() + kps + opt, results);
    if (results.size() != 1)
        ythrow yexception() << comment << ": au&bu - incorrect count: " << results.size() << " != 1";
    for (size_t r = 0; r < results.size(); ++r)
        if (results[r].GetUrl() != messages[1].GetDocument().GetUrl())
            ythrow yexception() << comment << ": au&bu - invalid url: " << results[r].GetUrl();

}

void GenMessages(TVector<NRTYServer::TMessage>& messages, int count) {
    GenerateInput(messages, count, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), "body body body");
    for (int i = 0; i < count; ++i) {
        for (int j = 0; j <= i; ++j) {
            NRTYServer::TMessage::TDocument::TProperty& prop = *messages[i].MutableDocument()->AddDocumentProperties();
            prop.SetName("SomeProp");
            prop.SetValue(ToString(j));
        }
        NRTYServer::TAttribute& attr = *messages[i].MutableDocument()->AddGroupAttributes();
        attr.SetName("ValueProp");
        attr.SetValue(ToString(i + 100));
        attr.SetType(NRTYServer::TAttribute::INTEGER_ATTRIBUTE);
    }
}

public:
bool Run() override {
    const int countMessages = 3;
    TVector<NRTYServer::TMessage> messages;
    GenMessages(messages, countMessages);

    IndexMessages(messages, REALTIME, 1);
    Check("mem", /*facetsfb=*/false, messages);
    Check("mem", /*facetsfb=*/true, messages);
    ReopenIndexers();
    Check("disk", /*facetsfb=*/false, messages);
    Check("disk", /*facetsfb=*/true, messages);
    return true;
}

bool InitConfig() override {
    (*ConfigDiff)["ComponentsConfig.FASTARC.FastProperties"] = "SomeProp";
    return true;
}
};

START_TEST_DEFINE_PARENT(TestFacetAndBanCache, TTestFacetAndBanCaseClass)
    bool Run() override {
        const int countMessages = 3;
        TVector<NRTYServer::TMessage> messages;
        GenMessages(messages, countMessages);
        IndexMessages(messages, REALTIME, 1);
        ReopenIndexers();
        Check("first", /*facetsfb=*/false, messages);
        Check("first", /*facetsfb=*/true, messages);

        TSet<std::pair<ui64, TString> > deleted;
        DeleteSomeMessages(messages, deleted, REALTIME, 1);

        Check("second", /*facetsfb=*/false, messages);
        Check("second", /*facetsfb=*/true, messages);
        return true;
    }
    bool InitConfig() override {
        SetSearcherParams(abTRUE, "10000s");
        SetMergerParams(false, 2, -1, mcpNONE);
        return true;
    }
};

START_TEST_DEFINE(TestSelectFormula)
void Check(const TString& comment, const TVector<NRTYServer::TMessage>& messages) {
    TVector<TDocSearchInfo> results;
    THashMultiMap<TString, TString> searchProps;
    const TString kps(GetAllKps(messages));
    QuerySearch("body" + kps, results, nullptr, &searchProps, true);
    if (results.size() != messages.size())
        ythrow yexception() << comment << ": incorrect count: " << results.size() << " != " << messages.size();
    if (results[0].GetUrl() != messages[0].GetDocument().GetUrl())
        ythrow yexception() << comment << ": incorrect order for default formula";
    if (searchProps.find("RankingModel")->second != "default")
        ythrow yexception() << "incorrect RankingModel search property " << searchProps.find("RankingModel")->second;

    QuerySearch("body&relev=formula=vasya" + kps, results, nullptr, &searchProps, true);
    if (results.size() != messages.size())
        ythrow yexception() << comment << ": incorrect count: " << results.size() << " != " << messages.size();
    if (results[0].GetUrl() != messages[0].GetDocument().GetUrl())
        ythrow yexception() << comment << ": incorrect order for default formula";

    QuerySearch("body&relev=formula=alternative" + kps, results, nullptr, &searchProps, true);
    if (results.size() != messages.size())
        ythrow yexception() << comment << ": incorrect count: " << results.size() << " != " << messages.size();
    if (results[0].GetUrl() != messages.back().GetDocument().GetUrl())
        ythrow yexception() << comment << ": incorrect order for alternative formula";
    if (searchProps.find("RankingModel")->second != "alternative")
        ythrow yexception() << "incorrect RankingModel search property " << searchProps.find("RankingModel")->second;

    QuerySearch("body&relev=formula=D9010000000V3" + kps, results, nullptr, &searchProps); //300 factor
    if (results.size() != messages.size())
        ythrow yexception() << comment << ": incorrect count: " << results.size() << " != " << messages.size();
    if (results[0].GetUrl() != messages[0].GetDocument().GetUrl())
        ythrow yexception() << comment << ": incorrect order for direct formula";

    QuerySearch("body&relev=formula=L0010000000V3" + kps, results, nullptr, &searchProps); //20 factor
    if (results.size() != messages.size())
        ythrow yexception() << comment << ": incorrect count: " << results.size() << " != " << messages.size();
    if (results[0].GetUrl() != messages[0].GetDocument().GetUrl())
        ythrow yexception() << comment << ": incorrect order for direct formula";

    QuerySearch("body&relev=formula=80010000000V3" + kps, results, nullptr, &searchProps); //7 factor
    if (results.size() != messages.size())
        ythrow yexception() << comment << ": incorrect count: " << results.size() << " != " << messages.size();
    if (results[0].GetUrl() != messages.back().GetDocument().GetUrl())
        ythrow yexception() << comment << ": incorrect order for direct formula";

    QuerySearch("body&relev=formula=30010000000V3" + kps, results, nullptr, &searchProps, true); //2 factor
    if (results.size() != messages.size())
        ythrow yexception() << comment << ": incorrect count: " << results.size() << " != " << messages.size();
    if (results[0].GetUrl() != messages[0].GetDocument().GetUrl())
        ythrow yexception() << comment << ": incorrect order for direct formula";
    if (searchProps.find("RankingModel")->second != "30010000000V3")
        ythrow yexception() << "incorrect RankingModel search property " << searchProps.find("RankingModel")->second;

    QuerySearch("body&relev=formula=alternative;set=user1:stat3" + kps, results, nullptr, &searchProps);
    if (results.size() != messages.size())
        ythrow yexception() << comment << ": incorrect count: " << results.size() << " != " << messages.size();
    if (results[0].GetUrl() != messages[0].GetDocument().GetUrl())
        ythrow yexception() << comment << ": incorrect order for alternative formula with user factor";
    QuerySearch("body&relev=formula=alternative;calc=user1:no(inset(#stat3, inset(#stat3, 2, 1)))" + kps, results, nullptr, &searchProps);
    if (results.size() != messages.size())
        ythrow yexception() << comment << ": incorrect count: " << results.size() << " != " << messages.size();
    if (results[0].GetUrl() != messages[0].GetDocument().GetUrl())
        ythrow yexception() << comment << ": incorrect order for alternative formula with user factor";

}

void GenMessages(TVector<NRTYServer::TMessage>& messages, int count) {
    GenerateInput(messages, count, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    for (int i = 0; i < count; ++i) {
        NSaas::AddSimpleFactor("stat1", ToString(i + 1), *messages[i].MutableDocument()->MutableFactors());
        NSaas::AddSimpleFactor("stat2", "0", *messages[i].MutableDocument()->MutableFactors());
        NSaas::AddSimpleFactor("stat3", ToString(count - i), *messages[i].MutableDocument()->MutableFactors());
    }
}

bool Run() override {
    const int countMessages = 2;
    TVector<NRTYServer::TMessage> messages;
    GenMessages(messages, countMessages);

    IndexMessages(messages, REALTIME, 1);
    Check("mem", messages);
    ReopenIndexers();
    Check("disk", messages);
    return true;
}
bool InitConfig() override {
    (*ConfigDiff)["Searcher.FactorsInfo"] = FactorsFileName;
    return true;
}
};

namespace {
    const TString Prop1 = "prop1";
    const TString Prop2 = "prop2";
    const TString Prop3 = "prop3";

    const TString Find1 = "hello";
    const TString Find2 = "world";
    const TString FindBoth = Find1 + " " + Find2;
    const TString FindAll = "url:\"*\"";

    template <class T>
    inline void AddProp(NRTYServer::TMessage& message, const TString& name, const T& value) {
        NRTYServer::TMessage::TDocument::TProperty& prop = *message.MutableDocument()->AddDocumentProperties();
        prop.SetName(name);
        prop.SetValue(ToString(value));
    }
}

SERVICE_TEST_RTYSERVER_DEFINE(FacetsAll)
TString GetKps() {
    int kps = GetIsPrefixed() ? 1 : 0;
    return "&kps=" + ToString(kps);
}
TVector<NRTYServer::TMessage> GenerateMessages() {
    TVector<NRTYServer::TMessage> messages;
    int count = 30;
    GenerateInput(messages, count, NRTYServer::TMessage::ADD_DOCUMENT, false);
    if (GetIsPrefixed())
        for (int i = 0; i < count; ++i) {
            messages[i].MutableDocument()->SetKeyPrefix(1);
        }
    int i = 0;
    for (; i < count / 3; ++i) {
        messages[i].MutableDocument()->SetBody(Find1);
        AddProp(messages[i], Prop1, ToString(i));
        AddProp(messages[i], Prop3, "stable_prop");
    }
    for (; i < 2 * (count / 3); ++i) {
        messages[i].MutableDocument()->SetBody(Find2);
        AddProp(messages[i], Prop2, ToString(i));
        AddProp(messages[i], Prop3, "stable_prop");
    }
    for (; i < count; ++i) {
        messages[i].MutableDocument()->SetBody(FindBoth);
        AddProp(messages[i], Prop1, ToString(i));
        AddProp(messages[i], Prop2, ToString(i));
        AddProp(messages[i], Prop3, "stable_prop");
    }
    return messages;
}
int GetFacetCount(const THashMultiMap<TString, TString>& searchProps) {
    int result = 0;
    for (THashMultiMap<TString, TString>::const_iterator i = searchProps.begin(); i != searchProps.end(); ++i) {
        if (i->first.StartsWith("facet_"))
            ++result;
    }
    return result;
}
TString GetRTYAllFacetsString(const THashMultiMap<TString, TString>& searchProps) {
    THashMultiMap<TString, TString>::const_iterator i = searchProps.find("rty_allfacets");
    if (i == searchProps.end())
        ythrow yexception() << "cant find rty_allfacets search prop";
    return i->second;
}
bool Run() override{
    const TVector<NRTYServer::TMessage> messages = GenerateMessages();
    IndexMessages(messages, REALTIME, 1);
    CheckResults();
    return true;
}

void CheckSingle(const TString & query, int expFacetCount, const TString& expFacetString) {
    TVector<TDocSearchInfo> results;
    THashMultiMap<TString, TString> searchProperties;
    QuerySearch(query, results, nullptr, &searchProperties);
    int fc = GetFacetCount(searchProperties);
    TString fs = GetRTYAllFacetsString(searchProperties);
    if (fc != expFacetCount)
        ythrow yexception() << "wrong facets count: expected " << expFacetCount << ", got " << fc;
    if (fs != expFacetString)
        ythrow yexception() << "wrong facets string: expected " << expFacetString << ", got " << fs;
}

virtual void CheckResults() {}

bool InitConfig() override {
    (*ConfigDiff)["ComponentsConfig.FASTARC.FastProperties"] = Prop2;
    return true;
}

};

START_TEST_DEFINE_PARENT(TestFacetsAll, FacetsAll)
void CheckResults() override {
    for (TString facetsfb: {"0", "1"}) {
        const TString facetCgi = "&facets=all&nofacet=Language2,prefix&qi=rty_allfacets&facetprefix=prop&numdoc=1000&facetsfb=" + facetsfb + GetKps();
        CheckSingle(Find1 + facetCgi, 3, "prop1,prop2,prop3");
        CheckSingle(Find2 + facetCgi, 3, "prop1,prop2,prop3");
        CheckSingle(FindBoth + facetCgi, 3, "prop1,prop2,prop3");
        CheckSingle(FindAll + facetCgi, 3, "prop1,prop2,prop3");
    }
}
};

START_TEST_DEFINE_PARENT(TestFacetsSemicolon, FacetsAll)
bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    int count = 3;
    GenerateInput(messages, count, NRTYServer::TMessage::ADD_DOCUMENT, false);
    if (GetIsPrefixed())
    for (int i = 0; i < count; ++i) {
        messages[i].MutableDocument()->SetKeyPrefix(1);
    }
    for (int i = 0; i < count; ++i) {
        AddProp(messages[i], Prop1, ToString(i) + ":prop;fvckof");
    }
    IndexMessages(messages, REALTIME, 1);

    TVector<TDocSearchInfo> results;
    THashMultiMap<TString, TString> searchProperties;
    const TString kps = "&kps=1";
    THashMultiMap<TString, TString>::const_iterator i;
    QuerySearch("body&facets=prop1&qi=facet_prop1" + kps, results, nullptr, &searchProperties);
    if (results.size() != messages.size())
        ythrow yexception() << ": incorrect count: " << results.size() << " != " << messages.size();
    i = searchProperties.find("facet_prop1");
    if (i == searchProperties.end())
        ythrow yexception() << ": facet_prop1 not found";
    if (i->second != "0 prop fvckof:1;1 prop fvckof:1;2 prop fvckof:1")
        ythrow yexception() << ": facet_prop1 has incorrect value: '" << i->second << "' != '0 prop fvckof:1;1 prop fvckof:1;2 prop fvckof:1'";

    return true;
}
};

START_TEST_DEFINE_PARENT(TestFacetsInterleaving, FacetsAll)
void CheckResults() override {
    const TString facetCgi = "&facets=interleaving&nofacet=Language2,prefix&qi=rty_allfacets&facetprefix=prop&numdoc=1000" + GetKps();
    CheckSingle(Find1 + facetCgi, 2, "prop1,prop3");
    CheckSingle(Find1 + facetCgi + "&forcefacet=prop2", 3, "prop1,prop2,prop3");
    CheckSingle(Find2 + facetCgi, 2, "prop2,prop3");
    CheckSingle(FindBoth + facetCgi, 3, "prop1,prop2,prop3");
    CheckSingle(FindAll + facetCgi, 1, "prop3");
}
};

START_TEST_DEFINE(TestUserFactors)
void Check(const TString& comment, const TString& kps, const TString& userFactorFormula, double correctValue) {
    TVector<TDocSearchInfo> results;
    TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > > resultProps;
    QuerySearch(
        "body&dbgrlv=da&fsgta=_JsonFactors&relev=calc=user1:" + userFactorFormula + "&relev=formula=alternative&relev=relev_param%3Dabc"\
        "%3Bquery_embedding_boosting_ctr%3DAQAAAAAAAAAAAAAAGQAAAKR%2Ba4l4b5OFi0hfhFdcmK1XlZFmnKkAaWEJAAAA" + kps,
        results,
        &resultProps
    );
    if (results.size() != 1) {
        ythrow yexception() << comment << "(" << userFactorFormula << "): incorrect count: " << results.size() << " != 1";
    }
    THashMap<TString, double> factors = TRTYFactorsParser::GetJsonFactorsValues(resultProps)[0];
    THashMap<TString, double>::const_iterator i = factors.find("user1");
    if (i == factors.end())
        ythrow yexception() << "there is no user1 factor";
    if (fabs(i->second - correctValue) > 1e-6)
        ythrow yexception() << "incorrect value: " << i->second << " != " << correctValue;
}

void Check(const TString& comment, const TString& kps) {
    Check(comment, kps, "#group_mid", 2);
    Check(comment, kps, "15", 15);
    Check(comment, kps, "lt(#stat2,#stat3)", 0);
    Check(comment, kps, "lt(#stat3,#stat2)", 1);
    Check(comment, kps, "gt(#stat2,#stat3)", 1);
    Check(comment, kps, "gt(#stat3,#stat2)", 0);
    Check(comment, kps, "and(#stat2,#stat3)", 0);
    Check(comment, kps, "and(#stat2,#stat1)", 1);
    Check(comment, kps, "or(#stat2,#stat3)", 1);
    Check(comment, kps, "or(and(#stat2,#stat3),#stat3)", 0);
    Check(comment, kps, "min(#stat1,#stat2,#stat3)", 0);
    Check(comment, kps, "max(#stat1,#stat2,#stat3)", 1);
    Check(comment, kps, "#user2;set=user2:TRDocQuorum", 1);
    Check(comment, kps, "max(#user2,#stat2);calc=user2:#TRDocQuorum", 1);
    Check(comment, kps, "sum(#stat1,#stat2,#group_mid)", 3.5);
    Check(comment, kps, "mul(#stat1,#stat2)", 0.5);
    Check(comment, kps, "mul(#stat1,#stat2,#stat3)", 0);
    Check(comment, kps, "ln(#stat2)", -0.6931471f);
    Check(comment, kps, "log10(#stat2)", -0.301029995f);
    Check(comment, kps, "diff(#stat2,#stat1)", -0.5f);
    Check(comment, kps, "fnvhash_f32(10,20)", 0.791904f);
    {
        // manually calculate in order to ensue that both algorithm and argument parsing is correct
        double expectedValue = static_cast<double>(::CityHash64WithSeed(TStringBuf("a grasshopper seats \" in grass"), 20));
        expectedValue /= std::numeric_limits<ui64>::max();
        Check(comment, kps, "cityhash(\"a grasshopper seats \\\" in grass\",20)", expectedValue);
    }
    Check(comment, kps, "cityhash(get_relev(\"relev_param\"),20)", 0.834397);
    {
        double expectedValue = static_cast<double>(::CityHash64(Base64Decode("AQAAAAAAAAAAAAAAGQAAAKR+a4l4b5OFi0hfhFdcmK1XlZFmnKkAaWEJAAAA")));
        expectedValue /= std::numeric_limits<ui64>::max();
        Check(comment, kps, "cityhash(base64_decode(get_relev(\"query_embedding_boosting_ctr\")))", expectedValue);
    }
    if (GetIsPrefixed()) {
        Check(comment, kps, "fnvhash_f32(10,zdocid_i64())", 0.292515f);
    } else {
        Check(comment, kps, "fnvhash_f32(10,zdocid_i64())", 0.905095f);
    }
    Check(comment, kps, "abs(diff(-1.5,#stat1))", 2.5f);
    Check(comment, kps, "hypot(#stat1,#stat2,5,10)", 10.3078f);
    Check(comment, kps, "sigma(hypot(#stat1,#stat2,5,10),5,15)", 0.772443f);
    Check(comment, kps, "clamp(diff(#stat1,0.1),0.6,1.0)", 0.75f);

    //
    // Handling attributes with multiple values.
    //
    // A group attribute can have multiple values. Most functions will interpret such subexpression as zero,
    // but a special function - insetany() - may handle them. Check out the reference for more details:
    // https://wiki.yandex-team.ru/jandekspoisk/saas/factorsinfo/user/
    Check(comment, kps, "sum(#stat1,#stat2,#group_xyz)", 1.5); // #group_xyz is interpreted as zero in sum()
    Check(comment, kps, "inset(#group_mid, 1, 2)", 1); // inset() works fine when there is only one value
    Check(comment, kps, "inset(#group_xyz, 22, 33)", 0); // inset() does not support multiattributes
    // insetany() works both for group attributes and group multiattributes (SAASSUP-1495)
    Check(comment, kps, "insetany(#group_nonexistentattr, 2)", 0);
    Check(comment, kps, "insetany(#group_mid, 2)", 1);
    Check(comment, kps, "insetany(#group_mid, 3, 5)", 0);
    Check(comment, kps, "insetany(#group_xyz, 22, 33)", 1);
    Check(comment, kps, "insetany(#group_xyz, 22, 11)", 1);
    Check(comment, kps, "insetany(#group_xyz, 11)", 1);
    Check(comment, kps, "insetany(#group_xyz, 0.5, 11)", 1);
    Check(comment, kps, "insetany(#group_xyz, 21.99, 33)", 0);
    // However, if the subexpression is complex, the magic comes to end
    Check(comment, kps, "insetany(max(#group_xyz, 5), 5)", 1);

    Check(comment, kps, "coalesce_zero(#stat3,sum(#stat1,-1),sum(#stat2,#stat1))", 1.5);
    Check(comment, kps, "coalesce_zero(#stat3,0.0)", 0);
    Check(comment, kps, "coalesce_zero(#stat3,-1.0)", -1);
}

void GenMessages(TVector<NRTYServer::TMessage>& messages) {
    TAttrMap::value_type map;
    map["mid"] = 2;

    GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(1, map));
    auto* document = messages[0].MutableDocument();
    document->SetUrl(WideToUTF8(UTF8ToWide("http://ПаНаМар.cOm/bla-bla")));
    if (GetIsPrefixed()) {
        document->SetKeyPrefix(1000);
    }
    NSaas::AddSimpleFactor("stat1", "1", *document->MutableFactors());
    NSaas::AddSimpleFactor("stat2", "0.5", *document->MutableFactors());
    NSaas::AddSimpleFactor("stat3", "0", *document->MutableFactors());

    // add #group_xyz with multiple values
    NRTYServer::TAttribute* attr;
    attr = document->AddGroupAttributes();
    attr->SetName("xyz");
    attr->SetValue("11");
    attr->SetType(NRTYServer::TAttribute::INTEGER_ATTRIBUTE);

    attr = document->AddGroupAttributes();
    attr->SetName("xyz");
    attr->SetValue("22");
    attr->SetType(NRTYServer::TAttribute::INTEGER_ATTRIBUTE);
}

bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    GenMessages(messages);

    IndexMessages(messages, REALTIME, 1);
    const TString kps(GetAllKps(messages));
    Check("mem", kps);
    ReopenIndexers();
    Check("disk", kps);
    return true;
}
bool InitConfig() override {
    (*ConfigDiff)["Searcher.FactorsInfo"] = FactorsFileName;
    return true;
}
};

START_TEST_DEFINE(TestDumpStaticFactors)
    static NJson::TJsonValue CreateFactorsConfigTemplate() {
        static const TStringBuf configBody = R"({
            "static_factors":{
                "stat1": 1
            },
            "rty_dynamic_factors": {
                "TestDpFactor": 0
            },
            "formulas":{
                "default":{
                    "polynom":"100100KPC6JR3"
                }
            },
            "factor_sets":{
                "fs_static": [{"type":"rty_group","name":"static_factors"}]
            }
        })";
        NJson::TJsonValue result;
        NJson::ReadJsonTree(configBody, &result, true /*throwOnError*/);
        return result;
    }
    bool InitConfig() override {
        (*ConfigDiff)["Searcher.FactorsInfo"] = WriteConfigFile<EConfigType::Factors>(CreateFactorsConfigTemplate());
        return true;
    }

    static bool AllDocsHasFactor(const TVector<TSimpleSharedPtr<THashMultiMap<TString, TString>>>& resultProps, const TString& factorName) {
        const TVector<THashMap<TString, double> > factors = TRTYFactorsParser::GetJsonFactorsValues(resultProps);
        CHECK_TEST_TRUE(!factors.empty())

        for (ui32 nDoc = 0; nDoc < factors.size(); ++nDoc) {
            const auto& docFactors = factors[nDoc];
            const auto i = docFactors.find(factorName);
            CHECK_TEST_TRUE(i != docFactors.end());
            CHECK_TEST_TRUE(fabs(i->second - 5.0f) <=  1e-6)
        }
        return true;
    }

    bool Run() override {
        TVector<NRTYServer::TMessage> messages;
        {
            GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
            auto* document = messages[0].MutableDocument();
            NSaas::AddSimpleFactor("stat1", "5", *document->MutableFactors());

            IndexMessages(messages, REALTIME, 1);
        }

        const TString kps = GetAllKps(messages);

        TVector<TDocSearchInfo> results;
        TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > > resultProps;
        QuerySearch("body&dbgrlv=da&fsgta=_JsonFactors&relev=formula=default&relev=all_static_factors" + kps, results, &resultProps, nullptr, true);
        CHECK_TEST_TRUE(AllDocsHasFactor(resultProps, "stat1"));

        // &relev=factors=fs_static is a second way to do the same
        QuerySearch("body&dbgrlv=da&fsgta=_JsonFactors&relev=formula=default&relev=factors=fs_static" + kps, results, &resultProps, nullptr, true);
        CHECK_TEST_TRUE(AllDocsHasFactor(resultProps, "stat1"));

        return true;
    }
};

START_TEST_DEFINE(TestNoOutputFactorsSearchProxy)
bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    IndexMessages(messages, REALTIME, 1);

    TString kps = GetAllKps(messages);

    const TString QueryPattern = "body&dbgrlv=da&fsgta=_JsonFactors&relev=calc=user1:10&relev=formula=alternative&fsgta=TR&fsgta=TextBM25";

    {
        TVector<TDocSearchInfo> results;
        TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > > resultProps;

        QuerySearch(QueryPattern + kps, results, &resultProps, nullptr, true);

        THashMap<TString, double> factors = TRTYFactorsParser::GetJsonFactorsValues(resultProps)[0];
        CHECK_TEST_TRUE(!factors.empty());
    }

    {
        TVector<TDocSearchInfo> results;
        TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > > resultProps;

        QuerySearch(QueryPattern + "&relev=no_output_factors=1" + kps, results, &resultProps, nullptr, true);
        CHECK_TEST_TRUE(!resultProps.empty());
        for (const auto& doc : resultProps) {
            CHECK_TEST_TRUE(!doc->contains("_JsonFactors"));
        }
    }
    return true;
}
};

START_TEST_DEFINE(TestFsgtaRtyFactors)
static NJson::TJsonValue CreateFactorsConfigTemplate() {
    static const TStringBuf configBody = R"({
       "user_factors": {
            "user1": {
                "index": 0
            }
        },
        "formulas":{
            "default":{
                "polynom":"10010000000V3"
            }
        }
    })";
    NJson::TJsonValue result;
    NJson::ReadJsonTree(configBody, &result, true /*throwOnError*/);
    return result;
}

bool InitConfig() override {
    const auto serverInfo = Controller->GetServerInfo();
    const NJson::TJsonValue controllerConfig = (*serverInfo)[0]["config"]["DaemonConfig"][0]["Controller"][0];

    TFsPath versionsFilePath = controllerConfig["ConfigsRoot"].GetString();
    versionsFilePath /= controllerConfig["VersionsFileName"].GetString();

    const TFsPath pathToFactorsFile = WriteConfigFile<EConfigType::Factors>(CreateFactorsConfigTemplate());

    TFileOutput fo {versionsFilePath};
    fo << "{ \"" << pathToFactorsFile.GetName() << "\" : " << 100 << "}";

    (*ConfigDiff)["Searcher.FactorsInfo"] = pathToFactorsFile;
    return true;
}

bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    IndexMessages(messages, REALTIME, 1);
    const TString kps = GetAllKps(messages);

    auto checkFactorValuesEqual = [](float actual, float expected) -> bool {
        return fabs(actual - expected) <= 1e-6 * Max(1.0f, fabs(expected));
    };

    auto parseSearchResultDocuments = [](TStringBuf searchResult, THashMap<TString, TString>& searchProps) -> TVector<NMetaProtocol::TDocument> {
        NMetaProtocol::TReport report;
        ::google::protobuf::io::CodedInputStream decoder((ui8*)searchResult.data(), searchResult.size());
        decoder.SetTotalBytesLimit(1 << 29);
        Y_ENSURE(report.ParseFromCodedStream(&decoder) && decoder.ConsumedEntireMessage());

        for (const auto& searchProp: report.GetSearcherProp()) {
            searchProps[searchProp.GetKey()] = searchProp.GetValue();
        }

        TVector<NMetaProtocol::TDocument> resultDocuments;
        if (report.GetGrouping().size() > 0) {
            const NMetaProtocol::TGrouping& grouping(report.GetGrouping(0));
            for (int i = 0; i < grouping.GetGroup().size(); i++) {
                const NMetaProtocol::TGroup& group(grouping.GetGroup(i));
                for (int d = 0; d < group.GetDocument().size(); d++) {
                    resultDocuments.push_back(group.GetDocument(d));
                }
            }
        }
        return resultDocuments;
    };

    TString searchResult;
    CHECK_TEST_EQ(ProcessQuery("/?text=body&ms=proto&fsgta=_RtyFactors&dbgrlv=da&relev=calc=user1:10&dump=eventlog&allfctrs=da" + kps, &searchResult), 200);

    THashMap<TString, TString> searchProps;
    const TVector<NMetaProtocol::TDocument> resultDocuments = parseSearchResultDocuments(searchResult, searchProps);
    CHECK_TEST_EQ(resultDocuments.size(), 1);

    // check factor value
    const auto& doc = resultDocuments[0];
    THolder<NMetaSearch::TDocRankingFactorsAccessor> accessor = MakeHolder<NMetaSearch::TDocRankingFactorsAccessor>();
    accessor->Prepare(doc.GetDocRankingFactors(), doc.GetDocRankingFactorsSliceBorders());
    TFactorStorage* fs = accessor->RankingFactors();
    CHECK_TEST_TRUE(fs != nullptr);

    TConstFactorView factorView = fs->CreateConstView();
    CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[0], 10.0f));

    // check doc propery
    bool rtyFactorsFound = false;
    for (const NMetaProtocol::TPairBytesBytes& attr: doc.GetFirstStageAttribute()) {
        if (attr.GetKey() == "_RtyFactors") {
            CHECK_TEST_EQ(attr.GetValue(), "100");
            rtyFactorsFound = true;
            break;
        }
    }
    CHECK_TEST_TRUE(rtyFactorsFound);

    // check search property
    auto propIter = searchProps.find("rty_factors");
    CHECK_TEST_TRUE(propIter != searchProps.end());
    NJson::TJsonValue usedFactors;
    usedFactors["user1"] = 0;
    NJson::TJsonValue expected;
    expected["100"] = NJson::WriteJson(usedFactors, false);
    TStringInput si(propIter->second);
    CHECK_TEST_EQ(NJson::ReadJsonTree(&si), expected);

    return true;
}
};

START_TEST_DEFINE(TestStb)
bool Run() override {
    const int CountMessages = 10000;
    TVector<NRTYServer::TMessage> messages;
    TString kps;
    GenerateInput(messages, CountMessages, NRTYServer::TMessage::ADD_DOCUMENT, false);
    if (GetIsPrefixed()) {
        kps = "&kps=1";
        for (ui64 i = 0; i < messages.size(); ++i)
            messages[i].MutableDocument()->SetKeyPrefix(1);
    }
    IndexMessages(messages, DISK, 1);
    ReopenIndexers();
    TVector<TDocSearchInfo> results;
    TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > > docProps;
    THashMultiMap<TString, TString> searchProps;
    QuerySearch("body&dump=eventlog&pron=timebound1" + kps, results, &docProps, &searchProps, true);
    if (results.size())
        ythrow yexception() << "timebound does not work: incorrect count: " << results.size() << " > " << 0;
    QuerySearch("body&dump=eventlog" + kps, results, &docProps, &searchProps, true);
    if (results.size() > CountMessages)
        ythrow yexception() << "incorrect count: " << results.size() << " != " << CountMessages;
    return true;
}
bool InitConfig() override {
    SetIndexerParams(DISK, 10000, 1);
    return true;
}
};

START_TEST_DEFINE(TestXml)
bool Run() override {

    TString res;
    ProcessQuery("/?text=body&xml=da", &res);
    DEBUG_LOG << res << Endl;
    if (res.find("<?xml version=") == TString::npos || res.find("<query>") == TString::npos)
        ythrow yexception() << "incorrect xml format: " << res << Endl;

    const int CountMessages = 13;
    TVector<NRTYServer::TMessage> messages;
    TString kps;
    GenerateInput(messages, CountMessages, NRTYServer::TMessage::ADD_DOCUMENT, false);
    if (GetIsPrefixed()) {
        kps = "&kps=1";
        for (ui64 i = 0; i < messages.size(); ++i)
            messages[i].MutableDocument()->SetKeyPrefix(1);
    }
    IndexMessages(messages, REALTIME, 1);

    ProcessQuery("/?text=body&xml=da" + kps, &res);
    if (res.find("<?xml version=") == TString::npos || res.find("<query>") == TString::npos)
        ythrow yexception() << "incorrect xml format: " << res << Endl;
    if (res.find("<found priority=\"all\">" + ToString(CountMessages) + "</found>") == TString::npos)
        ythrow yexception() << "count not found: " << res << Endl;
    return true;
}
};

START_TEST_DEFINE(TestInprocCompression)
bool Run() override {
    TString kps;
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 1, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    if (GetIsPrefixed()) {
        kps = "&kps=1";
        for (ui64 i = 0; i < messages.size(); ++i)
            messages[i].MutableDocument()->SetKeyPrefix(1);
    }
    IndexMessages(messages, REALTIME, 1);
    TVector<TDocSearchInfo> results;
    QuerySearch("url:\"*\"&pron=rcy-lzo&timeout=1000000" + kps, results);
    if (results.size() != 1)
        ythrow yexception() << "documents count too small";
    return true;
}
};

START_TEST_DEFINE(TestFsgtaFactors)
void Check(const TString& comment, const TVector<NRTYServer::TMessage>& messages) {
    TVector<TDocSearchInfo> results;
    THashMultiMap<TString, TString> searchProps;
    TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > > docProps;
    const TString kps(GetAllKps(messages));
    QuerySearch("body&fsgta=stat3&fsgta=TR&fsgta=TextBM25" + kps, results, &docProps, &searchProps, true);
    if (results.size() != messages.size())
        ythrow yexception() << comment << ": incorrect count: " << results.size() << " != " << messages.size();
    const float stat3 = FromString<float>(docProps[0]->find("stat3")->second);
    if (Abs(stat3 - 2) > 0.001f) {
        ythrow yexception() << "incorrect fsgta stat3 " << stat3;
    }

    const float TR = FromString<float>(docProps[0]->find("TR")->second);
    if (Abs(TR) < 0.001f) {
        ythrow yexception() << "incorrect fsgta TR " << TR;
    }

    const float TextBM25 = FromString<float>(docProps[0]->find("TextBM25")->second);
    if (Abs(TextBM25) < 0.001f) {
        ythrow yexception() << "incorrect fsgta TextBM25 " << TextBM25;
    }
}

void GenMessages(TVector<NRTYServer::TMessage>& messages, int count) {
    GenerateInput(messages, count, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    for (int i = 0; i < count; ++i) {
        NSaas::AddSimpleFactor("stat1", ToString(i + 1), *messages[i].MutableDocument()->MutableFactors());
        NSaas::AddSimpleFactor("stat2", "0", *messages[i].MutableDocument()->MutableFactors());
        NSaas::AddSimpleFactor("stat3", ToString(count - i), *messages[i].MutableDocument()->MutableFactors());
    }
}

bool Run() override {
    const int countMessages = 2;
    TVector<NRTYServer::TMessage> messages;
    GenMessages(messages, countMessages);

    IndexMessages(messages, REALTIME, 1);
    Check("mem", messages);
    ReopenIndexers();
    Check("disk", messages);
    return true;
}
bool InitConfig() override {
    (*ConfigDiff)["Searcher.FactorsInfo"] = FactorsFileName;
    (*ConfigDiff)["Searcher.KeepAllDocuments"] = FactorsFileName;
    return true;
}
};


START_TEST_DEFINE(TestAppendFsgtaFactors)
bool Check(const TVector<NRTYServer::TMessage>& messages) {
    TVector<TDocSearchInfo> results;
    THashMultiMap<TString, TString> searchProps;
    TVector<TSimpleSharedPtr<THashMultiMap<TString, TString>>> docProps;
    const TString kps(GetAllKps(messages));
    QuerySearch("body&fsgta=stat3&fsgta=TR" + kps, results, &docProps, &searchProps, true);
    CHECK_TEST_EQ(results.size(), messages.size());
    const float stat3 = FromString<float>(docProps[0]->find("stat3")->second);
    CHECK_TEST_FAILED(Abs(stat3 - 2) >= 0.001f, "incorrect fsgta stat3: " + ToString(stat3));

    const float TR = FromString<float>(docProps[0]->find("TR")->second);
    CHECK_TEST_FAILED(Abs(TR) <= 0.001f, "incorrect fsgta TR: " + ToString(TR));

    CHECK_TEST_TRUE(docProps[0]->contains("TextBM25"));
    const float TextBM25 = FromString<float>(docProps[0]->find("TextBM25")->second);
    CHECK_TEST_FAILED(Abs(TextBM25) <= 0.001f, "incorrect fsgta TextBM25: " + ToString(TextBM25));
    return true;
}

void GenMessages(TVector<NRTYServer::TMessage>& messages, int count) {
    GenerateInput(messages, count, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    for (int i = 0; i < count; ++i) {
        NSaas::AddSimpleFactor("stat1", ToString(i + 1), *messages[i].MutableDocument()->MutableFactors());
        NSaas::AddSimpleFactor("stat2", "0", *messages[i].MutableDocument()->MutableFactors());
        NSaas::AddSimpleFactor("stat3", ToString(count - i), *messages[i].MutableDocument()->MutableFactors());
    }
}

bool Run() override {
    const int countMessages = 2;
    TVector<NRTYServer::TMessage> messages;
    GenMessages(messages, countMessages);

    IndexMessages(messages, REALTIME, 1);
    if(!Check(messages)) {
        TEST_FAILED("Memory search failed");
    }
    ReopenIndexers();
    if(!Check(messages)) {
        TEST_FAILED("Disk search failed");
    }
    return true;
}
bool InitConfig() override {
    (*ConfigDiff)["Searcher.FactorsInfo"] = FactorsFileName;
    (*ConfigDiff)["Searcher.KeepAllDocuments"] = FactorsFileName;

    (*SPConfigDiff)["SearchConfig.AppendCgiParams.fsgta"] = "TR&TextBM25";

    return true;
}
};

START_TEST_DEFINE_PARENT(TestFastArchiveFsgta, FacetsAll)
bool Run() override {
    const TVector<NRTYServer::TMessage> messages = GenerateMessages();
    IndexMessages(messages, REALTIME, 1);

    TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > > docProps;
    TVector<TDocSearchInfo> results;
    const TString kps = GetAllKps(messages);
    QuerySearch(Find2 + "&haha=da&fsgta=" + Prop2 + kps, results, &docProps, nullptr, true);

    for (auto&& d : docProps) {
        if (!d->count(Prop2)) {
            ythrow yexception() << "Fsgta " << Prop2 << " not found ";
        }
    }

    return true;
}
};

START_TEST_DEFINE_PARENT(TestRestoreFastArchive, FacetsAll)
bool Run() override {
    const TVector<NRTYServer::TMessage> messages = GenerateMessages();
    IndexMessages(messages, REALTIME, 1);
    Controller->ProcessCommand("stop");
    (*ConfigDiff)["ComponentsConfig.FASTARC.FastProperties"] = Prop2;
    ApplyConfig();
    Controller->ProcessCommand("restart");
    TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > > docProps;
    TVector<TDocSearchInfo> results;
    const TString kps = GetAllKps(messages);
    QuerySearch(Find2 + "&haha=da&fsgta=" + Prop2 + kps, results, &docProps, nullptr, true);

    for (auto&& d : docProps) {
        if (!d->count(Prop2)) {
            ythrow yexception() << "Fsgta " << Prop2 << " not found ";
        }
    }

    return true;
}
bool InitConfig() override {
    (*ConfigDiff)["ComponentsConfig.FASTARC.FastProperties"] = "";
    return true;
}
};

START_TEST_DEFINE(TestRelevU)
TString Kps;

bool Run() override {
    int countMessages = 6;
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    messages[0].MutableDocument()->SetBody("a1");
    messages[1].MutableDocument()->SetBody("a2");
    messages[2].MutableDocument()->SetBody("a3");
    messages[3].MutableDocument()->SetBody("a1");
    messages[4].MutableDocument()->SetBody("a2");
    messages[5].MutableDocument()->SetBody("a3");
    Kps = GetAllKps(messages);
    IndexMessages(messages, REALTIME, 1);
    TVector<TDocSearchInfo> results;
    TQuerySearchContext ctx;
    ctx.PrintResult = true;
    TString askedUrl = messages[1].GetDocument().GetUrl();
    QuerySearch("(a1 | a3)&relev=u=" + askedUrl + Kps, results, ctx);
    if (results.size() != 5)
        ythrow yexception() << "some documents not found";
    CHECK_TEST_EQ(results[0].GetUrl(), askedUrl);

    ReopenIndexers();
    QuerySearch("(a1 | a3)&relev=u=" + askedUrl + Kps, results, ctx);
    if (results.size() != 5)
        ythrow yexception() << "some documents not found";
    CHECK_TEST_EQ(results[0].GetUrl(), askedUrl);
    return true;
}
};

START_TEST_DEFINE(TestSearchApi)
bool Run() override {
    ui64 kps = GetIsPrefixed() ? 1 : 0;
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 15, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    for (ui64 i = 0; i < messages.size(); ++i) {
        messages[i].MutableDocument()->SetKeyPrefix(kps);
    }
    IndexMessages(messages, REALTIME, 1);

    NSaas::TQuery q;
    q.SetText("body");
    q.SetKps(ToString(kps));
    q.SetUserId("fakeUserId");
    q.GroupingBuilder().SetGroupsCount(13);

    auto cfg = Controller->GetConfig().Searcher;
    NSaas::TSearchClient cli("tests", cfg.Host, cfg.Port);

    auto reply = cli.SendQuery(q);
    DEBUG_LOG << q.BuildQuery() << Endl;
    DEBUG_LOG << reply.GetRawReport() << Endl;
    Y_ENSURE(reply.IsSucceeded(), "not succeeded");
    Y_ENSURE(reply.GetCode() == 200, "not OK");
    Y_ENSURE(reply.GetReport().GetGrouping(0).GroupSize() == 13, "not 13");

    return true;
}
};

SERVICE_TEST_RTYSERVER_DEFINE(TestSearchproxyCgi)
bool Run() override {
    if (!GetIsPrefixed())
        return true;
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 5, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    for (ui64 i = 0; i < messages.size(); ++i) {
        messages[i].MutableDocument()->SetKeyPrefix(1);
    }
    IndexMessages(messages, REALTIME, 1);
    TVector<TDocSearchInfo> results;
    QuerySearch("abc&haha=da", results);
    Y_ENSURE(results.size() == 5, "not work");
    return true;
}
};

START_TEST_DEFINE_PARENT(TestSearchproxyCgiDef, TestSearchproxyCgi)
bool InitConfig() override {
    (*SPConfigDiff)["SearchConfig.StrictCgiParams.text"] = "body";
    (*SPConfigDiff)["SearchConfig.SoftCgiParams.kps"] = "1";
    return true;
}
};

START_TEST_DEFINE_PARENT(TestSearchproxyCgiService, TestSearchproxyCgi)
bool InitConfig() override {
    (*SPConfigDiff)["SearchConfig.StrictCgiParams.text"] = "chush";
    (*SPConfigDiff)["SearchConfig.SoftCgiParams.kps"] = "123";

    (*SPConfigDiff)["Service.StrictCgiParams.text"] = "body";
    (*SPConfigDiff)["Service.SoftCgiParams.kps"] = "1";
    return true;
}
};

START_TEST_DEFINE_PARENT(TestSearchproxyCgiServiceGlobal, TestSearchproxyCgi)
bool InitConfig() override {
    (*SPConfigDiff)["Service.GlobalCgiParams.text"] = "body";
    (*SPConfigDiff)["Service.GlobalCgiParams.sp_meta_search"] = "proxy";
    (*SPConfigDiff)["Service.GlobalCgiParams.kps"] = "1";
    return true;
}
};

START_TEST_DEFINE_PARENT(TestSearchproxyDefaultProxySearch, TestSearchproxyCgi)
bool InitConfig() override {
    (*SPConfigDiff)["Service.DefaultMetaSearch"] = "proxy";
    (*SPConfigDiff)["Service.GlobalCgiParams.text"] = "body";
    (*SPConfigDiff)["Service.GlobalCgiParams.kps"] = "1";
    return true;
}
};


START_TEST_DEFINE(TestSearchproxyCgiServiceRedirect)
bool Run() override{
    if (Controller->GetActiveBackends().size() != 3)
        ythrow yexception() << "incorrect backends number for this test, must be 3, found " << Controller->GetActiveBackends().size();

    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 1, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    ui32 kps = GetIsPrefixed() ? 1 : 0;
    messages[0].MutableDocument()->SetKeyPrefix(kps);

    IndexMessages(messages, REALTIME, 1, 0, true, true, TDuration(), TDuration(), 1, "tests1");

    TVector<TDocSearchInfo> results;
    QuerySearch("body&service=tests&kps=" + ::ToString(kps), results);
    CHECK_TEST_FAILED(results.size() != 0, "incorrect messages count");

    QuerySearch("body&service=tests1&kps=" + ::ToString(kps), results);
    CHECK_TEST_FAILED(results.size() != 1, "incorrect messages count");

    (*SPConfigDiff)["Service.GlobalCgiParams.service"] = "tests1";
    ApplyConfig();

    TQuerySearchContext ctx;
    ctx.AttemptionsCount = 1;
    ctx.PrintResult = true;

    QuerySearch("body&service=tests&kps=" + ::ToString(kps), results, ctx);
    CHECK_TEST_FAILED(results.size() != 1, "incorrect messages count after redirect");

    (*SPConfigDiff)["Service.GlobalCgiParams.service"] = "__remove__";
    (*SPConfigDiff)["Service.GlobalCgiParams.$service"] = "(relev and relev == \"noway\") and \"tests1\" or \"tests\"";
    ApplyConfig();

    QuerySearch("body&service=tests&relev=noway&kps=" + ::ToString(kps), results, ctx);
    CHECK_TEST_FAILED(results.size() != 1, "incorrect messages count after redirect");

    QuerySearch("body&service=tests&kps=" + ::ToString(kps), results, ctx);
    CHECK_TEST_FAILED(results.size() != 0, "incorrect messages count without redirect");

    return true;
}

};


START_TEST_DEFINE(TestRegexpFilteringViaCgiParametr)

void GenMessages(TVector<NRTYServer::TMessage>& messages) {
    GenerateInput(messages, 20, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed(), TAttrMap(), "body body body");
    if (GetIsPrefixed()){
        for (ui64 i = 0; i < messages.size(); ++i) {
            messages[i].MutableDocument()->SetKeyPrefix(1);
        }
    }
    for (ui64 i = 0; i < messages.size(); ++i) {
        NRTYServer::TMessage::TDocument::TProperty& prop = *messages[i].MutableDocument()->AddDocumentProperties();
        prop.set_name("p_abc");
        prop.set_value("prop" + ToString(i));
    }
}

void Check(const TString& comment, const TString& query, size_t totalCount, const TString& kps, THashMap<TString, THashSet<TString>> needProps = THashMap<TString, THashSet<TString>>()){
    TVector<TDocSearchInfo> results;
    TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > > docProps;

    QuerySearch(query + kps, results, &docProps);

    if (results.size() != totalCount){
        ythrow yexception() << comment << ": incorrect count: " << results.size() << " != " << totalCount;
    }
    THashMap<TString, THashSet<TString>> props;

    for (auto& docInfos : docProps){
        for (auto& i : *docInfos){
            props[i.first].insert(i.second);
        }
    }
    for (const auto& prop : needProps){
        if (prop.second != props[prop.first]){
            ythrow yexception() << comment << ": sets of expected and current prop \'" << prop.first <<"\' values don\'t match ";
        }
    }
}

bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    GenMessages(messages);
    IndexMessages(messages, REALTIME, 1);
    TString kps = GetAllKps(messages);

    Check("without regexp"
    , "body", 20, kps);

    Check("matching all"
    , "body&regexp=p_abc=~.*", 20, kps);

    Check("not exist prop"
    , "body&regexp=p_ab=~.*", 0, kps);

    Check("single regexp"
    , "body&regexp=p_abc=~prop.*6", 2, kps, {{"p_abc",{"prop6","prop16"}}});

    Check("double regexp"
    , "body&regexp=p_abc=~prop1.*&regexp=p_abc=~prop.*(2|4|6|8)", 4, kps, {{"p_abc",{"prop12", "prop14","prop16", "prop18"}}});

    Check("double regexp with rejecting"
    , "body&regexp=p_abc=~prop1.*&regexp=p_abc!~prop.(2|4|6|8)", 7, kps, {{"p_abc",{"prop1", "prop10","prop11", "prop13","prop15", "prop17","prop19"}}});

    Check("multiple regexp with rejecting"
    , "body&regexp=p_abc=~.*1.*&regexp=p_abc!~.*(2|4|6|8|0)&regexp=p_abc!~.*(11|.*3.*)", 4, kps, {{"p_abc",{"prop1", "prop15", "prop17","prop19"}}});

    return true;
}

};
