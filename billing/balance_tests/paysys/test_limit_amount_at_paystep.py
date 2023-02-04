# -*- coding: utf-8 -*-
import pytest

from balance.constants import PaymentMethodIDs, PaysysGroupIDs
from tests.balance_tests.paysys.paysys_common import create_paysys

allowed_receipts_payment_methods_item = 'ALLOWED_RECEIPTS_PAYMENT_METHODS'


@pytest.mark.parametrize('first_limit', [None, 100, 0])
def test_limit_amount_at_paystep(session, first_limit):
    """простой банковкий способ оплаты с указанным лимитом, будем проверять лимит на пейстепе"""
    paysys = create_paysys(session, group_id=PaysysGroupIDs.default,
                           payment_method_id=PaymentMethodIDs.bank, first_limit=first_limit)
    if paysys.first_limit is None:
        assert paysys.limit_amount_at_paystep is False
    else:
        assert paysys.limit_amount_at_paystep is True


def test_limit_amount_at_paystep_single_account(session):
    """у способов оплаты для ЕЛС, всегда проверяем лимит на пейстепе"""
    paysys = create_paysys(session, group_id=PaysysGroupIDs.default,
                           payment_method_id=PaymentMethodIDs.single_account, first_limit=None)
    assert paysys.limit_amount_at_paystep is True


@pytest.mark.parametrize('group_id, trust_paymethods, is_with_limit_on_paystep',
                         [(PaysysGroupIDs.auto_trust, [], False),
                          (PaysysGroupIDs.default, [], True),
                          (PaysysGroupIDs.auto_trust, [{}], False),
                          (PaysysGroupIDs.auto_trust, [{'max_amount': 99}], True),
                          (PaysysGroupIDs.auto_trust, [{}, {'max_amount': 99}], True),
                          (PaysysGroupIDs.auto_trust, [{}, {'max_amount': None}], True),
                          ])
def test_limit_amount_at_paystep_auto_trust(session, group_id, trust_paymethods, is_with_limit_on_paystep):
    """для способов оплаты через траст как процессинг (2) будем проверять лимит на пейстепе, только если он указан
    хотя бы в одном из trust_paymethods. Для всех остальных групп способов оплаты так же, но если trust_paymethods
     отсутствуют, смотрим на лимит, указанный в способе оплаты"""
    paysys = create_paysys(session, group_id=group_id, first_limit=100,
                           payment_method_id=PaymentMethodIDs.bank)
    paysys.trust_paymethods = trust_paymethods
    assert paysys.limit_amount_at_paystep == is_with_limit_on_paystep
