# coding: utf-8
__author__ = 'atkaya'

import datetime
from datetime import timedelta

import btestlib.utils as utils
from balance import balance_steps as steps
from btestlib.constants import PlaceType
from btestlib.data.defaults import Distribution
import balance.balance_api as api


def create_prepayment_invoice():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')
    service_order_id = steps.OrderSteps.next_id(7)
    steps.OrderSteps.create(client_id, service_order_id, product_id=1475, service_id=7)
    # service_order_id1 = steps.OrderSteps.next_id(11)
    # steps.OrderSteps.create(client_id, service_order_id1, product_id=2136, service_id=11)
    # service_order_id2 = steps.OrderSteps.next_id(11)
    # steps.OrderSteps.create(client_id, service_order_id2, product_id=2136, service_id=11)
    # service_order_id3 = steps.OrderSteps.next_id(11)
    # steps.OrderSteps.create(client_id, service_order_id3, product_id=2136, service_id=11)
    # service_order_id4 = steps.OrderSteps.next_id(11)
    # steps.OrderSteps.create(client_id, service_order_id4, product_id=2136, service_id=11)
    # service_order_id5 = steps.OrderSteps.next_id(11)
    # steps.OrderSteps.create(client_id, service_order_id5, product_id=2136, service_id=11)
    # service_order_id6 = steps.OrderSteps.next_id(11)
    # steps.OrderSteps.create(client_id, service_order_id6, product_id=2136, service_id=11)
    # service_order_id7 = steps.OrderSteps.next_id(11)
    # steps.OrderSteps.create(client_id, service_order_id7, product_id=2136, service_id=11)
    # service_order_id8 = steps.OrderSteps.next_id(11)
    # steps.OrderSteps.create(client_id, service_order_id8, product_id=2136, service_id=11)
    # service_order_id9 = steps.OrderSteps.next_id(11)
    # steps.OrderSteps.create(client_id, service_order_id9, product_id=2136, service_id=11)
    # service_order_id10 = steps.OrderSteps.next_id(11)
    # steps.OrderSteps.create(client_id, service_order_id10, product_id=2136, service_id=11)
    orders_list = [{'ServiceID': 7, 'ServiceOrderID': service_order_id, 'Qty': 50},
    #                # {'ServiceID': 11, 'ServiceOrderID': service_order_id1, 'Qty': 30},
    #                # {'ServiceID': 11, 'ServiceOrderID': service_order_id2, 'Qty': 30},
    #                # {'ServiceID': 11, 'ServiceOrderID': service_order_id3, 'Qty': 30},
    #                # {'ServiceID': 11, 'ServiceOrderID': service_order_id4, 'Qty': 30},
    #                # {'ServiceID': 11, 'ServiceOrderID': service_order_id5, 'Qty': 30},
    #                # {'ServiceID': 11, 'ServiceOrderID': service_order_id6, 'Qty': 30},
    #                # {'ServiceID': 11, 'ServiceOrderID': service_order_id7, 'Qty': 30},
    #                # {'ServiceID': 11, 'ServiceOrderID': service_order_id8, 'Qty': 30},
    #                # {'ServiceID': 11, 'ServiceOrderID': service_order_id9, 'Qty': 30},
    #                # {'ServiceID': 11, 'ServiceOrderID': service_order_id10, 'Qty': 30},
                   ]

    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params={'FirmID': 1})
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, 1003, credit=0, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.turn_on(invoice_id)
    steps.CampaignsSteps.do_campaigns(7, service_order_id, {'Bucks': 20}, 0)
    # steps.InvoiceSteps.pay(invoice_id)
    steps.ActsSteps.generate(client_id, force=1)
    # steps.CampaignsSteps.do_campaigns(11, service_order_id, {'Bucks': 50}, 0)
    # steps.ActsSteps.generate(client_id, force=1)
    # steps.CampaignsSteps.do_campaigns(11, service_order_id1, {'Bucks': 30}, 0)
    # steps.ActsSteps.generate(client_id, force=1)
    # steps.CampaignsSteps.do_campaigns(11, service_order_id2, {'Bucks': 30}, 0)
    # steps.ActsSteps.generate(client_id, force=1)
    # steps.CampaignsSteps.do_campaigns(11, service_order_id3, {'Bucks': 30}, 0)
    # steps.ActsSteps.generate(client_id, force=1)
    # steps.CampaignsSteps.do_campaigns(11, service_order_id4, {'Bucks': 30}, 0)
    # steps.ActsSteps.generate(client_id, force=1)
    # steps.CampaignsSteps.do_campaigns(11, service_order_id5, {'Bucks': 30}, 0)
    # steps.ActsSteps.generate(client_id, force=1)
    # steps.CampaignsSteps.do_campaigns(11, service_order_id6, {'Bucks': 30}, 0)
    # steps.ActsSteps.generate(client_id, force=1)
    # steps.CampaignsSteps.do_campaigns(11, service_order_id7, {'Bucks': 30}, 0)
    # steps.ActsSteps.generate(client_id, force=1)
    # steps.CampaignsSteps.do_campaigns(11, service_order_id8, {'Bucks': 30}, 0)
    # steps.ActsSteps.generate(client_id, force=1)
    # steps.CampaignsSteps.do_campaigns(11, service_order_id9, {'Bucks': 30}, 0)
    # steps.ActsSteps.generate(client_id, force=1)
    # steps.CampaignsSteps.do_campaigns(11, service_order_id10, {'Bucks': 30}, 0)
    # steps.ActsSteps.generate(client_id, force=1)

def create_overdraft_invoice():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')
    steps.OverdraftSteps.set_force_overdraft(client_id, 11, 100000, firm_id=111)
    service_order_id = steps.OrderSteps.next_id(11)
    steps.OrderSteps.create(client_id, service_order_id, product_id=2136, service_id=11)
    orders_list = [{'ServiceID': 11, 'ServiceOrderID': service_order_id, 'Qty': 50},
                   ]

    request_id = steps.RequestSteps.create(client_id, orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, 1003, credit=0, overdraft=1, endbuyer_id=None)
    # steps.InvoiceSteps.turn_on(invoice_id)
    steps.CampaignsSteps.do_campaigns(11, service_order_id, {'Bucks': 20}, 0)
    steps.InvoiceSteps.pay(invoice_id, 1500)
    steps.ActsSteps.generate(client_id, force=1)
    steps.CampaignsSteps.do_campaigns(11, service_order_id, {'Bucks': 50}, 0)
    steps.ActsSteps.generate(client_id, force=1)

def create_personal_account_new_of_old_fictive():
    old_fictive = False

    contract_type = 'opt_agency_post'
    to_iso = utils.Date.date_to_iso_format
    NOW = datetime.datetime.now()
    HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + timedelta(days=180))
    HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - timedelta(days=180))
    PAYSYS_ID = 1003
    SERVICE_ID = 7
    PRODUCT_ID = 1475

    client_id = steps.ClientSteps.create({'IS_AGENCY': 1})
    person_id = steps.PersonSteps.create(client_id, 'ur')
    contract_id, _ = steps.ContractSteps.create_contract(contract_type, {'CLIENT_ID': client_id, 'PERSON_ID': person_id,
                                                                         'DT': HALF_YEAR_BEFORE_NOW_ISO,
                                                                         'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
                                                                         'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
                                                                         'SERVICES': [7],
                                                                         'FIRM': '1',
                                                                         'PERSONAL_ACCOUNT_FICTIVE': 1,
                                                                         'DISCOUNT_POLICY_TYPE': 0})
    if old_fictive:
        steps.ContractSteps.force_convert_to_fictive_credit_scheme(contract_id)
    service_order_id = steps.OrderSteps.next_id(service_id=SERVICE_ID)
    order_id = steps.OrderSteps.create(client_id=client_id, product_id=PRODUCT_ID, service_id=SERVICE_ID,
                                       service_order_id=service_order_id)
    orders_list = [
        {'ServiceID': SERVICE_ID, 'ServiceOrderID': service_order_id, 'Qty': 200, 'BeginDT': NOW}
    ]
    request_id = steps.RequestSteps.create(client_id=client_id, orders_list=orders_list)
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id, paysys_id=PAYSYS_ID,
                                                 credit=1, contract_id=contract_id, overdraft=0, endbuyer_id=None)
    steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 50}, 0)
    steps.ActsSteps.generate(client_id, force=1)
    # steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 120}, 0)
    # steps.ActsSteps.generate(client_id, force=1)
    # steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 121}, 0)
    # steps.ActsSteps.generate(client_id, force=1)
    # steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 122}, 0)
    # steps.ActsSteps.generate(client_id, force=1)
    # steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 123}, 0)
    # steps.ActsSteps.generate(client_id, force=1)
    # steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 124}, 0)
    # steps.ActsSteps.generate(client_id, force=1)
    # steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 125}, 0)
    # steps.ActsSteps.generate(client_id, force=1)
    # steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 126}, 0)
    # steps.ActsSteps.generate(client_id, force=1)
    # steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 127}, 0)
    # steps.ActsSteps.generate(client_id, force=1)
    # steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 128}, 0)
    # steps.ActsSteps.generate(client_id, force=1)
    # steps.CampaignsSteps.do_campaigns(SERVICE_ID, service_order_id, {'Bucks': 120}, 0)
    # steps.ActsSteps.generate(client_id, force=1)


def create_request():
    client_id = steps.ClientSteps.create()
    # person_id = steps.PersonSteps.create(client_id, 'ph')
    # service_order_id = steps.OrderSteps.next_id(37)
    # steps.OrderSteps.create(client_id, service_order_id, product_id=502917, service_id=37)
    # orders_list = [{'ServiceID': 37, 'ServiceOrderID': service_order_id, 'Qty': 1},
    #                ]
    # request_id = steps.RequestSteps.create(client_id, orders_list)

# create_request()
# create_personal_account_new_of_old_fictive()
# steps.InvoiceSteps.pay(122671556, 1500)
# steps.CampaignsSteps.do_campaigns(7, 54707739, {'Bucks': 10000}, 0)
# steps.ActsSteps.generate(135195668, force=0, date=datetime.datetime(2020,6,1))
# create_prepayment_invoice()
# steps.ExportSteps.export_oebs(client_id=135108352, contract_id=4361073, invoice_id=114143717, act_id=119068072)
# client_id = steps.ClientSteps.create(single_account_activated=False,
#                                          enable_single_account=True)
# person_id= steps.PersonSteps.create(client_id, 'ur')
# from btestlib.constants import PersonTypes, Services, Paysyses, Products, User, ContractPaymentType, \
#     Currencies, ContractCreditType, ContractCommissionType, Firms
# LOGIN_INVOICES_SEARCH = User(1119019053, 'yndx-static-balance-2')
# steps.UserSteps.link_user_and_client(LOGIN_INVOICES_SEARCH, client_id)
#
# single_account_number = steps.ElsSteps.create_els(client_id)
# now = datetime.datetime.now()
# paysys_id = Paysyses.BANK_UR_RUB.id
# orders_list = []
# service_order_id = steps.OrderSteps.next_id(service_id=7)  # внешний ID заказа
# order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=7,
#                                        product_id=1475)

# agency_id = 2728706
# service_order_id = 21454391
# orders_list = [{'ServiceID': 11, 'ServiceOrderID': service_order_id, 'Qty': 5,
#                         'BeginDT': datetime.datetime.now()}]
# request_id = steps.RequestSteps.create(agency_id, orders_list)
# client_id = steps.ClientSteps.create()
# person_id = steps.PersonSteps.create(client_id, 'ur')
#
# invoice_id, external_id, _ = steps.InvoiceSteps.create(request_id, person_id, paysys_id)
#
# request_id_1 = steps.RequestSteps.create(client_id, orders_list, additional_params={'InvoiceDesireDT': now})
#
# invoice_id, external_id, _ = steps.InvoiceSteps.create(request_id_1, person_id, paysys_id)
#
# agency_id = steps.ClientSteps.create({'IS_AGENCY': 1, 'REGION_ID': Regions.SW.id})
# person_id = steps.PersonSteps.create(agency_id, PersonTypes.SW_UR.code)
# contract_id, _ = steps.ContractSteps.create_contract_new(ContractCommissionType.SW_OPT_AGENCY,
#                             {'CLIENT_ID': agency_id,
#                             'PERSON_ID': person_id,
#                             'DT': utils.Date.to_iso(context.contract_dt),
#                             'FINISH_DT': utils.Date.to_iso(context.contract_dt + relativedelta(years=3)),
#                             'IS_SIGNED': utils.Date.to_iso(d.datetime.now()),
#                             'SERVICES': [Services.DIRECT.id],
#                             'CURRENCY': Currencies.USD.num_code,
#                             'DISCOUNT_POLICY_TYPE': context.discount_policy.value,
#                             'CONTRACT_DISCOUNT': context.fixed_discount,
#                             'WHOLESALE_AGENT_PREMIUM_AWARDS_SCALE_TYPE': 25
#                             })
# client_id = steps.ClientSteps.create()
# steps.PersonSteps.create(client_id, 'ur', {'is-partner': '1'})
# service_order_id = steps.OrderSteps.next_id(11)
# create_personal_account_new_of_old_fictive()


# create_prepayment_invoice()

# create_prepayment_invoice()
# steps.ActsSteps.hide(129618579)
# create_personal_account_new_of_old_fictive()
# api.medium().GetClientPersons('90894242',)

list_a = {'test_2': '1', 'test_1': '2', 'gg': '343', 'val_1': 'one', 'val_2': 'two'}
k = { key.split('_')[1]: list_a[key] for key in list_a if key.startswith('test')}
v = { key.split('_')[1]: list_a[key] for key in list_a if key.startswith('val')}
new = {}
for i in range(1,len(k)+1):
    new.update({k[str(i)]: v[str(i)]})
print new
for el in new:
    print el, new[el]
