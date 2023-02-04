# -*- coding: utf-8 -*-
import mock
import pytest
import refsclient

from balance.constants import PersonCategoryCodes
from balance import exc, person as person_lib, constants
from balance.mapper import Person, OverdraftParams, BankIntClass

from muzzle.api import person as person_api

from tests.balance_tests.person.person_defaults import mandatory_fields_map, swift_examples
from tests.balance_tests.person.person_common import (
    create_client, create_person,
    non_existing_bik, existing_bik,
    create_contract, create_restricted_person_param
)

pytestmark = [
    pytest.mark.set_person,
]


def test_hide_person(session, person):
    person.hidden = 0
    person_api.hide_person(session, person.id, is_admin=True)
    assert person.hidden == 1


def test_1(session):
    with pytest.raises(exc.PERSON_NOT_FOUND):
        person_api.hide_person(session, -1, True)
    with pytest.raises(exc.MISSING_MANDATORY_PERSON_FIELD):
        person_lib.change_person(session, -1, -1, "ph", "set", {})
    req = dict((mf, "-") for mf in person_lib.mandatory_fields["ph"])
    with pytest.raises(exc.PERSON_NOT_FOUND):
        person_lib.change_person(session, -1, 1000000000, "", "set", req)
    with pytest.raises(exc.CLIENT_NOT_FOUND):
        person_lib.change_person(session, -1, -1, "ph", "set", req)


def test_missing_mandatory_field_when_update(session):
    person_params = mandatory_fields_map['ur'].copy()
    person = create_person(session=session, phone=person_params['phone'],
                           type='ur', inn=person_params['inn'])
    phone = person_params.pop('phone')
    person_lib.change_person(session=session, client_id=person.client.id,
                             person_id=person.id, req=person_params, type='ur', mode=None)
    assert person.phone == phone


def test_using_command_characters_in_field(session):
    person_params = mandatory_fields_map['ur'].copy()
    person = create_person(session=session, type='ur', inn=person_params['inn'])
    person_params.update({'name': u'А он не\x02прост'})
    with pytest.raises(exc.INCORRECT_CHARACTER) as exc_info:
        person_lib.change_person(
            session=session, client_id=person.client.id,
            person_id=person.id, req=person_params,
            type='ur', mode=None
        )
    assert exc_info.value._field_name == 'name'
    assert exc_info.value.msg == u"Control character in name"


@pytest.mark.parametrize('bik_func, account_func, expected', [
    (non_existing_bik, lambda: ' ',
     {'exc_type': exc.WRONG_BIK, 'msg': u"Bank with BIK={bik} not found in DB"}),

    (existing_bik, lambda: '1232',
     {'exc_type': exc.WRONG_ACCOUNT, 'msg': u"Account {account} doesn't match bank with BIK={bik}"}),

    (lambda x: None, lambda: '1',
     {'exc_type': exc.INVALID_PARAM,
      'msg': u"Invalid parameter for function: bik is required when the account value is passed"}),

    (existing_bik, lambda: '1a',
     {'exc_type': exc.INVALID_PARAM,
      'msg': u"Invalid parameter for function: account value '{account}' must be a number"}),

])
def test_set_person_bik_account_checks(session, bik_func, account_func, expected):
    client = create_client(session)
    bik, account = bik_func(session), account_func()
    person_params = mandatory_fields_map['ur'].copy()
    if bik:
        person_params['bik'] = bik
    if account:
        person_params['account'] = account
    with pytest.raises(Exception) as excinfo:
        person_lib.change_person(session=session, client_id=client.id, mode=None,
                                 req=person_params, type='ur', person_id=0)
    assert excinfo.type == expected['exc_type']
    assert excinfo.value.msg == expected['msg'].format(bik=bik, account=account)


def test_set_person_bik_account(session):
    client = create_client(session)
    person_params = mandatory_fields_map['ur'].copy()
    person_params['bik'] = '044030001'
    person_params['account'] = '40702810500000000000'
    person_id = person_lib.change_person(session=session, client_id=client.id, mode=None, req=person_params, type='ur',
                                         person_id=0).explain()
    created_person = session.query(Person).getone(person_id)
    assert created_person.bik == person_params['bik']
    assert created_person.account == person_params['account']


def test_set_person_fields_for_state_institutions(session):
    client = create_client(session)
    person_params = mandatory_fields_map['ur'].copy()
    person_params['kbk'] = '12345678901234567890'
    person_params['oktmo'] = '12345678'
    person_params['payment_purpose'] = 'Test payment'
    person_id = person_lib.change_person(session=session, client_id=client.id, mode=None, req=person_params, type='ur',
                                         person_id=0).explain()
    created_person = session.query(Person).getone(person_id)
    assert created_person.kbk == person_params['kbk']
    assert created_person.oktmo == person_params['oktmo']
    assert created_person.payment_purpose == person_params['payment_purpose']


@pytest.mark.parametrize('is_admin', [True, False])
def test_set_person_allow_archive_with_contract(session, xmlrpcserver, is_admin):
    """
    Проверка запрета архивации в КИ плательщиков с активным договором
    """
    person = create_person(session, type='ur')

    create_contract(session, person)
    session.flush()

    try:
        person_api.hide_person(session, person.id, is_admin=is_admin)
        assert is_admin
    except exc.PERSON_HAS_ACTIVE_CONTRACTS:
        assert not is_admin


@pytest.mark.parametrize('is_admin', [True, False])
def test_set_person_allow_archive_with_autooverdraft(session, xmlrpcserver, is_admin):
    """
    Проверка запрета архивации в КИ плательщиков с активным автоовердрафтом
    """
    person = create_person(session, type='ur')

    iso_currency = 'RUB'
    service_id = constants.ServiceId.DIRECT
    firm_id = constants.FirmId.YANDEX_OOO

    person.client.set_overdraft_limit(service_id, firm_id, 100, iso_currency)

    overdraft_params = OverdraftParams(
        person=person,
        client=person.client,
        service_id=service_id,
        payment_method_cc='card',
        iso_currency=iso_currency,
        client_limit=80
    )
    session.add(overdraft_params)
    session.flush()

    try:
        person_api.hide_person(session, person.id, is_admin=is_admin)
        assert is_admin
    except exc.PERSON_HAS_AUTO_OVERDRAFT:
        assert not is_admin


def test_invalid_person_type(session):
    client = create_client(session)
    with pytest.raises(exc.INVALID_PERSON_TYPE) as exc_info:
        person_lib.change_person(session=session, client_id=client.id, mode=None, req={},
                                 type='up', person_id=0)
    assert exc_info.value.msg == 'Invalid person type: up'


def test_auto_unhide_on_update(session):
    person_params = mandatory_fields_map['ur'].copy()
    person = create_person(session=session, phone=person_params['phone'],
                           type='ur', inn=person_params['inn'])
    person_lib.set_hidden(person, True)
    session.flush()
    person_lib.change_person(session=session, client_id=person.client.id,
                             person_id=person.id, req=person_params, type='ur', mode=None)
    assert not person.hidden


def check_person_id_is_none(person):
    assert person.id is None


@pytest.mark.single_account
@mock.patch('balance.person.single_account.availability.check_person_attributes',
            new=mock.Mock(side_effect=check_person_id_is_none))
def test_person_id_is_none_on_creation(session):
    """
    Убеждаемся, что при создании плательщика в момент вызова check_person_attributes
    person.id еще не заполнен, т.к. исходя из этого функция определяет выполняется ли
    создание или обновление плательщика.
    """
    client = create_client(session, with_single_account=True)
    category = PersonCategoryCodes.russia_resident_individual
    person_lib.change_person(session, client.id, person_id=None, type=category, mode=None,
                             req=mandatory_fields_map[category])


@pytest.mark.single_account
@mock.patch('balance.person.single_account.availability.check_person_attributes')
class TestCheckPersonAttributes(object):
    def test_called(self, check_person_attributes_mock, session):
        person = create_person(session, client=create_client(session, with_single_account=True))
        person_lib.change_person(session, person.client_id, person.id, type=person.type, mode=None,
                                 req=mandatory_fields_map[person.type])
        check_person_attributes_mock.assert_called_once_with(person)

    def test_not_called_on_hide(self, check_person_attributes_mock, session):
        person = create_person(session, client=create_client(session, with_single_account=True))
        person_api.hide_person(session, person.id, is_admin=True)
        check_person_attributes_mock.assert_not_called()

    def test_not_called_without_single_account(self, check_person_attributes_mock, person, session):
        person_lib.change_person(session, person.client_id, person.id, type=person.type, mode=None,
                                 req=mandatory_fields_map[person.type])
        check_person_attributes_mock.assert_not_called()


def normalize_swift(swift):
    return swift.upper() + 'X' * (11-len(swift))


exc_wrong_swift_len = exc.INVALID_SWIFT_LENGTH(
    u'SWIFT must be between 8 and 11 characters long'
)
exc_bank_not_found = exc.CLASSBANK_NOT_FOUND(
    normalize_swift(swift_examples['non_existing'])
)


def refs_bank_info(session, swift):
    swift = normalize_swift(swift)
    banks = session.query(BankIntClass).filter(
        BankIntClass.bicint == swift,
        BankIntClass.bictypeint == 'SWIFT'
    ).all()
    bank_is_branch = not swift.endswith('XXX')
    return {
        swift: {
            'addrBrStreet': bank.address if bank_is_branch else '',
            'instNameLegal': bank.name,
            'countryCode': bank.country.iso_alpha2,
            'addrOpCity': '' if bank_is_branch else bank.place,
            'addrBrCity': bank.place if bank_is_branch else '',
            'bicBranch': bank.bicint[8:] if len(bank.bicint) > 9 else '',
            'addrOpStreet': '' if bank_is_branch else bank.address,
            'bic8': bank.bicint[:8] if len(bank.bicint) > 9 else bank.bicint,
            'addrOpRegion': '' if bank_is_branch else bank.zipcode,
            'branchInfo': bank.branchname,
            'addrBrRegion': bank.zipcode if bank_is_branch else ''
        }
        for bank in banks
    }


@pytest.mark.parametrize('swift, expected_error', [
    (swift_examples['too_short'], exc_wrong_swift_len),
    (swift_examples['too_long'], exc_wrong_swift_len),
    (swift_examples['non_existing'], exc_bank_not_found),
    (swift_examples['existing'], None),
    (swift_examples['empty'], None),
])
def test_person_swift_code(session, swift, expected_error):
    client = create_client(session)
    person_params = mandatory_fields_map['byu'].copy()
    person_params['swift'] = swift

    created_person = None
    error = None

    try:
        with mock.patch(
            'refsclient.Refs',
            return_value=mock.MagicMock(
                get_ref_swift=lambda: mock.MagicMock(
                    get_bics_info=lambda *args, **kwargs: refs_bank_info(session, swift)
                )
            )
        ):
            person_id = person_lib.change_person(session=session, client_id=client.id, mode=None, req=person_params,
                                                 type='byu', person_id=0).explain()
        created_person = session.query(Person).getone(person_id)
    except Exception as error:
        pass

    assert error == expected_error

    if swift and (expected_error is None):
        assert created_person.swift == normalize_swift(swift)


wrong_pfr = exc.INVALID_KPP_VALUE(
    u'KPP must be 9 characters long and contains only digits'
)


@pytest.mark.parametrize('kpp, expected_error, flag, ptype', [
    ('11A2', wrong_pfr, 1, 'ur'),
    ('1112223399', wrong_pfr, 1, 'ur'),
    ('111A22333', wrong_pfr, 1, 'ur'),
    ('666999666', None, 1, 'ur'),
    ('AAA666AAA', None, 0, 'ur'),
    ('AA6AAA', None, 0, 'ur'),
])
def test_person_check_kpp_length(session, kpp, expected_error, flag, ptype):
    session.config.__dict__['CHECK_PERSON_KPP'] = flag
    client = create_client(session)
    person_params = mandatory_fields_map[ptype].copy()
    person_params['kpp'] = kpp

    created_person = None
    error = None

    try:
        person_id = person_lib.change_person(session=session, client_id=client.id, mode=None, req=person_params,
                                             type=ptype, person_id=0).explain()
        created_person = session.query(Person).getone(person_id)
    except Exception as error:
        pass

    assert error == expected_error

    if expected_error is None:
        if flag:
            assert created_person.kpp == '666999666'
        else:
            assert created_person.kpp in ['AAA666AAA', 'AA6AAA']


wrong_pfr = exc.INVALID_PFR_VALUE(
    u'PFR number should have 11 digits, allowed separators - space, dash'
)


@pytest.mark.parametrize('pfr, expected_error, flag', [
    ('11A  222-33399', wrong_pfr, 1),
    ('11122233', wrong_pfr, 1),
    ('111-222-333 99', None, 1),
    ('111-22233 399', None, 1),
    ('111-22233 399', None, 0),
])
def test_person_normalize_phr_number(session, pfr, expected_error, flag):
    session.config.__dict__['PERSON_CHECK_AND_NORMALIZE_PFR'] = flag
    client = create_client(session)
    person_params = mandatory_fields_map['ph'].copy()
    person_params['pfr'] = pfr

    created_person = None
    error = None

    try:
        person_id = person_lib.change_person(session=session, client_id=client.id, mode=None, req=person_params,
                                             type='ph', person_id=0).explain()
        created_person = session.query(Person).getone(person_id)
    except Exception as error:
        pass

    assert error == expected_error

    if pfr and (expected_error is None):
        if flag:
            assert created_person.pfr == '111-222-333 99'
        else:
            assert created_person.pfr == '111-22233 399'


@pytest.fixture(
    params=[
        dict(person_type='ur', attrname='country_id', value_str='1337'),
        dict(person_type='ph', attrname='email', value_str='banned_email@ya.ru'),
    ]
)
def restricted_param(session, request):
    session.config.__dict__['PERSON_CHECK_RESTRICTED_PARAMS'] = 1
    param_kwargs = request.param

    create_restricted_person_param(session, **param_kwargs)

    return param_kwargs


def test_check_restricted_person_params_when_updating(session, restricted_param):
    client = create_client(session)
    person_params = mandatory_fields_map[restricted_param['person_type']].copy()
    person = create_person(session=session, phone=person_params['phone'],
                           type=restricted_param['person_type'], inn=person_params.get('inn'), country_id='225')

    person_params[restricted_param['attrname']] = restricted_param['value_str']

    with pytest.raises(exc.INVALID_PARAM) as exc_info:
        person_lib.change_person(session=session, client_id=client.id, mode=None, req=person_params,
                                 type=restricted_param['person_type'], person_id=person.id)

    exp_message = u'Invalid parameter for function: %s person param is restricted' % restricted_param['attrname']
    assert exc_info.value.msg == exp_message

    person_after_update = session.query(Person).getone(person.id)
    assert person == person_after_update


def test_check_restricted_person_params_when_creating_new_person(session, restricted_param):
    client = create_client(session)
    person_params = mandatory_fields_map[restricted_param['person_type']].copy()

    person_params[restricted_param['attrname']] = restricted_param['value_str']

    with pytest.raises(exc.INVALID_PARAM) as exc_info:
        person_lib.change_person(session=session, client_id=client.id, mode=None, req=person_params,
                                 type=restricted_param['person_type'], person_id=None)

    exp_message = u'Invalid parameter for function: %s person param is restricted' % restricted_param['attrname']
    assert exc_info.value.msg == exp_message

    created_person = session.query(Person).filter_by(client_id=client.id).first()
    assert created_person is None


@pytest.mark.parametrize('attrname', ["COUNTRY_ID", "CoUnTRy-Id"])
def test_check_restricted_person_params_with_different_cases(session, attrname):
    session.config.__dict__['PERSON_CHECK_RESTRICTED_PARAMS'] = 1
    create_restricted_person_param(session, person_type='ur', attrname='country_id', value_str='1337')

    client = create_client(session)
    person_params = mandatory_fields_map['ur'].copy()

    del person_params['country_id']
    person_params[attrname] = '1337'

    with pytest.raises(exc.INVALID_PARAM) as exc_info:
        person_lib.change_person(session=session, client_id=client.id, mode=None, req=person_params,
                                 type='ur', person_id=None)

    exp_message = u'Invalid parameter for function: %s person param is restricted' % 'country_id'
    assert exc_info.value.msg == exp_message


def test_check_restricted_person_params_handles_legacy_inn_correctly(session):
    session.config.__dict__['PERSON_CHECK_RESTRICTED_PARAMS'] = 1
    # legacy restricted inns have person_type=None
    create_restricted_person_param(session, person_type=None, attrname='inn', value_str='7879679858')

    client = create_client(session)
    person_params = mandatory_fields_map['ur'].copy()

    person_params['inn'] = '7879679858'

    with pytest.raises(exc.INVALID_PARAM) as exc_info:
        person_lib.change_person(session=session, client_id=client.id, mode=None, req=person_params,
                                 type='ur', person_id=None)

    exp_message = u'Invalid parameter for function: %s person param is restricted' % 'inn'
    assert exc_info.value.msg == exp_message
