#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/rtyserver_test/util/factors_parsers.h>
#include <library/cpp/string_utils/quote/quote.h>
#include <library/cpp/charset/wide.h>
#include <saas/rtyserver_test/testerlib/standart_generator.h>
#include <saas/api/clientapi.h>

SERVICE_TEST_RTYSERVER_DEFINE(TestTextMachineParent)
TString Kps;
const unsigned CountMessages = 3;
TString UrlWithoutTitleHit = "notitlehiturl";

protected:
    void Index() {
        const bool isPrefixed = GetIsPrefixed();
        TVector<NRTYServer::TMessage> messages;

        GenerateInput(messages, CountMessages - 1, NRTYServer::TMessage::MODIFY_DOCUMENT, isPrefixed, TAttrMap(), "мигрень. уходи, мигрень, большая и маленькая");
        messages[CountMessages-2].MutableDocument()->SetBody("Это текст без нужного слова. А теперь будет мигрень.");
        messages[CountMessages-2].MutableDocument()->SetUrl(UrlWithoutTitleHit);
        NRTY::TAction action;
        action.SetActionType(NRTY::TAction::atAdd);
        action.SetId(CountMessages);
        action.SetPrefix(GetIsPrefixed() ? 10203 : 0);
        NRTY::TDocument& doc = action.AddDocument();
        doc.SetUrl(ToString("someurl.com"));
        doc.SetMimeType("text/html");
        doc.AddZone("z_body").SetText("текст статьи");
        doc.AddZone("z_title").SetText("про мигрень");
        messages.push_back(action.ToProtobuf());

        Kps = GetAllKps(messages);
        IndexMessages(messages, REALTIME, 1);
    }

    virtual TString GetQBundlePart(){
        TString reqbundle = "ehRMWi40AQAAAACAuQAB-BcK8wEq5gF6HhLfARKzARIO0LzQuNCz0YDQtdC90YwYACABKhQKEBYAmdC10LkQACoSCiwALtC4FAA90YwQPgBs0YzRjhAAVACo0Y_QvBAAKhYKEmoAABYAAVgAC4IA-QLRj9GFEAAyBRDO3_MBIKMCKooA8wPRjDICCAE4AUAASgUQ6OLpA1IpAPERADDrqMK96M7jgVwSEwgBEBEaCBICCAAaAggALc3MzD0VABYAFQBQLQAAgD8AAAA,";
        return "&qbundle=" + reqbundle;
    }

    virtual TString GetProns(){
        return "&pron=qbundleiter&pron=qbundleon_All";
    }

    void Test(TIndexerType indexer) {
        if (indexer == DISK)
            ReopenIndexers();
        else
            Sleep(TDuration::Seconds(2));
        TVector<TDocSearchInfo> results;
        TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > > resultProps;
        QuerySearch("мигрень" + Kps
            + "&relev=all_factors&fsgta=_JsonFactors&dbgrlv=da&fsgta=__HitsInfo&rty_hits_detail=da"
            + GetProns() + GetQBundlePart(), results, &resultProps, nullptr, true);
        if (results.size() != CountMessages)
            ythrow yexception() << "polnaya hren'";
        for (unsigned i=0; i < CountMessages; ++i) {
            DEBUG_LOG << resultProps[i]->find("_JsonFactors")->second << Endl;
            THashMap<TString, double> factors = TRTYFactorsParser::GetJsonFactorsValues(resultProps)[i];
            THashMap<TString, double>::const_iterator iFactor = factors.find("QfufAllSumWFSumWFieldSet3BclmWeightedFLogW0K0001");
            if (iFactor == factors.end()) {
                ythrow yexception() << "there is no QfufAllSumWFSumWFieldSet3BclmWeightedFLogW0K0001 in result";
            }
            if (iFactor->second < 0.1) {
                ythrow yexception() << "Too small factor: " << iFactor->second;
            }
            iFactor = factors.find("QfufAllMaxFFieldSetUTBm15FLogW0K00001");
            if (iFactor == factors.end()) {
                ythrow yexception() << "there is no QfufAllMaxFFieldSetUTBm15FLogW0K00001 in result";
            }
            if (results[i].GetUrl() == UrlWithoutTitleHit) {
                if (iFactor->second > 0.01)
                    ythrow yexception() << "Too big factor for no url hits: " << iFactor->second;
            } else if (iFactor->second < 0.1) {
                ythrow yexception() << "Too small factor: " << iFactor->second;
            }
        }
    }
public:
    bool Run() override {
        Index();
        Test(REALTIME);
        Test(DISK);
        return true;
    }

    bool InitConfig() override {
        (*ConfigDiff)["Searcher.FactorsInfo"] = GetResourcesDirectory() + "/factors/relev.conf-textmachine";
        (*ConfigDiff)["Searcher.TextMachine.TitleZones"] = "z_title";
        (*ConfigDiff)["Searcher.TextMachine.TitleSentencesNumber"] = 1;
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestTextMachine, TestTextMachineParent)
};


START_TEST_DEFINE_PARENT(TestTextMachineAsPlugin, TestTextMachineParent)
    TString GetProns() override {
        // Do the same test as TestTextMachine, but in the "call TextMachine from SaaS" mode (IsExternalRelevanceCalcer() returns false)
        return TestTextMachineParent::GetProns() + "&relev=tm=web";
    }
};

START_TEST_DEFINE_PARENT(TestTextMachineRearrange, TestTextMachineParent)
    TString GetQBundlePart() override {
        TString wizqbundle = "ehRMWi40AQAAAACArQAB-BcK8wEq5gF6HhLfARKzARIO0LzQuNCz0YDQtdC90YwYACABKhQKEBYAmdC10LkQACoSCiwALtC4FAA90YwQPgBs0YzRjhAAVACo0Y_QvBAAKhYKEmoAABYAAVgAC4IA-QLRj9GFEAAyBRDO3_MBIKMCKooA8wPRjDICCAE4AUAASgUQ6OLpA1IpAPARADDrqMK96M7jgVwSEwgBEAAaCBICCAAaAggALQAAgD8AAAA,";
        TString lbqbundle = "ehRMWi40AQAAAACAuQAB-BcK8wEq5gF6HhLfARKzARIO0LzQuNCz0YDQtdC90YwYACABKhQKEBYAmdC10LkQACoSCiwALtC4FAA90YwQPgBs0YzRjhAAVACo0Y_QvBAAKhYKEmoAABYAAVgAC4IA-QLRj9GFEAAyBRDO3_MBIKMCKooA8wPRjDICCAE4AUAASgUQ6OLpA1IpAPERADDrqMK96M7jgVwSEwgBEBEaCBICCAAaAggALc3MzD0VABYFFQBQLQAAgD8AAAA,";
        TString bundlePart = "&relev=wizqbundle=" + wizqbundle + "&relev=lbqbundle=" + lbqbundle;
        return bundlePart;
    }
    bool InitConfig() override {
        (*ConfigDiff)["Searcher.FactorsInfo"] = GetResourcesDirectory() + "/factors/relev.conf-textmachine";
        (*ConfigDiff)["Searcher.TextMachine.TitleZones"] = "z_title";
        (*ConfigDiff)["Searcher.TextMachine.TitleSentencesNumber"] = 1;
        (*SPConfigDiff)["SearchConfig.ReArrangeOptions"] = "LingBoostL2";
        (*SPConfigDiff)["SearchConfig.CompiledInOptions"] = "ReArrangeOptions";
        return true;
    }
};

SERVICE_TEST_RTYSERVER_DEFINE_PARENT(TestTextMachineFastrankParent, TestTextMachineParent)
private:
    void TestFactorNonZero(const bool expectedNonZero, const TString& prons) {
        NOTICE_LOG << "Checking TestFactorNonZero with " << prons << Endl;
        // Just check if the factor is not zero
        TVector<TDocSearchInfo> results;
        TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > > resultProps;
        QuerySearch("мигрень" + Kps
            + "&relev=all_factors&fsgta=_JsonFactors&dbgrlv=da&fsgta=__HitsInfo&rty_hits_detail=da"
            + prons + GetQBundlePart(), results, &resultProps, nullptr, true);
        if (results.size() != CountMessages)
            ythrow yexception() << "Incorrect number of search results";
        for (unsigned i=0; i < CountMessages; ++i) {
            DEBUG_LOG << resultProps[i]->find("_JsonFactors")->second << Endl;
            THashMap<TString, double> factors = TRTYFactorsParser::GetJsonFactorsValues(resultProps)[i];
            THashMap<TString, double>::const_iterator iFactor = factors.find("QfufAllSumWFSumWFieldSet3BclmWeightedFLogW0K0001");
            if (iFactor == factors.end()) {
                ythrow yexception() << "there is no QfufAllSumWFSumWFieldSet3BclmWeightedFLogW0K0001 in result";
            }
            if (expectedNonZero && iFactor->second < 0.1) {
                ythrow yexception() << "Too small factor: " << iFactor->second;
            }
            if (!expectedNonZero && iFactor->second > 0.001) {
                ythrow yexception() << "Too big factor: " << iFactor->second;
            }
        }
    }

protected:
    TString GetProns() override {
        return "&pron=fastrank&pron=fastcount10&relev=fast_formula=KC0100KPC6JR3" + TestTextMachineParent::GetProns();
    }

public:
    bool Run() override {
        CHECK_TEST_TRUE(TestTextMachineParent::Run()); // will execute the base test with fastrank enabled (using GetProns())

        // extra checks
        TestFactorNonZero(true, GetProns() + "&pron=text_machine_l2&pron=onlyfastrank");
        TestFactorNonZero(true, GetProns() + "&pron=text_machine_l2&pron=onlyfastrank&pron=sortbyfastmn");
        TestFactorNonZero(false, GetProns() + "&pron=onlyfastrank");
        TestFactorNonZero(false, GetProns() + "&pron=onlyfastrank&pron=sortbyfastmn");

        // we also expect that relev=all_factors=ext will make the factor appear, even when not used in formula
        TestFactorNonZero(true, GetProns() + "&pron=text_machine_l2&pron=onlyfastrank&pron=sortbyfastmn&relev=all_factors=ext&relev=fast_formula=default");

        return true;
    }
};

START_TEST_DEFINE_PARENT(TestTextMachineFastrank, TestTextMachineFastrankParent)
};

START_TEST_DEFINE_PARENT(TestTextMachineFastrankAsPlugin, TestTextMachineFastrankParent)
protected:
    TString GetProns() override {
        return TestTextMachineFastrankParent::GetProns() + "&relev=tm=web";
    }
};


