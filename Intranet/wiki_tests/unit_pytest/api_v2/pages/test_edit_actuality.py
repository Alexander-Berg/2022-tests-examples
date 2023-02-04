import datetime
from pprint import pprint

import pytest
from freezegun import freeze_time

from wiki.pages.models.consts import EDITED_PAGE_ACTUALITY_TIMEOUT, MARKED_PAGE_ACTUALITY_TIMEOUT
from wiki.utils.timezone import now


@pytest.mark.django_db
def test_page_edit__actuality__obsolete(client, wiki_users, page_cluster, organizations, groups):
    page = page_cluster['root/a']
    client.login(wiki_users.thasonic)

    response = client.post(
        f'/api/v2/public/pages/{page.id}?fields=actuality',
        {
            'actuality': {
                'is_actual': False,
                'comment': 'Wow so obsolete',
                'external_links': ['https://ya.ru', 'https://ya.com'],
                'actual_pages': [{'slug': 'root/b'}],
            }
        },
    )

    data = response.json()['actuality']
    pprint(data)
    assert response.status_code == 200
    assert data['actual_pages'][0]['slug'] == 'root/b'
    assert data['comment'] == 'Wow so obsolete'
    assert set(data['external_links']) == {'https://ya.ru', 'https://ya.com'}
    assert data['user'][0]['username'] == 'thasonic'


@pytest.mark.django_db
def test_page_edit__actuality__obsolete_bare_minimum(client, wiki_users, page_cluster, organizations, groups):
    page = page_cluster['root/a']
    client.login(wiki_users.thasonic)

    response = client.post(
        f'/api/v2/public/pages/{page.id}?fields=actuality',
        {
            'actuality': {
                'is_actual': False,
            }
        },
    )

    data = response.json()['actuality']
    pprint(data)
    assert response.status_code == 200
    assert data['user'][0]['username'] == 'thasonic'


@pytest.mark.django_db
def test_page__actuality_degradation(client, wiki_users, page_cluster, organizations, groups):
    """
    Проверим логику деградации

    1. После EDITED_PAGE_ACTUALITY_TIMEOUT с момента последнего редактирования - possibly_obsolete
    2. После MARKED_PAGE_ACTUALITY_TIMEOUT с момента выставления флага is_actual=True и п.1 - possibly_obsolete
    3. is_actual=False - вечно
    """
    page = page_cluster['root/a']
    client.login(wiki_users.thasonic)

    dt = now()

    with freeze_time(dt):
        response = client.get(f'/api/v2/public/pages/{page.id}?fields=actuality')
        data = response.json()['actuality']
        assert response.status_code == 200
        assert data['status'] == 'actual'

    dt += EDITED_PAGE_ACTUALITY_TIMEOUT + datetime.timedelta(hours=1)

    with freeze_time(dt):
        response = client.get(f'/api/v2/public/pages/{page.id}?fields=actuality')
        data = response.json()['actuality']
        assert response.status_code == 200
        assert data['status'] == 'possibly_obsolete'

        response = client.post(
            f'/api/v2/public/pages/{page.id}?fields=actuality',
            {
                'actuality': {
                    'is_actual': True,
                    'comment': 'Wow so actual',
                }
            },
        )

        data = response.json()['actuality']
        assert response.status_code == 200
        assert data['comment'] == 'Wow so actual'
        assert data['status'] == 'actual'

    dt += max(MARKED_PAGE_ACTUALITY_TIMEOUT, EDITED_PAGE_ACTUALITY_TIMEOUT) + datetime.timedelta(hours=1)

    with freeze_time(dt):
        response = client.get(f'/api/v2/public/pages/{page.id}?fields=actuality')
        data = response.json()['actuality']
        assert response.status_code == 200
        assert data['status'] == 'possibly_obsolete'

        response = client.post(
            f'/api/v2/public/pages/{page.id}?fields=actuality',
            {
                'actuality': {
                    'is_actual': False,
                    'comment': 'Wow so actual',
                }
            },
        )
        assert response.status_code == 200

    dt += max(MARKED_PAGE_ACTUALITY_TIMEOUT, EDITED_PAGE_ACTUALITY_TIMEOUT) * 3

    with freeze_time(dt):
        response = client.get(f'/api/v2/public/pages/{page.id}?fields=actuality')
        data = response.json()['actuality']
        assert response.status_code == 200
        assert data['status'] == 'obsolete'


@pytest.mark.django_db
def test_page_edit__actuality__actual(client, wiki_users, page_cluster, organizations, groups):
    page = page_cluster['root/a']
    client.login(wiki_users.thasonic)

    response = client.post(
        f'/api/v2/public/pages/{page.id}?fields=actuality',
        {
            'actuality': {
                'is_actual': True,
                'comment': 'Wow so actual',
            }
        },
    )

    data = response.json()['actuality']
    assert response.status_code == 200
    assert data['comment'] == 'Wow so actual'
    assert data['status'] == 'actual'
