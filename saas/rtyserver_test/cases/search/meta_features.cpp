#include <saas/api/search_client/client.h>
#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/rtyserver_test/util/factors_parsers.h>
#include <search/meta/doc_bin_data/ranking_factors_accessor.h>

#include <util/generic/ymath.h>

START_TEST_DEFINE(TestFactorsInSaasSlice)
    bool InitConfig() override {
        (*ConfigDiff)["Searcher.FactorsInfo"] = WriteConfigFile<EConfigType::Factors>(CreateFactorsConfigTemplate());
        (*ConfigDiff)["Components"] = INDEX_COMPONENT_NAME "," FULL_ARCHIVE_COMPONENT_NAME;
        (*ConfigDiff)["ComponentsConfig." FULL_ARCHIVE_COMPONENT_NAME ".ActiveLayers"] = "base,full";
        (*ConfigDiff)["Indexer.Memory.Enabled"] = "false";
        return true;
    }

    static NJson::TJsonValue CreateFactorsConfigTemplate() {
        static const TStringBuf configBody = R"({
                "user_factors": {
                    "user1": {
                        "index": 0,
                        "default_value": 42.0
                    },
                    "user2": 1
                },
                "formulas": {
                    "default": {
                        "polynom": "10010000000V3"
                    }
                }
            })";
        NJson::TJsonValue result;
        NJson::ReadJsonTree(configBody, &result, true /*throwOnError*/);
        return result;
    }

    bool Run() override {
        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();

        const TString kps{ GetAllKps(messages) };
        const TString query = "dbgrlv=da&allfctrs=da&relev=calc=user1:10" + kps;

        auto parseSearchResultDocuments = [](TStringBuf searchResult) -> TVector<NMetaProtocol::TDocument> {
            NMetaProtocol::TReport report;
            ::google::protobuf::io::CodedInputStream decoder((ui8*)searchResult.data(), searchResult.size());
            decoder.SetTotalBytesLimit(1 << 29);
            Y_ENSURE(report.ParseFromCodedStream(&decoder) && decoder.ConsumedEntireMessage());

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
        CHECK_TEST_EQ(ProcessQuery("/?text=body&dump=eventlog&ms=proto&service=tests&" + query, &searchResult), 200);

        const TVector<NMetaProtocol::TDocument> resultDocuments = parseSearchResultDocuments(searchResult);
        CHECK_TEST_EQ(resultDocuments.size(), 1);

        const auto& resultDoc = resultDocuments[0];

        // https://a.yandex-team.ru/arc/trunk/arcadia/search/meta/doc.cpp?rev=7162162#L31
        THolder<NMetaSearch::TDocRankingFactorsAccessor> accessor = MakeHolder<NMetaSearch::TDocRankingFactorsAccessor>();
        accessor->Prepare(resultDoc.GetDocRankingFactors(), resultDoc.GetDocRankingFactorsSliceBorders());
        TFactorStorage* fs = accessor->RankingFactors();
        CHECK_TEST_TRUE(fs != nullptr);

        auto checkFactorValuesEqual = [](float actual, float expected) -> bool {
            return fabs(actual - expected) <= 1e-6 * Max(1.0f, fabs(expected));
        };

        TConstFactorView factorView = fs->CreateConstViewFor(EFactorSlice::SAAS_PRODUCTION);
        CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[0], 10.0f));
        CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[1], 0.0f));
        CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[1000], 0.0f));

        CHECK_TEST_EQ(factorView.GetPrintableFactorName(0), "relev_index_0");
        CHECK_TEST_EQ(factorView.GetPrintableFactorName(1), "relev_index_1");
        return true;
    }
};

SERVICE_TEST_RTYSERVER_DEFINE(TestL3Rank)
    static NJson::TJsonValue CreateFactorsConfigTemplate() {
        static const TStringBuf configBody = R"({
            "static_factors":{
                "stat1": 1
            },
            "user_factors": {
                "user1": {
                    "index": 0
                },
                "user2": {
                    "index": 3
                }
            },
            "dynamic_factors" : {
                "MetaMatrixNet" : 4,
                "MetaPolynom" : 5
            },
            "formulas":{
                "default":{
                    "polynom":"200100KPC6JS3"
                },
                "fast_rank": {
                    "polynom":"10010000000V3"
                },
                "l3_rank":{
                    "polynom":"100500I00400000009G0000K120000U7"
                },
                "l3_rank_alt":{
                    "polynom":"1005002000000G480000Q01"
                }
            }
        })";
        // 100500I00400000009G0000K120000U7 == 10*Factor[0]+20*Factor[1]+Factors[3]
        // 1005002000000G480000Q01 == 10*Factor[0]+20*Factor[1]
        // 200100KPC6JS3 == 0.2*Factor[1]
        // 10010000000V3 == 1 * Factor[0]

        NJson::TJsonValue result;
        NJson::ReadJsonTree(configBody, &result, true /*throwOnError*/);
        return result;
    }

    bool InitConfigBase(TString noFactorsRequest) {
        const TString relevConf = WriteConfigFile<EConfigType::Factors>(CreateFactorsConfigTemplate());
        TStringStream mrOptions;
        mrOptions << "Enabled=yes, Default=yes, Groups=10, Docs=300, NoFactorsRequest=" << noFactorsRequest <<", GoodDocs=no";
        (*SPConfigDiff)["SearchConfig.MetaRankingOptions"] = mrOptions.Str();
        (*SPConfigDiff)["SearchConfig.RearrangeOptions"] = "RTYL3";
        (*SPConfigDiff)["Service.FactorsInfo"] = relevConf;

        (*ConfigDiff)["Searcher.FactorsInfo"] = relevConf;
        (*ConfigDiff)["Searcher.AskFactorsOnSearchStage"] = noFactorsRequest;
        return true;
    }

    static TVector<float> ParseFactors(TSimpleSharedPtr<THashMultiMap<TString, TString>> docProps) {
        const TString& factors = docProps->find("__docRankingFactors")->second;
        const TString& slices = docProps->find("__docRankingFactorsSliceBorders")->second;
        NMetaSearch::TDocRankingFactorsAccessor accessor;
        accessor.Prepare(factors, slices);
        TFactorStorage* fs = accessor.RankingFactors();
        TConstFactorView factorView = fs->CreateConstViewFor(EFactorSlice::SAAS_PRODUCTION);
        TVector<float> result { factorView.begin(), factorView.end() };
        return result;
    };

    static bool CheckFactorValuesEqual(float actual, float expected) {
        return fabs(actual - expected) <= 1e-6 * Max(1.0f, fabs(expected));
    };

    bool Run() override {
        CHECK_TEST_TRUE(HasSearchproxy());

        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
        const TString kps = GetAllKps(messages);
        const TString QueryPattern = "body&dbgrlv=da&allfctrs=da&relev=calc=user1:10&gta=stat1&relev=calc=user2:5"; // for now we should manually request factors for L3
        for (int i = 0; i < messages.ysize(); i++) {
            NSaas::AddSimpleFactor("stat1", ToString(1.0f), *messages[i].MutableDocument()->MutableFactors());
            messages[i].MutableDocument()->SetKeyPrefix(messages[0].GetDocument().GetKeyPrefix());
        }
        IndexMessages(messages, REALTIME, 1);

        // case 1: use default l3_rank formula
        {
            TVector<TDocSearchInfo> results;
            TQuerySearchContext::TDocProperties docProps;
            QuerySearch(QueryPattern + kps, results, &docProps, nullptr, true);
            CHECK_TEST_EQ(results.size(), 1);
            CHECK_TEST_EQ(results[0].GetRelevance(), 1350000000); // 100'000'000 + 125 * 10'000'000

            // CHECK_TEST_EQ(docProps.size(), 1);
            // TVector<float> factors = ParseFactors(docProps[0]);
            // CHECK_TEST_TRUE(factors.size() >= 6);
            // CHECK_TEST_TRUE(CheckFactorValuesEqual(factors[4], 0.0f)); //MetaMatrixNet
            // CHECK_TEST_TRUE(CheckFactorValuesEqual(factors[5], 125.0f)); //MetaPolynom
        }

        // case 2: use custom l3_rank formula
        {
            TVector<TDocSearchInfo> results;
            TQuerySearchContext::TDocProperties docProps;
            QuerySearch(QueryPattern + TString("&relev=l3_formula=l3_rank_alt") + kps, results, &docProps, nullptr, true);
            CHECK_TEST_EQ(results.size(), 1);
            CHECK_TEST_EQ(results[0].GetRelevance(), 1300000000); // 100'000'000 + 120 * 10'000'000

            // CHECK_TEST_EQ(docProps.size(), 1);
            // TVector<float> factors = ParseFactors(docProps[0]);
            // CHECK_TEST_TRUE(factors.size() >= 6);
            // CHECK_TEST_TRUE(CheckFactorValuesEqual(factors[4], 0.0f)); //MetaMatrixNet
            // CHECK_TEST_TRUE(CheckFactorValuesEqual(factors[5], 120.0f)); //MetaPolynom
        }
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestL3RankNoFactorsRequest, TestL3Rank)
public:
    bool InitConfig() override {
        return InitConfigBase("yes" /*noFactorsRequest*/);
    }
};

START_TEST_DEFINE_PARENT(TestL3RankFactorsRequest, TestL3Rank)
public:
    bool InitConfig() override {
        return InitConfigBase("no" /*noFactorsRequest*/);
    }
};
