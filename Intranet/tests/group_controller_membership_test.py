"""Membership methods for controller"""
from staff.person.effects.base import actualize_affiliation

from staff.groups.models import GroupMembership
from staff.groups.objects import GroupCtl


# add/delete/check members
def test_add_member(test_data):
    GroupCtl(group=test_data.third_lvl).add_member(test_data.mouse)
    assert GroupMembership.objects.filter(staff=test_data.mouse, group=test_data.third_lvl).exists()


def test_add_member_twice(test_data):
    created = GroupCtl(group=test_data.third_lvl).add_member(test_data.mouse)

    assert GroupMembership.objects.filter(staff=test_data.mouse, group=test_data.third_lvl).exists()
    assert created

    created = GroupCtl(group=test_data.third_lvl).add_member(test_data.mouse)

    assert GroupMembership.objects.filter(staff=test_data.mouse, group=test_data.third_lvl).exists()
    assert not created


def test_add_members_by_logins(test_data):
    GroupMembership.objects.create(staff=test_data.mouse, group=test_data.second_lvl)

    ctl = GroupCtl(group=test_data.second_lvl)
    added_count = ctl.add_members_by_logins(['mouse', 'squirrel', 'eye'])

    assert added_count == 2
    assert test_data.squirrel in test_data.second_lvl.members.all()
    assert test_data.eye in test_data.second_lvl.members.all()


def test_delete_member(test_data):
    GroupMembership.objects.create(staff=test_data.mouse, group=test_data.second_lvl)
    GroupMembership.objects.create(staff=test_data.squirrel,
                                   group=test_data.second_lvl)

    ctl = GroupCtl(group=test_data.second_lvl)
    ctl.delete_member(test_data.mouse)

    members = test_data.second_lvl.members.all()

    assert len(members) == 1
    assert members[0] == test_data.squirrel


def test_delete_all_members(test_data):
    GroupMembership.objects.create(staff=test_data.mouse, group=test_data.second_lvl)
    GroupMembership.objects.create(staff=test_data.squirrel, group=test_data.second_lvl)

    GroupCtl(group=test_data.second_lvl).delete_all_members()

    members_count = test_data.second_lvl.members.count()

    assert members_count == 0


def test_has_members(test_data):
    GroupMembership.objects.create(staff=test_data.mouse, group=test_data.first_lvl)

    assert GroupCtl(group=test_data.first_lvl).has_members()


def test_has_no_members(test_data):
    assert not GroupCtl(group=test_data.third_lvl).has_members()


# get members
def test_get_members(test_data):
    GroupMembership.objects.create(staff=test_data.mouse, group=test_data.second_lvl)
    GroupMembership.objects.create(staff=test_data.squirrel, group=test_data.second_lvl)

    second_members = GroupCtl(group=test_data.second_lvl).get_members()

    assert len(second_members) == 2
    assert test_data.mouse in second_members
    assert test_data.squirrel in second_members


def test_get_members_wo_nested(test_data):
    GroupMembership.objects.create(staff=test_data.mouse, group=test_data.first_lvl)
    GroupMembership.objects.create(staff=test_data.squirrel, group=test_data.second_lvl)

    first_members = GroupCtl(group=test_data.first_lvl).get_members()

    assert len(first_members) == 1
    assert test_data.mouse in first_members


def test_get_members_for_options(test_data):
    GroupMembership.objects.create(staff=test_data.mouse, group=test_data.options)
    GroupMembership.objects.create(staff=test_data.squirrel, group=test_data.options)

    assert len(GroupCtl(group=test_data.options).get_members()) == 0


def test_get_external_members(test_data):
    GroupMembership.objects.create(staff=test_data.mouse, group=test_data.second_lvl)
    GroupMembership.objects.create(staff=test_data.squirrel, group=test_data.second_lvl)

    test_data.mouse.department = test_data.yandex
    actualize_affiliation(test_data.mouse)
    test_data.mouse.save()

    test_data.squirrel.department = test_data.ext
    actualize_affiliation(test_data.squirrel)
    test_data.squirrel.save()

    ext_members = GroupCtl(group=test_data.second_lvl).get_external_members()

    assert len(ext_members) == 1
    assert test_data.squirrel in ext_members


def test_get_external_members_wo_nested(test_data):
    GroupMembership.objects.create(staff=test_data.mouse, group=test_data.first_lvl)
    GroupMembership.objects.create(staff=test_data.squirrel, group=test_data.second_lvl)

    test_data.mouse.department = test_data.ext
    actualize_affiliation(test_data.mouse)
    test_data.mouse.save()

    test_data.squirrel.department = test_data.ext
    actualize_affiliation(test_data.squirrel)
    test_data.squirrel.save()

    ext_members = GroupCtl(group=test_data.first_lvl).get_external_members()

    assert len(ext_members) == 1
    assert test_data.mouse in ext_members


def test_get_all_members(test_data):
    GroupMembership.objects.create(staff=test_data.mouse, group=test_data.first_lvl)

    all_members = GroupCtl(group=test_data.first_lvl).get_all_members()

    assert len(all_members) == 1
    assert test_data.mouse in all_members


def test_get_all_members_with_nested(test_data):
    GroupMembership.objects.create(staff=test_data.mouse, group=test_data.first_lvl)
    GroupMembership.objects.create(staff=test_data.squirrel, group=test_data.second_lvl)

    all_members = GroupCtl(group=test_data.first_lvl).get_all_members()

    assert len(all_members) == 2
    assert test_data.mouse in all_members
    assert test_data.squirrel in all_members


def test_get_all_members_with_nested_wo_doubles(test_data):
    GroupMembership.objects.create(staff=test_data.mouse, group=test_data.first_lvl)
    GroupMembership.objects.create(staff=test_data.mouse, group=test_data.second_lvl)
    GroupMembership.objects.create(staff=test_data.squirrel, group=test_data.second_lvl)

    all_members = GroupCtl(group=test_data.first_lvl).get_all_members()

    assert len(all_members) == 2
    assert test_data.mouse in all_members
    assert test_data.squirrel in all_members


def test_get_all_external_members(test_data):
    GroupMembership.objects.create(staff=test_data.mouse, group=test_data.first_lvl)
    GroupMembership.objects.create(staff=test_data.squirrel, group=test_data.second_lvl)

    test_data.mouse.department = test_data.yandex
    actualize_affiliation(test_data.mouse)
    test_data.mouse.save()

    test_data.squirrel.department = test_data.ext
    actualize_affiliation(test_data.squirrel)
    test_data.squirrel.save()

    ext_members = GroupCtl(group=test_data.first_lvl).get_all_external_members()

    assert len(ext_members) == 1
    assert test_data.squirrel in ext_members


# get members count
def test_get_members_count(test_data):
    GroupMembership.objects.create(staff=test_data.mouse, group=test_data.second_lvl)

    assert GroupCtl(group=test_data.second_lvl).get_members_count() == 1


def test_get_members_count_wo_nested(test_data):
    GroupMembership.objects.create(staff=test_data.mouse, group=test_data.second_lvl)
    GroupMembership.objects.create(staff=test_data.squirrel, group=test_data.third_lvl)

    assert GroupCtl(group=test_data.second_lvl).get_members_count() == 1


def test_externals_count_no_externals(test_data):
    test_data.mouse.department = test_data.yandex
    test_data.mouse.save()
    GroupMembership.objects.create(staff=test_data.mouse, group=test_data.first_lvl)

    assert GroupCtl(group=test_data.first_lvl).get_externals_count() == 0


def test_externals_count_with_externals(test_data):
    test_data.mouse.department = test_data.ext
    actualize_affiliation(test_data.mouse)
    test_data.mouse.save()

    GroupMembership.objects.create(staff=test_data.mouse, group=test_data.first_lvl)

    assert GroupCtl(group=test_data.first_lvl).get_externals_count() == 1


def test_get_all_members_count(test_data):
    GroupMembership.objects.create(staff=test_data.mouse, group=test_data.second_lvl)
    GroupMembership.objects.create(staff=test_data.squirrel, group=test_data.third_lvl)

    assert GroupCtl(group=test_data.second_lvl).get_all_members_count() == 2


def test_get_all_externals_count(test_data):
    GroupMembership.objects.create(staff=test_data.mouse, group=test_data.second_lvl)
    GroupMembership.objects.create(staff=test_data.squirrel, group=test_data.third_lvl)
    test_data.mouse.department = test_data.ext
    actualize_affiliation(test_data.mouse)
    test_data.mouse.save()

    test_data.squirrel.department = test_data.yandex
    actualize_affiliation(test_data.squirrel)
    test_data.squirrel.save()

    assert GroupCtl(group=test_data.second_lvl).get_all_externals_count() == 1


def test_get_all_externals_count_no_duplicates(test_data):
    GroupMembership.objects.create(staff=test_data.mouse, group=test_data.second_lvl)
    GroupMembership.objects.create(staff=test_data.squirrel, group=test_data.third_lvl)
    GroupMembership.objects.create(staff=test_data.mouse, group=test_data.third_lvl)

    test_data.mouse.department = test_data.ext
    actualize_affiliation(test_data.mouse)
    test_data.mouse.save()

    test_data.squirrel.department = test_data.yandex
    actualize_affiliation(test_data.squirrel)
    test_data.squirrel.save()

    assert GroupCtl(group=test_data.second_lvl).get_all_externals_count() == 1
