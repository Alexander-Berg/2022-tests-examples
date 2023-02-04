from datetime import datetime
import socket
import time

import mock
import pytest
from django.utils.encoding import smart_text

from staff.test_settings import DATABASES, IS_QYP


def stub(*args, **kwargs):
    pass


@pytest.fixture(autouse=True, scope='session')
def a_wait_for_db():
    conf_db = tuple([DATABASES['default'][attr] for attr in ('HOST', 'PORT')])
    max_attempts = 20

    for _ in range(max_attempts):
        try:
            family = socket.AF_INET6 if IS_QYP else socket.AF_INET
            sock = socket.socket(family)
            sock.connect(conf_db)
            sock.close()
            return
        except Exception:
            time.sleep(0.5)
            pass

    raise IOError('Cant connect to database')


@pytest.fixture(autouse=True, scope='session')
def locked_task():
    from staff.lib.tasks import LockedTask

    old_function = LockedTask.run

    def wrapper(*args, **kw):
        kw['nolock'] = True
        return old_function(*args, **kw)

    LockedTask.run = wrapper


@pytest.fixture(autouse=True)
def stub_side_effects(monkeypatch):
    from staff.person.controllers import effects
    import staff.verification.objects
    import staff.person.effects.base

    monkeypatch.setattr(
        effects,
        'actualize_yandex_disk',
        stub
    )

    monkeypatch.setattr(
        staff.person.effects.base,
        'actualize_yandex_disk',
        stub
    )

    monkeypatch.setattr(
        staff.verification.objects,
        'actualize_yandex_disk',
        stub
    )

    monkeypatch.setattr(
        staff.person.effects.base,
        'actualize_work_phone_task',
        mock.Mock()
    )

    monkeypatch.setattr(
        staff.person.effects.base,
        'push_person_data_to_ad_task',
        mock.Mock()
    )

    monkeypatch.setattr(
        effects,
        'start_emailing',
        stub
    )

    monkeypatch.setattr(
        staff.person.effects.base,
        'start_emailing',
        stub
    )


@pytest.fixture(autouse=True)
def stub_internal_passport(monkeypatch):
    import staff.person.passport.internal

    monkeypatch.setattr(
        staff.person.passport.internal,
        'IntPassport',
        mock.Mock()
    )


@pytest.fixture(autouse=True)
def stub_wikiformatter(monkeypatch):
    from staff.django_intranet_notifications.utils.wiki import WikiFormatter

    def stub_wf(_self, text):
        return smart_text(text)

    monkeypatch.setattr(WikiFormatter, 'to_html', stub_wf)


@pytest.fixture(autouse=True)
def inflections(monkeypatch):
    from staff.person.models import Staff

    class InflectionsMock(object):
        subjective = ''
        genitive = ''
        dative = ''
        accusative = ''
        ablative = ''
        prepositional = ''
        _inflect = []

        def inflect(self, form_name: str):
            self._inflect.append(form_name)
            return ('mocked_inflected_value1 mocked_inflected_value2')

    monkeypatch.setattr(
        Staff,
        'inflections',
        InflectionsMock(),
    )


@pytest.fixture(autouse=True)
def requests(monkeypatch):
    import requests.sessions

    monkeypatch.setattr(
        requests,
        'get',
        stub
    )
    monkeypatch.setattr(
        requests,
        'post',
        stub
    )
    monkeypatch.setattr(
        requests.sessions.Session,
        'get',
        stub
    )
    monkeypatch.setattr(
        requests.sessions.Session,
        'post',
        stub
    )


@pytest.fixture(autouse=True)
def obfuscate(monkeypatch):
    import staff.oebs.controllers.rolluppers.oebs_headcounts_rollupper

    monkeypatch.setattr(
        staff.oebs.controllers.rolluppers.oebs_headcounts_rollupper,
        'obfuscate_headcounts_data',
        stub
    )


@pytest.yield_fixture(autouse=True)
def mocked_calendar():
    with mock.patch('staff.lib.calendar._calendar.get'):
        yield


@pytest.fixture(autouse=True)
def lock_mock(monkeypatch):
    from ylock.backends.yt import YTLock

    monkeypatch.setattr(
        YTLock,
        'acquire',
        mock.Mock,
    )


@pytest.fixture(autouse=True)
def st_issues_repository_mock(monkeypatch):
    from staff.lib.requests import registry

    monkeypatch.setattr(
        registry,
        'get_repository',
        mock.Mock,
    )


@pytest.fixture(scope='session', autouse=True)
def create_roles(django_db_setup, django_db_blocker):
    from staff.departments.models import DepartmentRole
    from staff.departments.models import DepartmentRoles, ValuestreamRoles

    with django_db_blocker.unblock():
        DepartmentRole.objects.get_or_create(
            id=DepartmentRoles.CHIEF.value,
            defaults=dict(
                name='Руководитель',
                name_en='Chief',
                manage_by_idm=False,
                position=20,
                show_in_structure=True,
                slug='chief',
            )
        )
        DepartmentRole.objects.get_or_create(
            id=DepartmentRoles.DEPUTY.value,
            defaults=dict(
                name='Заместитель',
                name_en='Deputy',
                manage_by_idm=False,
                position=30,
                show_in_structure=True,
                slug='deputy',
            )
        )
        DepartmentRole.objects.get_or_create(
            id=DepartmentRoles.HR_PARTNER.value,
            defaults=dict(
                name='HR Партнер',
                name_en='HR Partner',
                manage_by_idm=True,
                position=40,
                show_in_structure=True,
                slug='hr_partner',
            )
        )
        DepartmentRole.objects.get_or_create(
            id=DepartmentRoles.BUDGET_HOLDER.value,
            defaults=dict(
                name='Держатель бюджета',
                name_en='Budget Holder',
                manage_by_idm=True,
                position=50,
                show_in_structure=True,
                slug='budget_holder',
            )
        )
        DepartmentRole.objects.get_or_create(
            id=DepartmentRoles.GENERAL_DIRECTOR.value,
            defaults=dict(
                name='Генеральный директор',
                name_en='General Director',
                manage_by_idm=True,
                position=10,
                show_in_structure=True,
                slug='general_director',
            )
        )
        DepartmentRole.objects.get_or_create(
            id=DepartmentRoles.HR_ANALYST.value,
            defaults=dict(
                name='HR Аналитик',
                name_en='HR Analyst',
                manage_by_idm=True,
                slug=DepartmentRoles.HR_ANALYST.value.lower(),
            )
        )
        DepartmentRole.objects.get_or_create(
            id=DepartmentRoles.MARKET_ANALYST.value,
            defaults=dict(
                name='Аналитик Маркета',
                name_en='Market Analyst',
                manage_by_idm=True,
                slug=DepartmentRoles.MARKET_ANALYST.value.lower(),
            )
        )
        DepartmentRole.objects.get_or_create(
            id=DepartmentRoles.HR_ANALYST_TEMP.value,
            defaults=dict(
                name='HR Аналитик Temp',
                name_en='HR Analyst Temp',
                manage_by_idm=True,
                slug=DepartmentRoles.HR_ANALYST_TEMP.value.lower(),
            )
        )
        DepartmentRole.objects.get_or_create(
            id=DepartmentRoles.CURATOR_EXPERIMENT.value,
            defaults=dict(
                name='Куратор эксперимента',
                name_en='Curator of the Experiment',
                manage_by_idm=True,
                position=70,
                show_in_structure=True,
                slug=DepartmentRoles.CURATOR_EXPERIMENT.value.lower(),
            ),
        )
        DepartmentRole.objects.get_or_create(
            id=DepartmentRoles.CURATOR_BU.value,
            defaults=dict(
                name='Куратор БЮ',
                name_en='Curator of the BU',
                manage_by_idm=True,
                position=60,
                show_in_structure=True,
                slug=DepartmentRoles.CURATOR_BU.value.lower(),
            )
        )
        DepartmentRole.objects.get_or_create(
            id=DepartmentRoles.FINCAB_VIEWER.value,
            defaults=dict(
                name='Смотритель финкабинета',
                name_en='Financial cabinet viewer',
                manage_by_idm=True,
                position=70,
                show_in_structure=False,
                slug=DepartmentRoles.FINCAB_VIEWER.value.lower(),
            )
        )
        DepartmentRole.objects.get_or_create(
            id=ValuestreamRoles.HEAD.value,
            defaults=dict(
                name='Руководитель ValueStream',
                name_en='ValueStream head',
                manage_by_idm=False,
                slug=ValuestreamRoles.HEAD.value.lower(),
            )
        )
        DepartmentRole.objects.get_or_create(
            id=ValuestreamRoles.MANAGER.value,
            defaults=dict(
                name='Менеджер ValueStream',
                name_en='ValueStream manager',
                manage_by_idm=False,
                slug=ValuestreamRoles.MANAGER.value.lower(),
            )
        )
        DepartmentRole.objects.get_or_create(
            id=ValuestreamRoles.HRBP.value,
            defaults=dict(
                name='HRBP ValueStream',
                name_en='HRBP ValueStream',
                manage_by_idm=False,
                slug=ValuestreamRoles.HRBP.value.lower(),
            )
        )

    yield

    with django_db_blocker.unblock():
        from staff.departments.models import DepartmentRoles, ValuestreamRoles
        dep_roles = DepartmentRoles.dict().values()
        vs_roles = ValuestreamRoles.dict().values()
        all_roles = set(dep_roles) | set(vs_roles)
        actual_count = (
            DepartmentRole.objects
            .filter(id__in=all_roles)
            .count()
        )
        assert actual_count >= len(all_roles), 'Some of yours tests removes roles, this affects other tests. Fix it'


@pytest.fixture(scope='session', autouse=True)
def create_proposal_grade_changes(django_db_setup, django_db_blocker):
    from staff.proposal.models import Grade

    with django_db_blocker.unblock():
        Grade.objects.create(level='+2', sort_order=5, name='На два больше', name_en='Two more')
        Grade.objects.create(level='+1', sort_order=4, name='На один больше', name_en='One more')
        Grade.objects.create(level='0', sort_order=3, name='Без изменений', name_en='No change')
        Grade.objects.create(level='-1', sort_order=2, name='Меньше на один', name_en='One less')
        Grade.objects.create(level='-2', sort_order=1, name='Уменьшение на два', name_en='Two less')

    yield


@pytest.fixture(autouse=True)
def mock_access_middleware(monkeypatch):
    from staff.lib.middleware import AccessMiddleware
    monkeypatch.setattr(
        AccessMiddleware,
        'process_view',
        stub
    )


@pytest.fixture(autouse=True)
def some_tvm_applications(settings):
    settings.TVM_APPLICATIONS = {
        'abc': 2012192,
        'avatars-mds': 2002148,
        'badgepay': 2011888,
        'calendar': 2011068,
        'collie': 2001411,
        'ml': 2010122,
        'oebs-hrproc': 2015755,
        'staff-www': 2016175,
        'wiki': 2002678,
        'yadisk': 2000061,
        'yasms': 2000834,
        'staff': 2000053,
        'skotty': 2029478,
    }


@pytest.fixture(scope='session', autouse=True)
def create_currencies(django_db_setup, django_db_blocker):
    from staff.payment.models import Currency
    now = datetime.now()

    with django_db_blocker.unblock():
        Currency.objects.create(code='AMD', created_at=now, modified_at=now)
        Currency.objects.create(code='AZN', created_at=now, modified_at=now)
        Currency.objects.create(code='BYN', created_at=now, modified_at=now)
        Currency.objects.create(code='CHF', created_at=now, modified_at=now)
        Currency.objects.create(code='CNY', created_at=now, modified_at=now)
        Currency.objects.create(code='EGP', created_at=now, modified_at=now)
        Currency.objects.create(code='EUR', created_at=now, modified_at=now)
        Currency.objects.create(code='GBP', created_at=now, modified_at=now)
        Currency.objects.create(code='GEL', created_at=now, modified_at=now)
        Currency.objects.create(code='HKD', created_at=now, modified_at=now)
        Currency.objects.create(code='ILS', created_at=now, modified_at=now)
        Currency.objects.create(code='KZT', created_at=now, modified_at=now)
        Currency.objects.create(code='RON', created_at=now, modified_at=now)
        Currency.objects.create(code='RUB', created_at=now, modified_at=now)
        Currency.objects.create(code='TRY', created_at=now, modified_at=now)
        Currency.objects.create(code='USD', created_at=now, modified_at=now)
        Currency.objects.create(code='UZS', created_at=now, modified_at=now)
        Currency.objects.create(code='ZAR', created_at=now, modified_at=now)

    yield
