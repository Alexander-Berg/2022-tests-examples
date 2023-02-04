#pragma once

#include <infra/yp_service_discovery/functional_tests/common/common.h>

#include <infra/yp_service_discovery/functional_tests/common/daemon_runner.h>

#include <infra/yp_service_discovery/api/api.grpc.pb.h>
#include <infra/libs/yp_replica/testing/testing_storage.h>
#include <infra/libs/yp_replica/storage.h>
#include <infra/libs/yp_replica/yp_replica.h>

#include <library/cpp/json/json_value.h>
#include <library/cpp/json/json_writer.h>
#include <library/cpp/protobuf/json/proto2json.h>
#include <library/cpp/scheme/ut_utils/scheme_ut_utils.h>
#include <library/cpp/string_utils/base64/base64.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/tests_data.h>

#include <util/system/hostname.h>


constexpr ui64 STORAGE_JSON_FORMAT_VERSION = 10;
constexpr TStringBuf DEFAULT_UPDATING_FREQUENCY = "1ms";
constexpr TStringBuf DEFAULT_TIMEOUT = "10s";
const TString PATH_TO_CONFIG = "patched_config.json";

constexpr std::initializer_list<TStringBuf> CLUSTERS = {
    TStringBuf("sas-test"),
    TStringBuf("sas"),
    TStringBuf("man-pre"),
    TStringBuf("man"),
    TStringBuf("vla"),
    TStringBuf("myt"),
    TStringBuf("iva"),
};

void InitEndpointStorages(ui64 storageFormatVersion = STORAGE_JSON_FORMAT_VERSION);
void InitPodStorages(ui64 storageFormatVersion = STORAGE_JSON_FORMAT_VERSION);
void InitNodeStorages(ui64 storageFormatVersion = STORAGE_JSON_FORMAT_VERSION);
void PatchConfig();
NProtoConfig::TLoadConfigOptions GetPatchedConfigOptions();
