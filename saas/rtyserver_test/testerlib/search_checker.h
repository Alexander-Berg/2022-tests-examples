#pragma once
#include "test_indexer_client.h"
#include <saas/protos/rtyserver.pb.h>
#include <library/cpp/logger/global/global.h>
#include <util/datetime/base.h>
#include <util/thread/pool.h>
#include <util/generic/set.h>
#include <util/generic/map.h>

class TRTYServerTestCase;

class TCallbackForCheckSearch: public TIndexerClient::ICallback {
public:

    enum ESearchStatus { ssFound, ssNotFound, ssFailed, ssTooMany };

    class TCheckerResult {
    private:
        NRTYServer::TMessage Message;
        ESearchStatus Status;
        TDuration Duration;
    public:

        TCheckerResult(const TDuration& duration, const NRTYServer::TMessage& message, ESearchStatus status) {
            Duration = duration;
            Message = message;
            Status = status;
        }

        TCheckerResult() {
        }

        const TDuration& GetDuration() const {
            return Duration;
        }

        ESearchStatus GetStatus() const {
            return Status;
        }

        const NRTYServer::TMessage& GetMessage() const {
            return Message;
        }

        bool operator<(const TCheckerResult& item) const {
            if (Status == item.Status) {
                if (Duration == item.Duration) {
                    return Message.GetDocument().GetUrl() < item.Message.GetDocument().GetUrl();
                } else
                    return Duration < item.Duration;
            } else {
                return Status < item.Status;
            }
        }
    };

    class TCountCheckerResult {
    private:
        TInstant Time;
        ui32 IndexCount;
        ui32 SearchCount;
    public:

        TCountCheckerResult(const TInstant& time, ui32 indexCount, ui32 searchCount) {
            Time = time;
            IndexCount = indexCount;
            SearchCount = searchCount;
        }

        const TInstant& GetTime() const {
            return Time;
        }

        ui32 GetIndexCount() const {
            return IndexCount;
        }

        ui32 GetSearchCount() const {
            return SearchCount;
        }

        bool operator<(const TCountCheckerResult& item) const {
            if (Time == item.Time) {
                if (GetIndexCount() == item.GetIndexCount()) {
                    return GetSearchCount() < item.GetSearchCount();
                } else
                    return GetIndexCount() < item.GetIndexCount();
            } else {
                return Time < item.Time;
            }
        }
    };

private:
    TThreadPool QueueSearchers;
    TThreadPool QueueWatch;
    TRTYServerTestCase* TestCase;
    ui32 AttemptsCount;

    TMutex Mutex;
    TMap<TString, TCheckerResult> Results;
    TSet<TCountCheckerResult> ResultsCount;
    TAtomic IndexCount;
    TInstant PredCheckTime;
    TMap<TString, TInstant> MessageTime;
    TMap<TString, NRTYServer::TMessage> Messages;

    class TWatcherTask: public IObjectInQueue {
    private:
        TCallbackForCheckSearch* Owner;
        const std::atomic<bool>* StopFlag;
    public:

        TWatcherTask(TCallbackForCheckSearch* owner, const std::atomic<bool>* stopFlag) {
            Owner = owner;
            StopFlag = stopFlag;
        }

        virtual void Process(void* /*ThreadSpecificResource*/);
    };

    class TSearchChecker: public IObjectInQueue {
    private:
        NRTYServer::TMessage Message;
        TCallbackForCheckSearch* Owner;
        TInstant TimeStart;
    public:

        TSearchChecker(TCallbackForCheckSearch* owner, const NRTYServer::TMessage& message) {
            Message = message;
            Owner = owner;
            TimeStart = Now();
        }

        virtual void Process(void* /*ThreadSpecificResource*/);
    };

    bool AddResult(const TString& url, const TInstant& time, ESearchStatus status);

    void AddResult(const TDuration& time, const NRTYServer::TMessage& message, ESearchStatus status);

    void AddResult(const TInstant& time, ui32 indexCount, ui32 searchCount);

    ui32 GetIndexCount() const {
        return IndexCount;
    }

    std::atomic<bool> Active;
    std::atomic<bool> StopFlag;
    mutable ui32 DocsCount;
public:

    TCallbackForCheckSearch(TRTYServerTestCase* testCase, ui32 attemptsCount) {
        TestCase = testCase;
        AttemptsCount = attemptsCount;
        IndexCount = 0;
        PredCheckTime = Now();
        Active = false;
        DocsCount = 0;
    }

    ~TCallbackForCheckSearch() {
        CHECK_WITH_LOG(!Active);
    }

    ui32 GetDocsCount() const {
        return DocsCount;
    }

    void Start();

    void Stop() {
        StopFlag = true;
        QueueWatch.Stop();
        QueueSearchers.Stop();
        Active = false;
    }

    virtual void Process(const NRTYServer::TMessage& message);

    bool CheckAndPrintInfo(ui32 border, double part) const;
};
