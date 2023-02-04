import pytest
import json

from django.core.urlresolvers import reverse


@pytest.mark.django_db
def test_smoke_test_view(client, company):
    response = client.get(
        reverse('profile:bulk-profiles'),
        data={'logins': 'dep111-person,out11-person,general,yandex-person,yandex-chief', 'fields': 'all'},
        content_type='application/json',
    )

    assert response.status_code == 200, response.content

    result = json.loads(response.content)
    assert result['total'] == 5
    result = result['result']

    assert set(result[0].keys()) == {
        'id',
        'login',
        'location_table',
        'location_office',
        'location_room',
        'personal',
        'birthday',
        'official_organization',
        'work_mode',
        'chief',
        'hr_partners',
        'value_streams',
    }
    persons = {person['login']: person for person in result}

    assert persons['dep111-person']['chief']['login'] == 'dep111-chief'
    assert persons['dep111-person']['hr_partners'][0]['login'] == 'dep1-hr-partner'
    assert persons['general']['chief']['login'] == 'yandex-chief'
    assert persons['yandex-person']['chief']['login'] == 'yandex-chief'
