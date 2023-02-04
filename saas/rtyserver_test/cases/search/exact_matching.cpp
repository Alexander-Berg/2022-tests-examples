#include <saas/rtyserver_test/testerlib/rtyserver_test.h>

START_TEST_DEFINE(TestExactMatching_Memory)
    bool Run() override {
        const TString shortText = "This";
        const TString medText = "This is story.";
        const TString longText =
                "This is long story. about new search server. When it was born, it was named RTYServer-> Its parents were developers in one big software company.";

        const unsigned int countMedMessages = 10;
        TVector<NRTYServer::TMessage> messagesTest, messagesAll;
        TMap<TString, NRTYServer::TMessage> needMessages;
        INFO_LOG << "Testing memory index" << Endl;

        GenerateInput(messagesTest, countMedMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), medText);
        GenerateInput(messagesAll, countMedMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), shortText);
        GenerateInput(messagesAll, countMedMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), longText);

        IndexMessages(messagesTest, REALTIME, 1);
        IndexMessages(messagesAll, REALTIME, 1);

        if (!CheckExistsByText("This+is+story.", true, messagesTest)) {
            DEBUG_LOG << "CheckExistsByText before reopen was failed(((" << Endl;
            ythrow yexception() << "CheckExistsByText before reopen was failed(((";
        }

        ReopenIndexers();

        if (!CheckExistsByText("This+is+story.", true, messagesTest)) {
            DEBUG_LOG << "CheckExistsByText after reopen was failed(((" << Endl;
            ythrow yexception() << "CheckExistsByText after reopen was failed(((";
        }
        return true;
    }
};

START_TEST_DEFINE(TestExactMatching_Disk)
    bool Run() override {
        const TString shortText = "This";
        const TString medText = "This is story.";
        const TString longText = "This is long story. about new search server. When it was born, it was named RTYServer-> Its parents were developers in one big software company.";

        const unsigned int countMedMessages = 10;
        TVector<NRTYServer::TMessage> messagesTest, messagesAll;
        TMap<TString, NRTYServer::TMessage> needMessages;
        INFO_LOG << "Testing disk index" << Endl;

        GenerateInput(messagesTest, countMedMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), medText);
        GenerateInput(messagesAll, countMedMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), shortText);
        GenerateInput(messagesAll, countMedMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed(), TAttrMap(), longText);

        IndexMessages(messagesTest, DISK, 1);
        IndexMessages(messagesAll, DISK, 1);

        ReopenIndexers();

        if (!CheckExistsByText("This+is+story.", true, messagesTest)) {
            DEBUG_LOG << "CheckExistsByText after reopen was failed(((" << Endl;
            ythrow yexception() << "CheckExistsByText after reopen was failed(((";
        }
        return true;
    }
};
