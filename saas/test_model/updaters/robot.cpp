#include <saas/tools/test_model/updater.h>
#include <saas/tools/test_model/utils.h>

class TRobotUpdater : public TDbUpdater {
    void Process(void* ThreadSpecificResource) override;
    static TDbUpdaterFactory::TRegistrator<TRobotUpdater> Registrator;
};

using NJson::TJsonValue;

TDbUpdaterFactory::TRegistrator<TRobotUpdater> TRobotUpdater::Registrator(TDbData::ROBOT);

void TRobotUpdater::Process(void* /*ThreadSpecificResource*/) {
    THolder<TRobotUpdater> suicide(this);

    DEBUG_LOG << "Update db: " << Db->GetData().Name << Endl;

    TJsonValue root;
    if (!GetJsonFromHttp(Owner->Config.Robot.Host, Owner->Config.Robot.Port, "/rrt_db_ui/rrt.pl?run_type.flt=night&export=json", root))
        return;
    if (root.Has("tests_run_results")) {
        const TJsonValue::TArray& tests = root["tests_run_results"].GetArray();
        TVector<TTestData> datas(tests.size());
        TRY
            for (size_t i = 0; i < tests.size(); ++i) {
                datas[i].Name = tests[i]["test_name"].GetString();
                datas[i].IsOn = true;
                datas[i].IsExecuted = false;
                datas[i].MaxWeatherCount = 5;
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
                    bool ok = tests[i]["result"].GetString() == "pass";
                    if (ok && tests[i]["new_files"].GetBoolean())
                        ok = false;
                    else if (!ok && tests[i]["canonize"].GetBoolean())
                        ok = true;
                    exec.Status = ok ? TTestExecutionData::OK : TTestExecutionData::FAILD;
                    exec.Revision = Singleton<TSvnInfo>()->GetRevisionInfo(tests[i]["revision"].GetInteger());
                    dbTests->find(datas[i].Name)->second->UpdateChildren(executions, false);
                CATCH("While Update test " + datas[i].Name)
            }
        }
        Db->Invalidate();
    }

    DEBUG_LOG << "Update db: " << Db->GetData().Name << " finished" << Endl;
}

