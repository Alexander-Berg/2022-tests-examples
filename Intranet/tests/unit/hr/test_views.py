import pretend
import pytest
import xlrd
from django.core import mail
from django.core.urlresolvers import reverse

from freezegun import freeze_time

from common import factories
from common.intranet import make_boss_for
from plan.common.utils.timezone import now
from plan.hr import models
from plan.internal_roles.utils import add_perm
from plan.staff.constants import DEPARTMENT_ROLES
from plan.roles.models import Role

pytestmark = pytest.mark.django_db


def person_compact(staff):
    return {
        'full_name': staff.get_full_name(),
        'id': staff.id,
        'is_dismissed': staff.is_dismissed,
        'login': staff.login
    }


def person_serialize(staff):
    data = person_compact(staff)
    data.update({
        'department': {
            'id': staff.department.id,
            'name': staff.department.i_name
        },
    })
    return data


def person_serialize_with_subrodinates(person):
    data = person_serialize(person)
    data.update(
        {'has_subordinates': bool(person.get_subordinate_logins(only_direct=False))}
    )
    return data


def service_serialize(service):
    return {
        'id': service.id,
        'name': service.i_name,
        'owner': person_compact(service.owner),
        'slug': service.slug,
        'state': service.state,
        'state_display': service.get_state_display(),
    }


def role_serialize(role):
    return {
        'id': role.id,
        'name': role.i_name,
        'scope': {
            'id': role.scope.slug,
            'name': role.scope.name,
            'scope_id': role.scope.id,
        },
        'code': role.code,
        'service': role.service.id if role.service else None,
        'is_exportable': role.is_exportable,
    }


@pytest.fixture
def staff_with_importance(staff_factory):
    staff = staff_factory()
    add_perm('change_importance', staff)
    return staff


def compose_history(service_member, changes):
    changes.sort()
    first_modified, first_rate = changes[0]
    with freeze_time(first_modified):
        factories.PartRateHistoryFactory(service_member=service_member, old_part_rate=None, new_part_rate=first_rate)
    for (_, old_rate), (modified, new_rate) in zip(changes[:-1], changes[1:]):
        with freeze_time(modified):
            factories.PartRateHistoryFactory(
                service_member=service_member, old_part_rate=old_rate, new_part_rate=new_rate
            )


def test_export_teams(client, staff_with_importance):
    service = factories.ServiceFactory(is_important=True)
    sm = [factories.ServiceMemberFactory(service=service) for _ in range(6)]

    compose_history(sm[0], [
        ('2017-01-01', 0.5),
        ('2017-05-20', 0.7),
    ])
    compose_history(sm[1], [
        ('2017-02-02', 0.6),
    ])
    compose_history(sm[2], [
        ('2017-03-26', 0.3),
        ('2017-05-26', 0.4)
    ])
    compose_history(sm[3], [
        ('2017-06-06', 0.5),
        ('2017-08-10', 0.6),
    ])
    compose_history(sm[4], [
        ('2017-03-05', 0.1),
        ('2017-04-06', 0.2),
        ('2017-05-07', 0.3),
        ('2017-05-30', 0.4),
    ])
    compose_history(sm[5], [
        ('2017-04-01', 0.1),
        ('2017-05-01', None)
    ])

    client.login(staff_with_importance.login)
    url = reverse('hr:important_teams_with_period')

    response = client.json.get(
        url,
        data={
            'year_from': 2017,
            'month_from': 3,
            'year_to': 2017,
            'month_to': 5,
        },
    )
    assert response.status_code == 200

    assert response.json()['content']['message'] == 'Запущена генерация отчета. Файл отчета придет вам на почту.'

    assert len(mail.outbox) == 1

    email = mail.outbox[0]
    assert email.subject == 'Отчет occupancy.yandex-team.ru'
    assert len(email.attachments) == 1
    assert email.attachments[0][0] == 'important_service_members.xls'

    book = xlrd.open_workbook(file_contents=email.attachments[0][1])
    sheet = book.sheet_by_index(0)
    assert sheet.ncols == 10
    assert sheet.nrows == 6
    report_history = {}
    for row in range(1, sheet.nrows):
        report_history[sheet.cell_value(row, 0)] = [sheet.cell_value(row, col) for col in range(7, 10)]

    assert report_history[sm[0].staff.login] == ['0.5000', '0.5000', '0.7000']
    assert report_history[sm[1].staff.login] == ['0.6000', '0.6000', '0.6000']
    assert report_history[sm[2].staff.login] == ['-', '0.3000', '0.3000']
    assert sm[3].staff.login not in report_history
    assert report_history[sm[4].staff.login] == ['0.1000', '0.2000', '0.3000']
    assert report_history[sm[5].staff.login] == ['-', '0.1000', '0']


def test_export_teams_bad_params(client, staff_with_importance):
    client.login(staff_with_importance.login)
    url = reverse('hr:important_teams_with_period')

    response = client.json.get(
        url,
        data={
            'year_from': 2017,
            'month_from': 1,
            'year_to': 2017,
            'month_to': 14,
        },
    )
    assert response.status_code == 409

    assert response.json()['error']['params'][0]['month_to'] == ['Ensure this value is less than or equal to 12.']

    assert len(mail.outbox) == 0


def test_export_teams_without_permission(client):
    url = reverse('hr:important_teams_with_period')
    response = client.json.get(
        url,
        data={
            'year_from': 2017,
            'month_from': 1,
            'year_to': 2017,
            'month_to': 14,
        },
    )

    assert response.status_code == 302
    assert 'passport.yandex-team.ru' in response.url


def test_mark_important(client, staff_with_importance):
    service = factories.ServiceFactory()

    client.login(staff_with_importance.login)
    url = reverse('hr:mark_important')

    response = client.json.post(
        url,
        data=[service.id],
    )
    assert response.status_code == 200

    service.refresh_from_db()
    assert service.is_important is True


@pytest.fixture
def base_data(staff_with_importance):
    service = factories.ServiceFactory()
    role = factories.RoleFactory()
    service_member = factories.ServiceMemberFactory(
        service=service,
        staff=staff_with_importance,
        role=role,
    )

    return pretend.stub(
        staff=staff_with_importance,
        service=service,
        role=role,
        service_member=service_member,
    )


def test_edit_participation(client, base_data):
    client.login(base_data.staff.login)
    url = reverse('hr:edit_participation')

    response = client.json.post(
        url,
        data=[{
            'login': base_data.staff.login,
            'service_id': base_data.service.id,
            'role_id': base_data.role.id,
            'rate': 1,
        }],
    )
    assert response.status_code == 200

    rec = models.PartRateHistory.objects.select_related('service_member', 'staff').last()
    assert rec.service_member == base_data.service_member
    assert rec.staff == base_data.staff
    assert rec.old_part_rate is None
    assert rec.new_part_rate == 1


def test_edit_participation_by_boss(client, base_data):
    boss = make_boss_for(base_data.staff)

    client.login(boss.login)
    url = reverse('hr:edit_participation')

    response = client.json.post(
        url,
        data=[{
            'login': base_data.staff.login,
            'service_id': base_data.service.id,
            'role_id': base_data.role.id,
            'rate': 1,
        }],
    )
    assert response.status_code == 200

    rec = models.PartRateHistory.objects.select_related('service_member', 'staff').last()
    assert rec.service_member == base_data.service_member
    assert rec.staff == boss
    assert rec.old_part_rate is None
    assert rec.new_part_rate == 1


def test_edit_participation_by_stranger(client, base_data, staff_factory):
    staff = staff_factory()
    client.login(staff.login)
    url = reverse('hr:edit_participation')

    response = client.json.post(
        url,
        data=[{
            'login': base_data.staff.login,
            'service_id': base_data.service.id,
            'role_id': base_data.role.id,
            'rate': 1,
        }],
    )
    assert response.status_code == 403


def test_service_participation_backend(client, staff_factory):
    staff = staff_factory()
    service = factories.ServiceFactory(owner=staff)
    service_member = factories.ServiceMemberFactory(service=service, part_rate=1)

    client.login(staff.login)
    url = reverse('hr:service_participation_backend', args=[service.id])

    response = client.json.get(url)
    assert response.status_code == 200

    json = response.json()['content']
    assert json['permissions']['can_send_dispute_request'] is False
    assert len(json['participation_data']) == 1
    assert json['participation_data'][0]['persons'][0]['rate'] == 1
    assert json['participation_data'][0]['persons'][0]['role']['id'] == service_member.role.id
    assert json['participation_data'][0]['persons'][0]['permissions'] == (
        {'can_edit_all_participation_rates': False, 'can_edit_participation_rates': False}
    )
    assert json['participation_data'][0]['scope'] == service_member.role.scope.name


def test_service_participation_backend_by_boss(client):
    staff = factories.StaffFactory()
    service = factories.ServiceFactory()
    factories.ServiceMemberFactory(service=service, staff=staff, part_rate=1)

    boss = make_boss_for(staff)

    client.login(boss.login)
    url = reverse('hr:service_participation_backend', args=[service.id])

    response = client.json.get(url)
    assert response.status_code == 200

    json = response.json()['content']
    assert json['permissions']['can_send_dispute_request'] is False
    assert json['participation_data'][0]['persons'][0]['permissions'] == (
        {'can_edit_all_participation_rates': True, 'can_edit_participation_rates': True}
    )


def test_service_participation_backend_by_owner(client, service_with_owner):
    client.login(service_with_owner.owner.login)
    url = reverse('hr:service_participation_backend', args=[service_with_owner.id])

    response = client.json.get(url)
    assert response.status_code == 200

    json = response.json()['content']
    assert json['permissions']['can_send_dispute_request'] is True


def test_service_participation_history(client, service_with_owner):
    service_member = factories.ServiceMemberFactory(service=service_with_owner)
    rate = factories.PartRateHistoryFactory(service_member=service_member)

    client.login(service_with_owner.owner.login)
    url = reverse('hr:service_participation_history', args=[service_with_owner.id])

    response = client.json.get(url)
    assert response.status_code == 200
    data = response.json()['content']

    service = service_serialize(service_with_owner)
    assert data['service'] == service
    assert len(data['participation_history']) == 1

    history = data['participation_history'][0]

    assert history['service'] == service
    assert history['person'] == person_serialize(service_member.staff)
    assert history['person_changed'] == person_serialize(rate.staff)
    assert history['role'] == {
        'id': rate.service_member.role.id,
        'name': rate.service_member.role.name,
        'scope': {
            'id': rate.service_member.role.scope.slug,
            'name': rate.service_member.role.scope.name,
            'scope_id': rate.service_member.role.scope.id,
        },
        'code': rate.service_member.role.code,
        'service': rate.service_member.role.service,
        'is_exportable': True,
    }
    assert history['new'] == history['old'] and history['old'] is None
    assert 'timestamp' in history


def test_person_participation_backend(client, staff_factory):
    staff = staff_factory()
    client.login(staff.login)

    service = factories.ServiceFactory()
    service2 = factories.ServiceFactory()
    role = factories.RoleFactory()
    factories.ServiceMemberFactory(service=service, staff=staff, role=role, part_rate=0.3)
    factories.ServiceMemberFactory(service=service, staff=staff, role=role, part_rate=0.5)
    factories.ServiceMemberFactory(service=service2, staff=staff, role=role, part_rate=0.8)

    url = reverse('hr:person_participation_backend', args=[staff.login])

    response = client.json.get(url)
    assert response.status_code == 200

    json = response.json()['content']
    assert json['person']['has_subordinates'] is False
    assert json['person']['login'] == staff.login
    assert json['permissions'] == {
        'can_send_dispute_request': True,
        'can_edit_participation_rates': False,
    }
    rates = [item['rate'] for item in json['participation_data']]
    assert sorted(rates) == [0.3, 0.5, 0.8]


def test_person_participation_backend_by_boss(client):
    staff = factories.StaffFactory()
    boss = make_boss_for(staff)

    client.login(boss.login)
    url = reverse('hr:person_participation_backend', args=[staff.login])

    response = client.json.get(url)
    assert response.status_code == 200

    json = response.json()['content']
    assert json['person']['has_subordinates'] is False
    assert json['person']['login'] == staff.login
    assert json['permissions'] == {
        'can_send_dispute_request': False,
        'can_edit_participation_rates': True,
    }


def test_person_participation_backend_by_boss_of_boss(client):
    staff = factories.StaffFactory()
    boss = make_boss_for(staff)

    client.login(boss.login)
    url = reverse('hr:person_participation_backend', args=[boss.login])

    response = client.json.get(url)
    assert response.status_code == 200

    json = response.json()['content']
    assert json['person']['has_subordinates'] is True
    assert json['person']['login'] == boss.login
    assert json['permissions'] == {
        'can_send_dispute_request': True,
        'can_edit_participation_rates': False,
    }


def test_person_participation_dispute_review(client, service_with_owner):
    staff = service_with_owner.owner
    role = Role.objects.get(code=Role.EXCLUSIVE_OWNER)

    client.login(staff.login)
    url = reverse('hr:person_participation_dispute', args=[staff.login, 'review'])

    response = client.json.get(
        url,
        data={
            'service': service_with_owner.id,
            'role': role.id,
        },
    )
    assert response.status_code == 200

    json = response.json()['content']
    assert json['copy'] == 'occupancy-hr@yandex-team.ru'
    assert json['recipients'][0]['login'] == staff.login


def test_person_participation_dispute_send_review(client, service_with_owner):
    staff = service_with_owner.owner
    staff.first_name = 'Вася'
    staff.last_name = 'Пупкин'
    staff.save()
    role = Role.objects.get(code=Role.EXCLUSIVE_OWNER)

    client.login(staff.login)
    url = reverse('hr:person_participation_dispute', args=[staff.login, 'review'])

    response = client.json.post(
        url,
        data={
            'service': service_with_owner.id,
            'role': role.id,
        },
    )
    assert response.status_code == 200

    assert len(mail.outbox) == 1

    email = mail.outbox[0]
    assert email.subject == f'Изменение занятости Васи Пупкина в сервисе {service_with_owner.name}'


def test_person_participation_dispute_raise(client, service_with_owner):
    staff = service_with_owner.owner
    role = Role.objects.get(code=Role.EXCLUSIVE_OWNER)

    client.login(staff.login)
    url = reverse('hr:person_participation_dispute', args=[staff.login, 'raise'])

    response = client.json.get(
        url,
        data={
            'service': service_with_owner.id,
            'role': role.id,
        },
    )
    assert response.status_code == 200

    json = response.json()['content']
    assert len(json['recipients']) == 1
    assert json['recipients'][0]['login'] == staff.login


def test_person_participation_dispute_send_raise(client, service_with_owner):
    staff = service_with_owner.owner
    staff.first_name = 'Вася'
    staff.last_name = 'Пупкин'
    staff.save()
    role = Role.objects.get(code=Role.EXCLUSIVE_OWNER)

    client.login(staff.login)
    url = reverse('hr:person_participation_dispute', args=[staff.login, 'raise'])

    response = client.json.post(
        url,
        data={
            'service': service_with_owner.id,
            'role': role.id,
        },
    )
    assert response.status_code == 200

    assert len(mail.outbox) == 1

    email = mail.outbox[0]
    assert email.subject == f'Увеличение занятости Васи Пупкина в сервисе {service_with_owner.name}'
    assert 'Вася Пупкин' in email.body
    assert 'Васи Пупкина' in email.body


def test_person_participation_dispute_raise_by_stranger(client, service_with_owner):
    staff = factories.StaffFactory()
    role = Role.objects.get(code=Role.EXCLUSIVE_OWNER)

    client.login(staff.login)
    url = reverse('hr:person_participation_dispute', args=[service_with_owner.owner.login, 'raise'])

    response = client.json.get(
        url,
        data={
            'service': service_with_owner.id,
            'role': role.id,
        },
    )
    assert response.status_code == 403


@pytest.mark.parametrize('direct', [True, False, None])
def test_person_participation_subordinates(direct, client, service_with_owner, staff_factory):
    service = service_with_owner
    service2 = factories.ServiceFactory()
    role = factories.RoleFactory()

    department = factories.DepartmentFactory()
    head = staff_factory(department=department)
    factories.DepartmentStaffFactory(department=department, staff=head, role=DEPARTMENT_ROLES.CHIEF)
    staff1 = factories.StaffFactory(department=department, chief=head)
    staff2 = factories.StaffFactory(department=department, chief=head)
    staff_3 = factories.StaffFactory(department=factories.DepartmentFactory(parent=department), chief=head)
    staff_4 = factories.StaffFactory(department=factories.DepartmentFactory(parent=department), chief=staff2)

    factories.ServiceMemberFactory(service=service, staff=staff1, role=role, part_rate=0.3)
    factories.ServiceMemberFactory(service=service, staff=staff2, role=role, part_rate=0.5)
    factories.ServiceMemberFactory(service=service2, staff=staff2, role=role, part_rate=0.8)
    factories.ServiceMemberFactory(service=service2, staff=staff_3, role=role, part_rate=0.1)
    factories.ServiceMemberFactory(service=service2, staff=staff_4, role=role, part_rate=0.9)

    client.login(head.login)
    url = reverse('hr:person_participation_subordinates', args=[head.login])

    data = {}
    if direct is not None:
        data['direct'] = direct

    response = client.json.get(url, data)

    assert response.status_code == 200

    json = response.json()['content']
    expected_data = [
        {
            'is_direct': True,
            'person': person_serialize(staff1),
            'participation_data': [{
                'permissions':  {'can_edit_service_participation_rates': False},
                'rate': 0.3,
                'role': role_serialize(role),
                'service': service_serialize(service),
            }]
        },
        {
            'is_direct': True,
            'person': person_serialize(staff2),
            'participation_data': [{
                'permissions': {'can_edit_service_participation_rates': False},
                'rate': 0.5,
                'role': role_serialize(role),
                'service': service_serialize(service),
            }, {
                'permissions': {'can_edit_service_participation_rates': False},
                'rate': 0.8,
                'role': role_serialize(role),
                'service': service_serialize(service2),
            }]
        },
        {
            'is_direct': True,
            'person': person_serialize(staff_3),
            'participation_data': [{
                'permissions': {'can_edit_service_participation_rates': False},
                'rate': 0.1,
                'role': role_serialize(role),
                'service': service_serialize(service2),
            }]
        }
    ]

    if not direct:
        expected_data.append(
            {
                'is_direct': False,
                'person': person_serialize(staff_4),
                'participation_data': [{
                    'permissions': {'can_edit_service_participation_rates': False},
                    'rate': 0.9,
                    'role': role_serialize(role),
                    'service': service_serialize(service2),
                }]
            }
        )

    person = person_serialize_with_subrodinates(head)
    print(person)
    person['is_chief'] = True
    expected = {
        'person': person,
        'permissions': {
            'can_edit_participation_rates': False
        },
        'subordinates_participation_data': expected_data
    }
    assert json == expected


def test_person_participation_subordinates_history(client, service_with_owner):
    client.login(service_with_owner.owner.login)
    url = reverse('hr:person_participation_subordinates_history', args=[service_with_owner.owner.login])

    response = client.json.get(url)
    assert response.status_code == 200

    json = response.json()['content']
    assert json['participation_history'] == []


def test_person_participation_history(client, service_with_owner):
    service_member = factories.ServiceMemberFactory(
        service=service_with_owner,
        staff=service_with_owner.owner,
    )
    rate = factories.PartRateHistoryFactory(
        service_member=service_member,
        modified_at=now(),
    )

    client.login(service_with_owner.owner.login)
    url = reverse('hr:person_participation_history', args=[service_with_owner.owner.login])

    response = client.json.get(url)
    assert response.status_code == 200

    json = response.json()['content']
    assert json['person']['login'] == service_with_owner.owner.login
    assert json['participation_history'][0]['person_changed']['login'] == rate.staff.login
    assert json['participation_history'][0]['person']['login'] == service_member.staff.login
