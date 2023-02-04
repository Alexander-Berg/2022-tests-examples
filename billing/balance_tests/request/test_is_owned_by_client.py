# -*- coding: utf-8 -*-
import pytest
from balance.mapper import RoleClientPassport, Service
from balance import exc
from balance.constants import ServiceId, RoleType
from tests.balance_tests.request.request_common import (
    create_request,
    create_passport,
    create_role,
    create_order,
    create_client,
)


def test_passport_wo_client(session):
    request = create_request(session)
    passport = create_passport(session, create_role(session), patch_session=True,
                               client=None)

    assert request.is_owned_by_client(passport) is False


def test_passport_w_client(session):
    request = create_request(session)
    passport = create_passport(session, create_role(session), patch_session=True,
                               client=request.client)

    assert request.is_owned_by_client(passport) is True


def test_passport_w_agency(session, client):
    agency = create_client(session, is_agency=1)
    request = create_request(session, firm_id=None, client=agency,
                             orders=[create_order(session, client=client, agency=agency,
                                                  service=session.query(Service).getone(ServiceId.DIRECT))])
    passport = create_passport(session, create_role(session), patch_session=True,
                               client=request.client)

    assert request.is_owned_by_client(passport) is True


@pytest.mark.parametrize('representative_in_order', [True, False])
def test_passport_w_agency_limited_check(session, representative_in_order):
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
        assert request.is_owned_by_client(passport) is True
    else:
        with pytest.raises(exc.PERMISSION_DENIED_DIRECT_LIMITED) as exc_info:
            request.is_owned_by_client(passport)
        assert exc_info.value.msg == 'User {uid} has no rights to access request {request}' \
                                     ' due to DirectLimited perm.'.format(uid=passport.passport_id, request=request.id)
