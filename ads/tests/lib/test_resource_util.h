#pragma once

#include <util/generic/vector.h>
#include <util/generic/string.h>
#include <util/generic/hash.h>

namespace NPyTorchTransportTests {
    TVector<TString> GetModelFeatureNames(const TString& modelName);
}
