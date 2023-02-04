import datetime
from json import loads

import pytest
from intranet.wiki.tests.wiki_tests.common.assert_helpers import assert_json, in_any_order
from wiki.api_v2.exceptions import AlreadyExists
from wiki.api_v2.public.me.exceptions import UserFeatureIsProcessing
from wiki.api_v2.public.me.favorites.exceptions import BookmarkNotFound, TagNotFound
from wiki.favorites.logic import create_bookmark, create_tags
from wiki.favorites.models import Bookmark, Tag
from wiki.pages.utils.remove import delete_page
from wiki.sync.connect.base_organization import as_base_organization
from wiki.users.consts import UserFeatureCode
from wiki.users.logic import features as user_features

pytestmark = [pytest.mark.django_db]


def user_org(user):
    org = None
    if hasattr(user, 'orgs'):
        org = user.orgs.all()[0]
    return org


def create_bookmarks(user, pages):
    organization = as_base_organization(user_org(user))

    bookmarks = {}
    for page in pages:
        bookmarks[page.slug] = create_bookmark(user, organization, page, tags=['tag1', 'tag2'])
    return bookmarks


def _to_page_order(response):
    assert response.status_code == 200, response.json()
    return [q['page']['slug'] for q in response.json()['results']]


def test_get_bookmarks_order_by(client, wiki_users, page_cluster, test_org, test_org_id):
    user = wiki_users.thasonic

    page_cluster['root/a'].title = 'А'
    page_cluster['root'].title = 'Б'
    page_cluster['root/a/aa'].title = 'В'
    page_cluster['root/b'].title = 'Г'

    page_cluster['root/a/aa'].owner = wiki_users.volozh  # Волож
    page_cluster['root/b'].owner = wiki_users.kolomeetz  # Коломеец
    page_cluster['root'].owner = wiki_users.asm  # Мазуров
    page_cluster['root/a'].owner = wiki_users.chapson  # Чапоргин

    page_cluster['root/a'].save()
    page_cluster['root'].save()
    page_cluster['root/a/aa'].save()
    page_cluster['root/b'].save()

    def _mk_bookmark(slug, day):
        b = create_bookmark(user, as_base_organization(test_org), page_cluster[slug], tags=[])
        b.created_at = datetime.datetime(year=2021, month=10, day=day)
        b.save()

    _mk_bookmark('root', 5)
    _mk_bookmark('root/a', 3)
    _mk_bookmark('root/b', 2)
    _mk_bookmark('root/a/aa', 1)

    client.login(user)
    response = client.get('/api/v2/public/me/favorites?order_by=created_at&order_direction=asc')
    assert _to_page_order(response) == ['root/a/aa', 'root/b', 'root/a', 'root']

    response = client.get('/api/v2/public/me/favorites?order_by=created_at&order_direction=desc')
    assert _to_page_order(response) == ['root', 'root/a', 'root/b', 'root/a/aa']

    response = client.get('/api/v2/public/me/favorites?order_by=page.title&order_direction=asc')
    assert _to_page_order(response) == ['root/a', 'root', 'root/a/aa', 'root/b']

    response = client.get('/api/v2/public/me/favorites?order_by=page.author&order_direction=asc')
    assert _to_page_order(response) == ['root/a/aa', 'root/b', 'root', 'root/a']


def test_get_bookmarks_order_by__cursor__asc(client, wiki_users, page_cluster, test_org, test_org_id):
    user = wiki_users.thasonic

    page_cluster['root/a/aa'].title = 'А'
    page_cluster['root'].title = 'Б'
    page_cluster['root/b'].title = 'В'
    page_cluster['root/a/ad'].title = 'Г'
    page_cluster['root/c'].title = 'Г'
    page_cluster['root/b/bd'].title = 'Д'

    page_cluster['root/a/aa'].save()
    page_cluster['root'].save()
    page_cluster['root/b'].save()
    page_cluster['root/a/ad'].save()
    page_cluster['root/c'].save()
    page_cluster['root/b/bd'].save()

    def _mk_bookmark(slug, day):
        b = create_bookmark(user, as_base_organization(test_org), page_cluster[slug], tags=[])
        b.created_at = datetime.datetime(year=2021, month=10, day=day)
        b.save()

    # порядок создания ~ id, и ~ slug
    _mk_bookmark('root', 8)
    _mk_bookmark('root/a/aa', 6)
    _mk_bookmark('root/a/ad', 4)
    _mk_bookmark('root/b', 7)
    _mk_bookmark('root/b/bd', 1)
    _mk_bookmark('root/c', 6)

    client.login(user)

    # получение первых двух
    response = client.get('/api/v2/public/me/favorites?order_by=page.title&page_size=2')
    res = response.json()
    prev_cursor, next_cursor = res['prev_cursor'], res['next_cursor']
    assert [q['page']['id'] for q in res['results']] == [page_cluster['root/a/aa'].id, page_cluster['root'].id]
    assert prev_cursor is None and next_cursor

    # получение следующие 2
    response = client.get(f'/api/v2/public/me/favorites?order_by=page.title&page_size=2&cursor={next_cursor}')
    res = response.json()
    prev_cursor, next_cursor = res['prev_cursor'], res['next_cursor']
    assert [q['page']['id'] for q in res['results']] == [page_cluster['root/b'].id, page_cluster['root/a/ad'].id]
    assert prev_cursor and next_cursor

    # вернемся к предыдущим
    response = client.get(f'/api/v2/public/me/favorites?order_by=page.title&page_size=2&&cursor={prev_cursor}')
    res = response.json()
    prev_cursor, next_cursor = res['prev_cursor'], res['next_cursor']
    assert [q['page']['id'] for q in res['results']] == [page_cluster['root/a/aa'].id, page_cluster['root'].id]
    assert prev_cursor is None and next_cursor

    # опять получим следующие 2
    response = client.get(f'/api/v2/public/me/favorites?order_by=page.title&page_size=2&cursor={next_cursor}')
    res = response.json()
    prev_cursor, next_cursor = res['prev_cursor'], res['next_cursor']
    assert [q['page']['id'] for q in res['results']] == [page_cluster['root/b'].id, page_cluster['root/a/ad'].id]
    assert prev_cursor and next_cursor

    # дойдем до конца
    response = client.get(f'/api/v2/public/me/favorites?order_by=page.title&page_size=5&cursor={next_cursor}')
    res = response.json()
    prev_cursor, next_cursor = res['prev_cursor'], res['next_cursor']
    assert [q['page']['id'] for q in res['results']] == [page_cluster['root/c'].id, page_cluster['root/b/bd'].id]
    assert prev_cursor and next_cursor is None


def test_get_bookmarks_order_by__cursor__desc(client, wiki_users, page_cluster, test_org, test_org_id):
    user = wiki_users.thasonic

    def _mk_bookmark(slug, day):
        b = create_bookmark(user, as_base_organization(test_org), page_cluster[slug], tags=[])
        b.created_at = datetime.datetime(year=2021, month=10, day=day)
        b.save()

    # порядок создания ~ id
    _mk_bookmark('root', 14)
    _mk_bookmark('root/a/aa', 12)
    _mk_bookmark('root/a/ad', 12)
    _mk_bookmark('root/b', 10)

    client.login(user)

    # получение первых двух
    response = client.get('/api/v2/public/me/favorites?order_direction=desc&page_size=2')
    res = response.json()
    prev_cursor, next_cursor = res['prev_cursor'], res['next_cursor']
    assert [q['page']['id'] for q in res['results']] == [page_cluster['root'].id, page_cluster['root/a/ad'].id]
    assert prev_cursor is None and next_cursor

    # получение последних двух
    response = client.get(f'/api/v2/public/me/favorites?order_direction=desc&page_size=2&cursor={next_cursor}')
    res = response.json()
    prev_cursor, next_cursor = res['prev_cursor'], res['next_cursor']
    assert [q['page']['id'] for q in res['results']] == [page_cluster['root/a/aa'].id, page_cluster['root/b'].id]
    assert prev_cursor and next_cursor is None


def test_get_bookmarks_by_tag(client, wiki_users, page_cluster, test_org, test_org_id):
    tag_foo = 'fo o'
    tag_bar = 'bar'
    tag_baz = 'baz'
    tag_abcd = 'abcd'

    bookmarks = {}

    bookmarks_template = {
        page_cluster['root']: [tag_foo, tag_bar, tag_baz],
        page_cluster['root/a']: [tag_foo],
        page_cluster['root/a/aa']: [tag_bar],
    }

    for page, tags in bookmarks_template.items():
        bookmarks[page.slug] = create_bookmark(wiki_users.thasonic, as_base_organization(test_org), page, tags=tags)

    client.login(wiki_users.thasonic)

    response = client.get(f'/api/v2/public/me/favorites?tags[]={tag_foo}')
    assert response.status_code == 200

    response_bookmarks = response.json()['results']

    assert_json(
        response_bookmarks,
        in_any_order(
            [
                {
                    'page': {'slug': page_cluster['root'].slug},
                },
                {
                    'page': {'slug': page_cluster['root/a'].slug},
                },
            ]
        ),
    )

    response = client.get(f'/api/v2/public/me/favorites?tags[]={tag_foo}&tags[]={tag_bar}&tags[]={tag_abcd}')
    assert response.status_code == 200

    response_bookmarks = response.json()['results']

    assert_json(
        response_bookmarks,
        in_any_order(
            [
                {
                    'page': {'slug': page_cluster['root'].slug},
                },
                {
                    'page': {'slug': page_cluster['root/a'].slug},
                },
                {
                    'page': {'slug': page_cluster['root/a/aa'].slug},
                },
            ]
        ),
    )

    response = client.get(f'/api/v2/public/me/favorites?tags[]={tag_abcd}')
    assert response.status_code == 200

    response_bookmarks = response.json()['results']
    assert response_bookmarks == []


def test_get_bookmarks__filter_by_q(client, wiki_users, page_cluster):
    client.login(wiki_users.asm)
    page_cluster['root/a'].title = 'foo bar'
    page_cluster['root/b'].title = 'foo baz'
    page_cluster['root/a'].save()
    page_cluster['root/b'].save()

    create_bookmarks(wiki_users.thasonic, [page_cluster['root/a'], page_cluster['root/b']])
    client.login(wiki_users.thasonic)

    # only my
    response = client.get('/api/v2/public/me/favorites?q=foo&order_by=page.slug')

    assert_json(
        response.json(),
        {
            'results': [
                {'page': {'slug': 'root/a'}},
                {'page': {'slug': 'root/b'}},
            ]
        },
    )

    response = client.get('/api/v2/public/me/favorites?q=Baz&order_by=page.slug')

    assert_json(
        response.json(),
        {
            'results': [
                {'page': {'slug': 'root/b'}},
            ]
        },
    )


def test_get_bookmarks(client, wiki_users, page_cluster):
    create_bookmarks(wiki_users.thasonic, [page_cluster['root/a'], page_cluster['root/b']])
    client.login(wiki_users.thasonic)

    response = client.get('/api/v2/public/me/favorites')
    assert response.status_code == 200

    response_bookmarks = response.json()['results']

    assert_json(
        response_bookmarks,
        [
            {
                'page': {
                    'title': page_cluster['root/b'].title,
                    'author': {'username': 'thasonic'},
                    'is_active': True,
                    'slug': page_cluster['root/b'].slug,
                },
                'tags': [
                    {'name': 'tag1'},
                    {'name': 'tag2'},
                ],
            },
            {
                'page': {
                    'title': page_cluster['root/a'].title,
                    'author': {'username': 'thasonic'},
                    'is_active': True,
                    'slug': page_cluster['root/a'].slug,
                },
                'tags': [
                    {'name': 'tag1'},
                    {'name': 'tag2'},
                ],
            },
        ],
    )


def test_favorites_migration_in_progress(client, wiki_users, page_cluster):
    user_features.enable_feature(wiki_users.thasonic, UserFeatureCode.DATA_UI_WEB)
    client.login(wiki_users.thasonic)

    response = client.get('/api/v2/public/me/favorites')
    assert response.status_code == 400
    assert response.json()['error_code'] == UserFeatureIsProcessing.error_code


def test_get_bookmarks_with_deleted_pages(client, wiki_users, page_cluster):
    create_bookmarks(wiki_users.thasonic, [page_cluster['root/a'], page_cluster['root/b']])
    delete_page(page_cluster['root/b'])

    client.login(wiki_users.thasonic)
    response = client.get('/api/v2/public/me/favorites')
    assert response.status_code == 200
    response_data = loads(response.content.decode())
    response_bookmarks = response_data['results']

    assert len(response_bookmarks) == 2
    assert response_bookmarks[0]['page']['is_active'] is False
    assert response_bookmarks[1]['page']['is_active'] is True


def test_create_bookmark(client, wiki_users, page_cluster):
    client.login(wiki_users.thasonic)
    data = {'page': {'slug': 'root/a'}, 'tags': ['tag1', 'tag2']}
    response = client.post('/api/v2/public/me/favorites', data=data)
    assert response.status_code == 200

    response_data = loads(response.content.decode())
    assert response_data['page']['author']['username'] == 'thasonic'
    assert response_data['page']['is_active'] is True
    assert response_data['page']['slug'] == page_cluster['root/a'].slug
    assert [tag['name'] for tag in response_data['tags']] == ['tag1', 'tag2']
    assert response_data['page']['title'] == page_cluster['root/a'].title

    response = client.post('/api/v2/public/me/favorites', data=data)
    assert response.status_code == 400
    response_data = loads(response.content.decode())
    assert response_data['error_code'] == AlreadyExists.error_code


def test_update_nonexistent_bookmark(client, wiki_users, page_cluster):
    client.login(wiki_users.thasonic)
    response = client.post('/api/v2/public/me/favorites/999', data={'tags': []})
    assert response.status_code == 404
    response_data = loads(response.content.decode())
    assert response_data['error_code'] == BookmarkNotFound.error_code

    bookmarks = create_bookmarks(wiki_users.chapson, [page_cluster['root/a'], page_cluster['root/b']])
    bookmark_id = bookmarks['root/a'].id

    # Существующая закладка, но для другого пользователя
    response = client.post(f'/api/v2/public/me/favorites/{bookmark_id}', data={'tags': []})
    assert response.status_code == 404
    response_data = loads(response.content.decode())
    assert response_data['error_code'] == BookmarkNotFound.error_code


def test_update_bookmark(client, wiki_users, page_cluster):
    bookmarks = create_bookmarks(wiki_users.thasonic, [page_cluster['root/a'], page_cluster['root/b']])
    bookmark_id = bookmarks['root/a'].id
    client.login(wiki_users.thasonic)
    response = client.post(f'/api/v2/public/me/favorites/{bookmark_id}', data={'tags': ['newtag1', 'newtag2']})
    assert response.status_code == 200
    response_data = loads(response.content.decode())

    response_tags = sorted([tag['name'] for tag in response_data['tags']])
    assert response_tags == ['newtag1', 'newtag2']

    response = client.post(f'/api/v2/public/me/favorites/{bookmark_id}', data={'tags': ['newtag2', 'newtag3']})
    assert response.status_code == 200
    response_data = loads(response.content.decode())

    response_tags = sorted([tag['name'] for tag in response_data['tags']])
    assert response_tags == ['newtag2', 'newtag3']

    response = client.post(f'/api/v2/public/me/favorites/{bookmark_id}', data={'tags': []})
    assert response.status_code == 200
    response_data = loads(response.content.decode())
    assert response_data['tags'] == []


def test_delete_bookmark(client, wiki_users, page_cluster):
    bookmarks = create_bookmarks(wiki_users.thasonic, [page_cluster['root/a'], page_cluster['root/b']])
    bookmark_id = bookmarks['root/a'].id

    client.login(wiki_users.thasonic)
    response = client.delete(f'/api/v2/public/me/favorites/{bookmark_id}')
    assert response.status_code == 204

    with pytest.raises(Bookmark.DoesNotExist):
        Bookmark.objects.get(id=bookmark_id)


def test_get_tags(client, wiki_users, page_cluster):
    create_bookmarks(wiki_users.thasonic, [page_cluster['root/a']])
    client.login(wiki_users.thasonic)

    # first_page
    response = client.get('/api/v2/public/me/favorites/tags', {'page_size': 1})
    assert response.status_code == 200

    res = response.json()
    assert [tag['name'] for tag in res['results']] == ['tag1']
    assert res['prev_cursor'] is None and res['next_cursor']

    # second page
    response = client.get('/api/v2/public/me/favorites/tags', {'cursor': res['next_cursor']})
    assert response.status_code == 200

    res = response.json()
    assert [tag['name'] for tag in res['results']] == ['tag2']
    assert res['prev_cursor'] and res['next_cursor'] is None


def test_get_tags_by_q(client, wiki_users):
    client.login(wiki_users.thasonic)
    tags = ['tag1', 'tag-2', 'tag_3']
    response = client.post('/api/v2/public/me/favorites/tags', data={'tags': tags})
    assert response.status_code == 200

    response = client.get('/api/v2/public/me/favorites/tags', {'q': 'tag-'})
    assert response.status_code == 200
    response_data = response.json()
    assert len(response_data['results']) == 1
    assert [tag['name'] for tag in response_data['results']] == ['tag-2']

    response = client.get('/api/v2/public/me/favorites/tags', {'q': 'tag'})
    assert response.status_code == 200
    response_data = response.json()
    assert len(response_data['results']) == 3
    assert sorted([tag['name'] for tag in response_data['results']]) == sorted(tags)


def test_create_tags(client, wiki_users, page_cluster):
    client.login(wiki_users.thasonic)
    tags = ['tag1', 'tag-2', 'tag_3']
    response = client.post('/api/v2/public/me/favorites/tags', data={'tags': tags})
    assert response.status_code == 200
    response_data = loads(response.content.decode())['results']
    assert [tag['name'] for tag in response_data] == sorted(tags)

    client.login(wiki_users.chapson)
    tags = ['tag1', 'tag2', 'tag3']
    response = client.post('/api/v2/public/me/favorites/tags', data={'tags': tags})
    assert response.status_code == 200
    chapson_tags = loads(response.content.decode())['results']
    assert [tag['name'] for tag in chapson_tags] == sorted(tags)

    response = client.get('/api/v2/public/me/favorites/tags')
    assert response.status_code == 200
    response_data = loads(response.content.decode())
    assert response_data['results'] == chapson_tags


def test_delete_used_tags(client, wiki_users, page_cluster):
    org = user_org(wiki_users.thasonic)
    create_bookmarks(wiki_users.thasonic, [page_cluster['root/a']])

    client.login(wiki_users.thasonic)
    response = client.delete('/api/v2/public/me/favorites/tags', data={'tags': ['tag1', 'tag2']})
    assert response.status_code == 204

    with pytest.raises(Tag.DoesNotExist):
        Tag.objects.get(name='tag1', user=wiki_users.thasonic, org=org)

    with pytest.raises(Tag.DoesNotExist):
        Tag.objects.get(name='tag2', user=wiki_users.thasonic, org=org)


def test_delete_nonexistent_tags(client, wiki_users, page_cluster):
    client.login(wiki_users.thasonic)
    response = client.delete('/api/v2/public/me/favorites/tags', data={'tags': ['tag1', 'tag2']})
    assert response.status_code == 404
    response_data = loads(response.content.decode())
    assert response_data['error_code'] == TagNotFound.error_code


def test_delete_tags(client, wiki_users, page_cluster):
    org = user_org(wiki_users.thasonic)
    create_tags(wiki_users.thasonic, as_base_organization(org), ['tag1', 'tag2'])
    Tag.objects.get(name='tag1', user=wiki_users.thasonic, org=org)
    Tag.objects.get(name='tag2', user=wiki_users.thasonic, org=org)

    client.login(wiki_users.thasonic)
    response = client.delete('/api/v2/public/me/favorites/tags', data={'tags': ['tag1', 'tag2']})
    assert response.status_code == 204

    with pytest.raises(Tag.DoesNotExist):
        Tag.objects.get(name='tag1', user=wiki_users.thasonic, org=org)

    with pytest.raises(Tag.DoesNotExist):
        Tag.objects.get(name='tag2', user=wiki_users.thasonic, org=org)
