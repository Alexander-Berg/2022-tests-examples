#pragma once

#include <solomon/libs/cpp/conf_db/db.h>
#include <solomon/libs/cpp/conf_db/model/agent_config.h>
#include <solomon/libs/cpp/conf_db/model/cluster_config.h>
#include <solomon/libs/cpp/conf_db/model/shard_config.h>
#include <solomon/libs/cpp/conf_db/model/service_config.h>
#include <solomon/libs/cpp/conf_db/ydb/ut_helpers/config_creators.h>
#include <solomon/libs/cpp/ydb/config/ydb_config.pb.h>

#include <ydb/public/sdk/cpp/client/ydb_table/table.h>
#include <ydb/public/sdk/cpp/client/ydb_driver/driver.h>

namespace NSolomon::NTesting {
    NDb::NModel::TServiceConfig MakeServiceConfig();
    NDb::NModel::TClusterConfig MakeClusterConfig();
    NDb::NModel::TShardConfig MakeShardConfig();
    NDb::NModel::TAgentConfig MakeAgentConfig();

    class TTestDb {
    public:
        static constexpr auto CLUSTER_TABLE_NAME = "Clusters";
        static constexpr auto SERVICE_TABLE_NAME = "Services";
        static constexpr auto AGENT_TABLE_NAME = "Agents";
        static constexpr auto SHARD_TABLE_NAME = "Shards";
        static constexpr auto PROVIDER_TABLE_NAME = "ServiceProviders";

        TTestDb();

        void InitTables(
            const TVector<NDb::NModel::TServiceConfig>& services,
            const TVector<NDb::NModel::TClusterConfig>& clusters,
            const TVector<NDb::NModel::TShardConfig>& shards);

        void DropTables();

        void Reset();

        NDb::TYdbConfig Config() const;

        NDb::IDbConnectionPtr Connection() const;
        NDb::IShardConfigDaoPtr Shards() const;
        NDb::IClusterConfigDaoPtr Clusters() const;
        NDb::IServiceConfigDaoPtr Services() const;
        NDb::IAgentConfigDaoPtr Agents() const;
        NDb::IProviderConfigDaoPtr Providers() const;

        void MakeDirectory(const TString& path);

    private:
        NDb::TShardTables CreateShardTables();
        NDb::TShardTables ShardTables_;

    private:
        THolder<NMonitoring::TMetricRegistry> Registry_;
        std::optional<NYdb::TDriver> Driver_;
        TString Prefix_;
        NDb::TYdbConfig Config_;
        NSolomon::NDb::IDbConnectionPtr Conn_;
        NSolomon::NDb::IClusterConfigDaoPtr ClusterDao_;
        NSolomon::NDb::IShardConfigDaoPtr ShardDao_;
        NSolomon::NDb::IServiceConfigDaoPtr ServiceDao_;
        NSolomon::NDb::IAgentConfigDaoPtr AgentsDao_;
        NSolomon::NDb::IProviderConfigDaoPtr ProvidersDao_;
    };


    class TConfigProvider {
    public:
        NDb::NModel::TServiceConfig& NewService();
        NDb::NModel::TClusterConfig& NewCluster();

        NDb::NModel::TShardConfig& NewShard(
            const NDb::NModel::TServiceConfig& service,
            const NDb::NModel::TClusterConfig& cluster,
            bool local = true);

    protected:
        TVector<NDb::NModel::TServiceConfig> Services_;
        TVector<NDb::NModel::TClusterConfig> Clusters_;
        TVector<NDb::NModel::TShardConfig> Shards_;
        TVector<ui32> LocalShardIds_;

    private:
        ui32 ShardId_ = 1;
    };
}
