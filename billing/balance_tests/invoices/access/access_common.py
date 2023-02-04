# -*- coding: utf-8 -*-
import pytest

from balance import mapper
from balance.constants import (
    Enum,
    FirmId,
)

from tests import object_builder as ob

PERMISSION = 'Permission'
YAMONEY_SUPPORT_ACCESS = 'YaMoneySupportAccess'
VIEW_INVOICES = 'ViewInvoices'
VIEW_ALL_INVOICES = 'ViewAllInvoices'


class CheckType(Enum):
    object = 'object'
    query = 'query'


def create_passport(session, roles, client=None, patch_session=True, **kwargs):
    passport = ob.create_passport(session, *roles, patch_session=patch_session, client=client, **kwargs)
    return passport


def create_manager(session, passport=None):
    return ob.SingleManagerBuilder(
        passport_id=passport and passport.passport_id,
        domain_login=passport and passport.login).build(session).obj


def create_role(session, *permissions):
    return ob.create_role(session, *permissions)


def create_client(session):
    return ob.ClientBuilder().build(session).obj


@pytest.fixture(name='firm_id')
def create_firm_id(session):
    return ob.FirmBuilder.construct(session).id


@pytest.fixture(name='role_client')
def create_role_client(session, client=None):
    return ob.RoleClientBuilder.construct(
        session,
        client=client or create_client(session),
    )


def create_request(session, firm_id=None, manager=None, client=None):
    client = client or create_client(session)

    return ob.RequestBuilder(
        firm_id=firm_id,
        basket=ob.BasketBuilder(
            client=client,
            rows=[ob.BasketItemBuilder(
                order=ob.OrderBuilder(client=client, manager=manager),
                quantity=666,
            )]
        )
    ).build(session).obj


@pytest.fixture(name='invoice')
def create_invoice(session, firm_id=FirmId.YANDEX_OOO, manager=None, client=None):
    request = create_request(session, firm_id, manager, client=client)
    person = ob.PersonBuilder(client=request.client).build(session).obj

    invoice = ob.InvoiceBuilder(
        person=person,
        request=request,
        client=request.client,
    ).build(session).obj
    return invoice


def create_act(session, firm_id=FirmId.YANDEX_OOO):
    invoice = create_invoice(session, firm_id=firm_id)
    invoice.turn_on_rows()
    order = invoice.invoice_orders[0].order
    order.calculate_consumption(dt=session.now(),
                                shipment_info={order.shipment_type: 333})
    return invoice.generate_act(force=True)[0]


def check_perm_constraints_by_check_type(session, obj, passport, check_type):
    if check_type == CheckType.object:
        return obj.check_perm_constraints(passport, PERMISSION)
    else:
        filters = mapper.Invoice.get_perm_constraints_query_filter(passport, PERMISSION)
        return (
            session.query(mapper.Invoice)
                .filter(mapper.Invoice.id == obj.id,
                        filters)
                .exists()
        )
