import datetime

import hamcrest
import pytest

import balance.balance_db as db
import btestlib.reporter as reporter
import btestlib.utils as utils
from balance import balance_steps as steps
from balance.features import Features
from btestlib.constants import Services
from btestlib.data.defaults import Date
from temp.igogor.balance_objects import Contexts, Products, Firms, Paysyses, PersonTypes, Currencies, Regions, \
    ContractCommissionType, ContractPaymentType, ContractCreditType

# context = Contexts.DIRECT_BYN_BYU_CONTEXT.new(service=Services.REALTY_KOMM, product=Products.REALTY_KOMM,
#                                               paysys=Paysyses.BANK_UR_RUB_VERTICAL, firm=Firms.VERTICAL_12,
#                                               person=PersonTypes.UR)


context = Contexts.DIRECT_BYN_BYU_CONTEXT.new(service=Services.REALTY_COMM, product=Products.REALTY_COMM,
                                              paysys=Paysyses.BANK_UR_RUB_VERTICAL, firm=Firms.VERTICAL_12,
                                              person=PersonTypes.UR)
#
# context = Contexts.DIRECT_BYN_BYU_CONTEXT.new(service=Services.DIRECT, product=Products.DIRECT_FISH,
#                                               paysys=Paysyses.BANK_UR_RUB, firm=Firms.YANDEX_1,
#                                               person=PersonTypes.UR)
NOW = datetime.datetime.now()
QTY = 10


def test_act_realty_wo_nds():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id=client_id, type_=context.person.code)
    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id,
                            product_id=context.product.id)
    orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': QTY,
                    'BeginDT': NOW}]
    request_id = steps.RequestSteps.create(client_id, orders_list,
                                           additional_params=dict(InvoiceDesireDT=NOW))
    contract_params = {
        'CLIENT_ID': client_id,
        'PERSON_ID': person_id,
        'PAYMENT_TYPE': ContractPaymentType.POSTPAY,
        'SERVICES': [context.service.id],
        'DT': utils.Date.to_iso(utils.Date.nullify_time_of_date(NOW)),
        'IS_SIGNED': utils.Date.to_iso(utils.Date.nullify_time_of_date(NOW)),
        'FINISH_DT': Date.HALF_YEAR_AFTER_TODAY_ISO,
        'CURRENCY': Currencies.RUB.num_code,
        'CREDIT_TYPE': ContractCreditType.BY_TERM_AND_SUM,
        'CREDIT_LIMIT_SINGLE': 1000000,
        'PERSONAL_ACCOUNT': 1,
        'PERSONAL_ACCOUNT_FICTIVE': 1,
        'FIRM': context.firm.id
    }
    contract_id, contract_external_id = steps.ContractSteps.create_contract_new(ContractCommissionType.NO_AGENCY,
                                                                                contract_params)

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id,
                                                 paysys_id=context.paysys.id,
                                                 credit=1, contract_id=contract_id, overdraft=0)

    steps.InvoiceSteps.pay(invoice_id)
    steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id, {'Bucks': QTY}, 0, datetime.datetime.now())
    act_id = steps.ActsSteps.generate(client_id, force=1, date=NOW)[0]
    invoice_id = db.get_repayment_by_invoice(invoice_id)[0]['repayment_invoice_id']
    steps.ExportSteps.export_oebs(client_id=client_id, contract_id=contract_id, invoice_id=invoice_id, act_id=act_id)


def test_act_realty_wo_nds_wo_contract():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id=client_id, type_=context.person.code)
    service_order_id = steps.OrderSteps.next_id(service_id=context.service.id)
    order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=context.service.id,
                                       product_id=context.product.id)
    orders_list = [{'ServiceID': context.service.id, 'ServiceOrderID': service_order_id, 'Qty': QTY,
                    'BeginDT': NOW}]
    request_id = steps.RequestSteps.create(client_id, orders_list,
                                           additional_params=dict(InvoiceDesireDT=NOW))

    invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id,
                                                 paysys_id=context.paysys.id,
                                                 credit=0, contract_id=None, overdraft=0)

    steps.InvoiceSteps.pay(invoice_id)
    db.balance().execute('''update (select * from t_invoice where id =:invoice_id) set nds = 0''',
                         {'invoice_id': invoice_id})
    steps.ExportSteps.export_oebs(invoice_id=invoice_id, client_id=client_id)

    steps.CampaignsSteps.do_campaigns(context.service.id, service_order_id, {'Bucks': QTY}, 0, datetime.datetime.now())
    act_id = steps.ActsSteps.generate(client_id, force=1, date=NOW)[0]


    # request_id = steps.RequestSteps.create(client_id, orders_list,
    #                                        additional_params=dict(InvoiceDesireDT=old_tax_dt))
    #
    # invoice_id, _, _ = steps.InvoiceSteps.create(request_id=request_id, person_id=person_id,
    #                                              paysys_id=context.paysys.id,
    #                                              credit=0, contract_id=None, overdraft=0)
    #
    # steps.InvoiceSteps.pay(invoice_id)
    #
    #
    # act_id = steps.ActsSteps.generate(client_id, force=1, date=NOW)[0]
