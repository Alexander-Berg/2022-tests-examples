#pragma once

#include "decode.h"

#include <infra/libs/yp_replica/storage.h>
#include <infra/libs/yp_replica/table.h>

#include <infra/libs/logger/test_common.h>

#include <yp/cpp/yp/data_model.h>

#include <library/cpp/json/json_reader.h>
#include <library/cpp/json/json_writer.h>

#include <library/cpp/protobuf/json/json2proto.h>
#include <library/cpp/protobuf/json/proto2json.h>

#include <util/stream/file.h>

namespace NYP::NYPReplica::NTesting {

template <typename TReplicaObject>
struct TProtobufMessage {
    using Type = void;
};

template <>
struct TProtobufMessage<NYP::NYPReplica::TEndpointSetReplicaObject> {
    using Type = NYP::NClient::NApi::NProto::TEndpointSet; 
};

template <>
struct TProtobufMessage<NYP::NYPReplica::TEndpointReplicaObject> {
    using Type = NYP::NClient::NApi::NProto::TEndpoint; 
};

template <>
struct TProtobufMessage<NYP::NYPReplica::TPodReplicaObject> {
    using Type = NYP::NClient::NApi::NProto::TPod; 
};

template <>
struct TProtobufMessage<NYP::NYPReplica::TPodWithNodeIdKeyReplicaObject> {
    using Type = NYP::NClient::NApi::NProto::TPod; 
};

template <>
struct TProtobufMessage<NYP::NYPReplica::TDnsRecordSetReplicaObject> {
    using Type = NYP::NClient::NApi::NProto::TDnsRecordSet; 
};

template <>
struct TProtobufMessage<NYP::NYPReplica::TDnsZoneReplicaObject> {
    using Type = NYP::NClient::NApi::NProto::TDnsZone; 
};

struct TDescriptionOptions {
    bool IsFull = true;
    bool OnlyIdAndKey = false;
};

bool IsFullJson(const NJson::TJsonValue& json);

template <typename TReplicaObject>
TReplicaObject JsonToReplicaObject(const NJson::TJsonValue& jsonObject) {
    typename TProtobufMessage<TReplicaObject>::Type message;
    auto cfg = NProtobufJson::TJson2ProtoConfig();
    cfg.SetMapAsObject(true);
    NProtobufJson::Json2Proto(jsonObject["object"], message, cfg);
    typename TReplicaObject::TObject object;
    if (message.has_meta()) {
        object.MutableMeta()->CopyFrom(message.meta());
    }
    if (message.has_spec()) {
        object.MutableSpec()->CopyFrom(message.spec());
    }
    if (message.has_status()) {
        object.MutableStatus()->CopyFrom(message.status());
    }
    if (jsonObject.Has("labels")) {
        *object.MutableLabels() = jsonObject["labels"];
    }
    if (jsonObject.Has("annotations")) {
        *object.MutableAnnotations() = jsonObject["annotations"];
    }
    return TReplicaObject(object);
}

template <class TReplicaObject>
NJson::TJsonValue ReplicaObjectToJson(const TReplicaObject& replicaObject) {
    typename TProtobufMessage<TReplicaObject>::Type message;
    NJson::TJsonValue jsonObject;
    auto cfg = NProtobufJson::TProto2JsonConfig();
    cfg.SetMapAsObject(true);
    typename TReplicaObject::TObject object = replicaObject.GetObject();
    if (object.HasMeta()) {
        message.mutable_meta()->CopyFrom(object.Meta());
    }
    if (object.HasSpec()) {
        message.mutable_spec()->CopyFrom(object.Spec());
    }
    if (object.HasStatus()) {
        message.mutable_status()->CopyFrom(object.Status());
    }
    NProtobufJson::Proto2Json(message, jsonObject["object"], cfg);
    if (object.HasLabels()) {
        jsonObject["labels"] = object.Labels();
    }
    if (object.HasAnnotations()) {
        jsonObject["annotations"] = object.Annotations();
    }
    return jsonObject;
}

template <typename TReplicaObject>
TString JsonToStorageValue(const NJson::TJsonValue& jsonObject) {
    return JsonToReplicaObject<TReplicaObject::TObject>(jsonObject).ToString();
}

template <typename TReplicaObject>
NJson::TJsonValue StorageValueToJson(const TString& strObject) {
    TReplicaObject object;
    object.FromString(strObject);
    return ReplicaObjectToJson<TReplicaObject::TObject>(object);
}

template <typename... TRestReplicaObjects>
typename std::enable_if_t<sizeof...(TRestReplicaObjects) == 0> AddMetaColumnFamily(TStorageOptions& /* options */) {
}

template <typename TReplicaObject, typename... TRestReplicaObjects>
void AddMetaColumnFamily(TStorageOptions& options) {
    options.ColumnFamilies.push_back(NYP::NYPReplica::GetMetaColumnFamilyName<TReplicaObject>());
    AddMetaColumnFamily<TRestReplicaObjects...>(options);
}

template <typename... TReplicaObjects>
TStorageOptions AddColumnFamily(TStorageOptions options, TTableRulesHolder<TReplicaObjects...>&& rulesHolder) {
    NYP::NYPReplica::TTableManagers tableManagers(std::move(rulesHolder));
    for (const TString& columnFamily : tableManagers.template GetAllStableColumnFamilyNames<TReplicaObjects...>()) {
        options.ColumnFamilies.push_back(columnFamily);
    }
    AddMetaColumnFamily<TReplicaObjects...>(options);
    return options;
}

template <typename... TRestReplicaObjects>
typename std::enable_if_t<sizeof...(TRestReplicaObjects) == 0, std::tuple<TUpdates<TRestReplicaObjects>...>> GetUpdates(
    const NJson::TJsonValue& /* jsonReplica */, 
    const ui64 /* ypTimestamp */,
    const TInstant& /* timestamp */
) {
    return {};
}

template <typename TReplicaObject, typename... TRestReplicaObjects>
std::tuple<TUpdates<TReplicaObject>, TUpdates<TRestReplicaObjects>...> GetUpdates(const NJson::TJsonValue& jsonReplica, const ui64 ypTimestamp, const TInstant& timestamp) {
    std::tuple<TUpdates<TRestReplicaObjects>...> restUpdates(GetUpdates<TRestReplicaObjects...>(jsonReplica, ypTimestamp, timestamp));
    TUpdates<TReplicaObject> firstUpdates;
    firstUpdates.YpTimestamp = ypTimestamp;
    firstUpdates.Timestamp = timestamp;
    firstUpdates.SnapshotObjectsNumber = 0;
    if (jsonReplica.Has(TReplicaObject::NAME)) {
        const auto& replicaObjects = jsonReplica[TReplicaObject::NAME].GetArray();
        firstUpdates.Data.reserve(replicaObjects.size());
        firstUpdates.SnapshotObjectsNumber = replicaObjects.size();
        for (const auto& object : replicaObjects) {
            firstUpdates.Data.emplace_back(Nothing(), TStorageElement(JsonToReplicaObject<TReplicaObject>(object)));
        }
    }
    return std::tuple_cat(std::tuple(std::move(firstUpdates)), restUpdates);
}

template <typename... TReplicaObjects>
TStatus FillWriteBatch(
    TStorage& storage,
    TWriteBatch& writeBatch,
    const NJson::TJsonValue& jsonReplica,
    const ui64 ypTimestamp,
    const TInstant& timestamp,
    TTableRulesHolder<TReplicaObjects...>&& rulesHolder = TTableRulesHolder<TReplicaObjects...>()
) {
    TTableManagers<TReplicaObjects...> tableManagers(std::move(rulesHolder));
    THashMap<TString, ui64> objectsNumber;
    auto updates = GetUpdates<TReplicaObjects...>(jsonReplica, ypTimestamp, timestamp);
    NInfra::TLogger logger{NInfra::NTestCommon::CreateLoggerConfig(TStringBuf("DEBUG"), "replica_log")};
    return tableManagers.template ApplyUpdates<TReplicaObjects...>(
        storage,
        writeBatch,
        objectsNumber,
        updates,
        MakeAtomicShared<TUpdatesDisabler>(true),
        logger.SpawnFrame(),
        NInfra::TSensorGroup(TString())
    );
}

template <typename... TRestReplicaObjects>
typename std::enable_if_t<sizeof...(TRestReplicaObjects) == 0> FillJson(
    NJson::TJsonValue& /* jsonStorage */,
    const TStorage& /* storage */,
    const TVector<TString>& /* columnFamilies */,
    const TDescriptionOptions& /* options */
) {
}

template <typename TReplicaObject>
NJson::TJsonValue ReplicaObjectToJson(
    const TReplicaObject& replicaObject,
    const TDescriptionOptions& options
) {
    NJson::TJsonValue result;
    if (options.OnlyIdAndKey) {
        result["id"] = replicaObject.GetObjectId();
        result["key"] = replicaObject.GetKey();
    } else {
        result = ReplicaObjectToJson<TReplicaObject>(replicaObject);
    }
    return result;
}

template <typename TReplicaObject, typename... TRestReplicaObjects>
void FillJson(
    NJson::TJsonValue& jsonResult,
    const TStorage& storage,
    const TVector<TString>& columnFamilies,
    const TDescriptionOptions& options = TDescriptionOptions()
) {
    TReadOptions readOptions = TReadOptions();

    const TString columnFamilyPrefix = TString::Join(TReplicaObject::COLUMN_FAMILY_NAME, "#");
    for (const TString& columnFamily : columnFamilies) {
        if (columnFamily.StartsWith(columnFamilyPrefix)) {
            if (options.IsFull) { // FullType
                ui64 objectsNumber = storage.GetObjectsNumber<TReplicaObject>(readOptions, columnFamily);
                if (objectsNumber) {
                    jsonResult[GetMetaColumnFamilyName<TReplicaObject>()]["objects_number"][columnFamily] = objectsNumber;
                }
                TIterator it = storage.NewIterator(readOptions, columnFamily);
                it.SeekToFirst();
                for (; it.Valid(); it.Next()) {
                    TVector<TStorageElement<TReplicaObject>> objects = it.Value<TReplicaObject>();
                    auto& jsonReplicaObject = jsonResult[columnFamily][it.Key()];
                    for (const auto& storageElement : objects) {
                        jsonReplicaObject.AppendValue(ReplicaObjectToJson<TReplicaObject>(storageElement.ReplicaObject, options));
                    }
                }
            } else { // ShortType
                if (columnFamily.Contains(TMainTableRule<TReplicaObject>::ID)) {
                    TIterator it = storage.NewIterator(readOptions, columnFamily);
                    it.SeekToFirst();
                    for (; it.Valid(); it.Next()) {
                        TVector<TStorageElement<TReplicaObject>> objects = it.Value<TReplicaObject>();
                        auto& jsonReplicaObject = jsonResult["replica_objects"][TReplicaObject::NAME];
                        for (const auto& storageElement : objects) {
                            jsonReplicaObject.AppendValue(ReplicaObjectToJson<TReplicaObject>(storageElement.ReplicaObject, options));
                        }
                    }
                }
            }
        }
    }
    FillJson<TRestReplicaObjects...>(jsonResult, storage, columnFamilies, options);
}

template <typename... TReplicaObjects>
class TTestingStorage : public TStorage {
public:
    using TStorage::TStorage;

    TTestingStorage(TStorageOptions options, TTableRulesHolder<TReplicaObjects...>&& rulesHolder = TTableRulesHolder<TReplicaObjects...>()) 
        : TStorage(AddColumnFamily(std::move(options), std::move(rulesHolder)))
    {
    }

    void LoadFromFullJson(const NJson::TJsonValue& fullJson) {
        Drop();
        Y_ENSURE(Open(/* validate */ false));

        TWriteOptions options;
        Y_ENSURE(UpdateTimestamp(options, TInstant::FromValue(fullJson["internal"]["timestamp"].GetUIntegerRobust())));
        Y_ENSURE(UpdateYpTimestamp(options, fullJson["internal"]["yp_timestamp"].GetUIntegerRobust()));
        for (const TString& columnFamily : Options().ColumnFamilies) {
            for (const auto& [key, jsonValue] : fullJson[columnFamily].GetMap()) {
                Y_ENSURE(Put(options, columnFamily, key, jsonValue.GetStringRobust()));
            }
        }
        Y_ENSURE(UpdateVersion(options, Options().Meta.Version));
    }

    void LoadFromShortJson(const NJson::TJsonValue& jsonReplica) {
        Drop();
        Y_ENSURE(Open(/* validate */ false));

        ui64 timestamp = 0;
        ui64 ypTimestamp = 0;
        ui64 version = Options().Meta.Version;
        if (jsonReplica.Has("meta")) {
            const auto& meta = jsonReplica["meta"];
            if (meta.Has("yp_timestamp")) {
                ypTimestamp = meta["yp_timestamp"].GetUIntegerRobust();
            }
            if (meta.Has("timestamp")) {
                timestamp = meta["timestamp"].GetUIntegerRobust();
            }
            if (meta.Has("version")) {
                version = meta["version"].GetUIntegerRobust();
            }
        }

        TWriteBatch writeBatch = CreateWriteBatch();

        Y_ENSURE(FillWriteBatch<TReplicaObjects...> (
            *this,
            writeBatch,
            jsonReplica["replica_objects"],
            ypTimestamp,
            TInstant::FromValue(timestamp)
        ));

        TWriteOptions options;
        Y_ENSURE(UpdateVersion(options, version));
        Y_ENSURE(Write(options, writeBatch));
    }

    void LoadFromJson(const TFsPath& file, bool decode = false) {
        const TString rawJsonReplica = TFileInput(file).ReadAll();
        NJson::TJsonValue json;
        NJson::ReadJsonTree(rawJsonReplica, &json, true);
        if (decode) {
            DecodeJson(json);
        }
        if (IsFullJson(json)) {
            LoadFromFullJson(json);
        } else {
            LoadFromShortJson(json);
        }
    }

    NJson::TJsonValue GetJsonStorage(const TDescriptionOptions& options = TDescriptionOptions()) const {
        NJson::TJsonValue jsonResult;
        TReadOptions readOptions;

        if (options.IsFull) {
            auto& internal = jsonResult["internal"];
            internal["timestamp"] = GetTimestamp(readOptions).GetValue();
            internal["yp_timestamp"] = GetYpTimestamp(readOptions);
        } else {
            auto& meta = jsonResult["meta"];
            meta["timestamp"] = GetTimestamp(readOptions).GetValue();
            meta["yp_timestamp"] = GetYpTimestamp(readOptions);
        }

        FillJson<TReplicaObjects...>(jsonResult, *this, ListColumnFamilies().Success(), options);

        return jsonResult;
    }

    void PrintJson(const TFsPath& file, const TDescriptionOptions& options = TDescriptionOptions()) const {
        TFileOutput out(file);
        NJson::TJsonValue jsonStorage = GetJsonStorage(options);
        NJson::WriteJson(&out, &jsonStorage, true, true);
    }
};

} // namespace NYP::NYPReplica::NTesting
