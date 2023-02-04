#include <saas/api/indexing_client/client.h>

#include <saas/rtyserver_test/testerlib/search_checker.h>
#include <saas/rtyserver_test/testerlib/rtyserver_test.h>


SERVICE_TEST_RTYSERVER_DEFINE(TestJsonRefIndexerProxy)

virtual bool RunProtocolWithCgi(const TString& protocolType = "", const TString& cgiParams = "") {
    const int CountMessages = 30;
    TVector<NRTYServer::TMessage> messagesToMem;
    GenerateInput(messagesToMem, CountMessages, NRTYServer::TMessage::MODIFY_DOCUMENT, GetIsPrefixed());
    TIndexerClient::TContext context;
    if (!protocolType.empty()) {
        context.ProtocolOverride = protocolType;
    }
    if (!cgiParams.empty()) {
        context.CgiRequest = cgiParams;
    }
    IndexMessages(messagesToMem, REALTIME, context);
    TSet<std::pair<ui64, TString>> deleted;
    CheckSearchResults(messagesToMem, deleted);
    return true;
}

};

START_TEST_DEFINE_PARENT(TestMemIndexingIndexerProxy, TestJsonRefIndexerProxy)
bool Run() override {
    return RunProtocolWithCgi("proto2json_ref");
}

};


START_TEST_DEFINE_PARENT(TestMemIndexingIndexerProxyInstantReply, TestJsonRefIndexerProxy)
bool Run() override {
    return RunProtocolWithCgi("proto2json_ref", "&instant_reply=yes");
}

};
