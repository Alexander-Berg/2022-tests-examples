#include <saas/protos/rtyserver.pb.h>
#include <saas/rtyserver_test/testerlib/globals.h>
#include <saas/rtyserver_test/testerlib/rtyserver_test.h>

using namespace NRTYServer;

SERVICE_TEST_RTYSERVER_DEFINE(IndexMinData)

    const int NOPREFIX = -1;
    int prefix = NOPREFIX;
    TVector<TMessage> GenerateMinMessage(NRTYServer::TMessage::TMessageType messType, int docPrefix, TString url="", bool doBody=false) {
        TVector<TMessage> messages(1);
        messages.back().SetMessageType(messType);
        messages.back().SetMessageId(IMessageGenerator::CreateMessageId());
        if (url.size()) {
            messages.back().MutableDocument()->SetUrl(url);
            messages.back().MutableDocument()->SetModificationTimestamp(Seconds());
            if (docPrefix != NOPREFIX) {
                messages.back().MutableDocument()->SetKeyPrefix(docPrefix);
            }
            if (doBody) {
                messages.back().MutableDocument()->SetBody("body");
            }
        }
        return messages;
    }

    int TestWithoutDocument(NRTYServer::TMessage::TMessageType messType, const TVector<TMessage>& messages) {
        bool mustBeGood = messType == NRTYServer::TMessage::REOPEN_INDEXERS;
        try{
            if (mustBeGood) {
                IndexMessageAsIs(messages);
            }
            else{
                MUST_BE_BROKEN(IndexMessageAsIs(messages));
            }
        } catch(...) {
            ERROR_LOG << "FAILCOUNT: Failed case: message_type " << NRTYServer::TMessage::TMessageType_Name(messType) << ", no document" << Endl;
            return 1;
        }
        return 0;
    }

    int TestDocWithoutBodyGood(NRTYServer::TMessage::TMessageType messType) {

        int failCount = 0;
        TVector<TMessage> messages(1);

        messages = GenerateMinMessage(messType, NOPREFIX);

        failCount += TestWithoutDocument(messType, messages);

        messages.back().MutableDocument()->SetUrl("doc-1");
        messages.back().MutableDocument()->SetModificationTimestamp(Seconds());
        messages.back().SetMessageId(IMessageGenerator::CreateMessageId());
        if (prefix != NOPREFIX)
            messages.back().MutableDocument()->SetKeyPrefix(prefix);
        try{
            IndexMessageAsIs(messages);
        } catch (...) {
            ERROR_LOG << "FAILCOUNT: Failed case: message_type " << NRTYServer::TMessage::TMessageType_Name(messType) << " +url -body -mimetype" << Endl;
            failCount += 1;
        }
        return failCount;
    }

    int TestBodyMessage(NRTYServer::TMessage::TMessageType messType) {
        VERIFY_WITH_LOG((messType == NRTYServer::TMessage::ADD_DOCUMENT || messType == NRTYServer::TMessage::MODIFY_DOCUMENT),\
            "Incorrect message type for this function");

        int failCount = TestDocWithoutBodyGood(messType);

        TVector<TMessage> messages = GenerateMinMessage(messType, prefix, "doc-1", true);

        MUST_BE_BROKEN(IndexMessageAsIs(messages));

        messages.back().SetMessageId(IMessageGenerator::CreateMessageId());
        messages.back().MutableDocument()->SetMimeType("text/plain");
        IndexMessageAsIs(messages);

        return failCount;
    }

    int TestMustDocWithoutBody(NRTYServer::TMessage::TMessageType messType) {
        VERIFY_WITH_LOG((messType == NRTYServer::TMessage::DEPRECATED__UPDATE_DOCUMENT || messType == NRTYServer::TMessage::DELETE_DOCUMENT \
            || messType == NRTYServer::TMessage::REOPEN_INDEXERS \
            || messType == NRTYServer::TMessage::SWITCH_PREFIX), \
            "Incorrect message type for this function");

        int failCount = TestDocWithoutBodyGood(messType);

        TVector<TMessage> messages = GenerateMinMessage(messType, prefix, "doc-1", true);
        try {
            MUST_BE_BROKEN(IndexMessageAsIs(messages));
        } catch (...) {
            ERROR_LOG << "FAILCOUNT: message " << NRTYServer::TMessage::TMessageType_Name(messType) << " with body succeeded" << Endl;
            failCount += 1;
        }
        return failCount;
    }

    int TestDeleteBodyQuery() {
        TVector<TMessage> messages = GenerateMinMessage(NRTYServer::TMessage::DELETE_DOCUMENT, prefix, "doc-1", true);
        messages.back().MutableDocument()->SetBody("query_del:url:\"*\"");
        messages.back().MutableDocument()->SetMimeType("text/plain");
        IndexMessageAsIs(messages);
        return 0;
    }
};

START_TEST_DEFINE_PARENT(IndexMinDataContent, IndexMinData)
    bool Run() override {
        int failCount = 0;
        prefix = (GetIsPrefixed() ? 2 : NOPREFIX);

        DEBUG_LOG << "Testing ADD_DOCUMENT" << Endl;
        failCount += TestBodyMessage(NRTYServer::TMessage::ADD_DOCUMENT);
        DEBUG_LOG << "Testing ADD_DOCUMENT end" << Endl;

        DEBUG_LOG << "Testing MODIFY_DOCUMENT" << Endl;
        failCount += TestBodyMessage(NRTYServer::TMessage::MODIFY_DOCUMENT);
        DEBUG_LOG << "Testing MODIFY_DOCUMENT end" << Endl;

        DEBUG_LOG << "Testing DEPRECATED__UPDATE_DOCUMENT" << Endl;
        failCount += TestMustDocWithoutBody(NRTYServer::TMessage::DEPRECATED__UPDATE_DOCUMENT);
        DEBUG_LOG << "Testing DEPRECATED__UPDATE_DOCUMENT end" << Endl;

        DEBUG_LOG << "Testing DELETE_DOCUMENT" << Endl;
        failCount += TestMustDocWithoutBody(NRTYServer::TMessage::DELETE_DOCUMENT);
        failCount += TestDeleteBodyQuery();
        DEBUG_LOG << "Testing DELETE_DOCUMENT end" << Endl;

        DEBUG_LOG << "fails count: " << failCount << Endl;
        CHECK_TEST_FAILED(failCount > 0, "some problems found, see FAILCOUNT entries");
        return true;
    }
};

START_TEST_DEFINE_PARENT(IndexMinDataNonContent, IndexMinData)

    bool Run() override {
        int failCount = 0;
        prefix = (GetIsPrefixed() ? 2 : NOPREFIX);

        DEBUG_LOG << "Testing REOPEN_INDEXERS" << Endl;
        failCount += TestMustDocWithoutBody(NRTYServer::TMessage::REOPEN_INDEXERS);
        DEBUG_LOG << "Testing REOPEN_INDEXERS end" << Endl;

        Sleep(TDuration::Seconds(5));

        if (prefix > 0) {
            DEBUG_LOG << "Testing SWITCH_PREFIX" << Endl;
            failCount += TestMustDocWithoutBody(NRTYServer::TMessage::SWITCH_PREFIX);
            DEBUG_LOG << "Testing SWITCH_PREFIX end" << Endl;
        }

        DEBUG_LOG << "fails count: " << failCount << Endl;
        CHECK_TEST_FAILED(failCount > 0, "some problems found, see FAILCOUNT entries");
        return true;
    }
};
