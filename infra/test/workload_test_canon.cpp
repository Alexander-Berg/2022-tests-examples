#include "workload_test_canon.h"

#include <infra/pod_agent/libs/behaviour/bt/nodes/base/private_util.h>
#include <infra/pod_agent/libs/pod_agent/object_meta/test_lib/test_functions.h>
#include <infra/pod_agent/libs/porto_client/porto_test_lib/test_functions.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/tests_data.h>

#include <util/folder/dirut.h>
#include <util/system/shellcommand.h>
#include <util/system/fs.h>

namespace NInfra::NPodAgent::NTreeTest {

void ITestWorkloadCanon::PrepareBox() {
    BoxPrepared_ = true;
    BoxStatusRepository_->AddObject(NObjectMetaTestLib::CreateBoxMetaSimple(BoxId_));
    const TString volumePath = PathHolder_->GetBoxRootfsPath(BoxId_);
    NFs::MakeDirectoryRecursive(volumePath);

    SafePorto_->CreateVolume(volumePath, PathHolder_->GetBoxRootfsPersistentStorage(BoxId_), "", {}, 0, "", EPortoVolumeBackend::Auto, {""}, {}, false).Success();
    SafePorto_->CreateRecursive(PathHolder_->GetBoxContainer(BoxId_)).Success();
    SafePorto_->SetProperty(PathHolder_->GetBoxContainer(BoxId_), EPortoContainerProperty::Root, PathHolder_->GetBoxRootfsPath(BoxId_)).Success();
    SafePorto_->Start(PathHolder_->GetBoxContainer(BoxId_)).Success();
    SafePorto_->SetProperty(PathHolder_->GetBoxContainer(BoxId_), EPortoContainerProperty::Private, PackContainerPrivate({CP_READY, "box_tree_hash"})).Success();

    // TODO(DEPLOY-4278): Refactor test with creating root volume for box
    TStringBuilder commandBuilder;
    commandBuilder << "bash -c \""
        << "cd " << volumePath << ";"
        << "tar xf " << RealPath(GetWorkPath()) << "/layer.tar.xz;"
        << "\"";

    TShellCommand shellCommand(commandBuilder);
    shellCommand.Run();
    shellCommand.Wait();
    UNIT_ASSERT_C(shellCommand.GetExitCode(), shellCommand.GetError());

    i32 exitCode = NPortoTestLib::IsInsideSandboxPortoIsolation() ? 0 : 2;
    UNIT_ASSERT_EQUAL_C(exitCode, *(shellCommand.GetExitCode()), *(shellCommand.GetExitCode()) << " :: " << shellCommand.GetError());
}

void ITestWorkloadCanon::PrepareWorkload(ui32 initSize) {
    WorkloadStatusRepository_->AddObject(
        TWorkloadMeta(
            WorkloadId_
            , 0
            , 0
            , BoxId_

            , TWorkloadMeta::TContainerInfo("start_container")
            , NObjectMetaTestLib::GenerateInitContainerInfo(initSize)

            , TWorkloadMeta::TEmptyInfo()
            , TWorkloadMeta::TEmptyInfo()
            , TWorkloadMeta::TEmptyInfo()
            , TWorkloadMeta::TEmptyInfo()
        )
    );
    WorkloadStatusRepository_->UpdateObjectTargetState(WorkloadId_, API::EWorkloadTarget_ACTIVE);
    WorkloadStatusRepositoryInternal_->AddObject(WorkloadId_);
}

TInet6StreamSocket ITestWorkloadCanon::CreateBindedSocket(ui16 port) {
    TInet6StreamSocket listenSocket(socket(AF_INET6, SOCK_STREAM, 0));
    TSockAddrInet6 addr("::", port);

    UNIT_ASSERT_NO_EXCEPTION(listenSocket.CheckSock());
    {
        SOCKET sock = listenSocket;
        const int yes = 1;
        ::setsockopt(sock, SOL_SOCKET, SO_REUSEADDR, (const char*)&yes, sizeof(yes));
    }

    UNIT_ASSERT_EQUAL_C(listenSocket.Bind(&addr), 0, strerror(errno));
    return listenSocket;
}

THttpService ITestWorkloadCanon::CreateAndStartHttpService(const THttpServiceConfig& config, TRequestRouterPtr router) {
    auto fakeFrame = logger.SpawnFrame();
    THttpService httpService(config, router);
    httpService.Start(fakeFrame);
    return httpService;
}

void ITestWorkloadCanon::RemoveStorage() {
    {
        auto result = SafePorto_->UnlinkVolume(PathHolder_->GetBoxRootfsPath(BoxId_));
        if (!result) {
            Cerr << ToString(result.Error()) << Endl;
        }
    }
    {
        auto result = SafePorto_->RemoveStorage(PathHolder_->GetBoxRootfsPersistentStorage(BoxId_));
        if (!result) {
            Cerr << ToString(result.Error()) << Endl;
        }
    }
}

} // namespace NInfra::NPodAgent::NTreeTest
