# -*- coding: utf-8 -*-

__author__ = 'atkaya'

import pytest

import balance.balance_api as api
import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features, AuditFeatures
from btestlib import utils
from btestlib.constants import Services, Managers
from btestlib.matchers import has_entries_casted, equal_to
from btestlib.data.partner_contexts import TAXI_RU_CONTEXT, TAXI_RU_CONTEXT_SPENDABLE, TAXI_BV_GEO_USD_CONTEXT

SERVICE_ID = Services.TAXI_DONATE.id

# тест на проверку external_id, country, currency, region
@pytest.mark.smoke
@reporter.feature(Features.TAXI, Features.SPENDABLE, Features.DONATE, Features.XMLRPC)
@pytest.mark.tickets('BALANCE-22816')
@pytest.mark.parametrize(
        'context, pay_to_tinkoff',
        [
            (TAXI_BV_GEO_USD_CONTEXT, 0),
            pytest.mark.smoke((TAXI_RU_CONTEXT, 0)),
            (TAXI_RU_CONTEXT, 1)
        ],
        ids=[
            'AcceptTaxiOffer for Yandex Taxi BV',
            'AcceptTaxiOffer for Yandex Taxi LLC',
            'AcceptTaxiOffer for Yandex Taxi LLC with pay_to = tinkoff',
        ]
)
def test_create_taxi_donate_contract(context, pay_to_tinkoff):
    expected_data = {
        'country': context.region.id,
        'currency': context.currency.iso_num_code,
        'services': dict.fromkeys([str(SERVICE_ID)], 1),
        'is_offer': 1,
        'pay_to': 5 if pay_to_tinkoff else 1
    }

    client_id, person_id, contract_id, external_contract_id = steps.ContractSteps.create_partner_contract(context,
                                                                                                          # additional_params={'manager_uid': Managers.MANAGER_WO_PASSPORT_ID.uid})
                                                                                                          additional_params={'manager_code': Managers.MANAGER_WO_PASSPORT_ID.code})

    # создаем плательщика-партнера для расхоного договора
    person_params = {'is-partner': '1'}
    if pay_to_tinkoff:
        person_params.update({'bik': '044525974', 'account': '40802810800000041990'})

    partner_person_id = steps.PersonSteps.create(client_id, context.person_type.code, person_params)

    # создаем расходный договор для прочих выплат Такси
    _, external_id = steps.ContractSteps.accept_taxi_offer(partner_person_id, contract_id)

    # вычисляем предполагаемый external_id расходного
    expected_prefix = steps.CommonPartnerSteps.get_prefix_for_spendable_contract(SERVICE_ID, context.firm.id, 1)
    expected_external_id = expected_prefix + '-' + str(external_contract_id)
    expected_data.update({'external_id': expected_external_id})

    # забираем данные по договору
    contract_data = api.medium().GetPartnerContracts({'ExternalID': external_id})[0]['Contract']

    print utils.Presenter.pretty(contract_data)

    # сравниваем данные с ожидаемымыми
    utils.check_that(contract_data, has_entries_casted(expected_data), 'Сравниваем данные')


@reporter.feature(Features.TO_UNIT)
# тест на повторный вызов метода с теми же параметрами
@reporter.feature(Features.TAXI, Features.SPENDABLE, Features.DONATE, Features.XMLRPC)
@pytest.mark.tickets('BALANCE-22816')
def test_second_call_of_taxi_donate_creation_method():
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(TAXI_RU_CONTEXT)

    # создаем плательщика-партнера для расхоного договора
    partner_person_id = steps.PersonSteps.create(client_id, 'ur', {'is-partner': '1'})

    # создаем расходный договор для прочих выплат Такси
    partner_contract_id, _ = steps.ContractSteps.accept_taxi_offer(partner_person_id, contract_id)

    expected_exception = 'Invalid parameter for function: Client ' + str(client_id) + \
                         ' already has contract ' + str(partner_contract_id) + \
                         ' with link_contract_id=' + str(contract_id)

    with pytest.raises(Exception) as exc:
        steps.ContractSteps.accept_taxi_offer(partner_person_id, contract_id, nds=TAXI_RU_CONTEXT_SPENDABLE.nds.nds_id)
    utils.check_that(steps.CommonSteps.get_exception_code(exc.value, 'msg'),
                     equal_to(expected_exception))


@reporter.feature(Features.TO_UNIT)
# тест на проверку невозможности создать расходный договор, когда доходный не подписан
# АХТУНГ! Тест не удалять и не переносить в юнит-тесты, на него смотрит аудит
@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C04_8_Taxi))
def test_impossible_create_spendable_when_general_not_signed():

    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(TAXI_RU_CONTEXT, unsigned=True)
    partner_person_id = steps.PersonSteps.create(client_id, TAXI_RU_CONTEXT_SPENDABLE.person_type.code, {'is-partner': '1'})

    expected_error = u"Rule violation: 'Нельзя подписывать договор, связанный с неподписанным. " \
                     u"ID связанного неподписанного договора = {}'".format(contract_id)

    with pytest.raises(Exception) as exc:
        steps.ContractSteps.accept_taxi_offer(partner_person_id, contract_id, nds=TAXI_RU_CONTEXT_SPENDABLE.nds.nds_id)
    utils.check_that(steps.CommonSteps.get_exception_code(exc.value, 'msg'),
                     equal_to(expected_error))