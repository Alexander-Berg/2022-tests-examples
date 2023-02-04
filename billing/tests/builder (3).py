from datetime import datetime
from decimal import Decimal
from typing import Dict, List, Optional

from sendr_utils import enum_value

from billing.hot.calculators.taxi.calculator.core.constants import CASH_COMM_SID, CASHLESS_SID, SUBVENTION_SID
from billing.hot.calculators.taxi.calculator.core.entities.personal_account import ServiceCode
from billing.hot.calculators.taxi.calculator.core.util import to_msk_dt
from billing.hot.calculators.taxi.calculator.tests.const import TAXI_RU_FIRM, UR_PERSON, YT_PERSON


def gen_person_category(category: str, is_resident: int = 1, is_legal: int = 1) -> Dict:
    return {
        'category': category,
        'is_resident': is_resident,
        'is_legal': is_legal
    }


def gen_tax_policy_pct(identity: int, mdh_id: str,
                       dt: datetime, nds_pct: Decimal,
                       nsp_pct: Decimal = Decimal(0),
                       hidden: int = 0) -> Dict:
    return {
        'id': identity,
        'mdh_id': mdh_id,
        'dt': dt,
        'nds_pct': nds_pct,
        'nsp_pct': nsp_pct,
        'hidden': hidden
    }


def gen_tax_policy(identity: int, mdh_id: str,
                   name: str,
                   hidden: int = 0,
                   default_tax: int = 1,
                   resident: int = 1,
                   region_id: int = 225,
                   spendable_nds_id: Optional[int] = None,
                   percents: Optional[List[Dict]] = None
                   ) -> Dict:
    percents = percents or [gen_tax_policy_pct(1, 'a1', to_msk_dt(datetime(2020, 1, 1)), nds_pct=18, hidden=0),
                            gen_tax_policy_pct(2, 'a2', to_msk_dt(datetime(2020, 2, 1)), nds_pct=16, hidden=1),
                            gen_tax_policy_pct(3, 'a3', to_msk_dt(datetime(2021, 1, 1)), nds_pct=20, hidden=0)
                            ]
    return {'id': identity,
            'mdh_id': mdh_id,
            'name': name,
            'hidden': hidden,
            'region_id': region_id,
            'spendable_nds_id': spendable_nds_id,
            'default_tax': default_tax,
            'resident': resident,
            'percents': percents
            }


def gen_firm(identity: int,
             mdh_id: str,
             title: str = 'firm',
             region_id: int = 225,
             default_iso_currency: str = 'RUB',
             person_categories: Optional[List[Dict]] = None,
             tax_policies: Optional[List[Dict]] = None) -> Dict:
    tax_policies = tax_policies or [gen_tax_policy(1, 'a1', 'resident', spendable_nds_id=18, default_tax=1),
                                    gen_tax_policy(2, 'a2', 'nonresident', resident=0, spendable_nds_id=0),
                                    gen_tax_policy(3, 'a3', 'resident_no_vat', resident=1, default_tax=0)
                                    ]
    person_categories = person_categories or [gen_person_category(UR_PERSON),
                                              gen_person_category(YT_PERSON, is_resident=0)
                                              ]
    return {'id': identity,
            'mdh_id': mdh_id,
            'title': title,
            'default_iso_currency': default_iso_currency,
            'region_id': region_id,
            'tax_policies': tax_policies,
            'person_categories': person_categories
            }


def gen_general_contract(contract_id: int, client_id: int,
                         person_id: int,
                         services: List[int],
                         person_type: str = UR_PERSON,
                         dt: datetime = datetime(2020, 1, 1),
                         signed: bool = True,
                         currency: str = 'RUB',
                         firm: int = TAXI_RU_FIRM,
                         postpay: bool = True,
                         netting: bool = True):
    dt = dt.isoformat()
    curr_to_num_code = {'RUB': 810}

    return {
        'client_id': client_id,
        'collaterals': {
            '0': {
                'bank_details_id': 990,
                'collateral_type_id': None,
                'commission': 9,
                'contract2_id': contract_id,
                'credit_type': 1,
                'currency': curr_to_num_code[currency],
                'dt': dt,
                'firm': firm,
                'id': contract_id,
                'is_cancelled': None,
                'is_faxed': None,
                'is_signed': dt if signed else None,
                'num': None,
                'partner_credit': 1,
                'payment_term': 10,
                'netting': int(netting),
                'payment_type': 3 if postpay else 2,
                'personal_account': 1,
                'services': {s: 1 for s in services},
                'unilateral': 1,
            }
        },
        'external_id': 'test_general',
        'id': contract_id,
        'person_id': person_id,
        'person_type': person_type,
        'type': 'GENERAL',
        'passport_id': 793360492,
        'update_dt': dt,
    }


def gen_spendable_contract(contract_id: int, client_id: int,
                           person_id: int, services: List[int],
                           person_type: str = UR_PERSON,
                           dt: datetime = datetime(2020, 1, 1),
                           signed: bool = True,
                           currency: str = 'RUB',
                           firm: int = TAXI_RU_FIRM,
                           nds: int = 18):
    dt = dt.isoformat()
    curr_to_iso_code = {'RUB': 643}

    return {'client_id': client_id,
            'collaterals': {
                '0': {
                    'collateral_type_id': None,
                    'contract2_id': contract_id,
                    'currency': curr_to_iso_code[currency],
                    'dt': dt,
                    'firm': firm,
                    'id': contract_id,
                    'is_cancelled': None,
                    'is_faxed': None,
                    'is_offer': 1,
                    'is_signed': dt if signed else None,
                    'nds': nds,
                    'num': None,
                    'pay_to': 1,
                    'payment_type': 1,
                    'service_start_dt': dt,
                    'services': {s: 1 for s in services},
                }
            },
            'external_id': 'test_spendable',
            'id': contract_id,
            'person_id': person_id,
            'person_type': person_type,
            'type': 'SPENDABLE',
            'passport_id': 793360492,
            'update_dt': dt,
            }


def gen_payout_event(client_id: int, tid: str = '1_1',
                     dt: datetime = datetime(2020, 1, 1),
                     payload: Optional[dict] = None):
    payload = payload or {}
    return {'transaction_id': tid,
            'event_time': dt.isoformat(),
            'client_id': client_id,
            'payload': payload
            }


def gen_cashless_event(client_id: int, contract_id: int, product: str = 'trip_payment',
                       amount: str = '100', terminal_id: int = 123,
                       dt: Optional[datetime] = None, transaction_type: str = 'payment'):
    dt = (dt or datetime.now()).isoformat()
    return {
        'transaction_id': 666,
        'event_time': dt,
        'transaction_time': dt,
        'due': dt,
        'transaction_type': transaction_type,
        'service_transaction_id': '666-1',
        'service_id': CASHLESS_SID,
        'client_id': client_id,
        'product': product,
        'amount': amount,
        'currency': 'RUB',
        'detailed_product': 'trip_payment',
        'tariff_class': 'uberx',
        'oebs_mvp_id': 'VLDc',
        'invoice_date': dt,
        'contract_id': contract_id,
        'payload': {'terminal_id': terminal_id},
    }


def gen_revenue_event(client_id, contract_id, product: str = 'order',
                      service_id: int = CASH_COMM_SID, amount: str = '100',
                      dt: Optional[datetime] = None,
                      aggregation_sign: int = 1,
                      ignore_in_balance: bool = False):
    dt = (dt or datetime.now()).isoformat()
    return {
        'transaction_id': 666,
        'event_time': dt,
        'transaction_time': dt,
        'due': dt,
        'transaction_type': 'payment',
        'orig_transaction_id': 666,
        'service_transaction_id': '666-1',
        'service_id': service_id,
        'client_id': client_id,
        'clid': None,
        'product': product,
        'amount': amount,
        'currency': 'RUB',
        'detailed_product': 'gross_taximeter_payment',
        'ignore_in_balance': ignore_in_balance,
        'tariff_class': 'econom',
        'agglomeration': 'KOS',
        'invoice_date': dt,
        'contract_id': contract_id,
        'aggregation_sign': aggregation_sign,
        'payload': {}
    }


def gen_subvention_event(client_id,
                         contract_id,
                         product: str = 'subsidy',
                         service_id: Optional[int] = None,
                         amount: str = '100',
                         transaction_type: str = 'payment',
                         dt: Optional[datetime] = None,
                         ignore_in_balance: bool = False):
    dt = (dt or datetime.now()).isoformat()
    return {
        'transaction_id': 666,
        'event_time': dt,
        'transaction_time': dt,
        'due': dt,
        'transaction_type': transaction_type,
        'orig_transaction_id': 666,
        'service_transaction_id': '666-1',
        'service_id': service_id or SUBVENTION_SID,
        'client_id': client_id,
        'clid': None,
        'product': product,
        'amount': amount,
        'currency': 'RUB',
        'detailed_product': 'subsidy_commission',
        'ignore_in_balance': ignore_in_balance,
        'tariff_class': 'econom',
        'agglomeration': 'MSKc',
        'invoice_date': dt,
        'contract_id': contract_id,
        'payment_type': None,
        'payload': {}
    }


def gen_thirdparty_event(client_id, contract_id, payment_type: str, paysys_type: str,
                         service_id: int = CASHLESS_SID, amount: str = '100',
                         transaction_type: str = 'payment',
                         dt: Optional[datetime] = None):
    dt = (dt or datetime.now()).isoformat()
    return {'id': 1,
            'trust_id': '666-1',
            'trust_payment_id': '666-1',
            'contract_id': contract_id,
            'person_id': 1,
            'transaction_type': transaction_type,
            'partner_id': client_id,
            'client_id': client_id,
            'dt': dt,
            'service_id': service_id,
            'order_id': 1,
            'invoice_id': 1,
            'iso_currency': 'RUB',
            'paysys_type_cc': paysys_type,
            'payment_type': payment_type,
            'transaction_dt': dt,
            'amount': amount,
            'invoice_eid': 'LS-666-1',
            }


def gen_transfer_event(sender_client_id, sender_contract_id,
                       recipient_client_id, recipient_contract_id,
                       transaction_id: str,
                       amount: str = '100',
                       transfer_type: str = 'selfemployed_rent',
                       transaction_type: str = 'PAYMENT',
                       dt: Optional[datetime] = None,
                       **kwargs):
    dt = (dt or datetime.now()).isoformat()
    return {
        'transaction_id': transaction_id,
        'event_time': dt,
        'amount': amount,
        'currency': 'RUB',
        'transaction_type': transaction_type,
        'transfer_type': transfer_type,
        'sender_billing_contract_id': sender_contract_id,
        'sender_billing_client_id': sender_client_id,
        'recipient_billing_contract_id': recipient_contract_id,
        'recipient_billing_client_id': recipient_client_id,
        **kwargs,
    }


def gen_transfer_cancel_event(sender_client_id, sender_contract_id, transaction_id: str):
    return {
        'transaction_id': transaction_id,
        'sender_billing_contract_id': sender_contract_id,
        'sender_billing_client_id': sender_client_id,
    }


def gen_loc(loc_type: str, **kwargs):
    loc = kwargs
    loc.update({'namespace': 'taxi', 'type': loc_type})
    return loc


def gen_account(
    account_type: str, client_id: int, contract_id: int,
    debit: str = '', credit: str = '',
    currency: str = 'RUB', ts: int = 0
):
    if not (debit or credit):
        raise ValueError('One of credit or debit is required')

    return {
        'loc': gen_loc(account_type, client_id=client_id,
                       contract_id=contract_id,
                       currency=currency),
        'debit': debit or '0',
        'credit': credit or '0',
        'dt': ts,
    }


def gen_personal_account(identity: int, client_id: int, contract_id: int, external_id: str,
                         service_code: Optional[ServiceCode] = None,
                         currency: str = 'RUB'):
    return {
        'obj': {
            'id': identity,
            'contract_id': contract_id,
            'external_id': external_id,
            'iso_currency': currency,
            'type': 'personal_account',
            'service_code': enum_value(service_code, service_code) if service_code is not None else None,
            'hidden': 0,
            'postpay': 1
        },
        'id': identity,
        'version': 1,
        'client_id': client_id,
        'contract_id': contract_id
    }


def gen_partner_product(order_type: str,
                        service_id: int,
                        currency_iso_code: str,
                        product_mdh_id: str,
                        ) -> Dict:
    return {
        'order_type': order_type,
        'service_id': service_id,
        'currency_iso_code': currency_iso_code,
        'product_mdh_id': product_mdh_id,
    }
