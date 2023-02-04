# -*- coding: utf-8 -*-
__author__ = 'sandyk'

import pytest
from datetime import datetime
from hamcrest import equal_to

import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance import balance_web as web
from balance.balance_steps.other_steps import UserSteps
from balance.features import Features
from btestlib import utils as utils
from btestlib.constants import Services, PersonTypes, Firms

SERVICE_ID = Services.DIRECT.id
FIRM = Firms.YANDEX_1
PERSON_TYPE = PersonTypes.UR.code

DT = datetime.now()
CONTRACT_DT = utils.Date.date_to_iso_format(DT)

pytestmark = [pytest.mark.tickets('BALANCE-27524'), reporter.feature(Features.UI, Features.EDO)]


@pytest.mark.smoke
def test_edo_page(get_free_user):
    user = get_free_user()
    UserSteps.set_role(user, role_id=0)
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PERSON_TYPE)
    steps.PersonSteps.accept_edo(person_id, FIRM.id, DT)
    contract_id, contract_external_id = steps.ContractSteps.create_contract_new('opt_agency_prem',
                                                                                {'CLIENT_ID': client_id,
                                                                                 'PERSON_ID': person_id,
                                                                                 'DT': CONTRACT_DT,
                                                                                 'IS_SIGNED': CONTRACT_DT,
                                                                                 'FIRM': FIRM.id, 'SERVICES': [
                                                                                    SERVICE_ID], 'PAYMENT_TYPE': 2,
                                                                                 'CURRENCY': 810})
    steps.ContractSteps.create_collateral(1048,
                                          {'CONTRACT2_ID': contract_id, 'DT': DT, 'FINISH_DT': CONTRACT_DT,
                                           'IS_SIGNED': CONTRACT_DT})
    with web.Driver(user=user) as driver:
        edo_page = web.AdminInterface.EdoPage.open(driver, client_id)
        edo_page.is_offer_table_present()
        edo_page.is_contract_table_present()
        ui_contract_eid = edo_page.get_contract_eid_from_table()
        ui_unit_name = edo_page.get_unit_name_from_table()
        utils.check_that(contract_external_id, equal_to(ui_contract_eid),
                         u'Проверяем, что в таблице укзан нужный договор')
        utils.check_that(FIRM.name, equal_to(ui_unit_name), u'Проверяем, что в таблице укзан нужный БЮ')
