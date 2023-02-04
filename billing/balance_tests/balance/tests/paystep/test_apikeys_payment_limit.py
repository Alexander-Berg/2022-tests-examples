# -*- coding: utf-8 -*-
from balance import balance_steps as steps
from balance import balance_db as db
from btestlib.data.defaults import Date
from btestlib.constants import ContractCommissionType, ContractPaymentType, Currencies, Services, Paysyses, Products
from btestlib import utils, reporter
from btestlib.environments import BalanceHosts as hosts
import hamcrest
import pytest
from balance.features import Features

terminal_limit_list = [
    {'id': Paysyses.CC_UR_RUB.id, 'payment_limit': '1000000'},
    {'id': Paysyses.CC_UR_RUB.id, 'payment_limit': '1000000'}
]


def get_request_for_apikeys(qty):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')
    contract_params = {
        'CLIENT_ID': client_id,
        'PERSON_ID': person_id,
        'PAYMENT_TYPE': ContractPaymentType.PREPAY,
        'SERVICES': [Services.APIKEYS.id],
        'DT': Date.TODAY_ISO,
        'FINISH_DT': Date.HALF_YEAR_AFTER_TODAY_ISO,
        'IS_SIGNED': Date.TODAY_ISO,
        'CURRENCY': Currencies.RUB.num_code
    }

    contract_id, _ = steps.ContractSteps.create_contract_new(ContractCommissionType.NO_AGENCY, contract_params)
    service_order_id = steps.OrderSteps.next_id(service_id=Services.APIKEYS.id)
    steps.OrderSteps.create(client_id, service_order_id, service_id=Services.APIKEYS.id,
                            product_id=Products.DIRECT_FISH.id)
    orders_list = [{'ServiceID': Services.APIKEYS.id, 'ServiceOrderID': service_order_id, 'Qty': qty,
                    'BeginDT': Date.NOW()}]
    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params=dict(InvoiceDesireDT=Date.NOW(),
                                                                                          FirmID=1))

    return client_id, person_id, request_id


def get_limit_list(request_id):
    limit_list = []
    for pcp in steps.RequestSteps.get_request_choices(request_id, show_disabled_paysyses=True)['pcp_list']:
        for paysys in pcp['paysyses']:
            if paysys['id'] in [limit['id'] for limit in terminal_limit_list]:
                limit_list.append(
                    {'id': paysys['id'], 'payment_limit': paysys['payment_limit']})
    return sorted(limit_list)


@reporter.feature(Features.TO_UNIT)
@pytest.mark.tickets('BALANCE-28225')
def test_cc_apikeys_terminal_payment_limits():
    _, _, request_id = get_request_for_apikeys(100)

    limit_list = get_limit_list(request_id)

    utils.check_that(limit_list, hamcrest.equal_to(terminal_limit_list),
                     step=u'Проверяем, что лимиты платежа берутся из терминала')

@pytest.mark.smoke
@pytest.mark.ignore_hosts(hosts.PT, hosts.PTY, hosts.PTA)
@pytest.mark.tickets('BALANCE-28225')
def test_cc_apikeys_terminal_payment_limits_for_reliable_client():
    client_id, _, request_id = get_request_for_apikeys(100)

    with reporter.step(u'Делаем плательщика надежным'):
        db.balance().execute("UPDATE T_CLIENT SET RELIABLE_CC_PAYER = 1 WHERE ID= :client_id", {'client_id': client_id})

    limit_list = get_limit_list(request_id)

    utils.check_that(limit_list, hamcrest.equal_to(terminal_limit_list),
                     step=u'Проверяем, что лимиты платежа берутся из терминала')
#
# ПРОВЕРКА ЕСТЬ В ЮНИТ-ТЕСТАХ
# @pytest.mark.tickets('BALANCE-28225')
# def test_unable_to_pay_over_limit():
#     _, person_id, request_id = get_request_for_apikeys(35000)
#
#     # Тут счет в любом случае не выставится, но можем проверить, что выпадает нужная ошибка
#     with pytest.raises(Exception) as exc:
#         invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, Paysyses.CC_UR_RUB.id, credit=0,
#                                                      contract_id=None, overdraft=0, endbuyer_id=None)
#     utils.check_that(steps.CommonSteps.get_exception_code(exc.value), hamcrest.equal_to('PAYSYS_LIMIT_EXCEEDED'),
#                      step=u'Проверяем, что нельзя выставить счет на сумму больше лимита платежа')
