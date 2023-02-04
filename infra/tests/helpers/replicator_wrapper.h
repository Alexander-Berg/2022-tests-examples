#pragma once

#include <infra/yp_dns_api/replicator/config/config.pb.h>

#include <infra/libs/controller/standalone_controller/standalone_controller.h>

namespace NInfra::NYpDnsApi::NReplicator::NTests {

class TReplicator {
public:
    TReplicator(const TStringBuf configJson);

    bool Sync();

private:
    const TReplicatorConfig Config_;
    NController::TStandaloneController Controller_;
};

} // namespace NInfra::NYpDnsApi::NReplicator::NTests
