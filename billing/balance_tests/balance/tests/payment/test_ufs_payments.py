# coding: utf-8
__author__ = 'a-vasin'

from datetime import datetime
from decimal import Decimal

import pytest
from dateutil.relativedelta import relativedelta
from hamcrest import has_length

import balance.balance_db as db
import btestlib.reporter as reporter
import btestlib.utils as utils
from balance import balance_steps as steps
from balance.features import Features
from btestlib.constants import Services, TransactionType, PaymentType, PaysysType, Export, Products, Collateral
from btestlib.data.partner_contexts import UFS_RU_CONTEXT
from btestlib.data.simpleapi_defaults import DEFAULT_FEE, DEFAULT_PRICE, UFS_REFUND_FEE, UFS_PAYMENT_FEE, \
    DEFAULT_COMMISSION_CATEGORY
from btestlib.matchers import contains_dicts_with_entries

pytestmark = [
    reporter.feature(Features.TRUST, Features.PAYMENT, Features.UFS),
    pytest.mark.tickets('BALANCE-24893'),
    # Больше не модифицируем технического партнёра в БД.
    # Потому можем теперь просто разбирать платежи на настоящий технический договор
    # pytest.mark.no_parallel('ufs', write=False)
]
UFS_INSURANCE_REWARD_PCT = Decimal('65')
INSURANCE_ROW_AMOUNT = Decimal('666.66')

COMMISSION_FRACTION = DEFAULT_COMMISSION_CATEGORY / Decimal('10000')

COLLATERAL_DT = datetime.now() + relativedelta(days=1)
COLLATERAL_SIGN_DT = utils.Date.nullify_time_of_date(datetime.now() - relativedelta(days=2)).isoformat()

SERVICE = UFS_RU_CONTEXT.service

START_DT = utils.Date.nullify_time_of_date(datetime.now())


def create_ids_for_payment_ufs(ufs_payment_fee=UFS_PAYMENT_FEE, ufs_refund_fee=UFS_REFUND_FEE,
                               ufs_insurance_reward_pct=None):
    with reporter.step(u'Создаем договор для клиента-партнера'):
        # создаем клиента-партнера
        client_id, product, product_fee_1 = steps.SimpleApi.create_partner_product_and_fee(SERVICE)
        product_fee_2 = steps.SimpleApi.create_service_product(SERVICE, client_id, service_fee=2)
        _ = steps.SimpleApi.create_thenumberofthebeast_service_product(SERVICE, client_id, service_fee=666)

        _, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
            UFS_RU_CONTEXT,
            client_id=client_id,
            additional_params={
                'START_DT': START_DT,
                'PARTNER_COMMISSION_SUM': ufs_payment_fee,
                'PARTNER_COMMISSION_SUM2': ufs_refund_fee,
                'PARTNER_COMMISSION_PCT2': ufs_insurance_reward_pct,
            })

        return client_id, person_id, contract_id, product, product_fee_1, product_fee_2


def create_tech_ids_for_payment_ufs():
    return steps.CommonPartnerSteps.get_tech_ids(Services.UFS)


@pytest.fixture(autouse=True)
def mock_trust(mock_simple_api):
    pass


# проверка платежа
@pytest.mark.parametrize('fee_amount', [
    pytest.param(DEFAULT_FEE, id='DEFAULT', marks=pytest.mark.smoke),
    pytest.param(UFS_PAYMENT_FEE / 2, id='LESSER_UFS_FEE')
])
def test_ufs_payment(fee_amount, switch_to_pg):
    tech_client_id, tech_person_id, tech_contract_id = create_tech_ids_for_payment_ufs()
    client_id, person_id, contract_id, product, product_fee_1, product_fee_2 = create_ids_for_payment_ufs()

    product_id_list = [product, product_fee_1]
    product_prices = [DEFAULT_PRICE, fee_amount]

    # создаем платеж в трасте и реестр
    service_order_id_list, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_multiple_trust_payments(Services.UFS, product_id_list, prices_list=product_prices)

    # формируем шаблон для сравнения
    expected_payment = steps.SimpleApi.create_expected_tpt_row(UFS_RU_CONTEXT, client_id, contract_id, person_id,
                                                               trust_payment_id, payment_id)

    expected_fee = create_expected_fee_data(tech_contract_id, tech_client_id, payment_id, tech_person_id,
                                            trust_payment_id, fee_amount, yandex_reward=fee_amount, internal=1)

    expected_fee_ufs = create_expected_fee_data(contract_id, client_id, payment_id, person_id, trust_payment_id,
                                                UFS_PAYMENT_FEE, paysys_type_cc=PaysysType.YANDEX)

    export_and_check_payment(payment_id, [expected_payment, expected_fee, expected_fee_ufs])


@pytest.mark.parametrize('payment_type, paysys_type_cc, amount, amount_fee, yandex_reward',
                        [pytest.mark.smoke((PaymentType.COST, PaysysType.SBERBANK, DEFAULT_PRICE, None, None)),
                         (PaymentType.REWARD, PaysysType.SBERBANK, 0, None, DEFAULT_PRICE),
                         (PaymentType.FEE, PaysysType.YANDEX, 0, DEFAULT_PRICE, None),
                         (PaymentType.COST_INSURANCE, PaysysType.INSURANCE, DEFAULT_PRICE, None, None),
                         (PaymentType.REWARD_INSURANCE, PaysysType.INSURANCE, 0, None, DEFAULT_PRICE)],
                         ids=['Cost', 'Reward', 'Fee', 'Cost_insurance', 'Reward_insurance'])
def test_ufs_sidepayment(payment_type, paysys_type_cc, amount, amount_fee, yandex_reward):
    client_id, person_id, contract_id, product, product_fee_1, product_fee_2 = create_ids_for_payment_ufs()

    trust_payment_id = steps.SimpleApi.generate_fake_trust_payment_id()

    # создаем сайдпеймент
    side_payment_id, side_transaction_id = \
        steps.PartnerSteps.create_sidepayment_transaction(client_id, START_DT, DEFAULT_PRICE,
                                                          payment_type, SERVICE.id,
                                                          transaction_type=TransactionType.PAYMENT,
                                                          currency=UFS_RU_CONTEXT.currency,
                                                          paysys_type_cc=paysys_type_cc,
                                                          extra_str_1= trust_payment_id,
                                                          payload="[]")

    side_refund_id, side_transaction_refund_id = \
        steps.PartnerSteps.create_sidepayment_transaction(client_id, START_DT, DEFAULT_PRICE,
                                                          payment_type, SERVICE.id,
                                                          transaction_type=TransactionType.REFUND,
                                                          orig_transaction_id=side_transaction_id,
                                                          currency=UFS_RU_CONTEXT.currency,
                                                          paysys_type_cc=paysys_type_cc,
                                                          extra_str_1= trust_payment_id,
                                                          payload="[]")

    def get_expected_data(payout_ready_dt):
        # формируем шаблон для сравнения
        payment_params = dict(payment_type=payment_type,
                              paysys_type_cc=paysys_type_cc,
                              amount=amount,
                              amount_fee=amount_fee,
                              yandex_reward=yandex_reward,
                              payout_ready_dt=payout_ready_dt)

        refund_params = dict({'transaction_type': TransactionType.REFUND.name,
                              'trust_refund_id': trust_payment_id}, **payment_params)

        if payment_type == PaymentType.REWARD:
            payment_params.update(dict(internal=1))
            refund_params.update(dict(internal=1))
            tech_client_id, tech_person_id, tech_contract_id = create_tech_ids_for_payment_ufs()
            expected_payment = steps.SimpleApi.create_expected_tpt_row(UFS_RU_CONTEXT,
                                                                       tech_client_id, tech_contract_id, tech_person_id,
                                                                       trust_payment_id, side_payment_id, **payment_params)
            expected_refund = steps.SimpleApi.create_expected_tpt_row(UFS_RU_CONTEXT,
                                                                      tech_client_id, tech_contract_id, tech_person_id,
                                                                      None, side_refund_id,
                                                                      **refund_params)
        else:
            expected_payment = steps.SimpleApi.create_expected_tpt_row(UFS_RU_CONTEXT, client_id, contract_id, person_id,
                                                                       trust_payment_id, side_payment_id, **payment_params)
            expected_refund = steps.SimpleApi.create_expected_tpt_row(UFS_RU_CONTEXT, client_id, contract_id, person_id,
                                                                      None, side_refund_id,
                                                                      **refund_params)
        return expected_payment, expected_refund

    expected_payment, expected_refund = get_expected_data(None)

    export_and_check_side_payment(side_payment_id, [expected_payment])
    export_and_check_side_payment(side_refund_id, [expected_refund], TransactionType.REFUND)

    with steps.reporter.step(u'Проставляем payout_ready_dt в платежах:'):
        steps.api.medium().UpdatePayment({'ServiceID': UFS_RU_CONTEXT.service.id, 'TransactionID': side_transaction_id},
                                   {'PayoutReady': START_DT})

    expected_payment, expected_refund = get_expected_data(START_DT)

    export_and_check_side_payment(side_payment_id, [expected_payment], with_export_record=False)
    export_and_check_side_payment(side_refund_id, [expected_refund], TransactionType.REFUND)



@pytest.mark.parametrize('ufs_insurance_reward_pct', [
    pytest.param(UFS_INSURANCE_REWARD_PCT, id='UFS_INSURANCE_REWARD_PCT_65'),
    pytest.param(Decimal('0'), id='UFS_INSURANCE_REWARD_PCT_0'),
    pytest.param(None, id='UFS_INSURANCE_REWARD_PCT_NONE'), ],
)
def test_ufs_insurance_payment(ufs_insurance_reward_pct, switch_to_pg):
    tech_client_id, tech_person_id, tech_contract_id = create_tech_ids_for_payment_ufs()
    client_id, person_id, contract_id, product, product_fee_1, product_fee_2 = \
        create_ids_for_payment_ufs(ufs_insurance_reward_pct=ufs_insurance_reward_pct)

    product_id_list = [product, product_fee_1, product_fee_2]
    product_prices = [DEFAULT_PRICE, DEFAULT_FEE, INSURANCE_ROW_AMOUNT]

    # создаем платеж в трасте и реестр
    service_order_id_list, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_multiple_trust_payments(Services.UFS, product_id_list, prices_list=product_prices)

    # формируем шаблон для сравнения
    expected_payment = steps.SimpleApi.create_expected_tpt_row(UFS_RU_CONTEXT, client_id, contract_id, person_id,
                                                               trust_payment_id, payment_id)

    expected_fee = create_expected_fee_data(tech_contract_id, tech_client_id, payment_id, tech_person_id,
                                            trust_payment_id, DEFAULT_FEE, yandex_reward=DEFAULT_FEE, internal=1)

    expected_fee_ufs = create_expected_fee_data(contract_id, client_id, payment_id, person_id, trust_payment_id,
                                                UFS_PAYMENT_FEE, paysys_type_cc=PaysysType.YANDEX)

    expected_insurance_reward = utils.dround2(
        (ufs_insurance_reward_pct or Decimal('0')) * Decimal('0.01') * INSURANCE_ROW_AMOUNT)

    expected_insurance_ufs = create_expected_insurance_data(contract_id, client_id, payment_id, person_id,
                                                            trust_payment_id, INSURANCE_ROW_AMOUNT, expected_insurance_reward)

    export_and_check_payment(payment_id, [expected_payment, expected_fee, expected_fee_ufs, expected_insurance_ufs])


@pytest.mark.parametrize('ufs_insurance_reward_pct', [
    pytest.param(UFS_INSURANCE_REWARD_PCT, id='UFS_INSURANCE_REWARD_PCT_65'),
    pytest.param(None, id='UFS_INSURANCE_REWARD_PCT_NONE'), ],
)
def test_ufs_insurance_refund(ufs_insurance_reward_pct, switch_to_pg):
    tech_client_id, tech_person_id, tech_contract_id = create_tech_ids_for_payment_ufs()
    client_id, person_id, contract_id, product, product_fee_1, product_fee_2 = \
        create_ids_for_payment_ufs(ufs_insurance_reward_pct=ufs_insurance_reward_pct)

    product_id_list = [product, product_fee_1, product_fee_2]
    product_prices = [DEFAULT_PRICE, DEFAULT_FEE, INSURANCE_ROW_AMOUNT]

    # создаем платеж в трасте и реестр
    service_order_id_list, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_multiple_trust_payments(Services.UFS, product_id_list, prices_list=product_prices)

    trust_refund_id, refund_id = steps.SimpleApi.create_multiple_refunds(Services.UFS, service_order_id_list,
                                                                         trust_payment_id, product_prices)

    # формируем шаблон для сравнения
    expected_refund = create_expected_refund(contract_id, client_id, payment_id, person_id, trust_refund_id,
                                              trust_payment_id, DEFAULT_PRICE)

    expected_fee = create_expected_refund(tech_contract_id, tech_client_id, payment_id, tech_person_id,
                                          trust_refund_id, trust_payment_id, DEFAULT_FEE, yandex_reward=DEFAULT_FEE,
                                          internal=1)

    expected_fee_ufs = create_expected_refund(contract_id, client_id, payment_id, person_id, trust_refund_id,
                                              trust_payment_id, amount=Decimal('0'), amount_fee=-UFS_REFUND_FEE,
                                              paysys_type_cc=PaysysType.YANDEX)

    expected_insurance_reward = utils.dround2(
        (ufs_insurance_reward_pct or Decimal('0')) * Decimal('0.01') * INSURANCE_ROW_AMOUNT)
    expected_insurance_ufs = create_expected_refund(contract_id, client_id, payment_id, person_id, trust_refund_id,
                                              trust_payment_id, amount=INSURANCE_ROW_AMOUNT, amount_fee=None,
                                              yandex_reward=expected_insurance_reward, paysys_type_cc=PaysysType.INSURANCE,
                                              product_id=Products.UFS_INSURANCE_PAYMENTS.id)

    export_and_check_payment(refund_id, [expected_refund, expected_fee, expected_fee_ufs, expected_insurance_ufs],
                             payment_id, TransactionType.REFUND)


def test_ufs_insurance_collateral_reward_pct_change(switch_to_pg):
    tech_client_id, tech_person_id, tech_contract_id = create_tech_ids_for_payment_ufs()
    client_id, person_id, contract_id, product, product_fee_1, product_fee_2 = \
        create_ids_for_payment_ufs(ufs_insurance_reward_pct=UFS_INSURANCE_REWARD_PCT)

    new_insurance_reward_pct = Decimal('25')
    create_insurance_collateral(contract_id, new_insurance_reward_pct)

    product_id_list = [product, product_fee_1, product_fee_2]
    product_prices = [DEFAULT_PRICE, DEFAULT_FEE, INSURANCE_ROW_AMOUNT]

    # создаем платеж в трасте и реестр
    service_order_id_list, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_multiple_trust_payments(Services.UFS, product_id_list, prices_list=product_prices)

    # формируем шаблон для сравнения
    expected_payment = steps.SimpleApi.create_expected_tpt_row(UFS_RU_CONTEXT, client_id, contract_id, person_id,
                                                               trust_payment_id, payment_id)

    expected_fee = create_expected_fee_data(tech_contract_id, tech_client_id, payment_id, tech_person_id,
                                            trust_payment_id, DEFAULT_FEE, yandex_reward=DEFAULT_FEE, internal=1)

    expected_fee_ufs = create_expected_fee_data(contract_id, client_id, payment_id, person_id, trust_payment_id,
                                                UFS_PAYMENT_FEE, paysys_type_cc=PaysysType.YANDEX)

    expected_insurance_reward = utils.dround2(
        (new_insurance_reward_pct or Decimal('0')) * Decimal('0.01') * INSURANCE_ROW_AMOUNT)

    expected_insurance_ufs = create_expected_insurance_data(contract_id, client_id, payment_id, person_id,
                                                            trust_payment_id, INSURANCE_ROW_AMOUNT, expected_insurance_reward)

    export_and_check_payment(payment_id, [expected_payment, expected_fee, expected_fee_ufs, expected_insurance_ufs])


# проверка полного рефанда
def test_ufs_refund_3_rows(switch_to_pg):
    tech_client_id, tech_person_id, tech_contract_id = create_tech_ids_for_payment_ufs()
    client_id, person_id, contract_id, product, product_fee_1, product_fee_2 = create_ids_for_payment_ufs()

    product_id_list = [product, product_fee_1]
    product_prices = [DEFAULT_PRICE, DEFAULT_FEE]

    # создаем платеж в трасте и реестр
    service_order_id_list, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_multiple_trust_payments(Services.UFS, product_id_list, prices_list=product_prices)

    trust_refund_id, refund_id = steps.SimpleApi.create_multiple_refunds(Services.UFS, service_order_id_list,
                                                                         trust_payment_id, product_prices)

    # формируем шаблон для сравнения
    expected_payment = create_expected_refund(contract_id, client_id, payment_id, person_id, trust_refund_id,
                                              trust_payment_id, DEFAULT_PRICE)

    expected_fee = create_expected_refund(tech_contract_id, tech_client_id, payment_id, tech_person_id,
                                          trust_refund_id, trust_payment_id, DEFAULT_FEE, yandex_reward=DEFAULT_FEE,
                                          internal=1)

    expected_fee_ufs = create_expected_refund(contract_id, client_id, payment_id, person_id, trust_refund_id,
                                              trust_payment_id, amount=Decimal('0'), amount_fee=-UFS_REFUND_FEE,
                                              paysys_type_cc=PaysysType.YANDEX)

    export_and_check_payment(refund_id, [expected_payment, expected_fee, expected_fee_ufs],
                             payment_id, TransactionType.REFUND)


# проверка рефанда без сбора яндекса
def test_ufs_refund_2_rows(switch_to_pg):
    client_id, person_id, contract_id, product, product_fee_1, product_fee_2 = create_ids_for_payment_ufs()

    product_id_list = [product, product_fee_1]
    product_prices = [DEFAULT_PRICE, DEFAULT_FEE]

    # создаем платеж в трасте и реестр
    service_order_id_list, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_multiple_trust_payments(Services.UFS, product_id_list, prices_list=product_prices)

    service_order_id_list_wo_fee = [service_order_id_list[0]]
    refund_prices = [DEFAULT_PRICE]
    trust_refund_id, refund_id = steps.SimpleApi.create_multiple_refunds(Services.UFS, service_order_id_list_wo_fee,
                                                                         trust_payment_id, refund_prices)

    # формируем шаблон для сравнения
    expected_payment = create_expected_refund(contract_id, client_id, payment_id, person_id, trust_refund_id,
                                              trust_payment_id, DEFAULT_PRICE)

    expected_fee_ufs = create_expected_refund(contract_id, client_id, payment_id, person_id, trust_refund_id,
                                              trust_payment_id, amount=Decimal('0'), amount_fee=-UFS_REFUND_FEE,
                                              paysys_type_cc=PaysysType.YANDEX)

    export_and_check_payment(refund_id, [expected_payment, expected_fee_ufs],
                             payment_id, TransactionType.REFUND)


# допник на изменение стоимости платежа
def test_ufs_payment_collateral(switch_to_pg):
    new_payment_price = UFS_PAYMENT_FEE / 3

    tech_client_id, tech_person_id, tech_contract_id = create_tech_ids_for_payment_ufs()
    client_id, person_id, contract_id, product, product_fee_1, product_fee_2 = create_ids_for_payment_ufs()
    create_payment_collateral(contract_id, new_payment_price)

    product_id_list = [product, product_fee_1]
    product_prices = [DEFAULT_PRICE, DEFAULT_FEE]

    # создаем платеж в трасте и реестр
    service_order_id_list, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_multiple_trust_payments(Services.UFS, product_id_list, prices_list=product_prices)

    # формируем шаблон для сравнения
    expected_payment = steps.SimpleApi.create_expected_tpt_row(UFS_RU_CONTEXT, client_id, contract_id, person_id,
                                                               trust_payment_id, payment_id)

    expected_fee = create_expected_fee_data(tech_contract_id, tech_client_id, payment_id, tech_person_id,
                                            trust_payment_id, DEFAULT_FEE, yandex_reward=DEFAULT_FEE, internal=1)

    expected_fee_ufs = create_expected_fee_data(contract_id, client_id, payment_id, person_id, trust_payment_id,
                                                new_payment_price, paysys_type_cc=PaysysType.YANDEX)

    export_and_check_payment(payment_id, [expected_payment, expected_fee, expected_fee_ufs])


# допник на изменение стоимости рефанда
def test_ufs_refund_collateral(switch_to_pg):
    new_refund_price = UFS_REFUND_FEE * 2

    tech_client_id, tech_person_id, tech_contract_id = create_tech_ids_for_payment_ufs()
    client_id, person_id, contract_id, product, product_fee_1, product_fee_2 = create_ids_for_payment_ufs()
    create_refund_collateral(contract_id, new_refund_price)

    product_id_list = [product, product_fee_1]
    product_prices = [DEFAULT_PRICE, DEFAULT_FEE]

    # создаем платеж в трасте и реестр
    service_order_id_list, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_multiple_trust_payments(Services.UFS, product_id_list, prices_list=product_prices)

    trust_refund_id, refund_id = steps.SimpleApi.create_multiple_refunds(Services.UFS, service_order_id_list,
                                                                         trust_payment_id, product_prices)

    # формируем шаблон для сравнения
    expected_payment = create_expected_refund(contract_id, client_id, payment_id, person_id, trust_refund_id,
                                              trust_payment_id, DEFAULT_PRICE)

    expected_fee = create_expected_refund(tech_contract_id, tech_client_id, payment_id, tech_person_id,
                                          trust_refund_id, trust_payment_id, DEFAULT_FEE, yandex_reward=DEFAULT_FEE,
                                          internal=1)

    expected_fee_ufs = create_expected_refund(contract_id, client_id, payment_id, person_id, trust_refund_id,
                                              trust_payment_id, amount=Decimal('0'), amount_fee=-new_refund_price,
                                              paysys_type_cc=PaysysType.YANDEX)

    export_and_check_payment(refund_id, [expected_payment, expected_fee, expected_fee_ufs],
                             payment_id, TransactionType.REFUND)


# ------------------------------------------------
# Utils
def change_start_dt(contract_id, collateral_type_id):
    with reporter.step(u'Меняем дату для допника с типом: {} для договора: {}'.format(collateral_type_id, contract_id)):
        query = 'UPDATE T_CONTRACT_COLLATERAL SET DT=:dt ' \
                'WHERE CONTRACT2_ID=:contract_id AND COLLATERAL_TYPE_ID=:collateral_type_id'
        params = {'contract_id': contract_id,
                  'collateral_type_id': collateral_type_id,
                  'dt': utils.Date.nullify_time_of_date(datetime.now() - relativedelta(days=1))}
        db.balance().execute(query, params)
        steps.ContractSteps.refresh_contracts_cache(contract_id)


def create_payment_collateral(contract_id, price):
    steps.ContractSteps.create_collateral(Collateral.COMMISSION_CHANGE, {'UFS_PAYMENT': price,
                                                 'CONTRACT2_ID': contract_id,
                                                 'DT': COLLATERAL_DT,
                                                 'IS_SIGNED': COLLATERAL_SIGN_DT})

    change_start_dt(contract_id, Collateral.COMMISSION_CHANGE)


def create_refund_collateral(contract_id, price):
    steps.ContractSteps.create_collateral(Collateral.COMMISSION_REFUND_CHANGE, {'UFS_REFUND': price,
                                                 'CONTRACT2_ID': contract_id,
                                                 'DT': COLLATERAL_DT,
                                                 'IS_SIGNED': COLLATERAL_SIGN_DT})

    change_start_dt(contract_id, Collateral.COMMISSION_REFUND_CHANGE)


def create_insurance_collateral(contract_id, insurance_reward_pct):
    steps.ContractSteps.create_collateral(Collateral.INSURANCE_REWARD_PCT_CHANGE,
                                          {'UFS_INSURANCE': insurance_reward_pct,
                                           'CONTRACT2_ID': contract_id,
                                           'DT': COLLATERAL_DT,
                                           'IS_SIGNED': COLLATERAL_SIGN_DT})

    change_start_dt(contract_id, Collateral.INSURANCE_REWARD_PCT_CHANGE)


def export_and_check_payment(payment_id, expected_data, thirdparty_payment_id=None,
                             transaction_type=TransactionType.PAYMENT):
    if not thirdparty_payment_id:
        thirdparty_payment_id = payment_id

    # запускаем обработку платежа
    steps.CommonPartnerSteps.export_payment(payment_id)

    # получаем данные по платежу
    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(thirdparty_payment_id, transaction_type)

    dt = steps.CommonPartnerSteps.get_delivered_date(thirdparty_payment_id, 'T_THIRDPARTY_TRANSACTIONS', 'payment_id')
    for data in expected_data:
        data['payout_ready_dt'] = dt

    # сравниваем платеж с шаблоном
    utils.check_that(payment_data, contains_dicts_with_entries(expected_data), u'Сравниваем платеж с шаблоном')

    utils.check_that(payment_data, has_length(len(expected_data)), u"Проверяем, что отсутствуют дополнительные записи")


def export_and_check_side_payment(side_payment_id, expected_data, transaction_type=TransactionType.PAYMENT,
                                  with_export_record=True):
    # запускаем обработку платежа
    if transaction_type==TransactionType.PAYMENT:
        steps.ExportSteps.create_export_record_and_export(side_payment_id, Export.Type.THIRDPARTY_TRANS,
                                                          Export.Classname.SIDE_PAYMENT, service_id=SERVICE.id,
                                                          with_export_record=with_export_record)
    else:
        steps.ExportSteps.create_export_record_and_export(side_payment_id, Export.Type.THIRDPARTY_TRANS,
                                                          Export.Classname.SIDE_PAYMENT, service_id=SERVICE.id,
                                                          with_export_record=False)

    # получаем данные по платежу
    payment_data = steps.CommonPartnerSteps.get_thirdparty_payment_by_sidepayment_id(side_payment_id)

    # сравниваем платеж с шаблоном
    utils.check_that(payment_data, contains_dicts_with_entries(expected_data), u'Сравниваем платеж с шаблоном')

    utils.check_that(payment_data, has_length(len(expected_data)), u"Проверяем, что отсутствуют дополнительные записи")


def create_common_expected_data(contract_id, partner_id, payment_id, person_id, trust_id, trust_payment_id,
                                payment_type, paysys_type_cc, trust_refund_id=None):
    return steps.SimpleApi.create_expected_tpt_row(UFS_RU_CONTEXT, partner_id, contract_id, person_id, trust_payment_id,
                                payment_id, trust_refund_id, payment_type=payment_type, paysys_type_cc=paysys_type_cc)


def create_expected_fee_data(contract_id, partner_id, payment_id, person_id, trust_payment_id, amount,
                             yandex_reward=None, paysys_type_cc=UFS_RU_CONTEXT.tpt_paysys_type_cc, internal=None):
    expected_data = create_common_expected_data(contract_id, partner_id, payment_id, person_id, trust_payment_id,
                                                trust_payment_id, UFS_RU_CONTEXT.tpt_payment_type, paysys_type_cc)

    expected_data.update({
        'amount': Decimal('0'),
        'amount_fee': amount,
        'yandex_reward': yandex_reward,
        'internal': internal,
    })

    return expected_data


def create_expected_insurance_data(contract_id, partner_id, payment_id, person_id, trust_payment_id, amount,
                             yandex_reward=None, paysys_type_cc=PaysysType.INSURANCE, internal=None):

    expected_data = create_common_expected_data(contract_id, partner_id, payment_id, person_id, trust_payment_id,
                                                trust_payment_id, UFS_RU_CONTEXT.tpt_payment_type, paysys_type_cc)

    expected_data.update({
        'amount': amount,
        'yandex_reward': yandex_reward,
        'internal': internal,
        'product_id': Products.UFS_INSURANCE_PAYMENTS.id,
    })

    return expected_data


def create_expected_refund(contract_id, partner_id, payment_id, person_id, trust_id, trust_payment_id, amount,
                           amount_fee=Decimal('0'), yandex_reward=None, paysys_type_cc=UFS_RU_CONTEXT.tpt_paysys_type_cc,
                           internal=None, product_id=None):
    expected_data = create_common_expected_data(contract_id, partner_id, payment_id, person_id, trust_id,
                                                trust_payment_id, UFS_RU_CONTEXT.tpt_payment_type, paysys_type_cc,
                                                trust_refund_id=trust_id)

    expected_data.update({
        'amount': amount,
        'amount_fee': amount_fee,
        'yandex_reward': yandex_reward,
        'internal': internal,
        'product_id': product_id,
    })

    return expected_data
