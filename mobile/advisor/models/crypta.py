import ujson as json
from collections import defaultdict

import logging
import mongoengine as me
import requests
from datetime import datetime
from django.conf import settings
from requests.exceptions import RequestException

from yaphone.advisor.common.tvm import add_service_ticket_header

logger = logging.getLogger(__name__)

# https://wiki.yandex-team.ru/BannernajaKrutilka/BigBExamples/
LOYALTY_CRYPTA_ID = 'user-loyalty'
GENDER_CRYPTA_ID = 'krypta-user-gender'
AGE_CRYPTA_ID = 'krypta-user-age'
REVENUE_CRYPTA_ID = 'krypta-user-revenue'
CRYPTA_WEIGHT_MULTIPLIER = 1E-6

BIGB_TIMEOUT = 1


# noinspection PyClassHasNoInit
class CryptaInfo(me.EmbeddedDocument):
    # example values at web https://me.crypta.yandex.ru/by_yandexuid/1385074051444060330
    updated_at = me.DateTimeField(required=False)
    loyalty = me.FloatField(required=False, min_value=0.0, max_value=1.0)
    gender = me.FloatField(required=False, min_value=0.0, max_value=1.0)
    yuid = me.StringField(required=False)

    age = me.MapField(required=False, field=me.FloatField(min_value=0.0, max_value=1.0))
    # dict field holds float values for age ranges according docs:
    # https://wiki.yandex-team.ru/crypta/OutputToBB/#sushhestvujushhiekeywordidiznachenija
    # age structure example:
    # {
    #   '0': 0.00661, #       age < 18
    #   '1': 0.45291, # 18 <= age < 25
    #   '2': 0.49276, # 25 <= age < 35
    #   '3': 0.03383, # 35 <= age < 45
    #   '4': 0.01388, # 45 <= age
    # }

    revenue = me.MapField(required=False, field=me.FloatField(min_value=0.0, max_value=1.0))

    # dict field holds float values for revenue ranges according docs:
    # https://wiki.yandex-team.ru/crypta/OutputToBB/#sushhestvujushhiekeywordidiznachenija
    # revenue structure example:
    # {
    #   '0': 0.01185, # revenue TNS A
    #   '1': 0.10683, # revenue TNS B
    #   '2': 0.88132, # revenue TNS C
    # }

    def update(self, device_id, uuid, ad_id=None, passport_uid=None):
        data = _get_crypta_info(device_id, uuid, ad_id, passport_uid)
        if not data:
            return

        if AGE_CRYPTA_ID in data:
            self.age = data[AGE_CRYPTA_ID]
        if REVENUE_CRYPTA_ID in data:
            self.revenue = data[REVENUE_CRYPTA_ID]
        if LOYALTY_CRYPTA_ID in data:
            self.loyalty = data[LOYALTY_CRYPTA_ID]
        if GENDER_CRYPTA_ID in data:
            self.gender = data[GENDER_CRYPTA_ID]['0']
        self.updated_at = datetime.utcnow()


def _parse_bigb_data(data):
    root = data['data'][0]['segment']
    values = defaultdict(dict)  # type: float
    for block in root:
        key = block['name']
        value = block['value']
        if 'weight' in block:
            weight = int(block['weight']) * CRYPTA_WEIGHT_MULTIPLIER
            values[key][value] = weight
        else:
            values[key] = int(value) * CRYPTA_WEIGHT_MULTIPLIER
    return values


def _get_crypta_info(device_id, uuid, ad_id=None, passport_uid=None):
    params = {
        'operation': 6,
        'client': 'mobile_advisor',
        'device-id': device_id.hex,
        'uuid': uuid.hex,
    }
    if ad_id:
        params['google-ad-id'] = ad_id.hex
    if passport_uid:
        params['puid'] = passport_uid
    # noinspection PyBroadException
    try:
        tvm_headers = add_service_ticket_header(settings.BIGB_CLIENT_ID)
        response = requests.get(settings.BIGB_URL, params=params, timeout=BIGB_TIMEOUT, headers=tvm_headers)
        response.raise_for_status()
        bigb_data = _parse_bigb_data(json.loads(response.content))
        return bigb_data
    except RequestException as e:
        logger.warning('Cannot get info from bigb: %s', e, extra=params)
    except (ValueError, TypeError):
        logger.error('Arrived bad content from bigb', extra={'stack': True})
    except Exception as e:
        logger.exception('Unknown exception in crypta update', extra=params)
