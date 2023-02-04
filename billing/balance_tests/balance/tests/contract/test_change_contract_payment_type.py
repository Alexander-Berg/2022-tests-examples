# coding: utf-8
__author__ = 'pelmeshka'

from decimal import Decimal
from xmlrpclib import Fault

import pytest
from hamcrest import contains_string

from balance import balance_steps as steps
from balance.features import Features
from btestlib import constants as const
from btestlib import reporter
from btestlib import utils
from btestlib.data.defaults import Date

PERSON_TYPE = const.PersonTypes.UR
PARTNER_SERVICE_ID = const.Services.DOSTAVKA.id
NO_PARTNER_SERVICE_ID = const.Services.MARKET.id
CONTRACT_TYPE = 'no_agency_post'


# Запрещаем заводить дс перехода на предоплату или постоплату для партнерских сервисов
@reporter.feature(Features.CONTRACT, Features.COLLATERAL)
@pytest.mark.tickets('BALANCE-27850')
def test_cannot_change_contract_payment_type_for_partner_service():
    with reporter.step(u'Подготавливаем постоплатный договор на партнерский сервис'):
        contract_id = prepare_contract(PARTNER_SERVICE_ID)

    with pytest.raises(Fault) as error:
        create_collateral_change_payment_type(contract_id)

    expected_error = u'Запрещено заводить ДС перевода на предоплату или постоплату для партнёрских сервисов'
    utils.check_that(error.value.faultString, contains_string(expected_error),
                     u'Проверяем текст ошибки при создании допника на смену типа оплаты')


# Для непартнёрских сервисов создание ДС на изменение типа оплаты должно проходить без ошибок
def test_can_change_contract_payment_type_for_non_partner_service():
    with reporter.step(u'Подготавливаем постоплатный договор на непартнерский сервис'):
        contract_id = prepare_contract(NO_PARTNER_SERVICE_ID)
    with reporter.step(u'Создаем допсоглашение для перевода с постоплаты на предоплату для непартнерского сервиса'):
        create_collateral_change_payment_type(contract_id)


def prepare_contract(service_id):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE.code)
    contract_id, _ = steps.ContractSteps.create_contract(CONTRACT_TYPE,
                                                         {
                                                             'CLIENT_ID': client_id,
                                                             'PERSON_ID': person_id,
                                                             'SERVICES': [service_id],
                                                             'MINIMAL_PAYMENT_COMMISSION': Decimal('0'),
                                                         })
    return contract_id


def create_collateral_change_payment_type(contract_id):
    with reporter.step(u'Создаем допсоглашение Перевод на предоплату'):
        steps.ContractSteps.create_collateral(const.Collateral.DO_PREPAY,
                                              {
                                                  'CONTRACT2_ID': contract_id,
                                                  'DT': Date.TODAY_ISO,
                                                  'IS_SIGNED': Date.TODAY_ISO,
                                              })
