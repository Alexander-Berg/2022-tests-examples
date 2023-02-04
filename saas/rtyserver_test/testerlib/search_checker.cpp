#include "search_checker.h"
#include "rtyserver_test.h"

#include <saas/rtyserver/common/should_stop.h>

namespace {
    TString UrlNormalize(const TString& url) {
        TString result = url;
        UrlEscape(result);
        UrlEscape(result);
        THttpURL urlInfo;
        urlInfo.Parse(result);
        result = "";
        urlInfo.Print(result);
        return result;
    }
}

void TCallbackForCheckSearch::Start() {
    Active = true;
    StopFlag = false;
    QueueWatch.Start(1);
    if (!TestCase->GetIsPrefixed())
        QueueWatch.SafeAddAndOwn(THolder(new TWatcherTask(this, &StopFlag)));
    else
        QueueSearchers.Start(48);
}

void TCallbackForCheckSearch::Process(const NRTYServer::TMessage& message) {
    TString url = UrlNormalize(message.GetDocument().GetUrl());
    Messages[url] = message;
    MessageTime[url] = Now();
    AtomicIncrement(IndexCount);
    if (TestCase->GetIsPrefixed())
        QueueSearchers.SafeAddAndOwn(THolder(new TSearchChecker(this, message)));

}

bool TCallbackForCheckSearch::CheckAndPrintInfo(ui32 border, double part) const {
    if (!TestCase->GetIsPrefixed()) {
        ui32 maxDelta = 0;
//        ui32 countPred = 0;
//        ui32 countPredIndex = 0;
        for (auto&& item : ResultsCount) {
            int delta = (int)item.GetIndexCount() - (int)item.GetSearchCount();
            DEBUG_LOG << item.GetTime() << "/" << item.GetIndexCount() << "/" << item.GetSearchCount() << "/" << delta << Endl;
//            if (item.GetIndexCount() >= countPredIndex)
//                CHECK_TEST_LESSEQ(countPred, item.GetSearchCount());
//            countPred = item.GetSearchCount();
//            countPredIndex = item.GetIndexCount();
            if ((int)maxDelta < delta)
                maxDelta = delta;
        }
    }
    TMap<ui32, ui32> info;
    for (auto&& itItem : Results) {
        auto& item = itItem.second;
        info[item.GetDuration().Seconds()]++;
        CHECK_TEST_EQ((ui32)item.GetStatus(), (ui32)TCallbackForCheckSearch::ssFound);
    }

    ui32 resultInt = 0;
    ui32 resultChecker = 0;
    for (auto&& i : info) {
        if (i.first <= border)
            resultChecker += i.second;
        resultInt += i.second;
        DEBUG_LOG << "DSTAffxZ: " << i.first << "/" << 100.0 * resultInt / Results.size() << Endl;
    }
    DocsCount = resultInt;
    if (part > 1.0 * resultChecker / Results.size()) {
        ERROR_LOG << "Incorrect border checker: " << part << ">" << 1.0 * resultChecker / Results.size() << " for " << border << " seconds" << Endl;
    }
    return true;
}

bool TCallbackForCheckSearch::AddResult(const TString& url, const TInstant& time, ESearchStatus status) {
    TGuard<TMutex> g(Mutex);
    TString normUrl = UrlNormalize(url);
    auto mess = Messages.find(normUrl);
    auto messTime = MessageTime.find(normUrl);
    CHECK_WITH_LOG((mess != Messages.end()) == (messTime != MessageTime.end()));
    if (mess != Messages.end()) {
        AddResult(time - messTime->second, mess->second, status);
        return true;
    }
    return false;
}

void TCallbackForCheckSearch::AddResult(const TDuration& time, const NRTYServer::TMessage& message, ESearchStatus status) {
    TGuard<TMutex> g(Mutex);
    TString url = UrlNormalize(message.GetDocument().GetUrl());
    if (status != ssFound) {
        INFO_LOG << UrlNormalize(message.GetDocument().GetUrl()) << " status == " << (ui32)status << Endl;
    }
    if (Results.find(url) == Results.end()) {
        INFO_LOG << time << "/" << UrlNormalize(message.GetDocument().GetUrl()) << Endl;
        Results[url] = TCheckerResult(time, message, status);
    }
}

void TCallbackForCheckSearch::AddResult(const TInstant& time, ui32 indexCount, ui32 searchCount) {
    TGuard<TMutex> g(Mutex);
    INFO_LOG << time << "/" << indexCount << "/" << searchCount << Endl;
    ResultsCount.insert(TCountCheckerResult(time, indexCount, searchCount));
}

void TCallbackForCheckSearch::TSearchChecker::Process(void* /*ThreadSpecificResource*/) {
    TVector<TDocSearchInfo> result;
    try {
        TQuerySearchContext context;
        context.AttemptionsCount = Owner->AttemptsCount;
        context.ResultCountRequirement = 1;
        Owner->TestCase->QuerySearch("url:\"" + Message.GetDocument().GetUrl() + "\"&numdoc=100000&kps=" + ToString(Message.GetDocument().GetKeyPrefix()), result, context);
    } catch (...) {
        Owner->AddResult(Now() - TimeStart, Message, ssFailed);
    }
    if (result.size() == 1)
        Owner->AddResult(Now() - TimeStart, Message, ssFound);
    else if (result.size() == 0)
        Owner->AddResult(Now() - TimeStart, Message, ssNotFound);
    else
        Owner->AddResult(Now() - TimeStart, Message, ssTooMany);
}

void TCallbackForCheckSearch::TWatcherTask::Process(void* /*ThreadSpecificResource*/) {
    TInstant startTime = Now();
    ui32 predCount = 0;
    TQuerySearchContext context;
    context.AttemptionsCount = Owner->AttemptsCount;
    while (true) {
        TInstant time = Now();
        ui32 countSearch = 0;
        ui32 countIndex = 0;
        try {
            NJson::TJsonValue info = Owner->TestCase->GetInfoRequest()[0];
            auto map = info["indexes"].GetMap();
            countSearch = info["searchable_docs"].GetUInteger();
            for (auto&& i : map) {
                if (i.first.StartsWith("index_") && i.second["has_searcher"].GetUInteger() == 0)
                    countIndex += i.second["count_withnodel"].GetUInteger();
            }
            if (countSearch != predCount) {
                TVector<TDocSearchInfo> result;
                TInstant timeStart = Now();
                Owner->TestCase->QuerySearch("url:\"*\"&how=docid&numdoc=100000&relev=attr_limit%3D100000&haha=da&pron=earlyurls", result, context);
                for (auto&& i : result) {
                    if (!Owner->AddResult(i.GetUrl(), timeStart, ssFound))
                        Owner->AddResult("http://" + i.GetUrl(), timeStart, ssFound);
                }
            }
        } catch (...) {
            continue;
        }
        if (predCount != countSearch) {
            startTime = Now();
            predCount = countSearch;
        }
        Owner->AddResult(time, countIndex, countSearch);
        if (ShouldStop(StopFlag) && (countSearch == countIndex || Now() - startTime > TDuration::Seconds(30)))
            break;
        Sleep(TDuration::Seconds(1));
    }
}
