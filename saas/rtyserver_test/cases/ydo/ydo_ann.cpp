#include <saas/rtyserver_test/cases/indexer/ann.h>

#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/rtyserver_test/util/factors_parsers.h>

#include <saas/rtyserver/components/ann/const.h>

#include <saas/library/robot/ann/prepare_ann.h>

#include <saas/api/action.h>

#include <library/cpp/json/json_reader.h>

using TAnnFormats = NSaas::TAnnFormats;

START_TEST_DEFINE(TestYdoAnn)
private:
    static const TStringBuf Doc1;

public:
    using TFactors = NSaas::TDocFactorsView;

public:
    bool InitConfig() override {
        (*ConfigDiff)["Searcher.FactorsInfo"] = GetResourcesDirectory() + "/text_search/ydo/relev.conf-ydo_search";
        (*ConfigDiff)["Components"] = "INDEX,FULLARC,ANN,MinGeo,FASTARC,STATIC_FILES";
        (*ConfigDiff)["Indexer.Memory.Enabled"] = "false";
        (*ConfigDiff)["Indexer.Common.OxygenOptionsFile"] = GetResourcesDirectory() + "/text_search/ydo/shardfiles.conf-ydo_search";
        (*ConfigDiff)["Indexer.Common.Groups"] = "popularity:2:unique geoa:2:unique";
        (*ConfigDiff)["ComponentsConfig.FULLARC.ActiveLayers"] = "base";
        (*ConfigDiff)["ComponentsConfig.FULLARC.LightLayers"] = "MinGeo";
        (*ConfigDiff)["ComponentsConfig." + NRTYServer::AnnComponentName + ".DataType"] = "PLAIN_ARRAY";
        (*ConfigDiff)["ComponentsConfig." + NRTYServer::AnnComponentName + ".ImitateQrQuorum"] = "false";
        (*ConfigDiff)["ComponentsConfig." + NRTYServer::AnnComponentName + ".UseExternalCalcer"] = "true";
        (*ConfigDiff)["ComponentsConfig." + NRTYServer::AnnComponentName + ".UseRegionChain"] = "true";
        return true;
    }

    void QuerySearchL(TString query, TVector<TDocSearchInfo>& results, TVector<TSimpleSharedPtr<THashMultiMap<TString, TString>>>* resultProps, int line) {
        DEBUG_LOG << "L" << line <<" Query: " << query << Endl;
        QuerySearch(query, results, resultProps);
    }

    void ReadFactors(TFactors& factors, const TString& query, const TString& kps, bool deleted, int line) {
        DEBUG_LOG << "L" << line <<" Query: " << query << Endl;
        ReadFactors(factors, query, kps, deleted);
    }

    void ReadFactors(TFactors& factors, const TString& query, const TString& kps, bool deleted) {
        TVector<TDocSearchInfo> results;
        TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > > resultProps;
        TString textSearch = query;
        Quote(textSearch);
        QuerySearch(textSearch + "&dbgrlv=da&relev=all_factors&fsgta=_JsonFactors&" + kps, results, &resultProps);
        if (deleted) {
            if (results.size() != 0)
                ythrow yexception() << "Documents found when there should be none";
            factors.Clear();
        } else {
            if (results.size() != 1)
                ythrow yexception() << "No documents found";
            factors.AssignFromSearchResult(*resultProps[0]);
        }
        factors.DebugPrint();
    }

    bool Run() override {
        TVector<NRTYServer::TMessage> messages;

        NRTYServer::TMessage mes = NSaas::TAction().ParseFromJson(NJson::ReadJsonFastTree(Doc1)).ToProtobuf();

        auto* doc = mes.MutableDocument();
        doc->SetKeyPrefix(0);
        doc->MutableAnnData()->Clear();

        NIndexAnn::TIndexAnnSiteData annData;
        {
            NIndexAnn::TAnnotationRec& annRec = *annData.AddRecs();
            annRec.SetText("отремонтировать карбюратор в ростове на дону");
            annRec.SetTextLanguage(1);
            NIndexAnn::TRegionData& annReg = *annRec.AddData();
            annReg.SetRegion(39); // City (Rostov-on-Don)
            annReg.MutableUserQueryUrl()->SetCorrectedCtr(0.2);
            annReg.MutableClickMachine()->SetOneClick(0.3);
            annReg.MutableClickMachine()->SetLongClick(0.04);
        }
        {
            NIndexAnn::TAnnotationRec& annRec = *annData.AddRecs();
            annRec.SetText("отремонтировать карбюратор в ростове на дону");
            annRec.SetTextLanguage(1);
            NIndexAnn::TRegionData& annReg = *annRec.AddData();
            annReg.SetRegion(225); // Country (Russia)
            annReg.MutableUserQueryUrl()->SetCorrectedCtr(0.05);
            annReg.MutableClickMachine()->SetOneClick(0.1);
            annReg.MutableClickMachine()->SetLongClick(0.01);
        }
        NIndexAnn::UnpackToSaasData(*doc->MutableAnnData(), annData, NIndexAnn::GetSaasProfile());
        messages.emplace_back(std::move(mes));
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();
        CheckSearchResults(messages);

        TVector<TDocSearchInfo> results;
        TVector<TSimpleSharedPtr<THashMultiMap<TString, TString>>> resultProps;

        QuerySearchL("ремонт карбюратора&kps=13", results, &resultProps, __LINE__);
        CHECK_WITH_LOG(results.size() == 1);

        TFactors factors;
        ReadFactors(factors, "ремонт карбюратора", "kps=13&relev=relevgeo=39", false, __LINE__);
        factors.CheckFactor("CorrectedCtrBm15V4K5", 0.787); // relevgeo 39 is a city (Rostov-on-Don)
        factors.CheckFactor("LongClickBm15AK4", 0.638);
        ReadFactors(factors, "ремонт карбюратора", "kps=13&relev=relevgeo=108163", false, __LINE__);
        factors.CheckFactor("CorrectedCtrBm15V4K5", 0.787); // relevgeo 108163 is a child of 39
        factors.CheckFactor("LongClickBm15AK4", 0.638);
        ReadFactors(factors, "ремонт карбюратора", "kps=13&relev=relevgeo=213", false, __LINE__);
        factors.CheckFactor("CorrectedCtrBm15V4K5", 0.036); // relevgeo 213 is another city (Moscow) - will use the country-level annotation (region=225)
        factors.CheckFactor("LongClickBm15AK4", 0.505);
        ReadFactors(factors, "ремонт карбюратора", "kps=13&relev=relevgeo=163", false, __LINE__);
        factors.CheckFactor("CorrectedCtrBm15V4K5", 0.0); // relevgeo 163 is a city that is not in Russia - no data
        factors.CheckFactor("LongClickBm15AK4", 0.0);
        return true;
    }
};

// Obtained from Ferryman full state as:
//     ./rtymsg_reader --proxy hahn --from 400 --to 401 --table //home/saas/ferryman-stable/ydo_search/ytpull/full.XXXXXXXXXX --column value --json 1 > doc.json
//     vim doc.json
//     cat doc.json | jq -c . | sed -e 's:\t:\\011:g' -e 's:":\t:g'
//                     | python -c 'import sys; s=sys.stdin.read(); print s.encode("string-escape");'
//                     | sed -e 's:\\t:\\":g' -e 's:\\n$::'

const TStringBuf TTestYdoAnnCaseClass::Doc1 =
"{\"action\":\"modify\",\"docs\":[{\"all_points\":{\"type\":\"#e\",\"value\":{\"coords\":[39.87169647,47.25727463],\"kind\":\"p\"}},\"attended_points\":{\"type\":\"#e\",\"value\":{\"coords\":[39.87169647,47.25727463],\"kind\":\"p\"}},\"departure_areas\":{\"type\":\"#e\",\"value\":{\"coords\":[39.86759186,47.25447845,39.87580109,47.2600708],\"kind\":\"r\"}},\"display_options\":[{\"type\":\"#p\",\"value\":\"{\\\"has_chat\\\":true,\\\"has_phone\\\":true}\"}],\"f_avg_absolute_price\":[{\"type\":\"#f\",\"value\":2501.74}],\"h_features\":[{\"type\":\"#h\",\"value\":\"has_cafe\"},{\"type\":\"#h\",\"value\":\"warranty\"},{\"type\":\"#h\",\"value\":\"work_place_out\"},{\"type\":\"#h\",\"value\":\"worker_type_org\"}],\"h_occupations\":[{\"type\":\"#h\",\"value\":\"868\"}],\"h_rating\":[{\"type\":\"#h\",\"value\":\"0.0\"}],\"h_services\":[{\"type\":\"#h\",\"value\":\"890\"},{\"type\":\"#h\",\"value\":\"891\"},{\"type\":\"#h\",\"value\":\"897\"}],\"i_address_attended_geoid\":[{\"type\":\"#i\",\"value\":11031}],\"i_address_geoid\":[{\"type\":\"#i\",\"value\":10000},{\"type\":\"#i\",\"value\":10001},{\"type\":\"#i\",\"value\":11029},{\"type\":\"#i\",\"value\":11031},{\"type\":\"#i\",\"value\":225},{\"type\":\"#i\",\"value\":26},{\"type\":\"#i\",\"value\":99409}],\"i_attended_region_id\":[{\"type\":\"#i\",\"value\":11031}],\"options\":{\"modification_timestamp\":1595258832,\"realtime\":false},\"s_card_id\":[{\"type\":\"#p\",\"value\":\"bd062123-21c7-4354-841d-95b2754e8c97\"}],\"s_occupations\":[{\"type\":\"#lp\",\"value\":\"/remont-avto\"}],\"s_services\":[{\"type\":\"#l\",\"value\":\"/remont-avto/dvigatel_/remont-karburatora\"}],\"s_specs\":[{\"type\":\"#lp\",\"value\":\"/remont-avto/dvigatel_\"}],\"s_worker_id\":[{\"type\":\"#l\",\"value\":\"f5c4271d-de78-4581-930d-aa60cb75c03a\"}],\"title\":{\"type\":\"#z\",\"value\":\"\xd0\xa1\xd0\xa2\xd0\x9e \\\"\xd0\x90\xd0\xb2\xd1\x82\xd0\xbe\xd0\xbc\xd0\xb5\xd1\x85\xd0\xb0\xd0\xbd\xd0\xb8\xd0\xba 12 \xd0\x92\xd0\xbe\xd0\xbb\xd1\x8c\xd1\x82\\\"\"},\"url\":\"bd062123-21c7-4354-841d-95b2754e8c97\",\"worker_hash\":[{\"type\":\"#p\",\"value\":\"14327237159045528809\"}],\"ydo\":[{\"type\":\"#h\",\"value\":\"f5c4271d-de78-4581-930d-aa60cb75c03a\"}],\"ydo_worker_id\":[{\"type\":\"#p\",\"value\":\"f5c4271d-de78-4581-930d-aa60cb75c03a\"}],\"z_occupations\":{\"type\":\"#z\",\"value\":\"\xd0\xa0\xd0\xb5\xd0\xbc\xd0\xbe\xd0\xbd\xd1\x82 \xd0\xb0\xd0\xb2\xd1\x82\xd0\xbe\"},\"z_occupations_alternames\":[{\"type\":\"#z\",\"value\":\"\xd0\x90\xd0\xb2\xd1\x82\xd0\xbe\xd0\xbc\xd0\xb0\xd1\x81\xd1\x82\xd0\xb5\xd1\x80\xd1\x81\xd0\xba\xd0\xb0\xd1\x8f\"},{\"type\":\"#z\",\"value\":\"\xd0\x9c\xd0\xb0\xd1\x81\xd1\x82\xd0\xb5\xd1\x80 \xd0\xbf\xd0\xbe \xd1\x80\xd0\xb5\xd0\xbc\xd0\xbe\xd0\xbd\xd1\x82\xd1\x83 \xd0\xb0\xd0\xb2\xd1\x82\xd0\xbe\"}],\"z_occupations_features\":{\"type\":\"#z\",\"value\":\"\xd0\xa2\xd0\xb8\xd0\xbf \xd0\xb8\xd1\x81\xd0\xbf\xd0\xbe\xd0\xbb\xd0\xbd\xd0\xb8\xd1\x82\xd0\xb5\xd0\xbb\xd1\x8f \xd0\x9e\xd1\x80\xd0\xb3\xd0\xb0\xd0\xbd\xd0\xb8\xd0\xb7\xd0\xb0\xd1\x86\xd0\xb8\xd1\x8f\"},\"z_occupations_synonyms\":[{\"type\":\"#z\",\"value\":\"\xd0\xb0\xd0\xb2\xd1\x82\xd0\xbe\xd1\x80\xd0\xb5\xd0\xbc\xd0\xbe\xd0\xbd\xd1\x82\"}],\"z_services\":[{\"type\":\"#z\",\"value\":\"\xd0\x9f\xd1\x80\xd0\xbe\xd0\xbc\xd1\x8b\xd0\xb2\xd0\xba\xd0\xb0 \xd0\xb8\xd0\xbd\xd0\xb6\xd0\xb5\xd0\xba\xd1\x82\xd0\xbe\xd1\x80\xd0\xb0\"},{\"type\":\"#z\",\"value\":\"\xd0\xa0\xd0\xb5\xd0\xbc\xd0\xbe\xd0\xbd\xd1\x82 \xd0\xba\xd0\xb0\xd1\x80\xd0\xb1\xd1\x8e\xd1\x80\xd0\xb0\xd1\x82\xd0\xbe\xd1\x80\xd0\xb0\"}]}],\"prefix\":13}"sv
;


START_TEST_DEFINE(TestYdoSoftness)
private:
    static const TStringBuf Doc1;

public:
    using TFactors = NSaas::TDocFactorsView;

public:
    bool InitConfig() override {
        (*ConfigDiff)["Searcher.FactorsInfo"] = GetResourcesDirectory() + "/text_search/ydo/relev.conf-ydo_search";
        (*ConfigDiff)["Components"] = "INDEX,FULLARC,ANN,MinGeo,FASTARC,STATIC_FILES";
        (*ConfigDiff)["Indexer.Memory.Enabled"] = "false";
        (*ConfigDiff)["Indexer.Common.OxygenOptionsFile"] = GetResourcesDirectory() + "/text_search/ydo/shardfiles.conf-ydo_search";
        (*ConfigDiff)["Indexer.Common.Groups"] = "popularity:2:unique geoa:2:unique";
        (*ConfigDiff)["ComponentsConfig.FULLARC.ActiveLayers"] = "base";
        (*ConfigDiff)["ComponentsConfig.FULLARC.LightLayers"] = "MinGeo";
        (*ConfigDiff)["ComponentsConfig." + NRTYServer::AnnComponentName + ".DataType"] = "PLAIN_ARRAY";
        (*ConfigDiff)["ComponentsConfig." + NRTYServer::AnnComponentName + ".ImitateQrQuorum"] = "false";
        (*ConfigDiff)["ComponentsConfig." + NRTYServer::AnnComponentName + ".UseExternalCalcer"] = "true";
        (*ConfigDiff)["ComponentsConfig." + NRTYServer::AnnComponentName + ".UseRegionChain"] = "true";
        return true;
    }

    static void SetDocumentTextAndAddRegion(NRTYServer::TMessage& message, TStringBuf text) {
        NRTYServer::TMessage::TDocument& doc = *message.MutableDocument();
        doc.SetBody(TString(text));

        NIndexAnn::TIndexAnnSiteData annData;
        {
            NIndexAnn::TAnnotationRec& annRec = *annData.AddRecs();
            annRec.SetText(TString(text));
            annRec.SetTextLanguage(1);
            NIndexAnn::TRegionData& annReg = *annRec.AddData();
            annReg.SetRegion(213); // Moscow
        }

        NIndexAnn::UnpackToSaasData(*doc.MutableAnnData(), annData, NIndexAnn::GetSaasProfile());
    }

    bool Run() override {
        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, 3, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
        SetDocumentTextAndAddRegion(messages[0], "диджей");
        SetDocumentTextAndAddRegion(messages[1], "топор");
        SetDocumentTextAndAddRegion(messages[2], "диджей топор");
        IndexMessages(messages, DISK, 1);
        const TString kps(GetAllKps(messages));
        ReopenIndexers();

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

        const TString DjAxeUrlEncoded = "%D0%B4%D0%B8%D0%B4%D0%B6%D0%B5%D0%B9%20%D1%82%D0%BE%D0%BF%D0%BE%D1%80"; // 'диджей топор'

        // Testcase 0: no softness -> should find only one document with both words
        {
            TString searchResult;
            CHECK_TEST_EQ(ProcessQuery(TString("/?text=" + DjAxeUrlEncoded + "&dump=eventlog&ms=proto&service=tests") + kps, &searchResult), 200);
            const TVector<NMetaProtocol::TDocument> resultDocuments = parseSearchResultDocuments(searchResult);
            CHECK_TEST_EQ(resultDocuments.size(), 1);
        }

        // Testcase 1: add DocQourum to 0 (max softness) but no enabling softness parameter
        {
            TString searchResult;
            CHECK_TEST_EQ(ProcessQuery(TString("/?text=" + DjAxeUrlEncoded + "&dump=eventlog&ms=proto&service=tests&pron=dqm=0") + kps, &searchResult), 200);
            const TVector<NMetaProtocol::TDocument> resultDocuments = parseSearchResultDocuments(searchResult);
            CHECK_TEST_EQ(resultDocuments.size(), 1);
        }

        // Testcase 2: add DocQourum to 0 (max softness) with enabling softness parameter
        {
            TString searchResult;
            CHECK_TEST_EQ(ProcessQuery(TString("/?text=" + DjAxeUrlEncoded + "&dump=eventlog&ms=proto&service=tests&pron=dqm=0&relev=enable_softness") + kps, &searchResult), 200);
            const TVector<NMetaProtocol::TDocument> resultDocuments = parseSearchResultDocuments(searchResult);
            CHECK_TEST_EQ(resultDocuments.size(), 3);
        }

        // Testcase 3: add DocQourum to 0 (max softness) but text is completly different
        {
            TString searchResult;
            CHECK_TEST_EQ(ProcessQuery(TString("/?text=wtf_odin_111&dump=eventlog&ms=proto&service=tests&pron=dqm=0&relev=enable_softness") + kps, &searchResult), 200);
            const TVector<NMetaProtocol::TDocument> resultDocuments = parseSearchResultDocuments(searchResult);
            CHECK_TEST_EQ(resultDocuments.size(), 0);
        }
        return true;
    }
};
