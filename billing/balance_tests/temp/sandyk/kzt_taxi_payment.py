# -*- coding: utf-8 -*-
__author__ = 'sandyk'

import datetime

import pytest

import balance.balance_api as api
import balance.balance_db as db
import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features
from btestlib.data import defaults, simpleapi_defaults

ORDER_SUM = 100
ORDER_TEXT = str(datetime.datetime.today().strftime("%d%m%Y%H%M%S"))
PAYMENT_TYPE = 'card'

TOKEN = simpleapi_defaults.TAXI_SERVICE_TOKEN

CONTRACT_START_DT = datetime.datetime.now() - datetime.timedelta(days=32)
TAXI_ORDER_DT = datetime.datetime.fromordinal(datetime.datetime.today().toordinal()).replace(
    day=1) - datetime.timedelta(days=5)


def taxi_method():
    # client_id, service_product_id = steps.SimpleApi.create_taxi_partner_and_product()

    partner = steps.CommonSteps.log(api.simple().server.BalanceSimple.CreatePartner)(
        simpleapi_defaults.TAXI_SERVICE_TOKEN,
        {'name': simpleapi_defaults.PARTNER_NAME,
         'operator_uid': defaults.PASSPORT_UID})
    if partner['status'] <> 'success':
        raise Exception(
            "CreatePartner: failed")

    partner_id = partner['partner_id']
    print 'partner_id: %s' % partner_id
    service_product_id = 'Taxi_test_' + str(partner_id)
    print 'service_product_id: %s' % service_product_id
    product = steps.CommonSteps.log(api.simple().server.BalanceSimple.CreateServiceProduct)(
        simpleapi_defaults.TAXI_SERVICE_TOKEN,
        {'prices': [{
            'price': 1,
            'currency': 'KZT',
            'dt': 1347521693,
            'region_id': 159
        }],
            'service_product_id': service_product_id,
            'name': simpleapi_defaults.PRODUCT_NAME,
            'partner_id': partner_id})

    if product['status'] <> 'success':
        raise Exception(
            "CreateServiceProduct: failed")

    person_id = steps.PersonSteps.create(partner_id, 'eu_yt', {'region': '159'})
    steps.ContractSteps.create_contract('taxi_postpay',
                                        {'CLIENT_ID': partner_id, 'PERSON_ID': person_id, 'DT': CONTRACT_START_DT,
                                         'FIRM': 22, 'COUNTRY': 159, 'PARTNER_COMMISSION_PCT2': 3})
    db.balance().execute("begin dbms_mview.refresh('BO.MV_PARTNER_TAXI_CONTRACT','C'); end;")

    steps.CommonSteps.log(steps.TaxiSteps.create_order)(partner_id, TAXI_ORDER_DT, PAYMENT_TYPE, ORDER_SUM, ORDER_TEXT)
    service_order_id, trust_payment_id, \
    purchase_token, payment_id = steps.SimpleApi.create_trust_payment(TOKEN,
                                                                      service_product_id,
                                                                      service_order_id=ORDER_TEXT,
                                                                      is_register_needed=1)
    # trust_payment_id, purchase_token = steps.SimpleApi.create_taxi_payment(ORDER_TEXT, service_product_id)
    # payment_id = steps.SimpleApi.create_register([trust_payment_id], 0)
    print payment_id
    return payment_id


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
