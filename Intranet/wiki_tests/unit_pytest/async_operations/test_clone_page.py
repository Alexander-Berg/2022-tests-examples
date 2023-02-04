import pytest
import re

from django.test import override_settings

from wiki.api_v2.exceptions import Forbidden, QuotaExceeded
from wiki.api_v2.public.pages.exceptions import IsCloudPage, SlugOccupied, SlugReserved, ClusterBlocked
from wiki.api_v2.public.pages.page_identity import PageIdentity
from wiki.async_operations.consts import Status, OperationOwner
from wiki.async_operations.operation_executors.clone_page.clone_page import ClonePageOperation
from wiki.async_operations.operation_executors.clone_page.schemas import PageClone, PageCloneRequest
from wiki.billing import RESTRICTION_NAMES
from wiki.files.models import File
from wiki.grids.models import Grid
from wiki.grids.utils import dummy_request_for_grids, insert_rows
from wiki.notifications.models import PageEvent
from wiki.pages.constants import ReservedSupertagAction as Action
from wiki.async_operations.progress_storage import ASYNC_OP_PROGRESS_STORAGE
from wiki.pages.logic.comment import add_comment
from wiki.pages.models import Page, Revision, PageLink, AbsentPage, Comment
from wiki.pages.models.cluster_change import ClusterChange, ClusterBlock, ChangeStatus

from intranet.wiki.tests.wiki_tests.common.acl_helper import set_access_author_only
from intranet.wiki.tests.wiki_tests.common.skips import only_biz
from intranet.wiki.tests.wiki_tests.common.utils import celery_eager

pytestmark = [pytest.mark.django_db]


def assert_validation_error(page: Page, target: str, error, owner: OperationOwner):
    data = PageClone(page=PageIdentity.serialize(page), data=PageCloneRequest(target=target))
    with pytest.raises(error):
        ClonePageOperation(data, owner).check_preconditions()


def test_preconditions__cloud(cloud_page_cluster, owner):
    page = cloud_page_cluster['root/b']
    assert_validation_error(page, target='some', error=IsCloudPage, owner=owner)


def test_preconditions__occupied(page_cluster, owner):
    page = page_cluster['root/b']
    assert_validation_error(page, target='root/a', error=SlugOccupied, owner=owner)


def test_preconditions__reserved(test_page, owner):
    reserved_slugs = {'pattern': re.compile(r'^users(/[^/]+)?/?$'), 'actions': [Action.CREATE, Action.DELETE]}
    with override_settings(RESERVED_SUPERTAGS=[reserved_slugs]):
        assert_validation_error(test_page, target='users/login', error=SlugReserved, owner=owner)


def test_preconditions__accesses(page_cluster, owner, wiki_users):
    page = page_cluster['root/c']
    set_access_author_only(page, [wiki_users.asm])

    # no access to source
    assert_validation_error(page, target='some', error=Forbidden, owner=owner)

    # no access to target
    assert_validation_error(page_cluster['root/b'], target='root/c/lock', error=Forbidden, owner=owner)


@only_biz
def test_preconditions__quota(test_page, owner):
    test_page.org.mode.limits[RESTRICTION_NAMES.org_page_num] = 0
    test_page.org.mode.save()
    assert_validation_error(test_page, target='some', error=QuotaExceeded, owner=owner)


def test_clone__post_endpoint(client, wiki_users, test_page):
    client.login(wiki_users.thasonic)
    url = f'/api/v2/public/pages/{test_page.id}/clone'

    data = PageCloneRequest(target='some')
    response = client.post(url, data=data.dict())
    assert response.status_code == 200

    task_id = response.json()['operation']['id']
    assert ASYNC_OP_PROGRESS_STORAGE.load_by_task_id(task_id).status == Status.SCHEDULED


@celery_eager
def test_clone__positive_case(client, wiki_users, test_page, test_files):
    client.login(wiki_users.thasonic)

    add_comment(user=wiki_users.asm, page=test_page, body='comment')

    url = f'/api/v2/public/pages/{test_page.id}/clone'

    target = 'some'
    data = PageCloneRequest(target=target)

    response = client.post(url, data=data.dict())
    assert response.status_code == 200

    copied_page = Page.objects.get(supertag=target)

    status_url = response.json()['status_url']
    response = client.get(status_url)

    result = response.json()
    assert result['status'] == Status.SUCCESS
    assert result['result']['page']['slug'] == target
    assert result['result']['page']['id'] == copied_page.id

    assert copied_page.body == test_page.body
    assert copied_page.reference_page == test_page
    assert copied_page.org == test_page.org
    assert copied_page.title == test_page.title
    assert list(copied_page.authors.all()) == [client.user]
    assert copied_page.owner == client.user
    assert copied_page.last_author == client.user

    assert test_page.comments != 0
    assert copied_page.comments == 0
    assert Comment.objects.filter(page=copied_page).exists() is False

    assert test_page.files != 0
    assert copied_page.files == 0
    assert File.objects.filter(page=copied_page).exists() is False


@celery_eager
def test_clone__positive_case__grid(client, wiki_users, test_grid):
    client.login(wiki_users.thasonic)

    insert_rows(
        test_grid,
        [
            {'src': 'source1', 'dst': 'destination1'},
            {'src': 'source2'},
            {'src': 'source3', 'dst': 'destination2', 'staff': 'chapson'},
        ],
        dummy_request_for_grids(),
    )
    test_grid.save()

    url = f'/api/v2/public/pages/{test_grid.id}/clone'
    target = 'some'
    data = PageCloneRequest(target=target, with_data=True)
    response = client.post(url, data=data.dict())
    assert response.status_code == 200

    grid = Grid.objects.get(supertag=target)
    assert grid.access_structure == test_grid.access_structure
    assert grid.access_data == test_grid.access_data


@celery_eager
def test_clone__side_effects(client, wiki_users, page_cluster):
    client.login(wiki_users.thasonic)

    page = page_cluster['root/b/bd']
    page.body = 'link to page ((/root/b))\n((/root/b/bd/bc bc))\n((!/bc relative))\n{{include page="/root/b/bd/bc"}}'
    page.save()

    url = f'/api/v2/public/pages/{page.id}/clone'
    target = 'some'
    data = PageCloneRequest(target=target)
    response = client.post(url, data=data.dict())
    assert response.status_code == 200

    copied_page = Page.objects.get(supertag=target)

    assert Revision.objects.filter(page=copied_page).exists()
    assert PageEvent.objects.filter(page=copied_page, event_type=PageEvent.EVENT_TYPES.create).exists()
    assert not ClusterChange.objects.filter(user=client.user).exists()

    assert PageLink.objects.filter(from_page=copied_page).count() == 2
    assert AbsentPage.objects.filter(from_page=copied_page, to_supertag=f'{copied_page.slug}/bc').count() == 1


@celery_eager
def test_clone__blocked_cluster(client, page_cluster, wiki_users, test_org):
    client.login(wiki_users.thasonic)

    page = page_cluster['root/b/bd']
    url = f'/api/v2/public/pages/{page.id}/clone'

    block = ClusterBlock(
        status=ChangeStatus.FWD_MOVE_PHASE, source='', target='', user=wiki_users.asm, org=test_org, task_id=''
    )

    for block_slug in ['root/b', 'root/a']:
        block.source = block_slug
        block.save()

        data = PageCloneRequest(target='root/a/xx')
        response = client.post(url, data=data.dict())

        assert response.status_code == 400, response.json()
        assert response.json()['error_code'] == ClusterBlocked.error_code

    block.delete()
    response = client.post(url, data=data.dict())
    assert response.status_code == 200, response.json()


@celery_eager
def test_clone__title(client, wiki_users, test_page, test_files):
    client.login(wiki_users.thasonic)

    url = f'/api/v2/public/pages/{test_page.id}/clone'

    data = PageCloneRequest(target='some', title='Hello!')
    response = client.post(url, data=data.dict())
    assert response.status_code == 200

    copied_page = Page.objects.get(supertag=data.target)
    assert copied_page.title == data.title

    # invalid
    for title in ['', '   ']:
        data = {'target': 'other', 'title': title}
        response = client.post(url, data=data)
        assert response.status_code == 400
