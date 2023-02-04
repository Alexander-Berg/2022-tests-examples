#include <library/cpp/testing/gtest/gtest.h>

#include <maps/libs/common/include/cpu_limit.h>

#include <util/system/env.h>

#include <stdlib.h>

namespace maps::tests {

TEST(test_cpu_limit, env_cpu_limit) {
    SetEnv("MAPS_CPU_CORES_LIMIT", "0.25");
    EXPECT_EQ(getCpuLimit(), 1u);
    SetEnv("MAPS_CPU_CORES_LIMIT", "2.25");
    EXPECT_EQ(getCpuLimit(), 2u);
}

} //namespace maps::tests
