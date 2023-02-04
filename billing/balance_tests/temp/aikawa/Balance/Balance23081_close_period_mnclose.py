import datetime

from balance import balance_steps as steps
from balance import balance_api as api
dt = datetime.datetime.now()
last_month = dt.replace(day=1)-datetime.timedelta(days=10)

PERSON_TYPE = 'ph'
PAYSYS_ID = 1001
SERVICE_ID = 35
PRODUCT_ID = 1475
MSR = 'Bucks'

# print steps.CloseMonth.resolve_mnclose_status('balance_close_stage2')
# print steps.CloseMonth.resolve_mnclose_status('defreeze_confirmed')
# steps.CloseMonth.resolve_task('balance_close_stage2')
# steps.CloseMonth.resolve_task('manual_acts_changes')
#
# api.test_balance().ExecuteSQL('mnclose',
#                               '''
#                               delete from (
#                               select * from mnclose.t_tasks t
#                               join mnclose.t_itasks it on t.id = it.task_id
#                               join mnclose.t_itask_states_changes itsc on it.id = itsc.itask_id
#                               join mnclose.t_itask_states its on itsc.state_id = its.id
#                               where
#                               t.name_id like :task_name
#                               and it.inst_dt >= TRUNC(:dt, 'MONTH')
#                               and itsc.state_id > 0)
#                               ''', {'task_name': 'defreeze_confirmed', 'dt': dt})

client_id = steps.ClientSteps.create()
person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)

service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
order_id = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
                                   service_order_id=service_order_id)
orders_list = [
    {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 200, 'BeginDT': last_month}
]
request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list,
                                       additional_params=dict(InvoiceDesireDT=last_month, FirmID=1))


invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                             credit=0, contract_id=None, overdraft=0, endbuyer_id=None)
steps.InvoiceSteps.pay(invoice_id)


steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 100}, 0, last_month)

