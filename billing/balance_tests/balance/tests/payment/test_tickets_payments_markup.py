# coding: utf-8

from collections import defaultdict
from dateutil.relativedelta import relativedelta
from decimal import Decimal
from datetime import datetime, timedelta
from hamcrest import has_length, none, contains_string, equal_to, not_none, greater_than_or_equal_to, any_of
import pytest

from balance import balance_steps as steps, balance_api as api
from balance.features import Features
from btestlib import utils
import btestlib.reporter as reporter
from btestlib.data.partner_contexts import \
    TICKETS_118_CONTEXT, EVENTS_TICKETS2_RU_CONTEXT, EVENTS_TICKETS2_KZ_CONTEXT,\
    EVENTS_TICKETS_CONTEXT, EVENTS_TICKETS3_RU_CONTEXT
from btestlib.constants import Services, PaymentType, PaysysType, TransactionType, ServiceFee, PaymentMethods as pm, \
    Collateral
from btestlib.data.simpleapi_defaults import DEFAULT_PRICE, DEFAULT_FEE, STATIC_EMULATOR_CARD
from btestlib.matchers import contains_dicts_with_entries
from check import db
from simpleapi.data.defaults import Discounts, Fiscal
from simpleapi.common.payment_methods import TYPE as PM_TYPE, VirtualPromocode, \
    AfishaCertificate, AfishaFakeRefund, FakeRefundCertificate, \
    CertificatePromocode, MarketingPromocode,\
    TrustWebPage, Via

pytestmark = [
    reporter.feature(Features.TRUST, Features.PAYMENT,
                     Features.EVENTS_TICKETS, Features.EVENTS_TICKETS_NEW,
                     Features.TICKETS,
                     ),
]

START_DT = utils.Date.first_day_of_month() - relativedelta(months=1)
MIN_PAYMENT = Decimal('0.01')
REWARD_VAT_PCT = Decimal('0.1')
# для логики с разделением - переведем проценты в табличные значения (T_PARTNER_COMMISSION_CATEGORY)
COMMISSION_CATEGORY_FOR_SPLITTED_REWARD = str(REWARD_VAT_PCT * 100 * 100)  # 10%
MASTERCARD_DISCOUNT = Discounts.id100
DISCOUNT_COEFFICIENT = utils.fraction_from_percent(-MASTERCARD_DISCOUNT['pct'])

PAYMENT_NO_COMPOSITE_PART_EXPORT_ERROR = 'TrustPayment({payment_id}) delayed: ' \
    'waiting for appearing composite part in t_thirdparty_transactions completely'
REFUND_NO_ORIG_PAYMENT_EXPORT_ERROR = 'Refund({payment_id}) delayed: waiting for appearing original payment in ' \
    't_thirdparty_transactions for refund'
NO_CONTRACT_EXPORT_ERROR = "TrustPayment({payment_id}) delayed: no active contracts found for client {client_id}"
PAYMENT_COMPLETELY_CANCELLED_EXPORT_ERROR = 'TrustPayment({payment_id}) skipped: payment has been completely cancelled'
PAYMENT_MONEY_CANCELLED_EXPORT_ERROR = 'TrustPayment({payment_id}) skipped:' \
                                       ' Money part of composite payment is cancelled or reversed'
REFUND_REVERSAL_IS_NOT_EXPORTABLE_EXPORT_ERROR = 'Refund({payment_id}) skipped: reversal is not exportable'

NO_INVOICE_EID_SIDS = frozenset(s.id for s in (Services.TICKETS, ))

CARD_AMOUNT_1 = 577
CARD_AMOUNT_2 = 2333
PROMOCODE_AMOUNT_1 = 1333
PROMOCODE_AMOUNT_2 = 777
CERTIFICATE_AMOUNT_1 = 1200
FAKE_REFUND_AMOUNT_1 = 688

CARD_PM = PM_TYPE.CARD
PROMOCODE_PM = PM_TYPE.VIRTUAL_PROMOCODE
CERTIFICATE_PROMOCODE_PM = PM_TYPE.CERTIFICATE_PROMOCODE
MARKETING_PROMOCODE_PM = PM_TYPE.MARKETING_PROMOCODE
AFISHA_CERTIFICATE_PM = PM_TYPE.AFISHA_CERTIFICATE
AFISHA_FAKE_REFUND_PM = PM_TYPE.AFISHA_FAKE_REFUND
FAKE_REFUND_CERTIFICATE_PM = PM_TYPE.FAKE_REFUND_CERTIFICATE

NONE_SERVICE_FEE_CODE = 'none_service_fee'
SPECIFIED_SERVICE_FEE_CODE = 'specified_service_fee'

SERVICE_WITH_ZERO_REWARD = frozenset([Services.EVENTS_TICKETS3.id, Services.EVENTS_TICKETS_NEW.id])

one_order_markup_params = [
    # # один заказ все картой
    # pytest.param([CARD_AMOUNT_1], [{CARD_PM: CARD_AMOUNT_1}], {pm.CARD.id: {SPECIFIED_SERVICE_FEE_CODE}},
    #              id='One order, card',
    #              marks=pytest.mark.skip),
    # # один заказ все промокодом
    # pytest.param([PROMOCODE_AMOUNT_1], [{PROMOCODE_PM: PROMOCODE_AMOUNT_1}],
    #              {pm.VIRTUAL.id: {SPECIFIED_SERVICE_FEE_CODE}},
    #              id='One order, promocode',
    #              marks=pytest.mark.skip),
    # один заказ две строки, промокод и карта
    pytest.param([CARD_AMOUNT_1 + PROMOCODE_AMOUNT_1], [{CARD_PM: CARD_AMOUNT_1, PROMOCODE_PM: PROMOCODE_AMOUNT_1}],
                 {pm.CARD.id: {SPECIFIED_SERVICE_FEE_CODE},
                  pm.VIRTUAL.id: {SPECIFIED_SERVICE_FEE_CODE}},
                 id='One order, card and promocode'),
]

two_orders_markup_params = [
    # пропускаем некоторые тесты, т.к тест долгий наша логика тестируется в оставшихся параметрах
    # а взаимодействие с трастом долгое время было стабильно
    # два заказа все картой,
    # pytest.param([CARD_AMOUNT_1, CARD_AMOUNT_2], [{CARD_PM: CARD_AMOUNT_1}, {CARD_PM: CARD_AMOUNT_2}],
    #              {pm.CARD.id: {NONE_SERVICE_FEE_CODE, SPECIFIED_SERVICE_FEE_CODE}},
    #              id='Two order, card',
    #              marks=pytest.mark.skip),
    # # два заказа все промокодом
    # pytest.param([PROMOCODE_AMOUNT_1, PROMOCODE_AMOUNT_2],
    #              [{PROMOCODE_PM: PROMOCODE_AMOUNT_1}, {PROMOCODE_PM: PROMOCODE_AMOUNT_2}],
    #              {pm.VIRTUAL.id: {NONE_SERVICE_FEE_CODE, SPECIFIED_SERVICE_FEE_CODE}},
    #              id='Two order, promocode',
    #              marks=pytest.mark.skip),
    # два заказа две строки промокод и карта
    pytest.param([CARD_AMOUNT_1 + PROMOCODE_AMOUNT_1, CARD_AMOUNT_2 + PROMOCODE_AMOUNT_2],
                 [{CARD_PM: CARD_AMOUNT_1, PROMOCODE_PM: PROMOCODE_AMOUNT_1},
                  {CARD_PM: CARD_AMOUNT_2, PROMOCODE_PM: PROMOCODE_AMOUNT_2}],
                 {pm.CARD.id: {NONE_SERVICE_FEE_CODE, SPECIFIED_SERVICE_FEE_CODE},
                  pm.VIRTUAL.id: {NONE_SERVICE_FEE_CODE, SPECIFIED_SERVICE_FEE_CODE}},
                 id='Two order. First: card, promocode. Second: card, promocode'),
    # # два заказа один промокодом один картой
    # pytest.param([CARD_AMOUNT_1, PROMOCODE_AMOUNT_1],
    #              [{CARD_PM: CARD_AMOUNT_1}, {PROMOCODE_PM: PROMOCODE_AMOUNT_1}],
    #              {pm.CARD.id: {NONE_SERVICE_FEE_CODE},
    #               pm.VIRTUAL.id: {SPECIFIED_SERVICE_FEE_CODE}},
    #              id='Two order. First: card. Second: promocode',
    #              marks=pytest.mark.skip),
    # # два заказа один промокодом и картой, другой только промокодом
    # pytest.param([CARD_AMOUNT_1 + PROMOCODE_AMOUNT_1, PROMOCODE_AMOUNT_2],
    #              [{CARD_PM: CARD_AMOUNT_1, PROMOCODE_PM: PROMOCODE_AMOUNT_1},
    #               {PROMOCODE_PM: PROMOCODE_AMOUNT_2}],
    #              {pm.CARD.id: {NONE_SERVICE_FEE_CODE},
    #               pm.VIRTUAL.id: {NONE_SERVICE_FEE_CODE, SPECIFIED_SERVICE_FEE_CODE}},
    #              id='Two order. First: card, promocode. Second: promocode',
    #              marks=pytest.mark.skip),
    # # два заказа один промокодом и картой, другой только картой
    # pytest.param([CARD_AMOUNT_1 + PROMOCODE_AMOUNT_1, CARD_AMOUNT_2],
    #              [{CARD_PM: CARD_AMOUNT_1, PROMOCODE_PM: PROMOCODE_AMOUNT_1},
    #               {CARD_PM: CARD_AMOUNT_2}],
    #              {pm.CARD.id: {NONE_SERVICE_FEE_CODE, SPECIFIED_SERVICE_FEE_CODE},
    #               pm.VIRTUAL.id: {NONE_SERVICE_FEE_CODE}},
    #              id='Two order. First : card, promocode. Second : card',
    #              marks=pytest.mark.skip
    #              )
]

# тестировать ситуации частичной покупки сертификата не имеет смысла исходя из бизнес-логики
# https://st.yandex-team.ru/BALANCE-34560
certificate_orders_markup_params = [
    # одна строка - все сертификат
    pytest.param([CERTIFICATE_AMOUNT_1],
                 [{AFISHA_CERTIFICATE_PM: CERTIFICATE_AMOUNT_1}],
                 {pm.AFISHA_CERTIFICATE.id: {SPECIFIED_SERVICE_FEE_CODE}},
                 id='One order, afisha_certificate'),
]

parametrize_two_orders_markup_params = pytest.mark.parametrize(
    'amounts, paymethod_markups, expected_fees', two_orders_markup_params)

parametrize_markup_params = pytest.mark.parametrize(
    'amounts, paymethod_markups, expected_fees', one_order_markup_params + two_orders_markup_params)

parametrize_markup_params_single = pytest.mark.parametrize('amounts, paymethod_markups, expected_fees', [
    # два заказа две строки промокод и карта
    pytest.param([CARD_AMOUNT_1 + PROMOCODE_AMOUNT_1, CARD_AMOUNT_2 + PROMOCODE_AMOUNT_2],
                 [{CARD_PM: CARD_AMOUNT_1, PROMOCODE_PM: PROMOCODE_AMOUNT_1},
                  {CARD_PM: CARD_AMOUNT_2, PROMOCODE_PM: PROMOCODE_AMOUNT_2}],
                 {pm.CARD.id: {NONE_SERVICE_FEE_CODE, SPECIFIED_SERVICE_FEE_CODE},
                  pm.VIRTUAL.id: {NONE_SERVICE_FEE_CODE, SPECIFIED_SERVICE_FEE_CODE}},
                 id='Two order. First : card, promocode. Second : card, promocode'),
])


all_contexts = (TICKETS_118_CONTEXT, EVENTS_TICKETS_CONTEXT,
                EVENTS_TICKETS2_RU_CONTEXT, EVENTS_TICKETS2_KZ_CONTEXT, EVENTS_TICKETS3_RU_CONTEXT)

ru_contexts = [ctx for ctx in all_contexts if ctx not in (EVENTS_TICKETS2_KZ_CONTEXT,)]

contexts_wo_reward_splitting = (TICKETS_118_CONTEXT, EVENTS_TICKETS2_KZ_CONTEXT)

events_contexts = (
    EVENTS_TICKETS_CONTEXT, EVENTS_TICKETS3_RU_CONTEXT,
    EVENTS_TICKETS2_RU_CONTEXT, EVENTS_TICKETS2_KZ_CONTEXT
)


def contexts_with_reward_splitting(contexts, one_reward_splitting=False):
    all_reward_splitting_values = [True] if one_reward_splitting else [True, False]
    return [pytest.param(context, with_splitting,
                         id=context.name + (' with ' if with_splitting else ' without ') + 'splitting reward')
            for context in contexts
            for with_splitting in ([False] if context in contexts_wo_reward_splitting else all_reward_splitting_values)]


parametrize_context = pytest.mark.parametrize('context', all_contexts, ids=lambda x: x.name)

parametrize_events_context = pytest.mark.parametrize('context', events_contexts, ids=lambda x: x.name)

parametrize_context_with_reward_splitting = pytest.mark.parametrize(
    'context', [ctx for ctx in all_contexts if ctx not in contexts_wo_reward_splitting], ids=lambda x: x.name)

parametrize_afisha_certificate_context_and_reward_splitting = pytest.mark.parametrize(
    'context, with_reward_splitting', contexts_with_reward_splitting(ru_contexts, one_reward_splitting=True))

parametrize_context_and_one_reward_splitting = pytest.mark.parametrize(
    'context, with_reward_splitting', contexts_with_reward_splitting(all_contexts, one_reward_splitting=True))

parametrize_events_context_and_reward_splitting = pytest.mark.parametrize(
    'context, with_reward_splitting', contexts_with_reward_splitting(events_contexts))

parametrize_118_no_markup_tests = pytest.mark.parametrize(
    'service_fee, commission_category, amount',
    [
        pytest.param(ServiceFee.SERVICE_FEE_NONE, '100', '10000.01', id='Min payment, amount > 0.01',
                     marks=pytest.mark.smoke),
        pytest.param(ServiceFee.SERVICE_FEE_2, '100', '10000.01', id='No min payment, amount > 0.01'),
        pytest.param(ServiceFee.SERVICE_FEE_NONE, '1', '9.01', id='Min payment, amount < 0.01'),
        pytest.param(ServiceFee.SERVICE_FEE_2, '1', '9.01', id='No min payment, amount < 0.01'),
        pytest.param(ServiceFee.SERVICE_FEE_NONE, '0', '20.01', id='Min payment, commission = 0'),
        pytest.param(ServiceFee.SERVICE_FEE_2, '0', '20.01', id='No min payment, commission = 0')
    ]
)

parametrize_events_service_fee = pytest.mark.parametrize(
    'service_fee', [ServiceFee.SERVICE_FEE_1, ServiceFee.SERVICE_FEE_NONE])

parametrize_118_service_fee = pytest.mark.parametrize(
    'service_fee', [ServiceFee.SERVICE_FEE_1, ServiceFee.SERVICE_FEE_NONE, ServiceFee.SERVICE_FEE_2])

parametrize_118_commission_category = pytest.mark.parametrize('commission_category', ['100', '1', '0'])


@pytest.fixture(autouse=True)
def mock_trust(mock_simple_api):
    pass


@parametrize_118_no_markup_tests
def test_ticket_118_no_markup_payment(service_fee, commission_category, amount):
    tickets_no_markup_payments(TICKETS_118_CONTEXT, service_fee, [amount], commission_category)


@parametrize_118_no_markup_tests
def test_ticket_118_no_markup_refunds(service_fee, commission_category, amount):
    tickets_no_markup_refunds(TICKETS_118_CONTEXT, service_fee, [amount], commission_category)


@parametrize_events_context
def test_ticket_to_events_no_markup_payment(context):
    tickets_no_markup_payments(context, ServiceFee.SERVICE_FEE_1, [DEFAULT_PRICE, DEFAULT_FEE], '1')


@parametrize_events_context
def test_ticket_to_events_no_markup_refund(context):
    tickets_no_markup_refunds(context, ServiceFee.SERVICE_FEE_1, [DEFAULT_PRICE, DEFAULT_FEE], '1')


@parametrize_118_service_fee
@parametrize_118_commission_category
@parametrize_markup_params
def test_ticket_118_markup_payments(service_fee, commission_category, amounts,
                                    paymethod_markups, expected_fees):
    tickets_markup_payments(TICKETS_118_CONTEXT, service_fee, commission_category, amounts,
                            paymethod_markups, expected_fees)


@parametrize_118_service_fee
@parametrize_118_commission_category
@parametrize_markup_params
def test_ticket_118_markup_refunds(service_fee, commission_category, amounts,
                                   paymethod_markups, expected_fees):
    tickets_markup_refunds(TICKETS_118_CONTEXT, service_fee, commission_category, amounts,
                           paymethod_markups, expected_fees)


@parametrize_events_context_and_reward_splitting
@parametrize_events_service_fee
@parametrize_two_orders_markup_params
def test_ticket_to_events_markup_payments(context, service_fee, amounts, paymethod_markups,
                                          expected_fees, with_reward_splitting):
    tickets_markup_payments(context, service_fee, COMMISSION_CATEGORY_FOR_SPLITTED_REWARD, amounts,
                            paymethod_markups, expected_fees, with_reward_splitting=with_reward_splitting)


@parametrize_events_context_and_reward_splitting
@parametrize_events_service_fee
@parametrize_two_orders_markup_params
def test_ticket_to_events_markup_refunds(context, service_fee, amounts, paymethod_markups,
                                         expected_fees, with_reward_splitting):
    tickets_markup_refunds(context, service_fee, COMMISSION_CATEGORY_FOR_SPLITTED_REWARD, amounts,
                           paymethod_markups, expected_fees, with_reward_splitting=with_reward_splitting)


@parametrize_context_with_reward_splitting
@parametrize_markup_params_single
@pytest.mark.parametrize('commission_category', [
    pytest.param('0', id='Zero reward', marks=pytest.mark.smoke),
    pytest.param('10', id='Min reward'),
])
def test_ticket_to_events_markup_payments_zero_reward(context, amounts, paymethod_markups, commission_category,
                                                      expected_fees):
    with_reward_splitting = True
    service_fee, commission_category = ServiceFee.SERVICE_FEE_1, commission_category
    tickets_markup_payments(context, service_fee, commission_category, amounts,
                            paymethod_markups, expected_fees, with_reward_splitting=with_reward_splitting)


@parametrize_context_with_reward_splitting
@parametrize_markup_params_single
@pytest.mark.parametrize('commission_category', [
    pytest.param('0', id='Zero reward'),
    pytest.param('10', id='Min reward'),
])
def test_ticket_to_events_markup_refunds_zero_reward(context, amounts, paymethod_markups, commission_category,
                                                     expected_fees):
    with_reward_splitting = True
    service_fee, commission_category = ServiceFee.SERVICE_FEE_1, commission_category
    tickets_markup_refunds(context, service_fee, commission_category, amounts,
                           paymethod_markups, expected_fees, with_reward_splitting=with_reward_splitting)


@parametrize_context_with_reward_splitting
@pytest.mark.parametrize('amounts, paymethod_markups, expected_fees', [
    # два заказа все картой
    ([2, CARD_AMOUNT_2], [{CARD_PM: 2}, {CARD_PM: CARD_AMOUNT_2}],
     {pm.CARD.id: {NONE_SERVICE_FEE_CODE, SPECIFIED_SERVICE_FEE_CODE}}),
])
@pytest.mark.parametrize('commission_category, expected_with_reward_splitting', [
    pytest.param('40', False, id='0.4 > 0.1 and 0.004 * 2 < min reward - not splitted'),
    pytest.param('80', True, id='0.8 > 0.1 and 0.008 * 2 > min reward - splitted'),
])
def test_ticket_to_events_markup_payments_non_splittable_reward(context, amounts, paymethod_markups,
                                                                commission_category, expected_with_reward_splitting,
                                                                expected_fees):
    with_reward_splitting = True
    service_fee, commission_category = ServiceFee.SERVICE_FEE_1, commission_category
    tickets_markup_payments(context, service_fee, commission_category, amounts,
                            paymethod_markups, expected_fees,
                            with_reward_splitting=with_reward_splitting,
                            expected_with_reward_splitting=expected_with_reward_splitting)


@parametrize_context_with_reward_splitting
@pytest.mark.parametrize('amounts, paymethod_markups, expected_fees', [
    # два заказа все картой
    ([2, CARD_AMOUNT_2], [{CARD_PM: 2}, {CARD_PM: CARD_AMOUNT_2}],
     {pm.CARD.id: {NONE_SERVICE_FEE_CODE, SPECIFIED_SERVICE_FEE_CODE}}),
])
@pytest.mark.parametrize('commission_category, expected_with_reward_splitting', [
    pytest.param('40', False, id='0.4 > 0.1 and 0.004 * 2 < min reward - not splitted'),
    pytest.param('80', True, id='0.8 > 0.1 and 0.008 * 2 > min reward - splitted'),
])
def test_ticket_to_events_markup_refunds_non_splittable_reward(context, amounts, paymethod_markups,
                                                               commission_category, expected_with_reward_splitting,
                                                               expected_fees):
    with_reward_splitting = True
    service_fee, commission_category = ServiceFee.SERVICE_FEE_1, commission_category
    tickets_markup_refunds(context, service_fee, commission_category, amounts,
                           paymethod_markups, expected_fees, with_reward_splitting=with_reward_splitting,
                           expected_with_reward_splitting=expected_with_reward_splitting
                           )


# Для сертификатов тестируем только платежи, т.к они невозвратные
@parametrize_afisha_certificate_context_and_reward_splitting
def test_tickets_to_events_no_markup_afisha_certificate_payment(context, with_reward_splitting):
    tickets_no_markup_payments(context, ServiceFee.SERVICE_FEE_NONE, [DEFAULT_PRICE, DEFAULT_FEE],
                               COMMISSION_CATEGORY_FOR_SPLITTED_REWARD,
                               payment_method=AfishaCertificate(), with_reward_splitting=with_reward_splitting)


# @pytest.mark.skip('Refund for certificate is not allowed in trust')
# @parametrize_afisha_certificate_context_and_reward_splitting
# def test_tickets_to_events_no_markup_afisha_certificate_refund(context, with_reward_splitting):
#     tickets_no_markup_refunds(context, ServiceFee.SERVICE_FEE_NONE, [DEFAULT_PRICE, DEFAULT_FEE],
#                               COMMISSION_CATEGORY_FOR_SPLITTED_REWARD,
#                               payment_method=AfishaCertificate(), with_reward_splitting=with_reward_splitting)


# Для фейковых возвратов тестируем только платежи, т.к они невозвратные
@parametrize_afisha_certificate_context_and_reward_splitting
@pytest.mark.parametrize('payment_method', [
    AfishaFakeRefund(),
    FakeRefundCertificate(),
], ids=lambda p: p.title)
def test_tickets_to_events_no_markup_afisha_fake_refund_payment(context, payment_method, with_reward_splitting):
    payments, _, _, _ = tickets_no_markup_payments(
        context, ServiceFee.SERVICE_FEE_NONE, [DEFAULT_PRICE, DEFAULT_FEE],
        COMMISSION_CATEGORY_FOR_SPLITTED_REWARD,
        payment_method=payment_method,
        with_reward_splitting=with_reward_splitting,
        transaction_developer_payload='{"fake_refund": true}'
        )
    # проверяем обновление значения payout_ready_dt у связанных платежей при обновлении у основного платежа
    payment_id, trust_payment_id = payments[0]['payment_id'], payments[0]['trust_payment_id']
    check_payout_ready_dt_updating(trust_payment_id, payment_id)


parametrize_promocode_payment_method = pytest.mark.parametrize('payment_method', [
    CertificatePromocode(), MarketingPromocode()
], ids=lambda p: p.title)


@parametrize_afisha_certificate_context_and_reward_splitting
@parametrize_promocode_payment_method
def test_tickets_to_events_certificate_promocodes_payment(context, payment_method, with_reward_splitting):
    tickets_no_markup_payments(context, ServiceFee.SERVICE_FEE_NONE, [DEFAULT_PRICE, DEFAULT_FEE],
                               COMMISSION_CATEGORY_FOR_SPLITTED_REWARD,
                               payment_method=payment_method, with_reward_splitting=with_reward_splitting)


@parametrize_afisha_certificate_context_and_reward_splitting
@parametrize_promocode_payment_method
def test_tickets_to_events_certificate_promocodes_refund(context, payment_method, with_reward_splitting):
    tickets_no_markup_refunds(context, ServiceFee.SERVICE_FEE_NONE, [DEFAULT_PRICE, DEFAULT_FEE],
                              COMMISSION_CATEGORY_FOR_SPLITTED_REWARD,
                              payment_method=payment_method, with_reward_splitting=with_reward_splitting)


@pytest.mark.skip('Only payments without markup are used in production now')
@parametrize_afisha_certificate_context_and_reward_splitting
@pytest.mark.parametrize('amounts, paymethod_markups, expected_fees', certificate_orders_markup_params)
def test_tickets_to_events_markup_afisha_certificate_payment(context, amounts, paymethod_markups, expected_fees,
                                                             with_reward_splitting):
    tickets_markup_payments(context, ServiceFee.SERVICE_FEE_NONE, COMMISSION_CATEGORY_FOR_SPLITTED_REWARD, amounts,
                            paymethod_markups, expected_fees, with_reward_splitting=with_reward_splitting)


# тест на обновление значения payout_ready_dt у связанных платежей при обновлении payout_ready_dt основного платежа
@reporter.feature(Features.TO_UNIT)
@parametrize_context
@parametrize_markup_params_single
def test_ticket_markup_payout_ready_dt_updating(context, amounts, paymethod_markups,
                                                expected_fees):
    service_fee, commission_category = ServiceFee.SERVICE_FEE_NONE, '1'
    # создаем платеж
    client_id, _, _, orders, paymethod_markups, _ = prepare_client_orders_markup(
        context, service_fee, commission_category, amounts, paymethod_markups)
    payments, group_payment_id, group_trust_payment_id, _ = create_test_payments(
        context, orders, amounts, len(expected_fees), paymethod_markups, client_id=client_id)

    # обрабатываем
    for p in payments:
        steps.CommonPartnerSteps.export_payment(p['payment_id'])

    # проверяем обновление значения payout_ready_dt у связанных платежей при обновлении у основного платежа
    related_payments = [p['payment_id'] for p in payments]
    check_payout_ready_dt_updating(group_trust_payment_id, group_payment_id, related_payments)


@parametrize_context
@parametrize_markup_params_single
def test_tickets_markup_promocode_export_delay(context, amounts, paymethod_markups,
                                               expected_fees):
    service_fee, commission_category = ServiceFee.SERVICE_FEE_NONE, '1'
    client_id, _, _, orders, paymethod_markups, _ = prepare_client_orders_markup(
        context, service_fee, commission_category, amounts, paymethod_markups)
    payments, group_payment_id, group_trust_payment_id, _ = create_test_payments(
        context, orders, amounts, len(expected_fees), paymethod_markups, client_id=client_id)

    # выделяем промокодный и карточный платеж
    payments = {p['payment_method_id']: p for p in payments}
    promocode_payment = payments.get(pm.VIRTUAL.id)
    card_payment = payments.get(pm.CARD.id)
    utils.check_that(promocode_payment, not_none(), u'Проверяем что создан промокодный платеж')
    utils.check_that(card_payment, not_none(), u'Проверяем что создан карточный платеж')

    # проверяем что обработка промокодной части без карточной вызовет ошибку
    with pytest.raises(utils.XmlRpc.XmlRpcError) as error:
        steps.CommonPartnerSteps.export_payment(promocode_payment['payment_id'])

    utils.check_that(
        error.value.response,
        contains_string(PAYMENT_NO_COMPOSITE_PART_EXPORT_ERROR.format(payment_id=promocode_payment['payment_id'])),
        u'Проверяем текст ошибки экспорта')

    # проверяем что в правильном порядке экспортится без ошибок
    steps.CommonPartnerSteps.export_payment(card_payment['payment_id'])
    steps.CommonPartnerSteps.export_payment(promocode_payment['payment_id'])


@parametrize_context
@parametrize_markup_params_single
def test_ticket_markup_promocode_refund_export_wo_payment(context, amounts, paymethod_markups,
                                                          expected_fees):
    service_fee, commission_category = ServiceFee.SERVICE_FEE_NONE, '1'
    client_id, person_id, contract_id, orders, real_paymethod_markups, expected_contract_getter = \
        prepare_client_orders_markup(context, service_fee, commission_category, amounts, paymethod_markups)
    # создаем платеж
    payments, group_payment_id, group_trust_payment_id, purchase_token = create_test_payments(
        context, orders, amounts, len(expected_fees), real_paymethod_markups, client_id=client_id)
    # создаем возврат
    refund_orders = [{'order_id': order['order_id'], 'delta_amount': order['price']} for order in orders]

    refund_paymethod_markup = real_paymethod_markups
    group_trust_refund_id, group_refund_id = steps.SimpleNewApi.create_refund(
        context.service, purchase_token, orders=refund_orders, paymethod_markup=refund_paymethod_markup)
    steps.CommonPartnerSteps.export_payment(group_refund_id)
    refunds = {}
    for p in payments:
        payment_id, payment_method_id = p['payment_id'], p['payment_method_id']
        refunds[payment_method_id] = steps.SimpleApi.find_refund_by_orig_payment_id(payment_id)

    payments = {p['payment_method_id']: p for p in payments}
    # выделяем промокодную часть платежа и возврата
    promocode_payment = payments.get(pm.VIRTUAL.id)
    promocode_refund = refunds.get(pm.VIRTUAL.id)
    utils.check_that(promocode_payment, not_none(), u'Проверяем что создан промокодный платеж')
    utils.check_that(promocode_refund, not_none(), u'Проверяем что создан промокодный возврат')

    # проверяем что экспорт промокодной части платежа без карточной вызовет ошибку.
    promocode_payment_id = promocode_payment['payment_id']
    with pytest.raises(utils.XmlRpc.XmlRpcError) as error_payment:
        steps.CommonPartnerSteps.export_payment(promocode_payment_id)

    # проверяем что экспорт промокодной части возврата без платежа вызовет ошибку.
    promocode_refund_id = promocode_refund[0]
    with pytest.raises(utils.XmlRpc.XmlRpcError) as error_refund:
        steps.CommonPartnerSteps.export_payment(promocode_refund_id)

    utils.check_that(error_payment.value.response,
                     contains_string(PAYMENT_NO_COMPOSITE_PART_EXPORT_ERROR.format(payment_id=promocode_payment_id)),
                     u'Проверяем текст ошибки экспорта платежа')
    utils.check_that(error_refund.value.response,
                     contains_string(REFUND_NO_ORIG_PAYMENT_EXPORT_ERROR.format(payment_id=promocode_refund_id)),
                     u'Проверяем текст ошибки экспорта рефанда')


@parametrize_context
@pytest.mark.parametrize('update_contract', [
    # не подписанный договор
    lambda contract_id: steps.ContractSteps.clear_contract_is_signed(contract_id),
    # завершившийся договор
    lambda contract_id: steps.ContractSteps.insert_attribute(
        contract_id, 'FINISH_DT', value_dt=utils.Date.nullify_time_of_date(datetime.today())),
    # не начавшийся договор
    lambda contract_id: steps.ContractSteps.update_contract_start_dt(
        contract_id, utils.Date.nullify_time_of_date(datetime.today() + timedelta(days=1)))
], ids=['UNSIGNED', 'ENDED', 'NOT_STARTED'])
@parametrize_markup_params_single
def test_tickets_no_active_contract_found(context, update_contract, amounts, paymethod_markups,
                                          expected_fees):
    service_fee, commission_category = ServiceFee.SERVICE_FEE_NONE, '1'
    client_id, person_id, contract_id, orders, real_paymethod_markups, expected_contract_getter = \
        prepare_client_orders_markup(context, service_fee, commission_category, amounts, paymethod_markups)
    # создаем платеж
    payments, group_payment_id, group_trust_payment_id, purchase_token = create_test_payments(
        context, orders, amounts, len(expected_fees), real_paymethod_markups, client_id=client_id)

    # обновляем договор
    update_contract(contract_id)
    # запускаем обработку платежа
    payments = {p['payment_method_id']: p for p in payments}
    card_payment = payments.get(pm.CARD.id)
    utils.check_that(card_payment, not_none(), u'Проверяем что создан карточный платеж')
    payment_id = card_payment['payment_id']
    # провереяем, что без договора платеж не обрабатывается
    with pytest.raises(utils.XmlRpc.XmlRpcError) as xmlrpc_error:
        steps.CommonPartnerSteps.export_payment(payment_id)

    export_error = xmlrpc_error.value.response
    # ожидаемая ошибка
    expected_error = NO_CONTRACT_EXPORT_ERROR.format(payment_id=payment_id, client_id=client_id)

    # ищем ожидаемую ошибку в пришедшей
    utils.check_that(export_error, contains_string(expected_error), u"Ищем ожидаемую ошибку в пришедшем платеже")


@parametrize_events_context_and_reward_splitting
@pytest.mark.parametrize('is_refund', [pytest.mark.smoke(False), True], ids=['Payment', 'Refund'])
@parametrize_markup_params_single
def test_create_compensation_discount(context, is_refund, amounts, paymethod_markups,
                                      expected_fees, with_reward_splitting):
    service_fee, commission_category = ServiceFee.SERVICE_FEE_NONE, COMMISSION_CATEGORY_FOR_SPLITTED_REWARD
    client_id, person_id, contract_id, orders, real_paymethod_markups, expected_contract_getter = \
        prepare_client_orders_markup(context, service_fee, commission_category, amounts, paymethod_markups,
                                     with_reward_splitting=with_reward_splitting)
    # создаем платеж
    payments, group_payment_id, group_trust_payment_id, purchase_token = create_test_payments(
        context, orders, amounts, len(expected_fees), real_paymethod_markups, client_id=client_id)
    # запускаем обработку
    for p in payments:
        steps.CommonPartnerSteps.export_payment(p['payment_id'])

    # создаем копенсацию в трасте
    order = orders[0]
    trust_payment_id, payment_id, purchase_token = steps.SimpleNewApi.create_compensation(
        context.service, orders=[order], amount=order['price'], currency=context.currency.iso_code, is_discount=True)

    # запускаем обработку платежа
    steps.CommonPartnerSteps.export_payment(payment_id)

    payment = steps.CommonPartnerSteps.get_trust_payments(trust_payment_id)[0]
    trust_refund_id = None
    if is_refund:
        # создаем рефанд
        refund_orders = [{'order_id': order['order_id'], 'delta_amount': order['price']}]
        trust_refund_id, refund_id = steps.SimpleNewApi.create_refund(context.service, purchase_token,
                                                                      orders=refund_orders)

        # обрабатываем рефанд
        steps.CommonPartnerSteps.export_payment(refund_id)

    # формируем шаблон для сравнения
    expected_rows = create_expected_rows_for_payment(
        payment, context, [service_fee], commission_category, amounts,
        expected_contract_getter, trust_refund_id=trust_refund_id, with_reward_splitting=with_reward_splitting)

    # получаем данные по платежу или рефанду
    payment_data = get_thirdparty_by_payment_id(payment_id,
                                                with_reward_splitting=with_reward_splitting, is_refund=is_refund)

    # сравниваем платеж с шаблоном
    utils.check_that(payment_data, contains_dicts_with_entries(expected_rows),
                     u'Сравниваем платеж с шаблоном')


@reporter.feature(Features.TO_UNIT)
@parametrize_events_context
@parametrize_markup_params_single
def test_compensation_discount_payout_ready_dt_updating(context, amounts, paymethod_markups,
                                                        expected_fees):
    service_fee, commission_category = ServiceFee.SERVICE_FEE_NONE, '1'
    client_id, person_id, contract_id, orders, real_paymethod_markups, expected_contract_getter = \
        prepare_client_orders_markup(context, service_fee, commission_category, amounts, paymethod_markups)
    # создаем платеж
    payments, group_payment_id, group_trust_payment_id, purchase_token = create_test_payments(
        context, orders, amounts, len(expected_fees), real_paymethod_markups, client_id=client_id)
    # запускаем обработку
    for p in payments:
        steps.CommonPartnerSteps.export_payment(p['payment_id'])

    # создаем копенсацию в трасте
    order = orders[0]
    trust_payment_id, payment_id, purchase_token = steps.SimpleNewApi.create_compensation(
        context.service, orders=[order], amount=order['price'], currency=context.currency.iso_code, is_discount=True)
    # проверяем обновление payout_ready_dt для компенасционного платежа
    check_payout_ready_dt_updating(trust_payment_id, payment_id)


@parametrize_context_and_one_reward_splitting
@parametrize_markup_params_single
def test_tickets_full_reversal(context, amounts, paymethod_markups, expected_fees, with_reward_splitting):
    service_fee, commission_category = ServiceFee.SERVICE_FEE_NONE, COMMISSION_CATEGORY_FOR_SPLITTED_REWARD
    client_id, person_id, contract_id, orders, real_paymethod_markups, expected_contract_getter = \
        prepare_client_orders_markup(context, service_fee, commission_category, amounts,
                                     paymethod_markups, with_reward_splitting=with_reward_splitting)
    _, _, group_trust_payment_id, purchase_token = create_test_payments(
        context, orders, amounts, expected_payment_count=None, real_paymethod_markups=real_paymethod_markups,
        need_clearing=False, wait_for_export_from_bs=False, client_id=client_id)

    # отменим заказ
    steps.SimpleNewApi.unhold_payment(context.service, purchase_token)
    steps.SimpleApi.wait_for_payment(group_trust_payment_id)
    payments = steps.CommonPartnerSteps.get_children_trust_group_payments(group_trust_payment_id)
    expected_payment_count = len(expected_fees)
    utils.check_that(payments, has_length(expected_payment_count),
                     u'Проверяем, что создано количество созданных платежей = {}'.format(expected_payment_count))
    payments = sort_payments_for_export(payments)
    # проверим обработку всех строк платежа
    for p in payments:
        payment_id, payment_method_id = p['payment_id'], p['payment_method_id']

        expected_output = get_reversal_payment_export_error(payment_id, payment_method_id)
        export_result = steps.CommonPartnerSteps.export_payment(payment_id)
        utils.check_that(export_result['output'], equal_to(expected_output), u'Проверяем, что платеж пропущен')
        rows = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id)
        utils.check_that(rows, has_length(0), u'Проверяем что строки платежа действительно не были экспортированы')

        reversals = steps.SimpleApi.find_reversal_by_orig_payment_id(payment_id)
        utils.check_that(reversals, has_length(1), u'Проверяем что создан возврат - reversal')
        reversal_id = reversals[0]['id']

        expected_output = REFUND_REVERSAL_IS_NOT_EXPORTABLE_EXPORT_ERROR.format(payment_id=reversal_id)
        export_result = steps.CommonPartnerSteps.export_payment(reversal_id)
        utils.check_that(export_result['output'], equal_to(expected_output), u'Проверяем, что возврат пропущен')
        rows = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(reversal_id)
        utils.check_that(rows, has_length(0), u'Проверяем что строки возвратов действительно не были экспортированы')


@parametrize_context_and_one_reward_splitting
@parametrize_markup_params_single
def test_tickets_partial_reversal_and_refund(context, amounts, paymethod_markups, expected_fees, with_reward_splitting):
    service_fee, commission_category = ServiceFee.SERVICE_FEE_NONE, COMMISSION_CATEGORY_FOR_SPLITTED_REWARD
    # создаем платеж, не клирим его
    client_id, person_id, contract_id, orders, real_paymethod_markups, expected_contract_getter = \
        prepare_client_orders_markup(context, service_fee, commission_category, amounts,
                                     paymethod_markups, with_reward_splitting=with_reward_splitting)
    payments, group_payment_id, group_trust_payment_id, purchase_token = create_test_payments(
        context, orders, amounts, expected_payment_count=None, real_paymethod_markups=real_paymethod_markups,
        need_clearing=False, wait_for_export_from_bs=False, client_id=client_id)

    # из первого заказа оставляем промокод, из второго карточную часть
    order_1_id, order_2_id = orders[0]['order_id'], orders[1]['order_id']
    reversal_paymethod_markups = {
        order_1_id: {CARD_PM: 0, PROMOCODE_PM: PROMOCODE_AMOUNT_1},
        order_2_id: {CARD_PM: CARD_AMOUNT_2, PROMOCODE_PM: 0}
    }
    # ресайзим платеж
    steps.SimpleNewApi.resize_multiple_orders(context.service, purchase_token, orders,
                                              [PROMOCODE_AMOUNT_1, CARD_AMOUNT_2],
                                              paymethod_markup=reversal_paymethod_markups)

    # клирим что осталось
    steps.SimpleNewApi.clear_payment(context.service, purchase_token)
    steps.SimpleApi.wait_for_payment(group_trust_payment_id)
    payments = steps.CommonPartnerSteps.get_children_trust_group_payments(group_trust_payment_id)  # два платежа
    payments = sort_payments_for_export(payments)
    # подготовим к возврату два оставшихся платежа
    refund_orders = [
        {'order_id': order['order_id'], 'delta_amount': amount}
        for order, amount in zip(orders, [PROMOCODE_AMOUNT_1, CARD_AMOUNT_2])]
    refund_paymethod_markup = {
        order_1_id: {PROMOCODE_PM: PROMOCODE_AMOUNT_1},
        order_2_id: {CARD_PM: CARD_AMOUNT_2},
    }

    expected_amounts = get_expected_amounts_from_markup(refund_paymethod_markup.values())
    # обработаем строки
    reversals = check_reversals(context, commission_category, payments, service_fee, expected_contract_getter,
                                expected_amounts=expected_amounts, with_reward_splitting=with_reward_splitting)

    steps.SimpleNewApi.create_refund(
        context.service, purchase_token, orders=refund_orders, paymethod_markup=refund_paymethod_markup)
    check_refunds(context, commission_category, payments, service_fee, expected_contract_getter, reversals,
                  expected_amounts=expected_amounts, with_reward_splitting=with_reward_splitting)


# проверки для test_tickets_partial_reversal_and_refund
def check_reversals(context, commission_category, payments, service_fee, expected_contract_getter,
                    expected_amounts, with_reward_splitting=False):
    actual_rows = []
    expected_rows = []
    reversals = set()  # соберем все реверсалы для их игнорирования при обработке рефандов
    for p in payments:
        payment_id, payment_method_id = p['payment_id'], p['payment_method_id']
        steps.CommonPartnerSteps.export_payment(payment_id)
        # собираем для проверки фактически обработанные строки
        rows = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id, transaction_type=None)
        actual_rows.extend(rows)

        # для реверсалов проверим их пропуск
        current_reversals = steps.SimpleApi.find_reversal_by_orig_payment_id(payment_id)
        utils.check_that(current_reversals, has_length(1),
                         u'Проверяем что создан возврат - reversal для платежа {}'.format(payment_id))
        reversal_id = current_reversals[0]['id']
        reversals.add(reversal_id)
        expected_output = REFUND_REVERSAL_IS_NOT_EXPORTABLE_EXPORT_ERROR.format(payment_id=reversal_id)
        export_result = steps.CommonPartnerSteps.export_payment(reversal_id)
        utils.check_that(export_result['output'], equal_to(expected_output), u'Проверяем, что возврат пропущен')

        expected_amount = expected_amounts[payment_method_id][0]
        service_fees = [ServiceFee.SERVICE_FEE_NONE] if payment_method_id == pm.VIRTUAL.id else [service_fee]
        # создаем ожидаемые значения
        rows = create_expected_rows_for_payment(
            p, context, service_fees, commission_category, [expected_amount] * len(service_fees),
            expected_contract_getter, with_reward_splitting=with_reward_splitting)
        expected_rows.extend(rows)

    utils.check_that(actual_rows, contains_dicts_with_entries(expected_rows), u'Сравниваем платеж с шаблоном')
    return reversals


def check_refunds(context, commission_category, payments, service_fee,
                  expected_contract_getter, ignored_refund_ids,
                  expected_amounts, with_reward_splitting=False):
    expected_rows = []
    actual_rows = []
    expected_refund_count = 2
    for p in payments:
        payment_id, payment_method_id = p['payment_id'], p['payment_method_id']
        wait_for_refunds = utils.wait_until2(steps.SimpleApi.find_refunds_by_orig_payment_id,
                                             has_length(greater_than_or_equal_to(expected_refund_count)))
        refunds = wait_for_refunds(payment_id)
        utils.check_that(payments, has_length(expected_refund_count),
                         u'Проверяем, что создано количество созданно 2 возврата - reversal и refund')
        for refund in refunds:
            refund_id, trust_refund_id = refund['id'], refund['trust_refund_id']
            steps.CommonPartnerSteps.export_payment(refund_id)
            rows = get_thirdparty_refund_by_payment_id(payment_id, with_reward_splitting)
            actual_rows.extend(rows)
            if refund_id in ignored_refund_ids:
                continue

            expected_amount = expected_amounts[payment_method_id][0]
            service_fees = [ServiceFee.SERVICE_FEE_NONE] if payment_method_id == pm.VIRTUAL.id else [service_fee]
            # создаем ожидаемые значения
            rows = create_expected_rows_for_payment(
                p, context, service_fees, commission_category, [expected_amount] * len(service_fees),
                expected_contract_getter, trust_refund_id=trust_refund_id, with_reward_splitting=with_reward_splitting)
            expected_rows.extend(rows)
    utils.check_that(actual_rows, contains_dicts_with_entries(expected_rows), u'Сравниваем возврат с шаблоном')


@parametrize_markup_params_single
def test_ticket_refund_after_pct2_setting(amounts, paymethod_markups, expected_fees):
    """
    Тестируется, что если платеж, прошел до перевода на схему с разделением вознаграждения на ндс/безндс
    - а возврат - после, то сервисный сбор не будет разделен.
    """
    # на данный момент единственный контекст, в котором возвращается вознаграждение
    context = EVENTS_TICKETS3_RU_CONTEXT
    service_fee, commission_category = ServiceFee.SERVICE_FEE_NONE, COMMISSION_CATEGORY_FOR_SPLITTED_REWARD
    expected_method_amounts = get_expected_amounts_from_markup(paymethod_markups)
    client_id, person_id, contract_id, orders, real_paymethod_markups, expected_contract_getter = \
        prepare_client_orders_markup(context, service_fee, commission_category, amounts,
                                     paymethod_markups, with_reward_splitting=False)
    # # создаем платеж
    payments, group_payment_id, group_trust_payment_id, purchase_token = create_test_payments(
        context, orders, amounts, len(expected_fees), real_paymethod_markups, client_id=client_id)

    # проведем платежи
    for p in payments:
        steps.CommonPartnerSteps.export_payment(p['payment_id'])

    # устанавливаем partner_commission_pct2 для контракта на сегодняшний день
    collateral_params = {
        'CONTRACT2_ID': contract_id,
        'DT': utils.Date.date_to_iso_format(datetime.today()),
        'PARTNER_COMMISSION_PCT2': '0.1',
        'IS_SIGNED': utils.Date.date_to_iso_format(datetime.today()), }
    steps.ContractSteps.create_collateral(Collateral.CHANGE_COMMISSION_PCT, collateral_params)
    # создаем возврат
    refund_orders = [{'order_id': order['order_id'], 'delta_amount': order['price']} for order in orders]

    refund_paymethod_markup = real_paymethod_markups
    group_trust_refund_id, group_refund_id = steps.SimpleNewApi.create_refund(
        context.service, purchase_token, orders=refund_orders, paymethod_markup=refund_paymethod_markup)
    steps.CommonPartnerSteps.export_payment(group_refund_id)

    # установим даты платежей вчерашним днем для последующей проверки работы допника
    for p in payments:
        steps.CommonPartnerSteps.set_payment_dt(p['payment_id'], datetime.today() - timedelta(days=1))

    expected_rows = []
    actual_rows = []
    for p in payments:
        payment_id, payment_method_id = p['payment_id'], p['payment_method_id']
        refund_id, trust_refund_id = steps.SimpleApi.find_refund_by_orig_payment_id(payment_id)
        steps.CommonPartnerSteps.export_payment(refund_id)
        expected_amounts = expected_method_amounts[payment_method_id]
        service_fees = get_service_fees(expected_fees, payment_method_id, service_fee)
        # создаем ожидаемые значения - без строк netting_wo_refund
        rows = create_expected_rows_for_payment(
            p, context, service_fees, commission_category, expected_amounts * len(service_fees),
            expected_contract_getter, trust_refund_id=trust_refund_id,
            with_reward_splitting=False)
        expected_rows.extend(rows)
        # ищем все возвраты - с netting_wo_refund ( их быть не должно )
        rows = get_thirdparty_refund_by_payment_id(payment_id, with_reward_splitting=True)
        actual_rows.extend(rows)
    utils.check_that(actual_rows, contains_dicts_with_entries(expected_rows), u'Сравниваем возврат с шаблоном')


# ------------------------- #
# common payment test logic
def tickets_no_markup_payments(context, service_fee, amounts, commission_category,
                               with_check=True, payment_method=None,
                               with_reward_splitting=False,
                               transaction_developer_payload=None
                               ):
    client_id, person_id, contract_id, orders, _, expected_contract_getter = \
        prepare_client_orders_markup(context, service_fee, commission_category, amounts,
                                     with_reward_splitting=with_reward_splitting)
    # создаем платеж
    payments, payment_id, trust_payment_id, purchase_token = create_test_payments(
        context, orders, amounts, 1, payment_method=payment_method,
        transaction_developer_payload=transaction_developer_payload, client_id=client_id)
    # обрабатываем его
    steps.CommonPartnerSteps.export_payment(payment_id)
    if with_check:
        # проверяем
        service_fees = [ServiceFee.SERVICE_FEE_NONE, service_fee] if len(amounts) > 1 else [service_fee]
        expected_rows = create_expected_rows_for_payment(payments[0], context, service_fees, commission_category,
                                                         amounts, expected_contract_getter,
                                                         with_reward_splitting=with_reward_splitting)
        for_refund = transaction_developer_payload and 'fake_refund' in transaction_developer_payload
        payment_data = get_thirdparty_by_payment_id(payment_id, with_reward_splitting, for_refund)
        utils.check_that(payment_data, contains_dicts_with_entries(expected_rows), u'Сравниваем платеж с шаблоном')
    return payments, orders, purchase_token, expected_contract_getter


def tickets_no_markup_refunds(context, service_fee, amounts, commission_category,
                              with_check=True, with_reward_splitting=False,
                              payment_method=None):
    # не проверяем платежи при тесте возврата
    payments, orders, purchase_token, expected_contract_getter = \
        tickets_no_markup_payments(context, service_fee, amounts, commission_category,
                                   with_check=False, payment_method=payment_method,
                                   with_reward_splitting=with_reward_splitting)
    # создаем рефанд
    refund_orders = [{'order_id': order['order_id'], 'delta_amount': str(utils.dround(Decimal(order['price']) / 2, 2))}
                     for order in orders]
    trust_refund_id, refund_id = steps.SimpleNewApi.create_refund(context.service, purchase_token, orders=refund_orders)
    # обрабатываем рефанд
    steps.CommonPartnerSteps.export_payment(refund_id)
    if with_check:
        # проверяем рефанд
        service_fees = [ServiceFee.SERVICE_FEE_NONE, service_fee] if len(amounts) > 1 else [service_fee]
        expected_rows = create_expected_rows_for_payment(payments[0], context, service_fees,
                                                         commission_category,
                                                         [str(utils.dround(Decimal(a) / 2, 2)) for a in amounts],
                                                         expected_contract_getter,
                                                         trust_refund_id=trust_refund_id,
                                                         with_reward_splitting=with_reward_splitting)
        payment_id = payments[0]['payment_id']
        payment_data = get_thirdparty_refund_by_payment_id(payment_id, with_reward_splitting)
        utils.check_that(payment_data, contains_dicts_with_entries(expected_rows), u'Сравниваем рефанд с шаблоном')


def get_expected_amounts_from_markup(paymethod_markups):
    """
      Метод для выдирания из данных разметки упорядоченных (по заказам) ожидаемых стоимостей строк
    """
    expected_amounts = defaultdict(list)
    for markup in paymethod_markups:
        for payment_method, amount in markup.items():
            pm_id = translate_payment_method_code_to_id(payment_method)
            expected_amounts[pm_id].append(amount)

    return expected_amounts


def translate_payment_method_code_to_id(code):
    if code.startswith('virtual::'):
        return pm.VIRTUAL.id
    try:
        method = next(method for method in pm.values() if method.cc == code)
        return method.id
    except StopIteration:
        raise ValueError('Unknown code payment method code "{}"'.format(code))


def tickets_markup_payments(context, service_fee, commission_category, amounts,
                            paymethod_markups, expected_fees,
                            discounts=None, discount_coefficients=None, with_reward_splitting=False,
                            transaction_developer_payload=None, expected_with_reward_splitting=None):
    expected_method_amounts = get_expected_amounts_from_markup(paymethod_markups)
    client_id, person_id, contract_id, orders, real_paymethod_markups, expected_contract_getter = \
        prepare_client_orders_markup(context, service_fee, commission_category, amounts,
                                     paymethod_markups, with_reward_splitting=with_reward_splitting)
    payments, group_payment_id, group_trust_payment_id, purchase_token = create_test_payments(
        context, orders, amounts, len(expected_fees), real_paymethod_markups, discounts,
        transaction_developer_payload=transaction_developer_payload, client_id=client_id
    )

    expected_with_reward_splitting = with_reward_splitting if expected_with_reward_splitting is None else expected_with_reward_splitting
    # экспортируем связанные платежи
    expected_rows = []
    actual_rows = []
    for p in payments:
        payment_id, payment_method_id = p['payment_id'], p['payment_method_id']
        steps.CommonPartnerSteps.export_payment(payment_id)
        expected_amounts = expected_method_amounts[payment_method_id]
        service_fees = get_service_fees(expected_fees, payment_method_id, service_fee)
        # создаем ожидаемые значения
        rows = create_expected_rows_for_payment(
            p, context, service_fees, commission_category, expected_amounts,
            expected_contract_getter, discount_coefficients,
            with_reward_splitting=expected_with_reward_splitting)
        expected_rows.extend(rows)
        # собираем фактические
        rows = get_thirdparty_payment_by_payment_id(payment_id, with_reward_splitting)
        actual_rows.extend(rows)

    utils.check_that(actual_rows, contains_dicts_with_entries(expected_rows), u'Сравниваем платеж с шаблоном')


def tickets_markup_refunds(context, service_fee, commission_category, amounts,
                           paymethod_markups, expected_fees,
                           discounts=None, discount_coefficients=None, with_reward_splitting=False,
                           expected_with_reward_splitting=None
                           ):
    expected_method_amounts = get_expected_amounts_from_markup(paymethod_markups)
    client_id, person_id, contract_id, orders, real_paymethod_markups, expected_contract_getter = \
        prepare_client_orders_markup(context, service_fee, commission_category, amounts,
                                     paymethod_markups, with_reward_splitting=with_reward_splitting)
    # создаем платеж
    payments, group_payment_id, group_trust_payment_id, purchase_token = create_test_payments(
        context, orders, amounts, len(expected_fees), real_paymethod_markups, discounts, client_id=client_id)
    # обрабатываем их
    for p in payments:
        steps.CommonPartnerSteps.export_payment(p['payment_id'])
    # создаем возврат
    refund_orders = [{'order_id': order['order_id'], 'delta_amount': order['price']} for order in orders]

    refund_paymethod_markup = real_paymethod_markups
    group_trust_refund_id, group_refund_id = steps.SimpleNewApi.create_refund(
        context.service, purchase_token, orders=refund_orders, paymethod_markup=refund_paymethod_markup)
    steps.CommonPartnerSteps.export_payment(group_refund_id)

    expected_with_reward_splitting = with_reward_splitting if expected_with_reward_splitting is None else expected_with_reward_splitting
    expected_rows = []
    actual_rows = []
    for p in payments:
        payment_id, payment_method_id = p['payment_id'], p['payment_method_id']
        refund_id, trust_refund_id = steps.SimpleApi.find_refund_by_orig_payment_id(payment_id)
        steps.CommonPartnerSteps.export_payment(refund_id)
        expected_amounts = expected_method_amounts[payment_method_id]
        service_fees = get_service_fees(expected_fees, payment_method_id, service_fee)
        # создаем ожидаемые значения
        rows = create_expected_rows_for_payment(
            p, context, service_fees, commission_category, expected_amounts * len(service_fees),
            expected_contract_getter, discount_coefficients, trust_refund_id,
            with_reward_splitting=expected_with_reward_splitting)
        expected_rows.extend(rows)
        rows = get_thirdparty_refund_by_payment_id(payment_id, with_reward_splitting)
        actual_rows.extend(rows)
    utils.check_that(actual_rows, contains_dicts_with_entries(expected_rows), u'Сравниваем возврат с шаблоном')


# ------------------------------------------------
# Utils
def get_thirdparty_by_payment_id(payment_id, with_reward_splitting, is_refund):
    # собираем фактические
    if is_refund:
        main_type, reward_type = TransactionType.REFUND, TransactionType.PAYMENT
    else:
        main_type, reward_type = TransactionType.PAYMENT, TransactionType.REFUND

    rows = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(payment_id, main_type)
    # если схема с разделением НДС - добавим возвраты коррекций (с типом - платеж) и уберем коррекции с типом платеж
    if with_reward_splitting:
        # (игнорируем возвраты - коррекции - они созданы (и проверяются) в платеже) для возврата и наоборот для платежа
        rows = [row for row in rows if row['paysys_type_cc'] != PaysysType.NETTING_WO_NDS]
        correction_refund_rows = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(
            payment_id, reward_type)
        correction_refund_rows = [row for row in correction_refund_rows
                                  if row['paysys_type_cc'] == PaysysType.NETTING_WO_NDS]
        rows.extend(correction_refund_rows)
    return rows


def get_thirdparty_payment_by_payment_id(payment_id, with_reward_splitting):
    return get_thirdparty_by_payment_id(payment_id, with_reward_splitting, is_refund=False)


def get_thirdparty_refund_by_payment_id(payment_id, with_reward_splitting):
    return get_thirdparty_by_payment_id(payment_id, with_reward_splitting, is_refund=True)


def prepare_client_orders_markup(context, service_fee, commission_category, amounts, paymethod_markups=None,
                                 with_reward_splitting=False):
    contract_additional_params = {'partner_commission_pct2': REWARD_VAT_PCT} if with_reward_splitting else None
    client_id, person_id, contract_id, service_product_id_0, service_product_id_1, expected_contract_getter = \
        create_contract(context, service_fee, contract_additional_params=contract_additional_params)
    orders = create_orders(context, service_product_id_0, service_product_id_1, commission_category, amounts)
    real_paymethod_markups = fill_markup_with_order_id(orders, paymethod_markups) if paymethod_markups else None
    return client_id, person_id, contract_id, orders, real_paymethod_markups, expected_contract_getter


def get_markup_payment_method(markup):
    """
    Если все платежи - виртуальные, одного типа - выбираем один из виртуальных типов на весь платеж.
    Иначе - карточный
    :param markup:
    :return:
    """
    markup = markup or dict()
    payment_types = []
    for markup_part in markup.values():
        payment_types.extend(markup_part.keys())

    if len(set(payment_types)) == 1 and payment_types[0] != CARD_PM:
        virtual_paymethods = {
            paymethod.type: paymethod
            for paymethod in (VirtualPromocode(), AfishaFakeRefund(), AfishaCertificate())
        }
        return virtual_paymethods[payment_types[0]]
    return TrustWebPage(Via.card(STATIC_EMULATOR_CARD, unbind_before=False))


def create_test_payments(context, orders, amounts, expected_payment_count=None, real_paymethod_markups=None,
                         discounts=None, need_clearing=True, wait_for_export_from_bs=True,
                         payment_method=None, transaction_developer_payload=None, client_id=None
                         ):
    _ = steps.SimpleApi.create_thenumberofthebeast_service_product(context.service, client_id, service_fee=666)
    total_amount = sum([Decimal(amount) for amount in amounts])
    payment_method = payment_method or get_markup_payment_method(real_paymethod_markups)
    group_trust_payment_id, group_payment_id, purchase_token = steps.SimpleNewApi.create_payment(
        context.service, orders=orders, amount=str(total_amount), discounts=discounts,
        currency=context.currency.iso_code, paymethod_markup=real_paymethod_markups,
        paymethod=payment_method, need_clearing=need_clearing,
        wait_for_export_from_bs=wait_for_export_from_bs, developer_payload=transaction_developer_payload)

    # виртуальные платежи не являются композитными
    if not real_paymethod_markups or not isinstance(payment_method, TrustWebPage):
        wait_for_payments = utils.wait_until2(steps.CommonPartnerSteps.get_trust_payments,
                                              has_length(greater_than_or_equal_to(expected_payment_count)))
    else:
        wait_for_payments = utils.wait_until2(steps.CommonPartnerSteps.get_children_trust_group_payments,
                                              has_length(greater_than_or_equal_to(expected_payment_count)))
    payments = wait_for_payments(group_trust_payment_id)

    return sort_payments_for_export(payments), group_payment_id, group_trust_payment_id, purchase_token


@utils.memoize
def create_contract(context, service_fee, contract_additional_params=None):
    with reporter.step(u'Создаем договор для клиента-партнера'):
        # создаем клиента-партнера
        client_id, product, product_fee = steps.SimpleApi.create_partner_product_and_fee(context.service, service_fee)

        contract_additional_params = contract_additional_params or {}
        contract_additional_params.update({'start_dt': START_DT})

        _, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
            context, client_id=client_id, additional_params=contract_additional_params)

        if context.service.id in (EVENTS_TICKETS2_RU_CONTEXT.service.id, EVENTS_TICKETS3_RU_CONTEXT.service.id):
            # для 131 ru или kz - сервис один и тот же
            tech_client_id, tech_person_id, tech_contract_id = steps.CommonPartnerSteps.get_active_tech_ids(
                context.service, currency=context.currency.num_code)
        else:
            tech_client_id, tech_person_id, tech_contract_id = client_id, person_id, contract_id

        # геттер для ожидаемых значений ids контракта
        def get_expected_contract(service_fee_):
            if service_fee_ == ServiceFee.SERVICE_FEE_1:
                return tech_client_id, tech_person_id, tech_contract_id
            return client_id, person_id, contract_id

        return client_id, person_id, contract_id, product, product_fee, get_expected_contract


def get_discount_coefficient_for_fee(discount_coefficients, fee):
    if discount_coefficients and fee in discount_coefficients:
        return discount_coefficients[fee]
    return Decimal('1')


def get_yandex_reward(service, price, commission_category, service_fee, discount_coefficient=Decimal('1')):
    yandex_reward = utils.dround((Decimal(price) * discount_coefficient * Decimal(commission_category)) /
                                 Decimal('10000'), 2)
    if yandex_reward > MIN_PAYMENT:
        return yandex_reward
    if commission_category == '0' and service.id in SERVICE_WITH_ZERO_REWARD:
        return Decimal('0')
    if service_fee == ServiceFee.SERVICE_FEE_2:
        return Decimal('0')
    return MIN_PAYMENT


def get_reversal_payment_export_error(payment_id, payment_method_id):
    if payment_method_id == pm.CARD.id:
        return PAYMENT_COMPLETELY_CANCELLED_EXPORT_ERROR.format(payment_id=payment_id)
    return PAYMENT_MONEY_CANCELLED_EXPORT_ERROR.format(payment_id=payment_id)


def create_order_structure(context, templates_count):
    return [{
        'currency': context.currency.iso_code,
        'fiscal_nds': Fiscal.NDS.nds_none,
        'fiscal_title': Fiscal.fiscal_title
    } for _ in range(templates_count)]


def create_orders(context, service_product_id_0, service_product_id_1, commission_category, amounts):
    order_count = len(amounts)
    if order_count > 1:
        service_products = [service_product_id_0, service_product_id_1]
    else:  # если тестируем один продукт - проверяем с динамическим service_fee
        service_products = [service_product_id_1]

    commission_categories = [commission_category] * order_count
    order_structure = create_order_structure(context, len(service_products))

    orders = steps.SimpleNewApi.create_multiple_orders_for_payment(
        context.service,
        product_id_list=service_products,
        commission_category_list=commission_categories,
        amount_list=amounts,
        orders_structure=order_structure
    )

    utils.check_that(orders, has_length(order_count),
                     u'Проверяем, что количество созданных заказов = {}'.format(order_count))
    return orders


def get_service_fees(expected_fees, payment_method_id, service_fee):
    service_fees = []
    if NONE_SERVICE_FEE_CODE in expected_fees[payment_method_id]:
        service_fees.append(ServiceFee.SERVICE_FEE_NONE)
    if SPECIFIED_SERVICE_FEE_CODE in expected_fees[payment_method_id]:
        service_fees.append(service_fee)
    return service_fees


def fill_markup_with_order_id(orders, template_markups):
    utils.check_that(orders, has_length(len(template_markups)),
                     u'Проверяем что количество заказов соответствует разметке')
    real_paymethod_markup = {}
    for order, markup in zip(orders, template_markups):
        real_paymethod_markup.update({order['order_id']: markup})
    return real_paymethod_markup


def get_and_check_payout_ready_dt(payment_id, expected_payout_ready_dt):
    payment_data = db.get_payment_data(payment_id)[0] or None
    payout_ready_dt = payment_data['payout_ready_dt'] or None
    if expected_payout_ready_dt:
        utils.check_that(payout_ready_dt, expected_payout_ready_dt)
    else:
        utils.check_that(payout_ready_dt, none())


def check_payout_ready_dt_updating(trust_payment_id, payment_id, related_payments_ids=None, payout_ready_dt=None):
    related_payments_ids = related_payments_ids or []
    with reporter.step(u'Проверяем, что у всех платежей поле payout_ready_dt пустое'):
        get_and_check_payout_ready_dt(payment_id, None)
        for related_payments_id in related_payments_ids:
            get_and_check_payout_ready_dt(related_payments_id, None)

    if payout_ready_dt is None:
        payout_ready_dt = (datetime.now() - timedelta(days=1)).strftime('%Y-%m-%d %H:%M:%S')

    with reporter.step(u'Проставляем значение payout_ready_dt '):
        api.medium().UpdatePayment({'TrustPaymentID': trust_payment_id}, {'PayoutReady': payout_ready_dt})
        steps.CommonPartnerSteps.export_payment(payment_id)
    # проставляем payout_ready_dt связанным через RequestOrder платежам
    payments = steps.CommonPartnerSteps.get_children_trust_group_payments(trust_payment_id)
    if payments:
        steps.CommonPartnerSteps.export_payment(payments[0]['payment_id'])
    else:
        # если платеж не групповой - проверим что ему так же проставилась дата
        get_and_check_payout_ready_dt(payment_id, payout_ready_dt)

    with reporter.step(u'Проверяем, что всем связанным платежам проставилось значение payout_ready_dt'):
        for related_payments_id in related_payments_ids:
            get_and_check_payout_ready_dt(related_payments_id, payout_ready_dt)


# ------------------------------------------------
# Expected params creation
def commission_categories_and_payment_params_for_fee(service, commission_category, fee,
                                                     with_reward_splitting, for_refund):
    # если обычный платеж - достаточный размер коммиссии и происходит разделение
    reward_vat_pct_as_commission = REWARD_VAT_PCT * 100
    if not fee and with_reward_splitting and Decimal(commission_category) > reward_vat_pct_as_commission:
        # для логики с разделением - переведем проценты в табличные значения (T_PARTNER_COMMISSION_CATEGORY)
        payment_params = {
            # инвертировано для безндсной части
            'transaction_type': TransactionType.PAYMENT.name if for_refund else TransactionType.REFUND.name,
            'payment_type': PaymentType.CORRECTION_NETTING,
            'paysys_type_cc': PaysysType.NETTING_WO_NDS,
            'internal': 1 if service == EVENTS_TICKETS3_RU_CONTEXT.service else None  # скрываем от оебс безндс для 638
        }

        # для платежей и возвратов в которых возвращаем вознаграждение - разделим его
        if not for_refund or steps.SimpleApi.get_reward_refund_for_service(service):
            return [(str(Decimal(commission_category) - reward_vat_pct_as_commission), payment_params),
                    (str(reward_vat_pct_as_commission), None)]

    return [(commission_category, None)]


def get_expected_invoice_eid(context, contract_id, client_id, row):
    if context.service.id in NO_INVOICE_EID_SIDS:
        return

    if row.get('paysys_type_cc') == PaysysType.NETTING_WO_NDS:
        return steps.InvoiceSteps.get_invoice_eid(contract_id, client_id, context.currency.char_code, 0)
    return steps.InvoiceSteps.get_invoice_eid(contract_id, client_id, context.currency.char_code, 1)


def create_expected_rows_for_payment(payment, context, service_fees, commission_category,
                                     expected_amounts, expected_contract_getter,
                                     discount_coefficients=None, trust_refund_id=None,
                                     with_reward_splitting=False):
    payment_id, trust_payment_id, payment_method_id, payment_method = payment['payment_id'], \
        payment['trust_payment_id'], payment['payment_method_id'], payment['payment_method']

    expected_rows = []
    # фейковый рефанд проходит платежом, который мы инвертируем
    for_refund = (trust_refund_id is not None) ^ ('fake_refund' in payment_method)
    for fee, expected_amount in zip(service_fees, expected_amounts):
        client_id, person_id, contract_id = expected_contract_getter(fee)
        discount_coefficient = get_discount_coefficient_for_fee(discount_coefficients, fee)
        commissions_with_params = commission_categories_and_payment_params_for_fee(
            context.service, commission_category, fee, with_reward_splitting, for_refund)

        for row_commission_category, payment_params in commissions_with_params:
            additional_params = get_expected_params(
                context, fee, row_commission_category, expected_amount,
                payment_method_id=payment_method_id, payment_method=payment_method,
                discount_coefficient=discount_coefficient, for_refund=for_refund)
            additional_params.update(payment_params or {})
            row = steps.SimpleApi.create_expected_tpt_row(
                context, client_id, contract_id, person_id,
                trust_payment_id, payment_id, trust_refund_id=trust_refund_id, **additional_params)

            row['invoice_eid'] = get_expected_invoice_eid(context, contract_id, client_id, row)

            if with_reward_splitting and row.get('paysys_type_cc') == PaysysType.NETTING_WO_NDS:
                # для коррекции меняем местами вознаграждение и сумму.
                row['amount'] = row['yandex_reward']
                row['yandex_reward'] = row['yandex_reward_wo_nds'] = Decimal('0')
            expected_rows.append(row)
    return expected_rows


def getters_expected_params_118(context):
    def get_payment_params(amount, yandex_reward):
        return {'amount': amount, 'yandex_reward': yandex_reward, 'amount_fee': None,
                'transaction_type': TransactionType.PAYMENT.name}

    def get_fee_params(amount, yandex_reward):
        return {'amount': Decimal('0'), 'yandex_reward': yandex_reward, 'amount_fee': amount,
                'transaction_type': TransactionType.PAYMENT.name}

    def get_refund_params(amount, yandex_reward):
        return {'amount': amount, 'yandex_reward': yandex_reward, 'amount_fee': Decimal('0'),
                'transaction_type': TransactionType.REFUND.name}

    def get_refund_certificate_params(amount, yandex_reward):
        params = get_refund_params(amount, yandex_reward)
        params.update({'amount_fee': None})
        return params

    return {
        'get_payment_params': get_payment_params,
        'get_refund_payment_params': get_refund_params,
        'get_fee_params': get_fee_params,
        'get_refund_fee_params': get_refund_params,
        'get_certificate_params': get_payment_params,
        'get_refund_certificate_params': get_refund_certificate_params,
    }


def getters_expected_params_126(context):
    def get_payment_params(amount, yandex_reward):
        return {'amount': amount, 'yandex_reward': yandex_reward, 'amount_fee': None,
                'transaction_type': TransactionType.PAYMENT.name}

    def get_fee_params(amount, yandex_reward):
        return {'amount': Decimal('0'), 'yandex_reward': None, 'amount_fee': amount,
                'transaction_type': TransactionType.PAYMENT.name}

    def get_refund_payment_params(amount, yandex_reward):
        return {'amount': amount, 'yandex_reward': None, 'amount_fee': Decimal('0'),
                'transaction_type': TransactionType.REFUND.name}

    def get_refund_fee_params(amount, yandex_reward):
        return {'amount': amount, 'amount_fee': Decimal('0'), 'transaction_type': TransactionType.REFUND.name}

    return {
        'get_payment_params': get_payment_params,
        'get_refund_payment_params': get_refund_payment_params,
        'get_fee_params': get_fee_params,
        'get_refund_fee_params': get_refund_fee_params
    }


def getters_expected_params_131_638(context):
    def get_payment_params(amount, yandex_reward):
        return {'amount': amount, 'yandex_reward': yandex_reward, 'internal': None,
                'transaction_type': TransactionType.PAYMENT.name}

    def get_fee_params(amount, yandex_reward):
        return {'amount': amount, 'yandex_reward': amount, 'internal': 1,
                'transaction_type': TransactionType.PAYMENT.name}

    def get_refund_payment_params(amount, yandex_reward):
        params = get_payment_params(amount, yandex_reward)
        reward_refund = steps.SimpleApi.get_reward_refund_for_service(context.service)
        if not reward_refund:
            yandex_reward = None
        params.update({'transaction_type': TransactionType.REFUND.name, 'yandex_reward': yandex_reward})
        return params

    def get_refund_fee_params(amount, yandex_reward):
        params = get_fee_params(amount, yandex_reward)
        params.update({'transaction_type': TransactionType.REFUND.name})
        return params

    return {
        'get_payment_params': get_payment_params,
        'get_refund_payment_params': get_refund_payment_params,
        'get_fee_params': get_fee_params,
        'get_refund_fee_params': get_refund_fee_params
    }


def get_expected_params_common(payment_method_id, payment_method):
    if payment_method_id == pm.CARD.id:
        return {
            # переопределяем дефолтную из контекста, пока полностью в тестах не перешли на разметку
            'payment_type': any_of(PaymentType.DIRECT_CARD, PaymentType.CARD)
        }

    if payment_method_id == pm.AFISHA_CERTIFICATE.id:
        return {
            'payment_type': PaymentType.AFISHA_CERTIFICATE,
            'paysys_type_cc': PaysysType.AFISHA_CERTIFICATE,
        }

    if payment_method_id == pm.AFISHA_FAKE_REFUND.id:
        return {
            'payment_type': PaymentType.FAKE_REFUND,
            'paysys_type_cc': PaysysType.FAKE_REFUND,
        }

    if payment_method_id == pm.VIRTUAL.id:
        if payment_method == 'virtual::fake_refund_cert':
            return {
                'payment_type': PaymentType.FAKE_REFUND,
                'paysys_type_cc': PaysysType.FAKE_REFUND_CERTIFICATE
            }

        if payment_method == 'virtual::certificate_promocode':
            return {
                'payment_type': PaymentType.CERTIFICATE_PROMOCODE,
                'paysys_type_cc': PaysysType.CERTIFICATE_PROMO
            }

        if payment_method == 'virtual::marketing_promocode':
            return {
                'payment_type': PaymentType.MARKETING_PROMOCODE,
                'paysys_type_cc': PaysysType.MARKETING_PROMO
            }

        return {
            'payment_type': PaymentType.NEW_PROMOCODE,
            'paysys_type_cc': PaysysType.YANDEX
        }

    if payment_method_id == pm.COMPENSATION_DISCOUNT.id:
        return {
            'payment_type': PaymentType.COMPENSATION_DISCOUNT,
            'paysys_type_cc': PaysysType.YANDEX,
        }

    return {}


def get_expected_params(context, service_fee, commission_category, amount,
                        payment_method_id=pm.CARD.id, payment_method=CARD_PM,
                        for_refund=False, discount_coefficient=Decimal('1')):
    getters_by_context = {
        TICKETS_118_CONTEXT.name: getters_expected_params_118,
        EVENTS_TICKETS_CONTEXT.name: getters_expected_params_126,
        EVENTS_TICKETS3_RU_CONTEXT.name: getters_expected_params_131_638,
        EVENTS_TICKETS2_RU_CONTEXT.name: getters_expected_params_131_638,
        EVENTS_TICKETS2_KZ_CONTEXT.name: getters_expected_params_131_638,
    }

    params_getters = getters_by_context[context.name](context)

    if service_fee == ServiceFee.SERVICE_FEE_1:
        getter = params_getters['get_refund_fee_params'] if for_refund else params_getters['get_fee_params']
    elif service_fee == ServiceFee.SERVICE_FEE_2:
        getter = params_getters['get_refund_certificate_params'] if for_refund else params_getters['get_certificate_params']
    else:
        getter = params_getters['get_refund_payment_params'] if for_refund else params_getters['get_payment_params']

    yandex_reward = get_yandex_reward(context.service, amount, commission_category, service_fee, discount_coefficient)
    amount = Decimal(amount) * discount_coefficient
    additional_params = getter(amount, yandex_reward)
    additional_params.update(get_expected_params_common(payment_method_id, payment_method))
    return additional_params


def sort_payments_for_export(payments):
    # отсортируем таким образом что бы карточный платеж был выше промокодного - иначе будет ошибка экспорта
    return sorted(payments, key=lambda x: x['payment_method_id'])
