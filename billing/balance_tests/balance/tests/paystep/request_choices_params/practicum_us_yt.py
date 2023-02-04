# -*- coding: utf-8 -*-
import itertools

from balance.tests.paystep.utils import post_pay_contract_personal_account_fictive
from btestlib.constants import (
    Paysyses,
    PersonTypes,
    Services,
    ContractCommissionType,
    Firms,
    Regions,
    Currencies,
)
from temp.igogor.balance_objects import Contexts
from .utils import get_test_case_name

TESTS_PREFIX = 'PRACTICUM'


def get_practicum_us_yt_contract(context, with_agency, with_contract):
    if with_contract:
        return [
            post_pay_contract_personal_account_fictive(
                contract_type=ContractCommissionType.USA_OPT_AGENCY if with_agency else ContractCommissionType.USA_OPT_CLIENT,
                person_type=context.person_type,
                firm_id=Firms.YANDEX_INC_4.id,
                service_list=[Services.PRACTICUM.id],
                currency=Currencies.USD.num_code,
                additional_params={'WHOLESALE_AGENT_PREMIUM_AWARDS_SCALE_TYPE': 0}
            )
        ]
    return []


def get_practicum_us_yt_expected_paysyses(context, with_agency, with_contract, pay_method):
    paysyses = {
        'card': {
            PersonTypes.US_YT.code: [Paysyses.CC_US_YT_UR_USD.id],
            PersonTypes.US_YT_PH.code: [Paysyses.CC_US_YT_PH_USD.id],
        },
        'bank': {
            PersonTypes.US_YT.code: [Paysyses.BANK_US_YT_UR_USD.id],
            PersonTypes.US_YT_PH.code: [Paysyses.BANK_US_YT_PH_USD.id],
        }
    }
    paysyses_wo_contract = {
        'without_contract': {
            str(Firms.YANDEX_INC_4.id): {
                'USD': paysyses[pay_method],
            },
        },
    }
    paysyses_w_contract = {
        'with_contract': {
            str(Firms.YANDEX_INC_4.id): {
                'USD': {
                    context.person_type.code: paysyses[pay_method][context.person_type.code]
                },
            },
        },
    }

    if with_agency and with_contract:
        return dict(paysyses_w_contract, **paysyses_wo_contract)
    elif with_contract:
        return paysyses_w_contract
    else:
        return paysyses_wo_contract


def gen_practicum_us_yt_test_cases(
        just=None,  # type: Optional[List[str]]
):
    """
    if "just" is not None, it should be a list of case names to return

    Cases:
    PRACTICUM_us_yt_agent_contract_card
    PRACTICUM_us_yt_agent_not_contract_card
    PRACTICUM_us_yt_not_agent_contract_card
    PRACTICUM_us_yt_not_agent_not_contract_card
    PRACTICUM_us_ytph_agent_contract_card
    PRACTICUM_us_ytph_agent_not_contract_card
    PRACTICUM_us_ytph_not_agent_contract_card
    PRACTICUM_us_ytph_not_agent_not_contract_card
    """

    return [
        (
            get_test_case_name(TESTS_PREFIX, context, with_agency, with_contract, pay_method),
            context.new(with_contract=with_contract),
            {
                'with_agency': with_agency,
                'persons': [{'type': context.person_type}],
                'region_id': Regions.SW.id,
                'contracts': get_practicum_us_yt_contract(context, with_agency, with_contract)
            },
            {
                'offer_type_id': 0 if with_contract else 91,
                'expected_paysys_list': get_practicum_us_yt_expected_paysyses(
                    context, with_agency, with_contract, pay_method,
                )
            },
        )
        for context, with_agency, with_contract, pay_method in itertools.product(
            [Contexts.PRACTICUM_US_YT_UR, Contexts.PRACTICUM_US_YT_PH],
            [True, False],
            [True, False],
            [
                'card',
                # 'bank',  # Not active yet
            ]
        )
        if just is None or get_test_case_name(TESTS_PREFIX, context, with_agency, with_contract, pay_method) in just
    ]
