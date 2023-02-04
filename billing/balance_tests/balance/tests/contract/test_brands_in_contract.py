# coding: utf-8

import datetime
import hamcrest
import json
import pytest

from dateutil.relativedelta import relativedelta
from btestlib import utils, shared
from balance import balance_db as db
from balance import balance_steps as steps
from btestlib.constants import Services, PersonTypes, Firms, Collateral, ContractPaymentType, ContractCreditType, \
    ContractCommissionType, Products, Paysyses

LAST_DAY_OF_PREVIOUS_MONTH = datetime.datetime.now().replace(day=1) - datetime.timedelta(days=1)
OVERDRAFT_OPCODE = 11
CLIENT_OPCODE = 10
NOW = utils.Date.nullify_time_of_date(datetime.datetime.now())


def test_brand_contract_unable_with_autooverdraft():
    client_limit = 10000
    service_id = Services.DIRECT.id

    client_id_1 = steps.ClientSteps.create()

    client_id_2 = steps.ClientSteps.create()
    person_id_2 = steps.PersonSteps.create(client_id_2, PersonTypes.UR.code)

    steps.ClientSteps.migrate_to_currency(client_id_1, currency_convert_type='MODIFY',
                                          dt=LAST_DAY_OF_PREVIOUS_MONTH - relativedelta(days=2))
    steps.ClientSteps.migrate_to_currency(client_id_2, currency_convert_type='MODIFY',
                                          dt=LAST_DAY_OF_PREVIOUS_MONTH - relativedelta(days=2))

    steps.OverdraftSteps.set_force_overdraft(client_id_2, service_id, client_limit, Firms.YANDEX_1.id,
                                             currency='RUB')

    steps.OverdraftSteps.set_overdraft_params(person_id=person_id_2, client_limit=client_limit)

    try:
        _, _ = steps.ContractSteps.create_brand_contract(client_id_1, client_id_2, dt=NOW)
    except Exception, exc:
        utils.check_that(steps.CommonSteps.get_exception_code(exc), hamcrest.equal_to('CONTRACT_RULE_VIOLATION'))
        utils.check_that(steps.CommonSteps.get_exception_code(exc, tag_name='msg'),
                         hamcrest.equal_to(u"Rule violation: 'Клиент(ы) " + str(client_id_2) +
                                           u" имеют автоовердрафт. Включение их в бренд запрещено.'"))
    else:
        raise utils.TestsError(u"Клиенту доступна техсвязка при подключенном автоовердрафте")

    try:
        _, _ = steps.ContractSteps.create_brand_contract(client_id_2, client_id_1, dt=NOW)
    except Exception, exc:
        utils.check_that(steps.CommonSteps.get_exception_code(exc), hamcrest.equal_to('CONTRACT_RULE_VIOLATION'))
        utils.check_that(steps.CommonSteps.get_exception_code(exc, tag_name='msg'),
                         hamcrest.equal_to(u"Rule violation: 'Клиент(ы) " + str(client_id_2) +
                                           u" имеют автоовердрафт. Включение их в бренд запрещено.'"))
    else:
        raise utils.TestsError(u"Клиенту доступна техсвязка при подключенном автоовердрафте")


def test_brand_contract_unable_with_other_brand():
    """договоры с перечением по брендам"""
    client_id_1 = steps.ClientSteps.create()
    client_id_2 = steps.ClientSteps.create()
    _, _ = steps.ContractSteps.create_brand_contract(client_id_1, client_id_2, dt=NOW)

    client_id_3 = steps.ClientSteps.create()

    with pytest.raises(Exception) as exc_info:
        _, _ = steps.ContractSteps.create_brand_contract(
            client_id_3, client_id_2,
            dt=NOW + datetime.timedelta(1),
            force_dt=False
        )

    exc = exc_info.value
    utils.check_that(steps.CommonSteps.get_exception_code(exc), hamcrest.equal_to('CONTRACT_RULE_VIOLATION'))
    utils.check_that(steps.CommonSteps.get_exception_code(exc, tag_name='msg'),
                     hamcrest.equal_to(u"Rule violation: 'Клиент(ы) " + str(client_id_2) +
                                       u" имеют активные бренды. Включение их в бренд запрещено.'"))

    with pytest.raises(Exception) as exc_info:
        _, _ = steps.ContractSteps.create_brand_contract(
            client_id_2, client_id_3,
            dt=NOW + datetime.timedelta(1),
            force_dt=False
        )

    exc = exc_info.value
    utils.check_that(steps.CommonSteps.get_exception_code(exc), hamcrest.equal_to('CONTRACT_RULE_VIOLATION'))
    utils.check_that(steps.CommonSteps.get_exception_code(exc, tag_name='msg'),
                     hamcrest.equal_to(u"Rule violation: 'Клиент(ы) " + str(client_id_2) +
                                       u" имеют активные бренды. Включение их в бренд запрещено.'"))


def test_brand_contract_unable_with_other_brand_collaterals():
    """дc с перечением по брендам"""
    client_id_1 = steps.ClientSteps.create()
    client_id_2 = steps.ClientSteps.create()
    _, _ = steps.ContractSteps.create_brand_contract(client_id_1, client_id_2, dt=NOW)

    client_id_3 = steps.ClientSteps.create()
    client_id_4 = steps.ClientSteps.create()
    client_id_5 = steps.ClientSteps.create()
    brand_contract_id, _ = steps.ContractSteps.create_brand_contract(client_id_3, client_id_4, dt=NOW)

    collateral_params = {'CONTRACT2_ID': brand_contract_id}
    collateral_params.update({'DT': utils.Date.to_iso(utils.Date.nullify_time_of_date(NOW + relativedelta(days=3))),
                              'IS_SIGNED': utils.Date.to_iso(NOW),
                              'BRAND_CLIENTS': json.dumps([{"id": "1", "num": client_id_1, "client": client_id_1},
                                                           {"id": "1", "num": client_id_3, "client": client_id_3},
                                                           {"id": "2", "num": client_id_5, "client": client_id_5}])})

    with pytest.raises(Exception) as exc_info:
        steps.ContractSteps.create_collateral(Collateral.BRAND_CHANGE, collateral_params)

    exc = exc_info.value
    utils.check_that(steps.CommonSteps.get_exception_code(exc), hamcrest.equal_to('CONTRACT_RULE_VIOLATION'))
    utils.check_that(steps.CommonSteps.get_exception_code(exc, tag_name='msg'),
                     hamcrest.equal_to(u"Rule violation: 'Клиент(ы) " + str(client_id_1) +
                                       u" имеют активные бренды. Включение их в бренд запрещено.'"))


def test_brand_contract_enable_with_other_brand_already_finished():
    client_id_1 = steps.ClientSteps.create()
    client_id_2 = steps.ClientSteps.create()
    _, _ = steps.ContractSteps.create_brand_contract(
        client_id_1, client_id_2,
        dt=NOW,
        finish_dt=NOW + datetime.timedelta(days=2)
    )

    client_id_3 = steps.ClientSteps.create()

    _, _ = steps.ContractSteps.create_brand_contract(
        client_id_3, client_id_2,
        dt=NOW + datetime.timedelta(days=2),
        force_dt=False
    )


def test_delete_from_brand_with_free_funds():
    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    person_id = steps.PersonSteps.create(client_id=agency_id, type_=PersonTypes.UR.code)

    client_id_1 = steps.ClientSteps.create()
    client_id_2 = steps.ClientSteps.create()
    brand_contract_id, _ = steps.ContractSteps.create_brand_contract(client_id_1, client_id_2,
                                                                     dt=NOW - relativedelta(days=180),
                                                                     finish_dt=NOW + datetime.timedelta(days=2))
    client_id_1 = steps.ClientSteps.create()
    contract_params = {'CLIENT_ID': agency_id,
                       'PERSON_ID': person_id,
                       'DT': utils.Date.date_to_iso_format(NOW - relativedelta(days=180)),
                       'FINISH_DT': utils.Date.date_to_iso_format(NOW + relativedelta(days=180)),
                       'IS_SIGNED': utils.Date.date_to_iso_format(NOW - relativedelta(days=180)),
                       'PAYMENT_TYPE': ContractPaymentType.POSTPAY,
                       'CREDIT_TYPE': ContractCreditType.BY_TERM_AND_SUM,
                       'CREDIT_LIMIT_SINGLE': 1}

    contract_with_agency_id, contract_eid = \
        steps.ContractSteps.create_contract_new(ContractCommissionType.OPT_AGENCY_PREM, contract_params)

    # Выдаем индивидуальный кредитный лимит клиенту client_1
    credit_limits = [{"id": "1",
                      "num": "{0}".format(client_id_1),
                      "client": "{0}".format(client_id_1),
                      "client_limit": "1000",
                      "client_payment_term": "45",
                      "client_credit_type": "1000",
                      "client_limit_currency": "RUR"}]

    collateral_params = {'CONTRACT2_ID': contract_with_agency_id,
                         'CLIENT_LIMITS': str(json.dumps(credit_limits)),
                         'DT': utils.Date.date_to_iso_format(NOW),
                         'IS_SIGNED': utils.Date.date_to_iso_format(NOW - relativedelta(days=180)),
                         }

    steps.ContractSteps.create_collateral(Collateral.SUBCLIENT_CREDIT_LIMIT, collateral_params)

    service_id = Services.DIRECT.id
    product_id = Products.DIRECT_FISH.id
    paysys_id = Paysyses.BANK_UR_RUB.id

    service_order_id_1 = steps.OrderSteps.next_id(service_id=service_id)
    order_id_1 = steps.OrderSteps.create(client_id=client_id_1, service_order_id=service_order_id_1,
                                         service_id=service_id, product_id=product_id,
                                         params={'AgencyID': agency_id})
    orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id_1, 'Qty': 2, 'BeginDT': NOW}]

    request_id = steps.RequestSteps.create(agency_id, orders_list, additional_params=dict(InvoiceDesireDT=NOW))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, paysys_id, credit=1,
                                                 contract_id=contract_with_agency_id, overdraft=0,
                                                 endbuyer_id=None)
    collateral_params = {'CONTRACT2_ID': brand_contract_id}
    collateral_params.update({'DT': utils.Date.to_iso(utils.Date.nullify_time_of_date(NOW + relativedelta(days=180))),
                              'IS_SIGNED': utils.Date.to_iso(NOW),
                              'BRAND_CLIENTS': json.dumps([{"id": "1", "num": client_id_2, "client": client_id_2}])})

    try:
        steps.ContractSteps.create_collateral(Collateral.BRAND_CHANGE, collateral_params)
    except Exception, exc:
        utils.check_that(steps.CommonSteps.get_exception_code(exc), hamcrest.equal_to('CONTRACT_RULE_VIOLATION'))
        utils.check_that(steps.CommonSteps.get_exception_code(exc, tag_name='msg'),
                         hamcrest.equal_to(u"Rule violation: 'Клиент(ы) " + str(client_id_1) +
                                           u" имеют активные бренды. Включение их в бренд запрещено.'"))


def test_add_brand_twice(shared_data):
    """добавляем в допник на лимит клиента, у которого бренд уже был указан в лимите субклиента"""
    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    person_id = steps.PersonSteps.create(client_id=agency_id, type_=PersonTypes.UR.code)

    client_id_1 = steps.ClientSteps.create()
    client_id_2 = steps.ClientSteps.create()
    brand_contract_id, _ = steps.ContractSteps.create_brand_contract(client_id_1, client_id_2,
                                                                     dt=NOW - relativedelta(days=180),
                                                                     finish_dt=NOW + datetime.timedelta(days=2))
    # client_id_1 = steps.ClientSteps.create()
    contract_params = {'CLIENT_ID': agency_id,
                       'PERSON_ID': person_id,
                       'DT': utils.Date.date_to_iso_format(NOW - relativedelta(days=180)),
                       'FINISH_DT': utils.Date.date_to_iso_format(NOW + relativedelta(days=180)),
                       'IS_SIGNED': utils.Date.date_to_iso_format(NOW - relativedelta(days=180)),
                       'PAYMENT_TYPE': ContractPaymentType.POSTPAY,
                       'CREDIT_TYPE': ContractCreditType.BY_TERM_AND_SUM,
                       'CREDIT_LIMIT_SINGLE': 1}

    contract_with_agency_id, contract_eid = \
        steps.ContractSteps.create_contract_new(ContractCommissionType.OPT_AGENCY_PREM, contract_params)

    # Выдаем индивидуальный кредитный лимит клиенту client_1
    credit_limits = [{"id": "1",
                      "num": "{0}".format(client_id_1),
                      "client": "{0}".format(client_id_1),
                      "client_limit": "1000",
                      "client_payment_term": "45",
                      "client_credit_type": "1000",
                      "client_limit_currency": "RUR"}]

    collateral_params = {'CONTRACT2_ID': contract_with_agency_id,
                         'CLIENT_LIMITS': str(json.dumps(credit_limits)),
                         'DT': utils.Date.date_to_iso_format(NOW - relativedelta(days=1)),
                         'IS_SIGNED': utils.Date.date_to_iso_format(NOW - relativedelta(days=180)),
                         }

    steps.ContractSteps.create_collateral(Collateral.SUBCLIENT_CREDIT_LIMIT, collateral_params)

    credit_limits = [{"id": "1",
                      "num": "{0}".format(client_id_2),
                      "client": "{0}".format(client_id_2),
                      "client_limit": "1000",
                      "client_payment_term": "45",
                      "client_credit_type": "1000",
                      "client_limit_currency": "RUR"}]

    collateral_params = {'CONTRACT2_ID': contract_with_agency_id,
                         'CLIENT_LIMITS': str(json.dumps(credit_limits)),
                         'DT': utils.Date.date_to_iso_format(NOW),
                         'IS_SIGNED': utils.Date.date_to_iso_format(NOW - relativedelta(days=180)),
                         }

    with pytest.raises(Exception) as exc:
        steps.ContractSteps.create_collateral(Collateral.SUBCLIENT_CREDIT_LIMIT, collateral_params)

    utils.check_that(steps.CommonSteps.get_exception_code(exc.value), hamcrest.equal_to('CONTRACT_RULE_VIOLATION'))
    utils.check_that(steps.CommonSteps.get_exception_code(exc.value, tag_name='msg'),
                     hamcrest.is_in(
                         {u"Rule violation: 'У клиента(ов) " + ', '.join([str(client_id_1), str(client_id_2)]) +
                          u" есть техническая связка с другим клиентом с индивидуальным лимитом. '",
                          u"Rule violation: 'У клиента(ов) " + ', '.join([str(client_id_2), str(client_id_1)]) +
                          u" есть техническая связка с другим клиентом с индивидуальным лимитом. '"
                          }))
