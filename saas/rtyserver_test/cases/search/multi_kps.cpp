#include <saas/rtyserver_test/testerlib/rtyserver_test.h>

START_TEST_DEFINE(TestMultiKeyPrefixInOneTouch)
bool Run() override {
    const int CountMessages = 2;
    const bool isPrefixed = GetIsPrefixed();
    TVector<NRTYServer::TMessage> messagesToMem;
    GenerateInput(messagesToMem, CountMessages, NRTYServer::TMessage::ADD_DOCUMENT, isPrefixed);
    messagesToMem[0].MutableDocument()->SetBody("abc dee ooo");
    messagesToMem[1].MutableDocument()->SetBody("uuu sss ooo");
    IndexMessages(messagesToMem, REALTIME, 1);

    TString kpss = "";
    for (ui32 i = 0; i < messagesToMem.size(); ++i) {
        if (i != 0)
            kpss += ",";
        kpss += ToString(messagesToMem[i].GetDocument().GetKeyPrefix());
    }

    TVector<TDocSearchInfo> results;
    QuerySearch("%28abc+%26%26+ooo%29+%7C+%28sss+%26%26+ooo%29&kps=" + kpss, results);
    if (results.size() != 2)
        TEST_FAILED("TestMultiKeyPrefix failed");

    QuerySearch("sss&kps=" + kpss, results);
    if (results.size() != 1)
        TEST_FAILED("TestMultiKeyPrefix failed");

    QuerySearch("dee&kps=" + kpss, results);
    if (results.size() != 1)
        TEST_FAILED("TestMultiKeyPrefix failed");

    QuerySearch("ooo&kps=" + kpss, results);
    if (results.size() != 2)
        TEST_FAILED("TestMultiKeyPrefix failed");
    return true;
}
};

START_TEST_DEFINE(TestMultiKeyPrefix)
    bool Run() override {
        const int CountMessages = 100;
        const bool isPrefixed = GetIsPrefixed();
        TVector<NRTYServer::TMessage> messagesToMem;
        GenerateInput(messagesToMem, CountMessages, NRTYServer::TMessage::ADD_DOCUMENT, isPrefixed);
        GenerateInput(messagesToMem, 1, NRTYServer::TMessage::ADD_DOCUMENT, isPrefixed, TAttrMap(), "как бы изменить настройки");
        IndexMessages(messagesToMem, REALTIME, 1);

        TString kpss = "";
        for (ui32 i = 0; i < messagesToMem.size(); ++i) {
            if (i != 0)
                kpss += ",";
            kpss += ToString(messagesToMem[i].GetDocument().GetKeyPrefix());
        }

        TVector<TDocSearchInfo> results;
        QuerySearch("как изменить настройки&kps=" + ToString(messagesToMem.back().GetDocument().GetKeyPrefix()), results);
        if (results.size() != 1)
            TEST_FAILED("TestMultiKeyPrefix failed");

        QuerySearch("test:test@test&kps="+
            ToString(messagesToMem[0].MutableDocument()->GetKeyPrefix())+","+
            ToString(messagesToMem[1].MutableDocument()->GetKeyPrefix())
            , results);

        if (results.size() != (isPrefixed ? 2 : 100))
            TEST_FAILED("TestMultiKeyPrefix failed");

        QuerySearch("body&kps="+
            ToString(messagesToMem[0].MutableDocument()->GetKeyPrefix())+","+
            ToString(messagesToMem[1].MutableDocument()->GetKeyPrefix())
            , results);

        if (results.size() != (isPrefixed ? 2 : 100))
            TEST_FAILED("TestMultiKeyPrefix failed");

        QuerySearch("body&kps=" + ToString(messagesToMem[1].MutableDocument()->GetKeyPrefix()), results);

        if (results.size() != (isPrefixed ? 1 : 100))
            TEST_FAILED("TestMultiKeyPrefix failed");

        QuerySearch("body&kps="+
            ToString(messagesToMem[0].MutableDocument()->GetKeyPrefix())+","+
            ToString(messagesToMem[0].MutableDocument()->GetKeyPrefix() + GetShardsNumber())
            , results);

        if (results.size() != (isPrefixed ? 2 : 100))
            TEST_FAILED("TestMultiKeyPrefix failed");

        ReopenIndexers();

        QuerySearch("body&kps="+
            ToString(messagesToMem[0].MutableDocument()->GetKeyPrefix())+","+
            ToString(messagesToMem[1].MutableDocument()->GetKeyPrefix())
            , results);

        if (results.size() != (isPrefixed ? 2 : 100))
            TEST_FAILED("TestMultiKeyPrefix failed");

        QuerySearch("body&kps="+
            ToString(messagesToMem[0].MutableDocument()->GetKeyPrefix())+","+
            ToString(messagesToMem[0].MutableDocument()->GetKeyPrefix() + GetShardsNumber())
            , results);

        if (results.size() != (isPrefixed ? 2 : 100))
            TEST_FAILED("TestMultiKeyPrefix failed");

        if (!isPrefixed) {
            QuerySearch("body", results);
            if (results.size() != 100)
                TEST_FAILED("TestMultiKeyPrefix failed");

        } else {
            QuerySearch("body keyprefix:"+kpss, results);
            if (results.size() != 100)
                TEST_FAILED("TestMultiKeyPrefix failed");
            QuerySearch("body&kps="+kpss, results);
            if (results.size() != 100)
                TEST_FAILED("TestMultiKeyPrefix failed");
        }
        return true;
    }
};

START_TEST_DEFINE(TestMultiKeyPrefixZonesAndAttrs)
    bool Run() override {
        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, 10, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), "<xml><aaa zone_attr=\"abc\">a b c</aaa><bbb>aa bb cc</bbb></xml>");
        for (ui32 docModif = 0; docModif < messages.size(); ++docModif) {
            messages[docModif].MutableDocument()->SetMimeType("text/xml");
        }
        IndexMessages(messages, REALTIME, 1);
        ReopenIndexers();
        TVector<TDocSearchInfo> results;
        QuerySearch("aaa:(a b c)" + GetAllKps(messages), results);
        if (results.size() != messages.size())
            ythrow yexception() << "inctorrect results count with zones " << results.size() << " != " << messages.size();
        QuerySearch("aaa:(zone_attr:\"abc\")" + GetAllKps(messages), results);
        if (results.size() != messages.size())
            ythrow yexception() << "inctorrect results count with attrs " << results.size() << " != " << messages.size();
        return true;
}
bool InitConfig() override {
    (*ConfigDiff)["Searcher.FiltrationModel"] = "SIMPLE";
    (*ConfigDiff)["Indexer.Common.XmlParserConfigFile"] = "";
    return true;
}
};
