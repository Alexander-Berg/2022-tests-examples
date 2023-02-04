#include <saas/tools/test_model/updater.h>
#include <saas/tools/test_model/sandbox.h>
#include <saas/tools/test_model/utils.h>
#include <library/cpp/json/json_value.h>
#include <library/cpp/charset/ci_string.h>
#include <util/string/vector.h>

class TStringToExecutionStatus : public TMap<TCiString, TTestExecutionData::TStatus> {
public:
    TStringToExecutionStatus();
    TTestExecutionData::TStatus Get(const char* status) const;
};

class TSandboxUpdater : public IObjectInQueue {
public:
    TSandboxUpdater(TUpdater* owner, TTest::TChildPtr& data);
    void Process(void* ThreadSpecificResource) override;

private:
    TTest::TChildPtr Data;
    TUpdater* Owner;
};

class TTEUpdater : public TDbUpdater {
public:
    void Process(void* ThreadSpecificResource) override;
    static TDbUpdaterFactory::TRegistrator<TTEUpdater> Registrator;

private:
    struct TTestSetRecord {
        TVector<TTestExecutionData> Executions;
        TTestData TestData;
    };
    typedef TMap<TCiString, TTestSetRecord> TTestSetResult;

private:
    void UpdateTestSet(TTest& testset, ui64 fromRevision, TTestSetResult& result);
    void ApplyTestSetChanges(const TTestSetResult& tsr);
    ui64 UpdateTest(TTest& test, const NJson::TJsonValue::TArray& rows, NJson::TJsonValue::TArray::const_iterator& rowsIter);
};

TDbUpdaterFactory::TRegistrator<TTEUpdater> TTEUpdater::Registrator(TDbData::TEST_ENVIRONMENT);

using NJson::TJsonValue;
//  TStringToExecutionStatus
TStringToExecutionStatus::TStringToExecutionStatus() {
    insert(value_type("OK", TTestExecutionData::OK));
    insert(value_type("ERROR", TTestExecutionData::FAILD));
    insert(value_type("Enqueued", TTestExecutionData::WAIT));
    insert(value_type("Running", TTestExecutionData::RUN));
}

TTestExecutionData::TStatus TStringToExecutionStatus::Get(const char* status) const {
    const_iterator i = find(status);
    if (i == end())
        return TTestExecutionData::UNKNOWN;
    else
        return i->second;
}

//  TSandboxUpdater
TSandboxUpdater::TSandboxUpdater(TUpdater* owner, TTest::TChildPtr& data)
    : Data(data)
    , Owner(owner)
{}

void TSandboxUpdater::Process(void* /*ThreadSpecificResource*/) {
    THolder<TSandboxUpdater> suicide(this);
    TGuardTransaction g(Data->GetTransaction());
    if (Data->GetData().TaskId == 0){
        DEBUG_LOG << "found taskId == 0: " << Endl;
        return;
    }
    if(!NSandbox::GetTask(Owner->Config.Sandbox.Host, Owner->Config.Sandbox.Port, Data->GetData().TaskId, Data->GetData().TaskInfo)){
        Owner->AddSecondaryTask(suicide.Release(), false);
    }else{
        if (Data->GetData().TaskInfo.Has("ctx")){
            if (Data->GetData().TaskInfo["ctx"].Has("best_rps")){
                Data->GetData().Result = Data->GetData().TaskInfo["ctx"]["best_rps"].GetIntegerRobust();
            }
        }
    }
}

//  TTEUpdater
void TTEUpdater::Process(void* /*ThreadSpecificResource*/) {
    THolder<TTEUpdater> suicide(this);
    DEBUG_LOG << "Update db: " << Db->GetData().Name << Endl;
    TJsonValue dbSysInfo;
    for (int i = 0; i < 2; ++i) {
        if (GetJsonFromHttp(Owner->Config.TE.Host, Owner->Config.TE.Port, "/handlers/systemInfo?database=" + Db->GetData().Name, dbSysInfo)) {
            if (dbSysInfo.Has("is_started") && dbSysInfo["is_started"].IsBoolean()) {
                Db->GetData().Status = dbSysInfo["is_started"].GetBoolean() ? TDbData::STARTED : TDbData::NOT_STARTED;
                FATAL_LOG << "base " << Db->GetData().Name << " status:" << ToString(Db->GetData().Status) << " try:" << i << Endl;
                if (Db->GetData().Status == TDbData::STARTED)
                    break;
            } else
                ERROR_LOG << "Cannot read started state for " << Db->GetData().Name << Endl;
        } else
            ERROR_LOG << "Cannot read db info for " << Db->GetData().Name << Endl;
        Sleep(TDuration::Seconds(1));
    }
    TString testsList;
    NJson::TJsonValue testsListJ, testHistoryResults;
    bool gotHistory = GetJsonFromHttp(Owner->Config.TE.Host, Owner->Config.TE.Port,
            "/handlers/grids/metatestsFlaky?database=" + Db->GetData().Name +
            "&show_all=true", testHistoryResults, TDuration::Seconds(120));
    DEBUG_LOG << "Update db: " << Db->GetData().Name << " testlist" << Endl;
    TVector<TTestData> datas;
    TSet<TString> testsNames;
    if (gotHistory) {
        TRY
            const NJson::TJsonValue::TArray& rows = testHistoryResults["rows"].GetArray();
            for (const auto& i : rows) {
                TTestData data;
                data.Name = i["test_name"].GetString();
                data.HistoryPattern = i["flaky_pattern"].GetString();
                data.HistoryLastRevision = i["flaky_test_result/revision"].GetUInteger();
                data.MaxWeatherCount = Owner->Config.TE.WeatherCount;
                datas.push_back(data);
                testsNames.insert(data.Name);
            }
        CATCH("While Process tests history")
    }
    if (GetTextFromHttp(Owner->Config.TE.Host, Owner->Config.TE.Port, "/handlers/getTests?database=" + Db->GetData().Name, testsList, TDuration::Seconds(240))) {
        TRY
            const TVector<TString> tests(SplitString(testsList, ","));
            for (size_t i = 0; i < tests.size(); ++i) {
                if (testsNames.contains(tests[i]))
                    continue;
                TTestData data;
                data.Name = tests[i];
                data.MaxWeatherCount = Owner->Config.TE.WeatherCount;
                datas.push_back(data);
            }
        CATCH("While Read tests list")
    }
    TRY
        Db->UpdateChildren(datas, true);
    CATCH("While Update tests list")

    ui64 startRevision = Max<ui64>();
    {
        TReaderPtr<TDataBase::TChildren> tests = Db->GetChildren();
        for (TDataBase::TChildren::iterator i = tests->begin(); i != tests->end(); ++i)
            startRevision = Min<ui64>(i->second->LastUnstableRevision, startRevision);
    }

    ui64 N_Last_Revisions = (Db->GetData().Name.find("dolbilo") == TString::npos) ? 1000 : 10000;
    startRevision = Max<ui64>(startRevision, 1090000);

    DEBUG_LOG << "Update db: " << Db->GetData().Name << " tasks" << Endl;
    TJsonValue testResults;
    bool gotMain = GetJsonFromHttp(Owner->Config.TE.Host, Owner->Config.TE.Port,
            "/handlers/custom/getLastTestResults?database=" + Db->GetData().Name +
            "&start_revision="+ToString(startRevision) +
            "&n_last_revisions="+ToString(N_Last_Revisions), testResults, TDuration::Seconds(240));
    if (gotMain)
    {
        const NJson::TJsonValue::TArray& rows = testResults["rows"].GetArray();
        NJson::TJsonValue::TArray::const_iterator iRow = rows.begin();
        TTestSetResult tsr;
        {
            TReaderPtr<TDataBase::TChildren> tests = Db->GetChildren();
            while (iRow != rows.end()) {
                TDataBase::TChildren::iterator i = tests->find((*iRow)["test_name"].GetString());
                if(i == tests->end()) {
                    ERROR_LOG << "Unknown test name " << (*iRow)["test_name"].GetString() << Endl;
                    continue;
                }
                const ui64 firstRevision = UpdateTest(*i->second, rows, iRow);
                UpdateTestSet(*i->second, firstRevision, tsr);
            }
        }
        ApplyTestSetChanges(tsr);
    }
    Db->Invalidate();
    DEBUG_LOG << "Update db: " << Db->GetData().Name << " finished" << Endl;
};

void TTEUpdater::UpdateTestSet(TTest& testset, ui64 fromRevision, TTestSetResult& result) {
    if (!testset.GetData().Name.StartsWith("TESTSET"))
        return;
    TReaderPtr<TTest::TChildren> setResults = testset.GetChildren();
    for (TTest::TChildren::const_iterator i = setResults->lower_bound(fromRevision); i != setResults->end(); ++i) {
        if(!i->second->GetData().TaskInfo.Has("ctx"))
            continue;
        const NJson::TJsonValue& ctx = i->second->GetData().TaskInfo["ctx"];
        if (!ctx.Has("test_results"))
            continue;
        const NJson::TJsonValue::TArray& tr = ctx["test_results"].GetArray();
        for (NJson::TJsonValue::TArray::const_iterator testRes = tr.begin(); testRes != tr.end(); ++testRes) {
            if (!testRes->Has("testname"))
                continue;
            TCiString testName = (*testRes)["testname"].GetString();
            TTestSetRecord& tsr = result[testName];
            tsr.TestData = testset.GetData();
            tsr.TestData.Name = testName;
            tsr.Executions.push_back(i->second->GetData());
            tsr.Executions.back().Status = Singleton<TStringToExecutionStatus>()->Get((*testRes)["status"].GetString().data());
        }
    }
}

void TTEUpdater::ApplyTestSetChanges(const TTestSetResult& tsr) {
    if (tsr.empty())
        return;
    TVector<TTestData> datas;
    for (TTestSetResult::const_iterator i = tsr.begin(); i != tsr.end(); ++i)
        datas.push_back(i->second.TestData);
    Db->UpdateChildren(datas, false);
    TReaderPtr<TDataBase::TChildren> tests = Db->GetChildren();
    for (TTestSetResult::const_iterator i = tsr.begin(); i != tsr.end(); ++i) {
        TDataBase::TChildren::iterator test = tests->find(i->first);
        Y_VERIFY(test != tests->end(), "hren' kakaya-to");
        test->second->UpdateChildren(i->second.Executions, false);
    }
}

ui64 TTEUpdater::UpdateTest(TTest& test, const NJson::TJsonValue::TArray& rows, NJson::TJsonValue::TArray::const_iterator& rowsIter) {
    TVector<TTestExecutionData> testResults;
    ui64 firstRevision = Max<ui64>();
    time_t sec = TInstant::Now().Seconds();
    TString timeZoneSuffix = Strftime("%z", localtime(&sec));
    TRY
        for (;rowsIter != rows.end(); ++rowsIter) {
            TTestExecutionData data;
            const NJson::TJsonValue& res = *rowsIter;
            if (res["test_name"].GetString() != test.GetData().Name)
                break;
            //DEBUG_LOG << test.GetData().Name << "stat:" << res["status"].GetString();
            //if (res["status"].GetUInteger() == 3 || res["status"].GetUInteger() == 4)
              //  continue;
            data.Revision.Revision = res["revision"].GetUInteger();
            data.TaskId = res["task_id"].GetUInteger();
            if (res["status"].GetString() == "ERROR"){
                data.FailInfo = res["data_cache"].GetString();
                //DEBUG_LOG << "Test" << test.GetData().Name << "FailInfo:" << data.FailInfo;
            }else if (res["status"].GetString() == "OK"){
                data.DataCache = res["data_cache"].GetString();
            }
            if (data.Revision.Revision < test.LastUnstableRevision)
                continue;
            if ((test.TaskId >= data.TaskId) && !!test.LastFinished)
                continue;
            firstRevision = Min<ui64>(data.Revision.Revision, firstRevision);
            data.Revision.Comment = res["revision_info/comment"].GetString();
            data.Revision.Author = res["revision_info/author"].GetString();
            ParseISO8601DateTimeDeprecated((res["revision_info/timestamp"].GetString() + timeZoneSuffix).data(), data.Revision.Timestamp);

            Singleton<TSvnInfo>()->StoreRevisionInfo(data.Revision);
            data.Status = Singleton<TStringToExecutionStatus>()->Get(res["status"].GetString().data());
            /*for(TSet<TTest*>::const_iterator build = test.Builds->begin(); build != test.Builds->end(); ++build) {
                TReaderPtr<TTest::TChildren> buildResults = (*build)->GetChildren();
                TTest::TChildren::const_iterator buildRevision = buildResults->find(data.Revision.Revision);
                if (buildRevision != buildResults->end() && buildRevision->second->GetData().Status != TTestExecutionData::OK) {
                    data.Status = TTestExecutionData::NOT_BUILD;
                    break;
                }
            }*/
            testResults.push_back(data);
        };
    CATCH("While Update results list")
    TVector<TTest::TChildPtr> resultsChld = test.UpdateChildren(testResults, false);
    bool isTestSet = test.GetData().Name.StartsWith("TESTSET");
    //DEBUG_LOG << "updating test " << test.GetData().Name << ": " << resultsChld.ysize() << " tasks";
    //todo: need only rps, cd, etc
    TStringStream taskIds;
    if (test.GetData().Name.EndsWith("DOLBILO")){
        for (TVector<TTest::TChildPtr>::iterator i = resultsChld.begin(); i != resultsChld.end(); ++i){
            if ((*i)->GetData().Status == TTestExecutionData::OK)
                Owner->AddSecondaryTask(new TSandboxUpdater(Owner, *i), isTestSet);
        }
    } else if (resultsChld.ysize() != 0){
        taskIds << (*resultsChld.rbegin())->GetData().TaskId << ",";
        //Owner->AddSecondaryTask(new TSandboxUpdater(Owner, *(resultsChld.rbegin())), isTestSet);
    }
    DEBUG_LOG << "skip tasks: " << taskIds.Str() << Endl;

    return firstRevision;
}

