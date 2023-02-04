# -*- coding: utf-8 -*-

from tests import object_builder as ob
from balance.mapper import Paysys
from tests.balance_tests.paysys.paysys_common import create_config


def test_default_allowed_payment_methods(session):
    paysys = ob.Getter(Paysys, 1000).build(session).obj
    config = create_config(session)
    session.delete(config)
    session.flush()
    assert paysys.receipt_deny_reason == "Not allowed payment method (allowed: set(['card'])," \
                                         " current: u'yamoney_wallet')"


def test_allowed_payment_methods(session):
    paysys = ob.Getter(Paysys, 1000).build(session).obj
    config = create_config(session)
    config.value_json = [u'card', u'yamoney_wallet']
    session.flush()
    assert paysys.receipt_deny_reason is None


def test_not_allowed_payment_methods(session):
    paysys = ob.Getter(Paysys, 1000).build(session).obj
    config = create_config(session)
    config.value_json = [u'card', u'webmoney']
    session.flush()
    assert paysys.receipt_deny_reason == "Not allowed payment method (allowed: [u'card', u'webmoney'], current: u'yamoney_wallet')"
