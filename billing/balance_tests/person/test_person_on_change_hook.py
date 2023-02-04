# -*- coding: utf-8 -*-

import mock
import pytest

from balance import person as person_lib
from balance import exc
from balance import mapper
from balance import constants as cst

from tests import object_builder as ob
from tests.balance_tests.person.person_common import (
    create_person,
    create_client,
)
from tests.balance_tests.person.person_defaults import VALID_INN_10


@pytest.mark.parametrize('person_type', ['ur', 'ph'])
def test_changing_inn(session, person, person_type):
    session.config.__dict__['CHANGING_INN_CONFIGURABLE_CHECK'] = 0
    person.type = person_type
    session.flush()
    with pytest.raises(exc.CHANGING_INN_IS_PROHIBITED) as exc_info:
        person_lib.on_change_hook(person=person, field_name='inn', value=str(VALID_INN_10))
    assert exc_info.value.msg == 'Changing INN is prohibited!'


@pytest.mark.parametrize(
    'person_type, field_name, allowed_person_types, raises',
    [
        ('ur', 'inn', [], True),
        ('ur', 'inn', ['ur'], False),
        ('ph', 'inn', [], True),
        ('ph', 'inn', ['ph', 'ur'], False),
        ('kzp', 'kz_in', ['ph'], True),
        ('kzp', 'kz_in', ['ph', 'kzp'], False),
    ]
)
def test_changing_inn_configurable(session, person, person_type, field_name, allowed_person_types, raises):
    session.config.__dict__['CHANGING_INN_CONFIGURABLE_CHECK'] = 1
    session.config.__dict__['CHANGING_INN_ALLOWED_PERSON_TYPES'] = allowed_person_types
    person.type = person_type
    session.flush()
    if raises:
        with pytest.raises(exc.CHANGING_INN_IS_PROHIBITED) as exc_info:
            person_lib.on_change_hook(person=person, field_name=field_name, value=str(VALID_INN_10))
        assert exc_info.value.msg == 'Changing INN is prohibited!'
    else:
        value = person_lib.on_change_hook(person=person, field_name=field_name, value=str(VALID_INN_10))
        assert value == str(VALID_INN_10)


def test_name_attr_join(person):
    person.lname = ''
    person.fname = ''
    person.mname = ''
    person.type = 'ph'
    person_lib.set_detail(person, 'fname', u'Василий')
    assert person_lib.get_detail(person, 'name') == u'Василий'
    person_lib.set_detail(person, 'mname', u'Иванович')
    assert person_lib.get_detail(person, 'name') == u'Василий Иванович'
    person_lib.set_detail(person, 'lname', u'Пупкин')
    assert person_lib.get_detail(person, 'name') == u'Пупкин Василий Иванович'


def test_oktmo_wrong_length(person):
    with pytest.raises(exc.WRONG_OKTMO_LENGTH):
        person_lib.on_change_hook(person, field_name='oktmo', value='str with wrong length')


@pytest.mark.parametrize('oktmo_len', [8, 11])
def test_oktmo_ok_length(person, oktmo_len):
    value = '0' * oktmo_len
    person_lib.on_change_hook(person, field_name='oktmo', value=value)


def test_oktmo_empty(person):
    """
    Regression test. Check that on_change_hook is able to handle empty oktmo.
    :param person:
    :return:
    """
    person_lib.on_change_hook(person, field_name='oktmo', value='')


@pytest.mark.parametrize(
    'perms, inn, result',
    [
        pytest.param([], '7703599768', True, id='valid'),
        pytest.param([], '1234567890', False, id='invalid'),
        pytest.param([], '7736207543', False, id='restricted'),
        pytest.param([], '000000000000', False, id='restricted_invalid'),
        pytest.param(['UseRestrictedINN'], '7736207543', True, id='restricted_perm'),
    ]
)
def test_inn_validation(session, client, perms, inn, result):
    role = ob.create_role(session, *perms)
    ob.set_roles(session, session.passport, [role])

    person = mapper.Person(client, 'ph')
    session.add(person)

    if result:
        person_lib.on_change_hook(person, 'inn', inn)
    else:
        with pytest.raises(exc.INVALID_INN):
            person_lib.on_change_hook(person, 'inn', inn)


@pytest.mark.parametrize(
    'match_client, allowed',
    [
        pytest.param(None, False, id='wo role'),
        pytest.param(False, False, id='wrong client'),
        pytest.param(True, True, id='right client'),
    ],
)
def test_ext_edit_permission(session, client, match_client, allowed):
    roles = []
    if match_client is not None:
        client_batch = ob.RoleClientBuilder.construct(session, client=client if match_client else None).client_batch_id
        role = ob.create_role(session, (cst.PermissionCode.PERSON_EXT_EDIT, {cst.ConstraintTypes.client_batch_id: None}))
        roles.append((role, {cst.ConstraintTypes.client_batch_id: client_batch}))
    ob.set_roles(session, session.passport, roles)

    person = create_person(session, client=client)
    person.type = 'yt'
    session.flush()

    if allowed:
        res = person_lib.on_change_hook(person, 'name', 'Lumen')
        assert res == 'Lumen'
    else:
        with pytest.raises(exc.PERMISSION_DENIED):
            person_lib.on_change_hook(person, 'name', 'Lumen')
