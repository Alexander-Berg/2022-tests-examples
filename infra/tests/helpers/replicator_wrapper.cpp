#include "replicator_wrapper.h"

#include <infra/libs/controller/object_manager/object_manager.h>
#include <infra/yp_dns_api/replicator/zone_replicator/factory.h>

#include <library/cpp/proto_config/config.h>

namespace NInfra::NYpDnsApi::NReplicator::NTests {

TVector<NController::TObjectManagersFactoryPtr> CreateFactories(TReplicatorConfig config) {
    const size_t numberOfShards = config.GetShardsZonesDistribution().GetShardsConfig().GetNumberOfShards();
    NInfra::NController::TSharding shardsFactory(config.GetController().GetLeadingInvader(), {}, numberOfShards);

    return CreateZoneReplicatorsFactories(
        TVector<TZonesReplicationConfig>(config.GetZonesReplicationConfigs().begin(), config.GetZonesReplicationConfigs().end())
        , shardsFactory
        , config.GetShardsZonesDistribution().GetZonesCoverage()
    );
}

TReplicator::TReplicator(const TStringBuf configJson)
    : Config_(NProtoConfig::ParseConfigFromJson<TReplicatorConfig>(configJson))
    , Controller_(
        Config_.GetController(),
        CreateFactories(Config_)
    )
{
}

bool TReplicator::Sync() {
    return Controller_.SafeSync();
}

} // namespace NInfra::NYpDnsApi::NReplicator::NTests
