#include "deploy_manager.h"
#include <saas/library/daemon_base/controller_actions/restart.h>
#include <saas/deploy_manager/scripts/add_replica/action.h>
#include <saas/library/searchmap/searchmap.h>
#include <library/cpp/json/json_reader.h>

START_TEST_DEFINE_PARENT(TestDeployManagerUserAccess, TestDeployManager)

bool Run() override {
    TString tester_cookie = "46ccdd71ddbfdfb28650a9f127037d2e";
    TString stranger_cookie = "fd69e34223208ce45e3cb45c3996d12b";
    TString res = Controller->SendCommandToDeployManager("get_cookie?user=tester");
    CHECK_TEST_FAILED(res.find(tester_cookie) == TString::npos, "no good cookie in answer: " + res);
    Controller->UploadFileToDeployManager(GetRunDir() + "/copy/searchmap_two_services", "/common/sbtests/cluster.meta");
    MUST_BE_BROKEN(Controller->SendCommandToDeployManager("get_user_access_config?user=tester"));
    NDaemonController::TControllerAgent agent(Controller->GetConfig().DeployManager.Host, Controller->GetConfig().DeployManager.Port);

    agent.ExecuteCommand(Controller->GetConfig().DeployManager.UriPrefix + "set_user_access_config?user=tester", res, 2000, 2, "{\"user_services\":[\"all_services\"],\"commands\":{\"all_commands\":{\"services\":[\"all_services\"]}}}");
    agent.ExecuteCommand(Controller->GetConfig().DeployManager.UriPrefix + "set_user_access_config?user=stranger&cookie=" + tester_cookie, res, 2000, 2, "{\"user_services\":[\"tests\"],\"commands\":{\"all_commands\":{\"services\":[\"tests\"]}}}");

    res = Controller->SendCommandToDeployManager("get_user_access_config?user=tester&cookie=" + tester_cookie);

    MUST_BE_BROKEN(Controller->SendCommandToDeployManager("get_user_access_config?user=stranger&cookie=" + stranger_cookie));
    Controller->SendCommandToDeployManager("get_cluster_map?service=tests&ctype=sbtests&cookie=" + tester_cookie);
    agent.ExecuteCommand(Controller->GetConfig().DeployManager.UriPrefix + "set_conf?filename=searchproxy-tests.conf&service=tests&cookie=" + tester_cookie, res, 2000, 2, "<Service>\nName: tests\n</Service>");
    agent.ExecuteCommand(Controller->GetConfig().DeployManager.UriPrefix + "set_conf?filename=searchproxy-tests.conf&service=tests&cookie=" + stranger_cookie, res, 2000, 2, "<Service>\nName: tests\n</Service>");

    return true;
}
};
