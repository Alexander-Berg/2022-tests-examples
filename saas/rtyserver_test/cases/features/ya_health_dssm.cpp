#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/rtyserver_test/util/factors_parsers.h>
#include <saas/rtyserver_test/cases/indexer/ann.h> // for TDocFactorsView

#include <saas/api/action.h>

#include <library/cpp/json/json_reader.h>

SERVICE_TEST_RTYSERVER_DEFINE(TestDssmTitleBase)
protected:
    static const TStringBuf Doc1;

public:
    using TFactors = NSaas::TDocFactorsView;

public:
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

    void IndexRealData() {
        TVector<NRTYServer::TMessage> messages;

        const TFsPath msgFilePath = GetResourcesDirectory() + "/text_search/health/sample_article.json";
        msgFilePath.CheckExists();
        TString doc1 = TFileInput(msgFilePath).ReadAll();
        NRTYServer::TMessage mes = NSaas::TAction().ParseFromJson(NJson::ReadJsonFastTree(doc1)).ToProtobuf();

        messages.emplace_back(std::move(mes));
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();
        CheckSearchResults(messages);
    }

    bool InitConfig() override {
        (*ConfigDiff)["Searcher.TextMachine.TitleZones"] = "z_article_title";
        (*ConfigDiff)["Searcher.TextMachine.TitleSentencesNumber"] = Max<ui32>();
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestDssmTitle, TestDssmTitleBase)
public:
    bool CheckFeature() {
        // Checks values of the embedding
        TVector<TDocSearchInfo> results;
        TVector<TSimpleSharedPtr<THashMultiMap<TString, TString>>> resultProps;

        QuerySearchL("глюкоза в крови&kps=100&fsgta=_DocZoneTitle", results, &resultProps, __LINE__);
        CHECK_TEST_EQ(results.size(), 1);

        const TString titleText = resultProps[0]->find("_DocZoneTitle")->second;
        DEBUG_LOG << "_DocZoneTitle: " << titleText << Endl;
        CHECK_TEST_EQ(titleText, "Глюкоза в крови - показания, подготовка, интерпретация");

        // здесь DSSM реагирует на слово "показания" и понимает, что "нормальный уровень" больше подходит к тайтлу, чем "виды сахара". Очень круто.
        TFactors factors;
        ReadFactors(factors, "глюкоза нормальный уровень", "kps=100", false, __LINE__);
        factors.CheckFactor("DssmQueryTitle", 0.23735);
        ReadFactors(factors, "глюкоза виды сахара", "kps=100", false, __LINE__);
        factors.CheckFactor("DssmQueryTitle", 0.17348);
        return true;
    }

    bool Run() override {
        IndexRealData();

        CHECK_TEST_TRUE(CheckFeature());
        //CHECK_TEST_TRUE(CheckFastRankOnly());
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestDssmTitleModelsNotSet, TestDssmTitleBase)
public:
    bool CheckNoModels() {
        TVector<TDocSearchInfo> results;
        TVector<TSimpleSharedPtr<THashMultiMap<TString, TString>>> resultProps;

        // due to the special "ZEROFILL" value in Searcher config, the factors are filled with their canonical values
        TFactors factors;
        ReadFactors(factors, "глюкоза нормальный уровень", "kps=100", false, __LINE__);
        factors.CheckFactor("DssmQueryTitle", 0);
        ReadFactors(factors, "глюкоза виды сахара", "kps=100", false, __LINE__);
        factors.CheckFactor("DssmQueryTitle", 0);
        return true;
    }

    bool Run() override {
        IndexRealData();

        CHECK_TEST_TRUE(CheckNoModels());
        return true;
    }

    bool InitConfig() override {
        if (!TestDssmTitleBase::InitConfig())
            return false;

        (*ConfigDiff)["Searcher.FactorsModels"] = "ZEROFILL";
        return true;
    }
};
