# -*- coding: utf-8 -*-
__author__ = 'sandyk'

import datetime

import pytest

import balance.balance_db as db
import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features
from btestlib.data import simpleapi_defaults

TOKEN = simpleapi_defaults.TAXI_SERVICE_TOKEN

ORDER_SUM = 100
ORDER_TEXT = str(datetime.datetime.today().strftime("%d%m%Y%H%M%S"))
PAYMENT_TYPE = 'card'

TOKEN = simpleapi_defaults.TAXI_SERVICE_TOKEN

CONTRACT_START_DT = datetime.datetime.now() - datetime.timedelta(days=31)
TAXI_ORDER_DT = datetime.datetime.fromordinal(datetime.datetime.today().toordinal()).replace(
    day=1) - datetime.timedelta(days=5)



# def taxi_method(test):
#     client_id, service_product_id = steps.SimpleApi.create_taxi_partner_and_product()
#     person_id = steps.PersonSteps.create(client_id, 'ur', {'kpp': '234567890'})
#     steps.ContractSteps.create('taxi_postpay',
#                                {'client_id': client_id, 'person_id': person_id, 'dt': CONTRACT_START_DT,'FIRM':13})
#     steps.CommonSteps.log(steps.sql)("begin dbms_mview.refresh('BO.MV_PARTNER_TAXI_CONTRACT','C'); end;")
#
#     # steps.CommonSteps.log(steps.TaxiSteps.create_order)(client_id, TAXI_ORDER_DT, PAYMENT_TYPE, ORDER_SUM, ORDER_TEXT)
#     trust_payment_id, purchase_token = steps.SimpleApi.create_taxi_payment(ORDER_TEXT, service_product_id)
#     payment_id = steps.SimpleApi.create_register([trust_payment_id], 0)
#     print payment_id
#     steps.CommonSteps.restart_pycron_task('process_thirdparty_transactions')
#     steps.CommonSteps.wait_for(
#         'select state as val from t_export where type = \'THIRDPARTY_TRANS\' and object_id = :payment_id',
#         {'payment_id': payment_id}, value=1)
#
#     return payment_id


@pytest.mark.slow
@reporter.feature(Features.TRUST, Features.PAYMENT)
@pytest.mark.tickets('BALANCE-21583')
@pytest.mark.parametrize('test_type', [
    'no_order',
    'no_contract',
    'no_register'])
def test_3rdparty_exceptions(test_type):
    print '!!!!'
    print  test_type
    # client_id, service_product_id = steps.SimpleApi.create_taxi_partner_and_product()
    client_id, service_product_id, _ = steps.SimpleApi.create_partner_and_product(TOKEN)
    person_id = steps.PersonSteps.create(client_id, 'ur', {'kpp': '234567890'})
    if test_type <> 'no_contract':
        steps.ContractSteps.create_contract('taxi_postpay',
                                            {'CLIENT_ID': client_id, 'PERSON_ID': person_id, 'DT': CONTRACT_START_DT,
                                             'FIRM': 13})
    db.balance().execute("begin dbms_mview.refresh('BO.MV_PARTNER_TAXI_CONTRACT','C'); end;")

    if test_type <> 'no_order':
        steps.CommonSteps.log(steps.TaxiSteps.create_order)(client_id, TAXI_ORDER_DT, PAYMENT_TYPE, ORDER_SUM, ORDER_TEXT)
    # trust_payment_id, purchase_token, payment_id = steps.SimpleApi.create_taxi_payment(ORDER_TEXT, service_product_id)
    service_order_id, trust_payment_id, \
    purchase_token, payment_id = steps.SimpleApi.create_trust_payment(TOKEN,
                                                                      service_product_id,
                                                                      service_order_id=ORDER_TEXT,
                                                                      is_register_needed=0)
    if test_type <> 'no_register':
        steps.SimpleApi.create_register([trust_payment_id], 0)

    steps.CommonSteps.restart_pycron_task('process_thirdparty_transactions')
    steps.CommonSteps.wait_for(
        'select state as val from t_export where type = \'THIRDPARTY_TRANS\' and object_id = :payment_id',
        {'payment_id': payment_id}, value=1)
    db.balance().execute(
        'update (select * from t_export where object_id = :payment_id and type = \'THIRDPARTY_TRANS\') set priority=-1',
        {'payment_id': payment_id})
    print 'export is in progress'

    if (test_type == 'no_order') or (test_type == 'no_contract'):

        steps.CommonSteps.wait_for( 'select state as val from t_export where object_id = :payment_id and type = \'THIRDPARTY_TRANS\''
                                    ,{'payment_id': payment_id}, value=2)
        result = \
            db.balance().execute(
                'select error from t_export where object_id = :payment_id and type = \'THIRDPARTY_TRANS\'',
                {'payment_id': payment_id})[0]['error']
        assert (len(result) <> 0), 'Check the payment %s'%payment_id

    if (test_type == 'no_register'):
        steps.CommonSteps.wait_for( 'select state as val from t_export where object_id = :payment_id and type = \'THIRDPARTY_TRANS\''
                                    ,{'payment_id': payment_id}, value=1)
        result = db.balance().execute(
            'select output from t_export where object_id = :payment_id and type = \'THIRDPARTY_TRANS\'',
            {'payment_id': payment_id})[0]['error']
        assert (len(result) <> 0), 'Check the payment %s' % payment_id


if __name__ == "__main__":
    pytest.main("-s -v thirdparty_exceptions.py")