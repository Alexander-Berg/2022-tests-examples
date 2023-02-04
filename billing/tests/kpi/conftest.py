# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import datetime as dt

import pytest
import mock

from butils.application import getApplication, Application

from rep.utils import balance_utils as ut
from rep import settings
from rep.core.kpi.calendar import Calendar
from rep.core.kpi.regulations import Regulations
from rep.core.kpi.users_manager import SliceCachingUsersManager

import utils as test_utils


@pytest.fixture(scope='session')
def app():
    # TODO: исключительно для логирования. Надо бы выковырять логирование и целиком app не создавать
    try:
        return getApplication()
    except RuntimeError:
        return Application(cfg_path=settings.app_cfg_path, database_id='lust-frick666s')


@pytest.fixture('session')
def calendar():
    cal_conf = ut.Struct(kpi_workday_start_hour=10, kpi_workday_end_hour=19)
    calendar_ = Calendar(None, cal_conf)

    def _update_cache(dt_):
        if dt_.year == 2017:
            calendar_._cache.update({
                dt.datetime(2017, 1, 1): (0, 0),
                dt.datetime(2017, 1, 2): (0, 0),
                dt.datetime(2017, 1, 3): (0, 0),
                dt.datetime(2017, 1, 4): (0, 0),
                dt.datetime(2017, 1, 5): (0, 0),
                dt.datetime(2017, 1, 6): (0, 0),
                dt.datetime(2017, 1, 7): (0, 0),
                dt.datetime(2017, 1, 8): (0, 0),
                dt.datetime(2017, 1, 9): (1, 1),
                dt.datetime(2017, 1, 10): (2, 1),
                dt.datetime(2017, 1, 11): (3, 1),
                dt.datetime(2017, 1, 12): (4, 1),
                dt.datetime(2017, 1, 13): (5, 1),
                dt.datetime(2017, 1, 14): (5, 0),
                dt.datetime(2017, 1, 15): (5, 0),
                dt.datetime(2017, 1, 16): (6, 1),
                dt.datetime(2017, 1, 17): (7, 1),
                dt.datetime(2017, 1, 18): (8, 1),
                dt.datetime(2017, 1, 19): (9, 1),
                dt.datetime(2017, 1, 20): (10, 1),
                dt.datetime(2017, 1, 21): (10, 0),
                dt.datetime(2017, 1, 22): (10, 0),
                dt.datetime(2017, 1, 23): (11, 1),
                dt.datetime(2017, 1, 24): (12, 1),
                dt.datetime(2017, 1, 25): (13, 1),
                dt.datetime(2017, 1, 26): (14, 1),
                dt.datetime(2017, 1, 27): (15, 1),
                dt.datetime(2017, 1, 28): (15, 0),
                dt.datetime(2017, 1, 29): (15, 0),
                dt.datetime(2017, 1, 30): (16, 1),
                dt.datetime(2017, 1, 31): (17, 1),
            })
        elif dt_.year == 2016:
            calendar_._cache.update({
                dt.datetime(2016, 12, 1, 0, 0): (226, 1),
                dt.datetime(2016, 12, 2, 0, 0): (227, 1),
                dt.datetime(2016, 12, 3, 0, 0): (227, 0),
                dt.datetime(2016, 12, 4, 0, 0): (227, 0),
                dt.datetime(2016, 12, 5, 0, 0): (228, 1),
                dt.datetime(2016, 12, 6, 0, 0): (229, 1),
                dt.datetime(2016, 12, 7, 0, 0): (230, 1),
                dt.datetime(2016, 12, 8, 0, 0): (231, 1),
                dt.datetime(2016, 12, 9, 0, 0): (232, 1),
                dt.datetime(2016, 12, 10, 0, 0): (232, 0),
                dt.datetime(2016, 12, 11, 0, 0): (232, 0),
                dt.datetime(2016, 12, 12, 0, 0): (233, 1),
                dt.datetime(2016, 12, 13, 0, 0): (234, 1),
                dt.datetime(2016, 12, 14, 0, 0): (235, 1),
                dt.datetime(2016, 12, 15, 0, 0): (236, 1),
                dt.datetime(2016, 12, 16, 0, 0): (237, 1),
                dt.datetime(2016, 12, 17, 0, 0): (237, 0),
                dt.datetime(2016, 12, 18, 0, 0): (237, 0),
                dt.datetime(2016, 12, 19, 0, 0): (238, 1),
                dt.datetime(2016, 12, 20, 0, 0): (239, 1),
                dt.datetime(2016, 12, 21, 0, 0): (240, 1),
                dt.datetime(2016, 12, 22, 0, 0): (241, 1),
                dt.datetime(2016, 12, 23, 0, 0): (242, 1),
                dt.datetime(2016, 12, 24, 0, 0): (242, 0),
                dt.datetime(2016, 12, 25, 0, 0): (242, 0),
                dt.datetime(2016, 12, 26, 0, 0): (243, 1),
                dt.datetime(2016, 12, 27, 0, 0): (244, 1),
                dt.datetime(2016, 12, 28, 0, 0): (245, 1),
                dt.datetime(2016, 12, 29, 0, 0): (246, 1),
                dt.datetime(2016, 12, 30, 0, 0): (247, 1),
                dt.datetime(2016, 12, 31, 0, 0): (247, 0)
            })

    patcher = mock.patch.object(calendar_, '_update_cache', _update_cache)
    patcher.start()

    return calendar_


@pytest.fixture('session')
def regulations():
    regulations_ = Regulations(mock.MagicMock())
    regulations_._cache = {
        (1, 1): None,
        (1, 2): 27,
        (1, 3): 1,
        (2, 1): None,
        (2, 2): 9,
        (2, 3): 1,
    }
    return regulations_


@pytest.fixture('session')
def users_manager():
    users_manager_ = SliceCachingUsersManager(None)

    users_manager_._cache_dts = [
        dt.datetime(2017, 1, 1),
        dt.datetime(2017, 2, 1),
        dt.datetime(2017, 3, 1),
    ]
    users_manager_._cache_logins = [
        {'pupkin', 'dupkin',           },
        {'pupkin', 'dupkin', 'shlupkin'},
        {'pupkin',           'shlupkin'}
    ]

    patcher = mock.patch.object(users_manager_, '_update_cache')
    patcher.start()

    return users_manager_


_tracked_components = [
    ut.Struct(id=1),
    ut.Struct(id=2),
]

@pytest.fixture('session', autouse=True)
def config(calendar, regulations, users_manager):
    items = {
        'KPI_IGNORED_ISSUE_TYPES': {'bad'},
        'KPI_IGNORED_ISSUE_PRIORITIES': {1},
        'KPI_OPEN_ISSUE_STATUSES': {"just_create", "new", "open", "inProgress"},
        'KPI_RESOLVED_ISSUE_STATUSES': {"closed", "resolved"},
        'KPI_FORCED_INTERNAL_USERS': {"autodasha", "robot-octopool"},
        'KPI_FORCED_ACTIVE_USERS': {"autodasha"},
        'KPI_RELEVANT_QUEUES': {'KEY'},
        'KPI_WORKDAY_START_HOUR': 10,
        'KPI_WORKDAY_END_HOUR': 19,
        'CALENDAR': calendar,
        'REGULATIONS': regulations,
        'USERS_MANAGER': users_manager,
        'TRACKED_COMPONENTS': _tracked_components,
    }
    return test_utils.TestConfig(items)

