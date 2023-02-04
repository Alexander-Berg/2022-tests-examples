# -*- coding: utf-8 -*-

import pytest
import copy
from . import steps
from balance import balance_steps
from temp.igogor.balance_objects import Contexts
from btestlib.constants import Firms, PersonTypes, Export
from jsonrpc import dispatcher
from ..common_defaults import *

import balance.balance_db as db


CONTEXT = steps.CONTEXT


# создание нескольких плательщиков разных типов у одного клиента
@pytest.mark.parametrize('person_params_list', [
    [FIXED_UR_PARAMS, FIXED_PH_PARAMS],
    [FIXED_BYU_PARAMS, FIXED_SW_YT_PARAMS, FIXED_SW_YTPH_PARAMS]
], ids=['ur, ph', 'byu, sw_yt, sw_ytph'])
def test_several_persons(person_params_list):
    client_id = balance_steps.ClientSteps.create({'NAME': CLIENT_NAME})
    for params in person_params_list:
        not_partner_params = copy.deepcopy(params)
        balance_steps.PersonSteps.create(client_id, not_partner_params['type'], params=not_partner_params)

        partner_params = copy.deepcopy(params)
        partner_params.update({'is-partner': 1})
        balance_steps.PersonSteps.create(client_id, not_partner_params['type'], params=partner_params)
    return client_id


# ur, ph партнерские и непартнерские на одного клиента
def test_ur_and_ph_partner_and_not_partner():
    client_id, person_id_ur_0 = steps.create_client_and_person()
    ur_partner_params = copy.deepcopy(FIXED_UR_PARAMS)
    ur_partner_params.update({'is-partner': 1})
    person_id_ur_1 = balance_steps.PersonSteps.create(client_id, PersonTypes.UR.code, params=ur_partner_params)

    ph_params = copy.deepcopy(FIXED_PH_PARAMS)
    person_id_ph_0 = balance_steps.PersonSteps.create(client_id, PersonTypes.PH.code, params=ph_params)

    ph_partner_params = copy.deepcopy(FIXED_PH_PARAMS)
    ph_partner_params.update({'is-partner': 1})
    person_id_ph_1 = balance_steps.PersonSteps.create(client_id, PersonTypes.PH.code, params=ph_partner_params)
    return client_id, person_id_ur_0, person_id_ur_1, person_id_ph_0, person_id_ph_1


# ur, ph партнерские и непартнерские на одного клиента с разными названиями
@dispatcher.add_method
def test_ur_and_ph_partner_and_not_partner_ci(login=None):
    client_id, person_id_ur_0 = steps.create_client_and_person()
    ur_partner_params = copy.deepcopy(FIXED_UR_PARAMS)
    ur_partner_params.update({'is-partner': 1, 'name':u'ООО "Плательщик" партнерский'})
    person_id_ur_1 = balance_steps.PersonSteps.create(client_id, PersonTypes.UR.code, params=ur_partner_params)

    ph_params = copy.deepcopy(FIXED_PH_PARAMS)
    ph_params.pop('inn')
    person_id_ph_0 = balance_steps.PersonSteps.create(client_id, PersonTypes.PH.code, params=ph_params)

    ph_partner_params = copy.deepcopy(FIXED_PH_PARAMS)
    ph_partner_params.update({'is-partner': 1, 'lname': u'Партнерова'})
    person_id_ph_1 = balance_steps.PersonSteps.create(client_id, PersonTypes.PH.code, params=ph_partner_params)

    person_params = copy.deepcopy(FIXED_UR_PARAMS)
    person_params.pop('name')
    person_params.update({'fname': 'Плательщик',
                          'lname': 'Плательщиков',
                          'mname': 'Плательщикович',
                          'ownership_type': 'SELFEMPLOYED',
                          'name': '',
                          'inn': 138321567704,
                          'kpp': ''})
    person_id = balance_steps.PersonSteps.create(client_id, PersonTypes.UR.code, person_params)
    if login:
        balance_steps.ClientSteps.link(client_id, login)
    return client_id, person_id_ur_0, person_id_ur_1, person_id_ph_0, person_id_ph_1


# byu, sw_yt, sw_ytph, партнерские и непартнерские на одного клиента
def test_byu_sw_yt_sw_ytph_partner_and_not_partner():
    client_id = balance_steps.ClientSteps.create({'NAME': CLIENT_NAME})
    byu_params = copy.deepcopy(FIXED_BYU_PARAMS)
    person_id_byu_0 = balance_steps.PersonSteps.create(client_id, PersonTypes.BYU.code, params=byu_params)

    byu_partner_params = copy.deepcopy(FIXED_BYU_PARAMS)
    byu_partner_params.update({'is-partner': 1})
    person_id_byu_1 = balance_steps.PersonSteps.create(client_id, PersonTypes.BYU.code, params=byu_partner_params)

    sw_yt_params = copy.deepcopy(FIXED_SW_YT_PARAMS)
    person_id_sw_yt_0 = balance_steps.PersonSteps.create(client_id, PersonTypes.SW_YT.code, params=sw_yt_params)

    sw_yt_partner_params = copy.deepcopy(FIXED_SW_YT_PARAMS)
    sw_yt_partner_params.update({'is-partner': 1})
    person_id_sw_yt_1 = balance_steps.PersonSteps.create(client_id, PersonTypes.SW_YT.code, params=sw_yt_partner_params)

    sw_ytph_params = copy.deepcopy(FIXED_SW_YTPH_PARAMS)
    person_id_sw_ytph_0 = balance_steps.PersonSteps.create(client_id, PersonTypes.SW_YTPH.code, params=sw_ytph_params)

    sw_ytph_partner_params = copy.deepcopy(FIXED_SW_YTPH_PARAMS)
    sw_ytph_partner_params.update({'is-partner': 1})
    person_id_sw_ytph_1 = balance_steps.PersonSteps.create(client_id, PersonTypes.SW_YTPH.code, params=sw_ytph_partner_params)
    return client_id, person_id_byu_0, person_id_byu_1, person_id_sw_yt_0, \
           person_id_sw_yt_1, person_id_sw_ytph_0, person_id_sw_ytph_1


# byu, sw_yt, sw_ytph, партнерские и непартнерские на одного клиента
@dispatcher.add_method
def test_various_partner_and_not_partner_ci(login=None):
    client_id = balance_steps.ClientSteps.create({'NAME': CLIENT_NAME})
    byu_params = copy.deepcopy(FIXED_BYU_PARAMS)
    byu_params.update({'name': u'BYU не партнер'})
    person_id_byu_0 = balance_steps.PersonSteps.create(client_id, PersonTypes.BYU.code, params=byu_params)

    byu_partner_params = copy.deepcopy(FIXED_BYU_PARAMS)
    byu_partner_params.update({'is-partner': 1, 'name': u'BYU партнер'})
    person_id_byu_1 = balance_steps.PersonSteps.create(client_id, PersonTypes.BYU.code, params=byu_partner_params)

    sw_yt_params = copy.deepcopy(FIXED_SW_YT_PARAMS)
    sw_yt_params.update({'name': u'SW_YT не партнер'})
    person_id_sw_yt_0 = balance_steps.PersonSteps.create(client_id, PersonTypes.SW_YT.code, params=sw_yt_params)

    sw_yt_partner_params = copy.deepcopy(FIXED_SW_YT_PARAMS)
    sw_yt_partner_params.update({'is-partner': 1, 'name': u'SW_YT партнер'})
    person_id_sw_yt_1 = balance_steps.PersonSteps.create(client_id, PersonTypes.SW_YT.code, params=sw_yt_partner_params)

    sw_ytph_params = copy.deepcopy(FIXED_SW_YTPH_PARAMS)
    person_id_sw_ytph_0 = balance_steps.PersonSteps.create(client_id, PersonTypes.SW_YTPH.code, params=sw_ytph_params)

    sw_ytph_partner_params = copy.deepcopy(FIXED_SW_YTPH_PARAMS)
    person_id_sw_ytph_1 = balance_steps.PersonSteps.create(client_id, PersonTypes.SW_YTPH.code, params=sw_ytph_partner_params)

    by_ytph_params = copy.deepcopy(FIXED_BY_YTPH_PARAMS)
    person_id_by_ytph_0 = balance_steps.PersonSteps.create(client_id, PersonTypes.BY_YTPH.code, params=by_ytph_params)

    byp_params = copy.deepcopy(FIXED_BYP_PARAMS)
    person_id_byp_0 = balance_steps.PersonSteps.create(client_id, PersonTypes.BYP.code, params=byp_params)

    ytph_params = copy.deepcopy(FIXED_YTPH_PARAMS)
    person_id_ytph_0 = balance_steps.PersonSteps.create(client_id, PersonTypes.YTPH.code, params=ytph_params)

    if login:
        balance_steps.ClientSteps.link(client_id, login)
    return client_id, person_id_byu_0, person_id_byu_1, person_id_sw_yt_0, \
           person_id_sw_yt_1, person_id_sw_ytph_0, person_id_sw_ytph_1

# выгруженный в ОЕБС
def test_exported_person():
    client_id, person_id = steps.create_client_and_person()
    query = "update t_export set state=1, export_dt=:export_dt where classname = 'Person' and object_id = :person_id"
    db.balance().execute(query, {'export_dt': steps.NOW,'person_id': person_id})
    return client_id, person_id


# самозанятый юрик РФ
@dispatcher.add_method
def test_selfemployed_ur_person():
    client_id = balance_steps.ClientSteps.create(params={'NAME': CLIENT_NAME})
    person_params = copy.deepcopy(FIXED_UR_PARAMS)
    person_params.pop('name')
    person_params.update({'fname': 'Плательщик',
                          'lname': 'Плательщиков',
                          'mname': 'Плательщикович',
                          'ownership_type': 'SELFEMPLOYED',
                          'name': '',
                          'inn': 138321567704,
                          'kpp': ''})
    person_id = balance_steps.PersonSteps.create(client_id, PersonTypes.UR.code, person_params)
    return client_id, person_id


# батч 9999999, добавляем клиента в батч
def test_client_in_batch():
    client_id, person_id = steps.create_client_and_person()
    balance_steps.ClientSteps.insert_client_into_batch(client_id)


@dispatcher.add_method
def test_hidden_persons_ci(login=None):
    client_id, person_id_ur_0 = steps.create_client_and_person()
    ur_partner_params = copy.deepcopy(FIXED_UR_PARAMS)
    ur_partner_params.update({'is-partner': 1, 'name':u'ООО "Плательщик" партнерский'})
    person_id_ur_1 = balance_steps.PersonSteps.create(client_id, PersonTypes.UR.code, params=ur_partner_params)

    ph_params = copy.deepcopy(FIXED_PH_PARAMS)
    ph_params.pop('inn')
    person_id_ph_0 = balance_steps.PersonSteps.create(client_id, PersonTypes.PH.code, params=ph_params)

    ph_partner_params = copy.deepcopy(FIXED_PH_PARAMS)
    ph_partner_params.update({'is-partner': 1, 'lname': u'Партнерова'})
    person_id_ph_1 = balance_steps.PersonSteps.create(client_id, PersonTypes.PH.code, params=ph_partner_params)
    balance_steps.PersonSteps.hide_person(person_id_ph_0)
    balance_steps.PersonSteps.hide_person(person_id_ph_1)
    balance_steps.PersonSteps.hide_person(person_id_ur_0)
    balance_steps.PersonSteps.hide_person(person_id_ur_1)
    if login:
        balance_steps.ClientSteps.link(client_id, login)
    return client_id


@dispatcher.add_method
def test_many_persons_ci(login='yndx-balance-assessor-100'):
    client_id = balance_steps.ClientSteps.create(params={'NAME': CLIENT_NAME})
    person_params = copy.deepcopy(FIXED_UR_PARAMS)
    for i in range(30):
        person_params.update({'name': 'Плательщик номер ' + str(i)})
        person_id = balance_steps.PersonSteps.create(client_id, PersonTypes.UR.code, params=person_params)
    if login:
        balance_steps.ClientSteps.link(client_id, login)
    return client_id

