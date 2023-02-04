# -*- coding: utf-8 -*-
import datetime
import mock
import pytest

from balance import constants as cst, exc
from balance.corba_buffers import StateBuffer
from balance.utils.xml2json import xml2json_auto

from tests import object_builder as ob


class PermCheckType(object):
    own_client = 0
    wo_role = 1
    w_role = 2
    wrong_client = 3
    right_client = 4


perm_parametrize = (
    'check_type, ans',
    [
        pytest.param(PermCheckType.own_client, True, id='own client'),
        pytest.param(PermCheckType.wo_role, False, id='wo role'),
        pytest.param(PermCheckType.w_role, True, id='w role'),
        pytest.param(PermCheckType.wrong_client, False, id='w wrong constraint'),
        pytest.param(PermCheckType.right_client, True, id='w right constraint'),
    ],
)


@pytest.fixture(name='person')
def create_person(session, **kwargs):
    return ob.PersonBuilder.construct(session, **kwargs)


@pytest.fixture(name='client')
def create_client(session):
    return ob.ClientBuilder().build(session).obj


def _set_edit_person_role(session, check_type, client):
    roles = []
    if check_type == PermCheckType.own_client:
        session.passport.client = client
        session.flush()
    elif check_type != PermCheckType.wo_role:
        role = ob.create_role(session, (cst.PermissionCode.EDIT_PERSONS, {cst.ConstraintTypes.client_batch_id: None}))
        if check_type != PermCheckType.w_role:
            client_batch_id = ob.RoleClientBuilder.construct(session, client=client if check_type == PermCheckType.right_client else None).client_batch_id
            role = (role, {cst.ConstraintTypes.client_batch_id: client_batch_id})
        roles = [role]
    ob.set_roles(
        session,
        session.passport,
        roles,
    )


def test_get_person_info(session, person, muzzle_logic):
    response = muzzle_logic.get_person_info(session, person.id)
    response_json = xml2json_auto(response)
    assert len(response_json) > 1


@pytest.mark.parametrize('has_edo', [True, False])
def test_get_person_has_edo(session, person, muzzle_logic, has_edo):
    person.inn = str(ob.get_big_number())
    person.kpp = str(ob.get_big_number())
    if has_edo:
        ob.EdoOfferBuilder.construct(session, person_inn=person.inn, person_kpp=person.kpp)

    response = muzzle_logic.get_person_info(session, person.id)
    response_json = xml2json_auto(response)

    actual_has_edo = response_json.get('has-edo')
    assert response_json.get('has-edo') == '1' if has_edo else '0'


def test_get_person_info_fail(session, muzzle_logic):
    non_existent_person_id = -123
    response = muzzle_logic.get_person_info(session, non_existent_person_id)
    assert response.tag == 'person-not-found'


@mock.patch('muzzle.muzzle_logic.set_person_hidden')
def test_set_person_unarchive(set_person_hidden_mock, session, person, muzzle_logic):
    muzzle_logic.set_person_unarchive(session, person.id)
    set_person_hidden_mock.assert_called_once_with(person, False)


@pytest.mark.permissions
@pytest.mark.parametrize(*perm_parametrize)
@mock.patch('muzzle.muzzle_logic.set_person_hidden')
def test_set_person_unarchive(set_person_hidden_mock, session, muzzle_logic, client, check_type, ans):
    _set_edit_person_role(session, check_type, client)
    person = create_person(session, client=client)

    params = dict(
        session=session,
        person_id=person.id,
    )

    if ans:
        muzzle_logic.set_person_unarchive(**params)
        set_person_hidden_mock.assert_called_once_with(person, False)
    else:
        with pytest.raises(exc.PERMISSION_DENIED):
            muzzle_logic.set_person_unarchive(**params)
        set_person_hidden_mock.assert_not_called()


@pytest.mark.set_person
@pytest.mark.permissions
@pytest.mark.parametrize(*perm_parametrize)
def test_set_person_check_perm(session, muzzle_logic, client, check_type, ans):
    _set_edit_person_role(session, check_type, client)
    person = create_person(session, client=client)
    new_email = 'new_person_emaaaaail_%s@mail.com' % ob.get_big_number()

    state_obj = StateBuffer(
        params={'req_email': new_email},
    )
    params = dict(
        session=session,
        client_id=person.client_id,
        person_id=person.id,
        person_type=person.type,
        mode='',
        is_admin=False,
        state_obj=state_obj,
    )

    if ans:
        muzzle_logic.set_person(**params)
        assert person.email == new_email
    else:
        with pytest.raises(exc.PERMISSION_DENIED):
            muzzle_logic.set_person(**params)


@pytest.mark.permissions
@pytest.mark.parametrize(
    'create',
    [True, False],
)
@pytest.mark.parametrize(*perm_parametrize)
def test_get_person_changer(session, muzzle_logic, client, create, check_type, ans):
    _set_edit_person_role(session, check_type, client)
    person_id = create_person(session, client=client).id if create else -1

    state_obj = StateBuffer(params={})
    if ans:
        res = muzzle_logic.get_person_changer(session, person_id, client.id, 'ph', 0, state_obj)
        new_person = res.getchildren()[0]
        new_person_id = new_person.findtext('id')
        if create:
            assert int(new_person_id) == person_id
        else:
            assert new_person_id == u''
            assert int(new_person.findtext('client-id')) == client.id
    else:
        with pytest.raises(exc.PERMISSION_DENIED):
            muzzle_logic.get_person_changer(session, person_id, client.id, 'ph', 0, state_obj)
