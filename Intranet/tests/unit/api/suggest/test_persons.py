import pretend
import pytest

from django.urls import reverse

from plan.staff.models import Staff

from common import factories


@pytest.fixture
def persons_suggest_data():
    staff_1 = factories.StaffFactory(first_name='а')
    staff_2 = factories.StaffFactory(last_name='б')
    staff_3 = factories.StaffFactory(login='в')

    role = factories.RoleFactory()
    service = factories.ServiceFactory(owner=staff_1)
    schedule = factories.ScheduleFactory(service=service, role=role)
    factories.ServiceMemberFactory(service=service, staff=staff_1, role=role)

    return pretend.stub(
        staff_1=staff_1, staff_2=staff_2, staff_3=staff_3, schedule=schedule
    )


def check_suggest(client, params, staffs):
    response = client.json.get(reverse('api-v3-common:suggests:persons'), params)
    assert response.status_code == 200
    assert [x['id'] for x in response.json()['results']] == [x.staff_id for x in staffs]


def test_suggest_first_name(client, persons_suggest_data):
    check_suggest(client, {'search': 'а'}, [persons_suggest_data.staff_1])


def test_suggest_last_name(client, persons_suggest_data):
    check_suggest(client, {'search': 'б'}, [persons_suggest_data.staff_2])


def test_suggest_login(client, persons_suggest_data):
    check_suggest(client, {'search': 'в'}, [persons_suggest_data.staff_3])


def test_suggest_staff_id(client, persons_suggest_data):
    check_suggest(client, {'id': persons_suggest_data.staff_3.staff_id}, [persons_suggest_data.staff_3])


def test_suggest_exclude_staff_id(client, persons_suggest_data):
    response = client.json.get(reverse('api-v3-common:suggests:persons'), {'exclude_id': persons_suggest_data.staff_3.staff_id},)
    assert response.status_code == 200
    staffs = Staff.objects.exclude(pk=persons_suggest_data.staff_3.pk)
    assert set(x['id'] for x in response.json()['results']) == set(x.staff_id for x in staffs)


def test_check_schedule(client, persons_suggest_data):
    check_suggest(client, {'schedule': persons_suggest_data.schedule.id}, [persons_suggest_data.staff_1])


def test_schedule_with_no_role(client, persons_suggest_data):
    schedule = persons_suggest_data.schedule
    schedule.role = None
    schedule.save()
    check_suggest(client, {'schedule': schedule.pk}, [persons_suggest_data.staff_1])
