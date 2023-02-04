import json
import pytest
import factory
from django.core.urlresolvers import reverse
from mock import Mock, patch
import mock

from staff.departments.models import DepartmentStaff, DepartmentRoles

from staff.lib.testing import StaffFactory


class ExternalLoginFactory(factory.DjangoModelFactory):
    class Meta:
        model = 'verification.ExternalLogin'
    person = factory.SubFactory(StaffFactory)
    uid = factory.Sequence('uid_{}'.format)
    login = uid


@pytest.mark.django_db()
@mock.patch('staff.person.effects.base.requests.get')
@mock.patch('staff.emails.tasks.SyncStaffEmails.locked_run')
def test_ext_login_ctl(yadisk_mock, emails_mock):
    from staff.verification.objects import ExternalLoginCtl
    from staff.verification.models import ExternalLogin

    target_a = StaffFactory(
        intranet_status=1,
        is_dismissed=False,
        login='target_a',
    )
    target_b = StaffFactory(
        intranet_status=1,
        is_dismissed=False,
        login='target_b',
    )
    observer = StaffFactory(
        intranet_status=1,
        is_dismissed=False,
        login='observer',
    )

    qs_fields = (
        'person_id',
        'person__login_passport',
        'person__is_login_passport_confirmed',
        'uid',
        'login',
        'status_active',
    )

    sync_external_logins = Mock()
    with patch('staff.verification.objects.sync_external_logins', sync_external_logins):
        ext_login_ctl = ExternalLoginCtl(target=target_a, observer=observer)
        ext_login_ctl.activate(ext_uid='a', ext_login='a')
        result = ExternalLogin.objects.values_list(*qs_fields)
        expected = [(target_a.id, 'a', True, 'a', 'a', True)]
        for n, result_row in enumerate(result):
            assert result_row == expected[n]
        sync_external_logins.assert_called_once_with(person_ids=[target_a.id])

        sync_external_logins.reset_mock()

        ext_login_ctl = ExternalLoginCtl(target=target_a, observer=observer)
        ext_login_ctl.deactivate()
        result = ExternalLogin.objects.values_list(*qs_fields)
        expected = [(target_a.id, None, False, 'a', 'a', False)]
        for n, result_row in enumerate(result):
            assert result_row == expected[n]
        sync_external_logins.assert_called_once_with(person_ids=[target_a.id])

        sync_external_logins.reset_mock()

        ext_login_ctl = ExternalLoginCtl(target=target_a, observer=observer)
        ext_login_ctl.activate(ext_uid='ab', ext_login='ab')
        result = ExternalLogin.objects.values_list(*qs_fields)
        expected = [
            (target_a.id, 'ab', True, 'a', 'a', False),
            (target_a.id, 'ab', True, 'ab', 'ab', True),
        ]
        for n, result_row in enumerate(result):
            assert result_row == expected[n]
        sync_external_logins.assert_called_once_with(person_ids=[target_a.id])

        sync_external_logins.reset_mock()

        ext_login_ctl = ExternalLoginCtl(target=target_b, observer=observer)
        ext_login_ctl.activate(ext_uid='ab', ext_login='ab')
        result = ExternalLogin.objects.values_list(*qs_fields)
        expected = [
            (target_a.id, None, False, 'a', 'a', False),
            (target_a.id, None, False, 'ab', 'ab', False),
            (target_b.id, 'ab', True, 'ab', 'ab', True),
        ]
        for n, result_row in enumerate(result):
            assert result_row == expected[n]
        sync_external_logins.assert_called_once_with(person_ids=[target_a.id, target_b.id])

        sync_external_logins.reset_mock()

        ext_login_ctl = ExternalLoginCtl(target=target_b, observer=observer)
        ext_login_ctl.activate(ext_uid='b', ext_login='b')
        result = ExternalLogin.objects.values_list(*qs_fields)
        expected = [
            (target_a.id, None, False, 'a', 'a', False),
            (target_a.id, None, False, 'ab', 'ab', False),
            (target_b.id, 'b', True, 'ab', 'ab', False),
            (target_b.id, 'b', True, 'b', 'b', True),
        ]
        for n, result_row in enumerate(result):
            assert result_row == expected[n]
        sync_external_logins.assert_called_once_with(person_ids=[target_b.id])

        sync_external_logins.reset_mock()

        ext_login_ctl = ExternalLoginCtl(target=target_a, observer=observer)
        ext_login_ctl.activate(ext_uid='a', ext_login='a')
        result = ExternalLogin.objects.values_list(*qs_fields)
        expected = [
            (target_a.id, 'a', True, 'a', 'a', True),
            (target_a.id, 'a', True, 'ab', 'ab', False),
            (target_b.id, 'b', True, 'ab', 'ab', False),
            (target_b.id, 'b', True, 'b', 'b', True),
        ]
        for n, result_row in enumerate(result):
            assert result_row == expected[n]
        sync_external_logins.assert_called_once_with(person_ids=[target_a.id])


@pytest.mark.django_db()
def test__get_person_ids():
    from staff.verification.tasks import _get_person_ids

    target_a = StaffFactory(
        intranet_status=1,
        is_dismissed=False,
        login='target_a',
    )
    target_b = StaffFactory(
        intranet_status=1,
        is_dismissed=False,
        login='target_b',
    )
    target_c = StaffFactory(
        intranet_status=1,
        is_dismissed=False,
        login='target_c',
    )

    ExternalLoginFactory(
        person=target_a, status_active=False, ext_passport_synced=True)
    ExternalLoginFactory(
        person=target_a, status_active=False, ext_passport_synced=False)
    ExternalLoginFactory(
        person=target_a, status_active=True, ext_passport_synced=False)
    ExternalLoginFactory(
        person=target_a, status_active=False, ext_passport_synced=False)

    ExternalLoginFactory(
        person=target_b, status_active=True, ext_passport_synced=True)
    ExternalLoginFactory(
        person=target_b, status_active=False, ext_passport_synced=False)
    ExternalLoginFactory(
        person=target_b, status_active=False, ext_passport_synced=True)

    ExternalLoginFactory(
        person=target_c, status_active=True, ext_passport_synced=True)

    result = _get_person_ids([target_a.id, target_b.id, target_c.id], False)
    expected = [
        (target_a.id, False),
        (target_b.id, False),
        (target_a.id, True),
    ]
    for n, result_row in enumerate(result):
        assert result_row == expected[n]

    result = _get_person_ids([target_a.id, target_b.id, target_c.id], True)
    expected = [
        (target_a.id, False),
        (target_b.id, False),
        (target_a.id, True),
        (target_b.id, True),
        (target_c.id, True),
    ]
    for n, result_row in enumerate(result):
        assert result_row == expected[n]


@pytest.mark.django_db()
def test__sync_state():
    from staff.verification.tasks import _sync_state
    from staff.verification.models import ExternalLogin

    target_a = StaffFactory(
        intranet_status=1,
        is_dismissed=False,
        login='target_a',
    )

    ExternalLoginFactory(
        person=target_a, status_active=False, ext_passport_synced=True)
    ExternalLoginFactory(
        person=target_a, status_active=False, ext_passport_synced=False)
    ExternalLoginFactory(
        person=target_a, status_active=True, ext_passport_synced=True)
    ExternalLoginFactory(
        person=target_a, status_active=False, ext_passport_synced=False)

    module = 'staff.verification.tasks'
    swep = Mock()
    with patch.multiple(module, sync_with_external_passport=swep, sleep=Mock()):

        _sync_state(target_a.id, False, False)
        assert swep.call_count == 2
        assert not ExternalLogin.objects.filter(ext_passport_synced=False).exists()

        swep.reset_mock()

        _sync_state(target_a.id, False, True)
        assert swep.call_count == 3
        assert not ExternalLogin.objects.filter(ext_passport_synced=False).exists()


def test_export_login_links(company, fetcher):
    export_login_links_url = reverse('verification-api:export_login_links')

    # убираем руководителя из промежуточного подразделения dep11
    DepartmentStaff.objects.get(
        department=company.dep11,
        staff=company.persons['dep11-chief'],
        role_id=DepartmentRoles.CHIEF.value,
    ).delete()

    yandex_chief = company.persons['yandex-chief']

    chief1 = company.persons['dep1-chief']
    chief12 = company.persons['dep12-chief']
    subchief = company.persons['dep111-chief']
    person = company.persons['dep12-person']

    # Создаём два внешних логина. Для chief1 не создаём.
    person_link = ExternalLoginFactory(
        person=person, status_active=True, ext_passport_synced=True)

    subchief_link = ExternalLoginFactory(
        person=subchief, status_active=True, ext_passport_synced=True)

    with patch('staff.lib.decorators._check_service_id', Mock(side_effect=lambda *a, **b: True)):
        response = fetcher.get(
            '{url}/?internal_uid={chief_uid}&internal_uid={subchief_uid}&internal_uid={person_uid}'.format(
                url=export_login_links_url,
                chief_uid=chief1.uid,
                subchief_uid=subchief.uid,
                person_uid=person.uid,
            )
        )

    assert response.status_code == 200
    response_data = json.loads(response.content)

    assert response_data[chief1.login] == {
        'internal_login': chief1.login,
        'internal_uid': chief1.uid,
        'external_login': None,
        'external_uid': None,
        'is_homeworker': chief1.is_homeworker,
        'affiliation': chief1.affiliation,
        'is_dismissed': chief1.is_dismissed,
        'chief_uid': yandex_chief.uid
    }

    assert response_data[person.login] == {
        'internal_login': person.login,
        'internal_uid': person.uid,
        'external_login': person_link.login,
        'external_uid': person_link.uid,
        'is_homeworker': person.is_homeworker,
        'affiliation': person.affiliation,
        'is_dismissed': person.is_dismissed,
        'chief_uid': chief12.uid
    }

    assert response_data[subchief.login] == {
        'internal_login': subchief.login,
        'internal_uid': subchief.uid,
        'external_login': subchief_link.login,
        'external_uid': subchief_link.uid,
        'is_homeworker': subchief.is_homeworker,
        'affiliation': subchief.affiliation,
        'is_dismissed': subchief.is_dismissed,
        'chief_uid': chief1.uid
    }
