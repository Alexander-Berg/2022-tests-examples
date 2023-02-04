# -*- coding: utf-8 -*-
__author__ = 'sandyk'

import datetime

import pytest

import balance.balance_api as api
import balance.balance_db as db
import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features

DT = datetime.datetime.now() - datetime.timedelta(days=1)
PERSON_TYPE = 'ph'
PAYSYS_ID = 1001
SERVICE_ID = 7
NON_CURRENCY_PRODUCT_ID = 1475
OVERDRAFT_LIMIT = 120
MAIN_DT = datetime.datetime.now()
QTY = 3600.03


@pytest.mark.slow
@reporter.feature(Features.OVERDRAFT)
@pytest.mark.tickets('BALANCE-22004')
def test_fair_overdraft_mv_client():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    ##даем честный овердрафт
    steps.ClientSteps.set_overdraft(client_id, SERVICE_ID, OVERDRAFT_LIMIT, firm_id=1, start_dt=MAIN_DT,
                                                currency=None, invoice_currency=None)
    db.balance().execute(
        'update (select * from t_export where OBJECT_ID = :client_id and type = \'OVERDRAFT\') set priority=-1',
        {'client_id': client_id})
    steps.CommonSteps.wait_for(
                'select state as val from t_export where OBJECT_ID = :client_id and type = \'OVERDRAFT\' and classname = \'Client\'',
                {'client_id': client_id}, 1, interval=2)

    service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id, service_order_id, NON_CURRENCY_PRODUCT_ID, SERVICE_ID)
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QTY, 'BeginDT': DT}
    ]

    ##переводим на мультивалютность
    steps.ClientSteps.migrate_to_currency(client_id=client_id, currency_convert_type='MODIFY', dt=DT)

    ##пересчитываем овердрафт
    api.test_balance().CalculateOverdraft([client_id])
    db.balance().execute(
        'update (select * from t_export where OBJECT_ID = :client_id and type = \'OVERDRAFT\') set priority=-1',
        {'client_id': client_id})
    steps.CommonSteps.wait_for(
                'select state as val from t_export where OBJECT_ID = :client_id and type = \'OVERDRAFT\' and classname = \'Client\'',
                {'client_id': client_id}, 1, interval=2)

    limit = db.balance().execute('select overdraft_limit from T_CLIENT_OVERDRAFT  where client_id = :client_id',
                                 {'client_id': client_id})[0]['overdraft_limit']

    assert str(limit) == str(OVERDRAFT_LIMIT*30), 'Please check overdraft for client %s'%client_id

    ##выставляем счет
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list, additional_params=dict(InvoiceDesireDT=DT))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=1, endbuyer_id=None)
    result = db.balance().execute('select total_sum, overdraft from t_invoice where id = :invoice_id',
                                  {'invoice_id': invoice_id})
    assert (result[0]['total_sum'] == str(QTY)) and (result[0]['overdraft'] == 1), 'Check invoice %s'%invoice_id


if __name__ == "__main__":
    test_fair_overdraft_mv_client()