#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/rtyserver_test/testerlib/globals.h>
#include <saas/rtyserver_test/util/doc_info.h>
#include <saas/rtyserver_test/util/factors_parsers.h>

#include <saas/rtyserver/components/l2/globals.h>
#include <saas/api/factors_erf.h>
#include <saas/util/json/json.h>

#include <kernel/indexann_data/data.h>
#include <kernel/multipart_archive/owner.h>

using TArchiveOwner = NRTYArchive::TMultipartOwner<NRTYArchive::TMultipartArchive>;

namespace {
    struct TQueryResult {
        TVector<TDocSearchInfo> Docs;
        TVector<TSimpleSharedPtr<THashMultiMap<TString, TString>>> DocProps;

    public:
        size_t Count() const {
            Y_ENSURE(Docs.size() == DocProps.size());
            return Docs.size();
        }

        TMaybe<TString> GetGta(size_t docPos, const TStringBuf gta) const {
            Y_ENSURE(docPos < Count());
            const auto& prop = DocProps[docPos];
            auto i = prop->find(gta);
            if (i == prop->end())
                return Nothing();
            return i->second;
        }
    };
}

SERVICE_TEST_RTYSERVER_DEFINE(TestL2ArcHelper)
public:
    bool InitConfig() override {
        SetIndexerParams(DISK, 100, 1);
        SetMergerParams(true, 1, -1, mcpNONE);
        (*ConfigDiff)["Components"] = INDEX_COMPONENT_NAME "," FULL_ARCHIVE_COMPONENT_NAME "," L2_COMPONENT_NAME;
        (*ConfigDiff)["ComponentsConfig." FULL_ARCHIVE_COMPONENT_NAME ".LightLayers"] = L2_COMPONENT_NAME;
        (*ConfigDiff)["Searcher.ExternalSearch"] = "rty_relevance";
        (*ConfigDiff)["Searcher.FactorsInfo"] = WriteConfigFile<EConfigType::Factors>(CreateFactorsConfigTemplate());

        (*ConfigDiff)["IndexGenerator"] = INDEX_COMPONENT_NAME;
        (*ConfigDiff)["Indexer.Memory.Enabled"] = "false"; //SAAS-5686
        return true;
    }

    static NJson::TJsonValue CreateFactorsConfigTemplate() {
        static const TStringBuf configBody = R"({
            "user_factors": {
                "user1": 0,
                "user2": 1
            },
            "user_functions": {
                "L2_RawData": "L2Raw"
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

protected:
    TQueryResult QueryOneUrlForGta(const TString& url, const TStringBuf gta, const TString& allKps) {
        TQueryResult result;
        QuerySearch("url:\"" + url + "\"&fsgta=" + gta + allKps, result.Docs, &result.DocProps, 0, true);
        return result;
    }

protected:
    virtual TString GetLayerName() const {
        return "myL2";
    }

    bool IsSeparateLayerUsed() {
        return GetLayerName() != "full";
    }
public:
    bool Run() override {
        // A basic check: write a string, receive a string
        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, 4, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());

        NRTYServer::TMessage& message = messages[0];
        auto* prop = message.MutableDocument()->AddDocumentProperties();
        prop->SetName(L2_COMPONENT_NAME);
        prop->SetValue("Aaa");

        IndexMessages(messages, DISK, 1);
        ReopenIndexers();

        const TStringBuf gta("_L2_RawData");
        auto queryResults = QueryOneUrlForGta(message.GetDocument().GetUrl(), gta, GetAllKps(messages));
        CHECK_TEST_EQ(1u, queryResults.Count());
        CHECK_TEST_EQ("Aaa", queryResults.GetGta(0, gta).GetOrElse("<no gta>"));

        return true;
    }
};

START_TEST_DEFINE_PARENT(TestL2ArcBasics, TestL2ArcHelper)
};

START_TEST_DEFINE_PARENT(TestL2ArcWithFull, TestL2ArcHelper)
    bool InitConfig() override {
        if (!TestL2ArcHelper::InitConfig()) {
            return false;
        }
        return true;
    }
};
