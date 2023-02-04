import pytest
from celery.result import AsyncResult

from django.conf import settings
from django.contrib.auth import get_user_model
from waffle.models import Switch

from .fixtures import *  # noqa
from .utils import disconnect_actionlog_signals

import intranet.femida.tests.factories as f

from intranet.femida.src.candidates.choices import REFERENCE_STATUSES


User = get_user_model()


def return_empty_str(*args, **kwargs):
    return str()


def return_empty_async_result(*args, **kwargs):
    return AsyncResult(1)


def fake_wiki_format(text, *args, **kwargs):
    return '<div>' + text + '</div>'


def get_mocked_service_ticket(*args, **kwargs):
    return 'service_ticket'


def get_mocked_service_tickets(*args, **kwargs):
    return {'key': 'service_ticket'}


def get_mocked_user_ticket(*args, **kwargs):
    return 'user_ticket'


@pytest.fixture(autouse=True)
def project_setup(db):
    Switch.objects.create(name='enable_fake_oebs', active=True)
    Switch.objects.create(name='ignore_job_issue_workflow', active=True)
    Switch.objects.create(name='enable_rotation', active=True)
    Switch.objects.create(name='enable_startrek_operations', active=True)
    Switch.objects.create(name='enable_head_sync', active=True)
    Switch.objects.create(name='enable_verification_check_on_offer_sending', active=True)
    Switch.objects.create(name='enable_interview_bulk_create', active=True)
    Switch.objects.create(name='show_interview_bulk_create_button', active=True)
    Switch.objects.create(name='enable_tirole', active=True)
    User.objects.create(
        username='robot-femida',
        uid=settings.FEMIDA_ROBOT_UID,
    )


@pytest.fixture(autouse=True)
def no_requests(monkeypatch):
    """
    Везде нужно отдельно мокать походы в разные api.
    Фикстура нужна, чтобы гарантировано упасть при попытке сходить куда-либо.
    """
    monkeypatch.delattr('requests.sessions.Session.request')


@pytest.fixture(autouse=True)
def no_wiki_format(monkeypatch):
    monkeypatch.setattr('intranet.femida.src.wf.models.wiki_format', fake_wiki_format)
    monkeypatch.setattr(
        'intranet.femida.src.notifications.problems.get_diff_in_json',
        return_empty_str,
    )


@pytest.fixture(autouse=True)
def no_get_user_tickets(monkeypatch):
    monkeypatch.setattr(
        'intranet.femida.src.utils.tvm2_client.get_user_ticket',
        get_mocked_user_ticket,
    )


@pytest.fixture(autouse=True)
def no_get_service_tickets(monkeypatch):
    monkeypatch.setattr(
        'intranet.femida.src.utils.tvm2_client.get_service_ticket',
        get_mocked_service_ticket,
    )
    monkeypatch.setattr(
        'intranet.femida.src.utils.tvm2_client.get_service_tickets',
        get_mocked_service_tickets,
    )


@pytest.fixture(autouse=True)
def no_inflector(monkeypatch):
    monkeypatch.setattr(
        'intranet.femida.src.templatetags.inflections._inflect_fio',
        return_empty_str,
    )


@pytest.fixture(autouse=True)
def no_celery_tasks(monkeypatch):
    monkeypatch.setattr('celery.Celery.send_task', return_empty_async_result)


def pytest_runtest_setup():
    disconnect_actionlog_signals()


@pytest.fixture
def expected_achievements():
    data = [
        # Tuple[
        #     User,
        #     approved references with benefits = (total, with offers),
        #     approved references without benefits = (total, with offers),
        #     declined references = (total, with offers),
        # ]
        (f.UserFactory(), (4, 2), (2, 1), (1, 0)),
        (f.UserFactory(), (5, 4), (3, 0), (10, 5)),
        (f.UserFactory(), (1, 0), (1, 1), (1, 1)),
        (f.UserFactory(), (0, 0), (0, 0), (10, 10)),
        (f.UserFactory(), (0, 0), (1, 0), (10, 10)),
    ]

    for user, benefits, no_benefits, declined in data:
        f.create_n_references_with_m_closed_offers(user, benefits, REFERENCE_STATUSES.approved)
        f.create_n_references_with_m_closed_offers(user, no_benefits, REFERENCE_STATUSES.approved_without_benefits)
        f.create_n_references_with_m_closed_offers(user, declined, REFERENCE_STATUSES.rejected)

    # Tuple[User, green count, purple count]
    return [(x[0], x[1][0] + x[2][0], x[1][1] + x[2][1]) for x in data]


@pytest.fixture
def language_tags_fixture():
    from intranet.femida.src.core.models import LanguageTag
    LanguageTag.objects.create(tag='ru', native='русский', name_en='Russian', name_ru='русский')
    LanguageTag.objects.create(tag='en', native='English', name_en='English', name_ru='английский')
    LanguageTag.objects.create(tag='de', native='Deutsch', name_en='German', name_ru='немецкий')
    LanguageTag.objects.create(tag='fr', native='français', name_en='French', name_ru='французский')
    LanguageTag.objects.create(tag='bg', native='български език', name_en='Bulgarian', name_ru='болгарский')
    LanguageTag.objects.create(tag='el', native='Ελληνικά', name_en='Greek', name_ru='греческий')
