# -*- coding: utf-8 -*-

import pytest
import balance.balance_api as api

from balance import balance_steps as steps
from btestlib import utils, reporter
from btestlib.constants import ExportNG
from btestlib.data.partner_contexts import TAXI_RU_CONTEXT, ZAXI_RU_CONTEXT, TAXI_RU_DELIVERY_CONTEXT, \
    ZAXI_DELIVERY_RU_CONTEXT, FOOD_CORP_CONTEXT, Services
from balance.features import Features


@pytest.fixture(autouse=True)
def mock_trust(mock_simple_api):
    pass

CONTRACT_START_DT, _ = utils.Date.current_month_first_and_last_days()


def create_contracts(service):
    if service.id == Services.ZAXI.id:
        client_id_taxopark = steps.SimpleApi.create_partner(TAXI_RU_CONTEXT.service)
        steps.SimpleApi.create_partner(ZAXI_RU_CONTEXT.service)

        _, person_id_taxopark, taxi_contract_id, _ = steps.ContractSteps. \
            create_partner_contract(TAXI_RU_CONTEXT, client_id=client_id_taxopark,
                                    additional_params={'start_dt': CONTRACT_START_DT})
        _, _, zaxi_contract_id, _ = steps.ContractSteps. \
            create_partner_contract(ZAXI_RU_CONTEXT, client_id=client_id_taxopark, person_id=person_id_taxopark,
                                    additional_params={'start_dt': CONTRACT_START_DT,
                                                       'link_contract_id': taxi_contract_id})
        return zaxi_contract_id
    elif service.id == Services.ZAXI_DELIVERY.id:
        client_id_taxopark = steps.SimpleApi.create_partner(TAXI_RU_DELIVERY_CONTEXT.service)
        steps.SimpleApi.create_partner(ZAXI_DELIVERY_RU_CONTEXT.service)

        _, person_id_taxopark, taxi_contract_id, _ = steps.ContractSteps. \
            create_partner_contract(TAXI_RU_DELIVERY_CONTEXT, client_id=client_id_taxopark,
                                    additional_params={'start_dt': CONTRACT_START_DT})
        _, _, zaxi_contract_id, _ = steps.ContractSteps. \
            create_partner_contract(ZAXI_DELIVERY_RU_CONTEXT, client_id=client_id_taxopark, person_id=person_id_taxopark,
                                    additional_params={'start_dt': CONTRACT_START_DT,
                                                       'link_contract_id': taxi_contract_id})
        return zaxi_contract_id
    elif service.id == Services.TAXI_CORP.id:
        _, _, start_dt_1, end_dt_1, start_dt_2, end_dt_2 = utils.Date.previous_three_months_start_end_dates()
        client_id, person_id, contract_id, contract_eid = steps.ContractSteps.create_partner_contract(
            FOOD_CORP_CONTEXT,
            additional_params={'start_dt': start_dt_1},
            is_offer=True,
            is_postpay=0
        )
        return contract_id


@reporter.feature(Features.TRUST)
@pytest.mark.parametrize('n', [1, 3])
@pytest.mark.parametrize('service, lb_topic', [(Services.ZAXI, 'partner-fast-balance-zaxi'),
                                               (Services.ZAXI_DELIVERY, 'partner-fast-balance-zaxi'),
                                               (Services.TAXI_CORP, 'partner-fast-balance-corp-taxi')])
@pytest.mark.skip(reason="fast balance queues are turned ON on testing")
def test_fast_balance_logbroker_enqueue(n, service, lb_topic):
    """
    В тестинге включены в пикроне обработчики быстрых балансов, чтобы сервис мог тестироваться самостоятельно.
    Чтобы этот тест заработал локально - комментируем skip декоратор, делаем:
    update (
      select *
      from bo.t_pycron_descr
      where name in ('partner-fast-balance-proc', 'process_ng_logbroker_partner_fast_balance')
    )
    set terminate=1
    ;
    Запускаем тест. После возвращаем terminate=0
    """
    contract_id = create_contracts(service)
    # TODO[kasimtj]: do some balance actions
    expected_data = steps.PartnerSteps.get_partner_balance(service, [contract_id], 'FAST_BALANCE')

    assert len(expected_data) > 0

    # Create export object for logbroker
    for _ in range(n):
        api.test_balance().ProcessFastBalance(contract_id)

    fast_balance_object = api.test_balance().GetPartnerFastBalance(contract_id, 'Contract', lb_topic)

    _, _, export_results = api.test_balance().ProcessNgExportQueue(ExportNG.Type.LOGBROKER_PARTNER_FAST_BALANCE,
                                                                   [fast_balance_object['id']])
    assert 'SUCCESS' in export_results
    assert export_results['SUCCESS'] == 1, 'Row is not exported to logbroker'

    fast_balance_object = api.test_balance().GetPartnerFastBalance(contract_id, 'Contract', lb_topic)
    assert fast_balance_object['version_id'] == n - 1
