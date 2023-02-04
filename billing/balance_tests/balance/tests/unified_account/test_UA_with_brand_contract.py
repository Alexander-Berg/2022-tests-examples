# -*- coding: utf-8 -*-
import datetime as d
import pytest
import json

from balance import balance_db as db
from dateutil.relativedelta import relativedelta

from balance import balance_steps as steps
from btestlib import utils, reporter
from btestlib.constants import Products, Paysyses, ContractCommissionType, ContractPaymentType, Services, Collateral, \
    ContractCreditType
from balance.features import Features
from btestlib.data.defaults import Date

pytestmark = [reporter.feature(Features.UNIFIED_ACCOUNT)]


@pytest.mark.tickets('BALANCE-28996')
def test_ua_transfer_after_brand_contract_finish(shared_data):
    # Все действия производим вчера, чтоб запустить разбор общего счета сегодня
    dt = d.datetime.now() - relativedelta(days=1)

    agency_id, client_id_1, person_id, contract_with_agency_id = prepare_contract_with_credit(dt)

    brand_contract_id = prepare_brand_contract_and_orders(agency_id, client_id_1, person_id, contract_with_agency_id,
                                                          dt, contract_dt=dt - relativedelta(days=1))

    with reporter.step(u'Меняем дату окончания договора на сегодня'):
        now_dt = d.datetime.now()
        db.balance().execute("UPDATE T_CONTRACT_ATTRIBUTES "
                             "SET VALUE_DT = TO_DATE( :now_dt, 'YYYY-MM-DD HH24:MI:SS') "
                             "WHERE  CODE = 'FINISH_DT' "
                             "AND ATTRIBUTE_BATCH_ID = ("
                             "SELECT ATTRIBUTE_BATCH_ID FROM T_CONTRACT_COLLATERAL WHERE CONTRACT2_ID = :contract_id)",
                             {'contract_id': brand_contract_id, 'now_dt': now_dt.strftime("%Y-%m-%d 00:00:00")
                              })
        steps.ContractSteps.refresh_contracts_cache(brand_contract_id)

    # Разбираем общий счет
    steps.OrderSteps.ua_enqueue([agency_id])
    steps.CommonSteps.export('UA_TRANSFER', 'Client', agency_id)


def prepare_contract_with_credit(dt):
    agency_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    person_id = steps.PersonSteps.create(agency_id, 'ur')
    client_id_1 = steps.ClientSteps.create()

    # Заключаем договор с агенством
    contract_params = {'CLIENT_ID': agency_id,
                       'PERSON_ID': person_id,
                       'DT': utils.Date.date_to_iso_format(dt - relativedelta(days=180)),
                       'FINISH_DT': utils.Date.date_to_iso_format(dt + relativedelta(days=180)),
                       'IS_SIGNED': utils.Date.date_to_iso_format(dt - relativedelta(days=180)),
                       'PAYMENT_TYPE': ContractPaymentType.POSTPAY,
                       'CREDIT_TYPE': ContractCreditType.BY_TERM_AND_SUM}

    contract_with_agency_id, contract_eid = \
        steps.ContractSteps.create_contract_new(ContractCommissionType.OPT_AGENCY_PREM, contract_params)

    # Выдаем индивидуальный кредитный лимит клиенту client_1
    credit_limits = [{"id": "1",
                      "num": "{0}".format(client_id_1),
                      "client": "{0}".format(client_id_1),
                      "client_limit": "1000.44",
                      "client_payment_term": "45",
                      "client_credit_type": "1",
                      "client_limit_currency": "RUR"}]

    collateral_params = {'CONTRACT2_ID': contract_with_agency_id,
                         'CLIENT_LIMITS': str(json.dumps(credit_limits)),
                         'DT': utils.Date.date_to_iso_format(dt),
                         'IS_SIGNED': utils.Date.date_to_iso_format(dt - relativedelta(days=180)),
                         }

    steps.ContractSteps.create_collateral(Collateral.SUBCLIENT_CREDIT_LIMIT, collateral_params)
    return agency_id, client_id_1, person_id, contract_with_agency_id


def prepare_brand_contract_and_orders(agency_id, client_id_1, person_id, contract_with_agency_id, dt, contract_dt):
    client_id_2 = steps.ClientSteps.create()

    contract_params = {
        'CLIENT_ID': client_id_1,
        'PERSON_ID': person_id,
        'SERVICES': [Services.DIRECT.id],
        'DT': utils.Date.to_iso(utils.Date.nullify_time_of_date(dt + relativedelta(days=3))),
        'IS_SIGNED': utils.Date.to_iso(utils.Date.nullify_time_of_date(dt)),
        'FINISH_DT': Date.HALF_YEAR_AFTER_TODAY_ISO,
        'BRAND_TYPE': 7,
        'BRAND_CLIENTS': json.dumps([{"id": "1", "num": client_id_1, "client": client_id_1},
                                     {"id": "2", "num": client_id_2, "client": client_id_2}])
    }

    brand_contract_id, contract_external_id = steps.ContractSteps.create_contract_new(ContractCommissionType.BRAND,
                                                                                      contract_params)

    contract_dt = utils.Date.nullify_time_of_date(contract_dt)

    #  Сейчас дата начала договора в будущем (иначе договор не создается), апдейтом меняем ее на прошлое
    with reporter.step(u'Меняем дату начала договора на {date}'.format(date=contract_dt)):
        db.balance().execute("UPDATE t_contract_collateral SET dt = :dt WHERE contract2_id = :contract_id",
                             {'dt': contract_dt, 'contract_id': brand_contract_id})
        steps.ContractSteps.refresh_contracts_cache(brand_contract_id)

    service_id = Services.DIRECT.id
    product_id = Products.DIRECT_FISH.id
    paysys_id = Paysyses.BANK_UR_RUB.id

    # Заказ 1, главный
    service_order_id_1 = steps.OrderSteps.next_id(service_id=service_id)
    order_id_1 = steps.OrderSteps.create(client_id_2, service_order_id_1, service_id=service_id,
                                         product_id=product_id, params={'AgencyID': agency_id})
    orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id_1, 'Qty': 20, 'BeginDT': dt}]
    request_id = steps.RequestSteps.create(agency_id, orders_list, additional_params=dict(InvoiceDesireDT=dt))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, paysys_id, credit=1,
                                                 contract_id=contract_with_agency_id, overdraft=0,
                                                 endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id_1, {'Bucks': 10}, 0, dt)

    # Заказ 2, дочерний
    service_order_id_2 = steps.OrderSteps.next_id(service_id=service_id)
    order_id_2 = steps.OrderSteps.create(client_id_2, service_order_id_2, service_id=service_id,
                                         product_id=product_id,
                                         params={'AgencyID': agency_id, 'GroupServiceOrderID': service_order_id_1})
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id_2, {'Bucks': 10}, 0, dt)
    return brand_contract_id
