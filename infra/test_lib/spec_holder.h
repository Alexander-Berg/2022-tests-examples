#pragma once

#include "test_functions.h"

#include <infra/pod_agent/libs/service_iface/protos/pod_agent_api.pb.h>

#include <library/cpp/protobuf/json/json2proto.h>
#include <library/cpp/testing/unittest/registar.h>

#include <util/stream/file.h>

namespace NInfra::NPodAgent::NDaemonTest  {

class TSpecHolder {
public:
    TSpecHolder(const TString& path) {
        TFileInput specs(path);
        NJson::ReadJsonTree(specs.ReadAll(), &Specs_, true);
    }

    API::TPodAgentRequest GetLayerSpec() {
        UNIT_ASSERT(Specs_.Has("layer_spec"));
        return NProtobufJson::Json2Proto<API::TPodAgentRequest>(Specs_["layer_spec"].GetStringRobust(), GetSpecHolderJson2ProtoConfig());
    }

    API::TPodAgentRequest GetLayerBadNameSpec() {
        UNIT_ASSERT(Specs_.Has("layer_bad_name_spec"));
        return NProtobufJson::Json2Proto<API::TPodAgentRequest>(Specs_["layer_bad_name_spec"].GetStringRobust(), GetSpecHolderJson2ProtoConfig());
    }

    API::TPodAgentRequest GetTestStdoutFileSpec() {
        UNIT_ASSERT(Specs_.Has("test_stdout_file_spec"));
        return NProtobufJson::Json2Proto<API::TPodAgentRequest>(Specs_["test_stdout_file_spec"].GetStringRobust(), GetSpecHolderJson2ProtoConfig());
    }

    API::TPodAgentRequest GetTestFilesSpec() {
        UNIT_ASSERT(Specs_.Has("test_files_spec"));
        NProtobufJson::TJson2ProtoConfig json2ProtoConfig = GetSpecHolderJson2ProtoConfig();
        json2ProtoConfig.SetMapAsObject(true);
        return NProtobufJson::Json2Proto<API::TPodAgentRequest>(Specs_["test_files_spec"].GetStringRobust(), json2ProtoConfig);
    }

    API::TPodAgentRequest GetInvalidStartCmdSpec() {
        UNIT_ASSERT(Specs_.Has("invalid_start_cmd_spec"));
        return NProtobufJson::Json2Proto<API::TPodAgentRequest>(Specs_["invalid_start_cmd_spec"].GetStringRobust(), GetSpecHolderJson2ProtoConfig());
    }

    API::TPodAgentRequest GetStaticResourceSpec(ui64 specTimestamp) {
        UNIT_ASSERT(Specs_.Has("static_resource_spec"));
        API::TPodAgentRequest result;
        NProtobufJson::Json2Proto(Specs_["static_resource_spec"], result, GetSpecHolderJson2ProtoConfig());

        if (specTimestamp == 1) {
            // nothing to do
        } else {
            ui64 checkPeriodMs = result.spec().resources().static_resources(0).verification().check_period_ms();
            result.mutable_spec()->mutable_resources()->mutable_static_resources(0)->mutable_verification()->set_check_period_ms(checkPeriodMs + 1);
        }

        result.set_spec_timestamp(specTimestamp);
        result.mutable_spec()->set_revision(specTimestamp + 1);
        return result;
    }

    API::TPodAgentRequest GetOtherLayerSpec() {
        UNIT_ASSERT(Specs_.Has("other_layer_spec"));
        return NProtobufJson::Json2Proto<API::TPodAgentRequest>(Specs_["other_layer_spec"].GetStringRobust(), GetSpecHolderJson2ProtoConfig());
    }

    API::TPodAgentRequest GetVolumeSpec() {
        UNIT_ASSERT(Specs_.Has("volume_spec"));
        return NProtobufJson::Json2Proto<API::TPodAgentRequest>(Specs_["volume_spec"].GetStringRobust(), GetSpecHolderJson2ProtoConfig());
    }

    API::TPodAgentRequest GetWorkloadSpec() {
        UNIT_ASSERT(Specs_.Has("workload_spec"));
        return NProtobufJson::Json2Proto<API::TPodAgentRequest>(Specs_["workload_spec"].GetStringRobust(), GetSpecHolderJson2ProtoConfig());
    }

    API::TPodAgentRequest GetExtendedWorkloadSpec() {
        UNIT_ASSERT(Specs_.Has("workload_extended_spec"));
        return NProtobufJson::Json2Proto<API::TPodAgentRequest>(Specs_["workload_extended_spec"].GetStringRobust(), GetSpecHolderJson2ProtoConfig());
    }

    API::TPodAgentRequest GetWorkloadUpdateStartSpec() {
        UNIT_ASSERT(Specs_.Has("workload_update_start_spec"));
        return NProtobufJson::Json2Proto<API::TPodAgentRequest>(Specs_["workload_update_start_spec"].GetStringRobust(), GetSpecHolderJson2ProtoConfig());
    }

    API::TPodAgentRequest GetWorkloadUpdateEndSpec() {
        UNIT_ASSERT(Specs_.Has("workload_update_end_spec"));
        return NProtobufJson::Json2Proto<API::TPodAgentRequest>(Specs_["workload_update_end_spec"].GetStringRobust(), GetSpecHolderJson2ProtoConfig());
    }

    API::TPodAgentRequest GetWorkloadRemoveEndSpec() {
        UNIT_ASSERT(Specs_.Has("workload_remove_end_spec"));
        return NProtobufJson::Json2Proto<API::TPodAgentRequest>(Specs_["workload_remove_end_spec"].GetStringRobust(), GetSpecHolderJson2ProtoConfig());
    }

    API::TPodAgentRequest GetTestInContainerWorkloadSpec() {
        UNIT_ASSERT(Specs_.Has("test_in_container_workload_spec"));
        return NProtobufJson::Json2Proto<API::TPodAgentRequest>(Specs_["test_in_container_workload_spec"].GetStringRobust(), GetSpecHolderJson2ProtoConfig());
    }

    API::TPodAgentRequest GetTestVolumePersistencySpec() {
        UNIT_ASSERT(Specs_.Has("volume_persistency_spec"));
        return NProtobufJson::Json2Proto<API::TPodAgentRequest>(Specs_["volume_persistency_spec"].GetStringRobust(), GetSpecHolderJson2ProtoConfig());
    }

    API::TPodAgentRequest GetTestInContainerBigWorkloadSpec() {
        UNIT_ASSERT(Specs_.Has("test_in_container_workload_spec"));

        // layer and volume counts per box are fixed in spec template
        const size_t workloadPerBox = 3;
        const size_t boxCount = 5;

        // different download urls to avoid cache
        const TVector<TMap<TString, TString>> downloadData = {
            {
                {"Layer_MyRootFS", "https://proxy.sandbox.yandex-team.ru/512457792"},
                {"Layer_MyData1", "rbtorrent:3f64511b57514d0b613514fda4728584f3232db6"},
                {"Layer_MyData2", "https://proxy.sandbox.yandex-team.ru/617090665"},
                {"Resource_MyData1", "rbtorrent:4be7b762eecbe3f929574d1ac326c90ae28cc5b5"}
            },
            {
                {"Layer_MyRootFS", "rbtorrent:1111e1623988e9898bd16f91eee03530b68c4c8d"},
                {"Layer_MyData1", "https://proxy.sandbox.yandex-team.ru/836355649"},
                {"Layer_MyData2", "rbtorrent:63c217027be09863deaa9091c6a018d133e691f2"},
                {"Resource_MyData1", "https://proxy.sandbox.yandex-team.ru/1648608543"}
            },
            {
                {"Layer_MyRootFS", "https://proxy.sandbox.yandex-team.ru/836337504"},
                {"Layer_MyData1", "https://proxy.sandbox.yandex-team.ru/836363783"},
                {"Layer_MyData2", "https://proxy.sandbox.yandex-team.ru/836378223"},
                {"Resource_MyData1", "https://proxy.sandbox.yandex-team.ru/1648609724"}
            },
            {
                {"Layer_MyRootFS", "https://proxy.sandbox.yandex-team.ru/836369894"},
                {"Layer_MyData1", "https://proxy.sandbox.yandex-team.ru/836357881"},
                {"Layer_MyData2", "https://proxy.sandbox.yandex-team.ru/836381749"},
                {"Resource_MyData1", "https://proxy.sandbox.yandex-team.ru/1648610174"}
            },
            {
                {"Layer_MyRootFS", "https://proxy.sandbox.yandex-team.ru/836347983"},
                {"Layer_MyData1", "https://proxy.sandbox.yandex-team.ru/836366116"},
                {"Layer_MyData2", "https://proxy.sandbox.yandex-team.ru/836384662"},
                {"Resource_MyData1", "https://proxy.sandbox.yandex-team.ru/1648654361"}
            },
        };
        UNIT_ASSERT_EQUAL_C(downloadData.size(), boxCount, downloadData.size());

        API::TPodAgentRequest specTemplate;
        API::TPodAgentRequest result;
        NProtobufJson::Json2Proto(Specs_["test_in_container_workload_spec"], specTemplate, GetSpecHolderJson2ProtoConfig());

        // Copy full spec
        result.CopyFrom(specTemplate);
        // Remove template objects from spec
        // They will be added below
        result.mutable_spec()->mutable_resources()->clear_layers();
        result.mutable_spec()->mutable_resources()->clear_static_resources();
        result.mutable_spec()->clear_volumes();
        result.mutable_spec()->clear_boxes();
        result.mutable_spec()->clear_workloads();
        result.mutable_spec()->clear_mutable_workloads();

        auto getIdInBox = [](const TString& id, ui32 boxNum) {
            return TStringBuilder()
                << id
                << "_box_" << ToString(boxNum)
            ;
        };
        auto getWorkloadId = [](const TString& id, ui32 boxNum, ui32 workloadNum) {
            return TStringBuilder()
                << id
                << "_box_" << ToString(boxNum)
                << "_workload_" << ToString(workloadNum)
            ;
        };

        auto patchSkyGetDownload = [](API::TSkyGetDownload* skyGetDownload, TProtoStringType* checksum, ui32 boxNum) {
            if (boxNum % 3 == 0) {
                skyGetDownload->set_deduplicate(API::ESkyGetDeduplicateMode_NO);
            } else if (boxNum % 3 == 1) {
                skyGetDownload->set_deduplicate(API::ESkyGetDeduplicateMode_HARDLINK);
                // The checksum may change if the file will be replaced with a hardlink
                *checksum = "EMPTY:";
            } else {
                skyGetDownload->set_deduplicate(API::ESkyGetDeduplicateMode_SYMLINK);
                // The checksum may change if the file will be replaced with a simlink
                *checksum = "EMPTY:";
            }
        };

        // inflate spec to match expected real load
        for (ui32 boxNum = 0; boxNum < boxCount; ++boxNum) {
            for (const auto& layer : specTemplate.spec().resources().layers()) {
                API::TLayer* currentLayer = result.mutable_spec()->mutable_resources()->add_layers();
                currentLayer->CopyFrom(layer);

                if (currentLayer->download_method_case() == API::TLayer::DownloadMethodCase::kUrl) {
                    currentLayer->set_url(downloadData[boxNum].at(currentLayer->id()));
                } else {
                    // Resid (rbtorrent) will be the same for the same resource, so we change the deduplication setting
                    patchSkyGetDownload(
                        currentLayer->mutable_sky_get()
                        , currentLayer->mutable_checksum()
                        , boxNum
                    );
                }
                currentLayer->set_id(getIdInBox(currentLayer->id(), boxNum));
            }

            for (const auto& staticResource : specTemplate.spec().resources().static_resources()) {
                API::TResource* currentStaticResource = result.mutable_spec()->mutable_resources()->add_static_resources();
                currentStaticResource->CopyFrom(staticResource);

                if (currentStaticResource->download_method_case() == API::TResource::DownloadMethodCase::kUrl) {
                    currentStaticResource->set_url(downloadData[boxNum].at(currentStaticResource->id()));
                } else {
                    // Resid (rbtorrent) will be the same for the same resource, so we change the deduplication setting
                    patchSkyGetDownload(
                        currentStaticResource->mutable_sky_get()
                        , currentStaticResource->mutable_verification()->mutable_checksum()
                        , boxNum
                    );
                }
                currentStaticResource->set_id(getIdInBox(currentStaticResource->id(), boxNum));
            }

            for (const auto& volume : specTemplate.spec().volumes()) {
                API::TVolume* currentVolume = result.mutable_spec()->add_volumes();
                currentVolume->CopyFrom(volume);

                for (auto& layerRef : *currentVolume->mutable_generic()->mutable_layer_refs()) {
                    layerRef = getIdInBox(layerRef, boxNum);
                }
                currentVolume->set_id(getIdInBox(currentVolume->id(), boxNum));
            }

            for (const auto& box : specTemplate.spec().boxes()) {
                API::TBox* currentBox = result.mutable_spec()->add_boxes();
                currentBox->CopyFrom(box);

                for (auto& layerRef : *currentBox->mutable_rootfs()->mutable_layer_refs()) {
                    layerRef = getIdInBox(layerRef, boxNum);
                }
                for (auto& mountedVolume : *currentBox->mutable_volumes()) {
                    mountedVolume.set_volume_ref(getIdInBox(mountedVolume.volume_ref(), boxNum));
                }
                for (auto& staticResource : *currentBox->mutable_static_resources()) {
                    staticResource.set_resource_ref(getIdInBox(staticResource.resource_ref(), boxNum));
                }
                currentBox->set_id(getIdInBox(currentBox->id(), boxNum));
            }

            for (size_t workloadNum = 0; workloadNum < workloadPerBox; ++workloadNum) {
                for (const auto& workload : specTemplate.spec().workloads()) {
                    API::TWorkload* currentWorkload = result.mutable_spec()->add_workloads();
                    currentWorkload->CopyFrom(workload);

                    currentWorkload->set_box_ref(getIdInBox(currentWorkload->box_ref(), boxNum));
                    currentWorkload->set_id(getWorkloadId(currentWorkload->id(), boxNum, workloadNum));
                }

                for (const auto& mutableWorkload : specTemplate.spec().mutable_workloads()) {
                    API::TMutableWorkload* currentMutableWorkload = result.mutable_spec()->add_mutable_workloads();
                    currentMutableWorkload->CopyFrom(mutableWorkload);

                    currentMutableWorkload->set_workload_ref(getWorkloadId(currentMutableWorkload->workload_ref(), boxNum, workloadNum));
                }
            }
        }

        return result;
    }

    API::TPodAgentRequest GetSelectiveRebuildSpec(ui64 specTimestamp) {
        UNIT_ASSERT(Specs_.Has("selective_rebuild_spec"));
        API::TPodAgentRequest result;
        NProtobufJson::Json2Proto(Specs_["selective_rebuild_spec"], result, GetSpecHolderJson2ProtoConfig());

        if (specTimestamp == 1) {
            // do nothing
        } else if (specTimestamp == 2) {
            *result.mutable_spec()->mutable_volumes(1)->mutable_generic()->add_layer_refs() = "Layer_MyData1";
        } else if (specTimestamp == 3) {
            result.mutable_spec()->mutable_resources()->mutable_layers(0)->set_url("local:search_ubuntu_precise_copy/layer.tar");
        } else {
            result.mutable_spec()->mutable_resources()->mutable_layers(0)->set_url("local:search_ubuntu_precise_copy/layer.tar");

            result.mutable_spec()->mutable_workloads(0)->mutable_stop_policy()->mutable_container()->set_command_line("bash -c \"echo stop4 > /data/stop_file_4.txt\"");
        }

        result.set_spec_timestamp(specTimestamp);
        result.mutable_spec()->set_revision(specTimestamp + 1);
        return result;
    }

    API::TPodAgentRequest GetRemoveSpec(ui64 specTimestamp) {
        UNIT_ASSERT(Specs_.Has("remove_spec"));
        API::TPodAgentRequest result;
        NProtobufJson::Json2Proto(Specs_["remove_spec"], result, GetSpecHolderJson2ProtoConfig());

        if (specTimestamp == 1) {
            // do nothing
        } else {
            *result.mutable_spec()->mutable_resources()->mutable_layers(0)->mutable_id() += "_other";
            result.mutable_spec()->mutable_resources()->mutable_layers(0)->set_url("local:search_ubuntu_precise_copy/layer.tar");
            *result.mutable_spec()->mutable_boxes(0)->mutable_rootfs()->mutable_layer_refs(0) += "_other";
        }

        result.set_spec_timestamp(specTimestamp);
        result.mutable_spec()->set_revision(specTimestamp + 1);
        return result;
    }

    API::TPodAgentRequest GetAddWithTargetCheckSpec(ui64 specTimestamp) {
        UNIT_ASSERT(Specs_.Has("add_with_target_check_spec"));
        API::TPodAgentRequest result;
        NProtobufJson::Json2Proto(Specs_["add_with_target_check_spec"], result, GetSpecHolderJson2ProtoConfig());

        if (specTimestamp == 1) {
            result.mutable_spec()->clear_volumes();
            result.mutable_spec()->clear_boxes();
            result.mutable_spec()->clear_workloads();
            result.mutable_spec()->clear_mutable_workloads();
        } else {
            result.mutable_spec()->mutable_resources()->mutable_layers(0)->set_url("local:search_ubuntu_precise_copy/layer.tar");
            result.mutable_spec()->mutable_resources()->mutable_layers(1)->set_url("local:layer_small_data_0_copy/layer.tar.gz");
        }

        result.set_spec_timestamp(specTimestamp);
        result.mutable_spec()->set_revision(specTimestamp + 1);
        return result;
    }

    API::TPodAgentRequest GetChaosEndSpec() {
        UNIT_ASSERT(Specs_.Has("chaos_end_spec"));
        return NProtobufJson::Json2Proto<API::TPodAgentRequest>(Specs_["chaos_end_spec"].GetStringRobust(), GetSpecHolderJson2ProtoConfig());
    }

    API::TPodAgentRequest GetWaitingForLayersSpec(ui64 specTimestamp) {
        UNIT_ASSERT(Specs_.Has("waiting_for_layers_spec"));
        API::TPodAgentRequest result;
        NProtobufJson::Json2Proto(Specs_["waiting_for_layers_spec"], result, GetSpecHolderJson2ProtoConfig());

        if (specTimestamp == 1) {
            // correct spec
        } else if (specTimestamp == 2) {
            for (auto& layer : *result.mutable_spec()->mutable_resources()->mutable_layers()) {
                layer.set_checksum("MD5:b7e9081350a579d73d52f60d6fd11d12"); // bad checksum
            }
        } else {
            // correct spec
        }

        result.set_spec_timestamp(specTimestamp);
        result.mutable_spec()->set_revision(specTimestamp + 1);
        return result;
    }

    API::TPodAgentRequest GetWorkloadSecretSpec(ui64 specTimestamp) {
        UNIT_ASSERT(Specs_.Has("workload_secret_spec"));
        NProtobufJson::TJson2ProtoConfig json2ProtoConfig = GetSpecHolderJson2ProtoConfig();
        json2ProtoConfig.SetMapAsObject(true);
        API::TPodAgentRequest result;
        NProtobufJson::Json2Proto(Specs_["workload_secret_spec"], result, json2ProtoConfig);

        if (specTimestamp == 1) {
            // correct spec
        } else {
            *result.mutable_secrets(0)->mutable_values(0)->mutable_value() += "_other";
            *result.mutable_secrets(0)->mutable_values(1)->mutable_value() += "_other";
        }

        result.set_spec_timestamp(specTimestamp);
        result.mutable_spec()->set_revision(specTimestamp + 1);
        return result;
    }

    API::TPodAgentRequest GetBoxInitSecretSpec(ui64 specTimestamp) {
        UNIT_ASSERT(Specs_.Has("box_init_secret_spec"));
        NProtobufJson::TJson2ProtoConfig json2ProtoConfig = GetSpecHolderJson2ProtoConfig();
        json2ProtoConfig.SetMapAsObject(true);
        API::TPodAgentRequest result;
        NProtobufJson::Json2Proto(Specs_["box_init_secret_spec"], result, json2ProtoConfig);

        if (specTimestamp == 1) {
            // correct spec
        } else {
            *result.mutable_secrets(0)->mutable_values(0)->mutable_value() += "_other";
            *result.mutable_secrets(0)->mutable_values(1)->mutable_value() += "_other";
        }

        result.set_spec_timestamp(specTimestamp);
        result.mutable_spec()->set_revision(specTimestamp + 1);
        return result;
    }

    API::TPodAgentRequest GetManyUlimitSpec() {
        UNIT_ASSERT(Specs_.Has("many_ulimit_spec"));
        NProtobufJson::TJson2ProtoConfig json2ProtoConfig = GetSpecHolderJson2ProtoConfig();
        json2ProtoConfig.SetMapAsObject(true);
        return NProtobufJson::Json2Proto<API::TPodAgentRequest>(Specs_["many_ulimit_spec"].GetStringRobust(), json2ProtoConfig);
    }

    API::TPodAgentRequest GetBoxPersistIpAllocationStartSpec(ui64 specTimestamp) {
        UNIT_ASSERT(Specs_.Has("box_persist_ip_allocation"));
        NProtobufJson::TJson2ProtoConfig json2ProtoConfig = GetSpecHolderJson2ProtoConfig();
        json2ProtoConfig.SetMapAsObject(true);
        json2ProtoConfig.AddStringTransform(new NProtobufJson::TBase64DecodeBytesTransform);
        API::TPodAgentRequest result = NProtobufJson::Json2Proto<API::TPodAgentRequest>(Specs_["box_persist_ip_allocation"].GetStringRobust(), json2ProtoConfig);
        if (specTimestamp == 2) {
            (*result.mutable_spec()->mutable_boxes())[0].set_id("new_box");
            (*result.mutable_spec()->mutable_boxes())[1].set_id("test_box_one"); // check that test_box_one will have same ip
        }

        result.set_spec_timestamp(specTimestamp);
        result.mutable_spec()->set_revision(specTimestamp + 1);
        return result;
    }

    API::TPodAgentRequest GetLayerTreeMergeSpec() {
        UNIT_ASSERT(Specs_.Has("layer_tree_merge"));
        return NProtobufJson::Json2Proto<API::TPodAgentRequest>(Specs_["layer_tree_merge"].GetStringRobust(), GetSpecHolderJson2ProtoConfig());
    }

    API::TPodAgentRequest GetCacheLayerMergeSpec(ui64 specTimestamp) {
        UNIT_ASSERT(Specs_.Has("cache_layer_merge"));
        API::TPodAgentRequest result;
        NProtobufJson::Json2Proto(Specs_["cache_layer_merge"], result, GetSpecHolderJson2ProtoConfig());

        if (specTimestamp == 1) {
            result.mutable_spec()->mutable_resources()->mutable_layers()->RemoveLast();
            result.mutable_resource_cache_spec()->mutable_layers(0)->CopyFrom(*result.mutable_resource_cache_spec()->mutable_layers(1));
            result.mutable_resource_cache_spec()->mutable_layers()->RemoveLast();
        } else if (specTimestamp == 2) {
            // correct spec
        } else if (specTimestamp == 3) {
            result.mutable_resource_cache_spec()->mutable_layers()->RemoveLast();
        } else if (specTimestamp == 4) {
            // correct spec
        } else if (specTimestamp == 5) {
            result.mutable_spec()->mutable_resources()->mutable_layers(0)->CopyFrom(*result.mutable_spec()->mutable_resources()->mutable_layers(1));
            result.mutable_spec()->mutable_resources()->mutable_layers()->RemoveLast();
            result.mutable_resource_cache_spec()->mutable_layers()->RemoveLast();
        }

        result.set_spec_timestamp(specTimestamp);
        result.mutable_spec()->set_revision(specTimestamp + 1);
        return result;
    }

    API::TPodAgentRequest GetStaticResourceTreeMergeSpec() {
        UNIT_ASSERT(Specs_.Has("static_resource_tree_merge"));
        return NProtobufJson::Json2Proto<API::TPodAgentRequest>(Specs_["static_resource_tree_merge"].GetStringRobust(), GetSpecHolderJson2ProtoConfig());
    }

    API::TPodAgentRequest GetCacheStaticResourceMergeSpec(ui64 specTimestamp) {
        UNIT_ASSERT(Specs_.Has("cache_static_resource_merge"));
        API::TPodAgentRequest result;
        NProtobufJson::Json2Proto(Specs_["cache_static_resource_merge"], result, GetSpecHolderJson2ProtoConfig());

        if (specTimestamp == 1) {
            result.mutable_spec()->mutable_resources()->mutable_static_resources()->RemoveLast();
            result.mutable_resource_cache_spec()->mutable_static_resources(0)->CopyFrom(*result.mutable_resource_cache_spec()->mutable_static_resources(1));
            result.mutable_resource_cache_spec()->mutable_static_resources()->RemoveLast();
        } else if (specTimestamp == 2) {
            // correct spec
        } else if (specTimestamp == 3) {
            result.mutable_resource_cache_spec()->mutable_static_resources()->RemoveLast();
        } else if (specTimestamp == 4) {
            // correct spec
        } else if (specTimestamp == 5) {
            result.mutable_spec()->mutable_resources()->mutable_static_resources(0)->CopyFrom(*result.mutable_spec()->mutable_resources()->mutable_static_resources(1));
            result.mutable_spec()->mutable_resources()->mutable_static_resources()->RemoveLast();
            result.mutable_resource_cache_spec()->mutable_static_resources()->RemoveLast();
        }

        result.set_spec_timestamp(specTimestamp);
        result.mutable_spec()->set_revision(specTimestamp + 1);
        return result;
    }

    API::TPodAgentRequest GetBoxWithRoRootfsSpec() {
        UNIT_ASSERT(Specs_.Has("box_with_ro_rootfs"));
        return NProtobufJson::Json2Proto<API::TPodAgentRequest>(Specs_["box_with_ro_rootfs"].GetStringRobust(), GetSpecHolderJson2ProtoConfig());
    }

    API::TPodAgentRequest GetBoxWithChildOnlyIsolationSpec() {
        UNIT_ASSERT(Specs_.Has("box_with_child_only_isolation"));
        return NProtobufJson::Json2Proto<API::TPodAgentRequest>(Specs_["box_with_child_only_isolation"].GetStringRobust(), GetSpecHolderJson2ProtoConfig());
    }

    API::TPodAgentRequest GetBoxWithCgroupFsMountSpec() {
        UNIT_ASSERT(Specs_.Has("box_with_cgroup_fs_mount"));
        return NProtobufJson::Json2Proto<API::TPodAgentRequest>(Specs_["box_with_cgroup_fs_mount"].GetStringRobust(), GetSpecHolderJson2ProtoConfig());
    }

    API::TPodAgentRequest GetSkynetInBoxsSpec() {
        UNIT_ASSERT(Specs_.Has("skynet_in_boxes"));
        return NProtobufJson::Json2Proto<API::TPodAgentRequest>(Specs_["skynet_in_boxes"].GetStringRobust(), GetSpecHolderJson2ProtoConfig());
    }

    API::TPodAgentRequest GetYtInBoxsSpec() {
        UNIT_ASSERT(Specs_.Has("yt_in_boxes"));
        return NProtobufJson::Json2Proto<API::TPodAgentRequest>(Specs_["yt_in_boxes"].GetStringRobust(), GetSpecHolderJson2ProtoConfig());
    }

    API::TPodAgentRequest GetBaseSearchInBoxsSpec() {
        UNIT_ASSERT(Specs_.Has("basesearch_in_boxes"));
        return NProtobufJson::Json2Proto<API::TPodAgentRequest>(Specs_["basesearch_in_boxes"].GetStringRobust(), GetSpecHolderJson2ProtoConfig());
    }

    API::TPodAgentRequest GetRbindVolumeSpec() {
        UNIT_ASSERT(Specs_.Has("rbind_volume"));
        return NProtobufJson::Json2Proto<API::TPodAgentRequest>(Specs_["rbind_volume"].GetStringRobust(), GetSpecHolderJson2ProtoConfig());
    }

    API::TPodAgentRequest GetVirtualDisksSpec() {
        UNIT_ASSERT(Specs_.Has("virtual_disks_spec"));
        return NProtobufJson::Json2Proto<API::TPodAgentRequest>(Specs_["virtual_disks_spec"].GetStringRobust(), GetSpecHolderJson2ProtoConfig());
    }

    API::TPodAgentRequest GetSensorsSpec() {
        UNIT_ASSERT(Specs_.Has("sensors_spec"));
        return NProtobufJson::Json2Proto<API::TPodAgentRequest>(Specs_["sensors_spec"].GetStringRobust(), GetSpecHolderJson2ProtoConfig());
    }

    API::TPodAgentRequest GetWorkloadNoReadinessSpec() {
        UNIT_ASSERT(Specs_.Has("workload_no_readiness_spec"));
        return NProtobufJson::Json2Proto<API::TPodAgentRequest>(Specs_["workload_no_readiness_spec"].GetStringRobust(), GetSpecHolderJson2ProtoConfig());
    }

    API::TPodAgentRequest GetPublicVolumeSpec() {
        UNIT_ASSERT(Specs_.Has("public_volume_spec"));
        return NProtobufJson::Json2Proto<API::TPodAgentRequest>(Specs_["public_volume_spec"].GetStringRobust(), GetSpecHolderJson2ProtoConfig());
    }

    API::TPodAgentRequest GetExecWrapperSpec() {
        UNIT_ASSERT(Specs_.Has("exec_wrapper_spec"));
        return NProtobufJson::Json2Proto<API::TPodAgentRequest>(Specs_["exec_wrapper_spec"].GetStringRobust(), GetSpecHolderJson2ProtoConfig());
    }

    API::TPodAgentRequest GetUnixSignalStopSpec(ui64 specTimestamp) {
        UNIT_ASSERT(Specs_.Has("unix_signal_stop_spec"));
        API::TPodAgentRequest result;
        NProtobufJson::Json2Proto(Specs_["unix_signal_stop_spec"], result, GetSpecHolderJson2ProtoConfig());

        if (specTimestamp == 1) {
            // correct spec
        } else if (specTimestamp == 2) {
            result.mutable_spec()->mutable_mutable_workloads(0)->set_target_state(API::EWorkloadTarget_REMOVED);
            result.mutable_spec()->mutable_mutable_workloads(1)->set_target_state(API::EWorkloadTarget_REMOVED);
        }

        result.set_spec_timestamp(specTimestamp);
        result.mutable_spec()->set_revision(specTimestamp + 1);
        return result;
    }

    API::TPodAgentRequest GetPodAgentTargetStateSpec(ui64 specTimestamp) {
        UNIT_ASSERT(Specs_.Has("pod_agent_target_state_spec"));
        API::TPodAgentRequest result;
        NProtobufJson::Json2Proto(Specs_["pod_agent_target_state_spec"], result, GetSpecHolderJson2ProtoConfig());

        if (specTimestamp == 1) {
            result.mutable_spec()->set_target_state(API::EPodAgentTargetState_UNKNOWN);
        } else if (specTimestamp == 2) {
            result.mutable_spec()->set_target_state(API::EPodAgentTargetState_SUSPENDED);
        } else if (specTimestamp == 3) {
            result.mutable_spec()->set_target_state(API::EPodAgentTargetState_ACTIVE);
        } else if (specTimestamp == 4) {
            result.mutable_spec()->set_target_state(API::EPodAgentTargetState_REMOVED);
        } else if (specTimestamp == 5) {
            result.mutable_spec()->set_target_state(API::EPodAgentTargetState_ACTIVE);
        }

        result.set_spec_timestamp(specTimestamp);
        result.mutable_spec()->set_revision(specTimestamp + 1);
        return result;
    }

    API::TPodAgentRequest GetHiddenSecretsAndDynamicAttributesSpec() {
        UNIT_ASSERT(Specs_.Has("hidden_secrets_spec_and_dynamic_attributes"));
        return NProtobufJson::Json2Proto<API::TPodAgentRequest>(Specs_["hidden_secrets_spec_and_dynamic_attributes"].GetStringRobust(), GetSpecHolderJson2ProtoConfig());
    }

    API::TPodAgentRequest GetVolumeChangePersistentTypeSpec() {
        UNIT_ASSERT(Specs_.Has("volume_change_persistent_type"));
        return NProtobufJson::Json2Proto<API::TPodAgentRequest>(Specs_["volume_change_persistent_type"].GetStringRobust(), GetSpecHolderJson2ProtoConfig());
    }

    API::TPodAgentRequest GetBoxWithRoRootfsAndInvalidMountPointsSpec() {
        UNIT_ASSERT(Specs_.Has("box_with_ro_rootfs_and_invalid_mount_points"));
        return NProtobufJson::Json2Proto<API::TPodAgentRequest>(Specs_["box_with_ro_rootfs_and_invalid_mount_points"].GetStringRobust(), GetSpecHolderJson2ProtoConfig());
    }

private:
    NJson::TJsonValue Specs_;
};

} // namespace NInfra::NPodAgent::NDaemonTest
