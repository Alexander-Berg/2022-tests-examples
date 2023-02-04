#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/api/indexing_client/client.h>
#include <search/session/compression/compression.h>

#include <kernel/querydata/idl/querydata_structs_client.pb.h>

#include <library/cpp/digest/md5/md5.h>


SERVICE_TEST_RTYSERVER_DEFINE(TestQSRearrangeBase)
void CreateAndIndex(TVector<NRTYServer::TMessage>& messages, bool useTag = false) {
    NJson::TJsonValue json(NJson::JSON_MAP);
    json["some_key"] = 123;
    GenerateInput(messages, 10, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    for (ui32 i = 0; i < messages.size(); ++i) {
        messages[i].MutableDocument()->SetKeyPrefix(messages[0].MutableDocument()->GetKeyPrefix());
        if (useTag) {
            messages[i].MutableDocument()->SetUrl("unique_key_" + ToString(i));

            auto specKey = messages[i].MutableDocument()->AddAdditionalKeys();
            specKey->SetName("merge_key");
            specKey->SetValue("key_" + ToString(i));

        } else {
            messages[i].MutableDocument()->SetUrl("key_" + ToString(i));
        }
        auto prop = messages[i].MutableDocument()->AddDocumentProperties();
        prop->SetName("kv_data_" + ToString(i));
        prop->SetValue(json.GetStringRobust());
    }

    IndexMessages(messages, DISK, 1);
    ReopenIndexers();
}

bool DoSearch(const TString& proxyType, TVector<NRTYServer::TMessage>& messages) {
    TQuerySearchContext ctx;
    ctx.AttemptionsCount = 5;
    ctx.PrintResult = true;

    TString keys;
    for (ui32 i = 0; i < messages.size(); ++i) {
        TString realKey = "key_" + ToString(i);
        if (!keys.empty())
            keys += ",";
        keys += realKey;

        TVector<TDocSearchInfo> results;
        THashMultiMap<TString, TString> searchProperties;
        ctx.SearchProperties = &searchProperties;
        QuerySearch(realKey + "&sp_meta_search=" + proxyType +
                                "&meta_search=first_found&normal_kv_report=1&sgkps=" + ToString(messages[i].GetDocument().GetKeyPrefix()), results, ctx);
        if (results.size() != 1) {
            PrintInfoServer();
            TEST_FAILED("Test failed: " + ToString(results.size()));
        }
        if (searchProperties.find("QueryData.debug") == searchProperties.end() || searchProperties.find("QueryData.debug")->second.empty()) {
            TEST_FAILED("Test failed: no data found in search property");
        }

        QuerySearch(realKey + "&rearr=QueryData_off&sp_meta_search=" + proxyType +
                                "&meta_search=first_found&normal_kv_report=1&sgkps=" + ToString(messages[i].GetDocument().GetKeyPrefix()), results, ctx);
        if (results.size() != 1) {
            PrintInfoServer();
            TEST_FAILED("Test failed: " + ToString(results.size()));
        }
        if (searchProperties.find("QueryData.debug") != searchProperties.end()) {
            TEST_FAILED("Test failed: data found in search property while rearrange is off");
        }

    }
    TVector<TDocSearchInfo> results;
    THashMultiMap<TString, TString> searchProperties;
    ctx.SearchProperties = &searchProperties;
    NOT_EXCEPT(QuerySearch(keys + "&sp_meta_search=" + proxyType +
                            "&meta_search=first_found&normal_kv_report=1&ag=fake&g=1.fake.13.1.-1.0.0.-1.rlv.0..0.0&sgkps=" + ToString(messages[0].GetDocument().GetKeyPrefix()), results, ctx));
    if (results.size() != 10) {
        PrintInfoServer();
        TEST_FAILED("Test failed: " + ToString(results.size()));
    }
    if (searchProperties.find("QueryData.debug") == searchProperties.end() || searchProperties.find("QueryData.debug")->second.empty()) {
        TEST_FAILED("Test failed: no data found in search property");
    }

    NOT_EXCEPT(QuerySearch(keys + "&rearr=QueryData_off&sp_meta_search=" + proxyType +
                            "&meta_search=first_found&normal_kv_report=1&ag=fake&g=1.fake.13.1.-1.0.0.-1.rlv.0..0.0&sgkps=" + ToString(messages[0].GetDocument().GetKeyPrefix()), results, ctx));
    if (results.size() != 10) {
        PrintInfoServer();
        TEST_FAILED("Test failed: " + ToString(results.size()));
    }
    if (searchProperties.find("QueryData.debug") != searchProperties.end()) {
        TEST_FAILED("Test failed: data found in search property while rearrange is off");
    }

    return true;
}

bool DoInitConfig() {
    SetIndexerParams(DISK, 100, 1);
    (*ConfigDiff)["IndexGenerator"] = FULL_ARCHIVE_COMPONENT_NAME;
    (*ConfigDiff)["Components"] = FULL_ARCHIVE_COMPONENT_NAME;
    (*ConfigDiff)["Indexer.Memory.Enabled"] = "false";
    (*ConfigDiff)["Searcher.SnippetsDeniedZones"] = "";
    (*ConfigDiff)["Searcher.TwoStepQuery"] = "false";
    (*SPConfigDiff)["Service.MetaSearch.TwoStepQuery"] = "false";
    (*SPConfigDiff)["Service.StrictCgiParams.ag"] = "__remove";
    (*SPConfigDiff)["Service.StrictCgiParams.g"] = "__remove";
    return true;
}
};

START_TEST_DEFINE_PARENT(TestQuerySearchRearrangeRule, TestQSRearrangeBase)
    bool Run() override {
        TVector<NRTYServer::TMessage> messages;
        CreateAndIndex(messages);
        return DoSearch("meta", messages);
    }

    bool InitConfig() override {
        (*SPConfigDiff)["Service.MetaSearch.ReArrangeOptions"] =
            "QueryData(Namespace=test_qs)";
        return DoInitConfig();
    }
};

START_TEST_DEFINE_PARENT(TestQuerySearchSpecKeys, TestQSRearrangeBase)
    bool Run() override {
        TVector<NRTYServer::TMessage> messages;
        CreateAndIndex(messages, true);
        return DoSearch("multi_proxy", messages);
    }

    bool InitConfig() override {
        (*SPConfigDiff)["Service.CustomRearranges.QueryData"] = "Namespace=test_qs;SourceName=categflag/test_qs;KeyType=categ";
        (*SPConfigDiff)["Service.GlobalCgiParams.key_name"] = "merge_key";
        return DoInitConfig();
    }
};

START_TEST_DEFINE_PARENT(TestQuerySearchCustomRearrangeRule, TestQSRearrangeBase)
    bool Run() override {
        TVector<NRTYServer::TMessage> messages;
        CreateAndIndex(messages);
        return DoSearch("multi_proxy", messages);
    }

    bool InitConfig() override {
        (*SPConfigDiff)["Service.CustomRearranges.QueryData"] = "Namespace=test_qs;SourceName=categflag/test_qs;KeyType=categ";
        return DoInitConfig();
    }
};

START_TEST_DEFINE(TestQSCompression)
    bool Run() override {
        TString propVal = "aaaaaaaaaaaaaaaaaaaadefdcmkefmvkflmbofgdkjfcwsaaasds";

        NSaas::TAction action;
        action.SetActionType(NSaas::TAction::atModify);
        NSaas::TDocument& doc = action.AddDocument();
        doc.SetUrl("some_key");
        doc.AddBinaryProperty("gzip_compressed_property", NMetaProtocol::GetDefaultCompressor().Compress(propVal, NMetaProtocol::CM_COMPRESSION_GZIP));
        doc.AddAttribute("not_compressed_property").AddValue(propVal).AddType(NSaas::TAttributeValue::avtProp);

        auto& backendConf = Controller->GetConfig();
        NSaas::TIndexingClient client(backendConf.Indexer.Host, backendConf.Indexer.Port, "/service/" + MD5::Calc("tests json_ref"));
        auto res = client.Send(action, "json_ref");
        CHECK_TEST_TRUE(res.GetCode() == NSaas::TSendResult::srOK);

        ReopenIndexers();

        {
            TQuerySearchContext ctx;
            ctx.AttemptionsCount = 5;
            ctx.PrintResult = true;
            ctx.SourceSelector = new TDirectSourceSelector();

            TVector<TDocSearchInfo> results;
            QuerySearch("unknown_key&sp_meta_search=proxy&meta_search=first_found&normal_kv_report=1&sgkps=0", results, ctx);

            if (results.size() != 0) {
                PrintInfoServer();
                TEST_FAILED("Test failed: " + ToString(results.size()));
        }
        }

        TQuerySearchContext ctx;
        ctx.AttemptionsCount = 5;
        ctx.PrintResult = true;
        TQuerySearchContext::TDocProperties resultProps;
        ctx.DocProperties = &resultProps;
        ctx.SourceSelector = new TDirectSourceSelector();

        TVector<TDocSearchInfo> results;
        QuerySearch("some_key&sp_meta_search=proxy&meta_search=first_found&normal_kv_report=1&sgkps=0", results, ctx);

        if (results.size() != 1) {
            PrintInfoServer();
            TEST_FAILED("Test failed: " + ToString(results.size()));
        }

        auto docProperties = resultProps[0];
        if (docProperties->find("compressed_property") == docProperties->end()
                || docProperties->find("not_compressed_property") == docProperties->end()) {
            TEST_FAILED("Test failed: no data found in search property");
        }

        CHECK_TEST_EQ((*docProperties).find("compressed_property")->second, propVal);
        CHECK_TEST_EQ((*docProperties).find("not_compressed_property")->second, propVal);

        return true;
    }

    bool InitConfig() override {
        SetIndexerParams(DISK, 100, 1);
        (*ConfigDiff)["IndexGenerator"] = FULL_ARCHIVE_COMPONENT_NAME;
        (*ConfigDiff)["Components"] = FULL_ARCHIVE_COMPONENT_NAME;
        (*ConfigDiff)["Indexer.Memory.Enabled"] = "false";
        (*ConfigDiff)["Searcher.SnippetsDeniedZones"] = "";
        (*ConfigDiff)["Searcher.TwoStepQuery"] = "false";
        (*SPConfigDiff)["Service.MetaSearch.TwoStepQuery"] = "false";
        (*SPConfigDiff)["Service.CustomRearranges.Compression"] = "empty";
        return true;
    }
};


START_TEST_DEFINE(TestQSBinaryData)
    bool Run() override {
        TString propVal = "aaaaaaaaaaaaaaaaaaaadefdcmkefmvkflmbofgdkjfcwsaaasds";

        NSaas::TAction action;
        action.SetActionType(NSaas::TAction::atModify);
        NSaas::TDocument& doc = action.AddDocument();
        doc.SetUrl("some_key");
        doc.AddBinaryProperty("gzip_compressed_property", NMetaProtocol::GetDefaultCompressor().Compress(propVal, NMetaProtocol::CM_COMPRESSION_GZIP));
        doc.AddAttribute("not_compressed_property").AddValue(propVal).AddType(NSaas::TAttributeValue::avtProp);

        auto& backendConf = Controller->GetConfig();
        NSaas::TIndexingClient client(backendConf.Indexer.Host, backendConf.Indexer.Port, "/service/" + MD5::Calc("tests json_ref"));
        auto res = client.Send(action, "json_ref");
        CHECK_TEST_TRUE(res.GetCode() == NSaas::TSendResult::srOK);

        ReopenIndexers();

        TQuerySearchContext ctx;
        ctx.AttemptionsCount = 5;
        ctx.PrintResult = true;
        TQuerySearchContext::TDocProperties resultProps;
        ctx.DocProperties = &resultProps;
        ctx.SourceSelector = new TDirectSourceSelector();

        TVector<TDocSearchInfo> results;
        QuerySearch("some_key&sp_meta_search=proxy&meta_search=first_found&normal_kv_report=1&sgkps=0", results, ctx);

        if (results.size() != 1) {
            PrintInfoServer();
            TEST_FAILED("Test failed: " + ToString(results.size()));
        }

        auto docProperties = resultProps[0];
        if (docProperties->find("gzip_compressed_property") == docProperties->end()
                || docProperties->find("not_compressed_property") == docProperties->end()) {
            TEST_FAILED("Test failed: no data found in search property");
        }

        TString val = docProperties->find("gzip_compressed_property")->second;
        INFO_LOG << val << Endl;

        CHECK_TEST_EQ((*docProperties).find("not_compressed_property")->second, propVal);
        CHECK_TEST_EQ(val, NMetaProtocol::GetDefaultCompressor().Compress(propVal, NMetaProtocol::CM_COMPRESSION_GZIP));

        return true;
    }

    bool InitConfig() override {
        SetIndexerParams(DISK, 100, 1);
        (*ConfigDiff)["IndexGenerator"] = FULL_ARCHIVE_COMPONENT_NAME;
        (*ConfigDiff)["Components"] = FULL_ARCHIVE_COMPONENT_NAME;
        (*ConfigDiff)["Indexer.Memory.Enabled"] = "false";
        (*ConfigDiff)["Searcher.SnippetsDeniedZones"] = "";
        (*ConfigDiff)["Searcher.TwoStepQuery"] = "false";
        (*SPConfigDiff)["Service.MetaSearch.TwoStepQuery"] = "false";
        return true;
    }
};


START_TEST_DEFINE(TestQueryTrieSearchSimple)
    bool Run() override {
        const TString propVal = "Cg95YW5zd2VyX2hpc3RvcnkQwLXbqZzp1gIiFm1hbGkgaGFuZ2kga8SxdGFkYWTEsXIoC0I+eyJBbnN3ZXJUaXRsZSI6ICLQkNGE0YDQuNC60LAsINCX0LDQv9Cw0LTQvdCw0Y8g0JDRhNGA0LjQutCwIn0=";
        const TString propKey = "QDSaaS:yanswer_history";

        NSaas::TAction action;
        action.SetActionType(NSaas::TAction::atModify);
        NSaas::TDocument& doc = action.AddDocument();
        doc.SetUrl(".1\tmali hangi k\304\261tadad\304\261r");
        doc.AddAttribute(propKey).AddValue(propVal).AddType(NSaas::TAttributeValue::avtProp);

        auto& backendConf = Controller->GetConfig();
        NSaas::TIndexingClient client(backendConf.Indexer.Host, backendConf.Indexer.Port, "/service/" + MD5::Calc("tests json_ref"));
        auto res = client.Send(action, "json_ref");
        CHECK_TEST_TRUE(res.GetCode() == NSaas::TSendResult::srOK);
        ReopenIndexers();

        TQuerySearchContext ctx;
        ctx.AttemptionsCount = 5;
        ctx.PrintResult = true;
        THashMultiMap<TString, TString> searchProperties;
        ctx.SearchProperties = &searchProperties;
        if (Cluster->GetNodesNames(TNODE_SEARCHPROXY).size()) {
            ctx.SourceSelector = new TLevelSourceSelector(2);
        } else {
            ctx.SourceSelector = new TDirectSourceSelector();
        }
        TVector<TDocSearchInfo> results;
        const TString query = TString("&relev=norm=mali hangi k\304\261tadad\304\261r&sp_meta_search=multi_proxy&meta_search=first_found&normal_kv_report=1&sgkps=0") +
            "&saas_no_text_split=1&noqtree=1";
        QuerySearch(query, results, ctx);

        if (results.size() != 1) {
            PrintInfoServer();
            TEST_FAILED("Test failed: " + ToString(results.size()));
        }
        if (searchProperties.find("QueryData.debug") == searchProperties.end() || searchProperties.find("QueryData.debug")->second.empty()) {
            TEST_FAILED("Test failed: no data found in search property");
        }
        return true;
    }

    bool InitConfig() override {
        SetIndexerParams(DISK, 100, 1);
        (*ConfigDiff)["IndexGenerator"] = FULL_ARCHIVE_COMPONENT_NAME;
        (*ConfigDiff)["Components"] = TString::Join(FULL_ARCHIVE_COMPONENT_NAME, ",TRIE");
        (*ConfigDiff)["Indexer.Memory.Enabled"] = "false";
        (*ConfigDiff)["Searcher.SnippetsDeniedZones"] = "";
        (*ConfigDiff)["Searcher.TwoStepQuery"] = "false";
        (*SPConfigDiff)["Service.MetaSearch.TwoStepQuery"] = "false";
        (*SPConfigDiff)["Service.CustomRearranges.QueryDataTrie"] = "SaaSType=Trie;OptimizeKeyTypes=false;KT_QueryStrong=false,false";
        return true;
    }

};
