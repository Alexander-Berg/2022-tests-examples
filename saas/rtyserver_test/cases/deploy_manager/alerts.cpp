#include "deploy_manager.h"

#include <saas/deploy_manager/scripts/process_alerts/total_actions/total_action.h>
#include <saas/deploy_manager/scripts/searchmap/action.h>

START_TEST_DEFINE_PARENT(TestDeployManagerAlerts, TestDeployManager)

bool Run() override {
    UploadCommon();
    Controller->UploadDataToDeployManager(" ", "/common/unused/cluster.meta");
    UploadService("tests");
    Controller->UploadFileToDeployManager(GetRunDir() + "/copy/searchmap_two_services", "/common/sbtests/cluster.meta");
    Controller->UploadFileToDeployManager(GetRunDir() + "/copy/alert_keys.json", "/configs/alert_keys.json");
    Controller->UploadFileToDeployManager(GetRunDir() + "/copy/alerts_sla.conf", "/configs/tests/sla_description.conf");

    CHECK_TEST_TRUE(SetAlertsUniqueNames());
    //check action=list case
    TString created = Controller->SendCommandToDeployManager("process_alerts?action=create&what=juggler,golovan&service=newservice&ctype=sbtests&akey=somekey-dm");
    DEBUG_LOG << created << Endl;
    created = Controller->SendCommandToDeployManager("process_alerts?action=create&service=tests&ctype=sbtests&akey=somekey-pr");
    DEBUG_LOG << created << Endl;
    //test get_conf
    TString conf = Controller->SendCommandToDeployManager("process_alerts?action=get_conf&service=tests&ctype=sbtests");
    DEBUG_LOG << conf << Endl;
    NJson::TJsonValue confJs;
    TStringStream ss(conf);
    NJson::ReadJsonTree(&ss, &confJs);
    CHECK_TEST_TRUE(confJs["conf"]["data"]["somekey-dm"]["limits"]["warn"].GetDouble() == 2.2);
    CHECK_TEST_TRUE(confJs["conf_parts"]["default_conf"]["data"]["somekey-dm"]["limits"]["warn"].GetDouble() == 2.2);
    CHECK_TEST_TRUE(confJs["conf_parts"]["default_conf"]["data"]["somekey-pr"]["limits"]["warn"].GetDouble() == 0.01);
    CHECK_TEST_TRUE(confJs["conf_parts"]["default_conf"]["data"]["somekey-pr"]["limits"]["crit"].GetDouble() == 0.15);
    CHECK_TEST_TRUE(confJs["conf"]["data"]["somekey-pr"]["limits"]["warn"].GetDouble() == 0.001);
    CHECK_TEST_TRUE(confJs["conf"]["data"]["somekey-pr"]["limits"]["crit"].GetDouble() == 0.01);

    NJson::TJsonValue alerts;
    CHECK_TEST_TRUE(CheckAlertsCount(2, "", alerts));
    CHECK_TEST_TRUE(CheckAlertsCount(1, "&service=newservice&akey=somekey-dm", alerts));

    //check deleting alert on rename_service
    NDaemonController::TSimpleSearchmapModifAction renameService("sbtests", "newservice", "rename_service", "rtyserver", true,  "newestservice", false, false);
    Controller->ExecuteActionOnDeployManager(renameService);
    CHECK_TEST_TRUE(CheckAlertsCount(0, "&service=newservice", alerts));

    //check action=clear_left
    created = Controller->SendCommandToDeployManager("process_alerts?action=create&service=newestservice&ctype=sbtests&akey=somekey-dm");
    DEBUG_LOG << created << Endl;
    Controller->UploadFileToDeployManager(GetRunDir() + "/copy/searchmap_two_services", "/common/sbtests/cluster.meta");
    CHECK_TEST_TRUE(CheckAlertsCount(1, "&service=newestservice", alerts));
    created = Controller->SendCommandToDeployManager("process_alerts?action=create&what=golovan,juggler&service=newservice&ctype=sbtests&akey=somekey-dm");
    CHECK_TEST_TRUE(CheckAlertsCount(3, "&ctype=sbtests", alerts));
    TString cleared = Controller->SendCommandToDeployManager("process_alerts?action=clear_left");
    DEBUG_LOG << cleared << Endl;
    CHECK_TEST_TRUE(CheckAlertsCount(2, "&ctype=sbtests", alerts));

    TString checked = Controller->SendCommandToDeployManager("process_alerts?action=check&service=newservice&ctype=sbtests");
    DEBUG_LOG << checked << Endl;
    //todo: fields checks

    //check deleting alert on remove_service
    TVector<TString> slots;
    NDaemonController::TSimpleSearchmapModifAction deleteService(slots, "sbtests", "newservice", "remove_service", "rtyserver");
    Controller->ExecuteActionOnDeployManager(deleteService);
    CHECK_TEST_TRUE(CheckAlertsCount(1, "", alerts));

    //check remove by id
    const NJson::TJsonValue& gHostData = alerts["checks"].GetMap().begin()->second;
    TString checkId = gHostData.GetMap().begin()->first;
    TString removed = Controller->SendCommandToDeployManager("process_alerts?action=remove&juggler_id=" + checkId + "&tag=saas_service_tests");
    DEBUG_LOG << removed << Endl;
    TString alertId = alerts["alerts"].GetArray()[0]["name"].GetString();
    removed = Controller->SendCommandToDeployManager("process_alerts?action=remove&golovan_id=" + alertId);
    DEBUG_LOG << removed << Endl;
    CHECK_TEST_TRUE(CheckAlertsCount(0, "", alerts));

    //check remove by service
    Controller->SendCommandToDeployManager("process_alerts?action=create&what=golovan,juggler&service=tests&ctype=sbtests&akey=somekey-dm");
    CHECK_TEST_TRUE(CheckAlertsCount(1, "", alerts));
    removed = Controller->SendCommandToDeployManager("process_alerts?action=remove&what=golovan,juggler&service=tests&ctype=sbtests");
    DEBUG_LOG << removed << Endl;
    CHECK_TEST_TRUE(CheckAlertsCount(0, "", alerts));
    return true;
}
};

START_TEST_DEFINE_PARENT(TestDeployManagerAlertsOnDeploy, TestDeployManager)
bool Run() override {
    UploadCommon();
    UploadService("tests");

    CHECK_TEST_TRUE(SetAlertsUniqueNames());
    Controller->UploadFileToDeployManager(GetRunDir() + "/copy/alert_keys.json", "/configs/alert_keys.json");

    NJson::TJsonValue alerts;
    CHECK_TEST_TRUE(CheckAlertsCount(0, "", alerts));
    ConfigureCluster(1, 1, NSaas::UrlHash, "rtyserver", "tests");
    CHECK_TEST_TRUE(CheckAlertsCount(2, "", alerts));
    DeploySp();
    CHECK_TEST_TRUE(CheckAlertsCount(2, "", alerts));

    TString checked = Controller->SendCommandToDeployManager("process_alerts?action=check&service=tests&ctype=sbtests");
    DEBUG_LOG << checked << Endl;
    TStringStream ss(checked);
    NJson::TJsonValue checkedJs;
    NJson::ReadJsonTree(&ss, &checkedJs);
    CHECK_TEST_TRUE(checkedJs["is_actual"].GetBoolean());
    CHECK_TEST_TRUE(checkedJs["checks"]["is_actual"].GetBoolean());
    CHECK_TEST_TRUE(checkedJs["alerts"]["is_actual"].GetBoolean());

    Controller->UploadFileToDeployManager(GetRunDir() + "/copy/alerts_changed.conf", "/configs/tests/alerts.conf");
    TString actualized = Controller->SendCommandToDeployManager("process_alerts?action=actualize&service=tests&ctype=sbtests");
    DEBUG_LOG << actualized << Endl;
    TStringStream ss1(actualized);
    NJson::ReadJsonTree(&ss1, &checkedJs);
    CHECK_TEST_TRUE(checkedJs["done_checks"]["cnt_changed"].GetUInteger() == 1);
    CHECK_TEST_TRUE(checkedJs["done_alerts"]["cnt_changed"].GetUInteger() == 1);

    Controller->UploadFileToDeployManager(GetRunDir() + "/copy/alerts_empty.conf", "/configs/tests/alerts.conf");
    DeployBe("tests");
    CHECK_TEST_TRUE(CheckAlertsCount(0, "", alerts));
    return true;
    }
};


START_TEST_DEFINE_PARENT(TestDeployManagerAlertsActions, TestDeployManager)

bool Run() override {
    UploadCommon();
    Controller->UploadDataToDeployManager(" ", "/common/unused/cluster.meta");
    UploadService("tests");
    Controller->UploadFileToDeployManager(GetRunDir() + "/copy/searchmap_two_services", "/common/sbtests/cluster.meta");
    Controller->UploadFileToDeployManager(GetRunDir() + "/copy/alert_keys.json", "/configs/alert_keys.json");
    Controller->UploadFileToDeployManager(GetRunDir() + "/copy/alerts_sla.conf", "/configs/tests/sla_description.conf");

    CHECK_TEST_TRUE(SetAlertsUniqueNames());
    NJson::TJsonValue alerts;

    TVector<NDaemonController::TAction::TPtr> tasks;
    TSet<TString> ids;
    for (int i = 0; i < 5; ++i) {
        NRTYDeploy::TAlertsTotalAction ta(NDaemonController::apStart);
        Controller->ExecuteActionOnDeployManager(ta);
        NRTYDeploy::TWaitAsyncAction* daWait = new NRTYDeploy::TWaitAsyncAction(ta.ActionId());
        daWait->SetIdTask(ta.GetIdTask());
        tasks.push_back(daWait);
        ids.insert(ta.GetIdTask());

        DEBUG_LOG << ta.GetIdTask() << Endl;
        Sleep(TDuration::Seconds(1));
        if (ids.size() > 2)
            ythrow yexception() << "too many unique task ids";
    }
    for (auto& i : tasks) {
        Controller->ExecuteActionOnDeployManager(*i);
    }

    CHECK_TEST_TRUE(CheckAlertsCount(4, "", alerts));

    //remove it
    Controller->UploadFileToDeployManager(GetRunDir() + "/copy/alerts_empty.conf", "/configs/tests/alerts.conf");
    Controller->UploadFileToDeployManager(GetRunDir() + "/copy/alerts_empty.conf", "/configs/newservice/alerts.conf");
    NRTYDeploy::TAlertsTotalAction ta0(NDaemonController::apStartAndWait);
    Controller->ExecuteActionOnDeployManager(ta0);
    CHECK_TEST_TRUE(CheckAlertsCount(0, "", alerts));
    return true;
}
};
