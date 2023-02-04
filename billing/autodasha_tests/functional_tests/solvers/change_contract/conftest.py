# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import pytest

from autodasha.comments.change_contract import ChangeContractCommentsManager
from tests.autodasha_tests.common import staff_utils


def _get_settings():
    return staff_utils.create_approvers_settings([], [
        staff_utils.PersonSettings('ChangeContract', 'megaboss', 'bigboss1', 1),
        staff_utils.PersonSettings('ChangeContract', 'megaboss', 'bigboss2', 1),
        staff_utils.PersonSettings('ChangeContract', 'kommando', 'bigboss1', 1),
        staff_utils.PersonSettings('ChangeContract', 'kommando', 'bigboss2', 1),
        staff_utils.PersonSettings('ChangeContract', 'shitty_boss', 'forced_boss', 1),
    ])


@pytest.fixture
def comments_manager():
    return ChangeContractCommentsManager()


@pytest.fixture
def config(config):
    old_ad = config._items.get('common_approve_departments')
    old_as = config._items.get('approvers_settings')

    config._items['common_approve_departments'] = ['backoffice']
    config._items['approvers_settings'] = _get_settings()
    config._items['backoffice_departments'] = ['backoffice', 'backoffice666']
    config._items['ultimate_backoffice_chiefs'] = ['megaboss']
    config._items['backoffice_ua_approvers'] = {'pchelovik': [], 'bigboss1': ['bigboss2', 'megaboss']}
    config._items['backoffice_ua_taxi_approvers'] = ['medovik']
    config._items['check_outdated'] = 1
    config._items['preapproved_attrs'] = ['external_id']
    config._items['CHANGE_CONTRACT'] = {'off_calculating_pages': [100125, 100126]}
    config._items['CHANGECONTRACT_PROCESS_RULES'] = False

    yield config

    config._items['common_approve_departments'] = old_ad
    config._items['approvers_settings'] = old_as
