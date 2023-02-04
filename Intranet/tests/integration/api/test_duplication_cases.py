import pytest

from django.urls.base import reverse

from intranet.femida.tests import factories as f


pytestmark = pytest.mark.django_db


def test_duplication_case_list(su_client):
    f.DuplicationCaseFactory.create()
    url = reverse('api:duplication-cases:list')
    response = su_client.get(url)
    assert response.status_code == 200


def test_duplication_case_detail(su_client):
    case = f.DuplicationCaseFactory.create()
    url = reverse('api:duplication-cases:detail', kwargs={'pk': case.id})
    response = su_client.get(url)
    assert response.status_code == 200


def test_duplication_case_cancel(su_client):
    case = f.DuplicationCaseFactory.create()
    url = reverse('api:duplication-cases:cancel', kwargs={'pk': case.id})
    response = su_client.post(url)
    assert response.status_code == 200


def test_duplication_case_mark_unclear(su_client):
    case = f.DuplicationCaseFactory.create()
    url = reverse('api:duplication-cases:mark-unclear', kwargs={'pk': case.id})
    response = su_client.post(url)
    assert response.status_code == 200
