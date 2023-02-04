# coding: utf-8
__author__ = 'atkaya'

import datetime

from balance import balance_db as db
from balance import balance_steps as steps
from btestlib.data.partner_contexts import TAXI_RU_CONTEXT_CLONE
from dateutil.relativedelta import relativedelta
import btestlib.environments as env
from btestlib import utils, reporter
from btestlib.constants import PaymentType, Export, Services
from balance.tests.payment.test_payments_wo_trust import create_payment, create_expected_payment_data, \
    prepare_test_data_for_payment
from btestlib.data.simpleapi_defaults import DEFAULT_USER, TrustPaymentCases


service = Services.TAXI
context = TAXI_RU_CONTEXT_CLONE

#----UTILS----------------------------------
def get_invoice_ids(client_id):
        query = "SELECT inv.id, inv.EXTERNAL_ID " \
                "FROM T_INVOICE inv LEFT JOIN T_EXTPROPS prop ON " \
                "inv.ID = prop.OBJECT_ID AND " \
                "prop.CLASSNAME='PersonalAccount' AND prop.ATTRNAME='service_code' " \
                "WHERE inv.type='personal_account' AND inv.CLIENT_ID=:client_id AND prop.VALUE_STR IS NULL"

        params = {'client_id': client_id}
        invoice = db.balance().execute(query, params, single_row=True)
        return invoice['id'], invoice['external_id']

def export_correction_netting(cash_fact_id, handle_with_process_payment=False):
    if not handle_with_process_payment:
            steps.CommonSteps.export(Export.Type.THIRDPARTY_TRANS, Export.Classname.OEBS_CPF, cash_fact_id)

def process_payment(invoice_id, handle_with_process_payment=False):
        if handle_with_process_payment:
            set_wait_for_correction_netting(timeout=5)
        steps.CommonSteps.export(Export.Type.PROCESS_PAYMENTS, Export.Classname.INVOICE, invoice_id)

def set_wait_for_correction_netting(timeout):
        query = "UPDATE T_CONFIG SET VALUE_NUM=:timeout WHERE ITEM='WAIT_FOR_CORRECTION_NETTING_BEFORE_PROCESS_PAYMENT'"
        params = {'timeout': timeout}
        db.balance().execute(query, params)

#-----TEST-----------------------------------
CONTRACT_START_DT = datetime.datetime(2019,1,1)
ORDER_DT = utils.Date.nullify_time_of_date(datetime.datetime.now() - relativedelta(days=3))
PROCESS_TAXI_DT = ORDER_DT + relativedelta(days=1)
PAYMENT_DT = ORDER_DT
commission_sum_1 = 100
commission_sum_2 = 500
commission_sum = commission_sum_2
client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(context, is_postpay=0,
                    additional_params={'start_dt': CONTRACT_START_DT, 'netting_pct': 100, 'netting': 1})
query_update_mv = "BEGIN dbms_mview.refresh('BO.MV_PARTNER_TAXI_CONTRACT','C'); END;"
db.balance().execute(query_update_mv, descr='Обновляем MV_PARTNER_TAXI_CONTRACT')
payment_data = TrustPaymentCases.TAXI_RU_124
data_for_payment = prepare_test_data_for_payment(payment_data)
data_for_payment = data_for_payment.new(client_id=client_id, person_id=person_id, contract_id=contract_id)
query_for_service_product = "Insert into t_service_product (ID,SERVICE_ID,PRODUCT_ID,NAME,PARTNER_ID,PRODUCT_TYPE,PARENT_ID,UPDATE_DT,EXTERNAL_ID," \
                            "PACKAGE_NAME,INAPP_NAME,SINGLE_PURCHASE,SUBS_PERIOD,SUBS_TRIAL_PERIOD," \
                            "ACTIVE_UNTIL_DT,HIDDEN,SERVICE_FEE,FISCAL_NDS,FISCAL_TITLE) " \
                            "values (s_service_product_id.nextval,'124','504691','Super Product'," \
                            ":partner_id,'app',null,to_date('04-03-19 13:26:14','DD-MM-RR HH24:MI:SS')," \
                            ":external_id,null,null,'0',null,null,null,null,null," \
                            "'nds_none','test_fiscal_title')"
params = {'partner_id': client_id, 'external_id': steps.SimpleApi.generate_fake_trust_payment_id()}
db.balance().execute(query_for_service_product, params)
create_payment(data_for_payment)
steps.TaxiSteps.create_order(client_id, ORDER_DT, payment_type=PaymentType.CARD,
                             currency=context.currency.iso_code,
                             commission_sum=commission_sum)
steps.TaxiSteps.process_netting(contract_id, PROCESS_TAXI_DT)
query = "SELECT id FROM T_THIRDPARTY_TRANSACTIONS where contract_id = :contract_id"
params = {'contract_id': contract_id}
transaction_id = db.balance().execute(query, params)[0]['id']
query = "SELECT id FROM T_THIRDPARTY_CORRECTIONS where contract_id = :contract_id"
params = {'contract_id': contract_id}
correction_id = db.balance().execute(query, params)[0]['id']
steps.CommonSteps.export(queue_='OEBS', classname='ThirdPartyCorrection', object_id=correction_id)

steps.ExportSteps.export_oebs(client_id=client_id, contract_id=contract_id,
                              correction_id=correction_id,
                              transaction_id=transaction_id)
invoice_id, invoice_eid = get_invoice_ids(client_id)



# первый договор с суммой неттинга 1200
# invoice_id_1 = 90607948
# invoice_eid_2 = u'ЛСТ-1602348805-1'
# fact_id_1 = '771363656'

# второй договор с суммой неттинга 6000
# invoice_id_2 = 90607951
# invoice_eid_2 = u'ЛСТ-1602348808-1'
# fact_id_2 = '771359445'
#
# export_correction_netting(fact_id_2, 1)
# process_payment(invoice_id_2, 1)





# steps.ExportSteps.export_oebs(client_id=105083330, contract_id=911472,
#                               correction_id='20000006160',
#                               transaction_id='20000006151')
#
# steps.ExportSteps.export_oebs(client_id=105083428, contract_id=911499,
#                               correction_id='20000006392',
#                               transaction_id='20000006384')





# env.SimpleapiEnvironment.switch_param(dbname=env.TrustDbNames.BS_XG, xmlrpc_url=env.TrustApiUrls.XMLRPC_ORA)
# service_order_id, trust_payment_id, purchase_token, payment_id = \
#         steps.SimpleApi.create_trust_payment(service, service_product_id, currency=context.payment_currency,
#                                              order_dt=PAYMENT_DT)
# steps.CommonPartnerSteps.export_payment(payment_id)