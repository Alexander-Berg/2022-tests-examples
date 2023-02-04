#include "db.h"

#include <ydb/public/sdk/cpp/client/ydb_scheme/scheme.h>

#include <library/cpp/monlib/metrics/metric_registry.h>

#include <util/generic/guid.h>
#include <util/stream/file.h>
#include <util/string/builder.h>
#include <util/string/split.h>
#include <util/string/strip.h>

namespace NSolomon::NTesting {
    using namespace NSolomon::NDb;
    using namespace NSolomon::NDb::NModel;

    TShardTables TTestDb::CreateShardTables() {
        return {
            .ShardTablePath = Prefix_ + "/" + SHARD_TABLE_NAME,
            .NumPcsTablePath = Prefix_ + "/ShardPcsKey",
            .NumIdPcsTablePath = Prefix_ + "/ShardPcsNumId",
        };
    }

    IDbConnectionPtr TTestDb::Connection() const {
        return Conn_;
    }

    IShardConfigDaoPtr TTestDb::Shards() const {
        return ShardDao_;
    }

    IClusterConfigDaoPtr TTestDb::Clusters() const {
        return ClusterDao_;
    }

    IServiceConfigDaoPtr TTestDb::Services() const {
        return ServiceDao_;
    }

    IAgentConfigDaoPtr TTestDb::Agents() const {
        return AgentsDao_;
    }

    IProviderConfigDaoPtr TTestDb::Providers() const {
        return ProvidersDao_;
    }

    TTestDb::TTestDb() {
        auto endpoint = Strip(TFileInput("ydb_endpoint.txt").ReadAll());
        auto database = Strip(TFileInput("ydb_database.txt").ReadAll());

        Config_.SetAddress(endpoint);
        Config_.SetDatabase(database);

        Reset();
    }

    void TTestDb::InitTables(
        const TVector<NDb::NModel::TServiceConfig>& services,
        const TVector<NDb::NModel::TClusterConfig>& clusters,
        const TVector<NDb::NModel::TShardConfig>& shards) {

        ClusterDao_->CreateTable().GetValueSync();
        ShardDao_->CreateTable().GetValueSync();
        ServiceDao_->CreateTable().GetValueSync();
        AgentsDao_->CreateTable().GetValueSync();
        ProvidersDao_->CreateTable().GetValueSync();

        auto insertMany = [] (auto& items, auto dao) {
            for (auto&& item: items) {
                dao->Insert(item).GetValueSync();
            }
        };

        insertMany(services, ServiceDao_);
        insertMany(clusters, ClusterDao_);
        insertMany(shards, ShardDao_);
    }

    void TTestDb::DropTables() {
        ServiceDao_->DropTable().GetValueSync();
        ClusterDao_->DropTable().GetValueSync();
        ShardDao_->DropTable().GetValueSync();
        AgentsDao_->DropTable().GetValueSync();
        ProvidersDao_->DropTable().GetValueSync();
    }

    void TTestDb::Reset() {
        Registry_ = MakeHolder<NMonitoring::TMetricRegistry>();

        Prefix_ = TString{"/"} + Config_.GetDatabase() + "/" + CreateGuidAsString();
        ShardTables_ = CreateShardTables();

        Driver_ = NYdb::TDriver{
            NYdb::TDriverConfig{}
                .SetEndpoint(Config_.GetAddress())
                .SetDatabase(Config_.GetDatabase())
        };

        MakeDirectory(Prefix_);

        Conn_ = CreateYdbConnection(Config_, *Registry_);

        ClusterDao_ = Conn_->CreateClusterDao(Prefix_ + "/" + CLUSTER_TABLE_NAME);
        ShardDao_ = Conn_->CreateShardDao(ShardTables_);
        ServiceDao_ = Conn_->CreateServiceDao(Prefix_ + "/" + SERVICE_TABLE_NAME);
        AgentsDao_ = Conn_->CreateAgentDao(Prefix_ + "/" + AGENT_TABLE_NAME);
        ProvidersDao_ = Conn_->CreateProviderDao(Prefix_ + "/" + PROVIDER_TABLE_NAME);
    }

    TYdbConfig TTestDb::Config() const {
        return Config_;
    }

    void TTestDb::MakeDirectory(const TString& path) {
        auto scheme = NYdb::NScheme::TSchemeClient{*Driver_};
        auto status = scheme.MakeDirectory(path)
            .GetValueSync();

        Y_ENSURE(
            status.IsSuccess() || (status.GetStatus() == NYdb::EStatus::ALREADY_EXISTS),
            status.GetIssues().ToString()
        );
    }

    NModel::TServiceConfig& TConfigProvider::NewService() {
        NModel::TServiceConfig service = MakeServiceConfig();
        service.Id = TStringBuilder() << "service_id_" << Services_.size();
        service.Name = TStringBuilder() << "service_name_" << Services_.size();
        service.ShardSettings.PullOrPush = TPullSettings{
            .Port = 1337,
            .Path = "/something",
        };
        Services_.push_back(service);

        return Services_.back();
    }

    NDb::NModel::TClusterConfig& TConfigProvider::NewCluster() {
        NModel::TClusterConfig cluster = MakeClusterConfig();
        cluster.Id = TStringBuilder() << "cluster_id_" << Clusters_.size();
        cluster.Name = TStringBuilder() << "cluster_name_" << Clusters_.size();

        return Clusters_.emplace_back(std::move(cluster));
    }

    NModel::TShardConfig& TConfigProvider::NewShard(const NModel::TServiceConfig& service, const NModel::TClusterConfig& cluster, bool local) {
        NModel::TShardConfig shard = MakeShardConfig();;
        shard.NumId = ShardId_++; // NumId must be globally unique
        shard.Id = TStringBuilder() << "shard_id_" << Shards_.size();

        if (local) {
            LocalShardIds_.push_back(shard.NumId);
        }

        shard.ServiceId = service.Id;
        shard.ServiceName = service.Name;

        shard.ClusterName = cluster.Name;
        shard.ClusterId = cluster.Id;

        Shards_.push_back(shard);
        return Shards_.back();
    }
}
