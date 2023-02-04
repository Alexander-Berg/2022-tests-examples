import itertools

from btestlib.constants import Regions, ContractCommissionType, Firms, Currencies, PersonTypes, Paysyses
from temp.igogor.balance_objects import Contexts
from .utils import get_test_case_name

from balance.tests.paystep.utils import post_pay_contract_personal_account_fictive

TESTS_PREFIX = 'SHOP_SPB_SOFT'


def get_contract(context, with_contract):
    if not with_contract:
        return []

    return [
        post_pay_contract_personal_account_fictive(
            contract_type=ContractCommissionType.NO_AGENCY,
            person_type=context.person_type,
            firm_id=Firms.SPB_SOFTWARE_1483.id,
            service_list=[context.service.id],
            currency=context.currency.num_code,
        )
    ]


def get_expected_paysyses(context, with_agency, with_contract, currency):
    paysyses = {
        'EUR': {
            PersonTypes.HK_YT.code: [
                Paysyses.CC_HK_YT_SPB_SOFTWARE_EUR.id,
                Paysyses.BANK_HK_YT_SPB_SOFTWARE_EUR_GENERATED.id,
            ],
            PersonTypes.HK_YTPH.code: [
                Paysyses.CC_HK_YTPH_SPB_SOFTWARE_EUR.id,
                Paysyses.BANK_HK_YTPH_SPB_SOFTWARE_EUR.id,
            ]
        },
        'USD': {
            PersonTypes.HK_YT.code: [
                Paysyses.CC_HK_YT_SPB_SOFTWARE_USD.id,
                Paysyses.BANK_HK_YT_SPB_SOFTWARE_USD_GENERATED.id,
            ],
            PersonTypes.HK_YTPH.code: [
                Paysyses.CC_HK_YTPH_SPB_SOFTWARE_USD.id,
                Paysyses.BANK_HK_YTPH_SPB_SOFTWARE_USD.id,
            ]
        }
    }

    with_contract_paysyses = {
        str(Firms.SPB_SOFTWARE_1483.id): {
            currency: {
                context.person_type.code: paysyses[currency][context.person_type.code]
            }
        }
    }

    without_contract_paysyses = {
        str(Firms.SPB_SOFTWARE_1483.id): {
            'EUR': paysyses['EUR']  # default firm currency
        }
    }

    result = {
        'without_contract': without_contract_paysyses
    }
    if with_contract:
        result['with_contract'] = with_contract_paysyses

    return result


def get_hk_yt_spb_software_cases(
    just=None,  # type: Optional[List[str]]
):  # type: (...) -> list[tuple[str, Context, dict, dict]]
    cases = []
    for context, with_agency, with_contract, currency in itertools.product(
        [
            Contexts.SHOP_HK_CONTEXT,
            Contexts.SHOP_HK_PH_CONTEXT,
        ],
        [False],
        [True, False],
        [Currencies.EUR, Currencies.USD]
    ):
        case_context = context.new(
            currency=currency,
        )

        case_name = get_test_case_name(
            TESTS_PREFIX, case_context, with_agency, with_contract=with_contract, pay_method=currency.iso_code
        )

        if just is not None and case_name not in just:
            continue

        given = {
            'with_agency': with_agency,
            'persons': [{'type': case_context.person_type}],
            'region_id': case_context.region.id,
            'contracts': get_contract(case_context, with_contract=with_contract)

        }
        expected = {
            'offer_type_id': 0,
            'expected_paysys_list': get_expected_paysyses(
                case_context, with_agency, with_contract, currency.iso_code
            )
        }

        cases.append(
            (
                case_name,
                case_context,
                given,
                expected,
            )
        )

    return cases
