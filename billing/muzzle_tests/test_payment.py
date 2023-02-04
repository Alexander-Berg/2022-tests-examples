# -*- coding: utf-8 -*-
import pytest
import hamcrest as hm

from balance import constants as cst, exc, mapper
from balance.corba_buffers import StateBuffer

from tests import object_builder as ob
from tests.tutils import has_exact_entries

SERVICE_ID = cst.ServiceId.DIRECT


@pytest.fixture(name='client')
def create_client(session):
    return ob.ClientBuilder.construct(session)


@pytest.fixture(name='role_client')
def create_role_client(session, client=None):
    return ob.RoleClientBuilder.construct(
        session,
        client=client or create_client(session),
    )


@pytest.fixture(name='edit_persons_role')
def create_edit_persons_role(session):
    return ob.create_role(session, (cst.PermissionCode.EDIT_PERSONS, {cst.ConstraintTypes.client_batch_id: None}))


@pytest.fixture(name='manage_rbs_role')
def create_manage_rbs_role(session):
    return ob.create_role(session, (cst.PermissionCode.MANAGE_RBS_PAYMENTS, {cst.ConstraintTypes.firm_id: None}))


@pytest.fixture(name='view_payments_role')
def create_view_payments_role(session):
    return ob.create_role(session, (cst.PermissionCode.VIEW_PAYMENTS, {cst.ConstraintTypes.firm_id: None}))


@pytest.fixture(name='payment_link_report_role')
def create_payment_link_report_role(session):
    return ob.create_role(session, cst.PermissionCode.PAYMENT_LINKS_REPORT)


def create_payment(session, firm_id=None):
    invoice = ob.InvoiceBuilder.construct(session, firm_id=firm_id)
    firm = firm_id and session.query(mapper.Firm).getone(firm_id)
    payment_method = ob.Getter(mapper.PaymentMethod, 1101)  # card
    processing = ob.Getter(mapper.Processing, 50501)  # trust api
    terminal = ob.TerminalBuilder.construct(
        session,
        firm=firm,
        currency='RUR',
        processing=processing,
        payment_method=payment_method,
    )
    payment = ob.CardPaymentBuilder.construct(
        session,
        invoice=invoice,
    )
    payment.firm = firm
    payment.set_terminal(terminal)
    session.flush()
    return payment


def create_base_payment(session, payment_type, inv=None, firm_id=None):
    inv = inv or ob.InvoiceBuilder.construct(session, firm_id=firm_id)
    payment = ob.GenericBuilder(payment_type, invoice=inv).build(session).obj
    payment.firm_id = firm_id
    session.flush()
    return payment


def create_payment_cash_payment_fact(payment):
    cpf = ob.OebsCashPaymentFactBuilder(
        amount=payment.amount,
        invoice=payment.invoice,
        operation_type=cst.OebsOperationType.ACTIVITY,
        orig_id=payment.id,
    ).build(payment.session).obj
    payment.session.expire_all()  # триггер
    return cpf


def create_paypal_payment(session, invoice=None, firm_id=None):
    return create_base_payment(session, mapper.PayPalPayment, invoice, firm_id)


def create_ym_payment(session, invoice=None, firm_id=None):
    payment = ob.YandexMoneyPaymentBuilder.construct(session, invoice=invoice or ob.InvoiceBuilder.construct(session))
    payment.firm_id = firm_id
    session.flush()
    return payment


def create_card_payment(session, invoice=None, firm_id=None):
    invoice = invoice or ob.InvoiceBuilder.construct(session, firm_id=firm_id)
    firm = firm_id and session.query(mapper.Firm).getone(firm_id)
    payment_method = ob.Getter(mapper.PaymentMethod, 1101)  # card
    processing = ob.Getter(mapper.Processing, 50501)  # trust api
    terminal = ob.TerminalBuilder.construct(
        session,
        firm=firm,
        currency='RUR',
        processing=processing,
        payment_method=payment_method,
    )
    payment = ob.CardPaymentBuilder.construct(
        session,
        invoice=invoice,
    )
    payment.firm = firm
    payment.set_terminal(terminal)
    session.flush()
    return payment


def create_trust_payment(session, invoice=None, firm_id=None):
    invoice = invoice or ob.InvoiceBuilder.construct(session, firm_id=firm_id)
    firm = firm_id and session.query(mapper.Firm).getone(firm_id)
    payment_method = ob.Getter(mapper.PaymentMethod, 1101)  # card
    processing = ob.Getter(mapper.Processing, 50501)  # trust api
    terminal = ob.TerminalBuilder.construct(
        session,
        firm=firm,
        currency='RUR',
        processing=processing,
        payment_method=payment_method,
    )
    payment = ob.TrustPaymentBuilder.construct(
        session,
        invoice=invoice,
    )
    payment.start_dt = payment.dt
    payment.firm = firm
    payment.set_terminal(terminal)
    session.flush()
    return payment


def create_trust_api_payment(session, invoice):
    payment = create_base_payment(session, mapper.TrustApiPayment, invoice)
    payment.start_dt = payment.dt
    return payment


def create_wm_payment(session, invoice=None, firm_id=None):
    return create_base_payment(session, mapper.WebMoneyPayment, invoice, firm_id)


def create_rbs_payment(session, invoice=None, firm_id=None):
    payment = create_base_payment(session, mapper.RBSPayment, invoice, firm_id)
    payment.register = ob.RBSRegisterBuilder.construct(session)
    session.flush()
    return payment


def create_sw_payment(session, invoice=None, firm_id=None):
    return create_base_payment(session, mapper.SixPaymentRUR, invoice, firm_id)


def create_tur_payment(session, invoice=None, firm_id=None):
    return create_base_payment(session, mapper.NestpayNatural, invoice, firm_id)


@pytest.mark.parametrize(
    'w_report_role, role_service_ids, res_service_id',
    [
        pytest.param(True, [], str(SERVICE_ID), id='w PaymentLinkReport role'),
        pytest.param(False, [], '-100500', id='wo services'),
        pytest.param(False, [666], '666', id='w service'),
        pytest.param(False, [333, 666], '-100500',  id='allowed w services'),
    ],
)
def test_payment_link_replace_service(
        session,
        muzzle_logic,
        payment_link_report_role,
        w_report_role,
        role_service_ids,
        res_service_id,
):
    roles = []

    if w_report_role:
        roles.append(payment_link_report_role)
    for service_id in role_service_ids:
        roles.append(ob.create_role(session, 'PaymentLinks%s' % service_id))

    ob.set_roles(session, session.passport, roles)
    session.flush()

    state_obj = StateBuffer(
        params={
            'req_service_id': str(SERVICE_ID),
            'req_service_order_id': '-12345',
        }
    )

    response = muzzle_logic.payment_links_report(session, state_obj)
    assert response.find('request/service_id').text == res_service_id


@pytest.mark.parametrize(
    'is_owner, has_perm',
    [
        pytest.param(True, False, id='owner'),
        pytest.param(False, True, id='has_perm'),
        pytest.param(False, False, id='nobody'),
    ],
)
def test_process_payment_choice_permissions(session, muzzle_logic, edit_persons_role, role_client, is_owner, has_perm):
    if is_owner:
        session.passport.client = role_client.client

    roles = [(edit_persons_role, {cst.ConstraintTypes.client_batch_id: role_client.client_batch_id})] if has_perm else []
    ob.set_roles(session, session.passport, roles)

    params = dict(
        session=session,
        client_id=role_client.client.id,
        person_id=-1,
        contract_id=-1,
        endbuyer_id=-1,
        person_type='ph',
        state_obj=StateBuffer(params={
            'req_lname': 'l name',
            'req_fname': 'f name',
            'req_mname': 'm name',
            'req_email': 'test-test@email.com',
            'req_phone': '89998889900',
        }),
    )
    if is_owner or has_perm:
        response = muzzle_logic.process_payment_choice(**params)
        assert response.attrib == {'id': 'set_person'}
        person_xml = response.find('person')
        assert person_xml.find('email').text == 'test-test@email.com'
        assert person_xml.find('lname').text == 'l name'
        assert person_xml.find('mname').text == 'm name'
        assert person_xml.find('phone').text == '89998889900'
    else:
        with pytest.raises(exc.PERMISSION_DENIED):
            muzzle_logic.process_payment_choice(**params)


def test_filter_payments_by_perm(session, muzzle_logic, manage_rbs_role):
    roles = [
        (manage_rbs_role, {cst.ConstraintTypes.firm_id: cst.FirmId.YANDEX_OOO}),
        (manage_rbs_role, {cst.ConstraintTypes.firm_id: cst.FirmId.DRIVE}),
    ]
    ob.set_roles(session, session.passport, roles)

    payments = [
        create_payment(session, firm_id=None),
        create_payment(session, firm_id=cst.FirmId.YANDEX_OOO),
        create_payment(session, firm_id=cst.FirmId.TAXI),
        create_payment(session, firm_id=cst.FirmId.DRIVE),
        create_payment(session, firm_id=cst.FirmId.BUS),
    ]
    invalid_payment = create_payment(session, firm_id=None)

    params = {'req_payment_%s' % p.id: str(p.id) for p in payments}
    params['req_paymen_%s' % invalid_payment.id] = str(invalid_payment.id)
    state_obj = StateBuffer(
        params=params,
    )
    res = muzzle_logic.filter_payments_by_perm(session, cst.PermissionCode.MANAGE_RBS_PAYMENTS, state_obj)

    p_ids = state_obj.to_dict('req_')
    params_match = {'payment_%s' % p.id: str(p.id) for p in [payments[0], payments[1], payments[3]]}
    params_match['paymen_%s' % invalid_payment.id] = str(invalid_payment.id)
    hm.assert_that(
        p_ids,
        has_exact_entries(params_match),
    )


@pytest.mark.parametrize(
    'muzzle_func, create_func',
    [
        pytest.param('find_ccard_tur_payments_2', create_tur_payment, id='tur_card'),
        pytest.param('find_ccard_sw_payments_2', create_sw_payment, id='sw_card'),
        pytest.param('find_webmoney_payments_2', create_wm_payment, id='webmoney'),
        pytest.param('find_yamoney_payments_2', create_ym_payment, id='yamoney'),
        pytest.param('find_paypal_payments_2', create_paypal_payment, id='paypal'),
        pytest.param('find_cc_payments_2', create_rbs_payment, id='rbs'),
        pytest.param('find_trust_payments', create_trust_payment, id='trust'),
        pytest.param('find_payments', create_card_payment, id='all_payments'),
    ],
)
def test_empty_filters(
    session,
    muzzle_logic,
    view_payments_role,
    muzzle_func,
    create_func,
):
    """При наличие ограничения по правам, но без переданных параметров поиска,
    ничего не ищем"""
    firm_id = cst.FirmId.DRIVE
    roles = [
        (view_payments_role, {cst.ConstraintTypes.firm_id: firm_id}),
    ]
    ob.set_roles(session, session.passport, roles)

    payment = create_func(session, firm_id=firm_id)

    state_obj = StateBuffer(
        params={},
    )
    res = getattr(muzzle_logic, muzzle_func)(session, state_obj)
    assert res.findall('entry') == []
