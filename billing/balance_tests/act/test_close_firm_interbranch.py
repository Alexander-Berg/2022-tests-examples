# -*- coding: utf-8 -*-
import copy

import datetime
from decimal import Decimal as D

import mock
from balance import mapper
from balance import constants as cst
from balance import muzzle_util as ut
from balance.actions.close_month import MonthFirmInterbranchCloser

from tests import object_builder as ob

TODAY = ut.trunc_date(datetime.datetime.now())
YESTERDAY = TODAY - datetime.timedelta(1)


@classmethod
def curr_rate_mock(cls, session, currency, dat, rate_src_id=None):
    class RateMock(object):
        def __init__(self, rate=D(666)):
            self.rate = rate if currency == 'EUR' else D(13)
            self.rate_dt = TODAY

    return RateMock()


def create_act(session, client, person, dt=None, firm_id=cst.FirmId.YANDEX_OOO, contract=None, product=None, paysys_id=ob.PAYSYS_ID):
    dt = dt or session.now()
    order = ob.OrderBuilder(
        dt=dt,
        client=client,
        product=product if product else ob.Getter(mapper.Product, cst.DIRECT_PRODUCT_RUB_ID) # ob.Getter(mapper.Product, 511648)
    ).build(session).obj
    invoice = ob.InvoiceBuilder(
        person=person,
        contract=contract,
        dt=dt,
        paysys_id=paysys_id,
        request=ob.RequestBuilder(
            firm_id=firm_id,
            basket=ob.BasketBuilder(
                client=client,
                rows=[ob.BasketItemBuilder(order=order, quantity=D('10000'))],
            ),
        ),
    ).build(session).obj
    invoice.turn_on_rows()
    invoice.close_invoice(dt)


def test_close_firm_interbranch(session):
    """Тестирование генерации актов для межфилиального расчета
    https://wiki.yandex-team.ru/balance/docs/process/crossfirm/

    1. Создаем счет и акт на firm
    2. Создаем договор на root_firm с указанием клиента и плательщика
    3. Создаем связь FirmInterbranchBuilder между firm и root_firm, связаных созданным договором
    4. Инициируем генерацию актов для межфилиального расчета
    5. Результат - счет и акт на root_firm с тем же клиентом и той же суммой

    PS: генерация актов инициируется на конец месяца за предыдущий месяц
    => счет и акт должны попадать в генерации за запрашиваемый месяц,
    все расчеты должны быть внутри срока действия договора
    """
    a_month = mapper.ActMonth(for_month=session.now())

    root_firm = ob.Getter(mapper.Firm, cst.FirmId.YANDEX_OOO).build(session).obj
    firm = ob.FirmBuilder().build(session).obj

    interco_client = ob.ClientBuilder().build(session).obj
    interco_person = ob.PersonBuilder(client=interco_client, person_type='ur').build(session).obj
    contract = ob.ContractBuilder(
        firm=root_firm.id,
        is_signed=a_month.begin_dt - datetime.timedelta(days=10),
        finish_dt=a_month.begin_dt + datetime.timedelta(days=300),
        calc_defermant=1,
        commission=0,
        postpay=1,
        personal_account=1,
        personal_account_fictive=1,
        payment_type=3,
        payment_term=30,
        credit=3,
        client=interco_client,
        person=interco_person,
        credit_limit_single='9' * 20,
        services={7, 11, 35},
        discount_fixed=20
    ).build(session).obj

    fb = ob.FirmInterbranchBuilder(
        firm=firm,
        root_firm=root_firm,
        contract=contract,
        invoice_paysys_id=ob.PAYSYS_ID,
    ).build(session).obj

    client = ob.ClientBuilder(is_agency=True, full_repayment=1).build(session).obj
    person = ob.PersonBuilder(client=client, person_type='ur').build(session).obj
    create_act(session, client, person, firm_id=firm.id, dt=session.now())

    fb_closer = MonthFirmInterbranchCloser(session, fb, a_month)
    root_invoice, = fb_closer.close()

    assert root_invoice.crossfirm is True
    assert len(root_invoice.acts) == 1
    assert root_invoice.total_sum == D('6666.66')


@mock.patch(
    'balance.mapper.common.CurrencyRate.get_currency_rate_by_date',
    curr_rate_mock
)
def test_close_firm_interbranch_currency_config(session):
    a_month = mapper.ActMonth(for_month=session.now())

    tax_policy = ob.TaxPolicyBuilder.construct(
        session, tax_pcts=[0], region_id=126, resident=0
    )

    root_firm = ob.Getter(mapper.Firm, cst.FirmId.YANDEX_OOO).build(session).obj
    firm = ob.FirmBuilder(
        country=ob.Getter(mapper.Country, 126), default_iso_currency='EUR'
    ).build(session).obj

    product = ob.ProductBuilder.construct(session, create_taxes=None, create_price=None)

    ob.PriceBuilder(
        product=product,
        dt=YESTERDAY,
        price=1,
        currency='TRY',
        iso_currency='TRY'
    ).build(session)

    ob.TaxBuilder.construct(
        session=session,
        firm_id=firm.id,
        product=product,
        currency=ob.Getter(mapper.Currency, 'TRY'),
        iso_currency='TRY',
        nds_pct=None,
        nds_operation_code_id=None,
        tax_policy_id=tax_policy.id
    )

    interco_client = ob.ClientBuilder().build(session).obj
    interco_person = ob.PersonBuilder(
        client=interco_client, type='ur'
    ).build(session).obj

    contract = ob.ContractBuilder(
        firm=root_firm.id,
        is_signed=a_month.begin_dt - datetime.timedelta(days=10),
        finish_dt=a_month.begin_dt + datetime.timedelta(days=300),
        calc_defermant=1,
        commission=0,
        postpay=1,
        personal_account=1,
        personal_account_fictive=1,
        payment_type=3,
        payment_term=30,
        credit=3,
        client=interco_client,
        person=interco_person,
        credit_limit_single='9' * 20,
        services={7, 11, 35},
        discount_fixed=20
    ).build(session).obj

    fb = ob.FirmInterbranchBuilder(
        firm=firm,
        root_firm=root_firm,
        contract=contract,
        invoice_paysys_id=1023,
    ).build(session).obj

    client = ob.ClientBuilder(is_agency=True, full_repayment=1).build(session).obj
    person = ob.PersonBuilder(client=client, type='sw_yt').build(session).obj
    paysys = ob.PaysysBuilder.construct(
        session,
        cc='yt',
        extern=1,
        firm=firm,
        category=person.type,
        currency='TRY',
        iso_currency='TRY',
        payment_method_id=cst.PaymentMethodIDs.credit_card,
    )

    create_act(
        session,
        client,
        person,
        firm_id=firm.id,
        dt=session.now(),
        product=product,
        paysys_id=paysys.id
    )

    interbranch_curr_map = {
        "rate_src_id": 1001,
        "allowed_curr_to_exchange_rate": ["TRY", "USD"]
    }

    fb_closer = MonthFirmInterbranchCloser(session, fb, a_month, interbranch_curr_map)
    root_invoice, = fb_closer.close()

    assert root_invoice.crossfirm is True
    assert len(root_invoice.acts) == 1
    assert root_invoice.total_sum == D('158.8')
