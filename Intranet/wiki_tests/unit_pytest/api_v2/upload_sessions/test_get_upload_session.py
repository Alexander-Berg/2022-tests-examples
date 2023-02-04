import pytest

from intranet.wiki.tests.wiki_tests.common.skips import only_biz

pytestmark = [pytest.mark.django_db]


def test_get_upload_session(client, wiki_users, upload_sessions):
    client.login(wiki_users.thasonic)

    test_id = '9b18deaa-b969-4caa-a4f0-b13e455b610b'

    response = client.get(f'/api/v2/public/upload_sessions/{test_id}')

    response_data = response.json()

    assert response.status_code == 200
    assert response_data['session_id'] == test_id


def test_get_no_access(client, wiki_users, upload_sessions):
    client.login(wiki_users.chapson)

    test_id = '9b18deaa-b969-4caa-a4f0-b13e455b610b'

    response = client.get(f'/api/v2/public/upload_sessions/{test_id}')

    assert response.status_code == 404


@only_biz
def test_get_another_org(client, wiki_users, upload_sessions, organizations):
    client.login(wiki_users.thasonic)

    test_id = '9b18deaa-b969-4caa-a4f0-b13e455b610b'

    upload_session = upload_sessions[test_id]
    upload_session.org_id = organizations.org_21.id
    upload_session.save()

    response = client.get(f'/api/v2/public/upload_sessions/{test_id}')
    assert response.status_code == 404


def test_get_incorrect(client, wiki_users):
    client.login(wiki_users.thasonic)

    test_id = '9b18deaa'

    response = client.get(f'/api/v2/public/upload_sessions/{test_id}')
    # после того как добавили uuid в фильтр путей, он просто не сматчится на плохой UUID4, поэтому 404 а не 400
    assert response.status_code == 404


def test_get_non_exists(client, wiki_users, upload_sessions):
    client.login(wiki_users.thasonic)

    test_id = '6d403959-ae14-4acf-bfd1-8fdb7520e0bc'

    response = client.get(f'/api/v2/public/upload_sessions/{test_id}')
    assert response.status_code == 404
