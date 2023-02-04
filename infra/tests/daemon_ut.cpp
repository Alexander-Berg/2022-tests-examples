#include "main.h"

#include <infra/pod_agent/libs/daemon/tests/test_lib/test_canon.h>
#include <infra/pod_agent/libs/daemon/tests/test_lib/test_functions.h>
#include <infra/pod_agent/libs/path_util/path_holder.h>
#include <infra/pod_agent/libs/pod_agent/cache_file/cache_file.h>
#include <infra/pod_agent/libs/pod_agent/core_service/core_service.h>
#include <infra/pod_agent/libs/porto_client/porto_test_lib/test_functions.h>

#include <library/cpp/digest/md5/md5.h>
#include <library/cpp/protobuf/json/json2proto.h>
#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/tests_data.h>

#include <util/datetime/base.h>
#include <util/folder/dirut.h>
#include <util/random/mersenne.h>
#include <util/stream/file.h>
#include <util/string/cast.h>

#include <google/protobuf/util/message_differencer.h>

#include <yt/yt/core/ytree/convert.h>

namespace NInfra::NPodAgent::NDaemonTest  {

Y_UNIT_TEST_SUITE(DaemonTest) {

Y_UNIT_TEST(TestAliveAndPing) {
    class TTest : public ITestCanon {
    public:
        TTest(): ITestCanon("TestAliveAndPing") {
        }

        void Init() final {
            // to be sure that pod_agent handle the error.
            Params_.LockSelfMemory = true;
            ITestCanon::Init();
        }

        void DoTest() final {
            TString aliveResponse = HttpAliveHandle_.Alive();
            TString pingResponse = Handle_.Ping();

            Cout << aliveResponse << Endl;
            Cout << pingResponse << Endl;
        }
    };

    TTest test;
    test.Test();
}

Y_UNIT_TEST(TestEmptySpec) {
    class TTest : public ITestCanon {
    public:
        TTest(): ITestCanon("TestEmptySpec") {
        }

        void DoTest() final {
            API::TPodAgentRequest request;
            request.set_spec_timestamp(1);
            API::TPodAgentStatus response = CorrectPodAgentStatus(Handle_.UpdatePodAgentRequest(request));

            Cout << response.Utf8DebugString() << Endl;
        }
    };

    TTest test;
    test.Test();
}

Y_UNIT_TEST(TestFiles) {
    class TTest: public ITestCanon {
    public:
        TTest(): ITestCanon("TestFiles") {
        }

        void DoTest() final {
            auto workloadSpec = SpecHolder_.GetTestFilesSpec();
            API::TPodAgentStatus response = UpdatePodAgentRequestAndWaitForReady(workloadSpec, 130);

            UNIT_ASSERT_EQUAL_C(response.boxes(0).inits_size(), 5, response.boxes(0).inits_size());
            // Human readable format
            for (const auto& init : response.boxes(0).inits()) {
                Cout << init.last().stdout_tail() << Endl;
                Cout << "==========================" << Endl;
            }

            Cout << response.Utf8DebugString() << Endl;
        }
    };

    TTest test;
    test.Test();
}

Y_UNIT_TEST(TestStdoutFile) {
    class TTest: public ITestCanon {
    public:
        TTest(): ITestCanon("TestStdoutFile") {
        }

        void DoTest() final {
            auto workloadSpec = SpecHolder_.GetTestStdoutFileSpec();

            API::TPodAgentStatus response = UpdatePodAgentRequestAndWaitForReady(workloadSpec, 130);

            const TString rootfsPath = Params_.VolumeStorage + "/rootfs_test_box";
            const TString path = rootfsPath + "/stdout_file";
            UNIT_ASSERT(NFs::Exists(path));

            TString stdoutOutput  = TUnbufferedFileInput(path).ReadAll();
            UNIT_ASSERT_EQUAL_C(stdoutOutput, "this is an stdout file\n", stdoutOutput);

            Cout << response.Utf8DebugString() << Endl;
        }
    };

    TTest test;
    test.Test();
}

Y_UNIT_TEST(TestInvalidStartCmd) {
    class TTest: public ITestCanon {
    public:
        TTest(): ITestCanon("TestInvalidStartCmd") {
        }

        void DoTest() final {
            auto workloadSpec = SpecHolder_.GetInvalidStartCmdSpec();
            auto readyHook = [](const API::TPodAgentStatus& status) {
                return status.boxes(0).state() == API::EBoxState_READY;
            };

            UpdatePodAgentRequestAndWaitForReady(workloadSpec, 130, readyHook);

            i32 cntCalls = -CountPortoCalls();

            const TDuration workDuration = TDuration::Seconds(10);
            Sleep(workDuration);

            cntCalls += CountPortoCalls();

            PortoRPS_ = 1.0 * cntCalls / workDuration.Seconds();

            // If we use longer timeouts upon recieving invalid state, we get approximately 4 rps
            // If we don't do it, we get approximately 19 rps
            UNIT_ASSERT_C(PortoRPS_ < 10, PortoRPS_);
        }

    private:
        size_t CountPortoCalls() {
            const auto portoCallsMap = GetPortoCallsMap();
            UNIT_ASSERT_C(portoCallsMap.contains("requests_total"), "'requests_total' not found in porto calls map");
            return portoCallsMap.at("requests_total");
        }

    public:
        double PortoRPS_;
    };

    TTest test;
    test.Test();

    UNIT_ADD_METRIC("porto_rps", test.PortoRPS_);
}

Y_UNIT_TEST(TestOneLayer) {
    class TTest : public ITestCanon {
    public:
        TTest(): ITestCanon("TestOneLayer") {
        }

        void DoTest() final {
            API::TPodAgentRequest layerSpec = SpecHolder_.GetLayerSpec();

            API::TPodAgentStatus addLayerRet = CorrectPodAgentStatus(Handle_.UpdatePodAgentRequest(layerSpec));

            API::TPodAgentStatus curRet = UpdatePodAgentRequestAndWaitForReady(layerSpec, 20);

            Cout << addLayerRet.Utf8DebugString() << Endl;
            Cout << curRet.Utf8DebugString() << Endl;
        }
    };

    TTest test;
    test.Test();
}

Y_UNIT_TEST(TestLayerBadName) {
    class TTest : public ITestCanon {
    public:
        TTest(): ITestCanon("TestLayerBadName") {
        }

        void DoTest() final {
            // Test with this layer https://sandbox.yandex-team.ru/resource/1133643356/view
            // Name is "Layer name with spaces and $HOSTNAME.tar.gz"
            API::TPodAgentRequest layerSpec = SpecHolder_.GetLayerBadNameSpec();

            API::TPodAgentStatus addLayerRet = CorrectPodAgentStatus(Handle_.UpdatePodAgentRequest(layerSpec));

            API::TPodAgentStatus curRet = UpdatePodAgentRequestAndWaitForReady(layerSpec, 100);

            Cout << addLayerRet.Utf8DebugString() << Endl;
            Cout << curRet.Utf8DebugString() << Endl;
        }
    };

    TTest test;
    test.Test();
}

Y_UNIT_TEST(TestOneStaticResource) {
    class TTest : public ITestCanon {
    public:
        TTest(): ITestCanon("TestOneStaticResource") {
        }

        void DoTest() final {
            API::TPodAgentStatus addStaticResourceRet = CorrectPodAgentStatus(Handle_.UpdatePodAgentRequest(SpecHolder_.GetStaticResourceSpec(1)));

            API::TPodAgentStatus specTimestamp1Ret = UpdatePodAgentRequestAndWaitForReady(SpecHolder_.GetStaticResourceSpec(1), 20);
            API::TPodAgentStatus specTimestamp2Ret  = UpdatePodAgentRequestAndWaitForReady(SpecHolder_.GetStaticResourceSpec(2), 20);

            Cout << addStaticResourceRet.Utf8DebugString() << Endl;
            Cout << specTimestamp1Ret.Utf8DebugString() << Endl;
            Cout << specTimestamp2Ret.Utf8DebugString() << Endl;
        }
    };

    TTest test;
    test.Test();
}

Y_UNIT_TEST(TestUpdateOneLayer) {
    class TTest : public ITestCanon {
    public:
        TTest(): ITestCanon("TestUpdateOneLayer") {
        }

        void DoTest() final {
            auto firstLayerSpec = SpecHolder_.GetLayerSpec();
            auto otherLayerSpec = SpecHolder_.GetOtherLayerSpec();
            API::TPodAgentRequest emptySpec;

            firstLayerSpec.set_spec_timestamp(1);
            API::TPodAgentStatus firstLayerRet = UpdatePodAgentRequestAndWaitForReady(firstLayerSpec, 20);

            emptySpec.set_spec_timestamp(2);
            CorrectPodAgentStatus(Handle_.UpdatePodAgentRequest(emptySpec));
            API::TPodAgentStatus emptyRet = CorrectPodAgentStatus(Handle_.UpdatePodAgentRequest(emptySpec));

            otherLayerSpec.set_spec_timestamp(3);
            API::TPodAgentStatus otherLayerRet = UpdatePodAgentRequestAndWaitForReady(otherLayerSpec, 20);

            Cout << firstLayerRet.Utf8DebugString() << Endl;
            Cout << emptyRet.Utf8DebugString() << Endl;
            Cout << otherLayerRet.Utf8DebugString() << Endl;
        }
    };

    TTest test;
    test.Test();
}

Y_UNIT_TEST(TestVolumeSpec) {
    class TTest : public ITestCanon {
    public:
        TTest(): ITestCanon("TestVolumeSpec") {
        }

        void DoTest() final {
            auto volumeSpec = SpecHolder_.GetVolumeSpec();
            CorrectPodAgentStatus(Handle_.UpdatePodAgentRequest(volumeSpec));

            API::TPodAgentStatus status = UpdatePodAgentRequestAndWaitForReady(volumeSpec, 240);

            Cout << status.Utf8DebugString() << Endl;
        }
    };

    TTest test;
    test.Test();
}

Y_UNIT_TEST(TestUpdateWorkloadSpec) {
    class TTest : public ITestCanon {
    public:
        TTest(): ITestCanon("TestUpdateWorkloadSpec") {
        }

        void DoTest() final {
            const TString volumeName = Params_.VolumeStorage + "/rootfs_test_box";

            auto workloadStartSpec = SpecHolder_.GetWorkloadUpdateStartSpec();
            auto workloadEndSpec = SpecHolder_.GetWorkloadUpdateEndSpec();

            API::TPodAgentStatus workloadRetStart = UpdatePodAgentRequestAndWaitForReady(workloadStartSpec, 120);

            API::TPodAgentStatus workloadRet = CorrectPodAgentStatus(Handle_.UpdatePodAgentRequest(workloadEndSpec));
            UNIT_ASSERT_UNEQUAL_C(
                API::EConditionStatus_TRUE
                , workloadRet.workloads(0).ready().status()
                , "Workload can't be READY so fast: " << NProtobufJson::Proto2Json(workloadRet.workloads(0))
            );
            API::TPodAgentStatus workloadRetEnd = UpdatePodAgentRequestAndWaitForReady(workloadEndSpec, 120);

            TString stopOutput  = TUnbufferedFileInput(volumeName + "/stop_file.txt").ReadAll();
            UNIT_ASSERT_EQUAL_C(stopOutput, "stopped\n", stopOutput);

            UNIT_ASSERT(!NFs::Exists(volumeName + "/destroy_file.txt"));

            Cout << workloadRetStart.Utf8DebugString() << Endl;
            Cout << workloadRetEnd.Utf8DebugString() << Endl;
        }
    };

    TTest test;
    test.Test();
}

Y_UNIT_TEST(TestRemoveWorkloadSpec) {
    class TTest : public ITestCanon {
    public:
        TTest(): ITestCanon("TestRemoveWorkloadSpec") {
        }

        void DoTest() final {
            const TString volumeName = Params_.VolumeStorage + "/rootfs_test_box";

            auto workloadStartSpec = SpecHolder_.GetWorkloadUpdateStartSpec();
            auto workloadEndSpec = SpecHolder_.GetWorkloadRemoveEndSpec();

            API::TPodAgentStatus workloadRetStart = UpdatePodAgentRequestAndWaitForReady(workloadStartSpec, 120);

            API::TPodAgentStatus workloadRet = CorrectPodAgentStatus(Handle_.UpdatePodAgentRequest(workloadEndSpec));
            UNIT_ASSERT_UNEQUAL_C(
                API::EConditionStatus_TRUE
                , workloadRet.workloads(0).ready().status()
                , "Workload can't be READY so fast: " << NProtobufJson::Proto2Json(workloadRet.workloads(0))
            );
            API::TPodAgentStatus workloadRetEnd = UpdatePodAgentRequestAndWaitForReady(workloadEndSpec, 120);

            TString stopOutput  = TUnbufferedFileInput(volumeName + "/stop_file.txt").ReadAll();
            UNIT_ASSERT_EQUAL_C(stopOutput, "stopped\n", stopOutput);

            TString destroyOutput  = TUnbufferedFileInput(volumeName + "/destroy_file.txt").ReadAll();
            UNIT_ASSERT_EQUAL_C(destroyOutput, "destroyed\n", stopOutput);

            Cout << workloadRetStart.Utf8DebugString() << Endl;
            Cout << workloadRetEnd.Utf8DebugString() << Endl;
        }
    };

    TTest test;
    test.Test();
}

Y_UNIT_TEST(TestPodAttributesAndGetStatus) {
    class TTest : public ITestCanon {
    public:
        TTest(): ITestCanon("TestPodAttributesAndGetStatus") {
        }

        void DoTest() final {
            auto workloadSpec = SpecHolder_.GetExtendedWorkloadSpec();

            auto readyHook = [](const API::TPodAgentStatus& status) {
                return CheckAllObjectsReady(status)
                    && status.workloads()[0].liveness_status().container_status().current().state() == API::EContainerState_WAITING_RESTART;
            };

            UpdatePodAgentRequestAndWaitForReady(workloadSpec, 130, readyHook);

            Cout << HttpPublicHandle_.PodAttributesJson() << Endl;
            Cout << HttpPublicHandle_.PodStatusJson() << Endl;
            Cout << GetPodAgentStatus().Utf8DebugString() << Endl;
        }
    };

    TTest test;
    test.Test();
}

Y_UNIT_TEST(TestSaveSpec) {
    class TTest : public ITestCanon {
    public:
        TTest(): ITestCanon("TestSaveSpec") {
        }

        void DoTest() final {
            auto layerSpec = SpecHolder_.GetLayerSpec();

            API::TPodAgentStatus ret1 = CorrectPodAgentStatus(Handle_.UpdatePodAgentRequest(layerSpec));

            API::TPodAgentStatus ret2 = UpdatePodAgentRequestAndWaitForReady(layerSpec, 20);

            Handle_.Shutdown();
            DestroyContainersWithPrefix(PortoClient_, Params_.ContainersPrefix);
            RemoveVolumes();
            NFs::RemoveRecursive(Params_.CacheVolumePath);
            NFs::MakeDirectoryRecursive(Params_.CacheVolumePath);
            PortoClient_->CreateVolume(Params_.CacheVolumePath, "", "", {}, 0, "", EPortoVolumeBackend::Auto, {""}, {}, false).Success();
            Sleep(TDuration::MilliSeconds(500));

            Init();
            API::TPodAgentStatus ret3 = UpdatePodAgentRequestAndWaitForReady(layerSpec, 20);

            // first response ret1 is flaky - do not canonize it
            Cout << ret2.Utf8DebugString() << Endl;
            Cout << ret3.Utf8DebugString() << Endl;
        }
    };

    TTest test;
    test.Test();
}

Y_UNIT_TEST(TestVolumePersistency) {
    class TTest : public ITestCanon {
    public:
        TTest(): ITestCanon("TestVolumePersistency") {
        }

        void DoTest() final {
            auto spec = SpecHolder_.GetTestVolumePersistencySpec();

            auto readyHookPositive = [](const API::TPodAgentStatus& status) {
                return status.workloads(0).start().positive_return_code_counter() > 0;
            };
            auto readyHookZero = [](const API::TPodAgentStatus& status) {
                return status.workloads(0).start().zero_return_code_counter() > 0;
            };

            auto ret1 = CorrectPodAgentStatus(Handle_.UpdatePodAgentRequest(spec));

            API::TPodAgentStatus ret2 = UpdatePodAgentRequestAndWaitForReady(spec, 120, readyHookPositive);
            UNIT_ASSERT_C(ret2.workloads(0).start().zero_return_code_counter() == 0, ret2.Utf8DebugString());
            UNIT_ASSERT_C(ret2.workloads(0).start().positive_return_code_counter() > 0, ret2.Utf8DebugString());

            Handle_.Shutdown();
            Sleep(TDuration::MilliSeconds(3000));
            RemoveVolumes();
            Init();

            API::TPodAgentStatus ret3 = UpdatePodAgentRequestAndWaitForReady(spec, 120, readyHookZero);
            UNIT_ASSERT_C(ret3.workloads(0).start().zero_return_code_counter() > 0, ret3.Utf8DebugString());
            UNIT_ASSERT_C(ret3.workloads(0).start().positive_return_code_counter() == 0, ret3.Utf8DebugString());
        }
    };

    TTest test;
    test.Test();
}

Y_UNIT_TEST(TestSensors) {
    class TTest : public ITestCanon {
    public:
        TTest(): ITestCanon("TestSensors") {
        }

        void DoTest() final {
            auto layerSpec = SpecHolder_.GetSensorsSpec();

            CorrectPodAgentStatus(Handle_.UpdatePodAgentRequest(layerSpec));
            API::TPodAgentStatus layerRet = UpdatePodAgentRequestAndWaitForReady(layerSpec, 120);

            for (TMultiUnistat::ESignalNamespace signalNamespace : {TMultiUnistat::ESignalNamespace::INFRA, TMultiUnistat::ESignalNamespace::USER}) {
                for (TMultiUnistat::ESignalPriority signalPriority : {TMultiUnistat::ESignalPriority::DEBUG, TMultiUnistat::ESignalPriority::USER_INFO, TMultiUnistat::ESignalPriority::INFRA_INFO}) {
                    Cout << Endl << ToString(signalNamespace) << " " << ToString(signalPriority) << ":" << Endl;
                    NJson::TJsonValue json;
                    if (signalNamespace == TMultiUnistat::ESignalNamespace::INFRA) {
                        ReadJsonTree(Handle_.Sensors(signalPriority), &json, true);
                    } else {
                        ReadJsonTree(Handle_.UserSensors(signalPriority), &json, true);
                    }
                    CheckSensorsDataJson(json);
                }
            }
        }

    private:
        void CheckSensorsDataJson(const NJson::TJsonValue& json) {
            UNIT_ASSERT_C(json.IsArray(), json.GetStringRobust());
            for (auto item : json.GetArray()) {
                UNIT_ASSERT_C(item.IsArray(), item.GetStringRobust());
                UNIT_ASSERT_C(item.GetArray().size() == 2, item.GetStringRobust());
                UNIT_ASSERT_C(item.GetArray()[0].IsString(), item.GetArray()[0].GetStringRobust());
                UNIT_ASSERT_C(item.GetArray()[1].IsDouble() || item.GetArray()[1].IsArray(), item.GetArray()[1].GetStringRobust());

                Cout << item.GetArray()[0].GetStringRobust() << Endl;
            }
        }
    };

    TTest test;
    test.Test();
}

Y_UNIT_TEST(TestWorkloadAndGC) {
    class TTest : public ITestCanon {
    public:
        TTest(): ITestCanon("TestWorkloadAndGC") {
        }

        void Init() final {
            Params_.GCPeriodMS = 1000;
            ITestCanon::Init();
        }

        void DoTest() final {
            auto workloadSpec = SpecHolder_.GetWorkloadSpec();
            API::TPodAgentRequest emptySpec;

            auto readyHookWorklaod = [](const API::TPodAgentStatus& status) {
                return CheckAllObjectsReady(status)
                    && status.workloads()[0].liveness_status().container_status().current().state() == API::EContainerState_WAITING_RESTART;
            };

            emptySpec.set_spec_timestamp(1);
            auto emptyRet1 = CorrectPodAgentStatus(Handle_.UpdatePodAgentRequest(emptySpec));

            workloadSpec.set_spec_timestamp(2);
            API::TPodAgentStatus workloadRet = UpdatePodAgentRequestAndWaitForReady(workloadSpec, 180, readyHookWorklaod);

            emptySpec.set_spec_timestamp(3);
            emptyRet1.set_spec_timestamp(3);

            auto readyHookEmpty = [&emptyRet1](const API::TPodAgentStatus& status) {
                return status.Utf8DebugString() == emptyRet1.Utf8DebugString();
            };

            API::TPodAgentStatus emptyRet2 = UpdatePodAgentRequestAndWaitForReady(emptySpec, 180, readyHookEmpty);

            Cout << emptyRet1.Utf8DebugString() << Endl;
            Cout << workloadRet.Utf8DebugString() << Endl;
            Cout << emptyRet2.Utf8DebugString() << Endl;

            for (size_t i = 0; i < 60; ++i) {
                if (HasGCFinished()) {
                    break;
                }
                Sleep(TDuration::MilliSeconds(500));
            }

            auto GCCheck = HasGCFinished();
            UNIT_ASSERT_C(GCCheck, GCCheck.Error());
        }

        TExpected<void, TString> HasGCFinished() {
            TString layersDir = Params_.ResourcesDownloadPath + "/default/layer";
            if (!NFs::Exists(layersDir)) {
                return "Resources path '" + layersDir + "' doesn't exist.";
            }
            if (NFs::Exists(layersDir + "/MyData1")) {
                return "Directory '" + layersDir + "/MyData1' still exists.";
            }
            if (NFs::Exists(layersDir + "/MyData2")) {
                return "Directory '" + layersDir + "/MyData2 still exists.";
            }
            if (NFs::Exists(layersDir + "/MyRootFS")) {
                return "Directory '" + layersDir + "/MyRootFS still exists.";
            }

            if (!NFs::Exists(Params_.VolumeStorage)) {
                return "Volume storage '" + Params_.VolumeStorage + "' doesn't exist.";
            }

            auto resourceVolumeExists = PortoClient_->ListVolumes(Params_.ResourcesDownloadPath + "/default");
            if (!resourceVolumeExists && resourceVolumeExists.Error().Code == EPortoError::VolumeNotFound) {
                return "Resource volume '" + Params_.ResourcesDownloadPath + "/default" + "' doesn't exist.";
            }

            auto cacheVolumeExists = PortoClient_->ListVolumes(Params_.CacheVolumePath);
            if (!cacheVolumeExists && cacheVolumeExists.Error().Code == EPortoError::VolumeNotFound) {
                return "Cache volume '" + Params_.CacheVolumePath + "' doesn't exist.";
            }

            {
                auto list = PortoClient_->List().Success();
                const TString resourceGangMeta = Params_.ContainersPrefix + "resource_gang_meta";
                bool hasResourceGangMeta = false;
                for (auto container : list) {
                    TString name = TString(container);
                    if (name.StartsWith(Params_.ContainersPrefix)) {
                        if (name == resourceGangMeta) {
                            hasResourceGangMeta = true;
                        } else {
                            return "Container '" + name + "' still exists.";
                        }
                    }
                }
                if (!hasResourceGangMeta) {
                    return "Resource gang meta container '" + resourceGangMeta + "' was removed";
                }
            }
            {
                auto list = PortoClient_->ListLayers().Success();
                for (auto layer : list) {
                    if (layer.name().StartsWith(Params_.LayerPrefix)) {
                        return "Layer '" + layer.name() + "' still exists.";
                    }
                }
            }
            {
                auto list = PortoClient_->ListVolumes().Success();
                for (auto volume : list) {
                    if (volume.path().StartsWith(Params_.VolumeStorage)) {
                        return "Volume '" + volume.path() + "' still exists.";
                    }
                }
            }

            return TExpected<void, TString>::DefaultSuccess();
        }
    };

    TTest test;
    test.Test();
}

Y_UNIT_TEST(TestUpdateSpecWithSelectiveRebuild) {
    class TTest : public ITestCanon {
    public:
        TTest(): ITestCanon("TestUpdateSpecWithSelectiveRebuild") {
        }

        void Init() final {
            Params_.GCPeriodMS = 15000;
            Params_.TreesUpdatePeriodMS = 1000;
            ITestCanon::Init();
        }

        void DoTest() final {
            const TString volumeName = Params_.VolumeStorage + "/volume_test_volume1";

            API::TPodAgentStatus specTimestamp1Ret = UpdatePodAgentRequestAndWaitForReady(SpecHolder_.GetSelectiveRebuildSpec(1), 240);

            API::TPodAgentStatus specTimestamp2StartRet = CorrectPodAgentStatus(Handle_.UpdatePodAgentRequest(SpecHolder_.GetSelectiveRebuildSpec(2)));
            API::TPodAgentStatus specTimestamp2EndRet = UpdatePodAgentRequestAndWaitForReady(SpecHolder_.GetSelectiveRebuildSpec(2), 240);

            API::TPodAgentStatus specTimestamp3StartRet = CorrectPodAgentStatus(Handle_.UpdatePodAgentRequest(SpecHolder_.GetSelectiveRebuildSpec(3)));
            API::TPodAgentStatus specTimestamp3EndRet = UpdatePodAgentRequestAndWaitForReady(SpecHolder_.GetSelectiveRebuildSpec(3), 240);

            {
                TString stopOutput  = TUnbufferedFileInput(volumeName + "/stop_file_1.txt").ReadAll();
                UNIT_ASSERT_EQUAL_C(stopOutput, "stop1\n", stopOutput);
                NFs::Remove(volumeName + "/stop_file_1.txt");
            }

            API::TPodAgentStatus specTimestamp4StartRet = CorrectPodAgentStatus(Handle_.UpdatePodAgentRequest(SpecHolder_.GetSelectiveRebuildSpec(4)));
            // Remove flaky part - workload stop container may be either started or not started yet
            specTimestamp4StartRet.clear_workloads();

            API::TPodAgentStatus specTimestamp5EndRet = UpdatePodAgentRequestAndWaitForReady(SpecHolder_.GetSelectiveRebuildSpec(5), 240); // 5 is not a mistake

            {
                TString stopOutput  = TUnbufferedFileInput(volumeName + "/stop_file_1.txt").ReadAll();
                UNIT_ASSERT_EQUAL_C(stopOutput, "stop1\n", stopOutput);
                NFs::Remove(volumeName + "/stop_file_1.txt");
            }

            Cout << specTimestamp1Ret.Utf8DebugString() << Endl;

            Cout << specTimestamp2StartRet.Utf8DebugString() << Endl;
            Cout << specTimestamp2EndRet.Utf8DebugString() << Endl;

            Cout << specTimestamp3StartRet.Utf8DebugString() << Endl;
            Cout << specTimestamp3EndRet.Utf8DebugString() << Endl;

            Cout << specTimestamp4StartRet.Utf8DebugString() << Endl;
            Cout << specTimestamp5EndRet.Utf8DebugString() << Endl;
        }
    };

    TTest test;
    test.Test();
}

Y_UNIT_TEST(TestUpdateSpecWithSelectiveRebuildNoWait) {
    class TTest : public ITestCanon {
    public:
        TTest(): ITestCanon("TestUpdateSpecWithSelectiveRebuildNoWait") {
        }

        void Init() final {
            Params_.GCPeriodMS = 2000;
            Params_.TreesUpdatePeriodMS = 250;
            ITestCanon::Init();
        }

        void DoTest() final {
            const size_t cntIter = 100;
            const size_t sleepPeriod = 20;
            const size_t cycle = 7;

            for (size_t i = 0; i < cntIter; ++i) {
                if (i % sleepPeriod == 0) {
                    Sleep(TDuration::MilliSeconds(300));
                }
                auto currentSpec = SpecHolder_.GetSelectiveRebuildSpec(i % cycle + 1);
                currentSpec.set_spec_timestamp(i + 1);
                CorrectPodAgentStatus(Handle_.UpdatePodAgentRequest(currentSpec));
            }

            API::TPodAgentStatus endRet = UpdatePodAgentRequestAndWaitForReady(SpecHolder_.GetSelectiveRebuildSpec(cntIter + 1), 240);

            // Check flaky part
            UNIT_ASSERT_C(CheckAllObjectsReady(endRet), endRet.Utf8DebugString());
            for (auto& layer : endRet.mutable_resource_gang()->layers()) {
                UNIT_ASSERT_C(layer.id() == "Layer_MyData1" || layer.id() == "Layer_MyData2" || layer.id() == "Layer_MyRootFS", endRet.Utf8DebugString());
                UNIT_ASSERT_EQUAL_C(layer.spec_timestamp(), cntIter + 1, endRet.Utf8DebugString());
            }

            // Remove flaky part
            endRet.clear_resource_gang();
            for (auto volume : *endRet.mutable_volumes()) {
                volume.mutable_failed()->set_message("");
            }

            Cout << endRet.Utf8DebugString() << Endl;
        }
    };

    TTest test;
    test.Test();
}

Y_UNIT_TEST(TestUpdateSpecWithRemove) {
    class TTest : public ITestCanon {
    public:
        TTest(): ITestCanon("TestUpdateSpecWithRemove") {
        }

        void Init() final {
            Params_.GCPeriodMS = 5000;
            Params_.TreesUpdatePeriodMS = 1000;
            ITestCanon::Init();
        }

        void DoTest() final {
            const TString volumeName = Params_.VolumeStorage + "/volume_test_volume";

            API::TPodAgentStatus specTimestamp1Ret = UpdatePodAgentRequestAndWaitForReady(SpecHolder_.GetRemoveSpec(1), 240);

            API::TPodAgentStatus specTimestamp2StartRet = CorrectPodAgentStatus(Handle_.UpdatePodAgentRequest(SpecHolder_.GetRemoveSpec(2)));

            // Check flaky part
            UNIT_ASSERT_C(!CheckAllObjectsReady(specTimestamp2StartRet), specTimestamp2StartRet.Utf8DebugString());
            for (auto& layer : specTimestamp2StartRet.mutable_resource_gang()->layers()) {
                if (layer.id() == "Layer_MyData") {
                    UNIT_ASSERT_EQUAL_C(layer.ready().status(), API::EConditionStatus_TRUE, specTimestamp2StartRet.Utf8DebugString());
                } else if (layer.id() == "Layer_MyRootFS") {
                    UNIT_ASSERT_UNEQUAL_C(layer.ready().status(), API::EConditionStatus_TRUE, specTimestamp2StartRet.Utf8DebugString());
                    UNIT_ASSERT_EQUAL_C(layer.ready().message(), "Changing spec_timestamp", specTimestamp2StartRet.Utf8DebugString());
                } else {
                    UNIT_ASSERT_EQUAL_C(layer.id(), "Layer_MyRootFS_other", specTimestamp2StartRet.Utf8DebugString());
                }
            }

            // Remove flaky part
            specTimestamp2StartRet.clear_resource_gang();
            // Remove flaky part - workload stop container may be either started or not started yet
            specTimestamp2StartRet.clear_workloads();
            specTimestamp2StartRet.mutable_in_progress()->clear_reason();
            specTimestamp2StartRet.mutable_ready()->clear_reason();

            API::TPodAgentStatus specTimestamp2EndRet = UpdatePodAgentRequestAndWaitForReady(SpecHolder_.GetRemoveSpec(2), 240);

            TString stopOutput  = TUnbufferedFileInput(volumeName + "/stop_file.txt").ReadAll();
            UNIT_ASSERT_EQUAL_C(stopOutput, "stop\n", stopOutput);

            Cout << specTimestamp1Ret.Utf8DebugString() << Endl;
            Cout << specTimestamp2StartRet.Utf8DebugString() << Endl;
            Cout << specTimestamp2EndRet.Utf8DebugString() << Endl;
        }
    };

    TTest test;
    test.Test();
}

Y_UNIT_TEST(TestAddWithTargetCheck) {
    class TTest : public ITestCanon {
    public:
        TTest(): ITestCanon("TestAddWithTargetCheck") {
        }

        void Init() final {
            Params_.GCPeriodMS = 15000;
            Params_.TreesUpdatePeriodMS = 1000;
            ITestCanon::Init();
        }

        void DoTest() final {
            API::TPodAgentStatus specTimestamp1Ret = UpdatePodAgentRequestAndWaitForReady(SpecHolder_.GetAddWithTargetCheckSpec(1), 240);
            API::TPodAgentStatus specTimestamp2StartRet = CorrectPodAgentStatus(Handle_.UpdatePodAgentRequest(SpecHolder_.GetAddWithTargetCheckSpec(2)));

            UNIT_ASSERT_EQUAL_C(specTimestamp2StartRet.volumes(0).spec_timestamp(), 0, specTimestamp2StartRet.Utf8DebugString());
            UNIT_ASSERT_EQUAL_C(specTimestamp2StartRet.boxes(0).spec_timestamp(), 0, specTimestamp2StartRet.Utf8DebugString());
            UNIT_ASSERT_EQUAL_C(specTimestamp2StartRet.workloads(0).spec_timestamp(), 0, specTimestamp2StartRet.Utf8DebugString());

            API::TPodAgentStatus specTimestamp2EndRet = UpdatePodAgentRequestAndWaitForReady(SpecHolder_.GetAddWithTargetCheckSpec(2), 240);

            Cout << specTimestamp1Ret.Utf8DebugString() << Endl;
            Cout << specTimestamp2EndRet.Utf8DebugString() << Endl;
        }
    };

    TTest test;
    test.Test();
}

Y_UNIT_TEST(TestChaosInSpec) {
    class TTest : public ITestCanon {
    public:
        TTest(): ITestCanon("TestChaosInSpec") {
        }

        void Init() final {
            Params_.GCPeriodMS = 5000;
            Params_.TreesUpdatePeriodMS = 1000;
            Params_.PortoPoolSize = 12;
            ITestCanon::Init();
        }

        void DoTest() final {
            size_t cntIter = 45;

            API::TPodAgentRequest emptySpec;
            TVector<API::TPodAgentRequest> specs = {
                emptySpec
                , SpecHolder_.GetLayerSpec()
                , SpecHolder_.GetOtherLayerSpec()
                , SpecHolder_.GetVolumeSpec()
                , SpecHolder_.GetTestVolumePersistencySpec()
                , SpecHolder_.GetWorkloadUpdateStartSpec()
                , SpecHolder_.GetWorkloadUpdateEndSpec()
                , SpecHolder_.GetWorkloadSpec()
                , SpecHolder_.GetTestInContainerWorkloadSpec()
                , SpecHolder_.GetRemoveSpec(1)
                , SpecHolder_.GetRemoveSpec(2)
                , SpecHolder_.GetSelectiveRebuildSpec(1)
                , SpecHolder_.GetSelectiveRebuildSpec(2)
                , SpecHolder_.GetSelectiveRebuildSpec(3)
                , SpecHolder_.GetSelectiveRebuildSpec(4)
            };

            // patch stop and destroy policy
            for (auto& spec : specs) {
                for (size_t i = 0; i < spec.mutable_spec()->mutable_workloadsSize(); ++i) {
                    API::TWorkload* workload = spec.mutable_spec()->mutable_workloads(i);
                    TVector<API::TTimeLimit*> timeLimits;

                    if (workload->has_stop_policy()) {
                        workload->mutable_stop_policy()->set_max_tries(2);

                        if (workload->mutable_stop_policy()->has_container()) {
                            timeLimits.push_back(workload->mutable_stop_policy()->mutable_container()->mutable_time_limit());
                        } else if (workload->mutable_stop_policy()->has_http_get()) {
                            timeLimits.push_back(workload->mutable_stop_policy()->mutable_http_get()->mutable_time_limit());
                        }
                    }

                    if (workload->has_destroy_policy()) {
                        workload->mutable_destroy_policy()->set_max_tries(2);

                        if (workload->mutable_destroy_policy()->has_container()) {
                            timeLimits.push_back(workload->mutable_destroy_policy()->mutable_container()->mutable_time_limit());
                        } else if (workload->mutable_destroy_policy()->has_http_get()) {
                            timeLimits.push_back(workload->mutable_destroy_policy()->mutable_http_get()->mutable_time_limit());
                        }
                    }

                    for (API::TTimeLimit* timeLimit : timeLimits) {
                        timeLimit->set_restart_period_back_off(0);
                        timeLimit->set_restart_period_scale_ms(0);
                        timeLimit->set_initial_delay_ms(0);
                        timeLimit->set_max_execution_time_ms(1000);
                        timeLimit->set_max_restart_period_ms(1000);
                        timeLimit->set_min_restart_period_ms(1000);
                    }
                }
            }

            TMersenne<size_t> rnd(12345);
            for (size_t i = 0; i < cntIter; ++i) {
                API::TPodAgentRequest currentSpec = specs[rnd.GenRand() % specs.size()];
                currentSpec.set_spec_timestamp(i + 1);
                Handle_.UpdatePodAgentRequest(currentSpec);
                Sleep(TDuration::MilliSeconds(300));
            }

            API::TPodAgentRequest currentSpec = SpecHolder_.GetChaosEndSpec();
            currentSpec.set_spec_timestamp(cntIter + 1);
            API::TPodAgentStatus endRet = UpdatePodAgentRequestAndWaitForReady(currentSpec, 240);

            // Remove flaky part
            for (auto& layer : *endRet.mutable_resource_gang()->mutable_layers()) {
                layer.set_download_attempts_counter(42);
                layer.set_verification_attempts_counter(42);
            }

            Cout << endRet.Utf8DebugString() << Endl;
        }
    };

    TTest test;
    test.Test();
}

Y_UNIT_TEST(TestWaitingForLayers) {
    class TTest : public ITestCanon {
    public:
        TTest(): ITestCanon("TestWaitingForLayers") {
        }

        void Init() final {
            Params_.GCPeriodMS = 5000;
            Params_.TreesUpdatePeriodMS = 1000;
            ITestCanon::Init();
        }

        void DoTest() final {
            auto readyHookRev2 = [](const API::TPodAgentStatus& status) {
                return status.resource_gang().layers(0).verification_attempts_counter() > 3
                       && status.resource_gang().layers(1).verification_attempts_counter() > 3
                       && status.resource_gang().layers(0).spec_timestamp() == 2
                       && status.resource_gang().layers(1).spec_timestamp() == 2
                       && status.volumes(0).spec_timestamp() == 2
                       && status.boxes(0).spec_timestamp() == 2
                       && status.workloads(0).spec_timestamp() == 2;
            };

            API::TPodAgentStatus specTimestamp1Ret = UpdatePodAgentRequestAndWaitForReady(SpecHolder_.GetWaitingForLayersSpec(1), 240);
            API::TPodAgentStatus specTimestamp2Ret = UpdatePodAgentRequestAndWaitForReady(SpecHolder_.GetWaitingForLayersSpec(2), 360, readyHookRev2);  // retries needs more time
            API::TPodAgentStatus specTimestamp3Ret = UpdatePodAgentRequestAndWaitForReady(SpecHolder_.GetWaitingForLayersSpec(3), 240);

            // Check flaky part
            UNIT_ASSERT_EQUAL_C(specTimestamp2Ret.resource_gang().layers(0).ready().status(), API::EConditionStatus_FALSE, specTimestamp2Ret.Utf8DebugString());
            UNIT_ASSERT_EQUAL_C(specTimestamp2Ret.resource_gang().layers(1).ready().status(), API::EConditionStatus_FALSE, specTimestamp2Ret.Utf8DebugString());

            // Remove flaky part
            specTimestamp2Ret.clear_resource_gang();
            specTimestamp2Ret.mutable_ready()->clear_reason(); // ELayerState_VERIFYING/ELayerState_DOWNLOADING flapping
            specTimestamp2Ret.mutable_in_progress()->clear_reason(); // ELayerState_VERIFYING/ELayerState_DOWNLOADING flapping

            Cout << specTimestamp1Ret.Utf8DebugString() << Endl;
            Cout << specTimestamp2Ret.Utf8DebugString() << Endl;
            Cout << specTimestamp3Ret.Utf8DebugString() << Endl;
        }
    };

    TTest test;
    test.Test();
}

Y_UNIT_TEST(TestSecretsWithWorkload) {
    class TTest : public ITestCanon {
    public:
        TTest(): ITestCanon("TestSecretsWithWorkload") {
        }

        void DoTest() final {
            const TString volumeName = Params_.VolumeStorage + "/rootfs_test_box";

            API::TPodAgentStatus specTimestamp1Ret = UpdatePodAgentRequestAndWaitForReady(SpecHolder_.GetWorkloadSecretSpec(1), 240);

            {
                TString startOutput = TUnbufferedFileInput(volumeName + "/my_secret_file").ReadAll();
                UNIT_ASSERT_EQUAL_C(startOutput, "my_secret my_base64_secret\n", startOutput);
            }

            API::TPodAgentStatus specTimestamp2Ret = UpdatePodAgentRequestAndWaitForReady(SpecHolder_.GetWorkloadSecretSpec(2), 240);

            {
                TString startOutput = TUnbufferedFileInput(volumeName + "/my_secret_file_other").ReadAll();
                UNIT_ASSERT_EQUAL_C(startOutput, "my_secret_other my_base64_secret\n", startOutput);
            }

            // Must be ready in 1 iteration
            API::TPodAgentStatus specTimestamp3Ret = UpdatePodAgentRequestAndWaitForReady(SpecHolder_.GetWorkloadSecretSpec(3), 1);

            // fix flaky part
            specTimestamp1Ret.mutable_workloads(0)->clear_readiness_status(); // <Zero/Positive>ReturnCodeCounter
            specTimestamp2Ret.mutable_workloads(0)->clear_readiness_status(); // <Zero/Positive>ReturnCodeCounter
            specTimestamp3Ret.mutable_workloads(0)->clear_readiness_status(); // <Zero/Positive>ReturnCodeCounter

            Cout << specTimestamp1Ret.Utf8DebugString() << Endl;
            Cout << specTimestamp2Ret.Utf8DebugString() << Endl;
            Cout << specTimestamp3Ret.Utf8DebugString() << Endl;
        }
    };

    TTest test;
    test.Test();
}

Y_UNIT_TEST(TestSecretsWithBoxInit) {
    class TTest : public ITestCanon {
    public:
        TTest(): ITestCanon("TestSecretsWithBoxInit") {
        }

        void DoTest() final {
            const TString volumeName = Params_.VolumeStorage + "/rootfs_test_box";

            API::TPodAgentStatus specTimestamp1Ret = UpdatePodAgentRequestAndWaitForReady(SpecHolder_.GetBoxInitSecretSpec(1), 240);

            {
                TString startOutput = TUnbufferedFileInput(volumeName + "/my_secret_file").ReadAll();
                UNIT_ASSERT_EQUAL_C(startOutput, "my_secret\n", startOutput);
            }

            API::TPodAgentStatus specTimestamp2Ret = UpdatePodAgentRequestAndWaitForReady(SpecHolder_.GetBoxInitSecretSpec(2), 240);

            {
                TString startOutput = TUnbufferedFileInput(volumeName + "/my_secret_file_other").ReadAll();
                UNIT_ASSERT_EQUAL_C(startOutput, "my_secret_other\n", startOutput);
            }

            // Must be ready in 1 iteration
            API::TPodAgentStatus specTimestamp3Ret = UpdatePodAgentRequestAndWaitForReady(SpecHolder_.GetBoxInitSecretSpec(3), 1);

            Cout << specTimestamp1Ret.Utf8DebugString() << Endl;
            Cout << specTimestamp2Ret.Utf8DebugString() << Endl;
            Cout << specTimestamp3Ret.Utf8DebugString() << Endl;
        }
    };

    TTest test;
    test.Test();
}

Y_UNIT_TEST(TestManyUlimit) {
    class TTest : public ITestCanon {
    public:
        TTest(): ITestCanon("TestManyUlimit") {
        }

        void DoTest() final {
            const TString volumeName = Params_.VolumeStorage + "/rootfs_test_box";

            API::TPodAgentRequest ulimitSpec = SpecHolder_.GetManyUlimitSpec();
            API::TPodAgentStatus ret = UpdatePodAgentRequestAndWaitForReady(ulimitSpec, 240);

            TString startOutput = TUnbufferedFileInput(volumeName + "/memlock.txt").ReadAll();
            UNIT_ASSERT_EQUAL_C(startOutput, "Maxlockedmemory00bytes\n", startOutput);

            startOutput = TUnbufferedFileInput(volumeName + "/core.txt").ReadAll();
            UNIT_ASSERT_EQUAL_C(startOutput, "Maxcorefilesize11bytes\n", startOutput);

            // fix flaky part
            ret.mutable_workloads(0)->clear_readiness_status(); // <Zero/Positive>ReturnCodeCounter

            Cout << ret.Utf8DebugString() << Endl;
        }
    };

    TTest test;
    test.Test();
}

Y_UNIT_TEST(TestBoxPersistIpAllocation) {
    class TTest : public ITestCanon {
    public:
        TTest(): ITestCanon("TestBoxPersistIpAllocation") {
        }

        void DoTest() final {
            API::TPodAgentStatus specTimestamp1Ret = UpdatePodAgentRequestAndWaitForReady(SpecHolder_.GetBoxPersistIpAllocationStartSpec(1), 240);
            API::TPodAgentStatus specTimestamp2Ret = UpdatePodAgentRequestAndWaitForReady(SpecHolder_.GetBoxPersistIpAllocationStartSpec(2), 240);

            Cout << specTimestamp1Ret.Utf8DebugString() << Endl;
            Cout << specTimestamp2Ret.Utf8DebugString() << Endl;
        }
    };

    TTest test;
    test.Test();
}

Y_UNIT_TEST(TestLayerTreeMerge) {
    class TTest : public ITestCanon {
    public:
        TTest(): ITestCanon("TestLayerTreeMerge") {
        }

        void DoTest() final {
            API::TPodAgentStatus ret = UpdatePodAgentRequestAndWaitForReady(SpecHolder_.GetLayerTreeMergeSpec(), 240);

            NJson::TJsonValue json;
            ReadJsonTree(Handle_.Sensors(), &json, true);

            UNIT_ASSERT_C(json.IsArray(), json.GetStringRobust());
            bool foundTreeCnt = false;
            for (auto item : json.GetArray()) {
                UNIT_ASSERT_C(item.IsArray(), item.GetStringRobust());
                UNIT_ASSERT_C(item.GetArray()[0].IsString(), item.GetArray()[0].GetStringRobust());
                if (item.GetArray()[0].GetString() == "pod_agent_mtp_period_ticker_TreeCnt_aeev") {
                    UNIT_ASSERT_C(item.GetArray()[1].IsDouble(), item.GetArray()[1].GetStringRobust());
                    UNIT_ASSERT_DOUBLES_EQUAL_C(item.GetArray()[1].GetDouble(), 2, 1e-9, item.GetArray()[1].GetDouble());
                    foundTreeCnt = true;
                    break;
                }
            }
            UNIT_ASSERT_C(foundTreeCnt, "TreeCnt not found");

            Cout << ret.Utf8DebugString() << Endl;
        }
    };

    TTest test;
    test.Test();
}

Y_UNIT_TEST(TestCacheLayerMerge) {
    class TTest : public ITestCanon {
    public:
        TTest(): ITestCanon("TestCacheLayerMerge") {
        }

        void Init() final {
            Params_.GCPeriodMS = 5000;
            // for fast specTimestamp 5
            Params_.TreesUpdatePeriodMS = 1000;
            ITestCanon::Init();
        }

        void DoTest() final {
            API::TPodAgentStatus specTimestamp1Ret = UpdatePodAgentRequestAndWaitForReady(SpecHolder_.GetCacheLayerMergeSpec(1), 240);

            // Must be ready in 1 iteration
            API::TPodAgentStatus specTimestamp2Ret = UpdatePodAgentRequestAndWaitForReady(SpecHolder_.GetCacheLayerMergeSpec(2), 1);
            API::TPodAgentStatus specTimestamp3Ret = UpdatePodAgentRequestAndWaitForReady(SpecHolder_.GetCacheLayerMergeSpec(3), 1);
            API::TPodAgentStatus specTimestamp4Ret = UpdatePodAgentRequestAndWaitForReady(SpecHolder_.GetCacheLayerMergeSpec(4), 1);

            API::TPodAgentStatus specTimestamp5Ret = UpdatePodAgentRequestAndWaitForReady(SpecHolder_.GetCacheLayerMergeSpec(5), 240);

            Cout << specTimestamp1Ret.Utf8DebugString() << Endl;
            Cout << specTimestamp2Ret.Utf8DebugString() << Endl;
            Cout << specTimestamp3Ret.Utf8DebugString() << Endl;
            Cout << specTimestamp4Ret.Utf8DebugString() << Endl;
            Cout << specTimestamp5Ret.Utf8DebugString() << Endl;
        }
    };

    TTest test;
    test.Test();
}

Y_UNIT_TEST(TestStaticResourceTreeMerge) {
    class TTest : public ITestCanon {
    public:
        TTest(): ITestCanon("TestStaticResourceTreeMerge") {
        }

        void DoTest() final {
            API::TPodAgentStatus ret = UpdatePodAgentRequestAndWaitForReady(SpecHolder_.GetStaticResourceTreeMergeSpec(), 240);

            NJson::TJsonValue json;
            ReadJsonTree(Handle_.Sensors(), &json, true);

            UNIT_ASSERT_C(json.IsArray(), json.GetStringRobust());
            bool foundTreeCnt = false;
            for (auto item : json.GetArray()) {
                UNIT_ASSERT_C(item.IsArray(), item.GetStringRobust());
                UNIT_ASSERT_C(item.GetArray()[0].IsString(), item.GetArray()[0].GetStringRobust());
                if (item.GetArray()[0].GetString() == "pod_agent_mtp_period_ticker_TreeCnt_aeev") {
                    UNIT_ASSERT_C(item.GetArray()[1].IsDouble(), item.GetArray()[1].GetStringRobust());
                    UNIT_ASSERT_DOUBLES_EQUAL_C(item.GetArray()[1].GetDouble(), 2, 1e-9, item.GetArray()[1].GetDouble());
                    foundTreeCnt = true;
                    break;
                }
            }
            UNIT_ASSERT_C(foundTreeCnt, "TreeCnt not found");

            Cout << ret.Utf8DebugString() << Endl;
        }
    };

    TTest test;
    test.Test();
}

Y_UNIT_TEST(TestCacheStaticResourceMerge) {
    class TTest : public ITestCanon {
    public:
        TTest(): ITestCanon("TestCacheStaticResourceMerge") {
        }

        void Init() final {
            Params_.GCPeriodMS = 5000;
            // for fast specTimestamp 5
            Params_.TreesUpdatePeriodMS = 1000;
            ITestCanon::Init();
        }

        void DoTest() final {
            API::TPodAgentStatus specTimestamp1Ret = UpdatePodAgentRequestAndWaitForReady(SpecHolder_.GetCacheStaticResourceMergeSpec(1), 240);

            // Must be ready in 1 iteration
            API::TPodAgentStatus specTimestamp2Ret = UpdatePodAgentRequestAndWaitForReady(SpecHolder_.GetCacheStaticResourceMergeSpec(2), 1);
            API::TPodAgentStatus specTimestamp3Ret = UpdatePodAgentRequestAndWaitForReady(SpecHolder_.GetCacheStaticResourceMergeSpec(3), 1);
            API::TPodAgentStatus specTimestamp4Ret = UpdatePodAgentRequestAndWaitForReady(SpecHolder_.GetCacheStaticResourceMergeSpec(4), 1);

            API::TPodAgentStatus specTimestamp5Ret = UpdatePodAgentRequestAndWaitForReady(SpecHolder_.GetCacheStaticResourceMergeSpec(5), 240);

            Cout << specTimestamp1Ret.Utf8DebugString() << Endl;
            Cout << specTimestamp2Ret.Utf8DebugString() << Endl;
            Cout << specTimestamp3Ret.Utf8DebugString() << Endl;
            Cout << specTimestamp4Ret.Utf8DebugString() << Endl;
            Cout << specTimestamp5Ret.Utf8DebugString() << Endl;
        }
    };

    TTest test;
    test.Test();
}

Y_UNIT_TEST(TestBoxWithSkynet) {
    class TTest : public ITestCanon {
    public:
        TTest(): ITestCanon("TestBoxWithSkynet") {
        }

        void Init() final {
            if (NPortoTestLib::IsInsideSandboxPortoIsolation()) {
                //TODO: Remove after fix binding of skynet in container. https://st.yandex-team.ru/DEVTOOLSSUPPORT-7789
                UNIT_ASSERT_C(NFs::Remove("/skynet"), "Error while removing /skynet symlink: " << LastSystemErrorText());
                UNIT_ASSERT_C(NFs::SymLink("/Berkanavt/supervisor/base/active", "/skynet"), "Error while creating /skynet symlink: " << LastSystemErrorText());
            }
            ITestCanon::Init();
        }

        void DoTest() final {
            auto skynetSpec = SpecHolder_.GetSkynetInBoxsSpec();
            API::TPodAgentStatus specTimestamp1Ret = UpdatePodAgentRequestAndWaitForReady(skynetSpec, 240);
            Cout << specTimestamp1Ret.Utf8DebugString() << Endl;
        }
    };

    TTest test;
    test.Test();
}

Y_UNIT_TEST(TestVirtualDisks) {
    class TTest : public ITestCanon {
    public:
        TTest(): ITestCanon("TestVirtualDisks") {
        }

        void Init() final {
            Params_.VirtualDisksEnable = true;
            PrepareVirtualDisks(
                {
                    "virtual_disk1"
                    , "virtual_disk2"
                }
            );
            ITestCanon::Init();
        }

        void DoTest() final {
            auto spec = SpecHolder_.GetVirtualDisksSpec();
            auto readyHook = [](const API::TPodAgentStatus& status) {
                return CheckAllObjectsReady(status)
                    && !status.workloads(0).start().current().stdout_tail().empty()
                    && !status.workloads(1).start().current().stdout_tail().empty();
            };
            API::TPodAgentStatus ret = UpdatePodAgentRequestAndWaitForReady(spec, 240, readyHook);

            // readiness can run few times while stdout_tail of start container is empty
            for (auto& obj: *ret.mutable_workloads()) {
                obj.mutable_readiness_status()->mutable_container_status()->set_zero_return_code_counter(1);
                obj.mutable_readiness_status()->mutable_container_status()->mutable_time_limit()->set_consecutive_successes_counter(1);
            }

            Cout << ret.Utf8DebugString() << Endl;
        }
    };

    TTest test;
    test.Test();
}

Y_UNIT_TEST(TestBoxWithYt) {
    class TTest : public ITestCanon {
    public:
        TTest(): ITestCanon("TestBoxWithYt") {
        }

        void Init() final {
            const TString ytPath = NFs::CurrentWorkingDirectory() + "/yt";
            NFs::MakeDirectoryRecursive(ytPath);

            TUnbufferedFileOutput fileOutput(ytPath + "/data");
            fileOutput.Write("yt_data");

            Params_.YtPath = ytPath;
            ITestCanon::Init();
        }

        void DoTest() final {
            auto ytSpec = SpecHolder_.GetYtInBoxsSpec();
            API::TPodAgentStatus specTimestamp1Ret = UpdatePodAgentRequestAndWaitForReady(ytSpec, 240);

            UNIT_ASSERT_EQUAL_C(specTimestamp1Ret.boxes(0).inits_size(), 1, specTimestamp1Ret.boxes(0).inits_size());
            UNIT_ASSERT_EQUAL_C(specTimestamp1Ret.boxes(0).inits(0).last().stdout_tail(), "yt_data", specTimestamp1Ret.boxes(0).inits(0).last().stdout_tail());

            Cout << specTimestamp1Ret.Utf8DebugString() << Endl;
        }
    };

    TTest test;
    test.Test();
}

Y_UNIT_TEST(TestBoxWithBaseSearch) {
    class TTest : public ITestCanon {
    public:
        TTest(): ITestCanon("TestBoxWithBaseSearch") {
        }

        void Init() final {
            const TString basesearchPath = NFs::CurrentWorkingDirectory() + "/basesearch";
            NFs::MakeDirectoryRecursive(basesearchPath);

            TUnbufferedFileOutput fileOutput(basesearchPath + "/data");
            fileOutput.Write("basesearch_data");

            Params_.BaseSearchPath = basesearchPath;
            ITestCanon::Init();
        }

        void DoTest() final {
            auto basesearchSpec = SpecHolder_.GetBaseSearchInBoxsSpec();
            API::TPodAgentStatus specTimestamp1Ret = UpdatePodAgentRequestAndWaitForReady(basesearchSpec, 240);

            UNIT_ASSERT_EQUAL_C(specTimestamp1Ret.boxes(0).inits_size(), 1, specTimestamp1Ret.boxes(0).inits_size());
            UNIT_ASSERT_EQUAL_C(specTimestamp1Ret.boxes(0).inits(0).last().stdout_tail(), "basesearch_data", specTimestamp1Ret.boxes(0).inits(0).last().stdout_tail());

            Cout << specTimestamp1Ret.Utf8DebugString() << Endl;
        }
    };

    TTest test;
    test.Test();
}

Y_UNIT_TEST(TestBoxWithRbindVolume) {
    class TTest : public ITestCanon {
    public:
        TTest(): ITestCanon("TestBoxWithRbindVolume")
        {
        }

        void Init() final {
            RbindVolumeStorage_ = Params_.RbindVolumeStorageDir + "/rbind_volume_storage";
            NFs::MakeDirectoryRecursive(RbindVolumeStorage_);

            TUnbufferedFileOutput fileOutput(RbindVolumeStorage_ + "/data");
            fileOutput.Write("rbind_volume_data");

            ITestCanon::Init();
        }

        void DoTest() final {
            auto rbindVolumeSpec = SpecHolder_.GetRbindVolumeSpec();
            API::TPodAgentStatus specTimestamp1Ret = UpdatePodAgentRequestAndWaitForReady(rbindVolumeSpec, 240);

            {
                // Check read
                UNIT_ASSERT_EQUAL_C(specTimestamp1Ret.boxes(0).inits_size(), 2, specTimestamp1Ret.boxes(0).inits_size());
                UNIT_ASSERT_EQUAL_C(specTimestamp1Ret.boxes(0).inits(0).last().stdout_tail(), "rbind_volume_data", specTimestamp1Ret.boxes(0).inits(0).last().stdout_tail());
            }

            {
                // Check write
                TUnbufferedFileInput fileInput(RbindVolumeStorage_ + "/init_data");
                const TString initData = fileInput.ReadAll();
                UNIT_ASSERT_EQUAL_C(initData, "init_data", initData);
            }

            Cout << specTimestamp1Ret.Utf8DebugString() << Endl;
        }

    private:
        TString RbindVolumeStorage_;
    };

    TTest test;
    test.Test();
}

Y_UNIT_TEST(TestWorkloadNoReadiness) {
    class TTest : public ITestCanon {
    public:
        TTest(): ITestCanon("TestWorkloadNoReadiness") {
        }

        void DoTest() final {
            auto spec = SpecHolder_.GetWorkloadNoReadinessSpec();

            auto readyHook = [](const API::TPodAgentStatus& status) {
                return CheckAllObjectsReady(status) && status.workloads(0).state() == API::EWorkloadState_INVALID && !status.workloads(0).start().last().fail_reason().empty();
            };

            API::TPodAgentStatus specTimestamp1Ret = UpdatePodAgentRequestAndWaitForReady(spec, 240, readyHook);

            // Remove flaky part
            specTimestamp1Ret.mutable_workloads(0)->mutable_start()->mutable_last()->set_fail_reason("");
            specTimestamp1Ret.mutable_workloads(0)->mutable_start()->mutable_last_failed()->set_fail_reason("");

            Cout << specTimestamp1Ret.Utf8DebugString() << Endl;

            spec.mutable_spec()->mutable_workloads(0)->mutable_start()->set_command_line("bash -c 'echo hello'");
            spec.set_spec_timestamp(2);

            UpdatePodAgentRequestAndWaitForReady(spec, 240);

            spec.mutable_spec()->mutable_workloads(0)->mutable_readiness_check()->mutable_start_container_alive_check();
            spec.mutable_spec()->mutable_workloads(0)->mutable_start()->set_command_line("wrong_command");
            spec.set_spec_timestamp(3);

            auto readyHookWithFailedPodStatus = [](const API::TPodAgentStatus& status) {
                return status.ready().status() == API::EConditionStatus_FALSE
                    && status.workloads(0).state() == API::EWorkloadState_INVALID
                    && status.workloads(0).ready().status() == API::EConditionStatus_FALSE
                    && !status.workloads(0).start().last().fail_reason().empty();
            };

            API::TPodAgentStatus specTimestamp3Ret = UpdatePodAgentRequestAndWaitForReady(spec, 240, readyHookWithFailedPodStatus);

            // Remove flaky part
            specTimestamp3Ret.mutable_workloads(0)->mutable_start()->mutable_last()->set_fail_reason("");
            specTimestamp3Ret.mutable_workloads(0)->mutable_start()->mutable_last_failed()->set_fail_reason("");

            Cout << specTimestamp3Ret.Utf8DebugString() << Endl;
        }
    };

    TTest test;
    test.Test();
}

Y_UNIT_TEST(TestPublicVolume) {
    class TTest : public ITestCanon {
    public:
        TTest(): ITestCanon("TestPublicVolume") {
        }

        void DoTest() final {
            auto spec = SpecHolder_.GetPublicVolumeSpec();

            API::TPodAgentStatus ret1 = UpdatePodAgentRequestAndWaitForReady(spec, 120);

            const TString humanReadableSpecFromWorkloadInit = ret1.workloads(0).init(0).last().stdout_tail();
            const TString humanReadableSpec = TUnbufferedFileInput(TStringBuilder() << Params_.PublicVolumePath << "/" << Params_.PublicHumanReadableSaveSpecFileName).ReadAll();
            UNIT_ASSERT_EQUAL_C(humanReadableSpecFromWorkloadInit, humanReadableSpec, humanReadableSpecFromWorkloadInit);

            {
                UNIT_ASSERT_C(!NFs::Exists(Params_.PublicVolumePath + "/read_only_test"), "Public volume mounted in rw mode, but must be in ro");
                UNIT_ASSERT_EQUAL_C(ret1.workloads(0).init(1).last().stdout_tail(), "read_only_test_ok\n", ret1.workloads(0).init(1).last().stdout_tail());
            }

            {
                const TString publicVolumeBinaryMD5 = MD5().File(Params_.PublicVolumePath + "/pod_agent");
                const TString realPodAgentBinaryMD5 = MD5().File(BinaryPath("infra/pod_agent/daemons/pod_agent/pod_agent"));
                UNIT_ASSERT_EQUAL_C(
                    publicVolumeBinaryMD5
                    , realPodAgentBinaryMD5
                    , "pod_agent binary has incorrect md5 hash"
                );
            }

            Cout << ret1.Utf8DebugString() << Endl;
            Cout << humanReadableSpec << Endl;
        }
    };

    TTest test;
    test.Test();
}

Y_UNIT_TEST(TestExecWrapper) {
    class TTest : public ITestCanon {
    public:
        TTest(): ITestCanon("TestExecWrapper") {
        }

        void DoTest() final {
            auto spec = SpecHolder_.GetExecWrapperSpec();

            API::TPodAgentStatus ret = UpdatePodAgentRequestAndWaitForReady(spec, 120);

            const TVector<TString> expectedWorkloadInitOutput = {
                "0\n0\n"
                , "root\nroot\n"

                , "31234\n41234\n"
                , "test_unique_user\ntest_unique_group\n"

                , "0\n41234\n"
                , "root\ntest_unique_group\n"

                , "31234\n0\n"
                , "test_unique_user\nroot\n"
            };

            UNIT_ASSERT_EQUAL_C(
                ret.workloads(0).init_size()
                , (i32)expectedWorkloadInitOutput.size()
                , ret.workloads(0).init_size()
            );

            for (size_t i = 0; i < expectedWorkloadInitOutput.size(); ++i) {
                UNIT_ASSERT_EQUAL_C(
                    ret.workloads(0).init(i).last().stdout_tail()
                    , expectedWorkloadInitOutput[i]
                    , ret.workloads(0).init(i).last().stdout_tail()
                );
            }

            Cout << ret.Utf8DebugString() << Endl;
        }
    };

    TTest test;
    test.Test();
}

Y_UNIT_TEST(TestUnixSignalStop) {
    class TTest : public ITestCanon {
    public:
        TTest(): ITestCanon("TestUnixSignalStop") {
        }

        void DoTest() final {
            API::TPodAgentStatus specTimestamp1Ret = UpdatePodAgentRequestAndWaitForReady(SpecHolder_.GetUnixSignalStopSpec(1), 120);
            API::TPodAgentStatus specTimestamp2Ret = UpdatePodAgentRequestAndWaitForReady(SpecHolder_.GetUnixSignalStopSpec(2), 120);

            UNIT_ASSERT_EQUAL_C(specTimestamp2Ret.workloads_size(), 2, specTimestamp2Ret.workloads_size());
            for (const auto& workload : specTimestamp2Ret.workloads()) {
                if (workload.id() == "MyWorkloadWithUnixSignalStop") {
                    UNIT_ASSERT_EQUAL_C(
                        -SIGINT
                        , workload.start().last().return_code()
                        , workload.start().last().return_code()
                    );
                } else if (workload.id() == "MyWorkloadWithDefaultStop") {
                    UNIT_ASSERT_EQUAL_C(
                        -SIGTERM
                        , workload.start().last().return_code()
                        , workload.start().last().return_code()
                    );
                } else {
                    UNIT_ASSERT_C(false, "Unxepected workload '" << workload.id() << "'");
                }
            }

            Cout << specTimestamp1Ret.Utf8DebugString() << Endl;
            Cout << specTimestamp2Ret.Utf8DebugString() << Endl;
        }
    };

    TTest test;
    test.Test();
}

Y_UNIT_TEST(TestPodAgentTargetState) {
    class TTest : public ITestCanon {
    public:
        TTest(): ITestCanon("TestPodAgentTargetState") {
        }

        void DoTest() final {
            API::TPodAgentStatus specTimestamp1Ret = UpdatePodAgentRequestAndWaitForReady(SpecHolder_.GetPodAgentTargetStateSpec(1), 120);
            ValidateActiveStatus(specTimestamp1Ret);

            API::TPodAgentStatus specTimestamp2Ret = UpdatePodAgentRequestAndWaitForReady(SpecHolder_.GetPodAgentTargetStateSpec(2), 120);
            ValidateSuspendedStatus(specTimestamp2Ret);

            API::TPodAgentStatus specTimestamp3Ret = UpdatePodAgentRequestAndWaitForReady(SpecHolder_.GetPodAgentTargetStateSpec(3), 120);
            ValidateActiveStatus(specTimestamp3Ret);

            API::TPodAgentStatus specTimestamp4Ret = UpdatePodAgentRequestAndWaitForReady(SpecHolder_.GetPodAgentTargetStateSpec(4), 120);
            ValidateRemovedStatus(specTimestamp4Ret);

            API::TPodAgentStatus specTimestamp5Ret = UpdatePodAgentRequestAndWaitForReady(SpecHolder_.GetPodAgentTargetStateSpec(5), 120);
            ValidateActiveStatus(specTimestamp5Ret);

            Cout << specTimestamp1Ret.Utf8DebugString() << Endl;
            Cout << specTimestamp2Ret.Utf8DebugString() << Endl;
            Cout << specTimestamp3Ret.Utf8DebugString() << Endl;
            Cout << specTimestamp4Ret.Utf8DebugString() << Endl;
            Cout << specTimestamp5Ret.Utf8DebugString() << Endl;
        }

    private:
        void ValidateActiveStatus(API::TPodAgentStatus& status) {
            UNIT_ASSERT_EQUAL_C(status.workloads_size(), 1, status.workloads_size());
            UNIT_ASSERT_EQUAL_C(
                status.workloads(0).state()
                , API::EWorkloadState_ACTIVE
                , API::EWorkloadState_Name(status.workloads(0).state())
            );
        }

        void ValidateSuspendedStatus(API::TPodAgentStatus& status) {
            UNIT_ASSERT_EQUAL_C(status.workloads_size(), 1, status.workloads_size());
            UNIT_ASSERT_EQUAL_C(
                status.workloads(0).state()
                , API::EWorkloadState_REMOVED
                , API::EWorkloadState_Name(status.workloads(0).state())
            );
        }

        void ValidateRemovedStatus(API::TPodAgentStatus& status) {
            UNIT_ASSERT_EQUAL_C(status.resource_gang().layers_size(), 0, status.resource_gang().layers_size());
            UNIT_ASSERT_EQUAL_C(status.resource_gang().static_resources_size(), 0, status.resource_gang().static_resources_size());
            UNIT_ASSERT_EQUAL_C(status.resource_cache().layers_size(), 0, status.resource_cache().layers_size());
            UNIT_ASSERT_EQUAL_C(status.resource_cache().static_resources_size(), 0, status.resource_cache().static_resources_size());
            UNIT_ASSERT_EQUAL_C(status.volumes_size(), 0, status.volumes_size());
            UNIT_ASSERT_EQUAL_C(status.boxes_size(), 0, status.boxes_size());
            UNIT_ASSERT_EQUAL_C(status.workloads_size(), 0, status.workloads_size());
        }
    };

    TTest test;
    test.Test();
}

Y_UNIT_TEST(TestHiddenSecretsAndDynamicAttributesInStoredSpec) {
    class TTest : public ITestCanon {
    public:
        TTest(): ITestCanon("TestHiddenSecretsAndDynamicAttributesInStoredSpec") {
        }

        void DoTest() final {
            auto spec = SpecHolder_.GetHiddenSecretsAndDynamicAttributesSpec();
            
            auto* attribute = spec.mutable_pod_dynamic_attributes()->mutable_labels()->add_attributes();
            attribute->set_key("long_label");
            TString longLabel = ("BytesBytesBytesBytesBytesBytesBytesBytesBytesBytesBytesBytesBytes");
            TString ysonLongLabel = NYT::NYson::ConvertToYsonString(longLabel).ToString();
            attribute->set_value(ysonLongLabel);

            API::TPodAgentStatus ret1 = UpdatePodAgentRequestAndWaitForReady(spec, 120);

            const TString humanReadableSpec = TUnbufferedFileInput(TStringBuilder() << Params_.PublicVolumePath << "/" << Params_.PublicHumanReadableSaveSpecFileName).ReadAll();
            API::TPodAgentRequest humanReadableSpecProto = NProtobufJson::Json2Proto<API::TPodAgentRequest>(humanReadableSpec, GetSpecHolderJson2ProtoConfig());

            UNIT_ASSERT_EQUAL_C(humanReadableSpecProto.secrets()[0].id(), HIDDEN_SECRET_ATTRIBUTE_ID_PREFIX + spec.secrets()[0].id(), humanReadableSpecProto.secrets()[0].id());
            UNIT_ASSERT_EQUAL_C(humanReadableSpecProto.secrets()[1].id(), HIDDEN_SECRET_ATTRIBUTE_ID_PREFIX + spec.secrets()[1].id(), humanReadableSpecProto.secrets()[1].id());

            for (const auto& [key, value] : humanReadableSpecProto.secrets()[0].payload()) {
                UNIT_ASSERT_EQUAL_C(value, HIDDEN_SECRET_ATTRIBUTE_VALUE, value);
            }

            UNIT_ASSERT_EQUAL_C(humanReadableSpecProto.secrets()[0].values()[0].value(), HIDDEN_SECRET_ATTRIBUTE_VALUE, humanReadableSpecProto.secrets()[0].values()[0].value());
            UNIT_ASSERT_EQUAL_C(humanReadableSpecProto.secrets()[0].values()[0].encoding(), HIDDEN_SECRET_ATTRIBUTE_VALUE, humanReadableSpecProto.secrets()[0].values()[0].encoding());

            UNIT_ASSERT_EQUAL_C(humanReadableSpecProto.secrets()[0].values()[1].value(), HIDDEN_SECRET_ATTRIBUTE_VALUE, humanReadableSpecProto.secrets()[0].values()[1].value());
            UNIT_ASSERT_EQUAL_C(humanReadableSpecProto.secrets()[0].values()[1].encoding(), HIDDEN_SECRET_ATTRIBUTE_VALUE, humanReadableSpecProto.secrets()[0].values()[1].encoding());

            UNIT_ASSERT_EQUAL_C(humanReadableSpecProto.secrets()[1].values()[0].value(), HIDDEN_SECRET_ATTRIBUTE_VALUE, humanReadableSpecProto.secrets()[1].values()[0].value());
            UNIT_ASSERT_EQUAL_C(humanReadableSpecProto.secrets()[1].values()[0].encoding(), HIDDEN_SECRET_ATTRIBUTE_VALUE, humanReadableSpecProto.secrets()[1].values()[0].encoding());

            UNIT_ASSERT_EQUAL_C(humanReadableSpecProto.pod_dynamic_attributes().labels().attributes()[1].value(), longLabel, humanReadableSpecProto.pod_dynamic_attributes().labels().attributes()[1].value());
            Cout << ret1.Utf8DebugString() << Endl;

            // Order in map payload is not guaranteed
            (*humanReadableSpecProto.mutable_secrets())[0].clear_payload();
            Cout << humanReadableSpecProto.Utf8DebugString() << Endl;

            API::TPodAgentRequest cachedBinarySpec = NCacheFile::LoadFromFileBin<API::TPodAgentRequest>(TStringBuilder() << Params_.CacheVolumePath << "/" << Params_.SaveSpecFileName);
            const TString cachedJsonSpecStr = TUnbufferedFileInput(TStringBuilder() << Params_.CacheVolumePath << "/" << Params_.CachedHumanReadableSaveSpecFileName).ReadAll();
            API::TPodAgentRequest cachedJsonSpec = NProtobufJson::Json2Proto<API::TPodAgentRequest>(cachedJsonSpecStr, GetSpecHolderJson2ProtoConfig());

            UNIT_ASSERT(google::protobuf::util::MessageDifferencer::Equals(spec, cachedJsonSpec));
            UNIT_ASSERT(google::protobuf::util::MessageDifferencer::Equals(spec, cachedBinarySpec));

            // Order in map payload is not guaranteed
            (*cachedJsonSpec.mutable_secrets())[0].clear_payload();
            Cout << cachedJsonSpec.Utf8DebugString() << Endl;
        }
    };

    TTest test;
    test.Test();
}

Y_UNIT_TEST(TestBoxWithRoRootfs) {
    class TTest : public ITestCanon {
    public:
        TTest(): ITestCanon("TestBoxWithRoRootfs") {
        }

        void Init() final {
            if (NPortoTestLib::IsInsideSandboxPortoIsolation()) {
                //TODO: Remove after fix binding of skynet in container. https://st.yandex-team.ru/DEVTOOLSSUPPORT-7789
                UNIT_ASSERT_C(NFs::Remove("/skynet"), "Error while removing /skynet symlink: " << LastSystemErrorText());
                UNIT_ASSERT_C(NFs::SymLink("/Berkanavt/supervisor/base/active", "/skynet"), "Error while creating /skynet symlink: " << LastSystemErrorText());
            }

            //yt volume environment
            const TString ytPath = NFs::CurrentWorkingDirectory() + "/yt";
            NFs::MakeDirectoryRecursive(ytPath);
            TUnbufferedFileOutput ytDataOutput(ytPath + "/data");
            ytDataOutput.Write("yt_data");
            Params_.YtPath = ytPath;

            //basesearch volume environment
            const TString basesearchPath = NFs::CurrentWorkingDirectory() + "/basesearch";
            NFs::MakeDirectoryRecursive(basesearchPath);
            TUnbufferedFileOutput basesearchDataOutput(basesearchPath + "/data");
            basesearchDataOutput.Write("basesearch_data");
            Params_.BaseSearchPath = basesearchPath;

            //rbind volume storage environment
            const TString rbindVolumeStorage = Params_.RbindVolumeStorageDir + "/writable_rbind_volume_storage";
            NFs::MakeDirectoryRecursive(rbindVolumeStorage);
            TUnbufferedFileOutput rbindDataOutput(rbindVolumeStorage + "/data");
            rbindDataOutput.Write("rbind_volume_data");

            ITestCanon::Init();
        }

        void DoTest() final {
            auto spec = SpecHolder_.GetBoxWithRoRootfsSpec();
            int runAllAssertionsCalls = 0;
            auto runAllAssertions = [&runAllAssertionsCalls](const API::TPodAgentStatus& ret) {
                //check rootfs readonly
                UNIT_ASSERT_EQUAL_C(ret.workloads(0).init(0).last().stdout_tail(), "rootfs_read_only_test_ok\n", ret.workloads(0).init(0).last().stdout_tail());

                //check yt volume read
                UNIT_ASSERT_EQUAL_C(ret.boxes(0).inits(0).last().stdout_tail(), "yt_data", ret.boxes(0).inits(0).last().stdout_tail());

                //check basesearch volume read
                UNIT_ASSERT_EQUAL_C(ret.boxes(0).inits(1).last().stdout_tail(), "basesearch_data", ret.boxes(0).inits(1).last().stdout_tail());

                //check rbind volume read
                UNIT_ASSERT_EQUAL_C(ret.boxes(0).inits(2).last().stdout_tail(), "rbind_volume_data", ret.boxes(0).inits(2).last().stdout_tail());

                //check yt volume writable
                UNIT_ASSERT_EQUAL_C(ret.boxes(0).inits(3).last().stdout_tail(), "yt_volume_writable_test_ok\n", ret.boxes(0).inits(3).last().stdout_tail());

                //check basesearch volume writable
                UNIT_ASSERT_EQUAL_C(ret.boxes(0).inits(4).last().stdout_tail(), "basesearch_volume_writable_test_ok\n", ret.boxes(0).inits(4).last().stdout_tail());

                //check rbind volume writable
                UNIT_ASSERT_EQUAL_C(ret.boxes(0).inits(5).last().stdout_tail(), "rbind_volume_writable_test_ok\n", ret.boxes(0).inits(5).last().stdout_tail());

                //check non persistent volume is empty
                UNIT_ASSERT_EQUAL_C(ret.boxes(0).inits(6).last().stdout_tail(), "non_persistent_volume_is_empty_test_ok\n", ret.boxes(0).inits(6).last().stdout_tail());

                //check non persistent volume writable
                UNIT_ASSERT_EQUAL_C(ret.boxes(0).inits(7).last().stdout_tail(), "non_persistent_volume_writable_test_ok\n", ret.boxes(0).inits(7).last().stdout_tail());

                //check static resource read
                UNIT_ASSERT_EQUAL_C(ret.boxes(0).inits(8).last().stdout_tail(), "some_data", ret.boxes(0).inits(8).last().stdout_tail());

                //check static resource read_only
                UNIT_ASSERT_EQUAL_C(ret.boxes(0).inits(9).last().stdout_tail(), "static_resource_readonly_test_ok\n", ret.boxes(0).inits(9).last().stdout_tail());

                //check non persistent volume is not empty after writing
                UNIT_ASSERT_EQUAL_C(ret.boxes(0).inits(11).last().stdout_tail(), "non_persistent_volume_is_not_empty_after_writing_test_ok\n", ret.boxes(0).inits(11).last().stdout_tail());

                ++runAllAssertionsCalls;
            };

            API::TPodAgentStatus ret1 = UpdatePodAgentRequestAndWaitForReady(spec, 120);

            runAllAssertions(ret1);

            // add simple init for recreating container of box
            auto* newInit = spec.mutable_spec()->mutable_boxes(0)->add_init();
            newInit->set_command_line("bash -c 'echo hello'");
            spec.mutable_spec()->set_revision(2);
            spec.set_spec_timestamp(2);

            API::TPodAgentStatus ret2 = UpdatePodAgentRequestAndWaitForReady(spec, 240);

            UNIT_ASSERT_EQUAL(ret2.revision(), 2);

            // non persistent volume must be empty again after recreating
            runAllAssertions(ret2);

            UNIT_ASSERT_EQUAL_C(runAllAssertionsCalls, 2, "Lambda with all assertions was called less times then expected!");
        }
    };

    TTest test;
    test.Test();
}

Y_UNIT_TEST(TestBoxWithChildOnlyIsolation) {
    class TTest : public ITestCanon {
    public:
        TTest(): ITestCanon("TestBoxWithChildOnlyIsolation") {
        }

        void DoTest() final {
            auto spec = SpecHolder_.GetBoxWithChildOnlyIsolationSpec();
            API::TPodAgentStatus ret = UpdatePodAgentRequestAndWaitForReady(spec, 240);

            const TString destroyError = "Can\'t destroy container: Permission:(Write access denied";
            UNIT_ASSERT_STRING_CONTAINS_C(ret.boxes(0).inits(1).last().stdout_tail(), destroyError, ret.boxes(0).inits(1).last().stdout_tail());
        }
    };

    TTest test;
    test.Test();
}

Y_UNIT_TEST(TestBoxWithCgroupFsMount) {
    class TTest : public ITestCanon {
    public:
        TTest(): ITestCanon("TestBoxWithCgroupFsMount") {
        }

        void DoTest() final {
            auto spec = SpecHolder_.GetBoxWithCgroupFsMountSpec();
            API::TPodAgentStatus ret = UpdatePodAgentRequestAndWaitForReady(spec, 240);

            UNIT_ASSERT_STRING_CONTAINS_C(ret.boxes(0).inits(0).last().stdout_tail(), "cgroup fs is here!", ret.boxes(0).inits(0).last().stdout_tail());

            (*spec.mutable_spec()->mutable_boxes())[0].set_cgroup_fs_mount_mode(API::ECgroupFsMountMode_NONE);
            spec.set_spec_timestamp(2);

            ret = UpdatePodAgentRequestAndWaitForReady(spec, 240);

            UNIT_ASSERT_STRING_CONTAINS_C(ret.boxes(0).inits(0).last().stdout_tail(), "cgroup fs not found" , ret.boxes(0).inits(0).last().stdout_tail());
        }
    };

    TTest test;
    test.Test();
}

Y_UNIT_TEST(TestVolumeChangePersistentType) {
    class TTest : public ITestCanon {
    public:
        TTest(): ITestCanon("TestVolumeChangePersistentType") {
        }

        void DoTest() final {
            auto spec = SpecHolder_.GetVolumeChangePersistentTypeSpec();

            API::TPodAgentStatus ret1 = UpdatePodAgentRequestAndWaitForReady(spec, 120);
            UNIT_ASSERT_EQUAL_C(ret1.boxes(0).inits(0).last().stdout_tail(), "volume_is_empty\n", ret1.boxes(0).inits(0).last().stdout_tail());

            // add simple init for recreating container of box
            auto* newInit = spec.mutable_spec()->mutable_boxes(0)->add_init();
            newInit->set_command_line("bash -c 'echo hello'");
            spec.mutable_spec()->set_revision(2);
            spec.set_spec_timestamp(2);

            API::TPodAgentStatus ret2 = UpdatePodAgentRequestAndWaitForReady(spec, 120);
            UNIT_ASSERT_EQUAL_C(ret2.boxes(0).inits(0).last().stdout_tail(), "volume_is_not_empty\n", ret2.boxes(0).inits(0).last().stdout_tail());

            (*spec.mutable_spec()->mutable_volumes())[0].set_persistence_type(API::EVolumePersistenceType_NON_PERSISTENT);
            spec.mutable_spec()->set_revision(3);
            spec.set_spec_timestamp(3);

            API::TPodAgentStatus ret3 = UpdatePodAgentRequestAndWaitForReady(spec, 120);
            UNIT_ASSERT_EQUAL_C(ret3.boxes(0).inits(0).last().stdout_tail(), "volume_is_empty\n", ret3.boxes(0).inits(0).last().stdout_tail());

            spec.mutable_spec()->mutable_boxes(0)->mutable_init(2)->set_command_line("bash -c 'echo hello new'");
            spec.mutable_spec()->set_revision(4);
            spec.set_spec_timestamp(4);

            API::TPodAgentStatus ret4 = UpdatePodAgentRequestAndWaitForReady(spec, 120);
            UNIT_ASSERT_EQUAL_C(ret4.boxes(0).inits(0).last().stdout_tail(), "volume_is_empty\n", ret4.boxes(0).inits(0).last().stdout_tail());
        }
    };

    TTest test;
    test.Test();
}

Y_UNIT_TEST(TestBoxWithRoRootfsAndNotDirectoryMountPoints) {
    class TTest : public ITestCanon {
    public:
        TTest(): ITestCanon("TestBoxWithRoRootfsAndNotDirectoryMountPoints") {
        }

        void Init() final {
            //rbind volume storage environment
            const TString rbindVolumeStorage = Params_.RbindVolumeStorageDir + "/my_rbind_volume";
            NFs::MakeDirectoryRecursive(rbindVolumeStorage);
            TUnbufferedFileOutput rbindDataOutput(rbindVolumeStorage + "/data");
            rbindDataOutput.Write("rbind_volume_data");

            ITestCanon::Init();
        }

        void DoTest() final {
            auto spec = SpecHolder_.GetBoxWithRoRootfsAndInvalidMountPointsSpec();

            TString rootFsPath = MakeFullPathPrefix() + "_volumes/rootfs_Test_box";
            TString unknownPath = "/tmp/unknown";
            auto breakHook = [unknownPath](const API::TPodAgentStatus& status) {
                return status.boxes(0).state() == API::EBoxState_INVALID;
            };

            // Case 1: Volume link path does not exist
            spec.mutable_spec()->mutable_boxes(0)->mutable_volumes(0)->set_mount_point(unknownPath);
            API::TPodAgentStatus ret1 = UpdatePodAgentRequestAndWaitForReady(spec, 120, breakHook);

            TString unknownFullPath = rootFsPath + unknownPath;
            UNIT_ASSERT_EQUAL_C(ret1.boxes(0).state(), API::EBoxState_INVALID, EBoxState_Name(ret1.boxes(0).state()));
            UNIT_ASSERT_EQUAL_C(
                ret1.boxes(0).failed().message()
                , TStringBuilder() << "volume link path '" << unknownFullPath << "' does not exist on RO rootfs"
                , ret1.boxes(0).failed().message()
            );

            spec.mutable_spec()->mutable_boxes(0)->clear_volumes();
            spec.set_spec_timestamp(2);
            API::TPodAgentStatus ret2 = UpdatePodAgentRequestAndWaitForReady(spec, 120);
            UNIT_ASSERT_EQUAL_C(ret2.boxes(0).state(), API::EBoxState_READY, EBoxState_Name(ret2.boxes(0).state()));

            // Case 2: Rbind volume link path does not exist
            auto* volume = spec.mutable_spec()->mutable_boxes(0)->add_volumes();
            volume->set_rbind_volume_ref("my_rbind_volume");
            volume->set_mount_point(unknownPath);
            spec.set_spec_timestamp(3);

            API::TPodAgentStatus ret3 = UpdatePodAgentRequestAndWaitForReady(spec, 120, breakHook);
            UNIT_ASSERT_EQUAL_C(ret3.boxes(0).state(), API::EBoxState_INVALID, EBoxState_Name(ret3.boxes(0).state()));
            UNIT_ASSERT_EQUAL_C(
                ret3.boxes(0).failed().message()
                , TStringBuilder() << "rbind volume mount path '" << unknownFullPath << "' does not exist on RO rootfs"
                , ret3.boxes(0).failed().message()
            );

            spec.mutable_spec()->mutable_boxes(0)->clear_volumes();
            spec.set_spec_timestamp(4);
            API::TPodAgentStatus ret4 = UpdatePodAgentRequestAndWaitForReady(spec, 120);
            UNIT_ASSERT_EQUAL_C(ret4.boxes(0).state(), API::EBoxState_READY, EBoxState_Name(ret4.boxes(0).state()));

            // Case 3: Volume link path is not directory
            TString nonDirectoryPath = "/bin/rbash";
            volume = spec.mutable_spec()->mutable_boxes(0)->add_volumes();
            volume->set_volume_ref("my_volume");
            volume->set_mount_point(nonDirectoryPath);

            spec.set_spec_timestamp(5);
            API::TPodAgentStatus ret5 = UpdatePodAgentRequestAndWaitForReady(spec, 120, breakHook);

            TString nonDirectoryFullPath = rootFsPath + nonDirectoryPath;
            UNIT_ASSERT_EQUAL_C(ret5.boxes(0).state(), API::EBoxState_INVALID, EBoxState_Name(ret5.boxes(0).state()));
            UNIT_ASSERT_EQUAL_C(
                ret5.boxes(0).failed().message()
                , TStringBuilder() << "volume link path '" << nonDirectoryFullPath << "' is not directory"
                , ret5.boxes(0).failed().message()
            );

            spec.mutable_spec()->mutable_boxes(0)->clear_volumes();
            spec.set_spec_timestamp(6);
            API::TPodAgentStatus ret6 = UpdatePodAgentRequestAndWaitForReady(spec, 120);
            UNIT_ASSERT_EQUAL_C(ret6.boxes(0).state(), API::EBoxState_READY, EBoxState_Name(ret6.boxes(0).state()));

            // Case 4: Rbind volume link path is not directory
            volume = spec.mutable_spec()->mutable_boxes(0)->add_volumes();
            volume->set_rbind_volume_ref("my_rbind_volume");
            volume->set_mount_point(nonDirectoryPath);
            spec.set_spec_timestamp(7);

            API::TPodAgentStatus ret7 = UpdatePodAgentRequestAndWaitForReady(spec, 120, breakHook);
            UNIT_ASSERT_EQUAL_C(ret7.boxes(0).state(), API::EBoxState_INVALID, EBoxState_Name(ret7.boxes(0).state()));
            UNIT_ASSERT_EQUAL_C(
                ret7.boxes(0).failed().message()
                , TStringBuilder() << "rbind volume mount path '" << nonDirectoryFullPath << "' is not directory"
                , ret7.boxes(0).failed().message()
            );
        }
    };

    TTest test;
    test.Test();
}

}

} // namespace NInfra::NPodAgent::NDaemonTest
