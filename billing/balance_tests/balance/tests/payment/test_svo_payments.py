# coding: utf-8
__author__ = 'a-vasin'

from datetime import datetime

import pytest
from dateutil.relativedelta import relativedelta
from hamcrest import greater_than_or_equal_to, equal_to, empty, anything, contains_string

import balance.balance_api as api
import balance.balance_db as db
import balance.balance_steps as steps
from balance.balance_steps.common_steps import CommonSteps
from btestlib import reporter, utils
from btestlib.constants import TransactionType, Services, Export, PaysysType, Currencies, \
    PartnerPaymentType, PaymentType
from btestlib.data.partner_contexts import TAXI_RU_CONTEXT
from btestlib.data.simpleapi_defaults import DEFAULT_PRICE
from btestlib.matchers import has_entries_casted, contains_dicts_with_entries

# https://st.yandex-team.ru/BALANCE-28492

TAXI_SVO_CONTEXT = TAXI_RU_CONTEXT.new(
        service=Services.TAXI_SVO,
        tpt_payment_type=PaymentType.SVO,
        tpt_paysys_type_cc=PaysysType.TAXI,
)

START_DT = utils.Date.first_day_of_month()
SERVICE = TAXI_SVO_CONTEXT.service


@pytest.mark.no_parallel('svo', write=False)
@pytest.mark.parametrize("transaction_type", [
    pytest.mark.smoke(
            TransactionType.PAYMENT),
    TransactionType.REFUND
], ids=lambda tt: tt.name)
def test_payment(transaction_type):
    client_id, person_id, contract_id = create_contract()

    payment_id, _ = steps.PartnerSteps.create_sidepayment_transaction(client_id, START_DT, DEFAULT_PRICE,
                                                                   PartnerPaymentType.SVO, SERVICE.id,
                                                                   transaction_type=transaction_type)
    steps.ExportSteps.create_export_record_and_export(payment_id, Export.Type.THIRDPARTY_TRANS,
                                                      Export.Classname.SIDE_PAYMENT)

    expected_data = [steps.SimpleApi.create_expected_tpt_row(TAXI_SVO_CONTEXT, client_id, contract_id,
                                                             person_id, None,
                                                             payment_id, internal=1,
                                                             transaction_type=transaction_type.name)]
    thirdparty_transaction_id = steps.CommonPartnerSteps.get_synthetic_thirdparty_transaction_id_by_payment_id(
            payment_id)
    payment_data = steps.CommonPartnerSteps.get_thirdparty_payment_by_id(thirdparty_transaction_id)
    payment_data[0]['trust_id'] = None
    utils.check_that(payment_data, contains_dicts_with_entries(expected_data), u'Сравниваем платеж с шаблоном')


@pytest.mark.no_parallel('svo')
def test_no_transactions():
    # создаем один реестр, на случай неучтенных платежей
    process_registers()

    registers_before = count_registers()
    # пытаемся создать реестр без транзакций
    process_registers()
    registers_after = count_registers()
    utils.check_that(registers_after - registers_before, equal_to(0), u"Проверяем, что реестр не создан")


@pytest.mark.no_parallel('svo')
def test_register():
    client_id, person_id, contract_id = create_contract()
    process_registers()

    payment_id, _ = steps.PartnerSteps.create_sidepayment_transaction(client_id, START_DT, 3 * DEFAULT_PRICE,
                                                                   PartnerPaymentType.SVO, SERVICE.id,
                                                                   transaction_type=TransactionType.PAYMENT)
    steps.ExportSteps.create_export_record_and_export(payment_id, Export.Type.THIRDPARTY_TRANS,
                                                      Export.Classname.SIDE_PAYMENT)

    payment_id, _ = steps.PartnerSteps.create_sidepayment_transaction(client_id, START_DT, DEFAULT_PRICE,
                                                                   PartnerPaymentType.SVO, SERVICE.id,
                                                                   transaction_type=TransactionType.REFUND)
    steps.ExportSteps.create_export_record_and_export(payment_id, Export.Type.THIRDPARTY_TRANS,
                                                      Export.Classname.SIDE_PAYMENT)

    # a-vasin: вот это должно быть неучтено
    payment_id, _ = steps.PartnerSteps.create_sidepayment_transaction(client_id, START_DT, DEFAULT_PRICE,
                                                                   PartnerPaymentType.SVO, SERVICE.id,
                                                                   transaction_type=TransactionType.REFUND)
    steps.ExportSteps.create_export_record(payment_id, classname=Export.Classname.SIDE_PAYMENT)

    letters_before = count_register_letters()
    registers_before = count_registers()
    process_registers()

    registers_after = count_registers()
    utils.check_that(registers_after - registers_before, equal_to(1), u"Проверяем, что реестр создан")

    expected_register = create_expected_register(2 * DEFAULT_PRICE)
    register = get_latest_register()
    utils.check_that(register, has_entries_casted(expected_register), u'Сравниваем реестр с шаблоном')

    letters_after = count_register_letters()
    utils.check_that(letters_after - letters_before, equal_to(1), u"Проверяем, что письмо создалось")

    transactions_count = get_register_transactions_count(register['id'])
    utils.check_that(transactions_count, equal_to(2), u'Проверяем, что обе транзакции учтены')


@pytest.mark.no_parallel('svo', write=False)
def test_empty_acts():
    client_id, person_id, contract_id = create_contract()

    payment_id, _ = steps.PartnerSteps.create_sidepayment_transaction(client_id, START_DT, 3 * DEFAULT_PRICE,
                                                                   PartnerPaymentType.SVO, SERVICE.id,
                                                                   transaction_type=TransactionType.PAYMENT)
    steps.ExportSteps.create_export_record_and_export(payment_id, Export.Type.THIRDPARTY_TRANS,
                                                      Export.Classname.SIDE_PAYMENT)

    steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, utils.Date.last_day_of_month())
    acts = steps.ActsSteps.get_act_data_by_client(client_id)
    utils.check_that(acts, empty(), u"Проверяем отсутствие актов")


@pytest.mark.no_parallel('svo')
def test_get_payment_batch_details():
    client_id, person_id, contract_id = create_contract()
    process_registers()

    refund_id, _ = steps.PartnerSteps.create_sidepayment_transaction(client_id, START_DT, DEFAULT_PRICE,
                                                                  PartnerPaymentType.SVO, SERVICE.id,
                                                                  transaction_type=TransactionType.REFUND)
    steps.ExportSteps.create_export_record_and_export(refund_id, Export.Type.THIRDPARTY_TRANS,
                                                      Export.Classname.SIDE_PAYMENT)

    payment_id, _ = steps.PartnerSteps.create_sidepayment_transaction(client_id, START_DT, 2 * DEFAULT_PRICE,
                                                                   PartnerPaymentType.SVO, SERVICE.id,
                                                                   transaction_type=TransactionType.PAYMENT)
    steps.ExportSteps.create_export_record_and_export(payment_id, Export.Type.THIRDPARTY_TRANS,
                                                      Export.Classname.SIDE_PAYMENT)

    process_registers()
    register_id = get_latest_register()['id']

    register_details = steps.MediumHttpSteps.get_payment_batch_details(register_id)
    expected_register_details = [
        create_expected_register_details(contract_id, DEFAULT_PRICE, TransactionType.REFUND),
        create_expected_register_details(contract_id, 2 * DEFAULT_PRICE, TransactionType.PAYMENT),
    ]
    utils.check_that(register_details, contains_dicts_with_entries(expected_register_details),
                     u'Сравниваем данные регистра с шаблоном')


@pytest.mark.no_parallel('svo')
def test_get_payment_headers():
    today = utils.Date.nullify_time_of_date(datetime.now())
    tomorrow = today + relativedelta(days=1)

    client_id, person_id, contract_id = create_contract()
    process_registers()

    refund_id, _ = steps.PartnerSteps.create_sidepayment_transaction(client_id, START_DT, DEFAULT_PRICE,
                                                                  PartnerPaymentType.SVO, SERVICE.id,
                                                                  transaction_type=TransactionType.REFUND)
    steps.ExportSteps.create_export_record_and_export(refund_id, Export.Type.THIRDPARTY_TRANS,
                                                      Export.Classname.SIDE_PAYMENT)

    payment_id, _ = steps.PartnerSteps.create_sidepayment_transaction(client_id, START_DT, 3 * DEFAULT_PRICE,
                                                                   PartnerPaymentType.SVO, SERVICE.id,
                                                                   transaction_type=TransactionType.PAYMENT)
    steps.ExportSteps.create_export_record_and_export(payment_id, Export.Type.THIRDPARTY_TRANS,
                                                      Export.Classname.SIDE_PAYMENT)

    process_registers()
    register_id = get_latest_register()['id']

    register_headers = steps.MediumHttpSteps.get_payment_headers(SERVICE, today, tomorrow)
    expected_register_headers = [create_expected_register_header(register_id, 2 * DEFAULT_PRICE)]
    utils.check_that(register_headers, contains_dicts_with_entries(expected_register_headers, same_length=False),
                     u'Сравниваем данные регистра с шаблоном')


def test_client_wo_contract():
    client_id = steps.ClientSteps.create()

    payment_id, _ = steps.PartnerSteps.create_sidepayment_transaction(client_id, START_DT, 3 * DEFAULT_PRICE,
                                                                   PartnerPaymentType.SVO, SERVICE.id,
                                                                   transaction_type=TransactionType.PAYMENT)
    steps.ExportSteps.create_export_record(payment_id, classname=Export.Classname.SIDE_PAYMENT)

    with pytest.raises(utils.XmlRpc.XmlRpcError) as error:
        CommonSteps.export(Export.Type.THIRDPARTY_TRANS, Export.Classname.SIDE_PAYMENT, payment_id)

    expected_error = 'SidePayment({}) delayed: no active contracts found for client {}'.format(payment_id, client_id)
    utils.check_that(error.value.response, contains_string(expected_error), u'Проверяем текст ошибки экспорта')


# -------------------------------------------
# Utils

def get_register_transactions_count(register_id):
    with reporter.step(u"Получаем количество транзакций в реестре: {}".format(register_id)):
        query = "SELECT count(*) cnt FROM T_THIRDPARTY_TRANSACTIONS WHERE BATCH_ID=:register_id"
        params = {'register_id': register_id}
        return db.balance().execute(query, params)[0]['cnt']


def create_expected_register_header(register_id, amount):
    today = utils.Date.nullify_time_of_date(datetime.now())

    return {
        'status': u'DONE',
        'eventtime': today.isoformat(sep=' '),
        'description': u'',
        'oebs_status': u'',
        'trantime': anything(),
        'sum': amount,
        'bank_order_id': register_id,
        'payment_batch_id': register_id
    }


def create_expected_register_details(contract_id, amount, transaction_type, currency=Currencies.RUB):
    return {
        'HANDLINGTIME': START_DT.strftime('%Y%m%d%H%M%S'),
        'CONTRACT_ID': contract_id,
        'PAYMENTTIME': START_DT.strftime('%Y%m%d%H%M%S'),
        'SUM': amount,
        'TRANSACTION_TYPE': transaction_type.name,
        'CURRENCY': currency.char_code,
        'SERVICE_ORDER_ID': '',
        'PAYMENT_TYPE': PaymentType.SVO,
        'PAYLOAD': '',
        'YANDEX_REWARD': 0
    }


def create_contract():
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(TAXI_RU_CONTEXT,
                                                                                       additional_params={
                                                                                           'start_dt': START_DT})
    return client_id, person_id, contract_id


def process_registers():
    with reporter.step(u"Создаем реестры по платежам"):
        api.test_balance().ProcessUncomposedPayments()


def get_latest_register():
    with reporter.step(u"Получаем данные последнего реестра из t_partner_payment_registry"):
        query = "SELECT * FROM t_partner_payment_registry WHERE REGISTRY_DATE = (SELECT MAX(REGISTRY_DATE) FROM t_partner_payment_registry) ORDER BY ID DESC"
        return db.balance().execute(query)[0]


def count_registers():
    with reporter.step(u"Считаем количество реестров в t_partner_payment_registry"):
        query = "SELECT count(*) cnt FROM t_partner_payment_registry"
        return db.balance().execute(query)[0]['cnt']


def create_expected_register(amount):
    return {
        'registry_date': greater_than_or_equal_to(utils.Date.nullify_time_of_date(datetime.now())),
        'service_id': SERVICE.id,
        'total_amount': amount
    }


def count_register_letters():
    with reporter.step(u"Считаем количество писем с типом 21"):
        query = "SELECT COUNT(*) cnt FROM T_MESSAGE WHERE OPCODE=21"
        return db.balance().execute(query)[0]['cnt']
