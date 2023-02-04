import pytest

from intranet.wiki.tests.wiki_tests.common.acl_helper import set_access_author_only
from intranet.wiki.tests.wiki_tests.common.assert_helpers import assert_json
from wiki.api_v2.collections import PaginationQuery
from wiki.api_v2.public.navigation_tree.schemas import NodeType
from wiki.api_v2.public.navigation_tree import views as treeview
from wiki.api_v2.public.navigation_tree.views import _build_navview, _load_next
from wiki.api_v2.schemas import SlugOrEmpty
from wiki.sync.connect.base_organization import as_base_organization

pytestmark = [pytest.mark.django_db]


def test_branch_view_highlight(client, wiki_users, organizations, big_page_cluster, test_org):
    set_access_author_only(big_page_cluster['root/page12'])
    set_access_author_only(big_page_cluster['root/page22'])

    client.login(wiki_users.asm)
    response = client.get(
        '/api/v2/public/navtree/open_node?parent_slug=root&breadcrumbs_branch_slug=root/page15/subpage1/gap/child&page_size=5'
    )  # noqa

    n = response.json()['children']['next_cursor']
    assert response.status_code == 200
    response = client.get(f'/api/v2/public/navtree/load_next?parent_slug=root&page_size=5&cursor={n}')  # noqa
    assert response.status_code == 200


def test_branch_view_highlight_404(client, wiki_users, organizations, big_page_cluster, test_org):
    set_access_author_only(big_page_cluster['root/page12'])
    set_access_author_only(big_page_cluster['root/page22'])

    client.login(wiki_users.asm)
    response = client.get(
        '/api/v2/public/navtree/open_node?parent_slug=root&breadcrumbs_branch_slug=root/azazaza&page_size=5'
    )  # noqa

    assert response.status_code == 200
    assert len(response.json()['children']['results']) == 4


def test_branch_view_first_level(client, wiki_users, organizations, big_page_cluster, test_org):
    client.login(wiki_users.asm)
    response = client.get('/api/v2/public/navtree/open_node?parent_slug=""&page_size=5')
    assert response.status_code == 200
    assert len(response.json()['children']['results']) == 5


def test_branch_view(client, wiki_users, organizations, big_page_cluster, test_org):
    organization = as_base_organization(test_org)
    set_access_author_only(big_page_cluster['root/page12'])
    set_access_author_only(big_page_cluster['root/page22'])

    result = _build_navview(
        wiki_users.asm,
        organization,
        parent_slug=SlugOrEmpty('root'),
        open_around_node_slug='root/page15',
        page_size=5,
    )

    assert len(result.children.results) == 5 + 5 + 1 - 1  # locked page
    assert result.children.next_cursor is not None
    assert result.children.prev_cursor is not None
    assert_json(
        result.children.dict(),
        {
            'results': [
                {'slug': 'root/page20', 'type': NodeType.PAGE},
                {'slug': 'root/page19', 'type': NodeType.PAGE},
                {'slug': 'root/page18', 'type': NodeType.PAGE},
                {'slug': 'root/page17', 'type': NodeType.PAGE},
                {'slug': 'root/page16', 'type': NodeType.PAGE, 'has_children': True},
                {'slug': 'root/page15', 'type': NodeType.PAGE, 'has_children': True},  # center
                {'slug': 'root/page14', 'type': NodeType.PAGE},
                {'slug': 'root/page13', 'type': NodeType.PAGE, 'has_children': True},
                # {'slug': 'root/page12', 'type': NodeType.LOCKED},
                {'slug': 'root/page11', 'type': NodeType.PAGE},
                {'slug': 'root/page10', 'type': NodeType.PAGE},
            ]
        },
    )

    prev_chunk = _load_next(
        wiki_users.asm,
        organization,
        SlugOrEmpty('root'),
        PaginationQuery(page_size=5, cursor=result.children.prev_cursor),
    )
    assert_json(
        prev_chunk.dict(),
        {
            'results': [
                {'slug': 'root/page24', 'type': NodeType.PAGE},
                {'slug': 'root/page23', 'type': NodeType.PAGE},
                # {'slug': 'root/page22', 'type': NodeType.LOCKED, 'has_children': False},
                {'slug': 'root/page21', 'type': NodeType.PAGE},
            ]
        },
    )

    next_chunk = _load_next(
        wiki_users.asm,
        organization,
        SlugOrEmpty('root'),
        PaginationQuery(page_size=5, cursor=result.children.next_cursor),
    )
    assert_json(
        next_chunk.dict(),
        {
            'results': [
                {'slug': 'root/page9', 'type': NodeType.PAGE},
                {'slug': 'root/page8', 'type': NodeType.PAGE, 'has_children': True},
                {'slug': 'root/page7', 'type': NodeType.PAGE},
                {'slug': 'root/page6', 'type': NodeType.PAGE},
                {
                    'slug': 'root/page5',
                    'type': NodeType.PAGE,
                },
            ]
        },
    )

    result = _build_navview(
        wiki_users.asm,
        organization,
        parent_slug=SlugOrEmpty('root'),
        open_around_node_slug='root/page2',
        page_size=5,
    )
    assert result.children.next_cursor is None
    assert result.children.prev_cursor is not None

    result = _build_navview(
        wiki_users.asm,
        organization,
        parent_slug=SlugOrEmpty('root'),
        open_around_node_slug='root/page24',
        page_size=5,
    )

    assert result.children.next_cursor is not None
    assert result.children.prev_cursor is None

    result = _build_navview(wiki_users.asm, organization, parent_slug=SlugOrEmpty('root'), page_size=5)

    assert_json(
        result.dict()['children']['results'],
        [
            {'has_children': False, 'slug': 'root/page24', 'type': NodeType.PAGE},
            {'has_children': False, 'slug': 'root/page23', 'type': NodeType.PAGE},
            # {'has_children': False, 'slug': 'root/page22', 'type': NodeType.LOCKED},
            {'has_children': False, 'slug': 'root/page21', 'type': NodeType.PAGE},
            {'has_children': False, 'slug': 'root/page20', 'type': NodeType.PAGE},
        ],
    )


def test_first_level_open_around(client, wiki_users, organizations, big_page_cluster, test_org):
    organization = as_base_organization(test_org)

    result = _build_navview(
        wiki_users.asm,
        organization,
        parent_slug=SlugOrEmpty(''),
        open_around_node_slug='kroot',
        page_size=5,
    )

    assert result.children.next_cursor is None
    assert result.children.prev_cursor is not None
    assert_json(
        result.dict()['children']['results'],
        [
            {'has_children': False, 'slug': 'root4', 'type': NodeType.PAGE},
            {'has_children': False, 'slug': 'root3', 'type': NodeType.PAGE},
            {'has_children': False, 'slug': 'root2', 'type': NodeType.PAGE},
            {'has_children': False, 'slug': 'root1', 'type': NodeType.PAGE},
            {'has_children': False, 'slug': 'root0', 'type': NodeType.PAGE},
            {'has_children': True, 'slug': 'kroot', 'type': NodeType.PAGE},
            {'has_children': True, 'slug': 'broot', 'type': NodeType.PAGE},
            {'has_children': True, 'slug': 'root', 'type': NodeType.PAGE},
        ],
    )

    next_chunk = _load_next(
        wiki_users.asm,
        organization,
        parent_slug=SlugOrEmpty(''),
        pagination=PaginationQuery(page_size=50, cursor=result.children.prev_cursor),
    )

    assert len(next_chunk.results) == 50
    assert next_chunk.next_cursor is not None
    assert next_chunk.prev_cursor is not None

    next_chunk = _load_next(
        wiki_users.asm,
        organization,
        parent_slug=SlugOrEmpty(''),
        pagination=PaginationQuery(page_size=50, cursor=next_chunk.prev_cursor),
    )

    assert len(next_chunk.results) == 45
    assert next_chunk.next_cursor is not None
    assert next_chunk.prev_cursor is None


def test_first_level_breadcrumbs_branch(client, wiki_users, organizations, big_page_cluster, test_org):
    organization = as_base_organization(test_org)

    # case 1 breadcrumbs_branch_slug='root'
    result = _build_navview(
        wiki_users.asm,
        organization,
        parent_slug=SlugOrEmpty(''),
        breadcrumbs_branch_slug='root',
        page_size=5,
    )

    assert result.children.next_cursor is None
    assert result.children.prev_cursor is not None

    assert_json(
        result.dict()['children']['results'],
        [
            {'has_children': False, 'slug': 'root2', 'type': NodeType.PAGE},
            {'has_children': False, 'slug': 'root1', 'type': NodeType.PAGE},
            {'has_children': False, 'slug': 'root0', 'type': NodeType.PAGE},
            {'has_children': True, 'slug': 'kroot', 'type': NodeType.PAGE},
            {'has_children': True, 'slug': 'broot', 'type': NodeType.PAGE},
            {'has_children': True, 'slug': 'root', 'type': NodeType.PAGE},
        ],
    )

    assert_json(
        result.dict()['breadcrumbs_branch'],
        [
            {'has_children': True, 'slug': 'root', 'type': NodeType.PAGE},
        ],
    )

    # case 2 breadcrumbs_branch_slug='root/page15/subpage1'
    result = _build_navview(
        wiki_users.asm,
        organization,
        parent_slug=SlugOrEmpty(''),
        breadcrumbs_branch_slug='root/page15/subpage1',
        page_size=5,
    )
    assert result.children.next_cursor is None
    assert result.children.prev_cursor is not None

    assert_json(
        result.dict()['children']['results'],
        [
            {'has_children': False, 'slug': 'root2', 'type': NodeType.PAGE},
            {'has_children': False, 'slug': 'root1', 'type': NodeType.PAGE},
            {'has_children': False, 'slug': 'root0', 'type': NodeType.PAGE},
            {'has_children': True, 'slug': 'kroot', 'type': NodeType.PAGE},
            {'has_children': True, 'slug': 'broot', 'type': NodeType.PAGE},
            {'has_children': True, 'slug': 'root', 'type': NodeType.PAGE},
        ],
    )

    assert_json(
        result.dict()['breadcrumbs_branch'],
        [
            {'has_children': True, 'slug': 'root', 'type': NodeType.PAGE},
            {'has_children': True, 'slug': 'root/page15', 'type': NodeType.PAGE},
            {'has_children': True, 'slug': 'root/page15/subpage1', 'type': NodeType.PAGE},
        ],
    )


def test_tree_with_copy_on_write_pages(client, wiki_users, organizations, big_page_cluster, test_org, monkeypatch):
    organization = as_base_organization(test_org)

    copy_on_write_info = {'id': -100, 'en': {'title': 'Homepage', 'template': 'pages/ru/homepage.txt'}}

    monkeypatch.setattr(treeview, 'COW_SLUGS', {'homepage': copy_on_write_info})

    result = _build_navview(
        wiki_users.asm,
        organization,
        parent_slug=SlugOrEmpty(''),
        page_size=5,
    )

    assert_json(
        result.dict()['children']['results'],
        [
            {'id': -100, 'has_children': False, 'slug': 'homepage', 'type': NodeType.COW},
            {'has_children': False, 'slug': 'root99', 'type': NodeType.PAGE},
            {'has_children': False, 'slug': 'root98', 'type': NodeType.PAGE},
            {'has_children': False, 'slug': 'root97', 'type': NodeType.PAGE},
            {'has_children': False, 'slug': 'root96', 'type': NodeType.PAGE},
            {'has_children': False, 'slug': 'root95', 'type': NodeType.PAGE},
        ],
    )


def test_copy_on_write_pages__breadcrumb(client, wiki_users, organizations, big_page_cluster, test_org, monkeypatch):
    organization = as_base_organization(test_org)

    copy_on_write_info = {'id': -100, 'en': {'title': 'Homepage', 'template': 'pages/ru/homepage.txt'}}

    big_page_cluster['root'].supertag = 'rut'
    big_page_cluster['root'].save()

    monkeypatch.setattr(treeview, 'COW_SLUGS', {'root': copy_on_write_info})

    result = _build_navview(
        wiki_users.asm,
        organization,
        parent_slug=SlugOrEmpty(''),
        breadcrumbs_branch_slug='root/page15/subpage1',
        page_size=5,
    )

    assert_json(
        result.dict()['children']['results'],
        [
            {'id': -100, 'has_children': True, 'slug': 'root', 'type': NodeType.COW},
            {'has_children': False, 'slug': 'root99', 'type': NodeType.PAGE},
            {'has_children': False, 'slug': 'root98', 'type': NodeType.PAGE},
            {'has_children': False, 'slug': 'root97', 'type': NodeType.PAGE},
            {'has_children': False, 'slug': 'root96', 'type': NodeType.PAGE},
            {'has_children': False, 'slug': 'root95', 'type': NodeType.PAGE},
        ],
    )

    assert_json(
        result.dict()['breadcrumbs_branch'],
        [
            {'has_children': True, 'slug': 'root', 'type': NodeType.COW},
            {'has_children': True, 'slug': 'root/page15', 'type': NodeType.PAGE},
            {'has_children': True, 'slug': 'root/page15/subpage1', 'type': NodeType.PAGE},
        ],
    )


def test_tree_with_fixed_nodes(client, wiki_users, organizations, big_page_cluster, test_org, monkeypatch):
    monkeypatch.setattr(treeview, 'FIXED_SLUGS', ['root96', 'kroot'])
    organization = as_base_organization(test_org)

    result = _build_navview(
        wiki_users.asm,
        organization,
        parent_slug=SlugOrEmpty(''),
        page_size=5,
    )

    assert result.children.next_cursor is not None
    assert result.children.prev_cursor is None

    expected_results = [
        {'has_children': False, 'slug': 'root96', 'type': NodeType.PAGE, 'is_fixed': True},
        {'has_children': True, 'slug': 'kroot', 'type': NodeType.PAGE, 'is_fixed': True},
        {'has_children': False, 'slug': 'root99', 'type': NodeType.PAGE, 'is_fixed': False},
        {'has_children': False, 'slug': 'root98', 'type': NodeType.PAGE, 'is_fixed': False},
        {'has_children': False, 'slug': 'root97', 'type': NodeType.PAGE, 'is_fixed': False},
    ]
    assert_json(result.dict()['children']['results'], expected_results)


def test_tree_with_fixed_nodes__breadcrumbs(client, wiki_users, organizations, big_page_cluster, test_org, monkeypatch):
    monkeypatch.setattr(treeview, 'FIXED_SLUGS', ['root96', 'kroot'])
    organization = as_base_organization(test_org)

    result = _build_navview(
        wiki_users.asm,
        organization,
        parent_slug=SlugOrEmpty(''),
        breadcrumbs_branch_slug='root93',
        page_size=3,
    )

    assert result.children.next_cursor is not None
    assert result.children.prev_cursor is not None

    expected_results = [
        {'has_children': False, 'slug': 'root97', 'type': NodeType.PAGE, 'is_fixed': False},
        {'has_children': False, 'slug': 'root95', 'type': NodeType.PAGE, 'is_fixed': False},
        {'has_children': False, 'slug': 'root94', 'type': NodeType.PAGE, 'is_fixed': False},
        {'has_children': False, 'slug': 'root93', 'type': NodeType.PAGE, 'is_fixed': False},
        {'has_children': False, 'slug': 'root92', 'type': NodeType.PAGE, 'is_fixed': False},
        {'has_children': False, 'slug': 'root91', 'type': NodeType.PAGE, 'is_fixed': False},
        {'has_children': False, 'slug': 'root90', 'type': NodeType.PAGE, 'is_fixed': False},
    ]
    assert_json(result.dict()['children']['results'], expected_results)

    prev_chunk = _load_next(
        user=wiki_users.asm,
        organization=organization,
        parent_slug=SlugOrEmpty(''),
        pagination=PaginationQuery(page_size=5, cursor=result.children.prev_cursor),
    )

    assert prev_chunk.next_cursor is not None
    assert prev_chunk.prev_cursor is None

    expected_results = [
        {'has_children': False, 'slug': 'root96', 'type': NodeType.PAGE, 'is_fixed': True},
        {'has_children': True, 'slug': 'kroot', 'type': NodeType.PAGE, 'is_fixed': True},
        {'has_children': False, 'slug': 'root99', 'type': NodeType.PAGE, 'is_fixed': False},
        {'has_children': False, 'slug': 'root98', 'type': NodeType.PAGE, 'is_fixed': False},
    ]

    assert_json(prev_chunk.dict()['results'], expected_results)


def test_tree_with_fixed_nodes__order_cow(client, wiki_users, organizations, big_page_cluster, test_org, monkeypatch):
    copy_on_write_info = {'id': -100, 'en': {'title': 'Homepage', 'template': 'pages/ru/homepage.txt'}}
    monkeypatch.setattr(treeview, 'COW_SLUGS', {'homepage': copy_on_write_info})

    monkeypatch.setattr(treeview, 'FIXED_SLUGS', ['root96', 'homepage', 'kroot'])

    big_page_cluster['kroot'].redirects_to = big_page_cluster['broot']
    big_page_cluster['kroot'].save()

    # check _build_navview
    result = _build_navview(
        user=wiki_users.asm,
        organization=as_base_organization(test_org),
        parent_slug=SlugOrEmpty(''),
        page_size=3,
    )

    assert result.children.next_cursor is not None
    assert result.children.prev_cursor is None

    expected_results = [
        {'has_children': False, 'slug': 'root96', 'type': NodeType.PAGE, 'is_fixed': True},
        {'has_children': False, 'slug': 'homepage', 'type': NodeType.COW, 'is_fixed': True},
        {'has_children': True, 'slug': 'kroot', 'type': NodeType.PAGE, 'is_fixed': True},
        {'has_children': False, 'slug': 'root99', 'type': NodeType.PAGE, 'is_fixed': False},
    ]
    assert_json(result.dict()['children']['results'], expected_results)

    # step forward
    prev_chunk = _load_next(
        user=wiki_users.asm,
        organization=as_base_organization(test_org),
        parent_slug=SlugOrEmpty(''),
        pagination=PaginationQuery(cursor=result.children.next_cursor),
    )
    assert prev_chunk.prev_cursor is not None

    # check _load_next
    prev_chunk = _load_next(
        user=wiki_users.asm,
        organization=as_base_organization(test_org),
        parent_slug=SlugOrEmpty(''),
        pagination=PaginationQuery(page_size=3, cursor=prev_chunk.prev_cursor),
    )
    assert prev_chunk.prev_cursor is None
    assert_json(prev_chunk.dict()['results'], expected_results)
