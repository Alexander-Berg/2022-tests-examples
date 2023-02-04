# coding=utf-8
import pytest

import balance.balance_api as balance_api
from balance import balance_steps
from balance.balance_api import TransferInfo
from btestlib.data import defaults

pytestmark = pytest.mark.xfail


# фикстуры
@pytest.fixture
def client():
    return balance_steps.ClientSteps.create()


@pytest.fixture
def orders(client):
    return [balance_steps.TransferSteps.prepare_order_for_transfer(client) for i in range(3)]


# тесты
INVALID_PASSPORT_UID = -1


def test_create_operation():
    response = balance_api.medium().create_operation(defaults.PASSPORT_UID)
    assert isinstance(response, int)  # TODO окрасивить
    # check(response, isNumber(), 'Проверяем, что ответ метода является числом')


def test_create_operation_invalid_passport_id():
    response = balance_api.medium().create_operation(INVALID_PASSPORT_UID)
    assert 'Passport with ID {} not found in DB'.format(INVALID_PASSPORT_UID) in str(response)  # TODO окрасивить
    # check(response, contains('Passport with ID {} not found in DB'.format(INVALID_PASSPORT_UID)))


def test_create_transfer_asinc(orders):
    operation_id = balance_steps.TransferSteps.create_operation(defaults.PASSPORT_UID)
    response = balance_api.medium().create_transfer_multiple(
        TransferInfo().add_from_order(service_id=7, service_order_id=orders[0], qty_old=100, qty_new=50) \
            .add_to_order(service_id=7, service_order_id=orders[1]) \
            .add_to_order(service_id=7, service_order_id=orders[2]),
        defaults.PASSPORT_UID, operation_id)
    # check(response, is_success())


def test_test_test():
    client_id = balance_steps.ClientSteps.create()




    # if __name__ == '__main__':
    #     pytest.main()
