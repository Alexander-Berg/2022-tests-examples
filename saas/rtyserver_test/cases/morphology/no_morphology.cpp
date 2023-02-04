#include <saas/rtyserver_test/testerlib/rtyserver_test.h>

START_TEST_DEFINE(TestNoMorphology)
    bool Run() override {
        int keyPrefix = 0;
        if (GetIsPrefixed())
            keyPrefix = 1;
        TVector<NRTYServer::TMessage> messagesForMemory, messagesForDisk;

        TVector<TString> texts;
        texts.push_back("люди");
        texts.push_back("красная");
        texts.push_back("красное");
        texts.push_back("краснеть");
        texts.push_back("красный");
        texts.push_back("34dnews");
        texts.push_back("784");
        texts.push_back("санкт-петербурга-нью-йорка-баден-баден");
        texts.push_back("reis");
        texts.push_back("rei");
        const bool isPrefixed = GetIsPrefixed();
        for(TVector<TString>::const_iterator i = texts.begin(), e = texts.end(); i != e; ++i) {
            GenerateInput(messagesForMemory, 1, NRTYServer::TMessage::ADD_DOCUMENT, isPrefixed, TAttrMap(), *i);
            if (isPrefixed)
                messagesForMemory.back().MutableDocument()->SetKeyPrefix(keyPrefix);
            GenerateInput(messagesForDisk, 1, NRTYServer::TMessage::ADD_DOCUMENT, isPrefixed, TAttrMap(), *i);
            if (isPrefixed)
                messagesForDisk.back().MutableDocument()->SetKeyPrefix(keyPrefix);
        }

        IndexMessages(messagesForDisk, DISK, 1);
        IndexMessages(messagesForMemory, REALTIME, 1);
        Sleep(TDuration::Seconds(2));

        TVector<TDocSearchInfo> results;

        QuerySearch("reis&kps=" + ToString(keyPrefix), results);
        if (results.size() != 1)
            ythrow yexception() << "TestNoMorphology failed case a";

        QuerySearch("красная&kps="+ToString(keyPrefix), results);
        if (results.size() != 1)
            ythrow yexception() << "TestNoMorphology failed case A";

        QuerySearch("красное&kps="+ToString(keyPrefix), results);
        if (results.size() != 1)
            ythrow yexception() << "TestNoMorphology failed case B";

        QuerySearch("красный&kps="+ToString(keyPrefix), results);
        if (results.size() != 1)
            ythrow yexception() << "TestNoMorphology failed case C";

        QuerySearch("красн*&kps="+ToString(keyPrefix), results);
        if (results.size() != 4)
            ythrow yexception() << "TestNoMorphology failed case C*";

        QuerySearch("люд*&kps="+ToString(keyPrefix), results);
        if (results.size() != 1)
            ythrow yexception() << "TestNoMorphology failed case D*";

        QuerySearch("люди&kps="+ToString(keyPrefix), results);
        if (results.size() != 1)
            ythrow yexception() << "TestNoMorphology failed case D";

        QuerySearch("человек&kps="+ToString(keyPrefix), results);
        if (results.size() != 0)
            ythrow yexception() << "TestNoMorphology failed case E";

        QuerySearch("чел*&kps="+ToString(keyPrefix), results);
        if (results.size() != 0)
            ythrow yexception() << "TestNoMorphology failed case E*";

        QuerySearch("34d*&kps="+ToString(keyPrefix), results);
        if (results.size() != 1)
            ythrow yexception() << "TestNoMorphology failed case M*";
    /*
        QuerySearch("7*&kps="+ToString(keyPrefix), results);
        if (results.size() != 1)
            ythrow yexception() << "TestNoMorphology failed case M*";
    */
        QuerySearch("санкт-петербурга-нью-йорк-баден-баде*&kps="+ToString(keyPrefix), results);
        if (results.size() != 0)
            ythrow yexception() << "TestNoMorphology failed case spb-new-baden";

        QuerySearch("санкт-петербурга-нью-йорка-баден-баде*&kps="+ToString(keyPrefix), results);
        if (results.size() != 1)
            ythrow yexception() << "TestNoMorphology failed case spb-new-baden*";

        ReopenIndexers();

        QuerySearch("красная&kps="+ToString(keyPrefix), results);
        if (results.size() != 2)
            ythrow yexception() << "TestNoMorphology failed case A";

        QuerySearch("красное&kps="+ToString(keyPrefix), results);
        if (results.size() != 2)
            ythrow yexception() << "TestNoMorphology failed case B";

        QuerySearch("красный&kps="+ToString(keyPrefix), results);
        if (results.size() != 2)
            ythrow yexception() << "TestNoMorphology failed case C";

        QuerySearch("красн*&kps="+ToString(keyPrefix), results);
        if (results.size() != 8)
            ythrow yexception() << "TestNoMorphology failed case C*";

        QuerySearch("люди&kps="+ToString(keyPrefix), results);
        if (results.size() != 2)
            ythrow yexception() << "TestNoMorphology failed case D";

        QuerySearch("люд*&kps="+ToString(keyPrefix), results);
        if (results.size() != 2)
            ythrow yexception() << "TestNoMorphology failed case D*";

        QuerySearch("человек&kps="+ToString(keyPrefix), results);
        if (results.size() != 0)
            ythrow yexception() << "TestNoMorphology failed case E";

        QuerySearch("чел*&kps="+ToString(keyPrefix), results);
        if (results.size() != 0)
            ythrow yexception() << "TestNoMorphology failed case E*";

        QuerySearch("34d*&kps="+ToString(keyPrefix), results);
        if (results.size() != 2)
            ythrow yexception() << "TestNoMorphology failed case M*";
    /*
        QuerySearch("7*&kps="+ToString(keyPrefix), results);
        if (results.size() != 2)
            ythrow yexception() << "TestNoMorphology failed case M*";
    */
        QuerySearch("санкт-петербурга-нью-йорк-баден-баде*&kps="+ToString(keyPrefix), results);
        if (results.size() != 0)
            ythrow yexception() << "TestNoMorphology failed case spb-new-baden*";

        QuerySearch("санкт-петербурга-нью-йорка-баден-баде*&kps="+ToString(keyPrefix), results);
        if (results.size() != 2)
            ythrow yexception() << "TestNoMorphology failed case spb-new-baden*";
        return true;
    }
public:
    bool InitConfig() override {
        SetMorphologyParams("auto", abTRUE);
        return true;
    }
};
