#include "backend_proxy.h"
#include "globals.h"
#include "test_indexer_client.h"
#include <saas/protos/reply.pb.h>
#include <saas/protos/tracer.pb.h>
#include <saas/library/indexer_protocol/sender_abstract.h>
#include <saas/library/indexer_protocol/socket_sender.h>
#include <saas/library/indexer_protocol/sender_neh.h>
#include <saas/util/logging/exception_process.h>
#include <saas/util/queue.h>
#include <library/cpp/digest/md5/md5.h>
#include <library/cpp/string_utils/base64/base64.h>
#include <library/cpp/json/json_reader.h>

namespace {

    void SendUntilFinal(NRTYServer::TReply& reply, TIndexerClient& owner){
        int tries = 0;
        int id = reply.GetMessageId();
        while (reply.GetStatus() == NRTYServer::TReply::DATA_ACCEPTED && tries < 90000){

            NRTYServer::TRequestTracer rt;
            TStringInput si(reply.GetStatusMessage());
            TString data;
            NJson::TJsonValue jsonValue, jsonValueReply;

            TString asyncCode;
            DEBUG_LOG << "REPLY: " << reply.GetStatusMessage() << Endl;
            if (NJson::ReadJsonTree(&si, &jsonValue)) {
                TString strReply = jsonValue["reply"].GetStringRobust();
                TStringInput siReply(strReply);
                VERIFY_WITH_LOG(NJson::ReadJsonTree(&siReply, &jsonValueReply), "Incorrect reply: %s", reply.GetStatusMessage().data());
                asyncCode = jsonValueReply["async_code"].GetStringRobust();
            } else {
                asyncCode = reply.GetStatusMessage();
            }
            Base64Decode(asyncCode, data);
            VERIFY_WITH_LOG(rt.ParseFromString(data), "Incorrect async code: %s/%lu", data.data(), data.size());


            NRTYServer::ISender::TPtr sender = owner.CreateSender();
            NRTYServer::IContext::TPtr ctxt = sender->Send(asyncCode, "?trace=yes&ftests=yes");
            sender->Recv(ctxt, reply);
            DEBUG_LOG << "action=recv_reply;id=" << id << ";status=" << reply.GetStatus() << Endl;
            Sleep(TDuration::MilliSeconds(10));
            ++tries;
        }
        CHECK_WITH_LOG(reply.GetStatus() != NRTYServer::TReply::DATA_ACCEPTED);
        reply.SetMessageId(id);
        return;
    }

    class TSendReceiveJob : public IObjectInQueue {
        const NRTYServer::TMessage& Message;
        TIndexerClient& Owner;
        const TDuration& InterMessageTimeout;
        bool DoWaitReply;
        TString Request;
        ui64 StartTime;
    public:
        TSendReceiveJob(const NRTYServer::TMessage& message, TIndexerClient& owner, const TDuration& interMessageTimeout, bool waitReply, TString request="")
            : Message(message)
            , Owner(owner)
            , InterMessageTimeout(interMessageTimeout)
            , DoWaitReply(waitReply)
            , Request(request)
            , StartTime(millisec())
        {}

        void TaskFail(const TString& msg) {
            TGuard<TMutex> g(Owner.GetProcessMutex());
            Owner.GetInProcess()->insert(TIndexerClient::TMessagesMap::value_type(Message.GetMessageId(), &Message));
            ERROR_LOG << msg << ";" << NRTYServer::TInfoContext::BuildId(Message) << Endl;

        }

        void Process(void* /*ThreadSpecificResource*/) override {
            THolder<TSendReceiveJob> suicide(this);
            TRY
                NRTYServer::ISender::TPtr sender = Owner.CreateSender();
                const TString id = NRTYServer::TInfoContext::BuildId(Message);

                DEBUG_LOG << "action=send_start;" << id << Endl;

                NRTYServer::IContext::TPtr ctxt = sender->Send(Message, Request);

                if (DoWaitReply && Message.HasMessageId() && !!ctxt)  {
                    NRTYServer::TReply reply;
                    reply.SetStatus(1);
                    reply.SetStatusMessage("timeout");

                    if (sender->Recv(ctxt, reply)) {
                        CHECK_WITH_LOG(reply.GetMessageId() == Message.GetMessageId());
                        SendUntilFinal(reply, Owner);
                        CHECK_WITH_LOG(reply.GetMessageId() == Message.GetMessageId());
                        Owner.AddReply(reply);
                        if (reply.GetStatus() == NRTYServer::TReply::OK) {
                            DEBUG_LOG << "action=recv_reply;id=" << reply.GetMessageId() << ";status=OK" << Endl;
                            if (Owner.GetCallback())
                                Owner.GetCallback()->Process(Message);
                        } else {
                            TaskFail("status_reply=" + ToString(reply.GetStatus()));
                        }
                    } else {
                        DEBUG_LOG << "action=recv_reply;id = " << Message.GetMessageId() << ";status_reply=fail;" << Endl;
                    }
                }
                Sleep(InterMessageTimeout);
                return;
            CATCH("")
            TaskFail("status_reply=fail");
            Sleep(InterMessageTimeout);
        }
    };


    class TSendJob : public IObjectInQueue {
        TIndexerClient& Owner;
        const NRTYServer::TMessage* Message;
        TString Request;

    public:
        TSendJob(TIndexerClient& owner, const NRTYServer::TMessage* message, TString request="")
            : Owner(owner)
            , Message(message)
            , Request(request)
        {}

        void Process(void* ThreadSpecificResource) override {
            THolder<TSendJob> suicide(this);
            NRTYServer::ISender* sender = (NRTYServer::ISender*)ThreadSpecificResource;
            TString id = NRTYServer::TInfoContext::BuildId(*Message);
            DEBUG_LOG << "action=send_doc;" << id << Endl;
            TRY
                NRTYServer::IContext::TPtr ctxt = sender->Send(*Message, Request);
                if (Message->HasMessageId() && !!ctxt) {
                    Owner.GetHandles().Enqueue(ctxt);
                    TGuard<TMutex> g(Owner.GetProcessMutex());
                    if (!Owner.GetInProcess()->insert(TIndexerClient::TMessagesMap::value_type(Message->GetMessageId(), Message)).second)
                        ERROR_LOG << "MessageId is not unique: " << Message->GetMessageId();
                }
            CATCH("while write document" + id)
        }
    };

    class TReceiveJob : public IObjectInQueue {
        TIndexerClient& Owner;

    public:
        TReceiveJob(TIndexerClient& owner)
            : Owner(owner)
        {}

        void Process(void* ThreadSpecificResource) override {
            THolder<TReceiveJob> suicide(this);
            NRTYServer::TReply reply;
            NRTYServer::IContext::TPtr ctxt;
            NRTYServer::ISender* sender = (NRTYServer::ISender*)ThreadSpecificResource;
            while (!Owner.GetHandles().IsEmpty()) {
                if (Owner.GetHandles().Dequeue(&ctxt)) {
                    NRTYServer::TInfoContext* infoContext = dynamic_cast<NRTYServer::TInfoContext*>(ctxt.Get());
                    DEBUG_LOG << "action=recv_reply_start;" << infoContext->BuildId() << Endl;
                    if (sender->Recv(ctxt, reply)) {
                        VERIFY_WITH_LOG(infoContext->GetId() == reply.GetMessageId(), "Incorrect reply!!!!");
                        Owner.AddReply(reply);
                        if (reply.GetStatus() == NRTYServer::TReply::OK || reply.GetStatus() == NRTYServer::TReply::DATA_ACCEPTED) {
                            DEBUG_LOG << "action=recv_reply;status=" << reply.GetStatus() << ";" << infoContext->BuildId() << Endl;
                            TGuard<TMutex> g(Owner.GetProcessMutex());
                            TIndexerClient::TMessagesMap::iterator i = Owner.GetInProcess()->find(reply.GetMessageId());
                            if (i != Owner.GetInProcess()->end()) {
                                if (Owner.GetCallback())
                                    Owner.GetCallback()->Process(*i->second);
                                Owner.GetInProcess()->erase(i);
                            } else
                                ERROR_LOG << "Unknown message " << reply.GetMessageId() << Endl;
                        } else {
                            WARNING_LOG << "action=recv_reply;status=" << reply.GetStatus() << ";" << infoContext->BuildId() << Endl;
                        }
                    } else {
                        ERROR_LOG << "action=recv_failed;" << infoContext->BuildId() << Endl;
                    }
                }
            }
        }
    };
};

NRTYServer::ISender* TIndexerClient::CreateSender() {
    if (!!ProtocolType) {
        NRTYServer::TSocketSender* sender = new NRTYServer::TSocketSender(Host, Port, TDuration::Seconds(1000), InterByteTimeout, ProtocolType, Service);
        if (!sender->Connect())
            ythrow yexception() << "cannot connect to " << Host << ":" << Port;
        return sender;
    } else {
        return new NRTYServer::TNehSender(Host, Port);
    }
}

TMutex SocketsMutex;

void* TIndexerClient::CreateThreadSpecificResource() {
    TGuard<TMutex> g(SocketsMutex);
    NRTYServer::ISender* sockets = CreateSender();
    SendersList.push_back(sockets);
    return sockets;
}

void TIndexerClient::DestroyThreadSpecificResource(void* /*resource*/) {
}

void TIndexerClient::AddReply(const NRTYServer::TReply& reply) {
    TGuard<TMutex> g(MutexReplies);
    Replies.push_back(reply);
}

TIndexerClient::TIndexerClient(const TString& host, ui16 port, const TDuration& timeout, const TDuration& interByteTimeout, const TString& protocolType, const TString& service, bool packSend, bool ignoreErrors, TString request, ICallback* callback)
    : Host(host)
    , Port(port)
    , Timeout(timeout)
    , InterByteTimeout(interByteTimeout)
    , ProtocolType(protocolType)
    , Service(service)
    , PackSend(packSend)
    , IgnoreErrors(ignoreErrors)
    , Request(request)
    , Callback(callback)
{
}

void TIndexerClient::Run(const TVector<NRTYServer::TMessage>& messages, size_t countThreads, const TDuration& interMessageTimeout, bool doWaitReply) {
    InProcess = &MessagesMap[0];
    TMessagesMap* inputMessages = &MessagesMap[1];
    inputMessages->clear();
    for (size_t i = 0; i < messages.size(); ++i) {
        const NRTYServer::TMessage& message = messages[i];
        if (!inputMessages->insert(TMessagesMap::value_type(message.HasMessageId() ? message.GetMessageId(): i, &message)).second)
            ythrow yexception() << "MessageId is not unique: " << message.GetMessageId();
    }

    int tries = Timeout.MilliSeconds() ? 1 : 2;
    ui64 startTime = millisec();
    do {
        InProcess->clear();
        if (PackSend)
            PackRun(inputMessages, countThreads, interMessageTimeout, doWaitReply, Request);
        else
            UnpackRun(inputMessages, countThreads, interMessageTimeout, doWaitReply, Request);
        swap(InProcess, inputMessages);
        if (!inputMessages->empty())
            sleep(1);
        --tries;
    } while (!inputMessages->empty() && ((millisec() - startTime < Timeout.MilliSeconds()) || (tries > 0)));

    if (!inputMessages->empty() && !IgnoreErrors) {
        yexception ex;
        ex << "Some messages not indexed:" << Endl;
        for (TMessagesMap::iterator i = inputMessages->begin(), e = inputMessages->end(); i != e; ++i)
            ex << i->first << Endl;
        ythrow ex;
    }

    if (GlobalOptions().GetUsingDistributor()) {
        for (int i = 0; i < 10; ++i) {
            if (GlobalOptions().GetBackendProxy()->GetMetric("Indexer_DocumentsAdded", TBackendProxy::TBackendSet(0))) {
                break;
            }
            Sleep(TDuration::Seconds(10));
        }
        GlobalOptions().GetBackendProxy()->WaitDistributorExhausted();
    }
}

void TIndexerClient::UnpackRun(TMessagesMap* messages, size_t countThreads, const TDuration& interMessageTimeout, bool doWaitReply, TString request) {
    TRTYMtpQueue queue;
    queue.Start(countThreads);
    for (TMessagesMap::const_iterator i = messages->begin(); i != messages->end(); ++i) {
        queue.SafeAdd(new TSendReceiveJob(*i->second, *this, interMessageTimeout, doWaitReply, request));
    }
    queue.Stop();
}

void TIndexerClient::PackRun(TMessagesMap* messages, size_t countThreads, const TDuration& interMessageTimeout, bool doWaitReply, TString request) {
    TThreadPoolBinder<TThreadPool, TIndexerClient> sendQueue(this);
    TMessagesMap::iterator i = messages->begin();
    while (i != messages->end()) {
        sendQueue.Start(countThreads);
        DEBUG_LOG << "send messages pack..." << Endl;
        ui32 index = 0;
        for (; i != messages->end() && index < 512; ++i, ++index) {
            sendQueue.SafeAdd(new TSendJob(*this, i->second, request));
            Sleep(interMessageTimeout);
        }
        sendQueue.Stop();
        DEBUG_LOG << "send messages pack...OK" << Endl;
        if (doWaitReply) {
            DEBUG_LOG << "receive messages pack..." << Endl;
            sendQueue.Start(countThreads);
            for (TSendersList::iterator i = SendersList.begin(), e = SendersList.end(); i != e; ++i) {
                sendQueue.SafeAdd(new TReceiveJob(*this));
            }
            sendQueue.Stop();
            DEBUG_LOG << "recive messages pack...OK" << Endl;
        }
    }
    SendersList.clear();
}
