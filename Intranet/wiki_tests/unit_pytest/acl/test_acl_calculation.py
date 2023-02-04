import pytest

from django.conf import settings

from wiki.acl.logic.calculation import get_effective_acl, check_permission, get_root_acl
from wiki.acl.consts import (
    Action,
    AclDiff,
    AclRecord,
    AclListType,
    PageAcl,
    AclSource,
    AclSubject,
    AclMetaGroup,
    AclSubjectType,
)
from wiki.acl.utils import invalidate_cache

pytestmark = [
    pytest.mark.django_db,
]


def test_no_acl_in_db(wiki_users, page_cluster):
    page = page_cluster['root']
    assert not check_permission(page, wiki_users.volozh, Action.READ)


def test_access_of_author(wiki_users, page_cluster, page_acl):
    def check_access_of_author(page):
        page_authors = page.get_authors().all()
        for action in Action:
            assert check_permission(page, page_authors[0], action)
            assert not check_permission(page, wiki_users.volozh, action)

    acl = page_acl['root']
    acl_obj = AclDiff(
        add=[
            AclRecord(
                list_name=AclListType.FULL_ACCESS,
                members=[AclSubject(subj_id=AclMetaGroup.AUTHORS.value, subj_type=AclSubjectType.META_GROUP)],
            )
        ]
    )
    acl.acl = acl_obj.json()
    acl.break_inheritance = True
    acl.save()

    page = page_cluster['root']
    check_access_of_author(page)

    page = page_cluster['root/a/aa']
    check_access_of_author(page)

    acl = page_acl['root/a/aa']
    acl_obj = AclDiff(add=[AclRecord(list_name=AclListType.DENY, members=[page.get_authors()[0].get_acl_subject()])])
    acl.acl = acl_obj.json()
    acl.save()
    check_access_of_author(page)


def test_break_inheritance(wiki_users, page_cluster, page_acl):
    page = page_cluster['root/a/aa']

    expected_eff_acl = PageAcl(
        page_id=page.id,
        items={
            **{item.list_name: item.members for item in get_root_acl().add},
        },
    )
    eff_acl = get_effective_acl(page.id)
    assert eff_acl == expected_eff_acl

    assert check_permission(page, wiki_users.volozh, Action.WRITE)
    assert check_permission(page, wiki_users.volozh, Action.MANAGE)
    assert check_permission(page, wiki_users.chapson, Action.READ)

    acl = page_acl['root']
    acl_obj = AclDiff(
        add=[
            AclRecord(
                list_name=AclListType.READ_WRITE,
                members=[wiki_users.volozh.get_acl_subject(), wiki_users.chapson.get_acl_subject()],
            )
        ]
    )
    acl.acl = acl_obj.json()
    acl.break_inheritance = True
    acl.save()

    invalidate_cache([page.id])

    expected_eff_acl = PageAcl(
        page_id=page.id,
        items={
            AclListType.READ_WRITE: [wiki_users.volozh.get_acl_subject(), wiki_users.chapson.get_acl_subject()],
        },
    )
    eff_acl = get_effective_acl(page.id)
    assert eff_acl == expected_eff_acl

    assert check_permission(page, wiki_users.volozh, Action.WRITE)
    assert not check_permission(page, wiki_users.volozh, Action.MANAGE)
    assert check_permission(page, wiki_users.chapson, Action.READ)

    invalidate_cache([page.id])

    acl = page_acl['root/a/aa']
    acl.break_inheritance = True
    acl.save()

    expected_eff_acl = PageAcl(page_id=page.id, items={})
    eff_acl = get_effective_acl(page.id)
    assert eff_acl == expected_eff_acl

    assert not check_permission(page, wiki_users.volozh, Action.WRITE)
    assert not check_permission(page, wiki_users.chapson, Action.READ)


if settings.IS_INTRANET:

    def test_access_of_user_from_group(wiki_users, page_cluster, groups, page_acl, add_user_to_group):
        page = page_cluster['root/a/aa']
        acl = page_acl['root/a/aa']
        acl_obj = AclDiff(
            add=[
                AclRecord(
                    list_name=AclListType.READ_WRITE,
                    members=[groups.root_group.get_acl_subject(), groups.child_group.get_acl_subject()],
                )
            ]
        )
        acl.acl = acl_obj.json()
        acl.break_inheritance = True
        acl.save()
        assert not check_permission(page, wiki_users.volozh, Action.WRITE)
        assert not check_permission(page, wiki_users.chapson, Action.READ)

        add_user_to_group(groups.root_group, wiki_users.volozh)
        add_user_to_group(groups.child_group, wiki_users.chapson)
        assert check_permission(page, wiki_users.volozh, Action.WRITE)
        assert check_permission(page, wiki_users.chapson, Action.READ)


def test_deny_user_access(wiki_users, page_cluster, page_acl):
    root_page = page_cluster['root']
    assert check_permission(root_page, wiki_users.chapson, Action.WRITE)
    assert check_permission(root_page, wiki_users.volozh, Action.WRITE)

    page1 = page_cluster['root/a']
    acl = page_acl['root/a']
    acl_obj = AclDiff(add=[AclRecord(list_name=AclListType.DENY, members=[wiki_users.chapson.get_acl_subject()])])
    acl.acl = acl_obj.json()
    acl.save()

    expected_eff_acl = PageAcl(
        page_id=page1.id,
        items={
            **{item.list_name: item.members for item in get_root_acl().add},
            AclListType.DENY: [wiki_users.chapson.get_acl_subject()],
        },
    )
    eff_acl = get_effective_acl(page1.id)
    assert eff_acl == expected_eff_acl

    assert not check_permission(page1, wiki_users.chapson, Action.WRITE)
    assert check_permission(page1, wiki_users.volozh, Action.WRITE)

    page2 = page_cluster['root/a/aa']
    assert not check_permission(page2, wiki_users.chapson, Action.WRITE)
    assert check_permission(page2, wiki_users.volozh, Action.WRITE)


def test_exclude_operation_right(wiki_users, page_cluster, page_acl):
    acl = page_acl['root']
    acl_obj = AclDiff(
        add=[
            AclRecord(
                list_name=AclListType.READ_WRITE,
                members=[wiki_users.chapson.get_acl_subject(), wiki_users.volozh.get_acl_subject()],
            )
        ]
    )
    acl.acl = acl_obj.json()
    acl.break_inheritance = True
    acl.save()

    page1 = page_cluster['root/a']
    acl = page_acl['root/a']
    acl_obj = AclDiff(rm=[AclRecord(list_name=AclListType.READ_WRITE, members=[wiki_users.chapson.get_acl_subject()])])
    acl.acl = acl_obj.json()
    acl.save()

    expected_eff_acl = PageAcl(
        page_id=page1.id,
        items={
            AclListType.READ_WRITE: [wiki_users.volozh.get_acl_subject()],
        },
    )
    eff_acl = get_effective_acl(page1.id)
    assert eff_acl == expected_eff_acl

    assert not check_permission(page1, wiki_users.chapson, Action.WRITE)
    assert check_permission(page1, wiki_users.volozh, Action.WRITE)

    page2 = page_cluster['root/a/aa']
    acl = page_acl['root/a/aa']
    acl_obj = AclDiff(
        add=[
            AclRecord(
                list_name=AclListType.READ_WRITE,
                members=[wiki_users.chapson.get_acl_subject(), wiki_users.volozh.get_acl_subject()],
            )
        ]
    )
    acl.acl = acl_obj.json()
    acl.save()
    assert check_permission(page2, wiki_users.chapson, Action.WRITE)
    assert check_permission(page2, wiki_users.volozh, Action.WRITE)


def test_non_wiki_source(wiki_users, page_cluster, page_acl):
    acl = page_acl['root']
    acl_obj = AclDiff(
        add=[
            AclRecord(
                list_name=AclListType.READ_WRITE,
                members=[wiki_users.chapson.get_acl_subject(), wiki_users.volozh.get_acl_subject()],
                source=AclSource.IDM,
            )
        ],
    )
    acl.acl = acl_obj.json()
    acl.break_inheritance = True
    acl.save()

    page1 = page_cluster['root/a']
    acl = page_acl['root/a']
    acl_obj = AclDiff(rm=[AclRecord(list_name=AclListType.READ_WRITE, members=[wiki_users.chapson.get_acl_subject()])])
    acl.acl = acl_obj.json()
    acl.save()

    expected_eff_acl = PageAcl(
        page_id=page1.id,
        items={
            AclListType.READ_WRITE: [wiki_users.chapson.get_acl_subject(), wiki_users.volozh.get_acl_subject()],
        },
    )
    eff_acl = get_effective_acl(page1.id)
    assert eff_acl == expected_eff_acl

    assert check_permission(page1, wiki_users.chapson, Action.WRITE)
    assert check_permission(page1, wiki_users.volozh, Action.WRITE)
