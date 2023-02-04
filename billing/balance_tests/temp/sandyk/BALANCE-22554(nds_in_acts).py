__author__ = 'sandyk'

import datetime
import decimal
import pprint

import balance.balance_db as db
from btestlib import balance_steps as steps
from btestlib import utils

SERVICE_ID = 7
LOGIN = 'clientuid34'
PRODUCT_ID = 1475
PAYSYS_ID = 1001
PERSON_TYPE = 'ph'
# 'pu'
QUANT = 0.8
ORDER_DT = datetime.datetime(2015, 11, 25)


def test_nds():
    agency_id =  steps.ClientSteps.create({'IS_AGENCY': 1})

    person_id = steps.PersonSteps.create(agency_id, PERSON_TYPE)
    data = []

    for x in range(0, 15):
        client_id = steps.ClientSteps.create({'IS_AGENCY': 0})
        service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
        order_id = steps.OrderSteps.create(client_id, service_order_id, PRODUCT_ID, SERVICE_ID,
                                           {'TEXT': 'Py_Test order', 'AgencyID': agency_id, 'ManagerUID': None})
        orders_list = {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QUANT, 'BeginDT': datetime.datetime(2015, 11, 25)}
        data.append({'client_id': client_id, 'service_order_id': service_order_id, 'order_id': order_id,
                     'orders_list': orders_list})
    pprint.pprint(data)
    orders_list = []
    for s in data:
        orders_list.append(s['orders_list'])
    pprint.pprint(orders_list)

    request_id = steps.RequestSteps.create(agency_id, orders_list, additional_params={'InvoiceDesireDT': datetime.datetime(2015, 11, 26)})
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=0, contract_id=None, overdraft=0, endbuyer_id=None)

    db.balance().execute('update (select * from t_client where id  =:agency_id) set is_docs_separated = 1',
                         {'agency_id': agency_id})

    dates = {}
    BEFORE_6_MONTHS_VALUE = utils.Date.shift_date(datetime.datetime.now(), days=-186)
    # if datetime.datetime.now().month - 6 <= 0:
    #     BEFORE_6_MONTHS_VALUE = BEFORE_6_MONTHS_VALUE.replace(year=datetime.datetime.now().year-1,month=datetime.datetime.now().month+12-6)
    # else:
    #     BEFORE_6_MONTHS_VALUE = datetime.datetime.now().replace(month=datetime.datetime.now().month - 7)  ## 3 months

    dates[1] = utils.Date.shift_date(BEFORE_6_MONTHS_VALUE, days=32)
    dates[2] = utils.Date.shift_date(dates[1], days=32)
    dates[3] = utils.Date.shift_date(dates[2], days=32)
    dates[4] = utils.Date.shift_date(dates[3], days=32)

    pprint.pprint(dates)
    bucks = decimal.Decimal(0.2).quantize(decimal.Decimal('.01'))
    steps.InvoiceSteps.pay(invoice_id, None, None)
    for i in dates:
        for s in data:
            steps.CampaignsSteps.do_campaigns(SERVICE_ID, s['service_order_id'],{'Bucks': bucks, 'Days': 0, 'Money': 0}, 0, dates[i])
        steps.ActsSteps.generate(agency_id,1, dates[i])
        bucks +=decimal.Decimal(0.2).quantize(decimal.Decimal('.01'))

    # steps.CampaignsSteps.do_campaigns(SERVICE_ID, 45816416,{'Bucks': 0.801, 'Days': 0, 'Money': 0}, 0, datetime.datetime(2016,3,2))
    # steps.ActsSteps.generate(9774956,1, datetime.datetime(2016,3,30))
    # data = [{'service_order_id': 45816431},
    #         {'service_order_id': 45816432},
    #         {'service_order_id': 45816433},
    #         {'service_order_id': 45816434},
    #         {'service_order_id': 45816435},
    #         {'service_order_id': 45816436},
    #         {'service_order_id': 45816437},
    #         {'service_order_id': 45816438},
    #         {'service_order_id': 45816439},
    #         {'service_order_id': 45816440},
    #         {'service_order_id': 45816441},
    #         {'service_order_id': 45816442},
    #         {'service_order_id': 45816443},
    #         {'service_order_id': 45816444},
    #         {'service_order_id': 45816445}]
    #
    # for s in data:
    #     steps.CampaignsSteps.do_campaigns(SERVICE_ID, s['service_order_id'], {'Bucks': 1, 'Days': 0, 'Money': 0}, 0,
    #                                       datetime.datetime(2016, 3, 29))
    # steps.ActsSteps.generate(9775020, 1, datetime.datetime(2016, 3, 29))


#     acts = [48025178,
# 48025192,
# 48025191,
# 48025190,
# 48025189,
# 48025188,
# 48025187,
# 48025186,
# 48025185,
# 48025184,
# 48025183,
# 48025182,
# 48025181,
# 48025180,
# 48025179]
#
#     for a in acts:
#         steps.ActsSteps.hide(a)

# steps.ActsSteps.generate(9774251,1, datetime.datetime(2016, 3, 27))

if __name__ == "__main__":
    # test_simple_client()
    test_nds()
