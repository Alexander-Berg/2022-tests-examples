#include "merger_test.h"
#include <saas/rtyserver/common/common_messages.h>

class TRTYExceptionMessageActor : public IMessageProcessor {
private:
    TVector<ui32> Handles;
    TVector<TRTYMessageException::TExceptionCase> ExceptionCases;
    TVector<ui32> Attemptions;
    TMutex Mutex;
public:

    TRTYExceptionMessageActor() {
        RegisterGlobalMessageProcessor(this);
    }

    TRTYExceptionMessageActor(TRTYMessageException::TExceptionCase exceptionCase, ui32 handle, ui32 attempts = Max<ui32>()) {
        RegisterGlobalMessageProcessor(this);
        RegisterError(exceptionCase, handle, attempts);
    }

    ~TRTYExceptionMessageActor() {
        UnregisterGlobalMessageProcessor(this);
    }

    void RegisterError(TRTYMessageException::TExceptionCase exceptionCase, ui32 handle, ui32 attempts = Max<ui32>()) {
        Handles.push_back(handle);
        ExceptionCases.push_back(exceptionCase);
        Attemptions.push_back(attempts);
    }

    bool Process(IMessage* message) override {
        TRTYMessageException* exMessage = dynamic_cast<TRTYMessageException*>(message);
        if (exMessage) {
            FATAL_LOG << "Fake exception request" << Endl;
            CHECK_WITH_LOG(Handles.size() == ExceptionCases.size());
            CHECK_WITH_LOG(Attemptions.size() == ExceptionCases.size());
            for (ui32 i = 0; i < Handles.size(); ++i) {
                ui32 h = Handles[i];
                TRTYMessageException::TExceptionCase e = ExceptionCases[i];
                if (h == exMessage->GetHandle() && e == exMessage->GetExceptionCase()) {
                    TGuard<TMutex> g(Mutex);
                    if (Attemptions[i]) {
                        FATAL_LOG << "Fake exception for test " << (ui32)e << "/" << h << Endl;
                        Attemptions[i]--;
                        ythrow yexception() << "Fake exception for test " << (ui32)e << "/" << h;
                    }
                }
            }
            return true;
        }
        return false;
    }

    TString Name() const override {
        return "FakeExceptionsGenerator";
    }
};

START_TEST_DEFINE_PARENT(TestMergeWhileIndexingModify, TMergerTest, TTestMarksPool::OneBackendOnly)
bool Run() override {
    const int messagesPerIndex = 500;
    ui32 maxSegments = GetMergerMaxSegments();
    VERIFY_WITH_LOG(maxSegments > 1, "at least two segments required for test");
    VERIFY_WITH_LOG(!IsMergerTimeCheck(), "timed check must be off for test");
    TVector<NRTYServer::TMessage> messages, additionalMessages;
    GenerateInput(messages, messagesPerIndex * (maxSegments + 3), NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    additionalMessages = messages;
    for (TVector<NRTYServer::TMessage>::iterator i = additionalMessages.begin(); i != additionalMessages.end(); ++i) {
        i->SetMessageType(NRTYServer::TMessage::MODIFY_DOCUMENT);
        i->MutableDocument()->SetBody("asdsda" + ToString(i - additionalMessages.begin()));
    }

    for (unsigned i = 0; i < maxSegments + 3; ++i) {
        IndexMessages(TVector<NRTYServer::TMessage>(messages.begin() + i * messagesPerIndex, messages.begin() + (i + 1) * messagesPerIndex), REALTIME, 1);
        ReopenIndexers();
    }
    Controller->ProcessCommand("create_merger_tasks");
    Controller->ProcessCommand("do_all_merger_tasks&wait=false");
    IndexMessages(additionalMessages, REALTIME, 1);
    Controller->ProcessCommand("do_all_merger_tasks");
    Sleep(TDuration::Seconds(15));
    CheckMergerResult();
    CheckSearchResults(additionalMessages, TSet<std::pair<ui64, TString> >(), 1, 1, true);
    Controller->GetServerInfo();
    return true;
}

public:
    bool InitConfig() override {
        SetIndexerParams(DISK, 2000, 1);
        SetIndexerParams(REALTIME, 2000);
        SetMergerParams(true, 2, -1, mcpNONE);
        (*ConfigDiff)["DeadIndexesClearIntervalSeconds"] = 2;
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestMergeWhileIndexingModifyOneSource, TMergerTest, TTestMarksPool::OneBackendOnly)
bool Run() override {
    const int messagesPerIndex = 500;
    ui32 maxSegments = GetMergerMaxSegments();
    VERIFY_WITH_LOG(maxSegments > 1, "at least two segments required for test");
    TVector<NRTYServer::TMessage> messages, additionalMessages;
    GenerateInput(messages, messagesPerIndex * (maxSegments + 3), NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    additionalMessages = messages;
    for (TVector<NRTYServer::TMessage>::iterator i = additionalMessages.begin(); i != additionalMessages.end(); ++i) {
        i->SetMessageType(NRTYServer::TMessage::MODIFY_DOCUMENT);
        i->MutableDocument()->SetBody("asdsda" + ToString(i - additionalMessages.begin()));
    }

    for (unsigned i = 0; i < maxSegments + 3; ++i) {
        IndexMessages(TVector<NRTYServer::TMessage>(messages.begin() + i * messagesPerIndex, messages.begin() + (i + 1) * messagesPerIndex), DISK, 1);
        ReopenIndexers();
    }
    Controller->ProcessCommand("create_merger_tasks");
    Controller->ProcessCommand("do_all_merger_tasks&wait=false");
    IndexMessages(additionalMessages, DISK, 1);
    ReopenIndexers();

    TSearchMessagesContext context1 = TSearchMessagesContext::BuildDefault(1);
    TSearchMessagesContext context0 = TSearchMessagesContext::BuildDefault(0);

    context1.ByText = true;
    context0.ByText = true;
    Controller->ProcessCommand("create_merger_tasks");
    Controller->ProcessCommand("do_all_merger_tasks");
    sleep(15);
    CheckMergerResult();
    CHECK_TEST_EQ(CheckSearchResultsSafe(additionalMessages, context1), true);
    CHECK_TEST_EQ(CheckSearchResultsSafe(messages, context0), true);
    Controller->GetServerInfo();
    return true;
}

public:
    bool InitConfig() override {
        SetIndexerParams(DISK, 2000, 1);
        SetIndexerParams(REALTIME, 2000);
        SetMergerParams(true, 3, -1, mcpTIME, 10000000);
        (*ConfigDiff)["SearchersCountLimit"] = 3;
        (*ConfigDiff)["Indexer.Memory.Enabled"] = false;
        (*ConfigDiff)["DeadIndexesClearIntervalSeconds"] = 2;
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestMergeWhileIndexingFastUpdate, TMergerTest)
bool Run() override {
    const int messagesPerIndex = 500;
    ui32 maxSegments = GetMergerMaxSegments();
    VERIFY_WITH_LOG(maxSegments > 1, "at least two segments required for test");
    VERIFY_WITH_LOG(!IsMergerTimeCheck(), "timed check must be off for test");
    TAttrMap::value_type map1, map2;
    map1["unique_attr"] = 1;
    map2["unique_attr"] = 2;
    const size_t countMessages = messagesPerIndex * (maxSegments + 1);
    TAttrMap initAttrs(countMessages, map1);
    TAttrMap updateAttrs(countMessages, map2);

    TVector<NRTYServer::TMessage> messages, additionalMessages;
    GenerateInput(messages, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), initAttrs);
    GenerateInput(additionalMessages, countMessages, NRTYServer::TMessage::DEPRECATED__UPDATE_DOCUMENT, GetIsPrefixed(), updateAttrs, "");
    for (size_t i = 0; i < countMessages; ++i) {
        additionalMessages[i].MutableDocument()->SetUrl(messages[i].GetDocument().GetUrl());
        additionalMessages[i].MutableDocument()->SetKeyPrefix(messages[i].GetDocument().GetKeyPrefix());
        additionalMessages[i].MutableDocument()->clear_groupattributes();
        {
            auto* prop = additionalMessages[i].MutableDocument()->AddGroupAttributes();
            prop->SetName("unique_attr");
            prop->SetValue("2");
            prop->SetType(NRTYServer::TAttribute::INTEGER_ATTRIBUTE);
        }
        additionalMessages[i].MutableDocument()->clear_searchattributes();
        additionalMessages[i].MutableDocument()->clear_factors();
        additionalMessages[i].MutableDocument()->clear_documentproperties();
    }

    for (unsigned i = 0; i < maxSegments + 1; ++i) {
        IndexMessages(TVector<NRTYServer::TMessage>(messages.begin() + i * messagesPerIndex, messages.begin() + (i + 1) * messagesPerIndex), REALTIME, 1);
        ReopenIndexers();
    }
    Controller->ProcessCommand("create_merger_tasks");
    Controller->ProcessCommand("do_all_merger_tasks&wait=false");
    IndexMessages(additionalMessages, REALTIME, 1);
    Controller->ProcessCommand("do_all_merger_tasks");
    CheckMergerResult();
    TVector<TDocSearchInfo> results;
    Controller->GetServerInfo();
    if (!NoSearch) {
        for (size_t i = 0; i < countMessages; ++i) {
            TQuerySearchContext context;
            context.AttemptionsCount = 5;
            context.ResultCountRequirement = 1;
            QuerySearch("url:\"" + messages[i].GetDocument().GetUrl() + "\"&kps=" + ToString(messages[i].GetDocument().GetKeyPrefix()), results, context);
            CHECK_TEST_EQ(results.size(), 1);
            context.AttemptionsCount = 5;
            context.ResultCountRequirement = 1;
            QuerySearch("url:\"" + messages[i].GetDocument().GetUrl() + "\"&fa=unique_attr:2&kps=" + ToString(messages[i].GetDocument().GetKeyPrefix()), results, context);
            CHECK_TEST_EQ(results.size(), 1);
        }
    }
    return true;
}

public:
    bool InitConfig() override {
        SetIndexerParams(DISK, 2000, 1);
        SetIndexerParams(REALTIME, 2000);
        SetMergerParams(true, 2, -1, mcpNONE);
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestMergeWhileIndexingReopen, TMergerTest)
bool Run() override {
    const int messagesPerIndex = 200;
    ui32 maxSegments = GetMergerMaxSegments();
    VERIFY_WITH_LOG(maxSegments > 1, "at least two segments required for test");
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, messagesPerIndex * (maxSegments + 1), NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    IndexMessages(messages, REALTIME, 20);
    CheckMergerResult();
    return true;
}

public:
    bool InitConfig() override {
        SetIndexerParams(DISK, 200, 1);
        (*ConfigDiff)["Indexer.Memory.MaxDocumentsReserveCapacityCoeff"] = 20;
        SetIndexerParams(REALTIME, 200);
        SetMergerParams(true, 2, -1, mcpTIME, 50);
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestMergeManySegments, TMergerTest)

bool CheckCountDocumentsDistribution() {
    PrintInfoServer();
    TJsonPtr serverInfo(Controller->GetServerInfo());
    NJson::TJsonValue& info = (*serverInfo)[0];

    const NJson::TJsonValue::TMapType& indicesMap = info["indexes"].GetMap();
    ui32 sumCount = 0;
    for (NJson::TJsonValue::TMapType::const_iterator i = indicesMap.begin(); i != indicesMap.end(); ++i) {
        const TString& name = i->first;
        if (!name.StartsWith("index_"))
            continue;
        if (i->second["has_searcher"].GetInteger() == 1) {
            CHECK_TEST_EQ(i->second["count_withnodel"].GetInteger(), 100);
            sumCount += i->second["count_withnodel"].GetInteger();
        }
    }
    CHECK_TEST_EQ(sumCount, 50 * 2 * GetMergerMaxSegments() * GetShardsNumber());
    return true;
}

bool Run() override {
    const int messagesPerSegment = 50;
    ui32 maxSegments = GetMergerMaxSegments();
    VERIFY_WITH_LOG(maxSegments > 1, "at least two segments required for test");
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, 2 * maxSegments * messagesPerSegment * GetShardsNumber(), NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    for (ui32 i = 0; i < 2 * maxSegments; ++i) {
        IndexMessages(TVector<NRTYServer::TMessage>(messages.begin() + i * messagesPerSegment * GetShardsNumber(), messages.begin() + (i + 1) * messagesPerSegment * GetShardsNumber()), REALTIME, 1);
        ReopenIndexers();
        Controller->ProcessCommand("do_all_merger_tasks");
    }
    return CheckCountDocumentsDistribution();
}

public:
    bool InitConfig() override {
        SetIndexerParams(DISK, 50, 1);
        (*ConfigDiff)["Indexer.Memory.MaxDocumentsReserveCapacityCoeff"] = 20;
        SetIndexerParams(REALTIME, 50);
        SetMergerParams(true, 5, -1, mcpTIME, 1000000);
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestMaxSegments, TMergerTest)
bool Run() override {
    const int messagesPerIndex = 10;
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, messagesPerIndex, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());

    IndexMessages(messages, DISK, 1);
    ReopenIndexers();

    IndexMessages(messages, DISK, 1);
    ReopenIndexers();

    MUST_BE_BROKEN(IndexMessages(messages, DISK, 1));

    Controller->ProcessCommand("create_merger_tasks");
    Controller->ProcessCommand("do_all_merger_tasks");

    IndexMessages(messages, DISK, 1);
    return true;
}

public:
    bool InitConfig() override {
        SetIndexerParams(DISK, 100, 1);
        (*ConfigDiff)["ShardsNumber"] = 1;
        (*ConfigDiff)["Indexer.Disk.MaxSegments"] = 2;
        SetIndexerParams(REALTIME, 200);
        SetMergerParams(true, 1, -1, mcpNONE, 50);
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestMergerExceptionDoTask, TMergerTest)
bool Run() override {
    THolder<TRTYExceptionMessageActor> exceptionGenerator(new TRTYExceptionMessageActor(TRTYMessageException::ecMergerDoTask, 0, 1));
    const int messagesPerIndex = 20;
    TVector<NRTYServer::TMessage> messages1;
    TVector<NRTYServer::TMessage> messages2;
    GenerateInput(messages1, messagesPerIndex, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    GenerateInput(messages2, messagesPerIndex, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    IndexMessages(messages1, REALTIME, 1);
    ReopenIndexers();
    IndexMessages(messages2, REALTIME, 1);
    ReopenIndexers();
    try {
        CheckMergerResult();
        ERROR_LOG << "Exception doesn't working..." << Endl;
        return false;
    } catch (...) {
    }
    IndexMessages(messages1, REALTIME, 1);
    ReopenIndexers();
    CheckMergerResult();

    return true;
}

public:
    bool InitConfig() override {
        SetIndexerParams(DISK, 200, 1);
        (*ConfigDiff)["Indexer.Memory.MaxDocumentsReserveCapacityCoeff"] = 20;
        SetIndexerParams(REALTIME, 200);
        SetMergerParams(true, 1, -1, mcpNONE, 50);
        return true;
    }
};
