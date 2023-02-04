#include <saas/rtyserver_test/testerlib/rtyserver_test.h>

START_TEST_DEFINE(TestFindLongText)
    bool Run() override {
        TString s200(200, 's');
        TString longTextMem =
            "This is story about new search server When it was born it was named RTYServer Its parents were developers in one big software company";
        TString longTextDisk =
            "One girl very likes cakes and cars with very big some hren dazhe ne znayu chto napisat ";

        const unsigned int countLongMessages = 3;
        TVector<NRTYServer::TMessage> messagesTestMem, messagesTestDisk;
        TMap<TString, NRTYServer::TMessage> needMessages;
        INFO_LOG << "Testing memory index" << Endl;

        GenerateInput(messagesTestMem, countLongMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), longTextMem + '\n' + s200);
        GenerateInput(messagesTestDisk, countLongMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), longTextDisk + '\n' + s200);

        IndexMessages(messagesTestMem, REALTIME, 1);
        IndexMessages(messagesTestDisk, DISK, 1);

        if (!CheckExistsByText(longTextMem, true, messagesTestMem)) {
            DEBUG_LOG << "CheckExistsByText on messagesTestMem before reopen was failed(((" << Endl;
            ythrow yexception() << "CheckExistsByText on messagesTestMem before reopen was failed(((";
        }

        if (!CheckExistsByText(s200, true, messagesTestMem)) {
            DEBUG_LOG << "CheckExistsByText on messagesTestMem with s200 before reopen was failed(((" << Endl;
            ythrow yexception() << "CheckExistsByText on messagesTestMem with s200 before reopen was failed(((";
        }

        ReopenIndexers();

        if (!CheckExistsByText(longTextMem, true, messagesTestMem)) {
            DEBUG_LOG << "CheckExistsByText on messagesTestMem after reopen was failed(((" << Endl;
            ythrow yexception() << "CheckExistsByText on messagesTestMem after reopen was failed(((";
        }

        if (!CheckExistsByText(longTextDisk, true, messagesTestDisk)) {
            DEBUG_LOG << "CheckExistsByText on messagesTestDisk after reopen was failed(((" << Endl;
            ythrow yexception() << "CheckExistsByText on messagesTestDisk after reopen was failed(((";
        }
        return true;
    }
};

START_TEST_DEFINE(TestSearchBigDocnum)
bool Prepare() override {
    if (GetIsPrefixed())
        PrepareData("prefix/bigdocs/coredumps_index");
    return true;
}
void CheckProps(const TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > >& props){
    for (size_t i = 0; i < props.size(); ++i){
        for (THashMultiMap<TString, TString>::const_iterator j = props[i]->begin(); j != props[i]->end(); ++j){
            if (j->first == "coretext" && j->second.find("signal") == NPOS)
                ythrow yexception() << "something wrong with property 'coretext': " << j->second;
            if (j->first == "core_html" && j->second.find("class=") == NPOS)
                ythrow yexception() << "something wrong with property 'core_html', len: " << j->second.length();
        }
    }
}
bool CheckResults(TString request, size_t numdoc){
    TVector<TDocSearchInfo> results;
    TVector<TSimpleSharedPtr<THashMultiMap<TString, TString> > > props;
    TString numStr = ToString(numdoc);
    QuerySearch(request + "&numdoc=" + numStr, results, &props);
    CHECK_TEST_FAILED(results.size() != numdoc, "bad results size: " + numStr + " !=" + ToString<int>(results.size()));
    CHECK_TEST_FAILED(props.size() != numdoc, "bad props size: " + numStr + " !=" + ToString<int>(props.size()));
    CheckProps(props);
    return true;
}
bool Run() override {
    if (!GetIsPrefixed())
        return true;

    TString request = "url:\"*\"&kps=6&how=usq&gta=usq&gta=_AllDocInfos&timeout=100000000";

    CHECK_TEST_FAILED(!CheckResults(request, 10), "error with 10 docs");
    CHECK_TEST_FAILED(!CheckResults(request, 50), "error with 50 docs");
    CHECK_TEST_FAILED(!CheckResults(request, 100), "error with 100 docs");
    CHECK_TEST_FAILED(!CheckResults(request, 150), "error with 150 docs");
    return true;
}
bool InitConfig() override {
    (*ConfigDiff)["Indexer.Common.UseSlowUpdate"] = false;
    (*ConfigDiff)["Components"] = "INDEX,DDK";
    (*ConfigDiff)["ComponentsConfig.FULLARC.ActiveLayers"] = "base";
    return true;
}
};
