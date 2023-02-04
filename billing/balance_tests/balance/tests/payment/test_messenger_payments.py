# coding: utf-8
from decimal import Decimal as D

import attr

import pytest

from balance import balance_steps as steps
from balance.features import Features
from btestlib import reporter
from btestlib import utils, matchers
from btestlib.constants import TransactionType, PaysysType, PaymentType
from btestlib.data import simpleapi_defaults
from simpleapi.data.defaults import Fiscal
from btestlib.data.partner_contexts import MESSENGER_CONTEXT
from simpleapi.common.payment_methods import TrustWebPage, Via, VirtualPromocode, TYPE as FullPaymentType
from simpleapi.data.cards_pool import Tinkoff

pytestmark = [
    reporter.feature(Features.TRUST, Features.PAYMENT),
    pytest.mark.tickets('BALANCE-28987'),
    pytest.mark.usefixtures("switch_to_pg")
]

# компенсаций нет https://st.yandex-team.ru/OEBS-21488#1536759931000

SERVICE = MESSENGER_CONTEXT.service
PRODUCT_ID = 509336
AMOUNT = simpleapi_defaults.DEFAULT_PRICE


def card_paymethod():
    return TrustWebPage(Via.card(Tinkoff.Valid.card_mastercard, unbind_before=False), in_browser=False)


def promocode_paymethod():
    return VirtualPromocode()


paymethod_constructors = {
    PaymentType.CARD: card_paymethod,
    PaymentType.NEW_PROMOCODE: promocode_paymethod,
}


@pytest.fixture(autouse=True)
def mock_trust(mock_simple_api):
    pass


@attr.s
class Case(object):
    partner_commission = attr.ib(type=D)
    payment_type = attr.ib()
    commission_category = attr.ib(type=int, default=None)
    old_case = attr.ib(type=bool, default=False)

    @property
    def get_pct(self):
        return self.partner_commission if self.commission_category is None else D(self.commission_category) / 100

    @property
    def paymethod(self):
        return paymethod_constructors[self.payment_type]()


# проверка платежа
@pytest.mark.parametrize('test_case', [
    pytest.mark.smoke(Case(2, payment_type=PaymentType.CARD,)),
    pytest.mark.smoke(Case(2, payment_type=PaymentType.CARD, commission_category=100)),
    pytest.mark.smoke(Case(2, payment_type=PaymentType.NEW_PROMOCODE,)),
    Case(0, payment_type=PaymentType.CARD),  # проверка минимального АВ,
    Case(2, payment_type=PaymentType.CARD, old_case=True),
    Case(2, payment_type=PaymentType.NEW_PROMOCODE, commission_category=100),
], ids=lambda t: str(t))
def test_payment_simple(test_case):
    partner_id, contract_id, person_id = create_contract(is_offer=True,
                                                         partner_commission=str(test_case.partner_commission))

    payment_amount = AMOUNT

    # создаем платеж в трасте
    trust_payment_id, payment_id, purchase_token = create_payment(partner_id, amount=payment_amount,
                                                                  commission_category=test_case.commission_category,
                                                                  old_case=test_case.old_case,
                                                                  paymethod=test_case.paymethod,
                                                                  )

    # запускаем обработку платежа
    steps.CommonPartnerSteps.export_payment(payment_id)

    check_payment(payment_id, trust_payment_id, test_case.get_pct, contract_id, partner_id, person_id, payment_amount,
                  paymethod=test_case.paymethod)


parametrize_payment_type = pytest.mark.parametrize('payment_type', [PaymentType.CARD, PaymentType.NEW_PROMOCODE],
                                                ids=lambda x: x.type)


# проверка возврата
@parametrize_payment_type
def test_refund_simple(payment_type):
    test_case = Case(2, payment_type=payment_type)
    partner_id, contract_id, person_id = create_contract(partner_commission=str(test_case.partner_commission))

    payment_amount = AMOUNT

    # создаем платеж в трасте
    trust_payment_id, payment_id, purchase_token = create_payment(partner_id, amount=payment_amount,
                                                                  commission_category=test_case.commission_category,
                                                                  old_case=test_case.old_case,
                                                                  paymethod=test_case.paymethod,
                                                                  )

    # запускаем обработку платежа
    steps.CommonPartnerSteps.export_payment(payment_id)

    # создаем рефанд
    trust_refund_id, refund_id = steps.SimpleNewApi.create_refund(SERVICE, purchase_token)

    # запускаем обработку рефанда
    steps.CommonPartnerSteps.export_payment(refund_id)

    check_payment(payment_id, trust_payment_id, test_case.get_pct, contract_id, partner_id, person_id, payment_amount,
                  trust_refund_id, paymethod=test_case.paymethod)


@parametrize_payment_type
@pytest.mark.parametrize("service_fee", [None, -1])
def test_service_fee_to_product_mapping(service_fee, payment_type):
    test_case = Case(2, payment_type=payment_type)
    payment_amount = AMOUNT

    partner_id, contract_id, person_id = create_contract(partner_commission=str(test_case.partner_commission))

    # создаем платеж в трасте
    trust_payment_id, payment_id, purchase_token = create_payment(partner_id,
                                                                  amount=payment_amount,
                                                                  service_fee=service_fee,
                                                                  commission_category=test_case.commission_category,
                                                                  old_case=test_case.old_case,
                                                                  paymethod=test_case.paymethod,
                                                                  )
    steps.CommonPartnerSteps.export_payment(payment_id)
    expected_product_id = expected_product_id_by_service_fee(service_fee)

    check_payment(payment_id=payment_id,
                  trust_payment_id=trust_payment_id,
                  commission_pct=test_case.get_pct,
                  contract_id=contract_id,
                  partner_id=partner_id,
                  person_id=person_id,
                  payment_amount=payment_amount,
                  expected_product_id=expected_product_id,
                  paymethod=test_case.paymethod,
                  )


# ----- utils

def create_contract(is_offer=True, partner_commission=2):
    partner_id = steps.SimpleNewApi.create_partner(SERVICE)
    _, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(MESSENGER_CONTEXT, client_id=partner_id,
                                                                               is_offer=is_offer,
                                                                               additional_params={
                                                                                   'partner_commission_pct2': partner_commission})
    return partner_id, contract_id, person_id


def create_payment(
        partner_id,
        amount=None, service_fee=None, commission_category=None,
        old_case=False,
        paymethod=None,
):
    product_id = steps.SimpleNewApi.create_product(SERVICE, partner_id, service_fee=service_fee)

    # для мессенджера обязательно нужен такой параметр (с) slppls
    pass_params = {"submerchantIdRbs": 63067}

    if old_case:
        payment_params = dict(product_id=product_id, amount=amount, paymethod=paymethod, pass_params=pass_params)
    else:
        order_structure = {
            'currency': MESSENGER_CONTEXT.currency.iso_code,
            'fiscal_nds': Fiscal.NDS.nds_none,
            'fiscal_title': Fiscal.fiscal_title
        }
        orders = steps.SimpleNewApi.create_multiple_orders_for_payment(
            SERVICE,
            product_id_list=[product_id],
            commission_category_list=None if commission_category is None else [str(commission_category)],
            amount_list=[amount],
            orders_structure=[order_structure]
        )
        payment_params = dict(orders=orders, paymethod=paymethod, pass_params=pass_params)

    trust_payment_id, payment_id, purchase_token = steps.SimpleNewApi.create_payment(SERVICE, **payment_params)
    return trust_payment_id, payment_id, purchase_token


def check_payment(payment_id, trust_payment_id, commission_pct, contract_id, partner_id,
                  person_id, payment_amount=AMOUNT, trust_refund_id=None, payment_not_exported=False,
                  expected_product_id=None, paymethod=None,):
    amount = utils.dround2(payment_amount)

    if amount == 0 or payment_not_exported:
        expected_payment_lines = []
    else:
        reward = calc_reward(amount, commission_pct) if not trust_refund_id else None

        additional_params = {}
        if FullPaymentType.VIRTUAL_PROMOCODE == paymethod.type:
            additional_params['payment_type'] = PaymentType.NEW_PROMOCODE
            additional_params['paysys_type_cc'] = PaysysType.NEW_PROMOCODE
        if expected_product_id is not None:
            additional_params['product_id'] = expected_product_id

        expected_payment_lines = steps.SimpleApi.create_expected_tpt_row(
            MESSENGER_CONTEXT, partner_id, contract_id,
            person_id, trust_payment_id, payment_id, trust_refund_id,
            amount=amount, yandex_reward=reward, **additional_params)

    # получаем данные по платежу
    actual_payment_lines = \
        steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(
            payment_id=payment_id,
            transaction_type=TransactionType.REFUND if trust_refund_id else TransactionType.PAYMENT)[0]

    # Проверяем данные платежа
    utils.check_that(actual_payment_lines,
                     matchers.has_entries_casted(expected_payment_lines),
                     u'Проверяем данные платежа')


def calc_reward(payment_amount, commission_pct):
    min_reward = D('0.01')
    amount = utils.dround2(payment_amount)
    reward = utils.dround2(utils.pct_sum(amount, commission_pct))
    return max(min_reward, reward)


def expected_product_id_by_service_fee(service_fee):
    """Из конфигурации сервиса вытаскиваем product_id для данного service_fee"""
    service_fee_config = steps.CommonPartnerSteps.get_product_mapping_config(MESSENGER_CONTEXT.service)
    service_fee_config = service_fee_config['service_fee_product_mapping'][MESSENGER_CONTEXT.payment_currency.iso_code]
    if str(service_fee) in service_fee_config:
        return service_fee_config[str(service_fee)]
    else:
        return service_fee_config['default']
