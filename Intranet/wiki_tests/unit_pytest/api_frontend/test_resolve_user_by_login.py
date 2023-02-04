import pytest

from intranet.wiki.tests.wiki_tests.common.factories.user import UserFactory
from intranet.wiki.tests.wiki_tests.common.factories.staff import StaffFactory
from intranet.wiki.tests.wiki_tests.common.skips import only_biz


@pytest.fixture
def create_user(organizations):
    _org = organizations and organizations.org_42

    def _create_user(login: str, email='', org=_org):
        user = UserFactory(username=login, email=email)
        StaffFactory(login=login, user=user)
        user.orgs.add(org) if org else None
        return user

    return _create_user


@pytest.mark.django_db
def test_without_client_login(client, wiki_users):
    client.logout()
    data = {'logins': [wiki_users.asm.get_username()]}
    response = client.post('/_api/frontend/.resolve_users_logins', data=data)
    assert response.status_code == 403


@pytest.mark.django_db
def test_empty_request_invalid(client, wiki_users):
    client.login(wiki_users.volozh)
    data = {}
    response = client.post('/_api/frontend/.resolve_users_logins', data=data)
    assert response.status_code == 409


@pytest.mark.django_db
def test_login_valid(client, wiki_users):
    client.login(wiki_users.volozh)
    data = {'logins': [wiki_users.asm.get_username()]}
    response = client.post('/_api/frontend/.resolve_users_logins', data=data)
    assert response.status_code == 200
    assert len(response.data['data']['users']) == 1


@pytest.mark.django_db
def test_email_valid(client, wiki_users):
    client.login(wiki_users.volozh)
    data = {'logins': [wiki_users.asm.email]}
    response = client.post('/_api/frontend/.resolve_users_logins', data=data)
    assert response.status_code == 200
    assert len(response.data['data']['users']) == 1


@pytest.mark.django_db
def test_email_as_login_valid(client, organizations, create_user):
    user = create_user(login='one@biz.com')
    client.login(user)

    data = {'logins': ['one']}
    response = client.post('/_api/frontend/.resolve_users_logins', data=data)

    assert response.status_code == 200
    assert len(response.data['data']['users']) == 1

    data = {'logins': ['one@biz.com']}
    response = client.post('/_api/frontend/.resolve_users_logins', data=data)

    assert response.status_code == 200
    assert len(response.data['data']['users']) == 1


@pytest.mark.django_db
def test_email_with_colon_valid(client, wiki_users):
    client.login(wiki_users.volozh)
    data = {'logins': [wiki_users.asm.email.replace('@', ':')]}
    response = client.post('/_api/frontend/.resolve_users_logins', data=data)
    assert response.status_code == 200
    assert len(response.data['data']['users']) == 1


@pytest.mark.django_db
def test_login_and_email_valid(client, wiki_users):
    client.login(wiki_users.volozh)
    data = {
        'logins': [
            wiki_users.thasonic.get_username(),
            wiki_users.kolomeetz.email,
            wiki_users.asm.email.replace('@', ':'),
        ]
    }
    response = client.post('/_api/frontend/.resolve_users_logins', data=data)
    assert response.status_code == 200
    assert len(response.data['data']['users']) == 3


@pytest.mark.django_db
def test_login_and_emails_invalid(client, wiki_users):
    client.login(wiki_users.volozh)
    data = {'logins': ['some invalid login', 'login:data@domen.ru']}
    response = client.post('/_api/frontend/.resolve_users_logins', data=data)
    assert response.status_code == 200
    assert len(response.data['data']['users']) == 0


@only_biz
@pytest.mark.django_db
def test_organization_contain(client, wiki_users, organizations):
    client.login(wiki_users.volozh, organization=organizations.org_42)  # в org_42 записаны все пользователи wiki_users
    data = {'logins': [wiki_users.robot_wiki.email, wiki_users.asm.email, wiki_users.thasonic.email]}
    response = client.post('/_api/frontend/.resolve_users_logins', data=data)

    assert response.status_code == 200
    assert len(response.data['data']['users']) == 3


@only_biz
@pytest.mark.django_db
def test_organization_not_contain(client, wiki_users, organizations):
    client.login(wiki_users.volozh, organization=organizations.org_21)  # в org_21 записан только wiki_robot
    data = {'logins': [wiki_users.robot_wiki.email, wiki_users.asm.email, wiki_users.thasonic.email]}
    response = client.post('/_api/frontend/.resolve_users_logins', data=data)

    assert response.status_code == 200
    assert len(response.data['data']['users']) == 1


@pytest.mark.django_db
def test_email_as_login_different_domain(client, organizations, create_user):
    user = create_user(login='one@biz.com')
    create_user(login='one')
    create_user(login='one@yandex-team.ru')

    client.login(user)

    data = {'logins': ['one']}
    response = client.post('/_api/frontend/.resolve_users_logins', data=data)

    assert response.status_code == 200
    assert len(response.data['data']['users']) == 3


@only_biz
@pytest.mark.django_db
def test_user_with_equal_login(client, organizations, create_user):
    user = create_user(login='one', email='one@biz.com')
    create_user(login='one', email='one@yandex-team.ru')

    client.login(user)

    data = {'logins': ['one']}
    response = client.post('/_api/frontend/.resolve_users_logins', data=data)

    assert response.status_code == 200
    assert len(response.data['data']['users']) == 2


@pytest.mark.django_db
def test_login__api_v2(client, wiki_users):
    client.login(wiki_users.volozh)
    data = {'logins': [wiki_users.asm.get_username()]}

    response = client.post('/api/v2/public/users/resolve_logins', data)
    assert response.status_code == 200

    assert len(response.json()['users']) == 1
    assert response.json()['users'][0]['id'] == wiki_users.asm.id
