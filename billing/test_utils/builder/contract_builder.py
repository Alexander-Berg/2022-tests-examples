from datetime import datetime
from typing import List

from billing.library.python.calculator.values import PersonType


def gen_general_contract(contract_id: int, client_id: int,
                         person_id: int,
                         services: List[int],
                         firm: int,
                         person_type: PersonType = PersonType.UR,
                         dt: datetime = datetime(2020, 1, 1),
                         signed: bool = True,
                         currency: str = 'RUB',
                         postpay: bool = True,
                         netting: bool = True,
                         withholding_commissions_from_payments: bool = False,
                         commission: int = 9,
                         ):
    dt_iso = dt.isoformat()
    curr_to_num_code = {'RUB': 810, 'USD': 840}

    return {'client_id': client_id,
            'collaterals': {
                '0': {
                    'bank_details_id': 990,
                    'collateral_type_id': None,
                    'commission': commission,
                    'contract2_id': contract_id,
                    'credit_type': 1,
                    'currency': curr_to_num_code[currency],
                    'dt': dt_iso,
                    'firm': firm,
                    'id': contract_id,
                    'is_cancelled': None,
                    'is_faxed': None,
                    'is_signed': dt_iso if signed else None,
                    'num': None,
                    'partner_credit': 1,
                    'payment_term': 10,
                    'netting': int(netting),
                    'payment_type': 3 if postpay else 2,
                    'personal_account': 1,
                    'services': {s: 1 for s in services},
                    'withholding_commissions_from_payments': int(withholding_commissions_from_payments),
                    'unilateral': 1,
                }
            },
            'external_id': 'test_general',
            'id': contract_id,
            'person_id': person_id,
            'person_type': person_type,
            'type': 'GENERAL',
            'passport_id': 793360492,
            'update_dt': dt_iso,
            'currency_iso_code': currency,
            }


def gen_spendable_contract(contract_id: int, client_id: int,
                           person_id: int, services: List[int],
                           firm: int, person_type: PersonType = PersonType.UR,
                           dt: datetime = datetime(2020, 1, 1),
                           signed: bool = True,
                           currency: str = 'RUB',
                           nds: int = 18):
    dt_iso = dt.isoformat()
    curr_to_iso_code = {'RUB': 643}

    return {'client_id': client_id,
            'collaterals': {
                '0': {
                    'collateral_type_id': None,
                    'contract2_id': contract_id,
                    'currency': curr_to_iso_code[currency],
                    'dt': dt_iso,
                    'firm': firm,
                    'id': contract_id,
                    'is_cancelled': None,
                    'is_faxed': None,
                    'is_offer': 1,
                    'is_signed': dt_iso if signed else None,
                    'nds': nds,
                    'num': None,
                    'pay_to': 1,
                    'payment_type': 1,
                    'service_start_dt': dt_iso,
                    'services': {s: 1 for s in services},
                }
            },
            'external_id': f'TEST-SPENDABLE/{contract_id}',
            'id': contract_id,
            'person_id': person_id,
            'person_type': person_type,
            'type': 'SPENDABLE',
            'passport_id': 793360492,
            'update_dt': dt_iso,
            }
