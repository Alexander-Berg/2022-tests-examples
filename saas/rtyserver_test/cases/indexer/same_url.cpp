#include <saas/rtyserver_test/testerlib/rtyserver_test.h>

SERVICE_TEST_RTYSERVER_DEFINE(TestSameUrl)
    void DoTest(TIndexerType indexer) {
        const size_t countMessages = 1;

        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap());
        IndexMessages(messages, indexer, 2);
        if (indexer == DISK)
            ReopenIndexers();
        Sleep(TDuration::Seconds(2));
        CheckSearchResults(messages);
    }

    bool InitConfig() override {
        SetIndexerParams(DISK, 10, 1);
        SetIndexerParams(REALTIME, 10, 1);
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestSameUrlDisk, TestSameUrl)
    bool Run() override {
        DoTest(DISK);
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestSameUrlMemory, TestSameUrl)
    bool Run() override {
        DoTest(REALTIME);
        return true;
    }
};

SERVICE_TEST_RTYSERVER_DEFINE(TestSameUrlDiffKps)
    void DoTest(TIndexerType indexer, bool reopen, bool check) {
        if (!GetIsPrefixed())
            return;
        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, 2, NRTYServer::TMessage::MODIFY_DOCUMENT, true);
        messages[1].MutableDocument()->SetUrl(messages[0].GetDocument().GetUrl());
        messages[0].MutableDocument()->SetBody("aaa");
        messages[1].MutableDocument()->SetBody("bbb");
        const TString kps = GetAllKps(messages);
        IndexMessages(messages, indexer, 2);
        TSet<std::pair<ui64, TString> > deleted;
        if (check) {
            CheckSearchResults(messages, deleted, 1, -1, true);
            CheckSearchResults(messages, deleted, 1, -1, false);
        }
        DeleteSomeMessages(messages, deleted, indexer, 2);
        if (reopen)
            ReopenIndexers();
        else
            Sleep(TDuration::Seconds(2));
        CheckSearchResults(messages, deleted, 1, -1, true);
        CheckSearchResults(messages, deleted, 1, -1, false);
    }
};

START_TEST_DEFINE_PARENT(TestSameUrlDiffKpsDisk, TestSameUrlDiffKps)
bool Run() override {
    DoTest(DISK, true, false);
    return true;
}
};

START_TEST_DEFINE_PARENT(TestSameUrlDiffKpsTemp, TestSameUrlDiffKps)
bool Run() override {
    DoTest(DISK, true, false);
    return true;
}
};

START_TEST_DEFINE_PARENT(TestSameUrlDiffKpsMemory, TestSameUrlDiffKps)
bool Run() override {
    DoTest(REALTIME, false, true);
    return true;
}
};
