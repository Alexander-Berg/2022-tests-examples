# coding: utf-8
from collections import namedtuple

import pytest
from datetime import datetime, timedelta as shift

from btestlib.utils import aDict
from apikeys.tests_by_typical_flows import typical_flows as flow


__author__ = 'kostya-karpus'

BASE_DT = datetime.utcnow().replace(hour=5)

APIMAPS_SERVICE = 'apimaps'

Tariff = namedtuple('Tariff', ['name', 'service_id',])
apimaps_tariffs = [
    Tariff('apikeys_apimaps_1000_yearprepay_2017', APIMAPS_SERVICE),]

#TODO: remove?
@pytest.mark.parametrize(
    'scenario', [
        # Test-case 0
        {'description': u'lazy_client'.encode('utf-8'),
         'base_dt': BASE_DT,
         'stats': [
             {'completions': {'shift_limit': -1}, 'dt': BASE_DT},
         ],
         'over_limit': False},
    ], ids=lambda x: x.get('description'))
@pytest.mark.parametrize(
    'tariff', apimaps_tariffs, ids=lambda x: '{}'.format(x.service_id))
def test_lazy_client(scenario, tariff, free_passport):
    scenario_copy = aDict(scenario)
    scenario_copy.tariff = tariff.name
    flow.Prepayment.pull_lazy_client(scenario_copy, free_passport, tariff.service_id)
