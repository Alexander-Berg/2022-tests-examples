# coding: utf-8
import json

import pytest

from review.oebs import const
from review.oebs.sync import fake


@pytest.mark.parametrize('data_type', const.OEBS_DATA_TYPES)
def test_fake_generators_no_exceptions(person, data_type):
    data = fake.generate_data(data_types=[data_type], logins=[person.login])
    assert data
    assert json.dumps(data)
