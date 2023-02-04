"""Create/update/delete methods for controller"""
import random

from staff.groups.tests.utils import assert_queryset_equals_list
from staff.groups.models import Group
from staff.groups.objects import WikiGroupCtl, GroupCtl
from staff.lib.testing import GroupFactory


def test_create_root_group_with_commit(test_data):
    # TODO: нужно сделать тесты на создание каждого типа группы
    new_group = WikiGroupCtl().create(commit=True)

    assert Group.objects.filter(pk=new_group.pk).exists()
    assert new_group.intranet_status == 0
    assert new_group.parent is None


def test_create_child_group_with_commit(test_data):
    new_group = WikiGroupCtl().create(parent=test_data.root, commit=True)

    assert Group.objects.filter(pk=new_group.pk).exists()
    assert new_group.intranet_status == 0
    assert new_group.parent == test_data.root
    assert_queryset_equals_list(new_group.get_ancestors(), [test_data.root.id])


def test_create_child_group_wo_commit(test_data):
    new_group = WikiGroupCtl().create(parent=test_data.root)

    assert not Group.objects.filter(pk=new_group.pk).exists()
    assert new_group.intranet_status == 0
    assert new_group.parent == test_data.root


def test_partial_update_root_group(test_data):
    Group.objects.filter(id=test_data.root.id).update(name='root', code='rt')
    cleaned_data = {
        'description': 'This is root group',
        'position': 1,
    }

    test_data.root = Group.objects.get(id=test_data.root.id)
    GroupCtl(group=test_data.root).update(data=cleaned_data)
    root = Group.objects.get(id=test_data.root.id)

    assert root.name == 'root'
    assert root.code == 'rt'
    assert root.description == 'This is root group'
    assert root.position == 1


def test_create_filled(test_data):
    cleaned_data = {
        'parent': test_data.second_lvl,
        'name': 'some_group'
    }
    new_group = WikiGroupCtl().create_filled(data=cleaned_data)

    assert Group.objects.filter(pk=new_group.pk).exists()
    assert new_group.parent == test_data.second_lvl
    assert new_group.name == cleaned_data['name']
    assert_queryset_equals_list(
        new_group.get_ancestors(),
        [test_data.root.id, test_data.first_lvl.id, test_data.second_lvl.id],
    )


def test_change_parent(test_data):
    new_parent_data = {'parent': test_data.first_lvl}
    GroupCtl(group=test_data.third_lvl).update(data=new_parent_data)

    assert test_data.third_lvl.parent == test_data.first_lvl
    assert_queryset_equals_list(
        test_data.third_lvl.get_ancestors(),
        [test_data.root.id, test_data.first_lvl.id],
    )


def test_group_delete(test_data):
    GroupCtl(group=test_data.third_lvl).delete()
    third_lvl = Group.objects.get(id=test_data.third_lvl.id)

    assert third_lvl.intranet_status == 0
    assert not third_lvl.url == 'third'


def test_update_root_group(test_data):
    group = GroupFactory()
    data = {
        'name_en': f'test name en {random.random()}',
    }

    test_data.root = Group.objects.get(id=test_data.root.id)
    GroupCtl(group=group).update(data=data)

    db_group = Group.objects.get(id=group.id)

    assert db_group.name_en == data['name_en']
