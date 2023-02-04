# -*- coding: utf-8 -*-
import pytest

from balance import exc
from balance.mapper import RoleClientPassport, Service
from balance.constants import ServiceId, RoleType
from tests.balance_tests.request.request_common import (
    create_request,
    create_passport,
    create_role,
    create_client,
    create_order,
)


@pytest.mark.parametrize('with_client_roles', [True, False])
def test_depend_on_passport_client_roles(session, with_client_roles):
    """Обычный представитель (не ограниченный) имеет доступ ко всем клиентам"""
    agency = create_client(session, is_agency=1)
    passport = create_passport(session, create_role(session), patch_session=False,
                               client=agency)
    representative = create_client(session)
    request = create_request(session, firm_id=None, client=agency,
                             orders=[create_order(session, client=representative, agency=agency,
                                                  service=session.query(Service).getone(ServiceId.DIRECT))])

    session.flush()
    assert request.check_direct_limited_access(passport) is None


@pytest.mark.parametrize('representative_in_order', [True, False])
def test_depend_on_client_in_representative(session, representative_in_order):
    """Если в реквесте есть заказы на Директ, проверяем, что у ограниченного представителя есть 101 роль
     в разрезе клиентов из этих заказов. Если роли нет, кидаем исключение"""
    agency = create_client(session, is_agency=1)
    passport = create_passport(session, create_role(session), patch_session=False,
                               client=agency)
    representative = create_client(session)

    passport._passport_client_roles = [
        RoleClientPassport(
            passport=passport,
            client_id=representative.id if representative_in_order else create_client(session).id,
            role_id=RoleType.DIRECT_LIMITED
        )
    ]
    request = create_request(session, firm_id=None, client=agency,
                             orders=[create_order(session, client=representative, agency=agency,
                                                  service=session.query(Service).getone(ServiceId.DIRECT))])

    session.flush()
    if representative_in_order:
        assert request.check_direct_limited_access(passport) is None
    else:
        with pytest.raises(exc.PERMISSION_DENIED_DIRECT_LIMITED) as exc_info:
            request.check_direct_limited_access(passport)
        assert exc_info.value.msg == 'User {uid} has no rights to access request {request}' \
                                     ' due to DirectLimited perm.'.format(uid=passport.passport_id, request=request.id)


@pytest.mark.parametrize('client_wo_perms_in_orders', [True, False])
def test_all_clients_in_order_are_representative(session, client_wo_perms_in_orders):
    """Если в реквесте есть заказы на Директ, проверяем, что у ограниченного представителя есть 101 роль
     в разрезе клиентов для каждого из таких заказов. В ином случае кидаем исключение"""
    agency = create_client(session, is_agency=1)
    passport = create_passport(session, create_role(session), patch_session=False,
                               client=agency)
    representative = create_client(session)

    passport._passport_client_roles = [
        RoleClientPassport(
            passport=passport,
            client_id=representative.id,
            role_id=RoleType.DIRECT_LIMITED
        )
    ]
    orders_clients = [representative, create_client(session)] if client_wo_perms_in_orders else [representative,
                                                                                                 representative]
    orders = [create_order(session, client=client, agency=agency,
                           service=session.query(Service).getone(ServiceId.DIRECT)) for client in orders_clients]
    request = create_request(session, firm_id=None, client=agency,
                             orders=orders)

    session.flush()
    if not client_wo_perms_in_orders:
        assert request.check_direct_limited_access(passport) is None
    else:
        with pytest.raises(exc.PERMISSION_DENIED_DIRECT_LIMITED) as exc_info:
            request.check_direct_limited_access(passport)
        assert exc_info.value.msg == 'User {uid} has no rights to access request {request}' \
                                     ' due to DirectLimited perm.'.format(uid=passport.passport_id, request=request.id)


@pytest.mark.parametrize('service_id', [ServiceId.DIRECT, ServiceId.MARKET])
def test_depend_on_services(session, service_id):
    """Если в реквесте есть заказы на Директ, проверяем, что у паспорта есть роль огр. представителя
     в разрезе клиентов из этих заказов.
     Заказы других сервисов не проверяем.
     """
    agency = create_client(session, is_agency=1)
    passport = create_passport(session, create_role(session), patch_session=False,
                               client=agency)
    representative = create_client(session)
    passport._passport_client_roles = [
        RoleClientPassport(
            passport=passport,
            client_id=representative.id,
            role_id=RoleType.DIRECT_LIMITED
        )
    ]
    request = create_request(session, firm_id=None, client=agency,
                             orders=[create_order(session, client=representative, agency=agency,
                                                  service=session.query(Service).getone(service_id))])

    session.flush()
    request.check_direct_limited_access(passport)
