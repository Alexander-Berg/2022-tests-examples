#pragma once

#include <saas/library/indexer_protocol/sender_abstract.h>
#include <saas/protos/rtyserver.pb.h>

#include <library/cpp/neh/neh.h>

#include <util/datetime/base.h>
#include <util/generic/list.h>
#include <util/generic/map.h>
#include <util/generic/ptr.h>
#include <util/generic/string.h>
#include <util/generic/vector.h>
#include <util/network/socket.h>
#include <util/system/mutex.h>
#include <util/thread/lfqueue.h>
#include <util/string/cast.h>

class TIndexerClient {
private:
    TLockFreeQueue<NRTYServer::IContext::TPtr> Handles;
public:

    class ICallback {
    public:
        virtual ~ICallback() {}
        virtual void Process(const NRTYServer::TMessage& message) = 0;
    };

    class TContext {
    public:
        int CopiesNumber = 1;
        ui64 WaitResponseMilliseconds = 0;
        bool DoWaitIndexing = true;
        bool DoWaitReply = true;
        TDuration InterByteTimeout = TDuration();
        TDuration InterMessageTimeout = TDuration();
        size_t CountThreads = 1;
        TString Service = "tests";
        int PackSendMark = 0;
        bool IgnoreErrors = false;
        TString CgiRequest = "";
        TIndexerClient::ICallback* Callback = nullptr;
        TString ProtocolOverride = "";
    };

    typedef TMap<i64, const NRTYServer::TMessage*> TMessagesMap;

    TIndexerClient(const TString& host, ui16 port, const TDuration& timeout, const TDuration& interByteTimeout, const TString& protocolType, const TString& service, bool packSend, bool ignoreErrors, TString request, ICallback* callback = nullptr);
    void Run(const TVector<NRTYServer::TMessage>& messages, size_t countThreads, const TDuration& interMessageTimeout, bool doWaitReply);

    void* CreateThreadSpecificResource();
    void DestroyThreadSpecificResource(void* resource);
    const TVector<NRTYServer::TReply>& GetReplies() {
        return Replies;
    }

    ICallback* GetCallback() {
        return Callback;
    }

    void AddReply(const NRTYServer::TReply& reply);

    TMutex& GetProcessMutex() {
        return ProcessMutex;
    }

    TMessagesMap* GetInProcess() {
        return InProcess;
    }

    TLockFreeQueue<NRTYServer::IContext::TPtr>& GetHandles() {
        return Handles;
    }

    NRTYServer::ISender* CreateSender();

private:
    typedef TList<NRTYServer::ISender::TPtr> TSendersList;

    void PackRun(TMessagesMap* messages, size_t countThreads, const TDuration& interMessageTimeout, bool doWaitReply, TString request);
    void UnpackRun(TMessagesMap* messages, size_t countThreads, const TDuration& interMessageTimeout, bool doWaitReply, TString request);
private:
    TMutex ProcessMutex;
    TMutex MutexReplies;
    TVector<NRTYServer::TReply> Replies;
    TMessagesMap MessagesMap[2];
    TMessagesMap* InProcess;
    TSendersList SendersList;
    const TString Host;
    ui16 Port;
    const TDuration Timeout;
    const TDuration InterByteTimeout;
    const TString ProtocolType;
    TString Service;
    bool PackSend;
    bool IgnoreErrors;
    TString Request;
    ICallback* Callback;
};
