# coding: utf-8
__author__ = 'torvald'

import datetime
from decimal import Decimal as D

import pytest

from apikeys import apikeys_api
from apikeys import apikeys_defaults
from apikeys import apikeys_steps
from balance import balance_api as api
from balance import balance_db as db
from balance import balance_steps as steps
from btestlib import matchers as mtch
from btestlib.utils import aDict
from btestlib import utils


ADMIN = 1120000000011035
APIKEYS_SERVICE_ID = 129
PAYSYS_ID = 1001

MOBILE_SDK_SERVICE_CC = 'speechkitmobile'
CLOUD_SERVICE_CC = 'speechkitcloud'

shift = datetime.timedelta
BASE_DT = datetime.datetime.utcnow().replace(hour=5)

#TODO: move to typical_flows

def speechkit_typical_flow(scenario, free_passport, service_cc):
    oper_uid, login, token, service_id, key, client_id, person_id = apikeys_steps.prepare_key(free_passport, service_cc)
    counters = apikeys_steps.counter_initialization(scenario, token, key)

    # apikeys_steps.process_every_stat(scenario, counters, oper_uid)
    apikeys_steps.process_all_stats(scenario, counters, oper_uid)

    result = apikeys_api.TEST().run_limit_checker(key=key, project=None, service_id=service_id)
    # TODO: asserts for limit_checker results

    key_service_config = apikeys_api.TEST().mongo_find('key_service_config', {"key": key})
    # TODO: asserts for key_service_config
    # TODO: example: { "_id" : ObjectId("5784d801795be26ce1a8fe53"), "key" : "22e2c6b7-7ee7-4b39-ae00-cf36b3920a40", "service_id" : 19, "config" : { "banned" : true, "approved" : true, "inactive_reason_id" : 29, "ban_reason_id" : 30, "ban_dt" : ISODate("2016-07-12T11:44:07.256Z"), "ban_memo" : "[LIMIT] speechkitmobile_voice_unit_daily" }, "hidden" : false, "active" : true, "limit_inherits" : {  }, "scheduled_contract_data" : {  }, "unblockable" : false }

    orders = db.get_order_by_client(client_id)
    # TODO: asserts for entities in Balance

# ----------------------------------------------------------------------------------------------------------------------

# @pytest.mark.docs(u'--group', u'--pattern|Превышение:.*?]')
@pytest.mark.skip(reason='obsolete')
@pytest.mark.parametrize('service_id', [MOBILE_SDK_SERVICE_CC])
@pytest.mark.parametrize('scenario',
[
    # Test-case 0
    {'description': u'Control test: [>, >]'.encode('utf-8'),
                  'base_dt': BASE_DT,
                  'stats': [{'completions': {'voice_unit': 15204, 'tts_unit': 2000}, 'dt': BASE_DT - shift(hours=3)}],
                  'expected': D('3888.8')},
    ],
    ids=lambda x: x['description'])
def test_speechkit_mobile_basic(service_id, scenario, free_passport):
    speechkit_typical_flow(aDict(scenario), free_passport, service_id)