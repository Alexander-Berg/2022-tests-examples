#include <saas/rtyserver_test/testerlib/rtyserver_test.h>

#include <util/system/env.h>

SERVICE_TEST_RTYSERVER_DEFINE(THTMLParserTestCase)
    void Test(TIndexerType indexer) {
    static const TString docBody =
        "<div yx:HDR_FROM>Pavel Guschin paradigm@yandex-team.ru </div><div yx:HDR_TO>Dmitry Filatov dfilatov@yandex-team.ru Ilya Vlasuk tail@yandex-team.ru Evklid Nikiforov euclid@yandex-team.ru Alexey Zatelepin ztlpn@yandex-team.ru Dmitry Suhov quoter@yandex-team.ru Alina Goryunova agoryunova@yandex-team.ru Andrey Karmatsky karmatsky@yandex-team.ru Sergey Lobov akbars@yandex-team.ru Sergey Kovalenko kesha@yandex-team.ru</div>\n"
        "<div yx:HDR_SUBJECT>Планы НК на q2 <noindex>bellamy</noindex> </div>\n"
        "<div yx:BODY_TEXT> \n\n <span>Коллеги, \n</span>Я тут сделал драфт планов НК на второй квартал http://wiki.yandex-team.ru/JandeksKarty/projects/wikimaps/plan#q2 Прошу вас посмотреть на него, прокомментировать (если надо) и добавить, если я что-то упустил. -- Павел Гущин http://staff.yandex-team.ru/paradigm </div>\n"
        "<div yx:X_URLS>http schemas.microsoft.com omml http%3A%2F%2Fstaff.yandex-team.ru%2F\n"
        "http%3A%2F%2Fwiki.yandex-team.ru%2FJandeksKarty%2Fprojects%2Fwikimaps%2Fplan%23q2\n"
        "http%3A%2F%2Fwww.w3.org%2FTR%2FREC-html40</div>\n"
        "<div yx:HEADERS>Received: from mxcorp2.mail.yandex.net ([127.0.0.1])     by mxcorp2.mail.yandex.net with LMTP id EY1usww5        Mon, 4 Apr 2011 15:14:34 +0400</div>\n"
        "<div yx:ATTACHNAME><a>to1</a>to2<b>to3</b></div><div yx:ATTACHNAME><a>to4</a>to5<b>to6</b></div>"
        "<div yx:HDR_CC><a>cc1</a>cc2<b>cc3</b></div><div yx:HDR_CC><a>cc4</a>cc5<b>cc6</b></div>"
        "<div yx:HDR_BCC><a>bcc1</a>bcc2<b>bcc3</b></div><div yx:HDR_BCC><a>bcc4</a>bcc5<b>bcc6</b></div>"
        "<div yx:REPLY_TO><a>rto1</a>rto2<b>rto3</b></div><div yx:REPLY_TO><a>rto4</a>rto5<b>rto6</b></div>"
        "<img src=\"http://crimea-news.com/img/20150601/489f78508038d2679b23b3046a384e0e.jpg\" alt=\"alternative text\" />"
        "<asdasd><a><div marker = \"1\">123</div><div>111111</div><div marker=\"1\">321</div></a></asdasd>";

        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), docBody);
        messages.back().MutableDocument()->SetMimeType("text/html");
        if (GetIsPrefixed())
            messages.back().MutableDocument()->SetKeyPrefix(1);
        IndexMessages(messages, indexer, 1);
        if (indexer == DISK)
            ReopenIndexers();
        else
            Sleep(TDuration::Seconds(5));
        if (!CheckExistsByText("hdr_from:(Guschin)", false, messages)) {
            ythrow yexception() << "Incorrect parsing - A case";
        }
        if (CheckExistsByText("hdr_from:(Планы)", false, messages)) {
            ythrow yexception() << "Incorrect parsing - B case";
        }
        if (!CheckExistsByText("hdr_subject:(Планы)", false, messages)) {
            ythrow yexception() << "Incorrect parsing - C case";
        }
        if (CheckExistsByText("hdr_subject:(bellamy)", false, messages)) {
            ythrow yexception() << "Incorrect parsing - <noindex> case";
        }
        if (CheckExistsByText("alternative", false, messages)) {
            ythrow yexception() << "Incorrect parsing - img alt";
        }
    }
public:
    bool InitConfig() override {
        SetIndexerParams(DISK, 10, 1);
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestHtmlParserDisk, THTMLParserTestCase)
    bool Run() override {
        Test(DISK);
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestHtmlParserMemory, THTMLParserTestCase)
    bool Run() override {
        Test(REALTIME);
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestHtmlParserExtractZones, THTMLParserTestCase)
void ThrowError(const THashMap<TString, TVector<TString>>::value_type& expected, std::pair<THashMultiMap<TString, TString>::const_iterator, THashMultiMap<TString, TString>::const_iterator> find) {
    TStringStream ss;
    ss << "invalid value for property '" << expected.first << "', expected [";
    for (const auto& i : expected.second) {
        if (ss.Str().back() != '[')
            ss << ",";
        ss << "'" << i << "'";
    }
    ss << "], get [";
    for (; find.first != find.second; ++find.first) {
        if (ss.Str().back() != '[')
            ss << ",";
        ss << "'" << find.first->second << "'";
    }
    ss << "]";
    ythrow yexception() << ss.Str();
}

void Check(const THashMultiMap<TString, TString>& props, const TMap<TString, TVector<TString>>& expected) {
    for (const auto& p : expected) {
        DEBUG_LOG << "Check " << p.first << Endl;
        auto findPair = props.equal_range(p.first);
        TVector<TString> findV;
        for (THashMultiMap<TString, TString>::const_iterator i = findPair.first; i != findPair.second; ++i)
            findV.push_back(i->second);
        Sort(findV.begin(), findV.end());
        std::pair<TVector<TString>::const_iterator, TVector<TString>::const_iterator> find(findV.begin(), findV.end());
        for (const auto& val : p.second) {
            if (find.first == find.second)
                ThrowError(p, findPair);
            if (*find.first != val)
                ThrowError(p, findPair);
            ++find.first;
        }
        if (find.first != find.second)
            ThrowError(p, findPair);
    }
}
bool Run() override {
    Test(REALTIME);
    TVector<TDocSearchInfo> results;
    TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > > properties;
    TString kps = GetIsPrefixed() ? "&kps=1" : "";
    QuerySearch("url:\"*\"" + kps, results, &properties);
    if (properties.size() != 1)
        ythrow yexception() << "incorrect results count: " << properties.size() << " != 1";
    TMap<TString, TVector<TString>> expected;
    expected["prop_cc"].push_back("cc1cc2cc3");
    expected["prop_cc"].push_back("cc4cc5cc6");
    expected["prop_bcc"].push_back("bcc1bcc2bcc3bcc4bcc5bcc6");
    expected["prop_to"].push_back("<a>to1</a>to2<b>to3</b>");
    expected["prop_to"].push_back("<a>to4</a>to5<b>to6</b>");
    expected["prop_rto"].push_back("rto1");
    expected["prop_rto"].push_back("rto2");
    expected["prop_rto"].push_back("rto3");
    expected["prop_rto"].push_back("rto4");
    expected["prop_rto"].push_back("rto5");
    expected["prop_rto"].push_back("rto6");
    expected["p_text"].push_back("123");
    expected["p_text"].push_back("321");
    expected["prop_body"].push_back("<span>Коллеги,</span>Я тут сделал драфт планов НК на второй квартал http://wiki.yandex-team.ru/JandeksKarty/projects/wikimaps/plan#q2 Прошу вас посмотреть на него, прокомментировать (если надо) и добавить, если я что-то упустил. -- Павел Гущин http://staff.yandex-team.ru/paradigm");
    Check(*properties[0], expected);
    return true;
}
bool InitConfig() override {
    if (!THTMLParserTestCase::InitConfig())
        return false;
    (*ConfigDiff)["Indexer.Common.ZonesToProperties.reply_to"] = "prop_rto";
    (*ConfigDiff)["Indexer.Common.ZonesToProperties.hdr_cc"] = "prop_cc,JOIN_PARAGRAPHS";
    (*ConfigDiff)["Indexer.Common.ZonesToProperties.hdr_bcc"] = "prop_bcc,JOIN_SPANS,JOIN_PARAGRAPHS";
    (*ConfigDiff)["Indexer.Common.ZonesToProperties.attachname"] = "prop_to,NO_STRIP_TAGS,JOIN_PARAGRAPHS";
    (*ConfigDiff)["Indexer.Common.ZonesToProperties.body_text"] = "prop_body,NO_STRIP_TAGS,JOIN_PARAGRAPHS,STRIP_NEWLINES";
    return true;
}
};

SERVICE_TEST_RTYSERVER_DEFINE(THTMLParserTestCaseGroupAttrs)
void Test(TIndexerType indexer) {
    static const TString docBody =
        "<meta name=\"attr_aa\" content=\"attr_aa_value\">\n"
        "<meta name=\"attr_bb\" content=\"attr_bb_value1\">sdas</meta>\n"
        "<meta name=\"attr_bb\" content=\"attr_bb_value2\">sdassada</meta>\n"
        "<meta name=\"attr_cc\" content=\"attr_cc_value\">321</meta>\n";
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), docBody);
    messages.back().MutableDocument()->SetMimeType("text/html");
    IndexMessages(messages, indexer, 1);
    if (indexer == DISK)
        ReopenIndexers();
    else
        Sleep(TDuration::Seconds(5));
    TString kpsStr = "&kps=" + ToString(messages.back().MutableDocument()->GetKeyPrefix());
    CheckExistsByText("attr_bb_prop:\"attr_bb_value1\"" + kpsStr, false, messages);
    CheckExistsByText("attr_bb_prop:\"attr_bb_value2\"" + kpsStr, false, messages);
    if (!CheckExistsByText("url:%22*%22&g=1.attr_aa_grp.10.10.-1" + kpsStr, false, messages)) {
        ythrow yexception() << "Incorrect parsing - A case";
    }
    TVector<TDocSearchInfo> results;
    TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > > docProperties;
    QuerySearch("url:%22*%22" + kpsStr, results, &docProperties);
    if (docProperties.size() != 1)
        ythrow yexception() << "Incorrect doc count";
    std::pair<THashMultiMap<TString, TString>::const_iterator, THashMultiMap<TString, TString>::const_iterator> range = docProperties[0]->equal_range("attr_bb_prop");
    bool val[]= {false, false};
    for (;range.first != range.second; ++range.first)
        if (range.first->second == "attr_bb_value1")
            val[0] = true;
        else if (range.first->second == "attr_bb_value2")
            val[1] = true;
    if (!val[0] || !val[1])
        ythrow yexception() << "incorrect props";

    if (!CheckExistsByText("url:%22*%22&g=1.attr_cc_grp.10.10.-1" + kpsStr, false, messages))
        ythrow yexception() << "HTML parser and literal grouping attributes failure";
}
public:
    bool InitConfig() override {
        SetIndexerParams(DISK, 10, 1);
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestHtmlParserGroupAttrsDisk, THTMLParserTestCaseGroupAttrs)
bool Run() override {
    Test(DISK);
    return true;
}
};

START_TEST_DEFINE_PARENT(TestHtmlParserGroupAttrsMemory, THTMLParserTestCaseGroupAttrs)
bool Run() override {
    Test(REALTIME);
    return true;
}
};


START_TEST_DEFINE(TestMemSearchZone)
bool Run() override {
    ui16 port = Controller->GetConfig().Indexer.Port;
    SetEnv("INDEXER_PORT", ToString(port));
    if (!Callback->RunNode("run_indexing")) {
        ythrow yexception() << "fail to run indexing" << Endl;
    }
    TVector<TDocSearchInfo> results;
    QuerySearch("z_keywords:((%C7%E0%EF%F0%E5%F2%E8%F2%FC%20%E8%F1%EF%EE%EB%FC%E7%EE%E2%E0%ED%E8%E5%20%EF%F0%EE%F4%E8%EB%E5%E9))&kps=1", results);
    if (results.size() != 1) {
        ythrow yexception() << "incorrect docs number: " << results.size() << " != 1";
    }
    return true;
}
};
