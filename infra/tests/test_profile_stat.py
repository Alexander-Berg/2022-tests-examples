"""Tests working with host profiling statistics."""

import pytest

from infra.walle.server.tests.lib.util import patch
from sepelib.mongo.mock import ObjectMocker
from walle import profile_stat
from walle.profile_stat import ProfileStat


@pytest.fixture
def test(database):
    return ObjectMocker(ProfileStat)


def test_sync(test):
    eine_profile_stat = {
        1: 1,
        2: 2,
        3: 3,
    }

    outdated_stat = test.mock(dict(inv=2, time=1))
    test.mock(dict(inv=1, time=1))  # up_to_date_stat
    test.mock(dict(inv=3, time=3), save=False)  # missing_stat
    test.mock(dict(inv=4, time=4))  # extra_stat

    with patch("walle.clients.eine.EineClient.get_profile_stat", return_value=eine_profile_stat):
        profile_stat.sync_profile_stat()
        outdated_stat.time = 2

    test.assert_equal()


def test_get(test):
    test.mock(dict(inv=1, time=1))
    test.mock(dict(inv=3, time=3))

    assert profile_stat.get_profile_time(2) is None

    test.mock(dict(inv=2, time=2))
    assert profile_stat.get_profile_time(2) == 2

    test.assert_equal()


def test_update(test):
    test.mock(dict(inv=1, time=1))
    test.mock(dict(inv=3, time=3))

    profile_stat.update_profile_time(2, 2)
    stat = test.mock(dict(inv=2, time=2), save=False)
    test.assert_equal()

    profile_stat.update_profile_time(2, 20)
    stat.time = 20
    test.assert_equal()
