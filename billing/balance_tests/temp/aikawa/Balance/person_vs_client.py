# -*- coding: utf-8 -*-
__author__ = 'aikawa'

import pytest
from hamcrest import *

from balance import balance_db as db
from balance import balance_steps as steps
from btestlib import matchers as match
import btestlib.utils as utils

person_categories_list = [
    {'person_type': 'yt'},
    {'person_type': 'ur'},
    {'person_type': 'ph'},
    {'person_type': 'ytph'},
    {'person_type': 'yt_kzp'},
    {'person_type': 'yt_kzu'},
    {'person_type': 'endbuyer_ph'},
    {'person_type': 'endbuyer_ur'},
    {'person_type': 'endbuyer_yt'},
    {'person_type': 'pu'},
    {'person_type': 'ua'},
    {'person_type': 'kzu'},
    {'person_type': 'kzp'},
    {'person_type': 'usu'},
    {'person_type': 'usp'},
    {'person_type': 'byu'},
    {'person_type': 'byp'},
    {'person_type': 'eu_ur'},
    {'person_type': 'eu_yt'},
    {'person_type': 'sw_ur'},
    {'person_type': 'sw_yt'},
    {'person_type': 'sw_ytph'},
    {'person_type': 'sw_ph'},
    {'person_type': 'by_ytph'},
    {'person_type': 'tru'},
    {'person_type': 'trp'},
    {'person_type': 'ur_autoru'},
    {'person_type': 'ph_autoru'}
]

person_categories_ids = [x['person_type'] for x in person_categories_list]


def create_person_on_client_wo_region(client_id, person_type):
    try:
        person_id = steps.PersonSteps.create(client_id, person_type)
    except Exception, exc:
        if 'PERSON_TYPE_MISMATCH' == steps.CommonSteps.get_exception_code(exc):
            return 'PERSON_TYPE_MISMATCH'
    return person_id


@pytest.mark.parametrize('person_category', person_categories_list
    , ids=person_categories_ids)
def test_create_person_on_client_wo_region(person_category):
    client_id = steps.ClientSteps.create()
    person_type = person_category['person_type']
    person_id = create_person_on_client_wo_region(client_id, person_type)
    if person_id == 'PERSON_TYPE_MISMATCH':
        assert person_type in ['eu_ur', 'eu_yt', 'endbuyer_ph', 'endbuyer_ur', 'endbuyer_yt']
    else:
        person = db.get_persons_by_client(client_id)[0]
        utils.check_that(person['id'], equal_to(int(person_id)))

#regions where count(client) > 1000

region_params_list = [
    {'region_id': '84', 'region_name': 'США', 'pay_policy_ids': [1200, 410, 1300, 100]},
    {'region_id': '96', 'region_name': 'Германия', 'pay_policy_ids': [100, 1200, 700, 1300]},
    {'region_id': '102', 'region_name': 'Великобритания', 'pay_policy_ids': [100, 1200, 700, 1300]},
    {'region_id': '115', 'region_name': 'Болгария', 'pay_policy_ids': [100, 1200, 700, 1300]},
    {'region_id': '120', 'region_name': 'Польша', 'pay_policy_ids': [100, 1200, 700, 1300]},
    {'region_id': '123', 'region_name': 'Финляндия', 'pay_policy_ids': [100, 1200, 700, 1300]},
    {'region_id': '124', 'region_name': 'Франция', 'pay_policy_ids': [100, 1200, 700, 1300]},
    {'region_id': '125', 'region_name': 'Чехия', 'pay_policy_ids': [100, 1200, 700, 1300]},
    {'region_id': '149', 'region_name': 'Беларусь', 'pay_policy_ids': [701,1300,100,1200,112, 510]},
    {'region_id': '159', 'region_name': 'Казахстан', 'pay_policy_ids': [112, 102, 1300, 310, 1200, 100]},
    {'region_id': '167', 'region_name': 'Азербайджан', 'pay_policy_ids': [100, 1200, 701, 1300]},
    {'region_id': '168', 'region_name': 'Армения', 'pay_policy_ids': [100, 1200, 701, 1300]},
    {'region_id': '171', 'region_name': 'Узбекистан', 'pay_policy_ids': [100, 1200, 701, 1300]},
    {'region_id': '179', 'region_name': 'Эстония', 'pay_policy_ids': [100, 1200, 700, 1300]},
    {'region_id': '181', 'region_name': 'Израиль', 'pay_policy_ids': [100, 1200, 700, 1300]},
    {'region_id': '187', 'region_name': 'Украина', 'pay_policy_ids': [1300, 112, 210, 1200]},
    {'region_id': '204', 'region_name': 'Испания', 'pay_policy_ids': [100, 1200, 700, 1300]},
    {'region_id': '205', 'region_name': 'Италия', 'pay_policy_ids': [100, 1200, 700, 1300]},
    {'region_id': '206', 'region_name': 'Латвия', 'pay_policy_ids': [100, 1200, 700, 1300]},
    {'region_id': '207', 'region_name': 'Киргизия', 'pay_policy_ids': [100, 1200, 701, 1300]},
    {'region_id': '208', 'region_name': 'Молдова', 'pay_policy_ids': [100, 1200, 701, 1300]},
    {'region_id': '211', 'region_name': 'Австралия', 'pay_policy_ids': [100, 1200, 700, 1300]},
    {'region_id': '225', 'region_name': 'Россия', 'pay_policy_ids': [112, 1310, 1210, 110, 1010]},
    {'region_id': '983', 'region_name': 'Турция', 'pay_policy_ids': [1200, 810, 100, 1300]},
    {'region_id': '994', 'region_name': 'Индия', 'pay_policy_ids': [100, 1200, 700, 1300]},
    {'region_id': '10093', 'region_name': 'Вьетнам', 'pay_policy_ids': [100, 1200, 700, 1300]},
    {'region_id': '10095', 'region_name': 'Индонезия', 'pay_policy_ids': [100, 1200, 700, 1300]}
]

region_params_list_ids = [x['region_name'] for x in region_params_list]


def create_client_with_region(region_id):
    client_id = steps.ClientSteps.create({'REGION_ID': region_id})
    return client_id


@pytest.mark.parametrize('region_param', region_params_list
    , ids=region_params_list_ids)
def test_create_client_with_region(region_param):
    region_id = region_param['region_id']
    client_id = create_client_with_region(region_id)

    client = db.get_client_by_id(client_id)[0]
    utils.check_that(client['region_id'], equal_to(int(region_id)))


person_categories_list_who_may_be_created = [
    {'person_type': 'yt', 'resident': 0, 'firm_ids': ['1', '12', '13'], 'region_id': '225', 'pay_policy_ids': [100, 701, 1200, 1300]},
    {'person_type': 'ur', 'resident': 1, 'firm_ids': ['1', '12', '13'], 'region_id': '225', 'pay_policy_ids': [110, 112, 1210, 1310]},
    {'person_type': 'ph', 'resident': 1, 'firm_ids': ['1', '12', '13'], 'region_id': '225', 'pay_policy_ids': [110, 112, 1210, 1310]},
    {'person_type': 'ytph', 'resident': 0, 'firm_ids': ['1', '12', '13'], 'region_id': '225', 'pay_policy_ids': [100, 112, 701, 1200, 1300]},
    {'person_type': 'yt_kzp', 'resident': 0, 'firm_ids': ['1'], 'region_id': '225', 'pay_policy_ids': [102]},
    {'person_type': 'yt_kzu', 'resident': 0, 'firm_ids': ['1'], 'region_id': '225', 'pay_policy_ids': [102]},
    {'person_type': 'pu', 'resident': 1, 'firm_ids': ['2'], 'region_id': '187', 'pay_policy_ids': [210]},
    {'person_type': 'ua', 'resident': 1, 'firm_ids': ['2'], 'region_id': '187', 'pay_policy_ids': [210]},
    {'person_type': 'kzu', 'resident': 1, 'firm_ids': ['3'], 'region_id': '159', 'pay_policy_ids': [310]},
    {'person_type': 'kzp', 'resident': 1, 'firm_ids': ['3'], 'region_id': '159', 'pay_policy_ids': [310]},
    {'person_type': 'usu', 'resident': 1, 'firm_ids': ['4'], 'region_id': '84', 'pay_policy_ids': [410]},
    {'person_type': 'usp', 'resident': 1, 'firm_ids': ['4'], 'region_id': '84', 'pay_policy_ids': [410]},
    {'person_type': 'byu', 'resident': 1, 'firm_ids': ['5'], 'region_id': '149', 'pay_policy_ids': [510]},
    {'person_type': 'byp', 'resident': 1, 'firm_ids': ['5'], 'region_id': '149', 'pay_policy_ids': [510]},
    {'person_type': 'sw_ur', 'resident': 1, 'firm_ids': ['7'], 'region_id': '126', 'pay_policy_ids': [710]},
    {'person_type': 'sw_yt', 'resident': 0, 'firm_ids': ['7'], 'region_id': '126', 'pay_policy_ids': [700]},
    {'person_type': 'sw_ytph', 'resident': 0, 'firm_ids': ['7'], 'region_id': '126', 'pay_policy_ids': [700]},
    {'person_type': 'sw_ph', 'resident': 1, 'firm_ids': ['7'], 'region_id': '126', 'pay_policy_ids': [710]},
    {'person_type': 'by_ytph', 'resident': 0, 'firm_ids': ['7'], 'region_id': '126', 'pay_policy_ids': [701]},
    {'person_type': 'tru', 'resident': 1, 'firm_ids': ['8'], 'region_id': '983', 'pay_policy_ids': [810]},
    {'person_type': 'trp', 'resident': 1, 'firm_ids': ['8'], 'region_id': '983', 'pay_policy_ids': [810]},
    {'person_type': 'ur_autoru', 'resident': 1, 'firm_ids': ['10'], 'region_id': '225', 'pay_policy_ids': [1010]},
    {'person_type': 'ph_autoru', 'resident': 1, 'firm_ids': ['10'], 'region_id': '225', 'pay_policy_ids': [1010]}
]

person_categories_list_who_may_be_created_ids = [x['person_type'] for x in person_categories_list_who_may_be_created]


def create_person_on_client_with_region(person_type, region_id):
    try:
        client_id = steps.ClientSteps.create({'REGION_ID': region_id})
    except Exception, exc:
        print exc
        if 'INVALID_PARAM' == steps.CommonSteps.get_exception_code(exc):
            client_id = 'INVALID_PARAM'
    try:
        person_id = steps.PersonSteps.create(client_id, person_type)
    except Exception, exc:
        if 'Error: DatabaseError' == steps.CommonSteps.get_exception_code(exc):
            person_id = 'Error: DatabaseError'
        elif 'PERSON_TYPE_MISMATCH' == steps.CommonSteps.get_exception_code(exc):
            person_id = 'PERSON_TYPE_MISMATCH'
    print client_id, person_id
    return client_id, person_id


@pytest.mark.parametrize('region_id', region_params_list
    , ids=region_params_list_ids)
@pytest.mark.parametrize('person_category', person_categories_list_who_may_be_created
    , ids=person_categories_list_who_may_be_created_ids)
def test_create_person_on_client_with_region(person_category, region_id):
    person_type = person_category['person_type']
    client_id, person_id = create_person_on_client_with_region(person_type, region_id)
    if client_id == 'INVALID_PARAM':
        assert region_id in ['83']
    else:
        client = db.get_client_by_id(client_id)[0]
        utils.check_that(client['region_id'], equal_to(int(region_id)))
    if person_id == 'Error: DatabaseError':
        assert region_id in ['83']
    else:
        person = db.get_persons_by_client(client_id)[0]
        utils.check_that(person['id'], equal_to(int(person_id)))


def create_second_person_on_client(first_person_category, second_person_category):
    client_id = steps.ClientSteps.create()
    first_person_id = steps.PersonSteps.create(client_id, first_person_category)
    try:
        second_person_id = steps.PersonSteps.create(client_id, second_person_category)
    except Exception, exc:
        if steps.CommonSteps.get_exception_code(exc) == 'PERSON_TYPE_MISMATCH':
            second_person_id = 'PERSON_TYPE_MISMATCH'
    return client_id, first_person_id, second_person_id


@pytest.mark.parametrize('first_person_category_params', person_categories_list_who_may_be_created
    , ids=person_categories_list_who_may_be_created_ids)
@pytest.mark.parametrize('second_person_category_params', person_categories_list_who_may_be_created
    , ids=person_categories_list_who_may_be_created_ids)
def test_create_second_person_on_client(first_person_category_params, second_person_category_params):
    first_person_category = first_person_category_params['person_type']
    second_person_category = second_person_category_params['person_type']

    common_firms = bool(set(first_person_category_params['firm_ids']).intersection(second_person_category_params['firm_ids']))
    both_residents = (first_person_category_params['resident'] == second_person_category_params['resident'] == True)
    both_non_residents = (first_person_category_params['resident'] == second_person_category_params['resident'] == False)

    client_id, first_person_id, second_person_id = create_second_person_on_client(first_person_category, second_person_category)
    if second_person_id == 'PERSON_TYPE_MISMATCH':
        # если с одного региона, то не с разными статусами резидентства ИЛИ оба не нерезиденты
        if first_person_category_params['region_id'] == second_person_category_params['region_id']:
            assert first_person_category_params['resident'] <> second_person_category_params['resident'] or both_non_residents
        else:
            # если с разных регионов, то есть общие фирмы и не оба резиденты ИЛИ нет общих фирм
            assert (common_firms is False) and not (common_firms and both_residents)

    else:
        assert db.get_person_by_id(first_person_id)[0]['resident'] == db.get_person_by_id(second_person_id)[0]['resident']


def create_person_on_client_with_region(region_id, person_category):
    client_id = steps.ClientSteps.create({'REGION_ID': region_id})
    try:
        person_id = steps.PersonSteps.create(client_id, person_category)
    except Exception, exc:
        print exc
        if steps.CommonSteps.get_exception_code(exc) == 'PERSON_TYPE_MISMATCH':
            person_id = 'PERSON_TYPE_MISMATCH'
    return person_id

@pytest.mark.parametrize('region_param', region_params_list
    , ids=region_params_list_ids)
@pytest.mark.parametrize('person_category_param', person_categories_list_who_may_be_created
    , ids=person_categories_list_who_may_be_created_ids)
def test_create_person_on_client_with_region(region_param, person_category_param):
    region_id = region_param['region_id']
    person_category = person_category_param['person_type']
    common_pay_policy = set(region_param['pay_policy_ids']).intersection(person_category_param['pay_policy_ids'])
    person_id = create_person_on_client_with_region(region_id, person_category)
    if person_id == 'PERSON_TYPE_MISMATCH':
        if person_category_param['resident'] == 0:
            # не создастся плательщик-нерезидент, если клиент из региона резиденства или нет общих платежных политик
            assert bool(region_id == person_category_param['region_id']) or (bool(common_pay_policy) is False)
        else:
            # не создастся плательщик-резидент, если клиент не из региона резиденства
            assert region_id <> person_category_param['region_id']
    else:
        assert bool(common_pay_policy) is True

def change_region_on_client_with_person(person_category, region_id):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, person_category)
    try:
        client_id = steps.ClientSteps.create({'REGION_ID': region_id})
    except Exception, exc:
        print exc
        if steps.CommonSteps.get_exception_code(exc) == 'PERSON_TYPE_MISMATCH':
            client_id = 'PERSON_TYPE_MISMATCH'
    return client_id

@pytest.mark.parametrize('region_param', region_params_list
    , ids=region_params_list_ids)
@pytest.mark.parametrize('person_category_param', person_categories_list_who_may_be_created
    , ids=person_categories_list_who_may_be_created_ids)
def test_change_region_on_client_with_person(region_param, person_category_param):
    person_category = person_category_param['person_type']
    region_id = region_param['region_id']
    client_id = change_region_on_client_with_person(person_category, region_id)

    client = db.get_client_by_id(client_id)[0]
    utils.check_that(client['region_id'], equal_to(int(region_id)))



def change_region_on_client_with_person_and_region(person_category, first_region_id, second_region_id):
    client_id = steps.ClientSteps.create({'REGION_ID': first_region_id})
    person_id = steps.PersonSteps.create(client_id, person_category)
    try:
        client_id = steps.ClientSteps.create({'REGION_ID': region_id})
    except Exception, exc:
        print exc
        if steps.CommonSteps.get_exception_code(exc) == 'PERSON_TYPE_MISMATCH':
            client_id = 'PERSON_TYPE_MISMATCH'
    return client_id

@pytest.mark.parametrize('first_region_param', region_params_list
    , ids=region_params_list_ids)
@pytest.mark.parametrize('second_region_param', region_params_list
    , ids=region_params_list_ids)
@pytest.mark.parametrize('person_category_param', person_categories_list_who_may_be_created
    , ids=person_categories_list_who_may_be_created_ids)
def test_change_region_on_client_with_person_and_region(person_category_param, first_region_param, second_region_param):
    person_category = person_category_param['person_type']
    first_region_id = first_region_param['region_id']
    second_region_id = second_region_param['region_id']
    client_id = change_region_on_client_with_person_and_region(person_category, first_region_id, second_region_id)




if __name__ == "__main__":
    # pytest.main("person_vs_client.py -v")
    # pytest.main("person_vs_client.py -vk 'test_create_second_person_on_client'")
    # pytest.main("person_vs_client.py -vk 'test_create_person_on_client_with_region'")
    pytest.main("person_vs_client.py -vk 'test_change_region_on_client_with_person'")
