# -*- coding: utf-8 -*-
import pytest
from mock import patch

from balance.constants import PaymentMethodIDs, PaysysGroupIDs
from tests.balance_tests.paysys.paysys_common import create_paysys, create_service


@pytest.mark.parametrize('reliable_payer', [True, False])
def test_get_limit_value(session, reliable_payer):
    """для обычных способов оплаты доступный лимит березтся из способа оплаты в зависимости от надежности клиента"""
    paysys = create_paysys(session, group_id=PaysysGroupIDs.default, first_limit=100, second_limit=200,
                           payment_method_id=PaymentMethodIDs.credit_card)
    paysys_limit_value = paysys.get_limit_value(reliable_payer=reliable_payer, service=None)
    if reliable_payer:
        assert paysys_limit_value == paysys.second_limit
    else:
        assert paysys_limit_value == paysys.first_limit


def test_get_limit_value_auto_trust(session):
    """для способов оплаты для траст как процессинг, лимит получаем из терминалов для сервиса,
     используем максимальный"""
    service = create_service(session)
    paysys = create_paysys(session, group_id=PaysysGroupIDs.auto_trust, first_limit=100, second_limit=200,
                           payment_method_id=PaymentMethodIDs.bank)
    patch_get_paysys_payment_methods = patch('balance.trust_api.actions.get_paysys_payment_methods',
                                             return_value=[{'max_amount': 99},
                                                           {'max_amount': 98}])
    with patch_get_paysys_payment_methods:
        paysys_limit_value = paysys.get_limit_value(reliable_payer=True, service=service)
    assert paysys_limit_value == 99


def test_get_limit_value_from_terminals(session):
    """для сервисов и методов оплаты, указанных в конфиге, используем максимальный лимит из терминалов"""
    service = create_service(session)
    paysys = create_paysys(session, group_id=PaysysGroupIDs.default, first_limit=100, second_limit=200,
                           payment_method_id=PaymentMethodIDs.credit_card)
    session.config.__dict__['PAYSYS_TERMINAL_LIMITS_PAYMETHODS'] = [[service.id, paysys.payment_method_id]]
    patch_get_paysys_payment_methods = patch('balance.trust_api.actions.get_paysys_payment_methods',
                                             return_value=[{'max_amount': 99},
                                                           {'max_amount': 98}])
    patch_get_terminals_limit = patch('balance.trust_api.actions.get_terminals_limit',
                                      return_value=97)

    with patch_get_paysys_payment_methods, patch_get_terminals_limit:
        paysys_limit_value = paysys.get_limit_value(reliable_payer=True, service=service)
    assert paysys_limit_value == 97
