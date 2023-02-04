# -*- coding: utf-8 -*-

import pytest

from balance import mapper
from balance import core

from tests import object_builder as ob


@pytest.fixture
def client(session):
    return ob.ClientBuilder().build(session).obj


@pytest.fixture
def paysys(session):
    return ob.Getter(mapper.Paysys, 1003).build(session).obj


@pytest.fixture
def core_obj(session):
    return core.Core(session)
