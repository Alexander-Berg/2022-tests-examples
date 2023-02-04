# -*- coding: utf-8 -*-

import datetime
from decimal import Decimal as D

from balance import balance_steps as steps
from btestlib import utils
from temp.igogor.balance_objects import Contexts
from btestlib.constants import Firms, ContractCommissionType, PersonTypes, Users
from random import randrange

to_iso = utils.Date.date_to_iso_format
dt_delta = utils.Date.dt_delta

NOW = datetime.datetime.now()
NOW_ISO = to_iso(NOW)
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - datetime.timedelta(days=180))
ORDER_DT = NOW
INVOICE_DT = NOW
COMPLETIONS_DT = NOW
ACT_DT = NOW

QTY = D('250')
COMPLETIONS = D('99.99')


data = [
    # [ client 1355974812
    # 'sw_yt', 328980, 508287, 35,
    # {'MANAGER_CODE':   31699,
    #     'PARTNER_CREDIT': 1,
    #     'CURRENCY':   840,
    #     'BANK_DETAILS_ID':999,
    #     'PAYMENT_TERM':   30,
    #     'UNILATERAL': 0,
    #     'COMMISSION': 22,
    #     'PAYMENT_TYPE':   3,
    #     'CREDIT_TYPE':2,
    #     'SERVICES':   [35, 98],
    #     'CALC_DEFERMANT': 0,
    #     'REPAYMENT_ON_CONSUME':   0,
    #     'PERSONAL_ACCOUNT':   1,
    #     'ATYPICAL_CONDITIONS':1,
    #     'FIRM':   7,
    #     'PERSONAL_ACCOUNT_FICTIVE':   1,
    # }, 1047],

    # ['sw_yt', 494155, 509395,35,
    #  {
    #      'MANAGER_CODE': 37649,
    #      'PARTNER_CREDIT': 1,
    #      'CURRENCY': 978,
    #      'PRINT_FORM_TYPE': 3,
    #      'FAKE_ID': 0,
    #      'BANK_DETAILS_ID': 1002,
    #      'REPAYMENT_ON_CONSUME': 0,
    #      'PAYMENT_TYPE': 3,
    #      'COMMISSION': 22,
    #      'PERSONAL_ACCOUNT_FICTIVE': 1,
    #      'FIRM': 16,
    #      'PAYMENT_TERM': 60,
    #      'CALC_DEFERMANT': 0,
    #      'PERSONAL_ACCOUNT': 1,
    #      'UNILATERAL': 0,
    #      'CREDIT_TYPE': 1,
    #      'SERVICES': [35],
    #      'IS_BOOKED': 0,
    #      'LIFT_CREDIT_ON_PAYMENT': 0,
    #      'ATYPICAL_CONDITIONS': 1,
    #
    #  }, 1601046],
    # ['sw_yt', 636500, 509698, 35,
    #  {
    #      'REPAYMENT_ON_CONSUME': 0,
    #      'PARTNER_CREDIT': 1,
    #      'COMMISSION': 0,
    #      'UNILATERAL': 0,
    #      'CURRENCY': 978,
    #      'FIRM': 7,
    #      'IS_BOOKED': 0,
    #      'BANK_DETAILS_ID': 998,
    #      'PAYMENT_TYPE': 3,
    #      'PAYMENT_TERM': 15,
    #      'PERSONAL_ACCOUNT_FICTIVE': 1,
    #      'CALC_DEFERMANT': 0,
    #      'ATYPICAL_CONDITIONS': 1,
    #      'PRINT_FORM_TYPE': 0,
    #      'MANAGER_CODE': 31699,
    #      'PERSONAL_ACCOUNT': 1,
    #      'CREDIT_TYPE': 1,
    #      'SERVICES': [35],
    #      'LIFT_CREDIT_ON_PAYMENT': 0,
    #      'MANAGER_BO_CODE': 22479,
    #      'FAKE_ID': 0,
    #  }, 1046],
    # нет платежных политик?
    # ['sw_yt', 3769282,512240, 110,
    #  {
    #      'PAYMENT_TERM': 90,
    #      'INDIVIDUAL_DOCS': 0,
    #      'UNILATERAL': 0,
    #      'PAYMENT_TYPE': 3,
    #      'COMMISSION': 23,
    #      'PERSONAL_ACCOUNT': 1,
    #      'SERVICES': [35, 110],
    #      'ATYPICAL_CONDITIONS': 0,
    #      'CREDIT_LIMIT_SINGLE': 1000000,
    #      'FIRM': 7,
    #      'MANAGER_CODE': 33557,
    #      'WHOLESALE_AGENT_PREMIUM_AWARDS_SCALE_TYPE': 26,
    #      'CURRENCY': 949,
    #      'CREDIT_LIMIT_IN_CONTRACT_CURRENCY': 0,
    #      'CALC_DEFERMANT': 0,
    #      'PERSONAL_ACCOUNT_FICTIVE': 1,
    #      'LIFT_CREDIT_ON_PAYMENT': 1,
    #      'PRINT_FORM_TYPE': 3,
    #      'BANK_DETAILS_ID': 1005,
    #      'CREDIT_TYPE': 2,
    #
    #  }, 11153],
    # ['sw_yt', 3769284, 511660, 35,
    #  {
    #      'PRINT_FORM_TYPE': 3,
    #      'FIRM': 7,
    #      'PAYMENT_TYPE': 3,
    #      'COMMISSION': 23,
    #      'DISCOUNT_POLICY_TYPE': 0,
    #      'SERVICES': [35, 110],
    #      'CREDIT_LIMIT_SINGLE': 800000,
    #      'WHOLESALE_AGENT_PREMIUM_AWARDS_SCALE_TYPE': 0,
    #      'ATYPICAL_CONDITIONS': 0,
    #      'BANK_DETAILS_ID': 1005,
    #      'LIFT_CREDIT_ON_PAYMENT': 1,
    #      'CURRENCY': 949,
    #      'CREDIT_LIMIT_IN_CONTRACT_CURRENCY': 0,
    #      'PERSONAL_ACCOUNT_FICTIVE': 1,
    #      'INDIVIDUAL_DOCS': 0,
    #      'CREDIT_TYPE': 2,
    #      'PERSONAL_ACCOUNT': 1,
    #      'MANAGER_CODE': 33557,
    #      'UNILATERAL': 0,
    #      'PAYMENT_TERM': 60,
    #      'CALC_DEFERMANT': 0,
    #  }, 11153],
    ['sw_yt', 3769349, 1475, 7,
     {
         'SERVICES': [67, 7, 77, 70],
         'DISCOUNT_POLICY_TYPE': 0,
         'REPAYMENT_ON_CONSUME': 0,
         'CURRENCY': 978,
         'CREDIT_TYPE': 2,
         'PRINT_FORM_TYPE': 3,
         'UNILATERAL': 0,
         'BANK_DETAILS_ID': 998,
         'COMMISSION': 23,
         'PAYMENT_TERM': 30,
         'FIRM': 7,
         'WHOLESALE_AGENT_PREMIUM_AWARDS_SCALE_TYPE': 0,
         'CALC_DEFERMANT': 0,
         'PERSONAL_ACCOUNT': 1,
         'ATYPICAL_CONDITIONS': 0,
         'PERSONAL_ACCOUNT_FICTIVE': 1,
         'LIFT_CREDIT_ON_PAYMENT': 1,
         'CREDIT_LIMIT_IN_CONTRACT_CURRENCY': 0,
         'PAYMENT_TYPE': 3,
         'CREDIT_LIMIT_SINGLE': 25000,
         'MANAGER_CODE': 20677,
     }, 1046],
    ['sw_yt', 3927175, 503387, 70,
     {
         'PARTNER_COMMISSION_PCT': 5,
         'SERVICES': [67, 7, 77, 70],
         'PAYMENT_TERM': 35,
         'CREDIT_LIMIT_SINGLE': 100000,
         'PAYMENT_TYPE': 3,
         'PERSONAL_ACCOUNT_FICTIVE': 1,
         'PERSONAL_ACCOUNT': 1,
         'COMMISSION': 22,
         'BANK_DETAILS_ID': 998,
         'FIRM': 7,
         'UNILATERAL': 0,
         'REPAYMENT_ON_CONSUME': 1,
         'LIFT_CREDIT_ON_PAYMENT': 1,
         'MANAGER_CODE': 22363,
         'CURRENCY': 978,
         'CREDIT_TYPE': 2,
     },1046],
    ['sw_yt', 3927288, 509961, 100,
     {
         'CREDIT_TYPE': 2,
         'SERVICES': [67, 70, 77, 110, 7],
         'FIRM': 7,
         'CURRENCY': 978,
         'BANK_DETAILS_ID': 998,
         'MANAGER_CODE': 22032,
         'CREDIT_LIMIT_SINGLE': 200000,
         'PERSONAL_ACCOUNT_FICTIVE': 1,
         'PERSONAL_ACCOUNT': 1,
         'PAYMENT_TERM': 30,
         'PAYMENT_TYPE': 3,
         'COMMISSION': 22,
         'LIFT_CREDIT_ON_PAYMENT': 1,
         'UNILATERAL': 0,

     },1046],
    ['sw_yt', 3927299, 1475, 7,
     {
         'PERSONAL_ACCOUNT': 1,
         'UNILATERAL': 0,
         'SERVICES': [70, 7, 67, 77],
         'MANAGER_CODE': 23613,
         'PAYMENT_TYPE': 3,
         'PAYMENT_TERM': 30,
         'LIFT_CREDIT_ON_PAYMENT': 1,
         'BANK_DETAILS_ID': 999,
         'CREDIT_LIMIT_SINGLE': 49999,
         'PERSONAL_ACCOUNT_FICTIVE': 1,
         'CURRENCY': 840,
         'CREDIT_TYPE': 2,
         'FIRM': 7,
         'COMMISSION': 22,

     }, 1047],
    ['sw_yt', 3927366, 506604, 70,
     {
         'PAYMENT_TYPE': 3,
         'SERVICES': [7, 67, 70, 77],
         'PERSONAL_ACCOUNT_FICTIVE': 1,
         'PERSONAL_ACCOUNT': 1,
         'BANK_DETAILS_ID': 999,
         'MANAGER_CODE': 20677,
         'LIFT_CREDIT_ON_PAYMENT': 1,
         'PAYMENT_TERM': 30,
         'UNILATERAL': 0,
         'CREDIT_TYPE': 2,
         'CREDIT_LIMIT_SINGLE': 90000,
         'FIRM': 7,
         'CURRENCY': 840,
         'COMMISSION': 22,
     },1047],
    ['sw_yt', 3927581, 1475, 7,
     {
         'SERVICES': [7, 67, 70, 77],
         'PAYMENT_TYPE': 3,
         'LIFT_CREDIT_ON_PAYMENT': 1,
         'CREDIT_TYPE': 2,
         'CURRENCY': 949,
         'BANK_DETAILS_ID': 1005,
         'PERSONAL_ACCOUNT_FICTIVE': 1,
         'COMMISSION': 22,
         'FIRM': 7,
         'UNILATERAL': 0,
         'PAYMENT_TERM': 30,
         'CREDIT_LIMIT_SINGLE': 70000,
         'PERSONAL_ACCOUNT': 1,
         'MANAGER_CODE': 38139,
     }, 11153],
    ['sw_ur', 3927722, 1475, 7,
     {
         'MANAGER_CODE': 22363,
         'COMMISSION': 22,
         'SERVICES': [7, 67, 70, 77],
         'CREDIT_TYPE': 2,
         'CURRENCY': 840,
         'PAYMENT_TERM': 30,
         'PAYMENT_TYPE': 3,
         'CREDIT_LIMIT_SINGLE': 30000,
         'PERSONAL_ACCOUNT': 1,
         'UNILATERAL': 0,
         'BANK_DETAILS_ID': 999,
         'FIRM': 7,
         'PERSONAL_ACCOUNT_FICTIVE': 1,
         'LIFT_CREDIT_ON_PAYMENT': 1,
         'ATYPICAL_CONDITIONS': 1,
     },1044],
    ['sw_ur', 3927748, 1475, 7,
     {
         'COMMISSION': 22,
         'SERVICES': [7, 67, 70, 77],
         'BANK_DETAILS_ID': 998,
         'UNILATERAL': 0,
         'PERSONAL_ACCOUNT_FICTIVE': 1,
         'CURRENCY': 978,
         'PERSONAL_ACCOUNT': 1,
         'MANAGER_CODE': 23336,
         'PAYMENT_TYPE': 3,
         'CREDIT_TYPE': 2,
         'PAYMENT_TERM': 30,
         'FIRM': 7,
         'LIFT_CREDIT_ON_PAYMENT': 1,
         'CREDIT_LIMIT_SINGLE': 100000,
     },1043],
    ['sw_ur', 3927749, 1475, 7,
     {
         'UNILATERAL': 0,
         'LIFT_CREDIT_ON_PAYMENT': 1,
         'COMMISSION': 22,
         'MANAGER_CODE': 20809,
         'SERVICES': [7, 67, 70, 77],
         'PAYMENT_TYPE': 3,
         'FIRM': 7,
         'BANK_DETAILS_ID': 997,
         'CREDIT_TYPE': 2,
         'ATYPICAL_CONDITIONS': 1,
         'PERSONAL_ACCOUNT_FICTIVE': 1,
         'PAYMENT_TERM': 30,
         'CURRENCY': 756,
         'PERSONAL_ACCOUNT': 1,
         'CREDIT_LIMIT_SINGLE': 20000,
     },1045]

]

res = []
for i in range(len(data)):
    client_id = steps.ClientSteps.create() if data[i][4]['COMMISSION'] in (0, 22) else steps.ClientSteps.create({'IS_AGENCY': 1})
    person_id = steps.PersonSteps.create(client_id, data[i][0], {'purchase_order': 'PO ' + str(randrange(10000000, 99999999))})

    contract_params = data[i][4]
    contract_params.update({'CLIENT_ID': client_id, 'PERSON_ID': person_id})
    if contract_params['COMMISSION'] == 0:
        commission_type = ContractCommissionType.NO_AGENCY
    elif contract_params['COMMISSION'] == 22:
        commission_type = ContractCommissionType.SW_OPT_CLIENT
    elif contract_params['COMMISSION'] == 23:
        commission_type = ContractCommissionType.SW_OPT_AGENCY
    contract_id, _ = steps.ContractSteps.create_contract_new(commission_type, contract_params)
    service_order_id_list = []
    orders_list = []


    for _ in xrange(1):
        service_order_id = steps.OrderSteps.next_id(data[i][3])
        steps.OrderSteps.create(client_id, service_order_id, service_id=data[i][3],
                                product_id=data[i][2])
        orders_list.append(
            {'ServiceID': data[i][3], 'ServiceOrderID': service_order_id, 'Qty': D('50'), 'BeginDT': ORDER_DT})
        service_order_id_list.append(service_order_id)

    # Создаём риквест
    request_id = steps.RequestSteps.create(client_id, orders_list,
                                           additional_params=dict(InvoiceDesireDT=INVOICE_DT))

    invoice_id, external_id, _ = steps.InvoiceSteps.create(request_id, person_id, data[i][5], credit=True,
                                                 contract_id=contract_id)
    steps.CampaignsSteps.do_campaigns(data[i][2], service_order_id_list[0], {'Bucks': QTY}, 0,
                                              campaigns_dt=COMPLETIONS_DT)
    steps.ActsSteps.generate(client_id, 1, ACT_DT)
    y_invoice_id, _ = steps.InvoiceSteps.get_invoice_ids(client_id, 'y_invoice')