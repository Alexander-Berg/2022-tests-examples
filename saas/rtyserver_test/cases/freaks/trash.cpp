#include <saas/rtyserver_test/testerlib/rtyserver_test.h>
#include <saas/rtyserver_test/testerlib/test_indexer_client.h>
#include <saas/library/indexer_protocol/sender_neh.h>
#include <saas/library/indexer_protocol/protocol.h>
#include <library/cpp/neh/factory.h>

START_TEST_DEFINE(TestINDEX_TRASH)
using TRTYServerTestCase::Run;

bool Run(const TBackendProxyConfig::TIndexer& indexer) {
    for (ui32 i = 0; i < 100; ++i) {
        TNetworkAddress addr(indexer.Host, indexer.Port);
        TSocket s(addr);

        {
            TSocketOutput so(s);
            so << "some trash" << "\r\n";
            so << "some trash" << "\r\n\r\n";
        }
        {
            TSocketInput si(s);
            THttpInput input(&si);
            unsigned httpCode = ParseHttpRetCode(input.FirstLine());
            INFO_LOG << "Reply " << httpCode << Endl;
            CHECK_TEST_TRUE(httpCode == 501);
        }
    }
    return true;
}

bool Run() override {
    return Run(Controller->GetConfig().Indexer);
}
};

START_TEST_DEFINE(TestIndexGarbageReply)
bool Run() override {
    const TBackendProxyConfig::TIndexer* indexConfig = &Controller->GetConfig().Indexer;

    TIndexerClient client(indexConfig->Host
        , indexConfig->Port
        , TDuration::MilliSeconds(100)
        , TDuration::MilliSeconds(100)
        , indexConfig->Protocol
        , "tests"
        , indexConfig->PackSend
        , false
        , "");

    NRTYServer::ISender::TPtr sender = client.CreateSender();

    NNeh::TMessage nehMessage(NRTYServer::IndexingNehScheme + "://" + indexConfig->Host + ":" + ToString(indexConfig->Port) + "/", "complete and disgraceful gibberish");
    NRTYServer::TMessage message;
    message.SetMessageId(0);
    NNeh::TServiceStatRef stat;
    NRTYServer::IContext::TPtr ctxt =
        new NRTYServer::TNehContext(NNeh::ProtocolFactory()->Protocol(NRTYServer::IndexingNehProtocol)->ScheduleRequest(nehMessage, nullptr, stat), message);

    if (!ctxt)
        ythrow yexception() << "no context" << Endl;

    NRTYServer::TReply reply;
    if (!sender->Recv(ctxt, reply))
        ythrow yexception() << "cannot receive reply" << Endl;

    if (reply.GetStatus() != NRTYServer::TReply::INCORRECT_DOCUMENT)
        ythrow yexception() << "wrong reply: got " << NRTYServer::TReply_TRTYStatus_Name((NRTYServer::TReply_TRTYStatus)reply.GetStatus()) << Endl;
    return true;
}
};
