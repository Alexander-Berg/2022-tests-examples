from django.urls import reverse
from freezegun import freeze_time

from common import factories


@freeze_time('2018-01-01')
def test_filter_by_service(client):
    service = factories.ServiceFactory()
    staff = factories.StaffFactory()
    factories.ServiceMemberFactory(staff=staff, service=service)
    other_staff = factories.StaffFactory()
    gap = factories.GapFactory(staff=staff, start='2018-01-01T10:00:00', end='2018-01-01T11:00:00')
    factories.GapFactory(staff=other_staff)
    response = client.json.get(
        reverse('api-v3:duty-gap-list'), {'service': service.id, 'date_from': '2017-01-01', 'date_to': '2222-12-12'}
    )
    assert response.status_code == 200
    results = response.json()['results']
    assert len(results) == 1
    result = results[0]
    assert result['type'] == gap.type
    assert result['full_day'] == gap.full_day
    assert result['person']['id'] == staff.staff_id
    assert result['work_in_absence'] == gap.work_in_absence
    assert result['start'] == '2018-01-01T07:00:00Z'
    assert result['end'] == '2018-01-01T08:00:00Z'
    assert result['person']['abc_id'] == staff.id
