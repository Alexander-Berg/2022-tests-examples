#include "merger_test.h"

START_TEST_DEFINE_PARENT(TestMergerDoubles, TMergerTest, TTestMarksPool::OneBackendOnly)
    bool Run() override {
        ui32 messagesPerIndex = 2;
        ui32 maxSegments = GetMergerMaxSegments();
        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, messagesPerIndex, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());

        for (ui32 i = 0; i < maxSegments + 1; ++i) {
            for (ui32 j = 0; j < messages.size(); ++j) {
                NRTYServer::TMessage::TDocument* doc = messages[j].MutableDocument();
                doc->SetModificationTimestamp(Seconds() + i);
                doc->SetBody("Generation" + ToString(i));
            }
            IndexMessages(messages, REALTIME, 1);
            ReopenIndexers();
        }
        Controller->ProcessCommand("create_merger_tasks");
        Controller->ProcessCommand("do_all_merger_tasks");
        CheckMergerResult();
        CheckSearchResults(messages);
        CheckSearchResults(messages, TSet<std::pair<ui64, TString> >(), GetIsPrefixed() ? 1 : messagesPerIndex, -1, true);
        for (ui32 i = 0; i < messages.size(); ++i)
            messages[i].MutableDocument()->SetBody("Generation" + ToString(maxSegments - 1));
        CheckSearchResults(messages, TSet<std::pair<ui64, TString> >(), 0, -1, true);
        return true;
    }
public:
    bool InitConfig() override {
        SetMergerParams(true, 1, -1, mcpNONE);
        SetIndexerParams(DISK, 100, 1);
        return true;
    }
};

START_TEST_DEFINE_PARENT(TestMergerDoublesOlderSegment, TMergerTest, TTestMarksPool::OneBackendOnly)
bool Run() override {
    ui32 messagesPerIndex = 2;
    ui32 maxSegments = GetMergerMaxSegments();
    TVector<NRTYServer::TMessage> messages;
    GenerateInput(messages, messagesPerIndex, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    for (ui32 i = 0; i < maxSegments + 2; ++i) {
        for (ui32 j = 0; j < messages.size(); ++j) {
            NRTYServer::TMessage::TDocument* doc = messages[j].MutableDocument();
            doc->SetModificationTimestamp(Seconds());
            doc->SetBody("Generation" + ToString(i));
            doc->SetKeyPrefix(messages[0].MutableDocument()->GetKeyPrefix());
        }
        sleep(2);
        IndexMessages(messages, REALTIME, 1);
        if (i != maxSegments + 1)
            ReopenIndexers();
    }
    Controller->ProcessCommand("create_merger_tasks");
    Controller->ProcessCommand("do_all_merger_tasks");
    CheckMergerResult();
    DEBUG_LOG << "Older segments merged, searching for the latest doc..." << Endl;
    CheckSearchResults(messages, TSet<std::pair<ui64, TString> >(), messagesPerIndex, -1, true);

    ReopenIndexers();
    Controller->ProcessCommand("create_merger_tasks");
    Controller->ProcessCommand("do_all_merger_tasks");
    CheckMergerResult();

    DEBUG_LOG << "All segments merged, searching for the latest doc..." << Endl;
    CheckSearchResults(messages);
    CheckSearchResults(messages, TSet<std::pair<ui64, TString> >(), messagesPerIndex, -1, true);
    DEBUG_LOG << "Check older docs absence..." << Endl;
    for (ui32 i = 0; i < messages.size(); ++i)
        messages[i].MutableDocument()->SetBody("Generation" + ToString(maxSegments));
    CheckSearchResults(messages, TSet<std::pair<ui64, TString> >(), 0, -1, true);
    return true;
}
public:
    bool InitConfig() override {
        SetMergerParams(true, 1, -1, mcpNONE);
        SetIndexerParams(DISK, 100, 1);
        return true;
    }
};
