# -*- coding: utf-8 -*-
__author__ = 'atkaya'

import datetime

import balance.balance_db as db
from balance import balance_api as api
from balance import balance_steps as steps
from balance.distribution.distribution_types import DistributionType
from btestlib.data import defaults as default

PASSPORT_ID = default.PASSPORT_UID


def createClientandPlace():
    dt = datetime.datetime(2016, 1, 1, 0, 0, 0)
    completion_dt = datetime.datetime(2016, 6, 8, 0, 0, 0)
    client_id, person_id, tag_id = steps.DistributionSteps.create_distr_client_person_tag()
    contract_id, external_id = steps.ContractSteps.create_contract('universal_distr',
                                                                   {'CLIENT_ID': client_id, 'PERSON_ID': person_id,
                                                                    'DT': dt,
                                                                    'DISTRIBUTION_TAG': tag_id})

    steps.DistributionSteps.create_places_and_completions(client_id, tag_id, contract_id, completion_dt)
    #steps.CommonSteps.log(api.medium().get_distribution_fixed)(completion_dt, completion_dt)
    # start_dt = datetime.datetime(2016, 2, 1, 0, 0, 0)
    # steps.CommonPartnerSteps.acts_enqueue(start_dt, contract_id)
    # steps.CommonPartnerSteps.acts_enqueue(datetime.datetime(2015, 11, 1, 0, 0, 0), datetime.datetime(2015, 11, 30, 0, 0, 0), [255654])

def distribution_fixed_data_filter(data, place_id = None):
    splitted = data.split('\n')
    header = tuple(splitted[0].split('\t'))
    filtered = []
    for line in splitted[1:]:
        row = tuple(line.split('\t'))
        if row[1] == place_id:
            filtered.append(dict(zip(header,row)))
    return filtered

def distribution_fixed_data_filter_page(page_id = 2080):
    completion_dt = datetime.datetime(2016, 5, 1, 0, 0, 0)
    data = steps.CommonSteps.log(api.medium().get_distribution_fixed)(completion_dt, completion_dt)
    splitted = data.split('\n')
    header = tuple(splitted[0].split('\t'))
    filtered = []
    for line in splitted[1:]:
        row = tuple(line.split('\t'))
        if row[1] == page_id:
            filtered.append(dict(zip(header,row)))
    return filtered

def check_acts_clicks_vid():
    payment_type = DistributionType.CLICKS.payment_type
    start_dt, end_dt = steps.CommonSteps.previous_month_first_and_last_days()
    client_id, person_id, tag_id = steps.DistributionSteps.create_distr_client_person_tag()
    contract_id, external_id = steps.ContractSteps.create_contract('universal_distr_empty',
                                                                   {'CLIENT_ID': client_id, 'PERSON_ID': person_id,
                                                                    'DT': start_dt,
                                                                    'DISTRIBUTION_TAG': tag_id,
                                                                    'SERVICE_START_DT': start_dt,
                                                                    'PRODUCTS_CURRENCY': 643, 'ADVISOR_PRICE': 55})
    place_id, search_id = steps.DistributionSteps.create_distr_place(client_id, tag_id, payment_type)
    steps.DistributionSteps.create_distr_completion(place_id, start_dt,completion_sum=500)
    steps.DistributionSteps.create_distr_completion(place_id, start_dt,vid=123,completion_sum=10000)
    steps.DistributionSteps.create_distr_completion(place_id, start_dt,vid=0,completion_sum=25000)

    steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, start_dt)
    steps.CommonSteps.restart_pycron_task('generate-partner-acts')

    sql = "select state as val from t_export where type = 'PARTNER_ACTS' and object_id = :contract_id"
    sql_params = {'contract_id': contract_id}
    steps.CommonSteps.wait_for(sql, sql_params, value = 1)

    #проверяем вознаграждение
    query_get_partner_reward = "select round(partner_reward_wo_nds,5) reward from t_partner_act_data where partner_contract_id = :contract_id and DESCRIPTION = 'Дистрибуция.Клики Советник'"
    params_invoice = {'contract_id': contract_id}
    reward = db.balance().execute(query_get_partner_reward, params_invoice)[0]['reward']
    print reward

    #expected_direct_reward = 1093.22034
    #assert str(reward_direct) == str(expected_direct_reward), 'Direct reward is incorrect!'

# createClientandPlace()
# check_acts_clicks_vid()
#data = 'PAGE_ID\tPLACE_ID\tVID\tDT\tTAG_ID\tCONTRACT_ID\tPARTNER\tPARTNER_WO_NDS\tSHOWS\tCLIENT_ID\tCURRENCY\n11157860\t909\t\t2015-12-08 00:00:00\t2085\t263558\t21240\t18000\t300\t9024397\tRUR\n102688\t3010\t4\t2015-12-08 00:00:00\t1190\t227536\t0.33\t0.33\t3\t1859232\tUSD\n102688\t3010\t8\t2015-12-08 00:00:00\t1190\t227536\t0.22\t0.22\t2\t1859232\tUSD\n102688\t3010\t213\t2015-12-08 00:00:00\t1190\t227536\t0.11\t0.11\t1\t1859232\tUSD\n11157861\t2080\t\t2015-12-08 00:00:00\t2085\t263558\t16520\t14000\t200\t9024397\tRUR\n11157862\t3010\t\t2015-12-08 00:00:00\t2085\t263558\t66080\t56000\t700\t9024397\tRUR\n102688\t3010\t\t2015-12-08 00:00:00\t1190\t227536\t0.22\t0.22\t2\t1859232\tUSD\n102688\t3010\t1\t2015-12-08 00:00:00\t1190\t227536\t599.17\t599.17\t5447\t1859232\tUSD\n102688\t3010\t2\t2015-12-08 00:00:00\t1190\t227536\t0.88\t0.88\t8\t1859232\tUSD\n102688\t3010\t6\t2015-12-08 00:00:00\t1190\t227536\t0.11\t0.11\t1\t1859232\tUSD\n11157859\t10001\t\t2015-12-08 00:00:00\t2085\t263558\t23600\t20000\t400\t9024397\tRUR'
#res = distribution_fixed_data_filter_page()
#print res

#createClientandPlace()
#check_acts_clicks_vid()
# steps.CommonPartnerSteps.acts_enqueue(datetime.datetime(2016,1,1,0,0,0), datetime.datetime(2016,1,31,0,0,0), [306085])

#steps.CommonPartnerSteps.acts_enqueue(datetime.datetime(2015, 11, 1, 0, 0, 0), datetime.datetime(2015, 11, 30, 0, 0, 0), [255654])
# client_id, person_id, tag_id = steps.DistributionSteps.create_distr_client_person_tag()

# client_id = 11487846
# passport_uid = 16571028
# client_id = steps.ClientSteps.create()
# tag_id = db.balance().execute("select max(id) from t_distribution_tag")[0]['MAX(ID)'] + 1
# api.medium().CreateOrUpdateDistributionTag(PASSPORT_ID,
#                                                                {'TagID': tag_id, 'TagName': 'CreatedByScriptNew2',
#                                                                 'ClientID': client_id})
# person_id = steps.PersonSteps.create(client_id, 'sw_yt', {'is-partner': '1'})


# steps.DistributionSteps.create_places_and_completions(14345648, 2697, 442436, datetime.datetime(2015, 4, 1, 0, 0, 0))
#check_acts_clicks_vid()

# dt = datetime.datetime(2016,12,1,0,0,0)
# client_id = 32445715
# data = api.medium().GetDistributionActed(dt, client_id)
# print data
#
#
# lst = data.split("\n")
# t = len(lst[1:])
# print t

#api.Medium().GetTaxiBalance([270225])

#createClientandPlace()

# contract_id = 271534
# start_dt = datetime.datetime(2016, 2, 1, 0, 0, 0)
# steps.CommonPartnerSteps.acts_enqueue(start_dt, [271534])

#api.test_balance().GeneratePartnerAct(263507, datetime.datetime(2016,1,1,0,0,0))



# completion_dt = datetime.datetime(2016, 5, 2, 0, 0, 0)
# steps.DistributionSteps.create_places_and_completions(11487846, 2172, 262102, completion_dt)

# steps.CampaignsSteps.do_campaigns(7, 41246338, {'Bucks': 20})


# steps.CommonSteps.export('OEBS', 'ContractCollateral', 297479)

# api.medium().CreateOrUpdatePartner(16571028, {'client_id': 9774131, 'mode': 'form', 'pers'})

# api.medium().CreateOrUpdatePartner(16571028, {'ben-account': "Test Test Test",'birthday': "1988-11-02",
#                                               'city': "Kiev",'client-id': 9774480,'email': "testagpi2@yandex.ru",
#                                               'fname': "Test",'lname': "Test",'name': "2efwef",
#                                               'mode': "edit",'passport-id': "168480136", 'mname': '',
#                                               'phone': "+7 905 1234567",'postaddress': "efwg, 234",
#                                               'postcode': 122112,'region': 187,'type': "ph",
#                                               'pfr': '142-539-156 54', 'inn': '344405345607',
#                                               'legal-address-gni': 'defdf', 'legal-address-region': 'erge',
#                                               'legal-address-code': '344444', 'legal-address-postcode': '324324',
#                                               'address-gni': 'sdsd', 'address-region': '23', 'address-postcode': '2323',
#                                                'address-code': '7601200002300', 'passport-d': '2015-11-12', 'passport-s': '1815',
#                                               'passport-e': '234234', 'passport-code': '34234', 'bank-type': '3',
#                                               'passport-n': '160994', 'bik': '044525225',
#                                               'account': '40817810211000060246', 'yamoney-wallet': '324324342'
#                                               })

# createClientandPlace()
# client_id = 13423430
# tag_id = 2264
# # contract_id = 381102
# contract_id = 384728 #второй договор с тем же клиентом
# completion_dt = datetime.datetime(2016,5,1,0,0,0)
# completion_sum_direct = 97000000
# completion_sum_taxi = 49000000
# completion_sum_market = 63000000
# completion_sum_clicls = 190
# completion_sum_downloads = 70
# completion_sum_installs = 135
# completion_sum_activations = 200
# completion_sum_searches = 9100
# steps.DistributionSteps.create_places_and_completions(client_id, tag_id, contract_id, completion_dt)
#
# # steps.DistributionSteps.create_distr_place(client_id, tag_id, 4, update_mv=False)
#
# #директ
# steps.DistributionSteps.create_distr_completion(11159868, completion_dt, completion_sum=completion_sum_direct, distr_type='Direct')
# #такси
# steps.DistributionSteps.create_distr_completion(11159869, completion_dt, completion_sum=completion_sum_taxi, distr_type='Taxi')
# #маркет
# steps.DistributionSteps.create_distr_completion(11159870, completion_dt, completion_sum=completion_sum_market)
# #installs
# steps.DistributionSteps.create_distr_completion(11159871, completion_dt, completion_sum=completion_sum_installs)
# #downloads
# steps.DistributionSteps.create_distr_completion(11159872, completion_dt, completion_sum=completion_sum_downloads)
# #clicks
# steps.DistributionSteps.create_distr_completion(11159873, completion_dt, completion_sum=completion_sum_clicls)
# #activations
# steps.DistributionSteps.create_distr_completion(11159874, completion_dt, completion_sum=completion_sum_activations)
# #searches
# steps.DistributionSteps.create_distr_completion(11159875, completion_dt, completion_sum=completion_sum_searches)



# steps.DistributionSteps.create_distr_completion(place_id, start_dt,vid=123,completion_sum=10000)
# steps.DistributionSteps.create_distr_completion(place_id, start_dt,vid=0,completion_sum=25000)

# dt_close_month = datetime.datetime(2016,6,1,0,0,0)
# steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, dt_close_month)


# client_id = 13427093
# tag_id_1 = 2265
# tag_id_2 = 2266

# steps.DistributionSteps.create_distr_place(client_id, tag_id_2, 4, update_mv=False)

# completion_dt = datetime.datetime(2016,5,1,0,0,0)
# completion_sum = 10000
# #директ
# steps.DistributionSteps.create_distr_completion(11158241, completion_dt, completion_sum=completion_sum, distr_type='Direct')
# #такси
# steps.DistributionSteps.create_distr_completion(11158242, completion_dt, completion_sum=completion_sum, distr_type='Taxi')
# #маркет
# steps.DistributionSteps.create_distr_completion(11158243, completion_dt, completion_sum=completion_sum)
# #installs
# steps.DistributionSteps.create_distr_completion(11158244, completion_dt, completion_sum=completion_sum)
# #downloads
# steps.DistributionSteps.create_distr_completion(11158245, completion_dt, completion_sum=completion_sum)
# #clicks
# steps.DistributionSteps.create_distr_completion(11158246, completion_dt, completion_sum=completion_sum)
# #activations
# steps.DistributionSteps.create_distr_completion(11158247, completion_dt, completion_sum=completion_sum)
# #searches
# steps.DistributionSteps.create_distr_completion(11158248, completion_dt, completion_sum=completion_sum)
#
# #директ
# steps.DistributionSteps.create_distr_completion(11158249, completion_dt, completion_sum=completion_sum, distr_type='Direct')
# #такси
# steps.DistributionSteps.create_distr_completion(11158250, completion_dt, completion_sum=completion_sum, distr_type='Taxi')
# #маркет
# steps.DistributionSteps.create_distr_completion(11158251, completion_dt, completion_sum=completion_sum)
# #installs
# steps.DistributionSteps.create_distr_completion(11158252, completion_dt, completion_sum=completion_sum)
# #downloads
# steps.DistributionSteps.create_distr_completion(11158253, completion_dt, completion_sum=completion_sum)
# #clicks
# steps.DistributionSteps.create_distr_completion(11158254, completion_dt, completion_sum=completion_sum)
# #activations
# steps.DistributionSteps.create_distr_completion(11158255, completion_dt, completion_sum=completion_sum)
# #searches
# steps.DistributionSteps.create_distr_completion(11158256, completion_dt, completion_sum=completion_sum)


# dt_close_month = datetime.datetime(2016,5,1,0,0,0)
# contract_id = 381184
# steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, dt_close_month)





# completion_dt = datetime.datetime(2016,2,1,0,0,0)
# steps.DistributionSteps.create_distr_completion(11158315, completion_dt, completion_sum=1000)

# dt_close_month = datetime.datetime(2016,6,1,0,0,0)
# contract_id = 384734
# steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, dt_close_month)

# steps.CommonPartnerSteps.generate_partner_acts_fair(241390, datetime.datetime(2016,5,1,0,0,0))

# db.balance().execute("begin dbms_mview.refresh('BO.MV_DISTR_CONTRACT_PLACES','C'); end;")
# db.balance().execute("begin dbms_mview.refresh('BO.MV_DIST_CONTRACT_DOWNLOAD_PROD','C'); end;")
# db.balance().execute("begin dbms_mview.refresh('BO.MV_DIST_CONTRACT_REVSHARE_PROD','C'); end;")

# completion_dt = datetime.datetime(2014,7,1,0,0,0)
# steps.DistributionSteps.create_places_and_completions(13665793, 2308, 392348, completion_dt)
# steps.CommonPartnerSteps.generate_partner_acts_fair(381102, datetime.datetime(2015,5,1,0,0,0))
# steps.CommonPartnerSteps.generate_partner_acts_fair(430334, datetime.datetime(2016,8,1,0,0,0))

# dt = datetime.datetime(2015, 1, 1, 0, 0, 0)
# client_id, person_id, tag_id = steps.DistributionSteps.create_distr_client_person_tag()
# contract_id, external_id = steps.ContractSteps.create('universal_distr',
#                                                               {'client_id': client_id, 'person_id': person_id, 'dt': dt,
#                                                                'DISTRIBUTION_TAG': tag_id})

# steps.DistributionSteps.create_distr_place(13764120, 2338, 3, update_mv=False)

# completion_dt = datetime.datetime(2016,5,30,0,0,0)
# completion_sum = 800000
# #директ
# steps.DistributionSteps.create_distr_completion(11158691, completion_dt, completion_sum=completion_sum, distr_type='Direct')
# #такси
# steps.DistributionSteps.create_distr_completion(11158690, completion_dt, completion_sum=completion_sum, distr_type='Taxi')
# #маркет
# steps.DistributionSteps.create_distr_completion(11158689, completion_dt, completion_sum=completion_sum)
# #installs
# steps.DistributionSteps.create_distr_completion(11158688, completion_dt, completion_sum=completion_sum)
# #downloads
# steps.DistributionSteps.create_distr_completion(11158687, completion_dt, completion_sum=completion_sum)
# #clicks
# steps.DistributionSteps.create_distr_completion(11158686, completion_dt, completion_sum=completion_sum)
# #activations
# steps.DistributionSteps.create_distr_completion(11158685, completion_dt, completion_sum=completion_sum)
# #searches
# steps.DistributionSteps.create_distr_completion(11158684, completion_dt, completion_sum=completion_sum)

# db.balance().execute("begin dbms_mview.refresh('BO.MV_DISTR_CONTRACT_PLACES','C'); end;")
# db.balance().execute("begin dbms_mview.refresh('BO.MV_DIST_CONTRACT_DOWNLOAD_PROD','C'); end;")
# db.balance().execute("begin dbms_mview.refresh('BO.MV_DIST_CONTRACT_REVSHARE_PROD','C'); end;")

# date_of_execution = datetime.datetime(2015,2,1,0,0,0)

# steps.CommonPartnerSteps.generate_partner_acts_fair(381102, date_of_execution)
# steps.CommonPartnerSteps.generate_partner_acts_fair(442435, date_of_execution)
# steps.CommonPartnerSteps.generate_partner_acts_fair(442436, date_of_execution)

# steps.CommonPartnerSteps.acts_enqueue(date_of_execution, [442436])

# completion_dt = datetime.datetime(2016,7,19,0,0,0)
# steps.DistributionSteps.create_places_and_completions(13918800, 2364, 403692, completion_dt)
# data = api.medium().GetDistributionRevenueShare(datetime.datetime(2016,7,19,0,0,0), datetime.datetime(2016,7,19,0,0,0))
# end_data = distribution_fixed_data_filter(data, place_id = 11158796)
# print end_data
#
# completion_dt = datetime.datetime(2016, 4, 1, 0, 0, 0)
# api.medium().GetPagesTagsStat(completion_dt, completion_dt)


# completion_dt = datetime.datetime(2016, 5, 1, 0, 0, 0)
# data = api.medium().GetDistributionFixed(completion_dt, completion_dt)
# splitted = data.split('\n')
# header = tuple(splitted[0].split('\t'))
# filtered = []
# for line in splitted[1:]:
#     row = tuple(line.split('\t'))
#     if row[1] == 11158803:
#         filtered.append(dict(zip(header,row)))
# print filtered

# client_id = 13962479
# api.medium().GetClientPersons(client_id)

# api.medium().CreatePerson(PASSPORT_ID,
#                           {
#                               'ID': '4337801',
#                               'MEMO': 'НДС 18',
#                               'CLIENT_ID': '13962479',
#                               'TYPE': 'sw_ytph',
#                               'IS_PARTNER': '1'
#                           })

# steps.PersonSteps.create(14008636, 'ur', params={'MEMO': 'вот еще 5'})
# client_id = steps.ClientSteps.create()
# steps.PersonSteps.create(client_id, 'ur')

# api.medium().GetDistributionActed(datetime.datetime(2016,6,1,0,0,0), 13524388)

# api.medium().QueryCatalog(['t_tax'])
# tm.Balance.QueryCatalog([‘t_price’], ‘t_price.id = 20618’)
# api.medium().QueryCatalog(['t_firm'], "t_firm.id = 1")

# steps.CommonPartnerSteps.generate_partner_acts_fair(307913, datetime.datetime(2016,12,1,0,0,0))
# steps.CommonPartnerSteps.generate_partner_acts_fair(226429, datetime.datetime(2016, 11, 1, 0, 0))
# client_id, person_id, tag_id = steps.DistributionSteps.create_distr_client_person_tag()
# api.medium().GetDistributionActed(datetime.datetime(2016, 10, 1, 0, 0), 24056168)
# api.medium().GetDistributionActed(datetime.datetime(2016, 1, 1, 0, 0))

# client_id = steps.ClientSteps.create()
# tag_id = db.balance().execute("SELECT s_test_distribution_tag_id.nextval AS tag_id FROM dual")[0]['tag_id']
# api.medium().CreateOrUpdateDistributionTag(PASSPORT_ID,
#                                                        {'TagID': tag_id, 'TagName': 'CreatedByScript',
#                                                         'ClientID': client_id})
# person_id = steps.PersonSteps.create(client_id, 'yt', {'is-partner': '1'})
# place_id = db.balance().execute("SELECT s_test_place_id.nextval place FROM dual")[0]['place']
# search_id = db.balance().execute("SELECT s_test_place_search_id.nextval search FROM dual")[0]['search']
# api.medium().CreateOrUpdatePlace(16571028, {'ClientID': client_id,
#  'ID': 156942,
#  'InternalType': 100,
#  'ProductList': [{'id': 10004}, {'id': 10003}, {'id': 909}],
#  'SearchID': search_id,
#  'TagID': tag_id,
#  'Type': 8,
#  'URL': 'pytest.com'})
# api.medium().CreateOrUpdatePlace(PASSPORT_ID,
#                                              {'ID': place_id, 'ClientID': client_id, 'Type': Distribution.PLACE_TYPE,
#                                               # 'PaymentTypeID': 3,
#                                               'ProductList': [{'id': 909},{'id': 910},{'id': 911},
#                                                               {'id': 920},{'id': 931},{'id': 932},
#                                                               {'id': 938},{'id': 942},{'id': 949},
#                                                               {'id': 952},{'id': 996},{'id': 1000},
#                                                               {'id': 1111},{'id': 1132},{'id': 1146},
#                                                               {'id': 1157},{'id': 1184},{'id': 2080},
#                                                               {'id': 3010},{'id': 4009},{'id': 4010},
#                                                               {'id': 4011},{'id': 4012},{'id': 10000},
#                                                               {'id': 10001},{'id': 10002},{'id': 10003},
#                                                               {'id': 10004},{'id': 100005}],
#                                               'URL': "pytest.com", 'SearchID': search_id,
#                                               'TagID': tag_id, 'InternalType': 100})

# person_id = steps.PersonSteps.create(25893370, 'ur', {'is-partner': '1'})
# steps.DistributionSteps.create_distr_completion(11158248, completion_dt, completion_sum=completion_sum)
# api.medium().CreateOrUpdatePlace(16571028, {'ClientID': 25975175,
#  'ID': 11170711,
#  'InternalType': 100,
#  'ProductList': [{'id': 10005}, {'id': 10006}, {'id': 909}, {'id': 10004}],
#  'SearchID': 2297765,
#  'TagID': 4924,
#  'Type': 8,
#  'URL': 'pytest.com'})

# api.medium().GetDistributionRevenueShareFull(datetime.datetime(2016,11,19,0,0,0), datetime.datetime(2016,1,19,0,0,0))

# data = api.medium().GetDistributionRevenueShare(datetime.datetime(2016,7,19,0,0,0), datetime.datetime(2016,7,19,0,0,0))
# end_data = distribution_fixed_data_filter(data, place_id = 11158796)
# print end_data

# tag_id = db.balance().execute("SELECT s_test_distribution_tag_id.nextval AS tag_id FROM dual")[0]['tag_id']
# api.medium().CreateOrUpdateDistributionTag(PASSPORT_ID,
#                                                        {'TagID': tag_id, 'TagName': 'CreatedByScript',
#                                                         'ClientID': 26106417})


# client_id = steps.ClientSteps.create()
# tag_id = db.balance().execute("SELECT s_test_distribution_tag_id.nextval AS tag_id FROM dual")[0]['tag_id']
# api.medium().CreateOrUpdateDistributionTag(PASSPORT_ID,
#                                                        {'TagID': tag_id, 'TagName': 'CreatedByScript',
#                                                         'ClientID': client_id})
# person_id = steps.PersonSteps.create(client_id, 'ur', {'is-partner': '1'})
# place_id = db.balance().execute("SELECT s_test_place_id.nextval place FROM dual")[0]['place']
# search_id = db.balance().execute("SELECT s_test_place_search_id.nextval search FROM dual")[0]['search']
# api.medium().CreateOrUpdatePlace(16571028, {'ClientID': client_id,
#  'ID': place_id,
#  'InternalType': 100,
#  'ProductList':  [{'id': 909},{'id': 910},{'id': 911},
#                                                               {'id': 920},{'id': 931},{'id': 932},
#                                                               {'id': 938},{'id': 942},{'id': 949},
#                                                               {'id': 952},{'id': 996},{'id': 1000},
#                                                               {'id': 1111},{'id': 1132},{'id': 1146},
#                                                               {'id': 1157},{'id': 1184},{'id': 2080},
#                                                               {'id': 3010},{'id': 4009},{'id': 4010},
#                                                               {'id': 4011},{'id': 4012},{'id': 10000},
#                                                               {'id': 10001},{'id': 10002},{'id': 10003},
#                                                               {'id': 10004},{'id': 100005}],
#  'SearchID': search_id,
#  'TagID': tag_id,
#  'Type': 8,
#  'URL': 'pytest.com'})

# api.medium().CreateUserClientAssociation(16571028, 13879008, 439275030)
# api.medium().CreateClient(16571028, {'SERVICE_ID': 120})

# api.medium().GetAddapterStat(datetime.datetime(2016, 11, 1, 0, 0), 'DISTRIBUTION')


#
# CONTRACT_TYPES = ['agile_distr_full', 'universal_distr_full']
# START_DT = utils.Date.first_day_of_month() - relativedelta(months=1)
#
# def create_full_contract_and_generate_act(contract_type):
#     # создаем клиента, плательщика и тэг
#     client_id, person_id, tag_id = steps.DistributionSteps.create_distr_client_person_tag()
#
#     # создаем договор дистрибуции
#     contract_id, external_id = steps.ContractSteps.create_contract(contract_type,
#                                                                    {'CLIENT_ID': client_id, 'PERSON_ID': person_id,
#                                                                     'DT': START_DT,
#                                                                     'IS_SIGNED': START_DT.isoformat(),
#                                                                     'FIRM': 111,
#                                                                     'DISTRIBUTION_TAG': tag_id,
#                                                                     'SERVICE_START_DT': START_DT,
#                                                                     'PRODUCTS_REVSHARE': [
#                                                                         ('1', str(
#                                                                             DistributionType.DIRECT.default_price)),
#                                                                         ('2', str(
#                                                                             DistributionType.TAXI.default_price)),
#                                                                         ('3', str(
#                                                                             DistributionType.MARKET_CPC.default_price)),
#                                                                         ('4', str(
#                                                                             DistributionType.MARKET_CPA.default_price))
#                                                                     ],
#                                                                     'INSTALL_PRICE': DistributionType.INSTALLS.default_price,
#                                                                     'PRODUCTS_DOWNLOAD': [('1', str(
#                                                                         DistributionType.DOWNLOADS.default_price))],
#                                                                     'ADVISOR_PRICE': DistributionType.CLICKS.default_price,
#                                                                     'SEARCH_PRICE': DistributionType.SEARCHES.default_price,
#                                                                     'ACTIVATION_PRICE': DistributionType.ACTIVATIONS.default_price
#                                                                     })
#
#     # создаем площадки
#     places_ids = steps.DistributionSteps.create_fixed_and_revshare_places(client_id, tag_id)
#
#     # добавляем открутки
#     steps.DistributionSteps.create_completions(places_ids, START_DT)
#
#     # запускаем генерацию актов
#     steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, START_DT)
#
#     return client_id, contract_id, places_ids, tag_id
#
# client_id, contract_id, places_ids, tag_id = create_full_contract_and_generate_act('agile_distr_full')

# expected_partner_act_data = steps.DistributionData.create_expected_full_partner_act_data(contract_id, client_id,
#                                                                                          tag_id, places_ids,
#                                                                                          START_DT)


# api.medium().QueryCatalog(['v_distribution_contract'], "v_distribution_contract.id = 464702")

# api.medium().QueryCatalog(['t_page_data'])



# client_id = steps.ClientSteps.create()
# person_id = steps.PersonSteps.create(client_id, 'ur', {'kpp': '123456789'})
# person_id = steps.PersonSteps.create(client_id, 'eu_yt')


# api.test_balance().GetNotification(10, 31902421)

# client_id = steps.ClientSteps.create()
# person_id = steps.PersonSteps.create(client_id, 'ur', {'kpp': '123456789'})
# steps.PersonSteps.create(client_id, 'sw_yt', {'is-partner': '1'})
# tag_id = steps.DistributionSteps.create_distr_tag(client_id)
# person_id = steps.PersonSteps.create(client_id, 'ur', {'kpp': '123456789'})
# tag_id = steps.DistributionSteps.create_distr_tag(client_id)
# contract_id, _ = steps.ContractSteps.create_contract('universal_distr_empty',
#                                                    {'CLIENT_ID': client_id,
#                                                     'PERSON_ID': person_id,
#                                                     'DISTRIBUTION_TAG': tag_id,
#                                                     'SUPPLEMENTS': [4],})
# contract_id, _ = steps.ContractSteps.create_contract('universal_distr_empty',
#                                                    {'CLIENT_ID': client_id,
#                                                     'PERSON_ID': person_id,
#                                                     'DISTRIBUTION_TAG': tag_id,
#                                                     'SUPPLEMENTS': [5],})
# contract_id, _ = steps.ContractSteps.create_contract('addapter_general',
#                                                    {'CLIENT_ID': client_id,
#                                                     'PERSON_ID': person_id,
#                                                     'SERVICES': [141]})
# contract_id, _ = steps.ContractSteps.create_contract('addapter_general',
#                                                    {'CLIENT_ID': client_id,
#                                                     'PERSON_ID': person_id,
#                                                     'SERVICES': [142]})
#
# places_ids, clids = steps.DistributionSteps.create_addapter_places(client_id, tag_id)
# steps.DistributionSteps.create_addapter_completions(clids, datetime.datetime(2016,12,1,0,0,0))

# stats_gen = steps.DistributionSteps.get_addapter_stat_by_contract_ids(datetime.datetime(2017,6,30,0,0,0),
#                                                                       ContractSubtype.GENERAL,
#                                                                               [498138])

# steps.CommonPartnerSteps.generate_partner_acts_fair(498139, datetime.datetime(2016,12,1,0,0,0))

client_id = steps.ClientSteps.create()
person_id = steps.PersonSteps.create(client_id, 'ur', {'is-partner': '1'})
# person_id = steps.PersonSteps.create(client_id, 'ua', {})
# person_id = steps.PersonSteps.create(client_id, 'eu_yt', {})
# tag_id = steps.DistributionSteps.create_distr_tag(client_id)
# contract_id, _ = steps.ContractSteps.create_contract('distr_univers_all_suppl',
#                                                    {'CLIENT_ID': client_id,
#                                                     'PERSON_ID': person_id,
#                                                     'DISTRIBUTION_TAG': tag_id})

# place_id = db.balance().execute("SELECT s_test_place_id.nextval place FROM dual")[0]['place']
# search_id = db.balance().execute("SELECT s_test_place_search_id.nextval search FROM dual")[0]['search']
# api.medium().CreateOrUpdatePlace(16571028, {'ClientID': 32132641,
#  'ID': place_id,
#  'InternalType': 100,
#  'ProductList':  [{'id': 10007}],
#     'SearchID': search_id,
#     'TagID': 25289,
#     'Type': 8,
#     'URL': 'pytest.com'})


# db.balance().execute("begin dbms_mview.refresh('BO.MV_DISTR_CONTRACT_PLACES','C'); end;")
# db.balance().execute("begin dbms_mview.refresh('BO.MV_DIST_CONTRACT_DOWNLOAD_PROD','C'); end;")
# db.balance().execute("begin dbms_mview.refresh('BO.MV_DIST_CONTRACT_REVSHARE_PROD','C'); end;")

# api.medium().GetDistributionRevenueShare(datetime.datetime(2017,1,21,0,0,0), datetime.datetime(2017,1,21,0,0,0))

# api.medium().QueryCatalog(['t_firm'])

# steps.CommonPartnerSteps.generate_partner_acts_fair(606116, datetime.datetime(2017,1,1,0,0,0))

# api.medium().GetClientContracts({'ClientID': 32513337,
#  'ContractType': 'GENERAL',})

# api.medium().GetClientContracts({'ClientID': 32513325,
#  'ContractType': 'GENERAL',
#  'Dt': datetime.datetime(2017, 2, 21, 0, 0),
#  'Signed': 1})

# api.medium().GetDspStat(datetime.datetime(2017, 3, 5, 0, 0), datetime.datetime(2017, 3, 6, 0, 0), None, None, 0)
# aggregate_period
# steps.CommonPartnerSteps.generate_partner_acts_fair(626624, datetime.datetime(2017,2,1,0,0,0))



# person_id = steps.PersonSteps.create(client_id, 'ur', {'is-partner': '1'})
# tag_id = steps.DistributionSteps.create_distr_tag(client_id)
# contract_id, _ = steps.ContractSteps.create_contract('distr_univers_all_suppl',
#                                                    {'CLIENT_ID': client_id,
#                                                     'PERSON_ID': person_id,
#                                                     'DISTRIBUTION_TAG': tag_id})
# contract_id = 718906
# steps.ContractSteps.create_collateral(3060, {'CONTRACT2_ID': contract_id,
#                                                  'DT': '2017-03-03T00:00:00',
#                                                  # 'TAIL_TIME': TAIL_TIME,
#                                                  'END_DT': '2017-03-14T00:00:00',
#                                                  'IS_SIGNED': '2017-03-14T00:00:00'})


# api.medium().InvalidatePersonBankProps(5244165)
