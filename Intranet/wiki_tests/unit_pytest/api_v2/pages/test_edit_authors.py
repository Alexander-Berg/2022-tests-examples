import pytest

from wiki.api_frontend.serializers.user_identity import UserIdentity
from wiki.api_v2.public.pages.page.preserve_order import store_order, restore_order


def _simplify(data):
    return data['authors']['owner']['username'], [q['username'] for q in data['authors']['all']]


def _idx(qq):
    return [UserIdentity.from_user(q).dict() for q in qq]


@pytest.mark.django_db
def test_page_edit__authors__403(client, wiki_users, page_cluster, organizations, groups):
    page = page_cluster['root/a/ad']
    client.login(wiki_users.kolomeetz)

    response = client.post(
        f'/api/v2/public/pages/{page.id}?fields=authors',
        {'authors': {'owner': UserIdentity.from_user(wiki_users.asm).dict()}},
    )
    assert response.status_code == 403


@pytest.mark.django_db
def test_page_edit__authors(client, wiki_users, page_cluster, organizations, groups):
    page = page_cluster['root/a/ad']
    client.login(wiki_users.thasonic)

    response = client.post(
        f'/api/v2/public/pages/{page.id}?fields=authors',
        {'authors': {'owner': UserIdentity.from_user(wiki_users.asm).dict()}},
    )

    data = response.json()
    owner, authors = _simplify(data)

    assert response.status_code == 200
    assert owner == 'asm'
    assert authors == ['thasonic', 'asm']

    response = client.post(
        f'/api/v2/public/pages/{page.id}?fields=authors',
        {'authors': {'owner': UserIdentity.from_user(wiki_users.volozh).dict()}},
    )

    data = response.json()
    owner, authors = _simplify(data)

    assert response.status_code == 200
    assert owner == 'volozh'
    assert set(authors) == {'thasonic', 'asm', 'volozh'}

    response = client.post(
        f'/api/v2/public/pages/{page.id}?fields=authors',
        {'authors': {'all': _idx([wiki_users.robot_wiki, wiki_users.thasonic])}},
    )

    data = response.json()
    owner, authors = _simplify(data)

    assert response.status_code == 200
    assert owner == 'volozh'
    assert authors == ['robot-wiki', 'thasonic', 'volozh']


@pytest.mark.django_db
def test_page_edit_authors__must_preserve_order(client, wiki_users, page_cluster, organizations, groups):
    page = page_cluster['root/a/ad']
    client.login(wiki_users.thasonic)

    page.authors.set([wiki_users.asm, wiki_users.thasonic, wiki_users.volozh])

    response = client.post(
        f'/api/v2/public/pages/{page.id}?fields=authors',
        {'authors': {'all': _idx([wiki_users.thasonic, wiki_users.volozh, wiki_users.asm, wiki_users.robot_wiki])}},
    )

    data = response.json()
    owner, authors = _simplify(data)

    assert response.status_code == 200
    assert authors == ['thasonic', 'volozh', 'asm', 'robot-wiki']


@pytest.mark.django_db
def test_preserve_order(wiki_users, page_cluster):
    order = [wiki_users.asm, wiki_users.thasonic, wiki_users.volozh]
    output = [wiki_users.robot_wiki, wiki_users.thasonic, wiki_users.volozh, wiki_users.asm]

    temp_model = page_cluster['root']

    store_order(temp_model, 'foo', order)
    output_sorted = restore_order(temp_model, 'foo', output)

    assert output_sorted == [wiki_users.asm, wiki_users.thasonic, wiki_users.volozh, wiki_users.robot_wiki]

    temp_model = page_cluster['root/a']
    output_sorted = restore_order(temp_model, 'foo', output)
    assert output_sorted == output


@pytest.mark.django_db
def test_page_edit__authors__empty(client, wiki_users, page_cluster, organizations, groups):
    page = page_cluster['root/a/ad']
    client.login(wiki_users.thasonic)

    response = client.post(f'/api/v2/public/pages/{page.id}?fields=authors', {'authors': {'authors': []}})

    assert response.status_code == 200

    response = client.post(f'/api/v2/public/pages/{page.id}?fields=authors', {'authors': {'owner': None}})

    assert response.status_code == 200
