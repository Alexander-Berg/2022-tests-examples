#include "deploy_manager.h"

#include <saas/deploy_manager/scripts/add_intsearch/action.h>
#include <saas/deploy_manager/scripts/add_replica/action.h>
#include <saas/deploy_manager/scripts/cluster_control/action/action.h>
#include <saas/deploy_manager/scripts/deploy/action.h>
#include <saas/deploy_manager/scripts/release_slots/action.h>
#include <saas/deploy_manager/scripts/reshard/action.h>
#include <saas/deploy_manager/scripts/searchmap/action.h>

#include <saas/library/sharding/sharding.h>

#include <library/cpp/json/json_reader.h>

#include <util/random/random.h>


START_TEST_DEFINE_PARENT(TestDeployManagerCopySlots, TestDeployManager)

NSearchMapParser::TSlotsPool Pool;

void InitCluster() override {
    UploadCommon();
    UploadService("tests");
    Pool = ConfigureCluster(1, 3, NSaas::UrlHash, "rtyserver", "tests");
    DeploySp();
    DeployIp();
}

bool Run() override {
    i64 countMessages = 30;
    TVector<NRTYServer::TMessage> messages;
    TSet<std::pair<ui64, TString> > deleted;
    GenerateInput(messages, countMessages, NRTYServer::TMessage::ADD_DOCUMENT, GetIsPrefixed());
    {
        TDebugLogTimer timer("Index messages full cluster");
        IndexMessages(messages, DISK, 1);
        ReopenIndexers();
    }

    CHECK_TEST_EQ(Pool.GetSlots().size(), 3);

    TSet<TString> nodes;

    for (auto&& node : Controller->GetConfig().Controllers) {
        INFO_LOG << "node: " << node.GetString() << Endl;
        nodes.insert(node.Host + ":" + ToString(node.Port - 3));
    }

    for (ui32 i = 0; i < Pool.GetSlots().size(); ++i) {
        INFO_LOG << "pool: " << Pool.GetSlots()[i].GetSlotName() << Endl;
        nodes.erase(Pool.GetSlots()[i].GetSlotName());
    }

    CHECK_TEST_EQ(nodes.size(), 5);
    TVector<TString> copy;
    copy.push_back(Pool.GetSlots()[0].GetSlotName());
    copy.push_back(Pool.GetSlots()[2].GetSlotName());

    TVector<TString> pool;
    pool.push_back(*nodes.begin());
    pool.push_back(*nodes.begin());

    {
        TDebugLogTimer timer(ToString(copy.size()) + " backends (" + JoinStrings(copy, ",") + ") copy");
        NDaemonController::TClusterControlAction ra("sbtests", "tests", TVector<TString>(), TVector<TString>(),
            NDaemonController::apStartAndWait, true);
        ra.SetCopySlotsPool(copy).SetNewSlotsPool(pool);
        MUST_BE_BROKEN(Controller->ExecuteActionOnDeployManager(ra));
    }
    pool.clear();
    pool.push_back(*nodes.begin());
    pool.push_back(*nodes.rbegin());
    {
        TDebugLogTimer timer(ToString(copy.size()) + " backends (" + JoinStrings(copy, ",") + ") copy");
        NDaemonController::TClusterControlAction ra("sbtests", "tests", TVector<TString>(), TVector<TString>(),
            NDaemonController::apStartAndWait, true);
        ra.SetCopySlotsPool(copy).SetNewSlotsPool(pool);
        MUSTNT_BE_BROKEN(Controller->ExecuteActionOnDeployManager(ra));
    }

    CheckDocuments(messages, deleted);
    CHECK_TEST_TRUE(CheckBackends(pool));
    return true;
}
};

