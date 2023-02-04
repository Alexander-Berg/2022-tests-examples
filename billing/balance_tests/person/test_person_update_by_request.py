# -*- coding: utf-8 -*-

import pytest

from balance.mapper import Person, EmailMessage, FPSBank
from balance import person as person_lib, exc, constants as cst
from tests.balance_tests.person.person_common import (
    create_person, create_client, create_fias_city, create_fias_row, POSTCODE
)

from tests import object_builder as ob


@pytest.fixture(name='admin_role')
def create_admin_role(session):
    return ob.create_role(session, cst.PermissionCode.ADMIN_ACCESS)


@pytest.fixture(name='ext_edit_role')
def create_ext_edit_role(session):
    return ob.create_role(session, (cst.PermissionCode.PERSON_EXT_EDIT, {cst.ConstraintTypes.client_batch_id: None}))


def test_set_not_defined_detail(person):
    person_lib.update_by_request(person, {'xyz': '123'})
    assert person_lib.get_detail(person, 'xyz') == ''


@pytest.mark.parametrize('config', [
    1,
    0
])
def test_new_person_invalid_address(session, client, config):
    session.config.__dict__['USE_NEW_POSTADDRESS'] = config
    person = Person(client, type='ur')
    person.invalid_address = 1
    req = {"invalid-address": 1}
    person_lib.update_by_request(person, req=req)
    assert "invalid-address" not in req
    assert person.invalid_address == 0


@pytest.mark.parametrize('config', [
    1,
    0
])
def test_person_address_updated(session, person, config):
    session.config.__dict__['USE_NEW_POSTADDRESS'] = config
    person.address_updated = 1
    person.postsuffix = 3242
    session.flush()
    person_lib.update_by_request(person, req={})
    assert person.address_updated is None


def test_iso_passport_d(session, person):
    person.type = 'ph'
    session.flush()
    person_lib.update_by_request(person, req={
        'passport-d': '2010-01-20T00:00:00'
    })
    assert person.passport_d == '2010-01-20'


def test_incorrect_passport_d(session, person):
    person.type = 'ph'
    session.flush()
    with pytest.raises(exc.INVALID_PARAM) as exc_info:
        person_lib.update_by_request(person, req={
            'passport-d': 'gg'
        })
    assert exc_info.value.msg == 'Invalid parameter for function: passport-d must be date. Incorrect date format'


@pytest.mark.parametrize('config', [
    1,
    0
])
@pytest.mark.parametrize('req', [{'city': 'person_city'},
                                 {'postaddress': 'person_postaddress'},
                                 {'postcode': 'person_postcode'},
                                 {'postsuffix': 'person_postsuffix'},
                                 {'address': 'person_address'},
                                 ])
def test_existing_person_valid_invalid_address_edit(session, req, person, config):
    session.config.__dict__['USE_NEW_POSTADDRESS'] = config
    person.invalid_address = 1

    req = req.copy()
    req["invalid-address"] = 1
    person_lib.update_by_request(person, req=req)
    assert person.invalid_address == 0
    assert "invalid-address" not in req


@pytest.mark.parametrize('config', [
    1,
    0
])
def test_existing_person_invalid_address_edit(session, person, config):
    session.config.__dict__['USE_NEW_POSTADDRESS'] = config


@pytest.mark.parametrize('req', [
    pytest.param({'fname': 'fname'}, id='fname'),
    pytest.param({'is-partner': 1}, id='postsuffix auto filling disabled without address')
])
def test_existing_person_invalid_address_edit(req, session, person):
    person.invalid_address = 1
    person.postsuffix = 'person_postsuffix'
    person.is_partner = 1
    ob.set_roles(session, session.passport, [])
    person_lib.update_by_request(person, req=req)
    assert person.invalid_address == 1


@pytest.mark.parametrize('config', [
    1,
    0
])
@pytest.mark.parametrize('person_type', [
    'usu', 'usp', 'kzu', 'kzp', 'sw_ur', 'de_ur', 'sw_ph', 'de_ph', 'sw_yt', 'de_yt', 'sw_ytph', 'de_ytph',
])
def test_existing_person_invalid_address_edit_with_perm_wrongperson_type(session, admin_role, person_type, config):
    session.config.__dict__['USE_NEW_POSTADDRESS'] = config
    ob.set_roles(session, session.passport, [admin_role])
    person = create_person(session)
    person.invalid_address = 1
    person.type = person_type
    if person.type == 'kzu':
        person.iik = 'iik'
        person.bik = 'bik'
    person_lib.update_by_request(person, req={'fname': 'fname'})
    assert person.invalid_address == 1


@pytest.mark.parametrize('config', [
    1,
    0
])
@pytest.mark.parametrize('value_before, req, value_after', [(1, {}, 0),
                                                            (1, {'invalid-address': 0}, 0),
                                                            (0, {'invalid-address': 1}, 1)])
def test_existing_person_set_invalid_address_edit_with_perm(session, value_before, req, admin_role, value_after,
                                                            config):
    session.config.__dict__['USE_NEW_POSTADDRESS'] = config
    ob.set_roles(session, session.passport, [admin_role])
    person = create_person(session)
    person.invalid_address = value_before
    person.type = 'ph'
    person_lib.update_by_request(person, req=req)
    assert person.invalid_address == value_after


@pytest.mark.parametrize('config', [
    1,
    0
])
def test_existing_person_invalid_address_edit_with_same_value(session, person, config):
    session.config.__dict__['USE_NEW_POSTADDRESS'] = config
    person.invalid_address = 1
    ob.set_roles(session, session.passport, [])
    person.city = ''
    person.postaddress = 'person_postaddress'
    person.postcode = 'person_postcode'
    person.postsuffix = 'person_postsuffix'
    person.address = 'person_address'
    person_lib.update_by_request(person, req={'city': '',
                                              'postaddress': 'person_postaddress',
                                              'postcode': 'person_postcode',
                                              'postsuffix': 'person_postsuffix',
                                              'address': 'person_address'}, )
    assert person.invalid_address == 1


@pytest.mark.parametrize('verified_docs_value', [1, 0])
@pytest.mark.parametrize('person_type', ['by_ytph', 'sw_ytph', 'ur'])
def test_sw_ytph_by_ytph_with_docs_verified(session, person, person_type, verified_docs_value):
    person.type = person_type
    person.verified_docs = verified_docs_value
    person.fname = 'person_fname'

    if verified_docs_value and person_type in ['by_ytph', 'sw_ytph']:
        with pytest.raises(exc.CHANGING_FIELD_IS_PROHIBITED) as exc_info:
            person_lib.update_by_request(person, req={'fname': 'another_fname'})
        assert exc_info.value.msg == 'You can not change the field'
    else:
        person_lib.update_by_request(person, req={'fname': 'another_fname'})
        assert person.fname == 'another_fname'


@pytest.mark.parametrize('w_role', [True, False])
def test_update_with_empty_req(session, admin_role, w_role):
    ob.set_roles(session, session.passport, [admin_role] if w_role else [])
    person = create_person(session)
    person.delivery_type = 1
    person.live_signature = 1
    person.invalid_bankprops = 1
    person_lib.update_by_request(person, req={})
    if session.check_perm('AdminAccess', strict=False):
        assert person.delivery_type == 1
        assert person.live_signature == 1
        assert person.invalid_bankprops == 1
    else:
        assert person.delivery_type == 1
        assert person.live_signature == 1
        assert person.invalid_bankprops == 1


@pytest.mark.parametrize('w_role', [True, False])
def test_selfemployed_person_updated_without_account(session, admin_role, w_role):
    '''
    Проверяем, что без реквизитов у самозанятого нельзя снять флаг invalid_bankprops
    Из-за минусификатора вместо invalid_bankprops передаем invalid-bankprops
    '''
    person = create_person(session, type='ur')
    person.ownership_type = 'SELFEMPLOYED'
    person.invalid_bankprops = 1
    ob.set_roles(session, session.passport, [admin_role] if w_role else [])
    person_lib.update_by_request(person, req={'invalid-bankprops': '0'})
    assert person.invalid_bankprops == 1


@pytest.mark.parametrize('add_params', [{'invalid-bankprops': '0'}, {}])
def test_selfemployed_person_updated_with_account(session, person, add_params):
    '''При проставлении бика и р/с у самозанятого снимается признак
    invalid_bankprops при явном указании

    Из-за минусификатора вместо invalid_bankprops передаем
    invalid-bankprops
    '''
    person.ownership_type = 'SELFEMPLOYED'
    person.invalid_bankprops = 1
    person.type = 'ur'
    params = {
        'account': '40802810000000481224',
        'bik': '044525974'
    }
    params.update(add_params)
    person_lib.update_by_request(person, req=params)
    assert person.invalid_bankprops == int(add_params.get('invalid-bankprops', 1))


@pytest.mark.parametrize('person_type', ['kzu', 'yt_kzu'])
def test_upper_iik_bik(person, person_type):
    person.type = person_type
    person.iik = 'lower_iik'
    person.bik = 'lower_bik'
    person_lib.update_by_request(person, req={})
    assert person.iik == 'LOWER_IIK'
    assert person.bik == 'LOWER_BIK'


def test_lname_is_equal_to_name_endbuyer(person):
    person.type = 'endbuyer_ph'
    person.name = 'person_name'
    person.lname = ''
    person_lib.update_by_request(person, req={})
    assert person.lname == 'person_name'


def test_person_region(person):
    person.region = None
    person_lib.update_by_request(person, req={'region': 225})
    assert person.region == 225


@pytest.mark.parametrize('req, type_inperson',
                         [({'ownership-type-ui': 'ORGANIZATION', 'ownership-type': 'INDIVIDUAL'},
                           'INDIVIDUAL'),
                          ({'ownership-type-ui': 'ORGANIZATION'}, 'ORGANIZATION'),
                          ({'ownership-type': 'INDIVIDUAL'}, 'INDIVIDUAL')
                          ])
def test_ownership_type(person, req, type_inperson):
    person.type = 'eu_yt'
    person.ownership_type_ui = None
    person_lib.update_by_request(person, req=req)
    assert person.ownership_type_ui == type_inperson


def test_person_yamoney_wallet(person):
    person.yamoney_wallet = None
    person_lib.update_by_request(person, req={'yamoney-wallet': 1})
    assert person.yamoney_wallet == 1


def test_person_payoneer_wallet(person):
    person.payoneer_wallet = None
    person_lib.update_by_request(person, req={'payoneer-wallet': 1})
    assert person.payoneer_wallet == 1


@pytest.mark.parametrize('person_vip_value, req, expected_vip', [(0, {'vip': 1}, 1),
                                                                 (0, {}, 0),
                                                                 (1, {}, 0)])
def test_person_vip(session, person, ext_edit_role, person_vip_value, req, expected_vip):
    person.vip = person_vip_value
    ob.set_roles(session, session.passport, [ext_edit_role])
    person_lib.update_by_request(person, req=req)
    assert person.vip == expected_vip


@pytest.mark.parametrize('person_vip_value, req, expected_vip', [(0, {'vip': 1}, 0)])
def test_person_vip_wo_perm(session, person_vip_value, req, expected_vip):
    ob.set_roles(session, session.passport, [])
    person = create_person(session)
    person.vip = person_vip_value
    person_lib.update_by_request(person, req=req)
    assert person.vip == expected_vip


@pytest.mark.parametrize('person_early_docs, req, expected_early_docs', [(0, {'early-docs': 1}, 1),
                                                                         (0, {}, 0),
                                                                         (1, {}, 0)])
def test_person_early_docs(session, person, ext_edit_role, person_early_docs, req, expected_early_docs):
    person.early_docs = person_early_docs
    ob.set_roles(session, session.passport, [ext_edit_role])
    person_lib.update_by_request(person, req=req)
    assert person.early_docs == expected_early_docs


@pytest.mark.parametrize('person_early_docs, req, expected_early_docs', [(0, {'early-docs': 1}, 0)])
def test_person_early_docs_wo_perm(session, person_early_docs, req, expected_early_docs):
    ob.set_roles(session, session.passport, [])
    person = create_person(session)
    person.early_docs = person_early_docs
    person_lib.update_by_request(person, req=req)
    assert person.early_docs == expected_early_docs


@pytest.mark.parametrize('person_revise_act_period_type, req, expected_revise_act_period_type',
                         [(1, {'revise-act-period-type': 2}, 2),
                          (1, {'revise-act-period-type': 0}, None),
                          (0, {}, 0),
                          (1, {}, None)])
def test_person_revise_act_period_type(session, person, ext_edit_role, person_revise_act_period_type, req,
                                       expected_revise_act_period_type):
    person.revise_act_period_type = person_revise_act_period_type
    ob.set_roles(session, session.passport, [ext_edit_role])
    person_lib.update_by_request(person, req=req)
    assert person.revise_act_period_type == expected_revise_act_period_type


@pytest.mark.parametrize('person_revise_act_period_type, req, expected_revise_act_period_type',
                         [(0, {'revise-act-period-type': 1}, 0),
                          (1, {'revise-act-period-type': 0}, 1)])
def test_person_revise_act_period_type_wo_perm(session, person_revise_act_period_type, req,
                                               expected_revise_act_period_type):
    ob.set_roles(session, session.passport, [])
    person = create_person(session)
    person.revise_act_period_type = person_revise_act_period_type
    person_lib.update_by_request(person, req=req)
    assert person.revise_act_period_type == expected_revise_act_period_type


@pytest.mark.parametrize('person_type', ['usu', 'usp'])
def test_person_us_state(person, person_type):
    person.type = person_type
    person.legaladdress = ''
    person.us_state = None
    person_lib.update_by_request(person, req={'us-state': 'us_state_value'})
    assert person.us_state == 'us_state_value'
    assert person.legaladdress == ', , us_state_value, '


def test_person_ben_account(person):
    person.ben_account = None
    person_lib.update_by_request(person, req={'ben-account': 1})
    assert person.ben_account == 1


def test_person_ben_bank(person):
    person.ben_bank = None
    person_lib.update_by_request(person, req={'ben-bank': 1})
    assert person.ben_bank == 1


def test_person_iban(person):
    person.iban = None
    person_lib.update_by_request(person, req={'iban': 1})
    assert person.iban == 1


def test_person_swift(person):
    person.swift = None
    person_lib.update_by_request(person, req={'swift': 1})
    assert person.swift == 1


def test_person_organization(person):
    person.organization = None
    person_lib.update_by_request(person, req={'organization': 1})
    assert person.organization == 1


def test_person_other(person):
    person.other = None
    person_lib.update_by_request(person, req={'other': 1})
    assert person.other == 1


def test_person_kz_in(person):
    person.kz_in = None
    person_lib.update_by_request(person, req={'kz-in': 1})
    assert person.kz_in == 1


@pytest.mark.parametrize('person_type, kbe_values', [
    ('kzu', ['11', '12', '13', '14', '15', '16', '17', '18', '19']),
    ('kzp', ['19'])])
def test_person_valid_kbe(person, person_type, kbe_values):
    person.type = person_type
    person.iik = 'iik'
    person.bik = 'bik'
    for kbe in kbe_values:
        person_lib.update_by_request(person, req={'kbe': kbe})
        assert person.kbe == kbe


def test_person_invalid_kbe_kzu(person):
    person.type = 'kzu'
    person.iik = 'iik'
    person.bik = 'bik'
    for kbe in ['10', '20']:
        with pytest.raises(exc.INVALID_PARAM) as exc_info:
            person_lib.update_by_request(person, req={'kbe': kbe})
        assert exc_info.value.msg == 'Invalid KBE for kzu or kzp person'


def test_person_invalid_kbe_kzp(person):
    person.type = 'kzp'
    person.iik = 'iik'
    person.bik = 'bik'
    with pytest.raises(exc.INVALID_PARAM) as exc_info:
        person_lib.update_by_request(person, req={'kbe': '17'})
    assert exc_info.value.msg == 'Invalid parameter for function: KBE for kzp is read-only'


def test_person_ogrn(person):
    person.type = 'ur'
    person.ogrn = None
    person_lib.update_by_request(person, req={'ogrn': 11})
    assert person.ogrn == 11


def test_person_rn(person):
    person.type = 'am_jp'
    person.rn = None
    person_lib.update_by_request(person, req={'rn': 11})
    assert person.rn == 11


def test_person_jpc(person):
    person.type = 'am_jp'
    person.jpc = None
    person_lib.update_by_request(person, req={'jpc': 11})
    assert person.jpc == 11


@pytest.mark.parametrize('req, expected_longname', [
    ({'name': u'Саша', 'longname': u'Александр'}, u'Александр'),
    ({'longname': u'Александр'}, u'Александр'),
    ({'name': u'Саша'}, u'Саша')])
def test_person_sw_yt_longname(person, req, expected_longname):
    person.type = 'sw_yt'
    person.longname = None
    person.name = None
    person.is_partner = 1
    req['is-partner'] = 1
    person_lib.update_by_request(person, req=req)
    assert person.longname == expected_longname


def test_person_corr_swift(person):
    person.corr_swift = None
    person_lib.update_by_request(person, req={'corr-swift': 11})
    assert person.corr_swift == 11


def test_local_attrs(person):
    person.local_name = None
    person.local_longname = None
    person.local_legaladdress = None
    person.local_postaddress = None
    person.local_city = None
    person.local_representative = None
    person.local_signerperson_name = None
    person.local_signer_position_name = None
    person.local_authority_doc_details = None
    person.local_bank = None
    person.local_ben_bank = None
    person.local_other = None
    person_lib.update_by_request(person, req={'local-name': ' local_name',
                                              'local-longname': ' local_longname',
                                              'local-legaladdress': ' local_legaladdress',
                                              'local-postaddress': ' local_postaddress',
                                              'local-city': 'local_city ',
                                              'local-representative': 'local_representative ',
                                              'local-signer-person-name': 'local_signerperson_name ',
                                              'local-signer-position-name': 'local_signer_position_name ',
                                              'local-authority-doc-details': 'local_authority_doc_details',
                                              'local-bank': 'local_bank',
                                              'local-ben-bank': 'local_ben_bank',
                                              'local-other': ' local_other'})
    assert person.local_name == 'local_name'
    assert person.local_longname == 'local_longname'
    assert person.local_legaladdress == 'local_legaladdress'
    assert person.local_postaddress == 'local_postaddress'
    assert person.local_city == 'local_city'
    assert person.local_representative == 'local_representative'
    assert person.local_signer_person_name == 'local_signerperson_name'
    assert person.local_signer_position_name == 'local_signer_position_name'
    assert person.local_authority_doc_details == 'local_authority_doc_details'
    assert person.local_bank == 'local_bank'
    assert person.local_ben_bank == 'local_ben_bank'
    assert person.local_other == 'local_other'


@pytest.mark.parametrize('initial_verified_docs', [
    0,
    # 1
])
@pytest.mark.parametrize('request_verified_docs', [
    # 0,
    1
])
def test_person_docs_verified_message(session, person, initial_verified_docs, request_verified_docs):
    person.verified_docs = initial_verified_docs
    person.name = 'person_name'
    person.type = 'sw_ur'
    person_lib.update_by_request(person, {'verified-docs': request_verified_docs})
    messages = [_object for _object in session.new if isinstance(_object, EmailMessage)]
    if request_verified_docs == 1:
        if initial_verified_docs == 0:
            assert person.verified_docs is True
            assert len(messages) == 1
        else:
            assert len(messages) == 0
            assert person.verified_docs is True
    else:
        assert len(messages) == 0
        assert person.verified_docs is False


def create_new_person(session, type='ur', is_partner=False):
    client = create_client(session)
    person = Person(client, type)
    person.is_partner = is_partner
    session.add(person)
    assert person.id is None
    return person


def create_person_wo_address(session, type='ur', is_partner=False, w_city=False, w_postaddress=False, w_fias=False,
                             w_postbox=False, w_postsuffix=False, w_postcode=False
                             ):
    person = create_person(session, type)
    if is_partner:
        person.is_partner = True
    if w_city:
        person.city = 'emerald'
    if w_postaddress:
        person.postaddress = 'main street build 666'
    if w_fias:
        fias_row = create_fias_row(session, short_name=u'ул.', formal_name=u'Ленина')
        person.fias_guid = fias_row.guid
    if w_postbox:
        person.is_postbox = 1
    if w_postsuffix:
        person.postsuffix = '2324'
    if w_postcode:
        person.postcode = '666666'
    person.invalid_address = None
    session.flush()
    assert person.id
    return person


def create_person_with_city(session, type='ur', is_partner=False):
    return create_person_wo_address(session, type, w_city=True, is_partner=is_partner)


def create_person_with_fias_postbox(session, type='ur', is_partner=False):
    return create_person_wo_address(session, type=type, w_postbox=True, w_fias=True, w_postcode=True,
                                    is_partner=is_partner)


def create_person_with_postsuffix(session, type='ur', is_partner=False):
    return create_person_wo_address(session, type, w_postsuffix=True, is_partner=is_partner)


def create_person_w_postaddress(session, type='ur', is_partner=False):
    return create_person_wo_address(session, type=type, w_postaddress=True, is_partner=is_partner)


def create_person_w_fias(session, type='ur', is_partner=False):
    return create_person_wo_address(session, type, w_fias=True, is_partner=is_partner)


PERSON_ADDRESS_TYPES_W_FIAS = [
    create_person_with_fias_postbox,
    create_person_w_fias
]

PERSON_ADDRESS_TYPES_WO_FIAS = [
    create_new_person,
    create_person_with_city,
    create_person_with_postsuffix,
    create_person_wo_address,
    create_person_w_postaddress,
]

OLD_PERSON_TYPE = PERSON_ADDRESS_TYPES_WO_FIAS + PERSON_ADDRESS_TYPES_W_FIAS

PERSON_ADDRESS_TYPES = OLD_PERSON_TYPE


def check_person(person, is_new_postaddress, is_postbox, postsuffix, city, fias_guid,
                 postaddress, postcode):
    if is_new_postaddress:
        assert person.is_postbox == is_postbox, 'is_postbox'
    assert person.postsuffix == postsuffix, 'postsuffix'
    assert person.city == city, 'city'
    assert person.fias_guid == fias_guid
    assert person.postaddress == postaddress, 'postaddress'
    assert person.postcode == postcode, 'postcode'
    assert person.invalid_address == 0


def get_req_copies(req, is_partner):
    req_copy = req.copy()
    if is_partner:
        req_copy.update({'is-partner': '1'})
    req_expected = req_copy.copy()
    return req_copy, req_expected


def safe_person_attrs(person):
    return {'postsuffix': person.postsuffix,
            'fias': person.fias_guid,
            'is-postbox': person.is_postbox,
            'city': person.city,
            'postaddress': person.postaddress}


class TestPersonAddress(object):

    @pytest.mark.parametrize('req', [
        {'postaddress': 'address_string', 'postcode': '666666', 'city': 'dfd'},
        {'postaddress': 'address_string', 'postcode': '666666'},
        {'postaddress': 'address_string', 'postcode': '666666', 'postsuffix': '234'}
    ])
    @pytest.mark.parametrize('person_creation_func', PERSON_ADDRESS_TYPES)
    @pytest.mark.parametrize('config', [
        0,
        1
    ])
    @pytest.mark.parametrize('person_type, is_partner', [
        ('ur', True),
        ('ur', False),
        ('ph', True)
    ])
    def test_address_string_only(self, session, person, config, req, person_creation_func, person_type, is_partner):
        """Адрес строкой принимаем"""
        session.config.__dict__['USE_NEW_POSTADDRESS'] = config
        person = person_creation_func(session, type=person_type, is_partner=True)
        before_attrs = safe_person_attrs(person)
        req_copy, req_expected = get_req_copies(req, is_partner)
        person_lib.update_by_request(person, req=req_copy)
        if not config:
            # без конфига не затирали постсуффикс
            postsuffix = req.get('postsuffix', before_attrs['postsuffix'])
        else:
            postsuffix = req.get('postsuffix')

        check_person(person,
                     config,
                     is_postbox=0,
                     # у непартнеров не стирали доп.данные об адресе, теперь стираем
                     postsuffix=postsuffix,
                     # раньше всегда затирали город, если дали индекс и адрес строкой
                     city=req.get('city') if config else None,
                     # затираем фиас, если это был фиас по а/я иначе не трогаем
                     fias_guid=None if config and before_attrs['is-postbox'] else before_attrs['fias'],
                     postaddress=req.get('postaddress'),
                     postcode=req['postcode'])

    @pytest.mark.parametrize('is_postbox', ['0', '1'])
    def test_fias_postbox(self, session, is_postbox):
        person = create_person_w_fias(session)
        city, _ = create_fias_city(session, postcode=666666, formal_name=u'Москва')
        person_lib.update_by_request(person, req={'is-postbox': is_postbox,
                                                  'fias-guid': city.guid,
                                                  'postsuffix': '666'})
        assert person.is_postbox == int(is_postbox)
        check_person(person,
                     is_postbox=int(is_postbox),
                     fias_guid=city.guid,
                     postsuffix='666',
                     city=' '.join([city.short_name, city.formal_name]),
                     postaddress=None,
                     postcode=None,
                     is_new_postaddress=True
                     )

    @pytest.mark.parametrize('req', [
        {'postcode': '666666', 'postsuffix': '666666'},
        {'postcode': '666666', 'postsuffix': '666666', 'city': 'ruby'}
    ])
    @pytest.mark.parametrize('person_creation_func', PERSON_ADDRESS_TYPES)
    @pytest.mark.parametrize('config', [
        0,
        1
    ])
    @pytest.mark.parametrize('person_type, is_partner', [
        ('ur', True),
        ('ur', False),
        ('ph', True)
    ])
    def test_old_postbox(self, session, person, config, req, person_creation_func, person_type, is_partner):
        """старый а/я"""
        session.config.__dict__['USE_NEW_POSTADDRESS'] = config
        person = person_creation_func(session, is_partner=is_partner)
        before_attrs = safe_person_attrs(person)
        req_copy, req_expected = get_req_copies(req, is_partner)

        person_lib.update_by_request(person, req=req_copy)

        check_person(person,
                     config,
                     is_postbox=0,
                     postsuffix=req.get('postsuffix', before_attrs['postsuffix']),
                     # теперь стали затирать город, если его не передали
                     city=req.get('city') if config else None,
                     # затираем фиас, если это был фиас по а/я иначе не трогаем
                     fias_guid=None if config and before_attrs['is-postbox'] else before_attrs['fias'],
                     postaddress=req.get('postaddress'),
                     postcode=req['postcode'])

    @pytest.mark.parametrize('req', [
        {'postaddress': 'address_string', 'postcode': '666666', 'city': 'dfd'},
        {'postaddress': 'address_string', 'postcode': '666666'},
        {'postaddress': 'address_string', 'postcode': '666666', 'postsuffix': '234'},
        {'postcode': '666666', 'postsuffix': '666666'},
        {'postcode': '666666', 'postsuffix': '666666', 'city': 'ruby'},
        {'postcode': '666666'}
    ])
    @pytest.mark.parametrize('person_creation_func', PERSON_ADDRESS_TYPES)
    @pytest.mark.parametrize('config', [
        0,
        1
    ])
    @pytest.mark.parametrize('person_type, is_partner', [
        ('ph', False)
    ])
    def test_old_postbox_and_postaddress_for_person_wo_postaddress(self, session, person, config, req,
                                                                   person_creation_func, person_type, is_partner):
        """проверяем, что не поломались физики непартнеры и не ru плательщики"""
        session.config.__dict__['USE_NEW_POSTADDRESS'] = config
        person = person_creation_func(session, type=person_type)
        before_attrs = safe_person_attrs(person)
        req_copy, req_expected = get_req_copies(req, is_partner)

        person_lib.update_by_request(person, req=req_copy)

        check_person(person,
                     config,
                     is_postbox=req.get('is-postbox', before_attrs['is-postbox']),
                     postsuffix=req.get('postsuffix', before_attrs['postsuffix']),
                     city=req.get('city', before_attrs['city']),
                     fias_guid=before_attrs.get('fias'),
                     postaddress=req.get('postaddress', before_attrs['postaddress']),
                     postcode=req['postcode'])

    @pytest.mark.parametrize('person_creation_func', PERSON_ADDRESS_TYPES)
    @pytest.mark.parametrize('config', [
        0,
        1
    ])
    @pytest.mark.parametrize('add_req', [
        {'postaddress': 'address_string', 'city': 'dfd'},
        {'postaddress': 'address_string'},
        {'postaddress': 'address_string', 'postsuffix': '234'},
    ])
    @pytest.mark.parametrize('person_type, is_partner', [
        ('ur', 1),
        ('ur', 0),
        ('ph', 1)
    ])
    def test_fias_find_by_postcode_postaddress(self, session, config, person_creation_func, add_req, person_type,
                                               is_partner):
        """по индексу определяем город"""
        session.config.__dict__['USE_NEW_POSTADDRESS'] = config

        city, _ = create_fias_city(session, postcode=POSTCODE, formal_name=u'Москва')
        req = {'postcode': city.postcode}
        req.update(add_req)

        person = person_creation_func(session, type=person_type, is_partner=bool(is_partner))

        req_copy, req_expected = get_req_copies(req, is_partner)

        session.flush()

        person_lib.update_by_request(person, req=req_copy)

        check_person(person,
                     config,
                     is_postbox=0,
                     postsuffix=req.get('postsuffix') or req.get('postaddress'),
                     city=' '.join([city.short_name, city.formal_name]),
                     fias_guid=city.guid,
                     postaddress=None,
                     postcode=req.get('postcode'))

    @pytest.mark.parametrize('config', [
        0,
        1
    ])
    def test_fias_find_by_postcode_postaddress_ph_non_partner(self, session, config):
        """по индексу не определяем город у физика непартнера"""
        session.config.__dict__['USE_NEW_POSTADDRESS'] = config

        city, _ = create_fias_city(session, postcode=POSTCODE, formal_name=u'Москва')
        req = {'postcode': city.postcode,
               'postaddress': 'loooong postaddress'}

        person = create_person_w_postaddress(session, type='ph', is_partner=bool(0))

        req_copy, req_expected = get_req_copies(req, is_partner=False)

        session.flush()

        person_lib.update_by_request(person, req=req_copy)

        check_person(person,
                     config,
                     is_postbox=0,
                     postsuffix=req.get('postsuffix'),
                     city=None,
                     fias_guid=None,
                     postaddress='loooong postaddress',
                     postcode=req.get('postcode'))

    @pytest.mark.parametrize('config', [
        0,
        1
    ])
    def test_fias_find_by_postcode_postaddress_w_fias(self, session, config):
        """по индексу не определяем город, если фиас уже есть"""
        session.config.__dict__['USE_NEW_POSTADDRESS'] = config

        postcode_city, _ = create_fias_city(session, postcode=POSTCODE, formal_name=u'НеМосква')

        city, _ = create_fias_city(session, formal_name=u'Москва')

        fias_row = create_fias_row(session, short_name=u'ул.', formal_name=u'Ленина', parent_fias=city)

        req = {'postcode': postcode_city.postcode,
               'fias-guid': fias_row.guid}

        person = create_person_w_postaddress(session, type='ur', is_partner=bool(0))

        req_copy, req_expected = get_req_copies(req, is_partner=False)

        session.flush()

        person_lib.update_by_request(person, req=req_copy)
        session.flush()
        check_person(person,
                     config,
                     is_postbox=0,
                     postsuffix=req.get('postsuffix'),
                     city=' '.join([city.short_name, city.formal_name]),
                     fias_guid=fias_row.guid,
                     postaddress=None,
                     postcode=req.get('postcode'))

    @pytest.mark.parametrize('config', [
        0,
        1
    ])
    @pytest.mark.parametrize('person_type, is_partner', [
        ('ur', 1),
        ('ur', 0),
        ('ph', 1)
    ])
    @pytest.mark.parametrize('w_fias_postcode', [
        True,
        False
    ])
    def test_fias_find_by_postcode_postaddress_error(self, session, config, person_type, is_partner, w_fias_postcode):
        """исключение, если нет ни постуффикса/ни постадреса (не можем определить а/я или полный адрес)"""
        session.config.__dict__['USE_NEW_POSTADDRESS'] = config

        if w_fias_postcode:
            city, _ = create_fias_city(session, postcode=POSTCODE, formal_name=u'Москва')
            req = {'postcode': city.postcode}
        else:
            req = {'postcode': '666666'}
        person = create_person_wo_address(session, type=person_type, is_partner=bool(is_partner))
        before_attrs = safe_person_attrs(person)
        req_copy, req_expected = get_req_copies(req, is_partner)
        session.flush()
        if not w_fias_postcode:
            person_lib.update_by_request(person, req=req_copy)
            check_person(person,
                         config,
                         is_postbox=req.get('is-postbox', before_attrs['is-postbox']),
                         postsuffix=req.get('postsuffix', before_attrs['postsuffix']),
                         # теперь стали затирать город, если его не передали
                         city=req.get('city', before_attrs['city']),
                         # затираем фиас, если это был фиас по а/я иначе не трогаем
                         fias_guid=before_attrs.get('fias'),
                         postaddress=req.get('postaddress', before_attrs['postaddress']),
                         postcode=req['postcode'])
        else:
            with pytest.raises(exc.INVALID_PARAM) as exc_info:
                person_lib.update_by_request(person, req=req_copy)

            assert exc_info.value.msg == 'Invalid parameter for function: missing key postaddress'

    @pytest.mark.parametrize('person_creation_func', PERSON_ADDRESS_TYPES_W_FIAS)
    @pytest.mark.parametrize('config', [0, 1])
    def test_do_not_erase_fias_with_postaddress(self, session, config, person_creation_func):
        """если фиас уже был установлен, не стираем значение при установке адреса строкой"""
        person = person_creation_func(session, type='ur')
        session.config.__dict__['USE_NEW_POSTADDRESS'] = config
        req = {'postaddress': 'address_string'}
        person_lib.update_by_request(person, req=req)
        assert person.fias_guid is not None
        assert req == {'postaddress': 'address_string'}

    @pytest.mark.parametrize('config', [0, 1])
    def test_address_by_kladr(self, session, person, config):
        """В адресе прописки по кладру ничего не подбираем"""
        session.config.__dict__['USE_NEW_POSTADDRESS'] = config
        city, _ = create_fias_city(session, formal_name=u'Москва')
        street = create_fias_row(session, short_name=u'ул.', formal_name=u'Ленина', parent_fias=city)
        req = {'address-code': street.kladr_code}
        person_lib.update_by_request(person, req=req)
        assert person.address_code == street.kladr_code

    @pytest.mark.parametrize('config', [0, 1])
    def test_fias_guid(self, session, person, config):
        """по ФИАСУ из адреса прописки определяем город"""
        session.config.__dict__['USE_NEW_POSTADDRESS'] = config
        city, _ = create_fias_city(session, formal_name=u'Москва')
        fias_row = create_fias_row(session, short_name=u'ул.', formal_name=u'Ленина', parent_fias=city)
        req = {'fias-guid': fias_row.guid}
        person_lib.update_by_request(person, req=req)
        assert person.city == fias_row.parent.short_name + ' ' + fias_row.parent.formal_name
        assert person.fias_guid == fias_row.guid

    @pytest.mark.parametrize('with_street, with_fias', [
        pytest.param(True, True, id='with both street and fias'),
        pytest.param(False, True, id='with only fias'),
        pytest.param(False, False, id='without both street and fias')
    ])
    @pytest.mark.parametrize('is_partner', [
        pytest.param(True, id='partner'),
        pytest.param(False, id='not partner')
    ])
    def test_street_field(self, session, is_partner, with_street, with_fias):
        # Тест заполнения поля STREET в соответствии с требованиями в BALANCE-39826
        person = create_new_person(session, is_partner=is_partner, type='ph' if is_partner else 'ur')
        req = {'is-partner': 1 if is_partner else 0}

        fias_street = None
        if with_fias:
            city, _ = create_fias_city(session, formal_name=u'Москва')
            fias_street = create_fias_row(session, short_name=u'ул.', formal_name=u'Ленина', parent_fias=city)
            req['fias-guid'] = fias_street.guid
        street = None
        if with_street:
            street = 'some streeet aaaa'
            req['street'] = street
        person_lib.update_by_request(person, req=req)

        if with_street:
            assert person.street == street
        elif with_fias and is_partner:
            assert person.street == ' '.join((fias_street.formal_name, fias_street.short_name))
        else:
            assert person.street is None

    @pytest.mark.parametrize('config', [0, 1])
    def test_fias_guid_not_in_city(self, session, person, config):
        """Если про создании плательщика передан ФИАС код улицы не в городе, выкидываем исключение"""
        session.config.__dict__['USE_NEW_POSTADDRESS'] = config
        fias_row = create_fias_row(session, short_name=u'ул.', formal_name=u'Ленина', parent_fias=None)
        req = {'fias-guid': fias_row.guid}
        with pytest.raises(exc.INVALID_PARAM) as exc_info:
            person_lib.update_by_request(person, req=req)
        assert exc_info.value.msg == 'Invalid parameter for function: {} is not a city'.format(
            'fias-guid' if not config else 'fias-guid (or address-code)')

    @pytest.mark.parametrize('config', [0, 1])
    def test_fias_find_by_postcode_city_by_street(self, session, config):
        """по индексу определяем город"""
        session.config.__dict__['USE_NEW_POSTADDRESS'] = config
        person = create_person(session, type='ur')
        city, _ = create_fias_city(session, formal_name=u'Москва')
        fias_row = create_fias_row(session, short_name=u'ул.', formal_name=u'Ленина', parent_fias=city,
                                   postcode=POSTCODE)
        session.flush()
        req = {'postcode': fias_row.postcode,
               'postaddress': 'df'}
        person_lib.update_by_request(person, req=req)
        assert person.city == fias_row.parent.short_name + ' ' + fias_row.parent.formal_name
        assert person.fias_guid == fias_row.parent.guid
        assert person.postaddress is None
        assert person.postcode == fias_row.postcode
        assert person.postsuffix == 'df'

    @pytest.mark.parametrize('config', [0, 1])
    def test_not_live_fias_find_by_postcode_city_by_street(self, session, person, config):
        """ по индексу определяем город, только если индекс принадлежит ФИАСу с live_status = 1"""
        session.config.__dict__['USE_NEW_POSTADDRESS'] = config
        city, _ = create_fias_city(session, postcode=POSTCODE, live_status=0, formal_name=u'Москва')
        session.flush()
        req = {'postcode': city.postcode,
               'postaddress': 'df'}
        person_lib.update_by_request(person, req=req)
        assert person.city is None
        assert person.fias_guid is None
        assert person.postaddress == 'df'
        assert person.postcode == city.postcode
        assert person.postsuffix is None

    @pytest.mark.parametrize('config', [0, 1])
    def test_several_fias_find_by_postcode_city_by_street(self, session, person, config):
        """ по индексу определяем город, только если индекс принадлежит одному ФИАСу"""
        session.config.__dict__['USE_NEW_POSTADDRESS'] = config
        person.city = 'prev_city'
        city, _ = create_fias_city(session, postcode=POSTCODE, live_status=1, formal_name=u'Москва')
        city, _ = create_fias_city(session, postcode=POSTCODE, live_status=1, formal_name=u'Москва')
        session.flush()
        req = {'postcode': city.postcode,
               'postaddress': 'df'}
        person_lib.update_by_request(person, req=req)
        assert person.city == 'prev_city'
        assert person.fias_guid is None
        assert person.postaddress == 'df'
        assert person.postcode == city.postcode
        assert person.postsuffix is None

    @pytest.mark.parametrize('config', [0, 1])
    def test_fias_find_by_postcode_no_city(self, session, person, config):
        """ Если по индексу не нашелся уникальный ФИАС города, обнуляем город, сохраняем индекс, сохраняем почтовый адрес"""
        session.config.__dict__['USE_NEW_POSTADDRESS'] = config
        fias_row = create_fias_row(session, postcode=POSTCODE, short_name=u'ул.', formal_name=u'Ленина')
        session.flush()
        req = {'postcode': fias_row.postcode,
               'postaddress': 'df'}
        person_lib.update_by_request(person, req=req)
        assert person.city is None
        assert person.fias_guid is None
        assert person.postaddress == 'df'
        assert person.postcode == fias_row.postcode
        assert person.postsuffix is None


class TestPersonLegalAddress(object):
    def test_legal_fias_by_kladr(self, session, person):
        """Если про создании плательщика передан КЛАДР код, которому соответствует только один актуальный адрес по ФИАСу,
        заполняем город, улицу, индекс из ФИАСа, сохраняя КЛАДР и ФИАС коды. Не сохраняем адрес строкой,
        если он был передан"""
        city, _ = create_fias_city(session, formal_name=u'Москва')
        fias_row = create_fias_row(session, short_name=u'ул.', formal_name=u'Ленина', parent_fias=city)
        req = {'legal-address-code': fias_row.kladr_code,
               'legaladdress': 'legaladdress_string'}
        person_lib.update_by_request(person, req=req)
        assert person.legal_address_city == fias_row.parent.short_name + ' ' + fias_row.parent.formal_name
        assert person.legal_address_code == fias_row.kladr_code
        assert person.legal_address_postcode == fias_row.postcode
        assert person.legal_address_street == fias_row.formal_name + ' ' + fias_row.short_name
        assert person.legal_fias_guid == fias_row.guid
        assert person.legaladdress is None

    def test_find_unique_live_fias_by_kladr_city(self, session, person):
        """ Если про создании плательщика передан КЛАДР код, которому соответстует только один актуальный адрес по ФИАСу
        и это город, заполняем город, индекс из ФИАСа, сохраняя КЛАДР и ФИАС коды. Не сохраняем адрес строкой,
        если он был передан """
        city, _ = create_fias_city(session, formal_name=u'Москва')
        req = {'legal-address-code': city.kladr_code,
               'legaladdress': 'legaladdress_string'}
        person_lib.update_by_request(person, req=req)
        assert person.legal_address_city == city.short_name + ' ' + city.formal_name
        assert person.legal_address_code == city.kladr_code
        assert person.legal_address_postcode == city.postcode
        assert person.legal_address_street is None
        assert person.legal_fias_guid == city.guid
        assert person.legaladdress is None

    @pytest.mark.parametrize('KLADR_in_req, KLADR_in_fias',
                             [(ob.generate_numeric_string(15), lambda x: x[:15]),
                              (ob.generate_numeric_string(14), lambda x: x[:11]),
                              (ob.generate_numeric_string(10), lambda x: x[:]),
                              ],
                             ids=['kladr_longer_than_15',
                                  'kladr_between_11_and_15',
                                  'kladr_less than 11'])
    def test_long_kladr_code(self, session, person, KLADR_in_req, KLADR_in_fias):
        """ Из КЛАДРА длиной до 15 символов, берем первые 11, из КЛАДРА длиной 15 символов и более - первые 15
            КЛАДР меньше 11 символов не добиваем нулями. В плательщика записываем короткий КЛАДР"""
        city, _ = create_fias_city(session, formal_name=u'Москва')
        fias_row = create_fias_row(session, short_name=u'ул.', formal_name=u'Ленина', parent_fias=city)
        fias_row.kladr_code = KLADR_in_fias(KLADR_in_req)
        session.flush()
        req = {'legal-address-code': KLADR_in_req}
        person_lib.update_by_request(person, req=req)
        assert person.legal_address_city == fias_row.parent.short_name + ' ' + fias_row.parent.formal_name
        assert person.legal_address_code == fias_row.kladr_code
        assert person.legal_address_postcode == fias_row.postcode
        assert person.legal_address_street == fias_row.formal_name + ' ' + fias_row.short_name
        assert person.legal_fias_guid == fias_row.guid

    @pytest.mark.parametrize('fias_postcode', [None, '333333'])
    def test_postcode_from_fias(self, session, person, fias_postcode):
        """если в подобранном фиасе не было индекса, затираем прежний, если был - устанавлием из фиаса"""
        city, _ = create_fias_city(session, formal_name=u'Москва')
        fias_row = create_fias_row(session, short_name=u'ул.', formal_name=u'Ленина', parent_fias=city,
                                   postcode=fias_postcode)
        person.legal_address_postcode = '123'
        session.flush()
        req = {'legal-address-code': fias_row.kladr_code}
        person_lib.update_by_request(person, req=req)
        assert person.legal_address_postcode == (fias_row.postcode if fias_row.postcode else None)

    def test_find_unique_not_live_fias_by_kladr(self, session):
        """ Если найденный по КЛАДРУ ФИАС нежилой, не заполняем поля по ФИАСу, просим адрес строкой"""
        person = create_person(session, type='ur')
        city, _ = create_fias_city(session, formal_name=u'Москва')
        fias_row = create_fias_row(session, short_name=u'ул.', formal_name=u'Ленина', parent_fias=city, live_status=0)
        session.flush()
        req = {'legal-address-code': fias_row.kladr_code}
        with pytest.raises(exc.MISSING_MANDATORY_PERSON_FIELD) as exc_info:
            person_lib.update_by_request(person, req=req)
        assert exc_info.value.msg == 'Missing mandatory person field \'legaladdress\' for person type ur'
        assert person.legal_address_code is None
        assert person.legal_fias_guid is None

    def test_find_non_unique_live_fias_by_kladr(self, session, person):
        """ Если по КЛАДРУ нашлось несколько ФИАСов, не заполняем поля по ФИАСу, просим адрес строкой"""
        person = create_person(session, type='ur')
        kladr_code = ob.generate_numeric_string(length=ob.FiasBuilder.KLADR_CODE_LENGTH)
        [create_fias_row(session, short_name=u'ул.', formal_name=u'Ленина', parent_fias=None,
                         kladr_code=kladr_code) for _ in range(2)]
        session.flush()
        req = {'legal-address-code': kladr_code}
        with pytest.raises(exc.MISSING_MANDATORY_PERSON_FIELD) as exc_info:
            person_lib.update_by_request(person, req=req)
        assert exc_info.value.msg == 'Missing mandatory person field \'legaladdress\' for person type ur'
        assert person.legal_address_code is None
        assert person.legal_fias_guid is None

    def test_find_non_unique_live_fias_by_kladr_with_legal_address(self, session, person):
        """ Если по КЛАДРУ нашлось несколько ФИАСов или не нашелся ни один и есть адрес строкой,сохраняем КЛАДР
         и адрес строкой"""
        kladr_code = ob.generate_numeric_string(length=ob.FiasBuilder.KLADR_CODE_LENGTH)
        [create_fias_row(session, short_name=u'ул.', formal_name=u'Ленина', parent_fias=None,
                         kladr_code=kladr_code) for _ in range(2)]
        session.flush()
        req = {'legal-address-code': kladr_code,
               'legaladdress': 'legaladdress_string'}
        person_lib.update_by_request(person, req=req)
        assert person.legal_address_postcode is None
        assert person.legal_address_code == kladr_code
        assert person.legaladdress == 'legaladdress_string'

    def test_legal_address(self, person):
        """ Если передан только юридический адрес строкой, сохраняем его"""
        req = {'legaladdress': 'legaladdress_string'}
        person_lib.update_by_request(person, req=req)
        assert person.legaladdress == 'legaladdress_string'

    def test_legal_fias_guid(self, session, person):
        """Если про создании плательщика передан ФИАС код, заполняем из него город, улицу, индекс, КЛАДР код.
         не сохраняем адрес строкой, если он был передан"""
        city, _ = create_fias_city(session, formal_name=u'Москва')
        fias_row = create_fias_row(session, short_name=u'ул.', formal_name=u'Ленина', parent_fias=city)
        person.legal_fias_guid = None
        req = {'legal-fias-guid': fias_row.guid,
               'legaladdress': 'legaladdress_string'}
        person_lib.update_by_request(person, req=req)
        assert person.legal_fias_guid == fias_row.guid
        assert person.legal_address_city == fias_row.parent.short_name + ' ' + fias_row.parent.formal_name
        assert person.legal_address_code == fias_row.kladr_code
        assert person.legal_address_postcode == fias_row.postcode
        assert person.legal_address_street == fias_row.formal_name + ' ' + fias_row.short_name
        assert person.legaladdress is None

    def test_legal_fias_guid_not_in_city(self, session, person):
        """Если про создании плательщика передан ФИАС код улицы не в городе, выкидываем исключение"""
        fias_row = create_fias_row(session, short_name=u'ул.', formal_name=u'Ленина', parent_fias=None)
        person.legal_fias_guid = None
        req = {'legal-fias-guid': fias_row.guid,
               'legaladdress': 'legaladdress_string'}
        with pytest.raises(exc.INVALID_PARAM) as exc_info:
            person_lib.update_by_request(person, req=req)
        assert exc_info.value.msg == 'Invalid parameter for function: Legal address fias not in a city'


@pytest.mark.parametrize(
    'match_client, allowed',
    [
        pytest.param(None, False, id='wo role'),
        pytest.param(False, False, id='wrong client'),
        pytest.param(True, True, id='right client'),
    ],
)
def test_ext_edit_permission(session, client, ext_edit_role, match_client, allowed):
    roles = []
    if match_client is not None:
        client_batch = ob.RoleClientBuilder.construct(session, client=client if match_client else None).client_batch_id
        roles.append((ext_edit_role, {cst.ConstraintTypes.client_batch_id: client_batch}))
    ob.set_roles(session, session.passport, roles)

    person = create_person(session, client=client)
    person.vip = 0

    person_lib.update_by_request(person, req={'vip': 1})
    assert person.vip is (True if match_client else False)


@pytest.mark.parametrize('is_partner', [0, 1])
@pytest.mark.parametrize('person_type, ownership_type, fps_bank, fps_phone, expected_error', [
    pytest.param('ur', 'SELFEMPLOYED', 'TROLO BANK', '78005552535', None,
                 id='ok-selfemployed'),
    pytest.param('ph', None, 'TROLO BANK', '78005552536', None,
                 id='ok-ph'),
    pytest.param('ur', None, 'TROLO BANK', '78005552535', u'bank-type: 10 (FPS) could be set only for ph or selfemployed persons',
                 id='ur-not_selfemployed'),
    pytest.param('ur', 'SELFEMPLOYED', '', '78005552535', u'Invalid fps_bank',
                 id='empty_fps_bank'),
    pytest.param('ur', 'SELFEMPLOYED', 'NONONO', '78005552535', u'Unknown fps-bank',
                 id='non_existing_fps_bank'),
    pytest.param('ur', 'SELFEMPLOYED', 'TROLO BANK', '', u'Invalid fps-phone',
                 id='empty_fps_phone'),
])
def test_fps_bank(session, person_type, is_partner, ownership_type, fps_bank, fps_phone, expected_error):
    session.add(FPSBank(front_id=666, processing_bank='vabank', cc='TROLO BANK', name=u'ЛАБЕАН-БАНК'))
    session.flush()
    person = create_person(session, type=person_type)
    raised_exception = None
    try:
        person_lib.update_by_request(person, req={
            'bank-type': 10,
            'ownership-type': ownership_type,
            'fps-bank': fps_bank,
            'fps-phone': fps_phone,
        })
    except Exception as e:
        raised_exception = e

    if expected_error:
        assert expected_error in repr(raised_exception)
    else:
        assert not raised_exception
        assert person.fps_bank == fps_bank
        assert person.fps_phone == fps_phone


@pytest.mark.parametrize('is_partner', [0, 1])
@pytest.mark.parametrize('person_type, bank_type, add_new_account_to_request, result', [
    pytest.param('ur', 'fps', False, 'account_saved', id='ur_fps_no_new_account'),
    pytest.param('ur', 'paypal', False, 'account_saved', id='ur_paypal_no_new_account'),
    pytest.param('ph', 'fps', False, 'account_saved', id='ph_fps_no_new_account'),
    pytest.param('ph', 'paypal', False, 'account_cleared', id='ph_paypal_no_new_account'),
    pytest.param('ur', 'fps', True, 'account_updated', id='ur_fps_new_account'),
    pytest.param('ur', 'paypal', True, 'account_updated', id='ur_paypal_new_account'),
    pytest.param('ph', 'fps', True, 'account_updated', id='ph_fps_new_account'),
    pytest.param('ph', 'paypal', True, 'account_cleared', id='ph_paypal_new_account'),
    pytest.param('ur', 'other_bank', False, 'account_updated', id='ur_other_bank'),
    pytest.param('ph', 'other_bank', False, 'account_updated', id='ph_other_bank'),
])
def test_save_account(session, person_type, is_partner, bank_type, add_new_account_to_request, result):
    old_account = '111'
    new_account = '222'
    ownership_type = 'SELFEMPLOYED' if person_type == 'ur' else ''
    reqs_by_bank_type = {
        'fps': {
            'bank-type': 10,
            'ownership-type': ownership_type,
            'fps-bank': 'TROLO BANK',
            'fps-phone': '78005552535',
        },
        'paypal': {
            'bank-type': 5,
            'paypal_wallet': 333,
        },
        'other_bank': {
            'bank_type': 2,
            'account': new_account
        }
    }
    if add_new_account_to_request:
        for k in reqs_by_bank_type:
            reqs_by_bank_type[k]['account'] = new_account
    if bank_type == 'fps':
        session.add(FPSBank(front_id=666, processing_bank='vabank', cc='TROLO BANK', name=u'ЛАБЕАН-БАНК'))
        session.flush()
    person = create_person(session, type=person_type, account=old_account)
    assert person.account == old_account

    person_lib.update_by_request(person, req=reqs_by_bank_type[bank_type])
    if result == 'account_cleared':
        assert person.account is None
    elif result == 'account_updated':
        assert person.account == new_account
    elif result == 'account_saved':
        assert person.account == old_account


def test_disallow_change_is_partner(session, xmlrpcserver):
    """Проверяем, что запрещено изменять свойство is_partner у плательщика после создания"""

    person = create_person(session, is_partner=1)

    # Изменяем плательщика (передаем такой же is_partner, как при создании). is_partner не должно измениться
    person_lib.update_by_request(person, req={'name': person.name, 'is-partner': 1})
    assert person.is_partner == 1

    # Изменяем плательщика (вообще не передаем is_partner в ручку). is_partner не должно измениться
    person_lib.update_by_request(person, req={'name': person.name})
    assert person.is_partner == 1

    # Изменяем плательщика (передаем is_partner, отличный от значения при создании). Сообщается об ошибке
    with pytest.raises(exc.PERSON_IS_PARTNER_IMMUTABLE) as e:
        person_lib.update_by_request(person, req={'name': person.name, 'is-partner': 0})
