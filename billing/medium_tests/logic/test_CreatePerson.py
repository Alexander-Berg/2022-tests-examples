# -*- coding: utf-8 -*-

from xmlrpclib import Fault

import mock
import pytest
import hamcrest as hm

from balance.mapper import Person, PersonCategory
from balance.simulation import States
from balance.constants import (
    PersonCategoryCodes,
    BankType,
)
from tests.balance_tests.person.person_defaults import (
    mandatory_fields_map, person_validator_spendable_mandatory_fields_map)
from tests import object_builder as ob
from tests.tutils import get_exception_code


@pytest.fixture(name='client')
def create_client(session, **attrs):
    res = ob.ClientBuilder(**attrs).build(session).obj
    return res


@pytest.fixture(name='person')
def create_person(session, **attrs):
    res = ob.PersonBuilder(**attrs).build(session).obj
    return res


@pytest.mark.parametrize(
    'person_type',
    mandatory_fields_map.keys()
)
def test_create_person_xmlrpc(session, xmlrpcserver, client, person_type):
    base_params = {'client_id': client.id, 'type': person_type}
    base_params.update(mandatory_fields_map[person_type])
    with mock.patch.object(client, 'get_creatable_person_categories', return_value={
        session.query(PersonCategory).filter(PersonCategory.category == person_type).one_or_none() or
        ob.PersonCategoryBuilder.construct(session, category=person_type)
    }):
        person_id = xmlrpcserver.CreatePerson(session.oper_id, base_params)
    person = session.query(Person).get(person_id)
    assert person.client == client
    assert person.type == person_type


@pytest.mark.parametrize('person_type', ['ph'])
def test_create_person_validate(session, xmlrpcserver, client, person_type):
    base_params = {'client_id': client.id, 'type': person_type}
    base_params.update(person_validator_spendable_mandatory_fields_map[person_type])
    result = xmlrpcserver.CreatePersonValidate(0, base_params)
    assert result[0] == 0
    assert '<ValidatorResult success validation' in result[1]

    with pytest.raises(Fault) as exc_info:
        del base_params['pfr']
        xmlrpcserver.CreatePersonValidate(0, base_params)
    msg = 'Invalid parameter for function: <ValidatorResult failed validation'
    assert msg in get_exception_code(exc_info.value, 'msg')


def test_create_person_invalid_bankprops(session, xmlrpcserver, client):
    params = {'client_id': client.id, 'type': 'ph', 'invalid_bankprops': '1'}
    params.update(mandatory_fields_map['ph'])
    person_id = xmlrpcserver.CreatePerson(0, params)
    person = session.query(Person).get(person_id)
    assert bool(person.invalid_bankprops) == True, "При создании плательщика не проставился invalid_bankprops"


def test_update_person_invalid_bankprops(session, xmlrpcserver, client):
    params = {'client_id': client.id, 'type': 'ph'}
    params.update(mandatory_fields_map['ph'])
    person_id = xmlrpcserver.CreatePerson(0, params)
    person = session.query(Person).get(person_id)
    assert bool(person.invalid_bankprops) == False, "Про создании плательщика без invalid_bankprops он проставлен"

    params.update({'invalid_bankprops': '1', 'person_id': person_id})
    person_id = xmlrpcserver.CreatePerson(0, params)
    session.refresh(person)
    assert bool(person.invalid_bankprops) == True, "При обновлении плательщика не проставился invalid_bankprops"

    del params['invalid_bankprops']
    person_id = xmlrpcserver.CreatePerson(0, params)
    assert bool(person.invalid_bankprops) == True, "При обновлении плательщика без invalid_bankprops он сбросился"


def test_create_person_simulate_with_errors_mandatory_fields(xmlrpcserver, client):
    base_params = {'client_id': client.id, 'simulate': '1', 'type': 'ur'}
    base_params.update(mandatory_fields_map['ur'])
    del base_params['phone']
    del base_params['longname']
    result = xmlrpcserver.CreatePerson(0, base_params)

    assert result['state'] == States.STATE_CRIT
    hm.assert_that(
        result['errors'],
        hm.contains_inanyorder(
            hm.has_entries({'description': "Missing mandatory person field 'phone' for person type ur",
                            'err_code': -1,
                            'field': 'phone'}),
            hm.has_entries({'description': "Missing mandatory person field 'longname' for person type ur",
                            'err_code': -1,
                            'field': 'longname'}),
        ),
    )


def test_create_person_simulate_wo_errors(xmlrpcserver, client):
    base_params = {'client_id': client.id, 'type': 'ur', 'simulate': '1'}
    base_params.update(mandatory_fields_map['ur'])
    assert client.persons == []
    result = xmlrpcserver.CreatePerson(0, base_params)
    assert result['state'] == States.STATE_OK
    assert result['errors'] == []
    assert client.persons == []


def test_create_person_xmlprc_non_valid_person_id(xmlrpcserver, session, client):
    base_params = {'client_id': client.id, 'type': 'ph', 'person_id': -1}
    base_params.update(mandatory_fields_map['ph'])
    person_id = xmlrpcserver.CreatePerson(0, base_params)
    # Убеждаемся, что person_id валидный и плательщик существует
    session.query(Person).getone(person_id)


def test_update_person_xmlprc(session, xmlrpcserver):
    initial_type = 'ph'
    person = create_person(session, type=initial_type, lname='test_lname')
    initial_client_id = person.client.id

    base_params = {'client_id': initial_client_id, 'type': initial_type, 'person_id': person.id}

    new_lname = 'another_lname'
    base_params.update({'lname': new_lname})

    person_id = xmlrpcserver.CreatePerson(0, base_params)
    assert person_id == person.id

    # refresh person instance
    person = session.query(Person).get(person_id)

    assert person.client_id == initial_client_id
    assert person.type == initial_type
    assert person.lname == new_lname


@pytest.mark.parametrize('params, expected', [
    ({}, {'missing_param': 'client_id'}),
    ({'client_id': 0}, {'missing_param': 'type'})
])
def test_create_person_mandatory_fields(xmlrpcserver, params, expected):
    with pytest.raises(Fault) as exc_info:
        xmlrpcserver.CreatePerson(0, params)
    msg = 'Invalid parameter for function: Missing {} from client_hash'
    assert get_exception_code(exc_info.value, 'msg') == msg.format(expected['missing_param'])


@mock.patch('balance.mapper.clients.Client.get_creatable_person_categories')
def test_create_person_wrong_type(get_creatable_person_categories_mock, xmlrpcserver, session, client):
    creatable_categories = session.query(PersonCategory). \
        filter(PersonCategory.category.in_({'ur', 'ph'}))
    get_creatable_person_categories_mock.return_value = creatable_categories
    base_params = {'client_id': client.id, 'type': 'yt'}
    base_params.update(mandatory_fields_map['yt'])
    with pytest.raises(Exception) as exc_info:
        xmlrpcserver.CreatePerson(0, base_params)
    get_creatable_person_categories_mock.assert_called_once()
    assert get_exception_code(exc=exc_info.value) == 'INVALID_PERSON_TYPE'
    assert (get_exception_code(exc=exc_info.value, tag_name='msg') ==
            'Cannot create person with type = yt, available types: ur, ph')


@pytest.mark.single_account
@mock.patch('balance.mapper.clients.Client.get_creatable_person_categories')
def test_get_creatable_person_categories_not_called_on_update(
    get_creatable_person_categories_mock, xmlrpcserver, session):
    """
    Регрессия. После добавления логики ЕЛС в get_creatable_person_categories
    пропадала возможность редактировать плательщиков через CreatePerson.
    """
    client = create_client(session, with_single_account=True)
    person = create_person(session, client=client, type=PersonCategoryCodes.russia_resident_individual)
    memo = 'test memo'
    assert getattr(person, 'memo', '') != memo
    xmlrpcserver.CreatePerson(0, {'person_id': person.id, 'client_id': person.client_id,
                                  'type': person.type, 'memo': memo})
    assert getattr(person, 'memo', '') == memo
    get_creatable_person_categories_mock.assert_not_called()


def test_create_person_wrong_person(xmlrpcserver, client):
    non_existing_person_id = ob.get_big_number()
    base_params = {'client_id': client.id, 'type': 'ph', 'person_id': non_existing_person_id}
    base_params.update(mandatory_fields_map['ph'])
    with pytest.raises(Exception) as exc_info:
        xmlrpcserver.CreatePerson(0, base_params)
    assert get_exception_code(exc=exc_info.value) == 'PERSON_NOT_FOUND'


def test_client_and_person_mismatch(xmlrpcserver, person, client):
    base_params = {'client_id': client.id, 'type': person.type, 'person_id': person.id}
    base_params.update(mandatory_fields_map['ph'])
    with pytest.raises(Exception) as exc_info:
        xmlrpcserver.CreatePerson(0, base_params)
    assert get_exception_code(exc=exc_info.value) == 'CLIENT_ID_MISMATCH'


def test_person_type_mismatch(session, xmlrpcserver):
    person = create_person(session, type='ph')
    base_params = {'client_id': person.client.id, 'type': 'ur', 'person_id': person.id}
    base_params.update(mandatory_fields_map['ph'])
    with pytest.raises(Exception) as exc_info:
        xmlrpcserver.CreatePerson(0, base_params)
    assert get_exception_code(exc=exc_info.value) == 'PERSON_TYPE_MISMATCH'
    assert get_exception_code(exc=exc_info.value,
                              tag_name='msg') == 'Person type (ph) cannot be changed to ur'


@pytest.mark.parametrize('is_partner', ['0', '1'])
@pytest.mark.parametrize('add_params', [{'mname': u'Хренович'}, {}])
def test_ur_selfemployed(session, xmlrpcserver, is_partner, add_params):
    client = create_client(session)
    params = dict(is_partner=is_partner,
                  client_id=client.id,
                  type='ur',
                  ownership_type='SELFEMPLOYED',
                  lname=u'Хренов', fname=u'Хрен',
                  phone=u'+7 (495) 666-0-666',
                  legaladdress=u'127287, г Москва, Старый Петровско-Разумовский проезд, д 1/23, стр 1',
                  email='robot@robot.com',
                  inn='352519189263',
                  account='40802810000000481224', bik='044525974',
                  invalid_bankprops='0')
    params.update(add_params)
    person_id = xmlrpcserver.CreatePerson(0, params)
    person = session.query(Person).get(person_id)
    assert person.ownership_type == 'SELFEMPLOYED'
    assert person.ownership_type_ui == 'SELFEMPLOYED'

    expected_name = ' '.join(n for n in (person.lname, person.fname, person.mname) if n)
    assert person.name == expected_name
    assert person.longname == expected_name


@pytest.mark.parametrize(
    'bank_type, field_name, field_value',
    [
        (BankType.SBERBANK, 'person_account', '1234567'),
        (BankType.OTHER_BANK, 'person_account', '1234567'),
        (BankType.YANDEX_MONEY, 'yamoney_wallet', '1234567'),
        (BankType.WEBMONEY, 'webmoney_wallet', '1234567'),
        (BankType.PAYPAL, 'paypal_wallet', '1234567'),
        (BankType.PAYONEER, 'payoneer_wallet', '1234567'),
        (BankType.PRIVATBANK, 'person_account', '1234567'),
    ]
)
def test_bank_types_ph(session, xmlrpcserver, bank_type, field_name, field_value):
    client = create_client(session)
    field_values = dict(
        person_account=666,
        yamoney_wallet=666,
        webmoney_wallet=666,
        paypal_wallet=666,
        payoneer_wallet=666,
        pingpong_wallet=666,
    )
    person = ob.PersonBuilder.construct(
        session,
        client=client,
        type='ph',
        is_partner=1,
        **field_values
    )

    params = dict(
        client_id=str(client.id),
        person_id=str(person.id),
        type='ph',
        is_partner='1',
        bank_type=str(bank_type),
    )
    params[field_name] = field_value
    xmlrpcserver.CreatePerson(0, params)
    session.expire_all()

    req_fields = {k: field_value if k == field_name else None for k in field_values}
    assert {k: getattr(person, k, None) for k in field_values} == req_fields


@pytest.mark.parametrize(
    'bank_type, set_fields',
    [
        pytest.param(
            BankType.NOT_SELECTED,
            {'swift': 'RZBMRUMMXXX', 'corr_swift': 'SABRRUMMEA1', 'other': 'rq3reere', 'local_other': 'qqqq'},
            id='not_selected'
        ),
        pytest.param(
            BankType.OTHER_BANK,
            {'swift': 'RZBMRUMMXXX', 'corr_swift': 'SABRRUMMEA1', 'other': 'rq3reere', 'local_other': 'qqqq'},
            id='other_bank'
        ),
        pytest.param(BankType.WEBMONEY, {'webmoney_wallet': '1234567'}, id='webmoney'),
        pytest.param(BankType.PAYPAL, {'paypal_wallet': '1234567'}, id='paypal'),
        pytest.param(BankType.PAYONEER, {'payoneer_wallet': '1234567'}, id='payoneer'),
        pytest.param(BankType.PINGPONG, {'pingpong_wallet': '1234567'}, id='pingpong'),
    ]
)
def test_bank_types_hk_yt(session, xmlrpcserver, bank_type, set_fields):
    client = create_client(session)
    field_values = dict(
        yamoney_wallet=666,
        webmoney_wallet=666,
        paypal_wallet=666,
        payoneer_wallet=666,
        pingpong_wallet=666,
        swift=666,
        corr_swift=666,
        other=666,
        local_other=666,
    )
    person = ob.PersonBuilder.construct(
        session,
        client=client,
        type='hk_yt',
        is_partner=1,
        **field_values
    )

    params = dict(
        client_id=str(client.id),
        person_id=str(person.id),
        type='hk_yt',
        is_partner='1',
        bank_type=str(bank_type),
    )
    params.update(set_fields)

    # todo-igogor удалить
    import httpretty
    import logging
    logging.debug("igogor-debug httpretty.HTTPretty._entries = %r" % (httpretty.HTTPretty._entries,))
    xmlrpcserver.CreatePerson(0, params)
    session.expire_all()

    req_fields = {k: set_fields.get(k, None) for k in field_values}
    assert {k: getattr(person, k, None) for k in field_values} == req_fields


def test_create_person_api_version(session, xmlrpcserver, client):
    # set config
    ob.ConfigBuilder.construct(session, item='CREATE_PERSON_VERSION', value_num=1)

    # call CreatePerson
    person_type = 'ur'
    params = mandatory_fields_map[person_type].copy()
    params['type'] = person_type
    params['client_id'] = client.id
    person_id = xmlrpcserver.CreatePerson(0, params)

    person = session.query(Person).get(person_id)
    assert person.api_version == u'1', u"С включенным конфигом версия должна быть 1"


def test_create_person_validator_failed(session, xmlrpcserver, client):
    # set config
    ob.ConfigBuilder.construct(session, item='CREATE_PERSON_VERSION', value_num=1)

    # call CreatePerson
    person_type = 'ur'
    params = mandatory_fields_map[person_type].copy()
    params['type'] = person_type
    params['client_id'] = client.id
    if 'country_id' in params:
        del params['country_id']
    with pytest.raises(Fault):
        xmlrpcserver.CreatePerson(0, params)


def test_create_person_validator_success(session, xmlrpcserver, client):
    # set config
    ob.ConfigBuilder.construct(session, item='CREATE_PERSON_VERSION', value_num=0)

    # call CreatePerson
    person_type = 'ur'
    params = mandatory_fields_map[person_type].copy()
    params['type'] = person_type
    params['client_id'] = client.id
    params['country_id'] = 666
    params['legal_address_postcode'] = 123456
    xmlrpcserver.CreatePerson(0, params)


def test_create_person_validator_disabled(session, xmlrpcserver, client):
    # set config
    ob.ConfigBuilder.construct(session, item='CREATE_PERSON_VERSION', value_num=0)

    # call CreatePerson
    person_type = 'ur'
    params = mandatory_fields_map[person_type].copy()
    params['type'] = person_type
    params['client_id'] = client.id
    xmlrpcserver.CreatePerson(0, params)


@pytest.mark.parametrize('is_partner, ptype',
                         [(0, 'ur'), (1, 'ur'), (1, 'ph')],
                         ids=['NOT_PARTNER_UR', 'PARTNER_UR', 'PARTNER_PH'])
@pytest.mark.parametrize('bank_account_params, expected_error', [
    pytest.param({'bik': '044525974', 'account': '40802810000000481224'},
                 None,
                 id='COMMON_CORRECT'),
    pytest.param({'bik': '014442501', 'account': '0' * 20},
                 None,
                 id='UTRA_CORRECT'),
    pytest.param({'bik': '014442501', 'account': '0' * 19},
                 "Account 0000000000000000000 doesn't match bank with BIK=014442501",
                 id='UTRA_SMALL'),
    pytest.param({'bik': '014442501', 'account': '0' * 21},
                 "Account 000000000000000000000 doesn't match bank with BIK=014442501",
                 id='UTRA_LARGE'),
    pytest.param({'bik': '014442501', 'account': '1' * 20},
                 "Account 11111111111111111111 doesn't match bank with BIK=014442501",
                 id='UTRA_NO_LEADING_ZERO'),
])
def test_ru_account_validation(session, xmlrpcserver, is_partner, ptype, bank_account_params, expected_error):
    client = create_client(session)
    params = {
        'client_id': client.id,
        'is-partner': is_partner,
        'type': ptype,
    }
    # key: (is_partner, ptype)
    params_template = {
        (0, 'ur'): mandatory_fields_map['ur'],
        (1, 'ur'): mandatory_fields_map['ur'],
        (1, 'ph'): person_validator_spendable_mandatory_fields_map['ph'],
    }
    params.update(params_template[(is_partner, ptype)])
    params.update(bank_account_params)
    try:
        person_id = xmlrpcserver.CreatePerson(0, params)
    except Exception as e:
        assert expected_error and expected_error in str(e), (expected_error, str(e))
    else:
        assert not expected_error, expected_error


@pytest.mark.parametrize(
    'person_type',
    [
        'ur', 'ph', 'yt_kzp', 'ur_ytkz',
        'am_np', 'byu', 'eu_yt', 'fr_ur', 'hk_ur', 'kg_ur'
    ]
)
def test_try_create_inapplicable_person_with_purchase_order(session, xmlrpcserver, client, person_type):
    params = {
        'client_id': client.id,
        'type': person_type,
        'purchase_order': '1!2@#$;,asd.('
    }
    params.update(mandatory_fields_map[person_type])

    with mock.patch.object(client, 'get_creatable_person_categories', return_value={
        session.query(PersonCategory).filter(PersonCategory.category == person_type).one_or_none() or
        ob.PersonCategoryBuilder.construct(session, category=person_type)
    }), pytest.raises(Fault) as exc_info:
        xmlrpcserver.CreatePerson(session.oper_id, params)

    msg = 'Purchase order can only be set for the following categories: '
    assert get_exception_code(exc_info.value, 'msg').startswith(msg)


@pytest.mark.parametrize(
    'person_type',
    [
        'usp', 'usu', 'us_ytph', 'us_yt', 'sw_ytph',
        'sw_ph', 'sw_ur', 'sw_yt', 'by_ytph'
    ]
)
def test_create_person_with_purchase_order(session, xmlrpcserver, client, person_type):
    params = {
        'client_id': client.id,
        'type': person_type,
        'purchase_order': u'1!2@#$;,asd.(фывцй'
    }
    params.update(mandatory_fields_map[person_type])

    with mock.patch.object(client, 'get_creatable_person_categories', return_value={
        session.query(PersonCategory).filter(PersonCategory.category == person_type).one_or_none() or
        ob.PersonCategoryBuilder.construct(session, category=person_type)
    }):
        person_id = xmlrpcserver.CreatePerson(session.oper_id, params)

    person = session.query(Person).get(person_id)
    assert person.client == client
    assert person.type == person_type
    assert person.purchase_order == params['purchase_order']


@pytest.mark.parametrize('additional_params, missing_field',
                         [
                             ({}, "postcode"),
                             ({"postsuffix": 'а/я', "fias-guid": '23'}, 'postcode'),
                             ({"postsuffix": 'а/я', "postcode": 123456}, 'fias-guid'),
                             ({"fias-guid": "45141195", "postcode": 123456}, 'postsuffix'),
                             ({"fias-guid": "45141195-0a4b-49a5-813c-90fc0089ff6b",
                               "postcode": 123456,
                               "postsuffix": 'а/я'}, None)
                         ])
def test_create_person_with_postbox_fias(session, medium_xmlrpc, client, additional_params, missing_field):
    params = mandatory_fields_map['ur'].copy()
    del params['postaddress']
    del params['postcode']
    params["client_id"] = client.id
    params['is-postbox'] = 1
    params['type'] = 'ur'

    params.update(additional_params)

    if not missing_field:
        medium_xmlrpc.CreatePerson(session.oper_id, params)
    else:
        with pytest.raises(Fault) as exc_info:
            medium_xmlrpc.CreatePerson(session.oper_id, params)
        msg = "Missing mandatory person field '{}' for person type ur".format(missing_field)
        assert msg in get_exception_code(exc_info.value, 'msg')


@pytest.mark.parametrize('additonal_params', [{}, {'is-postbox': 0}])
def test_create_person_with_not_fias_postbox(session, medium_xmlrpc, client, additonal_params):
    params = mandatory_fields_map['ur'].copy()
    del params['postaddress']

    params.update({"client_id": client.id,
                   'type': 'ur',
                   'postsuffix': 'а/я'})

    params.update(additonal_params)

    medium_xmlrpc.CreatePerson(session.oper_id, params)


@pytest.mark.parametrize('person_type', [
    'ur', 'ph',
    'usu'
])
@pytest.mark.parametrize('additional_params, missing_field',
                         [
                             ({}, "postcode"),
                             ({"postsuffix": 'а/я', "fias-guid": '23'}, 'postcode'),
                             ({"postsuffix": 'а/я', "postcode": 123456}, 'fias-guid'),
                             ({"fias-guid": "45141195", "postcode": 123456}, 'postsuffix'),
                             ({"fias-guid": "45141195-0a4b-49a5-813c-90fc0089ff6b",
                               "postcode": 123456,
                               "postsuffix": 'а/я',
                               "other": '322'}, None)
                         ])
def test_create_person_with_postbox_fias_partner(session, medium_xmlrpc, client, additional_params, missing_field,
                                                 person_type):
    """юрики и физики партнеры при взведенном is-postbox требуют индекс, фиас и а/я """

    params = {"client_id": client.id,
              'is-postbox': 1,
              'is-partner': 1,
              'type': person_type}

    params.update(additional_params)

    if not missing_field:
        person_id = medium_xmlrpc.CreatePerson(session.oper_id, params)

        person = session.query(Person).get(person_id)
        assert person.is_postbox == 1

    else:
        with pytest.raises(Fault) as exc_info:
            medium_xmlrpc.CreatePerson(session.oper_id, params)
        msg = "Missing mandatory person field '{}' for person type {}".format(missing_field, person_type)
        assert msg in get_exception_code(exc_info.value, 'msg')


@pytest.mark.parametrize('kpp, flag, ptype', [
    ('11A2', 1, 'ur'),
    ('1112223399', 1, 'ur'),
    ('111A22333', 1, 'ur'),
    ('666999666', 1, 'ur'),
    ('AAA666AAA', 0, 'ur'),
    ('AA6AAA', 0, 'ur'),
    ('', 1, 'yt'),
])
def test_person_check_kpp_length(session, xmlrpcserver, client, kpp, flag, ptype):
    ob.ConfigBuilder.construct(session, item='CHECK_PERSON_KPP', value_num=flag)
    base_params = {'client_id': client.id, 'type': ptype}
    base_params.update(mandatory_fields_map[ptype])
    if kpp:
        base_params['kpp'] = kpp
    try:
        person_id = xmlrpcserver.CreatePerson(session.oper_id, base_params)
        person = session.query(Person).get(person_id)
    except Exception as e:
        assert u'KPP must be 9 characters long and contains only digits' in str(e)
    else:
        if kpp:
            if flag:
                assert person.kpp == '666999666'
            else:
                assert person.kpp in ['AAA666AAA', 'AA6AAA']
        else:
            assert not person.kpp


@pytest.mark.parametrize('pfr', [
    '111-222-333 99',
    '111  222-33399',
    '11122233399',
    '1d112223339',
    '111222222233399'
])
def test_person_normalize_pfr_number(session, xmlrpcserver, client, pfr):
    person_type = 'ph'
    ob.ConfigBuilder.construct(session, item='PERSON_CHECK_AND_NORMALIZE_PFR', value_num=1)
    base_params = {'client_id': client.id, 'type': person_type}
    base_params.update(mandatory_fields_map[person_type])
    base_params['pfr'] = pfr
    try:
        person_id = xmlrpcserver.CreatePerson(session.oper_id, base_params)
        person = session.query(Person).get(person_id)
        assert person.pfr == '111-222-333 99'
    except Exception as e:
        assert u'PFR number should have 11 digits, allowed separators - space, dash' in str(e)
