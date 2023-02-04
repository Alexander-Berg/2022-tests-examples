import pytest

from django.urls.base import reverse

from intranet.femida.tests import factories as f


pytestmark = pytest.mark.django_db


def test_list_presets(su_client):
    f.PresetFactory.create()
    url = reverse('api:presets:list')
    response = su_client.get(url)
    assert response.status_code == 200


def test_create_preset(su_client):
    url = reverse('api:presets:list')
    data = {
        'name': 'name',
    }
    response = su_client.post(url, data)
    assert response.status_code == 201


def test_preset_filter_form(su_client):
    url = reverse('api:presets:filter-form')
    response = su_client.get(url)
    assert response.status_code == 200


def test_detail_preset(su_client):
    preset = f.PresetFactory.create()
    url = reverse('api:presets:detail', kwargs={'pk': preset.pk})
    response = su_client.get(url)
    assert response.status_code == 200


def test_update_preset(su_client):
    preset = f.PresetFactory.create()
    url = reverse('api:presets:detail', kwargs={'pk': preset.pk})
    data = {
        'name': 'updated_name',
    }
    response = su_client.patch(url, data)
    assert response.status_code == 200


def test_preset_put_problem(su_client):
    preset = f.PresetFactory.create()
    problem = f.create_problem()
    url = reverse('api:presets:problem', kwargs={'pk': preset.pk, 'problem_id': problem.id})
    response = su_client.put(url)
    assert response.status_code == 200


def test_preset_delete_problem(su_client):
    preset = f.PresetFactory.create()
    problem = f.create_problem()
    url = reverse('api:presets:problem', kwargs={'pk': preset.pk, 'problem_id': problem.id})
    response = su_client.delete(url)
    assert response.status_code == 200


def test_preset_reorder(su_client):
    preset = f.create_preset_with_problems()
    url = reverse('api:presets:reorder', kwargs={'pk': preset.pk})
    data = {
        'problem_ids': [2, 1],
    }
    response = su_client.post(url, data)
    assert response.status_code == 200


def test_preset_add_problems_from_preset(su_client):
    preset = f.create_preset_with_problems()
    url = reverse('api:presets:add-problems-from-preset', kwargs={'pk': preset.id})
    data = {
        'preset': f.create_preset_with_problems().id,
    }
    response = su_client.post(url, data)
    assert response.status_code == 200
