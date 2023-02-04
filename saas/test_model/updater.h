#pragma once

#include "model.h"
#include <library/cpp/object_factory/object_factory.h>
#include <util/thread/pool.h>

class TUpdater: IThreadFactory::IThreadAble {
public:
    struct TConfig {
        struct TCommon {
            TString Host;
            ui16 Port;
        };

        struct TTE : public TCommon {
            ui64 WeatherCount;
            ui64 RevisionsCount;
        };

        struct TSandbox : public TCommon {
        };

        struct TLunapark : public TCommon {
        };

        struct TAqua : public TCommon {
        };

        struct TRobot : public TCommon {
        };

    public:
        TTE TE;
        TSandbox Sandbox;
        TLunapark Lunapark;
        TAqua Aqua;
        TRobot Robot;
        TSvnInfo::TConfig Svn;

        ui16 MainThreads;
        ui16 SecondaryThreads;
        TDuration Period;
        TVector<TDbData> DbList;
    };

public:
    TUpdater(TModel& model, const TConfig& config);
    void Start();
    void Stop();
    void AddSecondaryTask(IObjectInQueue* task, bool immediately);

    const TConfig& Config;

private:
    virtual void DoExecute();
    void Update();
    TModel& Model;
    bool Stoped;
    TAutoPtr<IThreadFactory::IThread> Thread;
    TMutex Mutex;
    THolder<IThreadPool> SandboxQueue;
};

class TDbUpdater : public IObjectInQueue {
public:
    TDbUpdater()
        : Owner(nullptr)
        , Db(nullptr)
    {}

    void SetParams(TUpdater* owner, TDataBase& db) {
        Owner = owner;
        Db = &db;
    }

protected:
    TUpdater* Owner;
    TDataBase* Db;
};

typedef NObjectFactory::TObjectFactory<TDbUpdater, TDbData::TType> TDbUpdaterFactory;
