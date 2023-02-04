#include <saas/tools/test_model/updater.h>
#include <saas/tools/test_model/utils.h>

class TAquaUpdater : public TDbUpdater {
    void Process(void* ThreadSpecificResource) override;
    static TDbUpdaterFactory::TRegistrator<TAquaUpdater> Registrator;
};

using NJson::TJsonValue;

TDbUpdaterFactory::TRegistrator<TAquaUpdater> TAquaUpdater::Registrator(TDbData::AQUA);

namespace {
    ui64 GetTimestamp(const TJsonValue& value) {
        if (value.IsInteger())
            return value.GetInteger();
        if (value.IsString())
            return FromString<ui64>(value.GetString());
        ythrow yexception() << "incorrect value";
    }

    double GetFloat(const TJsonValue& value) {
        if (value.IsDouble())
            return value.GetDouble();
        if (value.IsString())
            return FromString<double>(value.GetString());
        ythrow yexception() << "incorrect value";
    }
}

void TAquaUpdater::Process(void* /*ThreadSpecificResource*/) {
    THolder<TAquaUpdater> suicide(this);

    DEBUG_LOG << "Update db: " << Db->GetData().Name << Endl;

    TJsonValue testsList;
    if (!GetJsonFromHttp(Owner->Config.Aqua.Host, Owner->Config.Aqua.Port, "/opt/testdata/innerpochta/" + Db->GetData().Name + "/test_results.json", testsList))
        return;
    const TJsonValue::TArray& tests = testsList["testResults"]["testResult"].GetArray();
    TVector<TTestData> datas(tests.size());
    TRY
        for (size_t i = 0; i < tests.size(); ++i) {
            datas[i].Name = tests[i]["title"].GetString();
            datas[i].IsOn = true;
            datas[i].IsExecuted = false;
            datas[i].MaxWeatherCount = 0;
        }
        Db->UpdateChildren(datas, true);
    CATCH("While Update tests list")

    {
        TReaderPtr<TDataBase::TChildren> dbTests = Db->GetChildren();
        for (size_t i = 0; i < tests.size(); ++i) {
            TRY
                TVector<TTestExecutionData> executions(1);
                TTestExecutionData& exec = executions[0];
                exec.TaskInfo = tests[i];
                exec.Result = GetFloat(exec.TaskInfo["percentageOfFailed"]) * 100;
                exec.Status = exec.Result == 0 ? TTestExecutionData::OK : TTestExecutionData::FAILD;
                exec.Revision.Timestamp = GetTimestamp(exec.TaskInfo["date"]);
                exec.Revision.Revision = exec.Revision.Timestamp;
                dbTests->find(datas[i].Name)->second->UpdateChildren(executions, false);
            CATCH("While Update test " + datas[i].Name)
        }
    }
    Db->Invalidate();
    DEBUG_LOG << "Update db: " << Db->GetData().Name << " finished" << Endl;
}

