__author__ = 'aikawa'
import datetime

import hamcrest

from balance import balance_db as db
from balance import balance_steps as steps
from btestlib import utils as utils
from btestlib import reporter
from balance.features import Features
import btestlib.config as balance_config

pytestmark = [
    reporter.feature(Features.OEBS)
]

dt = datetime.datetime.now() - datetime.timedelta(days=1)

SERVICE_ID = 7
PRODUCT_ID = 1475
PAYSYS_ID = 1003


@reporter.feature(Features.TO_UNIT)
def test_act_amount_is_amount_nds():
    client_id = steps.ClientSteps.create()

    person_id = steps.PersonSteps.create(client_id, 'ur')

    order_ids_list = []

    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id=client_id, service_order_id=service_order_id, product_id=PRODUCT_ID,
                                       service_id=SERVICE_ID)
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 100, 'BeginDT': dt}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                           additional_params=dict(InvoiceDesireDT=dt))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id, payment_dt=dt)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 50}, 0, dt)
    act_id = steps.ActsSteps.generate(client_id, force=1, date=dt)[0]
    db.balance().execute('UPDATE t_act_internal SET amount_nds = 249.99 WHERE id = :act_id', {'act_id': act_id})
    db.balance().execute('UPDATE t_act_trans SET amount_nds = 249.99 WHERE act_id = :act_id', {'act_id': act_id})
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 50.0004}, 0, dt)
    act_id = steps.ActsSteps.generate(client_id, force=1, date=dt)[0]
    act = db.get_act_by_id(act_id)[0]
    external_id = act['external_id']
    type = act['type']
    if balance_config.ENABLE_SINGLE_ACCOUNT:
        invoice_id = db.get_invoice_by_charge_note_id(invoice_id)[0]['id']
    steps.ExportSteps.export_oebs(client_id=client_id,
                                  invoice_id=invoice_id,
                                  person_id=person_id,
                                  act_id=act_id)
    steps.ExportSteps.export_oebs(act_id=act_id)
    act_oebs = db.oebs().execute('''
    SELECT *
    FROM apps.ra_customer_trx_all trx
    WHERE trx_number = :external_id''', {'external_id': external_id})
    utils.check_that(act_oebs, hamcrest.equal_to([]))
    utils.check_that(type, hamcrest.equal_to('internal'))
