# -*- coding: utf-8 -*-

__author__ = 'yuelyasheva'

import datetime
from decimal import Decimal as D

import pytest

import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance import balance_api as api
from balance.features import Features
from btestlib import utils
from btestlib.constants import TransactionType
from btestlib.data import defaults
from btestlib.data.defaults import SpendableContractDefaults as SpendableDefParams
from btestlib.data.simpleapi_defaults import ThirdPartyData
from btestlib.matchers import contains_dicts_equal_to
from btestlib.constants import Users, Nds, Currencies, Managers, Services, Firms, Regions, PersonTypes, Paysyses
from dateutil.relativedelta import relativedelta
from temp.igogor.balance_objects import Contexts

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


def create_commoncontract_ur():
    partner_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(partner_id, 'ur', {'is-partner': '1'})
    api.medium().CreateCommonContract(16571028,
                                      {'client_id': partner_id,
                                       'currency': 'RUR',
                                       'firm_id': Firms.YANDEX_1.id,
                                       'person_id': person_id,
                                       'manager_uid': Managers.NIGAI.uid,
                                       'contract-type': 6,
                                       'ctype': 'PARTNERS'
                                       })

create_commoncontract_ur()