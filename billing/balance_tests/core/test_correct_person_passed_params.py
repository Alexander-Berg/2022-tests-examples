# -*- coding: utf-8 -*-

"test_correct_person_passed_params"

# from __future__ import unicode_literals

import datetime
import itertools

import pytest
import mock
import hamcrest

from balance import core
from balance import exc
from balance import muzzle_util as ut
from balance import mapper
from balance.constants import (
    ServiceId,
    DIRECT_PRODUCT_ID,
)

from tests import object_builder as ob


@pytest.fixture
def person(session, client):
    return ob.PersonBuilder(client=client, type='ur').build(session).obj


@pytest.fixture
def person_ph(session, client):
    return ob.PersonBuilder(client=client, type='ph').build(session).obj


@pytest.mark.parametrize(
    'params, rm_inn_config_val, ch_kpp_config_val, res',
    [
        (
            {'inn': '123456789012', 'kpp': 'Мне не нужен КПП', 'type': 'ur'},
            1, 0, {'inn': '123456789012', 'kpp': ''},
        ),
        (
            {'inn': '1234567890', 'kpp': 'А мне нужен!', 'type': 'ur'},
            1, 0, {'inn': '1234567890', 'kpp': 'А мне нужен!'},
        ),
        (
            {'inn': '1234567890  ', 'kpp': 'И мне нужен, но я кривой', 'type': 'ur'},
            1, 0, {'inn': '1234567890  ', 'kpp': 'И мне нужен, но я кривой'},
        ),
        (
            {'inn': '123456789012', 'kpp': 'Мне тоже не нужен КПП, но нет конфига', 'type': 'ur'},
            0, 0, {'inn': '123456789012', 'kpp': 'Мне тоже не нужен КПП, но нет конфига'},
        ),
        (
            {'inn': '1234567890  ', 'kpp': 'НЕ НУЖЕН!', 'type': 'yt'},
            1, 1, {'inn': '1234567890  ', 'kpp': ''},
        ),
        (
            {'inn': '1234567890  ', 'kpp': 'НЕ НУЖЕН!', 'type': 'sw'},
            0, 1, {'inn': '1234567890  ', 'kpp': ''},
        ),
        (
            {'inn': '123456789012', 'kpp': 'Мне тоже не нужен КПП, но нет конфига', 'type': 'ur'},
            0, 1, {'inn': '123456789012', 'kpp': 'Мне тоже не нужен КПП, но нет конфига'},
        ),
        (
            {'inn': '1234567890  ', 'type': 'yt'},
            1, 1, {'inn': '1234567890  '},
        ),
    ],
    ids=['IP', 'Not IP', 'Not IP but spaces', 'IP but no config', 'Not ur', 'Not ur, inn config off',
         'Ip, inn config off', 'No kpp']
)
def test_correct_params_new_person(session, core_obj, params, rm_inn_config_val, ch_kpp_config_val, res):
    session.config.__dict__['PERSON_REMOVE_KPP_FROM_IP'] = rm_inn_config_val
    session.config.__dict__['CHECK_PERSON_KPP'] = ch_kpp_config_val
    ptype = params.pop('type')
    core_obj._correct_person_passed_params(None, params, ptype, 666)
    assert params == res


@pytest.mark.parametrize(
    'params, res',
    [
        (
            {'person_inn': '123456789012', 'kpp': 'Мне не нужен КПП', 'type': 'ur'},
            {'kpp': ''},
        ),
        (
            {'person_inn': '1234567890', 'kpp': 'А мне нужен!', 'type': 'ur'},
            {'kpp': 'А мне нужен!'},
        ),
        (
            {'person_inn': '1234567890  ', 'kpp': 'И мне нужен!', 'type': 'ur'},
            {'kpp': 'И мне нужен!'},
        ),
        (
            {'person_inn': '1234567890', 'kpp': 'А я теперь ИП!', 'inn': '123456789012', 'type': 'ur'},
            {'kpp': '', 'inn': '123456789012'},
        ),
        (
            {'person_inn': '123456789012', 'kpp': 'А я теперь ИП нерезидент!', 'type': 'yt'},
            {'kpp': ''},
        ),
        (
            {'person_inn': '1234567890', 'kpp': 'И мне нужен!', 'inn': '123456789012', 'type': 'yt'},
            {'kpp': '', 'inn': '123456789012'},
        )
    ],
    ids=['IP', 'Not IP', 'Not IP but spaces', 'Now IP', 'yt IP', 'yt']
)
def test_correct_params_update_person(session, core_obj, person, params, res):
    session.config.__dict__['PERSON_REMOVE_KPP_FROM_IP'] = 1
    session.config.__dict__['CHECK_PERSON_KPP'] = 1
    person.inn = params.pop('person_inn')
    ptype = params.pop('type')
    core_obj._correct_person_passed_params(person, params, ptype, 666)


@pytest.mark.parametrize(
    'params, res',
    [
        (
            {'inn': '123456789012', 'kpp': u'Мне не нужен КПП', 'type': 'ur'},
            None,
        ),
        (
            {'inn': '1234567890', 'kpp': u'111222333', 'type': 'ur'},
            u'111222333',
        ),
        (
            {'inn': '2311134280  ', 'kpp': u'666999666', 'person_id': None, 'type': 'ur'},
            u'666999666',
        ),
        (
            {'inn': '1234567890', 'kpp': u'А мне не нужен!', 'type': 'ph'},
            None,
        ),
        (
            {'inn': '2311134280  ', 'kpp': u'И мне не нужен!', 'person_id': None, 'type': 'ph'},
            None,
        )
    ],
    ids=['IP', 'Not IP', 'Not IP but spaces', 'Ph update', 'Ph create']
)
def test_create_or_update_person(session, core_obj, person, person_ph, params, res):
    session.config.__dict__['PERSON_REMOVE_KPP_FROM_IP'] = 1
    session.config.__dict__['CHECK_PERSON_KPP'] = 1
    ptype = params.pop('type')

    req = {
        'kpp': params['kpp']
    }

    if ptype == 'ph':
        person = person_ph
        req['lname'] = 'Fake'
        req['fname'] = 'Fak'
        req['mname'] = 'Fa'

    person_id = params.get('person_id', person.id)
    req.update(
        {
            'client_id': person.client_id,
            'person_id': person_id,
            'type': person.type,
        }
    )
    if person_id is None:
        req['inn'] = params['inn']
        req['name'] = 'Fake IP'
        req['longname'] = 'Fake IP but longname'
        req['phone'] = '+79136666666'
        req['email'] = 'fake@ip.su'
        req['postcode'] = '117321'
        req['postaddress'] = 'fake str ip'
        req['legaladdress'] = 'legal fake str ip'
    else:
        person.inn = params['inn']
        person.kpp = '666'

    person_id = core_obj.create_or_update_person(
        session,
        None,
        req
    )
    person = session.query(mapper.Person).getone(person_id)
    assert person.kpp == res
