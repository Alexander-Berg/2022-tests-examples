import pytest

from wiki.pages.models import Page
from wiki.files.models import File
from wiki.cloudsearch.utils import render_document_for_indexation, find_by_search_uuid
from model_mommy import mommy
from intranet.wiki.tests.wiki_tests.common.acl_helper import set_access
from intranet.wiki.tests.wiki_tests.common.factories.group import GroupFactory
from wiki.users_biz.models import GROUP_TYPES


def return_metadata(page: Page):
    return {
        'created_at': int(page.created_at.timestamp()),
        'is_obsolete': (page.actuality_status == 2),
        'modified_at': int(page.modified_at.timestamp()),
        'authors': [{'uid': a.get_uid(), 'cloud_uid': a.get_cloud_uid()} for a in page.get_authors()],
        'slug': page.supertag,
        'type': 'page',
        'url': page.url,
        'page_type': page.page_type,
    }


@pytest.mark.django_db
def test_render_document(page_cluster, wiki_users, organizations, test_org_ctx):
    page = page_cluster['root']
    users = [wiki_users.chapson, wiki_users.kolomeetz]
    groups = [
        GroupFactory(
            name='group_42',
            title='title',
            group_dir_type='generic',
            org_id=organizations.org_42.id,
            group_type=GROUP_TYPES.group,
        )
    ]

    set_access(page, 'restricted', restrictions={'users': users, 'groups': groups})
    data = render_document_for_indexation(page)

    assert 'acl' in data
    assert data['document'] == {'body': page.body, 'title': page.title, 'keywords': page.get_keywords()}
    assert data['metadata'] == return_metadata(page)
    assert data['uuid'] == f'wp:{page.id}'

    file: File = mommy.make(File, page=page, user=wiki_users.thasonic)

    data_ = render_document_for_indexation(file)

    assert data['acl'] == data_['acl']
    data = data_
    assert data['document'] == {'title': file.name, 'keywords': file.page.get_keywords()}
    assert data['metadata'] == {
        'created_at': int(file.created_at.timestamp()),
        'is_obsolete': (file.page.actuality_status == 2),
        'modified_at': int(file.modified_at.timestamp()),
        'authors': [{'uid': file.user.get_uid(), 'cloud_uid': file.user.get_cloud_uid()}],
        'slug': file.page.supertag,
        'type': 'file',
        'url': file.get_download_url(),
    }

    actual_uids = [el.get_uid() for el in users]
    actual_uids.extend([el['uid'] for el in data['metadata']['authors']])

    actual_cloud_uids = [el.get_cloud_uid() for el in users]
    actual_cloud_uids.extend([el['cloud_uid'] for el in data['metadata']['authors']])

    assert data['acl']['group_ids'] == [groups[0].id]
    assert data['acl']['is_restricted']
    assert data['acl']['org_id'] == page.org.dir_id

    assert set(data['acl']['uids']) == set(actual_uids)
    assert set(data['acl']['cloud_uids']) == set(actual_cloud_uids)

    assert data['uuid'] == f'wf:{file.id}'


@pytest.mark.django_db
def test_render_document_no_restrictions(page_cluster, test_org_ctx):
    page = page_cluster['root/a']

    data = render_document_for_indexation(page)

    assert data['acl'] == {
        'cloud_uids': [],
        'group_ids': [],
        'is_restricted': False,
        'org_id': page.org.dir_id,
        'uids': [],
    }

    assert data['uuid'] == f'wp:{page.id}'
    assert data['metadata'] == return_metadata(page)
    assert data['document'] == {'body': page.body, 'title': page.title, 'keywords': page.get_keywords()}


@pytest.mark.django_db
def test_render_document_only_authors(page_cluster, test_org_ctx):
    page = page_cluster['root/a/aa']
    set_access(page, 'owner')

    data = render_document_for_indexation(page)

    assert data['acl'] == {
        'cloud_uids': [el['cloud_uid'] for el in data['metadata']['authors']],
        'group_ids': [],
        'is_restricted': True,
        'org_id': page.org.dir_id,
        'uids': [el['uid'] for el in data['metadata']['authors']],
    }


@pytest.mark.django_db
def test_find(page_cluster, wiki_users, test_org_ctx):
    page = page_cluster['root/b']
    search_uuid = render_document_for_indexation(page)['uuid']

    result = find_by_search_uuid(search_uuid)
    assert result == page

    file: File = mommy.make(File, page=page, user=wiki_users.thasonic)

    search_uuid = render_document_for_indexation(file)['uuid']
    result = find_by_search_uuid(search_uuid)
    assert result == file
