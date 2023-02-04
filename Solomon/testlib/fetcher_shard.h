#pragma once

#include "db.h"

#include <solomon/services/fetcher/lib/fetcher_shard/simple_shard.h>

namespace NSolomon::NTesting {
    struct TMockShard: public NFetcher::TSimpleShard {
        using TSimpleShard::TSimpleShard;

        const NDb::NModel::TServiceConfig& ServiceConf() const {
            return *Service_;
        }

        const NDb::NModel::TShardConfig& ShardConf() const {
            return *Shard_;
        }

        const NDb::NModel::TClusterConfig& ClusterConf() const {
            return *Cluster_;
        }

        NDb::NModel::TServiceConfig& MutService() const {
            return const_cast<NDb::NModel::TServiceConfig&>(*Service_);
        }

        NDb::NModel::TClusterConfig& MutCluster() const {
            return const_cast<NDb::NModel::TClusterConfig&>(*Cluster_);
        }

        NDb::NModel::TShardConfig& MutShard() const {
            return const_cast<NDb::NModel::TShardConfig&>(*Shard_);
        }
    };

    inline TIntrusivePtr<TMockShard> CloneShard(const TMockShard& other) {
        return new TMockShard{
            MakeAtomicShared<const NDb::NModel::TShardConfig>(other.ShardConf()),
            MakeAtomicShared<const NDb::NModel::TClusterConfig>(other.ClusterConf()),
            MakeAtomicShared<const NDb::NModel::TServiceConfig>(other.ServiceConf()),
        };
    }

    struct TFetcherShardFactory: private TConfigProvider {
        TIntrusivePtr<TMockShard> MakeShard() {
            auto service = NewService();
            auto cluster = NewCluster();
            auto shard = NewShard(service, cluster);

            return new TMockShard(
                MakeAtomicShared<const NDb::NModel::TShardConfig>(shard),
                MakeAtomicShared<const NDb::NModel::TClusterConfig>(cluster),
                MakeAtomicShared<const NDb::NModel::TServiceConfig>(service)
            );
        }

    };
} // namespace NSolomon::NTesting
