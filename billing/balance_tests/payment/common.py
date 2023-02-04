# -*- coding: utf-8 -*-

import pytest
import datetime

from balance import (
    constants as cst,
    exc,
    mapper,
    muzzle_util as ut,
)

import tests.object_builder as ob

QTY = 1
NOW = datetime.datetime.now()
BANK = cst.PaymentMethodIDs.bank

PERMISSION = 'PERMISSION'
ADMIN_ACCESS = cst.PermissionCode.ADMIN_ACCESS
VIEW_PAYMENTS = cst.PermissionCode.VIEW_PAYMENTS


class CheckType(cst.Enum):
    object = 'object'
    query = 'query'


def check_perm_constraints_by_check_type(session, obj, passport, check_type):
    if check_type == CheckType.object:
        return obj.check_perm_constraints(passport, PERMISSION)
    else:
        filters = mapper.Payment.get_perm_constraints_query_filter(passport, PERMISSION)
        return (
            session.query(mapper.Payment)
                .filter(mapper.Payment.id == obj.id,
                        filters)
                .exists()
        )


@pytest.fixture(name='service')
def create_service(session, **kwargs):
    return ob.ServiceBuilder(**kwargs).build(session).obj


@pytest.fixture(name='firm')
def create_firm(session, **kwargs):
    return ob.FirmBuilder(**kwargs).build(session).obj


def create_role(session, *permissions):
    return ob.create_role(session, *permissions)


@pytest.fixture(name='role_client')
def create_role_client(session, client=None):
    return ob.RoleClientBuilder.construct(
        session,
        client=client or create_client(session),
    )


@pytest.fixture(name='yamoney_role')
def create_yamoney_role(session):
    return ob.create_role(session, (cst.PermissionCode.YAMONEY_SUPPORT_ACCESS, {cst.ConstraintTypes.firm_id: None}))


@pytest.fixture(name='view_payments_role')
def create_view_payments_role(session):
    return ob.create_role(
        session,
        (cst.PermissionCode.VIEW_PAYMENTS, {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None}),
    )


@pytest.fixture(name='client')
def create_client(session, **kwargs):
    return ob.ClientBuilder(**kwargs).build(session).obj


@pytest.fixture(name='agency')
def create_agency(session, **kwargs):
    return ob.ClientBuilder(is_agency=1, **kwargs).build(session).obj


def create_passport(session, *roles, **kwargs):
    kwargs['patch_session'] = True
    return ob.create_passport(session, *roles, **kwargs)


@pytest.fixture(name='invoice')
def create_invoice(session, **kwargs):
    return ob.InvoiceBuilder.construct(session, **kwargs)


@pytest.fixture(name='card_payment')
def create_card_payment(session, invoice=cst.SENTINEL, firm_id=None, client=None, **kwargs):
    client = client or create_client(session)
    invoice = create_invoice(session, client=client) if invoice is cst.SENTINEL else invoice
    payment = ob.CardPaymentBuilder.construct(
        session,
        invoice=invoice,
        **kwargs
    )
    payment.firm_id = firm_id
    session.flush()
    return payment
