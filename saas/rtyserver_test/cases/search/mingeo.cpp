#include <saas/api/search_client/client.h>
#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/rtyserver_test/util/factors_parsers.h>

#include <library/cpp/geo/geo.h>

#include <util/system/tempfile.h>
#include <util/generic/ymath.h>


SERVICE_TEST_RTYSERVER_DEFINE(TestMingeoHelper)
    bool InitConfig() override {
        (*ConfigDiff)["Searcher.FactorsInfo"] = WriteConfigFile<EConfigType::Factors>(CreateFactorsConfigTemplate());
        (*ConfigDiff)["Components"] = INDEX_COMPONENT_NAME "," FULL_ARCHIVE_COMPONENT_NAME ",MinGeo";
        (*ConfigDiff)["ComponentsConfig." FULL_ARCHIVE_COMPONENT_NAME ".ActiveLayers"] = "base,full";
        (*ConfigDiff)["ComponentsConfig." FULL_ARCHIVE_COMPONENT_NAME ".LightLayers"] = "MinGeo";
        (*ConfigDiff)["Indexer.Memory.Enabled"] = "false";
        return true;
    }

    static NJson::TJsonValue CreateFactorsConfigTemplate() {
        static const TStringBuf configBody = R"({
            "user_factors": {
                "user1": 0,
                "user2": 1
            },
            "user_functions": {
                "min_distance_to": "MinGeo",
                "hit_the_area": "MinGeo",
                "max_intersection_to": "MinGeo",
                "max_relintersection_to": "MinGeo",
                "min_rectangle_geo_dist_to": "MinGeo"
            },
            "formulas": {
                "default": {
                    "polynom": "10010000000V3"
                }
            },
            "geo_layers": {
                "coords": {
                    "stream_id": 0
                },
                "zones": {
                    "stream_id": 1
                },
                "ids": {
                    "stream_id": 254
                }
            }
        })";
        NJson::TJsonValue result;
        NJson::ReadJsonTree(configBody, &result, true /*throwOnError*/);
        return result;
    }

void CheckUserFactor(const TString& comment, const TString& kps, const TString& userFactorFormula, double correctValue) {
    TVector<TDocSearchInfo> results;
    TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > > resultProps;
    QuerySearch("body&dbgrlv=da&fsgta=_JsonFactors&relev=calc=user1:" + userFactorFormula + kps, results, &resultProps);
    if (results.size() != 1)
        ythrow yexception() << comment << "(" << userFactorFormula << "): incorrect count: " << results.size() << " != 1";
    THashMap<TString, double> factors = TRTYFactorsParser::GetJsonFactorsValues(resultProps)[0];
    THashMap<TString, double>::const_iterator i = factors.find("user1");
    if (i == factors.end())
        ythrow yexception() << "there is no user1 factor";
    if (fabs(i->second - correctValue) > 1e-6 * Max(1.0, fabs(correctValue)))
        ythrow yexception() << "incorrect value: " << i->second << " != " << correctValue;
    DEBUG_LOG << userFactorFormula << " == " << correctValue << "... OK" << Endl;
}
};

START_TEST_DEFINE_PARENT(TestMingeoBasics, TestMingeoHelper)
void GenMessages(TVector<NRTYServer::TMessage>& messages) {
    GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    {
        //add points to message
        auto* protoGeo = messages[0].MutableDocument()->MutableGeoData();
        auto* layer = protoGeo->AddLayers();
        layer->SetLayer("coords");
        auto* obj = layer->AddGeoDoc();
        obj->SetType(ToString(NSaas::EGeoObjectKind::PointSet));
        obj->AddData(23.1);
        obj->AddData(43.9);
        obj->AddData(-34.1);
        obj->AddData(55.16);

        layer = protoGeo->AddLayers();
        layer->SetLayer("zones");
        obj = layer->AddGeoDoc();
        obj->SetType(ToString(NSaas::EGeoObjectKind::RectSet));
        obj->AddData(10);
        obj->AddData(10);
        obj->AddData(11);
        obj->AddData(12);
        obj->AddData(50);
        obj->AddData(55);
        obj->AddData(70);
        obj->AddData(75);

        layer = protoGeo->AddLayers();
        layer->SetLayer("ids");
        obj = layer->AddGeoDoc();
        obj->SetType(ToString(NSaas::EGeoObjectKind::SpecialPointSet));
        obj->AddData(23.1);
        obj->AddData(43.8);
    }
}

void Check(const TString& comment, const TString& kps) {
    CheckUserFactor(comment, kps, "min_distance_to(-35.1,54.16)", 1.41421f);
    CheckUserFactor(comment, kps, "min_distance_to(23.1,43.65)", 0.25f);
    CheckUserFactor(comment, kps, "min_distance_to(23.1,43.9)", 0.f);
    CheckUserFactor(comment, kps, "min_distance_to(100000,43.9)", 100000.f - 23.1f);

    CheckUserFactor(comment, kps, "min_distance_to(#geo_ids,23.1,43.65)", 0.149998f); // 23.1:43.8 is selected
    CheckUserFactor(comment, kps, "min_distance_to(#geo_coords,23.1,43.65)", 0.25f); // 23.1:43.8 is not selected, because it is on another layer
    CheckUserFactor(comment, kps, "min_distance_to(#geo_zones,10.5,11.125)", 0.125f); // 10.5:11.0 is the center of rectangle

    CheckUserFactor(comment, kps, "hit_the_area(#geo_coords,23.0,43.0,24.0,44.0)", 1.f);
    CheckUserFactor(comment, kps, "hit_the_area(#geo_coords,0.0,0.0,1.0,1.0)", 0.f);

    CheckUserFactor(comment, kps, "max_intersection_to(#geo_zones,10.5,11.0,11.0,12.0)", 0.5f);
    CheckUserFactor(comment, kps, "max_intersection_to(#geo_zones,0.0,0.0,15.0,15.0)", 2.f);

    CheckUserFactor(comment, kps, "max_relintersection_to(#geo_zones,50.0,55.0,60.0,65.0)", 0.25f);
    CheckUserFactor(comment, kps, "max_relintersection_to(#geo_zones,25.0,25.0,80.0,800.0)", 1.f);

    CheckUserFactor(comment, kps, "min_rectangle_geo_dist_to(#geo_zones,50.0,55.0,60.0,65.0)", 0.f);
    CheckUserFactor(comment, kps, "min_rectangle_geo_dist_to(#geo_zones,75.0,55.0,80.0,65.0)", 2.5f);
    CheckUserFactor(comment, kps, "min_rectangle_geo_dist_to(#geo_zones,55.0,80.0,70.0,85.0)", 5.f);
    CheckUserFactor(comment, kps, "min_rectangle_geo_dist_to(#geo_zones,75.0,80.0,80.0,85.0)", 5.11577f);
}


bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    GenMessages(messages);

    IndexMessages(messages, DISK, 1);
    const TString kps(GetAllKps(messages));
    ReopenIndexers();
    Check("disk", kps);
    return true;
}
};

SERVICE_TEST_RTYSERVER_DEFINE_PARENT(TestMingeoInvBase, TestMingeoHelper)
public:
static constexpr ui32 DocCount = 200;
static constexpr ui32 SegmentSize = Max<ui32>(10, DocCount / 4);

bool InitConfig() override {
    if (!TestMingeoHelper::InitConfig())
        return false;

    (*ConfigDiff)["ComponentsConfig." FULL_ARCHIVE_COMPONENT_NAME ".ActiveLayers"] = "base,MinGeo";
    (*ConfigDiff)["Indexer.Memory.Enabled"] = "false";
    SetIndexerParams(DISK, SegmentSize);
    SetMergerParams(true, 1, 1, mcpNONE);
    return true;
}


static TVector<NGeo::TGeoWindow> MakeFlowerCoords(ui32 docCount) {
    using namespace NGeo;
    const TMercatorPoint yandexHQ = LLToMercator(TGeoPoint(/*lon=*/37.6155, /*lat=*/55.7522));
    const double metricRadius = 12000; // 12 km
    const TSize metricSize{5000, 5000};

    const double angleStep = (2 * M_PI) * 42 / 98;
    TVector<NGeo::TGeoWindow> geoObjects;
    for (double angle = 0; docCount; angle += angleStep, --docCount) {
        const std::pair<double, double> vec{metricRadius * cos(angle), metricRadius * sin(angle)};
        const TMercatorPoint center(yandexHQ.X() + vec.first, yandexHQ.Y() + vec.second);
        const TMercatorWindow rectangle(center, metricSize);

        geoObjects.push_back(MercatorToLL(rectangle));
    }

    return geoObjects; // This is a flower. Engineers like flowers, don't we?
}

static TString FormatCoords(const NGeo::TGeoWindow& rectangle) {
    TStringStream s;
    s << rectangle.GetLowerLeftCorner().ToString(":") << ";" << rectangle.GetUpperRightCorner().ToString(":");
    return s.Str();
}

void GenMessages(TVector<NRTYServer::TMessage>& messages) {
    const bool isPrefixed = GetIsPrefixed();
    GenerateInput(messages, DocCount, NRTYServer::TMessage::ADD_DOCUMENT, isPrefixed);
    TVector<NGeo::TGeoWindow> coords = MakeFlowerCoords(DocCount);

    Y_VERIFY(coords.size() == messages.size());


    for (ui32 i = 0; i < messages.size(); ++i) {
        //add points to message
        auto& document = *messages[i].MutableDocument();
        auto& attr = *document.AddDocumentProperties();
        attr.set_name("coords");
        attr.set_value(FormatCoords(coords[i]));

        // fix KPSes in messages (by default, there is 1 message per KPS)
        if (isPrefixed) {
           document.SetKeyPrefix(1000 + (i % 5));
        }
    }
}

bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    GenMessages(messages);

    IndexMessages(messages, DISK, /*copiesNumber=*/1);
    const TString kps(GetAllKps(messages));
    ReopenIndexers();

    if (!SendIndexReply) {
        Controller->WaitEmptyIndexingQueues();
        sleep(10);
    }


    Controller->ProcessCommand("create_merger_tasks");
    Controller->ProcessCommand("do_all_merger_tasks");

    TVector<TDocSearchInfo> results;
    const TString kpsArg = GetAllKps(messages);

    using namespace NGeo;

    // without geo
    QuerySearch("url:\"*\"&numdoc=250" + kpsArg, results);
    CHECK_TEST_EQ(200u, results.size());

    // a "whole world" window
    QuerySearch("url:\"*\"&numdoc=250&comp_search=comp:geo;lonlat:0,0;spn:360,180" + kpsArg, results);
    CHECK_TEST_EQ(200u, results.size());

    // a specific window
    TGeoPoint yandexHQ(/*lon=*/37.6155, /*lat=*/55.7522);
    TMercatorPoint flowerCenter = LLToMercator(yandexHQ);
    TGeoPoint easternPoint = MercatorToLL(TMercatorPoint(flowerCenter.X() + 100000, flowerCenter.Y()));
    double geoDist = abs(easternPoint.Lon() - yandexHQ.Lon());
    TSize span = {geoDist * 2, geoDist * 2};

    QuerySearch("url:\"*\"&numdoc=250&comp_search=comp:geo;lonlat:" + easternPoint.ToString(",") + ";spn:" + span.ToCgiStr() + kpsArg, results);
    CHECK_TEST_EQ(86u, results.size());

    // an automatically enlarged window (prne)
    QuerySearch("url:\"*\"&numdoc=250&comp_search=comp:geo;lonlat:" + easternPoint.ToString(",") + ";prne:1000000" + kpsArg, results);
    CHECK_TEST_EQ(200u, results.size());
    if (!GetIsPrefixed()) {
        QuerySearch("url:\"*\"&numdoc=250&comp_search=comp:geo;lonlat:" + easternPoint.ToString(",") + ";prne:10" + kpsArg, results);
        CHECK_TEST_EQ(29u, results.size());
        QuerySearch("url:\"*\"&numdoc=250&comp_search=comp:geo;lonlat:" + easternPoint.ToString(",") + ";prne:110" + kpsArg, results);
        CHECK_TEST_EQ(143u, results.size());
        QuerySearch("url:\"*\"&numdoc=250&comp_search=comp:geo;lonlat:" + easternPoint.ToString(",") + ";prne:120;maxspn:" + span.ToCgiStr() + kpsArg, results);
        CHECK_TEST_EQ(86u, results.size()); // maxSpn reached, so less that 120
    } else {
        INFO_LOG << "Some checks were skipped" << Endl; // when IsPrefixed, pruning is calculated separatedly on each shard, so we skip some tests
        // run a query for 2 (out of 5) kps, and expect more than 5*2 documents
        QuerySearch("url:\"*\"&numdoc=250&comp_search=comp:geo;lonlat:" + easternPoint.ToString(",") + ";prne:5" + "&kps=1000,1001", results);
        CHECK_TEST_EQ(12u, results.size());
    }

    // a window that is either enlarged or scaled down (prn)
    if (!GetIsPrefixed()) {
        QuerySearch("url:\"*\"&numdoc=250&comp_search=comp:geo;lonlat:" + easternPoint.ToString(",") + ";prn:27;spn:" + span.ToCgiStr() + kpsArg, results);
        CHECK_TEST_EQ(27u, results.size());
        QuerySearch("url:\"*\"&numdoc=250&comp_search=comp:geo;lonlat:" + easternPoint.ToString(",") + ";prn:27;maxspn:" + span.ToCgiStr() + kpsArg, results);
        CHECK_TEST_EQ(27u, results.size());
    }

    // a window that gives zero results ("center of the flower")
    TGeoWindow zeroResWindow = MercatorToLL(TMercatorWindow(flowerCenter, TSize(2000, 2000)));
    QuerySearch("url:\"*\"&numdoc=250&comp_search=comp:geo;lonlat:" + zeroResWindow.GetCenter().ToString(",") + ";spn:" + zeroResWindow.GetSize().ToCgiStr() + kpsArg, results);
    CHECK_TEST_EQ(0u, results.size());

    return true;
}
};

START_TEST_DEFINE_PARENT(TestMingeoInv, TestMingeoInvBase)
bool InitConfig() override {
    if (!TestMingeoInvBase::InitConfig())
        return false;
    TString cacheDir = GetRunDir() + "/cache";
    TFsPath(cacheDir).ForceDelete();
    (*ConfigDiff)["Searcher.QueryCache.Dir"] = cacheDir;
    SetSearcherParams(abTRUE, "3600s", "");

    return true;
}
};
