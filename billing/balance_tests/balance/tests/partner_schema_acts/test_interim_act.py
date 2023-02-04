# coding=utf-8

"""
Проверка формирования промежуточного акта через ручку GenerateInterimPartnerAct
"""

from decimal import Decimal as D

import datetime
import pytest
from hamcrest import equal_to

import balance.balance_api as api
from balance import balance_db as db
from balance import balance_steps as steps
from balance.balance_steps import CommonSteps
from btestlib import utils
from btestlib.constants import ContractPaymentType, ContractCommissionType, Products, Firms, Currencies
from temp.igogor.balance_objects import Contexts

to_iso = utils.Date.date_to_iso_format

APIKEYS_CONTEXT = Contexts.APIKEYS_CONTEXT.new(
    firm=Firms.YANDEX_1,
    currency=Currencies.RUB
)
AMOUNT_MINIMAL = D('100000')
AMOUNT_OVERLIMIT = D('1000')


def acted_sum(contract_id):
    acts_data = steps.ActsSteps.get_act_data_by_contract(contract_id)
    return sum(act['act_sum'] for act in acts_data)


def consumed_sum(context, contract_id):
    r = steps.PartnerSteps.get_partner_balance(service=context.service, contract_ids=[contract_id])
    return D(r[0]['ConsumeSum'])


@pytest.mark.parametrize('use_turn_on_request', [False, True])
def test_generate_partner_act_apikeys(use_turn_on_request):
    """Проверка генерации промежуточного партнерского акта для APIKEYS (BALANCE-39159)"""

    context = APIKEYS_CONTEXT

    # сервис создает договор
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id=client_id, type_=context.person_type.code)
    contract_params = {
        'PERSON_ID': person_id,
        'CLIENT_ID': client_id,
        'IS_SIGNED': to_iso(datetime.datetime.now() - datetime.timedelta(days=180)),
        'FINISH_DT': to_iso(datetime.datetime.now() + datetime.timedelta(days=180)),
        'SERVICES': [context.service.id],
        'FIRM': context.firm.id,
        'PAYMENT_TYPE': ContractPaymentType.POSTPAY,
        'PERSONAL_ACCOUNT': 1,
        'PARTNER_CREDIT': 1,
    }
    contract_id, _ = steps.ContractSteps.create_contract_new(type_=ContractCommissionType.NO_AGENCY,
                                                             params=contract_params)

    # сервис создает заказ и счет-квитанцию на "минималку"
    service_order_id_1 = steps.OrderSteps.next_id(context.service.id)
    steps.OrderSteps.create(client_id=client_id,
                            service_order_id=service_order_id_1,
                            product_id=Products.APIKEYS_ROUTES_COURIER_MINIMAL.id,
                            service_id=context.service.id,
                            contract_id=contract_id)
    request_id_1 = steps.RequestSteps.create(client_id=client_id,
                                             orders_list=[
                                                 {
                                                     'ServiceID': context.service.id,
                                                     'ServiceOrderID': service_order_id_1,
                                                     'Qty': AMOUNT_MINIMAL,
                                                     'BeginDT': datetime.datetime.now()
                                                 }],
                                             additional_params={
                                                 'InvoiceDesireDT': datetime.datetime.now(),
                                                 'InvoiceDesireType': 'charge_note',
                                                 'LinkChargeNotePayment': 1,
                                             })
    if use_turn_on_request:
        api.medium().TurnOnRequest({
            'ContractID': contract_id,
            'RequestID': request_id_1
        })
    invoice_id_1, invoice_external_id_1, invoice_total_sum_1 = \
        steps.InvoiceSteps.create(request_id=request_id_1,
                                  person_id=person_id,
                                  paysys_id=context.paysys.id,
                                  contract_id=contract_id)

    # клиент оплачивает "минималку"
    steps.InvoiceSteps.create_cash_payment_fact(invoice_eid=invoice_external_id_1,
                                                amount=AMOUNT_MINIMAL,
                                                dt=datetime.datetime.now(),
                                                type='INSERT',
                                                invoice_id=invoice_id_1)

    # убеждаемся, что не сгенерировались никакие лишние конзюмы при обработке платежа
    if use_turn_on_request:
        assert consumed_sum(context, contract_id) == AMOUNT_MINIMAL
    else:
        assert consumed_sum(context, contract_id) == D('0')

    # сервис отправляет открутки
    steps.CampaignsSteps.update_campaigns(service_id=context.service.id,
                                          service_order_id=service_order_id_1,
                                          campaign_params={
                                              Products.APIKEYS_ROUTES_COURIER_MINIMAL.type.code: AMOUNT_MINIMAL
                                          },
                                          do_stop=0,
                                          campaigns_dt=datetime.datetime.now())

    # сервис дергает ручку для формирования промежуточного акта (на "минималку")
    response = steps.CommonPartnerSteps.generate_interim_partner_act(contract_id, service_id=context.service.id,
                                                                     date=datetime.datetime.now(),
                                                                     service_orders_ids=[service_order_id_1])

    # убеждаемся, что сгенерировались конзюмы и акты на нужную сумму
    assert consumed_sum(context, contract_id) == AMOUNT_MINIMAL
    assert acted_sum(contract_id) == AMOUNT_MINIMAL

    for row in response:
        act_data = steps.ActsSteps.get_act_data_by_id(row['ID'])
        assert 'GenerateInterimPartnerAct' in act_data['memo']
        assert row['ExternalID'] == act_data['external_id']

    # сервис создает заказы для "превышений"
    service_order_id_2 = steps.OrderSteps.next_id(context.service.id)
    order_id_2 = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id_2,
                                         product_id=Products.APIKEYS_ROUTES_COURIER_OVERLIMIT.id,
                                         service_id=context.service.id,
                                         contract_id=contract_id)

    orders_list = [{
        'ServiceID': context.service.id,
        'ServiceOrderID': service_order_id_2,
        'Qty': AMOUNT_OVERLIMIT,
        'BeginDT': datetime.datetime.now()
    }]
    _ = steps.RequestSteps.create(client_id, orders_list)

    # сервис шлет открутки
    steps.CampaignsSteps.update_campaigns(service_id=context.service.id,
                                          service_order_id=service_order_id_2,
                                          campaign_params={
                                              Products.APIKEYS_ROUTES_COURIER_OVERLIMIT.type.code: AMOUNT_OVERLIMIT
                                          },
                                          do_stop=0,
                                          campaigns_dt=datetime.datetime.now())

    CommonSteps.export('PROCESS_COMPLETION', 'Order', order_id_2)
    assert consumed_sum(context, contract_id) == AMOUNT_MINIMAL

    # при закрытии месяца генерируется акт на "превышения"
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(partner_id=client_id,
                                                                   contract_id=contract_id,
                                                                   dt=datetime.datetime.now())
    assert consumed_sum(context, contract_id) == AMOUNT_MINIMAL + AMOUNT_OVERLIMIT
    assert acted_sum(contract_id) == AMOUNT_MINIMAL + AMOUNT_OVERLIMIT


def test_generate_partner_act_apikeys_prepayment_invoice():
    """Проверка генерации промежуточного партнерского акта на основе предоплатного Б-счета"""

    context = APIKEYS_CONTEXT

    # сервис создает договор
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id=client_id, type_=context.person_type.code)
    contract_params = {
        'PERSON_ID': person_id,
        'CLIENT_ID': client_id,
        'IS_SIGNED': to_iso(datetime.datetime.now() - datetime.timedelta(days=180)),
        'FINISH_DT': to_iso(datetime.datetime.now() + datetime.timedelta(days=180)),
        'SERVICES': [context.service.id],
        'FIRM': context.firm.id,
        'PAYMENT_TYPE': ContractPaymentType.POSTPAY,
        'PERSONAL_ACCOUNT': 1,
        'PARTNER_CREDIT': 1,
    }
    contract_id, _ = steps.ContractSteps.create_contract_new(type_=ContractCommissionType.NO_AGENCY,
                                                             params=contract_params)

    # сервис создает заказ и счет на "минималку"
    service_order_id = steps.OrderSteps.next_id(context.service.id)
    steps.OrderSteps.create(client_id=client_id,
                            service_order_id=service_order_id,
                            product_id=Products.APIKEYS_ROUTES_COURIER_MINIMAL.id,
                            service_id=context.service.id,
                            contract_id=contract_id)
    request_id = steps.RequestSteps.create(client_id=client_id,
                                           orders_list=[
                                               {
                                                   'ServiceID': context.service.id,
                                                   'ServiceOrderID': service_order_id,
                                                   'Qty': AMOUNT_MINIMAL,
                                                   'BeginDT': datetime.datetime.now()
                                               }
                                           ])
    invoice_id, invoice_external_id, invoice_total_sum = \
        steps.InvoiceSteps.create(request_id=request_id,
                                  person_id=person_id,
                                  paysys_id=context.paysys.id,
                                  contract_id=contract_id,
                                  turn_on=True)

    # клиент оплачивает "минималку"
    steps.InvoiceSteps.create_cash_payment_fact(invoice_eid=invoice_external_id,
                                                amount=AMOUNT_MINIMAL,
                                                dt=datetime.datetime.now(),
                                                type='INSERT',
                                                invoice_id=invoice_id)

    # сервис отправляет открутки
    steps.CampaignsSteps.do_campaigns(service_id=context.service.id, 
                                      service_order_id=service_order_id,
                                      campaigns_params={
                                          Products.APIKEYS_ROUTES_COURIER_MINIMAL.type.code: AMOUNT_MINIMAL
                                      },
                                      campaigns_dt=datetime.datetime.now())

    # генерируется ежедневный акт
    acts = steps.ActsSteps.generate(client_id=client_id, force=0)

    # проверяем акт
    assert len(acts) == 1
    act_id = acts[0]
    act = db.get_act_by_id(act_id)[0]
    print(act)
    utils.check_that(int(act['amount']), equal_to(AMOUNT_MINIMAL), step=u'Проверяем, сумму акта')
    assert int(act['invoice_id']) == invoice_id
