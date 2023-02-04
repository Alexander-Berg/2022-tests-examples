import pytest

from unittest.mock import patch, Mock, PropertyMock

from django.test import override_settings
from django.urls.base import reverse

from intranet.femida.src.candidates.choices import CANDIDATE_STATUSES, CONSIDERATION_STATUSES
from intranet.femida.src.offers.choices import OFFER_STATUSES

from intranet.femida.tests import factories as f
from intranet.femida.tests.utils import ctx_combine


pytestmark = pytest.mark.django_db


@pytest.mark.parametrize('checks, status_code, action', (
    ([lambda: True], 200, 'add_consumer'),
    ([lambda: False], 400, 'cancel_consumer'),
))
@patch('celery.utils.nodenames.gethostname', lambda: 'femida')
@override_settings(CELERY_DEFAULT_QUEUE='main-celery-queue')
def test_monitoring_celery_worker(client, checks, status_code, action):
    f.create_waffle_switch('enable_celery_worker_monitoring')
    mocked_action = Mock()
    ctx_managers = ctx_combine(
        patch(
            target='intranet.femida.src.api.monitoring.views.MonitoringCeleryWorkerView.checks',
            new=PropertyMock(return_value=checks),
        ),
        patch(f'celery.app.control.Control.{action}', mocked_action),
    )
    with ctx_managers:
        url = reverse('private-api:monitoring:celery-worker')
        response = client.get(url)

    assert response.status_code == status_code
    mocked_action.assert_called_once_with(
        'main-celery-queue',
        destination=['celery@femida'],
        reply=True,
    )


def test_monitoring_db_consistency_success(client):
    f.create_vacancy()
    f.create_heavy_candidate()
    f.create_offer()
    f.create_interview()
    url = reverse('private-api:monitoring:db-consistency')
    response = client.get(url)
    assert response.status_code == 200


@pytest.mark.parametrize('instance_factory,related_field,related_factory', (
    (f.InterviewFactory, 'candidate', f.CandidateFactory),
    (f.VacancyFactory, 'professional_sphere', f.ProfessionalSphereFactory),
    (f.CandidateProfessionFactory, 'professional_sphere', f.ProfessionalSphereFactory),
    (f.OfferFactory, 'vacancy', f.VacancyFactory),
    (f.OfferFactory, 'candidate', f.CandidateFactory),
))
def test_monitoring_db_consistency_failure(client, instance_factory,
                                           related_field, related_factory):
    instance_factory(**{related_field: related_factory()})
    url = reverse('private-api:monitoring:db-consistency')
    response = client.get(url)
    assert response.status_code == 400


def test_monitoring_offer_consistency_success(client):
    url = reverse('private-api:monitoring:offer-consistency')
    response = client.get(url)
    assert response.status_code == 200


@pytest.mark.parametrize('related_field, errors', (
    ('office', {'has_deleted_office'}),
    ('org', {'has_deleted_org'}),
    ('department', {'has_deleted_department'}),
))
def test_monitoring_offer_consistency_has_deleted_links(client, related_field, errors):
    offer = f.create_offer(status=OFFER_STATUSES.accepted, startrek_hr_key='TEST-1')
    related_instance = getattr(offer, related_field)
    related_instance.is_deleted = True
    related_instance.save()
    url = reverse('private-api:monitoring:offer-consistency')
    response = client.get(url)
    assert response.status_code == 400
    assert response.data['inconsistent_data'].keys() == errors


def test_monitoring_offer_consistency_has_no_boss(client):
    f.OfferFactory(status=OFFER_STATUSES.accepted, startrek_hr_key='TEST-1')
    url = reverse('private-api:monitoring:offer-consistency')
    response = client.get(url)
    assert response.status_code == 400
    assert response.data['inconsistent_data'].keys() == {'has_no_boss'}


@pytest.mark.parametrize('status', (
    OFFER_STATUSES.accepted,
    OFFER_STATUSES.closed,
))
def test_monitoring_offer_consistency_has_no_hr_issue(client, status):
    f.create_offer(status=status, startrek_hr_key='')
    url = reverse('private-api:monitoring:offer-consistency')
    response = client.get(url)
    assert response.status_code == 400
    assert response.data['inconsistent_data'].keys() == {'has_no_hr_issue'}


def test_monitoring_candidate_consistency_success(client):
    f.create_candidate_with_consideration()

    url = reverse('private-api:monitoring:candidate-consistency')
    response = client.get(url)
    assert response.status_code == 200


def test_monitoring_candidate_consistency_fail(client):
    candidate = f.CandidateFactory(status=CANDIDATE_STATUSES.closed)
    f.ConsiderationFactory(candidate=candidate, state=CONSIDERATION_STATUSES.in_progress)

    url = reverse('private-api:monitoring:candidate-consistency')
    response = client.get(url)

    assert response.status_code == 400
