# -*- coding: utf-8 -*-
import pytest

from balance import  constants as cst
from balance.utils.xml2json import xml2json_auto
from muzzle.api import client as client_api
from tests import object_builder as ob


class PermCheckType(object):
    own_client = 0
    wo_role = 1
    w_role = 2
    wrong_client = 3
    right_client = 4
    own_client_w_role = 5


@pytest.fixture(name='client')
def create_client(session):
    return ob.ClientBuilder().build(session).obj


def _set_edit_person_role(session, check_type, client):
    roles = []

    if check_type in (PermCheckType.own_client, PermCheckType.own_client_w_role):
        session.passport.client = client
        session.flush()
    if check_type not in (PermCheckType.wo_role, PermCheckType.own_client):
        role = ob.create_role(session, (cst.PermissionCode.VIEW_CLIENTS, {cst.ConstraintTypes.client_batch_id: None}))
        if check_type not in (PermCheckType.w_role, PermCheckType.own_client_w_role):
            client_batch_id = ob.RoleClientBuilder.construct(session, client=client if check_type == PermCheckType.right_client else None).client_batch_id
            role = (role, {cst.ConstraintTypes.client_batch_id: client_batch_id})
        roles = [role]
    ob.set_roles(
        session,
        session.passport,
        roles,
    )


def test_get_effective_client_info(session, muzzle_logic):
    client = ob.ClientBuilder().build(session).obj
    orders = []
    agency_ids = []
    for _i in range(3):
        agency = ob.ClientBuilder(is_agency=1).build(session).obj
        agency_ids.append(agency.id)
        orders.append(
            ob.OrderBuilder(
                client=client,
                agency=agency
            ).build(session).obj
        )

    res = muzzle_logic.get_effective_client_info(session, client.id)
    assert res.tag == 'client_info'

    res = xml2json_auto(res)
    fields = ['client_id',
              'client_type_id',
              'name',
              'email',
              'phone',
              'fax',
              'url',
              'city',
              'is_agency',
              'full_repayment_invoices',
              'overdraft_ban',
              'manual_suspect',
              'reliable_cc_payer',
              'deny_cc',
              'ufunds_wo_nds',
              'ufunds_w_nds',
              'is_ctype_3',
              'parent_agencies']

    assert sorted(res.keys()) == sorted(fields)
    assert sorted(agency_ids) == sorted([int(i['id']) for i in res['parent_agencies']['parent_agency']])
    assert sorted(agency_ids) == sorted([i.id for i in client.parent_agencies])


@pytest.mark.parametrize(
    'check_type, ans',
    [
        pytest.param(PermCheckType.own_client, True, id='own client'),
        pytest.param(PermCheckType.wo_role, False, id='wo role'),
        pytest.param(PermCheckType.w_role, True, id='w role'),
        pytest.param(PermCheckType.wrong_client, False, id='w wrong constraint'),
        pytest.param(PermCheckType.right_client, True, id='w right constraint'),
        pytest.param(PermCheckType.own_client_w_role, True, id='own client and w role'),
    ],
)
def test_get_effective_client(session, check_type, ans, client):
    _set_edit_person_role(session, check_type, client)
    res_client = client_api.get_effective_client(session, client.id)
    assert (res_client == client) is ans


@pytest.mark.parametrize(
    'check_type, ans',
    [
        pytest.param(PermCheckType.own_client, True, id='own client'),
        pytest.param(PermCheckType.wo_role, False, id='wo role'),
        pytest.param(PermCheckType.w_role, False, id='w role'),
        pytest.param(PermCheckType.wrong_client, False, id='w wrong constraint'),
        pytest.param(PermCheckType.right_client, False, id='w right constraint'),
        pytest.param(PermCheckType.own_client_w_role, False, id='own client and w role'),
    ],
)
def test_get_effective_client_wo_client_id(session, check_type, ans, client):
    _set_edit_person_role(session, check_type, client)
    res_client = client_api.get_effective_client(session, None)
    req_ans = client if ans else None
    assert res_client is req_ans
