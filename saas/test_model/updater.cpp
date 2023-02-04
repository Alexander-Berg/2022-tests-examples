#include "updater.h"

TUpdater::TUpdater(TModel& model, const TConfig& config)
    : Config(config)
    , Model(model)
    , Stoped(true)
{}

void TUpdater::Start() {
    TGuard<TMutex> G(Mutex);
    VERIFY_WITH_LOG(!Thread, "updater already started");
    Stoped = false;
    Thread = SystemThreadFactory()->Run(this);
}

void TUpdater::Stop() {
    TGuard<TMutex> G(Mutex);
    VERIFY_WITH_LOG(!!Thread, "updater already stoped");
    Stoped = true;
    Thread->Join();
    Thread.Reset(nullptr);
}

void TUpdater::DoExecute() {
    SandboxQueue.Reset(new TSimpleThreadPool);
    SandboxQueue->Start(Config.SecondaryThreads);
    while (!Stoped) {
        TRY
            ui64 nextUpdate = millisec() + Config.Period.MilliSeconds();
            Update();
            while (!Stoped && millisec() < nextUpdate)
                Sleep(TDuration::MilliSeconds(200));
        CATCH("While DoExecute")
    }
    SandboxQueue->Stop();
    SandboxQueue.Reset(nullptr);
}

void TUpdater::AddSecondaryTask(IObjectInQueue* task, bool immediately) {
    if (immediately)
        task->Process(nullptr);
    else if(!!SandboxQueue)
        SandboxQueue->SafeAdd(task);
}

void TUpdater::Update() {
    Model.UpdateChildren(Config.DbList, true);
    TReaderPtr<TModel::TChildren> bases = Model.GetChildren();
    TSimpleThreadPool queue;
    queue.Start(Config.MainThreads);
    for (TModel::TChildren::iterator i = bases->begin(); i != bases->end(); ++i) {
        TDbUpdater* updater = TDbUpdaterFactory::Construct(i->second->GetData().Type);
        VERIFY_WITH_LOG(updater, "Invalid usage");
        updater->SetParams(this, *i->second);
        queue.SafeAdd(updater);
    }
    DEBUG_LOG << "sandbox queue before: " << SandboxQueue->Size() << Endl;
    queue.Stop();
    DEBUG_LOG << "sandbox queue: " << SandboxQueue->Size() << Endl;
}
