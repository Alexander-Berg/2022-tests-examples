# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from future import standard_library

standard_library.install_aliases()

import pytest
import allure
import tests.object_builder as ob

from brest.core.tests import utils as test_utils


@pytest.fixture(name='person')
@allure.step('create person')
def create_person(client=None, **kwargs):
    session = test_utils.get_test_session()
    client = client or ob.ClientBuilder(name="Snout test client")
    params = dict(
        email=u'test-snout@mail.ya',
        name=u'Pupkin Snout Vasya',
        client=client,
        type=u'ph',
        operator_uid=0,
    )
    params.update(kwargs)
    person = ob.PersonBuilder.construct(session, **params)

    return person
