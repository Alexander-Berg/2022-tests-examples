#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/library/daemon_base/controller_actions/restart.h>
#include <saas/rtyserver/controller/controller_actions/clear_index_action.h>

START_TEST_DEFINE(TestMODIFY_ONE_DOCUMENT)
private:
    void Test(TIndexerType indexer) {
        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, 1, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
        ui32 maxDocuments = GetMaxDocuments() * 3;
        for (size_t i = 1; i < maxDocuments;++i) {
            messages.push_back(messages.front());
            if (SendIndexReply)
                messages.back().SetMessageId(IMessageGenerator::CreateMessageId());
        }
        IndexMessages(messages, indexer, 1);
        if (indexer == DISK)
            ReopenIndexers();
        CheckSearchResults(messages);
    }
public:
    bool Run() override {
        Test(REALTIME);
        Test(DISK);
        return true;
    }

    bool InitConfig() override {
        SetIndexerParams(REALTIME, 200, 1);
        SetIndexerParams(DISK, 200, 4);
        return true;
    }
};

START_TEST_DEFINE(TestMODIFY_DEL_MIX_ONE_DOCUMENT)
private:
    void Test(TIndexerType indexer) {
        TVector<NRTYServer::TMessage> messagesMod, messages;
        GenerateInput(messagesMod, 1, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
        ui32 maxDocuments = GetMaxDocuments() * 3;
        for (size_t i = 1; i < maxDocuments; ++i) {
            messages.push_back(messagesMod.front());
            if (SendIndexReply)
                messages.back().SetMessageId(IMessageGenerator::CreateMessageId());
            messages.push_back(BuildDeleteMessage(messagesMod.front()));
            if (SendIndexReply)
                messages.back().SetMessageId(IMessageGenerator::CreateMessageId());
        }
        IndexMessages(messages, indexer, 1);
        if (indexer == DISK)
            ReopenIndexers();
    }
public:
    bool Run() override {
        Test(REALTIME);
        Test(DISK);
        return true;
    }

    bool InitConfig() override {
        SetIndexerParams(REALTIME, 200, 1);
        SetIndexerParams(DISK, 200, 4);
        return true;
    }
};

START_TEST_DEFINE(TestMODIFY_ONE_DOCUMENT_REJECT_UNCHANGED)
public:
    bool DoRun(bool checkSerachResults) {
        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, 1, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());

        auto serverInfo = Controller->GetServerInfo();
        NJson::TJsonValue* info = &(*serverInfo)[0];
        auto indexersJson = (*info)["indexes"];
        const TString& infoStart = indexersJson.GetStringRobust();
        DEBUG_LOG << "ServerInfo at start: " << infoStart << Endl;

        TVector<NRTYServer::TReply> replies = IndexMessages(messages, DISK, 1);
        CHECK_TEST_EQ(replies.size(), 1);
        CHECK_TEST_EQ((ui32)replies[0].GetStatus(), (ui32)NRTYServer::TReply::OK);
        ReopenIndexers();

        serverInfo = Controller->GetServerInfo();
        info = &(*serverInfo)[0];
        const TString& infoOneMessage = (*info)["indexes"].GetStringRobust();
        DEBUG_LOG << "ServerInfo after one doc indexing " << infoOneMessage << Endl;
        CHECK_TEST_NEQ(infoStart, infoOneMessage);

        replies = IndexMessages(messages, DISK, 1);
        CHECK_TEST_EQ(replies.size(), 1);
        CHECK_TEST_EQ((ui32)replies[0].GetStatus(), (ui32)NRTYServer::TReply::OK);
        CHECK_TEST_TRUE(replies[0].GetStatusMessage().Contains("DUPLICATED"));
        ReopenIndexers();

        serverInfo = Controller->GetServerInfo();
        info = &(*serverInfo)[0];
        const TString& infoRepeatMessage = (*info)["indexes"].GetStringRobust();
        DEBUG_LOG << "ServerInfo after the same doc indexing "  << infoRepeatMessage << Endl;
        CHECK_TEST_EQ(infoOneMessage, infoRepeatMessage);
        if (checkSerachResults)
            CheckSearchResults(messages);
        return true;
    }

    bool Run() override {
        return DoRun(true);
    }

    bool InitConfig() override {
        SetIndexerParams(DISK, 200, 4);
        (*ConfigDiff)["Indexer.Common.RejectDuplicates"] = "true";
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestMODIFY_ONE_DOCUMENT_REJECT_UNCHANGED_FULL_ARC, TTestMODIFY_ONE_DOCUMENT_REJECT_UNCHANGEDCaseClass)
public:
    bool Run() override {
        return DoRun(false);
    }

    bool InitConfig() override {
        if (!TTestMODIFY_ONE_DOCUMENT_REJECT_UNCHANGEDCaseClass::InitConfig())
            return false;
        (*ConfigDiff)["IndexGenerator"] = FULL_ARCHIVE_COMPONENT_NAME;
        (*ConfigDiff)["Components"] = FULL_ARCHIVE_COMPONENT_NAME;
        (*ConfigDiff)["Indexer.Memory.Enabled"] = "false";
        (*ConfigDiff)["Searcher.SnippetsDeniedZones"] = "";
        (*ConfigDiff)["Searcher.TwoStepQuery"] = "false";
        (*SPConfigDiff)["Service.MetaSearch.TwoStepQuery"] = "false";
        return true;
    }
};

START_TEST_DEFINE(TestMODIFY_ONE_DOCUMENT_TIMESTAMP)
private:
    bool Test(TIndexerType indexer) {
        TString text = "body";
        if (indexer == DISK)
            text = "disk";
        else
            text = "realtime";
        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, 1, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
        messages.front().MutableDocument()->SetModificationTimestamp(0);
        ui32 maxDocuments = GetMaxDocuments() * 3.5;
        for (size_t i = 1; i < maxDocuments; ++i) {
            messages.push_back(messages.front());
            if (SendIndexReply)
                messages.back().SetMessageId(IMessageGenerator::CreateMessageId());
            messages.back().MutableDocument()->SetModificationTimestamp(i);
            messages.back().MutableDocument()->SetBody(text + ToString(i));
        }
        try {
            IndexMessages(messages, indexer, 1);
        } catch (...) {
        }
        if (indexer == DISK)
            ReopenIndexers();
        TVector<TDocSearchInfo> results;
        QuerySearch(text + ToString(maxDocuments - 1) + "&kps=" + ToString(messages.back().GetDocument().GetKeyPrefix()), results);
        CHECK_TEST_EQ(results.size(), 1);
        return true;
    }
public:
    bool Run() override {
        CHECK_TEST_EQ(Test(REALTIME), true);
        DeleteSpecial();
        CHECK_TEST_EQ(Test(DISK), true);
        return true;
    }

    bool InitConfig() override {
        SetIndexerParams(REALTIME, 200, 1);
        SetIndexerParams(DISK, 200, 4);
        return true;
    }
};

START_TEST_DEFINE(TestMODIFY_ONE_DOCUMENT_TIMESTAMP_ADD_AFTER_MERGE)
private:
    bool Test(TIndexerType indexer) {
        TString text = "body";
        if (indexer == DISK)
            text = "disk";
        else
            text = "realtime";
        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, 1, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
        ui32 maxDocuments = GetMaxDocuments() * 3.5;
        messages.front().MutableDocument()->SetModificationTimestamp(0);
        for (size_t i = 1; i < maxDocuments; ++i) {
            messages.push_back(messages.front());
            if (SendIndexReply)
                messages.back().SetMessageId(IMessageGenerator::CreateMessageId());
            messages.back().MutableDocument()->SetModificationTimestamp(i);
            messages.back().MutableDocument()->SetBody(text + ToString(i));
        }
        try {
            IndexMessages(messages, indexer, 1);
        } catch (...) {
        }
        if (indexer == DISK)
            ReopenIndexers();
        TVector<TDocSearchInfo> results;
        TQuerySearchContext context;
        context.AttemptionsCount = 30;
        context.ResultCountRequirement = 1;
        QuerySearch(text + ToString(maxDocuments - 1) + "&kps=" + ToString(messages.back().GetDocument().GetKeyPrefix()), results, context);
        PRINT_INFO_AND_TEST(CHECK_TEST_EQ(results.size(), 1));
        return true;
    }
public:
    bool Run() override {
        CHECK_TEST_EQ(Test(REALTIME), true);
        DeleteSpecial();
        CHECK_TEST_EQ(Test(DISK), true);
        return true;
    }

    bool InitConfig() override {
        (*ConfigDiff)["SearchersCountLimit"] = 1;
        SetMergerParams(true, 1, -1, mcpTIME, 500000);
        SetIndexerParams(REALTIME, 200, 1);
        SetIndexerParams(DISK, 200, 4);
        return true;
    }
};

START_TEST_DEFINE(TestMODIFY_ONE_DOCUMENT_CONTINUOUS_SEARCH)
private:
public:
    bool Run() override {
        TVector<NRTYServer::TMessage> messages;
        TVector<NRTYServer::TMessage> messages1;
        GenerateInput(messages, 1, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());

        messages1.push_back(messages.front());
        messages1.back().MutableDocument()->SetBody("newbody");

        IndexMessages(messages, REALTIME, 1);
        TVector<TDocSearchInfo> results;
        QuerySearch("body&kps=" + ToString(messages.back().GetDocument().GetKeyPrefix()), results);
        PRINT_INFO_AND_TEST(CHECK_TEST_EQ(results.size(), 1));

        ReopenIndexers();
        QuerySearch("body&kps=" + ToString(messages.back().GetDocument().GetKeyPrefix()), results);
        PRINT_INFO_AND_TEST(CHECK_TEST_EQ(results.size(), 1));

        IndexMessages(messages1, DISK, 1);
        QuerySearch("body&kps=" + ToString(messages.back().GetDocument().GetKeyPrefix()), results);
        PRINT_INFO_AND_TEST(CHECK_TEST_EQ(results.size(), 1));

        QuerySearch("newbody&kps=" + ToString(messages.back().GetDocument().GetKeyPrefix()), results);
        PRINT_INFO_AND_TEST(CHECK_TEST_EQ(results.size(), 0));

        ReopenIndexers();
        QuerySearch("newbody&kps=" + ToString(messages.back().GetDocument().GetKeyPrefix()), results);
        PRINT_INFO_AND_TEST(CHECK_TEST_EQ(results.size(), 1));

        QuerySearch("body&kps=" + ToString(messages.back().GetDocument().GetKeyPrefix()), results);
        PRINT_INFO_AND_TEST(CHECK_TEST_EQ(results.size(), 0));
        return true;
    }

    bool InitConfig() override {
        SetIndexerParams(REALTIME, 200, 1);
        SetIndexerParams(DISK, 200, 4);
        return true;
    }
};

START_TEST_DEFINE(TestMODIFY_ONE_DOCUMENT_CONTINUOUS_SEARCH_MERGER)
private:
public:
    bool Run() override {
        TVector<NRTYServer::TMessage> messages;
        TVector<NRTYServer::TMessage> messages1;
        GenerateInput(messages, 1, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());

        messages1.push_back(messages.front());
        messages1.back().MutableDocument()->SetBody("newbody");

        IndexMessages(messages, REALTIME, 1);
        TVector<TDocSearchInfo> results;
        QuerySearch("body&kps=" + ToString(messages.back().GetDocument().GetKeyPrefix()), results);
        PRINT_INFO_AND_TEST(CHECK_TEST_EQ(results.size(), 1));

        ReopenIndexers();
        QuerySearch("body&kps=" + ToString(messages.back().GetDocument().GetKeyPrefix()), results);
        PRINT_INFO_AND_TEST(CHECK_TEST_EQ(results.size(), 1));

        IndexMessages(messages1, DISK, 1);
        QuerySearch("body&kps=" + ToString(messages.back().GetDocument().GetKeyPrefix()), results);
        PRINT_INFO_AND_TEST(CHECK_TEST_EQ(results.size(), 1));

        QuerySearch("newbody&kps=" + ToString(messages.back().GetDocument().GetKeyPrefix()), results);
        PRINT_INFO_AND_TEST(CHECK_TEST_EQ(results.size(), 0));

        ReopenIndexers();
        QuerySearch("newbody&kps=" + ToString(messages.back().GetDocument().GetKeyPrefix()), results);
        PRINT_INFO_AND_TEST(CHECK_TEST_EQ(results.size(), 0));

        QuerySearch("body&kps=" + ToString(messages.back().GetDocument().GetKeyPrefix()), results);
        PRINT_INFO_AND_TEST(CHECK_TEST_EQ(results.size(), 1));

        Controller->ProcessCommand("create_merger_tasks");
        Controller->ProcessCommand("do_all_merger_tasks");

        QuerySearch("newbody&kps=" + ToString(messages.back().GetDocument().GetKeyPrefix()), results);
        PRINT_INFO_AND_TEST(CHECK_TEST_EQ(results.size(), 1));

        QuerySearch("body&kps=" + ToString(messages.back().GetDocument().GetKeyPrefix()), results);
        PRINT_INFO_AND_TEST(CHECK_TEST_EQ(results.size(), 0));

        return true;
    }

    bool InitConfig() override {
        (*ConfigDiff)["SearchersCountLimit"] = 1;
        SetMergerParams(true, 1, -1, mcpTIME, 500000);
        SetIndexerParams(REALTIME, 200, 1);
        SetIndexerParams(DISK, 200, 4);
        return true;
    }
};

START_TEST_DEFINE(TestRejectForFullIndex)
private:
    bool Test(TIndexerType indexer) {
        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, 1, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());

        IndexMessages(messages, indexer, 1);
        if (indexer == DISK) {
            ReopenIndexers();
        }

        (*ConfigDiff)["Indexer.Common.DocsCountLimit"] = 1;
        Controller->ApplyConfigDiff(ConfigDiff);

        IndexMessages(messages, indexer, 1);

        messages.clear();
        GenerateInput(messages, 1, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
        TIndexerClient::TContext context;
        context.IgnoreErrors = true;
        context.InterMessageTimeout = TDuration::MilliSeconds(200);
        TVector<NRTYServer::TReply> replies = IndexMessages(messages, DISK, context);
        CHECK_TEST_TRUE(replies.size() > 0);
        CHECK_TEST_EQ((ui32)replies[0].GetStatus(), (ui32)NRTYServer::TReply::NOTNOW);
        CHECK_TEST_TRUE(replies[0].GetStatusMessage().Contains("REDUNDANT"));
        return true;
    }

public:
    bool Run() override {
        if (!Test(REALTIME))
            return false;

        NDaemonController::TClearIndexAction cia(NDaemonController::apStartAndWait);
        NDaemonController::TControllerAgent agent(Controller->GetConfig().Controllers[0].Host, Controller->GetConfig().Controllers[0].Port);
        if (!agent.ExecuteAction(cia))
            ythrow yexception() << "Errors while execute restart" << Endl;
        Controller->RestartServer();
        return Test(DISK);
    }

    bool InitConfig() override {
        SetIndexerParams(REALTIME, 200, 4);
        SetIndexerParams(DISK, 200, 4);
        return true;
    }
};

