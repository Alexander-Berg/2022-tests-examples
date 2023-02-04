#include "maps/b2bgeo/vm_scheduler/server/libs/aws/allocator/impl/select_instance_type.h"

#include <maps/b2bgeo/vm_scheduler/libs/common/include/slot.h>

#include <library/cpp/testing/gtest/gtest.h>


using namespace maps::b2bgeo::vm_scheduler;

TEST(InstanceTypeSelection, simple)
{
    const InstanceTypeInfos vmTypeInfos = {
        {
            .type = Aws::EC2::Model::InstanceType::c6g_medium,
            .capacity = {
                .cpu = 1_cores,
                .ram = 1024_MB,
            },
        },
        {
            .type = Aws::EC2::Model::InstanceType::c6g_large,
            .capacity = {
                .cpu = 2_cores,
                .ram = 2048_MB,
            },
        },
        {
            .type = Aws::EC2::Model::InstanceType::c6g_xlarge,
            .capacity = {
                .cpu = 4_cores,
                .ram = 2048_MB,
            },
        },
    };

    {
        const SlotCapacity slot = {
            .cpu = 1_cores,
            .ram = 1500_MB,
        };
        const auto selectedType = getInstanceTypeForSlot(vmTypeInfos, slot);

        EXPECT_EQ(selectedType, Aws::EC2::Model::InstanceType::c6g_large);
    }
    {
        const SlotCapacity slot = {
            .cpu = 1_cores,
            .ram = 1024_MB,
        };
        const auto selectedType = getInstanceTypeForSlot(vmTypeInfos, slot);

        EXPECT_EQ(selectedType, Aws::EC2::Model::InstanceType::c6g_medium);
    }
    {
        const SlotCapacity slot = {
            .cpu = 5_cores,
            .ram = 1500_MB,
        };
        const auto selectedType = getInstanceTypeForSlot(vmTypeInfos, slot);

        EXPECT_EQ(selectedType, Aws::EC2::Model::InstanceType::c6g_xlarge);
    }
}
