#include <saas/tools/test_model/viewer_client.h>
#include <saas/tools/test_model/utils.h>
#include <library/cpp/regex/pcre/regexp.h>

class TReportWorkTime : public TViewerClientReport {
    void Process(void* ThreadSpecificResource) override;
    static TViewerClientReportFactory::TRegistrator<TReportWorkTime> Registrator;
};

using namespace NJson;

TViewerClientReportFactory::TRegistrator<TReportWorkTime> TReportWorkTime::Registrator("/worktime");

namespace {
    struct TRevInfo {
        TRevInfo()
            : Time(0)
            , Count(0)
        {}

        TMap<TString, TTestExecution*> Tests;
        ui64 Time;
        ui64 Count;
    };

    typedef TMap<ui64, TRevInfo> TReport;
    ui64 GetUInteger(const NJson::TJsonValue& value) {
        if (value.IsInteger())
            return value.GetInteger();
        else if (value.IsString()) {
            try {
                return FromString<ui64>(value.GetString());
            } catch (...) {
                return -1;
            }
        };
        ythrow yexception() << "invalid type of rty_tests_work_time_seconds";
    }
}

void TReportWorkTime::Process(void* /*ThreadSpecificResource*/) {
    TCiString db = RD->CgiParam.Get("database");
    TReaderPtr<TModel::TChildren> dbs = Model->GetChildren();
    TModel::TChildren::const_iterator iDb = dbs->find(db);
    if (iDb == dbs->end()) {
        Errors << "Unknown database: " << db;
        Success = false;
        return;
    }
    size_t revCount = 10;
    if (RD->CgiParam.Find("count") != RD->CgiParam.end())
        revCount = FromString<size_t>(RD->CgiParam.Get("count"));

    TString regStr("TEST_");
    if (RD->CgiParam.Find("filter") != RD->CgiParam.end())
        regStr = RD->CgiParam.Get("filter");
    TRegExMatch filter(regStr.data());

    TReaderPtr<TDataBase::TChildren> tests = iDb->second->GetChildren();
    TReport report;
    for (TDataBase::TChildren::const_iterator iTest = tests->begin(); iTest != tests->end(); ++iTest) {
        if (!filter.Match(iTest->first.data()))
            continue;
        TReaderPtr<TTest::TChildren> executions = iTest->second->GetChildren();
        size_t count = revCount;
        for (TTest::TChildren::reverse_iterator iEx = executions->rbegin(); iEx != executions->rend() && count > 0; ++iEx, --count) {
            if (iEx->second->GetData().Status != TTestExecutionData::OK)
                continue;
            ui64 wtValue = Max<ui64>();
            if(iEx->second->GetData().TaskInfo.Has("ctx") && iEx->second->GetData().TaskInfo["ctx"].Has("rty_tests_work_time_seconds"))
                wtValue = GetUInteger(iEx->second->GetData().TaskInfo["ctx"]["rty_tests_work_time_seconds"]);
            else if (iEx->second->GetData().TaskInfo.Has("timestamp_start") && iEx->second->GetData().TaskInfo.Has("timestamp_finish"))
                wtValue = GetUInteger(iEx->second->GetData().TaskInfo["timestamp_finish"]) - GetUInteger(iEx->second->GetData().TaskInfo["timestamp_start"]);
            if (wtValue == Max<ui64>())
                continue;
            std::pair<TReport::iterator, bool> i = report.insert(TReport::value_type(iEx->first, TRevInfo()));
            TRevInfo& ri = i.first->second;
            ri.Tests[iTest->second->GetData().Name] = iEx->second.Get();
            ri.Time += wtValue;
            ++ri.Count;
        }
    }
    NJson::TJsonValue& rows = Result.InsertValue("rows", NJson::JSON_ARRAY);
    if (report.empty())
        return;
    bool writeTaskInfo = true;
    if (RD->CgiParam.Find("task_info") != RD->CgiParam.end())
        writeTaskInfo = FromString<bool>(RD->CgiParam.Get("task_info"));
    size_t count = revCount;
    ui64 prevSum = report.rbegin()->second.Time;
    double prevAv = report.rbegin()->second.Count ? (double)prevSum / report.rbegin()->second.Count : 0;

    for (TReport::reverse_iterator rev = report.rbegin(); rev != report.rend() && count > 0; ++rev, --count) {
        NJson::TJsonValue& revInfo = rows.AppendValue(NJson::JSON_MAP);
        WriteRevisionInfo(rev->second.Tests.begin()->second->GetData().Revision, revInfo.InsertValue("revision", NJson::JSON_MAP));
        const double av = rev->second.Count ? (double)rev->second.Time / rev->second.Count : 0;
        revInfo.InsertValue("summ_time", rev->second.Time);
        revInfo.InsertValue("average_time", av);
        revInfo.InsertValue("delta_summ_time", rev->second.Time - prevSum);
        revInfo.InsertValue("delta_average_time", av - prevAv);
        revInfo.InsertValue("count_tests", rev->second.Count);
        if (writeTaskInfo) {
            NJson::TJsonValue& tests = revInfo.InsertValue("tests", NJson::JSON_MAP);
            for (TMap<TString, TTestExecution*>::const_iterator test = rev->second.Tests.begin(); test != rev->second.Tests.end(); ++test)
                tests.InsertValue(test->first, test->second->GetData().TaskInfo);
        } else {
            NJson::TJsonValue& tests = revInfo.InsertValue("tests", NJson::JSON_ARRAY);
            for (TMap<TString, TTestExecution*>::const_iterator test = rev->second.Tests.begin(); test != rev->second.Tests.end(); ++test)
                tests.AppendValue(test->first);
        }
        prevAv = av;
        prevSum = rev->second.Time;
    }
}
