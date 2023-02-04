# -*- coding: utf-8 -*-
import datetime

import pytest

import btestlib.utils as utils
from btestlib.constants import Services
from temp.aikawa.Balance.contracts import contracts_rules as contracts_defaults

to_iso = utils.Date.date_to_iso_format

CONTRACT_CONTEXT = contracts_defaults.COMMISS

fill_attrs = contracts_defaults.fill_attrs

NOW = datetime.datetime.now()
NOW_NULLIFIED = utils.Date.nullify_time_of_date(NOW)
NOW_NULLIFIED_ISO = utils.Date.date_to_iso_format(NOW_NULLIFIED)
TOMORROW = NOW + datetime.timedelta(days=1)
TOMORROW_NULLIFIED = utils.Date.nullify_time_of_date(TOMORROW)
TOMORROW_NULLIFIED_ISO = utils.Date.date_to_iso_format(TOMORROW_NULLIFIED)
HALF_YEAR_AFTER = NOW + datetime.timedelta(days=180)


@pytest.mark.parametrize('service, param_name, value, extra_params',
                         [(Services.RIT.id, 'FINISH_DT', to_iso(NOW_NULLIFIED), {})
                          ],
                         ids=lambda p, v, ep: '{}'.format(p))
def test_optional_params(service, param_name, value, extra_params):
    adds = {param_name: value, 'SERVICES': [service]}
    adds.update(extra_params)
    contracts_defaults.check_param(context=CONTRACT_CONTEXT.new(adds=adds), param_name=param_name, optional=True)
