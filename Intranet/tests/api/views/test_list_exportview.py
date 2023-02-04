import pytest

from django.core.urlresolvers import reverse


@pytest.mark.parametrize('view_name', (
    'controlplan',
    'controltest',
    'ipe',
    'deficiency',
    'deficiencygroup',
))
def test_list_export_success(view_name, db, client, control_plan,
                             control_test, ipe, deficiency, deficiency_group):
    obj_map = {
        'controlplan': control_plan.id,
        'controltest': control_test.id,
        'ipe': ipe.id,
        'deficiency': deficiency.id,
        'deficiencygroup': deficiency_group.id,
    }
    url = reverse('api_v1:list_export', kwargs={'obj_class': view_name})
    response = client.get(url)
    response_json = response.json()
    assert response.status_code == 200
    assert len(response_json['results']) == 1
    assert response_json['results'][0]['id'] == obj_map[view_name]


def test_list_export_fail(db, client, control_plan):
    url = reverse('api_v1:list_export', kwargs={'obj_class': 'somevalue'})
    response = client.get(url)
    response_json = response.json()
    assert response.status_code == 409
    assert 'Request with not known entity was made' in response_json['message']
