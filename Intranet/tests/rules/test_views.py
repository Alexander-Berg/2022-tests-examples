from unittest import mock

import pytest
from django.forms import ValidationError
from django.urls import reverse

from intranet.table_flow.tests.helpers import factories as f


pytestmark = [pytest.mark.django_db]


@pytest.mark.parametrize('execute_result, expected', [
    ({'some': 'value'}, {'some': 'value'}),
    (None, {}),
])
def test_execute_rule_view_ok(client, execute_result, expected):
    rule_model = f.Rule()
    url = reverse('rules:execute', kwargs={'slug': rule_model.slug})

    with mock.patch('intranet.table_flow.src.rules.domain_objs.Rule.execute',
                    return_value=execute_result):
        r = client.get(url)

    assert r.status_code == 200
    assert r.json() == expected


def test_execute_rule_view_validation(client):
    rule_model = f.Rule()
    url = reverse('rules:execute', kwargs={'slug': rule_model.slug})
    error_message = {'some_field': ['error']}

    with mock.patch('intranet.table_flow.src.rules.domain_objs.Rule.execute',
                    side_effect=ValidationError(error_message)):
        r = client.get(url)

    assert r.status_code == 400
    assert r.json() == error_message


def test_execute_rule_view_no_rule(client):
    url = reverse('rules:execute', kwargs={'slug': 'unknown'})
    r = client.get(url)
    assert r.status_code == 404
