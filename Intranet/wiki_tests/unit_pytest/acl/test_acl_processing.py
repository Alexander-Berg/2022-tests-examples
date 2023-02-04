from typing import Union

import pytest

from wiki.acl.logic.calculation import get_root_acl
from wiki.acl.logic.processing import (
    convert_page_access_to_new_acl,
    update_acl_after_page_creation,
    update_acl_after_page_deletion,
    update_acl_after_page_restore,
    update_acl_after_page_movement,
)
from wiki.acl.consts import ParentAclType, AclDiff, AclRecord, AclListType, AclSubjectType, AclSubject, AclMetaGroup
from wiki.acl.models import Acl
from wiki.pages.api import save_page
from wiki.pages.models import Page
from wiki import access as wiki_access
from wiki.pages.logic import access as access_logic
from wiki.pages.utils.resurrect import resurrect_page
from wiki.utils import timezone
from wiki.pages.utils.remove import delete_page
from wiki.pages.logic.move import move_clusters
from intranet.wiki.tests.wiki_tests.common.skips import only_intranet

pytestmark = [
    pytest.mark.django_db,
]


def _create_page(supertag: str, root_page: Page) -> Page:
    new_page = Page(
        supertag=supertag,
        modified_at=timezone.now(),
        org=root_page.org,
    )
    new_page.save()
    return new_page


def _get_page_acl(page: Page) -> Union[Acl, None]:
    try:
        return Acl.objects.get(page=page)
    except Acl.DoesNotExist:
        return None


def _get_acl_diff_for_allstaff():
    return get_root_acl()


def test_convert_empty_page_access_for_root_page(page_cluster):
    page = page_cluster['root']
    new_acl = convert_page_access_to_new_acl(page)

    expected_acl = Acl(
        page=page,
        break_inheritance=True,
        acl=_get_acl_diff_for_allstaff().json(),
        parent_acl=None,
        parent_acl_type=ParentAclType.ROOT_ACL,
    )
    assert new_acl == expected_acl


def test_convert_empty_page_access_for_child_page(page_cluster):
    page = page_cluster['root']
    page1 = page_cluster['root/a']
    new_acl1 = convert_page_access_to_new_acl(page1)

    expected_acl = Acl(
        page=page1,
        break_inheritance=False,
        acl=AclDiff().json(),
        parent_acl=_get_page_acl(page),
        parent_acl_type=ParentAclType.PAGE_ACL,
    )
    assert new_acl1 == expected_acl


def test_convert_exist_page_acl(page_cluster, page_acl):
    # для страницы 'root' уже создан acl объект
    page = page_cluster['root']
    new_acl = convert_page_access_to_new_acl(page)
    assert page_acl['root'] == new_acl


def test_convert_page_access_with_owner_access(wiki_users, test_page):
    wiki_access.set_access(test_page, wiki_access.TYPES.OWNER, wiki_users.thasonic)
    page_access = access_logic.get_access(test_page)
    assert page_access.type == 'owner'

    new_acl = convert_page_access_to_new_acl(test_page)

    acl_diff = AclDiff(
        add=[
            AclRecord(
                list_name=AclListType.FULL_ACCESS,
                members=[AclSubject(subj_id=AclMetaGroup.AUTHORS.value, subj_type=AclSubjectType.META_GROUP)],
            )
        ]
    )

    expected_acl = Acl(
        page=test_page,
        break_inheritance=True,
        acl=acl_diff.json(),
        parent_acl=None,
        parent_acl_type=ParentAclType.ROOT_ACL,
    )
    assert new_acl == expected_acl


@only_intranet
@pytest.mark.skip('Unfinished')
def test_convert_page_access_with_restricted_access(wiki_users, test_page, groups):
    access_users = (wiki_users.chapson, wiki_users.volozh)
    access_groups = (groups.root_group, groups.side_group)
    wiki_access.set_access(
        test_page,
        wiki_access.TYPES.RESTRICTED,
        wiki_users.thasonic,
        staff_models=[user.staff for user in access_users],
        groups=access_groups,
    )
    page_access = access_logic.get_access(test_page)
    assert page_access.type == 'restricted'

    new_acl = convert_page_access_to_new_acl(test_page)

    acl_diff = AclDiff(
        add=[
            AclRecord(
                list_name=AclListType.FULL_ACCESS,
                members=[obj.get_acl_subject() for obj in access_users + access_groups],
            )
        ]
    )

    expected_acl = Acl(
        page=test_page,
        break_inheritance=True,
        acl=acl_diff.json(),
        parent_acl=None,
        parent_acl_type=ParentAclType.ROOT_ACL,
    )
    assert new_acl == expected_acl


def test_convert_page_access_with_inherited_access(wiki_users, page_cluster):
    root_page = page_cluster['root']
    parent_page = page_cluster['root/a']
    page = page_cluster['root/a/aa']
    wiki_access.set_access(page, wiki_access.TYPES.INHERITED, wiki_users.thasonic)
    page_access = access_logic.get_access(page)
    assert page_access.type == 'inherited'
    page_access = access_logic.get_access(parent_page)
    assert page_access.type == 'inherited'
    page_access = access_logic.get_access(root_page)
    assert page_access.type == 'common'

    new_acl = convert_page_access_to_new_acl(page)
    expected_acl = Acl(
        page=page,
        break_inheritance=False,
        acl=AclDiff().json(),
        parent_acl=_get_page_acl(parent_page),
        parent_acl_type=ParentAclType.PAGE_ACL,
    )
    assert new_acl == expected_acl

    new_acl = convert_page_access_to_new_acl(parent_page)
    expected_acl = Acl(
        page=parent_page,
        break_inheritance=False,
        acl=AclDiff().json(),
        parent_acl=_get_page_acl(root_page),
        parent_acl_type=ParentAclType.PAGE_ACL,
    )
    assert new_acl == expected_acl

    new_acl = convert_page_access_to_new_acl(root_page)
    expected_acl = Acl(
        page=root_page,
        break_inheritance=True,
        acl=_get_acl_diff_for_allstaff().json(),
        parent_acl=None,
        parent_acl_type=ParentAclType.ROOT_ACL,
    )
    assert new_acl == expected_acl


def test_update_acl_after_page_creation(page_cluster, page_acl):
    root_page = page_cluster['root']
    new_page = _create_page('root/d', root_page)
    update_acl_after_page_creation(new_page)
    acl = _get_page_acl(new_page)

    expected_acl = Acl(
        page=new_page,
        break_inheritance=False,
        acl=AclDiff().json(),
        parent_acl=_get_page_acl(root_page),
        parent_acl_type=ParentAclType.PAGE_ACL,
    )
    assert acl == expected_acl

    new_page = _create_page('root/nopage/d', root_page)
    update_acl_after_page_creation(new_page)
    acl = _get_page_acl(new_page)

    expected_acl = Acl(
        page=new_page,
        break_inheritance=False,
        acl=AclDiff().json(),
        parent_acl=_get_page_acl(root_page),
        parent_acl_type=ParentAclType.PAGE_ACL,
    )
    assert acl == expected_acl


def test_update_acl_after_child_page_deletion_and_restore(page_cluster, page_acl):
    root_page = page_cluster['root']
    test_page = page_cluster['root/a']
    child1_page = page_cluster['root/a/aa']
    child2_page = page_cluster['root/a/ad']
    test_page_acl = _get_page_acl(test_page)
    child_page1_acl = _get_page_acl(child1_page)
    child_page2_acl = _get_page_acl(child2_page)
    expected_acl_before_deletion = Acl(
        page=test_page,
        break_inheritance=False,
        acl=AclDiff().json(),
        parent_acl=_get_page_acl(root_page),
        parent_acl_type=ParentAclType.PAGE_ACL,
    )
    assert test_page_acl == expected_acl_before_deletion
    assert child_page1_acl.parent_acl == test_page_acl
    assert child_page2_acl.parent_acl == test_page_acl

    delete_page(test_page)
    assert test_page.status == 0

    update_acl_after_page_deletion(test_page)
    test_page_acl = _get_page_acl(test_page)
    child_page1_acl = _get_page_acl(child1_page)
    child_page2_acl = _get_page_acl(child2_page)
    expected_acl_after_deletion = Acl(
        page=test_page,
        break_inheritance=False,
        acl=AclDiff().json(),
        parent_acl=None,
        parent_acl_type=ParentAclType.PAGE_ACL,
    )
    assert test_page_acl == expected_acl_after_deletion
    assert child_page1_acl.parent_acl == _get_page_acl(root_page)
    assert child_page2_acl.parent_acl == _get_page_acl(root_page)

    resurrect_page(test_page)
    assert test_page.status > 0

    update_acl_after_page_restore(test_page)
    test_page_acl = _get_page_acl(test_page)
    child_page1_acl = _get_page_acl(child1_page)
    child_page2_acl = _get_page_acl(child2_page)
    assert test_page_acl == expected_acl_before_deletion
    assert child_page1_acl.parent_acl == test_page_acl
    assert child_page2_acl.parent_acl == test_page_acl


def test_update_acl_after_root_page_deletion_and_restore(page_cluster, page_acl):
    root_page = page_cluster['root']
    child_page = page_cluster['root/a']
    root_page_acl = _get_page_acl(root_page)
    child_page_acl = _get_page_acl(child_page)
    expected_acl_before_deletion = Acl(
        page=root_page,
        break_inheritance=False,
        acl=AclDiff().json(),
        parent_acl=None,
        parent_acl_type=ParentAclType.ROOT_ACL,
    )
    assert root_page_acl == expected_acl_before_deletion
    assert child_page_acl.parent_acl == root_page_acl

    delete_page(root_page)
    assert root_page.status == 0

    update_acl_after_page_deletion(root_page)
    root_page_acl = _get_page_acl(root_page)
    child_page_acl = _get_page_acl(child_page)
    expected_acl_after_deletion = Acl(
        page=root_page,
        break_inheritance=False,
        acl=AclDiff().json(),
        parent_acl=None,
        parent_acl_type=ParentAclType.ROOT_ACL,
    )
    assert root_page_acl == expected_acl_after_deletion
    assert child_page_acl.parent_acl is None
    assert child_page_acl.parent_acl_type == ParentAclType.ROOT_ACL

    resurrect_page(root_page)
    assert root_page.status > 0

    update_acl_after_page_restore(root_page)
    root_page_acl = _get_page_acl(root_page)
    child_page_acl = _get_page_acl(child_page)
    assert root_page_acl == expected_acl_before_deletion
    assert child_page_acl.parent_acl == root_page_acl


def test_update_acl_after_page_movement(wiki_users, page_cluster, page_acl, test_org_ctx):
    root_page = page_cluster['root']
    source_page = page_cluster['root/b']
    target_root_page = page_cluster['root/a']
    child_page = page_cluster['root/a/ad']

    # пересохраняем страницу, чтобы переформатировать старым форматером, иначе падает при переносе кластера
    save_page(source_page, 'example text')

    access_users = (wiki_users.chapson, wiki_users.volozh)
    acl_diff = AclDiff(
        add=[
            AclRecord(
                list_name=AclListType.FULL_ACCESS,
                members=[obj.get_acl_subject() for obj in access_users],
            )
        ]
    )

    source_page_acl = _get_page_acl(source_page)
    source_page_acl.acl = acl_diff.json()
    source_page_acl.save()

    expected_source_page_acl = Acl(
        page=source_page,
        break_inheritance=False,
        acl=acl_diff.json(),
        parent_acl=_get_page_acl(root_page),
        parent_acl_type=ParentAclType.PAGE_ACL,
    )
    assert source_page_acl == expected_source_page_acl

    old_supertag = source_page.supertag
    new_supertag = 'root/a/b'
    clusters = {old_supertag: new_supertag}

    status, data = move_clusters(
        user=wiki_users.thasonic,
        clusters=clusters,
    )
    assert status == 200
    page_redirect = Page.objects.get(supertag=old_supertag)
    moved_page = Page.objects.get(supertag=new_supertag)
    assert page_redirect.redirects_to == moved_page

    update_acl_after_page_movement(page_redirect)

    expected_source_page_acl.page = page_redirect
    assert expected_source_page_acl == _get_page_acl(page_redirect)

    expected_moved_page_acl = expected_source_page_acl
    expected_moved_page_acl.page = moved_page
    expected_moved_page_acl.parent_acl = _get_page_acl(target_root_page)
    assert expected_moved_page_acl == _get_page_acl(moved_page)

    assert _get_page_acl(child_page).parent_acl == _get_page_acl(target_root_page)
