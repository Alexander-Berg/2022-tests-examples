# coding: utf-8

"""
В рамках работы над BALANCE-32122 делаем так чтобы в ручке CreateCommonContract параметр `currency` стал необязательным
для договоров на эквайринг. Балансовый rpcutil сейчас не умеет делать параметр обязательным в зависимости от других
параметров, поэтому пришлось просто убрать флаг mandatory=True напротив параметра currency.

В то же время нужно удостовериться, что для других договоров кроме ACQUIRING парамтр currency всё ещё ведёт себя как
обязательный.
"""

import datetime
import xmlrpclib

import pytest
from dateutil.relativedelta import relativedelta
from hamcrest import contains_string

from balance import balance_steps as steps
from balance.distribution.distribution_types import DistributionType
from btestlib import utils
from btestlib.constants import ContractSubtype, Currencies, DistributionContractType, Firms, NdsNew as Nds, PersonTypes, Services
from btestlib.data.partner_contexts import CORP_TAXI_RU_CONTEXT_GENERAL, FOOD_CONTEXTS
from btestlib.data.person_defaults import InnType
from btestlib import reporter
from balance.features import Features


@reporter.feature(Features.TO_UNIT)
@pytest.mark.tickets("BALANCE-32122")
def test_general():
    """
    Параметр currency является обязательным для GENERAL договоров
    Модифицированный тест balance/tests/partner_schema_acts/test_corp_taxi_general_acts.py::test_act_corp_taxi_first_month_wo_data
    """
    context = CORP_TAXI_RU_CONTEXT_GENERAL.new(contract_services=[Services.TAXI_CORP_CLIENTS.id])
    _, _, start_dt, _, _, _ = utils.Date.previous_three_months_start_end_dates()
    with pytest.raises(xmlrpclib.Fault) as excinfo:
        steps.ContractSteps.create_partner_contract(context,
                                                    additional_params={"start_dt": start_dt},

                                                    omit_currency=True,  # <- не посылаем currency в параметрах

                                                    )
    errmsg = str(excinfo.value)
    utils.check_that(errmsg, contains_string("currency"))
    utils.check_that(errmsg, contains_string("mandatory"))
    # utils.check_that(errmsg, contains_string("GENERAL"))


@reporter.feature(Features.TO_UNIT)
@pytest.mark.tickets("BALANCE-32122")
@pytest.mark.parametrize('context',
                         [fc.courier_spendable for fc in FOOD_CONTEXTS if fc.name == 'RU'],
                         ids=lambda context: context.name)
def test_spendable(context):
    """
    Параметр currency является обязательным для SPENDABLE договоров
    Модифицированный тест balance/tests/spendable/test_spendable_act_wo_data_common.py::test_spendable_acts_wo_data
    """
    start_dt, _ = utils.Date.previous_month_first_and_last_days()
    with pytest.raises(xmlrpclib.Fault) as excinfo:
        steps.ContractSteps.create_partner_contract(context,
                                                    additional_params={"start_dt": start_dt},

                                                    omit_currency=True,  # <- не посылаем currency в параметрах

                                                    )
    errmsg = str(excinfo.value)
    utils.check_that(errmsg, contains_string("currency"))
    utils.check_that(errmsg, contains_string("mandatory"))
    # utils.check_that(errmsg, contains_string("SPENDABLE"))


@reporter.feature(Features.TO_UNIT)
@pytest.mark.tickets("BALANCE-32122")
def test_distribution():
    """
    Параметр currency является обязательным для DISTRIBUTION договоров
    Модифицированный тест balance/tests/distribution/test_currency.py::test_currency
    """
    client_id, person_id, tag_id = steps.DistributionSteps.create_distr_client_person_tag(person_type='yt')
    start_dt = utils.Date.first_day_of_month() - relativedelta(months=1)
    with pytest.raises(xmlrpclib.Fault) as excinfo:
        steps.DistributionSteps.create_full_contract(DistributionContractType.AGILE, client_id, person_id,
                                                     tag_id,
                                                     start_dt, start_dt,
                                                     nds=Nds.NOT_RESIDENT,
                                                     firm=Firms.YANDEX_1,
                                                     contract_currency=Currencies.UAH,
                                                     product_currency=Currencies.RUB,
                                                     exclude_revshare_type=DistributionType.DIRECT,

                                                     omit_currency=True,  # <- не посылаем currency в параметрах

                                                     )
    errmsg = str(excinfo.value)
    utils.check_that(errmsg, contains_string("currency"))
    utils.check_that(errmsg, contains_string("mandatory"))
    # utils.check_that(errmsg, contains_string("DISTRIBUTION"))


@reporter.feature(Features.TO_UNIT)
@pytest.mark.tickets("BALANCE-32122")
def test_partners():
    """
    Параметр currency является обязательным для PARTNERS договоров
    """
    client_id = steps.ClientSteps.create(prevent_oebs_export=True)
    person_id = steps.PersonSteps.create_partner(client_id, PersonTypes.UR.code, inn_type=InnType.RANDOM)
    start_dt = utils.Date.nullify_time_of_date(datetime.datetime.now() - datetime.timedelta(days=60))
    end_dt = utils.Date.nullify_time_of_date(datetime.datetime.now() + datetime.timedelta(days=60))
    with pytest.raises(xmlrpclib.Fault) as excinfo:
        steps.ContractSteps.create_common_contract({
            "ctype": ContractSubtype.PARTNERS.name,
            "client_id": client_id,
            "person_id": person_id,
            "firm_id": Firms.YANDEX_1.id,
            "start_dt": start_dt,
            "end_dt": end_dt,
            "signed": 1,

            # "currency": "GOLD COIN OF KING JAEHAERYS I TARGARYEN",  # <- не посылаем currency в параметрах

        })
    errmsg = str(excinfo.value)
    utils.check_that(errmsg, contains_string("currency"))
    utils.check_that(errmsg, contains_string("mandatory"))
    # utils.check_that(errmsg, contains_string("PARTNERS"))
