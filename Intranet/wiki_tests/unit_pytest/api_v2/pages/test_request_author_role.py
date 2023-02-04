import pytest

from intranet.wiki.tests.wiki_tests.common.assert_helpers import assert_json
from wiki.api_frontend.serializers.user_identity import UserIdentity
from wiki.api_v2.public.pages.schemas import RequestAuthorRoleDetails, RequestAuthorRoleResult

from unittest import mock


@pytest.mark.django_db
def test_request_author_role__autogrant3(client, wiki_users, page_cluster):
    client.login(wiki_users.asm)

    page_cluster['root'].authors.add(wiki_users.asm)
    page = page_cluster['root/a']

    response = client.post(f'/api/v2/public/pages/{page.id}/request_author_role')

    assert response.status_code == 200

    assert_json(
        response.json(),
        {'result': RequestAuthorRoleResult.ROLE_GRANTED, 'details': RequestAuthorRoleDetails.VIA_INHERITANCE},
    )

    assert wiki_users.asm in page.authors.all()


@pytest.mark.django_db
def test_request_author_role__autogrant1(client, wiki_users, page_cluster):
    client.login(wiki_users.asm)
    page = page_cluster['root/a']

    with mock.patch('wiki.api_v2.public.pages.request_author_role_view.is_admin', return_value=True):
        response = client.post(f'/api/v2/public/pages/{page.id}/request_author_role')

        assert response.status_code == 200

    assert_json(
        response.json(),
        {'result': RequestAuthorRoleResult.ROLE_GRANTED, 'details': RequestAuthorRoleDetails.ADMIN},
    )

    assert wiki_users.asm in page.authors.all()


@pytest.mark.django_db
def test_request_author_role__autogrant2(client, wiki_users, page_cluster):
    client.login(wiki_users.asm)

    staff = wiki_users.thasonic.staff
    staff.is_dismissed = True
    staff.save()

    page = page_cluster['root/a']

    response = client.post(f'/api/v2/public/pages/{page.id}/request_author_role')

    assert response.status_code == 200

    assert_json(
        response.json(),
        {'result': RequestAuthorRoleResult.ROLE_GRANTED, 'details': RequestAuthorRoleDetails.EVERYONE_DISMISSED},
    )

    assert wiki_users.asm in page.authors.all()


@pytest.mark.django_db
def test_request_author_role__ok(client, wiki_users, page_cluster):
    client.login(wiki_users.asm)
    page = page_cluster['root/a']

    response = client.post(f'/api/v2/public/pages/{page.id}/request_author_role')

    assert response.status_code == 200

    assert_json(
        response.json(),
        {
            'result': RequestAuthorRoleResult.REQUEST_SENT,
        },
    )

    response = client.post(f'/api/v2/public/pages/{page.id}/request_author_role')

    assert response.status_code == 200

    assert_json(
        response.json(),
        {
            'result': RequestAuthorRoleResult.REQUEST_ALREADY_SENT,
        },
    )


@pytest.mark.django_db
def test_request_author_role__already_author(client, wiki_users, page_cluster):
    client.login(wiki_users.asm)
    page = page_cluster['root/a']

    page.authors.add(wiki_users.asm)

    response = client.post(f'/api/v2/public/pages/{page.id}/request_author_role')

    assert response.status_code == 200

    assert_json(
        response.json(),
        {
            'result': RequestAuthorRoleResult.ALREADY_AUTHOR,
        },
    )


@pytest.mark.django_db
def test_grant_author_role__access_denied(client, wiki_users, page_cluster):
    client.login(wiki_users.asm)
    page = page_cluster['root/a']

    response = client.post(
        f'/api/v2/public/pages/{page.id}/grant_author_role', {'user': UserIdentity.from_user(wiki_users.volozh).dict()}
    )

    assert response.status_code == 403


@pytest.mark.django_db
def test_grant_author_role(client, wiki_users, page_cluster):
    client.login(wiki_users.thasonic)
    page = page_cluster['root/a']

    response = client.post(
        f'/api/v2/public/pages/{page.id}/grant_author_role', {'user': UserIdentity.from_user(wiki_users.asm).dict()}
    )

    assert response.status_code == 200

    assert_json(
        response.json(),
        {'result': RequestAuthorRoleResult.ROLE_GRANTED},
    )

    assert wiki_users.asm in page.authors.all()
