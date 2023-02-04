#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <library/cpp/string_utils/quote/quote.h>
#include <library/cpp/charset/wide.h>
#include <saas/rtyserver_test/testerlib/standart_generator.h>

START_TEST_DEFINE(TestSearchUTF8Attr)
protected:
    void Test(TIndexerType indexer) {
        const unsigned countMessages = 1;
        TVector<NRTYServer::TMessage> messages;
        TString attrValue("Раз Два Три");
        TString attrValueUTF8 = WideToUTF8(UTF8ToWide(attrValue));
        TAttrMap attrs(countMessages);
        for (unsigned i = 0; i < countMessages; ++i) {
            attrs[i]["tag"] = attrValueUTF8;
        }
        const TString indexerType(indexer == DISK ? "disk" : "memory");
        TString textForSearch = indexerType + TString(" is test ") + WideToUTF8(u"ААА БББ");
        GenerateInput(messages, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), textForSearch, true, attrs);
        ui64 commonKps = messages.front().GetDocument().GetKeyPrefix();
        for (unsigned i = 0; i < countMessages; ++i) {
            messages[i].MutableDocument()->SetKeyPrefix(commonKps);
            NRTYServer::TMessage::TDocument::TProperty* prop = messages[i].MutableDocument()->AddDocumentProperties();
            prop->set_name("textProp");
            prop->set_value(attrValueUTF8.data());
        }
        IndexMessages(messages, indexer, 1);
        if (indexer == DISK)
            ReopenIndexers();
        else
            Sleep(TDuration::Seconds(2));
        TVector<TDocSearchInfo> results;
        QuerySearch(indexerType + "&kps=" + ToString(commonKps), results);
        if (results.size() != countMessages)
            ythrow yexception() << "polnaya hren'";
        for (unsigned i = 0; i < countMessages; ++i) {
            QuerySearch(indexerType + "+tag:\"" + attrValue + "\"&kps=" + ToString(messages[i].GetDocument().GetKeyPrefix()), results);
            if (results.size() != ((i < attrs.size()) ? attrs.size() : 0))
                ythrow yexception() << "cannot find " + attrValue;
        }
        TString res;
        if (200 != Controller->ProcessQuery("/?text=" + indexerType + "&xml=da&kps=" + ToString(messages.front().GetDocument().GetKeyPrefix()), &res))
            ythrow yexception() << "xml fail";
        if (strstr(res.c_str(), "error"))
            ythrow yexception() << "xml has error";
        if (200 != Controller->ProcessQuery("/?text=" + indexerType + "&ms=proto&kps=" + ToString(messages.front().GetDocument().GetKeyPrefix()), &res))
            ythrow yexception() << "protobuf fail";
        TString textQuery = "\"is test " + WideToChar(u"АА", CODES_YANDEX) + "*\" tag:\"" + attrValueUTF8 + "\"";
        Quote(textQuery);
        textQuery += "&kps=" + ToString(commonKps);
        DeleteQueryResult(textQuery, indexer);
        QuerySearch(textQuery, results);
        if (results.size() != 0)
            ythrow yexception() << "Deleting failed";
    }
public:
    bool Run() override {
        Test(REALTIME);
        Test(DISK);
        return true;
    }
};

START_TEST_DEFINE(TestSearchAttr)
protected:
    void Test(TIndexerType indexer) {
        const unsigned countMessages = 100;
        TVector<NRTYServer::TMessage> messages;
        TAttrMap attrs(countMessages / 2);
        for (unsigned i = 0; i < countMessages / 2; ++i) {
            attrs[i]["tag"] = ToString(i);
            attrs[i]["attachsize"] = i;
            attrs[i]["search_attr_lit_new"] = ToString(i);
        }
        const TString indexerType(indexer == DISK ? "disk" : "memory");
        GenerateInput(messages, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), indexerType, true, attrs);
        for (unsigned i = 0; i < countMessages; ++i) {
            NRTYServer::TMessage::TDocument &doc = *messages[i].MutableDocument();
            doc.SetKeyPrefix(messages.front().GetDocument().GetKeyPrefix());
            NRTYServer::TAttribute* attr = doc.AddSearchAttributes();
            attr->set_name("attachsize_b");
            attr->set_value(ToString(i));
            attr->set_type(NRTYServer::TAttribute::INTEGER_ATTRIBUTE);
            attr = doc.AddSearchAttributes();
            attr->set_name("lang");
            attr->set_value(ToString(100));
            attr->set_type(NRTYServer::TAttribute::INTEGER_ATTRIBUTE);
            attr = doc.AddSearchAttributes();
            attr->set_name("attachsize_b");
            attr->set_value(ToString(3 * countMessages - i));
            attr->set_type(NRTYServer::TAttribute::INTEGER_ATTRIBUTE);
        }
        IndexMessages(messages, indexer, 1);
        if (indexer == DISK)
            ReopenIndexers();
        else
            Sleep(TDuration::Seconds(2));
        TVector<TDocSearchInfo> results;
        QuerySearch(indexerType + "&kps=" + ToString(messages.front().GetDocument().GetKeyPrefix()), results);
        if (results.size() != countMessages)
            ythrow yexception() << "polnaya hren'";

        QuerySearch("lang:100&kps=" + ToString(messages.front().GetDocument().GetKeyPrefix()), results);
        if (results.size() != countMessages)
            ythrow yexception() << "attr lang search fail";

        QuerySearch("attachsize_b:>\"" + ToString(2 * countMessages) + "\"&kps=" + ToString(messages.front().GetDocument().GetKeyPrefix()), results);
        if (results.size() != countMessages)
            ythrow yexception() << "polnaya hren'";

        for (unsigned i = 0; i < countMessages; ++i) {
            QuerySearch(indexerType + "+tag:" + ToString(i) + "&kps=" + ToString(messages[i].GetDocument().GetKeyPrefix()), results);
            if (results.size() != ((i < attrs.size()) ? 1 : 0))
                ythrow yexception() << "cannot find tag:" + ToString(i);
            QuerySearch(indexerType + "+attachsize:" + ToString(i) + "&kps=" + ToString(messages[i].GetDocument().GetKeyPrefix()), results);
            if (results.size() != ((i < attrs.size()) ? 1 : 0))
                ythrow yexception() << "cannot find " + ToString(i);
            QuerySearch(indexerType + "+attachsize_b:\"" + ToString(i) + "\"&kps=" + ToString(messages[i].GetDocument().GetKeyPrefix()), results);
            if (results.size() != 1)
                ythrow yexception() << "cannot find " + ToString(i);
            QuerySearch(indexerType + "+search_attr_lit_new:" + ToString(i) + "&kps=" + ToString(messages[i].GetDocument().GetKeyPrefix()), results);
            if (results.size() != ((i < attrs.size()) ? 1 : 0))
                ythrow yexception() << "cannot find search_attr_lit_new:" + ToString(i);
        }
    }
public:
    bool Run() override {
        Test(REALTIME);
        Test(DISK);
        return true;
    }
};

START_TEST_DEFINE(TestSearchAttrsMixed)
protected:
    void TestSearchAttr(TIndexerType indexer) {
        TVector<NRTYServer::TMessage> messages;
        const unsigned countMessages = 2;
        TAttrMap attrs(countMessages / 2 + 1);
        for (unsigned i = 0; i <= countMessages / 2; ++i) {
            attrs[i]["tag"] = ToString(i);
            attrs[i]["attachsize"] = i;
            attrs[i]["tag"] = WideToUTF8(u"русскийтэг");
        }
        const TString addSuffix(indexer == DISK ? " yandex ru" : " mail com");
        const TString indexerType(indexer == DISK ? "disk" : "memory");
        GenerateInput(messages, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, false, TAttrMap(), indexerType + addSuffix, true, attrs);
        if (GetIsPrefixed())
            for (unsigned i = 0; i < countMessages; ++i)
                messages[i].MutableDocument()->SetKeyPrefix(1);
        IndexMessages(messages, indexer, 1);
        bool isDisk = indexer == DISK;
        if (isDisk)
            ReopenIndexers();
        else
            Sleep(TDuration::Seconds(2));

        TVector<TDocSearchInfo> results;

        QuerySearch("tag:" + WideToChar(u"русскийтэг", CODES_YANDEX) + "&kps=" + ToString(messages.front().GetDocument().GetKeyPrefix()), results);
        if (results.size() != (isDisk ? 4 : 2))
            ythrow yexception() << "TestSearchAttrsMixed failed case R: " << results.size() << " != " << (isDisk ? 4 : 2);

        QuerySearch(indexerType + addSuffix + "&kps=" + ToString(messages.front().GetDocument().GetKeyPrefix()), results);
        if (results.size() != 2)
            ythrow yexception() << "TestSearchAttrsMixed failed case A: " << results.size() << " != " << 2;

        QuerySearch("attachsize:0&kps=" + ToString(messages.front().GetDocument().GetKeyPrefix()), results);
        if (results.size() != (isDisk ? 2 : 1))
            ythrow yexception() << "TestSearchAttrsMixed failed case B: " << results.size() << " != " << (isDisk ? 2 : 1);

        QuerySearch("attachsize:1&kps=" + ToString(messages.front().GetDocument().GetKeyPrefix()), results);
        if (results.size() != (isDisk ? 2 : 1))
            ythrow yexception() << "TestSearchAttrsMixed failed case C: " << results.size() << " != " << (isDisk ? 2 : 1);

        QuerySearch(indexerType + " attachsize:0&kps=" + ToString(messages.front().GetDocument().GetKeyPrefix()), results);
        if (results.size() != 1)
            ythrow yexception() << "TestSearchAttrsMixed failed case D: " << results.size() << " != " << 1;

        QuerySearch("\"" + indexerType + addSuffix + "\"+attachsize:0&kps=" + ToString(messages.front().GetDocument().GetKeyPrefix()), results);
        if (results.size() != 1)
            ythrow yexception() << "TestSearchAttrsMixed failed case E: " << results.size() << " != " << 1;

        QuerySearch("%28" + indexerType + addSuffix + "%29+%26%26+%28attachsize:0%29&kps=" + ToString(messages.front().GetDocument().GetKeyPrefix()), results);
        if (results.size() != 1)
            ythrow yexception() << "TestSearchAttrsMixed failed case G: " << results.size() << " != " << 1;

        QuerySearch("%28" + addSuffix + "%29+%26%26+%28attachsize:0%29&kps=" + ToString(messages.front().GetDocument().GetKeyPrefix()), results);
        if (results.size() != 1)
            ythrow yexception() << "TestSearchAttrsMixed failed case G: " << results.size() << " != " << 1;

        QuerySearch(addSuffix + " attachsize:0&kps=" + ToString(messages.front().GetDocument().GetKeyPrefix()), results);
        if (results.size() != 1)
            ythrow yexception() << "TestSearchAttrsMixed failed case J: " << results.size() << " != " << 1;
    }
public:
    bool Run() override {
        TestSearchAttr(REALTIME);
        TestSearchAttr(DISK);
        return true;
    }
};

START_TEST_DEFINE(TestSearchAttrMultiTokens)
protected:
    void Test(TIndexerType indexer) {
        const unsigned countMessages = 1;
        TVector<NRTYServer::TMessage> messages;
        TAttrMap attrs(countMessages);
        for (unsigned i = 0; i < countMessages; ++i) {
            attrs[i]["tag"] = TString("abC.sdFsd/1231");
        }
        const TString indexerType(indexer == DISK ? "<xml>disk</xml>" : "<xml>memory</xml>");
        GenerateInput(messages, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), indexerType, true, attrs);
        messages.back().MutableDocument()->SetUrl("bBb.feDdd/234223");
        messages.back().MutableDocument()->SetMimeType("text/xml");
        IndexMessages(messages, indexer, 1);
        if (indexer == DISK)
            ReopenIndexers();
        else
            Sleep(TDuration::Seconds(2));
        TVector<TDocSearchInfo> results;
        QuerySearch("url:\"bBb.feDdd/234223\" tag:\"abC.sdFsd/1231\"&kps=" + ToString(messages.front().GetDocument().GetKeyPrefix()), results);
        if (results.size() != 1)
            ythrow yexception() << "polnaya hren'";
    }
public:
    bool Run() override {
        Test(REALTIME);
        Test(DISK);
        return true;
    }
};

START_TEST_DEFINE(TestSearchAttrNameCamelCase)
protected:
    bool Test(TIndexerType indexer) {

        TString value = (indexer == DISK ? "disk" : "memory");

        const unsigned countMessages = 1;
        TVector<NRTYServer::TMessage> messages;
        TAttrMap attrs(countMessages);
        for (unsigned i = 0; i < countMessages; ++i) {
            attrs[i]["s_case_normal"] = value;
            attrs[i]["s_caseCamel"] = value;
        }
        const TString indexerType("<xml>" + value + "</xml>");
        GenerateInput(messages, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), indexerType, true, attrs);
        messages.back().MutableDocument()->SetUrl(value);
        messages.back().MutableDocument()->SetMimeType("text/xml");
        IndexMessages(messages, indexer, 1);
        if (indexer == DISK)
            ReopenIndexers();
        else
            Sleep(TDuration::Seconds(2));
        TVector<TDocSearchInfo> results;
        QuerySearch("s_case_normal:\"" + value + "\"&kps=" + ToString(messages.front().GetDocument().GetKeyPrefix()), results);
        CHECK_TEST_EQ(results.size(), 1);
        QuerySearch("s_casecamel:\"" + value + "\"&kps=" + ToString(messages.front().GetDocument().GetKeyPrefix()), results);
        CHECK_TEST_EQ(results.size(), 1);
        return true;
    }
public:
    bool Run() override {
        CHECK_TEST_TRUE(Test(REALTIME));
        CHECK_TEST_TRUE(Test(DISK));
        return true;
    }
};

START_TEST_DEFINE(TestSearchAttrFreaks)
protected:
    void Test(TIndexerType indexer, const TString& name, const TString& value) {
        const unsigned countMessages = 1;
        TVector<NRTYServer::TMessage> messages;
        TAttrMap attrs(countMessages);
        for (unsigned i = 0; i < attrs.size(); ++i) {
            attrs[i][name] = value;
        }
        const TString indexerType(indexer == DISK ? "disk" : "memory");
        GenerateInput(messages, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), indexerType, true, attrs);
        for (unsigned i = 0; i < countMessages; ++i) {
            NRTYServer::TMessage::TDocument &doc = *messages[i].MutableDocument();
            doc.SetKeyPrefix(messages.front().GetDocument().GetKeyPrefix());
        }
        MUST_BE_BROKEN(
            IndexMessages(messages, indexer, 1)
        );
    }
public:
    bool Run() override {
        Test(DISK, "@7f07002", "0");
        Test(DISK, "android:live_wallpaper", "0");
        Test(DISK, "s_touchscreen_multitouch_distinct", "0");
        Test(REALTIME, "@7f07002", "0");
        Test(REALTIME, "android:live_wallpaper", "0");
        Test(REALTIME, "s_touchscreen_multitouch_distinct", "0");
        return true;
    }
};

