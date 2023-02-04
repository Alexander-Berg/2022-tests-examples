#pragma once

#include <solomon/libs/cpp/cluster_map/cluster.h>

#include <solomon/libs/cpp/clients/coremon/coremon_client.h>

#include <util/system/hostname.h>

namespace NSolomon::NTesting {
    struct TMockMapper: INodeMapper {
        ui32 GetId(TStringBuf) const override {
            return Idx_++;
        }

    private:
        mutable ui32 Idx_{0};
    };

    struct TMockCoremonClient: NCoremon::ICoremonClient {
        struct TCreateShard {
            TCreateShard(TString p, TString c, TString s, TString cb)
                : ProjectId{std::move(p)}
                , ServiceName{std::move(s)}
                , ClusterName{std::move(c)}
                , CreatedBy{std::move(cb)}
            {
            }

            bool operator==(const TCreateShard& other) const noexcept {
                return other.ProjectId == ProjectId
                    && other.ServiceName == ServiceName
                    && other.ClusterName == ClusterName
                    && other.CreatedBy == CreatedBy
                    ;
            }

            TString ProjectId;
            TString ServiceName;
            TString ClusterName;
            TString CreatedBy;
        };

        TMockCoremonClient(TClusterNode loc)
            : Location{std::move(loc)}
        {
        }

        TClusterNode Location;
        i32 AssignmentCalls{0};

        NCoremon::TAsyncDataProcessResponse ProcessPulledData(
            ui32, TString,
            const NMonitoring::TLabels&, NMonitoring::EFormat,
            TInstant, TString,
            TInstant, TString) noexcept override
        {
            return {};
        }

        void SetShardAssignments(NCoremon::TShardAssignments shardAssignments) {
            ShardAssignments_ = std::move(shardAssignments);
        }

        NCoremon::TAsyncShardAssignments GetShardAssignments() noexcept override {
            ++AssignmentCalls;
            return NThreading::MakeFuture(TErrorOr<NCoremon::TShardAssignments, TApiCallError>::FromValue(ShardAssignments_));
        }

        NCoremon::TAsyncDataProcessResponse ProcessPushedData(
                ui32,
                NMonitoring::EFormat,
                TInstant,
                TString) noexcept override
        {
            return {};
        }

        NCoremon::TAsyncShardCreated CreateShard(
                TString projectId,
                TString serviceName,
                TString clusterName,
                TString createdBy) noexcept override
        {
            CreateRequests.emplace_back(projectId, clusterName, serviceName, createdBy);
            return NThreading::MakeFuture(TErrorOr<NCoremon::TShardCreated, TApiCallError>::FromValue(NCoremon::TShardCreated{
                .Leader = FQDNHostName(),
                .AssignedToHost = FQDNHostName(),
                .ShardId = projectId + serviceName + clusterName,
                .NumId = 42,
            }));
        }

        /// Stop the client. This call blocks until all requests are done if wait is set to true
        void Stop(bool) override {
        }

        TVector<TCreateShard> CreateRequests;

    private:
        NCoremon::TShardAssignments ShardAssignments_;
    };

    struct TMockCoremonCluster: NCoremon::ICoremonClusterClient {
        explicit TMockCoremonCluster(const IClusterMapPtr& cluster) {
            for (auto&& loc: cluster->Nodes()) {
                Clients_.emplace(loc.Endpoint, std::make_unique<TMockCoremonClient>(loc));
                Addresses_.emplace_back(loc.Endpoint);
            }
        }

        NCoremon::ICoremonClient* Get(TStringBuf addr) noexcept override {
            if (auto it = Clients_.find(addr); it != Clients_.end()) {
                return it->second.get();
            }

            return nullptr;
        }

        NCoremon::ICoremonClient* GetAny() noexcept override {
            Y_VERIFY(!Clients_.empty());
            return Clients_.begin()->second.get();
        }

        TString GetAnyAddress() const noexcept override {
            Y_VERIFY(!Clients_.empty());
            return Clients_.begin()->first;
        }

        const std::vector<TString>& Addresses() const noexcept override {
            return Addresses_;
        }

        void Add(TStringBuf) override {
            ythrow yexception() << "not implemented";
        }

        void Stop(bool) override {
        }

    private:
        TStringMap<NCoremon::ICoremonClientPtr> Clients_;
        TVector<TString> Addresses_;
    };
} // namespace NSolomon::NTesting
