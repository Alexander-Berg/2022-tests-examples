# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from future import standard_library

standard_library.install_aliases()

import pytest

import tests.object_builder as ob

from brest.core.tests import utils as test_utils


def create_place(session, client):
    return ob.PlaceBuilder.construct(
        session,
        client=client,
    )


def create_mcb_category(session):
    return ob.MkbCategoryBuilder.construct(session)


@pytest.fixture(name='mcb_category')
def fixture_mcb_category():
    return create_mcb_category(test_utils.get_test_session())
