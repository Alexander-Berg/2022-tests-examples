#include <saas/tools/test_model/updater.h>
#include <saas/tools/test_model/utils.h>

class TLunaparkUpdater : public TDbUpdater {
public:
    void Process(void* ThreadSpecificResource) override;
    static TDbUpdaterFactory::TRegistrator<TLunaparkUpdater> Registrator;

private:
    bool UpdateTest(const TString& name, ui64 build);
};

using NJson::TJsonValue;

TDbUpdaterFactory::TRegistrator<TLunaparkUpdater> TLunaparkUpdater::Registrator(TDbData::LUNAPARK);

void TLunaparkUpdater::Process(void* /*ThreadSpecificResource*/) {
    THolder<TLunaparkUpdater> suicide(this);
    DEBUG_LOG << "Update db: " << Db->GetData().Name << Endl;
    TJsonValue testsList;
    if (GetJsonFromHttp(Owner->Config.Lunapark.Host, Owner->Config.Lunapark.Port, "/view/" + Db->GetData().Name + "/api/json", testsList)) {
        TRY
            const TJsonValue::TArray& tests = testsList["jobs"].GetArray();
            for (size_t i = 0; i < tests.size(); ++i) {
                const TString& jobName = tests[i]["name"].GetString();

                TSet<ui64> buildsNumbers;
                TJsonValue builds;
                if (!GetJsonFromHttp(Owner->Config.Lunapark.Host, Owner->Config.Lunapark.Port, "/job/" + jobName + "/api/json", builds)) {
                    ERROR_LOG << "Cannot download builds number for test " << jobName << " in db " << Db->GetData().Name << Endl;
                    return;
                }
                const TJsonValue::TArray& buildsArr = builds["builds"].GetArray();
                for (size_t i = 0; i < buildsArr.size(); ++i)
                    buildsNumbers.insert(buildsArr[i]["number"].GetUInteger());
                for (TSet<ui64>::reverse_iterator bNumber = buildsNumbers.rbegin(); bNumber != buildsNumbers.rend(); ++bNumber)
                    if (!UpdateTest(jobName, *bNumber))
                        break;
            }
        CATCH("While Update tests list")
    }
    Db->Invalidate();
    DEBUG_LOG << "Update db: " << Db->GetData().Name << " finished" << Endl;
}

namespace {
    ui64 GetRevision(const TJsonValue& value) {
        try {
            if (value.IsInteger())
                return value.GetUInteger();
            if (value.IsString())
                return FromString<ui64>(value.GetString());
        } catch (...) {

        }
        return 0;
    }
}

bool TLunaparkUpdater::UpdateTest(const TString& name, ui64 build) {
    TJsonValue status;
    if (!GetJsonFromHttp(Owner->Config.Lunapark.Host, Owner->Config.Lunapark.Port, "/job/" + name + "/" + ToString(build) + "/artifact/status.json", status))
        return true;

    TString testName(name);
    if (status["params"].Has("testname")) {
        const TString& testNameField = status["params"]["testname"].GetString();
        if (!!testNameField)
            testName = testNameField;
    }
    if (status["params"].Has("branch")) {
        const TString& brunchField = status["params"]["branch"].GetString();
        if (!!brunchField)
            testName += "_" + brunchField;
    }
    testName.to_upper();
    TReaderPtr<TDataBase::TChildren> tests = Db->GetChildren();
    TDataBase::TChildren::iterator iTest = tests->find(testName);
    TDataBase::TChildPtr test;
    if (iTest == tests->end()) {
        TTestData testData;
        testData.IsExecuted = false;
        testData.IsOn = true;
        testData.MaxWeatherCount = 0;
        testData.Name = testName;
        tests.Reset(nullptr);
        test = Db->UpdateChild(testData);
    } else {
        test = iTest->second;
        tests.Reset(nullptr);
    }

    TTestExecutionData execution;
    execution.TaskId = build;
    execution.Revision = Singleton<TSvnInfo>()->GetRevisionInfo(GetRevision(status["params"]["version"]));
    if (status["status"].GetString() == "done") {
        execution.Result = GetRevision(status["result"]["imbalance_rps"]);
        execution.Status = TTestExecutionData::OK;
    } else
        execution.Status = TTestExecutionData::FAILD;
    TJsonValue& bestResult = execution.TaskInfo.InsertValue("best_result", NJson::JSON_MAP);
    bestResult.InsertValue("rps", execution.Result);
    bestResult.InsertValue("sandbox_task", status["params"]["external_test_id"]);
    bestResult.InsertValue("task_id", build);
    execution.TaskInfo.InsertValue("results", NJson::JSON_ARRAY).AppendValue(bestResult);
    TReaderPtr<TTest::TChildren> executions = test->GetChildren();
    TTest::TChildren::const_iterator iExec = executions->find(execution.Revision.Revision);
    if (iExec != executions->end()) {
        if (iExec->second->GetData().Status == TTestExecutionData::OK)
            execution.Status = TTestExecutionData::OK;
        const TJsonValue& results = iExec->second->GetData().TaskInfo["results"];
        for (TJsonValue::TArray::const_iterator i = results.GetArray().begin(); i != results.GetArray().end(); ++i)
            if ((*i)["task_id"].GetUInteger() == build)
                return false;
        execution.TaskInfo.InsertValue("results", results).AppendValue(execution.TaskInfo["best_result"]);
    }
    const TJsonValue::TArray& resultsArray = execution.TaskInfo["results"].GetArray();
    for (TJsonValue::TArray::const_iterator i = resultsArray.begin(); i != resultsArray.end(); ++i) {
        i64 rps = (*i)["rps"].GetInteger();
        if (rps > execution.Result) {
            execution.Result = rps;
            execution.TaskId = (*i)["task_id"].GetInteger();
            execution.TaskInfo.InsertValue("best_result", *i);
        }
    }
    executions.Reset(nullptr);
    test->UpdateChild(execution);
    return true;
}
