# -*- coding: utf-8 -*-
__author__ = 'atkaya'

import balance.balance_db as db
from balance import balance_steps as steps


def create_ssp_with_acts():
    start_dt, end_dt = steps.CommonSteps.previous_month_first_and_last_days()
    completion_dt = start_dt
    #completion_dt = datetime.datetime(2015, 11, 1, 0, 0, 0)
    #start_dt = datetime.datetime(2015, 11, 1, 0, 0, 0)
    #end_dt = datetime.datetime(2015, 11, 30, 0, 0, 0)

    #partner and person
    client_id, person_id = steps.PartnerSteps.create_partner_client_person()
    contract_id, external_id = steps.ContractSteps.create_contract('rsya_ssp',
                                                                   {'CLIENT_ID': client_id, 'PERSON_ID': person_id})
    #place for partner
    place_id = steps.PartnerSteps.create_partner_place(client_id)

    #completions
    steps.PartnerSteps.create_direct_partner_completion(place_id, completion_dt)
    steps.PartnerSteps.create_dsp_partner_completions(place_id, completion_dt)

    #contract in queue
    steps.CommonPartnerSteps.acts_enqueue(start_dt, end_dt, [contract_id])

    #restart generate-partner-acts
    steps.CommonSteps.restart_pycron_task('generate-partner-acts')

    #waiting for state = 1 in t_export
    sql = "select state as val from t_export where type = 'PARTNER_ACTS' and object_id = :contract_id"
    sql_params = {'contract_id': contract_id}
    steps.CommonSteps.wait_for(sql, sql_params, value = 1)

#    expected = []
#    utils.check_that(expected, mtch.FullMatch(db.get_acts_by_contract(contract_id)))

    #проверяем вознаграждение по Директу
    query_get_partner_reward = "select round(partner_reward_wo_nds,5) reward from t_partner_act_data where partner_contract_id = :contract_id and description = 'Яндекс.Директ'"
    params_invoice = {'contract_id': contract_id}
    reward_direct = db.balance().execute(query_get_partner_reward, params_invoice)[0]['reward']
    print reward_direct

    expected_direct_reward = 1093.22034
    assert str(reward_direct) == str(expected_direct_reward), 'Direct reward is incorrect!'

    #проверяем вознаграждение по РТБ
    query_get_partner_reward = "select round(partner_reward_wo_nds,5) reward from t_partner_act_data where partner_contract_id = :contract_id and description = 'РТБ'"
    params_invoice = {'contract_id': contract_id}
    reward_rtb = db.balance().execute(query_get_partner_reward, params_invoice)[0]['reward']
    print reward_rtb

    expected_rtb_reward = 1000
    assert str(reward_rtb) == str(expected_rtb_reward), 'RTB reward is incorrect!'

#create_ssp_with_acts()
#if __name__ == "__main__":
#    test_create_ssp_with_acts()
#    # pytest.main('test_partner.py::test_create_ssp_with_acts')
#    # pytest.main('-k "test_create_ssp_with_acts"')

#start_dt = datetime.datetime(2016,1,20,0,0,0)
#end_dt = datetime.datetime(2016,1,20,0,0,0)
#include_deals = 1
#steps.CommonSteps.log(api.Medium().server.Balance.GetDspStat)(start_dt, end_dt, 0)


# template_header_without_deals = (
# 'PAGE_ID', 'BLOCK_ID', 'DSP_ID', 'DT', 'HITS', 'DSP', 'DSPWITHOUTNDS', 'PARTNER', 'PARTNERWITHOUTNDS', 'SHOWS',
# 'AGGREGATORWITHOUTNDS', 'AGGREGATOR', 'TOTAL_RESPONSE_COUNT', 'TOTAL_BID_SUM')
# template_header_with_deals = (
# 'PAGE_ID', 'BLOCK_ID', 'DSP_ID', 'DT', 'HITS', 'DSP', 'DSPWITHOUTNDS', 'PARTNER', 'PARTNERWITHOUTNDS', 'SHOWS',
# 'AGGREGATORWITHOUTNDS', 'AGGREGATOR', 'TOTAL_RESPONSE_COUNT', 'TOTAL_BID_SUM', 'DEAL_ID', 'YANDEX_PRICE', 'FAKE_PRICE')
# template_of_common_data = {'DSP_ID': defaults.partner()['DSP_ID'],
#                            'HITS': defaults.partner()['DSP_HITS'],
#                            'BLOCK_ID': defaults.partner()['DSP_Block_ID'],
#                            'TOTAL_BID_SUM': defaults.partner()['DSP_TOTAL_BID_SUM'],
#                            'TOTAL_RESPONSE_COUNT': defaults.partner()['DSP_TOTAL_RESPONSE_COUNT'],
#                            'SHOWS': defaults.partner()['DSP_SHOWS'],
#                            'DSP': round(defaults.partner()['DSP_Charge'] * 1.18, 4),
#                            'DSPWITHOUTNDS': defaults.partner()['DSP_Charge'],
#                            'PARTNER': round(defaults.partner()['PartnerRewardDSP'] * 1.18, 4),
#                            'PARTNERWITHOUTNDS': defaults.partner()['PartnerRewardDSP']}
#
# template_of_deals_data = {'DEAL_ID': defaults.partner()['DSP_DEAL_ID'],
#                           'YANDEX_PRICE': defaults.partner()['DSP_YANDEX_PRICE'],
#                           'FAKE_PRICE': defaults.partner()['DSP_FAKE_PRICE']}

# place_id = 11158840
# data = steps.CommonSteps.log(api.medium().server.Balance.GetDspStat)(datetime.datetime(2016, 2, 4, 0, 0),
#                                                                      datetime.datetime(2016, 2, 5, 0, 0), 0)
# header, filtered_data = steps.CommonSteps.get_header_and_data_by_id(data, 'PAGE_ID', place_id)

# dict_da_compare = {}
# for key in filtered_data[0]:
#     if key in template_of_common_data.keys():
#         dict_da_compare[key] = filtered_data[0][key]
#
# for key in dict_da_compare:
#     if key in ['PARTNER', 'DSP']:
#         dict_da_compare[key] = round(float(dict_da_compare[key]),4)
#
# assert header == template_header_with_deals, "Incorrect header!"
# utils.check_that([template_of_common_data], mtch.FullMatch([dict_da_compare]))




