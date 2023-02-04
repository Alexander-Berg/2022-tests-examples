#include <saas/api/search_client/client.h>
#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/rtyserver_test/testerlib/standart_generator.h>

SERVICE_TEST_RTYSERVER_DEFINE(TestStringSorting)
    bool Run() override {
        ui32 docsCount = GetMaxDocuments() * 5;

        auto* sdg = new TStandartDocumentGenerator(GetIsPrefixed());
        TString textForSearch = "hello";
        sdg->SetTextConstant(textForSearch);
        auto* saf = new TStandartAttributesFiller();
        saf->SetDocumentsCount(docsCount);
        sdg->RegisterFiller("gr", saf);

        TStandartMessagesGenerator smg(sdg, SendIndexReply);
        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, docsCount, smg);

        for (ui32 i = 0; i < docsCount; i++) {
            auto* doc = messages[i].MutableDocument();
            TString attr(ToString(i));
            attr = "sortgrattr_" + TString(10 - attr.size(), '0') + attr;

            auto* grAttr = doc->AddGroupAttributes();
            grAttr->set_name("sortgrattr");
            grAttr->set_value(attr);
            grAttr->set_type(::NRTYServer::TAttribute::LITERAL_ATTRIBUTE);
        }

        std::random_shuffle(messages.begin(), messages.end());
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();

        CHECK_TEST_TRUE(HasSearchproxy());
        TVector<TDocSearchInfo> results;
        TQuerySearchContext searchContext;
        searchContext.PrintResult = true;
        TQuerySearchContext::TDocProperties props;
        searchContext.DocProperties = &props;
        QuerySearch(textForSearch + "&service=tests&how=_Name_sortgrattr&gta=_Name_sortgrattr&numdoc=10&" + GetAllKps(messages), results, searchContext);

        CHECK_TEST_TRUE(results.size() == 10);
        for (ui32 i = 0, idx = docsCount - 1; i < results.size(); ++i, --idx) {
            const auto& prop = props[i]->find("_Name_sortgrattr")->second;
            CHECK_TEST_TRUE(FromString<i64>(prop.substr(TString("sortgrattr_").size())) == idx);
        }
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestSortingByStringAttr, TestStringSorting)
public:
    bool InitConfig() override {
        (*SPConfigDiff)["SearchConfig.ReArrangeOptions"] = "StringSort";
        (*ConfigDiff)["Searcher.ReArrangeOptions"] = "StringSort";
        (*ConfigDiff)["ComponentsConfig.FASTARC.FastProperties"] = "sortgrattr";

        SetIndexerParams(DISK, 20);
        SetIndexerParams(REALTIME, 20);
        SetMergerParams(false);
        return true;
    }
};
