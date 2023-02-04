import os
import sys

import copy
import signal
import time
from datetime import datetime
from decimal import Decimal
from typing import Optional, Dict

import arrow
import pytest
import flask
import pytz

import yatest.common
import yql_ports

from billing.log_tariffication.py.lib import constants


REWARD_GROSS = 1
REWARD_NET = 2


def create_table(yt_client, table_path, info):
    yt_client.create('table', table_path, recursive=True, attributes=info['attributes'])
    yt_client.write_table(
        table_path,
        list(map(lambda r: dict(copy.deepcopy(info.get('common_data_part', dict())), **r), info.get('data', list())))
    )


def positive_hash(obj):
    """Возвращает положительное число - хэш объекта"""
    h = hash(obj)
    return h if h >= 0 else h + sys.maxsize


@pytest.fixture(scope='session')
def udf_server_file_url():
    """
    Required in ya.make

    DEPENDS(
        billing/udf_python
    )
    """
    path = yatest.common.binary_path('billing/udf_python/libpython3_udf.so')
    port = yql_ports.get_yql_port('udf_server')
    pid = os.fork()
    if pid == 0:
        app = flask.Flask(__name__)

        @app.route('/')
        def get_file():
            return flask.send_file(path)
        app.run("127.0.0.1", port)

    yield f'http://localhost:{port}'
    os.kill(pid, signal.SIGTERM)


def to_ts(dt: datetime) -> int:
    return arrow.get(dt).replace(tzinfo=constants.MSK_TZ).int_timestamp


def append_collateral(contract_json, collateral_attrs):
    next_num = '{}'.format(max(int(i) for i in contract_json['collaterals'].keys()) + 1)
    contract_json['collaterals'][next_num] = collateral_attrs
    return contract_json


def gen_contract_ref_row(
    contract_id: int,
    client_id: int,
    signed: bool = True,
    cancelled: bool = False,
    dt: Optional[datetime] = None,
    finish_dt: Optional[datetime] = None,
    end_dt: Optional[datetime] = None,
    test_mode: bool = False,
    reward_type: Optional[int] = REWARD_GROSS,
    agregator_pct: Optional[str] = None,
    dsp_agregation_pct: Optional[str] = None,
    collaterals: Optional[list] = None,
    contract_type: str = 'PARTNERS',
    person_type: Optional[str] = None,
    currency: int = 643,
    firm: int = 1,
    services: Optional[list[int]] = None,
) -> dict:
    assert test_mode and not signed or signed and not test_mode

    dt = dt or datetime(2020, 1, 1)

    col0 = {
        'collateral_type_id': None,
        'currency': currency,
        'dt': dt.isoformat(),
        'firm': firm,
        'is_signed': dt.isoformat() if signed else None,
        'is_suspended': None,
        'manager_code': 27649,
        'nds': 18,
        'reward_type': reward_type,
        'partner_pct': 45,
        'num': None,
        'test_mode': int(test_mode),
        'services': {str(service_id): 1 for service_id in (services or [])}
    }

    if finish_dt:
        assert contract_type == 'GENERAL'
        col0['finish_dt'] = finish_dt.isoformat()
    if end_dt:
        assert contract_type != 'GENERAL'
        col0['end_dt'] = end_dt.isoformat()

    if agregator_pct:
        col0['agregator_pct'] = agregator_pct
    if dsp_agregation_pct:
        col0['dsp_agregation_pct'] = dsp_agregation_pct
    if cancelled:
        col0['is_cancelled'] = dt.isoformat()

    contract_obj = {
        'client_id': client_id,
        'collaterals': {
            '0': col0,
        },
        'external_id': 'e-{}'.format(contract_id),
        'id': contract_id,
        'type': contract_type,
        'version_id': 1,
        'passport_id': 666,
        'person_id': 666,
        'update_dt': '2020-01-01T15:35:00',
        'person_type': person_type,
    }
    if collaterals:
        for c in collaterals:
            append_collateral(contract_obj, c)

    return {'ID': contract_id, 'Version': 1, 'Object': contract_obj}


def gen_page_ref_row(page_id: int, client_id: int, internal: Optional[bool] = False) -> dict:
    page_obj = {
        'products': [],
        'client': client_id,
        'dt': "2000-08-24T10:58:11+03:00",
        'id': page_id,
        'internal_type': 2 if internal else 0,
        'mkb_category': None,
        'passport_id': 666,
        'search_id': page_id,
        'type': 20,
        'url': 'balance.yandex.ru',
        'version_id': 1
    }
    return {'ID': page_id, 'Version': 1, 'Object': page_obj}


def gen_aggregator_page_ref_row(page_id: int, client_id: int) -> dict:
    page_obj = {
        'ClientID': client_id,
        'EndDT': None,
        'PageID': page_id,
        'StartDT': '2005-10-18',
        'id': page_id
    }
    return {'ID': page_id, 'Version': 1, 'Object': page_obj}


def get_unixtime(dt: datetime) -> int:
    return int(time.mktime(dt.timetuple()))


def gen_currency_ref_row(currency: str, rate: Decimal, dt: str) -> Dict:
    id_ = positive_hash((currency, dt, rate))
    obj = {
        "dt": dt,
        "id": id_,
        "iso_currency_from": "RUB",
        "iso_currency_to": currency.upper(),
        "rate_from": str(rate),
        "rate_to": "1",
        "src_cc": "cbr",
        "version_id": 0
    }
    return {'ID': id_, 'Version': 1, 'Object': obj}


def gen_product_ref_row(currency: str, product_id: int, product_mdh_id: str) -> Dict:
    obj = {
        "PRODUCT_CURRENCY": currency.upper(),
        "PRODUCT_ID": product_id,
        "PRODUCT_MDH_ID": product_mdh_id,
    }
    return obj


def gen_client_dsp_ref_row(dsp_id: int, client_id: int) -> Dict:
    obj = {
        "dsp_id": dsp_id,
        "client_id": client_id,
        "operator_uid": 192754335,
        "update_dt": "2013-04-16 16:51:05",  # MSK timezone
    }
    return {'ID': dsp_id, 'Version': 1, 'Object': obj}


moscow_tz = pytz.timezone('Europe/Moscow')


def date_from_string(s: str, msk_aware: bool = False) -> datetime:
    dt = datetime.strptime(s, '%Y-%m-%d')
    return moscow_tz.localize(dt) if msk_aware else dt
