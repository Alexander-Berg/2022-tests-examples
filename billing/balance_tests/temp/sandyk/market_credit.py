__author__ = 'sandyk'

import datetime

from balance import balance_steps as steps
# dt = datetime.datetime.now()

dt = datetime.datetime(2016, 5, 20)

SERVICE_ID = 11
LOGIN = 'clientuid34'
PRODUCT_ID= 2136
PAYSYS_ID = 11101001
PERSON_TYPE = 'ph'
QUANT = 1000

def test_overdraft_notification():

        # client_id = steps.ClientSteps.create({'IS_AGENCY': 1})
        # person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
        # contract_id,_ =  steps.ContractSteps.create('opt_agency_prem_post',{'client_id': client_id, 'person_id': person_id,
        #                                            'dt'       : '2016-05-01T00:00:00',
        #                                            # 'FINISH_DT': None,
        #                                            'is_signed': '2016-05-01T00:00:00'
        #                                            # 'is_signed': None,
        #                                            ,'SERVICES': [11]
        #                                            ,'FIRM': 111
        #                                            # ,'SCALE':3
        #                                            })
        # service_order_id = steps.OrderSteps.next_id(SERVICE_ID)
        # order_id = steps.OrderSteps.create (client_id, service_order_id, PRODUCT_ID, SERVICE_ID,
        #     {'TEXT':'Py_Test order','AgencyID' : None, 'ManagerUID': None})
        #
        # orders_list = [
        #     {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': QUANT, 'BeginDT': dt}
        # ]
        # request_id = steps.RequestSteps.create(client_id, orders_list, additional_params={'InvoiceDesireDT': dt})
        # invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
        #                                              credit=1, contract_id=contract_id, overdraft=0, endbuyer_id=None)
        #
        # steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 10, 'Money': 0}, 0, datetime.datetime(2016, 5, 20))
        # steps.ActsSteps.create(invoice_id, datetime.datetime(2016, 5, 20))
        # steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 100, 'Money': 0}, 0, datetime.datetime(2016, 6, 20))
        # steps.ActsSteps.create(invoice_id, datetime.datetime(2016, 6, 20))
        # steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 300, 'Money': 0}, 0, datetime.datetime(2016, 7, 20))
        # steps.ActsSteps.create(invoice_id, datetime.datetime(2016, 7, 20))
        # steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 500, 'Money': 0}, 0, datetime.datetime(2016, 8, 20))
        # steps.ActsSteps.create(51919641, datetime.datetime(2016, 8, 30))


        steps.CloseMonth.UpdateLimits(datetime.datetime(2016, 8, 30), 0, [2124001])


        # steps.ActsSteps.hide(52001537)




if __name__ == "__main__":
    test_overdraft_notification()