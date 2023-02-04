import pytest

from django.urls.base import reverse

from intranet.femida.tests import factories as f


pytestmark = pytest.mark.django_db


def _get_candidate_url(candidate_id):
    return reverse(
        'candidate-detail',
        kwargs={
            'candidate_id': candidate_id,
        }
    )


def test_redirection_on_duplicate(client, populate_db_duplicates):
    """
    Используем client: редирект должен работать независимо от прав
    """
    url = _get_candidate_url(populate_db_duplicates['duplicate'].id)
    response = client.get(url)
    assert response.status_code == 302


def test_duplicate_redirects_to_original(client, populate_db_duplicates):
    """
    Проверям, что редирект идет на оригинал, а не на авторизацию, например.
    """
    url = _get_candidate_url(populate_db_duplicates['duplicate'].id)
    response = client.get(url)

    base_url = _get_candidate_url(populate_db_duplicates['original'].id)
    redirect_url = '{base_url}/?redirected_from={duplicate}'.format(
        base_url=base_url,
        duplicate=populate_db_duplicates['duplicate'].id,
    )
    assert response.url == redirect_url


def test_no_redirection_for_no_redirect_param(django_su_client, populate_db_duplicates):
    url = '{}/?no_redirect'.format(
        _get_candidate_url(populate_db_duplicates['duplicate'].id),
    )
    response = django_su_client.get(url)
    assert response.status_code == 200


def test_no_redirection_on_no_duplicate(django_su_client):
    candidate = f.CandidateFactory()
    url = _get_candidate_url(candidate.id)
    response = django_su_client.get(url)
    assert response.status_code == 200
