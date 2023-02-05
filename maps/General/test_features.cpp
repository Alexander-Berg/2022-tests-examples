#include "test_features.h"

namespace {

bool g_testFeatures = false;

} // namespace

void enableTestFeatures() { g_testFeatures = true; }

bool testFeaturesEnabled() { return g_testFeatures; }
