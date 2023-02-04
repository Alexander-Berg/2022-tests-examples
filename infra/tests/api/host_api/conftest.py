import itertools

import pytest

from infra.walle.server.tests.lib.util import TestCase
from walle.hosts import HostStatus

OTHER_USER = "other-user@"
ALLOWED_OWNERS = {TestCase.api_issuer, OTHER_USER}


def all_status_owner_combinations(include_invalid=False, include_manual=True):
    if include_invalid:
        all_statuses = set(HostStatus.ALL_STEADY) | {HostStatus.INVALID}
    else:
        all_statuses = set(HostStatus.ALL_STEADY)
    if not include_manual:
        all_statuses -= {HostStatus.MANUAL}

    # noinspection PyTypeChecker
    return pytest.mark.parametrize(
        ["status", "owner"],
        # generate cartesian product for all allowed status-owner values, excluding "maintenance"
        set(itertools.product(all_statuses, ALLOWED_OWNERS)) - {(HostStatus.MANUAL, OTHER_USER)},
    )


pytest.mark.all_status_owner_combinations = all_status_owner_combinations
