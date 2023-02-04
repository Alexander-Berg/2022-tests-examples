# -*- coding: utf-8 -*-
__author__ = 'atkaya'

import datetime

import balance.balance_db as db
from balance import balance_steps as steps
from btestlib.data import defaults as default

PASSPORT_ID = default.PASSPORT_UID

PAYMENT_TYPE_DISTR_RETAIL = 9
PAYMENT_TYPE_DISTR_DEVELOPER = 11
PEYMENT_TYPE_GENERAL_RETAIL = 10
PAYMENT_TYPE_GENERAL_DEVELOPER = 12

SRC_ID_DEVELOPER = 21
SRC_ID_RETAIL = 22
PAGE_ID_DEVELOPER = 3021
PAGE_ID_RETAIL = 3022


# payment_type
# 9 - Distr Adapter Retail
# 11 - Distr Adapter Developer
# 10 - General Retail
# 12 - General Developer

def create_addapter_completion(clid_dev, clid_ret, money_dev, money_ret, installs, dt):
    query_1 = "INSERT INTO t_partner_addapter_stat (DT,CLID,LINKED_CLID,PAGE_ID,MONEY,INSTALLS,SOURCE_ID) " \
              "VALUES (:dt,:clid_dev,:clid_ret,:page_id_dev,:money_dev,:installs,:src_id_dev)"
    query_2 = "INSERT INTO t_partner_addapter_stat (DT,CLID,LINKED_CLID,PAGE_ID,MONEY,INSTALLS,SOURCE_ID) " \
              "VALUES (:dt,:clid_ret,:clid_dev,:page_id_ret,:money_ret,:installs,:src_id_ret)"
    params_1 = {'dt': dt, 'clid_dev': clid_dev, 'clid_ret': clid_ret, 'page_id_dev': PAGE_ID_DEVELOPER,
                'installs': installs,
                'money_dev': money_dev, 'src_id_dev': SRC_ID_DEVELOPER}
    params_2 = {'dt': dt, 'clid_dev': clid_dev, 'clid_ret': clid_ret, 'page_id_ret': PAGE_ID_RETAIL,
                'installs': installs,
                'money_ret': money_ret, 'src_id_ret': SRC_ID_RETAIL}
    db.balance().execute(query_1, params_1)
    db.balance().execute(query_2, params_2)


def create_client_with_all_types_of_places(person_type):
    client_id = steps.ClientSteps.create({'IS_AGENCY': 0, 'NAME': u'Test AG Distribution'})
    person_id_distr = steps.PersonSteps.create(client_id, person_type, {'is-partner': '1'})
    tag_id = steps.DistributionSteps.create_distr_tag(client_id, PASSPORT_ID)

    place_id_distr_retail, search_id_distr_retail = steps.DistributionSteps.create_distr_place(client_id, tag_id,
                                                                                               [PAGE_ID_RETAIL])
    place_id_distr_dev, search_id_distr_dev = steps.DistributionSteps.create_distr_place(client_id, tag_id,
                                                                                         [PAGE_ID_DEVELOPER])
    place_id_gen_retail, search_id_gen_retail = steps.DistributionSteps.create_distr_place(client_id, tag_id,
                                                                                           [PAGE_ID_RETAIL])
    place_id_gen_dev, search_id_gen_dev = steps.DistributionSteps.create_distr_place(client_id, tag_id,
                                                                                     [PAGE_ID_DEVELOPER])
    return client_id, person_id_distr, tag_id, search_id_distr_retail, search_id_distr_dev, search_id_gen_retail, search_id_gen_dev


def create_contracts(client_id, person_id_distr, tag_id, person_type):
    person_id_general = steps.PersonSteps.create(client_id, person_type, {'kpp': '234567891'})
    contract_id_general, _ = steps.ContractSteps.create_contract('addapter_general',
                                                                 {'CLIENT_ID': client_id,
                                                                  'PERSON_ID': person_id_general})
    contract_id_addapter_ret, _ = steps.ContractSteps.create_contract('distr_addapter_ret',
                                                                      {'CLIENT_ID': client_id,
                                                                       'PERSON_ID': person_id_distr,
                                                                       'DT': datetime.datetime(2016, 1, 1, 0, 0, 0),
                                                                       'DISTRIBUTION_TAG': tag_id,
                                                                       'SERVICE_START_DT': datetime.datetime(2016, 1, 1,
                                                                                                             0, 0, 0),
                                                                       })
    contract_id_addapter_dev, _ = steps.ContractSteps.create_contract('distr_addapter_dev',
                                                                      {'CLIENT_ID': client_id,
                                                                       'PERSON_ID': person_id_distr,
                                                                       'DT': datetime.datetime(2016, 1, 1, 0, 0, 0),
                                                                       'DISTRIBUTION_TAG': tag_id,
                                                                       'SERVICE_START_DT': datetime.datetime(2016, 1, 1,
                                                                                                             0, 0, 0),
                                                                       })
    db.balance().execute("BEGIN dbms_mview.refresh('BO.MV_DISTR_CONTRACT_PLACES','C'); END;")
    return person_id_general, contract_id_general, contract_id_addapter_ret, contract_id_addapter_dev


# api.medium().GetAddapterStat(datetime.datetime(2016, 10, 15), 'DISTRIBUTION')
# api.medium().GetAddapterStat(datetime.datetime(2016, 10, 15), 'GENERAL')



# client_id, person_id_distr, tag_id = steps.DistributionSteps.create_distr_client_person_tag()

# client_id, person_id_distr, tag_id, search_id_distr_retail, \
# search_id_distr_dev, search_id_gen_retail, search_id_gen_dev = create_client_with_all_types_of_places('ur')
#
# person_id_general, contract_id_general, \
# contract_id_addapter_ret, contract_id_addapter_dev = create_contracts(client_id, person_id_distr, tag_id, 'yt')
#
# create_addapter_completion(search_id_distr_dev, search_id_gen_retail, 10, 20, 5, datetime.datetime(2016,10,16,0,0,0))
# create_addapter_completion(search_id_gen_dev, search_id_distr_retail, 80, 90, 30, datetime.datetime(2016,10,16,0,0,0))



# addapter_general distr_addapter_dev distr_addapter_ret

# v_partner_addapter_distr
# v_partner_addapter_comm


# place_id_distr_retail, search_id_distr_retail = steps.DistributionSteps.create_distr_place(19187635, 7295,
#                                                                                            PAYMENT_TYPE_DISTR_RETAIL)
# place_id_distr_dev, search_id_distr_dev = steps.DistributionSteps.create_distr_place(19187635, 7295,
#                                                                                      PAYMENT_TYPE_DISTR_DEVELOPER)
# place_id_gen_retail, search_id_gen_retail = steps.DistributionSteps.create_distr_place(19187635, 7295,
#                                                                                        PEYMENT_TYPE_GENERAL_RETAIL)
# place_id_gen_dev, search_id_gen_dev = steps.DistributionSteps.create_distr_place(19187635, 7295,
#                                                                                  PAYMENT_TYPE_GENERAL_DEVELOPER)

# create_addapter_completion(search_id_distr_dev, search_id_gen_retail, 1000, 5000, 230, datetime.datetime(2016,10,12,0,0,0))
# create_addapter_completion(search_id_gen_dev, search_id_distr_retail, 10000, 50000, 122, datetime.datetime(2016,10,12,0,0,0))

# create_addapter_completion(12290586, 12291108, 500, 100, 1, datetime.datetime(2016,10,15,0,0,0))
# create_addapter_completion(12290588, 12291106, 300, 140, 3, datetime.datetime(2016,10,15,0,0,0))

# tag_id = steps.DistributionSteps.create_distr_tag(19187635, PASSPORT_ID)
# place_id_distr_dev, search_id_distr_dev = steps.DistributionSteps.create_distr_place(19187635, tag_id,
#                                                                                      PAYMENT_TYPE_DISTR_DEVELOPER)
# place_id_gen_retail, search_id_gen_retail = steps.DistributionSteps.create_distr_place(19187635, tag_id,
#                                                                                        PEYMENT_TYPE_GENERAL_RETAIL)


# api.medium().CreateOrUpdateOrdersBatch(428033395, [{'RegionID': 47, 'ServiceID': 37, 'Text': '222',
#                                                    'ClientID': 19344175, 'ServiceOrderID': 1, 'ProductID': 502918}])

# client_id = steps.ClientSteps.create({'IS_AGENCY': 0, 'NAME': u'Test AG Distribution'})
# person_id_general = steps.PersonSteps.create(client_id, 'ur', {'kpp': '234567891'})
# contract_id_general, _ = steps.ContractSteps.create_contract('addapter_general',
#                                                              {'CLIENT_ID': client_id,
#                                                               'PERSON_ID': person_id_general})




# client_id_1 = steps.ClientSteps.create({'IS_AGENCY': 0, 'NAME': u'Test AG Distribution'})
# person_id_distr_1 = steps.PersonSteps.create(client_id_1, 'yt', {'is-partner': '1'})
# person_id_general_1 = steps.PersonSteps.create(client_id_1, 'yt', {'kpp': '234567891'})
# tag_id_1 = steps.DistributionSteps.create_distr_tag(client_id_1, PASSPORT_ID)
#
# client_id_2 = steps.ClientSteps.create({'IS_AGENCY': 0, 'NAME': u'Test AG Distribution'})
# person_id_distr_2 = steps.PersonSteps.create(client_id_2, 'yt', {'is-partner': '1'})
# person_id_general_2 = steps.PersonSteps.create(client_id_2, 'yt', {'kpp': '234567891'})
# tag_id_2 = steps.DistributionSteps.create_distr_tag(client_id_2, PASSPORT_ID)
#
# _, search_id_distr_retail = steps.DistributionSteps.create_distr_place(client_id_1, tag_id_1,
#                                                                                            PAYMENT_TYPE_DISTR_RETAIL)
# _, search_id_distr_dev = steps.DistributionSteps.create_distr_place(client_id_2, tag_id_2,
#                                                                                      PAYMENT_TYPE_DISTR_DEVELOPER)
# _, search_id_gen_retail = steps.DistributionSteps.create_distr_place(client_id_1, tag_id_1,
#                                                                                        PEYMENT_TYPE_GENERAL_RETAIL)
# _, search_id_gen_dev = steps.DistributionSteps.create_distr_place(client_id_2, tag_id_2,
#                                                                                  PAYMENT_TYPE_GENERAL_DEVELOPER)
# contract_id_general, _ = steps.ContractSteps.create_contract('addapter_general',
#                                                              {'CLIENT_ID': client_id_1,
#                                                               'PERSON_ID': person_id_general_1,
#                                                               'SERVICES': [142]
#                                                               })
# contract_id_general, _ = steps.ContractSteps.create_contract('addapter_general',
#                                                              {'CLIENT_ID': client_id_2,
#                                                               'PERSON_ID': person_id_general_2,
#                                                               'SERVICES': [141]
#                                                               })
# contract_id_addapter_ret, _ = steps.ContractSteps.create_contract('distr_addapter_ret',
#                                                                   {'CLIENT_ID': client_id_1,
#                                                                    'PERSON_ID': person_id_distr_1,
#                                                                    'DT': datetime.datetime(2016, 1, 1, 0, 0, 0),
#                                                                    'DISTRIBUTION_TAG': tag_id_1,
#                                                                    'SERVICE_START_DT': datetime.datetime(2016, 1, 1,
#                                                                                                          0, 0, 0),
#                                                                    })
# contract_id_addapter_dev, _ = steps.ContractSteps.create_contract('distr_addapter_dev',
#                                                                   {'CLIENT_ID': client_id_2,
#                                                                    'PERSON_ID': person_id_distr_2,
#                                                                    'DT': datetime.datetime(2016, 1, 1, 0, 0, 0),
#                                                                    'DISTRIBUTION_TAG': tag_id_2,
#                                                                    'SERVICE_START_DT': datetime.datetime(2016, 1, 1,
#                                                                                                          0, 0, 0),
#                                                                    })
# db.balance().execute("begin dbms_mview.refresh('BO.MV_DISTR_CONTRACT_PLACES','C'); end;")
#
# create_addapter_completion(search_id_distr_dev, search_id_gen_retail, 1000, 5000, 230, datetime.datetime(2016,10,4,0,0,0))
# create_addapter_completion(search_id_gen_dev, search_id_distr_retail, 10000, 50000, 122, datetime.datetime(2016,10,4,0,0,0))



# client_id_1 = steps.ClientSteps.create({'IS_AGENCY': 0, 'NAME': u'Test AG Distribution'})
# person_id_general_1 = steps.PersonSteps.create(client_id_1, 'ur', {'kpp': '234567891'})
# tag_id_1 = steps.DistributionSteps.create_distr_tag(client_id_1, PASSPORT_ID)
#
# client_id_2 = steps.ClientSteps.create({'IS_AGENCY': 0, 'NAME': u'Test AG Distribution'})
# person_id_general_2 = steps.PersonSteps.create(client_id_2, 'ur', {'kpp': '234567891'})
# tag_id_2 = steps.DistributionSteps.create_distr_tag(client_id_2, PASSPORT_ID)
#
# client_id_3 = steps.ClientSteps.create({'IS_AGENCY': 0, 'NAME': u'Test AG Distribution'})
# person_id_distr_3 = steps.PersonSteps.create(client_id_3, 'ur', {'is-partner': '1'})
# tag_id_3 = steps.DistributionSteps.create_distr_tag(client_id_3, PASSPORT_ID)
#
# client_id_4 = steps.ClientSteps.create({'IS_AGENCY': 0, 'NAME': u'Test AG Distribution'})
# person_id_distr_4 = steps.PersonSteps.create(client_id_4, 'ur', {'is-partner': '1'})
# tag_id_4 = steps.DistributionSteps.create_distr_tag(client_id_4, PASSPORT_ID)
#
# _, search_id_distr_retail = steps.DistributionSteps.create_distr_place(client_id_3, tag_id_3,
#                                                                                            PAYMENT_TYPE_DISTR_RETAIL)
# _, search_id_distr_dev = steps.DistributionSteps.create_distr_place(client_id_4, tag_id_4,
#                                                                                      PAYMENT_TYPE_DISTR_DEVELOPER)
# _, search_id_gen_retail = steps.DistributionSteps.create_distr_place(client_id_1, tag_id_1,
#                                                                                        PEYMENT_TYPE_GENERAL_RETAIL)
# _, search_id_gen_dev = steps.DistributionSteps.create_distr_place(client_id_2, tag_id_2,
#                                                                                  PAYMENT_TYPE_GENERAL_DEVELOPER)
# contract_id_general, _ = steps.ContractSteps.create_contract('addapter_general',
#                                                              {'CLIENT_ID': client_id_1,
#                                                               'PERSON_ID': person_id_general_1,
#                                                               'SERVICES': [142]
#                                                               })
# contract_id_general, _ = steps.ContractSteps.create_contract('addapter_general',
#                                                              {'CLIENT_ID': client_id_2,
#                                                               'PERSON_ID': person_id_general_2,
#                                                               'SERVICES': [141]
#                                                               })
# contract_id_addapter_ret, _ = steps.ContractSteps.create_contract('distr_addapter_ret',
#                                                                   {'CLIENT_ID': client_id_3,
#                                                                    'PERSON_ID': person_id_distr_3,
#                                                                    'DT': datetime.datetime(2016, 1, 1, 0, 0, 0),
#                                                                    'DISTRIBUTION_TAG': tag_id_3,
#                                                                    'SERVICE_START_DT': datetime.datetime(2016, 1, 1,
#                                                                                                          0, 0, 0),
#                                                                    })
# contract_id_addapter_dev, _ = steps.ContractSteps.create_contract('distr_addapter_dev',
#                                                                   {'CLIENT_ID': client_id_4,
#                                                                    'PERSON_ID': person_id_distr_4,
#                                                                    'DT': datetime.datetime(2016, 1, 1, 0, 0, 0),
#                                                                    'DISTRIBUTION_TAG': tag_id_4,
#                                                                    'SERVICE_START_DT': datetime.datetime(2016, 1, 1,
#                                                                                                          0, 0, 0),
#                                                                    })
# db.balance().execute("begin dbms_mview.refresh('BO.MV_DISTR_CONTRACT_PLACES','C'); end;")
#
# create_addapter_completion(search_id_distr_dev, search_id_gen_retail, 1000, 3000, 230, datetime.datetime(2016,10,20,0,0,0))
# create_addapter_completion(search_id_gen_dev, search_id_distr_retail, 5000, 7000, 122, datetime.datetime(2016,10,20,0,0,0))

# steps.CommonPartnerSteps.generate_partner_acts_fair(520774, datetime.datetime(2016,9,1,0,0,0))

steps.CommonPartnerSteps.generate_partner_acts_fair(277447, datetime.datetime(2016, 9, 1, 0, 0))
# TestBalance.GeneratePartnerAct(277447, datetime.datetime(2016, 9, 1, 0, 0))

# api.medium().CreateYaMoneyInvoice("4002019265", 22325884,[{"ServiceOrderID": "26407455", "Qty": 50.0, "ServiceID": 7}],{"GeoID": 72489})
