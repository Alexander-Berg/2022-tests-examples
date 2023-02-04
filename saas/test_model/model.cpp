#include "model.h"

void TTest::AfterUpdateChildren() {
    //LastUnstableRevision
    for (TChildren::iterator i = Children.lower_bound(LastUnstableRevision); i != Children.end(); ++i) {
        LastUnstableRevision = i->second->GetData().Revision.Revision;
        if (i->second->GetData().Status != TTestExecutionData::OK && i->second->GetData().Status != TTestExecutionData::FAILD && i->second->GetData().Status != TTestExecutionData::NOT_BUILD)
            break;
    }

    //Status, LastFinishedRevision, Result
    Status = UNKNOWN;
    LastFinished.Reset(nullptr);
    Result = 0;
    for (TChildren::reverse_iterator i = Children.rbegin(); i != Children.rend(); ++i) {
        if (i->second->GetData().Status == TTestExecutionData::OK) {
            Status = OK;
            LastFinished = i->second;
            TaskId = i->second->GetData().TaskId;
            Result = i->second->GetData().Result;
            break;
        }
        if (i->second->GetData().Status == TTestExecutionData::FAILD) {
            Status = TTest::FAILED;
            TaskId = i->second->GetData().TaskId;
            LastFinished = i->second;
            break;
        }
        if (i->second->GetData().Status == TTestExecutionData::NOT_BUILD) {
            TaskId = i->second->GetData().TaskId;
            LastFinished = i->second;
            break;
        }
    }

    //TaskId
    TaskId = 0;
    for (TChildren::reverse_iterator i = Children.rbegin(); i != Children.rend(); ++i) {
        if (i->second->GetData().Status == TTestExecutionData::OK || i->second->GetData().Status == TTestExecutionData::FAILD) {
            TaskId = i->second->GetData().TaskId;
            break;
        }
    }
    if (TaskId == 0 && !Children.empty())
        TaskId = Children.rbegin()->second->GetData().TaskId;


    //BrokenInRevision
    BrokenIn.Reset(nullptr);
    for (TChildren::reverse_iterator i = Children.rbegin(); Status == TTest::FAILED && i != Children.rend(); ++i) {
        if (i->second->GetData().Status == TTestExecutionData::FAILD)
            BrokenIn = i->second;
        else if (i->second->GetData().Status == TTestExecutionData::OK)
            break;
    }

    //Weather
    WeatherCountStarts = 0;
    WeatherCountOks = 0;
    for (TChildren::reverse_iterator i = Children.rbegin(); i != Children.rend(); ++i) {
        if (i->second->GetData().Status == TTestExecutionData::FAILD)
            ++WeatherCountStarts;
        else if (i->second->GetData().Status == TTestExecutionData::OK) {
            ++WeatherCountStarts;
            ++WeatherCountOks;
        }
        if (WeatherCountStarts >= GetData().MaxWeatherCount)
            break;
    }
}

bool TTest::NotFound() const {
    if (Status == OK)
        return false;
    for (TChildren::const_reverse_iterator i = Children.rbegin(); i != Children.rend(); ++i) {
        if (i->second->GetData().Status == TTestExecutionData::FAILD)
            return (i->second->GetData().FailInfo == "NotFound");
    }
    return false;
}

static TAtomicSharedPtr<TSet<TTest*> > EmptyTestSet(new TSet<TTest*>);

void TDataBase::AfterUpdateChildren() {
    TAtomicSharedPtr<TSet<TTest*> > builds(new TSet<TTest*>);
    TChildren::iterator endBuilds = Children.lower_bound("BUILZ"), iter = Children.lower_bound("BUILC");
    BuildStatus = TTest::UNKNOWN;
    BuildBrokenIn.Reset(nullptr);
    LastBuild.Reset(nullptr);
    for (; iter != endBuilds; ++iter) {
        builds->insert(iter->second.Get());
        iter->second->Builds = EmptyTestSet;
        if (iter->second->Status == TTest::FAILED)
            BuildStatus = TTest::FAILED;
        else if (iter->second->Status == TTest::OK && BuildStatus == TTest::UNKNOWN)
            BuildStatus = TTest::OK;
        if (!!iter->second->BrokenIn && (!BuildBrokenIn || BuildBrokenIn->GetData().Revision.Revision > iter->second->BrokenIn->GetData().Revision.Revision))
            BuildBrokenIn = iter->second->BrokenIn;
        if (!!iter->second->LastFinished && (!LastBuild || LastBuild->GetData().Revision.Revision > iter->second->LastFinished->GetData().Revision.Revision))
            LastBuild = iter->second->LastFinished;
    }
    LastFinished = LastBuild;
    for (iter = Children.begin(); iter != Children.end(); ++iter) {
        if (iter->first.StartsWith("BUILD"))
            continue;
        iter->second->Builds = builds;
        if (!!iter->second->LastFinished && (!LastFinished || LastFinished->GetData().Revision.Revision > iter->second->LastFinished->GetData().Revision.Revision))
            LastFinished = iter->second->LastFinished;
    }
}

template <>
void Out<TDbData::TType>(IOutputStream& o, TTypeTraits<TDbData::TType>::TFuncParam t) {
    Out<int>(o,t);
}

