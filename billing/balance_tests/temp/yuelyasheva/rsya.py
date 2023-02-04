# -*- coding: utf-8 -*-

__author__ = 'atkaya'

import datetime
from decimal import Decimal

import balance.balance_db as db
from balance import balance_api as api
from balance import balance_steps as steps
from btestlib.constants import Firms, Managers
from btestlib.data import person_defaults
from btestlib.constants import NdsNew
import pytest

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
                                               'signed': 1,
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



def create_know_stat(place_id, dt):
    query = "Insert into t_partner_tags_stat3 (DT,PLACE_ID,TAG_ID,PAGE_ID,SHOWS,CLICKS,BUCKS,COMPLETION_TYPE,VID,TYPE,SOURCE_ID,ORDERS,BUCKS_RS,CLICKSA) values (:dt,'164',:place_id,'542','12','4','906814','1',null,'1','11',null,null,null)"
    params = {'dt': dt, 'place_id': place_id}
    db.balance().execute(query, params)

def create_tags_stat(place_id, dt):
    query = "Insert into t_partner_tags_stat3 (DT,PLACE_ID,TAG_ID,PAGE_ID,SHOWS,CLICKS,BUCKS,COMPLETION_TYPE,VID,TYPE,SOURCE_ID,ORDERS,BUCKS_RS,CLICKSA) values (:dt,:place_id,'111','542','12','4','1000','1',null,'1','11',null,null,null)"
    params = {'dt': dt, 'place_id': place_id}
    db.balance().execute(query, params)


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

# partner_id, person_id, contract_id = create_commoncontract_ur()
# # partner_id, contract_id = create_offer_with_ph()
# # create_contract_distr()
#
# # update_contract(850668)
#
#
# # steps.PersonSteps.create(82070237, 'ph', {'is-partner': '1'})
# #
# create_place_and_completions(datetime.datetime(2019,2,24,1,0,0), partner_id)
# update_mvs()
# # steps.CommonSteps.export('OEBS', 'Contract', 575139)
# # steps.CommonSteps.export('OEBS', 'ContractCollateral', 737199)
# # get_dsp_stat(datetime.datetime(2018,3,1), datetime.datetime(2018,3,1))
# # steps.PartnerSteps.get_dsp_stat_by_page_id(77196071, datetime.datetime(2019,2,1))
# close_month(contract_id, datetime.datetime(2019,2,28))



# client_id = steps.ClientSteps.create()
# steps.PersonSteps.create(client_id, 'ur', {'is-partner': '1'})
# steps.PersonSteps.create(client_id, 'ph', {'is-partner': '1'}, inn_type=1)

# completion_dt_1 = datetime.datetime(2018,4,23,0,0,0,0)
# completion_dt_2 = datetime.datetime(2018,4,24,0,0,0,0)
# completion_dt_3 = datetime.datetime(2018,4,25,0,0,0,0)
# completion_dt_4 = datetime.datetime(2018,4,26,0,0,0,0)
#
# completion_dt_1 = datetime.datetime(2017,12,31,0,0,0,0)
# completion_dt_2 = datetime.datetime(2018,1,1,0,0,0,0)
# completion_dt_3 = datetime.datetime(2018,2,28,0,0,0,0)
# completion_dt_4 = datetime.datetime(2018,3,1,0,0,0,0)

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
# сlose_month(contract_id, dt)

# dt_get = datetime.datetime(2018,4,25)
# get_pages_stat(dt_get, dt_get)
# get_pages_tags_stat(dt_get, dt_get)


def taxi_distr_completion(start_dt, end_dt):  # taxi_distr
    api.test_balance().GetPartnerCompletions({'start_dt': start_dt, 'end_dt': end_dt, 'completion_source': 'taxi_distr'})


def create_offer(person_type, partner_type, reward_type, test_mode=0):
    partner_id = steps.ClientSteps.create()
    person_id = None
    if person_type:
        person_id = steps.PersonSteps.create(partner_id, person_type, {'is-partner': '1'})
    steps.ClientSteps.set_partner_type(partner_id, partner_type)
    contract_id, contract_eid = steps.ContractSteps.create_offer({
                                                        'client_id': partner_id,
                                                        'currency': 'RUB',
                                                        'firm_id': Firms.YANDEX_1.id,
                                                        'person_id': person_id if person_type else None,
                                                        'manager_uid': Managers.VECHER.uid,
                                                        'ctype': 'PARTNERS',
                                                        'test_mode': test_mode if (person_type == 'ph' or person_type is None) else None,
                                                        'partner_pct': '50' if partner_type != 2 else 43,
                                                        'agregator_pct': '33',
                                                        'start_dt': datetime.datetime(2019, 3, 24, 7, 8, 0),
                                                        'service_start_dt': datetime.datetime(2019, 3, 24, 7, 8, 0),
                                                        'nds': 0,
                                                        'reward_type': reward_type,
                                                        'is_signed': None if test_mode else datetime.datetime(2019, 3, 24, 7, 8, 0)
                                                    })
    return partner_id, person_id, contract_id


def add_person_and_sign(partner_id, contract_id):
    person_id = steps.PersonSteps.create(partner_id, 'ph', {'is-partner': '1'})
    api.medium().UpdateContract(16571028, contract_id, {'person_id': person_id, 'is_signed': datetime.datetime(2019, 3, 31, 7, 8, 0)})


# создание площадки и откруток
def create_place_and_completions(place_id, completion_dt, page_id):

    steps.PartnerSteps.create_direct_partner_completion(place_id, completion_dt, page_id=page_id, bucks=100)
    create_tags_stat(place_id, completion_dt)

    query = "UPDATE t_place SET mkb_category_id = 49 WHERE id = :place_id_internal"
    params = {'place_id_internal': place_id}
    db.balance().execute(query, params)

    return place_id


def create_contracts():
    partner_id, person_id, contract_id = create_offer(None, 1, 1, 1)
    add_person_and_sign(partner_id, contract_id)
    place_id = steps.PartnerSteps.create_partner_place(partner_id)
    page_ids = [2060, 2070, 854, 2040, 100001, 956]
    for page_id in page_ids:
        create_place_and_completions(place_id, datetime.datetime(2019, 3, 31, 7, 8, 0), page_id)
    steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, datetime.datetime(2019, 3, 1))


@pytest.mark.parametrize('person_type', [
    'sw_yt',
    'sw_ytph',
])
@pytest.mark.parametrize('currency', [
    'RUR',
    'EUR',
    'USD'
])
def test_rsya(person_type, currency):
    partner_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(partner_id, person_type, {'is-partner': '1'})
    contract_id, contract_eid = steps.ContractSteps.create_offer(
                                          {'client_id': partner_id,
                                           'currency': currency,
                                           'firm_id': Firms.EUROPE_AG_7.id,
                                           'person_id': person_id,
                                           'manager_uid': Managers.NIGAI.uid,
                                           # 'distribution_contract-type': 6,
                                           'ctype': 'PARTNERS',
                                           # 'signed': 1,
                                           'start_dt': datetime.datetime(2019,1,1),
                                           'pay_to': 2,
                                           'test_mode': 0,
                                           'signed': 1
                                           })
    place_id = steps.PartnerSteps.create_partner_place(partner_id)
    page_ids = [2070]
    update_mvs()
    for page_id in page_ids:
        create_place_and_completions(place_id, datetime.datetime(2019, 5,1, 7, 8, 0), page_id)
    steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, datetime.datetime(2019, 5, 31))
    steps.ExportSteps.export_oebs(client_id=partner_id, contract_id=contract_id)