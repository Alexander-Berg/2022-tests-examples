#include <saas/tools/test_model/viewer_client.h>
#include <saas/tools/test_model/utils.h>
#include <library/cpp/charset/ci_string.h>
#include <util/string/vector.h>

class TReportTests : public TViewerClientReport {
    void Process(void* ThreadSpecificResource) override;
    static TViewerClientReportFactory::TRegistrator<TReportTests> Registrator;
};

using namespace NJson;

TViewerClientReportFactory::TRegistrator<TReportTests> TReportTests::Registrator("/tests");

namespace {
    class TTestStatusToString: public TMap<TTest::TStatus, TString> {
    public:
        TTestStatusToString() {
            insert(value_type(TTest::OK, "OK"));
            insert(value_type(TTest::FAILED, "Failed"));
            insert(value_type(TTest::UNKNOWN, "Unknown"));
        }
        TString Get(TTest::TStatus status) const {
            const_iterator i = find(status);
            return i == end() ? "ERROR" : i->second;
        }
    };
}

void TReportTests::Process(void* /*ThreadSpecificResource*/) {
    TCiString db = RD->CgiParam.Get("database");
    TReaderPtr<TModel::TChildren> dbs = Model->GetChildren();
    TModel::TChildren::const_iterator iDb = dbs->find(db);
    if (iDb == dbs->end()) {
        Errors << "Unknown database: " << db;
        Success = false;
        return;
    }

    TSet<TCiString> statusFilter;
    TString statusFilterCgi = RD->CgiParam.Get("status");
    if (!!statusFilterCgi) {
        TVector<TString> sfV = SplitString(statusFilterCgi, ",");
        for (TVector<TString>::const_iterator i = sfV.begin(); i != sfV.end(); ++i)
            statusFilter.insert(i->data());
    }
    bool writeTaskInfo = true;
    if (RD->CgiParam.Find("task_info") != RD->CgiParam.end())
        writeTaskInfo = FromString<bool>(RD->CgiParam.Get("task_info"));

    Result.SetType(JSON_MAP);
    {
        TJsonValue& dbInfo = Result.InsertValue("database_info", JSON_MAP);
        dbInfo.InsertValue("is_started", ToString(iDb->second->GetData().Status));
        TJsonValue& build = Result.InsertValue("build", JSON_MAP);
        build.InsertValue("status", Singleton<TTestStatusToString>()->Get(iDb->second->BuildStatus));
        if (!!iDb->second->LastBuild)
            WriteRevisionInfo("last_complete", *iDb->second->LastBuild, build, writeTaskInfo);
        if (!!iDb->second->BuildBrokenIn)
            WriteRevisionInfo("broken_in", *iDb->second->BuildBrokenIn, build, writeTaskInfo);
    }
    if (!!iDb->second->LastFinished)
        WriteRevisionInfo("last_complete", *iDb->second->LastFinished, Result, writeTaskInfo);
    TJsonValue& rows = Result.InsertValue("rows", JSON_ARRAY);
    TReaderPtr<TDataBase::TChildren> tests = iDb->second->GetChildren();
    for (TDataBase::TChildren::const_iterator i = tests->begin(), e = tests->end(); i != e; ++i) {
        TCiString status = Singleton<TTestStatusToString>()->Get(i->second->Status);
        if (!statusFilter.empty() && statusFilter.find(status) == statusFilter.end())
            continue;
        if (i->second->TaskId == 0)
            continue;
        TJsonValue& test = rows.AppendValue(JSON_MAP);
        test.InsertValue("name", i->second->GetData().Name);
        test.InsertValue("flaky_pattern", i->second->GetData().HistoryPattern);
        test.InsertValue("last_flaky_saved", i->second->GetData().HistoryLastRevision);
        test.InsertValue("status", status);
        test.InsertValue("Result", i->second->Result);
        test.InsertValue("task_id", i->second->TaskId);
        test.InsertValue("is_executed", i->second->GetData().IsExecuted);
        test.InsertValue("not_found", i->second->NotFound());
        if (!!i->second->BrokenIn)
            WriteRevisionInfo("broken_in", *i->second->BrokenIn, test, writeTaskInfo);
        if (!!i->second->LastFinished){
            WriteRevisionInfo("last_complete", *i->second->LastFinished, test, writeTaskInfo);
            if(i->second->LastFinished->GetData().FailInfo != "")
                test.InsertValue("fail_info", i->second->LastFinished->GetData().FailInfo);
            }
        TJsonValue& weather = test.InsertValue("weather", JSON_MAP);
        weather.InsertValue("count_oks", i->second->WeatherCountOks);
        weather.InsertValue("count_starts", i->second->WeatherCountStarts);
    }
}
