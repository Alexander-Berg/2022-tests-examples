# -*- coding: utf-8 -*-
import datetime

import pytest

import btestlib.utils as utils
from balance import balance_steps as steps
from btestlib.constants import Firms, PersonTypes
from temp.aikawa.Balance.contracts import contracts_rules as contracts_defaults
from temp.aikawa.Balance.contracts.contracts_rules import ContractException

to_iso = utils.Date.date_to_iso_format

fill_attrs = contracts_defaults.fill_attrs

NOW = datetime.datetime.now()
NOW_NULLIFIED = utils.Date.nullify_time_of_date(NOW)
NOW_NULLIFIED_ISO = utils.Date.date_to_iso_format(NOW_NULLIFIED)
TOMORROW = NOW + datetime.timedelta(days=1)
TOMORROW_NULLIFIED = utils.Date.nullify_time_of_date(TOMORROW)
TOMORROW_NULLIFIED_ISO = utils.Date.date_to_iso_format(TOMORROW_NULLIFIED)
HALF_YEAR_AFTER = NOW + datetime.timedelta(days=180)


def diff(first, second):
    second = set(second)
    return [item for item in first if item not in second]


all_person_types = [PersonTypes.YTPH, PersonTypes.YT_KZU, PersonTypes.YT_KZP, PersonTypes.YT,
                    PersonTypes.UR, PersonTypes.PH, PersonTypes.BYP, PersonTypes.BYU, PersonTypes.USU,
                    PersonTypes.USP, PersonTypes.SW_UR, PersonTypes.SW_YT, PersonTypes.SW_YTPH,
                    PersonTypes.SW_PH, PersonTypes.BY_YTPH, PersonTypes.TRP, PersonTypes.TRU,
                    PersonTypes.KZU, PersonTypes.KZP, PersonTypes.AM_PH, PersonTypes.AM_UR,
                    PersonTypes.YTPH]

available_person_types_VERTICAL = [PersonTypes.YTPH, PersonTypes.YT_KZU, PersonTypes.YT_KZP, PersonTypes.YT,
                                   PersonTypes.UR, PersonTypes.PH]

available_person_types_REKLAMA = [PersonTypes.BYP, PersonTypes.BYU]

available_person_types_RU = [PersonTypes.YT_KZP, PersonTypes.YT_KZU, PersonTypes.YTPH, PersonTypes.PH,
                             PersonTypes.UR, PersonTypes.YT]

non_available_person_types_REKLAMA = diff(all_person_types, available_person_types_REKLAMA)

non_available_person_types_VERTICAL = diff(all_person_types, available_person_types_VERTICAL)

non_available_person_types_RU = diff(all_person_types, available_person_types_RU)

firm_to_contract_type = {Firms.VERTICAL_12.id: [contracts_defaults.AUTO_NO_AGENCY],
                         Firms.REKLAMA_BEL_27.id: [contracts_defaults.BEL_NO_AGENCY],
                         Firms.YANDEX_1.id: [contracts_defaults.COMMISS],
                         Firms.KINOPOISK_9.id: [contracts_defaults.COMMISS],
                         Firms.TAXI_13.id: [contracts_defaults.COMMISS],
                         Firms.OFD_18.id: [contracts_defaults.OFD_WO_COUNT],
                         Firms.ZEN_28.id: [contracts_defaults.COMMISS],
                         Firms.MARKET_111.id: [contracts_defaults.COMMISS]}


@pytest.mark.parametrize('param_name, value_list, extra_params',
                         [
                             # ('PERSON_ID', available_person_types_REKLAMA, {'FIRM': Firms.REKLAMA_BEL_27.id}),
                             # ('PERSON_ID', available_person_types_VERTICAL, {'FIRM': Firms.VERTICAL_12.id}),
                             # ('PERSON_ID', available_person_types_RU, {'FIRM': Firms.YANDEX_1.id}),
                             # ('PERSON_ID', available_person_types_RU, {'FIRM': Firms.KINOPOISK_9.id}),
                             # ('PERSON_ID', available_person_types_RU, {'FIRM': Firms.TAXI_13.id}),
                             # ('PERSON_ID', available_person_types_RU, {'FIRM': Firms.OFD_18.id}),
                             # ('PERSON_ID', available_person_types_RU, {'FIRM': Firms.ZEN_28.id}),
                             ('PERSON_ID', available_person_types_RU, {'FIRM': Firms.MARKET_111.id}),
                         ],
                         ids=lambda p, v, ep: '{}'.format(p))
def test_changeable_params(param_name, value_list, extra_params):
    for contract_context in firm_to_contract_type[extra_params['FIRM']]:
        for value in value_list:
            if param_name == 'PERSON_ID':
                client_id = steps.ClientSteps.create()
                person_id = steps.PersonSteps.create(client_id, value.code)
                adds = {'CLIENT_ID': client_id, 'PERSON_ID': person_id}
            else:
                adds = {param_name: value}
            adds.update(extra_params)
            contracts_defaults.check_param(context=contract_context.new(adds=adds), param_name=param_name,
                                           changeable=True)


@pytest.mark.parametrize('param_name, value_list, extra_params',
                         [
                             # ('PERSON_ID', non_available_person_types_REKLAMA, {'FIRM': Firms.REKLAMA_BEL_27.id}),
                             # ('PERSON_ID', non_available_person_types_VERTICAL, {'FIRM': Firms.VERTICAL_12.id}),
                             # ('PERSON_ID', non_available_person_types_RU, {'FIRM': Firms.YANDEX_1.id}),
                             # ('PERSON_ID', non_available_person_types_RU, {'FIRM': Firms.KINOPOISK_9.id}),
                             # ('PERSON_ID', non_available_person_types_RU, {'FIRM': Firms.TAXI_13.id}),
                             # ('PERSON_ID', non_available_person_types_RU, {'FIRM': Firms.OFD_18.id}),
                             # ('PERSON_ID', non_available_person_types_RU, {'FIRM': Firms.ZEN_28.id}),
                             # ('PERSON_ID', non_available_person_types_RU, {'FIRM': Firms.MARKET_111.id}),

                         ],
                         ids=lambda p, v, ep: '{}'.format(p))
def test_denied_value_params(param_name, value_list, extra_params):
    for contract_context in firm_to_contract_type[extra_params['FIRM']]:
        for value in value_list:
            if param_name == 'PERSON_ID':
                client_id = steps.ClientSteps.create()
                person_id = steps.PersonSteps.create(client_id, value.code)
                adds = {'CLIENT_ID': client_id, 'PERSON_ID': person_id}
            else:
                adds = {param_name: value}
            adds.update(extra_params)
            contracts_defaults.check_param(context=contract_context.new(adds=adds), param_name=param_name,
                                           with_exception=[ContractException.WRONG_PERSON_CATEGORY_FOR_FIRM,
                                                           ContractException.WRONG_PERSON_CATEGORY_FOR_CONTRACT_EXCEPTION])
