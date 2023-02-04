# -*- coding: utf-8 -*-
import pytest
import mock

import balance
from balance import (
    constants as cst,
    exc,
)

from tests.balance_tests.contract.access.access_common import (
    create_contract,
    create_wrong_role,
    create_client,
    create_role_client,
    create_passport,
    create_view_contract_role,
    create_firm_id,
)

pytestmark = [
    pytest.mark.permissions,
]


def _mock_cached_property_get(self, master, owner):
    return self.method(master)


def test_nobody(session, contract, wrong_role):
    """У пользователя без прав нет доступа к договору"""
    passport = create_passport(session, [wrong_role])
    with pytest.raises(exc.PERMISSION_DENIED):
        contract.check_access(passport)


def test_client(session, contract, wrong_role):
    """Если пользователь - представитель владельца договора, есть доступ к договору"""
    passport = create_passport(session, [wrong_role], client=contract.client)
    contract.check_access(passport)


def test_wrong_client(session, contract, wrong_role):
    """Если пользователь - он не представитель владельца договора, нет доступа к договору"""
    passport = create_passport(session, [wrong_role], client=create_client(session))
    with pytest.raises(exc.PERMISSION_DENIED):
        contract.check_access(passport)


def test_simple_client(session, contract, wrong_role):
    """Если пользователь - представитель владельца договора в персональных сервисах, есть доступ к счету"""
    passport = create_passport(session, [wrong_role], simple_client=contract.client)
    contract.check_access(passport)


@mock.patch('balance.deco.writable_cached_property.__get__', _mock_cached_property_get)
def test_direct_limited(session, contract, wrong_role):
    """Нельзя получить доступ к договору, если есть право DirectLimited"""
    passport = create_passport(session, [wrong_role], client=contract.client)
    passport.update_limited([contract.client.id])
    session.flush()
    with pytest.raises(exc.PERMISSION_DENIED_DIRECT_LIMITED):
        contract.check_access(passport)


def test_perm(session, contract, view_contract_role):
    """Пользователь с правом ViewContracts, роль не ограничены по фирме -> есть доступ к договору"""
    passport = create_passport(session, [view_contract_role])
    contract.check_access(passport)


def test_perm_firm(session, view_contract_role, firm_id):
    """Пользователь с правом ViewContracts, право ограничено по фирме, но фирма не указана,
     роль ограничена по фирме, фирма роли совпадает с фирмой договора -> есть доступ к договору"""
    passport = create_passport(session, [(view_contract_role, firm_id)])
    contract = create_contract(session, firm=firm_id)
    contract.check_access(passport)


def test_perm_wrong_firm(session, contract, view_contract_role):
    """Пользователь с правом ViewContracts, право ограничено по фирме, но фирма не указана,
     роль ограничена по фирме, фирма роли не совпадает с фирмой договора -> нет доступа к договору"""
    passport = create_passport(session, [(view_contract_role, cst.FirmId.TAXI)])
    with pytest.raises(exc.PERMISSION_DENIED):
        contract.check_access(passport)
        

def test_perm_w_clients(session, role_client, view_contract_role):
    contract = create_contract(session, client=role_client.client)
    passport = create_passport(session, [(view_contract_role, None, role_client.client_batch_id)])
    contract.check_access(passport)


def test_perm_w_clients_w_firm(session, role_client, firm_id, view_contract_role):
    passport = create_passport(
        session,
        [
            (view_contract_role, firm_id, role_client.client_batch_id),
            (view_contract_role, None, create_role_client(session).client_batch_id),
        ],
    )
    contract = create_contract(session, firm=firm_id, client=role_client.client)
    contract.check_access(passport)


def test_perm_w_clients_w_firm_in_different_roles_access(session, role_client, firm_id, view_contract_role):
    """У роли есть ограничения и по фирме и по клиентам.
    А к паспорту эта роль привязана дважды (в одном случае с firm_id, в другом с client_id)
    => есть доступ к договору, т.к. ограничения выглядят как (клиент и любая фирма) and (любой клиент и фирма).
    """
    passport = create_passport(
        session,
        [
            (view_contract_role, None, role_client.client_batch_id),
            (view_contract_role, firm_id, None),
        ],
    )
    contract = create_contract(session, firm=firm_id, client=role_client.client)
    contract.check_access(passport)


def test_perm_w_clients_w_firm_in_different_roles_failed(session, role_client, firm_id, view_contract_role):
    """У роли есть ограничения и по фирме и по клиентам.
    А к паспорту эта роль привязана дважды
    (в одном случае с firm_id и неправильным клиентом, в другом с правильным client_id и неправильной фирмой)
    => нет доступа к договору, т.к. ограничения не выполняются.
    """
    passport = create_passport(
        session,
        [
            (view_contract_role, create_firm_id(session), role_client.client_batch_id),
            (view_contract_role, firm_id, create_role_client(session).client_batch_id),
        ],
    )
    contract = create_contract(session, firm=firm_id, client=role_client.client)
    with pytest.raises(exc.PERMISSION_DENIED):
        contract.check_access(passport)


def test_perm_wrong_client(session, role_client, view_contract_role):
    """Для ограниченной роли с правом ViewContracts указан другой клиент"""
    passport = create_passport(session, [(view_contract_role, None, create_role_client(session).client_batch_id)])
    contract = create_contract(session, client=role_client.client)
    with pytest.raises(exc.PERMISSION_DENIED):
        contract.check_access(passport)

