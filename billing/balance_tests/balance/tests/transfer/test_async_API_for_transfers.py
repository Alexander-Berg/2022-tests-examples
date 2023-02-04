# coding=utf-8
import datetime
from xmlrpclib import Fault

import pytest
from hamcrest import contains_string, not_, empty, equal_to, all_of, instance_of, greater_than

import btestlib.reporter as reporter
from balance import balance_api as api
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils
from btestlib.data import defaults

# фикстуры
# @pytest.fixture
# def client():
#     return balance_steps.ClientSteps.create()
#

# @pytest.fixture
# def orders(client):
#     return [balance_steps.TransferSteps.prepare_order_for_transfer(client) for i in range(3)]

pytestmark = [pytest.mark.priority('mid'),
              reporter.feature(Features.TRANSFER, Features.OPERATION),
              pytest.mark.tickets('BALANCE-20227'),
              pytest.mark.docpath('https://wiki.yandex-team.ru/balance/docs/process/transfer')]

SERVICE_ID = 7
PAYSYS_ID = 1003
QTY = 100
QTY_NEW = [90, 85, 70.13]
PERSON_TYPE = 'ur'

INVALID_PASSPORT_UID = -1
OTHER_PASSPORT_UID = 16571029


def data_generator():
    service_id = SERVICE_ID
    qty = QTY
    paysys_id = PAYSYS_ID
    base_dt = datetime.datetime.now()

    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)

    service_order_ids = []
    order_ids = []
    for i in range(3):
        service_order_id = steps.OrderSteps.next_id(service_id)
        service_order_ids.append(service_order_id)
        order_ids.append(steps.OrderSteps.create(client_id, service_order_id))

    orders_list = [
        {'ServiceID': service_id, 'ServiceOrderID': service_order_ids[0], 'Qty': qty, 'BeginDT': base_dt}
    ]
    request_id = steps.RequestSteps.create(client_id, orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, paysys_id, credit=0, contract_id=None,
                                                 overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)

    return service_order_ids


def custom_transfer(service_order_ids, operation_id, passport_uid=defaults.PASSPORT_UID, incorrect_old_qty=False,
                    result_return=1):
    patched_qty = QTY if not incorrect_old_qty else QTY + 1
    response = api.medium().CreateTransferMultiple(passport_uid,
                                                   [
                                                       {"ServiceID": SERVICE_ID,
                                                        "ServiceOrderID": service_order_ids[0],
                                                        "QtyOld": patched_qty, "QtyNew": QTY_NEW[0]}
                                                   ],
                                                   [
                                                       {"ServiceID": SERVICE_ID,
                                                        "ServiceOrderID": service_order_ids[1],
                                                        "QtyDelta": 1}
                                                       , {"ServiceID": SERVICE_ID,
                                                          "ServiceOrderID": service_order_ids[2],
                                                          "QtyDelta": 2}
                                                   ], result_return, operation_id)
    return response


@pytest.fixture(scope="module")
def module_service_order_ids():
    return data_generator()


@pytest.fixture
def service_order_ids():
    return data_generator()


# TODO All asserts should check RESULT, INPUT, ERROR
@reporter.feature(Features.TO_UNIT)
@pytest.mark.docs(u'Работа метода CreateOperation')
def test_create_operation():
    response = api.medium().CreateOperation(defaults.PASSPORT_UID)
    utils.check_that(response, all_of(instance_of(int), greater_than(1)), step=u'Проверяем что ответ число')


@reporter.feature(Features.TO_UNIT)
# @pytest.mark.docs(u'')
def test_create_operation_invalid_passport_id():
    try:
        response = api.medium().CreateOperation(INVALID_PASSPORT_UID)
    except Exception, exc:
        assert 'Passport with ID {0} not found in DB'.format(INVALID_PASSPORT_UID) in str(exc)  # TODO окрасивить
        # check(response, contains('Passport with ID {} not found in DB'.format(INVALID_PASSPORT_UID)))


@reporter.feature(Features.TO_UNIT)
# @pytest.mark.docs(u'')
def test_transfer_with_unexisted_passport_uid(module_service_order_ids):
    operation_id = steps.TransferSteps.create_operation()
    with pytest.raises(Fault) as error:
        response = custom_transfer(module_service_order_ids, operation_id,
                                   passport_uid=INVALID_PASSPORT_UID, incorrect_old_qty=False, result_return=1)
    utils.check_that(str(error.value),
                     contains_string("User {} has no permission SomePermission.".format(INVALID_PASSPORT_UID)),
                     step=u'Проверяем корректность текста ошибки')


@reporter.feature(Features.TO_UNIT)
@pytest.mark.docs(u'Несовпадение passport_uid (1 вызов):', u'Несовпадение passport_uid (последующие вызовы)')
def test_transfer_with_incompatiable_passport_uids(module_service_order_ids):
    operation_id = steps.TransferSteps.create_operation(defaults.PASSPORT_UID)
    with pytest.raises(Fault) as error:
        response = custom_transfer(module_service_order_ids, operation_id,
                                   passport_uid=OTHER_PASSPORT_UID, incorrect_old_qty=False, result_return=1)
    utils.check_that(str(error.value),
                     contains_string("User {} has no permission SomePermission.".format(OTHER_PASSPORT_UID)),
                     step=u'Проверяем корректность текста ошибки')
    with pytest.raises(Fault) as error:
        response_next_call = custom_transfer(module_service_order_ids, operation_id,
                                             passport_uid=OTHER_PASSPORT_UID, incorrect_old_qty=False, result_return=1)
    utils.check_that(str(error.value),
                     contains_string("User {} has no permission SomePermission.".format(OTHER_PASSPORT_UID)),
                     step=u'Проверяем корректность текста ошибки')
        # TODO: Fault: <Fault -1: '<error><msg>QTY for order_id = 18312381 do not match (balance value=100.000000, service value=101.000000</msg><balance-consume-qty /><order-id>18312381</order-id><wo-rollback>1</wo-rollback><service-consume-qty>101</service-consume-qty><method>CreateTransferMultiple</method><code>ORDERS_NOT_SYNCHRONIZED</code><parent-codes><code>EXCEPTION</code></parent-codes><contents>QTY for order_id = 18312381 do not match (balance value=100.000000, service value=101.000000</contents></error>'>


@reporter.feature(Features.TO_UNIT)
@pytest.mark.docs(u'Ошибка в переносе', u'Повторный вызов после ошибки')
def test_transfer_with_exception(module_service_order_ids):
    operation_id = steps.TransferSteps.create_operation(defaults.PASSPORT_UID)
    with pytest.raises(Fault) as error:
        response = custom_transfer(module_service_order_ids, operation_id,
                                   passport_uid=defaults.PASSPORT_UID, incorrect_old_qty=True, result_return=1)
    utils.check_that(str(error.value), contains_string('ORDERS_NOT_SYNCHRONIZED'),
                     step=u'Проверяем корректность текста ошибки')
    with pytest.raises(Fault) as error:
        response_next_call = custom_transfer(module_service_order_ids, operation_id,
                                             passport_uid=defaults.PASSPORT_UID, incorrect_old_qty=True,
                                             result_return=1)
    utils.check_that(str(error.value), contains_string('ORDERS_NOT_SYNCHRONIZED'),
                     step=u'Проверяем корректность текста ошибки')


@pytest.mark.docs(u'Успешный перенос (ответ на 1 вызов)', u'Успешный перенос (ответ на последующие вызовы)')
def test_successful_transfer(service_order_ids):
    operation_id = steps.TransferSteps.create_operation()
    response = custom_transfer(service_order_ids, operation_id,
                               passport_uid=defaults.PASSPORT_UID, incorrect_old_qty=False, result_return=1)
    # TODO: dict comparison; redesign assert
    # Response: ([{'ServiceID': 7, 'ServiceOrderID': 47603020, 'Qty': '-10'}, {'ServiceID': 7, 'ServiceOrderID': 47603022, 'Qty': '6.666667'}, {'ServiceID': 7, 'ServiceOrderID': 47603021, 'Qty': '3.333333'}],)
    utils.check_that(response, not_(empty), step=u'Проверяем, что в ответе не пусто')
    response = custom_transfer(service_order_ids, operation_id,
                               passport_uid=defaults.PASSPORT_UID, incorrect_old_qty=False, result_return=1)
    utils.check_that(response, not_(empty), step=u'Проверяем, что в ответе не пусто')


@pytest.mark.docs(u'Комбинация с возвратом\невозвратом результата')
def test_without_result_return(service_order_ids):
    operation_id = steps.TransferSteps.create_operation()
    response = custom_transfer(service_order_ids, operation_id,
                               passport_uid=defaults.PASSPORT_UID, incorrect_old_qty=False, result_return=0)
    # TODO: redesign assert
    # Response: ([{'ServiceID': 7, 'ServiceOrderID': 47603020, 'Qty': '-10'}, {'ServiceID': 7, 'ServiceOrderID': 47603022, 'Qty': '6.666667'}, {'ServiceID': 7, 'ServiceOrderID': 47603021, 'Qty': '3.333333'}],)
    utils.check_that(response, equal_to([0, 'Success']), step=u'Проверяем, что в ответе Success')
    response = custom_transfer(service_order_ids, operation_id,
                               passport_uid=defaults.PASSPORT_UID, incorrect_old_qty=False, result_return=0)
    # TODO: redesign assert
    utils.check_that(response, equal_to([0, 'Success']), step=u'Проверяем, что в ответе Success')


# TODO OPERATION_IN_PROGRESS test - how to do?
# def test_create_transfer_asinc(orders):
#     operation_id = balance_steps.TransferSteps.create_operation(data.PASSPORT_UID)
#     response = balance_api.medium().create_transfer_multiple(
#         TransferInfo().add_from_order(service_id=7, service_order_id=orders[0], qty_old=100, qty_new=50) \
#             .add_to_order(service_id=7, service_order_id=orders[1]) \
#             .add_to_order(service_id=7, service_order_id=orders[2]),
#         defaults.PASSPORT_UID, operation_id)
# check(response, is_success())

if __name__ == "__main__":
    pytest.main('-v --docs "1" --collect-only')
