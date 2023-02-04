# -*- coding: utf-8 -*-
import pytest
import json
import httpretty
from base64 import b64encode
from tests import object_builder as ob

from muzzle.abt.usaas import URL as usaas_url
from muzzle.abt.experiment import check_experiment
from balance.corba_buffers import StateBuffer, RequestBuffer

abt_version = '1'
abt_boxes = 'boxes'
abt_boxes_crypted = 'boxes_crypted'
session = {}


@pytest.fixture(name='request_')
def create_request(session):
    return ob.RequestBuilder.construct(session)


@pytest.fixture(name='invoice')
def create_invoice(session):
    return ob.InvoiceBuilder.construct(session)


def mock_abt_active_experiment():
    httpretty.register_uri(
        httpretty.GET,
        usaas_url,
        **{
            'X-Yandex-ExpConfigVersion': abt_version,
            'X-Yandex-ExpBoxes': abt_boxes,
            'X-Yandex-ExpBoxes-Crypted': abt_boxes_crypted,
            'X-Yandex-ExpFlags': b64encode(json.dumps([{
                'HANDLER': 'BALANCE',
                'CONTEXT': {'MAIN': {'BALANCE': {'balance_user_persons': 'true'}}},
                'TESTID': ['123', '234'],
            }])),
        }
    )


def mock_abt_without_experiment():
    httpretty.register_uri(
        httpretty.GET,
        usaas_url,
        **{
            'X-Yandex-ExpConfigVersion': abt_version,
            'X-Yandex-ExpBoxes': abt_boxes,
            'X-Yandex-ExpBoxes-Crypted': abt_boxes_crypted,
            'X-Yandex-ExpFlags': b64encode(json.dumps([])),
        }
    )

def get_state_obj(request_id='12345', invoice_id=None):
    params = {
        'prot_remote_ip': '95.108.172.0',
        'prot_host': 'balance.yandex.ru'
    }
    if request_id:
        params['req_request_id'] = request_id
    if invoice_id:
        params['req_invoice_id'] = invoice_id
    return StateBuffer(params=params)


def get_request_obj(additional_cookies=None):
    if additional_cookies is None:
        additional_cookies = []

    in_headers = [
        ('User-Agent', 'Mozilla/5.0')
    ]
    in_cookies = [
                     ('Session_id', str(ob.get_big_number())),
                     ('sessionid2', str(ob.get_big_number())),
                     ('yandexuid', str(ob.get_big_number()))
                 ] + additional_cookies
    return RequestBuffer(
        params=[
            [],  # in_params
            in_headers,  # in_headers
            in_cookies,  # in_cookies
        ],
    )


@pytest.mark.parametrize(
    'obj_creator, obj_type',
    [
        pytest.param(create_request, 'request_id', id='request'),
        pytest.param(create_invoice, 'invoice_id', id='invoice'),
    ],
)
@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_check_persons_experiment_with_active_experiment(muzzle_logic, session, obj_creator, obj_type):
    mock_abt_active_experiment()
    obj = obj_creator(session)

    params = {'request_id': None, 'invoice_id': None}
    params[obj_type] = obj.id

    state_obj = get_state_obj(**params)
    request_obj = get_request_obj()

    abt_experiment = check_experiment(session, state_obj, request_obj)

    assert abt_experiment.boxes_crypted == abt_boxes_crypted
    assert abt_experiment.boxes == abt_boxes
    assert abt_experiment.version == abt_version
    assert abt_experiment.flags.get_props() == ['balance_user_persons']
    assert abt_experiment.test_ids == '123,234'


@pytest.mark.parametrize(
    'obj_creator, obj_type',
    [
        pytest.param(create_request, 'request_id', id='request'),
        pytest.param(create_invoice, 'invoice_id', id='invoice'),
    ],
)
@pytest.mark.usefixtures('httpretty_enabled_fixture')
# в experiment.py назывался test_check_persons_experiment_without_active_experiment
def test_check_persons_experiment_without_experiment(muzzle_logic, session, obj_creator, obj_type):
    mock_abt_without_experiment()
    obj = obj_creator(session)

    params = {'request_id': None, 'invoice_id': None}
    params[obj_type] = obj.id

    state_obj = get_state_obj(**params)
    request_obj = get_request_obj()

    abt_experiment = check_experiment(session, state_obj, request_obj)

    assert abt_experiment.boxes_crypted == abt_boxes_crypted
    assert abt_experiment.boxes == abt_boxes
    assert abt_experiment.version == abt_version
    assert abt_experiment.flags == ''
    assert abt_experiment.test_ids == ''
