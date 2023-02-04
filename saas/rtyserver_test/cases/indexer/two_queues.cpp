#include <saas/rtyserver_test/testerlib/rtyserver_test.h>

START_TEST_DEFINE(TestTwoQueues)
    bool Run() override{
        TVector<NRTYServer::TMessage> messages, messagesDouble;
        GenerateInput(messages, GetMaxDocuments(), NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
        for(TVector<NRTYServer::TMessage>::const_iterator i = messages.begin(), e = messages.end(); i != e; ++i)
            messagesDouble.push_back(*i);
        for(TVector<NRTYServer::TMessage>::const_reverse_iterator i = messages.rbegin(), e = messages.rend(); i != e; ++i) {
            messagesDouble.push_back(*i);
            if (SendIndexReply)
                messagesDouble.back().SetMessageId(IMessageGenerator::CreateMessageId());
        }
        IndexMessages(messagesDouble, REALTIME, 1);
        return true;
    }
public:
    bool InitConfig() override {
        SetIndexerParams(ALL, 10, 1);
        return true;
    }
};

