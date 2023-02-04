#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/api/factors_erf.h>

START_TEST_DEFINE(TestAbt)
protected:
virtual void GenMessages(TVector<NRTYServer::TMessage>& messages, int count) {
    GenerateInput(messages, count, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    for (int i = 0; i < count; ++i) {
        auto prop = messages[i].MutableDocument()->AddDocumentProperties();
        prop->SetName("prp");
        prop->SetValue("vls");
        NSaas::AddSimpleFactor("stat1", ToString(i + 1), *messages[i].MutableDocument()->MutableFactors());
        NSaas::AddSimpleFactor("stat2", "0", *messages[i].MutableDocument()->MutableFactors());
        NSaas::AddSimpleFactor("stat3", ToString(count - i), *messages[i].MutableDocument()->MutableFactors());
    }
}

bool InitConfig() override {
    (*ConfigDiff)["Searcher.FactorsInfo"] = FactorsFileName;
    return true;
}

bool CompareABResults(const TVector<TDocSearchInfo>& a, const TVector<TDocSearchInfo>& b) {
    if (a.size() != b.size()) {
        DEBUG_LOG << "DocCount mismatch " << a.size() << " != " << b.size() << Endl;
        return false;
    }
    for(size_t i=0; i<a.size(); ++i) {
        if (a[i].GetUrl() != b[i].GetUrl()) {
            DEBUG_LOG << "Url mismatch for position " << i << " " << a[i].GetUrl() << " != " << b[i].GetUrl() << Endl;
            return false;
        }
    }
    return true;
}

void CheckDocProps(const TVector<TSimpleSharedPtr<THashMultiMap<TString, TString>>>& props, ui32 testId) {
    for (size_t i = 0; i < props.size(); ++i) {
        auto it = props[i]->find("clickUrl");
        CHECK_WITH_LOG(it != props[i]->end());
        const TString& marker = "/slots=" + ToString(testId) + ",";
        if (it->second.find(marker) == TString::npos)
            ythrow yexception() << "Incorrect click url " << it->second;
    }
}

virtual void Check(const TString& comment, const TVector<NRTYServer::TMessage>& messages) {
    TVector<TSimpleSharedPtr<THashMultiMap<TString, TString>>> props;
    TVector<TDocSearchInfo> resultsA, resultsB, resultsC, resultsForce, resultsRestrict;
    THashMultiMap<TString, TString> searchPropA;
    THashMultiMap<TString, TString> searchPropB;
    const TString kps(GetAllKps(messages));

    TQuerySearchContext context;
    context.SearchProperties = &searchPropA;
    context.DocProperties = &props;
    context.PrintResult = true;
    context.HttpHeaders["X-Yandex-ExpConfigVersion"] = "1984";
    context.HttpHeaders["X-Yandex-ExpBoxes"] = "16303,0,-1";
    context.HttpHeaders["X-Yandex-ExpFlags"] = "W3siSEFORExFUiI6ICJTQUFTIiwgIkNPTlRFWFQiOiB7IlNBQVMiOiB7ImZsYWdzIjogeyIxNjMwMyI6IFsiYWJ0Il19fSwgIkExNjMwMyI6IHsic291cmNlIjogeyJ0ZXN0cyI6IHsicmVsZXYiOiBbImN0cmwtMTYzMDMiXX19fSwgIkIxNjMwMyI6IHsic291cmNlIjogeyJ0ZXN0cyI6IHsicmVsZXYiOiBbImZvcm11bGE9YWx0ZXJuYXRpdmUiXX19fX19XQ==";

    QuerySearch("body&uuid=a3ee5906bc0d56d87fe4bec1d8a738af&facets=prp&qi=facet_prp" + kps, resultsA, context); // exp on
    CheckDocProps(props, 16303);
    if (!resultsA.size()) {
        ythrow yexception() << "no docs in A";
    }
    QuerySearch("body&uuid=128ecf542a35ac5270a87dc740918403&facets=prp&qi=facet_prp" + kps, resultsB, nullptr, &searchPropB, true); // exp off
    if (!resultsB.size()) {
        ythrow yexception() << "no docs in B";
    }
    QuerySearch("body&uuid=128ecf542a35ac5270a87dc740918403&relev=formula=alternative;set=user1:stat1" + kps, resultsC, nullptr, nullptr, true); // exp off
    if (!resultsC.size()) {
        ythrow yexception() << "no docs in C";
    }

    if (searchPropA.find("facet_prp")->second != "vls:10")
        ythrow yexception() << "incorrect facet count during experiment";

    if (searchPropB.find("facet_prp")->second != "vls:10")
        ythrow yexception() << "incorrect facet count during experiment";

    if (CompareABResults(resultsA, resultsB))
        ythrow yexception() << comment << ": incorrect abt: A == B";
    if(!CompareABResults(resultsA, resultsC))
        ythrow yexception() << comment << ": incorrect abt: A != C";
}

public:
bool Run() override {
    const int countMessages = 10;
    TVector<NRTYServer::TMessage> messages;
    GenMessages(messages, countMessages);

    IndexMessages(messages, REALTIME, 1);
    Check("mem", messages);
    ReopenIndexers();
    Check("disk", messages);
    return true;
}
};

START_TEST_DEFINE_PARENT(TestTdi, TTestAbtCaseClass)
protected:
TString GetTdiSrc(TSimpleSharedPtr<THashMultiMap<TString, TString>> props) {
    auto range = props->equal_range("_Markers");
    for (auto i = range.first; i != range.second; ++i) {
        if (i->second.find("TdiSrc") != TString::npos) {
            return i->second;
        }
    }
    return TString();
}
void CheckMarkers(const TVector<TSimpleSharedPtr<THashMultiMap<TString, TString>>>& props) {
    for (size_t i = 0; i < props.size() / 2; i += 2) {
        const TString& joined = GetTdiSrc(props[i]) + GetTdiSrc(props[i + 1]);
        if (joined.find("saas_experiment") == TString::npos || joined.find("saas_production") == TString::npos)
            ythrow yexception() << "incorrect markers for pos=" << i << " " << joined;
    }
}

void CheckClickUrl(const TVector<TSimpleSharedPtr<THashMultiMap<TString, TString>>>& props) {
    for (size_t i = 0; i < props.size(); ++i) {
        auto it = props[i]->find("clickUrl");
        CHECK_WITH_LOG(it != props[i]->end());
        const TString& marker = "/p=" + ToString(i);
        if (it->second.find(marker) == TString::npos)
            ythrow yexception() << "Incorrect click url " << it->second;
    }
}

void Check(const TString& comment, const TVector<NRTYServer::TMessage>& messages) override {
    TVector<TSimpleSharedPtr<THashMultiMap<TString, TString>>> props;
    TVector<TDocSearchInfo> resultsA, resultsB, resultsC;
    const TString kps(GetAllKps(messages));

    TQuerySearchContext context;
    context.DocProperties = &props;
    context.PrintResult = true;
    context.HttpHeaders["X-Yandex-ExpConfigVersion"] = "1984";
    context.HttpHeaders["X-Yandex-ExpBoxes"] = "16303,0,-1";
    context.HttpHeaders["X-Yandex-ExpFlags"] = "W3siSEFORExFUiI6ICJTQUFTIiwgIkNPTlRFWFQiOiB7IlNBQVMiOiB7ImZsYWdzIjogeyIxNjMwMyI6IFsidGRpIl19fSwgIkExNjMwMyI6IHsic291cmNlIjogeyJ0ZXN0cyI6IHsicmVsZXYiOiBbImN0cmwtMTYzMDMiXX19fSwgIkIxNjMwMyI6IHsic291cmNlIjogeyJ0ZXN0cyI6IHsicmVsZXYiOiBbImZvcm11bGE9YWx0ZXJuYXRpdmUiXX19fX19XQ==";

    QuerySearch("body&uuid=a3ee5906bc0d56d87fe4bec1d8a738ag" + kps, resultsA, nullptr, nullptr, true); // exp off
    if (resultsA.size() != 10) {
        ythrow yexception() << "no docs in A";
    }
    QuerySearch("body&uuid=128ecf542a35ac5270a87dc740918404" + kps, resultsB, context); // exp on
    CheckDocProps(props, 16303);
    if (resultsB.size() != 10) {
        ythrow yexception() << "no docs in B";
    }
    CheckMarkers(props);
    CheckClickUrl(props);
    QuerySearch("body&uuid=a3ee5906bc0d56d87fe4bec1d8a738ag&relev=formula=alternative;set=user1:stat1" + kps, resultsC, nullptr, nullptr, true);
    if (resultsC.size() != 10) {
        ythrow yexception() << "no docs in C";
    }

    if (CompareABResults(resultsA, resultsB))
        ythrow yexception() << comment << ": incorrect GM_FLAT tdi: A == B";

    resultsA.clear();
    resultsB.clear();
    resultsC.clear();

    QuerySearch("body&g=1.test.10.1&uuid=ec55ff505a9386501600f7d5c0bffa81" + kps, resultsA, context); // exp on
    CheckDocProps(props, 16303);
    CheckMarkers(props);
    CheckClickUrl(props);
    QuerySearch("body&g=1.test.10.1&uuid=128ecf542a35ac5270a87dc740918405" + kps, resultsB, nullptr, nullptr, true); // exp off
    QuerySearch("body&g=1.test.10.1&uuid=128ecf542a35ac5270a87dc740918405&relev=formula=alternative;set=user1:stat1" + kps, resultsC, nullptr, nullptr, true);

    if (CompareABResults(resultsA, resultsB))
        ythrow yexception() << comment << ": incorrect GM_DEEP tdi: A == B";
}

void GenMessages(TVector<NRTYServer::TMessage>& messages, int count) override {
    TTestAbtCaseClass::GenMessages(messages, count);
    for (int i = 0; i < count; ++i) {
        NRTYServer::TAttribute& attr = *messages[i].MutableDocument()->AddGroupAttributes();
        attr.set_name("test");
        attr.set_value(ToString(i));
        attr.set_type(NRTYServer::TAttribute::INTEGER_ATTRIBUTE);
    }
}
};
