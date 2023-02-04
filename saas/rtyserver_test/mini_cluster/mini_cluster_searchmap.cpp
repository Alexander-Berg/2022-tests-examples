#include "mini_cluster_searchmap.h"

namespace NMiniCluster {

    TClusterReplica::TClusterReplica(NJson::TJsonValue& smap){
        if (smap.IsArray()){
            InitBackendsList(smap.GetArray());
            return;
        }
        if (smap.Has("backends")){
            if (!smap["backends"].IsArray())
                FAIL_LOG("backends must be array of strings");
            InitBackendsList(smap["backends"].GetArray());
            smap.EraseValue("backends");
        }
        RepOptions = smap;
    }

    void TClusterReplica::InitBackendsList(NJson::TJsonValue::TArray backs){
        for (const auto& back : backs){
            Backends.emplace_back(back);
        }
    }

    TClusterService::TClusterService(NJson::TJsonValue& smap)
        : Name("tests")
        {
            if (smap.Has("name")){
                Name = smap["name"].GetString();
                smap.EraseValue("name");
            }
            if (smap.Has("replicas")){
                if (!smap["replicas"].IsArray())
                    FAIL_LOG("replicas must be array");
                for (size_t i = 0; i < smap["replicas"].GetArray().size(); ++i)
                    Replicas.emplace_back(smap["replicas"][i]);
                smap.EraseValue("replicas");
            }
            Options = smap;
        }

    void TClusterSearchmap::Init(NJson::TJsonValue smap){
        SmapFilename = "searchmap.json";
        if (smap.Has("filename") && smap["filename"].GetString()) {
            SmapFilename = smap["filename"].GetString();
        }
        if (smap.Has("replicas") && smap.Has("searchmap"))
            FAIL_LOG("define either 'searchmap' (full form) or 'replicas' (short form), not bouth");
        if (!smap.Has("replicas") && !smap.Has("searchmap"))
            return;
        if (smap.Has("replicas")){
            InitShort(smap["replicas"]);
            return;
        }
        if (smap.Has("searchmap")){
            if (!smap["searchmap"].IsArray())
                FAIL_LOG("'searchmap' must be array of services maps");
            for (size_t i = 0; i < smap["searchmap"].GetArray().size(); ++i)
                Services.emplace_back(smap["searchmap"][i]);
        }
    }
    void TClusterSearchmap::InitShort(NJson::TJsonValue smap){
        SmapFilename = "searchmap.json";
        if (smap.Has("filename") && smap["filename"].GetString()) {
            SmapFilename = smap["filename"].GetString();
        }
        if (!smap.IsArray())
            FAIL_LOG("'replicas' must be array");
        NJson::TJsonValue service;
        service["replicas"] = smap;
        Services.emplace_back(service);
    }

    TClusterBackend::TClusterBackend(const NJson::TJsonValue& smap) {
        NSearchMapParser::TShardsInterval interval(0,0);

        if (smap.IsString()) {
            Name = smap.GetString();
        } else if (smap.IsMap()) {
            if (!smap.Has("name") || !smap["name"].IsString()) {
                ythrow yexception() << "backend must has name, got: " << smap;
            }
            Name = smap["name"].GetString();
            if (smap.Has("shard_min")) {
                interval.SetMin(smap["shard_min"].GetUIntegerRobust());
            }
            if (smap.Has("shard_max")) {
                interval.SetMax(smap["shard_max"].GetUIntegerRobust());
            }
            if (smap.Has("patch")) {
                CHECK_WITH_LOG(Host.Deserialize(smap["patch"]));
            }
        } else {
            ythrow yexception() << "backend must be a string or map, got: " << smap;
        }

        Host.Shards = interval;
    }

}
