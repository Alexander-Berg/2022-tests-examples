#include <saas/rtyserver_test/testerlib/rtyserver_test.h>

START_TEST_DEFINE(TestDisableRTSearch)
bool Run() override {
    const ui32 kps = GetIsPrefixed();
    TVector<NRTYServer::TMessage> messages;
    TVector<TDocSearchInfo> results;

    messages.clear();
    GenerateInput(messages, 10, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    for (auto&& m : messages) {
        m.MutableDocument()->SetKeyPrefix(kps);
    }
    IndexMessages(messages, DISK, 1);
    ReopenIndexers();
    QuerySearch("url:\"*\"&kps=" + ToString(kps), results, nullptr, nullptr, true);
    CHECK_TEST_EQ(results.size(), 10);

    Controller->ProcessCommand("disable_rtsearch");
    QuerySearch("url:\"*\"&kps=" + ToString(kps), results, nullptr, nullptr, true);
    CHECK_TEST_EQ(results.size(), 10);
    Controller->ProcessCommand("enable_rtsearch");

    messages.clear();
    GenerateInput(messages, 10, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    for (auto&& m : messages) {
        m.MutableDocument()->SetKeyPrefix(kps);
    }
    IndexMessages(messages, REALTIME, 1);
    QuerySearch("url:\"*\"&kps=" + ToString(kps), results, nullptr, nullptr, true);
    Y_ENSURE(results.size() == 20, "incorrect result count after indexing second batch");

    for (ui32 i = 0; i < 10; ++i) {
        Controller->ProcessCommand("disable_rtsearch");
        QuerySearch("url:\"*\"&kps=" + ToString(kps), results, nullptr, nullptr, true);
        Y_ENSURE(results.size() == 10, "incorrect result count after disable_rtsearch");

        Controller->ProcessCommand("enable_rtsearch");
        QuerySearch("url:\"*\"&kps=" + ToString(kps), results, nullptr, nullptr, true);
        Y_ENSURE(results.size() == 20, "incorrect result count after enable_rtsearch");
    }

    return true;
}
};

START_TEST_DEFINE(TestSearchersCountLimitAfterRestart)

ui32 SearchersCountLimit = 3;

bool Run() override {
    const ui32 kps = GetIsPrefixed();
    TVector<NRTYServer::TMessage> messages;
    TVector<TDocSearchInfo> results;

    for (ui32 i = 0; i < 2 * SearchersCountLimit; ++i) {
        messages.clear();
        GenerateInput(messages, 1, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
        for (auto&& m : messages) {
            m.MutableDocument()->SetKeyPrefix(kps);
        }
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();
    }
    QuerySearch("url:\"*\"&kps=" + ToString(kps), results, nullptr, nullptr, true);
    Y_ENSURE(results.size() == SearchersCountLimit, "incorrect result count after reopen");

    Controller->ProcessCommand("restart");
    QuerySearch("url:\"*\"&kps=" + ToString(kps), results, nullptr, nullptr, true);
    Y_ENSURE(results.size() == SearchersCountLimit, "incorrect result count after restart");

    return true;
}

bool InitConfig() override {
    SetMergerParams(true, 3, -1, mcpTIME, 10000000);
    (*ConfigDiff)["SearchersCountLimit"] = 3;
    return true;
}
};

START_TEST_DEFINE(TestSearchersCountLimitAfterRestartAndMerge)

ui32 SearchersCountLimit = 3;

bool Run() override {
    const ui32 kps = GetIsPrefixed();
    TVector<NRTYServer::TMessage> messages;
    TVector<TDocSearchInfo> results;

    for (ui32 i = 0; i < 4 * SearchersCountLimit; ++i) {
        messages.clear();
        GenerateInput(messages, 1, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
        for (auto&& m : messages) {
            m.MutableDocument()->SetKeyPrefix(kps);
        }
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();

        if (i % 2 == 1) {
            Controller->ProcessCommand("create_merger_tasks");
            Controller->ProcessCommand("do_all_merger_tasks");
        }
    }
    QuerySearch("url:\"*\"&kps=" + ToString(kps), results, nullptr, nullptr, true);
    CHECK_TEST_EQ(results.size(), 2 * SearchersCountLimit);

    Controller->ProcessCommand("restart");
    QuerySearch("url:\"*\"&kps=" + ToString(kps), results, nullptr, nullptr, true);
    CHECK_TEST_EQ(results.size(), 2 * SearchersCountLimit);

    return true;
}

bool InitConfig() override {
    SetMergerParams(true, 1, -1, mcpTIME, 10000000, 2);
    (*ConfigDiff)["SearchersCountLimit"] = SearchersCountLimit;
    return true;
}
};

START_TEST_DEFINE(TestDelegateFuckup)

ui32 SearchersCountLimit = 1;

bool Run() override {
    Controller->SetRequiredMetaSearchLevel(1);
    const ui32 kps = GetIsPrefixed();
    TVector<NRTYServer::TMessage> messages;
    TVector<TDocSearchInfo> results;

    for (ui32 i = 0; i < SearchersCountLimit + 1; ++i) {
        messages.clear();
        GenerateInput(messages, 1, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
        for (auto&& m : messages) {
            m.MutableDocument()->SetKeyPrefix(kps);
        }
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();
    }
    {
        messages.clear();
        GenerateInput(messages, 1, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
        for (auto&& m : messages) {
            m.MutableDocument()->SetKeyPrefix(kps);
        }
        IndexMessages(messages, DISK, 1);
        // Do not close
    }

    Controller->ProcessCommand("create_merger_tasks");
    Controller->ProcessCommand("do_all_merger_tasks");

    QuerySearch("url:\"*\"&kps=" + ToString(kps), results, nullptr, nullptr, true);
    Y_ENSURE(results.size() == SearchersCountLimit + 1, "incorrect result count after merge");

    ReopenIndexers();

    QuerySearch("url:\"*\"&kps=" + ToString(kps), results, nullptr, nullptr, true);
    Y_ENSURE(results.size() == SearchersCountLimit + 1, "incorrect result count after closing extra index");

    Controller->ProcessCommand("restart");

    QuerySearch("url:\"*\"&kps=" + ToString(kps), results, nullptr, nullptr, true);
    Y_ENSURE(results.size() == SearchersCountLimit + 1, "incorrect result count after restart");

    return true;
}

bool InitConfig() override {
    SetMergerParams(true, 1, -1, mcpTIME, 10000000);
    (*ConfigDiff)["Searcher.DelegateRequestOptimization"] = "true";
    (*ConfigDiff)["SearchersCountLimit"] = SearchersCountLimit;
    (*ConfigDiff)["Indexer.Memory.Enabled"] = "false";
    return true;
}
};

START_TEST_DEFINE(TestSearchPaths)
bool Run() override {
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 1, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    IndexMessages(messages, REALTIME, 1);

    const TString query = "?ms=proto&hr=da&text=body";
    TString result;
    Y_ENSURE(200 == ProcessQuery("/" + query, &result), "Empty collection did not work");
    DEBUG_LOG << result << Endl;
    Y_ENSURE(200 == ProcessQuery("/yandsearch" + query, &result), "Empty collection did not work");
    DEBUG_LOG << result << Endl;
    Y_ENSURE(200 == ProcessQuery("/pony" + query, &result), "Empty collection did not work");
    DEBUG_LOG << result << Endl;

    return true;
}
bool InitConfig() override {
    (*ConfigDiff)["Searcher.SearchPath"] = ",yandsearch,pony";
    return true;
}
};
