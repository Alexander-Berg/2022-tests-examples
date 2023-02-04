#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/api/deserialize.h>
#include <saas/protos/rtyserver.pb.h>

#include <library/cpp/json/json_reader.h>
#include <library/cpp/protobuf/json/json2proto.h>
#include <library/cpp/string_utils/base64/base64.h>

#include <util/stream/str.h>
#include <util/string/subst.h>

using namespace NRTYServer;

namespace {
    TVector<TMessage> MakeMessages(bool prefixed) {
        TVector<TMessage> result;
        TVector<TString> messages = {
            R"_({
                "docs": [ {
                        "options": { "mime_type": "text/html"},
                        "body": { "value": "first"},
                        "prop_test": {"type": "#p", "value": "foo"},
                        "prop_test2": {"type": "#p", "value": "foo2"},
                        "url": "first"
                     } ],
                 "action": "modify"
                 __PREFIX__
                 }
            )_",
            R"_({
                "docs": [ {
                        "options": { "mime_type": "text/html"},
                        "body": { "value": "second"},
                        "prop_test": {"type": "#p", "value": "bar"},
                        "prop_test2": {"type": "#p", "value": "bar2"},
                        "url": "second"
                     } ],
                 "action": "modify"
                 __PREFIX__
                 }
            )_",
            R"_({
                "docs": [ {
                        "options": { "mime_type": "text/html"},
                        "body": { "value": "third"},
                        "prop_test": {"type": "#p", "value": "baz"},
                        "prop_test2": {"type": "#p", "value": "baz2"},
                        "url": "third"
                     } ],
                 "action": "modify"
                 __PREFIX__
                 }
            )_",
        };
        for (auto msg: messages) {
            SubstGlobal(msg, "__PREFIX__", prefixed ? R"(, "prefix": 123)" : "");
            TStringInput in(msg);
            const auto docJson = NJson::ReadJsonTree(&in, true);
            NSaas::TAction action;
            NSaas::TJsonDeserializer{}.Deserialize(docJson, action);
            result.push_back(action.ToProtobuf());
        }
        return result;
    }

    TString AddSgkps(TString query, bool prefixed) {
        if (prefixed) {
            query += "&sgkps=123";
        }
        return query;
    }

    TString AddKps(TString query, bool prefixed) {
        if (prefixed) {
            query += "&kps=123";
        }
        return query;
    }
}

SERVICE_TEST_RTYSERVER_DEFINE(TestPropKv)
    bool InitConfig() override {
        SetEnabledDiskSearch();
        for (const auto& [k, v]: std::initializer_list<std::pair<TString, TString>>{
                {"Components", "FULLARC,PROP"},
                {"IndexGenerator", "FULLARC"},
                {"Indexer.Memory.Enabled", "false"},
                {"Merger.Enabled", "true"},
                {"Searcher.TwoStepQuery", "false"},
                {"Searcher.SnippetsDeniedZones", ""},
                {"ComponentsConfig.PROP.Properties", "prop_test"},
                {"ComponentsConfig.FULLARC.ActiveLayers", "full PROP"},
                {"ComponentsConfig.FULLARC.Layers.PROP.Compression", "RAW"},
                {"ComponentsConfig.FULLARC.Layers.PROP.ReadContextDataAccessType", "MEMORY_LOCKED_MAP"},
                {"ShardsNumber", "1"}
                }) {
            (*ConfigDiff)[k] = v;
        }
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestPropKvIndexation, TestPropKv)
    bool Run() override {
        const auto& messages = MakeMessages(GetIsPrefixed());
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();
        const TString query = AddSgkps(R"_(first&ms=proto&component=PROP&gta=_AllDocInfos)_", GetIsPrefixed());
        TVector<TDocSearchInfo> results;
        TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > > resultProps;
        QuerySearch(query, results, &resultProps);
        // Make sure search returns results.
        CHECK_TEST_EQ(results.size(), 1);

        auto iter = resultProps[0]->find("prop_test");
        CHECK_TEST_TRUE(iter != resultProps[0]->end());
        CHECK_TEST_EQ(iter->second, "foo");

        return true;
    }
};

START_TEST_DEFINE_PARENT(TestPropKvMemoryIndexation, TestPropKv)
    bool InitConfig() override {
        TestPropKv::InitConfig();
        (*ConfigDiff)["Indexer.Memory.Enabled"] = "true";
        return true;
    }

    bool Run() override {
        const auto& messages = MakeMessages(GetIsPrefixed());
        IndexMessages(messages, REALTIME, 1);
        const TString query = AddSgkps(R"_(first&ms=proto&component=PROP&gta=_AllDocInfos)_", GetIsPrefixed());
        TVector<TDocSearchInfo> results;
        TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > > resultProps;
        QuerySearch(query, results, &resultProps);
        // Make sure search returns results.
        CHECK_TEST_EQ(results.size(), 1);

        auto iter = resultProps[0]->find("prop_test");
        CHECK_TEST_TRUE(iter != resultProps[0]->end());
        CHECK_TEST_EQ(iter->second, "foo");

        return true;
    }
};

START_TEST_DEFINE_PARENT(TestPropKvNormalization, TestPropKv)
    bool Run() override {
        const auto& messages = MakeMessages(GetIsPrefixed());
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();
        const TString query = AddSgkps(R"_(first&ms=proto&component=PROP&gta=_AllDocInfos)_", GetIsPrefixed());
        TVector<TDocSearchInfo> results;
        TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > > resultProps;
        QuerySearch(query, results, &resultProps);
        // Make sure search returns results.
        CHECK_TEST_EQ(results.size(), 1);

        {
            // prop2 is not in the prop config initially
            auto iter = resultProps[0]->find("prop_test2");
            CHECK_TEST_TRUE(iter == resultProps[0]->end());
        }

        // Enable prop_test2 in the config and restart the server
        (*ConfigDiff)["ComponentsConfig.PROP.Properties"] = "prop prop_test2";
        Controller->ApplyConfigDiff(ConfigDiff);
        Controller->RestartServer();

        QuerySearch(query, results, &resultProps);
        CHECK_TEST_EQ(results.size(), 1);

        // Now PROP should have reindexed the documents and 
        {
            auto iter = resultProps[0]->find("prop_test2");
            CHECK_TEST_TRUE(iter != resultProps[0]->end());
            CHECK_TEST_EQ(iter->second, "foo2");
        }

        return true;
    }
};

SERVICE_TEST_RTYSERVER_DEFINE(TestProp)
    bool InitConfig() override {
        SetEnabledDiskSearch();
        for (const auto& [k, v]: std::initializer_list<std::pair<TString, TString>>{
                {"Components", "INDEXER,FULLARC,PROP"},
                {"Indexer.Memory.Enabled", "false"},
                {"Merger.Enabled", "true"},
                {"Searcher.FactorsInfo", WriteConfigFile<EConfigType::Factors>(CreateFactorsConfigTemplate())},
                {"ComponentsConfig.PROP.Properties", "prop_test"},
                {"ComponentsConfig.PROP.EnableFsgta", "true"},
                {"ComponentsConfig.PROP.EnableGta", "true"},
                {"ComponentsConfig.FULLARC.ActiveLayers", "base PROP"},
                {"ComponentsConfig.FULLARC.Layers.PROP.Compression", "RAW"},
                {"ComponentsConfig.FULLARC.Layers.PROP.ReadContextDataAccessType", "MEMORY_LOCKED_MAP"},
                {"ShardsNumber", "1"}
                }) {
            (*ConfigDiff)[k] = v;
        }
        return true;
    }

    static NJson::TJsonValue CreateFactorsConfigTemplate() {
        static const TStringBuf configBody = R"({
            "user_factors": {
                "user1": 0,
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
};

START_TEST_DEFINE_PARENT(TestPropIndexationFsgta, TestProp)
    bool Run() override {
        const auto& messages = MakeMessages(GetIsPrefixed());
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();
        const TString query = AddKps(R"_(first&ms=proto&fsgta=prop_test&haha=da)_", GetIsPrefixed());
        TVector<TDocSearchInfo> results;
        TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > > resultProps;
        QuerySearch(query, results, &resultProps);
        // Make sure search returns results.
        CHECK_TEST_EQ(results.size(), 1);

        auto iter = resultProps[0]->find("prop_test");
        CHECK_TEST_TRUE(iter != resultProps[0]->end());
        CHECK_TEST_EQ(iter->second, "foo");

        return true;
    }
};

START_TEST_DEFINE_PARENT(TestPropMemoryIndexationFsgta, TestProp)
    bool InitConfig() override {
        TestProp::InitConfig();
        (*ConfigDiff)["Indexer.Memory.Enabled"] = "true";
        return true;
    }

    bool Run() override {
        const auto& messages = MakeMessages(GetIsPrefixed());
        IndexMessages(messages, REALTIME, 1);
        const TString query = AddKps(R"_(first&ms=proto&fsgta=prop_test&haha=da)_", GetIsPrefixed());
        TVector<TDocSearchInfo> results;
        TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > > resultProps;
        QuerySearch(query, results, &resultProps);
        // Make sure search returns results.
        CHECK_TEST_EQ(results.size(), 1);

        auto iter = resultProps[0]->find("prop_test");
        CHECK_TEST_TRUE(iter != resultProps[0]->end());
        CHECK_TEST_EQ(iter->second, "foo");

        return true;
    }
};

START_TEST_DEFINE_PARENT(TestPropOnlyStorage, TestProp)
    bool InitConfig() override {
        TestProp::InitConfig();
        (*ConfigDiff)["ComponentsConfig.PROP.Properties"] = "*";
        (*ConfigDiff)["ComponentsConfig.INDEX.ExcludeProperties"] = "true";
        return true;
    }

    bool Run() override {
        const auto& messages = MakeMessages(GetIsPrefixed());
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();
        const TString query = AddKps(R"_(url:first&ms=proto&gta=_AllDocInfos)_", GetIsPrefixed());
        TVector<TDocSearchInfo> results;
        TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > > resultProps;
        QuerySearch(query, results, &resultProps);
        // Make sure search returns results.
        CHECK_TEST_EQ(results.size(), 1);

        auto [begin, end] = resultProps[0]->equal_range("prop_test");
        CHECK_TEST_TRUE(begin != end);
        CHECK_TEST_TRUE(std::next(begin) == end);
        CHECK_TEST_EQ(begin->second, "foo");

        return true;
    }
};

START_TEST_DEFINE_PARENT(TestPropOnlyStorageMerge, TestProp)
    bool InitConfig() override {
        TestProp::InitConfig();
        (*ConfigDiff)["ComponentsConfig.PROP.Properties"] = "*";
        (*ConfigDiff)["ComponentsConfig.INDEX.ExcludeProperties"] = "true";
        SetMergerParams(true, 1, -1, mcpCONTINUOUS, 500000);
        return true;
    }

    bool Run() override {
        const auto& messages = MakeMessages(GetIsPrefixed());
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();
        Controller->ProcessCommand("create_merger_tasks");
        Controller->ProcessCommand("do_all_merger_tasks");
        const TString query = AddKps(R"_(url:first&ms=proto&gta=_AllDocInfos)_", GetIsPrefixed());
        TVector<TDocSearchInfo> results;
        TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > > resultProps;
        QuerySearch(query, results, &resultProps);
        // Make sure search returns results.
        CHECK_TEST_EQ(results.size(), 1);

        auto [begin, end] = resultProps[0]->equal_range("prop_test");
        CHECK_TEST_TRUE(begin != end);
        CHECK_TEST_TRUE(std::next(begin) == end);
        CHECK_TEST_EQ(begin->second, "foo");

        return true;
    }
};

class TestPropNormalizationBase : public TestProp {
protected:
    bool InitConfig() override {
        TestProp::InitConfig();
        (*ConfigDiff)["Components"] = "INDEXER";
        return true;
    }

    bool Run() override {
        const auto& messages = MakeMessages(GetIsPrefixed());
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();
        TVector<TDocSearchInfo> results;
        TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > > resultProps;

        // Query our property through fsgta. Only the PROP can answer
        const TString query = AddKps(R"_(url:first&ms=proto&fsgta=prop_test2&haha=da)_", GetIsPrefixed());
        QuerySearch(query, results, &resultProps);
        CHECK_TEST_EQ(results.size(), 1);

        // PROP is disabled, so no answer
        {
            auto iter = resultProps[0]->find("prop_test2");
            CHECK_TEST_TRUE(iter == resultProps[0]->end());
        }

        // Change the config so that the properties must now go into the PROP
        (*ConfigDiff)["Components"] = "INDEXER,FULLARC,PROP";
        (*ConfigDiff)["ComponentsConfig.PROP.Properties"] = "*";
        (*ConfigDiff)["ComponentsConfig.INDEX.ExcludeProperties"] = "true";
        Controller->ApplyConfigDiff(ConfigDiff);
        Controller->RestartServer();

        {
            const TString query = AddKps(R"_(url:first&ms=proto&fsgta=prop_test2&haha=da)_", GetIsPrefixed());
            QuerySearch(query, results, &resultProps);
        }
        CHECK_TEST_EQ(results.size(), 1);

        // PROP should get properties from INDEX and answer now
        {
            auto iter = resultProps[0]->find("prop_test2");
            CHECK_TEST_TRUE(iter != resultProps[0]->end());
            CHECK_TEST_EQ(iter->second, "foo2");
        }

        return true;
    }
};

START_TEST_DEFINE_PARENT(TestPropNormalizationFlat, TestPropNormalizationBase)
    bool InitConfig() override {
        TestPropNormalizationBase::InitConfig();
        (*ConfigDiff)["Searcher.ArchiveType"] = "AT_FLAT";
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestPropNormalizationMultipart, TestPropNormalizationBase)
    bool InitConfig() override {
        TestPropNormalizationBase::InitConfig();
        (*ConfigDiff)["Searcher.ArchiveType"] = "AT_MULTIPART";
        return true;
    }
};
