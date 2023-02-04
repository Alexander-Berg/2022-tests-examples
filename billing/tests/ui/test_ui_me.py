from mdh.core.models import ContributorRole


def test_me(drf_client, init_user):

    user = init_user(roles=[ContributorRole])
    client = drf_client(user=user)
    response = client.get('/uiapi/me/')
    assert response.status_code == 200

    data = response.json()
    assert data['username'] == 'tester'
    assert len(data['roles']) == 1
    assert len(data['roles_effective']) == 2
