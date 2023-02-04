import pytest
from mock import patch, MagicMock

from wiki.cloudsearch.staff_client import STAFF_API_CLIENT
from intranet.wiki.tests.wiki_tests.common.skips import only_intranet

from wiki.pages.logic.authors_add_chief import add_first_found_chief_to_authors
from wiki.utils.wiki_robot import get_wiki_robot


@only_intranet
@pytest.mark.django_db
@patch.object(STAFF_API_CLIENT, 'get_chief')
def test_add_chief(get_chief: MagicMock, wiki_users, test_page):
    """test_page: thasonic (chief=chapson)"""
    wiki_users.thasonic.staff.is_dismissed = True
    wiki_users.thasonic.staff.save()

    staff_ans = {'thasonic': {wiki_users.chapson.username}}
    get_chief.side_effect = lambda login: staff_ans.get(login, set())

    add_first_found_chief_to_authors(page=test_page)
    assert wiki_users.chapson in test_page.get_authors()


@only_intranet
@pytest.mark.django_db
@patch.object(STAFF_API_CLIENT, 'get_chief')
def test_add_chief__dont_add_robots(get_chief: MagicMock, wiki_users, page_cluster):
    """
    root/a/aa
    """
    wiki_users.thasonic.staff.is_dismissed = True
    wiki_users.thasonic.staff.save()

    wiki_robot = get_wiki_robot()

    page_cluster['root/a'].authors.set([wiki_robot])
    get_chief.side_effect = lambda login: set()

    add_first_found_chief_to_authors(page=page_cluster['root/a/aa'])

    assert wiki_robot not in page_cluster['root/a/aa'].get_authors()


@only_intranet
@pytest.mark.django_db
@patch.object(STAFF_API_CLIENT, 'get_chief')
def test_not_add_big_chief(get_chief: MagicMock, wiki_users, test_page):
    """test_page: thasonic (chief=volozh)"""
    wiki_users.thasonic.staff.is_dismissed = True
    wiki_users.thasonic.staff.save()

    staff_ans = {'thasonic': {wiki_users.volozh.username}, 'volozh': set()}
    get_chief.side_effect = lambda login: staff_ans.get(login, set())

    add_first_found_chief_to_authors(page=test_page)
    assert wiki_users.volozh not in test_page.get_authors()


@only_intranet
@pytest.mark.django_db
@patch.object(STAFF_API_CLIENT, 'get_chief')
def test_add_chief_next_author(get_chief: MagicMock, wiki_users, test_page):
    """test_page: thasonic (chief=chapson), asm (chief=kolomeetz)"""
    test_page.authors.add(wiki_users.asm)  # now two authors: thasonic and asm
    test_page.save()

    for user in [wiki_users.thasonic, wiki_users.asm, wiki_users.chapson]:  # thasonic's chief also dismissed
        user.staff.is_dismissed = True
        user.staff.save()

    staff_ans = {'thasonic,asm': {wiki_users.chapson.username, wiki_users.kolomeetz.username}}
    get_chief.side_effect = lambda login: staff_ans.get(login, set())

    add_first_found_chief_to_authors(page=test_page)
    assert wiki_users.kolomeetz in test_page.get_authors()


@only_intranet
@pytest.mark.django_db
@patch.object(STAFF_API_CLIENT, 'get_chief')
def test_add_chief_chief(get_chief: MagicMock, wiki_users, test_page):
    """test_page: thasonic (chief=chapson (chief=kolomeetz))"""
    for user in [wiki_users.thasonic, wiki_users.chapson]:
        user.staff.is_dismissed = True
        user.staff.save()

    staff_ans = {'thasonic': {wiki_users.chapson.username}, 'chapson': {wiki_users.kolomeetz.username}}
    get_chief.side_effect = lambda login: staff_ans.get(login, set())

    add_first_found_chief_to_authors(page=test_page)
    assert wiki_users.kolomeetz in test_page.get_authors()


@only_intranet
@pytest.mark.django_db
@patch.object(STAFF_API_CLIENT, 'get_chief')
def test_add_alive_author_parent_page(get_chief: MagicMock, wiki_users, page_cluster):
    """
    root/a: thasonic, asm
    root/a/aa: thasonic
    """
    wiki_users.thasonic.staff.is_dismissed = True
    wiki_users.thasonic.staff.save()

    page_cluster['root/a'].authors.add(wiki_users.asm)
    page_cluster['root/a'].save()

    get_chief.side_effect = lambda login: set()

    add_first_found_chief_to_authors(page=page_cluster['root/a/aa'])
    assert wiki_users.asm in page_cluster['root/a/aa'].get_authors()


@only_intranet
@pytest.mark.django_db
@patch.object(STAFF_API_CLIENT, 'get_chief')
def test_chief_author_parent_page(get_chief: MagicMock, wiki_users, page_cluster):
    """
    root/a: thasonic, asm (chief=kolomeetz)
    root/a/aa: thasonic
    """
    for user in [wiki_users.thasonic, wiki_users.asm]:
        user.staff.is_dismissed = True
        user.staff.save()

    page_cluster['root/a'].authors.add(wiki_users.asm)
    page_cluster['root/a'].save()

    staff_ans = {'thasonic': set(), 'thasonic,asm': {wiki_users.kolomeetz.username}}
    get_chief.side_effect = lambda login: staff_ans.get(login, set())

    add_first_found_chief_to_authors(page=page_cluster['root/a/aa'])
    assert wiki_users.kolomeetz in page_cluster['root/a/aa'].get_authors()


@only_intranet
@pytest.mark.django_db
@patch.object(STAFF_API_CLIENT, 'get_chief')
def test_no_add_big_chief_author_parent_page(get_chief: MagicMock, wiki_users, page_cluster):
    """
    root/a: thasonic, volozh
    root/a/aa: thasonic
    """
    wiki_users.thasonic.staff.is_dismissed = True
    wiki_users.thasonic.staff.save()

    page_cluster['root/a'].authors.add(wiki_users.volozh)
    page_cluster['root/a'].save()

    get_chief.side_effect = lambda login: set()

    add_first_found_chief_to_authors(page=page_cluster['root/a/aa'])
    assert wiki_users.volozh not in page_cluster['root/a/aa'].get_authors()


@only_intranet
@pytest.mark.django_db
@patch.object(STAFF_API_CLIENT, 'get_chief')
def test_all_at_once(get_chief: MagicMock, wiki_users, page_cluster):
    """
    root: thasonic (chief=chapson), asm (chief=kolomeetz)
    root/a: thasonic (chief=chapson), volozh (chief={})
    root/a/aa: thasonic (chief=chapson)

    dismissed: thasonic, chapson, asm

    as result, wanna add kolomeetz to page['root/a/aa']
    """

    for user in [wiki_users.thasonic, wiki_users.chapson, wiki_users.asm]:
        user.staff.is_dismissed = True
        user.staff.save()

    page_cluster['root/a'].authors.add(wiki_users.volozh)
    page_cluster['root'].authors.add(wiki_users.asm)
    page_cluster['root/a'].save()
    page_cluster['root'].save()

    staff_ans = {
        'thasonic': {wiki_users.chapson.username},
        'chapson': set(),
        'volozh': set(),
        'thasonic,volozh': {wiki_users.chapson.username},
        'thasonic,asm': {wiki_users.chapson.username, wiki_users.kolomeetz.username},
    }
    get_chief.side_effect = lambda login: staff_ans.get(login, set())

    add_first_found_chief_to_authors(page=page_cluster['root/a/aa'])
    assert wiki_users.kolomeetz in page_cluster['root/a/aa'].get_authors()
    assert wiki_users.volozh not in page_cluster['root/a/aa'].get_authors()
