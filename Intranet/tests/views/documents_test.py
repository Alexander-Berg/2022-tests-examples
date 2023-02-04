import json

import pytest
from django.core.urlresolvers import reverse

from staff.lib.testing import (
    StaffFactory,
    VisaFactory,
)
from staff.person_profile.views import (
    documents,
)


@pytest.fixture
def api_answer():
    return {'target': {'documents': {
        'visas': [],
    }}}


@pytest.fixture
def tester():
    return StaffFactory()


@pytest.fixture
def get_documents(rf, tester):
    def func():
        request = rf.get(reverse('profile:documents', kwargs={'login': tester.login}))
        request.user = tester.user
        request.service_is_readonly = False
        response = documents.get(request, tester.login)
        return json.loads(response.content)
    return func


@pytest.mark.django_db
def test_empty_get(get_documents, api_answer):
    answer = get_documents()
    assert answer == api_answer


@pytest.mark.django_db
def test_visas_get(tester, get_documents, api_answer):
    visas = [VisaFactory(person=tester), VisaFactory(person=tester)]

    answer = get_documents()

    api_answer['target']['documents']['visas'] = [
        {
            'id': v.id,
            'description': v.description,
            'country': v.country,
            'number': v.number,
            'is_multiple': v.is_multiple,
            'issue_date': v.issue_date.isoformat(),
            'expire_date': v.expire_date and v.expire_date.isoformat(),
        }
        for v in visas
    ]
    assert answer == api_answer


@pytest.fixture
def edit_documents(rf, tester):
    def func(method_type='get', post_kwargs=None, target_login=tester.login):
        reversed_url = reverse('profile:documents_edit', kwargs={'login': target_login})
        if method_type == 'get':
            request = rf.get(reversed_url)
        else:
            request = rf.post(reversed_url, json.dumps(post_kwargs), 'application/json')
        request.user = tester.user
        setattr(request, 'service_is_readonly', False)
        response = documents.edit(request, target_login)
        return json.loads(response.content)
    return func
