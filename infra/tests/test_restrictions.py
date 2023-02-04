"""Tests host restriction handling."""

import pytest

from walle import restrictions
from walle.restrictions import (
    REBOOT,
    PROFILE,
    REDEPLOY,
    AUTOMATED_DNS,
    AUTOMATED_REBOOT,
    AUTOMATED_PROFILE,
    AUTOMATED_REDEPLOY,
    AUTOMATED_HEALING,
    AUTOMATION,
    AUTOMATED_MEMORY_CHANGE,
    AUTOMATED_DISK_CHANGE,
    AUTOMATED_LINK_REPAIR,
    AUTOMATED_BMC_REPAIR,
    AUTOMATED_REPAIRING,
    AUTOMATED_CPU_REPAIR,
    AUTOMATED_OVERHEAT_REPAIR,
    AUTOMATED_CAPPING_REPAIR,
    AUTOMATED_RACK_REPAIR,
    AUTOMATED_GPU_REPAIR,
    AUTOMATED_DISK_CABLE_REPAIR,
    AUTOMATED_MEMORY_REPAIR,
    AUTOMATED_INFINIBAND_REPAIR,
    AUTOMATED_PROFILE_WITH_FULL_DISK_CLEANUP,
    EXCLUDE_FOR_MACS,
)
from walle.hosts import Host, HostType
from walle.util.misc import drop_none


def test_restriction_mapping():
    assert restrictions.RESTRICTION_MAPPING == {
        REBOOT: {
            PROFILE,
            REDEPLOY,
            AUTOMATED_REBOOT,
            AUTOMATED_MEMORY_CHANGE,
            AUTOMATED_PROFILE,
            AUTOMATED_REDEPLOY,
            AUTOMATED_GPU_REPAIR,
            AUTOMATED_CPU_REPAIR,
            AUTOMATED_OVERHEAT_REPAIR,
            AUTOMATED_BMC_REPAIR,
            AUTOMATED_CAPPING_REPAIR,
            AUTOMATED_MEMORY_REPAIR,
            AUTOMATED_PROFILE_WITH_FULL_DISK_CLEANUP,
        },
        PROFILE: {AUTOMATED_PROFILE, AUTOMATED_PROFILE_WITH_FULL_DISK_CLEANUP},
        REDEPLOY: {AUTOMATED_REDEPLOY},
        AUTOMATED_PROFILE: {AUTOMATED_PROFILE_WITH_FULL_DISK_CLEANUP},
        AUTOMATED_REBOOT: {
            AUTOMATED_PROFILE,
            AUTOMATED_REDEPLOY,
            AUTOMATED_MEMORY_CHANGE,
            AUTOMATED_GPU_REPAIR,
            AUTOMATED_CPU_REPAIR,
            AUTOMATED_CAPPING_REPAIR,
            AUTOMATED_MEMORY_REPAIR,
            AUTOMATED_OVERHEAT_REPAIR,
            AUTOMATED_BMC_REPAIR,
            AUTOMATED_PROFILE_WITH_FULL_DISK_CLEANUP,
        },
        AUTOMATED_HEALING: {
            AUTOMATED_REBOOT,
            AUTOMATED_PROFILE,
            AUTOMATED_REDEPLOY,
            AUTOMATED_REPAIRING,
            AUTOMATED_MEMORY_CHANGE,
            AUTOMATED_DISK_CHANGE,
            AUTOMATED_LINK_REPAIR,
            AUTOMATED_BMC_REPAIR,
            AUTOMATED_GPU_REPAIR,
            AUTOMATED_CPU_REPAIR,
            AUTOMATED_OVERHEAT_REPAIR,
            AUTOMATED_CAPPING_REPAIR,
            AUTOMATED_RACK_REPAIR,
            AUTOMATED_MEMORY_REPAIR,
            AUTOMATED_DISK_CABLE_REPAIR,
            AUTOMATED_INFINIBAND_REPAIR,
            AUTOMATED_PROFILE_WITH_FULL_DISK_CLEANUP,
        },
        AUTOMATED_REPAIRING: {
            AUTOMATED_MEMORY_CHANGE,
            AUTOMATED_DISK_CHANGE,
            AUTOMATED_LINK_REPAIR,
            AUTOMATED_BMC_REPAIR,
            AUTOMATED_GPU_REPAIR,
            AUTOMATED_CPU_REPAIR,
            AUTOMATED_OVERHEAT_REPAIR,
            AUTOMATED_MEMORY_REPAIR,
            AUTOMATED_CAPPING_REPAIR,
            AUTOMATED_RACK_REPAIR,
            AUTOMATED_DISK_CABLE_REPAIR,
            AUTOMATED_INFINIBAND_REPAIR,
        },
        AUTOMATION: {
            AUTOMATED_DNS,
            AUTOMATED_HEALING,
            AUTOMATED_REPAIRING,
            AUTOMATED_REBOOT,
            AUTOMATED_PROFILE,
            AUTOMATED_REDEPLOY,
            AUTOMATED_MEMORY_REPAIR,
            AUTOMATED_MEMORY_CHANGE,
            AUTOMATED_DISK_CHANGE,
            AUTOMATED_LINK_REPAIR,
            AUTOMATED_BMC_REPAIR,
            AUTOMATED_GPU_REPAIR,
            AUTOMATED_CPU_REPAIR,
            AUTOMATED_OVERHEAT_REPAIR,
            AUTOMATED_CAPPING_REPAIR,
            AUTOMATED_RACK_REPAIR,
            AUTOMATED_DISK_CABLE_REPAIR,
            AUTOMATED_INFINIBAND_REPAIR,
            AUTOMATED_PROFILE_WITH_FULL_DISK_CLEANUP,
        },
    }


def test_strip_empty_restrictions():
    assert restrictions.strip_restrictions([]) == []
    assert restrictions.strip_restrictions([], strip_to_none=True) is None


@pytest.mark.parametrize(
    "user_restrictions,result_restrictions",
    (
        ([AUTOMATION], [AUTOMATION]),
        ([AUTOMATED_DNS], [AUTOMATED_DNS]),
        ([AUTOMATED_HEALING], [AUTOMATED_HEALING]),
        ([AUTOMATED_REBOOT], [AUTOMATED_REBOOT]),
        ([AUTOMATED_PROFILE], [AUTOMATED_PROFILE]),
        ([AUTOMATED_REDEPLOY], [AUTOMATED_REDEPLOY]),
        ([AUTOMATED_MEMORY_CHANGE], [AUTOMATED_MEMORY_CHANGE]),
        ([AUTOMATED_DISK_CHANGE], [AUTOMATED_DISK_CHANGE]),
        ([AUTOMATED_LINK_REPAIR], [AUTOMATED_LINK_REPAIR]),
        ([AUTOMATED_BMC_REPAIR], [AUTOMATED_BMC_REPAIR]),
        ([AUTOMATED_OVERHEAT_REPAIR], [AUTOMATED_OVERHEAT_REPAIR]),
        ([AUTOMATED_CAPPING_REPAIR], [AUTOMATED_CAPPING_REPAIR]),
        ([AUTOMATED_REBOOT, AUTOMATED_REBOOT, AUTOMATED_PROFILE, AUTOMATED_REDEPLOY], [AUTOMATED_REBOOT]),
        ([AUTOMATED_REBOOT, AUTOMATED_PROFILE, PROFILE], [AUTOMATED_REBOOT, PROFILE]),
        ([AUTOMATED_REBOOT, AUTOMATED_REDEPLOY, REDEPLOY], [AUTOMATED_REBOOT, REDEPLOY]),
        ([AUTOMATED_REBOOT, AUTOMATED_REDEPLOY, REBOOT], [REBOOT]),
        ([AUTOMATION, AUTOMATED_REDEPLOY, REBOOT], [AUTOMATION, REBOOT]),
        (
            [AUTOMATED_REBOOT, AUTOMATED_MEMORY_CHANGE, AUTOMATED_DISK_CHANGE, AUTOMATED_LINK_REPAIR],
            [AUTOMATED_REBOOT, AUTOMATED_DISK_CHANGE, AUTOMATED_LINK_REPAIR],
        ),
        (
            [
                AUTOMATED_REPAIRING,
                AUTOMATED_MEMORY_CHANGE,
                AUTOMATED_DISK_CHANGE,
                AUTOMATED_LINK_REPAIR,
                AUTOMATED_BMC_REPAIR,
                AUTOMATED_OVERHEAT_REPAIR,
                AUTOMATED_CAPPING_REPAIR,
            ],
            [AUTOMATED_REPAIRING],
        ),
        (
            [
                AUTOMATED_REBOOT,
                AUTOMATED_REPAIRING,
                AUTOMATED_MEMORY_CHANGE,
                AUTOMATED_DISK_CHANGE,
                AUTOMATED_LINK_REPAIR,
                AUTOMATED_BMC_REPAIR,
                AUTOMATED_OVERHEAT_REPAIR,
                AUTOMATED_CAPPING_REPAIR,
            ],
            [AUTOMATED_REBOOT, AUTOMATED_REPAIRING],
        ),
        ([AUTOMATED_REPAIRING, AUTOMATED_HEALING], [AUTOMATED_HEALING]),
        ([AUTOMATION, AUTOMATED_REBOOT], [AUTOMATION]),
        ([AUTOMATION, AUTOMATED_PROFILE], [AUTOMATION]),
        ([AUTOMATION, AUTOMATED_DISK_CHANGE], [AUTOMATION]),
        ([AUTOMATION, AUTOMATED_REDEPLOY], [AUTOMATION]),
        (
            [
                AUTOMATED_DNS,
                AUTOMATED_HEALING,
                AUTOMATED_REBOOT,
                AUTOMATED_REDEPLOY,
                AUTOMATED_PROFILE,
                AUTOMATED_MEMORY_CHANGE,
                AUTOMATED_DISK_CHANGE,
                AUTOMATED_LINK_REPAIR,
                AUTOMATED_BMC_REPAIR,
                AUTOMATED_OVERHEAT_REPAIR,
                AUTOMATED_CAPPING_REPAIR,
            ],
            [AUTOMATED_DNS, AUTOMATED_HEALING],
        ),
        ([AUTOMATION, AUTOMATED_REPAIRING, AUTOMATED_HEALING], [AUTOMATION]),
        ([AUTOMATION, AUTOMATED_DNS, AUTOMATED_HEALING], [AUTOMATION]),
    ),
)
def test_strip_restrictions(user_restrictions, result_restrictions):
    assert restrictions.strip_restrictions(user_restrictions) == sorted(result_restrictions)


@pytest.mark.parametrize(
    "user_restrictions,result_restrictions",
    (
        ([], []),
        ([AUTOMATED_REBOOT], [AUTOMATED_REBOOT, REBOOT, AUTOMATED_HEALING, AUTOMATION]),
        ([AUTOMATED_PROFILE], [AUTOMATED_PROFILE, AUTOMATED_REBOOT, AUTOMATED_HEALING, AUTOMATION, REBOOT, PROFILE]),
        ([AUTOMATED_DISK_CHANGE], [AUTOMATED_DISK_CHANGE, AUTOMATED_REPAIRING, AUTOMATED_HEALING, AUTOMATION]),
        ([AUTOMATED_REDEPLOY], [AUTOMATED_REDEPLOY, AUTOMATED_REBOOT, AUTOMATED_HEALING, AUTOMATION, REBOOT, REDEPLOY]),
        ([AUTOMATION], [AUTOMATION]),
        ([AUTOMATED_DNS], [AUTOMATED_DNS, AUTOMATION]),
        ([AUTOMATED_REPAIRING], [AUTOMATED_REPAIRING, AUTOMATED_HEALING, AUTOMATION]),
        ([AUTOMATED_HEALING], [AUTOMATED_HEALING, AUTOMATION]),
        ([REBOOT], [REBOOT]),
        ([PROFILE], [PROFILE, REBOOT]),
        ([REDEPLOY], [REBOOT, REDEPLOY]),
    ),
)
def test_expand_restrictions(user_restrictions, result_restrictions):
    assert restrictions.expand_restrictions(user_restrictions) == sorted(result_restrictions)


@pytest.mark.parametrize(
    "host_restrictions,operation_restrictions,allowed",
    (
        ([REDEPLOY], [REDEPLOY], False),
        ([REDEPLOY], [AUTOMATED_REDEPLOY], False),
        ([REDEPLOY], [REBOOT], True),
        ([AUTOMATED_HEALING], [AUTOMATED_REBOOT], False),
        ([AUTOMATED_HEALING], [AUTOMATED_MEMORY_CHANGE], False),
        ([AUTOMATED_HEALING], [AUTOMATED_BMC_REPAIR], False),
        ([AUTOMATED_HEALING], [AUTOMATED_OVERHEAT_REPAIR], False),
        ([AUTOMATED_HEALING], [AUTOMATED_CAPPING_REPAIR], False),
        ([AUTOMATED_REBOOT], [AUTOMATED_MEMORY_CHANGE], False),
        ([AUTOMATED_REBOOT], [AUTOMATED_LINK_REPAIR], True),
        ([AUTOMATED_REPAIRING], [AUTOMATED_LINK_REPAIR], False),
        ([AUTOMATED_HEALING], [AUTOMATED_LINK_REPAIR], False),
        ([AUTOMATION], [AUTOMATED_LINK_REPAIR], False),
    ),
)
def test_check_restrictions_for_macs(mp, host_restrictions, operation_restrictions, allowed):
    host = Host(
        **drop_none(
            dict(
                type=HostType.MAC,
                restrictions=host_restrictions,
            )
        )
    )

    if allowed and not (set(operation_restrictions) - EXCLUDE_FOR_MACS):
        restrictions.check_restrictions(host, operation_restrictions)
    else:
        with pytest.raises(restrictions.OperationRestrictedError):
            restrictions.check_restrictions(host, operation_restrictions)
