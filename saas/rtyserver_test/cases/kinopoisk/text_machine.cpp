
#include <saas/rtyserver_test/cases/indexer/ann.h>
#include <saas/rtyserver_test/testerlib/rtyserver_test.h>

namespace {

    /*
        "aven": 1
        "avengers infinite war": 1
        "avengers the": 0.9
        "avenger": 0.8
        "avenue": 0.5
    */
    constexpr TStringBuf QBUNDLE = "ehRMWi40AQAAAACALAIB8j8KYSpVehRMWi40AQAAAACAQwAB8QISWRIbEgRhdmVuGAEgAioICgwAnxABMgUQhPucBB0ACkEgkwIqLwDwCTICCAI4AUAASgUQwq_MBFIiABAgSgDoMPaD__2C5tmSSApXKktjALA5AAHzDRI2EhMSB2MA0GdlchgAIAIyBBDs_RZZAGUIFgDwCHNbAGIEEOjvHFIeAAFZANj5k-urqrLpk3AKWypOWQDwBDwAAf8JEk0SFBIIaW5maW5pdGXAAKAyBBCenhMWAANFXgBHLQDwB1wAQcChE1IgAAK1AOie6cCN3vjLtOMBClYqSV0A4zcAAf8EEk8SDxIDd2FyWABw0KUBEQAPQFgAGjRYAEH87wFSIAACWADokKy04ZfPx5SsAQpMKkBYAMEuAAASKhIOEgN0aGUIAUADENwBUQAAEQADqQAgAUoVABBSBQACTwCf34qYoKf_zvA1pgAAT_QMEjVcAQYaFqYAOIrhcFoB6Pahp5HCyMr29AEKVCpIpgCwNgAB8wsSMxISEga0ARJ1qwBQBBCc6gisABoVVwBChIwJUhwAAbEB8gXioqmjgvq2gR0SKggBEAAaEQoHKjEC8QESAggAGgIIAC0AAIA_MgYadALwA0IECAEYARInCAMQWhoIEgIIASMAAQoAIAIaDgAAFABRAxoCCAI3AE0SHQgCKQAQBCkAky1mZmY_EhMIAR8AEAVIAEgtzcxMFQCgBhoCCAAtAAAAPwAAAA%2C%2C";

    using TExpectedFactors = THashMap<TString, float>;

    const static TVector<TExpectedFactors> EXPECTED_FACTORS = {
        {
            {"KinopoiskSuggestAllMaxFTitleAttenV1Bm15K001", 0.980418},
            {"KinopoiskSuggestAllMaxFTitleWordCoverageExact", 1},
            {"KinopoiskSuggestAllMaxWFMaxWTitleCosineMatchMaxPrediction", 0.9},
            {"KinopoiskSuggestAllMaxWFMaxWTitleExactQueryMatchAvgValue", 0},
            {"KinopoiskSuggestAllMaxWFSumWTitleExactQueryMatchAvgValue", 0},
            {"KinopoiskSuggestAllMaxWFTitleExactQueryMatchAvgValue", 0},
            {"KinopoiskSuggestAllSumW2FSumWTitleExactQueryMatchAvgValue", 0},
            {"KinopoiskSuggestTopMinWFMaxWTitleBclmMixPlainKE5", 0.333},
            {"KinopoiskSuggestTopMinWFSumWTitleExactQueryMatchAvgValue", 0},
            {"KinopoiskSuggestTopMinWFTitleWordCoverageForm", 0.400556},
            {"KinopoiskSuggestTopSumW2FSumWTitleExactQueryMatchAvgValue", 0},
            {"KinopoiskSuggestAllTotalW", (1 + 1 + 0.9 + 0.8 + 0.5) / (1 + 1 + 0.9 + 0.8 + 0.5 + 10.0)},
            {"KinopoiskSuggestAllAvgW", (1 + 1 + 0.9 + 0.8 + 0.5) / 5.0},
            {"KinopoiskSuggestAllMinW", 0.5},
            {"KinopoiskSuggestAllNumX", 5.0 / (5.0 + 10.0)},
            {"KinopoiskSuggestTopNumX", 3.0 / (3.0 + 10.0)}
        },
        {
            {"KinopoiskSuggestAllMaxFTitleAttenV1Bm15K001", 0.987654},
            {"KinopoiskSuggestAllMaxFTitleWordCoverageExact", 1},
            {"KinopoiskSuggestAllMaxWFMaxWTitleCosineMatchMaxPrediction", 1},
            {"KinopoiskSuggestAllMaxWFMaxWTitleExactQueryMatchAvgValue", 1},
            {"KinopoiskSuggestAllMaxWFSumWTitleExactQueryMatchAvgValue", 0.238095},
            {"KinopoiskSuggestAllMaxWFTitleExactQueryMatchAvgValue", 1},
            {"KinopoiskSuggestAllSumW2FSumWTitleExactQueryMatchAvgValue", 0.238095},
            {"KinopoiskSuggestTopMinWFMaxWTitleBclmMixPlainKE5", 0.44955},
            {"KinopoiskSuggestTopMinWFSumWTitleExactQueryMatchAvgValue", 0},
            {"KinopoiskSuggestTopMinWFTitleWordCoverageForm", 0.8},
            {"KinopoiskSuggestTopSumW2FSumWTitleExactQueryMatchAvgValue", 0.344828},
            {"KinopoiskSuggestAllTotalW", (1 + 1 + 0.9 + 0.8 + 0.5) / (1 + 1 + 0.9 + 0.8 + 0.5 + 10.0)},
            {"KinopoiskSuggestAllAvgW", (1 + 1 + 0.9 + 0.8 + 0.5) / 5.0},
            {"KinopoiskSuggestAllMinW", 0.5},
            {"KinopoiskSuggestAllNumX", 5.0 / (5.0 + 10.0)},
            {"KinopoiskSuggestTopNumX", 3.0 / (3.0 + 10.0)}
        }
    };

}
    
START_TEST_DEFINE(TestKinopoiskTextMachineTitle)
private:
    using TFactors = NSaas::TDocFactorsView;
public:
    TVector<TFactors> ReadFactors(const TString& query, int line) {
        DEBUG_LOG << "L" << line <<" Query: " << query << Endl;
        return ReadFactors(query);
    }

    TVector<TFactors> ReadFactors(const TString& query) {
        TVector<TDocSearchInfo> results;
        TVector<TSimpleSharedPtr<THashMultiMap<TString, TString>>> resultProps;
        QuerySearch(query, results, &resultProps);
        TVector<TFactors> factors;
        for (const auto& props : resultProps) {
            auto& docFactors = factors.emplace_back();
            docFactors.AssignFromSearchResult(*props);
            docFactors.DebugPrint();
        }
        return factors;
    }

    bool Run() override {
        TVector<NRTYServer::TMessage> messages;
        bool prefixed = GetIsPrefixed();
        {
            NSaas::TAction action;
            action.SetPrefix(prefixed ? 1 : 0);
            action.SetActionType(NSaas::TAction::atModify);
            auto& doc = action.GetDocument();
            doc.SetUrl("kinopoisk.ru/film/1");
            doc.AddFactor("static_factor", 1.0);
            doc.AddZone("z_title").SetText("мстители");
            doc.AddZone("z_original_title").SetText("the avengers");
            messages.push_back(action.ToProtobuf());
        }
        {
            NSaas::TAction action;
            action.SetPrefix(prefixed ? 1 : 0);
            action.SetActionType(NSaas::TAction::atModify);
            auto& doc = action.GetDocument();
            doc.SetUrl("kinopoisk.ru/film/2");
            doc.AddZone("z_title").SetText("мстители война бесконечности");
            doc.AddZone("z_original_title").SetText("avengers infinite war");
            messages.push_back(action.ToProtobuf());
        }

        IndexMessages(messages, DISK, 1);
        ReopenIndexers();

        auto factors = ReadFactors(
            "мстители&relev=tm%3Dkinopoisk&relev=relevgeo%3D225&dbgrlv=da&relev=all_factors&fsgta=_JsonFactors" 
                + Qbundle()
                + EnableTextMachine() 
                + GetAllKps(messages),
            __LINE__
        );
        CHECK_TEST_FAILED(factors.size() != EXPECTED_FACTORS.size(), "Did not find enough document");
        for (size_t i = 0; i < factors.size(); i++) {
            DEBUG_LOG << "Checking document #" << i << Endl;
            for (const auto& [factorName, expectedValue]: EXPECTED_FACTORS[i]) {
                factors[i].CheckFactor(factorName, expectedValue);
            }
        }
            
        return true;
    }
private:
    TString Qbundle() {
        return TString{"&qbundle="} + QBUNDLE;
    }

    TString EnableTextMachine() {
        return "&pron=qbundleiter&pron=qbundleon_All";
    }
};
