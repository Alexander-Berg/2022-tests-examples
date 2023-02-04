#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <util/charset/wide.h>
#include <dict/dictutil/dictutil.h>

SERVICE_TEST_RTYSERVER_DEFINE(TXMLParserTestCase)
    void Test(TIndexerType indexer) {
        static const TString docBody = "<root\n"
            "X_URLS=\"url.domain.ctry\"\n"
            "attachsize=\"1234567\"\n"
            "attachsize_b=\"1\"\n"
            "attachtype=\"trash\"\n"
            "suid=\"123456\"\n"
            "mid=\"654321\"\n"
            "hid=\"123654\">\n"
            "<HEADER received_date=\"15.08.2018\">\n"
            "header_text\n"
            "</HEADER>\n"
            "<HDR_SUBJECT>\n"
            "subject\n"
            "</HDR_SUBJECT>\n"
            "<HDR_TO>\n"
            "to_address\n"
            "</HDR_TO>\n"
            "<HDR_FROM>\n"
            "from_address\n"
            "</HDR_FROM>\n"
            "<HDR_CC>\n"
            "cc_address\n"
            "</HDR_CC>\n"
            "<HDR_BCC>\n"
            "bcc_address\n"
            "</HDR_BCC>\n"
            "<grrr grr_attr=\"дурак\">"
            "hrr asa bsb"
            "</grrr>"
            "<REPLY_TO>\n"
            "reply_to_address\n"
            "</REPLY_TO>\n"
            "<ATTACHNAME>\n"
            "attach name SoMeq12TrnVH8kly11333\n"
            "</ATTACHNAME>\n"
            "<FAKESECTION>\n"
            "testfake section\n"
            "</FAKESECTION>\n"
            "<BODY_TEXT>\n"
            "this is latter about life and death, happiness and grief. Bla-bla-bla.\n"
            "---\n"
            "Author.\n"
            "email@domain.ctry\n"
            "</BODY_TEXT>\n"
            "</root>\n";
        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), docBody);
        messages.back().MutableDocument()->SetMimeType("text/xml");
        IndexMessages(messages, indexer, 1);
        if (indexer == DISK)
            ReopenIndexers();
        else
            Sleep(TDuration::Seconds(5));

        if (!CheckExistsByText("grr_attr:(дурак)", false, messages)) {
            ythrow yexception() << "Incorrect parsing - A1 case";
        }
        if (CheckExistsByText("FAKESECTION:(section)", false, messages)) {
            ythrow yexception() << "Incorrect parsing - A case";
        }
        if (CheckExistsByText("FAKESECTION:(attach)", false, messages)) {
            ythrow yexception() << "Incorrect parsing - B case";
        }
        if (CheckExistsByText("attachname:(fake)", false, messages)) {
            ythrow yexception() << "Incorrect parsing - C case";
        }
        if (!CheckExistsByText("attachname%3A%28attach+name%29", false, messages)) {
            ythrow yexception() << "Incorrect parsing - D case";
        }
        if (CheckExistsByText("fakesection", false, messages)) {
            ythrow yexception() << "Incorrect parsing - E case";
        }
        if (!CheckExistsByText("SoMeq12TrnVH8kly11333", false, messages)) {
            ythrow yexception() << "Incorrect parsing - G case";
        }
        if (!CheckExistsByText("testfake", false, messages)) {
            ythrow yexception() << "Incorrect parsing - H case";
        }
        if (!CheckExistsByText("attachname%3Aname", false, messages)) {
            ythrow yexception() << "Incorrect parsing - K case";
        }
    }
public:
    bool InitConfig() override {
        SetIndexerParams(DISK, 10, 1);
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestXmlParserDisk, TXMLParserTestCase)
    bool Run() override {
        Test(DISK);
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestXmlParserMemory, TXMLParserTestCase)
    bool Run() override {
        Test(REALTIME);
        return true;
    }
};

START_TEST_DEFINE(TestXmlParserExtractZones)
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
    TString body = "<xml>"
        "<ATTACHNAME><a>to1</a>to2<b abc=\"123\">to3</b></ATTACHNAME><ATTACHNAME><a>to4</a>to5<b>to6</b></ATTACHNAME>"
        "<HDR_CC><a>cc1</a>cc2<b>cc3</b></HDR_CC><HDR_CC><a>cc4</a>cc5<b>cc6</b></HDR_CC>"
        "<HDR_BCC><a>bcc1</a>bcc2<b>bcc3</b></HDR_BCC><HDR_BCC><a>bcc4</a>bcc5<b>bcc6</b></HDR_BCC>"
        "<REPLY_TO><a>rto1</a>rto2<b>rto3</b></REPLY_TO><REPLY_TO><a>rto4</a>rto5<b>rto6</b></REPLY_TO>"
        "</xml>";
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 1, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), body);
    messages.back().MutableDocument()->SetMimeType("text/xml");
    TString kps;
    if (GetIsPrefixed()) {
        messages.back().MutableDocument()->SetKeyPrefix(1);
        kps = "&kps=1";
    }
    IndexMessages(messages, REALTIME, 1);
    TVector<TDocSearchInfo> results;
    TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > > properties;
    QuerySearch("url:\"*\"" + kps, results, &properties);
    if (properties.size() != 1)
        ythrow yexception() << "incorrect results count: " << properties.size() << " != 1";
    TMap<TString, TVector<TString>> expected;
    expected["prop_cc"].push_back("cc1cc2cc3");
    expected["prop_cc"].push_back("cc4cc5cc6");
    expected["prop_bcc"].push_back("bcc1bcc2bcc3bcc4bcc5bcc6");
    expected["prop_to"].push_back("<a>to1</a>to2<b abc=\"123\">to3</b>");
    expected["prop_to"].push_back("<a>to4</a>to5<b>to6</b>");
    expected["prop_rto"].push_back("rto1");
    expected["prop_rto"].push_back("rto2");
    expected["prop_rto"].push_back("rto3");
    expected["prop_rto"].push_back("rto4");
    expected["prop_rto"].push_back("rto5");
    expected["prop_rto"].push_back("rto6");
    Check(*properties[0], expected);
    return true;
}
bool InitConfig() override {
    (*ConfigDiff)["Indexer.Common.ZonesToProperties.hdr_replyto"] = "prop_rto";
    (*ConfigDiff)["Indexer.Common.ZonesToProperties.hdr_cc"] = "prop_cc,JOIN_PARAGRAPHS";
    (*ConfigDiff)["Indexer.Common.ZonesToProperties.hdr_bcc"] = "prop_bcc,JOIN_SPANS,JOIN_PARAGRAPHS";
    (*ConfigDiff)["Indexer.Common.ZonesToProperties.attachname"] = "prop_to,NO_STRIP_TAGS,JOIN_PARAGRAPHS";
    return true;
}
};
