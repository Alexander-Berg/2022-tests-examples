# -*- coding: utf-8 -*-

import datetime
from hamcrest import equal_to, is_
import pytest

from balance import balance_steps as steps
from balance import balance_db as db
from balance import balance_api as api
from btestlib.constants import Services, Products, Paysyses, Export, Firms, Users
import btestlib.utils as utils
import btestlib.reporter as reporter

now = datetime.datetime.now()

firm_id = Firms.YANDEX_1.id
service_id = Services.DIRECT.id
product_id = Products.DIRECT_FISH.id
paysys_bank_id = Paysyses.BANK_UR_RUB.id
paysys_card_id = Paysyses.CC_UR_RUB.id


def create_src_invoice(client_id, person_id):
    with reporter.step(u'Создаем счет, с которого переносим средства, оплачиваем и наполовину откручиваем:'):
        src_service_order_id = steps.OrderSteps.next_id(service_id=service_id)
        steps.OrderSteps.create(
            client_id, src_service_order_id, service_id=service_id, product_id=product_id, params={'AgencyID': None}
        )
        src_orders_list = [
            {'ServiceID': service_id, 'ServiceOrderID': src_service_order_id, 'Qty': 100, 'BeginDT': now}
        ]
        src_request_id = steps.RequestSteps.create(
            client_id, src_orders_list, additional_params=dict(InvoiceDesireDT=now)
        )
        src_invoice_id, src_invoice_eid, _ = steps.InvoiceSteps.create(src_request_id, person_id, paysys_bank_id)
        for amount in (600, 2400):
            steps.InvoiceSteps.create_cash_payment_fact(
                invoice_eid=src_invoice_eid,
                amount=amount,
                dt=datetime.datetime.now(),
                type='ONLINE',
                invoice_id=src_invoice_id
            )
        steps.CampaignsSteps.do_campaigns(service_id, src_service_order_id, {'Bucks': 50}, 0, now)

    utils.check_that(
        int(api.test_balance().GetInvoiceTransferAvailableSum(Users.YB_ADM.uid, src_invoice_id)),
        equal_to(1500),
        step=u'Проверяем сумму, доступную для переноса'
    )

    return src_invoice_id, src_invoice_eid


def create_invoice_transfer(src_invoice_id, src_invoice_eid, dst_invoice_id, dst_invoice_eid):
    with reporter.step(u"Создаем перенос:"):
        invoice_transfer_id = api.test_balance().CreateInvoiceTransfer(
            Users.YB_ADM.uid, src_invoice_id, dst_invoice_id, 900, False
        )
        db.balance().execute(
            "update bo.t_invoice_transfer set status_code = 'exported' where id={}".format(invoice_transfer_id)
        )
        for amount in (600, 300):
            steps.InvoiceSteps.create_cash_payment_fact(
                invoice_eid=src_invoice_eid,
                amount=-amount,
                dt=datetime.datetime.now(),
                type='ACTIVITY',
                orig_id=invoice_transfer_id
            )
            steps.InvoiceSteps.create_cash_payment_fact(
                invoice_eid=dst_invoice_eid,
                amount=amount,
                dt=datetime.datetime.now(),
                type='ACTIVITY',
                orig_id=invoice_transfer_id
            )
        steps.CommonSteps.export(Export.Type.PROCESS_PAYMENTS, Export.Classname.INVOICE, src_invoice_id)
        steps.CommonSteps.export(Export.Type.PROCESS_PAYMENTS, Export.Classname.INVOICE, dst_invoice_id)


def test_invoice_transfer_to_cc_invoice():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')

    src_invoice_id, src_invoice_eid = create_src_invoice(client_id, person_id)

    with reporter.step(u'Создаем счет, на который переносим средства:'):
        dst_service_order_id = steps.OrderSteps.next_id(service_id=service_id)
        steps.OrderSteps.create(
            client_id, dst_service_order_id, service_id=service_id, product_id=product_id, params={'AgencyID': None}
        )
        dst_orders_list = [
            {'ServiceID': service_id, 'ServiceOrderID': dst_service_order_id, 'Qty': 30, 'BeginDT': now}
        ]
        dst_request_id = steps.RequestSteps.create(
            client_id, dst_orders_list, additional_params=dict(InvoiceDesireDT=now)
        )
        dst_invoice_id, dst_invoice_eid, _ = steps.InvoiceSteps.create(dst_request_id, person_id, paysys_card_id)
        dst_invoice = db.get_invoice_by_id(dst_invoice_id)[0]
        utils.check_that(
            dst_invoice['turn_on_dt'],
            is_(None),
            step=u'Проверяем, что счет не включен'
        )

    create_invoice_transfer(src_invoice_id, src_invoice_eid, dst_invoice_id, dst_invoice_eid)

    utils.check_that(
        int(api.test_balance().GetInvoiceTransferAvailableSum(Users.YB_ADM.uid, src_invoice_id)),
        equal_to(600),
        step=u'Проверяем новую сумму, доступную для переноса'
    )

    src_invoice = db.get_invoice_by_id(src_invoice_id)[0]
    utils.check_that(src_invoice['receipt_sum_1c'], equal_to(2100))
    utils.check_that(src_invoice['receipt_sum'], equal_to(2100))
    utils.check_that(src_invoice['consume_sum'], equal_to(2100))

    dst_invoice = db.get_invoice_by_id(dst_invoice_id)[0]
    utils.check_that(dst_invoice['receipt_sum_1c'], equal_to(900))
    utils.check_that(dst_invoice['receipt_sum'], equal_to(900))
    utils.check_that(
        dst_invoice['consume_sum'],
        equal_to(900),
        step=u'Проверяем, что средства разложились на козюмы после переноса на полную сумму счета'
    )
    utils.check_that(
        dst_invoice['turn_on_dt'].date(),
        equal_to(now.date()),
        step=u'Проверяем, что счет включен'
    )


def test_invoice_transfer_to_invoice_with_overdraft_debt():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')

    src_invoice_id, src_invoice_eid = create_src_invoice(client_id, person_id)

    with reporter.step(u'Создаем счет, на который переносим средства:'):
        dst_service_order_id = steps.OrderSteps.next_id(service_id=service_id)
        steps.OrderSteps.create(
            client_id, dst_service_order_id, service_id=service_id, product_id=product_id, params={'AgencyID': None}
        )
        dst_orders_list = [
            {'ServiceID': service_id, 'ServiceOrderID': dst_service_order_id, 'Qty': 30, 'BeginDT': now}
        ]
        dst_request_id = steps.RequestSteps.create(
            client_id, dst_orders_list, additional_params=dict(InvoiceDesireDT=now)
        )
        steps.OverdraftSteps.set_force_overdraft(
            client_id=client_id, service_id=service_id, limit=100, firm_id=firm_id
        )
        dst_invoice_id, dst_invoice_eid, _ = steps.InvoiceSteps.create(
            dst_request_id, person_id, paysys_card_id, overdraft=1
        )
        steps.CampaignsSteps.do_campaigns(service_id, dst_service_order_id, {'Bucks': 15}, 0, now)
        act_id = steps.ActsSteps.generate(client_id, force=1, date=now)[1]
        steps.OverdraftSteps.expire_overdraft_invoice(dst_invoice_id, delta=90)
        act = db.get_act_by_id(act_id)[0]
        utils.check_that(
            int(act['amount']) - int(act['paid_amount']),
            equal_to(450),
            step=u'Проверяем, что есть долг по овердрафтному счету'
        )

    create_invoice_transfer(src_invoice_id, src_invoice_eid, dst_invoice_id, dst_invoice_eid)

    utils.check_that(
        int(api.test_balance().GetInvoiceTransferAvailableSum(Users.YB_ADM.uid, src_invoice_id)),
        equal_to(600),
        step=u'Проверяем новую сумму, доступную для переноса'
    )

    src_invoice = db.get_invoice_by_id(src_invoice_id)[0]
    utils.check_that(src_invoice['receipt_sum_1c'], equal_to(2100))
    utils.check_that(src_invoice['receipt_sum'], equal_to(2100))
    utils.check_that(src_invoice['consume_sum'], equal_to(2100))

    dst_invoice = db.get_invoice_by_id(dst_invoice_id)[0]
    utils.check_that(dst_invoice['receipt_sum_1c'], equal_to(900))
    utils.check_that(dst_invoice['receipt_sum'], equal_to(900))

    act = db.get_act_by_id(act_id)[0]
    utils.check_that(
        int(act['amount']) - int(act['paid_amount']),
        equal_to(0),
        step=u'Проверяем, что нет долга по овердрафтному счету'
    )
