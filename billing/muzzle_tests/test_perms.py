# -*- coding: utf-8 -*-

from balance import (
    constants as cst
)
from tests import object_builder as ob
from balance.corba_buffers import StateBuffer


def get_state_obj():
    params = {
        'prot_remote_ip': '95.108.172.0',
        'prot_host': 'balance.yandex.ru'
    }
    return StateBuffer(params=params)


def test_get_perms(session, muzzle_logic):
    role = ob.create_role(session, cst.PermissionCode.ADMIN_ACCESS, cst.PermissionCode.ADDITIONAL_FUNCTIONS)
    ob.create_passport(session, role, patch_session=True)
    session.flush()

    state_obj = StateBuffer()

    perms = muzzle_logic.get_perms(state_obj, session.passport.oper_id)
    perms_str = perms.text
    perms_list = perms.text.split(',')
    assert len(perms_list) in [2, 3]
    assert 'AdminAccess' in perms_list and 'AdditionalFunctions' in perms_list
    if len(perms_str) == 3:
        assert 'NewUIEarlyAdopter' in perms_list
