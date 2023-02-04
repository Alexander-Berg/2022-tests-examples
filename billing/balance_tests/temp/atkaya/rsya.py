# -*- coding: utf-8 -*-

__author__ = 'atkaya'

import datetime
from decimal import Decimal

import balance.balance_db as db
from balance import balance_api as api
from balance import balance_steps as steps
from btestlib.constants import Firms, Managers
from btestlib.data import person_defaults


# создание договора через CreateOffer с плательщиком физиком
# добавить в CreateOffer недостающих полей перед запуском
def create_offer_with_ph():
    partner_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(partner_id, 'ur', {'is-partner': '1'}, inn_type=person_defaults.InnType.RANDOM)
    # query = "UPDATE t_client SET partner_type = 2 WHERE id = :client_id"
    # query = "UPDATE t_person SET bank_type = 5 WHERE client_id = :client_id"
    # params = {'client_id': partner_id}
    # db.balance().execute(query, params)
    contract_id = api.medium().CreateCommonContract(16571028,
                                           {
                                               'client_id': partner_id,
                                               'currency': 'RUB',
                                               'firm_id': Firms.YANDEX_1.id,
                                               'person_id': person_id,
                                               'manager_uid': Managers.NIGAI.uid,
                                               'ctype': 'PARTNERS',
                                               'partner_pct': 43,
                                               'market_api_pct': 26,  # Decimal('40.01'),
                                               'test_mode': 1,
                                               # 'signed': 1,
                                               # 'is_faxed': 1,
                                               # 'is_faxed_dt': '2018-03-01T4:23:15',

                                               # 'test_mode': 1,
                                               # 'search_forms': 1,
                                               # 'open_date': 1,
                                               # 'unilateral_acts': 0
                                               # 'agregator_pct': '34.22',
                                               # 'dsp_agregation_pct': '33',
                                               'payment_type': 2,
                                               'start_dt': datetime.datetime(2018, 2, 1, 0, 0, 0),
                                               'service_start_dt': datetime.datetime(2018, 1, 1, 0, 0, 0),
                                               # 'end_dt': datetime.datetime(2018,4,25,0,0,0)
                                               # 'nds': 18
                                               # 'reward_type': 2
                                               # 'external_id': 'RRDDT8888802'
                                               # 'pay_to': 2
                                           })['ID']
    print "https://admin-balance.greed-tm.paysys.yandex.ru/contract-edit.xml?contract_id=" + str(
        contract_id)
    return partner_id, contract_id


def create_contract_distr():
    partner_id, person_id, tag_id = steps.DistributionSteps.create_distr_client_person_tag(person_type='ur')
    contract_id = api.medium().CreateCommonContract(16571028,
                                                    {
                                                        'client_id': partner_id,
                                                        'currency': 'RUB',
                                                        'firm_id': Firms.YANDEX_1.id,
                                                        'person_id': person_id,
                                                        'manager_uid': Managers.VECHER.uid,
                                                        'ctype': 'DISTRIBUTION',
                                                        'distribution_tag': tag_id,
                                                        'distribution_contract_type': 3,
                                                        # 'supplements': [1],
                                                        # 'signed': 1,
                                                        # 'is_faxed': 1,
                                                        # 'is_faxed_dt': '2018-03-01T4:23:15',

                                                        # 'test_mode': 1,
                                                        # 'search_forms': 1,
                                                        # 'open_date': 1,
                                                        # 'unilateral_acts': 0
                                                        # 'agregator_pct': '34.22',
                                                        # 'dsp_agregation_pct': '33',
                                                        # 'payment_type': 2
                                                        # 'start_dt': datetime.datetime(2018,4,24,0,0,0),
                                                        'service_start_dt': datetime.datetime(2018, 4, 24, 0, 0, 0),
                                                        # 'end_dt': datetime.datetime(2018,4,25,0,0,0)
                                                        'nds': 18,
                                                        # 'reward_type': 2
                                                        # 'external_id': 'RRDDT8888802'
                                                        # 'pay_to': 2
                                                    })['ID']
    print "https://admin-BALANCE.greed-tm.paysys.yandex.ru/contract-edit.xml?contract_id=" + str(contract_id)
    return partner_id, contract_id

# create_contract_distr()

# создание договора через CreateCommonContract с плательщиком юриком
# добавить в CreateCommonContract недостающих полей перед запуском
def create_commoncontract_ur():
    partner_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(partner_id, 'ur', {'is-partner': '1'})
    api.medium().CreateCommonContract(16571028,
                                      {'client_id': partner_id,
                                       'currency': 'RUR',
                                       'firm_id': Firms.YANDEX_1.id,
                                       'person_id': person_id,
                                       'manager_uid': Managers.NIGAI.uid,
                                       'distribution_contract-type': 6,
                                       'ctype': 'PARTNERS',
                                       'signed': 1,
                                       'start_dt': datetime.datetime(2019,1,1)
                                       })

def create_know_stat(place_id, dt):
    query = "Insert into t_partner_tags_stat3 (DT,PLACE_ID,TAG_ID,PAGE_ID,SHOWS,CLICKS,BUCKS,COMPLETION_TYPE,VID,TYPE,SOURCE_ID,ORDERS,BUCKS_RS,CLICKSA) values (:dt,'164',:place_id,'542','12','4','906814','1',null,'1','11',null,null,null)"
    params = {'dt': dt, 'place_id': place_id}
    db.balance().execute(query, params)

def create_tags_stat(place_id, dt):
    query = "Insert into t_partner_tags_stat3 (DT,PLACE_ID,TAG_ID,PAGE_ID,SHOWS,CLICKS,BUCKS,COMPLETION_TYPE,VID,TYPE,SOURCE_ID,ORDERS,BUCKS_RS,CLICKSA) values (:dt,:place_id,'111','542','12','4','1000','1',null,'1','11',null,null,null)"
    params = {'dt': dt, 'place_id': place_id}
    db.balance().execute(query, params)

# создание площадки и откруток
def create_place_and_completions(completion_dt, client_id):
    place_id = steps.PartnerSteps.create_partner_place(client_id)
    # place_id_internal = steps.PartnerSteps.create_partner_place(client_id, internal_type=2)

    steps.PartnerSteps.create_direct_partner_completion(place_id, completion_dt, bucks=100)
    steps.PartnerSteps.create_direct_partner_completion(place_id, completion_dt, page_id=2020, bucks=300)
    steps.PartnerSteps.create_direct_partner_completion(place_id, completion_dt, page_id=2070, bucks=500)
    # steps.PartnerSteps.create_direct_partner_completion(place_id, completion_dt, page_id = 854, bucks=150)

    create_know_stat(place_id, completion_dt)
    create_tags_stat(place_id, completion_dt)

    # steps.PartnerSteps.create_direct_partner_completion(place_id_internal, completion_dt, bucks=500)
    query = "UPDATE t_place SET mkb_category_id = 49 WHERE id = :place_id_internal"
    params = {'place_id_internal': place_id}
    db.balance().execute(query, params)

    steps.PartnerSteps.create_dsp_partner_completions(completion_dt, place_id=place_id)
    # steps.PartnerSteps.create_dsp_partner_completions(completion_dt, place_id=place_id_internal, partner_reward=Decimal('150'))
    return place_id

# обновление матвьюх
def update_mvs():
    db.balance().execute("BEGIN dbms_mview.refresh('BO.MV_PARTNER_CONTRACT_PUTTEE','C'); END;")
    db.balance().execute("BEGIN dbms_mview.refresh('BO.mv_partner_place_owners','C'); END;")

# закрытие месяца
def close_month(contract_id, dt):
    steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, dt)

# вызов метода GetDspStat
def get_dsp_stat(start_dt, end_dt):
    api.medium().GetDspStat(start_dt, end_dt)

# вызов метода GetPagesStat
def get_pages_stat(start_dt, end_dt):
    api.medium().GetPagesStat(start_dt, end_dt)

# вызов метода GetPagesTagsStat
def get_pages_tags_stat(start_dt, end_dt):
    api.medium().GetPagesTagsStat(start_dt, end_dt)


def update_contract(contract_id):
    api.medium().UpdateContract(16571028, contract_id, {
        # 'person_id': 6841362,# 6841305,
        # 'currency': 'USD',
        'partner_pct': 42,
    })

# create_offer_with_ph()
# partner_id, contract_id = create_offer_with_ph()
# create_contract_distr()

# update_contract(850668)


# steps.PersonSteps.create(82070237, 'ph', {'is-partner': '1'})
#
# create_place_and_completions(datetime.datetime(2018,4,24,1,0,0), partner_id)
# update_mvs()
# steps.CommonSteps.export('OEBS', 'Contract', 575139)
# steps.CommonSteps.export('OEBS', 'ContractCollateral', 737199)
# get_dsp_stat(datetime.datetime(2018,3,1), datetime.datetime(2018,3,1))
# steps.PartnerSteps.get_dsp_stat_by_page_id(77196071, datetime.datetime(2018,3,1))

# client_id = steps.ClientSteps.create()
# steps.PersonSteps.create(client_id, 'ur', {'is-partner': '1'})
# steps.PersonSteps.create(client_id, 'ph', {'is-partner': '1'}, inn_type=1)

# completion_dt_1 = datetime.datetime(2018,4,23,0,0,0,0)
# completion_dt_2 = datetime.datetime(2018,4,24,0,0,0,0)
# completion_dt_3 = datetime.datetime(2018,4,25,0,0,0,0)
# completion_dt_4 = datetime.datetime(2018,4,26,0,0,0,0)
#
completion_dt_1 = datetime.datetime(2017,12,31,0,0,0,0)
completion_dt_2 = datetime.datetime(2018,1,1,0,0,0,0)
completion_dt_3 = datetime.datetime(2018,2,28,0,0,0,0)
completion_dt_4 = datetime.datetime(2018,3,1,0,0,0,0)

# partner_id, contract_id = create_offer_with_ph()

# partner_id = steps.ClientSteps.create()
# person_id = steps.PersonSteps.create(partner_id, 'ph', {'is-partner': '1'}, inn_type=1)
# contract_id,_ = steps.ContractSteps.create_contract('rsya_universal', {'CLIENT_ID': partner_id, 'PERSON_ID': person_id})

# place_id = create_place_and_completions(completion_dt_1, partner_id)
# place_id = create_place_and_completions(completion_dt_2, partner_id)
# place_id = create_place_and_completions(completion_dt_3, partner_id)
# place_id = create_place_and_completions(completion_dt_4, partner_id)
# update_mvs()


# api.medium().GetPartnerContracts({'ClientID': 81491508})

# contract_id = 874279
# dt = datetime.datetime(2018,2,1,0,0,0)
# close_month(contract_id, dt)

# dt_get = datetime.datetime(2018,4,25)
# get_pages_stat(dt_get, dt_get)
# get_pages_tags_stat(dt_get, dt_get)


def taxi_distr_completion(start_dt, end_dt):  # taxi_distr
    api.test_balance().GetPartnerCompletions({'start_dt': start_dt, 'end_dt': end_dt, 'completion_source': 'taxi_distr'})


def create_offer_distr():
    partner_id, person_id, tag_id = steps.DistributionSteps.create_distr_client_person_tag(person_type='ur')
    # partner_id = 81778369#81778352
    # person_id = 6698425#6698404
    # tag_id = 22899#22870
    contract_id = api.medium().CreateCommonContract(16571028,
                                                    {
                                                        'client_id': partner_id,
                                                        'currency': 'RUB',
                                                        'firm_id': Firms.TAXI_13.id,
                                                        'person_id': person_id,
                                                        'manager_uid': Managers.VECHER.uid,
                                                        'ctype': 'DISTRIBUTION',
                                                        'distribution_tag': tag_id,
                                                        # 'parent_contract_id': 647325,
                                                        'distribution_contract_type': 6,
                                                        # 'external_id': '746646TEst-ДС',
                                                        # 'currency_calculation': 0,
                                                        # 'manager_bo_code': 33434,
                                                        # 'end_dt': datetime.datetime(2019,1,1,6,23,18),
                                                        # 'platform_type': 1,
                                                        'supplements': [1],
                                                        # 'signed': 1,
                                                        'products_revshare': {'13002': Decimal('45')},
                                                        # 'payment_type': 1,
                                                        # 'is_faxed'https://admin-balance.greed-tm.paysys.yandex.ru/contract-edit.xml?contract_id=588379: 1,
                                                        # 'is_faxed_dt': '2018-03-01T4:23:15',

                                                        # 'test_mode': 1,
                                                        # 'search_forms': 1,
                                                        # 'open_date': 1,
                                                        # 'unilateral_acts': 0
                                                        # 'agregator_pct': '34.22',
                                                        # 'dsp_agregation_pct': '33',
                                                        # 'payment_type': 2
                                                        'start_dt': datetime.datetime(2018, 4, 24, 7, 8, 0),
                                                        'service_start_dt': datetime.datetime(2018, 4, 24, 3, 2, 3),
                                                        # 'end_dt': datetime.datetime(2018,4,25,0,0,0)
                                                        # 'nds': 0,
                                                        # 'reward_type': 2
                                                        # 'external_id': 'RRDDT8888802'
                                                        # 'pay_to': 2
                                                    })['ID']
    print "https://admin-BALANCE.greed-tm.paysys.yandex.ru/contract-edit.xml?contract_id=" + str(contract_id)
    return partner_id, contract_id

# create_offer_distr()

# dt = datetime.datetime(2018,5,24)
# dt2 = datetime.datetime(2018,5,25)
# taxi_distr_completion(dt, dt2)

# steps.DistributionSteps.create_distr_place_with_update(81838390, 24841, {13002})
# steps.DistributionSteps.create_distr_completion_revshare(DistributionType.TAXI_LUCKY_RIDE, 77237207, datetime.datetime(2018,5,1))

# steps.DistributionSteps.get_distribution_revenue_share_full(datetime.datetime(2018,5,30))
# steps.DistributionSteps.get_distribution_revenue_share(datetime.datetime(2018,4,1), place_id=270424)
# steps.CommonPartnerSteps.generate_partner_acts_fair(665442, datetime.datetime(2018,5,1))

# partner_id = steps.ClientSteps.create()
# person_id = steps.PersonSteps.create(partner_id, 'ph', {'is-partner': '1'})
# api.medium().CreateOffer(16571028,
#                                       {'client_id': partner_id,
#                                        'currency': 'RUR',
#                                        'firm_id': Firms.EUROPE_AG_7.id,
#                                        'person_id': person_id,
#                                        'manager_uid': Managers.NIGAI.uid,
#                                        'distribution_contract-type': 6,
#                                        'ctype': 'PARTNERS',
#                                        # 'signed': 1,
#                                        'start_dt': datetime.datetime(2019,1,1),
#                                        'pay_to': 2,
#                                        'test_mode': 1
#                                        })