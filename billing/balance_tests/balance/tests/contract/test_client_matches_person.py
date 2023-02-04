# coding: utf-8
__author__ = 'a-vasin'

import xmlrpclib
from datetime import datetime

import pytest
from dateutil.relativedelta import relativedelta
from hamcrest import contains_string

import balance.balance_steps as steps
import btestlib.utils as utils
from btestlib.constants import Services, Currencies, ContractPaymentType, Firms, Managers, PersonTypes
from btestlib import reporter
from balance.features import Features

START_DT = utils.Date.first_day_of_month(datetime.now() - relativedelta(months=1))
ERROR = "Invalid parameter for function: CLIENT_ID. Client must be linked to person"


@reporter.feature(Features.TO_UNIT)
def test_client_with_different_person():
    wrong_client_id = steps.ClientSteps.create()

    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code)
    project_uuid = steps.PartnerSteps.create_cloud_project_uuid()

    with pytest.raises(xmlrpclib.Fault) as exception:
        create_contract(wrong_client_id, person_id, [project_uuid])

    utils.check_that(exception.value.faultString, contains_string(ERROR),
                     u"Проверяем, что при несвязанных клиенте и плательщике пробрасывается исключение")


def create_contract(client_id, person_id, projects):
    return steps.ContractSteps.create_offer({
        'client_id': client_id,
        'currency': Currencies.RUB.char_code,
        'firm_id': Firms.YANDEX_1.id,
        'manager_uid': Managers.PERANIDZE.uid,
        'payment_type': ContractPaymentType.POSTPAY,
        'payment_term': 90,
        'person_id': person_id,
        'start_dt': START_DT,
        'projects': projects,
        'services': [Services.CLOUD_143.id]
    })
