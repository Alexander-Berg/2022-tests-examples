# coding: utf-8
__author__ = 'atkaya'

import datetime
import decimal
from decimal import Decimal

from balance import balance_api as api
from balance import balance_db as db
from balance import balance_steps as steps
from btestlib.constants import Services, Managers, NdsNew as Nds, Currencies, \
    OfferConfirmationType, Paysyses, PromocodeClass, ContractSubtype, Regions, CountryRegion
from btestlib.data import defaults
from btestlib.data.simpleapi_defaults import ThirdPartyData

PASSPORT_ID = 16571028
MANAGER = Managers.SOME_MANAGER
SERVICE_CONNECT = Services.CONNECT.id
COUNTRY_ARMENIA = 168
COUNTRY_KAZAKHSTAN = 159

def Create_client_person_contract_taxi():
    client_id = steps.ClientSteps.create()
    # person_id = steps.PersonSteps.create(client_id, 'eu_yt')
    # partner_person_id = steps.PersonSteps.create(client_id, 'eu_yt', {'is-partner': '1'})
    # params = {'CLIENT_ID': client_id, 'PERSON_ID': person_id,
    #           'DT': datetime.datetime(2017,1,1), 'CURRENCY': Currencies.USD.num_code, 'COUNTRY': COUNTRY_KAZAKHSTAN,
    #           'FIRM': 22
    #           }
    # contract_id, external_id = steps.ContractSteps.create_contract('taxi_postpay_sng', params)
    # partner_contract_id = api.medium().AcceptTaxiOffer(PASSPORT_ID, {'contract_id': contract_id,
    #                                                            'person_id': partner_person_id,
    #                                                            })['ID']
    person_id = steps.PersonSteps.create(client_id, 'ur')
    partner_person_id = steps.PersonSteps.create(client_id, 'ur', {'is-partner': '1'})
    params = {'CLIENT_ID': client_id, 'PERSON_ID': person_id,
              'DT': datetime.datetime(2017,1,1), 'COUNTRY': 225,
              'FIRM': 13, 'REGION': '77000000000', 'NDS_FOR_RECEIPT': 18
              }
    contract_id, external_id = steps.ContractSteps.create_contract('taxi_postpay_sng', params)
    # partner_contract_id = api.medium().AcceptTaxiOffer(PASSPORT_ID, {'contract_id': contract_id,
    #                                                            'person_id': partner_person_id,
    #                                                            })['ID']
    # print 'https://balance-26029.admin.branch.greed-dev.paysys.yandex.ru/contract-edit.xml?contract_id='+str(partner_contract_id)

def Create_offer():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')
    contract_id = api.medium().CreateOffer(PASSPORT_ID,
                                 {'client_id': client_id,
                                  'currency': 'RUR',
                                  'firm_id': 1,
                                  'manager_uid': MANAGER.uid,
                                  'payment_term': 10,
                                  'payment_type': 3,
                                  'person_id': person_id,
                                  'personal_account': 1,
                                  'start_dt': datetime.datetime(2017,7,1,0,0,0),
                                  'services': [SERVICE_CONNECT]})['ID']

    # contract_id = api.medium().CreateCommonContract(PASSPORT_ID,
    #                              {'client_id': client_id,
    #                               'currency': 'RUR',
    #                               'firm_id': 1,
    #                               'manager_uid': MANAGER.uid,
    #                               'payment_term': 10,
    #                               'payment_type': 3,
    #                               'person_id': person_id,
    #                               'personal_account': 1,
    #                               'services': [SERVICE_CONNECT]})['ID']

    # print 'https://quark-connect.admin.branch.greed-dev.paysys.yandex.ru/contract-edit.xml?contract_id='+str(contract_id)
    return client_id, person_id, contract_id

def export():
    # steps.CommonSteps.export('OEBS', 'Client', partner_id)
    contract_id = 693518
    steps.CommonSteps.export('OEBS', 'Contract', contract_id)
    import balance.balance_db as db
    collateral_id = db.balance().execute("select id from t_contract_collateral where contract2_id = " + str(contract_id))[0]['id']
    steps.CommonSteps.export('OEBS', 'ContractCollateral', collateral_id)

def create_request():
    begin_dt = datetime.datetime.now()
    service_id = 7
    qty = 50
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')
    client_id = 81975678

    product_id = 503162

    # service_order_id = steps.OrderSteps.next_id(service_id)
    # order_id = steps.OrderSteps.create(client_id, service_order_id, product_id=product_id,
    #                                    service_id=service_id)
    #
    # orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': begin_dt}]
    # steps.RequestSteps.create(client_id, orders_list)

    service_order_id = steps.OrderSteps.next_id(service_id)
    steps.OrderSteps.create(client_id, service_order_id, product_id=product_id,
                            service_id=service_id)

    orders_list = [{'ServiceID':service_id, 'ServiceOrderID': service_order_id, 'Qty': qty,
                    'BeginDT': begin_dt}]
    request_id = steps.RequestSteps.create(client_id, orders_list)
    steps.RequestSteps.create(client_id, orders_list,
                              additional_params={'InvoiceDesireType': 'charge_note', 'InvoiceDesireDT': begin_dt})


# create_request()

def get_country_iso_code(country):
    query = "select iso_code from t_country where region_id = :country"
    country_iso_code = str(db.balance().execute(query, {'country': country})[0]['iso_code'])
    return country_iso_code.rjust(3,'0')

def add_connect_completions(client_id, dt, qty=10, product_id=508507):
    query = "Insert into t_partner_connect_stat (DT,PRODUCT_ID,CLIENT_ID,QTY) values (:dt,:product_id,:client_id,:qty)"
    params = {'client_id': client_id, 'dt': dt, 'qty': qty, 'product_id': product_id}
    db.balance().execute(query, params)


def corp_taxi():
    start_dt_1 = datetime.datetime(2017,1,1,0,0,0)
    dt1 = datetime.datetime(2017,8,7,0,0,0)
    dt2 = datetime.datetime(2017,8,8,0,0,0)
    SERVICE_ID = defaults.taxi_corp()['SERVICE_ID']
    taxi_client_id = steps.ClientSteps.create()
    taxi_person_id = steps.PersonSteps.create(taxi_client_id, 'ur', {'kpp': '234567891'})
    taxi_person_partner_id = steps.PersonSteps.create(taxi_client_id, 'ur', {'is-partner': '1'})

    # создаем клиента и плательщика для корпоративного клиента
    corp_client_id = steps.ClientSteps.create()
    corp_person_id = steps.PersonSteps.create(corp_client_id, 'ur', {'kpp': '234567890'})

    # создаем договоры (коммерческий с таксопарком, расходный с таксопарком и коммерческий с корпоративным клиентом)
    taxi_contract_id, _ = steps.ContractSteps.create_contract('taxi_postpay',
                                                              {'CLIENT_ID': taxi_client_id, 'PERSON_ID': taxi_person_id,
                                                               'DT': start_dt_1})
    query_update_mv = "BEGIN dbms_mview.refresh('BO.MV_PARTNER_TAXI_CONTRACT','C'); END;"
    db.balance().execute(query_update_mv, descr='Обновляем MV_PARTNER_TAXI_CONTRACT')

    taxi_contract_spendable_id, _ = steps.ContractSteps.create_contract('spendable_corp_clients',
                                                                        {'CLIENT_ID': taxi_client_id,
                                                                         'PERSON_ID': taxi_person_partner_id,
                                                                         'DT': start_dt_1,
                                                                         'SERVICES': [SERVICE_ID]})
    corp_contract_id, _ = steps.ContractSteps.create_contract('taxi_corporate',
                                                              {'CLIENT_ID': corp_client_id, 'PERSON_ID': corp_person_id,
                                                               'DT': start_dt_1, 'FINISH_DT': dt2}) #
    corp_contract_id2, _ = steps.ContractSteps.create_contract('taxi_corporate',
                                                              {'CLIENT_ID': corp_client_id, 'PERSON_ID': corp_person_id,
                                                               'DT': dt2})
    steps.SimpleApi.create_fake_thirdparty_payment(ThirdPartyData.TAXI_CORP, taxi_contract_spendable_id,
                                                   taxi_person_partner_id,
                                                   taxi_client_id, '1000',
                                                   None, dt=dt1, client_amount=300,
                                                   client_id=corp_client_id)
    steps.SimpleApi.create_fake_thirdparty_payment(ThirdPartyData.TAXI_CORP, taxi_contract_spendable_id,
                                                   taxi_person_partner_id,
                                                   taxi_client_id, '1000',
                                                   None, dt=datetime.datetime(2017,8,1,0,0,0), client_amount=1000,
                                                   client_id=corp_client_id)
    steps.SimpleApi.create_fake_thirdparty_payment(ThirdPartyData.TAXI_CORP, taxi_contract_spendable_id,
                                                   taxi_person_partner_id,
                                                   taxi_client_id, '1000',
                                                   None, dt=dt2, client_amount=124,
                                                   client_id=corp_client_id)
    steps.SimpleApi.create_fake_thirdparty_payment(ThirdPartyData.TAXI_CORP, taxi_contract_spendable_id,
                                                   taxi_person_partner_id,
                                                   taxi_client_id, '1000',
                                                   None, dt=datetime.datetime(2017,8,31,0,0,0), client_amount=32,
                                                   client_id=corp_client_id)

# corp_taxi()
# test = get_country_iso_code(225)
# print test
# export()
# Create_client_person_contract_taxi()
# create_request()

# steps.CampaignsSteps.do_campaigns(129, '22676293', {})
# steps.CampaignsSteps.do_campaigns(132, '7571', {'Bucks': 666.666667}, campaigns_dt=datetime.datetime(2019,5,2))
# steps.ActsSteps.generate(8159846, force=1, date=datetime.datetime(2019,5,3))

# api.medium().GetPartnerBalance(Services.TAXI_CORP.id, [540122])

# postpay
# api.medium().GetTaxiBalance([940959])

# client_id = steps.ClientSteps.create()
# steps.PersonSteps.create(client_id, 'ur', {'is-partner': '1'})

#prepay
# api.medium().GetTaxiBalance([625270])

# api.medium().GetTaxiBalance([625270])
# api.medium().GetPartnerBalance(135, [625267])
# api.test_balance().CacheBalance(625289)


# client_id, person_id, contract_id = Create_offer()
# add_connect_completions(client_id, datetime.datetime(2017,7,1,0,0,0), qty=10, product_id=508507)
# contract_id = 701095
# steps.CommonPartnerSteps.generate_partner_acts_fair(950301, datetime.datetime(2018,7,1,0,0,0))
#

# steps.TaxiSteps.generate_acts_prepay(281150, datetime.datetime(2018,2,1,0,0,0))

# api.medium().GetClientActs('connect_c3efa38f7aa8b2b6bc7ed282cdf135ff', {'ClientID': 55787383})
# api.medium().GetClientContracts({'ClientID': 106713537, 'ContractType': 'SPENDABLE'})
# api.medium().GetPartnerContracts({'ClientID': '42442417'})
# api.medium().GetClientContracts('400000002626')
# api.medium().GetClientContracts({'ClientID': 56052428, 'Dt': datetime.datetime(2017,9,30), 'Signed': 1})


# client_id = steps.ClientSteps.create()
# person_id = steps.PersonSteps.create(client_id, 'ur')
# steps.CommonSteps.export('OEBS', 'Client', 55876713)
# steps.CommonSteps.export('THIRDPARTY_TRANS', 'Payment', 1747984046)
# print steps.PartnerSteps.get_spendable_contract_type_by_service_id([137])

# steps.CommonPartnerSteps.generate_partner_acts_fair(253226, datetime.datetime(2017,11,1,0,0,0))
# steps.CommonPartnerSteps.generate_partner_acts_fair(426805, datetime.datetime(2017,10,1,0,0,0))

# api.medium().GetDspStat(datetime.datetime(2017, 9, 8, 0, 0), datetime.datetime(2017, 9, 8, 0, 0), 1, 1)
# api.medium().GetDspStat(datetime.datetime(2017, 9, 8, 0, 0), datetime.datetime(2017, 9, 8, 0, 0), 1, 1)
# api.test_balance().OEBSPayment(75755790, 100)

# api.medium().UpdatePayment({'TrustPaymentID': '5cd03235910d397850fe1c6f'}, {'PayoutReady': 1})
# SERVICE = Services.MEDICINE_PAY
# purchase_token = 'a29df590a3e43de55573910e63e5b8c5'
# payments_api_steps.Payments.clear(service=SERVICE, user=simpleapi_defaults.USER_NEW_API,
#                                purchase_token=purchase_token)
# steps.CommonSteps.export('THIRDPARTY_TRANS', 'Payment', 1747965115)
# steps.SimpleApi.create_register(334485539, 5000.1, 1)

# print Decimal('33') - Decimal('0')


# steps.CommonSteps.get_pickled_value("select input from t_export where type = 'PREPAY_ACTS' and classname = 'Contract' and object_id = 359752")

# import pickle
# value = 'Y2NvcHlfcmVnCl9yZWNvbnN0cnVjdG9yCnAwCihjYmFsYW5jZS5tYXBwZXIuYWN0cwpBY3RNb250aApwMQpjX19idWlsdGluX18Kb2JqZWN0CnAyCk50cDMKUnA0CihkcDUKUydfZm9yX21vbnRoJwpwNgpjZGF0ZXRpbWUKZGF0ZXRpbWUKcDcKKFMnXHgwN1x4ZTFceDA4XHgwMVx4MDBceDAwXHgwMFx4MDBceDAwXHgwMCcKcDgKdHA5ClJwMTAKc2Iu'
# pickle.loads(value.decode('base64'))

# query = "select utl_encode.base64_decode(UTL_ENCODE.BASE64_ENCODE(input)) as key1 from t_export where type = 'PREPAY_ACTS' and classname = 'Contract' and object_id = 359752"
# value = db.balance().execute(query)[0]['key1']
# pickle.loads(value)
# steps.CommonSteps.export('PREPAY_ACTS', 'Contract', 401245)

# print steps.CommonSteps.get_pickled_value("select input from t_export where type = 'MONTH_PROC' and classname = 'Client' and object_id = 2549520")
# print steps.CommonSteps.get_pickled_value("select input from t_export where type = 'PREPAY_ACTS' and classname = 'Contract' and object_id = 359752")

# contract_id = 412303
# dt = datetime.datetime(2017,8,1,0,0,0)
# api.test_balance().EnqueuePrepayActs(dt, contract_id)

# steps.CommonSteps.export('PREPAY_ACTS', 'Contract', contract_id)

# steps.SimpleApi.create_register(334243188, Decimal('50'))
# steps.CommonSteps.export('THIRDPARTY_TRANS', 'Payment', 334485535)

# steps.CommonSteps.export('OEBS', 'Client', 57463469)
# steps.CommonSteps.export('OEBS', 'Person', 7001832)

# steps.ClientSteps.create()

# query_update_mv = "BEGIN dbms_mview.refresh('T_PERSON','F'); END;"
# db.meta().execute("select * from v_agent_report where contract_id = 554105")
# steps.CommonSteps.export('OEBS', 'Client', 103917934)
# steps.CommonSteps.export('OEBS', 'Contract', 630082)
# steps.CommonSteps.export('OEBS', 'ContractCollateral', 889946)

# db.meta().execute("select * from t_person where id = 8479015")
# db.meta().execute("BEGIN dbms_mview.refresh('T_PERSON','F'); END;")
# steps.CommonSteps.export('OEBS', 'ContractCollateral', 612658)
# steps.CommonSteps.export('OEBS', 'Manager', 20453)
# steps.CommonSteps.export('OEBS', 'Invoice', 90005670)
# steps.CommonSteps.export('OEBS', 'Act', 87341734)


# steps.CommonSteps.export('OEBS', 'Product', '60509007')
# steps.CommonSteps.export('OEBS', 'ThirdPartyTransaction# steps.CommonSteps.export('OEBS', 'ThirdPartyTransaction', '999999985481796')', '999999985481796')
# steps.CommonSteps.export('OEBS', 'ThirdPartyTransaction', '999999996483847')

from btestlib.constants import Export
# steps.ExportSteps.create_export_record('20003324121', Export.Classname.TRANSACTION, Export.Type.OEBS)
# steps.CommonSteps.export('OEBS', 'ThirdPartyTransaction', '20003324121')
# steps.CommonSteps.export('MONTH_PROC', 'Client', '83778587')
# steps.CommonSteps.export('THIRDPARTY_TRANS', 'Payment', '603861805')

# steps.CommonPartnerSteps.generate_partner_acts_fair(553060, datetime.datetime(2018,8,1,0,0,0))


# steps.CommonSteps.export('OEBS', 'ThirdPartyTransaction', '999999981514256')

person_list = [8350169,
8356391,
8356430,
8356435,
8356437,
8356438,
8356440,
8356444,
8356445,
8356450,
8356451,
8356452,
8356453,
8356486,
8356487,
8356488,
8356489,
8356490,
8356491,
8356498,
8356499]

# for i in person_list:
#     steps.CommonSteps.export('OEBS', 'Person', i)

#
# api.test_balance().SyncRevPartnerServices()

# steps.DistributionSteps.create_distr_tag(30430321, PASSPORT_ID)
# source = 'distr_pages'
# start_dt = datetime.datetime(2018,11,18)
# end_dt = datetime.datetime(2018,11,18)
# api.test_balance().GetPartnerCompletions({'start_dt': start_dt, 'end_dt': end_dt, 'completion_source': source})

# client_id = steps.ClientSteps.create()
# person = steps.PersonSteps.create(client_id, 'ur')
# person_id = api.medium().CreatePerson(PASSPORT_ID, {"bik" : "044525700",
#       "postaddress" : "115478, Москва г, Москворечье ул, 1, 1",
#       "legal-address-region" : "Москва г",
#       "postcode" : "115478",
#       "resources" : "мои ресурсы",
#       "address" : "115478, Москва г, Москворечье ул, 1, 1",
#       "legal-address-home" : "1",
#       "address-region" : "Москва г",
#       "address-home" : "1",
#       "operator_uid" : '121154534',
#       "legal-address-postcode" : "115478",
#       "legaladdress" : "115478, Москва г, Москворечье ул, 1, 1",
#       "account" : "40817810501000830304",
#       "address-postcode" : "115478",
#       "legal-address-street" : "Москворечье ул",
#       "client_id": client_id,
#       "legal-address-gni" : "7724",
#       "phone" : "+79160915478",
#       "type" : "ph",
#       "MEMO" : "",
#       "email" : "aa@aa.ru",
#       "legal-address-code" : "770000000000509",
#       "address-gni" : "7724",
#       "address-street" : "Москворечье ул",
#       "address-code" : "770000000000509"})
#
# person_id = api.medium().CreatePerson(121154534, {'legal-address-gni': '7724',
#                                       'postcode': '115478',
#                                       'address-gni': '7724',
#                                       'address-region': u'\u041c\u043e\u0441\u043a\u0432\u0430 \u0433',
#                                       'is-partner': '1',
#                                       'address-postcode': '115478',
#                                       'address-street': u'\u041c\u043e\u0441\u043a\u0432\u043e\u0440\u0435\u0447\u044c\u0435 \u0443\u043b',
#                                       'person_id': -1,
#                                       'type': 'ph',
#                                       'email': 'aa@aa.ru',
#                                       'resources': u'\u043c\u043e\u0438 \u0440\u0435\u0441\u0443\u0440\u0441\u044b',
#                                       'legal-address-street': u'\u041c\u043e\u0441\u043a\u0432\u043e\u0440\u0435\u0447\u044c\u0435 \u0443\u043b',
#                                       'bik': '044525700',
#                                       'MEMO': '',
#                                       'phone': '+79160915478',
#                                       'legal-address-code': '770000000000509',
#                                       'client_id': client_id,
#                                       'address': u'115478, \u041c\u043e\u0441\u043a\u0432\u0430 \u0433, \u041c\u043e\u0441\u043a\u0432\u043e\u0440\u0435\u0447\u044c\u0435 \u0443\u043b, 1, 1',
#                                       'address-code': '770000000000509',
#                                       'address-home': '1',
#                                       'account': '40817810501000830304',
#                                       'legal-address-home': '1',
#                                       'legal-address-postcode': '115478',
#                                       'legal-address-region': u'\u041c\u043e\u0441\u043a\u0432\u0430 \u0433',
#                                       # 'legaladdress': u'115478, \u041c\u043e\u0441\u043a\u0432\u0430 \u0433, \u041c\u043e\u0441\u043a\u0432\u043e\u0440\u0435\u0447\u044c\u0435 \u0443\u043b, 1, 1',
#                                       'legaladdress': '115478, Москва г, Москворечье ул, 1, 1',
#                                       'postaddress': u'115478, \u041c\u043e\u0441\u043a\u0432\u0430 \u0433, \u041c\u043e\u0441\u043a\u0432\u043e\u0440\u0435\u0447\u044c\u0435 \u0443\u043b, 1, 1'})


# steps.TaxiSteps.generate_acts_prepay(56220512, 407680, datetime.datetime(2017,8,1,0,0,0))

# steps.InvoiceSteps.pay(84378938, 100000)

def create_telemed():
    SERVICE_ID_AR = Services.MEDICINE_PAY.id  # за АВ
    SERVICE_ID_SERVICES = Services.TELEMEDICINE2.id
    client_id = steps.ClientSteps.create({'Name': 'Client test telemedicine'})
    person_id = steps.PersonSteps.create(client_id, 'ur')
    person_id_partner = steps.PersonSteps.create(client_id, 'ur', {'is-partner': '1'})
    # создаем договор
    contract_id, _ = steps.ContractSteps.create_contract('telemedicine',
                                                         {'CLIENT_ID': client_id, 'PERSON_ID': person_id,
                                                          'DT': datetime.datetime(2017,1,1,0,0,0),
                                                          'MEDICINE_PAY_COMMISSION': Decimal('30'),
                                                          'MEDICINE_PAY_COMMISSION2': Decimal('40'),
                                                          'SERVICES': [SERVICE_ID_SERVICES, SERVICE_ID_AR]})

# create_telemed()



# client_id = steps.ClientSteps.create()
# person_id = steps.PersonSteps.create(client_id, 'kzu')

# steps.CommonSteps.export('OEBS', 'Person', 5761669)
# query = "INSERT INTO T_OEBS_CASH_PAYMENT_FACT" \
#                 "(XXAR_CASH_FACT_ID, AMOUNT, RECEIPT_NUMBER, RECEIPT_DATE, OPERATION_TYPE, " \
#                 "LAST_UPDATED_BY, LAST_UPDATE_DATE, CREATED_BY, CREATION_DATE) VALUES " \
#                 "(S_OEBS_CASH_PAYMENT_FACT_TEST.nextval, :amount, :invoice_eid, :dt, :type, " \
#                 "-1, :dt, -1, :dt)"
# params = {
#             'invoice_eid': 'ЛСТ-425278927-1',
#             'amount': 1000,
#             'dt': datetime.datetime(2017,10,3,0,0,0),
#             'type': 'INSERT'
#         }
# db.balance().execute(query, params)

# steps.TaxiSteps.process_taxi(826684, datetime.datetime(2018,12,4,0,0,0))

# client_id = steps.ClientSteps.create()
# steps.PersonSteps.create(client_id, 'ur')

# steps.PersonSteps.create(56023178, 'ur')
# steps.PersonSteps.create(client_id, 'ur', {'is-partner': '1'})

def agaagga():
    from simpleapi.common.payment_methods import Coupon
    SERVICE = Services.TELEMEDICINE_DONATE
    trust_payment_id, payment_id, purchase_token = \
        steps.SimpleNewApi.create_payment(SERVICE, "2044576307887", paymethod=Coupon(), is_register_needed=0)
    steps.CommonPartnerSteps.export_payment(payment_id)


def ffff():
    SERVICE = Services.MEDICINE_PAY
    SERVICE2 = Services.TELEMEDICINE2
    partner_id = 56300599
    product_id = steps.SimpleNewApi.create_product(SERVICE, partner_id)
    trust_payment_id, payment_id, purchase_token = steps.SimpleNewApi.create_payment(SERVICE, product_id)
    steps.CommonPartnerSteps.export_payment(payment_id)


# api.medium().GetDspStat(datetime.datetime(2017, 10, 9, 0, 0), datetime.datetime(2017, 10, 9, 0, 0), 0, None, 0, {'place_id': 11222133})
# api.medium().GetPagesTagsStat(datetime.datetime(2018, 3, 31, 0, 0), datetime.datetime(2018, 3, 31, 0, 0))

# SERVICE_DONATE = Services.BUSES_DONATE
SERVICE_AUTOBUS = Services.BUSES_2
# SERVICE_TELEMED = Services.TELEMEDICINE_DONATE
# SERVICE_TAXI_DONATE = Services.TAXI_DONATE
# partner_id = steps.SimpleApi.create_partner(SERVICE_AUTOBUS)
partner_id = 103917878
# partner_id = 103917934
# partner_id_with_contract = 56370252
# donate:
# service_product_id = '1509456231505-XJ9QAEWM1Y-37599'

# payment:
# service_product_id = '2047088829297'
# igogor это больше не работает. Можно добавить в тест фикстуру switch_to_pg.
# Если вне теста, то можно руками дернуть
# environments.SimpleapiEnvironment.swith_param(dbname=environments.TrustDbNames.BS_PG,
#                                               xmlrpc_url=environments.TrustApiUrls.XMLRPC_PG)
# SIMPLE_VERSION = balance_config.SIMPLE_PG

# service_product_id = steps.SimpleApi.create_service_product(SERVICE_AUTOBUS, partner_id)
# steps.PersonSteps.create(partner_id, 'ur')
# steps.PersonSteps.create(partner_id, 'ur', {'is-partner': '1'})
# service_order_id, trust_payment_id, purchase_token, payment_id = steps.SimpleApi.create_trust_payment(
#         SERVICE_AUTOBUS,
#         service_product_id,
#     currency=Currencies.BYN,)
#             paymethod=Compensation())
    # price = Decimal('456'),
    # user=simpleapi_defaults.USER_ANONYMOUS,

# service_order_id = '96843888'
# trust_payment_id = '5bae1f25910d39229216c03e'
# steps.CommonPartnerSteps.export_payment(payment_id)
# trust_refund_id, refund_id = steps.SimpleApi.create_refund(SERVICE_AUTOBUS,
#                                                            service_order_id, trust_payment_id)



# api.simple().CheckBasket('taxi_corp_adfb605b109d525fceca9590b0c7d7e9', {
#     'trust_payment_id': '5cee9672910d39502ad9a407',
#     'uid': '4003098409',
    # 'user_ip': '::ffff:86.57.167.10'
# })

# api.simple().DoRefund('buses_pay_26643c0072e9f3665a8e33fa04c1c905', {'trust_refund_id': '5bae1f95910d394d6316c021', 'user_ip': '::ffff:86.57.167.10'})


# client_id = steps.ClientSteps.create()
# person_id = steps.PersonSteps.create(client_id, 'eu_yt')
# tag_id = steps.DistributionSteps.create_distr_tag(client_id, PASSPORT_ID)
# api.test_balance().GetPartnerCompletions({'start_dt': datetime.datetime(2017,11,1,0,0,0), 'end_dt': datetime.datetime(2017,11,9,0,0,0), 'completion_source': 'zen'})

# client_id = steps.ClientSteps.create()
# steps.PersonSteps.create(client_id, 'ur', {'is-partner': '1', 'signer-person-gender': 'M'})

# api.medium().GetPartnerBalance(143, [493701])
# api.medium().CreatePerson(PASSPORT_ID, {
#     "signer-person-gender": "M",
#     "ogrn": "317507400044089",
#     "inn": "504809885809",
#     "street": "Весенняя",
#     "postcode": "142300",
#     "signer-position-name": "Индивидуальный предприниматель",
#     "city": "Московская область, Чеховский район,город Чехов",
#     "authority-doc-type": "Свидетельство о регистрации",
#     "type": "ur",
#     "email": "agevorkoff@yandex.ru",
#     "signer-person-name": "Геворков Артем Ашотович",
#     "bik": "044525974",
#     "phone": "+79262551927",
#     "representative": "Геворков Артем Ашотович",
#     "client_id": client_id,
#     "LIVE_SIGNATURE": "1",
#     "kpp": "",
#     "account": "40802810800000288889",
#     "name": "Индивидуальный предприниматель Геворков Артем Ашотович",
#     "longname": "Индивидуальный предприниматель Геворков Артем Ашотович",
#     "legaladdress": "142300,Московская область, Чеховский район,город Чехов,Весенняя,дом 30, квартира 17",
#     "postaddress": "142300,Московская область, Чеховский район,город Чехов,Весенняя,дом 30, квартира 17",
#     "postsuffix": "дом 30, квартира 17",
#     'fias_guid': '33d3bfb7-de99-4840-9798-219057123a9f'
# })

# CORP_DATA = defaults.GeneralPartnerContractDefaults.YANDEX_TAXI_CORP
# steps.CommonPartnerSteps.create_gerenal_partner_person_and_contract(client_id,
#                                                                             CORP_DATA,
#                                                                             additional_params={
#                                                                                                'ATYPICAL_CONDITIONS': 1})


# api.medium().GetPagesStat(datetime.datetime(2017,10,1),datetime.datetime(2017,10,1))

# client_id = steps.ClientSteps.create({'IS_AGENCY': 0, 'NAME': u'Test AG RSYA'})
# steps.ClientSteps.set_client_partner_type(client_id)
# person_id = steps.PersonSteps.create(client_id, 'ur', {'is-partner': '1'})


def data_by_page(page_id = '11279944'):
    completion_dt = datetime.datetime(2017, 10, 1, 0, 0, 0)
    data = api.medium().GetPagesStat(completion_dt,completion_dt)
    splitted = data.split('\n')
    header = tuple(splitted[0].split('\t'))
    filtered = []
    for line in splitted[1:]:
        row = tuple(line.split('\t'))
        if row[0] == page_id:
            filtered.append(dict(zip(header,row)))
    return filtered


#168	Армения
#159	Казахстан
#149	Беларусь
#169	Грузия
#187	Украина
#208	Молдова
#207	Киргизия
#94	Бразилия


#508161 USD    2201041
#508162 EUR    2201039
def create_contract_request():
    # currency = Currencies.RUB.iso_code
    # region_id = 168
    product_id = 508686
    paysys_id = 1003
    begin_dt = datetime.datetime(2018, 3, 1, 0, 0, 0)
    service_id = 35
    qty = 50
    client_id = steps.ClientSteps.create()
    # person_id = steps.PersonSteps.create(client_id, 'ur')
    # client_id = 60752653
    # contract_id = api.medium().CreateCommonContract(PASSPORT_ID,
    #                                      {'advance_payment_sum': Decimal('1000'),
    #                                       'client_id': client_id,
    #                                       'country': region_id,
    #                                       'currency': currency,
    #                                       'firm_id': 22,
    #                                       'manager_uid': '244916211',
    #                                       'partner_commission_pct2': Decimal('0'),
    #                                       'payment_type': 2,
    #                                       'person_id': person_id,
    #                                       'personal_account': 1,
    #                                       'region': None,
    #                                       'services': [128, 124, 111],
    #                                       'signed': 1})['ID']
    service_order_id = steps.OrderSteps.next_id(service_id)
    order_id = steps.OrderSteps.create(client_id, service_order_id, product_id=product_id,
                                       service_id=service_id)
    orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': begin_dt}]
    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params={'FirmID': 1})
    # request_id = steps.RequestSteps.create(client_id, orders_list, additional_params={'FirmID': 1})
    # invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, paysys_id, credit=0, contract_id=contract_id,
    #                                          overdraft=0, endbuyer_id=None)

# create_contract_request()

# create_contract_request()

# data_by_page()

# SERVICE_BUSES2 = Services.BUSES_2
# trust_refund_id, refund_id = steps.SimpleApi.create_refund(SERVICE_BUSES2, '1915234247', '5a05a4c3b9256c2eeed053be')
# steps.CommonPartnerSteps.export_payment(refund_id)


# api.test_balance().GeneratePartnerAct(16775538, datetime.datetime(2019, 12, 1, 0, 0))
# api.test_balance().ExportObject('MONTH_PROC', 'Client', 107467023, 0, None, None)

# client_id = steps.ClientSteps.create()
# client_id1 = steps.ClientSteps.create()
# person_id = steps.PersonSteps.create(client_id1, 'ur', {'is-partner': '1'})
# person_id = steps.PersonSteps.create(client_id, 'eu_yt')
# api.medium().CreateCommonContract(PASSPORT_ID,
#                                                   {'client_id': client_id,
#                                                    'country': 225,
#                                                    'ctype': 'SPENDABLE',
#                                                    'currency': 'RUR',
#                                                    'firm_id': 13,
#                                                    'is_offer': 0,
#                                                    'link_contract_id': 645817,
#                                                    'manager_uid': '244916211',
#                                                    'nds': 18,
#                                                    'person_id': person_id,
#                                                    'region': '02000001000',
#                                                    'services': [135]})


# client_id = steps.ClientSteps.create()
# person_id = steps.PersonSteps.create(client_id, 'ur', {'is-partner': '1'})
# person_id = steps.PersonSteps.create(client_id, 'ur')
# steps.CommonPartnerSteps.generate_partner_acts_fair(868796, datetime.datetime(2017,12,1,0,0,0))

# api.medium().CreateCommonContract(PASSPORT_ID,
#                                          {'client_id': client_id,
#                                           'currency': 'RUR',
#                                           'firm_id': 1,
#                                           'manager_uid': '244916211',
#                                           'nds': 18,
#                                           'person_id': person_id,
#                                           'services': [280]})

# api.test_balance().GetHost()
# api.test_balance().GeneratePartnerAct(788975, datetime.datetime(2017, 12, 31, 0, 0))
# steps.ActsSteps.hide(87974347)

# steps.CommonPartnerSteps.generate_partner_acts_fair(868796, datetime.datetime(2017, 12, 31, 0, 0))
# steps.ActsSteps.unhide(71472371)
# steps.ActsSteps.unhide(71476488)
# steps.ActsSteps.unhide(71476658)
# steps.ActsSteps.unhide(71476660)
# steps.ActsSteps.unhide(71476661)
# steps.ActsSteps.unhide(71476659)


# api.medium().GetDspStat(datetime.datetime(2017, 12, 12, 0, 0),
#                         datetime.datetime(2017, 12, 12, 0, 0), 1, 0, 0,
#                         {'place_id': 11376436})

# api.medium().GetPartnerBalance(135, [728366])

# steps.TaxiSteps.process_taxi(694910)

# steps.InvoiceSteps.pay(94050371, 9500)

# client_id = steps.ClientSteps.create()
# steps.PersonSteps.create(client_id, 'kzu')
# steps.PersonSteps.create(client_id, 'ph', {'is-partner': '1'}, inn_type=1)
# create_request()
# steps.CommonSteps.export('OEBS', 'Client', client_id)

# SERVICE_AERO = Services.AEROEXPRESS
# partner_id = steps.SimpleApi.create_partner(SERVICE_AERO)
# service_product_id = steps.SimpleApi.create_service_product(SERVICE_AERO, partner_id=partner_id)
# service_order_id_list, trust_payment_id, purchase_token, payment_id = \
#     steps.SimpleApi.create_trust_payment(SERVICE_AERO, service_product_id)


def create_act_70():
    product_id = 1202
    product_id2 = 2184
    # product_id = 508333
    # product_id2 = 508334
    paysys_id = 1003
    # paysys_id = 1047 #(USD)
    # paysys_id = 1046 #EUR
    # paysys_id = 1050 #TRY
    begin_dt = datetime.datetime(2017, 12, 25, 0, 0, 0)
    service_id = 70
    qty = 7000
    qty2 = 5800
    # client_id = steps.ClientSteps.create()
    client_id = 81665372
    # person_id = steps.PersonSteps.create(client_id, 'ur') #sw_yt  tru
    person_id = 6655044
    service_order_id = steps.OrderSteps.next_id(service_id)
    order_id = steps.OrderSteps.create(client_id, service_order_id, product_id=product_id,
                                       service_id=service_id)
    service_order_id2 = steps.OrderSteps.next_id(service_id)
    order_id = steps.OrderSteps.create(client_id, service_order_id2, product_id=product_id2,
                                       service_id=service_id)
    service_order_id3 = steps.OrderSteps.next_id(service_id)

    # service_order_id2 = steps.OrderSteps.next_id(service_id)
    # order_id2 = steps.OrderSteps.create(client_id, service_order_id2, product_id=product_id2,
    #                                    service_id=service_id)
    orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': begin_dt},
                   {'ServiceID': service_id, 'ServiceOrderID': service_order_id2, 'Qty': qty, 'BeginDT': begin_dt},
                   {'ServiceID': service_id, 'ServiceOrderID': service_order_id3, 'Qty': qty, 'BeginDT': begin_dt},
                   {'ServiceID': service_id, 'ServiceOrderID': service_order_id4, 'Qty': qty, 'BeginDT': begin_dt},]

    # orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty, 'BeginDT': begin_dt},]
    #                {'ServiceID': service_id, 'ServiceOrderID': service_order_id2, 'Qty': qty2, 'BeginDT': begin_dt}]
    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params={'InvoiceDesireDT': begin_dt})
    # invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, paysys_id, credit=0,
    #                                          overdraft=0, endbuyer_id=None)
    # steps.InvoiceSteps.pay(invoice_id)
    # steps.CampaignsSteps.do_campaigns(7, 135767948, {'Bucks': 50}, 0, begin_dt)
    # steps.CampaignsSteps.do_campaigns(service_id, service_order_id2, {'Shows': qty2}, 0, begin_dt)
    # steps.ActsSteps.generate(client_id, force=1, date=begin_dt)
    # steps.ActsSteps.generate(client_id, force=0, date=begin_dt)
    pass
# steps.CampaignsSteps.do_campaigns(7, 135767948, {'Bucks': 50}, 0, datetime.datetime.now())
# steps.ActsSteps.generate(81278072, force=0, date=datetime.datetime.now())
# create_act_70()

# steps.InvoiceSteps.pay(70301325, 3500)
# steps.CampaignsSteps.do_campaigns(103, 22773359, {'Units': 50}, 0, datetime.datetime.now())
# steps.ActsSteps.generate(60714084, force=1, date=datetime.datetime.now())

# create_act_70()
# api.medium().GetCurrencyRate('USD', datetime.datetime(2018, 1, 14, 0, 0), 1005, 'AMD')

# api.medium().GetCurrencyRate('USD', datetime.datetime(2018, 1, 1, 0, 0), 1007, 'KZT')
# steps.CampaignsSteps.do_campaigns(35, '20000000707202', {'Bucks': 1}, 0, datetime.datetime.now())
# steps.ActsSteps.generate(60566453, force=1, date=datetime.datetime.now())
SERVICE=Services.TAXI
# client_id, service_product_id = steps.SimpleApi.create_partner_and_product(SERVICE)
# person_id = steps.PersonSteps.create(client_id, 'eu_yt')
# params = {'CLIENT_ID': client_id, 'PERSON_ID': person_id,
#           'DT': datetime.datetime(2017,1,1), 'COUNTRY': 208,
#           'FIRM': 22, 'SERVICES': [124, 111, 128]
#           }
# contract_id, external_id = steps.ContractSteps.create_contract('taxi_postpay_sng', params)

service_product_id = '1516271483104-347Y70PCS2-2554'
client_id = 57463469
yesterday = datetime.datetime.now() - datetime.timedelta(days=1)
# service_order_id, trust_payment_id, purchase_token, payment_id = \
#     steps.SimpleApi.create_trust_payment(SERVICE,
#                                          service_product_id,
#                                          currency=Currencies.MDL,
#                                             order_dt=utils.Date.moscow_offset_dt(yesterday)
#                                          )



# обрабатываем платеж
# steps.CommonPartnerSteps.export_payment(334624524)

# api.medium().GetTaxiBalance([826684])
# steps.TaxiSteps.pay_to_personal_account(2997000, 380622)
# steps.TaxiSteps.process_taxi(380021)

# api.test_balance().ExportObject('PROCESS_TAXI', 'Contract', 876745, 0, {'on_dt': datetime.datetime(2017, 12, 31, 0, 0)}, None)
client_id = 57528026
contract_id = 888959
card_childchair_amount = Decimal('23.4')
cash_childchair_amount = Decimal('1.4')
corporate_childchair_amount = Decimal('12.3')
# steps.TaxiSteps.create_orders(card_childchair_amount, cash_childchair_amount, corporate_childchair_amount,
#                                   client_id, datetime.datetime(2017,12,1), order_type=TaxiOrderType.childchair)
# api.medium().GetTaxiBalance([897230])
# steps.TaxiSteps.pay_to_personal_account(500, contract_id)
# steps.TaxiSteps.generate_acts_prepay(contract_id, datetime.datetime(2017,12,31))
# steps.TaxiSteps.process_taxi(884719, datetime.datetime(2017,12,31))
# steps.TaxiSteps.generate_acts_postpay(client_id, contract_id, datetime.datetime(2017,12,31))


# steps.CommonPartnerSteps.generate_partner_acts_fair(868796, datetime.datetime(2017,12,31))

# api.test_balance().CacheBalance(449130)


# api.medium().GetPartnerBalance(135, [449130])
# api.medium().GetPartnerBalance(135, [920523])

# steps.CommonPartnerSteps.generate_partner_acts_fair(383540, datetime.datetime(2018,3,1))


d = Decimal('298.658')
def round00(value, round=decimal.ROUND_HALF_UP):
    if not isinstance(value, decimal.Decimal):
        value = decimal.Decimal(value)
    return value.quantize(decimal.Decimal('0.01'), round)


# print round00(d)
#
# print Decimal(d.quantize(Decimal('.01'), rounding=decimal.ROUND_HALF_UP))
# api.test_balance().ExportObject('PROCESS_TAXI', 'Contract', 893365, 0, {'on_dt': datetime.datetime(2018, 1, 1, 0, 0, 1)}, None)
# steps.TaxiSteps.process_taxi(897014, datetime.datetime(2018,1,2,0,0,1))
# api.medium().GetPartnerContracts({'ClientID': 1323258})

client_id = 57537278 #57527774
# steps.TaxiSteps.create_order(client_id, datetime.datetime(2017,12,1), PaymentType.CASH,
#                                    commission_sum=Decimal('20'),
#                                    currency=Currencies.RUB.iso_code, order_type=TaxiOrderType.commission)
# steps.TaxiSteps.create_order(client_id, datetime.datetime(2017,12,1), PaymentType.CARD,
#                                    commission_sum=Decimal('4'),
#                                    currency=Currencies.RUB.iso_code, order_type=TaxiOrderType.commission)
# steps.TaxiSteps.create_order(client_id, datetime.datetime(2017,12,1), PaymentType.CASH,
#                                    commission_sum=Decimal('30'),
#                                    currency=Currencies.RUB.iso_code, order_type=TaxiOrderType.childchair)
# api.test_balance().ExportObject('PROCESS_TAXI', 'Contract', 897203, 0, {'on_dt': datetime.datetime(2018, 1, 2, 0, 0)}, None)
# api.test_balance().GeneratePartnerAct(266744, datetime.datetime(2018, 1, 31, 0, 0))
# api.test_balance().GeneratePartnerAct(897219, datetime.datetime(2017, 12, 31, 0, 0))
# steps.TaxiSteps.generate_acts_prepay(270797, datetime.datetime(2018, 1, 31, 0, 0))
# steps.CommonSteps.export('MONTH_PROC', 'Client', '57537278')

# steps.ClientSteps.create()
# api.medium().QueryCatalog(['V_DISTR_CONTRACT_REVSHARE_PROD'], "V_DISTR_CONTRACT_REVSHARE_PROD.contract_id = 750267")
# api.medium().QueryCatalog(['t_scale_points'], "t_scale_points.namespace='distribution'")

# print steps.TaxiSteps.get_taxi_products_by_currency(Currencies.RUB)
# steps.TaxiSteps.get_completions_from_view(974865)

# db.balance().execute("BEGIN dbms_mview.refresh('BO.MV_PARTNER_TAXI_CONTRACT','C'); END;")
# db.balance_bs().execute("select * from t_payment where id = 750000000")

# api.medium().GetCurrencyRate(Currencies.RUB.iso_code, datetime.datetime(2018,1,1,0,0,0),
#                              1000, Currencies.RUB.iso_code, 1)


# steps.CommonPartnerSteps.generate_partner_acts_fair(567636, datetime.datetime(2018,4,1,0,0,0))



#---------------------создание договора, платежа, акта со страной ТАКСИ
from btestlib import utils

currency = Currencies.EUR #Currencies.USD
payment_currency = Currencies.EUR #Currencies.UZS
country = 117
# client_id, service_product_id = steps.SimpleApi.create_partner_and_product(SERVICE)
# person_id = steps.PersonSteps.create(client_id, 'eu_yt')
# params = {'CLIENT_ID': client_id, 'PERSON_ID': person_id,
#           'DT': datetime.datetime(2018,1,1,0,0,0), 'CURRENCY': currency.num_code,
#           'PARTNER_COMMISSION_PCT2': 10, 'COUNTRY': country}
# contract_id, external_id = steps.ContractSteps.create_contract('taxi_postpay_sng', params)
# # db.balance().execute("BEGIN dbms_mview.refresh('BO.MV_PARTNER_TAXI_CONTRACT','C'); END;")
# service_order_id, trust_payment_id, purchase_token, payment_id = \
# steps.SimpleApi.create_trust_payment(SERVICE,
#                                          service_product_id,
#                                          currency=payment_currency,
#                                          order_dt=utils.Date.moscow_offset_dt(datetime.datetime.now()),)
                                     # fiscal_nds=CMNds.NDS_18)
# steps.CommonPartnerSteps.export_payment(payment_id)
# db.balance().execute("update t_thirdparty_transactions set dt = date'2018-06-01' where payment_id = " + str(payment_id))
# steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, datetime.datetime(2018,6,1))
# steps.CommonSteps.export('MONTH_PROC', 'Client', client_id)

# steps.CommonSteps.export('OEBS', 'Client', 82772283)
# steps.CommonSteps.export('OEBS', 'Contract', 561269)
# steps.CommonSteps.export('OEBS', 'ContractCollateral', 812456)
# steps.CommonSteps.export('OEBS', 'ThirdPartyTransaction', '18616264609')
# steps.CommonSteps.export('OEBS', 'Act', 87060094)
# steps.CommonSteps.export('THIRDPARTY_TRANS', 'Payment', 1748263232)

# client_id = steps.ClientSteps.create()
# person_id = steps.PersonSteps.create(client_id, 'byu', {'is-partner': '1'})


# partner_id_ooo = 81133181
# partner_id = 81149392 #kazakhstan

# SERVICE = Services.UBER
# currency = Currencies.KZT
# partner_id = steps.SimpleApi.create_partner(SERVICE)

# service_product_id_uber = '1522763860304-5Z8SH4O2EQ-12104'
# service_product_id_kzt_taxi = '1522765157598-ZPZ0EGLP3P-13003'
# service_product_id_kzt_uber='1522767224910-67ALPFBOZT-21655'
# service_product_id_kzt_uber_roaming='1522767398020-C8WKVTDB6O-21767'
# service_product_id = steps.SimpleApi.create_service_product(SERVICE, partner_id_ooo)

# service_product_id = '1522851769380-EPIED6P87D-46566'

# # создаем платеж
# service_order_id, trust_payment_id, purchase_token, payment_id = \
#     steps.SimpleApi.create_trust_payment(SERVICE, service_product_id,
#                                          order_dt=utils.Date.moscow_offset_dt(),
#                                          # price=Decimal('150'),
#                                          # paymethod = Compensation(),
#                                          # currency=currency
#                                          )
# # обрабатываем платеж
# steps.CommonPartnerSteps.export_payment(payment_id)
#
# # создаем рефанд для этого платежа
# trust_refund_id, refund_id = steps.SimpleApi.create_refund(SERVICE, service_order_id, trust_payment_id)
# # обрабатываем рефанд
# steps.CommonPartnerSteps.export_payment(refund_id)

# steps.TaxiSteps.create_taxi_orders(partner_id_ooo, datetime.datetime(2018,3,1,0,0,0), currency=Currencies.RUB)
# steps.TaxiSteps.process_taxi(495457, datetime.datetime.now())

# steps.TaxiSteps.create_order(81223518, datetime.datetime(2018,4,6,3,6,4), payment_type='cash',
#                                  promocode_sum=40, commission_sum=50)
# steps.TaxiSteps.process_taxi(519973, datetime.datetime(2018,4,7,0,0,0))
# from btestlib import shared, utils, reporter
# #
from btestlib.constants import Currencies, OEBSOperationType, Export, Firms

# def export_correction_netting(cash_fact_id):
#     with reporter.step(u'Экспортируем {} очереди {}, id: {}'
#                                .format(Export.Classname.OEBS_CPF, Export.Type.THIRDPARTY_TRANS, cash_fact_id)):
#         steps.CommonSteps.export(Export.Type.THIRDPARTY_TRANS, Export.Classname.OEBS_CPF, cash_fact_id)
#
#
def process_payment(invoice_id):
    steps.CommonSteps.export(Export.Type.PROCESS_PAYMENTS, Export.Classname.INVOICE, invoice_id)
#
# def create_correction_netting(invoice_eid, amount, dt, netting_amount):
#     with reporter.step(u'Создаем отрицательную корректировку на "избыток" взаимозачета '
#                        u'для счета: {}, на сумму: {}, дата: {}'.format(invoice_eid, amount, dt)):
#         steps.TaxiSteps.create_cash_payment_fact(invoice_eid, netting_amount, dt, OEBSOperationType.INSERT_NETTING)
#         return steps.TaxiSteps.create_cash_payment_fact(invoice_eid, -amount, dt, OEBSOperationType.CORRECTION_NETTING)
#
#
# fact_id, source_id = create_correction_netting(u'ЛСТ-754474657-1', Decimal('1.5'), datetime.datetime(2018,4,5,0,0,0), 100)
# export_correction_netting(fact_id)
# process_payment(75877909)

# steps.ClientSteps.create()
# steps.CommonSteps.export('OEBS', 'Contract', 574424)
# steps.CommonSteps.export('OEBS', 'ContractCollateral', 736449)


# steps.CommonPartnerSteps.generate_partner_acts_fair(693759, datetime.datetime(2018,4,1,0,0,0))

client_id = 81936046
service = Services.TAXI_DONATE
currency = Currencies.EUR

# SIMPLE_VERSION = balance_config.SIMPLE_PG
# person_id = steps.PersonSteps.create(client_id, 'eu_yt', {'is-partner': '1'})
# service_product_id = steps.SimpleApi.create_service_product(service, partner_id=client_id)
# from simpleapi.common.payment_methods import Coupon
# from btestlib.data import simpleapi_defaults
# service_order_id, trust_payment_id, purchase_token, payment_id = steps.SimpleApi.create_trust_payment(
#         Services.TAXI_DONATE, service_product_id, paymethod=Coupon(), user=simpleapi_defaults.USER_ANONYMOUS,
#         currency=currency, order_dt=datetime.datetime.now())
# steps.CommonPartnerSteps.export_payment(payment_id)

# steps.SimpleApi.create_fake_thirdparty_payment(ThirdPartyData.TAXI_DONATE, 693759,
#                                                    6780165, 81936046, amount=800,
#                                                    dt=datetime.datetime(2018,4,1,6,0,0), payment_type='coupon',
#                                                    payment_currency=currency,
#                                                    contract_currency=currency)
# steps.SimpleApi.create_fake_thirdparty_payment(ThirdPartyData.TAXI_DONATE, 693759,
#                                                    6780165, 81936046, amount=100,
#                                                    dt=datetime.datetime(2018,4,1,6,0,0), payment_type='subsidy',
#                                                    payment_currency=currency,
#                                                    contract_currency=currency)


# steps.TaxiSteps.process_taxi(704276, datetime.datetime(2018,5,31,0,0,0))


# from btestlib.constants import Currencies, Regions, TaxiOrderType, PaymentType
# from btestlib.data.defaults import GeneralPartnerContractDefaults as GenDefParams
# from btestlib.data.defaults import Distribution, Partner, Client, Order, EventsTicketsNew, EventsTickets, TaxiPayment, \
#     Taxi
# client_id, person_id, contract_id = steps.TaxiSteps.create_taxi_client_person_contract(GenDefParams.YANDEX_TAXI, is_postpay=0,
#                                                                                        country = Regions.RU,
#                                        currency=currency,
#                                        additional_params={'start_dt': datetime.datetime(2018,5,1),  'advance_payment_sum': 0})
# query_update_mv = "BEGIN dbms_mview.refresh('BO.MV_PARTNER_TAXI_CONTRACT','C'); END;"
# db.balance().execute(query_update_mv, descr='Обновляем MV_PARTNER_TAXI_CONTRACT')
#
# # steps.TaxiSteps.create_order(client_id, datetime.datetime(2018,5,1), PaymentType.CASH,
# #                                    commission_sum=Taxi.childchair_cash,
# #                                    currency=currency.iso_code, order_type=TaxiOrderType.childchair,
# #                                    promocode_sum=0)
#
# steps.TaxiSteps.create_order(client_id, datetime.datetime(2018,5,1), PaymentType.CASH,
#                                    commission_sum=Taxi.order_commission_cash,
#                                    currency=currency.iso_code, order_type=TaxiOrderType.commission,
#                                    promocode_sum=0)
# steps.TaxiSteps.process_taxi(contract_id, datetime.datetime(2018,5,5))
# steps.TaxiSteps.generate_acts_prepay(contract_id,  datetime.datetime(2018,5,1))

# steps.TaxiSteps.check_taxi_invoice_data(client_id, contract_id, person_id,
#                                         contract_data, currency=currency, nds=nds)
# steps.TaxiSteps.check_taxi_act_data(client_id, contract_id, ACT_DT)
# steps.TaxiSteps.check_taxi_order_data(client_id, contract_id, 0, currency=currency)
# steps.TaxiSteps.check_taxi_consume_data(client_id, contract_id, currency=currency)

# api.medium().GetTaxiBalance([717911])


# ------------------ТАКСИ СНГ


#
def create_request_corp_taxi(client_id):
    begin_dt = datetime.datetime.now()
    service_id = 135
    qty = 50
    product_id = 507154#509192 #509170

    service_order_id = steps.OrderSteps.next_id(service_id)
    steps.OrderSteps.create(client_id, service_order_id, product_id=product_id,
                            service_id=service_id)

    orders_list = [{'ServiceID':service_id, 'ServiceOrderID': service_order_id, 'Qty': qty,
                    'BeginDT': begin_dt}]
    request_id = steps.RequestSteps.create(client_id, orders_list)
    steps.RequestSteps.create(client_id, orders_list, additional_params={'InvoiceDesireDT': begin_dt, 'InvoiceDesireType': 'charge_note'})

# create_request_corp_taxi()
# from simpleapi.common.payment_methods import Cash
# from simpleapi.data.uids_pool import User
#
# SERVICE = Services.TAXI_CORP
# PAYMETHOD = Cash()
# USER_EMPTY_CORP_TAXI = User(313834851, 'atkaya-test-3', None)
# client_id_partner, service_product_id = steps.SimpleApi.create_partner_and_product(SERVICE)
# service_product_id_fee = steps.SimpleApi.create_service_product(SERVICE, client_id_partner, service_fee=1)
# person_id_partner = steps.PersonSteps.create(client_id_partner, 'kzu', {'is-partner': '1'})
#
# api.medium().CreateCommonContract(PASSPORT_ID, {'client_id': client_id_partner,
#                                           'country': 159,
#                                           'ctype': 'SPENDABLE',
#                                           'currency': 'KZT',
#                                           'firm_id': 31,
#                                           'manager_uid': '244916211',
#                                           'nds': 0,#12,
#                                           'person_id': person_id_partner,
#                                           'services': [135],
#                                           'signed': 1,
#                                           'partner_commission_pct': 4})
#
# client_id_corp = steps.ClientSteps.create()
# person_id_corp = steps.PersonSteps.create(client_id_corp, 'kzu')
# person_id_corp = steps.PersonSteps.create(client_id_corp, 'ur')
#
# # client_id_corp = 82088802
# # service_product_id_prep = '1528366105954-CB7ZCRDKTN-3892'
# # service_product_id_fee_prep = '1528366106081-8UOVQA6K3N-3892'
#
# # service_product_id = '1528379367749-GDHM96SIB0-12332'
# # service_product_id_fee = '1528379367863-9RHTA5UFNX-12332'
#
#
#
#
# steps.UserSteps.link_user_and_client(USER_EMPTY_CORP_TAXI, client_id_corp)
# api.medium().CreateCommonContract(PASSPORT_ID,
#                                          {'client_id': client_id_corp,
#                                           'ctype': 'GENERAL',
#                                           'currency': 'RUB',
#                                           'firm_id': 13,
#                                           # 'manager_code': '23131312312',#30100,
#                                           'manager_uid': '244916211',
#                                           # 'manager_uid': '1120000000048750',
#                                           # 'offer_confirmation_type': 'no',
#                                           # 'payment_term': None,
#                                           'payment_type': 3,
#                                           'payment_term': 30,
#                                           'person_id': person_id_corp,
#                                           'personal_account': 1,
#                                           'services': [135],
#                                           'signed': 1,
#                                           'start_dt': datetime.datetime(2018, 10, 3, 0, 0)
#                                           })
# create_request_corp_taxi(client_id_corp)
# service_order_id, trust_payment_id, purchase_token, payment_id = \
#         steps.SimpleApi.create_multiple_trust_payments(SERVICE,
#                                              [service_product_id, service_product_id_fee],
#                                              paymethod=PAYMETHOD,
#                                              user=USER_EMPTY_CORP_TAXI,
#                                              order_dt=utils.Date.moscow_offset_dt(),
#                                              currency = Currencies.KZT,
#                                              prices_list=[33.22, 1.3])
                                                       #fiscal_nds_list=[CMNds.NDS_18, CMNds.NDS_10])
#
# # запускаем обработку платежа
# steps.CommonPartnerSteps.export_payment(payment_id)


# trust_refund_id, refund_id = steps.SimpleApi.create_multiple_refunds(SERVICE, ['3171254994', '3171254997'], '5b1915e9910d39422cba01b3',
#                                                                      delta_amount_list=[100.44, 7.1])
# steps.CommonPartnerSteps.export_payment(refund_id)

# api.medium().GetClientContracts({'ClientID': 106805980, 'ContractType': 'SPENDABLE'})
# api.medium().GetClientContracts({'ClientID': '82088802'})

# api.medium().GetPartnerBalance(Services.TAXI_CORP.id, [540122])
# api.test_balance().CacheBalance(736449)
# steps.TaxiSteps.process_taxi(540122, datetime.datetime.today())
# steps.TaxiSteps.process_taxi(741415)
# api.test_balance().ExportObject('PROCESS_TAXI', 'Contract', 741415)


# steps.TaxiSteps.pay_to_personal_account(1000, 741415)

# from btestlib.data.partner_contexts import TAXI_RU_CONTEXT
# steps.ContractSteps.create_partner_contract(TAXI_RU_CONTEXT, additional_params={'memo': '2323'})


# api.test_balance().SyncRevPartnerServices()










# USER_TAXI = User(4012702134, 'testkzclient', None)
# client_id_corp = steps.ClientSteps.create()
# person_id_corp = steps.PersonSteps.create(client_id_corp, 'kzu')
# steps.UserSteps.link_user_and_client(USER_TAXI, client_id_corp)




# partner_id = steps.SimpleApi.create_partner(SERVICE)

# partner_id = 82107656
# person_id_comm_bv = 6858040 #steps.PersonSteps.create(partner_id, 'eu_yt')
# person_id_comm_kzt = steps.PersonSteps.create(partner_id, 'kzu')
# person_id_rash_kzt = steps.PersonSteps.create(partner_id, 'kzu', {'is-partner': '1'})


# api.medium().CreateCommonContract(PASSPORT_ID,
#                                          {'client_id': partner_id,
#                                           # 'ctype': 'GENERAL',
#                                           'currency': 'KZT',
#                                           'firm_id': 24,
#                                           'manager_uid': '244916211',
#                                           'payment_type': 3,
#                                           'person_id': person_id_comm_kzt,
#                                           'personal_account': 1,
#                                           'services': [124],
#                                           'signed': 1,
#                                           'payment_term': 10,
#                                           'country': 159,
#                                           'partner_commission_pct2': 1,
#                                           'start_dt': datetime.datetime(2018, 6, 1, 0, 0)
#                                           })
# api.medium().CreateCommonContract(PASSPORT_ID, {'client_id': partner_id,
#                                           'country': 159,
#                                           'ctype': 'SPENDABLE',
#                                           'currency': 'KZT',
#                                           'firm_id': 31,
#                                           'manager_uid': '244916211',
#                                           'nds': 12,
#                                           'person_id': person_id_rash_kzt,
#                                           'services': [135],
#                                           'signed': 1,
#                                           'partner_commission_pct': 4})




# client_id = steps.ClientSteps.create()
# person_id = steps.PersonSteps.create(client_id, 'ur', {'is-partner': '1', 'bik': '044525974',
#                                                        'account': '30101810800000000777'})

def create_request_autobus():
    client_id = steps.ClientSteps.create({'Name': 'Client test autobus'})
    contract_id, _, person_id = steps.ContractSteps.\
        create_person_and_offer_with_additional_params(client_id,

                                                      GenDefParams.BUSES2_0_BELARUS,
                                                      # GenDefParams.BUSES2_0,
                                                      additional_params={'start_dt': datetime.datetime(2018,1,1),
                                                                         'offer_confirmation_type': 'no',
                                                                         'services': [602, 205]},
                                                      is_offer=True,
                                                      )
    begin_dt = datetime.datetime.now()
    service_id = 205
    qty = 50
    product_id = 509325 #Belarus
    # product_id = 508665 #RF

    service_order_id = steps.OrderSteps.next_id(service_id)
    steps.OrderSteps.create(client_id, service_order_id, product_id=product_id,
                            service_id=service_id)

    orders_list = [{'ServiceID':service_id, 'ServiceOrderID': service_order_id, 'Qty': qty,
                    'BeginDT': begin_dt}]
    # request_id = steps.RequestSteps.create(client_id, orders_list)
    steps.RequestSteps.create(client_id, orders_list, additional_params={'InvoiceDesireType': 'charge_note'}) #'InvoiceDesireType': 'charge_note',

# create_request_autobus()

def create_request_prepay():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ph')

    begin_dt = datetime.datetime.now()
    service_id = 129
    qty = 50
    product_id = 508229

    service_order_id = steps.OrderSteps.next_id(service_id)
    steps.OrderSteps.create(client_id, service_order_id, product_id=product_id,
                            service_id=service_id)

    orders_list = [{'ServiceID':service_id, 'ServiceOrderID': service_order_id, 'Qty': qty,
                    'BeginDT': begin_dt}]
    # request_id = steps.RequestSteps.create(client_id, orders_list)
    steps.RequestSteps.create(client_id, orders_list, additional_params={'FirmID': 1})

# create_request_prepay()
# steps.CommonSteps.export('OEBS', 'Client', 82646703)
# steps.CommonSteps.export('THIRDPARTY_TRANS', 'Payment', 603721632)
# steps.CommonPartnerSteps.generate_partner_acts_fair(1615692, datetime.datetime(2019,4,1))

# steps.ClientSteps.create()

# from simpleapi.steps.payments_api_steps import Bindings
# from simpleapi.data.uids_pool import User
# from btestlib.constants import Services
#
# service = Services.CLOUD_143
# purchase_token = '6d53d80333b6ebe516bb3e96fcd8c27c'
# user = User(450606209, 'yb-atst-user-25', None)
# Bindings.get(service, user, purchase_token)

# client_id = steps.ClientSteps.create()
# steps.PersonSteps.create(client_id, 'ur')



def create_request():
    # client_id = steps.ClientSteps.create()
    # person_id = steps.PersonSteps.create(client_id, 'ph')
    client_id = 81278072
    begin_dt = datetime.datetime.now()
    service_id = 7
    qty = 50
    product_id = 1475

    service_order_id = steps.OrderSteps.next_id(service_id)
    steps.OrderSteps.create(client_id, service_order_id, product_id=product_id,
                            service_id=service_id)

    orders_list = [{'ServiceID':service_id, 'ServiceOrderID': service_order_id, 'Qty': qty,
                    'BeginDT': begin_dt}]
    # request_id = steps.RequestSteps.create(client_id, orders_list)
    steps.RequestSteps.create(client_id, orders_list)
# create_request()

# client_id = steps.ClientSteps.create()
# steps.PersonSteps.create(client_id, 'ur')

service_id = 123
product_id =  503801
passport_id = 168196307
paysys_id = 1000
#
# service_id = 23
# product_id =  504084
passport_id = 168196307
# paysys_id = 1078

# client_id = steps.ClientSteps.create() #{'REGION_ID': 159}
# # person_id = steps.PersonSteps.create(client_id, 'ur')
# client_to_remove = api.medium().FindClient({'PassportID': passport_id})[2][0]['CLIENT_ID']
# api.medium().RemoveUserClientAssociation(112986776, client_to_remove, passport_id)
# db.balance().execute("DELETE FROM t_service_client WHERE service_id = "+str(service_id)+" AND passport_id = "+ str(passport_id))
# db.balance().execute("Insert into t_service_client (PASSPORT_ID,SERVICE_ID,CLIENT_ID) values ("+str(passport_id)+","+str(service_id)+","+str(client_id)+")")
# api.medium().CreateUserClientAssociation(112986776, client_id, passport_id)
# service_order_id = steps.OrderSteps.next_id(service_id)
# steps.OrderSteps.create(client_id, service_order_id, product_id=product_id,
#                         service_id=service_id)
# api.medium().CreateOrUpdateOrdersBatch(passport_id, ({'ClientID': client_id, 'ProductID': product_id,
#                                                       'ServiceOrderID': service_order_id},),
#                                                        'music_039128f74eaa55f94617c329b4c06e65')
# orders_list = [{'ServiceID':service_id, 'ServiceOrderID': service_order_id, 'Qty': 15,
#                     'BeginDT': datetime.datetime.now()}]
# request_id = steps.RequestSteps.create(client_id, orders_list)
# invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, paysys_id, credit=0, overdraft=0, endbuyer_id=None)
# api.medium().CreateFastInvoice({'back_url': 'http://music.mt.yandex.ru',
#                                                 'login': 'clientuid40',
#                                                 'mobile': False,
#                                                 'overdraft': False,
#                                                 'paysys_id': paysys_id,
#                                                 'qty': 30,
#                                                 'service_id': service_id,
#                                                 'service_order_id': service_order_id})
# client_id = steps.ClientSteps.create()
# steps.PersonSteps.create(client_id, 'ur')




#-------------NETTING 2.0

def create_correction_netting(invoice_eid, amount, dt, netting_amount):
        steps.TaxiSteps.create_cash_payment_fact(invoice_eid, netting_amount, dt, OEBSOperationType.INSERT_NETTING)
        return steps.TaxiSteps.create_cash_payment_fact(invoice_eid, -amount, dt, OEBSOperationType.CORRECTION_NETTING)


def export_correction_netting(cash_fact_id, handle_with_process_payment=False):
    if not handle_with_process_payment:
            steps.CommonSteps.export(Export.Type.THIRDPARTY_TRANS, Export.Classname.OEBS_CPF, cash_fact_id)

# def process_payment(invoice_id, handle_with_process_payment=False):
#         if handle_with_process_payment:
#             set_wait_for_correction_netting(timeout=5)
#         steps.CommonSteps.export(Export.Type.PROCESS_PAYMENTS, Export.Classname.INVOICE, invoice_id)

def set_wait_for_correction_netting(timeout):
        query = "UPDATE T_CONFIG SET VALUE_NUM=:timeout WHERE ITEM='WAIT_FOR_CORRECTION_NETTING_BEFORE_PROCESS_PAYMENT'"
        params = {'timeout': timeout}
        db.balance().execute(query, params)

def get_invoice_ids(client_id):
        query = "SELECT inv.id, inv.EXTERNAL_ID " \
                "FROM T_INVOICE inv LEFT JOIN T_EXTPROPS prop ON " \
                "inv.ID = prop.OBJECT_ID AND " \
                "prop.CLASSNAME='PersonalAccount' AND prop.ATTRNAME='service_code' " \
                "WHERE inv.type='personal_account' AND inv.CLIENT_ID=:client_id AND prop.VALUE_STR IS NULL"

        params = {'client_id': client_id}
        invoice = db.balance().execute(query, params, single_row=True)
        return invoice['id'], invoice['external_id']

from temp.igogor.balance_objects import Contexts
context = Contexts.TAXI_RU_CONTEXT
# client_id, contract_id, person_id = steps.TaxiSteps.create_taxi_contract_prepay(datetime.datetime(2018,5,1),
#                                                                                         0,
#                                                                                         netting_pct=70,
#                                                                                         firm=context.firm,
#                                                                                         person_type=context.person_type.code,
#                                                                                         region=context.region,
#                                                                                         currency=context.currency,
#                                                                                         nds=context.nds,
#                                                                                         services=context.services)
# query_update_mv = "BEGIN dbms_mview.refresh('BO.MV_PARTNER_TAXI_CONTRACT','C'); END;"
# db.balance().execute(query_update_mv, descr='Обновляем MV_PARTNER_TAXI_CONTRACT')

# client_id = 83085212
# contract_id = 1010642

# client_id = 83081330
# contract_id = 1008535

# steps.TaxiSteps.create_taxi_orders(client_id, datetime.datetime(2018,5,1), currency=context.currency)

# steps.TaxiSteps.create_order(client_id, datetime.datetime(2018,8,5,0,0,0), payment_type=PaymentType.CASH,
#                              promocode_sum=10,
#                              currency=context.currency.iso_code,
#                              commission_sum=1000,
#                              order_type=TaxiOrderType.commission)

# steps.TaxiSteps.create_order(client_id, datetime.datetime(2018,8,7,0,0,0), payment_type=PaymentType.CARD,
#                              promocode_sum=1000,
#                              currency=context.currency.iso_code,
#                              commission_sum=10000)

# steps.TaxiSteps.create_order(client_id, datetime.datetime(2018,8,2,0,0,0), payment_type=PaymentType.CASH,
#                              promocode_sum=100,
#                              currency=context.currency.iso_code,
#                              commission_sum=300,
#                              order_type=TaxiOrderType.driver_workshift)

# steps.TaxiSteps.create_order(client_id, datetime.datetime(2018,8,4,0,0,0), payment_type=PaymentType.CASH,
#                              promocode_sum=0,
#                              currency=context.currency.iso_code,
#                              commission_sum=500,
#                              order_type=TaxiOrderType.commission)

# steps.TaxiSteps.process_taxi(contract_id, datetime.datetime(2018,8,6,23,59,59))
# steps.TaxiSteps.process_taxi(contract_id)
# steps.CommonSteps.export('PROCESS_TAXI', 'Contract', contract_id)


# correction_netting_amount = 1000
# netting_amount = 900
# invoice_id, invoice_eid = get_invoice_ids(client_id)
# fact_id, source_id = create_correction_netting(invoice_eid,
#                                                correction_netting_amount,
#                                                datetime.datetime(2018,8,7,0,0,0),
#                                                netting_amount)
# export_correction_netting(fact_id, 1)
# process_payment(invoice_id, 1)

# service_product_id = '1533825307862-Y81SXKZ628-11668'
# client_id = 83107056
# contract_id = 1018725


#ВЗАИМОЗАЧЕТ АУДИТ
service = Services.TAXI
# client_id, service_product_id = steps.SimpleApi.create_partner_and_product(service)
#
# _, contract_id, person_id = steps.TaxiSteps.create_taxi_contract_prepay(datetime.datetime(2018,5,1),
#                                                                                         0,
#                                                                                         netting_pct=100,
#                                                                                         firm=context.firm,
#                                                                                         person_type=context.person_type.code,
#                                                                                         region=context.region,
#                                                                                         currency=context.currency,
#                                                                                         nds=context.nds,
#                                                                                         services=context.services,
#                                                                                 client_id = client_id)
# query_update_mv = "BEGIN dbms_mview.refresh('BO.MV_PARTNER_TAXI_CONTRACT','C'); END;"
# db.balance().execute(query_update_mv, descr='Обновляем MV_PARTNER_TAXI_CONTRACT')
#
#
#
# from dateutil.relativedelta import relativedelta
# order_dt = utils.Date.moscow_offset_dt() - relativedelta(months=1)
#
# service_order_id, trust_payment_id, purchase_token, payment_id = \
#         steps.SimpleApi.create_trust_payment(service, service_product_id, currency=context.payment_currency,
#                                              order_dt=order_dt)
# steps.CommonPartnerSteps.export_payment(payment_id)
# steps.TaxiSteps.create_order(client_id, datetime.datetime(2018,8,1,0,0,0), payment_type=PaymentType.CARD,
#                              promocode_sum=1000,
#                              currency=context.currency.iso_code,
#                              commission_sum=2000)
# steps.TaxiSteps.process_taxi(contract_id, datetime.datetime(2018,8,12,0,0,0))
#
# query = "SELECT id FROM T_THIRDPARTY_TRANSACTIONS where contract_id = :contract_id"
# params = {'contract_id': contract_id}
# transaction_id = db.balance().execute(query, params)[0]['id']
#
# query = "SELECT id FROM T_THIRDPARTY_CORRECTIONS where contract_id = :contract_id"
# params = {'contract_id': contract_id}
# correction_id = db.balance().execute(query, params)[0]['id']
#
# steps.ExportSteps.export_oebs(client_id=client_id, contract_id=contract_id,
#                               correction_id=correction_id,
#                               transaction_id=transaction_id)
# invoice_id, invoice_eid = get_invoice_ids(client_id)


# invoice_id = 77074583
# invoice_eid = u'ЛСТ-986617046-1'
# fact_id, source_id = create_correction_netting(invoice_eid, 900,
#                                                    datetime.datetime.now(),
#                                                    10000)
# export_correction_netting('8000012739')
# process_payment(invoice_id)

# invoice_id_2 = 76980980
# process_payment(invoice_id_2, 1)















# api.medium().GetTaxiBalance([846181])


#
# steps.ExportSteps.export_oebs(client_id=83124841, contract_id=1025354,
#                               )
# steps.CommonSteps.export('OEBS', 'Invoice', 76945948)

# context_BV = Contexts.TAXI_UBER_BY_CONTEXT
# client_id, contract_id, person_id = steps.TaxiSteps.create_taxi_contract_prepay(datetime.datetime(2018,5,1),
#                                                                                         0,
#                                                                                         netting_pct=70,
#                                                                                         firm=context_BV.firm,
#                                                                                         person_type=context_BV.person_type.code,
#                                                                                         region=context_BV.region,
#                                                                                         currency=context_BV.currency,
#                                                                                         nds=context_BV.nds,
#                                                                                         services=context_BV.services)
# steps.ExportSteps.export_oebs(client_id=client_id, contract_id=contract_id)

# client_id = steps.ClientSteps.create({'IS_AGENCY': 1})
# person_id = steps.PersonSteps.create(client_id, 'ur')

# client_id = steps.ClientSteps.create({'IS_AGENCY': 1})
# person_id = steps.PersonSteps.create(client_id, 'byu')

# steps.ExportSteps.export_oebs(client_id=83186985)
# steps.CommonSteps.export('OEBS', 'Product', 509060)
# steps.CommonSteps.export('OEBS', 'ContractCollateral', 1417013)
# steps.CommonSteps.export('OEBS', 'ContractCollateral', 1255293)
# steps.CommonSteps.export('OEBS', 'ContractCollateral', 1256243)

# contract_id, _ = steps.ContractSteps.create_contract('no_agency_apikeys_post',
#                                                          {'CLIENT_ID': client_id, 'PERSON_ID': person_id,
#                                                           })

# query_update_mv = "BEGIN dbms_mview.refresh('BO.MV_PARTNER_TAXI_CONTRACT','C'); END;"
# db.balance().execute(query_update_mv, descr='Обновляем MV_PARTNER_TAXI_CONTRACT')

# api.test_balance().SyncRevPartnerServices()

# source = 'buses2'
# start_dt = datetime.datetime(2018,8,31)
# end_dt = datetime.datetime(2018,8,31)
# api.test_balance().GetPartnerCompletions({'start_dt': start_dt, 'end_dt': end_dt, 'completion_source': source})

# steps.ExportSteps.export_oebs(client_id = 83230267, person_id = 7366835, contract_id=1055348)

# from btestlib.constants import Currencies, Users, Passports
# url = 'https://wiki-api.yandex-team.ru/_api/frontend/sales/processing/servisy/Pamjatka/Ja.Konnjekt/Dogovory/1/test/contract-1073582/'
# session = steps.passport_steps.auth_session(Users.TESTUSER_BALANCE3, Passports.INTERNAL)
# response = session.get(url, verify=False)
# print response.status_code



# deactivation buses
#
# # создаем клиента и плательщика
# client_id = steps.ClientSteps.create({'Name': 'Client test autobus'})

# создаем договор
# contract_id, _, person_id = steps.ContractSteps.\
#     create_person_and_offer_with_additional_params(client_id,
#                                                    GenDefParams.BUSES2_0,
#                                                    additional_params={'start_dt': datetime.datetime(2018,1,1),
#                                                                       'services': [Services.BUSES_2_0.id, Services.BUSES_2.id],
#                                                                       'offer_activation_payment_amount': 100,
#                                                                       'offer_activation_due_term': 5,
#                                                                       'offer_confirmation_type': 'min-payment',
#                                                                       },
#                                                    is_offer=True,
#                                                    )
# end_dt = datetime.datetime(2018,9,19)
# params = {'CONTRACT2_ID': contract_id, 'FINISH_DT': end_dt,
#               'DT': end_dt.isoformat(), 'IS_FAXED': end_dt.isoformat(), 'IS_BOOKED': end_dt.isoformat()}
#
# steps.ContractSteps.create_collateral(Collateral.TERMINATE, params)
# steps.TaxiSteps.process_taxi(contract_id, datetime.datetime(2018,9,17))
# # steps.ClientSteps.get_client_contracts(client_id, ContractSubtype.GENERAL)
#
# invoice_eid = steps.InvoiceSteps.get_invoice_data_by_client_with_ids(client_id)[0]['external_id']
#
# steps.TaxiSteps.create_cash_payment_fact(invoice_eid, 100, datetime.datetime(2018,9,1), OEBSOperationType.ONLINE)
#
# steps.TaxiSteps.process_taxi(contract_id, datetime.datetime(2018,9,19))
# steps.ClientSteps.get_client_contracts(81500711, ContractSubtype.GENERAL)

# dt = datetime.datetime.now()
# print dt.isoformat()


# client_id = steps.ClientSteps.create()
# person_id = steps.PersonSteps.create(client_id, 'ur', {'is-partner': '1'})
# completion_sum_cash1 = Decimal('50')
# promo_sum = Decimal('20')
# completion_sum_cash2 = Decimal('125')
# completion_sum_card1 = Decimal('80')
# payment_sum = Decimal('200')
#
# act_sum = utils.dround((completion_sum_cash1 * utils.fraction_from_percent(context.nds) +
#                         completion_sum_card1 * utils.fraction_from_percent(context.nds)), 2) \
#           - (payment_sum - utils.dround((completion_sum_cash1 * utils.fraction_from_percent(context.nds) +
#                         completion_sum_cash2 * utils.fraction_from_percent(context.nds)), 2) + promo_sum)
#
# print act_sum

# api.test_balance().SyncRevPartnerServices()


# client_id = steps.ClientSteps.create()
# # steps.PersonSteps.create(client_id, 'eu_yt')
# api.medium().CreatePerson(16571028,
#                                           {
#
#                                               'type': 'eu_yt',
#                                               'ownership-type-ui': 'INDIVIDUAL',
#                                               'name': u'Individual entrepreneur Test',
#                                               'local-name': u'Individual entrepreneur 1',
#                                               'phone': '+2323232323',
#                                               'email': 'test@test.com',
#                                               'representative': u'Test Test',
#                                               'local-representative': u'Test Test Test',
#                                               'postaddress': u'220075 Minsk, Street Test',
#                                               'local-postaddress': u'Street Test 77',
#                                               'postcode': '220075',
#                                               'region': '149',
#                                               'inn': '591708183',
#                                               'account': '30132377550020270000',
#                                               'swift': 'ALFABY2XXXX',
#                                               'client_id': client_id})
#                                               # 'pay-type': '1'})

# client_id = steps.ClientSteps.create()
# person_id = steps.PersonSteps.create(client_id, 'ur')
# api.medium().CreateOffer(PASSPORT_ID,
#                          {'client_id': '83815728', 'currency': 'RUB', 'payment_type': 3, 'payment_term': 30, 'person_id': '7628256', 'services': [143], 'manager_uid': '98700241', 'projects': ['0d65e2c8-d84f-4d3f-9dd1-6827bd6ea70e'], 'firm_id': 123})


#PROMOCODES

def create_request_taxi(client_id, service_order_id, product_id, qty, promo=None, turnonrows=1, postpay=0):
    begin_dt = datetime.datetime.now()
    service_id = Services.TAXI_CORP.id

    additional_params = {}

    # service_order_id = steps.OrderSteps.next_id(service_id)
    # steps.OrderSteps.create(client_id, service_order_id, product_id=product_id,
    #                         service_id=service_id)

    orders_list = [{'ServiceID':service_id, 'ServiceOrderID': service_order_id, 'Qty': qty,
                    'BeginDT': begin_dt}]
    if turnonrows:
        additional_params.update({'TurnOnRows': 1})
    if promo:
        additional_params.update({'PromoCode': promo})
    if postpay:
        additional_params.update({'InvoiceDesireType': 'charge_note'})

    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params=additional_params)#'InvoiceDesireType': 'charge_note'})
    return request_id

def create_charge_note(request_id, person_id, paysys_id, contract_id):
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, paysys_id,
                                                 credit=0, contract_id=contract_id)
    return invoice_id

def create_promo(firm=Firms.TAXI_13.id, client_id=None):
    start_dt = datetime.datetime(2018,1,1)
    end_dt = datetime.datetime(2019,1,1)
    bonus1 = bonus2 = Decimal('5000')
    minimal_qty = Decimal('1000')
    firm_id = firm
    promo_code_id = steps.PromocodeSteps.create(start_dt=start_dt, end_dt=end_dt, bonus1=bonus1, bonus2=bonus2,
                                                    minimal_qty=minimal_qty,
                                                    reservation_days=None,
                                                    firm_id=firm_id,
                                                    multicurrency_bonuses='{"RUB": {"bonus1": 200, "bonus2": 200, "minimal_qty": 60}}',
                                                    # multicurrency_bonuses='{"RUB": {"bonus1": 200, "bonus2": 200}}',
                                                    is_global_unique=0,
                                                )
                                                    # new_clients_only=new_clients_only, valid_until_paid=valid_until_paid,
                                                    # code=code, middle_dt=middle_dt)

    steps.PromocodeSteps.set_services(promo_code_id, [Services.TAXI_CORP.id])

    promo_code_code = db.get_promocode_by_id(promo_code_id)[0]['code']
    if client_id:
        steps.PromocodeSteps.reserve(client_id, promo_code_id)
    return promo_code_id, promo_code_code


from balance.tests.promocode_new.promocode_commons import fill_calc_params_fixed_sum, \
    create_and_reserve_promocode, import_promocode, reserve, fill_calc_params_fixed_qty, \
    fill_calc_params_fixed_discount, fill_calc_params_scale
def create_new_promo(client_id=None, currency=Currencies.RUB, firm_id=Firms.TAXI_13.id, product_id=None):
    start_dt = datetime.datetime(2018,1,1)
    end_dt = datetime.datetime(2019,1,1)
    minimal_amounts={Currencies.RUB.iso_code: 70}

    #PromocodeClass.FIXED_SUM
    calc_params = fill_calc_params_fixed_sum(currency_bonuses={currency.iso_code: 20},
                                             reference_currency=currency.iso_code,
                                             adjust_quantity=True,
                                             apply_on_create=True)

    #PromocodeClass.FIXED_QTY
    # calc_params = fill_calc_params_fixed_qty(product_bonuses={str(product_id): 200},
    #                                      adjust_quantity=False,
    #                                      apply_on_create=False)

    # PromocodeClass.FIXED_DISCOUNT
    # calc_params = fill_calc_params_fixed_discount(discount_pct=10,
    #                                      adjust_quantity=False,
    #                                      apply_on_create=True)

    # #PromocodeClass.SCALE
    # calc_params = fill_calc_params_scale(currency=currency.iso_code, scale_points = [(10, 15), (50, 25)],
    #                                      adjust_quantity=False,
    #                                      apply_on_create=False)

    code = steps.PromocodeSteps.generate_code()

    promo_code_id, promo_code_code = import_promocode(calc_class_name=PromocodeClass.FIXED_SUM,
                                                    start_dt=start_dt, end_dt=end_dt,
                                                    calc_params=calc_params,
                                                    firm_id=firm_id,
                                                    promocodes=[code],
                                                    service_ids=[Services.TAXI_CORP.id],
                                                    minimal_amounts=minimal_amounts)[0]
    if client_id:
        reserve(client_id, promo_code_id)


    # promo = steps.PromocodeSteps.create_new(calc_class_name=PromocodeClass.FIXED_SUM,
    #                                         promocodes=[code],
    #                                             start_dt=start_dt, end_dt=end_dt,
    #                                                 firm_id=firm_id,
    #                                                 is_global_unique=1,
    #                                                 calc_params=calc_params,
    #                                                 service_ids=[Services.TAXI_CORP.id],
    #                                                 new_clients_only=0,
    #                                         reservation_days=400
    #                                             )[0]
    # promo_code_id, promo_code_code = promo['id'], promo['code']

    # if client_id:
    #     steps.PromocodeSteps.reserve(client_id, promo_code_id)
    return promo_code_id, promo_code_code

def create_contract_taxi(contract_data, is_postpay=0):
    client_id = steps.ClientSteps.create()

    additional_params = {'ctype': 'GENERAL', 'start_dt': datetime.datetime(2018,10,1)}
    # 'offer_confirmation_type': 'no'
    contract_id, external_id, person_id = steps.ContractSteps.create_person_and_offer_with_additional_params(client_id,
                                                                                                  contract_data,
                                                                                                  is_offer=0,
                                                                                                  # person_id=person_id,
                                                                                                  is_postpay=is_postpay,
                                                                                                  additional_params=additional_params)

    # if is_postpay:
    #     service_order_id = steps.OrderSteps.next_id(Services.TAXI_CORP.id)
    #     steps.OrderSteps.create(client_id, service_order_id, product_id=product_id,
    #                             service_id=Services.TAXI_CORP.id, params={'ContractID':contract_id})
    # else:
    steps.CommonSteps.export('PROCESS_TAXI', 'Contract', contract_id)
    query = "select service_order_id from t_order where service_id = 135 and contract_id = :contract_id"
    params = {'contract_id': contract_id}
    service_order_id = db.balance().execute(query, params)[0]['service_order_id']

    return client_id, person_id, service_order_id, contract_id


def create_fact(invoice_eid, amount, orig_id=None):
    query = "INSERT INTO T_OEBS_CASH_PAYMENT_FACT" \
                    "(XXAR_CASH_FACT_ID, AMOUNT, RECEIPT_NUMBER, RECEIPT_DATE, OPERATION_TYPE, " \
                    "LAST_UPDATED_BY, LAST_UPDATE_DATE, CREATED_BY, CREATION_DATE, ORIG_ID) VALUES " \
                    "(S_OEBS_CASH_PAYMENT_FACT_TEST.nextval, :amount, :invoice_eid, :dt, :type, " \
                    "-1, :dt, -1, :dt, :orig_id)"
    params = {
                'invoice_eid': invoice_eid,
                'amount': amount,
                'dt': datetime.datetime(2018,10,18,0,0,0),
                'type': 'INSERT',
                'orig_id': orig_id
            }
    db.balance().execute(query, params)

def get_pa(contract_id):
    query = "SELECT id, external_id FROM t_invoice WHERE contract_id = :contract_id AND type = 'personal_account'"
    params = {'contract_id': contract_id}
    data = db.balance().execute(query, params)
    invoice_id = data[0]['id']
    invoice_external_id = data[0]['external_id']
    return invoice_id, invoice_external_id

def get_payment_id_for_charge_note(invoice_id):
    query = "SELECT id FROM t_payment WHERE invoice_id = :invoice_id"
    params = {'invoice_id': invoice_id}
    payment_id = db.balance().execute(query, params)[0]['id']
    return payment_id

paysys_id = Paysyses.BANK_UR_RUB_TAXI.id
# paysys_id = Paysyses.CARD_UR_RUB_TAXI.id
# paysys_id = Paysyses.BANK_UR_KZT_TAXI_CORP.id
# contract_data = GenDefParams.YANDEX_TAXI_CORP
# contract_data = GenDefParams.TAXI_UBER_KZT_CORP
qty = 100
qty_to_pay = qty
product_id = 507154 #Russia
# product_id = 509192 #Kazakhstan
# client_id = 104243264 #104243067
firm = Firms.TAXI_13.id

# client_id, person_id, service_order_id, contract_id = create_contract_taxi(contract_data, is_postpay=0)
# promo_code_id, promo_code_code = create_new_promo(client_id=client_id, firm_id=firm, product_id=product_id)
# promo_code_id, promo_code_code = create_promo(client_id=client_id, firm=firm)
# promo_code_id, promo_code_code = create_promo(firm=firm)

# client_id = 104439677
# service_order_id = '20000001260967'
# person_id = 8602545
# contract_id = 707451

# request_id = create_request_taxi(client_id, service_order_id, product_id, qty, promo=None, turnonrows=1)
# invoice_id_charge_note = create_charge_note(request_id, person_id, paysys_id, contract_id)
# invoice_id, invoice_external_id = get_pa(contract_id)
# payment_id = get_payment_id_for_charge_note(invoice_id_charge_note)
# create_fact(invoice_external_id, qty, payment_id)
# steps.CommonSteps.export(Export.Type.PROCESS_PAYMENTS, Export.Classname.INVOICE, invoice_id)
# steps.CommonSteps.export('PROCESS_TAXI', 'Contract', contract_id)
# create_fact(invoice_external_id, 100, payment_id)
# steps.CommonSteps.export(Export.Type.PROCESS_PAYMENTS, Export.Classname.INVOICE, invoice_id)
# steps.CommonSteps.export('PROCESS_TAXI', 'Contract', contract_id)

# db.balance().execute("BEGIN dbms_mview.refresh('BO.MV_DIST_CONTRACT_REVSHARE_PROD','C'); END;")

# request_id1 = create_request_taxi(client_id, service_order_id, product_id, qty, promo=promo_code_code, turnonrows=1)
# invoice_id_charge_note1 = create_charge_note(request_id1, person_id, paysys_id, contract_id)
# payment_id1 = get_payment_id_for_charge_note(invoice_id_charge_note1)
# create_fact(invoice_external_id, 100, payment_id1)
# steps.CommonSteps.export(Export.Type.PROCESS_PAYMENTS, Export.Classname.INVOICE, invoice_id)
# steps.CommonSteps.export('PROCESS_TAXI', 'Contract', contract_id)


# steps.CommonSteps.export('PROCESS_TAXI', 'Contract', 699042)



# client_id = 104257684
# person_id = 8511316
# contract_id = 650094
# service_order_id = '20000001222093'
# promo_code_code = 'N2RMWIGACOHCUZS3'








# create_fact(invoice_external_id, 100, payment_id)
# steps.CommonSteps.export(Export.Type.PROCESS_PAYMENTS, Export.Classname.INVOICE, invoice_id)
# steps.CommonSteps.export('PROCESS_TAXI', 'Contract', 650102)

# create_fact('ЛСТ-1259917711-1', 100, 1033038859)
# create_fact('ЛСТ-1259946282-1', 30)
# steps.CommonSteps.export(Export.Type.PROCESS_PAYMENTS, Export.Classname.INVOICE, 83954979)
# steps.TaxiSteps.process_taxi(646243, datetime.datetime(2018,10,18,0,0,0))
# steps.CommonSteps.export('PROCESS_TAXI', 'Contract', 707451)



# api.medium().GetPartnerBalance(Services.TAXI_CORP.id, [707451])

# steps.SimpleApi.create_fake_thirdparty_payment(ThirdPartyData.TAXI_CORP, 5555555,
#                                                    555555,
#                                                    5555, Decimal('200'),
#                                                    None, dt=datetime.datetime(2018,10,31), client_amount=Decimal('400'),
#                                                    client_id=105434681)
# steps.SimpleApi.create_fake_thirdparty_payment(ThirdPartyData.TAXI_CORP, 5555555,
#                                                    555555,
#                                                    5555, Decimal('200'),
#                                                    None, dt=datetime.datetime(2018,11,1), client_amount=Decimal('23'),
#                                                    client_id=105434681)


# steps.PartnerSteps.create_autobus_completion(103917878, datetime.datetime(2018,9,1), Decimal('200.3'))
# steps.CommonPartnerSteps.generate_partner_acts_fair(699042, datetime.datetime(2018,11,1))
# steps.CommonSteps.export('MONTH_PROC', 'Client', 103917878)
# print u'\u041b\u0421-1259492064-1'

#ЛС-1259492064-1
# api.medium().GetPersonalAccount({'PersonID': u'8383033', 'PaysysID': 1117, 'FirmID': 1, 'ProductID': u'508899'},)
# api.test_balance().ExecuteOEBS(7, 'SELECT * FROM apps.hz_contact_points WHERE orig_system_reference  = :object_id',
#                                     {'object_id': 'P8397226_F'})

# api.test_balance().ExportObject('THIRDPARTY_TRANS', 'Payment', 1032998641)
# api.medium().GetOrdersInfo({'ContractID': 300864})
# api.medium().GetNDSInfo(7)
# api.test_balance().ExportObject('THIRDPARTY_TRANS', 'Payment', 1033132706)


# client_id = steps.ClientSteps.create()
# person_id = steps.PersonSteps.create(client_id, 'kzu', {'is-partner': '1'})



###############DISTRIBUTION
from balance.distribution.distribution_types import DistributionType, DistributionSubtype
# client_id, person_id, tag_id = steps.DistributionSteps.create_distr_client_person_tag()
# api.medium().CreateCommonContract(PASSPORT_ID,
#                                   {'activation_price': Decimal('4.50'),
#                                    'advisor_price': Decimal('3.50'),
#                                    'client_id': client_id,
#                                    'ctype': 'DISTRIBUTION',
#                                    'currency': 'RUR',
#                                    'currency_calculation': 1,
#                                    'distribution_contract_type': 3,
#                                    'distribution_products': 'test',
#                                    'distribution_tag': tag_id,
#                                    'download_domains': 'test-balance.ru',
#                                    'firm_id': 1,
#                                    'install_price': Decimal('0.50'),
#                                    'install_soft': 'test-balance.ru',
#                                    'manager_bo_code': 20431,
#                                    'manager_uid': '3692781',
#                                    'nds': '18',
#                                    'parent_contract_id': None,
#                                    'partner_resources': 'test',
#                                    'person_id': person_id,
#                                    'platform_type': None,
#                                    'product_options': 'test',
#                                    'product_search': 'test',
#                                    'product_searchf': 'test',
#                                    'products_currency': 'RUR',
#                                    'products_download': {'909': Decimal('1.50')},
#                                    'products_revshare': {'10000': Decimal('5.50'),
#                                    #                       '10002': Decimal('6.50'),
#                                    #                       '10003': Decimal('7.50'),
#                                    #                       '10004': Decimal('12.50'),
#                                                           '10007': Decimal('13.50'),
#                                    #                       '13002': Decimal('4.38')
#                                                          },
#                                    'products_revshare_scales': {
#                                                         # '10000': 'Test Revshare bucks',
#                                                          '10002': 'Y.Browser Individual 2,66-3,30',
#                                                          '10003': 'Test Revshare bucks',
#                                                          '10004': 'Test Revshare bucks',
#                                                         # '10007': 'Y.Browser Standart',
#                                                          '13002': 'Y.Browser Standart'},
#                                    'reward_type': 1,
#                                    'search_currency': 'RUR',
#                                    'search_price': Decimal('2.50'),
#                                    'service_start_dt': datetime.datetime(2018, 9, 1, 0, 0),
#                                    'signed': 1,
#                                    'start_dt': datetime.datetime(2018, 9, 1, 0, 0),
#                                    'supplements': [1, 2, 3]})
# revshare_types = [distribution_type for distribution_type in DistributionType
#                       if distribution_type.subtype == DistributionSubtype.REVSHARE
#                       ]
#
# places_ids, _ = steps.DistributionSteps.create_places(client_id, tag_id, revshare_types)
# db.balance().execute("BEGIN dbms_mview.refresh('BO.MV_DISTR_CONTRACT_PLACES','C'); END;")
# db.balance().execute("BEGIN dbms_mview.refresh('BO.MV_DIST_CONTRACT_REVSHARE_PROD','C'); END;")
# db.balance().execute("BEGIN dbms_mview.refresh('BO.MV_DIST_CONTRACT_DOWNLOAD_PROD','C'); END;")
# steps.DistributionSteps.create_completions(places_ids, datetime.datetime(2018,10,1))
# steps.DistributionSteps.create_completions(places_ids, datetime.datetime(2018,9,30))



#
# api.medium().GetDistributionRevenueShare(datetime.datetime(2018,9,18), datetime.datetime(2018,9,18), {'place_id': 77247407})

# api.test_balance().MakeOEBSPayment({'InvoiceID': invoice_external_id, 'PaymentSum': 3000})
# api.test_balance().MakePayment(84236447,1500)



# api.medium().GetTaxiBalance([826684])
# api.medium().GetPartnerBalance(Services.TAXI_CORP.id, [782889])
# steps.CommonSteps.export('PROCESS_TAXI', 'Contract', 782889)


# invoice_extrenal_id = 'ЛСО-1259692441-1'
# invoice_id = 83854981
# amount = 4444
# orig_id = 1032980247
# query = "INSERT INTO T_OEBS_CASH_PAYMENT_FACT" \
#                 "(XXAR_CASH_FACT_ID, AMOUNT, RECEIPT_NUMBER, RECEIPT_DATE, OPERATION_TYPE, " \
#                 "LAST_UPDATED_BY, LAST_UPDATE_DATE, CREATED_BY, CREATION_DATE, SOURCE_TYPE, ORIG_ID) VALUES " \
#                 "(S_OEBS_CASH_PAYMENT_FACT_TEST.nextval, :amount, :invoice_eid, :dt, :type, " \
#                 "-1, :dt, -1, :dt, :source_type, :orig_id)"
# params = {
#             'invoice_eid': invoice_extrenal_id,
#             'amount': amount,
#             'dt': datetime.datetime(2017,11,16,1,0,0),
#             'type': 'ACTIVITY',
#             'source_type': 'SBER_YACL',
#             'orig_id': orig_id
#         }
# db.balance().execute(query, params)
#
# steps.CommonSteps.export(Export.Type.PROCESS_PAYMENTS, Export.Classname.INVOICE, invoice_id)

# steps.CommonSteps.export('OEBS', 'CardRegister', 1238121)
# steps.CommonSteps.export('OEBS', 'CardRegister', 1238183)
# steps.CommonSteps.export('OEBS', 'ContractCollateral', 1063120)
# print u'\u0411-1260967209-1'
# i = 0
# while i < 1000000:
#     db.oebs().execute('select XXAR_REESTR_LINES_S.nextval from dual')
#     i=i+1

# db.oebs().execute('select XXAR_REESTR_LINES_S.nextval from (select level from dual connect by level < 1000000)')



# db.meta().execute("update (select * from t_manager where domain_passport_id = 1120000000048750) set domain_login = 'kharitskayayandex-team.ru'")

# db.meta().execute("select * from t_manager where domain_passport_id = 1120000000048750")

# TestBalance.ExecuteSQL('bs_xg', "select value_num from t_config where item='BATCH_EXPORT_ACTIVE'"

# client_id = steps.ClientSteps.create()
# person_id = steps.PersonSteps.create(client_id, 'ph', {'is-partner': '1'})
# steps.CommonSteps.export('OEBS', 'Invoice', 86546977)
# steps.CommonSteps.export('OEBS', 'Act', 90467041)

# api.medium().CreateScale(581155435, dict(code="testik1", comments="комментарий7", type="staircase", service_token='distribution_6e5ef72ae0cc3c399d8edd17026cbc5b'))
# api.medium().GetScale(dict(service_token='distribution_6e5ef72ae0cc3c399d8edd17026cbc5b', code="testik"))


# print ContractSubtype.PARTNERS.name


# test1 = None
# test2 = None
# test3 = None

# steps.api.oebs_gate().server.GetPartnerActHeaders({'ClientID': 889805})
# steps.ClientSteps.create()

# steps.CommonPartnerSteps.generate_partner_acts_fair(1026982, datetime.datetime(2018,12,1))

# query = "update t_tax_policy_pct set dt = :dt where id = 325"
# params = {'dt': datetime.datetime(2018,11,1)}
# db.meta().execute(query, params)

# api.medium().GetDspStat(datetime.datetime(2018,11,30), datetime.datetime(2018,11,30))


#
# client_id = steps.ClientSteps.create()
# steps.PersonSteps.create(client_id, 'ur', {'is-partner': '1'})
# api.medium().GetClientPersons(client_id)

# steps.InvoiceSteps.pay(83676647, 100000)
# steps.TaxiSteps.process_taxi(540122, datetime.datetime(2019,3,24,0,0,0))
# steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(106353526, 1415179, datetime.datetime(2019,2,1))

# steps.ExportSteps.export_oebs(client_id=106353526, contract_id=1415179,  transaction_id='20003483192', invoice_id=85508134,
#                               act_id=87843602)
# steps.CommonSteps.export('MONTH_PROC', 'Client', '105434681')

# steps.TaxiSteps.create_cash_payment_fact(u'ЛСТ-1477120778-1', 10, datetime.datetime(2018,11,1), 'INSERT')
# process_payment(87603994)

# print Nds.DEFAULT.pct_on_dt(datetime.datetime(2018,12,31))
# print round(499 * (Decimal('1') + 18 / Decimal('100')), 2)
# print round((500 - Decimal(499)) * (Decimal('1') + 18 / Decimal('100')), 2)

# steps.CommonPartnerSteps.generate_partner_acts_fair(1416863, datetime.datetime(2019,1,1))

# steps.CommonSteps.export('MONTH_PROC', 'Client', '36499865')


# steps.ExportSteps.create_export_record(1033766993, classname=Export.Classname.PAYMENT, type=Export.Type.THIRDPARTY_TRANS)
# steps.CommonSteps.export(Export.Type.THIRDPARTY_TRANS, Export.Classname.PAYMENT, '1033766993')

#18-01-19 17:26:26
# import time
# dt = datetime.datetime(2019,1,18,17,26,26)
# print time.mktime(dt.timetuple())

# steps.ExportSteps.export_oebs(client_id=105867713, contract_id=1211109, invoice_id=85113951)
#1209337
# steps.ExportSteps.export_oebs(invoice_id=85109898)
# steps.ExportSteps.export_oebs(transaction_id='20001779131')
# steps.CommonSteps.export('OEBS', 'Invoice', 85085685)
# client_id = steps.ClientSteps.create()
# person_id = steps.PersonSteps.create(client_id, 'ur')
# api.medium().CreateCommonContract(PASSPORT_ID,
#                                      {'client_id': client_id,
#                                       'country': Regions.BY.id,
#                                       'currency': Currencies.BYN.char_code,
#                                       'firm_id': Firms.UBER_115.id,
#                                       'manager_uid': '244916211',
#                                       'partner_commission_pct2': Decimal('0'),
#                                       'payment_type': 2,
#                                       'person_id': person_id,
#                                       'personal_account': 1,
#                                       'region': None,
#                                       'services': [128, 124, 111, 125, 605],
#                                       'signed': 1
#                                       })

# api.test_balance().ExecuteOEBS(1, u"select k_header_id from apps.oke_k_headers_full_v where k_alias = '696056/19' and authoring_org_id = '121'",
#                                              {})

# api.test_balance().ExecuteOEBS(115, 'SELECT * FROM apps.xxoke_contract_find_v WHERE reference_id = :balance_collateral_id',
#                                {'balance_collateral_id': 1525718})

# k_header_id = api.test_balance().ExecuteOEBS(115,u"select k_header_id from apps.oke_k_headers_full_v where k_alias = '695946/19/BYN'",
#                                              {})[0]['k_header_id']
# api.test_balance().ExecuteOEBS(115, 'SELECT * FROM apps.okc_k_headers_b WHERE id = :oebs_contract_id', {'oebs_contract_id': k_header_id})
#


# api.test_balance().ExecuteOEBS(1, 'SELECT * FROM apps.xxoke_contract_find_v WHERE reference_id = :balance_collateral_id',
#                                              {'balance_collateral_id': 1526034})

# steps.CommonPartnerSteps.generate_partner_acts_fair(1198667, datetime.datetime(2018,12,1))


# api.medium().GetPersonalAccount({'FirmID': 1, 'PaysysID': 1128, 'PersonID': 9424304, 'ProductID': 508899})



from btestlib.data.partner_contexts import TAXI_UBER_BV_BY_BYN_CONTEXT, CORP_TAXI_KZ_CONTEXT_SPENDABLE, \
    CORP_TAXI_KZ_CONTEXT_GENERAL, TAXI_KZ_CONTEXT, TAXI_BV_GEO_USD_CONTEXT

# client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(TAXI_UBER_BV_BY_BYN_CONTEXT)
# steps.ExportSteps.export_oebs(client_id=106311801, contract_id=1398820)
# steps.ExportSteps.export_oebs(invoice_id=85473927)
# steps.ExportSteps.export_oebs(invoice_id=85473926, act_id=87834366)
# steps.ActsSteps.hide(87834240)
# steps.ExportSteps.export_oebs(act_id=87834241)
# from btestlib import secrets
# from simpleapi.data.uids_pool import User
# user = User(4009740922, 'osiei1', secrets.get_secret(*secrets.UsersPwd.CLIENTUID_PWD))
# steps.UserSteps.link_user_and_client(user, 106306961)

# _, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(TAXI_KZ_CONTEXT, client_id=106307120)
# _, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(TAXI_BV_GEO_USD_CONTEXT, client_id=106307120)

# api.test_balance().ExportObject('OEBS', 'ContractCollateral', 1762142, 0, None, None)
# steps.ExportSteps.export_oebs(contract_id = 1615352)
# steps.CommonSteps.export(Export.Type.OEBS, 'CurrencyRate', 460035)

# contracts = db.balance().execute("select contract_id from t_invoice where id in (select object_id from t_export where type = 'OEBS' and state = 0 and classname = 'Invoice' and error like 'Contract%')")
# for contract in contracts:
#     steps.ExportSteps.export_oebs(contract_id=contract['contract_id'])

# clients = db.balance().execute("select distinct client_id from t_invoice "
#                                  "where id in (select object_id from t_export "
#                                  "where type = 'OEBS' and state = 0 and classname = 'Invoice' "
#                                  "and error like 'Contract%')")
# for client in clients:
#     steps.ExportSteps.export_oebs(client_id=client['client_id'])

# steps.ActsSteps.generate(106278398, force=1, date=datetime.datetime(2019,2,28))
# api.medium().GetClientCreditLimits(106278398, 508643)

# api.medium().CreateInvoice(passport_uid, request_params)

# api.medium().CreateInvoice2(16571028, {'ContractID': 1386905, 'Credit': 1,
#                                        'Overdraft': 0, 'PaysysID': 11101003, 'PersonID': 9645428,
#                                        'RequestID': 1266532017})


# client_id = steps.ClientSteps.create()
# steps.PersonSteps.create(client_id, 'ur')
# steps.PersonSteps.create(client_id, 'il_ur', {'is-partner': '1'})

# steps.ExportSteps.export_oebs(client_id=106862444, contract_id=1636614, correction_id='20011818739')
# orders_list = [{'ServiceID': 111, 'ServiceOrderID': '20000001950015', 'Qty': 50,
#                         'BeginDT': datetime.datetime.now()}]
# request_id = steps.RequestSteps.create(106845626, orders_list)

# api.medium().GetTaxiBalance([1640006])
# api.test_balance().CacheBalance(1640006)
# steps.InvoiceSteps.pay(85958356, 900)
# steps.TaxiSteps.process_taxi(1640006)

# api.medium().GetTaxiBalance([1640021])
# api.test_balance().CacheBalance(1640021)

# source = 'red_market'
# start_dt = datetime.datetime(2019,5,1)
# end_dt = datetime.datetime(2019,5,13)
# api.test_balance().GetPartnerCompletions({'start_dt': start_dt, 'end_dt': end_dt, 'completion_source': source})


# steps.ExportSteps.export_oebs(contract_id=1622221)


# steps.ExportSteps.export_oebs(client_id=106893375, contract_id=1648617, invoice_id=85975320, act_id=87974347)
# steps.ExportSteps.export_oebs(contract_id=1681357)


# import simpleapi.steps.simple_steps as simpleapi_steps
# from btestlib.data import simpleapi_defaults
# service = Services.EVENTS_TICKETS
# user_ip = simpleapi_defaults.DEFAULT_USER_IP
# user = simpleapi_defaults.DEFAULT_USER
#
# purchase_token = '90415e92b355f8d8e5984507be93e547'

# trust_payment_id = '5cb5fbf5910d392927515893'
# purchase_token = '048ec2dd42c07f759087ad9452bc5313'

# simpleapi_steps.check_basket(service, purchase_token=purchase_token)


# api.medium().GetPartnerContracts({'ClientID': 42556796})
# api.medium().GetPartnerContracts({'ExternalID': 'ОФ-70103'})
# api.medium().GetPartnerContracts({'ContractID': '490925'})




# from btestlib.data.partner_contexts import TAXI_ISRAEL_CONTEXT, TAXI_ISRAEL_CONTEXT_SPENDABLE
# client_id = steps.ClientSteps.create()
# person_id = steps.PersonSteps.create(client_id, 'il_ur', {"il-id": "888888777"})
# _, _, contract_id, _ = steps.ContractSteps.create_partner_contract(TAXI_ISRAEL_CONTEXT, client_id=client_id,
#                                                                                    person_id=person_id)
# steps.ExportSteps.export_oebs(client_id=107033483, contract_id=1709798)
# spendable_person_id = steps.PersonSteps.create(client_id, 'il_ur', {"is-partner": '1', "il-id": "999111666"})
# _, _, spendable_contract_id, _ = \
#     steps.ContractSteps.create_partner_contract(TAXI_ISRAEL_CONTEXT_SPENDABLE, client_id=client_id, person_id=spendable_person_id,
#                                                 additional_params={'link_contract_id': contract_id})
# steps.ExportSteps.export_oebs(contract_id=spendable_contract_id)
#
# api.test_balance().ExecuteOEBS(35, "select ca.business_id, ca.* from apps.xxar_customer_attributes ca where ca.CUSTOMER_ID=(select customer_id from apps.xxar_customers cu where cu.CUSTOMER_number='P'||:person_id)",
#                                              {'person_id': person_id})
# api.test_balance().ExecuteOEBS(35, "select ca.business_id, ca.* from apps.xxar_customer_attributes ca where ca.CUSTOMER_ID=(select customer_id from apps.xxar_customers cu where cu.CUSTOMER_number='P'||:person_id)",
#                                              {'person_id': spendable_person_id})

# steps.ExportSteps.export_oebs(person_id=person_id)
# steps.ExportSteps.export_oebs(person_id=spendable_person_id)
#
# api.test_balance().ExecuteOEBS(35, "select ca.business_id, ca.* from apps.xxar_customer_attributes ca where ca.CUSTOMER_ID=(select customer_id from apps.xxar_customers cu where cu.CUSTOMER_number='P'||:person_id)",
#                                              {'person_id': person_id})
# api.test_balance().ExecuteOEBS(35, "select ca.business_id, ca.* from apps.xxar_customer_attributes ca where ca.CUSTOMER_ID=(select customer_id from apps.xxar_customers cu where cu.CUSTOMER_number='P'||:person_id)",
#                                              {'person_id': spendable_person_id})


# api.test_balance().ExecuteOEBS(120, u"select k_header_id from apps.oke_k_headers_full_v where k_alias = 'ОФ-98557' and authoring_org_id = '201'",
#                                              {})
# person_id=10134543
# spendable_person_id=10134544


# sources = db.balance().execute("select id from t_currency_rate_v2 where rate_dt > date'2019-04-01' "
#                                "and cc = 'EUR' and base_cc = 'USD'")
# sources = db.balance().execute("select * from t_currency_rate_v2 where rate_dt >= date'2019-04-01' "
#                                "and cc = 'USD' and base_cc = 'BYN'")
# sources = db.balance().execute("select id from t_currency_rate_v2 where rate_dt >= date'2019-04-01'")
# sources = [490238]
# for source in sources:
#     steps.CommonSteps.export('OEBS', 'CurrencyRate', source)
# for source in sources:
#     steps.CommonSteps.export('OEBS', 'CurrencyRate', source['id'])
# steps.CommonSteps.export('OEBS', 'CurrencyRate', '486432')
# steps.ExportSteps.export_oebs(contract_id=982859)
# steps.ExportSteps.export_oebs(client_id = 107101625, contract_id=798343)
# steps.ExportSteps.create_export_record('17690076928', 'ThirdPartyTransaction', 'OEBS')
# steps.CommonSteps.export('OEBS', 'ThirdPartyTransaction', '17690076928')
# steps.ExportSteps.export_oebs(contract_id=2040621)
# steps.ExportSteps.export_oebs(transaction_id='17690574119')
# steps.ExportSteps.export_oebs(act_id=98067967)
# steps.ExportSteps.export_oebs(act_id=98067968)
# steps.ExportSteps.export_oebs(act_id=98067969)

invoices = [
    # 93016623,
89960650,
90780509,
92343660,
92706278,
92766220,
92810277,
93112398]

# for invoice in invoices:
#     steps.CommonSteps.export('OEBS', 'Invoice', invoice)

# steps.ExportSteps.export_oebs(person_id=spendable_person_id)
#
# api.test_balance().ExecuteOEBS(35, "select ca.business_id, ca.* from apps.xxar_customer_attributes ca where ca.CUSTOMER_ID=(select customer_id from apps.xxar_customers cu where cu.CUSTOMER_number='P'||:person_id)",
#                                              {'person_id': person_id})
# api.test_balance().ExecuteOEBS(33, "select attribute14, attribute7 from apps.xxar_customers where customer_number = 'P8499815'")

# api.test_balance().ExecuteOEBS(1, "select * from ra_customer_trx_all where trx_number = '126667914'")
# pass
# steps.TaxiSteps.process_taxi(798343, datetime.datetime(2019,5,13))

# api.medium().GetRequestChoises(16571028, {'ContractID': 868991,
#  'OperatorUid': 16571028,
#  'PersonID': 8610495,
#  'RequestID': 1721379566})

# api.medium().GetRequestChoices({'OperatorUid': 16571028, 'RequestID': 1721379566, 'ContractID': 868991})

# steps.CommonSteps.export('OEBS', 'Person', 8485297)
# steps.CommonSteps.export('OEBS', 'ThirdPartyTransaction', '18616264609')

# for payment in [1748290375]:
# steps.CommonSteps.export('THIRDPARTY_TRANS', 'Payment', 1953226687)
# steps.CommonSteps.export('OEBS', 'ThirdPartyTransaction', '2747979093169')

# client_id = steps.ClientSteps.create()
# steps.PersonSteps.create(client_id, 'ur')
# api.medium().UpdatePayment({'TrustPaymentID': '5cd03235910d397850fe1c6f'}, {'PayoutReady': datetime.datetime(2019,6,13,2)})

# api.medium().GetDistributionMoney(datetime.datetime(2019,6,1), datetime.datetime(2019,6,1), 10000)

# api.medium().GetDistributionActed(datetime.datetime(2019,5,1), 44239398)
from balance.balance_steps import new_taxi_steps as taxi_steps
# taxi_steps.TaxiSteps.process_payment(96491031, True)


# steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(108051272, 2131281, datetime.datetime(2019,6,1))
# query="select input from t_export where object_id=502246 and type = 'PARTNER_ACTS'"
# print steps.CommonSteps.get_pickled_value(query, 'input')


client_id = steps.ClientSteps.create()
person_id = steps.PersonSteps.create(client_id, 'ur')
person_id1 = steps.PersonSteps.create(client_id, 'ur', {'is-partner': '1'})
steps.PersonSteps.create(client_id, 'ph')
# api.medium().GetAvailablePersonCategories({'ClientID': client_id, 'ServiceID': 129})
# person_id = steps.PersonSteps.create(client_id, 'by_ytph')
# person_id2 = steps.PersonSteps.create(client_id, 'ur')
# # steps.PersonSteps.create(client_id, 'ph')
#
# client_id = 133883224
# service_order_id = steps.OrderSteps.next_id(7)
#
# order_id = steps.OrderSteps.create(client_id, service_order_id, product_id=1475, service_id=7)
#
# service_order_id1 = steps.OrderSteps.next_id(7)
# order_id = steps.OrderSteps.create(client_id, service_order_id1, product_id=1475, service_id=7)
# orders_list = [{'ServiceID': 7, 'ServiceOrderID': service_order_id, 'Qty': 1},
#                {'ServiceID': 7, 'ServiceOrderID': service_order_id1, 'Qty': 10}
#                ]
#
# request_id = steps.RequestSteps.create(client_id, orders_list)
# invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, 1003, credit=0, overdraft=0, endbuyer_id=None)
# steps.InvoiceSteps.turn_on(invoice_id)
# steps.CampaignsSteps.do_campaigns(7, service_order_id, {'Bucks': 50}, 0)
#
# request_id1 = steps.RequestSteps.create(client_id, orders_list)
# invoice_id1, _, _ = steps.InvoiceSteps.create(request_id1, person_id1, 1003, credit=0, overdraft=0, endbuyer_id=None)
# steps.InvoiceSteps.pay(invoice_id1)
# steps.CampaignsSteps.do_campaigns(7, service_order_id, {'Bucks': 100}, 0)
#
# request_id1 = steps.RequestSteps.create(client_id, orders_list)
# invoice_id1, _, _ = steps.InvoiceSteps.create(request_id1, person_id2, 1003, credit=0, overdraft=0, endbuyer_id=None)
# # steps.InvoiceSteps.pay(invoice_id1)
# steps.InvoiceSteps.pay_fair(invoice_id1)
# steps.CampaignsSteps.do_campaigns(7, service_order_id, {'Bucks': 150}, 0)
# steps.ActsSteps.generate(client_id, force=1)

# api.test_balance().SyncRevPartnerServices()

# api.test_balance().ExecuteSQL('bs_new', "select * from t_config where item='BATCH_EXPORT_ACTIVE'")
# print u'\u0414\u043b\u044f \u043f\u043e\u0437\u0438\u0446\u0438\u0438 \u0441 ID=510039 ( \u0422\u0415\u0421\u0422 \u0423\u0441\u043b\u0443\u0433\u0430 \u043f\u043e \u043f\u0440\u0435\u0434\u043e\u0441\u0442\u0430\u0432\u043b\u0435\u043d\u0438\u044e \u0434\u043e\u0441\u0442\u0443\u043f\u0430 \u043a \u0424\u0443\u043d\u043a\u0446\u0438\u0438 \u0441\u043a\u0430\u043d\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u044f \u043d\u0430 \u0421\u0435\u0440\u0432\u0438\u0441\u0435 ) \u0443\u043a\u0430\u0437\u0430\u043d \u043d\u0435 \u0441\u0443\u0449\u0435\u0441\u0442\u0432\u0443\u044e\u0449\u0438\u0439 ID \u0441\u0435\u0440\u0432\u0438\u0441\u0430!'

# login_template = 'yndx-balance-dev-{}'
# for num in xrange(1, 11):
#     login = login_template.format(num)
#     data = steps.api.medium().GetPassportByLogin(0, login)

# steps.PartnerSteps.create_direct_partner_completion(77222398, datetime.datetime(2019,8,30))

# steps.CommonPartnerSteps.generate_partner_acts_fair(10169445, datetime.datetime(2019,8,30))


# print utils.Date.first_day_of_month()

# api.medium().GetPartnerContracts({'ClientID': '485371'})

# api.medium().GetClientPersons(485371, 1)

# api.medium().EstimateDiscount({'ClientID': 109697301, 'ContractID': 10131002, 'PaysysID': 2701101},
#                                            [{'BeginDT': datetime.datetime(2018, 3, 11, 0, 0), 'ClientID': 109697302, 'ID': 1, 'ProductID': 507529, 'Qty': 100, 'RegionID': None, 'discard_agency_discount': 0}])
#

# api.test_balance().ExecuteOEBS(1, u"select * from apps.ra_customer_trx_all where trx_number = '140984724'", {})

# api.medium().CreateInvoice(16571028,
#                       {'ContractID': 982679, 'Credit': 1, 'Overdraft': 0, 'PaysysID': 1003, 'PersonID': 9382359, 'RequestID': '1919090122'})

# client_id, person_id, tag_id = steps.DistributionSteps.create_distr_client_person_tag()
# params = {'client_id': client_id,
#           'ctype': 'DISTRIBUTION',
#           'currency': 'RUR',
#           'currency_calculation': 1,
#           'distribution_contract_type': 3,
#           'distribution_tag': tag_id,
#           'firm_id': 1,
#           'manager_bo_code': 20431,
#           'manager_uid': '3692781',
#           'nds': '18',
#           'person_id': person_id,
#           'test_mode': 1,
#           'reward_type': 1,
#           'service_start_dt': datetime.datetime(2019,12,1),
#           'start_dt': datetime.datetime(2019,12,1),
#           'supplements': [3],
#           'search_price': 12,
#           'search_currency': 'RUR'}
# api.medium().CreateCommonContract(PASSPORT_ID,params)

# api.test_balance().HideAct(118898865)
# api.test_balance().UnhideAct(118898865)

# promocodes = ['ABCD-ABCD-ABCD-1114']
# api.medium().ImportPromoCodes([
#     {
#         'CalcClassName': 'ActBonusPromoCodeGroup',  # тип промокодов
#         'CalcParams': {
#             'adjust_quantity': True,  # увеличиваем количество (иначе уменьшаем сумму)
#             'apply_on_create': True,  # применяем при создании счёта или при включении (оплате)
#             'act_bonus_pct': '15', # % бонуса от суммы актов за расчётный период
#             'min_act_amount': '300',  # минимальная сумма актов для получения бонуса
#             'max_bonus_amount': '1000',  # больше этой суммы мы не даем
#             # если мы используем adjust_quantity=False, то рискуем, что сумма бонуса получится больше, чем выставляемый счёт,
#             # так что процент скидки нужно ограничить
#             'max_discount_pct': '99',
#             'currency': 'RUB',
#             'act_month_count': 1,  # количество месяц для расчёта скидки по актам
#             # 'act_product_ids': [1475],  # фильтр актов по продуктам в заказах
#         },
#         'StartDt': datetime.datetime(2020, 04, 01, 0, 0),  # дата начала действия промокода
#         'EndDt': datetime.datetime(2021, 04, 30, 0, 0),  # дата окончания действия промокода
#         'Promocodes': [  # собственно сами коды
#             {'client_id': None, 'code': code}
#             for code in promocodes
#         ],
#         'FirmId': 1,  # фильтр по фирме и для счёта, который получит промокод, и для актов, по которым будет расчитана скидка
#         'ServiceIds': [7],  # аналогично FirmId
#         # 'ProductIds': [111, 222],  # только для этих продуктов можно выставить счёт с промокодом
#         'ReservationDays': 90,  # количество дней резервации промокода за клиентом. лучше установить не менее (EndDt - StartDt)
#         'IsGlobalUnique': True,  # может быть использован только в 1 счёте
#         'MinimalAmounts': {},  # минимальные суммы для счёта, который получит промокод
#         'NeedUniqueUrls': False,  # нужны уникальные url. для ActBonusPromoCodeGroup надо оставить False
#         'NewClientsOnly': False,  # для ActBonusPromoCodeGroup не поддерживается
#         'TicketId': 'FAKE-1',
#         'ValidUntilPaid': False,  # может быть использовано 1 клиентом в 1 счёте
#         },
# ])


# person_type = "yt"
# PERSON_YT_PARAMS = {u'name': u'YT legal Payer',
#                     u'inn': u'584759476',
#                     u'account': u'100548'}
# client_id = steps.ClientSteps.create(params=None)
# params = PERSON_YT_PARAMS
# params = params.copy()
# params.update({'is-partner': "0"})
# person_id = steps.PersonSteps.create(client_id, person_type, params)
steps.ClientSteps.create()