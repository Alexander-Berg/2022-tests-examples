# coding: utf-8
import pytest
import datetime
import json
from balance import snout_steps
from balance import balance_db as db
from btestlib.constants import Services
from temp.igogor.balance_objects import Contexts, Firms, PersonTypes, Paysyses
from balance import balance_steps as steps

DIRECT_YANDEX_FIRM_FISH_UR = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1)
DIRECT_YANDEX_FIRM_FISH_PH = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1, person_type=PersonTypes.PH,
                                                                  paysys=Paysyses.BANK_PH_RUB)

NOW = datetime.datetime.now()


@pytest.mark.parametrize('context', [DIRECT_YANDEX_FIRM_FISH_PH,
                                     # DIRECT_YANDEX_FIRM_FISH_UR
                                     ])
def test_bank_payment(context):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    person_data = db.get_person_by_id(person_id)[0]
    steps.ClientSteps.set_force_overdraft(client_id, service_id=context.service.id, limit=100)
    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id,
                            product_id=context.product.id, params={'AgencyID': None})
    orders_list = [
        {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': 100,
         'BeginDT': NOW}]
    request_id = steps.RequestSteps.create(client_id, orders_list,
                                           additional_params={'InvoiceDesireDT': NOW})

    invoice_id, invoice_eid, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id,
                                                           credit=0, overdraft=1, contract_id=None)

    steps.InvoiceSteps.pay_fair(invoice_id, payment_sum=3050, orig_id=None, operation_type='INSERT',
                                source_id=123, inn='7801875896', bik='044525440',
                                account='40702810135463172116', customer_name=person_data['name'],
                                cash_receipt_number=332)
    cpf_id = db.get_oebs_cash_payment_fact_by_receipt_number(invoice_eid)[0]['xxar_cash_fact_id']
    steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id, {'Bucks': 100}, 0, NOW)
    act_id = steps.ActsSteps.generate(client_id, force=1, date=NOW)[0]
    steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id, {'Bucks': 10}, 0, NOW)
    refund_id = steps.RefundSteps.create_refund(cpf_id=cpf_id, amount=50)


def test_real_refund():
    print  steps.RefundSteps.create_refund(cpf_id=819810180, amount=100)
    # steps.CommonSteps.export('PROCESS_PAYMENTS', 'Invoice', 92052608)
    # print steps.CommonSteps.export('OEBS_API', 'InvoiceRefund', 1748032393)
    # steps.RefundSteps.check_invoice_refund_status(1748032393)


@pytest.mark.parametrize('context', [DIRECT_YANDEX_FIRM_FISH_PH.new(paysys=Paysyses.YM_PH_RUB),
                                     # DIRECT_YANDEX_FIRM_FISH_UR
                                     ])
def test_ym_payment(context):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    person_data = db.get_person_by_id(person_id)[0]

    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id,
                            product_id=context.product.id, params={'AgencyID': None})
    orders_list = [
        {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': 100,
         'BeginDT': NOW}]
    request_id = steps.RequestSteps.create(client_id, orders_list,
                                           additional_params={'InvoiceDesireDT': NOW})

    invoice_id, invoice_eid, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id,
                                                           credit=0, overdraft=0, contract_id=None)

    payment_id = steps.PaymentSteps.create(invoice_id, paysys_code='YAMONEY', payment_method_id=1201, receipt_sum=0,
                                           receipt_sum_1c=3000)
    steps.PaymentSteps.create_paycash(payment_id, wallet=123, ym_invoice_id=2342)
    # steps.PaymentSteps.create_paycash(payment_id, wallet=None, ym_invoice_id=None)
    steps.InvoiceSteps.pay_fair(invoice_id, payment_sum=1000, orig_id=payment_id, operation_type='ACTIVITY',
                                source_id=21123, inn='7801875896', bik='044525440',
                                account='40702810135463172116', customer_name=person_data['name'],
                                cash_receipt_number=12321)
    cpf_id = db.get_oebs_cash_payment_fact_by_receipt_number(invoice_eid)[0]['xxar_cash_fact_id']
    refund_id = steps.RefundSteps.create_refund(cpf_id=cpf_id, amount=100)
    # steps.CommonSteps.export('OEBS_API', 'InvoiceRefund', refund_id)
    steps.RefundSteps.check_invoice_refund_status(refund_id)


@pytest.mark.parametrize('status', ['export_failed', 'failed'])
@pytest.mark.parametrize('context', [DIRECT_YANDEX_FIRM_FISH_PH])
def test_bank_payment_failed_refund(context, status):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    person_data = db.get_person_by_id(person_id)[0]

    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id,
                            product_id=context.product.id, params={'AgencyID': None})
    orders_list = [
        {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': 100,
         'BeginDT': NOW}]
    request_id = steps.RequestSteps.create(client_id, orders_list,
                                           additional_params={'InvoiceDesireDT': NOW})

    invoice_id, invoice_eid, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id,
                                                           credit=0, overdraft=0, contract_id=None)

    steps.InvoiceSteps.pay_fair(invoice_id, payment_sum=3050, orig_id=None, operation_type='INSERT',
                                source_id=123, inn='7801875896', bik='044525440',
                                account='40702810135463172116', customer_name=person_data['name'],
                                cash_receipt_number=332)
    cpf_id = db.get_oebs_cash_payment_fact_by_receipt_number(invoice_eid)[0]['xxar_cash_fact_id']
    refund_id = steps.RefundSteps.create_refund(cpf_id=cpf_id, amount=100)
    steps.RefundSteps.set_properties(refund_id=refund_id, status=status)
    refund_id = steps.RefundSteps.create_refund(cpf_id=cpf_id, amount=100)
    refund_id = steps.RefundSteps.create_refund(cpf_id=cpf_id, amount=100)
    refund_id = steps.RefundSteps.create_refund(cpf_id=cpf_id, amount=100)


@pytest.mark.parametrize('context', [DIRECT_YANDEX_FIRM_FISH_PH])
def test_bank_payment_all_statuses(context):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)
    person_data = db.get_person_by_id(person_id)[0]

    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id,
                            product_id=context.product.id, params={'AgencyID': None})
    orders_list = [
        {'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': 100,
         'BeginDT': NOW}]
    request_id = steps.RequestSteps.create(client_id, orders_list,
                                           additional_params={'InvoiceDesireDT': NOW})

    invoice_id, invoice_eid, _ = steps.InvoiceSteps.create(request_id, person_id, context.paysys.id,
                                                           credit=0, overdraft=0, contract_id=None)

    steps.InvoiceSteps.pay_fair(invoice_id, payment_sum=3050, orig_id=None, operation_type='INSERT',
                                source_id=123, inn='7801875896', bik='044525440',
                                account='40702810135463172116', customer_name=person_data['name'],
                                cash_receipt_number=332)
    cpf_id = db.get_oebs_cash_payment_fact_by_receipt_number(invoice_eid)[0]['xxar_cash_fact_id']
    for status in ['not_exported', 'exported', 'export_failed', 'oebs_transmitted', 'oebs_reconciled',
                   'failed', 'successful', 'failed_unlocked', 'successful_reconciled']:
        refund_id = steps.RefundSteps.create_refund(cpf_id=cpf_id, amount=100)
        steps.RefundSteps.set_properties(refund_id=refund_id, status=status, descr=u'Ошибв', system_id=2,
                                         payload=json.dumps({"payment_num": 2323}))
