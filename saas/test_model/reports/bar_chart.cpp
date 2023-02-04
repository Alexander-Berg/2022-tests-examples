#include <saas/tools/test_model/viewer_client.h>
#include <saas/tools/test_model/utils.h>
#include <library/cpp/regex/pcre/regexp.h>

class TReportWorkTimeBarChart : public TViewerClientReport {
    void Process(void* ThreadSpecificResource) override;
    static TViewerClientReportFactory::TRegistrator<TReportWorkTimeBarChart> Registrator;
};

namespace {
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

using namespace NJson;

TViewerClientReportFactory::TRegistrator<TReportWorkTimeBarChart> TReportWorkTimeBarChart::Registrator("/worktime/barchart");

void TReportWorkTimeBarChart::Process(void* /*ThreadSpecificResource*/) {
    TCiString db = RD->CgiParam.Get("database");
    TReaderPtr<TModel::TChildren> dbs = Model->GetChildren();
    TModel::TChildren::const_iterator iDb = dbs->find(db);
    if (iDb == dbs->end()) {
        Errors << "Unknown database: " << db;
        Success = false;
        return;
    }
    if (RD->CgiParam.Find("revision") == RD->CgiParam.end()) {
        Errors << "Revision must be set";
        Success = false;
        return;
    }
    const ui64 revision = FromString<ui64>(RD->CgiParam.Get("revision"));
    const TRevisionInfo* revInfo = nullptr;

    TString regStr("TEST_");
    if (RD->CgiParam.Find("filter") != RD->CgiParam.end())
        regStr = RD->CgiParam.Get("filter");
    TRegExMatch filter(regStr.data());

    TReaderPtr<TDataBase::TChildren> tests = iDb->second->GetChildren();
    NJson::TJsonValue& jvTests = Result.InsertValue("tests", NJson::JSON_MAP);
    for (TDataBase::TChildren::const_iterator iTest = tests->begin(); iTest != tests->end(); ++iTest) {
        if (!filter.Match(iTest->first.data()))
            continue;
        TReaderPtr<TTest::TChildren> executions = iTest->second->GetChildren();
        TTest::TChildren::const_iterator iEx = executions->find(revision);
        if (iEx == executions->end())
            continue;
        if (iEx->second->GetData().Status != TTestExecutionData::OK)
            continue;
        ui64 wtValue = Max<ui64>();
        if(iEx->second->GetData().TaskInfo.Has("ctx") && iEx->second->GetData().TaskInfo["ctx"].Has("rty_tests_work_time_seconds"))
            wtValue = GetUInteger(iEx->second->GetData().TaskInfo["ctx"]["rty_tests_work_time_seconds"]);
        else if (iEx->second->GetData().TaskInfo.Has("timestamp_start") && iEx->second->GetData().TaskInfo.Has("timestamp_finish"))
            wtValue = GetUInteger(iEx->second->GetData().TaskInfo["timestamp_finish"]) - GetUInteger(iEx->second->GetData().TaskInfo["timestamp_start"]);
        if (wtValue == Max<ui64>())
            continue;
        jvTests.InsertValue(iTest->second->GetData().Name, wtValue);
        revInfo = &iEx->second->GetData().Revision;
    }

    if (revInfo)
        WriteRevisionInfo(*revInfo, Result.InsertValue("revision", NJson::JSON_MAP));
}
