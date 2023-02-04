# -*- coding: utf-8 -*-

import pytest
import datetime
import json
from decimal import Decimal as D
from jsonrpc import dispatcher
import copy

from . import steps
import balance.balance_db as db
from .. import common_defaults
from balance import balance_steps
from btestlib import utils
from temp.igogor.balance_objects import Contexts
from btestlib.data.partner_contexts import DMP_SPENDABLE_CONTEXT, RSYA_OFFER_RU
from btestlib.constants import Firms, ContractCommissionType, PersonTypes, Currencies, Services, Collateral, \
    DistributionContractType, Managers
from balance.distribution.distribution_types import DistributionType
from .. import common_defaults

to_iso = utils.Date.date_to_iso_format
dt_delta = utils.Date.dt_delta

NOW = datetime.datetime.now()
NOW_ISO = to_iso(NOW)
END_DT = datetime.datetime(year=2025, month=1, day=1)
START_DT = datetime.datetime(year=2020, month=1, day=1)
THREE_MONTHS_AFTER_START = START_DT + datetime.timedelta(days=90)
ORDER_DT = NOW
INVOICE_DT = NOW
COMPLETIONS_DT = NOW
ACT_DT = NOW

CONTEXT = steps.CONTEXT


@dispatcher.add_method
def create_base_contract(contract_params, is_agency=0, context=CONTEXT):
    client_id = steps.ClientSteps.create({'NAME': common_defaults.CLIENT_NAME, 'IS_AGENCY': is_agency})
    person_id = steps.PersonSteps.create(client_id, context.person_type.code, params=context.person_params)
    contract_params.update({'client_id': client_id, 'person_id': person_id})
    contract_id, contract_eid = steps.ContractSteps.create_common_contract(contract_params)
    return client_id, person_id, contract_id, contract_eid


# договор со всеми параметрами, отображающимися в поиске, и с необходимым минимумом для создания договора
@dispatcher.add_method
@pytest.mark.parametrize('set_all_params', [True, False],
                         ids=['Contract with all search result params', 'Contract with min search result params'])
def test_direct_1_contract(set_all_params):
    context = CONTEXT
    contract_params = {'DT': to_iso(START_DT),
                       'PAYMENT_TYPE': 3,
                       'PAYMENT_TERM': 100,
                       'CREDIT_TYPE': 1,
                       'CREDIT_LIMIT_SINGLE': 123,
                       'SERVICES': context.contract_services,
                       'CURRENCY': str(Currencies.RUB.num_code),
                       'FIRM': context.firm.id,
                       }
    if set_all_params:
        contract_params.update({'FINISH_DT': to_iso(END_DT),
                                'IS_SIGNED': to_iso(START_DT + utils.relativedelta(days=5)),
                                'IS_FAXED': to_iso(START_DT + utils.relativedelta(days=2)),
                                'IS_BOOKED': 1,
                                'IS_BOOKED_DT': to_iso(START_DT + utils.relativedelta(days=1)),
                                'IS_SUSPENDED': to_iso(START_DT + utils.relativedelta(months=2)),
                                'SENT_DT':  to_iso(START_DT + utils.relativedelta(days=20))})
    client_id, person_id, contract_id, contract_eid = steps.create_base_contract(contract_params, context=context)
    if not set_all_params:
        query = "update T_CONTRACT_COLLATERAL set is_cancelled=:cancel_dt where contract2_id=:contract_id"
        db.balance().execute(query, {'cancel_dt': steps.NOW, 'contract_id': contract_id})
        balance_steps.ContractSteps.refresh_contracts_cache(contract_id)
    return client_id, person_id, contract_id, contract_eid


@dispatcher.add_method
def test_general_contract_ind_limits():
    context = steps.CONTEXT.new(contract_type=ContractCommissionType.OPT_AGENCY)

    contract_params = {'DT': to_iso(START_DT),
                       'FINISH_DT': to_iso(START_DT+utils.relativedelta(years=5)),
                       'IS_SIGNED': to_iso(START_DT),
                       'PAYMENT_TYPE': 3,
                       'PAYMENT_TERM': 100,
                       'CREDIT_TYPE': 2,
                       'CREDIT_LIMIT_SINGLE': D('5700'),
                       'PERSONAL_ACCOUNT': 0,
                       'LIFT_CREDIT_ON_PAYMENT': 0,
                       'PERSONAL_ACCOUNT_FICTIVE': 0,
                       'CURRENCY': str(Currencies.RUB.num_code),
                       'FIRM': context.firm.id,
                       'SERVICES': [Services.MEDIA_BANNERS.id, Services.GEO.id, Services.MEDIA_70.id,
                                    Services.MEDIA_BANNERS_167.id, Services.DIRECT.id, Services.BAYAN.id,
                                    Services.BANKI.id, Services.SPAMDEF.id, Services.TECHNOLOGIES.id]
                       }

    client_id, person_id, contract_id, contract_eid = steps.create_base_contract(contract_params, context=context,
                                                                                 is_agency=1)

    # создаем клиента и допник на индивидуальный кредитный лимит
    client_id = balance_steps.ClientSteps.create(params={'NAME': common_defaults.CLIENT_NAME})
    credit_limits = [{"id": "1",
                      "num": "{0}".format(client_id),
                      "client": "{0}".format(client_id),
                      "client_limit": "1000",
                      "client_payment_term": "45",
                      "client_credit_type": "1000",
                      "client_limit_currency": "RUR"}]

    collateral_params = {'CONTRACT2_ID': contract_id,
                         'CLIENT_LIMITS': str(json.dumps(credit_limits)),
                         'DT': utils.Date.date_to_iso_format(START_DT+utils.relativedelta(days=2)),
                         'IS_SIGNED': utils.Date.date_to_iso_format(START_DT+utils.relativedelta(days=2))}
    balance_steps.ContractSteps.create_collateral(Collateral.SUBCLIENT_CREDIT_LIMIT, collateral_params)

    return client_id, contract_id, contract_eid


@dispatcher.add_method
def test_distribution_group_and_child_offer():
    client_id = balance_steps.ClientSteps.create(params={'NAME': ''})
    person_params = copy.deepcopy(common_defaults.FIXED_UR_PARAMS)
    person_params.update({'is-partner': 1})
    person_id = balance_steps.PersonSteps.create(client_id, PersonTypes.UR.code, person_params)
    _, _, first_tag_id = \
        balance_steps.DistributionSteps.create_distr_client_person_tag(client_id=client_id, person_id=person_id)
    second_tag_id = balance_steps.DistributionSteps.create_distr_tag(client_id)
    group_type = DistributionContractType.GROUP
    first_type = DistributionContractType.AGILE
    second_type = DistributionContractType.UNIVERSAL
    firm = Firms.YANDEX_1

    group_contract_id, _ = \
        balance_steps.DistributionSteps.create_full_contract(group_type, client_id, person_id, None,
                                                             START_DT, THREE_MONTHS_AFTER_START,
                                                             firm=firm, create_contract=balance_steps.
                                                             ContractSteps.create_common_contract)

    first_child_contract_id, external_contract_id = \
        balance_steps.DistributionSteps.create_full_contract(first_type, client_id, person_id, first_tag_id,
                                                             START_DT, THREE_MONTHS_AFTER_START, firm=firm,
                                                             parent_contract_id=group_contract_id,
                                                             exclude_revshare_type=DistributionType.VIDEO_HOSTING,
                                                             create_contract=balance_steps.
                                                             ContractSteps.create_common_contract)

    second_chile_contract_id, external_contract_id = \
        balance_steps.DistributionSteps.create_full_contract(second_type, client_id, person_id, second_tag_id,
                                                             START_DT, THREE_MONTHS_AFTER_START, firm=firm,
                                                             parent_contract_id=group_contract_id,
                                                             exclude_revshare_type=DistributionType.DIRECT,
                                                             create_contract=balance_steps.
                                                             ContractSteps.create_common_contract)

    return client_id, group_contract_id, first_child_contract_id, second_chile_contract_id


@dispatcher.add_method
def test_spendable_dmp_contract():
    default_dmp_products = [
        {u'enabled': u'X', u'id': 508333, u'num': 508333,
         u'name': u"Использование+данных+'Aidata.me'.+Тариф+02"},
        {u'enabled': u'X', u'id': 508334, u'num': 508334,
         u'name': u"Использование+данных+'Aidata.me'.+Тариф+03"}]

    additional_params = {'start_dt': START_DT, 'dmp_products': json.dumps(default_dmp_products),
                         'currency_rate_dt': START_DT}
    client_id = balance_steps.ClientSteps.create(params={'NAME': ''})
    person_params = copy.deepcopy(common_defaults.FIXED_UR_PARAMS)
    person_params.update({'is-partner': 1})
    person_id = balance_steps.PersonSteps.create(client_id, DMP_SPENDABLE_CONTEXT.person_type.code, person_params)
    _, _, contract_id, _ = \
        balance_steps.ContractSteps.create_partner_contract(DMP_SPENDABLE_CONTEXT, additional_params=additional_params,
                                                            client_id=client_id, person_id=person_id)
    return client_id, contract_id


@dispatcher.add_method
def test_partners_contract():
    context = RSYA_OFFER_RU
    params = dict(
        service_start_dt=START_DT,
        start_dt=START_DT,
        end_dt=END_DT,
        nds=context.nds.nds_id,
        manager_uid=Managers.NIGAI.uid,
        partners_contract_type=context.rsya_contract_type
    )
    client_id = balance_steps.ClientSteps.create(params={'NAME': ''})
    person_params = copy.deepcopy(common_defaults.FIXED_UR_PARAMS)
    person_params.update({'is-partner': 1})
    person_id = balance_steps.PersonSteps.create(client_id, context.person_type.code, person_params)
    _, _, contract_id, external_id = \
        balance_steps.ContractSteps.create_partner_contract(context, client_id=client_id, person_id=person_id,
                                                            additional_params=params)
    return client_id, contract_id
