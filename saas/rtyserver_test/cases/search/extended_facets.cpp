#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/rtyserver_test/testerlib/standart_generator.h>

#include <library/cpp/json/json_reader.h>


START_TEST_DEFINE(TestExtendedFacets)
public:
    TString Kps;
    TString Opt;
    bool FacetsAfterFilterBorderMode;

public:
    bool PrepareData() {
        TAttrMap::value_type groupMap1, groupMap2, groupMap3;
        groupMap1["mid"] = 1;
        groupMap1["attr_cc_grp"] = TAttr("1_1");
        groupMap2["mid"] = 2;
        groupMap2["attr_cc_grp"] = TAttr("2_2");
        groupMap3["mid"] = 3;
        groupMap3["attr_cc_grp"] = TAttr("3_3");

        TAttrMap::value_type groupMap12, groupMap23, groupMap34;
        groupMap12["mid"] = 2;
        groupMap12["attr_cc_grp"] = TAttr("1_1");
        groupMap23["mid"] = 3;
        groupMap23["attr_cc_grp"] = TAttr("2_2");
        groupMap34["mid"] = 4;
        groupMap34["attr_cc_grp"] = TAttr("3_3");

        TAttrMap::value_type searchMap;
        searchMap["mid"] = TString("123");
        TVector<NRTYServer::TMessage> messages;
        TString body1 = "Some hits are very useful";
        TString body2 = "Some hints are very useful";
        TString body3 = "Some hits are very hopeful";
        GenerateInput(messages, 2, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(2, groupMap1), body1, true, TAttrMap(2, searchMap));
        GenerateInput(messages, 2, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(2, groupMap2), body2, true, TAttrMap(2, searchMap));
        GenerateInput(messages, 2, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(2, groupMap3), body3, true, TAttrMap(2, searchMap));
        GenerateInput(messages, 2, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(2, groupMap12), body3, true, TAttrMap(2, searchMap));
        GenerateInput(messages, 2, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(2, groupMap23), body1, true, TAttrMap(2, searchMap));
        GenerateInput(messages, 2, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(2, groupMap34), body2, true, TAttrMap(2, searchMap));
        IndexMessages(messages, REALTIME, 1);
        Kps = GetAllKps(messages);
        return true;
    }

    bool CheckPropEquality(const TString& propGot, const TString& propMustBe) {
        TSet<TString> got, mustBe;
        StringSplitter(propGot).Split(';').SkipEmpty().Collect(&got);
        StringSplitter(propMustBe).Split(';').SkipEmpty().Collect(&mustBe);
        DEBUG_LOG << "got=" << propGot << ", mustBe=" << propMustBe << Endl;
        if (got != mustBe) { return false; }
        return true;
    }

    void SetCase(bool facetsfb) {
        Opt = facetsfb ? "&facetsfb=1" : "";
        FacetsAfterFilterBorderMode = facetsfb;
    }

bool Check() {
    TVector<TDocSearchInfo> results;
    TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > > docProperties;
    THashMultiMap<TString, TString> searchProperties;
    QuerySearch("url:\"*\"&how=mid&efacets=attr_cc_grp:mid@min&efacets=mid:attr_cc_grp@unique&efacets=attr_cc_grp:mid@sum&qi=rty_allfacets" + Kps + Opt,
        results, &docProperties, &searchProperties, true);

    THashMultiMap<TString, TString>::const_iterator i = searchProperties.find("rty_efacet_min_attr_cc_grp__mid");
    if (i == searchProperties.end())
        ythrow yexception() << "no min facet";

    NJson::TJsonValue prop;
    TSet<TString> propRes;
    NJson::ReadJsonTree(i->second, &prop);
    CHECK_TEST_EQ(prop["aggr_type"].GetString(), "min");
    TString propMustBe = "1_1:1;2_2:2;3_3:3";
    if (!CheckPropEquality(prop["result"].GetString(), propMustBe)) {
        DEBUG_LOG << "Min aggr not equal: got=" << prop["result"].GetString() << ", mustBe=" << propMustBe << Endl;
        return false;
    }

    i = searchProperties.find("rty_efacet_sum_attr_cc_grp__mid");
    if (i == searchProperties.end())
        ythrow yexception() << "no sum facet";

    NJson::ReadJsonTree(i->second, &prop);
    CHECK_TEST_EQ(prop["aggr_type"].GetString(), "sum");
    propMustBe = "1_1:6;2_2:10;3_3:14";
    if (!CheckPropEquality(prop["result"].GetString(), propMustBe)) {
        DEBUG_LOG << "Sum aggr not equal: got=" << prop["result"].GetString() << ", mustBe=" << propMustBe << Endl;
        return false;
    }

    i = searchProperties.find("rty_efacet_unique_mid__attr_cc_grp");
    if (i == searchProperties.end())
        ythrow yexception() << "no unique facet";
    NJson::ReadJsonTree(i->second, &prop);
    CHECK_TEST_EQ(prop["aggr_type"].GetString(), "unique");
    propMustBe = "1:1_1;2:1_1,2_2;3:2_2,3_3;4:3_3";
    if (!CheckPropEquality(prop["result"].GetString(), propMustBe)) {
        DEBUG_LOG << "Unique aggr not equal: got=" << prop["result"].GetString() << ", mustBe=" << propMustBe << Endl;
        return false;
    }

    QuerySearch("url:\"*\"&how=mid&efacets=attr_cc_grp:mid@max&qi=rty_efacet_max_attr_cc_grp__mid" + Kps + Opt, results, &docProperties, &searchProperties, true);

    i = searchProperties.find("rty_efacet_max_attr_cc_grp__mid");
    if (i == searchProperties.end())
        ythrow yexception() << "no max facet";
    NJson::ReadJsonTree(i->second, &prop);
    CHECK_TEST_EQ(prop["aggr_type"].GetString(), "max");
    propMustBe = "1_1:2;2_2:3;3_3:4";
    if (!CheckPropEquality(prop["result"].GetString(), propMustBe)) {
        DEBUG_LOG << "Max aggr not equal: got=" << prop["result"].GetString() << ", mustBe=" << propMustBe << Endl;
        return false;
    }

    QuerySearch("useful hits softness:100&how=mid&efacets=attr_cc_grp:RTY_HITS@sum&qi=rty_allfacets" + Kps + Opt, results, &docProperties, &searchProperties, true);

    i = searchProperties.find("rty_efacet_sum_attr_cc_grp__RTY_HITS");
    if (i == searchProperties.end())
        ythrow yexception() << "no hits facet";
    NJson::ReadJsonTree(i->second, &prop);
    CHECK_TEST_EQ(prop["aggr_type"].GetString(), "sum");

    if (!FacetsAfterFilterBorderMode) {
        propMustBe = "1_1:6;2_2:6;3_3:4";
        if (!CheckPropEquality(prop["result"].GetString(), propMustBe)) {
            DEBUG_LOG << "Hits aggr not equal: got=" << prop["result"].GetString() << ", mustBe=" << propMustBe << Endl;
            return false;
        }
    } else {
        DEBUG_LOG << "Check skipped due to '" + Opt + "', the result was: '" << prop["result"].GetString() << "'" << Endl;
    }
    return true;
}

bool Run() override {
    PrepareData();

    SetCase(/*facetsfb=*/false);
    CHECK_TEST_TRUE(Check());

    SetCase(/*facetsfb=*/true);
    CHECK_TEST_TRUE(Check());

    return true;
}
};
