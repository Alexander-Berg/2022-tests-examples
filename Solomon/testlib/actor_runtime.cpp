#include "actor_runtime.h"

#include <library/cpp/actors/dnsresolver/dnsresolver.h>
#include <library/cpp/actors/interconnect/interconnect.h>
#include <library/cpp/actors/interconnect/interconnect_tcp_server.h>

#include <library/cpp/testing/common/network.h>

#include <util/system/hostname.h>

using namespace NActors;
using namespace NActors::NDnsResolver;

namespace NSolomon::NTesting {
    void TCoordinationActorRuntime::InitIc() {
        IcTable_ = new TTableNameserverSetup;
        Ports_.reserve(GetNodeCount());
        for (ui32 nodeIndex = 0; nodeIndex < GetNodeCount(); ++nodeIndex) {
            Ports_.push_back(::NTesting::GetFreePort());
            IcTable_->StaticNodeTable[FirstNodeId + nodeIndex] =
                std::make_pair(TString{"::1"}, static_cast<ui16>(Ports_.back()));
            IcTable_->StaticNodeTable[FirstNodeId + nodeIndex].ResolveHost = FQDNHostName();

            NActorsInterconnect::TNodeLocation location;
            location.SetDataCenter(ToString(nodeIndex % DataCenterCount + 1));
            location.SetModule(ToString(nodeIndex + 1));
            location.SetRack(ToString(nodeIndex + 1));
            location.SetUnit(ToString(nodeIndex + 1));
            IcTable_->StaticNodeTable[FirstNodeId + nodeIndex].Location = TNodeLocation(location);
        }
    }

    void TCoordinationActorRuntime::InitNodeImpl(TNodeDataBase* node, size_t nodeIndex) {
        const auto dnsId = MakeDnsResolverActorId();
        const auto namesId = GetNameserviceActorId();
        node->Poller.Reset(new NInterconnect::TPollerThreads);
        node->Poller->Start();

        AddLocalService(
            dnsId,
            TActorSetupCmd{CreateOnDemandDnsResolver(), TMailboxType::ReadAsFilled, 0},
            nodeIndex
        );

        AddLocalService(
            namesId,
            TActorSetupCmd{CreateNameserverTable(IcTable_), TMailboxType::ReadAsFilled, 0},
            nodeIndex
        );

        const auto& nameNode = IcTable_->StaticNodeTable[FirstNodeId + nodeIndex];

        TIntrusivePtr<TInterconnectProxyCommon> common;
        common.Reset(new TInterconnectProxyCommon);
        common->NameserviceId = namesId;
        common->TechnicalSelfHostName = "::1";
        common->ClusterUUID = ClusterUUID;
        common->AcceptUUID = {ClusterUUID};

        AddLocalService(
            MakePollerActorId(),
            TActorSetupCmd{CreatePollerActor(), TMailboxType::Simple, 0},
            nodeIndex
        );

        auto listener = new TInterconnectListenerTCP(
                nameNode.first, nameNode.second, common
        );

        AddLocalService(
            {},
            TActorSetupCmd{listener, TMailboxType::Simple, InterconnectPoolId()},
            nodeIndex
        );

        TTestActorRuntime::InitNodeImpl(node, nodeIndex);
    }
} // namespace NSolomon::NTesting
