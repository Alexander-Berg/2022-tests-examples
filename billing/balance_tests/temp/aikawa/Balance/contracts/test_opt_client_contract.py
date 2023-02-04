# -*- coding: utf-8 -*-
import datetime

import pytest

import btestlib.utils as utils
from btestlib.constants import Firms, \
    ContractPaymentType
from temp.aikawa.Balance.contracts import contracts_rules as contracts_defaults
from temp.aikawa.Balance.contracts.contracts_rules import ContractException

to_iso = utils.Date.date_to_iso_format

CONTRACT_CONTEXT = contracts_defaults.OPT_CLIENT

fill_attrs = contracts_defaults.fill_attrs

NOW = datetime.datetime.now()
NOW_NULLIFIED = utils.Date.nullify_time_of_date(NOW)
NOW_NULLIFIED_ISO = utils.Date.date_to_iso_format(NOW_NULLIFIED)
TOMORROW = NOW + datetime.timedelta(days=1)
TOMORROW_NULLIFIED = utils.Date.nullify_time_of_date(TOMORROW)
TOMORROW_NULLIFIED_ISO = utils.Date.date_to_iso_format(TOMORROW_NULLIFIED)
HALF_YEAR_AFTER = NOW + datetime.timedelta(days=180)


@pytest.mark.parametrize('param_name, exception_type, extra_params',
                         [
                             ('SERVICES', ContractException.SERVICE_NEEDED_EXCEPTION, {}),
                             ('PAYMENT_TYPE', ContractException.PAYMENT_TYPE_NEEDED_EXCEPTION, {}),
                             ('MANAGER_CODE', ContractException.MANAGER_NEEDED_EXCEPTION, {}),
                             ('PERSON_ID', ContractException.PERSON_NEEDED_EXCEPTION, {})],
                         ids=lambda a: '{}'.format(a))
def test_strictly_needed_params(param_name, exception_type, extra_params):
    contracts_defaults.check_param(context=CONTRACT_CONTEXT, param_name=param_name,
                                   strictly_needed=exception_type)


@pytest.mark.parametrize('adds', [{'IS_FAXED': to_iso(NOW_NULLIFIED)}],
                         ids=lambda a: '{}'.format(a))
def test_optional_params(adds):
    contracts_defaults.check_param(context=CONTRACT_CONTEXT.new(adds=adds), param_name=adds.keys()[0], optional=True)


@pytest.mark.parametrize('payment_type', [ContractPaymentType.POSTPAY,
                                          ContractPaymentType.PREPAY],
                         ids=lambda a: '{}'.format(CONTRACT_CONTEXT.name))
def test_finish_dt_is_optional_when_post_pay(payment_type):
    CONTRACT_CONTEXT.payment_type = payment_type
    if payment_type == ContractPaymentType.POSTPAY:
        adds = {'FINISH_DT': to_iso(NOW_NULLIFIED)}
        contracts_defaults.check_param(context=CONTRACT_CONTEXT.new(adds=adds), param_name='FINISH_DT',
                                       optional=True)
    else:
        contracts_defaults.check_param(context=CONTRACT_CONTEXT.new(), param_name='FINISH_DT',
                                       strictly_needed=True)


@pytest.mark.parametrize('suspended_dt', [TOMORROW_NULLIFIED,
                                          NOW_NULLIFIED],
                         ids=lambda a: '{}'.format(a))
def test_is_suspended_value_cannot_be_future_date(suspended_dt):
    adds = {'IS_SUSPENDED': to_iso(suspended_dt)}
    if suspended_dt == TOMORROW_NULLIFIED:
        contracts_defaults.check_param(context=CONTRACT_CONTEXT.new(adds=adds), param_name='IS_SUSPENDED',
                                       with_exception=contracts_defaults.ContractException.IS_SUSPENDED_EXCEPTION)
    else:
        contracts_defaults.check_param(context=CONTRACT_CONTEXT.new(adds=adds), param_name='IS_SUSPENDED',
                                       optional=True)


@pytest.mark.parametrize('default_firm', [Firms.YANDEX_1],
                         ids=lambda a: '{}'.format(CONTRACT_CONTEXT.name))
def test_firm_is_default(default_firm):
    contracts_defaults.check_param(context=CONTRACT_CONTEXT.new(), param_name='FIRM',
                                   with_default=default_firm.id)


@pytest.mark.parametrize('default_firm',
                         [Firms.YANDEX_1, Firms.KINOPOISK_9, Firms.VERTICAL_12, Firms.TAXI_13, Firms.OFD_18,
                          Firms.ZEN_28, Firms.MARKET_111, Firms.CLOUD_112, Firms.AUTOBUS_113, Firms.HEALTH_114],
                         ids=lambda a: '{}{}'.format(CONTRACT_CONTEXT.name, a.id))
def test_available_firms(default_firm):
    # тест с негативными проверок есть в комиссионном договоре
    adds = {'FIRM': default_firm.id}
    contracts_defaults.check_param(context=CONTRACT_CONTEXT.new(adds=adds), param_name='FIRM',
                                   changeble=True)
