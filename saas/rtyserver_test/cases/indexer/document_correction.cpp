#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/library/daemon_base/controller_actions/restart.h>
#include <saas/rtyserver/controller/controller_actions/clear_index_action.h>

START_TEST_DEFINE(TestDOCUMENT_CORRECTION)
/*
    Checks that invalid document doesn't cause valid document deletion
    with same keys due to deffered deletions mechanism
*/
private:
    void MakeInvalid(NRTYServer::TMessage &correctMessage) {
        correctMessage.SetMessageType(NRTYServer::TMessage::MODIFY_DOCUMENT);
        NRTYServer::TMessage::TDocument& doc = *correctMessage.MutableDocument();
        NRTYServer::TAttribute& att = *doc.AddSearchAttributes();
        att.set_name("date");
        att.set_type(NRTYServer::TAttribute::LITERAL_ATTRIBUTE);
        att.set_value("");
    }
public:
    bool Run() override {
        TVector<NRTYServer::TMessage> correctMessage, incorrectMessage, message;
        TAttrMap correctSearchAttrs;
        correctSearchAttrs.resize(1);
        correctSearchAttrs[0].insert(std::make_pair("date", TAttr("28.07.2012")));
        GenerateInput(correctMessage, 1, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed(), TAttrMap(), "cor", false, correctSearchAttrs);
        IndexMessages(correctMessage, DISK, 1);
        ReopenIndexers();

        TVector<TDocSearchInfo> results;

        if (!CheckIndexSize(1, DISK, 30)) {
            TEST_FAILED("Wrong documents number in DISK indexer");
        }
        QuerySearch("cor&" + GetAllKps(correctMessage), results);
        if (results.size() != 1) {
            TEST_FAILED("Couldn't find document in DISK indexer");
        }

        GenerateInput(incorrectMessage, 1, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed(), TAttrMap(), "add");
        incorrectMessage.push_back(correctMessage[0]);
        MakeInvalid(incorrectMessage[1]);
        incorrectMessage[1].MutableDocument()->SetBody("inc");
        IndexMessages(incorrectMessage, DISK, 1, 0, true, true, TDuration(), TDuration(), 1, "tests", 0, true); //setting ignoreFails = true because document won't be indexed
        ReopenIndexers();

        if (!CheckIndexSize(2, DISK, 30)) {
            TEST_FAILED("Disk index must contain 2 document");
        }

        QuerySearch("cor&" + GetAllKps(correctMessage), results);
        if (results.size() != 1) {
            TEST_FAILED("Correct document not found");
        }
        message.push_back(incorrectMessage[0]);
        QuerySearch("add&" + GetAllKps(message), results);
        if (results.size() != 1) {
            TEST_FAILED("Additional correct document not found");
        }
        return true;
    }

    bool InitConfig() override {
        SetIndexerParams(DISK, 10, 1);
        return true;
    }
};

START_TEST_DEFINE(TestDEFFERED_DELETIONS)
public:
    bool Run() override {
        TVector<NRTYServer::TMessage> messages;
        GenerateInput(messages, 1, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed(), TAttrMap(), "abca");
        IndexMessages(messages, REALTIME, 1);
        IndexMessages(messages, DISK, 1);
        if (!CheckIndexSize(1, REALTIME, 30)) {
            TEST_FAILED("REALTIME indexer must contain 1 element");
        }

        ReopenIndexers();

        if (!CheckIndexSize(1, DISK, 30)) {
            TEST_FAILED("Document must appear in DISK indexer");
        }
        if (!CheckIndexSize(0, REALTIME, 30)) {
            TEST_FAILED("Disk indexer did't delete document in REALTIME");
        }
        return true;
    }
    bool InitConfig() override {
        SetIndexerParams(REALTIME, 10, 1);
        SetIndexerParams(DISK, 10, 1);
        return true;
    }
};
