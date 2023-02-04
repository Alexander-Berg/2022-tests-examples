# -*- coding: utf-8 -*-
import pytest
import hamcrest
import copy

from balance import balance_steps as b_steps
import btestlib.reporter as reporter
from balance.features import Features
from balance.balance_steps.other_steps import UserSteps
from balance.snout_steps import api_steps as steps
from btestlib.constants import (
    Export,
    Permissions,
    PersonTypes,
)
from btestlib.utils import get_secret_key
from btestlib.data.snout_constants import Handles
from btestlib.data.person_defaults import get_details

pytestmark = [reporter.feature(Features.UI, Features.PERSON)]


def prepare_data(data):
    data = data.get('data', {})
    for k, v in data.items():
        data[k.replace('_', '-')] = unicode(int(v) if isinstance(v, bool) else v)
    return data


@pytest.mark.smoke
def test_person_handle(get_free_user):
    """
    :example:
        https://snout.greed-tm.paysys.yandex.ru/v1/person?person_id=XXX
    """
    user = get_free_user()
    UserSteps.set_role(user, role_id=0)
    _, person_id = steps.create_client_person()
    steps.pull_handle_and_check_result(Handles.PERSON, person_id, user=user)


@pytest.mark.smoke
@pytest.mark.parametrize(
    'person_type',
    [PersonTypes.PH.code, PersonTypes.UR.code],
)
def test_create_person(get_free_user, person_type):
    user = get_free_user()
    client_id = b_steps.ClientSteps.create()
    b_steps.ClientSteps.link(client_id, user.login)

    person_data = copy.deepcopy(get_details(person_type))
    excepted_data = ['invalid-address', 'invalid-bankprops', 'live-signature', 'vip']
    for field in excepted_data:
        person_data.pop(field, None)

    data = steps.pull_handle_and_check_result(
        Handles.PERSON_EDIT,
        method='POST',
        user=user,
        custom_headers={'Content-Type': 'application/json', 'X-Is-Admin': 'false'},
        json_data={
            'person_type': person_type,
            'mode': 'EDIT',
            'client_id': client_id,
            'is_partner': False,
            'data': person_data,
            '_csrf': get_secret_key(user.uid),
        },
    )

    data = prepare_data(data)

    excepted_fields = ['account', 'address', 'postaddress', 'revise-act-period-type', 's_signer-position-name']
    for field in excepted_fields:
        person_data.pop(field, None)

    hamcrest.assert_that(
        data,
        hamcrest.has_entries(person_data),
    )


@pytest.mark.smoke
@pytest.mark.parametrize(
    'person_type, prev_data, in_data, perms, res_error, should_be_200_w_perm',
    [
        pytest.param(
            PersonTypes.UR.code,
            {'verified-docs': '0', 'invalid-address': '0', 'invalid-bankprops': '0', 'live-signature': '0'},
            {'invalid-address': '1', 'invalid-bankprops': '1', 'live-signature': '1'},
            [Permissions.ADMIN_ACCESS_0, Permissions.EDIT_PERSONS],
            'PERMISSION_DENIED',
            True,
            id='ur admin_access',
        ),
        pytest.param(
            PersonTypes.UR.code,
            {'verified-docs': '0', 'vip': '0', 'revise-act-period-type': '0'},
            {'vip': '1', 'verified-docs': '1', 'revise-act-period-type': '1'},
            [Permissions.PERSON_POST_ADDRESS_EDIT, Permissions.PERSON_EXT_EDIT, Permissions.EDIT_PERSONS],
            'PERMISSION_DENIED',
            True,
            id='ur post_address_edit_access',
        ),
        pytest.param(
            PersonTypes.SW_YTPH.code,
            {'verified-docs': '1'},
            {'phone': '+890', 'fax': 'new@fax.ru', 'email': 'new@email.ru',
             'postcode': '0b3f0723-5fe0-4c23-af44-8082166c6d2e', 'city': u'Петропавловск-Камчатский',
             'postaddress': 'Abc',
             'lname': 'ABC', 'fname': 'abc'},
            [Permissions.EDIT_PERSONS, Permissions.PERSON_POST_ADDRESS_EDIT],
            'CHANGING_FIELD_IS_PROHIBITED',
            False,
            id='verified-docs sw_ytph extended',
        ),
        pytest.param(
            PersonTypes.BY_YTPH.code,
            {'verified-docs': '1'},
            {'phone': '+890', 'fax': 'new@fax.ru', 'email': 'new@email.ru',
             'postcode': '12345', 'city': 'London', 'postaddress': 'Abc',
             'lname': 'ABC', 'fname': 'abc'},
            [Permissions.EDIT_PERSONS, Permissions.PERSON_POST_ADDRESS_EDIT],
            'CHANGING_FIELD_IS_PROHIBITED',
            False,
            id='verified-docs by_utph',
        ),
        pytest.param(
            PersonTypes.SW_YTPH.code,
            {'verified-docs': '1'},
            {'country-id': 84},
            [Permissions.EDIT_PERSONS, Permissions.PERSON_POST_ADDRESS_EDIT],
            'CHANGING_FIELD_IS_PROHIBITED',
            False,
            id='verified-docs sw_ytph',
        ),
        pytest.param(
            PersonTypes.HK_YTPH.code,
            {'verified-docs': '1'},
            {'country-id': 84},
            [Permissions.EDIT_PERSONS, Permissions.PERSON_POST_ADDRESS_EDIT],
            'PERMISSION_DENIED',
            True,
            id='verified-docs hk_ytph',
        ),
        pytest.param(
            PersonTypes.BY_YTPH.code,
            {'verified-docs': '1', 'invalid-address': '0'},
            {'invalid-address': '1'},  # адрес не входит в те поля, которые нельзя менять BALANCE-24033 и BALANCE-38762
            [Permissions.EDIT_PERSONS, Permissions.PERSON_POST_ADDRESS_EDIT, Permissions.ADMIN_ACCESS_0],
            'PERMISSION_DENIED',
            True,
            id='verified-docs by_ytph',
        ),
    ],
)
@pytest.mark.parametrize(
    'has_perm',
    [
        pytest.param(True, id='w_perm'),
        pytest.param(False, id='wo_perm'),
    ],
)
def test_restricted_fields(get_free_user, person_type, prev_data, in_data, perms, res_error, should_be_200_w_perm, has_perm):
    user = get_free_user()
    client_id = b_steps.ClientSteps.create()

    headers = {'Content-Type': 'application/json'}

    # Добавляем нужные права
    if has_perm:
        perms.append(Permissions.ADMIN_ACCESS_0)
        b_steps.UserSteps.set_role_with_permissions_strict(user, perms)
    else:
        b_steps.UserSteps.clear_roles(user)
        b_steps.ClientSteps.link(client_id, user.login)
        headers['X-Is-Admin'] = 'false'

    person_id = b_steps.PersonSteps.create(client_id, person_type, params=prev_data)
    is_200 = (has_perm and should_be_200_w_perm)

    data = steps.pull_handle_and_check_result(
        Handles.PERSON_EDIT,
        method='POST',
        user=user,
        custom_headers=headers,
        status_code=200 if is_200 else 400,
        expected_error=None if is_200 else {'error': 'FORM_VALIDATION_ERROR',
                                              'description': 'Form validation error.'},
        json_data={
            'person_type': person_type,
            'mode': 'EDIT',
            'client_id': client_id,
            'person_id': person_id,
            'is_partner': False,
            'data': in_data,
            '_csrf': get_secret_key(user.uid),
        },
    )

    if is_200:
        excepted_fields = ['revise-act-period-type']
        for field in excepted_fields:
            in_data.pop(field, None)

        hamcrest.assert_that(
            prepare_data(data),
            hamcrest.has_entries(prepare_data(in_data)),
        )

    else:
        hamcrest.assert_that(
            data,
            hamcrest.has_entries({
                'form_errors': hamcrest.has_entries({
                    k: hamcrest.contains(hamcrest.has_entries({'error': res_error}))
                    for k in prepare_data(in_data).keys()
                }),
            }),
        )


@pytest.mark.parametrize(
    'person_type, has_perm, res_err',
    [
        # никогда нельзя
        pytest.param(PersonTypes.PH.code, True, True, id='ph w perm'),
        pytest.param(PersonTypes.PH.code, False, True, id='ph wo perm'),
        pytest.param(PersonTypes.UR.code, True, True, id='ur w perm'),
        pytest.param(PersonTypes.UR.code, False, True, id='ur wo perm'),
        pytest.param(PersonTypes.FR_UR.code, True, True, id='fr_ur w perm'),
        pytest.param(PersonTypes.FR_UR.code, False, True, id='fr_ur wo perm'),

        # если есть права, то можно
        pytest.param(PersonTypes.SW_PH.code, True, False, id='sw_ph w perm'),
        pytest.param(PersonTypes.SW_PH.code, False, True, id='sw_ph wo perm'),
    ],
)
def test_change_inn(get_free_user, person_type, has_perm, res_err):
    user = get_free_user()
    client_id = b_steps.ClientSteps.create()

    headers = {'Content-Type': 'application/json'}

    # Добавляем нужные права
    if has_perm:
        b_steps.UserSteps.set_role_with_permissions_strict(
            user,
            [Permissions.EDIT_PERSONS, Permissions.PERSON_POST_ADDRESS_EDIT, Permissions.ADMIN_ACCESS_0],
        )
    else:
        b_steps.UserSteps.clear_roles(user)
        b_steps.ClientSteps.link(client_id, user.login)
        headers['X-Is-Admin'] = 'false'

    person_id = b_steps.PersonSteps.create(client_id, person_type, params={'verified-docs': '0'})

    data = steps.pull_handle_and_check_result(
        Handles.PERSON_EDIT,
        method='POST',
        user=user,
        custom_headers=headers,
        status_code=200 if not res_err else 400,
        expected_error=None if not res_err else {'error': 'FORM_VALIDATION_ERROR',
                                                 'description': 'Form validation error.'},
        json_data={
            'person_type': person_type,
            'mode': 'EDIT',
            'client_id': client_id,
            'person_id': person_id,
            'is_partner': False,
            'data': {'inn': '9876543'},
            '_csrf': get_secret_key(user.uid),
        },
    )

    if not res_err:
        hamcrest.assert_that(
            prepare_data(data),
            hamcrest.has_entries({'inn': '9876543'}),
        )

    else:
        hamcrest.assert_that(
            data,
            hamcrest.has_entries({
                'form_errors': hamcrest.has_entries({
                    'inn': hamcrest.contains(hamcrest.has_entries({'error': 'CHANGING_INN_IS_PROHIBITED'}))
                }),
            }),
        )


def test_default_value(get_free_user):
    """
    краевой случай
    поля заполняются на фронте как '0', дано правильно матчить их с нашими значениями

    """
    user = get_free_user()
    client_id = b_steps.ClientSteps.create()
    b_steps.ClientSteps.link(client_id, user.login)

    person_type = PersonTypes.UR.code
    person_data = copy.deepcopy(get_details(person_type))
    excepted_data = ['invalid-address', 'invalid-bankprops', 'live-signature', 'vip']
    for field in excepted_data:
        person_data[field] = '0'

    data = steps.pull_handle_and_check_result(
        Handles.PERSON_EDIT,
        method='POST',
        user=user,
        custom_headers={'Content-Type': 'application/json', 'X-Is-Admin': 'false'},
        json_data={
            'person_type': person_type,
            'mode': 'EDIT',
            'client_id': client_id,
            'is_partner': False,
            'data': person_data,
            '_csrf': get_secret_key(user.uid),
        },
    )


def test_change_other_field(get_free_user):
    """
    создаем админом плательщика с защищенным полем,
    а затем меняем другое поле в клиентке

    """
    admin = get_free_user()
    b_steps.UserSteps.set_role_with_permissions_strict(
        admin,
        [Permissions.EDIT_PERSONS, Permissions.PERSON_POST_ADDRESS_EDIT, Permissions.ADMIN_ACCESS_0],
    )

    params = {
        'invalid-bankprops': '1',
        'live-signature': '1',
        'vip': '1',
        'verified-docs': '1',
    }

    person_type = person_type = PersonTypes.UR.code
    client_id = b_steps.ClientSteps.create()
    person_id = b_steps.PersonSteps.create(
        client_id,
        person_type,
        params=params,
    )

    user = get_free_user()
    b_steps.ClientSteps.link(client_id, user.login)

    params['fax'] = '+89996667788'

    data = steps.pull_handle_and_check_result(
        Handles.PERSON_EDIT,
        method='POST',
        user=user,
        custom_headers={'Content-Type': 'application/json', 'X-Is-Admin': 'false'},
        json_data={
            'person_type': person_type,
            'mode': 'EDIT',
            'client_id': client_id,
            'person_id': person_id,
            'is_partner': False,
            'data': params,
            '_csrf': get_secret_key(user.uid),
        },
    )
    hamcrest.assert_that(
        prepare_data(data),
        hamcrest.has_entries(params),
    )
