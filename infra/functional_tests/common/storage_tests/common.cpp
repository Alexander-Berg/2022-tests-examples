#include "common.h"

#include <infra/libs/yp_replica/backup.h>

void InitEndpointStorages(ui64 storageFormatVersion) {
    NYP::NYPReplica::TStorageOptions options;
    options.Meta.Version = storageFormatVersion;
    for (const TStringBuf cluster : CLUSTERS) {
        options.Paths.StoragePath = TFsPath("storage/endpoints") / cluster;
        options.Paths.BackupPath = TFsPath("backup/endpoints") / cluster;
        NYP::NYPReplica::NTesting::TTestingStorage<NYP::NYPReplica::TEndpointReplicaObject, NYP::NYPReplica::TEndpointSetReplicaObject> storage(options);
        UNIT_ASSERT(storage.Open(/* validate */ false));
        storage.LoadFromJson("endpoint_storage.json", /* decode */ true);

        NYP::NYPReplica::TBackupEngineOptions backupOptions;
        backupOptions.Path = options.Paths.BackupPath;
        NYP::NYPReplica::TBackupEngine backup(std::move(backupOptions));
        backup.Drop();
    }
}

void InitPodStorages(ui64 storageFormatVersion) {
    NYP::NYPReplica::TStorageOptions options;
    options.Meta.Version = storageFormatVersion;
    for (const TStringBuf cluster : CLUSTERS) {
        options.Paths.StoragePath = TFsPath("storage/pods") / cluster;
        options.Paths.BackupPath = TFsPath("backup/pods") / cluster;
        NYP::NYPReplica::NTesting::TTestingStorage<NYP::NYPReplica::TPodReplicaObject> storage(options);
        UNIT_ASSERT(storage.Open(/* validate */ false));
        storage.LoadFromJson("pod_storage.json", /* decode */ true);
    }
}

void InitNodeStorages(ui64 storageFormatVersion) {
    NYP::NYPReplica::TStorageOptions options;
    options.Meta.Version = storageFormatVersion;
    for (const TStringBuf cluster : CLUSTERS) {
        options.Paths.StoragePath = TFsPath("storage/node") / cluster;
        options.Paths.BackupPath = TFsPath("backup/node") / cluster;
        NYP::NYPReplica::NTesting::TTestingStorage<NYP::NYPReplica::TPodWithNodeIdKeyReplicaObject> storage(options);
        UNIT_ASSERT(storage.Open(/* validate */ false));
        storage.LoadFromJson("node_storage.json", /* decode */ true);
    }
}

void PatchConfig() {
    auto config = NYP::NServiceDiscovery::GetDefaultConfig();

    config["YPEndpointClusterConfigs"] = NJson::JSON_ARRAY;
    config["YPPodClusterConfigs"] = NJson::JSON_ARRAY;
    config["YPNodeClusterConfigs"] = NJson::JSON_ARRAY;
    for (const TStringBuf clusterName : CLUSTERS) {
        NJson::TJsonValue cluster;
        cluster["Name"] = clusterName;
        cluster["EnableSsl"] = false;
        cluster["UpdatingFrequency"] = DEFAULT_UPDATING_FREQUENCY;
        cluster["SelectAllMinimalInterval"] = "0s";
        cluster["Timeout"] = DEFAULT_TIMEOUT;
        cluster["UpdateIfNoBackup"] = false;

        config["YPEndpointClusterConfigs"].AppendValue(cluster);
        config["YPPodClusterConfigs"].AppendValue(cluster);
        config["YPNodeClusterConfigs"].AppendValue(std::move(cluster));
    }

    TFileOutput configFile{PATH_TO_CONFIG};
    configFile << WriteJson(config);
}

NProtoConfig::TLoadConfigOptions GetPatchedConfigOptions() {
    PatchConfig();
    NProtoConfig::TLoadConfigOptions options;
    options.Path = PATH_TO_CONFIG;
    return options;
}
