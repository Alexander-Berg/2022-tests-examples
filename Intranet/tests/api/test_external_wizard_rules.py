import pytest

from operator import itemgetter

from django.urls import reverse

from intranet.search.tests.helpers import models_helpers as mh
from intranet.search.tests.helpers.api_helpers import dump_model_list


pytestmark = pytest.mark.django_db(transaction=False)


def _dump_rules(rules):
    keys = ('id', 'search', 'index', 'name', 'rule', 'params')
    return dump_model_list(rules, keys)


def test_get_rules(api_client):
    rules = [mh.ExternalWizardRule(), mh.ExternalWizardRule()]

    url = reverse('external-wizard-rules-get')
    r = api_client.get(url)

    assert r.status_code == 200
    assert sorted(r.json(), key=itemgetter('id')) == _dump_rules(rules)
