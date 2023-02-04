# -*- coding: utf-8 -*-


import pytest

from balance import person, exc
from tests.balance_tests.person.person_defaults import mandatory_fields_map


@pytest.mark.parametrize('person_type', mandatory_fields_map.keys())
def test_check_missing_mandatory_person_field_when_create_partner(person_type):
    "у партнеров не проверяем обязательность полей"
    person.check_missing_mandatory_fields(person_type=person_type, req={'is-partner': '1'},
                                          edit_mode=False)


@pytest.mark.parametrize('person_type', mandatory_fields_map.keys())
def test_check_missing_mandatory_person_field_when_create(person_type):
    person_params = mandatory_fields_map[person_type]
    for field in person_params.keys():
        if field not in ('country_id', 'legal_address_postcode'):  # BALANCE-33870
            # в этом тесте проверяется только старый список полей из person.py
            person_params_copy = person_params.copy()
            del person_params_copy[field]
            with pytest.raises(exc.MISSING_MANDATORY_PERSON_FIELD) as exc_info:
                person.check_missing_mandatory_fields(person_type=person_type, req=person_params_copy,
                                                      edit_mode=False, skip_kpp_check=False)
            error_msg = 'Missing mandatory person field \'{0}\' for person type {1}'.format(field, person_type)
            assert error_msg == exc_info.value.msg


@pytest.mark.parametrize('person_subtype, extra_params',
                         [('ur-kladr', {'postsuffix': '223', 'kladr-code': '7000000100000'}),
                          ('ur-postbox', {'postsuffix': '223'}),
                          ('ur', {'kladr-code': '7000000100000'}),
                          ])
def test_ur_with_address_mandatory_field(person_subtype, extra_params):
    person_params = mandatory_fields_map['ur'].copy()
    if person_subtype in ['ur-kladr', 'ur-postbox']:
        person_params = {k: v for k, v in person_params.iteritems() if k not in ['kpp', 'postaddress']}
    mandatory_field = person_params.keys()
    person_params.update(extra_params)
    for field in mandatory_field:
        if field not in ('country_id', 'legal_address_postcode'):  # BALANCE-33870
            # в этом тесте проверяется только старый список полей из person.py
            person_params_copy = person_params.copy()
            del person_params_copy[field]
            with pytest.raises(exc.MISSING_MANDATORY_PERSON_FIELD) as exc_info:
                person.check_missing_mandatory_fields(person_type='ur', req=person_params_copy,
                                                      edit_mode=False, skip_kpp_check=False)
            error_msg = 'Missing mandatory person field \'{0}\' for person type ur'.format(field, person_subtype)
            assert error_msg == exc_info.value.msg


def test_ur_selfemployed_mandatory_field():
    person_params = {"fname": "fname",
                     "lname": "lname",
                     "phone": "+7 812 3990776",
                     "email": "m-SC@qCWF.rKU",
                     "legaladdress": "Avenue 5",
                     "inn": "7879679858"}
    mandatory_field = person_params.keys()
    for field in mandatory_field:
        person_params_copy = person_params.copy()
        del person_params_copy[field]
        with pytest.raises(exc.MISSING_MANDATORY_PERSON_FIELD) as exc_info:
            person.check_missing_mandatory_fields(person_type='ur-selfemployed', req=person_params_copy,
                                                  edit_mode=False, skip_kpp_check=False)
        error_msg = 'Missing mandatory person field \'{0}\' for person type {1}'.format(field, 'ur-selfemployed')
        assert error_msg == exc_info.value.msg


def test_kpp_is_optional_for_ip():
    person_params = mandatory_fields_map['ur'].copy()
    person_params.pop('kpp')
    person.check_missing_mandatory_fields(person_type='ur', req=person_params,
                                          edit_mode=False, skip_kpp_check=True)


PARTNER_POSTBOX_MIN_PARAMS = {'postcode': '23234',
                              'postsuffix': 'а/я',
                              'fias-guid': '666-66'}

PH_NON_PARTNER_MIN_PARAMS = {'lname': 'lname',
                             'fname': 'fname',
                             'mname': 'mname',
                             'phone': '452534',
                             'email': '32@vgr.tt'}

UR_POSTBOX_MIN_PARAMS = {'name': 'name',
                         'phone': '452534',
                         'email': '32@vgr.tt',
                         'postcode': '1234',
                         'postsuffix': 'refe,',
                         'longname': 'longname',
                         'legaladdress': 'legaladdress',
                         'inn': '345345',
                         'fias-guid': '4534'}

UR_SELFEMPLOYED_MIN_PARAMS = {'lname': 'lname',
                              'fname': 'fname',
                              'phone': '452534',
                              'email': '32@vgr.tt',
                              'legaladdress': 'rfer',
                              'inn': '4353453'}


@pytest.mark.parametrize('ownership_type', [
    None,
    'SELFEMPLOYED'
])
@pytest.mark.parametrize('is_partner',
                         [
                             1,
                             0
                         ]
                         )
@pytest.mark.parametrize('person_type', [
    'ur',
    'ph'
])
def test_selfemployed_is_postbox(ownership_type, is_partner, person_type):
    person_params = {'is-partner': is_partner,
                     'is-postbox': 1}
    if ownership_type:
        person_params['ownership-type'] = ownership_type

    person_params_copy = person_params.copy()
    if is_partner and ownership_type == 'SELFEMPLOYED':
        pass
    elif is_partner:
        person_params_copy.update(PARTNER_POSTBOX_MIN_PARAMS)
    elif person_type == 'ph':
        person_params_copy.update(PH_NON_PARTNER_MIN_PARAMS)
    elif ownership_type == 'SELFEMPLOYED':
        person_params_copy.update(UR_SELFEMPLOYED_MIN_PARAMS)
    else:
        person_params_copy.update(UR_POSTBOX_MIN_PARAMS)

    person.check_missing_mandatory_fields(person_type=person_type, req=person_params_copy,
                                          edit_mode=False, skip_kpp_check=False)


@pytest.mark.parametrize('extra_fields', [{'legal-fias-guid': '', 'legal-address-home': ''},
                                          {'legal-address-code': '', 'legal-address-home': ''}])
def test_legal_address_mandatory_field(extra_fields):
    person_params = mandatory_fields_map['ur'].copy()
    person_params.pop('legaladdress')
    person_params.update(extra_fields)
    person.check_missing_mandatory_fields(person_type='ur', req=person_params,
                                          edit_mode=False, skip_kpp_check=False)


@pytest.mark.parametrize('edit_mode', [True, False])
def test_empty_mandatory_field_is_missing(edit_mode):
    person_params = mandatory_fields_map['ur'].copy()
    person_params['email'] = ''
    with pytest.raises(exc.MISSING_MANDATORY_PERSON_FIELD):
        person.check_missing_mandatory_fields(person_type='ur', req=person_params,
                                              edit_mode=edit_mode, skip_kpp_check=False)
