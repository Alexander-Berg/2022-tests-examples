#pragma once

#include <balancer/kernel/rpslimiter/quota.h>

#include <util/generic/maybe.h>

namespace NSrvKernel::NRpsLimiter {
    TMaybe<TPeerQuotas> ParsePeerQuotasPy(TString val);
}
