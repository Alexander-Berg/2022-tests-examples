# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from future import standard_library

standard_library.install_aliases()

import pytest
from sqlalchemy.sql.expression import func

from balance.mapper import Service
from tests import object_builder as ob

from brest.core.tests import utils as test_utils


@pytest.fixture(name='service')
def create_service(**kwargs):
    session = test_utils.get_test_session()
    service = ob.ServiceBuilder.construct(session, **kwargs)
    if kwargs.get('thirdparty'):
        ob.ThirdpartyServiceBuilder.construct(session, id=service.id, **kwargs['thirdparty'])
    return service


@pytest.fixture(name='not_existing_id')
def not_existing_service_id():
    session = test_utils.get_test_session()
    max_service_id = session.query(func.max(Service.id)).scalar()
    return max_service_id + 1000
