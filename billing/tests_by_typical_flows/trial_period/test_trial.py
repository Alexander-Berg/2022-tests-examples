# -*- coding: utf-8 -*-

from collections import namedtuple
from datetime import datetime, timedelta as shift
import pytest
from hamcrest import has_property
from apikeys.apikeys_defaults import WAITER_PARAMS as W
from apikeys import apikeys_steps
from apikeys import apikeys_api
from btestlib import utils

__author__ = 'kostya-karpus'

trial_service = namedtuple('service', ['service_cc', 'free_days_limit'])
maps = trial_service('apimaps', 14)
static_maps = trial_service('staticmaps', 14)
routingmatrix = trial_service('routingmatrix', 14)
speechkitjs = trial_service('speechkitjsapi', 30)
speechkitcloud = trial_service('speechkitcloud', 30)
pogoda = trial_service('pogoda', 30)


@pytest.mark.parametrize('service', [maps, static_maps, routingmatrix, speechkitjs, speechkitcloud, pogoda],
                         ids=lambda service: 'service: {}; days_limit: {}'.format(service.service_cc,
                                                                                  service.free_days_limit))
@pytest.mark.good
def test_trial_period(free_passport, service):
    _, _, token, _, keys, _, _ = apikeys_steps.prepare_several_keys(free_passport,
                                                                    service.service_cc, 1, 1)
    key = keys.linked[0]
    link_id = apikeys_steps.get_link_by_key(key)[0].get('link_id')
    for _ in xrange(2):  # двойной запуск тарификатора
        apikeys_api.TEST.run_tarifficator(link_id, datetime.utcnow() + shift(days=service.free_days_limit - 1))
    utils.wait_until2(apikeys_api.API.check_key, has_property('status_code', 200), timeout=W.time,
                      sleep_time=W.s_time)(token, key)
    for _ in xrange(2):  # двойной запуск тарификатора
        apikeys_api.TEST.run_tarifficator(link_id, datetime.utcnow() + shift(days=service.free_days_limit + 1))
    utils.wait_until2(apikeys_api.API.check_key, has_property('status_code', 403), timeout=W.time,
                      sleep_time=W.s_time)(token, key, allow_not_200=True)
