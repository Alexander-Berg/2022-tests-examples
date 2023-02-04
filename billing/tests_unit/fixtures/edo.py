# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from future import standard_library
from future.builtins import str as text

standard_library.install_aliases()

import datetime
import pytest

from balance import mapper, constants as cst
import tests.object_builder as ob

from brest.core.tests import utils as test_utils


@pytest.fixture(name='edo_offer')
def create_edo_offer(active_start_date=datetime.datetime.now(), active_end_date=None, edo_status='FRIENDS', blocked=False, default_flag=True, enabled_flag=True, firm_id=cst.FirmId.YANDEX_OOO):
    session = test_utils.get_test_session()
    person_inn, person_kpp = ob.get_big_number(), ob.get_big_number()

    existing_edo = mapper.EdoOffer(
        person_inn=text(person_inn),
        person_kpp=text(person_kpp),
        firm_id=firm_id,
        active_start_date=active_start_date,
        active_end_date=active_end_date,
        status=edo_status,
        blocked=blocked,
        default_flag=default_flag,
        enabled_flag=enabled_flag,
        edo_type_id=1,
        org_orarowid=text(ob.get_big_number()),
        inv_orarowid=text(ob.get_big_number()),
    )
    session.add(existing_edo)
    session.flush()

    return existing_edo
