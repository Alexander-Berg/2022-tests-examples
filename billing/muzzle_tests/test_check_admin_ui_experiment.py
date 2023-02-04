# -*- coding: utf-8 -*-

from balance.corba_buffers import StateBuffer
from balance.constants import PermissionCode
from tests import object_builder as ob


def test_experiment_on(muzzle_logic):
    state_obj = StateBuffer()
    state_obj.setParam('balance_admin_new_ui', '1')

    res = muzzle_logic.check_admin_ui_experiment(state_obj)

    assert res.tag == 'experiment'
    assert res.text == '1'
    assert state_obj.getParam('skip') == '1'


def test_experiment_off(muzzle_logic):
    state_obj = StateBuffer()

    res = muzzle_logic.check_admin_ui_experiment(state_obj)

    assert res.tag == 'experiment'
    assert res.text == '0'
    assert not state_obj.hasParam('skip')


def test_old_ui_user(muzzle_logic, session):
    role = ob.create_role(session, PermissionCode.ADMIN_ACCESS, PermissionCode.FORCE_OLD_UI)
    ob.create_passport(session, role, patch_session=True)
    session.flush()

    state_obj = StateBuffer()
    muzzle_logic.check_old_ui_user(session, state_obj)

    assert not state_obj.hasParam('skip')


def test_new_ui_user(muzzle_logic, session):
    role = ob.create_role(session, PermissionCode.ADMIN_ACCESS)
    ob.create_passport(session, role, patch_session=True)
    session.flush()

    state_obj = StateBuffer()
    muzzle_logic.check_old_ui_user(session, state_obj)

    assert state_obj.hasParam('skip')
