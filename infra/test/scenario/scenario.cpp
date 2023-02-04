#include "scenario.h"

#include <infra/libs/logger/test_common.h>
#include <infra/libs/updatable_proto_config/accessor.h>
#include <infra/libs/updatable_proto_config/holder.h>
#include <infra/libs/updatable_proto_config/protos/config.pb.h>
#include <infra/libs/yp_replica/replica_objects.h>
#include <infra/libs/yp_replica/yp_replica.h>

#include <yp/yp_proto/yp/client/api/proto/data_model.pb.h>
#include <yp/cpp/yp/client.h>

#include <util/folder/path.h>
#include <util/stream/output.h>
#include <util/string/builder.h>
#include <util/system/tempfile.h>
#include <util/system/yassert.h>

using namespace NYP;
using namespace NYPReplica;

namespace {
    const TString STORAGE_PATH = "storage";
    const TString BACKUP_PATH = "backup";
    const TString LOG_FILE = "test_eventlog";

    static constexpr std::array<TStringBuf, 2> UPDATE_HISTOGRAMS = {{
        NSensors::COPY_UPDATES_TO_STORAGE_RESPONSE_TIME,
        NSensors::REQUEST_UPDATES_RESPONSE_TIME,
    }};

    static constexpr std::array<TStringBuf, 1> UPDATE_HISTOGRAMS_BY_REPLICA_OBJECTS = {{
        NSensors::SELECT_UPDATES_RESPONSE_TIME,
    }};

    static constexpr std::array<TStringBuf, 4> SELECT_ALL_HISTOGRAMS = {{
        NSensors::CHUNK_SELECTION_RESPONSE_TIME,
        NSensors::SELECT_ALL_OBJECTS_RESPONSE_TIME,
        NSensors::SELECT_ALL_OBJECTS_RESULT_SIZE,
        NSensors::SELECT_ALL_OBJECTS_CHUNK_SIZE,
    }};

    static constexpr std::array<TStringBuf, 2> WATCH_HISTOGRAMS = {{
        NSensors::SELECT_UPDATED_OBJECTS_RESPONSE_TIME,
        NSensors::WATCH_OBJECTS_RESPONSE_TIME,
    }};

    using namespace NUpdatableProtoConfig;

    void SetupYpMaster(NClient::TClientPtr client) {
        for (const TString endpointSetId : {"esid-1", "esid-2", "esid-3"}) {
            NClient::TEndpointSet endpointSet;
            endpointSet.MutableMeta()->set_id(endpointSetId);
            client->CreateObject(endpointSet).GetValueSync();
        }
        for (const TString endpointId : {"eid-1", "eid-2"}) {
            NClient::TEndpoint endpoint;
            endpoint.MutableMeta()->set_id(endpointId);
            endpoint.MutableMeta()->set_endpoint_set_id("esid-1");
            client->CreateObject(endpoint).GetValueSync();
        }
        for (const TString endpointId : {"eid-3"}) {
            NClient::TEndpoint endpoint;
            endpoint.MutableMeta()->set_id(endpointId);
            endpoint.MutableMeta()->set_endpoint_set_id("esid-2");
            client->CreateObject(endpoint).GetValueSync();
        }

        TVector<NClient::TCreateObjectRequest> createEsid3;
        for (size_t i = 0; i < 30; ++i) {
            NClient::TEndpoint endpoint;
            endpoint.MutableMeta()->set_id(TStringBuilder() << "endpoint-" << i);
            endpoint.MutableMeta()->set_endpoint_set_id("esid-3");
            createEsid3.emplace_back(endpoint);
        }
        client->CreateObjects(createEsid3).GetValueSync();
    }

    template <typename TConfig>
    TConfigHolder<TConfig> CreateConfigHolder(TConfig config, NInfra::TLoggerConfig loggerConfig) {
        return TConfigHolder<TConfig>(config, TWatchPatchConfig(), loggerConfig);
    }

    TYPReplicaConfig GetYPReplicaTestConfig(bool useWatches, TString backup = BACKUP_PATH, TString storage = STORAGE_PATH) {
        TYPReplicaConfig replicaConfig;
        replicaConfig.SetChunkSize(9);
        replicaConfig.MutableStorageConfig()->SetPath(backup);
        replicaConfig.MutableStorageConfig()->MutableValidationConfig()->SetMaxAge("1296000s");
        replicaConfig.MutableStorageConfig()->SetAgeAlertThreshold("1us");
        replicaConfig.MutableBackupConfig()->SetPath(storage);
        replicaConfig.MutableBackupConfig()->SetBackupFrequency("30s");
        replicaConfig.MutableBackupConfig()->SetBackupLogFiles(true);
        replicaConfig.MutableBackupConfig()->SetMaxBackupsNumber(10);
        if (useWatches) {
            replicaConfig.SetWatchObjectsEventCountLimit(100);
            replicaConfig.SetWatchObjectsTimeLimit("2s");
        }
        return replicaConfig;
    }

    TYPClusterConfig GetYPClusterTestConfig(bool useWatches, const NYP::NClient::TClientOptions& clientOptions) {
        TYPClusterConfig clusterConfig;
        if (!useWatches) {
            clusterConfig.SetGetUpdatesMode(SELECT_ALL);
        } else {
            clusterConfig.SetGetUpdatesMode(WATCH_UPDATES);
        }
        clusterConfig.SetAddress(clientOptions.Address());
        clusterConfig.SetEnableSsl(clientOptions.EnableSsl());
        clusterConfig.SetUpdatingFrequency("1s");
        clusterConfig.SetTimeout("2s");
        return clusterConfig;
    }

    ui64 SumHistogramRecords(TIntrusivePtr<NInfra::THistogramRateSensor> histogram) {
        const NMonitoring::IHistogramSnapshotPtr snapshot = histogram->TakeSnapshot();
        ui64 records = 0;
        for (size_t bucket = 0; bucket < snapshot->Count(); ++bucket) {
            records += snapshot->Value(bucket);
        }
        return records;
    }
}

void SimpleTest(const TString& address, bool useBackup, bool useWatches) {
    NYP::NClient::TClientOptions clientOptions;
    clientOptions
        .SetAddress(useBackup ? "bad-address:13377" : address)
        .SetMaxReceiveMessageSize(256000000)
        .SetEnableSsl(false);

    if (!useBackup) {
        auto client = NClient::CreateClient(clientOptions);
        SetupYpMaster(client);
    }

    TTempFile tmp(LOG_FILE);

    NInfra::TLoggerConfig loggerConfig(NInfra::NTestCommon::CreateLoggerConfig(TStringBuf("DEBUG"), LOG_FILE));
    TConfigHolder<TYPReplicaConfig> replicaConfigHolder = CreateConfigHolder(GetYPReplicaTestConfig(useWatches), loggerConfig);
    TConfigHolder<TYPClusterConfig> clusterConfigHolder = CreateConfigHolder(GetYPClusterTestConfig(useWatches, clientOptions), loggerConfig);

    TTableRulesHolder<TEndpointReplicaObject, TEndpointSetReplicaObject> rulesHolder;
    NInfra::TLogger logger{NInfra::NTestCommon::CreateLoggerConfig(TStringBuf("DEBUG"), LOG_FILE)};
    TYPReplica<TEndpointReplicaObject, TEndpointSetReplicaObject> replica(replicaConfigHolder.Accessor(), clusterConfigHolder.Accessor(), std::move(rulesHolder), /* token */ "", logger, 1);
    replica.Start();

    {
        const auto esid1 = *replica.GetByKey<TEndpointSetReplicaObject>("esid-1");
        Y_ENSURE(esid1.Objects.size() == 1);
        Y_ENSURE(esid1.Objects.front().Meta().id() == TStringBuf("esid-1"));
    }
    {
        const auto esid1 = *replica.GetByKey<TEndpointReplicaObject>("esid-1");
        Y_ENSURE(esid1.Objects.size() == 2);
        Y_ENSURE(esid1.Objects.front().Meta().id() == TStringBuf("eid-1"));
        Y_ENSURE(esid1.Objects.back().Meta().id() == TStringBuf("eid-2"));
    }

    {
        const auto esid2 = *replica.GetByKey<TEndpointSetReplicaObject>("esid-2");
        Y_ENSURE(esid2.Objects.size() == 1);
        Y_ENSURE(esid2.Objects.front().Meta().id() == TStringBuf("esid-2"));
    }
    {
        const auto esid2 = *replica.GetByKey<TEndpointReplicaObject>("esid-2");
        Y_ENSURE(esid2.Objects.size() == 1);
        Y_ENSURE(esid2.Objects.front().Meta().id() == TStringBuf("eid-3"));
    }

    {
        const auto esidUnknown = replica.GetByKey<TEndpointSetReplicaObject>("esid-unknown");
        Y_ENSURE(!esidUnknown.Defined());
    }
    {
        const auto esidUnknown = replica.GetByKey<TEndpointReplicaObject>("esid-unknown");
        Y_ENSURE(!esidUnknown.Defined());
    }

    {
        const auto esid3 = *replica.GetByKey<TEndpointSetReplicaObject>("esid-3");
        Y_ENSURE(esid3.Objects.size() == 1);
        Y_ENSURE(esid3.Objects.front().Meta().id() == TStringBuf("esid-3"));
    }
    {
        THashSet<TString> esid3set;
        const auto esid3 = *replica.GetByKey<TEndpointReplicaObject>("esid-3");
        for (const auto& object : esid3.Objects) {
            esid3set.insert(object.Meta().id());
        }
        Y_ENSURE(esid3set.size() == 30);
        for (size_t i = 0; i < 30; ++i) {
            Y_ENSURE(esid3set.contains(TStringBuilder() << "endpoint-" << i));
        }
    }

    replica.Stop();
}

template <typename... TReplicaObjects>
class TTestReplica : public TYPReplica<TReplicaObjects...> {
public:
    TTestReplica(TAccessor<TYPReplicaConfig> replicaConfig, TAccessor<TYPClusterConfig> clusterConfig, TTableRulesHolder<TReplicaObjects...>&& tableRulesHolder, const TString& ypToken, NInfra::TLogger& logger, ui64 backupFormatVersion)
        : TYPReplica<TReplicaObjects...>(std::move(replicaConfig), std::move(clusterConfig), std::forward<TTableRulesHolder<TReplicaObjects...>>(tableRulesHolder), ypToken, logger, backupFormatVersion)
    {
    }

    bool RequestUpdates() {
        return TYPReplica<TReplicaObjects...>::RequestUpdates();
    }

    void TryUpdateUntilSuccess(const TDuration maxSleep) {
        TYPReplica<TReplicaObjects...>::TryUpdateUntilSuccess(maxSleep);
    }

    THashMap<TStringBuf, TIntrusivePtr<NInfra::THistogramRateSensor>>& GetHistograms() {
        return TYPReplica<TReplicaObjects...>::Histograms_;
    }

    TMap<TString, THashMap<TStringBuf, TIntrusivePtr<NInfra::THistogramRateSensor>>>& GetHistogramsByColumnFamily() {
        return TYPReplica<TReplicaObjects...>::HistogramsByColumnFamily_;
    }
};

void HistogramsTest(const TString& address, bool useWatches) {
    NYP::NClient::TClientOptions clientOptions;
    clientOptions
        .SetAddress(address)
        .SetMaxReceiveMessageSize(256000000)
        .SetEnableSsl(false);

    TTempFile tmp(LOG_FILE);

    NInfra::TLoggerConfig loggerConfig(NInfra::NTestCommon::CreateLoggerConfig(TStringBuf("DEBUG"), LOG_FILE));
    TConfigHolder<TYPReplicaConfig> replicaConfigHolder = CreateConfigHolder(GetYPReplicaTestConfig(useWatches), loggerConfig);
    TConfigHolder<TYPClusterConfig> clusterConfigHolder = CreateConfigHolder(GetYPClusterTestConfig(useWatches, clientOptions), loggerConfig);

    TTableRulesHolder<TEndpointReplicaObject, TEndpointSetReplicaObject> rulesHolder;
    NInfra::TLogger logger{NInfra::NTestCommon::CreateLoggerConfig(TStringBuf("DEBUG"), LOG_FILE)};
    TTestReplica<TEndpointReplicaObject, TEndpointSetReplicaObject> replica(replicaConfigHolder.Accessor(), clusterConfigHolder.Accessor(), std::move(rulesHolder), "", logger, 1);
    replica.Start();

    const ui32 attemptCount = 64;
    for (ui32 i = 0; i < attemptCount; ++i) {
        replica.RequestUpdates();
        Sleep(TDuration::Seconds(1));

        replica.TryUpdateUntilSuccess(TDuration::Seconds(10));
        Sleep(TDuration::Seconds(1));
    }

    const auto& histograms = replica.GetHistograms();
    const auto& histogramsByColumnFamily = replica.GetHistogramsByColumnFamily();

    rulesHolder = TTableRulesHolder<TEndpointReplicaObject, TEndpointSetReplicaObject>();
    TTableManagers<TEndpointReplicaObject, TEndpointSetReplicaObject> tableManagers(std::move(rulesHolder));
    const TVector<TString>& columnFamilies = {{
        tableManagers.template GetMainColumnFamilyName<TEndpointReplicaObject>(),
        tableManagers.template GetMainColumnFamilyName<TEndpointSetReplicaObject>(),
    }};

    for (const TStringBuf histogramName : UPDATE_HISTOGRAMS) {
        ui64 records = SumHistogramRecords(histograms.at(histogramName));
        Y_ENSURE(records >= attemptCount);
    }

    for (const TStringBuf histogramName : UPDATE_HISTOGRAMS_BY_REPLICA_OBJECTS) {
        for (const TString& cf : columnFamilies) {
            ui64 records = SumHistogramRecords(histogramsByColumnFamily.at(cf).at(histogramName));
            Y_ENSURE(records >= attemptCount);
        }
    }
    if (useWatches) {
        for (const TStringBuf histogramName : WATCH_HISTOGRAMS) {
            for (const TString& cf : columnFamilies) {
                ui64 records = SumHistogramRecords(histogramsByColumnFamily.at(cf).at(histogramName));
                Y_ENSURE(records >= attemptCount);
            }
        }
    } else {
        for (const TStringBuf histogramName : SELECT_ALL_HISTOGRAMS) {
            for (const TString& cf : columnFamilies) {
                ui64 records = SumHistogramRecords(histogramsByColumnFamily.at(cf).at(histogramName));
                Y_ENSURE(records >= attemptCount);
            }
        }
        for (const TStringBuf histogramName : WATCH_HISTOGRAMS) {
            for (const TString& cf : columnFamilies) {
                ui64 records = SumHistogramRecords(histogramsByColumnFamily.at(cf).at(histogramName));
                Y_ENSURE(records == 0);
            }
        }
    }

    replica.Stop();
}

void CreateFqdnsRequest(NClient::TClientPtr client, const TVector<TString>& fqdns, THashSet<TString>& createdFqdns, int siftRatio = 1) {
    const TRetryOptions retryOptions = TRetryOptions()
        .WithCount(3)
        .WithSleep(TDuration::Seconds(1))
        .WithIncrement(TDuration::MilliSeconds(250));

    TVector<NClient::TCreateObjectRequest> createRequests;
    for (const TString& fqdn : fqdns) {
        if (!RandomNumber<ui64>(siftRatio)) {
            NClient::TDnsRecordSet recordSet;
            recordSet.MutableMeta()->set_id(fqdn);
            createRequests.push_back(recordSet);
            createdFqdns.insert(fqdn);
        }
    }
    DoWithRetry<NYP::NClient::TResponseError>(
        [&client, &createRequests] {
            client->CreateObjects(createRequests).GetValueSync();
        },
        retryOptions,
        /* throwLast */ false
    );
}

void UpdateFqdnsRequest(NClient::TClientPtr client, const TVector<TString>& fqdns, THashSet<TString>& updatedFqdns, int siftRatio = 1, TString updateFlag = "upd", TString changelist = "aboba") {
    const TRetryOptions retryOptions = TRetryOptions()
        .WithCount(3)
        .WithSleep(TDuration::Seconds(1))
        .WithIncrement(TDuration::MilliSeconds(250));

    TVector<NClient::TUpdateRequest> updateRequests;
    for (const TString& fqdn : fqdns) {
        if (!RandomNumber<ui64>(siftRatio)) {
            updateRequests.push_back(NYP::NClient::TUpdateRequest(
                NYP::NClient::NApi::NProto::OT_DNS_RECORD_SET, fqdn,
                {NClient::TSetRequest("/labels/update", updateFlag), NClient::TSetRequest("/labels/changelist", changelist)},
                {}
            ));
            updatedFqdns.insert(fqdn);
        }
    }
    DoWithRetry<NYP::NClient::TResponseError>(
        [&client, &updateRequests] {
            client->UpdateObjects(updateRequests).GetValueSync();
        },
        retryOptions,
        /* throwLast */ false
    );
}

void RemoveFqdnsRequest(NClient::TClientPtr client, const TVector<TString>& fqdns, THashSet<TString>& removedFqdns, int siftRatio = 1) {
    const TRetryOptions retryOptions = TRetryOptions()
        .WithCount(3)
        .WithSleep(TDuration::Seconds(1))
        .WithIncrement(TDuration::MilliSeconds(250));

    TVector<NClient::TRemoveObjectRequest> removeRequests;
    for (const TString& fqdn : fqdns) {
        if (!removedFqdns.contains(fqdn) && !RandomNumber<ui64>(siftRatio)) {
            removeRequests.push_back(NClient::TRemoveObjectRequest(NYP::NClient::NApi::NProto::OT_DNS_RECORD_SET, fqdn));
            removedFqdns.insert(fqdn);
        }
    }
    DoWithRetry<NYP::NClient::TResponseError>(
        [&client, &removeRequests] {
            client->RemoveObjects(removeRequests).GetValueSync();
        },
        retryOptions,
        /* throwLast */ false
    );
}

template<typename TReplicaObject>
void SyncReplica(NClient::TClientPtr client, TTestReplica<TReplicaObject>& replica) {
    const TRetryOptions retryOptions = TRetryOptions()
        .WithCount(3)
        .WithSleep(TDuration::Seconds(1))
        .WithIncrement(TDuration::MilliSeconds(250));

    DoWithRetry<NYP::NClient::TResponseError>(
        [&client, &replica] {
            ui64 currentTimestamp = client->GenerateTimestamp().GetValueSync();
            while (currentTimestamp > *replica.GetYpTimestamp()) {
                Sleep(TDuration::MilliSeconds(250));
            }
        },
        retryOptions,
        /* throwLast */ false
    );
}

class TTrackingRuleCallback : public ITableRuleCallback<TDnsRecordSetReplicaObject> {
public:
    TTrackingRuleCallback(THashMap<TString, TVector<TStorageElement<TDnsRecordSetReplicaObject>>>& data)
        : Data_(data)
    {
    }

    void Do(const TString& key, const TVector<TStorageElementRef<TDnsRecordSetReplicaObject>>& newValue) override {
        Data_[key] = TVector<TStorageElement<TDnsRecordSetReplicaObject>>(newValue.begin(), newValue.end());
    }

private:
    THashMap<TString, TVector<TStorageElement<TDnsRecordSetReplicaObject>>>& Data_;
};

class TFqdnDynamicRule final : public ITableRule<TDnsRecordSetReplicaObject> {
public:
    constexpr TStringBuf GetID() const override {
        return ID;
    }

    constexpr bool IsStable() const override {
        return false;
    }

    TString GetKey(const TDnsRecordSetReplicaObject& object) const override {
        return TString(TStringBuf(object.GetKey()).Head(2));
    }

    bool Filter(const TDnsRecordSetReplicaObject& object) const override {
        return object.GetKey().find("aboba") != TString::npos;
    }

    TString GetTableName(const TDnsRecordSetReplicaObject& object) const override {
        return TString(TStringBuf(object.GetKey()).Head(1));
    }

    constexpr static TStringBuf ID = "FQDN_ABOBA";
};

class TUpdateDynamicRule final : public ITableRule<TDnsRecordSetReplicaObject> {
public:
    constexpr TStringBuf GetID() const override {
        return ID;
    }

    constexpr bool IsStable() const override {
        return false;
    }

    TString GetKey(const TDnsRecordSetReplicaObject& object) const override {
        return object.GetKey();
    }

    TString GetTableName(const TDnsRecordSetReplicaObject& object) const override {
        NJson::TJsonValue changelist;
        object.GetObject().Labels().GetValueByPath("changelist", changelist);
        return changelist.IsString() && changelist.GetString() == "aboba" ? "second" : "first";
    }

    constexpr static TStringBuf ID = "UPDATE";
};

THashMap<TString, TVector<TString>> GetFqdnsData(const THashMap<TString, TVector<TStorageElement<TDnsRecordSetReplicaObject>>>& Data) {
    THashMap<TString, TVector<TString>> fqdnsData;
    for (const auto& [key, storageElements] : Data) {
        for (const auto& storageElement : storageElements) {
            fqdnsData[key].push_back(storageElement.ReplicaObject.GetKey());
        }
    }
    return fqdnsData;
}

THashMap<TString, TVector<TString>> GetFqdnsData(const TVector<std::pair<TString, TVector<TStorageElement<TDnsRecordSetReplicaObject>>>>& Data) {
    THashMap<TString, TVector<TString>> fqdnsData;
    for (const auto& [key, storageElements] : Data) {
        for (const auto& storageElement : storageElements) {
            fqdnsData[key].push_back(storageElement.ReplicaObject.GetKey());
        }
    }
    return fqdnsData;
}

static TString UPDATE_SEPARATOR = "aboba";

void RulesTest(const TString& address, bool useWatches) {
    NYP::NClient::TClientOptions clientOptions;
    clientOptions
        .SetAddress(address)
        .SetMaxReceiveMessageSize(256000000)
        .SetEnableSsl(false);

    TTempFile tmp(LOG_FILE);

    auto client = NClient::CreateClient(clientOptions);

    TString backupPath = "RulesTest/backup/DNS";
    TString storagePath = "RulesTest/storage/DNS";
    if (useWatches) {
        backupPath += "_useWatches";
        storagePath += "_useWatches";
    }

    NInfra::TLoggerConfig loggerConfig(NInfra::NTestCommon::CreateLoggerConfig(TStringBuf("DEBUG"), LOG_FILE));
    TConfigHolder<TYPReplicaConfig> replicaConfigHolder = CreateConfigHolder(GetYPReplicaTestConfig(useWatches, backupPath, storagePath), loggerConfig);
    TConfigHolder<TYPClusterConfig> clusterConfigHolder = CreateConfigHolder(GetYPClusterTestConfig(useWatches, clientOptions), loggerConfig);
    NInfra::TLogger logger{NInfra::NTestCommon::CreateLoggerConfig(TStringBuf("DEBUG"), LOG_FILE)};
    THashMap<TString, TVector<TStorageElement<TDnsRecordSetReplicaObject>>> fqdnRuleCallbackData;
    TTableRulePtr<TDnsRecordSetReplicaObject> fqdnRule = MakeHolder<TFqdnDynamicRule>();
    fqdnRule->SetCallback(MakeHolder<TTrackingRuleCallback>(fqdnRuleCallbackData));
    TTableRulesHolder<TDnsRecordSetReplicaObject> rulesHolder;
    rulesHolder.AddRule<TDnsRecordSetReplicaObject>(std::move(fqdnRule));
    rulesHolder.AddRule<TDnsRecordSetReplicaObject>(MakeHolder<TUpdateDynamicRule>());
    TTestReplica<TDnsRecordSetReplicaObject> replica(replicaConfigHolder.Accessor(), clusterConfigHolder.Accessor(), std::move(rulesHolder), "", logger, 1);

    THashMap<TString, TVector<TString>> expectedCallbackData;
    THashMap<TString, THashMap<TString, TVector<TString>>> expectedUpdateRuleTables;

    auto createRequest = [&](const TVector<TString>& initialFqdns, bool check = true) {
        TVector<TString> fqdns;
        for (const TString& prefixFqdn : initialFqdns) {
            for (const TString suffixFqdn : {"aboba", "bobi"}) {
                TString fqdn = JoinSeq(".", {prefixFqdn, suffixFqdn});
                if (fqdn.find("aboba") != TString::npos) {
                    expectedCallbackData[TStringBuf(fqdn).Head(2)].push_back(fqdn);
                }
                expectedUpdateRuleTables["first"][fqdn] = {fqdn};
                fqdns.push_back(fqdn);
            }
        }

        THashSet<TString> createdFqdns;
        CreateFqdnsRequest(client, fqdns, createdFqdns);
        if (check) {
            SyncReplica(client, replica);
            Y_ENSURE(expectedCallbackData == GetFqdnsData(fqdnRuleCallbackData));
        }
    };

    auto updateRequest = [&](const TVector<TString>& initialFqdns, TString changelist = "aboba") {
        TVector<TString> fqdns;
        for (const TString& prefixFqdn : initialFqdns) {
            for (const TString& suffixFqdn : {"aboba", "bobi"}) {
                TString fqdn = JoinSeq(".", {prefixFqdn, suffixFqdn});
                if (changelist == "aboba") {
                    expectedUpdateRuleTables["first"].erase(fqdn);
                    expectedUpdateRuleTables["second"][fqdn] = {fqdn};
                } else {
                    expectedUpdateRuleTables["second"].erase(fqdn);
                    expectedUpdateRuleTables["first"][fqdn] = {fqdn};
                }
                fqdns.push_back(fqdn);
            }
        }

        THashSet<TString> updatedFqdns;
        UpdateFqdnsRequest(client, fqdns, updatedFqdns, 1, "upd", changelist);
        SyncReplica(client, replica);
        Y_ENSURE(expectedCallbackData == GetFqdnsData(fqdnRuleCallbackData));
    };

    auto removeRequest = [&](const TVector<TString>& initialFqdns) {
        TVector<TString> fqdns;
        for (const TString& prefixFqdn : initialFqdns) {
            for (const TString suffixFqdn : {"aboba", "bobi"}) {
                TString fqdn = JoinSeq(".", {prefixFqdn, suffixFqdn});
                if (fqdn.find("aboba") != TString::npos) {
                    expectedCallbackData.erase(TStringBuf(fqdn).Head(2));
                }
                expectedUpdateRuleTables["first"].erase(fqdn);
                expectedUpdateRuleTables["second"].erase(fqdn);
                fqdns.push_back(fqdn);
            }
        }

        THashSet<TString> removedFqdns;
        RemoveFqdnsRequest(client, fqdns, removedFqdns);
        SyncReplica(client, replica);
        Y_ENSURE(expectedCallbackData == GetFqdnsData(fqdnRuleCallbackData));
    };

    auto getExpectedListElementsForFqdnRule = [&expectedCallbackData](const TString& tableName) {
        THashMap<TString, TVector<TString>> expected;
        for (const auto& [key, value] : expectedCallbackData) {
            if (TStringBuf(key).Head(1) == tableName) {
                expected[key] = value;
            }
        }
        return expected;
    };

    auto getExpectedListElementsForUpdateRule = [&expectedUpdateRuleTables](const TString& tableName) {
        return expectedUpdateRuleTables[tableName];
    };

    auto checkListElements = [&](bool fqdnRule, const TString& tableName, bool tableExists = true) {
        TYPReplica<TDnsRecordSetReplicaObject>::TListOptions<TDnsRecordSetReplicaObject> listOptions;
        listOptions.TableInfo = TTableInfo<TDnsRecordSetReplicaObject>(TString(fqdnRule ? TFqdnDynamicRule::ID : TUpdateDynamicRule::ID), tableName);

        Y_ENSURE(replica.ContainsTable(listOptions.TableInfo) == tableExists, tableName);

        if (tableExists) {
            THashMap<TString, TVector<TString>> expected = fqdnRule ? getExpectedListElementsForFqdnRule(tableName) : getExpectedListElementsForUpdateRule(tableName);
            Y_ENSURE(expected == GetFqdnsData(replica.ListElements(listOptions)));
        }
    };

    auto checkAllListElements = [&checkListElements](THashSet<TString> existingTables) {
        for (TString tableName : {"a", "b", "c"}) {
            checkListElements(/* fqdnRule */ true, tableName, existingTables.contains(tableName));
        }
        for (TString tableName : {"first", "second"}) {
            checkListElements(/* fqdnRule */ false, tableName, existingTables.contains(tableName));
        }
    };

    createRequest({"aa", "ab", "bb", "ba"}, /* check */ false);

    replica.Start();

    SyncReplica(client, replica);
    Y_ENSURE(expectedCallbackData == GetFqdnsData(fqdnRuleCallbackData));
    checkAllListElements(/* existingTables*/ {"a", "b", "first"});

    updateRequest({"ab", "ba"});
    checkAllListElements(/* existingTables*/ {"a", "b", "first", "second"});
    updateRequest({"ab", "aa"}, "bobi");
    checkAllListElements(/* existingTables*/ {"a", "b", "first", "second"});

    removeRequest({"bb", "ba"});
    checkAllListElements(/* existingTables*/ {"a", "first"});

    removeRequest({"aa", "ab"});

    createRequest({"aa", "ab", "bb", "ba", "ca", "cb"});
    checkAllListElements(/* existingTables*/ {"a", "b", "c", "first"});
    
    updateRequest({"ab", "ba", "cb"});
    checkAllListElements(/* existingTables*/ {"a", "b", "c", "first", "second"});
    updateRequest({"aa", "ba", "cb", "ca"}, "bobi");
    checkAllListElements(/* existingTables*/ {"a", "b", "c", "first", "second"});

    removeRequest({"aa", "ab", "bb", "ba", "ca", "cb"});

    replica.Stop();
}

void FilterTest(const TString& address, TString replicaFilter, TString clusterFilter, std::function<bool(const TString&)> predicate, bool useWatches = true) {
    NYP::NClient::TClientOptions clientOptions;
    clientOptions
        .SetAddress(address)
        .SetMaxReceiveMessageSize(256000000)
        .SetEnableSsl(false);

    TTempFile tmp(LOG_FILE);

    auto client = NClient::CreateClient(clientOptions);

    auto addFqdnsToYP = [&client](const TVector<TString>& fqdns, THashSet<TString>& updatedFqdns, THashSet<TString>& removedFqdns) {
        THashSet<TString> createdFqdns;
        CreateFqdnsRequest(client, fqdns, createdFqdns);
        UpdateFqdnsRequest(client, fqdns, updatedFqdns, 3);
        RemoveFqdnsRequest(client, fqdns, removedFqdns, 3);
    };

    if (!replicaFilter.empty()) {
        replicaFilter = TString::Join("(", replicaFilter, ") or ([/labels/update] = \"upd\")");
    }
    if (!clusterFilter.empty()) {
        clusterFilter = TString::Join("(", clusterFilter, ") or ([/labels/update] = \"upd\")");
    }

    TVector<TString> fqdns;
    for (const TString prefixFqdn : {"abacaba", "aboba", "lmrh"}) {
        for (const TString leftSubstrFqdn : {"aaa", "bbb", "ccc", "ddd"}) {
            for (const TString rightSubstrFqdn : {"yandex", "yandex-team", "ya-am"}) {
                for (const TString suffixFqdn : {"ru", "com", "ug", "jp", "es"}) {
                    fqdns.push_back(JoinSeq(".", {prefixFqdn, leftSubstrFqdn, rightSubstrFqdn, suffixFqdn}));
                }
            }
        }
    }

    THashSet<TString> updatedFqdns;
    THashSet<TString> removedFqdns;

    NInfra::TLoggerConfig loggerConfig(NInfra::NTestCommon::CreateLoggerConfig(TStringBuf("DEBUG"), LOG_FILE));
    TYPReplicaConfig replicaConfig = GetYPReplicaTestConfig(useWatches, "FilterTest/backup/DNS", "FilterTest/storage/DNS");
    if (!replicaFilter.empty()) {
        replicaConfig.SetFilter(replicaFilter);
    }
    TYPClusterConfig clusterConfig = GetYPClusterTestConfig(useWatches, clientOptions);
    if (!clusterFilter.empty()) {
        clusterConfig.SetFilter(clusterFilter);
    }
    TConfigHolder<TYPReplicaConfig> replicaConfigHolder = CreateConfigHolder(replicaConfig, loggerConfig);
    TConfigHolder<TYPClusterConfig> clusterConfigHolder = CreateConfigHolder(clusterConfig, loggerConfig);

    TTableRulesHolder<TDnsRecordSetReplicaObject> rulesHolder;
    NInfra::TLogger logger{NInfra::NTestCommon::CreateLoggerConfig(TStringBuf("DEBUG"), LOG_FILE)};
    TTestReplica<TDnsRecordSetReplicaObject> replica(replicaConfigHolder.Accessor(), clusterConfigHolder.Accessor(), std::move(rulesHolder), "", logger, 1);
    replica.Start();

    addFqdnsToYP(fqdns, updatedFqdns, removedFqdns);
    SyncReplica(client, replica);

    for (const TString& fqdn : fqdns) {
        bool fitCondition = !removedFqdns.contains(fqdn) && (predicate(fqdn) || updatedFqdns.contains(fqdn));
        Y_ENSURE(replica.GetByKey<TDnsRecordSetReplicaObject>(fqdn).Defined() == fitCondition);
    }

    if (useWatches) {
        TVector<TString> watchFqdns;
        for (const TString prefixFqdn : {"watch", "watch2"}) {
            for (const TString& fqdn : fqdns) {
                watchFqdns.push_back(JoinSeq(".", {prefixFqdn, fqdn}));
            }
        }

        addFqdnsToYP(watchFqdns, updatedFqdns, removedFqdns);
        SyncReplica(client, replica);

        for (const TString& fqdn : fqdns) {
            bool fitCondition = !removedFqdns.contains(fqdn) && (predicate(fqdn) || updatedFqdns.contains(fqdn));
            Y_ENSURE(replica.GetByKey<TDnsRecordSetReplicaObject>(fqdn).Defined() == fitCondition);
        }

        for (const TString& watchFqdn : watchFqdns) {
            bool fitCondition = !removedFqdns.contains(watchFqdn) && (predicate(watchFqdn) || updatedFqdns.contains(watchFqdn));
            Y_ENSURE(replica.GetByKey<TDnsRecordSetReplicaObject>(watchFqdn).Defined() == fitCondition);
        }

        RemoveFqdnsRequest(client, watchFqdns, removedFqdns);
    }

    RemoveFqdnsRequest(client, fqdns, removedFqdns);
    replica.Stop();
}

void RunTest(const TString& address) {
    SimpleTest(address, /* useBackup */ false, /* useWatches */ false);
    SimpleTest(address, /* useBackup */ true, /* useWatches */ false);
    SimpleTest(address, /* useBackup */ true, /* useWatches */ true);

    for (bool useWatches : {false, true}) {
        HistogramsTest(address, useWatches);
    }

    for (bool useWatches : {false, true}) {
        RulesTest(address, useWatches);
    }

    FilterTest(address, "is_substr(\"m.j\", [/meta/id]) = %true", "is_substr(\"h.a\", [/meta/id]) = %true",
               [](const TString& s) { return s.find("h.a") != TString::npos; }, /* useWatches */ true);
    FilterTest(address, "is_substr(\"m.j\", [/meta/id]) = %true", "",
               [](const TString& s) { return s.find("m.j") != TString::npos; }, /* useWatches */ true);
}
