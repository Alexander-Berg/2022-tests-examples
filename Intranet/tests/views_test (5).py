import collections
import json
import random

import pytest
from unittest.mock import patch

from django.core.urlresolvers import reverse

from staff.departments.models import HeadcountPosition
from staff.departments.tests.factories import HRProductFactory
from staff.lib.testing import ValueStreamFactory

from staff.headcounts.tests.factories import HeadcountPositionFactory
from staff.headcounts.views import login_to_vs


@pytest.mark.django_db
def test_login_to_vs(rf):
    page_size = 50
    value_streams = [ValueStreamFactory() for _ in range(random.randint(15, 20))]
    hr_products = [HRProductFactory() for _ in range(random.randint(15, 20))]
    min_samples = int(page_size * 1.2)
    max_samples = int(page_size * 1.8)

    for i in range(random.randint(min_samples, max_samples)):
        current_login = f'login{i}'
        HeadcountPositionFactory(
            current_login=current_login,
            main_assignment=True,
            valuestream=random.choice(value_streams),
            hr_product=random.choice(hr_products),
        )
        HeadcountPositionFactory(
            current_login=current_login,
            main_assignment=False,
            valuestream=random.choice(value_streams),
            hr_product=random.choice(hr_products),
        )

    with patch('staff.headcounts.views.views.PAGE_SIZE', page_size):
        request = rf.get(reverse('headcounts-api:login_to_vs'))
        request.auth_mechanism = None
        result = login_to_vs(request)

        _validate_result(result, page_size)
        continuation_token = json.loads(result.content).get('continuation_token', None)
        assert continuation_token, 'result should not fit in single page'

        request = rf.get(reverse('headcounts-api:login_to_vs'), data={'continuation_token': continuation_token})
        request.auth_mechanism = None
        result = login_to_vs(request)

        _validate_result(result, page_size)
        assert not json.loads(result.content).get('continuation_token', None), 'result should fit in two pages'


def _validate_result(result, page_size):
    assert result.status_code == 200
    response = json.loads(result.content)
    returned_logins = [key for key in response['data']]
    duplicates = [item for item, count in collections.Counter(returned_logins).items() if count > 1]
    assert duplicates == []
    assert len(response['data']) <= page_size

    for login, data in response['data'].items():
        correct_data = HeadcountPosition.objects.get(current_login=login, main_assignment=True)
        assert data['vs_url'] == correct_data.valuestream.url
        assert data['vs_name'] == correct_data.valuestream.name
        assert data['hr_product_id'] == correct_data.hr_product_id
        assert data['service_id'] == correct_data.hr_product.service_id
