import pytest

from unittest.mock import patch

from django.urls.base import reverse

from intranet.femida.src.candidates.choices import CONTACT_TYPES

from intranet.femida.tests import factories as f
from intranet.femida.tests.factories import CandidateContactFactory
from intranet.femida.tests.mock.startrek import EmptyIssue


pytestmark = pytest.mark.django_db


def test_reference_create_form(su_client):
    url = reverse('api:references:create-form')
    response = su_client.get(url)
    assert response.status_code == 200


def test_reference_form_prefill(su_client):
    vac1 = f.create_publication()
    vac2 = f.create_publication()
    data = {
        'publications': [
            vac1.id,
            vac2.id,
        ],
    }

    url = reverse('api:references:create-form')
    response = su_client.get(url, data)
    assert response.status_code == 200

    # Проверяем предзаполнение формы
    publication_ids_expected = {vac1.id, vac2.id}
    data = response.json()['data']
    publication_ids_received = set(data['publications']['value'])
    assert publication_ids_expected == publication_ids_received


def _get_reference_form_data():
    return {
        'first_name': 'First',
        'last_name': 'Last',
        'email': 'candidate@ya.ru',
        'phone': '+77777777777',
        'comment': 'comment',
        'attachments': [f.AttachmentFactory.create().id],
        'is_candidate_informed': True,
        'publications': [f.create_publication().id, f.create_publication().id],
    }


@pytest.mark.parametrize('create_extra', [
    [],
    ['duplicate'],
    ['duplicate', 'consideration'],
])
@patch('intranet.femida.src.candidates.startrek.issues.create_issue', return_value=EmptyIssue())
def test_reference_create(create_issue_mock, create_extra, su_client):
    data = _get_reference_form_data()
    if 'duplicate' in create_extra:
        # Создаем дубликат для new_strategy
        duplicate = f.CandidateFactory(
            first_name=data['first_name'],
            last_name=data['last_name'],
        )
        CandidateContactFactory.create(
            candidate=duplicate,
            account_id=data['email'],
            type=CONTACT_TYPES.email,
        )

        if 'consideration' in create_extra:
            f.create_completed_consideration(candidate=duplicate)

    url = reverse('api:references:list')
    response = su_client.post(url, data)

    assert response.status_code == 201
    create_issue_mock.assert_called_once()
