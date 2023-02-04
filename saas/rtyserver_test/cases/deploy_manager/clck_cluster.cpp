#include "deploy_manager.h"

#include <saas/deploy_manager/meta/cluster.h>
#include <saas/deploy_manager/scripts/deploy/action.h>
#include <saas/deploy_manager/scripts/add_intsearch/action.h>
#include <saas/deploy_manager/scripts/add_replica/action.h>
#include <saas/deploy_manager/scripts/searchmap/action.h>

#include <saas/api/indexing_client/client.h>

#include <search/session/compression/compression.h>
#include <kernel/querydata/idl/querydata_structs_client.pb.h>


SERVICE_TEST_RTYSERVER_DEFINE_PARENT(TestDeployManagerMetaServicePersonal, TestDeployManager)
void InitCluster() override {
    UploadCommon();
    UploadService("personal-long");
    UploadService("personal-login");
    UploadService("personal-meta");
    ConfigureCluster(1, 1, NSaas::UrlHash, "rtyserver", "personal-long");
    ConfigureCluster(1, 1, NSaas::UrlHash, "rtyserver", "personal-login");
    Controller->SetActiveBackends(TBackendProxy::TBackendSet());
    NSearchMapParser::TMetaService ms;
    ms.Name = "personal-meta";
    ms.Components.push_back(NSearchMapParser::TMetaComponent("personal-long"));
    ms.Components.push_back(NSearchMapParser::TMetaComponent("personal-login"));

    NDaemonController::TSimpleSearchmapModifAction addService(TVector<TString>(), "sbtests", ms.Name, "add_service", "metaservice");
    NDaemonController::TSimpleSearchmapModifAction editService(ms.SerializeToJson().GetStringRobust(), "sbtests", ms.Name, "metaservice");

    Controller->ExecuteActionOnDeployManager(addService);
    Controller->ExecuteActionOnDeployManager(editService);
    DeployIp();
    DeploySp();
}

struct TDoc {
    TString Key;
    TString PropName;
    TString PropValue;
};

NSaas::TAction BuildIndexingAction(const TDoc& document, NMetaProtocol::ECompressionMethod compression) {
    NSaas::TAction action;
    action.SetActionType(NSaas::TAction::atModify);
    NSaas::TDocument& doc = action.AddDocument();
    doc.SetRealtime(true);
    doc.SetUrl(document.Key);
    if (compression != NMetaProtocol::CM_COMPRESSION_NONE)
        doc.AddBinaryProperty(document.PropName, NMetaProtocol::GetDefaultCompressor().Compress(document.PropValue, compression));
    else
        doc.AddAttribute(document.PropName).AddValue(document.PropValue).AddType(NSaas::TAttributeValue::avtProp);

    return action;
}

bool DoIndexMessage(const TDoc& document, NMetaProtocol::ECompressionMethod compression, const TString& service) {
    auto& backendConf = Controller->GetConfig();
    NSaas::TIndexingClient client(backendConf.Indexer.Host, backendConf.Indexer.Port, "/service/" + MD5::Calc(service + " json_ref"));

    NSaas::TAction action = BuildIndexingAction(document, compression);
    auto res = client.Send(action, "json_ref");
    CHECK_TEST_TRUE(res.GetCode() == NSaas::TSendResult::srOK);

    return true;
}

bool CheckOneDocument(const TDoc& document, bool binary, const TString& service) {
    TQuerySearchContext ctx;
    ctx.AttemptionsCount = 5;
    ctx.PrintResult = true;
    ctx.AppendService = false;
    TQuerySearchContext::TDocProperties resultProps;
    ctx.DocProperties = &resultProps;
    ctx.SourceSelector = new TDirectSourceSelector();

    TVector<TDocSearchInfo> results;
    QuerySearch(document.Key + "&sp_meta_search=proxy&service=" + service, results, ctx);

    if (results.size() != 1) {
        TEST_FAILED("Test failed: " + ToString(results.size()));
    }

    TString propName = document.PropName;
    if (binary) {
        auto parts = SplitString(document.PropName, "_", 2);
        CHECK_TEST_TRUE(parts.size() == 2);
        propName = parts[1];
    }

    auto docProperties = resultProps[0];
    if (docProperties->find(propName) == docProperties->end()) {
        TEST_FAILED("Test failed: no data found in search property " + propName);
    }

    CHECK_TEST_EQ((*docProperties).find(propName)->second, document.PropValue);

    return true;
}

};


START_TEST_DEFINE_PARENT(TestDeployManagerMetaServiceWithCustomSearch, TestDeployManagerMetaServicePersonal)
bool Run() override {
    TDoc Doc1 = {"doc1", "gzip_compressed_property", "aaaaaaaaaaaaaaaaaaaadefdcmkefmvkflmbofgdkjfcwsaaasds"};
    TDoc Doc2 = {"doc2", "not_compressed_property", "aaaaaaaaaaaaaaaaaa"};
    TDoc Doc3 = {"doc3", "lz4_prop3", "bbbbbbbbbbbbbb"};
    TDoc Doc4 = {"doc1", "lz4_prop4", "asadsadsadbbbbbbbbbbbbbb"};
    TDoc Doc5 = {"doc5", "prop5", "asadsadsadbbbbbbbbbbbbbb"};

    CHECK_TEST_TRUE(DoIndexMessage(Doc1, NMetaProtocol::CM_COMPRESSION_GZIP, "personal-long"));
    CHECK_TEST_TRUE(DoIndexMessage(Doc2, NMetaProtocol::CM_COMPRESSION_NONE, "personal-login"));
    CHECK_TEST_TRUE(DoIndexMessage(Doc3, NMetaProtocol::CM_COMPRESSION_LZ4, "personal-login"));
    CHECK_TEST_TRUE(DoIndexMessage(Doc4, NMetaProtocol::CM_COMPRESSION_LZ4, "personal-login"));
    CHECK_TEST_TRUE(DoIndexMessage(Doc5, NMetaProtocol::CM_COMPRESSION_NONE, "personal-long"));

    ReopenIndexers("personal-long");

    // Check documents for each service
    CHECK_TEST_TRUE(CheckOneDocument(Doc1, true, "personal-long"));
    CHECK_TEST_TRUE(CheckOneDocument(Doc2, false, "personal-login"));
    CHECK_TEST_TRUE(CheckOneDocument(Doc3, true, "personal-login"));
    CHECK_TEST_TRUE(CheckOneDocument(Doc4, true, "personal-login"));

    // Check documents with metaservice
    {
        TQuerySearchContext ctx;
        ctx.AttemptionsCount = 5;
        ctx.PrintResult = true;
        ctx.AppendService = false;
        ctx.SourceSelector = new TLevelSourceSelector(3);

        THashMultiMap<TString, TString> searchProperties;
        ctx.SearchProperties = &searchProperties;
        TVector<TDocSearchInfo> results;

        QuerySearch(Doc2.Key + "&service=personal-meta", results, ctx);

        if (results.size() != 1) {
            TEST_FAILED("Test meta service search failed: " + ToString(results.size()));
        }

        if (searchProperties.find("QueryData.debug") == searchProperties.end() || searchProperties.find("QueryData.debug")->second.empty()) {
            TEST_FAILED("Test failed: no data found in search property");
        }

        QuerySearch(Doc2.Key + "&service=personal-meta&sp_meta_search=multi_proxy", results, ctx);
        if (results.size() != 1) {
            TEST_FAILED("Test meta service search failed: " + ToString(results.size()));
        }
    }

    // One key in two services
    {
        TQuerySearchContext ctx;
        ctx.AttemptionsCount = 5;
        ctx.PrintResult = true;
        ctx.AppendService = false;
        ctx.SourceSelector = new TLevelSourceSelector(3);

        THashMultiMap<TString, TString> searchProperties;
        ctx.SearchProperties = &searchProperties;
        TVector<TDocSearchInfo> results;

        QuerySearch(Doc1.Key + "&service=personal-meta", results, ctx);

        if (results.size() != 2) {
            TEST_FAILED("Test meta service search failed: " + ToString(results.size()));
        }

        if (searchProperties.find("QueryData.debug") == searchProperties.end() || searchProperties.find("QueryData.debug")->second.empty()) {
            TEST_FAILED("Test failed: no data found in search property");
        }

        QuerySearch(Doc1.Key + "&service=personal-meta&sp_meta_search=multi_proxy", results, ctx);
        if (results.size() != 2) {
            TEST_FAILED("Test meta service search failed: " + ToString(results.size()));
        }
    }

    // Two keys from different services services
    {
        TQuerySearchContext ctx;
        ctx.AttemptionsCount = 5;
        ctx.PrintResult = true;
        ctx.AppendService = false;
        ctx.SourceSelector = new TLevelSourceSelector(3);

        THashMultiMap<TString, TString> searchProperties;
        ctx.SearchProperties = &searchProperties;
        TVector<TDocSearchInfo> results;

        QuerySearch(Doc2.Key + "," + Doc5.Key + "&service=personal-meta", results, ctx);

        if (results.size() != 2) {
            TEST_FAILED("Test meta service search failed: " + ToString(results.size()));
        }

        if (searchProperties.find("QueryData.debug") == searchProperties.end() || searchProperties.find("QueryData.debug")->second.empty()) {
            TEST_FAILED("Test failed: no data found in search property");
        }

        QuerySearch(Doc2.Key + "," + Doc5.Key + "&service=personal-meta&sp_meta_search=multi_proxy", results, ctx);
        if (results.size() != 2) {
            TEST_FAILED("Test meta service search failed: " + ToString(results.size()));
        }
    }

    // Two keys - one common
    {
        TQuerySearchContext ctx;
        ctx.AttemptionsCount = 5;
        ctx.PrintResult = true;
        ctx.AppendService = false;
        ctx.SourceSelector = new TLevelSourceSelector(3);

        THashMultiMap<TString, TString> searchProperties;
        ctx.SearchProperties = &searchProperties;
        TVector<TDocSearchInfo> results;

        QuerySearch(Doc1.Key + "," + Doc3.Key + "&service=personal-meta", results, ctx);

        if (results.size() != 3) {
            TEST_FAILED("Test meta service search failed: " + ToString(results.size()));
        }

        if (searchProperties.find("QueryData.debug") == searchProperties.end() || searchProperties.find("QueryData.debug")->second.empty()) {
            TEST_FAILED("Test failed: no data found in search property");
        }

        QuerySearch(Doc1.Key + "," + Doc3.Key + "&service=personal-meta&sp_meta_search=multi_proxy", results, ctx);
        if (results.size() != 3) {
            TEST_FAILED("Test meta service search failed: " + ToString(results.size()));
        }
    }

    return true;
}
};


START_TEST_DEFINE_PARENT(TestDeployManagerMetaServiceGrouping, TestDeployManagerMetaServicePersonal)
bool Run() override {
    TDoc Doc1 = {"doc", "property1", "aaaaaaaaaaaaaaaaaaaadefdcmkefmvkflmbofgdkjfcwsaaasds"};
    TDoc Doc2 = {"doc2", "property2", "aaaaaaaaaaaaaaaaaa"};
    TDoc Doc3 = {"doc", "property3", "aadefdcmkefmvkflmbofgdkjfcwsaaasds"};

    CHECK_TEST_TRUE(DoIndexMessage(Doc1, NMetaProtocol::CM_COMPRESSION_NONE, "personal-long"));
    CHECK_TEST_TRUE(DoIndexMessage(Doc2, NMetaProtocol::CM_COMPRESSION_NONE, "personal-long"));
    CHECK_TEST_TRUE(DoIndexMessage(Doc3, NMetaProtocol::CM_COMPRESSION_NONE, "personal-login"));

    ReopenIndexers("personal-long");
    ReopenIndexers("personal-login");

    CHECK_TEST_TRUE(CheckOneDocument(Doc1, false, "personal-long"));
    CHECK_TEST_TRUE(CheckOneDocument(Doc2, false, "personal-long"));
    CHECK_TEST_TRUE(CheckOneDocument(Doc3, false, "personal-login"));

    {
        TQuerySearchContext ctx;
        ctx.AttemptionsCount = 5;
        ctx.PrintResult = true;
        ctx.AppendService = false;
        ctx.SourceSelector = new TLevelSourceSelector(2);

        THashMultiMap<TString, TString> searchProperties;
        ctx.SearchProperties = &searchProperties;
        TVector<TDocSearchInfo> results;

        QuerySearch(Doc1.Key + "&service=personal-long&sp_meta_search=multi_proxy&ag=saas-grouping&g=1.saas-grouping.1.3.-1.0.0.-1.rlv.0..0.0&gta=_MimeType", results, ctx);

        if (results.size() != 1) {
            TEST_FAILED("Test meta service search failed: " + ToString(results.size()));
        }
    }

    {
        TQuerySearchContext ctx;
        ctx.AttemptionsCount = 5;
        ctx.PrintResult = true;
        ctx.AppendService = false;
        ctx.SourceSelector = new TLevelSourceSelector(3);

        THashMultiMap<TString, TString> searchProperties;
        ctx.SearchProperties = &searchProperties;
        ctx.PrintResult = true;
        TVector<TDocSearchInfo> results;

        QuerySearch(Doc1.Key + "," + Doc2.Key + "&service=personal-meta&ag=saas-grouping&g=1.saas-grouping.1.3.-1.0.0.-1.rlv.0..0.0", results, ctx);

        if (results.size() != 3) {
            TEST_FAILED("Test meta service search failed: " + ToString(results.size()));
        }

        if (searchProperties.find("QueryData.debug") != searchProperties.end()) {
            TEST_FAILED("Test failed: incorrect rearrange behaviour");
        }
    }

    return true;
}

};
