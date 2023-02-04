import pytest
from django.conf import settings

from intranet.wiki.tests.wiki_tests.common.acl_helper import (
    set_access_author_only,
    make_user_staff,
    make_user_outstaff,
    drop_acl_cache,
)
from intranet.wiki.tests.wiki_tests.common.skips import only_intranet
from intranet.wiki.tests.wiki_tests.common.assert_helpers import assert_json
from intranet.wiki.tests.wiki_tests.unit_pytest.featured_pages.conftest import create_featured_page_group
from intranet.wiki.tests.wiki_tests.unit_pytest.featured_pages.helpers import _assert_msk, _assert_both, _assert_spb
from wiki.featured_pages.models import (
    VisibilityFilter,
    GeoFilter,
    Affilation,
)

if settings.IS_INTRANET:
    pass

pytestmark = [pytest.mark.django_db]


def test_get_featured_page_group(client, wiki_users, test_page, featured_pages_msk, featured_pages_spb):
    client.login(wiki_users.volozh)

    response = client.get('/api/v2/public/me/featured_pages')
    assert response.status_code == 200
    data = response.json()

    assert_json(
        data,
        {
            'groups': [
                {
                    'title': 'Москва',
                    'links': [
                        {'page': {'slug': 'root'}, 'type': 'page'},
                        {'external_url': 'https://ya.ru', 'type': 'link'},
                        {'external_url': 'https://auto.ru', 'type': 'link'},
                        {'page': {'slug': 'root/a'}, 'type': 'page'},
                    ],
                },
                {
                    'title': 'Питер',
                    'links': [
                        {'page': {'slug': 'root/b'}, 'type': 'page'},
                        {'page': {'slug': 'root/a/aa'}, 'type': 'page'},
                    ],
                },
            ]
        },
    )


def test_filter_pages_by_acl(client, wiki_users, page_cluster, featured_pages_msk, featured_pages_spb):
    client.login(wiki_users.volozh)

    set_access_author_only(page=page_cluster['root/b'])  # only thasonic

    response = client.get('/api/v2/public/me/featured_pages')
    assert response.status_code == 200
    data = response.json()

    assert_json(
        data,
        {
            'groups': [
                {
                    'title': 'Москва',
                    'links': [
                        {'page': {'slug': 'root'}, 'type': 'page'},
                        {'external_url': 'https://ya.ru', 'type': 'link'},
                        {'external_url': 'https://auto.ru', 'type': 'link'},
                        {'page': {'slug': 'root/a'}, 'type': 'page'},
                    ],
                },
                {
                    'title': 'Питер',
                    'links': [{'page': {'slug': 'root/a/aa'}, 'type': 'page'}],
                },
            ]
        },
    )


def test_pass_empty_featured_pages(client, wiki_users, test_org):
    client.login(wiki_users.volozh)

    empty = create_featured_page_group(org=test_org)
    assert empty.pages.count() == 0
    assert len(empty.links.get('links', [])) == 0

    response = client.get('/api/v2/public/me/featured_pages')
    assert len(response.json()['groups']) == 0


def test_pass_empty_featured_pages_after_check_acl(
    client, wiki_users, page_cluster, featured_pages_msk, featured_pages_spb
):
    client.login(wiki_users.volozh)
    set_access_author_only(page=page_cluster['root/b'])  # only thasonic
    set_access_author_only(page=page_cluster['root/a/aa'])  # only thasonic

    response = client.get('/api/v2/public/me/featured_pages')
    assert response.status_code == 200
    assert_json(response.json(), {'groups': [{'title': 'Москва'}]})


# visibility ------


def test_filter_by_visibility_option(client, wiki_users, featured_pages_msk, featured_pages_spb):
    client.login(wiki_users.volozh)

    response = client.get('/api/v2/public/me/featured_pages')
    assert response.status_code == 200
    assert_json(response.json(), {'groups': [{'title': 'Москва'}, {'title': 'Питер'}]})

    featured_pages_spb.set_visibility(VisibilityFilter(is_hidden=True))
    featured_pages_spb.save()

    response = client.get('/api/v2/public/me/featured_pages')
    assert response.status_code == 200
    assert_json(response.json(), {'groups': [{'title': 'Москва'}]})


@only_intranet
def test_filter_by_group(
    client, wiki_users, test_page, add_user_to_group, groups, featured_pages_msk, featured_pages_spb
):
    client.login(wiki_users.asm)

    featured_pages_spb.set_visibility(VisibilityFilter(groups=[groups.child_group.id]))
    featured_pages_spb.save()

    drop_acl_cache()

    _assert_msk(client)

    drop_acl_cache()

    add_user_to_group(group=groups.child_group, user=client.user)

    _assert_both(client)


@only_intranet
def test_filter_by_geo(client, wiki_users, test_page, geo, featured_pages_msk, featured_pages_spb):
    client.login(wiki_users.volozh)

    for vis, bad_office, good_office in [
        (VisibilityFilter(geo=GeoFilter(country=geo.il.id)), geo.redrose, geo.sarona),
        (VisibilityFilter(geo=GeoFilter(city=geo.spb.id)), geo.redrose, geo.neva),
        (VisibilityFilter(geo=GeoFilter(office=geo.redrose.id)), geo.neva, geo.redrose),
    ]:
        featured_pages_spb.set_visibility(vis)
        featured_pages_spb.save()

        client.user.staff.office = bad_office
        client.user.staff.save()

        _assert_msk(client)

        client.user.staff.office = good_office
        client.user.staff.save()

        _assert_both(client)


@only_intranet
def test_filter_by_staff_outstaff(client, wiki_users, test_page, geo, featured_pages_msk, featured_pages_spb):
    featured_pages_spb.set_visibility(VisibilityFilter(affilation=Affilation.STAFF))
    featured_pages_spb.save()

    featured_pages_msk.set_visibility(VisibilityFilter(affilation=Affilation.OUTSTAFF))
    featured_pages_msk.save()

    drop_acl_cache()
    make_user_outstaff(wiki_users.asm)
    client.login(wiki_users.asm)
    _assert_msk(client)

    drop_acl_cache()
    make_user_staff(wiki_users.volozh)
    client.login(wiki_users.volozh)
    _assert_spb(client)


def test_filter_by_language(client, wiki_users, featured_pages_msk, featured_pages_spb):
    client.login(wiki_users.volozh)

    featured_pages_spb.set_visibility(VisibilityFilter(language='en'))
    featured_pages_spb.save()

    _assert_msk(client)

    featured_pages_spb.set_visibility(VisibilityFilter(language='ru'))
    featured_pages_spb.save()

    _assert_both(client)
