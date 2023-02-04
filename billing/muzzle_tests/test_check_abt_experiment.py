# -*- coding: utf-8 -*-
import mock
from tests import object_builder as ob

from muzzle.abt.experiment import AbtExperiment
from balance.corba_buffers import StateBuffer, RequestBuffer

abt_version = '1'
abt_boxes = 'boxes'
abt_boxes_crypted = 'boxes_crypted'
abt_default_experiment = AbtExperiment(abt_version, {'balance_admin_new_ui': 'true'}, ['123', '234'], abt_boxes,
                               abt_boxes_crypted)
empty_abt_experiment = AbtExperiment(abt_version, {}, [], abt_boxes, abt_boxes_crypted)
session = {}

def get_state_obj():
    params = {
        'prot_remote_ip': '95.108.172.0',
        'prot_host': 'balance.yandex.ru'
    }
    return StateBuffer(params=params)


def get_request_obj():
    in_headers = [
        ('User-Agent', 'Mozilla/5.0')
    ]
    in_cookies = [
        ('Session_id', str(ob.get_big_number())),
        ('sessionid2', str(ob.get_big_number())),
        ('yandexuid', str(ob.get_big_number()))
    ]
    return RequestBuffer(
        params=[
            [],  # in_params
            in_headers,  # in_headers
            in_cookies,  # in_cookies
        ],
    )


def test_abt_default_experiment_with_experiment(muzzle_logic):
    with mock.patch('muzzle.abt.experiment.check_experiment', mock.MagicMock(return_value=abt_default_experiment)):
        state_obj = get_state_obj()
        request_obj = get_request_obj()

        res = muzzle_logic.check_abt_experiment(session, state_obj, request_obj)

        assert res.findtext('boxes-crypted') == abt_boxes_crypted
        assert res.findtext('boxes') == abt_boxes
        assert res.findtext('version') == abt_version
        assert res.find('flags').findtext('balance-admin-new-ui') == 'true'
        assert state_obj.getParam('balance_admin_new_ui') == '1'


def test_abt_default_experiment_without_experiment(muzzle_logic):
    with mock.patch('muzzle.abt.experiment.check_experiment', mock.MagicMock(return_value=empty_abt_experiment)):
        state_obj = get_state_obj()
        request_obj = get_request_obj()

        res = muzzle_logic.check_abt_experiment(session, state_obj, request_obj)

        assert res.findtext('boxes-crypted') == abt_boxes_crypted
        assert res.findtext('boxes') == abt_boxes
        assert res.findtext('version') == abt_version
        assert res.find('flags').findtext('balance-admin-new-ui') is None
        assert not state_obj.hasParam('balance_admin_new_ui')


def test_abt_default_experiment_with_error(muzzle_logic):
    with mock.patch('muzzle.abt.experiment.check_experiment', mock.MagicMock(side_effect=TypeError)):
        state_obj = get_state_obj()
        request_obj = get_request_obj()

        res = muzzle_logic.check_abt_experiment(session, state_obj, request_obj)

        assert res.find('abt-error') is not None
