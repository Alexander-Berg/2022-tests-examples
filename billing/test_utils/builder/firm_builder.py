from datetime import datetime
from decimal import Decimal
from typing import Dict, List, Optional

from billing.library.python.calculator.util import to_msk_dt
from billing.library.python.calculator.values import PersonType


def gen_person_category(category: PersonType, is_resident: int = 1, is_legal: int = 1) -> Dict:
    return {'category': category,
            'is_resident': is_resident,
            'is_legal': is_legal
            }


def gen_tax_policy_pct(identity: int, mdh_id: str,
                       dt: datetime, nds_pct: Decimal,
                       nsp_pct: Decimal = Decimal(0),
                       hidden: int = 0) -> Dict:
    return {'id': identity,
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
    percents = percents or [
        gen_tax_policy_pct(1, 'a1', to_msk_dt(datetime(2020, 1, 1)), nds_pct=Decimal('18.00'), hidden=0),
        gen_tax_policy_pct(2, 'a2', to_msk_dt(datetime(2020, 2, 1)), nds_pct=Decimal('16.00'), hidden=1),
        gen_tax_policy_pct(3, 'a3', to_msk_dt(datetime(2021, 1, 1)), nds_pct=Decimal('20.00'), hidden=0)
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
    person_categories = person_categories or [gen_person_category(PersonType.UR),
                                              gen_person_category(PersonType.YT, is_resident=0)
                                              ]
    return {'id': identity,
            'mdh_id': mdh_id,
            'title': title,
            'default_iso_currency': default_iso_currency,
            'region_id': region_id,
            'tax_policies': tax_policies,
            'person_categories': person_categories
            }
