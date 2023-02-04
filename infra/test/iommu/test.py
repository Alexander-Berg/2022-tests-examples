import os


def test_check_iommu_enabled():
    groups = os.listdir("/sys/kernel/iommu_groups")
    assert groups
    for g in ["5", "6", "7", "8"]:
        assert g in groups
