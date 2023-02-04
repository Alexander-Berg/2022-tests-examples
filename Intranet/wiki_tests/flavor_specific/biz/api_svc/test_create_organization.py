import mock
import pytest


@pytest.mark.django_db
@mock.patch('wiki.sync.cloud.views.create_organization.create_new_org_in_cloud')
@mock.patch('wiki.sync.cloud.views.create_organization.ensure_cloud_org_in_connect')
def test_create_organization(ensure_org, _, client, wiki_users, groups):
    client.login(wiki_users.thasonic)

    dir_id = 1234
    ensure_org.return_value = dir_id

    response = client.post('/_api/svc/cloud/.create_organization', data={'org_name': 'my_org', 'user_iam_token': 'any'})
    assert response.status_code == 200, response.json()
    assert response.json()['data'] == {'success': True, 'org_id': dir_id}

    response = client.post('/_api/svc/cloud/.create_organization', data={'user_iam_token': 'any'})
    assert response.status_code == 409

    response = client.post('/_api/svc/cloud/.create_organization', data={'org_name': 'blabla'})
    assert response.status_code == 409
