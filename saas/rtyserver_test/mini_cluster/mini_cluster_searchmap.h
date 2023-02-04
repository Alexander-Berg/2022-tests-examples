#pragma once

#include <saas/library/sharding/sharding.h>
#include <saas/library/searchmap/host.h>

#include <library/cpp/logger/global/global.h>

#include <library/cpp/json/json_value.h>
#include <util/string/vector.h>
#include <util/generic/vector.h>
#include <util/generic/string.h>

namespace NMiniCluster {
    struct TClusterBackend {
    public:
        TClusterBackend(const NJson::TJsonValue& smap);
    public:
        TString Name;
        NSearchMapParser::TSearchMapHost Host;
    };
    struct TClusterReplica {
    public:
        TClusterReplica(NJson::TJsonValue& smap);
    public:
        NJson::TJsonValue RepOptions;
        TVector<TClusterBackend> Backends;
    private:
        void InitBackendsList(NJson::TJsonValue::TArray backs);
    };

    typedef TVector<TClusterReplica> TClusterReplicas;

    struct TClusterService {
        TClusterService(NJson::TJsonValue& smap);
        TString Name;
        TClusterReplicas Replicas;
        NJson::TJsonValue Options;
    };

    typedef TVector<TClusterService> TClusterServices;

    struct TClusterSearchmap {
        void Init(NJson::TJsonValue smap);
        void InitShort(NJson::TJsonValue smap);
        TClusterServices Services;
        TString SmapFilename;
    };
}
