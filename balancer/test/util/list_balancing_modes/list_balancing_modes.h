#pragma once

#include <balancer/modules/balancer/backends_factory.h>

inline TVector<TString> ListBalancingModes() {
    return NSrvKernel::CommonBackends()->ListHandleNames();
}
