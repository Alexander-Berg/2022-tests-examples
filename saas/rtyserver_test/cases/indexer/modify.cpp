#include <saas/rtyserver_test/testerlib/rtyserver_test.h>

START_TEST_DEFINE(TestMODIFY_DOCUMENT)
    bool Run() override {
        const int CountMessages = 10;
        TVector<NRTYServer::TMessage> messagesForMemory, messagesForDisk;
        GenerateInput(messagesForDisk, CountMessages, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
        GenerateInput(messagesForMemory, CountMessages, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());

        IndexMessages(messagesForDisk, DISK, 3);
        ReopenIndexers();
        CheckSearchResults(messagesForDisk);

        CHECK_TEST_EQ(Controller->GetMetric("Indexer_DocumentsAdded", TBackendProxy::TBackendSet(0)), 10);
        CHECK_TEST_EQ(Controller->GetMetric("Indexer_DocumentsModified", TBackendProxy::TBackendSet(0)), 20);
        CHECK_TEST_EQ(Controller->GetMetric("Indexer_DocumentsDeleted", TBackendProxy::TBackendSet(0)), 0);
        CHECK_TEST_EQ(Controller->GetMetric("Indexer_DocumentsUpdated", TBackendProxy::TBackendSet(0)), 0);

        IndexMessages(messagesForDisk, DISK, 1);
        ReopenIndexers();
        CheckSearchResults(messagesForDisk);

        IndexMessages(messagesForDisk, DISK, 1);
        ReopenIndexers();
        CheckSearchResults(messagesForDisk);

        IndexMessages(messagesForMemory, REALTIME, 10);
        IndexMessages(messagesForDisk, REALTIME, 10);
        CheckSearchResults(messagesForMemory);
        CheckSearchResults(messagesForDisk);

        ReopenIndexers();
        CheckSearchResults(messagesForMemory);
        CheckSearchResults(messagesForDisk);

        CHECK_TEST_EQ(Controller->GetMetric("Indexer_DocumentsAdded", TBackendProxy::TBackendSet(0)), 20);
        CHECK_TEST_EQ(Controller->GetMetric("Indexer_DocumentsModified", TBackendProxy::TBackendSet(0)), 230);
        CHECK_TEST_EQ(Controller->GetMetric("Indexer_DocumentsDeleted", TBackendProxy::TBackendSet(0)), 0);
        CHECK_TEST_EQ(Controller->GetMetric("Indexer_DocumentsUpdated", TBackendProxy::TBackendSet(0)), 0);

        return true;
    }
};
