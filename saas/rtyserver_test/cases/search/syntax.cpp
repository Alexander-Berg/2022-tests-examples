#include <saas/api/clientapi.h>
#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <library/cpp/charset/recyr.hh>
#include <library/cpp/charset/wide.h>
#include <saas/rtyserver_test/testerlib/standart_generator.h>
#include <library/cpp/string_utils/quote/quote.h>

SERVICE_TEST_RTYSERVER_DEFINE(TestSearchSyntax)
protected:
    TString Kps;
protected:
    TVector<NRTYServer::TMessage> IndexTexts(const TVector<TString>& texts, TIndexerType indexer, const TString& mime = "text/html") {
        TVector<NRTYServer::TMessage> messages;
        const bool isPrefixed = GetIsPrefixed();
        GenerateInput(messages, texts.size(), NRTYServer::TMessage::ADD_DOCUMENT, isPrefixed);
        for (unsigned i = 0; i < texts.size(); ++i) {
            messages[i].MutableDocument()->SetBody(WideToUTF8(CharToWide(texts[i], CODES_UTF8)));
            messages[i].MutableDocument()->SetMimeType(mime);
            if (isPrefixed)
                messages[i].MutableDocument()->SetKeyPrefix(1);
        }
        IndexMessages(messages, indexer, 1);
        if (indexer == DISK)
            ReopenIndexers();
        if (isPrefixed) {
            Kps = "&kps=" + ToString(1);
        } else {
            Kps = "";
        }

        return messages;
    }
    void CheckQuery(const TString& query, int count) {
        TVector<TDocSearchInfo> results;
        QuerySearch(RecodeToYandex(CODES_UTF8, query) + Kps, results);
        if (results.ysize() != count) {
            ythrow yexception() << "Query '" << query + Kps << "'results count incorrect: " << results.ysize() << " != " << count;
        }
    }
};

START_TEST_DEFINE_PARENT(TestSearchSyntaxOrAnd, TestSearchSyntax)
bool Run() override {
    TVector<TString> texts;
    texts.push_back("Раз, два, три, четыре, пять. Вышел зайчик погулять. Вдруг охотник выбегает, прямо в зайчика стреляет!");
    texts.push_back("Пять, четыре, три, два, раз. Выбегает охотник вдруг, в зайчика прямо стреляет! Зайчик погулять вышел. Сила.");
    texts.push_back("Охотник перешел на темную сторону силы. Зайчик познал внутрений покой");
    texts.push_back("Внутрений покой - признак силы.");
    IndexTexts(texts, DISK);
    CheckQuery("охотник | признак", 4);
    CheckQuery("сила %26%26 покой", 2);
    CheckQuery("сила %26 покой", 1);
    CheckQuery("раз /+3 четыре", 2);
    CheckQuery("раз /-3 четыре", 1);
    CheckQuery("раз %26/(+3 +3) четыре", 1);
    CheckQuery("раз %26/(-3 +3) четыре", 2);
    CheckQuery("охотник %26/(-1 3) зайчик", 1);
    CheckQuery("вышел %26%26/(-10 +30) стреляет", 2);
    CheckQuery("вышел %26/(-10 +30) стреляет", 0);
    return true;
}
};

START_TEST_DEFINE_PARENT(TestSearchTreeHitsMarkers, TestSearchSyntax)
bool Run() override {
    TVector<TString> texts;
    texts.push_back("Раз, два, три, четыре, пять. Вышел зайчик погулять. Вдруг охотник выбегает, прямо в зайчика стреляет!");
    texts.push_back("Пять, четыре, три, два, раз. Выбегает охотник вдруг, в зайчика прямо стреляет! Зайчик погулять вышел. Сила.");
    IndexTexts(texts, REALTIME);

    ReopenIndexers();

    texts.clear();
    texts.push_back("Охотник перешел на темную сторону силы. Зайчик познал внутрений покой");
    texts.push_back("Внутрений покой - признак силы.");
    IndexTexts(texts, REALTIME);

    TVector<TDocSearchInfo> results;
    TQuerySearchContext context;
    THashMultiMap<TString, TString> props;
    context.SearchProperties = &props;

    sleep(10);

    TString hitsCount = "";

    for (ui32 att = 0; att < 10; ++att) {
        props.clear();
        context.ResultCountRequirement = 3;
        QuerySearch(RecodeToYandex(CODES_UTF8, "зайчик | охотник&gta=rty_sum_expected_docs_count&qi=rty_sum_expected_docs_count&gta=rty_setsum_rich_tree_stat&qi=rty_setsum_rich_tree_stat&gta=rty_sum_docs_count&qi=rty_sum_docs_count") + Kps, results, context);


        DEBUG_LOG << "----------------------------" << Endl;
        CHECK_TEST_EQ(results.size(), 3);
        CHECK_TEST_TRUE(context.SearchProperties->contains("rty_sum_docs_count"));
        CHECK_TEST_EQ((*context.SearchProperties).find("rty_sum_docs_count")->second, "4");
        CHECK_TEST_TRUE(context.SearchProperties->contains("rty_setsum_rich_tree_stat"));
        CHECK_TEST_NEQ((*context.SearchProperties).find("rty_setsum_rich_tree_stat")->second, "");
        CHECK_TEST_TRUE(context.SearchProperties->contains("rty_sum_expected_docs_count"));
        CHECK_TEST_EQ((*context.SearchProperties).find("rty_sum_expected_docs_count")->second, "3");
        CHECK_TEST_TRUE(context.SearchProperties->contains("ExpectedDocsCount.rty_sum_expected_docs_count"));
        hitsCount = (*context.SearchProperties).find("ExpectedDocsCount.rty_sum_expected_docs_count")->second;
        if (hitsCount == "3")
            break;
        sleep(5);
    }
    CHECK_TEST_EQ(hitsCount, "3");

    props.clear();
    context.ResultCountRequirement = 2;
    QuerySearch(RecodeToYandex(CODES_UTF8, "раз зайчик&gta=rty_sum_expected_docs_count&qi=rty_sum_expected_docs_count&gta=rty_setsum_rich_tree_stat&qi=rty_setsum_rich_tree_stat&gta=rty_sum_docs_count&skip-wizard=1&qi=rty_sum_docs_count") + Kps, results, context);

    CHECK_TEST_EQ(results.size(), 2);
    CHECK_TEST_TRUE(context.SearchProperties->contains("rty_sum_docs_count"));
    CHECK_TEST_EQ((*context.SearchProperties).find("rty_sum_docs_count")->second, "4");
    CHECK_TEST_TRUE(context.SearchProperties->contains("rty_setsum_rich_tree_stat"));
    CHECK_TEST_NEQ((*context.SearchProperties).find("rty_setsum_rich_tree_stat")->second, "");
    CHECK_TEST_TRUE(context.SearchProperties->contains("rty_sum_expected_docs_count"));
    CHECK_TEST_EQ((*context.SearchProperties).find("rty_sum_expected_docs_count")->second, "1");
    CHECK_TEST_TRUE(context.SearchProperties->contains("ExpectedDocsCount.rty_sum_expected_docs_count"));
    CHECK_TEST_EQ((*context.SearchProperties).find("ExpectedDocsCount.rty_sum_expected_docs_count")->second, "0");

    props.clear();
    context.ResultCountRequirement = 3;
    QuerySearch(RecodeToYandex(CODES_UTF8, "зайчик&gta=rty_sum_expected_docs_count&qi=rty_sum_expected_docs_count&gta=rty_setsum_rich_tree_stat&qi=rty_setsum_rich_tree_stat&gta=rty_sum_docs_count&qi=rty_sum_docs_count") + Kps, results, context);

    CHECK_TEST_EQ(results.size(), 3);
    CHECK_TEST_TRUE(context.SearchProperties->contains("rty_sum_docs_count"));
    CHECK_TEST_EQ((*context.SearchProperties).find("rty_sum_docs_count")->second, "4");
    CHECK_TEST_TRUE(context.SearchProperties->contains("rty_setsum_rich_tree_stat"));
    CHECK_TEST_NEQ((*context.SearchProperties).find("rty_setsum_rich_tree_stat")->second, "");
    CHECK_TEST_TRUE(context.SearchProperties->contains("rty_sum_expected_docs_count"));
    CHECK_TEST_EQ((*context.SearchProperties).find("rty_sum_expected_docs_count")->second, "2");
    CHECK_TEST_TRUE(context.SearchProperties->contains("ExpectedDocsCount.rty_sum_expected_docs_count"));
    CHECK_TEST_EQ((*context.SearchProperties).find("ExpectedDocsCount.rty_sum_expected_docs_count")->second, "2");

    ReopenIndexers();

    sleep(10);

    hitsCount = "";

    for (ui32 att = 0; att < 10; ++att) {

        props.clear();
        context.ResultCountRequirement = 3;
        QuerySearch(RecodeToYandex(CODES_UTF8, "зайчик&gta=rty_sum_expected_docs_count&qi=rty_sum_expected_docs_count&gta=rty_setsum_rich_tree_stat&qi=rty_setsum_rich_tree_stat&gta=rty_sum_docs_count&qi=rty_sum_docs_count") + Kps, results, context);

        CHECK_TEST_EQ(results.size(), 3);
        CHECK_TEST_TRUE(context.SearchProperties->contains("rty_sum_docs_count"));
        CHECK_TEST_EQ((*context.SearchProperties).find("rty_sum_docs_count")->second, "4");
        CHECK_TEST_TRUE(context.SearchProperties->contains("rty_setsum_rich_tree_stat"));
        CHECK_TEST_NEQ((*context.SearchProperties).find("rty_setsum_rich_tree_stat")->second, "");
        CHECK_TEST_TRUE(context.SearchProperties->contains("rty_sum_expected_docs_count"));
        CHECK_TEST_EQ((*context.SearchProperties).find("rty_sum_expected_docs_count")->second, "2");
        CHECK_TEST_TRUE(context.SearchProperties->contains("ExpectedDocsCount.rty_sum_expected_docs_count"));
        hitsCount = (*context.SearchProperties).find("ExpectedDocsCount.rty_sum_expected_docs_count")->second;
        if (hitsCount == "2")
            break;

        sleep(5);
    }
    CHECK_TEST_EQ(hitsCount, "2");


    props.clear();
    context.ResultCountRequirement = 2;
    QuerySearch(RecodeToYandex(CODES_UTF8, "раз зайчик&gta=rty_sum_expected_docs_count&qi=rty_sum_expected_docs_count&gta=rty_setsum_rich_tree_stat&qi=rty_setsum_rich_tree_stat&gta=rty_sum_docs_count&qi=rty_sum_docs_count") + Kps, results, context);

    CHECK_TEST_EQ(results.size(), 2);
    CHECK_TEST_TRUE(context.SearchProperties->contains("rty_sum_docs_count"));
    CHECK_TEST_EQ((*context.SearchProperties).find("rty_sum_docs_count")->second, "4");
    CHECK_TEST_TRUE(context.SearchProperties->contains("rty_setsum_rich_tree_stat"));
    CHECK_TEST_NEQ((*context.SearchProperties).find("rty_setsum_rich_tree_stat")->second, "");
    CHECK_TEST_TRUE(context.SearchProperties->contains("rty_sum_expected_docs_count"));
    CHECK_TEST_EQ((*context.SearchProperties).find("rty_sum_expected_docs_count")->second, "1");
    CHECK_TEST_TRUE(context.SearchProperties->contains("ExpectedDocsCount.rty_sum_expected_docs_count"));
    CHECK_TEST_EQ((*context.SearchProperties).find("ExpectedDocsCount.rty_sum_expected_docs_count")->second, "0");

    props.clear();
    context.ResultCountRequirement = 3;
    QuerySearch(RecodeToYandex(CODES_UTF8, "зайчик&gta=rty_sum_expected_docs_count&qi=rty_sum_expected_docs_count") + Kps, results, context);
    CHECK_TEST_EQ(results.size(), 3);
    CHECK_TEST_TRUE(context.SearchProperties->contains("rty_sum_expected_docs_count"));
    CHECK_TEST_EQ((*context.SearchProperties).find("rty_sum_expected_docs_count")->second, "2");
    CHECK_TEST_TRUE(context.SearchProperties->contains("ExpectedDocsCount.rty_sum_expected_docs_count"));
    CHECK_TEST_EQ((*context.SearchProperties).find("ExpectedDocsCount.rty_sum_expected_docs_count")->second, "2");

    return true;
}
bool InitConfig() override {
    (*SPConfigDiff)["SearchConfig.ReArrangeOptions"] = "ExpectedDocsCount";
    if (!Callback || !Callback->GetNodesNames(TNODE_SEARCHPROXY).size())
        (*ConfigDiff)["Searcher.ReArrangeOptions"] = "ExpectedDocsCount";
    SetMergerParams(true, 1, 1, mcpTIME, Max<i32>());
    return true;
}
};

START_TEST_DEFINE_PARENT(TestSearchTreeHitsMarkersDiskOnly, TestSearchSyntax)
bool Run() override {
    TVector<TString> texts;
    texts.push_back("Раз, два, три, четыре, пять. Вышел зайчик погулять. Вдруг охотник выбегает, прямо в зайчика стреляет!");
    texts.push_back("Пять, четыре, три, два, раз. Выбегает охотник вдруг, в зайчика прямо стреляет! Зайчик погулять вышел. Сила.");
    IndexTexts(texts, DISK);

    ReopenIndexers();

    texts.clear();
    texts.push_back("Охотник перешел на темную сторону силы. Зайчик познал внутрений покой");
    texts.push_back("Внутрений покой - признак силы.");
    IndexTexts(texts, DISK);

    ReopenIndexers();

    TVector<TDocSearchInfo> results;
    TQuerySearchContext context;
    context.ResultCountRequirement = 3;
    THashMultiMap<TString, TString> props;
    context.SearchProperties = &props;
    props.clear();

    sleep(10);

    QuerySearch(RecodeToYandex(CODES_UTF8, "зайчик | охотник&gta=rty_sum_expected_docs_count&qi=rty_sum_expected_docs_count&gta=rty_setsum_rich_tree_stat&qi=rty_setsum_rich_tree_stat&gta=rty_sum_docs_count&qi=rty_sum_docs_count") + Kps, results, context);

    DEBUG_LOG << "----------------------------" << Endl;
    CHECK_TEST_EQ(results.size(), 3);
    CHECK_TEST_TRUE(context.SearchProperties->contains("rty_sum_docs_count"));
    CHECK_TEST_EQ((*context.SearchProperties).find("rty_sum_docs_count")->second, "4");
    CHECK_TEST_TRUE(context.SearchProperties->contains("rty_setsum_rich_tree_stat"));
    CHECK_TEST_NEQ((*context.SearchProperties).find("rty_setsum_rich_tree_stat")->second, "");
    CHECK_TEST_TRUE(context.SearchProperties->contains("rty_sum_expected_docs_count"));
    CHECK_TEST_EQ((*context.SearchProperties).find("rty_sum_expected_docs_count")->second, "3");
    CHECK_TEST_TRUE(context.SearchProperties->contains("ExpectedDocsCount.rty_sum_expected_docs_count"));
    CHECK_TEST_EQ((*context.SearchProperties).find("ExpectedDocsCount.rty_sum_expected_docs_count")->second, "3");

    props.clear();
    context.ResultCountRequirement = 2;
    QuerySearch(RecodeToYandex(CODES_UTF8, "раз зайчик&gta=rty_sum_expected_docs_count&qi=rty_sum_expected_docs_count&gta=rty_setsum_rich_tree_stat&qi=rty_setsum_rich_tree_stat&gta=rty_sum_docs_count&qi=rty_sum_docs_count") + Kps, results, context);

    CHECK_TEST_EQ(results.size(), 2);
    CHECK_TEST_TRUE(context.SearchProperties->contains("rty_sum_docs_count"));
    CHECK_TEST_EQ((*context.SearchProperties).find("rty_sum_docs_count")->second, "4");
    CHECK_TEST_TRUE(context.SearchProperties->contains("rty_setsum_rich_tree_stat"));
    CHECK_TEST_NEQ((*context.SearchProperties).find("rty_setsum_rich_tree_stat")->second, "");
    CHECK_TEST_TRUE(context.SearchProperties->contains("rty_sum_expected_docs_count"));
    CHECK_TEST_EQ((*context.SearchProperties).find("rty_sum_expected_docs_count")->second, "1");
    CHECK_TEST_TRUE(context.SearchProperties->contains("ExpectedDocsCount.rty_sum_expected_docs_count"));
    CHECK_TEST_EQ((*context.SearchProperties).find("ExpectedDocsCount.rty_sum_expected_docs_count")->second, "0");

    props.clear();
    context.ResultCountRequirement = 3;
    QuerySearch(RecodeToYandex(CODES_UTF8, "зайчик&gta=rty_sum_expected_docs_count&qi=rty_sum_expected_docs_count&gta=rty_setsum_rich_tree_stat&qi=rty_setsum_rich_tree_stat&gta=rty_sum_docs_count&qi=rty_sum_docs_count") + Kps, results, context);

    CHECK_TEST_EQ(results.size(), 3);
    CHECK_TEST_TRUE(context.SearchProperties->contains("rty_sum_docs_count"));
    CHECK_TEST_EQ((*context.SearchProperties).find("rty_sum_docs_count")->second, "4");
    CHECK_TEST_TRUE(context.SearchProperties->contains("rty_setsum_rich_tree_stat"));
    CHECK_TEST_NEQ((*context.SearchProperties).find("rty_setsum_rich_tree_stat")->second, "");
    CHECK_TEST_TRUE(context.SearchProperties->contains("rty_sum_expected_docs_count"));
    CHECK_TEST_EQ((*context.SearchProperties).find("rty_sum_expected_docs_count")->second, "2");
    CHECK_TEST_TRUE(context.SearchProperties->contains("ExpectedDocsCount.rty_sum_expected_docs_count"));
    CHECK_TEST_EQ((*context.SearchProperties).find("ExpectedDocsCount.rty_sum_expected_docs_count")->second, "2");

    props.clear();
    context.ResultCountRequirement = 3;
    QuerySearch(RecodeToYandex(CODES_UTF8, "зайчик&gta=rty_sum_expected_docs_count&qi=rty_sum_expected_docs_count") + Kps, results, context);
    CHECK_TEST_EQ(results.size(), 3);
    CHECK_TEST_TRUE(context.SearchProperties->contains("rty_sum_expected_docs_count"));
    CHECK_TEST_EQ((*context.SearchProperties).find("rty_sum_expected_docs_count")->second, "2");
    CHECK_TEST_TRUE(context.SearchProperties->contains("ExpectedDocsCount.rty_sum_expected_docs_count"));
    CHECK_TEST_EQ((*context.SearchProperties).find("ExpectedDocsCount.rty_sum_expected_docs_count")->second, "2");

    return true;
}
bool InitConfig() override {
    (*SPConfigDiff)["SearchConfig.ReArrangeOptions"] = "ExpectedDocsCount";
    if (!Callback || !Callback->GetNodesNames(TNODE_SEARCHPROXY).size())
        (*ConfigDiff)["Searcher.ReArrangeOptions"] = "ExpectedDocsCount";
    SetMergerParams(true, 1, 1, mcpTIME, Max<i32>());
    return true;
}
};

START_TEST_DEFINE_PARENT(TestSearchDistanceForAttrs, TestSearchSyntax)
bool Run() override {
    TVector<TString> texts;
    texts.push_back("<html><div group=\"222\">crrr</div>core<div group=\"111\">trrr</div>cored<div group=\"333\">grrr</div></html>");
    IndexTexts(texts, DISK);
    TVector<TDocSearchInfo> results;
    QuerySearch("crrr %26/(+1 +1) grr_attr:\"111\"" + Kps, results);
    CHECK_TEST_EQ(results.size(), 0);
    QuerySearch("crrr %26/(+1 +2) grr_attr:\"111\"" + Kps, results);
    CHECK_TEST_EQ(results.size(), 0);
    QuerySearch("crrr %26%26/(+1 +1) grr_attr:\"111\"" + Kps, results);
    CHECK_TEST_EQ(results.size(), 0);
    QuerySearch("crrr %26%26/(+1 +2) grr_attr:\"111\"" + Kps, results);
    CHECK_TEST_EQ(results.size(), 1);
    QuerySearch("crrr %26%26/(+1 +2) grr_attr:\"111\" %26%26/(+1 +2) grrr" + Kps, results);
    CHECK_TEST_EQ(results.size(), 1);
    QuerySearch("crrr %26%26/(+1 +2) grr_attr:\"111\" %26%26/(+1 +1) grrr" + Kps, results);
    CHECK_TEST_EQ(results.size(), 0);
    return true;
}
};

START_TEST_DEFINE_PARENT(TestSearchCamelCaseZoneNameXML, TestSearchSyntax)
bool Run() override {
    TVector<TString> texts;
    texts.push_back("<xml s_abcCase=\"asd\"><z_groupCase>crrr</z_groupCase><z_group_case>crrraaa</z_group_case></xml>");
    IndexTexts(texts, DISK, "text/xml");
    TVector<TDocSearchInfo> results;
    QuerySearch("z_group_case:crrraaa" + Kps, results);
    CHECK_TEST_EQ(results.size(), 1);
    QuerySearch("z_groupcase:crrr" + Kps, results);
    CHECK_TEST_EQ(results.size(), 1);
    QuerySearch("s_abccase:asd" + Kps, results);
    CHECK_TEST_EQ(results.size(), 1);
    return true;
}
bool InitConfig() override {
    const TString xmlParserConfig =
        "<XmlParser>\n"
        "<DOCTYPE>\n"
        "<Zones>\n"
        "_ : _\n"
        "</Zones>\n"
        "<Attributes>\n"
        "_ : LITERAL,any/_._\n"
        "</Attributes>\n"
        "</DOCTYPE>\n"
        "</XmlParser>\n";
    const TString xmlParserFileName = GetRunDir() + "/xmlparser.conf";
    TUnbufferedFileOutput out(xmlParserFileName);
    out << xmlParserConfig;
    SetIndexerParams(ALL, 10, 1);
    (*ConfigDiff)["Indexer.Common.XmlParserConfigFile"] = xmlParserFileName;
    return true;
}
};

START_TEST_DEFINE_PARENT(TestSearchDistanceForAttrsInSent, TestSearchSyntax)
bool Run() override {
    TVector<TString> texts;
    texts.push_back("<xml><sent><word grr_attr=\"222\">crrr</word><word grr_attr=\"111\">trrr</word><word grr_attr=\"333\">grrr</word></sent><sent><word grr_attr=\"vvv\">aaa</word><word grr_attr=\"uuu\">ggg</word><word grr_attr=\"fff\">ccc</word></sent></xml>");
    IndexTexts(texts, DISK, "text/xml");
    TVector<TDocSearchInfo> results;

    TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > > docProperties;
    TQuerySearchContext context;
    context.AttemptionsCount = 10;
    context.DocProperties = &docProperties;

    context.ResultCountRequirement = 1;
    QuerySearch("grr_attr:\"333\"&fsgta=__HitsInfo&rty_hits_detail=da" + Kps, results, context);
    CHECK_TEST_EQ(results.size(), 1);
    CHECK_TEST_EQ(docProperties[0]->count("__HitsInfo"), 2);
    CHECK_TEST_EQ(docProperties[0]->find("__HitsInfo")->second, "[{\"sent\":1,\"word\":3}]");
    context.ResultCountRequirement = 1;
    QuerySearch("trrr&fsgta=__HitsInfo&rty_hits_detail=da" + Kps, results, context);
    CHECK_TEST_EQ(results.size(), 1);
    CHECK_TEST_EQ(docProperties[0]->count("__HitsInfo"), 2);
    CHECK_TEST_EQ(docProperties[0]->find("__HitsInfo")->second, "[{\"sent\":1,\"word\":2}]");
    context.ResultCountRequirement = 1;
    QuerySearch("crrr %26/(+1 +1) grr_attr:\"111\"" + Kps, results, context);
    CHECK_TEST_EQ(results.size(), 1);
    context.ResultCountRequirement = 0;
    QuerySearch("grrr %26/(+1 +1) aaa" + Kps, results, context);
    CHECK_TEST_EQ(results.size(), 0);
    context.ResultCountRequirement = 1;
    QuerySearch("grrr %26%26/(+1 +1) aaa&fsgta=__HitsInfo&rty_hits_detail=da" + Kps, results, context);
    CHECK_TEST_EQ(results.size(), 1);
    CHECK_TEST_EQ(docProperties[0]->count("__HitsInfo"), 2);
    CHECK_TEST_EQ(docProperties[0]->find("__HitsInfo")->second, "[{\"sent\":2,\"word\":1},{\"sent\":1,\"word\":3}]");
    context.ResultCountRequirement = 1;
    QuerySearch("aaa %26/(+1 +1) ggg" + Kps, results, context);
    CHECK_TEST_EQ(results.size(), 1);
    context.ResultCountRequirement = 0;
    QuerySearch("crrr %26/(+2 +2) grr_attr:\"111\"" + Kps, results, context);
    CHECK_TEST_EQ(results.size(), 0);
    context.ResultCountRequirement = 0;
    QuerySearch("crrr %26%26/(+1 +1) grr_attr:\"111\"" + Kps, results, context);
    CHECK_TEST_EQ(results.size(), 0);

    context.ResultCountRequirement = 0;
    QuerySearch("grr_attr:\"222\" %26%26/(+1 +1) grr_attr:\"111\"" + Kps, results, context);
    CHECK_TEST_EQ(results.size(), 0);
    context.ResultCountRequirement = 1;
    QuerySearch("grr_attr:\"222\" %26/(+1 +1) grr_attr:\"111\"" + Kps, results, context);
    CHECK_TEST_EQ(results.size(), 1);
    context.ResultCountRequirement = 0;
    QuerySearch("grr_attr:\"222\" %26/(+2 +2) grr_attr:\"111\"" + Kps, results, context);
    CHECK_TEST_EQ(results.size(), 0);
    context.ResultCountRequirement = 1;
    QuerySearch("crrr %26/(+1 +1) grr_attr:\"111\" %26/(+1 +1) grrr" + Kps, results, context);
    CHECK_TEST_EQ(results.size(), 1);
    context.ResultCountRequirement = 1;
    QuerySearch("crrr %26/(+1 +1) grr_attr:\"111\" %26/(+1 +1) grr_attr:\"333\"" + Kps, results, context);
    CHECK_TEST_EQ(results.size(), 1);
    context.ResultCountRequirement = 1;
    QuerySearch("grr_attr:\"222\" %26/(+1 +1) grr_attr:\"111\" %26/(+1 +1) grr_attr:\"333\"" + Kps, results, context);
    CHECK_TEST_EQ(results.size(), 1);
    context.ResultCountRequirement = 0;
    QuerySearch("crrr %26%26/(+1 +1) grr_attr:\"111\" %26%26/(+1 +1) grrr" + Kps, results, context);
    CHECK_TEST_EQ(results.size(), 0);
    return true;
}
bool InitConfig() override {
    const TString xmlParserConfig =
        "<XmlParser>\n"
        "<DOCTYPE>\n"
        "<Zones>\n"
        "_ : _\n"
        "</Zones>\n"
        "<Textflags>\n"
        "BREAK_WORD : _._\n"
        "BREAK_PARAGRAPH : sent\n"
        "BREAK_WORD : word\n"
        "</Textflags>\n"
        "<Attributes>\n"
        "_ : LITERAL,any/_._\n"
        "</Attributes>\n"
        "</DOCTYPE>\n"
        "</XmlParser>\n";
    const TString xmlParserFileName = GetRunDir() + "/xmlparser.conf";
    TUnbufferedFileOutput out(xmlParserFileName);
    out << xmlParserConfig;
    SetIndexerParams(ALL, 10, 1);
    (*ConfigDiff)["Indexer.Common.XmlParserConfigFile"] = xmlParserFileName;
    return true;
}
};

START_TEST_DEFINE_PARENT(TestNGrammsSearch, TestSearchSyntax)
bool Run() override {
    TVector<TString> texts;
    texts.push_back("<xml><ngr_sent>79261234567 crrr</ngr_sent></xml>");
    IndexTexts(texts, DISK, "text/xml");
    TVector<TDocSearchInfo> results;

    TQuerySearchContext context;
    context.AttemptionsCount = 2;

    context.ResultCountRequirement = 1;
    QuerySearch("ngr_sent:(926123567)%20zone_softness:ngr_sent-50" + Kps, results, context);
    CHECK_TEST_EQ(results.size(), 1);

    context.ResultCountRequirement = 0;
    QuerySearch("ngr_sent:(926123567)%20zone_softness:ngr_sent-100" + Kps, results, context);
    CHECK_TEST_EQ(results.size(), 0);

    context.ResultCountRequirement = 1;
    QuerySearch("ngr_sent:(926123567)%20zone_softness:ngr_sent-30" + Kps, results, context);
    CHECK_TEST_EQ(results.size(), 1);

    context.ResultCountRequirement = 0;
    QuerySearch("ngr_sent:(927123557)%20zone_softness:ngr_sent-30" + Kps, results, context);
    CHECK_TEST_EQ(results.size(), 0);

    return true;
}
bool InitConfig() override {
    SetIndexerParams(ALL, 10, 1);
    (*ConfigDiff)["Indexer.Memory.Enabled"] = "false";
    (*ConfigDiff)["Indexer.Common.NGrammZones"] = "ngr_sent=3";
    (*ConfigDiff)["Searcher.FiltrationModel"] = "SIMPLE";

    const TString xmlParserConfig =
        "<XmlParser>\n"
        "<DOCTYPE>\n"
        "<Zones>\n"
        "_ : _\n"
        "</Zones>\n"
        "<Textflags>\n"
        "BREAK_WORD : _._\n"
        "BREAK_PARAGRAPH : sent\n"
        "BREAK_WORD : word\n"
        "</Textflags>\n"
        "<Attributes>\n"
        "_ : LITERAL,any/_._\n"
        "</Attributes>\n"
        "</DOCTYPE>\n"
        "</XmlParser>\n";
    const TString xmlParserFileName = GetRunDir() + "/xmlparser.conf";
    TUnbufferedFileOutput out(xmlParserFileName);
    out << xmlParserConfig;
    (*ConfigDiff)["Indexer.Common.XmlParserConfigFile"] = xmlParserFileName;

    return true;
}
};

START_TEST_DEFINE_PARENT(TestSearchSyntaxFiltersExact, TestSearchSyntax)
bool Run() override {
    TVector<TString> texts;
    texts.push_back("Лужков - бывший мэр Москвы");
    texts.push_back("лужков - бывший мэр москвы");
    texts.push_back("В москве есть лужки");
    texts.push_back("Теперь Москва без Лужкова");
    texts.push_back("z d s");
    texts.push_back("z d i s");
    texts.push_back("q w. r t. f g.");
    texts.push_back("q w. f g. r t.");
    IndexTexts(texts, DISK);

//    CheckQuery("лужков", 4);
    CheckQuery("!лужков", 2);
//    CheckQuery("!Лужков", 0);
    CheckQuery("!!лужков", 3);
    CheckQuery("!!Лужков", 3);
    CheckQuery("\"лужков\"", 2);
    CheckQuery("\"Лужков\"", 2);
    CheckQuery("\"z * s\"", 1);
    CheckQuery("\"z * * s\"", 1);
    CheckQuery("z ~/2 s", 1);
//    CheckQuery("q ~~/1 t", 1);

    CheckQuery("мэр <- aaaaaaa", 2);
    CheckQuery("aaaaaaa <- мэр", 0);
    return true;
}
};

START_TEST_DEFINE_PARENT(TestSearchSyntaxFreakTree, TestSearchSyntax)
bool Run() override {
    TVector<TString> texts;
    texts.push_back("grani.ru test test aaa");
    texts.push_back("grani.ru test test aaa");
    texts.push_back("world of warcraft");

    TVector<NRTYServer::TMessage> messages;
    const bool isPrefixed = GetIsPrefixed();
    GenerateInput(messages, texts.size(), NRTYServer::TMessage::ADD_DOCUMENT, isPrefixed);
    for (unsigned i = 0; i < texts.size(); ++i) {
        messages[i].MutableDocument()->SetBody(texts[i]);
        if (isPrefixed) {
            messages[i].MutableDocument()->SetKeyPrefix(messages.front().GetDocument().GetKeyPrefix());
            Kps = "&kps=" + ToString(messages.front().GetDocument().GetKeyPrefix());
        }
        {
            auto* attr = messages[i].MutableDocument()->AddSearchAttributes();
            attr->SetType(NRTYServer::TAttribute::INTEGER_ATTRIBUTE);
            attr->SetName("porno");
            attr->SetValue("1");
        }
        {
            auto* attr = messages[i].MutableDocument()->AddSearchAttributes();
            attr->SetType(NRTYServer::TAttribute::INTEGER_ATTRIBUTE);
            attr->SetName("ft");
            attr->SetValue("1");
        }
    }
    {
        auto* attr = messages[1].MutableDocument()->AddSearchAttributes();
        attr->SetType(NRTYServer::TAttribute::LITERAL_ATTRIBUTE);
        attr->SetName("link");
        attr->SetValue("grani.ru");
    }

    IndexMessages(messages, DISK, 1);
    ReopenIndexers();
    TVector<TDocSearchInfo> result;
    TQuerySearchContext context;
    context.AttemptionsCount = 5;
    context.ResultCountRequirement = 2;
    QuerySearch("(link:grani.ru | grani.ru) &&/(-32768 32768) porno:<450 << (ft:1 | ft:5)" + Kps, result, context);
    CHECK_TEST_EQ(result.size(), 2);
    QuerySearch("grani.ru&qtree=cHicrVVPSBRRGP--t7P69iUybQg2UEyT6ChUM9GCCGl0WuoiHiKWiJTYVopVtgKRDpNBiIgIXaTjht0KKQgvdguKDu527Bx061wne-_Nmz_OjtuSzunNe9_3-77f7_u9GXaN9VDQoR9MYhMH8mCABSNwEcaCfbDBgSvZYnYSbsIdqOA6wkuEOsI7hB0E_nxGaOBd408PKyo4wtPawXUVuwRcG7CvyKoKjCoweq82XZ09X3tsHDefmA9mq_fHgp1hC21VhsbKQBFkma3XP3ZzQakQJlHSgVG8ep2iDkYYYoEtcx3kSOuaL0BtgIUBopiNPBWmYIaXNkTpExW6AItQ6Z7v9hB4XWMVWVmxySk2miAg5clZAZokoLUlINPqGOWksegXlQQ2bw19CNH4IvjtWMEwMrbmgIOjWKpr1POwT-AYa5qcI9GJ7ONwc3xK2O3EHLOydSNnDl6wXdMdNkPobAr1jz83zgbofmYa43WUg_MDFG-cfEHUxIrMP9GxH9W4cLlENt9GM5thfEWGHvJ1ZianEwODJRg4JJYVXCB89q1IXik-ex61SCiJTX8T2Y2EBoS7GKxIAiUASRFgrRrQJ-nTviypE-nWBO_TjG_zVtM9mujT8mdOlC04tNrXROs2lH7FTPL9KE3yibDZhECs8b7pNbYaO43tTp2y75bH8tM0--L7JRYVXpZXgXiDjAbH-665NEpjO7joPC4Gc_AHoTbCeqO45lJLLInF2hFmczVRHePV931m3iC7ldBRa3rNZ51a7cPzDRIIKBPTpJuQysnjVs1MJg86-yz-y3D8bQXPMPmWP0nR6KO0b7o8OHHq27g7bsI50ZkjmS9FzHM-qXx2fq5WnZOezFwqOB3Yxs-oowj_n8-qTvf2MrrnaZySZmccGNnNhL9E7dC35Dc5wv_rMrKpxB-JlB9JOHQPNEikFQ-uI7ppMg2IVANd65gTPm5Ssphr27VS6LiVQptWCvFWCge30vpj9Pcja6rVCvZIW5K8Rntr4uAvT8OwqQ%2C%2C" + Kps, result, context);
    CHECK_TEST_EQ(result.size(), 1); // it's fail - in this tree we have incorrect synonyms section for Or operator

    context.ResultCountRequirement = 0;
    QuerySearch("wow" + Kps, result, context);
    CHECK_TEST_EQ(result.size(), 0);
    context.ResultCountRequirement = 1;
    QuerySearch("wow&qtree=cHicpZRNaBNREMdn3m7S122RUAnUlUIaik2F4lIQYkUqUiRQhFJU9PmBrdFGEFtSIaUgRFRIFbGiFz-Raj4PSVvxaGk9SU_JVb176UG8qRff7Eeaj5WCDWTe5M38J-_9Zna1Ma2dgw86eQBCzIAOJTGV0CEI-2EABtu5IkMgQ2DAUYjAKJyBSxC7-61lAeE5wiJS_grCRwT5-YxQxqgBYTz2ATn6QKdwEEJSYGAER58wU4_xEY0iPuxEKh2GMZwX7M17gcsTXMroL3dPaNJjV6LSVybafExXrt2YNH-0-hQdZ8iNwSyLH28qlhIsXWys1Dtji5ktli7o2OvUmWOciU0PRz_A3LD-xaNFJBzmYyYBggM1ZKCGjDfiJTIxdKhAAxI9ybQLDaQ9ian49ajeGth3IDQQGOgLBDFk1_Y0U4-tnnOKW0I36AsWdCtBYsd66BHNiuyYVLzHpVJSwFYdh2esZboliSCPrv9kWrQBAU-Mxy_Hx6_e1LEvuAXCpsBdKCw-7XYwVMVuJP5YJKo5zTBGtGrQvAX-Lw-UPA66FmOposC0ezFT1ucqU1LpYh1LSu11P26yMbEeerCdqz61Uw0oIdUAA8No76vUh5AqHqic-_dWBof1X4p2tqE93vJKealyh5pTHU2XF0Ls3b3NNqcrtsatJw8Vsyd2ht0RNvrW6cghzQ7JJ865oCf1uri-IbAgMCcwKzAjMF8HVV7639JP20tfoouWpTaEkimukVkXSo5MmkyB9grkZcnLWgHp5S1DKSVSkMmSyZDJk0mXzOgaRcmYBcjLkMmTSZfWGo53yuV0SkoiYYWS_BYFy8k1J9esXLNyzcg1I9e8XPNyTZfqR5rKyinBmimxpgHECyanYVm5OKzPM9dpWC2vbDsNv79-99ROg9S4TcMrrE6DzGiehpOaHaL3r_2OYanHO7g3SJw9Greqlpfo6DZQeobQzuKz4E7H3rmP3Zq507GHo-7nqv_Z9K2hrsnblXDA239614khoylH84fOPzrS9ePweDgA_QTCoMp_AacZtV8%2C" + Kps, result, context);
    CHECK_TEST_EQ(result.size(), 1);
    return true;
}
bool InitConfig() override {
    (*ConfigDiff)["Indexer.Memory.Enabled"] = "false";
    (*ConfigDiff)["Indexer.Common.XmlParserConfigFile"] = "";
    (*ConfigDiff)["Indexer.Common.HtmlParserConfigFile"] = "";
    return true;
}
};

START_TEST_DEFINE_PARENT(TestSearchSyntaxZonesModify, TestSearchSyntax)
bool Run() override {
    TVector<TString> texts;
    texts.push_back(
        "<xml><hhh><aaa zone_attr=\"asd\">cbcbcb</aaa></hhh><hhh zone_attr=\"bbb\">lololo lalala lululu</hhh><aaa>cccccc</aaa></xml>");

    TVector<NRTYServer::TMessage> messages;
    const bool isPrefixed = GetIsPrefixed();
    GenerateInput(messages, texts.size(), NRTYServer::TMessage::ADD_DOCUMENT, isPrefixed);
    for (unsigned i = 0; i < texts.size(); ++i) {
        messages[i].MutableDocument()->SetBody(texts[i]);
        messages[i].MutableDocument()->SetMimeType("text/xml");
        if (isPrefixed) {
            messages[i].MutableDocument()->SetKeyPrefix(messages.front().GetDocument().GetKeyPrefix());
            Kps = "&kps=" + ToString(messages.front().GetDocument().GetKeyPrefix());
        }
    }
    IndexMessages(messages, REALTIME, 1);
    CheckQuery("hhh:(\"lololo\")", 1);
    CheckQuery("lololo lalala lululu lilili softness:50", 1);
    CheckQuery("hhh:(zone_attr:\"bbb\")", 1);
    CheckQuery("zone_attr:\"bbb\"", 1);
    CheckQuery("zone_attr:\"asd\"", 1);
    CheckQuery("aaa:(zone_attr:\"bbb\")", 0);
    CheckQuery("aaa:(zone_attr:\"asd\")", 1);
    CheckQuery("hhh:(lololo lalala lululu lilili) softness:50", 1);
    CheckQuery("aaa:cbcbcb", 1);
    CheckQuery("hhh:cbcbcb", 1);
    CheckQuery("hhh:lalala", 1);
    ReopenIndexers();
    texts[0] =
        "<xml><sss zone_attr=\"abc\"><fff zone_attr=\"ccc\">cacacaca</fff><bbb zone_attr=\"bbb\">acacacac</bbb></sss><hhh>lololo lalala lylyly</hhh><aaa zone_attr=\"aaa\">cccccc</aaa></xml>";

    messages[0].MutableDocument()->SetBody(WideToUTF8(CharToWide(texts[0], csYandex)));
    messages[0].SetMessageType(NRTYServer::TMessage::MODIFY_DOCUMENT);
    messages[0].SetMessageId(IMessageGenerator::CreateMessageId());
    IndexMessages(messages, REALTIME, 1);

    CheckQuery("sss:(zone_attr:\"abc\")", 1);
    CheckQuery("fff:(zone_attr:\"ccc\")", 1);
    CheckQuery("bbb:(zone_attr:\"bbb\")", 1);
    CheckQuery("aaa:(zone_attr:\"aaa\")", 1);

    // Its strange, but ...
    CheckQuery("fff:(zone_attr:\"abc\")", 1);

    CheckQuery("sss:cacacaca", 1);
    CheckQuery("sss:acacacac", 1);
    CheckQuery("fff:cacacaca", 1);
    CheckQuery("bbb:acacacac", 1);
    CheckQuery("aaa:lylyly", 0);
    CheckQuery("hhh:lululu", 0);
    CheckQuery("hhh:lylyly", 1);
    CheckQuery("hhh:cccccc", 0);
    CheckQuery("aaa:cccccc", 1);
    return true;
}
bool InitConfig() override {
    (*ConfigDiff)["Indexer.Common.XmlParserConfigFile"] = "";
    (*ConfigDiff)["Indexer.Common.HtmlParserConfigFile"] = "";
    return true;
}
};

START_TEST_DEFINE_PARENT(TestSearchSyntaxZones, TestSearchSyntax)
bool Run() override {
    TVector<TString> texts;
    texts.push_back(
        "<div yx:HEADERS>Первый хэдер (заголовок)</div>\n"
        "<div yx:BODY_TEXT> Текст внутри документа </div>\n"
        "<div yx:HEADERS>Второй хэдер (подвал)</div>\n");
    texts.push_back(
        "<div yx:HEADERS>Первый хэдер (заголовок подвал)</div>\n"
        "<div yx:BODY_TEXT> Текст внутри документа </div>\n");
    IndexTexts(texts, DISK);
    //    CheckQuery("внутри headers:(заголовок подвал)", 2);
    CheckQuery("внутри headers:((заголовок подвал))", 1);
    CheckQuery("внутри headers:'заголовок подвал'", 2);
    CheckQuery("внутри headers:\"заголовок подвал\"", 1);
    CheckQuery("внутри headers:'заголовок подвала'", 2);
    CheckQuery("внутри headers:\"заголовок подвала\"", 0);
    return true;
}
};

START_TEST_DEFINE_PARENT(TestSearchSyntaxAttrs, TestSearchSyntax)
bool Run() override {
    TVector<TString> texts;
    texts.push_back(
        "<div yx:HEADERS>\n"
            "Первый хэдер (заголовок)\n"
            "<a date=\"19850813T110000\"/>\n"
        "</div>\n"
        "<a attachsize=\"1\" xxx_urls=\"http://www.yandex.ru\" tag=\"abc\" bool_flag=\"true\" app_date=\"19850813T110000\"/>\n"
        "<div yx:BODY_TEXT> Текст внутри документа </div>\n"
        "<div yx:HEADERS>Второй хэдер (подвал)</div>\n");
    texts.push_back(
        "<div yx:HEADERS>\n"
            "Первый хэдер (заголовок подвал)\n"
            "<a date=\"19850117T120000\"/>\n"
        "</div>\n"
        "<a attachsize=\"2\" xxx_urls=\"http://www.mail.ru\" tag=\"abs\" bool_flag=\"false\" app_date=\"19850117T120000\"/>\n"
        "<div yx:BODY_TEXT> Текст внутри документа </div>\n");
    texts.push_back(
        "<div yx:HEADERS>\n"
            "Первый хэдер (заголовок подвал)\n"
            "<a date=\"19860301T130000\"/>\n"
        "</div>\n"
        "<a attachsize=\"3\" xxx_urls=\"http://www.abs.ru\" tag=\"ghj\" bool_flag=\"false\" app_date=\"19860301T130000\"/>\n"
        "<div yx:BODY_TEXT> Текст внутри документа </div>\n");
    texts.push_back(
        "<div yx:HEADERS>\n"
        "Первый хэдер (заголовок подвал)\n"
        "<a date=\"19860301T130000\"/>\n"
        "</div>\n"
        "<a attachsize=\"3\" xxx_urls=\"http://www.tuv.ru\" tag=\"&quot;tuv&quot;\" bool_flag=\"false\" app_date=\"19860301T130000\"/>\n"
        "<div yx:BODY_TEXT> Текст внутри документа </div>\n");
    IndexTexts(texts, DISK);
//Common
    CheckQuery("внутри tag:abc", 1);
    CheckQuery("внутри tag:(abc)", 1);
    CheckQuery("внутри tag:\"abc\"", 1);
    CheckQuery("внутри tag:'abc'", 1);
    CheckQuery("внутри tag:(\"abc\")", 0);
    CheckQuery("внутри tag:('abc')", 0);
    CheckQuery("внутри tag:(\"tuv\")", 1);
    CheckQuery("внутри tag:\"tuv\"", 0);
    CheckQuery("внутри tag:tuv", 0);
//Literal
    CheckQuery("внутри tag:ab*", 2);
    CheckQuery("внутри tag:>abs", 1);
    CheckQuery("внутри tag:>=ab", 3);
    CheckQuery("внутри tag:<=abs", 3);
    CheckQuery("внутри tag:<abs", 2);
    CheckQuery("внутри tag:\"'ab'..'b'\"", 2);
    CheckQuery("внутри tag:\"'abc'..'abs'\"", 2);
    CheckQuery("внутри tag:\"'abc'..'abc'\"", 1);
    CheckQuery("внутри tag:\"'abc'..'abd'\"", 1);
    CheckQuery("внутри tag:\"'b'..'ab'\"", 0);
    CheckQuery("внутри tag:\"'a'..'abb'\"", 0);
//Date
/*
    CheckQuery("внутри apocalipse_date:19850813", 1);
    CheckQuery("внутри apocalipse_date:1985-08-13", 1);
    CheckQuery("внутри apocalipse_date:1985-08-13T11:00:00", 1);
    CheckQuery("внутри apocalipse_date:1985-08-13T11:00", 1);
    CheckQuery("внутри apocalipse_date:1985-08-13T11", 1);
    CheckQuery("внутри apocalipse_date:1985*", 2);
    CheckQuery("внутри apocalipse_date:1985*", 2);
    CheckQuery("внутри apocalipse_date:19850812..19870101", 2);
    CheckQuery("внутри apocalipse_date:>=19850813", 2);
    CheckQuery("внутри apocalipse_date:<=19850813", 2);
    CheckQuery("внутри apocalipse_date:>19850813", 1);
    CheckQuery("внутри apocalipse_date:<19850813", 1);
*/
/*
    CheckQuery("внутри headers:(received_date:19850813)", 1);
    CheckQuery("внутри headers:(received_date:1985-08-13)", 1);
    CheckQuery("внутри headers:(received_date:1985-08-13T11:00:00)", 1);
    CheckQuery("внутри headers:(received_date:1985-08-13T11:00)", 1);
    CheckQuery("внутри headers:(received_date:1985-08-13T11)", 1);
    CheckQuery("внутри headers:(received_date:1985*)", 2);
    CheckQuery("внутри headers:(received_date:1985*)", 2);
    CheckQuery("внутри headers:(received_date:19850812..19870101)", 2);
    CheckQuery("внутри headers:(received_date:>=19850813)", 2);
    CheckQuery("внутри headers:(received_date:<=19850813)", 2);
    CheckQuery("внутри headers:(received_date:>19850813)", 1);
    CheckQuery("внутри headers:(received_date:<19850813)", 1);
    */
//URL
/*
    CheckQuery("внутри xxx_urls:www.yandex.ru", 1);
    CheckQuery("внутри xxx_urls:http://www.yandex.ru", 1);
    CheckQuery("внутри xxx_urls:https://www.yandex.ru", 1);
    CheckQuery("внутри xxx_urls:>=www.mail.ru", 2);
    CheckQuery("внутри xxx_urls:<=www.mail.ru", 2);
    CheckQuery("внутри xxx_urls:>www.mail.ru", 1);
    CheckQuery("внутри xxx_urls:<www.mail.ru", 1);
*/
//Integer
    CheckQuery("внутри attachsize:2", 1);
    CheckQuery("внутри attachsize:1..3", 4);
    CheckQuery("внутри attachsize:>2", 2);
    CheckQuery("внутри attachsize:<2", 1);
    CheckQuery("внутри attachsize:>=2", 3);
    CheckQuery("внутри attachsize:<=2", 2);
//Boolean
/*
    CheckQuery("внутри bool_flag:yes", 1);
    CheckQuery("внутри bool_flag:true", 1);
    CheckQuery("внутри bool_flag:1", 1);
    CheckQuery("внутри bool_flag:on", 1);
    CheckQuery("внутри bool_flag:no", 2);
    CheckQuery("внутри bool_flag:false", 2);
    CheckQuery("внутри bool_flag:0", 2);
    CheckQuery("внутри bool_flag:off", 2);
*/
    return true;
}
};

START_TEST_DEFINE_PARENT(TestSearchInterval, TestSearchSyntax)
bool Run() override {
    TVector<TString> texts;

    const unsigned CountMessages = 4000;

    TVector<NRTYServer::TMessage> messages;

    bool IsPref = GetIsPrefixed();
    if (IsPref)
        Kps = "&kps=1";

    GenerateInput(messages, CountMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    for (unsigned i = 0; i < CountMessages; ++i) {
        NRTYServer::TAttribute* sa = messages[i].MutableDocument()->AddSearchAttributes();
        sa->set_name("hid");
        sa->set_value(ToString(i));
        sa->set_type(NRTYServer::TAttribute::LITERAL_ATTRIBUTE);

        sa = messages[i].MutableDocument()->AddSearchAttributes();
        sa->set_name("attachsize");
        sa->set_value(ToString(i));
        sa->set_type(NRTYServer::TAttribute::INTEGER_ATTRIBUTE);

        sa = messages[i].MutableDocument()->AddSearchAttributes();
        sa->set_name("attachsize_b");
        sa->set_value(ToString(i));
        sa->set_type(NRTYServer::TAttribute::INTEGER_ATTRIBUTE);
        if (IsPref)
            messages[i].MutableDocument()->SetKeyPrefix(1);
    }
    IndexMessages(messages, REALTIME, 1);
    CheckQuery("attachsize:>2000 attachsize:<4000&numdoc=10000&relev=attr_limit%3D2000000000&pron=keepalldocs", 1999);

    CheckQuery("attachsize_b:>2000 attachsize:<4000&numdoc=10000&relev=attr_limit%3D2000000000&pron=keepalldocs", 1999);

    CheckQuery("attachsize:<2000&numdoc=10000&relev=attr_limit%3D2000000000&pron=keepalldocs", 2000);

    CheckQuery("attachsize_b:<2000&numdoc=10000&relev=attr_limit%3D2000000000&pron=keepalldocs", 2000);

    ReopenIndexers();

    CheckQuery("attachsize:>2000 attachsize:<4000&numdoc=10000&relev=attr_limit%3D2000000000&pron=keepalldocs", 1999);

    CheckQuery("attachsize:<2000&numdoc=10000&relev=attr_limit%3D2000000000&pron=keepalldocs", 2000);

    return true;
}
public:
    bool InitConfig() override {
        SetIndexerParams(REALTIME, 20000, 4);
        SetIndexerParams(DISK, 20000, 4);
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestSearchRefine, TestSearchSyntax)
bool Run() override {
    TVector<TString> texts;
    texts.push_back("a1");
    texts.push_back("a2");
    texts.push_back("a3");
    texts.push_back("a1");
    texts.push_back("a2");
    texts.push_back("a3");
    TVector<NRTYServer::TMessage> messages = IndexTexts(texts, REALTIME);
    THashMap<TString, TString> urlToText;
    for (TVector<NRTYServer::TMessage>::const_iterator i = messages.begin(); i != messages.end(); ++i)
        urlToText[i->GetDocument().GetUrl()] = i->GetDocument().GetBody();
    TVector<TDocSearchInfo> results;
    QuerySearch("(a1 | a2 | a3) <- (a2 | a3) <- a3" + Kps, results);
    if (results.size() < texts.size())
        ythrow yexception() << "some documents not found";
    if (urlToText[results[0].GetUrl()] != "a3" || urlToText[results[1].GetUrl()] != "a3")
        ythrow yexception() << "first two results must be 'a3'";
    if (urlToText[results[2].GetUrl()] != "a2" || urlToText[results[3].GetUrl()] != "a2")
        ythrow yexception() << "second two results must be 'a2'";
    if (urlToText[results[4].GetUrl()] != "a1" || urlToText[results[5].GetUrl()] != "a1")
        ythrow yexception() << "third two results must be 'a1'";
    return true;
}
};

START_TEST_DEFINE(TestWordsFreqs)
    bool Run() override {
        int countMessages = 3;
        TStandartDocumentGenerator* sdg = new TStandartDocumentGenerator(GetIsPrefixed());
        TStandartMessagesGenerator smg(sdg, true);
        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, countMessages, smg);
        messages[0].MutableDocument()->SetBody("aaa bbb");
        messages[1].MutableDocument()->SetBody("aaa ccc");
        messages[2].MutableDocument()->SetBody("bbb fff");
        IndexMessages(messages, REALTIME, 1);
        TString kps = "";
        if (GetIsPrefixed())
            kps = "&kps=" + ToString(messages[0].MutableDocument()->GetKeyPrefix());

        TVector<TDocSearchInfo> results;
        QuerySearch("aaa::10 bbb::1000 softness:50&pron=sortbyTRDocQuorum" + kps, results);
        CHECK_TEST_EQ(results.size(), 2);
        CHECK_TEST_EQ(results[0].GetUrl(), messages[0].GetDocument().GetUrl());
        CHECK_TEST_EQ(results[1].GetUrl(), messages[2].GetDocument().GetUrl());

        QuerySearch("aaa::40 bbb::60 softness:41" + kps, results);
        CHECK_TEST_EQ(results.size(), 2);

        QuerySearch("aaa::40 bbb::60 softness:40" + kps, results);
        CHECK_TEST_EQ(results.size(), 1);

        QuerySearch("aaa::40 bbb::60 softness:39" + kps, results);
        CHECK_TEST_EQ(results.size(), 1);

        QuerySearch("aaa::100 bbb::10 softness:50&pron=sortbyTRDocQuorum" + kps, results);
        CHECK_TEST_EQ(results.size(), 2);
        CHECK_TEST_EQ(results[0].GetUrl(), messages[0].GetDocument().GetUrl());
        CHECK_TEST_EQ(results[1].GetUrl(), messages[1].GetDocument().GetUrl());

        QuerySearch("aaa bbb softness:50&pron=sortbyTRDocQuorum" + kps, results);
        CHECK_TEST_EQ(results.size(), 3);
        return true;
    }
    bool InitConfig() override {
        (*ConfigDiff)["Searcher.FiltrationModel"] = "WEIGHT";
        return true;
    }

};

START_TEST_DEFINE_PARENT(TestSearchQuorumFormula, TestSearchSyntax)
    bool Run() override {
        TVector<TString> texts;
        texts.push_back("a");
        texts.push_back("a b");
        texts.push_back("b c");
        TVector<NRTYServer::TMessage> messages = IndexTexts(texts, REALTIME);
        THashMap<TString, TString> urlToText;
        for (TVector<NRTYServer::TMessage>::const_iterator i = messages.begin(); i != messages.end(); ++i)
            urlToText[i->GetDocument().GetUrl()] = i->GetDocument().GetBody();
        TVector<TDocSearchInfo> results;
        QuerySearch("(a b) <- (c) softness:100" + Kps, results);
        if (results.size() < texts.size())
            ythrow yexception() << "some documents not found";
        if (urlToText[results[0].GetUrl()] != "b c")
            ythrow yexception() << "first result must be 'b'";
        if (urlToText[results[1].GetUrl()] != "a b")
            ythrow yexception() << "second result must be 'a b'";
        if (urlToText[results[2].GetUrl()] != "a")
            ythrow yexception() << "third result must be 'a'";
        return true;
    }
    bool InitConfig() override {
        (*ConfigDiff)["Searcher.FiltrationModel"] = "SIMPLE";
        (*ConfigDiff)["Searcher.FactorsInfo"] = GetResourcesDirectory() + "/text_relev/text_factors.cfg";
        return true;
    }
};

SERVICE_TEST_RTYSERVER_DEFINE(TestMultiLongTitle)

TVector<NRTYServer::TMessage> messages;

bool PrepareTest(TString mimeType, int count) {
    TString TitleTextA = "";
    for (int i = 0; i < count; i++) {
        TitleTextA += ToString(i) + "abs ";
    }
    TString TitleTextB = "";
    for (int i = 0; i < count; i++) {
        TitleTextB += ToString(i) + "ass ";
    }
    static const TString docBody =
        "<?xml version=\"1.0\" encoding=\"utf-8\"?><text><title lang=\"en\">" + TitleTextA + "</title>\n"
        "<title lang=\"ru\" default=\"1\">" + TitleTextB + "</title>\n"
        "<body lang=\"en\">asdasd sad sdasd</body></text>\n";
    GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), docBody);
    messages.back().MutableDocument()->SetMimeType("text/" + mimeType);
    IndexMessages(messages, REALTIME, 1);
    sleep(2);
    return true;
}

bool Test(TString suffix) {
    TString kps = "&kps=" + ToString(messages.back().GetDocument().GetKeyPrefix());
    if (!CheckExistsByText("title:(999" + suffix + ")" + kps, false, messages)) {
        ERROR_LOG << "Incorrect test case " << suffix << "(999)" + kps << Endl;
        return false;
    }
    if (!CheckExistsByText("title:(1" + suffix + ")" + kps, false, messages)) {
        ERROR_LOG << "Incorrect test case " << suffix << "(1)" + kps << Endl;
        return false;
    }
    return true;
}

public:
    bool InitConfig() override {
        SetIndexerParams(DISK, 10, 1);
        (*ConfigDiff)["Indexer.Common.XmlParserConfigFile"] = "";
        (*ConfigDiff)["Indexer.Common.HtmlParserConfigFile"] = "";
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestHtmlMultiLongTitle, TestMultiLongTitle)
bool Run() override {
    PrepareTest("html", 1000);
    if (!Test("abs"))
        ythrow yexception() << "Incorrect abs case memory search";
    if (Test("ass"))
        ythrow yexception() << "Incorrect ass case memory search";
    ReopenIndexers();
    if (!Test("abs"))
        ythrow yexception() << "Incorrect abs case base search";
    if (Test("ass"))
        ythrow yexception() << "Incorrect ass case base search";

    return true;
};
};

START_TEST_DEFINE_PARENT(TestXmlMultiLongTitle, TestMultiLongTitle)
bool Run() override {
    PrepareTest("xml", 1000);
    if (!Test("abs"))
        ythrow yexception() << "Incorrect abs case memory search";
    if (!Test("ass"))
        ythrow yexception() << "Incorrect ass case memory search";
    ReopenIndexers();
    if (!Test("abs"))
        ythrow yexception() << "Incorrect abs case base search";
    if (!Test("ass"))
        ythrow yexception() << "Incorrect ass case base search";

    return true;
};
};

START_TEST_DEFINE_PARENT(TestSyntaxExclusion, TestSearchSyntax)
bool Run() override {
    const std::pair<TString, TString> data[] = {
        { "sasha", "chibisov" },
        { "misha", "chibisov" },
        { "gena", "chibisov" },
        { "vania", "ivanov" }
    };

    TVector<NRTYServer::TMessage> messages;
    for (ui32 i = 0; i < Y_ARRAY_SIZE(data); ++i) {
        NRTY::TAction action;
        action.SetActionType(NRTY::TAction::atAdd);
        action.SetId(i + 1);
        action.SetPrefix(GetIsPrefixed() ? 1 : 0);
        NRTY::TDocument& doc = action.AddDocument();
        doc.SetUrl(ToString(i));
        doc.SetMimeType("text/html");
        doc.AddSearchAttribute("test", data[i].first);
        doc.AddSearchAttribute("tag", data[i].second);
        messages.push_back(action.ToProtobuf());
    }

    IndexMessages(messages, DISK, 1);
    ReopenIndexers();

    if (GetIsPrefixed())
        Kps = "&kps=1";

    CheckQuery(CGIEscapeRet("(url:\"*\" ~ test:\"gena\") && tag:\"chibisov\""), 2);
    CheckQuery(CGIEscapeRet("tag:\"chibisov\" && (url:\"*\" ~ test:\"gena\")"), 2);

    return true;
}
};

START_TEST_DEFINE_PARENT(TestSyntaxExclusionCases, TestSearchSyntax)
bool Run() override {
    const std::pair<TString, TString> data[] = {
        { "sasha", "chibisov" },
        { "misha", "chibisov" },
        { "gena", "chibisov" },
        { "vania", "ivanov" },
        { "petya", "ivanov" },
    };

    TVector<NRTYServer::TMessage> messages;
    for (ui32 i = 0; i < Y_ARRAY_SIZE(data); ++i) {
        NRTY::TAction action;
        action.SetActionType(NRTY::TAction::atAdd);
        action.SetId(i + 1);
        action.SetPrefix(GetIsPrefixed() ? 1 : 0);
        NRTY::TDocument& doc = action.AddDocument();
        doc.SetUrl(ToString(i));
        doc.SetMimeType("text/html");
        doc.AddZone("text").SetText(data[i].second);
        doc.AddSearchAttribute("test", data[i].first);
        doc.AddSearchAttribute("attachsize", i);
        messages.push_back(action.ToProtobuf());
    }

    IndexMessages(messages, DISK, 1);
    ReopenIndexers();

    if (GetIsPrefixed())
        Kps = "&kps=1";

    CheckQuery("url:\"*\" ~~ text:\"chibisov\"", 2);
    CheckQuery("url:\"*\" ~~ test:\"misha\"", 4);
    CheckQuery("url:\"*\" ~~ attachsize:<3", 2);
    CheckQuery("url:\"*\" ~~ (attachsize:<3 attachsize:>1)", 4);
    CheckQuery("chibisov ~~ attachsize:<2", 1);
    CheckQuery("test:\"misha\" ~~ attachsize:<3", 0);
    return true;
}
};

START_TEST_DEFINE_PARENT(TestSyntaxExclusionXml, TestSearchSyntax)
bool Run() override {
    TVector<TString> texts;
    texts.push_back("<root suid=\"sasha\" hid=\"chibisov\"/>");
    texts.push_back("<root suid=\"misha\" hid=\"chibisov\"/>");
    texts.push_back("<root suid=\"gena\" hid=\"chibisov\"/>");
    texts.push_back("<root suid=\"vania\" hid=\"ivanov\"/>");
    IndexTexts(texts, DISK, "text/xml");

    CheckQuery(CGIEscapeRet("(url:\"*\" ~ suid:\"gena\") && hid:\"chibisov\""), 2);
    CheckQuery(CGIEscapeRet("hid:\"chibisov\" && (url:\"*\" ~ suid:\"gena\")"), 2);

    return true;
}
};

START_TEST_DEFINE_PARENT(TestSyntaxException, TestSearchSyntax)
bool Run() override {
    TVector<TString> texts;

    TString req = "(yandex () <- xxx_urls:(yandex() tag:\"chibisov\"";
    Quote(req);
    TString query = "/?ms=proto&hr=da&text=" + req + "&strictsyntax=yes";

    if (GetIsPrefixed())
        query += "&kps=1";

    TString result;

    Controller->ProcessQuery(query, &result);
    if (result.find("Syntax error") == TString::npos) {
        TEST_FAILED("Incorrect exception type");
    }
    return true;
}
};
