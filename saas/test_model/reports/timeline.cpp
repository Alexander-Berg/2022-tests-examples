#include <saas/tools/test_model/viewer_client.h>
#include <saas/tools/test_model/utils.h>
#include <library/cpp/regex/pcre/regexp.h>

class TReportTimeline : public TViewerClientReport {
    void Process(void* ThreadSpecificResource) override;
    static TViewerClientReportFactory::TRegistrator<TReportTimeline> Registrator;
};

using namespace NJson;

TViewerClientReportFactory::TRegistrator<TReportTimeline> TReportTimeline::Registrator("/timeline");

namespace {
    typedef TMap<TCiString, NJson::TJsonValue> TTestList;
    struct TTestsInfo {
        TTestList listFailed;
        TTestList listFinished;
        TTestList listNotFinished;
    };

    struct TRevInfo {
        typedef TMap<TString, TTestsInfo> TLists;
        TRevisionInfo Revision;
        TLists Lists;
    };

    typedef TMap<ui64, TRevInfo> TReport;

    void WriteTestList(const TTestList& testList, TJsonValue& Result, bool writeInfo) {
        for (TTestList::const_iterator i = testList.begin(); i != testList.end(); ++i) {
            if (writeInfo)
                Result.InsertValue(i->first, i->second);
            else
                Result.InsertValue(i->first, i->second["id"]);
        }
    }

    void WriteTestInfo(const TTestsInfo& testInfo, TJsonValue& Result, bool writeInfo) {
        WriteTestList(testInfo.listFinished, Result.InsertValue("finishedTests", NJson::JSON_MAP), writeInfo);
        WriteTestList(testInfo.listNotFinished, Result.InsertValue("notFinishedTests", NJson::JSON_MAP), writeInfo);
        WriteTestList(testInfo.listFailed, Result.InsertValue("failedTests", NJson::JSON_MAP), writeInfo);
        Result.InsertValue("numbersOfTests", testInfo.listFinished.size() + testInfo.listNotFinished.size() + testInfo.listFailed.size());
    }

    TString GetListName(const TDataBase& db, const TTest& test) {
        switch (db.GetData().Type) {
            case TDbData::LUNAPARK:
                return "load";
            case TDbData::AQUA:
            case TDbData::ROBOT:
                return "func_test";
            case TDbData::TEST_ENVIRONMENT:
                if (test.GetData().Name.EndsWith("DOLBILO"))
                    return "load";
                return test.GetData().Name < "BUILZ" ? "build"
                    : (test.GetData().Name < "PACKAGEZ" ? "packages" : "unit_test");
            default:
                VERIFY_WITH_LOG(false, "invalid usage");
        }
    }
}

void TReportTimeline::Process(void* /*ThreadSpecificResource*/) {
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
    time_t firstRevisionTimestamp = 0;
    if (RD->CgiParam.Find("first_time") != RD->CgiParam.end())
        firstRevisionTimestamp = FromString<time_t>(RD->CgiParam.Get("first_time"));
    TString regStr("");
    if (RD->CgiParam.Find("filter") != RD->CgiParam.end())
        regStr = RD->CgiParam.Get("filter");
    TRegExMatch filter(regStr.data());
    TReaderPtr<TDataBase::TChildren> tests = iDb->second->GetChildren();
    TReport report;
    for (TDataBase::TChildren::const_iterator iTest = tests->begin(); iTest != tests->end(); ++iTest) {
        if (!!regStr && !filter.Match(iTest->first.data()))
            continue;
        TReaderPtr<TTest::TChildren> executions = iTest->second->GetChildren();
        size_t count = revCount;
        for (TTest::TChildren::reverse_iterator iEx = executions->rbegin(); iEx != executions->rend() && count > 0; ++iEx, --count) {
            if (iEx->second->GetData().Revision.Timestamp < firstRevisionTimestamp)
                break;
            std::pair<TReport::iterator, bool> i = report.insert(TReport::value_type(iEx->first, TRevInfo()));
            if (i.second)
                i.first->second.Revision = iEx->second->GetData().Revision;
            TTestsInfo& ti = i.first->second.Lists[GetListName(*iDb->second, *iTest->second)];
            switch (iEx->second->GetData().Status) {
                case TTestExecutionData::OK:
                    ti.listFinished.insert(TTestList::value_type(iTest->first, iEx->second->GetData().TaskInfo));
                    ti.listFinished[iTest->first].InsertValue("integer_result", iEx->second->GetData().Result);
                    ti.listFinished[iTest->first].InsertValue("data_cache", iEx->second->GetData().DataCache);
                    ti.listFinished[iTest->first].InsertValue("task_id", iEx->second->GetData().TaskId);
                    break;

                case TTestExecutionData::COREDUMP:
                case TTestExecutionData::FAILD:
                    ti.listFailed.insert(TTestList::value_type(iTest->first, iEx->second->GetData().TaskInfo));
                    ti.listFailed[iTest->first].InsertValue("fail_info", iEx->second->GetData().FailInfo);
                    break;

                case TTestExecutionData::RUN:
                case TTestExecutionData::WAIT:
                case TTestExecutionData::UNKNOWN:
                    ti.listNotFinished.insert(TTestList::value_type(iTest->first, iEx->second->GetData().TaskInfo));
                    break;
                default:
                    break;
            }
        }
    }
    NJson::TJsonValue& rows = Result.InsertValue("rows", NJson::JSON_ARRAY);
    size_t count = revCount;
    bool writeTaskInfo = true;
    if (RD->CgiParam.Find("task_info") != RD->CgiParam.end())
        writeTaskInfo = FromString<bool>(RD->CgiParam.Get("task_info"));
    for (TReport::reverse_iterator rev = report.rbegin(); rev != report.rend() && count > 0; ++rev, --count) {
        NJson::TJsonValue& revInfo = rows.AppendValue(NJson::JSON_MAP);
        WriteRevisionInfo(rev->second.Revision, revInfo.InsertValue("revision", NJson::JSON_MAP));
        for (TRevInfo::TLists::const_iterator iList = rev->second.Lists.begin(); iList != rev->second.Lists.end(); ++iList)
            WriteTestInfo(iList->second, revInfo.InsertValue(iList->first, NJson::JSON_MAP), writeTaskInfo);
    }
}
