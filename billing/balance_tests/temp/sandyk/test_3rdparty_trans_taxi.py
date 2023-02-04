# -*- coding: utf-8 -*-
__author__ = 'sandyk'

import datetime

import pytest

import balance.balance_db as db
import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features
from btestlib.data import simpleapi_defaults

ORDER_SUM = 100
ORDER_TEXT = str(datetime.datetime.today().strftime("%d%m%Y%H%M%S"))
PAYMENT_TYPE = 'card'

TOKEN = simpleapi_defaults.TAXI_SERVICE_TOKEN

CONTRACT_START_DT = datetime.datetime.now() - datetime.timedelta(days=32)
TAXI_ORDER_DT = datetime.datetime.fromordinal(datetime.datetime.today().toordinal()).replace(
    day=1) - datetime.timedelta(days=5)


def taxi_method():
    # client_id, service_product_id = steps.SimpleApi.create_taxi_partner_and_product()
    client_id, service_product_id, _ = steps.SimpleApi.create_partner_and_product(TOKEN)
    person_id = steps.PersonSteps.create(client_id, 'ur', {'kpp': '234567890'})
    steps.ContractSteps.create_contract('taxi_postpay',
                                        {'CLIENT_ID': client_id, 'PERSON_ID': person_id, 'DT': CONTRACT_START_DT,
                                         'FIRM': 13})
    db.balance().execute("begin dbms_mview.refresh('BO.MV_PARTNER_TAXI_CONTRACT','C'); end;")

    steps.CommonSteps.log(steps.TaxiSteps.create_order)(client_id, TAXI_ORDER_DT, PAYMENT_TYPE, ORDER_SUM, ORDER_TEXT)
    service_order_id, trust_payment_id, \
    purchase_token, payment_id = steps.SimpleApi.create_trust_payment(TOKEN,
                                                                      service_product_id,
                                                                      service_order_id=ORDER_TEXT,
                                                                      is_register_needed=1)
    # trust_payment_id, purchase_token = steps.SimpleApi.create_taxi_payment(ORDER_TEXT, service_product_id)
    # payment_id = steps.SimpleApi.create_register([trust_payment_id], 0)
    print payment_id
    return payment_id


@pytest.mark.smoke
@pytest.mark.slow
@reporter.feature(Features.TRUST, Features.PAYMENT)
@pytest.mark.tickets('BALANCE-21583')
def test_3rdparty_trans_taxi1():
    payment_id = taxi_method()
    steps.CommonPartnerSteps.export_payment(payment_id)
    print 'DONE! select * from t_thirdparty_transactions where payment_id  =%s' % payment_id
    result = \
        db.balance().execute('select count(*) as result from t_thirdparty_transactions where payment_id  =:payment_id',
                             {'payment_id': payment_id})[0]['result']
    assert result == 1, 'Check the payment %s' % payment_id


if __name__ == "__main__":
    test_3rdparty_trans_taxi1()
    # pytest.main("test_3rdparty_trans_taxi.py -v")
