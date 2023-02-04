# -*- coding: utf-8 -*-
import datetime
import pickle

import pytest
from mock import MagicMock
from typing import Any, List, Optional

from balance import mapper, core
from balance.actions import acts as a_a
from balance.constants import FirmId, ServiceId, MONTHLY_ACT_REPORT_MESSAGE_OPCODE
from balance.mapper import EmailMessage, Contract, Act
from butils.dbhelper.session import Session
from cluster_tools.monthly_act_report import MailGen, AG_FIRMS
from tests import object_builder as ob


def create_y_invoice(session, firm_id):
    person = ob.PersonBuilder.construct(session, type='sw_ur')
    contract = ob.ContractBuilder(
        client=person.client,
        person=person,
        commission=0,
        firm=firm_id,
        postpay=1,
        personal_account=1,
        personal_account_fictive=1,
        payment_type=3,
        payment_term=30,
        credit=3,
        credit_limit_single='9' * 20,
        services={7, 11, 35},
        is_signed=session.now(),
    ).build(session).obj
    contract.client.is_agency = 1
    orders = [ob.OrderBuilder(product=ob.Getter(mapper.Product, 1475),
                              service=ob.Getter(mapper.Service, 7),
                              client=ob.ClientBuilder.construct(session),
                              agency=contract.client
                              ).build(session).obj]
    basket = ob.BasketBuilder(
        client=contract.client,
        rows=[
            ob.BasketItemBuilder(order=o, quantity=qty)
            for order in orders for o, qty in [(order, 10)]
        ]
    )
    request = ob.RequestBuilder(basket=basket).build(session).obj
    coreobj = core.Core(request.session)
    pa, = coreobj.pay_on_credit(
        request_id=request.id,
        paysys_id=1045,
        person_id=contract.person.id,
        contract_id=contract.id
    )
    request.session.flush()
    now = datetime.datetime.now()
    for order in orders:
        order.calculate_consumption(now, {order.shipment_type: 10})
    act, = a_a.ActAccounter(
        pa.client,
        mapper.ActMonth(for_month=now),
        invoices=[pa.id], dps=[],
        force=1
    ).do()
    invoice = act.invoice
    pa.session.flush()
    return invoice


def check_mail(message, person, act, contract, attach_info):
    def fmt_date(d):
        return d.strftime(u'%m/%Y')

    assert message.opcode == MONTHLY_ACT_REPORT_MESSAGE_OPCODE
    assert message.object_id == 0
    assert message.recepient_name == person.name
    assert message.recepient_address == person.email
    data = pickle.loads(message.data)
    if contract:
        contract_subject = ' in accordance with contract No. {} from {}'.format(contract.external_id,
                                                                                fmt_date(contract.col0.dt))
    else:
        contract_subject = ''

    assert data['attach_info'] == attach_info

    with_invoice = any(info['rep_type'] == 'act' for info in data['attach_info'])
    if with_invoice:
        assert data['subject'] == u'Invoice for advertising services for {}{}'.format(
            fmt_date(act.dt),
            contract_subject,
        )
    else:
        assert data['subject'] == u'Electronic report on advertising services rendered' \
                                  u' for {}{}'.format(fmt_date(act.dt), contract_subject)

    assert data['sender'] == ('info-noreply@support.yandex.com', 'Yandex.Balance')
    assert data['needs_registry'] is False


def create_invoice(session, firm_id, postpay, contract=None, paysys_id=1029):
    client = ob.ClientBuilder.construct(session)
    orders = [ob.OrderBuilder.construct(session, service_id=ServiceId.DIRECT, client=client) for _ in range(2)]
    paysys = ob.Getter(mapper.Paysys, paysys_id).build(session).obj
    request = ob.RequestBuilder.construct(
        session,
        basket=ob.BasketBuilder(rows=[ob.BasketItemBuilder(order=order, quantity=1) for order in orders]),
        firm_id=firm_id
    )
    invoice = ob.InvoiceBuilder.construct(
        session,
        paysys_id=paysys.id,
        person=ob.PersonBuilder(client=client, type=paysys.category),
        request=request,
    )
    invoice.postpay = postpay
    if contract:
        contract.client = client
        contract.person = invoice.person
        invoice.contract = contract
    session.flush()
    return invoice


def create_act(session, invoice):
    invoice.turn_on_rows()
    for invoice_order in invoice.invoice_orders:
        invoice_order.order.calculate_consumption(dt=session.now(),
                                                  shipment_info={invoice_order.order.shipment_type: 333})
    return invoice.generate_act(force=True)[0]


# -------------------------
# старые тесты, которые были выпилены в BALANCE-37610, но все еще работают в BALANCE-37734

firms_list = [
    FirmId.YANDEX_INC,
    FirmId.YANDEX_OOO,
    FirmId.YANDEX_EU_AG,
    FirmId.SERVICES_EU_AG,
]


@pytest.mark.parametrize(
    'firm_id',
    [
        AG_FIRMS[0],
    ],
)
@pytest.mark.parametrize(
    'postpay',
    [
        0,
    ]
)
def test_work_erep_needed(session, firm_id, postpay):
    invoice = create_invoice(session, firm_id, postpay)
    act = create_act(session, invoice)
    contract = ob.ContractBuilder.construct(session, client=invoice.client, ctype='GENERAL', firm=firm_id)
    session.flush()
    lookup_mock = MagicMock()
    lookup_mock.render.return_value = {'34': 'd'}
    mail_gen = MailGen(lookup_mock, act.dt, act.dt - datetime.timedelta(days=1))

    mail_gen.work(session, firms=[firm_id])


@pytest.mark.parametrize('firm_id', firms_list)
@pytest.mark.parametrize('postpay', [
    0,
    1
])
@pytest.mark.parametrize('service_id', [
    ServiceId.DIRECT,
    ServiceId.TOLOKA,
])
def test_acts_query(session, firm_id, postpay, service_id):
    invoice = create_invoice(session, firm_id, postpay)
    act = create_act(session, invoice)
    act.rows[0].order.service_id = service_id
    session.flush()
    mail_gen = MailGen(None, act.dt, act.dt - datetime.timedelta(days=1))
    acts = mail_gen._q_acts(session, firms_list).all()

    if firm_id == FirmId.YANDEX_INC and not postpay and service_id != ServiceId.TOLOKA:
        assert not acts

    elif firm_id in [FirmId.YANDEX_INC,
                     FirmId.YANDEX_EU_AG,
                     FirmId.SERVICES_EU_AG]:
        assert len(acts) == 1
        assert act in acts
    else:
        assert not acts


# ------------------------- пачка тестов для BALANCE-37734

def generate_acts_tmp(
    session, firm_id, contract, person_email
):  # type: (Any, int, Optional[Contract], str) -> Act
    invoice = create_invoice(session, firm_id, postpay=0, contract=contract)
    act = create_act(session, invoice)
    person = invoice.person
    person.email = person_email
    session.flush()

    return act


def get_emails_from_session(session):  # type: (Session) -> List[EmailMessage]
    email_like_object_ids = [
        pks
        for klass, pks, _ in list(session.identity_map)
        if issubclass(klass, EmailMessage)
    ]
    if len(email_like_object_ids) == 1:
        return session.query(EmailMessage).filter(
            EmailMessage.id.in_(email_like_object_ids[0]),
        ).all()

    return []


COMMON_FIRM_ID = FirmId.YANDEX_EU_AG


@pytest.fixture(scope='session')
def ag_firm_id():
    assert COMMON_FIRM_ID in AG_FIRMS
    return COMMON_FIRM_ID


def create_need_erep_act(session, ag_firm_id):
    contract = ob.ContractBuilder(
        commission=0,
        firm=ag_firm_id,
        personal_account=1,
        personal_account_fictive=1,
        payment_type=3,
        payment_term=30,
        services={7, 11, 35},
        is_signed=session.now(),
    ).build(session).obj
    act = generate_acts_tmp(
        session,
        ag_firm_id,
        contract,
        'test@yandex-team.ru',
    )
    return act


def create_mocked_mailgen(dt, begin_dt=None):
    if begin_dt is None:
        begin_dt = dt - datetime.timedelta(days=1)

    lookup_mock = MagicMock()
    lookup_mock.render.return_value = "{'34': 'd'}"

    mail_gen = MailGen(lookup_mock, dt, begin_dt)
    emails = []
    original_generate = mail_gen.generate

    def fake_generate(*args, **kwargs):
        email = original_generate(*args, **kwargs)
        emails.append(email)
        return email

    mail_gen.generate = fake_generate
    return mail_gen, emails


def test_need_erep(session, ag_firm_id):
    act = create_need_erep_act(session, ag_firm_id)
    person = act.invoice.person
    firm = act.invoice.firm

    mail_gen, sent_emails = create_mocked_mailgen(act.dt)
    mail_gen.work(session, [firm.id])

    # messages = get_emails_from_session(session)
    assert len(sent_emails) == 1
    assert sent_emails[0].recepient_address == person.email
    assert sent_emails[0].recepient_name == person.name


def test_mixed_persons(session, ag_firm_id):
    # Проверяет, что не происходит кейса, когда имеил уходит
    # в acts[0], как было при старой реализации.
    # inspired by BALANCE-37734

    # y_invoice will have is_contract_erep_needed == False, because of credit
    y_invoice = create_y_invoice(session, ag_firm_id)
    y_invoice.person.email = 'no-erep@yandex-team.ru'
    no_need_erep_act = y_invoice.acts[0]

    need_erep_act = create_need_erep_act(session, ag_firm_id)
    need_erep_act.invoice.person.email = 'need-erep@yandex-team.ru'

    assert need_erep_act.dt <= no_need_erep_act.dt  # a workaround for next line

    document_dt = no_need_erep_act.dt
    mail_gen, sent_emails = create_mocked_mailgen(document_dt, need_erep_act.dt)
    mail_gen.work(session, [ag_firm_id])

    assert len(sent_emails) == 2

    check_mail(
        message=sent_emails[0],
        person=no_need_erep_act.invoice.person,
        act=no_need_erep_act,
        contract=no_need_erep_act.invoice.contract,
        attach_info=[{
            'p_type': 'pdf',
            'rep_type': 'act',
            'object_id': no_need_erep_act.id,
        }]
    )

    check_mail(
        message=sent_emails[1],
        person=need_erep_act.invoice.person,
        act=need_erep_act,
        contract=need_erep_act.invoice.contract,
        attach_info=[{
            'p_type': 'pdf',
            'rep_type': 'contr_erep',
            'object_id': need_erep_act.invoice.contract.id,
            'dt': no_need_erep_act.dt,
        }]
    )

# Тесты, которые почему-то не переписаны в BALANCE-37610
# @pytest.mark.parametrize('firm_id', [
#     FirmId.YANDEX_INC,
#     FirmId.YANDEX_EU_AG,
#     FirmId.SERVICES_EU_AG
# ])
# @pytest.mark.parametrize('postpay', [
#     0,
#     1
# ])
# @pytest.mark.parametrize('w_contract', [
#     0,
#     1
# ])
# @pytest.mark.parametrize('service_id', [
#     ServiceId.DIRECT,
#     ServiceId.TOLOKA])
# def test_m_acts(session, firm_id, postpay, w_contract, service_id):
#     """
#     В рассылке по актам к постоплатным/предоплатным счетам в Yandex Inc по Толоке и к постоплатным счетам швейцарских
#     фирм шлем и electronic report, и счет
#
#     Всем остальным - только electronic report.
#     """
#     invoice = create_invoice(session, firm_id, postpay)
#     act = create_act(session, invoice)
#     if w_contract:
#         contract = ob.ContractBuilder(client=invoice.client, ctype='GENERAL').build(session).obj
#     else:
#         contract = None
#     act.rows[0].order.service_id = service_id
#     session.flush()
#     lookup_mock = MagicMock()
#     lookup_mock.render.return_value = {'34': 'd'}
#     mail_gen = MailGen(lookup_mock, act.dt, act.dt - datetime.timedelta(days=1))
#     try:
#         mail_gen._m_acts(session, [act], contract, invoice.credit)
#     except AssertionError:
#         messages = [object for object in session.new if isinstance(object, mapper.EmailMessage)]
#         assert firm_id == FirmId.YANDEX_INC and postpay == 0 and service_id != ServiceId.TOLOKA
#         assert len(messages) == 0
#     else:
#         messages = [object for object in session.new if isinstance(object, mapper.EmailMessage)]
#         assert len(messages) == 1
#         toloka_inc = (service_id == ServiceId.TOLOKA and firm_id == FirmId.YANDEX_INC)
#         postpay_in_AG = (postpay and act.invoice.firm_id in AG_FIRMS)
#
#         if toloka_inc or postpay_in_AG:
#             attach_info = [
#                 {'object_id': act.id,
#                  'p_type': 'pdf',
#                  'rep_type': 'erep'
#                  },
#
#                 {'object_id': act.id,
#                  'p_type': 'pdf',
#                  'rep_type': 'act'
#                  },
#             ]
#         elif postpay:
#             attach_info = [{'object_id': act.id,
#                             'p_type': 'pdf',
#                             'rep_type': 'act'
#                             }]
#         else:
#             attach_info = [
#                 {'object_id': act.id,
#                  'p_type': 'pdf',
#                  'rep_type': 'erep'
#                  }]
#
#         check_mail(messages[0], person=invoice.person, act=act, contract=contract, attach_info=attach_info)
#
#
# @pytest.mark.parametrize('firm_id', [
#     FirmId.YANDEX_INC,
#     FirmId.YANDEX_EU_AG,
#     FirmId.SERVICES_EU_AG])
# @pytest.mark.parametrize('postpay', [
#     0,
#     1
# ])
# def test_m_contract(session, firm_id, postpay):
#     invoice = create_invoice(session, firm_id, postpay)
#     act = create_act(session, invoice)
#     contract = ob.ContractBuilder.construct(session, client=invoice.client, ctype='GENERAL', firm=firm_id)
#     session.flush()
#     lookup_mock = MagicMock()
#     lookup_mock.render.return_value = {'34': 'd'}
#     mail_gen = MailGen(lookup_mock, act.dt, act.dt - datetime.timedelta(days=1))
#     if firm_id == FirmId.YANDEX_INC:
#         try:
#             mail_gen._m_contract(session, contract)
#         except AssertionError:
#             pass
#     else:
#         mail_gen._m_contract(session, contract)
#         messages = [object for object in session.new if isinstance(object, mapper.EmailMessage)]
#         assert len(messages) == 1
#
#         attach_info = [{'dt': act.dt,
#                         'object_id': contract.id,
#                         'p_type': 'pdf',
#                         'rep_type': 'contr_erep'}]
#         check_mail(messages[0], person=invoice.person, act=act, contract=contract, attach_info=attach_info)
