# -*- coding: utf-8 -*-
import pytest
from mock import patch

from balance.constants import PaymentMethodIDs, PaysysGroupIDs
from tests import object_builder as ob
from balance.mapper import Paysys, Config

allowed_receipts_payment_methods_item = 'ALLOWED_RECEIPTS_PAYMENT_METHODS'


def create_config(session):
    config = ob.Getter(Config, 'ALLOWED_RECEIPTS_PAYMENT_METHODS').build(session).obj
    if not config:
        config = Config(item='ALLOWED_RECEIPTS_PAYMENT_METHODS')
        session.add(config)
        session.flush()
    return config


def create_paysys(session, group_id, payment_method_id, trust_paymethods=None, **kwargs):
    return ob.PaysysBuilder(group_id=group_id, payment_method_id=payment_method_id,
                            trust_paymethods=trust_paymethods or [],
                            **kwargs).build(session).obj


def create_service(session, **kwargs):
    return ob.ServiceBuilder(**kwargs).build(session).obj