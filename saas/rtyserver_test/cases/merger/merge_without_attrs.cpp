#include "merger_test.h"

START_TEST_DEFINE_PARENT(TestMergeWithoutAttrs, TMergerTest, TTestMarksPool::OneBackendOnly)
    bool Run() override {
        const int messagesPerIndex = 2;
        ui32 maxSegments = GetMergerMaxSegments();
        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, messagesPerIndex * (maxSegments + 1), NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), "body", false);
        for (unsigned i = 0; i < maxSegments + 1; ++i) {
            IndexMessages(TVector<NRTYServer::TMessage>(messages.begin() + i * messagesPerIndex, messages.begin() + (i + 1) * messagesPerIndex), REALTIME, 1);
            ReopenIndexers();
        }
        Controller->ProcessCommand("create_merger_tasks");
        Controller->ProcessCommand("do_all_merger_tasks");
        CheckMergerResult();
        CheckSearchResults(messages);
        return true;
    }
public:
    bool InitConfig() override {
        SetMergerParams(true, 1, -1, mcpNONE);
        SetIndexerParams(DISK, 100, 1);
        return true;
    }
};
