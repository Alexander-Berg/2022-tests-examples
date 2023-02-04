# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import pytest

from brest.core.tests import utils as test_utils
from balance import constants as cst, mapper
from tests import object_builder as ob


def not_existing_id(builder_class, id_col_name='id'):
    # type: (ob.ObjectBuilder, str) -> int
    """
    Generate not existing id via ObjectBuilder
    """
    session = test_utils.get_test_session()
    cb = builder_class()
    return cb.generate_unique_id(session, id_col_name)


@pytest.fixture(name='bank')
def create_bank(**kw):
    session = test_utils.get_test_session()
    params = dict(
        name='New snout test bank.',
        bik=str(ob.get_big_number()),
        swift=str(ob.get_big_number()),
        info='New original bank info',
        city='P.-Kamchatskij',
        cor_acc='123456',
    )
    params.update(kw)
    return ob.BankBuilder.construct(session, **params)


@pytest.fixture(name='bank_int')
def create_bank_int(**kw):
    session = test_utils.get_test_session()
    return ob.BankIntBuilder.construct(session, **kw)


@pytest.fixture(name='paysys')
def create_paysys(firm_id=None):
    session = test_utils.get_test_session()
    paysys = ob.PaysysBuilder.construct(
        session,
        firm_id=firm_id or cst.FirmId.YANDEX_OOO,
        payment_method_id=cst.PaymentMethodIDs.bank,
        iso_currency='RUB',
        currency=mapper.fix_crate_base_cc('RUB'),
        extern=1,
    )
    return paysys
