#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/rtyserver_test/util/factors_parsers.h>

START_TEST_DEFINE(TestWILDCARD)
    bool Run() override {
        const int CountMessages = 2;
        const bool isPrefixed = GetIsPrefixed();
        TVector<NRTYServer::TMessage> messagesForMemory, messagesForDisk;
        GenerateInput(messagesForDisk, CountMessages, NRTYServer::TMessage::MODIFY_DOCUMENT, isPrefixed, TAttrMap(), "май хэлло ворлд сырник санкт-петербурга-нью-йорка солярис");
        GenerateInput(messagesForMemory, CountMessages, NRTYServer::TMessage::MODIFY_DOCUMENT, isPrefixed, TAttrMap(), "май хэлло ворлд сырник санкт-петербурга-нью-йорка солярис");

        IndexMessages(messagesForDisk, DISK, 1);
        IndexMessages(messagesForMemory, REALTIME, 1);
        Sleep(TDuration::Seconds(2));

        int count_zeros = 0;
        for (int message = 0; message < CountMessages; message++) {
            count_zeros += CheckMessage("сол*", messagesForMemory[message].GetDocument().GetKeyPrefix(), isPrefixed);
            count_zeros += CheckMessage("ворлд+url:\"*\"", messagesForMemory[message].GetDocument().GetKeyPrefix(), isPrefixed);
            count_zeros += CheckMessage("хэлло+url:\"*\"", messagesForMemory[message].GetDocument().GetKeyPrefix(), isPrefixed);
            count_zeros += CheckMessage("май+url:\"*\"", messagesForMemory[message].GetDocument().GetKeyPrefix(), isPrefixed);
            //        count_zeros += CheckMessage("*ай", messagesForMemory[message].GetDocument().GetKeyPrefix(), isPrefixed);
            count_zeros += CheckMessage("ма*", messagesForMemory[message].GetDocument().GetKeyPrefix(), isPrefixed);
            //        count_zeros += CheckMessage("*элло", messagesForMemory[message].GetDocument().GetKeyPrefix(), isPrefixed);
            count_zeros += CheckMessage("хэлл*", messagesForMemory[message].GetDocument().GetKeyPrefix(), isPrefixed);
            count_zeros += CheckMessage("ворл*", messagesForMemory[message].GetDocument().GetKeyPrefix(), isPrefixed);
            //        count_zeros += CheckMessage("*орлд", messagesForMemory[message].GetDocument().GetKeyPrefix(), isPrefixed);
            count_zeros += 1 - CheckMessage("\"сыру\"*", messagesForMemory[message].GetDocument().GetKeyPrefix(), isPrefixed);

            count_zeros += 1 - CheckMessage("санкт-петербург-нью-йор*", messagesForMemory[message].GetDocument().GetKeyPrefix(), isPrefixed);
        }

        if (count_zeros)
            ythrow yexception() << "Wildcard test failed: " << ToString(count_zeros) << " times. For memory indexer";
        ReopenIndexers();

        count_zeros = 0;
        for (int message = 0; message < CountMessages; message++) {
            count_zeros += CheckMessage("сол*", messagesForMemory[message].GetDocument().GetKeyPrefix(), isPrefixed);
            count_zeros += CheckMessage("ворлд+url:\"*\"", messagesForDisk[message].GetDocument().GetKeyPrefix(), isPrefixed);
            count_zeros += CheckMessage("хэлло+url:\"*\"", messagesForDisk[message].GetDocument().GetKeyPrefix(), isPrefixed);
            count_zeros += CheckMessage("май+url:\"*\"", messagesForDisk[message].GetDocument().GetKeyPrefix(), isPrefixed);
    //        count_zeros += CheckMessage("*ай", messagesForDisk[message].GetDocument().GetKeyPrefix(), isPrefixed);
            count_zeros += CheckMessage("ма*", messagesForDisk[message].GetDocument().GetKeyPrefix(), isPrefixed);
            //count_zeros += CheckMessage("*элло", messagesForDisk[message].GetDocument().GetKeyPrefix(), isPrefixed);
            count_zeros += CheckMessage("хэлл*", messagesForDisk[message].GetDocument().GetKeyPrefix(), isPrefixed);
            count_zeros += CheckMessage("ворл*", messagesForDisk[message].GetDocument().GetKeyPrefix(), isPrefixed);
            //        count_zeros += CheckMessage("*орлд", messagesForDisk[message].GetDocument().GetKeyPrefix(), isPrefixed);

            count_zeros += 1 - CheckMessage("\"сыру\"*", messagesForDisk[message].GetDocument().GetKeyPrefix(), isPrefixed);

            count_zeros += 1 - CheckMessage("санкт-петербург-нью-йор*", messagesForDisk[message].GetDocument().GetKeyPrefix(), isPrefixed);
        }
        if (count_zeros)
            ythrow yexception() << "Wildcard test failed: " << ToString(count_zeros) << " times. For disk indexer";
        return true;
    }
};

START_TEST_DEFINE(TestNUMBERS)
bool Run() override {
    const int CountMessages = 2;
    const bool isPrefixed = GetIsPrefixed();
    TVector<NRTYServer::TMessage> messagesForMemory, messagesForDisk;
    GenerateInput(messagesForDisk, CountMessages, NRTYServer::TMessage::MODIFY_DOCUMENT, isPrefixed, TAttrMap(), "word 0 926 abc163def 34dnews test77");
    GenerateInput(messagesForMemory, CountMessages, NRTYServer::TMessage::MODIFY_DOCUMENT, isPrefixed, TAttrMap(), "word 0 926 abc163def 34dnews test77");

    IndexMessages(messagesForDisk, DISK, 1);
    IndexMessages(messagesForMemory, REALTIME, 1);
    Sleep(TDuration::Seconds(2));

    int count_zeros = 0;
    for (int message = 0; message < CountMessages; message++) {
        count_zeros += CheckMessage("0", messagesForMemory[message].GetDocument().GetKeyPrefix(), isPrefixed);
        count_zeros += CheckMessage("0*", messagesForMemory[message].GetDocument().GetKeyPrefix(), isPrefixed);
        count_zeros += 1 - CheckMessage("92", messagesForMemory[message].GetDocument().GetKeyPrefix(), isPrefixed);
        count_zeros += CheckMessage("92*", messagesForMemory[message].GetDocument().GetKeyPrefix(), isPrefixed);
        count_zeros += CheckMessage("34", messagesForMemory[message].GetDocument().GetKeyPrefix(), isPrefixed);
        count_zeros += CheckMessage("163", messagesForMemory[message].GetDocument().GetKeyPrefix(), isPrefixed);
        count_zeros += 1 - CheckMessage("789", messagesForMemory[message].GetDocument().GetKeyPrefix(), isPrefixed);
/*
        count_zeros += CheckMessage("*c163d*", messagesForMemory[message].GetDocument().GetKeyPrefix(), isPrefixed);
        count_zeros += CheckMessage("*63d*", messagesForMemory[message].GetDocument().GetKeyPrefix(), isPrefixed);
        count_zeros += CheckMessage("*c16*", messagesForMemory[message].GetDocument().GetKeyPrefix(), isPrefixed);
*/
        count_zeros += CheckMessage("abc16*", messagesForMemory[message].GetDocument().GetKeyPrefix(), isPrefixed);
        count_zeros += CheckMessage("34d*", messagesForMemory[message].GetDocument().GetKeyPrefix(), isPrefixed);
        count_zeros += CheckMessage("word test7*", messagesForMemory[message].GetDocument().GetKeyPrefix(), isPrefixed);
        count_zeros += 1 - CheckMessage("word 926.0*", messagesForMemory[message].GetDocument().GetKeyPrefix(), isPrefixed);
    }

    if (count_zeros)
        ythrow yexception() << "Wildcard test failed: " << ToString(count_zeros) << " times. For memory indexer";
    ReopenIndexers();

    count_zeros = 0;
    for (int message = 0; message < CountMessages; message++) {
        count_zeros += CheckMessage("0", messagesForDisk[message].GetDocument().GetKeyPrefix(), isPrefixed);
        count_zeros += CheckMessage("0*", messagesForDisk[message].GetDocument().GetKeyPrefix(), isPrefixed);
        count_zeros += 1 - CheckMessage("92", messagesForDisk[message].GetDocument().GetKeyPrefix(), isPrefixed);
        count_zeros += CheckMessage("92*", messagesForDisk[message].GetDocument().GetKeyPrefix(), isPrefixed);
        count_zeros += CheckMessage("34", messagesForDisk[message].GetDocument().GetKeyPrefix(), isPrefixed);
        count_zeros += CheckMessage("163", messagesForDisk[message].GetDocument().GetKeyPrefix(), isPrefixed);
        count_zeros += 1 - CheckMessage("789", messagesForDisk[message].GetDocument().GetKeyPrefix(), isPrefixed);
/*
        count_zeros += CheckMessage("*c163d*", messagesForDisk[message].GetDocument().GetKeyPrefix(), isPrefixed);
        count_zeros += CheckMessage("*63d*", messagesForDisk[message].GetDocument().GetKeyPrefix(), isPrefixed);
        count_zeros += CheckMessage("*c16*", messagesForDisk[message].GetDocument().GetKeyPrefix(), isPrefixed);
*/
        count_zeros += CheckMessage("abc16*", messagesForDisk[message].GetDocument().GetKeyPrefix(), isPrefixed);
        count_zeros += CheckMessage("34d*", messagesForDisk[message].GetDocument().GetKeyPrefix(), isPrefixed);
        count_zeros += CheckMessage("word test7*", messagesForDisk[message].GetDocument().GetKeyPrefix(), isPrefixed);
        count_zeros += 1 - CheckMessage("word 926.0*", messagesForMemory[message].GetDocument().GetKeyPrefix(), isPrefixed);
    }
    if (count_zeros)
        ythrow yexception() << "Wildcard test failed: " << ToString(count_zeros) << " times. For disk indexer";
    return true;
}
};

START_TEST_DEFINE(TestWildcardFactors)
    TVector<TDocSearchInfo> Results;
    TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > > ResultProps;

    double GetFactor(const TString& factor, const THashMap<TString, double>& factors) {
        THashMap<TString, double>::const_iterator iFactor = factors.find(factor);
        if (iFactor == factors.end())
            ythrow yexception() << "there is no " << factor << " in result";
        return iFactor->second;
    }
    void CheckFactor(const ui32 num, const TString& factorName, bool isZero, TString caseName) {
        if (Results.size() < num + 1) {
            ythrow yexception() << "incorrect results count, case " << caseName;
        }
        THashMap<TString, double> factors = TRTYFactorsParser::GetJsonFactorsValues(ResultProps)[num];
        double f = GetFactor(factorName, factors);
        if (isZero ? (f > 0.01) : (f < 0.1)) {
            ythrow yexception() << "incorrect " << factorName << " value: " << f << ", case " << caseName;
        }
    }

    bool Run() override {
        const int CountMessages = 2;
        const bool isPrefixed = GetIsPrefixed();
        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, CountMessages, NRTYServer::TMessage::MODIFY_DOCUMENT, isPrefixed, TAttrMap(), "популярное");
        messages[1].MutableDocument()->SetBody("попа Länder попробовать");
        TString kps = GetAllKps(messages);
        IndexMessages(messages, REALTIME, 1);

        ReopenIndexers();

        //C0010000000V3 == 1*TxtBm25Ex
        QuerySearch("Попу*&relev=formula%3DC0010000000V3&dbgrlv=da&fsgta=_JsonFactors" + kps, Results, &ResultProps);
        if (Results.size() != 2)
            ythrow yexception() << "incorrect results count: expected 2, got " << Results.size();
        if (Results[0].GetUrl() != messages[0].GetDocument().GetUrl())
            ythrow yexception() << "incorrect order";

        CheckFactor(0, "TxtBm25Ex", false, "exact prefix");
        CheckFactor(1, "TxtBm25Ex", true, "not-exact prefix");

        QuerySearch("Популярное*&relev=formula%3DC0010000000V3&dbgrlv=da&fsgta=_JsonFactors" + kps, Results, &ResultProps);
        CheckFactor(0, "TxtBm25Ex", false, "exact form");

        QuerySearch("Популярно*&relev=formula%3DC0010000000V3&dbgrlv=da&fsgta=_JsonFactors" + kps, Results, &ResultProps);
        //yeah, this is epic bug with forms
        CheckFactor(0, "TxtBm25Ex", true, "buggy form-prefix case");

        QuerySearch("Lä*&relev=formula%3DC0010000000V3&dbgrlv=da&fsgta=_JsonFactors" + kps, Results, &ResultProps);
        CheckFactor(0, "TxtBm25Ex", false, "umlaut prefix");

        return true;
    }

    bool InitConfig() override {
        (*ConfigDiff)["Searcher.FactorsInfo"] = GetResourcesDirectory() + "/factors/text_factors_nomx.cfg";
        return true;
    }
};
