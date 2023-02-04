#include <saas/api/search_client/client.h>
#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/rtyserver_test/util/factors_parsers.h>
#include <search/meta/doc_bin_data/ranking_factors_accessor.h>

#include <util/generic/ymath.h>

// 100500I00400000009G0000K120000U7 == 10*Factor[0]+20*Factor[1]+Factors[3]
// 1005002000000G480000Q01 == 10*Factor[0]+20*Factor[1]
// 200100KPC6JS3 == 0.2*Factor[1]
// 10010000000V3 == 1 * Factor[0]

START_TEST_DEFINE(TestFictiveMxNetFactors)
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
                    "MatrixNet" : 3,
                    "FullMatrixNet" : 4,
                    "FastMatrixNet" : 5,
                    "FullPolynom" : 6,
                    "FastPolynom" : 7,
                    "FilterMatrixNet" : 8,
                    "FilterPolynom" : {
                        "index": 9,
                        "default_value": 179
                    },
                    "FastFilterMatrixNet" : 10,
                    "FastFilterPolynom" : 11
                },
                "user_factors": {
                    "user1": {
                        "index": 0,
                        "default_value": 42.0
                    },
                    "user2": {
                        "index": 1,
                        "default_value": 2.0
                    },
                    "user3": {
                        "index": 2,
                        "default_value": 42.0
                    }
                },
                "formulas": {
                    "default": {
                        "polynom": "100500I00400000009G0000K120000U7",
                        "matrixnet": ")" + GetResourcesDirectory() + R"(/text_relev/mxnet_1.info"
                    },
                    "default_no_poly": {
                        "matrixnet": ")" + GetResourcesDirectory() + R"(/text_relev/mxnet_1.info"
                    },
                    "trivial_poly": {
                        "polynom": "40010000000V3",
                        "matrixnet": ")" + GetResourcesDirectory() + R"(/text_relev/mxnet_1.info"
                    },
                    "fast_rank": {
                        "polynom": "100500I00400000009G0000K120000U7",
                        "matrixnet": ")" + GetResourcesDirectory() + R"(/text_relev/mxnet_2.info"
                    },
                    "filter_rank": {
                        "polynom": "100500I00400000009G0000K120000U7",
                        "matrixnet": ")" + GetResourcesDirectory() + R"(/text_relev/mxnet_3.info"
                    },
                    "fast_filter_rank": {
                        "polynom": "100500I00400000009G0000K120000U7",
                        "matrixnet": ")" + GetResourcesDirectory() + R"(/text_relev/mxnet_3.info"
                    }
                }
            })";

        NJson::TJsonValue result;
        Cerr << configBody << Endl;
        NJson::ReadJsonTree(configBody, &result, true /*throwOnError*/);
        return result;
    }

    const TString queryPrefix = "/?text=body&dump=eventlog&ms=proto&service=tests&";

    THolder<NMetaSearch::TDocRankingFactorsAccessor> accessor;

    static TVector<NMetaProtocol::TDocument> parseSearchResultDocuments(TStringBuf searchResult) {
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
    }

    static bool checkFactorValuesEqual(float actual, float expected) {
        return fabs(actual - expected) <= 1e-6 * Max(1.0f, fabs(expected));
    };

    bool getQueryResult(TStringBuf query, TString* searchResult, TFactorStorage** fs, size_t expectedCount) {
        CHECK_TEST_EQ(ProcessQuery(queryPrefix + query, searchResult), expectedCount == 0 ? 404 : 200);
        const TVector<NMetaProtocol::TDocument> resultDocuments = parseSearchResultDocuments(*searchResult);
        CHECK_TEST_EQ(resultDocuments.size(), expectedCount);
        if (expectedCount == 0) {
            Cerr << "got zero documents" << Endl;
            return true;
        } else if (expectedCount == 1) {
            const auto &resultDoc = resultDocuments[0];
            Cerr << "relevance: " << resultDoc.GetRelevance() << Endl;
            // https://a.yandex-team.ru/arc/trunk/arcadia/search/meta/doc.cpp?rev=7162162#L31
            accessor = MakeHolder<NMetaSearch::TDocRankingFactorsAccessor>();
            accessor->Prepare(resultDoc.GetDocRankingFactors(), resultDoc.GetDocRankingFactorsSliceBorders());
            *fs = accessor->RankingFactors();
            CHECK_TEST_TRUE(*fs != nullptr);
            return true;
        }
        return false;
    }

    static void printFactors(const TConstFactorView& factorView, int n) {
        for(int i = 0; i < n; ++i) {
            Cerr << "factors[" << i << "]: " << factorView[i] << Endl;
        }
    }

    bool Run() override {
        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();

        const TString kps{ GetAllKps(messages) };

        TString searchResult;
        TFactorStorage *fs = nullptr;
        {
            // only full matrixnet, factors[0] > 10 -> model value is greater than 0
            const TString query = "dbgrlv=da&allfctrs=da&relev=calc=user1:11" + kps;
            CHECK_TEST_TRUE(getQueryResult(query, &searchResult, &fs, 1));

            TConstFactorView factorView = fs->CreateConstViewFor(EFactorSlice::SAAS_PRODUCTION);
            printFactors(factorView, 12);
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[0], 11.0f));
            CHECK_TEST_TRUE(factorView[3] > 0); // MatrixNet
            CHECK_TEST_TRUE(factorView[4] > 0); // FullMatrixNet
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[5], .0f));
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[6], factorView[4] + 110)); // FullPolynom
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[7], .0f));
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[8], .0f));
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[9], .0f));
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[10], .0f)); // FastFilterMatrixNet
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[11], .0f)); // FastFilterPolynom
        }

        {
            // full + fast matrixnet, factors[0] > 10 -> full matrix net value is greater than 0
            // factors[1] <= 10 -> fast matrix net value is less than 0
            const TString query = "dbgrlv=da&allfctrs=da&relev=calc=user1:11&relev=calc=user2:9&pron=fastcount10" + kps;
            CHECK_TEST_TRUE(getQueryResult(query, &searchResult, &fs, 1));

            TConstFactorView factorView = fs->CreateConstViewFor(EFactorSlice::SAAS_PRODUCTION);
            printFactors(factorView, 12);
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[0], 11.0f));
            CHECK_TEST_TRUE(factorView[3] > 0); // MatrixNet
            CHECK_TEST_TRUE(factorView[4] > 0); // FullMatrixNet
            CHECK_TEST_TRUE(factorView[5] < 0); // FastMatrixNet
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[6], factorView[4] + 11 * 10 + 9 * 20)); // FullPolynom
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[7], factorView[5] + 11 * 10 + 9 * 20)); // FastPolynom
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[8], .0f));
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[9], .0f));
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[10], .0f)); // FastFilterMatrixNet
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[11], .0f)); // FastFilterPolynom
        }

        {
            // full + fast + filter by fullrank model, factors[0] > 10 -> full matrix net value is greater than 0
            // factors[1] <= 10 -> fast matrix net value is less than 0
            // factors[2] > 10 -> filter matrix net value is greater than 0
            const TString query = "dbgrlv=da&allfctrs=da&relev=calc=user1:11&relev=calc=user2:9&relev=calc=user3:11&pron=fastcount10&relev=filter_border=0.5" + kps;
            CHECK_TEST_TRUE(getQueryResult(query, &searchResult, &fs, 1));

            TConstFactorView factorView = fs->CreateConstViewFor(EFactorSlice::SAAS_PRODUCTION);
            printFactors(factorView, 12);
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[0], 11.0f));
            CHECK_TEST_TRUE(factorView[3] > 0); // MatrixNet
            CHECK_TEST_TRUE(factorView[4] > 0); // FullMatrixNet
            CHECK_TEST_TRUE(factorView[5] < 0); // FastMatrixNet
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[6], factorView[4] + 11 * 10 + 9 * 20)); // FullPolynom
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[7], factorView[5] + 11 * 10 + 9 * 20)); // FastPolynom
            CHECK_TEST_TRUE(factorView[8] > 0); // FilterMatrixNet
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[9], factorView[8] + 11 * 10 + 9 * 20)); // FilterPolynom
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[10], .0f)); // FastFilterMatrixNet
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[11], .0f)); // FastFilterPolynom
        }

        //BEGIN FAST_FILTER_TESTS

        {
            // full + fast + filter by fastrank model, factors[0] > 10 -> full matrix net value is greater than 0
            // factors[1] <= 10 -> fast matrix net value is less than 0
            const TString query = "dbgrlv=da&allfctrs=da&relev=calc=user1:11;calc=user2:9;calc=user3:11;fast_filter_border=0.5&pron=fastcount10" + kps;
            CHECK_TEST_TRUE(getQueryResult(query, &searchResult, &fs, 1));

            TConstFactorView factorView = fs->CreateConstViewFor(EFactorSlice::SAAS_PRODUCTION);
            printFactors(factorView, 12);
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[0], 11.0f));
            CHECK_TEST_TRUE(factorView[3] > 0); // MatrixNet
            CHECK_TEST_TRUE(factorView[4] > 0); // FullMatrixNet
            CHECK_TEST_TRUE(factorView[5] < 0); // FastMatrixNet
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[6], factorView[4] + 11 * 10 + 9 * 20)); // FullPolynom
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[7], factorView[5] + 11 * 10 + 9 * 20)); // FastPolynom
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[8], .0f)); // FilterMatrixNet
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[9], .0f)); // FilterPolynom
            CHECK_TEST_TRUE(factorView[10] < 0); // FastFilterMatrixNet
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[11], factorView[10] + 11 * 10 + 9 * 20)); // FastFilterPolynom
        }

        {
            // full + fast + filter by fastrank mxnet
            // factors[1] > 10 -> fast matrixnet > 0
            // fast_fb_mode = mxonly
            const TString query = "dbgrlv=da&allfctrs=da&relev=calc=user1:11;calc=user2:11;calc=user3:11;fast_filter_border=0.5;fast_fb_mode=mxonly&pron=fastcount10" + kps;
            CHECK_TEST_TRUE(getQueryResult(query, &searchResult, &fs, 1));

            TConstFactorView factorView = fs->CreateConstViewFor(EFactorSlice::SAAS_PRODUCTION);
            printFactors(factorView, 12);
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[0], 11.0f));
            CHECK_TEST_TRUE(factorView[3] > 0); // MatrixNet
            CHECK_TEST_TRUE(factorView[4] > 0); // FullMatrixNet
            CHECK_TEST_TRUE(factorView[5] > 0); // FastMatrixNet
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[6], factorView[4] + 11 * 10 + 11 * 20)); // FullPolynom
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[7], factorView[5] + 11 * 10 + 11 * 20)); // FastPolynom
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[8], .0f)); // FilterMatrixNet
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[9], .0f)); // FilterPolynom
            CHECK_TEST_TRUE(factorView[10] > 0); // FastFilterMatrixNet
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[11], .0f)); // FastFilterPolynom
        }

        {
            // full + fast + filter by fastrank model
            // document will be filtered due to high fast_filter_border
            const TString query = "dbgrlv=da&allfctrs=da&relev=calc=user1:11;calc=user2:9;calc=user3:11;fast_filter_border=301.1&pron=fastcount10" + kps;
            CHECK_TEST_TRUE(getQueryResult(query, &searchResult, &fs, 0));
        }

        {
            // full + fast + filter by fastrank mxnet
            // factors[2] <= 10 -> fast filter matrixnet <= 0
            // fast_fb_mode = mxonly
            const TString query = "dbgrlv=da&allfctrs=da&relev=calc=user1:11;calc=user2:9;calc=user3:9;fast_fb_mode=mxonly;fast_filter_border=0.001&pron=fastcount10" + kps;
            CHECK_TEST_TRUE(getQueryResult(query, &searchResult, &fs, 0));
        }

        {
            // singlepass: full + filter by fastrank model
            // document will not be filtered because fast_filter works only on 2-stage relevance calculation mode
            const TString query = "dbgrlv=da&allfctrs=da&relev=calc=user1:11;calc=user2:9;calc=user3:11;fast_filter_border=100500" + kps;
            CHECK_TEST_TRUE(getQueryResult(query, &searchResult, &fs, 1));

            TConstFactorView factorView = fs->CreateConstViewFor(EFactorSlice::SAAS_PRODUCTION);
            printFactors(factorView, 12);
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[0], 11.0f));
            CHECK_TEST_TRUE(factorView[3] > 0); // MatrixNet
            CHECK_TEST_TRUE(factorView[4] > 0); // FullMatrixNet
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[5], .0f)); // FastMatrixNet
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[6], factorView[4] + 11 * 10 + 9 * 20)); // FullPolynom
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[7], .0f)); // FastPolynom
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[8], .0f)); // FilterMatrixNet
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[9], .0f)); // FilterPolynom
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[10], .0f)); // FastFilterMatrixNet
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[11], .0f)); // FastFilterPolynom
        }

        {
            // full + fast + filter matrixnet with custom fast filter model
            // document will not be filtered due to custom fast filter model
            const TString query = "dbgrlv=da&allfctrs=da&relev=calc=user1:11;calc=user2:9;calc=user3:11;fast_filter_border=0.5;"
                                  "fast_filter=100500I00400000009G0000K12000008&pron=fastcount10" + kps; // 10*fFactor[0]+20*fFactor[1]+2*fFactor[3]
            CHECK_TEST_TRUE(getQueryResult(query, &searchResult, &fs, 1));

            TConstFactorView factorView = fs->CreateConstViewFor(EFactorSlice::SAAS_PRODUCTION);
            printFactors(factorView, 12);
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[0], 11.0f));
            CHECK_TEST_TRUE(factorView[3] > 0); // MatrixNet
            CHECK_TEST_TRUE(factorView[4] > 0); // FullMatrixNet
            CHECK_TEST_TRUE(factorView[5] < 0); // FastMatrixNet
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[6], factorView[4] + 11 * 10 + 9 * 20)); // FullPolynom
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[7], factorView[5] + 11 * 10 + 9 * 20)); // FastPolynom
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[8], .0f)); // FilterMatrixNet
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[9], .0f)); // FilterPolynom
            CHECK_TEST_TRUE(factorView[10] > 0); // FastFilterMatrixNet
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[11], 2 * factorView[10] + 11 * 10 + 9 * 20)); // FastFilterPolynom
        }

        {
            // full + fast + filter matrixnet with custom fast filter model
            // document will not be filtered due to custom fast filter model
            const TString query = "dbgrlv=da&allfctrs=da&relev=calc=user1:11;calc=user2:9;calc=user3:11;fast_filter_border=0.5;fast_filter=fast_filter_rank&pron=fastcount10" + kps;
            CHECK_TEST_TRUE(getQueryResult(query, &searchResult, &fs, 1));

            TConstFactorView factorView = fs->CreateConstViewFor(EFactorSlice::SAAS_PRODUCTION);
            printFactors(factorView, 12);
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[0], 11.0f));
            CHECK_TEST_TRUE(factorView[3] > 0); // MatrixNet
            CHECK_TEST_TRUE(factorView[4] > 0); // FullMatrixNet
            CHECK_TEST_TRUE(factorView[5] < 0); // FastMatrixNet
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[6], factorView[4] + 11 * 10 + 9 * 20)); // FullPolynom
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[7], factorView[5] + 11 * 10 + 9 * 20)); // FastPolynom
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[8], .0f)); // FilterMatrixNet
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[9], .0f)); // FilterPolynom
            CHECK_TEST_TRUE(factorView[10] > 0); // FastFilterMatrixNet
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[11], factorView[10] + 11 * 10 + 9 * 20)); // FastFilterPolynom
        }

        {
            // full + fast + filter matrixnet with custom fast filter model
            // document will be filtered due to custom fast filter model
            const TString query = "dbgrlv=da&allfctrs=da&relev=calc=user1:11;calc=user2:9;calc=user3:11;fast_filter_border=313;"
                                  "fast_filter=100500I00400000009G0000K12000008&pron=fastcount10" + kps; // 10*fFactor[0]+20*fFactor[1]+2*fFactor[3]
            CHECK_TEST_TRUE(getQueryResult(query, &searchResult, &fs, 0));
        }

        {
            // full + fast + filter matrixnet with custom fast filter model
            // document will be filtered due to custom fast filter model
            const TString query = "dbgrlv=da&allfctrs=da&relev=calc=user1:11;calc=user2:9;calc=user3:11;"
                                  "fast_filter_border=313;fast_filter=fast_filter_rank&pron=fastcount10" + kps;
            CHECK_TEST_TRUE(getQueryResult(query, &searchResult, &fs, 0));
        }

        //END FAST_FILTER_TESTS

        {
            // full + fast + filter matrixnet with custom filter model. factors[0] > 10 -> full matrix net value is greater than 0
            // factors[1] <= 10 -> fast matrix net value is less than 0
            // factors[2] < 10 -> filter matrix net value is less than 0
            const TString query = "dbgrlv=da&allfctrs=da&relev=calc=user1:11&relev=calc=user2:9&relev=calc=user3:9&pron=fastcount10&relev=filter_border=0.5&relev=filter=filter_rank" + kps;
            CHECK_TEST_TRUE(getQueryResult(query, &searchResult, &fs, 1));

            TConstFactorView factorView = fs->CreateConstViewFor(EFactorSlice::SAAS_PRODUCTION);
            printFactors(factorView, 12);
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[0], 11.0f));
            CHECK_TEST_TRUE(factorView[3] < 0); // MatrixNet < 0 because of custom FilterMatrixNet
            CHECK_TEST_TRUE(factorView[4] > 0); // FullMatrixNet
            CHECK_TEST_TRUE(factorView[5] < 0); // FastMatrixNet
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[6], factorView[4] + 11 * 10 + 9 * 20)); // FullPolynom
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[7], factorView[5] + 11 * 10 + 9 * 20)); // FastPolynom
            CHECK_TEST_TRUE(factorView[8] < 0); // FilterMatrixNet
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[9], factorView[8] + 11 * 10 + 9 * 20)); // FilterPolynom
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[10], .0f)); // FastFilterMatrixNet
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[11], .0f)); // FastFilterPolynom
        }

        // TODO Polynom generator
        {
            // full mxnet doesn't affect fast and filter results
            const TString query = "dbgrlv=da&allfctrs=da&relev=calc=user1:11&relev=calc=user2:2&relev=calc=user3:42&"
                                  "relev=formula=default&relev=polynom=10010000000V3&"
                                  "relev=fast_formula=fast_rank&pron=fastcount10&"
                                  "relev=filter=filter_rank&relev=filter_border=0.5" + kps;
            CHECK_TEST_TRUE(getQueryResult(query, &searchResult, &fs, 1));
            TConstFactorView factorView = fs->CreateConstViewFor(EFactorSlice::SAAS_PRODUCTION);
            printFactors(factorView, 12);
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[0], 11.0f));
            CHECK_TEST_TRUE(factorView[3] > 0); // MatrixNet
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[4], 0.0f)); // FullMatrixNet
            CHECK_TEST_TRUE(factorView[5] < 0); // FastMatrixNet
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[6], 11.0f)); // FullPolynom
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[7], factorView[5] + 11 * 10 + 2 * 20)); // FastPolynom
            CHECK_TEST_TRUE(factorView[8] > 0); // FilterMatrixnet
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[9], factorView[8] + 11 * 10 + 2 * 20)); // FilterPolynom
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[10], .0f)); // FastFilterMatrixNet
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[11], .0f)); // FastFilterPolynom
        }

        {
            // full polynom doesn't affect fast and filter results
            const TString query = "dbgrlv=da&allfctrs=da&relev=calc=user1:11&relev=calc=user2:2&relev=calc=user3:42&"
                                  "relev=formula=default_no_poly&"
                                  "relev=fast_formula=fast_rank&pron=fastcount10&"
                                  "relev=filter=filter_rank&relev=filter_border=0.5" + kps;
            CHECK_TEST_TRUE(getQueryResult(query, &searchResult, &fs, 1));

            TConstFactorView factorView = fs->CreateConstViewFor(EFactorSlice::SAAS_PRODUCTION);
            printFactors(factorView, 12);
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[0], 11.0f));
            CHECK_TEST_TRUE(factorView[3] > 0); // MatrixNet
            CHECK_TEST_TRUE(factorView[4] > 0); // FullMatrixNet
            CHECK_TEST_TRUE(factorView[5] < 0); // FastMatrixNet
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[6], 0.0f)); // FullPolynom
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[7], factorView[5] + 11 * 10 + 2 * 20)); // FastPolynom
            CHECK_TEST_TRUE(factorView[8] > 0); // FilterMatrixnet
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[9], factorView[8] + 11 * 10 + 2 * 20)); // FilterPolynom
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[10], .0f)); // FastFilterMatrixNet
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[11], .0f)); // FastFilterPolynom
        }

        {
            // full polynom doesn't affect fast and filter results
            const TString query = "dbgrlv=da&allfctrs=da&relev=calc=user1:11&relev=calc=user2:2&relev=calc=user3:42&"
                                  "relev=formula=trivial_poly" + kps;
            CHECK_TEST_TRUE(getQueryResult(query, &searchResult, &fs, 1));

            TConstFactorView factorView = fs->CreateConstViewFor(EFactorSlice::SAAS_PRODUCTION);
            printFactors(factorView, 12);/*
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[0], 11.0f));
            CHECK_TEST_TRUE(factorView[3] > 0); // MatrixNet
            CHECK_TEST_TRUE(factorView[4] > 0); // FullMatrixNet
            CHECK_TEST_TRUE(factorView[5] < 0); // FastMatrixNet
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[6], 0.0f)); // FullPolynom
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[7], factorView[5] + 11 * 10 + 2 * 20)); // FastPolynom
            CHECK_TEST_TRUE(factorView[8] > 0); // FilterMatrixnet
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[9], factorView[8] + 11 * 10 + 2 * 20)); // FilterPolynom*/
        }

        {
            // full + filter matrixnet with custom filter model, factors[0] > 10 -> full matrix net value is greater than 0
            // factors[2] < 10 -> filter matrix net value is less than 0
            const TString query = "dbgrlv=da&allfctrs=da&relev=calc=user1:11&relev=calc=user2:9&relev=calc=user3:9&"
                                  "relev=filter=filter_rank&relev=filter_border=0.5" + kps;
            CHECK_TEST_TRUE(getQueryResult(query, &searchResult, &fs, 1));

            TConstFactorView factorView = fs->CreateConstViewFor(EFactorSlice::SAAS_PRODUCTION);
            printFactors(factorView, 12);
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[0], 11.0f));
            CHECK_TEST_TRUE(factorView[3] > 0); // MatrixNet
            CHECK_TEST_TRUE(factorView[4] > 0); // FullMatrixNet
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[5], 0.0f)); // FastMatrixNet
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[6], factorView[4] + 11 * 10 + 9 * 20)); // FullPolynom
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[7], 0.0f)); // FastPolynom
            CHECK_TEST_TRUE(factorView[8] < 0); // FilterMatrixNet
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[9], factorView[8] + 11 * 10 + 9 * 20)); // FilterPolynom
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[10], .0f)); // FastFilterMatrixNet
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[11], .0f)); // FastFilterPolynom
        }

        {
            // full + filter matrixnet with custom filter model, factors[0] > 10 -> full matrix net value is greater than 0
            // factors[0] > 10 and filter==full -> filter matrix net value is greater than 0
            // fb_mode=mxonly -> filter_poly is equal to 0
            const TString query = "dbgrlv=da&allfctrs=da&relev=calc=user1:11&relev=calc=user2:9&"
                                  "relev=filter_border=0.5&relev=fb_mode=mxonly" + kps;
            CHECK_TEST_TRUE(getQueryResult(query, &searchResult, &fs, 1));

            TConstFactorView factorView = fs->CreateConstViewFor(EFactorSlice::SAAS_PRODUCTION);
            printFactors(factorView, 12);
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[0], 11.0f));
            CHECK_TEST_TRUE(factorView[3] > 0); // MatrixNet
            CHECK_TEST_TRUE(factorView[4] > 0); // FullMatrixNet
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[5], 0.0f)); // FastMatrixNet
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[6], factorView[4] + 11 * 10 + 9 * 20)); // FullPolynom
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[7], 0.0f)); // FastPolynom
            CHECK_TEST_TRUE(factorView[8] > 0); // FilterMatrixNet
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[9], 0)); // FilterPolynom
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[10], .0f)); // FastFilterMatrixNet
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[11], .0f)); // FastFilterPolynom
        }

        {
            // full + filter matrixnet with custom filter model, factors[0] > 10 -> full matrix net value is greater than 0
            // factors[2] > 10 and filter!=full -> filter matrix net value is greater than 0
            // fb_mode=default -> filter_poly is not equal to 0
            const TString query = "dbgrlv=da&allfctrs=da&relev=calc=user1:11&relev=calc=user2:9&relev=calc=user3:11&"
                                  "relev=filter=filter_rank&relev=filter_border=0.5" + kps;
            CHECK_TEST_TRUE(getQueryResult(query, &searchResult, &fs, 1));

            TConstFactorView factorView = fs->CreateConstViewFor(EFactorSlice::SAAS_PRODUCTION);
            printFactors(factorView, 12);
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[0], 11.0f));
            CHECK_TEST_TRUE(factorView[3] > 0); // MatrixNet
            CHECK_TEST_TRUE(factorView[4] > 0); // FullMatrixNet
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[5], 0.0f)); // FastMatrixNet
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[6], factorView[4] + 11 * 10 + 9 * 20)); // FullPolynom
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[7], 0.0f)); // FastPolynom
            CHECK_TEST_TRUE(factorView[8] > 0); // FilterMatrixNet
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[9], factorView[8] + 11 * 10 + 9 * 20)); // FilterPolynom
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[10], .0f)); // FastFilterMatrixNet
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[11], .0f)); // FastFilterPolynom
        }

        {
            // full + filter matrixnet with custom filter model, factors[0] > 10 -> full matrix net value is greater than 0
            // factors[2] > 10 and filter!=full -> filter matrix net value is greater than 0
            // fb_mode=mxonly -> filter_poly is equal to 0
            const TString query = "dbgrlv=da&allfctrs=da&relev=calc=user1:11&relev=calc=user2:9&relev=calc=user3:11&"
                                  "relev=filter=filter_rank&relev=filter_border=0.5&relev=fb_mode=mxonly" + kps;
            CHECK_TEST_TRUE(getQueryResult(query, &searchResult, &fs, 1));

            TConstFactorView factorView = fs->CreateConstViewFor(EFactorSlice::SAAS_PRODUCTION);
            printFactors(factorView, 12);
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[0], 11.0f));
            CHECK_TEST_TRUE(factorView[3] > 0); // MatrixNet
            CHECK_TEST_TRUE(factorView[4] > 0); // FullMatrixNet
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[5], 0.0f)); // FastMatrixNet
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[6], factorView[4] + 11 * 10 + 9 * 20)); // FullPolynom
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[7], 0.0f)); // FastPolynom
            CHECK_TEST_TRUE(factorView[8] > 0); // FilterMatrixNet
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[9], 0)); // FilterPolynom
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[10], .0f)); // FastFilterMatrixNet
            CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[11], .0f)); // FastFilterPolynom
        }

        //Test &pron=nomn.  This parameter should disable matrixnet calculation
        {
            const TString factorsQueryPrefix = "/?allfctrs=da&noqtree=1&relev=calc%3Duser1%3A11&ms=proto&dh=0-0";
            auto requestBackend = [&] (const TString& query, TFactorStorage** fs) {
                const TVector<TBackendProxyConfig::TAddress>& controllers = Controller->GetConfig().Controllers;
                CHECK_TEST_TRUE(controllers.size() > 0);
                const auto &backendAddress = controllers[0];
                CHECK_TEST_EQ(Controller->ProcessQuery(query, &searchResult, backendAddress.Host, backendAddress.Port - 3), 200);
                const TVector<NMetaProtocol::TDocument> resultDocuments = parseSearchResultDocuments(searchResult);
                CHECK_TEST_EQ(resultDocuments.size(), 1);
                const auto &resultDoc = resultDocuments[0];
                accessor = MakeHolder<NMetaSearch::TDocRankingFactorsAccessor>();
                accessor->Prepare(resultDoc.GetDocRankingFactors(), resultDoc.GetDocRankingFactorsSliceBorders());
                *fs = accessor->RankingFactors();
                CHECK_TEST_TRUE(*fs != nullptr)
                return true;
            };

            // singlepass + nomn => dont calculate mxnets
            {
                CHECK_TEST_TRUE(requestBackend(factorsQueryPrefix + "&pron=nomn", &fs));
                TConstFactorView factorView = fs->CreateConstViewFor(EFactorSlice::SAAS_PRODUCTION);
                printFactors(factorView, 12);
                CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[4], .0f)); // FullMatrixNet
                CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[5], .0f)); // FastMatrixNet
                CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[8], .0f)); // FilterMatrixNet
                CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[10], .0f)); // FastFilterMatrixNet
                CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[11], .0f)); // FastFilterPolynom
            }

            // singlepass + without nomn => calculate only full mxnet
            {
                CHECK_TEST_TRUE(requestBackend(factorsQueryPrefix, &fs));
                TConstFactorView factorView = fs->CreateConstViewFor(EFactorSlice::SAAS_PRODUCTION);
                printFactors(factorView, 12);
                CHECK_TEST_TRUE(factorView[4] > 0); // FullMatrixNet
                CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[5], .0f)); // FastMatrixNet
                CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[8], .0f)); // FilterMatrixNet
                CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[10], .0f)); // FastFilterMatrixNet
                CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[11], .0f)); // FastFilterPolynom
            }

            // two pass + nomn => dont calculate mxnets
            {
                CHECK_TEST_TRUE(requestBackend(factorsQueryPrefix + "&pron=nomn&pron=fastcount10", &fs));
                TConstFactorView factorView = fs->CreateConstViewFor(EFactorSlice::SAAS_PRODUCTION);
                printFactors(factorView, 12);
                CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[4], .0f)); // FullMatrixNet
                CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[5], .0f)); // FastMatrixNet
                CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[8], .0f)); // FilterMatrixNet
                CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[10], .0f)); // FastFilterMatrixNet
                CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[11], .0f)); // FastFilterPolynom
            }

            // two pass + without nomn => calculate full + fast mxnets
            {
                CHECK_TEST_TRUE(requestBackend(factorsQueryPrefix + "&pron=fastcount10", &fs));
                TConstFactorView factorView = fs->CreateConstViewFor(EFactorSlice::SAAS_PRODUCTION);
                printFactors(factorView, 12);
                CHECK_TEST_TRUE(factorView[4] > 0); // FullMatrixNet
                CHECK_TEST_TRUE(factorView[5] < 0); // FastMatrixNet
                CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[8], .0f)); // FilterMatrixNet
                CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[10], .0f)); // FastFilterMatrixNet
                CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[11], .0f)); // FastFilterPolynom
            }

            // two pass + filter + nomn => calculate only filter mxnet because we need it to filter
            {
                CHECK_TEST_TRUE(requestBackend(factorsQueryPrefix + "&pron=nomn&pron=fastcount10&relev=filter_border=0.5&relev=filter=filter_rank", &fs));
                TConstFactorView factorView = fs->CreateConstViewFor(EFactorSlice::SAAS_PRODUCTION);
                printFactors(factorView, 12);
                CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[4], .0f)); // FullMatrixNet
                CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[5], .0f)); // FastMatrixNet
                CHECK_TEST_TRUE(factorView[8] < 0); // FilterMatrixNet
                CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[10], .0f)); // FastFilterMatrixNet
                CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[11], .0f)); // FastFilterPolynom
            }

            // two pass + filter + without nomn  => calculate each kind of mxnets
            {
                CHECK_TEST_TRUE(requestBackend(factorsQueryPrefix + "&pron=fastcount10&relev=filter_border=0.5&relev=filter=filter_rank", &fs));
                TConstFactorView factorView = fs->CreateConstViewFor(EFactorSlice::SAAS_PRODUCTION);
                printFactors(factorView, 12);
                CHECK_TEST_TRUE(factorView[4] > 0); // FullMatrixNet
                CHECK_TEST_TRUE(factorView[5] < 0); // FastMatrixNet
                CHECK_TEST_TRUE(factorView[8] < 0); // FilterMatrixNet
                CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[10], .0f)); // FastFilterMatrixNet
                CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[11], .0f)); // FastFilterPolynom
            }

            // two pass + filter + fb_mode=mxonly + nomn => calculate only full mxnet because we use it to filter
            {
                CHECK_TEST_TRUE(requestBackend(factorsQueryPrefix + "&pron=nomn&pron=fastcount10&relev=filter_border=0.5;fb_mode=mxonly", &fs));
                TConstFactorView factorView = fs->CreateConstViewFor(EFactorSlice::SAAS_PRODUCTION);
                printFactors(factorView, 12);
                CHECK_TEST_TRUE(factorView[4] > 0); // FullMatrixNet
                CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[5], .0f)); // FastMatrixNet
                CHECK_TEST_TRUE(factorView[8] > 0); // FilterMatrixNet
                CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[10], .0f)); // FastFilterMatrixNet
                CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[11], .0f)); // FastFilterPolynom
            }

            // two pass + filter + fb_mode=mxonly + without nomn  => calculate each kind of mxnets
            {
                CHECK_TEST_TRUE(requestBackend(factorsQueryPrefix + "&pron=fastcount10&relev=filter_border=0.5;fb_mode=mxonly", &fs));
                TConstFactorView factorView = fs->CreateConstViewFor(EFactorSlice::SAAS_PRODUCTION);
                printFactors(factorView, 12);
                CHECK_TEST_TRUE(factorView[4] > 0); // FullMatrixNet
                CHECK_TEST_TRUE(factorView[5] < 0); // FastMatrixNet
                CHECK_TEST_TRUE(factorView[8] > 0); // FilterMatrixNet
                CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[10], .0f)); // FastFilterMatrixNet
                CHECK_TEST_TRUE(checkFactorValuesEqual(factorView[11], .0f)); // FastFilterPolynom
            }
        }

        return true;
    }
};
