# -*- coding: utf-8 -*-
__author__ = 'atkaya'

from datetime import datetime
import balance.balance_db as db
from balance import balance_steps as steps
from btestlib.constants import TransactionType, Export, PaymentType, FoodProductType, PaysysType, Services
from btestlib.data.partner_contexts import FOOD_RESTAURANT_CONTEXT

def get_fake_id():
    return db.balance().execute("select S_TEST_TRUST_PAYMENT_ID.nextval as id from dual")[0]['id']
def get_fake_transaction_id():
    return db.balance().execute("select S_TEST_FOOD_TRANSACTION_ID.nextval as id from dual")[0]['id']

context = FOOD_RESTAURANT_CONTEXT
RESTAURANT_COMPLETION_SERVICE = Services.FOOD_SERVICES
payment_dt = datetime.now().replace(microsecond=0)
AMOUNT = 1000
NETTING_AMOUNT = 500
service_order_id = get_fake_id()
payment_id = get_fake_id()
transaction_id = get_fake_transaction_id()

client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(context, additional_params={'start_dt': datetime(2019,11,1),
                                                                                                               'manager_uid': 217731810})
# steps.ExportSteps.export_oebs(client_id=client_id, contract_id=contract_id)
side_payment_id, side_transaction_id = \
    steps.PartnerSteps.create_sidepayment_transaction(client_id, payment_dt, AMOUNT,
                                                      PaymentType.CARD, context.service.id,
                                                      transaction_type=TransactionType.PAYMENT,
                                                      currency=context.currency,
                                                      paysys_type_cc=PaysysType.PAYTURE,
                                                      extra_str_0=FoodProductType.GOODS,
                                                      extra_str_1=service_order_id,
                                                      extra_str_2=payment_id,
                                                      transaction_id=transaction_id,
                                                      payload="[]")
steps.ExportSteps.create_export_record_and_export(side_payment_id, Export.Type.THIRDPARTY_TRANS,
                                                      Export.Classname.SIDE_PAYMENT, service_id=context.service.id)

new_tpt_id = db.balance().execute("select S_REQUEST_ORDER_ID.nextval as id from dual")[0]['id']
transaction_id = steps.CommonPartnerSteps.get_synthetic_thirdparty_transaction_id_by_payment_id(side_payment_id)
db.balance().execute("update t_thirdparty_transactions set id = "+str(new_tpt_id)+" where id = "+str(transaction_id))
db.balance().execute("update t_export set object_id = "+str(new_tpt_id)+" where type = 'OEBS' and object_id = "+str(transaction_id))
steps.ExportSteps.export_oebs(transaction_id=str(new_tpt_id))
steps.PartnerSteps.create_fake_product_completion(datetime(2019,11,22), client_id=client_id,
                                                      service_id=RESTAURANT_COMPLETION_SERVICE.id, service_order_id=0,
                                                      commission_sum=NETTING_AMOUNT,
                                                      type = FoodProductType.GOODS)
steps.TaxiSteps.process_netting(contract_id, datetime.now())
correction_id = db.balance().execute("select id from t_thirdparty_corrections where contract_id ="+str(contract_id))[0]['id']
steps.ExportSteps.export_oebs(correction_id=str(correction_id))

