#include "test_functions.h"

#include <google/protobuf/util/message_differencer.h>

#include <infra/pod_agent/libs/behaviour/bt/render/console_renderer.h>
#include <infra/pod_agent/libs/behaviour/loaders/behavior3_editor_json_reader.h>

#include <infra/pod_agent/libs/ip_client/async_client.h>

#include <infra/pod_agent/libs/porto_client/nested_client.h>
#include <infra/pod_agent/libs/porto_client/porto_test_lib/client_with_retries.h>
#include <infra/pod_agent/libs/porto_client/porto_test_lib/test_functions.h>

#include <infra/pod_agent/libs/system_logs_sender/mock_system_logs_sender.h>

#include <google/protobuf/util/json_util.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

#include <util/stream/file.h>
#include <util/system/thread.h>

namespace NInfra::NPodAgent::NTreeTest {

namespace {

TLoggerConfig GetLoggerConfig() {
    TLoggerConfig result;
    result.SetBackend(TLoggerConfig_ELogBackend_STDERR);
    result.SetLevel("ERROR");
    return result;
}

static TLogger logger(GetLoggerConfig());

} // namespace

TPortoClientPtr GetTestPortoClient(const TString& testName) {
    const TString rootName = TEST_PREFIX + testName + ToString(TThread::CurrentThreadId());
    TPortoClientPtr client = NPortoTestLib::GetTestPortoClient(TEST_PREFIX, TEST_PREFIX, TEST_PREFIX, {});

    static TMap<TString, TLocalContainerPtr> localContainers;
    if (!localContainers.contains(rootName)) {
        localContainers[rootName] = new TLocalContainer(client, {rootName});
        client->Start(rootName);
    }

    return new TNestedPortoClient(client, rootName);
}

TPortoClientPtr GetTestPortoClientWithRetries(const TString& testName) {
    return new TPortoClientWithRetries(GetTestPortoClient(testName));
}

TTemplateBTStoragePtr GetTemplateBTStorage() {
    TTemplateBTStorageConfig config;
    config.AddTreeDirs("/pod_agent/behavior_templates/");

    return TTemplateBTStoragePtr(
        new TTemplateBTStorage(config)
    );
}

TTreePtr GetBuildedTree(
    const TBehavior3& protoTree
    , TTemplateBTStoragePtr templateBTStorage
    , TUpdateHolderTargetPtr updateHolderTarget
    , TBoxStatusRepositoryPtr boxStatusRepository
    , TLayerStatusRepositoryPtr layerStatusRepository
    , TStaticResourceStatusRepositoryPtr staticResourceStatusRepository
    , TVolumeStatusRepositoryPtr volumeStatusRepository
    , TWorkloadStatusRepositoryPtr workloadStatusRepository
    , TWorkloadStatusRepositoryInternalPtr workloadStatusRepositoryInternal
    , TIpClientPtr ipClient
    , TNetworkClientPtr networkClient
    , TPortoClientPtr portoClient
    , TPathHolderPtr pathHolder
) {
    TAtomicSharedPtr<IThreadPool> mtpQueue = new TFakeThreadPool();
    TAsyncIpClientPtr asyncIpClient = new TAsyncIpClient(ipClient, mtpQueue);
    TAsyncPortoClientPtr asyncPortoClient = new TAsyncPortoClient(portoClient, mtpQueue);
    TPosixWorkerPtr posixWorker = new TPosixWorker(mtpQueue);
    ISystemLogsSenderPtr systemLogsSender = new TMockSystemLogsSender();

    TTreePtr tree = new TTree(
        logger
        , "tree"
        , TBehavior3EditorJsonReader(protoTree)
            .WithPosixWorker(posixWorker)
            .WithIpClient(asyncIpClient)
            .WithPorto(asyncPortoClient)
            .WithNetworkClient(networkClient)
            .WithUpdateHolderTarget(updateHolderTarget)
            .WithBoxStatusRepository(boxStatusRepository)
            .WithLayerStatusRepository(layerStatusRepository)
            .WithStaticResourceStatusRepository(staticResourceStatusRepository)
            .WithVolumeStatusRepository(volumeStatusRepository)
            .WithWorkloadStatusRepository(workloadStatusRepository)
            .WithWorkloadInternalStatusRepository(workloadStatusRepositoryInternal)
            .WithTemplateBTStorage(templateBTStorage)
            .WithPathHolder(pathHolder)
            .WithSystemLogsSender(systemLogsSender)
            .BuildRootNode()
    );

    return tree;
}

void TickTree(TTreePtr tree, size_t tickCount, std::function<bool()> breakHook) {
    for (size_t i = 0; i < tickCount; ++i) {
        TTickResult tickResult = TNodeSuccess{ENodeStatus::RUNNING, ""};
        while (tickResult && tickResult.Success().Status == ENodeStatus::RUNNING) {
            tickResult = tree->Tick();
        }
        if (!tickResult || (tickResult.Success().Status != ENodeStatus::SUCCESS)) {
            NLogEvent::TBehaviourTreeTickError ev;
            ev.SetType("bad tick result");
            ev.SetTitle(TConsoleRenderer(false).Render(tree));
            ev.SetTreeId(tree->GetTreeId());
            logger.SpawnFrame()->LogEvent(ELogPriority::TLOG_CRIT, ev);
        }
        if (breakHook()) {
            break;
        }
        Sleep(TDuration::MilliSeconds(500));
    }
}

void AssertIsEqual(const google::protobuf::Message& messageFirst, const google::protobuf::Message& messageSecond, bool statement) {
    TString msgAsStringFirst;
    TString msgAsStringSecond;
    google::protobuf::TextFormat::PrintToString(messageFirst, &msgAsStringFirst);
    google::protobuf::TextFormat::PrintToString(messageSecond, &msgAsStringSecond);
    UNIT_ASSERT_EQUAL_C(
        google::protobuf::util::MessageDifferencer::Equals(
            messageFirst
            , messageSecond
        )
        , statement
        , TStringBuilder()
            << "\n" << msgAsStringFirst
            << "\n" << msgAsStringSecond
    );
}

} // namespace NInfra::NPodAgent::NTreeTest
