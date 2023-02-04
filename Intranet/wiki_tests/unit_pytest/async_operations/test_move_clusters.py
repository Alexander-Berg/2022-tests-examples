import emoji
import pytest
import re
import mock

from django.test import override_settings
from django.conf import settings

from wiki.api_v2.exceptions import BadRequest
from wiki.async_operations.consts import Status, OperationOwner
from wiki.pages.constants import ReservedSupertagAction as Action, PageOrderPosition
from wiki.async_operations.operation_executors.move_cluster.consts import (
    MoveCluster,
    MoveClusterRequest,
    MoveClusterRequestWithLegacy,
)
from wiki.async_operations.operation_executors.move_cluster.messages import (
    BECOME_NOT_ACCESSIBLE,
    CLUSTER_BLOCKED,
    CLUSTER_NOT_EXISTS,
    CLUSTER_PERMISSION_DENIED,
    DESTINATION_RESERVED,
    NEXT_TO_HAS_WRONG_CLUSTER,
    NEXT_TO_NOT_EXISTS,
    NEXT_TO_WILL_MOVE,
    OPERATION_LIMIT_EXCEEDED,
    OVERRIDE_ATTEMPT,
    SOURCE_RESERVED,
    SLUG_IS_FIXED,
    TOO_LONG_NAME,
)
from wiki.async_operations.operation_executors.move_cluster.move_cluster import MoveClusterOperation
from wiki.async_operations.progress_storage import ASYNC_OP_PROGRESS_STORAGE
from wiki.pages.logic.backlinks import track_links
from wiki.pages.models import Page, LocationHistory
from wiki.pages.models.cluster_change import (
    AffectedPage,
    ClusterChange,
    ChangeStatus,
    ClusterBlock,
    Operations,
    AffectedPages,
)
from wiki.sync.connect.org_ctx import org_ctx

from intranet.wiki.tests.wiki_tests.common.acl_helper import set_access_author_only, set_access_everyone
from intranet.wiki.tests.wiki_tests.common.skips import only_biz
from intranet.wiki.tests.wiki_tests.common.utils import celery_eager

pytestmark = [pytest.mark.django_db]


def assert_validation_error(operations: list[MoveCluster], owner: OperationOwner, error_code: str):
    with pytest.raises(BadRequest) as err:
        data = MoveClusterRequestWithLegacy(operations=operations)
        MoveClusterOperation(data, owner).check_preconditions()

    assert err.value.error_code == error_code
    return err.value


def test_preconditions__exists(page_cluster, owner):
    # Нечего перемещать
    operations = [
        MoveCluster(source='not-exists', target='root/a/b'),
    ]
    assert_validation_error(operations, owner, CLUSTER_NOT_EXISTS)

    # Уже переместили
    operations = [
        MoveCluster(source='root/b', target='root/a/b'),
        MoveCluster(source='root/b', target='root/c/b'),
    ]
    assert_validation_error(operations, owner, CLUSTER_NOT_EXISTS)

    # Страница кластера не найдена
    page_cluster['root/b'].delete()
    operations = [
        MoveCluster(source='root/b', target='root/a/b'),
    ]
    assert_validation_error(operations, owner, CLUSTER_NOT_EXISTS)


def test_preconditions__reserved(page_cluster, owner):
    reserved_users = {'pattern': re.compile(r'^users(/[^/]+)?/?$'), 'actions': [Action.CREATE, Action.DELETE]}
    with override_settings(RESERVED_SUPERTAGS=[reserved_users]):

        # source reserved
        operations = [
            MoveCluster(source='users/someprotectedpage', target='root/a/b'),
        ]
        assert_validation_error(operations, owner, SOURCE_RESERVED)

        # target reserved
        operations = [
            MoveCluster(source='root/a', target='users/protected'),
        ]
        assert_validation_error(operations, owner, DESTINATION_RESERVED)


def test_preconditions__long_slug(page_cluster, owner):
    # simple
    operations = [
        MoveCluster(source='root/a', target='a' * 255),
    ]
    assert_validation_error(operations, owner, TOO_LONG_NAME)

    # many operation
    operations = []
    prev = 'root'
    for step in ['a' * 10, 'b' * 50, 'c' * 150, 'e' * 253]:
        operation = MoveCluster(source=f'{prev}/a', target=f'{step}/a')
        operations.append(operation)
        prev = step
    assert_validation_error(operations, owner, TOO_LONG_NAME)


def test_preconditions__clashing_pages(page_cluster, owner):
    # simple
    operations = [
        MoveCluster(source='root/b', target='root/a'),
    ]
    assert_validation_error(operations, owner, OVERRIDE_ATTEMPT)

    # сначала освободили root/b, затем заняли, а второй раз занять не получилось
    operations = [
        MoveCluster(source='root/b', target='root/a/b'),
        MoveCluster(source='root/c', target='root/b'),
        MoveCluster(source='root/a', target='root/b'),
    ]
    assert_validation_error(operations, owner, OVERRIDE_ATTEMPT)


def test_preconditions__clashing_pages__only_reorder(page_cluster, owner):
    operations = [
        MoveCluster(source='root/b', target='root/b', next_to_slug='root/c', position='after'),
        MoveCluster(source='root/b', target='root/b', next_to_slug='root/a', position='before'),
        MoveCluster(source='root', target='same'),
        MoveCluster(source='same/b', target='same/b', next_to_slug='same/c', position='after'),
        MoveCluster(source='same/b', target='same/b', next_to_slug='same/a', position='before'),
    ]

    data = MoveClusterRequestWithLegacy(operations=operations)
    MoveClusterOperation(data, owner).check_preconditions()


def test_preconditions__accesses(page_cluster, owner, wiki_users, test_org_ctx):
    set_access_author_only(page_cluster['root/b/bd/bc'], [wiki_users.asm])

    # no access to source sub_page 'root/b/bd/bc'
    operations = [
        MoveCluster(source='root/b/bd', target='root/a/b'),
    ]
    assert_validation_error(operations, owner, CLUSTER_PERMISSION_DENIED)

    # no access to target
    page_cluster['root/c'].authors.set([wiki_users.asm])  # no thasonic
    set_access_everyone(page_cluster['root/c'])

    operations = [
        MoveCluster(source='root/c', target='root/b/bd/bc/c'),
    ]
    assert_validation_error(operations, owner, BECOME_NOT_ACCESSIBLE)


def test_preconditions__next_to_slug(page_cluster, owner):
    # out cluster
    operations = [
        MoveCluster(source='root/b', target='root/a/b', next_to_slug='root/c'),
    ]
    assert_validation_error(operations, owner, NEXT_TO_HAS_WRONG_CLUSTER)

    # next to nested page, which moved
    operations = [
        MoveCluster(source='root/a', target='root/a/inside', next_to_slug='root/a/aa'),
    ]
    assert_validation_error(operations, owner, NEXT_TO_WILL_MOVE)

    # next to equal source and target
    operations = [
        MoveCluster(source='root/a', target='moved'),
        MoveCluster(source='moved', target='any', next_to_slug='moved'),
    ]
    assert_validation_error(operations, owner, NEXT_TO_WILL_MOVE)

    # next to not existing page
    operations = [
        MoveCluster(source='root/b', target='root/a/b', next_to_slug='root/a/non-exist'),
    ]
    assert_validation_error(operations, owner, NEXT_TO_NOT_EXISTS)

    # next to moved page
    operations = [
        MoveCluster(source='root/b', target='root/a/b'),
        MoveCluster(source='root/a/aa', target='root/aa', next_to_slug='root/b'),
    ]
    assert_validation_error(operations, owner, NEXT_TO_NOT_EXISTS)


@mock.patch('wiki.async_operations.operation_executors.move_cluster.move_cluster.FIXED_SLUGS', ['root/a/ad', 'fixed'])
def test_preconditions__fixed_page(page_cluster, owner):
    # source - fixed page
    operations = [
        MoveCluster(source='root/a/ad', target='any'),
    ]
    assert_validation_error(operations, owner, SLUG_IS_FIXED)

    # target - fixed page
    operations = [
        MoveCluster(source='root/a', target='fixed'),
    ]
    assert_validation_error(operations, owner, SLUG_IS_FIXED)

    # only reorder - fixed page
    operations = [
        MoveCluster(source='root/a/ad', target='root/a/ad', next_to_slug='root/a/aa'),
    ]
    assert_validation_error(operations, owner, SLUG_IS_FIXED)

    # reorder after fixed page
    operations = [
        MoveCluster(source='root/a/aa', target='root/a/aa', next_to_slug='root/a/ad'),
    ]
    assert_validation_error(operations, owner, SLUG_IS_FIXED)


def test_preconditions__limit_operations(page_cluster, owner):
    operations = [
        MoveCluster(source='root/b', target='b'),
        MoveCluster(source='root/c', target='c'),
        MoveCluster(source='root/a', target='a'),
    ]
    with mock.patch('wiki.async_operations.operation_executors.move_cluster.move_cluster.LIMIT_OPERATIONS', 2):
        assert_validation_error(operations, owner, OPERATION_LIMIT_EXCEEDED)


def test_move__post_endpoint(client, wiki_users, page_cluster):
    client.login(wiki_users.thasonic)

    operations = [MoveCluster(source='root/b', target='root/a/b')]
    api_v2 = '/api/v2/public/pages/move', MoveClusterRequest(operations=operations).dict()
    legacy = '/_api/frontend/.async_operations/move_cluster', {'source': 'root/c', 'target': 'lol'}

    for url, data in [api_v2, legacy]:
        response = client.post(url, data=data)
        assert response.status_code == 200

        if 'operation' in response.json():
            task_id = response.json()['operation']['id']
        else:  # legacy
            task_id = response.json()['data']['task_id']

        assert ASYNC_OP_PROGRESS_STORAGE.load_by_task_id(task_id).status == Status.SCHEDULED


@celery_eager
def test_move__dry_run(client, page_cluster, wiki_users):
    client.login(wiki_users.thasonic)

    request_url = '/api/v2/public/pages/move?dry_run=1'
    operations = [MoveCluster(source='root/b', target='root/a/b')]
    data = MoveClusterRequest(operations=operations)

    response = client.post(request_url, data=data.dict())
    assert response.status_code == 200, response.json()
    assert response.json()['dry_run']

    for page in page_cluster.values():
        if page.slug.startswith('root'):
            original_slug = page.slug
            page.refresh_from_db()
            assert page.slug == original_slug

    assert ClusterChange.objects.exists() is False


@celery_eager
def test_move__positive_case(client, page_cluster, wiki_users):
    client.login(wiki_users.thasonic)
    request_url = '/api/v2/public/pages/move'

    operations = [
        MoveCluster(source='root', target='target'),
    ]
    data = MoveClusterRequest(operations=operations)

    response = client.post(request_url, data=data.dict())
    assert response.status_code == 200
    assert not response.json()['dry_run']

    status_url = response.json()['status_url']

    response = client.get(status_url)
    assert response.json()['status'] == Status.SUCCESS

    affected_pages = []
    for page in page_cluster.values():
        if page.slug.startswith('root'):
            old_url = page.slug
            new_url = page.slug.replace('root', 'target', 1)

            page.refresh_from_db()
            assert page.slug == new_url
            assert LocationHistory.objects.filter(slug=old_url, page=page).exists()

            affected_page = AffectedPage(id=page.id, slug=new_url, previous_slug=old_url)
            affected_pages.append(affected_page)

    cluster_change = ClusterChange.objects.get(user=client.user)
    assert cluster_change.affected_pages == AffectedPages(pages=affected_pages).dict()
    assert cluster_change.operations == Operations(input=operations, compress=operations).dict()

    assert Page.objects.filter(redirects_to__isnull=False).exists() is False  # no redirects


@celery_eager
def test_move__many(client, page_cluster, wiki_users):
    client.login(wiki_users.thasonic)
    request_url = '/api/v2/public/pages/move'

    operations = [
        MoveCluster(source='root/b', target='root/a/b'),
        MoveCluster(source='root/a', target='root/c/a'),
    ]
    data = MoveClusterRequest(operations=operations)

    response = client.post(request_url, data=data.dict())
    assert response.status_code == 200

    assert Page.objects.get(id=page_cluster['root/a'].id).slug == 'root/c/a'
    assert Page.objects.get(id=page_cluster['root/b/bd/bc'].id).slug == 'root/c/a/b/bd/bc'


@celery_eager
def test_move__legacy(client, wiki_users, page_cluster):
    client.login(wiki_users.thasonic)

    source, target = 'root', 'first/second'
    before = Page.objects.filter(supertag__startswith=source).count()

    request_url = '/_api/frontend/.async_operations/move_cluster'
    data = {'source': source, 'target': target, 'with_children': True}
    response = client.post(request_url, data=data)
    assert response.status_code == 200

    after = Page.objects.filter(supertag__startswith=source).count()

    pages = set(Page.objects.all())

    for page in pages:
        if page.redirects_to:
            assert page.redirects_to in pages
            assert list(page.authors.all()) == [wiki_users.thasonic]

    assert before == after == Page.objects.filter(supertag__startswith=target).count()
    assert LocationHistory.objects.exists() is False


@celery_eager
def test_move__legacy__without_children(client, wiki_users, page_cluster):
    client.login(wiki_users.thasonic)

    source, target = 'root', 'first/second'

    request_url = '/_api/frontend/.async_operations/move_cluster'
    data = {'source': source, 'target': target, 'with_children': False}
    response = client.post(request_url, data=data)
    assert response.status_code == 200

    for page in page_cluster.values():
        new_slug = target if page.slug == source else page.slug
        page.refresh_from_db()
        assert page.slug == new_slug


@celery_eager
def test_move_legacy__with_redirect(client, wiki_users, page_cluster):
    client.login(wiki_users.thasonic)
    request_url = '/_api/frontend/.async_operations/move_cluster'

    # mark page as redirect
    page_cluster['root/b'].redirects_to = page_cluster['root/a']
    page_cluster['root/b'].save()

    data = {'source': 'root', 'target': 'first/second', 'with_children': True}
    response = client.post(request_url, data=data)
    assert response.status_code == 200

    assert Page.objects.get(supertag='first/second/b').redirects_to == Page.objects.get(supertag='first/second/a')
    assert Page.objects.get(supertag='root/b').redirects_to == Page.objects.get(supertag='first/second/b')


@celery_eager
def test_move_legacy__cloud_redirect(client, wiki_users, cloud_page_cluster):
    client.login(wiki_users.thasonic)
    request_url = '/_api/frontend/.async_operations/move_cluster'

    # page = cloud_page_cluster['root/a']

    data = {'source': 'root/a', 'target': 'first/second', 'with_children': True}
    response = client.post(request_url, data=data)
    assert response.status_code == 200

    redirect, moved = Page.objects.get(supertag='root/a'), Page.objects.get(supertag='first/second')

    assert redirect.page_type == Page.TYPES.PAGE
    assert moved.page_type == Page.TYPES.CLOUD
    assert redirect.redirects_to == moved


@celery_eager
def test_move__move_to_vacant_space(client, page_cluster, wiki_users):
    client.login(wiki_users.thasonic)
    request_url = '/api/v2/public/pages/move'

    operations = [
        MoveCluster(source='root/b', target='root/a/b'),
        MoveCluster(source='root/c', target='root/b'),
    ]
    data = MoveClusterRequest(operations=operations)

    response = client.post(request_url, data=data.dict())
    assert response.status_code == 200

    assert Page.objects.get(id=page_cluster['root/c'].id).slug == 'root/b'
    assert Page.objects.get(id=page_cluster['root/b/bd/bc'].id).slug == 'root/a/b/bd/bc'


@celery_eager
def test_move__replace(client, page_cluster, wiki_users):
    client.login(wiki_users.thasonic)
    request_url = '/api/v2/public/pages/move'

    operations = [
        MoveCluster(source='root/b', target='root/out'),
        MoveCluster(source='root/a', target='root/b'),
        MoveCluster(source='root/out', target='root/a'),
    ]
    data = MoveClusterRequest(operations=operations)

    response = client.post(request_url, data=data.dict())
    assert response.status_code == 200

    assert Page.objects.get(id=page_cluster['root/a'].id).slug == 'root/b'
    assert Page.objects.get(id=page_cluster['root/b'].id).slug == 'root/a'


@celery_eager
def test_move__blocked_cluster(client, page_cluster, wiki_users, test_org):
    ASYNC_OP_PROGRESS_STORAGE.clear()
    client.login(wiki_users.thasonic)
    request_url = '/api/v2/public/pages/move'

    op = MoveCluster(source='root/a/aa', target='target/one')
    data = MoveClusterRequest(operations=[op])

    def create_cluster_block(source='', target='', status=ChangeStatus.FWD_MOVE_PHASE) -> ClusterBlock:
        author = wiki_users.asm
        change = ClusterBlock(status=status, source=source, target=target, user=author, org=test_org, task_id='')
        change.save()
        return change

    # 1. блокируем родительскую страницу источника
    source_block = create_cluster_block(source='root/a')
    response = client.post(request_url, data=data.dict())
    assert response.status_code == 400, response.json()
    assert response.json()['error_code'] == CLUSTER_BLOCKED
    source_block.delete()

    # 2. блокируем родительскую страницу назначения
    target_block = create_cluster_block(target='target')
    response = client.post(request_url, data=data.dict())
    assert response.status_code == 400, response.json()
    assert response.json()['error_code'] == CLUSTER_BLOCKED
    target_block.delete()

    # 3. блокируем дочернюю страницу источника
    child_block = create_cluster_block(source='root/a/aa/aaa')
    response = client.post(request_url, data=data.dict())
    assert response.status_code == 400, response.json()
    assert response.json()['error_code'] == CLUSTER_BLOCKED
    child_block.delete()

    response = client.post(request_url, data=data.dict())
    assert response.status_code == 200, response.json()


@celery_eager
@only_biz
def test_move__filter_by_org(client, wiki_users, page_cluster, organizations):
    client.login(wiki_users.thasonic)
    request_url = '/_api/frontend/.async_operations/move_cluster'

    # change page org
    other = page_cluster['root/a']
    other.org = organizations.org_21
    other.save()

    data = {'source': 'root', 'target': 'first/second', 'with_children': True}
    response = client.post(request_url, data=data)
    assert response.status_code == 200

    assert Page.objects.filter(supertag='root/a', org=organizations.org_21).exists()
    assert Page.objects.filter(supertag='first/second', org=organizations.org_42).exists()


def assert_order(ordered: list[str]):
    pages = Page.objects.filter(supertag__in=ordered).order_by('-rank').values_list('supertag', flat=True)
    assert list(pages) == ordered


@celery_eager
def test_move__rank__next_to_moved_out(client, page_cluster, wiki_users):
    client.login(wiki_users.thasonic)
    request_url = '/api/v2/public/pages/move'

    operations = [
        MoveCluster(source='root/a/ad', target='root/ad', next_to_slug='root/a', position=PageOrderPosition.BEFORE),
        MoveCluster(source='root/a', target='root/b/a'),
    ]
    data = MoveClusterRequest(operations=operations)

    response = client.post(request_url, data=data.dict())
    assert response.status_code == 200
    assert_order(['root/c', 'root/b', 'root/ad'])
    assert_order(['root/b/a', 'root/b/bd'])


@celery_eager
def test_move__rank__next_to_new(client, page_cluster, wiki_users):
    client.login(wiki_users.thasonic)
    request_url = '/api/v2/public/pages/move'

    operations = [
        MoveCluster(source='root/b/bd/bc', target='root/bc', next_to_slug='root/a', position=PageOrderPosition.BEFORE),
        MoveCluster(source='root/b/bd', target='root/bd', next_to_slug='root/bc', position=PageOrderPosition.AFTER),
    ]
    data = MoveClusterRequest(operations=operations)

    response = client.post(request_url, data=data.dict())
    assert response.status_code == 200
    assert_order(['root/c', 'root/b', 'root/bc', 'root/bd', 'root/a'])


@celery_eager
def test_move__rank__cluster_move(client, page_cluster, wiki_users):
    client.login(wiki_users.thasonic)
    request_url = '/api/v2/public/pages/move'

    operations = [
        MoveCluster(source='root/a/ac/bd', target='root/c/bd'),
        MoveCluster(source='root/a/aa', target='root/c/aa', next_to_slug='root/c/bd', position='before'),
        MoveCluster(source='root/b/bd/bc', target='root/c/bc', next_to_slug='root/c/aa', position='after'),
        MoveCluster(source='root/c', target='out'),
        MoveCluster(source='root/a/ad', target='out/ad', next_to_slug='out/bc', position='before'),
    ]
    data = MoveClusterRequest(operations=operations)

    response = client.post(request_url, data=data.dict())
    assert response.status_code == 200
    assert_order(['out/aa', 'out/ad', 'out/bc', 'out/bd'])


@celery_eager
def test_move__rank__same_place(client, page_cluster, wiki_users):
    client.login(wiki_users.thasonic)
    request_url = '/api/v2/public/pages/move'

    operations = [
        MoveCluster(source='root/b/bd', target='root/a/bd', next_to_slug='root/a/aa', position=PageOrderPosition.AFTER),
        MoveCluster(source='root/b', target='root/out'),
        MoveCluster(source='root/a', target='root/b'),
    ]
    data = MoveClusterRequest(operations=operations)

    response = client.post(request_url, data=data.dict())
    assert response.status_code == 200
    assert_order(['root/b/ad', 'root/b/aa', 'root/b/bd'])


@celery_eager
def test_move__rank__position_before(client, wiki_users, page_cluster):
    client.login(wiki_users.thasonic)
    request_url = '/api/v2/public/pages/move'

    operations = [
        MoveCluster(source='root/b', target='root/a/b', next_to_slug='root/a/aa', position=PageOrderPosition.BEFORE),
    ]
    data = MoveClusterRequest(operations=operations)

    response = client.post(request_url, data=data.dict())
    assert response.status_code == 200
    assert_order(['root/a/ad', 'root/a/b', 'root/a/aa'])


@celery_eager
def test_move__rank__position_after(client, wiki_users, page_cluster):
    client.login(wiki_users.thasonic)
    request_url = '/api/v2/public/pages/move'

    operations = [
        MoveCluster(source='root/b', target='root/a/b', next_to_slug='root/a/aa', position=PageOrderPosition.AFTER),
    ]
    data = MoveClusterRequest(operations=operations)

    response = client.post(request_url, data=data.dict())
    assert response.status_code == 200
    assert_order(['root/a/aa', 'root/a/b'])


@celery_eager
def test_rewrite_links(client, page_cluster, wiki_users, test_org):
    client.login(wiki_users.thasonic)

    wiki_host = 'https://' + list(settings.FRONTEND_HOSTS)[0]

    inner_page = page_cluster['root/b/bd']
    outer_page = page_cluster['root/c']
    inner_page.body = (
        'link to page ((/root/b))\n'
        f'link to page with host (({wiki_host}/root/b/ coolpage))\n'
        '((/root/b/bd/bc bc))\n'
        '((!/bc relative))\n'
        '{{include page="/root/b/bd/bc"}}\n'
        '{{include page="' + wiki_host + '/root/b/bd/bc"}}'
    )
    inner_page.save()
    outer_page.body = (
        'http://yandex.ru\n'
        '{{include page="/root/b/bd/bc" notitle notoc nowarning from="start-anchor" to="end-anchor"}}\n'
        '{{include page="' + wiki_host + '/root/b/bd/bc" notitle notoc nowarning from="start-anchor" to="end-anchor"}}'
    )
    outer_page.save()

    with org_ctx(test_org):
        track_links(inner_page, False)
        track_links(outer_page, False)

    request_url = '/api/v2/public/pages/move'
    op = MoveCluster(source='root/b', target='root/a/b')
    data = MoveClusterRequest(operations=[op])
    response = client.post(request_url, data=data.dict())
    assert response.status_code == 200

    inner_page = Page.objects.get(id=inner_page.id)
    expected_inner_body = (
        'link to page ((/root/a/b))\n'
        'link to page with host ((/root/a/b coolpage))\n'
        '((/root/a/b/bd/bc bc))\n'
        '((/root/a/b/bd/bc relative))\n'
        '{{include page="/root/a/b/bd/bc"}}\n'
        '{{include page="/root/a/b/bd/bc"}}'
    )
    assert inner_page.body == expected_inner_body

    outer_page = Page.objects.get(id=outer_page.id)
    expected_outer_body = (
        'http://yandex.ru\n'
        '{{include page="/root/a/b/bd/bc" notitle notoc nowarning from="start-anchor" to="end-anchor"}}\n'
        '{{include page="/root/a/b/bd/bc" notitle notoc nowarning from="start-anchor" to="end-anchor"}}'
    )
    assert outer_page.body == expected_outer_body


@celery_eager
def test_rewrite_links__with_grid(client, wiki_users, page_cluster, test_grid, test_org):
    client.login(wiki_users.thasonic)

    wiki_host = 'https://' + list(settings.FRONTEND_HOSTS)[0]

    root_a = page_cluster['root/a']
    root_b = page_cluster['root/b']

    root_a.body = '{{grid page="/testgrid"}}\n' '{{grid page="' + wiki_host + '/testgrid"}}\n'
    root_a.save()

    root_b.body = (
        '{{grid page="/testgrid" width="100%" readonly num="0" filter="[101] > 50" columns="100,101,103"}}\n'
        '{{grid page="' + wiki_host + '/testgrid" width="100%" readonly num="0" filter="[103] > 50" columns="103"}}\n'
    )
    root_b.save()

    with org_ctx(test_org):
        track_links(root_a, False)
        track_links(root_b, False)

    request_url = '/api/v2/public/pages/move'
    op = MoveCluster(source='testgrid', target='root/testgrid')
    data = MoveClusterRequest(operations=[op])
    response = client.post(request_url, data=data.dict())
    assert response.status_code == 200

    root_a = Page.objects.get(id=root_a.id)
    expected_body = '{{grid page="/root/testgrid"}}\n' '{{grid page="/root/testgrid"}}\n'
    assert root_a.body == expected_body

    root_b = Page.objects.get(id=root_b.id)
    expected_body = (
        '{{grid page="/root/testgrid" width="100%" readonly num="0" filter="[101] > 50" columns="100,101,103"}}\n'
        '{{grid page="/root/testgrid" width="100%" readonly num="0" filter="[103] > 50" columns="103"}}\n'
    )
    assert root_b.body == expected_body


@celery_eager
def test_rewrite_links__with_emoji(client, wiki_users, page_cluster, test_org):
    client.login(wiki_users.thasonic)

    root_a = page_cluster['root/a']
    root_b = page_cluster['root/b']

    root_a.body = '((/root/c))'
    root_a.save()

    body_with_emoji = emoji.emojize('((/root/c)) :thumbs_up:!')
    root_b.body = body_with_emoji
    root_b.save()

    with org_ctx(test_org):
        track_links(root_a, False)
        track_links(root_b, False)

    request_url = '/api/v2/public/pages/move'
    op = MoveCluster(source='root/c', target='root/d')
    data = MoveClusterRequest(operations=[op])
    response = client.post(request_url, data=data.dict())
    assert response.status_code == 200

    root_a = Page.objects.get(id=root_a.id)
    expected_body = '((/root/d))'
    assert root_a.body == expected_body

    root_b = Page.objects.get(id=root_b.id)
    assert root_b.body == body_with_emoji
